package com.nuklear.escola.security

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Guarda o PIN do app (nunca o número em si, só um "hash" com sal).
 * É uma trava contra curiosos com o celular desbloqueado; não criptografa o banco de dados.
 */
object PinStore {
    private const val ARQUIVO = "seguranca"
    private const val K_HASH = "pin_hash"
    private const val K_SALT = "pin_salt"

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)

    fun temPin(ctx: Context): Boolean = prefs(ctx).getString(K_HASH, null) != null

    fun definir(ctx: Context, pin: String) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        prefs(ctx).edit()
            .putString(K_SALT, saltB64)
            .putString(K_HASH, hash(saltB64, pin))
            .apply()
    }

    fun conferir(ctx: Context, pin: String): Boolean {
        val p = prefs(ctx)
        val salt = p.getString(K_SALT, null) ?: return false
        val esperado = p.getString(K_HASH, null) ?: return false
        return MessageDigest.isEqual(
            esperado.toByteArray(Charsets.UTF_8),
            hash(salt, pin).toByteArray(Charsets.UTF_8)
        )
    }

    fun remover(ctx: Context) {
        prefs(ctx).edit().remove(K_HASH).remove(K_SALT).apply()
    }

    private fun hash(salt: String, pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
