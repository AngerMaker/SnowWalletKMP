package com.zanini.snowwallet.data.repository

import com.zanini.snowwallet.data.local.dao.EmprestimoDao
import com.zanini.snowwallet.model.Emprestimo
import kotlinx.coroutines.flow.Flow

class EmprestimoRepository(private val dao: EmprestimoDao) {

    fun getTodos(): Flow<List<Emprestimo>> = dao.getTodos()

    fun getPendentes(): Flow<List<Emprestimo>> = dao.getPendentes()

    suspend fun getById(id: Long): Emprestimo? = dao.getById(id)

    suspend fun inserir(emprestimo: Emprestimo) = dao.inserir(emprestimo)

    suspend fun atualizar(emprestimo: Emprestimo) = dao.atualizar(emprestimo)

    suspend fun deletar(emprestimo: Emprestimo) = dao.deletar(emprestimo)
}