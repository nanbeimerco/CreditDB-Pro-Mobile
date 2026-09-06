"""
Audit and Realignment Script for AniList - Bangumi Metadata.
Fixes all franchise/remake collisions, subtitle mismatches, and year-offset discrepancies.
"""

import json
import logging
import os
import sys
import time
from pathlib import Path
from typing import Any, Dict, Optional

# Ensure project root is in sys.path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from animeevaluate.data.bangumi_client import BangumiClient

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
)
logger = logging.getLogger("RealignBangumi")

# Canonical known fixes for franchise / remake collisions
CANONICAL_FIXES = {
    # 1. Chainsaw Man Reze Arc
    "anime_171627": 470660,  # 劇場版 チェンソーマン レゼ篇 (2025, Dir: 吉原達矢, Studio: MAPPA)
    "anime_127230": 321885,  # チェンソーマン TV (2022, Dir: 中山竜)
    "chainsaw_man_2022": 321885,

    # 2. Kimagure Orange Road
    "anime_2454": 13677,   # きまぐれオレンジ☆ロード あの日にかえりたい (1988, Dir: 望月智充, Char: 高田明美)
    "anime_2098": 13679,   # 新きまぐれオレンジ☆ロード ～ そして、あの夏のはじまり (1996, Dir: 湯山邦彦, Char: 後藤隆幸)
    "anime_1087": 2042,    # きまぐれオレンジ☆ロード TV (1987, Dir: 小林治, Char: 高田明美)
    "anime_2458": 13674,   # きまぐれオレンジ☆ロード OVA (1989)
    "anime_3394": 57010,   # きまぐれオレンジ☆ロード 少年ジャンプ・スペシャル (1985, Dir: 望月智充)

    # 3. Ranma 1/2
    "anime_210": 2789,     # らんま1/2 TV (1989, Dir: 芝山努, Char: 中嶋敦子, Studio: ディーン)
    "anime_149939": 65883, # らんま1/2 熱闘編 (1989, Dir: 西村純二/澤井幸次)
    "anime_178533": 489820,# らんま1/2 (2024 TV, Dir: 宇田鋼之介, Char: 谷口宏美, Studio: MAPPA)
    "anime_185731": 529966,# らんま1/2 (2024) 第2期 (2025)
    "anime_1007": 72337,   # Ranma 1/2 (OVA 1993, Dir: 芝山努, Char: 中嶋敦子, Studio: ディーン)
    "anime_1008": 37020,   # らんま1/2 スペシャル (OVA 1994, Dir: 西村純二, Char: 中嶋敦子, Studio: ディーン)
    "anime_1011": 72342,   # らんま1/2 SUPER (OVA 1995, Dir: 井内秀治/西村純二, Char: 中嶋敦子, Studio: ディーン)
    "anime_792": 22509,    # らんま1/2 決戦桃幻郷! (Movie 1992, Dir: 鈴木行)
    "anime_1010": 22510,   # らんま1/2 超無差別決戦! (Movie 1994, Dir: 西村純二)
    "anime_418": 22508,    # らんま1/2 中国寝崑崙大決戦! (Movie 1991, Dir: 井内秀治)

    # 4. Classic vs Modern Remakes
    "anime_2116": 5426,    # キャプテン翼 (1983 TV, Dir: 光延博愛, Char: 岡迫亘弘, Studio: 土田プロ)
    "anime_1293": 10388,   # うる星やつら (1981 TV, Dir: 押井守 / やまざきかずお, Char: 高田明美, Studio: ぴえろ/ディーン)
    "anime_101347": 240838,# どろろ (2019 TV, Dir: 古橋一浩, Char: 岩瀧智, Studio: MAPPA)
    "anime_5688": 3217,    # ゲゲゲの鬼太郎 (1968 TV)
    "anime_8149": 3218,    # ゲゲゲの鬼太郎 (1971 TV)
    "anime_6971": 3218,    # ゲゲゲの鬼太郎 (1971 TV)
    "anime_2892": 3226,    # ハクション大魔王 (1969 TV)
}

# Absurd false positive matches to disconnect completely
DISCONNECT_IDS = {
    "anime_104417",  # 1954 シンテリヤ嬢の花婿 -> Falsely matched シン・エヴァンゲリオン
    "anime_143156",  # 1972 MADE IN JAPAN -> Falsely matched メイドインアビス
    "anime_109352",  # 1961 かぐや姫 -> Falsely matched 2026 超かぐや姫
    "anime_104409",  # 1960 かがみ -> Falsely matched 2022 かがみの孤城
    "anime_145750",  # 1961 おんぷ -> Falsely matched 2021 ソードアート・オンライン
    "anime_116712",  # 1963 時間 -> Falsely matched 2018 踏切時間
    "anime_139932",  # 1969 ゼンちゃんツーちゃん -> Falsely matched 2022 禅 グローグー
    "anime_102076",  # 1975 優しい金曜日 -> Falsely matched 2026 オタクに優しいギャル
    "anime_7020",    # 1960 ファッション -> Falsely matched Barbie
    "anime_143306",  # 1951 聖書幻想譜 -> Falsely matched 1997 手塚治虫の旧約聖書物語
    "anime_117801",  # 1976 ベルとかいじゅう王子 -> Falsely matched 2021 竜とそばかすの姫
    "anime_116706",  # 1966 追跡 -> Falsely matched 2009 名探偵コナン 漆黒の追跡者
    "anime_2607",    # 1984 バース -> Falsely matched 2025 新星ギャルバース
    "anime_6950",    # 1964 アオス -> Falsely matched 2002 藍より青し
    "anime_169765",  # 1985 柳水華苑 -> Falsely matched 2022 Dr.STONE 龍水
    "anime_130366",  # 1988 わくわくどきどき めいさくわ～るど -> Falsely matched ハズレ枠
    "anime_121880",  # 1988 にんげんの詩 -> Falsely matched 2023 人間不信の冒険者
    "anime_101432",  # 2018 ヴァイオレット・エヴァーガーデン -> Falsely matched 1984 超時空要塞マクロス
}


def build_staff_from_subject(bangumi: BangumiClient, subject_id: int, subject_name: str) -> Dict[str, Any]:
    persons = bangumi.get_subject_persons(subject_id)
    staff: Dict[str, Any] = {
        "director": [],
        "series_comp": [],
        "char_design": [],
        "sakkan": [],
        "genga": [],
        "unit_director": [],
        "music": [],
        "art_dir": [],
        "studio": [],
        "bangumi_id": subject_id,
        "bangumi_name": subject_name,
    }

    genga_set = set()
    seen_roles = {k: set() for k in staff if isinstance(staff[k], list)}

    for p in persons:
        name = p.get("name", "").strip()
        relation = p.get("relation", "").strip()
        if not name or not relation:
            continue

        if relation in ("总导演", "导演"):
            if name not in seen_roles["director"]:
                staff["director"].append(name)
                seen_roles["director"].add(name)

        elif relation in ("系列构成", "脚本"):
            if name not in seen_roles["series_comp"]:
                staff["series_comp"].append(name)
                seen_roles["series_comp"].add(name)

        elif relation in ("人物设定", "人物原案"):
            if name not in seen_roles["char_design"]:
                staff["char_design"].append(name)
                seen_roles["char_design"].add(name)

        elif relation in ("总作画监督", "作画监督", "动作作画监督", "机械作画监督", "角色作画监督"):
            if name not in seen_roles["sakkan"]:
                staff["sakkan"].append(name)
                seen_roles["sakkan"].add(name)

        elif relation in ("原画", "第二原画"):
            if name not in genga_set:
                genga_set.add(name)
                staff["genga"].append({"name": name, "weight": 1.0, "ep_ratio": 1.0})

        elif relation in ("演出", "分镜"):
            if name not in seen_roles["unit_director"]:
                staff["unit_director"].append(name)
                seen_roles["unit_director"].add(name)

        elif relation in ("音乐",):
            if name not in seen_roles["music"]:
                staff["music"].append(name)
                seen_roles["music"].add(name)

        elif relation in ("美术监督",):
            if name not in seen_roles["art_dir"]:
                staff["art_dir"].append(name)
                seen_roles["art_dir"].add(name)

        elif relation in ("动画制作",):
            if name not in seen_roles["studio"]:
                staff["studio"].append(name)
                seen_roles["studio"].add(name)

    return staff


def main():
    root_dir = Path(__file__).resolve().parent.parent
    meta_path = root_dir / "data" / "anime_metadata.json"
    cache_dir = root_dir / "data" / "bangumi_cache"

    with open(meta_path, "r", encoding="utf-8") as f:
        meta = json.load(f)

    logger.info(f"Loaded {len(meta)} anime works from {meta_path}")

    bangumi = BangumiClient(cache_dir=cache_dir)

    # 1. Apply Canonical Hard Fixes
    logger.info("Applying canonical fixes for franchise / remake collisions...")
    for wid, sid in CANONICAL_FIXES.items():
        if wid in meta:
            w = meta[wid]
            old_sid = w.get("bangumi_id")
            old_dir = (w.get("staff") or {}).get("director")

            # Fetch correct staff for canonical subject ID
            s_name = w.get("title", "")
            new_staff = build_staff_from_subject(bangumi, sid, s_name)
            if not new_staff.get("studio") and w.get("staff", {}).get("studio"):
                new_staff["studio"] = w["staff"]["studio"]

            if wid in ("anime_1008", "anime_1011", "anime_1007"):
                if not new_staff.get("char_design"):
                    new_staff["char_design"] = ["中嶋敦子"]
                new_staff["studio"] = ["スタジオディーン"]
                if wid == "anime_1008" and not new_staff.get("director"):
                    new_staff["director"] = ["西村純二"]

            w["bangumi_id"] = sid
            w["source_url"] = f"https://bangumi.tv/subject/{sid}"
            w["staff"] = new_staff

            logger.info(f"Fixed {wid} ({w.get('title')}): ID {old_sid} -> {sid} | Director: {old_dir} -> {new_staff.get('director')}")

    # 2. Apply Disconnections for absurd false hits
    logger.info("Disconnecting absurd false positive matches...")
    for wid in DISCONNECT_IDS:
        if wid in meta:
            w = meta[wid]
            old_sid = w.get("bangumi_id")
            w["bangumi_id"] = None
            w["source_url"] = ""
            w["staff"] = {
                "director": [], "series_comp": [], "char_design": [], "sakkan": [],
                "genga": [], "unit_director": [], "music": [], "art_dir": [],
                "studio": w.get("staff", {}).get("studio", []),
                "bangumi_id": None, "bangumi_name": "",
                "cv": [],
            }
            w["characters"] = []
            logger.info(f"Disconnected {wid} ({w.get('title')}) from false Bangumi ID {old_sid}")

    # 3. Scan all remaining works for year discrepancies >= 2 years
    logger.info("Auditing remaining works for year discrepancies...")
    realigned_count = 0
    cleared_count = 0

    # Build bgm date map
    bgm_dates = {}
    for fpath in cache_dir.glob("search_*.json"):
        try:
            with open(fpath, "r", encoding="utf-8") as fl:
                d = json.load(fl)
                if isinstance(d, dict) and "id" in d and d.get("date"):
                    bgm_dates[d["id"]] = d["date"][:4]
        except Exception:
            pass

    for wid, w in meta.items():
        if wid in CANONICAL_FIXES or wid in DISCONNECT_IDS:
            continue

        sid = w.get("bangumi_id")
        if not sid:
            continue

        al_year = w.get("year")
        al_title = w.get("title") or w.get("title_en") or ""
        al_eps = w.get("episodes", 1)
        al_format = w.get("format", "TV")

        bgm_year_str = bgm_dates.get(sid)
        if not bgm_year_str or not bgm_year_str.isdigit() or not al_year:
            continue

        diff = abs(int(bgm_year_str) - al_year)
        if diff >= 2:
            # Stale or mis-matched! Re-query with new search engine!
            logger.info(f"Re-evaluating {wid} '{al_title}' ({al_year}) vs Bangumi ID {sid} ({bgm_year_str}, diff={diff}y)...")
            match = bangumi.search_subject(
                title=al_title,
                year=al_year,
                episodes=al_eps,
                format_type=al_format,
            )
            if not match and w.get("title_en") and w["title_en"] != al_title:
                match = bangumi.search_subject(
                    title=w["title_en"],
                    year=al_year,
                    episodes=al_eps,
                    format_type=al_format,
                )

            if match and match.get("id"):
                new_sid = match["id"]
                new_date = (match.get("date") or "")[:4]
                new_diff = abs(int(new_date) - al_year) if new_date.isdigit() else 0
                if new_diff <= 1:
                    new_staff = build_staff_from_subject(bangumi, new_sid, match.get("name", al_title))
                    if not new_staff.get("studio") and w.get("staff", {}).get("studio"):
                        new_staff["studio"] = w["staff"]["studio"]
                    w["bangumi_id"] = new_sid
                    w["source_url"] = f"https://bangumi.tv/subject/{new_sid}"
                    w["staff"] = new_staff
                    realigned_count += 1
                    logger.info(f"  -> Successfully realigned to ID {new_sid} ({match.get('name')}, {new_date})")
                else:
                    # Still too far away, disconnect
                    w["bangumi_id"] = None
                    w["source_url"] = ""
                    w["staff"] = {
                        "director": [], "series_comp": [], "char_design": [], "sakkan": [],
                        "genga": [], "unit_director": [], "music": [], "art_dir": [],
                        "studio": w.get("staff", {}).get("studio", []),
                        "bangumi_id": None, "bangumi_name": "",
                        "cv": [],
                    }
                    w["characters"] = []
                    cleared_count += 1
                    logger.info(f"  -> Disconnected false match (closest candidate {new_sid} is {new_date}, diff={new_diff}y)")
            else:
                # No valid candidate satisfies threshold
                w["bangumi_id"] = None
                w["source_url"] = ""
                w["staff"] = {
                    "director": [], "series_comp": [], "char_design": [], "sakkan": [],
                    "genga": [], "unit_director": [], "music": [], "art_dir": [],
                    "studio": w.get("staff", {}).get("studio", []),
                    "bangumi_id": None, "bangumi_name": "",
                    "cv": [],
                }
                w["characters"] = []
                cleared_count += 1
                logger.info(f"  -> No valid match found for {wid}; cleared false staff.")

    logger.info(f"Audit Summary: Realigned: {realigned_count}, Disconnected/Cleared: {cleared_count}")

    # Save compact metadata (< 25MB)
    with open(meta_path, "w", encoding="utf-8") as f:
        json.dump(meta, f, ensure_ascii=False, separators=(",", ":"))

    sz = os.path.getsize(meta_path)
    logger.info(f"Saved updated metadata to {meta_path} ({sz / (1024*1024):.2f} MB)")


if __name__ == "__main__":
    main()
