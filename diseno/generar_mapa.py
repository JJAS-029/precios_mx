"""Genera android/app/src/main/assets/estados.json (contornos simplificados de los estados de México).

Fuente: Natural Earth, "Admin 1 – States, Provinces" 1:10m (dominio público).
    https://naciscdn.org/naturalearth/10m/cultural/ne_10m_admin_1_states_provinces.zip

    .venv/Scripts/python -m pip install pyshp shapely
    .venv/Scripts/python diseno/generar_mapa.py <carpeta>/ne_10m_admin_1_states_provinces

Salida: {"ancho": 1, "alto": a, "estados": [{"nombre": "Jalisco", "anillos": [[x0, y0, x1, y1, ...], ...]}]}
Coordenadas ya proyectadas (equirectangular con cos(23.5°)) y normalizadas: x en [0, 1], y en [0, alto],
con y hacia abajo como en la pantalla. Los nombres coinciden con Mercado.estado de indice.json.
"""

import json
import math
import sys
from pathlib import Path

import shapefile
from shapely.geometry import shape

RAIZ = Path(__file__).resolve().parent.parent
SALIDA = RAIZ / "android" / "app" / "src" / "main" / "assets" / "estados.json"

# Natural Earth -> nombre usado por el SNIIM / indice.json
NOMBRES = {"Distrito Federal": "Ciudad de México", "México": "Estado de México"}

TOLERANCIA = 0.02  # grados; suficiente para un mapa del tamaño de una tarjeta
AREA_MINIMA = 0.004  # grados²; descarta islotes que no se verían
COS_LAT = math.cos(math.radians(23.5))


def main() -> None:
    lector = shapefile.Reader(sys.argv[1], encoding="utf-8")
    estados = []
    for s in lector.iterShapeRecords():
        if s.record["adm0_a3"] != "MEX" or not s.record["name"]:
            continue
        nombre = NOMBRES.get(s.record["name"], s.record["name"])
        geom = shape(s.shape.__geo_interface__).simplify(TOLERANCIA, preserve_topology=True)
        poligonos = list(geom.geoms) if geom.geom_type == "MultiPolygon" else [geom]
        anillos = [list(p.exterior.coords) for p in poligonos if p.area >= AREA_MINIMA]
        if not anillos:  # estados muy pequeños: conservar al menos el mayor
            anillos = [list(max(poligonos, key=lambda p: p.area).exterior.coords)]
        estados.append({"nombre": nombre, "anillos": anillos})

    xs = [lon * COS_LAT for e in estados for a in e["anillos"] for lon, _ in a]
    ys = [lat for e in estados for a in e["anillos"] for _, lat in a]
    x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
    escala = 1 / (x1 - x0)
    for e in estados:
        e["anillos"] = [
            [v for lon, lat in a for v in (round((lon * COS_LAT - x0) * escala, 4), round((y1 - lat) * escala, 4))]
            for a in e["anillos"]
        ]
    estados.sort(key=lambda e: e["nombre"])
    SALIDA.parent.mkdir(parents=True, exist_ok=True)
    datos = {"ancho": 1, "alto": round((y1 - y0) * escala, 4), "estados": estados}
    SALIDA.write_text(json.dumps(datos, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    puntos = sum(len(a) // 2 for e in estados for a in e["anillos"])
    print(f"{SALIDA.relative_to(RAIZ)}: {len(estados)} estados, {puntos} puntos, {SALIDA.stat().st_size // 1024} KB")


if __name__ == "__main__":
    main()
