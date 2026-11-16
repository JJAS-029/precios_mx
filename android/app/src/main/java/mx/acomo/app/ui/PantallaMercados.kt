package mx.acomo.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mx.acomo.app.data.Indice
import mx.acomo.app.data.Mercado
import mx.acomo.app.data.coincide
import mx.acomo.app.data.distanciaKm
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMercados(
    indice: Indice?,
    seleccionado: String?,
    error: String?,
    onElegir: (String) -> Unit,
    /** null cuando es la primera vez y aún no hay mercado elegido (no se puede volver). */
    onVolver: (() -> Unit)?,
    onReintentar: () -> Unit,
) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val teclado = LocalSoftwareKeyboardController.current
    var busqueda by rememberSaveable { mutableStateOf("") }
    var lat by rememberSaveable { mutableStateOf<Double?>(null) }
    var lon by rememberSaveable { mutableStateOf<Double?>(null) }
    var buscando by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf<String?>(null) }

    fun ubicar() {
        buscando = true
        aviso = null
        alcance.launch {
            val loc = obtenerUbicacion(contexto)
            buscando = false
            if (loc != null) {
                lat = loc.latitude
                lon = loc.longitude
                busqueda = ""
            } else {
                aviso = "No pudimos obtener tu ubicación. Revisa que esté activada o busca tu ciudad."
            }
        }
    }

    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) ubicar() else aviso = "Sin permiso de ubicación. Puedes buscar tu ciudad o estado."
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("¿Dónde compras?")
                        Text(
                            "Elige la central de abasto más cercana",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    if (onVolver != null) {
                        IconButton(onClick = onVolver) { Text("←", style = MaterialTheme.typography.titleLarge) }
                    }
                },
            )
        },
    ) { padding ->
        if (indice == null) {
            Column(
                Modifier.padding(padding).fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (error == null) {
                    CircularProgressIndicator()
                } else {
                    Text(error)
                    FilledTonalButton(onClick = onReintentar, modifier = Modifier.padding(top = 12.dp)) {
                        Text("Reintentar")
                    }
                }
            }
            return@Scaffold
        }

        val ordenados: List<Pair<Mercado, Double?>> = remember(indice, busqueda, lat, lon) {
            val filtrados = indice.mercados.filter { it.coincide(busqueda) }
            val la = lat
            val lo = lon
            if (la != null && lo != null) {
                filtrados.map { it to distanciaKm(la, lo, it.lat, it.lon) }.sortedBy { it.second }
            } else {
                filtrados.sortedWith(compareBy({ it.estado }, { it.ciudad })).map { it to null }
            }
        }

        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            item {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    placeholder = { Text("Buscar ciudad o estado") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { teclado?.hide() }),
                    modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
                )
            }
            item {
                FilledTonalButton(
                    onClick = {
                        if (tienePermisoUbicacion(contexto)) ubicar()
                        else pedirPermiso.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                    enabled = !buscando,
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Text(if (buscando) "Buscando…" else "📍 Usar mi ubicación")
                }
                Text(
                    aviso ?: "Tu ubicación solo se usa en tu teléfono para ordenar los mercados por cercanía.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (aviso != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp, 6.dp),
                )
            }
            if (lat != null && busqueda.isBlank()) {
                item {
                    Text(
                        "Más cercanos a ti",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                    )
                }
            }
            items(ordenados, key = { it.first.id }) { (m, km) ->
                val elegido = m.id == seleccionado
                ListItem(
                    headlineContent = {
                        Text(m.ciudad, fontWeight = if (elegido) FontWeight.Bold else FontWeight.Normal)
                    },
                    supportingContent = { Text("${m.nombre} · ${m.estado}") },
                    trailingContent = {
                        when {
                            elegido -> Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            km != null -> Text(formatoKm(km), style = MaterialTheme.typography.labelLarge)
                        }
                    },
                    colors = if (elegido) {
                        ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    } else {
                        ListItemDefaults.colors()
                    },
                    modifier = Modifier.clickable { onElegir(m.id) },
                )
                HorizontalDivider()
            }
            if (ordenados.isEmpty()) {
                item {
                    Text(
                        "No hay centrales de abasto que coincidan con \"$busqueda\".",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun formatoKm(km: Double): String = when {
    km < 10 -> "${(km * 10).roundToInt() / 10.0} km"
    else -> "${km.roundToInt()} km"
}
