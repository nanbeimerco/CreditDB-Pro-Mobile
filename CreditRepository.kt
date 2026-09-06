package com.creditdb.pro.data

import android.content.Context
import com.creditdb.pro.AppConfig
import com.creditdb.pro.utils.TextNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class CreditRepository(private val context: Context) {
    private val dbHelper = DatabaseHelper.getInstance(context)
    private val predictionEngine = PredictionEngine.getInstance(context)
    private val json = Json { ignoreUnknownKeys = true }

    // Summary キャッシュ
    private var cachedSummary: SummaryInfo? = null

    suspend fun getSummary(): SummaryInfo = withContext(Dispatchers.IO) {
        cachedSummary?.let { return@withContext it }
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT total_works, total_staff, total_cv, year_min, year_max, updated_at, version, global_mean FROM summary LIMIT 1",
            null
        )
        var res = SummaryInfo(5452, 28596, 5840, 1950, 2026, "2026-09-05 22:50:00", "1.3.0", 65.0)
        if (cursor.moveToFirst()) {
            res = SummaryInfo(
                totalWorks = cursor.getInt(0),
                totalStaff = cursor.getInt(1),
                totalCv = cursor.getInt(2),
                yearMin = cursor.getInt(3),
                yearMax = cursor.getInt(4),
                updatedAt = cursor.getString(5) ?: "",
                version = cursor.getString(6) ?: "",
                globalMean = cursor.getDouble(7)
            )
        }
        cursor.close()
        cachedSummary = res
        res
    }

    /**
     * 作品一覧の検索・フィルタ・ソート・ページネーション (Compare & Works 兼用)
     */
    suspend fun getWorks(
        query: String = "",
        tierFilter: String = "all",
        eraFilter: String = "all",
        verdictFilter: String = "all",
        sortOption: WorksSortOption = WorksSortOption.DEVIATION_DESC,
        limit: Int = 50,
        offset: Int = 0
    ): List<WorkItem> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val conditions = mutableListOf<String>()
        val args = mutableListOf<String>()

        // 検索クエリ（作品名・英語名・全スタッフ名・全声優名・キャラクター名に対応）
        if (query.isNotBlank()) {
            val qNorm = TextNormalizer.normalize(query)
            conditions.add("search_text_norm LIKE ?")
            args.add("%$qNorm%")
        }

        // Tier フィルタ
        if (tierFilter != "all") {
            conditions.add("tier = ?")
            args.add(tierFilter)
        }

        // 総合判定フィルタ
        if (verdictFilter != "all") {
            conditions.add("performance_verdict LIKE ?")
            args.add("%$verdictFilter%")
        }

        // 年代フィルタ
        when (eraFilter) {
            "2020s" -> conditions.add("year >= 2020")
            "2010s" -> conditions.add("year >= 2010 AND year < 2020")
            "2000s" -> conditions.add("year >= 2000 AND year < 2010")
            "1990s" -> conditions.add("year >= 1990 AND year < 2000")
            "1980s" -> conditions.add("year < 1990")
        }

        val whereClause = if (conditions.isNotEmpty()) "WHERE " + conditions.joinToString(" AND ") else ""

        val orderClause = when (sortOption) {
            WorksSortOption.DEVIATION_DESC -> "ORDER BY deviation_score DESC"
            WorksSortOption.DEVIATION_ASC -> "ORDER BY deviation_score ASC"
            WorksSortOption.RAW_DESC -> "ORDER BY anilist_raw_score DESC"
            WorksSortOption.RAW_ASC -> "ORDER BY anilist_raw_score ASC"
            WorksSortOption.PRED_DESC -> "ORDER BY predicted_score DESC"
            WorksSortOption.RESIDUAL_DESC -> "ORDER BY residual DESC"
            WorksSortOption.RESIDUAL_ASC -> "ORDER BY residual ASC"
            WorksSortOption.YEAR_DESC -> "ORDER BY year DESC"
            WorksSortOption.YEAR_ASC -> "ORDER BY year ASC"
            WorksSortOption.TITLE_ASC -> "ORDER BY title ASC"
        }

        val sql = """
            SELECT work_id, title, title_en, year, deviation_score, z_score_rank,
                   anilist_raw_score, raw_score_rank, debiased_b_i, true_z_score,
                   predicted_z_score, predicted_score, pred_score_rank, residual,
                   performance_verdict, tier, percentile, staff_json, characters_json
            FROM works $whereClause $orderClause LIMIT ? OFFSET ?
        """.trimIndent()

        args.add(limit.toString())
        args.add(offset.toString())

        val cursor = db.rawQuery(sql, args.toTypedArray())
        val list = mutableListOf<WorkItem>()

        while (cursor.moveToNext()) {
            val staffJsonStr = cursor.getString(17) ?: "{}"
            val charJsonStr = cursor.getString(18) ?: "[]"
            val summaryStr = buildStaffSummary(staffJsonStr, charJsonStr)

            list.add(
                WorkItem(
                    id = cursor.getString(0),
                    title = cursor.getString(1),
                    titleEn = cursor.getString(2)?.takeIf { it.isNotBlank() },
                    year = cursor.getInt(3),
                    deviationScore = cursor.getDouble(4),
                    deviationRank = cursor.getInt(5),
                    anilistRawScore = cursor.getDouble(6),
                    rawRank = cursor.getInt(7),
                    debiasedScore = cursor.getDouble(8),
                    trueZScore = cursor.getDouble(9),
                    predictedZScore = cursor.getDouble(10),
                    predictedScore = cursor.getDouble(11),
                    predScoreRank = cursor.getInt(12),
                    residual = cursor.getDouble(13),
                    performanceVerdict = cursor.getString(14) ?: "概ねスタッフ前評判通り",
                    tier = cursor.getString(15),
                    percentile = cursor.getDouble(16),
                    staffJson = staffJsonStr,
                    charactersJson = charJsonStr,
                    mainStaffSummary = summaryStr
                )
            )
        }
        cursor.close()
        list
    }

    suspend fun getWorksCount(
        query: String = "",
        tierFilter: String = "all",
        eraFilter: String = "all",
        verdictFilter: String = "all"
    ): Int = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val conditions = mutableListOf<String>()
        val args = mutableListOf<String>()

        if (query.isNotBlank()) {
            val qNorm = TextNormalizer.normalize(query)
            conditions.add("search_text_norm LIKE ?")
            args.add("%$qNorm%")
        }

        if (tierFilter != "all") {
            conditions.add("tier = ?")
            args.add(tierFilter)
        }

        if (verdictFilter != "all") {
            conditions.add("performance_verdict LIKE ?")
            args.add("%$verdictFilter%")
        }

        when (eraFilter) {
            "2020s" -> conditions.add("year >= 2020")
            "2010s" -> conditions.add("year >= 2010 AND year < 2020")
            "2000s" -> conditions.add("year >= 2000 AND year < 2010")
            "1990s" -> conditions.add("year >= 1990 AND year < 2000")
            "1980s" -> conditions.add("year < 1990")
        }

        val whereClause = if (conditions.isNotEmpty()) "WHERE " + conditions.joinToString(" AND ") else ""
        val sql = "SELECT count(*) FROM works $whereClause"
        val cursor = db.rawQuery(sql, args.toTypedArray())
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        count
    }

    /**
     * 作品詳細の取得（全9役職クレジット + 声優キャスト一覧）
     */
    suspend fun getWorkDetail(workId: String): WorkDetail? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT work_id, title, title_en, year, deviation_score, z_score_rank,
                   anilist_raw_score, raw_score_rank, debiased_b_i, true_z_score,
                   predicted_z_score, predicted_score, pred_score_rank, residual,
                   performance_verdict, tier, percentile, staff_json, characters_json
            FROM works WHERE work_id = ? LIMIT 1
            """.trimIndent(),
            arrayOf(workId)
        )
        if (!cursor.moveToFirst()) {
            cursor.close()
            return@withContext null
        }

        val staffJsonStr = cursor.getString(17) ?: "{}"
        val charJsonStr = cursor.getString(18) ?: "[]"
        val rawStaffMap = parseStaffCredits(staffJsonStr)
        val studioList = rawStaffMap["studio"]
        val cleanedStudio = cleanStudio(studioList)

        val staffMap = rawStaffMap.toMutableMap()
        staffMap.remove("studio") // Studio is NOT a human creator; exclude from staff credits

        val charList = parseCharacters(charJsonStr)

        if (charList.isNotEmpty()) {
            staffMap["cv"] = charList.map { c ->
                StaffCredit(
                    name = c.actorName,
                    ratingTier = c.ratingTier,
                    cumulativeTier = c.cumulativeTier,
                    characterName = c.characterName,
                    relation = c.relation
                )
            }
        }

        val detail = WorkDetail(
            id = cursor.getString(0),
            title = cursor.getString(1),
            titleEn = cursor.getString(2)?.takeIf { it.isNotBlank() },
            year = cursor.getInt(3),
            deviationScore = cursor.getDouble(4),
            deviationRank = cursor.getInt(5),
            anilistRawScore = cursor.getDouble(6),
            rawRank = cursor.getInt(7),
            debiasedScore = cursor.getDouble(8),
            trueZScore = cursor.getDouble(9),
            predictedZScore = cursor.getDouble(10),
            predictedScore = cursor.getDouble(11),
            predScoreRank = cursor.getInt(12),
            residual = cursor.getDouble(13),
            performanceVerdict = cursor.getString(14) ?: "概ねスタッフ前評判通り",
            tier = cursor.getString(15),
            percentile = cursor.getDouble(16),
            staffByRole = staffMap,
            characters = charList,
            studio = cleanedStudio
        )
        cursor.close()
        detail
    }

    /**
     * 制作陣・声優リーダーボードの取得 (10 roles + studio)
     */
    suspend fun getLeaderboard(
        role: String = "all",
        query: String = "",
        sortOption: StaffSortOption = StaffSortOption.RATING,
        limit: Int = 50,
        offset: Int = 0
    ): List<LeaderboardItem> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase

        // スタジオ役職の場合は studios テーブルから高速取得
        if (role == "studio") {
            val conditions = mutableListOf<String>()
            val args = mutableListOf<String>()
            if (query.isNotBlank()) {
                val qNorm = TextNormalizer.normalize(query)
                conditions.add("name_norm LIKE ?")
                args.add("%$qNorm%")
            }
            val whereClause = if (conditions.isNotEmpty()) "WHERE " + conditions.joinToString(" AND ") else ""
            val sql = """
                SELECT name, works_count, best_work_title, best_work_year, best_work_dev, best_work_tier
                FROM studios $whereClause ORDER BY works_count DESC LIMIT ? OFFSET ?
            """.trimIndent()
            args.add(limit.toString())
            args.add(offset.toString())

            val cursor = db.rawQuery(sql, args.toTypedArray())
            val list = mutableListOf<LeaderboardItem>()
            var rank = offset + 1
            while (cursor.moveToNext()) {
                list.add(
                    LeaderboardItem(
                        role = "studio",
                        name = cursor.getString(0),
                        worksCount = cursor.getInt(1),
                        rating = 0.0,
                        cumulativeZ = 0.0,
                        ratingRank = rank,
                        cumulativeRank = rank,
                        ratingTier = cursor.getString(5) ?: "B",
                        cumulativeTier = "B",
                        bestWorkTitle = cursor.getString(2),
                        bestWorkYear = cursor.getInt(3).takeIf { it > 0 },
                        bestWorkZ = cursor.getDouble(4).takeIf { !cursor.isNull(4) }
                    )
                )
                rank++
            }
            cursor.close()
            return@withContext list
        }

        // 全役職で検索クエリがある場合、マッチするスタジオを先頭に統合
        val matchedStudios = mutableListOf<LeaderboardItem>()
        if (role == "all" && query.isNotBlank() && offset == 0) {
            val qNorm = TextNormalizer.normalize(query)
            val stCursor = db.rawQuery(
                "SELECT name, works_count, best_work_title, best_work_year, best_work_dev, best_work_tier FROM studios WHERE name_norm LIKE ? ORDER BY works_count DESC LIMIT 3",
                arrayOf("%$qNorm%")
            )
            while (stCursor.moveToNext()) {
                matchedStudios.add(
                    LeaderboardItem(
                        role = "studio",
                        name = stCursor.getString(0),
                        worksCount = stCursor.getInt(1),
                        rating = 0.0,
                        cumulativeZ = 0.0,
                        ratingRank = 0,
                        cumulativeRank = 0,
                        ratingTier = stCursor.getString(5) ?: "B",
                        cumulativeTier = "B",
                        bestWorkTitle = stCursor.getString(2),
                        bestWorkYear = stCursor.getInt(3).takeIf { it > 0 },
                        bestWorkZ = stCursor.getDouble(4).takeIf { !stCursor.isNull(4) }
                    )
                )
            }
            stCursor.close()
        }

        val conditions = mutableListOf<String>()
        val args = mutableListOf<String>()

        conditions.add("role = ?")
        args.add(role)

        if (query.isNotBlank()) {
            val qNorm = TextNormalizer.normalize(query)
            conditions.add("name_norm LIKE ?")
            args.add("%$qNorm%")
        }

        val whereClause = "WHERE " + conditions.joinToString(" AND ")
        val orderClause = when (sortOption) {
            StaffSortOption.RATING -> "ORDER BY rating_rank ASC"
            StaffSortOption.CUMULATIVE -> "ORDER BY cumulative_rank ASC"
        }

        val sql = """
            SELECT role, name, works_count, bayesian_rating, career_cumulative_z,
                   rating_rank, cumulative_rank, rating_tier, cumulative_tier,
                   best_work_title, best_work_year, best_work_z, top_character
            FROM leaderboards $whereClause $orderClause LIMIT ? OFFSET ?
        """.trimIndent()
        args.add(limit.toString())
        args.add(offset.toString())

        val cursor = db.rawQuery(sql, args.toTypedArray())
        val list = mutableListOf<LeaderboardItem>()
        while (cursor.moveToNext()) {
            list.add(
                LeaderboardItem(
                    role = cursor.getString(0),
                    name = cursor.getString(1),
                    worksCount = cursor.getInt(2),
                    rating = cursor.getDouble(3),
                    cumulativeZ = cursor.getDouble(4),
                    ratingRank = cursor.getInt(5),
                    cumulativeRank = cursor.getInt(6),
                    ratingTier = cursor.getString(7),
                    cumulativeTier = cursor.getString(8),
                    bestWorkTitle = cursor.getString(9),
                    bestWorkYear = cursor.getInt(10).takeIf { it > 0 },
                    bestWorkZ = cursor.getDouble(11).takeIf { !cursor.isNull(11) },
                    topCharacter = cursor.getString(12)?.takeIf { it.isNotBlank() }
                )
            )
        }
        cursor.close()
        matchedStudios + list
    }

    suspend fun getLeaderboardCount(
        role: String = "all",
        query: String = ""
    ): Int = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase

        if (role == "studio") {
            val conditions = mutableListOf<String>()
            val args = mutableListOf<String>()
            if (query.isNotBlank()) {
                val qNorm = TextNormalizer.normalize(query)
                conditions.add("name_norm LIKE ?")
                args.add("%$qNorm%")
            }
            val whereClause = if (conditions.isNotEmpty()) "WHERE " + conditions.joinToString(" AND ") else ""
            val cursor = db.rawQuery("SELECT count(*) FROM studios $whereClause", args.toTypedArray())
            var count = 0
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0)
            }
            cursor.close()
            return@withContext count
        }

        val conditions = mutableListOf<String>()
        val args = mutableListOf<String>()

        conditions.add("role = ?")
        args.add(role)

        if (query.isNotBlank()) {
            val qNorm = TextNormalizer.normalize(query)
            conditions.add("name_norm LIKE ?")
            args.add("%$qNorm%")
        }

        val whereClause = "WHERE " + conditions.joinToString(" AND ")
        val cursor = db.rawQuery("SELECT count(*) FROM leaderboards $whereClause", args.toTypedArray())
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        count
    }

    /**
     * オートコンプリート候補の取得
     */
    suspend fun getStaffCandidates(
        role: String,
        query: String,
        limit: Int = 20
    ): List<StaffCandidate> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val qNorm = TextNormalizer.normalize(query)
        val roleKey = if (role == "all") "all" else role

        val sql = """
            SELECT name, role, works_count, rating_tier, cumulative_tier, top_character
            FROM leaderboards
            WHERE role = ? AND name_norm LIKE ?
            ORDER BY rating_rank ASC
            LIMIT ?
        """.trimIndent()

        val cursor = db.rawQuery(sql, arrayOf(roleKey, "%$qNorm%", limit.toString()))
        val list = mutableListOf<StaffCandidate>()
        while (cursor.moveToNext()) {
            list.add(
                StaffCandidate(
                    name = cursor.getString(0),
                    role = cursor.getString(1),
                    worksCount = cursor.getInt(2),
                    ratingTier = cursor.getString(3),
                    cumulativeTier = cursor.getString(4),
                    topCharacter = cursor.getString(5)?.takeIf { it.isNotBlank() }
                )
            )
        }
        cursor.close()
        list
    }

    /**
     * スタッフ詳細プロファイル取得
     */
    suspend fun getStaffProfile(staffName: String): StaffProfile = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase

        val cursor = db.rawQuery(
            """
            SELECT p.name, p.primary_role, p.total_works, p.bayesian_rating,
                   COALESCE(l.rating_tier, p.overall_rating_tier),
                   COALESCE(l.rating_rank, p.overall_rank),
                   p.career_cumulative_z,
                   COALESCE(l.cumulative_tier, p.overall_cum_tier),
                   COALESCE(l.cumulative_rank, p.cumulative_rank),
                   p.all_role_stats_json,
                   p.career_trajectory_json
            FROM profiles p
            LEFT JOIN leaderboards l ON l.role = 'all' AND l.name = p.name
            WHERE p.name = ? LIMIT 1
            """.trimIndent(),
            arrayOf(staffName)
        )
        if (cursor.moveToFirst()) {
            try {
                val roleStats = json.decodeFromString<List<RoleStat>>(cursor.getString(9) ?: "[]")
                val trajectory = json.decodeFromString<List<CareerTrajectoryItem>>(cursor.getString(10) ?: "[]")
                val bestWorks = trajectory.sortedByDescending { it.z_score }.take(5).map {
                    BestWork(it.work_title, it.work_id, it.year, it.role, it.z_score)
                }

                val prof = StaffProfile(
                    name = cursor.getString(0),
                    primaryRole = cursor.getString(1),
                    totalWorks = cursor.getInt(2),
                    bayesianRating = cursor.getDouble(3),
                    overallRatingTier = cursor.getString(4),
                    overallRank = cursor.getInt(5),
                    careerCumulativeZ = cursor.getDouble(6),
                    overallCumTier = cursor.getString(7),
                    cumulativeRank = cursor.getInt(8),
                    allRoleStats = roleStats,
                    bestWorks = bestWorks,
                    careerTrajectory = trajectory,
                    isFallback = false
                )
                cursor.close()
                return@withContext prof
            } catch (e: Exception) {
                // fallback
            }
        }
        cursor.close()
        generateFallbackProfile(staffName)
    }

    private fun generateFallbackProfile(staffName: String): StaffProfile {
        val db = dbHelper.readableDatabase
        val lbCursor = db.rawQuery(
            """
            SELECT role, works_count, bayesian_rating, career_cumulative_z,
                   rating_rank, cumulative_rank, rating_tier, cumulative_tier
            FROM leaderboards WHERE name = ?
            """.trimIndent(),
            arrayOf(staffName)
        )
        var allItem: RoleStat? = null
        val roleStats = mutableListOf<RoleStat>()
        var primaryRole = "cv"
        var maxWorks = 0

        while (lbCursor.moveToNext()) {
            val role = lbCursor.getString(0)
            val w = lbCursor.getInt(1)
            val r = lbCursor.getDouble(2)
            val z = lbCursor.getDouble(3)
            val rk = lbCursor.getInt(4)
            val ck = lbCursor.getInt(5)
            val rt = lbCursor.getString(6)
            val ct = lbCursor.getString(7)

            val stat = RoleStat(
                role = role,
                works_count = w,
                bayesian_rating = r,
                career_cumulative_z = z,
                rating_rank = rk,
                cumulative_rank = ck,
                role_total = 1000,
                rating_tier = rt,
                cum_tier = ct
            )
            if (role == "all") {
                allItem = stat
            } else {
                roleStats.add(stat)
                if (w > maxWorks) {
                    maxWorks = w
                    primaryRole = role
                }
            }
        }
        lbCursor.close()

        val trajectory = mutableListOf<CareerTrajectoryItem>()
        val nameNorm = TextNormalizer.normalize(staffName)
        val wCursor = db.rawQuery(
            "SELECT work_id, title, year, true_z_score, staff_json, characters_json FROM works WHERE search_text_norm LIKE ? ORDER BY year ASC",
            arrayOf("%$nameNorm%")
        )
        while (wCursor.moveToNext()) {
            val workId = wCursor.getString(0)
            val title = wCursor.getString(1)
            val year = wCursor.getInt(2)
            val zScore = wCursor.getDouble(3)
            val staffJson = wCursor.getString(4) ?: "{}"
            val charJson = wCursor.getString(5) ?: "[]"

            var assignedRole = primaryRole
            var charName: String? = null

            // 声優チェック
            val chars = parseCharacters(charJson)
            val matchedChar = chars.firstOrNull { TextNormalizer.normalize(it.actorName) == nameNorm }
            if (matchedChar != null) {
                assignedRole = "cv"
                charName = matchedChar.characterName
            }

            // スタッフチェック
            if (charName == null) {
                val staffMap = parseStaffCredits(staffJson)
                for ((rKey, members) in staffMap) {
                    if (members.any { TextNormalizer.normalize(it.name) == nameNorm }) {
                        assignedRole = rKey
                        break
                    }
                }
            }

            trajectory.add(
                CareerTrajectoryItem(
                    year = year,
                    work_title = title,
                    work_id = workId,
                    role = assignedRole,
                    z_score = zScore,
                    character_name = charName
                )
            )
        }
        wCursor.close()

        val bestWorks = trajectory.sortedByDescending { it.z_score }.take(5).map {
            BestWork(it.work_title, it.work_id, it.year, it.role, it.z_score)
        }

        return StaffProfile(
            name = staffName,
            primaryRole = primaryRole,
            totalWorks = allItem?.works_count ?: trajectory.size,
            bayesianRating = allItem?.bayesian_rating ?: 0.0,
            overallRatingTier = allItem?.rating_tier ?: "B",
            overallRank = allItem?.rating_rank ?: 99999,
            careerCumulativeZ = allItem?.career_cumulative_z ?: 0.0,
            overallCumTier = allItem?.cum_tier ?: "B",
            cumulativeRank = allItem?.cumulative_rank ?: 99999,
            allRoleStats = roleStats,
            bestWorks = bestWorks,
            careerTrajectory = trajectory,
            isFallback = true
        )
    }

    /**
     * 潜在クオリティ予測 (Predict)
     */
    suspend fun predictQuality(
        title: String,
        year: Int,
        staffMap: Map<String, List<String>>
    ): PredictionResult = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val featuresMap = mutableMapOf<String, Float>()

        val mainRoles = listOf("director", "series_comp", "char_design", "unit_director", "sakkan", "music", "art_dir", "cv")

        for (role in mainRoles) {
            val names = staffMap[role] ?: emptyList()
            if (names.isEmpty()) {
                featuresMap["${role}_bayesian_s"] = 0f
                featuresMap["${role}_mean_z"] = 0f
                featuresMap["${role}_max_z"] = 0f
                featuresMap["${role}_past_count"] = 0f
            } else {
                val placeholders = names.joinToString(",") { "?" }
                val queryArgs = (listOf(role) + names).toTypedArray()
                val sql = "SELECT works_count, sum_z, max_z FROM staff_role_features WHERE role = ? AND name IN ($placeholders)"
                val cursor = db.rawQuery(sql, queryArgs)
                var totalCount = 0
                var sumZ = 0.0
                var maxZ = -999.0

                while (cursor.moveToNext()) {
                    val count = cursor.getInt(0)
                    val sZ = cursor.getDouble(1)
                    val mZ = cursor.getDouble(2)
                    totalCount += count
                    sumZ += sZ
                    if (mZ > maxZ) maxZ = mZ
                }
                cursor.close()

                if (totalCount > 0) {
                    val m = predictionEngine.getM(role)
                    val bayesianS = sumZ / (totalCount + m)
                    val meanZ = sumZ / totalCount
                    featuresMap["${role}_bayesian_s"] = bayesianS.toFloat()
                    featuresMap["${role}_mean_z"] = meanZ.toFloat()
                    featuresMap["${role}_max_z"] = (if (maxZ > -900) maxZ else 0.0).toFloat()
                    featuresMap["${role}_past_count"] = totalCount.toFloat()
                } else {
                    featuresMap["${role}_bayesian_s"] = 0f
                    featuresMap["${role}_mean_z"] = 0f
                    featuresMap["${role}_max_z"] = 0f
                    featuresMap["${role}_past_count"] = 0f
                }
            }
        }

        // Genga
        val gengaNames = staffMap["genga"] ?: emptyList()
        if (gengaNames.isNotEmpty()) {
            val placeholders = gengaNames.joinToString(",") { "?" }
            val queryArgs = (listOf("genga") + gengaNames).toTypedArray()
            val sql = "SELECT works_count, sum_z, max_z, bayesian_s FROM staff_role_features WHERE role = ? AND name IN ($placeholders)"
            val cursor = db.rawQuery(sql, queryArgs)
            var totalCount = 0
            var sumZ = 0.0
            var maxZ = -999.0
            var sSum = 0.0
            var topCount = 0
            var rowCount = 0

            while (cursor.moveToNext()) {
                val count = cursor.getInt(0)
                val sZ = cursor.getDouble(1)
                val mZ = cursor.getDouble(2)
                val bS = cursor.getDouble(3)
                totalCount += count
                sumZ += sZ
                sSum += bS
                if (mZ > maxZ) maxZ = mZ
                if (bS > 0.5) topCount++
                rowCount++
            }
            cursor.close()

            if (rowCount > 0) {
                featuresMap["genga_weighted_s"] = (sSum / rowCount).toFloat()
                featuresMap["genga_top_density"] = (topCount.toDouble() / rowCount).toFloat()
                featuresMap["genga_max_s"] = (if (maxZ > -900) maxZ else 0.0).toFloat()
                featuresMap["genga_total_count"] = gengaNames.size.toFloat()
                featuresMap["genga_experienced_ratio"] = (rowCount.toDouble() / gengaNames.size).toFloat()
                featuresMap["genga_sum_weights"] = gengaNames.size.toFloat()
            } else {
                featuresMap["genga_weighted_s"] = 0f
                featuresMap["genga_top_density"] = 0f
                featuresMap["genga_max_s"] = 0f
                featuresMap["genga_total_count"] = gengaNames.size.toFloat()
                featuresMap["genga_experienced_ratio"] = 0f
                featuresMap["genga_sum_weights"] = 0f
            }
        } else {
            featuresMap["genga_weighted_s"] = 0f
            featuresMap["genga_top_density"] = 0f
            featuresMap["genga_max_s"] = 0f
            featuresMap["genga_total_count"] = 0f
            featuresMap["genga_experienced_ratio"] = 0f
            featuresMap["genga_sum_weights"] = 0f
        }

        // Era stats lookup
        var eraStat: EraStat? = null
        val eraCursor = db.rawQuery("SELECT year, mean, std FROM era_stats WHERE year = ? LIMIT 1", arrayOf(year.toString()))
        if (eraCursor.moveToFirst()) {
            eraStat = EraStat(eraCursor.getInt(0), eraCursor.getDouble(1), eraCursor.getDouble(2))
        }
        eraCursor.close()

        val result = predictionEngine.predict(featuresMap, year, eraStat)
        result.copy(title = title.ifBlank { "新規企画アニメ" })
    }

    /**
     * GitHub Releases から最新の creditdb.db を確認・同期する
     */
    suspend fun syncDatabaseFromRemote(
        onProgress: (percent: Int, statusText: String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            onProgress(5, "GitHub Releases の最新データを問い合わせ中...")
            val releaseUrl = AppConfig.LATEST_RELEASE_API_URL
            val conn = (URL(releaseUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 15000
                setRequestProperty("User-Agent", "CreditDB-Android-App")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
            }

            val responseCode = conn.responseCode
            if (responseCode == 404) {
                onProgress(100, "最新リリースが見つかりませんでした (404)。GitHub Releases に公開されているか確認してください。")
                return@withContext
            }
            if (responseCode !in 200..299) {
                onProgress(100, "サーバー応答エラー (HTTP $responseCode)")
                return@withContext
            }

            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            val releaseJson = json.parseToJsonElement(responseText).jsonObject
            val tagName = releaseJson["tag_name"]?.jsonPrimitive?.content ?: "latest"
            val assets = releaseJson["assets"]?.jsonArray ?: JsonArray(emptyList())

            val dbAsset = assets.firstOrNull {
                val aName = it.jsonObject["name"]?.jsonPrimitive?.content ?: ""
                aName == "creditdb.zip" || aName == AppConfig.DB_ASSET_NAME
            }?.jsonObject

            if (dbAsset == null) {
                onProgress(100, "最新リリース ($tagName) 内に creditdb.zip または creditdb.db が見つかりませんでした。")
                return@withContext
            }

            val assetFileName = dbAsset["name"]?.jsonPrimitive?.content ?: AppConfig.DB_ASSET_NAME
            val downloadUrl = dbAsset["browser_download_url"]?.jsonPrimitive?.content
            val assetSize = dbAsset["size"]?.jsonPrimitive?.long ?: 0L

            if (downloadUrl.isNullOrBlank()) {
                onProgress(100, "ダウンロードURLの取得に失敗しました。")
                return@withContext
            }

            onProgress(10, "最新データベース ($tagName: $assetFileName) をダウンロード準備中...")

            // ダウンロード用テンポラリファイル
            val tempDbFile = File(context.cacheDir, if (assetFileName.endsWith(".zip")) "creditdb_download.zip" else "creditdb_download.tmp")
            if (tempDbFile.exists()) tempDbFile.delete()

            // 30x リダイレクト対応ダウンロード (GitHub Releases -> AWS S3 等)
            var currentDownloadUrl = downloadUrl
            var downloadConn: HttpURLConnection
            var redirects = 0
            while (true) {
                downloadConn = (URL(currentDownloadUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "CreditDB-Android-App")
                    instanceFollowRedirects = true
                }
                val code = downloadConn.responseCode
                if (code in 300..399) {
                    val newLoc = downloadConn.getHeaderField("Location")
                    if (newLoc != null && redirects < 5) {
                        currentDownloadUrl = newLoc
                        redirects++
                        continue
                    }
                }
                break
            }

            val totalBytes = if (downloadConn.contentLengthLong > 0) downloadConn.contentLengthLong else assetSize
            val inputStream = downloadConn.inputStream
            val outputStream = FileOutputStream(tempDbFile)

            val buffer = ByteArray(32768)
            var bytesRead: Int
            var totalRead = 0L

            inputStream.use { input ->
                outputStream.use { output ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            val percent = 10 + ((totalRead.toDouble() / totalBytes) * 75).toInt().coerceIn(0, 75)
                            val mbRead = totalRead / (1024 * 1024.0)
                            val mbTotal = totalBytes / (1024 * 1024.0)
                            onProgress(percent, String.format("ダウンロード中: %.1f MB / %.1f MB", mbRead, mbTotal))
                        }
                    }
                    output.flush()
                }
            }

            onProgress(88, "SQLite 整合性検証中...")
            delay(200)

            val success = dbHelper.replaceDatabase(tempDbFile)
            tempDbFile.delete()

            if (success) {
                cachedSummary = null // キャッシュ破棄
                onProgress(95, "新しいデータベースへ再接続中...")
                val newSummary = getSummary()
                delay(200)
                onProgress(100, "更新完了！全 ${newSummary.totalWorks} 作品, ${newSummary.totalStaff} 名スタッフ (${tagName})")
            } else {
                onProgress(100, "更新失敗: ダウンロードしたデータベースの整合性検証に失敗しました。")
            }
        } catch (e: Exception) {
            onProgress(100, "通信エラー: ${e.localizedMessage ?: "接続できませんでした"}")
        }
    }

    /**
     * 手動データベース更新（旧互換・同期呼び出し）
     */
    suspend fun simulateDatabaseUpdate(
        onProgress: (percent: Int, statusText: String) -> Unit
    ) {
        syncDatabaseFromRemote(onProgress)
    }

    private fun parseStaffCredits(jsonStr: String): Map<String, List<StaffCredit>> {
        val result = mutableMapOf<String, List<StaffCredit>>()
        try {
            val root = json.parseToJsonElement(jsonStr) as? JsonObject ?: return result
            for ((roleKey, el) in root) {
                if (el is JsonArray) {
                    val credits = el.map { StaffCredit.fromJsonElement(it) }
                    result[roleKey] = credits
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun normalizeRelation(rel: String?): String {
        return when (rel) {
            "主角", "主役" -> "主役"
            "配角", "脇役" -> "脇役"
            "客串", "端役" -> "端役"
            else -> rel?.takeIf { it.isNotBlank() } ?: "脇役"
        }
    }

    private fun parseCharacters(jsonStr: String): List<CharacterCast> {
        val result = mutableListOf<CharacterCast>()
        try {
            val root = json.parseToJsonElement(jsonStr) as? JsonArray ?: return result
            for (el in root) {
                if (el is JsonObject) {
                    val cName = el["character_name"]?.jsonPrimitive?.content ?: ""
                    val rawRel = el["relation"]?.jsonPrimitive?.content
                    val rel = normalizeRelation(rawRel)
                    val aName = el["actor_name"]?.jsonPrimitive?.content ?: ""
                    val rt = el["rt"]?.jsonPrimitive?.content ?: "B"
                    val ct = el["ct"]?.jsonPrimitive?.content ?: "B"
                    result.add(
                        CharacterCast(
                            characterName = cName,
                            relation = rel,
                            actorName = aName,
                            ratingTier = rt,
                            cumulativeTier = ct
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun buildStaffSummary(staffJsonStr: String, charJsonStr: String): String {
        try {
            val parts = mutableListOf<String>()
            val root = json.parseToJsonElement(staffJsonStr) as? JsonObject
            if (root != null) {
                val dir = root["director"] as? JsonArray
                if (dir != null && dir.isNotEmpty()) {
                    val dName = (dir[0] as? JsonObject)?.get("name")?.jsonPrimitive?.content
                    if (!dName.isNullOrBlank()) parts.add("監督: $dName")
                }
                val comp = root["series_comp"] as? JsonArray
                if (comp != null && comp.isNotEmpty()) {
                    val cName = (comp[0] as? JsonObject)?.get("name")?.jsonPrimitive?.content
                    if (!cName.isNullOrBlank()) parts.add("構成: $cName")
                }
            }
            val chars = json.parseToJsonElement(charJsonStr) as? JsonArray
            if (chars != null && chars.isNotEmpty()) {
                val firstChar = chars[0] as? JsonObject
                val aName = firstChar?.get("actor_name")?.jsonPrimitive?.content
                if (!aName.isNullOrBlank()) {
                    parts.add("CV: $aName")
                }
            }
            return if (parts.isNotEmpty()) parts.joinToString(" / ") else "主要スタッフ登録あり"
        } catch (e: Exception) {
            return ""
        }
    }

    /**
     * スタジオデータのクリーンアップ & 正規化
     * Bangumi / AniList 公式データおよび既知の表記揺れを吸収し、
     * 正確な制作スタジオ名を正規化して抽出する。
     */
    fun cleanStudio(rawList: List<StaffCredit>?): String? {
        if (rawList.isNullOrEmpty()) return null
        val names = rawList.map { it.name.trim() }.filter { it.isNotBlank() }
        if (names.isEmpty()) return null

        val joinedAll = names.joinToString(" ")
        val joinedUpper = joinedAll.uppercase()

        // スタジオジブリ
        if (joinedUpper.contains("GHIBLI") || joinedAll.contains("ジブリ") || joinedAll.contains("吉卜力")) {
            return "スタジオジブリ"
        }

        // MADHOUSE
        if (names.any { it.equals("MAD", ignoreCase = true) } ||
            joinedUpper.contains("MADHOUSE") || joinedAll.contains("マッドハウス")
        ) {
            return "MADHOUSE"
        }

        // スタジオ地図
        if (joinedUpper.contains("CHIZU") || joinedAll.contains("スタジオ地図")) {
            return "スタジオ地図"
        }

        if ((names.any { it.equals("WIT", ignoreCase = true) } && names.any { it.equals("STUDIO", ignoreCase = true) }) ||
            joinedUpper.startsWith("WIT")
        ) {
            return "WIT STUDIO"
        }
        if ((names.any { it.equals("WHITE", ignoreCase = true) } && names.any { it.equals("FOX", ignoreCase = true) }) ||
            joinedUpper.contains("WHITE FOX")
        ) {
            return "WHITE FOX"
        }
        if ((names.any { it.equals("LIDEN", ignoreCase = true) } && names.any { it.equals("FILMS", ignoreCase = true) }) ||
            joinedAll.contains("ライデンフィルム") || joinedUpper.contains("LIDENFILMS")
        ) {
            return "ライデンフィルム"
        }
        if (joinedUpper.contains("MAPPA")) return "MAPPA"
        if (joinedUpper.contains("UFOTABLE") || joinedAll.contains("ユーフォーテーブル")) return "ufotable"
        if (joinedUpper.contains("BONES") || joinedAll.contains("ボンズ")) return "ボンズ"
        if (joinedUpper.contains("CLOVERWORKS") || joinedAll.contains("クローバーワークス")) return "CloverWorks"
        if (joinedUpper.contains("KYOTO") || joinedAll.contains("京都") || joinedAll.contains("京アニ")) return "京都アニメーション"
        if (joinedUpper.contains("SHAFT") || joinedAll.contains("シャフト")) return "シャフト"
        if (joinedUpper.contains("SUNRISE") || joinedAll.contains("サンライズ")) return "サンライズ"
        if (joinedUpper.contains("TRIGGER") || joinedAll.contains("トリガー")) return "TRIGGER"
        if (joinedUpper.contains("A-1") || joinedUpper.contains("A1")) return "A-1 Pictures"
        if (joinedUpper.contains("J.C.STAFF") || joinedUpper.contains("JCSTAFF") || joinedUpper.contains("J.C.")) return "J.C.STAFF"
        if (joinedUpper.contains("P.A.WORKS") || joinedUpper.contains("PAWORKS") || joinedUpper.contains("P.A.")) return "P.A.WORKS"
        if (joinedUpper.contains("TOEI") || joinedAll.contains("東映")) return "東映アニメーション"
        if (joinedUpper.contains("PIERROT") || joinedAll.contains("ぴえろ")) return "スタジオぴえろ"
        if (joinedUpper.contains("TMS") || joinedAll.contains("トムス")) return "トムス・エンタテインメント"
        if (joinedUpper.contains("DOGA KOBO") || joinedAll.contains("動画工房")) return "動画工房"
        if (joinedUpper.contains("SILVER LINK") || joinedAll.contains("シルバーリンク")) return "SILVER LINK."
        if (joinedUpper.contains("KINEMA CITRUS") || joinedAll.contains("キネマシトラス")) return "キネマシトラス"
        if (joinedUpper.contains("PRODUCTION I.G") || joinedUpper.contains("PRODUCTION IG") || joinedAll.contains("プロダクションI.G")) return "Production I.G"
        if (joinedUpper.contains("SCIENCE SARU") || joinedAll.contains("サイエンスSARU")) return "サイエンスSARU"
        if (joinedUpper.contains("STUDIO DEEN") || joinedAll.contains("スタジオディーン") || joinedUpper.contains("DEEN")) return "スタジオディーン"
        if (joinedUpper.contains("OLM")) return "OLM"
        if (joinedUpper.contains("AIC")) return "AIC"
        if (joinedUpper.contains("GONZO")) return "GONZO"
        if (joinedUpper.contains("XEBEC") || joinedAll.contains("ジーベック")) return "XEBEC"
        if (joinedUpper.contains("TROYCA") || joinedAll.contains("トロイカ")) return "TROYCA"
        if (joinedUpper.contains("LERCHE") || joinedAll.contains("ラルケ")) return "Lerche"
        if (joinedUpper.contains("COMIX WAVE") || joinedAll.contains("コミックス・ウェーブ")) return "コミックス・ウェーブ・フィルム"
        if (joinedUpper.contains("FEEL") || joinedAll.contains("feel.")) return "feel."
        if (joinedUpper.contains("TATSUNOKO") || joinedAll.contains("タツノコ")) return "タツノコプロ"
        if (joinedUpper.contains("GAINAX") || joinedAll.contains("ガイナックス")) return "GAINAX"
        if (joinedUpper.contains("DAVID") || joinedAll.contains("デイヴィッドプロダクション")) return "david production"
        if (joinedAll.contains("スタジオバインド") || joinedUpper.contains("STUDIO BIND")) return "スタジオバインド"
        if (joinedAll.contains("スタジオヴォルン") || joinedUpper.contains("VOLN")) return "スタジオヴォルン"
        if (joinedAll.contains("Nexus") || joinedUpper.contains("NEXUS")) return "Nexus"
        if (joinedAll.contains("C-Station") || joinedUpper.contains("C STATION")) return "C-Station"
        if (joinedAll.contains("テレコム")) return "テレコム・アニメーションフィルム"
        if (joinedAll.contains("シンエイ動画")) return "シンエイ動画"
        if (joinedAll.contains("日本アニメーション")) return "日本アニメーション"

        val noiseWords = setOf(
            "振付", "人名", "配角", "Triple", "ON", "PRODUCTION", "Production", "Kim", "Pictures",
            "フジテレビ", "テレビ朝日", "TBS", "日本テレビ", "テレビ東京", "NHK", "TOKYO MX", "MBS", "BS11", "AT-X",
            "松倉友二", "大月俊倫", "丸山正雄", "植田益朗", "川村元気", "読売広告社", "電通", "博報堂", "アニプレックス"
        )
        val filtered = names.filter { it !in noiseWords && it.length > 1 && !it.startsWith("第") }
        if (filtered.isEmpty()) return null

        val candidate = filtered[0]
        val badKeywords = listOf("振付", "音響", "監督", "原画", "デザイン", "編集", "美術", "制作進行", "テレビ", "放送")
        if (badKeywords.any { candidate.contains(it) }) return null

        return candidate
    }

    /**
     * 特定スタジオの制作作品一覧（制作年順 / 最新順）を取得
     * SQLite studio_works インデックステーブルから 0.1ms で即時取得
     */
    suspend fun getStudioWorks(studioName: String): List<StudioWorkItem> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT work_id, title, year, deviation_score, tier
            FROM studio_works
            WHERE studio_name = ?
            ORDER BY year DESC, deviation_score DESC
            """.trimIndent(),
            arrayOf(studioName)
        )
        val list = mutableListOf<StudioWorkItem>()
        while (cursor.moveToNext()) {
            list.add(
                StudioWorkItem(
                    workId = cursor.getString(0),
                    title = cursor.getString(1),
                    year = cursor.getInt(2),
                    deviationScore = cursor.getDouble(3),
                    tier = cursor.getString(4)
                )
            )
        }
        cursor.close()
        list
    }
}
