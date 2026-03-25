// src/commonMain/kotlin/com/zanini/snowwallet/App.kt
package com.zanini.snowwallet

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zanini.snowwallet.data.repository.UserPreferencesRepository
import com.zanini.snowwallet.ui.presentation.*
import com.zanini.snowwallet.ui.theme.SnowWalletTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
@Preview
fun App(
    deepLinkDestination: String? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val userPreferences: UserPreferencesRepository = koinInject()
    val darkModePref by userPreferences.isDarkMode.collectAsState(initial = isSystemInDarkTheme())
    val appThemeColor by userPreferences.appThemeColor.collectAsState(initial = "PURPLE") // NOVO

    // NOVO: Passando a cor para o tema
    SnowWalletTheme(darkTheme = darkModePref, themeColor = appThemeColor) {
        Surface(color = MaterialTheme.colorScheme.background) {
            val navController = rememberNavController()

            LaunchedEffect(deepLinkDestination) {
                if (deepLinkDestination != null) {
                    navController.navigate(deepLinkDestination)
                    onDeepLinkConsumed()
                }
            }

            NavHost(navController = navController, startDestination = "home") {
                composable("home") { HomeScreen(navController) }

                composable("add_receita") {
                    AddEditTransactionScreen(navController = navController, initialTipo = "RECEITA", transactionId = 0L, cartaoId = null)
                }
                composable("add_despesa") {
                    AddEditTransactionScreen(navController = navController, initialTipo = "DESPESA", transactionId = 0L, cartaoId = null)
                }

                composable("gerenciar_cartoes") { GerenciarCartoesScreen(navController) }
                composable("gerenciar_contas") { GerenciarContasScreen(navController) }
                composable("gerenciar_categorias") { GerenciarCategoriasScreen(navController) }

                composable("backup") { BackupScreen(navController) }
                composable("listagem") { TransactionListScreen(navController) }
                composable("configuracoes") { ConfiguracoesScreen(navController) }
                composable("recorrencia") { RecorrenciaScreen(navController) }
                composable("emprestimos") { EmprestimosScreen(navController) }
                composable("relatorios") { RelatoriosScreen(navController) }
                composable("search") { SearchScreen(navController) }
                composable("notificacoes") { NotificacoesScreen(navController) }
                composable("transferencia") { TransferenciaScreen(navController) }

                composable(
                    "add_edit_transaction?id={id}&tipo={tipo}",
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                        navArgument("tipo") { type = NavType.StringType; defaultValue = "DESPESA" }
                    )
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getLong("id") ?: 0L
                    val tipo = backStackEntry.arguments?.getString("tipo") ?: "DESPESA"

                    AddEditTransactionScreen(navController = navController, transactionId = id, initialTipo = tipo, cartaoId = null)
                }

                composable(
                    "category_transactions/{categoryId}?month={month}&year={year}",
                    arguments = listOf(
                        navArgument("categoryId") { type = NavType.LongType },
                        navArgument("month") { type = NavType.IntType },
                        navArgument("year") { type = NavType.IntType }
                    )
                ) {
                    CategoryTransactionsScreen(navController)
                }

                composable(
                    "fatura/{cartaoId}",
                    arguments = listOf(navArgument("cartaoId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val cartaoId = backStackEntry.arguments?.getLong("cartaoId") ?: 0L
                    FaturaScreen(navController, cartaoId = cartaoId)
                }
            }
        }
    }
}