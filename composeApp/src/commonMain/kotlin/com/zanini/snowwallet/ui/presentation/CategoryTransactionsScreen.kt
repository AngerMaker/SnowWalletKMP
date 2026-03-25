package com.zanini.snowwallet.ui.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Import Novo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zanini.snowwallet.ui.presentation.components.TransacaoItem
import com.zanini.snowwallet.util.toCurrency
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTransactionsScreen(
    navController: NavController,
    viewModel: CategoryTransactionsViewModel = koinInject()
) {
    val backStackEntry = navController.currentBackStackEntry
    val categoryId = backStackEntry?.arguments?.getLong("categoryId") ?: 0L
    val month = backStackEntry?.arguments?.getInt("month") ?: 1
    val year = backStackEntry?.arguments?.getInt("year") ?: 2024

    LaunchedEffect(categoryId) {
        viewModel.init(categoryId, month, year)
    }

    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.nomeCategoria) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        // CORREÇÃO: Ícone AutoMirrored
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total na Categoria", style = MaterialTheme.typography.labelMedium)
                    Text(state.total.toCurrency(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }

            if (state.transacoes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma transação encontrada.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.transacoes) { transacao ->
                        TransacaoItem(transacao = transacao)
                    }
                }
            }
        }
    }
}