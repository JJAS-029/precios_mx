"""Índice de temporada por producto: en qué meses suele estar más barato o más caro.

Se calcula con varios años de historia de algunas centrales grandes (no de todas, para no cargar al
servidor del SNIIM) y se guarda en data/temporada.json. No hace falta correrlo cada semana: una vez al
año basta. Las descargas mensuales se guardan en recolector/historico/ (no se sube a git), así que si
se interrumpe, al volver a correrlo continúa donde se quedó.

Uso:
    python recolector/temporada.py                  # descarga lo que falte y calcula
    python recolector/temporada.py --solo-calcular  # sin descargar

Método, por producto:
  1. Precio mensual de cada central = mediana de los precios frecuentes del mes.
  2. Cada mes se divide entre la mediana de los 12 meses alrededor (6 antes, 5 después) de esa misma
     central; así se quitan la inflación y las tendencias de un año en particular.
  3. El índice de cada mes del calendario es la mediana de esos cocientes en todas las centrales y años.
     1.00 = precio normal; 0.85 = suele estar 15 % abajo de lo normal ese mes.
"""

from __future__ import annotations

import argparse
import json
import statistics
import sys
import time
from collections import defaultdict
from datetime import date, datetime, timedelta, timezone
from pathlib import Path

import requests

sys.path.insert(0, str(Path(__file__).resolve().parent))
from sniim import DIR_MERCADOS, RAIZ, descargar, escribir_json, slug  # noqa: E402

# Centrales grandes repartidas por el país (ids de recolector/mercados.json).
MERCADOS = {
    "100": "Iztapalapa", "140": "Guadalajara", "190": "Monterrey", "210": "Puebla",
    "110": "León", "310": "Mérida", "250": "Culiacán", "306": "Veracruz",
}
DESDE = date(2022, 11, 1)          # 3 años antes del historial semanal que ya está en data/mercados
HASTA = date(2025, 11, 16)
DIR_HISTORICO = RAIZ / "recolector" / "historico"
SALIDA = RAIZ / "data" / "temporada.json"

MIN_MESES_VENTANA = 9    # meses con dato en la ventana de 12 para calcular el cociente
MIN_OBSERVACIONES = 4    # cocientes por mes del calendario para confiar en el índice
MIN_MESES_CUBIERTOS = 10 # meses del calendario con índice para publicar el producto


def meses(desde: date, hasta: date):
    m = date(desde.year, desde.month, 1)
    while m <= hasta:
        siguiente = date(m.year + (m.month == 12), m.month % 12 + 1, 1)
        yield m, min(siguiente - timedelta(days=1), hasta)
        m = siguiente


def descargar_historico() -> None:
    sesion = requests.Session()
    sesion.headers["User-Agent"] = "Mozilla/5.0"
    for mid, ciudad in MERCADOS.items():
        carpeta = DIR_HISTORICO / mid
        carpeta.mkdir(parents=True, exist_ok=True)
        nuevos = 0
        for inicio, fin in meses(DESDE, HASTA):
            archivo = carpeta / f"{inicio:%Y-%m}.json"
            if archivo.exists():
                continue
            filas = descargar(mid, inicio, fin, sesion)
            # Solo lo necesario: id de producto, fecha y precio frecuente.
            compacto = [[slug(f"{f['producto']} {f['calidad']}"), f["fecha"].isoformat(), f["frec"]] for f in filas]
            archivo.write_text(json.dumps(compacto, separators=(",", ":")), encoding="utf-8")
            nuevos += 1
            time.sleep(1.5)  # ser amables con el servidor
        print(f"{ciudad} ({mid}): {nuevos} meses descargados")


def series_mensuales() -> dict[tuple[str, str], dict[str, float]]:
    """{(mercado, producto): {"AAAA-MM": precio mensual}} con el histórico + el año reciente."""
    valores: dict[tuple[str, str, str], list[float]] = defaultdict(list)
    for mid in MERCADOS:
        for archivo in sorted((DIR_HISTORICO / mid).glob("*.json")):
            for pid, fecha, frec in json.loads(archivo.read_text(encoding="utf-8")):
                valores[(mid, pid, fecha[:7])].append(frec)
        reciente = DIR_MERCADOS / f"{mid}.json"
        if reciente.exists():
            for p in json.loads(reciente.read_text(encoding="utf-8"))["productos"]:
                for s in p["semanas"]:
                    anio, sem = s["semana"].split("-W")
                    lunes = date.fromisocalendar(int(anio), int(sem), 1)
                    valores[(mid, p["id"], f"{lunes:%Y-%m}")].append(s["frec"])
    series: dict[tuple[str, str], dict[str, float]] = defaultdict(dict)
    for (mid, pid, mes), vs in valores.items():
        series[(mid, pid)][mes] = statistics.median(vs)
    return series


def mes_mas(mes: str, n: int) -> str:
    a, m = map(int, mes.split("-"))
    total = a * 12 + (m - 1) + n
    return f"{total // 12}-{total % 12 + 1:02d}"


def calcular() -> dict:
    cocientes: dict[str, list[list[float]]] = defaultdict(lambda: [[] for _ in range(12)])
    for (mid, pid), serie in series_mensuales().items():
        for mes, precio in serie.items():
            ventana = [serie[v] for v in (mes_mas(mes, k) for k in range(-6, 6)) if v in serie]
            if len(ventana) < MIN_MESES_VENTANA:
                continue
            cocientes[pid][int(mes[5:]) - 1].append(precio / statistics.median(ventana))

    productos = {}
    for pid, por_mes in cocientes.items():
        indice = [round(statistics.median(c), 3) if len(c) >= MIN_OBSERVACIONES else None for c in por_mes]
        if sum(i is not None for i in indice) >= MIN_MESES_CUBIERTOS:
            productos[pid] = indice
    return {
        "version": 1,
        "generado": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "desde": DESDE.isoformat(),
        "mercados": list(MERCADOS.values()),
        "productos": dict(sorted(productos.items())),
    }


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--solo-calcular", action="store_true")
    args = ap.parse_args()
    if not args.solo_calcular:
        descargar_historico()
    datos = calcular()
    escribir_json(SALIDA, datos)
    print(f"{SALIDA.relative_to(RAIZ)}: {len(datos['productos'])} productos, {SALIDA.stat().st_size // 1024} KB")


if __name__ == "__main__":
    main()
