"""Genera los íconos de la app a partir de diseno/logo_original.jpg.

    .venv/Scripts/python -m pip install pillow
    .venv/Scripts/python diseno/generar_iconos.py

Salidas:
  * android/.../mipmap-*/ic_launcher_foreground.png  capa frontal del ícono adaptativo (insignia completa)
  * android/.../drawable-nodpi/logo_koala.png        logo de la barra superior (insignia completa)
  * diseno/play_store_512.png                        ícono de alta resolución para Google Play (insignia completa)
"""

from pathlib import Path

from PIL import Image, ImageDraw

RAIZ = Path(__file__).resolve().parent.parent
LOGO = RAIZ / "diseno" / "logo_original.jpg"
RES = RAIZ / "android" / "app" / "src" / "main" / "res"

# Insignia completa (aro verde con "¿A CÓMO?"): círculo exterior en píxeles del logo original.
CENTRO_INSIGNIA = (370, 353)
RADIO_INSIGNIA = 346

# Ícono adaptativo: lienzo de 108 dp; el launcher muestra 72 dp y recorta con la forma del teléfono
# (círculo, cuadrado redondeado...). Solo el círculo central de 66 dp está garantizado: la insignia
# va justo de ese tamaño para que el aro nunca se corte.
DENSIDADES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}
LIENZO_DP = 108
INSIGNIA_DP = 66


def insignia(tamano: int) -> Image.Image:
    """Logo completo recortado al círculo exterior, con transparencia fuera del aro."""
    logo = Image.open(LOGO).convert("RGBA")
    x, y = CENTRO_INSIGNIA
    r = RADIO_INSIGNIA
    recorte = logo.crop((x - r, y - r, x + r, y + r)).resize((tamano, tamano), Image.LANCZOS)
    grande = Image.new("L", (tamano * 4, tamano * 4), 0)
    ImageDraw.Draw(grande).ellipse((0, 0, tamano * 4 - 1, tamano * 4 - 1), fill=255)
    recorte.putalpha(grande.resize((tamano, tamano), Image.LANCZOS))
    return recorte


def main() -> None:
    for nombre, escala in DENSIDADES.items():
        lienzo = round(LIENZO_DP * escala)
        contenido = round(INSIGNIA_DP * escala)
        # Fondo blanco opaco en la capa frontal: así el launcher no tiñe el espacio alrededor del aro.
        img = Image.new("RGBA", (lienzo, lienzo), (255, 255, 255, 255))
        offset = (lienzo - contenido) // 2
        img.alpha_composite(insignia(contenido), (offset, offset))
        destino = RES / f"mipmap-{nombre}" / "ic_launcher_foreground.png"
        destino.parent.mkdir(parents=True, exist_ok=True)
        img.save(destino, optimize=True)
        print(destino.relative_to(RAIZ))

    nodpi = RES / "drawable-nodpi"
    nodpi.mkdir(parents=True, exist_ok=True)
    # Logo de la barra superior: la misma insignia completa que el ícono, con fondo transparente.
    insignia(192).save(nodpi / "logo_koala.png", optimize=True)
    print((nodpi / "logo_koala.png").relative_to(RAIZ))

    # Google Play pide 512x512 PNG y lo muestra con esquinas redondeadas: insignia al 88 % con margen blanco.
    play = Image.new("RGBA", (512, 512), (255, 255, 255, 255))
    lado = round(512 * 0.88)
    play.alpha_composite(insignia(lado), ((512 - lado) // 2, (512 - lado) // 2))
    play.convert("RGB").save(RAIZ / "diseno" / "play_store_512.png", optimize=True)
    print("diseno/play_store_512.png")


if __name__ == "__main__":
    main()
