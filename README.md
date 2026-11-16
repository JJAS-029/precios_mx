# precios_mx

**¿A cómo?** — precios de referencia de frutas y verduras en centrales de abasto de México, para saber si lo que te cobran en el mercado es razonable.

Fuente: [SNIIM](http://www.economia-sniim.gob.mx/) (Secretaría de Economía), precios de mayoreo en pesos por kg.

## Estructura

| Ruta | Qué es |
|---|---|
| `recolector/sniim.py` | Descarga precios del SNIIM y genera los datos |
| `data/sniim_diario.csv` | Histórico diario crudo (todas las filas del SNIIM) |
| `data/precios.json` | Agregado semanal que consume la app |
| `.github/workflows/actualizar-precios.yml` | Actualización automática cada miércoles |
| `plang.md` | Plan del proyecto |

URL pública de los datos para la app:
`https://raw.githubusercontent.com/JJAS-029/precios_mx/main/data/precios.json`

## Correr el recolector localmente

```bash
python -m venv .venv
.venv/Scripts/activate        # Windows  (Linux/Mac: source .venv/bin/activate)
pip install -r requirements.txt

python recolector/sniim.py                     # últimos 14 días
python recolector/sniim.py --desde 2025-01-01  # carga histórica
python recolector/sniim.py --solo-json         # solo regenerar el JSON
```
