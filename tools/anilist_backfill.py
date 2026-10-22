# -*- coding: utf-8 -*-
"""
AniList の話数付きクレジットの初回一括取得（途中で止めても続きから再開できる）。

  python tools/anilist_backfill.py --creditdb creditdb.zip --store anilist_credits.zip \
      --out anilist_credits.zip --minutes 330 [--scope scored|all] [--limit N]

- 対象: --scope scored（既定）= スコアのある作品。新しい年代から順に取得する
- 既に取得済み（media に行がある）作品は飛ばす
- --minutes に達したら途中結果を保存して終了する（GitHub Actions の6時間制限対策）
"""
import argparse
import sqlite3
import sys
import time
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import anilist_credits as ac  # noqa: E402

if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--creditdb", required=True, help="CreditDB の DB (.zip / .db)")
    ap.add_argument("--store", default=None, help="既存の anilist_credits (.zip / .db)。無ければ新規作成")
    ap.add_argument("--out", required=True, help="保存先 anilist_credits.zip")
    ap.add_argument("--minutes", type=float, default=330)
    ap.add_argument("--scope", choices=["scored", "all"], default="scored")
    ap.add_argument("--limit", type=int, default=0, help="試運転用: 処理する作品数の上限")
    args = ap.parse_args()

    deadline = time.time() + args.minutes * 60
    cdb = sqlite3.connect(":memory:")
    p = Path(args.creditdb)
    if p.suffix == ".zip":
        with zipfile.ZipFile(p) as zf:
            cdb.deserialize(zf.read(next(n for n in zf.namelist() if n.endswith(".db"))))
    else:
        cdb.deserialize(p.read_bytes())

    store = ac.open_store(Path(args.store) if args.store else None)
    done = {r[0] for r in store.execute("SELECT work_id FROM media")}
    not_found = {r[0] for r in store.execute("SELECT work_id FROM idmap WHERE anilist_id IS NULL")}

    where = "anilist_raw_score > 0" if args.scope == "scored" else "1=1"
    targets = [r for r in cdb.execute(
        f"SELECT work_id, title, title_en, year FROM works WHERE {where} ORDER BY year DESC, anilist_raw_score DESC")
        if r[0] not in done and r[0] not in not_found]
    total_scope = cdb.execute(f"SELECT COUNT(*) FROM works WHERE {where}").fetchone()[0]
    print(f"対象 {total_scope} 作品 / 取得済み {len(done)} / 見つからず {len(not_found)} / 残り {len(targets)}", flush=True)

    client = ac.AniListClient()
    fetched = skipped = 0
    for i, (work_id, title, title_en, year) in enumerate(targets):
        if time.time() > deadline or (args.limit and fetched + skipped >= args.limit):
            break
        aid = ac.anilist_id_of(work_id)
        if aid is None:
            aid = ac.resolve_anilist_id(client, store, work_id, title, year, title_en)
            if aid is None:
                skipped += 1
                continue
        if ac.fetch_work(client, store, work_id, aid):
            fetched += 1
        if (fetched + skipped) % 50 == 0:
            print(f"  {fetched + skipped}/{len(targets)} 件処理 (取得 {fetched} / AniList に無し {skipped}) "
                  f"リクエスト {client.requests}", flush=True)
            ac.save_store(store, Path(args.out))  # 途中で落ちても進捗を失わない

    ac.save_store(store, Path(args.out))
    done_now = store.execute("SELECT COUNT(*) FROM media").fetchone()[0]
    nf_now = store.execute("SELECT COUNT(*) FROM idmap WHERE anilist_id IS NULL").fetchone()[0]
    remaining = total_scope - done_now - nf_now
    print(f"今回: 取得 {fetched} / AniList に無し {skipped} / リクエスト {client.requests} / 失敗 {client.failures}")
    print(f"累計: 取得済み {done_now} / AniList に無し {nf_now} / 残り {max(0, remaining)}")
    summary = Path(__import__("os").environ.get("GITHUB_STEP_SUMMARY", "")) if __import__("os").environ.get("GITHUB_STEP_SUMMARY") else None
    if summary:
        with open(summary, "a", encoding="utf-8") as f:
            f.write(f"## AniList 話数クレジット 一括取得\n\n| 項目 | 件数 |\n|---|---|\n"
                    f"| 今回取得 | {fetched} |\n| 今回 AniList に無し | {skipped} |\n| リクエスト | {client.requests} |\n"
                    f"| 累計取得済み | {done_now} |\n| 残り | {max(0, remaining)} |\n")
    # 残りがあるかを呼び出し元（ワークフロー）へ伝える
    print(f"REMAINING={max(0, remaining)}")


if __name__ == "__main__":
    main()
