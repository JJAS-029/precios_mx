package mx.acomo.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.acomo.app.BuildConfig
import mx.acomo.app.data.AlmacenLocal
import mx.acomo.app.data.DatosMercado
import mx.acomo.app.data.Indice
import mx.acomo.app.data.Mercado
import mx.acomo.app.data.Producto
import mx.acomo.app.data.RegistroPrecio
import mx.acomo.app.data.Repositorio
import mx.acomo.app.data.evaluarPrecio
import mx.acomo.app.data.hoy
import mx.acomo.app.data.nombreVisible
import java.io.File

data class EstadoUi(
    val indice: Indice? = null,
    /** Historial del mercado elegido. */
    val datos: DatosMercado? = null,
    val cargando: Boolean = true,
    /** Mensaje si la descarga falló (se siguen mostrando los datos guardados si hay). */
    val error: String? = null,
    /** id del mercado elegido; null = el usuario aún no elige. */
    val mercado: String? = null,
    val registros: List<RegistroPrecio> = emptyList(),
    val ultimoLugar: String = "",
) {
    val mercadoActual: Mercado? get() = indice?.mercados?.find { it.id == mercado }

    /** Datos que corresponden al mercado elegido (evita mostrar los del anterior mientras carga). */
    val datosActuales: DatosMercado? get() = datos?.takeIf { it.mercado == mercado }
}

class PreciosViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repositorio(BuildConfig.URL_DATOS, File(app.filesDir, "datos"))
    private val almacen = AlmacenLocal(
        File(app.filesDir, "mis_precios.json"),
        app.getSharedPreferences("preferencias", Context.MODE_PRIVATE),
    )

    private val _estado = MutableStateFlow(
        EstadoUi(mercado = almacen.mercado, ultimoLugar = almacen.ultimoLugar),
    )
    val estado: StateFlow<EstadoUi> = _estado.asStateFlow()

    private var cargaMercado: Job? = null

    init {
        viewModelScope.launch {
            val registros = almacen.registros()
            val indice = repo.indiceCache()
            val datos = _estado.value.mercado?.let { repo.mercadoCache(it) }
            _estado.update { it.copy(indice = indice, datos = datos, registros = registros) }
            actualizar()
        }
    }

    /** Descarga el índice y los datos del mercado elegido. */
    fun actualizar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            val error = runCatching {
                val indice = repo.descargarIndice()
                _estado.update { it.copy(indice = indice) }
                _estado.value.mercado?.let { id ->
                    val datos = repo.descargarMercado(id)
                    if (_estado.value.mercado == id) _estado.update { it.copy(datos = datos) }
                }
            }.exceptionOrNull()
            _estado.update { it.copy(cargando = false, error = error?.let { mensajeError() }) }
        }
    }

    fun elegirMercado(id: String) {
        almacen.mercado = id
        _estado.update { it.copy(mercado = id, error = null) }
        cargaMercado?.cancel()
        cargaMercado = viewModelScope.launch {
            repo.mercadoCache(id)?.let { cache -> _estado.update { it.copy(datos = cache) } }
            _estado.update { it.copy(cargando = true) }
            val error = runCatching {
                val datos = repo.descargarMercado(id)
                if (_estado.value.mercado == id) _estado.update { it.copy(datos = datos) }
            }.exceptionOrNull()
            _estado.update { it.copy(cargando = false, error = error?.let { mensajeError() }) }
        }
    }

    private fun mensajeError(): String =
        if (_estado.value.datosActuales != null) {
            "Sin conexión: mostrando los últimos datos guardados."
        } else {
            "No se pudieron descargar los precios. Revisa tu conexión."
        }

    fun guardarPrecio(producto: Producto, precio: Double, mayoreo: Double, lugar: String) {
        val estado = _estado.value
        val registro = RegistroPrecio(
            id = System.currentTimeMillis(),
            fecha = hoy(),
            productoId = producto.id,
            producto = producto.nombreVisible,
            precio = precio,
            mayoreo = mayoreo,
            mercado = estado.mercado.orEmpty(),
            lugar = lugar.trim(),
            semaforo = evaluarPrecio(precio, mayoreo, producto.categoria).semaforo,
        )
        almacen.ultimoLugar = lugar.trim()
        val nuevos = estado.registros + registro
        _estado.update { it.copy(registros = nuevos, ultimoLugar = lugar.trim()) }
        viewModelScope.launch { almacen.guardar(nuevos) }
    }

    fun borrarRegistro(id: Long) {
        val nuevos = _estado.value.registros.filterNot { it.id == id }
        _estado.update { it.copy(registros = nuevos) }
        viewModelScope.launch { almacen.guardar(nuevos) }
    }
}
