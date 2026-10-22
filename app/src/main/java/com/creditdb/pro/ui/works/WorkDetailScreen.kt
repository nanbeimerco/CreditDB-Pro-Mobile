package com.creditdb.pro.ui.works

import androidx.compose.animation.AnimatedVisibility
import com.creditdb.pro.ui.theme.accent
import com.creditdb.pro.ui.components.AppLoadingIndicator
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
import com.creditdb.pro.ui.components.LargeTierBadge
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
    val repository = remember { CreditRepository.getInstance(context) }
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
                AppLoadingIndicator(contained = true, size = 56.dp)
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
            val episodeStaff = remember(work) { com.creditdb.pro.data.buildEpisodeStaff(work.staffByRole) }

            LazyColumn(
                modifier = modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // 1. タイトル & 基本情報ヘッダー
                item {
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    // M3 Expressive ヒーロー: カバー画像 + Tier の形状バッジ
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    val coverUrl = remember(work.id) {
                        com.creditdb.pro.data.tier.WorkImageResolver.getCoverImageUrl(work.id, ctx)
                    }
                    // percentile は「下位からの割合」(1位 = 100) なので、上位何%かに変換する
                    val topPercent = (100.0 - work.percentile).coerceAtLeast(0.1)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (coverUrl != null) {
                                    coil.compose.AsyncImage(
                                        model = coverUrl,
                                        contentDescription = primaryTitle,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier
                                            .width(88.dp)
                                            .height(124.dp)
                                            .clip(MaterialTheme.shapes.large)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LargeTierBadge(tier = work.tier)
                                    Text(
                                        text = if (isEn) "Top ${String.format("%.1f", topPercent)}%" else "上位 ${String.format("%.1f", topPercent)}%",
                                        style = MaterialTheme.typography.labelLargeEmphasized,
                                        color = tierSpec.accent
                                    )
                                }
                            }

                            Text(
                                text = primaryTitle,
                                style = MaterialTheme.typography.headlineSmallEmphasized,
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
                                text = if (isEn) "Released: ${work.year}" else "公開年: ${work.year}年",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                }

                // 2. 数理指標グリッド (基本指標 + Advancedモード切り替え)
                item {
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
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
                                valueColor = tierSpec.accent,
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
                }

                // 3. 制作陣・声優クレジット (Role by Role)
                item {
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(
                        text = if (isEn) "Staff & Cast Credits" else "制作陣・声優クレジット",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                }

                work.staffByRole.forEach { (roleKey, staffList) ->
                    if (staffList.isNotEmpty()) {
                        item(key = roleKey) {
                            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                            StaffRoleCard(
                                roleKey = roleKey,
                                staffList = staffList,
                                onStaffClick = onStaffClick
                            )
                        }
                        }
                    }
                }

                // 3b. 各話スタッフ（AniList の話数付きクレジット）
                if (episodeStaff.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(bottom = 8.dp)) {
                            Text(
                                text = if (isEn) "Episode Staff" else "各話スタッフ",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val total = work.episodeCount
                            Text(
                                text = if (isEn) {
                                    "From AniList episode credits" + (if (total != null) " · ${episodeStaff.size} of $total episodes" else "")
                                } else {
                                    "AniList の話数付きクレジットより" + (if (total != null) "（全${total}話中 ${episodeStaff.size}話分）" else "")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    itemsIndexed(
                        items = episodeStaff,
                        key = { _, ep -> "ep_${ep.episode}" }
                    ) { index, ep ->
                        EpisodeStaffRow(
                            ep = ep,
                            index = index,
                            count = episodeStaff.size,
                            onStaffClick = onStaffClick,
                            modifier = Modifier.padding(bottom = if (index == episodeStaff.size - 1) 16.dp else 0.dp)
                        )
                    }
                }

                // 4. 制作スタジオ（画面最下部に配置・タップで作品一覧）
                if (!work.studio.isNullOrBlank()) {
                    item {
                        Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        Text(
                            text = if (isEn) "Animation Studio" else "制作スタジオ",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    }

                    val studioDisplayName = com.creditdb.pro.data.StaffNameResolver.getStudioName(work.studio, isEn)

                    item {

                        Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStudioClick(work.studio) },
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

                // 5. データソース・クレジット表記（最下部フッター）
                item {
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = com.creditdb.pro.ui.theme.AppStrings.workDetailDataFooter(isEn),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
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
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
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
                        val epText = com.creditdb.pro.data.formatEpisodes(staff.episodes, staff.parts, isEn)
                        if (epText != null) {
                            Text(
                                text = epText,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
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

/**
 * 各話スタッフの1行。タップで、その話の全スタッフ（原画を含む）を展開する。名前をタップでクリエイター詳細へ。
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun EpisodeStaffRow(
    ep: com.creditdb.pro.data.EpisodeStaff,
    index: Int,
    count: Int,
    onStaffClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEn = com.creditdb.pro.ui.theme.LanguageManager.isEnglish
    var expanded by remember { mutableStateOf(false) }
    fun disp(n: String) = com.creditdb.pro.data.StaffNameResolver.getStaffName(n, isEn)
    com.creditdb.pro.ui.components.GroupedListItem(
        index = index,
        count = count,
        onClick = { expanded = !expanded },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isEn) "Episode ${ep.episode}" else "第${ep.episode}話",
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                val ka = ep.keyAnimators.size + ep.secondKeyAnimators.size
                if (ka > 0) {
                    Text(
                        text = if (isEn) "$ka key animators" else "原画 ${ka}人",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) (if (isEn) "Collapse" else "閉じる") else (if (isEn) "Expand" else "展開"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (ep.unitDirectors.isNotEmpty()) {
                Text(
                    text = (if (isEn) "Storyboard / Ep. Director: " else "絵コンテ・演出: ") + ep.unitDirectors.joinToString(if (isEn) ", " else "、") { disp(it) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (ep.animationDirectors.isNotEmpty()) {
                Text(
                    text = (if (isEn) "Animation Director: " else "作画監督: ") + ep.animationDirectors.joinToString(if (isEn) ", " else "、") { disp(it) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expanded) {
                val groups = listOf(
                    (if (isEn) "Storyboard / Ep. Director" else "絵コンテ・演出") to ep.unitDirectors,
                    (if (isEn) "Animation Director" else "作画監督") to ep.animationDirectors,
                    (if (isEn) "Key Animation" else "原画") to ep.keyAnimators,
                    (if (isEn) "2nd Key Animation" else "第二原画") to ep.secondKeyAnimators
                ).filter { it.second.isNotEmpty() }
                groups.forEach { (label, names) ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        names.distinct().forEach { n ->
                            Surface(
                                onClick = { onStaffClick(n) },
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ) {
                                Text(
                                    text = disp(n),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
