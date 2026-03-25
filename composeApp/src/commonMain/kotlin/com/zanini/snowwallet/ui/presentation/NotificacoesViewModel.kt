package com.zanini.snowwallet.ui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zanini.snowwallet.data.repository.NotificacaoRepository
import com.zanini.snowwallet.model.Notificacao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificacoesViewModel(
    private val repository: NotificacaoRepository
) : ViewModel() {

    val notificacoes: StateFlow<List<Notificacao>> = repository.todasNotificacoes
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun marcarComoLida(notificacao: Notificacao) {
        if (!notificacao.lida) {
            viewModelScope.launch {
                repository.marcarComoLida(notificacao)
            }
        }
    }

    fun deletar(notificacao: Notificacao) {
        viewModelScope.launch {
            repository.deletar(notificacao)
        }
    }

    fun limparTodas() {
        viewModelScope.launch {
            repository.limparTodas()
        }
    }
}