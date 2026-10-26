"""
IMDb の各話評価を CreditDB に取り込む（フェーズ2）。

データ: IMDb Non-Commercial Datasets（https://datasets.imdbws.com/）。非商用の個人利用に限り利用可。
表示する画面には「Information courtesy of IMDb (https://www.imdb.com). Used with permission.」を出す。

作品と IMDb シリーズの対応づけ（優先順）:
  1. 手動の対応表 tools/imdb_map_overrides.csv
  2. Wikidata（AniList ID → IMDb ID）
  3. IMDb のタイトル（原題・別名）と開始年の一致（候補が1つに決まるときだけ）
シーズンは「放送開始年」と「話数」が合うものだけを採用し、曖昧なら使わない。

単体実行（確認用）:
  python tools/imdb_episodes.py --creditdb creditdb.db --imdb-dir imdb/ [--report out.json]
"""
import argparse
import csv
import gzip
import json
import os
import re
import sys
import time
import unicodedata
import urllib.parse
import urllib.request
from collections import defaultdict

IMDB_BASE = "https://datasets.imdbws.com/"
IMDB_FILES = ["title.basics.tsv.gz", "title.episode.tsv.gz", "title.ratings.tsv.gz", "title.akas.tsv.gz"]
WIKIDATA_URL = "https://query.wikidata.org/sparql"
WIKIDATA_QUERY = """
SELECT ?al ?imdb WHERE {
  ?item wdt:P8729 ?al .
  ?item wdt:P345 ?imdb .
  FILTER(STRSTARTS(?imdb, "tt"))
}"""
USER_AGENT = "CreditDB/1.0 (non-commercial anime credit database; https://github.com/nanbeimerco/CreditDB-Pro-Mobile)"
OVERRIDES_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "imdb_map_overrides.csv")
SERIES_TYPES = {"tvSeries", "tvMiniSeries"}
MIN_EPISODES = 4  # これより話数の少ない作品（劇場版・OVA 1〜3話など）は対象外

csv.field_size_limit(10_000_000)


# ---------------------------------------------------------------- 取得

def download_imdb(dest_dir: str, retries: int = 5):
    os.makedirs(dest_dir, exist_ok=True)
    for name in IMDB_FILES:
        path = os.path.join(dest_dir, name)
        if os.path.exists(path) and time.time() - os.path.getmtime(path) < 6 * 86400:
            continue
        for attempt in range(retries):
            try:
                req = urllib.request.Request(IMDB_BASE + name, headers={"User-Agent": USER_AGENT})
                with urllib.request.urlopen(req, timeout=120) as r, open(path + ".part", "wb") as f:
                    while True:
                        chunk = r.read(1 << 20)
                        if not chunk:
                            break
                        f.write(chunk)
                os.replace(path + ".part", path)
                print(f"  IMDb: {name} {os.path.getsize(path) / 1048576:.0f}MB")
                break
            except Exception as e:  # 接続が切れることがあるので数回やり直す
                print(f"  IMDb: {name} の取得に失敗（{attempt + 1}回目）: {e}")
                time.sleep(5 * (attempt + 1))
        else:
            raise RuntimeError(f"IMDb のデータ {name} を取得できませんでした")


def fetch_wikidata_map(retries: int = 3) -> dict:
    """AniList ID → {IMDb ID}"""
    url = WIKIDATA_URL + "?" + urllib.parse.urlencode({"query": WIKIDATA_QUERY, "format": "json"})
    for attempt in range(retries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT, "Accept": "application/sparql-results+json"})
            with urllib.request.urlopen(req, timeout=180) as r:
                data = json.load(r)
            out = defaultdict(set)
            for b in data["results"]["bindings"]:
                try:
                    out[int(b["al"]["value"])].add(b["imdb"]["value"])
                except (ValueError, KeyError):
                    continue
            return out
        except Exception as e:
            print(f"  Wikidata の取得に失敗（{attempt + 1}回目）: {e}")
            time.sleep(10 * (attempt + 1))
    raise RuntimeError("Wikidata から AniList と IMDb の対応を取得できませんでした")


def _tsv(path):
    with gzip.open(path, "rt", encoding="utf-8", newline="") as f:
        reader = csv.reader(f, delimiter="\t", quoting=csv.QUOTE_NONE)
        next(reader)
        yield from reader


def _int(v):
    return None if v in ("\\N", "") else int(v)


# ---------------------------------------------------------------- タイトル照合

def norm_title(s: str) -> str:
    s = unicodedata.normalize("NFKC", s or "").lower()
    s = re.sub(r"[\s\-‐―−~〜～!！?？:：;；,，.。・･'\"“”’「」『』【】()（）\[\]☆★♪♡&＆/／]", "", s)
    return s


def load_series(imdb_dir: str) -> dict:
    """アニメの TV シリーズ: tconst → {title, original, year, type}"""
    series = {}
    for row in _tsv(os.path.join(imdb_dir, "title.basics.tsv.gz")):
        tconst, ttype, primary, original, _adult, start, _end, _rt, genres = row[:9]
        if ttype in SERIES_TYPES and "Animation" in genres:
            series[tconst] = {"title": primary, "original": original, "year": _int(start), "type": ttype}
    return series


def load_episodes(imdb_dir: str, parents: set) -> dict:
    """シリーズ → [(episode_tconst, season, episode)]"""
    eps = defaultdict(list)
    for tconst, parent, season, episode in _tsv(os.path.join(imdb_dir, "title.episode.tsv.gz")):
        if parent in parents:
            eps[parent].append((tconst, _int(season), _int(episode)))
    return eps


def load_episode_years(imdb_dir: str, wanted: set) -> dict:
    years = {}
    for row in _tsv(os.path.join(imdb_dir, "title.basics.tsv.gz")):
        if row[0] in wanted:
            years[row[0]] = _int(row[5])
    return years


def load_ratings(imdb_dir: str, wanted: set) -> dict:
    out = {}
    for tconst, rating, votes in _tsv(os.path.join(imdb_dir, "title.ratings.tsv.gz")):
        if tconst in wanted:
            out[tconst] = (float(rating), int(votes))
    return out


def load_title_index(imdb_dir: str, series: dict) -> dict:
    """正規化タイトル → {tconst}（原題・別名すべて）"""
    index = defaultdict(set)
    for t, info in series.items():
        for name in (info["title"], info["original"]):
            k = norm_title(name)
            if k:
                index[k].add(t)
    path = os.path.join(imdb_dir, "title.akas.tsv.gz")
    if os.path.exists(path):
        for row in _tsv(path):
            if row[0] in series:
                k = norm_title(row[2])
                if k:
                    index[k].add(row[0])
    return index


# ---------------------------------------------------------------- シーズンの決定

def load_overrides(path: str = OVERRIDES_PATH) -> dict:
    """work_id → (imdb_id, season, offset) / None（除外）"""
    out = {}
    if not os.path.exists(path):
        return out
    with open(path, encoding="utf-8") as f:
        for row in csv.DictReader(line for line in f if not line.startswith("#")):
            wid = (row.get("work_id") or "").strip()
            if not wid:
                continue
            imdb_id = (row.get("imdb_id") or "").strip()
            if imdb_id in ("", "exclude"):
                out[wid] = None
            else:
                season = (row.get("season") or "").strip()
                offset = (row.get("offset") or "").strip()
                out[wid] = (imdb_id, int(season) if season else None, int(offset) if offset else 0)
    return out


def _season_blocks(episodes, years):
    """シーズン番号 → 話の一覧（話数順）。話数の欠けた行は除く"""
    by_season = defaultdict(list)
    for tconst, season, number in episodes:
        if season is None or number is None:
            continue
        by_season[season].append((number, tconst, years.get(tconst)))
    for s in by_season:
        by_season[s].sort()
    return by_season


def _first_year(block):
    ys = [y for _n, _t, y in block if y]
    return min(ys) if ys else None


def _count_ok(n_imdb: int, n_work) -> bool:
    if not n_work:
        return True
    return abs(n_imdb - n_work) <= max(2, round(n_work * 0.2))


def assign_seasons(works_by_series: dict, episodes: dict, years: dict) -> dict:
    """
    IMDb シリーズごとに、対応する作品（1つ以上）へシーズンを割り当てる。
    戻り値: work_id → (imdb_id, [(作品内の話数, episode_tconst)], method)
    """
    result = {}
    for imdb_id, works in works_by_series.items():
        blocks = _season_blocks(episodes.get(imdb_id, []), years)
        if not blocks:
            continue
        works = sorted(works, key=lambda w: (w["year"], w["work_id"]))

        # A. 開始年と話数が合うシーズンが1つだけなら採用
        claimed = defaultdict(list)
        for w in works:
            cands = [s for s, b in blocks.items() if s > 0 and _first_year(b) == w["year"] and _count_ok(len(b), w["episodes"])]
            if len(cands) == 1:
                claimed[cands[0]].append(w)
        for s, ws in claimed.items():
            if len(ws) != 1:
                continue  # 同じシーズンを複数の作品が取り合う場合は使わない
            block = blocks[s]
            base = block[0][0] - 1 if block[0][0] > 1 else 0  # シーズン2が13話から始まる場合など
            result[ws[0]["work_id"]] = (imdb_id, [(n - base, t) for n, t, _y in block], "season")

        # B. 1つのシーズンに複数の作品（分割2クールなど）がまとまっている場合: 放送順に話数で区切る
        rest = [w for w in works if w["work_id"] not in result]
        if len(rest) >= 2 and len(blocks) == 1 and all(w["episodes"] for w in rest):
            block = next(iter(blocks.values()))
            total = sum(w["episodes"] for w in rest)
            if abs(len(block) - total) <= 1:
                pos, ok, parts = 0, True, []
                for w in rest:
                    seg = block[pos:pos + w["episodes"]]
                    if not seg or (_first_year(seg) and abs(_first_year(seg) - w["year"]) > 0):
                        ok = False
                        break
                    parts.append((w, seg))
                    pos += w["episodes"]
                if ok:
                    for w, seg in parts:
                        result[w["work_id"]] = (imdb_id, [(i + 1, t) for i, (_n, t, _y) in enumerate(seg)], "split")
    return result


# ---------------------------------------------------------------- 全体

def build_episode_scores(conn, imdb_dir: str, wikidata_map: dict = None, overrides: dict = None, log=print) -> dict:
    """
    episode_scores / imdb_map テーブルを作り直す。戻り値は集計（レポート用）。
    conn: CreditDB（works テーブルを持つ）
    """
    t0 = time.time()
    if wikidata_map is None:
        wikidata_map = fetch_wikidata_map()
    if overrides is None:
        overrides = load_overrides()
    log(f"  Wikidata: AniList→IMDb {len(wikidata_map)}件")

    has_eps_col = "episodes" in [r[1] for r in conn.execute("PRAGMA table_info(works)")]
    rows = conn.execute(f"SELECT work_id, title, title_en, year, {'episodes' if has_eps_col else 'NULL'} FROM works").fetchall()
    works = []
    for wid, title, title_en, year, n_eps in rows:
        m = re.match(r"^(?:anime|anilist)_(\d+)$", wid)
        works.append({"work_id": wid, "aid": int(m.group(1)) if m else None, "title": title, "title_en": title_en,
                      "year": year, "episodes": n_eps})

    series = load_series(imdb_dir)
    log(f"  IMDb: アニメの TV シリーズ {len(series)}件（{time.time() - t0:.0f}秒）")

    # 作品 → IMDb シリーズ
    work_series, method_of = {}, {}
    pending_title = []
    for w in works:
        if w["work_id"] in overrides:
            o = overrides[w["work_id"]]
            if o is not None:
                work_series[w["work_id"]] = o[0]
                method_of[w["work_id"]] = "manual"
            continue
        if w["episodes"] is not None and w["episodes"] < MIN_EPISODES:
            continue
        cands = {t for t in wikidata_map.get(w["aid"], ()) if t in series}
        if len(cands) == 1:
            work_series[w["work_id"]] = cands.pop()
            method_of[w["work_id"]] = "wikidata"
        elif not cands:
            pending_title.append(w)

    title_index = load_title_index(imdb_dir, series)
    log(f"  IMDb: タイトル索引 {len(title_index)}件（{time.time() - t0:.0f}秒）")
    for w in pending_title:
        cands = set()
        for name in (w["title"], w["title_en"]):
            k = norm_title(name)
            if k:
                cands |= {t for t in title_index.get(k, ()) if series[t]["year"] == w["year"]}
        if len(cands) == 1:
            work_series[w["work_id"]] = cands.pop()
            method_of[w["work_id"]] = "title"

    parents = set(work_series.values())
    episodes = load_episodes(imdb_dir, parents)
    ep_ids = {t for lst in episodes.values() for t, _s, _n in lst}
    years = load_episode_years(imdb_dir, ep_ids)
    ratings = load_ratings(imdb_dir, ep_ids)
    log(f"  IMDb: 各話 {len(ep_ids)}件・評価あり {len(ratings)}件（{time.time() - t0:.0f}秒）")

    by_id = {w["work_id"]: w for w in works}
    works_by_series = defaultdict(list)
    manual = {}
    for wid, imdb_id in work_series.items():
        o = overrides.get(wid)
        if o and o[1] is not None:
            manual[wid] = o
        else:
            works_by_series[imdb_id].append(by_id[wid])
    assigned = assign_seasons(works_by_series, episodes, years)
    for wid, (imdb_id, season, offset) in manual.items():
        block = _season_blocks(episodes.get(imdb_id, []), years).get(season, [])
        assigned[wid] = (imdb_id, [(n - offset, t) for n, t, _y in block if n - offset >= 1], "manual")

    conn.execute("DROP TABLE IF EXISTS episode_scores")
    conn.execute("DROP TABLE IF EXISTS imdb_map")
    conn.execute("""CREATE TABLE episode_scores (
        work_id TEXT NOT NULL, episode INTEGER NOT NULL, source TEXT NOT NULL,
        rating REAL NOT NULL, votes INTEGER NOT NULL, ext_id TEXT,
        PRIMARY KEY (work_id, episode, source))""")
    conn.execute("""CREATE TABLE imdb_map (
        work_id TEXT PRIMARY KEY, imdb_id TEXT NOT NULL, method TEXT NOT NULL,
        episodes INTEGER NOT NULL, rated INTEGER NOT NULL)""")
    n_rows, n_works, by_method = 0, 0, defaultdict(int)
    for wid, (imdb_id, eps, how) in assigned.items():
        rated = [(n, t, *ratings[t]) for n, t in eps if t in ratings]
        if not rated:
            continue
        method = method_of.get(wid, "manual") + ("" if how == "season" else "+" + how)
        conn.execute("INSERT INTO imdb_map VALUES (?,?,?,?,?)", (wid, imdb_id, method, len(eps), len(rated)))
        conn.executemany("INSERT OR REPLACE INTO episode_scores VALUES (?,?,'imdb',?,?,?)",
                         [(wid, n, r, v, t) for n, t, r, v in rated])
        n_rows += len(rated)
        n_works += 1
        by_method[method] += 1
    stats = {
        "works_with_episode_scores": n_works,
        "episode_scores": n_rows,
        "series_candidates": len(parents),
        "by_method": dict(by_method),
        "seconds": round(time.time() - t0),
    }
    log(f"  IMDb: 各話スコアのある作品 {n_works}件・{n_rows}話 {dict(by_method)}")
    return stats


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--creditdb", required=True, help="creditdb.db（展開済み）")
    ap.add_argument("--imdb-dir", required=True)
    ap.add_argument("--download", action="store_true")
    ap.add_argument("--report")
    a = ap.parse_args()
    import sqlite3
    if a.download:
        download_imdb(a.imdb_dir)
    conn = sqlite3.connect(a.creditdb)
    stats = build_episode_scores(conn, a.imdb_dir)
    conn.commit()
    if a.report:
        with open(a.report, "w", encoding="utf-8") as f:
            json.dump(stats, f, ensure_ascii=False, indent=2)


if __name__ == "__main__":
    sys.exit(main())
