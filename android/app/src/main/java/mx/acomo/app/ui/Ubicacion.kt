package mx.acomo.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

fun tienePermisoUbicacion(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Ubicación aproximada del teléfono (permiso ACCESS_COARSE_LOCATION).
 * Solo se usa en el teléfono para ordenar mercados por cercanía; no se envía a ningún lado.
 */
@SuppressLint("MissingPermission")
suspend fun obtenerUbicacion(context: Context): Location? {
    if (!tienePermisoUbicacion(context)) return null
    val lm = context.getSystemService(LocationManager::class.java) ?: return null
    val activos = runCatching { lm.getProviders(true) }.getOrDefault(emptyList())

    val ultima = activos
        .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }
    if (ultima != null && System.currentTimeMillis() - ultima.time < 30 * 60_000) return ultima

    val proveedor = listOf(LocationManager.NETWORK_PROVIDER, "fused", LocationManager.GPS_PROVIDER)
        .firstOrNull { it in activos } ?: return ultima

    return withTimeoutOrNull(15_000) {
        suspendCancellableCoroutine { cont ->
            val senal = CancellationSignal()
            cont.invokeOnCancellation { senal.cancel() }
            LocationManagerCompat.getCurrentLocation(
                lm, proveedor, senal, ContextCompat.getMainExecutor(context),
            ) { loc -> if (cont.isActive) cont.resume(loc ?: ultima) }
        }
    } ?: ultima
}
