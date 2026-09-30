"""What players wear: the Cord on the wrist, drawn by the Cord layer, and the leather that holds casting
gear on them, drawn by the gear layer.

Writes textures/entity/cord/<tier>.png (the band, 32x16, one per Cord tier) and
textures/entity/cord/bead.png (8x8, white, tinted in game by each rune's colour), and
textures/entity/gear/leather.png (32x16: the belt, with its gold buckle, and the straps).
Run from the project root:  python tools/wear_art.py   (generate_assets.py runs it too)
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/wildercord/textures/entity/cord"
GEAR_OUT = ROOT / "src/main/resources/assets/wildercord/textures/entity/gear"

# Each tier's cord: its main colour and its highlight, matching the Cord items.
TIERS = {
    "twine": ((176, 142, 98), (214, 186, 140)),
    "copper": ((196, 110, 64), (236, 160, 110)),
    "amethyst": ((170, 124, 58), (246, 214, 122)),
    "echo": ((26, 94, 104), (82, 206, 220)),
}


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def band(main, light, tier):
    """Material-specific braiding: fibre, linked copper, gilded amethyst or living echo veins."""
    img = Image.new("RGBA", (32, 16), (0, 0, 0, 0))
    px = img.load()
    dark = mix(main, (0, 0, 0), 0.45)
    for y in range(16):
        for x in range(32):
            strand = (x + y * 2) % 4
            c = light if strand == 0 else main if strand in (1, 2) else dark
            if tier == "copper":
                c = dark if x % 4 == 0 else light if (y + x // 4) % 3 == 0 else main
                if (x + y * 3) % 17 == 0:
                    c = (62, 148, 126)  # oxidation in the link seams
            elif tier == "amethyst":
                if (x + y) % 7 == 0:
                    c = (164, 112, 226)
                elif (x + y) % 7 == 1:
                    c = (84, 48, 152)
            elif tier == "echo":
                c = (8, 48, 60) if strand == 3 else main if strand in (1, 2) else light
                if (x * 3 + y * 5) % 11 == 0:
                    c = (164, 246, 250)
            px[x, y] = c + (255,)
    # Small clasps break up the repeat and catch light on both turns of the wrist.
    clasp = (238, 206, 116) if tier == "amethyst" else (226, 166, 98) if tier == "copper" else (144, 228, 230) if tier == "echo" else (225, 199, 148)
    for y in (2, 8):
        for x in (7, 8):
            px[x, y] = clasp + (255,)
            px[x, y + 1] = dark + (255,)
    return img


def bead():
    """A round bead: bright in the middle, softer at the edge."""
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    px = img.load()
    for y in range(8):
        for x in range(8):
            d = ((x - 3.5) ** 2 + (y - 3.5) ** 2) ** 0.5
            v = max(0.0, 1 - d / 5.2)
            g = round(150 + 105 * v)
            px[x, y] = (g, g, g, 255)
    px[2, 2] = (255, 255, 255, 255)
    return img


def leather():
    """Worn leather: stitched, a little lighter along its top edge. The belt's front face (32x16 sheet, u 4-12,
    v 4-5) carries a gold buckle in the middle; the straps take any part of the sheet."""
    base = (112, 72, 42)
    img = Image.new("RGBA", (32, 16), base + (255,))
    px = img.load()
    for y in range(16):
        for x in range(32):
            c = base
            if (x + y) % 5 == 0:
                c = mix(base, (0, 0, 0), 0.14)
            if y % 2 == 0 and x % 3 == 0:
                c = mix(base, (255, 214, 150), 0.16)
            px[x, y] = c + (255,)
    # The belt's four side faces are each a row 1-2 pixels deep: lit above, shaded below.
    for x in range(32):
        px[x, 4] = mix(base, (255, 214, 150), 0.22) + (255,)
        px[x, 5] = mix(base, (0, 0, 0), 0.3) + (255,)
    for x in (7, 8):
        px[x, 4] = (238, 204, 112, 255)
        px[x, 5] = (176, 136, 58, 255)
    return img


def main():
    GEAR_OUT.mkdir(parents=True, exist_ok=True)
    leather().save(GEAR_OUT / "leather.png")
    OUT.mkdir(parents=True, exist_ok=True)
    for tier, (main_c, light) in TIERS.items():
        band(main_c, light, tier).save(OUT / f"{tier}.png")
    bead().save(OUT / "bead.png")
    print("cord and gear textures written")


if __name__ == "__main__":
    main()
