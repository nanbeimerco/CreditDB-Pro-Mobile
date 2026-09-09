package com.creditdb.pro.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class SummaryInfo(
    val totalWorks: Int,
    val totalStaff: Int,
    val totalCv: Int = 0,
    val yearMin: Int,
    val yearMax: Int,
    val updatedAt: String,
    val version: String,
    val globalMean: Double = 65.0
)

@Serializable
data class StaffCredit(
    val name: String,
    val ratingTier: String = "B",
    val cumulativeTier: String = "B",
    val characterName: String? = null,
    val relation: String? = null
) {
    fun getDisplayName(isEn: Boolean = com.creditdb.pro.ui.theme.LanguageManager.isEnglish): String =
        StaffNameResolver.getStaffName(name, isEn)

    fun getDisplayCharacterName(isEn: Boolean = com.creditdb.pro.ui.theme.LanguageManager.isEnglish): String? =
        characterName?.let { CharacterNameResolver.getCharacterName(it, isEn) }

    companion object {
        fun fromJsonElement(el: JsonElement): StaffCredit {
            return when (el) {
                is JsonObject -> {
                    val name = el["name"]?.jsonPrimitive?.content ?: ""
                    val rt = el["rt"]?.jsonPrimitive?.content ?: "B"
                    val ct = el["ct"]?.jsonPrimitive?.content ?: "B"
                    val cName = el["character_name"]?.jsonPrimitive?.content
                    val rel = el["relation"]?.jsonPrimitive?.content
                    StaffCredit(name = name, ratingTier = rt, cumulativeTier = ct, characterName = cName, relation = rel)
                }
                is JsonPrimitive -> {
                    StaffCredit(name = el.content, ratingTier = "B", cumulativeTier = "B")
                }
                else -> StaffCredit(name = el.toString(), ratingTier = "B", cumulativeTier = "B")
            }
        }
    }
}

@Serializable
data class CharacterCast(
    val characterName: String,
    val relation: String = "脇役",
    val actorName: String,
    val ratingTier: String = "B",
    val cumulativeTier: String = "B"
) {
    fun getActorDisplayName(isEn: Boolean = com.creditdb.pro.ui.theme.LanguageManager.isEnglish): String =
        StaffNameResolver.getStaffName(actorName, isEn)

    fun getCharacterDisplayName(isEn: Boolean = com.creditdb.pro.ui.theme.LanguageManager.isEnglish): String =
        CharacterNameResolver.getCharacterName(characterName, isEn)

    fun getRelationDisplayName(isEn: Boolean = com.creditdb.pro.ui.theme.LanguageManager.isEnglish): String =
        com.creditdb.pro.ui.theme.AppStrings.relationFull(relation, isEn)
}

data class WorkItem(
    val id: String,
    val title: String,
    val titleEn: String?,
    val year: Int,
    val deviationScore: Double,
    val deviationRank: Int,
    val anilistRawScore: Double,
    val rawRank: Int,
    val debiasedScore: Double = 0.0,
    val trueZScore: Double = 0.0,
    val predictedZScore: Double = 0.0,
    val predictedScore: Double = 0.0,
    val predScoreRank: Int = 0,
    val residual: Double = 0.0,
    val performanceVerdict: String = "概ねスタッフ前評判通り",
    val tier: String,
    val percentile: Double,
    val staffJson: String,
    val charactersJson: String = "[]",
    val mainStaffSummary: String
) {
    fun getDisplayTitle(isEn: Boolean = com.creditdb.pro.ui.theme.LanguageManager.isEnglish): Pair<String, String?> {
        val en = titleEn?.takeIf { it.isNotBlank() }
        return if (isEn) {
            (en ?: title) to (en?.takeIf { it != title }?.let { title })
        } else {
            title to (en?.takeIf { it != title })
        }
    }

    fun getMainStaffSummary(isEn: Boolean = com.creditdb.pro.ui.theme.LanguageManager.isEnglish): String {
        if (!isEn) return mainStaffSummary
        try {
            val parts = mutableListOf<String>()
            if (staffJson.isNotBlank()) {
                val root = org.json.JSONObject(staffJson)
                if (root.has("director")) {
                    val dir = root.getJSONArray("director")
                    if (dir.length() > 0) {
                        val dName = dir.getJSONObject(0).optString("name")
                        if (dName.isNotBlank()) {
                            val enName = StaffNameResolver.getStaffName(dName, true)
                            parts.add("Dir: $enName")
                        }
                    }
                }
                if (root.has("series_comp")) {
                    val comp = root.getJSONArray("series_comp")
                    if (comp.length() > 0) {
                        val cName = comp.getJSONObject(0).optString("name")
                        if (cName.isNotBlank()) {
                            val enName = StaffNameResolver.getStaffName(cName, true)
                            parts.add("Comp: $enName")
                        }
                    }
                }
            }
            if (charactersJson.isNotBlank()) {
                val chars = org.json.JSONArray(charactersJson)
                if (chars.length() > 0) {
                    val aName = chars.getJSONObject(0).optString("actor_name")
                    if (aName.isNotBlank()) {
                        val enName = StaffNameResolver.getStaffName(aName, true)
                        parts.add("CV: $enName")
                    }
                }
            }
            return if (parts.isNotEmpty()) parts.joinToString(" / ") else "Staff credits available"
        } catch (e: Exception) {
            return mainStaffSummary
        }
    }
}

data class WorkDetail(
    val id: String,
    val title: String,
    val titleEn: String?,
    val year: Int,
    val deviationScore: Double,
    val deviationRank: Int,
    val anilistRawScore: Double,
    val rawRank: Int,
    val debiasedScore: Double,
    val trueZScore: Double,
    val predictedZScore: Double,
    val predictedScore: Double,
    val predScoreRank: Int,
    val residual: Double,
    val performanceVerdict: String,
    val tier: String,
    val percentile: Double,
    val staffByRole: Map<String, List<StaffCredit>>,
    val characters: List<CharacterCast> = emptyList(),
    val studio: String? = null
) {
    fun getDisplayTitle(isEn: Boolean = com.creditdb.pro.ui.theme.LanguageManager.isEnglish): Pair<String, String?> {
        val en = titleEn?.takeIf { it.isNotBlank() }
        return if (isEn) {
            (en ?: title) to (en?.takeIf { it != title }?.let { title })
        } else {
            title to (en?.takeIf { it != title })
        }
    }
}

data class StudioWorkItem(
    val workId: String,
    val title: String,
    val year: Int,
    val deviationScore: Double,
    val tier: String,
    val titleEn: String? = null
) {
    fun getDisplayTitle(isEn: Boolean = com.creditdb.pro.ui.theme.LanguageManager.isEnglish): String {
        return if (isEn && !titleEn.isNullOrBlank()) titleEn else title
    }
}

data class LeaderboardItem(
    val role: String,
    val name: String,
    val worksCount: Int,
    val rating: Double,
    val cumulativeZ: Double,
    val ratingRank: Int,
    val cumulativeRank: Int,
    val ratingTier: String,
    val cumulativeTier: String,
    val bestWorkTitle: String?,
    val bestWorkYear: Int?,
    val bestWorkZ: Double?,
    val topCharacter: String? = null,
    val bestWorkTitleEn: String? = null
) {
    fun getDisplayName(isEn: Boolean = com.creditdb.pro.ui.theme.LanguageManager.isEnglish): String {
        return if (role == "studio") {
            StaffNameResolver.getStudioName(name, isEn)
        } else {
            StaffNameResolver.getStaffName(name, isEn)
        }
    }

    fun getDisplayBestWork(isEn: Boolean = com.creditdb.pro.ui.theme.LanguageManager.isEnglish): String? {
        if (bestWorkTitle.isNullOrBlank()) return null
        return if (isEn && !bestWorkTitleEn.isNullOrBlank()) bestWorkTitleEn else bestWorkTitle
    }

    fun getDisplayTopCharacter(isEn: Boolean = com.creditdb.pro.ui.theme.LanguageManager.isEnglish): String? {
        if (topCharacter.isNullOrBlank()) return null
        return CharacterNameResolver.formatTopCharacter(topCharacter, isEn)
    }
}

@Serializable
data class RoleStat(
    val role: String,
    val works_count: Int,
    val bayesian_rating: Double,
    @SerialName("cumulative_z")
    val career_cumulative_z: Double = 0.0,
    val rating_rank: Int = 0,
    val cumulative_rank: Int = 0,
    val role_total: Int = 0,
    val rating_tier: String = "B",
    @SerialName("cumulative_tier")
    val cum_tier: String = "B"
)

@Serializable
data class BestWork(
    val work_title: String,
    val work_id: String,
    val year: Int,
    val role: String,
    val z_score: Double,
    val work_title_en: String? = null
) {
    fun getDisplayTitle(isEn: Boolean = com.creditdb.pro.ui.theme.LanguageManager.isEnglish): String {
        return if (isEn && !work_title_en.isNullOrBlank()) work_title_en else work_title
    }
    fun getSecondaryTitle(isEn: Boolean = com.creditdb.pro.ui.theme.LanguageManager.isEnglish): String? {
        return if (isEn && !work_title_en.isNullOrBlank() && work_title_en != work_title) work_title else null
    }
}

@Serializable
data class CareerTrajectoryItem(
    val year: Int,
    val work_title: String,
    val work_id: String,
    val role: String,
    val z_score: Double,
    val character_name: String? = null,
    val character_relation: String? = null,
    val work_title_en: String? = null
) {
    fun getDisplayTitle(isEn: Boolean = com.creditdb.pro.ui.theme.LanguageManager.isEnglish): String {
        return if (isEn && !work_title_en.isNullOrBlank()) work_title_en else work_title
    }
    fun getSecondaryTitle(isEn: Boolean = com.creditdb.pro.ui.theme.LanguageManager.isEnglish): String? {
        return if (isEn && !work_title_en.isNullOrBlank() && work_title_en != work_title) work_title else null
    }
    fun getDisplayCharacterName(isEn: Boolean = com.creditdb.pro.ui.theme.LanguageManager.isEnglish): String? {
        if (character_name.isNullOrBlank()) return null
        return CharacterNameResolver.getCharacterName(character_name, isEn)
    }
}

data class StaffProfile(
    val name: String,
    val primaryRole: String,
    val totalWorks: Int,
    val bayesianRating: Double,
    val overallRatingTier: String,
    val overallRank: Int,
    val careerCumulativeZ: Double,
    val overallCumTier: String,
    val cumulativeRank: Int,
    val allRoleStats: List<RoleStat>,
    val bestWorks: List<BestWork>,
    val careerTrajectory: List<CareerTrajectoryItem>,
    val isFallback: Boolean = false
)

data class StaffRoleFeature(
    val name: String,
    val role: String,
    val worksCount: Int,
    val sumZ: Double,
    val meanZ: Double,
    val maxZ: Double,
    val bayesianS: Double
)

data class StaffCandidate(
    val name: String,
    val role: String,
    val worksCount: Int,
    val ratingTier: String,
    val cumulativeTier: String,
    val topCharacter: String? = null
) {
    fun getDisplayTopCharacter(isEn: Boolean = com.creditdb.pro.ui.theme.LanguageManager.isEnglish): String? {
        if (topCharacter.isNullOrBlank()) return null
        return CharacterNameResolver.formatTopCharacter(topCharacter, isEn)
    }
}

data class PredictionResult(
    val title: String,
    val year: Int,
    val predictedZ: Double,
    val predictedScore: Double,
    val deviationScore: Double,
    val tier: String,
    val verdict: String
)

@Serializable
data class PredictionPreset(
    val id: String,
    val name: String,
    val year: Int = 2024,
    val director: List<String> = emptyList(),
    val seriesComp: List<String> = emptyList(),
    val charDesign: List<String> = emptyList(),
    val sakkan: List<String> = emptyList(),
    val genga: List<String> = emptyList(),
    val unitDirector: List<String> = emptyList(),
    val music: List<String> = emptyList(),
    val artDir: List<String> = emptyList(),
    val cv: List<String> = emptyList()
)

enum class WorksSortOption(val label: String) {
    DEVIATION_DESC("偏差値 (高い順)"),
    DEVIATION_ASC("偏差値 (低い順)"),
    RAW_DESC("AniList素点 (高い順)"),
    RAW_ASC("AniList素点 (低い順)"),
    PRED_DESC("予測スコア (高い順)"),
    RESIDUAL_DESC("残差 (期待値以上順)"),
    RESIDUAL_ASC("残差 (ポテンシャル未達順)"),
    YEAR_DESC("公開年 (新しい順)"),
    YEAR_ASC("公開年 (古い順)"),
    TITLE_ASC("作品名 (五十音順)");

    fun getDisplayName(isEn: Boolean = com.creditdb.pro.ui.theme.LanguageManager.isEnglish): String {
        return if (isEn) {
            when (this) {
                DEVIATION_DESC -> "Deviation (High-Low)"
                DEVIATION_ASC -> "Deviation (Low-High)"
                RAW_DESC -> "AniList Raw (High-Low)"
                RAW_ASC -> "AniList Raw (Low-High)"
                PRED_DESC -> "Predicted (High-Low)"
                RESIDUAL_DESC -> "Residual (Surprise)"
                RESIDUAL_ASC -> "Residual (Underperform)"
                YEAR_DESC -> "Year (Newest)"
                YEAR_ASC -> "Year (Oldest)"
                TITLE_ASC -> "Title (A-Z)"
            }
        } else {
            label
        }
    }
}

enum class StaffSortOption(val label: String) {
    RATING("総合実力 S(a) 順"),
    CUMULATIVE("生涯累積実績 ΣZ 順");

    fun getDisplayName(isEn: Boolean = com.creditdb.pro.ui.theme.LanguageManager.isEnglish): String {
        return if (isEn) {
            when (this) {
                RATING -> "Power Score S(a)"
                CUMULATIVE -> "Cumulative ΣZ"
            }
        } else {
            label
        }
    }
}

object RoleConstants {
    val ROLE_ORDER = listOf(
        "director",
        "series_comp",
        "char_design",
        "sakkan",
        "genga",
        "unit_director",
        "music",
        "art_dir",
        "cv",
        "studio"
    )

    val ROLE_NAMES = mapOf(
        "all" to "全役職",
        "director" to "監督",
        "series_comp" to "シリーズ構成 / 脚本",
        "char_design" to "キャラクターデザイン",
        "sakkan" to "作画監督",
        "genga" to "原画",
        "unit_director" to "演出 / 副監督",
        "music" to "音楽",
        "art_dir" to "美術監督",
        "cv" to "声優 / キャスト",
        "studio" to "制作スタジオ"
    )

    val SHORT_ROLE_NAMES = mapOf(
        "all" to "全体",
        "director" to "監督",
        "series_comp" to "構成/脚本",
        "char_design" to "キャラデザ",
        "sakkan" to "作監",
        "genga" to "原画",
        "unit_director" to "演出/副監督",
        "music" to "音楽",
        "art_dir" to "美術監督",
        "cv" to "声優",
        "studio" to "スタジオ"
    )

    fun getDisplayName(roleKey: String, isEn: Boolean = com.creditdb.pro.ui.theme.LanguageManager.isEnglish): String {
        return com.creditdb.pro.ui.theme.AppStrings.roleFull(roleKey, isEn)
    }

    fun getShortDisplayName(roleKey: String, isEn: Boolean = com.creditdb.pro.ui.theme.LanguageManager.isEnglish): String {
        return com.creditdb.pro.ui.theme.AppStrings.roleCompact(roleKey, isEn)
    }

    fun getMicroDisplayName(roleKey: String): String {
        return com.creditdb.pro.ui.theme.AppStrings.roleMicro(roleKey)
    }
}

typealias RoleDefinitions = RoleConstants

/**
 * アプリ設定・状態の永続化管理
 */
object AppSettings {
    private const val PREFS_NAME = "creditdb_app_settings"
    private const val KEY_ADVANCED_METRICS = "is_advanced_metrics_enabled"

    var isAdvancedMetricsEnabled by mutableStateOf(false)
        private set

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            isAdvancedMetricsEnabled = prefs?.getBoolean(KEY_ADVANCED_METRICS, false) ?: false
        }
    }

    fun setAdvancedMetrics(enabled: Boolean) {
        isAdvancedMetricsEnabled = enabled
        prefs?.edit()?.putBoolean(KEY_ADVANCED_METRICS, enabled)?.apply()
    }
}
