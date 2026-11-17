package mx.acomo.app.data

import kotlinx.serialization.Serializable
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * data/temporada.json (recolector/temporada.py): por producto, 12 índices (enero..diciembre).
 * 1.0 = precio normal del año; 0.85 = ese mes suele estar 15 % abajo. null = pocos datos ese mes.
 */
@Serializable
data class Temporada(
    val version: Int = 1,
    val desde: String = "",
    val mercados: List<String> = emptyList(),
    val productos: Map<String, List<Double?>> = emptyMap(),
)

/** Un mes "de temporada" si suele estar al menos 8 % abajo de lo normal. */
const val UMBRAL_TEMPORADA = 0.92

val MESES_LARGOS = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)
private val MESES_CORTOS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/** Mes actual, 0 = enero. */
fun mesActual(hoy: LocalDate = LocalDate.now()): Int = hoy.monthValue - 1

fun Temporada.indice(productoId: String): List<Double?>? = productos[productoId]?.takeIf { it.size == 12 }

/** Meses (0..11) en que el producto suele estar barato. */
fun mesesBaratos(indice: List<Double?>): List<Int> = indice.indices.filter { (indice[it] ?: 1.0) <= UMBRAL_TEMPORADA }

/**
 * Agrupa meses consecutivos (también de diciembre a enero): [0, 1, 2, 11] -> "dic–mar";
 * [3, 7] -> "abr y ago".
 */
fun rangoMeses(meses: List<Int>): String {
    if (meses.isEmpty()) return ""
    if (meses.size == 12) return "todo el año"
    val conjunto = meses.toSet()
    // Se recorre el año empezando en un mes cuyo anterior no esté, para no partir un tramo que cruza el año.
    val inicio = (0 until 12).first { it in conjunto && (it + 11) % 12 !in conjunto }
    val tramos = mutableListOf<Pair<Int, Int>>()
    var abierto: Int? = null
    var ultimo = inicio
    for (k in 0 until 12) {
        val m = (inicio + k) % 12
        if (m in conjunto) {
            if (abierto == null) abierto = m
            ultimo = m
        } else if (abierto != null) {
            tramos += abierto to ultimo
            abierto = null
        }
    }
    abierto?.let { tramos += it to ultimo }
    val textos = tramos.map { (a, b) -> if (a == b) MESES_CORTOS[a] else "${MESES_CORTOS[a]}–${MESES_CORTOS[b]}" }
    return if (textos.size == 1) textos[0] else textos.dropLast(1).joinToString(", ") + " y " + textos.last()
}

/** Productos del mercado que están en temporada este mes, de los que más bajan a los que menos, uno por producto. */
fun Temporada.deTemporada(productos: List<Producto>, mes: Int = mesActual(), max: Int = 8): List<Pair<Producto, Double>> =
    productos.asSequence()
        .filter { it.resumen() != null }
        .mapNotNull { p -> indice(p.id)?.get(mes)?.takeIf { it <= UMBRAL_TEMPORADA }?.let { p to it } }
        // Una tarjeta por producto (plátano Chiapas y Tabasco cuentan como "plátano"): la variedad que más baja.
        .groupBy { normalizar(it.first.nombre).substringBefore(' ') }.values.map { v -> v.minBy { it.second } }
        .sortedBy { it.second }
        .take(max)
        .toList()

/**
 * Productos del mercado que EMPIEZAN temporada en [mes]: ese mes suelen estar baratos y el anterior no.
 * Una entrada por producto (la variedad que más baja).
 */
fun Temporada.empiezan(productos: List<Producto>, mes: Int = mesActual()): List<Pair<Producto, Double>> {
    val anterior = (mes + 11) % 12
    return productos.asSequence()
        .filter { it.resumen() != null }
        .mapNotNull { p ->
            val i = indice(p.id) ?: return@mapNotNull null
            val ahora = i[mes] ?: return@mapNotNull null
            if (ahora <= UMBRAL_TEMPORADA && (i[anterior] ?: 1.0) > UMBRAL_TEMPORADA) p to ahora else null
        }
        .groupBy { normalizar(it.first.nombre).substringBefore(' ') }.values.map { v -> v.minBy { it.second } }
        .sortedBy { it.second }
        .toList()
}

/** Título y líneas del aviso de temporada (máximo 3 productos). */
fun textoTemporada(empiezan: List<Pair<Producto, Double>>, temporada: Temporada): Pair<String, List<String>> {
    val tres = empiezan.take(3)
    // Primera palabra del nombre como se dice ("plátano", "jitomate"), sin la variedad.
    val nombres = tres.map { it.first.nombreVisible.lowercase().substringBefore(' ') }
    val titulo = "Empieza la temporada de " + when (nombres.size) {
        1 -> nombres[0]
        else -> nombres.dropLast(1).joinToString(", ") + " y " + nombres.last()
    }
    val lineas = tres.map { (p, indice) ->
        val baja = kotlin.math.abs(((indice - 1) * 100).roundToInt())
        val meses = temporada.indice(p.id)?.let { rangoMeses(mesesBaratos(it)) }.orEmpty()
        "${p.nombreVisible}: suele bajar $baja% (temporada: $meses)"
    }
    return titulo to lineas
}
