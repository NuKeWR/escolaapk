package com.nuklear.escola.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nuklear.escola.MainActivity
import com.nuklear.escola.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

object NotificationHelper {

    private const val TAG = "NukNotif"
    private const val CHANNEL_VENCIMENTO = "vencimento"
    private const val CHANNEL_ANIVERSARIO = "aniversario"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)

            nm.createNotificationChannel(NotificationChannel(
                CHANNEL_VENCIMENTO, "Vencimento de Mensalidade",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Avisos de mensalidades vencendo" })

            nm.createNotificationChannel(NotificationChannel(
                CHANNEL_ANIVERSARIO, "Aniversariantes",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Aniversariantes do dia" })
        }
    }

    /**
     * Verifica e envia notificações de:
     * - Mensalidades vencendo hoje
     * - Aniversariantes do dia
     */
    suspend fun verificarENotificar(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.get(context)
                val hoje = LocalDate.now()
                val diaHoje = hoje.dayOfMonth
                val mesHoje = "%02d".format(hoje.monthValue)
                val diaHojeStr = "%02d".format(diaHoje)
                val comp = "${hoje.year}-$mesHoje"

                // ─── Mensalidades vencendo hoje ───
                val ativos = db.alunoDao().getAtivos()
                val vencendoHoje = mutableListOf<String>()

                for (aluno in ativos) {
                    if (aluno.tipo.lowercase() in listOf("bolsista", "professor")) continue
                    if (aluno.diaVencimento != diaHoje) continue

                    val ult = db.pagamentoDao().ultimaCompetencia(aluno.id)
                    if (ult == null || ult < comp) {
                        vencendoHoje.add(aluno.nome)
                    }
                }

                if (vencendoHoje.isNotEmpty()) {
                    enviarNotificacao(
                        context,
                        CHANNEL_VENCIMENTO,
                        1001,
                        "💳 ${vencendoHoje.size} mensalidade(s) vencem hoje!",
                        vencendoHoje.joinToString(", ")
                    )
                }

                // ─── Aniversariantes do dia ───
                val aniversariantes = db.alunoDao().aniversariantes(mesHoje)
                    .filter { a ->
                        a.dataNascimento.length >= 10 &&
                        a.dataNascimento.substring(8, 10) == diaHojeStr
                    }

                if (aniversariantes.isNotEmpty()) {
                    val nomes = aniversariantes.joinToString(", ") { it.nome }
                    enviarNotificacao(
                        context,
                        CHANNEL_ANIVERSARIO,
                        1002,
                        "🎂 ${aniversariantes.size} aniversariante(s) hoje!",
                        nomes
                    )
                }

                Log.d(TAG, "Verificado: ${vencendoHoje.size} vencimentos, ${aniversariantes.size} aniversários")
            } catch (e: Exception) {
                Log.e(TAG, "Erro verificando notificações", e)
            }
        }
    }

    private fun enviarNotificacao(context: Context, channel: String, id: Int, title: String, text: String) {
        // Verificar permissão (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Sem permissão de notificação")
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
