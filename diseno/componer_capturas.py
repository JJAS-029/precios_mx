# Uso: python diseno/componer_capturas.py <carpeta con raw/ y fonts/Roboto.ttf> diseno/play diseno/play_store_512.png
# raw/: capturas 1080x2400 del emulador (01_lista.png ... 06_oscuro.png). Roboto: /system/fonts/Roboto-Regular.ttf del emulador.
import sys
from PIL import Image, ImageDraw, ImageFont, ImageFilter
S, OUT, LOGO = sys.argv[1], sys.argv[2], sys.argv[3]
FUENTE = S + '/fonts/Roboto.ttf'

def fuente(tam, peso):
    f = ImageFont.truetype(FUENTE, tam)
    f.set_variation_by_axes([peso, 100, 0])
    return f

def centrado(d, y, texto, f, color, ancho):
    w = d.textlength(texto, font=f)
    d.text(((ancho - w) / 2, y), texto, font=f, fill=color)

CLARO = ((205, 237, 163), (16, 32, 0), (53, 78, 22))     # fondo, título, subtítulo
OSCURO = ((26, 28, 22), (205, 237, 163), (197, 200, 186))
CAPTURAS = [
    ('01_lista', '¿Te están cobrando de más?', 'Precios oficiales de mayoreo de tu central', CLARO),
    ('02_semaforo', 'Escribe lo que te cobran', 'y ve al instante si es un precio justo', CLARO),
    ('03_cambia', '¿Subió la papa? Mejor cambia', 'Sustitutos baratos y avisos cuando baje', CLARO),
    ('04_mandado', 'Calcula tu mandado', 'y compártelo por WhatsApp', CLARO),
    ('05_grafica', '¿Cómo ha cambiado?', '12 semanas y mercados cercanos', CLARO),
    ('06_oscuro', 'Más de 40 centrales de abasto', 'Modo oscuro y temas de color', OSCURO),
]
W, H = 1080, 1920
for nombre, titulo, sub, (fondo, ct, cs) in CAPTURAS:
    im = Image.new('RGB', (W, H), fondo)
    d = ImageDraw.Draw(im)
    centrado(d, 92, titulo, fuente(66, 800), ct, W)
    centrado(d, 186, sub, fuente(42, 400), cs, W)
    # teléfono: ancho 880, sale por abajo
    pw, x, y = 880, 100, 300
    cap = Image.open(f'{S}/raw/{nombre}.png').convert('RGB')
    esc = pw / cap.width
    cap = cap.resize((pw, round(cap.height * esc)), Image.LANCZOS)
    alto = H - y + 60
    cap = cap.crop((0, 0, pw, min(alto, cap.height)))
    marco = 18
    # sombra
    sombra = Image.new('L', (W, H), 0)
    ImageDraw.Draw(sombra).rounded_rectangle((x - marco, y - marco + 14, x + pw + marco, H + 200), 72, fill=90)
    sombra = sombra.filter(ImageFilter.GaussianBlur(28))
    im.paste((0, 0, 0), (0, 0), sombra)
    # bisel del teléfono
    ImageDraw.Draw(im).rounded_rectangle((x - marco, y - marco, x + pw + marco, H + 200), 72, fill=(28, 30, 26))
    mascara = Image.new('L', cap.size, 0)
    ImageDraw.Draw(mascara).rounded_rectangle((0, 0, cap.width, cap.height + 200), 56, fill=255)
    im.paste(cap, (x, y), mascara)
    im.save(f'{OUT}/{nombre}.png', optimize=True)

# ---- imagen destacada 1024x500
im = Image.new('RGB', (1024, 500), (76, 102, 43))
g = Image.new('L', (1024, 500))
gd = ImageDraw.Draw(g)
for i in range(1024):
    gd.line((i, 0, i, 500), fill=int(70 * i / 1024))
im.paste((40, 58, 18), (0, 0), g)
d = ImageDraw.Draw(im)
logo = Image.open(LOGO).convert('RGBA').resize((340, 340), Image.LANCZOS)
m = Image.new('L', logo.size, 0)
ImageDraw.Draw(m).rounded_rectangle((0, 0, 340, 340), 80, fill=255)
m = Image.composite(logo.split()[3], Image.new('L', logo.size, 0), m) if logo.mode == 'RGBA' else m
im.paste(logo, (70, 80), m)
d.text((460, 118), '¿A cómo?', font=fuente(104, 900), fill=(255, 255, 255))
d.text((464, 252), '¿Te están cobrando de más?', font=fuente(38, 600), fill=(205, 237, 163))
d.text((464, 304), 'Compara con los precios oficiales', font=fuente(32, 400), fill=(235, 240, 225))
d.text((464, 346), 'de mayoreo de México.', font=fuente(32, 400), fill=(235, 240, 225))
im.save(f'{OUT}/destacada_1024x500.png', optimize=True)
print('listo')
