"""The world of aura: the wandering duelists' skins (one per breathing method) and the fallen knight's, their glow layers, the
manual pages, the Aura Shard, the three aura-forged weapons and the Breath Sash, the spawn eggs, the recipes (smithing
forgings, pages bound into a manual, the sash), the knight's loot table, the structures knights haunt, and the English text.
Called by generate_assets.py (write(g), and LANG merged into en_us.json).

Skins are painted face by face on the box-UV layouts in the models' javadoc (client/auraworld/), from materials: woven cloth
with a trim in the method's colours, wrapped leather, skin, lacquered wood, old plate with tarnish and rust, a tattered
tabard. Glow layers hold only what glows, white, for the renderer to tint with the aura's colour. Icons are 16x16, lit from the
top left with a one-pixel outline like the rest of the mod's. Every choice is seeded, so a run always writes the same files.

    python tools/aura_world_art.py      renders a review sheet into build/art-preview/aura_world_art.png
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
from monster_art import LIGHT, cells, faces, shade, smooth  # noqa: E402
from aura_art import METHODS, EMBLEMS, rgb  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent


def tone(c, k):
    return tuple(max(0, min(255, int(round(v * k)))) for v in c)


# ============================================================== shared materials

def cloth(cv, area, tones, seed, face, base=2, weave=True):
    """Woven cloth: a fine weave (alternate pixels a step apart), soft folds running down the face, a little wear."""
    for x, y, px, py in cells(area):
        t = base + LIGHT[face]
        fold = smooth(px * 0.7, py * 0.25, seed, 2.4)
        if fold > 0.68:
            t += 1
        elif fold < 0.25:
            t -= 1
        if weave and (px + py) % 2 == 0 and noise(px, py, seed) > 0.55:
            t -= 1
        cv.put(px, py, shade(tones, t))


def plate(cv, area, tones, seed, face, base=3, rust=None, rust_amount=0.0):
    """Old plate: a smooth sheen lit along the top, a darker lower edge, scratches, tarnish in patches, and rust where it's worn."""
    x0, y0, w, h = area
    for x, y, px, py in cells(area):
        t = base + LIGHT[face]
        if y == 0 and face not in ("top", "bottom"):
            t += 1
        if y == h - 1 and face not in ("top", "bottom"):
            t -= 1
        if smooth(px, py, seed, 2.5) > 0.72:
            t -= 1
        if noise(px, py, seed + 3) > 0.93:
            t += 1
        c = shade(tones, t)
        if rust is not None and smooth(px, py, seed + 17, 2.0) < rust_amount:
            c = mix(c, rust, 0.6)
        cv.put(px, py, c)


def leather(cv, area, tones, seed, face, base=2):
    for x, y, px, py in cells(area):
        t = base + LIGHT[face] + (1 if smooth(px, py, seed, 1.8) > 0.7 else 0) - (1 if noise(px, py, seed) > 0.9 else 0)
        cv.put(px, py, shade(tones, t))


def wraps(cv, area, tones, seed, face, base=2):
    """Cloth wrapped round and round: diagonal bands, each lit at its top edge."""
    for x, y, px, py in cells(area):
        band = (y * 2 + x) % 4
        t = base + LIGHT[face] + (1 if band == 0 else -1 if band == 3 else 0)
        cv.put(px, py, shade(tones, t))


def trim(cv, area, colour, edges, width=1):
    """A band of {colour} along the named edges of a face ('top', 'bottom', 'left', 'right')."""
    x0, y0, w, h = area
    for x, y, px, py in cells(area):
        if ("top" in edges and y < width) or ("bottom" in edges and y >= h - width) or ("left" in edges and x < width) \
                or ("right" in edges and x >= w - width):
            cv.put(px, py, colour)


# ============================================================== the duelist (DuelistModel, 64x64)

D_HEAD = box(0, 0, 8, 8, 8)
D_HOOD = box(32, 0, 8, 8, 8)
D_BODY = box(16, 16, 8, 12, 4)
D_ARM = box(40, 16, 4, 12, 4)
D_LEG = box(0, 16, 4, 12, 4)
D_CLOAK = box(0, 32, 10, 17, 1)
D_MANTLE = box(22, 32, 10, 4, 6)
D_SCABBARD = box(54, 32, 1, 12, 2)
D_GRIP = box(60, 32, 1, 4, 1)
D_GUARD = box(54, 46, 1, 1, 3)
D_TASSEL = box(22, 42, 2, 5, 0)

SKIN = ramp("#5A3A28", "#7A5038", "#9A6A4A", "#B88460", "#D09E78")
HAIR = ramp("#141010", "#201816", "#2E2420", "#3E3028")
CLOTH_DARK = ramp("#16141A", "#201E26", "#2C2A34", "#3A3844", "#4A4856")
WRAP = ramp("#6E6658", "#8A8270", "#A8A08A", "#C6BEA6")
WOOD = ramp("#140C0A", "#22140E", "#341E14", "#4A2C1C", "#5E3A26")
STEEL = ramp("#2A2E36", "#3E444E", "#565E6A", "#727C8A", "#949EAC", "#BCC4CE")
BRASS = ramp("#4A3410", "#6E5018", "#987024", "#C29A3A", "#E8C468")

# The face under the hood: brows, eyes and a cloth over the mouth in the method's colour (M), skin (s), shadow (d).
D_FACE = """
    hhhhhhhh
    hhhhhhhh
    hssssssh
    hbbssbbh
    hewsswed
    dssssssd
    dMMMMMMd
    dMMMMMMd
"""


def cloak_tones(color):
    """A traveller's cloak, its dye a muted, dusty version of the method's colour."""
    c = rgb(color)
    dusty = mix(c, (92, 84, 76), 0.62)
    return [tone(dusty, k) for k in (0.42, 0.58, 0.74, 0.9, 1.04, 1.16)]


def duelist_texture(method_id, color, highlight):
    cv = Sheet(64, 64)
    c = rgb(color)
    hi = rgb(highlight)
    cloak = cloak_tones(color)
    trim_c = mix(c, (255, 255, 255), 0.1)
    trim_dark = tone(c, 0.62)
    seed = sum(ord(ch) for ch in method_id)
    # The head: hair, and the face.
    for name, area in faces(D_HEAD):
        if name == "front":
            continue
        for x, y, px, py in cells(area):
            t = 1 + LIGHT[name] + (1 if smooth(px, py, seed, 1.5) > 0.7 else 0)
            cv.put(px, py, shade(HAIR, t))
    fx, fy, fw, fh = D_HEAD["front"]
    pal = {"h": shade(HAIR, 1), "s": shade(SKIN, 3), "d": shade(SKIN, 1), "b": shade(HAIR, 0), "e": (24, 20, 22), "w": (226, 222, 214),
           "M": trim_dark}
    for y, row in enumerate(r.strip() for r in D_FACE.strip("\n").splitlines()):
        for x, ch in enumerate(row):
            if ch in pal:
                col = pal[ch]
                if ch == "M" and (x + y) % 3 == 0:
                    col = tone(trim_dark, 1.15)
                cv.put(fx + x, fy + y, col)
    # The hood: the cloak's cloth, its face open, a trim round the opening.
    for name, area in faces(D_HOOD):
        cloth(cv, area, cloak, seed + 1 + len(name), name, base=2)
    hx, hy, hw, hh = D_HOOD["front"]
    for y in range(hh):
        for x in range(hw):
            if 1 <= x <= 6 and y >= 2:
                cv.put(hx + x, hy + y, None)
            elif (x in (0, 7) and y >= 1) or (y == 1 and 1 <= x <= 6):
                cv.put(hx + x, hy + y, trim_c if (x + y) % 4 else tone(trim_c, 1.1))
    # The body: a dark tunic, crossed lapels trimmed, a sash at the waist with the emblem.
    for name, area in faces(D_BODY):
        cloth(cv, area, CLOTH_DARK, seed + 5 + len(name), name, base=2)
    bx, by, bw, bh = D_BODY["front"]
    for y in range(bh):
        for x in range(bw):
            if y < 7 and (x == 3 - min(3, y // 2) or x == 4 + min(3, y // 2)):
                cv.put(bx + x, by + y, mix(tone(c, 0.8), (40, 36, 44), 0.35))
            if 7 <= y <= 8:
                cv.put(bx + x, by + y, tone(c, 0.92 if y == 7 else 0.72))
    for name in ("back", "right", "left"):
        ax, ay, aw, ah = D_BODY[name]
        for x in range(aw):
            cv.put(ax + x, ay + 7, tone(c, 0.92))
            cv.put(ax + x, ay + 8, tone(c, 0.72))
    # The sash's knot at the left hip, its ends hanging.
    for (x, y) in ((6, 7), (6, 8), (6, 9), (7, 9), (6, 10)):
        cv.put(bx + x, by + y, tone(c, 1.05) if y < 9 else tone(c, 0.8))
    # Arms: sleeves, wrapped forearms, bare hands.
    for name, area in faces(D_ARM):
        cloth(cv, area, CLOTH_DARK, seed + 9 + len(name), name, base=2)
        x0, y0, w, h = area
        if name not in ("top", "bottom"):
            wraps(cv, (x0, y0 + 6, w, 4), WRAP, seed + 2, name, base=1)
            for x, y, px, py in cells((x0, y0 + 10, w, 2)):
                cv.put(px, py, shade(SKIN, 2 + LIGHT[name]))
            for x in range(w):
                cv.put(x0 + x, y0 + 5, trim_dark)
    for x, y, px, py in cells(D_ARM["bottom"]):
        cv.put(px, py, shade(SKIN, 1))
    # Legs: dark trousers, wrapped shins, soft boots.
    for name, area in faces(D_LEG):
        cloth(cv, area, CLOTH_DARK, seed + 13 + len(name), name, base=1)
        x0, y0, w, h = area
        if name not in ("top", "bottom"):
            wraps(cv, (x0, y0 + 6, w, 3), WRAP, seed + 4, name, base=0)
            leather(cv, (x0, y0 + 9, w, 3), WOOD, seed + 6, name, base=2)
    leather(cv, D_LEG["bottom"], WOOD, seed + 7, "bottom", base=2)
    # The cloak down the back: cloth, trimmed at the hem and edges, a little frayed at the bottom.
    for name, area in faces(D_CLOAK):
        cloth(cv, area, cloak, seed + 21 + len(name), name, base=2)
        if name in ("front", "back"):
            trim(cv, area, trim_c, ("bottom",))
            trim(cv, area, trim_dark, ("left", "right"))
            x0, y0, w, h = area
            for x in range(w):
                if noise(x, 0, seed + 30) > 0.72:
                    cv.put(x0 + x, y0 + h - 1, None)
    # The mantle over the shoulders.
    for name, area in faces(D_MANTLE):
        cloth(cv, area, cloak, seed + 25 + len(name), name, base=3)
        if name not in ("top", "bottom"):
            trim(cv, area, trim_c, ("bottom",))
    # The scabbard: dark lacquered wood, a brass collar and tip.
    for name, area in faces(D_SCABBARD):
        x0, y0, w, h = area
        for x, y, px, py in cells(area):
            t = 2 + LIGHT[name] + (1 if (y % 5 == 0) else 0)
            cv.put(px, py, shade(WOOD, t))
        if name not in ("top", "bottom"):
            for x in range(w):
                cv.put(x0 + x, y0, shade(BRASS, 3))
                cv.put(x0 + x, y0 + 1, shade(BRASS, 2))
                cv.put(x0 + x, y0 + h - 1, shade(BRASS, 3))
                cv.put(x0 + x, y0 + 6, trim_dark)
    for name, area in faces(D_GRIP):
        for x, y, px, py in cells(area):
            cv.put(px, py, trim_c if (y + (1 if name in ("left", "right") else 0)) % 2 else shade(CLOTH_DARK, 1))
    for name, area in faces(D_GUARD):
        for x, y, px, py in cells(area):
            cv.put(px, py, shade(BRASS, 3 + (1 if name == "top" else 0)))
    for name, area in faces(D_TASSEL, "front", "back"):
        x0, y0, w, h = area
        for x, y, px, py in cells(area):
            if y == 0:
                cv.put(px, py, shade(BRASS, 3))
            elif y < h - 1 or x == 0:
                cv.put(px, py, tone(c, 1.0 if x == 0 else 0.8))
    return cv.image()


def duelist_glow_texture():
    """What glows on a duelist: the emblem on its sash's knot and a line of light along its scabbard's collar (white: tinted)."""
    cv = Sheet(64, 64)
    bx, by, _, _ = D_BODY["front"]
    for (x, y, a) in ((6, 7, 255), (6, 8, 230), (6, 9, 140), (5, 7, 90), (5, 8, 70), (7, 8, 60)):
        cv.put(bx + x, by + y, (255, 255, 255, a))
    for name in ("front", "right", "left", "back"):
        x0, y0, w, h = D_SCABBARD[name]
        for x in range(w):
            cv.put(x0 + x, y0 + 6, (255, 255, 255, 170))
    hx, hy, hw, hh = D_HOOD["front"]
    for x in (1, 6):
        cv.put(hx + x, hy + 1, (255, 255, 255, 90))
    return cv.image()


# ============================================================== the fallen knight (FallenKnightModel, 64x64)

K_HEAD = box(0, 0, 8, 8, 8)
K_HELM = box(32, 0, 8, 8, 8)
K_BODY = box(16, 16, 8, 12, 4)
K_ARM = box(40, 16, 4, 12, 4)
K_LEG = box(0, 16, 4, 12, 4)
K_CREST = box(0, 32, 1, 4, 8)
K_PAULDRON = box(18, 32, 5, 3, 5)
K_TABARD = box(38, 32, 6, 10, 0)
K_CLOAK = box(0, 44, 8, 14, 0)

OLD_STEEL = ramp("#14161C", "#22262E", "#323842", "#454C58", "#5C6470", "#78808C", "#98A0AA")
RUST = hexc("#6A3A1E")
FADED_RED = ramp("#1E0A0A", "#2E1010", "#441818", "#5A2222", "#702C2A")
RAG = ramp("#14120F", "#1E1B17", "#2A2620", "#38332B")
CRACK = (14, 12, 16)

# The helm's front: a visor slit (v), breaths (o), the ridge down its middle (r), rivets (i).
K_VISOR = """
    ...rr...
    i..rr..i
    ...rr...
    vvvvvvvv
    ...rr...
    .o.rr.o.
    o.orro.o
    i..rr..i
"""
# Cracks in the breastplate (c), where the aura shows through.
K_CRACKS = """
    ........
    ...c....
    ....c...
    ...cc...
    ..c..c..
    .c....c.
    ......c.
    ........
    ........
    ........
    ........
    ........
"""


def knight_paint(glow=False):
    cv = Sheet(64, 64)
    seed = 77
    if not glow:
        for name, area in faces(K_HEAD):
            for x, y, px, py in cells(area):
                cv.put(px, py, (10, 9, 12))
        for name, area in faces(K_HELM):
            plate(cv, area, OLD_STEEL, seed + len(name), name, base=3, rust=RUST, rust_amount=0.16)
        for name, area in faces(K_BODY):
            plate(cv, area, OLD_STEEL, seed + 3 + len(name), name, base=3, rust=RUST, rust_amount=0.2)
        for name, area in faces(K_ARM):
            plate(cv, area, OLD_STEEL, seed + 6 + len(name), name, base=3, rust=RUST, rust_amount=0.18)
            x0, y0, w, h = area
            if name not in ("top", "bottom"):
                # The elbow's joint and the gauntlet's cuff.
                for x in range(w):
                    cv.put(x0 + x, y0 + 5, shade(OLD_STEEL, 1))
                    cv.put(x0 + x, y0 + 9, shade(OLD_STEEL, 5))
                for x, y, px, py in cells((x0, y0 + 10, w, 2)):
                    cv.put(px, py, shade(OLD_STEEL, 2 + LIGHT[name]))
        for name, area in faces(K_LEG):
            plate(cv, area, OLD_STEEL, seed + 9 + len(name), name, base=2, rust=RUST, rust_amount=0.22)
            x0, y0, w, h = area
            if name not in ("top", "bottom"):
                for x in range(w):
                    cv.put(x0 + x, y0 + 5, shade(OLD_STEEL, 5))
                    cv.put(x0 + x, y0 + 6, shade(OLD_STEEL, 1))
        # The breastplate's ridge, its belt and faulds.
        bx, by, bw, bh = K_BODY["front"]
        for y in range(8):
            cv.put(bx + 3, by + y, shade(OLD_STEEL, 5))
            cv.put(bx + 4, by + y, shade(OLD_STEEL, 4))
        for name in ("front", "back", "right", "left"):
            x0, y0, w, h = K_BODY[name]
            for x in range(w):
                cv.put(x0 + x, y0 + 8, shade(WOOD, 2))
                cv.put(x0 + x, y0 + 10, shade(OLD_STEEL, 5 if x % 2 else 4))
                cv.put(x0 + x, y0 + 11, shade(OLD_STEEL, 2))
            if name == "front":
                cv.put(x0 + 3, y0 + 8, shade(BRASS, 2))
                cv.put(x0 + 4, y0 + 8, shade(BRASS, 3))
        # The crack across the breastplate, dark (its light is on the glow layer).
        for y, row in enumerate(r.strip() for r in K_CRACKS.strip("\n").splitlines()):
            for x, ch in enumerate(row):
                if ch == "c":
                    cv.put(bx + x, by + y, CRACK)
        # The crest: a ragged, faded red.
        for name, area in faces(K_CREST):
            for x, y, px, py in cells(area):
                t = 2 + LIGHT[name] + (1 if smooth(px, py * 2, seed, 1.5) > 0.6 else 0)
                if name in ("front", "back", "right", "left") and y == 0 and noise(px, py, seed) > 0.5:
                    continue
                cv.put(px, py, shade(FADED_RED, t))
        for name, area in faces(K_PAULDRON):
            plate(cv, area, OLD_STEEL, seed + 12 + len(name), name, base=4, rust=RUST, rust_amount=0.14)
            x0, y0, w, h = area
            if name not in ("top", "bottom"):
                for x in range(w):
                    cv.put(x0 + x, y0 + h - 1, shade(BRASS, 2))
                cv.put(x0 + 1, y0 + 1, shade(OLD_STEEL, 6))
                cv.put(x0 + w - 2, y0 + 1, shade(OLD_STEEL, 6))
        # The tabard: torn cloth with the ghost of an old device.
        for name, area in faces(K_TABARD, "front", "back"):
            x0, y0, w, h = area
            for x, y, px, py in cells(area):
                if y >= h - 3 and noise(px, py, seed + 40) > 0.55 + 0.1 * (h - 1 - y):
                    continue
                t = 1 + (1 if smooth(px * 0.6, py * 0.3, seed + 41, 2) > 0.6 else 0)
                cv.put(px, py, shade(FADED_RED, t + 1))
            for (dx, dy) in ((2, 2), (3, 2), (2, 3), (3, 3), (1, 3), (4, 3), (2, 4), (3, 4), (2, 5), (3, 5)):
                cv.put(x0 + dx, y0 + dy, shade(FADED_RED, 4))
        for name, area in faces(K_CLOAK, "front", "back"):
            x0, y0, w, h = area
            for x, y, px, py in cells(area):
                if (y >= h - 4 and noise(px, py, seed + 50) > 0.45 + 0.1 * (h - 1 - y)) or noise(px, py, seed + 51) > 0.96:
                    continue
                cv.put(px, py, shade(RAG, 1 + (1 if smooth(px * 0.5, py * 0.3, seed + 52, 2) > 0.55 else 0)))
    # The visor: dark on the skin, its slit lit on the glow layer.
    hx, hy, hw, hh = K_HELM["front"]
    for y, row in enumerate(r.strip() for r in K_VISOR.strip("\n").splitlines()):
        for x, ch in enumerate(row):
            if glow:
                if ch == "v":
                    a = 255 if 1 <= x <= 6 else 150
                    cv.put(hx + x, hy + y, (255, 255, 255, a))
                elif ch == "o":
                    cv.put(hx + x, hy + y, (255, 255, 255, 80))
            else:
                if ch == "v":
                    cv.put(hx + x, hy + y, (8, 7, 10))
                elif ch == "o":
                    cv.put(hx + x, hy + y, (16, 14, 18))
                elif ch == "r":
                    cv.put(hx + x, hy + y, shade(OLD_STEEL, 5))
                elif ch == "i":
                    cv.put(hx + x, hy + y, shade(OLD_STEEL, 6))
    if glow:
        bx, by, _, _ = K_BODY["front"]
        for y, row in enumerate(r.strip() for r in K_CRACKS.strip("\n").splitlines()):
            for x, ch in enumerate(row):
                if ch == "c":
                    cv.put(bx + x, by + y, (255, 255, 255, 230))
                    for dx, dy in ((1, 0), (-1, 0)):
                        if cv.get(bx + x + dx, by + y + dy) is None:
                            cv.put(bx + x + dx, by + y + dy, (255, 255, 255, 60))
        # The joints, where aura leaks between the plates.
        for name in ("front", "back"):
            x0, y0, w, h = K_ARM[name]
            for x in range(w):
                cv.put(x0 + x, y0 + 5, (255, 255, 255, 110))
            x0, y0, w, h = K_LEG[name]
            for x in range(w):
                cv.put(x0 + x, y0 + 6, (255, 255, 255, 90))
    return cv.image()


# ============================================================== icons (16x16)

def outline(cv: Canvas, colour):
    """A one-pixel outline round everything drawn."""
    filled = {(x, y) for y in range(cv.n) for x in range(cv.n) if cv.get(x, y) is not None}
    for (x, y) in list(filled):
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, y + dy) not in filled and cv.inside(x + dx, y + dy):
                cv.put(x + dx, y + dy, colour)


ICONS = {
    "aura_shard": ("""
        ................
        ..........o.....
        .........oWo....
        ........oWHco...
        .......oWHHco...
        ......oWHgHco...
        .....oWHgGgco...
        ....oWHgGgHco...
        ....oHHgGgHo....
        ...oWHHgHHco....
        ...oHHHHHco.....
        ..oHHHHHco......
        ..occHHco.......
        ...occco........
        ....ooo.........
        ................
    """, {"o": "#2A2440", "W": "#FFFFFF", "H": "#C8D4F4", "c": "#7C86C0", "g": "#F4D88A", "G": "#FFF4C8"}),
    "breath_sash": ("""
        ................
        ................
        .....oooooo.....
        ....oWWWWWWo....
        ...oWHHHHHHWo...
        ...oWHooooHWo...
        ...oWHo..oHWo...
        ....oWWooWWo....
        .....oWggWo.....
        .....ogGGgo.....
        ....oWWggWWo....
        ...oWHo..oHWo...
        ...oWHo..oHWWo..
        ..oWHo....oHWo..
        ..oWWo....oWWo..
        ..oooo....oooo..
    """, {"o": "#3A3446", "W": "#F2ECDE", "H": "#C8BEA8", "g": "#C8962E", "G": "#FFE89A"}),
}

FORGED_ICONS = {
    # A pale crystal blade grown like antler-bone on a gilt hilt, a line of light down its fuller.
    "lumenedge": ("""
        .............ooo
        ............oWHo
        ...........oWHco
        ..........oWHco.
        .........oWLco..
        ........oWLco...
        .......oWLco....
        ......oWLco.....
        ..oo.oWLco......
        ..ogoWLco.......
        ...ogLco........
        ...ogGo.........
        ..ogoogo........
        .ogo..oo........
        .oo.............
        ................
    """, {"o": "#1E2236", "W": "#FFFFFF", "H": "#D8EEFF", "c": "#8CB4E0", "L": "#BFE6FF", "g": "#B8862E", "G": "#F0C860"}),
    # A long haft and a curved storm-steel blade, its edge crackling.
    "skyrend_glaive": ("""
        ..........oooo..
        .........oWYYo..
        ........oWSSYo..
        ........oSSSSo..
        .......oSSSSo...
        .......oSSSo....
        ......oySSo.....
        ......ogyo......
        .....ohgo.......
        ....ohho........
        ...ohho.........
        ..ohho..........
        .ohho...........
        .oho............
        .oo.............
        ................
    """, {"o": "#141A2A", "Y": "#FFF6A0", "W": "#FFFFFF", "S": "#8CA8D8", "y": "#5C78B0", "h": "#5E4228", "g": "#C29A3A"}),
    # A heavy stone-and-steel maul's head on a short haft, geode crystal set in its face.
    "bulwark_maul": ("""
        ................
        ..oooooooooooo..
        .oSHHHHHHHHHHSo.
        .oSHHpPPpHHHSSo.
        .oSHHPAAPHHSSSo.
        .oSSHpPPpHSSSSo.
        .oDSSSSSSSSSSDo.
        ..oooooghoooooo.
        .......oho......
        ......oho.......
        .....oho........
        ....oho.........
        ...oho..........
        ..oho...........
        ..oo............
        ................
    """, {"o": "#16141E", "S": "#5A5E68", "H": "#8C909A", "D": "#3E424C", "p": "#6A44BC", "P": "#B996F4", "A": "#E2D2FF",
          "h": "#5E4228", "g": "#C29A3A"}),
}

# A manual's torn page: parchment, a ragged right edge, writing, and the method's emblem in a corner.
PAGE = """
    ................
    ..oooooooooo....
    ..oPPPPPPPPPo...
    ..oPPPPPPPPPPo..
    ..oPEEEPPPPPo...
    ..oPEEEPllPPPo..
    ..oPEEEPPPPPo...
    ..oPPPPPlllPPo..
    ..oPllPPPPPPo...
    ..oPPPPPllPPPo..
    ..oPlllPPPPPo...
    ..oPPPPPPllPPo..
    ..oPllPPPPPPo...
    ..oSPPPPPPSo....
    ...oooooooo.....
    ................
"""


def page_icon(method_id, color, highlight):
    cv = Canvas()
    c = rgb(color)
    blit(cv, PAGE, {"o": hexc("#4A3A26"), "P": hexc("#E8DCC0"), "S": hexc("#C8B894"), "l": hexc("#8A7A60"),
                    "E": hexc("#E8DCC0")})
    # A wax seal in the method's colour, its highlight struck into the heart.
    hi = rgb(highlight)
    wax = tone(c, 0.85)
    for (x, y) in ((5, 4), (4, 5), (5, 5), (6, 5), (5, 6), (4, 4), (6, 4), (4, 6), (6, 6)):
        cv.put(x, y, wax if (x, y) not in ((4, 4), (6, 4), (4, 6), (6, 6)) else tone(c, 0.6))
    cv.put(5, 5, hi)
    cv.put(5, 7, tone(c, 0.6))
    return cv.image()


def icon(name):
    art, pal = ICONS[name] if name in ICONS else FORGED_ICONS[name]
    cv = Canvas()
    blit(cv, art, {k: hexc(v) for k, v in pal.items()})
    return cv.image()


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
EGGS = {
    "duelist": (("#3E342C", "#6A5A4A", "#8A7660"), "#E8C468", """
        ................
        ................
        .......hh.......
        ......hhhh......
        ......hssh......
        .....chhhhc.....
        .....chhhhc.....
        ......ttt.......
        ........b.......
        .........b......
        ................
    """, {"h": "#2C2620", "s": "#B88460", "c": "#E8C468", "t": "#E8C468", "b": "#8A6A3A"}),
    "fallen_knight": (("#22262E", "#454C58", "#5C6470"), "#702C2A", """
        ................
        ................
        .......rr.......
        ......SSSS......
        ......vvvv......
        ......SSSS......
        .....PSSSSP.....
        ......SccS......
        ......SSSS......
        ................
    """, {"r": "#702C2A", "S": "#78808C", "v": "#C8D8FF", "P": "#98A0AA", "c": "#C8D8FF"}),
}


def egg_icon(kind):
    shell, spot, motif, pal = EGGS[kind]
    dark, mid, light = (hexc(c) for c in shell)
    cv = Canvas()
    rows = [r.strip() for r in EGG.strip("\n").splitlines()]
    edge = mix(dark, (0, 0, 0), 0.55)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == "o":
                cv.put(x, y, edge)
            elif ch == "1":
                t = (x + y) / 26
                col = light if t < 0.42 else mid if t < 0.72 else dark
                if noise(x, y, len(kind)) > 0.82:
                    col = hexc(spot)
                cv.put(x, y, col)
    for y, row in enumerate(r.strip() for r in motif.strip("\n").splitlines()):
        for x, ch in enumerate(row):
            if ch in pal and cv.get(x, y) is not None:
                cv.put(x, y, hexc(pal[ch]))
    return cv.image()


# ============================================================== data

def _smithing(name, template, base, addition, result_id, forged, rarity="rare"):
    return {"type": "minecraft:smithing_transform", "template": template, "base": base, "addition": addition,
            "result": {"id": result_id, "components": {
                "wildercord:aura_forged": forged,
                "minecraft:item_model": f"wildercord:{forged}",
                "minecraft:item_name": {"translate": f"item.wildercord.{forged}"},
                "minecraft:rarity": rarity}}}


FORGINGS = {
    # forging: (reagent, the two weapons it can be forged from)
    "lumenedge": ("wildercord:lumen_antler", ("minecraft:diamond_sword", "minecraft:netherite_sword")),
    "skyrend_glaive": ("wildercord:fulgurite_shard", ("minecraft:diamond_spear", "minecraft:netherite_spear")),
    "bulwark_maul": ("wildercord:geode_grit", ("minecraft:diamond_axe", "minecraft:netherite_axe")),
}

KNIGHT_LOOT = {
    "type": "minecraft:entity",
    "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "wildercord:manual_page", "functions": [
            {"function": "wildercord:knight_method"},
            {"function": "minecraft:set_count", "count": 1},
            {"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting", "count": {"type": "minecraft:uniform", "min": 0.0, "max": 1.0}}]}]},
        {"rolls": 1, "conditions": [{"condition": "minecraft:killed_by_player"},
                                    {"condition": "minecraft:random_chance_with_enchanted_bonus", "enchantment": "minecraft:looting",
                                     "unenchanted_chance": 0.25, "enchanted_chance": {"type": "minecraft:linear", "base": 0.33, "per_level_above_first": 0.08}}],
         "entries": [{"type": "minecraft:item", "name": "wildercord:aura_shard"}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:iron_nugget", "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": 3}}]}]},
    ],
    "random_sequence": "wildercord:entities/fallen_knight",
}


def skins():
    out = {f"duelist/{m}": duelist_texture(m, c, h) for m, _, c, h in METHODS}
    out["duelist/glow"] = duelist_glow_texture()
    out["fallen_knight"] = knight_paint()
    out["fallen_knight_glow"] = knight_paint(glow=True)
    return out


def write(g):
    for name, image in skins().items():
        g.save(image, g.ASSETS / f"textures/entity/{name}.png")
    tex = g.ASSETS / "textures/item"
    # Manual pages: one item, its method a component, a page per method.
    cases = []
    for method_id, _, color, highlight in METHODS:
        g.save(page_icon(method_id, color, highlight), tex / f"manual_page/{method_id}.png")
        g.item_model(f"manual_page/{method_id}", f"manual_page/{method_id}")
        cases.append({"when": method_id, "model": {"type": "minecraft:model", "model": f"wildercord:item/manual_page/{method_id}"}})
    g.save(page_icon("unknown", 0x8A84A0, 0xD8D0F0), tex / "manual_page/unknown.png")
    g.item_model("manual_page/unknown", "manual_page/unknown")
    g.write_json(g.ASSETS / "items/manual_page.json", {"model": {
        "type": "minecraft:select", "property": "minecraft:component", "component": "wildercord:breathing_method", "cases": cases,
        "fallback": {"type": "minecraft:model", "model": "wildercord:item/manual_page/unknown"}}})
    for name in ("aura_shard", "breath_sash"):
        g.save(icon(name), tex / f"{name}.png")
        g.item_model(name, name)
        g.write_json(g.ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{name}"}})
    # The forged weapons' own looks (set by the forging's item_model component): a sword and an axe held like tools, the glaive
    # held as a spear is.
    for name in ("lumenedge", "bulwark_maul"):
        g.save(icon(name), tex / f"{name}.png")
        g.write_json(g.ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": f"wildercord:item/{name}"}})
        g.write_json(g.ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{name}"}})
    g.save(icon("skyrend_glaive"), tex / "skyrend_glaive.png")
    g.item_model("skyrend_glaive", "skyrend_glaive")
    g.write_json(g.ASSETS / "models/item/skyrend_glaive_in_hand.json", {"parent": "minecraft:item/spear_in_hand",
                                                                         "textures": {"layer0": "wildercord:item/skyrend_glaive"}})
    g.write_json(g.ASSETS / "items/skyrend_glaive.json", {"model": {
        "type": "minecraft:select", "property": "minecraft:display_context",
        "cases": [{"when": ["gui", "ground", "fixed", "on_shelf"], "model": {"type": "minecraft:model", "model": "wildercord:item/skyrend_glaive"}}],
        "fallback": {"type": "minecraft:model", "model": "wildercord:item/skyrend_glaive_in_hand"}}, "swap_animation_scale": 1.95})
    for kind in EGGS:
        egg = f"{kind}_spawn_egg"
        g.save(egg_icon(kind), tex / f"{egg}.png")
        g.item_model(egg, egg)
        g.write_json(g.ASSETS / f"items/{egg}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{egg}"}})
    # Recipes: the forgings at a smithing table, pages bound into a manual, the sash.
    for forged, (reagent, bases) in FORGINGS.items():
        for base in bases:
            tier = base.split(":")[1].split("_")[0]
            name = f"{forged}_from_{tier}"
            g.write_json(g.DATA / f"recipe/{name}.json", _smithing(name, "wildercord:aura_shard", base, reagent, base, forged))
            g.unlock_advancement(f"wildercord:{name}", "wildercord:aura_shard")
    g.write_json(g.DATA / "recipe/breathing_manual_from_pages.json", {
        "type": "wildercord:manual_pages", "category": "misc",
        "ingredients": ["wildercord:manual_page"] * 4 + ["minecraft:book"], "result": {"id": "wildercord:breathing_manual"}})
    g.unlock_advancement("wildercord:breathing_manual_from_pages", "wildercord:manual_page")
    g.write_json(g.DATA / "recipe/breath_sash.json", {
        "type": "minecraft:crafting_shaped", "category": "equipment",
        "key": {"W": "minecraft:white_wool", "S": "minecraft:string", "A": "wildercord:aura_shard"},
        "pattern": [" SW", "WAW", "WS "], "result": {"id": "wildercord:breath_sash"}})
    g.unlock_advancement("wildercord:breath_sash", "wildercord:aura_shard")
    g.write_json(g.DATA / "loot_table/entities/fallen_knight.json", KNIGHT_LOOT)
    g.write_json(g.DATA / "loot_table/entities/duelist.json", {"type": "minecraft:entity", "pools": [],
                                                              "random_sequence": "wildercord:entities/duelist"})
    # Where knights haunt: a data pack can add more.
    g.write_json(g.DATA / "tags/worldgen/structure/knight_haunts.json", {"replace": False, "values": [
        "minecraft:stronghold", "minecraft:ancient_city", "#wildercord:dungeon"]})


# ============================================================== English

LANG = {
    "entity.wildercord.duelist": "Wandering Duelist",
    "entity.wildercord.fallen_knight": "Fallen Knight",
    "item.wildercord.duelist_spawn_egg": "Wandering Duelist Spawn Egg",
    "item.wildercord.fallen_knight_spawn_egg": "Fallen Knight Spawn Egg",
    "item.wildercord.manual_page": "Manual Page",
    "item.wildercord.manual_page.named": "%s Page",
    "item.wildercord.manual_page.lore": "Carried in a knight's armour since the knight was a swordsman.",
    "item.wildercord.manual_page.use": "%s pages of one method and a book bind into its Breathing Manual.",
    "item.wildercord.aura_shard": "Aura Shard",
    "item.wildercord.aura_shard.lore": "A sliver of a fallen knight's aura, set hard as crystal. It's still warm.",
    "item.wildercord.aura_shard.use": "At a smithing table it forges a diamond or netherite blade with aura; it's the heart of a Breath Sash.",
    "item.wildercord.breath_sash": "Breath Sash",
    "item.wildercord.lumenedge": "Lumenedge",
    "item.wildercord.skyrend_glaive": "Skyrend Glaive",
    "item.wildercord.bulwark_maul": "Bulwark Maul",
    "tooltip.wildercord.gear.sash": "Aura: holds more, and the breathing stance settles twice as fast and breathes in half again as much",
    "tooltip.wildercord.aura_forged.lumenedge": "Aura-forged: %s%% more aura from every blow",
    "tooltip.wildercord.aura_forged.lumenedge.lore": "Lumen antler grown along the blade. It drinks in what it strikes.",
    "tooltip.wildercord.aura_forged.skyrend_glaive": "Aura-forged: Aura Slash %s%% stronger, flying further and wider, through more foes",
    "tooltip.wildercord.aura_forged.skyrend_glaive.lore": "Storm-glass in the edge. Its crescents carry like thunder.",
    "tooltip.wildercord.aura_forged.bulwark_maul": "Aura-forged: Aura Guard costs %s%% less",
    "tooltip.wildercord.aura_forged.bulwark_maul.lore": "Riven stone in its head. It stands like a wall.",
    "tooltip.wildercord.aura_forged.from": "Forged from a %s",
    "message.wildercord.duelist.challenge": "%1$s, who breathes %2$s, looks you over: \"You carry a blade. Will you cross it with mine?\"",
    "message.wildercord.duelist.challenge_how": "Use the duelist again within ten seconds to accept. It will meet you at %s. Nobody dies: brought low, you're knocked out, and it yields.",
    "message.wildercord.duelist.begins": "The duel with %s begins. Win, and it teaches you its breathing.",
    "message.wildercord.duelist.busy": "%s is in a duel",
    "message.wildercord.duelist.resting": "%s is catching its breath: \"Again? Give me a moment.\"",
    "message.wildercord.duelist.no_blade": "%s eyes your empty hands: \"Come back with a blade.\"",
    "message.wildercord.duelist.farewell": "%s bows, and turns to go.",
    "message.wildercord.duelist.turns_aside": "%s turns your blow aside with its sheathed blade: \"Speak to me first.\"",
    "message.wildercord.duelist.won": "%1$s kneels: \"Well fought. Breathe as I breathe.\" It shows you %2$s.",
    "message.wildercord.duelist.manual": "You already breathe another way: it hands you its manual of %s, to read if you choose.",
    "message.wildercord.duelist.magic": "%s frowns: \"You leaned on magic. The lesson stands, but it proves nothing of your blade.\"",
    "message.wildercord.duelist.lost": "%s sheathes its blade: \"Breathe, and come back when your blade is steadier.\"",
    "message.wildercord.duelist.forfeit": "%s sheathes its blade: the duel is over.",
    "message.wildercord.duelist.called_off": "The duel with %s is called off.",
    "message.wildercord.aura_world.turned": "%s turns your blow aside!",
    "message.wildercord.aura_world.guard_broken": "Your axe breaks %s's guard!",
    "screen.wildercord.aura.trial.duel": "A duel: best a wandering duelist in an aura duel, without magic",
    "screen.wildercord.grimoire.field_guide_wanderers": "Wanderers",
    "toast.wildercord.aura.duelist": "A duelist's lesson",
    "toast.wildercord.aura.clash": "Aura Clash",
    "guide.wildercord.duelist": "A sword master in a travelling cloak, met near villages, on roads and at small campfires. Use one to be challenged to a duel at your own stage; win, and it teaches you its breathing method",
    "guide.wildercord.duelist.hint": "A cloaked traveller by a campfire, a blade at the hip",
    "guide.wildercord.fallen_knight": "Old armour that aura still walks in. It raises its blade before a slash (step off the line on the ground) and braces its guard: wait it out, strike from behind, or break it with an axe",
    "guide.wildercord.fallen_knight.hint": "Plate scraping in the deep halls: strongholds, ancient cities, old dungeons",
    "subtitles.wildercord.kit.duelist.challenge": "Blade drawn",
    "subtitles.wildercord.kit.duelist.bow": "Duelist bows",
    "subtitles.wildercord.kit.duelist.yield": "Duelist yields",
    "subtitles.wildercord.kit.duelist.sheathe": "Blade sheathed",
    "subtitles.wildercord.kit.duelist.clash": "Aura slashes clash",
    "subtitles.wildercord.kit.fallen_knight.ambient": "Fallen Knight's armour creaks",
    "subtitles.wildercord.kit.fallen_knight.hurt": "Fallen Knight clangs",
    "subtitles.wildercord.kit.fallen_knight.death": "Fallen Knight collapses",
    "subtitles.wildercord.kit.fallen_knight.windup": "Fallen Knight raises its blade",
    "subtitles.wildercord.kit.fallen_knight.step": "Armoured footsteps",
}


def preview(out_dir):
    images = list(skins().items())
    images += [(m, page_icon(m, c, h)) for m, _, c, h in METHODS]
    images += [(n, icon(n)) for n in list(ICONS) + list(FORGED_ICONS)] + [(k, egg_icon(k)) for k in EGGS]
    scale = 6
    cols = 6
    cell = (64 * scale + 16, 64 * scale + 16)
    rows = (len(images) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell[0], rows * cell[1]), (44, 40, 52, 255))
    for i, (name, img) in enumerate(images):
        s = scale if img.width > 16 else scale * 4
        big = img.resize((img.width * s, img.height * s), Image.NEAREST)
        x, y = (i % cols) * cell[0] + 8, (i // cols) * cell[1] + 8
        sheet.paste(big, (x, y), big)
    out = Path(out_dir) / "aura_world_art.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    return str(out)


if __name__ == "__main__":
    print(preview(ROOT / "build/art-preview"))
