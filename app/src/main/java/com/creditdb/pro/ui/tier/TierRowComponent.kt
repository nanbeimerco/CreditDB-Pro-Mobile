package com.creditdb.pro.ui.tier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditdb.pro.data.tier.TierAnimeItem
import com.creditdb.pro.data.tier.TierRowData
import com.creditdb.pro.ui.theme.LanguageManager

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TierRowComponent(
    row: TierRowData,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {},
    isDraggingAnimeId: String? = null,
    onAnimeClick: (TierAnimeItem) -> Unit,
    onAnimeLongClick: (TierAnimeItem, TierRowData) -> Unit,
    onAddAnimeClick: (TierRowData) -> Unit,
    onEditRowClick: (TierRowData) -> Unit,
    onAnimeDragStart: (TierAnimeItem) -> Unit = {},
    onAnimeDragEnd: () -> Unit = {},
    onAnimeDrag: (TierAnimeItem, Float, Float) -> Unit = { _, _, _ -> }
) {
    val tierColor = remember(row.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(row.colorHex))
        } catch (e: Exception) {
            Color(0xFFEAC765)
        }
    }

    // Determine contrast text color for tier header
    val headerTextColor = remember(tierColor) {
        val luminance = (0.299 * tierColor.red + 0.587 * tierColor.green + 0.114 * tierColor.blue)
        if (luminance > 0.55) Color(0xFF1E1E24) else Color.White
    }

    val lazyListState = rememberSaveable(row.id, saver = LazyListState.Saver) {
        LazyListState()
    }

    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isExpanded) Modifier.height(IntrinsicSize.Min) else Modifier.height(134.dp)),
            verticalAlignment = if (isExpanded) Alignment.Top else Alignment.CenterVertically
        ) {
            // Left Tier Label Header
            Box(
                modifier = Modifier
                    .width(74.dp)
                    .fillMaxHeight()
                    .background(tierColor)
                    .clickable { onEditRowClick(row) }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = row.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = if (row.name.length > 2) 16.sp else 22.sp,
                            color = headerTextColor
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = headerTextColor.copy(alpha = 0.2f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "${row.items.size}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = headerTextColor,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // 引き出し（展開／折りたたみ）ボタン
                    if (row.items.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = headerTextColor.copy(alpha = 0.25f),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable { onToggleExpand() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    tint = headerTextColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Right Items Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(if (isExpanded) Modifier.wrapContentHeight() else Modifier.fillMaxHeight())
                    .padding(8.dp)
            ) {
                if (row.items.isEmpty()) {
                    // Empty row placeholder
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(118.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onAddAnimeClick(row) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Anime",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (LanguageManager.isEnglish) "Tap to add anime" else "タップして作品を追加",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                } else if (isExpanded) {
                    // 展開（引き出し）表示: 全作品をグリッド状にマルチライン展開
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.items.forEach { anime ->
                            TierAnimeCard(
                                anime = anime,
                                isDragging = isDraggingAnimeId == anime.id,
                                onClick = { onAnimeClick(anime) },
                                onLongClick = { onAnimeLongClick(anime, row) }
                            )
                        }

                        // Add More Button at the end of grid
                        Surface(
                            modifier = Modifier
                                .width(44.dp)
                                .height(118.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onAddAnimeClick(row) },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add to ${row.name}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    // 通常時: 横スクロール (LazyRow) のまま
                    LazyRow(
                        state = lazyListState,
                        modifier = Modifier.fillMaxHeight(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        itemsIndexed(
                            items = row.items,
                            key = { _, anime -> anime.id }
                        ) { _, anime ->
                            TierAnimeCard(
                                anime = anime,
                                isDragging = isDraggingAnimeId == anime.id,
                                onClick = { onAnimeClick(anime) },
                                onLongClick = { onAnimeLongClick(anime, row) }
                            )
                        }

                        // Add More Button at the end of row
                        item(key = "add_btn_${row.id}") {
                            Surface(
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(118.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onAddAnimeClick(row) },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add to ${row.name}",
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
}
