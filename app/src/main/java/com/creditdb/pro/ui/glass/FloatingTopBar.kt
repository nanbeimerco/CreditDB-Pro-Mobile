// Adapted from Tinque (https://github.com/nanbeimerco/Tinque), GPL-3.0
package com.creditdb.pro.ui.glass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The bar's own height, above whatever inset it is sitting under.
 */
val TopBarContentHeight = 52.dp

/**
 * The breathing room between the bar's bottom edge and the first thing under
 * it, so content rests below the glass instead of against it.
 */
val TopBarContentGap = 12.dp

/**
 * How far down the window the bar actually ends: the status bar inset it is
 * pinned under, plus its own height.
 */
@Composable
fun topBarHeight(): Dp = topSafeInset() + TopBarContentHeight

/**
 * How far below the top edge floating controls must start.
 */
@Composable
fun topSafeInset(): Dp {
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val cutout = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    return maxOf(statusBar, cutout, MinTopSafeInset)
}

/** The least room left above floating top controls, even with no status bar or cutout. */
val MinTopSafeInset = 16.dp

/**
 * Where page content should start: clear of the bar, plus [TopBarContentGap].
 */
@Composable
fun topBarContentPadding(): Dp = topBarHeight() + TopBarContentGap

/** Set by [FloatingTopBar] for the buttons inside it — see its `glassBackdrop`. */
val LocalTopBarGlassBackdrop = staticCompositionLocalOf { false }

/** Diameter of the round glass behind each top-bar button. */
val TopBarGlassButtonSize = 42.dp

/** The glass behind floating top controls, full refraction when the page is being recorded. */
@Composable
fun topBarGlass(shape: CornerBasedShape): Modifier {
    val glassEnabled = LocalLiquidGlassEnabled.current && isGlassSupported()
    return if (LocalTopBarGlassBackdrop.current && glassEnabled) {
        Modifier.liquidGlass(shape = shape)
    } else {
        Modifier.lightweightLiquidGlass(
            shape = shape,
            fallbackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    }
}

/**
 * The page name in a small floating glass capsule.
 */
@Composable
fun TopBarTitleCapsule(title: String, modifier: Modifier = Modifier) {
    val glassEnabled = LocalLiquidGlassEnabled.current && isGlassSupported()
    val capsule = RoundedCornerShape(percent = 50)
    Box(
        modifier = modifier
            .height(TopBarGlassButtonSize)
            .then(topBarGlass(capsule))
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = if (glassEnabled) glassContentColor() else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * A top-bar button on its own round Liquid Glass — the same material as the
 * floating bottom bar.
 */
@Composable
fun TopBarGlassButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val glassEnabled = LocalLiquidGlassEnabled.current && isGlassSupported()
    val tint = if (glassEnabled) glassContentColor() else MaterialTheme.colorScheme.onSurface
    val glass = topBarGlass(CircleShape)
    Box(
        modifier = modifier
            .size(TopBarGlassButtonSize)
            .then(glass)
            .clip(CircleShape)
            .clickable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * Floating top bar row with no band of its own.
 */
@Composable
fun FloatingTopBar(
    title: String = "",
    scrolled: Boolean = false,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    alwaysShowTitle: Boolean = false,
    glassBackdrop: Boolean = false,
    backIcon: ImageVector = Icons.AutoMirrored.Rounded.ArrowBack,
    backDescription: String? = "Back",
    actions: @Composable () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topSafeInset())
                .height(TopBarContentHeight),
        ) {
            CompositionLocalProvider(LocalTopBarGlassBackdrop provides glassBackdrop) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = title.isNotEmpty() && (alwaysShowTitle || scrolled),
                    enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.92f),
                    exit = fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.92f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 64.dp),
                ) {
                    TopBarTitleCapsule(title)
                }
                if (onBack != null) {
                    TopBarGlassButton(
                        onClick = onBack,
                        icon = backIcon,
                        contentDescription = backDescription,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 12.dp),
                    )
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    actions()
                }
            }
        }
    }
}
