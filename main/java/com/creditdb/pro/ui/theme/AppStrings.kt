package com.creditdb.pro.ui.theme

/**
 * 日英バイリンガル対応のタイプセーフな文字列リソース
 */
object AppStrings {

    // --- ナビゲーション ---
    val navWorks get() = "Works"
    val navPredict get() = "Predict"
    val navStaff get() = "Staff & CV"
    val navGuide get() = "Guide"

    fun navWorks(isEn: Boolean) = if (isEn) "Works" else "作品DB"
    fun navPredict(isEn: Boolean) = if (isEn) "Predict" else "予測編成"
    fun navStaff(isEn: Boolean) = if (isEn) "Staff & CV" else "制作・声優"
    fun navGuide(isEn: Boolean) = if (isEn) "Guide" else "解説"

    // --- 検索バー・プレースホルダー ---
    const val searchPlaceholder = "作品名・スタッフ名・声優名で検索..."
    const val searchPlaceholderEn = "Search works, staff, voice actors..."

    fun searchPlaceholderWorks(isEn: Boolean) =
        if (isEn) searchPlaceholderEn else searchPlaceholder
    fun searchPlaceholderStaff(isEn: Boolean) =
        if (isEn) "Search creators & voice actors..." else "クリエイター名・声優名・代表作で検索..."
    fun searchPlaceholderPredict(isEn: Boolean) =
        if (isEn) "Search candidate by name..." else "候補者を名前で検索..."

    // --- キャラクター配役関係 (MAIN / SUPPORTING 等) ---
    fun relationCompact(relation: String, isEn: Boolean): String {
        return if (isEn) {
            when (relation.uppercase()) {
                "MAIN" -> "Main"
                "SUPPORTING" -> "Sub"
                "BACKGROUND" -> "Minor"
                "主役", "メイン" -> "Main"
                "助演", "サブ" -> "Sub"
                else -> relation
            }
        } else {
            when (relation.uppercase()) {
                "MAIN" -> "メイン"
                "SUPPORTING" -> "サブ"
                "BACKGROUND" -> "その他"
                else -> relation
            }
        }
    }

    // --- ステータスバー ---
    fun worksCountFormat(count: Int, isEn: Boolean) =
        if (isEn) "$count works" else "$count 件の作品"
    fun staffCountFormat(count: Int, isEn: Boolean) =
        if (isEn) "$count creators / CV" else "$count 名のクリエイター・声優"

    // --- 役職名 (3階層: Full / Compact / Micro) ---
    fun roleFull(roleKey: String, isEn: Boolean): String {
        return if (isEn) {
            when (roleKey) {
                "director" -> "Director"
                "series_comp" -> "Series Composition"
                "char_design" -> "Character Design"
                "sakkan" -> "Animation Director"
                "genga" -> "Key Animation"
                "unit_director" -> "Unit Director"
                "music" -> "Music"
                "art_dir" -> "Art Director"
                "cv" -> "Voice Actor (CV)"
                "studio" -> "Animation Studio"
                "all" -> "All Roles"
                else -> roleKey.replace("_", " ").capitalizeWords()
            }
        } else {
            when (roleKey) {
                "director" -> "監督"
                "series_comp" -> "シリーズ構成・脚本"
                "char_design" -> "キャラクターデザイン"
                "sakkan" -> "作画監督"
                "genga" -> "原画"
                "unit_director" -> "演出・絵コンテ"
                "music" -> "音楽"
                "art_dir" -> "美術監督"
                "cv" -> "声優"
                "studio" -> "スタジオ"
                "all" -> "全役職"
                else -> roleKey
            }
        }
    }

    fun roleCompact(roleKey: String, isEn: Boolean): String {
        return if (isEn) {
            when (roleKey) {
                "director" -> "Director"
                "series_comp" -> "Series Comp"
                "char_design" -> "Char Des"
                "sakkan" -> "Anim Dir"
                "genga" -> "Key Anim"
                "unit_director" -> "Unit Dir"
                "music" -> "Music"
                "art_dir" -> "Art Dir"
                "cv" -> "Voice Actor"
                "studio" -> "Studio"
                "all" -> "All"
                else -> roleKey
            }
        } else {
            when (roleKey) {
                "director" -> "監督"
                "series_comp" -> "構成/脚本"
                "char_design" -> "キャラデザ"
                "sakkan" -> "作監"
                "genga" -> "原画"
                "unit_director" -> "演出/絵コンテ"
                "music" -> "音楽"
                "art_dir" -> "美術監督"
                "cv" -> "声優"
                "studio" -> "スタジオ"
                "all" -> "全役職"
                else -> roleKey
            }
        }
    }

    fun roleMicro(roleKey: String): String {
        return when (roleKey) {
            "director" -> "DIR"
            "series_comp" -> "COMP"
            "char_design" -> "DES"
            "sakkan" -> "AD"
            "genga" -> "KA"
            "unit_director" -> "UNIT"
            "music" -> "MUS"
            "art_dir" -> "ART"
            "cv" -> "CV"
            "studio" -> "STU"
            else -> roleKey.take(3).uppercase()
        }
    }

    // --- 配役関係 (Lead / Supporting / Guest) ---
    fun relationFull(rel: String?, isEn: Boolean): String {
        if (rel == null) return ""
        return if (isEn) {
            when (rel) {
                "主役", "主角" -> "Lead"
                "脇役", "配角" -> "Supporting"
                "端役", "客串" -> "Guest"
                else -> rel
            }
        } else {
            when (rel) {
                "主角" -> "主役"
                "配角" -> "脇役"
                "客串" -> "端役"
                else -> rel
            }
        }
    }

    // --- 評価判定 (Full & Compact) ---
    fun verdictFull(verdict: String, isEn: Boolean): String {
        return if (isEn) {
            when {
                verdict.contains("サプライズ") -> "★ Exceeded Expectations (Surprise)"
                verdict.contains("期待外れ") -> "▲ Underperformed"
                else -> "● As Expected"
            }
        } else {
            verdict
        }
    }

    fun verdictCompact(verdict: String, isEn: Boolean): String {
        return if (isEn) {
            when {
                verdict.contains("サプライズ") -> "★ Surprise"
                verdict.contains("期待外れ") -> "▲ Below"
                else -> "● Normal"
            }
        } else {
            when {
                verdict.contains("サプライズ") -> "★ サプライズ名作"
                verdict.contains("期待外れ") -> "▲ 期待外れ"
                else -> "● 前評判通り"
            }
        }
    }

    // --- ソート・フィルター ---
    fun sortOptionLabel(sortOptionId: String, isEn: Boolean): String {
        return if (isEn) {
            when (sortOptionId) {
                "DEVIATION_DESC" -> "Sort: Deviation (High to Low)"
                "DEVIATION_ASC" -> "Sort: Deviation (Low to High)"
                "RAW_DESC" -> "Sort: AniList Raw (High to Low)"
                "RAW_ASC" -> "Sort: AniList Raw (Low to High)"
                "YEAR_DESC" -> "Sort: Year (Newest first)"
                "YEAR_ASC" -> "Sort: Year (Oldest first)"
                "RATING" -> "Sort: Bayesian Rating S(a)"
                "CUMULATIVE" -> "Sort: Cumulative ΣZ"
                else -> sortOptionId
            }
        } else {
            when (sortOptionId) {
                "DEVIATION_DESC" -> "並替: 偏差値 (高い順)"
                "DEVIATION_ASC" -> "並替: 偏差値 (低い順)"
                "RAW_DESC" -> "並替: AniList素点 (高い順)"
                "RAW_ASC" -> "並替: AniList素点 (低い順)"
                "YEAR_DESC" -> "並替: 公開年 (新しい順)"
                "YEAR_ASC" -> "並替: 公開年 (古い順)"
                "RATING" -> "ソート: 実力評価 S(a) 順"
                "CUMULATIVE" -> "ソート: 生涯累積実績 ΣZ 順"
                else -> sortOptionId
            }
        }
    }

    fun eraLabel(eraId: String, isEn: Boolean): String {
        return if (isEn) {
            when (eraId) {
                "all" -> "All Eras"
                "2020s" -> "2020s (2020-)"
                "2010s" -> "2010s (2010-2019)"
                "2000s" -> "2000s (2000-2009)"
                "1990s" -> "1990s (1990-1999)"
                "1980s" -> "Pre-1990s (-1989)"
                else -> eraId
            }
        } else {
            when (eraId) {
                "all" -> "全年代"
                "2020s" -> "2020年代 (2020〜)"
                "2010s" -> "2010年代 (2010〜2019)"
                "2000s" -> "2000年代 (2000〜2009)"
                "1990s" -> "1990年代 (1990〜1999)"
                "1980s" -> "1980年代以前 (〜1989)"
                else -> eraId
            }
        }
    }

    // --- 詳細画面メトリクス ---
    fun labelDeviation(isEn: Boolean) = if (isEn) "Era Deviation" else "年代補正偏差値"
    fun labelRawScore(isEn: Boolean) = if (isEn) "AniList Raw" else "AniList素点"
    fun labelPredicted(isEn: Boolean) = if (isEn) "Expected" else "事前期待値"
    fun labelVerdict(isEn: Boolean) = if (isEn) "Verdict" else "評価判定"
    fun labelStudio(isEn: Boolean) = if (isEn) "Animation Studio" else "アニメーション制作"
    fun labelCast(isEn: Boolean) = if (isEn) "Cast & Voice Actors" else "キャラクター & 出演声優"
    fun labelStaff(isEn: Boolean) = if (isEn) "Production Staff" else "制作陣クレジット"
    fun labelDualTierHint(isEn: Boolean) = if (isEn) "[Rating / Cumulative]" else "[実力Tier / 累積Tier]"
    fun labelBestWorks(isEn: Boolean) = if (isEn) "Notable Works" else "代表作 (Top Works)"
    fun labelCareerTrajectory(isEn: Boolean) = if (isEn) "Career Trajectory Timeline" else "キャリア参加作品 タイムライン"
    fun labelDepartmentStats(isEn: Boolean) = if (isEn) "Department Stats & Tiers" else "部門別実績 & Tier"
    fun labelTotalWorks(isEn: Boolean) = if (isEn) "Total Works" else "参加作品数"
    fun labelYear(isEn: Boolean) = if (isEn) "Year" else "年"
    fun labelTitle(isEn: Boolean) = if (isEn) "Title" else "作品名"
    fun labelRoleOrChar(isEn: Boolean) = if (isEn) "Role / Character" else "担当 / キャラ"
    fun labelWorkZ(isEn: Boolean) = if (isEn) "Work Z" else "作品Z値"

    // --- ボタン & ダイアログ ---
    fun btnReset(isEn: Boolean) = if (isEn) "Reset" else "リセット"
    fun btnApply(isEn: Boolean) = if (isEn) "Apply" else "適用する"
    fun btnClose(isEn: Boolean) = if (isEn) "Close" else "閉じる"
    fun btnPredict(isEn: Boolean) = if (isEn) "Simulate Quality Score" else "期待クオリティを予測判定"

    // --- 予測編成画面 ---
    fun predictTitle(isEn: Boolean) = if (isEn) "Staff Composition Predictor" else "仮想スタッフィング 予測編成"
    fun predictSubtitle(isEn: Boolean) =
        if (isEn) "Simulate quality deviation and Bayesian team rating from anime creators."
        else "監督・脚本・作画・声優陣の組み合わせから、期待クオリティ偏差値を事前予測。"
    fun predictSlotsTitle(isEn: Boolean) = if (isEn) "Staffing Roster (10 Slots)" else "制作スタッフ編成枠 (全10枠)"
    fun predictEmptySlot(isEn: Boolean) = if (isEn) "Tap to assign creator" else "タップしてクリエイターを指名"

    // --- 比較画面 ---
    fun compareTitle(isEn: Boolean) = if (isEn) "Side-by-Side Comparison" else "作品並列比較"
    fun compareSelectHint(isEn: Boolean) = if (isEn) "Select 2 works to compare" else "比較する2作品を選択してください"

    // --- 設定 & テーマ ---
    fun settingsThemeTitle(isEn: Boolean) = if (isEn) "Color Theme Presets" else "カラーテーマ・プリセット"
    fun settingsThemeDesc(isEn: Boolean) =
        if (isEn) "Material 3 color presets. Tier accent badges remain strictly consistent."
        else "Material 3のカラー原則に基づいた上質なプリセットを選択できます。Tier格付けの色分けは維持されます。"
    fun settingsLanguageTitle(isEn: Boolean) = if (isEn) "Display Language" else "表示言語 (Language)"

    // --- Tier 説明文 ---
    fun tierDescription(tier: String, isEn: Boolean): String {
        return if (isEn) {
            when (tier.uppercase()) {
                "S+" -> "Top 2.3% historically outstanding masterpiece across its era."
                "S" -> "Top 6.7% era-defining monumental hit with widespread acclaim."
                "A+" -> "Top 15.9% high-caliber quality title clearly exceeding average."
                "A" -> "Top 30.9% solid, well-received anime with dedicated fanbase."
                "B+" -> "Upper 50% consistently maintaining above-average standard."
                "B" -> "Median volume zone representing standard era performance."
                "C" -> "Works falling below the era-relative median baseline."
                "D" -> "Works significantly underperforming the era average."
                else -> "Standard quality level."
            }
        } else {
            when (tier.uppercase()) {
                "S+" -> "同年代の中で極めて突出した歴史的メガヒット・超名作水準（上位約2.3%以内）"
                "S" -> "その時代を代表する大傑作。高いクオリティと広範な支持を獲得した作品（上位約6.7%以内）"
                "A+" -> "同年代の上位15%に位置する確かな完成度と魅力を誇る秀作（上位約15.9%以内）"
                "A" -> "平均を明確に上回り、ファン層から根強く支持される良作水準（上位約30.9%以内）"
                "B+" -> "年代の平均水準以上を堅実に維持している安定作"
                "B" -> "年代の平均的ボリュームゾーンに位置する標準的な作品"
                "C" -> "同年代の平均的な評価を下回った作品群"
                "D" -> "同年代の平均的な評価を大きく下回った作品群"
                else -> "標準的な水準"
            }
        }
    }

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}
