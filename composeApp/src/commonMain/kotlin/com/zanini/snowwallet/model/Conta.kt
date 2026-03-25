package com.zanini.snowwallet.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contas")
data class Conta(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    // Armazena o nome do ícone (ex: "Wallet", "Bank") para ser recuperado na UI
    val nomeIcone: String,
    val saldoInicial: Double = 0.0
)