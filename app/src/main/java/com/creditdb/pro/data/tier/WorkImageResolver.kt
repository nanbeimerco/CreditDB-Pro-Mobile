package com.creditdb.pro.data.tier

import android.content.Context
import com.creditdb.pro.data.WorkItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object WorkImageResolver {
    private var coverMap: Map<String, Int>? = null

    fun initialize(context: Context) {
        if (coverMap != null) return
        try {
            context.assets.open("work_covers.json").use { stream ->
                val text = stream.bufferedReader().use { it.readText() }
                val jsonElement = Json.parseToJsonElement(text).jsonObject
                val map = mutableMapOf<String, Int>()
                for ((k, v) in jsonElement) {
                    v.jsonPrimitive.content.toIntOrNull()?.let { id ->
                        map[k] = id
                    }
                }
                coverMap = map
            }
        } catch (e: Exception) {
            coverMap = emptyMap()
        }
    }

    fun getCoverImageUrl(workId: String, context: Context? = null): String? {
        if (coverMap == null && context != null) {
            initialize(context)
        }
        val bgmId = coverMap?.get(workId) ?: return null
        return "https://api.bgm.tv/v0/subjects/$bgmId/image?type=medium"
    }

    fun fromWorkItem(work: WorkItem, context: Context? = null): TierAnimeItem {
        return TierAnimeItem(
            id = work.id,
            title = work.title,
            titleEn = work.titleEn,
            year = work.year,
            deviationScore = work.deviationScore,
            tier = work.tier,
            residual = work.residual,
            predictedScore = work.predictedScore,
            imageUrl = getCoverImageUrl(work.id, context),
            staffJson = work.staffJson,
            charactersJson = work.charactersJson
        )
    }
}
