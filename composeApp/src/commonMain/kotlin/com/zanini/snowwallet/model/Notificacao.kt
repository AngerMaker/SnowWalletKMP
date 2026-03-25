package com.zanini.snowwallet.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notificacoes")
data class Notificacao(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val mensagem: String,
    val data: Long,
    val lida: Boolean = false,
    val tipo: String = "INFO" // "ALERTA", "SUCESSO", "INFO"
)