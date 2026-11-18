"""Precios vigentes de gasolina y diésel por gasolinera, con ubicación.

Fuente: datos abiertos del regulador de energía (antes CRE, ahora CNE), actualizados a diario:
  * places: cada gasolinera (place_id, razón social, coordenadas)
  * prices: precio vigente por place_id (regular = Magna, premium, diesel)

Salida: data/gasolina.json, una gasolinera por línea (así git solo guarda las líneas que cambian):
  {"version":1,"generado":"…","fuente":"…","estaciones":[
  {"i":2039,"a":32.47641,"o":-116.9214,"n":"Estación Hipódromo","r":22.95,"p":27.9,"d":27.0},
  …]}
  i = place_id, a = latitud, o = longitud, n = nombre, r/p/d = regular/premium/diésel en pesos por litro.

Uso:
    python recolector/gasolina.py
"""

from __future__ import annotations

import json
import re
import statistics
import sys
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path

import requests

RAIZ = Path(__file__).resolve().parent.parent
SALIDA = RAIZ / "data" / "gasolina.json"
URL_LUGARES = "https://publicacionexterna.azurewebsites.net/publicaciones/places"
URL_PRECIOS = "https://publicacionexterna.azurewebsites.net/publicaciones/prices"

# Un precio fuera de este rango contra la mediana nacional se considera error de captura.
RANGO_VALIDO = (0.75, 1.35)
TIPOS = {"regular": "r", "premium": "p", "diesel": "d"}

# Formas societarias que no ayudan a reconocer la gasolinera.
SOCIEDADES = re.compile(
    r"[,.]?\s*\b(S\.?\s?A\.?\s?P\.?\s?I\.?|S\.?\s?A\.?\s?B\.?|S\.?\s?A\.?|S\.?\s?DE\s?R\.?\s?L\.?|S\.?\s?C\.?|"
    r"S\.?\s?A\.?\s?S\.?|DE\s+C\.?\s?V\.?|C\.?\s?V\.?)(?=\s|$|[,.])",
    re.IGNORECASE,
)
MINUSCULAS = {"de", "del", "la", "las", "los", "y", "e", "el", "en"}


def limpiar_nombre(nombre: str) -> str:
    """"SERVICIOS GASOLINEROS DE MEXICO SA DE CV" -> "Servicios Gasolineros de Mexico"."""
    n = SOCIEDADES.sub(" ", nombre or "")
    n = re.sub(r"\s+", " ", n).strip(" ,.-")
    palabras = [p.capitalize() if i == 0 or p.lower() not in MINUSCULAS else p.lower()
                for i, p in enumerate(n.split(" "))]
    return " ".join(palabras) or "Gasolinera"


def descargar(url: str) -> ET.Element:
    r = requests.get(url, timeout=180, headers={"User-Agent": "Mozilla/5.0"})
    r.raise_for_status()
    return ET.fromstring(r.content)


def main() -> None:
    lugares = {}
    for p in descargar(URL_LUGARES):
        try:
            lugares[p.get("place_id")] = (
                float(p.findtext("location/y")), float(p.findtext("location/x")), limpiar_nombre(p.findtext("name")),
            )
        except (TypeError, ValueError):
            continue

    precios: dict[str, dict[str, float]] = {}
    for p in descargar(URL_PRECIOS):
        valores = {}
        for g in p.findall("gas_price"):
            try:
                valores[g.get("type")] = float(g.text)
            except (TypeError, ValueError):
                pass
        precios[p.get("place_id")] = valores

    medianas = {t: statistics.median(v[t] for v in precios.values() if t in v) for t in TIPOS}
    estaciones = []
    descartados = 0
    for pid, valores in precios.items():
        if pid not in lugares:
            continue
        lat, lon, nombre = lugares[pid]
        if not (14 <= lat <= 33 and -118 <= lon <= -86):  # coordenadas fuera de México
            continue
        e = {"i": int(pid), "a": round(lat, 5), "o": round(lon, 5), "n": nombre}
        for tipo, clave in TIPOS.items():
            v = valores.get(tipo)
            if v is None:
                continue
            if RANGO_VALIDO[0] * medianas[tipo] <= v <= RANGO_VALIDO[1] * medianas[tipo]:
                e[clave] = round(v, 2)
            else:
                descartados += 1
        if any(k in e for k in TIPOS.values()):
            estaciones.append(e)
    estaciones.sort(key=lambda e: e["i"])

    cabecera = {
        "version": 1,
        "generado": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "fuente": "Comisión Nacional de Energía (antes CRE). Precios vigentes reportados por cada gasolinera.",
    }
    lineas = ",\n".join(json.dumps(e, ensure_ascii=False, separators=(",", ":")) for e in estaciones)
    texto = json.dumps(cabecera, ensure_ascii=False, separators=(",", ":"))[:-1] + ',"estaciones":[\n' + lineas + "\n]}\n"
    json.loads(texto)  # valida antes de escribir
    SALIDA.write_text(texto, encoding="utf-8")
    print(f"{SALIDA.relative_to(RAIZ)}: {len(estaciones)} gasolineras, {descartados} precios atípicos descartados, "
          f"medianas {', '.join(f'{t} ${m:.2f}' for t, m in medianas.items())}, {SALIDA.stat().st_size // 1024} KB",
          file=sys.stdout)


if __name__ == "__main__":
    main()
