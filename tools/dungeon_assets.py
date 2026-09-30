"""The dimension dungeons' assets: the bosses' skins and glow layers, the trophies' icons, the
altar's models, the loot tables and the worldgen data. Called by generate_assets.py (which owns the
English text: DUNGEON_LANG there); run that, not this.

Skins are painted face by face on each model's box-UV layout, like world_art.py's, but from rules
rather than ASCII: iron plates with seams and rivets, magma in the cracks between them, chain links,
and so on. Glow layers are pale-to-bright on transparency, drawn emissive in game. Every choice is
seeded, so a run always writes the same files.
"""
from __future__ import annotations

import math
import random

from item_art import Canvas, blit, hexc, mix, ramp
from world_art import Sheet, box, fill, noise

# ============================================================== shared painting


def shade(tones, t):
    return tones[max(0, min(len(tones) - 1, t))]


def faces(area_box, *names):
    return [area_box[n] for n in names]


def plates(cv, area, tones, seed, seam=5, rivets=True, base=2, vertical=False):
    """Iron plating: a mottled face split by seams every `seam` pixels, lit from the top, with rivets
    at the seam corners."""
    x0, y0, w, h = area
    for y in range(h):
        for x in range(w):
            along = x if vertical else y
            t = base
            if along % seam == 0:
                t -= 1
            elif along % seam == 1:
                t += 1
            n = noise(x0 + x, y0 + y, seed)
            if n < 0.1:
                t -= 1
            elif n > 0.93:
                t += 1
            if y == 0:
                t += 1
            elif y == h - 1:
                t -= 1
            cv.put(x0 + x, y0 + y, shade(tones, t))
            if rivets and along % seam == 2 and (x if not vertical else y) % 4 == 1:
                cv.put(x0 + x, y0 + y, shade(tones, base + 2))


def crack_paths(area, seed, count, length):
    """Random-walk cracks inside a face: a set of (x, y) in face coordinates."""
    x0, y0, w, h = area
    rng = random.Random(seed)
    out = set()
    for _ in range(count):
        x, y = rng.randrange(w), rng.randrange(h)
        dx, dy = rng.choice(((1, 0), (-1, 0), (0, 1), (0, -1)))
        for _ in range(length):
            out.add((x, y))
            if rng.random() < 0.35:
                dx, dy = rng.choice(((1, 0), (-1, 0), (0, 1), (0, -1), (dx, dy)))
            x = max(0, min(w - 1, x + dx))
            y = max(0, min(h - 1, y + dy))
    return out


def paint_set(cv, area, pixels, colour):
    x0, y0, _, _ = area
    for x, y in pixels:
        cv.put(x0 + x, y0 + y, colour)


def halo(cv, area, pixels, colour):
    """Rings a set of face pixels with `colour`, inside the face, where nothing's painted yet."""
    x0, y0, w, h = area
    for x, y in pixels:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < w and 0 <= ny < h and (nx, ny) not in pixels and cv.get(x0 + nx, y0 + ny) is None:
                cv.put(x0 + nx, y0 + ny, colour)


def disc(cx, cy, r):
    return {(x, y) for x in range(int(cx - r - 1), int(cx + r + 2)) for y in range(int(cy - r - 1), int(cy + r + 2))
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r}


# ============================================================== the Cinder Warden (CinderWardenModel, 128x128)

IRON = ramp("#141012", "#1E1A1C", "#2A2426", "#3A3234", "#4A4042", "#5E5254", "#76686A")
CHAIN = ramp("#26262C", "#3A3A42", "#56565F", "#74747E", "#9A9AA6")
GOLD = ramp("#5A3E10", "#8A6A1E", "#C89A2E", "#E8C25A")
EMBER = {"deep": hexc("#5A1406"), "low": hexc("#8A2A0A"), "mid": hexc("#C8400E"), "hot": hexc("#FF7A1E")}
GLOW_HOT = (255, 190, 90, 255)
GLOW_MID = (255, 120, 40, 230)
GLOW_HALO = (255, 90, 30, 80)

W_TORSO = box(0, 0, 18, 18, 11)
W_HEAD = box(60, 0, 9, 9, 9)
W_PAULDRON = box(60, 18, 9, 6, 10)
W_ARM = box(98, 0, 7, 16, 7)
W_GAUNTLET = box(96, 34, 8, 7, 8)
W_LEG = box(0, 30, 8, 20, 8)
W_BELT = box(32, 34, 17, 5, 10)
W_CHAIN = box(0, 60, 2, 12, 2)

# Where the magma shows, face by face: (area, seed, cracks, length).
W_CRACKS = [
    (W_TORSO["front"], 11, 7, 9), (W_TORSO["back"], 12, 5, 8), (W_TORSO["right"], 13, 3, 7), (W_TORSO["left"], 14, 3, 7),
    (W_ARM["front"], 21, 2, 7), (W_ARM["right"], 22, 2, 6), (W_ARM["left"], 23, 2, 6), (W_ARM["back"], 24, 2, 6),
    (W_LEG["front"], 31, 2, 8), (W_LEG["right"], 32, 2, 6), (W_LEG["left"], 33, 2, 6), (W_LEG["back"], 34, 1, 6),
    (W_PAULDRON["top"], 41, 2, 6), (W_GAUNTLET["front"], 51, 2, 5), (W_HEAD["top"], 61, 1, 5),
]
W_CORE = (9, 7, 3.4)          # on the torso's front: centre x, y and radius of the molten core
# the helm's front: a T-shaped visor slit with two ember eyes
W_VISOR = """
    .........
    .........
    .#######.
    .#e###e#.
    ....#....
    ....#....
    ....#....
    .........
    .........
"""


def cinder_warden_texture():
    cv = Sheet(128, 128)
    for part, seed, seam, vertical in ((W_TORSO, 1, 6, False), (W_HEAD, 2, 4, False), (W_PAULDRON, 3, 3, False),
                                       (W_ARM, 4, 5, False), (W_GAUNTLET, 5, 3, True), (W_LEG, 6, 5, False)):
        for name, area in part.items():
            plates(cv, area, IRON, seed * 10 + len(name), seam=seam, vertical=vertical, base=3 if name == "top" else 2)
    # The belt: dark iron with a gilded buckle and trim.
    for name, area in W_BELT.items():
        plates(cv, area, IRON, 70 + len(name), seam=3, base=1)
        x0, y0, w, h = area
        for x in range(w):
            cv.put(x0 + x, y0, GOLD[1])
            cv.put(x0 + x, y0 + h - 1, GOLD[0])
    bx, by, bw, bh = W_BELT["front"]
    fill(cv, (bx + bw // 2 - 2, by + 1, 5, bh - 2), GOLD[2])
    fill(cv, (bx + bw // 2 - 1, by + 2, 3, bh - 4), GOLD[3])
    # The pauldrons' gilded rims.
    for name in ("front", "right", "left", "back"):
        x0, y0, w, h = W_PAULDRON[name]
        for x in range(w):
            cv.put(x0 + x, y0 + h - 1, GOLD[1] if x % 2 else GOLD[2])
    # Chain links.
    for name, area in W_CHAIN.items():
        x0, y0, w, h = area
        for y in range(h):
            for x in range(w):
                link = (y // 2) % 2
                cv.put(x0 + x, y0 + y, CHAIN[3 if (x + y) % 2 == link else 1] if y % 2 == 0 else CHAIN[2])
    # The magma between the plates: dark red in the skin (the glow layer lights it).
    for area, seed, count, length in W_CRACKS:
        paths = crack_paths(area, seed, count, length)
        paint_set(cv, area, paths, EMBER["mid"])
    tx, ty, _, _ = W_TORSO["front"]
    for (x, y) in disc(W_CORE[0], W_CORE[1], W_CORE[2]):
        cv.put(tx + x, ty + y, EMBER["hot"])
    for (x, y) in disc(W_CORE[0], W_CORE[1], W_CORE[2] + 1.2) - disc(W_CORE[0], W_CORE[1], W_CORE[2]):
        cv.put(tx + x, ty + y, IRON[4])
    # The helm's visor: a black slit.
    hx, hy, _, _ = W_HEAD["front"]
    for y, row in enumerate(line for line in (l.strip() for l in W_VISOR.strip("\n").splitlines()) if line):
        for x, ch in enumerate(row):
            if ch in "#e":
                cv.put(hx + x, hy + y, IRON[0] if ch == "#" else EMBER["low"])
    return cv.image()


def cinder_warden_glow_texture():
    """What burns: the magma in its cracks and its core, bright on transparency."""
    cv = Sheet(128, 128)
    for area, seed, count, length in W_CRACKS:
        paths = crack_paths(area, seed, count, length)
        paint_set(cv, area, paths, GLOW_MID)
        halo(cv, area, paths, GLOW_HALO)
    tx, ty, _, _ = W_TORSO["front"]
    core = disc(W_CORE[0], W_CORE[1], W_CORE[2])
    for (x, y) in core:
        d = math.hypot(x - W_CORE[0], y - W_CORE[1]) / W_CORE[2]
        cv.put(tx + x, ty + y, (255, int(250 - 110 * d), int(200 - 170 * d), 255))
    halo(cv, W_TORSO["front"], core, (255, 120, 40, 120))
    return cv.image()


def cinder_warden_eyes_texture():
    cv = Sheet(128, 128)
    hx, hy, _, _ = W_HEAD["front"]
    for y, row in enumerate(line for line in (l.strip() for l in W_VISOR.strip("\n").splitlines()) if line):
        for x, ch in enumerate(row):
            if ch == "e":
                cv.put(hx + x, hy + y, (255, 236, 150, 255))
            elif ch == "#":
                cv.put(hx + x, hy + y, (255, 110, 30, 70))
    return cv.image()


# ============================================================== the Star-Eater (StarEaterModel, 64x64)

VOID = ramp("#07040E", "#0E0819", "#170D2A", "#22133C", "#321C56", "#472878", "#6440A0")
CRYSTAL = ramp("#3C2462", "#6A48A8", "#9C7CDA", "#C8B4F2", "#EDE4FF")
S_CORE = box(0, 0, 12, 12, 12)
S_SHARD = box(48, 0, 3, 6, 3)
S_SPIKE = box(48, 10, 2, 6, 2)
S_TENDRIL = box(0, 24, 2, 8, 2)
S_TIP = box(8, 24, 2, 7, 2)
S_EYE = (5.5, 5.5, 4.6)        # on the core's front: centre x, y and radius
S_VEINS = [(S_CORE[n], 100 + i, 3, 8) for i, n in enumerate(("front", "back", "right", "left", "top", "bottom"))]


def void_face(cv, area, seed, base=2):
    """Void: a deep, uneven dark with far stars scattered through it."""
    x0, y0, w, h = area
    for y in range(h):
        for x in range(w):
            n = noise(x0 + x, y0 + y, seed)
            t = base + (1 if n > 0.8 else -1 if n < 0.2 else 0)
            if y == 0:
                t += 1
            cv.put(x0 + x, y0 + y, shade(VOID, t))
            if n > 0.975:
                cv.put(x0 + x, y0 + y, hexc("#E8D8FF") if n > 0.99 else hexc("#9C7CDA"))


def star_eater_texture():
    cv = Sheet(64, 64)
    for name, area in S_CORE.items():
        void_face(cv, area, 90 + len(name))
    for area, seed, count, length in S_VEINS:
        paint_set(cv, area, crack_paths(area, seed, count, length), VOID[5])
    # The eye: a violet iris round a black slit, on the front.
    ex, ey, _, _ = S_CORE["front"]
    cx, cy, r = S_EYE
    for (x, y) in disc(cx, cy, r):
        d = math.hypot(x - cx, y - cy) / r
        cv.put(ex + x, ey + y, mix(hexc("#E0C8FF"), hexc("#5A2E9A"), d))
    for (x, y) in disc(cx, cy, r + 1.0) - disc(cx, cy, r):
        if 0 <= x < 12 and 0 <= y < 12:
            cv.put(ex + x, ey + y, VOID[0])
    for y in range(int(cy - r + 1), int(cy + r)):
        cv.put(ex + int(cx), ey + y, VOID[0])
    cv.put(ex + int(cx) - 1, ey + int(cy) - 2, hexc("#FFFFFF"))
    # Shards: pale crystal, lit from the top.
    for name, area in S_SHARD.items():
        x0, y0, w, h = area
        for y in range(h):
            for x in range(w):
                t = 3 - (y * 3) // max(1, h) + (1 if x == 0 else 0)
                cv.put(x0 + x, y0 + y, shade(CRYSTAL, t))
    for name, area in S_SPIKE.items():
        x0, y0, w, h = area
        for y in range(h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, shade(VOID, 5 - y if y < 3 else 3))
    for part in (S_TENDRIL, S_TIP):
        for name, area in part.items():
            void_face(cv, area, 120 + len(name), base=3)
            x0, y0, w, h = area
            for y in range(0, h, 3):
                for x in range(w):
                    cv.put(x0 + x, y0 + y, VOID[5])
    return cv.image()


def star_eater_eye_texture():
    """What always glows: its eye and the veins across it."""
    cv = Sheet(64, 64)
    for area, seed, count, length in S_VEINS:
        paths = crack_paths(area, seed, count, length)
        paint_set(cv, area, paths, (190, 130, 255, 210))
        halo(cv, area, paths, (150, 90, 240, 60))
    ex, ey, _, _ = S_CORE["front"]
    cx, cy, r = S_EYE
    iris = disc(cx, cy, r)
    for (x, y) in iris:
        d = math.hypot(x - cx, y - cy) / r
        if abs(x - int(cx)) >= 1 or d > 0.95:
            cv.put(ex + x, ey + y, (235, int(215 - 90 * d), 255, int(255 - 80 * d)))
    cv.put(ex + int(cx) - 1, ey + int(cy) - 2, (255, 255, 255, 255))
    for part in (S_TIP,):
        x0, y0, w, h = part["front"]
        for x in range(w):
            cv.put(x0 + x, y0 + h - 1, (220, 180, 255, 230))
            cv.put(x0 + x, y0 + h - 2, (200, 150, 255, 120))
    return cv.image()


def star_eater_shards_texture():
    """The edges of its shards, bright while its shield is up (and the tips of its crown)."""
    cv = Sheet(64, 64)
    for name, area in S_SHARD.items():
        x0, y0, w, h = area
        for y in range(h):
            for x in range(w):
                edge = x in (0, w - 1) or y in (0, h - 1)
                cv.put(x0 + x, y0 + y, (240, 230, 255, 255) if edge else (200, 170, 255, 110))
    for name in ("front", "right", "left", "back", "top"):
        x0, y0, w, h = S_SPIKE[name]
        for x in range(w):
            cv.put(x0 + x, y0, (230, 210, 255, 200))
    return cv.image()


# ============================================================== the Tide Scribe (TideScribeModel, 128x64)

FLESH = ramp("#1E3A36", "#2E5048", "#3E6E64", "#56887A", "#6FA290")
ROBE_DEEP = ramp("#0E1A2C", "#142440", "#1A2E4A", "#243E62", "#2E5078", "#3C6490")
KELP = ramp("#1E3A14", "#2E5A1E", "#44782A")
INK = ramp("#06080E", "#0C1018", "#141C2A", "#1E2A3E", "#2A3A54")
PARCHMENT = ramp("#8A8266", "#A8A084", "#C8C0A0", "#DCD6BA")
T_HEAD = box(0, 0, 8, 8, 8)
T_HOOD = box(32, 0, 9, 9, 9)
T_TORSO = box(0, 18, 8, 12, 4)
T_COLLAR = box(24, 18, 10, 3, 6)
T_ARM = box(56, 18, 4, 12, 4)
T_TENDRIL = box(0, 36, 3, 7, 3)
T_MID = box(12, 36, 2, 7, 2)
T_TIP = box(20, 36, 1, 6, 1)
T_SCROLL = box(32, 36, 12, 8, 0)
T_ROD = box(58, 36, 1, 10, 1)
T_QUILL = box(64, 36, 1, 7, 1)
# the drowned face: hollow cheeks, a slack mouth, two glowing eyes ('e')
T_FACE = """
    aaaaaaaa
    abbbbbba
    bccbbccb
    ceeccee.
    bccbbccb
    bbbccbbb
    bcaaaacb
    bbccccbb
"""
T_SCRIPT = [(1, 1, 7), (1, 3, 9), (2, 5, 6), (1, 6, 8)]     # the scroll's lines: (x, y, length)


def cloth(cv, area, tones, seed, base=2, period=3):
    """Sodden cloth: vertical folds and a little mottling, darker toward the hem."""
    x0, y0, w, h = area
    for y in range(h):
        for x in range(w):
            k = (x + seed) % period
            t = base + (1 if k == 0 else -1 if k == period - 1 else 0)
            if y >= h - max(1, h // 5):
                t -= 1
            n = noise(x0 + x, y0 + y, seed)
            if n < 0.12:
                t -= 1
            elif n > 0.92:
                t += 1
            cv.put(x0 + x, y0 + y, shade(tones, t))


def tide_scribe_texture():
    cv = Sheet(128, 64)
    face_pal = {"a": FLESH[1], "b": FLESH[2], "c": FLESH[3], "e": hexc("#9AF0E8")}
    for name, area in T_HEAD.items():
        cloth(cv, area, FLESH, 200 + len(name), base=2, period=5)
    hx, hy, _, _ = T_HEAD["front"]
    for y, row in enumerate(line for line in (l.strip() for l in T_FACE.strip("\n").splitlines()) if line):
        for x, ch in enumerate(row):
            if ch in face_pal:
                cv.put(hx + x, hy + y, face_pal[ch])
    # The hood: open at the front (its front face left clear, so the face shows), kelp trailing from it.
    for name, area in T_HOOD.items():
        if name != "front":
            cloth(cv, area, ROBE_DEEP, 210 + len(name))
    fx, fy, fw, fh = T_HOOD["front"]
    for x in range(fw):
        cv.put(fx + x, fy, ROBE_DEEP[3])
        cv.put(fx + x, fy + 1, ROBE_DEEP[2])
    for y in range(fh):
        cv.put(fx, fy + y, ROBE_DEEP[2])
        cv.put(fx + fw - 1, fy + y, ROBE_DEEP[2])
    for part, seed in ((T_TORSO, 220), (T_COLLAR, 230), (T_ARM, 240)):
        for name, area in part.items():
            cloth(cv, area, ROBE_DEEP, seed + len(name))
    # Kelp strands down the robe, and barnacles.
    for area in (T_TORSO["front"], T_TORSO["back"], T_ARM["right"], T_ARM["left"]):
        x0, y0, w, h = area
        for x in range(w):
            if noise(x0 + x, y0, 7) < 0.3:
                length = 2 + int(noise(x0 + x, y0, 8) * (h - 2))
                for y in range(length):
                    cv.put(x0 + x, y0 + y, KELP[(y + x) % 3])
        for y in range(h):
            for x in range(w):
                if noise(x0 + x, y0 + y, 9) > 0.97:
                    cv.put(x0 + x, y0 + y, hexc("#C8C0B0"))
    for part, seed in ((T_TENDRIL, 250), (T_MID, 260), (T_TIP, 270)):
        for name, area in part.items():
            cloth(cv, area, INK, seed + len(name), base=2, period=4)
    for name, area in T_SCROLL.items():
        x0, y0, w, h = area
        for y in range(h):
            for x in range(w):
                t = 2 + (1 if noise(x0 + x, y0 + y, 280) > 0.8 else -1 if noise(x0 + x, y0 + y, 281) < 0.15 else 0)
                cv.put(x0 + x, y0 + y, shade(PARCHMENT, t))
        for lx, ly, length in T_SCRIPT:
            for x in range(length):
                if (x * 7 + ly) % 5 != 3 and lx + x < w:
                    cv.put(x0 + lx + x, y0 + ly, hexc("#1E3A6A"))
    for name, area in T_ROD.items():
        fill(cv, area, hexc("#5A4026"))
    for name, area in T_QUILL.items():
        x0, y0, w, h = area
        for y in range(h):
            cv.put(x0, y0 + y, hexc("#E8E4D8") if y < h - 2 else hexc("#2A3A54"))
    return cv.image()


def tide_scribe_glow_texture():
    """What always glows: its eyes, and the lights along its tendrils."""
    cv = Sheet(128, 64)
    hx, hy, _, _ = T_HEAD["front"]
    for y, row in enumerate(line for line in (l.strip() for l in T_FACE.strip("\n").splitlines()) if line):
        for x, ch in enumerate(row):
            if ch == "e":
                cv.put(hx + x, hy + y, (170, 250, 240, 255))
                for dx, dy in ((0, -1), (0, 1)):
                    if cv.get(hx + x + dx, hy + y + dy) is None:
                        cv.put(hx + x + dx, hy + y + dy, (120, 220, 230, 70))
    for part, stride in ((T_TENDRIL, 3), (T_MID, 3), (T_TIP, 2)):
        for name in ("front", "back", "right", "left"):
            x0, y0, w, h = part[name]
            for y in range(1, h, stride):
                cv.put(x0 + w // 2, y0 + y, (110, 230, 255, 220))
    x0, y0, w, h = T_TIP["front"]
    cv.put(x0, y0 + h - 1, (190, 255, 250, 255))
    return cv.image()


def tide_scribe_script_texture():
    """The writing on its scroll, and the quill's nib: dim at rest, blazing as it writes a spell."""
    cv = Sheet(128, 64)
    for name, area in T_SCROLL.items():
        x0, y0, w, h = area
        for lx, ly, length in T_SCRIPT:
            for x in range(length):
                if (x * 7 + ly) % 5 != 3 and lx + x < w:
                    cv.put(x0 + lx + x, y0 + ly, (120, 200, 255, 255))
                    if cv.get(x0 + lx + x, y0 + ly + 1) is None and ly + 1 < h:
                        cv.put(x0 + lx + x, y0 + ly + 1, (80, 160, 255, 60))
    for name, area in T_QUILL.items():
        x0, y0, w, h = area
        cv.put(x0, y0 + h - 1, (150, 230, 255, 255))
    return cv.image()


# ============================================================== trophies (16x16)

CINDER_HEART = """
    ................
    ......CC........
    .....C..C.......
    ....C....C......
    ...ooo..ooo.....
    ..oMMMooMMMo....
    .oMHHMMMMmmMo...
    .oMHWHMMmmmMo...
    .oMHHMMMMmmMo...
    ..oMMMMMMmmo....
    ...oMMmMmmo.....
    ....oMMmmo......
    .....oMmo.......
    ......oo........
    ................
    ................
"""
CINDER_HEART_PAL = {"o": hexc("#1E1A1C"), "M": hexc("#C8400E"), "m": hexc("#8A2A0A"), "H": hexc("#FF9A3A"),
                    "W": hexc("#FFE6A0"), "C": hexc("#74747E")}


ASTRAL_LENS = """
    ................
    .....gGGGg......
    ...gGpppppGg....
    ..gppvvvvvppg...
    ..Gpvv*vvvvpG...
    .gpvv***vvvvpg..
    .Gpvvv*vvvsvpG..
    .Gpvvvvvvv*vpG..
    .GpvsvvvvvvvpG..
    .gpvvvvvvsvvpg..
    ..Gpvvvvvvvpg...
    ..gGpvvvvvpGg...
    ....gGpppGg.....
    ......gGg.......
    ................
    ................
"""
ASTRAL_LENS_PAL = {"g": hexc("#8A6A1E"), "G": hexc("#E8C25A"), "p": hexc("#6A48A8"), "v": hexc("#22133C"),
                   "*": hexc("#FFFFFF"), "s": hexc("#C8B4F2")}


DROWNED_QUILL = """
    ................
    ............WW..
    ...........WwW..
    ..........WwwW..
    .........WwwW...
    ........WwwW....
    .......WwwW.....
    ......WwwW......
    .....WwwW.......
    .....wwW........
    ....kbk.........
    ....bk..........
    ...ik...........
    ..ii............
    .iII............
    ..I.............
"""
DROWNED_QUILL_PAL = {"W": hexc("#E8E4D8"), "w": hexc("#B8C8C8"), "k": hexc("#2E5A1E"), "b": hexc("#44782A"),
                     "i": hexc("#1E3A6A"), "I": hexc("#9AF0E8")}

ROOTBOUND_RELIC = """
    ...............
    ......bb.......
    ....bbBBbb.....
    ...bBG..GBb....
    ..bBG..l..GBb...
    .bBG..l*l..GBb..
    .bBG.l*H*l.GBb..
    .bBG..*H*..GBb..
    .bBG...H...GBb..
    ..bBG..l..GBb...
    ...bBG..GBb....
    ....bbBBbb.....
    ...bB.bbb.Bb...
    ..bB...b...Bb...
    .bb....b....bb.
    ...............
"""
ROOTBOUND_RELIC_PAL = {"b": hexc("#382818"), "B": hexc("#8A5E31"), "g": hexc("#254A2A"),
                        "G": hexc("#447D36"), "L": hexc("#9CCB56"), "l": hexc("#D0E481"),
                        "*": hexc("#F3F5B7"), "H": hexc("#FFFBE2")}

STORMGLASS_RELIC = """
    ................
    .....yyYYyy.....
    ...yyAaBBaAyy...
    ..yAaB....BAay..
    .yAaB..C...BAay.
    .yAaB.CWC..BAay.
    .yAaB..WC..BAay.
    .yAaB...WC.BAay.
    .yAaB.CWC..BAay.
    ..yAaB....BAay..
    ...yyAaBBaAyy...
    .....yyYYyy.....
    ...yY..yy..Yy...
    ..yy........yy..
    ................
    ................
"""
STORMGLASS_RELIC_PAL = {"a": hexc("#283256"), "A": hexc("#586EA6"), "B": hexc("#A4BBE7"),
                         "c": hexc("#52B8DE"), "C": hexc("#C3F1FF"), "W": hexc("#FFFFFF"),
                         "y": hexc("#704C1C"), "Y": hexc("#F3D77A")}


def trophy_icon(text, pal):
    cv = Canvas(16)
    blit(cv, text, pal)
    return cv.image()


# ============================================================== writing it all out


def elements_runes(runes, elements, tiers, innate):
    return [r["path"] for r in runes if r["element"] in elements and r["tier"] in tiers and r["path"] not in innate and r["family"] == "effect"]


# The dungeons: their structure data, where they grow, and what their chests and bosses hold.
DUNGEONS = {
    "ember_sanctum": {
        "boss": "cinder_warden", "trophy": "cinder_heart", "elements": ("fire", "earth"), "name": "the Ember Sanctum",
        # Built after the basalt deltas' columns and lava sheets (surface_structures), which would otherwise grow inside
        # its halls, as a fortress is; and kept clear of fortresses and bastions, which would carve through it.
        "step": "underground_decoration", "biomes": ["minecraft:nether_wastes", "minecraft:basalt_deltas", "minecraft:crimson_forest"],
        "spacing": 36, "separation": 12, "salt": 20260928, "exclusion": ("minecraft:nether_complexes", 5),
        "spawns": [("minecraft:wither_skeleton", 3), ("minecraft:blaze", 1), ("minecraft:magma_cube", 1)],
        "extras": [("minecraft:blaze_powder", 4, 2, 6), ("minecraft:magma_cream", 3, 1, 4), ("minecraft:gold_ingot", 3, 2, 6),
                   ("minecraft:netherite_scrap", 1, 1, 1)],
    },
    "astral_observatory": {
        "boss": "star_eater", "trophy": "astral_lens", "elements": ("void", "arcane"), "name": "the Astral Observatory",
        "step": "surface_structures", "biomes": ["minecraft:end_highlands", "minecraft:end_midlands"],
        "spacing": 32, "separation": 10, "salt": 20260929,
        "spawns": [],
        "extras": [("minecraft:ender_pearl", 4, 1, 4), ("minecraft:chorus_fruit", 3, 2, 6), ("minecraft:amethyst_shard", 3, 2, 6),
                   ("minecraft:shulker_shell", 1, 1, 1)],
    },
    "drowned_scriptorium": {
        "boss": "tide_scribe", "trophy": "drowned_quill", "elements": ("storm", "frost"), "name": "the Drowned Scriptorium",
        "step": "surface_structures",
        "biomes": ["minecraft:deep_ocean", "minecraft:deep_cold_ocean", "minecraft:deep_lukewarm_ocean", "minecraft:deep_frozen_ocean"],
        "spacing": 40, "separation": 14, "salt": 20260930,
        "spawns": [],
        "extras": [("minecraft:prismarine_crystals", 4, 2, 6), ("minecraft:nautilus_shell", 2, 1, 2), ("minecraft:ink_sac", 3, 2, 5),
                   ("minecraft:heart_of_the_sea", 1, 1, 1)],
    },
    "rootbound_maze": {
        "elements": ("life", "earth"), "name": "the Rootbound Maze", "relic": "wildercord:rootbound_relic",
        "step": "surface_structures", "biomes": ["minecraft:swamp", "minecraft:mangrove_swamp"],
        "spacing": 28, "separation": 9, "salt": 20261001, "spawns": [],
        "extras": [("minecraft:moss_block", 4, 2, 7), ("minecraft:vine", 3, 2, 6),
                   ("minecraft:amethyst_block", 2, 1, 2), ("minecraft:golden_apple", 1, 1, 1)],
    },
    "storm_spire": {
        "elements": ("storm", "wind"), "name": "the Storm Spire", "relic": "wildercord:stormglass_relic",
        "step": "surface_structures", "biomes": ["minecraft:jagged_peaks", "minecraft:frozen_peaks", "minecraft:stony_peaks"],
        "spacing": 32, "separation": 10, "salt": 20261002, "spawns": [],
        "extras": [("minecraft:lightning_rod", 4, 1, 3), ("minecraft:amethyst_shard", 4, 2, 8),
                   ("minecraft:amethyst_block", 2, 1, 2), ("minecraft:emerald", 2, 1, 3)],
    },
}


def main(g, runes):
    """Writes everything. `g` is generate_assets (for its writers and the rune list's helpers)."""
    tex = g.ASSETS / "textures"
    data = g.DATA
    # ---- skins
    g.save(cinder_warden_texture(), tex / "entity/cinder_warden.png")
    g.save(cinder_warden_glow_texture(), tex / "entity/cinder_warden_glow.png")
    g.save(cinder_warden_eyes_texture(), tex / "entity/cinder_warden_eyes.png")
    g.save(star_eater_texture(), tex / "entity/star_eater.png")
    g.save(star_eater_eye_texture(), tex / "entity/star_eater_eye.png")
    g.save(star_eater_shards_texture(), tex / "entity/star_eater_shards.png")
    g.save(tide_scribe_texture(), tex / "entity/tide_scribe.png")
    g.save(tide_scribe_glow_texture(), tex / "entity/tide_scribe_glow.png")
    g.save(tide_scribe_script_texture(), tex / "entity/tide_scribe_script.png")
    # ---- trophies
    icons = {"cinder_heart": trophy_icon(CINDER_HEART, CINDER_HEART_PAL), "astral_lens": trophy_icon(ASTRAL_LENS, ASTRAL_LENS_PAL),
             "drowned_quill": trophy_icon(DROWNED_QUILL, DROWNED_QUILL_PAL),
             "rootbound_relic": trophy_icon(ROOTBOUND_RELIC, ROOTBOUND_RELIC_PAL),
             "stormglass_relic": trophy_icon(STORMGLASS_RELIC, STORMGLASS_RELIC_PAL)}
    for name, image in icons.items():
        g.save(image, tex / f"item/{name}.png")
        g.item_model(name, name)
        g.write_json(g.ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{name}"}})
    # ---- the altar: one model per kind, borrowing the dungeons' own stone
    kinds = {"cinder": ("minecraft:block/polished_blackstone_bricks", "minecraft:block/magma", "minecraft:block/polished_blackstone"),
             "astral": ("minecraft:block/purpur_pillar_side", "minecraft:block/end_portal_frame_top", "minecraft:block/end_stone_bricks"),
             "tide": ("minecraft:block/prismarine_bricks", "minecraft:block/sea_lantern", "minecraft:block/dark_prismarine")}
    face = lambda t, uv=(2, 2, 14, 14): {"texture": f"#{t}", "uv": list(uv)}
    variants = {}
    for kind, (side, top, bottom) in kinds.items():
        g.write_json(g.ASSETS / f"models/block/dungeon_altar_{kind}.json", {
            "parent": "minecraft:block/block",
            "textures": {"particle": side, "top": top, "side": side, "bottom": bottom},
            "elements": [{"from": [2, 0, 2], "to": [14, 14, 14], "faces": {
                "down": face("bottom"), "up": face("top"), "north": face("side", (2, 2, 14, 16)), "south": face("side", (2, 2, 14, 16)),
                "west": face("side", (2, 2, 14, 16)), "east": face("side", (2, 2, 14, 16))}}]})
        for awake in ("false", "true"):
            variants[f"awake={awake},kind={kind}"] = {"model": f"wildercord:block/dungeon_altar_{kind}"}
    g.write_json(g.ASSETS / "blockstates/dungeon_altar.json", {"variants": variants})
    g.write_json(g.ASSETS / "items/dungeon_altar.json", {"model": {"type": "minecraft:model", "model": "wildercord:block/dungeon_altar_cinder"}})

    world = g.found_only()
    sources = {sid: paths for sid, _, paths in g.rune_sources()}
    tier = {r["path"]: r["tier"] for r in runes}
    fourth = [r["path"] for r in runes if r["tier"] == 4 and r["path"] not in g.INNATE and r["path"] not in world and r["path"] not in g.FUSED]

    def found_pool(sid, odds=None):
        """This place's own runes (RuneSources.java), commoner by tier; with {@code odds}, only sometimes."""
        pool = {"rolls": 1, "entries": [g.rune_entry(p, {1: 8, 2: 5, 3: 2}.get(tier[p], 1)) for p in sources[sid]]}
        return dict(pool, conditions=[{"condition": "minecraft:random_chance", "chance": odds}]) if odds else pool

    for key, d in DUNGEONS.items():
        # Common runes of its elements only: never an innate, fused or found-only one.
        unfound = set(g.INNATE) | world | set(g.FUSED)
        second_third = elements_runes(runes, d["elements"], (2, 3), unfound)
        # ---- the guarded hall's chest: runes of its elements, and what a keeper of the place would hoard
        g.write_json(data / f"loot_table/chests/{key}_hall.json", {"type": "minecraft:chest", "pools": [
            {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 3}, "entries": [g.rune_entry(p, 3) for p in second_third]},
            {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 3}, "entries": [
                g.item_entry("wildercord:torn_page", 5), g.item_entry("wildercord:mana_crystal", 3), g.item_entry("wildercord:blank_rune", 4, 2, 5),
                g.item_entry("minecraft:lapis_lazuli", 4, 3, 9)] + [g.item_entry(i, w, lo, hi) for i, w, lo, hi in d["extras"]]},
        ]})
        # ---- the vault: a Tier IV rune, runes of its elements, and treasure
        g.write_json(data / f"loot_table/chests/{key}_vault.json", {"type": "minecraft:chest", "pools": [
            *([{"rolls": 1, "entries": [g.item_entry(d["relic"], 1)]}] if "relic" in d else []),
            {"rolls": 1, "entries": [g.rune_entry(p, 1) for p in fourth]} if "boss" in d else
            {"rolls": 1, "entries": [g.rune_entry(p, 1) for p in second_third]},
            # The dungeon's own runes, found nowhere else: one, and a second a third of the time.
            *([found_pool(key), found_pool(key, 0.35)] if key in sources else []),
            {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "entries": [
                g.item_entry("wildercord:mana_crystal", 4, 1, 2), g.item_entry("wildercord:torn_page", 4), g.item_entry("minecraft:diamond", 2, 1, 3),
                g.item_entry("minecraft:gold_ingot", 3, 2, 6), g.item_entry("minecraft:echo_shard", 1)] +
                [g.item_entry(i, w, lo, hi) for i, w, lo, hi in d["extras"]]},
        ]})
        # ---- the boss (its Tier IV rune, one its killer doesn't know, is dropped in code)
        if "boss" in d:
            g.write_json(data / f"loot_table/entities/{d['boss']}.json", {"type": "minecraft:entity", "pools": [
            {"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"wildercord:{d['trophy']}"}]},
            {"rolls": 1, "entries": [g.item_entry("wildercord:mana_crystal", 1, 3, 3)]},
            {"rolls": 1, "entries": [g.item_entry("wildercord:torn_page", 1)]},
            {"rolls": 1, "entries": [g.rune_entry(p, 1) for p in second_third]},
            # The boss's own rune, found nowhere else, and half the time one of its dungeon's.
            found_pool(d["boss"]),
            found_pool(key, 0.5),
            ]})
        # ---- where it grows
        g.write_json(data / f"worldgen/structure/{key}.json", {
            "type": "wildercord:dungeon", "dungeon": key, "biomes": f"#wildercord:has_structure/{key}",
            "spawn_overrides": {"monster": {"bounding_box": "piece", "spawns": [
                {"type": t, "weight": w, "count": 1} for t, w in d["spawns"]]}} if d["spawns"] else {},
            "step": d["step"], "terrain_adaptation": "none"})
        placement = {"type": "minecraft:random_spread", "salt": d["salt"], "separation": d["separation"], "spacing": d["spacing"]}
        if "exclusion" in d:
            placement["exclusion_zone"] = {"other_set": d["exclusion"][0], "chunk_count": d["exclusion"][1]}
        g.write_json(data / f"worldgen/structure_set/{key}.json", {
            "placement": placement, "structures": [{"structure": f"wildercord:{key}", "weight": 1}]})
        g.write_json(data / f"tags/worldgen/biome/has_structure/{key}.json", {"values": d["biomes"]})
    g.write_json(data / "tags/worldgen/structure/dungeon.json", {"values": [f"wildercord:{k}" for k in DUNGEONS]})


def sources(runes, excluded):
    """Rune path -> the dungeons whose chests hold it, for the tooltips' 'Found:' line ({@code excluded}: runes the chests never hold)."""
    found = {}
    for key, d in DUNGEONS.items():
        name = d["name"][4:] if d["name"].startswith("the ") else d["name"]
        for path in elements_runes(runes, d["elements"], (2, 3), excluded):
            found.setdefault(path, []).append(name)
    return found
