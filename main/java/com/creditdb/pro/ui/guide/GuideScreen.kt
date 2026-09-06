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
                        "CreditDB curates public ratings into three core metrics: Standard Score (Deviation), Raw Score (AniList), and Quality Tier, allowing intuitive understanding of each work's relative standing."
                    else
                        "CreditDB では、誰でも直感的に作品の評価を把握できるよう、公開指標を「偏差値」「AniList素点」「Tier」の3点に厳選して表示しています。",
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
                            text = if (isEn) "📊 Standard Score (Era-Relative Quality)" else "📊 偏差値（年代相対クオリティ）",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isEn)
                                "Measures how prominently a title stood out among anime released in the same era, standardized to a mean of 50.0 and standard deviation of 10.0 using statistical Z-scores."
                            else
                                "「その作品が公開された年代のアニメ群の中で、どれだけ突出して評価されたか」を、統計的な標準偏差単位で算出し、平均を 50.0、標準偏差を 10.0 に規格化した指標です。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FormulaBox(formula = if (isEn) "Standard Score = 50.0 + 10.0 × Z_i" else "偏差値 = 50.0 + 10.0 × Z_i")
                        Text(
                            text = if (isEn)
                                "※ Since Z_i (Era-Adjusted Z) completely eliminates inflation and deflation across eras, 1980s classics and 2020s hits can be compared fairly side-by-side."
                            else
                                "※ Z_i（年代補正Z値）により年代ごとのインフレ・デフレが完全に補正されているため、1980年代の名作も2020年代の話題作も公平に横並び比較できます。",
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
                                "※ While raw score directly reflects global popularity and excitement, newer works suffer from score inflation due to larger voting populations. Refer to 'Standard Score' for true, era-independent quality comparison."
                            else
                                "※ 素点は世界的な人気や熱量を直接反映するメリットがありますが、年代が新しい作品ほど投票人口の増加により高得点化（インフレ）しやすい性質があります。時代背景を超えた真の実力比較には「偏差値」をご参照ください。",
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
                    title = if (isEn) "Era-Adjusted Z-Score (Z_i)" else "年代補正Z値（Z_i）について"
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
                                "The Era-Adjusted Z-Score (Local Z-Score Z_i) represents the intrinsic quality of a work, debiasing both user-level grading tendencies (generous vs strict) and era-level rating inflation/deflation via a two-stage statistical process."
                            else
                                "年代補正Z値（局所Zスコア Z_i）は、ユーザーごとの採点バイアス（甘口・辛口傾向）と、年代ごとの評価相場（投票人口差によるインフレ・デフレ）を二段階で数理的に除去した、作品の根源的なクオリティ指標です。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = if (isEn) "Stage 1 (Eliminating User Scoring Bias):" else "第1段階（ユーザー採点バイアスの除去）:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isEn)
                                "By solving the regularized alternating least squares (ALS) matrix factorization R_ui = μ + b_u + b_i + ε_ui, user-specific bias b_u is separated out to extract the pure item quality bias b_i."
                            else
                                "レビュー R_ui = μ + b_u + b_i + ε_ui の交互最小二乗法（正則化ALS行列分解）を解き、ユーザー特有の甘口・辛口バイアス b_u を除外して、作品の素の実力バイアス b_i を抽出します。",
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
                                "作品 i の公開年を中心とする前後3年間（計7年間）の移動窓において、作品群の平均 μ_year と標準偏差 σ_year を計算し、局所Zスコア化します：",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FormulaBox(formula = "Z_i = (b_i - μ_year) / σ_year")

                        Text(
                            text = if (isEn)
                                "This normalizes all anime across history into a common scale of mean 0.0 and standard deviation 1.0. Multiplying Z_i by 10 and adding 50 yields the Standard Score (50 + 10 × Z_i)."
                            else
                                "これにより、全年代の作品が「平均 0.0、標準偏差 1.0」の共通尺度に規格化されます。この Z_i を 10倍して 50 を加えたものが「偏差値（50 + 10 × Z_i）」です。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ==========================================
            // 第3章: アニメーター・制作陣の能力評価指標について
            // ==========================================
            item {
                GuideSectionHeader(
                    number = "3",
                    title = if (isEn) "Staff Ability & Career Metrics" else "制作陣の能力評価指標について"
                )
            }

            // 総合実力 S(a) Card
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
                            text = if (isEn) "🎯 Power Score S(a) (Bayesian Shrunken Mean)" else "🎯 総合実力 S(a)（ベイジアン平均評価値）",
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
                                "Calculated as the expected posterior mean to prevent creators with very few credits (|P|) from monopolizing top rankings due to a single lucky hit."
                            else
                                "参加作品数 |P| が極端に少ないスタッフが、たまたま1作のヒットによってランキング最上位を独占してしまう現象を防ぐため、事後分布の期待値として手堅く算出されます。",
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
                            text = if (isEn) "🏛️ Cumulative Record ΣZ (Total Career Contribution)" else "🏛️ 生涯累積実績 ΣZ（全作品通算貢献値）",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = if (isEn)
                                "The simple sum of era-adjusted Z-scores across all works the creator has contributed to."
                            else
                                "これまでに携わった全作品の年代補正Zスコアを単純合算した累積値です。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FormulaBox(formula = "Σ Z = Σ_{p ∈ P} Z_p")
                        Text(
                            text = if (isEn)
                                "Reflects the total volume of quality output delivered to the animation industry throughout a sustained career across numerous good works and masterpieces."
                            else
                                "長年のキャリアにわたり多数の良作・名作を支え続け、アニメーション産業全体にどれだけのクオリティ実績を積み上げてきたかという通算貢献量の総量を示します。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // クレジット表記の見方 [実力Tier / 累積Tier]
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
                            text = if (isEn) "🏷️ Dual Tier Badge [Power Tier / Cumulative Tier]" else "🏷️ クレジット表記 [実力Tier / 累積Tier] の見方",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEn)
                                "In the staff list on the work detail screen, each creator name is prefixed with [Power Tier / Cumulative Tier]:"
                            else
                                "作品詳細画面の制作陣一覧では、各スタッフ名の前に [総合実力Tier / 生涯累積実績Tier] を並べて表示しています：",
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
                                "• Left (Power Tier): Average expected quality delivered in that specific role.\n• Right (Cumulative Tier): Total volume of career contributions accumulated in the industry.\n• [S+ / S+]: Historic Top Creator\n• [S+ / B]: Rising Star / Pure Genius\n• [A / S+]: Veteran Master Key Animator sustaining the industry for decades"
                            else
                                "• 左側（総合実力Tier）: その役職として発揮される平均的なクオリティ水準の高さを示します。\n• 右側（生涯累積実績Tier）: その役職としてキャリア全体で業界に積み上げてきた通算貢献量の総量を示します。\n• [S+ / S+]: 歴史的トップクリエイター\n• [S+ / B]: 新進気鋭の超実力派\n• [A / S+]: 長年現場を支え続ける名匠原画",
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
                            if (isEn) "High responsibility per title; reflects ability with high sensitivity from few works." else "1作あたりの責任負荷が極めて大きいため、少本数から実力を高感度に反映。"
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
                            if (isEn) "Evaluates unique musical style and track record with sensitivity." else "劇伴作曲家としての作風・実績を高感度に評価。"
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
                            if (isEn) "Estimates solid ability with low variance over high credit volume." else "参加作品数が多いため、ブレを抑えて手堅く実力を推定。"
                        ),
                        Triple(
                            if (isEn) "All Roles Combined (all)" else "スタッフ全体総合 (all)",
                            "6.0",
                            if (isEn) "Baseline smoothing value for overall ability S(a) across all roles." else "全役職通算の総合実力 S(a) の平滑化基準値。"
                        )
                    )

                    roleParams.forEach { (role, mVal, note) ->
                        RoleParamRow(role = role, mVal = mVal, note = note)
                    }
                }
            }

            // ==========================================
            // 第5章: 制作スタジオの評価設計（人間至上主義）
            // ==========================================
            item {
                GuideSectionHeader(
                    number = "5",
                    title = if (isEn) "Studio Philosophy (Human-Centric Design)" else "制作スタジオの評価設計（人間至上主義）"
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
                            text = if (isEn) "🎨 Works Are Crafted by Humans, Not Corporate Brands" else "🎨 作品のクオリティは「箱」ではなく「人」が創る",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isEn)
                                "CreditDB deliberately does NOT compute Bayesian scores (S(a) or ΣZ) for animation studios, nor does our prediction model include studio identity as a feature."
                            else
                                "CreditDB では、制作スタジオに対して「実力 S(a)」や「累積 ΣZ」といったベイズ数理スコアを算出・付与していません。また、スタッフ予測モデルの特徴量からもスタジオ情報は意図的に除外しています。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEn)
                                "The visual beauty and narrative depth of animation are born from the craftsmanship, vision, and passion of individual creators—directors, scriptwriters, character designers, and animators—not corporate logos."
                            else
                                "アニメーションの映像美や脚本の妙味は、企業の商号（スタジオという器）ではなく、監督、シリーズ構成、キャラクターデザイン、作画監督、原画といった生身のクリエイターたちの卓越した職人技と情熱によって生み出されるものです。",
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
                            text = if (isEn) "📈 Interpreting 'Power vs Record'" else "📈 「実力 / 実績」の解釈と見方",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = if (isEn)
                                "The two metrics displayed on the right side of creator tables provide a multidimensional view of talent:"
                            else
                                "スタッフ検索や役職別ランキングのテーブル右側に表示される数値は、クリエイターの評価を多角的に捉えるための二連指標です：",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEn)
                                "• Power (Bayesian Mean S(a)): The expected average quality per work delivered in that role.\n• Record (Career Total ΣZ): Total cumulative industry contribution across their full career."
                            else
                                "• 実力（ベイジアン平均 S(a)）: その役職において手掛けた1作あたりの平均クオリティ水準（期待値）を表します。\n• 実績（生涯累積実績 ΣZ）: その役職としてキャリア全体で業界に積み上げてきた総貢献量を表します。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isEn)
                                "For example, a breakthrough talent who created a single masterpiece has high 'Power', while a seasoned veteran supporting the frontlines for decades has tremendous 'Record'. Viewing both metrics together reveals the true character of each creator's career."
                            else
                                "例えば、参加本数が少なくても傑作を生み出した新鋭は「実力」が高く、数十年にわたり現場を支え続ける名匠は「実績」が圧倒的に高くなります。両指標を併せて見ることで、クリエイターの真価を正確に理解できます。",
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
