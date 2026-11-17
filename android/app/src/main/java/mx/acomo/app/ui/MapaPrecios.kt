package mx.acomo.app.ui

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import mx.acomo.app.data.Indice
import mx.acomo.app.data.MapaEstados
import mx.acomo.app.data.Mercado
import mx.acomo.app.data.NivelPrecio
import mx.acomo.app.data.PrecioEstado
import mx.acomo.app.data.PreciosPais
import mx.acomo.app.data.estadoEn
import mx.acomo.app.data.pesos
import mx.acomo.app.data.porcentaje
import mx.acomo.app.data.preciosPorEstado
import mx.acomo.app.ui.theme.fondoOscuro
import kotlin.math.roundToInt

/**
 * Escala divergente (azul = más barato, rojo = más caro, gris = como en el país), de ColorBrewer RdBu:
 * se distingue con cualquier tipo de daltonismo, a diferencia de verde contra rojo.
 */
private object ColoresMapa {
    private val claro = listOf(0xFF2166AC, 0xFF92C5DE, 0xFFE4E2DC, 0xFFF4A582, 0xFFB2182B)
    private val oscuro = listOf(0xFF4393C3, 0xFF92C5DE, 0xFF57564F, 0xFFF4A582, 0xFFD6604D)

    fun de(nivel: NivelPrecio, oscuro: Boolean): Color =
        Color((if (oscuro) this.oscuro else claro)[nivel.ordinal])
}

private var mapaEnMemoria: MapaEstados? = null

private suspend fun cargarMapa(contexto: Context): MapaEstados = mapaEnMemoria ?: withContext(Dispatchers.IO) {
    val texto = contexto.assets.open("estados.json").bufferedReader().use { it.readText() }
    Json { ignoreUnknownKeys = true }.decodeFromString<MapaEstados>(texto)
}.also { mapaEnMemoria = it }

/** Mapa de México con el precio del producto por estado. No se muestra si hay muy pocos mercados. */
@Composable
fun TarjetaMapa(indice: Indice, productoId: String, mercado: Mercado) {
    val pais = remember(indice, productoId) { indice.preciosPorEstado(productoId) } ?: return
    val contexto = LocalContext.current
    val mapa by produceState(mapaEnMemoria, contexto) { if (value == null) value = runCatching { cargarMapa(contexto) }.getOrNull() }
    val m = mapa ?: return
    var elegido by rememberSaveable(productoId) { mutableStateOf(mercado.estado) }
    var verLista by rememberSaveable(productoId) { mutableStateOf(false) }
    val oscuro = fondoOscuro()

    Tarjeta {
        Text("En el país", style = MaterialTheme.typography.titleSmall)
        Text(
            "Precio típico: ${pesos(pais.tipico)}/kg (mediana de las centrales de abasto). Toca un estado.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MapaMexico(m, pais, elegido, oscuro, Modifier.padding(top = 12.dp)) { elegido = it }
        val conCentral = remember(indice) { indice.mercados.map { it.estado }.toSet() }
        DetalleEstado(pais, elegido, elegido in conCentral)
        Leyenda(oscuro)
        TextButton(onClick = { verLista = !verLista }, contentPadding = PaddingValues(0.dp)) {
            Text(if (verLista) "Ocultar lista" else "Ver todos los estados")
        }
        if (verLista) ListaEstados(pais, mercado.estado)
    }
}

@Composable
private fun MapaMexico(
    mapa: MapaEstados,
    pais: PreciosPais,
    elegido: String?,
    oscuro: Boolean,
    modifier: Modifier,
    onElegir: (String) -> Unit,
) {
    val trazos = remember(mapa) {
        mapa.estados.associate { e ->
            e.nombre to Path().apply {
                e.anillos.forEach { a ->
                    moveTo(a[0], a[1])
                    for (i in 2 until a.size step 2) lineTo(a[i], a[i + 1])
                    close()
                }
            }
        }
    }
    val separador = MaterialTheme.colorScheme.surfaceContainer
    val sinDatos = MaterialTheme.colorScheme.outline
    val resalte = MaterialTheme.colorScheme.onSurface
    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f / mapa.alto)
            .pointerInput(mapa) {
                detectTapGestures { p ->
                    mapa.estadoEn(p.x / size.width, p.y / size.width)?.let(onElegir)
                }
            },
    ) {
        val w = size.width
        scale(w, w, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            val linea = 1.dp.toPx() / w
            trazos.forEach { (nombre, trazo) ->
                val dato = pais.estados[nombre]
                if (dato != null) {
                    drawPath(trazo, ColoresMapa.de(pais.nivel(dato), oscuro), style = Fill)
                    drawPath(trazo, separador, style = Stroke(linea))
                } else {
                    // Sin central de abasto: solo el contorno, para no confundirlo con "como en el país".
                    drawPath(trazo, sinDatos, style = Stroke(linea))
                }
            }
            trazos[elegido]?.let { drawPath(it, resalte, style = Stroke(linea * 2.5f)) }
        }
    }
}

@Composable
private fun DetalleEstado(pais: PreciosPais, estado: String?, tieneCentral: Boolean) {
    val dato = estado?.let { pais.estados[it] }
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        if (estado == null) return@Column
        Text(estado, fontWeight = FontWeight.Bold)
        if (dato == null) {
            Text(
                if (tieneCentral) "Su central de abasto no ha reportado este producto en las últimas semanas."
                else "No tiene central de abasto en el SNIIM.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val d = pais.diferencia(dato)
            val comparacion = if (d.roundToInt() == 0) "igual que lo típico" else
                "${porcentaje(d).removePrefix("+").removePrefix("-")} ${if (d < 0) "más barato" else "más caro"} que lo típico"
            Text("${pesos(dato.precio)}/kg · $comparacion", style = MaterialTheme.typography.bodyMedium)
            val ciudades = dato.mercados.map { it.ciudad }.distinct()
            val n = dato.mercados.size
            Text(
                ciudades.joinToString(" · ") + when {
                    n == 1 -> ""
                    ciudades.size == 1 -> " ($n centrales, promedio)"
                    else -> " (promedio)"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Leyenda(oscuro: Boolean) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            NivelPrecio.entries.forEach { n ->
                Box(Modifier.size(width = 28.dp, height = 12.dp).background(ColoresMapa.de(n, oscuro), RoundedCornerShape(3.dp)))
            }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier.size(width = 28.dp, height = 12.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(3.dp)),
            )
            Text(
                " Sin datos",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(Modifier.width((28 * 5 + 3 * 4).dp)) {
            Text("Más barato", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text("Más caro", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** La misma información en texto: no depende de distinguir colores. */
@Composable
private fun ListaEstados(pais: PreciosPais, propio: String) {
    pais.estados.values.sortedBy { it.precio }.forEach { e: PrecioEstado ->
        val esPropio = e.estado == propio
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                e.estado,
                Modifier.weight(1f),
                fontWeight = if (esPropio) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                pais.nivel(e).texto,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(pesos(e.precio), fontWeight = if (esPropio) FontWeight.Bold else FontWeight.Normal)
        }
    }
}
