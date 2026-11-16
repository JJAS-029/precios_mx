package mx.acomo.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mx.acomo.app.data.Alternativa
import mx.acomo.app.data.Producto
import mx.acomo.app.data.Resumen
import mx.acomo.app.data.SugerenciaCambio
import mx.acomo.app.data.UMBRAL_ALTERNATIVA
import mx.acomo.app.data.UMBRAL_CARO
import mx.acomo.app.data.nombreVisible
import mx.acomo.app.data.pesos
import mx.acomo.app.data.porcentaje

/** En el detalle: otros productos que sirven para lo mismo, del más barato al más caro. */
@Composable
fun TarjetaAlternativas(
    producto: Producto,
    resumen: Resumen,
    alternativas: List<Alternativa>,
    onAbrir: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (alternativas.isEmpty()) return
    val mejor = alternativas.first()
    val conviene = resumen.vsPromedio >= UMBRAL_CARO && mejor.resumen.vsPromedio <= UMBRAL_ALTERNATIVA
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Column(Modifier.padding(vertical = 16.dp)) {
            Text(
                if (conviene) "💡 Está caro, mejor cambia" else "También te sirve",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (conviene) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            if (conviene) {
                Text(
                    "${producto.nombreVisible} está ${porcentaje(resumen.vsPromedio)} sobre su promedio; " +
                        "${mejor.producto.nombreVisible.lowercase()} está ${porcentaje(mejor.resumen.vsPromedio)} del suyo.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            alternativas.forEach { a ->
                FilaAlternativa(a.producto, a.resumen, a.motivo) { onAbrir(a.producto.id) }
            }
            Text(
                "Comparado contra el promedio de 8 semanas de cada producto.",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
            )
        }
    }
}

@Composable
private fun FilaAlternativa(p: Producto, r: Resumen, motivo: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InsigniaProducto(p, tamano = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(p.nombreVisible, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(motivo, style = MaterialTheme.typography.labelMedium)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${pesos(r.actual.frec)}/kg", fontWeight = FontWeight.Bold)
            VsPromedio(r.vsPromedio)
        }
    }
}

/** En la lista: "Está caro → mejor compra…". */
@Composable
fun SeccionCambios(sugerencias: List<SugerenciaCambio>, onAbrir: (String) -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            sugerencias.forEach { s ->
                Row(
                    Modifier.fillMaxWidth().clickable { onAbrir(s.alternativa.producto.id) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(s.caro.nombreVisible, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        VsPromedio(s.resumenCaro.vsPromedio)
                    }
                    Text("→", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 8.dp))
                    InsigniaProducto(s.alternativa.producto, tamano = 36.dp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            s.alternativa.producto.nombreVisible,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        VsPromedio(s.alternativa.resumen.vsPromedio)
                    }
                }
            }
        }
    }
}

/** "▲ +25 % vs. promedio" en rojo o "▼ -30 % vs. promedio" en verde. */
@Composable
private fun VsPromedio(valor: Double) {
    val texto = when {
        valor >= 0.5 -> "▲ ${porcentaje(valor)} vs. promedio"
        valor <= -0.5 -> "▼ ${porcentaje(valor)} vs. promedio"
        else -> "= en su promedio"
    }
    val color = when {
        valor >= 0.5 -> mx.acomo.app.ui.theme.ColorSube
        valor <= -0.5 -> mx.acomo.app.ui.theme.ColorBaja
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(texto, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = color)
}
