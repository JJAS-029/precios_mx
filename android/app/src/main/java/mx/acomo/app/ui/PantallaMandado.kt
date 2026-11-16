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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mx.acomo.app.data.ArticuloMandado
import mx.acomo.app.data.LineaMandado
import mx.acomo.app.data.Producto
import mx.acomo.app.data.cantidad
import mx.acomo.app.data.mandadoTexto
import mx.acomo.app.data.nombreVisible
import mx.acomo.app.data.masKg
import mx.acomo.app.data.menosKg
import mx.acomo.app.data.pesosRedondo
import mx.acomo.app.data.presupuesto

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PantallaMandado(
    articulos: List<ArticuloMandado>,
    productos: List<Producto>,
    mercado: String,
    onCambiarKg: (productoId: String, kg: Double) -> Unit,
    onAbrirProducto: (String) -> Unit,
    onVaciar: () -> Unit,
    onVolver: () -> Unit,
) {
    val contexto = LocalContext.current
    val p = remember(articulos, productos) { presupuesto(articulos, productos) }
    var confirmarVaciar by remember { mutableStateOf(false) }

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
                Modifier.padding(padding).fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("🛒", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.height(12.dp))
                Text("Tu mandado está vacío", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Abre un producto y toca \"Agregar al mandado\" para calcular cuánto vas a gastar.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Button(onClick = onVolver, shapes = ButtonDefaults.shapes(), modifier = Modifier.padding(top = 20.dp)) {
                    Text("Ver productos")
                }
            }
            return@Scaffold
        }

        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
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
                        OutlinedButton(
                            shapes = ButtonDefaults.shapes(),
                            onClick = {
                                val envio = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, mandadoTexto(p, mercado))
                                }
                                contexto.startActivity(Intent.createChooser(envio, "Compartir mandado"))
                            },
                        ) { Text("Compartir lista") }
                    }
                }
            }
            items(p.lineas, key = { it.articulo.productoId }) { l ->
                FilaMandado(l, onCambiarKg, onAbrirProducto)
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            }
            item {
                Text(
                    "Estimado con el precio de mayoreo de esta semana × el margen típico de menudeo. " +
                        "El precio real puede variar según el puesto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
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
}

@Composable
private fun FilaMandado(l: LineaMandado, onCambiarKg: (String, Double) -> Unit, onAbrir: (String) -> Unit) {
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
        SelectorKg(l.articulo.kg) { onCambiarKg(id, it) }
    }
}

/** − 1.5 kg + ; bajar de 250 g quita el producto. */
@Composable
fun SelectorKg(kg: Double, onCambiar: (Double) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = { onCambiar(menosKg(kg)) }, modifier = Modifier.size(40.dp)) {
            Text(if (kg <= 0.25) "🗑" else "−", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            cantidad(kg),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(72.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        FilledTonalIconButton(onClick = { onCambiar(masKg(kg)) }, modifier = Modifier.size(40.dp)) {
            Text("+", style = MaterialTheme.typography.titleMedium)
        }
    }
}
