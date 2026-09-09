package com.creditdb.pro.data.tier

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

object TierStorageManager {
    private const val FILE_NAME = "tier_data.json"
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    val COLOR_PRESETS = listOf(
        "#EAC765", // M3 Gold (S+)
        "#F4B266", // Amber
        "#F29C5B", // Tangerine
        "#F08272", // Coral
        "#E06A7C", // Crimson
        "#DEA1A9", // Muted Rose
        "#D67597", // Berry
        "#C5A6C1", // Muted Plum
        "#B893D6", // Violet
        "#D7AEFB", // Soft Purple
        "#9C9EE8", // Periwinkle
        "#A3B3E7", // Cool Indigo
        "#8AB4F8", // Royal Blue
        "#74C0FC", // Sky Blue
        "#8BC5E3", // Slate Cyan
        "#7DD3FC", // Cyan Glacier
        "#93CCCC", // Sage Teal
        "#7CE0C3", // Mint
        "#8BD3A7", // Soft Emerald
        "#A2D785", // Lime Green
        "#B7D974", // Olive Light
        "#B0B7C6", // Warm Slate
        "#AEB2BA", // Slate Neutral
        "#9497A0"  // Charcoal Slate
    )

    private val HARMONIOUS_GRADIENT = listOf(
        "#EAC765", // 0: Gold
        "#F4B266", // 1: Amber
        "#8BD3A7", // 2: Soft Emerald
        "#7CE0C3", // 3: Mint
        "#8BC5E3", // 4: Slate Cyan
        "#74C0FC", // 5: Sky Blue
        "#A3B3E7", // 6: Cool Indigo
        "#B893D6", // 7: Violet
        "#C5A6C1", // 8: Plum
        "#DEA1A9", // 9: Muted Rose
        "#AEB2BA", // 10: Slate Neutral
        "#9497A0"  // 11: Charcoal Slate
    )

    /**
     * 行数に応じて上位〜下位まで美しく調和するグラデーション配色に一括整列
     */
    fun harmonizeRowColors(rows: List<TierRowData>): List<TierRowData> {
        if (rows.isEmpty()) return rows
        val n = rows.size
        return rows.mapIndexed { index, row ->
            val colorIndex = if (n == 1) 0 else ((index.toDouble() / (n - 1)) * (HARMONIOUS_GRADIENT.size - 1)).toInt()
            val newColor = HARMONIOUS_GRADIENT[colorIndex.coerceIn(0, HARMONIOUS_GRADIENT.lastIndex)]
            row.copy(colorHex = newColor)
        }
    }

    fun createDefaultConfig(): TierTableConfig {
        return TierTableConfig(
            version = 1,
            title = "My Anime Tier List",
            rows = listOf(
                TierRowData(id = UUID.randomUUID().toString(), name = "S+", colorHex = "#EAC765"),
                TierRowData(id = UUID.randomUUID().toString(), name = "S", colorHex = "#8BD3A7"),
                TierRowData(id = UUID.randomUUID().toString(), name = "A+", colorHex = "#8BC5E3"),
                TierRowData(id = UUID.randomUUID().toString(), name = "A", colorHex = "#A3B3E7"),
                TierRowData(id = UUID.randomUUID().toString(), name = "B", colorHex = "#AEB2BA"),
                TierRowData(id = UUID.randomUUID().toString(), name = "C", colorHex = "#C5A6C1"),
                TierRowData(id = UUID.randomUUID().toString(), name = "D", colorHex = "#DEA1A9")
            )
        )
    }

    fun loadConfig(context: Context): TierTableConfig {
        return try {
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists() && file.length() > 0) {
                val text = file.readText(Charsets.UTF_8)
                json.decodeFromString<TierTableConfig>(text)
            } else {
                val def = createDefaultConfig()
                saveConfig(context, def)
                def
            }
        } catch (e: Exception) {
            createDefaultConfig()
        }
    }

    fun saveConfig(context: Context, config: TierTableConfig) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            val text = json.encodeToString(config)
            file.writeText(text, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resetToDefault(context: Context): TierTableConfig {
        val def = createDefaultConfig()
        saveConfig(context, def)
        return def
    }

    fun clearAllItems(context: Context, current: TierTableConfig): TierTableConfig {
        val cleared = current.copy(
            rows = current.rows.map { it.copy(items = emptyList()) }
        )
        saveConfig(context, cleared)
        return cleared
    }
}
