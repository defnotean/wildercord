"""The Shield's particle textures: the glass shards its magic circles break into, and the crack line
that races across a circle just before.

All white on transparency (tinted in game), drawn at 4x and scaled down so every edge is smooth.

Writes:
    textures/particle/shield_crack.png
    textures/particle/glass_shard_{0..5}.png
Run from the project root:  python tools/shield_art.py [--preview]
"""
import math
import random
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
PARTICLES = ROOT / "src/main/resources/assets/wildercord/textures/particle"

N = 32          # final size
S = 4           # supersampling
BIG = N * S


def finish(im):
    return im.resize((N, N), Image.LANCZOS)


def crack():
    """A thin jagged line straight across the middle, with a soft halo: one piece of a crack."""
    im = Image.new("RGBA", (BIG, BIG), (255, 255, 255, 0))
    rng = random.Random(7)
    pts = []
    for i in range(9):
        x = BIG * i / 8
        y = BIG / 2 + (rng.uniform(-1, 1) * S * 1.4 if 0 < i < 8 else 0)
        pts.append((x, y))
    halo = Image.new("RGBA", (BIG, BIG), (255, 255, 255, 0))
    ImageDraw.Draw(halo).line(pts, fill=(255, 255, 255, 120), width=int(S * 3))
    halo = halo.filter(ImageFilter.GaussianBlur(S * 1.2))
    im.alpha_composite(halo)
    ImageDraw.Draw(im).line(pts, fill=(255, 255, 255, 255), width=int(S * 0.9))
    return finish(im)


def shard(seed):
    """An irregular piece of glass: a faint body, bright edges, and a streak of light along one of them."""
    rng = random.Random(1000 + seed)
    c = BIG / 2
    count = rng.choice((3, 4, 4, 5))
    # Long thin shards, broad flat ones and splinters: stretch the outline one way.
    stretch = rng.uniform(1.0, 1.9)
    turn = rng.uniform(0, math.pi)
    # Keep the corners spread round the centre, so the piece fills its square.
    angles = [(2 * math.pi * i / count) + rng.uniform(-0.45, 0.45) * (2 * math.pi / count) for i in range(count)]
    pts = []
    for a in angles:
        r = rng.uniform(0.62, 0.97) * BIG * 0.47
        x, y = math.cos(a) * r / stretch, math.sin(a) * r
        pts.append((c + x * math.cos(turn) - y * math.sin(turn), c + x * math.sin(turn) + y * math.cos(turn)))
    im = Image.new("RGBA", (BIG, BIG), (255, 255, 255, 0))
    d = ImageDraw.Draw(im)
    d.polygon(pts, fill=(255, 255, 255, 58))
    # Fresh glass edges catch the light: a halo, then a crisp line.
    halo = Image.new("RGBA", (BIG, BIG), (255, 255, 255, 0))
    ImageDraw.Draw(halo).line(pts + [pts[0]], fill=(255, 255, 255, 110), width=int(S * 2.4))
    halo = halo.filter(ImageFilter.GaussianBlur(S * 1.0))
    im.alpha_composite(halo)
    ImageDraw.Draw(im).line(pts + [pts[0]], fill=(255, 255, 255, 235), width=int(S * 0.9))
    # A streak of reflected light across the face, parallel to its longest edge.
    edges = [(pts[i], pts[(i + 1) % len(pts)]) for i in range(len(pts))]
    (ax, ay), (bx, by) = max(edges, key=lambda e: math.dist(*e))
    mx, my = sum(p[0] for p in pts) / len(pts), sum(p[1] for p in pts) / len(pts)
    ox, oy = (mx - (ax + bx) / 2) * 0.38, (my - (ay + by) / 2) * 0.38
    streak = Image.new("RGBA", (BIG, BIG), (255, 255, 255, 0))
    sd = ImageDraw.Draw(streak)
    sd.line([(ax + (bx - ax) * 0.2 + ox, ay + (by - ay) * 0.2 + oy), (ax + (bx - ax) * 0.75 + ox, ay + (by - ay) * 0.75 + oy)],
            fill=(255, 255, 255, 170), width=int(S * 1.3))
    streak = streak.filter(ImageFilter.GaussianBlur(S * 0.6))
    mask = Image.new("L", (BIG, BIG), 0)
    ImageDraw.Draw(mask).polygon(pts, fill=255)
    clipped = Image.new("RGBA", (BIG, BIG), (255, 255, 255, 0))
    clipped.paste(streak, (0, 0), mask)
    im.alpha_composite(clipped)
    return finish(im)


def main(preview=False):
    PARTICLES.mkdir(parents=True, exist_ok=True)
    images = {"shield_crack": crack()}
    for i in range(6):
        images[f"glass_shard_{i}"] = shard(i)
    for name, im in images.items():
        im.save(PARTICLES / f"{name}.png")
    if preview:
        out = ROOT / "build/art-preview"
        out.mkdir(parents=True, exist_ok=True)
        sheet = Image.new("RGBA", (len(images) * 136 + 8, 144), (24, 20, 34, 255))
        tint = (242, 220, 168)
        for i, im in enumerate(images.values()):
            big = im.resize((128, 128), Image.NEAREST)
            col = Image.new("RGBA", big.size, tint + (255,))
            sheet.alpha_composite(Image.composite(col, Image.new("RGBA", big.size, (0, 0, 0, 0)), big.split()[3]), (8 + i * 136, 8))
        sheet.save(out / "shield.png")
    print(f"{len(images)} shield textures written")


if __name__ == "__main__":
    main(preview="--preview" in sys.argv)
