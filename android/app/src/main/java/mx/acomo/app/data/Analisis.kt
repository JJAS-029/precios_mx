package mx.acomo.app.data

import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

// ------------------------------------------------------------------ nombres y búsqueda

/** Nombres como se dicen en el centro del país (el SNIIM usa nombres comerciales). */
private val NOMBRES_COMUNES = mapOf(
    "Tomate Saladette" to "Jitomate saladette",
    "Tomate Bola" to "Jitomate bola",
)

/** Palabras extra con las que también se puede encontrar un producto. */
private val SINONIMOS = mapOf(
    "Tomate Verde" to "tomatillo miltomate",
    "Chile Poblano" to "chile para rellenar",
    "Elote" to "maiz",
    "Ejote" to "judia",
    "Nopal" to "nopales",
)

val Producto.nombreVisible: String
    get() = NOMBRES_COMUNES[nombre] ?: nombre

fun normalizar(texto: String): String =
    Normalizer.normalize(texto, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase()
        .trim()

/** Todas las palabras de la búsqueda deben aparecer en el nombre, nombre común o sinónimos. */
fun Producto.coincide(busqueda: String): Boolean {
    val palabras = normalizar(busqueda).split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (palabras.isEmpty()) return true
    val texto = normalizar("$nombre $nombreVisible $calidad ${SINONIMOS[nombre].orEmpty()}")
    return palabras.all { it in texto }
}

// ------------------------------------------------------------------ series de precios

/** Historial del producto en un mercado, de la semana más antigua a la más reciente. */
fun Producto.serie(mercado: String): List<PrecioSemana> =
    semanas.filter { it.mercado == mercado }.sortedBy { it.semana }

data class Resumen(
    val actual: PrecioSemana,
    /** Variación % contra la semana anterior con datos (null si no hay). */
    val cambioSemanal: Double?,
    /** Promedio del precio frecuente en las últimas 8 semanas (incluida la actual). */
    val promedio8: Double,
) {
    /** Qué tan por debajo (-) o por encima (+) del promedio de 8 semanas está hoy, en %. */
    val vsPromedio: Double get() = (actual.frec / promedio8 - 1) * 100
}

fun Producto.resumen(mercado: String): Resumen? {
    val s = serie(mercado)
    val actual = s.lastOrNull() ?: return null
    val anterior = s.getOrNull(s.size - 2)
    val cambio = anterior?.let { (actual.frec / it.frec - 1) * 100 }
    val ult8 = s.takeLast(8)
    return Resumen(actual, cambio, ult8.sumOf { it.frec } / ult8.size)
}

// ------------------------------------------------------------------ semáforo

enum class Semaforo { VERDE, AMARILLO, ROJO }

/**
 * Factor aproximado de mayoreo -> menudeo (mercado, tianguis, recaudería).
 * Valores iniciales por calibrar con datos reales de menudeo.
 */
fun factorMenudeo(categoria: String): Double = when (normalizar(categoria)) {
    "chiles secos" -> 1.4
    else -> 1.5
}

/** Margen adicional sobre la referencia antes de pasar de amarillo a rojo. */
const val TOLERANCIA_AMARILLO = 1.25

data class Evaluacion(
    val semaforo: Semaforo,
    /** Precio de menudeo razonable estimado (mayoreo × factor). */
    val referencia: Double,
    /** Diferencia del precio ofrecido contra la referencia, en %. */
    val diferencia: Double,
)

fun evaluarPrecio(precioOfrecido: Double, mayoreoFrec: Double, categoria: String): Evaluacion {
    val referencia = mayoreoFrec * factorMenudeo(categoria)
    val semaforo = when {
        precioOfrecido <= referencia -> Semaforo.VERDE
        precioOfrecido <= referencia * TOLERANCIA_AMARILLO -> Semaforo.AMARILLO
        else -> Semaforo.ROJO
    }
    return Evaluacion(semaforo, referencia, (precioOfrecido / referencia - 1) * 100)
}

// ------------------------------------------------------------------ formato

private val MESES = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/** Lunes de una semana ISO "2026-W46". */
fun lunesDeSemana(semanaIso: String): LocalDate {
    val (anio, sem) = semanaIso.split("-W").map { it.toInt() }
    return LocalDate.of(anio, 6, 1)
        .with(IsoFields.WEEK_BASED_YEAR, anio.toLong())
        .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, sem.toLong())
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}

/** "9–13 nov" (lunes a viernes de la semana). */
fun etiquetaSemana(semanaIso: String): String {
    val lunes = lunesDeSemana(semanaIso)
    val viernes = lunes.plusDays(4)
    return if (lunes.month == viernes.month) {
        "${lunes.dayOfMonth}–${viernes.dayOfMonth} ${MESES[viernes.monthValue - 1]}"
    } else {
        "${lunes.dayOfMonth} ${MESES[lunes.monthValue - 1]}–${viernes.dayOfMonth} ${MESES[viernes.monthValue - 1]}"
    }
}

/** "13 nov" (fecha corta del lunes, para ejes de gráficas). */
fun etiquetaCorta(semanaIso: String): String {
    val lunes = lunesDeSemana(semanaIso)
    return "${lunes.dayOfMonth} ${MESES[lunes.monthValue - 1]}"
}

fun pesos(valor: Double): String {
    val centavos = (valor * 100).roundToInt()
    return "$" + (centavos / 100) + "." + (centavos % 100).toString().padStart(2, '0')
}

fun porcentaje(valor: Double): String {
    val r = valor.roundToInt()
    return if (r > 0) "+$r%" else "$r%"
}
