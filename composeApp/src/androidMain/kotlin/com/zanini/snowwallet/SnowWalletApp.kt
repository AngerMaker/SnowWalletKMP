package com.zanini.snowwallet

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.zanini.snowwallet.di.appModule
import com.zanini.snowwallet.di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module

// Extensão para criar o DataStore (Singleton)
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SnowWalletApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            // Log do Koin para ajudar no debug
            androidLogger()

            // Contexto do Android (Necessário para o Banco de Dados e Resources)
            androidContext(this@SnowWalletApp)

            // Módulos
            modules(
                appModule,       // Módulo comum (ViewModels, UseCases, Repositories)
                platformModule,  // Módulo de plataforma (Banco de dados, Backup Drive)
                module {
                    // Módulo específico do App para injetar o DataStore
                    single { this@SnowWalletApp.dataStore }
                }
            )
        }
    }
}