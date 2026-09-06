import json
import sqlite3
import os
import re
import sys
import unicodedata
from pathlib import Path

# Add project root to sys.path
root_dir = Path(__file__).resolve().parent.parent.parent
sys.path.insert(0, str(root_dir))

from animeevaluate.pipeline import AnimePipeline
from animeevaluate.models.staff_evaluator import calculate_tier, DEFAULT_ROLE_M

def normalize_text(text: str) -> str:
    if not text:
        return ""
    s = str(text).lower()
    s = unicodedata.normalize('NFKC', s)
    res = []
    for ch in s:
        code = ord(ch)
        if ch in ('ゐ', 'ヰ'): res.append('い')
        elif ch in ('ゑ', 'ヱ'): res.append('え')
        elif ch in ('ゔ', 'ヴ'): res.append('ぶ')
        elif 0x30A1 <= code <= 0x30F6:
            res.append(chr(code - 0x60))
        else:
            res.append(ch)
    s = "".join(res)
    s = re.sub(r'[\s\-_・:：,，.．!！?？/／★☆♪〜~・\(\)（）「」『』\[\]【】]', '', s)
    return s

def main():
    print("Initializing AnimePipeline and loading fast cache...")
    p = AnimePipeline()
    p.fast_load()

    # Train predictor if needed to get tree model
    if not p.is_predictor_ready and not p.is_trained:
        print("Training quality predictor model...")
        p.train()

    assets_dir = os.path.join(str(root_dir), "CreditDB Pro for Android", "app", "src", "main", "assets")
    os.makedirs(assets_dir, exist_ok=True)

    # 0. Export LightGBM tree model to predictor_model.json
    model_json_path = os.path.join(assets_dir, "predictor_model.json")
    print(f"Exporting predictor tree model to {model_json_path}...")
    if hasattr(p.predictor, "model") and hasattr(p.predictor.model, "booster_"):
        model_dump = p.predictor.model.booster_.dump_model()
        model_data = {
            "feature_names": p.predictor.feature_names,
            "global_mean": float(p.bias_model.global_mean),
            "role_m": DEFAULT_ROLE_M,
            "tree_info": [
                {"tree_structure": t["tree_structure"]}
                for t in model_dump.get("tree_info", [])
            ]
        }
        with open(model_json_path, "w", encoding="utf-8") as f:
            json.dump(model_data, f, ensure_ascii=False)
        print(f"Exported {len(model_data['tree_info'])} trees to predictor_model.json.")

    dest_db = os.path.join(assets_dir, "creditdb.db")
    if os.path.exists(dest_db):
        os.remove(dest_db)

    conn = sqlite3.connect(dest_db)
    cursor = conn.cursor()
    cursor.execute("PRAGMA synchronous = OFF;")
    cursor.execute("PRAGMA journal_mode = MEMORY;")

    print("Creating tables...")

    # 1. Summary
    cursor.execute("""
    CREATE TABLE summary (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        total_works INTEGER NOT NULL,
        total_staff INTEGER NOT NULL,
        total_cv INTEGER NOT NULL,
        year_min INTEGER NOT NULL,
        year_max INTEGER NOT NULL,
        updated_at TEXT NOT NULL,
        version TEXT NOT NULL,
        global_mean REAL NOT NULL
    );
    """)

    # 2. Works (CreditDB Pro Compare & Works)
    cursor.execute("""
    CREATE TABLE works (
        work_id TEXT PRIMARY KEY,
        title TEXT NOT NULL,
        title_en TEXT,
        title_norm TEXT NOT NULL,
        year INTEGER NOT NULL,
        anilist_raw_score REAL NOT NULL,
        raw_score_rank INTEGER NOT NULL,
        debiased_b_i REAL NOT NULL,
        true_z_score REAL NOT NULL,
        deviation_score REAL NOT NULL,
        z_score_rank INTEGER NOT NULL,
        predicted_z_score REAL NOT NULL,
        predicted_score REAL NOT NULL,
        pred_score_rank INTEGER NOT NULL,
        residual REAL NOT NULL,
        performance_verdict TEXT NOT NULL,
        tier TEXT NOT NULL,
        percentile REAL NOT NULL,
        staff_json TEXT NOT NULL,
        characters_json TEXT NOT NULL,
        search_text_norm TEXT NOT NULL
    );
    """)
    cursor.execute("CREATE INDEX idx_works_dev_desc ON works(deviation_score DESC);")
    cursor.execute("CREATE INDEX idx_works_dev_asc ON works(deviation_score ASC);")
    cursor.execute("CREATE INDEX idx_works_res_desc ON works(residual DESC);")
    cursor.execute("CREATE INDEX idx_works_res_asc ON works(residual ASC);")
    cursor.execute("CREATE INDEX idx_works_pred_desc ON works(predicted_score DESC);")
    cursor.execute("CREATE INDEX idx_works_raw_desc ON works(anilist_raw_score DESC);")
    cursor.execute("CREATE INDEX idx_works_year_desc ON works(year DESC);")
    cursor.execute("CREATE INDEX idx_works_verdict ON works(performance_verdict);")
    cursor.execute("CREATE INDEX idx_works_tier ON works(tier);")
    cursor.execute("CREATE INDEX idx_works_title ON works(title);")

    # 3. Leaderboards (10 roles including CV)
    cursor.execute("""
    CREATE TABLE leaderboards (
        role TEXT NOT NULL,
        name TEXT NOT NULL,
        name_norm TEXT NOT NULL,
        works_count INTEGER NOT NULL,
        bayesian_rating REAL NOT NULL,
        career_cumulative_z REAL NOT NULL,
        rating_rank INTEGER NOT NULL,
        cumulative_rank INTEGER NOT NULL,
        rating_tier TEXT NOT NULL,
        cumulative_tier TEXT NOT NULL,
        best_work_title TEXT,
        best_work_year INTEGER,
        best_work_z REAL,
        top_character TEXT,
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
        career_trajectory_json TEXT NOT NULL
    );
    """)

    # 5. Era Window Stats (for offline prediction)
    cursor.execute("""
    CREATE TABLE era_stats (
        year INTEGER PRIMARY KEY,
        mean REAL NOT NULL,
        std REAL NOT NULL
    );
    """)

    # 6. Staff Role Features (for rapid custom prediction)
    cursor.execute("""
    CREATE TABLE staff_role_features (
        name TEXT NOT NULL,
        role TEXT NOT NULL,
        works_count INTEGER NOT NULL,
        sum_z REAL NOT NULL,
        mean_z REAL NOT NULL,
        max_z REAL NOT NULL,
        bayesian_s REAL NOT NULL,
        PRIMARY KEY (name, role)
    );
    """)
    cursor.execute("CREATE INDEX idx_srf_name ON staff_role_features(name);")
    cursor.execute("CREATE INDEX idx_srf_role ON staff_role_features(role);")

    print("Populating Summary...")
    cv_count = len(p.staff_evaluator.get_leaderboard(role='cv', limit=0))
    cursor.execute("""
    INSERT INTO summary (total_works, total_staff, total_cv, year_min, year_max, updated_at, version, global_mean)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """, (
        len(p.works_metadata),
        p.staff_evaluator.total_staff_count,
        cv_count,
        1950,
        2026,
        "2026-09-05 22:50:00",
        "1.3.0",
        float(p.bias_model.global_mean)
    ))

    print("Populating Era Stats...")
    for yr, stats in p.local_z_model.year_stats_cache.items():
        m, s = stats[0], stats[1]
        cursor.execute("INSERT INTO era_stats (year, mean, std) VALUES (?, ?, ?)", (int(yr), float(m), float(s)))

    print("Populating Works & Comparison Table...")
    comp_map = {item["work_id"]: item for item in p.cached_comparison_table}
    works_rows = []

    for work_id, meta in p.works_metadata.items():
        comp = comp_map.get(work_id, {})
        title = meta.get("title", work_id)
        title_en = meta.get("title_en", "")
        year = int(meta.get("year", 2020))

        raw_score = float(comp.get("anilist_raw_score", meta.get("anilist_mean_score", 0.0)))
        raw_rank = int(comp.get("raw_score_rank", p.work_raw_ranks.get(work_id, 0)))
        b_val = float(comp.get("debiased_b_i", p.item_biases.get(work_id, 0.0)))
        true_z = float(comp.get("true_z_score", p.z_scores.get(work_id, 0.0)))
        dev_score = float(comp.get("deviation_score", round(50.0 + 10.0 * true_z, 1)))
        z_rank = int(comp.get("z_score_rank", p.work_z_ranks.get(work_id, 0)))
        pred_z = float(comp.get("predicted_z_score", 0.0))
        pred_score = float(comp.get("predicted_score", raw_score))
        pred_rank = int(comp.get("pred_score_rank", p.work_pred_ranks.get(work_id, 0)))
        residual = float(comp.get("residual", round(true_z - pred_z, 3)))
        verdict = str(comp.get("performance_verdict", "概ねスタッフ前評判通り"))

        total_works = len(p.works_metadata)
        pct = (z_rank / total_works * 100.0) if total_works > 0 else 50.0
        tier = calculate_tier(z_rank, total_works)

        # Build enriched staff
        raw_staff = meta.get("staff", {})
        enriched_staff = {}
        search_terms = [title, title_en]

        for rk, members in raw_staff.items():
            if not isinstance(members, list):
                continue
            enriched_members = []
            for m in members:
                name = m.get("name") if isinstance(m, dict) else str(m)
                name = name.strip()
                if not name:
                    continue
                rt, ct = p.staff_evaluator.get_staff_role_tier(name, rk)
                enriched_members.append({"name": name, "rt": rt, "ct": ct})
                search_terms.append(name)
            enriched_staff[rk] = enriched_members

        # Build enriched characters / voice actors
        raw_chars = meta.get("characters", [])
        enriched_chars = []
        for c in raw_chars:
            c_name = c.get("character_name", "")
            rel = c.get("relation", "配角")
            actor = c.get("actor_name", "")
            rt, ct = p.staff_evaluator.get_staff_role_tier(actor, "cv")
            enriched_chars.append({
                "character_name": c_name,
                "relation": rel,
                "actor_name": actor,
                "rt": rt,
                "ct": ct
            })
            if c_name: search_terms.append(c_name)
            if actor: search_terms.append(actor)

        search_text_norm = normalize_text(" ".join(search_terms))

        works_rows.append((
            work_id,
            title,
            title_en,
            normalize_text(title + " " + title_en),
            year,
            raw_score,
            raw_rank,
            b_val,
            true_z,
            dev_score,
            z_rank,
            pred_z,
            pred_score,
            pred_rank,
            residual,
            verdict,
            tier,
            round(pct, 2),
            json.dumps(enriched_staff, ensure_ascii=False),
            json.dumps(enriched_chars, ensure_ascii=False),
            search_text_norm
        ))

    cursor.executemany("""
    INSERT INTO works (
        work_id, title, title_en, title_norm, year, anilist_raw_score, raw_score_rank,
        debiased_b_i, true_z_score, deviation_score, z_score_rank, predicted_z_score,
        predicted_score, pred_score_rank, residual, performance_verdict, tier,
        percentile, staff_json, characters_json, search_text_norm
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, works_rows)
    print(f"Inserted {len(works_rows)} works.")

    print("Populating Leaderboards (10 roles)...")
    roles = ["all", "director", "series_comp", "char_design", "sakkan", "genga", "unit_director", "music", "art_dir", "cv"]
    lb_rows = []

    for role_key in roles:
        role_param = None if role_key == "all" else role_key
        items_rating = p.get_staff_leaderboard(role=role_param, sort_by="rating", limit=0)
        items_cum = p.get_staff_leaderboard(role=role_param, sort_by="cumulative", limit=0)

        cum_rank_map = {it["name"]: idx + 1 for idx, it in enumerate(items_cum)}

        for it in items_rating:
            name = it["name"]
            lb_rows.append((
                role_key,
                name,
                normalize_text(name),
                it.get("works_count", 0),
                round(float(it.get("bayesian_rating", 0.0)), 3),
                round(float(it.get("career_cumulative_z", 0.0)), 2),
                int(it.get("rank", 99999)),
                cum_rank_map.get(name, 99999),
                it.get("rating_tier", "B"),
                it.get("cumulative_tier", "B"),
                it.get("best_work_title"),
                it.get("best_work_year"),
                it.get("best_work_z"),
                it.get("top_character")
            ))

    cursor.executemany("""
    INSERT INTO leaderboards (
        role, name, name_norm, works_count, bayesian_rating, career_cumulative_z,
        rating_rank, cumulative_rank, rating_tier, cumulative_tier,
        best_work_title, best_work_year, best_work_z, top_character
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, lb_rows)
    print(f"Inserted {len(lb_rows)} leaderboard entries across 10 roles.")

    print("Populating Profiles...")
    top_staff_names = set()
    for role_key in roles:
        role_param = None if role_key == "all" else role_key
        top_items = p.get_staff_leaderboard(role=role_param, sort_by="rating", limit=300)
        for it in top_items:
            top_staff_names.add(it["name"])

    prof_rows = []
    print(f"Generating detailed profiles for {len(top_staff_names)} prominent staff/CVs...")
    for name in top_staff_names:
        prof = p.get_staff_profile(name)
        if not prof:
            continue
        prof_rows.append((
            name,
            normalize_text(name),
            prof.get("primary_role", "cv"),
            prof.get("total_works", 0),
            round(float(prof.get("bayesian_rating", 0.0)), 3),
            prof.get("overall_rating_tier", "B"),
            prof.get("overall_rank", 99999),
            round(float(prof.get("career_cumulative_z", 0.0)), 2),
            prof.get("overall_cum_tier", "B"),
            prof.get("cumulative_rank", 99999),
            json.dumps(prof.get("all_role_stats", []), ensure_ascii=False),
            json.dumps(prof.get("career_trajectory", []), ensure_ascii=False)
        ))

    cursor.executemany("""
    INSERT INTO profiles (
        name, name_norm, primary_role, total_works, bayesian_rating,
        overall_rating_tier, overall_rank, career_cumulative_z, overall_cum_tier,
        cumulative_rank, all_role_stats_json, career_trajectory_json
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, prof_rows)
    print(f"Inserted {len(prof_rows)} profiles.")

    print("Populating Staff Role Features for Instant Offline Prediction...")
    srf_rows = []
    for name, recs in p.staff_evaluator.staff_records.items():
        # Group by role
        role_map = {}
        for r in recs:
            rk = r["role"]
            if rk not in role_map:
                role_map[rk] = []
            role_map[rk].append(r)

        for rk, r_list in role_map.items():
            z_vals = [r["z_score"] for r in r_list]
            w_vals = [float(r.get("ep_ratio", 1.0)) for r in r_list]
            count = len(z_vals)
            sum_z = sum(z * w for z, w in zip(z_vals, w_vals))
            mean_z = sum_z / max(1, sum(w_vals))
            max_z = max(z_vals)
            m = DEFAULT_ROLE_M.get(rk, 4.0)
            bayesian_s = sum_z / (sum(w_vals) + m)
            srf_rows.append((
                name,
                rk,
                count,
                round(float(sum_z), 3),
                round(float(mean_z), 3),
                round(float(max_z), 3),
                round(float(bayesian_s), 3)
            ))

    cursor.executemany("""
    INSERT INTO staff_role_features (
        name, role, works_count, sum_z, mean_z, max_z, bayesian_s
    ) VALUES (?, ?, ?, ?, ?, ?, ?)
    """, srf_rows)
    print(f"Inserted {len(srf_rows)} staff role features.")

    conn.commit()
    conn.close()

    db_size = os.path.getsize(dest_db) / (1024 * 1024)
    print(f"=== Successfully created CreditDB Pro SQLite DB: {dest_db} ({db_size:.2f} MB) ===")

if __name__ == "__main__":
    main()
