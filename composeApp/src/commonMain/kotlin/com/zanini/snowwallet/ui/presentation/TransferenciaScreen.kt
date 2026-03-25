package com.zanini.snowwallet.ui.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zanini.snowwallet.ui.presentation.components.AppTopBar
import com.zanini.snowwallet.ui.presentation.components.DropdownSelector
import com.zanini.snowwallet.ui.presentation.IconesDisponiveis
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferenciaScreen(
    navController: NavController,
    viewModel: TransferenciaViewModel = koinInject()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.sucesso) {
        if (state.sucesso) navController.popBackStack()
    }

    Scaffold(
        topBar = { AppTopBar("Transferência", onBackClick = { navController.popBackStack() }) }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.valor,
                onValueChange = { viewModel.onValorChange(it) },
                label = { Text("Valor da Transferência") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Origem
            DropdownSelector(
                label = "De (Sairá dinheiro)",
                items = state.contas,
                selectedItem = state.contaOrigem,
                onItemSelected = { viewModel.onOrigemChange(it) },
                itemLabel = { it.nome },
                itemIcon = { IconesDisponiveis.getIcone(it.nomeIcone) }
            )

            Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally))

            // Destino
            DropdownSelector(
                label = "Para (Entrará dinheiro)",
                items = state.contas,
                selectedItem = state.contaDestino,
                onItemSelected = { viewModel.onDestinoChange(it) },
                itemLabel = { it.nome },
                itemIcon = { IconesDisponiveis.getIcone(it.nomeIcone) }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.transferir() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = state.valor.toDoubleOrNull() ?: 0.0 > 0 && state.contaOrigem != null && state.contaDestino != null
            ) {
                Text("Confirmar Transferência")
            }
        }
    }
}