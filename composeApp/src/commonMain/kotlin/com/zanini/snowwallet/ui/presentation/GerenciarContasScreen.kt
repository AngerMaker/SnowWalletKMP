package com.zanini.snowwallet.ui.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.zanini.snowwallet.ui.presentation.components.AppTopBar
import com.zanini.snowwallet.ui.presentation.components.DropdownSelector
import com.zanini.snowwallet.util.toCurrency
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenciarContasScreen(
    navController: NavController,
    viewModel: GerenciarContasViewModel = koinInject()
) {
    val contas by viewModel.contasComSaldo.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var contaEmEdicao by remember { mutableStateOf<Conta?>(null) }

    Scaffold(
        topBar = { AppTopBar(title = "Minhas Contas", onBackClick = { navController.popBackStack() }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                contaEmEdicao = null
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Nova Conta")
            }
        }
    ) { padding ->
        if (contas.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Nenhuma conta cadastrada.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(contas) { item ->
                    ContaItem(
                        conta = item.conta,
                        saldoAtual = item.saldoAtual,
                        onEdit = {
                            contaEmEdicao = it
                            showDialog = true
                        },
                        onDelete = { viewModel.deletarConta(it) }
                    )
                }
            }
        }

        if (showDialog) {
            AddEditContaDialog(
                conta = contaEmEdicao,
                onDismiss = { showDialog = false },
                onSalvar = { nome, saldo, icone ->
                    viewModel.salvarConta(
                        id = contaEmEdicao?.id ?: 0,
                        nome = nome,
                        saldoInicial = saldo,
                        icone = icone
                    )
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun ContaItem(
    conta: Conta,
    saldoAtual: Double,
    onEdit: (Conta) -> Unit,
    onDelete: (Conta) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // USO SEGURO DO ÍCONE
                Icon(
                    imageVector = IconesDisponiveis.getIcone(conta.nomeIcone),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(conta.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Saldo: ${saldoAtual.toCurrency()}", style = MaterialTheme.typography.bodyMedium, color = if (saldoAtual >= 0) Color(0xFF4CAF50) else Color(0xFFF44336))
                }
            }
            Row {
                IconButton(onClick = { onEdit(conta) }) { Icon(Icons.Default.Edit, "Editar") }
                IconButton(onClick = { onDelete(conta) }) { Icon(Icons.Default.Delete, "Excluir") }
            }
        }
    }
}

@Composable
fun AddEditContaDialog(
    conta: Conta?,
    onDismiss: () -> Unit,
    onSalvar: (String, Double, String) -> Unit
) {
    var nome by remember { mutableStateOf(conta?.nome ?: "") }
    var saldoInicial by remember { mutableStateOf(conta?.saldoInicial?.toString() ?: "") }
    var iconeSelecionado by remember { mutableStateOf(conta?.nomeIcone ?: "AccountBalanceWallet") }

    // Pega a lista ordenada de nomes de ícones
    val listaIcones = remember { IconesDisponiveis.getNomesOrdenados() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (conta == null) "Nova Conta" else "Editar Conta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome da Conta") }
                )
                OutlinedTextField(
                    value = saldoInicial,
                    onValueChange = { saldoInicial = it },
                    label = { Text("Saldo Inicial (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // Seletor de Ícone
                DropdownSelector(
                    label = "Ícone",
                    items = listaIcones,
                    selectedItem = iconeSelecionado,
                    onItemSelected = { iconeSelecionado = it },
                    itemLabel = { it },
                    itemIcon = { IconesDisponiveis.getIcone(it) }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val saldoDouble = saldoInicial.replace(",", ".").toDoubleOrNull() ?: 0.0
                if (nome.isNotBlank()) {
                    onSalvar(nome, saldoDouble, iconeSelecionado)
                }
            }) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}