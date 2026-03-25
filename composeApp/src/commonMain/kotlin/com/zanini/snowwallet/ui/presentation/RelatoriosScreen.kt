package com.zanini.snowwallet.ui.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zanini.snowwallet.model.GastoPorCategoria
import com.zanini.snowwallet.ui.presentation.components.AppTopBar
import com.zanini.snowwallet.util.toCurrency
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatoriosScreen(
    navController: NavController,
    viewModel: RelatoriosViewModel = koinInject()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) } // 0 = Despesas, 1 = Receitas

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Relatórios",
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // --- Seletor de Mês ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.mesAnterior() }) {
                    Icon(Icons.Default.ChevronLeft, "Anterior")
                }
                Text(
                    text = "${state.mesSelecionado}/${state.anoSelecionado}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { viewModel.proximoMes() }) {
                    Icon(Icons.Default.ChevronRight, "Próximo")
                }
            }

            // --- Abas (Despesas / Receitas) ---
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Despesas") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Receitas") }
                )
            }

            val dadosAtuais = if (selectedTabIndex == 0) state.gastosPorCategoria else state.receitasPorCategoria
            val corPadrao = if (selectedTabIndex == 0) Color(0xFFE53935) else Color(0xFF4CAF50)

            if (dadosAtuais.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sem dados neste período.", color = Color.Gray)
                }
            } else {
                // Conteúdo com Scroll
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // --- GRÁFICO DE PIZZA ---
                    PieChart(
                        data = dadosAtuais,
                        modifier = Modifier.size(250.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // --- LISTA DE CATEGORIAS ---
                    Text(
                        "Detalhamento por Categoria",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    dadosAtuais.forEach { item ->
                        CategoryListItem(
                            item = item,
                            onClick = {
                                // Navega para a lista detalhada daquela categoria
                                navController.navigate(
                                    "category_transactions/${item.categoriaId}?month=${state.mesSelecionado}&year=${state.anoSelecionado}"
                                )
                            }
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun PieChart(
    data: List<GastoPorCategoria>,
    modifier: Modifier = Modifier
) {
    val total = data.sumOf { it.valorTotal }

    // Converte Strings Hex (#RRGGBB) para Color
    val colors = remember(data) {
        data.map { parseColor(it.cor) }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 40.dp.toPx()
            val radius = size.minDimension / 2 - strokeWidth / 2
            val center = Offset(size.width / 2, size.height / 2)

            var startAngle = -90f // Começa do topo

            data.forEachIndexed { index, item ->
                val sweepAngle = (item.valorTotal / total * 360).toFloat()

                // Desenha o arco
                drawArc(
                    color = colors[index],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false, // false cria efeito Donut (com style Stroke)
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth)
                )

                startAngle += sweepAngle
            }
        }

        // Texto no Centro
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Total", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(
                text = total.toCurrency(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CategoryListItem(
    item: GastoPorCategoria,
    onClick: () -> Unit
) {
    val cor = parseColor(item.cor)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Bolinha da cor da categoria
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(cor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = item.nomeCategoria,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.valorTotal.toCurrency(),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// Função auxiliar para converter Hex String (#RRGGBB) em Color
fun parseColor(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        val colorInt = cleanHex.toLong(16)
        // Adiciona Alpha FF se for apenas RRGGBB
        if (cleanHex.length == 6) {
            Color(colorInt or 0xFF000000)
        } else {
            Color(colorInt)
        }
    } catch (e: Exception) {
        Color.Gray // Fallback
    }
}