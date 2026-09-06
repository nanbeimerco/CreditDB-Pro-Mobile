package com.creditdb.pro.ui.staff

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.SwapVert
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
import com.creditdb.pro.data.LeaderboardItem
import com.creditdb.pro.data.RoleConstants
import com.creditdb.pro.data.StaffSortOption
import com.creditdb.pro.ui.components.DualTierBadge
import com.creditdb.pro.ui.components.RoleBadge
import com.creditdb.pro.ui.components.SmartSearchBar
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

    val roles = listOf("all") + RoleConstants.ROLE_ORDER

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. スマート1行検索バー
        SmartSearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            onFilterClick = {
                val nextSort = if (uiState.sortOption == StaffSortOption.RATING) StaffSortOption.CUMULATIVE else StaffSortOption.RATING
                viewModel.onSortOptionSelect(nextSort)
            },
            hasActiveFilters = uiState.sortOption == StaffSortOption.CUMULATIVE,
            placeholder = "スタッフ・声優・制作スタジオ名で検索..."
        )

        // 2. 10部門+スタジオ ロールセレクター (水平スクロール)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            roles.forEach { rKey ->
                val isSelected = uiState.selectedRole == rKey
                val displayName = RoleConstants.getDisplayName(rKey)
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.onRoleSelect(rKey) },
                    label = {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }
        }

        // 3. スリムステータスバー (件数 & ソート切り替え)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val countLabel = if (uiState.selectedRole == "studio") {
                "${uiState.totalCount} スタジオ"
            } else {
                "${uiState.totalCount} 名のスタッフ・声優"
            }
            Text(
                text = countLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (uiState.selectedRole != "studio") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        val nextSort = if (uiState.sortOption == StaffSortOption.RATING) StaffSortOption.CUMULATIVE else StaffSortOption.RATING
                        viewModel.onSortOptionSelect(nextSort)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = uiState.sortOption.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 4. リーダーボードリスト
        Box(modifier = Modifier.weight(1f)) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (uiState.items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (uiState.selectedRole == "studio") "該当する制作スタジオが見つかりませんでした" else "該当するスタッフ・声優が見つかりませんでした",
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
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
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
                            text = "スタジオ",
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
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = "${item.worksCount} 作品",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                if (isStudio && !item.bestWorkTitle.isNullOrBlank()) {
                    Text(
                        text = "代表作: ${item.bestWorkTitle} (${item.bestWorkYear ?: ""})",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (!item.topCharacter.isNullOrBlank()) {
                    Text(
                        text = "代表配役: ${item.topCharacter}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (!item.bestWorkTitle.isNullOrBlank()) {
                    Text(
                        text = "最高評価作: ${item.bestWorkTitle} (${item.bestWorkYear ?: ""})",
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
                        text = "作品一覧",
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
                            text = if (isRatingSort) "🎯実力" else "実力",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = if (isRatingSort) FontWeight.Bold else FontWeight.Normal,
                            color = if (isRatingSort) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format("%+.3f", item.rating),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isRatingSort) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = ratingSpec.onContainerColor
                        )
                    }

                    // 生涯累計 ΣZ
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = if (!isRatingSort) "🏛️累計" else "累計",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = if (!isRatingSort) FontWeight.Bold else FontWeight.Normal,
                            color = if (!isRatingSort) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format("%+.2f", item.cumulativeZ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (!isRatingSort) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = cumSpec.onContainerColor
                        )
                    }
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
