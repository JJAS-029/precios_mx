from materialyoucolor.scheme.scheme_content import SchemeContent as SchemeTonalSpot
from materialyoucolor.dynamiccolor.material_dynamic_colors import MaterialDynamicColors as M
from materialyoucolor.hct import Hct
SEMILLAS = {"Jitomate": 0xFFC62828, "Mandarina": 0xFFEF6C00, "Jamaica": 0xFF8E2C6E, "MaizAzul": 0xFF3949AB}
ROLES = ["primary","onPrimary","primaryContainer","onPrimaryContainer","secondary","onSecondary","secondaryContainer","onSecondaryContainer","tertiary","onTertiary","tertiaryContainer","onTertiaryContainer","background","onBackground","surface","onSurface","surfaceVariant","onSurfaceVariant","outline","outlineVariant","inverseSurface","inverseOnSurface","inversePrimary","surfaceContainerLowest","surfaceContainerLow","surfaceContainer","surfaceContainerHigh","surfaceContainerHighest"]
for nombre, seed in SEMILLAS.items():
    for oscuro in (False, True):
        s = SchemeTonalSpot(Hct.from_int(seed), oscuro, 0.0)
        partes = []
        for r in ROLES:
            argb = getattr(M, r).get_argb(s) if hasattr(M, r) else getattr(M, r)().get_argb(s)
            partes.append(f"{r} = Color(0x{argb & 0xFFFFFFFF:08X})")
        fn = "darkColorScheme" if oscuro else "lightColorScheme"
        print(f"private val {nombre}{'Oscuro' if oscuro else 'Claro'} = {fn}(\n    " + ",\n    ".join(partes) + ",\n)\n")
