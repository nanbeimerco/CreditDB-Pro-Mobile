package com.creditdb.pro.data.tier

import android.content.Context
import com.creditdb.pro.data.WorkItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object WorkImageResolver {
    @Volatile
    private var coverMap: Map<String, String>? = null
    private var appContext: Context? = null

    fun initialize(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
        if (coverMap != null) return
        synchronized(this) {
            if (coverMap != null) return
            try {
                context.applicationContext.assets.open("work_covers.json").use { stream ->
                    val text = stream.bufferedReader().use { it.readText() }
                    val jsonElement = Json.parseToJsonElement(text).jsonObject
                    val map = HashMap<String, String>(jsonElement.size)
                    for ((k, v) in jsonElement) {
                        map[k] = v.jsonPrimitive.content
                    }
                    coverMap = map
                }
            } catch (e: Exception) {
                coverMap = emptyMap()
            }
        }
    }

    fun getCoverImageUrl(workId: String, context: Context? = null): String? {
        val ctx = context ?: appContext
        if (coverMap == null && ctx != null) {
            initialize(ctx)
        }
        val target = coverMap?.get(workId) ?: return null
        if (target.startsWith("http")) {
            return target
        }
        return "https://api.bgm.tv/v0/subjects/$target/image?type=medium"
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
