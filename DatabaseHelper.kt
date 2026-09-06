package com.creditdb.pro.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

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
     * ダウンロードされた新しいDBファイルで既存DBを安全にアトミック置換する
     */
    fun replaceDatabase(newDbFile: File): Boolean {
        // 1. SQLite整合性チェック (PRAGMA integrity_check)
        try {
            val tempDb = SQLiteDatabase.openDatabase(newDbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
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
            if (!newDbFile.renameTo(dbPath)) {
                newDbFile.copyTo(dbPath, overwrite = true)
                newDbFile.delete()
            }
            if (backupFile.exists()) backupFile.delete()
            return true
        } catch (e: Exception) {
            if (backupFile.exists()) {
                backupFile.renameTo(dbPath)
            }
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
        try {
            context.assets.open(DB_NAME).use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(8192)
                    var length: Int
                    while (input.read(buffer).also { length = it } > 0) {
                        output.write(buffer, 0, length)
                    }
                    output.flush()
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("Failed to copy database from assets", e)
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
