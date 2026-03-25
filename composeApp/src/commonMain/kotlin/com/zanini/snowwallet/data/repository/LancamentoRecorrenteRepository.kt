package com.zanini.snowwallet.data.repository

import com.zanini.snowwallet.data.local.dao.LancamentoRecorrenteDao
import com.zanini.snowwallet.data.local.dao.TransacaoDao
import com.zanini.snowwallet.model.LancamentoRecorrente
import com.zanini.snowwallet.model.Transacao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.*

class LancamentoRecorrenteRepository(
    private val dao: LancamentoRecorrenteDao,
    private val transacaoDao: TransacaoDao
) {
    fun getAll(): Flow<List<LancamentoRecorrente>> = dao.getTodos()
    suspend fun inserir(lancamento: LancamentoRecorrente): Long = dao.inserir(lancamento)
    suspend fun atualizar(lancamento: LancamentoRecorrente) = dao.atualizar(lancamento)
    suspend fun deletar(lancamento: LancamentoRecorrente) = dao.deletar(lancamento)

    // NOVA LÓGICA: Geração apenas incremental (não recria deletados)
    suspend fun processarRecorrencias() {
        val ativos = dao.getTodos().first().filter { it.ativo }
        val hoje = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        // Define o alvo: Queremos garantir transações até Hoje + 12 meses
        val dataLimite = LocalDate(hoje.year, hoje.monthNumber, 1).plus(1, DateTimeUnit.YEAR)

        ativos.forEach { recorrencia ->
            // Determina de onde começar a gerar
            var cursorMes: Int
            var cursorAno: Int

            if (recorrencia.ultimoAnoGerado == 0) {
                // Primeira vez: Começa do mês atual
                cursorMes = hoje.monthNumber
                cursorAno = hoje.year
            } else {
                // Já rodou antes: Começa do próximo mês após o último gerado
                val ultimaData = LocalDate(recorrencia.ultimoAnoGerado, recorrencia.ultimoMesGerado, 1)
                val proximaData = ultimaData.plus(1, DateTimeUnit.MONTH)
                cursorMes = proximaData.monthNumber
                cursorAno = proximaData.year
            }

            // Loop: Enquanto o cursor for menor ou igual à data limite (Daqui a 1 ano)
            var cursorData = LocalDate(cursorAno, cursorMes, 1)
            var precisaAtualizarRecorrencia = false
            var novoUltimoMes = recorrencia.ultimoMesGerado
            var novoUltimoAno = recorrencia.ultimoAnoGerado

            while (cursorData <= dataLimite) {
                // Cria a transação para este cursor
                val dia = recorrencia.diaVencimento.coerceIn(1, 31)

                // Ajuste preciso para garantir resguardo do último dia do mês para Fevereiro e meses curtos
                var safeDia = dia
                var safeDate: LocalDate? = null
                while (safeDia >= 28 && safeDate == null) {
                    try {
                        safeDate = LocalDate(cursorData.year, cursorData.monthNumber, safeDia)
                    } catch (e: Exception) {
                        safeDia--
                    }
                }
                val dataTransacaoFinal = safeDate ?: LocalDate(cursorData.year, cursorData.monthNumber, 28)

                val novaTransacao = Transacao(
                    descricao = recorrencia.descricao,
                    valor = recorrencia.valor,
                    tipo = "DESPESA",
                    data = dataTransacaoFinal.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
                    pago = false,
                    recorrenciaId = recorrencia.id,
                    categoriaId = recorrencia.categoriaId,
                    contaId = recorrencia.contaId,
                    cartaoId = recorrencia.cartaoId
                )
                transacaoDao.inserir(novaTransacao)

                // Atualiza marcadores
                novoUltimoMes = cursorData.monthNumber
                novoUltimoAno = cursorData.year
                precisaAtualizarRecorrencia = true

                // Avança o cursor
                cursorData = cursorData.plus(1, DateTimeUnit.MONTH)
            }

            // Salva o novo estado da recorrência (Último mês gerado)
            if (precisaAtualizarRecorrencia) {
                val recorrenciaAtualizada = recorrencia.copy(
                    ultimoMesGerado = novoUltimoMes,
                    ultimoAnoGerado = novoUltimoAno
                )
                dao.atualizar(recorrenciaAtualizada)
            }
        }
    }
}