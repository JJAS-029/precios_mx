package mx.acomo.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.acomo.app.BuildConfig
import mx.acomo.app.data.AlmacenLocal
import mx.acomo.app.data.DatosPrecios
import mx.acomo.app.data.Producto
import mx.acomo.app.data.RegistroPrecio
import mx.acomo.app.data.Repositorio
import mx.acomo.app.data.evaluarPrecio
import mx.acomo.app.data.hoy
import mx.acomo.app.data.nombreVisible
import java.io.File

data class EstadoUi(
    val datos: DatosPrecios? = null,
    val cargando: Boolean = true,
    /** Mensaje si la descarga falló (se siguen mostrando los datos guardados si hay). */
    val error: String? = null,
    val mercado: String = MERCADO_INICIAL,
    val registros: List<RegistroPrecio> = emptyList(),
    val ultimoLugar: String = "",
)

/** Toluca: primer mercado del Estado de México. */
const val MERCADO_INICIAL = "150"

class PreciosViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repositorio(BuildConfig.URL_PRECIOS, File(app.filesDir, "precios.json"))
    private val almacen = AlmacenLocal(
        File(app.filesDir, "mis_precios.json"),
        app.getSharedPreferences("preferencias", Context.MODE_PRIVATE),
    )

    private val _estado = MutableStateFlow(
        EstadoUi(mercado = almacen.mercado ?: MERCADO_INICIAL, ultimoLugar = almacen.ultimoLugar),
    )
    val estado: StateFlow<EstadoUi> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val registros = almacen.registros()
            val cache = repo.leerCache()
            _estado.update { it.copy(datos = cache ?: it.datos, registros = registros) }
            actualizar()
        }
    }

    fun actualizar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            try {
                val datos = repo.descargar()
                _estado.update { it.copy(datos = datos, cargando = false) }
            } catch (e: Exception) {
                val msg = if (_estado.value.datos != null) {
                    "Sin conexión: mostrando los últimos datos guardados."
                } else {
                    "No se pudieron descargar los precios. Revisa tu conexión."
                }
                _estado.update { it.copy(cargando = false, error = msg) }
            }
        }
    }

    fun elegirMercado(id: String) {
        almacen.mercado = id
        _estado.update { it.copy(mercado = id) }
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
            mercado = estado.mercado,
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
