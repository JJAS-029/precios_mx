package mx.acomo.app.data

import kotlinx.serialization.Serializable

/** data/gasolina.json (recolector/gasolina.py): precio vigente de cada gasolinera, de la CNE (antes CRE). */
@Serializable
data class DatosGasolina(
    val version: Int = 1,
    val generado: String = "",
    val fuente: String = "",
    val estaciones: List<Estacion> = emptyList(),
)

/** Claves cortas para que el archivo pese poco: a = latitud, o = longitud, r/p/d = regular/premium/diésel. */
@Serializable
data class Estacion(
    val i: Int,
    val a: Double,
    val o: Double,
    val n: String,
    val r: Double? = null,
    val p: Double? = null,
    val d: Double? = null,
)

enum class Combustible(val nombre: String) {
    MAGNA("Magna"), PREMIUM("Premium"), DIESEL("Diésel");

    fun precio(e: Estacion): Double? = when (this) {
        MAGNA -> e.r
        PREMIUM -> e.p
        DIESEL -> e.d
    }
}

data class GasolineraCercana(val estacion: Estacion, val precio: Double, val km: Double)

/** Gasolineras con precio de [tipo] a menos de [radioKm], de la más barata a la más cara (y la más cercana si empatan). */
fun DatosGasolina.cercanas(lat: Double, lon: Double, radioKm: Double, tipo: Combustible): List<GasolineraCercana> {
    // Filtro rápido por caja antes de calcular distancias (1° de latitud ≈ 111 km).
    val dLat = radioKm / 111.0
    val dLon = radioKm / (111.0 * kotlin.math.cos(Math.toRadians(lat)).coerceAtLeast(0.2))
    return estaciones.asSequence()
        .filter { kotlin.math.abs(it.a - lat) <= dLat && kotlin.math.abs(it.o - lon) <= dLon }
        .mapNotNull { e ->
            val precio = tipo.precio(e) ?: return@mapNotNull null
            val km = distanciaKm(lat, lon, e.a, e.o)
            if (km <= radioKm) GasolineraCercana(e, precio, km) else null
        }
        .sortedWith(compareBy({ it.precio }, { it.km }))
        .toList()
}

/** Litros de un tanque típico, para decir cuánto se ahorra. */
const val LITROS_TANQUE = 40.0

data class ResumenGasolina(
    val masBarata: GasolineraCercana,
    val masCara: GasolineraCercana,
    val promedio: Double,
) {
    /** Diferencia en un tanque de [LITROS_TANQUE] entre la más cara y la más barata. */
    val ahorroTanque: Double get() = (masCara.precio - masBarata.precio) * LITROS_TANQUE
}

fun resumenGasolina(lista: List<GasolineraCercana>): ResumenGasolina? {
    if (lista.isEmpty()) return null
    return ResumenGasolina(lista.first(), lista.maxBy { it.precio }, lista.sumOf { it.precio } / lista.size)
}

/** "350 m", "2.3 km", "12 km". */
fun distancia(km: Double): String = when {
    km < 1 -> "${(km * 1000 / 50).toInt() * 50} m"
    km < 10 -> "${(km * 10).toInt() / 10.0} km"
    else -> "${km.toInt()} km"
}
