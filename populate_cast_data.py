"""
Extracts character and voice actor data from Bangumi dump and populates anime_metadata.json.
"""

import json
import os
import time
import zipfile
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parent.parent
DATA_DIR = ROOT_DIR / "data"
DUMP_ZIP = DATA_DIR / "bangumi_dump.zip"
METADATA_PATH = DATA_DIR / "anime_metadata.json"


def populate_cast():
    print("=" * 70)
    print("  Populating Characters and Voice Actors (Cast) Data")
    print("=" * 70)

    if not DUMP_ZIP.exists():
        raise FileNotFoundError(f"Bangumi dump not found at {DUMP_ZIP}")

    t0 = time.time()
    with open(METADATA_PATH, "r", encoding="utf-8") as f:
        anime_meta = json.load(f)

    # Map bangumi_id (int) -> list of work_ids (some works might share bangumi_id)
    bgm_to_works = {}
    for wid, meta in anime_meta.items():
        bgm_id = meta.get("bangumi_id")
        if bgm_id:
            try:
                b_int = int(bgm_id)
                if b_int not in bgm_to_works:
                    bgm_to_works[b_int] = []
                bgm_to_works[b_int].append(wid)
            except (ValueError, TypeError):
                pass

    target_subjects = set(bgm_to_works.keys())
    print(f"Target Bangumi subjects in metadata: {len(target_subjects)} / {len(anime_meta)} works")

    with zipfile.ZipFile(DUMP_ZIP, "r") as z:
        # 0. subject: read dates for safety check
        print("[0/4] Indexing subject dates for year mismatch protection...")
        subject_dates = {}
        with z.open("subject.jsonlines") as f:
            for line in f:
                obj = json.loads(line.decode("utf-8"))
                sid = obj.get("id")
                if sid in target_subjects:
                    d = obj.get("date") or ""
                    if len(d) >= 4 and d[:4].isdigit():
                        subject_dates[sid] = int(d[:4])

        # 1. subject-characters: (subject_id, character_id) -> type
        print("[1/4] Reading subject-characters.jsonlines...")
        sub_chars = {}  # (s_id, c_id) -> rel_str
        needed_chars = set()
        with z.open("subject-characters.jsonlines") as f:
            for line in f:
                obj = json.loads(line.decode("utf-8"))
                s_id = obj.get("subject_id")
                if s_id in target_subjects:
                    c_id = obj.get("character_id")
                    t = obj.get("type", 2)
                    rel = "主角" if t == 1 else ("配角" if t == 2 else "客串")
                    sub_chars[(s_id, c_id)] = rel
                    needed_chars.add(c_id)
        print(f"  -> Found {len(sub_chars)} subject-character relations.")

        # 2. person-characters: (subject_id, character_id) -> list of person_ids
        print("[2/4] Reading person-characters.jsonlines...")
        sub_char_actors = {}  # (s_id, c_id) -> list of p_ids
        needed_persons = set()
        with z.open("person-characters.jsonlines") as f:
            for line in f:
                obj = json.loads(line.decode("utf-8"))
                s_id = obj.get("subject_id")
                if s_id in target_subjects:
                    c_id = obj.get("character_id")
                    p_id = obj.get("person_id")
                    key = (s_id, c_id)
                    if key not in sub_char_actors:
                        sub_char_actors[key] = []
                    sub_char_actors[key].append(p_id)
                    needed_persons.add(p_id)
        print(f"  -> Found {len(sub_char_actors)} character-actor links ({len(needed_persons)} actors).")

        # 3. character.jsonlines: resolve character names
        print("[3/4] Resolving character names from character.jsonlines...")
        char_names = {}
        with z.open("character.jsonlines") as f:
            for line in f:
                obj = json.loads(line.decode("utf-8"))
                cid = obj.get("id")
                if cid in needed_chars:
                    char_names[cid] = obj.get("name", "")
        print(f"  -> Resolved {len(char_names)} character names.")

        # 4. person.jsonlines: resolve actor names
        print("[4/4] Resolving actor names from person.jsonlines...")
        person_names = {}
        with z.open("person.jsonlines") as f:
            for line in f:
                obj = json.loads(line.decode("utf-8"))
                pid = obj.get("id")
                if pid in needed_persons:
                    person_names[pid] = obj.get("name", "")
        print(f"  -> Resolved {len(person_names)} actor names.")

    # 5. Build characters structure for each subject
    print("\nMerging cast data into anime metadata...")
    rel_priority = {"主角": 0, "配角": 1, "客串": 2}
    chars_by_subject = {}

    for (s_id, c_id), rel in sub_chars.items():
        c_name = char_names.get(c_id, "").strip()
        if not c_name:
            continue
        p_ids = sub_char_actors.get((s_id, c_id), [])
        actor_names = [person_names[pid].strip() for pid in p_ids if pid in person_names and person_names[pid].strip()]

        if not actor_names:
            continue

        if s_id not in chars_by_subject:
            chars_by_subject[s_id] = []

        for a_name in actor_names:
            chars_by_subject[s_id].append({
                "character_name": c_name,
                "relation": rel,
                "actor_name": a_name,
            })

    # Sort each subject's characters by relation priority (主角 first)
    for s_id in chars_by_subject:
        chars_by_subject[s_id].sort(key=lambda x: rel_priority.get(x["relation"], 3))

    # Update anime_meta
    works_with_cast = 0
    total_cast_entries = 0
    all_distinct_actors = set()

    for s_id, wids in bgm_to_works.items():
        s_year = subject_dates.get(s_id)
        cast_list = chars_by_subject.get(s_id, [])
        for wid in wids:
            al_year = anime_meta[wid].get("year")
            # Strict year protection: reject cast injection if release years differ by 3+ years!
            if s_year and al_year and abs(al_year - s_year) >= 3:
                anime_meta[wid].setdefault("characters", [])
                anime_meta[wid].setdefault("staff", {}).setdefault("cv", [])
                continue

            if cast_list:
                works_with_cast += 1
                total_cast_entries += len(cast_list)
                for item in cast_list:
                    all_distinct_actors.add(item["actor_name"])

                anime_meta[wid]["characters"] = cast_list
                staff = anime_meta[wid].setdefault("staff", {})
                cv_names = []
                seen_cv = set()
                for c in cast_list:
                    an = c["actor_name"]
                    if an and an not in seen_cv:
                        seen_cv.add(an)
                        cv_names.append(an)
                staff["cv"] = cv_names
            else:
                anime_meta[wid].setdefault("characters", [])
                anime_meta[wid].setdefault("staff", {}).setdefault("cv", [])

    for wid, meta in anime_meta.items():
        meta.setdefault("characters", [])
        meta.setdefault("staff", {}).setdefault("cv", [])

    print(f"Summary:")
    print(f"  Works with cast data: {works_with_cast} / {len(anime_meta)} ({works_with_cast/len(anime_meta)*100:.1f}%)")
    print(f"  Total cast entries: {total_cast_entries}")
    print(f"  Total distinct voice actors: {len(all_distinct_actors)}")

    print(f"\nWriting to {METADATA_PATH}...")
    with open(METADATA_PATH, "w", encoding="utf-8") as f:
        json.dump(anime_meta, f, ensure_ascii=False, separators=(",", ":"))

    dt = time.time() - t0
    print(f"Successfully populated cast data in {dt:.1f} seconds!")


if __name__ == "__main__":
    populate_cast()
