package com.zanini.snowwallet.ui.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zanini.snowwallet.ui.presentation.components.AppTopBar
import com.zanini.snowwallet.ui.presentation.components.DropdownSelector
import com.zanini.snowwallet.ui.theme.GreenIncome
import com.zanini.snowwallet.ui.theme.RedExpense
import com.zanini.snowwallet.util.formatDate
import com.zanini.snowwallet.ui.presentation.IconesDisponiveis
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.Clock
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    navController: NavController,
    transactionId: Long = 0L,
    initialTipo: String = "DESPESA",
    cartaoId: Long? = null
) {
    val viewModel: AddEditTransactionViewModel = koinInject()
    val state by viewModel.uiState.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.data
    )

    var showNotifDatePicker by remember { mutableStateOf(false) }
    val notifDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.dataNotificacao.takeIf { it > 0 } ?: Clock.System.now().toEpochMilliseconds()
    )

    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour = state.horaNotificacao,
        initialMinute = state.minutoNotificacao
    )

    LaunchedEffect(transactionId, cartaoId) {
        if (transactionId != 0L) {
            viewModel.carregarTransacao(transactionId)
        } else {
            viewModel.resetState(initialTipo, cartaoId)
        }
    }

    LaunchedEffect(state.salvoComSucesso) {
        if (state.salvoComSucesso) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(state.data) {
        datePickerState.selectedDateMillis = state.data
    }

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

    // --- ALERTA DE EXCLUSÃO ---
    if (state.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            title = { Text("Excluir Transação") },
            text = { Text("Esta transação faz parte de uma série (parcelada ou recorrente). O que deseja fazer?") },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { viewModel.deletarEstaEFuturas() }) {
                        Text("Apagar esta e as futuras", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { viewModel.deletarApenasEsta() }) {
                        Text("Apagar apenas esta")
                    }
                    TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        val dataUtc = Instant.fromEpochMilliseconds(selectedMillis).toLocalDateTime(TimeZone.UTC)
                        val dataLocal = LocalDateTime(
                            year = dataUtc.year,
                            monthNumber = dataUtc.monthNumber,
                            dayOfMonth = dataUtc.dayOfMonth,
                            hour = 12, minute = 0, second = 0, nanosecond = 0
                        )
                        val adjustedMillis = dataLocal.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                        viewModel.onDateChange(adjustedMillis)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showNotifDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showNotifDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedMillis = notifDatePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        val dataUtc = Instant.fromEpochMilliseconds(selectedMillis).toLocalDateTime(TimeZone.UTC)
                        val dataLocal = LocalDateTime(
                            year = dataUtc.year,
                            monthNumber = dataUtc.monthNumber,
                            dayOfMonth = dataUtc.dayOfMonth,
                            hour = 12, minute = 0, second = 0, nanosecond = 0
                        )
                        val adjustedMillis = dataLocal.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                        viewModel.onDataNotificacaoChange(adjustedMillis)
                    }
                    showNotifDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showNotifDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = notifDatePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Selecionar Horário") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onHoraNotificacaoChange(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (transactionId == 0L) "Nova Transação" else "Editar Transação",
                onBackClick = { navController.popBackStack() },
                actions = {
                    if (transactionId != 0L) {
                        IconButton(onClick = { viewModel.confirmarDelecao() }) {
                            Icon(Icons.Default.Delete, "Excluir", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.salvarTransacao() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Check, "Salvar")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = state.tipo == "DESPESA",
                    onClick = { viewModel.onTipoChange("DESPESA") },
                    label = { Text("Despesa") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RedExpense),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = state.tipo == "RECEITA",
                    onClick = { viewModel.onTipoChange("RECEITA") },
                    label = { Text("Receita") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenIncome),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = state.valor,
                onValueChange = { viewModel.onValorChange(it) },
                label = { Text("Valor (R$)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.descricao,
                onValueChange = { viewModel.onDescricaoChange(it) },
                label = { Text("Descrição") },
                modifier = Modifier.fillMaxWidth()
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = formatDate(state.data),
                    onValueChange = {},
                    label = { Text("Data") },
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Default.CalendarToday, null) },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker = true }
                )
            }

            if (state.listaCategorias.isNotEmpty()) {
                DropdownSelector(
                    label = "Categoria",
                    items = state.listaCategorias,
                    selectedItem = state.categoriaSelecionada,
                    onItemSelected = { viewModel.onCategoriaChange(it) },
                    itemLabel = { it.nome },
                    itemIcon = { IconesDisponiveis.getIcone(it.nomeIcone) }
                )
            }

            if (state.listaOpcoesPagamento.isNotEmpty()) {
                DropdownSelector(
                    label = if(state.tipo == "RECEITA") "Receber em" else "Pagar com",
                    items = state.listaOpcoesPagamento,
                    selectedItem = state.opcaoPagamentoSelecionada,
                    onItemSelected = { viewModel.onOpcaoPagamentoChange(it) },
                    itemLabel = { it.nome },
                    itemIcon = { IconesDisponiveis.getIcone(it.icone) }
                )
            }

            val isCartao = state.opcaoPagamentoSelecionada is OpcaoPagamento.OpcaoCartao

            if (state.tipo == "DESPESA") {
                OutlinedTextField(
                    value = state.numeroParcelas,
                    onValueChange = { viewModel.onParcelasChange(it) },
                    label = { Text("Parcelas (1 = À vista)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                if ((state.numeroParcelas.toIntOrNull() ?: 1) > 1) {
                    val numParcelas = state.numeroParcelas.toIntOrNull() ?: 1
                    val valorTotal = state.valor.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val valorParcela = valorTotal / numParcelas

                    val cents = (valorParcela * 100).roundToLong()
                    val reais = cents / 100
                    val centavos = (cents % 100).toString().padStart(2, '0')

                    Text(
                        text = "Valor da parcela: R$ $reais,$centavos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (!isCartao) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.pago,
                        onCheckedChange = { viewModel.onPagoChange(it) }
                    )
                    Text(if (state.tipo == "RECEITA") "Recebido" else "Pago")
                }
                if ((state.numeroParcelas.toIntOrNull() ?: 1) > 1 && state.pago) {
                    Text(
                        "Apenas a 1ª parcela será marcada como paga.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            } else if (state.tipo == "DESPESA") {
                Text(
                    "Será adicionado à fatura do cartão.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Seção de Lembrete Inteligente ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("🔔 Lembrete Inteligente", style = MaterialTheme.typography.titleMedium)
                        Switch(
                            checked = state.agendarNotificacao,
                            onCheckedChange = { viewModel.onAgendarNotificacaoChange(it) }
                        )
                    }
                    if (state.agendarNotificacao) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            OutlinedButton(onClick = { showNotifDatePicker = true }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(formatDate(state.dataNotificacao))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) {
                                Text("${state.horaNotificacao.toString().padStart(2, '0')}:${state.minutoNotificacao.toString().padStart(2, '0')}")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}