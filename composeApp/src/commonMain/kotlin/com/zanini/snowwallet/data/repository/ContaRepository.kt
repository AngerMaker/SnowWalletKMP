package com.zanini.snowwallet.data.repository

import com.zanini.snowwallet.data.local.dao.ContaDao
import com.zanini.snowwallet.model.Conta
import kotlinx.coroutines.flow.Flow

class ContaRepository(
    private val dao: ContaDao
) {
    fun getTodasContas(): Flow<List<Conta>> = dao.getAllContas()

    suspend fun getContaById(id: Long): Conta? = dao.getContaById(id)

    suspend fun inserir(conta: Conta) = dao.insertConta(conta)

    suspend fun atualizar(conta: Conta) = dao.updateConta(conta)

    suspend fun deletar(conta: Conta) = dao.deleteConta(conta)

    // Usado em casos onde não queremos observar (Flow), apenas pegar o valor atual
    suspend fun getTodasContasOneShot(): List<Conta> = dao.getAllContasOneShot()
}