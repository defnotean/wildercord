"""Foxfire Kitsune: its white coat (FoxfireKitsuneModel's 64x64 box-UV layout), the foxfire in its tails and eyes, spawn egg, loot and field-guide text."""
from bobcat_art import egg_icon
from item_art import hexc, ramp
from monster_art import faces, smooth
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.foxfire_kitsune': 'Foxfire Kitsune',
 'item.wildercord.foxfire_kitsune_spawn_egg': 'Foxfire Kitsune Spawn Egg',
 'guide.wildercord.foxfire_kitsune.hint': 'In the cherry groves at night, three blue lights moving together.',
 'guide.wildercord.foxfire_kitsune': 'A slim white fox of the cherry groves, bigger than any fox, with red markings on its face '
                                     'and three great fluffy tails tipped with cold blue fire. From a distance it fans its tails '
                                     'up and flicks a wisp of foxfire at you; the wisp sails slow and straight, so step aside, '
                                     'for it burns where it lands. Up close it bites. Hurt past two thirds of its health, and '
                                     'again past a third, it spins and splits into three of itself in a puff of blue fire, and '
                                     'takes the place of one of its copies. The copies run and snap and throw just as it does, '
                                     'but their bites close on nothing, their fire is cold and any blow scatters one. Only the '
                                     'real one sheds cherry petals as it runs, and only the real one casts a shadow. The copies '
                                     'fade after a while, or once the real one is gone.',
}

WHITE = ramp("#B8BCC8", "#CDD0DA", "#E0E2EA", "#EEF0F5", "#FAFBFD")
SHADE = ramp("#8A8E9C", "#A0A4B2", "#B4B8C6", "#C6CAD6", "#D6D9E3")
RED = ramp("#7A1420", "#9E1E2C", "#C02C38", "#D84450", "#EC6670")
FIRE = ramp("#1E5AA8", "#2E80D0", "#4AA8EE", "#86D0FA", "#D2F0FF")
EYE, NOSE = hexc("#2A1A3A"), hexc("#3A2A34")

BODY = box(0, 0, 5, 5, 10)
HEAD = box(0, 16, 6, 5, 5)
SNOUT = box(22, 16, 2, 2, 3)
EAR = box(32, 16, 2, 3, 1)
TAIL = box(0, 28, 4, 4, 8)
TIP = box(24, 28, 4, 4, 3)
LEG = box(0, 44, 2, 8, 2)

LIGHT = {"top": 1, "bottom": -1, "front": 0, "back": -1, "right": 0, "left": -1}


def fur(cv, part, seed, tones=WHITE, base=2, cell=1.8):
    """Soft white fur, lit from above, in faint clumps, a little cooler in the shadows."""
    for name, (x0, y0, w, h) in faces(part):
        for y in range(h):
            for x in range(w):
                t = base + LIGHT.get(name, 0) - (1 if smooth(x0 + x, y0 + y, seed, cell) < 0.3 else 0)
                if noise(x0 + x, y0 + y, seed) > 0.9:
                    t -= 1
                cv.put(x0 + x, y0 + y, tones[max(0, min(4, t))])


def skin():
    cv = Sheet(64, 64)
    fur(cv, BODY, 10)
    # A thin red line down the spine.
    x0, y0, w, h = BODY["top"]
    for y in range(h):
        if y % 4 != 3:
            cv.put(x0 + 2, y0 + y, RED[2])
    fur(cv, {"bottom": BODY["bottom"]}, 12, tones=SHADE, base=3)
    fur(cv, HEAD, 20)
    # The face: red sweeps up from the eyes to the brow, and a red mark on the forehead.
    x0, y0, w, h = HEAD["front"]
    cv.put(x0 + 1, y0 + 2, EYE)
    cv.put(x0 + 4, y0 + 2, EYE)
    for x, y in ((0, 1), (1, 1), (0, 2), (4, 1), (5, 1), (5, 2)):
        cv.put(x0 + x, y0 + y, RED[2])
    cv.put(x0 + 2, y0, RED[1])
    cv.put(x0 + 3, y0, RED[1])
    cv.put(x0 + 2, y0 + 1, RED[3])
    cv.put(x0 + 3, y0 + 1, RED[3])
    for name, col in (("right", 4), ("left", 0)):
        sx, sy, sw, sh = HEAD[name]
        cv.put(sx + col, sy + 2, EYE)
        cv.put(sx + (col - 1 if col else col + 1), sy + 1, RED[2])
    fur(cv, SNOUT, 30)
    x0, y0, w, h = SNOUT["front"]
    cv.put(x0, y0, NOSE)
    cv.put(x0 + 1, y0, NOSE)
    x0, y0, w, h = SNOUT["bottom"]
    for x in range(w):
        for y in range(h):
            cv.put(x0 + x, y0 + y, SHADE[2])
    # Tall ears: white outside, red within.
    fur(cv, EAR, 40)
    x0, y0, w, h = EAR["front"]
    for y in range(h):
        for x in range(w):
            cv.put(x0 + x, y0 + y, RED[1] if y else RED[3])
    # Fluffy tails, white to the tip, where the fire takes over.
    fur(cv, TAIL, 50, cell=1.4)
    for name, (x0, y0, w, h) in faces(TIP):
        for y in range(h):
            for x in range(w):
                t = 2 + (1 if noise(x0 + x, y0 + y, 60) > 0.5 else 0) + (1 if name == "back" and 0 < x < w - 1 and 0 < y < h - 1 else 0)
                cv.put(x0 + x, y0 + y, FIRE[max(0, min(4, t))])
    # Long thin legs, white with dark red socks.
    fur(cv, LEG, 70)
    for name in ("front", "back", "right", "left"):
        x0, y0, w, h = LEG[name]
        for y in range(h - 2, h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, RED[0 if y == h - 1 else 1])
    return cv.image()


def glow():
    """Only what glows: the fire in its tail tips, and its eyes."""
    cv = Sheet(64, 64)
    src = skin()
    for name, (x0, y0, w, h) in faces(TIP):
        for y in range(h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, src.getpixel((x0 + x, y0 + y)))
    x0, y0, w, h = HEAD["front"]
    for ex in (1, 4):
        cv.put(x0 + ex, y0 + 2, (*FIRE[3], 255))
    for name, col in (("right", 4), ("left", 0)):
        sx, sy, sw, sh = HEAD[name]
        cv.put(sx + col, sy + 2, (*FIRE[3], 170))
    return cv.image()


def _count(lo, hi):
    return {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}


def _looting(hi):
    return {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting',
            'count': {'type': 'minecraft:uniform', 'min': 0, 'max': hi}}


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/foxfire_kitsune.png')
    g.save(glow(), g.ASSETS / 'textures/entity/foxfire_kitsune_glow.png')
    name = 'foxfire_kitsune_spawn_egg'
    g.save(egg_icon(("#CDD0DA", "#E0E2EA", "#FAFBFD"), "#4AA8EE", name="foxfire_kitsune"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/foxfire_kitsune.json', {'type': 'minecraft:entity', 'pools': [
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:glowstone_dust', 'functions': [_count(0, 2), _looting(1)]}]}],
        'random_sequence': 'wildercord:entities/foxfire_kitsune'})
