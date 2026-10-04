"""Glowcaps, the Fungal Nursery, the Belowkeeper Breathmarks and the Cave Breather: block textures, models, icons, text.

Drawn in the house style (docs/ART.md, item_art.py): 16x16 icons from ASCII grids, a one-pixel outline tinted toward
each part's own colour, lit from the top left; block textures at vanilla resolution with a few deliberate tones per
material. Block models use vanilla auto-UV, so every box shows its texture at block scale.

Also holds the small shared helpers sporeback_art and rootmolt_art use for their icons (sprite, book_icon, egg_icon).
"""
from __future__ import annotations

import math
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gear_art import outline  # noqa: E402
from item_art import Canvas, blit, grid, hexc, mix, ramp  # noqa: E402
from monster_art import EGG, smooth  # noqa: E402
from world_art import noise  # noqa: E402

LANG={
 'block.wildercord.glowcap':'Glowcap','block.wildercord.fungal_nursery':'Fungal Nursery','block.wildercord.breathmark':'Belowkeeper Breathmark',
 'item.wildercord.dried_glowcap_gills':'Dried Glowcap Gills','item.wildercord.dried_glowcap_gills.lore':'Paper-thin lamellae hold the memory of a damp bank.','item.wildercord.dried_glowcap_gills.use':'Craft a Fungal Nursery or a Cave Breather.',
 'item.wildercord.cave_breather':'Cave Breather','item.wildercord.cave_breather.lore':'A Belowkeeper filter, built to trade quick feet for one clear breath.','item.wildercord.cave_breather.use':'Hold in your offhand and use to clear current Poison. 32 filter uses; 10 second rest; 3 seconds of Slowness. Ongoing attacks can poison you again.',
 'item.wildercord.breathmark_roots':'Where the Roots Drink','item.wildercord.breathmark_air':'The Second Breath','item.wildercord.nursery_journal':'Three Breathmarks',
 'message.wildercord.fungal.replica':'This is an ordinary carved stone. Its field memory is quiet.',
 'message.wildercord.fungal.filter_rest':'Hold the filter in your offhand. It needs current Poison and a rested filter.',
 **{'message.wildercord.fungal.hint_'+k:v for k,v in {
 'snail':'First, kneel empty-handed to gather dew from a patient Sporeback Snail.',
 'root':'Find the root-glyph Breathmark on covered cave moss or clay below Y48.',
 'air':'Find a different Breathmark bearing the open-air glyph. Another root mark adds no step.',
 'harvest':'Grow a damp dark Glowcap bud, let a snail browse it, and harvest its mature gills yourself.',
 'roof':'Craft a Fungal Nursery and place it yourself after harvesting gills.',
 'breather':'Craft your Cave Breather after placing the nursery: dew, dried gills, leather and copper.',
 'return':'Show your Cave Breather to a Fungal Nursery to finish Mara\'s investigation.',
 'done':'Mara\'s Three Breathmarks are complete. The nursery still shelters; the filter still works.'}.items()},
 'book.wildercord.fungal.0.1':'WHERE ROOTS DRINK\n\nMara, Belowkeeper\n\nI followed a patient spiral to the cap below this stone. The stream stayed in its bed. Its roots drank from the side, never from water over their heads. The lamp was enough to spoil a bud.',
 'book.wildercord.fungal.0.2':'Set a Glowcap cutting on moss or clay, beside water and under cover. Keep it below bright torchlight. Life can prepare a bud. Only the slow mouth of a living snail can open its gills; leave the visitor its own resting clocks.',
 'book.wildercord.fungal.0.3':'Harvest the opened gills and leave the stem standing. Each cap begins again as a young bud. The next stone bears an open-air glyph. Seek another Belowkeeper mark on covered cave moss or clay, not a copy of this root.',
 'book.wildercord.fungal.1.1':'THE SECOND BREATH\n\nMara, Belowkeeper\n\nWe once boxed our spirals in. They went still, but it was fear, not rest. This mark remembers the apprentice who cut four windows into a shelter and let the visitors choose whether to stay.',
 'book.wildercord.fungal.1.2':'Two sticks, paper and dried Glowcap gills make an open nursery canopy. Put its roof one block above a walkable dark footing. One snail can tuck beneath it after browsing. It gives no extra dew and shortens no gathering rest.',
 'book.wildercord.fungal.1.3':'Dew, gills, leather and copper make a Cave Breather. Keep it in the offhand. One breath clears present poison, then leaves three heavy seconds. The filter rests ten seconds. A fresh bite can still reach you. It is no shield.',
 'book.wildercord.fungal.2.1':'THREE BREATHMARKS\n\nMara, Belowkeeper\n\nYou learned the living spiral, the drinking root and the open roof. Then you made your own breath. I leave three blank runes for what you discover next. This gift is made once.',
 'book.wildercord.fungal.2.2':'Keep the garden working: water beside the roots, dark cover above, and a visitor free to leave. A new harvest needs another prepared bud and another real browse. No lamp or chant pays the snail\'s resting time for it.',
 'book.wildercord.fungal.2.3':'We are not owners of the deep. We are guests who mark a safe step for the next traveller. Record the openings you find. Share a filter when a friend stumbles. The nursery remains useful after the journal is finished.',
 **{'subtitles.wildercord.kit.fungal.'+n:t for n,t in [('cap_open','Glowcap gills unfold'),('cap_harvest','Gills are gathered'),('mark_read','Stone memory is read'),('nursery_settle','Visitor settles'),('filter','Filter exhales')]}}


# ============================================================== shared icon helpers (also used by sporeback_art, rootmolt_art)

def sprite(text, pal, size=16, ring=None):
    """Blit an ASCII grid; with ring set, outline every part in a darker shade of whatever it touches."""
    cv = Canvas(size)
    blit(cv, text, {k: (hexc(v) if isinstance(v, str) else v) for k, v in pal.items()})
    if ring is not None:
        outline(cv, ring)
    return cv


# A Belowkeeper field book, drawn like the established lore books: a cloth cover with gold corners, a stitched spine
# with its binding rings on the left, the page block at the foot and an embossed emblem on the cover.
BOOK = """
    ................
    ...ooooooooooo..
    ..oSgCCCCCCCCgo.
    .RoSCccccccccdo.
    ..oSCccccccccdo.
    .RoSCccccccccdo.
    ..oSCccccccccdo.
    .RoSCccccccccdo.
    ..oSCccccccccdo.
    .RoSCccccccccdo.
    ..oSCccccccccdo.
    .RoSCccccccccdo.
    ..oSgddddddddgo.
    ..oPPPPPPPPPPPo.
    ..oppppppppppdo.
    ...ooooooooooo..
"""


def book_icon(cover, emblem, ink, shine=None):
    """cover: (outline, spine, light, mid, dark); emblem: 7x7 ASCII ('e' ink, 'h' shine) set on the cover at (5, 4)."""
    o, spine, light, mid, dark = cover
    pal = {"o": o, "S": spine, "C": light, "c": mid, "d": dark, "g": "#E8C46A", "R": "#D9C392",
           "P": "#F4EAD0", "p": "#C9B48C"}
    cv = sprite(BOOK, pal)
    rows = grid(emblem)
    lit = {(5 + x, 4 + y): ch for y, row in enumerate(rows) for x, ch in enumerate(row) if ch in "eh"}
    for (x, y) in lit:
        # Embossed: a shadow below-right of every stroke, the stroke itself in the ink colour.
        if (x + 1, y + 1) not in lit:
            cv.put(x + 1, y + 1, hexc(dark))
    for (x, y), ch in lit.items():
        cv.put(x, y, hexc(shine if ch == "h" and shine else ink))
    return cv.image()


def egg_icon(shell, spot, motif, pal, seed):
    """A spawn egg on monster_art.EGG's silhouette: shell ramp (dark, mid, light) lit from the top left, spots, motif."""
    dark, mid, light = (hexc(c) for c in shell)
    cv = Canvas()
    outline_c = mix(dark, (0, 0, 0), 0.55)
    for y, row in enumerate(grid(EGG)):
        for x, ch in enumerate(row):
            if ch == "o":
                cv.put(x, y, outline_c)
            elif ch == "1":
                t = (x + y) / 26
                c = light if t < 0.42 else mid if t < 0.72 else dark
                if noise(x, y, seed) > 0.8:
                    c = hexc(spot)
                cv.put(x, y, c)
    for y, row in enumerate(grid(motif)):
        for x, ch in enumerate(row):
            if ch in pal and cv.get(x, y) is not None:
                cv.put(x, y, hexc(pal[ch]))
    return cv.image()


# ============================================================== block textures (16x16)

STONE = ramp("#1C1C25", "#2A2B36", "#3A3C4A", "#4B4E5E", "#5E6274", "#787D90")
CAP = ramp("#9C7E5C", "#C4A47A", "#E2CDA2", "#F2E4C2", "#FFF6E0")
GLOW = ramp("#2E6E5E", "#4EA088", "#7CD4B0", "#B4F2D2", "#E6FFF0")
STEM = ramp("#7A6A8E", "#9E8EB2", "#C2B4D2", "#E0D6EA", "#F4EEF8")
WOOD = ramp("#3A2614", "#553A20", "#6E4C2C", "#8A6238", "#A47A48")
COPPER = ramp("#6D3421", "#A0502F", "#C86C46", "#E38A5E", "#F8B48A")
VERDIGRIS = ramp("#2E7A64", "#4CA488", "#7CCCB0")
LEATHER = ramp("#3E2416", "#5E3822", "#7E4E2E", "#A0683E", "#C08A58")
PAPER = ramp("#8A7652", "#B09A70", "#D4C094", "#EADCB4", "#F8F0D4")


def tile(paint):
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = (*paint(x, y), 255)
    return im


def stone_tone(x, y):
    """Cut cave stone: a bevelled block, lit on its top and left edges, with soft worn patches on its face."""
    if x == 0 or y == 0:
        return 4
    if x == 15 or y == 15:
        return 1
    if x == 1 or y == 1:
        return 3
    if x == 14 or y == 14:
        return 2 if (x + y) % 5 else 1
    n = smooth(x, y, 1201, 4.0)
    return 3 if n > 0.7 else 1 if n < 0.12 else 2


def stone(x, y):
    return STONE[stone_tone(x, y)]


# The carved field marks, centred where the stele's faces show them (x 4-11, y 5-12 of the texture).
ROOT_GLYPH = """
    .#######
    ....#...
    ....#...
    ...###..
    ..#.#.#.
    .#..#..#
    .#..#..#
    ....#...
"""
AIR_GLYPH = """
    ....#...
    ...#.#..
    ..#...#.
    ##..#..#
    ..#...#.
    ...#.#..
    ....#...
    ........
"""


def glyph_tile(text, inlay):
    cells = {(4 + x, 5 + y) for y, row in enumerate(grid(text)) for x, ch in enumerate(row) if ch == "#"}
    hi, mid, lo = (hexc(c) for c in inlay)
    base = {(x, y): stone(x, y) for y in range(16) for x in range(16)}
    for (x, y) in cells:
        # Cut into the stone: the lip above-left falls into shadow, the far wall below-right catches the light.
        if (x - 1, y - 1) not in cells and (x - 1, y - 1) in base:
            base[(x - 1, y - 1)] = STONE[0]
        if (x + 1, y + 1) not in cells and (x + 1, y + 1) in base:
            base[(x + 1, y + 1)] = STONE[4]
    for (x, y) in cells:
        edge = (x - 1, y) not in cells or (x, y - 1) not in cells
        base[(x, y)] = hi if edge and (x + y) % 3 == 0 else mid if edge else lo
    return tile(lambda x, y: base[(x, y)])


def cap(x, y):
    """The Glowcap's ivory cap from above: lit at the top left, a darker rim, a few luminous mint spots."""
    r = math.hypot(x - 7.5, y - 7.5)
    t = 3 - (1 if r > 5.6 else 0) - (1 if r > 7.0 else 0)
    if x + y < 9 and r < 5.6:
        t += 1
    if x + y > 20:
        t -= 1
    for sx, sy in ((5, 5), (10, 4), (4, 10), (11, 11), (8, 8)):
        d = abs(x - sx) + abs(y - sy)
        if d == 0:
            return GLOW[4]
        if d == 1 and (x <= sx or y <= sy):
            return GLOW[3]
    return CAP[max(0, min(4, t))]


def gills(x, y):
    """The cap's underside: fine glowing lamellae with dark gaps."""
    if x % 2 == 1:
        return GLOW[0] if (y + x) % 7 else GLOW[1]
    return GLOW[3] if (y + x // 2) % 5 == 0 else GLOW[2]


def stem(x, y):
    """A pale lilac stem with long fibres, lit on its left."""
    t = (3, 2, 2, 1)[x % 4]
    if smooth(x, y, 1233, 3.0) > 0.75:
        t += 1
    return STEM[t]


def planks(x, y):
    """The nursery's frame: dark worked wood in boards, vanilla-plank fashion."""
    row = y // 4
    if y % 4 == 3:
        return WOOD[0]
    if (x + row * 7) % 16 == 0:
        return WOOD[1]
    t = 3 if y % 4 == 0 else 2
    if smooth(x * 0.5, y * 2, 1240 + row, 2.0) > 0.7:
        t += 1
    if smooth(x * 0.5, y * 2, 1250 + row, 2.0) < 0.2:
        t -= 1
    return WOOD[t]


def roof(x, y):
    """The woven canopy: paper strips and dried gills in an over-under weave."""
    bx, by = x // 4, y // 4
    lx, ly = x % 4, y % 4
    if (bx + by) % 2 == 0:
        # A paper strip running across: lit along its top, shadowed along its bottom.
        t = 4 if ly == 0 else 1 if ly == 3 else 3
        if lx == 3 and ly in (1, 2):
            t = 2
        return PAPER[t]
    # A gill strip running down: mint-veined, lit at its left.
    if lx == 3:
        return GLOW[0]
    return GLOW[3] if lx == 0 else GLOW[2] if ly != 3 else GLOW[1]


def copper(x, y):
    """Copper bands: bright metal with a lit top edge, rivets at the corners, a little verdigris in the seams."""
    if y in (0, 8):
        return COPPER[4]
    if y in (7, 15):
        return COPPER[0]
    if x in (2, 13) and y in (3, 11):
        return COPPER[4]
    if x in (3, 14) and y in (4, 12):
        return COPPER[0]
    if y in (6, 14) and x in ((5, 6, 10) if y == 6 else (2, 9, 10)):
        return VERDIGRIS[1]
    if y in (5, 13) and x == (5 if y == 5 else 9):
        return VERDIGRIS[2]
    if y in (6, 14):
        return COPPER[1]
    return COPPER[3] if y in (1, 9) or (y in (2, 10) and x < 5) else COPPER[2]


def leather(x, y):
    """Leather lashing: wrapped bands, each lit along its top, with a pale stitch line."""
    ly = y % 4
    if ly == 3:
        return LEATHER[0]
    if ly == 0:
        return LEATHER[4] if (x + y) % 6 else LEATHER[3]
    if ly == 1 and x % 3 == 1:
        return hexc("#E8D0A0")
    return LEATHER[2] if (x + y // 4) % 5 else LEATHER[1]


BLOCK_TEXTURES = {
    "cap": lambda: tile(cap),
    "stem": lambda: tile(stem),
    "gills": lambda: tile(gills),
    "roof": lambda: tile(roof),
    "frame": lambda: tile(planks),
    "copper": lambda: tile(copper),
    "leather": lambda: tile(leather),
    "stone": lambda: tile(stone),
    "root_glyph": lambda: glyph_tile(ROOT_GLYPH, ("#FFE6A0", "#E8B858", "#B8803A")),
    "air_glyph": lambda: glyph_tile(AIR_GLYPH, ("#F0FFFA", "#B4F2E2", "#6CCAB8")),
}


# ============================================================== item icons (16x16)

def dried_gills_icon():
    """Dried gills, pleated like a folding fan: ivory lamellae spread from a twine-bound root, their rims still mint."""
    cv = Canvas()
    px, py = 3.0, 13.0
    for y in range(16):
        for x in range(16):
            dx, dy = x - px, py - y
            r = math.hypot(dx, dy)
            if r > 11.6 or r < 0.5 or dx < -0.5 or dy < -0.5:
                continue
            ang = math.degrees(math.atan2(dy, dx))
            if ang < 4 or ang > 86:
                continue
            pleat = int((ang - 4) / 82 * 7)
            if r > 9.6:
                c = GLOW[3] if pleat % 2 == 0 else GLOW[2]
            elif pleat % 2 == 0:
                c = CAP[4] if r < 6 else CAP[3]
            else:
                c = CAP[1] if r < 6 else CAP[2]
            cv.put(x, y, c)
    for x, y in ((2, 13), (3, 13), (3, 12), (4, 12), (2, 14), (3, 14)):
        cv.put(x, y, LEATHER[3] if (x + y) % 2 else LEATHER[2])
    # The pale mint rim needs a deeper ring than the house default to keep a solid dark edge all the way round.
    outline(cv, 0.8)
    return cv.image()


CAVE_BREATHER = """
    ................
    ......llll......
    .....lL..Ll.....
    .....l....l.....
    ....ABBBBBBC....
    ...ABBBBBBBBC...
    ...WWwwwwwwwv...
    ...WgGGGGGGgv...
    ...Wwwwwwwwwv...
    ...WgGGGGGGgv...
    ...Wwwwwwwwwv...
    ...WgGGGGGGgv...
    ...ABBBBBBBBC...
    ....CBBBBBBC....
    .....mMMMMm.....
    ................
"""
CAVE_BREATHER_PAL = {
    "l": LEATHER[1], "L": LEATHER[3], "A": COPPER[4], "B": COPPER[2], "C": COPPER[1],
    "W": CAP[4], "w": CAP[3], "v": CAP[1], "g": GLOW[0], "G": GLOW[3], "m": LEATHER[1], "M": LEATHER[3],
}

ROOTS_EMBLEM = """
    eeeeeee
    ...e...
    ..eee..
    .e.e.e.
    e..e..e
    e..e..e
    ...e...
"""
AIR_EMBLEM = """
    ...h...
    ..e.e..
    .e...e.
    ee.h..e
    .e...e.
    ..e.e..
    ...e...
"""
NURSERY_EMBLEM = """
    ...e...
    ..ehe..
    ...e...
    .......
    .e...e.
    ehe.ehe
    .e...e.
"""
BELOWKEEPER = ("#0E201B", "#1C3A31", "#5AA08A", "#3E7A68", "#2A5548")


def write(g):
    for name, paint in BLOCK_TEXTURES.items():
        g.save(paint(), g.ASSETS / f"textures/block/fungal_{name}.png")

    def element(a, b, tex, top=None):
        """A box with vanilla auto-UV (texture pixels at block scale); 'top' re-skins its up and down faces."""
        return {"from": a, "to": b, "faces": {f: {"texture": "#" + (top if top and f in ("up", "down") else tex)}
                                              for f in ("up", "down", "north", "south", "east", "west")}}

    def model(path, textures, elements):
        g.write_json(g.ASSETS / f"models/{path}.json", {"parent": "minecraft:block/block", "textures": {
            "particle": next(iter(textures.values())), **textures}, "elements": elements})

    # Glowcap: a pale stem, a ring of glowing gills under an ivory cap; the mature cap has a smaller sister.
    for age in range(3):
        top = 5 + age * 3
        half = 3 + age
        els = [element([7, 0, 7], [9, top, 9], "stem"),
               element([8 - half, top - 1, 8 - half], [8 + half, top, 8 + half], "gills"),
               element([7 - half, top, 7 - half], [9 + half, top + 1, 9 + half], "cap"),
               element([8 - half, top + 1, 8 - half], [8 + half, top + 2, 8 + half], "cap")]
        if age == 2:
            els += [element([3, 0, 10], [4, 5, 11], "stem"), element([1, 5, 8], [6, 6, 13], "gills"),
                    element([1, 6, 8], [6, 7, 13], "cap"), element([2, 7, 9], [5, 8, 12], "cap")]
        model(f"block/glowcap_{age}", {m: f"wildercord:block/fungal_{m}" for m in ("cap", "stem", "gills")}, els)
    g.write_json(g.ASSETS / "blockstates/glowcap.json",
                 {"variants": {f"age={a}": {"model": f"wildercord:block/glowcap_{a}"} for a in range(3)}})

    # Fungal Nursery: four grounded posts lashed with leather, beams and copper rails, a woven canopy drying two gills.
    els = []
    for x in (0, 14):
        for z in (0, 14):
            els.append(element([x, -16, z], [x + 2, 12, z + 2], "frame"))
            els.append(element([x - 0.25, 7, z - 0.25], [x + 2.25, 9, z + 2.25], "leather"))
    for x in (0, 4, 8, 12):
        els.append(element([x, 12, 0], [x + 4, 14, 16], "roof"))
    for z in (0, 14):
        els.append(element([0, 14, z], [16, 15, z + 2], "copper"))
        els.append(element([0, 10, z], [16, 12, z + 2], "frame"))
    els += [element([4, 14, 3], [6, 16, 13], "gills"), element([10, 14, 3], [12, 16, 13], "gills")]
    model("block/fungal_nursery",
          {m: f"wildercord:block/fungal_{m}" for m in ("frame", "roof", "copper", "leather", "gills")}, els)
    g.write_json(g.ASSETS / "blockstates/fungal_nursery.json", {"variants": {"": {"model": "wildercord:block/fungal_nursery"}}})

    # Breathmark: a stone footing, a carved stele bearing its glyph on every side, and a capstone.
    for kind, glyph in ((0, "root_glyph"), (1, "air_glyph")):
        model(f"block/breathmark_{kind}", {m: f"wildercord:block/fungal_{m}" for m in ("stone", glyph)},
              [element([2, 0, 2], [14, 3, 14], "stone"), element([3, 3, 4], [13, 11, 12], glyph, top="stone"),
               element([4, 11, 5], [12, 13, 11], "stone")])
    g.write_json(g.ASSETS / "blockstates/breathmark.json",
                 {"variants": {f"kind={k}": {"model": f"wildercord:block/breathmark_{k}"} for k in range(2)}})

    nursery_display = {"display": {
        "gui": {"rotation": [30, 225, 0], "translation": [0, 4, 0], "scale": [.45, .45, .45]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 3, 0], "scale": [.35, .35, .35]},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 3, 0], "scale": [.35, .35, .35]}}}
    for name, parent in (("glowcap", "glowcap_2"), ("fungal_nursery", "fungal_nursery")):
        g.write_json(g.ASSETS / f"models/item/{name}.json",
                     {"parent": f"wildercord:block/{parent}", **(nursery_display if name == "fungal_nursery" else {})})
        g.write_json(g.ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{name}"}})

    # Every carried item is a flat 16x16 icon, the Cave Breather included.
    icons = {
        "dried_glowcap_gills": dried_gills_icon(),
        "cave_breather": sprite(CAVE_BREATHER, CAVE_BREATHER_PAL, ring=0.62).image(),
        "breathmark_roots": book_icon(BELOWKEEPER, ROOTS_EMBLEM, "#F0C860"),
        "breathmark_air": book_icon(BELOWKEEPER, AIR_EMBLEM, "#C8F4EC", "#FFFFFF"),
        "nursery_journal": book_icon(BELOWKEEPER, NURSERY_EMBLEM, "#F0C860", "#B4F2D2"),
    }
    for name, im in icons.items():
        g.save(im, g.ASSETS / f"textures/item/{name}.png")
        g.write_json(g.ASSETS / f"models/item/{name}.json",
                     {"parent": "minecraft:item/generated", "textures": {"layer0": f"wildercord:item/{name}"}})
        g.write_json(g.ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{name}"}})
    # Recipes, loot and worldgen.
    for name,ingredients in [('fungal_nursery',['minecraft:stick','minecraft:stick','minecraft:paper','wildercord:dried_glowcap_gills']),('cave_breather',['wildercord:mycelial_dew','wildercord:dried_glowcap_gills','minecraft:leather','minecraft:copper_ingot'])]:
        g.write_json(g.DATA/f'recipe/{name}.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':ingredients,'result':{'id':'wildercord:'+name}});g.unlock_advancement('wildercord:'+name,'wildercord:dried_glowcap_gills')
    for name in ['glowcap','fungal_nursery']:
        g.write_json(g.DATA/f'loot_table/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'wildercord:'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    for name,rarity in [('glowcap_patch',3),('breathmark_site',4)]:
        g.write_json(g.DATA/f'worldgen/feature/{name}.json',{'type':'wildercord:'+name})
        placement=[{'type':'minecraft:count','count':2},{'type':'minecraft:rarity_filter','chance':rarity},{'type':'minecraft:in_square'},{'type':'minecraft:height_range','height':{'type':'minecraft:uniform','min_inclusive':{'absolute':-32},'max_inclusive':{'absolute':40}}}]
        if name=='breathmark_site': placement.append({'type':'minecraft:biome'})
        g.write_json(g.DATA/f'worldgen/placed_feature/{name}.json',{'feature':'wildercord:'+name,'placement':placement})
