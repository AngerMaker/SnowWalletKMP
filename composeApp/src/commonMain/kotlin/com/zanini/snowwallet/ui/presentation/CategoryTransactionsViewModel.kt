package com.zanini.snowwallet.ui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zanini.snowwallet.data.repository.CategoriaRepository
import com.zanini.snowwallet.data.repository.TransacaoRepository
import com.zanini.snowwallet.model.Transacao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class CategoryTransactionsUiState(
    val nomeCategoria: String = "",
    val transacoes: List<Transacao> = emptyList(),
    val total: Double = 0.0
)

class CategoryTransactionsViewModel(
    private val transacaoRepository: TransacaoRepository,
    private val categoriaRepository: CategoriaRepository
) : ViewModel() {

    private val _filtros = MutableStateFlow(Triple(0L, 0, 0)) // CatId, Mes, Ano

    val uiState: StateFlow<CategoryTransactionsUiState> = combine(
        _filtros,
        transacaoRepository.getAllTransacoes(),
        categoriaRepository.getTodasCategorias()
    ) { (catId, mes, ano), todasTransacoes, categorias ->

        // Filtro manual já que removemos o método específico do DAO para simplificar
        val filtradas = todasTransacoes.filter { t ->
            val data = Instant.fromEpochMilliseconds(t.data)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            t.categoriaId == catId && data.monthNumber == mes && data.year == ano
        }

        val nomeCat = categorias.find { it.id == catId }?.nome ?: "Categoria"

        // Correção da ambiguidade do sumOf
        val soma: Double = filtradas.sumOf { it.valor }

        CategoryTransactionsUiState(
            nomeCategoria = nomeCat,
            transacoes = filtradas,
            total = soma
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategoryTransactionsUiState())

    fun init(categoryId: Long, month: Int, year: Int) {
        _filtros.value = Triple(categoryId, month, year)
    }
}