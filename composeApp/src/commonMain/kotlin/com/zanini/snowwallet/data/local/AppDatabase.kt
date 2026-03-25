package com.zanini.snowwallet.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zanini.snowwallet.data.local.dao.CartaoCreditoDao
import com.zanini.snowwallet.data.local.dao.CategoriaDao
import com.zanini.snowwallet.data.local.dao.ContaDao
import com.zanini.snowwallet.data.local.dao.EmprestimoDao
import com.zanini.snowwallet.data.local.dao.LancamentoRecorrenteDao
import com.zanini.snowwallet.data.local.dao.NotificacaoDao
import com.zanini.snowwallet.data.local.dao.TransacaoDao
import com.zanini.snowwallet.model.CartaoCredito
import com.zanini.snowwallet.model.Categoria
import com.zanini.snowwallet.model.Conta
import com.zanini.snowwallet.model.Emprestimo
import com.zanini.snowwallet.model.LancamentoRecorrente
import com.zanini.snowwallet.model.Notificacao
import com.zanini.snowwallet.model.Transacao

@Database(
    entities = [
        Transacao::class,
        Conta::class,
        CartaoCredito::class,
        Categoria::class,
        LancamentoRecorrente::class,
        Emprestimo::class,
        Notificacao::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    // Declaração dos DAOs com os imports corretos acima
    abstract fun transacaoDao(): TransacaoDao
    abstract fun contaDao(): ContaDao
    abstract fun cartaoCreditoDao(): CartaoCreditoDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun lancamentoRecorrenteDao(): LancamentoRecorrenteDao
    abstract fun emprestimoDao(): EmprestimoDao
    abstract fun notificacaoDao(): NotificacaoDao
}