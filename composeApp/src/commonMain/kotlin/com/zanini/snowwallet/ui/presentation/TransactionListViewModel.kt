package com.zanini.snowwallet.ui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zanini.snowwallet.data.repository.TransacaoRepository
import com.zanini.snowwallet.model.Transacao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionListUiState(
    val transacoes: List<Transacao> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false
)

class TransactionListViewModel(
    private val repository: TransacaoRepository
) : ViewModel() {

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<TransactionListUiState> = combine(
        repository.getAllTransacoes(),
        _selectedIds
    ) { transacoes, selected ->
        TransactionListUiState(
            transacoes = transacoes,
            selectedIds = selected,
            isSelectionMode = selected.isNotEmpty()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionListUiState())

    fun toggleSelection(id: Long) {
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedIds.value = current
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val ids = _selectedIds.value.toList()
            repository.deletarMultiplos(ids)
            clearSelection()
        }
    }
}