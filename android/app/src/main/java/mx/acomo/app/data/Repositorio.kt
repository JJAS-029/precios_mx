package mx.acomo.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Descarga precios.json y guarda una copia local para que la app funcione
 * sin internet (en el mercado suele haber mala señal).
 */
class Repositorio(
    private val url: String,
    private val archivoCache: File,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun leerCache(): DatosPrecios? = withContext(Dispatchers.IO) {
        runCatching { json.decodeFromString<DatosPrecios>(archivoCache.readText()) }.getOrNull()
    }

    suspend fun descargar(): DatosPrecios = withContext(Dispatchers.IO) {
        val conexion = URL(url).openConnection() as HttpURLConnection
        try {
            conexion.connectTimeout = 15_000
            conexion.readTimeout = 30_000
            conexion.setRequestProperty("Accept-Encoding", "gzip")
            val codigo = conexion.responseCode
            check(codigo == 200) { "El servidor respondió $codigo" }
            val flujo = if (conexion.contentEncoding == "gzip") {
                java.util.zip.GZIPInputStream(conexion.inputStream)
            } else {
                conexion.inputStream
            }
            val texto = flujo.bufferedReader().use { it.readText() }
            val datos = json.decodeFromString<DatosPrecios>(texto) // valida antes de guardar
            val tmp = File(archivoCache.path + ".tmp")
            tmp.writeText(texto)
            tmp.renameTo(archivoCache)
            datos
        } finally {
            conexion.disconnect()
        }
    }
}
