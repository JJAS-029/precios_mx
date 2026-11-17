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
import mx.acomo.app.data.empiezan
import mx.acomo.app.data.mesActual
import mx.acomo.app.data.textoTemporada
import java.util.concurrent.TimeUnit

/**
 * Avisos de precios bajos, sin servidor: WorkManager descarga los datos del mercado elegido
 * dos veces al día y, cuando hay una semana nueva, avisa si bajó algún producto seguido.
 */
object AvisosPrecios {
    private const val TRABAJO = "avisos_precios"
    internal const val CANAL_BAJADAS = "bajadas"
    internal const val CANAL_TEMPORADA = "temporada"
    internal const val ID_BAJADAS = 1
    internal const val ID_TEMPORADA = 2
    const val EXTRA_PRODUCTO = "producto"

    /**
     * Programa o cancela la revisión: hace falta si hay productos seguidos con avisos activos, o si está
     * activo el aviso de temporada (y hay permiso de notificaciones, para no descargar en balde).
     */
    fun programar(contexto: Context) {
        val almacen = AlmacenLocal.de(contexto)
        val wm = WorkManager.getInstance(contexto)
        val bajadas = almacen.avisos && almacen.seguidos.isNotEmpty()
        val temporada = almacen.avisosTemporada && permisoConcedido(contexto)
        if (!bajadas && !temporada) {
            wm.cancelUniqueWork(TRABAJO)
            return
        }
        val pedido = PeriodicWorkRequestBuilder<RevisarPrecios>(12, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        wm.enqueueUniquePeriodicWork(TRABAJO, ExistingPeriodicWorkPolicy.KEEP, pedido)
    }

    /** Solo pruebas: simula el cambio de mes para el aviso de temporada. */
    fun probarTemporada(contexto: Context) {
        AlmacenLocal.de(contexto).mesTemporada = "2000-01"
        WorkManager.getInstance(contexto).enqueue(OneTimeWorkRequestBuilder<RevisarPrecios>().build())
    }

    /** Solo pruebas: vuelve a revisar la semana actual ya mismo. */
    fun probarAhora(contexto: Context) {
        AlmacenLocal.de(contexto).semanaAvisada = ""
        WorkManager.getInstance(contexto).enqueue(OneTimeWorkRequestBuilder<RevisarPrecios>().build())
    }

    fun permisoConcedido(contexto: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    internal fun notificar(
        contexto: Context,
        titulo: String,
        lineas: List<String>,
        productoId: String?,
        canal: String = CANAL_BAJADAS,
        id: Int = ID_BAJADAS,
    ) {
        if (!permisoConcedido(contexto)) return
        val nm = contexto.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CANAL_BAJADAS, "Precios que bajaron", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Avisos cuando baja un producto que sigues"
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CANAL_TEMPORADA, "Temporada", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Un aviso al mes cuando empieza la temporada de algún producto"
            },
        )
        val abrir = Intent(contexto, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            productoId?.let { putExtra(EXTRA_PRODUCTO, it) }
        }
        val pendiente = PendingIntent.getActivity(
            contexto, id, abrir, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val estilo = NotificationCompat.InboxStyle().also { s -> lineas.take(5).forEach(s::addLine) }
        val notificacion = NotificationCompat.Builder(contexto, canal)
            .setSmallIcon(R.drawable.ic_aviso)
            .setColor(0xFF4C662B.toInt())
            .setContentTitle(titulo)
            .setContentText(lineas.joinToString(" · "))
            .setStyle(if (lineas.size == 1) NotificationCompat.BigTextStyle().bigText(lineas[0]) else estilo)
            .setContentIntent(pendiente)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(contexto).notify(id, notificacion)
        } catch (_: SecurityException) {
            // El usuario quitó el permiso entre la revisión y el aviso.
        }
    }
}

class RevisarPrecios(contexto: Context, params: WorkerParameters) : CoroutineWorker(contexto, params) {
    override suspend fun doWork(): Result {
        val almacen = AlmacenLocal.de(applicationContext)
        val mercado = almacen.mercado ?: return Result.success()
        val repo = Repositorio.de(applicationContext)
        if (almacen.avisosTemporada) revisarTemporada(almacen, repo, mercado)
        return revisarBajadas(almacen, repo, mercado)
    }

    /** Una vez al mes: productos que empiezan temporada en el mercado del usuario. */
    private suspend fun revisarTemporada(almacen: AlmacenLocal, repo: Repositorio, mercado: String) {
        val hoy = java.time.LocalDate.now()
        val mes = "%d-%02d".format(hoy.year, hoy.monthValue)
        val anterior = almacen.mesTemporada
        if (anterior == mes) return
        almacen.mesTemporada = mes
        if (anterior.isEmpty()) return // primera vez: avisar desde el próximo cambio de mes
        val temporada = runCatching { repo.descargarTemporada() }.getOrNull() ?: repo.temporadaCache() ?: return
        val productos = repo.mercadoCache(mercado)?.productos ?: return
        val empiezan = temporada.empiezan(productos, mesActual(hoy))
        if (empiezan.isEmpty()) return
        val (titulo, lineas) = textoTemporada(empiezan, temporada)
        AvisosPrecios.notificar(
            applicationContext, titulo, lineas, empiezan.singleOrNull()?.first?.id,
            canal = AvisosPrecios.CANAL_TEMPORADA, id = AvisosPrecios.ID_TEMPORADA,
        )
    }

    private suspend fun revisarBajadas(almacen: AlmacenLocal, repo: Repositorio, mercado: String): Result {
        val seguidos = almacen.seguidos.keys
        if (!almacen.avisos || seguidos.isEmpty()) return Result.success()
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
