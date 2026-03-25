package com.zanini.snowwallet.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.zanini.snowwallet.MainActivity
import com.zanini.snowwallet.R
import com.zanini.snowwallet.data.repository.ContaRepository
import com.zanini.snowwallet.data.repository.TransacaoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.NumberFormat
import java.util.Locale

class SnowWalletWidget : AppWidgetProvider(), KoinComponent {

    private val transacaoRepo: TransacaoRepository by inject()
    private val contaRepo: ContaRepository by inject()

    private val widgetScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val ACTION_TOGGLE_PRIVACY = "com.zanini.snowwallet.ACTION_TOGGLE_PRIVACY"
        const val ACTION_ADD_INCOME = "com.zanini.snowwallet.ACTION_ADD_INCOME"
        const val ACTION_ADD_EXPENSE = "com.zanini.snowwallet.ACTION_ADD_EXPENSE"
        const val ACTION_OPEN_HOME = "com.zanini.snowwallet.ACTION_OPEN_HOME"
        const val ACTION_UPDATE_WIDGET = "android.appwidget.action.APPWIDGET_UPDATE"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync() // Inicia o trabalho assíncrono

        widgetScope.launch {
            try {
                calcularESalvarSaldo(context)
                for (appWidgetId in appWidgetIds) {
                    renderWidgetUI(context, appWidgetManager, appWidgetId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // Proteção contra NullPointerException se o Android fechar o contexto antes
                try {
                    pendingResult?.finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val action = intent.action
        if (action == ACTION_TOGGLE_PRIVACY || action == ACTION_UPDATE_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, SnowWalletWidget::class.java))

            val pendingResult = goAsync()
            widgetScope.launch {
                try {
                    if (action == ACTION_TOGGLE_PRIVACY) {
                        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                        val atual = prefs.getBoolean("saldo_visivel", true)
                        prefs.edit().putBoolean("saldo_visivel", !atual).apply()
                    } else {
                        calcularESalvarSaldo(context)
                    }

                    for (id in ids) {
                        renderWidgetUI(context, appWidgetManager, id)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    try {
                        pendingResult?.finish()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    // --- CÁLCULO EXATAMENTE IGUAL AO HOMEVIEWMODEL (COM CARTÕES) ---
    private suspend fun calcularESalvarSaldo(context: Context) {
        val contas = contaRepo.getTodasContas().first()
        val transacoes = transacaoRepo.getAllTransacoes().first()

        val timeZone = TimeZone.currentSystemDefault()
        val agoraLocal = Clock.System.now().toLocalDateTime(timeZone)

        val primeiroDiaMesSeguinteAtual = if (agoraLocal.monthNumber == 12) LocalDate(agoraLocal.year + 1, 1, 1) else LocalDate(agoraLocal.year, agoraLocal.monthNumber + 1, 1)
        val fimDoMesAtualMillis = primeiroDiaMesSeguinteAtual.minus(DatePeriod(days = 1)).atTime(23, 59, 59).toInstant(timeZone).toEpochMilliseconds()

        val saldoInicial = contas.sumOf { it.saldoInicial }

        val transacoesAteFimDoMes = transacoes.filter { it.data <= fimDoMesAtualMillis }

        val receitas = transacoesAteFimDoMes.filter { it.tipo == "RECEITA" }.sumOf { it.valor }
        val despesasPagas = transacoesAteFimDoMes.filter { it.tipo == "DESPESA" && it.pago && it.cartaoId == null }.sumOf { it.valor }

        val saldoContaFisico = saldoInicial + receitas - despesasPagas

        val contasPendentes = transacoesAteFimDoMes.filter { it.tipo == "DESPESA" && !it.pago && it.cartaoId == null }.sumOf { it.valor }
        val faturaCartoes = transacoesAteFimDoMes.filter { it.tipo == "DESPESA" && !it.pago && it.cartaoId != null }.sumOf { it.valor }
        val despesasPendentesTotais = contasPendentes + faturaCartoes

        val saldoDisponivel = saldoContaFisico - despesasPendentesTotais

        context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            .edit()
            .putFloat("saldo_disponivel_cache", saldoDisponivel.toFloat())
            .apply()
    }

    private fun renderWidgetUI(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val saldo = prefs.getFloat("saldo_disponivel_cache", 0f)
        val visivel = prefs.getBoolean("saldo_visivel", true)

        val views = RemoteViews(context.packageName, R.layout.snow_wallet_widget)

        if (visivel) {
            val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            val texto = try {
                formatter.format(saldo)
            } catch (e: Exception) { "R$ $saldo" }
            views.setTextViewText(R.id.widget_balance, texto)
            views.setImageViewResource(R.id.widget_privacy_toggle, R.drawable.ic_eye_open)
        } else {
            views.setTextViewText(R.id.widget_balance, "R$ •••••")
            views.setImageViewResource(R.id.widget_privacy_toggle, R.drawable.ic_eye_closed)
        }

        views.setInt(R.id.widget_privacy_toggle, "setColorFilter", Color.parseColor("#757575"))

        val privacyIntent = Intent(context, SnowWalletWidget::class.java).apply { action = ACTION_TOGGLE_PRIVACY }
        val privacyPending = PendingIntent.getBroadcast(context, 0, privacyIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_privacy_toggle, privacyPending)

        val incomeIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_ADD_INCOME
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "add_receita")
        }
        val incomePending = PendingIntent.getActivity(context, 1, incomeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.btn_add_income, incomePending)

        val expenseIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_ADD_EXPENSE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "add_despesa")
        }
        val expensePending = PendingIntent.getActivity(context, 2, expenseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.btn_add_expense, expensePending)

        val updateIntent = Intent(context, SnowWalletWidget::class.java).apply { action = ACTION_UPDATE_WIDGET }
        val updatePending = PendingIntent.getBroadcast(context, 3, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_title, updatePending)

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_HOME
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPending = PendingIntent.getActivity(context, 4, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_balance, mainPending)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}