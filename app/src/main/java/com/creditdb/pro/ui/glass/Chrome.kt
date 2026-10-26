package com.creditdb.pro.ui.glass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 各画面のスクロールコンテナ（LazyColumn / Column 等）が上部フローティングバーと
 * 下部フローティングタブバーに隠れないようにするための余白を提供する CompositionLocal。
 */
val LocalChromePadding = compositionLocalOf { PaddingValues(0.dp) }

/**
 * 下部 GlassTabBar の現在の実測高さ（px）。インライン縮小に連動して変化する。
 */
val LocalFloatingBarsHeightPx = compositionLocalOf { 0 }

/**
 * [LocalChromePadding] に画面固有のパディングを加算した [PaddingValues] を返すヘルパー。
 */
@Composable
fun rememberChromeContentPadding(
    horizontal: Dp = 0.dp,
    start: Dp = horizontal,
    end: Dp = horizontal,
    extraTop: Dp = 0.dp,
    extraBottom: Dp = 0.dp,
): PaddingValues {
    val chrome = LocalChromePadding.current
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = chrome.calculateStartPadding(layoutDirection) + start,
        top = chrome.calculateTopPadding() + extraTop,
        end = chrome.calculateEndPadding(layoutDirection) + end,
        bottom = chrome.calculateBottomPadding() + extraBottom,
    )
}

/**
 * 画面ごとに上部1行フローティングバーの内容を登録・解除するコントローラ。
 * 登録した画面を積み重ねて覚え、最後に登録した画面の内容を表示する。
 * 画面が解除されたら、まだ残っている1つ前の画面の内容に戻す。
 * （1枠だけだと、戻る操作を途中でやめたときなどに、表示中の画面のバーまで消えて
 *   「CreditDB PRO」の代わりの表示になってしまう）
 */
@Stable
class TopBarController {
    private val owners = mutableListOf<Pair<Any, @Composable () -> Unit>>()
    var content by mutableStateOf<(@Composable () -> Unit)?>(null)
        private set

    private val overlayOwners = mutableListOf<Pair<Any, @Composable BoxScope.() -> Unit>>()
    var floatingOverlay by mutableStateOf<(@Composable BoxScope.() -> Unit)?>(null)
        private set

    fun set(owner: Any, composable: @Composable () -> Unit) {
        owners.removeAll { it.first === owner }
        owners.add(owner to composable)
        content = composable
    }

    fun clear(owner: Any) {
        owners.removeAll { it.first === owner }
        content = owners.lastOrNull()?.second
    }

    fun setOverlay(owner: Any, composable: @Composable BoxScope.() -> Unit) {
        overlayOwners.removeAll { it.first === owner }
        overlayOwners.add(owner to composable)
        floatingOverlay = composable
    }

    fun clearOverlay(owner: Any) {
        overlayOwners.removeAll { it.first === owner }
        floatingOverlay = overlayOwners.lastOrNull()?.second
    }
}

val LocalTopBarController = staticCompositionLocalOf { TopBarController() }

/**
 * 右上端の「⋮」オーバーフローメニューボタンを描画するスロット。
 * MainActivity 側で GlassDropdownMenu 付きのボタンを提供する。
 */
val LocalOverflowMenuButton = compositionLocalOf<@Composable () -> Unit> { {} }

/**
 * 各画面から上部フローティングバー（1行）の内容を提供する Composable。
 * 退場時に自動でクリアされるため、遷移中に前画面のボタンが残らない。
 */
@Composable
fun ProvideTopBar(content: @Composable () -> Unit) {
    val controller = LocalTopBarController.current
    val owner = remember { Any() }
    val currentContent by rememberUpdatedState(content)
    DisposableEffect(controller, owner) {
        controller.set(owner) { currentContent() }
        onDispose {
            controller.clear(owner)
        }
    }
}

/**
 * 各画面から appBackdrop の外側（GlassTabBar の直上など）に配置する
 * Liquid Glass フローティングオーバーレイ（Tier ツールバー・FAB・Snackbar 等）を提供する Composable。
 * appBackdrop の記録対象外に描画されるため、RenderNode の再帰参照（SIGSEGV）を防ぎつつ
 * リアルタイム屈折（Modifier.liquidGlass）を安全に適用できる。
 */
@Composable
fun ProvideFloatingOverlay(content: @Composable BoxScope.() -> Unit) {
    val controller = LocalTopBarController.current
    val owner = remember { Any() }
    val currentContent by rememberUpdatedState(content)
    DisposableEffect(controller, owner) {
        controller.setOverlay(owner) { currentContent() }
        onDispose {
            controller.clearOverlay(owner)
        }
    }
}

/**
 * 上部1行フローティングバーの標準行レイアウト（高さ 52.dp、左右パディング 12.dp、ボタン間隔 8.dp）。
 * 内部のガラス要素でフル屈折（LocalTopBarGlassBackdrop = true）を有効化する。
 */
@Composable
fun FloatingTopBarRow(
    modifier: Modifier = Modifier,
    glassBackdrop: Boolean = true,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    content: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topSafeInset())
            .height(TopBarContentHeight)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalTopBarGlassBackdrop provides glassBackdrop) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = horizontalArrangement,
                content = content,
            )
        }
    }
}
