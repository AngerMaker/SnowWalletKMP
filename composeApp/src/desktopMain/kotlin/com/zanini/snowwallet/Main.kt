package com.zanini.snowwallet

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.res.painterResource
import com.zanini.snowwallet.di.appModule
import com.zanini.snowwallet.di.platformModule
import org.koin.core.context.startKoin
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import org.koin.dsl.module
import java.io.File

fun main() = application {
    // Inicializa o Koin apenas se ainda não foi iniciado
    try {
        initKoin()
    } catch (e: Exception) {
        // Ignora erro se já estiver rodando
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Snow Wallet",
        // Certifique-se de que 'icon.png' está na pasta 'src/desktopMain/resources'
        icon = painterResource("icon.png")
    ) {
        App()
    }
}

private var koinStarted = false

fun initKoin() {
    if (koinStarted) return
    koinStarted = true

    startKoin {
        modules(
            appModule,
            platformModule,
            desktopDataStoreModule
        )
    }
}

val desktopDataStoreModule = module {
    single {
        val userHome = System.getProperty("user.home")
        // Salva as preferências na pasta do usuário
        val file = File(userHome, "SnowWallet/user_preferences.preferences_pb")
        file.parentFile?.mkdirs()
        PreferenceDataStoreFactory.create { file }
    }
}