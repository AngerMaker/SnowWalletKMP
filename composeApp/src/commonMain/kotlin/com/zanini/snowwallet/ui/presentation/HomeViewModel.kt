// src/commonMain/kotlin/com/zanini/snowwallet/ui/presentation/HomeViewModel.kt
package com.zanini.snowwallet.ui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zanini.snowwallet.data.repository.*
import com.zanini.snowwallet.model.CartaoCredito
import com.zanini.snowwallet.model.Categoria
import com.zanini.snowwallet.model.Conta
import com.zanini.snowwallet.model.Transacao
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*

data class CartaoUiModel(val cartao: CartaoCredito, val limiteDisponivel: Double, val limiteTotal: Double)
data class TransacaoUiModel(val transacao: Transacao, val iconeCategoria: String?, val nomeOrigem: String?)

enum class FiltroTransacao { TODOS, RECEITA, DESPESA, PENDENTE }

data class HomeUiState(
    val saldoTotal: Double = 0.0,
    val saldoProjetado: Double = 0.0,
    val saldoDisponivel: Double = 0.0,
    val receitasMes: Double = 0.0,
    val despesasMes: Double = 0.0,
    val pendentesMes: Double = 0.0,
    val transacoesDoMesSelecionado: List<TransacaoUiModel> = emptyList(),
    val mostrarSaldo: Boolean = true,
    val notificacoesNaoLidas: Int = 0,
    val cartoesUi: List<CartaoUiModel> = emptyList(),
    val mesSelecionado: Int = 0,
    val anoSelecionado: Int = 0,
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val valorSelecionado: Double = 0.0, // NOVO: Soma das transações selecionadas
    val filtroAtual: FiltroTransacao = FiltroTransacao.TODOS,
    val errorMessage: String? = null
)

private data class DbData(
    val transacoes: List<Transacao>,
    val contas: List<Conta>,
    val cartoes: List<CartaoCredito>,
    val categorias: List<Categoria>,
    val qtdNotificacoes: Int
)

private data class DadosRepo(
    val transacoes: List<Transacao>,
    val contas: List<Conta>,
    val cartoes: List<CartaoCredito>,
    val categorias: List<Categoria>,
    val qtdNotificacoes: Int
)

private data class DadosUi(
    val mostrar: Boolean,
    val mes: Int,
    val ano: Int,
    val selected: Set<Long>,
    val filtro: FiltroTransacao,
    val errorMessage: String?
)

class HomeViewModel(
    private val transacaoRepository: TransacaoRepository,
    private val contaRepository: ContaRepository,
    private val cartaoRepository: CartaoCreditoRepository,
    private val categoriaRepository: CategoriaRepository,
    private val notificacaoRepository: NotificacaoRepository,
    private val recorrenciaRepository: LancamentoRecorrenteRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _mostrarSaldo = MutableStateFlow(true)

    private val _mesAnoSelecionado = MutableStateFlow(
        Pair(
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).monthNumber,
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
        )
    )

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _filtroAtual = MutableStateFlow(FiltroTransacao.TODOS)
    private val _errorMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            try {
                categoriaRepository.inicializarPadroes()
                recorrenciaRepository.processarRecorrencias()
            } catch (e: Exception) { e.printStackTrace() }
        }
        viewModelScope.launch {
            userPreferencesRepository.showBalance.collect { _mostrarSaldo.value = it }
        }
    }

    private val dbFlow = combine(
        transacaoRepository.getAllTransacoes(),
        contaRepository.getTodasContas(),
        cartaoRepository.getTodosCartoes(),
        categoriaRepository.getTodasCategorias(),
        notificacaoRepository.quantidadeNaoLidas
    ) { transacoes, contas, cartoes, categorias, qtd ->
        DbData(transacoes, contas, cartoes, categorias, qtd)
    }

    private val dadosRepoFlow = dbFlow.map { db ->
        DadosRepo(
            transacoes = db.transacoes,
            contas = db.contas,
            cartoes = db.cartoes,
            categorias = db.categorias,
            qtdNotificacoes = db.qtdNotificacoes
        )
    }

    private val dadosUiFlow = combine(
        _mostrarSaldo,
        _mesAnoSelecionado,
        _selectedIds,
        _filtroAtual,
        _errorMessage
    ) { mostrar, mesAno, selected, filtro, error ->
        DadosUi(mostrar, mesAno.first, mesAno.second, selected, filtro, error)
    }

    val uiState: StateFlow<HomeUiState> = combine(dadosRepoFlow, dadosUiFlow) { repo, ui ->
        val timeZone = TimeZone.currentSystemDefault()

        val primeiroDiaMesSeguinteSel = if (ui.mes == 12) LocalDate(ui.ano + 1, 1, 1) else LocalDate(ui.ano, ui.mes + 1, 1)
        val fimDoMesSelMillis = primeiroDiaMesSeguinteSel.minus(DatePeriod(days = 1)).atTime(23, 59, 59).toInstant(timeZone).toEpochMilliseconds()

        val saldoInicialContas = repo.contas.sumOf { it.saldoInicial }
        val transacoesAteFimMesSel = repo.transacoes.filter { it.data <= fimDoMesSelMillis }

        // Receitas TOTAIS Projetadas
        val receitasProjetadas = transacoesAteFimMesSel.filter { it.tipo == "RECEITA" }.sumOf { it.valor }
        
        // Receitas TOTAIS já DE FATO recebidas (caíram na conta)
        val receitasRealizadas = transacoesAteFimMesSel.filter { it.tipo == "RECEITA" && it.pago }.sumOf { it.valor }

        val despesasPagas = transacoesAteFimMesSel.filter { it.tipo == "DESPESA" && it.pago && it.cartaoId == null }.sumOf { it.valor }

        val saldoContaProjetado = saldoInicialContas + receitasProjetadas - despesasPagas

        val contasPendentes = transacoesAteFimMesSel.filter { it.tipo == "DESPESA" && !it.pago && it.cartaoId == null }.sumOf { it.valor }
        val faturaCartoes = transacoesAteFimMesSel.filter { it.tipo == "DESPESA" && !it.pago && it.cartaoId != null }.sumOf { it.valor }

        val despesasPendentesTotais = contasPendentes + faturaCartoes
        val saldoDisponivelProjetado = (saldoInicialContas + receitasRealizadas - despesasPagas) - despesasPendentesTotais

        val transacoesMes = repo.transacoes.filter {
            val dt = Instant.fromEpochMilliseconds(it.data).toLocalDateTime(timeZone)
            dt.monthNumber == ui.mes && dt.year == ui.ano
        }.sortedByDescending { it.data }

        val receitasMes = transacoesMes.filter { it.tipo == "RECEITA" }.sumOf { it.valor }
        val despesasMes = transacoesMes.filter { it.tipo == "DESPESA" && it.pago }.sumOf { it.valor }
        val pendentesMes = transacoesMes.filter { it.tipo == "DESPESA" && !it.pago }.sumOf { it.valor }

        val transacoesUi = transacoesMes.filter { tx ->
            when (ui.filtro) {
                FiltroTransacao.TODOS -> true
                FiltroTransacao.RECEITA -> tx.tipo == "RECEITA"
                FiltroTransacao.DESPESA -> tx.tipo == "DESPESA" && tx.pago
                FiltroTransacao.PENDENTE -> !tx.pago
            }
        }.map { tx ->
            val cat = repo.categorias.find { it.id == tx.categoriaId }
            val nomeOrigem = if (tx.cartaoId != null) repo.cartoes.find { it.id == tx.cartaoId }?.nome ?: "Cartão"
            else repo.contas.find { it.id == tx.contaId }?.nome ?: "Conta"
            TransacaoUiModel(tx, cat?.nomeIcone, nomeOrigem)
        }

        val cartoesProcessados = repo.cartoes.map { c ->
            val gasto = repo.transacoes.filter { it.cartaoId == c.id && !it.pago }.sumOf { it.valor }
            CartaoUiModel(c, c.limite - gasto, c.limite)
        }

        // NOVO: Calcula a soma do que foi selecionado
        val transacoesSelecionadas = repo.transacoes.filter { it.id in ui.selected }
        val valorSelecionado = transacoesSelecionadas.sumOf { if (it.tipo == "RECEITA") it.valor else -it.valor }

        HomeUiState(
            saldoTotal = saldoContaProjetado,
            saldoProjetado = saldoContaProjetado,
            saldoDisponivel = saldoDisponivelProjetado,
            receitasMes = receitasMes,
            despesasMes = despesasMes,
            pendentesMes = pendentesMes,
            transacoesDoMesSelecionado = transacoesUi,
            mostrarSaldo = ui.mostrar,
            notificacoesNaoLidas = repo.qtdNotificacoes,
            cartoesUi = cartoesProcessados,
            mesSelecionado = ui.mes,
            anoSelecionado = ui.ano,
            isSelectionMode = ui.selected.isNotEmpty(),
            selectedIds = ui.selected,
            valorSelecionado = valorSelecionado,
            filtroAtual = ui.filtro,
            errorMessage = ui.errorMessage
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, HomeUiState())

    fun toggleMostrarSaldo() {
        _mostrarSaldo.value = !_mostrarSaldo.value
        viewModelScope.launch {
            userPreferencesRepository.updateShowBalance(_mostrarSaldo.value)
        }
    }

    fun mesAnterior() { updateMes(-1) }
    fun proximoMes() { updateMes(1) }
    private fun updateMes(delta: Int) {
        var m = _mesAnoSelecionado.value.first + delta
        var a = _mesAnoSelecionado.value.second
        if (m < 1) { m = 12; a-- }
        if (m > 12) { m = 1; a++ }
        _mesAnoSelecionado.value = Pair(m, a)
    }

    fun setFiltro(novoFiltro: FiltroTransacao) {
        _filtroAtual.value = if (_filtroAtual.value == novoFiltro) FiltroTransacao.TODOS else novoFiltro
    }

    fun toggleSelection(id: Long) {
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedIds.value = current
    }

    fun clearSelection() { _selectedIds.value = emptySet() }

    fun selectAll() {
        _selectedIds.value = uiState.value.transacoesDoMesSelecionado.map { it.transacao.id }.toSet()
    }

    fun markSelectedAsPaid() {
        viewModelScope.launch {
            try {
                val ids = _selectedIds.value.toList()
                if (ids.isNotEmpty()) {
                    transacaoRepository.marcarFaturaComoPaga(ids)
                    clearSelection()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao marcar como pago: ${e.message}"
            }
        }
    }

    fun hasRecurringInSelection(): Boolean {
        val uiList = uiState.value.transacoesDoMesSelecionado
        val selected = _selectedIds.value
        return uiList.any { item ->
            item.transacao.id in selected &&
                    (item.transacao.recorrenciaId != null || item.transacao.numeroParcelas > 1)
        }
    }

    fun deleteSelected(deleteFutureRecorrentes: Boolean) {
        viewModelScope.launch {
            try {
                val idsToDelete = _selectedIds.value.toList()
                if (idsToDelete.isEmpty()) return@launch

                if (deleteFutureRecorrentes) {
                    val allTransacoes = transacaoRepository.getAllTransacoes().first()
                    val selectedTransacoes = allTransacoes.filter { it.id in idsToDelete }

                    selectedTransacoes.forEach { t ->
                        if (t.recorrenciaId != null) {
                            transacaoRepository.deletarRecorrentesFuturas(t.recorrenciaId, t.data)
                        } else if (t.numeroParcelas > 1) {
                            val baseDescricao = if (t.descricao.contains(" (")) t.descricao.substringBeforeLast(" (") else t.descricao
                            val searchPattern = "$baseDescricao (%"
                            transacaoRepository.deletarParcelasFuturas(searchPattern, t.data)
                        }
                    }
                }

                transacaoRepository.deletarMultiplos(idsToDelete)
                clearSelection()

            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Falha ao excluir transação: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}