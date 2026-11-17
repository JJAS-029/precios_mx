# Cómo publicar ¿A cómo? en Google Play

## 1. Firma (ya está lista)
* Llave de subida: `android/acomo-upload.jks`, con sus contraseñas en `android/keystore.properties`.
  **Ninguno de los dos se sube a git.**
* **Guarda una copia de los dos archivos** (Drive, USB). Sin ellos no puedes subir actualizaciones
  (Google permite restablecer la llave de subida, pero tarda unos días).
* En Play Console activa **Play App Signing** (viene activado por defecto): Google guarda la llave
  final y tú solo usas la de subida.

## 2. Compilar
```
cd android
gradlew bundleRelease     # -> app/build/outputs/bundle/release/app-release.aab (esto se sube a Play)
gradlew assembleRelease   # -> APK firmado para probar en un teléfono
```
Antes de cada versión nueva, subir `versionCode` (y `versionName`) en `app/build.gradle.kts`.

## 3. AdMob (ya está lista)
* App "¿A cómo?" en la cuenta de AdMob, con su bloque Banner. Los IDs están en
  `android/gradle.properties` (`admob.appId`, `admob.banner`); solo los usa la versión release,
  la de desarrollo siempre muestra anuncios de prueba.
* `app-ads.txt` ya está en `https://jjas-029.github.io/app-ads.txt` (misma cuenta que Lab Pomodoro).
  En la ficha de Play hay que poner como **sitio web** `https://jjas-029.github.io/acomo/`
  (mismo dominio, así AdMob encuentra el `app-ads.txt`).
* Cuando esté publicada, vincularla con Play desde AdMob (Apps → Configuración de la app).

## 4. Sitio y política de privacidad (ya publicadas)
* Viven en el repo del sitio `JJAS-029/JJAS-029.github.io`, carpeta `acomo/`:
  * Página: `https://jjas-029.github.io/acomo/`
  * Aviso de privacidad: `https://jjas-029.github.io/acomo/privacidad.html`
* Contacto: ciencia.koala@gmail.com. Si la app cambia lo que hace con datos, actualizar el aviso ahí.
* Cuando salga en Play, cambiar en `acomo/index.html` el letrero "Muy pronto en Google Play" por el
  enlace `https://play.google.com/store/apps/details?id=mx.acomo.app`.

## 5. Ficha de la tienda

**Nombre (30):** ¿A cómo? Precios de mercado

**Descripción corta (80):** ¿Te están cobrando de más? Compara con los precios oficiales de mayoreo.

**Descripción completa:**
> ¿A cómo está el jitomate? ¿Te están cobrando de más por el aguacate?
>
> ¿A cómo? te muestra cada semana los precios oficiales de mayoreo de frutas, verduras y chiles en
> las centrales de abasto de todo México (datos del SNIIM, Secretaría de Economía), para que sepas
> cuánto es un precio justo en el mercado o tianguis.
>
> • Más de 40 centrales de abasto: elige la tuya o usa tu ubicación.
> • Semáforo: escribe lo que te cobran y ve si es buen precio, un poco caro o caro.
> • "Barato esta semana": lo que bajó de precio.
> • "Está caro, mejor cambia": si el jitomate subió, te dice qué sustituto está barato.
> • Mi mandado: arma tu lista, calcula cuánto vas a gastar y compártela por WhatsApp.
> • Avisos: te llega una notificación cuando baja un producto que sigues.
> • Mis precios: guarda lo que pagaste y compáralo.
> • Gráfica de 12 semanas y comparación con mercados cercanos.
> • Funciona sin internet con los últimos precios descargados.
> • Modo oscuro.
>
> Los precios de mayoreo son datos públicos del SNIIM. El precio de menudeo es un estimado.
> Esta app no está afiliada a la Secretaría de Economía.

**Categoría:** Compras. **Sitio web:** `https://jjas-029.github.io/acomo/` · **Condiciones:** `https://jjas-029.github.io/acomo/condiciones.html`.

*Agregar la línea "¿A cómo? Plus (pago único): sin anuncios y temas de color extra." cuando el cobro esté activo.*

 **Etiquetas:** precios, mercado, ahorro.

**Gráficos (listos en `diseno/play/`):**
* Ícono 512 px: `diseno/play_store_512.png`.
* Imagen destacada 1024×500: `diseno/play/destacada_1024x500.png`.
* 6 capturas de teléfono 1080×1920 (`01_lista` … `06_oscuro`), en ese orden. Play pide que el lado
  largo no pase del doble del corto; las capturas directas del emulador (1080×2400) no sirven tal cual.
* Se generan con un script de Pillow (fondo, título y la captura con marco) a partir de capturas del
  emulador con la barra de estado en modo demo (10:00, batería llena) y Plus de prueba (sin anuncio).

## 6. Formularios de Play Console
* **Acceso a la app:** todo disponible sin cuenta.
* **Anuncios:** Sí, contiene anuncios.
* **Clasificación de contenido:** cuestionario IARC → utilidad / compras, sin contenido sensible.
* **Público objetivo:** 18+ (o 13+). No marcar "dirigida a niños" (cambia las reglas de anuncios).
* **Seguridad de los datos:**
  * ¿Recopila o comparte datos? **Sí** (por el SDK de AdMob).
  * *Identificadores del dispositivo* (ID de publicidad): recopilado y compartido, para **publicidad**
    y **estadísticas**, no opcional en la versión gratuita.
  * *Información y rendimiento de la app* (diagnósticos/fallas de AdMob): recopilado, estadísticas.
  * *Ubicación aproximada*: **sí**, recopilada y compartida, por AdMob (la estima con la dirección IP).
    La ubicación que usa la app para ordenar mercados no sale del teléfono y no cuenta.
  * *Actividad en apps → Interacciones con la app* (toques y vistas de anuncios): recopilado y compartido.
  * En los cuatro tipos (ubicación aproximada, interacciones, fallas/diagnóstico e IDs): no efímero,
    obligatorio, finalidades Publicidad, Estadísticas y Prevención de fraudes.
  * Cuentas: "Mi app no permite que los usuarios creen cuentas".
  * Los productos seguidos para avisos se quedan en el teléfono: no cuentan como datos recopilados.
  * Datos cifrados en tránsito: **Sí**. Opción para borrar datos: no aplica (no hay cuenta).
* **Países:** empezar solo con **México** (evita el formulario de consentimiento europeo, GDPR).
* **ID de publicidad:** declarar que la app lo usa (AdMob agrega el permiso `AD_ID`).

## 7. Prueba cerrada (cuentas personales nuevas)
12 testers durante 14 días antes de poder pasar a producción. Crear una lista de correos en
Play Console → Pruebas → Prueba cerrada, subir el `.aab` y mandarles el enlace de suscripción.

## 8. Plus (Google Play Billing) — pendiente
1. Play Console → Monetizar → Productos → **Productos integrados**: id `plus`, pago único.
2. Integrar `com.android.billingclient:billing-ktx` en la app: comprar, confirmar (acknowledge) y
   restaurar al abrir. Requiere que la app ya esté subida a una pista de prueba.
