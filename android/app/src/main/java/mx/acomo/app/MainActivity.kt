package mx.acomo.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.remember
import mx.acomo.app.data.alternativas
import mx.acomo.app.data.fechaDatos
import mx.acomo.app.ui.PantallaAjustes
import mx.acomo.app.ui.PantallaDetalle
import mx.acomo.app.ui.PantallaMandado
import mx.acomo.app.ui.PantallaLista
import mx.acomo.app.ui.PantallaMercados
import mx.acomo.app.ui.PantallaMisPrecios
import mx.acomo.app.ui.PreciosViewModel
import mx.acomo.app.ui.theme.ModoTema
import mx.acomo.app.ui.theme.TemaAcomo

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: PreciosViewModel = viewModel()
            val estado by vm.estado.collectAsStateWithLifecycle()
            val oscuro = when (estado.modo) {
                ModoTema.SISTEMA -> isSystemInDarkTheme()
                ModoTema.CLARO -> false
                ModoTema.OSCURO -> true
            }
            // Íconos de la barra de estado claros u oscuros según el modo elegido en la app.
            LaunchedEffect(oscuro) {
                val estilo = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { oscuro }
                enableEdgeToEdge(statusBarStyle = estilo, navigationBarStyle = estilo)
            }
            TemaAcomo(estilo = estado.estilo, modo = estado.modo, plus = estado.plus) { App(vm) }
        }
    }
}

@Composable
private fun App(vm: PreciosViewModel) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    var productoId by rememberSaveable { mutableStateOf<String?>(null) }
    var misPrecios by rememberSaveable { mutableStateOf(false) }
    var elegirMercado by rememberSaveable { mutableStateOf(false) }
    var ajustes by rememberSaveable { mutableStateOf(false) }
    var mandado by rememberSaveable { mutableStateOf(false) }

    val producto = productoId?.let { id -> estado.datosActuales?.productos?.find { it.id == id } }
    val indice = estado.indice
    val mercado = estado.mercadoActual

    when {
        estado.mercado == null || elegirMercado -> {
            val primeraVez = estado.mercado == null
            if (!primeraVez) BackHandler { elegirMercado = false }
            PantallaMercados(
                indice = indice,
                seleccionado = estado.mercado,
                error = estado.error,
                onElegir = { id ->
                    vm.elegirMercado(id)
                    elegirMercado = false
                    productoId = null
                },
                onVolver = if (primeraVez) null else ({ elegirMercado = false }),
                onReintentar = vm::actualizar,
            )
        }
        producto != null && indice != null && mercado != null -> {
            BackHandler { productoId = null }
            PantallaDetalle(
                indice = indice,
                producto = producto,
                mercado = mercado,
                ultimoLugar = estado.ultimoLugar,
                fechaDatos = estado.datosActuales?.fechaDatos.orEmpty(),
                onGuardarPrecio = { precio, mayoreo, lugar -> vm.guardarPrecio(producto, precio, mayoreo, lugar) },
                alternativas = remember(producto, estado.datosActuales) {
                    alternativas(producto, estado.datosActuales?.productos.orEmpty())
                },
                onAbrirProducto = { productoId = it },
                kgEnMandado = estado.mandado.find { it.productoId == producto.id }?.kg,
                onPonerEnMandado = { kg -> vm.ponerEnMandado(producto, kg) },
                onAbrirMandado = { productoId = null; mandado = true },
                onVolver = { productoId = null },
            )
        }
        mandado -> {
            BackHandler { mandado = false }
            PantallaMandado(
                articulos = estado.mandado,
                productos = estado.datosActuales?.productos.orEmpty(),
                mercado = mercado?.ciudad.orEmpty(),
                onCambiarKg = vm::cambiarKg,
                onAbrirProducto = { productoId = it },
                onVaciar = vm::vaciarMandado,
                onVolver = { mandado = false },
            )
        }
        ajustes -> {
            BackHandler { ajustes = false }
            PantallaAjustes(
                estilo = estado.estilo,
                modo = estado.modo,
                plus = estado.plus,
                onEstilo = vm::elegirEstilo,
                onModo = vm::elegirModo,
                onCambiarPlus = vm::cambiarPlus,
                onVolver = { ajustes = false },
            )
        }
        misPrecios -> {
            BackHandler { misPrecios = false }
            PantallaMisPrecios(
                registros = estado.registros,
                mercados = indice?.mercados.orEmpty().associate { it.id to "${it.nombre}, ${it.ciudad}" },
                onBorrar = vm::borrarRegistro,
                onVolver = { misPrecios = false },
            )
        }
        else -> PantallaLista(
            estado = estado,
            onCambiarMercado = { elegirMercado = true },
            onActualizar = vm::actualizar,
            onAbrirProducto = { productoId = it },
            onAbrirMisPrecios = { misPrecios = true },
            onAbrirAjustes = { ajustes = true },
            onAbrirMandado = { mandado = true },
        )
    }
}
