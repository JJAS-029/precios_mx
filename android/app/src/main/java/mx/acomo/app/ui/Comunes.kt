package mx.acomo.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mx.acomo.app.data.Mercado
import mx.acomo.app.data.porcentaje
import mx.acomo.app.ui.theme.ColorBaja
import mx.acomo.app.ui.theme.ColorSube
import kotlin.math.abs

/** Tarjeta con el mercado elegido; al tocarla se abre el selector de mercados. */
@Composable
fun BotonMercado(mercado: Mercado?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(onClick = onClick, modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("📍", modifier = Modifier.padding(end = 12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    mercado?.ciudad ?: "Elige tu mercado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (mercado != null) {
                    Text(
                        "${mercado.nombre} · ${mercado.estado}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text("Cambiar", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
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
