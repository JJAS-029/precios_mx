# Plan de Proyecto: App "¿A cómo?" (Bolsa de Valores Agrícola)

## 1. Visión general
App móvil (B2C) para combatir la asimetría de información en precios de frutas, verduras y básicos. El usuario consulta si el precio que le ofrecen en el mercado o tianguis es razonable, comparándolo con los precios oficiales de las Centrales de Abasto.

* **Plataforma inicial:** Android nativo (Kotlin + Jetpack Compose). Paquete `mx.acomo.app`.
* **Fuente de datos:** SNIIM (Secretaría de Economía), precios de mayoreo de frutas y hortalizas.
* **Cobertura:** las 49 centrales de abasto que publica el SNIIM en todo el país (unas 45 con datos vigentes).
* **Costo de infraestructura:** $0 (GitHub Actions + archivos JSON estáticos).

## 2. Arquitectura (sin servidor)

```
GitHub Actions (miércoles) ──> recolector/sniim.py ──> data/indice.json          (mercados, catálogo, último precio)
                                                   └─> data/mercados/<id>.json   (52 semanas por mercado)
App Android ──descarga──> raw.githubusercontent.com/JJAS-029/precios_mx/main/data/...
            └─ guarda copia local (funciona sin internet en el mercado)
```

* La app descarga el índice (~100 KB) y **solo el archivo del mercado elegido** (~250 KB).
* Firebase queda para más adelante, cuando haya cuentas de usuario o reportes de precios de usuarios.

## 3. Hallazgos sobre el SNIIM

### Frutas y hortalizas (en uso)
* Consulta por URL GET: `ResultadosConsultaFechaFrutasYHortalizas.aspx?fechaInicio=dd/mm/aaaa&fechaFinal=dd/mm/aaaa&ProductoId=-1&OrigenId=-1&DestinoId=<id>&PreciosPorId=2&RegistrosPorPagina=10000`
* `ProductoId=-1` trae todos los productos de un mercado; `PreciosPorId=2` = pesos por kg (calculado).
* Datos diarios (lunes a viernes): mínimo, máximo y frecuente. Catálogo de mercados con coordenadas aproximadas en `recolector/mercados.json`.

### Pollo, huevo y cárnicos (investigado el 2026-11-16, pendiente de integrar)
Base: `http://www.economia-sniim.gob.mx/SNIIM-Pecuarios-Nacionales/`

| Producto | Consulta | Cobertura | Frecuencia | Ejemplo (nov-2026) | ¿Útil para la app? |
|---|---|---|---|---|---|
| **Huevo** blanco y rojo | `e_Hue.asp?prod=0&destino=<id>&sem=0&mes=MM&anio=AAAA` | ~44 centrales, incluye Toluca (150) y Ecatepec (151) | Semanal (miércoles) | Toluca: blanco $39.90/kg | ✅ Sí, mismos mercados |
| **Pollo entero** y rosticero | `e_Ent.asp?prod=0&origen=0&destino=<id>&del=DD&al=DD&mes=MM&anio=AAAA` | CDMX (Becerra, San Juan, Nuevo San Juan, Rayón), Querétaro, Durango, Nayarit, Zacatecas, La Paz | Diario | CDMX: ~$65/kg | ✅ Sí, como referencia regional |
| **Pollo en piezas** (pechuga, pierna/muslo, retazo) | `e_Pza.asp?destino=<id>&del=…` | Mismos centros que pollo entero | Diario | CDMX: pechuga ~$112, pierna/muslo ~$57 | ✅ Sí |
| Res y cerdo: cortes | `e_Cor.asp?Var=Bov\|Por&…` | Empacadoras, sobre todo de Nuevo León; poca cobertura en el centro | Diario | Pulpa negra ~$157/kg (NL) | ⚠️ Limitado |
| Res y cerdo: en canal / en pie | `SelCan.asp`, `SelPie.asp` | Rastros de todo el país | Diario | — | ❌ No es lo que compra el consumidor |

Notas: las páginas pecuarias son ASP clásico (codificación latin-1), con fechas en campos separados (`del`, `al`, `mes`, `anio`) y paginación con `RegPag`. Para mercados sin datos de pollo se puede mostrar el de la central más cercana que sí tenga.

## 4. Modelo de datos

`data/indice.json`
```json
{
  "version": 2, "generado": "...", "ultimaSemana": "2026-W46",
  "mercados": [ { "id": "150", "nombre": "Central de Abasto", "ciudad": "Toluca", "estado": "Estado de México",
                  "lat": 19.27, "lon": -99.58, "ultimaSemana": "2026-W46", "productos": 59 } ],
  "productos": [ { "id": "tomate-saladette-primera", "nombre": "Tomate Saladette", "calidad": "Primera", "categoria": "Hortalizas" } ],
  "ultimos": { "tomate-saladette-primera": { "150": 30.73, "151": 37.5 } }
}
```

`data/mercados/<id>.json`
```json
{ "version": 2, "mercado": "150", "semanas": ["2025-W47", "..."],
  "productos": [ { "id": "...", "nombre": "...", "calidad": "...", "categoria": "...",
                   "semanas": [ { "semana": "2026-W46", "min": 25.0, "max": 39.58, "frec": 30.73, "dias": 5 } ] } ] }
```
`frec` = mediana semanal del precio frecuente diario. Se guardan 52 semanas.

## 5. Fases

### ✅ Fase 0–2: Investigación, recolector y automatización
GitHub Actions consulta el SNIIM cada miércoles (verificado que no bloquea las IPs de GitHub).

### ✅ Fase 3: App básica
Lista con buscador, "Barato esta semana", detalle con semáforo, gráfica de 12 semanas y comparación entre mercados. Funciona sin internet.

### ✅ Fase 3.5: Identidad y "Mis precios"
Logo del koala e íconos (`diseno/`), "Mis precios" para guardar lo que pagaste y compartirlo como CSV.

### ✅ Fase 3.6: Cobertura nacional, selector de zona y modo oscuro
* Los 49 mercados del SNIIM.
* Selector de mercado con buscador (ciudad o estado) y botón **"Usar mi ubicación"** (permiso de ubicación aproximada, se pide solo al tocar el botón y no sale del teléfono).
* Comparación contra los mercados más cercanos al tuyo.
* Modo oscuro según la configuración del teléfono.

### ✅ Fase 3.7: Material 3 Expressive y fechas de actualización
* `material3 1.5.0-rc01` con `MaterialExpressiveTheme`, `MotionScheme.expressive()`, formas (`MaterialShapes`), barra superior flexible y `LoadingIndicator`.
* Color dinámico (Material You) en Android 12+; en versiones anteriores, paleta tonal verde de la marca.
* Emoji por producto dentro de formas expresivas; "Barato esta semana" en tarjetas horizontales.
* Fechas: "Precios de mayoreo al vie 13 nov" en el encabezado, "Último dato del SNIIM … · revisado el …" bajo el mercado, fecha por mercado en el selector y aviso ⚠ si un mercado lleva más de 10 días sin publicar.
* El recolector guarda `ultimaFecha` (día exacto del último dato) por mercado.

### ✅ Fase 3.8: Temas, Ajustes y monetización (base)
* Pantalla **Ajustes** (⚙ en la barra superior).
* **Gratis:** tema verde del koala y modo Sistema / Claro / Oscuro.
* **¿A cómo? Plus:** sin anuncios, 4 temas extra (Jitomate, Mandarina, Jamaica, Maíz azul; paletas generadas con `diseno/paletas.py`) y "Colores de tu fondo de pantalla" (Material You, Android 12+).
* **Anuncios:** banner adaptable de AdMob al pie de la lista de productos, solo sin Plus. Por ahora con los **IDs de prueba de Google**.
* Plus se guarda en el teléfono; en la versión de desarrollo hay un interruptor "Activar Plus de prueba". La compra real llega en la Fase 4.

### ✅ Fase 3.9: Equivalencias
* En el detalle: tarjeta "También te sirve" con sustitutos del mismo mercado, ordenados por qué tan baratos están contra su propio promedio. Si el producto está ≥15 % arriba de su promedio y hay uno ≥5 % abajo, cambia a "💡 Está caro, mejor cambia".
* En la lista: sección "Está caro, mejor cambia" con hasta 3 cambios (p. ej. Toluca: col +35 % → lechuga; Iztapalapa: limón sin semilla +19 % → limón con semilla −22 %).
* Grupos automáticos por variedad (misma primera palabra; sin juntar tamaños como "# 4" y "# 5") y grupos manuales por uso en `Analisis.kt` (salsa, rajas, chile picoso, chile seco, pimiento, jugo, limón, ensalada, brócoli/coliflor, papa/camote, guisado, durazno/nectarina).

### ✅ Fase 3.10: Mi mandado (calculadora de presupuesto)
* Botón 🛒 (con contador) en la barra superior; en el detalle, "🛒 Agregar" y selector − / + (250 g en 250 g hasta 2 kg, luego medio kilo).
* Total estimado en mercado o tianguis (mayoreo × factor), total de mayoreo, aviso de productos sin precio en el mercado elegido y "Compartir lista" (texto para WhatsApp). Los estimados se muestran en pesos enteros.
* La lista se guarda en el teléfono y sirve al cambiar de mercado (el id del producto es el mismo).
* Pendiente: piezas (aguacate, piña, lechuga) con peso promedio por pieza; varias listas guardadas.

### 💰 Modelo de monetización
* **Gratis con anuncios:** un solo banner en la pantalla principal. Nada de anuncios de pantalla completa ni en la pantalla de detalle (es la que se usa frente al puesto).
* **Plus, pago único** (sugerido $29–49 MXN, por definir): quita anuncios y desbloquea temas. Más adelante podría incluir funciones extra (avisos de precio, varias listas de mandado), pero las funciones de precios básicas siempre gratis.
* AdMob paga poco por usuario en México; el ingreso real depende de volumen. Plus es el complemento.

### ✔ Fase 3.11: Avisos de precios bajos
* En el detalle: "🔔 Avisarme cuando baje" (pide el permiso de notificaciones en Android 13+).
* WorkManager revisa cada 12 h (con internet) si hay semana nueva del mercado elegido; avisa una sola vez por semana.
* Regla: bajó ≥10% contra la semana pasada, o está ≥10% bajo su promedio de 8 semanas **y** bajó ≥3% esa semana (para no repetir cada semana si se queda barato).
* Una notificación agrupada; si es un solo producto, al tocarla abre su detalle.
* Ajustes: interruptor general y lista de productos seguidos. En debug, botón "Probar aviso ahora".

### ⏳ Fase 4: Antes de publicar en Google Play
* ✔ Cuenta de desarrollador de Google Play: ya existe.
* 📝 **Testers (pendiente, se ve más adelante):** las cuentas personales nuevas deben hacer una prueba cerrada con al menos 12 testers durante 14 días antes de poder publicar en producción. Ir juntando correos de familiares y amigos con Android.
* ✔ Firma de la app (llave de subida local, fuera de git) y compilación release (`.aab`) verificada en el emulador. Guía paso a paso: `diseno/PUBLICAR.md`.
* ✔ R8: AdMob trae WorkManager 2.7, que fallaba en release; se fuerza WorkManager 2.12.
* ✔ **AdMob:** app "¿A cómo?" registrada; IDs en `android/gradle.properties` (la versión debug siempre usa anuncios de prueba). `app-ads.txt` ya en `jjas-029.github.io`.
* **Consentimiento de anuncios:** si se distribuye solo en México no se exige el formulario europeo (GDPR); si se abre a otros países, integrar UMP (`user-messaging-platform`).
* **Plus con Google Play Billing:** crear el producto "plus" (pago único) en Play Console e integrar `billing-ktx`; al abrir la app, confirmar la compra y restaurarla en otro teléfono. Quitar el interruptor de prueba de la versión release (ya está oculto con `BuildConfig.DEBUG`).
* ✔ Política de privacidad publicada en `https://jjas-029.github.io/acomo/privacidad.html` (página de la app en `/acomo/`). La app no recopila datos personales y la ubicación solo se usa en el teléfono, **pero AdMob sí usa el identificador de publicidad**; debe decirlo la política y el formulario de seguridad de datos.
* ✔ Textos de la ficha y respuestas de seguridad de datos en `diseno/PUBLICAR.md`; ícono 512 px en `diseno/play_store_512.png`. ✔ Capturas (6, 1080×1920) e imagen destacada 1024×500 en `diseno/play/`. Falta llenar los formularios en Play Console.

### Fase 5: Funciones que suman (ideas del 2026-11-16)
Ordenadas de más fácil a más trabajo:

1. ~~Equivalencias / sustitutos~~ ✅ (Fase 3.9). Pendiente: ampliar la tabla con lo que digan los usuarios.
2. ~~**Calculadora / carrito de presupuesto.**~~ ✅ (Fase 3.10, falta lo de piezas). El usuario arma su lista ("2 kg jitomate, 1 kg cebolla, 3 aguacates…") y ve el costo estimado al menudeo (mayoreo × factor), con total y semáforo por producto. Para piezas (aguacate, piña, lechuga) hace falta un peso promedio por pieza. Guardar listas frecuentes ("mandado de la semana").
3. **Productos de temporada.** Con el historial de 52 semanas se calcula en qué meses cada producto suele estar más barato. Sección "De temporada este mes" y aviso del tipo "Empieza la temporada de mandarina".
4. ~~**Avisos de precios bajos.**~~ ✅ (Fase 3.11). El usuario marca productos favoritos y recibe una notificación cuando bajan cierto % o están abajo de su promedio. Se puede hacer local: WorkManager revisa los datos nuevos cada miércoles y lanza una notificación, sin servidor. Requiere permiso de notificaciones (Android 13+).
5. **Huevo y pollo** (ver sección 3). Huevo primero, porque cubre los mismos mercados.
6. **Reportes de usuarios** ("pagué $X en el tianguis de Y") para tener precio de menudeo real; aquí entraría Firebase.
7. **Mapa de mercados.** Opcional; el buscador con ubicación ya resuelve la selección. Un mapa requiere el SDK de Google Maps (API key) u OpenStreetMap (osmdroid).
8. Datos abiertos de PROFECO (Quién es Quién en los Precios) para calibrar el factor de menudeo.

## 6. Pendientes y decisiones abiertas
* Calibrar el factor de menudeo (hoy ×1.5); usar los CSV de "Mis precios".
* Ampliar la tabla de nombres comunes y sinónimos por región (jitomate/tomate, elote, ejote…).
* Las coordenadas de `mercados.json` son aproximadas (a nivel ciudad); bastan para ordenar por cercanía.
