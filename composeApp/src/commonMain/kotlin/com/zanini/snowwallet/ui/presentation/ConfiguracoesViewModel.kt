// src/commonMain/kotlin/com/zanini/snowwallet/ui/presentation/ConfiguracoesViewModel.kt
package com.zanini.snowwallet.ui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zanini.snowwallet.data.repository.LancamentoRecorrenteRepository
import com.zanini.snowwallet.data.repository.TransacaoRepository
import com.zanini.snowwallet.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ConfiguracoesViewModel(
    private val transacaoRepository: TransacaoRepository,
    private val recorrenciaRepository: LancamentoRecorrenteRepository,
    private val userPreferences: UserPreferencesRepository
) : ViewModel() {

    val isDarkMode = userPreferences.isDarkMode
    val appThemeColor = userPreferences.appThemeColor

    fun toggleTheme(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.updateDarkMode(enabled)
        }
    }

    fun updateThemeColor(colorName: String) {
        viewModelScope.launch {
            userPreferences.updateAppThemeColor(colorName)
        }
    }

    fun limparTudo() {
        viewModelScope.launch {
            val todasTransacoes = transacaoRepository.getAllTransacoes().first()
            if (todasTransacoes.isNotEmpty()) {
                transacaoRepository.deletarMultiplos(todasTransacoes.map { it.id })
            }
            val todasRecorrencias = recorrenciaRepository.getAll().first()
            todasRecorrencias.forEach { recorrenciaRepository.deletar(it) }
        }
    }
}