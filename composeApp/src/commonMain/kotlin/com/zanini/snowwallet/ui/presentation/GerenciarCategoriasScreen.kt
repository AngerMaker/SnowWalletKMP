package com.zanini.snowwallet.ui.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zanini.snowwallet.model.Categoria
import com.zanini.snowwallet.ui.presentation.components.AppTopBar
import com.zanini.snowwallet.ui.presentation.components.DropdownSelector
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenciarCategoriasScreen(
    navController: NavController,
    viewModel: GerenciarCategoriasViewModel = koinInject()
) {
    val categorias by viewModel.categorias.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var categoriaEmEdicao by remember { mutableStateOf<Categoria?>(null) }

    Scaffold(
        topBar = { AppTopBar(title = "Categorias", onBackClick = { navController.popBackStack() }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                categoriaEmEdicao = null
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Nova Categoria")
            }
        }
    ) { padding ->
        if (categorias.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Nenhuma categoria encontrada.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categorias) { categoria ->
                    CategoriaItem(
                        categoria = categoria,
                        onEdit = {
                            categoriaEmEdicao = it
                            showDialog = true
                        },
                        onDelete = { viewModel.deletarCategoria(it) }
                    )
                }
            }
        }

        if (showDialog) {
            AddEditCategoriaDialog(
                categoria = categoriaEmEdicao,
                onDismiss = { showDialog = false },
                onSalvar = { nome, icone ->
                    viewModel.salvarCategoria(
                        id = categoriaEmEdicao?.id ?: 0,
                        nome = nome,
                        icone = icone
                    )
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun CategoriaItem(
    categoria: Categoria,
    onEdit: (Categoria) -> Unit,
    onDelete: (Categoria) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Corrigido para usar nomeIcone do modelo
                Icon(
                    imageVector = IconesDisponiveis.getIcone(categoria.nomeIcone),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(categoria.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Row {
                IconButton(onClick = { onEdit(categoria) }) { Icon(Icons.Default.Edit, "Editar") }
                IconButton(onClick = { onDelete(categoria) }) { Icon(Icons.Default.Delete, "Excluir") }
            }
        }
    }
}

@Composable
fun AddEditCategoriaDialog(
    categoria: Categoria?,
    onDismiss: () -> Unit,
    onSalvar: (String, String) -> Unit
) {
    var nome by remember { mutableStateOf(categoria?.nome ?: "") }
    // Usa nomeIcone do modelo, padrão "Category"
    var iconeSelecionado by remember { mutableStateOf(categoria?.nomeIcone ?: "Category") }

    // Lista ordenada correta
    val listaIcones = remember { IconesDisponiveis.getNomesOrdenados() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (categoria == null) "Nova Categoria" else "Editar Categoria") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome da Categoria") },
                    modifier = Modifier.fillMaxWidth()
                )

                DropdownSelector(
                    label = "Ícone",
                    items = listaIcones,
                    selectedItem = iconeSelecionado,
                    onItemSelected = { iconeSelecionado = it },
                    itemLabel = { it },
                    itemIcon = { nome -> IconesDisponiveis.getIcone(nome) }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (nome.isNotBlank()) {
                    onSalvar(nome, iconeSelecionado)
                }
            }) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}