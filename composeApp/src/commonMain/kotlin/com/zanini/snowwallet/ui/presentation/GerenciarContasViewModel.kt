package com.zanini.snowwallet.ui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zanini.snowwallet.data.repository.ContaRepository
import com.zanini.snowwallet.data.repository.TransacaoRepository
import com.zanini.snowwallet.model.Conta
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ContaComSaldo(
    val conta: Conta,
    val saldoAtual: Double
)

class GerenciarContasViewModel(
    private val contaRepository: ContaRepository,
    private val transacaoRepository: TransacaoRepository
) : ViewModel() {

    // Combinar contas e transações para calcular saldo atual
    val contasComSaldo: StateFlow<List<ContaComSaldo>> = combine(
        contaRepository.getTodasContas(),
        transacaoRepository.getAllTransacoes()
    ) { contas, transacoes ->
        contas.map { conta ->
            val movimentacoes = transacoes.filter {
                it.contaId == conta.id && it.pago
            }.sumOf { t ->
                if (t.tipo == "RECEITA") t.valor else -t.valor
            }
            ContaComSaldo(conta, conta.saldoInicial + movimentacoes)
        }
    }
        .catch { emit(emptyList()) } // Evita crash
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun salvarConta(id: Long, nome: String, saldoInicial: Double, icone: String) {
        viewModelScope.launch {
            val novaConta = Conta(id = id, nome = nome, saldoInicial = saldoInicial, nomeIcone = icone)
            if (id == 0L) {
                contaRepository.inserir(novaConta)
            } else {
                contaRepository.atualizar(novaConta)
            }
        }
    }

    fun deletarConta(conta: Conta) {
        viewModelScope.launch {
            contaRepository.deletar(conta)
        }
    }
}