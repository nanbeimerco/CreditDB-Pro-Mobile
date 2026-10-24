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
    private const val KEY_GLASS_RENDER_STYLE = "glass_render_style"
    private const val KEY_GLASS_CLARITY = "glass_clarity"
    private const val KEY_GLASS_SHADE = "glass_shade"
    private const val KEY_LIQUID_GLASS_BLUR_DP = "glass_blur_dp"
    private const val KEY_LIQUID_GLASS_OPACITY = "glass_opacity"

    const val DEFAULT_LIQUID_GLASS_BLUR_DP = 8f
    const val MIN_LIQUID_GLASS_BLUR_DP = 0f
    const val MAX_LIQUID_GLASS_BLUR_DP = 24f
    const val DEFAULT_LIQUID_GLASS_OPACITY = 0.40f
    const val MIN_LIQUID_GLASS_OPACITY = 0.10f
    const val MAX_LIQUID_GLASS_OPACITY = 0.85f

    val liquidGlass = MutableStateFlow(true)
    val reduceDynamicBlur = MutableStateFlow(false)
    val liquidGlassBlurDp = MutableStateFlow(DEFAULT_LIQUID_GLASS_BLUR_DP)
    val liquidGlassOpacity = MutableStateFlow(DEFAULT_LIQUID_GLASS_OPACITY)
    val glassRenderStyle = MutableStateFlow(GlassRenderStyle.FROSTED)
    val glassClarity = MutableStateFlow(DEFAULT_GLASS_CLARITY)
    val glassShade = MutableStateFlow(DEFAULT_GLASS_SHADE)
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
            glassRenderStyle.value = GlassRenderStyle.fromKey(prefs?.getString(KEY_GLASS_RENDER_STYLE, null))
            glassClarity.value = (prefs?.getFloat(KEY_GLASS_CLARITY, DEFAULT_GLASS_CLARITY) ?: DEFAULT_GLASS_CLARITY).coerceIn(0f, 1f)
            glassShade.value = (prefs?.getFloat(KEY_GLASS_SHADE, DEFAULT_GLASS_SHADE) ?: DEFAULT_GLASS_SHADE).coerceIn(0f, 1f)
            liquidGlassBlurDp.value = (prefs?.getFloat(KEY_LIQUID_GLASS_BLUR_DP, DEFAULT_LIQUID_GLASS_BLUR_DP) ?: DEFAULT_LIQUID_GLASS_BLUR_DP)
                .coerceIn(MIN_LIQUID_GLASS_BLUR_DP, MAX_LIQUID_GLASS_BLUR_DP)
            liquidGlassOpacity.value = (prefs?.getFloat(KEY_LIQUID_GLASS_OPACITY, DEFAULT_LIQUID_GLASS_OPACITY) ?: DEFAULT_LIQUID_GLASS_OPACITY)
                .coerceIn(MIN_LIQUID_GLASS_OPACITY, MAX_LIQUID_GLASS_OPACITY)
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

    fun setGlassRenderStyle(style: GlassRenderStyle) {
        glassRenderStyle.value = style
        prefs?.edit()?.putString(KEY_GLASS_RENDER_STYLE, style.key)?.apply()
    }

    fun setGlassClarity(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        glassClarity.value = clamped
        prefs?.edit()?.putFloat(KEY_GLASS_CLARITY, clamped)?.apply()
    }

    fun setGlassShade(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        glassShade.value = clamped
        prefs?.edit()?.putFloat(KEY_GLASS_SHADE, clamped)?.apply()
    }

    fun setLiquidGlassBlurDp(value: Float) {
        val clamped = value.coerceIn(MIN_LIQUID_GLASS_BLUR_DP, MAX_LIQUID_GLASS_BLUR_DP)
        liquidGlassBlurDp.value = clamped
        prefs?.edit()?.putFloat(KEY_LIQUID_GLASS_BLUR_DP, clamped)?.apply()
    }

    fun setLiquidGlassOpacity(value: Float) {
        val clamped = value.coerceIn(MIN_LIQUID_GLASS_OPACITY, MAX_LIQUID_GLASS_OPACITY)
        liquidGlassOpacity.value = clamped
        prefs?.edit()?.putFloat(KEY_LIQUID_GLASS_OPACITY, clamped)?.apply()
    }
}
