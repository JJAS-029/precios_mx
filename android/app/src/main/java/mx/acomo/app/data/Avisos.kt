package mx.acomo.app.data

import kotlin.math.min

/** Cuánto tiene que bajar un producto seguido (contra la semana pasada o su promedio) para avisar. */
const val UMBRAL_BAJADA = -10.0

/**
 * Si está bajo su promedio, además tiene que haber bajado al menos esto en la semana; así un
 * producto que se queda barato varias semanas no avisa cada semana.
 */
const val BAJA_MINIMA_SEMANAL = -3.0

/** Un producto seguido que bajó de precio en la semana más reciente. */
data class Bajada(val producto: Producto, val resumen: Resumen) {
    /** La baja más notoria de las dos comparaciones. */
    val cambio: Double get() = min(resumen.cambioSemanal ?: 0.0, resumen.vsPromedio)

    val avisar: Boolean
        get() {
            val semanal = resumen.cambioSemanal ?: return false
            return semanal <= UMBRAL_BAJADA || (resumen.vsPromedio <= UMBRAL_BAJADA && semanal <= BAJA_MINIMA_SEMANAL)
        }

    /** "-15% vs. la semana pasada" o "-12% vs. su promedio". */
    val motivo: String
        get() = if ((resumen.cambioSemanal ?: 0.0) <= resumen.vsPromedio) {
            "${porcentaje(resumen.cambioSemanal ?: 0.0)} vs. la semana pasada"
        } else {
            "${porcentaje(resumen.vsPromedio)} vs. su promedio"
        }
}

/**
 * Productos seguidos que bajaron en [semana] (la más reciente del mercado).
 * Se ignoran los que no tienen dato esa semana, para no avisar de precios viejos.
 */
fun bajadas(seguidos: Set<String>, productos: List<Producto>, semana: String): List<Bajada> =
    productos.asSequence()
        .filter { it.id in seguidos }
        .mapNotNull { p ->
            p.resumen()
                ?.takeIf { it.actual.semana == semana }
                ?.let { Bajada(p, it) }
                ?.takeIf { it.avisar }
        }
        .sortedBy { it.cambio }
        .toList()

/** Título y líneas de la notificación. */
fun textoAviso(b: List<Bajada>, ciudad: String): Pair<String, List<String>> {
    val lineas = b.map { "${it.producto.nombreVisible}: ${pesos(it.resumen.actual.frec)}/kg (${it.motivo})" }
    val donde = if (ciudad.isBlank()) "" else " en $ciudad"
    val titulo = if (b.size == 1) {
        "Bajó ${b[0].producto.nombreVisible.lowercase()}$donde"
    } else {
        "Bajaron ${b.size} de tus productos$donde"
    }
    return titulo to lineas
}
