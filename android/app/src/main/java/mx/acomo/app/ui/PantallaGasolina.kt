package mx.acomo.app.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mx.acomo.app.data.Combustible
import mx.acomo.app.data.DatosGasolina
import mx.acomo.app.data.GasolineraCercana
import mx.acomo.app.data.LITROS_TANQUE
import mx.acomo.app.data.Mercado
import mx.acomo.app.data.cercanas
import mx.acomo.app.data.distancia
import mx.acomo.app.data.fechaCorta
import mx.acomo.app.data.pesos
import mx.acomo.app.data.pesosRedondo
import mx.acomo.app.data.resumenGasolina

private val RADIOS = listOf(3.0, 5.0, 10.0)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PantallaGasolina(
    datos: DatosGasolina?,
    cargando: Boolean,
    error: String?,
    mercado: Mercado?,
    onCargar: () -> Unit,
    onVolver: () -> Unit,
) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    var tipo by rememberSaveable { mutableStateOf(Combustible.MAGNA) }
    var radio by rememberSaveable { mutableStateOf(5.0) }
    // Punto de búsqueda: la ubicación del teléfono si hay permiso; si no, la ciudad de la central elegida.
    var punto by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var desdeUbicacion by rememberSaveable { mutableStateOf(false) }
    var buscando by remember { mutableStateOf(false) }

    fun ubicar() {
        buscando = true
        alcance.launch {
            obtenerUbicacion(contexto)?.let { punto = it.latitude to it.longitude; desdeUbicacion = true }
            buscando = false
        }
    }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) ubicar() }

    LaunchedEffect(Unit) {
        onCargar()
        if (tienePermisoUbicacion(contexto)) ubicar()
    }
    val origen = punto ?: mercado?.let { it.lat to it.lon }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gasolina") },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Text("←", style = MaterialTheme.typography.titleLarge) }
                },
            )
        },
    ) { padding ->
        val lista = remember(datos, origen, radio, tipo) {
            if (datos == null || origen == null) emptyList() else datos.cercanas(origen.first, origen.second, radio, tipo)
        }
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            when {
                                desdeUbicacion -> "📍 Cerca de tu ubicación"
                                mercado != null -> "📍 Cerca de ${mercado.ciudad} (centro)"
                                else -> "📍 Elige un mercado o usa tu ubicación"
                            },
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        if (!desdeUbicacion) {
                            TextButton(onClick = {
                                if (tienePermisoUbicacion(contexto)) ubicar()
                                else pedirPermiso.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            }) { Text(if (buscando) "Buscando…" else "Usar mi ubicación") }
                        }
                    }
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Combustible.entries.forEachIndexed { i, c ->
                            SegmentedButton(
                                selected = c == tipo,
                                onClick = { tipo = c },
                                shape = SegmentedButtonDefaults.itemShape(i, Combustible.entries.size),
                            ) { Text(c.nombre, maxLines = 1) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        RADIOS.forEach { r ->
                            FilterChip(selected = r == radio, onClick = { radio = r }, label = { Text("${r.toInt()} km") })
                        }
                    }
                }
            }
            error?.let { msg ->
                item {
                    Text(
                        msg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp, 8.dp),
                    )
                }
            }
            when {
                datos == null && cargando -> item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        LoadingIndicator(Modifier.size(56.dp))
                    }
                }
                datos == null -> item {
                    TextButton(onClick = onCargar, modifier = Modifier.padding(16.dp)) { Text("Reintentar") }
                }
                lista.isEmpty() -> item {
                    Text(
                        "No hay gasolineras con precio de ${tipo.nombre} a menos de ${radio.toInt()} km." +
                            if (radio < RADIOS.last()) " Prueba con un radio mayor." else "",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                else -> {
                    item { TarjetaResumenGasolina(lista, tipo, radio) }
                    items(lista.take(50), key = { it.estacion.i }) { g ->
                        FilaGasolinera(g, esLaMasBarata = g === lista.first()) {
                            val etiqueta = Uri.encode(g.estacion.n)
                            val uri = Uri.parse("geo:${g.estacion.a},${g.estacion.o}?q=${g.estacion.a},${g.estacion.o}($etiqueta)")
                            runCatching { contexto.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    }
                    if (lista.size > 50) {
                        item {
                            Text(
                                "Se muestran las 50 más baratas de ${lista.size}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp, 8.dp),
                            )
                        }
                    }
                }
            }
            item {
                val fecha = datos?.generado?.let(::fechaCorta).orEmpty()
                Text(
                    "Fuente: Comisión Nacional de Energía (antes CRE), precios vigentes que reporta cada gasolinera" +
                        (if (fecha.isNotBlank()) ", revisados el $fecha" else "") +
                        ". El precio en la bomba puede haber cambiado. Tu ubicación solo se usa en tu teléfono.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun TarjetaResumenGasolina(lista: List<GasolineraCercana>, tipo: Combustible, radio: Double) {
    val r = resumenGasolina(lista) ?: return
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("La ${tipo.nombre} más barata a ${radio.toInt()} km", style = MaterialTheme.typography.labelLarge)
            Text("${pesos(r.masBarata.precio)}/L", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
            Text(
                "${r.masBarata.estacion.n} · a ${distancia(r.masBarata.km)}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Promedio cerca: ${pesos(r.promedio)}/L · ${lista.size} gasolineras",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (r.ahorroTanque >= 1) {
                Text(
                    "Ahorras ~${pesosRedondo(r.ahorroTanque)} en un tanque de ${LITROS_TANQUE.toInt()} L contra la más cara (${pesos(r.masCara.precio)}).",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun FilaGasolinera(g: GasolineraCercana, esLaMasBarata: Boolean, onIr: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onIr).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("⛽", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(g.estacion.n, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "a ${distancia(g.km)}" + if (esLaMasBarata) " · la más barata" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(pesos(g.precio), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            AssistChip(onClick = onIr, label = { Text("Ir") }, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
