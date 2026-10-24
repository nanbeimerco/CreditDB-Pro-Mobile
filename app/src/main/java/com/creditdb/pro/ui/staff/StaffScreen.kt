package com.creditdb.pro.ui.staff

import androidx.compose.foundation.background
import com.creditdb.pro.ui.theme.accent
import com.creditdb.pro.ui.components.AppLoadingIndicator
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.creditdb.pro.data.DebutEraFilter
import com.creditdb.pro.data.LeaderboardItem
import com.creditdb.pro.data.RoleConstants
import com.creditdb.pro.data.StaffSortOption
import com.creditdb.pro.ui.components.DualTierBadge
import com.creditdb.pro.ui.components.RoleBadge
import com.creditdb.pro.ui.components.SmartSearchBar
import com.creditdb.pro.ui.components.StaffFilterBottomSheet
import com.creditdb.pro.ui.theme.TierTheme

@Composable
fun StaffScreen(
    onStaffClick: (String) -> Unit,
    onStudioClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: StaffViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - 5
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }

    val isEn = com.creditdb.pro.ui.theme.LanguageManager.isEnglish
    val roles = listOf("all") + RoleConstants.ROLE_ORDER

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(com.creditdb.pro.ui.glass.LocalChromePadding.current)
    ) {
        // 1. スマート1行検索バー
        SmartSearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            onFilterClick = {
                viewModel.setFilterSheetVisible(true)
            },
            hasActiveFilters = uiState.hasActiveFilters,
            placeholder = if (isEn) com.creditdb.pro.ui.theme.AppStrings.searchPlaceholderEn else "スタッフ・声優・制作スタジオ名で検索..."
        )

        // 2. 11部門+スタジオ ロールセレクター (水平スクロール)
        com.creditdb.pro.ui.components.SingleChoiceButtonGroup(
            options = roles,
            selected = uiState.selectedRole,
            onSelect = { viewModel.onRoleSelect(it) },
            label = { com.creditdb.pro.data.RoleDefinitions.getShortDisplayName(it, isEn) },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        // 3. スリムステータスバー (件数 & 条件表示)
        // Tier 指定は検索窓右のフィルタボタンから開くシートで行う（一覧の表示領域を優先）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val n = uiState.totalCount
            val countLabel = when {
                uiState.selectedRole == "studio" -> if (isEn) "$n Studios" else "$n スタジオ"
                uiState.selectedRole == "cv" -> if (isEn) "$n Voice Actors" else "$n 名の声優"
                // 「全スタッフ」は声優を含まない（検索時のみ声優も表示する）
                uiState.selectedRole == "all" && uiState.searchQuery.isBlank() -> if (isEn) "$n Creators" else "$n 名のスタッフ"
                else -> if (isEn) "$n Creators & Cast" else "$n 名のスタッフ・声優"
            }
            com.creditdb.pro.ui.components.CountLabel(text = countLabel, modifier = Modifier.weight(1f))

            run {
                val conditionLabel = buildString {
                    if (uiState.debutEra != DebutEraFilter.ALL) {
                        append(uiState.debutEra.getDisplayName(isEn))
                        append(" · ")
                    }
                    if (uiState.crossTierFilter != "all" && uiState.selectedRole != "studio") {
                        val prefix = if (uiState.sortOption == StaffSortOption.CUMULATIVE) {
                            if (isEn) "S(a):" else "実力:"
                        } else {
                            if (isEn) "ΣZ:" else "累計:"
                        }
                        append("$prefix${uiState.crossTierFilter} · ")
                    }
                    append(uiState.sortOption.getDisplayName(isEn))
                }
                com.creditdb.pro.ui.components.SortLabelPill(
                    text = conditionLabel,
                    onClick = { viewModel.setFilterSheetVisible(true) },
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }

        // 4. リーダーボードリスト
        Box(modifier = Modifier.weight(1f)) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AppLoadingIndicator(contained = true, size = 56.dp)
                }
            } else if (uiState.items.isEmpty()) {
                val emptyText = if (isEn) {
                    if (uiState.selectedRole == "studio") "No matching animation studios found" else "No matching creators or cast found"
                } else {
                    if (uiState.selectedRole == "studio") "該当する制作スタジオが見つかりませんでした" else "該当するスタッフ・声優が見つかりませんでした"
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = emptyText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = uiState.items,
                        key = { _, item -> "${item.role}_${item.name}" }
                    ) { _, staff ->
                        CompactStaffCard(
                            item = staff,
                            sortOption = uiState.sortOption,
                            onClick = {
                                if (staff.role == "studio") {
                                    onStudioClick(staff.name)
                                } else {
                                    onStaffClick(staff.name)
                                }
                            }
                        )
                    }

                    if (uiState.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AppLoadingIndicator(size = 32.dp)
                            }
                        }
                    }
                }
            }
        }
    }

    // フィルタ・ソート用 ModalBottomSheet
    if (uiState.showFilterSheet) {
        StaffFilterBottomSheet(
            onDismiss = { viewModel.setFilterSheetVisible(false) },
            sortOption = uiState.sortOption,
            onSortChange = viewModel::onSortOptionSelect,
            debutEra = uiState.debutEra,
            onDebutEraChange = viewModel::onDebutEraSelect,
            crossTierFilter = uiState.crossTierFilter,
            onCrossTierChange = viewModel::onCrossTierSelect,
            onReset = viewModel::resetFilters
        )
    }
}

/**
 * 制作陣 & 声優 & 制作スタジオのコンパクトリーダーボードカード
 */
@Composable
fun CompactStaffCard(
    item: LeaderboardItem,
    sortOption: StaffSortOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isStudio = item.role == "studio"
    val rank = if (sortOption == StaffSortOption.RATING) item.ratingRank else item.cumulativeRank
    val isEn = com.creditdb.pro.ui.theme.LanguageManager.isEnglish

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左: 順位 & 二連Tier (スタジオの場合はスタジオバッジ)
            if (isStudio) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(62.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = if (isEn) "Studio" else "スタジオ",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(62.dp)
                ) {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    DualTierBadge(
                        ratingTier = item.ratingTier,
                        cumulativeTier = item.cumulativeTier
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 中央: 名前、作品数、代表配役 (CV) または代表作
            val primaryName = if (isStudio) {
                com.creditdb.pro.data.StaffNameResolver.getStudioName(item.name, isEn)
            } else {
                item.getDisplayName(isEn)
            }
            val secondaryName = if (!isStudio && isEn) {
                val en = com.creditdb.pro.data.StaffNameResolver.getStaffName(item.name, true)
                if (en != item.name) item.name else null
            } else null

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = primaryName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = if (isEn) "${item.worksCount} works" else "${item.worksCount} 作品",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!secondaryName.isNullOrBlank()) {
                    Text(
                        text = secondaryName,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                val displayBestWork = item.getDisplayBestWork(isEn)
                if (isStudio && !displayBestWork.isNullOrBlank()) {
                    Text(
                        text = if (isEn) "Notable: $displayBestWork (${item.bestWorkYear ?: ""})" else "代表作: $displayBestWork (${item.bestWorkYear ?: ""})",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (!item.topCharacter.isNullOrBlank()) {
                    val displayTopChar = item.getDisplayTopCharacter(isEn) ?: item.topCharacter
                    Text(
                        text = if (isEn) "Top Role: $displayTopChar" else "代表配役: $displayTopChar",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (!displayBestWork.isNullOrBlank()) {
                    Text(
                        text = if (isEn) "Best Work: $displayBestWork (${item.bestWorkYear ?: ""})" else "最高評価作: $displayBestWork (${item.bestWorkYear ?: ""})",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 右: 指標スコア (スタジオは作品一覧リンクのみ、通常スタッフは実力・累計の両方をTier色で表示)
            if (isStudio) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isEn) "Works" else "作品一覧",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                val isRatingSort = sortOption == StaffSortOption.RATING
                val ratingSpec = TierTheme.forTier(item.ratingTier)
                val cumSpec = TierTheme.forTier(item.cumulativeTier)

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // 実力 S(a)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = if (isEn) (if (isRatingSort) "🎯Rating" else "Rating") else (if (isRatingSort) "🎯実力" else "実力"),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = if (isRatingSort) FontWeight.Bold else FontWeight.Normal,
                            color = if (isRatingSort) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format("%+.3f", item.rating),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isRatingSort) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = ratingSpec.accent
                        )
                    }

                    // 生涯累計 ΣZ
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = if (isEn) (if (!isRatingSort) "🏛️Cumul" else "Cumul") else (if (!isRatingSort) "🏛️累計" else "累計"),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = if (!isRatingSort) FontWeight.Bold else FontWeight.Normal,
                            color = if (!isRatingSort) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format("%+.2f", item.cumulativeZ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (!isRatingSort) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = cumSpec.accent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (isEn) "Details" else "詳細",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
