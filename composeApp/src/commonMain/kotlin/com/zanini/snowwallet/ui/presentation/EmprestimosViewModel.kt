// src/commonMain/kotlin/com/zanini/snowwallet/ui/presentation/EmprestimosViewModel.kt
package com.zanini.snowwallet.ui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zanini.snowwallet.data.repository.CategoriaRepository
import com.zanini.snowwallet.data.repository.ContaRepository
import com.zanini.snowwallet.data.repository.EmprestimoRepository
import com.zanini.snowwallet.data.repository.TransacaoRepository
import com.zanini.snowwallet.model.Categoria
import com.zanini.snowwallet.model.Conta
import com.zanini.snowwallet.model.Emprestimo
import com.zanini.snowwallet.model.Transacao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class EmprestimosUiState(
    val lista: List<Emprestimo> = emptyList(),
    val totalAReceber: Double = 0.0,
    val totalAPagar: Double = 0.0
)

class EmprestimosViewModel(
    private val repository: EmprestimoRepository,
    private val transacaoRepository: TransacaoRepository, // Injetado para mexer no saldo
    private val contaRepository: ContaRepository,
    private val categoriaRepository: CategoriaRepository
) : ViewModel() {

    private var _isProcessandoAbatimento = false

    val uiState: StateFlow<EmprestimosUiState> = repository.getTodos()
        .map { lista ->
            EmprestimosUiState(
                lista = lista,
                totalAReceber = lista.filter { it.tipo == "CONCEDIDO" && !it.finalizado }
                    .sumOf { it.valorTotal - it.valorPago },
                totalAPagar = lista.filter { it.tipo == "TOMADO" && !it.finalizado }
                    .sumOf { it.valorTotal - it.valorPago }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = EmprestimosUiState()
        )

    val contas: StateFlow<List<Conta>> = contaRepository.getTodasContas()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun salvarEmprestimo(
        descricao: String,
        valor: Double,
        pessoa: String,
        tipo: String, // "TOMADO" ou "CONCEDIDO"
        dataVencimento: Long?
    ) {
        viewModelScope.launch {
            val novo = Emprestimo(
                descricao = descricao,
                valorTotal = valor,
                valorPago = 0.0,
                dataEmprestimo = Clock.System.now().toEpochMilliseconds(),
                dataVencimento = dataVencimento,
                pessoa = pessoa,
                tipo = tipo,
                finalizado = false
            )
            repository.inserir(novo)
        }
    }

    // ATUALIZADO: Nova função para editar um empréstimo existente
    fun editarEmprestimo(
        emprestimo: Emprestimo,
        descricao: String,
        valor: Double,
        pessoa: String,
        tipo: String,
        dataVencimento: Long?
    ) {
        viewModelScope.launch {
            // Se o novo valor for menor que o que já foi pago, ajusta o valor pago ao novo total
            val valorPagoAjustado = if (emprestimo.valorPago > valor) valor else emprestimo.valorPago
            val finalizado = valorPagoAjustado >= valor

            val atualizado = emprestimo.copy(
                descricao = descricao,
                valorTotal = valor,
                pessoa = pessoa,
                tipo = tipo,
                dataVencimento = dataVencimento,
                valorPago = valorPagoAjustado,
                finalizado = finalizado
            )
            repository.atualizar(atualizado)
        }
    }

    fun abaterValor(emprestimo: Emprestimo, valorAbatido: Double, gerarTransacao: Boolean, contaId: Long?) {
        if (_isProcessandoAbatimento || valorAbatido <= 0.0) return
        _isProcessandoAbatimento = true
        
        viewModelScope.launch {
            try {
                // 1. Atualiza o registro do Empréstimo
                val novoPago = emprestimo.valorPago + valorAbatido
                val finalizado = novoPago >= emprestimo.valorTotal

                repository.atualizar(
                    emprestimo.copy(
                        valorPago = if (novoPago > emprestimo.valorTotal) emprestimo.valorTotal else novoPago,
                        finalizado = finalizado
                    )
                )

                // 2. Gera a Transação para impactar o Saldo APENAS se o utilizador marcou a caixa
                if (gerarTransacao) {
                    val tipoTransacao = if (emprestimo.tipo == "CONCEDIDO") "RECEITA" else "DESPESA"

                    // Busca ou cria a categoria para vincular as transações de empréstimos
                    var categoria = categoriaRepository.buscarPorNome("Empréstimos")
                    if (categoria == null) {
                        val novaCategoria = Categoria(
                            nome = "Empréstimos",
                            nomeIcone = "AccountBalance",
                            cor = "9E9E9E",
                            tipo = tipoTransacao
                        )
                        categoriaRepository.inserir(novaCategoria)
                        categoria = categoriaRepository.buscarPorNome("Empréstimos")
                    }

                    val descricaoTransacao = "Abatimento: ${emprestimo.pessoa} (${emprestimo.descricao})"

                    val novaTransacao = Transacao(
                        descricao = descricaoTransacao,
                        valor = valorAbatido,
                        tipo = tipoTransacao,
                        data = Clock.System.now().toEpochMilliseconds(),
                        categoriaId = categoria?.id,
                        contaId = contaId, // Utiliza a conta selecionada na UI
                        cartaoId = null,
                        pago = true, // Já foi pago/recebido no ato do abatimento
                        numeroParcelas = 1,
                        parcelaAtual = 1
                    )
                    transacaoRepository.inserir(novaTransacao)
                }
            } finally {
                _isProcessandoAbatimento = false
            }
        }
    }

    fun deletar(item: Emprestimo) {
        viewModelScope.launch {
            repository.deletar(item)
        }
    }
}