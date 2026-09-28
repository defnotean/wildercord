"""Hand-tuned 16x16 art for Wildercord's casting gear: the ten elemental staffs and their greater
versions, the Tome of the Fifth Page and the four foci.

Same house style as item_art.py: ASCII pictograms, ramps lit from the top-left, no anti-aliasing.
Each part is drawn without its outline; `outline()` then rings every part in a darker shade of
whatever it touches, so a staff's gem, cradle and shaft each get their own edge.

A staff is a shaft (a stick, or for fire a blaze rod; ebony banded in gold for a greater staff) with a
metal cradle holding a head that is its element's own shape, cut from the element's gem colours
(item_art.ELEMENT): a flame, an ice shard, a lightning gem, a swirling orb, a rough stone, a bud,
a dark orb, a star, an hourglass, a drop. Greater staffs set the head in gold, add a halo of
sparks, and their head breathes.

Public API (imported by generate_assets.py):
    staff_icon(element, greater) -> list[Image]   (16x16; greater staffs are animated)
    tome_icon() -> list[Image]                    (16x16, animated)
    focus_icon(kind) -> list[Image]               (16x16; kind: haste, thrift, deep_well, echoes)

Run this file directly to render a review sheet into build/art-preview/gear_art.png.
"""
from __future__ import annotations

import math
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from item_art import ANIM_FRAMES, ELEMENT, Canvas, blit, grid, hexc, mix, ramp  # noqa: E402

N4 = ((1, 0), (-1, 0), (0, 1), (0, -1))
BLACK = (0, 0, 0)


def outline(cv: Canvas, strength: float = 0.68):
    """Rings every opaque pixel in a darker shade of itself (the darkest neighbour wins)."""
    add = {}
    for y in range(cv.n):
        for x in range(cv.n):
            if cv.get(x, y) is not None:
                continue
            best = None
            for dx, dy in N4:
                c = cv.get(x + dx, y + dy)
                if c is not None and (best is None or sum(c) < sum(best)):
                    best = c
            if best is not None:
                add[(x, y)] = mix(best, BLACK, strength)
    for (x, y), c in add.items():
        cv.put(x, y, c)


# ============================================================== staffs

# The shaft: a two-pixel diagonal from the grip at the bottom left to the cradle at the top right.
# 'S' lit edge, 's' grain, 'd' shaded edge, 'w'/'W' a leather grip.
SHAFT = """
    ................
    ................
    ................
    ................
    ................
    ................
    .........M......
    ........Sd......
    .......Sd.......
    ......sd........
    .....Sd.........
    ....WW..........
    ...Ww...........
    ..Sd............
    .sd.............
    ................
"""

# The cradle that holds the head, continuing the shaft's diagonal.
CRADLE = {(9, 6): "M", (10, 6): "n", (10, 5): "M", (11, 5): "m"}

SHAFTS = {
    "wood": {"S": hexc("#B8864A"), "s": hexc("#94683A"), "d": hexc("#6A4622"), "W": hexc("#7A3A2A"), "w": hexc("#54261C")},
    "blaze": {"S": hexc("#FFE08A"), "s": hexc("#F4B03A"), "d": hexc("#C8701C"), "W": hexc("#7A3A2A"), "w": hexc("#54261C")},
    "ebony": {"S": hexc("#6E5A80"), "s": hexc("#4E3E5E"), "d": hexc("#2E2238"), "W": hexc("#F6DA84"), "w": hexc("#B8841E")},
}
METALS = {
    "silver": {"M": hexc("#E2E2EC"), "m": hexc("#A2A2B4"), "n": hexc("#646478")},
    "gold": {"M": hexc("#FFF0B0"), "m": hexc("#E8B83A"), "n": hexc("#A8741A")},
}

# Heads: 6x6, placed with their top-left at (10, 0). '*' the gem's core, '+' light, '#' main,
# '-' shade, 'k' dark, 'M'/'m'/'n' metal of the cradle.
HEADS = {
    # a flame, licking up and to the right
    "fire": """
        ...+..
        ..+#..
        .+*#-.
        +#*#-.
        .#*-k.
        Mm#k..
    """,
    # a long ice shard
    "frost": """
        .....+
        ....+*
        ...+*-
        ..+*#-
        .+#-k.
        Mm-k..
    """,
    # a gem split by a lightning zigzag
    "storm": """
        ..++#.
        .+*#..
        +**+..
        ..*#-.
        .+#-k.
        Mm-...
    """,
    # an orb with a swirl running through it
    "wind": """
        ..++#.
        .+*..-
        +*.+#-
        +#.*-k
        .#--k.
        Mm....
    """,
    # a rough chunk of stone with a bright seam
    "earth": """
        ..#+..
        .#+##-
        #+*#-k
        #+#*-k
        .#--k.
        Mm....
    """,
    # a bud between two leaves
    "life": """
        .+..#.
        +#.+#-
        .+*+-.
        .#*#-.
        ..#-..
        Mm....
    """,
    # a dark orb with a bright rim
    "void": """
        ..++..
        .+kk#.
        +kk*k-
        #k*kk-
        .#kk-.
        Mm--..
    """,
    # a four-pointed star
    "arcane": """
        ...+..
        ...*..
        .+**#-
        ...*-.
        ..#-..
        Mm.#..
    """,
    # an hourglass in a metal frame
    "time": """
        .MMMm.
        ..+#..
        ...*..
        ..+#..
        .Mmmn.
        Mm....
    """,
    # a drop of blood
    "blood": """
        ...+..
        ..+#..
        .+*#-.
        .+*#-.
        ..#-k.
        Mm-k..
    """,
}

# A greater staff's halo: sparks round the head.
SPARKS = [(9, 1), (15, 6), (13, 7)]


def _gem(element: str) -> dict:
    pal = ELEMENT[element]
    fa, gl = pal["facet"], pal["glow"]
    return {"k": fa[0], "-": fa[1], "#": fa[2], "+": fa[3], "*": gl[3]}


# Where a greater staff's sparks can be: a ring round its head, travelled a step a frame.
ORBIT = [(12, -1), (14, 0), (15, 2), (15, 4), (13, 6), (11, 6), (9, 5), (8, 3), (9, 1), (10, 0)]


def _orbit(frame: int) -> list:
    """Three sparks spaced round the ring, moving on a step each frame."""
    n = len(ORBIT)
    return [ORBIT[(frame + k * n // 3) % n] for k in range(3)]


def _staff(element: str, greater: bool, glow: float = 0.0, orbit=tuple(SPARKS)) -> Canvas:
    cv = Canvas()
    shaft = SHAFTS["ebony" if greater else "blaze" if element == "fire" else "wood"]
    metal = METALS["gold" if greater else "silver"]
    blit(cv, SHAFT, {**shaft, **metal})
    if greater:
        # gold bands up the ebony
        for x, y in [(7, 8), (5, 10)]:
            cv.put(x, y, metal["M"])
    for (x, y), ch in CRADLE.items():
        cv.put(x, y, metal[ch])
    gem = _gem(element)
    for y, row in enumerate(grid(HEADS[element])):
        for x, ch in enumerate(row):
            if ch in gem:
                c = gem[ch]
                if glow > 0 and ch in "*+":
                    c = mix(c, ELEMENT[element]["glow"][3], glow)
                cv.put(10 + x, y, c)
            elif ch in metal:
                cv.put(10 + x, y, metal[ch])
    if greater:
        # A pommel gem at the foot, capped in gold.
        cv.put(1, 14, gem["#"])
        cv.put(0, 15, metal["m"])
        # Gold prongs cupping the head.
        for (x, y), ch in {(9, 3): "M", (9, 2): "m", (8, 4): "n", (14, 6): "M", (15, 5): "m"}.items():
            if cv.get(x, y) is None:
                cv.put(x, y, metal[ch])
    outline(cv)
    if greater:
        # A soft halo of the element's light round the head, and sparks circling it.
        halo = ELEMENT[element]["glow"][1]
        for x, y in [(15, 0), (8, 2), (15, 7)]:
            if cv.get(x, y) is None:
                cv.put(x, y, mix(halo, (30, 24, 44), 0.55 - 0.25 * glow))
        spark = mix(ELEMENT[element]["glow"][2], (255, 255, 255), 0.3 + 0.5 * glow)
        for x, y in orbit:
            if 0 <= x < 16 and 0 <= y < 16 and cv.get(x, y) is None:
                cv.put(x, y, spark)
    return cv


def staff_icon(element: str, greater: bool = False) -> list[Image.Image]:
    """A staff of an element: one frame, or for a greater staff a head that breathes."""
    if not greater:
        return [_staff(element, False).image()]
    frames = []
    for f in range(ANIM_FRAMES):
        glow = 0.5 + 0.5 * math.cos(2 * math.pi * f / ANIM_FRAMES)
        frames.append(_staff(element, True, 0.6 * glow, _orbit(f)).image())
    return frames


# ============================================================== the Tome of the Fifth Page

TOME_PAL = {
    "V": hexc("#8E5CE0"), "v": hexc("#6A3CB8"), "u": hexc("#4A2688"), "B": hexc("#3A1C6A"),
    "P": hexc("#F4E8C4"), "p": hexc("#CBB07A"),
    "G": hexc("#F6DA84"), "g": hexc("#B8841E"),
    "Y": hexc("#FFF6D0"), "y": hexc("#E8C860"),
}

# A thick violet tome with gold corners, its pages at the right, and a five-pointed star on the cover.
TOME = """
    ................
    ..GVVVVVVVVVG...
    ..VvvvvvvvvvuP..
    ..Vvvvvvyvvvupp.
    ..Vvvvvvyvvvupp.
    ..VvyyyYYyyvupp.
    ..VvvyYYYyvvupp.
    ..VvvvYyYvvvupp.
    ..VvvyvvvyvvupP.
    ..Vvvvvvvvvvupp.
    ..Vvvvvvvvvvupp.
    ..VvvvvvvvvvupP.
    ..Vvvvvvvvvvupp.
    ..guuuuuuuuugpp.
    ...BBBBBBBBBBp..
    ................
"""
STAR = {(8, 3), (8, 4), (5, 5), (6, 5), (7, 5), (8, 5), (9, 5), (10, 5), (6, 6), (7, 6), (8, 6), (9, 6), (6, 7), (7, 7), (8, 7), (9, 7),
        (5, 8), (10, 8)}


def tome_icon() -> list[Image.Image]:
    """The Tome of the Fifth Page: its star brightens and dims."""
    frames = []
    for f in range(ANIM_FRAMES):
        glow = 0.5 + 0.5 * math.cos(2 * math.pi * f / ANIM_FRAMES)
        cv = Canvas()
        blit(cv, TOME, TOME_PAL)
        for x, y in STAR:
            c = cv.get(x, y)
            if c is not None:
                cv.put(x, y, mix(c, (255, 255, 255), 0.45 * glow))
        outline(cv)
        frames.append(cv.image())
    return frames


# ============================================================== foci

# Each focus is a small orb in a setting, with its own motif.
FOCI = {
    # Haste: a teal orb on swept-back wings
    "haste": ("""
        ................
        ................
        .....++##.......
        ....+*+###......
        ww..+++##-..ww..
        wWw.###--k.wWw..
        .wWwM##-knwWw...
        ..wWWMmmnWWw....
        ...wwwMmnww.....
        ......Mmn.......
        .......n........
        ................
        ................
        ................
        ................
        ................
    """, {"*": hexc("#F2FFF8"), "+": hexc("#A2F0DC"), "#": hexc("#4ED8CC"), "-": hexc("#2A968F"), "k": hexc("#175654"),
          "W": hexc("#F2F6FA"), "w": hexc("#B4C4D8"), "M": hexc("#FFF0B0"), "m": hexc("#E8B83A"), "n": hexc("#A8741A")}),
    # Thrift: a gold coin set with a small green gem
    "thrift": ("""
        ................
        ................
        .....MMMMm......
        ....MmmmmMm.....
        ...MmM+#Mmmn....
        ...Mm+*#-mmn....
        ...Mm##-kmmn....
        ...MmM-kMmmn....
        ....Mmmmmmn.....
        .....mnnnn......
        ................
        ................
        ................
        ................
        ................
        ................
    """, {"*": hexc("#F2FFEA"), "+": hexc("#92E27E"), "#": hexc("#50AC44"), "-": hexc("#32842E"), "k": hexc("#18441A"),
          "M": hexc("#FFF0B0"), "m": hexc("#E8B83A"), "n": hexc("#A8741A")}),
    # the Deep Well: a deep blue orb sunk in a ring of stone
    "deep_well": ("""
        ................
        ................
        .....ssSSs......
        ....sS+##ss.....
        ...sS+*#--sd....
        ...s+*#--kkd....
        ...s##--kkkd....
        ...sd-kkkkdd....
        ....sddkkddd....
        .....dddddd.....
        ................
        ................
        ................
        ................
        ................
        ................
    """, {"*": hexc("#CCF2FF"), "+": hexc("#56AEE4"), "#": hexc("#2C63A0"), "-": hexc("#1D4374"), "k": hexc("#0F2244"),
          "S": hexc("#9A9AA8"), "s": hexc("#6E6E7E"), "d": hexc("#46464F")}),
    # Echoes: a pale orb with ripples ringing out from it
    "echoes": ("""
        ................
        ..r..........r..
        .r....++#.....r.
        .r...+*+##....r.
        r...+*+##-.....r
        r...+++##-.....r
        r...###--k.....r
        .r...##-k.....r.
        .r....--k.....r.
        ..r...Mmn....r..
        ......Mmn.......
        .......n........
        ................
        ................
        ................
        ................
    """, {"*": hexc("#FFF1FC"), "+": hexc("#FFBAF4"), "#": hexc("#C24EBC"), "-": hexc("#943492"), "k": hexc("#682266"),
          "r": hexc("#CDB2FF"), "M": hexc("#E2E2EC"), "m": hexc("#A2A2B4"), "n": hexc("#646478")}),
}


def focus_icon(kind: str) -> list[Image.Image]:
    """A focus. The Focus of Echoes' ripples pulse outward; the others are still."""
    art, pal = FOCI[kind]
    if kind != "echoes":
        cv = Canvas()
        blit(cv, art, pal)
        outline(cv)
        return [cv.image()]
    frames = []
    for f in range(ANIM_FRAMES):
        glow = 0.5 + 0.5 * math.cos(2 * math.pi * f / ANIM_FRAMES)
        cv = Canvas()
        blit(cv, art, {**pal, "r": mix(pal["r"], (255, 255, 255), 0.5 * glow)})
        outline(cv)
        frames.append(cv.image())
    return frames


ELEMENTS = ["fire", "frost", "storm", "wind", "earth", "life", "void", "arcane", "time", "blood"]
FOCUS_KINDS = ["haste", "thrift", "deep_well", "echoes"]


def preview(out_dir: str) -> str:
    items = []
    for element in ELEMENTS:
        items.append(staff_icon(element)[0])
    for element in ELEMENTS:
        items.append(staff_icon(element, True)[0])
    items.append(tome_icon()[0])
    for kind in FOCUS_KINDS:
        items.append(focus_icon(kind)[0])
    for im in items:
        assert im.size == (16, 16) and set(im.getchannel("A").tobytes()) <= {0, 255}
    per = 10
    rows = (len(items) + per - 1) // per
    sheet = Image.new("RGBA", (per * 20 + 2, rows * 20 * 2 + 4), (0x8B, 0x8B, 0x8B, 255))
    sheet.alpha_composite(Image.new("RGBA", (per * 20 + 2, rows * 20 + 2), (0x1B, 0x17, 0x26, 255)), (0, rows * 20 + 2))
    for i, im in enumerate(items):
        x, y = 2 + (i % per) * 20, 2 + (i // per) * 20
        sheet.alpha_composite(im, (x, y))
        sheet.alpha_composite(im, (x, y + rows * 20 + 2))
    Path(out_dir).mkdir(parents=True, exist_ok=True)
    out = f"{out_dir}/gear_art.png"
    sheet.resize((sheet.width * 5, sheet.height * 5), Image.NEAREST).save(out)
    return out


if __name__ == "__main__":
    default = str(Path(__file__).resolve().parent.parent / "build" / "art-preview")
    print(preview(sys.argv[1] if len(sys.argv) > 1 else default))
