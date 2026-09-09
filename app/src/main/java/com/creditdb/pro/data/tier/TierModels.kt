package com.creditdb.pro.data.tier

import kotlinx.serialization.Serializable
import com.creditdb.pro.ui.theme.LanguageManager

@Serializable
data class TierAnimeItem(
    val id: String,
    val title: String,
    val titleEn: String? = null,
    val year: Int = 0,
    val deviationScore: Double = 50.0,
    val tier: String = "B",
    val residual: Double = 0.0,
    val predictedScore: Double = 0.0,
    val imageUrl: String? = null,
    val staffJson: String = "{}",
    val charactersJson: String = "[]"
) {
    fun getDisplayTitle(isEn: Boolean = LanguageManager.isEnglish): Pair<String, String?> {
        val en = titleEn?.takeIf { it.isNotBlank() }
        return if (isEn) {
            (en ?: title) to (en?.takeIf { it != title }?.let { title })
        } else {
            title to (en?.takeIf { it != title })
        }
    }
}

@Serializable
data class TierRowData(
    val id: String,
    val name: String,
    val colorHex: String,
    val items: List<TierAnimeItem> = emptyList()
)

@Serializable
data class TierTableConfig(
    val version: Int = 1,
    val title: String = "My Anime Tier List",
    val rows: List<TierRowData> = emptyList()
)

@Serializable
data class StaffAffinityScore(
    val staffName: String,
    val roleKey: String,
    val weightedScore: Double,
    val workCount: Int,
    val works: List<String>
)

@Serializable
data class TasteCorrelationResult(
    val spearmanRho: Double,
    val sampleSize: Int,
    val avgDeviationPerTier: Map<String, Double>,
    val countPerTier: Map<String, Int>,
    val diagnosisTitleEn: String,
    val diagnosisTitleJa: String,
    val diagnosisDescEn: String,
    val diagnosisDescJa: String
)
