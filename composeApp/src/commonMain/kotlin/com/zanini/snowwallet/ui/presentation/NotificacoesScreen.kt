package com.zanini.snowwallet.ui.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zanini.snowwallet.model.Notificacao
import com.zanini.snowwallet.ui.presentation.components.AppTopBar
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject // <--- USAR ESTE IMPORT

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacoesScreen(
    navController: NavController,
    // Alterado de koinViewModel() para koinInject() para compatibilidade
    viewModel: NotificacoesViewModel = koinInject()
) {
    val notificacoes by viewModel.notificacoes.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Notificações",
                onBackClick = { navController.popBackStack() },
                actions = {
                    if (notificacoes.isNotEmpty()) {
                        IconButton(onClick = { viewModel.limparTodas() }) {
                            Icon(Icons.Default.DeleteSweep, "Limpar Tudo")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (notificacoes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Notifications, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Nenhuma notificação", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notificacoes) { notificacao ->
                    NotificacaoItem(
                        notificacao = notificacao,
                        onClick = { viewModel.marcarComoLida(notificacao) },
                        onDelete = { viewModel.deletar(notificacao) }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificacaoItem(
    notificacao: Notificacao,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val backgroundColor = if (notificacao.lida) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Notifications,
                null,
                tint = if (notificacao.lida) Color.Gray else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    notificacao.titulo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (notificacao.lida) FontWeight.Normal else FontWeight.Bold
                )
                Text(notificacao.mensagem, style = MaterialTheme.typography.bodyMedium)

                val data = Instant.fromEpochMilliseconds(notificacao.data)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                Text(
                    "${data.dayOfMonth}/${data.monthNumber} - ${data.hour}:${data.minute}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Apagar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}