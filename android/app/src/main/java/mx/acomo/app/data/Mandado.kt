package mx.acomo.app.data

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/** Un producto de la lista del mandado. El id es el mismo en todos los mercados. */
@Serializable
data class ArticuloMandado(
    val productoId: String,
    /** Nombre guardado por si el mercado elegido no tiene el producto. */
    val nombre: String,
    /** Siempre en kg (si va por piezas, piezas × peso promedio): con esto se calcula el total. */
    val kg: Double,
    /** Si se compra por pieza: cuántas. null = por kilo. */
    val piezas: Int? = null,
)

/** Una lista del mandado guardada con nombre ("Mandado de la semana"). */
@Serializable
data class ListaGuardada(val nombre: String, val articulos: List<ArticuloMandado>)

/**
 * Peso promedio aproximado por pieza, en kg, de lo que se suele comprar por pieza.
 * Se busca por la primera palabra del nombre (sin acentos).
 */
private val PESO_PIEZA = mapOf(
    "aguacate" to 0.2, "pina" to 1.5, "lechuga" to 0.5, "coliflor" to 0.8, "brocoli" to 0.5,
    "melon" to 1.5, "sandia" to 6.0, "papaya" to 1.8, "mango" to 0.3, "elote" to 0.3,
    "coco" to 1.0, "col" to 1.2, "apio" to 0.5,
)

fun pesoPieza(nombre: String): Double? = PESO_PIEZA[normalizar(nombre).substringBefore(' ')]

/** Artículo por piezas, con su kg calculado. */
fun articuloPorPiezas(productoId: String, nombre: String, piezas: Int, peso: Double) =
    ArticuloMandado(productoId, nombre, piezas * peso, piezas)

data class LineaMandado(
    val articulo: ArticuloMandado,
    /** null si el mercado elegido no tiene precio de este producto. */
    val producto: Producto?,
    /** Precio frecuente de mayoreo por kg. */
    val mayoreoKg: Double?,
) {
    val subtotalMayoreo: Double? get() = mayoreoKg?.let { it * articulo.kg }

    /** Estimado en mercado o tianguis (mayoreo × factor de menudeo). */
    val subtotalEstimado: Double?
        get() = producto?.let { p -> subtotalMayoreo?.let { it * factorMenudeo(p.categoria) } }
}

data class Presupuesto(val lineas: List<LineaMandado>) {
    val totalEstimado: Double get() = lineas.sumOf { it.subtotalEstimado ?: 0.0 }
    val totalMayoreo: Double get() = lineas.sumOf { it.subtotalMayoreo ?: 0.0 }
    val sinPrecio: Int get() = lineas.count { it.mayoreoKg == null }
}

fun presupuesto(articulos: List<ArticuloMandado>, productos: List<Producto>): Presupuesto {
    val porId = productos.associateBy { it.id }
    return Presupuesto(
        articulos.map { a ->
            val p = porId[a.productoId]
            LineaMandado(a, p, p?.resumen()?.actual?.frec)
        },
    )
}

/** Pasos del selector de cantidad: de 250 g en 250 g hasta 2 kg, luego de medio en medio. */
fun masKg(kg: Double): Double = if (kg < 2.0) kg + 0.25 else kg + 0.5
fun menosKg(kg: Double): Double = if (kg <= 2.0) kg - 0.25 else kg - 0.5

/** "3 piezas (≈ 600 g)" o "1.5 kg". */
fun cantidad(a: ArticuloMandado): String =
    a.piezas?.let { "$it ${if (it == 1) "pieza" else "piezas"}" } ?: cantidad(a.kg)

/** "250 g", "1 kg", "1.5 kg". */
fun cantidad(kg: Double): String = when {
    kg < 1.0 -> "${(kg * 1000).roundToInt()} g"
    kg % 1.0 == 0.0 -> "${kg.toInt()} kg"
    else -> "${(kg * 100).roundToInt() / 100.0} kg"
}

/** Lista en texto para mandar por WhatsApp. */
fun mandadoTexto(p: Presupuesto, mercado: String): String = buildString {
    appendLine("🛒 Mi mandado")
    p.lineas.forEach { l ->
        val precio = l.subtotalEstimado?.let { " ≈ ${pesosRedondo(it)}" }.orEmpty()
        val aprox = if (l.articulo.piezas != null) " (≈ ${cantidad(l.articulo.kg)})" else ""
        appendLine("• ${cantidad(l.articulo)} ${l.producto?.nombreVisible ?: l.articulo.nombre}$aprox$precio")
    }
    appendLine()
    appendLine("Total aproximado: ${pesosRedondo(p.totalEstimado)}")
    append("(Estimado con precios de mayoreo de $mercado, app ¿A cómo?)")
}
