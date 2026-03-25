package com.zanini.snowwallet.ui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zanini.snowwallet.data.repository.TransacaoRepository
import com.zanini.snowwallet.model.Transacao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class SearchViewModel(
    private val repository: TransacaoRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow() // Expondo para a UI usar no TextField

    // Filtra as transações baseado no texto
    val transacoesFiltradas: StateFlow<List<Transacao>> = combine(
        _query,
        repository.getAllTransacoes()
    ) { queryTexto, listaTransacoes ->
        if (queryTexto.isBlank()) {
            emptyList()
        } else {
            listaTransacoes.filter { it.descricao.contains(queryTexto, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }
}