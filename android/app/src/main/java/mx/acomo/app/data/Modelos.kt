package mx.acomo.app.data

import kotlinx.serialization.Serializable

/** data/indice.json: mercados disponibles, catálogo y último precio por producto y mercado. */
@Serializable
data class Indice(
    val version: Int,
    val generado: String,
    val fuente: String = "",
    val unidad: String = "MXN/kg",
    val ultimaSemana: String = "",
    val mercados: List<Mercado>,
    val productos: List<InfoProducto> = emptyList(),
    /** id de producto -> (id de mercado -> precio frecuente de su última semana). */
    val ultimos: Map<String, Map<String, Double>> = emptyMap(),
)

@Serializable
data class Mercado(
    val id: String,
    /** "Central de Abasto", "Mercado de Abasto Estrella"... */
    val nombre: String,
    val ciudad: String,
    val estado: String,
    val lat: Double,
    val lon: Double,
    val ultimaSemana: String = "",
    /** Fecha (AAAA-MM-DD) del dato más reciente que publicó el SNIIM para este mercado. */
    val ultimaFecha: String = "",
    val productos: Int = 0,
)

@Serializable
data class InfoProducto(
    val id: String,
    val nombre: String,
    val calidad: String,
    val categoria: String,
)

/** data/mercados/<id>.json: historial semanal de un mercado. */
@Serializable
data class DatosMercado(
    val version: Int,
    val mercado: String,
    val generado: String,
    /** Fecha (AAAA-MM-DD) del dato más reciente que publicó el SNIIM. */
    val ultimaFecha: String = "",
    val unidad: String = "MXN/kg",
    /** Semanas ISO ordenadas, p. ej. "2026-W46". */
    val semanas: List<String>,
    val productos: List<Producto>,
)

@Serializable
data class Producto(
    val id: String,
    val nombre: String,
    val calidad: String,
    val categoria: String,
    val semanas: List<PrecioSemana>,
)

@Serializable
data class PrecioSemana(
    val semana: String,
    val min: Double? = null,
    val max: Double? = null,
    /** Precio más frecuente (mediana de la semana), en pesos por kg. */
    val frec: Double,
    val dias: Int = 0,
)
