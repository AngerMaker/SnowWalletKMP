package com.zanini.snowwallet.ui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zanini.snowwallet.data.repository.CartaoCreditoRepository
import com.zanini.snowwallet.data.repository.ContaRepository
import com.zanini.snowwallet.data.repository.TransacaoRepository
import com.zanini.snowwallet.model.Transacao
import kotlinx.coroutines.ExperimentalCoroutinesApi // Import adicionado
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*

data class FaturaUiState(
    val transacoes: List<Transacao> = emptyList(),
    val valorTotal: Double = 0.0,
    val mes: Int = 0,
    val ano: Int = 0,
    val nomeCartao: String = "",
    val contaVinculadaId: Long? = null,
    val nomeContaVinculada: String = "",
    val faturaPaga: Boolean = false
)

class FaturaViewModel(
    private val repository: TransacaoRepository,
    private val cartaoRepository: CartaoCreditoRepository,
    private val contaRepository: ContaRepository
) : ViewModel() {

    private val hoje = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    private val _mesFatura = MutableStateFlow(hoje.monthNumber)
    private val _anoFatura = MutableStateFlow(hoje.year)
    private val _cartaoIdSelecionado = MutableStateFlow<Long?>(null)
    private val _faturaPaga = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class) // Correção do Warning
    val uiState: StateFlow<FaturaUiState> = combine(
        _cartaoIdSelecionado,
        _mesFatura,
        _anoFatura,
        _faturaPaga
    ) { id, mes, ano, paga ->
        Triple(id, mes to ano, paga)
    }.flatMapLatest { (id, data, paga) ->
        val (mes, ano) = data
        if (id == null) {
            flowOf(FaturaUiState(mes = mes, ano = ano))
        } else {
            val cartao = cartaoRepository.getCartaoById(id)
            if (cartao != null) {
                val contaVinculada = cartao.contaId?.let { contaRepository.getContaById(it) }

                val diaFechamento = cartao.diaFechamento
                val dataFim = getValidDate(ano, mes, diaFechamento)
                val (mesAnt, anoAnt) = if (mes == 1) 12 to (ano - 1) else (mes - 1) to ano
                val dataFechamentoAnterior = getValidDate(anoAnt, mesAnt, diaFechamento)
                val dataInicio = dataFechamentoAnterior.plus(1, DateTimeUnit.DAY)

                val tz = TimeZone.currentSystemDefault()
                val inicioTs = dataInicio.atStartOfDayIn(tz).toEpochMilliseconds()
                val fimTs = dataFim.atStartOfDayIn(tz).toEpochMilliseconds() + (24 * 60 * 60 * 1000) - 1

                repository.getTransacoesPorIntervaloCartao(id, inicioTs, fimTs).map { lista ->
                    FaturaUiState(
                        transacoes = lista,
                        valorTotal = lista.filter { !it.pago }.sumOf { it.valor },
                        mes = mes,
                        ano = ano,
                        nomeCartao = cartao.nome,
                        contaVinculadaId = cartao.contaId,
                        nomeContaVinculada = contaVinculada?.nome ?: "Nenhuma (Usará Padrão)",
                        faturaPaga = paga
                    )
                }
            } else {
                flowOf(FaturaUiState(mes = mes, ano = ano, nomeCartao = "Cartão não encontrado"))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FaturaUiState())

    fun selecionarCartao(id: Long) { _cartaoIdSelecionado.value = id }
    fun mesAnterior() { updateMes(-1) }
    fun proximoMes() { updateMes(1) }

    private fun updateMes(delta: Int) {
        var m = _mesFatura.value + delta
        var a = _anoFatura.value
        if (m < 1) { m = 12; a-- }
        if (m > 12) { m = 1; a++ }
        _mesFatura.value = m
        _anoFatura.value = a
    }

    fun pagarFatura() {
        if (_faturaPaga.value) return // Evita duplo clique (Race Condition)

        val state = uiState.value
        val valorPagar = state.valorTotal
        val idsParaPagar = state.transacoes.filter { !it.pago }.map { it.id }

        if (valorPagar <= 0 || idsParaPagar.isEmpty()) return

        _faturaPaga.value = true

        viewModelScope.launch {
            val contaIdFinal = if (state.contaVinculadaId != null) {
                state.contaVinculadaId
            } else {
                val contas = contaRepository.getTodasContas().first()
                if (contas.isNotEmpty()) contas.first().id else null
            }

            if (contaIdFinal == null) {
                _faturaPaga.value = false
                return@launch
            }

            // Descobrindo a data exata do vencimento com base no cartao para lançar o débito na mesma data
            val cartao = cartaoRepository.getCartaoById(state.contaVinculadaId ?: _cartaoIdSelecionado.value ?: 0L)
            val dataVencimentoDebito = if (cartao != null) {
                if (cartao.diaVencimento < cartao.diaFechamento) {
                    var novoMes = state.mes + 1
                    var novoAno = state.ano
                    if (novoMes > 12) { novoMes = 1; novoAno++ }
                    getValidDate(novoAno, novoMes, cartao.diaVencimento)
                } else {
                    getValidDate(state.ano, state.mes, cartao.diaVencimento)
                }
            } else {
                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            }

            val pagamentoTransacao = Transacao(
                descricao = "Fatura ${state.nomeCartao} - ${state.mes}/${state.ano}",
                valor = valorPagar,
                tipo = "DESPESA",
                data = dataVencimentoDebito.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
                categoriaId = null,
                contaId = contaIdFinal,
                cartaoId = null,
                pago = true,
                numeroParcelas = 1,
                parcelaAtual = 1
            )
            repository.inserir(pagamentoTransacao)
            repository.marcarFaturaComoPaga(idsParaPagar)

            _faturaPaga.value = true
            kotlinx.coroutines.delay(2000)
            _faturaPaga.value = false
        }
    }

    private fun getValidDate(ano: Int, mes: Int, diaPreferido: Int): LocalDate {
        var dia = diaPreferido
        while (dia > 28) {
            try { return LocalDate(ano, mes, dia) } catch (e: Exception) { dia-- }
        }
        return LocalDate(ano, mes, dia)
    }
}