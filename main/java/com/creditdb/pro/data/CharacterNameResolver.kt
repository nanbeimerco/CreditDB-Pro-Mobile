package com.creditdb.pro.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.InputStream

/**
 * キャラクター英語名（公式英名・ローマ字）解決シングルトン
 */
object CharacterNameResolver {
    private val json = Json { ignoreUnknownKeys = true }
    private var charEnMap: Map<String, String> = emptyMap()
    private var isLoaded = false

    fun init(context: Context) {
        if (isLoaded) return
        CoroutineScope(Dispatchers.IO).launch {
            loadDictionary(context)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun loadDictionary(context: Context) = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream = context.assets.open("character_en_names.json")
            charEnMap = json.decodeFromStream<Map<String, String>>(inputStream)
            isLoaded = true
        } catch (e: Exception) {
            charEnMap = emptyMap()
        }
    }

    /**
     * キャラクター名を言語設定に応じて取得
     */
    fun getCharacterName(
        name: String?,
        isEn: Boolean = com.creditdb.pro.ui.theme.LanguageManager.isEnglish
    ): String {
        if (name.isNullOrBlank()) return ""
        val trimmed = name.trim()
        if (!isEn) return trimmed
        return charEnMap[trimmed] ?: trimmed
    }

    /**
     * 代表配役（例: "夜ト (主角)" や "狡噛慎也 (配角)"）を言語設定に応じて整形
     */
    fun formatTopCharacter(
        topChar: String?,
        isEn: Boolean = com.creditdb.pro.ui.theme.LanguageManager.isEnglish
    ): String {
        if (topChar.isNullOrBlank()) return ""
        val trimmed = topChar.trim()
        if (!isEn) return trimmed

        val match = Regex("""^(.*?)\s*\((主角|配角|客串)\)$""").find(trimmed)
        if (match != null) {
            val cName = match.groupValues[1].trim()
            val rel = match.groupValues[2]
            val enName = getCharacterName(cName, true)
            val enRel = when (rel) {
                "主角" -> "Main"
                "配角" -> "Supporting"
                "客串" -> "Guest"
                else -> rel
            }
            return "$enName ($enRel)"
        }
        return getCharacterName(trimmed, true)
    }
}
