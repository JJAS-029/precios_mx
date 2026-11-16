# precios_mx

**¿A cómo?** — precios de referencia de frutas y verduras en centrales de abasto de México, para saber si lo que te cobran en el mercado es razonable.

Fuente: [SNIIM](http://www.economia-sniim.gob.mx/) (Secretaría de Economía), precios de mayoreo en pesos por kg.

## Estructura

| Ruta | Qué es |
|---|---|
| `recolector/sniim.py` | Descarga precios del SNIIM y genera los datos |
| `recolector/mercados.json` | Catálogo de las 49 centrales de abasto (con coordenadas aproximadas) |
| `data/indice.json` | Mercados, catálogo de productos y último precio de cada uno |
| `data/mercados/<id>.json` | Historial semanal (52 semanas) por mercado |
| `.github/workflows/actualizar-precios.yml` | Actualización automática cada miércoles |
| `android/` | App Android "¿A cómo?" (Kotlin + Compose) |
| `diseno/` | Logo e íconos (`generar_iconos.py`) |
| `plang.md` | Plan del proyecto |

URL pública de los datos para la app:
`https://raw.githubusercontent.com/JJAS-029/precios_mx/main/data/indice.json`

## Correr el recolector localmente

```bash
python -m venv .venv
.venv/Scripts/activate        # Windows  (Linux/Mac: source .venv/bin/activate)
pip install -r requirements.txt

python recolector/sniim.py                         # últimas 3 semanas, todos los mercados
python recolector/sniim.py --desde 2025-11-17      # carga histórica
python recolector/sniim.py --mercados 150 151      # solo algunos mercados
python recolector/sniim.py --solo-indice           # solo regenerar indice.json
```
