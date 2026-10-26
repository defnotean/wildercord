"""Coffer mimic: its skin (CofferMimicModel's 64x64 box-UV layout), the glow of its eyes, spawn egg, loot and field-guide text."""
from bobcat_art import egg_icon
from item_art import hexc, mix, ramp
from monster_art import faces, fill, smooth
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.coffer_mimic': 'Coffer Mimic',
 'item.wildercord.coffer_mimic_spawn_egg': 'Coffer Mimic Spawn Egg',
 'guide.wildercord.coffer_mimic.hint': 'Not every chest out on its own in the wilds is a chest. Watch the lid a while before you open it.',
 'guide.wildercord.coffer_mimic': 'An iron-bound oak coffer that is nothing of the kind. Shut, it sits square to the world and keeps '
                                  'perfectly still; but stand near it a while and now and then its lid lifts a hair and settles with a '
                                  'faint creak, and a drop of drool runs out from under it. Open it, strike it or linger right beside '
                                  'it and its lid flies open on rows of teeth. It bites up close, shoots out its long tongue to haul '
                                  'prey in, and from further off crouches, its lid chattering, and leaps; miss, and it lands on its '
                                  'back with its legs kicking, soft to blows. Hurt hard and fast, it clamps shut. Left alone, it walks '
                                  'back to its spot and shuts. Its belly is full of gold. Feed it books, though, and now and then '
                                  'it takes to you: it follows you about, shuts up as a coffer beside you whenever you stop, and opens '
                                  'like a chest for you alone.',
 'message.wildercord.coffer_mimic.tamed': 'The coffer mimic takes to you.',
 'message.wildercord.coffer_mimic.pet': 'The coffer mimic’s lid lifts happily.',
 'message.wildercord.coffer_mimic.stay': 'The coffer mimic stays put.',
 'message.wildercord.coffer_mimic.follow': 'The coffer mimic follows you.',
 'message.wildercord.coffer_mimic.not_yours': 'This coffer mimic won’t open for you.',
}

OAK = ramp("#4A2E14", "#62401E", "#7A5228", "#926434", "#A87640")
OAK_DARK = ramp("#2E1C0C", "#3C2612", "#4A3018", "#5A3C1E", "#684624")
IRON = ramp("#2E2E32", "#45454C", "#5E5E66", "#7E7E86", "#A2A2AA")
GULLET = ramp("#140406", "#2E080C", "#4A1014", "#6A1C1E", "#86302A")
PALATE = ramp("#6A2A34", "#84384A", "#A04C5E", "#B86472", "#CC7E88")
BONE = ramp("#8E8470", "#B0A68C", "#CEC6AA", "#E2DCC4", "#F2EEDC")
TONGUE = ramp("#7A2E44", "#9C3C58", "#BC5070", "#D06A86", "#E08AA0")
EYE, EYE_HOT, SLIT = hexc("#E8B820"), hexc("#FFE86A"), hexc("#140C02")

LID = box(0, 0, 14, 5, 14)
BASE = box(0, 19, 14, 10, 14)
TOOTH = box(56, 0, 1, 2, 1)
FANG = box(56, 4, 1, 3, 1)
EYES = box(56, 9, 2, 2, 1)
LOCK = box(56, 14, 2, 4, 1)
TONGUE_SEG = box(0, 43, 3, 1, 4)
LEG = box(16, 43, 3, 4, 3)
TIP = box(30, 43, 3, 1, 3)

LIGHT = {"top": 1, "bottom": -1, "front": 0, "back": -1, "right": 0, "left": -1}


def planks(cv, area, seed, face, tones=OAK, seam=4, vertical=False):
    """Oak boards: grain along each board, a dark seam between boards, a knot now and then."""
    x0, y0, w, h = area
    for y in range(h):
        for x in range(w):
            along, across = (y, x) if vertical else (x, y)
            board = across // seam
            if across % seam == seam - 1:
                c = tones[0]
            else:
                grain = smooth(along * 0.35 + board * 7, across * 2.0, seed + board, 2.0)
                t = 2 + LIGHT.get(face, 0) + (1 if grain > 0.68 else 0) - (1 if grain < 0.25 else 0)
                c = tones[max(0, min(4, t))]
                if noise(x0 + x, y0 + y, seed) > 0.97:
                    c = tones[1]
            cv.put(x0 + x, y0 + y, c)


def bands(cv, area, face, corners=True):
    """The iron binding round a face's edge, riveted at the corners."""
    x0, y0, w, h = area
    t = 2 + LIGHT.get(face, 0)
    for x in range(w):
        cv.put(x0 + x, y0, IRON[min(4, t + 1)])
        cv.put(x0 + x, y0 + h - 1, IRON[max(0, t - 1)])
    for y in range(h):
        cv.put(x0, y0 + y, IRON[t])
        cv.put(x0 + w - 1, y0 + y, IRON[max(0, t - 1)])
    if corners and w > 4 and h > 4:
        for cx, cy in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)):
            cv.put(x0 + cx, y0 + cy, IRON[4])


def maw(cv, area, tones, seed, ridges=False):
    """Inside the mouth: a wooden rim, then flesh darkening toward the throat (the palate ridged across)."""
    x0, y0, w, h = area
    cx, cy = (w - 1) / 2, (h - 1) / 2
    for y in range(h):
        for x in range(w):
            if x == 0 or y == 0 or x == w - 1 or y == h - 1:
                cv.put(x0 + x, y0 + y, OAK_DARK[2])
                continue
            d = max(abs(x - cx) / cx, abs(y - cy) / cy)
            t = 1 + int(d * 3.2) + (1 if smooth(x, y, seed, 2.5) > 0.7 else 0)
            if ridges and y % 3 == 0 and 1 < x < w - 2:
                t -= 1
            cv.put(x0 + x, y0 + y, tones[max(0, min(4, t))])
    if not ridges:
        # The throat, a dark hole in the middle.
        for y in range(5, 9):
            for x in range(5, 9):
                if not ((y in (5, 8)) and (x in (5, 8))):
                    cv.put(x0 + x, y0 + y, GULLET[0])


def skin():
    cv = Sheet(64, 64)
    # The lid: boards across the top, the iron binding round every face.
    for name, area in faces(LID):
        if name == "bottom":
            maw(cv, area, PALATE, 71, ridges=True)
            continue
        planks(cv, area, 11 + len(name), name, seam=5 if name == "top" else 3)
        bands(cv, area, name, corners=name == "top")
    # A band across the middle of the lid top, front to back.
    x0, y0, w, h = LID["top"]
    for y in range(h):
        cv.put(x0 + 6, y0 + y, IRON[3])
        cv.put(x0 + 7, y0 + y, IRON[2])
    # The base: boards and binding outside, the gullet inside.
    for name, area in faces(BASE):
        if name == "top":
            maw(cv, area, GULLET, 83)
            continue
        planks(cv, area, 31 + len(name), name, tones=OAK if name != "bottom" else OAK_DARK, seam=3 if name != "bottom" else 5)
        bands(cv, area, name)
    # A drool stain running down from the front rim.
    x0, y0, w, h = BASE["front"]
    for y in range(1, 5):
        cv.put(x0 + 9, y0 + y, mix(OAK[1], PALATE[1], 0.35))
    # Teeth and fangs, bone-white, the tips palest.
    for part in (TOOTH, FANG):
        for name, (x0, y0, w, h) in faces(part):
            for y in range(h):
                for x in range(w):
                    t = 2 + (1 if y == 0 else 0) - (1 if y == h - 1 and h > 1 else 0) + LIGHT.get(name, 0)
                    cv.put(x0 + x, y0 + y, BONE[max(0, min(4, t))])
    # Eyes: slitted yellow on the front, flesh round the rest.
    for name, area in faces(EYES):
        fill(cv, area, GULLET[2])
    x0, y0, w, h = EYES["front"]
    cv.put(x0, y0, EYE_HOT)
    cv.put(x0, y0 + 1, EYE)
    cv.put(x0 + 1, y0, SLIT)
    cv.put(x0 + 1, y0 + 1, SLIT)
    # The lock: iron, with a keyhole.
    for name, area in faces(LOCK):
        fill(cv, area, IRON[2 + LIGHT.get(name, 0)])
    x0, y0, w, h = LOCK["front"]
    for y in range(h):
        cv.put(x0, y0 + y, IRON[3])
        cv.put(x0 + 1, y0 + y, IRON[2])
    cv.put(x0, y0 + 1, IRON[0])
    cv.put(x0, y0 + 2, IRON[0])
    # The tongue: pink, a groove down the middle, the tip rounded.
    for part, seed in ((TONGUE_SEG, 91), (TIP, 97)):
        for name, (x0, y0, w, h) in faces(part):
            for y in range(h):
                for x in range(w):
                    t = 2 + LIGHT.get(name, 0) + (1 if noise(x0 + x, y0 + y, seed) > 0.75 else 0)
                    if name == "top" and x == w // 2:
                        t -= 1
                    cv.put(x0 + x, y0 + y, TONGUE[max(0, min(4, t))])
    x0, y0, w, h = TIP["top"]
    cv.put(x0, y0, TONGUE[1])
    cv.put(x0 + w - 1, y0, TONGUE[1])
    # Legs: dark knotted oak, splintered toes.
    for name, area in faces(LEG):
        planks(cv, area, 101 + len(name), name, tones=OAK_DARK, seam=99, vertical=True)
        x0, y0, w, h = area
        if name not in ("top", "bottom"):
            for x in range(w):
                cv.put(x0 + x, y0 + h - 1, OAK_DARK[0])
    return cv.image()


def eyes():
    cv = Sheet(64, 64)
    x0, y0, w, h = EYES["front"]
    cv.put(x0, y0, (*EYE_HOT, 255))
    cv.put(x0, y0 + 1, (*EYE, 255))
    cv.put(x0 + 1, y0, (*mix(SLIT, EYE, 0.3), 120))
    cv.put(x0 + 1, y0 + 1, (*mix(SLIT, EYE, 0.3), 120))
    return cv.image()


def _count(lo, hi):
    return {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}


def _looting(hi):
    return {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting',
            'count': {'type': 'minecraft:uniform', 'min': 0, 'max': hi}}


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/coffer_mimic.png')
    g.save(eyes(), g.ASSETS / 'textures/entity/coffer_mimic_eyes.png')
    name = 'coffer_mimic_spawn_egg'
    g.save(egg_icon(("#4A2E14", "#7A5228", "#A87640"), "#E8B820", name="coffer_mimic"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/coffer_mimic.json', {'type': 'minecraft:entity', 'pools': [
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:gold_ingot', 'functions': [_count(1, 4), _looting(1)]}]},
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:emerald', 'functions': [_count(0, 2)]}]},
        {'rolls': 1, 'conditions': [{'condition': 'minecraft:random_chance', 'chance': 0.5}], 'entries': [
            {'type': 'minecraft:item', 'name': 'minecraft:golden_apple', 'weight': 2},
            {'type': 'minecraft:item', 'name': 'minecraft:name_tag', 'weight': 3},
            {'type': 'minecraft:item', 'name': 'minecraft:diamond', 'weight': 1}]},
        {'rolls': 1, 'conditions': [{'condition': 'minecraft:killed_by_player'}, {'condition': 'minecraft:random_chance', 'chance': 0.25}],
         'entries': [{'type': 'minecraft:item', 'name': 'minecraft:book', 'functions': [{'function': 'minecraft:enchant_randomly'}]}]}],
        'random_sequence': 'wildercord:entities/coffer_mimic'})
