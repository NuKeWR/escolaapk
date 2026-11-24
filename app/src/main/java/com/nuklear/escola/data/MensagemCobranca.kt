package com.nuklear.escola.data

import android.content.Context
import java.time.YearMonth
import java.util.Locale

/**
 * Modelo da mensagem de cobrança enviada pelo WhatsApp (editável em Configurações).
 *
 * Campos que o app troca automaticamente em cada cobrança:
 *   {nome}  -> responsável (ou o aluno, se não houver), só os dois primeiros nomes
 *   {aluno} -> nome do aluno, só os dois primeiros nomes
 *   {mes}   -> primeiro mês em aberto, no formato 09/2026
 *   {valor} -> mensalidade do aluno, no formato R$ 80,00
 */
object MensagemCobranca {
    private const val ARQUIVO = "mensagens"
    private const val K_COBRANCA = "cobranca"

    const val PADRAO =
        "Olá, {nome}! Tudo bem? 😊\n\n" +
        "Identificamos que sua mensalidade referente a {mes}, no valor de {valor}, " +
        "ainda consta em aberto em nosso sistema.\n\n" +
        "Poderia, por gentileza, verificar se o pagamento já foi realizado?\n\n" +
        "Caso já tenha efetuado o pagamento, pedimos que desconsidere esta mensagem.\n\n" +
        "Agradecemos pela confiança e por fazer parte da Escola Nuklear! 🥋💜"

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)

    /** Modelo salvo pelo usuário, ou o padrão se nunca foi editado (ou ficou vazio). */
    fun carregar(ctx: Context): String {
        val salvo = prefs(ctx).getString(K_COBRANCA, null)
        return if (salvo.isNullOrBlank()) PADRAO else salvo
    }

    fun salvar(ctx: Context, texto: String) {
        prefs(ctx).edit().putString(K_COBRANCA, texto).apply()
    }

    fun restaurarPadrao(ctx: Context) {
        prefs(ctx).edit().remove(K_COBRANCA).apply()
    }

    /** "Maria da Silva Santos" -> "Maria da" (os dois primeiros nomes). */
    fun doisPrimeirosNomes(nome: String): String =
        nome.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString(" ")

    /** 80.0 -> "R$ 80,00" */
    fun formatarValor(valor: Double): String =
        "R$ " + String.format(Locale("pt", "BR"), "%.2f", valor)

    /**
     * Primeiro mês em aberto: o mês seguinte ao último pago.
     * Sem nenhum pagamento (ou formato desconhecido), usa o mês atual.
     */
    fun mesEmAberto(ultimaCompetenciaPaga: String?): String {
        val mes = try {
            if (ultimaCompetenciaPaga.isNullOrBlank()) YearMonth.now()
            else YearMonth.parse(ultimaCompetenciaPaga).plusMonths(1)
        } catch (e: Exception) {
            YearMonth.now()
        }
        return String.format(Locale.US, "%02d/%d", mes.monthValue, mes.year)
    }

    fun montar(modelo: String, nome: String, aluno: String, mes: String, valor: Double): String =
        modelo
            .replace("{nome}", doisPrimeirosNomes(nome))
            .replace("{aluno}", doisPrimeirosNomes(aluno))
            .replace("{mes}", mes)
            .replace("{valor}", formatarValor(valor))
}
