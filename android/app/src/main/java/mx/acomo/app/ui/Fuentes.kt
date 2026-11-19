package mx.acomo.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp

/**
 * Fuentes oficiales de los datos y la renuncia de responsabilidad. Google Play exige que una app con
 * información gubernamental enlace sus fuentes originales y diga que no representa a ninguna entidad pública.
 */
object Fuentes {
    const val SNIIM = "https://www.economia-sniim.gob.mx"
    const val CNE = "https://www.gob.mx/cne"
    const val CNE_PRECIOS = "https://publicacionexterna.azurewebsites.net/publicaciones/prices"

    const val RENUNCIA =
        "¿A cómo? es una app independiente: no representa a ninguna entidad pública ni está afiliada al " +
            "Gobierno de México, la Secretaría de Economía ni la Comisión Nacional de Energía."
}

/** Texto con uno o más enlaces que se abren en el navegador. [partes]: texto normal o (texto, url). */
@Composable
fun TextoConEnlaces(vararg partes: Any, estilo: TextStyle = MaterialTheme.typography.bodySmall, modifier: Modifier = Modifier) {
    val enlace = TextLinkStyles(
        SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium, textDecoration = TextDecoration.Underline),
    )
    Text(
        buildAnnotatedString {
            partes.forEach { p ->
                when (p) {
                    is Pair<*, *> -> withLink(LinkAnnotation.Url(p.second as String, enlace)) { append(p.first as String) }
                    else -> append(p.toString())
                }
            }
        },
        style = estilo,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** Pie corto con la fuente del SNIIM enlazada. */
@Composable
fun FuenteSniim(detalle: String, modifier: Modifier = Modifier) {
    TextoConEnlaces(
        "Fuente: SNIIM, Secretaría de Economía (",
        "economia-sniim.gob.mx" to Fuentes.SNIIM,
        "). $detalle No es una app del gobierno.",
        modifier = modifier,
    )
}

/** Sección completa para Ajustes: todas las fuentes y la renuncia. */
@Composable
fun SeccionFuentes() {
    Column {
        TextoConEnlaces(
            "• Precios de mayoreo de frutas, verduras, chiles, huevo y pollo: Sistema Nacional de Información e " +
                "Integración de Mercados (SNIIM), Secretaría de Economía — ",
            "economia-sniim.gob.mx" to Fuentes.SNIIM,
            estilo = MaterialTheme.typography.bodyMedium,
        )
        TextoConEnlaces(
            "• Precios de gasolina y diésel: Comisión Nacional de Energía (antes CRE) — ",
            "gob.mx/cne" to Fuentes.CNE,
            " · ",
            "datos de precios" to Fuentes.CNE_PRECIOS,
            estilo = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            Fuentes.RENUNCIA + " Los precios de menudeo, el semáforo y la temporada son estimados de la app.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}
