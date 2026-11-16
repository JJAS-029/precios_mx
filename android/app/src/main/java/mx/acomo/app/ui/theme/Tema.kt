package mx.acomo.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Paleta tonal Material 3 generada a partir del verde de la marca (hojas del logo).
private val Claro = lightColorScheme(
    primary = Color(0xFF4C662B), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDEDA3), onPrimaryContainer = Color(0xFF354E16),
    secondary = Color(0xFF586249), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE7C8), onSecondaryContainer = Color(0xFF404A33),
    tertiary = Color(0xFF386663), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBCECE7), onTertiaryContainer = Color(0xFF1F4E4B),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF9FAEF), onBackground = Color(0xFF1A1C16),
    surface = Color(0xFFF9FAEF), onSurface = Color(0xFF1A1C16),
    surfaceVariant = Color(0xFFE1E4D5), onSurfaceVariant = Color(0xFF44483D),
    outline = Color(0xFF75796C), outlineVariant = Color(0xFFC5C8BA),
    inverseSurface = Color(0xFF2F312A), inverseOnSurface = Color(0xFFF1F2E6), inversePrimary = Color(0xFFB1D18A),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF3F4E9),
    surfaceContainer = Color(0xFFEEEFE3), surfaceContainerHigh = Color(0xFFE8E9DE),
    surfaceContainerHighest = Color(0xFFE2E3D8),
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFFB1D18A), onPrimary = Color(0xFF1F3701),
    primaryContainer = Color(0xFF354E16), onPrimaryContainer = Color(0xFFCDEDA3),
    secondary = Color(0xFFBFCBAD), onSecondary = Color(0xFF2A331E),
    secondaryContainer = Color(0xFF404A33), onSecondaryContainer = Color(0xFFDCE7C8),
    tertiary = Color(0xFFA0D0CB), onTertiary = Color(0xFF003735),
    tertiaryContainer = Color(0xFF1F4E4B), onTertiaryContainer = Color(0xFFBCECE7),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF12140E), onBackground = Color(0xFFE2E3D8),
    surface = Color(0xFF12140E), onSurface = Color(0xFFE2E3D8),
    surfaceVariant = Color(0xFF44483D), onSurfaceVariant = Color(0xFFC5C8BA),
    outline = Color(0xFF8F9285), outlineVariant = Color(0xFF44483D),
    inverseSurface = Color(0xFFE2E3D8), inverseOnSurface = Color(0xFF2F312A), inversePrimary = Color(0xFF4C662B),
    surfaceContainerLowest = Color(0xFF0C0F09), surfaceContainerLow = Color(0xFF1A1C16),
    surfaceContainer = Color(0xFF1E201A), surfaceContainerHigh = Color(0xFF282B24),
    surfaceContainerHighest = Color(0xFF33362E),
)

/** Esquinas más redondeadas, al estilo Expressive. */
private val Formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/** Colores del semáforo (fijos, no dependen del tema ni del fondo de pantalla). */
object ColoresSemaforo {
    val verde = Color(0xFF2E7D32)
    val amarillo = Color(0xFFF9A825)
    val rojo = Color(0xFFC62828)
}

/** Subió de precio = malo para el comprador (rojo); bajó = bueno (verde). */
val ColorSube: Color
    @Composable @ReadOnlyComposable get() = if (fondoOscuro()) Color(0xFFFF8A80) else Color(0xFFC62828)
val ColorBaja: Color
    @Composable @ReadOnlyComposable get() = if (fondoOscuro()) Color(0xFF81C784) else Color(0xFF2E7D32)

/** Si el tema activo es oscuro (sirve también cuando el usuario forzó claro u oscuro). */
@Composable
@ReadOnlyComposable
private fun fondoOscuro(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

/** Paletas de color. Solo [VERDE] es gratuita; las demás son parte de ¿A cómo? Plus. */
enum class EstiloColor(val nombre: String, val muestra: Color, val plus: Boolean) {
    VERDE("Koala (verde)", Color(0xFF4C662B), plus = false),
    JITOMATE("Jitomate", Color(0xFFC62828), plus = true),
    MANDARINA("Mandarina", Color(0xFFEF6C00), plus = true),
    JAMAICA("Jamaica", Color(0xFF8E2C6E), plus = true),
    MAIZ_AZUL("Maíz azul", Color(0xFF3949AB), plus = true),
    /** Material You: toma los colores del fondo de pantalla (Android 12+). */
    DINAMICO("Colores de tu fondo de pantalla", Color(0xFF6750A4), plus = true);

    companion object {
        val dinamicoDisponible: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        fun desde(nombre: String?): EstiloColor = entries.find { it.name == nombre } ?: VERDE
    }
}

/** Claro u oscuro. Gratis para todos (es también una cuestión de accesibilidad). */
enum class ModoTema(val nombre: String) {
    SISTEMA("Sistema"), CLARO("Claro"), OSCURO("Oscuro");

    companion object {
        fun desde(nombre: String?): ModoTema = entries.find { it.name == nombre } ?: SISTEMA
    }
}

/** Tema Material 3 Expressive. Si el estilo pide Plus y no hay Plus, se usa el verde de la marca. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TemaAcomo(
    estilo: EstiloColor = EstiloColor.VERDE,
    modo: ModoTema = ModoTema.SISTEMA,
    plus: Boolean = false,
    content: @Composable () -> Unit,
) {
    val oscuro = when (modo) {
        ModoTema.SISTEMA -> isSystemInDarkTheme()
        ModoTema.CLARO -> false
        ModoTema.OSCURO -> true
    }
    val contexto = LocalContext.current
    val efectivo = if (estilo.plus && !plus) EstiloColor.VERDE else estilo
    val colores = when (efectivo) {
        EstiloColor.DINAMICO ->
            if (!EstiloColor.dinamicoDisponible) { if (oscuro) Oscuro else Claro }
            else if (oscuro) dynamicDarkColorScheme(contexto) else dynamicLightColorScheme(contexto)
        EstiloColor.JITOMATE -> if (oscuro) JitomateOscuro else JitomateClaro
        EstiloColor.MANDARINA -> if (oscuro) MandarinaOscuro else MandarinaClaro
        EstiloColor.JAMAICA -> if (oscuro) JamaicaOscuro else JamaicaClaro
        EstiloColor.MAIZ_AZUL -> if (oscuro) MaizAzulOscuro else MaizAzulClaro
        EstiloColor.VERDE -> if (oscuro) Oscuro else Claro
    }
    MaterialExpressiveTheme(
        colorScheme = colores,
        motionScheme = MotionScheme.expressive(),
        shapes = Formas,
        content = content,
    )
}
