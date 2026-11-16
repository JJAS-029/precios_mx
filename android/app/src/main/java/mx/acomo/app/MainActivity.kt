package mx.acomo.app

import android.os.Bundle
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
import mx.acomo.app.ui.PantallaDetalle
import mx.acomo.app.ui.PantallaLista
import mx.acomo.app.ui.PreciosViewModel
import mx.acomo.app.ui.theme.TemaAcomo

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TemaAcomo { App() }
        }
    }
}

@Composable
private fun App(vm: PreciosViewModel = viewModel()) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    var productoId by rememberSaveable { mutableStateOf<String?>(null) }

    val producto = productoId?.let { id -> estado.datos?.productos?.find { it.id == id } }
    val datos = estado.datos

    if (producto != null && datos != null) {
        BackHandler { productoId = null }
        PantallaDetalle(
            datos = datos,
            producto = producto,
            mercado = estado.mercado,
            onElegirMercado = vm::elegirMercado,
            onVolver = { productoId = null },
        )
    } else {
        PantallaLista(
            estado = estado,
            onElegirMercado = vm::elegirMercado,
            onActualizar = vm::actualizar,
            onAbrirProducto = { productoId = it },
        )
    }
}
