package mx.acomo.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mx.acomo.app.R
import mx.acomo.app.data.Producto
import mx.acomo.app.data.Resumen
import mx.acomo.app.data.coincide
import mx.acomo.app.data.fechaCorta
import mx.acomo.app.data.fechaDatos
import mx.acomo.app.data.nombreVisible
import mx.acomo.app.data.pesos
import mx.acomo.app.data.porcentaje
import mx.acomo.app.data.resumen

/** Un producto "está barato" si su precio está al menos 8 % abajo de su promedio de 8 semanas. */
private const val UMBRAL_BARATO = -8.0

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PantallaLista(
    estado: EstadoUi,
    onCambiarMercado: () -> Unit,
    onActualizar: () -> Unit,
    onAbrirProducto: (String) -> Unit,
    onAbrirMisPrecios: () -> Unit,
    onAbrirAjustes: () -> Unit,
) {
    var busqueda by rememberSaveable { mutableStateOf("") }
    val teclado = LocalSoftwareKeyboardController.current
    val datos = estado.datosActuales
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text("¿A cómo?", fontWeight = FontWeight.Bold) },
                subtitle = {
                    val fecha = datos?.fechaDatos?.let(::fechaCorta).orEmpty()
                    if (fecha.isNotBlank()) Text("Precios de mayoreo al $fecha")
                },
                navigationIcon = {
                    Image(
                        painter = painterResource(R.drawable.logo_koala),
                        contentDescription = null,
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp).size(44.dp),
                    )
                },
                actions = {
                    TextButton(onClick = onAbrirMisPrecios) { Text("Mis precios", fontWeight = FontWeight.Bold) }
                    IconButton(onClick = onAbrirAjustes) { Text("⚙", style = MaterialTheme.typography.titleLarge) }
                },
                scrollBehavior = scroll,
            )
        },
        bottomBar = { BannerAnuncio(estado.plus) },
    ) { padding ->
        if (datos == null) {
            Column(Modifier.padding(padding)) {
                BotonMercado(estado.mercadoActual, onCambiarMercado, Modifier.padding(top = 8.dp))
                SinDatos(estado, onActualizar)
            }
            return@Scaffold
        }

        // Productos con datos en el mercado elegido, con su resumen.
        val conResumen: List<Pair<Producto, Resumen>> = remember(datos) {
            datos.productos
                .mapNotNull { p -> p.resumen()?.let { p to it } }
                .sortedBy { it.first.nombreVisible }
        }
        val filtrados = remember(conResumen, busqueda) {
            conResumen.filter { it.first.coincide(busqueda) }
        }
        val baratos = remember(conResumen) {
            conResumen.filter { it.second.vsPromedio <= UMBRAL_BARATO }
                .sortedBy { it.second.vsPromedio }
                .take(8)
        }

        PullToRefreshBox(
            isRefreshing = estado.cargando,
            onRefresh = onActualizar,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    BotonMercado(estado.mercadoActual, onCambiarMercado, Modifier.padding(top = 4.dp))
                    LineaActualizacion(
                        ultimaFecha = datos.fechaDatos,
                        revisado = estado.indice?.generado,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp),
                    )
                }
                item {
                    OutlinedTextField(
                        value = busqueda,
                        onValueChange = { busqueda = it },
                        placeholder = { Text("🔍  Buscar: jitomate, aguacate, limón…") },
                        singleLine = true,
                        shape = CircleShape,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { teclado?.hide() }),
                        modifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp),
                    )
                }
                estado.error?.let { msg ->
                    item { Aviso(msg) }
                }

                if (busqueda.isBlank() && baratos.isNotEmpty()) {
                    item { Encabezado("Barato esta semana") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(baratos, key = { "b-" + it.first.id }) { (p, r) ->
                                TarjetaBarato(p, r) { onAbrirProducto(p.id) }
                            }
                        }
                    }
                    item { Encabezado("Todos los productos") }
                }

                items(filtrados, key = { it.first.id }) { (p, r) ->
                    FilaProducto(p, r, onAbrirProducto)
                }

                if (filtrados.isEmpty()) {
                    item {
                        Text(
                            "No encontramos \"$busqueda\" en ${estado.mercadoActual?.ciudad.orEmpty()}.",
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
private fun TarjetaBarato(p: Producto, r: Resumen, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.width(150.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            InsigniaProducto(p, tamano = 48.dp, fondo = MaterialTheme.colorScheme.surfaceContainerLowest)
            Spacer(Modifier.height(10.dp))
            Text(
                p.nombreVisible,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                "${pesos(r.actual.frec)}/kg",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                "▼ ${porcentaje(r.vsPromedio)} vs. promedio",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun FilaProducto(p: Producto, r: Resumen, onAbrir: (String) -> Unit) {
    ListItem(
        leadingContent = { InsigniaProducto(p) },
        headlineContent = { Text(p.nombreVisible, fontWeight = FontWeight.Medium) },
        supportingContent = {
            val calidad = if (p.calidad == "Primera") "" else " · ${p.calidad}"
            Text("${p.categoria}$calidad")
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${pesos(r.actual.frec)}/kg",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Cambio(r.cambioSemanal)
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.clickable { onAbrir(p.id) },
    )
}

@Composable
private fun Encabezado(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun Aviso(texto: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(16.dp, 4.dp),
    ) {
        Text(texto, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SinDatos(estado: EstadoUi, onReintentar: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (estado.cargando) {
            LoadingIndicator(Modifier.size(64.dp))
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(32.dp),
            ) {
                Text(estado.error ?: "Sin datos", style = MaterialTheme.typography.bodyLarge)
                Button(onClick = onReintentar, shapes = ButtonDefaults.shapes()) { Text("Reintentar") }
            }
        }
    }
}
