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

KANJI_VARIANTS = {
    '惡': '悪', '櫻': '桜', '鐵': '鉄', '國': '国', '龍': '竜',
    '廣': '広', '髙': '高', '﨑': '崎', '壽': '寿', '體': '体',
    '戰': '戦', '畫': '画', '號': '号', '變': '変', '戀': '恋',
    '黑': '黒', '蟲': '虫', '擊': '撃', '寫': '写', '眞': '真',
    '遙': '遥', '條': '条', '齊': '斉', '齋': '斉', '斎': '斉',
    '藪': '薮', '峰': '峯', '嶋': '島', '濱': '浜', '濵': '浜'
}

def normalize_text(text: str) -> str:
    if not text:
        return ""
    s = str(text).lower()
    s = unicodedata.normalize('NFKC', s)
    res = []
    for ch in s:
        if ch in KANJI_VARIANTS:
            res.append(KANJI_VARIANTS[ch])
        elif ch in ('ゐ', 'ヰ'):
            res.append('い')
        elif ch in ('ゑ', 'ヱ'):
            res.append('え')
        elif ch in ('ゔ', 'ヴ'):
            res.append('ぶ')
        else:
            code = ord(ch)
            if 0x30A1 <= code <= 0x30F6:
                res.append(chr(code - 0x60))
            else:
                res.append(ch)
    s = "".join(res)
    s = re.sub(r'[\s\-_・:：,，.．!！?？/／★☆♪〜~・\(\)（）「」『』\[\]【】]', '', s)
    return s

def clean_studio(names):
    if not names:
        return None
    joined_all = " ".join(names)
    joined_upper = joined_all.upper()

    if (any(n.upper() == "MAD" for n in names) and any(n.upper() == "HOUSE" for n in names)) or "MADHOUSE" in joined_upper or "マッドハウス" in joined_all:
        return "MADHOUSE"
    if (any(n.upper() == "WIT" for n in names) and any(n.upper() == "STUDIO" for n in names)) or joined_upper.startswith("WIT"):
        return "WIT STUDIO"
    if (any(n.upper() == "WHITE" for n in names) and any(n.upper() == "FOX" for n in names)) or "WHITE FOX" in joined_upper:
        return "WHITE FOX"
    if (any(n.upper() == "LIDEN" for n in names) and any(n.upper() == "FILMS" for n in names)) or "ライデンフィルム" in joined_all or "LIDENFILMS" in joined_upper:
        return "ライデンフィルム"
    if any(k in joined_upper for k in ["GHIBLI", "ジブリ", "吉卜力"]):
        return "スタジオジブリ"
    if "MAPPA" in joined_upper:
        return "MAPPA"
    if "UFOTABLE" in joined_upper or "ユーフォーテーブル" in joined_all:
        return "ufotable"
    if "BONES" in joined_upper or "ボンズ" in joined_all:
        return "ボンズ"
    if "CLOVERWORKS" in joined_upper or "クローバーワークス" in joined_all:
        return "CloverWorks"
    if "KYOTO" in joined_upper or "京都" in joined_all or "京アニ" in joined_all:
        return "京都アニメーション"
    if "SHAFT" in joined_upper or "シャフト" in joined_all:
        return "シャフト"
    if "SUNRISE" in joined_upper or "サンライズ" in joined_all:
        return "サンライズ"
    if "TRIGGER" in joined_upper or "トリガー" in joined_all:
        return "TRIGGER"
    if "A-1" in joined_upper or "A1" in joined_upper:
        return "A-1 Pictures"
    if "J.C.STAFF" in joined_upper or "JCSTAFF" in joined_upper or "J.C." in joined_upper:
        return "J.C.STAFF"
    if "P.A.WORKS" in joined_upper or "PAWORKS" in joined_upper or "P.A." in joined_upper:
        return "P.A.WORKS"
    if "TOEI" in joined_upper or "東映" in joined_all:
        return "東映アニメーション"
    if "PIERROT" in joined_upper or "ぴえろ" in joined_all:
        return "スタジオぴえろ"
    if "TMS" in joined_upper or "トムス" in joined_all:
        return "トムス・エンタテインメント"
    if "DOGA KOBO" in joined_upper or "動画工房" in joined_all:
        return "動画工房"
    if "SILVER LINK" in joined_upper or "シルバーリンク" in joined_all:
        return "SILVER LINK."
    if "KINEMA CITRUS" in joined_upper or "キネマシトラス" in joined_all:
        return "キネマシトラス"
    if "PRODUCTION I.G" in joined_upper or "PRODUCTION IG" in joined_upper or "プロダクションI.G" in joined_all or "プロダクション・アイジー" in joined_all:
        return "Production I.G"
    if "SCIENCE SARU" in joined_upper or "サイエンスSARU" in joined_all:
        return "サイエンスSARU"
    if "STUDIO DEEN" in joined_upper or "スタジオディーン" in joined_all or "DEEN" in joined_upper:
        return "スタジオディーン"
    if "OLM" in joined_upper:
        return "OLM"
    if "AIC" in joined_upper:
        return "AIC"
    if "GONZO" in joined_upper:
        return "GONZO"
    if "XEBEC" in joined_upper or "ジーベック" in joined_all:
        return "XEBEC"
    if "TROYCA" in joined_upper or "トロイカ" in joined_all:
        return "TROYCA"
    if "LERCHE" in joined_upper or "ラルケ" in joined_all:
        return "Lerche"
    if "COMIX WAVE" in joined_upper or "コミックス・ウェーブ" in joined_all:
        return "コミックス・ウェーブ・フィルム"
    if "FEEL" in joined_upper or "feel." in joined_all:
        return "feel."
    if "TATSUNOKO" in joined_upper or "タツノコ" in joined_all:
        return "タツノコプロ"
    if "GAINAX" in joined_upper or "ガイナックス" in joined_all:
        return "GAINAX"
    if "DAVID" in joined_upper or "デイヴィッドプロダクション" in joined_all:
        return "david production"
    if "スタジオバインド" in joined_all or "STUDIO BIND" in joined_upper:
        return "スタジオバインド"
    if "スタジオヴォルン" in joined_all or "VOLN" in joined_upper:
        return "スタジオヴォルン"
    if "Nexus" in joined_all or "NEXUS" in joined_upper:
        return "Nexus"
    if "C-Station" in joined_all or "C STATION" in joined_upper:
        return "C-Station"
    if "テレコム" in joined_all:
        return "テレコム・アニメーションフィルム"
    if "シンエイ動画" in joined_all:
        return "シンエイ動画"
    if "日本アニメーション" in joined_all:
        return "日本アニメーション"

    noise = {
        "振付", "人名", "配角", "Triple", "ON", "PRODUCTION", "Production", "Kim", "Pictures",
        "フジテレビ", "テレビ朝日", "TBS", "日本テレビ", "テレビ東京", "NHK", "TOKYO MX", "MBS", "BS11", "AT-X",
        "松倉友二", "大月俊倫", "丸山正雄", "植田益朗", "川村元気", "読売広告社", "電通", "博報堂", "アニプレックス"
    }
    filtered = [n for n in names if n not in noise and len(n) > 1 and not n.startswith("第")]
    if not filtered:
        return None
    candidate = filtered[0]
    bad = ["振付", "音響", "監督", "原画", "デザイン", "編集", "美術", "制作進行", "テレビ", "放送"]
    if any(b in candidate for b in bad):
        return None
    return candidate


def main():
    print("Initializing AnimePipeline and loading fast cache...")
    p = AnimePipeline()
    p.fast_load()

    assets_dir = os.path.join(str(root_dir), "CreditDB Pro for Android", "app", "src", "main", "assets")
    os.makedirs(assets_dir, exist_ok=True)
    model_json_path = os.path.join(assets_dir, "predictor_model.json")

    # Train predictor if needed to get tree model
    if not os.path.exists(model_json_path):
        if not p.is_predictor_ready and not p.is_trained:
            print("Training quality predictor model...")
            p.train()

        # Export LightGBM tree model to predictor_model.json
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
    else:
        print(f"Predictor model {model_json_path} already exists. Skipping training.")


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

    # 7. Studios (for studio ranking & fast studio search)
    cursor.execute("""
    CREATE TABLE studios (
        name TEXT PRIMARY KEY,
        name_norm TEXT NOT NULL,
        works_count INTEGER NOT NULL,
        best_work_title TEXT,
        best_work_year INTEGER,
        best_work_dev REAL,
        best_work_tier TEXT
    );
    """)
    cursor.execute("CREATE INDEX idx_studios_works_count ON studios(works_count DESC);")
    cursor.execute("CREATE INDEX idx_studios_name_norm ON studios(name_norm);")

    # 8. Studio Works (for studio detail screen)
    cursor.execute("""
    CREATE TABLE studio_works (
        studio_name TEXT NOT NULL,
        work_id TEXT NOT NULL,
        title TEXT NOT NULL,
        year INTEGER NOT NULL,
        deviation_score REAL NOT NULL,
        tier TEXT NOT NULL,
        PRIMARY KEY (studio_name, work_id)
    );
    """)
    cursor.execute("CREATE INDEX idx_sw_studio ON studio_works(studio_name, year DESC);")


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
    studios_aggregation = {}

    # Count evaluated works for accurate percentile/tiering
    evaluated_works_count = sum(1 for wid, m in p.works_metadata.items() if not m.get("is_archive_only") and (wid in comp_map or float(m.get("anilist_raw_score", 0.0)) > 0))
    if evaluated_works_count == 0:
        evaluated_works_count = len(p.works_metadata)

    for work_id, meta in p.works_metadata.items():
        comp = comp_map.get(work_id, {})
        title = meta.get("title", work_id)
        title_en = meta.get("title_en", "")
        year = int(meta.get("year", 2020))

        raw_score = float(comp.get("anilist_raw_score", meta.get("anilist_raw_score", meta.get("anilist_mean_score", 0.0))))
        is_archive = bool(meta.get("is_archive_only")) or (raw_score <= 0.0 and work_id not in comp_map)

        if is_archive:
            raw_score = 0.0
            raw_rank = 0
            b_val = 0.0
            true_z = 0.0
            dev_score = 0.0
            z_rank = 0
            pred_z = 0.0
            pred_score = 0.0
            pred_rank = 0
            residual = 0.0
            verdict = "ARCHIVE"
            tier = "-"
            pct = 0.0
        else:
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
            pct = (z_rank / evaluated_works_count * 100.0) if evaluated_works_count > 0 else 50.0
            tier = calculate_tier(z_rank, evaluated_works_count)

        # Build enriched staff
        raw_staff = meta.get("staff", {})
        enriched_staff = {}
        search_terms = [title, title_en]

        for rk, members in raw_staff.items():
            if not isinstance(members, list):
                continue
            enriched_members = []
            for m in members:
                if isinstance(m, dict):
                    name = m.get("name")
                else:
                    name = str(m)
                if not name:
                    continue
                name = str(name).strip()
                if not name:
                    continue
                rt, ct = p.staff_evaluator.get_staff_role_tier(name, rk)
                enriched_members.append({"name": name, "rt": rt, "ct": ct})
                search_terms.append(name)
            enriched_staff[rk] = enriched_members

        # Collect studio info
        raw_studios = raw_staff.get("studio", [])
        if isinstance(raw_studios, str):
            raw_studios = [raw_studios]
        s_names = [str(x).strip() for x in raw_studios if x]
        clean_s = clean_studio(s_names)
        if clean_s:
            search_terms.append(clean_s)
            studios_aggregation.setdefault(clean_s, []).append({
                "work_id": work_id,
                "title": title,
                "year": year,
                "deviation_score": dev_score,
                "tier": tier
            })

        # Build enriched characters / voice actors
        raw_chars = meta.get("characters", [])
        enriched_chars = []
        for c in raw_chars:
            if not isinstance(c, dict):
                continue
            c_name = str(c.get("character_name") or "").strip()
            rel = str(c.get("relation") or "配角").strip()
            actor = str(c.get("actor_name") or "").strip()
            rt, ct = p.staff_evaluator.get_staff_role_tier(actor, "cv") if actor else ("-", "-")
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

    print("Populating Studios & Studio Works...")
    studio_rows = []
    sw_rows = []
    for sname, w_list in studios_aggregation.items():
        wcount = len(w_list)
        best_w = max(w_list, key=lambda x: (x["deviation_score"], x["year"]))
        studio_rows.append((
            sname,
            normalize_text(sname),
            wcount,
            best_w["title"],
            best_w["year"],
            best_w["deviation_score"],
            best_w["tier"]
        ))
        for w in w_list:
            sw_rows.append((
                sname,
                w["work_id"],
                w["title"],
                w["year"],
                w["deviation_score"],
                w["tier"]
            ))

    cursor.executemany("""
    INSERT INTO studios (
        name, name_norm, works_count, best_work_title, best_work_year, best_work_dev, best_work_tier
    ) VALUES (?, ?, ?, ?, ?, ?, ?)
    """, studio_rows)
    print(f"Inserted {len(studio_rows)} studios.")

    cursor.executemany("""
    INSERT INTO studio_works (
        studio_name, work_id, title, year, deviation_score, tier
    ) VALUES (?, ?, ?, ?, ?, ?)
    """, sw_rows)
    print(f"Inserted {len(sw_rows)} studio works entries.")


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
        top_cum = p.get_staff_leaderboard(role=role_param, sort_by="cumulative", limit=300)
        for it in top_cum:
            top_staff_names.add(it["name"])

    # Also include notable CVs (>= 20 works) and directors (>= 5 works)
    for name, recs in p.staff_evaluator.staff_records.items():
        has_cv = any(r["role"] == "cv" for r in recs)
        has_dir = any(r["role"] == "director" for r in recs)
        if (has_cv and len(recs) >= 20) or (has_dir and len(recs) >= 5):
            top_staff_names.add(name)

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
            prof.get("overall_tier", prof.get("overall_rating_tier", "B")),
            prof.get("overall_rank", 99999),
            round(float(prof.get("career_cumulative_z", 0.0)), 2),
            prof.get("cumulative_tier", prof.get("overall_cum_tier", "B")),
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

    # Auto-compress to creditdb.zip (assets and root)
    import zipfile
    import shutil
    zip_assets = os.path.join(assets_dir, "creditdb.zip")
    zip_root = os.path.join(str(root_dir), "CreditDB Pro for Android", "creditdb.zip")
    print(f"Compressing {dest_db} to {zip_assets} (level 9)...")
    with zipfile.ZipFile(zip_assets, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as zf:
        zf.write(dest_db, arcname="creditdb.db")
    shutil.copy2(zip_assets, zip_root)
    zip_size = os.path.getsize(zip_assets) / (1024 * 1024)
    print(f"=== Successfully created creditdb.zip: {zip_size:.2f} MB ===")

    # Auto-compress to creditdb.db.gz for Web
    import gzip
    web_public_dir = os.path.join(str(root_dir), "CreditDB for Web", "public", "data")
    if os.path.exists(web_public_dir):
        gz_web = os.path.join(web_public_dir, "creditdb.db.gz")
        print(f"Compressing {dest_db} to {gz_web} (level 9)...")
        with open(dest_db, "rb") as f_in, gzip.open(gz_web, "wb", compresslevel=9) as f_out:
            shutil.copyfileobj(f_in, f_out)
        gz_size = os.path.getsize(gz_web) / (1024 * 1024)
        print(f"=== Successfully created creditdb.db.gz for Web: {gz_size:.2f} MB ===")
        web_dist_dir = os.path.join(str(root_dir), "CreditDB for Web", "dist", "data")
        if os.path.exists(web_dist_dir):
            shutil.copy2(gz_web, os.path.join(web_dist_dir, "creditdb.db.gz"))

        # Update version.json with exact sha256
        import hashlib
        with open(gz_web, "rb") as f_gz:
            gz_sha = hashlib.sha256(f_gz.read()).hexdigest()
        v_data = {
            "version": "1.3.1",
            "updatedAt": time.strftime("%Y-%m-%d %H:%M:%S"),
            "totalWorks": len(p.works_metadata),
            "totalStaff": p.staff_evaluator.total_staff_count,
            "totalCv": cv_count,
            "totalStudios": len(studio_rows),
            "yearMin": 1950,
            "yearMax": 2026,
            "globalMean": float(p.bias_model.global_mean),
            "dbFileName": "creditdb.db.gz",
            "dbSizeCompressed": os.path.getsize(gz_web),
            "dbSizeUncompressed": os.path.getsize(dest_db),
            "sha256": gz_sha
        }
        for vpath in [os.path.join(web_public_dir, "version.json"), os.path.join(web_dist_dir, "version.json")]:
            if os.path.exists(os.path.dirname(vpath)):
                with open(vpath, "w", encoding="utf-8") as f_v:
                    json.dump(v_data, f_v, indent=2)
                print(f"Updated {vpath} (sha256={gz_sha[:16]}...).")


    # Remove raw db from assets
    if os.path.exists(dest_db):
        os.remove(dest_db)
        print("Cleaned up uncompressed creditdb.db from assets.")

if __name__ == "__main__":
    main()

