package com.creditdb.pro.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon

/**
 * Material 3 Expressive の共通部品。
 * 画面ごとに個別実装せず、ここを通して使うことで見た目と動きを揃える。
 */

/** Tier ごとの形。上位ほど「とげ」が多く華やかな形にして、色に頼らず区別できるようにする */
fun tierPolygon(tier: String?): RoundedPolygon = when (tier?.trim()?.uppercase()) {
    "S+" -> MaterialShapes.SoftBurst
    "S" -> MaterialShapes.Cookie9Sided
    "A+" -> MaterialShapes.Cookie7Sided
    "A" -> MaterialShapes.Cookie4Sided
    "B+" -> MaterialShapes.Pentagon
    "B" -> MaterialShapes.Square
    "C" -> MaterialShapes.Pill
    "D" -> MaterialShapes.Circle
    else -> MaterialShapes.Circle
}

@Composable
fun tierShape(tier: String?): Shape = tierPolygon(tier).toShape()

/**
 * 単一選択のボタングループ（M3 Expressive の connected button group）。
 * 選択肢が多い場合は横スクロールする。FilterChip の横並びを置き換える。
 */
@Composable
fun <T> SingleChoiceButtonGroup(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    compact: Boolean = false
) {
    val rowModifier = if (scrollable) modifier.horizontalScroll(rememberScrollState()) else modifier.fillMaxWidth()
    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = option == selected
            ToggleButton(
                checked = isSelected,
                onCheckedChange = { onSelect(option) },
                modifier = (if (scrollable) Modifier else Modifier.weight(1f))
                    .then(if (compact) Modifier.height(34.dp) else Modifier)
                    .semantics { role = Role.RadioButton },
                contentPadding = if (compact) PaddingValues(horizontal = 12.dp) else ToggleButtonDefaults.contentPaddingFor(ToggleButtonDefaults.MinHeight),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = label(option),
                    style = when {
                        compact && isSelected -> MaterialTheme.typography.labelMediumEmphasized
                        compact -> MaterialTheme.typography.labelMedium
                        isSelected -> MaterialTheme.typography.labelLargeEmphasized
                        else -> MaterialTheme.typography.labelLarge
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 並び順・絞り込み条件の表示ラベル。塗り（secondaryContainer）＋その上の文字色の組を使うので、
 * どのカラープリセット・ライト/ダークでも読みやすい。タップでフィルタシートを開く。
 */
@Composable
fun SortLabelPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMediumEmphasized,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 長い一覧（参加作品・制作作品など）の1行。LazyColumn の1項目として使い、表示される行だけを描画する。
 * 見た目はひとまとまりのグループ（先頭と末尾だけ大きな角丸、行の間は小さな角丸と2dpの隙間）。
 */
@Composable
fun GroupedListItem(
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val large = 20.dp
    val small = 4.dp
    val isFirst = index == 0
    val isLast = index == count - 1
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(
        topStart = if (isFirst) large else small,
        topEnd = if (isFirst) large else small,
        bottomStart = if (isLast) large else small,
        bottomEnd = if (isLast) large else small
    )
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = if (isLast) 0.dp else 2.dp)
    ) {
        content()
    }
}

/** 一覧の件数表示（例: 21728 作品） */
@Composable
fun CountLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1
    )
}

/** 画面全体・セクションの読み込み中表示（形が変化する LoadingIndicator） */
@Composable
fun AppLoadingIndicator(
    modifier: Modifier = Modifier,
    contained: Boolean = false,
    size: Dp = 48.dp
) {
    if (contained) {
        ContainedLoadingIndicator(modifier = modifier.size(size))
    } else {
        LoadingIndicator(modifier = modifier.size(size))
    }
}

/** 中央寄せの読み込み中表示（従来の Box + CircularProgressIndicator の置き換え） */
@Composable
fun CenteredLoading(modifier: Modifier = Modifier, message: String? = null) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppLoadingIndicator(contained = true, size = 56.dp)
            if (message != null) {
                Text(
                    text = message,
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
