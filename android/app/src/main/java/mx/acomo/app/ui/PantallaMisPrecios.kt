package mx.acomo.app.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import mx.acomo.app.data.RegistroPrecio
import mx.acomo.app.data.Semaforo
import mx.acomo.app.data.pesos
import mx.acomo.app.data.registrosCsv
import mx.acomo.app.ui.theme.ColoresSemaforo
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMisPrecios(
    registros: List<RegistroPrecio>,
    mercados: Map<String, String>,
    onBorrar: (Long) -> Unit,
    onVolver: () -> Unit,
) {
    val contexto = LocalContext.current
    var aBorrar by remember { mutableStateOf<RegistroPrecio?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis precios") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    if (registros.isNotEmpty()) {
                        TextButton(onClick = {
                            val envio = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Mis precios - ¿A cómo?")
                                putExtra(Intent.EXTRA_TEXT, registrosCsv(registros, mercados))
                            }
                            contexto.startActivity(Intent.createChooser(envio, "Compartir mis precios"))
                        }) { Text("Compartir") }
                    }
                },
            )
        },
    ) { padding ->
        if (registros.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Aún no has guardado precios.\n\nAbre un producto, escribe cuánto te cobraron " +
                        "y toca \"Guardar en Mis precios\".",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            item { Resumen(registros) }
            items(registros.sortedByDescending { it.id }, key = { it.id }) { r ->
                ListItem(
                    leadingContent = { Punto(r.semaforo) },
                    headlineContent = { Text(r.producto) },
                    supportingContent = {
                        val lugar = if (r.lugar.isBlank()) "" else " · ${r.lugar}"
                        Text("${r.fecha}$lugar\nMayoreo ${pesos(r.mayoreo)} · ${veces(r.factor)}")
                    },
                    trailingContent = {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${pesos(r.precio)}/kg", fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = { aBorrar = r }) { Text("Borrar") }
                        }
                    },
                )
                HorizontalDivider()
            }
        }
    }

    aBorrar?.let { r ->
        AlertDialog(
            onDismissRequest = { aBorrar = null },
            title = { Text("¿Borrar este precio?") },
            text = { Text("${r.producto}, ${pesos(r.precio)}/kg del ${r.fecha}") },
            confirmButton = {
                TextButton(onClick = { onBorrar(r.id); aBorrar = null }) { Text("Borrar") }
            },
            dismissButton = { TextButton(onClick = { aBorrar = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Resumen(registros: List<RegistroPrecio>) {
    val promedio = registros.map { it.factor }.average()
    Card(Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (registros.size == 1) "1 precio guardado" else "${registros.size} precios guardados",
                style = MaterialTheme.typography.titleMedium,
            )
            Text("En promedio pagas ${String.format(Locale.US, "%.1f", promedio)} veces el precio de mayoreo.")
            Text(
                "Comparte tu lista para ayudar a calibrar el semáforo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Punto(semaforo: Semaforo) {
    val color = when (semaforo) {
        Semaforo.VERDE -> ColoresSemaforo.verde
        Semaforo.AMARILLO -> ColoresSemaforo.amarillo
        Semaforo.ROJO -> ColoresSemaforo.rojo
    }
    Box(Modifier.size(14.dp).background(color, CircleShape))
}

private fun veces(factor: Double) = String.format(Locale.US, "%.1f× mayoreo", factor)
