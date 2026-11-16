package mx.preciojusto.app.ui.theme

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
fun TemaPrecioJusto(content: @Composable () -> Unit) {
    val colores = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = VerdeClaro, secondary = VerdeClaro)
    } else {
        lightColorScheme(primary = VerdeHoja, secondary = VerdeHoja)
    }
    MaterialTheme(colorScheme = colores, content = content)
}
