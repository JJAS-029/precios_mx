"""Recolector de precios de frutas y hortalizas del SNIIM.

Descarga los precios de mayoreo (en pesos por kilogramo calculado) de las
centrales de abasto configuradas, los acumula en un CSV diario y genera
`data/precios.json` con agregados semanales para la app.

Uso:
    python recolector/sniim.py                      # últimos 14 días (uso semanal)
    python recolector/sniim.py --desde 2025-11-01   # carga histórica
    python recolector/sniim.py --solo-json          # regenerar JSON sin descargar
"""

from __future__ import annotations

import argparse
import csv
import json
import re
import statistics
import sys
import time
import unicodedata
from collections import defaultdict
from datetime import date, datetime, timedelta, timezone
from pathlib import Path

import requests
from bs4 import BeautifulSoup

RAIZ = Path(__file__).resolve().parent.parent
CSV_DIARIO = RAIZ / "data" / "sniim_diario.csv"
JSON_SALIDA = RAIZ / "data" / "precios.json"

URL_RESULTADOS = (
    "http://www.economia-sniim.gob.mx/nuevo/Consultas/MercadosNacionales/"
    "PreciosDeMercado/Agricolas/ResultadosConsultaFechaFrutasYHortalizas.aspx"
)

# DestinoId del SNIIM -> nombre corto. Ver ddlDestino en ConsultaFrutasYHortalizas.aspx
MERCADOS = {
    "150": "Central de Abasto de Toluca",
    "151": "Central de Abasto de Ecatepec",
    "100": "Central de Abasto de Iztapalapa (CDMX)",
}

CAMPOS = ["fecha", "mercado", "categoria", "producto", "calidad",
          "presentacion", "origen", "min", "max", "frec", "obs"]

# Días por petición en cargas históricas (evita respuestas enormes).
TAMANO_BLOQUE_DIAS = 31
SEMANAS_EN_JSON = 52


# ---------------------------------------------------------------- descarga

def descargar(mercado_id: str, inicio: date, fin: date, sesion: requests.Session) -> list[dict]:
    """Devuelve todas las filas de un mercado entre dos fechas (inclusive)."""
    params = {
        "fechaInicio": inicio.strftime("%d/%m/%Y"),
        "fechaFinal": fin.strftime("%d/%m/%Y"),
        "ProductoId": "-1",
        "OrigenId": "-1",
        "Origen": "Todos",
        "DestinoId": mercado_id,
        "Destino": "",
        "PreciosPorId": "2",           # 2 = Kilogramo (calculado)
        "RegistrosPorPagina": "10000",
    }
    for intento in range(3):
        try:
            r = sesion.get(URL_RESULTADOS, params=params, timeout=120)
            r.raise_for_status()
            return parsear(r.content, mercado_id)
        except requests.RequestException as e:
            print(f"  aviso: intento {intento + 1} falló ({e})", file=sys.stderr)
            time.sleep(5 * (intento + 1))
    raise RuntimeError(f"No se pudo descargar mercado {mercado_id} {inicio}..{fin}")


def _num(texto: str) -> float | None:
    texto = texto.replace(",", "").strip()
    try:
        return float(texto)
    except ValueError:
        return None


def parsear(html: bytes, mercado_id: str) -> list[dict]:
    soup = BeautifulSoup(html, "html.parser")
    tabla = soup.find("table", id="tblResultados")
    if tabla is None:
        return []

    filas: list[dict] = []
    categoria = ""
    encabezado: list[str] | None = None
    for tr in tabla.find_all("tr"):
        celdas = [c.get_text(" ", strip=True) for c in tr.find_all(["td", "th"])]
        if not celdas:
            continue
        if encabezado is None and "Fecha" in celdas:
            encabezado = celdas
            continue
        if len(celdas) == 1:          # fila de categoría: "Frutas", "Hortalizas"...
            categoria = celdas[0]
            continue
        if encabezado is None or len(celdas) != len(encabezado):
            continue

        d = dict(zip(encabezado, celdas))
        try:
            fecha = datetime.strptime(d["Fecha"], "%d/%m/%Y").date()
        except (KeyError, ValueError):
            continue
        frec = _num(d.get("Precio Frec", ""))
        if frec is None:
            continue
        filas.append({
            "fecha": fecha.isoformat(),
            "mercado": mercado_id,
            "categoria": categoria,
            "producto": d.get("Producto", ""),
            "calidad": d.get("Calidad", ""),
            "presentacion": d.get("Presentación", ""),
            "origen": d.get("Origen", ""),
            "min": _num(d.get("Precio Mín", "")),
            "max": _num(d.get("Precio Max", "")),
            "frec": frec,
            "obs": d.get("Obs.", ""),
        })
    return filas


# ---------------------------------------------------------------- CSV diario

def _clave(f: dict) -> tuple:
    return (f["fecha"], f["mercado"], f["producto"], f["calidad"],
            f["presentacion"], f["origen"], f["obs"])


def leer_csv() -> dict[tuple, dict]:
    if not CSV_DIARIO.exists():
        return {}
    with CSV_DIARIO.open(encoding="utf-8", newline="") as fh:
        filas = {}
        for f in csv.DictReader(fh):
            for k in ("min", "max", "frec"):
                f[k] = float(f[k]) if f[k] not in ("", None) else None
            filas[_clave(f)] = f
        return filas


def guardar_csv(filas: dict[tuple, dict]) -> None:
    CSV_DIARIO.parent.mkdir(parents=True, exist_ok=True)
    orden = sorted(filas.values(), key=lambda f: (f["fecha"], f["mercado"], f["producto"], f["calidad"]))
    with CSV_DIARIO.open("w", encoding="utf-8", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=CAMPOS, lineterminator="\n")
        w.writeheader()
        w.writerows(orden)


# ---------------------------------------------------------------- JSON semanal

def slug(texto: str) -> str:
    t = unicodedata.normalize("NFKD", texto).encode("ascii", "ignore").decode()
    return re.sub(r"[^a-z0-9]+", "-", t.lower()).strip("-")


def semana_iso(fecha: str) -> str:
    a, s, _ = date.fromisoformat(fecha).isocalendar()
    return f"{a}-W{s:02d}"


def generar_json(filas: dict[tuple, dict]) -> dict:
    # (producto_id, mercado, semana) -> lista de filas
    grupos: dict[tuple, list[dict]] = defaultdict(list)
    info: dict[str, dict] = {}
    for f in filas.values():
        pid = slug(f"{f['producto']} {f['calidad']}")
        info.setdefault(pid, {"id": pid, "nombre": f["producto"],
                              "calidad": f["calidad"], "categoria": f["categoria"]})
        grupos[(pid, f["mercado"], semana_iso(f["fecha"]))].append(f)

    semanas_validas = sorted({k[2] for k in grupos})[-SEMANAS_EN_JSON:]
    validas = set(semanas_validas)

    historia: dict[str, list[dict]] = defaultdict(list)
    for (pid, mercado, semana), fs in grupos.items():
        if semana not in validas:
            continue
        mins = [f["min"] for f in fs if f["min"] is not None]
        maxs = [f["max"] for f in fs if f["max"] is not None]
        historia[pid].append({
            "semana": semana,
            "mercado": mercado,
            "min": round(min(mins), 2) if mins else None,
            "max": round(max(maxs), 2) if maxs else None,
            "frec": round(statistics.median(f["frec"] for f in fs), 2),
            "dias": len({f["fecha"] for f in fs}),
        })

    productos = []
    for pid, h in historia.items():
        p = dict(info[pid])
        p["semanas"] = sorted(h, key=lambda x: (x["semana"], x["mercado"]))
        productos.append(p)
    productos.sort(key=lambda p: (p["categoria"], p["nombre"], p["calidad"]))

    return {
        "version": 1,
        "generado": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "fuente": "SNIIM - Secretaría de Economía. Precios de mayoreo, pesos por kg (calculado).",
        "unidad": "MXN/kg",
        "mercados": MERCADOS,
        "semanas": semanas_validas,
        "productos": productos,
    }


# ---------------------------------------------------------------- main

def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--desde", type=date.fromisoformat, help="fecha inicial AAAA-MM-DD")
    ap.add_argument("--hasta", type=date.fromisoformat, default=date.today(), help="fecha final AAAA-MM-DD")
    ap.add_argument("--solo-json", action="store_true", help="no descargar, solo regenerar el JSON")
    args = ap.parse_args()

    filas = leer_csv()
    antes = len(filas)

    if not args.solo_json:
        desde = args.desde or (args.hasta - timedelta(days=14))
        sesion = requests.Session()
        sesion.headers["User-Agent"] = "Mozilla/5.0"
        for mid, nombre in MERCADOS.items():
            inicio = desde
            while inicio <= args.hasta:
                fin = min(inicio + timedelta(days=TAMANO_BLOQUE_DIAS - 1), args.hasta)
                nuevas = descargar(mid, inicio, fin, sesion)
                print(f"{nombre}: {inicio} a {fin} -> {len(nuevas)} registros")
                for f in nuevas:
                    filas[_clave(f)] = f
                inicio = fin + timedelta(days=1)
                time.sleep(1)  # ser amables con el servidor
        guardar_csv(filas)
        print(f"CSV diario: {antes} -> {len(filas)} registros")

    salida = generar_json(filas)
    JSON_SALIDA.parent.mkdir(parents=True, exist_ok=True)
    JSON_SALIDA.write_text(json.dumps(salida, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(f"JSON: {len(salida['productos'])} productos, {len(salida['semanas'])} semanas, "
          f"{JSON_SALIDA.stat().st_size / 1024:.0f} KB")


if __name__ == "__main__":
    main()
