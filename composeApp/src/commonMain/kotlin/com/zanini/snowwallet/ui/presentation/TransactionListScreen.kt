package com.zanini.snowwallet.ui.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zanini.snowwallet.model.Transacao
import com.zanini.snowwallet.ui.theme.GreenIncome
import com.zanini.snowwallet.ui.theme.RedExpense
import com.zanini.snowwallet.util.toCurrency
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen( // Nome da função deve ser único se usado em navegação
    navController: NavController,
    viewModel: TransactionListViewModel = koinInject()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            if (state.isSelectionMode) {
                TopAppBar(
                    title = { Text("${state.selectedIds.size} selecionado(s)") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) { Icon(Icons.Default.Close, "Cancelar") }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.deleteSelected() }) {
                            Icon(Icons.Default.Delete, "Excluir", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }
    ) { padding ->
        if (state.transacoes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Nenhuma transação encontrada.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                // Agrupamento simples por data
                val agrupadas = state.transacoes.groupBy {
                    val instant = Instant.fromEpochMilliseconds(it.data)
                    instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
                }

                agrupadas.forEach { (data, lista) ->
                    item {
                        Text(
                            text = "${data.dayOfMonth}/${data.monthNumber}/${data.year}",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(lista) { transacao ->
                        val isSelected = state.selectedIds.contains(transacao.id)
                        TransactionListItemSelectable(
                            transacao = transacao,
                            isSelectionMode = state.isSelectionMode,
                            isSelected = isSelected,
                            onLongClick = { viewModel.toggleSelection(transacao.id) },
                            onClick = {
                                if (state.isSelectionMode) viewModel.toggleSelection(transacao.id)
                                else navController.navigate("add_edit_transaction?id=${transacao.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionListItemSelectable(
    transacao: Transacao,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    val isReceita = transacao.tipo == "RECEITA"
    val color = if (isReceita) GreenIncome else RedExpense
    val icon = if (isReceita) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward

    val cardColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val border = if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CardDefaults.shape) else Modifier

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .then(border)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isSelectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onClick() })
                Spacer(modifier = Modifier.width(8.dp))
            }
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(transacao.descricao, fontWeight = FontWeight.SemiBold)
                Text(
                    if(transacao.cartaoId != null) "Cartão" else "Conta",
                    style = MaterialTheme.typography.bodySmall,
                    color = androidx.compose.ui.graphics.Color.Gray
                )
            }
            Text(transacao.valor.toCurrency(), fontWeight = FontWeight.Bold, color = color)
        }
    }
}