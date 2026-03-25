package com.zanini.snowwallet.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zanini.snowwallet.model.Emprestimo
import kotlinx.coroutines.flow.Flow

@Dao
interface EmprestimoDao {
    @Query("SELECT * FROM emprestimos ORDER BY dataEmprestimo DESC")
    fun getTodos(): Flow<List<Emprestimo>>

    @Query("SELECT * FROM emprestimos WHERE id = :id")
    suspend fun getById(id: Long): Emprestimo?

    @Query("SELECT * FROM emprestimos WHERE finalizado = 0")
    fun getPendentes(): Flow<List<Emprestimo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(emprestimo: Emprestimo)

    @Update
    suspend fun atualizar(emprestimo: Emprestimo)

    @Delete
    suspend fun deletar(emprestimo: Emprestimo)
}