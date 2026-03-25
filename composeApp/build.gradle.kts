import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    androidTarget {
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }
    }

    jvm("desktop")

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(compose.preview)
                implementation(libs.androidx.activity.compose)

                // --- Koin e Coroutines Android ---
                implementation("io.insert-koin:koin-android:3.5.6")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0")

                // --- Google Drive, AndroidHttp e Extensions ---
                implementation("com.google.android.gms:play-services-auth:20.7.0")
                implementation("com.google.api-client:google-api-client-android:1.33.0")
                implementation("com.google.http-client:google-http-client-android:1.43.3")
                implementation("com.google.http-client:google-http-client-gson:1.43.3")
                implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0")
            }
        }

        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)

                // Ícones estendidos
                implementation(compose.materialIconsExtended)

                // Navegação
                implementation(libs.androidx.navigation.compose)

                // ViewModel
                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.runtime.compose)

                // Koin
                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)

                // Room
                implementation(libs.androidx.room.runtime)
                implementation(libs.sqlite.bundled)

                // Datastore e DateTime
                implementation(libs.androidx.datastore.preferences)
                implementation(libs.kotlinx.datetime)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)

                // Bibliotecas Java puras para o Google Drive no Desktop
                implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
                implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0")
                implementation("com.google.api-client:google-api-client:2.2.0")
            }
        }
    }
}

android {
    namespace = "com.zanini.snowwallet"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/composeResources")

    defaultConfig {
        applicationId = "com.zanini.snowwallet"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    // --- CORREÇÃO DO ERRO DE META-INF (Ficheiros duplicados) ---
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    dependencies {
        debugImplementation(compose.uiTooling)
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Processador de anotações do Room (KSP)
    add("kspCommonMainMetadata", libs.androidx.room.compiler)
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspDesktop", libs.androidx.room.compiler)
}

compose.desktop {
    application {
        mainClass = "com.zanini.snowwallet.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "SnowWallet"
            packageVersion = "1.0.0"

            // --- A SUPER CORREÇÃO DOS MÓDULOS (Para o Google Drive funcionar no .EXE) ---
            modules(
                "jdk.unsupported",
                "java.sql",
                "jdk.crypto.ec",     // Criptografia SSL obrigatória para o Google
                "jdk.httpserver",    // Mini-servidor para capturar o login no navegador
                "java.naming",       // Resolução de DNS (Internet)
                "java.management",
                "java.desktop",      // Para conseguir forçar a abertura do Chrome/Edge
                "jdk.security.auth"
            )

            windows {
                menu = true
                // --- CORREÇÃO DO ÍCONE ---
                iconFile.set(project.file("src/desktopMain/resources/icon.ico"))
            }
        }
    }
}