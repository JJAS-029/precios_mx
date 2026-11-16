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

/** Primera palabra del nombre -> emoji. Se buscan en orden; gana la primera que coincide. */
private val EMOJIS = listOf(
    "tomate" to "🍅", "aguacate" to "🥑", "cebolla" to "🧅", "papa" to "🥔",
    "limon" to "🍋", "lima" to "🍋", "platano" to "🍌", "manzana" to "🍎", "naranja" to "🍊",
    "mandarina" to "🍊", "toronja" to "🍊", "mango" to "🥭", "pina" to "🍍", "uva" to "🍇",
    "fresa" to "🍓", "sandia" to "🍉", "melon" to "🍈", "pera" to "🍐", "durazno" to "🍑",
    "chabacano" to "🍑", "nectarina" to "🍑", "ciruela" to "🍑", "cereza" to "🍒", "coco" to "🥥",
    "kiwi" to "🥝", "chile" to "🌶️", "elote" to "🌽", "zanahoria" to "🥕", "ajo" to "🧄",
    "brocoli" to "🥦", "coliflor" to "🥦", "lechuga" to "🥬", "col " to "🥬", "espinaca" to "🥬",
    "acelga" to "🥬", "apio" to "🥬", "pepino" to "🥒", "calabac" to "🥒", "chayote" to "🥒",
    "nopal" to "🌵", "cacahuate" to "🥜", "nuez" to "🌰", "camote" to "🍠", "berenjena" to "🍆",
    "jamaica" to "🌺", "champinon" to "🍄", "ejote" to "🫛", "haba" to "🫛", "chicharo" to "🫛",
    "papaya" to "🍈", "guayaba" to "🍐", "granada" to "🍎", "jicama" to "🥔", "betabel" to "🥕",
    "cilantro" to "🌿", "perejil" to "🌿", "epazote" to "🌿", "yerbabuena" to "🌿", "oregano" to "🌿",
)

val Producto.emoji: String
    get() {
        val n = normalizar(nombre) + " "
        return EMOJIS.firstOrNull { n.startsWith(it.first) }?.second
            ?: when (normalizar(categoria)) {
                "frutas", "frutas de temporada" -> "🍏"
                "chiles secos" -> "🌶️"
                else -> "🥬"
            }
    }

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

/** Historial del producto, de la semana más antigua a la más reciente. */
fun Producto.serie(): List<PrecioSemana> = semanas.sortedBy { it.semana }

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

fun Producto.resumen(): Resumen? {
    val s = serie()
    val actual = s.lastOrNull() ?: return null
    val anterior = s.getOrNull(s.size - 2)
    val cambio = anterior?.let { (actual.frec / it.frec - 1) * 100 }
    val ult8 = s.takeLast(8)
    return Resumen(actual, cambio, ult8.sumOf { it.frec } / ult8.size)
}

// ------------------------------------------------------------------ equivalencias / sustitutos

/**
 * Grupos de productos que se pueden usar uno por otro, con el motivo que se muestra.
 * Se comparan contra el nombre normalizado + " " (por eso "papa " no incluye "papaya").
 * Además, las variedades de un mismo producto (misma primera palabra) son sustitutos automáticos.
 */
private val GRUPOS_SUSTITUTOS: List<Pair<String, List<String>>> = listOf(
    "para salsa" to listOf("tomate saladette", "tomate bola", "tomate verde"),
    "para rajas o rellenar" to listOf("chile poblano", "chile chilaca", "chile anaheim"),
    "chile picoso" to listOf("chile serrano", "chile jalapeno", "chile de arbol fresco", "chile caribe", "chile habanero"),
    "chile seco" to listOf("chile guajillo", "chile ancho", "chile pasilla", "chile puya", "chile de arbol seco", "chile mirasol"),
    "pimiento" to listOf("chile pimiento", "chile california", "chile dulce"),
    "para jugo" to listOf("naranja ", "mandarina", "toronja"),
    "limón" to listOf("limon", "lima "),
    "para ensalada" to listOf("lechuga", "col "),
    "brócoli o coliflor" to listOf("brocoli", "coliflor"),
    "papa o camote" to listOf("papa ", "camote"),
    "para guisado" to listOf("calabacita", "chayote"),
    "durazno o nectarina" to listOf("durazno", "nectarina"),
)

/** Primeras palabras que NO agrupan variedades automáticamente (son productos muy distintos). */
private val SIN_GRUPO_AUTOMATICO = setOf("chile", "nuez", "calabaza", "cana")

/** Motivo de cada grupo al que pertenece el producto. "otra variedad" para el grupo automático. */
fun Producto.gruposSustitutos(): Map<String, String> {
    val n = normalizar(nombre) + " "
    val grupos = mutableMapOf<String, String>()
    GRUPOS_SUSTITUTOS.forEach { (motivo, prefijos) ->
        if (prefijos.any { n.startsWith(it) }) grupos[motivo] = motivo
    }
    val primera = n.substringBefore(' ')
    // "Ciruela pasa" o "Uva pasa" no sustituyen a la fruta fresca.
    if (primera !in SIN_GRUPO_AUTOMATICO && " pasa " !in n) grupos["auto:$primera"] = "otra variedad"
    return grupos
}

/** Nombre sin tamaño ni clasificación: "Limón c/semilla # 4" y "# 5" son el mismo producto. */
private val TAMANOS = Regex("""\s*(#\s*\d+|\bsin clasificacion\b|\bgrande\b|\bmedian[ao]\b|\bchic[ao]\b)""")
fun Producto.nombreBase(): String = normalizar(nombre).replace(TAMANOS, "").trim()

data class Alternativa(val producto: Producto, val resumen: Resumen, val motivo: String)

/**
 * Productos del mismo mercado que pueden sustituir a [producto], del más barato
 * (contra su propio promedio) al más caro. Solo con datos de la misma semana o más recientes.
 */
fun alternativas(producto: Producto, todos: List<Producto>, max: Int = 4): List<Alternativa> {
    val propio = producto.resumen() ?: return emptyList()
    val grupos = producto.gruposSustitutos()
    if (grupos.isEmpty()) return emptyList()
    return todos.asSequence()
        .filter { it.nombreBase() != producto.nombreBase() }
        .mapNotNull { otro ->
            val comunes = otro.gruposSustitutos().keys.intersect(grupos.keys)
            val motivo = comunes.firstOrNull { !it.startsWith("auto:") } ?: comunes.firstOrNull() ?: return@mapNotNull null
            val r = otro.resumen()?.takeIf { it.actual.semana >= propio.actual.semana } ?: return@mapNotNull null
            Alternativa(otro, r, grupos.getValue(motivo))
        }
        // Una sola calidad por producto: la más barata.
        .groupBy { it.producto.nombreBase() }.values.map { v -> v.minBy { it.resumen.actual.frec } }
        .sortedBy { it.resumen.vsPromedio }
        .take(max)
}

/** "Está caro, mejor compra…": un producto caro contra su promedio y una alternativa barata. */
data class SugerenciaCambio(val caro: Producto, val resumenCaro: Resumen, val alternativa: Alternativa) {
    val ventaja: Double get() = resumenCaro.vsPromedio - alternativa.resumen.vsPromedio
}

const val UMBRAL_CARO = 15.0
const val UMBRAL_ALTERNATIVA = -5.0

fun sugerenciasCambio(todos: List<Producto>, max: Int = 3): List<SugerenciaCambio> =
    todos.mapNotNull { p ->
        val r = p.resumen()?.takeIf { it.vsPromedio >= UMBRAL_CARO } ?: return@mapNotNull null
        val alt = alternativas(p, todos).firstOrNull()?.takeIf { it.resumen.vsPromedio <= UMBRAL_ALTERNATIVA }
            ?: return@mapNotNull null
        SugerenciaCambio(p, r, alt)
    }
        .sortedByDescending { it.ventaja }
        .distinctBy { it.caro.nombre }
        .take(max)

// ------------------------------------------------------------------ mercados

/** Distancia aproximada en km entre dos puntos (fórmula del haversine). */
fun distanciaKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = kotlin.math.sin(dLat / 2).let { it * it } +
        kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
        kotlin.math.sin(dLon / 2).let { it * it }
    return 2 * r * kotlin.math.asin(kotlin.math.sqrt(a))
}

fun Mercado.distanciaA(otro: Mercado) = distanciaKm(lat, lon, otro.lat, otro.lon)

fun Mercado.coincide(busqueda: String): Boolean {
    val palabras = normalizar(busqueda).split(Regex("\\s+")).filter { it.isNotEmpty() }
    val texto = normalizar("$ciudad $estado $nombre")
    return palabras.all { it in texto }
}

/** Los [n] mercados más cercanos a éste (incluido él mismo), de más cerca a más lejos. */
fun Indice.cercanos(mercado: Mercado, n: Int): List<Mercado> =
    mercados.sortedBy { it.distanciaA(mercado) }.take(n)

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

private val DIAS = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")

/** "jue 13 nov" (agrega el año si no es el actual). Vacío si la fecha no es válida. */
fun fechaCorta(iso: String, hoy: LocalDate = LocalDate.now()): String {
    val f = runCatching { LocalDate.parse(iso.take(10)) }.getOrNull() ?: return ""
    val base = "${DIAS[f.dayOfWeek.value - 1]} ${f.dayOfMonth} ${MESES[f.monthValue - 1]}"
    return if (f.year == hoy.year) base else "$base ${f.year}"
}

/** Días entre una fecha AAAA-MM-DD (o fecha-hora ISO) y hoy; null si no es válida. */
fun diasDesde(iso: String, hoy: LocalDate = LocalDate.now()): Long? =
    runCatching { java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(iso.take(10)), hoy) }.getOrNull()

/** Viernes de una semana ISO, como AAAA-MM-DD (respaldo cuando no hay fecha exacta). */
fun viernesDeSemana(semanaIso: String): String =
    runCatching { lunesDeSemana(semanaIso).plusDays(4).toString() }.getOrDefault("")

/** Fecha del último dato de un mercado: la exacta si existe, si no el viernes de su última semana. */
val Mercado.fechaDatos: String get() = ultimaFecha.ifBlank { viernesDeSemana(ultimaSemana) }
val DatosMercado.fechaDatos: String get() = ultimaFecha.ifBlank { semanas.lastOrNull()?.let(::viernesDeSemana).orEmpty() }

/** Un mercado se considera desactualizado si su último dato tiene más de este número de días. */
const val DIAS_DESACTUALIZADO = 10

fun pesos(valor: Double): String {
    val centavos = (valor * 100).roundToInt()
    return "$" + (centavos / 100) + "." + (centavos % 100).toString().padStart(2, '0')
}

fun porcentaje(valor: Double): String {
    val r = valor.roundToInt()
    return if (r > 0) "+$r%" else "$r%"
}
