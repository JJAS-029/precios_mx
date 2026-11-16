# Plan de Proyecto: App "¿A cómo?" (Bolsa de Valores Agrícola)

## 1. Visión general
App móvil (B2C) para combatir la asimetría de información en precios de frutas, verduras y básicos. El usuario consulta si el precio que le ofrecen en el mercado o tianguis es razonable, comparándolo con los precios oficiales de las Centrales de Abasto.

* **Plataforma inicial:** Android nativo (Kotlin + Jetpack Compose).
* **Fuente de datos:** SNIIM (Secretaría de Economía), precios de mayoreo de frutas y hortalizas.
* **Cobertura inicial:** Estado de México (Toluca, Ecatepec) + Iztapalapa (CDMX) como referencia.
* **Costo de infraestructura:** $0 (GitHub Actions + archivo JSON estático).

## 2. Arquitectura (simplificada: sin servidor)

```
GitHub Actions (miércoles)  ──>  recolector/sniim.py  ──>  data/sniim_diario.csv  (histórico crudo)
                                                       └─>  data/precios.json      (agregado semanal)
App Android  ──descarga──>  raw.githubusercontent.com/JJAS-029/precios_mx/main/data/precios.json
             └─ guarda copia local (funciona sin internet en el mercado)
```

Firebase queda para más adelante, cuando haya cuentas de usuario o reportes de precios hechos por usuarios.

## 3. Hallazgos sobre el SNIIM (Fase 0, completada)
* La consulta es una URL GET (no hace falta simular el formulario):
  `ResultadosConsultaFechaFrutasYHortalizas.aspx?fechaInicio=dd/mm/aaaa&fechaFinal=dd/mm/aaaa&ProductoId=-1&OrigenId=-1&DestinoId=<id>&PreciosPorId=2&RegistrosPorPagina=10000`
* `ProductoId=-1` trae **todos los productos** de un mercado en una sola petición.
* `PreciosPorId=2` = **pesos por kilogramo (calculado)**: el SNIIM ya convierte cajas a kg. Esto resuelve el problema de unidades.
* IDs de mercado: Toluca = 150, Ecatepec = 151, Iztapalapa = 100.
* Datos **diarios** (lunes a viernes), con mínimo, máximo y frecuente, más origen y presentación.
* Hay unos 100 productos con datos en estos 3 mercados.

## 4. Modelo de datos (`data/precios.json`)
```json
{
  "version": 1,
  "generado": "2026-11-16T18:00:00+00:00",
  "unidad": "MXN/kg",
  "mercados": { "150": "Central de Abasto de Toluca", "151": "...", "100": "..." },
  "semanas": ["2025-W47", "...", "2026-W46"],
  "productos": [
    {
      "id": "tomate-saladette-primera",
      "nombre": "Tomate Saladette", "calidad": "Primera", "categoria": "Hortalizas",
      "semanas": [ { "semana": "2026-W46", "mercado": "151", "min": 32.5, "max": 41.0, "frec": 37.5, "dias": 5 } ]
    }
  ]
}
```
Agregado semanal por producto y mercado: `min` = mínimo de la semana, `max` = máximo, `frec` = mediana del precio frecuente diario. Se guardan las últimas 52 semanas.

## 5. Fases

### Fase 0: Investigación del SNIIM ✅
### Fase 1: Recolector en Python ✅
`recolector/sniim.py`: descarga, guarda el CSV diario y genera el JSON. Histórico cargado desde nov-2025.

### Fase 2: Automatización ✅ (verificada: GitHub sí puede consultar el SNIIM)
`.github/workflows/actualizar-precios.yml`: cada miércoles descarga los últimos 14 días y hace commit.
* Riesgo: el sitio del gobierno podría bloquear IPs fuera de México (los servidores de GitHub están en EE.UU.). Si pasa, alternativas: ejecutarlo desde una PC local con el Programador de tareas, o desde una Raspberry Pi o un VPS en México.

### Fase 3: App Android básica ✅ (falta probar en un teléfono)
Carpeta `android/`: Kotlin, Jetpack Compose, minSdk 26, compileSdk 37.
* Descarga `precios.json` (HttpURLConnection + kotlinx.serialization) y lo guarda en el teléfono; funciona sin internet.
* Lista con buscador (sin acentos, "jitomate" encuentra Tomate Saladette/Bola), selector de mercado y sección "Barato esta semana".
* Detalle: precio de la semana, cambio semanal, promedio de 8 semanas, semáforo, gráfica de 12 semanas (Canvas) y comparación entre mercados.
* Pruebas unitarias en `app/src/test` (incluyen leer el JSON real).

### Fase 3.5: Identidad y "Mis precios" ✅
* Logo del koala (`diseno/logo_original.jpg`); íconos generados con `diseno/generar_iconos.py`. Ícono para Google Play: `diseno/play_store_512.png`.
* La app recuerda el último mercado elegido.
* "Mis precios": guardar lo que pagaste (precio, lugar, fecha) contra el mayoreo de la semana; se puede compartir como CSV para calibrar el semáforo.

### Fase 4: MVP (parcialmente adelantado en la Fase 3)
* **Buscador** con tolerancia a acentos y sinónimos ("jitomate" → Tomate Saladette/Bola).
* **Detalle de producto:** precio de la semana, tendencia contra la semana anterior y gráfica de 12 semanas (Vico).
* **Semáforo:** el usuario escribe el precio que le ofrecen por kg y lo compara contra el mayoreo × factor de menudeo.
  * Factor inicial: alrededor de ×1.3–1.6 según la categoría (por calibrar). Presentarlo como **"precio de referencia"**, no como "precio justo".
* **"Barato esta semana":** productos con precio más bajo que su promedio de 8 semanas.

### Fase 5: Crecimiento
* Más mercados y estados (solo hay que agregar IDs en `MERCADOS`).
* Reportes de usuarios ("pagué $X en el tianguis de Y") → precio de menudeo propio (aquí entra Firebase).
* Lista del súper con costo estimado y notificaciones de bajadas de precio.
* Investigar los datos abiertos de PROFECO (Quién es Quién en los Precios) para calibrar el factor de menudeo.

## 6. Pendientes y decisiones abiertas
* Nombres amigables: el SNIIM dice "Tomate Saladette"; en el centro del país se dice "jitomate". Hace falta una tabla de sinónimos y nombres para mostrar.
* Calibrar el factor de menudeo por categoría.
* Ícono, nombre comercial y cuenta de Google Play (pago único de 25 USD).
