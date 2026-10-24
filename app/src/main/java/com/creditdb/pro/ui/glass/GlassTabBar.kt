// Adapted from Tinque (https://github.com/nanbeimerco/Tinque), GPL-3.0
@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.creditdb.pro.ui.glass

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.creditdb.pro.ui.glass.floatingtabbar.FloatingTabBar
import com.creditdb.pro.ui.glass.floatingtabbar.FloatingTabBarDefaults
import com.creditdb.pro.ui.glass.floatingtabbar.FloatingTabBarScrollConnection
import com.creditdb.pro.ui.glass.haptics.Haptic
import com.creditdb.pro.ui.glass.haptics.rememberHaptics

/**
 * Liquid Glass 下部フローティングタブバー（再生中アクセサリー・standaloneTab なし、全タブ通常タブ）。
 */
@Composable
fun GlassTabBar(
    tabs: List<BottomTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    scrollConnection: FloatingTabBarScrollConnection,
    useGlass: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val pillShape = remember { RoundedCornerShape(percent = 50) }
    val isDark = MaterialTheme.colorScheme.surface.luminance() <= 0.5f || isSystemInDarkTheme()
    val borderColor = if (isDark) GLASS_EDGE_COLOR else Color.Black.copy(alpha = 0.08f)
    val hideNavigationBarLabels by GlassSettings.hideNavigationBarLabels.collectAsStateWithLifecycle()
    val liquidGlassOpacity by GlassSettings.liquidGlassOpacity.collectAsStateWithLifecycle()

    val contentColor = if (useGlass) {
        glassContentColor()
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val selectedColor = if (useGlass) {
        contentColor
    } else {
        MaterialTheme.colorScheme.primary
    }
    val unselectedColor = if (useGlass) {
        contentColor.copy(alpha = 0.70f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val milkyPillAlpha = (0.14f + liquidGlassOpacity * 0.25f).coerceIn(0.15f, 0.40f)
    val renderStyle by GlassSettings.glassRenderStyle.collectAsStateWithLifecycle()
    val indicatorColor = when {
        !useGlass -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        renderStyle == GlassRenderStyle.CLEAR ->
            if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
        isDark -> Color.White.copy(alpha = milkyPillAlpha)
        else -> Color.Black.copy(alpha = 0.12f)
    }
    val haptics = rememberHaptics()
    val currentOnTabSelected by rememberUpdatedState(onTabSelected)

    val surfaceModifier: @Composable () -> Modifier = remember(useGlass, pillShape, borderColor) {
        if (useGlass) {
            { Modifier.liquidGlass(shape = pillShape, role = GlassRole.TabBar) }
        } else {
            {
                Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f), pillShape)
                    .border(GLASS_EDGE_WIDTH, borderColor, pillShape)
            }
        }
    }

    FloatingTabBar(
        selectedTabKey = selectedIndex,
        scrollConnection = scrollConnection,
        modifier = modifier
            .windowInsetsPadding(floatingBarInsets)
            .padding(horizontal = PAGE_GUTTER)
            .padding(bottom = 2.dp)
            .fillMaxWidth(),
        tabBarContentModifier = surfaceModifier,
        colors = FloatingTabBarDefaults.colors(
            backgroundColor = Color.Transparent,
            accessoryBackgroundColor = Color.Transparent,
            indicatorColor = indicatorColor,
            selectedContentColor = selectedColor,
            artworkColor = MaterialTheme.colorScheme.primary,
            unselectedContentColor = unselectedColor,
            isArtworkTheme = false,
        ),
        elevations = FloatingTabBarDefaults.elevations(
            inlineElevation = 0.dp,
            expandedElevation = 0.dp,
        ),
        sizes = FloatingTabBarDefaults.sizes(
            tabBarContentPadding = PaddingValues(PILL_INSET),
            tabExpandedContentPadding = PaddingValues(vertical = TAB_VERTICAL_PADDING),
        ),
        contentKey = remember(selectedIndex, tabs, contentColor, selectedColor, unselectedColor, hideNavigationBarLabels) {
            listOf(selectedIndex, tabs, contentColor, selectedColor, unselectedColor, hideNavigationBarLabels)
        },
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = index == selectedIndex
            val defaultTint = if (isSelected) selectedColor else unselectedColor
            val onClick = {
                if (!isSelected) haptics.play(Haptic.Select)
                currentOnTabSelected(index)
            }
            tab(
                key = index,
                icon = {
                    val currentTint = LocalContentColor.current
                    val tint = if (isSelected) selectedColor else if (currentTint.alpha > 0f) currentTint else defaultTint
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = tint,
                        modifier = Modifier.size(25.dp),
                    )
                },
                title = {
                    if (!hideNavigationBarLabels) {
                        val currentTint = LocalContentColor.current
                        val tint = if (isSelected) selectedColor else if (currentTint.alpha > 0f) currentTint else defaultTint
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = tint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = TAB_ICON_LABEL_GAP),
                        )
                    }
                },
                onClick = onClick,
                indication = null,
            )
        }
    }
}
