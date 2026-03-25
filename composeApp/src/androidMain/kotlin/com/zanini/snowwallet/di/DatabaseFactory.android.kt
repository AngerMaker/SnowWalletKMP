package com.zanini.snowwallet.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zanini.snowwallet.data.local.AppDatabase

actual class DatabaseFactory(private val context: Context) {
    actual fun create(): RoomDatabase.Builder<AppDatabase> {
        val dbFile = context.getDatabasePath("snowwallet.db")
        return Room.databaseBuilder<AppDatabase>(
            context = context.applicationContext,
            name = dbFile.absolutePath
        )
    }
}