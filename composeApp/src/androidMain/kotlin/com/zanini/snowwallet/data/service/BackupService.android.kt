package com.zanini.snowwallet.data.service

import android.content.Context
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class AndroidBackupService(private val context: Context) : BackupService {

    override suspend fun exportDatabase(): String {
        return try {
            val dbName = "snowwallet.db"
            val dbFile = context.getDatabasePath(dbName)

            if (!dbFile.exists()) {
                return "Erro: Banco de dados não encontrado."
            }

            // Define a pasta de destino: /Android/data/com.zanini.snowwallet/files/Backups
            val backupDir = File(context.getExternalFilesDir(null), "Backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            // Cria nome do arquivo com data: snowwallet_backup_2023-10-25.db
            val hoje = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val timestamp = "${hoje.year}-${hoje.monthNumber}-${hoje.dayOfMonth}_${hoje.hour}-${hoje.minute}"
            val backupFile = File(backupDir, "snowwallet_backup_$timestamp.db")

            // Copia o arquivo
            FileInputStream(dbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }

            "Backup salvo em:\n${backupFile.absolutePath}"
        } catch (e: Exception) {
            e.printStackTrace()
            "Falha ao realizar backup: ${e.message}"
        }
    }
}