"""Hand-tuned art for Wildercord's world content: the Spell Scroll, Torn Page and Training
Dummy items, the Wellstone, the Rune Seals, the Archive Lectern, the two entity skins, the
Archivist's glow layers and the Runebound rune marks.

Same house style as item_art.py: ASCII pictograms for the 16x16 items, ramps lit from the
top-left, no anti-aliasing. Block faces are built from small deterministic rules so they
tile cleanly; the skins are painted face by face on their models' box-UV layouts. Glow and mark
layers are pale on transparency, some of it soft-edged: they're drawn emissive and tinted in game.

Public API (imported by generate_assets.py):
    spell_scroll_icon() -> Image                (16x16 item)
    torn_page_icon() -> Image                   (16x16 item)
    training_dummy_icon() -> Image              (16x16 item)
    wellstone_textures() -> dict                (16x16 faces; the "_active" ones are 8-frame lists)
    rune_seal_textures() -> dict                (element -> (unlit, lit), 16x16 each)
    archive_lectern_textures() -> dict          ("top", "side", "front", "bottom")
    fusion_altar_textures() -> dict             ("top", an 8-frame list; "side"; "bottom")
    archivist_texture() -> Image                (128x64, ArchivistModel layout)
    dummy_texture() -> Image                    (64x64, DummyModel layout)
    creature_textures() -> dict                 (path under textures/entity/ -> Image: the Archivist's
                                                 two glow layers and the Runebound rune marks)
    advancement_background() -> Image           (16x16 tile behind the Wildercord advancement tab)

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


class Sheet:
    """A grid of any width and height for skins that aren't square (None = transparent).
    Colours are RGB, or RGBA for the glow and mark layers, which are drawn translucent."""

    def __init__(self, w: int, h: int):
        self.w, self.h = w, h
        self.px: list[list] = [[None] * w for _ in range(h)]

    def get(self, x, y):
        return self.px[y][x] if 0 <= x < self.w and 0 <= y < self.h else None

    def put(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[y][x] = c

    def image(self) -> Image.Image:
        img = Image.new("RGBA", (self.w, self.h), (0, 0, 0, 0))
        p = img.load()
        for y in range(self.h):
            for x in range(self.w):
                c = self.px[y][x]
                if c is not None:
                    p[x, y] = c if len(c) == 4 else (c[0], c[1], c[2], 255)
        return img


def fill(cv, area, col):
    x0, y0, w, h = area
    rect(cv, x0, y0, w, h, col)


# ============================================================== Runebound rune marks

# White marks on transparency, tinted in game with the spell's colour and drawn at full brightness
# over the monster's own model (RuneMarksLayer). '#' is a bright stroke, '+' a dim one; every bright
# stroke gets a faint halo inside its face. One sheet per skin layout.
MARK = (255, 255, 255, 255)
MARK_DIM = (255, 255, 255, 150)
MARK_HALO = (255, 255, 255, 56)

# a bind-rune on the chest: arms raised over a knot on a spine
M_CHEST = """
    ........
    .#....#.
    ..#..#..
    ...##...
    ..####..
    .#.##.#.
    ...##...
    ...##...
    ..#..#..
    .+....+.
    ........
    ........
"""
# between the shoulder blades: a ring over a crossed spine
M_BACK = """
    ........
    ...##...
    ..#..#..
    ..#..#..
    ...##...
    .######.
    ...##...
    ...##...
    ..+##+..
    ...##...
    ...++...
    ........
"""
# down the outside of an arm, and a short rune on its front
M_ARM_SIDE = """
    ....
    .#..
    .##.
    .#..
    .#..
    ..#.
    .##.
    ..#.
    ..#.
    .+..
    ....
    ....
"""
M_ARM_FRONT = """
    ....
    ....
    .##.
    .#..
    .##.
    ..#.
    .##.
    ....
    .+..
    ....
    ....
    ....
"""
M_LEG = """
    ....
    ....
    .#..
    .##.
    .#..
    .#+.
    ....
"""
# a mark on the brow, above the eyes
M_HEAD = """
    ........
    ...##...
    ..#..#..
"""
# thin limbs: a zigzag of light
M_THIN_ARM = """
    ..
    #.
    .#
    #.
    .#
    ..
    #.
    .#
    ..
    +.
"""
M_THIN_LEG = """
    ..
    ..
    #.
    .#
    #.
"""
M_MID_ARM = """
    ...
    .#.
    .#.
    #..
    .#.
    ..#
    .#.
    .#.
    ...
    .+.
"""
# folded forearms (illagers, witches)
M_BAR = """
    ........
    .#.##.#.
    ..+..+..
"""


def _marks(cv, area, text: str, mirror: bool = False):
    """Paint marks into a face, padding the design with nothing to the face's size."""
    x0, y0, w, h = area
    rows = grid(text)
    assert len(rows) <= h and len(rows[0]) <= w, f"marks {area}: design is {len(rows[0])}x{len(rows)}"
    bright = []
    for y, row in enumerate(rows):
        row = row.ljust(w, ".")
        for x, ch in enumerate(row[::-1] if mirror else row):
            if ch == "#":
                cv.put(x0 + x, y0 + y, MARK)
                bright.append((x, y))
            elif ch == "+":
                cv.put(x0 + x, y0 + y, MARK_DIM)
    for x, y in bright:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < w and 0 <= ny < h and cv.get(x0 + nx, y0 + ny) is None:
                cv.put(x0 + nx, y0 + ny, MARK_HALO)


def _torso(cv, b):
    _marks(cv, b["front"], M_CHEST)
    _marks(cv, b["back"], M_BACK)


def _limb(cv, b, side: str, front: str, outer: str, mirror: bool = False):
    _marks(cv, b[outer], side, mirror)
    _marks(cv, b["front"], front, mirror)


def runebound_marks_textures() -> dict:
    """Rune marks for each monster skin layout a Runebound can wear, keyed by layout name:
    humanoid (zombies, husks, drowned, zombified piglins; 64x64, with the overlay layer too),
    skeleton (64x32), parched (its second body over the first; 64x64), zombie_villager and
    illager (64x64) and witch (64x128). Robed layouts carry the marks on the robe (the outer
    layer) as well as the body beneath, so they show whichever is drawn on top."""
    out = {}

    cv = Sheet(64, 64)
    _marks(cv, box(0, 0, 8, 8, 8)["front"], M_HEAD)
    for u, v in ((16, 16), (16, 32)):
        _torso(cv, box(u, v, 8, 12, 4))
    for u, v in ((40, 16), (40, 32)):
        _limb(cv, box(u, v, 4, 12, 4), M_ARM_SIDE, M_ARM_FRONT, "right")
    for u, v in ((32, 48), (48, 48)):
        _limb(cv, box(u, v, 4, 12, 4), M_ARM_SIDE, M_ARM_FRONT, "left", mirror=True)
    for u, v in ((0, 16), (0, 32)):
        _marks(cv, box(u, v, 4, 12, 4)["front"], M_LEG)
    for u, v in ((16, 48), (0, 48)):
        _marks(cv, box(u, v, 4, 12, 4)["front"], M_LEG, mirror=True)
    out["humanoid"] = cv.image()

    cv = Sheet(64, 32)
    _marks(cv, box(0, 0, 8, 8, 8)["front"], M_HEAD)
    _torso(cv, box(16, 16, 8, 12, 4))
    _limb(cv, box(40, 16, 2, 12, 2), M_THIN_ARM, M_THIN_ARM, "right")
    _marks(cv, box(0, 16, 2, 12, 2)["front"], M_THIN_LEG)
    out["skeleton"] = cv.image()

    cv = Sheet(64, 64)
    for u, v in ((0, 0), (0, 32)):
        _marks(cv, box(u, v, 8, 8, 8)["front"], M_HEAD)
    for u, v in ((16, 16), (16, 48)):
        _torso(cv, box(u, v, 8, 12, 4))
    _limb(cv, box(40, 16, 2, 12, 2), M_THIN_ARM, M_THIN_ARM, "right")
    _limb(cv, box(56, 16, 2, 12, 2), M_THIN_ARM, M_THIN_ARM, "left", mirror=True)
    _limb(cv, box(42, 33, 3, 12, 3), M_MID_ARM, M_MID_ARM, "right")
    _limb(cv, box(40, 48, 3, 12, 3), M_MID_ARM, M_MID_ARM, "left", mirror=True)
    _marks(cv, box(0, 16, 2, 12, 2)["front"], M_THIN_LEG)
    out["parched"] = cv.image()

    def villager_body(cv):
        _marks(cv, box(0, 0, 8, 10, 8)["front"], M_HEAD)
        _torso(cv, box(16, 20, 8, 12, 6))
        _torso(cv, box(0, 38, 8, 20, 6))
        _marks(cv, box(0, 22, 4, 12, 4)["front"], M_LEG)

    def folded_arms(cv):
        arms = box(44, 22, 4, 8, 4)
        _marks(cv, arms["right"], "\n".join(grid(M_ARM_SIDE)[:8]))
        _marks(cv, arms["left"], "\n".join(grid(M_ARM_SIDE)[:8]), mirror=True)
        _marks(cv, arms["front"], "\n".join(grid(M_ARM_FRONT)[:8]))
        _marks(cv, box(40, 38, 8, 4, 4)["front"], M_BAR)

    cv = Sheet(64, 64)
    villager_body(cv)
    _limb(cv, box(44, 22, 4, 12, 4), M_ARM_SIDE, M_ARM_FRONT, "right")
    out["zombie_villager"] = cv.image()

    cv = Sheet(64, 64)
    villager_body(cv)
    folded_arms(cv)
    _limb(cv, box(40, 46, 4, 12, 4), M_ARM_SIDE, M_ARM_FRONT, "right")
    out["illager"] = cv.image()

    cv = Sheet(64, 128)
    villager_body(cv)
    folded_arms(cv)
    out["witch"] = cv.image()
    return out


# ============================================================== the Archivist (ArchivistModel layout, 128x64)

ARCHIVIST_PAL = {
    # deep indigo robe, darkest..lightest
    "a": hexc("#0F0A20"), "b": hexc("#191232"), "c": hexc("#221946"), "d": hexc("#2E2358"),
    "e": hexc("#3B2E6C"), "f": hexc("#4B3D84"),
    # the dark under the hood
    "0": hexc("#06040C"),
    # gold trim
    "z": hexc("#4E340C"), "J": hexc("#86601C"), "j": hexc("#B48C2C"), "g": hexc("#DEB84A"),
    "G": hexc("#F8E08A"),
    # pale eyes and violet gems
    "v": hexc("#6E38C4"), "V": hexc("#B48AFF"), "W": hexc("#EADCFF"),
    # parchment and violet ink
    "p": hexc("#E9DDB8"), "q": hexc("#D2C08E"), "r": hexc("#A8925E"), "!": hexc("#4A2E86"),
}
ROBE = [ARCHIVIST_PAL[k] for k in "abcdef"]
GOLD_BAND = "jgjGjgj"

# ---- hood front (0,0)+(8,8), 10x11: only columns 2-7 of rows 3-10 show, between the cheeks and
# under the brim; the rest is hidden but painted as cloth. Two eyes burn in the dark.
A_FACE = """
    dddddddddd
    dccccccccd
    cbbbbbbbbc
    cbaaaaaabc
    cba0000abc
    cba0000abc
    cbaW00Wabc
    cbaV00Vabc
    cba0000abc
    cbaa00aabc
    cbaaaaaabc
"""
# ---- robe front (36,0)+(43,7), 10x9: a gold collar, a ringed gem on the chest, the robe's opening
A_ROBE_FRONT = """
    ddcgjjgcdd
    dcgcbbcgcd
    dgcbvVbcgd
    dgcbVWbcgd
    dcgcbbcgcd
    ddccjJccdd
    edccjJccde
    ddccjJccdd
    dcccjJcccd
"""
A_MANTLE_FRONT = """
    ddedgGGgdedd
    dddcgvVgcddd
    jjgjjGGjjgjj
"""
# ---- skirt front (0,19)+(8,27), 11x7: a belt, then a pale panel edged in gold
A_SKIRT_FRONT = """
    JjgjjGjjgjJ
    dcdjeeejdcd
    dcdjefejdcd
    cddjeeejddc
    dcdjefejdcd
    dccjeeejccd
    cdcjeeejcdc
"""
# ---- hem front (38,19)+(48,29), 13x7: the panel runs on down to a band of gold script
A_HEM_FRONT = """
    dcdcjeeejcdcd
    cdcdjefejdcdc
    dcdcjeeejcdcd
    dcddjefejddcd
    cdcdjeeejdcdc
    GjgGjgGjgGjgG
    JJjJJjJJjJJjJ
"""
# ---- the tome: its cover (outside, facing the Archivist), its pages, a turning page and a loose one
A_COVER = """
    gjjjjjg
    jdcccdj
    jcdddcj
    jcdvdcj
    jcvVvcj
    jcdvdcj
    jcdddcj
    jcdcdcj
    jdcccdj
    gjjjjjg
"""
A_PAGES = """
    pppppq
    p!!p!q
    pp!!pq
    pppppq
    p!p!!q
    p!!p!q
    pppppq
    p!!!pq
    pp!ppq
"""
A_TURNING = """
    pppppp
    p!!p!p
    pppppp
    p!p!!p
    pp!ppp
    pppppp
    p!!p!p
    p!pp!p
    pppppp
"""
A_LOOSE = """
    .ppq
    p!!p
    pp!p
    p!pp
    qpp.
"""

# ArchivistModel's boxes: texture offset and size (w, h, d)
A_HOOD = box(0, 0, 10, 11, 8)
A_BRIM = box(70, 11, 10, 3, 3)
A_CHEEK = box(110, 0, 2, 8, 2)
A_MANTLE = box(70, 0, 12, 3, 8)
A_ROBE = box(36, 0, 10, 9, 7)
A_SKIRT = box(0, 19, 11, 7, 8)
A_HEM = box(38, 19, 13, 7, 10)
A_SLEEVE = box(84, 19, 4, 11, 4)
A_CUFF = box(100, 19, 5, 5, 5)
A_TOME_COVER = box(0, 36, 7, 10, 1)
A_TOME_PAGES = box(16, 36, 6, 9, 1)
A_TURNING_PAGE = (30, 36, 6, 9)
A_LOOSE_PAGE = (42, 36, 4, 5)


def _cloth(cv, area, seed: int, base: int = 2, period: int = 3):
    """Heavy robe cloth: vertical folds (a lit ridge, a shadowed crease), a little weave noise,
    lit from above and darkening toward the bottom."""
    x0, y0, w, h = area
    edge = max(1, h // 6)
    for y in range(h):
        for x in range(w):
            k = (x + seed) % period
            t = base + (1 if k == 0 else -1 if k == period - 1 else 0)
            if y < edge:
                t += 1
            elif y >= h - edge:
                t -= 1
            n = noise(x0 + x, y0 + y, seed)
            if n < 0.12:
                t -= 1
            elif n > 0.9:
                t += 1
            cv.put(x0 + x, y0 + y, ROBE[max(0, min(5, t))])


def _band(cv, area, row: int, pattern: str = GOLD_BAND):
    """A row of gold trim across a face, `row` counted from the top (negative from the bottom)."""
    x0, y0, w, h = area
    y = y0 + (row if row >= 0 else h + row)
    for x in range(w):
        cv.put(x0 + x, y, ARCHIVIST_PAL[pattern[x % len(pattern)]])


def archivist_texture() -> Image.Image:
    """128x64 skin for ArchivistModel: a hooded keeper in a deep indigo robe that flares to a
    gold-scripted hem, a gold-trimmed mantle with a clasp, a ringed gem on the chest, long sleeves
    with gold cuffs, an empty dark under the hood with two pale eyes, and a tome bound in indigo
    and gold with violet-inked pages."""
    cv = Sheet(128, 64)
    P = ARCHIVIST_PAL
    # ---- the hood, its brim and cheeks
    _cloth(cv, A_HOOD["top"], 1, base=2)
    fill(cv, A_HOOD["bottom"], P["a"])
    _cloth(cv, A_HOOD["right"], 2)
    _cloth(cv, A_HOOD["left"], 3)
    _cloth(cv, A_HOOD["back"], 4, base=2, period=5)
    for side in ("right", "left", "back"):
        _band(cv, A_HOOD[side], -1, "jJ")
    face(cv, A_HOOD["front"], A_FACE, P)
    _cloth(cv, A_BRIM["top"], 5, base=3)
    fill(cv, A_BRIM["bottom"], P["0"])
    for side, seed in (("front", 6), ("right", 7), ("left", 8), ("back", 9)):
        _cloth(cv, A_BRIM[side], seed, base=3)
        _band(cv, A_BRIM[side], -1, "jgjGGjgj" if side == "front" else "jJ")
    _cloth(cv, A_CHEEK["right"], 10)
    fill(cv, A_CHEEK["left"], P["a"])
    face(cv, A_CHEEK["front"], "dj\ndj\ndg\ndj\ndj\ndg\ndj\njG", P)
    fill(cv, A_CHEEK["top"], P["c"])
    fill(cv, A_CHEEK["bottom"], P["a"])
    _cloth(cv, A_CHEEK["back"], 11)
    # ---- the mantle: gold-rimmed shoulders and a clasp
    x0, y0, w, h = A_MANTLE["top"]
    _cloth(cv, A_MANTLE["top"], 12, base=3, period=4)
    for x in range(w):
        for y in range(h):
            if x in (0, w - 1) or y in (0, h - 1):
                cv.put(x0 + x, y0 + y, P["j"] if (x + y) % 3 else P["g"])
    for side, seed in (("right", 13), ("left", 14), ("back", 15)):
        _cloth(cv, A_MANTLE[side], seed, base=3)
        _band(cv, A_MANTLE[side], -1)
    face(cv, A_MANTLE["front"], A_MANTLE_FRONT, P)
    fill(cv, A_MANTLE["bottom"], P["b"])
    # ---- the robe, skirt and hem
    face(cv, A_ROBE["front"], A_ROBE_FRONT, P)
    for side, seed in (("right", 16), ("left", 17), ("back", 18)):
        _cloth(cv, A_ROBE[side], seed)
    fill(cv, A_ROBE["top"], P["c"])
    fill(cv, A_ROBE["bottom"], P["b"])
    face(cv, A_SKIRT["front"], A_SKIRT_FRONT, P)
    for side, seed in (("right", 19), ("left", 20), ("back", 21)):
        _cloth(cv, A_SKIRT[side], seed)
        _band(cv, A_SKIRT[side], 0, "JjgjjGjjgj")
    fill(cv, A_SKIRT["top"], P["c"])
    fill(cv, A_SKIRT["bottom"], P["b"])
    face(cv, A_HEM["front"], A_HEM_FRONT, P)
    for side, seed in (("right", 22), ("left", 23), ("back", 24)):
        _cloth(cv, A_HEM[side], seed, period=4)
        _band(cv, A_HEM[side], -2, "GjgGjg")
        _band(cv, A_HEM[side], -1, "JJj")
    fill(cv, A_HEM["top"], P["c"])
    fill(cv, A_HEM["bottom"], P["a"])
    # ---- sleeves and cuffs
    for side, seed in (("right", 25), ("front", 26), ("left", 27), ("back", 28)):
        _cloth(cv, A_SLEEVE[side], seed, base=1)
        _cloth(cv, A_CUFF[side], seed, base=3)
        _band(cv, A_CUFF[side], 0, "jJ")
        _band(cv, A_CUFF[side], -1, "gjGjg")
    fill(cv, A_SLEEVE["top"], P["b"])
    fill(cv, A_SLEEVE["bottom"], P["a"])
    fill(cv, A_CUFF["top"], P["c"])
    face(cv, A_CUFF["bottom"], "aaaaa\nabbba\nab0ba\nabbba\naaaaa", P)
    # ---- the tome
    face(cv, A_TOME_COVER["back"], A_COVER, P)
    fill(cv, A_TOME_COVER["front"], P["b"])
    for side in ("right", "left", "top", "bottom"):
        fill(cv, A_TOME_COVER[side], P["c"])
    face(cv, A_TOME_PAGES["front"], A_PAGES, P)
    face(cv, A_TOME_PAGES["back"], "\n".join(["pppppp"] * 9), P)
    for side in ("right", "left"):
        face(cv, A_TOME_PAGES[side], "p\nq\np\nr\np\nq\np\nr\np", P)
    face(cv, A_TOME_PAGES["top"], "pqprpq", P)
    face(cv, A_TOME_PAGES["bottom"], "qprpqp", P)
    face(cv, A_TURNING_PAGE, A_TURNING, P)
    face(cv, A_LOOSE_PAGE, A_LOOSE, P)
    return cv.image()


def _glow(cv, area, text: str, colours: dict, halo=None, mirror: bool = False):
    """Paint glowing pixels (RGBA) from an ASCII face; `halo` (RGBA) rings the brightest inside the face."""
    x0, y0, w, h = area
    rows = grid(text)
    assert len(rows) == h and len(rows[0]) == w, f"glow {area}: got {len(rows[0])}x{len(rows)}"
    hot = []
    for y, row in enumerate(rows):
        for x, ch in enumerate(row[::-1] if mirror else row):
            if ch in colours:
                cv.put(x0 + x, y0 + y, colours[ch])
                hot.append((x, y))
    if halo:
        for x, y in hot:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and cv.get(x0 + nx, y0 + ny) is None:
                    cv.put(x0 + nx, y0 + ny, halo)


def archivist_eyes_texture() -> Image.Image:
    """128x64: what always glows on the Archivist, drawn over its skin at full brightness: its two
    eyes (with a faint violet haze and a drip of light under each) and the ringed gem on its chest."""
    cv = Sheet(128, 64)
    _glow(cv, A_HOOD["front"], A_FACE, {"W": (250, 244, 255, 255), "V": (196, 160, 255, 150)}, halo=(170, 130, 255, 90))
    chest = "\n".join(grid(A_ROBE_FRONT)[:5] + ["." * 10] * 4)
    _glow(cv, A_ROBE["front"], chest, {"g": (248, 214, 120, 150), "v": (170, 120, 255, 200), "V": (214, 186, 255, 230),
                                       "W": (246, 238, 255, 255)})
    return cv.image()


def archivist_runes_texture() -> Image.Image:
    """128x64: the writing on the Archivist's pages, the gem on its tome and the light in its cuffs,
    drawn over its skin at full brightness: dim at rest, blazing while it casts."""
    cv = Sheet(128, 64)
    ink = {"!": (186, 144, 255, 255)}
    halo = (150, 108, 255, 40)
    _glow(cv, A_TOME_PAGES["front"], A_PAGES, ink, halo)
    _glow(cv, A_TURNING_PAGE, A_TURNING, ink, halo)
    _glow(cv, A_LOOSE_PAGE, A_LOOSE, ink, halo)
    _glow(cv, A_TOME_COVER["back"], A_COVER, {"v": (170, 120, 255, 170), "V": (220, 196, 255, 230)})
    _glow(cv, A_CUFF["bottom"], ".....\n.bbb.\n.bWb.\n.bbb.\n.....", {"b": (180, 150, 255, 120), "W": (236, 226, 255, 220)})
    return cv.image()


def creature_textures() -> dict:
    """Entity textures beyond the two skins, keyed by their path under textures/entity/:
    the Archivist's glow layers and every Runebound marks sheet."""
    out = {"archivist_eyes": archivist_eyes_texture(), "archivist_runes": archivist_runes_texture()}
    for layout, image in runebound_marks_textures().items():
        out[f"runebound/{layout}"] = image
    return out


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


# ============================================================== the advancement tab's background
# Tiled 16x16 behind the whole Wildercord tab, so it stays dim and quiet: dark indigo ashlar
# (courses 4 px high, joints staggered) with a line of faint rune script cut into a few stones,
# only just violet, like the Archive's walls with the lamps low.

IN = ramp("#0D0A17", "#120E1F", "#161226", "#1B162E", "#211B37", "#282141", "#30284D")
SCRIPT = mix(IN[3], hexc("#6B4FB0"), 0.42)      # a groove with a little light left in it
SCRIPT_LIT = mix(IN[4], hexc("#9C7CE0"), 0.40)

# Glyphs cut into the stones, clear of the joints: (x, y) of the top-left cell, then its two rows
# ('#' groove, '+' groove catching the light).
BACKGROUND_SCRIPT = [
    (2, 1, ("#.+#", "+#.#")),
    (6, 5, ("+.#", "##.")),
    (3, 9, ("#+#", "#..")),
    (8, 13, ("+#.#", "#.#.")),
]


def advancement_background() -> Image.Image:
    cv = Canvas()
    stone_fill(cv, IN, 71, 2, 4)
    for y in range(16):
        course, row = divmod(y, 4)
        shift = 0 if course % 2 == 0 else 4
        for x in range(16):
            joint = (x + shift) % 8 == 0
            if row == 3 or joint:
                cv.put(x, y, IN[0])                                     # mortar
            elif row == 0:
                cv.put(x, y, mix(cv.get(x, y), IN[6], 0.35))            # lit top edge
            elif (x + shift) % 8 == 1:
                cv.put(x, y, mix(cv.get(x, y), IN[5], 0.2))             # lit left edge
            elif (x + shift) % 8 == 7 or row == 2:
                cv.put(x, y, mix(cv.get(x, y), IN[1], 0.4))             # shaded bottom/right
    for x0, y0, rows in BACKGROUND_SCRIPT:
        groove = {(x0 + i, y0 + j): ch for j, row in enumerate(rows) for i, ch in enumerate(row) if ch != "."}
        carve(cv, groove, lambda p, ch: SCRIPT_LIT if ch == "+" else SCRIPT)
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


def archivist_views(skin, eyes, runes) -> list:
    """Flat front views of ArchivistModel at rest (lit, and in the dark with only its glow), and
    its open tome, for checking seams and where the glow falls."""
    P = lambda im, r, m=False: _part(im, r, m)

    def front(im):
        # (x, y) are the model's pixels from the top of the hood, x from its right side (the viewer's left)
        return [(P(im, A_SLEEVE["front"]), (3, 12)), (P(im, A_CUFF["front"]), (2, 22)),
                (P(im, A_SLEEVE["front"], True), (17, 12)), (P(im, A_CUFF["front"], True), (17, 22)),
                (P(im, A_HOOD["front"]), (7, 1)), (P(im, A_BRIM["front"]), (7, 1)),
                (P(im, A_CHEEK["front"]), (7, 4)), (P(im, A_CHEEK["front"], True), (15, 4)),
                (P(im, A_MANTLE["front"]), (6, 12)), (P(im, A_ROBE["front"]), (7, 15)),
                (P(im, A_SKIRT["front"]), (6, 24)), (P(im, A_HEM["front"]), (5, 31))]

    lit = _assemble((24, 39), front(skin))
    dark = Image.eval(lit, lambda v: v)
    r, g, b, a = dark.split()
    dark = Image.merge("RGBA", (r.point(lambda v: v // 5), g.point(lambda v: v // 5), b.point(lambda v: v // 5), a))
    dark.alpha_composite(_assemble((24, 39), front(eyes)))
    dark = _on((10, 8, 16), dark)

    def book(im):
        return [(P(im, A_TOME_PAGES["front"]), (1, 1)), (P(im, A_TOME_PAGES["front"], True), (7, 1))]

    cover = _assemble((14, 11), [(P(skin, A_TOME_COVER["front"]), (0, 0)), (P(skin, A_TOME_COVER["front"], True), (7, 0))])
    tome = cover.copy()
    for part, at in book(skin):
        tome.alpha_composite(part, at)
    glowing = cover.copy()
    for part, at in book(skin) + book(runes):
        glowing.alpha_composite(part, at)
    return [("front", lit), ("front, dark", dark), ("tome, open", tome), ("tome, casting", glowing),
            ("cover", P(skin, A_TOME_COVER["back"])), ("pages", _assemble((12, 9), [(P(skin, A_TURNING_PAGE), (0, 0)),
                                                                                    (P(skin, A_LOOSE_PAGE), (7, 2))]))]


def _on(rgb, im) -> Image.Image:
    """An image over a solid background, for layers that are pale on transparency."""
    bg = Image.new("RGBA", im.size, (*rgb, 255))
    bg.alpha_composite(im)
    return bg


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


# ============================================================== the Fusion Altar
# An amethyst table on a deepslate-tile plinth (the model: a 14x3 foot, a 10x7 pillar, a 16x4 table
# top). Its faces take their natural uv, so the side texture reads top to bottom as the table's rim
# (rows 2-5), the pillar (rows 6-12) and the foot (rows 13-15). The top is an 8-frame loop: light
# runs round the amethyst ring and pools in the three rune sockets and the lodestone heart.

ALTAR_FRAMES = 8
# the lodestone at the altar's heart: dark iron with a bright core
LODE = ramp("#1A1B20", "#2A2C33", "#3D4049", "#5A5E6A", "#8A8FA0", "#C4C8D4")


def _tiles(cv: Canvas, seed: int, size: int = 8):
    """Deepslate tiles, as the Rune Seals lay them: seams, lit top/left edges, shaded bottom/right."""
    stone_fill(cv, DT, seed, 3, 5)
    for y in range(16):
        for x in range(16):
            tx, ty = x % size, y % size
            if tx == 0 or ty == 0:
                cv.put(x, y, DT[0])
            elif tx == 1 or ty == 1:
                cv.put(x, y, mix(cv.get(x, y), DT[6], 0.35))
            elif tx == size - 1 or ty == size - 1:
                cv.put(x, y, mix(cv.get(x, y), DT[1], 0.5))


def _altar_top(frame: int | None) -> Canvas:
    cv = Canvas()
    stone_fill(cv, DS, 41, 3, 5)
    bevel(cv, DS, light=6, dark=1)
    c = 7.5
    ring = {}
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - c, y - c)
            if 5.0 <= r <= 6.2:
                ring[(x, y)] = (math.atan2(y - c, x - c) / (2 * math.pi)) % 1.0
    # three sockets on the ring, where the runes lie, and the lodestone in the middle
    sockets = [(7, 3), (8, 3), (3, 10), (4, 10), (11, 10), (12, 10)]
    heart = [(7, 7), (8, 7), (7, 8), (8, 8)]
    heart_rim = [(6, 7), (6, 8), (9, 7), (9, 8), (7, 6), (8, 6), (7, 9), (8, 9)]
    # the triangle joining the sockets, cut shallow
    lines = set()
    pts = [(7.5, 3.5), (3.5, 10.5), (11.5, 10.5)]
    for i in range(3):
        (x0, y0), (x1, y1) = pts[i], pts[(i + 1) % 3]
        for k in range(24):
            t = k / 23
            lines.add((int(round(x0 + (x1 - x0) * t - 0.5)), int(round(y0 + (y1 - y0) * t - 0.5))))
    for p in lines:
        if p not in ring and cv.inside(*p):
            cv.put(*p, DS[2])
    for p in heart_rim:
        cv.put(*p, LODE[2])
    for p, a in ring.items():
        if frame is None:
            cv.put(*p, AM[3] if (p[0] + p[1]) % 3 else AM[4])
        else:
            # a bright crest running round the ring
            d = min(abs(a - frame / ALTAR_FRAMES), 1 - abs(a - frame / ALTAR_FRAMES))
            cv.put(*p, glow_at(0.55 + 0.45 * max(0.0, 1 - d * 5)) if d < 0.2 else AM[3 + (p[0] + p[1]) % 2])
    beat = 0.5 if frame is None else pulse(0, frame, width=5)
    for p in sockets:
        cv.put(*p, mix(hexc("#120C1E"), GLOW[4], 0.5 * beat))
    for p in heart:
        cv.put(*p, LODE[4] if p == (7, 7) else LODE[3])
    cv.put(8, 8, glow_at(0.7 + 0.3 * beat))
    for (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)):
        cv.put(x, y, AM[5])
    return cv


def _altar_side() -> Canvas:
    cv = Canvas()
    _tiles(cv, 53)
    # the table's rim: an amethyst band framed in polished deepslate
    for x in range(16):
        cv.put(x, 2, DS[6])
        cv.put(x, 3, AM[4] if x % 4 else AM[5])
        cv.put(x, 4, AM[2] if x % 4 else AM[3])
        cv.put(x, 5, DS[1])
    # the pillar: polished deepslate with a glowing channel running down it
    for y in range(6, 13):
        for x in range(3, 13):
            cv.put(x, y, DS[4] if noise(x, y, 61) > 0.12 else DS[3])
        cv.put(3, y, DS[6])
        cv.put(12, y, DS[1])
    for y in range(6, 13):
        cv.put(7, y, GLOW_DIM if y % 3 else GLOW[3])
        cv.put(8, y, GLOW[2] if y % 3 else GLOW[3])
    for (x, y) in ((5, 8), (10, 8), (5, 10), (10, 10)):
        cv.put(x, y, AM[3])
    # the foot: a lip of lit tile
    for x in range(1, 15):
        cv.put(x, 13, DT[6])
    return cv


def _altar_bottom() -> Canvas:
    cv = Canvas()
    _tiles(cv, 67)
    return cv


def fusion_altar_textures() -> dict:
    """16x16 faces: "top" (an 8-frame list), "side" and "bottom"."""
    return {
        "top": [_altar_top(f).image() for f in range(ALTAR_FRAMES)],
        "side": _altar_side().image(),
        "bottom": _altar_bottom().image(),
    }


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

    altar = fusion_altar_textures()
    _check(altar["top"] + [altar["side"], altar["bottom"]], "fusion_altar")
    rows.append(("Fusion Altar", [("side", altar["side"]), ("bottom", altar["bottom"])]
                 + [(f"top f{i}", im) for i, im in enumerate(altar["top"])]))

    arch, eyes, runes = archivist_texture(), archivist_eyes_texture(), archivist_runes_texture()
    for n, im in (("archivist", arch), ("archivist_eyes", eyes), ("archivist_runes", runes)):
        assert im.size == (128, 64) and im.mode == "RGBA", n
    assert set(arch.getchannel("A").tobytes()) <= {0, 255}, "archivist: partial alpha"
    rows.append(("Archivist (128x64, ArchivistModel layout)", [("skin", arch), ("eyes", _on((10, 8, 16), eyes)),
                                                               ("runes", _on((10, 8, 16), runes))]))
    rows.append(("Archivist views", archivist_views(arch, eyes, runes)))
    marks = runebound_marks_textures()
    rows.append(("Runebound marks (white, tinted in game)", [(n, _on((40, 36, 48), im)) for n, im in marks.items()]))
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
