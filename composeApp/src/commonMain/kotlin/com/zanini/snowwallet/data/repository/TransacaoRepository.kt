package com.zanini.snowwallet.data.repository

import com.zanini.snowwallet.data.local.dao.CartaoCreditoDao
import com.zanini.snowwallet.data.local.dao.CategoriaDao
import com.zanini.snowwallet.data.local.dao.TransacaoDao
import com.zanini.snowwallet.model.GastoPorCategoria
import com.zanini.snowwallet.model.Transacao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class TransacaoRepository(
    private val dao: TransacaoDao,
    private val cartaoDao: CartaoCreditoDao,
    private val categoriaDao: CategoriaDao
) {
    fun getAllTransacoes(): Flow<List<Transacao>> = dao.getTodasTransacoes()
    suspend fun getTransacaoById(id: Long): Transacao? = dao.getTransacaoById(id)
    suspend fun inserir(transacao: Transacao) = dao.inserir(transacao)
    suspend fun atualizar(transacao: Transacao) = dao.atualizar(transacao)
    suspend fun deletar(transacao: Transacao) = dao.deletar(transacao)
    suspend fun deletarMultiplos(ids: List<Long>) = dao.deletarMultiplos(ids)

    suspend fun deletarTransacoesRecorrentesPendentes(recorrenciaId: Long) {
        dao.deletarRecorrentesNaoPagas(recorrenciaId)
    }

    // --- REPASSE DAS NOVAS QUERYS ---
    suspend fun deletarRecorrentesFuturas(recorrenciaId: Long, dataMinima: Long) {
        dao.deletarRecorrentesAPartirDe(recorrenciaId, dataMinima)
    }

    suspend fun deletarParcelasFuturas(descricaoBasePattern: String, dataMinima: Long) {
        dao.deletarParcelasAPartirDe(descricaoBasePattern, dataMinima)
    }

    fun getLimiteUsado(cartaoId: Long): Flow<Double> = dao.getGastoTotalNaoPagoCartao(cartaoId).map { it ?: 0.0 }
    suspend fun marcarFaturaComoPaga(idsTransacoes: List<Long>) = dao.marcarComoPago(idsTransacoes)

    fun getTransacoesPorCategoria(categoriaId: Long) = dao.getTransacoesPorCategoria(categoriaId)
    fun getTransacoesPorIntervaloCartao(cartaoId: Long, inicio: Long, fim: Long) = dao.getTransacoesPorCartao(cartaoId).map { l -> l.filter { it.data in inicio..fim } }

    fun getGastosPorCategoria(mes: Int, ano: Int): Flow<List<GastoPorCategoria>> {
        return combine(dao.getTodasTransacoes(), categoriaDao.getAll()) { transacoes, categorias ->
            val filtradas = transacoes.filter {
                val data = Instant.fromEpochMilliseconds(it.data).toLocalDateTime(TimeZone.currentSystemDefault())
                data.monthNumber == mes && data.year == ano && it.tipo == "DESPESA" && it.categoriaId != null
            }
            val listaBasica = filtradas.groupBy { it.categoriaId!! }.map { entry ->
                val catId = entry.key
                val lista = entry.value
                val nome = categorias.find { it.id == catId }?.nome ?: "Desconhecido"
                GastoPorCategoria(nome, lista.sumOf { it.valor }, catId, "#000000")
            }
            val cores = listOf("#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50")
            listaBasica.sortedByDescending { it.valorTotal }.mapIndexed { index, item ->
                item.copy(cor = cores[index % cores.size])
            }
        }
    }

    fun getReceitasPorCategoria(mes: Int, ano: Int): Flow<List<GastoPorCategoria>> {
        return combine(dao.getTodasTransacoes(), categoriaDao.getAll()) { transacoes, categorias ->
            val filtradas = transacoes.filter {
                val data = Instant.fromEpochMilliseconds(it.data).toLocalDateTime(TimeZone.currentSystemDefault())
                data.monthNumber == mes && data.year == ano && it.tipo == "RECEITA"
            }
            val listaBasica = filtradas.groupBy { it.categoriaId ?: -1L }.map { entry ->
                val catId = entry.key
                val lista = entry.value
                val nome = categorias.find { it.id == catId }?.nome ?: "Outros"
                GastoPorCategoria(nome, lista.sumOf { it.valor }, catId, "#000000")
            }
            val cores = listOf("#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50")
            listaBasica.sortedByDescending { it.valorTotal }.mapIndexed { index, item ->
                item.copy(cor = cores[(index + 5) % cores.size])
            }
        }
    }
}