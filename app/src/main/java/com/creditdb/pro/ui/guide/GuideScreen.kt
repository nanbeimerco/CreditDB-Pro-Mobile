package com.creditdb.pro.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditdb.pro.ui.components.DualTierBadge
import com.creditdb.pro.ui.components.TierBadge
import com.creditdb.pro.ui.theme.LanguageManager
import com.creditdb.pro.ui.theme.TierTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isEn = LanguageManager.isEnglish
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (onBackClick != null) {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isEn) "Mathematical Guide" else "数理解説ガイド",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (isEn) "Back" else "戻る"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ==========================================
            // 第1章: アニメ作品の評価指標について
            // ==========================================
            item {
                GuideSectionHeader(
                    number = "1",
                    title = if (isEn) "Anime Evaluation Metrics" else "アニメ作品の評価指標について"
                )
            }

            item {
                Text(
                    text = if (isEn)
                        "CreditDB organizes public audience ratings into three core metrics: Standard Score (Deviation), Raw Score (AniList), and Quality Tier, helping users intuitively grasp each work's relative standing."
                    else
                        "CreditDB では、作品の評価傾向を直感的に把握できるよう、公開指標を「偏差値」「AniList素点」「Tier」の3点に整理して表示しています。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 偏差値 Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isEn) "📊 Standard Score (Era-Relative Benchmark)" else "📊 偏差値（年代相対スコア）",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isEn)
                                "Measures where a title was situated among anime released around the same era, standardized to a mean of 50.0 and standard deviation of 10.0 using statistical Z-scores."
                            else
                                "「その作品が公開された年代のアニメ作品群の中で、どのような相対的評価位置にあったか」を統計的な標準偏差単位で算出し、平均 50.0、標準偏差 10.0 に標準化した指標です。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FormulaBox(formula = if (isEn) "Standard Score = 50.0 + 10.0 × Z_i" else "偏差値 = 50.0 + 10.0 × Z_i")
                        Text(
                            text = if (isEn)
                                "※ By adjusting for era-specific score distributions, works from different decades can be referenced on a consistent relative benchmark."
                            else
                                "※ 年代ごとのスコア分布（インフレ・デフレ傾向）を平準化しているため、公開時期が異なる作品同士でも相対的な評価水準を比較しやすくなっています。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // AniList素点 Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isEn) "🌐 AniList Raw Score (Global User Reviews)" else "🌐 AniList素点（全世界レビュー実績）",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = if (isEn)
                                "The weighted average rating (out of 100) submitted by anime fans across the globe on AniList."
                            else
                                "全世界のアニメデータベース AniList に投稿されたユーザーレビューの加重平均点（100点満点）です。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEn)
                                "※ While raw score directly reflects global popularity and excitement, newer works often experience higher score baselines due to demographic shifts. Refer to 'Standard Score' for a consistent relative comparison adjusted for era-specific distributions."
                            else
                                "※ 素点は世界的な人気や熱量を直接反映するメリットがありますが、年代が新しい作品ほど投票傾向により高得点化（インフレ）しやすい性質があります。時代背景による得点水準の違いを考慮した比較には「偏差値」をご参照ください。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 作品 Tier 判定基準表
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isEn) "🏆 Quality Tier Criteria" else "🏆 作品 Tier（品質階層）の判定基準",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isEn)
                            "Classified by objective percentiles across all 5,452 anime based on Standard Scores:"
                        else
                            "全5,452作品の偏差値に基づき、客観的なパーセンタイル（上位何%か）によって以下の階層に分類しています。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val tiers = listOf(
                        Triple(
                            "S+",
                            if (isEn) "70.0+ (Top ~2.3%)" else "70.0 以上 (上位 約 2.3% 以内)",
                            if (isEn) "Historical mega-hit / monumental masterpiece standing exceptionally high among its peers." else "同年代の全アニメの中で極めて突出した歴史的メガヒット・超名作水準。"
                        ),
                        Triple(
                            "S",
                            if (isEn) "65.0 - 69.9 (Top ~6.7%)" else "65.0 〜 69.9 (上位 約 6.7% 以内)",
                            if (isEn) "Great masterpiece representing its era, achieving top-tier quality and widespread acclaim." else "その時代を代表する大傑作。高いクオリティと広範な支持を獲得した作品。"
                        ),
                        Triple(
                            "A+",
                            if (isEn) "60.0 - 64.9 (Top ~15.9%)" else "60.0 〜 64.9 (上位 約 15.9% 以内)",
                            if (isEn) "Outstanding work ranking in the top 15% with solid craftsmanship and charm." else "同年代の上位15%に位置する確かな完成度と魅力を誇る秀作。"
                        ),
                        Triple(
                            "A",
                            if (isEn) "55.0 - 59.9 (Top ~30.9%)" else "55.0 〜 59.9 (上位 約 30.9% 以内)",
                            if (isEn) "Solid good work well above average with dedicated fan followings." else "平均を明確に上回り、ファン層から根強く支持される良作水準。"
                        ),
                        Triple(
                            "B+",
                            if (isEn) "50.0 - 54.9 (Top 50%)" else "50.0 〜 54.9 (平均〜上位 50%)",
                            if (isEn) "Consistent, reliable production maintaining above-average era standards." else "年代の平均水準以上を堅実に維持している安定作。"
                        ),
                        Triple(
                            "B",
                            if (isEn) "45.0 - 49.9 (Average zone)" else "45.0 〜 49.9 (平均的ボリュームゾーン)",
                            if (isEn) "Standard benchmark anime." else "標準的な評価帯。"
                        ),
                        Triple(
                            "C",
                            if (isEn) "40.0 - 44.9 (Bottom 30%)" else "40.0 〜 44.9 (下位 30% 未満)",
                            if (isEn) "Works falling below the era's average." else "同年代の平均的な評価を下回った作品群。"
                        ),
                        Triple(
                            "D",
                            if (isEn) "Under 40.0 (Bottom 15%)" else "40.0 未満 (下位 15% 未満)",
                            if (isEn) "Works significantly below the era's average." else "同年代の平均的な評価を大きく下回った作品群。"
                        )
                    )

                    tiers.forEach { (tier, standard, desc) ->
                        TierStandardRow(tier = tier, standard = standard, desc = desc)
                    }
                }
            }

            // ==========================================
            // 第2章: 年代補正Z値（Z_i）について
            // ==========================================
            item {
                GuideSectionHeader(
                    number = "2",
                    title = if (isEn) "Two-Stage Normalization (Z_i)" else "年代補正Z値（Z_i）の二段階数理モデル"
                )
            }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isEn) "📐 Two-Stage Mathematical Debiasing" else "📐 二段階の数理的補正プロセス",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isEn)
                                "The Era-Adjusted Z-Score (Local Z-Score Z_i) reflects a work's relative standing by statistically adjusting for both reviewer grading tendencies and era-dependent score distribution shifts."
                            else
                                "年代補正Z値（局所Zスコア Z_i）は、レビュアーごとの採点傾向（甘口・辛口バイアス）や年代ごとのスコア水準の違いを二段階の統計処理で緩和・調整し、時代間での相対的な立ち位置を示す指標です。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = if (isEn) "Stage 1 (Separating User Scoring Bias via ALS):" else "第1段階（ユーザー採点バイアスの分離・推計）:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isEn)
                                "By solving the regularized alternating least squares (ALS) matrix factorization R_ui = μ + b_u + b_i + ε_ui, user-specific bias b_u is separated out to estimate the work-specific rating component b_i."
                            else
                                "レビュー R_ui = μ + b_u + b_i + ε_ui の交互最小二乗法（正則化ALS行列分解）を解き、ユーザー特有の甘口・辛口バイアス b_u を分離して、作品固有の評価成分 b_i を推計します。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = if (isEn) "Stage 2 (Local Standardization via Moving Era Window):" else "第2段階（年代移動窓による局所標準化）:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isEn)
                                "In a 7-year moving window centered around the release year of work i (±3 years), the mean μ_year and standard deviation σ_year are computed to convert into a local Z-score:"
                            else
                                "各公開年の前後を含む移動窓において、作品群の平均 μ_year と標準偏差 σ_year を計算し、局所Zスコア化します：",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FormulaBox(formula = "Z_i = (b_i - μ_year) / σ_year")

                        Text(
                            text = if (isEn)
                                "This normalizes all anime across history into a common scale of mean 0.0 and standard deviation 1.0. Multiplying Z_i by 10 and adding 50 yields the Standard Score (50 + 10 × Z_i)."
                            else
                                "これにより、全年代の作品が「平均 0.0、標準偏差 1.0」の共通尺度に標準化されます。この Z_i を 10倍して 50 を加えたものが「偏差値（50 + 10 × Z_i）」です。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ==========================================
            // 第3章: 制作陣・声優のクレジット分析モデル
            // ==========================================
            item {
                GuideSectionHeader(
                    number = "3",
                    title = if (isEn) "Creator & Cast Statistical Metric Model" else "制作陣・声優のクレジット分析モデル"
                )
            }

            // ベイズ推定レーティング S(a) Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isEn) "🎯 Bayesian Rating S(a) (Empirical Bayesian Shrinkage)" else "🎯 ベイズ推定レーティング S(a)（経験的ベイズ平滑化）",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isEn)
                                "Smoothes the era-adjusted Z-scores of works in a creator's portfolio using a Bayesian statistical model parameterized by the role's median credit count m_role."
                            else
                                "各スタッフがこれまでに手掛けた参加作品の年代補正Zスコアを、部門別の参加作品数中央値 m_role に基づくベイズ統計モデルで平滑化した指標です。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FormulaBox(formula = "S(a) = (Σ Z) / (|P| + m_role)")
                        Text(
                            text = if (isEn)
                                "Applies empirical Bayesian shrinkage to moderate statistical outliers from small sample sizes toward role-specific baselines, providing a stable indicator of average work reception."
                            else
                                "参加作品数が少ない場合の統計的な極端値（少数の作品による過大・過小推計）を抑えるため、担当役職全体の事前分布に向けて平滑化を行い、継続的・安定的な評価水準を客観的に推計する指標です。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 生涯累積実績 ΣZ Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isEn) "🏛️ Career Cumulative Impact ΣZ (Lifetime Credit Volume)" else "🏛️ 生涯累積実績 ΣZ（全作品通算実績量）",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = if (isEn)
                                "Aggregates positive era-adjusted evaluations across a career, reflecting the cumulative volume and reception of credited creative works."
                            else
                                "長年にわたり多数の作品に携わり、アニメーション文化を支えてきた制作陣・声優の活動実績と、関与作品が獲得してきた評価の蓄積を通算値として算出する指標です。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FormulaBox(formula = "Σ Z = Σ_{p ∈ P} Z_p")
                        Text(
                            text = if (isEn)
                                "Reflects the total volume of credited output delivered across a sustained career, highlighting cumulative contribution to the medium."
                            else
                                "長年のキャリアを通じて多数の作品を支え続け、アニメーション界にどれだけの作品実績を積み上げてきたかという通算の足跡を示します。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // クレジット表記の見方 [レーティングTier / 累積実績Tier]
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isEn) "🏷️ Dual Tier Badge [Rating Tier / Cumulative Tier]" else "🏷️ クレジット表記 [レーティングTier / 累積実績Tier] の見方",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEn)
                                "In the staff list on the work detail screen, each creator name is prefixed with [Rating Tier / Cumulative Tier]:"
                            else
                                "作品詳細画面の制作陣一覧では、各スタッフ名の前に [レーティングTier / 生涯累積実績Tier] を並べて表示しています：",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            DualTierBadge(ratingTier = "S+", cumulativeTier = "S")
                            Text(
                                text = if (isEn) "e.g.: [S+ / S] Hideaki Anno" else "例: [S+ / S] 庵野秀明",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (isEn)
                                "• Left (Rating Tier): Average evaluation level of credited works in that role.\n• Right (Cumulative Tier): Total volume of career contributions accumulated over time.\n• [S+ / S+]: Historic Top Creator\n• [S+ / B]: Rising Talent / High-Impact Contributor\n• [A / S+]: Veteran Master sustaining the industry for decades"
                            else
                                "• 左側（レーティングTier）: その役職として関与した作品群における平均的な評価水準を示します。\n• 右側（生涯累積実績Tier）: その役職としてキャリアを通じて積み上げてきた通算実績の総量を示します。\n• [S+ / S+]: 歴史的トップクリエイター\n• [S+ / B]: 新進気鋭の注目クリエイター\n• [A / S+]: 長年現場を支え続ける熟練の制作陣・名匠",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ==========================================
            // 第4章: 部門別平滑化パラメータ m_role 設計表
            // ==========================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isEn) "⚖️ Smoothing Parameter m_role by Role (Median)" else "⚖️ 部門別平滑化パラメータ m_role（中央値）の設計",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isEn)
                            "Workload and annual output vary dramatically across roles. Median credit counts derived from all 5,452 works are applied as smoothing priors m_role:"
                        else
                            "役職ごとに1作品を制作する作業負荷や年間参加可能本数は大きく異なります。全5,452作品の実績データから各部門の参加作品数中央値を求め、平滑化定数 m_role として適用しています：",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val roleParams = listOf(
                        Triple(
                            if (isEn) "Director (director)" else "監督 (director)",
                            "2.0",
                            if (isEn) "High overall impact per title; reflects reception tendencies with high sensitivity from few works." else "作品全体への影響度が極めて大きいため、少本数から評価傾向を高感度に反映。"
                        ),
                        Triple(
                            if (isEn) "Series Comp / Script (series_comp)" else "シリーズ構成 / 脚本 (series_comp)",
                            "2.0",
                            if (isEn) "Reflects overall storytelling impact from few titles." else "物語統括としての影響度を少本数から反映。"
                        ),
                        Triple(
                            if (isEn) "Character Design (char_design)" else "キャラクターデザイン (char_design)",
                            "2.0",
                            if (isEn) "Reflects visual direction impact with high sensitivity." else "ビジュアル総括の実績を高感度に反映。"
                        ),
                        Triple(
                            if (isEn) "Music / Composer (music)" else "音楽 (music)",
                            "2.0",
                            if (isEn) "Reflects musical direction and creative reception with high sensitivity." else "劇伴作曲家としての作風や関与実績を高感度に反映。"
                        ),
                        Triple(
                            if (isEn) "Animation Director (sakkan)" else "作画監督 (sakkan)",
                            "4.0",
                            if (isEn) "Moderately smoothed based on episode & chief supervisor credits." else "各話・総作監の実績に基づき適度に平滑化。"
                        ),
                        Triple(
                            if (isEn) "Unit Director / Storyboard (unit_director)" else "演出 / 絵コンテ (unit_director)",
                            "4.0",
                            if (isEn) "Moderately smoothed based on episode directing credits." else "各話演出としての実績を適度に平滑化。"
                        ),
                        Triple(
                            if (isEn) "Art Director (art_dir)" else "美術監督 (art_dir)",
                            "4.0",
                            if (isEn) "Moderately smoothed based on background/world art credits." else "背景・世界観統括としての実績を適度に平滑化。"
                        ),
                        Triple(
                            if (isEn) "Key Animation (genga)" else "原画 (genga)",
                            "6.0",
                            if (isEn) "Estimates stable rating levels with reduced statistical variance over high credit volume." else "参加作品数が多いため、統計的ばらつきを抑えて安定的に水準を推定。"
                        ),
                        Triple(
                            if (isEn) "All Roles Combined (all)" else "スタッフ全体総合 (all)",
                            "6.0",
                            if (isEn) "Baseline smoothing value for overall Bayesian rating S(a) across all roles." else "全役職通算のベイズ推定レーティング S(a) の平滑化基準値。"
                        )
                    )

                    roleParams.forEach { (role, mVal, note) ->
                        RoleParamRow(role = role, mVal = mVal, note = note)
                    }
                }
            }

            // ==========================================
            // 第5章: 制作スタジオの分析設計（クリエイター重視の思想）
            // ==========================================
            item {
                GuideSectionHeader(
                    number = "5",
                    title = if (isEn) "Studio Analytical Design (Creator-Centric Focus)" else "制作スタジオの分析設計（クリエイター重視の思想）"
                )
            }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isEn) "🎨 Anime is Crafted by Creators, Not Corporate Entities" else "🎨 作品は法人ではなくクリエイターが生み出すもの",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isEn)
                                "CreditDB does not compute statistical ratings (S(a) or ΣZ) for corporate animation studios, focusing analysis directly on individual creative staff."
                            else
                                "CreditDB では、制作スタジオに対して「レーティング S(a)」や「生涯累積実績 ΣZ」といった数理指標を算出・付与していません。また、クレジット分析においてもスタジオ情報は指標算出対象から除外しています。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEn)
                                "The expressive depth and visual artistry of animation are shaped directly by the craftsmanship and dedication of individual creators—directors, scriptwriters, character designers, and animators—rather than corporate entities alone."
                            else
                                "アニメーションの豊かな映像表現や物語は、スタジオという法人組織そのもの以上に、監督、脚本、キャラクターデザイン、作画監督、原画など各現場のクリエイター陣の技術と尽力によって形作られると考えているためです。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isEn)
                                "Studio browsing is provided purely as an objective chronological filmography archive."
                            else
                                "スタジオを検索・閲覧する機能は、あくまで制作年順（公開年降順）の客観的な作品アーカイブ・フィルモグラフィとして提供されています。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ==========================================
            // 第6章: 制作陣・声優テーブルの両指標表示について
            // ==========================================
            item {
                GuideSectionHeader(
                    number = "6",
                    title = if (isEn) "Dual Metrics in Staff / Voice Actor Table" else "制作陣・声優テーブルの両指標表示について"
                )
            }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isEn) "📈 Interpreting 'Rating vs Record'" else "📈 「レーティング / 実績」の解釈と見方",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = if (isEn)
                                "The dual metrics displayed in creator tables provide complementary perspectives on reception levels and cumulative career output:"
                            else
                                "制作陣・声優テーブルの右側に表示される数値は、関与作品の評価傾向と通算の活動量を多角的に捉えるための二連指標です：",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEn)
                                "• Rating (Bayesian Shrinkage S(a)): The estimated average work reception level associated with credits in that role.\n• Record (Career Cumulative ΣZ): The cumulative volume and reception of credited works across their career."
                            else
                                "• レーティング（ベイジアン平滑化 S(a)）: その役職として関与した作品における平均的な評価水準（統計的期待値）を表します。\n• 実績（生涯累積実績 ΣZ）: その役職としてキャリアを通じて積み上げてきた通算の活動量および関与作品の評価蓄積を表します。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isEn)
                                "For example, a rising creator with a highly acclaimed initial release will reflect a high 'Rating', while a seasoned veteran supporting productions over decades will show an extensive 'Record'. Referencing both metrics helps users appreciate the distinct career trajectory of each creator."
                            else
                                "例えば、参加本数が少なくても高評価作品を手掛けた新鋭は「レーティング」が高く、数十年にわたり現場を支え続ける熟練の制作陣は「実績」が圧倒的に高くなります。両指標を併せて参照することで、各クリエイターのキャリアの歩みと特徴を客観的に把握できます。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideSectionHeader(
    number: String,
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FormulaBox(
    formula: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = formula,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun TierStandardRow(
    tier: String,
    standard: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TierBadge(tier = tier)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = standard,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TierTheme.forTier(tier).onContainerColor
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RoleParamRow(
    role: String,
    mVal: String,
    note: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = role,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "m = $mVal",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
