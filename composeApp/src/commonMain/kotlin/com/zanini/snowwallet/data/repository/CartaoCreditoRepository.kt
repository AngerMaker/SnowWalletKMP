package com.zanini.snowwallet.data.repository

import com.zanini.snowwallet.data.local.dao.CartaoCreditoDao
import com.zanini.snowwallet.model.CartaoCredito
import kotlinx.coroutines.flow.Flow

class CartaoCreditoRepository(private val dao: CartaoCreditoDao) {

    fun getTodosCartoes(): Flow<List<CartaoCredito>> = dao.getTodosCartoes()

    suspend fun getCartaoById(id: Long): CartaoCredito? = dao.getCartaoById(id)

    suspend fun inserir(cartao: CartaoCredito) = dao.inserir(cartao)

    suspend fun atualizar(cartao: CartaoCredito) = dao.atualizar(cartao)

    suspend fun deletar(cartao: CartaoCredito) = dao.deletar(cartao)
}