package com.creditdb.pro.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditdb.pro.data.RoleConstants
import com.creditdb.pro.data.RoleDefinitions
import com.creditdb.pro.data.WorksSortOption
import com.creditdb.pro.ui.theme.*

/**
 * 単一 Tier バッジ (例: "Tier S+")
 * DualTierBadge と同様に、ダークグレー下地 (surfaceVariant) + グレー枠線 (outlineVariant) に文字のみTier色で統一
 */
@Composable
fun TierBadge(
    tier: String,
    modifier: Modifier = Modifier,
    prefix: String = "Tier "
) {
    val spec = TierTheme.forTier(tier)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$prefix$tier",
            color = spec.onContainerColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 大サイズ Tier バッジ (アルファベット単体表示 例: "S+", "A")
 */
@Composable
fun LargeTierBadge(
    tier: String,
    modifier: Modifier = Modifier
) {
    val spec = TierTheme.forTier(tier)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
            .border(1.2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = tier,
            color = spec.onContainerColor,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/**
 * 二連 Tier バッジ (例: "[ S+ / S ]")
 */
@Composable
fun DualTierBadge(
    ratingTier: String,
    cumulativeTier: String,
    modifier: Modifier = Modifier
) {
    val rSpec = TierTheme.forTier(ratingTier)
    val cSpec = TierTheme.forTier(cumulativeTier)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = ratingTier,
            color = rSpec.onContainerColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "/",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Normal
        )
        Text(
            text = cumulativeTier,
            color = cSpec.onContainerColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/**
 * 総合判定バッジ (サプライズ名作 / 期待外れ / 概ねスタッフ前評判通り)
 */
@Composable
fun VerdictBadge(
    verdict: String,
    modifier: Modifier = Modifier
) {
    val isSurprise = verdict.contains("サプライズ")
    val isUnder = verdict.contains("期待外れ") || verdict.contains("ポテンシャル未達")

    val bg = when {
        isSurprise -> VerdictSurprise.copy(alpha = 0.15f)
        isUnder -> VerdictUnderperform.copy(alpha = 0.15f)
        else -> VerdictNormal.copy(alpha = 0.12f)
    }
    val fg = when {
        isSurprise -> VerdictSurprise
        isUnder -> VerdictUnderperform
        else -> VerdictNormal
    }
    val border = when {
        isSurprise -> VerdictSurprise.copy(alpha = 0.4f)
        isUnder -> VerdictUnderperform.copy(alpha = 0.4f)
        else -> VerdictNormal.copy(alpha = 0.3f)
    }

    val displayLabel = AppStrings.verdictCompact(verdict, LanguageManager.isEnglish)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .border(0.8.dp, border, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayLabel,
            color = fg,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 役職タグバッジ (Material 3 準拠シックバッジ)
 */
@Composable
fun RoleBadge(
    roleKey: String,
    modifier: Modifier = Modifier
) {
    val displayName = RoleDefinitions.getShortDisplayName(roleKey, LanguageManager.isEnglish)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .border(0.8.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayName,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 主要指標表示カード (偏差値、AniList素点、Tier等)
 */
@Composable
fun MetricCard(
    title: String,
    value: String,
    subValue: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.defaultMinSize(minHeight = 80.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subValue != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subValue,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 1行統合スマート検索バー
 * 検索窓とフィルタボタンを1行に集約し、画面縦領域を最大化
 */
@Composable
fun SmartSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    hasActiveFilters: Boolean = false,
    placeholder: String = if (LanguageManager.isEnglish) AppStrings.searchPlaceholderEn else AppStrings.searchPlaceholder,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(46.dp),
            shape = RoundedCornerShape(23.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = if (LanguageManager.isEnglish) "Search" else "検索",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = if (LanguageManager.isEnglish) "Clear" else "クリア",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // フィルタ展開ボタン
        BadgedBox(
            badge = {
                if (hasActiveFilters) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
                    )
                }
            }
        ) {
            FilledTonalIconButton(
                onClick = onFilterClick,
                modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (hasActiveFilters) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = if (hasActiveFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = if (LanguageManager.isEnglish) "Filters" else "条件絞込",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 作品フィルタ & ソート ModalBottomSheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkFilterBottomSheet(
    onDismiss: () -> Unit,
    sortOption: WorksSortOption,
    onSortChange: (WorksSortOption) -> Unit,
    verdictFilter: String,
    onVerdictChange: (String) -> Unit,
    tierFilter: String,
    onTierChange: (String) -> Unit,
    eraFilter: String,
    onEraChange: (String) -> Unit,
    onReset: () -> Unit,
    isCompareMode: Boolean = false
) {
    val isEn = LanguageManager.isEnglish
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEn) {
                        if (isCompareMode) "Residual & Verdict Filter" else "Filter & Sort Works"
                    } else {
                        if (isCompareMode) "対比・判定フィルタ" else "作品絞込 & 並び替え"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onReset) {
                    Text(if (isEn) "Reset" else "リセット", color = MaterialTheme.colorScheme.primary)
                }
            }

            // 1. ソート順
            Text(
                text = if (isEn) "Sort Criteria" else "並び替え基準",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            val availableSorts = if (isCompareMode) {
                listOf(
                    WorksSortOption.RESIDUAL_DESC,
                    WorksSortOption.RESIDUAL_ASC,
                    WorksSortOption.DEVIATION_DESC,
                    WorksSortOption.PRED_DESC,
                    WorksSortOption.RAW_DESC
                )
            } else {
                listOf(
                    WorksSortOption.DEVIATION_DESC,
                    WorksSortOption.DEVIATION_ASC,
                    WorksSortOption.PRED_DESC,
                    WorksSortOption.RAW_DESC,
                    WorksSortOption.YEAR_DESC,
                    WorksSortOption.TITLE_ASC
                )
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableSorts.forEach { opt ->
                    FilterChip(
                        selected = sortOption == opt,
                        onClick = { onSortChange(opt) },
                        label = { Text(opt.getDisplayName(isEn)) }
                    )
                }
            }

            // 2. 総合判定 (Compare / Pro の独自機能)
            Text(
                text = if (isEn) "Performance Verdict" else "総合パフォーマンス判定",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            val verdicts = if (isEn) {
                listOf(
                    "all" to "All",
                    "サプライズ名作" to "★ Exceeded Expectations",
                    "期待外れ" to "▼ Below Potential",
                    "概ねスタッフ前評判通り" to "● As Expected"
                )
            } else {
                listOf(
                    "all" to "すべて",
                    "サプライズ名作" to "★ サプライズ名作 (期待以上)",
                    "期待外れ" to "▼ 期待外れ (未達)",
                    "概ねスタッフ前評判通り" to "● 前評判通り"
                )
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                verdicts.forEach { (key, label) ->
                    FilterChip(
                        selected = verdictFilter == key,
                        onClick = { onVerdictChange(key) },
                        label = { Text(label) }
                    )
                }
            }

            // 3. Tier
            Text(
                text = if (isEn) "Tier (Deviation Range)" else "Tier (偏差値区分)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            val tiers = listOf("all", "S+", "S", "A+", "A", "B+", "B", "C", "D")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tiers.forEach { t ->
                    FilterChip(
                        selected = tierFilter == t,
                        onClick = { onTierChange(t) },
                        label = { Text(if (t == "all") (if (isEn) "All Tiers" else "全Tier") else t) }
                    )
                }
            }

            // 4. 年代
            Text(
                text = if (isEn) "Release Era" else "公開年代",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            val eras = if (isEn) {
                listOf(
                    "all" to "All Eras",
                    "2020s" to "2020s",
                    "2010s" to "2010s",
                    "2000s" to "2000s",
                    "1990s" to "1990s",
                    "1980s" to "1980s & Earlier"
                )
            } else {
                listOf(
                    "all" to "全年代",
                    "2020s" to "2020年代",
                    "2010s" to "2010年代",
                    "2000s" to "2000年代",
                    "1990s" to "1990年代",
                    "1980s" to "1980年代以前"
                )
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                eras.forEach { (key, label) ->
                    FilterChip(
                        selected = eraFilter == key,
                        onClick = { onEraChange(key) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isEn) "Apply & Close" else "適用して閉じる", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * 手動データベース更新モーダルダイアログ
 */
@Composable
fun DatabaseUpdateDialog(
    isUpdating: Boolean,
    percent: Int,
    statusText: String,
    onDismiss: () -> Unit
) {
    val isEn = LanguageManager.isEnglish
    AlertDialog(
        onDismissRequest = {
            if (!isUpdating) onDismiss()
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (percent == 100) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = if (isEn) "Complete" else "完了",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                Text(
                    text = if (percent == 100) {
                        if (isEn) "Database Update Complete" else "データベース更新完了"
                    } else {
                        if (isEn) "Updating Database..." else "データベース更新中..."
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LinearProgressIndicator(
                    progress = { percent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            if (!isUpdating) {
                Button(onClick = onDismiss) {
                    Text(if (isEn) "Close" else "閉じる")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}
