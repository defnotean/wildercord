"""Strawman: its skin (StrawmanModel's 64x64 box-UV layout), the glow of its eyes, spawn egg, loot and field-guide text."""
from bobcat_art import egg_icon
from item_art import hexc, ramp
from monster_art import faces, smooth
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.strawman': 'Strawman',
 'item.wildercord.strawman_spawn_egg': 'Strawman Spawn Egg',
 'guide.wildercord.strawman.hint': 'A scarecrow out in the fields at night that was not standing quite like that a moment ago.',
 'guide.wildercord.strawman': 'A scarecrow of the plains and meadows: a stuffed sack for a head under a tattered hat, a patched '
                              'coat, straw for hands and sticks for legs. By day it stands where it stands and is only a '
                              'scarecrow. At night it hunts, but only while nobody is looking at it. Look its way and it is stock '
                              'still, in whatever way it was standing when you caught it, never the same way twice; turn your back '
                              'and it comes on fast, with a rustle of straw, and strikes from right behind. Anyone looking holds it, '
                              'so two together can watch each other\'s backs. Stare it down too long from close by and the crows '
                              'burst out of its sack and blind you for a moment, and in that moment it moves. It is straw: fire '
                              'takes it twice as hard.',
}

SACK = ramp("#6E5634", "#8A6E44", "#A68A58", "#BCA26E", "#D0B884")
STITCH = ramp("#1C140C", "#2A1E12", "#3A2A18", "#4C3820", "#5E4628")
HAT = ramp("#16141A", "#221F26", "#2E2A32", "#3C3740", "#4A444E")
COAT = ramp("#2A2016", "#3A2C1E", "#4C3A28", "#5E4A34", "#705A40")
PATCH = ramp("#3A2A2A", "#5A3434", "#74443C", "#8A5848", "#A06C58")
PATCH2 = ramp("#2A3428", "#36442E", "#465838", "#566A44", "#687E52")
STRAW = ramp("#8A6A1C", "#B08C2C", "#D0AC40", "#E4C45A", "#F2DA80")
STICK = ramp("#2E2012", "#3E2C18", "#50381E", "#624626", "#74542E")
TWINE = ramp("#4A3C24", "#5E4C30", "#74603C", "#8A744A", "#A08A5C")
EMBER, EMBER_HOT = hexc("#E06A10"), hexc("#FFC040")

HEAD = box(0, 0, 8, 8, 8)
CROWN = box(32, 0, 6, 4, 6)
TIP = box(32, 10, 3, 3, 3)
BRIM = box(0, 16, 12, 1, 12)
TORSO = box(0, 29, 8, 12, 4)
ARM = box(24, 29, 3, 10, 3)
HAND = box(36, 29, 3, 3, 3)
ANKLE = box(36, 35, 3, 2, 3)
LEG = box(48, 16, 2, 12, 2)
HEM = box(0, 47, 9, 4, 5)

LIGHT = {"top": 1, "bottom": -1, "front": 0, "back": -1, "right": 0, "left": -1}


def weave(cv, area, tones, seed, face):
    """Burlap or coarse cloth: a criss-cross weave, blotched with dirt."""
    x0, y0, w, h = area
    for y in range(h):
        for x in range(w):
            t = 2 + LIGHT.get(face, 0) + (1 if (x + y) % 2 == 0 else 0) - (1 if smooth(x, y, seed, 3.0) < 0.3 else 0)
            if noise(x0 + x, y0 + y, seed) > 0.93:
                t -= 1
            cv.put(x0 + x, y0 + y, tones[max(0, min(4, t))])


def straw(cv, area, seed, face):
    """Loose straw: strands running down, bright and dark."""
    x0, y0, w, h = area
    for y in range(h):
        for x in range(w):
            t = 2 + LIGHT.get(face, 0) + (1 if noise(x0 + x, seed, seed) > 0.6 else 0) - (1 if noise(x0 + x, y0 + y, seed + 3) > 0.8 else 0)
            cv.put(x0 + x, y0 + y, STRAW[max(0, min(4, t))])


def skin():
    cv = Sheet(64, 64)
    # The sack head, its front stitched: two hollow eyes and a wide grin, twine tied round the neck.
    for name, area in faces(HEAD):
        weave(cv, area, SACK, 13 + len(name), name)
        if name not in ("top", "bottom"):
            x0, y0, w, h = area
            for x in range(w):
                cv.put(x0 + x, y0 + h - 1, TWINE[2 + (x % 2)])
    x0, y0, w, h = HEAD["front"]
    for ex in (1, 5):
        # Each eye a little hollow ringed in stitches, the ember deep inside.
        cv.put(x0 + ex, y0 + 2, STITCH[1])
        cv.put(x0 + ex + 1, y0 + 2, STITCH[1])
        cv.put(x0 + ex, y0 + 3, STITCH[0])
        cv.put(x0 + ex + 1, y0 + 3, EMBER)
    for x in range(1, 7):
        cv.put(x0 + x, y0 + 5, STITCH[1])
        if x % 2 == 1:
            cv.put(x0 + x, y0 + 6, STITCH[2])
            cv.put(x0 + x, y0 + 4, STITCH[2])
    # A patch sewn over one cheek.
    cv.put(x0 + 6, y0 + 4, PATCH[2])
    cv.put(x0 + 7, y0 + 4, PATCH[2])
    cv.put(x0 + 7, y0 + 5, PATCH[1])
    # Straw sticking out from under the sack at the bottom.
    straw(cv, HEAD["bottom"], 17, "bottom")
    # The hat: black felt gone grey, frayed at the brim, a tear in the crown.
    for part, seed in ((BRIM, 21), (CROWN, 23), (TIP, 29)):
        for name, area in faces(part):
            weave(cv, area, HAT, seed + len(name), name)
    x0, y0, w, h = BRIM["top"]
    for i in range(w):
        if noise(i, 0, 31) > 0.6:
            cv.put(x0 + i, y0, HAT[0])
        if noise(i, 1, 31) > 0.6:
            cv.put(x0 + i, y0 + h - 1, HAT[0])
    x0, y0, w, h = CROWN["front"]
    cv.put(x0 + 4, y0 + 1, STRAW[3])
    cv.put(x0 + 4, y0 + 2, STRAW[2])
    # A band of faded red round the crown.
    for name in ("front", "back", "left", "right"):
        x0, y0, w, h = CROWN[name]
        for x in range(w):
            cv.put(x0 + x, y0 + h - 1, PATCH[2 + LIGHT.get(name, 0)])
    # The coat: dark brown, patched, buttoned down the front, the hem ragged.
    for part, seed in ((TORSO, 41), (ARM, 43), (HEM, 47)):
        for name, area in faces(part):
            weave(cv, area, COAT, seed + len(name), name)
    x0, y0, w, h = TORSO["front"]
    for y in range(h):
        cv.put(x0 + 3, y0 + y, COAT[1])
    for y in range(1, h - 1, 3):
        cv.put(x0 + 4, y0 + y, STICK[4])
    for y in range(2, 5):
        for x in range(5, 8):
            cv.put(x0 + x, y0 + y, PATCH2[2 + (1 if (x + y) % 3 == 0 else 0)])
    cv.put(x0 + 5, y0 + 2, STITCH[3])
    cv.put(x0 + 7, y0 + 4, STITCH[3])
    # Straw poking from the collar.
    x0, y0, w, h = TORSO["top"]
    straw(cv, (x0 + 2, y0, 4, h), 49, "top")
    x0, y0, w, h = TORSO["back"]
    for y in range(6, 10):
        for x in range(1, 4):
            cv.put(x0 + x, y0 + y, PATCH[1 + (1 if (x + y) % 2 else 0)])
    x0, y0, w, h = ARM["right"]
    for y in range(3, 6):
        for x in range(w):
            cv.put(x0 + x, y0 + y, PATCH[2])
    # The hem, torn short in places, straw showing through.
    for name in ("front", "back", "left", "right"):
        x0, y0, w, h = HEM[name]
        for x in range(w):
            if noise(x0 + x, 0, 53) > 0.55:
                cv.put(x0 + x, y0 + h - 1, STRAW[1])
    # Straw for hands and round the ankles, the cuffs bound with twine.
    for part, seed in ((HAND, 61), (ANKLE, 67)):
        for name, area in faces(part):
            straw(cv, area, seed + len(name), name)
    for name in ("front", "back", "left", "right"):
        x0, y0, w, h = HAND[name]
        for x in range(w):
            cv.put(x0 + x, y0, TWINE[2])
    # Legs: two bare sticks, the bark knotted.
    for name, (x0, y0, w, h) in faces(LEG):
        for y in range(h):
            for x in range(w):
                t = 2 + LIGHT.get(name, 0) + (1 if x == 0 else 0) - (1 if noise(x0 + x, y0 + y, 71) > 0.8 else 0)
                cv.put(x0 + x, y0 + y, STICK[max(0, min(4, t))])
    return cv.image()


def eyes():
    cv = Sheet(64, 64)
    x0, y0, w, h = HEAD["front"]
    for ex in (1, 5):
        cv.put(x0 + ex + 1, y0 + 3, (*EMBER_HOT, 255))
        cv.put(x0 + ex, y0 + 3, (*EMBER, 110))
    return cv.image()


def _count(lo, hi):
    return {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}


def _looting(hi):
    return {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting',
            'count': {'type': 'minecraft:uniform', 'min': 0, 'max': hi}}


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/strawman.png')
    g.save(eyes(), g.ASSETS / 'textures/entity/strawman_eyes.png')
    name = 'strawman_spawn_egg'
    g.save(egg_icon(("#3A2C1E", "#A68A58", "#D0AC40"), "#E06A10", name="strawman"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/strawman.json', {'type': 'minecraft:entity', 'pools': [
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:wheat', 'functions': [_count(2, 5), _looting(2)]}]},
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:string', 'functions': [_count(0, 2), _looting(1)]}]},
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:feather', 'functions': [_count(0, 2), _looting(1)]}]},
        {'rolls': 1, 'conditions': [{'condition': 'minecraft:killed_by_player'}, {'condition': 'minecraft:random_chance', 'chance': 0.1}],
         'entries': [{'type': 'minecraft:item', 'name': 'minecraft:carved_pumpkin'}]}],
        'random_sequence': 'wildercord:entities/strawman'})
