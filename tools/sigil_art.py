"""Magic circles (the sigil particle) and the GUI sprites added with them.

Sigils are 64x64 white line art on transparency, tinted in game by each circle's colour.
They're drawn pixel by pixel at roughly the texel density of a block seen at circle size,
with a one-pixel soft halo around every line so they glow.

Writes:
    textures/particle/sigil_{circle,ring,star,target,cracked,glow,band,beam}.png   (+ particles/sigil.json)
    textures/gui/sprites/hud/beat_ring.png
    textures/gui/sprites/wheel/{ring,node,node_hover}.png
    textures/gui/sprites/toast/grimoire.png
Run from the project root:  python tools/sigil_art.py [--preview]
"""
import json
import math
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/wildercord"
PARTICLES = ASSETS / "textures/particle"
SPRITES = ASSETS / "textures/gui/sprites"

N = 64
C = (N - 1) / 2

# The GUI palette, shared with gui_art.py.
OUTLINE = (11, 9, 16)
STONE = (27, 23, 38)
STONE_DARK = (18, 15, 26)
BEVEL_LIGHT = (74, 63, 102)
BEVEL_MID = (52, 44, 74)
GOLD_DARK = (122, 92, 46)
GOLD = (176, 138, 62)
GOLD_LIGHT = (232, 196, 106)
GEM_DARK = (80, 40, 140)
GEM = (160, 100, 240)
GEM_LIGHT = (220, 190, 255)
RECESS = (15, 12, 22)

# Little rune marks for the bands of the circles (3x3, '#' = lit).
GLYPHS = [
    ["#.#", ".#.", "#.#"], ["###", "#..", "###"], [".#.", "###", ".#."], ["#..", "###", "..#"],
    ["##.", ".#.", ".##"], ["#.#", "###", "#.#"], [".##", "#..", ".##"], ["###", ".#.", ".#."],
    ["#..", "#..", "###"], [".#.", "#.#", ".#."], ["##.", "#.#", "##."], ["#.#", "#.#", ".#."],
]


class Sigil:
    """A grid of line intensities (0..1); lines get a halo when rendered."""

    def __init__(self, n=N):
        self.n = n
        self.v = [[0.0] * n for _ in range(n)]

    def put(self, x, y, a=1.0):
        x, y = int(round(x)), int(round(y))
        if 0 <= x < self.n and 0 <= y < self.n:
            self.v[y][x] = max(self.v[y][x], a)

    def ring(self, r, a=1.0, width=0.55, gaps=None):
        c = (self.n - 1) / 2
        for y in range(self.n):
            for x in range(self.n):
                d = math.hypot(x - c, y - c)
                if abs(d - r) < width:
                    ang = math.atan2(y - c, x - c) % (2 * math.pi)
                    if gaps and gaps(ang):
                        continue
                    self.put(x, y, a)

    def line(self, x0, y0, x1, y1, a=1.0):
        steps = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
        for i in range(steps + 1):
            t = i / steps
            self.put(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, a)

    def polar(self, r, ang):
        c = (self.n - 1) / 2
        return c + math.cos(ang) * r, c + math.sin(ang) * r

    def glyph(self, cx, cy, g, a=1.0):
        for j, row in enumerate(g):
            for i, ch in enumerate(row):
                if ch == "#":
                    self.put(cx - 1 + i, cy - 1 + j, a)

    def disc(self, r, a):
        c = (self.n - 1) / 2
        for y in range(self.n):
            for x in range(self.n):
                if math.hypot(x - c, y - c) <= r:
                    self.put(x, y, a)

    def image(self, halo=0.32):
        img = Image.new("RGBA", (self.n, self.n), (0, 0, 0, 0))
        p = img.load()
        for y in range(self.n):
            for x in range(self.n):
                a = self.v[y][x]
                if a <= 0:
                    # A soft pixel halo next to lit pixels.
                    near = 0.0
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        xx, yy = x + dx, y + dy
                        if 0 <= xx < self.n and 0 <= yy < self.n:
                            near = max(near, self.v[yy][xx])
                    if near >= 0.6:
                        p[x, y] = (255, 255, 255, int(255 * halo * near))
                    continue
                p[x, y] = (255, 255, 255, int(255 * a))
        return img


def polygon(s, r, points, start, a=1.0):
    pts = [s.polar(r, start + 2 * math.pi * k / points) for k in range(points)]
    for k in range(points):
        s.line(*pts[k], *pts[(k + 1) % points], a)


def band_glyphs(s, r, count, a=1.0, start=0.0):
    for k in range(count):
        ang = start + 2 * math.pi * k / count
        x, y = s.polar(r, ang)
        s.glyph(round(x), round(y), GLYPHS[k % len(GLYPHS)], a)


def circle_sigil():
    """The main circle: a runed band, a hexagram, an inner ring and a bright heart."""
    s = Sigil()
    s.ring(30.5)
    s.ring(25.6)
    band_glyphs(s, 28.0, 16, 0.95)
    s.ring(21.0, 0.9)
    polygon(s, 21.0, 3, -math.pi / 2, 0.85)
    polygon(s, 21.0, 3, math.pi / 2, 0.85)
    s.ring(10.5, 0.9)
    for k in range(6):
        x, y = s.polar(21.0, -math.pi / 2 + k * math.pi / 3)
        s.glyph(round(x), round(y), [".#.", "###", ".#."], 1.0)
    polygon(s, 6.0, 4, math.pi / 4, 0.8)
    s.put(C, C, 1.0)
    s.put(C + 1, C, 1.0)
    s.put(C, C + 1, 1.0)
    s.put(C + 1, C + 1, 1.0)
    return s


def ring_sigil():
    """A rune ring: two thin lines with marks between them, and nothing inside."""
    s = Sigil()
    s.ring(30.6)
    s.ring(26.4, 0.8)
    band_glyphs(s, 28.5, 12, 0.9, start=math.pi / 12)
    for k in range(12):
        x, y = s.polar(28.5, 2 * math.pi * k / 12)
        s.put(x, y, 1.0)
    return s


def star_sigil():
    """An eight-pointed star in a ring: the heart of a circle."""
    s = Sigil()
    polygon(s, 29.0, 4, 0, 1.0)
    polygon(s, 29.0, 4, math.pi / 4, 1.0)
    s.ring(14.0, 0.9)
    s.ring(8.0, 0.7)
    for k in range(8):
        x, y = s.polar(29.0, k * math.pi / 4)
        s.glyph(round(x), round(y), [".#.", "###", ".#."], 1.0)
    return s


def target_sigil():
    """Where a spell will land: a dashed ring, a faint fill, and four arrows pointing in.
    Drawn at 128 pixels, since reticles are shown at a spell's full radius (up to 24 blocks)."""
    s = Sigil(128)
    c = 63.5
    s.disc(59.0, 0.12)
    s.ring(61.2, 1.0, width=0.75, gaps=lambda a: (a * 180 / math.pi) % 11.25 > 7.5)
    s.ring(48.0, 0.45)
    for k in range(4):
        ang = k * math.pi / 2
        tip = s.polar(38.0, ang)
        for side in (-1, 1):
            base = s.polar(49.5, ang + side * 0.14)
            s.line(*tip, *base, 1.0)
    s.line(c - 4, c, c + 5, c, 0.7)
    s.line(c, c - 4, c, c + 5, 0.7)
    return s


def cracked_sigil():
    """A broken circle: the ring snapped in places, cracks running out from the breaks."""
    s = Sigil()
    breaks = [0.4, 1.7, 2.9, 4.1, 5.3]
    s.ring(27.5, 1.0, gaps=lambda a: any(abs(((a - b + math.pi) % (2 * math.pi)) - math.pi) < 0.12 for b in breaks))
    s.ring(22.0, 0.7, gaps=lambda a: any(abs(((a - b - 0.3 + math.pi) % (2 * math.pi)) - math.pi) < 0.2 for b in breaks))
    for k, b in enumerate(breaks):
        r0 = 12.0
        x, y = s.polar(r0, b + 0.1)
        for step in range(5):
            r1 = r0 + 4.2
            jag = (0.18 if (step + k) % 2 == 0 else -0.14)
            nx, ny = s.polar(r1, b + jag)
            s.line(x, y, nx, ny, 1.0)
            x, y, r0 = nx, ny, r1
    return s


# ---------------------------------------------------------------- GUI sprites


def glow_sigil():
    """A flash of light: a bright core in stepped rings, with a four-point sparkle."""
    s = Sigil(32)
    c = (s.n - 1) / 2
    for y in range(s.n):
        for x in range(s.n):
            dx, dy = x - c, y - c
            r = math.hypot(dx, dy)
            body = max(0.0, 1 - r / 14.0) ** 1.25
            ray = max(0.0, 1 - r / 15.8) ** 0.8 * max(0.0, 1 - min(abs(dx), abs(dy)) / 1.1)
            a = max(body, ray)
            # Stepped, like pixel art, rather than a smooth gradient.
            a = math.floor(a * 6 + 0.35) / 6
            if a > 0:
                s.v[y][x] = min(1.0, a)
    return s


def band_sigil():
    """A thin line across the middle of a square: laid end to end around a circle, a ring of even width."""
    s = Sigil(16)
    for x in range(s.n):
        for y, a in ((5, 0.3), (6, 1.0), (7, 1.0), (8, 1.0), (9, 1.0), (10, 0.3)):
            s.v[y][x] = a
    return s


def beam_sigil():
    """A soft glowing band across the middle of a square: a beam's halo, a slash's glow."""
    s = Sigil(16)
    falloff = {7: 1.0, 8: 1.0, 6: 0.78, 9: 0.78, 5: 0.5, 10: 0.5, 4: 0.28, 11: 0.28, 3: 0.12, 12: 0.12, 2: 0.04, 13: 0.04}
    for x in range(s.n):
        for y, a in falloff.items():
            s.v[y][x] = a
    return s


def rgba(c, a=255):
    return tuple(c) + (a,)


def beat_ring():
    """The HUD's beat: a gold ring with four notches, drawn tinted and scaled as the beat comes."""
    n = 32
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    p = img.load()
    c = (n - 1) / 2
    for y in range(n):
        for x in range(n):
            d = math.hypot(x - c, y - c)
            if 13.2 <= d <= 15.2:
                p[x, y] = rgba(GOLD_LIGHT if d < 14.2 else GOLD)
            elif 12.4 <= d < 13.2 or 15.2 < d <= 15.8:
                p[x, y] = rgba(GOLD_DARK, 150)
    for k in range(4):
        ang = k * math.pi / 2
        for r in (11, 12):
            x, y = c + math.cos(ang) * r, c + math.sin(ang) * r
            p[int(round(x)), int(round(y))] = rgba((255, 244, 208))
    return img


def wheel_ring():
    """The spell wheel's backdrop: a dim stone disc with a gold rim and a faint circle inside."""
    n = 180
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    p = img.load()
    c = (n - 1) / 2
    inner = Sigil(n)
    inner.ring(40, 1.0)
    inner.ring(36, 0.6)
    band_glyphs(inner, 38, 16, 0.8)
    polygon(inner, 36, 3, -math.pi / 2, 0.5)
    polygon(inner, 36, 3, math.pi / 2, 0.5)
    for y in range(n):
        for x in range(n):
            d = math.hypot(x - c, y - c)
            if d > 89.5:
                continue
            if d > 87.5:
                p[x, y] = rgba(OUTLINE, 230)
            elif d > 85.5:
                p[x, y] = rgba(GOLD_LIGHT if (x + y) % 7 else GOLD)
            elif d > 84.5:
                p[x, y] = rgba(GOLD_DARK)
            elif d > 83.5:
                p[x, y] = rgba(OUTLINE, 220)
            else:
                shade = 205 + int(30 * (d / 84))
                p[x, y] = rgba(STONE_DARK if d < 30 else STONE, shade)
                if inner.v[y][x] > 0:
                    p[x, y] = rgba(GEM if inner.v[y][x] > 0.7 else GEM_DARK, 200)
    # Four gold studs on the rim.
    for k in range(8):
        ang = k * math.pi / 4
        x, y = int(round(c + math.cos(ang) * 86.5)), int(round(c + math.sin(ang) * 86.5))
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                p[x + dx, y + dy] = rgba(GEM_LIGHT if dx == 0 and dy == 0 else GEM)
    return img


def wheel_node(hover):
    """One spell on the wheel: a round carved badge, gold-rimmed; brighter with a gem glow when pointed at."""
    n = 32
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    p = img.load()
    c = (n - 1) / 2
    for y in range(n):
        for x in range(n):
            d = math.hypot(x - c, y - c)
            if d > 15.5:
                if hover and d < 16.5:
                    p[x, y] = rgba(GEM, 140)
                continue
            if d > 14.5:
                p[x, y] = rgba(OUTLINE)
            elif d > 12.8:
                light = (x - c) + (y - c) < 0
                if hover:
                    p[x, y] = rgba(GOLD_LIGHT if light else GOLD)
                else:
                    p[x, y] = rgba(GOLD if light else GOLD_DARK)
            elif d > 12.0:
                p[x, y] = rgba(OUTLINE)
            else:
                lit = (x - c) + (y - c) < -6
                base = BEVEL_MID if hover else STONE
                p[x, y] = rgba(BEVEL_LIGHT if lit and hover else base if d < 10 else STONE_DARK)
    return img


def grimoire_toast():
    """The Grimoire toast: carved stone, gold trim, an icon well on the left and a violet gem."""
    w, h = 160, 32
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    p = img.load()
    for y in range(h):
        for x in range(w):
            edge = min(x, y, w - 1 - x, h - 1 - y)
            if edge == 0:
                p[x, y] = rgba(OUTLINE)
            elif edge == 1:
                p[x, y] = rgba(GOLD_LIGHT if x + y < (w + h) / 2 else GOLD)
            elif edge == 2:
                p[x, y] = rgba(GOLD_DARK)
            elif edge == 3:
                p[x, y] = rgba(BEVEL_MID if y < h / 2 else OUTLINE)
            else:
                p[x, y] = rgba(STONE if (x // 16 + y // 16) % 2 == 0 else (29, 25, 41))
    # The icon well.
    for y in range(6, 26):
        for x in range(6, 26):
            edge = min(x - 6, y - 6, 25 - x, 25 - y)
            p[x, y] = rgba(OUTLINE if edge == 0 else RECESS if edge > 1 else BEVEL_DARK_OR(x, y))
    for (x, y) in ((w - 8, 5), (w - 9, 6), (w - 8, 6), (w - 7, 6), (w - 8, 7)):
        p[x, y] = rgba(GEM_LIGHT if (x, y) == (w - 8, 6) else GEM)
    return img


def BEVEL_DARK_OR(x, y):
    return (14, 12, 21) if (x + y) % 2 else STONE_DARK


# ---------------------------------------------------------------- output


def main(preview=False):
    PARTICLES.mkdir(parents=True, exist_ok=True)
    sigils = {"circle": circle_sigil(), "ring": ring_sigil(), "star": star_sigil(), "target": target_sigil(), "cracked": cracked_sigil(),
              "glow": glow_sigil(), "band": band_sigil(), "beam": beam_sigil()}
    images = {}
    for name, s in sigils.items():
        im = s.image(halo=0 if name in ("glow", "band", "beam") else 0.32)
        im.save(PARTICLES / f"sigil_{name}.png")
        images[name] = im
    (ASSETS / "particles").mkdir(parents=True, exist_ok=True)
    (ASSETS / "particles/sigil.json").write_text(json.dumps(
        {"textures": [f"wildercord:sigil_{n}" for n in sigils]}, indent=2) + "\n", encoding="utf-8", newline="\n")
    sprites = {
        "hud/beat_ring": beat_ring(),
        "wheel/ring": wheel_ring(),
        "wheel/node": wheel_node(False),
        "wheel/node_hover": wheel_node(True),
        "toast/grimoire": grimoire_toast(),
    }
    for name, im in sprites.items():
        path = SPRITES / f"{name}.png"
        path.parent.mkdir(parents=True, exist_ok=True)
        im.save(path)
    if preview:
        out = ROOT / "build/art-preview"
        out.mkdir(parents=True, exist_ok=True)
        sheet = Image.new("RGBA", (8 * 272 + 16, 2 * 272 + 16), (20, 16, 30, 255))
        tints = {"circle": (245, 213, 106), "ring": (140, 220, 255), "star": (255, 248, 232), "target": (255, 138, 48), "cracked": (255, 90, 58),
                 "glow": (255, 200, 120), "band": (140, 220, 255), "beam": (255, 150, 90)}
        for i, (name, im) in enumerate(images.items()):
            big = im.resize((256, 256), Image.NEAREST)
            tint = Image.new("RGBA", big.size, tints[name] + (255,))
            tinted = Image.composite(tint, Image.new("RGBA", big.size, (0, 0, 0, 0)), big.split()[3])
            sheet.alpha_composite(tinted, (16 + i * 272, 16))
            sheet.alpha_composite(big, (16 + i * 272, 288))
        sheet.save(out / "sigils.png")
        gui = Image.new("RGBA", (420, 200), (40, 70, 40, 255))
        gui.alpha_composite(sprites["wheel/ring"], (10, 10))
        gui.alpha_composite(sprites["wheel/node"], (200, 20))
        gui.alpha_composite(sprites["wheel/node_hover"], (240, 20))
        gui.alpha_composite(sprites["hud/beat_ring"], (280, 20))
        gui.alpha_composite(sprites["toast/grimoire"], (200, 80))
        gui.resize((840, 400), Image.NEAREST).save(out / "gui_new.png")
    print("sigils and sprites written")


if __name__ == "__main__":
    main("--preview" in sys.argv)
