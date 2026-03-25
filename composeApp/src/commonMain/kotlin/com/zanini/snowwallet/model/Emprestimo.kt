package com.zanini.snowwallet.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emprestimos")
data class Emprestimo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val descricao: String, // Ex: "Empréstimo para João"
    val valorTotal: Double,
    val valorPago: Double = 0.0, // Quanto já foi quitado
    val dataEmprestimo: Long,
    val dataVencimento: Long? = null,
    val pessoa: String, // Nome da pessoa envolvida
    val tipo: String, // "TOMADO" (Peguei emprestado) ou "CONCEDIDO" (Eu emprestei)
    val finalizado: Boolean = false
)