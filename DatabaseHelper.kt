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
        const val DB_VERSION = 5

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

    fun ensureDatabaseExists() {
        val dbPath = context.getDatabasePath(DB_NAME)
        if (!dbPath.exists()) {
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
                    ZipInputStream(fis).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            if (entry.name.endsWith(".db")) {
                                FileOutputStream(tempUnpacked).use { fos ->
                                    zip.copyTo(fos)
                                }
                                break
                            }
                            entry = zip.nextEntry
                        }
                    }
                }
                if (tempUnpacked.exists() && tempUnpacked.length() > 0) {
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
        // 1. まず解凍済み creditdb.db が assets に存在するか確認
        val hasRawDb = try {
            context.assets.list("")?.contains(DB_NAME) == true
        } catch (e: Exception) {
            false
        }

        if (hasRawDb) {
            try {
                context.assets.open(DB_NAME).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                return
            } catch (e: IOException) {
                // zip展開へフォールバック
            }
        }

        // 2. assets/creditdb.zip から展開
        try {
            context.assets.open("creditdb.zip").use { input ->
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (entry.name == DB_NAME || entry.name.endsWith(".db")) {
                            FileOutputStream(destFile).use { output ->
                                zip.copyTo(output)
                            }
                            return
                        }
                        entry = zip.nextEntry
                    }
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("Failed to copy or unpack database from assets (neither $DB_NAME nor creditdb.zip found)", e)
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // Assets から直接コピーするため、新規テーブル作成は不要
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (newVersion > oldVersion) {
            val dbPath = context.getDatabasePath(DB_NAME)
            if (dbPath.exists()) {
                dbPath.delete()
            }
            ensureDatabaseExists()
        }
    }
}
