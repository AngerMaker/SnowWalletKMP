package com.zanini.snowwallet.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.zanini.snowwallet.data.local.AppDatabase
import com.zanini.snowwallet.data.repository.BackupRepository
import com.zanini.snowwallet.data.repository.DesktopDriveBackupRepository
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

actual val platformModule = module {
    // 1. Criação do Banco de Dados usando a Factory
    single<AppDatabase> {
        DatabaseFactory().create()
            .setDriver(BundledSQLiteDriver()) // Driver SQLite para Desktop
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    // 2. Repositório de Backup
    single<BackupRepository> {
        DesktopDriveBackupRepository(database = get())
    }

    // 3. Notification Scheduler (Desktop Stub)
    single<com.zanini.snowwallet.util.NotificationScheduler> {
        com.zanini.snowwallet.util.DesktopNotificationScheduler()
    }
}