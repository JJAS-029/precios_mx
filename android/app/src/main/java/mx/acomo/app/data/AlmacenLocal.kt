package mx.acomo.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate

/** Un precio que el usuario pagó o le ofrecieron, comparado contra el mayoreo de esa semana. */
@Serializable
data class RegistroPrecio(
    val id: Long,
    /** AAAA-MM-DD */
    val fecha: String,
    val productoId: String,
    val producto: String,
    /** Precio por kg que pagó o le ofrecieron. */
    val precio: Double,
    /** Precio frecuente de mayoreo de la semana, en el mercado de referencia. */
    val mayoreo: Double,
    /** id SNIIM del mercado de referencia. */
    val mercado: String,
    /** Dónde compró (texto libre: "Tianguis Santa Cruz", "Mercado Juárez"...). */
    val lugar: String = "",
    val semaforo: Semaforo,
) {
    /** Cuántas veces el precio de mayoreo. */
    val factor: Double get() = precio / mayoreo
}

/** Preferencias y registros guardados en el teléfono. */
class AlmacenLocal(
    private val archivo: File,
    private val prefs: SharedPreferences,
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    var mercado: String?
        get() = prefs.getString(CLAVE_MERCADO, null)
        set(v) = prefs.edit().putString(CLAVE_MERCADO, v).apply()

    var ultimoLugar: String
        get() = prefs.getString(CLAVE_LUGAR, "").orEmpty()
        set(v) = prefs.edit().putString(CLAVE_LUGAR, v).apply()

    /** Nombre del EstiloColor elegido. */
    var estiloColor: String?
        get() = prefs.getString(CLAVE_ESTILO, null)
        set(v) = prefs.edit().putString(CLAVE_ESTILO, v).apply()

    /** Nombre del ModoTema elegido. */
    var modoTema: String?
        get() = prefs.getString(CLAVE_MODO, null)
        set(v) = prefs.edit().putString(CLAVE_MODO, v).apply()

    /**
     * ¿A cómo? Plus. Por ahora solo se activa en compilaciones de prueba; cuando exista
     * el producto en Google Play, este valor lo confirmará Play Billing al abrir la app.
     */
    var plus: Boolean
        get() = prefs.getBoolean(CLAVE_PLUS, false)
        set(v) = prefs.edit().putBoolean(CLAVE_PLUS, v).apply()

    /** Lista del mandado (pocos elementos, cabe en las preferencias). */
    var mandado: List<ArticuloMandado>
        get() = runCatching {
            json.decodeFromString<List<ArticuloMandado>>(prefs.getString(CLAVE_MANDADO, null) ?: "[]")
        }.getOrDefault(emptyList())
        set(v) = prefs.edit().putString(CLAVE_MANDADO, json.encodeToString(v)).apply()

    /** Productos con aviso de bajada: id -> nombre (el id es el mismo en todos los mercados). */
    var seguidos: Map<String, String>
        get() = runCatching {
            json.decodeFromString<Map<String, String>>(prefs.getString(CLAVE_SEGUIDOS, null) ?: "{}")
        }.getOrDefault(emptyMap())
        set(v) = prefs.edit().putString(CLAVE_SEGUIDOS, json.encodeToString(v)).apply()

    /** Productos favoritos: id -> nombre (el id es el mismo en todos los mercados). */
    var favoritos: Map<String, String>
        get() = runCatching {
            json.decodeFromString<Map<String, String>>(prefs.getString(CLAVE_FAVORITOS, null) ?: "{}")
        }.getOrDefault(emptyMap())
        set(v) = prefs.edit().putString(CLAVE_FAVORITOS, json.encodeToString(v)).apply()

    /** Gasolineras favoritas: place_id de la CNE (como texto) -> nombre. */
    var gasolinerasFavoritas: Map<String, String>
        get() = runCatching {
            json.decodeFromString<Map<String, String>>(prefs.getString(CLAVE_GASOLINERAS, null) ?: "{}")
        }.getOrDefault(emptyMap())
        set(v) = prefs.edit().putString(CLAVE_GASOLINERAS, json.encodeToString(v)).apply()

    /** Interruptor general de avisos (Ajustes). */
    var avisos: Boolean
        get() = prefs.getBoolean(CLAVE_AVISOS, true)
        set(v) = prefs.edit().putBoolean(CLAVE_AVISOS, v).apply()

    /** Aviso mensual "Empieza la temporada de …". */
    var avisosTemporada: Boolean
        get() = prefs.getBoolean(CLAVE_AVISOS_TEMPORADA, true)
        set(v) = prefs.edit().putBoolean(CLAVE_AVISOS_TEMPORADA, v).apply()

    /** Último mes ("AAAA-MM") ya revisado para el aviso de temporada. */
    var mesTemporada: String
        get() = prefs.getString(CLAVE_MES_TEMPORADA, "").orEmpty()
        set(v) = prefs.edit().putString(CLAVE_MES_TEMPORADA, v).apply()

    /** Última semana ISO ya revisada para avisos, para no avisar dos veces de la misma. */
    var semanaAvisada: String
        get() = prefs.getString(CLAVE_SEMANA_AVISADA, "").orEmpty()
        set(v) = prefs.edit().putString(CLAVE_SEMANA_AVISADA, v).apply()

    /** Listas del mandado guardadas con nombre. */
    var listas: List<ListaGuardada>
        get() = runCatching {
            json.decodeFromString<List<ListaGuardada>>(prefs.getString(CLAVE_LISTAS, null) ?: "[]")
        }.getOrDefault(emptyList())
        set(v) = prefs.edit().putString(CLAVE_LISTAS, json.encodeToString(v)).apply()

    suspend fun registros(): List<RegistroPrecio> = withContext(Dispatchers.IO) {
        runCatching { json.decodeFromString<List<RegistroPrecio>>(archivo.readText()) }.getOrDefault(emptyList())
    }

    suspend fun guardar(registros: List<RegistroPrecio>) = withContext(Dispatchers.IO) {
        val tmp = File(archivo.path + ".tmp")
        tmp.writeText(json.encodeToString(registros))
        tmp.renameTo(archivo)
    }

    companion object {
        private const val CLAVE_MERCADO = "mercado"
        private const val CLAVE_LUGAR = "ultimo_lugar"
        private const val CLAVE_ESTILO = "estilo_color"
        private const val CLAVE_MODO = "modo_tema"
        private const val CLAVE_PLUS = "plus"
        private const val CLAVE_MANDADO = "mandado"
        private const val CLAVE_LISTAS = "listas"
        private const val CLAVE_SEGUIDOS = "seguidos"
        private const val CLAVE_FAVORITOS = "favoritos"
        private const val CLAVE_GASOLINERAS = "gasolineras_favoritas"
        private const val CLAVE_AVISOS = "avisos"
        private const val CLAVE_AVISOS_TEMPORADA = "avisos_temporada"
        private const val CLAVE_MES_TEMPORADA = "mes_temporada"
        private const val CLAVE_SEMANA_AVISADA = "semana_avisada"

        fun de(contexto: Context) = AlmacenLocal(
            File(contexto.filesDir, "mis_precios.json"),
            contexto.getSharedPreferences("preferencias", Context.MODE_PRIVATE),
        )
    }
}

fun hoy(): String = LocalDate.now().toString()

/** Texto CSV para compartir los registros (por WhatsApp, correo, etc.). */
fun registrosCsv(registros: List<RegistroPrecio>, mercados: Map<String, String>): String = buildString {
    appendLine("fecha,producto,precio_kg,lugar,mayoreo_kg,mercado_referencia,veces_mayoreo")
    registros.sortedBy { it.fecha }.forEach { r ->
        val lugar = r.lugar.replace("\"", "'")
        val mercado = mercados[r.mercado] ?: r.mercado
        appendLine(
            "${r.fecha},\"${r.producto}\",${"%.2f".format(java.util.Locale.US, r.precio)},\"$lugar\"," +
                "${"%.2f".format(java.util.Locale.US, r.mayoreo)},\"$mercado\",${"%.2f".format(java.util.Locale.US, r.factor)}",
        )
    }
}
