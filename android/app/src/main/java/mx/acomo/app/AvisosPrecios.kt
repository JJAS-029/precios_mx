package mx.acomo.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import mx.acomo.app.data.AlmacenLocal
import mx.acomo.app.data.Repositorio
import mx.acomo.app.data.bajadas
import mx.acomo.app.data.textoAviso
import java.util.concurrent.TimeUnit

/**
 * Avisos de precios bajos, sin servidor: WorkManager descarga los datos del mercado elegido
 * dos veces al día y, cuando hay una semana nueva, avisa si bajó algún producto seguido.
 */
object AvisosPrecios {
    private const val TRABAJO = "avisos_precios"
    private const val CANAL = "bajadas"
    private const val ID_NOTIFICACION = 1
    const val EXTRA_PRODUCTO = "producto"

    /** Programa o cancela la revisión según haya avisos activos y productos seguidos. */
    fun programar(contexto: Context) {
        val almacen = AlmacenLocal.de(contexto)
        val wm = WorkManager.getInstance(contexto)
        if (!almacen.avisos || almacen.seguidos.isEmpty()) {
            wm.cancelUniqueWork(TRABAJO)
            return
        }
        val pedido = PeriodicWorkRequestBuilder<RevisarPrecios>(12, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        wm.enqueueUniquePeriodicWork(TRABAJO, ExistingPeriodicWorkPolicy.KEEP, pedido)
    }

    /** Solo pruebas: vuelve a revisar la semana actual ya mismo. */
    fun probarAhora(contexto: Context) {
        AlmacenLocal.de(contexto).semanaAvisada = ""
        WorkManager.getInstance(contexto).enqueue(OneTimeWorkRequestBuilder<RevisarPrecios>().build())
    }

    fun permisoConcedido(contexto: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    internal fun notificar(contexto: Context, titulo: String, lineas: List<String>, productoId: String?) {
        if (!permisoConcedido(contexto)) return
        val nm = contexto.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CANAL, "Precios que bajaron", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Avisos cuando baja un producto que sigues"
            },
        )
        val abrir = Intent(contexto, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            productoId?.let { putExtra(EXTRA_PRODUCTO, it) }
        }
        val pendiente = PendingIntent.getActivity(
            contexto, 0, abrir, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val estilo = NotificationCompat.InboxStyle().also { s -> lineas.take(5).forEach(s::addLine) }
        val notificacion = NotificationCompat.Builder(contexto, CANAL)
            .setSmallIcon(R.drawable.ic_aviso)
            .setColor(0xFF4C662B.toInt())
            .setContentTitle(titulo)
            .setContentText(lineas.joinToString(" · "))
            .setStyle(if (lineas.size == 1) NotificationCompat.BigTextStyle().bigText(lineas[0]) else estilo)
            .setContentIntent(pendiente)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(contexto).notify(ID_NOTIFICACION, notificacion)
        } catch (_: SecurityException) {
            // El usuario quitó el permiso entre la revisión y el aviso.
        }
    }
}

class RevisarPrecios(contexto: Context, params: WorkerParameters) : CoroutineWorker(contexto, params) {
    override suspend fun doWork(): Result {
        val almacen = AlmacenLocal.de(applicationContext)
        val seguidos = almacen.seguidos.keys
        val mercado = almacen.mercado ?: return Result.success()
        if (!almacen.avisos || seguidos.isEmpty()) return Result.success()

        val repo = Repositorio.de(applicationContext)
        val datos = runCatching { repo.descargarMercado(mercado) }.getOrElse { return Result.retry() }
        val semana = datos.semanas.lastOrNull() ?: return Result.success()
        if (semana <= almacen.semanaAvisada) return Result.success()
        almacen.semanaAvisada = semana

        val b = bajadas(seguidos, datos.productos, semana)
        if (b.isEmpty()) return Result.success()
        val ciudad = repo.indiceCache()?.mercados?.find { it.id == mercado }?.ciudad.orEmpty()
        val (titulo, lineas) = textoAviso(b, ciudad)
        AvisosPrecios.notificar(applicationContext, titulo, lineas, b.singleOrNull()?.producto?.id)
        return Result.success()
    }
}
