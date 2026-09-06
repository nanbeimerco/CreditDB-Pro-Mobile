package com.creditdb.pro.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * カラーテーマプリセット管理シングルトン
 * SharedPreferences を利用して選択状態を永続化
 */
object ThemeManager {
    private const val PREFS_NAME = "creditdb_pro_theme_prefs"
    private const val KEY_SELECTED_PRESET = "selected_preset_id"

    var currentPreset by mutableStateOf(ColorPresets.TITANIUM_SLATE)
        private set

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedId = prefs?.getString(KEY_SELECTED_PRESET, ColorPresets.TITANIUM_SLATE.id) ?: ColorPresets.TITANIUM_SLATE.id
            currentPreset = ColorPresets.getPresetById(savedId)
        }
    }

    fun selectPreset(preset: ColorPresetSpec) {
        currentPreset = preset
        prefs?.edit()?.putString(KEY_SELECTED_PRESET, preset.id)?.apply()
    }
}
