package com.zanini.snowwallet.data.local.dao

import androidx.room.*
import com.zanini.snowwallet.model.LancamentoRecorrente
import kotlinx.coroutines.flow.Flow

@Dao
interface LancamentoRecorrenteDao {
    @Query("SELECT * FROM lancamentos_recorrentes")
    fun getTodos(): Flow<List<LancamentoRecorrente>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(lancamento: LancamentoRecorrente): Long // Retorna o ID gerado

    @Update
    suspend fun atualizar(lancamento: LancamentoRecorrente)

    @Delete
    suspend fun deletar(lancamento: LancamentoRecorrente)
}