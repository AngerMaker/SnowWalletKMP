package com.zanini.snowwallet.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transacoes",
    foreignKeys = [
        ForeignKey(
            entity = Categoria::class,
            parentColumns = ["id"],
            childColumns = ["categoriaId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Conta::class,
            parentColumns = ["id"],
            childColumns = ["contaId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CartaoCredito::class,
            parentColumns = ["id"],
            childColumns = ["cartaoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    // Índices adicionados para corrigir avisos e melhorar performance
    indices = [
        Index(value = ["categoriaId"]),
        Index(value = ["contaId"]),
        Index(value = ["cartaoId"]),
        Index(value = ["recorrenciaId"])
    ]
)
data class Transacao(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val descricao: String,
    val valor: Double,
    val tipo: String, // RECEITA ou DESPESA
    val data: Long,
    val categoriaId: Long?,
    val contaId: Long?,
    val cartaoId: Long?,
    val pago: Boolean,
    val numeroParcelas: Int = 1,
    val parcelaAtual: Int = 1,
    val recorrenciaId: Long? = null
)