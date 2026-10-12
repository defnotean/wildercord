"""Cackling Hyena: its spotted coat (CacklingHyenaModel's 64x64 box-UV layout), the shine of its eyes, spawn egg, loot and field-guide text."""
from bobcat_art import egg_icon
from item_art import hexc, ramp
from monster_art import faces, smooth
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.cackling_hyena': 'Cackling Hyena',
 'item.wildercord.cackling_hyena_spawn_egg': 'Cackling Hyena Spawn Egg',
 'guide.wildercord.cackling_hyena.hint': 'Out on the savanna at night, laughter in the grass, going round and round.',
 'guide.wildercord.cackling_hyena': 'A scruffy spotted hyena of the savannas, high in the shoulder and low in the hip, with a '
                                    'bristling mane, round ears, a dark muzzle and eyes that shine amber in the dark. They hunt '
                                    'in packs at night and never all at once: the pack circles you at a distance, cackling, and '
                                    'one at a time one of them crouches and darts in to bite, sooner if your back is turned. The '
                                    'bite hamstrings you. Then it falls back to the circle and another takes its turn. Watch for '
                                    'the crouch and turn to face it; a dart that misses leaves it stumbling, and a blow then '
                                    'hurts it badly. Once you are badly hurt the pack grows bold, the circle tightens and the '
                                    'darts come quicker. A badly hurt hyena slinks off with its tail tucked for a while, and '
                                    'comes back.',
}

COAT = ramp("#6E5A3C", "#8A7450", "#A68E64", "#BEA67A", "#D2BC90")
SPOT = ramp("#2A1E14", "#3A2A1C", "#4A3826", "#5A4630", "#6A543A")
DARK = ramp("#16100C", "#221A14", "#30261C", "#3E3226", "#4C3E30")
BELLY = ramp("#9A8A6A", "#B0A07E", "#C4B492", "#D4C6A6", "#E2D6BA")
EYE, AMBER = hexc("#1A0E06"), hexc("#FFB030")

CHEST = box(0, 0, 7, 8, 7)
RUMP = box(28, 0, 6, 6, 7)
HEAD = box(0, 16, 6, 6, 5)
MUZZLE = box(22, 16, 3, 2, 4)
JAW = box(36, 16, 3, 1, 4)
EAR = box(50, 16, 2, 3, 1)
MANE = box(0, 28, 2, 2, 12)
TAIL = box(28, 28, 1, 5, 1)
TUFT = box(32, 28, 2, 3, 2)
FORELEG = box(0, 44, 2, 10, 2)
HIND = box(8, 44, 2, 8, 2)

LIGHT = {"top": 1, "bottom": -1, "front": 0, "back": -1, "right": 0, "left": -1}


def coat(cv, part, seed, spots=0.0, belly=True):
    """Coarse sandy fur, lit from above, scattered with dark blotchy spots, paler beneath."""
    for name, (x0, y0, w, h) in faces(part):
        for y in range(h):
            for x in range(w):
                t = 2 + LIGHT.get(name, 0) - (1 if smooth(x0 + x, y0 + y, seed, 2.0) < 0.3 else 0)
                if noise(x0 + x, y0 + y, seed) > 0.88:
                    t += 1
                tones = BELLY if belly and name == "bottom" else COAT
                c = tones[max(0, min(4, t))]
                if spots and name != "bottom" and smooth(x0 + x, y0 + y, seed + 7, 1.6) > 1 - spots:
                    c = SPOT[max(0, min(4, t))]
                cv.put(x0 + x, y0 + y, c)


def dark(cv, part, seed, tones=DARK):
    for name, (x0, y0, w, h) in faces(part):
        for y in range(h):
            for x in range(w):
                t = 2 + LIGHT.get(name, 0) + (1 if noise(x0 + x, y0 + y, seed) > 0.8 else 0)
                cv.put(x0 + x, y0 + y, tones[max(0, min(4, t))])


def skin():
    cv = Sheet(64, 64)
    coat(cv, CHEST, 10, spots=0.28)
    coat(cv, RUMP, 20, spots=0.3)
    coat(cv, HEAD, 30, spots=0.08)
    # The face: dark around the eyes and down onto the muzzle, the eyes themselves.
    x0, y0, w, h = HEAD["front"]
    for y in range(2, h):
        for x in range(1, w - 1):
            if y >= 3 or x in (1, w - 2):
                cv.put(x0 + x, y0 + y, DARK[3 if y < 4 else 2])
    cv.put(x0 + 1, y0 + 2, EYE)
    cv.put(x0 + w - 2, y0 + 2, EYE)
    for name, col in (("right", 4), ("left", 0)):
        sx, sy, sw, sh = HEAD[name]
        cv.put(sx + col, sy + 2, EYE)
    dark(cv, MUZZLE, 40)
    x0, y0, w, h = MUZZLE["front"]
    cv.put(x0 + 1, y0, DARK[0])  # the nose
    dark(cv, JAW, 44)
    # Teeth along the top of the jaw's front edge.
    x0, y0, w, h = JAW["top"]
    for x in range(w):
        cv.put(x0 + x, y0, hexc("#E8DEC8"))
    # Round ears: coat outside, dark rims, pale within.
    coat(cv, EAR, 50)
    x0, y0, w, h = EAR["front"]
    for y in range(h):
        for x in range(w):
            cv.put(x0 + x, y0 + y, DARK[2] if y == 0 else BELLY[1])
    # The mane: dark and bristly, ragged.
    for name, (x0, y0, w, h) in faces(MANE):
        for y in range(h):
            for x in range(w):
                t = 2 + LIGHT.get(name, 0) + (1 if noise(x0 + x, y0 + y, 61) > 0.7 else 0) - (1 if noise(x0 + x, y0 + y, 62) > 0.8 else 0)
                cv.put(x0 + x, y0 + y, (SPOT if (x + y) % 3 else DARK)[max(0, min(4, t))])
    coat(cv, TAIL, 70)
    dark(cv, TUFT, 74)
    # Legs: spotted above, darker toward the feet.
    for part, seed in ((FORELEG, 80), (HIND, 90)):
        coat(cv, part, seed, spots=0.35, belly=False)
        for name in ("front", "back", "right", "left"):
            x0, y0, w, h = part[name]
            for y in range(h - 2, h):
                for x in range(w):
                    cv.put(x0 + x, y0 + y, DARK[3])
    return cv.image()


def eyes():
    cv = Sheet(64, 64)
    x0, y0, w, h = HEAD["front"]
    cv.put(x0 + 1, y0 + 2, (*AMBER, 255))
    cv.put(x0 + w - 2, y0 + 2, (*AMBER, 255))
    for name, col in (("right", 4), ("left", 0)):
        sx, sy, sw, sh = HEAD[name]
        cv.put(sx + col, sy + 2, (*AMBER, 160))
    return cv.image()


def _count(lo, hi):
    return {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}


def _looting(hi):
    return {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting',
            'count': {'type': 'minecraft:uniform', 'min': 0, 'max': hi}}


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/cackling_hyena.png')
    g.save(eyes(), g.ASSETS / 'textures/entity/cackling_hyena_eyes.png')
    name = 'cackling_hyena_spawn_egg'
    g.save(egg_icon(("#8A7450", "#A68E64", "#BEA67A"), "#2A1E14", name="cackling_hyena"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/cackling_hyena.json', {'type': 'minecraft:entity', 'pools': [
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:bone', 'functions': [_count(0, 2), _looting(1)]}]},
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:leather', 'functions': [_count(0, 1), _looting(1)]}]}],
        'random_sequence': 'wildercord:entities/cackling_hyena'})
