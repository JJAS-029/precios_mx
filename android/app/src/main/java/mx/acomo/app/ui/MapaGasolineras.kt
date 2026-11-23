package mx.acomo.app.ui

import android.graphics.RectF
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import mx.acomo.app.data.GasolineraCercana
import mx.acomo.app.data.NivelPrecio
import mx.acomo.app.ui.theme.fondoOscuro
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import kotlin.math.ln

/** Mapas de OpenFreeMap (datos de OpenStreetMap): gratis, sin clave. Estilos sobrios para que resalten los puntos. */
private const val ESTILO_CLARO = "https://tiles.openfreemap.org/styles/positron"
private const val ESTILO_OSCURO = "https://tiles.openfreemap.org/styles/dark"
private const val FUENTE = "gasolineras"
private const val FUENTE_YO = "yo"
private const val CAPA_PUNTOS = "gas-puntos"
private const val DORADO = 0xFFF2B705.toInt()

/**
 * Nivel de cada gasolinera por su lugar entre las que se ven (quintiles): con precios tan parecidos,
 * comparar contra el promedio dejaría casi todas en gris.
 */
fun nivelesPorRango(lista: List<GasolineraCercana>): Map<Int, NivelPrecio> {
    if (lista.isEmpty()) return emptyMap()
    val ordenadas = lista.sortedBy { it.precio }
    return ordenadas.withIndex().associate { (i, g) ->
        // Las de igual precio quedan en el mismo nivel que la primera de ellas.
        val rango = ordenadas.indexOfFirst { it.precio == g.precio }
        g.estacion.i to NivelPrecio.entries[(rango * 5 / ordenadas.size).coerceIn(0, 4)]
    }
}

private fun Color.hex() = String.format("#%06X", toArgb() and 0xFFFFFF)

@Composable
fun MapaGasolineras(
    gasolineras: List<GasolineraCercana>,
    centro: Pair<Double, Double>,
    radioKm: Double,
    /** Ubicación del teléfono (si se usó), para el punto azul. */
    yo: Pair<Double, Double>?,
    favoritas: Set<String>,
    elegida: Int?,
    onElegir: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contexto = LocalContext.current
    val oscuro = fondoOscuro()
    val ciclo = LocalLifecycleOwner.current.lifecycle
    val vista = remember {
        MapLibre.getInstance(contexto)
        MapView(contexto).apply { onCreate(null) }
    }
    var mapa by remember { mutableStateOf<MapLibreMap?>(null) }
    var estilo by remember { mutableStateOf<Style?>(null) }
    val alElegir by rememberUpdatedState(onElegir)
    val colorTexto = if (oscuro) "#FFFFFF" else "#1A1C16"
    val colorHalo = if (oscuro) "#12140E" else "#FFFFFF"

    DisposableEffect(ciclo) {
        val observador = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_START -> vista.onStart()
                Lifecycle.Event.ON_RESUME -> vista.onResume()
                Lifecycle.Event.ON_PAUSE -> vista.onPause()
                Lifecycle.Event.ON_STOP -> vista.onStop()
                else -> Unit
            }
        }
        ciclo.addObserver(observador)
        onDispose {
            ciclo.removeObserver(observador)
            vista.onPause()
            vista.onStop()
            vista.onDestroy()
        }
    }

    // Estilo (claro u oscuro) y capas.
    LaunchedEffect(oscuro) {
        estilo = null
        vista.getMapAsync { m ->
            m.uiSettings.isRotateGesturesEnabled = false
            m.uiSettings.isTiltGesturesEnabled = false
            m.setStyle(Style.Builder().fromUri(if (oscuro) ESTILO_OSCURO else ESTILO_CLARO)) { s ->
                s.addSource(GeoJsonSource(FUENTE))
                s.addSource(GeoJsonSource(FUENTE_YO))
                s.addLayer(
                    CircleLayer("yo-halo", FUENTE_YO).withProperties(
                        PropertyFactory.circleRadius(14f),
                        PropertyFactory.circleColor("#1A73E8"),
                        PropertyFactory.circleOpacity(0.18f),
                    ),
                )
                s.addLayer(
                    CircleLayer("yo", FUENTE_YO).withProperties(
                        PropertyFactory.circleRadius(6f),
                        PropertyFactory.circleColor("#1A73E8"),
                        PropertyFactory.circleStrokeWidth(2f),
                        PropertyFactory.circleStrokeColor("#FFFFFF"),
                    ),
                )
                s.addLayer(
                    CircleLayer(CAPA_PUNTOS, FUENTE).withProperties(
                        PropertyFactory.circleColor(Expression.get("color")),
                        PropertyFactory.circleRadius(
                            Expression.switchCase(Expression.get("sel"), Expression.literal(11f), Expression.literal(7f)),
                        ),
                        PropertyFactory.circleStrokeWidth(
                            Expression.switchCase(
                                Expression.any(Expression.get("sel"), Expression.get("fav")), Expression.literal(3f), Expression.literal(1.5f),
                            ),
                        ),
                        PropertyFactory.circleStrokeColor(
                            Expression.switchCase(
                                Expression.get("fav"), Expression.color(DORADO),
                                Expression.get("sel"), Expression.color(android.graphics.Color.parseColor(colorTexto)),
                                Expression.color(android.graphics.Color.parseColor(colorHalo)),
                            ),
                        ),
                    ),
                )
                // Precio junto a cada punto, solo de cerca para no amontonar.
                s.addLayer(
                    SymbolLayer("gas-precios", FUENTE).withProperties(
                        PropertyFactory.textField(Expression.get("precio")),
                        PropertyFactory.textFont(arrayOf("Noto Sans Bold")),
                        PropertyFactory.textSize(11f),
                        PropertyFactory.textOffset(arrayOf(0f, 1.25f)),
                        PropertyFactory.textAnchor("top"),
                        PropertyFactory.textColor(colorTexto),
                        PropertyFactory.textHaloColor(colorHalo),
                        PropertyFactory.textHaloWidth(1.5f),
                    ).also { it.minZoom = 12f },
                )
                m.addOnMapClickListener { p ->
                    val pt = m.projection.toScreenLocation(p)
                    val r = 24f * contexto.resources.displayMetrics.density
                    val toque = m.queryRenderedFeatures(RectF(pt.x - r, pt.y - r, pt.x + r, pt.y + r), CAPA_PUNTOS)
                        .minByOrNull {
                            val g = it.geometry() as? Point ?: return@minByOrNull Float.MAX_VALUE
                            val q = m.projection.toScreenLocation(LatLng(g.latitude(), g.longitude()))
                            (q.x - pt.x) * (q.x - pt.x) + (q.y - pt.y) * (q.y - pt.y)
                        }
                    alElegir(toque?.getNumberProperty("id")?.toInt())
                    true
                }
                mapa = m
                estilo = s
            }
        }
    }

    // Cámara: centrada en el punto, con el radio elegido a la vista.
    LaunchedEffect(mapa, centro, radioKm) {
        val m = mapa ?: return@LaunchedEffect
        // Que el diámetro del radio quepa a lo ancho (teléfono de ~400 dp, latitudes de México): zoom ≈ 13.8 − log2(radio).
        val zoom = 13.8 - ln(radioKm) / ln(2.0)
        m.cameraPosition = CameraPosition.Builder().target(LatLng(centro.first, centro.second)).zoom(zoom).build()
    }

    // Puntos.
    LaunchedEffect(estilo, gasolineras, favoritas, elegida, oscuro) {
        val s = estilo ?: return@LaunchedEffect
        val niveles = nivelesPorRango(gasolineras)
        val puntos = gasolineras.map { g ->
            Feature.fromGeometry(Point.fromLngLat(g.estacion.o, g.estacion.a)).apply {
                addNumberProperty("id", g.estacion.i)
                addStringProperty("precio", "$" + "%.2f".format(g.precio))
                addStringProperty("color", ColoresMapa.de(niveles[g.estacion.i] ?: NivelPrecio.NORMAL, oscuro).hex())
                addBooleanProperty("fav", g.estacion.i.toString() in favoritas)
                addBooleanProperty("sel", g.estacion.i == elegida)
            }
        }
        // La elegida y las favoritas al final, para que queden encima.
        val orden = puntos.sortedBy { (if (it.getBooleanProperty("sel")) 2 else 0) + (if (it.getBooleanProperty("fav")) 1 else 0) }
        s.getSourceAs<GeoJsonSource>(FUENTE)?.setGeoJson(FeatureCollection.fromFeatures(orden))
        s.getSourceAs<GeoJsonSource>(FUENTE_YO)?.setGeoJson(
            FeatureCollection.fromFeatures(yo?.let { listOf(Feature.fromGeometry(Point.fromLngLat(it.second, it.first))) } ?: emptyList()),
        )
    }

    AndroidView(factory = { vista }, modifier = modifier)
}

/** Leyenda: los cinco colores y qué significan, más el borde dorado de las favoritas. */
@Composable
fun LeyendaGasolineras(modifier: Modifier = Modifier) {
    val oscuro = fondoOscuro()
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Más barata", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        NivelPrecio.entries.forEach { n ->
            Box(Modifier.size(12.dp).background(ColoresMapa.de(n, oscuro), CircleShape))
        }
        Text("Más cara", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.size(12.dp).border(2.dp, Color(DORADO), CircleShape))
        Text("Favorita", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
