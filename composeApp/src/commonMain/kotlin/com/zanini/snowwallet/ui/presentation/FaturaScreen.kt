package com.zanini.snowwallet.ui.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zanini.snowwallet.ui.presentation.components.AppTopBar
import com.zanini.snowwallet.ui.presentation.components.TransacaoItem
import com.zanini.snowwallet.util.toCurrency
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaturaScreen(
    navController: NavController,
    cartaoId: Long = 0L, // <--- ADICIONADO VALOR PADRÃO PARA EVITAR O ERRO
    viewModel: FaturaViewModel = koinInject()
) {
    // Garante que só busca se tiver um ID válido
    LaunchedEffect(cartaoId) {
        if (cartaoId != 0L) {
            viewModel.selecionarCartao(cartaoId)
        }
    }

    val state by viewModel.uiState.collectAsState()

    var showConfirmacao by remember { mutableStateOf(false) }

    LaunchedEffect(state.faturaPaga) {
        if (state.faturaPaga) {
            showConfirmacao = false
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = state.nomeCartao.ifBlank { "Fatura" }, onBackClick = { navController.popBackStack() }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Seletor Mês
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.mesAnterior() }) { Icon(Icons.Default.ChevronLeft, "Ant") }
                        Text("${state.mes}/${state.ano}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewModel.proximoMes() }) { Icon(Icons.Default.ChevronRight, "Prox") }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Total e Pagar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total da Fatura", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = state.valorTotal.toCurrency(),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        if (state.valorTotal > 0) {
                            Button(
                                onClick = {
                                    showConfirmacao = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimaryContainer)
                            ) {
                                Text("Pagar", color = MaterialTheme.colorScheme.primaryContainer)
                            }
                        } else if (state.transacoes.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Paga", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (state.contaVinculadaId == null && state.valorTotal > 0) {
                        Text(
                            "Aviso: Este cartão não tem conta vinculada.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            if (state.transacoes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma compra nesta fatura.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(state.transacoes) { transacao ->
                        // Componente visual da transação
                        TransacaoItem(transacao = transacao)
                    }
                }
            }
        }
    }

    // Dialog simples de confirmação
    if (showConfirmacao) {
        AlertDialog(
            onDismissRequest = { showConfirmacao = false },
            title = { Text("Confirmar Pagamento") },
            text = {
                Text("Deseja pagar a fatura de ${state.valorTotal.toCurrency()} debitando da conta \"${state.nomeContaVinculada}\"?")
            },
            confirmButton = {
                Button(onClick = { viewModel.pagarFatura() }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmacao = false }) { Text("Cancelar") }
            }
        )
    }
}