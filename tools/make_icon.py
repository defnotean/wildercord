"""Wildercord's icon, drawn pixel by pixel on a 64x64 grid and upscaled nearest-neighbour to 640x640.

The picture: a braided amethyst cord arcing across a night tile, strung with four rune beads
(fire, frost, storm, life), each etched with its glyph and glowing, over a faint magic circle.
Everything is deterministic so the file can be rebuilt and reviewed.

    python tools/make_icon.py

Writes src/main/resources/assets/wildercord/icon.png (640x640) and, with --store <dir>, a
640x640 copy for the store uploaders.
"""
import argparse
import colorsys
from pathlib import Path

from PIL import Image

SIZE = 64
ROOT = Path(__file__).resolve().parent.parent
ICON = ROOT / "src/main/resources/assets/wildercord/icon.png"

BG_TOP = (0x12, 0x10, 0x1F)
BG_BOTTOM = (0x20, 0x1A, 0x38)
STAR_DIM = (0x4E, 0x48, 0x78)
STAR_BRIGHT = (0xCF, 0xCB, 0xF0)
CIRCLE = (0x38, 0x30, 0x6A)
CIRCLE_TICK = (0x4A, 0x3F, 0x88)
CORD_HI = (0xB4, 0x87, 0xF0)
CORD_MID = (0x7A, 0x4F, 0xB8)
CORD_LO = (0x47, 0x2A, 0x75)
BEAD_BODY = (0x45, 0x3F, 0x6B)
BEAD_HI = (0x6B, 0x64, 0x94)
BEAD_LO = (0x2B, 0x27, 0x47)
BEAD_OUTLINE = (0x0C, 0x0A, 0x16)
WHITE = (0xFF, 0xF6, 0xE8)

ELEMENTS = {
    "fire": ((0xFF, 0x8A, 0x3D), (0xB3, 0x4E, 0x14)),
    "frost": ((0x8F, 0xE3, 0xFF), (0x3E, 0x8F, 0xBF)),
    "storm": ((0xFF, 0xD7, 0x5E), (0xB8, 0x86, 0x0B)),
    "life": ((0x8F, 0xE3, 0x6B), (0x3E, 0x8F, 0x3E)),
}

# 5x5 glyphs, (dx, dy) from the bead centre.
GLYPHS = {
    "fire": [(0, -2), (1, -2), (-1, -1), (0, -1), (1, -1), (-2, 0), (-1, 0), (0, 0), (1, 0), (2, 0), (-1, 1), (0, 1), (1, 1), (0, 2)],
    "frost": [(0, -2), (0, -1), (-2, 0), (-1, 0), (0, 0), (1, 0), (2, 0), (0, 1), (0, 2), (-1, -1), (1, -1), (-1, 1), (1, 1)],
    "storm": [(1, -2), (0, -1), (1, -1), (0, 0), (-1, 0), (0, 1), (-1, 2)],
    "life": [(0, -2), (-1, -1), (0, -1), (1, -1), (0, 0), (0, 1), (0, 2), (-1, 2)],
}

# Fixed star field: (x, y, bright?) so the tile is the same every build.
STARS = [(5, 6, 0), (12, 3, 1), (21, 9, 0), (30, 4, 0), (41, 7, 1), (52, 5, 0), (58, 12, 0),
         (3, 20, 0), (9, 33, 0), (56, 27, 1), (60, 38, 0), (7, 45, 0), (16, 58, 0), (28, 60, 0),
         (45, 57, 1), (55, 50, 0), (36, 14, 0), (48, 44, 0)]


def blend(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


class Tile:
    def __init__(self):
        self.pixels = {}

    def put(self, x, y, colour):
        x, y = round(x), round(y)
        if 0 <= x < SIZE and 0 <= y < SIZE:
            self.pixels[(x, y)] = colour

    def over(self, x, y, colour, alpha):
        """Paint with alpha over whatever is already there."""
        x, y = round(x), round(y)
        if not (0 <= x < SIZE and 0 <= y < SIZE):
            return
        base = self.pixels.get((x, y), BG_TOP)
        self.pixels[(x, y)] = blend(base, colour, alpha)

    def ring(self, cx, cy, radius, colour, ticks=0):
        """A one-pixel circle; with ticks, short radial marks every 30 degrees."""
        for step in range(0, 360 * 4):
            angle = step / 4.0
            rad = angle * 3.141592653589793 / 180.0
            x, y = cx + radius * __import__("math").cos(rad), cy + radius * __import__("math").sin(rad)
            self.over(x, y, colour, 0.9)
            if ticks and int(angle) % 30 == 0 and step % 4 == 0:
                self.over(x + 1.4 * __import__("math").cos(rad), y + 1.4 * __import__("math").sin(rad), colour, 0.9)


def background(tile):
    for y in range(SIZE):
        for x in range(SIZE):
            tile.pixels[(x, y)] = blend(BG_TOP, BG_BOTTOM, y / (SIZE - 1))
    for x, y, bright in STARS:
        tile.put(x, y, STAR_BRIGHT if bright else STAR_DIM)
        if bright:
            tile.over(x - 1, y, STAR_DIM, 0.5)
            tile.over(x + 1, y, STAR_DIM, 0.5)
            tile.over(x, y - 1, STAR_DIM, 0.5)
            tile.over(x, y + 1, STAR_DIM, 0.5)


def magic_circle(tile):
    tile.ring(32, 34, 24, CIRCLE)
    tile.ring(32, 34, 20, CIRCLE, ticks=1)
    # Four rune dashes on the inner ring, at the diagonals.
    import math
    for angle in (45, 135, 225, 315):
        rad = angle * math.pi / 180.0
        for step in (-2, -1, 0, 1, 2):
            x = 32 + (20 + step * 0) * math.cos(rad) + step * -math.sin(rad)
            y = 34 + 20 * math.sin(rad) + step * math.cos(rad)
            tile.over(x, y, CIRCLE_TICK, 0.95)


def cord(tile):
    """A braided arc running off both edges of the tile, three pixels thick, with a glowing tip."""
    for x in range(0, 64):
        t = x / 63.0
        y = 53 - 40 * t + 5 * __import__("math").sin(3.141592653589793 * t)
        y = round(y)
        tile.put(x, y - 1, CORD_HI if (x + y) % 4 < 2 else CORD_MID)
        tile.put(x, y, CORD_MID)
        tile.put(x, y + 1, CORD_LO if (x + y) % 4 >= 2 else CORD_MID)
        if x % 6 == 0:  # braid knots
            tile.put(x, y - 1, CORD_HI)
            tile.put(x, y + 1, CORD_LO)
    # The cord leaves the frame at the lower left and ends in a glowing tip at the upper right.
    tile.put(62, 12, CORD_HI)
    tile.put(62, 13, CORD_MID)
    tile.over(61, 11, CORD_HI, 0.6)


def bead(tile, cx, cy, element):
    """An 11x11 rounded rune bead with outline, shading, glow and its glyph."""
    glow, dark = ELEMENTS[element]
    radius = 5.5
    inside = set()
    for dy in range(-6, 7):
        for dx in range(-6, 7):
            if (abs(dx) / radius) ** 3 + (abs(dy) / radius) ** 3 <= 1.0:
                inside.add((cx + dx, cy + dy))
    # Glow: a round halo hugging the bead, and a sparse fainter ring beyond it.
    for dy in range(-9, 10):
        for dx in range(-9, 10):
            x, y = cx + dx, cy + dy
            if (x, y) in inside:
                continue
            dist = (dx * dx + dy * dy) ** 0.5
            if 5.7 <= dist <= 7.1:
                tile.over(x, y, glow, 0.30)
            elif 7.1 < dist <= 8.3 and (x + y) % 2 == 0:
                tile.over(x, y, glow, 0.14)
    # Outline: body pixels' outer neighbours.
    for (x, y) in list(inside):
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, y + dy) not in inside:
                tile.put(x + dx, y + dy, BEAD_OUTLINE)
    # Body with top-left light and bottom-right shade.
    for (x, y) in inside:
        dx, dy = x - cx, y - cy
        if dx + dy <= -4:
            tile.put(x, y, BEAD_HI)
        elif dx + dy >= 5:
            tile.put(x, y, BEAD_LO)
        else:
            tile.put(x, y, BEAD_BODY)
    # Glyph: dark offset, then the element colour, then a hot core.
    for dx, dy in GLYPHS[element]:
        tile.put(cx + dx, cy + dy + 1, dark)
    for dx, dy in GLYPHS[element]:
        tile.put(cx + dx, cy + dy, glow)
    tile.put(cx, cy, WHITE)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--store", type=Path, help="also write a 256x256 copy of the icon here")
    args = parser.parse_args()

    tile = Tile()
    background(tile)
    magic_circle(tile)
    cord(tile)
    for cx, cy, element in ((11, 49, "fire"), (25, 42, "frost"), (39, 33, "storm"), (53, 21, "life")):
        bead(tile, cx, cy, element)

    image = Image.new("RGB", (SIZE, SIZE))
    for (x, y), colour in tile.pixels.items():
        image.putpixel((x, y), colour)
    image.resize((640, 640), Image.NEAREST).save(ICON)
    print(f"wrote {ICON} 640x640 from a {SIZE}x{SIZE} grid")
    if args.store:
        args.store.mkdir(parents=True, exist_ok=True)
        copy = args.store / "wildercord-icon-640.png"
        image.resize((640, 640), Image.NEAREST).save(copy)
        print(f"wrote {copy}")


if __name__ == "__main__":
    main()
