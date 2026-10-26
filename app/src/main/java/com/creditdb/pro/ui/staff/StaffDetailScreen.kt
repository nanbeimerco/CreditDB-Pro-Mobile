package com.creditdb.pro.ui.staff

import androidx.compose.animation.AnimatedVisibility
import com.creditdb.pro.ui.theme.accent
import com.creditdb.pro.ui.components.AppLoadingIndicator
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditdb.pro.data.CareerTrajectoryItem
import com.creditdb.pro.data.CreditRepository
import com.creditdb.pro.data.RoleConstants
import com.creditdb.pro.data.RoleStat
import com.creditdb.pro.data.StaffProfile
import com.creditdb.pro.ui.components.DualTierBadge
import com.creditdb.pro.ui.components.LargeTierBadge
import com.creditdb.pro.ui.components.RoleBadge
import com.creditdb.pro.ui.components.TierBadge
import com.creditdb.pro.ui.theme.TierTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffDetailScreen(
    staffName: String,
    onBackClick: () -> Unit,
    onWorkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { CreditRepository.getInstance(context) }
    var profile by remember { mutableStateOf<StaffProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showAdvancedRoleStats by remember { mutableStateOf(false) }
    var metrics by remember { mutableStateOf<List<com.creditdb.pro.data.StaffMetric>>(emptyList()) }

    LaunchedEffect(staffName) {
        isLoading = true
        profile = repository.getStaffProfile(staffName)
        metrics = repository.getStaffMetrics(profile?.name ?: staffName)
        isLoading = false
    }

    val isEn = com.creditdb.pro.ui.theme.LanguageManager.isEnglish
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 120
        }
    }
    val topBarTitle = remember(profile, isEn, staffName) {
        val raw = profile?.name ?: staffName
        if (isEn) com.creditdb.pro.data.StaffNameResolver.getStaffName(raw, true) else raw
    }

    // 上部1行フローティングバー: 戻るボタン + スクロール時にスタッフ名カプセルがフェードイン
    com.creditdb.pro.ui.glass.ProvideTopBar {
        com.creditdb.pro.ui.glass.FloatingTopBar(
            title = topBarTitle,
            scrolled = isScrolled,
            onBack = onBackClick,
            glassBackdrop = true,
            backDescription = if (isEn) "Back" else "戻る",
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(com.creditdb.pro.ui.glass.LocalChromePadding.current),
                contentAlignment = Alignment.Center
            ) {
                AppLoadingIndicator(contained = true, size = 56.dp)
            }
        } else if (profile == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(com.creditdb.pro.ui.glass.LocalChromePadding.current),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isEn) "Creator information not found" else "スタッフ情報が見つかりませんでした",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val prof = profile!!
            val isCv = prof.primaryRole == "cv"

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = com.creditdb.pro.ui.glass.rememberChromeContentPadding(
                    horizontal = 16.dp,
                    extraTop = 4.dp,
                    extraBottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // 1. スタッフ名ヘッダーカード
                item {
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    val primaryName = if (isEn) {
                        com.creditdb.pro.data.StaffNameResolver.getStaffName(prof.name, true)
                    } else {
                        prof.name
                    }
                    val secondaryName = if (isEn && primaryName != prof.name) prof.name else null

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = primaryName,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                RoleBadge(roleKey = prof.primaryRole)
                            }

                            if (secondaryName != null) {
                                Text(
                                    text = secondaryName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = if (isEn) "Total Participations: ${prof.totalWorks} works" else "通算参加作品数: ${prof.totalWorks} 作品",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                }

                // 2. 2大サマリーカード: 総合実力 S(a) & 生涯累積実績 ΣZ
                item {
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = if (isEn) "🎯 Power Score S(a)" else "🎯 総合実力",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = String.format("%+.3f", prof.bayesianRating),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TierTheme.forTier(prof.overallRatingTier).accent
                                    )
                                    Text(
                                        text = if (isEn) "#${prof.overallRank}" else "#${prof.overallRank} 位",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                LargeTierBadge(tier = prof.overallRatingTier)
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = if (isEn) "🏛️ Cumulative Record ΣZ" else "🏛️ 生涯累積実績",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = String.format("%+.2f", prof.careerCumulativeZ),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TierTheme.forTier(prof.overallCumTier).accent
                                    )
                                    Text(
                                        text = if (isEn) "#${prof.cumulativeRank}" else "#${prof.cumulativeRank} 位",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                LargeTierBadge(tier = prof.overallCumTier)
                            }
                        }
                    }
                }
                }

                // 2b. 新しい指標（各話評価・個人寄与度）。既存の2指標と並べて表示する
                val allMetrics = metrics.filter { it.role == "all" }.associateBy { it.metric }
                if (!isCv && allMetrics.isNotEmpty()) {
                    item(key = "new_metrics") {
                        Column(modifier = Modifier.padding(bottom = 16.dp)) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = if (isEn) "New Metrics (for reference)" else "新しい指標（参考）",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        NewMetricCell(
                                            label = if (isEn) "🎬 Episode Score" else "🎬 各話評価",
                                            metric = allMetrics["episode"],
                                            countText = { n -> if (isEn) "$n episodes" else "${n}話" },
                                            modifier = Modifier.weight(1f)
                                        )
                                        NewMetricCell(
                                            label = if (isEn) "🧮 Contribution" else "🧮 個人寄与度",
                                            metric = allMetrics["contribution"],
                                            countText = { n -> if (isEn) "$n works" else "${n}作品" },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Text(
                                        text = if (isEn) {
                                            "Episode Score: how much higher this person's episodes were rated on IMDb than the rest of each series. Contribution: their own effect on work scores, with studio and series effects removed (ridge regression). See the Guide for how well each one predicts."
                                        } else {
                                            "各話評価: 担当した話が、同じ作品のほかの話より IMDb でどれだけ高く評価されたか。個人寄与度: スタジオとシリーズの影響を除いた、その人自身が作品評価に与えた影響（リッジ回帰）。どれだけ当たるかはガイドを参照"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. 部門別実績セクション
                if (prof.allRoleStats.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isEn) "Role Breakdown & Tiers" else "部門別実績 & Tier",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilterChip(
                                selected = showAdvancedRoleStats,
                                onClick = { showAdvancedRoleStats = !showAdvancedRoleStats },
                                label = {
                                    Text(
                                        text = "Advanced",
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                },
                                leadingIcon = if (showAdvancedRoleStats) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                    }

                    item {

                        Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateContentSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                prof.allRoleStats.forEach { stat ->
                                    val statRatingSpec = TierTheme.forTier(stat.rating_tier)
                                    val statCumSpec = TierTheme.forTier(stat.cum_tier)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        // 1行目: 役職名（左）と DualTierBadge（右）を固定配置（左右に一切ブレない）
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                RoleBadge(roleKey = stat.role)
                                                Text(
                                                    text = if (isEn) "${stat.works_count} works" else "${stat.works_count} 作品",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            DualTierBadge(
                                                ratingTier = stat.rating_tier,
                                                cumulativeTier = stat.cum_tier
                                            )
                                        }

                                        // 2行目: 実力と累計の数値 + 部門順位（右寄せ・上下方向のアニメーションのみ）
                                        AnimatedVisibility(
                                            visible = showAdvancedRoleStats,
                                            enter = fadeIn() + expandVertically(),
                                            exit = fadeOut() + shrinkVertically()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    // 実力グループ
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isEn) "Rating ${String.format("%+.3f", stat.bayesian_rating)}" else "実力 ${String.format("%+.3f", stat.bayesian_rating)}",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                            fontWeight = FontWeight.Bold,
                                                            color = statRatingSpec.accent
                                                        )
                                                        if (stat.rating_rank > 0) {
                                                            Text(
                                                                text = if (isEn) "#${stat.rating_rank}" else "#${stat.rating_rank} 位",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }

                                                    Text(
                                                        text = "•",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                    )

                                                    // 累計グループ
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isEn) "Cumul ${String.format("%+.2f", stat.career_cumulative_z)}" else "累計 ${String.format("%+.2f", stat.career_cumulative_z)}",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                            fontWeight = FontWeight.Bold,
                                                            color = statCumSpec.accent
                                                        )
                                                        if (stat.cumulative_rank > 0) {
                                                            Text(
                                                                text = if (isEn) "#${stat.cumulative_rank}" else "#${stat.cumulative_rank} 位",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        val roleMetrics = metrics.filter { it.role == stat.role }.associateBy { it.metric }
                                        if (roleMetrics.isNotEmpty()) {
                                            AnimatedVisibility(
                                                visible = showAdvancedRoleStats,
                                                enter = fadeIn() + expandVertically(),
                                                exit = fadeOut() + shrinkVertically()
                                            ) {
                                                Text(
                                                    text = listOfNotNull(
                                                        roleMetrics["episode"]?.let { m ->
                                                            (if (isEn) "Episode " else "各話 ") + String.format("%+.3f", m.value) + (if (isEn) " #${m.rank}" else " #${m.rank} 位")
                                                        },
                                                        roleMetrics["contribution"]?.let { m ->
                                                            (if (isEn) "Contrib " else "寄与 ") + String.format("%+.3f", m.value) + (if (isEn) " #${m.rank}" else " #${m.rank} 位")
                                                        }
                                                    ).joinToString("  •  "),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    }
                }

                // 4. キャリア参加作品一覧 (Career Trajectory)
                if (prof.careerTrajectory.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        Text(
                            text = if (isEn) "Filmography (${prof.careerTrajectory.size} works)" else "参加作品一覧 (全 ${prof.careerTrajectory.size} 作品)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    }

                    itemsIndexed(
                        items = prof.careerTrajectory,
                        key = { index, traj -> "${traj.work_id}_${traj.role}_$index" }
                    ) { index, traj ->
                        com.creditdb.pro.ui.components.GroupedListItem(
                            index = index,
                            count = prof.careerTrajectory.size,
                            onClick = { onWorkClick(traj.work_id) }
                        ) {
                                    Row(
                                        modifier = Modifier
    .fillMaxWidth()
    .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                // 参加部門バッジ (原画, 監督, 演出, 声優等)
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                                ) {
                                                    Text(
                                                        text = com.creditdb.pro.data.RoleDefinitions.getShortDisplayName(traj.role, isEn),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                Text(
                                                    text = traj.getDisplayTitle(isEn),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = if (isEn) 2 else 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Text(
                                                    text = "(${traj.year})",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            val secondaryTitle = traj.getSecondaryTitle(isEn)
                                            if (!secondaryTitle.isNullOrBlank()) {
                                                Text(
                                                    text = secondaryTitle,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(start = 2.dp, top = 1.dp)
                                                )
                                            }

                                            // 担当話（AniList の話数付きクレジット）
                                            val epText = com.creditdb.pro.data.formatEpisodes(traj.eps, traj.parts, isEn)
                                            if (epText != null) {
                                                Text(
                                                    text = if (isEn) "Worked on: $epText" else "担当: $epText",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                                                )
                                            }

                                            // 声優のキャラクター役名が存在する場合は表示
                                            val displayCharName = traj.getDisplayCharacterName(isEn)
                                            if (!displayCharName.isNullOrBlank()) {
                                                Text(
                                                    text = if (isEn) "as: $displayCharName" else "役: $displayCharName",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = String.format("Z: %+.2f", traj.z_score),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = if (isEn) "Work Details" else "作品詳細",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                    }
                }
            }
        }
    }
}

/** 新指標の1マス（値・順位・対象数）。値が無い人は「データ不足」 */
@Composable
private fun NewMetricCell(
    label: String,
    metric: com.creditdb.pro.data.StaffMetric?,
    countText: (Int) -> String,
    modifier: Modifier = Modifier
) {
    val isEn = com.creditdb.pro.ui.theme.LanguageManager.isEnglish
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (metric == null) {
            Text(
                text = if (isEn) "Not enough data" else "データ不足",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = String.format("%+.3f", metric.value),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = TierTheme.forTier(metric.tier).accent
            )
            Text(
                text = (if (isEn) "#${metric.rank} of ${metric.total}" else "${metric.total}人中 #${metric.rank} 位") + " · " + countText(metric.n),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
