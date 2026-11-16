"""Recolector de precios de frutas y hortalizas del SNIIM.

Descarga los precios de mayoreo (pesos por kilogramo calculado) de todas las
centrales de abasto de recolector/mercados.json y genera:

  data/mercados/<id>.json   historial semanal (52 semanas) de un mercado
  data/indice.json          lista de mercados, catálogo de productos y último
                            precio de cada producto en cada mercado

Uso:
    python recolector/sniim.py                        # últimas 3 semanas (uso semanal)
    python recolector/sniim.py --desde 2025-11-03     # carga histórica
    python recolector/sniim.py --mercados 150 151     # solo algunos mercados
"""

from __future__ import annotations

import argparse
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
CATALOGO_MERCADOS = RAIZ / "recolector" / "mercados.json"
DIR_MERCADOS = RAIZ / "data" / "mercados"
INDICE = RAIZ / "data" / "indice.json"

URL_RESULTADOS = (
    "http://www.economia-sniim.gob.mx/nuevo/Consultas/MercadosNacionales/"
    "PreciosDeMercado/Agricolas/ResultadosConsultaFechaFrutasYHortalizas.aspx"
)

TAMANO_BLOQUE_DIAS = 31      # días por petición en cargas históricas
SEMANAS_GUARDADAS = 52
SEMANAS_VIGENCIA = 4         # un precio más viejo que esto no entra al índice
VERSION = 2


# ---------------------------------------------------------------- descarga

def descargar(mercado_id: str, inicio: date, fin: date, sesion: requests.Session) -> list[dict]:
    """Todas las filas de un mercado entre dos fechas (inclusive)."""
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
            return parsear(r.content)
        except requests.RequestException as e:
            print(f"  aviso: intento {intento + 1} falló ({e})", file=sys.stderr)
            time.sleep(5 * (intento + 1))
    raise RuntimeError(f"No se pudo descargar mercado {mercado_id} {inicio}..{fin}")


def _num(texto: str) -> float | None:
    try:
        return float(texto.replace(",", "").strip())
    except ValueError:
        return None


def parsear(html: bytes) -> list[dict]:
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
        if frec is None or frec <= 0:
            continue
        filas.append({
            "fecha": fecha,
            "categoria": categoria,
            "producto": d.get("Producto", ""),
            "calidad": d.get("Calidad", ""),
            "min": _num(d.get("Precio Mín", "")),
            "max": _num(d.get("Precio Max", "")),
            "frec": frec,
        })
    return filas


# ---------------------------------------------------------------- agregados

def slug(texto: str) -> str:
    t = unicodedata.normalize("NFKD", texto).encode("ascii", "ignore").decode()
    return re.sub(r"[^a-z0-9]+", "-", t.lower()).strip("-")


def semana_iso(d: date) -> str:
    a, s, _ = d.isocalendar()
    return f"{a}-W{s:02d}"


def lunes(d: date) -> date:
    return d - timedelta(days=d.weekday())


def agregar_semanas(filas: list[dict]) -> tuple[dict, dict]:
    """Devuelve ({producto_id: {semana: datos}}, {producto_id: info})."""
    grupos: dict[tuple[str, str], list[dict]] = defaultdict(list)
    info: dict[str, dict] = {}
    for f in filas:
        pid = slug(f"{f['producto']} {f['calidad']}")
        info[pid] = {"id": pid, "nombre": f["producto"], "calidad": f["calidad"], "categoria": f["categoria"]}
        grupos[(pid, semana_iso(f["fecha"]))].append(f)

    semanas: dict[str, dict] = defaultdict(dict)
    for (pid, semana), fs in grupos.items():
        mins = [f["min"] for f in fs if f["min"] is not None]
        maxs = [f["max"] for f in fs if f["max"] is not None]
        semanas[pid][semana] = {
            "semana": semana,
            "min": round(min(mins), 2) if mins else None,
            "max": round(max(maxs), 2) if maxs else None,
            "frec": round(statistics.median(f["frec"] for f in fs), 2),
            "dias": len({f["fecha"] for f in fs}),
        }
    return semanas, info


def leer_mercado(mid: str) -> tuple[dict, dict, str]:
    """Historial guardado de un mercado: ({pid: {semana: datos}}, {pid: info}, ultima_fecha)."""
    archivo = DIR_MERCADOS / f"{mid}.json"
    if not archivo.exists():
        return defaultdict(dict), {}, ""
    datos = json.loads(archivo.read_text(encoding="utf-8"))
    semanas: dict[str, dict] = defaultdict(dict)
    info = {}
    for p in datos["productos"]:
        info[p["id"]] = {k: p[k] for k in ("id", "nombre", "calidad", "categoria")}
        for s in p["semanas"]:
            semanas[p["id"]][s["semana"]] = s
    return semanas, info, datos.get("ultimaFecha", "")


def guardar_mercado(mid: str, semanas: dict, info: dict, ultima_fecha: str) -> dict:
    todas = sorted({s for ps in semanas.values() for s in ps})[-SEMANAS_GUARDADAS:]
    validas = set(todas)
    productos = []
    for pid, ps in semanas.items():
        historia = [ps[s] for s in sorted(ps) if s in validas]
        if historia:
            productos.append({**info[pid], "semanas": historia})
    productos.sort(key=lambda p: (p["categoria"], p["nombre"], p["calidad"]))
    salida = {
        "version": VERSION,
        "mercado": mid,
        "generado": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "ultimaFecha": ultima_fecha,
        "unidad": "MXN/kg",
        "semanas": todas,
        "productos": productos,
    }
    DIR_MERCADOS.mkdir(parents=True, exist_ok=True)
    escribir_json(DIR_MERCADOS / f"{mid}.json", salida)
    return salida


def generar_indice(mercados: list[dict]) -> dict:
    """Catálogo + último precio por producto y mercado (para comparar y elegir mercado)."""
    hoy_iso = semana_iso(date.today() - timedelta(weeks=SEMANAS_VIGENCIA))
    productos: dict[str, dict] = {}
    ultimos: dict[str, dict[str, float]] = defaultdict(dict)
    con_datos = []
    ultima_semana = ""
    for m in mercados:
        archivo = DIR_MERCADOS / f"{m['id']}.json"
        if not archivo.exists():
            continue
        datos = json.loads(archivo.read_text(encoding="utf-8"))
        if not datos["productos"]:
            continue
        sem = datos["semanas"][-1] if datos["semanas"] else ""
        con_datos.append({**m, "ultimaSemana": sem, "ultimaFecha": datos.get("ultimaFecha", ""),
                          "productos": len(datos["productos"])})
        ultima_semana = max(ultima_semana, sem)
        for p in datos["productos"]:
            productos.setdefault(p["id"], {k: p[k] for k in ("id", "nombre", "calidad", "categoria")})
            u = p["semanas"][-1]
            if u["semana"] >= hoy_iso:
                ultimos[p["id"]][m["id"]] = u["frec"]
    indice = {
        "version": VERSION,
        "generado": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "fuente": "SNIIM - Secretaría de Economía. Precios de mayoreo, pesos por kg (calculado).",
        "unidad": "MXN/kg",
        "ultimaSemana": ultima_semana,
        "mercados": con_datos,
        "productos": sorted(productos.values(), key=lambda p: (p["nombre"], p["calidad"])),
        "ultimos": ultimos,
    }
    escribir_json(INDICE, indice)
    return indice


def escribir_json(ruta: Path, datos: dict) -> None:
    ruta.write_text(json.dumps(datos, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")


# ---------------------------------------------------------------- main

def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--desde", type=date.fromisoformat, help="fecha inicial AAAA-MM-DD (se ajusta al lunes)")
    ap.add_argument("--hasta", type=date.fromisoformat, default=date.today(), help="fecha final AAAA-MM-DD")
    ap.add_argument("--mercados", nargs="*", help="ids de mercado (por defecto, todos)")
    ap.add_argument("--solo-indice", action="store_true", help="no descargar, solo regenerar indice.json")
    args = ap.parse_args()

    mercados = json.loads(CATALOGO_MERCADOS.read_text(encoding="utf-8"))

    if not args.solo_indice:
        # Siempre desde un lunes, para que cada semana descargada quede completa.
        desde = lunes(args.desde or (args.hasta - timedelta(weeks=2)))
        sesion = requests.Session()
        sesion.headers["User-Agent"] = "Mozilla/5.0"
        elegidos = [m for m in mercados if not args.mercados or m["id"] in args.mercados]
        for m in elegidos:
            filas: list[dict] = []
            inicio = desde
            while inicio <= args.hasta:
                fin = min(inicio + timedelta(days=TAMANO_BLOQUE_DIAS - 1), args.hasta)
                filas += descargar(m["id"], inicio, fin, sesion)
                inicio = fin + timedelta(days=1)
                time.sleep(1)  # ser amables con el servidor
            semanas, info, ultima_fecha = leer_mercado(m["id"])
            if filas:
                ultima_fecha = max(ultima_fecha, max(f["fecha"] for f in filas).isoformat())
            nuevas, info_nueva = agregar_semanas(filas)
            info.update(info_nueva)
            for pid, ps in nuevas.items():
                semanas[pid].update(ps)  # las semanas descargadas reemplazan a las guardadas
            if not semanas:
                print(f"{m['ciudad']} ({m['id']}): sin datos")
                continue
            salida = guardar_mercado(m["id"], semanas, info, ultima_fecha)
            print(f"{m['ciudad']} ({m['id']}): {len(filas)} filas -> "
                  f"{len(salida['productos'])} productos, {len(salida['semanas'])} semanas")

    indice = generar_indice(mercados)
    print(f"Índice: {len(indice['mercados'])} mercados, {len(indice['productos'])} productos, "
          f"última semana {indice['ultimaSemana']}, {INDICE.stat().st_size / 1024:.0f} KB")


if __name__ == "__main__":
    main()
