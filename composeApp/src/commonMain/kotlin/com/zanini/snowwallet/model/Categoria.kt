package com.zanini.snowwallet.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categorias") // Nome explícito para segurança
data class Categoria(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val nomeIcone: String,
    val cor: String,
    val tipo: String // "RECEITA" ou "DESPESA"
)