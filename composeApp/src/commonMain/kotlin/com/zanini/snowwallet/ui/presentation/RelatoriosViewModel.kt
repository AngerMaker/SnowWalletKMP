package com.zanini.snowwallet.ui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zanini.snowwallet.data.repository.TransacaoRepository
import com.zanini.snowwallet.model.GastoPorCategoria
import kotlinx.coroutines.ExperimentalCoroutinesApi // Import adicionado
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class RelatoriosUiState(
    val mesSelecionado: Int = 0,
    val anoSelecionado: Int = 0,
    val gastosPorCategoria: List<GastoPorCategoria> = emptyList(),
    val receitasPorCategoria: List<GastoPorCategoria> = emptyList(),
    val isLoading: Boolean = false
)

class RelatoriosViewModel(
    private val repository: TransacaoRepository
) : ViewModel() {

    private val hoje = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    private val _mesSelecionado = MutableStateFlow(hoje.monthNumber)
    private val _anoSelecionado = MutableStateFlow(hoje.year)

    @OptIn(ExperimentalCoroutinesApi::class) // Correção do Warning
    val uiState: StateFlow<RelatoriosUiState> = combine(
        _mesSelecionado,
        _anoSelecionado
    ) { mes, ano ->
        mes to ano
    }.flatMapLatest { (mes, ano) ->
        combine(
            repository.getGastosPorCategoria(mes, ano),
            repository.getReceitasPorCategoria(mes, ano)
        ) { gastos, receitas ->
            RelatoriosUiState(
                mesSelecionado = mes,
                anoSelecionado = ano,
                gastosPorCategoria = gastos,
                receitasPorCategoria = receitas
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RelatoriosUiState(
            mesSelecionado = hoje.monthNumber,
            anoSelecionado = hoje.year
        )
    )

    fun mesAnterior() {
        var mes = _mesSelecionado.value - 1
        var ano = _anoSelecionado.value
        if (mes < 1) {
            mes = 12
            ano--
        }
        _mesSelecionado.value = mes
        _anoSelecionado.value = ano
    }

    fun proximoMes() {
        var mes = _mesSelecionado.value + 1
        var ano = _anoSelecionado.value
        if (mes > 12) {
            mes = 1
            ano++
        }
        _mesSelecionado.value = mes
        _anoSelecionado.value = ano
    }
}