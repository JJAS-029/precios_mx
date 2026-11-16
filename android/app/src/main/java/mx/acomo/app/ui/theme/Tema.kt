package mx.acomo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VerdeHoja = Color(0xFF2E7D32)
private val VerdeClaro = Color(0xFF81C784)

/** Colores del semáforo (se usan igual en tema claro y oscuro). */
object ColoresSemaforo {
    val verde = Color(0xFF2E7D32)
    val amarillo = Color(0xFFF9A825)
    val rojo = Color(0xFFC62828)
}

/** Subió de precio = malo para el comprador (rojo); bajó = bueno (verde). */
val ColorSube = Color(0xFFC62828)
val ColorBaja = Color(0xFF2E7D32)

@Composable
fun TemaAcomo(content: @Composable () -> Unit) {
    val colores = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = VerdeClaro,
            secondary = VerdeClaro,
            primaryContainer = Color(0xFF1B5E20),
            secondaryContainer = Color(0xFF2E4A30),
            onSecondaryContainer = Color(0xFFDCEFD9),
            background = Color(0xFF121512),
            surface = Color(0xFF121512),
            surfaceVariant = Color(0xFF2A2F2A),
            surfaceContainerHighest = Color(0xFF2A2F2A),
        )
    } else {
        lightColorScheme(
            primary = VerdeHoja,
            secondary = VerdeHoja,
            primaryContainer = Color(0xFFC8E6C9),
            secondaryContainer = Color(0xFFDCEFD9),
            onSecondaryContainer = Color(0xFF0D3B12),
            background = Color(0xFFFAFBF8),
            surface = Color(0xFFFAFBF8),
            surfaceVariant = Color(0xFFEEF2EC),
            surfaceContainerHighest = Color(0xFFEEF2EC),
        )
    }
    MaterialTheme(colorScheme = colores, content = content)
}
