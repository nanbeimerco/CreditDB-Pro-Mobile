package com.creditdb.pro.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream

class DatabaseHelper private constructor(private val context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        const val DB_NAME = "creditdb.db"
        const val DB_VERSION = 9
        private const val BUFFER_SIZE = 65536 // 64 KB

        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context.applicationContext).also {
                    it.ensureDatabaseExists()
                    instance = it
                }
            }
        }
    }

    @Synchronized
    fun ensureDatabaseExists() {
        val dbPath = context.getDatabasePath(DB_NAME)
        var needsRefresh = !dbPath.exists() || dbPath.length() < 1024 * 1024 // 1MB 未満は破損とみなす

        if (!needsRefresh) {
            // テーブル整合性チェック: studios テーブル、作品数 20,000 件以上、first_year カラム存在確認、DB v2.1.0 確認
            try {
                SQLiteDatabase.openDatabase(dbPath.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                    val cursor = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name='studios'", null)
                    val hasStudios = cursor.moveToFirst()
                    cursor.close()

                    var totalWorks = 0
                    var dbVersion = ""
                    val countCursor = db.rawQuery("SELECT total_works, version FROM summary LIMIT 1", null)
                    if (countCursor.moveToFirst()) {
                        totalWorks = countCursor.getInt(0)
                        dbVersion = if (countCursor.columnCount > 1) countCursor.getString(1) ?: "" else ""
                    }
                    countCursor.close()

                    var hasFirstYear = false
                    val colCursor = db.rawQuery("PRAGMA table_info(leaderboards)", null)
                    while (colCursor.moveToNext()) {
                        if (colCursor.getString(1) == "first_year") hasFirstYear = true
                    }
                    colCursor.close()

                    // DB v2.1.0 (新世代/年代ソート、再生成プロファイル、AniList連携完了版) が必要
                    if (!hasStudios || totalWorks < 20000 || !hasFirstYear || dbVersion != "2.1.0") {
                        needsRefresh = true
                    }
                }
            } catch (e: Exception) {
                // SQLite破損例外等の場合はリフレッシュ
                needsRefresh = true
            }
        }

        if (needsRefresh) {
            close()
            dbPath.parentFile?.mkdirs()
            copyDatabaseFromAssets(dbPath)
        }
    }

    /**
     * ダウンロードされた新しいDBファイル（またはZIP）で既存DBを安全にアトミック置換する
     */
    fun replaceDatabase(newDbFile: File): Boolean {
        var dbToApply = newDbFile
        val tempUnpacked = File(context.cacheDir, "unpacked_temp.db")
        if (tempUnpacked.exists()) tempUnpacked.delete()

        // ZIP形式の場合は自動解凍
        if (newDbFile.name.endsWith(".zip", ignoreCase = true)) {
            try {
                FileInputStream(newDbFile).use { fis ->
                    java.io.BufferedInputStream(fis, BUFFER_SIZE).use { bis ->
                        ZipInputStream(bis).use { zip ->
                            var entry = zip.nextEntry
                            while (entry != null) {
                                if (entry.name.endsWith(".db")) {
                                    FileOutputStream(tempUnpacked).use { fos ->
                                        java.io.BufferedOutputStream(fos, BUFFER_SIZE).use { bos ->
                                            zip.copyTo(bos, BUFFER_SIZE)
                                            bos.flush()
                                        }
                                    }
                                    break
                                }
                                entry = zip.nextEntry
                            }
                        }
                    }
                }
                if (tempUnpacked.exists() && tempUnpacked.length() > 1024 * 1024) {
                    dbToApply = tempUnpacked
                } else {
                    return false
                }
            } catch (e: Exception) {
                return false
            }
        }

        // 1. SQLite整合性チェック (PRAGMA integrity_check)
        try {
            val tempDb = SQLiteDatabase.openDatabase(dbToApply.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val cursor = tempDb.rawQuery("PRAGMA integrity_check", null)
            val isOk = cursor.moveToFirst() && cursor.getString(0).equals("ok", ignoreCase = true)
            cursor.close()
            tempDb.close()
            if (!isOk) return false
        } catch (e: Exception) {
            return false
        }

        // 2. 既存のSQLite接続を閉じる
        close()

        // 3. アトミックスワップ（ロールバック退避付き）
        val dbPath = context.getDatabasePath(DB_NAME)
        val backupFile = File(dbPath.parentFile, "$DB_NAME.bak")
        if (backupFile.exists()) backupFile.delete()

        try {
            if (dbPath.exists()) {
                if (!dbPath.renameTo(backupFile)) {
                    dbPath.copyTo(backupFile, overwrite = true)
                    dbPath.delete()
                }
            }
            if (!dbToApply.renameTo(dbPath)) {
                dbToApply.copyTo(dbPath, overwrite = true)
                dbToApply.delete()
            }
            if (backupFile.exists()) backupFile.delete()
            if (tempUnpacked.exists()) tempUnpacked.delete()
            return true
        } catch (e: Exception) {
            if (backupFile.exists()) {
                backupFile.renameTo(dbPath)
            }
            if (tempUnpacked.exists()) tempUnpacked.delete()
            return false
        }
    }

    fun forceRebuildDatabase() {
        close()
        val dbPath = context.getDatabasePath(DB_NAME)
        if (dbPath.exists()) {
            dbPath.delete()
        }
        ensureDatabaseExists()
    }

    private fun copyDatabaseFromAssets(destFile: File) {
        val tempFile = File(destFile.parentFile, "$DB_NAME.unpack.tmp")
        if (tempFile.exists()) tempFile.delete()

        // 1. まず解凍済み creditdb.db が assets に存在し、かつ 10MB 以上の有効サイズか確認 (空ファイル除外)
        var rawCopied = false
        try {
            val assetList = context.assets.list("") ?: emptyArray()
            if (assetList.contains(DB_NAME)) {
                val fd = try { context.assets.openFd(DB_NAME) } catch (e: Exception) { null }
                val length = fd?.length ?: -1L
                fd?.close()
                // AssetFileDescriptor が取れない場合も考慮しストリームを開いてサイズ確認
                if (length > 10 * 1024 * 1024L || length == -1L) {
                    context.assets.open(DB_NAME).use { input ->
                        if (input.available() > 10 * 1024 * 1024) {
                            FileOutputStream(tempFile).use { fos ->
                                java.io.BufferedOutputStream(fos, BUFFER_SIZE).use { bos ->
                                    input.copyTo(bos, BUFFER_SIZE)
                                    bos.flush()
                                }
                            }
                            if (tempFile.length() > 50 * 1024 * 1024L && isValidSqlite(tempFile)) {
                                rawCopied = true
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            rawCopied = false
            if (tempFile.exists()) tempFile.delete()
        }

        // 2. assets/creditdb.zip から一時ファイルへ展開 (バッファサイズ 64KB)
        if (!rawCopied) {
            try {
                context.assets.open("creditdb.zip").use { input ->
                    java.io.BufferedInputStream(input, BUFFER_SIZE).use { bis ->
                        ZipInputStream(bis).use { zip ->
                            var entry = zip.nextEntry
                            var found = false
                            while (entry != null) {
                                if (entry.name == DB_NAME || entry.name.endsWith(".db")) {
                                    FileOutputStream(tempFile).use { fos ->
                                        java.io.BufferedOutputStream(fos, BUFFER_SIZE).use { bos ->
                                            zip.copyTo(bos, BUFFER_SIZE)
                                            bos.flush()
                                        }
                                    }
                                    found = true
                                    break
                                }
                                entry = zip.nextEntry
                            }
                            if (!found) {
                                throw IOException("creditdb.db not found inside creditdb.zip")
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                if (tempFile.exists()) tempFile.delete()
                throw RuntimeException("Failed to copy or unpack database from assets (neither valid $DB_NAME nor creditdb.zip usable)", e)
            }
        }

        // 3. 展開後の一時ファイルの検証 (サイズ > 50MB かつ SQLite ヘッダー)
        if (!isValidSqlite(tempFile)) {
            if (tempFile.exists()) tempFile.delete()
            throw RuntimeException("Unpacked database failed SQLite header validation")
        }

        // 4. アトミック置換
        if (destFile.exists()) {
            destFile.delete()
        }
        if (!tempFile.renameTo(destFile)) {
            tempFile.copyTo(destFile, overwrite = true)
            tempFile.delete()
        }
    }

    private fun isValidSqlite(file: File): Boolean {
        if (!file.exists() || file.length() < 100) return false
        return try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(16)
                val read = fis.read(header)
                read == 16 && String(header, 0, 15, Charsets.US_ASCII) == "SQLite format 3"
            }
        } catch (e: Exception) {
            false
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // Assets から直接コピーするため、新規テーブル作成は不要
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (newVersion > oldVersion) {
            val dbPath = context.getDatabasePath(DB_NAME)
            try {
                copyDatabaseFromAssets(dbPath)
            } catch (e: Exception) {
                // copy fallback
            }
        }
    }

}
