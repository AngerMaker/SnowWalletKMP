package com.zanini.snowwallet.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lancamentos_recorrentes")
data class LancamentoRecorrente(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val descricao: String,
    val valor: Double,
    val ativo: Boolean = true,
    val diaVencimento: Int = 1,

    // CONTROLE DE GERAÇÃO (Novo)
    // Guarda até onde já criamos transações para não recriar as deletadas
    val ultimoMesGerado: Int = 0,
    val ultimoAnoGerado: Int = 0,

    val tipo: String = "DESPESA",
    val categoriaId: Long? = null,
    val contaId: Long? = null,
    val cartaoId: Long? = null,
    val dataInicio: Long = 0L,
    val frequencia: String = "MENSAL"
)