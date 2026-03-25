package com.zanini.snowwallet.data.repository

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.zanini.snowwallet.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.*
import java.util.Collections
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DesktopDriveBackupRepository(
    private val database: AppDatabase
) : BackupRepository {

    override val statusBackup = MutableStateFlow("")
    private val _emailConectado = MutableStateFlow<String?>(null)
    override val emailConectado: StateFlow<String?> = _emailConectado

    private val APPLICATION_NAME = "SnowWallet Desktop"
    private val JSON_FACTORY = GsonFactory.getDefaultInstance()

    private val TOKENS_DIRECTORY_FILE: File by lazy {
        val userHome = System.getProperty("user.home")
        File(userHome, "Documents/SnowWallet/tokens").apply { mkdirs() }
    }

    private val SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE)

    private fun getDbFile(): File {
        val userHome = System.getProperty("user.home")
        return File(userHome, "Documents/SnowWallet/snowwallet.db")
    }

    init {
        try {
            val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
            val flow = getFlow(httpTransport)
            val credential = flow.loadCredential("user")

            if (credential != null && (credential.refreshToken != null || credential.expiresInSeconds > 60)) {
                _emailConectado.value = "Conectado (Clique para ver e-mail)"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun setContext(context: Any?) { }

    private fun getFlow(httpTransport: com.google.api.client.http.HttpTransport): GoogleAuthorizationCodeFlow {
        val inputStream = DesktopDriveBackupRepository::class.java.getResourceAsStream("/client_secret.json")
            ?: throw Exception("Arquivo 'client_secret.json' não encontrado em src/desktopMain/resources/")

        val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(inputStream))

        return GoogleAuthorizationCodeFlow.Builder(
            httpTransport, JSON_FACTORY, clientSecrets, SCOPES
        )
            .setDataStoreFactory(FileDataStoreFactory(TOKENS_DIRECTORY_FILE))
            .setAccessType("offline")
            .build()
    }

    private fun getDriveService(): Drive? {
        return try {
            val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
            val flow = getFlow(httpTransport)

            var credential = flow.loadCredential("user")
            if (credential == null || (credential.refreshToken == null && credential.expiresInSeconds < 60)) {
                credential = AuthorizationCodeInstalledApp(flow, LocalServerReceiver()).authorize("user")
            }

            Drive.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build()
        } catch (e: Exception) {
            statusBackup.value = "Erro de Auth: ${e.message}"
            e.printStackTrace()
            null
        }
    }

    private fun fetchUserEmail(service: Drive): String {
        return try {
            val about = service.about().get().setFields("user").execute()
            about.user.emailAddress
        } catch (e: Exception) {
            "Usuário Google"
        }
    }

    override suspend fun fazerLogin() {
        withContext(Dispatchers.IO) {
            try {
                statusBackup.value = "Trocando conta..."

                val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
                val flow = getFlow(httpTransport)

                flow.credentialDataStore.delete("user")
                _emailConectado.value = null

                val credential = AuthorizationCodeInstalledApp(flow, LocalServerReceiver()).authorize("user")

                val service = Drive.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build()

                val email = fetchUserEmail(service)
                _emailConectado.value = email

                statusBackup.value = "Login realizado: $email"
            } catch (e: Exception) {
                statusBackup.value = "Erro ao logar: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    // --- FUNÇÕES DE ZIP (Iguais ao Android) ---
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

    override suspend fun realizarBackup() {
        withContext(Dispatchers.IO) {
            try {
                statusBackup.value = "Preparando Backup..."

                try { database.close() } catch (e: Exception) { }

                val driveService = getDriveService() ?: return@withContext

                if (_emailConectado.value == null || _emailConectado.value!!.contains("Clique")) {
                    _emailConectado.value = fetchUserEmail(driveService)
                }

                val dbFile = getDbFile()
                val walFile = File(dbFile.path + "-wal")
                val shmFile = File(dbFile.path + "-shm")

                if (!dbFile.exists() || dbFile.length() == 0L) {
                    statusBackup.value = "Erro: Banco vazio ou inexistente."
                    return@withContext
                }

                val zipFile = File(dbFile.parentFile, "backup_temp.zip")
                zipFiles(listOf(dbFile, walFile, shmFile), zipFile)

                statusBackup.value = "Enviando para o Drive..."
                val folderId = getTargetAppFolder(driveService) ?: return@withContext

                val query = "name = 'snowwallet_backup.zip' and '$folderId' in parents and trashed = false"
                val fileList = driveService.files().list().setQ(query).setSpaces("drive").setFields("files(id, name)").execute()

                val mediaContent = FileContent("application/zip", zipFile)

                if (fileList.files.isNotEmpty()) {
                    driveService.files().update(fileList.files[0].id, null, mediaContent).execute()
                    statusBackup.value = "Backup atualizado com sucesso!"
                } else {
                    val fileMetadata = com.google.api.services.drive.model.File()
                    fileMetadata.name = "snowwallet_backup.zip"
                    fileMetadata.parents = listOf(folderId)
                    driveService.files().create(fileMetadata, mediaContent).execute()
                    statusBackup.value = "Novo backup criado!"
                }

                zipFile.delete()

            } catch (e: Exception) {
                e.printStackTrace()
                statusBackup.value = "Erro: ${e.message}"
            }
        }
    }

    override suspend fun restaurarBackup() {
        withContext(Dispatchers.IO) {
            try {
                statusBackup.value = "Buscando backup..."
                val driveService = getDriveService() ?: return@withContext

                // PROCURA GLOBALMENTE (Não tenta criar pastas aqui para evitar duplicação vazia)
                val query = "(name = 'snowwallet_backup.zip' or name = 'snowwallet_backup.db') and trashed = false"
                val fileList = driveService.files().list().setQ(query).setSpaces("drive").setFields("files(id, name)").execute()

                if (fileList.files.isEmpty()) {
                    statusBackup.value = "Nenhum backup encontrado no Drive."
                    return@withContext
                }

                statusBackup.value = "Baixando..."
                val file = fileList.files[0]
                val isZip = file.name.endsWith(".zip")

                val tempFile = File.createTempFile("restore_temp", if (isZip) ".zip" else ".db")
                val outputStream = FileOutputStream(tempFile)
                driveService.files().get(file.id).executeMediaAndDownloadTo(outputStream)
                outputStream.flush()
                outputStream.close()

                try { database.close() } catch (e: Exception) {}

                val dbFile = getDbFile()
                val dbDir = dbFile.parentFile
                val walFile = File(dbFile.path + "-wal")
                val shmFile = File(dbFile.path + "-shm")

                if (walFile.exists()) walFile.delete()
                if (shmFile.exists()) shmFile.delete()
                if (dbFile.exists()) dbFile.delete()

                if (isZip) {
                    unzipFile(tempFile, dbDir)
                } else {
                    tempFile.copyTo(dbFile, overwrite = true)
                }

                tempFile.delete()

                statusBackup.value = "SUCESSO! Feche e abra o App para ver os dados."

            } catch (e: Exception) {
                e.printStackTrace()
                statusBackup.value = "Erro ao restaurar: ${e.message}"
            }
        }
    }
}