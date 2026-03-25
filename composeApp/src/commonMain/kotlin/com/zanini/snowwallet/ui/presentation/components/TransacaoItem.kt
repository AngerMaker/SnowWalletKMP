package com.zanini.snowwallet.ui.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zanini.snowwallet.model.Transacao
import com.zanini.snowwallet.ui.theme.GreenIncome
import com.zanini.snowwallet.ui.theme.RedExpense
import com.zanini.snowwallet.util.toCurrency
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransacaoItem(
    transacao: Transacao,
    nomeOrigem: String? = null,
    onClick: () -> Unit = {},
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onLongClick: () -> Unit = {}
) {
    val isReceita = transacao.tipo == "RECEITA"
    // ATUALIZADO: Identificando se é pendente
    val isPendente = transacao.tipo == "DESPESA" && !transacao.pago

    // ATUALIZADO: Cores separadas para Receita, Pendente (Laranja) e Despesa Paga (Vermelho)
    val color = when {
        isReceita -> GreenIncome
        isPendente -> Color(0xFFFF9800)
        else -> RedExpense
    }

    // ATUALIZADO: Ícones correspondentes
    val icon = when {
        isReceita -> Icons.Default.ArrowDownward
        isPendente -> Icons.Default.Schedule
        else -> Icons.Default.ArrowUpward
    }

    val data = Instant.fromEpochMilliseconds(transacao.data)
        .toLocalDateTime(TimeZone.currentSystemDefault())

    val cardColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = cardColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(transacao.descricao, fontWeight = FontWeight.SemiBold)

                val origemTexto = nomeOrigem ?: when {
                    transacao.cartaoId != null -> "Cartão"
                    transacao.contaId != null -> "Conta"
                    else -> "Outro"
                }

                Text(
                    "${data.dayOfMonth}/${data.monthNumber} • $origemTexto",
                    style = MaterialTheme.typography.bodySmall,
                    color = if(isSelected) contentColor.copy(alpha = 0.8f) else Color.Gray
                )
            }
            Text(
                transacao.valor.toCurrency(),
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}