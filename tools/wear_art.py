"""What players wear: the Cord on the wrist, drawn by the Cord layer.

Writes textures/entity/cord/<tier>.png (the band, 32x16, one per Cord tier) and
textures/entity/cord/bead.png (8x8, white, tinted in game by each rune's colour).
Run from the project root:  python tools/wear_art.py   (generate_assets.py runs it too)
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/wildercord/textures/entity/cord"

# Each tier's cord: its main colour and its highlight, matching the Cord items.
TIERS = {
    "twine": ((176, 142, 98), (214, 186, 140)),
    "copper": ((196, 110, 64), (236, 160, 110)),
    "amethyst": ((122, 78, 196), (180, 140, 236)),
    "echo": ((24, 78, 88), (60, 150, 160)),
}


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def band(main, light):
    """A twisted cord: diagonal strands, lighter on top, darker in the grooves."""
    img = Image.new("RGBA", (32, 16), (0, 0, 0, 0))
    px = img.load()
    dark = mix(main, (0, 0, 0), 0.35)
    for y in range(16):
        for x in range(32):
            strand = (x + y * 2) % 4
            c = light if strand == 0 else main if strand in (1, 2) else dark
            px[x, y] = c + (255,)
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


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for tier, (main_c, light) in TIERS.items():
        band(main_c, light).save(OUT / f"{tier}.png")
    bead().save(OUT / "bead.png")
    print("cord textures written")


if __name__ == "__main__":
    main()
