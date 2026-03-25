package com.zanini.snowwallet.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface BackupRepository {
    val statusBackup: Flow<String>

    // Novo Flow para saber quem está logado
    val emailConectado: Flow<String?>
        get() = MutableStateFlow(null) // Implementação padrão para Desktop/Web não quebrarem

    suspend fun realizarBackup()
    suspend fun restaurarBackup()

    // Nova função dedicada apenas para login
    suspend fun fazerLogin() {}

    fun setContext(context: Any?)
}