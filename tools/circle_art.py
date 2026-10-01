"""Every rune's own piece of a magic circle: a ring pattern and an emblem, unique to that rune.

A spell's circle is its runes' rings, stacked from the inside out in threading order, so a caster's
circle can be read by anyone who has learned the patterns (see SpellSigil.java).

Each rune gets two 16x16 white textures, tinted in game by the rune's colour:
    textures/particle/circle/<rune>_band.png   one tile of its ring, laid end to end around the circle
                                               (row 0 is the outer edge; the tile repeats sideways)
    textures/particle/circle/<rune>_mark.png   its emblem, riding the ring on three spokes
A ring's line says the rune's family (Shape: a double line, Effect: solid, Modifier: dashed, Link:
a chain); its motif says the element (flame teeth for fire, crystals for frost, zig-zags for storm,
waves for wind...), and each rune has its own mix of motif, placement and accent. The emblem's
frame says the family (Shape: square, Effect: circle, Modifier: diamond, Link: octagon) and, for
effects, its glyph the element. Designs are handed out by a stable hash of the rune's id, in the
order runes are defined in Runes.java (so a rune added after the others never changes an existing
rune's design), and no two runes share a ring or an emblem.

A fused rune (made at the Fusion Altar from two elements) wears both: its ring is a braid of two
strands, one per element, with the first element's motif outside and the second's inside, and its
emblem is split down the middle, the first element's glyph on the left and the second's on the
right (a fusion of one element with itself shows that element's glyph widened). Each half goes in
its own texture so the game can tint it in its own element's colour:
    textures/particle/circle/<rune>_band.png, _mark.png     the rune's own element's half
    textures/particle/circle/<rune>_band2.png, _mark2.png   its partner element's half

A signature fusion (made from two particular runes rather than any two of their elements) wears its two
runes' elements the same way, and a star besides: a four-pointed star in the empty space of each half of
its ring, and the points of a star behind its emblem's heavy ring, so a signature is told from the element
fusion of the same two elements at a glance. No two signatures share a pair of elements.

Also writes circle/_<family>_band.png and _mark.png, used for add-on runes.
Run from the project root:  python tools/circle_art.py [--preview]
"""
import hashlib
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_assets import read_runes, read_fusions, read_signatures, ELEMENT_COLOR, FAMILY_COLOR  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/wildercord/textures/particle/circle"
N = 16

# ---------------------------------------------------------------- ring motifs (up = outward)

MOTIFS = {
    # fire
    "tooth": ["..#..", ".###.", "#####"],
    "flame": ["..#..", "..#..", ".###.", "#####"],
    "twin": ["#...#", "#...#", "##.##", "#####"],
    "ember": ["..#..", ".....", "..#..", ".###."],
    # frost
    "diamond": ["..#..", ".###.", "#####", ".###.", "..#.."],
    "crystal": ["..#..", "..#..", "#####", "..#..", "..#.."],
    "flake": ["#.#.#", ".###.", "##.##", ".###.", "#.#.#"],
    "shard": ["..#..", ".###.", ".###.", "..#.."],
    # storm
    "bolt": ["...##", "..##.", ".####", ".##..", "##..."],
    "chevron": ["#...#", ".#.#.", "..#.."],
    "fork": ["#...#", ".#.#.", "..#..", "..#.."],
    "spark": ["#...#", ".#.#.", "..#..", ".#.#.", "#...#"],
    # wind
    "wave": [".##..", "#..#.", "....#"],
    "swirl": [".###.", "#...#", "#.#.#", "..##."],
    "arch": [".###.", "#...#", "#...#"],
    "feather": ["..#..", ".#.#.", "#.#.#", "..#.."],
    # earth
    "block": ["#####", "#...#", "#####"],
    "crenel": ["##.##", "##.##", "#####"],
    "step": ["#....", "##...", "###..", "####."],
    "pillar": [".###.", "..#..", "..#..", ".###."],
    # life
    "bud": [".###.", "#...#", "#...#", ".###."],
    "leaf": ["..##.", ".###.", "###..", "#...."],
    "sprout": ["#.#.#", ".###.", "..#..", "..#.."],
    "berry": [".#.#.", "#.#.#", ".#.#."],
    # void
    "crescent": [".###.", "##...", "#....", "##...", ".###."],
    "eye": [".###.", "#.#.#", ".###."],
    "hollow": [".#.#.", "#...#", "#...#", ".###."],
    "rift": ["#.#.#", "#.#.#", "#.#.#"],
    # arcane
    "star": ["..#..", "..#..", "##.##", "..#..", "..#.."],
    "cross": ["..#..", "#####", "..#.."],
    "trine": ["..#..", ".#.#.", "#####"],
    "rune": ["#.#", ".#.", "#.#"],
    # time
    "hourglass": ["#####", ".#.#.", "..#..", ".#.#.", "#####"],
    "dial": [".###.", "#.#.#", "#.###", ".###."],
    "sand": ["#.#.#", ".#.#.", "..#.."],
    "loop": [".#.", "#.#", ".#."],
    # blood
    "drop": ["..#..", ".###.", "#####", ".###."],
    "fang": ["#####", ".###.", "..#.."],
    "vein": ["#.#.#", ".###.", "..#.."],
    "heart": ["##.##", "#####", ".###.", "..#.."],
    # shapes
    "arrow": ["..#..", ".###.", "#.#.#", "..#.."],
    "frame": ["#####", "#...#", "#...#", "#####"],
    "delta": ["..#..", ".#.#.", "#...#", "#####"],
    "spoke": ["..#..", "..#..", "..#..", "..#.."],
    # modifiers
    "dot": ["#"],
    "plus": [".#.", "###", ".#."],
    "caret": [".#.", "#.#"],
    "notch": ["###", ".#."],
    # links
    "hook": ["##.", "..#", ".#."],
    "key": [".#.", "#.#", ".#.", ".#."],
    "knot": ["#.#", "###", "#.#"],
    "ward": ["###", "#.#"],
}

GROUP_MOTIFS = {
    "fire": ["tooth", "flame", "twin", "ember"], "frost": ["diamond", "crystal", "flake", "shard"],
    "storm": ["bolt", "chevron", "fork", "spark"], "wind": ["wave", "swirl", "arch", "feather"],
    "earth": ["block", "crenel", "step", "pillar"], "life": ["bud", "leaf", "sprout", "berry"],
    "void": ["crescent", "eye", "hollow", "rift"], "arcane": ["star", "cross", "trine", "rune"],
    "time": ["hourglass", "dial", "sand", "loop"], "blood": ["drop", "fang", "vein", "heart"],
    "shape": ["arrow", "frame", "delta", "spoke"], "modifier": ["dot", "plus", "caret", "notch"],
    "link": ["hook", "key", "knot", "ward"],
}
PLACEMENTS = ["out", "in", "both", "alt"]
ACCENTS = ["none", "dot", "tick", "bead"]

# The line each family draws: rows lit across the whole tile (or a function for patterned lines).
LINES = {
    "shape": {5: "all", 10: "all"},
    "effect": {7: "all", 8: "all"},
    "modifier": {7: "dash", 8: "dash"},
    "link": "chain",
}


def line_pixels(family):
    lit = set()
    style = LINES[family]
    if style == "chain":
        # Ovals linked end to end, two per tile.
        for base in (0, 8):
            for x in range(base + 1, base + 6):
                lit.add((x, 6))
                lit.add((x, 9))
            for y in (7, 8):
                lit.add((base, y))
                lit.add((base + 6, y))
            lit.add((base + 7, 7))
            lit.add((base + 7, 8))
        return lit
    for y, kind in style.items():
        for x in range(N):
            if kind == "all" or (x % 8) < 6:
                lit.add((x, y))
    return lit


def band_tile(family, motif, placement, accent):
    lit = line_pixels(family)
    rows = [y for _, y in lit]
    outer, inner = min(rows) - 1, max(rows) + 1
    sprite = MOTIFS[motif]
    h, w = len(sprite), len(sprite[0])

    def stamp(cx, flip):
        for j, row in enumerate(sprite):
            for i, ch in enumerate(row):
                if ch != "#":
                    continue
                x = cx - w // 2 + i
                # Out: the motif's bottom row sits on the line; in: mirrored, its top row on the line.
                y = outer - (h - 1 - j) if not flip else inner + (h - 1 - j)
                if 0 <= y < N:
                    lit.add((x % N, y))

    if placement in ("out", "both"):
        stamp(8, False)
    if placement in ("in", "both"):
        stamp(8, True)
    if placement == "alt":
        stamp(4, False)
        stamp(12, True)
    if accent == "dot":
        lit.add((0, max(0, outer - 1)))
    elif accent == "tick":
        lit.add((0, outer))
        lit.add((0, max(0, outer - 1)))
        lit.add((0, inner))
    elif accent == "bead":
        for y in range(min(rows) - 1, max(rows) + 2):
            lit.add((15, y))
            lit.add((0, y))
    return lit


# ---------------------------------------------------------------- emblems

GLYPHS = {
    "fire": ["...#...", "...#...", "..#.#..", "..#.#..", ".#...#.", ".#...#.", "#######"],
    "frost": ["#..#..#", ".#.#.#.", "..###..", "#######", "..###..", ".#.#.#.", "#..#..#"],
    "storm": ["....##.", "...##..", "..##...", ".#####.", "...##..", "..##...", ".##...."],
    "wind": [".......", ".##..#.", "#..##..", ".......", ".##..#.", "#..##..", "......."],
    "earth": ["#######", ".#...#.", ".#####.", "..#.#..", "..#.#..", "...#...", "...#..."],
    "life": ["..###..", ".#...#.", "..###..", "...#...", ".#####.", "...#...", "...#..."],
    "void": ["..###..", ".##....", "##.....", "##.....", "##.....", ".##....", "..###.."],
    "arcane": ["...#...", "...#...", "..###..", "#######", "..###..", "...#...", "...#..."],
    "time": ["#######", ".#...#.", "..#.#..", "...#...", "..#.#..", ".#...#.", "#######"],
    "blood": ["...#...", "..###..", "..###..", ".#####.", ".#####.", ".#####.", "..###.."],
}
GENERIC = [
    ["...#...", "..###..", ".#.#.#.", "#..#..#", "...#...", "...#...", "...#..."],   # arrow
    [".......", "..###..", ".#...#.", ".#.#.#.", ".#...#.", "..###..", "......."],   # ring with a dot
    ["...#...", "...#...", "...#...", "#######", "...#...", "...#...", "...#..."],   # cross
    ["#.....#", ".#...#.", "..#.#..", "...#...", "..#.#..", ".#...#.", "#.....#"],   # saltire
    [".......", "#######", ".......", ".......", ".......", "#######", "......."],   # two bars
    ["#.....#", ".#...#.", "..#.#..", "...#...", ".......", ".......", "......."],   # chevron
    ["...#...", "..#.#..", ".#...#.", "#.....#", ".#...#.", "..#.#..", "...#..."],   # lozenge
    ["#######", "...#...", "...#...", "...#...", "...#...", "...#...", "#######"],   # I-beam
]
VARIANTS = ["plain", "underbar", "dots", "overdot", "sidebars"]
# More ways to mark an element's own glyph, for an element with more runes than VARIANTS gives it
# designs: handed out only after those are all taken, so no existing emblem moves.
EXTRA_VARIANTS = ["overbar", "underdots", "sidedots", "bars", "overdots"]
DECOR = ["none", "ticks2", "ticks4", "corners"]


def frame_pixels(family, heavy=False):
    """The emblem's frame, one pixel wide or (heavy) two."""
    lit = set()
    c = 7.5
    width = 0.95 if heavy else 0.55
    for y in range(N):
        for x in range(N):
            dx, dy = x - c, y - c
            if family == "effect":
                on = abs(math.hypot(dx, dy) - 6.8) < width
            elif family == "shape":
                on = abs(max(abs(dx), abs(dy)) - 7.0 + (0.5 if heavy else 0)) < width
            elif family == "modifier":
                on = abs(abs(dx) + abs(dy) - 7.5 + (0.5 if heavy else 0)) < width + 0.05
            else:
                m = max(abs(dx), abs(dy), (abs(dx) + abs(dy)) / 1.4)
                on = abs(m - 7.0 + (0.5 if heavy else 0)) < width
            if on:
                lit.add((x, y))
    return lit


def mark_tile(family, glyph, variant, decor, heavy):
    lit = frame_pixels(family, heavy)
    # Marks on the frame: ticks sticking out of it, or dots at its corners.
    if decor in ("ticks2", "ticks4"):
        lit.update({(0, 7), (0, 8), (15, 7), (15, 8)})
        if decor == "ticks4":
            lit.update({(7, 0), (8, 0), (7, 15), (8, 15)})
    elif decor == "corners":
        lit.update({(1, 1), (14, 1), (1, 14), (14, 14)})
    for j, row in enumerate(glyph):
        for i, ch in enumerate(row):
            if ch == "#":
                lit.add((4 + i, 4 + j))
    if variant == "underbar":
        for x in range(5, 11):
            lit.add((x, 12))
    elif variant == "dots":
        lit.update({(5, 12), (10, 12)})
    elif variant == "overdot":
        lit.add((7, 2))
        lit.add((8, 2))
    elif variant == "sidebars":
        for y in range(6, 10):
            lit.add((2, y))
            lit.add((13, y))
    elif variant == "overbar":
        for x in range(5, 11):
            lit.add((x, 2))
    elif variant == "underdots":
        lit.update({(5, 12), (7, 12), (8, 12), (10, 12)})
    elif variant == "sidedots":
        lit.update({(2, 7), (2, 8), (13, 7), (13, 8)})
    elif variant == "bars":
        for x in range(5, 11):
            lit.add((x, 2))
            lit.add((x, 12))
    elif variant == "overdots":
        lit.update({(5, 2), (10, 2)})
    return lit


# ---------------------------------------------------------------- fused runes

# The motif each element shows in a fused rune's braid (a fusion of one element with itself shows two of its own).
FUSED_MOTIF = {"fire": "tooth", "frost": "diamond", "storm": "bolt", "wind": "wave", "earth": "block", "life": "bud",
               "void": "crescent", "arcane": "star", "time": "hourglass", "blood": "drop"}
# The first twelve fused runes wore single-element designs before fused runes had their own; they still
# hold those designs in the handing out, so no other rune's design moves.
LEGACY_FUSED = {"firestorm", "steam", "magma", "tempest", "plasma", "hail", "glacier", "lifesteal", "warp", "bloom", "surge", "nullify"}


def fused_pair(rune, fusions):
    """A fused rune's (own, partner) elements: its own element first if it's one of the two it was fused from."""
    a, b = fusions[rune["path"]]
    own = rune["element"] if rune["element"] in (a, b) else a
    return own, (b if own == a else a)


def braid_strands():
    """The fused ring's line: two strands crossing twice a tile, each a pixel thick and unbroken."""
    first, second = set(), set()
    prev = None
    for x in range(N):
        wave = 1.5 * math.sin(2 * math.pi * (x + 0.5) / N)
        y1, y2 = round(7.5 - wave - 0.01), round(7.5 + wave + 0.01)
        first.add((x, y1))
        second.add((x, y2))
        if prev is not None:
            # Fill a step of two rows so the strand never breaks.
            for y in range(min(prev[0], y1) + 1, max(prev[0], y1)):
                first.add((x, y))
            for y in range(min(prev[1], y2) + 1, max(prev[1], y2)):
                second.add((x, y))
        prev = (y1, y2)
    return first, second


def fused_band(own, partner):
    """(own half, partner half) of a fused rune's ring: a strand and a motif each, one outside, one inside."""
    first, second = braid_strands()
    rows = [y for _, y in first | second]
    outer, inner = min(rows) - 1, max(rows) + 1
    same = own == partner
    outside = FUSED_MOTIF[own]
    inside = GROUP_MOTIFS[own][1] if same else FUSED_MOTIF[partner]

    def stamp(lit, motif, cx, flip):
        sprite = MOTIFS[motif]
        h, w = len(sprite), len(sprite[0])
        for j, row in enumerate(sprite):
            for i, ch in enumerate(row):
                if ch == "#":
                    y = outer - (h - 1 - j) if not flip else inner + (h - 1 - j)
                    if 0 <= y < N:
                        lit.add(((cx - w // 2 + i) % N, y))

    stamp(first, outside, 4, False)
    stamp(second, inside, 12, True)
    return first, second


def half(glyph):
    """A glyph's fuller half and its middle column, as four columns reading outward-in (the middle last)."""
    left = [row[:4] for row in glyph]
    right = [row[3:][::-1] for row in glyph]
    lit = lambda rows: sum(row.count("#") for row in rows)
    return left if lit(left) >= lit(right) else right


def fused_mark(own, partner):
    """(own half, partner half) of a fused rune's emblem: a heavy ring round a glyph split down the middle."""
    left, right = frame_pixels("effect", heavy=True), set()
    # Each element shows the fuller half of its glyph (with its middle column), turned to face the seam
    # if need be, so a lopsided glyph like void's crescent isn't cut down to a sliver: the own element
    # on the left, the partner on the right, eight columns in all, centred in the tile.
    for j, row in enumerate(half(GLYPHS[own])):
        for i, ch in enumerate(row):
            if ch == "#":
                left.add((4 + i, 4 + j))
    for j, row in enumerate(half(GLYPHS[partner])):
        for i, ch in enumerate(row):
            if ch == "#":
                right.add((11 - i, 4 + j))
    # A seam where the halves meet, above and below the glyph.
    left.update({(7, 2), (7, 12)})
    right.update({(8, 2), (8, 12)})
    return left, right


# ---------------------------------------------------------------- signature fusions

# A small four-pointed star: its centre and the four pixels round it.
STAR = [(0, -1), (-1, 0), (0, 0), (1, 0), (0, 1)]


def signature_band(own_half, partner_half):
    """A signature rune's ring: its braid (see {@code fused_band}), with a star in the empty space of each half, the
    own element's above the braid (between its motifs) and the partner's below it, clear of the strands."""
    own, partner = set(own_half), set(partner_half)
    own.update(((12 + dx) % N, 3 + dy) for dx, dy in STAR)
    partner.update(((4 + dx) % N, 12 + dy) for dx, dy in STAR)
    return own, partner


def signature_mark(own_half, partner_half):
    """A signature rune's emblem: its split glyph in a heavy ring (see {@code fused_mark}), with the points of a star
    reaching out behind the ring at the four corners, the left two in the own element's colour, the right two in
    the partner's."""
    own, partner = set(own_half), set(partner_half)
    own.update({(0, 0), (1, 1), (0, 15), (1, 14)})
    partner.update({(15, 0), (14, 1), (15, 15), (14, 14)})
    return own, partner


# ---------------------------------------------------------------- handing out designs

def stable(key):
    return int(hashlib.sha1(key.encode("utf-8")).hexdigest()[:8], 16)


def assign(keys, options, render, used, spare=(), spare_render=None):
    """
    Gives each key its own design: its hashed favourite among {@code options}, or the next one
    along whose picture ({@code render}) nobody has yet ({@code used}, shared across groups). A group
    with more runes than designs goes on to {@code spare} (drawn by {@code spare_render}) only once its
    own are all taken, so adding spares never moves anyone's design.
    """
    chosen = {}
    # In the order the runes are defined, so a rune added after the others never takes a design from
    # one that came before it: the emblems players have learned stay put.
    for key in keys:
        start = stable(key) % len(options)
        for step in range(len(options)):
            pixels = frozenset(render(options[(start + step) % len(options)]))
            if pixels not in used:
                used.add(pixels)
                chosen[key] = pixels
                break
        else:
            for option in spare:
                pixels = frozenset(spare_render(option))
                if pixels not in used:
                    used.add(pixels)
                    chosen[key] = pixels
                    break
            else:
                raise AssertionError(f"ran out of designs for {key}")
    return chosen


def group_of(rune):
    return rune["element"] if rune["family"] == "effect" and rune["element"] else rune["family"]


def designs(runes, skip=frozenset()):
    """Every rune's ring and emblem from its element or family group, bar those in {@code skip}."""
    groups = {}
    for r in runes:
        if r["path"] not in skip:
            groups.setdefault(group_of(r), []).append(r["path"])
    bands, marks = {}, {}
    used_bands, used_marks = set(), set()
    for group in sorted(groups):
        paths = groups[group]
        family = next(r["family"] for r in runes if r["path"] == paths[0])
        motifs = GROUP_MOTIFS.get(group, GROUP_MOTIFS["arcane"])
        band_options = [(m, p, a) for a in ACCENTS for p in PLACEMENTS for m in motifs]
        # Spares: every other group's motifs, for a group that has outgrown its own.
        other_motifs = [m for g in sorted(GROUP_MOTIFS) for m in GROUP_MOTIFS[g] if m not in motifs]
        band_spares = [(m, p, a) for m in other_motifs for a in ACCENTS for p in PLACEMENTS]
        bands.update(assign(paths, band_options, lambda o: band_tile(family, *o), used_bands,
            band_spares, lambda o: band_tile(family, *o)))
        glyphs = [GLYPHS[group]] if group in GLYPHS else GENERIC
        mark_options = [(g, v, e, h) for h in (False, True) for e in DECOR for v in VARIANTS for g in range(len(glyphs))]
        # Spares, for an element with more runes than its own emblem has designs: its own glyph marked
        # new ways first (so the emblem still says the element), then the generic emblems.
        mark_spares = []
        if group in GLYPHS:
            mark_spares = [(glyphs[0], v, e, h) for h in (False, True) for e in DECOR for v in EXTRA_VARIANTS]
            mark_spares += [(GENERIC[g], v, e, h) for g in range(len(GENERIC)) for h in (False, True) for e in DECOR for v in VARIANTS]
        marks.update(assign(paths, mark_options, lambda o: mark_tile(family, glyphs[o[0]], o[1], o[2], o[3]), used_marks,
            mark_spares, lambda o: mark_tile(family, *o)))
    return bands, marks


def image(lit):
    img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    for x, y in lit:
        img.putpixel((x, y), (255, 255, 255, 255))
    return img


# ---------------------------------------------------------------- preview

def ring_preview(band, mark, color, size=150, band2=(), mark2=(), color2=None):
    """A rune's ring drawn as a full circle (the band unwrapped around it), with its emblem on three spokes;
    a fused rune's second half ({@code band2}, {@code mark2}) in {@code color2}."""
    img = Image.new("RGBA", (size, size), (20, 16, 30, 255))
    px = img.load()
    c = size / 2
    radius, height = size * 0.34, size * 0.1
    tiles = max(8, round(2 * math.pi * radius / height))
    for y in range(size):
        for x in range(size):
            dx, dy = x + 0.5 - c, y + 0.5 - c
            r = math.hypot(dx, dy)
            v = (radius + height / 2 - r) / height
            if 0 <= v < 1:
                a = (math.atan2(dy, dx) / (2 * math.pi)) % 1.0
                u = (a * tiles) % 1.0
                if (int(u * N), int(v * N)) in band:
                    px[x, y] = color + (255,)
                elif (int(u * N), int(v * N)) in band2:
                    px[x, y] = color2 + (255,)
    m = Image.new("RGBA", (int(height * 1.4), int(height * 1.4)), (0, 0, 0, 0))
    for lit, col in ((mark, color), (mark2, color2)):
        if lit:
            layer = image(lit).resize(m.size, Image.NEAREST)
            m.alpha_composite(Image.composite(Image.new("RGBA", m.size, col + (255,)), Image.new("RGBA", m.size, (0, 0, 0, 0)), layer.split()[3]))
    for k in range(3):
        a = -math.pi / 2 + k * 2 * math.pi / 3
        rot = m.rotate(-math.degrees(a + math.pi / 2), expand=True)
        img.alpha_composite(rot, (int(c + math.cos(a) * radius - rot.width / 2), int(c + math.sin(a) * radius - rot.height / 2)))
    return img


def color_of(rune):
    if rune["family"] == "effect":
        return ELEMENT_COLOR.get(rune["element"], (0xE6, 0x78, 0xDC))
    return FAMILY_COLOR[rune["family"]]


def second_color(own, partner):
    """A fused rune's second colour: its partner element's, or for one element with itself, its own lightened."""
    base = ELEMENT_COLOR[partner]
    return tuple(round(c + (255 - c) * 0.45) for c in base) if own == partner else base


def main(preview=False):
    # No two motifs may look alike: they are what tells the elements apart.
    sprites = [tuple(v) for v in MOTIFS.values()]
    assert len(set(sprites)) == len(sprites), "two motifs are the same picture"
    runes = read_runes()
    element = {r["path"]: r["element"] for r in runes}
    # A signature rune's two elements are its two runes'.
    signatures = {path: (element[a], element[b]) for path, (a, b) in read_signatures().items()}
    fusions = {**read_fusions(), **signatures}
    fused = [r for r in runes if r["path"] in fusions]
    bands, marks = designs(runes, skip=frozenset(r["path"] for r in fused if r["path"] not in LEGACY_FUSED))
    # Fused runes wear their two elements instead (the legacy twelve's old designs stay held, unused).
    second = {}
    for r in fused:
        own, partner = fused_pair(r, fusions)
        band, band2 = fused_band(own, partner)
        mark, mark2 = fused_mark(own, partner)
        if r["path"] in signatures:
            band, band2 = signature_band(band, band2)
            mark, mark2 = signature_mark(mark, mark2)
        bands[r["path"]], marks[r["path"]] = band, mark
        second[r["path"]] = (band2, mark2, second_color(own, partner))
    import physical_art
    physical_art.circles(bands, marks, second)
    import circle_discipline_art
    circle_discipline_art.circles(bands, marks)
    # Every rune's ring and emblem must be its own (a fused rune's counted with both halves).
    def whole(table, path, index):
        return frozenset(table[path]) | (frozenset(second[path][index]) if path in second else frozenset())
    band_keys = {whole(bands, path, 0) for path in bands}
    mark_keys = {whole(marks, path, 1) for path in marks}
    assert len(band_keys) == len(bands), "two runes share a ring"
    assert len(mark_keys) == len(marks), "two runes share an emblem"
    OUT.mkdir(parents=True, exist_ok=True)
    for path in bands:
        image(bands[path]).save(OUT / f"{path}_band.png")
        image(marks[path]).save(OUT / f"{path}_mark.png")
        for part in ("band2", "mark2"):
            extra = OUT / f"{path}_{part}.png"
            if path in second:
                image(second[path][0 if part == "band2" else 1]).save(extra)
            elif extra.exists():
                extra.unlink()
    for family, motif, glyph in (("shape", "spoke", GENERIC[2]), ("effect", "cross", GENERIC[6]), ("modifier", "dot", GENERIC[1]), ("link", "knot", GENERIC[4])):
        image(band_tile(family, motif, "out", "none")).save(OUT / f"_{family}_band.png")
        image(mark_tile(family, glyph, "plain", "none", False)).save(OUT / f"_{family}_mark.png")
    if preview:
        out = ROOT / "build/art-preview"
        out.mkdir(parents=True, exist_ok=True)
        cols, cell = 12, 150
        rows = (len(runes) + cols - 1) // cols
        sheet = Image.new("RGBA", (cols * cell, rows * (cell + 14)), (12, 10, 18, 255))
        draw = ImageDraw.Draw(sheet)
        for i, r in enumerate(runes):
            x, y = (i % cols) * cell, (i // cols) * (cell + 14)
            band2, mark2, color2 = second.get(r["path"], ((), (), None))
            sheet.alpha_composite(ring_preview(bands[r["path"]], marks[r["path"]], color_of(r), cell, band2, mark2, color2), (x, y + 14))
            draw.text((x + 4, y + 1), r["name"], fill=(230, 225, 245))
        sheet.save(out / "rune_circles.png")
    print(f"{len(bands)} rune rings and emblems written ({len(fused)} fused, in two halves, {len(signatures)} of them signatures)")


if __name__ == "__main__":
    main("--preview" in sys.argv)
