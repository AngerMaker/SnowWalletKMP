package com.zanini.snowwallet.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zanini.snowwallet.model.Conta
import kotlinx.coroutines.flow.Flow

@Dao
interface ContaDao {
    @Query("SELECT * FROM contas")
    fun getAllContas(): Flow<List<Conta>>

    @Query("SELECT * FROM contas WHERE id = :id")
    suspend fun getContaById(id: Long): Conta?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConta(conta: Conta)

    @Update
    suspend fun updateConta(conta: Conta)

    @Delete
    suspend fun deleteConta(conta: Conta)

    // Método para pegar todas as contas como lista direta (sem Flow)
    @Query("SELECT * FROM contas")
    suspend fun getAllContasOneShot(): List<Conta>
}