package com.zanini.snowwallet.di

import com.zanini.snowwallet.data.local.AppDatabase
import com.zanini.snowwallet.data.repository.*
import com.zanini.snowwallet.ui.presentation.*
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun initKoin(additionalModules: List<Module> = emptyList()) = module {
    includes(appModule, platformModule)
    includes(additionalModules)
}

// Este módulo será preenchido pelo Android ou Desktop (contendo o Banco e o Backup)
expect val platformModule: Module

val appModule = module {
    // --- Database ---
    // AQUI ESTAVA O ERRO: Removemos a criação do banco daqui.
    // O 'AppDatabase' agora vem injetado do 'platformModule'.

    // --- DAOs ---
    // O Koin pega o AppDatabase fornecido pela plataforma e extrai os DAOs
    single { get<AppDatabase>().transacaoDao() }
    single { get<AppDatabase>().contaDao() }
    single { get<AppDatabase>().categoriaDao() }
    single { get<AppDatabase>().cartaoCreditoDao() }
    single { get<AppDatabase>().lancamentoRecorrenteDao() }
    single { get<AppDatabase>().emprestimoDao() }
    single { get<AppDatabase>().notificacaoDao() }

    // --- Repositories ---
    single { TransacaoRepository(get(), get(), get()) }
    singleOf(::ContaRepository)
    singleOf(::CategoriaRepository)
    singleOf(::CartaoCreditoRepository)
    single { LancamentoRecorrenteRepository(get(), get()) }
    singleOf(::EmprestimoRepository)
    singleOf(::NotificacaoRepository)
    single { UserPreferencesRepository(get()) }

    // --- ViewModels ---
    factoryOf(::HomeViewModel)
    factoryOf(::AddEditTransactionViewModel)
    factoryOf(::GerenciarContasViewModel)
    factoryOf(::RecorrenciaViewModel)
    factoryOf(::TransactionListViewModel)
    factoryOf(::GerenciarCartoesViewModel)
    factoryOf(::FaturaViewModel)
    factoryOf(::ConfiguracoesViewModel)
    factoryOf(::GerenciarCategoriasViewModel)
    factoryOf(::BackupViewModel)
    factoryOf(::SearchViewModel)
    factoryOf(::NotificacoesViewModel)
    factoryOf(::RelatoriosViewModel)
    factoryOf(::CategoryTransactionsViewModel)
    factoryOf(::EmprestimosViewModel)
    factoryOf(::TransferenciaViewModel)
}

// REMOVIDO: expect fun getDatabaseBuilder()
// (Não precisamos mais dele, pois cada plataforma cria seu banco internamente)