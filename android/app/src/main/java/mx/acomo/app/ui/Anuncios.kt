package mx.acomo.app.ui

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mx.acomo.app.BuildConfig

object Anuncios {
    private var iniciado = false

    /** Inicia el SDK de anuncios en segundo plano (una sola vez y solo si no hay Plus). */
    fun iniciar(contexto: Context) {
        if (iniciado) return
        iniciado = true
        CoroutineScope(Dispatchers.IO).launch { MobileAds.initialize(contexto.applicationContext) {} }
    }
}

/** Banner adaptable al ancho de la pantalla. No se muestra con ¿A cómo? Plus. */
@Composable
fun BannerAnuncio(plus: Boolean, modifier: Modifier = Modifier) {
    if (plus) return
    val contexto = LocalContext.current
    val ancho = LocalConfiguration.current.screenWidthDp
    val vista = remember(ancho) {
        Anuncios.iniciar(contexto)
        AdView(contexto).apply {
            adUnitId = BuildConfig.ADMOB_BANNER
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(contexto, ancho))
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(vista) { onDispose { vista.destroy() } }
    AndroidView({ vista }, modifier.fillMaxWidth().navigationBarsPadding())
}
