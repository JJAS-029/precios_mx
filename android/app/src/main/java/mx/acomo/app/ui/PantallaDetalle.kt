package mx.acomo.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import mx.acomo.app.data.fechaCorta
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import mx.acomo.app.data.Indice
import mx.acomo.app.data.Mercado
import mx.acomo.app.data.cercanos
import mx.acomo.app.data.Producto
import mx.acomo.app.data.Semaforo
import mx.acomo.app.data.etiquetaSemana
import mx.acomo.app.data.evaluarPrecio
import mx.acomo.app.data.nombreVisible
import mx.acomo.app.data.pesos
import mx.acomo.app.data.porcentaje
import mx.acomo.app.data.resumen
import mx.acomo.app.data.serie
import mx.acomo.app.ui.theme.ColoresSemaforo

private const val SEMANAS_GRAFICA = 12
private const val MERCADOS_COMPARACION = 4

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PantallaDetalle(
    indice: Indice,
    producto: Producto,
    mercado: Mercado,
    ultimoLugar: String,
    /** Fecha (AAAA-MM-DD) del dato más reciente del mercado. */
    fechaDatos: String,
    onGuardarPrecio: (precio: Double, mayoreo: Double, lugar: String) -> Unit,
    onVolver: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(producto.nombreVisible)
                        Text(
                            "${mercado.nombre}, ${mercado.ciudad}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val r = producto.resumen()
            if (r == null) {
                Text(
                    "No hay datos de este producto en ${mercado.ciudad}.",
                    modifier = Modifier.padding(16.dp),
                )
                return@Column
            }

            // ----- precio de la semana
            Tarjeta(destacada = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InsigniaProducto(producto, tamano = 56.dp, fondo = MaterialTheme.colorScheme.surfaceContainerLowest)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(producto.categoria, style = MaterialTheme.typography.labelLarge)
                        val fecha = fechaCorta(fechaDatos)
                        if (fecha.isNotBlank()) Text("Dato más reciente: $fecha", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Mayoreo, semana del ${etiquetaSemana(r.actual.semana)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        pesos(r.actual.frec),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                    )
                    Text(" /kg", modifier = Modifier.padding(bottom = 6.dp))
                    Spacer(Modifier.weight(1f))
                    Cambio(r.cambioSemanal, Modifier.padding(bottom = 8.dp))
                }
                if (r.actual.min != null && r.actual.max != null) {
                    Text("Rango de la semana: ${pesos(r.actual.min)} – ${pesos(r.actual.max)}")
                }
                Text(
                    "Promedio de 8 semanas: ${pesos(r.promedio8)} (${porcentaje(r.vsPromedio)})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ----- semáforo
            TarjetaSemaforo(producto, r.actual.frec, ultimoLugar) { precio, lugar ->
                onGuardarPrecio(precio, r.actual.frec, lugar)
            }

            // ----- gráfica
            Tarjeta {
                Text("Últimas $SEMANAS_GRAFICA semanas", style = MaterialTheme.typography.titleSmall)
                GraficaPrecios(producto.serie().takeLast(SEMANAS_GRAFICA), Modifier.padding(top = 8.dp))
            }

            // ----- comparación entre mercados
            Tarjeta {
                Text("En mercados cercanos", style = MaterialTheme.typography.titleSmall)
                val ultimos = indice.ultimos[producto.id].orEmpty()
                val precios = indice.cercanos(mercado, MERCADOS_COMPARACION + 4)
                    .mapNotNull { m -> ultimos[m.id]?.let { m to it } }
                    .take(MERCADOS_COMPARACION)
                val masBarato = precios.minOfOrNull { it.second }
                precios.forEach { (m, frec) ->
                    val actual = m.id == mercado.id
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        Text(
                            m.ciudad + if (frec == masBarato && precios.size > 1) "  · más barato" else "",
                            fontWeight = if (actual) FontWeight.Bold else FontWeight.Normal,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(pesos(frec), fontWeight = if (actual) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            Text(
                "Fuente: SNIIM, Secretaría de Economía. Precio de mayoreo en central de abasto, " +
                    "calculado por kilogramo. En mercados y tianguis es normal pagar más por el transporte y la venta al menudeo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun TarjetaSemaforo(
    producto: Producto,
    mayoreo: Double,
    ultimoLugar: String,
    onGuardar: (precio: Double, lugar: String) -> Unit,
) {
    var texto by rememberSaveable(producto.id) { mutableStateOf("") }
    var lugar by rememberSaveable(producto.id) { mutableStateOf(ultimoLugar) }
    var guardado by rememberSaveable(producto.id) { mutableStateOf<String?>(null) }
    val precio = texto.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }

    Tarjeta {
        Text("¿Cuánto te cobran por kilo?", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = texto,
            onValueChange = { nuevo -> texto = nuevo.filter { it.isDigit() || it == '.' || it == ',' }.take(7) },
            prefix = { Text("$") },
            suffix = { Text("/kg") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        if (precio != null) {
            val ev = evaluarPrecio(precio, mayoreo, producto.categoria)
            val (color, titulo, detalle) = when (ev.semaforo) {
                Semaforo.VERDE -> Triple(ColoresSemaforo.verde, "Buen precio", "Está dentro de lo razonable.")
                Semaforo.AMARILLO -> Triple(ColoresSemaforo.amarillo, "Un poco caro", "Está ${porcentaje(ev.diferencia)} arriba de la referencia.")
                Semaforo.ROJO -> Triple(ColoresSemaforo.rojo, "Caro", "Está ${porcentaje(ev.diferencia)} arriba de la referencia. Compara en otro puesto.")
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = color),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                val textoSemaforo = if (ev.semaforo == Semaforo.AMARILLO) Color(0xFF2B1D00) else Color.White
                Column(Modifier.padding(12.dp)) {
                    Text(titulo, color = textoSemaforo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(detalle, color = textoSemaforo)
                }
            }
            Text(
                "Referencia al menudeo: hasta ${pesos(ev.referencia)}/kg (mayoreo ${pesos(mayoreo)} más el margen típico de venta).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            // ----- guardar en "Mis precios"
            OutlinedTextField(
                value = lugar,
                onValueChange = { lugar = it.take(60) },
                label = { Text("¿Dónde? (opcional)") },
                placeholder = { Text("Tianguis, mercado, recaudería…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            val claveActual = "$texto|$lugar"
            if (guardado == claveActual) {
                Text(
                    "✓ Guardado en Mis precios",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp),
                )
            } else {
                OutlinedButton(
                    shapes = ButtonDefaults.shapes(),
                    onClick = {
                        onGuardar(precio, lugar)
                        guardado = claveActual
                    },
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text("Guardar en Mis precios") }
            }
        }
    }
}

@Composable
private fun Tarjeta(destacada: Boolean = false, content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (destacada) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
            contentColor = if (destacada) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Column(Modifier.padding(20.dp)) { content() }
    }
}
