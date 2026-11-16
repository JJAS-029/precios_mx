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

## 3. AdMob
1. En AdMob: Apps → Agregar app → Android → "¿Está publicada?" **No** → nombre "¿A cómo?".
2. Crear un bloque **Banner** dentro de esa app.
3. Poner los dos IDs en `android/gradle.properties` (`admob.appId` y `admob.banner`).
   La versión de desarrollo sigue usando anuncios de prueba.
4. Cuando esté publicada, vincularla con Play desde AdMob.
5. `app-ads.txt`: debe estar en la **raíz** del sitio web que pongas en la ficha de Play
   (p. ej. `https://tusitio.com/app-ads.txt`). Si ya lo tienes para tu otra app en ese mismo sitio,
   ya queda cubierto; la línea es la misma porque es la misma cuenta.

## 4. Política de privacidad
* Archivo: `docs/privacidad.html`. **Cambiar `CORREO_DE_CONTACTO`** por un correo de contacto.
* Publicarla con GitHub Pages: repo → Settings → Pages → Branch `main`, carpeta `/docs`.
  Queda en `https://jjas-029.github.io/precios_mx/privacidad.html`.

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
> • Semáforo: escribe lo que te cobran y ve si es razonable, alto o muy caro.
> • "Barato esta semana": lo que bajó de precio.
> • "Está caro, mejor cambia": si el jitomate subió, te dice qué sustituto está barato.
> • Mi mandado: arma tu lista y calcula cuánto vas a gastar. Compártela por WhatsApp.
> • Mis precios: guarda lo que pagaste y compáralo.
> • Gráfica de 12 semanas y comparación con mercados cercanos.
> • Funciona sin internet con los últimos datos descargados.
> • Modo oscuro.
>
> ¿A cómo? Plus (pago único): sin anuncios y temas de color extra.
>
> Los precios de mayoreo son datos públicos del SNIIM. El precio de menudeo es un estimado.
> Esta app no está afiliada a la Secretaría de Economía.

**Categoría:** Compras (o Estilo de vida). **Etiquetas:** precios, mercado, ahorro.

**Gráficos:** ícono 512 px (`diseno/play_store_512.png`), imagen destacada 1024×500 (pendiente),
2 a 8 capturas de teléfono (lista, detalle con semáforo, mandado, equivalencias).

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
  * *Ubicación aproximada*: **no** se recopila (se usa solo en el teléfono, no sale de él).
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
