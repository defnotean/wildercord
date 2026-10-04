"""The Rootmolt Strider: its skin, the Root Tether effect icon, the spawn egg, the loot table and the English text.

A broad six-footed cave arthropod: a carapace of bark plates (moss in the seams, pale molted root fibres on its
flanks), coral breathing gills along its abdomen, ochre horn shovel forearms with a worn bright edge, dark jointed legs
and small amber eyes. The skin is painted face by face on RootmoltModel's box layout (128x128) with monster_art's
painters, lit like vanilla mobs; the icons are drawn in the house style.
"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from fungal_art import egg_icon  # noqa: E402
from gear_art import outline  # noqa: E402
from item_art import Canvas, hexc, ramp  # noqa: E402
from monster_art import LIGHT, MOSS, bark, cells, chitin, faces, fill, moss, shade, smooth  # noqa: E402
from world_art import Sheet, box, noise  # noqa: E402

PLATE = ramp("#1E140C", "#33231A", "#4C3422", "#68482C", "#866038", "#A47C48", "#C29A62")
CHITIN = ramp("#120C10", "#1E1418", "#2C1E22", "#3E2C2C", "#544038", "#6E5646")
BELLY = ramp("#3A2C24", "#5A4636", "#7A624A", "#9A8062", "#B89C7C")
GILL = ramp("#5E1A2C", "#962E46", "#C8506A", "#EE8090", "#FFB8BE")
HORN = ramp("#3E2A14", "#6A4C24", "#987034", "#C49A4E", "#E6C47A", "#FFF0C0")
FIBRE = hexc("#D8C496")
AMBER = hexc("#FFB43A")

R_CHEST = box(0, 0, 10, 4, 12)
R_RIDGE = box(48, 24, 12, 1, 14)
R_ABDOMEN = box(0, 24, 10, 5, 8)
R_GILL = box(96, 0, 2, 3, 6)
R_HEAD = box(48, 0, 6, 4, 5)
R_SHOVEL = box(32, 48, 3, 6, 2)
R_EDGE = box(64, 48, 4, 1, 3)
R_FEELER = box(48, 48, 1, 1, 5)
R_EYE = box(82, 48, 1, 1, 1)
R_LEG = box(0, 48, 2, 2, 7)
R_TIBIA = box(24, 48, 1, 6, 1)


def plates(cv, area, seed, rows=4, base=3):
    """The carapace from above: overlapping bark plates across the back, each lit along its front edge and split by a
    dark seam behind, with bark grain running down it and a pale keel along the spine."""
    x0, y0, w, h = area
    for x, y, px, py in cells(area):
        k = y % rows
        t = base + 1
        if k == 0:
            t += 1
        elif k == rows - 1:
            t -= 3
        groove = noise(px, y // rows, seed) < 0.28
        if groove and 0 < k < rows - 1:
            t -= 1
        if x in (0, w - 1):
            t -= 1
        if x in (w // 2 - 1, w // 2) and k < rows - 1:
            t += 1
        cv.put(px, py, shade(PLATE, t))


def belly(cv, area, seed):
    """The underside: soft segments, each a step lighter at its front."""
    for x, y, px, py in cells(area):
        t = 1 + (1 if y % 3 == 0 else 0) - (1 if y % 3 == 2 else 0)
        if x in (0, area[2] - 1):
            t -= 1
        cv.put(px, py, shade(BELLY, t + 1))


def fibres(cv, area, seed, count):
    """Pale molted root fibres hanging from a plate edge."""
    x0, y0, w, h = area
    for k in range(count):
        x = int(noise(k, 3, seed) * w)
        length = 1 + int(noise(k, 4, seed) * (h - 1))
        for y in range(length):
            cv.put(x0 + x, y0 + y, FIBRE if y < length - 1 else hexc("#A8946A"))


def skin():
    cv = Sheet(128, 128)
    # The carapace: the broad ridge over the chest, and the abdomen's plates behind it.
    for name, area in faces(R_RIDGE):
        if name == "top":
            plates(cv, area, 1801)
            moss(cv, area, MOSS, 1811, "top", cover=0.15, base=2)
        elif name == "bottom":
            fill(cv, area, CHITIN[1])
        else:
            x0, y0, w, h = area
            for x, y, px, py in cells(area):
                cv.put(px, py, PLATE[1] if x % 4 == 3 else shade(PLATE, 5 + LIGHT[name]))
    for name, area in faces(R_CHEST):
        if name == "bottom":
            belly(cv, area, 1821)
        elif name == "top":
            plates(cv, area, 1823)
        else:
            bark(cv, area, PLATE, 1825 + len(name), name, base=3)
            fibres(cv, area, 1831 + len(name), 4 if name in ("left", "right") else 2)
    for name, area in faces(R_ABDOMEN):
        if name == "bottom":
            belly(cv, area, 1841)
        elif name == "top":
            plates(cv, area, 1843, rows=3, base=3)
            moss(cv, area, MOSS, 1845, "top", cover=0.18, base=2)
        else:
            chitin(cv, area, PLATE, 1847 + len(name), name, base=3, seam=3, vertical=name in ("left", "right"))
            if name == "back":
                fibres(cv, area, 1849, 3)
    # Exposed gills: coral fronds, each filament lit along its length, tips blushing pale.
    for name, area in faces(R_GILL):
        x0, y0, w, h = area
        for x, y, px, py in cells(area):
            t = 3 if (x if name in ("left", "right", "top", "bottom") else y) % 2 == 0 else 1
            t += LIGHT[name] // 2
            if name in ("left", "right") and x == w - 1:
                t += 1
            cv.put(px, py, shade(GILL, t))
    # The head: a dark plated helm over a pale-jawed mouth.
    for name, area in faces(R_HEAD):
        if name == "top":
            plates(cv, area, 1861, rows=5, base=2)
        else:
            chitin(cv, area, CHITIN, 1863 + len(name), name, base=3, seam=4)
    fx, fy, fw, fh = R_HEAD["front"]
    for x in range(fw):
        cv.put(fx + x, fy, PLATE[4])
    for x, y, c in ((1, 2, HORN[3]), (4, 2, HORN[3]), (2, 2, CHITIN[0]), (3, 2, CHITIN[0]), (1, 3, HORN[2]),
                    (2, 3, HORN[4]), (3, 3, HORN[4]), (4, 3, HORN[2])):
        cv.put(fx + x, fy + y, c)
    # Shovel forearms: ochre horn in growth bands, the blade's broad face lit at its left, a worn pale edge.
    for name, area in faces(R_SHOVEL):
        chitin(cv, area, HORN, 1871 + len(name), name, base=3, seam=3)
        if name == "front":
            x0, y0, w, h = area
            for y in range(h):
                cv.put(x0, y0 + y, HORN[4])
    for name, area in faces(R_EDGE):
        x0, y0, w, h = area
        for x, y, px, py in cells(area):
            t = 5 if name in ("bottom", "front") else 4 + (1 if x == 0 else 0) + LIGHT[name]
            cv.put(px, py, shade(HORN, t))
    # Legs: dark jointed chitin banded with bark, the tibiae ending in pale claws.
    for name, area in faces(R_LEG):
        chitin(cv, area, PLATE, 1881 + len(name), name, base=2, seam=3, vertical=name in ("right", "left"))
    for name, area in faces(R_TIBIA):
        x0, y0, w, h = area
        for x, y, px, py in cells(area):
            if name == "bottom" or y >= h - 1:
                c = HORN[4]
            elif name == "top":
                c = PLATE[3]
            else:
                c = shade(CHITIN, 4 + LIGHT[name] - (2 if y % 3 == 2 else 0))
            cv.put(px, py, c)
    for name, area in faces(R_FEELER):
        x0, y0, w, h = area
        for x, y, px, py in cells(area):
            cv.put(px, py, shade(CHITIN, 4 + LIGHT[name]))
        if name in ("right", "left", "top", "bottom"):
            # The feeler's tip, at its far front end.
            tip = (x0 + (w - 1 if name == "left" else 0), y0) if name in ("right", "left") else (x0, y0 + (0 if name == "top" else h - 1))
            cv.put(*tip, HORN[4])
    for name, area in faces(R_FEELER, "front"):
        fill(cv, area, HORN[4])
    for name, area in faces(R_EYE):
        fill(cv, area, AMBER if name == "front" else hexc("#C8781E"))
    return cv.image()


# ============================================================== icons

EGG_MOTIF = """
    ................
    ................
    ................
    ................
    ......e..e......
    ....ssssssss....
    ...gG......Gg...
    ...GG......GG...
    ...gG......Gg...
    ...ssssssssss...
    ................
    ....H......H....
    ....HH....HH....
    ................
    ................
    ................
"""
EGG_PAL = {"e": "#FFB43A", "s": "#2A1C12", "g": "#C8506A", "G": "#EE8090", "H": "#E6C47A"}


def root_tether_icon():
    """18x18 mob-effect icon: a twisted root rope sprouting at its tip and splaying into rootlets below."""
    cv = Canvas(18)
    twist = (HORN[4], HORN[3], PLATE[5], PLATE[4])
    for y in range(4, 13):
        for x in range(7, 11):
            c = twist[(x + y) % 4]
            if x == 7 and (x + y) % 4 in (2, 3):
                c = HORN[3]
            cv.put(x, y, c)
    for x, y in ((8, 3), (9, 3), (8, 2), (9, 2)):
        cv.put(x, y, twist[(x + y) % 4])
    for x, y in ((5, 2), (6, 2), (6, 3), (7, 3), (11, 1), (12, 1), (10, 2), (11, 2)):
        cv.put(x, y, hexc("#8CC850") if (x + y) % 2 else hexc("#5E9A36"))
    for pts in (((7, 13), (6, 14), (5, 15), (4, 16)), ((8, 13), (8, 14), (8, 15), (8, 16)),
                ((9, 13), (10, 14), (11, 14), (12, 15), (13, 16)), ((10, 13), (11, 13))):
        for i, (x, y) in enumerate(pts):
            cv.put(x, y, HORN[3] if i == 0 else PLATE[5])
    outline(cv, 0.7)
    return cv.image()

LANG={
 'entity.wildercord.rootmolt_strider':'Rootmolt Strider',
 'item.wildercord.rootmolt_strider_spawn_egg':'Rootmolt Strider Spawn Egg',
 'effect.wildercord.root_tether':'Root Tether',
 'guide.wildercord.rootmolt_strider.hint':'Watch broad bark plates below damp cave banks where a snail has matured Glowcaps. Raised shovel arms announce a straight physical rake.',
 'guide.wildercord.rootmolt_strider':'Rootmolts eat mature Glowcaps, then guard the same garden while their next meal settles. Six feet carry bark plates and exposed gills; raised shovel arms commit to one straight physical rake. Step sideways during the warning, strike the creature to break its grip, or cleanse the short Root Tether. Sheltered narrow nurseries can admit a snail while excluding this broader rival. Killing it yields no materials.',
 'subtitles.wildercord.kit.rootmolt.call':'Rootmolt plates rasp',
 'subtitles.wildercord.kit.rootmolt.warn':'Rootmolt shovel arms rise',
 'subtitles.wildercord.kit.rootmolt.rake':'Roots rake across stone',
 'subtitles.wildercord.kit.rootmolt.meal':'Rootmolt gills chew',
 'subtitles.wildercord.kit.rootmolt.release':'Rootmolt roots snap loose',
}


def write(g):
    g.save(skin(), g.ASSETS / "textures/entity/rootmolt_strider.png")
    g.save(root_tether_icon(), g.ASSETS / "textures/mob_effect/root_tether.png")
    egg = egg_icon(("#33231A", "#68482C", "#A47C48"), "#866038", EGG_MOTIF, EGG_PAL, 31)
    g.save(egg, g.ASSETS / "textures/item/rootmolt_strider_spawn_egg.png")
    g.write_json(g.ASSETS / "models/item/rootmolt_strider_spawn_egg.json",
                 {"parent": "minecraft:item/generated", "textures": {"layer0": "wildercord:item/rootmolt_strider_spawn_egg"}})
    g.write_json(g.ASSETS / "items/rootmolt_strider_spawn_egg.json",
                 {"model": {"type": "minecraft:model", "model": "wildercord:item/rootmolt_strider_spawn_egg"}})
    g.write_json(g.DATA / "loot_table/entities/rootmolt_strider.json", {"type": "minecraft:entity", "pools": []})
