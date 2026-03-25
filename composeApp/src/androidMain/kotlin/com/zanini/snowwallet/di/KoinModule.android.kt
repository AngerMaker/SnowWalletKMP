package com.zanini.snowwallet.di

import com.zanini.snowwallet.data.local.AppDatabase
import com.zanini.snowwallet.data.repository.AndroidDriveBackupRepository
import com.zanini.snowwallet.data.repository.BackupRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    // 1. Banco de Dados (Android)
    single<AppDatabase> {
        DatabaseFactory(androidContext()).create()
            .build()
    }

    // 2. Repositório de Backup
    single<BackupRepository> {
        AndroidDriveBackupRepository(
            database = get(),
            context = androidContext()
        )
    }

    // 3. Notification Scheduler (Android Base)
    single<com.zanini.snowwallet.util.NotificationScheduler> {
        com.zanini.snowwallet.util.AndroidNotificationScheduler(
            context = androidContext()
        )
    }
}