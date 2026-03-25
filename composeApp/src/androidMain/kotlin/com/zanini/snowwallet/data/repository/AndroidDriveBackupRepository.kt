package com.zanini.snowwallet.data.repository

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.zanini.snowwallet.data.local.AppDatabase
import com.zanini.snowwallet.widget.SnowWalletWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.*
import java.util.Calendar
import java.util.Collections
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.system.exitProcess

class AndroidDriveBackupRepository(
    private val database: AppDatabase,
    private val context: Context
) : BackupRepository {

    override val statusBackup = MutableStateFlow("")
    private val _emailConectado = MutableStateFlow<String?>(null)
    override val emailConectado: StateFlow<String?> = _emailConectado

    private val _loginIntent = MutableStateFlow<Intent?>(null)
    val loginIntent: StateFlow<Intent?> = _loginIntent

    init {
        verificarLoginSilencioso()
    }

    override fun setContext(context: Any?) { }

    fun limparIntent() {
        _loginIntent.value = null
    }

    private fun verificarLoginSilencioso() {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account != null) {
                _emailConectado.value = account.email
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun getDriveService(): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(context, Collections.singleton(DriveScopes.DRIVE_FILE))
        credential.selectedAccount = account.account
        return Drive.Builder(AndroidHttp.newCompatibleTransport(), GsonFactory.getDefaultInstance(), credential)
            .setApplicationName("SnowWallet Android")
            .build()
    }

    override suspend fun fazerLogin() {
        statusBackup.value = "Abrindo Google..."
        try {
            val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE_FILE))
                .build()
            val client = GoogleSignIn.getClient(context, signInOptions)
            _loginIntent.value = client.signInIntent
        } catch (e: Exception) {
            statusBackup.value = "Erro ao iniciar login: ${e.message}"
        }
    }

    suspend fun handleLoginResult(intent: Intent?) {
        if (intent == null) return
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            val account = task.await()
            _emailConectado.value = account.email
            statusBackup.value = "Conectado: ${account.email}"
        } catch (e: Exception) {
            statusBackup.value = "Login falhou."
        }
    }

    // --- FUNÇÕES DE ARQUIVO E ZIP ---

    private fun zipFiles(files: List<File>, zipFile: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { out ->
            files.filter { it.exists() }.forEach { file ->
                FileInputStream(file).use { origin ->
                    val entry = ZipEntry(file.name)
                    out.putNextEntry(entry)
                    origin.copyTo(out)
                }
            }
        }
    }

    private fun unzipFile(zipFile: File, targetDir: File) {
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val outFile = File(targetDir, entry.name)
                if (outFile.exists()) outFile.delete()

                FileOutputStream(outFile).use { fos ->
                    zis.copyTo(fos)
                }
                entry = zis.nextEntry
            }
        }
    }

    private fun getOrCreateFolder(driveService: Drive, folderName: String, parentId: String = "root"): String? {
        try {
            val query = "mimeType = 'application/vnd.google-apps.folder' and name = '$folderName' and '$parentId' in parents and trashed = false"
            val fileList = driveService.files().list().setQ(query).setSpaces("drive").setFields("files(id, name)").execute()
            if (fileList.files.isNotEmpty()) return fileList.files[0].id

            val folderMetadata = com.google.api.services.drive.model.File()
            folderMetadata.name = folderName
            folderMetadata.mimeType = "application/vnd.google-apps.folder"
            folderMetadata.parents = listOf(parentId)
            return driveService.files().create(folderMetadata).setFields("id").execute().id
        } catch (e: Exception) { return null }
    }

    private fun getTargetAppFolder(driveService: Drive): String? {
        val snowAppsId = getOrCreateFolder(driveService, "SnowApps", "root") ?: return null
        return getOrCreateFolder(driveService, "SnowWallet", snowAppsId)
    }

    private fun restartApp() {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent?.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        context.startActivity(mainIntent)
        exitProcess(0)
    }

    // --- CÁLCULO E ATUALIZAÇÃO DO WIDGET PÓS-RESTORE ---

    private fun recalcularSaldoEAtualizarWidget() {
        var db: SQLiteDatabase? = null
        try {
            val dbPath = context.getDatabasePath("snowwallet.db").absolutePath
            // Abre o banco em modo leitura para calcular o saldo diretamente do arquivo restaurado
            db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)

            // 1. Definição do Fim do Mês Atual (Igual ao HomeViewModel)
            val calendar = Calendar.getInstance()
            // Configura para o último milissegundo do mês
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val fimMesMillis = calendar.timeInMillis

            // 2. Queries SQL alinhadas com a lógica de "Fluxo de Caixa"
            // Saldo Disponível = Saldo Inicial + (Todas Receitas) - (Todas Despesas) acumuladas até o fim do mês

            val queryInicial = "SELECT SUM(saldoInicial) FROM contas"

            // Soma TODAS as Receitas (Conta) até o fim do mês (pagas ou não)
            val queryReceitas = "SELECT SUM(valor) FROM transacoes WHERE tipo = 'RECEITA' AND cartaoId IS NULL AND data <= $fimMesMillis"

            // Soma TODAS as Despesas (Conta) até o fim do mês (pagas ou não)
            val queryDespesas = "SELECT SUM(valor) FROM transacoes WHERE tipo = 'DESPESA' AND cartaoId IS NULL AND data <= $fimMesMillis"

            fun executeSum(query: String): Double {
                val cursor = db.rawQuery(query, null)
                return try {
                    if (cursor.moveToFirst()) cursor.getDouble(0) else 0.0
                } catch (e: Exception) {
                    0.0
                } finally {
                    cursor.close()
                }
            }

            val saldoInicial = executeSum(queryInicial)
            val totalReceitas = executeSum(queryReceitas)
            val totalDespesas = executeSum(queryDespesas)

            // Matemática exata do "Disponível (Previsto)"
            val saldoFinal = saldoInicial + totalReceitas - totalDespesas

            // 3. Atualiza SharedPreferences do Widget (COMMIT síncrono para garantir escrita)
            val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            prefs.edit().putFloat("saldo_disponivel_cache", saldoFinal.toFloat()).commit()

            // 4. Força update visual
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, SnowWalletWidget::class.java))
            val intent = Intent(context, SnowWalletWidget::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db?.close()
        }
    }

    override suspend fun realizarBackup() {
        withContext(Dispatchers.IO) {
            try {
                statusBackup.value = "Compactando dados..."

                try {
                    if (database.isOpen) {
                        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").close()
                    }
                } catch (e: Exception) { }

                val driveService = getDriveService()
                if (driveService == null) {
                    statusBackup.value = "Erro: Conecte ao Google."
                    return@withContext
                }

                val dbFile = context.getDatabasePath("snowwallet.db")
                val walFile = File(dbFile.path + "-wal")
                val shmFile = File(dbFile.path + "-shm")

                val zipFile = File(context.cacheDir, "backup_temp.zip")
                zipFiles(listOf(dbFile, walFile, shmFile), zipFile)

                statusBackup.value = "Enviando ZIP para o Drive..."
                val folderId = getTargetAppFolder(driveService) ?: return@withContext

                val query = "name = 'snowwallet_backup.zip' and '$folderId' in parents and trashed = false"
                val fileList = driveService.files().list().setQ(query).setSpaces("drive").setFields("files(id, name)").execute()

                val mediaContent = FileContent("application/zip", zipFile)

                if (fileList.files.isNotEmpty()) {
                    driveService.files().update(fileList.files[0].id, null, mediaContent).execute()
                } else {
                    val fileMetadata = com.google.api.services.drive.model.File()
                    fileMetadata.name = "snowwallet_backup.zip"
                    fileMetadata.parents = listOf(folderId)
                    driveService.files().create(fileMetadata, mediaContent).execute()
                }

                zipFile.delete()
                statusBackup.value = "Backup concluído com sucesso!"

            } catch (e: Exception) {
                e.printStackTrace()
                statusBackup.value = "Erro no backup: ${e.message}"
            }
        }
    }

    override suspend fun restaurarBackup() {
        withContext(Dispatchers.IO) {
            try {
                statusBackup.value = "Buscando backup..."
                val driveService = getDriveService()
                if (driveService == null) {
                    statusBackup.value = "Erro: Conecte ao Google."
                    return@withContext
                }

                val folderId = getTargetAppFolder(driveService)
                var query = "name = 'snowwallet_backup.zip' and trashed = false"
                if (folderId != null) query += " and '$folderId' in parents"

                var fileList = driveService.files().list().setQ(query).setSpaces("drive").setFields("files(id, name)").execute()

                val isZip: Boolean
                if (fileList.files.isEmpty()) {
                    query = "name = 'snowwallet_backup.db' and trashed = false"
                    if (folderId != null) query += " and '$folderId' in parents"
                    fileList = driveService.files().list().setQ(query).setSpaces("drive").setFields("files(id, name)").execute()
                    isZip = false
                } else {
                    isZip = true
                }

                if (fileList.files.isEmpty()) {
                    statusBackup.value = "Nenhum backup encontrado."
                    return@withContext
                }

                statusBackup.value = "Baixando..."
                val fileId = fileList.files[0].id
                val tempDownload = File.createTempFile("restore_download", if (isZip) ".zip" else ".db")

                val outputStream = FileOutputStream(tempDownload)
                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
                outputStream.flush()
                outputStream.close()

                val dbFile = context.getDatabasePath("snowwallet.db")
                val dbDir = dbFile.parentFile

                try { if (database.isOpen) database.close() } catch (e: Exception) {}

                val walFile = File(dbFile.path + "-wal")
                val shmFile = File(dbFile.path + "-shm")
                if (dbFile.exists()) dbFile.delete()
                if (walFile.exists()) walFile.delete()
                if (shmFile.exists()) shmFile.delete()

                if (isZip) {
                    unzipFile(tempDownload, dbDir!!)
                } else {
                    tempDownload.copyTo(dbFile, overwrite = true)
                }

                tempDownload.delete()

                statusBackup.value = "Atualizando widget..."
                // Agora calcula o saldo projetado do mês atual (igual à Home)
                recalcularSaldoEAtualizarWidget()

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Dados recuperados! O app irá reiniciar...", Toast.LENGTH_LONG).show()
                    delay(1500)
                    restartApp()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                statusBackup.value = "Erro ao restaurar: ${e.message}"
            }
        }
    }
}