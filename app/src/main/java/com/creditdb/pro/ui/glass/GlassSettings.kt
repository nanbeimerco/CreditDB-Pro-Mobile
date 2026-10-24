// Adapted from Tinque (https://github.com/nanbeimerco/Tinque), GPL-3.0
package com.creditdb.pro.ui.glass

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow

enum class PillMotionStyle {
    FAST,
    FLUID,
    RELAXED;

    companion object {
        val DEFAULT = FLUID
    }
}

/**
 * Liquid Glass 表示設定の永続化管理
 */
object GlassSettings {
    private const val PREFS_NAME = "creditdb_app_settings"
    private const val KEY_LIQUID_GLASS = "glass_liquid_glass"
    private const val KEY_REDUCE_DYNAMIC_BLUR = "glass_reduce_dynamic_blur"

    val liquidGlass = MutableStateFlow(true)
    val reduceDynamicBlur = MutableStateFlow(false)
    val liquidGlassBlurDp = MutableStateFlow(8f)
    val liquidGlassOpacity = MutableStateFlow(0.40f)
    val glassRenderStyle = MutableStateFlow(GlassRenderStyle.FROSTED)
    val glassClarity = MutableStateFlow(0.75f)
    val glassShade = MutableStateFlow(0.5f)
    val pillMotionStyle = MutableStateFlow(PillMotionStyle.FLUID)
    val reduceAnimation = MutableStateFlow(false)
    val hideNavigationBarLabels = MutableStateFlow(false)
    val lockBottomBarExpanded = MutableStateFlow(false)

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            liquidGlass.value = prefs?.getBoolean(KEY_LIQUID_GLASS, true) ?: true
            reduceDynamicBlur.value = prefs?.getBoolean(KEY_REDUCE_DYNAMIC_BLUR, false) ?: false
        }
    }

    fun setLiquidGlass(enabled: Boolean) {
        liquidGlass.value = enabled
        prefs?.edit()?.putBoolean(KEY_LIQUID_GLASS, enabled)?.apply()
    }

    fun setReduceDynamicBlur(enabled: Boolean) {
        reduceDynamicBlur.value = enabled
        prefs?.edit()?.putBoolean(KEY_REDUCE_DYNAMIC_BLUR, enabled)?.apply()
    }
}
