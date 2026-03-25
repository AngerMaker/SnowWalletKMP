plugins {
    // Apenas declaramos os plugins aqui, com 'apply false' para não aplicar na raiz
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}

// Tarefa opcional de limpeza
tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}