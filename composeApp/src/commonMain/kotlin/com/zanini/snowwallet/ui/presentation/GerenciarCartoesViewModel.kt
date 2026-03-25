package com.zanini.snowwallet.ui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zanini.snowwallet.data.repository.CartaoCreditoRepository
import com.zanini.snowwallet.data.repository.ContaRepository
import com.zanini.snowwallet.model.CartaoCredito
import com.zanini.snowwallet.model.Conta
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GerenciarCartoesViewModel(
    private val cartaoRepository: CartaoCreditoRepository,
    private val contaRepository: ContaRepository
) : ViewModel() {

    val cartoes: StateFlow<List<CartaoCredito>> = cartaoRepository.getTodosCartoes()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contas: StateFlow<List<Conta>> = contaRepository.getTodasContas()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun salvarCartao(
        id: Long,
        nome: String,
        limite: Double,
        diaFechamento: Int,
        diaVencimento: Int,
        contaId: Long?
        // Parâmetro icone removido
    ) {
        viewModelScope.launch {
            val novoCartao = CartaoCredito(
                id = id,
                nome = nome,
                limite = limite,
                diaFechamento = diaFechamento,
                diaVencimento = diaVencimento,
                contaId = contaId
            )
            if (id == 0L) {
                cartaoRepository.inserir(novoCartao)
            } else {
                cartaoRepository.atualizar(novoCartao)
            }
        }
    }

    fun deletarCartao(cartao: CartaoCredito) {
        viewModelScope.launch {
            cartaoRepository.deletar(cartao)
        }
    }
}