# -*- coding: utf-8 -*-
"""
AniList の話数付きスタッフクレジットを取得・保存し、CreditDB の staff_json に反映する。

保存先は配布用 DB とは別の SQLite（anilist_credits.db）。GitHub Release の
"anilist-credits" タグに anilist_credits.zip として置き、初回一括取得
（tools/anilist_backfill.py）と週次更新（tools/weekly_updater.py）で共有する。

テーブル:
  media    : 作品ごとの取得状態（AniList ID・形式・話数・取得日時）
  credits  : 取得したクレジット（役職・話数・OP/ED 等）
  idmap    : Bangumi ID の作品 → AniList ID の対応（見つからなければ anilist_id = NULL）
"""
import json
import re
import sqlite3
import time
import unicodedata
import urllib.error
import urllib.request
import zipfile
from pathlib import Path

ANILIST_ENDPOINT = "https://graphql.anilist.co"
REQUEST_INTERVAL_SEC = 2.1  # AniList の制限（劣化時 30回/分）を超えない間隔

# AniList の役職名 → CreditDB の役職キー（Bangumi 側の RELATION_MAP と同じ分類）
ROLE_MAP = {
    "Director": "director",
    "Chief Director": "director",
    "Assistant Director": "unit_director",
    "Series Composition": "series_comp",
    "Script": "series_comp",
    "Character Design": "char_design",
    "Chief Animation Director": "sakkan",
    "Animation Director": "sakkan",
    "Assistant Animation Director": "sakkan",
    "Action Animation Director": "sakkan",
    "Effects Animation Director": "sakkan",
    "Mechanical Animation Director": "sakkan",
    "Key Animation": "genga",
    "2nd Key Animation": "genga_2nd",
    "Episode Director": "unit_director",
    "Assistant Episode Director": "unit_director",
    "Storyboard": "unit_director",
    "Music": "music",
    "Art Director": "art_dir",
    "Art Design": "art_dir",
    "Background Art": "art_dir",
}

# 話数で参加度が変わる役職（監督・シリーズ構成などは作品全体に関わるので対象外）
EPISODE_ROLES = {"unit_director", "sakkan", "genga", "genga_2nd"}

STAFF_QUERY = """
query ($id: Int, $page: Int) {
  Media(id: $id, type: ANIME) {
    id format episodes
    staff(page: $page, perPage: 50) {
      pageInfo { hasNextPage }
      edges { role node { id name { native full } } }
    }
  }
}
"""

SEARCH_QUERY = """
query ($search: String) {
  Page(page: 1, perPage: 8) {
    media(search: $search, type: ANIME) {
      id format episodes seasonYear startDate { year }
      title { native romaji english }
      synonyms
    }
  }
}
"""

SCHEMA = """
CREATE TABLE IF NOT EXISTS media (
  work_id TEXT PRIMARY KEY,
  anilist_id INTEGER,
  format TEXT,
  episodes INTEGER,
  credit_count INTEGER,
  fetched_at TEXT
);
CREATE TABLE IF NOT EXISTS credits (
  work_id TEXT NOT NULL,
  anilist_staff_id INTEGER,
  name TEXT NOT NULL,
  role_key TEXT NOT NULL,
  raw_role TEXT,
  eps TEXT,
  parts TEXT
);
CREATE INDEX IF NOT EXISTS idx_credits_work ON credits(work_id);
CREATE TABLE IF NOT EXISTS idmap (
  work_id TEXT PRIMARY KEY,
  anilist_id INTEGER,
  method TEXT,
  tried_at TEXT
);
"""


# ---------------------------------------------------------------- 保存先 DB

def open_store(path: Path) -> sqlite3.Connection:
    """anilist_credits.db（.zip なら展開して）をメモリ上に開く。無ければ空で作る"""
    conn = sqlite3.connect(":memory:")
    if path and path.exists():
        if path.suffix == ".zip":
            with zipfile.ZipFile(path) as zf:
                conn.deserialize(zf.read("anilist_credits.db"))
        else:
            conn.deserialize(path.read_bytes())
    conn.executescript(SCHEMA)
    return conn


def save_store(conn: sqlite3.Connection, zip_path: Path) -> None:
    conn.commit()
    zip_path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(zip_path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as zf:
        zf.writestr("anilist_credits.db", conn.serialize())


# ---------------------------------------------------------------- 役職・話数の解析

_EP_TOKEN = re.compile(r"^(?:eps?\.?\s*)?(\d+)(?:\s*-\s*(\d+))?$", re.I)
_PART_TOKEN = re.compile(r"^(OP|ED|PV|CM|ending|opening)\s*\d*$", re.I)


def parse_role(role: str):
    """
    "Key Animation (eps 13, 18, 22)" → ("Key Animation", [13, 18, 22], [])
    "Key Animation (OP1, OP2, eps 1, 3)" → ("Key Animation", [1, 3], ["OP1", "OP2"])
    括弧が無い・話数が無い場合は ([], [])
    """
    role = (role or "").strip()
    m = re.match(r"^(.*?)\s*\((.*)\)\s*$", role)
    if not m:
        return role, [], []
    base, inner = m.group(1).strip(), m.group(2)
    eps, parts = [], []
    in_eps = False
    for tok in re.split(r"[,;、]", inner):
        tok = tok.strip()
        if not tok:
            continue
        low = tok.lower()
        if low.startswith("ep"):
            in_eps = True
        em = _EP_TOKEN.match(tok)
        if em and (in_eps or low.startswith("ep")):
            a = int(em.group(1))
            b = int(em.group(2)) if em.group(2) else a
            if 0 < a <= b <= a + 500:
                eps.extend(range(a, b + 1))
            continue
        if _PART_TOKEN.match(tok):
            parts.append(tok.upper().replace(" ", ""))
            in_eps = False
            continue
        in_eps = False
    return base, sorted(set(eps)), parts


# ---------------------------------------------------------------- AniList 通信

class AniListClient:
    def __init__(self, interval: float = REQUEST_INTERVAL_SEC):
        self.interval = interval
        self.requests = 0
        self.failures = 0
        self._last = 0.0

    def query(self, q: str, variables: dict):
        payload = json.dumps({"query": q, "variables": variables}).encode("utf-8")
        for _ in range(5):
            wait = self.interval - (time.time() - self._last)
            if wait > 0:
                time.sleep(wait)
            self._last = time.time()
            self.requests += 1
            req = urllib.request.Request(
                ANILIST_ENDPOINT, data=payload, method="POST",
                headers={"Content-Type": "application/json", "Accept": "application/json",
                         "User-Agent": "Mozilla/5.0 (CreditDB-Updater; +https://github.com/nanbeimerco/CreditDB-Pro-Mobile)"})
            try:
                with urllib.request.urlopen(req, timeout=30) as res:
                    return json.loads(res.read().decode("utf-8"))
            except urllib.error.HTTPError as e:
                if e.code == 429:
                    time.sleep(int(e.headers.get("Retry-After", 60)) + 1)
                    continue
                if e.code == 404:
                    return None
                time.sleep(5)
            except Exception:
                time.sleep(5)
        self.failures += 1
        return None

    def fetch_staff(self, anilist_id: int, max_pages: int = 60):
        """作品のスタッフを全ページ取得する。戻り値: (format, episodes, [(staff_id, name, raw_role)]) / 失敗時 None"""
        edges = []
        fmt = eps = None
        for page in range(1, max_pages + 1):
            res = self.query(STAFF_QUERY, {"id": anilist_id, "page": page})
            media = ((res or {}).get("data") or {}).get("Media")
            if not media:
                return None if page == 1 else (fmt, eps, edges)
            fmt, eps = media.get("format"), media.get("episodes")
            for e in media["staff"]["edges"]:
                node = e.get("node") or {}
                name = (node.get("name") or {}).get("native") or (node.get("name") or {}).get("full")
                if name:
                    edges.append((node.get("id"), name.strip(), e.get("role") or ""))
            if not media["staff"]["pageInfo"]["hasNextPage"]:
                break
        return fmt, eps, edges

    def search(self, title: str):
        res = self.query(SEARCH_QUERY, {"search": title})
        return (((res or {}).get("data") or {}).get("Page") or {}).get("media") or []


# ---------------------------------------------------------------- 取得・保存

def store_credits(conn, work_id: str, anilist_id: int, fmt, episodes, edges) -> int:
    conn.execute("DELETE FROM credits WHERE work_id = ?", (work_id,))
    rows = []
    for staff_id, name, raw in edges:
        base, eps, parts = parse_role(raw)
        key = ROLE_MAP.get(base)
        if not key:
            continue
        rows.append((work_id, staff_id, name, key, raw,
                     json.dumps(eps) if eps else None, json.dumps(parts) if parts else None))
    conn.executemany("INSERT INTO credits VALUES (?, ?, ?, ?, ?, ?, ?)", rows)
    conn.execute("INSERT OR REPLACE INTO media VALUES (?, ?, ?, ?, ?, datetime('now'))",
                 (work_id, anilist_id, fmt, episodes, len(rows)))
    return len(rows)


def fetch_work(client: AniListClient, conn, work_id: str, anilist_id: int) -> bool:
    got = client.fetch_staff(anilist_id)
    if got is None:
        return False
    fmt, episodes, edges = got
    store_credits(conn, work_id, anilist_id, fmt, episodes, edges)
    conn.commit()
    return True


def _norm_title(t: str) -> str:
    t = unicodedata.normalize("NFKC", t or "").lower()
    return re.sub(r"[\s\-_・:：~〜\.\,\!\?！？\(\)（）「」『』【】\[\]/／★☆＆&'’\"]", "", t)


def resolve_anilist_id(client: AniListClient, conn, work_id: str, title: str, year: int, title_en: str = None):
    """Bangumi ID の作品について、タイトルと年が一致する AniList 作品を探す（結果は idmap に保存）"""
    row = conn.execute("SELECT anilist_id FROM idmap WHERE work_id = ?", (work_id,)).fetchone()
    if row:
        return row[0]
    target = {_norm_title(title)}
    if title_en:
        target.add(_norm_title(title_en))
    found = None
    for m in client.search(title):
        names = [m["title"].get("native"), m["title"].get("romaji"), m["title"].get("english")] + (m.get("synonyms") or [])
        y = m.get("seasonYear") or (m.get("startDate") or {}).get("year")
        if any(n and _norm_title(n) in target for n in names) and (not year or not y or abs(int(y) - int(year)) <= 1):
            found = m["id"]
            break
    conn.execute("INSERT OR REPLACE INTO idmap VALUES (?, ?, ?, datetime('now'))",
                 (work_id, found, "title+year" if found else "not_found"))
    conn.commit()
    return found


def anilist_id_of(work_id: str):
    """CreditDB の work_id が AniList ID 由来ならその ID を返す"""
    for prefix in ("anime_", "anilist_"):
        if work_id.startswith(prefix) and not work_id.startswith("anime_bgm_"):
            try:
                return int(work_id[len(prefix):])
            except ValueError:
                return None
    return None


# ---------------------------------------------------------------- CreditDB への反映

_ITAIJI = str.maketrans({"髙": "高", "﨑": "崎", "濵": "浜", "邊": "辺", "邉": "辺", "齋": "斎", "齊": "斉", "德": "徳"})


def name_key(name: str) -> str:
    n = unicodedata.normalize("NFKC", name or "").translate(_ITAIJI)
    return re.sub(r"[\s・･=＝]", "", n)


def apply_to_creditdb(credit_conn, ac_conn) -> dict:
    """
    AniList のクレジットを CreditDB の staff_json に反映する。
    - 同じ役職に同じ人（名前を正規化して比較）がいれば、話数（eps）・OP/ED（parts）・AniList ID（aid）を付ける
    - いなければ AniList 由来の人として追加する（src = "al"）
    - works.episodes に AniList の話数を入れる
    """
    cols = {r[1] for r in credit_conn.execute("PRAGMA table_info(works)")}
    if "episodes" not in cols:
        credit_conn.execute("ALTER TABLE works ADD COLUMN episodes INTEGER")

    by_work = {}
    for work_id, staff_id, name, role_key, eps, parts in ac_conn.execute(
            "SELECT work_id, anilist_staff_id, name, role_key, eps, parts FROM credits"):
        by_work.setdefault(work_id, []).append((staff_id, name, role_key, eps, parts))
    episodes = {w: e for w, e in ac_conn.execute("SELECT work_id, episodes FROM media WHERE episodes IS NOT NULL")}

    stats = {"works": 0, "attached": 0, "added": 0}
    updates = []
    for work_id, s_json in credit_conn.execute("SELECT work_id, staff_json FROM works").fetchall():
        creds = by_work.get(work_id)
        if not creds:
            continue
        try:
            s_map = json.loads(s_json or "{}")
        except Exception:
            continue
        if not isinstance(s_map, dict):
            continue
        for staff_id, name, role_key, eps, parts in creds:
            lst = s_map.setdefault(role_key, [])
            if not isinstance(lst, list):
                continue
            k = name_key(name)
            hit = next((p for p in lst if isinstance(p, dict) and name_key(p.get("name", "")) == k), None)
            eps_l = json.loads(eps) if eps else []
            parts_l = json.loads(parts) if parts else []
            if hit is None:
                hit = {"name": name, "rt": "B", "ct": "B", "src": "al"}
                if role_key == "genga_2nd":
                    hit["w"] = 0.2
                lst.append(hit)
                stats["added"] += 1
            else:
                stats["attached"] += 1
            if eps_l:
                hit["eps"] = sorted(set(hit.get("eps", [])) | set(eps_l))
            if parts_l:
                hit["parts"] = sorted(set(hit.get("parts", [])) | set(parts_l))
            if staff_id:
                hit["aid"] = staff_id
        updates.append((json.dumps(s_map, ensure_ascii=False), episodes.get(work_id), work_id))
        stats["works"] += 1
    credit_conn.executemany("UPDATE works SET staff_json = ?, episodes = COALESCE(?, episodes) WHERE work_id = ?", updates)
    credit_conn.commit()
    return stats
