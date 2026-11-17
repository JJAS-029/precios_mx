package mx.acomo.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mx.acomo.app.data.PrecioSemana
import mx.acomo.app.data.Producto
import mx.acomo.app.data.etiquetaCorta
import mx.acomo.app.data.etiquetaSemana
import mx.acomo.app.data.lunesDeSemana
import mx.acomo.app.data.pesos
import mx.acomo.app.data.porcentaje
import mx.acomo.app.data.serieUltimas
import kotlin.math.roundToInt

/** Periodos de la gráfica, en semanas. */
enum class Periodo(val etiqueta: String, val semanas: Int, val texto: String) {
    MES("1 mes", 5, "el último mes"),
    TRIMESTRE("3 meses", 13, "los últimos 3 meses"),
    SEMESTRE("6 meses", 26, "los últimos 6 meses"),
    ANIO("1 año", 52, "el último año"),
}

/** Tarjeta con selector de periodo, resumen de la tendencia y la gráfica. */
@Composable
fun TarjetaGrafica(producto: Producto, mercado: String) {
    var periodo by rememberSaveable(producto.id) { mutableStateOf(Periodo.TRIMESTRE) }
    val serie = remember(producto, periodo) { producto.serieUltimas(periodo.semanas) }
    Tarjeta {
        Text("¿Cómo ha cambiado en $mercado?", style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Periodo.entries.forEachIndexed { i, p ->
                SegmentedButton(
                    selected = p == periodo,
                    onClick = { periodo = p },
                    shape = SegmentedButtonDefaults.itemShape(i, Periodo.entries.size),
                    icon = {},
                ) { Text(p.etiqueta, maxLines = 1, style = MaterialTheme.typography.labelMedium) }
            }
        }
        if (serie.size >= 2) {
            val cambio = (serie.last().frec / serie.first().frec - 1) * 100
            val verbo = when {
                cambio.roundToInt() == 0 -> "se mantuvo"
                cambio > 0 -> "subió ${porcentaje(cambio).removePrefix("+")}"
                else -> "bajó ${porcentaje(cambio).removePrefix("-")}"
            }
            Text(
                "En ${periodo.texto} $verbo: de ${pesos(serie.first().frec)} a ${pesos(serie.last().frec)}/kg.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp),
            )
            if (periodo == Periodo.ANIO) {
                Text(
                    "En un año, la inflación general sube los precios alrededor de 4 %.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        GraficaPrecios(serie, Modifier.padding(top = 8.dp))
    }
}

/**
 * Línea del precio frecuente semanal, con una banda sombreada del rango mínimo–máximo.
 * Tocar o deslizar sobre la gráfica muestra el precio de esa semana.
 */
@Composable
fun GraficaPrecios(serie: List<PrecioSemana>, modifier: Modifier = Modifier) {
    if (serie.size < 2) {
        Text(
            "Aún no hay suficientes semanas para graficar.",
            style = MaterialTheme.typography.bodySmall,
            modifier = modifier,
        )
        return
    }
    val bajos = serie.map { it.min ?: it.frec }
    val altos = serie.map { it.max ?: it.frec }
    val yMin = bajos.min()
    val yMax = altos.max().let { if (it - yMin < 1.0) yMin + 1.0 else it }
    var elegido by remember(serie) { mutableStateOf<Int?>(null) }

    val linea = MaterialTheme.colorScheme.primary
    val banda = linea.copy(alpha = 0.15f)
    val ejes = MaterialTheme.colorScheme.outlineVariant
    val guia = MaterialTheme.colorScheme.onSurfaceVariant
    val anillo = MaterialTheme.colorScheme.surfaceContainer

    Column(modifier) {
        // Lectura de la semana tocada (o de la última).
        val i = elegido ?: serie.lastIndex
        val s = serie[i]
        val rango = if (s.min != null && s.max != null && s.min != s.max) " · rango ${pesos(s.min)}–${pesos(s.max)}" else ""
        Text(
            "Semana del ${etiquetaSemana(s.semana)}: ${pesos(s.frec)}/kg$rango",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(pesos(yMax), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .pointerInput(serie) {
                    fun indice(x: Float) = ((x / size.width) * (serie.size - 1)).roundToInt().coerceIn(0, serie.lastIndex)
                    detectTapGestures { elegido = indice(it.x) }
                }
                .pointerInput(serie) {
                    fun indice(x: Float) = ((x / size.width) * (serie.size - 1)).roundToInt().coerceIn(0, serie.lastIndex)
                    detectDragGestures(onDragEnd = {}) { cambio, _ -> elegido = indice(cambio.position.x) }
                },
        ) {
            val paso = size.width / (serie.size - 1)
            fun x(i: Int) = i * paso
            fun y(v: Double) = (size.height * (1 - (v - yMin) / (yMax - yMin))).toFloat()

            drawLine(ejes, Offset(0f, size.height), Offset(size.width, size.height))
            drawLine(ejes, Offset(0f, 0f), Offset(size.width, 0f))

            val rangoPath = Path().apply {
                moveTo(x(0), y(altos[0]))
                for (k in 1 until serie.size) lineTo(x(k), y(altos[k]))
                for (k in serie.indices.reversed()) lineTo(x(k), y(bajos[k]))
                close()
            }
            drawPath(rangoPath, banda)

            val frec = Path().apply {
                moveTo(x(0), y(serie[0].frec))
                for (k in 1 until serie.size) lineTo(x(k), y(serie[k].frec))
            }
            val grosor = if (serie.size > 30) 2.dp else 3.dp
            drawPath(frec, linea, style = Stroke(width = grosor.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

            val punto = Offset(x(i), y(serie[i].frec))
            if (elegido != null) drawLine(guia, Offset(punto.x, 0f), Offset(punto.x, size.height), strokeWidth = 1.dp.toPx())
            drawCircle(anillo, radius = 7.dp.toPx(), center = punto)
            drawCircle(linea, radius = 5.dp.toPx(), center = punto)
        }
        Text(pesos(yMin), style = MaterialTheme.typography.labelSmall)
        // Con el año si el periodo cruza de un año a otro ("17 nov 2025" … "9 nov 2026").
        val anio = { s: String -> lunesDeSemana(s).year }
        val conAnio = anio(serie.first().semana) != anio(serie.last().semana)
        fun etiqueta(s: String) = etiquetaCorta(s) + if (conAnio) " ${anio(s)}" else ""
        Row(Modifier.fillMaxWidth()) {
            Text(etiqueta(serie.first().semana), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.weight(1f))
            Text(etiqueta(serie.last().semana), style = MaterialTheme.typography.labelSmall)
        }
    }
}
