// src/commonMain/kotlin/com/zanini/snowwallet/ui/presentation/RecorrenciaViewModel.kt
package com.zanini.snowwallet.ui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zanini.snowwallet.data.repository.CartaoCreditoRepository
import com.zanini.snowwallet.data.repository.CategoriaRepository
import com.zanini.snowwallet.data.repository.ContaRepository
import com.zanini.snowwallet.data.repository.LancamentoRecorrenteRepository
import com.zanini.snowwallet.data.repository.TransacaoRepository
import com.zanini.snowwallet.model.Categoria
import com.zanini.snowwallet.model.LancamentoRecorrente
import com.zanini.snowwallet.model.Transacao
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*

data class RecorrenciaUiState(
    val itens: List<LancamentoRecorrente> = emptyList(),
    val categorias: List<Categoria> = emptyList(),
    val opcoesPagamento: List<OpcaoPagamento> = emptyList(),
    val errorMessage: String? = null // NOVO: Controle de erro
)

class RecorrenciaViewModel(
    private val repository: LancamentoRecorrenteRepository,
    private val transacaoRepository: TransacaoRepository,
    private val categoriaRepository: CategoriaRepository,
    private val contaRepository: ContaRepository,
    private val cartaoRepository: CartaoCreditoRepository
) : ViewModel() {

    // Extraído o stateFlow para usar MutableStateFlow para podermos alterar a UI internamente
    private val _uiState = MutableStateFlow(RecorrenciaUiState())
    val uiState: StateFlow<RecorrenciaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getAll(),
                categoriaRepository.getTodasCategorias(),
                contaRepository.getTodasContas(),
                cartaoRepository.getTodosCartoes()
            ) { itens, categorias, contas, cartoes ->

                val listaUnificada = mutableListOf<OpcaoPagamento>()
                contas.forEach { listaUnificada.add(OpcaoPagamento.OpcaoConta(it)) }
                cartoes.forEach { listaUnificada.add(OpcaoPagamento.OpcaoCartao(it)) }

                // Mantém as mensagens de erro se existirem
                _uiState.value.copy(
                    itens = itens,
                    categorias = categorias,
                    opcoesPagamento = listaUnificada
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun emitError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun adicionarRecorrencia(
        descricao: String,
        valor: Double,
        diaCobranca: Int,
        categoriaId: Long?,
        contaId: Long?,
        cartaoId: Long?
    ) {
        // Validação adicional rigorosa pelo ViewModel
        if (categoriaId == null) {
            emitError("É obrigatório selecionar uma Categoria.")
            return
        }
        if (contaId == null && cartaoId == null) {
            emitError("É obrigatório selecionar uma Conta ou Cartão.")
            return
        }

        viewModelScope.launch {
            try {
                // 1. Cria a Recorrência
                val nova = LancamentoRecorrente(
                    descricao = descricao,
                    valor = valor,
                    ativo = true,
                    diaVencimento = diaCobranca,
                    categoriaId = categoriaId,
                    contaId = contaId,
                    cartaoId = cartaoId,
                    tipo = "DESPESA",
                    ultimoMesGerado = 0,
                    ultimoAnoGerado = 0
                )
                val idRecorrencia = repository.inserir(nova)

                // 2. Gera a PRIMEIRA transação manualmente
                val hoje = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val dataTransacao = tentarCriarData(hoje.year, hoje.monthNumber, diaCobranca)

                val transacao = Transacao(
                    descricao = descricao,
                    valor = valor,
                    tipo = "DESPESA",
                    data = dataTransacao.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
                    pago = false,
                    recorrenciaId = idRecorrencia,
                    numeroParcelas = 1,
                    parcelaAtual = 1,
                    categoriaId = categoriaId,
                    contaId = contaId,
                    cartaoId = cartaoId
                )
                transacaoRepository.inserir(transacao)

                // 3. Atualiza marcadores
                val recorrenciaAtualizada = nova.copy(
                    id = idRecorrencia,
                    ultimoMesGerado = hoje.monthNumber,
                    ultimoAnoGerado = hoje.year
                )
                repository.atualizar(recorrenciaAtualizada)

                // 4. Processa futuro
                repository.processarRecorrencias()
            } catch (e: Exception) {
                emitError("Erro ao salvar conta fixa: ${e.message}")
            }
        }
    }

    fun deletarRecorrencia(item: LancamentoRecorrente) {
        viewModelScope.launch {
            repository.deletar(item)
            transacaoRepository.deletarTransacoesRecorrentesPendentes(item.id)
        }
    }

    private fun tentarCriarData(ano: Int, mes: Int, dia: Int): LocalDate {
        var d = dia
        while (d > 28) {
            try { return LocalDate(ano, mes, d) } catch (e: Exception) { d-- }
        }
        return LocalDate(ano, mes, d)
    }
}