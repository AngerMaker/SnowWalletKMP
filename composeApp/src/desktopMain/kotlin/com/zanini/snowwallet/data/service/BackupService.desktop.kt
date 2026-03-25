package com.zanini.snowwallet.data.service

import java.io.File

class DesktopBackupService : BackupService {
    override suspend fun exportDatabase(): String {
        return try {
            val userHome = System.getProperty("user.home")
            val dbFile = File(userHome, "SnowWallet/financeiro.db")

            if (!dbFile.exists()) {
                return "Erro: Banco de dados original não encontrado."
            }

            val backupDir = File(userHome, "Downloads/SnowWallet_Backup")
            backupDir.mkdirs()

            val backupFile = File(backupDir, "financeiro_backup_${System.currentTimeMillis()}.db")

            dbFile.copyTo(backupFile, overwrite = true)

            "Backup salvo em:\n${backupFile.absolutePath}"
        } catch (e: Exception) {
            "Erro no backup Desktop: ${e.message}"
        }
    }
}