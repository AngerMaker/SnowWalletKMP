package com.zanini.snowwallet.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cartoes_credito")
data class CartaoCredito(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val limite: Double,
    val diaFechamento: Int,
    val diaVencimento: Int,
    val nomeIcone: String = "CreditCard",
    val contaId: Long? = null // Novo campo: Vínculo com a conta bancária
)