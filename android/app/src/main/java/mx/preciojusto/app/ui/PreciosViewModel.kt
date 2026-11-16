package mx.preciojusto.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.preciojusto.app.BuildConfig
import mx.preciojusto.app.data.DatosPrecios
import mx.preciojusto.app.data.Repositorio
import java.io.File

data class EstadoUi(
    val datos: DatosPrecios? = null,
    val cargando: Boolean = true,
    /** Mensaje si la descarga falló (se siguen mostrando los datos guardados si hay). */
    val error: String? = null,
    val mercado: String = MERCADO_INICIAL,
)

/** Toluca: primer mercado del Estado de México. */
const val MERCADO_INICIAL = "150"

class PreciosViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repositorio(BuildConfig.URL_PRECIOS, File(app.filesDir, "precios.json"))

    private val _estado = MutableStateFlow(EstadoUi())
    val estado: StateFlow<EstadoUi> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            repo.leerCache()?.let { cache -> _estado.update { it.copy(datos = cache) } }
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

    fun elegirMercado(id: String) = _estado.update { it.copy(mercado = id) }
}
