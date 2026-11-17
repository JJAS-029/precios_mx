package mx.acomo.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mx.acomo.app.data.MESES_LARGOS
import mx.acomo.app.data.NivelPrecio
import mx.acomo.app.data.Producto
import mx.acomo.app.data.mesActual
import mx.acomo.app.data.mesesBaratos
import mx.acomo.app.data.nombreVisible
import mx.acomo.app.data.pesos
import mx.acomo.app.data.rangoMeses
import mx.acomo.app.data.resumen
import mx.acomo.app.ui.theme.fondoOscuro
import kotlin.math.abs
import kotlin.math.roundToInt

private val INICIALES = listOf("E", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")

/** "18 % más barato" / "6 % más caro" / "su precio normal". */
private fun comparacionMes(indice: Double): String {
    val d = ((indice - 1) * 100).roundToInt()
    return when {
        d == 0 -> "a su precio normal"
        d < 0 -> "${abs(d)}% más barato que en el resto del año"
        else -> "$d% más caro que en el resto del año"
    }
}

/** Detalle: en qué meses suele estar barato y cómo está este mes. */
@Composable
fun TarjetaTemporada(indice: List<Double?>, aniosDeHistoria: String) {
    val mes = mesActual()
    val baratos = mesesBaratos(indice)
    Tarjeta {
        Text("Temporada", style = MaterialTheme.typography.titleSmall)
        Text(
            if (baratos.isEmpty()) "Su precio cambia poco durante el año."
            else "Suele estar más barato en ${rangoMeses(baratos)}.",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp),
        )
        indice[mes]?.let {
            Text(
                "En ${MESES_LARGOS[mes]} suele estar ${comparacionMes(it)}.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        BarrasMeses(indice, mes, Modifier.padding(top = 12.dp))
        Text(
            "Barras hacia abajo: meses en que suele estar más barato. Con $aniosDeHistoria de precios del SNIIM.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** 12 barras divergentes desde la línea del precio normal; el mes actual va resaltado. */
@Composable
private fun BarrasMeses(indice: List<Double?>, mes: Int, modifier: Modifier) {
    val oscuro = fondoOscuro()
    val linea = MaterialTheme.colorScheme.outline
    val sinDato = MaterialTheme.colorScheme.outlineVariant
    val resalte = MaterialTheme.colorScheme.onSurface
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(96.dp)) {
            val ancho = size.width / 12
            val barra = ancho * 0.6f
            val medio = size.height / 2
            val tope = 0.30 // ±30 % llena media altura
            indice.forEachIndexed { i, v ->
                val x = i * ancho + (ancho - barra) / 2
                if (v == null) {
                    drawCircle(sinDato, radius = 2.dp.toPx(), center = Offset(x + barra / 2, medio))
                    return@forEachIndexed
                }
                val d = (v - 1).coerceIn(-tope, tope)
                val alto = maxOf((abs(d) / tope * (medio - 4.dp.toPx())).toFloat(), 2.dp.toPx())
                val y = if (d < 0) medio else medio - alto
                val color = ColoresMapa.de(NivelPrecio.de(d * 100), oscuro)
                drawRoundRect(color, Offset(x, y), Size(barra, alto), CornerRadius(3.dp.toPx()))
                if (i == mes) {
                    drawRoundRect(
                        resalte, Offset(x - 2.dp.toPx(), y - 2.dp.toPx()),
                        Size(barra + 4.dp.toPx(), alto + 4.dp.toPx()), CornerRadius(4.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()),
                    )
                }
            }
            drawLine(linea, Offset(0f, medio), Offset(size.width, medio), strokeWidth = 1.dp.toPx())
        }
        Row(Modifier.fillMaxWidth()) {
            INICIALES.forEachIndexed { i, letra ->
                Text(
                    letra,
                    Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (i == mes) FontWeight.Black else FontWeight.Normal,
                    color = if (i == mes) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Lista: "De temporada en noviembre", tarjetas horizontales. */
@Composable
fun SeccionTemporada(productos: List<Pair<Producto, Double>>, onAbrir: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(productos, key = { "t-" + it.first.id }) { (p, indice) ->
            Card(
                onClick = { onAbrir(p.id) },
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
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
                    )
                    p.resumen()?.let {
                        Text("${pesos(it.actual.frec)}/kg", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    }
                    Text(
                        "Suele bajar ${abs(((indice - 1) * 100).roundToInt())}% este mes",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
