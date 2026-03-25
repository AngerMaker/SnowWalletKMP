// src/commonMain/kotlin/com/zanini/snowwallet/ui/presentation/HomeScreen.kt
package com.zanini.snowwallet.ui.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zanini.snowwallet.ui.theme.AppThemePalette
import com.zanini.snowwallet.ui.theme.GreenIncome
import com.zanini.snowwallet.ui.theme.RedExpense
import com.zanini.snowwallet.util.toCurrency
import com.zanini.snowwallet.ui.presentation.IconesDisponiveis
import kotlinx.datetime.*
import org.koin.compose.koinInject
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = koinInject()
) {
    val state by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPayDialog by remember { mutableStateOf(false) }


    Scaffold(
        topBar = {
            if (state.isSelectionMode) {
                TopAppBar(
                    // ATUALIZADO: Mostrando a soma em tempo real ao selecionar
                    title = {
                        Column {
                            Text("${state.selectedIds.size} selecionados", style = MaterialTheme.typography.titleMedium)
                            if (state.selectedIds.isNotEmpty()) {
                                val corSoma = if (state.valorSelecionado >= 0) GreenIncome else RedExpense
                                val prefixo = if (state.valorSelecionado < 0) "-" else ""
                                Text(
                                    text = "$prefixo${abs(state.valorSelecionado).toCurrency()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = corSoma,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, "Cancelar")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Selecionar Tudo")
                        }
                        IconButton(onClick = { showPayDialog = true }) {
                            Icon(Icons.Default.Check, contentDescription = "Pagar Selecionados", tint = GreenIncome)
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        },
        floatingActionButton = {
            if (!state.isSelectionMode) {
                FloatingActionButton(
                    onClick = { navController.navigate("add_edit_transaction?tipo=DESPESA") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) { Icon(Icons.Default.Add, contentDescription = "Nova Transação") }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                HeaderSection(
                    saldoExibido = state.saldoTotal,
                    saldoDisponivel = state.saldoDisponivel,
                    mostrarSaldo = state.mostrarSaldo,
                    onToggleSaldo = { viewModel.toggleMostrarSaldo() },
                    notificacoesCount = state.notificacoesNaoLidas,
                    onNotificacaoClick = { navController.navigate("notificacoes") },
                    onSearchClick = { navController.navigate("search") },
                    onSettingsClick = { navController.navigate("configuracoes") }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Text("Atalhos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(end = 16.dp)
                ) {
                    item { QuickActionItem(Icons.Default.SwapHoriz, "Transferir", MaterialTheme.colorScheme.primary) { navController.navigate("transferencia") } }
                    item { QuickActionItem(Icons.Default.CreditCard, "Cartões", MaterialTheme.colorScheme.primary) { navController.navigate("gerenciar_cartoes") } }
                    item { QuickActionItem(Icons.Default.AccountBalanceWallet, "Contas", MaterialTheme.colorScheme.primary) { navController.navigate("gerenciar_contas") } }
                    item { QuickActionItem(Icons.Default.Repeat, "Fixas", MaterialTheme.colorScheme.primary) { navController.navigate("recorrencia") } }
                    item { QuickActionItem(Icons.Default.AttachMoney, "Emprést.", MaterialTheme.colorScheme.primary) { navController.navigate("emprestimos") } }
                    item { QuickActionItem(Icons.Default.BarChart, "Relat.", MaterialTheme.colorScheme.primary) { navController.navigate("relatorios") } }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (state.cartoesUi.isNotEmpty()) {
                item {
                    Column {
                        Text("Meus Cartões", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                            items(state.cartoesUi) { cartaoUi ->
                                CartaoHomeItem(
                                    item = cartaoUi,
                                    color = MaterialTheme.colorScheme.primary,
                                    onClick = { navController.navigate("fatura/${cartaoUi.cartao.id}") }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.mesAnterior() }) { Icon(Icons.Default.ChevronLeft, "Ant") }
                    Text(text = "${state.mesSelecionado}/${state.anoSelecionado}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { viewModel.proximoMes() }) { Icon(Icons.Default.ChevronRight, "Prox") }
                }
                Spacer(modifier = Modifier.height(16.dp))
                MonthSummaryCard(
                    receitas = state.receitasMes,
                    despesas = state.despesasMes,
                    pendentes = state.pendentesMes,
                    filtroAtual = state.filtroAtual,
                    onFiltroClick = { viewModel.setFiltro(it) }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (state.transacoesDoMesSelecionado.isEmpty()) {
                item { Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { Text("Nenhuma transação encontrada.", color = Color.Gray) } }
            } else {
                val agrupadas = state.transacoesDoMesSelecionado.groupBy { uiModel ->
                    Instant.fromEpochMilliseconds(uiModel.transacao.data).toLocalDateTime(TimeZone.currentSystemDefault()).date
                }

                agrupadas.forEach { (date, transactions) ->
                    item { DateHeader(date) }
                    items(transactions) { itemUi ->
                        val isSelected = state.selectedIds.contains(itemUi.transacao.id)
                        TransactionItemSelectable(
                            item = itemUi,
                            isSelectionMode = state.isSelectionMode,
                            isSelected = isSelected,
                            onLongClick = { viewModel.toggleSelection(itemUi.transacao.id) },
                            onClick = { if (state.isSelectionMode) viewModel.toggleSelection(itemUi.transacao.id) else navController.navigate("add_edit_transaction?id=${itemUi.transacao.id}") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        val hasRecurring = viewModel.hasRecurringInSelection()
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir Transações") },
            text = {
                Column {
                    Text("Deseja apagar ${state.selectedIds.size} transação(ões)?")
                    if (hasRecurring) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Atenção: Você selecionou contas fixas ou parceladas.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Deseja apagar todas as pendentes futuras ou apenas estas selecionadas?", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                if (hasRecurring) {
                    Column(horizontalAlignment = Alignment.End) {
                        Button(
                            onClick = { viewModel.deleteSelected(deleteFutureRecorrentes = true); showDeleteDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Apagar Todas as Futuras") }
                        OutlinedButton(
                            onClick = { viewModel.deleteSelected(deleteFutureRecorrentes = false); showDeleteDialog = false }
                        ) { Text("Apenas Selecionadas") }
                    }
                } else {
                    Button(
                        onClick = { viewModel.deleteSelected(deleteFutureRecorrentes = false); showDeleteDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Confirmar Exclusão") }
                }
            },
            dismissButton = {
                if (!hasRecurring) { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
            }
        )
    }

    if (showPayDialog) {
        AlertDialog(
            onDismissRequest = { showPayDialog = false },
            title = { Text("Confirmar Pagamento") },
            text = { Text("Marcar ${state.selectedIds.size} transação(ões) como paga(s)?") },
            confirmButton = { Button(onClick = { viewModel.markSelectedAsPaid(); showPayDialog = false }) { Text("Confirmar") } },
            dismissButton = { TextButton(onClick = { showPayDialog = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
fun QuickActionItem(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun HeaderSection(
    saldoExibido: Double,
    saldoDisponivel: Double,
    mostrarSaldo: Boolean,
    onToggleSaldo: () -> Unit,
    notificacoesCount: Int,
    onNotificacaoClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Column {
            Text("Saldo Acumulado", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = if (mostrarSaldo) saldoExibido.toCurrency() else "R$ •••••", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onToggleSaldo) { Icon(imageVector = if (mostrarSaldo) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = "Alternar Saldo", modifier = Modifier.size(20.dp), tint = Color.Gray) }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Disponível (Previsto)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(text = if (mostrarSaldo) saldoDisponivel.toCurrency() else "R$ •••••", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
        Row {
            IconButton(onClick = onSearchClick) { Icon(Icons.Default.Search, "Pesquisar") }
            IconButton(onClick = onSettingsClick) { Icon(Icons.Default.Settings, "Configurações") }
            Box {
                IconButton(onClick = onNotificacaoClick) { Icon(Icons.Outlined.Notifications, "Notificações") }
                if (notificacoesCount > 0) Badge(modifier = Modifier.align(Alignment.TopEnd).offset(x = (-8).dp, y = 8.dp)) { Text("$notificacoesCount") }
            }
        }
    }
}

@Composable
fun MonthSummaryCard(receitas: Double, despesas: Double, pendentes: Double, filtroAtual: FiltroTransacao, onFiltroClick: (FiltroTransacao) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Top) {
            SummaryItem(label = "Receitas", valor = receitas, color = GreenIncome, icon = Icons.Default.ArrowDownward, isSelected = filtroAtual == FiltroTransacao.RECEITA, onClick = { onFiltroClick(FiltroTransacao.RECEITA) })
            HorizontalDivider(modifier = Modifier.height(40.dp).width(1.dp), color = Color.Gray.copy(alpha = 0.3f))
            SummaryItem(label = "Despesas", valor = despesas, color = RedExpense, icon = Icons.Default.ArrowUpward, isSelected = filtroAtual == FiltroTransacao.DESPESA, onClick = { onFiltroClick(FiltroTransacao.DESPESA) })
            HorizontalDivider(modifier = Modifier.height(40.dp).width(1.dp), color = Color.Gray.copy(alpha = 0.3f))
            SummaryItem(label = "Pendentes", valor = pendentes, color = Color(0xFFFF9800), icon = Icons.Default.Schedule, isSelected = filtroAtual == FiltroTransacao.PENDENTE, onClick = { onFiltroClick(FiltroTransacao.PENDENTE) })
        }
    }
}

@Composable
fun SummaryItem(label: String, valor: Double, color: Color, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onClick() }.background(if (isSelected) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else Color.Transparent).padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = if (isSelected) MaterialTheme.colorScheme.onSurface else color, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        }
        Text(text = valor.toCurrency(), fontWeight = FontWeight.Bold, style = if(isSelected) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionItemSelectable(item: TransacaoUiModel, isSelectionMode: Boolean, isSelected: Boolean, onLongClick: () -> Unit, onClick: () -> Unit) {
    com.zanini.snowwallet.ui.presentation.components.TransacaoItem(
        transacao = item.transacao,
        nomeOrigem = item.nomeOrigem,
        onClick = onClick,
        isSelectionMode = isSelectionMode,
        isSelected = isSelected,
        onLongClick = onLongClick
    )
}

@Composable
fun DateHeader(date: LocalDate) {
    val hoje = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val ontem = hoje.minus(1, DateTimeUnit.DAY)
    val textoData = when (date) { hoje -> "Hoje"; ontem -> "Ontem"; else -> "${date.dayOfMonth}/${date.monthNumber}" }
    Text(text = textoData, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
fun CartaoHomeItem(item: CartaoUiModel, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(170.dp).height(110.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CreditCard, null, modifier = Modifier.size(20.dp), tint = color)
                Spacer(modifier = Modifier.width(8.dp))
                Text(item.cartao.nome, style = MaterialTheme.typography.titleSmall, maxLines = 1, fontWeight = FontWeight.Bold, color = color)
            }
            Column {
                val progresso = if (item.limiteTotal > 0) ((item.limiteTotal - item.limiteDisponivel) / item.limiteTotal).toFloat() else 0f
                LinearProgressIndicator(progress = { progresso }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)), color = color)
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.limiteDisponivel.toCurrency(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
            }
        }
    }
}