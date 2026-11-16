package mx.preciojusto.app.data

import kotlinx.serialization.Serializable

/** Estructura de data/precios.json generado por recolector/sniim.py. */
@Serializable
data class DatosPrecios(
    val version: Int,
    val generado: String,
    val fuente: String = "",
    val unidad: String = "MXN/kg",
    /** id de mercado SNIIM -> nombre. */
    val mercados: Map<String, String>,
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
    val mercado: String,
    val min: Double? = null,
    val max: Double? = null,
    /** Precio más frecuente (mediana de la semana), en pesos por kg. */
    val frec: Double,
    val dias: Int = 0,
)
