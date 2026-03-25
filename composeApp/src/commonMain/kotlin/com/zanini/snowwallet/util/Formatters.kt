package com.zanini.snowwallet.util

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.roundToLong

// Formata data de milissegundos para DD/MM/AAAA
fun formatDate(epochMillis: Long): String {
    val date = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    val day = date.dayOfMonth.toString().padStart(2, '0')
    val month = date.monthNumber.toString().padStart(2, '0')
    val year = date.year
    return "$day/$month/$year"
}

// Formata Double para Moeda (R$) de forma segura para KMP (sem depender de Java)
fun Double.toCurrency(): String {
    val valor = this
    val isNegative = valor < 0
    val valorAbsoluto = abs(valor)

    // Arredonda para 2 casas decimais multiplicando por 100 e convertendo para Long
    val valorLong = (valorAbsoluto * 100).roundToLong()

    val parteInteira = valorLong / 100
    val parteDecimal = valorLong % 100

    // Garante que a parte decimal tenha 2 dígitos (ex: 5 vira 05)
    val decimalString = parteDecimal.toString().padStart(2, '0')

    // Formata a parte inteira com pontos de milhar (ex: 1000 vira 1.000)
    val inteiroString = parteInteira.toString()
    val inteiroComPontos = StringBuilder()
    var contador = 0

    for (i in inteiroString.length - 1 downTo 0) {
        inteiroComPontos.append(inteiroString[i])
        contador++
        if (contador % 3 == 0 && i > 0) {
            inteiroComPontos.append('.')
        }
    }

    val resultadoInteiro = inteiroComPontos.reverse().toString()
    val sinal = if (isNegative) "-" else ""

    return "R$ $sinal$resultadoInteiro,$decimalString"
}