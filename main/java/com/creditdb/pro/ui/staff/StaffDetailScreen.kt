package com.creditdb.pro.ui.staff

import androidx.compose.animation.AnimatedVisibility
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
    val repository = remember { CreditRepository(context) }
    var profile by remember { mutableStateOf<StaffProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showAdvancedRoleStats by remember { mutableStateOf(false) }

    LaunchedEffect(staffName) {
        isLoading = true
        profile = repository.getStaffProfile(staffName)
        isLoading = false
    }

    val isEn = com.creditdb.pro.ui.theme.LanguageManager.isEnglish

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEn) "Creator Details" else "クリエイター詳細",
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
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (profile == null) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
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
                modifier = modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. スタッフ名ヘッダーカード
                item {
                    val primaryName = if (isEn) {
                        com.creditdb.pro.data.StaffNameResolver.getStaffName(prof.name, true)
                    } else {
                        prof.name
                    }
                    val secondaryName = if (isEn && primaryName != prof.name) prof.name else null

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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

                // 2. 2大サマリーカード: 総合実力 S(a) & 生涯累積実績 ΣZ
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
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
                                        color = TierTheme.forTier(prof.overallRatingTier).onContainerColor
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
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
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
                                        color = TierTheme.forTier(prof.overallCumTier).onContainerColor
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

                // 3. 部門別実績セクション
                if (prof.allRoleStats.isNotEmpty()) {
                    item {
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

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
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
                                                            color = statRatingSpec.onContainerColor
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
                                                            color = statCumSpec.onContainerColor
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
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. キャリア参加作品一覧 (Career Trajectory)
                if (prof.careerTrajectory.isNotEmpty()) {
                    item {
                        Text(
                            text = if (isEn) "Filmography (${prof.careerTrajectory.size} works)" else "参加作品一覧 (全 ${prof.careerTrajectory.size} 作品)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                prof.careerTrajectory.forEach { traj ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onWorkClick(traj.work_id) }
                                            .padding(horizontal = 8.dp, vertical = 7.dp),
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
    }
}
