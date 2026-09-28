"""Soft lights and vapours: the textures behind motes, butterflies of light, steam and smoke, and
the smooth glow under a drawn magic circle.

All white on transparency (tinted in game). Unlike the sigils these are smooth, not pixel art: a
cloud or a glow with hard steps reads as a flat square once it's a few blocks across.

Writes:
    textures/particle/sigil_soft.png          a smooth round glow (under drawn circles, a mote's halo)
    textures/particle/mote_cloud_{0..3}.png   soft billows of steam and smoke
    textures/particle/mote_wing.png           one wing of a butterfly of light (the body at its left edge)
Run from the project root:  python tools/mote_art.py [--preview]
"""
import math
import random
import sys
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
PARTICLES = ROOT / "src/main/resources/assets/wildercord/textures/particle"


def soft_glow(n=64):
    """A round glow with a smooth falloff, brightest in the middle, clear at the edge."""
    im = Image.new("RGBA", (n, n), (255, 255, 255, 0))
    p = im.load()
    c = (n - 1) / 2
    for y in range(n):
        for x in range(n):
            r = math.hypot(x - c, y - c) / (n / 2)
            if r >= 1:
                continue
            a = (1 - r) ** 2.2
            p[x, y] = (255, 255, 255, int(round(255 * a)))
    return im


def cloud(seed, n=64):
    """A billow: a handful of overlapping soft blobs, denser in the middle, ragged at the rim."""
    rng = random.Random(seed)
    blobs = [(0.0, 0.0, 0.5, 0.9)]
    for _ in range(7):
        a = rng.uniform(0, math.tau)
        d = rng.uniform(0.15, 0.38)
        blobs.append((math.cos(a) * d, math.sin(a) * d, rng.uniform(0.2, 0.34), rng.uniform(0.45, 0.85)))
    # Slow ripples through it, so it isn't one flat puff: lighter and thinner lumps.
    waves = [(rng.uniform(2.5, 5.0), rng.uniform(0, math.tau), rng.uniform(0, math.tau)) for _ in range(3)]
    im = Image.new("RGBA", (n, n), (255, 255, 255, 0))
    p = im.load()
    for y in range(n):
        for x in range(n):
            u = (x + 0.5) / n * 2 - 1
            v = (y + 0.5) / n * 2 - 1
            density = 0.0
            for bx, by, br, w in blobs:
                d = math.hypot(u - bx, v - by) / br
                density += w * math.exp(-d * d * 2.0)
            lumps = 1.0
            for f, a, ph in waves:
                lumps *= 0.9 + 0.1 * math.sin(f * (u * math.cos(a) + v * math.sin(a)) + ph)
            # Fade out well before the square's edge, whatever the blobs did.
            r = math.hypot(u, v)
            t = min(1.0, max(0.0, (0.97 - r) / 0.45))
            edge = t * t * (3 - 2 * t)
            alpha = (1 - math.exp(-density * 1.3)) * lumps * edge
            p[x, y] = (255, 255, 255, int(round(255 * min(1.0, alpha ** 1.25 * 0.95))))
    return im


def wing(n=32):
    """One wing, spread to the right from the body at the left edge: a rounded forewing over a
    smaller hindwing, a bright rim, faint veins and a lit patch near the tip."""
    s = 8
    big = n * s
    im = Image.new("RGBA", (big, big), (255, 255, 255, 0))
    fill = Image.new("L", (big, big), 0)
    rim = Image.new("L", (big, big), 0)
    df = ImageDraw.Draw(fill)
    dr = ImageDraw.Draw(rim)

    def shape(cx, cy, rx, ry, tilt, steps=48):
        pts = []
        for i in range(steps):
            a = math.tau * i / steps
            x = math.cos(a) * rx
            y = math.sin(a) * ry
            # A little fuller toward the tip.
            x *= 1 + 0.12 * math.cos(a)
            ct, st = math.cos(tilt), math.sin(tilt)
            pts.append((cx + x * ct - y * st, cy + x * st + y * ct))
        return pts

    body = (0.04 * big, 0.5 * big)
    fore = shape(0.5 * big, 0.3 * big, 0.44 * big, 0.25 * big, -0.5)
    hind = shape(0.36 * big, 0.7 * big, 0.3 * big, 0.2 * big, 0.55)
    for pts in (fore, hind):
        df.polygon(pts, fill=130)
        dr.line(pts + [pts[0]], fill=255, width=int(s * 1.3))
    # Veins from the body out to the edge: thinner light through the wing.
    for tip in ((0.9 * big, 0.08 * big), (0.95 * big, 0.3 * big), (0.72 * big, 0.5 * big), (0.6 * big, 0.88 * big), (0.3 * big, 0.9 * big)):
        df.line([body, tip], fill=60, width=int(s * 0.9))
    # A lit eye-spot near the forewing's tip, and a softer one on the hindwing.
    for ex, ey, rx, ry, v in ((0.72, 0.2, 0.08, 0.07, 240), (0.4, 0.74, 0.06, 0.05, 200)):
        dr.ellipse([(ex - rx) * big, (ey - ry) * big, (ex + rx) * big, (ey + ry) * big], fill=v)
    # The veins and eye-spot only show inside the wings.
    area = Image.new("L", (big, big), 0)
    ad = ImageDraw.Draw(area)
    ad.polygon(fore, fill=255)
    ad.polygon(hind, fill=255)
    area = area.filter(ImageFilter.MaxFilter(int(s * 1.3) | 1))
    rim = ImageChops.multiply(rim, area)
    lines = ImageChops.lighter(fill, rim)
    # A soft glow round it all, under the lines.
    glow = lines.filter(ImageFilter.GaussianBlur(s * 1.2)).point(lambda v: int(v * 0.55))
    im.putalpha(ImageChops.lighter(lines, glow))
    return im.resize((n, n), Image.LANCZOS)


def main(preview=False):
    PARTICLES.mkdir(parents=True, exist_ok=True)
    images = {"sigil_soft": soft_glow(), "mote_wing": wing()}
    for i in range(4):
        images[f"mote_cloud_{i}"] = cloud(101 + i * 17)
    for name, im in images.items():
        im.save(PARTICLES / f"{name}.png")
    if preview:
        out = ROOT / "build/art-preview"
        out.mkdir(parents=True, exist_ok=True)
        sheet = Image.new("RGBA", (len(images) * 272 + 16, 288), (40, 60, 90, 255))
        for i, im in enumerate(images.values()):
            sheet.alpha_composite(im.resize((256, 256), Image.NEAREST), (16 + i * 272, 16))
        sheet.save(out / "motes.png")
    print("motes written")


if __name__ == "__main__":
    main("--preview" in sys.argv)
