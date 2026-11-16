package mx.preciojusto.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import mx.preciojusto.app.data.Producto
import mx.preciojusto.app.data.Resumen
import mx.preciojusto.app.data.coincide
import mx.preciojusto.app.data.etiquetaSemana
import mx.preciojusto.app.data.nombreVisible
import mx.preciojusto.app.data.pesos
import mx.preciojusto.app.data.porcentaje
import mx.preciojusto.app.data.resumen

/** Un producto "está barato" si su precio está al menos 8 % abajo de su promedio de 8 semanas. */
private const val UMBRAL_BARATO = -8.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaLista(
    estado: EstadoUi,
    onElegirMercado: (String) -> Unit,
    onActualizar: () -> Unit,
    onAbrirProducto: (String) -> Unit,
) {
    var busqueda by rememberSaveable { mutableStateOf("") }
    val datos = estado.datos

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Precio Justo")
                        datos?.semanas?.lastOrNull()?.let {
                            Text(
                                "Mayoreo, semana del ${etiquetaSemana(it)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (datos == null) {
            SinDatos(estado, onActualizar, Modifier.padding(padding))
            return@Scaffold
        }

        // Productos con datos en el mercado elegido, con su resumen.
        val conResumen: List<Pair<Producto, Resumen>> = remember(datos, estado.mercado) {
            datos.productos
                .mapNotNull { p -> p.resumen(estado.mercado)?.let { p to it } }
                .sortedBy { it.first.nombreVisible }
        }
        val filtrados = remember(conResumen, busqueda) {
            conResumen.filter { it.first.coincide(busqueda) }
        }
        val baratos = remember(conResumen) {
            conResumen.filter { it.second.vsPromedio <= UMBRAL_BARATO }
                .sortedBy { it.second.vsPromedio }
                .take(5)
        }

        PullToRefreshBox(
            isRefreshing = estado.cargando,
            onRefresh = onActualizar,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    OutlinedTextField(
                        value = busqueda,
                        onValueChange = { busqueda = it },
                        placeholder = { Text("Buscar: jitomate, aguacate, limón…") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
                    )
                }
                item {
                    SelectorMercado(datos.mercados, estado.mercado, onElegirMercado)
                }
                estado.error?.let { msg ->
                    item { Aviso(msg) }
                }

                if (busqueda.isBlank() && baratos.isNotEmpty()) {
                    item { Encabezado("Barato esta semana") }
                    items(baratos, key = { "b-" + it.first.id }) { (p, r) ->
                        FilaProducto(p, r, onAbrirProducto, mostrarVsPromedio = true)
                    }
                    item { Encabezado("Todos los productos") }
                }

                items(filtrados, key = { it.first.id }) { (p, r) ->
                    FilaProducto(p, r, onAbrirProducto)
                }

                if (filtrados.isEmpty()) {
                    item {
                        Text(
                            "No encontramos \"$busqueda\" en ${nombreCorto(datos.mercados[estado.mercado].orEmpty())}.",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item {
                    Text(
                        "Fuente: SNIIM, Secretaría de Economía. Precios de mayoreo por kg.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaProducto(
    p: Producto,
    r: Resumen,
    onAbrir: (String) -> Unit,
    mostrarVsPromedio: Boolean = false,
) {
    ListItem(
        headlineContent = { Text(p.nombreVisible) },
        supportingContent = {
            val calidad = if (p.calidad == "Primera") "" else " · ${p.calidad}"
            Text(
                if (mostrarVsPromedio) "${porcentaje(r.vsPromedio)} vs. su promedio" else "${p.categoria}$calidad",
            )
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${pesos(r.actual.frec)}/kg",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Cambio(r.cambioSemanal)
            }
        },
        modifier = Modifier.clickable { onAbrir(p.id) },
    )
    HorizontalDivider()
}

@Composable
private fun Encabezado(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun Aviso(texto: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
    ) {
        Text(texto, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SinDatos(estado: EstadoUi, onReintentar: () -> Unit, modifier: Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (estado.cargando) {
            CircularProgressIndicator()
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(32.dp),
            ) {
                Text(estado.error ?: "Sin datos", style = MaterialTheme.typography.bodyLarge)
                Button(onClick = onReintentar) { Text("Reintentar") }
            }
        }
    }
}
