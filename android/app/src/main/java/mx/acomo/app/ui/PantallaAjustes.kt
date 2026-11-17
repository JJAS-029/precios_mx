package mx.acomo.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import mx.acomo.app.AvisosPrecios

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mx.acomo.app.BuildConfig
import mx.acomo.app.ui.theme.EstiloColor
import mx.acomo.app.ui.theme.ModoTema

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PantallaAjustes(
    estilo: EstiloColor,
    modo: ModoTema,
    plus: Boolean,
    avisos: Boolean,
    /** Productos seguidos: id -> nombre. */
    seguidos: Map<String, String>,
    onAvisos: (Boolean) -> Unit,
    onDejarDeSeguir: (String) -> Unit,
    onEstilo: (EstiloColor) -> Unit,
    onModo: (ModoTema) -> Unit,
    onCambiarPlus: (Boolean) -> Unit,
    onVolver: () -> Unit,
) {
    var aviso by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Text("←", style = MaterialTheme.typography.titleLarge) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TarjetaPlus(plus, onObtener = {
                aviso = "Muy pronto: Plus se podrá comprar cuando la app esté en Google Play."
            })
            aviso?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }

            Titulo("Modo")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ModoTema.entries.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = m == modo,
                        onClick = { onModo(m) },
                        shape = SegmentedButtonDefaults.itemShape(i, ModoTema.entries.size),
                    ) { Text(m.nombre, maxLines = 1) }
                }
            }

            Titulo("Color")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = MaterialTheme.shapes.large,
            ) {
                EstiloColor.entries
                    .filter { it != EstiloColor.DINAMICO || EstiloColor.dinamicoDisponible }
                    .forEach { e ->
                        val bloqueado = e.plus && !plus
                        val efectivo = if (estilo.plus && !plus) EstiloColor.VERDE else estilo
                        val elegido = e == efectivo
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable {
                                    if (bloqueado) aviso = "\"${e.nombre}\" es parte de ¿A cómo? Plus."
                                    else { aviso = null; onEstilo(e) }
                                }
                                .padding(16.dp, 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(36.dp)
                                    .clip(if (elegido) MaterialShapes.Cookie9Sided.toShape() else CircleShape)
                                    .background(e.muestra)
                                    .then(if (elegido) Modifier else Modifier.border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (elegido) Text("✓", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(16.dp))
                            Text(
                                e.nombre,
                                Modifier.weight(1f),
                                fontWeight = if (elegido) FontWeight.Bold else FontWeight.Normal,
                                color = if (bloqueado) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            )
                            if (bloqueado) {
                                Text("🔒 Plus", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
            }

            Titulo("Avisos de precios bajos")
            SeccionAvisos(avisos, seguidos, onAvisos, onDejarDeSeguir)

            if (BuildConfig.DEBUG) {
                Titulo("Pruebas (solo en versión de desarrollo)")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Activar Plus de prueba", Modifier.weight(1f))
                    Switch(checked = plus, onCheckedChange = onCambiarPlus)
                }
                val ctx = LocalContext.current
                androidx.compose.material3.OutlinedButton(onClick = { AvisosPrecios.probarAhora(ctx) }) {
                    Text("Probar aviso ahora")
                }
            }

            Titulo("Acerca de")
            Text(
                "Precios de mayoreo del SNIIM (Secretaría de Economía), actualizados cada semana. " +
                    "El precio justo de menudeo es un estimado.\n\nVersión ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TarjetaPlus(plus: Boolean, onObtener: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("¿A cómo? Plus", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            if (plus) {
                Text("✓ Tienes Plus. ¡Gracias por apoyar la app!")
            } else {
                Text("• Sin anuncios\n• 4 temas de color extra\n• Colores de tu fondo de pantalla (Android 12+)")
                Spacer(Modifier.height(12.dp))
                Button(onClick = onObtener, shapes = ButtonDefaults.shapes()) { Text("Obtener Plus") }
            }
        }
    }
}

@Composable
private fun Titulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun SeccionAvisos(
    avisos: Boolean,
    seguidos: Map<String, String>,
    onAvisos: (Boolean) -> Unit,
    onDejarDeSeguir: (String) -> Unit,
) {
    val contexto = LocalContext.current
    var permiso by remember { mutableStateOf(AvisosPrecios.permisoConcedido(contexto)) }
    // El aviso de "desactivadas" sale si ya estaba negado o después de que el usuario lo niega,
    // no mientras el diálogo del sistema está abierto.
    var mostrarNegado by remember { mutableStateOf(!permiso) }
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permiso = it
        mostrarNegado = !it
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Avisarme cuando bajen")
                    Text(
                        "Se revisa cada semana, cuando salen precios nuevos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = avisos,
                    onCheckedChange = { activo ->
                        onAvisos(activo)
                        if (activo && !permiso && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            mostrarNegado = false
                            pedir.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
            }
            if (avisos && !permiso && seguidos.isNotEmpty() && mostrarNegado) {
                Text(
                    "Las notificaciones están desactivadas en el teléfono.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            if (seguidos.isEmpty()) {
                Text(
                    "Aún no sigues ningún producto. Ábrelo y activa \"Avisarme cuando baje\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                seguidos.entries.sortedBy { it.value }.forEach { (id, nombre) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔔 $nombre", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { onDejarDeSeguir(id) }) { Text("Quitar") }
                    }
                }
            }
        }
    }
}
