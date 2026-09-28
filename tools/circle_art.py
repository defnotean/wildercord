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

Also writes circle/_<family>_band.png and _mark.png, used for add-on runes.
Run from the project root:  python tools/circle_art.py [--preview]
"""
import hashlib
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_assets import read_runes, ELEMENT_COLOR, FAMILY_COLOR  # noqa: E402

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


def designs(runes):
    groups = {}
    for r in runes:
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

def ring_preview(band, mark, color, size=150):
    """A rune's ring drawn as a full circle (the band unwrapped around it), with its emblem on three spokes."""
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
    m = image(mark).resize((int(height * 1.4), int(height * 1.4)), Image.NEAREST)
    tint = Image.new("RGBA", m.size, color + (255,))
    m = Image.composite(tint, Image.new("RGBA", m.size, (0, 0, 0, 0)), m.split()[3])
    for k in range(3):
        a = -math.pi / 2 + k * 2 * math.pi / 3
        rot = m.rotate(-math.degrees(a + math.pi / 2), expand=True)
        img.alpha_composite(rot, (int(c + math.cos(a) * radius - rot.width / 2), int(c + math.sin(a) * radius - rot.height / 2)))
    return img


def color_of(rune):
    if rune["family"] == "effect":
        return ELEMENT_COLOR.get(rune["element"], (0xE6, 0x78, 0xDC))
    return FAMILY_COLOR[rune["family"]]


def main(preview=False):
    # No two motifs may look alike: they are what tells the elements apart.
    sprites = [tuple(v) for v in MOTIFS.values()]
    assert len(set(sprites)) == len(sprites), "two motifs are the same picture"
    runes = read_runes()
    bands, marks = designs(runes)
    # Every rune's ring and emblem must be its own.
    band_keys = {frozenset(v) for v in bands.values()}
    mark_keys = {frozenset(v) for v in marks.values()}
    assert len(band_keys) == len(bands), "two runes share a ring"
    assert len(mark_keys) == len(marks), "two runes share an emblem"
    OUT.mkdir(parents=True, exist_ok=True)
    for path in bands:
        image(bands[path]).save(OUT / f"{path}_band.png")
        image(marks[path]).save(OUT / f"{path}_mark.png")
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
            sheet.alpha_composite(ring_preview(bands[r["path"]], marks[r["path"]], color_of(r), cell), (x, y + 14))
            draw.text((x + 4, y + 1), r["name"], fill=(230, 225, 245))
        sheet.save(out / "rune_circles.png")
    print(f"{len(bands)} rune rings and emblems written")


if __name__ == "__main__":
    main("--preview" in sys.argv)
