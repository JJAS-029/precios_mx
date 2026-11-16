"""Genera los íconos de la app a partir de diseno/logo_original.jpg.

    .venv/Scripts/python -m pip install pillow
    .venv/Scripts/python diseno/generar_iconos.py

Salidas:
  * android/.../mipmap-*/ic_launcher_foreground.png  capa frontal del ícono adaptativo (koala recortado en círculo)
  * android/.../drawable-nodpi/logo_koala.png        logo pequeño para usar dentro de la app
  * diseno/play_store_512.png                        ícono de alta resolución para Google Play (logo completo)
"""

from pathlib import Path

from PIL import Image, ImageDraw

RAIZ = Path(__file__).resolve().parent.parent
LOGO = RAIZ / "diseno" / "logo_original.jpg"
RES = RAIZ / "android" / "app" / "src" / "main" / "res"

# Círculo (en píxeles del logo original) que contiene al koala con sus verduras, sin el texto.
CENTRO = (370, 420)
RADIO = 235

# Ícono adaptativo: lienzo de 108 dp; el launcher muestra aprox. el círculo central de 72 dp.
DENSIDADES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}
LIENZO_DP = 108
CONTENIDO_DP = 72


def koala_circular(tamano: int) -> Image.Image:
    """Recorte circular del koala, con transparencia fuera del círculo."""
    logo = Image.open(LOGO).convert("RGBA")
    x, y = CENTRO
    recorte = logo.crop((x - RADIO, y - RADIO, x + RADIO, y + RADIO)).resize((tamano, tamano), Image.LANCZOS)
    # Máscara con antialiasing: se dibuja 4x más grande y se reduce.
    grande = Image.new("L", (tamano * 4, tamano * 4), 0)
    ImageDraw.Draw(grande).ellipse((0, 0, tamano * 4 - 1, tamano * 4 - 1), fill=255)
    recorte.putalpha(grande.resize((tamano, tamano), Image.LANCZOS))
    return recorte


def main() -> None:
    for nombre, escala in DENSIDADES.items():
        lienzo = round(LIENZO_DP * escala)
        contenido = round(CONTENIDO_DP * escala)
        img = Image.new("RGBA", (lienzo, lienzo), (0, 0, 0, 0))
        offset = (lienzo - contenido) // 2
        img.alpha_composite(koala_circular(contenido), (offset, offset))
        destino = RES / f"mipmap-{nombre}" / "ic_launcher_foreground.png"
        destino.parent.mkdir(parents=True, exist_ok=True)
        img.save(destino, optimize=True)
        print(destino.relative_to(RAIZ))

    nodpi = RES / "drawable-nodpi"
    nodpi.mkdir(parents=True, exist_ok=True)
    koala_circular(192).save(nodpi / "logo_koala.png", optimize=True)
    print((nodpi / "logo_koala.png").relative_to(RAIZ))

    # Google Play pide 512x512 PNG; se usa el logo completo sobre fondo blanco.
    logo = Image.open(LOGO).convert("RGB")
    lado = max(logo.size)
    cuadrado = Image.new("RGB", (lado, lado), "white")
    cuadrado.paste(logo, ((lado - logo.width) // 2, (lado - logo.height) // 2))
    cuadrado.resize((512, 512), Image.LANCZOS).save(RAIZ / "diseno" / "play_store_512.png", optimize=True)
    print("diseno/play_store_512.png")


if __name__ == "__main__":
    main()
