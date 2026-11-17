package mx.acomo.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import mx.acomo.app.BuildConfig
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

/**
 * Descarga los JSON generados por el recolector y guarda copia local
 * para que la app funcione sin internet (en el mercado suele haber mala señal).
 */
class Repositorio(
    /** URL base que termina en "/data/". */
    private val urlBase: String,
    private val dirCache: File,
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        fun de(contexto: Context) = Repositorio(BuildConfig.URL_DATOS, File(contexto.filesDir, "datos"))
    }

    suspend fun indiceCache(): Indice? = leer("indice.json")
    suspend fun mercadoCache(id: String): DatosMercado? = leer("mercados/$id.json")

    suspend fun descargarIndice(): Indice = descargar("indice.json")
    suspend fun descargarMercado(id: String): DatosMercado = descargar("mercados/$id.json")

    private suspend inline fun <reified T> leer(ruta: String): T? = withContext(Dispatchers.IO) {
        runCatching { json.decodeFromString<T>(archivo(ruta).readText()) }.getOrNull()
    }

    private suspend inline fun <reified T> descargar(ruta: String): T = withContext(Dispatchers.IO) {
        val texto = descargarTexto(urlBase + ruta)
        val datos = json.decodeFromString<T>(texto) // valida antes de guardar
        val destino = archivo(ruta)
        destino.parentFile?.mkdirs()
        val tmp = File(destino.path + ".tmp")
        tmp.writeText(texto)
        tmp.renameTo(destino)
        datos
    }

    private fun archivo(ruta: String) = File(dirCache, ruta)

    private fun descargarTexto(url: String): String {
        val conexion = URL(url).openConnection() as HttpURLConnection
        try {
            conexion.connectTimeout = 15_000
            conexion.readTimeout = 30_000
            conexion.setRequestProperty("Accept-Encoding", "gzip")
            val codigo = conexion.responseCode
            check(codigo == 200) { "El servidor respondió $codigo" }
            val flujo = if (conexion.contentEncoding == "gzip") GZIPInputStream(conexion.inputStream) else conexion.inputStream
            return flujo.bufferedReader().use { it.readText() }
        } finally {
            conexion.disconnect()
        }
    }
}
