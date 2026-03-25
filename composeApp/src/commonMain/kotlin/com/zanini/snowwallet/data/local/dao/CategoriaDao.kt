// src/commonMain/kotlin/com/zanini/snowwallet/data/local/dao/CategoriaDao.kt
package com.zanini.snowwallet.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zanini.snowwallet.model.Categoria
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaDao {
    // ATUALIZADO: Adicionado ORDER BY nome ASC para garantir a ordem alfabética em todo o app
    @Query("SELECT * FROM categorias ORDER BY nome ASC")
    fun getAll(): Flow<List<Categoria>>

    // ATUALIZADO: Ordem alfabética também na busca direta
    @Query("SELECT * FROM categorias ORDER BY nome ASC")
    suspend fun getAllList(): List<Categoria>

    // Busca por nome para evitar duplicar na inserção
    @Query("SELECT * FROM categorias WHERE nome = :nome LIMIT 1")
    suspend fun buscarPorNome(nome: String): Categoria?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(categoria: Categoria): Long

    @Update
    suspend fun atualizar(categoria: Categoria)

    @Delete
    suspend fun deletar(categoria: Categoria)

    // Deletar lista (para limpeza de duplicatas)
    @Delete
    suspend fun deletarLista(categorias: List<Categoria>)

    @Query("SELECT COUNT(*) FROM categorias")
    suspend fun contarCategorias(): Int
}