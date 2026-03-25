package com.zanini.snowwallet.ui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zanini.snowwallet.data.repository.ContaRepository
import com.zanini.snowwallet.data.repository.TransacaoRepository
import com.zanini.snowwallet.model.Conta
import com.zanini.snowwallet.model.Transacao
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class TransferenciaUiState(
    val contas: List<Conta> = emptyList(),
    val contaOrigem: Conta? = null,
    val contaDestino: Conta? = null,
    val valor: String = "",
    val sucesso: Boolean = false
)

class TransferenciaViewModel(
    private val contaRepository: ContaRepository,
    private val transacaoRepository: TransacaoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferenciaUiState())
    val uiState = _uiState.asStateFlow()
    private var _isTransferindo = false

    init {
        viewModelScope.launch {
            val lista = contaRepository.getTodasContas().first()
            _uiState.update { it.copy(contas = lista, contaOrigem = lista.firstOrNull(), contaDestino = lista.getOrNull(1)) }
        }
    }

    fun onValorChange(v: String) { _uiState.update { it.copy(valor = v) } }
    fun onOrigemChange(c: Conta) { _uiState.update { it.copy(contaOrigem = c) } }
    fun onDestinoChange(c: Conta) { _uiState.update { it.copy(contaDestino = c) } }

    fun transferir() {
        if (_isTransferindo) return
        val state = _uiState.value
        val v = state.valor.replace(",", ".").toDoubleOrNull() ?: return
        if (state.contaOrigem == null || state.contaDestino == null || state.contaOrigem.id == state.contaDestino.id) return

        _isTransferindo = true
        viewModelScope.launch {
            try {
                val agora = Clock.System.now().toEpochMilliseconds()

                // 1. Saída da Origem
                transacaoRepository.inserir(Transacao(
                    descricao = "Transf. para ${state.contaDestino.nome}",
                    valor = v, tipo = "DESPESA", data = agora, pago = true, contaId = state.contaOrigem.id, categoriaId = null, cartaoId = null
                ))

                // 2. Entrada no Destino
                transacaoRepository.inserir(Transacao(
                    descricao = "Transf. de ${state.contaOrigem.nome}",
                    valor = v, tipo = "RECEITA", data = agora, pago = true, contaId = state.contaDestino.id, categoriaId = null, cartaoId = null
                ))

                _uiState.update { it.copy(sucesso = true) }
            } finally {
                _isTransferindo = false
            }
        }
    }
}