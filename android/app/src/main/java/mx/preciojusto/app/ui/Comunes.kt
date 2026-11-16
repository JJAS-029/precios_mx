package mx.preciojusto.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mx.preciojusto.app.data.porcentaje
import mx.preciojusto.app.ui.theme.ColorBaja
import mx.preciojusto.app.ui.theme.ColorSube
import kotlin.math.abs

/** "Central de Abasto de Toluca" -> "Toluca". */
fun nombreCorto(mercado: String): String =
    mercado.removePrefix("Central de Abasto de ").removePrefix("Central de Abasto ")

@Composable
fun SelectorMercado(
    mercados: Map<String, String>,
    seleccionado: String,
    onElegir: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        mercados.forEach { (id, nombre) ->
            FilterChip(
                selected = id == seleccionado,
                onClick = { onElegir(id) },
                label = { Text(nombreCorto(nombre)) },
            )
        }
    }
}

/** Flecha y porcentaje de cambio; rojo si sube, verde si baja. */
@Composable
fun Cambio(valor: Double?, modifier: Modifier = Modifier) {
    if (valor == null) return
    val (flecha, color) = when {
        abs(valor) < 0.5 -> "=" to MaterialTheme.colorScheme.onSurfaceVariant
        valor > 0 -> "▲" to ColorSube
        else -> "▼" to ColorBaja
    }
    Text(
        text = if (flecha == "=") "= sin cambio" else "$flecha ${porcentaje(valor)}",
        color = color,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier,
    )
}
