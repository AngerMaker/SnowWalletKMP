// src/commonMain/kotlin/com/zanini/snowwallet/ui/presentation/RecorrenciaScreen.kt
package com.zanini.snowwallet.ui.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zanini.snowwallet.model.Categoria
import com.zanini.snowwallet.ui.presentation.components.AppTopBar
import com.zanini.snowwallet.ui.presentation.components.DropdownSelector
import com.zanini.snowwallet.ui.presentation.IconesDisponiveis
import com.zanini.snowwallet.util.toCurrency
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecorrenciaScreen(
    navController: NavController,
    viewModel: RecorrenciaViewModel = koinInject()
) {
    val state by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    // --- DIÁLOGO DE TRATAMENTO DE ERROS ---
    if (state.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Atenção") },
            text = { Text(state.errorMessage!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    Scaffold(
        topBar = { AppTopBar(title = "Contas Fixas", onBackClick = { navController.popBackStack() }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar")
            }
        }
    ) { padding ->
        if (state.itens.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Nenhuma conta fixa cadastrada.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.itens) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Repeat, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(item.descricao, fontWeight = FontWeight.Bold)
                                    val origem = if (item.cartaoId != null) "Cartão" else "Conta"
                                    Text("Dia ${item.diaVencimento} • $origem", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.valor.toCurrency(), fontWeight = FontWeight.Bold)
                                IconButton(onClick = {
                                    viewModel.deletarRecorrencia(item)
                                }) {
                                    Icon(Icons.Default.Delete, "Excluir", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AddRecorrenciaDialog(
                categorias = state.categorias,
                opcoesPagamento = state.opcoesPagamento,
                onDismiss = { showDialog = false },
                onError = { message -> viewModel.emitError(message) },
                onConfirm = { desc, valor, dia, catId, contaId, cartaoId ->
                    viewModel.adicionarRecorrencia(desc, valor, dia, catId, contaId, cartaoId)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun AddRecorrenciaDialog(
    categorias: List<Categoria>,
    opcoesPagamento: List<OpcaoPagamento>,
    onDismiss: () -> Unit,
    onError: (String) -> Unit, // NOVO CALLBACK para enviar o erro para o ViewModel exibir
    onConfirm: (String, Double, Int, Long?, Long?, Long?) -> Unit
) {
    var descricao by remember { mutableStateOf("") }
    var valor by remember { mutableStateOf("") }
    var dia by remember { mutableStateOf("10") }

    var categoriaSelecionada by remember { mutableStateOf<Categoria?>(null) }
    var pagamentoSelecionado by remember { mutableStateOf<OpcaoPagamento?>(null) }

    // Seleciona a primeira opção por padrão, se houver, para mitigar o problema
    LaunchedEffect(opcoesPagamento) {
        if (opcoesPagamento.isNotEmpty() && pagamentoSelecionado == null) {
            pagamentoSelecionado = opcoesPagamento.first()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova Conta Fixa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = descricao,
                    onValueChange = { descricao = it },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = valor,
                    onValueChange = { valor = it },
                    label = { Text("Valor (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dia,
                    onValueChange = { dia = it },
                    label = { Text("Dia de Vencimento") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                DropdownSelector(
                    label = "Categoria",
                    items = categorias,
                    selectedItem = categoriaSelecionada,
                    itemLabel = { it.nome },
                    itemIcon = { IconesDisponiveis.getIcone(it.nomeIcone) },
                    onItemSelected = { categoriaSelecionada = it }
                )

                DropdownSelector(
                    label = "Forma de Pagamento",
                    items = opcoesPagamento,
                    selectedItem = pagamentoSelecionado,
                    itemLabel = { it.nome },
                    itemIcon = { IconesDisponiveis.getIcone(it.icone) },
                    onItemSelected = { pagamentoSelecionado = it }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val v = valor.replace(",", ".").toDoubleOrNull() ?: 0.0
                val d = dia.toIntOrNull() ?: 1

                var finalContaId: Long? = null
                var finalCartaoId: Long? = null

                if (pagamentoSelecionado is OpcaoPagamento.OpcaoConta) {
                    finalContaId = (pagamentoSelecionado as OpcaoPagamento.OpcaoConta).conta.id
                } else if (pagamentoSelecionado is OpcaoPagamento.OpcaoCartao) {
                    finalCartaoId = (pagamentoSelecionado as OpcaoPagamento.OpcaoCartao).cartao.id
                }

                // VALIDAÇÕES NA UI ANTES DE FECHAR O DIALOG
                if (descricao.isBlank() || v <= 0) {
                    onError("Preencha a descrição e um valor válido.")
                    return@Button
                }

                if (categoriaSelecionada == null) {
                    onError("Por favor, selecione uma Categoria.")
                    return@Button
                }

                if (finalContaId == null && finalCartaoId == null) {
                    onError("Por favor, selecione uma Forma de Pagamento.")
                    return@Button
                }

                onConfirm(descricao, v, d, categoriaSelecionada?.id, finalContaId, finalCartaoId)
            }) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}