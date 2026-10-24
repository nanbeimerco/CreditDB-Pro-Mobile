// Adapted from Tinque (https://github.com/nanbeimerco/Tinque), GPL-3.0
package com.creditdb.pro.ui.glass

import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class BottomTab(
    val label: String,
    val icon: ImageVector,
)

/** Outer horizontal inset for floating bottom bars (mini player, nav pill). */
val PAGE_GUTTER = 10.dp

/**
 * Widest the floating bottom bars (nav pill and mini player) may grow.
 *
 * A phone is narrower than this, so on a phone `widthIn(max = ...)` before
 * `fillMaxWidth` does nothing and the bars run to [PAGE_GUTTER] as before. On a
 * tablet or in landscape they stop here and sit centred instead of stretching
 * four or five tabs across two feet of glass.
 */
val FLOATING_BAR_MAX_WIDTH = 440.dp

/** A gesture bar's inset — 45px at the test phone's 3x density. */
private val GESTURE_BAR_FLOOR = 15.dp

/**
 * At least a gesture bar's height below the floating pill, even on devices
 * that report a 0dp navigation bar inset (hidden gesture hint, custom ROMs,
 * or immersive edge-to-edge configurations).
 */
internal val floatingBarInsets: WindowInsets
    @Composable get() = WindowInsets.navigationBars.union(WindowInsets(bottom = GESTURE_BAR_FLOOR))

/**
 * The gap between the pill's glass edge and the tabs inside it.
 */
internal val PILL_INSET = 6.dp

/**
 * Each tab's own vertical padding, and the counterweight to [PILL_INSET].
 */
internal val TAB_VERTICAL_PADDING = 9.dp

/** The gap between a tab's glyph and its label, in both bars. */
internal val TAB_ICON_LABEL_GAP = 2.dp

/**
 * The spring the selection indicator and the tab glyphs both travel on.
 */
internal val GlassSpring = spring<Float>(dampingRatio = 0.72f, stiffness = 320f)
