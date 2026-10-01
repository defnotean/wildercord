"""Residues and reagents: the lasting marks big magic leaves on the world, and what they give.

Ten residue blocks (one per element) and ten reagent items, drawn in the house style: 16x16, lit from the
top-left, small deterministic rules rather than photographic noise, a one-pixel dark outline on items.
What glows (embers, storm-glass tips, a void scar's rim, a glyph's lines...) is drawn on a separate
cut-out layer that the block model lights itself (`light_emission`), so it glows in the dark the vanilla
way and stays correct under shader packs; most of those layers flicker or pulse as animations.

Also writes each block's model and blockstate, the loot tables (each residue gives its reagent), the
block and item tags (`wildercord:residue_ground`, `wildercord:reagents`, mining tags, bees' flowers) and
the English text. Called from generate_assets.py: residue_art.write(generate_assets_module).

Run this file directly to render a review sheet into build/art-preview/residue_art.png.
"""
from __future__ import annotations

import math
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from item_art import Canvas, blit, hexc, mix  # noqa: E402
from world_art import noise  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent

# path, element, placement, reagent, block name, reagent name
RESIDUES = [
    ("smouldering_ash", "fire", "rest", "cinder_ash", "Smouldering Ash", "Cinder Ash"),
    ("everfrost", "frost", "cover", "everfrost_shard", "Everfrost", "Everfrost Shard"),
    ("fulgurite", "storm", "rest", "fulgurite_shard", "Fulgurite", "Fulgurite Shard"),
    ("lingering_eddy", "wind", "rest", "bottled_gale", "Lingering Eddy", "Bottled Gale"),
    ("riven_stone", "earth", "cover", "geode_grit", "Riven Stone", "Geode Grit"),
    ("wildbloom", "life", "rest", "wildbloom_petal", "Wildbloom", "Wildbloom Petal"),
    ("void_scar", "void", "cover", "hollow_dust", "Void Scar", "Hollow Dust"),
    ("star_glyph", "arcane", "rest", "star_dust", "Star Glyph", "Star Dust"),
    ("stilled_sand", "time", "rest", "hourglass_sand", "Stilled Sand", "Hourglass Sand"),
    ("bloodmoss", "blood", "rest", "sanguine_bead", "Bloodmoss", "Sanguine Bead"),
]

# Natural ground a residue may take or lie on. Only what the world makes; never anything players build with
# (planks, cobblestone, bricks, smooth stone, glass, terracotta, farmland, paths...).
RESIDUE_GROUND = [
    "#minecraft:substrate_overworld",  # dirt, coarse and rooted dirt, grass, podzol, mycelium, moss, mud
    "#minecraft:sand",
    "minecraft:gravel",
    "minecraft:clay",
    "#minecraft:base_stone_overworld",  # stone, granite, diorite, andesite, tuff, deepslate
    "minecraft:calcite",
    "minecraft:dripstone_block",
    "minecraft:snow_block",
    "#minecraft:base_stone_nether",  # netherrack, basalt, blackstone
    "#minecraft:nylium",
    "minecraft:soul_sand",
    "minecraft:soul_soil",
    "minecraft:end_stone",
]


# ============================================================== little helpers


def rgba(w: int = 16, h: int = 16) -> Image.Image:
    return Image.new("RGBA", (w, h), (0, 0, 0, 0))


def put(im: Image.Image, x: int, y: int, c, a: int = 255):
    if 0 <= x < im.width and 0 <= y < im.height:
        im.putpixel((x, y), (c[0], c[1], c[2], a))


def smooth(x: float, y: float, seed: int, cell: int = 4) -> float:
    """Value noise in [0, 1) that tiles every 16 pixels: lattice values eased between."""
    gx, gy = x / cell, y / cell
    x0, y0 = math.floor(gx), math.floor(gy)
    fx, fy = gx - x0, gy - y0
    n = 16 // cell

    def lattice(ix, iy):
        return noise(ix % n, iy % n, seed)

    def ease(t):
        return t * t * (3 - 2 * t)

    a, b = lattice(x0, y0), lattice(x0 + 1, y0)
    c, d = lattice(x0, y0 + 1), lattice(x0 + 1, y0 + 1)
    u, v = ease(fx), ease(fy)
    return (a * (1 - u) + b * u) * (1 - v) + (c * (1 - u) + d * u) * v


def lit_field(seed: int, cell: int = 4, rough: float = 0.0, rough_seed: int = 1):
    """A height field and its light from the top-left: 16x16 values in [0, 1]."""
    h = [[smooth(x, y, seed, cell) + rough * (noise(x, y, rough_seed) - 0.5) for x in range(16)] for y in range(16)]
    out = [[0.0] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            slope = h[(y - 1) % 16][(x - 1) % 16] - h[(y + 1) % 16][(x + 1) % 16]
            out[y][x] = max(0.0, min(1.0, 0.5 + (h[y][x] - 0.5) * 0.9 + slope * 1.4))
    return out


def tone(ramp, level: float):
    return ramp[max(0, min(len(ramp) - 1, int(round(level * (len(ramp) - 1)))))]


def line(points, step: float = 0.25):
    """The pixels along a polyline."""
    out = []
    for (x0, y0), (x1, y1) in zip(points, points[1:]):
        n = max(1, int(max(abs(x1 - x0), abs(y1 - y0)) / step))
        for i in range(n + 1):
            t = i / n
            p = (int(round(x0 + (x1 - x0) * t)), int(round(y0 + (y1 - y0) * t)))
            if not out or out[-1] != p:
                out.append(p)
    return out


def frames(make, count: int):
    return [make(f) for f in range(count)]


def save_png(g, img, path: Path, frametime: int = 4, interpolate: bool = False):
    """One image, or a list of frames as an animated strip with its own .mcmeta (frame time, smooth or stepped)."""
    import json
    path.parent.mkdir(parents=True, exist_ok=True)
    meta = path.with_name(path.name + ".mcmeta")
    if not isinstance(img, list):
        img.save(path)
        if meta.exists():
            meta.unlink()
        return
    w, h = img[0].size
    strip = Image.new("RGBA", (w, h * len(img)), (0, 0, 0, 0))
    for i, f in enumerate(img):
        strip.paste(f, (0, i * h))
    strip.save(path)
    anim = {"frametime": frametime}
    if interpolate:
        anim["interpolate"] = True
    meta.write_text(json.dumps({"animation": anim}, indent=2) + "\n", encoding="utf-8", newline="\n")


# ============================================================== Smouldering Ash (fire)

ASH = [hexc(h) for h in ("#221E1E", "#322D2C", "#433D3B", "#57504D", "#6D6662", "#867E78", "#A39A93")]
EMBER = [hexc(h) for h in ("#5A1206", "#9A2A0A", "#D24A12", "#F06E32", "#FFA448", "#FFD878", "#FFF2C8")]
# Where the embers sit in the ash, and their phase in the glow's pulse.
EMBERS = [(3, 3, 0), (4, 3, 2), (11, 2, 1), (12, 5, 3), (7, 7, 0), (8, 7, 2), (8, 8, 1), (2, 10, 3), (13, 11, 0), (12, 12, 2), (5, 13, 1),
          (9, 12, 3), (6, 4, 1)]


def ash_top() -> Image.Image:
    im = rgba()
    light = lit_field(11, 8, 0.22, 12)
    for y in range(16):
        for x in range(16):
            r = noise(x, y, 13)
            c = tone(ASH, light[y][x] * 0.9 + 0.02)
            if r < 0.06:
                c = ASH[0]  # flecks of char
            elif r > 0.95:
                c = ASH[6]  # pale flakes
            put(im, x, y, c)
    # The embers sit in little hollows: dark round each.
    for x, y, _ in EMBERS:
        put(im, x, y, ASH[0])
    return im


def ash_embers(frame: int) -> Image.Image:
    im = rgba()
    for x, y, phase in EMBERS:
        glow = 0.55 + 0.45 * math.sin((frame + phase) / 4 * 2 * math.pi)
        put(im, x, y, tone(EMBER, 0.35 + 0.6 * glow))
    return im


# ============================================================== Everfrost (frost)

FROST_TOP = [hexc(h) for h in ("#5E86A8", "#7EA8C8", "#A2C8E2", "#C2E0F2", "#DDF1FB", "#F2FBFF", "#FFFFFF")]
FROST_SIDE = [hexc(h) for h in ("#3E4C5E", "#4E5F74", "#62768C", "#7890A6", "#94AEC4", "#B6D0E2")]
# Frost ferns: crystals branching over the crust.
FERNS = [[(1, 14), (4, 10), (6, 6), (7, 2)], [(4, 10), (2, 8)], [(5, 8), (8, 7)], [(6, 6), (4, 4)], [(9, 15), (11, 11), (14, 9)],
         [(11, 11), (12, 14)], [(12, 10), (11, 7)], [(10, 1), (12, 3), (15, 3)]]
GLINTS = [(3, 5, 0), (10, 4, 2), (13, 12, 1), (6, 12, 3), (8, 9, 2)]


def everfrost_top() -> Image.Image:
    im = rgba()
    light = lit_field(21, 8, 0.15, 22)
    for y in range(16):
        for x in range(16):
            put(im, x, y, tone(FROST_TOP, 0.12 + light[y][x] * 0.4))
    for fern in FERNS:
        for (x, y) in line(fern):
            if (x + 1, y + 1) not in line(fern):
                put(im, x + 1, y + 1, FROST_TOP[0])  # each crystal casts a little shadow down-right
        for (x, y) in line(fern):
            put(im, x, y, FROST_TOP[5])
    for x, y, _ in GLINTS:
        put(im, x, y, FROST_TOP[6])
    return im


def everfrost_side() -> Image.Image:
    im = rgba()
    light = lit_field(23, 4, 0.3, 24)
    for y in range(16):
        for x in range(16):
            c = tone(FROST_SIDE, 0.2 + light[y][x] * 0.6)
            # Ice veins run down through the frozen ground.
            if (x * 3 + y * 5 + int(noise(x // 3, y, 25) * 6)) % 11 == 0:
                c = FROST_SIDE[5]
            put(im, x, y, c)
    # The frost crust over the top edge, and drips of it creeping down.
    drips = [2, 3, 1, 2, 4, 2, 1, 3, 2, 5, 2, 1, 3, 2, 4, 2]
    for x in range(16):
        for y in range(drips[x]):
            put(im, x, y, FROST_TOP[5] if y == 0 else FROST_TOP[3] if y < drips[x] - 1 else FROST_TOP[2])
    return im


def everfrost_glint(frame: int) -> Image.Image:
    im = rgba()
    for x, y, phase in GLINTS:
        step = (frame + phase) % 4
        if step == 0:
            for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
                put(im, x + dx, y + dy, FROST_TOP[6] if (dx, dy) == (0, 0) else FROST_TOP[4])
        elif step == 1:
            put(im, x, y, FROST_TOP[6])
    return im


# ============================================================== Fulgurite (storm)

GLASS = [hexc(h) for h in ("#14121C", "#26232F", "#3A3646", "#524D62", "#6E6984", "#9894B0", "#C8C4DC")]
SPARK = [hexc(h) for h in ("#FFB830", "#FFE650", "#FFF6B0", "#FFFFFF")]
FUSED = [hexc(h) for h in ("#6E5A3A", "#9A8058", "#C2A676")]
# The branching tubes lightning fused out of the ground: trunk and branches, bottom to tip.
TUBES = [[(8, 15), (8, 12), (7, 9), (7, 6), (6, 3), (6, 1)], [(7, 10), (5, 8), (3, 6), (2, 3)], [(8, 9), (10, 7), (12, 5), (13, 2)],
         [(8, 13), (11, 12), (13, 10)], [(7, 6), (9, 4), (9, 2)]]


def _tube_pixels():
    core, tips = set(), []
    for tube in TUBES:
        pts = line(tube)
        core.update(pts)
        tips.append(pts[-1])
    return core, tips


def fulgurite() -> Image.Image:
    im = rgba()
    core, tips = _tube_pixels()
    wall = set()
    for (x, y) in core:
        wall.add((x - 1, y))
        wall.add((x + 1, y))
    for (x, y) in wall - core:
        # Lit on its left, in shadow on its right.
        left = (x + 1, y) in core
        put(im, x, y, GLASS[5] if left else GLASS[1])
    for (x, y) in core:
        put(im, x, y, GLASS[3] if noise(x, y, 31) < 0.6 else GLASS[4])
    # A crust of fused sand round its foot.
    for x in range(4, 13):
        if noise(x, 15, 32) < 0.7:
            put(im, x, 15, FUSED[int(noise(x, 15, 33) * 3)])
        if 5 <= x <= 11 and noise(x, 14, 34) < 0.45:
            put(im, x, 14, FUSED[1])
    for (x, y) in tips:
        put(im, x, y, SPARK[1])
    return im


def fulgurite_glow(frame: int) -> Image.Image:
    im = rgba()
    core, tips = _tube_pixels()
    for i, (x, y) in enumerate(tips):
        hot = (frame + i) % 3
        put(im, x, y, SPARK[3] if hot == 0 else SPARK[2])
        if hot != 2:
            put(im, x, y + 1, SPARK[1])
    # Now and then a thread of the old lightning still runs down one tube.
    tube = line(TUBES[frame % len(TUBES)])
    for j, (x, y) in enumerate(tube):
        if j % 3 == frame % 3:
            put(im, x, y, SPARK[0])
    return im


# ============================================================== Lingering Eddy (wind)

WIND = [hexc(h) for h in ("#7FE0C0", "#B6F2DC", "#D8FFF0", "#FFFFFF")]
EDDY_FRAMES = 8


def eddy(frame: int) -> Image.Image:
    im = rgba()
    turn = frame / EDDY_FRAMES * 2 * math.pi
    # Two wisps spiralling up round the eddy's heart, turning frame by frame; fainter as they widen.
    for arm in range(2):
        for i in range(90):
            t = i / 90
            a = turn + arm * math.pi + t * 4 * math.pi
            r = 0.8 + t * 5.0
            x = 7.5 + math.cos(a) * r
            y = 15 - t * 14
            # The near side of the turn is brighter than the far side.
            near = math.sin(a) > 0
            alpha = int((235 if near else 150) * (1 - t * 0.45))
            c = WIND[3] if i % 11 == 0 else WIND[2] if near else WIND[1]
            put(im, int(round(x)), int(round(y)), c, alpha)
    return im


# ============================================================== Riven Stone (earth)

RIVEN = [hexc(h) for h in ("#2E2622", "#41372F", "#564A3F", "#6C5E52", "#847566", "#9E8E7E", "#BCAC9A")]
AMBER = [hexc(h) for h in ("#8A4A12", "#C86E1C", "#FF9A30", "#FFC468", "#FFE8B0")]
# The cracks the ground split along (on the top face; the sides take theirs from the top's edges).
CRACKS_TOP = [[(0, 4), (3, 5), (6, 4), (8, 7), (12, 6), (15, 8)], [(8, 7), (7, 11), (9, 15)], [(3, 5), (2, 9), (0, 12)],
              [(12, 6), (13, 2), (12, 0)], [(7, 11), (11, 12), (15, 13)], [(2, 9), (5, 13), (4, 15)]]
CRACKS_SIDE = [[(3, 0), (4, 4), (2, 8), (3, 12), (2, 15)], [(9, 0), (11, 5), (10, 9)], [(10, 9), (13, 12), (12, 15)], [(4, 4), (8, 6)]]


def _riven(cracks, seed: int) -> tuple[Image.Image, set]:
    im = rgba()
    split = set()
    for crack in cracks:
        split.update(line(crack))
    light = lit_field(seed, 4, 0.4, seed + 1)
    for y in range(16):
        for x in range(16):
            # Each plate between the cracks sits at its own tilt: a tone of its own.
            plate = int(smooth(x, y, seed + 5, 8) * 3)
            put(im, x, y, tone(RIVEN, 0.25 + light[y][x] * 0.45 + plate * 0.05))
    for (x, y) in split:
        put(im, x, y, RIVEN[0])
        if (x + 1, y + 1) not in split:
            put(im, x + 1, y + 1, RIVEN[5])  # the lower lip catches the light
    return im, split


def riven_top() -> Image.Image:
    return _riven(CRACKS_TOP, 41)[0]


def riven_side() -> Image.Image:
    return _riven(CRACKS_SIDE, 43)[0]


def riven_veins(cracks, frame: int) -> Image.Image:
    im = rgba()
    for i, crack in enumerate(cracks):
        for j, (x, y) in enumerate(line(crack)):
            glow = 0.5 + 0.5 * math.sin((j * 0.7 - frame * 1.6 + i) )
            if glow > 0.25:
                put(im, x, y, tone(AMBER, 0.3 + glow * 0.7))
    return im


# ============================================================== Wildbloom (life)

STEM = [hexc(h) for h in ("#1E3E1A", "#2E5A24", "#3E7A30", "#58A040", "#7CC85A")]
PETAL = [hexc(h) for h in ("#7A2A62", "#B04A8A", "#E070B8", "#FF9CD8", "#FFD0EE", "#FFF6FC")]
HEART = [hexc(h) for h in ("#5A3AB8", "#9A7CFF", "#FFE8A0")]

WILDBLOOM = """
    ................
    .....aPPPa......
    ....aPpWpPa.....
    ...aPpWWWpPa....
    ..aPPpWhWpPPa...
    ..aPpWhHhWpPa...
    ...aPpWhWpPa....
    ....aPpWpPa..b..
    .....aPsPa..bQb.
    .......s.....bs.
    ..ll...s....ss..
    ..lLl..s...s....
    ...lLl.s..sL....
    ....lLss.lLl....
    .......s.ll.....
    .......s........
"""


def wildbloom() -> Image.Image:
    cv = Canvas()
    blit(cv, WILDBLOOM, {"a": PETAL[1], "P": PETAL[2], "p": PETAL[3], "W": PETAL[4], "h": HEART[1], "H": HEART[2],
                         "s": STEM[2], "l": STEM[1], "L": STEM[3], "b": PETAL[2], "Q": PETAL[4]})
    return cv.image()


def wildbloom_glow(frame: int) -> Image.Image:
    cv = Canvas()
    breathe = frame % 4
    pal = {"W": PETAL[5] if breathe < 2 else PETAL[4], "h": HEART[1], "H": HEART[2], "Q": PETAL[5]}
    rows = [r.strip() for r in WILDBLOOM.strip("\n").splitlines()]
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                cv.put(x, y, pal[ch])
    return cv.image()


# ============================================================== Void Scar (void)

VOID_GROUND = [hexc(h) for h in ("#100A16", "#1A1222", "#251B2E", "#30263A", "#3E3248", "#4C3E58")]
VOID_RIM = [hexc(h) for h in ("#3A1060", "#6A2AA8", "#9A48E0", "#B45AF0", "#E0B0FF")]
VOID_CORE = hexc("#020004")


def _scar(stage: int):
    """The rift's pixels at a stage: (core, rim, fringe). It narrows from a gash to a sliver as it closes."""
    half_width = [2.6, 1.9, 1.2, 0.55][stage]
    length = [7.2, 6.4, 5.4, 4.2][stage]
    core, near = set(), set()
    for y in range(16):
        for x in range(16):
            # Along the diagonal from bottom-left to top-right, a lens of darkness, ragged at its edges.
            u = ((x - 7.5) + (7.5 - y)) / math.sqrt(2)
            v = ((x - 7.5) - (7.5 - y)) / math.sqrt(2)
            if abs(u) > length:
                continue
            w = half_width * math.sqrt(max(0.0, 1 - (u / length) ** 2)) + (noise(x, y, 61) - 0.5) * 0.6
            if abs(v) <= w:
                core.add((x, y))
            elif abs(v) <= w + 1.2:
                near.add((x, y))
    rim = {p for p in near if any((p[0] + dx, p[1] + dy) in core for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))}
    fringe = near - rim
    return core, rim, fringe


def void_top(stage: int) -> Image.Image:
    im = rgba()
    light = lit_field(63, 4, 0.3, 64)
    core, rim, fringe = _scar(stage)
    for y in range(16):
        for x in range(16):
            put(im, x, y, tone(VOID_GROUND, 0.2 + light[y][x] * 0.5))
    # Cracks running out from the rift's ends into the scorched ground.
    for crack in ([(13, 2), (15, 1)], [(2, 13), (0, 15)], [(12, 4), (14, 6)], [(4, 12), (2, 10)]):
        for (x, y) in line(crack):
            put(im, x, y, VOID_RIM[0])
    for (x, y) in fringe:
        put(im, x, y, VOID_RIM[0])
    for (x, y) in rim:
        put(im, x, y, VOID_RIM[2])
    for (x, y) in core:
        put(im, x, y, VOID_CORE)
    return im


def void_rim(stage: int, frame: int) -> Image.Image:
    im = rgba()
    core, rim, _ = _scar(stage)
    for (x, y) in rim:
        shimmer = 0.5 + 0.5 * math.sin((x + y) * 0.9 - frame * 1.57)
        put(im, x, y, tone(VOID_RIM, 0.45 + shimmer * 0.55))
    return im


def void_side() -> Image.Image:
    im = rgba()
    light = lit_field(65, 4, 0.35, 66)
    for y in range(16):
        for x in range(16):
            put(im, x, y, tone(VOID_GROUND, 0.25 + light[y][x] * 0.5))
    for vein in ([(4, 0), (5, 4), (3, 8)], [(11, 0), (10, 3), (12, 6), (11, 9)]):
        for (x, y) in line(vein):
            put(im, x, y, VOID_RIM[1])
    for x in range(16):
        put(im, x, 0, VOID_RIM[0])
    return im


# ============================================================== Star Glyph (arcane)

ARCANE = [hexc(h) for h in ("#5A1A54", "#9A3A90", "#E678DC", "#FFB8F5", "#FFE8FC")]
SCORCH = hexc("#1C0A1E")


def _glyph():
    """The glyph's strokes: a ring with eight ticks, and a five-pointed star inside it."""
    ring, marks = set(), set()
    for i in range(96):
        a = i / 96 * 2 * math.pi
        ring.add((int(round(7.5 + math.cos(a) * 6.4)), int(round(7.5 + math.sin(a) * 6.4))))
    for k in range(8):
        a = k / 8 * 2 * math.pi + math.pi / 8
        marks.add((int(round(7.5 + math.cos(a) * 5.0)), int(round(7.5 + math.sin(a) * 5.0))))
    star = []
    for k in range(5):
        a = -math.pi / 2 + k * 4 * math.pi / 5
        star.append((7.5 + math.cos(a) * 4.0, 7.5 + math.sin(a) * 4.0))
    lines = set(line(star + [star[0]]))
    return ring, marks, lines


def star_glyph() -> Image.Image:
    im = rgba()
    ring, marks, lines = _glyph()
    # A soft scorch where it burned in (only these semi-clear pixels; the strokes on top are solid).
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d <= 7.4:
                put(im, x, y, SCORCH, int(110 * (1 - d / 7.4) ** 0.6))
    for (x, y) in ring | marks | lines:
        put(im, x, y, ARCANE[1])
    put(im, 7, 7, ARCANE[2])
    put(im, 8, 8, ARCANE[2])
    return im


def star_glyph_glow(frame: int) -> Image.Image:
    im = rgba()
    ring, marks, lines = _glyph()
    for (x, y) in ring:
        a = math.atan2(y - 7.5, x - 7.5)
        # A brightening running round the ring.
        run = 0.5 + 0.5 * math.cos(a - frame / 4 * 2 * math.pi)
        put(im, x, y, tone(ARCANE, 0.5 + run * 0.5))
    for (x, y) in marks:
        put(im, x, y, ARCANE[3])
    for (x, y) in lines:
        put(im, x, y, ARCANE[2] if frame % 2 else ARCANE[3])
    put(im, 7, 7, ARCANE[4])
    put(im, 8, 8, ARCANE[4])
    return im


# ============================================================== Stilled Sand (time)

SAND = [hexc(h) for h in ("#7A5A2A", "#9C7A3A", "#B8944C", "#D4AE62", "#E8C87A", "#F6E0A0", "#FFF4D0")]
GRAINS = [(3, 4, 0), (9, 2, 1), (12, 7, 2), (6, 10, 3), (13, 13, 1), (2, 12, 2), (8, 6, 0)]


def stilled_sand() -> Image.Image:
    im = rgba()
    light = lit_field(71, 4, 0.5, 72)
    for y in range(16):
        for x in range(16):
            c = tone(SAND, 0.2 + light[y][x] * 0.6)
            if noise(x, y, 73) > 0.93:
                c = SAND[6]
            put(im, x, y, c)
    return im


def stilled_sand_glow(frame: int) -> Image.Image:
    im = rgba()
    for x, y, phase in GRAINS:
        if (frame + phase) % 4 < 2:
            put(im, x, y, SAND[6])
    return im


# ============================================================== Bloodmoss (blood)

BLOOD = [hexc(h) for h in ("#2A040A", "#4A0A14", "#6A1220", "#8E1A2A", "#B42634", "#D84650", "#FF8C94")]
DROPS = [(4, 3), (11, 5), (7, 10), (13, 12), (2, 13)]
VEINS = [[(0, 6), (3, 7), (6, 5), (9, 7), (12, 6), (15, 8)], [(6, 5), (5, 2), (7, 0)], [(9, 7), (10, 11), (8, 15)], [(10, 11), (14, 12)],
         [(3, 7), (2, 11), (4, 15)]]


def bloodmoss() -> Image.Image:
    im = rgba()
    light = lit_field(81, 4, 0.55, 82)
    for y in range(16):
        for x in range(16):
            put(im, x, y, tone(BLOOD, 0.2 + light[y][x] * 0.6))
    # Dark veins wind through the moss.
    for vein in VEINS:
        for (x, y) in line(vein):
            put(im, x, y, BLOOD[0])
    for (x, y) in DROPS:
        put(im, x, y, BLOOD[6])
    return im


def bloodmoss_glow(frame: int) -> Image.Image:
    im = rgba()
    # A heartbeat: two quick beats, then a rest.
    beat = [1.0, 0.3, 0.8, 0.0, 0.0, 0.0][frame % 6]
    if beat > 0:
        for (x, y) in DROPS:
            put(im, x, y, tone(BLOOD[3:], beat))
    return im


# ============================================================== reagents (items)

REAGENT_ART = {
    "cinder_ash": ("""
        ................
        ................
        ................
        ................
        ................
        ................
        ......oooo......
        ....ooaAAaoo....
        ...oaAAaEaado...
        ..oaAeaaaadddo..
        ..oaaaaadaeddo..
        .oaAaaEaadaddo..
        .oaaadaaaddedo..
        .oddddddddddddo.
        ..oooooooooooo..
        ................
    """, {"o": "#1A1414", "A": "#A39A93", "a": "#6D6662", "d": "#433D3B", "e": "#F06E32", "E": "#FFD878"}),
    "everfrost_shard": ("""
        ................
        ...........oo...
        ..........oWHo..
        .........oWHLo..
        ........oWHLLo..
        .......oWHLLdo..
        ......oWHLLdo...
        .....oWHLLdo....
        ....oWHLLdo.....
        ...oHHLLdo......
        ...oLLLdo.......
        ..oLLddo........
        ..oLddo.........
        ..oddo..........
        ...oo...........
        ................
    """, {"o": "#1E3A5A", "W": "#FFFFFF", "H": "#DDF1FB", "L": "#A2C8E2", "d": "#5E86A8"}),
    "fulgurite_shard": ("""
        ................
        ..........y.....
        .........yYy....
        ..........y.....
        .........oo.....
        ........oghd....
        .......oghdo....
        ......oghdo.....
        .....oghddo.....
        ....oghddo......
        ...oghddo.......
        ..ogsddo........
        ..osddo.........
        ..oddo..........
        ...oo...........
        ................
    """, {"o": "#14121C", "g": "#9894B0", "h": "#524D62", "d": "#26232F", "s": "#C2A676", "y": "#FFE650", "Y": "#FFFFFF"}),
    "bottled_gale": ("""
        ................
        ......oooo......
        ......occo......
        .......oo.......
        ......owwo......
        .....owwwwo.....
        ....owmWmwwo....
        ....omWwwWmo....
        ....owWmmWwo....
        ....owmWWmwo....
        ....owwmmwwo....
        ....owwwwwwo....
        .....owwwwo.....
        ......oooo......
        ................
        ................
    """, {"o": "#3A4A52", "c": "#8A6A44", "w": "#C8E4EC", "m": "#9FE8C8", "W": "#F4FFFA"}),
    "geode_grit": ("""
        ................
        ................
        ................
        ................
        ................
        .......oo.......
        ......oAyo......
        .....oaAYao.....
        ....oaaayaao....
        ...oaAaaaadao...
        ..oaayaAaddado..
        ..oaYaaaadyddo..
        .oaaaadaadddddo.
        .oddddddddddddo.
        ..oooooooooooo..
        ................
    """, {"o": "#241A12", "A": "#BCAC9A", "a": "#847566", "d": "#564A3F", "y": "#FF9A30", "Y": "#FFE8B0"}),
    "wildbloom_petal": ("""
        ................
        ................
        ..........ooo...
        ........ooPPPo..
        .......oPPWWPo..
        ......oPPWWPPo..
        .....oPPWWPPpo..
        ....oPPWWPPpo...
        ...oPPWPPPppo...
        ...oPWPPPppo....
        ..oPWPPpppo.....
        ..oWPPppoo......
        ..oPppoo........
        ..ogoo..........
        ..oo............
        ................
    """, {"o": "#5A1A40", "P": "#FF9CD8", "W": "#FFE0F4", "p": "#D060A8", "g": "#58A040"}),
    "hollow_dust": ("""
        ................
        ................
        .....*..........
        ....*+*......*..
        .....*......*+*.
        .............*..
        .......oo.......
        ......ovvo......
        .....ovVvdo.....
        ....ovVvvddo....
        ...ovvvvdvddo...
        ..ovVvvdvddddo..
        ..ovvvdvdddddo..
        .oddddddddddddo.
        ..oooooooooooo..
        ................
    """, {"o": "#0A0412", "v": "#6A2AA8", "V": "#B45AF0", "d": "#2A1440", "*": "#E0B0FF", "+": "#FFFFFF"}),
    "star_dust": ("""
        ................
        .......+........
        ......+*+.......
        ....+..+..+.....
        ...+*+...+*+....
        ....+.....+.....
        .......p........
        ......pPp...+...
        .....pPWPp.+*+..
        ......pPp...+...
        .......p........
        ...+........+...
        ..+*+......+*+..
        ...+........+...
        ................
        ................
    """, {"P": "#E678DC", "W": "#FFFFFF", "p": "#9A3A90", "+": "#FFB8F5", "*": "#FFFFFF"}),
    "hourglass_sand": ("""
        ................
        .......y........
        .......Y........
        .......y........
        .......Y........
        .......y........
        ......oyo.......
        .....oAYAo......
        ....oAAyAao.....
        ...oAAaAaaao....
        ..oAaAaaadaao...
        ..oaAaaadaadao..
        .oaaaadaaadddao.
        .oddddddddddddo.
        ..oooooooooooo..
        ................
    """, {"o": "#3A2810", "A": "#F6E0A0", "a": "#D4AE62", "d": "#9C7A3A", "y": "#FFF4D0", "Y": "#E8C87A"}),
    "sanguine_bead": ("""
        ................
        ................
        ................
        .....oooooo.....
        ....oRRrrrdo....
        ...oRWRrrrrdo...
        ..oRWWRrrrrddo..
        ..oRWRrrrrrddo..
        ..orRrrrrrdddo..
        ..orrrrrrrdddo..
        ..orrrrrrddddo..
        ...ordrrddddo...
        ....odddddo.....
        .....oooooo.....
        ................
        ................
    """, {"o": "#2A0408", "R": "#FF6474", "W": "#FFD0D4", "r": "#D2283C", "d": "#7A0A18"}),
}


def reagent_icon(path: str) -> Image.Image:
    art, pal = REAGENT_ART[path]
    cv = Canvas()
    blit(cv, art, {k: hexc(v) for k, v in pal.items()})
    return cv.image()


# ============================================================== models

def _faces(texture: str, uv_side=(0, 14, 16, 16), cull=True, down=True):
    faces = {"up": {"uv": [0, 0, 16, 16], "texture": texture}}
    if down:
        faces["down"] = {"uv": [0, 0, 16, 16], "texture": texture, **({"cullface": "down"} if cull else {})}
    for d in ("north", "south", "west", "east"):
        faces[d] = {"uv": list(uv_side), "texture": texture, **({"cullface": d} if cull else {})}
    return faces


def _plane(y: float, texture: str, light: int):
    """A flat, lit overlay lying just over a surface (only its top face)."""
    return {"from": [0, y, 0], "to": [16, y, 16], "light_emission": light,
            "faces": {"up": {"uv": [0, 0, 16, 16], "texture": texture}}}


def _cross(texture: str, light: int = 0, inset: float = 0.8, height: float = 16):
    elements = []
    for a, b, faces in (((inset, 0, 8), (16 - inset, height, 8), ("north", "south")), ((8, 0, inset), (8, height, 16 - inset), ("west", "east"))):
        e = {"from": list(a), "to": list(b), "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True},
             "shade_direction_override": "up", "faces": {f: {"uv": [0, 0, 16, 16], "texture": texture} for f in faces}}
        if light:
            e["light_emission"] = light
        elements.append(e)
    return elements


def _cube(side: str, top: str, bottom: str):
    return {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
        "down": {"texture": bottom, "cullface": "down"}, "up": {"texture": top, "cullface": "up"},
        **{d: {"texture": side, "cullface": d} for d in ("north", "south", "west", "east")}}}


def models(tex: str) -> dict:
    """Model JSON for every residue block (the void scar has one per stage)."""
    t = lambda name: f"wildercord:block/residue/{name}"  # noqa: E731
    out = {
        "smouldering_ash": {"parent": "minecraft:block/thin_block", "textures": {"particle": t("smouldering_ash"), "ash": t("smouldering_ash"),
                            "embers": t("smouldering_ash_embers")},
                            "elements": [{"from": [0, 0, 0], "to": [16, 2, 16], "faces": _faces("#ash")}, _plane(2.02, "#embers", 13)]},
        "everfrost": {"parent": "minecraft:block/block", "textures": {"particle": t("everfrost_top"), "top": t("everfrost_top"), "side": t("everfrost_side"),
                      "glint": t("everfrost_glint")},
                      "elements": [_cube("#side", "#top", "#side"), _plane(16.01, "#glint", 9)]},
        "fulgurite": {"ambientocclusion": False, "textures": {"particle": t("fulgurite"), "glass": t("fulgurite"), "glow": t("fulgurite_glow")},
                      "elements": _cross("#glass", 0, 1.5, 13) + _cross("#glow", 12, 1.45, 13)},
        "lingering_eddy": {"ambientocclusion": False, "textures": {"particle": t("lingering_eddy"), "wind": t("lingering_eddy")},
                           "elements": _cross("#wind", 6, 1.0, 16)},
        "riven_stone": {"parent": "minecraft:block/block", "textures": {"particle": t("riven_stone_top"), "top": t("riven_stone_top"),
                        "side": t("riven_stone_side"), "veins": t("riven_stone_veins"), "side_veins": t("riven_stone_side_veins")},
                        "elements": [_cube("#side", "#top", "#side"), _plane(16.01, "#veins", 10),
                                     {"from": [-0.01, 0, -0.01], "to": [16.01, 16, 16.01], "light_emission": 10, "faces": {
                                         d: {"texture": "#side_veins", "cullface": d} for d in ("north", "south", "west", "east")}}]},
        "wildbloom": {"ambientocclusion": False, "textures": {"particle": t("wildbloom"), "flower": t("wildbloom"), "glow": t("wildbloom_glow")},
                      "elements": _cross("#flower", 0, 0.8, 16) + _cross("#glow", 11, 0.75, 16)},
        "star_glyph": {"parent": "minecraft:block/thin_block", "textures": {"particle": t("star_glyph"), "glyph": t("star_glyph"), "glow": t("star_glyph_glow")},
                       "elements": [_plane(0.25, "#glyph", 0), _plane(0.3, "#glow", 15)]},
        "stilled_sand": {"parent": "minecraft:block/thin_block", "textures": {"particle": t("stilled_sand"), "sand": t("stilled_sand"), "glow": t("stilled_sand_glow")},
                         "elements": [{"from": [1, 0, 1], "to": [15, 3, 15], "faces": _faces("#sand", (1, 13, 15, 16), cull=False)},
                                      {"from": [3, 3, 3], "to": [13, 4, 13], "faces": _faces("#sand", (3, 12, 13, 13), cull=False, down=False)},
                                      _plane(4.02, "#glow", 8)]},
        "bloodmoss": {"parent": "minecraft:block/thin_block", "textures": {"particle": t("bloodmoss"), "moss": t("bloodmoss"), "glow": t("bloodmoss_glow")},
                      "elements": [{"from": [0, 0, 0], "to": [16, 1, 16], "faces": _faces("#moss", (0, 15, 16, 16))}, _plane(1.02, "#glow", 7)]},
    }
    for stage in range(4):
        out[f"void_scar_{stage}"] = {"parent": "minecraft:block/block", "textures": {
            "particle": t("void_scar_side"), "top": t(f"void_scar_top_{stage}"), "side": t("void_scar_side"), "rim": t(f"void_scar_rim_{stage}")},
            "elements": [_cube("#side", "#top", "#side"), _plane(16.01, "#rim", 11)]}
    return out


def blockstates() -> dict:
    states = {}
    for path, *_ in RESIDUES:
        if path == "void_scar":
            states[path] = {"variants": {f"stage={s}": {"model": f"wildercord:block/residue/void_scar_{s}"} for s in range(4)}}
        elif path in ("fulgurite", "star_glyph", "riven_stone", "smouldering_ash", "bloodmoss"):
            # Turned at random from one spot to the next, so a patch never looks stamped.
            states[path] = {"variants": {"": [{"model": f"wildercord:block/residue/{path}", **({"y": r} if r else {})} for r in (0, 90, 180, 270)]}}
        else:
            states[path] = {"variants": {"": {"model": f"wildercord:block/residue/{path}"}}}
    return states


# ============================================================== text

ALTAR = {
    "cinder_ash": "At the Fusion Altar, tempers a fusion: it keeps the higher rank of the two runes, not the lower",
    "everfrost_shard": "At the Fusion Altar, stills a fusion: half the XP levels",
    "fulgurite_shard": "At the Fusion Altar, charges a fusion: a rank I result comes out rank II",
    "bottled_gale": "At the Fusion Altar, unbinds a signature pair: it makes its elements' fusion instead",
    "geode_grit": "At the Fusion Altar, grounds a fusion: the amethyst stays on the altar",
    "wildbloom_petal": "At the Fusion Altar, a fusion blooms twice: two of the result",
    "hollow_dust": "At the Fusion Altar, hollows a fusion: the lower-tier rune stays on the altar",
    "star_dust": "At the Fusion Altar, exalts a fusion: one rank higher, for 3 more XP levels",
    "hourglass_sand": "At the Fusion Altar, a fusion you've made before costs no XP",
    "sanguine_bead": "At the Fusion Altar, pays up to 6 of the XP levels in health, half a heart each",
}
USE = {
    "cinder_ash": "Burns in a furnace: four items, a quarter faster",
    "everfrost_shard": "Use on still water to freeze it into ice, or on still lava to cool it into obsidian",
    "fulgurite_shard": "Use on copper to scrape off a stage of weathering",
    "bottled_gale": "Drink: you fall slowly for 30 seconds",
    "geode_grit": "Use: the ore around you glows through the rock for 10 seconds",
    "wildbloom_petal": "Use on a plant: it grows, as bone meal makes it",
    "hollow_dust": "Use: every loose item and orb within 10 blocks comes to you",
    "star_dust": "Use on a block: a mote of starlight hangs beside it for five minutes",
    "hourglass_sand": "Use on a lit furnace: it jumps ten seconds ahead. On a young animal: it grows two minutes older",
    "sanguine_bead": "Use on nether wart: it grows a stage",
}

LANG = {
    **{f"block.wildercord.{path}": block for path, _, _, _, block, _ in RESIDUES},
    **{f"item.wildercord.{reagent}": name for _, _, _, reagent, _, name in RESIDUES},
    **{f"item.wildercord.{k}.altar": v for k, v in ALTAR.items()},
    **{f"item.wildercord.{k}.use": v for k, v in USE.items()},
    "tag.item.wildercord.reagents": "Reagents",
    # The altar's panel, with a reagent on it.
    "screen.wildercord.altar.reagent": "With %s: %s",
    "screen.wildercord.altar.reagent.tempered": "keeps the higher rank",
    "screen.wildercord.altar.reagent.stilled": "half the XP",
    "screen.wildercord.altar.reagent.charged": "comes out rank II",
    "screen.wildercord.altar.reagent.unbound": "its elements' fusion instead",
    "screen.wildercord.altar.reagent.grounded": "the amethyst stays",
    "screen.wildercord.altar.reagent.bountiful": "two of the result",
    "screen.wildercord.altar.reagent.hollowed": "the lower-tier rune stays",
    "screen.wildercord.altar.reagent.exalted": "one rank higher, 3 more XP",
    "screen.wildercord.altar.reagent.familiar": "no XP: you've made it before",
    "screen.wildercord.altar.reagent.bloodbound": "%s XP levels paid in health",
    # Places and times of power: the climate's new conditions, and the HUD's lines.
    "climate.wildercord.ley_crossing": "A ley crossing",
    "climate.wildercord.full_moon": "Full moon",
    "climate.wildercord.new_moon": "New moon",
    "climate.wildercord.noon": "Noon sun",
    "climate.wildercord.dawn": "Dawn",
    "climate.wildercord.dusk": "Dusk",
    "element.wildercord.all": "Every element",
    "hud.wildercord.climate_line": "%s: %s",
    "hud.wildercord.crossing_cost": "%s, spells %s%% cheaper",
    "message.wildercord.ley_crossing": "A ley crossing: every spell is stronger and cheaper here",
    "message.wildercord.ley_crossing_first": "Two ley lines cross beneath you. Where they meet, every spell you cast is a little stronger and costs a little less.",
}


# ============================================================== writing

def write(g):
    tex = g.ASSETS / "textures/block/residue"
    save_png(g, ash_top(), tex / "smouldering_ash.png")
    save_png(g, frames(ash_embers, 8), tex / "smouldering_ash_embers.png", 3, True)
    save_png(g, everfrost_top(), tex / "everfrost_top.png")
    save_png(g, everfrost_side(), tex / "everfrost_side.png")
    save_png(g, frames(everfrost_glint, 4), tex / "everfrost_glint.png", 5)
    save_png(g, fulgurite(), tex / "fulgurite.png")
    save_png(g, frames(fulgurite_glow, 6), tex / "fulgurite_glow.png", 2)
    save_png(g, frames(eddy, EDDY_FRAMES), tex / "lingering_eddy.png", 2, True)
    save_png(g, riven_top(), tex / "riven_stone_top.png")
    save_png(g, riven_side(), tex / "riven_stone_side.png")
    save_png(g, frames(lambda f: riven_veins(CRACKS_TOP, f), 8), tex / "riven_stone_veins.png", 3, True)
    save_png(g, frames(lambda f: riven_veins(CRACKS_SIDE, f), 8), tex / "riven_stone_side_veins.png", 3, True)
    save_png(g, wildbloom(), tex / "wildbloom.png")
    save_png(g, frames(wildbloom_glow, 4), tex / "wildbloom_glow.png", 8, True)
    save_png(g, void_side(), tex / "void_scar_side.png")
    for stage in range(4):
        save_png(g, void_top(stage), tex / f"void_scar_top_{stage}.png")
        save_png(g, frames(lambda f, s=stage: void_rim(s, f), 4), tex / f"void_scar_rim_{stage}.png", 3, True)
    save_png(g, star_glyph(), tex / "star_glyph.png")
    save_png(g, frames(star_glyph_glow, 4), tex / "star_glyph_glow.png", 4, True)
    save_png(g, stilled_sand(), tex / "stilled_sand.png")
    save_png(g, frames(stilled_sand_glow, 4), tex / "stilled_sand_glow.png", 6)
    save_png(g, bloodmoss(), tex / "bloodmoss.png")
    save_png(g, frames(bloodmoss_glow, 6), tex / "bloodmoss_glow.png", 3, True)

    for name, model in models(str(tex)).items():
        g.write_json(g.ASSETS / f"models/block/residue/{name}.json", model)
    for path, state in blockstates().items():
        g.write_json(g.ASSETS / f"blockstates/{path}.json", state)

    for path, element, placement, reagent, _, _ in RESIDUES:
        save_png(g, reagent_icon(reagent), g.ASSETS / f"textures/item/{reagent}.png")
        g.item_model(reagent, reagent)
        g.write_json(g.ASSETS / f"items/{reagent}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{reagent}"}})
        # Every residue gives its reagent, however it's harvested (a pinch more from the strongest-looking ground ones).
        count = {"type": "minecraft:uniform", "min": 1, "max": 2} if placement == "cover" or path in ("smouldering_ash", "wildbloom", "stilled_sand") else 1
        g.write_json(g.DATA / f"loot_table/blocks/{path}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
            {"type": "minecraft:item", "name": f"wildercord:{reagent}",
             **({"functions": [{"function": "minecraft:set_count", "count": count}]} if count != 1 else {})}]}]})

    g.write_json(g.DATA / "tags/block/residue_ground.json", {"values": RESIDUE_GROUND})
    g.write_json(g.DATA / "tags/item/reagents.json", {"values": [f"wildercord:{r[3]}" for r in RESIDUES]})
    # Bees visit wildblooms as they would any flower.
    g.write_json(g.RES / "data/minecraft/tags/block/bee_attractive.json", {"replace": False, "values": ["wildercord:wildbloom"]})
    g.write_json(g.RES / "data/minecraft/tags/block/mineable/shovel.json", {"replace": False, "values": [
        "wildercord:smouldering_ash", "wildercord:stilled_sand", "wildercord:void_scar"]})
    g.write_json(g.RES / "data/minecraft/tags/block/mineable/hoe.json", {"replace": False, "values": ["wildercord:bloodmoss"]})


RESIDUE_PICKAXE = ["wildercord:everfrost", "wildercord:fulgurite", "wildercord:riven_stone"]


def preview(out_dir: str) -> str:
    """A contact sheet of every residue face and reagent, 8x, for review."""
    tiles = [ash_top(), ash_embers(0), everfrost_top(), everfrost_side(), everfrost_glint(0), fulgurite(), fulgurite_glow(0), eddy(0),
             riven_top(), riven_side(), riven_veins(CRACKS_TOP, 0), wildbloom(), wildbloom_glow(0), void_side(),
             *[void_top(s) for s in range(4)], void_rim(0, 0), star_glyph(), star_glyph_glow(0), stilled_sand(), stilled_sand_glow(0),
             bloodmoss(), bloodmoss_glow(0), *[reagent_icon(r[3]) for r in RESIDUES]]
    cols = 8
    rows = (len(tiles) + cols - 1) // cols
    scale = 8
    sheet = Image.new("RGBA", (cols * 16 * scale + (cols + 1) * 4, rows * 16 * scale + (rows + 1) * 4), (60, 56, 70, 255))
    for i, tile in enumerate(tiles):
        x = 4 + (i % cols) * (16 * scale + 4)
        y = 4 + (i // cols) * (16 * scale + 4)
        backing = Image.new("RGBA", (16, 16), (30, 28, 36, 255))
        backing.alpha_composite(tile)
        sheet.paste(backing.resize((16 * scale, 16 * scale), Image.NEAREST), (x, y))
    out = Path(out_dir) / "residue_art.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    return str(out)


if __name__ == "__main__":
    print(preview(str(ROOT / "build/art-preview")))
