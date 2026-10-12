"""Cragcleaver Ram: its fleece (CragcleaverRamModel's 128x64 box-UV layout), spawn egg, loot and field-guide text."""
from bobcat_art import egg_icon
from item_art import hexc, ramp
from monster_art import faces, smooth
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.cragcleaver_ram': 'Cragcleaver Ram',
 'item.wildercord.cragcleaver_ram_spawn_egg': 'Cragcleaver Ram Spawn Egg',
 'guide.wildercord.cragcleaver_ram.hint': 'High on the windswept hills at night, something big stood scraping the ground with one hoof, '
                                          'watching.',
 'guide.wildercord.cragcleaver_ram': 'A huge shaggy grey ram of the windswept hills and snowy slopes, taller than a horse at the '
                                     'shoulder, with a dark face, a beard of wool under its chest and two great ridged horns curled '
                                     'back past its ears and round its jaw. Up close it butts you back. Further off it stops, lowers '
                                     'its head and paws the ground, its eyes on you, then charges straight and fast, and whatever it '
                                     'hits is thrown a long way. Its aim sets a moment before it goes, so step aside at the last '
                                     'moment and it thunders past. Lead it into a wall or a cliff and it stands dazed a few seconds, '
                                     'seeing stars, while your blows land half again as hard.',
}

WOOL = ramp("#5E5C58", "#76736E", "#8E8B85", "#A7A49D", "#C0BDB6")
FACE = ramp("#1C1A18", "#2A2724", "#3A3632", "#4C4742", "#5E5852")
HORN = ramp("#5E4E36", "#7E6A4A", "#9E8862", "#BCA67E", "#D6C49E")
HOOF = hexc("#262220")
EYE, GLINT, NOSE = hexc("#C89A2A"), hexc("#F0D070"), hexc("#0E0C0A")

BODY = box(0, 0, 12, 11, 18)
HEAD = box(64, 0, 7, 7, 8)
SNOUT = box(96, 0, 5, 4, 3)
HORN_BASE = box(64, 16, 3, 3, 5)
HORN_CURL = box(82, 16, 3, 5, 3)
HORN_TIP = box(96, 16, 2, 2, 3)
EAR = box(106, 16, 3, 1, 2)
LEG = box(0, 32, 4, 10, 4)
TAIL = box(20, 32, 3, 4, 2)
BEARD = box(32, 32, 10, 6, 4)

LIGHT = {"top": 1, "bottom": -1, "front": 0, "back": -1, "right": 0, "left": -1}


def fleece(cv, part, seed, tones=WOOL, base=2):
    """Thick shaggy wool in clumps and hanging locks, lit from above, darker in the hollows between."""
    for name, (x0, y0, w, h) in faces(part):
        for y in range(h):
            for x in range(w):
                clump = smooth(x0 + x, y0 + y, seed, 2.2)
                t = base + LIGHT.get(name, 0) + (1 if clump > 0.68 else 0) - (1 if clump < 0.22 else 0)
                # Locks hanging down the sides: a darker line every few columns, broken here and there.
                if name in ("right", "left", "front", "back") and (x0 + x) % 3 == 0 and noise(x0 + x, y0 + y, seed + 3) > 0.45:
                    t -= 1
                if noise(x0 + x, y0 + y, seed) > 0.9:
                    t += 1
                cv.put(x0 + x, y0 + y, tones[max(0, min(4, t))])


def ridged(cv, part, seed, along="y"):
    """Horn, ridged across its length, paler where it catches the light, darker at the root."""
    for name, (x0, y0, w, h) in faces(part):
        for y in range(h):
            for x in range(w):
                i = y if along == "y" or name in ("right", "left") else x
                t = 2 + LIGHT.get(name, 0) - (1 if i % 2 == 0 else 0)
                if noise(x0 + x, y0 + y, seed) > 0.85:
                    t += 1
                cv.put(x0 + x, y0 + y, HORN[max(0, min(4, t))])


def skin():
    cv = Sheet(128, 64)
    fleece(cv, BODY, 10)
    fleece(cv, BEARD, 14, base=3)
    fleece(cv, TAIL, 18)
    # The head: dark and close-furred, with grey wool on the crown between the horns.
    fleece(cv, HEAD, 20, tones=FACE, base=2)
    x0, y0, w, h = HEAD["top"]
    for y in range(h):
        for x in range(w):
            if y >= 3 and noise(x0 + x, y0 + y, 22) > 0.25:
                cv.put(x0 + x, y0 + y, WOOL[3] if noise(x0 + x, y0 + y, 23) > 0.5 else WOOL[2])
    x0, y0, w, h = HEAD["back"]
    for y in range(h):
        for x in range(w):
            cv.put(x0 + x, y0 + y, WOOL[2] if noise(x0 + x, y0 + y, 24) > 0.3 else WOOL[1])
    # Goat-gold eyes with a dark bar of a pupil, set on the sides of the face, and a hard brow over them.
    for name in ("right", "left"):
        sx, sy, sw, sh = HEAD[name]
        ex = 1 if name == "right" else sw - 3
        cv.put(sx + ex, sy + 2, EYE)
        cv.put(sx + ex + 1, sy + 2, EYE)
        cv.put(sx + ex + (1 if name == "right" else 0), sy + 2, NOSE)
        cv.put(sx + ex + (0 if name == "right" else 1), sy + 1, GLINT)
        cv.put(sx + ex, sy + 1, FACE[0])
        cv.put(sx + ex + 1, sy + 1, FACE[0])
    x0, y0, w, h = HEAD["front"]
    for x in range(w):
        cv.put(x0 + x, y0, FACE[1])
    # The snout: dark, paler at the lip, with two slit nostrils.
    fleece(cv, SNOUT, 30, tones=FACE, base=3)
    x0, y0, w, h = SNOUT["front"]
    cv.put(x0 + 1, y0 + 1, NOSE)
    cv.put(x0 + 3, y0 + 1, NOSE)
    for x in range(w):
        cv.put(x0 + x, y0 + h - 1, FACE[2])
    cv.put(x0 + 2, y0 + h - 1, NOSE)
    fleece(cv, EAR, 40, tones=FACE, base=3)
    # Horns: dark at the root, paler toward the tip.
    ridged(cv, HORN_BASE, 50, along="z")
    ridged(cv, HORN_CURL, 52)
    ridged(cv, HORN_TIP, 54, along="z")
    for name, (x0, y0, w, h) in faces(HORN_TIP):
        for y in range(h):
            for x in range(w):
                if noise(x0 + x, y0 + y, 56) > 0.4:
                    cv.put(x0 + x, y0 + y, HORN[4])
    # Legs: woolly above, dark and bare below, with black hooves.
    fleece(cv, LEG, 60)
    for name, (x0, y0, w, h) in faces(LEG):
        if name in ("top", "bottom"):
            if name == "bottom":
                for y in range(h):
                    for x in range(w):
                        cv.put(x0 + x, y0 + y, HOOF)
            continue
        for y in range(4, h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, HOOF if y >= h - 2 else FACE[2 + (1 if noise(x0 + x, y0 + y, 62) > 0.7 else 0)])
    return cv.image()


def _count(lo, hi):
    return {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}


def _looting(hi):
    return {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting',
            'count': {'type': 'minecraft:uniform', 'min': 0, 'max': hi}}


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/cragcleaver_ram.png')
    name = 'cragcleaver_ram_spawn_egg'
    g.save(egg_icon(("#76736E", "#8E8B85", "#A7A49D"), "#2A2724", name="cragcleaver_ram"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/cragcleaver_ram.json', {'type': 'minecraft:entity', 'pools': [
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:mutton', 'functions': [_count(1, 3), _looting(1)]}]},
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:gray_wool', 'functions': [_count(0, 2)]}]}],
        'random_sequence': 'wildercord:entities/cragcleaver_ram'})
