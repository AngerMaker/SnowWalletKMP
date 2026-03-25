package com.zanini.snowwallet.data.repository

import com.zanini.snowwallet.data.local.dao.NotificacaoDao
import com.zanini.snowwallet.model.Notificacao
import kotlinx.coroutines.flow.Flow

class NotificacaoRepository(private val dao: NotificacaoDao) {

    val todasNotificacoes: Flow<List<Notificacao>> = dao.getTodas()

    val quantidadeNaoLidas: Flow<Int> = dao.getQuantidadeNaoLidas()

    suspend fun inserir(notificacao: Notificacao): Long = dao.inserir(notificacao)

    suspend fun marcarComoLida(notificacao: Notificacao) {
        dao.atualizar(notificacao.copy(lida = true))
    }

    suspend fun deletar(notificacao: Notificacao) = dao.deletar(notificacao)

    suspend fun limparTodas() = dao.limparTodas()
}