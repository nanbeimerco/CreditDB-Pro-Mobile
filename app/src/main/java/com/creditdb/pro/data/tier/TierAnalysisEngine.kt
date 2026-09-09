package com.creditdb.pro.data.tier

import com.creditdb.pro.data.CharacterNameResolver
import com.creditdb.pro.data.StaffNameResolver
import com.creditdb.pro.ui.theme.AppStrings
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.sqrt

object TierAnalysisEngine {

    val DEPARTMENT_KEYS = listOf(
        "all",
        "director",
        "series_comp",
        "char_design",
        "sakkan",
        "genga",
        "unit_director",
        "music",
        "art_dir",
        "cv",
        "studio"
    )

    /**
     * Tier表の配置情報から、スタッフおよび制作会社の好みを重み付け集計する
     */
    fun calculateStaffAffinity(
        config: TierTableConfig,
        targetRole: String = "all",
        isEn: Boolean = false
    ): List<StaffAffinityScore> {
        val rows = config.rows
        val totalRows = rows.size
        if (totalRows == 0) return emptyList()

        // staffName -> (role -> list of (workTitle, weight))
        class StaffEntry(
            val name: String,
            val role: String,
            var score: Double = 0.0,
            val works: MutableSet<String> = mutableSetOf()
        )

        val map = mutableMapOf<String, StaffEntry>()

        rows.forEachIndexed { rowIndex, row ->
            // 上位Tierほど高い重み (例: 7行の場合 14, 12, 10, 8, 6, 4, 2)
            val weight = maxOf(1.0, (totalRows - rowIndex) * 2.0)

            for (anime in row.items) {
                val workDisplayTitle = anime.getDisplayTitle(isEn).first

                // 1. staffJson 解析
                try {
                    if (anime.staffJson.isNotBlank()) {
                        val root = JSONObject(anime.staffJson)
                        val keys = root.keys()
                        while (keys.hasNext()) {
                            val role = keys.next()
                            if (targetRole != "all" && targetRole != role) {
                                if (!(targetRole == "studio" && role == "studio")) continue
                            }

                            val arr = root.optJSONArray(role) ?: continue
                            for (i in 0 until arr.length()) {
                                val item = arr.optJSONObject(i) ?: continue
                                val rawName = item.optString("name").trim()
                                if (rawName.isBlank()) continue

                                val displayName = if (role == "studio") {
                                    rawName
                                } else {
                                    StaffNameResolver.getStaffName(rawName, isEn)
                                }

                                val entryKey = "$role:$displayName"
                                val entry = map.getOrPut(entryKey) {
                                    StaffEntry(name = displayName, role = role)
                                }
                                entry.score += weight
                                entry.works.add(workDisplayTitle)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // ignore malformed json
                }

                // 2. charactersJson (CV) 解析
                if (targetRole == "all" || targetRole == "cv") {
                    try {
                        if (anime.charactersJson.isNotBlank()) {
                            val arr = JSONArray(anime.charactersJson)
                            for (i in 0 until arr.length()) {
                                val item = arr.optJSONObject(i) ?: continue
                                val actorName = item.optString("actor_name").trim()
                                if (actorName.isBlank()) continue

                                val displayName = StaffNameResolver.getStaffName(actorName, isEn)
                                val entryKey = "cv:$displayName"
                                val entry = map.getOrPut(entryKey) {
                                    StaffEntry(name = displayName, role = "cv")
                                }
                                entry.score += weight
                                entry.works.add(workDisplayTitle)
                            }
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        }

        return map.values
            .map {
                StaffAffinityScore(
                    staffName = it.name,
                    roleKey = it.role,
                    weightedScore = it.score,
                    workCount = it.works.size,
                    works = it.works.toList()
                )
            }
            .sortedWith(compareByDescending<StaffAffinityScore> { it.weightedScore }.thenByDescending { it.workCount })
    }

    /**
     * ユーザーの主観Tier配置とCreditDB客観偏差値のSpearman順位相関および感性診断
     */
    fun calculateTasteCorrelation(config: TierTableConfig): TasteCorrelationResult {
        val rankedItems = mutableListOf<Pair<Double, Double>>() // (userScore, deviationScore)
        val avgDevPerTier = mutableMapOf<String, Double>()
        val countPerTier = mutableMapOf<String, Int>()

        val totalRows = config.rows.size
        config.rows.forEachIndexed { rowIndex, row ->
            val userScore = (totalRows - rowIndex).toDouble()
            val devs = row.items.map { it.deviationScore }
            countPerTier[row.name] = row.items.size
            if (devs.isNotEmpty()) {
                avgDevPerTier[row.name] = devs.average()
                for (d in devs) {
                    rankedItems.add(userScore to d)
                }
            } else {
                avgDevPerTier[row.name] = 0.0
            }
        }

        val sampleSize = rankedItems.size
        if (sampleSize < 3) {
            return TasteCorrelationResult(
                spearmanRho = 0.0,
                sampleSize = sampleSize,
                avgDeviationPerTier = avgDevPerTier,
                countPerTier = countPerTier,
                diagnosisTitleJa = "データ蓄積中（最低3作品必要）",
                diagnosisTitleEn = "Accumulating Data (Min 3 anime needed)",
                diagnosisDescJa = "作品をTier表に配置すると、客観的数理指標との相関診断が自動計算されます。",
                diagnosisDescEn = "Place anime on the Tier list to compute mathematical correlation and taste profile."
            )
        }

        // 順位相関計算 (Pearson on ranks to handle ties perfectly)
        val userRanks = computeRanks(rankedItems.map { it.first })
        val devRanks = computeRanks(rankedItems.map { it.second })

        val rho = computePearson(userRanks, devRanks)

        val (titleJa, descJa, titleEn, descEn) = when {
            rho >= 0.50 -> Quadruple(
                "王道・客観指標一致型（Mainstream Connoisseur）",
                "あなたの主観評価は、時代補正後の客観偏差値や作品完成度指標と極めて高く連動しています。歴史的評価の定まった傑作や、洗練された演出・脚本を持つ本質的な名作を的確に見抜く高い鑑賞眼を持っています。",
                "Mainstream Connoisseur (Objective Alignment)",
                "Your taste strongly aligns with era-adjusted objective quality metrics. You have an exceptional appreciation for universally acclaimed masterpieces, structural narrative excellence, and sophisticated direction."
            )
            rho >= 0.15 -> Quadruple(
                "独自審美眼・ハイブリッド型（Discerning Eclectic）",
                "世間・時代の高評価作を押さえつつも、特定の監督やスタジオ、作家性の強い隠れた名作を自身の感性で見出して上位に据える、バランスの取れた独自の審美眼を持っています。",
                "Discerning Eclectic (Balanced Independent)",
                "While acknowledging widely acclaimed works, you champion auteur-driven titles and personal favorites with a mature, independent aesthetic perspective."
            )
            rho >= -0.15 -> Quadruple(
                "完全個人主義・カルト嗜好型（Autonomous Individualist）",
                "世間の平均的評価やトレンドに左右されず、自身固有の美的感覚、特定ジャンルへの愛着、あるいは作家への共鳴によって純粋に評価を行っています。",
                "Autonomous Individualist (Cult & Niche Devotee)",
                "Your ranking is decoupled from mainstream consensus, governed purely by personal resonance, niche genres, or devotion to specific creative voices."
            )
            else -> Quadruple(
                "孤高・オルタナティブ探求型（Iconoclast Specialist）",
                "一般的に評価の分かれる尖った作品や、実験的な表現を試みた異色作、過小評価された作品に強い魅力を感じる、探求心に満ちた鑑賞スタイルを持っています。",
                "Iconoclast Specialist (Alternative Explorer)",
                "You find profound merit in polarizing, avant-garde, or overlooked productions that challenge conventional formulas, valuing creative audacity over safe consensus."
            )
        }

        return TasteCorrelationResult(
            spearmanRho = rho,
            sampleSize = sampleSize,
            avgDeviationPerTier = avgDevPerTier,
            countPerTier = countPerTier,
            diagnosisTitleJa = titleJa,
            diagnosisTitleEn = titleEn,
            diagnosisDescJa = descJa,
            diagnosisDescEn = descEn
        )
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private fun computeRanks(values: List<Double>): List<Double> {
        val n = values.size
        val indexed = values.mapIndexed { idx, v -> IndexedValue(idx, v) }
            .sortedBy { it.value }

        val ranks = DoubleArray(n)
        var i = 0
        while (i < n) {
            var j = i
            while (j < n - 1 && indexed[j + 1].value == indexed[j].value) {
                j++
            }
            val avgRank = (i + 1 + j + 1) / 2.0
            for (k in i..j) {
                ranks[indexed[k].index] = avgRank
            }
            i = j + 1
        }
        return ranks.toList()
    }

    private fun computePearson(xs: List<Double>, ys: List<Double>): Double {
        val n = xs.size
        if (n == 0) return 0.0
        val meanX = xs.average()
        val meanY = ys.average()

        var num = 0.0
        var denX = 0.0
        var denY = 0.0

        for (i in 0 until n) {
            val dx = xs[i] - meanX
            val dy = ys[i] - meanY
            num += dx * dy
            denX += dx * dx
            denY += dy * dy
        }

        val den = sqrt(denX * denY)
        return if (den > 1e-9) (num / den).coerceIn(-1.0, 1.0) else 0.0
    }

    /**
     * AI（ChatGPT / Claude / Gemini）深層分析用のMarkdownエクスポート生成
     * CreditDB独自指標の数理定義、Tier配置データ、スタッフ好み集計、およびプロンプトを完全収録
     */
    fun exportToMarkdown(config: TierTableConfig, isEn: Boolean = false): String {
        val correlation = calculateTasteCorrelation(config)
        val topDirectors = calculateStaffAffinity(config, "director", isEn).take(5)
        val topComps = calculateStaffAffinity(config, "series_comp", isEn).take(5)
        val topStudios = calculateStaffAffinity(config, "studio", isEn).take(5)
        val topCvs = calculateStaffAffinity(config, "cv", isEn).take(5)

        val d = "$"
        val sb = StringBuilder()

        sb.appendLine("# CreditDB Anime Taste & Tier Evaluation Profile")
        sb.appendLine()
        sb.appendLine("## 1. CreditDB 数理指標モデルの定義 (Mathematical Metric Specification)")
        sb.appendLine("本プロファイルに含まれるCreditDB独自の客観評価指標の定義と算出方法は以下の通りです：")
        sb.appendLine()
        sb.appendLine("- **AniList Raw Score (生スコア)**: 一般的なレビューサイト（AniList等）におけるユーザー投票の算術平均（0〜100点）。年代ごとの採点基準の変化や投票者数の偏り（インフレ）を含みます。")
        sb.appendLine("- **Debiased Score (${d}b_i${d}, デバイアス得点)**: 投票数規模による過小・過大評価および年代ベースラインのドリフトを、経験的ベイズ縮小推定量（Empirical Bayes Shrinkage）により補正した真の品質推定量。")
        sb.appendLine("- **Local Z-Score (${d}Z_i${d}, 年代相対標準化得点)**: 当該作品が放送された「同一年代の作品群（平均0, 標準偏差1）」の中での相対位置を示す標準化得点。これにより、1980年代の名作と2020年代の現代作を年代のスコアインフレに影響されず公平に横断比較できます。")
        sb.appendLine("- **Deviation Score (偏差値 ${d}T_i = 50 + 10 \\cdot Z_i${d})**: 同年代作品群における客観的クオリティ偏差値（平均50, 標準偏差10）。")
        sb.appendLine("  - ${d}T_i \\ge 70.0${d} (Top 2.3%): S+ Tier（歴史的メガヒット・超名作）")
        sb.appendLine("  - 65.0 ${d}\\le T_i < 70.0${d} (Top 6.7%): S Tier（時代を代表する大傑作）")
        sb.appendLine("  - 60.0 ${d}\\le T_i < 65.0${d} (Top 15.9%): A+ Tier（極めて完成度の高い秀作）")
        sb.appendLine("  - 55.0 ${d}\\le T_i < 60.0${d} (Top 30.9%): A Tier（平均を上回る良作）")
        sb.appendLine("  - 45.0 ${d}\\le T_i < 55.0${d}: B Tier（標準的ボリュームゾーン）")
        sb.appendLine("  - ${d}T_i < 45.0${d}: C / D Tier（平均を下回る作品群）")
        sb.appendLine("- **Predicted Score (${d}\\hat{Z}_i${d}, 期待偏差値)**: 参加スタッフ（監督・シリーズ構成・キャラデザ・作監・原画・演出・音楽・美術・声優・制作会社）の過去全実績から算出されたチーム戦闘力に基づく事前期待値。")
        sb.appendLine("- **Residual (実績乖離度 ${d}Z_i - \\hat{Z}_i${d})**: 事前期待値に対する実績の上振れ・下振れ幅。")
        sb.appendLine("  - 正値（+）: スタッフ陣の前評判や予算規模を劇的に超えて大成功した作品（シナリオの爆発力やケミストリーによる下克上）。")
        sb.appendLine("  - 負値（-）: 豪華スタッフ陣の事前期待値に対して結果が伸び悩んだ作品。")
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()

        sb.appendLine("## 2. ユーザー主観 Tier表 (User Tier Placements)")
        sb.appendLine()
        for (row in config.rows) {
            val avgDev = correlation.avgDeviationPerTier[row.name] ?: 0.0
            sb.appendLine("### Tier [ ${row.name} ] (作品数: ${row.items.size}件, 平均偏差値: ${String.format(Locale.US, "%.1f", avgDev)})")
            if (row.items.isEmpty()) {
                sb.appendLine("*(作品なし)*")
            } else {
                sb.appendLine("| 作品名 | 放送年 | 偏差値 (${d}T_i${d}) | 乖離度 (Residual) | 主な参加スタッフ |")
                sb.appendLine("| :--- | :---: | :---: | :---: | :--- |")
                for (item in row.items) {
                    val title = item.getDisplayTitle(isEn).first
                    val dev = String.format(Locale.US, "%.1f", item.deviationScore)
                    val res = String.format(Locale.US, "%+.2f", item.residual)
                    val staffSummary = buildCompactStaff(item.staffJson)
                    sb.appendLine("| **$title** | ${item.year} | $dev | $res | $staffSummary |")
                }
            }
            sb.appendLine()
        }

        sb.appendLine("---")
        sb.appendLine()

        sb.appendLine("## 3. スタッフ・スタジオ好み集計 (Staff & Studio Affinity)")
        sb.appendLine("ユーザーのTier上位配置に基づき、重み付け集計された好みのクリエイター・制作スタジオです：")
        sb.appendLine()
        sb.appendLine("### 監督 (Directors)")
        if (topDirectors.isEmpty()) sb.appendLine("- なし")
        else topDirectors.forEachIndexed { i, s ->
            sb.appendLine("${i + 1}. **${s.staffName}** (${String.format(Locale.US, "%.1f", s.weightedScore)} pts, ${s.workCount}作: ${s.works.take(3).joinToString(", ")})")
        }
        sb.appendLine()

        sb.appendLine("### シリーズ構成・脚本 (Series Composition & Screenplay)")
        if (topComps.isEmpty()) sb.appendLine("- なし")
        else topComps.forEachIndexed { i, s ->
            sb.appendLine("${i + 1}. **${s.staffName}** (${String.format(Locale.US, "%.1f", s.weightedScore)} pts, ${s.workCount}作: ${s.works.take(3).joinToString(", ")})")
        }
        sb.appendLine()

        sb.appendLine("### 制作スタジオ (Animation Studios)")
        if (topStudios.isEmpty()) sb.appendLine("- なし")
        else topStudios.forEachIndexed { i, s ->
            sb.appendLine("${i + 1}. **${s.staffName}** (${String.format(Locale.US, "%.1f", s.weightedScore)} pts, ${s.workCount}作: ${s.works.take(3).joinToString(", ")})")
        }
        sb.appendLine()

        sb.appendLine("### 声優 (Voice Actors)")
        if (topCvs.isEmpty()) sb.appendLine("- なし")
        else topCvs.forEachIndexed { i, s ->
            sb.appendLine("${i + 1}. **${s.staffName}** (${String.format(Locale.US, "%.1f", s.weightedScore)} pts, ${s.workCount}作: ${s.works.take(3).joinToString(", ")})")
        }
        sb.appendLine()

        sb.appendLine("---")
        sb.appendLine()

        sb.appendLine("## 4. 数理相関診断 (Taste & Mathematical Correlation)")
        sb.appendLine("- **Spearman順位相関係数 (${d}\\\\rho${d})**: **${String.format(Locale.US, "%.3f", correlation.spearmanRho)}** (標本数: ${correlation.sampleSize}件)")
        sb.appendLine("- **感性診断タイプ**: **${if (isEn) correlation.diagnosisTitleEn else correlation.diagnosisTitleJa}**")
        sb.appendLine("- **診断詳細**: ${if (isEn) correlation.diagnosisDescEn else correlation.diagnosisDescJa}")
        sb.appendLine()

        sb.appendLine("---")
        sb.appendLine()

        sb.appendLine("## 5. AI深層分析プロンプト (Prompt for ChatGPT / Claude / Gemini)")
        sb.appendLine("```markdown")
        sb.appendLine("あなたは世界最高峰のアニメーション批評家およびデータサイエンティストです。")
        sb.appendLine("上記に提示された【CreditDBの数理指標定義】、【ユーザーのTier表】、【スタッフ集計】、【相関係数】を熟読した上で、以下の4項目について精緻かつ洞察に満ちた深層プロファイリングレポートを作成してください。")
        sb.appendLine()
        sb.appendLine("1. 【物語・テーマ・文脈の深層嗜好分析】")
        sb.appendLine("   ユーザーが上位Tier（S+, S, A+）に選定した作品群に共通する物語構造、実存的テーマ、キャラクターの葛藤様式、および世界観の傾向を解き明かしてください。")
        sb.appendLine()
        sb.appendLine("2. 【演出・ビジュアル・作家性の好み】")
        sb.appendLine("   好みの監督、シリーズ構成、スタジオの作風（カッティングテンポ、レイアウト美学、脚本の伏線回収力など）から、ユーザーがどのようなアニメーション表現に最も心を動かされるかを分析してください。")
        sb.appendLine()
        sb.appendLine("3. 【客観指標との関係性と審美眼】")
        sb.appendLine("   Spearman相関係数（${d}\\\\rho = ${String.format(Locale.US, "%.2f", correlation.spearmanRho)}${d}）および偏差値・Residualの分布を踏まえ、ユーザーが「世間的高評価作の本質を見抜くタイプ」か「隠れたカルト作・作家性を愛するタイプ」か、その審美眼の特異性を講評してください。")
        sb.appendLine()
        sb.appendLine("4. 【次に観るべき絶対的おすすめ作品 5選】")
        sb.appendLine("   Tier表にまだ含まれていない作品の中から、ユーザーのスタッフ親和性（監督・脚本・スタジオ）および物語の好みに極限まで合致するアニメを厳選し、推薦理由を各作品200字程度で提示してください。")
        sb.appendLine("```")

        return sb.toString()
    }

    private fun buildCompactStaff(staffJson: String): String {
        return try {
            val root = JSONObject(staffJson)
            val parts = mutableListOf<String>()
            root.optJSONArray("director")?.let { arr ->
                if (arr.length() > 0) {
                    val name = arr.getJSONObject(0).optString("name")
                    if (name.isNotBlank()) parts.add("監督:$name")
                }
            }
            root.optJSONArray("series_comp")?.let { arr ->
                if (arr.length() > 0) {
                    val name = arr.getJSONObject(0).optString("name")
                    if (name.isNotBlank()) parts.add("構成:$name")
                }
            }
            root.optJSONArray("studio")?.let { arr ->
                if (arr.length() > 0) {
                    val name = arr.getJSONObject(0).optString("name")
                    if (name.isNotBlank()) parts.add("制作:$name")
                }
            }
            if (parts.isNotEmpty()) parts.joinToString(" / ") else "クレジット記載あり"
        } catch (e: Exception) {
            "-"
        }
    }
}
