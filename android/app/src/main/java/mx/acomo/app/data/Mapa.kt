package mx.acomo.app.data

import kotlinx.serialization.Serializable
import kotlin.math.abs

/** assets/estados.json, generado por diseno/generar_mapa.py (Natural Earth, dominio público). */
@Serializable
data class MapaEstados(val ancho: Float, val alto: Float, val estados: List<EstadoMapa>)

/** [anillos]: polígonos como x0, y0, x1, y1… en coordenadas normalizadas (x de 0 a 1, y hacia abajo). */
@Serializable
data class EstadoMapa(val nombre: String, val anillos: List<List<Float>>)

/** Precio de un producto en un estado: promedio de sus centrales de abasto. */
data class PrecioEstado(val estado: String, val precio: Double, val mercados: List<Mercado>)

/** Cinco niveles contra el precio típico del país (la mediana de todos los mercados). */
enum class NivelPrecio(val texto: String) {
    MUY_BARATO("Mucho más barato"),
    BARATO("Más barato"),
    NORMAL("Como en el país"),
    CARO("Más caro"),
    MUY_CARO("Mucho más caro"),
    ;

    companion object {
        /** [diferencia] en %: hasta ±5 % es normal; más de 20 % es "mucho". */
        fun de(diferencia: Double): NivelPrecio = when {
            diferencia <= -20 -> MUY_BARATO
            diferencia < -5 -> BARATO
            diferencia <= 5 -> NORMAL
            diferencia < 20 -> CARO
            else -> MUY_CARO
        }
    }
}

data class PreciosPais(
    /** Mediana de los precios de todos los mercados con dato. */
    val tipico: Double,
    val estados: Map<String, PrecioEstado>,
) {
    fun diferencia(e: PrecioEstado): Double = (e.precio / tipico - 1) * 100
    fun nivel(e: PrecioEstado): NivelPrecio = NivelPrecio.de(diferencia(e))
    val masBarato: PrecioEstado? get() = estados.values.minByOrNull { it.precio }
    val masCaro: PrecioEstado? get() = estados.values.maxByOrNull { it.precio }
}

/** null si hay menos de 3 mercados con precio (un mapa así no dice nada). */
fun Indice.preciosPorEstado(productoId: String): PreciosPais? {
    val ultimos = ultimos[productoId].orEmpty()
    val conPrecio = mercados.mapNotNull { m -> ultimos[m.id]?.let { m to it } }
    if (conPrecio.size < 3) return null
    val ordenados = conPrecio.map { it.second }.sorted()
    val n = ordenados.size
    val mediana = if (n % 2 == 1) ordenados[n / 2] else (ordenados[n / 2 - 1] + ordenados[n / 2]) / 2
    val estados = conPrecio.groupBy { it.first.estado }.mapValues { (estado, lista) ->
        PrecioEstado(estado, lista.sumOf { it.second } / lista.size, lista.map { it.first })
    }
    return PreciosPais(mediana, estados)
}

/** Estado que contiene el punto (coordenadas normalizadas), o el más cercano a 0.02 si se tocó en el borde. */
fun MapaEstados.estadoEn(x: Float, y: Float): String? =
    estados.firstOrNull { e -> e.anillos.any { contiene(it, x, y) } }?.nombre
        ?: estados.minByOrNull { e -> e.anillos.minOf { distanciaVertice(it, x, y) } }
            ?.takeIf { e -> e.anillos.minOf { distanciaVertice(it, x, y) } < 0.02f }?.nombre

/** Prueba de rayo (par-impar). */
private fun contiene(a: List<Float>, x: Float, y: Float): Boolean {
    var dentro = false
    val n = a.size / 2
    var j = n - 1
    for (i in 0 until n) {
        val xi = a[2 * i]; val yi = a[2 * i + 1]
        val xj = a[2 * j]; val yj = a[2 * j + 1]
        if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) dentro = !dentro
        j = i
    }
    return dentro
}

private fun distanciaVertice(a: List<Float>, x: Float, y: Float): Float {
    var min = Float.MAX_VALUE
    for (i in a.indices step 2) min = minOf(min, abs(a[i] - x) + abs(a[i + 1] - y))
    return min
}
