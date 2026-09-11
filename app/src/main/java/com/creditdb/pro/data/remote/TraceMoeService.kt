package com.creditdb.pro.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class TraceMoeItem(
    val anilist: Int,
    val filename: String,
    val episode: String?,
    val from: Double,
    val to: Double,
    val at: Double,
    val similarity: Double,
    val video: String,
    val image: String
)

object TraceMoeService {
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    suspend fun searchScene(context: Context, uri: Uri): List<TraceMoeItem> = withContext(Dispatchers.IO) {
        val bytes = compressImageUri(context, uri)
        searchSceneBytes(bytes)
    }

    suspend fun searchSceneBytes(imageBytes: ByteArray): List<TraceMoeItem> = withContext(Dispatchers.IO) {
        val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaType())
        val request = Request.Builder()
            .url("https://api.trace.moe/search?cutBorders=true")
            .header("User-Agent", "CreditDB-Android/1.0 (Linux; Android)")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val code = response.code
            if (code == 429) {
                throw IllegalStateException("API利用制限に達しました。しばらく待ってから再試行してください。(Rate limit reached)")
            }
            throw IllegalStateException("trace.moe API error: HTTP $code")
        }

        val bodyString = response.body?.string() ?: throw IllegalStateException("Empty response from trace.moe")
        val json = JSONObject(bodyString)
        if (json.has("error") && json.optString("error").isNotBlank()) {
            throw IllegalStateException(json.getString("error"))
        }

        val results = mutableListOf<TraceMoeItem>()
        val jsonArray = json.optJSONArray("result") ?: return@withContext emptyList()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val anilist = obj.optInt("anilist", 0)
            val filename = obj.optString("filename", "")
            val epVal = if (obj.isNull("episode")) null else obj.opt("episode")?.toString()
            val from = obj.optDouble("from", 0.0)
            val to = obj.optDouble("to", 0.0)
            val at = obj.optDouble("at", 0.0)
            val similarity = obj.optDouble("similarity", 0.0)
            val video = obj.optString("video", "")
            val image = obj.optString("image", "")

            results.add(
                TraceMoeItem(
                    anilist = anilist,
                    filename = filename,
                    episode = epVal,
                    from = from,
                    to = to,
                    at = at,
                    similarity = similarity,
                    video = video,
                    image = image
                )
            )
        }
        results
    }

    private fun compressImageUri(context: Context, uri: Uri): ByteArray {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Failed to open image URI")

        val tempBytes = inputStream.use { it.readBytes() }

        // First decode bounds
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(tempBytes, 0, tempBytes.size, options)

        var sampleSize = 1
        val maxDim = 1280
        val maxOriginal = maxOf(options.outWidth, options.outHeight)
        while (maxOriginal / (sampleSize * 2) >= maxDim) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        val originalBitmap = BitmapFactory.decodeByteArray(tempBytes, 0, tempBytes.size, decodeOptions)
            ?: throw IllegalArgumentException("Failed to decode image bitmap")

        val width = originalBitmap.width
        val height = originalBitmap.height
        val scaledBitmap = if (width > maxDim || height > maxDim) {
            val ratio = if (width > height) maxDim.toFloat() / width else maxDim.toFloat() / height
            val targetW = (width * ratio).toInt()
            val targetH = (height * ratio).toInt()
            Bitmap.createScaledBitmap(originalBitmap, targetW, targetH, true)
        } else {
            originalBitmap
        }

        val bos = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, bos)
        return bos.toByteArray()
    }

    fun compressBitmap(bitmap: Bitmap): ByteArray {
        val maxDim = 1280
        val width = bitmap.width
        val height = bitmap.height
        val scaledBitmap = if (width > maxDim || height > maxDim) {
            val ratio = if (width > height) maxDim.toFloat() / width else maxDim.toFloat() / height
            val targetW = (width * ratio).toInt()
            val targetH = (height * ratio).toInt()
            Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
        } else {
            bitmap
        }
        val bos = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, bos)
        return bos.toByteArray()
    }

    fun formatTimestamp(seconds: Double): String {
        if (seconds.isNaN() || seconds < 0) return "00:00"
        val totalSec = seconds.toInt()
        val hrs = totalSec / 3600
        val mins = (totalSec % 3600) / 60
        val secs = totalSec % 60
        return if (hrs > 0) {
            String.format("%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }
}
