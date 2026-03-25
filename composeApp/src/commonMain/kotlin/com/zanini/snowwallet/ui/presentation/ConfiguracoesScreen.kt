// src/commonMain/kotlin/com/zanini/snowwallet/ui/presentation/ConfiguracoesScreen.kt
package com.zanini.snowwallet.ui.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.zanini.snowwallet.ui.presentation.components.AppTopBar
import com.zanini.snowwallet.ui.theme.AppThemePalette
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracoesScreen(
    navController: NavController,
    viewModel: ConfiguracoesViewModel = koinInject()
) {
    var showDialog by remember { mutableStateOf(false) }
    val isDarkMode by viewModel.isDarkMode.collectAsState(initial = isSystemInDarkTheme())

    val currentThemeColor by viewModel.appThemeColor.collectAsState(initial = "PURPLE")

    Scaffold(
        topBar = { AppTopBar(title = "Configurações", onBackClick = { navController.popBackStack() }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                Text("Aparência", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)

                ListItem(
                    leadingContent = { Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary) },
                    headlineContent = { Text("Tema Escuro") },
                    trailingContent = { Switch(checked = isDarkMode, onCheckedChange = { viewModel.toggleTheme(it) }) }
                )

                // SELETORES DE COR (Componente Reutilizável)
                ColorSelector(
                    title = "Cor Principal do Aplicativo",
                    currentColorName = currentThemeColor,
                    isDarkMode = isDarkMode,
                    onColorSelected = { viewModel.updateThemeColor(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }

            // ... RESTANTE DO CÓDIGO IGUAL (Geral, Backup, Perigo) ...
            item {
                Text("Geral", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)
                ConfigItem(Icons.Default.Category, "Gerenciar Categorias", "Adicione ou edite suas categorias", { navController.navigate("gerenciar_categorias") })
                HorizontalDivider()
            }

            item {
                Text("Backup e Sincronização", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)
                ConfigItem(Icons.Default.CloudUpload, "Google Drive Backup", "Sincronizar seus dados na nuvem", { navController.navigate("backup") })
                HorizontalDivider()
            }

            item {
                Text("Perigo", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                ConfigItem(Icons.Default.Delete, "Limpar Dados", "Apagar tudo permanentemente", { showDialog = true }, true)
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Tem certeza?") },
            text = { Text("Isso apagará TODAS as suas transações. Essa ação não pode ser desfeita.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.limparTudo(); showDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Apagar Tudo") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancelar") } }
        )
    }
}

// COMPONENTE REUTILIZÁVEL PARA SELEÇÃO DE COR
@Composable
fun ColorSelector(title: String, currentColorName: String, isDarkMode: Boolean, onColorSelected: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(AppThemePalette.entries) { palette ->
                val isSelected = palette.name == currentColorName
                val displayColor = if(isDarkMode) palette.primaryDark else palette.primaryLight

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(displayColor)
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(palette.name) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = "Selecionado", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit, isDestructive: Boolean = false) {
    ListItem(
        modifier = Modifier.clickable { onClick() },
        leadingContent = { Icon(icon, null, tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Icon(Icons.Default.ChevronRight, null) }
    )
}