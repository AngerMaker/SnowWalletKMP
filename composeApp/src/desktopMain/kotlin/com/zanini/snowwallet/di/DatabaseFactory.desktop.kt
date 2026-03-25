package com.zanini.snowwallet.di

import androidx.room.Room
import androidx.room.RoomDatabase
import com.zanini.snowwallet.data.local.AppDatabase
import java.io.File

// A implementação deve ser uma classe, pois o commonMain define 'expect class DatabaseFactory'
actual class DatabaseFactory {
    actual fun create(): RoomDatabase.Builder<AppDatabase> {
        // 1. Pega a pasta do usuário (C:\Users\Nome)
        val userHome = System.getProperty("user.home")

        // 2. Define o caminho: Documents/SnowWallet/snowwallet.db
        val dbFile = File(userHome, "Documents/SnowWallet/snowwallet.db")

        // 3. Cria a pasta se não existir
        val parent = dbFile.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }

        // Retorna o Builder do Room
        return Room.databaseBuilder<AppDatabase>(
            name = dbFile.absolutePath
        )
    }
}