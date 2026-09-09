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
 * スタッフ・声優・スタジオの英語名（ローマ字・公式英名）解決シングルトン
 */
object StaffNameResolver {
    private val json = Json { ignoreUnknownKeys = true }
    private var staffEnMap: Map<String, String> = emptyMap()
    private var isLoaded = false

    private val STUDIO_EN_MAP = mapOf(
        "スタジオジブリ" to "Studio Ghibli",
        "MADHOUSE" to "MADHOUSE",
        "京都アニメーション" to "Kyoto Animation",
        "ボンズ" to "Bones",
        "シャフト" to "Shaft",
        "サンライズ" to "Sunrise",
        "WIT STUDIO" to "WIT STUDIO",
        "CloverWorks" to "CloverWorks",
        "A-1 Pictures" to "A-1 Pictures",
        "MAPPA" to "MAPPA",
        "ufotable" to "ufotable",
        "TRIGGER" to "TRIGGER",
        "スタジオ地図" to "Studio Chizu",
        "コミックス・ウェーブ・フィルム" to "CoMix Wave Films",
        "Production I.G" to "Production I.G",
        "東映アニメーション" to "Toei Animation",
        "スタジオぴえろ" to "Pierrot",
        "トムス・エンタテインメント" to "TMS Entertainment",
        "J.C.STAFF" to "J.C.STAFF",
        "P.A.WORKS" to "P.A.WORKS",
        "動画工房" to "Doga Kobo",
        "SILVER LINK." to "SILVER LINK.",
        "キネマシトラス" to "Kinema Citrus",
        "サイエンスSARU" to "Science SARU",
        "スタジオディーン" to "Studio Deen",
        "OLM" to "OLM",
        "AIC" to "AIC",
        "GONZO" to "GONZO",
        "XEBEC" to "XEBEC",
        "TROYCA" to "TROYCA",
        "Lerche" to "Lerche",
        "feel." to "feel.",
        "タツノコプロ" to "Tatsunoko Production",
        "GAINAX" to "GAINAX",
        "david production" to "David Production",
        "スタジオバインド" to "Studio Bind",
        "スタジオヴォルン" to "Studio VOLN",
        "Nexus" to "Nexus",
        "C-Station" to "C-Station",
        "テレコム・アニメーションフィルム" to "Telecom Animation Film",
        "シンエイ動画" to "Shin-Ei Animation",
        "日本アニメーション" to "Nippon Animation",
        "ライデンフィルム" to "LIDENFILMS",
        "WHITE FOX" to "WHITE FOX"
    )

    private var reverseRomajiIndex: List<Pair<String, String>> = emptyList()

    fun init(context: Context) {
        if (isLoaded) return
        CoroutineScope(Dispatchers.IO).launch {
            loadDictionary(context)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun loadDictionary(context: Context) = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream = context.assets.open("staff_en_names.json")
            val map = json.decodeFromStream<Map<String, String>>(inputStream)
            staffEnMap = map
            reverseRomajiIndex = map.map { (kanji, en) ->
                normalizeRomaji(en) to kanji
            }
            isLoaded = true
        } catch (e: Exception) {
            staffEnMap = emptyMap()
            reverseRomajiIndex = emptyList()
        }
    }

    private fun normalizeRomaji(str: String): String {
        return str.lowercase()
            .replace("ou", "o")
            .replace("oo", "o")
            .replace("uu", "u")
            .replace("ā", "a").replace("ē", "e").replace("ī", "i").replace("ō", "o").replace("ū", "u")
            .replace("[^a-z0-9]".toRegex(), "")
    }

    /**
     * ローマ字・英名クエリから合致するスタッフ漢字名リストを高速抽出
     */
    fun searchKanjiByRomaji(query: String, maxResults: Int = 100): List<String> {
        val cleanQ = normalizeRomaji(query)
        if (cleanQ.isBlank() || cleanQ.length < 2) return emptyList()
        return reverseRomajiIndex
            .filter { it.first.contains(cleanQ) }
            .map { it.second }
            .distinct()
            .take(maxResults)
    }

    /**
     * ローマ字・英名クエリから合致するスタジオ漢字名リストを抽出
     */
    fun searchStudiosByRomaji(query: String): List<String> {
        val cleanQ = normalizeRomaji(query)
        if (cleanQ.isBlank()) return emptyList()
        return STUDIO_EN_MAP.entries
            .filter { normalizeRomaji(it.value).contains(cleanQ) }
            .map { it.key }
    }

    fun getStaffEn(name: String): String? {
        return staffEnMap[name.trim()]?.takeIf { it.isNotBlank() }
    }

    fun getStaffName(name: String, isEnglish: Boolean): String {
        if (!isEnglish) return name
        return getStaffEn(name) ?: name
    }

    fun getStudioEn(name: String): String? {
        return STUDIO_EN_MAP[name.trim()]
    }

    fun getStudioName(name: String, isEnglish: Boolean): String {
        if (!isEnglish) return name
        return getStudioEn(name) ?: name
    }
}
