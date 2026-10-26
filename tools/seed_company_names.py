"""
スタッフ欄に入っている会社名（背景美術の会社・音楽制作会社など）の一覧を作る（1回だけ手元で実行）。

Bangumi の作品別スタッフ一覧には、人物ごとに type（1 = 個人, 2 = 会社, 3 = グループ）が付いている。
AniList の ID を持たない名前（＝ Bangumi 由来）を、なるべく少ない作品数で網羅するように作品を選び、
その作品のスタッフ一覧から type を調べる。結果は tools/non_person_names.txt に書く（週次更新が読み込む）。

  python tools/seed_company_names.py --creditdb creditdb.db --cache seen_types.json [--minutes 60]
"""
import argparse
import json
import os
import sys
import time
import urllib.request
from collections import defaultdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import weekly_updater as wu  # noqa: E402

OUT_PATH = Path(__file__).resolve().parent / "non_person_names.txt"
HEADERS = {"Accept": "application/json", "User-Agent": wu.USER_AGENT, "Content-Type": "application/json"}


def _req(url, payload=None):
    data = json.dumps(payload).encode("utf-8") if payload is not None else None
    req = urllib.request.Request(url, data=data, headers=HEADERS, method="POST" if data else "GET")
    for attempt in range(3):
        try:
            time.sleep(0.35)
            with urllib.request.urlopen(req, timeout=15) as r:
                return json.loads(r.read().decode("utf-8"))
        except urllib.error.HTTPError as e:
            if e.code == 404:
                return None
            time.sleep(3 * (attempt + 1))
        except Exception:
            time.sleep(3 * (attempt + 1))
    return None


def person_types_for(title, year):
    res = _req(f"{wu.BANGUMI_ENDPOINT}/v0/search/subjects?limit=5", {"keyword": title, "filter": {"type": [2]}})
    items = (res or {}).get("data") or []
    sid = wu.pick_bangumi_subject(items, title, year) if items else None
    if not sid:
        return None
    persons = _req(f"{wu.BANGUMI_ENDPOINT}/v0/subjects/{sid}/persons") or []
    return {(p.get("name") or "").strip(): p.get("type") for p in persons if p.get("name")}


def main():
    import sqlite3
    ap = argparse.ArgumentParser()
    ap.add_argument("--creditdb", required=True)
    ap.add_argument("--cache", required=True, help="調べた結果の保存先（途中から再開できる）")
    ap.add_argument("--minutes", type=float, default=120)
    ap.add_argument("--min-works", type=int, default=3)
    a = ap.parse_args()

    cache = json.load(open(a.cache, encoding="utf-8")) if os.path.exists(a.cache) else {"types": {}, "done": []}
    types, done = cache["types"], set(cache["done"])

    conn = sqlite3.connect(a.creditdb)
    has_aid, occ, info = set(), defaultdict(set), {}
    for wid, title, year, s in conn.execute("SELECT work_id, title, year, staff_json FROM works WHERE staff_json IS NOT NULL"):
        info[wid] = (title, year)
        try:
            m = json.loads(s)
        except Exception:
            continue
        for lst in m.values():
            if not isinstance(lst, list):
                continue
            for x in lst:
                if isinstance(x, dict):
                    n = (x.get("name") or "").strip()
                    if x.get("aid"):
                        has_aid.add(n)
                    if x.get("src") != "al" and n:
                        occ[n].add(wid)
    cands = {n: ws for n, ws in occ.items() if n not in has_aid and len(ws) >= a.min_works}
    unresolved = {n for n in cands if n not in types}
    work_names = defaultdict(set)
    for n in unresolved:
        for w in cands[n]:
            if w not in done:
                work_names[w].add(n)
    print(f"候補 {len(cands)} 名 / 未判定 {len(unresolved)} 名")

    deadline = time.time() + a.minutes * 60
    n_req = 0
    while unresolved and work_names and time.time() < deadline:
        wid = max(work_names, key=lambda w: len(work_names[w] & unresolved))
        if not work_names[wid] & unresolved:
            break
        names = work_names.pop(wid)
        done.add(wid)
        title, year = info[wid]
        got = person_types_for(title, year)
        n_req += 2
        if got:
            for n, t in got.items():
                types[n] = t
            unresolved -= set(got)
        if n_req % 100 == 0:
            json.dump({"types": types, "done": sorted(done)}, open(a.cache, "w", encoding="utf-8"), ensure_ascii=False)
            print(f"  リクエスト {n_req} / 未判定 {len(unresolved)} 名", flush=True)
    json.dump({"types": types, "done": sorted(done)}, open(a.cache, "w", encoding="utf-8"), ensure_ascii=False)

    companies = sorted(n for n, t in types.items() if t in (2, 3) and n in cands)
    existing = []
    if OUT_PATH.exists():
        existing = [l.strip() for l in OUT_PATH.read_text(encoding="utf-8").splitlines() if l.strip() and not l.startswith("#")]
    merged = sorted(set(existing) | set(companies))
    OUT_PATH.write_text(
        "# スタッフ欄に入っている会社・グループの名前（個人の評価一覧から除外する）。1行に1つ。\n"
        "# tools/seed_company_names.py が Bangumi の人物種別（会社・グループ）から作成。手で追加してよい。\n"
        + "\n".join(merged) + "\n", encoding="utf-8")
    print(f"会社・グループ {len(companies)} 件（合計 {len(merged)} 件）→ {OUT_PATH} / 未判定 {len(unresolved)} 名")


if __name__ == "__main__":
    main()
