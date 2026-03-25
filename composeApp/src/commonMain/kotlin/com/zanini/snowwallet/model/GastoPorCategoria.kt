package com.zanini.snowwallet.model

data class GastoPorCategoria(
    val nomeCategoria: String,
    val valorTotal: Double,
    val categoriaId: Long,
    val cor: String // Adicionado para resolver o erro do Repositório
)