// src/commonMain/kotlin/com/zanini/snowwallet/ui/presentation/AddEditTransactionViewModel.kt
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
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toInstant

sealed class OpcaoPagamento {
    abstract val id: Long
    abstract val nome: String
    abstract val icone: String

    data class OpcaoConta(val conta: Conta) : OpcaoPagamento() {
        override val id = conta.id
        override val nome = conta.nome
        override val icone = conta.nomeIcone
    }

    data class OpcaoCartao(val cartao: CartaoCredito) : OpcaoPagamento() {
        override val id = cartao.id
        override val nome = "${cartao.nome} (Crédito)"
        override val icone = "CreditCard"
    }
}

data class AddEditTransactionUiState(
    val valor: String = "",
    val descricao: String = "",
    val tipo: String = "DESPESA",
    val data: Long = Clock.System.now().toEpochMilliseconds(),
    val pago: Boolean = true,
    val numeroParcelas: String = "1",

    val categoriaSelecionada: Categoria? = null,
    val opcaoPagamentoSelecionada: OpcaoPagamento? = null,
    val listaOpcoesPagamento: List<OpcaoPagamento> = emptyList(),
    val listaCategorias: List<Categoria> = emptyList(),

    val salvoComSucesso: Boolean = false,
    val isLoading: Boolean = false,
    val transacaoId: Long = 0L,

    val showDeleteDialog: Boolean = false,
    val isRecorrenteOuParcelada: Boolean = false,
    val recorrenciaId: Long? = null,
    val baseDescricao: String = "",
    val originalData: Long = 0L,

    // --- Notificações ---
    val agendarNotificacao: Boolean = false,
    val dataNotificacao: Long = 0L,
    val horaNotificacao: Int = 12,
    val minutoNotificacao: Int = 0,

    val errorMessage: String? = null
)

class AddEditTransactionViewModel(
    private val transacaoRepository: TransacaoRepository,
    private val categoriaRepository: CategoriaRepository,
    private val contaRepository: ContaRepository,
    private val cartaoRepository: CartaoCreditoRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val notificacaoRepository: com.zanini.snowwallet.data.repository.NotificacaoRepository,
    private val notificationScheduler: com.zanini.snowwallet.util.NotificationScheduler
) : ViewModel() {

    private fun getSafeDate(dataBase: LocalDate, mesesParaFrente: Int): LocalDate {
        var novoMes = dataBase.monthNumber + mesesParaFrente
        var novoAno = dataBase.year
        while (novoMes > 12) {
            novoMes -= 12
            novoAno += 1
        }
        var dia = dataBase.dayOfMonth
        while (dia > 28) {
            try { return LocalDate(novoAno, novoMes, dia) } catch (e: Exception) { dia-- }
        }
        return LocalDate(novoAno, novoMes, dia)
    }

    private val _uiState = MutableStateFlow(AddEditTransactionUiState())
    val uiState: StateFlow<AddEditTransactionUiState> = _uiState.asStateFlow()

    private suspend fun obterListas(): Pair<List<Categoria>, List<OpcaoPagamento>> {
        val categorias = categoriaRepository.getTodasCategorias().first()
        val contas = contaRepository.getTodasContas().first()
        val cartoes = cartaoRepository.getTodosCartoes().first()

        val listaUnificada = mutableListOf<OpcaoPagamento>()
        val cartoesVinculadosIds = mutableSetOf<Long>()

        contas.forEach { conta ->
            listaUnificada.add(OpcaoPagamento.OpcaoConta(conta))
            val cartoesDestaConta = cartoes.filter { it.contaId == conta.id }
            cartoesDestaConta.forEach { cartao ->
                listaUnificada.add(OpcaoPagamento.OpcaoCartao(cartao))
                cartoesVinculadosIds.add(cartao.id)
            }
        }

        val cartoesOrfaos = cartoes.filter { !cartoesVinculadosIds.contains(it.id) }
        cartoesOrfaos.forEach { cartao ->
            listaUnificada.add(OpcaoPagamento.OpcaoCartao(cartao))
        }

        return Pair(categorias, listaUnificada)
    }

    fun resetState(tipoInicial: String, cartaoId: Long? = null) {
        viewModelScope.launch {
            val (categorias, opcoes) = obterListas()
            var selectedOpcao = opcoes.firstOrNull()

            if (cartaoId != null) {
                val preSelecionado = opcoes.find { it is OpcaoPagamento.OpcaoCartao && it.cartao.id == cartaoId }
                if (preSelecionado != null) selectedOpcao = preSelecionado
            } else {
                val lastUsed = userPreferencesRepository.lastPaymentOption.first()
                if (lastUsed != null) {
                    val (type, id) = lastUsed
                    val found = opcoes.find { op ->
                        when (type) {
                            "CONTA" -> op is OpcaoPagamento.OpcaoConta && op.conta.id == id
                            "CARTAO" -> op is OpcaoPagamento.OpcaoCartao && op.cartao.id == id
                            else -> false
                        }
                    }
                    if (found != null) selectedOpcao = found
                }
            }

            _uiState.update {
                it.copy(
                    valor = "",
                    descricao = "",
                    tipo = tipoInicial,
                    salvoComSucesso = false,
                    transacaoId = 0L,
                    pago = tipoInicial == "DESPESA",
                    numeroParcelas = "1",
                    showDeleteDialog = false,
                    isRecorrenteOuParcelada = false,
                    errorMessage = null,
                    listaCategorias = categorias,
                    listaOpcoesPagamento = opcoes,
                    categoriaSelecionada = categorias.firstOrNull(),
                    opcaoPagamentoSelecionada = selectedOpcao,
                    agendarNotificacao = false,
                    dataNotificacao = Clock.System.now().toEpochMilliseconds(),
                    horaNotificacao = 12,
                    minutoNotificacao = 0
                )
            }
        }
    }

    fun carregarTransacao(id: Long) {
        viewModelScope.launch {
            try {
                val (categorias, opcoes) = obterListas()
                val transacao = transacaoRepository.getTransacaoById(id)

                if (transacao != null) {
                    val opcaoSelecionada = if (transacao.cartaoId != null) {
                        opcoes.find { it is OpcaoPagamento.OpcaoCartao && it.cartao.id == transacao.cartaoId }
                    } else {
                        opcoes.find { it is OpcaoPagamento.OpcaoConta && it.conta.id == transacao.contaId }
                    }

                    val isParcelada = transacao.numeroParcelas > 1
                    val isRecorrente = transacao.recorrenciaId != null

                    val notificacaoLigada = notificacaoRepository.todasNotificacoes.first()
                        .find { it.titulo == "Lembrete: ${transacao.descricao}" }

                    _uiState.update { state ->
                        state.copy(
                            listaCategorias = categorias,
                            listaOpcoesPagamento = opcoes,
                            transacaoId = transacao.id,
                            valor = transacao.valor.toString(),
                            descricao = transacao.descricao,
                            tipo = transacao.tipo,
                            data = transacao.data,
                            pago = transacao.pago,
                            numeroParcelas = if(transacao.numeroParcelas > 1) transacao.numeroParcelas.toString() else "1",
                            categoriaSelecionada = categorias.find { it.id == transacao.categoriaId },
                            opcaoPagamentoSelecionada = opcaoSelecionada,
                            isRecorrenteOuParcelada = isRecorrente || isParcelada,
                            recorrenciaId = transacao.recorrenciaId,
                            baseDescricao = if (isParcelada) transacao.descricao.substringBeforeLast(" (") else transacao.descricao,
                            originalData = transacao.data,
                            agendarNotificacao = notificacaoLigada != null,
                            dataNotificacao = notificacaoLigada?.data ?: Clock.System.now().toEpochMilliseconds(),
                            horaNotificacao = notificacaoLigada?.let { Instant.fromEpochMilliseconds(it.data).toLocalDateTime(TimeZone.currentSystemDefault()).hour } ?: 12,
                            minutoNotificacao = notificacaoLigada?.let { Instant.fromEpochMilliseconds(it.data).toLocalDateTime(TimeZone.currentSystemDefault()).minute } ?: 0
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Erro ao carregar transação: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun confirmarDelecao() {
        if (_uiState.value.isRecorrenteOuParcelada) {
            _uiState.update { it.copy(showDeleteDialog = true) }
        } else {
            deletarApenasEsta()
        }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun deletarApenasEsta() {
        val id = _uiState.value.transacaoId
        if (id == 0L) return

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                transacaoRepository.deletarMultiplos(listOf(id))
                _uiState.update { it.copy(showDeleteDialog = false, salvoComSucesso = true, isLoading = false) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false, errorMessage = "Falha ao apagar transação: ${e.message}") }
            }
        }
    }

    fun deletarEstaEFuturas() {
        val state = _uiState.value
        if (state.transacaoId == 0L) return

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                transacaoRepository.deletarMultiplos(listOf(state.transacaoId))

                if (state.recorrenciaId != null) {
                    transacaoRepository.deletarRecorrentesFuturas(state.recorrenciaId, state.originalData)
                } else if (state.isRecorrenteOuParcelada) {
                    val searchPattern = "${state.baseDescricao} (%"
                    transacaoRepository.deletarParcelasFuturas(searchPattern, state.originalData)
                }

                _uiState.update { it.copy(showDeleteDialog = false, salvoComSucesso = true, isLoading = false) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(showDeleteDialog = false, isLoading = false, errorMessage = "Erro crítico ao apagar futuras: ${e.message}") }
            }
        }
    }

    fun onValorChange(novoValor: String) {
        if (novoValor.all { it.isDigit() || it == '.' || it == ',' }) _uiState.update { it.copy(valor = novoValor) }
    }
    fun onDescricaoChange(v: String) { _uiState.update { it.copy(descricao = v) } }
    fun onTipoChange(v: String) {
        _uiState.update { it.copy(tipo = v) }
        if (v == "RECEITA") _uiState.update { it.copy(numeroParcelas = "1") }
    }
    fun onDateChange(v: Long) { _uiState.update { it.copy(data = v) } }
    fun onPagoChange(v: Boolean) { _uiState.update { it.copy(pago = v) } }
    fun onCategoriaChange(v: Categoria) { _uiState.update { it.copy(categoriaSelecionada = v) } }
    fun onOpcaoPagamentoChange(v: OpcaoPagamento) { _uiState.update { it.copy(opcaoPagamentoSelecionada = v) } }
    fun onParcelasChange(v: String) {
        if (v.all { it.isDigit() }) _uiState.update { it.copy(numeroParcelas = v) }
    }

    // --- Notificações Handlers ---
    fun onAgendarNotificacaoChange(v: Boolean) { _uiState.update { it.copy(agendarNotificacao = v) } }
    fun onDataNotificacaoChange(millis: Long) { _uiState.update { it.copy(dataNotificacao = millis) } }
    fun onHoraNotificacaoChange(hora: Int, minuto: Int) {
        _uiState.update { it.copy(horaNotificacao = hora, minutoNotificacao = minuto) }
    }

    fun salvarTransacao() {
        val state = _uiState.value
        val valorTotal = state.valor.replace(",", ".").toDoubleOrNull() ?: 0.0
        val numParcelas = maxOf(1, state.numeroParcelas.toIntOrNull() ?: 1)

        if (state.descricao.isBlank() || valorTotal <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Descrição e Valor são obrigatórios e o valor deve ser maior que zero.") }
            return
        }

        if (state.categoriaSelecionada == null) {
            _uiState.update { it.copy(errorMessage = "Por favor, selecione uma Categoria.") }
            return
        }

        if (state.opcaoPagamentoSelecionada == null) {
            _uiState.update { it.copy(errorMessage = "Por favor, selecione uma Conta ou Cartão de Pagamento/Recebimento.") }
            return
        }

        val opcao = state.opcaoPagamentoSelecionada
        val finalContaId = if (opcao is OpcaoPagamento.OpcaoConta) opcao.conta.id else null
        val finalCartaoId = if (opcao is OpcaoPagamento.OpcaoCartao) opcao.cartao.id else null
        val isCartao = finalCartaoId != null

        val sysZone = TimeZone.currentSystemDefault()
        
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                if (opcao != null) {
                    val typeStr = if (opcao is OpcaoPagamento.OpcaoConta) "CONTA" else "CARTAO"
                    userPreferencesRepository.updateLastPaymentOption(typeStr, opcao.id)
                }

                var inseriuAlguma = false

                if (numParcelas > 1 && state.transacaoId == 0L) {
                    // Parcelamento com correção matemática Anti-Dízima
                    val valorParcelaBase = kotlin.math.round((valorTotal / numParcelas) * 100) / 100.0
                    var somaParcelasAteAgora = 0.0
                    val dataBase = Instant.fromEpochMilliseconds(state.data).toLocalDateTime(sysZone)

                    for (i in 0 until numParcelas) {
                        val valorDestaParcela = if (i == numParcelas - 1) {
                            val resto = valorTotal - somaParcelasAteAgora
                            kotlin.math.round(resto * 100) / 100.0
                        } else {
                            valorParcelaBase
                        }
                        somaParcelasAteAgora += valorDestaParcela

                        val dataFinalMs = if (i == 0) state.data else {
                            val safeDate = getSafeDate(dataBase.date, i)
                            safeDate.atStartOfDayIn(sysZone).toEpochMilliseconds()
                        }
                        val estaParcelaPaga = if (isCartao) false else { if (i == 0) state.pago else false }

                        val novaTransacao = Transacao(
                            descricao = "${state.descricao} (${i + 1}/$numParcelas)",
                            valor = valorDestaParcela,
                            tipo = state.tipo,
                            data = dataFinalMs,
                            pago = estaParcelaPaga,
                            categoriaId = state.categoriaSelecionada.id,
                            contaId = finalContaId,
                            cartaoId = finalCartaoId,
                            numeroParcelas = numParcelas,
                            parcelaAtual = i + 1
                        )
                        transacaoRepository.inserir(novaTransacao)
                        inseriuAlguma = true
                    }
                } else {
                    // Transação Única / Atualização
                    val finalPago = if (isCartao) false else state.pago
                    val novaTransacao = Transacao(
                        id = state.transacaoId,
                        descricao = state.descricao,
                        valor = valorTotal,
                        tipo = state.tipo,
                        data = state.data,
                        pago = finalPago,
                        categoriaId = state.categoriaSelecionada.id,
                        contaId = finalContaId,
                        cartaoId = finalCartaoId,
                        numeroParcelas = state.numeroParcelas.toIntOrNull() ?: 1,
                        parcelaAtual = 1
                    )

                    if (state.transacaoId == 0L) {
                        transacaoRepository.inserir(novaTransacao)
                        inseriuAlguma = true
                    } else {
                        transacaoRepository.atualizar(novaTransacao)
                        inseriuAlguma = true
                    }
                }

                // --- Agendamento de Notificação ---
                if (state.agendarNotificacao && inseriuAlguma) {
                    val dtReminderUtc = Instant.fromEpochMilliseconds(state.dataNotificacao).toLocalDateTime(TimeZone.UTC)
                    val reminderLocalDate = kotlinx.datetime.LocalDateTime(
                        year = dtReminderUtc.year,
                        monthNumber = dtReminderUtc.monthNumber,
                        dayOfMonth = dtReminderUtc.dayOfMonth,
                        hour = state.horaNotificacao,
                        minute = state.minutoNotificacao,
                        second = 0,
                        nanosecond = 0
                    )
                    
                    val triggerMillis = reminderLocalDate.toInstant(sysZone).toEpochMilliseconds()
                    
                    val notifTitle = "Lembrete: ${state.descricao}"
                    val notifBody = "Você tem um lembrete planejado."

                    if (state.transacaoId != 0L) {
                        val velha = notificacaoRepository.todasNotificacoes.first().find { it.titulo == notifTitle }
                        if (velha != null) {
                            notificacaoRepository.deletar(velha)
                            notificationScheduler.cancelNotification(velha.id.toInt())
                        }
                    }

                    val idN = notificacaoRepository.inserir(
                        com.zanini.snowwallet.model.Notificacao(
                            titulo = notifTitle,
                            mensagem = notifBody,
                            data = triggerMillis,
                            lida = false,
                            tipo = "INFO"
                        )
                    )

                    notificationScheduler.scheduleNotification(
                        id = idN.toInt(),
                        title = notifTitle,
                        message = notifBody,
                        timeInMillis = triggerMillis
                    )
                } else if (!state.agendarNotificacao && state.transacaoId != 0L) {
                    val notifTitle = "Lembrete: ${state.descricao}"
                    val velha = notificacaoRepository.todasNotificacoes.first().find { it.titulo == notifTitle }
                    if (velha != null) {
                        notificacaoRepository.deletar(velha)
                        notificationScheduler.cancelNotification(velha.id.toInt())
                    }
                }

                _uiState.update { it.copy(salvoComSucesso = true, isLoading = false) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false, errorMessage = "Falha ao gravar dados: ${e.message}") }
            }
        }
    }
}