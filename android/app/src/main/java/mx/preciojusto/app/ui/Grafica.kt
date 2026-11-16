package mx.preciojusto.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import mx.preciojusto.app.data.PrecioSemana
import mx.preciojusto.app.data.etiquetaCorta
import mx.preciojusto.app.data.pesos

/**
 * Línea del precio frecuente semanal, con una banda sombreada del rango mínimo–máximo.
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

    val linea = MaterialTheme.colorScheme.primary
    val banda = linea.copy(alpha = 0.15f)
    val ejes = MaterialTheme.colorScheme.outlineVariant

    Column(modifier) {
        Text(pesos(yMax), style = MaterialTheme.typography.labelSmall)
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            val paso = size.width / (serie.size - 1)
            fun x(i: Int) = i * paso
            fun y(v: Double) = (size.height * (1 - (v - yMin) / (yMax - yMin))).toFloat()

            drawLine(ejes, Offset(0f, size.height), Offset(size.width, size.height))
            drawLine(ejes, Offset(0f, 0f), Offset(size.width, 0f))

            val rango = Path().apply {
                moveTo(x(0), y(altos[0]))
                for (i in 1 until serie.size) lineTo(x(i), y(altos[i]))
                for (i in serie.indices.reversed()) lineTo(x(i), y(bajos[i]))
                close()
            }
            drawPath(rango, banda)

            val frec = Path().apply {
                moveTo(x(0), y(serie[0].frec))
                for (i in 1 until serie.size) lineTo(x(i), y(serie[i].frec))
            }
            drawPath(frec, linea, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawCircle(linea, radius = 5.dp.toPx(), center = Offset(x(serie.lastIndex), y(serie.last().frec)))
        }
        Text(pesos(yMin), style = MaterialTheme.typography.labelSmall)
        Row(Modifier.fillMaxWidth()) {
            Text(etiquetaCorta(serie.first().semana), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.weight(1f))
            Text(etiquetaCorta(serie.last().semana), style = MaterialTheme.typography.labelSmall)
        }
    }
}
