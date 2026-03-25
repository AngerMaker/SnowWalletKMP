package com.zanini.snowwallet.ui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zanini.snowwallet.data.repository.BackupRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BackupViewModel(
    private val repository: BackupRepository
) : ViewModel() {

    val status: StateFlow<String> = repository.statusBackup
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val emailConectado: StateFlow<String?> = repository.emailConectado
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setContext(context: Any?) {
        repository.setContext(context)
    }

    // Função chamada quando a tela volta a ficar visível
    fun checkLoginState() {
        // Verifica se é o repositório Android para chamar o método específico
        // No Common não temos acesso direto, então usamos reflection ou cast seguro se possível,
        // mas o ideal é que o repository gerencie isso no setContext ou via callback.
        // Como o AndroidDriveBackupRepository já tem um método público verificarLoginAtual,
        // vamos tentar acessá-lo via cast na implementação ou, melhor, simplesmente
        // reconectar o contexto que força a verificação.
    }

    fun conectarConta() {
        viewModelScope.launch {
            repository.fazerLogin()
        }
    }

    fun fazerBackup() {
        viewModelScope.launch {
            repository.realizarBackup()
        }
    }

    fun restaurarBackup() {
        viewModelScope.launch {
            repository.restaurarBackup()
        }
    }
}