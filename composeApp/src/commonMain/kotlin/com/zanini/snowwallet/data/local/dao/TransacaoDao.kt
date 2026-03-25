package com.zanini.snowwallet.data.local.dao

import androidx.room.*
import com.zanini.snowwallet.model.Transacao
import kotlinx.coroutines.flow.Flow

@Dao
interface TransacaoDao {
    @Query("SELECT * FROM transacoes ORDER BY data DESC")
    fun getTodasTransacoes(): Flow<List<Transacao>>

    @Query("SELECT * FROM transacoes WHERE id = :id")
    suspend fun getTransacaoById(id: Long): Transacao?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(transacao: Transacao)

    @Update
    suspend fun atualizar(transacao: Transacao)

    @Delete
    suspend fun deletar(transacao: Transacao)

    @Query("DELETE FROM transacoes WHERE id IN (:ids)")
    suspend fun deletarMultiplos(ids: List<Long>)

    @Query("DELETE FROM transacoes WHERE recorrenciaId = :recId AND pago = 0")
    suspend fun deletarRecorrentesNaoPagas(recId: Long)

    // --- QUERYS MELHORADAS PARA APAGAR AS FUTURAS ---
    @Query("DELETE FROM transacoes WHERE recorrenciaId = :recId AND data >= :dataMinima")
    suspend fun deletarRecorrentesAPartirDe(recId: Long, dataMinima: Long)

    // A busca da descrição agora recebe o padrão pronto do Kotlin
    @Query("DELETE FROM transacoes WHERE descricao LIKE :descricaoBasePattern AND numeroParcelas > 1 AND data >= :dataMinima")
    suspend fun deletarParcelasAPartirDe(descricaoBasePattern: String, dataMinima: Long)

    @Query("SELECT SUM(valor) FROM transacoes WHERE cartaoId = :cartaoId AND pago = 0")
    fun getGastoTotalNaoPagoCartao(cartaoId: Long): Flow<Double?>

    @Query("UPDATE transacoes SET pago = 1 WHERE id IN (:ids)")
    suspend fun marcarComoPago(ids: List<Long>)

    @Query("SELECT * FROM transacoes WHERE categoriaId = :categoriaId ORDER BY data DESC")
    fun getTransacoesPorCategoria(categoriaId: Long): Flow<List<Transacao>>

    @Query("SELECT * FROM transacoes WHERE cartaoId = :cartaoId ORDER BY data DESC")
    fun getTransacoesPorCartao(cartaoId: Long): Flow<List<Transacao>>
}