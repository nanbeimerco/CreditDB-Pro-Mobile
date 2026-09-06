package com.creditdb.pro.ui.theme

import androidx.compose.ui.graphics.Color

data class TierColorSpec(
    val containerColor: Color,
    val onContainerColor: Color,
    val borderColor: Color,
    val tierName: String,
    val description: String
) {
    fun getDescription(isEn: Boolean = LanguageManager.isEnglish): String =
        AppStrings.tierDescription(tierName, isEn)
}

object TierTheme {
    val TIER_S_PLUS = TierColorSpec(
        containerColor = Color(0xFFEAC765).copy(alpha = 0.15f),
        onContainerColor = Color(0xFFEAC765), // M3 Classic Gold (Tone 80)
        borderColor = Color(0xFFEAC765).copy(alpha = 0.4f),
        tierName = "S+",
        description = "同年代の中で極めて突出した歴史的メガヒット・超名作水準（上位約2.3%以内）"
    )

    val TIER_S = TierColorSpec(
        containerColor = Color(0xFF8BD3A7).copy(alpha = 0.15f),
        onContainerColor = Color(0xFF8BD3A7), // M3 Soft Emerald (Tone 80)
        borderColor = Color(0xFF8BD3A7).copy(alpha = 0.4f),
        tierName = "S",
        description = "その時代を代表する大傑作。高いクオリティと広範な支持を獲得した作品（上位約6.7%以内）"
    )

    val TIER_A_PLUS = TierColorSpec(
        containerColor = Color(0xFF8BC5E3).copy(alpha = 0.15f),
        onContainerColor = Color(0xFF8BC5E3), // M3 Slate Cyan (Tone 80)
        borderColor = Color(0xFF8BC5E3).copy(alpha = 0.4f),
        tierName = "A+",
        description = "同年代の上位15%に位置する確かな完成度と魅力を誇る秀作（上位約15.9%以内）"
    )

    val TIER_A = TierColorSpec(
        containerColor = Color(0xFFA3B3E7).copy(alpha = 0.15f),
        onContainerColor = Color(0xFFA3B3E7), // M3 Cool Indigo (Tone 80)
        borderColor = Color(0xFFA3B3E7).copy(alpha = 0.4f),
        tierName = "A",
        description = "平均を明確に上回り、ファン層から根強く支持される良作水準（上位約30.9%以内）"
    )

    val TIER_B_PLUS = TierColorSpec(
        containerColor = Color(0xFF93CCCC).copy(alpha = 0.15f),
        onContainerColor = Color(0xFF93CCCC), // M3 Sage Teal (Tone 80)
        borderColor = Color(0xFF93CCCC).copy(alpha = 0.4f),
        tierName = "B+",
        description = "年代の平均水準以上を堅実に維持している安定作"
    )

    val TIER_B = TierColorSpec(
        containerColor = Color(0xFFAEB2BA).copy(alpha = 0.15f),
        onContainerColor = Color(0xFFAEB2BA), // M3 Neutral Slate (Tone 80)
        borderColor = Color(0xFFAEB2BA).copy(alpha = 0.4f),
        tierName = "B",
        description = "年代の平均的ボリュームゾーンに位置する標準的な作品"
    )

    val TIER_C = TierColorSpec(
        containerColor = Color(0xFFC5A6C1).copy(alpha = 0.15f),
        onContainerColor = Color(0xFFC5A6C1), // M3 Muted Plum (Tone 80)
        borderColor = Color(0xFFC5A6C1).copy(alpha = 0.4f),
        tierName = "C",
        description = "同年代の平均的な評価を下回った作品群"
    )

    val TIER_D = TierColorSpec(
        containerColor = Color(0xFFDEA1A9).copy(alpha = 0.15f),
        onContainerColor = Color(0xFFDEA1A9), // M3 Muted Rose (Tone 80)
        borderColor = Color(0xFFDEA1A9).copy(alpha = 0.4f),
        tierName = "D",
        description = "同年代の平均的な評価を大きく下回った作品群"
    )

    fun forTier(tier: String?): TierColorSpec {
        return when (tier?.trim()?.uppercase()) {
            "S+" -> TIER_S_PLUS
            "S" -> TIER_S
            "A+" -> TIER_A_PLUS
            "A" -> TIER_A
            "B+" -> TIER_B_PLUS
            "B" -> TIER_B
            "C" -> TIER_C
            "D" -> TIER_D
            else -> TIER_B
        }
    }
}
