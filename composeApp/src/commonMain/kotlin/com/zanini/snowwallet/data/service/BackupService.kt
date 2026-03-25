package com.zanini.snowwallet.data.service

interface BackupService {
    // Retorna uma mensagem de sucesso ou erro
    suspend fun exportDatabase(): String
}