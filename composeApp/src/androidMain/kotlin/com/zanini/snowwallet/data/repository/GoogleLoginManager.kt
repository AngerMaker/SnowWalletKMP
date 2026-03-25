package com.zanini.snowwallet.data.repository

/**
 * Objeto simples para comunicar o pedido de Login entre o Repositório (que não tem UI)
 * e a MainActivity (que tem o contexto para abrir janelas do Google).
 */
object GoogleLoginManager {
    // A MainActivity vai preencher isso com a função que abre a tela do Google
    var launchLogin: (() -> Unit)? = null
}