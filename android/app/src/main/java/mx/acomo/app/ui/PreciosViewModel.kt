package mx.acomo.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.app.Activity
import mx.acomo.app.AvisosPrecios
import mx.acomo.app.BuildConfig
import mx.acomo.app.Compras
import mx.acomo.app.EstadoCompra
import mx.acomo.app.data.AlmacenLocal
import mx.acomo.app.data.ArticuloMandado
import mx.acomo.app.data.DatosMercado
import mx.acomo.app.data.Indice
import mx.acomo.app.data.Mercado
import mx.acomo.app.data.Producto
import mx.acomo.app.data.RegistroPrecio
import mx.acomo.app.data.Temporada
import mx.acomo.app.data.Repositorio
import mx.acomo.app.data.evaluarPrecio
import mx.acomo.app.data.hoy
import mx.acomo.app.data.nombreVisible
import mx.acomo.app.ui.theme.EstiloColor
import mx.acomo.app.ui.theme.ModoTema

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
    val estilo: EstiloColor = EstiloColor.VERDE,
    val modo: ModoTema = ModoTema.SISTEMA,
    val plus: Boolean = false,
    val mandado: List<ArticuloMandado> = emptyList(),
    /** Productos con aviso de bajada: id -> nombre. */
    val seguidos: Map<String, String> = emptyMap(),
    val avisos: Boolean = true,
    val avisosTemporada: Boolean = true,
    /** Índice de temporada por producto (varios años de historia). */
    val temporada: Temporada? = null,
) {
    val mercadoActual: Mercado? get() = indice?.mercados?.find { it.id == mercado }

    /** Datos que corresponden al mercado elegido (evita mostrar los del anterior mientras carga). */
    val datosActuales: DatosMercado? get() = datos?.takeIf { it.mercado == mercado }
}

class PreciosViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repositorio.de(app)
    private val almacen = AlmacenLocal.de(app)

    private val _estado = MutableStateFlow(
        EstadoUi(
            mercado = almacen.mercado,
            ultimoLugar = almacen.ultimoLugar,
            estilo = EstiloColor.desde(almacen.estiloColor),
            modo = ModoTema.desde(almacen.modoTema),
            plus = almacen.plus,
            mandado = almacen.mandado,
            seguidos = almacen.seguidos,
            avisos = almacen.avisos,
            avisosTemporada = almacen.avisosTemporada,
        ),
    )
    val estado: StateFlow<EstadoUi> = _estado.asStateFlow()

    private var cargaMercado: Job? = null

    /** ¿A cómo? Plus con Google Play Billing. */
    private val compras = Compras(app) { activo ->
        // En la versión de desarrollo manda el interruptor de prueba, no Google Play.
        if (!activo && BuildConfig.DEBUG) return@Compras
        if (almacen.plus != activo) almacen.plus = activo
        _estado.update { it.copy(plus = activo) }
    }
    val compra: StateFlow<EstadoCompra> = compras.estado

    init {
        AvisosPrecios.programar(app)
        compras.conectar()
        viewModelScope.launch {
            val registros = almacen.registros()
            val indice = repo.indiceCache()
            val datos = _estado.value.mercado?.let { repo.mercadoCache(it) }
            val temporada = repo.temporadaCache()
            _estado.update { it.copy(indice = indice, datos = datos, registros = registros, temporada = temporada) }
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
            // Cambia una vez al año; si falla, se queda la copia guardada.
            runCatching { repo.descargarTemporada() }.onSuccess { tem -> _estado.update { it.copy(temporada = tem) } }
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

    fun elegirEstilo(estilo: EstiloColor) {
        almacen.estiloColor = estilo.name
        _estado.update { it.copy(estilo = estilo) }
    }

    fun elegirModo(modo: ModoTema) {
        almacen.modoTema = modo.name
        _estado.update { it.copy(modo = modo) }
    }

    fun comprarPlus(actividad: Activity) = compras.comprar(actividad)

    /** Al abrir Ajustes: vuelve a consultar precio y compras (p. ej. un pago pendiente que ya se confirmó). */
    fun refrescarCompras() = compras.conectar()

    fun limpiarMensajeCompra() = compras.limpiarMensaje()

    override fun onCleared() = compras.cerrar()

    /** Solo en la versión de desarrollo: activar Plus sin comprar. */
    fun cambiarPlus(activo: Boolean) {
        almacen.plus = activo
        _estado.update { it.copy(plus = activo) }
    }

    // ----- mandado

    /** Agrega el producto o cambia su cantidad; con kg <= 0 lo quita. */
    fun ponerEnMandado(producto: Producto, kg: Double) =
        cambiarMandado { lista ->
            val sin = lista.filterNot { it.productoId == producto.id }
            if (kg <= 0.0) sin
            else if (sin.size == lista.size) lista + ArticuloMandado(producto.id, producto.nombreVisible, kg)
            else lista.map { if (it.productoId == producto.id) it.copy(kg = kg) else it }
        }

    fun cambiarKg(productoId: String, kg: Double) = cambiarMandado { lista ->
        if (kg <= 0.0) lista.filterNot { it.productoId == productoId }
        else lista.map { if (it.productoId == productoId) it.copy(kg = kg) else it }
    }

    fun vaciarMandado() = cambiarMandado { emptyList() }

    private fun cambiarMandado(cambio: (List<ArticuloMandado>) -> List<ArticuloMandado>) {
        val nueva = cambio(_estado.value.mandado)
        almacen.mandado = nueva
        _estado.update { it.copy(mandado = nueva) }
    }

    // ----- avisos de precios bajos

    fun seguir(producto: Producto, activo: Boolean) {
        val nuevos = if (activo) _estado.value.seguidos + (producto.id to producto.nombreVisible)
        else _estado.value.seguidos - producto.id
        // Al seguir el primer producto, la semana actual ya la está viendo: avisar desde la siguiente.
        if (activo && almacen.semanaAvisada.isBlank()) {
            _estado.value.datosActuales?.semanas?.lastOrNull()?.let { almacen.semanaAvisada = it }
        }
        almacen.seguidos = nuevos
        if (activo && !almacen.avisos) almacen.avisos = true
        _estado.update { it.copy(seguidos = nuevos, avisos = almacen.avisos) }
        AvisosPrecios.programar(getApplication())
    }

    fun dejarDeSeguir(productoId: String) {
        val nuevos = _estado.value.seguidos - productoId
        almacen.seguidos = nuevos
        _estado.update { it.copy(seguidos = nuevos) }
        AvisosPrecios.programar(getApplication())
    }

    fun cambiarAvisosTemporada(activo: Boolean) {
        almacen.avisosTemporada = activo
        _estado.update { it.copy(avisosTemporada = activo) }
        AvisosPrecios.programar(getApplication())
    }

    /** Después de conceder el permiso de notificaciones: el aviso de temporada ya puede programarse. */
    fun permisoNotificacionesCambio() = AvisosPrecios.programar(getApplication())

    fun cambiarAvisos(activo: Boolean) {
        almacen.avisos = activo
        _estado.update { it.copy(avisos = activo) }
        AvisosPrecios.programar(getApplication())
    }

    fun borrarRegistro(id: Long) {
        val nuevos = _estado.value.registros.filterNot { it.id == id }
        _estado.update { it.copy(registros = nuevos) }
        viewModelScope.launch { almacen.guardar(nuevos) }
    }
}
