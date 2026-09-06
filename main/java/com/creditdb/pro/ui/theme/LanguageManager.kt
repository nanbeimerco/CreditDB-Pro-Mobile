package com.creditdb.pro.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

enum class AppLanguage(val id: String, val displayNameJa: String, val displayNameEn: String) {
    SYSTEM("system", "端末の言語に従う", "System Default"),
    JAPANESE("ja", "日本語", "Japanese"),
    ENGLISH("en", "English", "English")
}

/**
 * アプリの表示言語管理シングルトン
 * SharedPreferences で選択状態を永続化し、Compose のリアクティブな状態として管理
 */
object LanguageManager {
    private const val PREFS_NAME = "creditdb_pro_language_prefs"
    private const val KEY_SELECTED_LANGUAGE = "selected_language_id"

    var currentLanguage by mutableStateOf(AppLanguage.SYSTEM)
        private set

    var isEnglish by mutableStateOf(false)
        private set

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedId = prefs?.getString(KEY_SELECTED_LANGUAGE, AppLanguage.SYSTEM.id) ?: AppLanguage.SYSTEM.id
            currentLanguage = AppLanguage.values().firstOrNull { it.id == savedId } ?: AppLanguage.SYSTEM
            updateIsEnglish()
        }
    }

    fun setLanguage(language: AppLanguage) {
        currentLanguage = language
        prefs?.edit()?.putString(KEY_SELECTED_LANGUAGE, language.id)?.apply()
        updateIsEnglish()
    }

    /**
     * TopAppBarのボタン等からJP/ENをワンタップで高速切り替え
     */
    fun toggleLanguage() {
        val next = if (isEnglish) AppLanguage.JAPANESE else AppLanguage.ENGLISH
        setLanguage(next)
    }

    private fun updateIsEnglish() {
        isEnglish = when (currentLanguage) {
            AppLanguage.ENGLISH -> true
            AppLanguage.JAPANESE -> false
            AppLanguage.SYSTEM -> {
                val sysLang = Locale.getDefault().language
                sysLang.startsWith("en", ignoreCase = true) || !sysLang.startsWith("ja", ignoreCase = true)
            }
        }
    }
}
