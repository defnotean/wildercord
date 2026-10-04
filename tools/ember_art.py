"""Cinder Bailiffs and Cinder Ferns: the Bailiff's skin and spawn egg, the fern's three growth stages in their cooled and
hot fronds, and their data (blockstate, models, loot, worldgen, recipe) and English text. Called by generate_assets.py
(write(g), and LANG merged into en_us.json).

The skin is painted face by face on CinderBailiffModel's box-UV layout (128x128, client/wildlife/CinderBailiffModel.java)
with monster_art's material painters, lit like the other creatures: tops a step lighter, undersides darker. The fern is
a 16x16 crossed plant drawn from ASCII like the other plants; the egg uses monster_art's egg silhouette.

Run this file on its own to render a review sheet into build/art-preview/ember_art.png.
"""
from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from item_art import Canvas, blit, grid, hexc, mix, ramp  # noqa: E402
from monster_art import EGG, LIGHT, cells, faces, fill, fur, shade, smooth  # noqa: E402
from world_art import Sheet, box, noise  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent

# ============================================================== the Cinder Fern (16x16 crossed plant)

# 's' the woody stem, 'r' a frond's midrib, '1'..'3' the fronds from shadow to lit (lit on their top-left edges),
# '4' the tips. Young: curled fiddleheads; prepared: two fronds open round an unrolling crozier; mature: a clump of
# arching fronds, the tallest bowing over at its tip.
FERN = [
    """
    ................
    ................
    ................
    ................
    ................
    ................
    ................
    ................
    ................
    .......343......
    ......3...2.....
    ......3.4.r.....
    ...43..3r.r..4..
    ....33..r.r.32..
    ....12r.srr21...
    .......ss.......
    """,
    """
    ................
    ................
    ................
    ................
    ......343.......
    .....3...2......
    .....3.4.r......
    ......33r2......
    ..43...3r2..34..
    ...33.33r2.322..
    ...21r3r22r21...
    .....3r2rr2.....
    ......3sr222....
    ....4..sr2.42...
    ...321.s.122....
    .......s........
    """,
    """
    ................
    ..........3.....
    .........3rr3...
    ........3r224...
    3......3r22.....
    .r3....3r2......
    43r42.3r2.......
    2.r.4.3r22...4..
    .2r34.3r2..43.3.
    .22r4.3r2.43rrr3
    ..2r343r243r2224
    ..22r33r23r22...
    ..322r3r2r22....
    .3rrr3rsr.3.....
    .42.2rrsrr4.....
    .......s........
    """,
]

# Cooled: ash-dusted green fronds on a dry brown stem. Hot: ember-orange fronds round glowing midribs, on a charred stem.
FERN_COOL = {"s": "#4E3F2E", "r": "#46603A", "1": "#3A4E30", "2": "#5A7A4C", "3": "#82A06C", "4": "#C8CCB4"}
FERN_HOT = {"s": "#40180E", "r": "#FFC048", "1": "#7A2410", "2": "#C2421A", "3": "#F0782A", "4": "#FFE48A"}


def fern(age, cooled):
    cv = Canvas()
    blit(cv, FERN[age], {k: hexc(v) for k, v in (FERN_COOL if cooled else FERN_HOT).items()})
    return cv.image()


# ============================================================== the Cinder Bailiff (CinderBailiffModel, 128x128)

FUR = ramp("#1C110A", "#2E1D10", "#442C18", "#5C3D20", "#78522A", "#946A36", "#B08548")
BUFF = ramp("#4A3A2A", "#665140", "#836A52", "#A08868", "#BCA684", "#D6C4A2")
ASH = ramp("#2A2624", "#433D38", "#615850", "#837868", "#A69A88", "#C8BEAA", "#E2DACA")
CLAY = ramp("#3A1A10", "#5C2A18", "#843E22", "#A8562E", "#C4703E", "#DA9058", "#EEB47A")
GLAZE = hexc("#F4D6A2")
EMBER = ramp("#5A1408", "#9A2810", "#D84C1A", "#FF8428", "#FFBC44", "#FFEAA0")
CHAR = hexc("#160C08")
EYE = hexc("#FFC23E")
CLAW = hexc("#E2D2B0")
NOSE = hexc("#120C0A")

BODY = box(0, 0, 12, 8, 16)
PLATE = box(0, 28, 13, 6, 4)
HEAD = box(64, 0, 8, 6, 8)
MUZZLE = box(64, 18, 6, 3, 4)
EAR = box(96, 18, 2, 4, 2)
EYEBOX = box(112, 18, 1, 1, 1)
VENT = box(0, 42, 8, 2, 4)
SLATS = box(32, 42, 7, 1, 3)
LEG = box(52, 42, 3, 7, 4)
FOOT = box(68, 42, 4, 2, 5)
TAIL = box(88, 42, 4, 3, 8)
TUFT = box(0, 54, 6, 1, 3)

# The breastplate's front: a cream glaze rim, a chevron incised into the fired clay (dark groove, lit lower lip), a glazed
# boss at its heart and the plate's shadowed lower edge. 'C' clay, 'l' clay lit, 'm' clay shaded, 'h' glaze, 'd' groove.
PLATE_FRONT = """
    hhhhhhhhhhhhh
    ldCCCCCCCCCdm
    lCdCCChCCCdCm
    lCCdCCCCCdCCm
    lCCCdCCCdCCCm
    mmmmmdddmmmmm
"""
# The three vent sockets under the plates, glowing when the plates lift (rows of the body's top face; columns 2..9).
SOCKETS = ((1, 4), (6, 9), (11, 14))


def _fur(cv, part, tones, seed, base=3, along_y=False):
    for name, area in faces(part):
        fur(cv, area, tones, seed * 10 + len(name), name, base=base, along_y=along_y)


def _clay(cv, part, seed, base=3):
    """Fired clay: smooth, a soft lighter bloom here and there where the kiln caught it."""
    for name, area in faces(part):
        for x, y, px, py in cells(area):
            t = base + LIGHT[name] + (1 if smooth(px, py, seed, 2.5) > 0.72 else 0)
            cv.put(px, py, shade(CLAY, t))


def bailiff_texture():
    cv = Sheet(128, 128)

    # Body: umber fur stroked back along the flanks, a buff belly, the spine darker, three sockets on the back.
    _fur(cv, BODY, FUR, 1)
    # A sooty saddle grizzled with ash along the top of the flanks, a buff belly underneath.
    for name in ("right", "left", "front", "back"):
        x0, y0, w, h = BODY[name]
        for x in range(w):
            for y in (0, 1):
                if y == 1 and noise(x, 3, 9) < 0.4:
                    continue
                grizzle = noise(x0 + x, y0 + y, 11) > 0.8
                cv.put(x0 + x, y0 + y, shade(ASH, 4) if grizzle else shade(FUR, 1 + LIGHT[name] // 2 + y))
            cv.put(x0 + x, y0 + h - 1, shade(BUFF, 2 + LIGHT[name] + (1 if noise(x, 1, 7) > 0.6 else 0)))
            if noise(x, 2, 7) > 0.45:
                cv.put(x0 + x, y0 + h - 2, shade(BUFF, 1 + LIGHT[name]))
    fur(cv, BODY["bottom"], BUFF, 19, "bottom", base=3)
    tx, ty, tw, th = BODY["top"]
    for y in range(th):
        for x in range(tw):
            edge = min(x, tw - 1 - x)
            if edge <= 1 or x in (5, 6):
                grizzle = noise(tx + x, ty + y, 11) > 0.8
                cv.put(tx + x, ty + y, shade(ASH, 5) if grizzle else shade(FUR, 2 if x in (5, 6) else 2 + edge))
    for a, b in SOCKETS:
        for y in range(a, b + 1):
            for x in range(2, 10):
                rim = y in (a, b) or x in (2, 9)
                if rim:
                    cv.put(tx + x, ty + y, CHAR)
                else:
                    core = 3 <= x <= 6 and y == a + 1
                    cv.put(tx + x, ty + y, EMBER[5] if core else EMBER[4] if y == a + 1 else EMBER[3])

    # Breastplate: fired clay, glazed along its top edge, the chevron and boss on its front.
    _clay(cv, PLATE, 31, base=3)
    blit_area(cv, PLATE["front"], PLATE_FRONT, {"C": CLAY[4], "l": CLAY[5], "m": CLAY[3], "h": GLAZE, "d": CLAY[1]})
    px0, py0, pw, ph = PLATE["top"]
    for x in range(pw):
        cv.put(px0 + x, py0 + ph - 1, GLAZE)
        cv.put(px0 + x, py0 + ph - 2, CLAY[6])
    for name in ("right", "left"):
        x0, y0, w, h = PLATE[name]
        for x in range(w):
            cv.put(x0 + x, y0, CLAY[5] if name == "right" else CLAY[4])
            cv.put(x0 + x, y0 + h - 1, CLAY[2])

    # Head: umber fur, a cream blaze from brow to nose, a dark mask round each amber eye.
    _fur(cv, HEAD, FUR, 2)
    hx, hy, hw, hh = HEAD["top"]
    for y in range(hh):
        for x in (3, 4):
            cv.put(hx + x, hy + y, shade(ASH, 5 if y > hh - 4 else 4) if y > 1 else shade(ASH, 3))
    fx, fy, fw, fh = HEAD["front"]
    for y in range(fh):
        for x in range(fw):
            if x in (3, 4):
                cv.put(fx + x, fy + y, shade(ASH, 5 if x == 3 else 4))
            elif y == 0:
                cv.put(fx + x, fy + y, shade(FUR, 4))
            elif y >= 2 and x in (1, 2, 5, 6):
                cv.put(fx + x, fy + y, shade(FUR, 1))
    for name in ("right", "left"):
        x0, y0, w, h = HEAD[name]
        for x in range(w):
            z = w - 1 - x if name == "right" else x  # 0 at the front
            if z <= 5:
                for y in (1, 2, 3):
                    if y == 3 and z > 3:
                        continue
                    cv.put(x0 + x, y0 + y, shade(FUR, 0 if y == 2 else 1))
            cv.put(x0 + x, y0, shade(FUR, 4))
    for name in ("right", "left"):
        x0, y0, w, h = HEAD[name]
        ex = w - 1 - 3 if name == "right" else 3
        cv.put(x0 + ex, y0 + 2, EYE)

    # Muzzle: a pale snout, black nose, mouth line.
    for name, area in faces(MUZZLE):
        fur(cv, area, BUFF, 40 + len(name), name, base=3)
    mx, my, mw, mh = MUZZLE["top"]
    for y in range(mh):
        for x in (2, 3):
            cv.put(mx + x, my + y, shade(ASH, 5))
    for x in (2, 3):
        cv.put(mx + x, my + mh - 1, NOSE)
    fx, fy, fw, fh = MUZZLE["front"]
    for x in range(1, 5):
        cv.put(fx + x, fy, NOSE)
    cv.put(fx + 2, fy, hexc("#4A3C36"))
    cv.put(fx + 2, fy + 1, shade(BUFF, 1))
    cv.put(fx + 3, fy + 1, shade(BUFF, 1))
    for x in range(1, 5):
        cv.put(fx + x, fy + 2, shade(BUFF, 0) if x in (1, 4) else shade(BUFF, 1))
    for name in ("right", "left"):
        x0, y0, w, h = MUZZLE[name]
        for x in range(w):
            cv.put(x0 + x, y0 + h - 1, shade(BUFF, 1))

    # Ears: dark, tipped with ash, a warm inner ear on the front.
    _fur(cv, EAR, FUR, 5, base=2)
    for name in ("right", "front", "left", "back"):
        x0, y0, w, h = EAR[name]
        for x in range(w):
            cv.put(x0 + x, y0, shade(ASH, 5))
    fill(cv, EAR["top"], shade(ASH, 5))
    ex, ey, _, _ = EAR["front"]
    cv.put(ex, ey + 2, hexc("#6E4234"))
    cv.put(ex + 1, ey + 2, hexc("#5A3428"))
    cv.put(ex, ey + 1, hexc("#7E5040"))

    # The eye studs on the sides of the head: amber, brightest on the faces that show.
    for name, area in faces(EYEBOX):
        fill(cv, area, EYE if name in ("right", "left", "front") else hexc("#D88426") if name == "top" else hexc("#8A4A1A"))

    # Vent plates: fired clay, glazed rim, and the ember glow underneath that shows when a plate lifts.
    _clay(cv, VENT, 51, base=3)
    vx, vy, vw, vh = VENT["top"]
    for x in range(vw):
        cv.put(vx + x, vy + vh - 1, GLAZE)
    for y in range(vh):
        cv.put(vx, vy + y, CLAY[5])
        cv.put(vx + vw - 1, vy + y, CLAY[2])
    bx, by, bw, bh = VENT["bottom"]
    for y in range(bh):
        for x in range(bw):
            edge = x in (0, bw - 1) or y in (0, bh - 1)
            cv.put(bx + x, by + y, CLAY[1] if edge else EMBER[5] if (x + y) % 3 == 0 else EMBER[4])
    for name in ("right", "front", "left", "back"):
        x0, y0, w, h = VENT[name]
        for x in range(w):
            cv.put(x0 + x, y0, CLAY[5] if name in ("front", "right") else CLAY[4])
            cv.put(x0 + x, y0 + h - 1, CLAY[1])
    # The slats on each plate: four clay bars with three smouldering gaps between.
    for name, area in faces(SLATS):
        x0, y0, w, h = area
        for x, y, px, py in cells(area):
            col = x if name in ("top", "bottom", "front", "back") else None
            if col is not None and col % 2 == 1:
                cv.put(px, py, EMBER[1] if name != "top" else (EMBER[2] if y == 1 else EMBER[1]))
            else:
                lit = 1 if (name == "top" and y == h - 1) or name in ("right",) else 0
                cv.put(px, py, shade(CLAY, 4 + LIGHT[name] + lit))

    # Legs: darker fur stroked down, stockings darkening to the feet.
    _fur(cv, LEG, FUR, 6, base=2, along_y=True)
    for name in ("right", "front", "left", "back"):
        x0, y0, w, h = LEG[name]
        for x in range(w):
            for y in range(h - 2, h):
                cv.put(x0 + x, y0 + y, shade(FUR, 1 if y == h - 1 else 2 + LIGHT[name] // 2))
    # Feet: sculpted fired clay, three toes with grooves between, pale claws at the front.
    _clay(cv, FOOT, 61, base=2)
    fx, fy, fw, fh = FOOT["top"]
    for y in range(fh):
        for x in range(fw):
            cv.put(fx + x, fy + y, CLAY[2] if x == 2 else CLAY[4] if x == 0 else CLAY[3])
        cv.put(fx + 1, fy + y, CLAY[4] if y < fh - 1 else CLAY[5])
    fx, fy, fw, fh = FOOT["front"]
    for x in range(fw):
        cv.put(fx + x, fy, CLAY[1] if x == 2 else CLAY[4])
        cv.put(fx + x, fy + 1, CLAW if x != 2 else CLAY[0])
    for name in ("right", "left"):
        x0, y0, w, h = FOOT[name]
        for x in range(w):
            cv.put(x0 + x, y0, CLAY[4])
            cv.put(x0 + x, y0 + h - 1, CLAY[2])
        fx_ = x0 + (w - 1 if name == "right" else 0)
        cv.put(fx_, y0 + h - 1, CLAW)

    # Brush tail: umber at the root, the brush grizzled ash toward its tip; the tufts flare it out on either side.
    for name, area in faces(TAIL):
        x0, y0, w, h = area
        fur(cv, area, FUR, 70 + len(name), name, base=3, along_y=name in ("top", "bottom"))
        for x, y, px, py in cells(area):
            if name == "top":
                z = h - 1 - y
            elif name == "bottom":
                z = h - 1 - y
            elif name == "right":
                z = w - 1 - x
            elif name == "left":
                z = x
            elif name == "back":
                z = 7
            else:
                z = 0
            if z >= 4:
                streak = smooth(px * (0.6 if name in ("top", "bottom") else 2.5), py * (2.5 if name in ("top", "bottom") else 0.6), 77, 2.0)
                t = (3 if z < 6 else 4) + LIGHT[name] + (1 if streak > 0.68 else 0) - (1 if streak < 0.25 else 0)
                cv.put(px, py, shade(ASH, t))
    for name, area in faces(TUFT):
        x0, y0, w, h = area
        for x, y, px, py in cells(area):
            if name in ("top", "bottom"):
                outer = x in (0, w - 1)
                t = (4 if outer else 3) + LIGHT[name] + (1 if x % 2 == 0 and not outer else 0)
                cv.put(px, py, shade(ASH if outer or y == 0 else FUR, t))
            else:
                cv.put(px, py, shade(ASH, 3 + LIGHT[name] + (x % 2)))
    return cv.image()


def blit_area(cv, area, text, pal):
    x0, y0, w, h = area
    rows = grid(text)
    assert len(rows) == h and len(rows[0]) == w, f"face {area}: got {len(rows[0])}x{len(rows)}"
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                cv.put(x0 + x, y0 + y, pal[ch])


# ============================================================== spawn egg (16x16)

# Umber shell flecked with ash, three glowing vent slits across the top and a fired-clay breastplate below.
EGG_SHELL = ("#2E2018", "#4E3A2A", "#6E5640")
EGG_SPOT = "#7E7264"
EGG_MOTIF = """
    ................
    ................
    ................
    .....e.ee.e.....
    .....Y.YY.Y.....
    ................
    ................
    .....a....a.....
    ................
    ................
    ....hhhhhhhh....
    ....lCdCCdCm....
    .....mCddCm.....
    ................
    ................
    ................
"""
EGG_PAL = {"a": "#FFC23E", "e": "#D84C1A", "Y": "#FFBC44", "h": "#F4D6A2", "l": "#DA9058", "C": "#C4703E",
           "m": "#843E22", "d": "#5C2A18"}


def egg_icon():
    dark, mid, light = (hexc(c) for c in EGG_SHELL)
    cv = Canvas()
    outline = mix(dark, (0, 0, 0), 0.55)
    for y, row in enumerate(grid(EGG)):
        for x, ch in enumerate(row):
            if ch == "o":
                cv.put(x, y, outline)
            elif ch == "1":
                t = (x + y) / 26
                c = light if t < 0.42 else mid if t < 0.72 else dark
                if noise(x, y, len("cinder_bailiff")) > 0.8:
                    c = hexc(EGG_SPOT)
                cv.put(x, y, c)
    for y, row in enumerate(grid(EGG_MOTIF)):
        for x, ch in enumerate(row):
            if ch in EGG_PAL and cv.get(x, y) is not None:
                cv.put(x, y, hexc(EGG_PAL[ch]))
    return cv.image()


# ============================================================== text and data

LANG = {'entity.wildercord.cinder_bailiff': 'Cinder Bailiff', 'item.wildercord.cinder_bailiff_spawn_egg': 'Cinder Bailiff Spawn Egg', 'block.wildercord.cinder_fern': 'Cinder Fern', 'guide.wildercord.cinder_bailiff.hint': 'Listen for three ceramic vent clicks near a cooled woodland fern.', 'guide.wildercord.cinder_bailiff': 'A Bailiff rests only after reaching and browsing a mature cooled fern. Raised vents warn of three fixed ash lanes. Step behind it or use solid cover; each fan can strike you once. Ordinary physical damage during its warning or a successful cooling spell interrupts preparation. Its fern root survives browsing and harvest. Killing it grants no special materials.'}
for n, t in [('warn', 'Ceramic vents lift'), ('fan', 'Bailiff ash vents sweep'), ('recover', 'Bailiff breath settles'), ('browse', 'Bailiff browses dry fronds'), ('cool', 'Hot vents cool')]:
    LANG['subtitles.wildercord.kit.ember_bailiff.' + n] = t
LANG['subtitles.wildercord.kit.ember_fern.pick'] = 'Dry fern fronds pluck'


def write(g):
    g.save(bailiff_texture(), g.ASSETS / 'textures/entity/cinder_bailiff.png')
    variants = {}
    for age in range(3):
        for cooled in [False, True]:
            name = f'cinder_fern_{age}_{"cool" if cooled else "hot"}'
            g.save(fern(age, cooled), g.ASSETS / f'textures/block/{name}.png')
            g.write_json(g.ASSETS / f'models/block/{name}.json', {'parent': 'minecraft:block/cross', 'textures': {'cross': f'wildercord:block/{name}'}})
            variants[f'age={age},cooled={str(cooled).lower()}'] = {'model': f'wildercord:block/{name}'}
    g.write_json(g.ASSETS / 'blockstates/cinder_fern.json', {'variants': variants})
    g.write_json(g.ASSETS / 'models/item/cinder_fern.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'wildercord:block/cinder_fern_2_cool'}})
    g.save(egg_icon(), g.ASSETS / 'textures/item/cinder_bailiff_spawn_egg.png')
    g.write_json(g.ASSETS / 'models/item/cinder_bailiff_spawn_egg.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'wildercord:item/cinder_bailiff_spawn_egg'}})
    for name in ['cinder_fern', 'cinder_bailiff_spawn_egg']:
        g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/cinder_bailiff.json', {'type': 'minecraft:entity', 'pools': []})
    g.write_json(g.DATA / 'loot_table/blocks/cinder_fern.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'wildercord:cinder_fern'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    g.write_json(g.DATA / 'worldgen/feature/cinder_fern_patch.json', {'type': 'wildercord:cinder_fern_patch'})
    g.write_json(g.DATA / 'worldgen/placed_feature/cinder_fern_patch.json', {'feature': 'wildercord:cinder_fern_patch', 'placement': [{'type': 'minecraft:rarity_filter', 'chance': 8}, {'type': 'minecraft:in_square'}, {'type': 'minecraft:heightmap', 'heightmap': 'MOTION_BLOCKING_NO_LEAVES'}, {'type': 'minecraft:biome'}]})

    # Ordinary crafting supplies an existing-world route without replacing explored chunks.
    g.write_json(g.DATA / 'recipe/cinder_fern.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': ['minecraft:fern', 'minecraft:clay_ball', 'minecraft:charcoal'], 'result': {'id': 'wildercord:cinder_fern', 'count': 1}})
    g.write_json(g.DATA / 'advancement/recipes/cinder_fern.json', {'criteria': {'has_clay': {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': 'minecraft:clay_ball'}]}}}, 'requirements': [['has_clay']], 'rewards': {'recipes': ['wildercord:cinder_fern']}})


# ============================================================== review sheet


def preview(path=None):
    """Upscaled review sheet: the skin, the six fern textures, the egg."""
    skin = bailiff_texture()
    ferns = [fern(a, c) for c in (True, False) for a in range(3)]
    sc = 4
    w = 128 * sc + 24 + 3 * (16 * 8 + 8)
    out = Image.new("RGBA", (w, 128 * sc), (58, 58, 70, 255))
    out.alpha_composite(skin.resize((128 * sc, 128 * sc), Image.NEAREST), (0, 0))
    x0 = 128 * sc + 24
    for i, im in enumerate(ferns):
        out.alpha_composite(im.resize((128, 128), Image.NEAREST), (x0 + (i % 3) * 136, (i // 3) * 136))
    out.alpha_composite(egg_icon().resize((128, 128), Image.NEAREST), (x0, 2 * 136))
    for i, im in enumerate(ferns + [egg_icon()]):
        out.alpha_composite(im, (x0 + 136 + i * 20, 2 * 136 + 8))
    path = path or ROOT / "build/art-preview/ember_art.png"
    Path(path).parent.mkdir(parents=True, exist_ok=True)
    out.save(path)
    return path


if __name__ == "__main__":
    print(preview(sys.argv[1] if len(sys.argv) > 1 else None))
