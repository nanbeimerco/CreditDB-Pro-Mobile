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
        const val DB_VERSION = 4

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
