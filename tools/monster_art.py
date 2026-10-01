"""The magical monsters of the wilds: their skins and glow layers, the drops' icons, the spawn eggs, the loot tables, the
brewing recipes and the English text. Called by generate_assets.py (write(g), and LANG merged into en_us.json).

Skins are painted face by face on each model's box-UV layout (the layouts are in the models' javadoc, client/monster/), from
materials rather than ASCII: bark with its grain and grooves, moss, clumped leaves, directional fur, scalloped feathers,
chitin plates, faceted crystal, mottled frog skin, clear jelly. Each face is lit like vanilla mobs: tops a step lighter,
undersides darker. Glow layers hold only what glows, bright on transparency, drawn emissive in game. Every choice is
seeded, so a run always writes the same files.

Run this file on its own to render a review sheet of every skin, icon and egg into build/art-preview/monster_art.png.
"""
from __future__ import annotations

import math
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from item_art import Canvas, blit, hexc, mix, ramp  # noqa: E402
from world_art import Sheet, box, noise  # noqa: E402

# ============================================================== shared painting

LIGHT = {"top": 1, "bottom": -2, "front": 0, "back": -1, "right": 0, "left": -1}


def shade(tones, t):
    return tones[max(0, min(len(tones) - 1, int(t)))]


def cells(area):
    x0, y0, w, h = area
    for y in range(h):
        for x in range(w):
            yield x, y, x0 + x, y0 + y


def faces(b, *names):
    return [(n, b[n]) for n in (names or b.keys()) if b[n][2] > 0 and b[n][3] > 0]


def rgba(c, a=255):
    return (c[0], c[1], c[2], a)


def smooth(x, y, seed, cell=3.0):
    """Value noise in [0, 1) that changes over about `cell` pixels: soft clusters rather than salt and pepper."""
    fx, fy = x / cell, y / cell
    ix, iy = math.floor(fx), math.floor(fy)
    tx, ty = fx - ix, fy - iy
    tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
    a, b = noise(ix, iy, seed), noise(ix + 1, iy, seed)
    c, d = noise(ix, iy + 1, seed), noise(ix + 1, iy + 1, seed)
    return (a + (b - a) * tx) * (1 - ty) + (c + (d - c) * tx) * ty


def bark(cv, area, tones, seed, face, base=2):
    """Bark: long vertical ridges split by dark grooves (a ridge lit on its left edge), a knot now and then; a top or bottom
    shows rings."""
    x0, y0, w, h = area
    light = LIGHT[face]
    if face in ("top", "bottom"):
        cx, cy = (w - 1) / 2, (h - 1) / 2
        for x, y, px, py in cells(area):
            r = math.hypot(x - cx, y - cy) + smooth(px, py, seed, 2) * 0.9
            t = base + light + (1 if int(r) % 2 == 0 else 0)
            cv.put(px, py, shade(tones, t))
        return
    for x, y, px, py in cells(area):
        # Grooves run down the face, wandering a pixel now and then.
        wander = 1 if smooth(px * 3.0, y, seed + 7, 5) > 0.62 else 0
        col = px + wander
        groove = noise(col, 0, seed) < 0.3
        lit = noise(col - 1, 0, seed) < 0.3 and not groove
        t = base + light
        if groove:
            t -= 2 if noise(col, y // 5, seed + 1) > 0.15 else 0
        elif lit:
            t += 1
        if smooth(px, py, seed + 3, 3) > 0.78:
            t += 1
        if y == 0:
            t += 1
        cv.put(px, py, shade(tones, t))
    # A knot or two: a dark eye ringed lighter.
    for k in range(max(0, (w * h) // 90)):
        kx, ky = int(noise(k, 1, seed + 11) * w), int(noise(k, 2, seed + 11) * h)
        for dx, dy, dt in ((0, 0, -2), (1, 0, -1), (-1, 0, 1), (0, -1, 1), (0, 1, -1)):
            if 0 <= kx + dx < w and 0 <= ky + dy < h:
                cv.put(x0 + kx + dx, y0 + ky + dy, shade(tones, base + light + dt))


def moss(cv, area, tones, seed, face, cover=0.55, base=2):
    """Moss in soft cushions, thickest toward the top of a face (it grows where the rain sits), each cushion darker at its
    rim and lit at its crown."""
    x0, y0, w, h = area
    for x, y, px, py in cells(area):
        n = smooth(px, py, seed, 3.2)
        top = 1 - y / max(1, h - 1) if face not in ("top", "bottom") else 1.0
        reach = cover * (0.45 + 0.75 * top) + (0.15 if face == "top" else 0) - (0.4 if face == "bottom" else 0)
        if n < reach:
            depth = (reach - n) / max(0.01, reach)
            t = base + LIGHT[face] + (1 if depth > 0.55 else 0) + (1 if depth > 0.8 and (px + py) % 2 == 0 else 0) - (1 if depth < 0.15 else 0)
            cv.put(px, py, shade(tones, t))


def leaves(cv, area, tones, seed, face, holes=0.0):
    """Clumped leaves: rounded clusters lit from the top left with dark gaps between, and holes at the ragged edge."""
    x0, y0, w, h = area
    for x, y, px, py in cells(area):
        edge = min(x, w - 1 - x, y, h - 1 - y)
        if holes and edge == 0 and smooth(px, py, seed + 9, 1.6) < holes:
            continue
        n = smooth(px, py, seed, 2.2)
        lit = smooth(px - 1, py - 1, seed, 2.2)
        t = 2 + LIGHT[face]
        if n < 0.3:
            t -= 2
        elif lit > n + 0.08:
            t -= 1
        elif n > 0.62:
            t += 1
            if n > 0.8 and (px + py) % 2 == 0:
                t += 1
        cv.put(px, py, shade(tones, t))


def fur(cv, area, tones, seed, face, base=2, along_y=False):
    """Short fur, stroked one way: soft streaks a few pixels long, a sheen where it catches the light."""
    for x, y, px, py in cells(area):
        a, b = (px * 2.5, py * 0.6) if along_y else (px * 0.6, py * 2.5)
        streak = smooth(a, b, seed, 2.0)
        t = base + LIGHT[face] + (1 if streak > 0.7 else 0) - (1 if streak < 0.22 else 0)
        cv.put(px, py, shade(tones, t))


def feathers(cv, area, tones, seed, face, base=2, row=3, flip=False):
    """Scalloped rows of feathers: each feather's rounded tip a step lighter, its shadow under the next row."""
    x0, y0, w, h = area
    for x, y, px, py in cells(area):
        yy = (h - 1 - y) if flip else y
        r = yy // row
        offset = (r % 2) * 2
        k = (x + offset) % 4
        tip = yy % row == row - 1
        t = base + LIGHT[face]
        if tip and k in (1, 2):
            t += 1
        elif yy % row == 0:
            t -= 1
        if k == 0:
            t -= 1
        if noise(px, py, seed) > 0.9:
            t += 1
        cv.put(px, py, shade(tones, t))


def chitin(cv, area, tones, seed, face, base=2, seam=4, vertical=False):
    """Chitin: plates split by dark seams, each with a sheen along its top edge."""
    for x, y, px, py in cells(area):
        along = x if vertical else y
        t = base + LIGHT[face]
        if along % seam == 0:
            t -= 2
        elif along % seam == 1:
            t += 1
        if noise(px, py, seed) > 0.93:
            t += 1
        cv.put(px, py, shade(tones, t))


def crystal(cv, area, tones, seed, face):
    """Amethyst: a face split on the diagonal into a lit facet and a shaded one, bright at the edge where they meet."""
    x0, y0, w, h = area
    for x, y, px, py in cells(area):
        d = x - y * (w / max(1, h))
        t = (3 if d < 0 else 1) + (1 if face == "top" else 0) - (1 if face in ("back", "left") else 0)
        if abs(d) < 0.8:
            t = 4
        if x == 0 or y == 0:
            t += 1
        if noise(px, py, seed) > 0.9:
            t += 1
        cv.put(px, py, shade(tones, t))


def mottle(cv, area, tones, blotch, seed, face, base=2, blotchiness=0.25):
    """Mottled skin: big soft blotches of a second colour over the first, darker at their hearts."""
    for x, y, px, py in cells(area):
        n = smooth(px, py, seed, 3.5)
        t = base + LIGHT[face] + (1 if smooth(px, py, seed + 2, 1.7) > 0.72 else 0)
        c = shade(tones, t)
        if n < blotchiness:
            c = shade(blotch, t - (2 if n < blotchiness * 0.5 else 1))
        cv.put(px, py, c)


def warts(cv, area, rim, top, seed, count):
    """Warts: a lighter bump with a dark rim under it."""
    x0, y0, w, h = area
    rng = random.Random(seed)
    for _ in range(count):
        x, y = rng.randrange(w), rng.randrange(max(1, h - 1))
        cv.put(x0 + x, y0 + y, top)
        if y + 1 < h:
            cv.put(x0 + x, y0 + y + 1, rim)


def fill(cv, area, colour):
    for _, _, px, py in cells(area):
        cv.put(px, py, colour)


def line(cv, area, points, colour):
    x0, y0, _, _ = area
    for x, y in points:
        cv.put(x0 + x, y0 + y, colour)


def dots(cv, area, pixels, colour):
    line(cv, area, pixels, colour)


def vine_strand(cv, area, seed, stem, leaf, glow=None, glow_cv=None, slope=0.5):
    """A vine winding across a face: a stem with a shadow beside it, a pair of leaves now and then (and, on a glow sheet,
    the sap running in it)."""
    x0, y0, w, h = area
    rng = random.Random(seed)
    x = rng.uniform(0, w - 1)
    shadow = mix(stem, (0, 0, 0), 0.5)
    for y in range(h):
        x += math.sin(y * 0.7 + seed) * slope
        xi = int(round(x)) % w
        cv.put(x0 + xi, y0 + y, stem)
        if w > 2:
            cv.put(x0 + (xi + 1) % w, y0 + y, shadow)
        if glow_cv is not None and glow is not None and y % 3 != 1:
            glow_cv.put(x0 + xi, y0 + y, glow)
        if y % 3 == 1 and rng.random() < 0.7:
            side = 1 if rng.random() < 0.5 else -1
            for k in (1, 2):
                lx = xi + side * k
                if 0 <= lx < w:
                    cv.put(x0 + lx, y0 + y - (k - 1), leaf if k == 1 else mix(leaf, (255, 255, 200), 0.25))


def strands(cv, area, colours, seed, density=0.55, alpha=255):
    """Hanging strands on a plane (vines, moss): columns that run down and stop, on transparency."""
    x0, y0, w, h = area
    rng = random.Random(seed)
    for x in range(w):
        if rng.random() > density + (0.3 if x in (w // 2, w // 2 - 1) else 0):
            continue
        length = rng.randint(max(2, h // 2), h)
        for y in range(length):
            c = colours[(y + x) % len(colours)] if y < length - 1 else colours[-1]
            cv.put(x0 + x, y0 + y, rgba(c, alpha) if alpha < 255 else c)


# ============================================================== the Bramblewalker (BramblewalkerModel, 128x64)

BARK = ramp("#1A110A", "#2A1C11", "#3E2B1A", "#553C24", "#6E4F30", "#8A653E", "#A57D50")
MOSS = ramp("#22300F", "#334818", "#486222", "#5E7C2E", "#7C9A3E", "#A0B858")
LEAF = ramp("#16300F", "#224418", "#305C20", "#41762A", "#589234", "#78AE46")
VINE_STEM = hexc("#3C6224")
VINE_LEAF = hexc("#6A9A38")
THORN = hexc("#D6C69A")
SAP = (156, 255, 122, 255)
SAP_HALO = (110, 230, 90, 110)

B_TORSO = box(0, 0, 14, 15, 9)
B_HEAD = box(48, 0, 9, 8, 8)
B_ARM = box(84, 0, 5, 17, 5)
B_HAND = box(104, 0, 6, 4, 6)
B_LEG = box(0, 26, 6, 10, 6)
B_ROOT = box(24, 26, 8, 3, 8)
B_LEAVES = box(56, 22, 8, 5, 7)
B_BRANCH = box(88, 22, 2, 12, 2)
B_TWIG = box(96, 22, 1, 5, 1)
B_VINE = box(100, 22, 4, 10, 0)
B_TUFT = box(0, 44, 6, 4, 6)

# Its face: a dark hollow in the brambles, two green lights deep in it, thorns round the edge ('e' an eye, 'h' hollow).
B_FACE = """
    t...t.t..
    .hhhhhhh.
    hhhhhhhhh
    hhehhhehh
    hhhhhhhhh
    .hhh.hhh.
    t.hh.hh.t
    ..h...h..
"""


def bramblewalker_texture():
    cv = Sheet(128, 64)
    for part, seed in ((B_TORSO, 1), (B_HEAD, 2), (B_ARM, 3), (B_HAND, 4), (B_LEG, 5), (B_BRANCH, 6), (B_TWIG, 7)):
        for name, area in faces(part):
            bark(cv, area, BARK, seed * 10 + len(name), name)
    for name, area in faces(B_ROOT):
        bark(cv, area, BARK, 80 + len(name), name, base=1)
        if name == "top":
            # Roots spreading from the trunk, with earth between them.
            x0, y0, w, h = area
            for x, y, px, py in cells(area):
                dx, dy = x - (w - 1) / 2, y - (h - 1) / 2
                ang = math.atan2(dy, dx)
                if abs(math.sin(ang * 3 + 0.4)) > 0.75 and math.hypot(dx, dy) > 1.5:
                    cv.put(px, py, hexc("#2E2216"))
    # Moss over its shoulders, back and the tops of everything; leaves in clumps.
    for part, seed, cover in ((B_TORSO, 11, 0.42), (B_HEAD, 12, 0.4), (B_ARM, 13, 0.3), (B_LEG, 14, 0.18)):
        for name, area in faces(part):
            if name != "bottom":
                moss(cv, area, MOSS, seed + len(name), name, cover=cover)
    for part, seed in ((B_LEAVES, 21), (B_TUFT, 22)):
        for name, area in faces(part):
            leaves(cv, area, LEAF, seed + len(name), name, holes=0.35)
    # Vines winding round the arms and over the chest.
    for name, area in faces(B_ARM, "front", "right", "left", "back"):
        vine_strand(cv, area, 30 + len(name), VINE_STEM, VINE_LEAF, slope=0.9)
    for i, (name, area) in enumerate(faces(B_TORSO, "front", "right", "left")):
        for k in range(3 if name == "front" else 1):
            vine_strand(cv, area, 40 + i * 5 + k, VINE_STEM, VINE_LEAF, slope=0.6)
    # Thorns: pale points along the branches and twigs, and the claws' tips.
    for part in (B_BRANCH, B_TWIG):
        for name, area in faces(part, "front", "right", "left", "back"):
            x0, y0, w, h = area
            rng = random.Random(x0 * 31 + y0)
            for y in range(1, h, 4):
                if rng.random() < 0.6:
                    cv.put(x0 + rng.randrange(w), y0 + y + rng.randrange(2), THORN)
    for name, area in faces(B_TWIG, "top"):
        fill(cv, area, THORN)
    # The hanging vine curtains: strands of vine and leaf on transparency.
    for name, area in faces(B_VINE, "front", "back"):
        strands(cv, area, [VINE_STEM, hexc("#4E7A2C"), VINE_LEAF, hexc("#2E4A1A")], 50 + len(name), density=0.7)
    # The face.
    fx, fy, fw, fh = B_HEAD["front"]
    for y, row in enumerate(r.strip() for r in B_FACE.strip("\n").splitlines()):
        for x, ch in enumerate(row):
            if ch == "h":
                cv.put(fx + x, fy + y, hexc("#0E0905") if (x + y) % 3 else hexc("#1A110A"))
            elif ch == "e":
                cv.put(fx + x, fy + y, hexc("#C8FF8A"))
            elif ch == "t":
                cv.put(fx + x, fy + y, THORN)
    # A dark hollow in its chest, where the vines knot.
    tx, ty, tw, th = B_TORSO["front"]
    for (x, y) in [(6, 5), (7, 5), (5, 6), (6, 6), (7, 6), (8, 6), (6, 7), (7, 7)]:
        cv.put(tx + x, ty + y, hexc("#120B06"))
    return cv.image()


def bramblewalker_glow_texture():
    """The sap in its vines: thin green veins that blaze as it rears for the lash."""
    cv = Sheet(128, 64)
    scratch = Sheet(128, 64)
    for name, area in faces(B_ARM, "front", "right", "left", "back"):
        vine_strand(scratch, area, 30 + len(name), VINE_STEM, VINE_LEAF, SAP, cv, slope=0.9)
    for i, (name, area) in enumerate(faces(B_TORSO, "front", "right", "left")):
        for k in range(3 if name == "front" else 1):
            vine_strand(scratch, area, 40 + i * 5 + k, VINE_STEM, VINE_LEAF, SAP, cv, slope=0.6)
    tx, ty, _, _ = B_TORSO["front"]
    for (x, y) in [(6, 5), (7, 5), (5, 6), (6, 6), (7, 6), (8, 6), (6, 7), (7, 7)]:
        cv.put(tx + x, ty + y, SAP_HALO)
    return cv.image()


def bramblewalker_eyes_texture():
    cv = Sheet(128, 64)
    fx, fy, _, _ = B_HEAD["front"]
    for y, row in enumerate(r.strip() for r in B_FACE.strip("\n").splitlines()):
        for x, ch in enumerate(row):
            if ch == "e":
                cv.put(fx + x, fy + y, (230, 255, 190, 255))
                for dx, dy in ((1, 0), (-1, 0), (0, -1), (0, 1)):
                    if cv.get(fx + x + dx, fy + y + dy) is None:
                        cv.put(fx + x + dx, fy + y + dy, (150, 255, 110, 90))
    return cv.image()


# ============================================================== the Gloomstalker (GloomstalkerModel, 128x64)

GLOOM = ramp("#060309", "#0C0714", "#140C22", "#1E1232", "#2A1A46", "#3A2660", "#523A84")
SHEEN = hexc("#6A4CA8")
G_CHEST = box(0, 0, 7, 6, 9)
G_HIPS = box(0, 16, 6, 5, 10)
G_HEAD = box(48, 0, 5, 4, 5)
G_SNOUT = box(72, 0, 3, 2, 3)
G_EAR = box(86, 0, 2, 2, 1)
G_JAW = box(92, 0, 3, 1, 3)
G_LEG = box(48, 12, 3, 7, 3)
G_PAW = box(60, 12, 3, 2, 4)
G_TAIL = box(76, 12, 2, 2, 12)
G_TIP = box(104, 12, 2, 2, 8)
G_CREST = box(0, 32, 0, 3, 16)
G_WISP = box(34, 32, 0, 4, 5)
# Its eyes on the head's front: slanted, wide apart.
G_EYES = [(1, 1), (0, 1), (3, 1), (4, 1)]


def gloomstalker_texture():
    cv = Sheet(128, 64)
    for part, seed in ((G_CHEST, 1), (G_HIPS, 2), (G_HEAD, 3), (G_SNOUT, 4), (G_EAR, 5), (G_JAW, 6), (G_LEG, 7), (G_PAW, 8), (G_TAIL, 9), (G_TIP, 10)):
        for name, area in faces(part):
            fur(cv, area, GLOOM, seed * 10 + len(name), name, base=2, along_y=name in ("front", "back", "right", "left") and part in (G_LEG,))
    # Ghost rosettes: rings of a shade lighter on the flanks, as a panther's spots show only in the right light.
    for part in (G_CHEST, G_HIPS):
        for name, area in faces(part, "right", "left", "top"):
            x0, y0, w, h = area
            rng = random.Random(x0 * 7 + y0)
            for _ in range(max(1, w * h // 18)):
                cx, cy = rng.randrange(1, max(2, w - 1)), rng.randrange(1, max(2, h - 1))
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    if 0 <= cx + dx < w and 0 <= cy + dy < h:
                        cv.put(x0 + cx + dx, y0 + cy + dy, GLOOM[4])
    # A sheen down the spine.
    for part in (G_CHEST, G_HIPS):
        x0, y0, w, h = part["top"]
        for y in range(h):
            cv.put(x0 + w // 2, y0 + y, SHEEN)
            cv.put(x0 + w // 2 - 1, y0 + y, GLOOM[5])
    # Ears lined violet; claws and fangs pale.
    ex, ey, ew, eh = G_EAR["front"]
    for y in range(1, eh):
        cv.put(ex + 0, ey + y, hexc("#5A3A8A"))
    for name in ("front",):
        px0, py0, pw, ph = G_PAW[name]
        for x in range(0, pw, 1):
            if x % 2 == 0:
                cv.put(px0 + x, py0 + ph - 1, hexc("#B8A8D0"))
    jx, jy, jw, jh = G_JAW["front"]
    cv.put(jx, jy, hexc("#E8E0F0"))
    cv.put(jx + jw - 1, jy, hexc("#E8E0F0"))
    # The face: dark around the eyes, a nose pad, the eyes themselves dim (their light is the eyes layer).
    hx, hy, hw, hh = G_HEAD["front"]
    for x in range(hw):
        cv.put(hx + x, hy + 1, GLOOM[0])
    for (x, y) in G_EYES:
        cv.put(hx + x, hy + y, hexc("#7A4AB8"))
    sx, sy, sw, sh = G_SNOUT["front"]
    cv.put(sx + 1, sy, hexc("#3A2450"))
    cv.put(sx + 2, sy, hexc("#3A2450"))
    for x in range(sw):
        cv.put(sx + x, sy + sh - 1, GLOOM[0])
    # The tail's tip and its crest and wisps: smoke, thinning to nothing.
    for name, area in faces(G_TIP):
        x0, y0, w, h = area
        for x, y, px, py in cells(area):
            along = x / max(1, w - 1) if name in ("top", "bottom") else 0
            if name in ("right", "left"):
                along = x / max(1, w - 1) if name == "right" else 1 - x / max(1, w - 1)
            a = int(255 * (1 - 0.55 * along))
            cv.put(px, py, rgba(shade(GLOOM, 2 + (1 if noise(px, py, 3) > 0.7 else 0)), a))
    for part, seed in ((G_CREST, 41), (G_WISP, 42)):
        for name, area in faces(part, "front", "back"):
            x0, y0, w, h = area
            for x, y, px, py in cells(area):
                rise = (h - 1 - y) / max(1, h - 1)
                n = noise(px, py // 2, seed)
                if n < 0.35 + rise * 0.4:
                    continue
                a = int(200 * (1 - rise * 0.7))
                cv.put(px, py, rgba(shade(GLOOM, 3 + (1 if n > 0.85 else 0)), a))
    return cv.image()


def gloomstalker_eyes_texture():
    cv = Sheet(128, 64)
    hx, hy, _, _ = G_HEAD["front"]
    for (x, y) in G_EYES:
        core = x in (1, 3)
        cv.put(hx + x, hy + y, (255, 255, 255, 255) if core else (232, 190, 255, 255))
    for (x, y) in ((0, 0), (1, 0), (3, 0), (4, 0), (0, 2), (1, 2), (3, 2), (4, 2)):
        cv.put(hx + x, hy + y, (190, 110, 255, 140))
    # From the side, a glint where each eye wraps round.
    for name, ex in (("right", G_HEAD["right"]), ("left", G_HEAD["left"])):
        x0, y0, w, h = ex
        cv.put(x0 + (w - 1 if name == "right" else 0), y0 + 1, (220, 170, 255, 220))
    return cv.image()


def gloomstalker_flare_texture():
    """A wider burn of violet round the eyes, shown as it crouches to pounce."""
    cv = Sheet(128, 64)
    hx, hy, hw, hh = G_HEAD["front"]
    for x in range(hw):
        for y in range(3):
            near = min(abs(x - 1), abs(x - 3))
            if near <= 1:
                cv.put(hx + x, hy + y, (255, 230, 255, 255) if (y == 1 and near == 0) else (210, 140, 255, 200 if near == 0 else 130))
    return cv.image()


def gloomstalker_glow_texture():
    """Faint violet seams: along its spine, and the hearts of its rosettes."""
    cv = Sheet(128, 64)
    for part in (G_CHEST, G_HIPS):
        x0, y0, w, h = part["top"]
        for y in range(h):
            if y % 2 == 0:
                cv.put(x0 + w // 2, y0 + y, (180, 90, 240, 200))
    for part in (G_CHEST, G_HIPS):
        for name, area in faces(part, "right", "left", "top"):
            x0, y0, w, h = area
            rng = random.Random(x0 * 7 + y0)
            for _ in range(max(1, w * h // 18)):
                cx, cy = rng.randrange(1, max(2, w - 1)), rng.randrange(1, max(2, h - 1))
                cv.put(x0 + cx, y0 + cy, (160, 80, 230, 150))
    return cv.image()


# ============================================================== the Thunderwing Harpy (ThunderwingHarpyModel, 128x64)

STORMF = ramp("#141A2A", "#1E2840", "#2C3A5A", "#3E5278", "#56709A", "#7892B8", "#A0B6D2")
PALE = ramp("#7A8496", "#9AA4B6", "#BCC6D4", "#DCE4EE", "#F4F8FC")
HORN = ramp("#2A2010", "#5A4418", "#9A7828", "#C8A040", "#E8C868")
SCALE = ramp("#3A3020", "#5E5034", "#857048", "#AC925E", "#CCB27A")
BOLT = hexc("#FFE650")
BOLT_HOT = hexc("#FFFBE0")
H_BODY = box(0, 0, 6, 9, 4)
H_HEAD = box(20, 0, 6, 6, 6)
H_BEAK = box(44, 0, 2, 3, 3)
H_CREST = box(54, 0, 0, 7, 8)
H_RUFF = box(70, 0, 7, 3, 5)
H_THIGH = box(94, 0, 3, 4, 3)
H_SHIN = box(106, 0, 2, 4, 2)
H_FOOT = box(114, 0, 3, 1, 4)
H_TALON = box(94, 8, 1, 2, 1)
H_BONE = box(0, 16, 10, 2, 3)
H_FOREBONE = box(26, 16, 10, 2, 2)
H_COVERTS = box(0, 22, 10, 0, 9)
H_FLIGHT = box(0, 32, 12, 0, 11)
H_PRIMARIES = box(0, 44, 8, 0, 14)
H_TAIL = box(48, 22, 9, 0, 11)


def zigzag(w, h, seed, start=None):
    """A lightning streak across a face: (x, y) pixels zigging down it."""
    rng = random.Random(seed)
    x = start if start is not None else rng.randrange(w)
    out = []
    for y in range(h):
        out.append((x, y))
        if y % 2 == 1:
            x = max(0, min(w - 1, x + rng.choice((-1, 1))))
            out.append((x, y))
    return out


def long_feathers(cv, area, tones, seed, face, along_x, tips=None, tip_band=3, bolt=None, glow_cv=None):
    """Flight feathers laid side by side: each a strip with a pale shaft, darker at its trailing edge, its tip coloured."""
    x0, y0, w, h = area
    for x, y, px, py in cells(area):
        k, length, pos = (y, w, x) if along_x else (x, h, y)
        feather = k // 2
        shaft = k % 2 == 0
        t = 3 + (0 if face in ("top", "front") else -1) - (1 if feather % 2 else 0)
        if shaft and noise(px, py, seed) > 0.25:
            t += 1
        c = shade(tones, t)
        if tips is not None and pos >= length - tip_band:
            c = tips
        if pos == length - 1 and (feather % 2 == 0):
            c = shade(tones, 0)
        cv.put(px, py, c)
    if bolt is not None:
        bw, bh = (w, h)
        for (bx, by) in zigzag(bh, bw, seed + 7) if along_x else zigzag(bw, bh, seed + 7):
            gx, gy = (by, bx) if along_x else (bx, by)
            if 0 <= gx < w and 0 <= gy < h:
                cv.put(x0 + gx, y0 + gy, bolt)
                if glow_cv is not None:
                    glow_cv.put(x0 + gx, y0 + gy, (255, 240, 120, 255))


def harpy_paint(glow=False):
    cv = Sheet(128, 64)
    g = Sheet(128, 64)
    # Body: a pale breast barred with chevrons, storm-blue back and sides, a streak of lightning down the spine.
    for name, area in faces(H_BODY):
        if name == "front":
            for x, y, px, py in cells(area):
                t = 2 + (1 if noise(px, py, 5) > 0.7 else 0)
                c = shade(PALE, t)
                if (y + abs(x - 2.5)) % 3 < 1:
                    c = shade(STORMF, 4)
                cv.put(px, py, c)
        else:
            feathers(cv, area, STORMF, 10 + len(name), name, base=3)
    bx, by, bw, bh = H_BODY["back"]
    for (x, y) in zigzag(bw, bh, 3, start=2):
        cv.put(bx + x, by + y, BOLT)
        g.put(bx + x, by + y, (255, 238, 110, 255))
    for name, area in faces(H_RUFF):
        feathers(cv, area, PALE, 20 + len(name), name, base=2, row=2, flip=True)
    # Head: storm feathers, a dark raptor's mask round fierce yellow eyes.
    for name, area in faces(H_HEAD):
        feathers(cv, area, STORMF, 30 + len(name), name, base=3, row=2)
    hx, hy, hw, hh = H_HEAD["front"]
    for x in range(hw):
        cv.put(hx + x, hy + 2, STORMF[0])
        cv.put(hx + x, hy + 3, STORMF[1])
    for (x, y, c) in ((1, 2, BOLT), (4, 2, BOLT), (0, 2, STORMF[0]), (5, 2, STORMF[0])):
        cv.put(hx + x, hy + y, c)
    for x in range(1, hw - 1):
        cv.put(hx + x, hy + hh - 1, PALE[2])
    # Beak: gold horn, dark at the hooked tip.
    for name, area in faces(H_BEAK):
        x0, y0, w, h = area
        for x, y, px, py in cells(area):
            t = 3 + LIGHT[name] - (2 if y == h - 1 else 0) + (1 if x == 0 else 0)
            cv.put(px, py, shade(HORN, t))
    # Crest: long blade feathers, their tips lightning-yellow.
    for name, area in faces(H_CREST, "front", "back"):
        x0, y0, w, h = area
        for x, y, px, py in cells(area):
            blade = (x + y // 2) % 3
            if blade == 0 and y < h - 1 and noise(px, py, 61) < 0.3:
                continue
            c = shade(STORMF, 2 + blade)
            if y < 2:
                c = BOLT if x % 2 == 0 else BOLT_HOT
                g.put(px, py, (255, 240, 120, 255) if x % 2 == 0 else (255, 250, 210, 200))
            cv.put(px, py, c)
    # Legs: feathered thighs, scaled yellow shins, dark talons.
    for name, area in faces(H_THIGH):
        feathers(cv, area, STORMF, 40 + len(name), name, base=3, row=2)
    for part in (H_SHIN, H_FOOT):
        for name, area in faces(part):
            for x, y, px, py in cells(area):
                t = 2 + LIGHT[name] + (1 if (x + y) % 2 == 0 else 0)
                cv.put(px, py, shade(SCALE, t))
    for name, area in faces(H_TALON):
        fill(cv, area, hexc("#18120C") if name != "top" else hexc("#3A3020"))
    # Wing bones: dark blue, a pale edge.
    for part in (H_BONE, H_FOREBONE):
        for name, area in faces(part):
            feathers(cv, area, STORMF, 50 + len(name), name, base=2, row=2)
    # Feathers: coverts in small scalloped rows; flight feathers and primaries long, crossed by a lightning streak.
    for name, area in faces(H_COVERTS, "top", "bottom"):
        feathers(cv, area, STORMF if name == "top" else PALE, 60 + len(name), "top" if name == "top" else "front", base=3, row=2)
    for name, area in faces(H_FLIGHT, "top", "bottom"):
        long_feathers(cv, area, STORMF if name == "top" else PALE, 70 + len(name), "top" if name == "top" else "bottom", along_x=False,
                      tips=STORMF[1] if name == "top" else PALE[1], bolt=BOLT if name == "top" else None, glow_cv=g)
    for name, area in faces(H_PRIMARIES, "top", "bottom"):
        long_feathers(cv, area, STORMF if name == "top" else PALE, 80 + len(name), "top" if name == "top" else "bottom", along_x=False,
                      tips=BOLT if name == "top" else PALE[0], tip_band=2, glow_cv=None)
        if name == "top":
            x0, y0, w, h = area
            for x in range(0, w, 2):
                for y in range(h - 2, h):
                    g.put(x0 + x, y0 + y, (255, 238, 110, 230))
    for name, area in faces(H_TAIL, "top", "bottom"):
        x0, y0, w, h = area
        for x, y, px, py in cells(area):
            c = shade(STORMF if name == "top" else PALE, 3 - (1 if x % 3 == 0 else 0))
            if name == "top" and y % 4 == 2:
                c = BOLT
                g.put(px, py, (255, 238, 110, 200))
            cv.put(px, py, c)
    return g.image() if glow else cv.image()


def harpy_eyes_texture():
    cv = Sheet(128, 64)
    hx, hy, _, _ = H_HEAD["front"]
    for x in (1, 4):
        cv.put(hx + x, hy + 2, (255, 252, 200, 255))
    for x in (0, 2, 3, 5):
        cv.put(hx + x, hy + 2, (255, 230, 80, 110))
    return cv.image()


# ============================================================== the Geode Crawler (GeodeCrawlerModel, 128x64)

CHITIN = ramp("#0C0911", "#151020", "#1F182E", "#2B2240", "#3A2F56", "#4D3F70")
AMETHYST = ramp("#2A1450", "#47288A", "#6A44BC", "#8E66E0", "#B996F4", "#E2D2FF")
C_BODY = box(0, 0, 12, 5, 14)
C_HEAD = box(52, 0, 8, 4, 5)
C_MANDIBLE = box(78, 0, 2, 1, 4)
C_CRYSTALS = [box(90, 0, 3, 7, 3), box(102, 0, 2, 5, 2), box(110, 0, 2, 3, 2)]
C_FEELER = box(118, 0, 1, 1, 4)
C_LEG = box(0, 20, 7, 2, 2)
C_SHIN = box(18, 20, 2, 6, 2)
C_SHELL = box(40, 20, 13, 2, 12)
C_BALL = box(0, 36, 12, 12, 12)


def crystal_spots(cv, area, seed, count, glow_cv=None):
    """Where crystal grows through the shell: a lilac patch with a bright heart."""
    x0, y0, w, h = area
    rng = random.Random(seed)
    for _ in range(count):
        cx, cy = rng.randrange(w), rng.randrange(h)
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            if 0 <= cx + dx < w and 0 <= cy + dy < h:
                cv.put(x0 + cx + dx, y0 + cy + dy, AMETHYST[2 + (dx + dy == 0)])
        if glow_cv is not None:
            glow_cv.put(x0 + cx, y0 + cy, (214, 190, 255, 200))


def geode_crawler_paint(glow=False):
    cv = Sheet(128, 64)
    g = Sheet(128, 64)
    for name, area in faces(C_BODY):
        chitin(cv, area, CHITIN, 10 + len(name), name, base=2, seam=3 if name == "bottom" else 5, vertical=name in ("top", "bottom"))
    for name, area in faces(C_SHELL):
        chitin(cv, area, CHITIN, 20 + len(name), name, base=3, seam=4)
        if name == "top":
            crystal_spots(cv, area, 21, 9, g)
    for name, area in faces(C_BALL):
        chitin(cv, area, CHITIN, 30 + len(name), name, base=3, seam=3, vertical=name in ("right", "left"))
        crystal_spots(cv, area, 31 + len(name), 3, g)
    for name, area in faces(C_HEAD):
        chitin(cv, area, CHITIN, 40 + len(name), name, base=2, seam=3)
    hx, hy, hw, hh = C_HEAD["front"]
    for x in (1, 6):
        cv.put(hx + x, hy + 1, AMETHYST[4])
        g.put(hx + x, hy + 1, (232, 220, 255, 255))
    for x in range(2, 6):
        cv.put(hx + x, hy + hh - 1, CHITIN[0])
    for name, area in faces(C_MANDIBLE):
        x0, y0, w, h = area
        for x, y, px, py in cells(area):
            cv.put(px, py, shade(CHITIN, 3 + LIGHT[name]))
    mx, my, mw, mh = C_MANDIBLE["front"]
    cv.put(mx, my, hexc("#D8C8B8"))
    cv.put(mx + 1, my, hexc("#B8A898"))
    for part in (C_FEELER, C_LEG, C_SHIN):
        for name, area in faces(part):
            chitin(cv, area, CHITIN, 50 + len(name), name, base=2, seam=3, vertical=part is C_LEG)
    for b in C_CRYSTALS:
        for name, area in faces(b):
            crystal(cv, area, AMETHYST, 60 + len(name), name)
            x0, y0, w, h = area
            for x, y, px, py in cells(area):
                d = x - y * (w / max(1, h))
                edge = abs(d) < 0.7
                g.put(px, py, (232, 214, 255, 230) if edge else (150, 100, 240, 70 if name != "top" else 140))
    return g.image() if glow else cv.image()


# ============================================================== the Bog Witch-Frog (BogWitchFrogModel, 128x64)

FROG = ramp("#16220E", "#223418", "#304822", "#41602C", "#567836", "#6E9244")
BRUISE = ramp("#2A1A2C", "#3E2640", "#563452", "#6E4466")
BELLY = ramp("#5E5A2C", "#7E7838", "#A09A4C", "#C0B864", "#D8D07E")
WART_TOP = hexc("#9AB862")
WART_RIM = hexc("#223418")
F_BODY = box(0, 0, 14, 8, 14)
F_HEAD = box(56, 0, 15, 5, 10)
F_EYE = box(106, 0, 4, 3, 4)
F_JAW = box(0, 22, 15, 2, 10)
F_SAC = box(50, 22, 8, 5, 5)
F_FORELEG = box(76, 22, 3, 6, 3)
F_FOREFOOT = box(88, 22, 5, 1, 5)
F_THIGH = box(0, 36, 5, 5, 9)
F_HINDFOOT = box(28, 36, 6, 1, 8)
F_PAD = box(56, 36, 10, 0, 10)
F_STEM = box(96, 36, 1, 2, 1)
F_CAP = box(100, 36, 3, 2, 3)
F_MOSS = box(112, 36, 0, 5, 6)
F_TONGUE = box(60, 48, 2, 1, 12)


def frog_skin(cv, area, seed, face, belly_from=None):
    """Mossy frog skin, bruise-purple blotches and warts; down a side it fades into a sallow belly."""
    x0, y0, w, h = area
    mottle(cv, area, FROG, BRUISE, seed, face, base=3, blotchiness=0.22)
    if belly_from is not None:
        for x, y, px, py in cells(area):
            if y >= belly_from + (1 if noise(px, 0, seed) > 0.5 else 0):
                cv.put(px, py, shade(BELLY, 2 + (1 if smooth(px, py, seed + 4, 2.5) > 0.66 else 0) - (1 if y == h - 1 else 0)))
    if face in ("top", "front", "right", "left", "back"):
        warts(cv, area, WART_RIM, WART_TOP, seed + 7, max(1, w * h // 26))


def bog_witch_frog_texture():
    cv = Sheet(128, 64)
    for name, area in faces(F_BODY):
        if name == "bottom":
            for x, y, px, py in cells(area):
                cv.put(px, py, shade(BELLY, 2 + (1 if noise(px, py, 3) > 0.7 else 0)))
        else:
            frog_skin(cv, area, 10 + len(name), name, belly_from=5 if name in ("front", "right", "left", "back") else None)
    for name, area in faces(F_HEAD):
        frog_skin(cv, area, 20 + len(name), name)
    # The mouth: a long dark line along the head's lower edge, curling up at the corners; two nostrils.
    hx, hy, hw, hh = F_HEAD["front"]
    for x in range(hw):
        cv.put(hx + x, hy + hh - 1, hexc("#1A1210"))
    cv.put(hx, hy + hh - 2, hexc("#1A1210"))
    cv.put(hx + hw - 1, hy + hh - 2, hexc("#1A1210"))
    cv.put(hx + 6, hy + 1, hexc("#101808"))
    cv.put(hx + 8, hy + 1, hexc("#101808"))
    for name in ("right", "left"):
        sx, sy, sw, sh = F_HEAD[name]
        for x in range(sw):
            cv.put(sx + x, sy + sh - 1, hexc("#1A1210"))
    # The jaw: a pale lower lip outside, a dark red mouth inside.
    for name, area in faces(F_JAW):
        if name == "top":
            for x, y, px, py in cells(area):
                cv.put(px, py, hexc("#6A2A2A") if noise(px, py, 5) > 0.2 else hexc("#4A1C1C"))
        elif name == "bottom":
            for x, y, px, py in cells(area):
                cv.put(px, py, shade(BELLY, 2 + (1 if noise(px, py, 6) > 0.75 else 0)))
        else:
            frog_skin(cv, area, 30 + len(name), name, belly_from=1)
    # Eyes: lamp-yellow, a black slit across, a heavy green lid.
    for name, area in faces(F_EYE):
        x0, y0, w, h = area
        if name in ("front", "top"):
            for x, y, px, py in cells(area):
                c = hexc("#E8C838") if (x + y) % 3 else hexc("#F4DC5A")
                if name == "front" and y == h // 2 + (0 if name == "front" else 0) and 0 < x < w - 1:
                    c = hexc("#0A0A06")
                if name == "top" and x == w // 2:
                    c = hexc("#0A0A06")
                if name == "front" and y == 0:
                    c = FROG[4]
                cv.put(px, py, c)
        else:
            frog_skin(cv, area, 40 + len(name), name)
    # The throat sac: pale, stretched thin, veined.
    for name, area in faces(F_SAC):
        for x, y, px, py in cells(area):
            c = hexc("#A8B474") if smooth(px, py, 50, 2) > 0.3 else hexc("#909C5E")
            if (x * 2 + y) % 5 == 0:
                c = hexc("#7E8C4E")
            cv.put(px, py, c)
    # Legs: green with darker bands; webbed feet.
    for part, seed in ((F_FORELEG, 60), (F_THIGH, 70)):
        for name, area in faces(part):
            frog_skin(cv, area, seed + len(name), name, belly_from=None)
            x0, y0, w, h = area
            for x, y, px, py in cells(area):
                if (x if part is F_THIGH and name in ("right", "left", "top", "bottom") else y) % 3 == 0 and name != "bottom":
                    cv.put(px, py, shade(FROG, 1))
    for part, seed in ((F_FOREFOOT, 80), (F_HINDFOOT, 90)):
        for name, area in faces(part):
            x0, y0, w, h = area
            for x, y, px, py in cells(area):
                c = shade(FROG, 3 + LIGHT[name])
                if name == "top" and x % 2 == 1 and y < h - 1:
                    c = shade(FROG, 1)
                cv.put(px, py, c)
    # The lily pad hat: radial veins, a notch cut out, a paler underside.
    for name, area in faces(F_PAD, "top", "bottom"):
        x0, y0, w, h = area
        cx, cy = (w - 1) / 2, (h - 1) / 2
        for x, y, px, py in cells(area):
            dx, dy = x - cx, y - cy
            r = math.hypot(dx, dy)
            if r > w / 2:
                continue
            ang = math.atan2(dy, dx)
            if -0.35 < ang < 0.35 and r > 0.8:
                continue
            c = hexc("#3E8A34") if name == "top" else hexc("#5AA048")
            if abs(math.sin(ang * 4)) < 0.18:
                c = hexc("#2A6024") if name == "top" else hexc("#78B860")
            if r > w / 2 - 1.2:
                c = hexc("#2E6A2A")
            cv.put(px, py, c)
    for name, area in faces(F_STEM):
        fill(cv, area, hexc("#D8CCB0") if name != "bottom" else hexc("#A89C80"))
    for name, area in faces(F_CAP):
        x0, y0, w, h = area
        for x, y, px, py in cells(area):
            c = hexc("#B8322A") if name != "bottom" else hexc("#D8C8A8")
            if name in ("top", "front", "back", "right", "left") and (x + y * 2) % 3 == 0:
                c = hexc("#F4ECE0")
            cv.put(px, py, c)
    for name, area in faces(F_MOSS, "front", "back"):
        strands(cv, area, [hexc("#3E5A20"), hexc("#4E6E28"), hexc("#2E4418")], 95 + len(name), density=0.75)
    for name, area in faces(F_TONGUE):
        x0, y0, w, h = area
        for x, y, px, py in cells(area):
            cv.put(px, py, hexc("#E07A8A") if name == "top" else hexc("#B84A5E"))
    return cv.image()


def bog_witch_frog_eyes_texture():
    cv = Sheet(128, 64)
    for name in ("front", "top"):
        x0, y0, w, h = F_EYE[name]
        for x, y, px, py in cells(F_EYE[name]):
            if name == "front" and y == 0:
                continue
            slit = (name == "front" and y == h // 2 and 0 < x < w - 1) or (name == "top" and x == w // 2)
            if not slit:
                cv.put(px, py, (250, 226, 90, 230))
    return cv.image()


def bog_witch_frog_glow_texture():
    """The throat sac, glowing a sickly green as it swells."""
    cv = Sheet(128, 64)
    for name, area in faces(F_SAC):
        for x, y, px, py in cells(area):
            vein = (x * 2 + y) % 5 == 0
            cv.put(px, py, (200, 255, 120, 255) if vein else (150, 230, 80, 170))
    return cv.image()


# ============================================================== the Mana Ooze (ManaOozeModel, 64x32)

O_OUTER = box(0, 0, 8, 8, 8)
O_INNER = box(0, 16, 6, 6, 6)
O_CORE = box(32, 0, 4, 4, 4)
O_MOTE = box(48, 0, 2, 2, 2)
JELLY = hexc("#E89AE0")
JELLY_EDGE = hexc("#FFD4FA")
JELLY_DEEP = hexc("#B464C8")
# A faint rune written on each outer face.
O_GLYPH = [(2, 2), (3, 2), (4, 2), (5, 2), (3, 3), (4, 4), (3, 5), (2, 5), (5, 5)]


def mana_ooze_texture():
    cv = Sheet(64, 32)
    for name, area in faces(O_OUTER):
        x0, y0, w, h = area
        for x, y, px, py in cells(area):
            edge = x in (0, w - 1) or y in (0, h - 1)
            if edge:
                cv.put(px, py, rgba(JELLY_EDGE, 170))
            else:
                cv.put(px, py, rgba(JELLY, 70 + (30 if noise(px, py, 4) > 0.8 else 0)))
        if name in ("front", "right", "left", "back", "top"):
            for (gx, gy) in O_GLYPH:
                cv.put(x0 + gx, y0 + gy, rgba(hexc("#FFE8FC"), 120))
        if name in ("front", "top", "left"):
            cv.put(x0 + 1, y0 + 1, rgba(hexc("#FFFFFF"), 210))
            cv.put(x0 + 2, y0 + 1, rgba(hexc("#FFFFFF"), 150))
    for name, area in faces(O_INNER):
        x0, y0, w, h = area
        for x, y, px, py in cells(area):
            cv.put(px, py, rgba(mix(JELLY_DEEP, JELLY, noise(px, py, 7) * 0.6), 150))
    for name, area in faces(O_CORE):
        x0, y0, w, h = area
        for x, y, px, py in cells(area):
            centre = x in (1, 2) and y in (1, 2)
            cv.put(px, py, rgba(hexc("#FFF0FE") if centre else hexc("#F2A8EC"), 255))
    for name, area in faces(O_MOTE):
        fill(cv, area, rgba(hexc("#E8DCFF"), 255))
    return cv.image()


def mana_ooze_glow_texture():
    cv = Sheet(64, 32)
    for name, area in faces(O_CORE):
        x0, y0, w, h = area
        for x, y, px, py in cells(area):
            centre = x in (1, 2) and y in (1, 2)
            cv.put(px, py, (255, 255, 255, 255) if centre else (240, 140, 230, 220))
    for name, area in faces(O_MOTE):
        fill(cv, area, (220, 200, 255, 255))
    for name, area in faces(O_INNER):
        x0, y0, w, h = area
        for x, y, px, py in cells(area):
            if noise(px, py, 9) > 0.86:
                cv.put(px, py, (230, 150, 240, 120))
    return cv.image()


# ============================================================== the Bog Witch-Frog's bubble (BogBubbleRenderer, 32x32)

BUB_OUTER = box(0, 0, 6, 6, 6)
BUB_CORE = box(0, 12, 3, 3, 3)


def bog_bubble_texture():
    cv = Sheet(32, 32)
    for name, area in faces(BUB_OUTER):
        x0, y0, w, h = area
        for x, y, px, py in cells(area):
            edge = x in (0, w - 1) or y in (0, h - 1)
            cv.put(px, py, (170, 230, 110, 175) if edge else (120, 200, 70, 95))
        cv.put(x0 + 1, y0 + 1, (240, 255, 220, 230))
        cv.put(x0 + 2, y0 + 1, (220, 255, 200, 170))
        cv.put(x0 + 1, y0 + 2, (220, 255, 200, 170))
    for name, area in faces(BUB_CORE):
        fill(cv, area, (150, 220, 70, 255))
    return cv.image()


def bog_bubble_glow_texture():
    cv = Sheet(32, 32)
    for name, area in faces(BUB_CORE):
        x0, y0, w, h = area
        for x, y, px, py in cells(area):
            cv.put(px, py, (230, 255, 160, 255) if (x, y) == (1, 1) else (170, 250, 90, 230))
    return cv.image()


# ============================================================== the drops' icons (16x16)

ICONS = {
    "living_bramble": ("""
        ................
        ..........t.....
        .........gGt....
        ....t...gGg.....
        ...gGg.bbgl.....
        ...lGgbBbl......
        ....lbBbg...t...
        ...t.bBb...gGt..
        .....bBbgggGl...
        ...gGbBbbllb....
        ..tGlbBbbbbb....
        ....lbBb..b.....
        .....bBb...t....
        ....dbbbd.......
        ...d.d.d.d......
        ................
    """, {"b": "#5E4228", "B": "#8A653E", "d": "#3A2816", "g": "#4E8A30", "G": "#7AB84A", "l": "#305C1E", "t": "#D6C69A"}),
    "shadow_pelt": ("""
        ................
        ................
        ...oo......oo...
        ..oPPo....oPPo..
        ..oPpPoooPPpPo..
        ...oPpppppppPo..
        ...opppvpppppo..
        ..oppvpppppvppo.
        ..opppppvpppppo.
        ..oppvppppppvpo.
        ..oppppppvppppo.
        ...oppvpppppoo..
        ...oPpppppPo....
        ..oPPo.ooPPo....
        ..oo....oo......
        ................
    """, {"o": "#05030A", "p": "#1E1232", "P": "#2E1A4E", "v": "#7A4AB8"}),
    "storm_feather": ("""
        ................
        ............ooo.
        ...........oBbo.
        ..........oBbbo.
        .........oBbYbo.
        ........oBbYbbo.
        .......oBbbYbo..
        ......oBbYbbo...
        .....oBbbYbo....
        ....oBbYbbo.....
        ...oBbbbbo......
        ...obbbbo.......
        ..owoooo........
        .ow.............
        ow..............
        ................
    """, {"o": "#141A2A", "b": "#3E5278", "B": "#7892B8", "Y": "#FFE650", "w": "#DCE4EE"}),
    "bog_gland": ("""
        ................
        ................
        ......oooo......
        ....oolLLloo....
        ...olLLWlllvo...
        ..olLWWllllvvo..
        ..olLWlllllvvo..
        ..olllllllvvvo..
        ..ollvllllvvvo..
        ..olllllllvvdo..
        ...ollllvlvddo..
        ....olllvvddo...
        .....oddddoo....
        ......oooo......
        ................
        ................
    """, {"o": "#1E2E10", "l": "#9CC85A", "L": "#C8E88A", "W": "#F0FFD0", "v": "#6E9A38", "d": "#4E6E28"}),
    "mana_gel": ("""
        ................
        ................
        .......oo.......
        ......oPPo......
        .....oPWPpo.....
        ....oPWWPppo....
        ...oPPWPpppPo...
        ...oPPPppcpPo...
        ..oPPPpccCcpPo..
        ..oPPppcCCcppo..
        ..oPpppccccppo..
        ...oppppcpppo...
        ....oopppppo....
        ......ooooo.....
        ................
        ................
    """, {"o": "#5A2A6A", "P": "#F2A8EC", "p": "#D07ACC", "W": "#FFF0FE", "c": "#FFD4FA", "C": "#FFFFFF"}),
}


def icon(path):
    art, pal = ICONS[path]
    cv = Canvas()
    blit(cv, art, {k: hexc(v) for k, v in pal.items()})
    return cv.image()


# ============================================================== spawn eggs (16x16)

EGG = """
    ................
    .......oo.......
    .....oo11oo.....
    ....o111111o....
    ...o11111111o...
    ...o11111111o...
    ..o1111111111o..
    ..o1111111111o..
    ..o1111111111o..
    ..o1111111111o..
    ..o1111111111o..
    ...o11111111o...
    ...o11111111o...
    ....oo1111oo....
    ......oooo......
    ................
"""
# Each egg: shell ramp (dark, mid, light), spots, and a motif over it in its own colours.
EGGS = {
    "bramblewalker": (("#3E2B1A", "#6E4F30", "#8A653E"), "#4E8A30", """
        ................
        ......t..t......
        .......tt.......
        ................
        ................
        ....l......l....
        ...lgl....lgl...
        ................
        .....e....e.....
        ................
        ................
        ................
        ................
        ................
        ................
        ................
    """, {"t": "#D6C69A", "l": "#305C1E", "g": "#7AB84A", "e": "#C8FF8A"}),
    "gloomstalker": (("#0C0714", "#1E1232", "#2E1A4E"), "#3A2660", """
        ................
        ................
        ................
        ....p......p....
        ................
        ................
        ................
        ....vV....Vv....
        ................
        ................
        ................
        ................
        ................
        ................
        ................
        ................
    """, {"p": "#5A3A8A", "v": "#B45AF0", "V": "#FFFFFF"}),
    "thunderwing_harpy": (("#1E2840", "#3E5278", "#7892B8"), "#A0B6D2", """
        ................
        ................
        ................
        ................
        ................
        ................
        bb..........bb..
        .bY........Yb...
        ..b........b....
        ......Y.........
        .....Y..........
        ......Y.........
        .....Y..........
        ................
        ................
        ................
    """, {"b": "#141A2A", "Y": "#FFE650"}),
    "geode_crawler": (("#151020", "#2B2240", "#3A2F56"), "#4D3F70", """
        ................
        ................
        .......C........
        ......CcC.......
        ...C..CcC..c....
        ..CcC.....CcC...
        ..CcC...........
        ................
        ......a..a......
        ................
        ................
        ................
        ................
        ................
        ................
        ................
    """, {"C": "#C6B0F2", "c": "#7556B8", "a": "#EEE6FF"}),
    "bog_witch_frog": (("#223418", "#41602C", "#567836"), "#563452", """
        ................
        ................
        ....rrr.........
        ...rwrrr........
        ....|...........
        ..ppppppp.......
        ................
        ...Y......Y.....
        ................
        ................
        ..kkkkkkkkkk....
        ................
        ................
        ................
        ................
        ................
    """, {"r": "#B8322A", "w": "#F4ECE0", "|": "#D8CCB0", "p": "#3E8A34", "Y": "#F4DC5A", "k": "#1A1210"}),
    "mana_ooze": (("#B464C8", "#E89AE0", "#FFD4FA"), "#FFF0FE", """
        ................
        ................
        ................
        ................
        ................
        ......cc........
        .....cCCc.......
        .....cCCc.......
        ......cc........
        ................
        ................
        ................
        ................
        ................
        ................
        ................
    """, {"c": "#F2A8EC", "C": "#FFFFFF"}),
}


def egg_icon(kind):
    shell, spot, motif, pal = EGGS[kind]
    dark, mid, light = (hexc(c) for c in shell)
    cv = Canvas()
    rows = [r.strip() for r in EGG.strip("\n").splitlines()]
    outline = mix(dark, (0, 0, 0), 0.55)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == "o":
                cv.put(x, y, outline)
            elif ch == "1":
                # Lit from the top left, darker to the bottom right; spots scattered.
                t = (x + y) / 26
                c = light if t < 0.42 else mid if t < 0.72 else dark
                if noise(x, y, len(kind)) > 0.8:
                    c = hexc(spot)
                cv.put(x, y, c)
    for y, row in enumerate(r.strip() for r in motif.strip("\n").splitlines()):
        for x, ch in enumerate(row):
            if ch in pal and cv.get(x, y) is not None:
                cv.put(x, y, hexc(pal[ch]))
            elif ch in pal and kind == "thunderwing_harpy":
                # The harpy's wings spread past the shell.
                cv.put(x, y, hexc(pal[ch]))
            elif ch in pal and kind == "bramblewalker":
                cv.put(x, y, hexc(pal[ch]))
    return cv.image()


# ============================================================== loot, brewing and text

MONSTERS = ["bramblewalker", "gloomstalker", "thunderwing_harpy", "geode_crawler", "bog_witch_frog", "mana_ooze"]
ITEMS = ["living_bramble", "shadow_pelt", "storm_feather", "bog_gland", "mana_gel"]


def _drop(item, low, high, looting=1.0, player_only=False):
    pool = {"rolls": 1, "entries": [{"type": "minecraft:item", "name": item, "functions": [
        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}},
        {"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting", "count": {"type": "minecraft:uniform", "min": 0.0, "max": looting}}]}]}
    if player_only:
        pool["conditions"] = [{"condition": "minecraft:killed_by_player"}]
    return pool


LOOT = {
    "bramblewalker": [_drop("wildercord:living_bramble", 1, 2), _drop("minecraft:stick", 0, 2)],
    "gloomstalker": [_drop("wildercord:shadow_pelt", 1, 1, player_only=False)],
    "thunderwing_harpy": [_drop("wildercord:storm_feather", 1, 3), _drop("minecraft:feather", 0, 2)],
    "geode_crawler": [_drop("wildercord:geode_grit", 1, 2), _drop("minecraft:amethyst_shard", 0, 2)],
    "bog_witch_frog": [_drop("wildercord:bog_gland", 1, 1), _drop("minecraft:slime_ball", 0, 2)],
    "mana_ooze": [_drop("wildercord:mana_gel", 1, 2)],
}

# (base potion, reagent, result): a Shadow Pelt brews invisibility straight from an awkward potion (no night vision
# needed); a Bog Gland brews water breathing, and turns even a thick potion (otherwise good for nothing) into leaping.
BREWS = [("minecraft:awkward", "wildercord:shadow_pelt", "minecraft:invisibility"),
         ("minecraft:awkward", "wildercord:bog_gland", "minecraft:water_breathing"),
         ("minecraft:thick", "wildercord:bog_gland", "minecraft:leaping")]

LANG = {
    # The field guide's notes on each monster, and where to look before one is met (spell/FieldGuide.java).
    "guide.wildercord.bramblewalker": "A walking thicket of the night forests. It rears back before its vines lash out to root you: step aside. Fire makes it burn and run",
    "guide.wildercord.bramblewalker.hint": "A bush that wasn't there by daylight",
    "guide.wildercord.gloomstalker": "A shadow panther, all but invisible in the dark until it pounces. Light, glow and any spell of fire, storm, arcane or life lay it bare",
    "guide.wildercord.gloomstalker.hint": "Two violet eyes in the dark woods and deep caves",
    "guide.wildercord.thunderwing_harpy": "A storm-feathered hunter of the peaks. It shrieks before it dives, and in a storm marks the ground before the lightning falls. Earth drags it down",
    "guide.wildercord.thunderwing_harpy.hint": "A shriek above the mountains, worse in a storm",
    "guide.wildercord.geode_crawler": "A crystal-backed beetle of the caves that curls up when struck and rolls at you. Blunt blows and shocks crack it open",
    "guide.wildercord.geode_crawler.hint": "Something rattling near the amethyst",
    "guide.wildercord.bog_witch_frog": "A huge swamp frog that lobs poison bubbles and swallows small creatures whole. Pop a bubble before it lands; a shield stops its tongue",
    "guide.wildercord.bog_witch_frog.hint": "A croak in the swamp at night",
    "guide.wildercord.mana_ooze": "A slime that drinks magic: spells feed it until it bursts in two. Blades, arrows and fire do what spells can't",
    "guide.wildercord.mana_ooze.hint": "A glow in the deep places where the ley lines run",
    "entity.wildercord.bramblewalker": "Bramblewalker",
    "entity.wildercord.gloomstalker": "Gloomstalker",
    "entity.wildercord.thunderwing_harpy": "Thunderwing Harpy",
    "entity.wildercord.geode_crawler": "Geode Crawler",
    "entity.wildercord.bog_witch_frog": "Bog Witch-Frog",
    "entity.wildercord.mana_ooze": "Mana Ooze",
    "entity.wildercord.bog_bubble": "Bog Bubble",
    "entity.wildercord.thrown_bramble": "Thrown Living Bramble",
    "item.wildercord.bramblewalker_spawn_egg": "Bramblewalker Spawn Egg",
    "item.wildercord.gloomstalker_spawn_egg": "Gloomstalker Spawn Egg",
    "item.wildercord.thunderwing_harpy_spawn_egg": "Thunderwing Harpy Spawn Egg",
    "item.wildercord.geode_crawler_spawn_egg": "Geode Crawler Spawn Egg",
    "item.wildercord.bog_witch_frog_spawn_egg": "Bog Witch-Frog Spawn Egg",
    "item.wildercord.mana_ooze_spawn_egg": "Mana Ooze Spawn Egg",
    "item.wildercord.living_bramble": "Living Bramble",
    "item.wildercord.living_bramble.lore": "Still creeping, still hungry for an ankle to hold.",
    "item.wildercord.living_bramble.use": "Throw it: it roots the creature it hits for 2 seconds. Composts well.",
    "item.wildercord.shadow_pelt": "Shadow Pelt",
    "item.wildercord.shadow_pelt.lore": "Light slides off it. Hold it up and your hand is hard to see.",
    "item.wildercord.shadow_pelt.use": "Brewing: an Awkward Potion and a Shadow Pelt make a Potion of Invisibility.",
    "item.wildercord.storm_feather": "Storm Feather",
    "item.wildercord.storm_feather.lore": "It hums before thunder, and stands up when lightning is near.",
    "item.wildercord.storm_feather.use": "Use: a gust lifts you a few blocks, and you drift down after.",
    "item.wildercord.bog_gland": "Bog Gland",
    "item.wildercord.bog_gland.lore": "The frog's poison sac, emptied. It still smells of the swamp.",
    "item.wildercord.bog_gland.use": "Brewing: with an Awkward Potion, Water Breathing; with a Thick Potion, Leaping.",
    "item.wildercord.mana_gel": "Mana Gel",
    "item.wildercord.mana_gel.lore": "A Mana Ooze's jelly, still full of the spells it ate.",
    "item.wildercord.mana_gel.use": "Eat it: 15 mana back at once.",
    "message.wildercord.gloomstalker_revealed": "Light falls on the Gloomstalker: it can't hide for a moment!",
    "message.wildercord.geode_crawler_curled": "Its crystal turns the blow: crack it with a mace, a pickaxe, a blast or a shock",
    "message.wildercord.mana_ooze_drinks": "The Mana Ooze drinks your spell: try a blade, an arrow or fire",
    "subtitles.wildercord.kit.bramblewalker.ambient": "Bramblewalker creaks",
    "subtitles.wildercord.kit.bramblewalker.hurt": "Bramblewalker splinters",
    "subtitles.wildercord.kit.bramblewalker.death": "Bramblewalker collapses",
    "subtitles.wildercord.kit.bramblewalker.rear": "Bramblewalker rears back",
    "subtitles.wildercord.kit.bramblewalker.lash": "Vine lashes",
    "subtitles.wildercord.kit.gloomstalker.ambient": "Gloomstalker purrs",
    "subtitles.wildercord.kit.gloomstalker.hurt": "Gloomstalker hisses",
    "subtitles.wildercord.kit.gloomstalker.death": "Gloomstalker fades",
    "subtitles.wildercord.kit.gloomstalker.growl": "Gloomstalker growls",
    "subtitles.wildercord.kit.gloomstalker.pounce": "Gloomstalker pounces",
    "subtitles.wildercord.kit.gloomstalker.reveal": "Gloomstalker is revealed",
    "subtitles.wildercord.kit.harpy.ambient": "Harpy calls",
    "subtitles.wildercord.kit.harpy.shriek": "Harpy shrieks",
    "subtitles.wildercord.kit.harpy.hurt": "Harpy hurts",
    "subtitles.wildercord.kit.harpy.death": "Harpy dies",
    "subtitles.wildercord.kit.harpy.dive": "Harpy dives",
    "subtitles.wildercord.kit.geode_crawler.ambient": "Geode Crawler clicks",
    "subtitles.wildercord.kit.geode_crawler.curl": "Geode Crawler curls up",
    "subtitles.wildercord.kit.geode_crawler.rattle": "Geode Crawler rattles",
    "subtitles.wildercord.kit.geode_crawler.roll": "Geode Crawler rolls",
    "subtitles.wildercord.kit.geode_crawler.crack": "Crystal cracks",
    "subtitles.wildercord.kit.geode_crawler.hurt": "Geode Crawler hurts",
    "subtitles.wildercord.kit.geode_crawler.death": "Geode Crawler shatters",
    "subtitles.wildercord.kit.frog.ambient": "Bog Witch-Frog croaks",
    "subtitles.wildercord.kit.frog.swell": "Bog Witch-Frog's throat swells",
    "subtitles.wildercord.kit.frog.spit": "Bog Witch-Frog spits",
    "subtitles.wildercord.kit.frog.tongue": "Tongue lashes",
    "subtitles.wildercord.kit.frog.gulp": "Bog Witch-Frog swallows",
    "subtitles.wildercord.kit.frog.hurt": "Bog Witch-Frog hurts",
    "subtitles.wildercord.kit.frog.death": "Bog Witch-Frog dies",
    "subtitles.wildercord.kit.frog.pop": "Bubble pops",
    "subtitles.wildercord.kit.ooze.ambient": "Mana Ooze burbles",
    "subtitles.wildercord.kit.ooze.absorb": "Mana Ooze drinks a spell",
    "subtitles.wildercord.kit.ooze.grow": "Mana Ooze swells",
    "subtitles.wildercord.kit.ooze.split": "Mana Ooze bursts",
    "subtitles.wildercord.kit.ooze.hurt": "Mana Ooze squelches",
    "subtitles.wildercord.kit.ooze.death": "Mana Ooze dies",
}


def skins():
    """Every texture under textures/entity/, by file name."""
    return {
        "bramblewalker": bramblewalker_texture(),
        "bramblewalker_glow": bramblewalker_glow_texture(),
        "bramblewalker_eyes": bramblewalker_eyes_texture(),
        "gloomstalker": gloomstalker_texture(),
        "gloomstalker_eyes": gloomstalker_eyes_texture(),
        "gloomstalker_glow": gloomstalker_glow_texture(),
        "gloomstalker_flare": gloomstalker_flare_texture(),
        "thunderwing_harpy": harpy_paint(),
        "thunderwing_harpy_glow": harpy_paint(glow=True),
        "thunderwing_harpy_eyes": harpy_eyes_texture(),
        "geode_crawler": geode_crawler_paint(),
        "geode_crawler_glow": geode_crawler_paint(glow=True),
        "bog_witch_frog": bog_witch_frog_texture(),
        "bog_witch_frog_eyes": bog_witch_frog_eyes_texture(),
        "bog_witch_frog_glow": bog_witch_frog_glow_texture(),
        "mana_ooze": mana_ooze_texture(),
        "mana_ooze_glow": mana_ooze_glow_texture(),
        "bog_bubble": bog_bubble_texture(),
        "bog_bubble_glow": bog_bubble_glow_texture(),
    }


def write(g):
    for name, image in skins().items():
        g.save(image, g.ASSETS / f"textures/entity/{name}.png")
    for path in ITEMS:
        g.save(icon(path), g.ASSETS / f"textures/item/{path}.png")
        g.item_model(path, path)
        g.write_json(g.ASSETS / f"items/{path}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{path}"}})
    for kind in MONSTERS:
        egg = f"{kind}_spawn_egg"
        g.save(egg_icon(kind), g.ASSETS / f"textures/item/{egg}.png")
        g.item_model(egg, egg)
        g.write_json(g.ASSETS / f"items/{egg}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{egg}"}})
        g.write_json(g.DATA / f"loot_table/entities/{kind}.json", {"type": "minecraft:entity", "pools": LOOT[kind],
                                                                    "random_sequence": f"wildercord:entities/{kind}"})
    for base, reagent, result in BREWS:
        for container in ("potion", "splash_potion", "lingering_potion"):
            name = f"{container}_{base.split(':')[1]}_{reagent.split(':')[1]}"
            g.brewing(name, f"minecraft:{container}", base, reagent, f"minecraft:{container}", result)


def preview(out_dir):
    images = list(skins().items()) + [(p, icon(p)) for p in ITEMS] + [(k, egg_icon(k)) for k in MONSTERS]
    scale = 4
    cols = 4
    cell = (128 * scale + 16, 64 * scale + 16)
    rows = (len(images) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell[0], rows * cell[1]), (44, 40, 52, 255))
    for i, (name, img) in enumerate(images):
        s = scale if img.width > 16 else scale * 4
        big = img.resize((img.width * s, img.height * s), Image.NEAREST)
        x, y = (i % cols) * cell[0] + 8, (i // cols) * cell[1] + 8
        sheet.paste(big, (x, y), big)
    out = Path(out_dir) / "monster_art.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    return str(out)


if __name__ == "__main__":
    print(preview(Path(__file__).resolve().parent.parent / "build/art-preview"))
