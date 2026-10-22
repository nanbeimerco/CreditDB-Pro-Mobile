package com.creditdb.pro.data.tier

import android.content.Context
import com.creditdb.pro.data.DatabaseHelper
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
            val map = HashMap<String, String>()
            // 1. APK 同梱の対応表（初回起動・オフライン用）
            try {
                context.applicationContext.assets.open("work_covers.json").use { stream ->
                    val text = stream.bufferedReader().use { it.readText() }
                    val jsonElement = Json.parseToJsonElement(text).jsonObject
                    for ((k, v) in jsonElement) {
                        map[k] = v.jsonPrimitive.content
                    }
                }
            } catch (_: Exception) {
            }
            // 2. DB 内の対応表（週次の DB 更新で新作のカバーが届く）。古い DB にはテーブルが無いので失敗は無視
            try {
                DatabaseHelper.getInstance(context.applicationContext).readableDatabase
                    .rawQuery("SELECT work_id, target FROM work_covers", null).use { c ->
                        while (c.moveToNext()) {
                            map[c.getString(0)] = c.getString(1)
                        }
                    }
            } catch (_: Exception) {
            }
            coverMap = map
        }
    }

    /** DB を差し替えた後に呼び、次回参照時に対応表を読み直させる */
    fun invalidate() {
        coverMap = null
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
