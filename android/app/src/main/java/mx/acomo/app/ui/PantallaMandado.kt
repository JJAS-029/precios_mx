package mx.acomo.app.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mx.acomo.app.data.ArticuloMandado
import mx.acomo.app.data.LineaMandado
import mx.acomo.app.data.ListaGuardada
import mx.acomo.app.data.Producto
import mx.acomo.app.data.articuloPorPiezas
import mx.acomo.app.data.cantidad
import mx.acomo.app.data.mandadoTexto
import mx.acomo.app.data.masKg
import mx.acomo.app.data.menosKg
import mx.acomo.app.data.nombreVisible
import mx.acomo.app.data.pesoPieza
import mx.acomo.app.data.pesosRedondo
import mx.acomo.app.data.presupuesto
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PantallaMandado(
    articulos: List<ArticuloMandado>,
    productos: List<Producto>,
    mercado: String,
    listas: List<ListaGuardada>,
    onCambiar: (ArticuloMandado) -> Unit,
    onAbrirProducto: (String) -> Unit,
    onVaciar: () -> Unit,
    onGuardarLista: (String) -> Unit,
    onCargarLista: (ListaGuardada) -> Unit,
    onBorrarLista: (String) -> Unit,
    onVolver: () -> Unit,
) {
    val contexto = LocalContext.current
    val p = remember(articulos, productos) { presupuesto(articulos, productos) }
    var confirmarVaciar by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }
    var porCargar by remember { mutableStateOf<ListaGuardada?>(null) }
    var porBorrar by remember { mutableStateOf<String?>(null) }

    val cargar = { l: ListaGuardada -> if (articulos.isEmpty()) onCargarLista(l) else porCargar = l }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi mandado") },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Text("←", style = MaterialTheme.typography.titleLarge) }
                },
                actions = {
                    if (articulos.isNotEmpty()) {
                        TextButton(onClick = { confirmarVaciar = true }) { Text("Vaciar") }
                    }
                },
            )
        },
    ) { padding ->
        if (articulos.isEmpty()) {
            Column(
                Modifier.padding(padding).fillMaxSize().padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("🛒", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.height(12.dp))
                Text("Tu mandado está vacío", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Abre un producto y toca \"Agregar\" para calcular cuánto vas a gastar.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp),
                )
                if (listas.isNotEmpty()) {
                    Text(
                        "O usa una de tus listas:",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
                    )
                    ChipsListas(listas, cargar) { porBorrar = it }
                }
                Button(onClick = onVolver, shapes = ButtonDefaults.shapes(), modifier = Modifier.padding(top = 20.dp)) {
                    Text("Ver productos")
                }
            }
        } else {
            LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                if (listas.isNotEmpty()) {
                    item {
                        Text(
                            "Mis listas",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 20.dp, top = 4.dp),
                        )
                        ChipsListas(listas, cargar) { porBorrar = it }
                    }
                }
                item {
                    Card(
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text("Vas a gastar aprox.", style = MaterialTheme.typography.labelLarge)
                            Text(pesosRedondo(p.totalEstimado), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
                            Text(
                                "en mercado o tianguis · ${articulos.size} producto${if (articulos.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "En la central de abasto (mayoreo): ${pesosRedondo(p.totalMayoreo)}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            if (p.sinPrecio > 0) {
                                Text(
                                    "⚠ ${p.sinPrecio} sin precio en $mercado (no se suman).",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    shapes = ButtonDefaults.shapes(),
                                    onClick = {
                                        val envio = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, mandadoTexto(p, mercado))
                                        }
                                        contexto.startActivity(Intent.createChooser(envio, "Compartir mandado"))
                                    },
                                ) { Text("Compartir") }
                                OutlinedButton(shapes = ButtonDefaults.shapes(), onClick = { guardando = true }) {
                                    Text("Guardar lista")
                                }
                            }
                        }
                    }
                }
                items(p.lineas, key = { it.articulo.productoId }) { l ->
                    FilaMandado(l, onCambiar, onAbrirProducto)
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
                item {
                    Text(
                        "Estimado con el precio de mayoreo de esta semana × el margen típico de menudeo. " +
                            "Las piezas se calculan con un peso promedio. El precio real puede variar según el puesto.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }

    if (confirmarVaciar) {
        AlertDialog(
            onDismissRequest = { confirmarVaciar = false },
            title = { Text("¿Vaciar el mandado?") },
            text = { Text("Se quitarán todos los productos de la lista.") },
            confirmButton = {
                TextButton(onClick = { onVaciar(); confirmarVaciar = false }) { Text("Vaciar") }
            },
            dismissButton = { TextButton(onClick = { confirmarVaciar = false }) { Text("Cancelar") } },
        )
    }
    if (guardando) {
        var nombre by remember { mutableStateOf("Mandado de la semana") }
        AlertDialog(
            onDismissRequest = { guardando = false },
            title = { Text("Guardar lista") },
            text = {
                Column {
                    Text("Para volver a usarla con un toque. Si ya hay una con ese nombre, se reemplaza.")
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it.take(40) },
                        singleLine = true,
                        label = { Text("Nombre") },
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { onGuardarLista(nombre); guardando = false }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { guardando = false }) { Text("Cancelar") } },
        )
    }
    porCargar?.let { l ->
        AlertDialog(
            onDismissRequest = { porCargar = null },
            title = { Text("¿Usar \"${l.nombre}\"?") },
            text = { Text("Se reemplaza el mandado actual por esta lista (${l.articulos.size} productos).") },
            confirmButton = { TextButton(onClick = { onCargarLista(l); porCargar = null }) { Text("Usar lista") } },
            dismissButton = { TextButton(onClick = { porCargar = null }) { Text("Cancelar") } },
        )
    }
    porBorrar?.let { nombre ->
        AlertDialog(
            onDismissRequest = { porBorrar = null },
            title = { Text("¿Borrar \"$nombre\"?") },
            text = { Text("La lista guardada se borra; tu mandado actual no cambia.") },
            confirmButton = { TextButton(onClick = { onBorrarLista(nombre); porBorrar = null }) { Text("Borrar") } },
            dismissButton = { TextButton(onClick = { porBorrar = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun ChipsListas(listas: List<ListaGuardada>, onCargar: (ListaGuardada) -> Unit, onBorrar: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(listas, key = { it.nombre }) { l ->
            InputChip(
                selected = false,
                onClick = { onCargar(l) },
                label = { Text("${l.nombre} · ${l.articulos.size}") },
                trailingIcon = {
                    Text(
                        "✕",
                        modifier = Modifier.clickable { onBorrar(l.nombre) }.padding(4.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }
    }
}

@Composable
private fun FilaMandado(l: LineaMandado, onCambiar: (ArticuloMandado) -> Unit, onAbrir: (String) -> Unit) {
    val id = l.articulo.productoId
    Row(
        Modifier.fillMaxWidth()
            .clickable(enabled = l.producto != null) { onAbrir(id) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (l.producto != null) InsigniaProducto(l.producto, tamano = 40.dp) else Text("❔", Modifier.width(40.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                l.producto?.nombreVisible ?: l.articulo.nombre,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                l.subtotalEstimado?.let { "≈ ${pesosRedondo(it)}" } ?: "Sin precio aquí",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (l.subtotalEstimado == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
        SelectorCantidad(l.articulo, onCambiar)
    }
}

/**
 * − cantidad + ; bajar de lo mínimo quita el producto. Si el producto se suele comprar por pieza,
 * abajo se puede cambiar entre piezas y kg.
 */
@Composable
fun SelectorCantidad(a: ArticuloMandado, onCambiar: (ArticuloMandado) -> Unit) {
    val peso = pesoPieza(a.nombre)
    val piezas = a.piezas
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val minimo = if (piezas != null) piezas <= 1 else a.kg <= 0.25
            FilledTonalIconButton(
                onClick = {
                    onCambiar(
                        if (piezas != null && peso != null) articuloPorPiezas(a.productoId, a.nombre, piezas - 1, peso)
                        else a.copy(kg = menosKg(a.kg).let { if (it < 0.25) 0.0 else it }),
                    )
                },
                modifier = Modifier.size(40.dp),
            ) { Text(if (minimo) "🗑" else "−", style = MaterialTheme.typography.titleMedium) }
            Text(
                if (piezas != null) "$piezas pza${if (piezas == 1) "" else "s"}" else cantidad(a.kg),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(72.dp),
                textAlign = TextAlign.Center,
            )
            FilledTonalIconButton(
                onClick = {
                    onCambiar(
                        if (piezas != null && peso != null) articuloPorPiezas(a.productoId, a.nombre, piezas + 1, peso)
                        else a.copy(kg = masKg(a.kg)),
                    )
                },
                modifier = Modifier.size(40.dp),
            ) { Text("+", style = MaterialTheme.typography.titleMedium) }
        }
        if (peso != null) {
            Text(
                if (piezas != null) "≈ ${cantidad(a.kg)} · usar kg" else "usar piezas",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable {
                        onCambiar(
                            if (piezas != null) {
                                // A kg en pasos de 250 g, al menos 250 g.
                                a.copy(piezas = null, kg = maxOf(0.25, (a.kg * 4).roundToInt() / 4.0))
                            } else {
                                articuloPorPiezas(a.productoId, a.nombre, maxOf(1, (a.kg / peso).roundToInt()), peso)
                            },
                        )
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

/** Artículo nuevo para el mandado: por pieza si el producto se suele comprar así, si no 1 kg. */
fun articuloNuevo(producto: Producto): ArticuloMandado =
    pesoPieza(producto.nombre)?.let { articuloPorPiezas(producto.id, producto.nombreVisible, 1, it) }
        ?: ArticuloMandado(producto.id, producto.nombreVisible, 1.0)
