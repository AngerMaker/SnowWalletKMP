// src/commonMain/kotlin/com/zanini/snowwallet/ui/presentation/EmprestimosScreen.kt
package com.zanini.snowwallet.ui.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zanini.snowwallet.model.Conta
import com.zanini.snowwallet.model.Emprestimo
import com.zanini.snowwallet.ui.presentation.components.AppTopBar
import com.zanini.snowwallet.ui.theme.GreenIncome
import com.zanini.snowwallet.ui.theme.RedExpense
import com.zanini.snowwallet.util.toCurrency
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmprestimosScreen(
    navController: NavController,
    viewModel: EmprestimosViewModel = koinInject()
) {
    val state by viewModel.uiState.collectAsState()
    val contas by viewModel.contas.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var emprestimoParaAbater by remember { mutableStateOf<Emprestimo?>(null) }
    var emprestimoParaEditar by remember { mutableStateOf<Emprestimo?>(null) } // NOVO: Controle de Edição

    Scaffold(
        topBar = { AppTopBar(title = "Empréstimos", onBackClick = { navController.popBackStack() }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Novo")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            // Resumo
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ResumoCard("A Receber", state.totalAReceber, GreenIncome, Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                ResumoCard("A Pagar", state.totalAPagar, RedExpense, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.lista) { item ->
                    EmprestimoItem(
                        item = item,
                        onAbater = { emprestimoParaAbater = item },
                        onEdit = { emprestimoParaEditar = item }, // Ação de Edição
                        onDelete = { viewModel.deletar(item) }
                    )
                }
            }
        }

        // Reutilizamos o Dialog para Adição e Edição
        if (showAddDialog || emprestimoParaEditar != null) {
            AddEditEmprestimoDialog(
                emprestimo = emprestimoParaEditar,
                onDismiss = {
                    showAddDialog = false
                    emprestimoParaEditar = null
                },
                onConfirm = { desc, valTotal, pessoa, tipo ->
                    if (emprestimoParaEditar != null) {
                        viewModel.editarEmprestimo(emprestimoParaEditar!!, desc, valTotal, pessoa, tipo, null)
                    } else {
                        viewModel.salvarEmprestimo(desc, valTotal, pessoa, tipo, null)
                    }
                    showAddDialog = false
                    emprestimoParaEditar = null
                }
            )
        }

        if (emprestimoParaAbater != null) {
            AbaterDialog(
                emprestimo = emprestimoParaAbater!!,
                contas = contas,
                onDismiss = { emprestimoParaAbater = null },
                onConfirm = { valor, gerarTransacao, contaId ->
                    viewModel.abaterValor(emprestimoParaAbater!!, valor, gerarTransacao, contaId)
                    emprestimoParaAbater = null
                }
            )
        }
    }
}

@Composable
fun ResumoCard(titulo: String, valor: Double, cor: Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = cor.copy(alpha = 0.1f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.bodyMedium, color = cor)
            Text(valor.toCurrency(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cor)
        }
    }
}

@Composable
fun EmprestimoItem(item: Emprestimo, onAbater: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val progresso = if(item.valorTotal > 0) (item.valorPago / item.valorTotal).toFloat() else 0f
    val corStatus = if (item.tipo == "CONCEDIDO") GreenIncome else RedExpense

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, tint = corStatus)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(item.pessoa, fontWeight = FontWeight.Bold)
                        Text(item.descricao, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Editar", tint = Color.Gray) }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Excluir", tint = MaterialTheme.colorScheme.error) }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = { progresso }, modifier = Modifier.fillMaxWidth(), color = corStatus)
            Spacer(modifier = Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Pago: ${item.valorPago.toCurrency()}", style = MaterialTheme.typography.bodySmall)
                Text("Total: ${item.valorTotal.toCurrency()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }

            if (!item.finalizado) {
                TextButton(onClick = onAbater, modifier = Modifier.align(Alignment.End)) {
                    Text("Abater Valor")
                }
            } else {
                Text("Finalizado", color = Color.Gray, modifier = Modifier.align(Alignment.End).padding(top = 8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEmprestimoDialog(
    emprestimo: Emprestimo?,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String) -> Unit
) {
    var descricao by remember { mutableStateOf(emprestimo?.descricao ?: "") }
    var valor by remember { mutableStateOf(emprestimo?.valorTotal?.toString() ?: "") }
    var pessoa by remember { mutableStateOf(emprestimo?.pessoa ?: "") }
    var tipo by remember { mutableStateOf(emprestimo?.tipo ?: "CONCEDIDO") }

    val titulo = if (emprestimo == null) "Novo Empréstimo" else "Editar Empréstimo"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = descricao, onValueChange = { descricao = it }, label = { Text("Descrição") })
                OutlinedTextField(value = pessoa, onValueChange = { pessoa = it }, label = { Text("Pessoa") })
                OutlinedTextField(value = valor, onValueChange = { valor = it }, label = { Text("Valor Total") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

                Row {
                    FilterChip(selected = tipo == "CONCEDIDO", onClick = { tipo = "CONCEDIDO" }, label = { Text("Eu Emprestei") })
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(selected = tipo == "TOMADO", onClick = { tipo = "TOMADO" }, label = { Text("Peguei Emprestado") })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val v = valor.replace(",", ".").toDoubleOrNull() ?: 0.0
                if (descricao.isNotBlank() && v > 0) onConfirm(descricao, v, pessoa, tipo)
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbaterDialog(
    emprestimo: Emprestimo,
    contas: List<Conta>,
    onDismiss: () -> Unit,
    onConfirm: (Double, Boolean, Long?) -> Unit
) {
    var valor by remember { mutableStateOf("") }
    var gerarTransacao by remember { mutableStateOf(false) }
    var selectedConta by remember { mutableStateOf<Conta?>(contas.firstOrNull()) }
    var expandedContaMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Abater Valor") },
        text = {
            Column {
                Text("Restante: ${(emprestimo.valorTotal - emprestimo.valorPago).toCurrency()}")
                OutlinedTextField(
                    value = valor,
                    onValueChange = { valor = it },
                    label = { Text("Valor a pagar agora") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = gerarTransacao,
                        onCheckedChange = { gerarTransacao = it }
                    )
                    Text("Gerar transação na carteira", style = MaterialTheme.typography.bodyMedium)
                }

                if (gerarTransacao) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = expandedContaMenu,
                        onExpandedChange = { expandedContaMenu = it }
                    ) {
                        OutlinedTextField(
                            value = selectedConta?.nome ?: "Selecione uma conta",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Conta de Destino/Origem") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedContaMenu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedContaMenu,
                            onDismissRequest = { expandedContaMenu = false }
                        ) {
                            contas.forEach { conta ->
                                DropdownMenuItem(
                                    text = { Text(conta.nome) },
                                    onClick = {
                                        selectedConta = conta
                                        expandedContaMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val v = valor.replace(",", ".").toDoubleOrNull() ?: 0.0
                if (v > 0) {
                    onConfirm(v, gerarTransacao, if (gerarTransacao) selectedConta?.id else null)
                }
            }) { Text("Confirmar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}