"""
Audit and Purge Script for Corrupted Metadata & False Match Credits.
Specifically fixes year mismatches, cleans bogus cast/staff, and realigns franchises.
"""

import json
import os
import re
import sys
import zipfile
from pathlib import Path

ROOT_DIR = Path("c:/Users/masak/Desktop/animeevaluate")
DATA_DIR = ROOT_DIR / "data"
DUMP_ZIP = DATA_DIR / "bangumi_dump.zip"
METADATA_PATH = DATA_DIR / "anime_metadata.json"

sys.path.insert(0, str(ROOT_DIR))
from animeevaluate.data.bangumi_client import BangumiClient

# Verified Realign Targets: work_id -> correct bangumi_id
CONFIRMED_REALIGNS = {
    # Crayon Shin-chan classic movies
    "anime_8358": 8966,    # アクション仮面ＶＳハイグレ魔王 (1993, Dir: 本郷みつる)
    "anime_6460": 8971,    # 暗黒タマタマ大追跡 (1997, Dir: 原恵一)
    "anime_8360": 8972,    # 電撃！ブタのヒヅメ大作戦 (1998, Dir: 原恵一)
    "anime_8361": 8973,    # 爆発！温泉わくわく大決戦 (1999, Dir: 原恵一)
    "anime_2450": 8986,    # 嵐を呼ぶモーレツ！オトナ帝国の逆襲 (2001, Dir: 原恵一)

    # Classics & Remakes
    "anime_85": 9622,      # 機動戦士Ζガンダム (1985 TV, Dir: 富野由悠季)
    "anime_404": 20115,    # BASTARD!! -暗黒の破壊神- (1992 OVA, Dir: 秋山勝仁)
    "anime_1482": 488,     # D.Gray-man (2006 TV, Dir: 鍋島修)
    "anime_3038": 67738,   # 千夜一夜物語 (1969, Dir: 手塚治虫/山本暎一)
    "anime_9496": 135452,  # 愛しのベティ 魔物語 (1986)
    "anime_10244": 220061, # ウルトラB ブラックホールからの独裁者B・B (1988)
    "anime_7244": 168100,  # ひゃっかずかん (1989)
    "anime_3306": 97942,   # 青き炎 (1989)
    "anime_16303": 220520, # チックン タックン (1984)
    "anime_11049": 698541, # 新メイプルタウン物語 パームタウン編 こんにちは! 新しい町 (1987)
    "anime_3259": 67771,   # レモンエンジェル (1988)
    "anime_3264": 67771,   # レモンエンジェル (1988/II)
    "anime_6065": 130560,  # レモンエンジェル（ＹＪ版） (1990)
    "anime_2428": 112664,  # 短編ユニコ 黒い雲と白い羽 (1979)
    "anime_7028": 274322,  # ポップ (1974, 久里洋二)
    "anime_7030": 274318,  # ケメ子のLOVE (1968, 久里洋二)
    "anime_6953": 212454,  # 人間動物園 (1962, 久里洋二)
    "anime_5225": 10387,   # ルパン三世 Pilot Film (1969)
    "anime_5289": 415762,  # 映画 ビーストウォーズII ライオコンボイ危機一髪! (1998)
    "anime_153841": 396988,# ツルネ －つながりの一射－ (2023)
    "anime_7771": 115885,  # 小さなジャンボ (1977)
    "anime_121651": 220064,# 100ばんめのサル (1986)
    "anime_8149": 40379,   # ゲゲゲの鬼太郎 (1968 TV)
    "anime_5688": 40379,   # ゲゲゲの鬼太郎 (1968 TV)
    "anime_6971": 133510,  # ゲゲゲの鬼太郎 (1971 TV)
    "anime_2892": 53740,   # ハクション大魔王 (1969 TV)
}

def clean_title(t):
    if not t: return ""
    t = re.sub(r"[\(（\[【].*?[\)）\]】]", " ", t)
    t = re.sub(r"^(劇場版|映画|アニメ|TVアニメ|OVA)\s*", "", t)
    t = re.sub(r"[\s\:\-\_・☆★~～!！?？.．/／,，]+", "", t).lower()
    return t

def build_staff_from_persons(persons, subject_id, subject_name):
    staff = {
        "director": [], "series_comp": [], "char_design": [], "sakkan": [],
        "genga": [], "unit_director": [], "music": [], "art_dir": [],
        "studio": [], "bangumi_id": subject_id, "bangumi_name": subject_name,
        "cv": []
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
                staff["director"].append(name); seen_roles["director"].add(name)
        elif relation in ("系列构成", "脚本"):
            if name not in seen_roles["series_comp"]:
                staff["series_comp"].append(name); seen_roles["series_comp"].add(name)
        elif relation in ("人物设定", "人物原案"):
            if name not in seen_roles["char_design"]:
                staff["char_design"].append(name); seen_roles["char_design"].add(name)
        elif relation in ("总作画监督", "作画监督", "动作作画监督", "机械作画监督", "角色作画监督"):
            if name not in seen_roles["sakkan"]:
                staff["sakkan"].append(name); seen_roles["sakkan"].add(name)
        elif relation == "原画":
            if name not in genga_set:
                genga_set.add(name)
                staff["genga"].append({"name": name, "weight": 1.0, "ep_ratio": 1.0})
        elif relation in ("分镜", "演出", "副导演"):
            if name not in seen_roles["unit_director"]:
                staff["unit_director"].append(name); seen_roles["unit_director"].add(name)
        elif relation == "音乐":
            if name not in seen_roles["music"]:
                staff["music"].append(name); seen_roles["music"].add(name)
        elif relation in ("美术监督", "美术设计", "背景美术"):
            if name not in seen_roles["art_dir"]:
                staff["art_dir"].append(name); seen_roles["art_dir"].add(name)
        elif relation == "动画制作":
            if name not in seen_roles["studio"]:
                staff["studio"].append(name); seen_roles["studio"].add(name)

    return staff


def build_characters_from_chars(characters_data):
    chars = []
    seen = set()
    for item in characters_data:
        c_name = item.get("name", "").strip()
        rel_int = item.get("relation", 2)
        rel = "主角" if rel_int == 1 else ("配角" if rel_int == 2 else "客串")
        actors = item.get("actors", [])
        for a in actors:
            a_name = a.get("name", "").strip()
            if not a_name:
                continue
            key = (c_name, a_name)
            if key not in seen:
                seen.add(key)
                chars.append({
                    "character_name": c_name,
                    "relation": rel,
                    "actor_name": a_name
                })
    return chars


def main():
    print("=" * 70)
    print("  Realigning & Purging Contaminated Credits from Anime Metadata")
    print("=" * 70)

    with open(METADATA_PATH, "r", encoding="utf-8") as f:
        meta = json.load(f)

    print(f"Loaded {len(meta)} anime works from metadata.")

    # 1. Load subjects from bangumi_dump.zip to get official release years
    print("\n[Step 1/4] Indexing Bangumi subjects from dump...")
    bgm_dates = {}
    bgm_names = {}
    with zipfile.ZipFile(DUMP_ZIP, "r") as z:
        with z.open("subject.jsonlines") as f:
            for line in f:
                obj = json.loads(line.decode("utf-8"))
                sid = obj.get("id")
                date = obj.get("date") or ""
                if sid and date and len(date) >= 4 and date[:4].isdigit():
                    bgm_dates[sid] = int(date[:4])
                if sid and obj.get("name"):
                    bgm_names[sid] = obj.get("name")

    print(f"Indexed {len(bgm_dates)} subject dates from dump.")

    # 2. Identify all works with year discrepancy >= 3
    print("\n[Step 2/4] Auditing all works for year discrepancies...")
    mismatched_works = set()
    for wid, w in meta.items():
        bid = w.get("bangumi_id")
        al_year = w.get("year")
        if not bid or not al_year:
            continue
        try:
            bid = int(bid)
        except (ValueError, TypeError):
            continue

        b_year = bgm_dates.get(bid)
        if b_year and abs(al_year - b_year) >= 3:
            mismatched_works.add(wid)

    print(f"Found {len(mismatched_works)} works with year discrepancy >= 3.")

    cache_dir = ROOT_DIR / "data" / "bangumi_cache"
    client = BangumiClient(cache_dir=str(cache_dir), request_delay=0.1)

    realigned_count = 0
    purged_count = 0

    # 3. Apply Realigns and Disconnects
    print("\n[Step 3/4] Processing realignments and purges...")
    for wid in list(mismatched_works):
        w = meta[wid]
        al_title = w.get("title", "")
        al_year = w.get("year")
        old_bid = w.get("bangumi_id")

        if wid in CONFIRMED_REALIGNS:
            new_bid = CONFIRMED_REALIGNS[wid]
            print(f"[REALIGN] {wid} '{al_title}' ({al_year}): BGM {old_bid} -> BGM {new_bid}")
            
            # Fetch true staff & characters
            persons = client.get_subject_persons(new_bid)
            subject_name = bgm_names.get(new_bid, al_title)
            new_staff = build_staff_from_persons(persons, new_bid, subject_name)
            
            # Preserve existing studio if new staff has no studio
            if not new_staff.get("studio") and w.get("staff", {}).get("studio"):
                new_staff["studio"] = w["staff"]["studio"]

            # Characters
            chars_raw = client.get_subject_characters(new_bid)
            new_chars = build_characters_from_chars(chars_raw)

            cv_set = []
            for c in new_chars:
                an = c["actor_name"]
                if an and an not in cv_set:
                    cv_set.append(an)
            new_staff["cv"] = cv_set

            w["bangumi_id"] = new_bid
            w["source_url"] = f"https://bangumi.tv/subject/{new_bid}"
            w["staff"] = new_staff
            w["characters"] = new_chars
            realigned_count += 1
        else:
            # DISCONNECT & PURGE
            print(f"[PURGE] {wid} '{al_title}' ({al_year}): Disconnecting false BGM {old_bid}")
            preserved_studio = w.get("staff", {}).get("studio", [])
            w["bangumi_id"] = None
            w["source_url"] = ""
            w["staff"] = {
                "director": [], "series_comp": [], "char_design": [], "sakkan": [],
                "genga": [], "unit_director": [], "music": [], "art_dir": [],
                "studio": preserved_studio, "bangumi_id": None, "bangumi_name": "",
                "cv": []
            }
            w["characters"] = []
            purged_count += 1

    print(f"\nProcessing Summary:")
    print(f"  Realigned works: {realigned_count}")
    print(f"  Purged/Disconnected works: {purged_count}")

    # 4. Global Sanity Sweep on all 5,452 works:
    print("\n[Step 4/4] Global sanity verification across all works...")
    remaining_severe = 0
    kito_old_works = []
    for wid, w in meta.items():
        bid = w.get("bangumi_id")
        al_year = w.get("year")
        if bid and al_year:
            b_year = bgm_dates.get(int(bid)) if str(bid).isdigit() else None
            if b_year and abs(al_year - b_year) >= 3:
                remaining_severe += 1
                print(f"  [STILL MISMATCHED] {wid} '{w.get('title')}' ({al_year}) vs BGM {bid} ({b_year})")

        # Check if 鬼頭明里 still appears in pre-2014 works
        for c in w.get("characters", []):
            if c.get("actor_name") == "鬼頭明里" and al_year and al_year < 2014:
                kito_old_works.append((wid, w.get("title"), al_year))

    print(f"Remaining works with year discrepancy >= 3: {remaining_severe}")
    print(f"Pre-2014 works with 鬼頭明里: {len(kito_old_works)}")

    # Backup original metadata before overwriting
    backup_path = DATA_DIR / "anime_metadata.json.bak"
    if not backup_path.exists():
        import shutil
        shutil.copyfile(METADATA_PATH, backup_path)
        print(f"Created backup at {backup_path}")

    # Save cleaned metadata
    print(f"\nSaving cleaned metadata to {METADATA_PATH}...")
    with open(METADATA_PATH, "w", encoding="utf-8") as f:
        json.dump(meta, f, ensure_ascii=False, separators=(",", ":"))

    sz_mb = os.path.getsize(METADATA_PATH) / (1024 * 1024)
    print(f"Saved {METADATA_PATH} ({sz_mb:.2f} MB). Cleanse complete!")

if __name__ == "__main__":
    main()
