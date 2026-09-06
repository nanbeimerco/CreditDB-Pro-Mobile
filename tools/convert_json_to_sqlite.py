import json
import sqlite3
import os
import re
import sys
import unicodedata

def normalize_text(text: str) -> str:
    if not text:
        return ""
    s = str(text).lower()
    # Zenkaku to Hankaku alphanumeric
    s = unicodedata.normalize('NFKC', s)
    # Katakana to Hiragana
    res = []
    for c in s:
        code = ord(c)
        if 0x30A1 <= code <= 0x30F6:
            res.append(chr(code - 0x60))
        elif c in "ゐゑゔ":
            if c == "ゐ": res.append("い")
            elif c == "ゑ": res.append("え")
            elif c == "ゔ": res.append("ぶ")
        else:
            res.append(c)
    s = "".join(res)
    # Remove punctuation & whitespaces
    s = re.sub(r'[\s\-_・:：,，.．!！?？/／★☆♪〜~・\(\)（）「」『』\[\]【】]', '', s)
    return s

def main():
    base_dir = os.path.dirname(os.path.abspath(__file__))
    source_dir = os.path.join(base_dir, "..", "..", "creditdb", "docs", "data")
    dest_db = os.path.join(base_dir, "..", "app", "src", "main", "assets", "creditdb.db")

    os.makedirs(os.path.dirname(dest_db), exist_ok=True)
    if os.path.exists(dest_db):
        os.remove(dest_db)

    conn = sqlite3.connect(dest_db)
    cursor = conn.cursor()

    # Enable fast insertion
    cursor.execute("PRAGMA synchronous = OFF;")
    cursor.execute("PRAGMA journal_mode = MEMORY;")

    print("Creating tables...")

    # 1. Summary
    cursor.execute("""
    CREATE TABLE summary (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        total_works INTEGER NOT NULL,
        total_staff INTEGER NOT NULL,
        year_min INTEGER NOT NULL,
        year_max INTEGER NOT NULL,
        updated_at TEXT NOT NULL,
        version TEXT NOT NULL
    );
    """)

    # 2. Works
    cursor.execute("""
    CREATE TABLE works (
        id TEXT PRIMARY KEY,
        title TEXT NOT NULL,
        title_en TEXT,
        title_norm TEXT NOT NULL,
        year INTEGER NOT NULL,
        deviation_score REAL NOT NULL,
        deviation_rank INTEGER NOT NULL,
        anilist_raw_score REAL NOT NULL,
        raw_rank INTEGER NOT NULL,
        tier TEXT NOT NULL,
        percentile REAL NOT NULL,
        staff_json TEXT NOT NULL,
        staff_names_norm TEXT NOT NULL
    );
    """)
    cursor.execute("CREATE INDEX idx_works_dev_desc ON works(deviation_score DESC);")
    cursor.execute("CREATE INDEX idx_works_dev_asc ON works(deviation_score ASC);")
    cursor.execute("CREATE INDEX idx_works_raw_desc ON works(anilist_raw_score DESC);")
    cursor.execute("CREATE INDEX idx_works_raw_asc ON works(anilist_raw_score ASC);")
    cursor.execute("CREATE INDEX idx_works_year_desc ON works(year DESC);")
    cursor.execute("CREATE INDEX idx_works_year_asc ON works(year ASC);")
    cursor.execute("CREATE INDEX idx_works_tier ON works(tier);")
    cursor.execute("CREATE INDEX idx_works_title ON works(title);")

    # 3. Leaderboards
    cursor.execute("""
    CREATE TABLE leaderboards (
        role TEXT NOT NULL,
        name TEXT NOT NULL,
        name_norm TEXT NOT NULL,
        works_count INTEGER NOT NULL,
        rating REAL NOT NULL,
        cumulative_z REAL NOT NULL,
        rating_rank INTEGER NOT NULL,
        cumulative_rank INTEGER NOT NULL,
        rating_tier TEXT NOT NULL,
        cumulative_tier TEXT NOT NULL,
        best_work_title TEXT,
        best_work_year INTEGER,
        best_work_z REAL,
        PRIMARY KEY (role, name)
    );
    """)
    cursor.execute("CREATE INDEX idx_lb_role_rating ON leaderboards(role, rating_rank);")
    cursor.execute("CREATE INDEX idx_lb_role_cum ON leaderboards(role, cumulative_rank);")
    cursor.execute("CREATE INDEX idx_lb_name ON leaderboards(name);")
    cursor.execute("CREATE INDEX idx_lb_name_norm ON leaderboards(name_norm);")

    # 4. Profiles
    cursor.execute("""
    CREATE TABLE profiles (
        name TEXT PRIMARY KEY,
        name_norm TEXT NOT NULL,
        primary_role TEXT NOT NULL,
        total_works INTEGER NOT NULL,
        bayesian_rating REAL NOT NULL,
        overall_rating_tier TEXT NOT NULL,
        overall_rank INTEGER NOT NULL,
        career_cumulative_z REAL NOT NULL,
        overall_cum_tier TEXT NOT NULL,
        cumulative_rank INTEGER NOT NULL,
        all_role_stats_json TEXT NOT NULL,
        best_works_json TEXT NOT NULL,
        career_trajectory_json TEXT NOT NULL
    );
    """)

    # Populate Summary
    summary_path = os.path.join(source_dir, "summary.json")
    if os.path.exists(summary_path):
        with open(summary_path, "r", encoding="utf-8") as f:
            sdata = json.load(f)
            cursor.execute("""
            INSERT INTO summary (total_works, total_staff, year_min, year_max, updated_at, version)
            VALUES (?, ?, ?, ?, ?, ?)
            """, (
                sdata.get("total_works", 5452),
                sdata.get("total_staff", 22896),
                sdata.get("year_min", 1950),
                sdata.get("year_max", 2026),
                sdata.get("updated_at", "2026-09-04 17:58:01"),
                sdata.get("version", "1.1.0")
            ))
        print("Summary loaded.")

    # Populate Works
    works_path = os.path.join(source_dir, "works.json")
    print("Loading works.json...")
    with open(works_path, "r", encoding="utf-8") as f:
        works_data = json.load(f)
    
    print(f"Inserting {len(works_data)} works...")
    works_rows = []
    for w in works_data:
        staff_dict = w.get("staff", {})
        names_list = []
        for rk, members in staff_dict.items():
            for m in members:
                if isinstance(m, dict):
                    names_list.append(m.get("name", ""))
                elif isinstance(m, str):
                    names_list.append(m)
        staff_names_norm = normalize_text(" ".join(names_list))
        title_norm = normalize_text(w.get("title", "") + " " + (w.get("title_en") or ""))

        works_rows.append((
            w["id"],
            w["title"],
            w.get("title_en", ""),
            title_norm,
            w["year"],
            w["deviation_score"],
            w["deviation_rank"],
            w["anilist_raw_score"],
            w["raw_rank"],
            w["tier"],
            w["percentile"],
            json.dumps(staff_dict, ensure_ascii=False),
            staff_names_norm
        ))
    cursor.executemany("""
    INSERT INTO works (id, title, title_en, title_norm, year, deviation_score, deviation_rank,
                       anilist_raw_score, raw_rank, tier, percentile, staff_json, staff_names_norm)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, works_rows)
    print("Works inserted.")

    # Populate Leaderboards
    lb_path = os.path.join(source_dir, "leaderboards.json")
    print("Loading leaderboards.json...")
    with open(lb_path, "r", encoding="utf-8") as f:
        lb_data = json.load(f)

    lb_rows = []
    total_lb_items = 0
    for role_key, role_obj in lb_data.items():
        items = role_obj.get("items", [])
        total_lb_items += len(items)
        for it in items:
            name = it["n"]
            name_norm = normalize_text(name)
            lb_rows.append((
                role_key,
                name,
                name_norm,
                it["w"],
                it["r"],
                it["z"],
                it["rk"],
                it["ck"],
                it["rt"],
                it["ct"],
                it.get("bt"),
                it.get("by"),
                it.get("bz")
            ))
    print(f"Inserting {len(lb_rows)} leaderboard entries...")
    cursor.executemany("""
    INSERT INTO leaderboards (role, name, name_norm, works_count, rating, cumulative_z,
                              rating_rank, cumulative_rank, rating_tier, cumulative_tier,
                              best_work_title, best_work_year, best_work_z)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, lb_rows)
    print("Leaderboards inserted.")

    # Populate Profiles
    prof_path = os.path.join(source_dir, "profiles.json")
    print("Loading profiles.json...")
    with open(prof_path, "r", encoding="utf-8") as f:
        prof_data = json.load(f)

    print(f"Inserting {len(prof_data)} staff profiles...")
    prof_rows = []
    for name, p in prof_data.items():
        name_norm = normalize_text(p["name"])
        prof_rows.append((
            p["name"],
            name_norm,
            p.get("primary_role", "genga"),
            p.get("total_works", 0),
            p.get("bayesian_rating", 0.0),
            p.get("overall_rating_tier", "B"),
            p.get("overall_rank", 99999),
            p.get("career_cumulative_z", 0.0),
            p.get("overall_cum_tier", "B"),
            p.get("cumulative_rank", 99999),
            json.dumps(p.get("all_role_stats", []), ensure_ascii=False),
            json.dumps(p.get("best_works", []), ensure_ascii=False),
            json.dumps(p.get("career_trajectory", []), ensure_ascii=False)
        ))
    cursor.executemany("""
    INSERT INTO profiles (name, name_norm, primary_role, total_works, bayesian_rating,
                          overall_rating_tier, overall_rank, career_cumulative_z, overall_cum_tier,
                          cumulative_rank, all_role_stats_json, best_works_json, career_trajectory_json)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, prof_rows)
    print("Profiles inserted.")

    conn.commit()
    conn.close()

    db_size = os.path.getsize(dest_db) / (1024 * 1024)
    print(f"Successfully created SQLite database: {dest_db} ({db_size:.2f} MB)")

if __name__ == "__main__":
    main()
