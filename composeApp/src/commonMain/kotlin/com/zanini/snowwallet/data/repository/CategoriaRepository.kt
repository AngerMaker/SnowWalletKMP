// src/commonMain/kotlin/com/zanini/snowwallet/data/repository/CategoriaRepository.kt
package com.zanini.snowwallet.data.repository

import com.zanini.snowwallet.data.local.AppDatabase
import com.zanini.snowwallet.model.Categoria
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class CategoriaRepository(private val database: AppDatabase) {

    private val dao = database.categoriaDao()
    private val initMutex = Mutex()

    fun getTodasCategorias(): Flow<List<Categoria>> {
        // A ordenação agora é garantida diretamente no DAO pelo banco de dados
        return dao.getAll()
    }

    suspend fun buscarPorNome(nome: String): Categoria? {
        return dao.buscarPorNome(nome)
    }

    suspend fun inicializarPadroes() {
        withContext(Dispatchers.IO) {
            initMutex.withLock {
                // 1. Limpeza de Duplicatas
                val todas = dao.getAllList()
                if (todas.isNotEmpty()) {
                    val agrupadas = todas.groupBy { it.nome }
                    val paraDeletar = mutableListOf<Categoria>()
                    agrupadas.forEach { (_, lista) ->
                        if (lista.size > 1) paraDeletar.addAll(lista.drop(1))
                    }
                    if (paraDeletar.isNotEmpty()) dao.deletarLista(paraDeletar)
                }

                // 2. Lista de Categorias
                val padroesRaw = listOf(
                    // --- Essenciais (Anteriores) ---
                    Categoria(nome = "Alimentação", nomeIcone = "Restaurant", cor = "FF5722", tipo = "DESPESA"),
                    Categoria(nome = "Mercado", nomeIcone = "LocalGroceryStore", cor = "FF9800", tipo = "DESPESA"),
                    Categoria(nome = "Transporte", nomeIcone = "DirectionsCar", cor = "2196F3", tipo = "DESPESA"),
                    Categoria(nome = "Combustível", nomeIcone = "LocalGasStation", cor = "E65100", tipo = "DESPESA"),
                    Categoria(nome = "Moradia", nomeIcone = "Home", cor = "795548", tipo = "DESPESA"),
                    Categoria(nome = "Contas", nomeIcone = "Lightbulb", cor = "FBC02D", tipo = "DESPESA"),

                    // --- Saúde & Pessoal (Anteriores) ---
                    Categoria(nome = "Saúde", nomeIcone = "LocalHospital", cor = "F44336", tipo = "DESPESA"),
                    Categoria(nome = "Farmácia", nomeIcone = "LocalPharmacy", cor = "D32F2F", tipo = "DESPESA"),
                    Categoria(nome = "Beleza", nomeIcone = "Face", cor = "E91E63", tipo = "DESPESA"),
                    Categoria(nome = "Roupas", nomeIcone = "ShoppingBag", cor = "880E4F", tipo = "DESPESA"),

                    // --- Estilo de Vida (Anteriores) ---
                    Categoria(nome = "Lazer", nomeIcone = "Movie", cor = "9C27B0", tipo = "DESPESA"),
                    Categoria(nome = "Viagem", nomeIcone = "Flight", cor = "03A9F4", tipo = "DESPESA"),
                    Categoria(nome = "Pets", nomeIcone = "Pets", cor = "5D4037", tipo = "DESPESA"),
                    Categoria(nome = "Educação", nomeIcone = "School", cor = "3F51B5", tipo = "DESPESA"),
                    Categoria(nome = "Presentes", nomeIcone = "CardGiftcard", cor = "FF4081", tipo = "DESPESA"),

                    // --- Outros (Anteriores) ---
                    Categoria(nome = "Assinaturas", nomeIcone = "Tv", cor = "673AB7", tipo = "DESPESA"),
                    Categoria(nome = "Manutenção", nomeIcone = "Build", cor = "607D8B", tipo = "DESPESA"),
                    Categoria(nome = "Outros", nomeIcone = "Category", cor = "9E9E9E", tipo = "DESPESA"),

                    // --- Receitas (Anteriores) ---
                    Categoria(nome = "Salário", nomeIcone = "AttachMoney", cor = "4CAF50", tipo = "RECEITA"),
                    Categoria(nome = "Investimentos", nomeIcone = "TrendingUp", cor = "009688", tipo = "RECEITA"),
                    Categoria(nome = "Extras", nomeIcone = "Savings", cor = "8BC34A", tipo = "RECEITA"),

                    // ==========================================
                    // --- NOVAS CATEGORIAS ADICIONADAS ---
                    // ==========================================
                    Categoria(nome = "Empréstimos", nomeIcone = "AccountBalance", cor = "9E9E9E", tipo = "DESPESA"),
                    Categoria(nome = "Jogos", nomeIcone = "SportsEsports", cor = "673AB7", tipo = "DESPESA"),
                    Categoria(nome = "Eletrônicos", nomeIcone = "Devices", cor = "607D8B", tipo = "DESPESA"),
                    Categoria(nome = "Eletrodomésticos", nomeIcone = "Kitchen", cor = "795548", tipo = "DESPESA"),
                    Categoria(nome = "Doação", nomeIcone = "Favorite", cor = "E91E63", tipo = "DESPESA"),

                    // --- Sugestões extras ---
                    Categoria(nome = "Impostos", nomeIcone = "Receipt", cor = "D32F2F", tipo = "DESPESA"),
                    Categoria(nome = "Serviços", nomeIcone = "HomeRepairService", cor = "5D4037", tipo = "DESPESA")
                )

                // Ordena a lista alfabeticamente antes de inserir na inicialização
                val padroesOrdenados = padroesRaw.sortedBy { it.nome }

                padroesOrdenados.forEach { padrao ->
                    val existente = dao.buscarPorNome(padrao.nome)
                    if (existente == null) {
                        dao.inserir(padrao)
                    } else {
                        // Atualiza ícone/cor se necessário para manter o app atualizado
                        if (existente.nomeIcone != padrao.nomeIcone) {
                            dao.atualizar(existente.copy(nomeIcone = padrao.nomeIcone, cor = padrao.cor))
                        }
                    }
                }
            }
        }
    }

    suspend fun inserir(categoria: Categoria) = dao.inserir(categoria)
    suspend fun atualizar(categoria: Categoria) = dao.atualizar(categoria)
    suspend fun deletar(categoria: Categoria) = dao.deletar(categoria)
}