package mx.acomo.app.data

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/** Un producto de la lista del mandado. El id es el mismo en todos los mercados. */
@Serializable
data class ArticuloMandado(
    val productoId: String,
    /** Nombre guardado por si el mercado elegido no tiene el producto. */
    val nombre: String,
    val kg: Double,
)

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
        appendLine("• ${cantidad(l.articulo.kg)} ${l.producto?.nombreVisible ?: l.articulo.nombre}$precio")
    }
    appendLine()
    appendLine("Total aproximado: ${pesosRedondo(p.totalEstimado)}")
    append("(Estimado con precios de mayoreo de $mercado, app ¿A cómo?)")
}
