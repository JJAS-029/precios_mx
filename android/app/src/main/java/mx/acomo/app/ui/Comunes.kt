package mx.acomo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.acomo.app.data.DIAS_DESACTUALIZADO
import mx.acomo.app.data.Mercado
import mx.acomo.app.data.Producto
import mx.acomo.app.data.diasDesde
import mx.acomo.app.data.emoji
import mx.acomo.app.data.fechaCorta
import mx.acomo.app.data.porcentaje
import mx.acomo.app.ui.theme.ColorBaja
import mx.acomo.app.ui.theme.ColorSube
import kotlin.math.abs

/** Tarjeta con el mercado elegido y la fecha de su último dato; al tocarla se abre el selector. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BotonMercado(mercado: Mercado?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(MaterialShapes.Cookie9Sided.toShape())
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) { Text("📍", fontSize = 20.sp) }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(
                    mercado?.ciudad ?: "Elige tu mercado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                if (mercado != null) {
                    Text(
                        "${mercado.nombre} · ${mercado.estado}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            Text("Cambiar", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * "Último dato del SNIIM: jue 13 nov · revisado el lun 16 nov".
 * Si el mercado lleva más de [DIAS_DESACTUALIZADO] días sin datos, lo dice en color de error.
 */
@Composable
fun LineaActualizacion(ultimaFecha: String, revisado: String?, modifier: Modifier = Modifier) {
    val dias = diasDesde(ultimaFecha)
    val viejo = dias != null && dias > DIAS_DESACTUALIZADO
    val texto = buildString {
        if (ultimaFecha.isNotBlank()) {
            append(if (viejo) "⚠ Este mercado no publica precios desde el ${fechaCorta(ultimaFecha)}" else "Último dato del SNIIM: ${fechaCorta(ultimaFecha)}")
        }
        if (!revisado.isNullOrBlank() && !viejo) append(" · revisado el ${fechaCorta(revisado)}")
    }
    if (texto.isBlank()) return
    Text(
        texto,
        style = MaterialTheme.typography.labelMedium,
        color = if (viejo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** Emoji del producto dentro de una forma expresiva (galleta, trébol, flor…). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InsigniaProducto(
    producto: Producto,
    tamano: Dp = 44.dp,
    fondo: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
) {
    val forma = when (abs(producto.id.hashCode()) % 4) {
        0 -> MaterialShapes.Cookie6Sided
        1 -> MaterialShapes.Clover4Leaf
        2 -> MaterialShapes.Sunny
        else -> MaterialShapes.Cookie4Sided
    }
    Box(
        Modifier.size(tamano).clip(forma.toShape()).background(fondo),
        contentAlignment = Alignment.Center,
    ) { Text(producto.emoji, fontSize = (tamano.value * 0.48f).sp) }
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
