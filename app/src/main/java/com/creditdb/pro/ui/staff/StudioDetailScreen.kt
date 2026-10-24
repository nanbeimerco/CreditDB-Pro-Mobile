package com.creditdb.pro.ui.staff

import androidx.compose.foundation.background
import com.creditdb.pro.ui.theme.accent
import com.creditdb.pro.ui.components.AppLoadingIndicator
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Star
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
import com.creditdb.pro.data.CreditRepository
import com.creditdb.pro.data.StudioStaffMember
import com.creditdb.pro.data.StudioWorkItem
import com.creditdb.pro.ui.components.RoleBadge
import com.creditdb.pro.ui.components.TierBadge
import com.creditdb.pro.ui.theme.AppStrings
import com.creditdb.pro.ui.theme.TierTheme

private data class DeptOption(val key: String, val labelJa: String, val labelEn: String)

private val DEPARTMENTS = listOf(
    DeptOption("all", "全体", "All"),
    DeptOption("sakkan", "作画監督", "Animation"),
    DeptOption("genga", "原画", "Key Anim"),
    DeptOption("genga_2nd", "2原", "2nd KA"),
    DeptOption("director", "監督", "Director"),
    DeptOption("unit_director", "演出・絵コンテ", "Episode Dir"),
    DeptOption("char_design", "キャラデザ", "Ch. Design"),
    DeptOption("series_comp", "シリーズ構成", "Script"),
    DeptOption("art_dir", "美術監督", "Art Dir"),
    DeptOption("music", "音楽", "Music"),
    DeptOption("cv", "声優・キャスト", "Cast")
)

/**
 * 制作スタジオ詳細画面
 * クリエイター個人至上主義に基づき、スタジオへの実力・累計値算出は行わず、
 * そのスタジオを支える主要アニメーター・制作陣の貢献度と手掛けた作品一覧を表示する。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioDetailScreen(
    studioName: String,
    onBackClick: () -> Unit,
    onWorkClick: (String) -> Unit,
    onStaffClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { CreditRepository.getInstance(context) }
    var works by remember { mutableStateOf<List<StudioWorkItem>>(emptyList()) }
    var staffList by remember { mutableStateOf<List<StudioStaffMember>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedDept by remember { mutableStateOf("all") }
    var sortOption by remember { mutableStateOf("weighted") } // "weighted" or "count"

    val isEn = com.creditdb.pro.ui.theme.LanguageManager.isEnglish

    LaunchedEffect(studioName) {
        isLoading = true
        val w = repository.getStudioWorks(studioName)
        val s = repository.getStudioStaff(studioName)
        works = w
        staffList = s
        isLoading = false
    }

    // 部門絞り込みとソート
    val filteredStaff = remember(staffList, selectedDept, sortOption) {
        if (selectedDept == "all") {
            if (sortOption == "weighted") {
                staffList.sortedByDescending { it.weightedScore }
            } else {
                staffList.sortedByDescending { it.totalWorks }
            }
        } else {
            staffList.filter { (it.rolesBreakdown[selectedDept] ?: 0) > 0 }
                .sortedWith(
                    compareByDescending<StudioStaffMember> { it.rolesBreakdown[selectedDept] ?: 0 }
                        .thenByDescending { it.weightedScore }
                )
        }
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 120
        }
    }
    val primaryStudioName = com.creditdb.pro.data.StaffNameResolver.getStudioName(studioName, isEn)
    val secondaryStudioName = if (isEn && primaryStudioName != studioName) studioName else null

    // 上部1行フローティングバー: 戻るボタン + スクロール時にスタジオ名カプセルがフェードイン
    com.creditdb.pro.ui.glass.ProvideTopBar {
        com.creditdb.pro.ui.glass.FloatingTopBar(
            title = primaryStudioName,
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
        } else {
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
                // 1. スタジオ名ヘッダーカード
                item {
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
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
                                    text = primaryStudioName,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                RoleBadge(roleKey = "studio")
                            }

                            if (secondaryStudioName != null) {
                                Text(
                                    text = secondaryStudioName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = if (isEn) "Total Works: ${works.size}" else "通算制作: ${works.size} 作品",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = if (isEn) "Creators: ${staffList.size}" else "制作陣: ${staffList.size} 名",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isEn) {
                                        "※ Rooted in creator individualism, our mathematical model values individual human talent and highlights the core animators, directors, and creators shaping each studio."
                                    } else {
                                        "※ 本アプリの数理評価モデルは制作スタッフ個人のクレジットに主眼を置いており、スタジオを支える主要アニメーターや監督陣の貢献度を可視化しています。"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
                }

                // 2. セグメント切り替え (M3 Expressive SingleChoiceButtonGroup: 主要制作陣 / 制作作品一覧)
                item {
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        com.creditdb.pro.ui.components.SingleChoiceButtonGroup(
                            options = listOf(0, 1),
                            selected = selectedTabIndex,
                            onSelect = { selectedTabIndex = it },
                            label = { idx ->
                                if (idx == 0) {
                                    if (isEn) "Key Creators (${staffList.size})" else "主要制作陣 (${staffList.size})"
                                } else {
                                    if (isEn) "Works (${works.size})" else "制作作品 (${works.size})"
                                }
                            },
                            scrollable = false,
                        )
                    }
                }

                // 3. タブコンテンツ
                if (selectedTabIndex == 0) {
                    // --- 主要制作陣タブ ---
                    item {
                        Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 部門別フィルターチップ (水平スクロール)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DEPARTMENTS.forEach { dept ->
                                    val isSelected = selectedDept == dept.key
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedDept = dept.key },
                                        label = {
                                            Text(
                                                text = if (isEn) dept.labelEn else dept.labelJa,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }

                            // 全体タブの場合のソート切り替え
                            if (selectedDept == "all") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (isEn) "Sort:" else "並び順:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    com.creditdb.pro.ui.components.SingleChoiceButtonGroup(
                                        options = listOf("weighted", "count"),
                                        selected = sortOption,
                                        onSelect = { sortOption = it },
                                        label = {
                                            if (it == "weighted") (if (isEn) "Contribution" else "貢献度順")
                                            else (if (isEn) "Works" else "作品数順")
                                        },
                                        compact = true
                                    )
                                }
                            } else {
                                val currentDeptLabel = DEPARTMENTS.firstOrNull { it.key == selectedDept }
                                Text(
                                    text = if (isEn) {
                                        "Showing creators sorted by ${currentDeptLabel?.labelEn} credits at $primaryStudioName"
                                    } else {
                                        "$primaryStudioName での「${currentDeptLabel?.labelJa}」担当作品数順に表示中"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                    }

                    if (filteredStaff.isEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isEn) "No creators found for this department" else "該当するクリエイターが見つかりませんでした",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        }
                    } else {
                        itemsIndexed(filteredStaff, key = { _, it -> it.name }) { index, member ->
                            val (displayName, secondaryName) = member.getDisplayName(isEn)
                            val deptCount = if (selectedDept != "all") member.rolesBreakdown[selectedDept] else null

                            Card(
                                modifier = Modifier
                                    .padding(bottom = 16.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = onStaffClick != null) {
                                        onStaffClick?.invoke(member.name)
                                    },
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 上段: 順位 + 名前 + Tierバッジ + 参加数
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // 順位バッジ
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = when (index) {
                                                    0 -> Color(0xFFFFD700).copy(alpha = 0.2f)
                                                    1 -> Color(0xFFC0C0C0).copy(alpha = 0.2f)
                                                    2 -> Color(0xFFCD7F32).copy(alpha = 0.2f)
                                                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                                }
                                            ) {
                                                Text(
                                                    text = "#${index + 1}",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 11.sp,
                                                        fontFamily = FontFamily.SansSerif,
                                                        fontWeight = FontWeight.Black
                                                    ),
                                                    color = when (index) {
                                                        0 -> Color(0xFFD4AF37)
                                                        1 -> Color(0xFF808080)
                                                        2 -> Color(0xFFB87333)
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            }

                                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                                Text(
                                                    text = displayName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (secondaryName != null) {
                                                    Text(
                                                        text = secondaryName,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (member.ratingTier != null) {
                                                TierBadge(tier = member.ratingTier, prefix = "")
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = if (deptCount != null) "${deptCount}作" else "${member.totalWorks}作",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontFamily = FontFamily.SansSerif,
                                                        fontWeight = FontWeight.ExtraBold
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (selectedDept == "all" && sortOption == "weighted") {
                                                    Text(
                                                        text = "★${String.format("%.1f", member.weightedScore)}",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 10.sp,
                                                            fontFamily = FontFamily.SansSerif,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }

                                            if (onStaffClick != null) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                    contentDescription = if (isEn) "Staff Details" else "スタッフ詳細",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    // 中段: 担当役職ピル
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        member.primaryRoles.take(4).forEach { roleKey ->
                                            val count = member.rolesBreakdown[roleKey] ?: 0
                                            val isHighlight = selectedDept == roleKey
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isHighlight) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)
                                            ) {
                                                Text(
                                                    text = "${AppStrings.roleCompact(roleKey, isEn)} $count",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isHighlight) MaterialTheme.colorScheme.onPrimaryContainer
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        if (member.primaryRoles.size > 4) {
                                            Text(
                                                text = "+${member.primaryRoles.size - 4}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.align(Alignment.CenterVertically)
                                            )
                                        }
                                    }

                                    // 下段: 代表作サマリー
                                    if (member.sampleWorks.isNotEmpty()) {
                                        Text(
                                            text = (if (isEn) "Works: " else "代表作: ") + member.sampleWorks.joinToString(" / "),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // --- 制作作品一覧タブ ---
                    item {
                        Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        Text(
                            text = if (isEn) "Produced Works (Chronological · ${works.size} works)" else "制作作品一覧 (制作年順・全 ${works.size} 作品)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    }

                    if (works.isEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isEn) "No produced works found" else "制作作品が見つかりませんでした",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        }
                    } else {
                        itemsIndexed(
                            items = works,
                            key = { index, item -> "${item.workId}_$index" }
                        ) { index, item ->
                            com.creditdb.pro.ui.components.GroupedListItem(
                                index = index,
                                count = works.size,
                                onClick = { onWorkClick(item.workId) }
                            ) {
                                        val itemTierSpec = TierTheme.forTier(item.tier)
                                        Row(
                                            modifier = Modifier
    .fillMaxWidth()
    .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                                ) {
                                                    Text(
                                                        text = if (isEn) "${item.year}" else "${item.year}年",
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                Text(
                                                    text = item.getDisplayTitle(isEn),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = String.format("%.1f", item.deviationScore),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = itemTierSpec.accent
                                                )
                                                TierBadge(tier = item.tier, prefix = "")
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

