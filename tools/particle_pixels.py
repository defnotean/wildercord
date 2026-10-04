"""Pixel art for the physical material particles (earth, life, void material, camp).

These particles are tinted in game by the colour each spell sends (EarthParticle, LifeParticle,
VoidParticle, CampParticle all call setColor), so, like every Wildercord particle texture, they are
drawn in greys on transparency: the tint gives the hue, the greys give the form. White takes the
tint exactly, so the light tones are where the sent colour shows; the dark outline becomes a deep
shade of the same hue.

Each sprite is a 16x16 tone grid, doubled to the 32x32 texture the particle atlas expects, so it
keeps the chunky, readable pixels of vanilla's particles at the sizes these are drawn (a quarter of
a block or so). Light comes from the top left: highlights on top/left edges, shadow bottom/right.

Tones (one character each):
    .  clear
    K D B   very dark body tones (void matter's interior)
    o       outline
    k       deep shade (cracks, seams, the far side)
    s       shadow
    m       mid tone
    l       light
    h       highlight (white: the tint itself)
    r       a rim of light on dark matter
    1 2 3   soft tones (light, mid, shadow) at lower opacity, for dust, spores and haze
"""
from PIL import Image

N = 16
SCALE = 2

TONES = {
    "K": (22, 255), "D": (38, 255), "B": (56, 255),
    "o": (70, 255), "k": (104, 255), "s": (146, 255), "m": (192, 255), "l": (226, 255), "h": (255, 255),
    "r": (206, 255),
    "1": (236, 170), "2": (200, 170), "3": (160, 170),
}


def grid(rows):
    """A tone grid from 16 strings of 16 characters (checked, so a typo fails loudly)."""
    assert len(rows) == N, f"{len(rows)} rows"
    for row in rows:
        assert len(row) == N, f"row {row!r} is {len(row)} wide"
        assert set(row) <= set(TONES) | {"."}, f"unknown tone in {row!r}"
    return [list(row) for row in rows]


def blank():
    return [["."] * N for _ in range(N)]


def edit(rows, cells):
    """A copy of a grid with some cells changed: cells is 'x,y,tone x,y,tone ...'."""
    out = [list(r) for r in rows]
    for cell in cells.split():
        x, y, tone = cell.split(",")
        assert tone in TONES or tone == ".", tone
        out[int(y)][int(x)] = tone
    return out


def place(rows, piece, x, y):
    """A copy of a grid with a small piece (list of strings) laid over it at (x, y); '.' leaves the grid."""
    out = [list(r) for r in rows]
    for j, line in enumerate(piece):
        for i, tone in enumerate(line):
            if tone != "." and 0 <= x + i < N and 0 <= y + j < N:
                out[y + j][x + i] = tone
    return out


def compose(pieces):
    """A grid built from small pieces: [(piece, x, y), ...]."""
    rows = blank()
    for piece, x, y in pieces:
        rows = place(rows, piece, x, y)
    return rows


def render(rows):
    """The 32x32 greyscale texture for a grid."""
    if isinstance(rows[0], str):
        rows = grid(rows)
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    px = im.load()
    for y, row in enumerate(rows):
        for x, tone in enumerate(row):
            if tone != ".":
                v, a = TONES[tone]
                px[x, y] = (v, v, v, a)
    return im.resize((N * SCALE, N * SCALE), Image.NEAREST)


# How shade() lights a silhouette: the outline (on edges facing the light, and the rest), the
# first and second bands in from a lit edge, the body, and the band in from a shaded edge.
MATTER = {"edge_lit": "o", "edge": "o", "hi": "h", "hi2": "l", "body": "m", "lo": "s"}
# Void matter is dark: a rim of light on its lit edges, a near-black body.
DARK = {"edge_lit": "r", "edge": "o", "hi": "B", "hi2": "D", "body": "K", "lo": "K"}


def shade(rows, ramp=MATTER):
    """
    Lights a silhouette drawn with '#': its edge becomes the outline, the pixels just inside a top or
    left edge the highlight (and the next ones in a lighter band), those just inside a bottom or right
    edge the shadow, the rest the body tone. Any tone character in the drawing is kept as drawn (veins,
    seams, cracks), and counts as body when the outline is worked out.
    """
    rows = [list(r) for r in rows]
    assert len(rows) == N and all(len(r) == N for r in rows), "a silhouette is 16 rows of 16"

    def inside(x, y):
        return 0 <= x < N and 0 <= y < N and rows[y][x] != "."

    edge = {(x, y) for y in range(N) for x in range(N) if inside(x, y)
            and not all(inside(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))}
    out = blank()
    for y in range(N):
        for x in range(N):
            if not inside(x, y):
                continue
            drawn = rows[y][x]
            if (x, y) in edge:
                lit = not inside(x - 1, y) or not inside(x, y - 1)
                out[y][x] = ramp["edge_lit"] if lit else ramp["edge"]
            elif drawn != "#":
                out[y][x] = drawn
            elif (x - 1, y) in edge or (x, y - 1) in edge:
                out[y][x] = ramp["hi"]
            elif (x + 1, y) in edge or (x, y + 1) in edge or (x + 1, y + 1) in edge:
                out[y][x] = ramp["lo"]
            elif (x - 2, y) in edge or (x, y - 2) in edge or (x - 1, y - 1) in edge:
                out[y][x] = ramp["hi2"]
            else:
                out[y][x] = ramp["body"]
    return out
