package com.zanini.snowwallet.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zanini.snowwallet.model.Notificacao
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificacaoDao {
    @Query("SELECT * FROM notificacoes ORDER BY data DESC")
    fun getTodas(): Flow<List<Notificacao>>

    @Query("SELECT COUNT(*) FROM notificacoes WHERE lida = 0")
    fun getQuantidadeNaoLidas(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(notificacao: Notificacao): Long

    @Update
    suspend fun atualizar(notificacao: Notificacao)

    @Delete
    suspend fun deletar(notificacao: Notificacao)

    @Query("DELETE FROM notificacoes")
    suspend fun limparTodas()
}