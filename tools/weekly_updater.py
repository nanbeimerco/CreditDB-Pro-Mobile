# -*- coding: utf-8 -*-
"""
Weekly CreditDB Auto-Updater v3.0 (AniList GraphQL & Bangumi API)
Runs weekly via GitHub Actions in CreditDB-Pro-Mobile (.github/workflows/weekly_data_update.yml):
1. Load the previous creditdb.zip (downloaded from the latest data release) into SQLite memory
2. Fetch seasonal anime (WINTER, SPRING, SUMMER, FALL) and currently RELEASING anime from AniList API
3. Always fetch currently airing anime from Bangumi Calendar API (/calendar) and merge via Smart Title Matching
4. Enrich unevaluated works in DB via direct AniList search
5. Dynamically refresh staff & cast credits via Bangumi API for all airing/recent/under-staffed works (capturing weekly episode staff & score changes)
6. Recompute Era-adjusted Z-scores, Bayesian ratings S(a) (excluding Z=0 works), leaderboards, profiles, and resolve staff tiers (rt, ct) inside works
7. Write creditdb.zip (with work_covers table) and version.json to --out-dir for the next release
"""

import os
import sys
import gzip
import json
import time
import hashlib
import math
import statistics
import sqlite3
import datetime
import unicodedata
import zipfile
import re
import urllib.parse
import urllib.request
from pathlib import Path
from collections import defaultdict

sys.path.insert(0, str(Path(__file__).resolve().parent))  # tools/anilist_credits.py を読み込むため

# Ensure UTF-8 output across Windows and CI environments
if sys.platform == "win32":
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8", errors="replace")

ANILIST_ENDPOINT = "https://graphql.anilist.co"
BANGUMI_ENDPOINT = "https://api.bgm.tv"
USER_AGENT = "CreditDB-Updater/3.1 (https://github.com/nanbeimerco/CreditDB-Pro-Mobile)"

DEFAULT_ROLE_M = {
    "director": 2.0,
    "series_comp": 2.0,
    "char_design": 2.0,
    "music": 2.0,
    "unit_director": 4.0,
    "sakkan": 4.0,
    "art_dir": 4.0,
    "genga": 6.0,
    "genga_2nd": 6.0,
    "cv": 6.0,
    "all": 6.0,
}

RELATION_MAP = {
    "导演": "director",
    "总导演": "director",
    "副导演": "director",
    "系列构成": "series_comp",
    "脚本": "series_comp",
    "人物设定": "char_design",
    "角色设计": "char_design",
    "副人物设定": "char_design",
    "总作画监督": "sakkan",
    "作画监督": "sakkan",
    "作画监督助理": "sakkan",
    "总作画监督助理": "sakkan",
    "特效作画监督": "sakkan",
    "动作作画监督": "sakkan",
    "原画": "genga",
    "第二原画": "genga_2nd",
    "主动画师": "genga",
    "演出": "unit_director",
    "演出助理": "unit_director",
    "助监督": "unit_director",
    "OP・ED 演出": "unit_director",
    "分镜": "unit_director",
    "OP・ED 分镜": "unit_director",
    "分镜协力": "unit_director",
    "音乐": "music",
    "美术监督": "art_dir",
    "美术设计": "art_dir",
    "背景美术": "art_dir",
    "动画制作": "studio",
}

def norm(t):
    if not t:
        return ""
    t = unicodedata.normalize("NFKC", str(t)).lower()
    return re.sub(r'[\s\-_・:：~〜\.\,\!\?！？\(\)（）「」『』【】〈〉\[\]/／★☆＆&]', '', t)

def normalize_for_match(t: str) -> str:
    """
    Normalizes anime titles for smart cross-platform matching between AniList and Bangumi.
    Unifies season markers (Season 2, 2nd Season, 第2期, II -> s2) and strips years/punctuation.
    """
    if not t:
        return ""
    s = unicodedata.normalize("NFKC", str(t)).lower()
    # Remove broadcast year suffixes like (2024), (2026)
    s = re.sub(r'\(20\d\d\)', ' ', s)
    # Convert Kanji numbers in 第X期 / 第Xクール
    kanji_map = {'一': '1', '二': '2', '三': '3', '四': '4', '五': '5', '六': '6', '七': '7', '八': '8', '九': '9'}
    s = re.sub(r'第([一二三四五六七八九])(?:期|クール|章|部)', lambda m: f" s{kanji_map[m.group(1)]} ", s)
    # Convert Season X, 第X期, Part X
    s = re.sub(r'(?:season\s*|第\s*|part\s*|パート\s*)([0-9]+)(?:\s*期|\s*シーズン|\s*クール|\s*章|\s*部)?', r' s\1 ', s)
    s = re.sub(r'([0-9]+)(?:st|nd|rd|th)\s*(?:season|シーズン|クール)', r' s\1 ', s)
    s = re.sub(r'([0-9]+)\s*(?:期|クール)', r' s\1 ', s)
    # Convert trailing or standalone Roman numerals (II, III, IV)
    s = re.sub(r'(?:(?<=\s)|(?<=[^\x00-\x7f]))iii(?=\s|$)', ' s3 ', s)
    s = re.sub(r'(?:(?<=\s)|(?<=[^\x00-\x7f]))ii(?=\s|$)', ' s2 ', s)
    s = re.sub(r'(?:(?<=\s)|(?<=[^\x00-\x7f]))iv(?=\s|$)', ' s4 ', s)
    # Strip punctuation and spaces
    s = re.sub(r'[\s\-_・:：~〜\.\,\!\?！？\(\)（）「」『』【】〈〉\[\]/／★☆＆&]', '', s)
    return s

def extract_main_title_norm(t: str) -> str:
    """
    Extracts the main title before long subtitles (e.g. '～...～', '-...-', or space-separated light novel subtitles)
    while preserving any season marker.
    """
    if not t:
        return ""
    s = unicodedata.normalize("NFKC", str(t))
    # Detect season token from full title first
    full_norm = normalize_for_match(s)
    season_match = re.search(r'(s[2-9])$', full_norm)
    season_suffix = season_match.group(1) if season_match else ""

    # Strip ~subtitle~
    s_base = re.sub(r'[~〜].*?[~〜]', '', s).strip()
    # If there is a space and the first part is at least 5 chars, take the first part
    parts = re.split(r'[\s　]+', s_base)
    if len(parts) > 1 and len(parts[0]) >= 5:
        # Check if parts[1] is not just a season marker
        if not re.match(r'^(?:第?\d|season|part|ii|iii|iv|\d+(?:st|nd|rd|th))', parts[1].lower()):
            s_base = parts[0]

    base_norm = normalize_for_match(s_base)
    if season_suffix and not base_norm.endswith(season_suffix):
        base_norm += season_suffix
    return base_norm

def calculate_tier(rank: int, total: int) -> str:
    if total <= 0 or rank <= 0:
        return "-"
    pct = (rank - 1) / max(1, total)
    if pct <= 0.023:
        return "S+"
    elif pct <= 0.067:
        return "S"
    elif pct <= 0.159:
        return "A+"
    elif pct <= 0.309:
        return "A"
    elif pct <= 0.500:
        return "B+"
    elif pct <= 0.700:
        return "B"
    elif pct <= 0.850:
        return "C"
    else:
        return "D"

def count_total_staff(staff_json_str: str) -> int:
    if not staff_json_str:
        return 0
    try:
        d = json.loads(staff_json_str)
        if isinstance(d, dict):
            return sum(len(v) for v in d.values() if isinstance(v, list))
    except:
        pass
    return 0

EXCLUDED_STAFF_ROLES = {"color_design", "photo_dir", "sound_dir"}

# 話数で参加度が変わる役職と、話数データが足りないときの「1話あたりの人数」の既定値
EPISODE_ROLES = {"unit_director", "sakkan", "genga", "genga_2nd"}
DEFAULT_PEOPLE_PER_EPISODE = {"unit_director": 2.0, "sakkan": 3.0, "genga": 12.0, "genga_2nd": 10.0}
# Old data stores 2nd key animation under this key; it is folded into genga_2nd.
LEGACY_GENGA_2ND_KEYS = ("genga_second",)

# API health counters, checked at the end of the run (see check_api_health)
STATS = defaultdict(int)

def sanitize_staff_dict(s_map: dict) -> tuple[dict, bool]:
    """
    Merges 'storyboard' into 'unit_director' without duplicate staff names,
    separates 2nd Key Animation (w <= 0.25) into 'genga_2nd' vs 1st Key Animation in 'genga',
    excludes 'color_design', 'photo_dir', 'sound_dir', and deduplicates entries per role.
    Returns (cleaned_dict, changed_flag).
    """
    if not isinstance(s_map, dict):
        return {}, False

    changed = False
    cleaned = {}

    # 1. Copy all non-genga roles (excluding storyboard & EXCLUDED_STAFF_ROLES) with deduplication
    for role, persons in s_map.items():
        if role in EXCLUDED_STAFF_ROLES or role == "storyboard":
            changed = True
            continue
        if role in ("genga", "genga_2nd") or role in LEGACY_GENGA_2ND_KEYS:
            continue
        if isinstance(persons, list):
            dedup_list = []
            seen_names = set()
            for p in persons:
                p_name = (p.get("name") if isinstance(p, dict) else str(p) or "").strip()
                if p_name:
                    if p_name not in seen_names:
                        seen_names.add(p_name)
                        p_obj = dict(p) if isinstance(p, dict) else {"name": p_name, "rt": "B", "ct": "B"}
                        dedup_list.append(p_obj)
                    else:
                        changed = True
            cleaned[role] = dedup_list

    # 2. Separate 'genga' (w > 0.25) and 'genga_2nd' (w <= 0.25)
    g1_list = []
    g1_names = set()
    g2_candidates = []

    raw_genga = s_map.get("genga")
    if isinstance(raw_genga, list):
        for p in raw_genga:
            p_name = (p.get("name") if isinstance(p, dict) else str(p) or "").strip()
            if not p_name:
                continue
            p_obj = dict(p) if isinstance(p, dict) else {"name": p_name, "rt": "B", "ct": "B"}
            w_val = float(p_obj.get("w", 1.0))
            if w_val <= 0.25:
                changed = True  # Moving from 'genga' to 'genga_2nd'
                p_obj["w"] = 0.2
                g2_candidates.append(p_obj)
            else:
                if "w" in p_obj:
                    p_obj.pop("w", None)
                    changed = True
                if p_name not in g1_names:
                    g1_names.add(p_name)
                    g1_list.append(p_obj)
                else:
                    changed = True

    raw_genga_2nd = list(s_map.get("genga_2nd") or []) if isinstance(s_map.get("genga_2nd"), list) else []
    for legacy_key in LEGACY_GENGA_2ND_KEYS:
        if legacy_key in s_map:
            changed = True
            if isinstance(s_map[legacy_key], list):
                raw_genga_2nd.extend(s_map[legacy_key])
    if raw_genga_2nd:
        for p in raw_genga_2nd:
            p_name = (p.get("name") if isinstance(p, dict) else str(p) or "").strip()
            if not p_name:
                continue
            p_obj = dict(p) if isinstance(p, dict) else {"name": p_name, "rt": "B", "ct": "B"}
            p_obj["w"] = 0.2
            g2_candidates.append(p_obj)

    g2_list = []
    g2_names = set()
    for p_obj in g2_candidates:
        p_name = p_obj["name"]
        if p_name in g1_names or p_name in g2_names:
            changed = True
            continue
        g2_names.add(p_name)
        g2_list.append(p_obj)

    if g1_list or "genga" in s_map:
        cleaned["genga"] = g1_list
    if g2_list:
        cleaned["genga_2nd"] = g2_list
    elif "genga_2nd" in s_map:
        changed = True

    # 3. Merge storyboard into unit_director without duplicates
    sb_list = s_map.get("storyboard")
    if isinstance(sb_list, list) and sb_list:
        changed = True
        ud_list = cleaned.setdefault("unit_director", [])
        ud_names = {(x.get("name") if isinstance(x, dict) else str(x) or "").strip() for x in ud_list}
        for p in sb_list:
            p_name = (p.get("name") if isinstance(p, dict) else str(p) or "").strip()
            if p_name and p_name not in ud_names:
                ud_list.append(p if isinstance(p, dict) else {"name": p_name, "rt": "B", "ct": "B"})
                ud_names.add(p_name)

    return cleaned, changed

def merge_staff_dicts(old_json_str: str, new_dict: dict) -> dict:
    merged = {}
    if old_json_str:
        try:
            old_d = json.loads(old_json_str)
            if isinstance(old_d, dict):
                old_d, _ = sanitize_staff_dict(old_d)
                for k, v in old_d.items():
                    if isinstance(v, list):
                        merged[k] = list(v)
        except:
            pass
    clean_new, _ = sanitize_staff_dict(new_dict or {})
    for role, persons in clean_new.items():
        existing = merged.setdefault(role, [])
        name_to_existing = {(x.get("name") if isinstance(x, dict) else str(x) or "").strip(): x for x in existing if isinstance(x, dict)}
        for p in persons:
            p_name = (p.get("name") if isinstance(p, dict) else str(p) or "").strip()
            if not p_name:
                continue
            p_obj = dict(p) if isinstance(p, dict) else {"name": p_name, "rt": "B", "ct": "B"}
            if p_name not in name_to_existing:
                existing.append(p_obj)
                name_to_existing[p_name] = p_obj
    final_merged, _ = sanitize_staff_dict(merged)
    return final_merged

def merge_cast_lists(old_json_str: str, new_list: list) -> list:
    merged = []
    seen = set()
    if old_json_str:
        try:
            old_l = json.loads(old_json_str)
            if isinstance(old_l, list):
                for c in old_l:
                    if isinstance(c, dict) and c.get("actor_name"):
                        key = (c.get("character_name", ""), c.get("actor_name", ""))
                        merged.append(c)
                        seen.add(key)
        except:
            pass
    for c in (new_list or []):
        if isinstance(c, dict) and c.get("actor_name"):
            key = (c.get("character_name", ""), c.get("actor_name", ""))
            if key not in seen:
                merged.append(c)
                seen.add(key)
    return merged

def execute_anilist_query(query: str, variables: dict = None, retries: int = 3):
    headers = {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }
    payload = json.dumps({"query": query, "variables": variables or {}}).encode("utf-8")
    req = urllib.request.Request(ANILIST_ENDPOINT, data=payload, headers=headers, method="POST")

    last_err = None
    for attempt in range(retries):
        try:
            time.sleep(0.55)
            with urllib.request.urlopen(req, timeout=15) as res:
                if res.status == 200:
                    STATS["anilist_ok"] += 1
                    return json.loads(res.read().decode("utf-8"))
                last_err = f"HTTP {res.status}"
        except urllib.error.HTTPError as e:
            last_err = f"HTTP {e.code}"
            if e.code == 429:
                wait_sec = int(e.headers.get("Retry-After", 6))
                print(f"[AniList] Rate limit reached. Waiting {wait_sec}s...")
                time.sleep(wait_sec)
                continue
            break
        except Exception as e:
            last_err = str(e)
            time.sleep(1.0)
    STATS["anilist_fail"] += 1
    print(f"[AniList] Query failed after retries: {last_err}")
    return None

def fetch_comprehensive_anilist_anime(current_year: int):
    """
    Fetches:
    1. All 4 seasons (WINTER, SPRING, SUMMER, FALL) for current_year and current_year - 1
    2. Currently RELEASING anime across all years
    3. Upcoming anime for current_year + 1
    """
    print(f"[AniList] Fetching seasonal & releasing anime around {current_year}...")
    seasonal_query = """
    query ($year: Int, $season: MediaSeason, $page: Int, $perPage: Int) {
      Page(page: $page, perPage: $perPage) {
        pageInfo { hasNextPage }
        media(type: ANIME, seasonYear: $year, season: $season, sort: [POPULARITY_DESC, SCORE_DESC]) {
          id
          title { romaji english native }
          seasonYear
          status
          averageScore
          meanScore
          coverImage { large extraLarge }
        }
      }
    }
    """
    releasing_query = """
    query ($page: Int, $perPage: Int) {
      Page(page: $page, perPage: $perPage) {
        pageInfo { hasNextPage }
        media(type: ANIME, status: RELEASING, sort: [POPULARITY_DESC, SCORE_DESC]) {
          id
          title { romaji english native }
          seasonYear
          status
          averageScore
          meanScore
          coverImage { large extraLarge }
        }
      }
    }
    """
    by_id = {}

    def add_media_items(media_list, fallback_year, force_airing=False):
        for m in media_list:
            aid = m["id"]
            t_ja = m.get("title", {}).get("native") or m.get("title", {}).get("romaji") or ""
            t_en = m.get("title", {}).get("english") or m.get("title", {}).get("romaji") or ""
            yr = m.get("seasonYear") or fallback_year
            sc = float(m.get("averageScore") or m.get("meanScore") or 0.0)
            st = m.get("status") or ""
            cov = (m.get("coverImage") or {}).get("large") or (m.get("coverImage") or {}).get("extraLarge")
            by_id[aid] = {
                "id": f"anime_{aid}",
                "anilist_id": aid,
                "title_ja": t_ja,
                "title_en": t_en,
                "year": yr,
                "score": sc,
                "is_airing": force_airing or (st == "RELEASING"),
                "cover_url": cov,
                "bangumi_id": None
            }

    # 1. Currently RELEASING anime (up to 3 pages = 150 items)
    for page in range(1, 4):
        res = execute_anilist_query(releasing_query, {"page": page, "perPage": 50})
        if res and res.get("data", {}).get("Page"):
            pg = res["data"]["Page"]
            add_media_items(pg.get("media", []), current_year, force_airing=True)
            if not pg.get("pageInfo", {}).get("hasNextPage"):
                break
    print(f"  -> Fetched {len(by_id)} currently releasing anime from AniList.")

    # 2. Seasonal anime for current_year (all 4 seasons, up to 2 pages = 100 per season)
    for season in ["WINTER", "SPRING", "SUMMER", "FALL"]:
        for page in range(1, 3):
            res = execute_anilist_query(seasonal_query, {"year": current_year, "season": season, "page": page, "perPage": 50})
            if res and res.get("data", {}).get("Page"):
                pg = res["data"]["Page"]
                add_media_items(pg.get("media", []), current_year, force_airing=(season == "FALL"))
                if not pg.get("pageInfo", {}).get("hasNextPage"):
                    break
    print(f"  -> Total unique AniList entries after {current_year} seasonal scan: {len(by_id)}")

    # 3. Previous year top seasonal works (1 page per season)
    for season in ["WINTER", "SPRING", "SUMMER", "FALL"]:
        res = execute_anilist_query(seasonal_query, {"year": current_year - 1, "season": season, "page": 1, "perPage": 50})
        if res and res.get("data", {}).get("Page"):
            add_media_items(res["data"]["Page"].get("media", []), current_year - 1, force_airing=False)

    print(f"  -> Total unique AniList entries collected: {len(by_id)}")
    return list(by_id.values())

def search_anilist_by_title(title: str):
    terms = [title]
    t_clean = re.sub(r'\(20\d\d\)', '', title).strip()
    if t_clean and t_clean not in terms:
        terms.append(t_clean)
    # Also try main title if subtitle is long
    parts = re.split(r'[\s　〜~]+', t_clean)
    if len(parts) > 1 and len(parts[0]) >= 5:
        if parts[0] not in terms:
            terms.append(parts[0])

    query = """
    query ($search: String) {
      Page(page: 1, perPage: 5) {
        media(search: $search, type: ANIME, sort: [POPULARITY_DESC]) {
          id
          title { native romaji english }
          seasonYear
          status
          averageScore
          meanScore
          coverImage { large }
        }
      }
    }
    """
    target_norm = normalize_for_match(title)
    target_main = extract_main_title_norm(title)

    for term in terms[:2]:
        if not term:
            continue
        res = execute_anilist_query(query, {"search": term})
        if res and res.get("data", {}).get("Page", {}).get("media"):
            media_list = res["data"]["Page"]["media"]
            # Prefer exact normalized match first
            for m in media_list:
                m_ja = m.get("title", {}).get("native") or ""
                m_ro = m.get("title", {}).get("romaji") or ""
                m_en = m.get("title", {}).get("english") or ""
                for cand_t in [m_ja, m_ro, m_en]:
                    if not cand_t:
                        continue
                    c_norm = normalize_for_match(cand_t)
                    c_main = extract_main_title_norm(cand_t)
                    if c_norm == target_norm or (len(target_main) >= 5 and c_main == target_main):
                        sc = float(m.get("averageScore") or m.get("meanScore") or 0.0)
                        return m, sc
            # Fallback to first result if year is >= 2024 and search term was specific
            m0 = media_list[0]
            if (m0.get("seasonYear") or 0) >= 2024 and len(target_norm) >= 6:
                sc = float(m0.get("averageScore") or m0.get("meanScore") or 0.0)
                return m0, sc
    return None, 0.0

def fetch_bangumi_calendar_anime():
    print("[Bangumi] Fetching currently airing anime via Calendar API (/calendar)...")
    headers = {"User-Agent": USER_AGENT, "Accept": "application/json"}
    req = urllib.request.Request(f"{BANGUMI_ENDPOINT}/calendar", headers=headers)
    results = []
    current_year = datetime.datetime.now().year

    try:
        with urllib.request.urlopen(req, timeout=15) as res:
            if res.status == 200:
                calendar_days = json.loads(res.read().decode("utf-8"))
                for day in calendar_days:
                    for item in day.get("items", []):
                        title_ja = (item.get("name") or item.get("name_cn") or "").strip()
                        if not title_ja:
                            continue
                        air_date = item.get("air_date") or ""
                        year = int(air_date[:4]) if len(air_date) >= 4 and air_date[:4].isdigit() else current_year
                        rating = item.get("rating") or {}
                        score_10 = float(rating.get("score") or 0.0)
                        bgm_score_100 = round(score_10 * 10.0, 1) if score_10 > 0 else 0.0

                        results.append({
                            "id": f"anime_bgm_{item['id']}",
                            "anilist_id": None,
                            "title_ja": title_ja,
                            "title_en": item.get("name_cn") or "",
                            "year": year,
                            "score": 0.0, # Will be enriched via AniList matching first, fallback to bgm_score_100
                            "bgm_score": bgm_score_100,
                            "is_airing": True,
                            "cover_url": None,
                            "bangumi_id": int(item["id"])
                        })
                print(f"  -> Fetched {len(results)} airing titles from Bangumi Calendar")
    except Exception as e:
        STATS["bgm_calendar_error"] = str(e)
        print(f"[Bangumi] Calendar request error: {e}")
    return results

def pick_bangumi_subject(items: list, title_jp: str, year: int = None):
    """
    Picks the Bangumi search result that really is the same work.
    Never falls back to the first result: a wrong pick would attach another work's staff,
    and the merge is append-only, so wrong credits would stay forever.
    """
    target_n = normalize_for_match(title_jp)
    target_main = extract_main_title_norm(title_jp)

    def year_ok(it):
        if not year:
            return True
        d = str(it.get("date") or "")
        if len(d) >= 4 and d[:4].isdigit():
            return abs(int(d[:4]) - int(year)) <= 1
        return True  # Bangumi has no date: accept on title match alone

    # 1. Exact normalized title match (year must not contradict)
    for it in items:
        for t in (it.get("name", ""), it.get("name_cn", "")):
            if t and normalize_for_match(t) == target_n and year_ok(it):
                return it.get("id")
    # 2. Main title (subtitle stripped) + year match
    if len(target_main) >= 6 and year:
        for it in items:
            for t in (it.get("name", ""), it.get("name_cn", "")):
                if t and extract_main_title_norm(t) == target_main and year_ok(it) and it.get("date"):
                    return it.get("id")
    return None

def fetch_bangumi_staff_and_cast(title_jp: str, subject_id: int = None, year: int = None):
    headers = {
        "Accept": "application/json",
        "User-Agent": USER_AGENT,
        "Content-Type": "application/json"
    }
    try:
        if not subject_id:
            search_url = f"{BANGUMI_ENDPOINT}/v0/search/subjects?limit=5"
            payload = json.dumps({"keyword": title_jp, "filter": {"type": [2]}}).encode("utf-8")
            req = urllib.request.Request(search_url, data=payload, headers=headers, method="POST")
            time.sleep(0.25)
            with urllib.request.urlopen(req, timeout=12) as res:
                if res.status == 200:
                    data = json.loads(res.read().decode("utf-8"))
                    items = data.get("data", [])
                    if items:
                        subject_id = pick_bangumi_subject(items, title_jp, year)
                        if not subject_id:
                            STATS["bgm_no_match"] += 1

        if not subject_id:
            return {}, [], None

        # 1. Fetch Staff (Persons)
        persons_url = f"{BANGUMI_ENDPOINT}/v0/subjects/{subject_id}/persons"
        req = urllib.request.Request(persons_url, headers=headers, method="GET")
        time.sleep(0.25)
        staff_by_role = {}
        with urllib.request.urlopen(req, timeout=12) as res:
            if res.status == 200:
                persons = json.loads(res.read().decode("utf-8"))
                for p in persons:
                    r_name = p.get("relation", "").strip()
                    p_name = p.get("name", "").strip()
                    role_key = RELATION_MAP.get(r_name)
                    if role_key and p_name:
                        existing = staff_by_role.setdefault(role_key, [])
                        ex_item = next((x for x in existing if x["name"] == p_name), None)
                        is_second_genga = (role_key == "genga_2nd")
                        if not ex_item:
                            entry = {"name": p_name, "rt": "B", "ct": "B"}
                            if is_second_genga:
                                entry["w"] = 0.2
                            existing.append(entry)
        staff_by_role, _ = sanitize_staff_dict(staff_by_role)

        # 2. Fetch Characters & Cast
        chars_url = f"{BANGUMI_ENDPOINT}/v0/subjects/{subject_id}/characters"
        req = urllib.request.Request(chars_url, headers=headers, method="GET")
        time.sleep(0.25)
        cast_list = []
        with urllib.request.urlopen(req, timeout=12) as res:
            if res.status == 200:
                characters = json.loads(res.read().decode("utf-8"))
                for c in characters:
                    char_name = c.get("name", "").strip()
                    relation = c.get("relation", "配角")
                    actors = c.get("actors", [])
                    for act in actors:
                        actor_name = act.get("name", "").strip()
                        if char_name and actor_name:
                            cast_list.append({
                                "character_name": char_name,
                                "relation": relation,
                                "actor_name": actor_name,
                                "rt": "B",
                                "ct": "B"
                            })
                            break

        STATS["bgm_ok"] += 1
        return staff_by_role, cast_list, subject_id
    except urllib.error.HTTPError as e:
        if e.code == 404:
            # Subject hidden from anonymous API (mostly R18) or deleted: not an outage
            STATS["bgm_not_found"] += 1
            return {}, [], subject_id
        STATS["bgm_fail"] += 1
        print(f"[Bangumi] Fetch error for '{title_jp}' (sid={subject_id}): {e}")
        return {}, [], subject_id
    except Exception as e:
        STATS["bgm_fail"] += 1
        print(f"[Bangumi] Fetch error for '{title_jp}' (sid={subject_id}): {e}")
        return {}, [], subject_id

def recalculate_all_metrics_and_resolve_tiers(conn):
    print("\n[4/6] 全作品の時代補正Zスコア・偏差値 & 全スタッフ実力S(a)（Z=0除外）を再計算中...")
    cur = conn.cursor()

    cur.execute("SELECT work_id, year, anilist_raw_score FROM works")
    all_works = cur.fetchall()
    scored_items = {wid: float(sc) for wid, yr, sc in all_works if sc and float(sc) > 0.0}
    item_years = {wid: int(yr or 0) for wid, yr, sc in all_works if sc and float(sc) > 0.0}

    year_to_items = defaultdict(list)
    for wid, yr in item_years.items():
        year_to_items[yr].append(wid)

    work_zscores = {}
    work_devscores = {}

    for wid, b_val in scored_items.items():
        target_year = item_years[wid]
        window_b = []
        for y_offset in range(-3, 4):
            y_check = target_year + y_offset
            for neighbor in year_to_items.get(y_check, []):
                window_b.append(scored_items[neighbor])

        if len(window_b) >= 5:
            mu_t = sum(window_b) / len(window_b)
            var_t = sum((x - mu_t) ** 2 for x in window_b) / len(window_b)
            sigma_t = max(var_t ** 0.5, 1.0)
        else:
            mu_t = sum(scored_items.values()) / len(scored_items)
            var_t = sum((x - mu_t) ** 2 for x in scored_items.values()) / len(scored_items)
            sigma_t = max(var_t ** 0.5, 1.0)

        z = (b_val - mu_t) / sigma_t
        dev = 50.0 + 10.0 * z
        work_zscores[wid] = z
        work_devscores[wid] = dev

    sorted_by_z = sorted(scored_items.keys(), key=lambda w: work_zscores[w], reverse=True)
    total_scored = len(sorted_by_z)

    works_update_batch = []
    for rank, wid in enumerate(sorted_by_z, start=1):
        z = work_zscores[wid]
        dev = work_devscores[wid]
        tier = calculate_tier(rank, total_scored)
        pct = round((1.0 - (rank - 1) / max(1, total_scored)) * 100, 1)
        works_update_batch.append((round(z, 3), round(dev, 1), tier, rank, pct, wid))

    cur.executemany("""
        UPDATE works
        SET true_z_score = ?, deviation_score = ?, tier = ?, z_score_rank = ?, percentile = ?
        WHERE work_id = ?;
    """, works_update_batch)
    conn.commit()
    print(f"  -> {len(works_update_batch)} 評価済み作品のZスコア・偏差値・Tierを更新しました。")

    work_cols = {r[1] for r in cur.execute("PRAGMA table_info(works)")}
    ep_col = "episodes" if "episodes" in work_cols else "NULL"
    cur.execute(f"SELECT work_id, title, year, staff_json, characters_json, {ep_col} FROM works")
    all_works_data = cur.fetchall()

    staff_overall = defaultdict(list)
    staff_by_role = defaultdict(list)
    rel_weights = {"主角": 1.0, "配角": 0.5, "客串": 0.2, "MAIN": 1.0, "SUPPORTING": 0.5, "BACKGROUND": 0.2}
    sanitized_updates = []

    # --- 参加度（話数補正）の準備 ---------------------------------------------
    # 1回目: staff_json を正規化し、話数データのある作品から「1話あたりの人数」を役職ごとに推定する
    parsed_staff = {}
    per_ep_samples = defaultdict(list)
    for wid, title, yr, s_json, c_json, n_eps in all_works_data:
        if not s_json:
            continue
        try:
            s_map = json.loads(s_json)
        except Exception:
            continue
        s_map, was_sanitized = sanitize_staff_dict(s_map)
        parsed_staff[wid] = s_map
        if was_sanitized:
            sanitized_updates.append((json.dumps(s_map, ensure_ascii=False), wid))
        E = int(n_eps or 0)
        if E >= 2:
            for role in EPISODE_ROLES:
                lst = [x for x in s_map.get(role, []) if isinstance(x, dict)]
                with_eps = [x for x in lst if x.get("eps")]
                if lst and len(with_eps) >= 0.5 * len(lst):
                    per_ep_samples[role].append(sum(len(x["eps"]) for x in with_eps) / E)
    people_per_ep = {
        r: (statistics.median(per_ep_samples[r]) if len(per_ep_samples[r]) >= 20 else DEFAULT_PEOPLE_PER_EPISODE[r])
        for r in EPISODE_ROLES
    }
    print("  -> 1話あたりの人数（推定）: " + ", ".join(f"{r} {people_per_ep[r]:.1f} (n={len(per_ep_samples[r])})" for r in sorted(EPISODE_ROLES)))

    def participation(role, person, n_in_role, n_eps):
        """作品への参加度 (0〜1]。話数が分かればその割合、分からなければ人数から推定する"""
        if role not in EPISODE_ROLES:
            return 1.0, None
        eps = person.get("eps") if isinstance(person, dict) else None
        if eps:
            total = max(int(n_eps or 0), max(eps))
            return min(1.0, len(eps) / total), eps
        return min(1.0, people_per_ep[role] / max(1, n_in_role)), None

    ep_known = ep_total = 0
    for wid, title, yr, s_json, c_json, n_eps in all_works_data:
        year = int(yr or 0)
        z = work_zscores.get(wid, 0.0)
        s_map = parsed_staff.get(wid)

        if s_map:
            for role in ["director", "series_comp", "char_design", "unit_director", "sakkan", "music", "art_dir", "genga", "genga_2nd"]:
                names = s_map.get(role, [])
                if isinstance(names, str):
                    names = [names]
                if not isinstance(names, list):
                    continue
                base_w = 0.2 if role == "genga_2nd" else 1.0
                n_in_role = len(names)
                for n in names:
                    name = n.get("name") if isinstance(n, dict) else str(n)
                    name = name.strip() if name else ""
                    if not name:
                        continue
                    share, eps = participation(role, n, n_in_role, n_eps)
                    if role in EPISODE_ROLES:
                        ep_total += 1
                        ep_known += 1 if eps else 0
                    rec = {"work_id": wid, "title": title, "year": year, "role": role, "z": z,
                           "w": base_w * math.sqrt(share), "share": share}
                    if eps:
                        rec["eps"] = eps
                    if isinstance(n, dict) and n.get("parts"):
                        rec["parts"] = n["parts"]
                    staff_overall[name].append(rec)
                    staff_by_role[(role, name)].append(rec)

        if c_json:
            try:
                chars = json.loads(c_json)
                seen_actor = set()
                for cr in chars:
                    aname = (cr.get("actor_name") or cr.get("actor") or "").strip()
                    if aname and aname not in seen_actor:
                        seen_actor.add(aname)
                        rel = cr.get("relation", "配角")
                        w = rel_weights.get(rel, 0.5)
                        cname = cr.get("character_name") or ""
                        rec = {"work_id": wid, "title": title, "year": year, "role": "cv", "z": z, "w": w, "top_char": f"{cname} ({rel})"}
                        staff_overall[aname].append(rec)
                        staff_by_role[("cv", aname)].append(rec)
            except:
                pass

    if sanitized_updates:
        cur.executemany("UPDATE works SET staff_json = ? WHERE work_id = ?", sanitized_updates)
        conn.commit()
        print(f"  -> {len(sanitized_updates)} 作品の staff_json を正規化しました（原画・2原の分離、storyboard統合、不要役職除外）。")

    roles_list = ["all", "director", "series_comp", "char_design", "unit_director", "sakkan", "genga", "genga_2nd", "music", "art_dir", "cv"]
    lb_update_batch = []

    # 参加度で重みが小さくなった分、縮小（m）も同じ割合で小さくして、縮小の強さを保つ
    def _median_w(recs):
        ws = [r["w"] for r in recs]
        return statistics.median(ws) if ws else 1.0
    m_scale = {r: _median_w([rec for (rr, _), recs in staff_by_role.items() if rr == r for rec in recs]) /
                  (0.2 if r == "genga_2nd" else 1.0)
               for r in EPISODE_ROLES}
    m_scale["all"] = _median_w([rec for recs in staff_overall.values() for rec in recs if rec["role"] != "cv"])
    print(f"  -> 話数が分かっている参加: {ep_known}/{ep_total} 件 / 縮小の調整係数: " +
          ", ".join(f"{k} {v:.2f}" for k, v in sorted(m_scale.items())))

    for rKey in roles_list:
        m_val = DEFAULT_ROLE_M.get(rKey, 6.0) * m_scale.get(rKey, 1.0)
        candidates = []

        if rKey == "all":
            # 「全役職」は制作スタッフだけで集計する（声優は「声優」の一覧で見る）
            for name, all_recs in staff_overall.items():
                recs = [r for r in all_recs if r["role"] != "cv"]
                if not recs:
                    continue
                eval_recs = [r for r in recs if abs(r["z"]) > 1e-6]
                if eval_recs:
                    w_sum = sum(r["w"] for r in eval_recs)
                    z_sum = sum(r["z"] * r["w"] for r in eval_recs)
                    rating = z_sum / (w_sum + m_val)
                    best = max(eval_recs, key=lambda r: r["z"])
                else:
                    w_sum = 0.0
                    z_sum = 0.0
                    rating = 0.0
                    best = recs[0]

                years = [r["year"] for r in recs if r["year"] > 0]
                first_y = min(years) if years else None
                latest_y = max(years) if years else None

                candidates.append({
                    "role": "all",
                    "name": name,
                    "works_count": len(recs),
                    "rating": rating,
                    "cum_z": z_sum,
                    "first_year": first_y,
                    "latest_year": latest_y,
                    "best_title": best["title"],
                    "best_year": best["year"],
                    "best_z": best["z"],
                    "top_char": None
                })
        else:
            for (role, name), recs in staff_by_role.items():
                if role != rKey:
                    continue
                eval_recs = [r for r in recs if abs(r["z"]) > 1e-6]
                if eval_recs:
                    w_sum = sum(r["w"] for r in eval_recs)
                    z_sum = sum(r["z"] * r["w"] for r in eval_recs)
                    rating = z_sum / (w_sum + m_val)
                    best = max(eval_recs, key=lambda r: r["z"])
                else:
                    w_sum = 0.0
                    z_sum = 0.0
                    rating = 0.0
                    best = recs[0]

                years = [r["year"] for r in recs if r["year"] > 0]
                first_y = min(years) if years else None
                latest_y = max(years) if years else None

                top_char = None
                if role == "cv":
                    for r in recs:
                        if r.get("top_char"):
                            top_char = r["top_char"]
                            if "主角" in top_char or "MAIN" in top_char:
                                break

                candidates.append({
                    "role": role,
                    "name": name,
                    "works_count": len(recs),
                    "rating": rating,
                    "cum_z": z_sum,
                    "first_year": first_y,
                    "latest_year": latest_y,
                    "best_title": best["title"],
                    "best_year": best["year"],
                    "best_z": best["z"],
                    "top_char": top_char
                })

        candidates.sort(key=lambda c: c["rating"], reverse=True)
        tot_c = len(candidates)
        for rk, c in enumerate(candidates, start=1):
            c["rating_rank"] = rk
            c["rating_tier"] = calculate_tier(rk, tot_c)

        candidates.sort(key=lambda c: c["cum_z"], reverse=True)
        for rk, c in enumerate(candidates, start=1):
            c["cum_rank"] = rk
            c["cum_tier"] = calculate_tier(rk, tot_c)

        for c_info in candidates:
            lb_update_batch.append((
                c_info["role"],
                c_info["name"],
                norm(c_info["name"]),
                c_info["works_count"],
                round(c_info["rating"], 3),
                round(c_info["cum_z"], 2),
                c_info["rating_rank"],
                c_info["cum_rank"],
                c_info["rating_tier"],
                c_info["cum_tier"],
                c_info["best_title"],
                c_info["best_year"],
                round(c_info["best_z"], 3),
                c_info["top_char"],
                c_info["first_year"],
                c_info["latest_year"]
            ))

    cur.execute("DELETE FROM leaderboards;")
    cur.executemany("""
        INSERT INTO leaderboards (
            role, name, name_norm, works_count, bayesian_rating, career_cumulative_z,
            rating_rank, cumulative_rank, rating_tier, cumulative_tier,
            best_work_title, best_work_year, best_work_z, top_character,
            first_year, latest_year
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
    """, lb_update_batch)

    # Rebuild profiles
    cur.execute("DELETE FROM profiles;")
    cur.execute("SELECT role, COUNT(*) FROM leaderboards GROUP BY role;")
    role_totals = dict(cur.fetchall())

    cur.execute("""
        SELECT role, name, works_count, bayesian_rating, career_cumulative_z,
               rating_rank, cumulative_rank, rating_tier, cumulative_tier,
               first_year, latest_year
        FROM leaderboards
        ORDER BY role, rating_rank;
    """)
    lb_all_rows = cur.fetchall()
    staff_lb_map = defaultdict(dict)
    for r in lb_all_rows:
        role, name, w_count, b_rating, cum_z, r_rank, c_rank, r_tier, c_tier, f_yr, l_yr = r
        staff_lb_map[name][role] = {
            "role": role,
            "works_count": w_count,
            "bayesian_rating": round(b_rating, 3),
            "cumulative_z": round(cum_z, 2),
            "rating_rank": r_rank,
            "cumulative_rank": c_rank,
            "role_total": role_totals.get(role, 0),
            "rating_tier": r_tier,
            "cum_tier": c_tier,
            "first_year": f_yr,
            "latest_year": l_yr
        }

    prof_batch = []
    for name, roles in staff_lb_map.items():
        sub_roles = [st for r, st in roles.items() if r != "all"]
        sub_roles.sort(key=lambda s: s["works_count"], reverse=True)
        primary_role = sub_roles[0]["role"] if sub_roles else "cv"

        # 総合成績: 主に声優の人は「声優」の成績、それ以外は「全スタッフ」（声優を除く）の成績を使う
        if primary_role == "cv" and "cv" in roles:
            all_stat = roles["cv"]
        else:
            all_stat = roles.get("all") or roles.get("cv")
        if not all_stat:
            continue

        role_stats_list = []
        for st in sub_roles:
            role_stats_list.append({
                "role": st["role"],
                "works_count": st["works_count"],
                "bayesian_rating": st["bayesian_rating"],
                "cumulative_z": st["cumulative_z"],
                "career_cumulative_z": st["cumulative_z"],
                "rating_rank": st["rating_rank"],
                "cumulative_rank": st["cumulative_rank"],
                "role_total": st["role_total"],
                "rating_tier": st["rating_tier"],
                "cum_tier": st["cum_tier"],
                "cumulative_tier": st["cum_tier"]
            })

        raw_recs = staff_overall.get(name, [])
        traj = []
        for rec in raw_recs:
            t_item = {
                "work_id": rec["work_id"],
                "work_title": rec["title"],
                "year": rec["year"],
                "role": rec["role"],
                "z_score": round(rec["z"], 3)
            }
            if "top_char" in rec and rec["top_char"]:
                t_item["character_name"] = rec["top_char"]
            if rec.get("eps"):
                t_item["eps"] = rec["eps"]
            if rec.get("parts"):
                t_item["parts"] = rec["parts"]
            traj.append(t_item)
        traj.sort(key=lambda x: (x["year"], x["work_id"]))

        prof_batch.append((
            name,
            norm(name),
            primary_role,
            all_stat["works_count"],
            all_stat["bayesian_rating"],
            all_stat["rating_tier"],
            all_stat["rating_rank"],
            all_stat["cumulative_z"],
            all_stat["cum_tier"],
            all_stat["cumulative_rank"],
            json.dumps(role_stats_list, ensure_ascii=False),
            json.dumps(traj, ensure_ascii=False),
            all_stat["first_year"],
            all_stat["latest_year"]
        ))

    cur.executemany("""
        INSERT INTO profiles (
            name, name_norm, primary_role, total_works, bayesian_rating,
            overall_rating_tier, overall_rank, career_cumulative_z,
            overall_cum_tier, cumulative_rank, all_role_stats_json,
            career_trajectory_json, first_year, latest_year
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
    """, prof_batch)
    conn.commit()
    print(f"  -> {len(prof_batch)} 名のスタッフプロファイルを再構築しました。")

    # Resolve staff & cast tiers in works
    print("  -> 各作品の staff_json / characters_json に最新のスタッフTier (rt, ct) を反映中...")
    cur.execute("SELECT role, name, rating_tier, cumulative_tier FROM leaderboards")
    tier_map = {}
    for role, name, r_tier, c_tier in cur.fetchall():
        tier_map[(role, name)] = (r_tier, c_tier)
        if role == "all":
            tier_map[("fallback", name)] = (r_tier, c_tier)

    cur.execute("SELECT work_id, staff_json, characters_json FROM works")
    works = cur.fetchall()
    updates = []
    for wid, s_json, c_json in works:
        changed = False
        new_s_json = s_json
        new_c_json = c_json
        if s_json:
            try:
                s_map = json.loads(s_json)
                for role, staff_list in s_map.items():
                    if isinstance(staff_list, list):
                        for item in staff_list:
                            if isinstance(item, dict):
                                name = item.get("name", "").strip()
                                tiers = tier_map.get((role, name)) or tier_map.get(("fallback", name))
                                if tiers:
                                    r_t, c_t = tiers
                                    if item.get("rt") != r_t or item.get("ct") != c_t:
                                        item["rt"] = r_t
                                        item["ct"] = c_t
                                        changed = True
                if changed:
                    new_s_json = json.dumps(s_map, ensure_ascii=False)
            except:
                pass
        if c_json:
            try:
                c_list = json.loads(c_json)
                if isinstance(c_list, list):
                    for char in c_list:
                        if isinstance(char, dict):
                            actor_name = char.get("actor_name", "").strip()
                            tiers = tier_map.get(("cv", actor_name)) or tier_map.get(("fallback", actor_name))
                            if tiers:
                                r_t, c_t = tiers
                                if char.get("rt") != r_t or char.get("ct") != c_t:
                                    char["rt"] = r_t
                                    char["ct"] = c_t
                                    changed = True
                    if changed:
                        new_c_json = json.dumps(c_list, ensure_ascii=False)
            except:
                pass
        if changed:
            updates.append((new_s_json, new_c_json, wid))

    if updates:
        cur.executemany("UPDATE works SET staff_json = ?, characters_json = ? WHERE work_id = ?", updates)
        conn.commit()
        print(f"  -> {len(updates)} 作品のスタッフ・キャストTierを更新しました。")

def build_db_lookup_index(cursor):
    """Builds fast lookup indices for matching existing DB works by ID, normalized title, and main title."""
    cursor.execute("SELECT work_id, title, title_en, year, anilist_raw_score, staff_json, characters_json FROM works")
    by_id = {}
    by_norm_title = {}
    by_main_title_year = {}

    for row in cursor.fetchall():
        wid, title, title_en, yr, sc, s_json, c_json = row
        info = {
            "work_id": wid,
            "title": title or "",
            "title_en": title_en or "",
            "year": int(yr or 0),
            "score": float(sc or 0.0),
            "staff_json": s_json or "{}",
            "characters_json": c_json or "[]"
        }
        by_id[wid] = info
        for t in [title, title_en]:
            if t:
                nt = normalize_for_match(t)
                if nt:
                    # Prefer works with higher staff count or existing score if duplicate norm title exists
                    if nt not in by_norm_title or info["score"] > by_norm_title[nt]["score"]:
                        by_norm_title[nt] = info
                mt = extract_main_title_norm(t)
                if mt and len(mt) >= 6:
                    for y_off in (0, -1, 1):
                        key = (mt, info["year"] + y_off)
                        if key not in by_main_title_year:
                            by_main_title_year[key] = info

    return by_id, by_norm_title, by_main_title_year

def match_existing_work(cand, by_id, by_norm_title, by_main_title_year):
    # 1. Direct ID match
    for cand_id in [cand.get("id"), f"anime_{cand.get('anilist_id')}", f"anilist_{cand.get('anilist_id')}", f"anime_bgm_{cand.get('bangumi_id')}"]:
        if cand_id and cand_id in by_id:
            return by_id[cand_id]

    # 2. Normalized title match
    for t in [cand.get("title_ja"), cand.get("title_en")]:
        if t:
            nt = normalize_for_match(t)
            if nt and nt in by_norm_title:
                return by_norm_title[nt]

    # 3. Main title + year match (absorbs long light novel subtitle differences)
    yr = int(cand.get("year") or 0)
    for t in [cand.get("title_ja"), cand.get("title_en")]:
        if t:
            mt = extract_main_title_norm(t)
            if mt and len(mt) >= 6 and yr > 0:
                if (mt, yr) in by_main_title_year:
                    return by_main_title_year[(mt, yr)]

    return None

def write_step_summary(lines):
    """Appends a Markdown report to the GitHub Actions run page (no-op locally)."""
    path = os.environ.get("GITHUB_STEP_SUMMARY")
    if not path:
        return
    with open(path, "a", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")

def check_api_health(anilist_count: int, bgm_cal_count: int) -> list:
    """
    Returns a list of fatal problems. A non-empty list means the run must fail
    (red in GitHub Actions) instead of silently committing a stale database.
    """
    problems = []
    if anilist_count == 0:
        problems.append(f"AniList から作品を1件も取得できませんでした (成功 {STATS['anilist_ok']} / 失敗 {STATS['anilist_fail']})")
    if bgm_cal_count == 0:
        problems.append(f"Bangumi Calendar から作品を1件も取得できませんでした ({STATS.get('bgm_calendar_error') or '0件'})")
    bgm_total = STATS["bgm_ok"] + STATS["bgm_fail"]
    if bgm_total >= 10 and STATS["bgm_fail"] / bgm_total > 0.3:
        problems.append(f"Bangumi スタッフ取得の失敗率が高すぎます ({STATS['bgm_fail']}/{bgm_total})")
    ani_total = STATS["anilist_ok"] + STATS["anilist_fail"]
    if ani_total >= 10 and STATS["anilist_fail"] / ani_total > 0.3:
        problems.append(f"AniList クエリの失敗率が高すぎます ({STATS['anilist_fail']}/{ani_total})")
    return problems

def parse_args():
    import argparse
    p = argparse.ArgumentParser(description="CreditDB weekly updater (AniList & Bangumi)")
    p.add_argument("--input", required=True, help="前回のDB (.zip / .db.gz / .db)")
    p.add_argument("--out-dir", required=True, help="creditdb.zip と version.json の出力先")
    p.add_argument("--covers-seed", default=None, help="DBに work_covers テーブルが無い場合に使う work_covers.json")
    p.add_argument("--recalc-only", action="store_true", help="API取得をせず再計算だけ行う")
    p.add_argument("--anilist-store", default=None, help="AniList 話数クレジットの保存先 (anilist_credits.zip)")
    p.add_argument("--anilist-out", default=None, help="更新後の anilist_credits.zip の出力先")
    p.add_argument("--anilist-minutes", type=float, default=25, help="AniList 再取得に使う時間の上限（分）")
    return p.parse_args()

def refresh_anilist_recent(conn, store, current_year: int, minutes: float) -> dict:
    """今年・来年の作品の AniList クレジットを再取得する（前回取得から6日以上たったものだけ）"""
    import anilist_credits as ac
    deadline = time.time() + minutes * 60
    fresh = {r[0] for r in store.execute("SELECT work_id FROM media WHERE fetched_at >= datetime('now', '-6 days')")}
    not_found = {r[0] for r in store.execute("SELECT work_id FROM idmap WHERE anilist_id IS NULL AND tried_at >= datetime('now', '-30 days')")}
    targets = [r for r in conn.execute(
        "SELECT work_id, title, title_en, year FROM works WHERE year >= ? ORDER BY anilist_raw_score DESC", (current_year,))
        if r[0] not in fresh and r[0] not in not_found]
    client = ac.AniListClient()
    fetched = 0
    for work_id, title, title_en, year in targets:
        if time.time() > deadline:
            break
        aid = ac.anilist_id_of(work_id) or ac.resolve_anilist_id(client, store, work_id, title, year, title_en)
        if aid and ac.fetch_work(client, store, work_id, aid):
            fetched += 1
    STATS["anilist_credit_refresh"] = fetched
    return {"targets": len(targets), "fetched": fetched, "requests": client.requests}

def load_db_bytes(path: Path) -> bytes:
    name = path.name.lower()
    if name.endswith(".zip"):
        with zipfile.ZipFile(path) as zf:
            member = next((n for n in zf.namelist() if n.endswith(".db")), None)
            if not member:
                raise SystemExit(f"[ERROR] {path} に .db が含まれていません。")
            return zf.read(member)
    if name.endswith(".gz"):
        with gzip.open(path, "rb") as f:
            return f.read()
    return path.read_bytes()

def load_work_covers(conn, seed_path):
    """Reads the cover map from the DB table (if present), filling gaps from the seed JSON."""
    covers = {}
    if seed_path and seed_path.exists():
        try:
            with open(seed_path, "r", encoding="utf-8") as fc:
                covers.update({k: str(v) for k, v in json.load(fc).items()})
        except Exception as e:
            print(f"[WARN] {seed_path} を読めませんでした: {e}")
    try:
        for wid, target in conn.execute("SELECT work_id, target FROM work_covers"):
            covers[wid] = str(target)
    except sqlite3.OperationalError:
        pass  # table not created yet (first run after migration)
    return covers

def save_work_covers(conn, covers: dict):
    """Stores the cover map inside the DB so the app gets new covers with each DB release."""
    conn.execute("CREATE TABLE IF NOT EXISTS work_covers (work_id TEXT PRIMARY KEY, target TEXT NOT NULL)")
    conn.execute("DELETE FROM work_covers")
    conn.executemany("INSERT INTO work_covers (work_id, target) VALUES (?, ?)",
                     [(k, str(v)) for k, v in covers.items()])
    conn.commit()

def main():
    print("=================================================================")
    print("  CreditDB: 週次データ自動更新・放送中アニメ動的追跡バッチ v3.0")
    print("=================================================================")

    args = parse_args()
    in_path = Path(args.input)
    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    if not in_path.exists():
        print(f"[ERROR] {in_path} が見つかりません。")
        sys.exit(1)

    print(f"[1/6] 既存データベースを読み込み中: {in_path.name}...")
    db_bytes = load_db_bytes(in_path)

    conn = sqlite3.connect(":memory:")
    conn.deserialize(db_bytes)
    cursor = conn.cursor()

    cursor.execute("SELECT total_works, total_staff, total_cv, global_mean FROM summary LIMIT 1")
    prev_summary = cursor.fetchone() or (5452, 22896, 5840, 65.0)
    print(f"  -> 現在登録数: {prev_summary[0]} 作品 / 平均スコア: {prev_summary[3]:.1f}")

    # Cover map: DB table first (kept in sync every week), then the seed JSON
    work_covers = load_work_covers(conn, Path(args.covers_seed) if args.covers_seed else None)
    print(f"  -> カバー画像の対応表: {len(work_covers)} 件")

    current_year = datetime.datetime.now().year
    recalc_only = args.recalc_only

    if not recalc_only:
        print("\n[2/6] AniList (季節別・放送中) & Bangumi Calendar (放送中番組表) を両方取得・統合中...")
        anilist_list = fetch_comprehensive_anilist_anime(current_year)
        bgm_cal_list = fetch_bangumi_calendar_anime()
    else:
        print("\n[2-3/6] --recalc-only モード: API取得をスキップし、全作品のスタッフ役職正規化・Tier再計算を実行します...")
        anilist_list = []
        bgm_cal_list = []

    # Merge AniList and Bangumi Calendar candidates using normalized title matching
    merged_candidates = []
    norm_to_cand = {}

    for item in anilist_list:
        merged_candidates.append(item)
        for t in [item["title_ja"], item["title_en"]]:
            nt = normalize_for_match(t)
            if nt:
                norm_to_cand[nt] = item
            mt = extract_main_title_norm(t)
            if mt and len(mt) >= 6:
                norm_to_cand[mt] = item

    for b_item in bgm_cal_list:
        nt_ja = normalize_for_match(b_item["title_ja"])
        mt_ja = extract_main_title_norm(b_item["title_ja"])
        matched = norm_to_cand.get(nt_ja) or (norm_to_cand.get(mt_ja) if len(mt_ja) >= 6 else None)
        if matched:
            matched["bangumi_id"] = b_item["bangumi_id"]
            matched["is_airing"] = True
        else:
            # Query AniList specifically for this Bangumi Calendar anime
            m_ani, sc_ani = search_anilist_by_title(b_item["title_ja"])
            if m_ani:
                b_item["score"] = sc_ani
                b_item["anilist_id"] = m_ani.get("id")
                if m_ani.get("title", {}).get("english"):
                    b_item["title_en"] = m_ani["title"]["english"]
                cov = (m_ani.get("coverImage") or {}).get("large")
                if cov:
                    b_item["cover_url"] = cov
            merged_candidates.append(b_item)
            if nt_ja:
                norm_to_cand[nt_ja] = b_item

    print(f"  -> 統合後の精査対象候補: {len(merged_candidates)} 作品 (うち現在放送中: {sum(1 for x in merged_candidates if x.get('is_airing'))} 作品)")

    print("\n[3/6] データベース照合・スコア更新・放送中スタッフ動的取得中...")
    by_id, by_norm_title, by_main_title_year = build_db_lookup_index(cursor)

    updated_score_count = 0
    updated_staff_count = 0
    new_count = 0

    for anime in merged_candidates:
        title_ja = anime["title_ja"]
        title_en = anime["title_en"]
        year = anime["year"]
        raw_score = float(anime["score"] or 0.0)
        bangumi_id = anime.get("bangumi_id")
        is_airing = anime.get("is_airing", False)
        cover_url = anime.get("cover_url")

        if not title_ja:
            continue

        existing = match_existing_work(anime, by_id, by_norm_title, by_main_title_year)

        if existing:
            db_work_id = existing["work_id"]
            old_raw = existing["score"]
            old_staff_json = existing["staff_json"]
            old_chars_json = existing["characters_json"]
            old_staff_count = count_total_staff(old_staff_json)

            # Extract bangumi_id from db_work_id if it starts with anime_bgm_
            if not bangumi_id and db_work_id.startswith("anime_bgm_"):
                try:
                    bangumi_id = int(db_work_id.replace("anime_bgm_", ""))
                except:
                    pass

            # Update cover mapping
            if db_work_id not in work_covers:
                if bangumi_id:
                    work_covers[db_work_id] = bangumi_id
                elif cover_url:
                    work_covers[db_work_id] = cover_url

            # 1. Dynamic Score Update (even 0.1pt change or newly scored)
            if raw_score > 0 and (old_raw <= 0.0 or abs(old_raw - raw_score) >= 0.1):
                dev_score = round(max(30.0, min(85.0, (raw_score - 65.0) / 9.8 * 10.0 + 50.0)), 1)
                cursor.execute("""
                    UPDATE works SET anilist_raw_score = ?, deviation_score = ? WHERE work_id = ?
                """, (raw_score, dev_score, db_work_id))
                existing["score"] = raw_score
                updated_score_count += 1
                print(f"  [SCORE] {existing['title']} ({db_work_id}): {old_raw} -> {raw_score}")

            # 2. Dynamic Staff & Cast Refresh for airing works or under-staffed recent works
            should_refresh_staff = (
                is_airing
                or (year >= current_year and old_staff_count < 35)
                or (old_staff_count < 15)
            )
            if should_refresh_staff:
                s_dict, c_list, resolved_bgm_id = fetch_bangumi_staff_and_cast(title_ja, subject_id=bangumi_id, year=year)
                if resolved_bgm_id and db_work_id not in work_covers:
                    work_covers[db_work_id] = resolved_bgm_id
                if s_dict or c_list:
                    merged_s = merge_staff_dicts(old_staff_json, s_dict)
                    merged_c = merge_cast_lists(old_chars_json, c_list)
                    new_staff_count = sum(len(v) for v in merged_s.values())
                    if new_staff_count > old_staff_count or len(merged_c) > len(json.loads(old_chars_json or "[]")):
                        new_s_str = json.dumps(merged_s, ensure_ascii=False)
                        new_c_str = json.dumps(merged_c, ensure_ascii=False)
                        cursor.execute("""
                            UPDATE works SET staff_json = ?, characters_json = ? WHERE work_id = ?
                        """, (new_s_str, new_c_str, db_work_id))
                        existing["staff_json"] = new_s_str
                        existing["characters_json"] = new_c_str
                        updated_staff_count += 1
                        print(f"  [STAFF] {existing['title']} ({db_work_id}): スタッフ {old_staff_count}名 -> {new_staff_count}名 / キャスト {len(merged_c)}名")
        else:
            # New work not yet in DB
            work_id = anime["id"]
            s_dict, c_list, resolved_bgm_id = fetch_bangumi_staff_and_cast(title_ja, subject_id=bangumi_id, year=year)
            if resolved_bgm_id:
                work_covers[work_id] = resolved_bgm_id
            elif cover_url:
                work_covers[work_id] = cover_url

            dev_score = round(max(30.0, min(85.0, (raw_score - 65.0) / 9.8 * 10.0 + 50.0)), 1) if raw_score > 0 else 50.0
            tier = "S+" if dev_score >= 70.0 else "S" if dev_score >= 65.0 else "A" if dev_score >= 58.0 else "B" if dev_score >= 50.0 else "C"
            title_norm = norm(title_ja)
            staff_json = json.dumps(s_dict, ensure_ascii=False)
            chars_json = json.dumps(c_list, ensure_ascii=False)
            s_cnt = sum(len(v) for v in s_dict.values())

            cursor.execute("""
            INSERT INTO works (
                work_id, title, title_en, title_norm, year, anilist_raw_score, raw_score_rank,
                debiased_b_i, true_z_score, deviation_score, z_score_rank, predicted_z_score,
                predicted_score, pred_score_rank, residual, performance_verdict, tier,
                percentile, staff_json, characters_json, search_text_norm
            ) VALUES (?, ?, ?, ?, ?, ?, 9999, 0.0, 0.0, ?, 9999, 0.0, ?, 9999, 0.0, 'NEW', ?, 50.0, ?, ?, ?)
            """, (
                work_id, title_ja, title_en, title_norm, year, raw_score,
                dev_score, raw_score, tier, staff_json, chars_json, title_norm
            ))
            new_count += 1
            print(f"  [NEW] 新規作品登録: {title_ja} ({work_id}, {year}年) - Score: {raw_score} / スタッフ: {s_cnt}名 / キャスト: {len(c_list)}名")

            # Add to lookup index so we don't insert duplicates in the same run
            new_info = {
                "work_id": work_id,
                "title": title_ja,
                "title_en": title_en,
                "year": year,
                "score": raw_score,
                "staff_json": staff_json,
                "characters_json": chars_json
            }
            by_id[work_id] = new_info
            by_norm_title[normalize_for_match(title_ja)] = new_info

    # Step 3b: Sweep remaining unevaluated or under-staffed 2026 works in DB
    if not recalc_only:
        print("\n  [3b] DB内の残存2026年作品（スコア未反映・スタッフ少）をスイープ補完中...")
        cursor.execute("""
            SELECT work_id, title, year, anilist_raw_score, staff_json, characters_json
            FROM works
            WHERE year = ? AND (anilist_raw_score <= 0.0 OR length(staff_json) < 800)
        """, (current_year,))
        rem_rows = cursor.fetchall()
    else:
        rem_rows = []
    sweep_count = 0
    for wid, title, yr, sc, s_json, c_json in rem_rows:
        old_sc = float(sc or 0.0)
        old_s_cnt = count_total_staff(s_json)
        # Only query Bangumi directly if it's a bgm work with < 20 staff
        if wid.startswith("anime_bgm_") and old_s_cnt < 20:
            try:
                bid = int(wid.replace("anime_bgm_", ""))
                s_dict, c_list, _ = fetch_bangumi_staff_and_cast(title, subject_id=bid)
                if s_dict or c_list:
                    merged_s = merge_staff_dicts(s_json, s_dict)
                    merged_c = merge_cast_lists(c_json, c_list)
                    new_s_cnt = sum(len(v) for v in merged_s.values())
                    if new_s_cnt > old_s_cnt:
                        cursor.execute("UPDATE works SET staff_json = ?, characters_json = ? WHERE work_id = ?",
                                       (json.dumps(merged_s, ensure_ascii=False), json.dumps(merged_c, ensure_ascii=False), wid))
                        updated_staff_count += 1
                        sweep_count += 1
            except:
                pass
        if sweep_count >= 60:
            break

    conn.commit()
    print(f"  -> 照合完了: スコア更新 {updated_score_count} 件 / スタッフ増分更新 {updated_staff_count} 件 / 新規追加 {new_count} 件")

    summary_lines = [
        "## CreditDB 週次更新レポート",
        "",
        "| 項目 | 件数 |",
        "|---|---|",
        f"| AniList 取得候補 | {len(anilist_list)} |",
        f"| Bangumi Calendar 取得候補 | {len(bgm_cal_list)} |",
        f"| スコア更新 | {updated_score_count} |",
        f"| スタッフ増分更新 | {updated_staff_count} |",
        f"| 新規追加 | {new_count} |",
        f"| AniList クエリ 成功 / 失敗 | {STATS['anilist_ok']} / {STATS['anilist_fail']} |",
        f"| Bangumi スタッフ取得 成功 / 失敗 | {STATS['bgm_ok']} / {STATS['bgm_fail']} |",
        f"| Bangumi 非公開・削除済み (404、主に成人向け) | {STATS['bgm_not_found']} |",
        f"| Bangumi 検索で一致作品なし（誤マッチ防止でスキップ） | {STATS['bgm_no_match']} |",
    ]
    if not recalc_only:
        problems = check_api_health(len(anilist_list), len(bgm_cal_list))
        if problems:
            for p in problems:
                print(f"::error::{p}")
            write_step_summary(summary_lines + ["", "### ❌ 更新中止"] + [f"- {p}" for p in problems])
            print("[ERROR] API 取得に問題があるため、データベースを書き出さずに終了します。")
            sys.exit(1)
    write_step_summary(summary_lines)

    # Step 3c: AniList の話数付きクレジットを取り込む（保存先が指定された場合）
    if args.anilist_store or args.anilist_out:
        import anilist_credits as ac
        store = ac.open_store(Path(args.anilist_store) if args.anilist_store else None)
        if not recalc_only:
            r = refresh_anilist_recent(conn, store, current_year, args.anilist_minutes)
            print(f"  [3c] AniList 話数クレジット再取得: 対象 {r['targets']} / 取得 {r['fetched']} / リクエスト {r['requests']}")
        st = ac.apply_to_creditdb(conn, store)
        n_media = store.execute("SELECT COUNT(*) FROM media").fetchone()[0]
        print(f"  [3c] 話数クレジット反映: {st['works']} 作品 / 既存スタッフに話数付与 {st['attached']} / 追加 {st['added']} (保存済み {n_media} 作品)")
        write_step_summary([f"| AniList 話数クレジット反映 | {st['works']} 作品（話数付与 {st['attached']} / 追加 {st['added']}） |"])
        if args.anilist_out:
            ac.save_store(store, Path(args.anilist_out))

    # Step 4: Recalculate all metrics & resolve staff tiers
    recalculate_all_metrics_and_resolve_tiers(conn)

    print("\n[5/6] サマリー集計 & ZIP 作成中...")
    cursor.execute("SELECT COUNT(*) FROM works")
    total_works = cursor.fetchone()[0]
    cursor.execute("SELECT COUNT(DISTINCT name) FROM leaderboards WHERE role != 'cv'")
    total_staff = cursor.fetchone()[0]
    cursor.execute("SELECT COUNT(DISTINCT name) FROM leaderboards WHERE role = 'cv'")
    total_cv = cursor.fetchone()[0]

    now_str = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    cursor.execute("""
    UPDATE summary SET
        total_works = ?,
        total_staff = ?,
        total_cv = ?,
        updated_at = ?,
        year_max = ?,
        version = '2.3.0'
    """, (total_works, total_staff, total_cv, now_str, current_year))
    conn.commit()

    save_work_covers(conn, work_covers)

    cursor.execute("VACUUM")
    new_db_bytes = conn.serialize()
    conn.close()

    zip_path = out_dir / "creditdb.zip"
    with zipfile.ZipFile(zip_path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as zf:
        zf.writestr("creditdb.db", new_db_bytes)

    zip_size = zip_path.stat().st_size
    sha256_hash = hashlib.sha256(new_db_bytes).hexdigest()
    print(f"  -> creditdb.zip 作成完了: {zip_size / (1024*1024):.2f} MB (SHA-256: {sha256_hash[:12]}...)")

    version_info = {
        "dbVersion": "2.3.0",
        "updatedAt": now_str,
        "totalWorks": int(total_works),
        "totalStaff": int(total_staff),
        "totalCv": int(total_cv),
        "yearMin": 1950,
        "yearMax": int(current_year),
        "globalMean": float(prev_summary[3]),
        "dbFileName": "creditdb.zip",
        "dbSizeCompressed": zip_size,
        "dbSizeUncompressed": len(new_db_bytes),
        "sha256": sha256_hash,
        "workCovers": len(work_covers),
    }
    with open(out_dir / "version.json", "w", encoding="utf-8") as f:
        json.dump(version_info, f, ensure_ascii=False, indent=2)

    print(f"[6/6] 出力完了: {zip_path} / {out_dir / 'version.json'}")
    print("=================================================================")
    print("  週次自動更新・放送中アニメ動的追跡が正常に完了しました！")
    print("=================================================================")

if __name__ == "__main__":
    main()
