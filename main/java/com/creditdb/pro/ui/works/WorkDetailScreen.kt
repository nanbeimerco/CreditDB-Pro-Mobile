package com.creditdb.pro.ui.works

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditdb.pro.data.AppSettings
import com.creditdb.pro.data.CharacterCast
import com.creditdb.pro.data.CreditRepository
import com.creditdb.pro.data.RoleConstants
import com.creditdb.pro.data.StaffCredit
import com.creditdb.pro.data.StudioWorkItem
import com.creditdb.pro.data.WorkDetail
import com.creditdb.pro.ui.components.DualTierBadge
import com.creditdb.pro.ui.components.MetricCard
import com.creditdb.pro.ui.components.RoleBadge
import com.creditdb.pro.ui.components.TierBadge
import com.creditdb.pro.ui.components.VerdictBadge
import com.creditdb.pro.ui.theme.TierTheme
import com.creditdb.pro.ui.theme.VerdictSurprise
import com.creditdb.pro.ui.theme.VerdictUnderperform

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkDetailScreen(
    workId: String,
    onBackClick: () -> Unit,
    onStaffClick: (String) -> Unit,
    onWorkClick: (String) -> Unit = {},
    onStudioClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { CreditRepository(context) }
    var workDetail by remember { mutableStateOf<WorkDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(workId) {
        isLoading = true
        workDetail = repository.getWorkDetail(workId)
        isLoading = false
    }

    val isEn = com.creditdb.pro.ui.theme.LanguageManager.isEnglish

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEn) "Work Details" else "作品詳細",
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
        } else if (workDetail == null) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isEn) "Work information not found" else "作品情報が見つかりませんでした",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val work = workDetail!!
            val tierSpec = TierTheme.forTier(work.tier)
            val (primaryTitle, secondaryTitle) = work.getDisplayTitle(isEn)

            LazyColumn(
                modifier = modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. タイトル & 基本情報ヘッダー
                item {
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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TierBadge(tier = work.tier, prefix = "Tier ")
                            }

                            Text(
                                text = primaryTitle,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (!secondaryTitle.isNullOrBlank()) {
                                Text(
                                    text = secondaryTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = if (isEn) {
                                    "Released: ${work.year} (Top ${String.format("%.1f", work.percentile)}%)"
                                } else {
                                    "公開年: ${work.year}年 (上位 ${String.format("%.1f", work.percentile)}%)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 2. 数理指標グリッド (基本指標 + Advancedモード切り替え)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isEn) "Metrics & Comparison" else "数理評価・対比指標",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = AppSettings.isAdvancedMetricsEnabled,
                            onClick = { AppSettings.setAdvancedMetrics(!AppSettings.isAdvancedMetricsEnabled) },
                            label = {
                                Text(
                                    text = "Advanced",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            leadingIcon = if (AppSettings.isAdvancedMetricsEnabled) {
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
                    Spacer(modifier = Modifier.height(6.dp))

                    val resColor = if (work.residual > 0.4) VerdictSurprise else if (work.residual < -0.4) VerdictUnderperform else MaterialTheme.colorScheme.onSurface

                    Column(
                        modifier = Modifier.animateContentSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val verdictShort = com.creditdb.pro.ui.theme.AppStrings.verdictCompact(work.performanceVerdict, isEn)

                        // 基本指標 1行目: 偏差値 & AniList素点
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricCard(
                                title = if (isEn) "Deviation Score" else "偏差値",
                                value = String.format("%.1f", work.deviationScore),
                                subValue = if (isEn) "Overall #${work.deviationRank}" else "総合 #${work.deviationRank}",
                                valueColor = tierSpec.onContainerColor,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            MetricCard(
                                title = if (isEn) "AniList Raw" else "AniList素点",
                                value = if (isEn) String.format("%.1f pts", work.anilistRawScore) else String.format("%.1f点", work.anilistRawScore),
                                subValue = if (isEn) "Raw #${work.rawRank}" else "素点 #${work.rawRank}",
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }

                        // 基本指標 2行目: スタッフ予測点 & 総合判定
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricCard(
                                title = if (isEn) "Predicted Score" else "スタッフ予測点",
                                value = if (isEn) String.format("%.1f pts", work.predictedScore) else String.format("%.1f点", work.predictedScore),
                                subValue = if (isEn) "Pred #${work.predScoreRank}" else "予測 #${work.predScoreRank}",
                                valueColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            MetricCard(
                                title = if (isEn) "Verdict" else "総合判定",
                                value = verdictShort,
                                subValue = if (isEn) "vs Expectation" else "期待値対比",
                                valueColor = resColor,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }

                        // 高度指標: Advanced モード時のみ展開
                        AnimatedVisibility(visible = AppSettings.isAdvancedMetricsEnabled) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MetricCard(
                                        title = if (isEn) "Debiased b_i" else "バイアス補正 b_i",
                                        value = String.format("%+.2f", work.debiasedScore),
                                        subValue = if (isEn) "User Bias Removed" else "ユーザー補正後",
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                    MetricCard(
                                        title = if (isEn) "True Score Z_i" else "年代補正真値 Z_i",
                                        value = String.format("%+.3f", work.trueZScore),
                                        subValue = if (isEn) "Inflation Adjusted" else "時代インフレ除去",
                                        valueColor = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MetricCard(
                                        title = if (isEn) "Residual ΔZ" else "乖離残差 ΔZ",
                                        value = String.format("%+.3f", work.residual),
                                        subValue = if (isEn) "True - Predicted" else "真値 - 予測値",
                                        valueColor = resColor,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                    MetricCard(
                                        title = if (isEn) "Staff Latent Z" else "スタッフ潜在 Z値",
                                        value = String.format("%+.3f", work.predictedZScore),
                                        subValue = if (isEn) "Staff Potential" else "制作陣潜在評価",
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. 制作陣・声優クレジット (Role by Role)
                item {
                    Text(
                        text = if (isEn) "Staff & Cast Credits" else "制作陣・声優クレジット",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                work.staffByRole.forEach { (roleKey, staffList) ->
                    if (staffList.isNotEmpty()) {
                        item(key = roleKey) {
                            StaffRoleCard(
                                roleKey = roleKey,
                                staffList = staffList,
                                onStaffClick = onStaffClick
                            )
                        }
                    }
                }

                // 4. 制作スタジオ（画面最下部に配置・タップで作品一覧）
                if (!work.studio.isNullOrBlank()) {
                    item {
                        Text(
                            text = if (isEn) "Animation Studio" else "制作スタジオ",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    val studioDisplayName = com.creditdb.pro.data.StaffNameResolver.getStudioName(work.studio, isEn)

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStudioClick(work.studio) },
                            shape = RoundedCornerShape(10.dp),
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = studioDisplayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isEn) "Tap to view studio works & statistics" else "タップして制作作品・偏差値一覧を表示",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = if (isEn) "Studio Works" else "制作作品一覧",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 役職別スタッフカード (声優の場合は配役名・種別タグも併記 & 5名超過時は自動折りたたみ)
 */
@Composable
fun StaffRoleCard(
    roleKey: String,
    staffList: List<StaffCredit>,
    onStaffClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(staffList.size <= 5) }
    val isEn = com.creditdb.pro.ui.theme.LanguageManager.isEnglish

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RoleBadge(roleKey = roleKey)
                    Text(
                        text = if (isEn) "${staffList.size} members" else "${staffList.size}名",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (staffList.size > 5) {
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) (if (isEn) "Collapse" else "閉じる") else (if (isEn) "Expand" else "展開")
                        )
                    }
                }
            }

            val displayList = if (expanded) staffList else staffList.take(5)

            displayList.forEach { staff ->
                val displayName = staff.getDisplayName(isEn)
                val relationTag = staff.relation?.let { com.creditdb.pro.ui.theme.AppStrings.relationCompact(it, isEn) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onStaffClick(staff.name) }
                        .padding(horizontal = 6.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!relationTag.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Text(
                                        text = relationTag,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        val displayCharName = staff.getDisplayCharacterName(isEn)
                        if (!displayCharName.isNullOrBlank()) {
                            Text(
                                text = if (isEn) "as: $displayCharName" else "役: $displayCharName",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DualTierBadge(
                        ratingTier = staff.ratingTier,
                        cumulativeTier = staff.cumulativeTier
                    )
                }
            }

            if (!expanded && staffList.size > 5) {
                Text(
                    text = if (isEn) "Show ${staffList.size - 5} more..." else "他 ${staffList.size - 5} 名を表示...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { expanded = true }
                        .padding(start = 6.dp, top = 2.dp)
                )
            }
        }
    }
}
