package com.zanini.snowwallet.ui.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
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
import com.zanini.snowwallet.model.CartaoCredito
import com.zanini.snowwallet.model.Conta
import com.zanini.snowwallet.ui.presentation.components.AppTopBar
import com.zanini.snowwallet.ui.presentation.components.DropdownSelector
import com.zanini.snowwallet.util.toCurrency
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenciarCartoesScreen(
    navController: NavController,
    viewModel: GerenciarCartoesViewModel = koinInject()
) {
    val cartoes by viewModel.cartoes.collectAsState()
    val contas by viewModel.contas.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var cartaoEmEdicao by remember { mutableStateOf<CartaoCredito?>(null) }

    Scaffold(
        topBar = { AppTopBar(title = "Meus Cartões", onBackClick = { navController.popBackStack() }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                cartaoEmEdicao = null
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Novo Cartão")
            }
        }
    ) { padding ->
        if (cartoes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Nenhum cartão cadastrado.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cartoes) { cartao ->
                    val nomeConta = contas.find { it.id == cartao.contaId }?.nome
                    CartaoItem(
                        cartao = cartao,
                        nomeConta = nomeConta,
                        onEdit = {
                            cartaoEmEdicao = it
                            showDialog = true
                        },
                        onDelete = { viewModel.deletarCartao(it) },
                        onClick = { navController.navigate("fatura/${cartao.id}") }
                    )
                }
            }
        }

        if (showDialog) {
            AddEditCartaoDialog(
                cartao = cartaoEmEdicao,
                contas = contas,
                onDismiss = { showDialog = false },
                onSalvar = { nome, limite, diaFech, diaVenc, contaId ->
                    viewModel.salvarCartao(
                        id = cartaoEmEdicao?.id ?: 0,
                        nome = nome,
                        limite = limite,
                        diaFechamento = diaFech,
                        diaVencimento = diaVenc,
                        contaId = contaId
                    )
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun CartaoItem(
    cartao: CartaoCredito,
    nomeConta: String?,
    onEdit: (CartaoCredito) -> Unit,
    onDelete: (CartaoCredito) -> Unit,
    onClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Ícone fixo "CreditCard" já que o banco não tem campo de ícone
                Icon(
                    imageVector = IconesDisponiveis.getIcone("CreditCard"),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(cartao.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Limite: ${cartao.limite.toCurrency()}", style = MaterialTheme.typography.bodySmall)
                    if (nomeConta != null) {
                        Text("Debita de: $nomeConta", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Text("Vence dia ${cartao.diaVencimento}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
            Row {
                IconButton(onClick = { onEdit(cartao) }) { Icon(Icons.Default.Edit, "Editar") }
                IconButton(onClick = { onDelete(cartao) }) { Icon(Icons.Default.Delete, "Excluir") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCartaoDialog(
    cartao: CartaoCredito?,
    contas: List<Conta>,
    onDismiss: () -> Unit,
    onSalvar: (String, Double, Int, Int, Long?) -> Unit
) {
    var nome by remember { mutableStateOf(cartao?.nome ?: "") }
    var limite by remember { mutableStateOf(cartao?.limite?.toString() ?: "") }
    var diaFechamento by remember { mutableStateOf(cartao?.diaFechamento ?: 1) }
    var diaVencimento by remember { mutableStateOf(cartao?.diaVencimento ?: 10) }
    var contaSelecionada by remember { mutableStateOf(contas.find { it.id == cartao?.contaId }) }

    var showFechamentoPicker by remember { mutableStateOf(false) }
    var showVencimentoPicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    fun extrairDia(millis: Long?): Int {
        if (millis == null) return 1
        val data = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
        return data.dayOfMonth
    }

    if (showFechamentoPicker) {
        DatePickerDialog(
            onDismissRequest = { showFechamentoPicker = false },
            confirmButton = { TextButton(onClick = { diaFechamento = extrairDia(datePickerState.selectedDateMillis); showFechamentoPicker = false }) { Text("OK") } }
        ) { DatePicker(state = datePickerState, title = { Text("Dia de Fechamento") }) }
    }

    if (showVencimentoPicker) {
        DatePickerDialog(
            onDismissRequest = { showVencimentoPicker = false },
            confirmButton = { TextButton(onClick = { diaVencimento = extrairDia(datePickerState.selectedDateMillis); showVencimentoPicker = false }) { Text("OK") } }
        ) { DatePicker(state = datePickerState, title = { Text("Dia de Vencimento") }) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (cartao == null) "Novo Cartão" else "Editar Cartão") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome do Cartão") })
                OutlinedTextField(value = limite, onValueChange = { limite = it }, label = { Text("Limite (R$)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

                DropdownSelector(
                    label = "Vincular a Conta",
                    items = contas,
                    selectedItem = contaSelecionada,
                    itemLabel = { it.nome },
                    itemIcon = { IconesDisponiveis.getIcone(it.nomeIcone) },
                    onItemSelected = { contaSelecionada = it }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(value = diaFechamento.toString(), onValueChange = {}, label = { Text("Dia Fech.") }, readOnly = true, trailingIcon = { Icon(Icons.Default.CalendarToday, null) }, modifier = Modifier.fillMaxWidth())
                        Box(modifier = Modifier.matchParentSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { showFechamentoPicker = true }))
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(value = diaVencimento.toString(), onValueChange = {}, label = { Text("Dia Venc.") }, readOnly = true, trailingIcon = { Icon(Icons.Default.CalendarToday, null) }, modifier = Modifier.fillMaxWidth())
                        Box(modifier = Modifier.matchParentSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { showVencimentoPicker = true }))
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { val limiteDouble = limite.replace(",", ".").toDoubleOrNull() ?: 0.0; if (nome.isNotBlank() && limiteDouble > 0) { onSalvar(nome, limiteDouble, diaFechamento, diaVencimento, contaSelecionada?.id) } }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}