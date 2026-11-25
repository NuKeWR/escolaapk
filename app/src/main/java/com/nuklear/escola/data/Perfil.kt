package com.nuklear.escola.data

import android.content.Context

/** Nome de quem usa o app, mostrado na saudação do Início ("Olá, Wagner"). Editável no Perfil. */
object Perfil {
    private const val ARQUIVO = "perfil"
    private const val K_NOME = "nome"
    private const val NOME_PADRAO = "Wagner"

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)

    fun nome(ctx: Context): String = prefs(ctx).getString(K_NOME, NOME_PADRAO) ?: NOME_PADRAO

    fun salvarNome(ctx: Context, nome: String) {
        prefs(ctx).edit().putString(K_NOME, nome.trim()).apply()
    }
}
