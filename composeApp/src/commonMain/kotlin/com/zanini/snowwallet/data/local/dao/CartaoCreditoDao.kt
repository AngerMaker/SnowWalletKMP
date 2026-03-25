package com.zanini.snowwallet.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zanini.snowwallet.model.CartaoCredito
import kotlinx.coroutines.flow.Flow

@Dao
interface CartaoCreditoDao {
    @Query("SELECT * FROM cartoes_credito")
    fun getTodosCartoes(): Flow<List<CartaoCredito>>

    @Query("SELECT * FROM cartoes_credito WHERE id = :id")
    suspend fun getCartaoById(id: Long): CartaoCredito?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(cartao: CartaoCredito)

    @Update
    suspend fun atualizar(cartao: CartaoCredito)

    @Delete
    suspend fun deletar(cartao: CartaoCredito)
}