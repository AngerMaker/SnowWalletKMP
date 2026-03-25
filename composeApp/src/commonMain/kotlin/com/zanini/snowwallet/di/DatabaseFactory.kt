package com.zanini.snowwallet.di

import androidx.room.RoomDatabase
import com.zanini.snowwallet.data.local.AppDatabase

expect class DatabaseFactory {
    fun create(): RoomDatabase.Builder<AppDatabase>
}