package com.zanini.snowwallet.ui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zanini.snowwallet.data.repository.CategoriaRepository
import com.zanini.snowwallet.model.Categoria
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GerenciarCategoriasViewModel(
    private val repository: CategoriaRepository
) : ViewModel() {

    val categorias: StateFlow<List<Categoria>> = repository.getTodasCategorias()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun salvarCategoria(id: Long, nome: String, icone: String) {
        viewModelScope.launch {
            // CORREÇÃO: Precisamos definir 'cor' e 'tipo'.
            // Se for edição, tentamos recuperar os valores antigos para não perdê-los.
            // Se for criação, usamos padrões.

            var corFinal = "#9C27B0" // Roxo padrão
            var tipoFinal = "DESPESA" // Padrão

            if (id != 0L) {
                // Tenta achar a categoria existente na lista atual
                val existente = categorias.value.find { it.id == id }
                if (existente != null) {
                    corFinal = existente.cor
                    tipoFinal = existente.tipo
                }
            }

            val categoria = Categoria(
                id = id,
                nome = nome,
                nomeIcone = icone,
                cor = corFinal,
                tipo = tipoFinal
            )

            if (id == 0L) {
                repository.inserir(categoria)
            } else {
                repository.atualizar(categoria)
            }
        }
    }

    fun deletarCategoria(categoria: Categoria) {
        viewModelScope.launch {
            repository.deletar(categoria)
        }
    }
}