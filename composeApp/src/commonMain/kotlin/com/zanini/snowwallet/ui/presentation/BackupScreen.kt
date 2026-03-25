package com.zanini.snowwallet.ui.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner // Import Necessário
import androidx.compose.ui.text.font.FontWeight // Import Corrigido
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver // Import Necessário
import androidx.navigation.NavController
import com.zanini.snowwallet.ui.presentation.components.AppTopBar
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    navController: NavController,
    viewModel: BackupViewModel = koinInject(),
    platformContext: Any? = null
) {
    val status by viewModel.status.collectAsState()
    val email by viewModel.emailConectado.collectAsState()

    // Para observar o ciclo de vida (ON_RESUME)
    val lifecycleOwner = LocalLifecycleOwner.current

    // Passa contexto inicial
    LaunchedEffect(Unit) {
        viewModel.setContext(platformContext)
    }

    // ATUALIZAÇÃO AUTOMÁTICA AO VOLTAR DO LOGIN (Substituto do LifecycleEventEffect)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Reenvia o contexto para forçar a verificação do login atual quando o app volta
                viewModel.setContext(platformContext)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = { AppTopBar("Backup Drive", onBackClick = { navController.popBackStack() }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CloudUpload,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "Sincronização SnowApps",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold // Agora vai funcionar
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Salva seus dados na pasta 'SnowApps' do Google Drive.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- INFO DA CONTA ---
            if (email != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccountCircle, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Conectado como:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(email!!, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // --- BOTÕES DE AÇÃO ---

            Button(
                onClick = { viewModel.fazerBackup() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = email != null
            ) {
                Icon(Icons.Default.CloudUpload, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Fazer Backup Agora")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { viewModel.restaurarBackup() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = email != null
            ) {
                Icon(Icons.Default.CloudDownload, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restaurar do Drive")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- BOTÃO CONECTAR / TROCAR ---
            TextButton(
                onClick = { viewModel.conectarConta() }
            ) {
                Icon(Icons.Default.AccountCircle, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (email == null) "Conectar Google Drive" else "Trocar Conta")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- STATUS ---
            if (status.isNotEmpty()) {
                val isError = status.contains("Erro", ignoreCase = true) || status.contains("Falha", ignoreCase = true)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isError) Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}