package com.zanini.snowwallet.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
// Imports específicos de plataforma seriam necessários aqui para FilePicker,
// mas para manter simples, faremos uma simulação ou usaremos lógica comum.

class FileBackupRepository : BackupRepository {
    override val statusBackup = MutableStateFlow("")

    override fun setContext(context: Any?) {}

    override suspend fun realizarBackup() {
        statusBackup.value = "No PC/Web, use a opção 'Exportar Dados' nas configurações."
        // Aqui implementaria a cópia do arquivo .db para uma pasta escolhida pelo usuário
    }

    override suspend fun restaurarBackup() {
        statusBackup.value = "No PC/Web, use a opção 'Importar Dados'."
    }
}