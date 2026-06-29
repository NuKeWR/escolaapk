package com.nuklear.escola.notification

import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

/**
 * Worker que roda uma vez por dia para verificar:
 * - Mensalidades vencendo
 * - Aniversariantes
 */
class DailyCheckWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            runBlocking {
                NotificationHelper.verificarENotificar(applicationContext)
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("DailyCheck", "Erro no worker", e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "escola_nuklear_daily_check"

        /**
         * Agenda a verificação diária. 
         * Roda a cada 12 horas (para garantir que pega de manhã e à tarde).
         */
        fun agendar(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyCheckWorker>(
                12, TimeUnit.HOURS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.d("DailyCheck", "Worker agendado (a cada 12h)")
        }

        /**
         * Executa verificação AGORA (para teste ou ao abrir o app).
         */
        fun executarAgora(context: Context) {
            val request = OneTimeWorkRequestBuilder<DailyCheckWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
