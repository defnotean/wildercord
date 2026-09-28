"""Hand-tuned art for Wildercord's world content: the Spell Scroll, Torn Page and Training
Dummy items, the Wellstone, the Rune Seals, the Archive Lectern and the two entity skins.

Same house style as item_art.py: ASCII pictograms for the 16x16 items, ramps lit from the
top-left, no anti-aliasing. Block faces are built from small deterministic rules so they
tile cleanly; the 64x64 skins are painted face by face on the vanilla box-UV layout.

Public API (imported by generate_assets.py):
    spell_scroll_icon() -> Image                (16x16 item)
    torn_page_icon() -> Image                   (16x16 item)
    training_dummy_icon() -> Image              (16x16 item)
    wellstone_textures() -> dict                (16x16 faces; the "_active" ones are 8-frame lists)
    rune_seal_textures() -> dict                (element -> (unlit, lit), 16x16 each)
    archive_lectern_textures() -> dict          ("top", "side", "front", "bottom")
    archivist_texture() -> Image                (64x64, vanilla illager layout)
    dummy_texture() -> Image                    (64x64, DummyModel layout)

Run this file directly to render a review contact sheet into build/art-preview/world_art.png.
"""
from __future__ import annotations

import math
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from item_art import Canvas, blit, grid, hexc, mix, ramp  # noqa: E402

WELL_FRAMES = 8          # the lit Wellstone faces loop over this many frames

# ============================================================== helpers


def noise(x: int, y: int, seed: int = 0) -> float:
    """Deterministic per-pixel hash in [0, 1)."""
    h = (x * 374761393 + y * 668265263 + seed * 2246822519) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFF) / 65536


def rect(cv: Canvas, x0: int, y0: int, w: int, h: int, col):
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            cv.put(x, y, col)


def sprite(text: str, pal: dict, size: int = 16) -> Canvas:
    cv = Canvas(size)
    blit(cv, text, pal)
    return cv


def cells(text: str):
    """(x, y, ch) for every non-'.' cell of an ASCII block."""
    for y, row in enumerate(grid(text)):
        for x, ch in enumerate(row):
            if ch != ".":
                yield x, y, ch


# ============================================================== items

SCROLL_PAL = {
    "o": hexc("#3A2814"), "d": hexc("#9C7B4C"), "m": hexc("#CFB07A"), "l": hexc("#E9D5A2"),
    "h": hexc("#F8EDCC"), "e": hexc("#E2C890"), "s": hexc("#7A5630"),
    "x": hexc("#3C1F72"), "v": hexc("#6B3DB8"), "V": hexc("#9C70EA"),
    "w": hexc("#5A2A9A"), "W": hexc("#8E4FE0"), "S": hexc("#D8BCFF"),
    "*": hexc("#FFFFFF"), "+": hexc("#CDB2FF"),
}

# A rolled parchment tied with a violet ribbon and a wax seal, and a spark of magic.
SPELL_SCROLL = """
    ................
    ...+......oo....
    ..+*+....oeeo...
    ...+....ohesdo..
    .......ohleesdo.
    ......ohllmdddo.
    .....oVllmmddo..
    ....oVxWWmddo...
    ...ohlWSWxdo....
    ..ohllwWwxo.....
    .ohllmmxvo......
    .oelmmddvo......
    .odemddoxv......
    ..oddoo..x......
    ...oo...........
    ................
"""


def spell_scroll_icon() -> Image.Image:
    """16x16 Spell Scroll item."""
    return sprite(SPELL_SCROLL, SCROLL_PAL).image()


PAGE_PAL = {
    "o": hexc("#5A4022"), "P": hexc("#F4E8C4"), "p": hexc("#E2CE9C"), "q": hexc("#CBB07A"),
    "Q": hexc("#AE9058"), "f": hexc("#FFF8E4"), "k": hexc("#5E4A34"), "K": hexc("#94805C"),
    "v": hexc("#7446C4"), "V": hexc("#B690FF"),
}

# A yellowed page torn from an old spellbook: ragged lower-right edge (with pale fibres),
# faded lines of writing and a small violet sigil.
TORN_PAGE = """
    ................
    ..ooooooooooo...
    ..oPPPPPPPPPqo..
    ..oPkKpkkKpqqo..
    ..oPpppppppqpo..
    ..oPKkkpkKkpqo..
    ..oPpppppppqqo..
    ..oPkkpKkkpffo..
    ..oPpppppppfo...
    ..oPpVpkKqfo....
    ..oPVvVpqQqfo...
    ..oPpVpkqfoo....
    ..oPpppqfo......
    ..oqQpffo.......
    ..ooooo.........
    ................
"""


def torn_page_icon() -> Image.Image:
    """16x16 Torn Page item."""
    return sprite(TORN_PAGE, PAGE_PAL).image()


DUMMY_PAL = {
    "o": hexc("#2E2014"),
    # burlap head
    "B": hexc("#C8A474"), "b": hexc("#A88258"), "n": hexc("#7E5E3A"),
    # painted target
    "R": hexc("#C8302C"), "r": hexc("#8E1E1E"), "W": hexc("#F2ECE0"), "w": hexc("#CFC6B4"),
    # straw
    "S": hexc("#F0D686"), "s": hexc("#D8B45A"), "z": hexc("#A8843A"), "Z": hexc("#7E6024"),
    # rope
    "C": hexc("#B8945E"), "c": hexc("#7C5C34"),
    # pale pole wood, dark base wood
    "L": hexc("#DDB883"), "l": hexc("#C49A62"), "j": hexc("#946C3E"),
    "E": hexc("#74522F"), "D": hexc("#5A3E24"), "d": hexc("#3E2A18"),
}

# A straw training dummy: a burlap sack head painted with a target, a roped straw body,
# a crossbar with straw hands, all on a pole in a dark wooden foot.
TRAINING_DUMMY = """
    ....ooooooooo...
    ....oBBRRRbno...
    ....oBRWWWRno...
    ....obRWRWRno...
    ....obRwwwRno...
    ....obnRRRnno...
    .....ooCcCco....
    .oo..oSSSszo..oo
    oSzooSsSszzooSzo
    oszLLccCccclLjzo
    .ooooSsSszzoooo.
    .....oSzSzzo....
    ......oljo......
    ...ooooljoooooo.
    ...oEEEDDDDDDdo.
    ....oooooooooo..
"""


def training_dummy_icon() -> Image.Image:
    """16x16 Training Dummy item."""
    return sprite(TRAINING_DUMMY, DUMMY_PAL).image()


# ============================================================== blocks: shared stone

# polished deepslate, darkest..lightest
DS = ramp("#141418", "#1C1C22", "#24242B", "#2D2D35", "#373740", "#43434D", "#52525D", "#65656F")
# amethyst, darkest..lightest
AM = ramp("#2A1650", "#43267A", "#63409E", "#8563C4", "#A887E0", "#CCB0F5", "#EDE0FF")
# channel glow by level 0..1: dead groove -> faint violet -> bright violet -> white
GLOW = ramp("#1E1530", "#35245A", "#4E3584", "#7A52C8", "#A874F6", "#CBA6FF", "#EBDDFF", "#FFFFFF")
GLOW_DIM = mix(GLOW[1], GLOW[2], 0.5)       # a sleeping channel: only just violet


def glow_at(level: float):
    """Colour on the GLOW ramp for a level in 0..1 (stepped, never blended off-ramp)."""
    i = max(0.0, min(1.0, level)) * (len(GLOW) - 1)
    return GLOW[int(round(i))]


def stone_fill(cv: Canvas, tones, seed: int, lo: int, hi: int):
    """Fill with tones[lo..hi]: mostly the middle tone, with sparse lighter and darker specks."""
    mid = (lo + hi) // 2
    for y in range(cv.n):
        for x in range(cv.n):
            r = noise(x, y, seed)
            t = lo if r < 0.10 else hi if r > 0.90 else mid
            cv.put(x, y, tones[t])


def bevel(cv: Canvas, tones, x0=0, y0=0, w=16, h=16, light=6, dark=1):
    """A 1px raised edge: lit along the top and left, shaded along the bottom and right."""
    for x in range(x0, x0 + w):
        cv.put(x, y0, tones[light])
        cv.put(x, y0 + h - 1, tones[dark])
    for y in range(y0, y0 + h):
        cv.put(x0, y, tones[light])
        cv.put(x0 + w - 1, y, tones[dark])
    cv.put(x0 + w - 1, y0, tones[(light + dark) // 2])
    cv.put(x0, y0 + h - 1, tones[(light + dark) // 2])


def recess(cv: Canvas, tones, x0, y0, w, h, shadow=1, lit=5):
    """A 1px sunken edge: shadow along the top and left, light along the bottom and right."""
    for x in range(x0, x0 + w):
        cv.put(x, y0, tones[shadow])
        cv.put(x, y0 + h - 1, tones[lit])
    for y in range(y0, y0 + h):
        cv.put(x0, y, tones[shadow])
        cv.put(x0 + w - 1, y, tones[lit])


def carve(cv: Canvas, groove: dict, colour_of, bleed=None):
    """Cut a groove: every groove pixel takes colour_of(pos, value); its lower/right lip
    catches the light and its upper/left lip falls into shadow. bleed = (colour, amount)
    tints the stone round the groove (light spilling out of a lit channel)."""
    lips = {}
    for (x, y) in groove:
        for dx, dy, sign in ((1, 0, 1), (0, 1, 1), (-1, 0, -1), (0, -1, -1)):
            q = (x + dx, y + dy)
            if q not in groove and cv.inside(*q):
                lips[q] = max(lips.get(q, -1), sign)
    for q, sign in lips.items():
        base = cv.get(*q)
        base = mix(base, (255, 255, 255), 0.12) if sign > 0 else mix(base, (0, 0, 0), 0.25)
        if bleed is not None:
            base = mix(base, bleed[0], bleed[1])
        cv.put(*q, base)
    for p, v in groove.items():
        cv.put(*p, colour_of(p, v))


def path_distance(pixels: set, sources) -> dict:
    """8-connected walking distance of every pixel from the nearest source pixel."""
    dist = {p: 0 for p in sources if p in pixels}
    frontier = list(dist)
    while frontier:
        nxt = []
        for (x, y) in frontier:
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    q = (x + dx, y + dy)
                    if q in pixels and q not in dist:
                        dist[q] = dist[(x, y)] + 1
                        nxt.append(q)
        frontier = nxt
    return dist


def pulse(d: float, frame: int, wavelength: float = 16.0, width: float = 3.0) -> float:
    """0..1 brightness of a pulse that travels `wavelength` px per loop, sampled at distance d."""
    head = frame / WELL_FRAMES * wavelength
    off = (d - head) % wavelength
    off = min(off, wavelength - off)
    return max(0.0, 1.0 - off / width)


# ============================================================== the Wellstone

# Rune channels cut into the side: '#' channel, 'n' the amethyst heart. The top run meets
# the basin's channel over the rim, and the side branches wrap round onto the next face.
WELL_SIDE_CHANNELS = """
    .......##.......
    .......##.......
    .......##.......
    .......##.......
    .......##.......
    ......#..#......
    .....#....#.....
    ######.nn.######
    ######.nn.######
    .....#....#.....
    ......#..#......
    .......##.......
    .......##.......
    ................
    ................
    ................
"""


def _well_base(seed: int) -> Canvas:
    """Polished deepslate with a raised rim and a thin amethyst inlay framing the face."""
    cv = Canvas()
    stone_fill(cv, DS, seed, 3, 5)
    bevel(cv, DS, light=6, dark=1)
    for i in range(2, 14):                  # the inlay, lit along its top and left runs
        cv.put(i, 2, AM[3])
        cv.put(2, i, AM[3])
        cv.put(i, 13, AM[1])
        cv.put(13, i, AM[1])
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):   # a crystal set in each corner
        cv.put(x, y, AM[5])
    cv.put(2, 2, AM[6])
    cv.put(13, 13, AM[3])
    recess(cv, DS, 3, 3, 10, 10, shadow=2, lit=5)
    return cv


def _well_side(frame: int | None) -> Canvas:
    cv = _well_base(11)
    chan = {(x, y) for x, y, ch in cells(WELL_SIDE_CHANNELS) if ch == "#"}
    heart = [(x, y) for x, y, ch in cells(WELL_SIDE_CHANNELS) if ch == "n"]
    dist = path_distance(chan, [(7, 0), (8, 0)])
    for p in chan:
        dist.setdefault(p, 7)
    if frame is None:
        carve(cv, dist, lambda p, d: GLOW_DIM)
        for p in heart:
            cv.put(*p, AM[2])
        cv.put(7, 7, AM[4])
        return cv
    carve(cv, dist, lambda p, d: glow_at(0.6 + 0.4 * pulse(d + 5, frame)), bleed=(GLOW[4], 0.2))
    beat = pulse(12, frame, width=5)
    for p in heart:
        cv.put(*p, glow_at(0.7 + 0.3 * beat))
    cv.put(7, 7, GLOW[7])
    return cv


def _well_top(frame: int | None) -> Canvas:
    cv = _well_base(23)
    c = 7.5
    rim, pool = {}, set()
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - c, y - c)
            if r <= 4.6:
                if r > 3.4:
                    rim[(x, y)] = (x - c) + (y - c)        # > 0: the far wall, facing the light
                else:
                    pool.add((x, y))
    # four channels running from the basin out to the middle of each edge
    chan = {}
    for i in range(0, 4):
        for j in (7, 8):
            for p in ((j, i), (j, 15 - i), (i, j), (15 - i, j)):
                chan[p] = 3 - i
    for p, f in rim.items():
        cv.put(*p, DS[6] if f > 2.0 else DS[1] if f < -2.0 else DS[3])
    sigil = [(7, 5), (8, 5), (5, 7), (5, 8), (10, 7), (10, 8), (7, 10), (8, 10),
             (6, 6), (9, 6), (6, 9), (9, 9)]
    core = [(7, 7), (8, 7), (7, 8), (8, 8)]
    if frame is None:
        for p in pool:
            cv.put(*p, hexc("#120C1E"))
        carve(cv, chan, lambda p, d: GLOW_DIM)
        for p in sigil:
            cv.put(*p, GLOW_DIM)
        for p in core:
            cv.put(*p, GLOW[2])
        return cv
    beat = pulse(0, frame, width=5)
    for p in pool:
        cv.put(*p, mix(hexc("#24163E"), GLOW[3], 0.4 * beat))
    carve(cv, chan, lambda p, d: glow_at(0.6 + 0.4 * pulse(d + 1, frame)), bleed=(GLOW[4], 0.2))
    for p in sigil:
        cv.put(*p, glow_at(0.6 + 0.4 * beat))
    for p in core:
        cv.put(*p, glow_at(0.86 + 0.14 * beat))
    return cv


def _well_bottom() -> Canvas:
    cv = _well_base(37)
    for p in ((7, 7), (8, 7), (7, 8), (8, 8)):
        cv.put(*p, AM[2])
    cv.put(7, 7, AM[4])
    return cv


def wellstone_textures() -> dict:
    """16x16 faces; the two '_active' entries are 8-frame lists (a pulse rolling down the channels)."""
    return {
        "wellstone_side": _well_side(None).image(),
        "wellstone_top": _well_top(None).image(),
        "wellstone_bottom": _well_bottom().image(),
        "wellstone_side_active": [_well_side(f).image() for f in range(WELL_FRAMES)],
        "wellstone_top_active": [_well_top(f).image() for f in range(WELL_FRAMES)],
    }


# ============================================================== Rune Seals

# dark deepslate tile, darkest..lightest
DT = ramp("#0E0E12", "#15151A", "#1C1C22", "#24242B", "#2D2D35", "#383841", "#45454F")

SEAL_COLOURS = {
    "fire": "#F06E32", "frost": "#8CDCFF", "storm": "#FFE650", "wind": "#C8F0DC", "earth": "#B48C5A",
    "life": "#6EDC64", "void": "#B45AF0", "arcane": "#E678DC", "time": "#F2D98A", "blood": "#D2283C",
}

# Carved glyphs, 9x9 at most: '#' groove, '+' bright groove, '*' core, '-' shallow fill.
SEAL_GLYPHS = {
    "fire": """
        ....+....
        ...+#....
        ...##+...
        ..##+#+..
        .#+#*##+.
        .##***##.
        .##*+*##.
        ..#***#..
        ...###...
    """,
    "frost": """
        ....+....
        .+..#..+.
        ..#.#.#..
        ...#*#...
        +##*+*##+
        ...#*#...
        ..#.#.#..
        .+..#..+.
        ....+....
    """,
    "storm": """
        .....+##.
        ....+##..
        ...+##...
        ..+#####.
        ....##-..
        ...##-...
        ..##-....
        .#-......
    """,
    "wind": """
        .....##..
        ....#..#.
        .......#.
        ######+..
        .........
        .#######+
        .........
        ####+#...
        ......#..
    """,
    "earth": """
        ....#....
        ...#+#...
        ...#-#...
        ..#---#..
        ..#-#-#..
        .#-#+#-#.
        .#-----#.
        #########
    """,
    "life": """
        ......###
        ....##--#
        ...#---+#
        ..#---+-#
        .#---+--#
        .#--+--#.
        .#-+--#..
        ..+##....
        .+.......
    """,
    "void": """
        ..#####..
        .#.....#.
        #..###..#
        #.#...#.#
        #.#.*##.#
        #.#.....#
        #..#...#.
        .#..###..
        ..#......
    """,
    "arcane": """
        ....+....
        ...###...
        .##---##.
        #--###--#
        +--#*#--+
        #--###--#
        .##---##.
        ...###...
        ....+....
    """,
    "time": """
        #######
        .#+++#.
        ..#+#..
        ...#...
        ..#-#..
        .#-*-#.
        #######
    """,
    "blood": """
        ....#....
        ...#+#...
        ...#+#...
        ..#-+-#..
        .#-+---#.
        .#-+---#.
        .#-----#.
        ..#---#..
        ...###...
    """,
}


def _seal_base(seed: int) -> tuple[Canvas, dict, set]:
    """Deepslate tiles (seams on the left/top edges and through the middle, so a wall of
    seals reads as one tiled door) with a round carved medallion in the centre."""
    cv = Canvas()
    stone_fill(cv, DT, seed, 3, 5)
    for y in range(16):
        for x in range(16):
            tx, ty = x % 8, y % 8
            if tx == 0 or ty == 0:
                cv.put(x, y, DT[0])                             # mortar seam
            elif tx == 1 or ty == 1:
                cv.put(x, y, mix(cv.get(x, y), DT[6], 0.35))    # lit top/left tile edge
            elif tx == 7 or ty == 7:
                cv.put(x, y, mix(cv.get(x, y), DT[1], 0.5))     # shaded bottom/right tile edge
    groove, plate = {}, set()
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 8, y - 8)
            if r <= 6.45:
                if r > 5.35:
                    groove[(x, y)] = (x - 8) + (y - 8)          # > 0: lower-right, faces the light
                else:
                    plate.add((x, y))
    for p in plate:
        cv.put(*p, DT[4] if noise(*p, seed + 1) > 0.12 else DT[3])
    for p, f in groove.items():
        cv.put(*p, DT[2] if f > 2.5 else DT[0])
    # the plate's rim: a lit lower-right lip and a shadowed upper-left one
    for p in plate:
        r = math.hypot(p[0] - 8, p[1] - 8)
        if r > 4.4:
            f = (p[0] - 8) + (p[1] - 8)
            if f > 2.5:
                cv.put(*p, DT[5])
            elif f < -2.5:
                cv.put(*p, DT[2])
    return cv, groove, plate


def _seal(element: str, lit: bool) -> Canvas:
    colour = hexc(SEAL_COLOURS.get(element, "#E678DC"))
    cv, groove, plate = _seal_base(sum(map(ord, element)))
    glyph = {(x + 8 - len(grid(SEAL_GLYPHS[element])[0]) // 2,
              y + 8 - len(grid(SEAL_GLYPHS[element])) // 2): ch
             for x, y, ch in cells(SEAL_GLYPHS[element])}
    dark = mix(DT[1], colour, 0.12)
    if lit:
        tone = {"-": mix(colour, (0, 0, 0), 0.45), "#": colour,
                "+": mix(colour, (255, 255, 255), 0.45), "*": mix(colour, (255, 255, 255), 0.8)}
        # light spilling out: the ring groove and the plate round the glyph pick up colour
        for p in groove:
            cv.put(*p, mix(cv.get(*p), colour, 0.45))
        for p in plate:
            cv.put(*p, mix(cv.get(*p), colour, 0.08))
        cut = {p: ch for p, ch in glyph.items() if ch != "-"}
        carve(cv, cut, lambda p, ch: tone[ch], bleed=(colour, 0.25))
        for p, ch in glyph.items():
            if ch == "-":
                cv.put(*p, tone["-"])
    else:
        # a faint engraving: dim grooves with the element's colour only just showing
        tone = {"-": mix(dark, DT[3], 0.5), "#": mix(dark, colour, 0.30),
                "+": mix(dark, colour, 0.40), "*": mix(dark, colour, 0.50)}
        cut = {p: ch for p, ch in glyph.items() if ch != "-"}
        carve(cv, cut, lambda p, ch: tone[ch])
        for p, ch in glyph.items():
            if ch == "-":
                cv.put(*p, tone["-"])
    return cv


def rune_seal_textures() -> dict:
    """element -> (unlit, lit), 16x16 each; the edges tile into a seamless door."""
    return {el: (_seal(el, False).image(), _seal(el, True).image()) for el in SEAL_COLOURS}


# ============================================================== the Archive Lectern
# The model is a 12x14x12 plinth that samples uv (2, 2, 14, 14) of the top/bottom and
# (2, 2, 14, 16) of the sides, so every face is designed inside that window; the border
# outside it just continues the pattern (it only shows in break particles).

# dark oak, darkest..lightest
DO = ramp("#1E1309", "#2A1B0D", "#372412", "#452F18", "#533A1F", "#624628", "#735534")

LECTERN_PAL = {
    # dark oak and deepslate
    "H": DO[6], "h": DO[5], "w": DO[4], "W": DO[3], "u": DO[2], "U": DO[1], "X": DO[0],
    "S": DS[6], "s": DS[5], "t": DS[4], "T": DS[3], "q": DS[2], "Q": DS[1],
    # the book: leather cover, pages, the gutter's shade, writing and a ribbon
    "k": hexc("#3A1A12"), "K": hexc("#5C2C1C"), "P": hexc("#EADFBE"), "p": hexc("#D2C298"),
    "g": hexc("#B8A474"), "v": hexc("#9C70F0"), "V": hexc("#C8A8FF"), "y": hexc("#E0B04A"),
    "Y": hexc("#FFE08A"), "r": hexc("#7A2EB8"),
    # book spines on the front shelf
    "1": hexc("#7A2A24"), "2": hexc("#A6443A"), "3": hexc("#243E6A"), "4": hexc("#3A5E96"),
    "5": hexc("#2E5A2C"), "6": hexc("#4A8440"), "7": hexc("#4A2A6E"), "8": hexc("#7048A6"),
    "G": hexc("#D8AC48"),
}

LECTERN_TOP = """
    WWWWWWWWWWWWWWWW
    WHHHHHHHHHHHHHHW
    WHHHHHHHHHHHHHuW
    WHhKKKKKKKKKKuuW
    WHhkPPPppPPPkuuW
    WHhkvVvppyYykuuW
    WHhkPPPppPPPkuuW
    WHhkVvPppYyPkuuW
    WHhkPPPppPPPkuuW
    WHhkvVVppyPYkuuW
    WHhkpppggpppkuuW
    WHhKKKKKrKKKKuuW
    WHwwwwwwrwwwwwuW
    WHuuuuuuuuuuuuUW
    WHuuuuuuuuuuuuUW
    WWWWWWWWWWWWWWWW
"""

LECTERN_SIDE = """
    WWWWWWWWWWWWWWWW
    WWWWWWWWWWWWWWWW
    WWHHHHHHHHHHHHWW
    WWwwwwwwwwwwwwWW
    WWsUUUUUUUUUUqWW
    WWswwwwwwwwwwqWW
    WWswUUUUUUUUhqWW
    WWswUWuWWuWhhqWW
    WWswUWuWWuWhhqWW
    WWswUWuWWuWhhqWW
    WWswUWuWWuWhhqWW
    WWswUhhhhhhhhqWW
    WWswwwwwwwwwwqWW
    WWsUUUUUUUUUUqWW
    WWSSSSSSSSSSSSWW
    WWtTTTTTTTTTTQWW
"""

LECTERN_FRONT = """
    WWWWWWWWWWWWWWWW
    WWWWWWWWWWWWWWWW
    WWHHHHHHHHHHHHWW
    WWwwwwwwwwwwwwWW
    WWsUUUUUUUUUUqWW
    WWswwwwGGwwwwqWW
    WWswXXXXXXXXhqWW
    WWswX7X3XXXXhqWW
    WWswX78342X5hqWW
    WWswX78342156qWW
    WWswX78342156qWW
    WWswhhhhhhhhhqWW
    WWswwwwwwwwwwqWW
    WWsUUUUUUUUUUqWW
    WWSSSSSSSSSSSSWW
    WWtTTTTTTTTTTQWW
"""

LECTERN_BOTTOM = """
    TTTTTTTTTTTTTTTT
    TTTTTTTTTTTTTTTT
    TTssssssssssssTT
    TTstttttttttTqTT
    TTsttttttttttqTT
    TTsttTtttttttqTT
    TTsttttttttTtqTT
    TTstttttttttTqTT
    TTsttttTtttttqTT
    TTsttttttttttqTT
    TTstTttttttttqTT
    TTsttttttTtttqTT
    TTsttttttttttqTT
    TTqqqqqqqqqqqqTT
    TTTTTTTTTTTTTTTT
    TTTTTTTTTTTTTTTT
"""


def _oak_fleck(cv: Canvas, keys, seed: int):
    """Break up flat dark-oak runs with a sparse, darker grain fleck."""
    for y in range(16):
        for x in range(16):
            c = cv.get(x, y)
            if c in keys and noise(x, y, seed) > 0.86:
                cv.put(x, y, mix(c, (0, 0, 0), 0.18))


def archive_lectern_textures() -> dict:
    """16x16 faces for the Archive Lectern: 'top' (the open book), 'side', 'front', 'bottom'."""
    out = {}
    for key, text, seed in (("top", LECTERN_TOP, 5), ("side", LECTERN_SIDE, 6),
                            ("front", LECTERN_FRONT, 7), ("bottom", LECTERN_BOTTOM, 8)):
        cv = sprite(text, LECTERN_PAL)
        _oak_fleck(cv, {DO[3], DO[4], DO[2]}, seed)
        out[key] = cv.image()
    return out


# ============================================================== entity skins: box UV


def box(u: int, v: int, w: int, h: int, d: int) -> dict:
    """Face rectangles (x, y, width, height) of a cube with texture offset (u, v)."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
            "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def face(cv: Canvas, area, text: str, pal: dict, mirror: bool = False):
    """Paint one face from an ASCII block that must match the face's size exactly."""
    x0, y0, w, h = area
    rows = grid(text)
    assert len(rows) == h and len(rows[0]) == w, f"face {area}: got {len(rows[0])}x{len(rows)}"
    for y, row in enumerate(rows):
        for x, ch in enumerate(row[::-1] if mirror else row):
            if ch in pal:
                cv.put(x0 + x, y0 + y, pal[ch])


# ============================================================== the Archivist (illager layout)

ARCHIVIST_PAL = {
    # deep indigo robe, darkest..lightest
    "a": hexc("#0F0A20"), "b": hexc("#191232"), "c": hexc("#221946"), "d": hexc("#2E2358"),
    "e": hexc("#3B2E6C"), "f": hexc("#4B3D84"),
    # gold trim
    "z": hexc("#4E340C"), "J": hexc("#86601C"), "j": hexc("#B48C2C"), "g": hexc("#DEB84A"),
    "G": hexc("#F8E08A"),
    # dark sleeves
    "m": hexc("#0C0914"), "n": hexc("#140F20"), "N": hexc("#1D162E"), "M": hexc("#291F40"),
    # grey-blue skin, darkest..lightest
    "1": hexc("#3A4250"), "2": hexc("#4C5666"), "3": hexc("#5E6A7C"), "4": hexc("#717E90"),
    "5": hexc("#8894A6"), "6": hexc("#A2AEBE"),
    # glowing amber eyes
    "O": hexc("#E0901C"), "E": hexc("#FFC43C"), "F": hexc("#FFF4B0"),
    # dark hair and brow
    "k": hexc("#110E17"), "K": hexc("#1C1726"), "L": hexc("#2C2538"),
    # violet gem
    "v": hexc("#6E38C4"), "V": hexc("#B48AFF"), "W": hexc("#EADCFF"),
    # boots
    "x": hexc("#16110C"), "X": hexc("#241C14"), "y": hexc("#3A2E20"), "Y": hexc("#54432E"),
}

# ---- head (0,0) 8x10x8 and nose (24,0) 2x4x2
A_HEAD_TOP = """
    KKKKKKKK
    KKLKKKKK
    KKKKKLKK
    KLKKKKKK
    KKKKKKLK
    KKKLKKKK
    KKKKKKKK
    kKKKKKKk
"""
A_HEAD_BOTTOM = """
    12222221
    22333322
    23333332
    23333332
    23333332
    23333332
    22333322
    12222221
"""
A_HEAD_FRONT = """
    KKKKKKKK
    K455554K
    jgjvVjgj
    44555544
    3kkkkkk3
    4EF44FE4
    43344334
    34455443
    3k4444k3
    23444432
"""
# the head's right side: column 0 meets the back, column 7 meets the face
A_HEAD_RIGHT = """
    KKKKKKKK
    KKKKKKKK
    JJjjjjjj
    KKKK3344
    KKK33334
    KKK35334
    KKK32434
    KK333344
    KK233343
    K1222232
"""
A_HEAD_BACK = """
    KKKKKKKK
    KKLKKKLK
    jjjGGjjJ
    KKKKKKKK
    KLKKKKLK
    KKKKKKKK
    KKKLKKKK
    KKKKKKKK
    kKKKKKKk
    k222222k
"""
A_NOSE = {
    "top": "55\n55", "bottom": "21\n12",
    "right": "44\n34\n34\n23", "front": "56\n45\n45\n21", "left": "44\n43\n43\n32", "back": "33\n33\n33\n22",
}

# ---- robe (0,38) 8x20x6: a V collar, a gold sigil on the chest, a belt, a gold hem
A_ROBE_FRONT = """
    dgaaaagc
    ddgaagdc
    dddggddc
    dddccddc
    ddcggcdc
    dcgccgcc
    dgcvVcgb
    dcgccgcb
    ddcggccb
    ddccccbb
    JjgGGgjJ
    ddcgjcbb
    dbcgjcbb
    dbcgjcbb
    dbcgjcab
    dbcgjcab
    dbcgjcab
    dccgjcbb
    eddgjddc
    jgggjjjJ
"""
# column 0 meets the back, column 5 meets the front
A_ROBE_RIGHT = """
    cdddde
    cccddd
    ccccdd
    cbccdd
    cbccdd
    cbccdd
    cbccdd
    cbcccd
    cbcccd
    ccccdd
    JJjjjj
    cbccdd
    cbcbdd
    cbcbdd
    cbcbcd
    cbcbcd
    cbcbcd
    ccccdd
    cdddde
    JJjjjg
"""
A_ROBE_BACK = """
    cjbbbbjc
    cjbaabjc
    ccjbbjcc
    cccjjccc
    cccccccc
    cdccccbc
    cdccccbc
    cdccccbc
    cdccccbc
    cccccccc
    JjjjjjjJ
    ccbccbcc
    cdbcdbcc
    cdbcdbcb
    cdbcdbcb
    cdbcdbcb
    cdbcdbcb
    ccbccbcc
    cddddddc
    JjjjjjjJ
"""
A_ROBE_TOP = """
    cddddddc
    djggggjd
    dgaaaajd
    dgaaaajd
    djjjjjjd
    cddddddc
"""
A_ROBE_BOTTOM = """
    JjjjjjjJ
    jaaaaaaj
    jaaaaaaj
    jaaaaaaj
    jaaaaaaj
    JjjjjjjJ
"""

# ---- legs (0,22) 4x12x4: dark hose under the robe, boots below its hem
A_LEG_SIDE = """
    NNNn
    NNNn
    NNnn
    NNNn
    NNNn
    NNnn
    NNNn
    NNNn
    YyyX
    yXXX
    yXXx
    xxxx
"""
A_LEG_FRONT = """
    MNNn
    MNNn
    NNNn
    MNNn
    MNNn
    NNnn
    MNNn
    MNNn
    YYyX
    yyXX
    YyyX
    xxxx
"""

# ---- crossed arms: upper arms (44,22) 4x8x4 and the folded forearms (40,38) 8x4x4
A_ARM_SIDE = """
    MNNN
    MNNn
    NNNn
    MNNn
    NNNn
    MNnn
    jggj
    mmmm
"""
A_ARM_FRONT = """
    MMNN
    MNNn
    MNNn
    NNNn
    MNNn
    NNnn
    gGgj
    mmmm
"""
A_BAR_FRONT = """
    MNNg65gN
    NNNj54jN
    NNNj45jn
    nnnj34jn
"""
A_BAR_TOP = """
    MMMg66gM
    MNNg55gN
    NNNg55gN
    NNNj44jN
"""
A_BAR_BOTTOM = """
    nnnj33jn
    nnnj33jn
    nnnj22jn
    mmmJ22Jm
"""

# ---- casting arms (40,46) 4x12x4: sleeve, gold cuff, grey hand at the far (bottom) end
A_CAST_FRONT = """
    MMNN
    MNNn
    MNNn
    NNNn
    MNNn
    MNNn
    NNnn
    MNNn
    gGgj
    5654
    4543
    4343
"""
A_CAST_SIDE = """
    MNNN
    MNNn
    NNNn
    MNNn
    NNNn
    MNnn
    NNNn
    NNnn
    jggJ
    4543
    4432
    3432
"""


def archivist_texture() -> Image.Image:
    """64x64 skin on the vanilla illager layout: a spell-keeper in an indigo robe with gold
    trim and a chest sigil, dark sleeves with gold cuffs, grey-blue skin, glowing amber eyes
    and a gold circlet. The hat layer (32,0) stays transparent."""
    cv = Canvas(64)
    P = ARCHIVIST_PAL
    h = box(0, 0, 8, 10, 8)
    face(cv, h["top"], A_HEAD_TOP, P)
    face(cv, h["bottom"], A_HEAD_BOTTOM, P)
    face(cv, h["front"], A_HEAD_FRONT, P)
    face(cv, h["right"], A_HEAD_RIGHT, P)
    face(cv, h["left"], A_HEAD_RIGHT, P, mirror=True)
    face(cv, h["back"], A_HEAD_BACK, P)
    for k, r in box(24, 0, 2, 4, 2).items():
        face(cv, r, A_NOSE[k], P)
    # (32,0) is the hat layer: left transparent
    robe = box(0, 38, 8, 20, 6)
    face(cv, robe["front"], A_ROBE_FRONT, P)
    face(cv, robe["right"], A_ROBE_RIGHT, P)
    face(cv, robe["left"], A_ROBE_RIGHT, P, mirror=True)
    face(cv, robe["back"], A_ROBE_BACK, P)
    face(cv, robe["top"], A_ROBE_TOP, P)
    face(cv, robe["bottom"], A_ROBE_BOTTOM, P)
    # the body under the robe wears the robe's upper half, in case any of it shows
    body = box(16, 20, 8, 12, 6)
    face(cv, body["front"], "\n".join(grid(A_ROBE_FRONT)[:12]), P)
    face(cv, body["right"], "\n".join(grid(A_ROBE_RIGHT)[:12]), P)
    face(cv, body["left"], "\n".join(grid(A_ROBE_RIGHT)[:12]), P, mirror=True)
    face(cv, body["back"], "\n".join(grid(A_ROBE_BACK)[:12]), P)
    face(cv, body["top"], A_ROBE_TOP, P)
    face(cv, body["bottom"], A_ROBE_BOTTOM.replace("J", "a").replace("j", "a"), P)
    legs = box(0, 22, 4, 12, 4)
    face(cv, legs["front"], A_LEG_FRONT, P)
    face(cv, legs["right"], A_LEG_SIDE, P)
    face(cv, legs["left"], A_LEG_SIDE, P, mirror=True)
    face(cv, legs["back"], A_LEG_SIDE, P, mirror=True)
    face(cv, legs["top"], "NNNN\nNNNN\nNNNN\nNNNN", P)
    face(cv, legs["bottom"], "xxxx\nxXXx\nxXXx\nxxxx", P)
    arm = box(44, 22, 4, 8, 4)
    face(cv, arm["front"], A_ARM_FRONT, P)
    face(cv, arm["right"], A_ARM_SIDE, P)
    face(cv, arm["left"], A_ARM_SIDE, P, mirror=True)
    face(cv, arm["back"], A_ARM_SIDE, P, mirror=True)
    face(cv, arm["top"], "MMMN\nMNNN\nNNNn\nNNnn", P)
    face(cv, arm["bottom"], "mmmm\nmnnm\nmnnm\nmmmm", P)
    bar = box(40, 38, 8, 4, 4)
    face(cv, bar["front"], A_BAR_FRONT, P)
    face(cv, bar["top"], A_BAR_TOP, P)
    face(cv, bar["bottom"], A_BAR_BOTTOM, P)
    face(cv, bar["back"], "NNNNNNNN\nNNNNNNNN\nNnNNNNnN\nnnnnnnnn", P)
    face(cv, bar["right"], "MNNn\nNNNn\nNNnn\nnnnn", P)
    face(cv, bar["left"], "MNNn\nNNNn\nNNnn\nnnnn", P, mirror=True)
    cast = box(40, 46, 4, 12, 4)
    face(cv, cast["front"], A_CAST_FRONT, P)
    face(cv, cast["right"], A_CAST_SIDE, P)
    face(cv, cast["left"], A_CAST_SIDE, P, mirror=True)
    face(cv, cast["back"], A_CAST_SIDE, P, mirror=True)
    face(cv, cast["top"], "MMMN\nMNNN\nNNNn\nNNnn", P)
    face(cv, cast["bottom"], "4543\n5434\n4343\n3432", P)
    return cv.image()


# ============================================================== the Training Dummy (DummyModel layout)

BURLAP = ramp("#5A4026", "#7A5A38", "#96724C", "#A9855C", "#BE9C70", "#D4B488")
SACK = ramp("#76581F", "#9C7C2C", "#BE9A42", "#D2AE56", "#E4C66E", "#F4DE96")
ROPE = ramp("#4A331E", "#6A4C2E", "#8E6C3E", "#B4905C")
PALE_WOOD = ramp("#6A4A2A", "#8E6A44", "#AE875A", "#C8A270", "#DEBE8C")
DARK_WOOD = ramp("#20150A", "#2E1F0E", "#402C15", "#54391D", "#684826")
TARGET_PAL = {"R": hexc("#C4302A"), "r": hexc("#8E1E1C"), "W": hexc("#F0EADC"), "w": hexc("#CEC4B0"),
              "s": hexc("#3A2616"), "T": SACK[5], "t": SACK[3]}

# the painted target on the head's front face ('.' leaves the burlap showing)
D_TARGET = """
    ..RRR..
    .RWWWR.
    RWRRRWR
    RWRrRWR
    RWRRRwR
    .RwWwR.
    ..RRr..
"""
# the back seam, stitched shut
D_HEAD_BACK = """
    ...s...
    ..s.s..
    ...s...
    ..s.s..
    ...s...
    ..s.s..
    ...s...
"""
# the sack's top: two seams crossing, and a tuft of straw where they meet
D_HEAD_TOP = """
    ...s...
    .......
    ...s...
    s.stt.s
    ...T...
    .......
    ...s...
"""
# a mended tear on each side, zigzag-stitched
D_HEAD_SIDE = """
    .......
    .......
    .s.s.s.
    ..s.s..
    .......
    .......
    .......
"""


def _weave(cv: Canvas, area, tones, seed: int, lo: int = 2):
    """Coarse woven cloth: a two-tone checker with a few darker and lighter slubs."""
    x0, y0, w, h = area
    for y in range(h):
        for x in range(w):
            r = noise(x0 + x, y0 + y, seed)
            t = lo + ((x + y) % 2)
            if r < 0.08:
                t = lo - 1
            elif r > 0.94:
                t = lo + 2
            cv.put(x0 + x, y0 + y, tones[t])


def _grain(cv: Canvas, area, tones, seed: int, along_x: bool):
    """Pale wood: lit first row/column, darker second, the odd darker fleck of grain."""
    x0, y0, w, h = area
    for y in range(h):
        for x in range(w):
            i = y if along_x else x
            t = 3 if i == 0 else 2
            if noise(x0 + x, y0 + y, seed) > 0.85:
                t = 1
            cv.put(x0 + x, y0 + y, tones[t])


def _end_grain(cv: Canvas, area, tones):
    x0, y0, w, h = area
    for y in range(h):
        for x in range(w):
            cv.put(x0 + x, y0 + y, tones[1] if (x + y) % 2 else tones[2])


def dummy_texture() -> Image.Image:
    """64x64 skin for the Training Dummy: a burlap head with a target on the front, a roped
    straw sack body, a pale wooden pole and crossbar, straw hands and a dark wooden foot."""
    cv = Canvas(64)
    # ---- head (0,0) 7x7x7: burlap all round, the target on the front
    head = box(0, 0, 7, 7, 7)
    for k, r in head.items():
        _weave(cv, r, BURLAP, 3)
    face(cv, head["front"], D_TARGET, TARGET_PAL)
    face(cv, head["back"], D_HEAD_BACK, TARGET_PAL)
    face(cv, head["top"], D_HEAD_TOP, TARGET_PAL)
    face(cv, head["right"], D_HEAD_SIDE, TARGET_PAL)
    face(cv, head["left"], D_HEAD_SIDE, TARGET_PAL, mirror=True)
    x0, y0, w, h = head["bottom"]
    rect(cv, x0 + 2, y0 + 2, 3, 3, BURLAP[1])            # gathered where it sits on the body
    cv.put(x0 + 3, y0 + 3, BURLAP[0])

    # ---- torso (0,14) 8x11x5: a straw-coloured sack, roped twice, straw poking out
    torso = box(0, 14, 8, 11, 5)
    x0, y0 = 0, 14 + 5
    band_w = 2 * (8 + 5)
    _weave(cv, (x0, y0, band_w, 11), SACK, 5)
    for s in range(band_w):
        for ry in (2, 8):                                   # twisted rope ties
            cv.put(x0 + s, y0 + ry, ROPE[3] if (s + ry) % 3 else ROPE[1])
            cv.put(x0 + s, y0 + ry + 1, ROPE[1] if (s + ry) % 3 else ROPE[0])
        if noise(s, 0, 9) > 0.45:                           # tufts at the neck and the hem
            cv.put(x0 + s, y0, SACK[5])
        if noise(s, 10, 9) > 0.5:
            cv.put(x0 + s, y0 + 10, SACK[4])
    fx, fy = torso["front"][:2]
    _weave(cv, (fx + 1, fy + 4, 3, 3), BURLAP, 7)           # a burlap patch sewn over a split
    for (px, py) in ((1, 4), (3, 4), (1, 6), (3, 6)):
        cv.put(fx + px, fy + py, TARGET_PAL["s"])
    for k in ("top", "bottom"):
        x1, y1, w1, h1 = torso[k]
        for y in range(h1):
            for x in range(w1):
                r = noise(x1 + x, y1 + y, 21)
                cv.put(x1 + x, y1 + y, SACK[5] if r > 0.7 else SACK[4] if r > 0.35 else SACK[2])
    x1, y1, w1, h1 = torso["bottom"]
    rect(cv, x1 + 3, y1 + 1, 2, 3, SACK[0])                 # where the pole goes in

    # ---- pole (28,0) 2x12x2 and crossbar (0,32) 16x2x2: pale wood
    pole = box(28, 0, 2, 12, 2)
    for k in ("right", "front", "left", "back"):
        _grain(cv, pole[k], PALE_WOOD, 13, along_x=False)
    for k in ("top", "bottom"):
        _end_grain(cv, pole[k], PALE_WOOD)
    bar = box(0, 32, 16, 2, 2)
    for k in ("front", "back", "top", "bottom"):
        _grain(cv, bar[k], PALE_WOOD, 17, along_x=True)
    for k in ("right", "left"):
        _end_grain(cv, bar[k], PALE_WOOD)

    # ---- hands (40,26) 3x3x3: straw bundles tied round the bar ends
    hand = box(40, 26, 3, 3, 3)
    for k, r in hand.items():
        _weave(cv, r, SACK, 29, lo=3)
    for k in ("right", "front", "left", "back"):
        x1, y1, w1, h1 = hand[k]
        for x in range(w1):
            cv.put(x1 + x, y1 + 1, ROPE[2] if x % 2 else ROPE[1])
    for k in ("top", "bottom"):
        x1, y1, w1, h1 = hand[k]
        cv.put(x1 + 1, y1 + 1, SACK[1])

    # ---- base (32,16) 6x2x6: dark planks with the pole socket in the middle
    base = box(32, 16, 6, 2, 6)
    x1, y1, w1, h1 = base["top"]
    for y in range(6):
        for x in range(6):
            t = 3 if y % 2 == 0 else 2
            if x == 0 or y == 0:
                t = 4
            if noise(x1 + x, y1 + y, 31) > 0.85:
                t -= 1
            cv.put(x1 + x, y1 + y, DARK_WOOD[t])
    rect(cv, x1 + 2, y1 + 2, 2, 2, DARK_WOOD[0])
    for k in ("right", "front", "left", "back"):
        x1, y1, w1, h1 = base[k]
        for x in range(w1):
            cv.put(x1 + x, y1, DARK_WOOD[4])
            cv.put(x1 + x, y1 + 1, DARK_WOOD[2])
        cv.put(x1 + 1, y1 + 1, DARK_WOOD[0])                # nail heads
        cv.put(x1 + w1 - 2, y1 + 1, DARK_WOOD[0])
    x1, y1, w1, h1 = base["bottom"]
    rect(cv, x1, y1, w1, h1, DARK_WOOD[1])
    return cv.image()


# ============================================================== preview


def _check(frames, name, size=16, allow_alpha=False):
    for im in frames:
        assert im.size == (size, size) and im.mode == "RGBA", name
        alphas = set(im.getchannel("A").tobytes())
        assert alphas <= {0, 255}, f"{name}: partial alpha {alphas}"
        if not allow_alpha:
            assert alphas == {255}, f"{name}: must be opaque"


def _part(skin, rect, mirror=False, flip=False):
    x, y, w, h = rect
    im = skin.crop((x, y, x + w, y + h))
    if mirror:
        im = im.transpose(Image.FLIP_LEFT_RIGHT)
    if flip:
        im = im.transpose(Image.FLIP_TOP_BOTTOM)
    return im


def _assemble(size, placements) -> Image.Image:
    """Paste face crops onto a transparent canvas: placements = [(image, (x, y)), ...]."""
    im = Image.new("RGBA", size)
    for part, at in placements:
        im.alpha_composite(part, at)
    return im


def archivist_views(skin) -> list:
    """Flat front / casting / back / side views of the illager model, for checking seams."""
    head, nose, robe = box(0, 0, 8, 10, 8), box(24, 0, 2, 4, 2), box(0, 38, 8, 20, 6)
    legs, arm, bar, cast = box(0, 22, 4, 12, 4), box(44, 22, 4, 8, 4), box(40, 38, 8, 4, 4), box(40, 46, 4, 12, 4)
    P = lambda r, m=False, f=False: _part(skin, r, m, f)
    body = [(P(legs["front"]), (4, 22)), (P(legs["front"], True), (8, 22)), (P(robe["front"]), (4, 10)),
            (P(head["front"]), (4, 0)), (P(nose["front"]), (7, 7))]
    front = _assemble((16, 34), body + [(P(arm["front"]), (0, 11)), (P(arm["front"], True), (12, 11)),
                                        (P(bar["front"]), (4, 15))])
    casting = _assemble((16, 34), body + [(P(cast["front"], f=True), (0, 0)), (P(cast["front"], True, True), (12, 0))])
    back = _assemble((16, 34), [(P(legs["back"]), (4, 22)), (P(legs["back"], True), (8, 22)), (P(robe["back"]), (4, 10)),
                                (P(head["back"]), (4, 0)), (P(arm["back"]), (0, 11)), (P(arm["back"], True), (12, 11))])
    right = _assemble((12, 34), [(P(legs["right"]), (3, 22)), (P(robe["right"]), (3, 10)), (P(head["right"]), (2, 0)),
                                 (P(nose["right"]), (10, 7)), (P(arm["right"]), (4, 11))])
    left = _assemble((12, 34), [(P(legs["left"]), (5, 22)), (P(robe["left"]), (3, 10)), (P(head["left"]), (2, 0)),
                                (P(nose["left"]), (0, 7)), (P(arm["left"]), (4, 11))])
    return [("front", front), ("casting", casting), ("back", back), ("right", right), ("left", left),
            ("head top", P(head["top"]))]


def dummy_views(skin) -> list:
    """Flat front / back / side views of the Training Dummy model."""
    head, pole, torso = box(0, 0, 7, 7, 7), box(28, 0, 2, 12, 2), box(0, 14, 8, 11, 5)
    bar, hand, base = box(0, 32, 16, 2, 2), box(40, 26, 3, 3, 3), box(32, 16, 6, 2, 6)
    P = lambda r, m=False: _part(skin, r, m)
    out = []
    for f in ("front", "back"):
        out.append((f, _assemble((22, 33), [
            (P(base[f]), (8, 31)), (P(pole[f]), (10, 19)), (P(bar[f]), (3, 10)), (P(torso[f]), (7, 8)),
            (P(hand[f]), (0, 9)), (P(hand[f], True), (19, 9)), (P(head[f]), (7, 1))])))
    out.append(("right", _assemble((10, 33), [
        (P(base["right"]), (2, 31)), (P(pole["right"]), (4, 19)), (P(torso["right"]), (2, 8)),
        (P(bar["right"]), (4, 10)), (P(hand["right"]), (3, 9)), (P(head["right"]), (1, 1))])))
    out.append(("head top", P(head["top"])))
    return out


def lectern_views(faces: dict) -> list:
    """The faces as the 12x14x12 model actually samples them."""
    return [("top (in game)", faces["top"].crop((2, 2, 14, 14))),
            ("front (in game)", faces["front"].crop((2, 2, 14, 16))),
            ("side (in game)", faces["side"].crop((2, 2, 14, 16)))]


def preview(out_dir: str) -> str:
    """Write a labelled contact sheet of everything above, 8x nearest-neighbour."""
    from PIL import ImageDraw

    S = 8
    rows = []
    items = [("spell_scroll", spell_scroll_icon()), ("torn_page", torn_page_icon()),
             ("training_dummy", training_dummy_icon())]
    for n, im in items:
        _check([im], n, allow_alpha=True)
    rows.append(("Items", items))

    well = wellstone_textures()
    for n, v in well.items():
        _check(v if isinstance(v, list) else [v], n)
    rows.append(("Wellstone", [(n, well[n]) for n in ("wellstone_side", "wellstone_top", "wellstone_bottom")]))
    for n in ("wellstone_side_active", "wellstone_top_active"):
        rows.append((f"{n} ({len(well[n])} frames)", [(f"f{i}", im) for i, im in enumerate(well[n])]))

    seals = rune_seal_textures()
    for n, pair in seals.items():
        _check(list(pair), n)
    rows.append(("Rune Seals, unlit", [(n, pair[0]) for n, pair in seals.items()]))
    rows.append(("Rune Seals, lit", [(n, pair[1]) for n, pair in seals.items()]))
    doors = []
    for lit in (0, 1):
        door = Image.new("RGBA", (80, 64))
        for i, pair in enumerate(list(seals.values()) * 2):
            door.paste(pair[lit], ((i % 5) * 16, (i // 5) * 16))
        doors.append((("lit" if lit else "unlit") + " 5x4 door (4x)", door.resize((80 * 4, 64 * 4), Image.NEAREST)))
    rows.append(("Seal door tiling", doors))

    lectern = archive_lectern_textures()
    for n, im in lectern.items():
        _check([im], n)
    rows.append(("Archive Lectern", list(lectern.items()) + lectern_views(lectern)))

    arch = archivist_texture()
    _check([arch], "archivist", size=64, allow_alpha=True)
    rows.append(("Archivist (64x64, illager layout)", [("skin", arch)] + archivist_views(arch)))
    dummy = dummy_texture()
    _check([dummy], "dummy", size=64, allow_alpha=True)
    rows.append(("Training Dummy (64x64)", [("skin", dummy)] + dummy_views(dummy)))

    pad, gap, title_h, label_h = 12, 10, 18, 14

    def scale(im):
        return im if im.width > 64 * 2 else im.resize((im.width * S, im.height * S), Image.NEAREST)

    width = max(sum(scale(im).width + gap for _, im in r) for _, r in rows) + 2 * pad
    height = pad + sum(title_h + max(scale(im).height for _, im in r) + label_h + gap for _, r in rows)
    sheet = Image.new("RGBA", (width, height), (0x8B, 0x8B, 0x8B, 255))
    d = ImageDraw.Draw(sheet)
    y = pad
    for title, r in rows:
        d.text((pad, y), title, fill=(15, 15, 15, 255))
        y += title_h
        x = pad
        for label, im in r:
            big = scale(im)
            sheet.alpha_composite(big, (x, y))
            d.text((x, y + big.height + 2), label, fill=(25, 25, 25, 255))
            x += big.width + gap
        y += max(scale(im).height for _, im in r) + label_h + gap
    Path(out_dir).mkdir(parents=True, exist_ok=True)
    out = str(Path(out_dir) / "world_art.png")
    sheet.save(out)
    return out


if __name__ == "__main__":
    default = str(Path(__file__).resolve().parent.parent / "build" / "art-preview")
    print(preview(sys.argv[1] if len(sys.argv) > 1 else default))
