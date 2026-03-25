package com.zanini.snowwallet

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import com.zanini.snowwallet.data.repository.AndroidDriveBackupRepository
import com.zanini.snowwallet.data.repository.BackupRepository
import com.zanini.snowwallet.data.repository.ContaRepository
import com.zanini.snowwallet.data.repository.TransacaoRepository
import com.zanini.snowwallet.widget.SnowWalletWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val backupRepository: BackupRepository by inject()
    private val contaRepository: ContaRepository by inject()
    private val transacaoRepository: TransacaoRepository by inject()

    // Estado local para controlar a navegação vinda do Widget
    private var pendingRoute by mutableStateOf<String?>(null)

    private val googleLoginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            (backupRepository as? AndroidDriveBackupRepository)?.let { repo ->
                GlobalScope.launch(Dispatchers.Main) {
                    repo.handleLoginResult(data)
                }
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> 
        // Lida com resposta se necessário
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Captura clique do Widget na abertura
        intent?.getStringExtra("navigate_to")?.let { route ->
            pendingRoute = route
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 2. Monitoramento de Saldo (Melhorado com commit síncrono no background)
        GlobalScope.launch(Dispatchers.IO) {
            combine(
                contaRepository.getTodasContas(),
                transacaoRepository.getAllTransacoes()
            ) { contas, transacoes ->
                val saldoInicial = contas.fold(0.0) { acc, conta -> acc + conta.saldoInicial }
                val movimentacao = transacoes.fold(0.0) { acc, t ->
                    if (t.pago && t.contaId != null) {
                        when (t.tipo) {
                            "RECEITA" -> acc + t.valor
                            "DESPESA" -> acc - t.valor
                            else -> acc
                        }
                    } else {
                        acc
                    }
                }
                saldoInicial + movimentacao
            }.collect { saldoTotal ->
                try {
                    val context = applicationContext
                    val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)

                    // CORREÇÃO: Usar commit() para garantir que salvou antes de atualizar o widget
                    prefs.edit().putFloat("saldo_disponivel_cache", saldoTotal.toFloat()).commit()

                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, SnowWalletWidget::class.java))

                    // Chama update direto
                    SnowWalletWidget().onUpdate(context, appWidgetManager, ids)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        backupRepository.setContext(this)

        setContent {
            // Monitora login do Google
            if (backupRepository is AndroidDriveBackupRepository) {
                val repo = backupRepository as AndroidDriveBackupRepository
                val loginIntent by repo.loginIntent.collectAsState()

                LaunchedEffect(loginIntent) {
                    loginIntent?.let { intent ->
                        googleLoginLauncher.launch(intent)
                        repo.limparIntent()
                    }
                }
            }

            // --- PASSA A ROTA PARA O APP ---
            App(
                deepLinkDestination = pendingRoute,
                onDeepLinkConsumed = {
                    pendingRoute = null // Limpa para não navegar de novo sem querer
                    intent?.removeExtra("navigate_to") // Limpa o intent do Android também
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Captura clique do Widget com o App já aberto
        intent.getStringExtra("navigate_to")?.let { route ->
            pendingRoute = route
        }
    }
}