package com.creditdb.pro.ui.works

import androidx.compose.foundation.background
import com.creditdb.pro.ui.theme.badgeFill
import com.creditdb.pro.ui.theme.accent
import com.creditdb.pro.ui.components.AppLoadingIndicator
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import com.creditdb.pro.data.WorkItem
import com.creditdb.pro.ui.components.SmartSearchBar
import com.creditdb.pro.ui.components.TierBadge
import com.creditdb.pro.ui.components.VerdictBadge
import com.creditdb.pro.ui.components.WorkFilterBottomSheet
import com.creditdb.pro.ui.theme.TierTheme

@Composable
fun WorksScreen(
    onWorkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorksViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // 無限スクロール検知
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

    // 上部1行フローティング検索バー（検索カプセル + フィルタ丸ボタン + ⋮メニュー）
    com.creditdb.pro.ui.glass.ProvideTopBar {
        SmartSearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            onFilterClick = { viewModel.setFilterSheetVisible(true) },
            hasActiveFilters = uiState.hasActiveFilters,
            placeholder = if (isEn) com.creditdb.pro.ui.theme.AppStrings.searchPlaceholderEn else com.creditdb.pro.ui.theme.AppStrings.searchPlaceholder,
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.isLoading) {
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
                contentPadding = com.creditdb.pro.ui.glass.rememberChromeContentPadding(
                    horizontal = 12.dp,
                    extraTop = 2.dp,
                    extraBottom = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 先頭アイテム: 件数 & ソート条件ピル（スクロールで自然に流れる）
                item(key = "works_status_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        com.creditdb.pro.ui.components.CountLabel(
                            text = if (isEn) "${uiState.totalCount} works" else "${uiState.totalCount} 件の作品",
                            modifier = Modifier.weight(1f)
                        )
                        com.creditdb.pro.ui.components.SortLabelPill(
                            text = uiState.sortOption.getDisplayName(isEn),
                            onClick = { viewModel.setFilterSheetVisible(true) },
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }

                if (uiState.works.isEmpty()) {
                    item(key = "works_empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isEn) "No matching works found" else "該当する作品が見つかりませんでした",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = uiState.works,
                        key = { _, item -> item.id }
                    ) { _, work ->
                        CompactWorkCard(
                            work = work,
                            onClick = { onWorkClick(work.id) }
                        )
                    }

                    if (uiState.isLoadingMore) {
                        item(key = "works_loading_more") {
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
        WorkFilterBottomSheet(
            onDismiss = { viewModel.setFilterSheetVisible(false) },
            sortOption = uiState.sortOption,
            onSortChange = viewModel::onSortOptionSelect,
            verdictFilter = uiState.selectedVerdict,
            onVerdictChange = viewModel::onVerdictSelect,
            tierFilter = uiState.selectedTier,
            onTierChange = viewModel::onTierSelect,
            eraFilter = uiState.selectedEra,
            onEraChange = viewModel::onEraSelect,
            onReset = viewModel::resetFilters,
            isCompareMode = false
        )
    }
}

/**
 * 画面領域を最大化する洗練されたスリム作品カード
 */
@Composable
fun CompactWorkCard(
    work: WorkItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tierSpec = TierTheme.forTier(work.tier)

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
            // 左端: 偏差値 & 順位 & Tier
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(58.dp)
            ) {
                // Tier ごとの形状バッジ（M3 Expressive）
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(com.creditdb.pro.ui.components.tierShape(work.tier))
                        .background(tierSpec.badgeFill),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = work.tier,
                        style = MaterialTheme.typography.labelLargeEmphasized,
                        color = com.creditdb.pro.ui.theme.TierBadgeTextColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format("%.1f", work.deviationScore),
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = tierSpec.accent
                )
                Text(
                    text = "#${work.deviationRank}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 中央: タイトル、年、主要スタッフ・声優
            val isEn = com.creditdb.pro.ui.theme.LanguageManager.isEnglish
            val (primaryTitle, secondaryTitle) = work.getDisplayTitle(isEn)

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = primaryTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (isEn) 2 else 1,
                        lineHeight = 17.sp,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = "${work.year}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!secondaryTitle.isNullOrBlank()) {
                    Text(
                        text = secondaryTitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = work.getMainStaffSummary(isEn),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 右端: AniList素点
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "AniList",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("%.0f", work.anilistRawScore),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "詳細",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
