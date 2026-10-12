"""Gulper Pitcher: its skin (GulperPitcherModel's 64x64 box-UV layout), the glow of its nectar, spawn egg, loot and field-guide text."""
from bobcat_art import egg_icon
from item_art import hexc, ramp
from monster_art import faces, smooth
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.gulper_pitcher': 'Gulper Pitcher',
 'item.wildercord.gulper_pitcher_spawn_egg': 'Gulper Pitcher Spawn Egg',
 'guide.wildercord.gulper_pitcher.hint': 'In the jungle at night, something smells very sweet, and there is a vine lying on the ground that was not there before.',
 'guide.wildercord.gulper_pitcher': 'A carnivorous plant of the jungles as tall as a man: a great green pitcher veined in red on a '
                                    'rosette of leaves, its rim beaded with glowing nectar, a lid over its mouth and a long vine '
                                    'coiled at its foot. It is rooted where it grew and never moves, nor can it be pushed. Asleep, '
                                    'it sways and drips; come near and it opens its lid, though crouched you can get much closer. '
                                    'Out of reach of its mouth, it throws its lid wide and rears its vine back over itself, and then '
                                    'lashes it out and hauls you in towards it; get behind something or out of reach before it '
                                    'lashes. At its mouth it slams its lid down on you, and the acid of its pool poisons. It is a '
                                    'plant, and fire takes it twice as hard.',
}

GREEN = ramp("#2A4A1C", "#3A6224", "#4E7C2E", "#64963A", "#80B04A")
RED = ramp("#4A0E14", "#6A1620", "#8A202A", "#A82E34", "#C44440")
LEAF = ramp("#1E3A16", "#2C5020", "#3C682A", "#508436", "#68A044")
VINE = ramp("#22401A", "#305622", "#40702C", "#548A38", "#6AA246")
TOOTH = ramp("#A89A70", "#C0B488", "#D4CAA0", "#E4DCB8", "#F2ECD0")
POOL = ramp("#3A1A0A", "#5A2A10", "#7A3E18", "#9A5820", "#C08030")
NECTAR = hexc("#FFC850")

BELLY = box(0, 0, 8, 9, 8)
NECK = box(32, 0, 6, 8, 6)
RIM = box(0, 18, 9, 2, 9)
TEETH = box(36, 18, 7, 1, 1)
VINE_BOX = box(36, 22, 1, 1, 5)
HOOK = box(48, 22, 2, 2, 3)
LID = box(0, 30, 9, 1, 9)
LEAF_BOX = box(0, 42, 4, 1, 8)

LIGHT = {"top": 1, "bottom": -1, "front": 0, "back": -1, "right": 0, "left": -1}


def shade(cv, area, tones, seed, face, cell=2.5, base=2):
    """Soft mottling, lit from above."""
    x0, y0, w, h = area
    for y in range(h):
        for x in range(w):
            t = base + LIGHT.get(face, 0) - (1 if smooth(x0 + x, y0 + y, seed, cell) < 0.3 else 0)
            if noise(x0 + x, y0 + y, seed) > 0.93:
                t += 1
            cv.put(x0 + x, y0 + y, tones[max(0, min(4, t))])


def veined(cv, area, seed, face, blush):
    """The pitcher's wall: green, veined in red running up it, flushing redder towards the top (blush 0..1 over the face)."""
    shade(cv, area, GREEN, seed, face)
    x0, y0, w, h = area
    for y in range(h):
        for x in range(w):
            up = 1 - y / max(1, h - 1)
            red = blush[0] + (blush[1] - blush[0]) * up
            if smooth(x0 + x, y0 + y, seed + 5, 2.0) < red * 0.75:
                cv.put(x0 + x, y0 + y, RED[2 + LIGHT.get(face, 0) + (1 if smooth(x0 + x, y0 + y, seed + 6, 2.0) > 0.7 else 0)])
            elif face not in ("top", "bottom") and x % 3 == 1 and noise(x0 + x, y0, seed + 8) > 0.25 and y < h * (0.5 + noise(x0 + x, y0, seed + 9)):
                # Veins running up the wall from the base.
                cv.put(x0 + x, y0 + h - 1 - y, RED[1 + max(0, LIGHT.get(face, 0))])


def skin():
    cv = Sheet(64, 64)
    for name, area in faces(BELLY):
        veined(cv, area, 10 + len(name), name, (0.0, 0.25))
    for name, area in faces(NECK):
        veined(cv, area, 20 + len(name), name, (0.25, 0.6))
    # The rim: thick and red, ribbed all round; its top a ring round a dark pool.
    for name, area in faces(RIM):
        x0, y0, w, h = area
        if name == "top":
            for y in range(h):
                for x in range(w):
                    edge = min(x, y, w - 1 - x, h - 1 - y)
                    if edge == 0:
                        c = RED[3 if (x + y) % 2 == 0 else 2]
                    else:
                        d = max(abs(x - (w - 1) / 2), abs(y - (h - 1) / 2))
                        c = POOL[max(0, min(4, int(4 - d)))]
                    cv.put(x0 + x, y0 + y, c)
            continue
        shade(cv, area, RED, 30 + len(name), name)
        for y in range(h):
            for x in range(w):
                if x % 2 == 0:
                    cv.put(x0 + x, y0 + y, RED[1 + max(0, LIGHT.get(name, 0))])
    # The lid: green above, red and glistening beneath.
    for name, area in faces(LID):
        if name == "bottom":
            shade(cv, area, RED, 40, name, base=3)
        else:
            veined(cv, area, 42 + len(name), name, (0.2, 0.2))
    for name, area in faces(TEETH):
        shade(cv, area, TOOTH, 50 + len(name), name, base=2)
    # Its teeth, a zigzag along the front.
    x0, y0, w, h = TEETH["front"]
    for x in range(w):
        cv.put(x0 + x, y0, TOOTH[3] if x % 2 == 0 else RED[1])
    for name, area in faces(VINE_BOX):
        shade(cv, area, VINE, 60 + len(name), name)
        x0, y0, w, h = area
        for y in range(h):
            for x in range(w):
                if (x + y) % 3 == 0:
                    cv.put(x0 + x, y0 + y, VINE[1])
    for name, area in faces(HOOK):
        shade(cv, area, LEAF, 70 + len(name), name, base=3)
    # A red barb at the hook's point.
    x0, y0, w, h = HOOK["front"]
    cv.put(x0, y0 + h - 1, RED[3])
    cv.put(x0 + 1, y0 + h - 1, RED[3])
    for name, area in faces(LEAF_BOX):
        shade(cv, area, LEAF, 80 + len(name), name, base=2)
        if name == "top":
            # A pale midrib down the leaf, red at the tip.
            x0, y0, w, h = area
            for y in range(h):
                cv.put(x0 + 1, y0 + y, LEAF[4])
            cv.put(x0 + 1, y0, RED[3])
            cv.put(x0 + 2, y0, RED[2])
    return cv.image()


def nectar():
    cv = Sheet(64, 64)
    # Beads of nectar round the rim and the pool glowing in the mouth.
    x0, y0, w, h = RIM["top"]
    for y in range(h):
        for x in range(w):
            edge = min(x, y, w - 1 - x, h - 1 - y)
            d = max(abs(x - (w - 1) / 2), abs(y - (h - 1) / 2))
            if edge == 0 and noise(x0 + x, y0 + y, 91) > 0.6:
                cv.put(x0 + x, y0 + y, (*NECTAR, 220))
            elif edge > 0 and d < 3:
                cv.put(x0 + x, y0 + y, (*NECTAR, 140 if d < 2 else 70))
    for name in ("front", "right", "left", "back"):
        x0, y0, w, h = RIM[name]
        for x in range(w):
            if noise(x0 + x, y0, 93) > 0.7:
                cv.put(x0 + x, y0, (*NECTAR, 180))
    x0, y0, w, h = LID["bottom"]
    for y in range(h):
        for x in range(w):
            if noise(x0 + x, y0 + y, 95) > 0.85:
                cv.put(x0 + x, y0 + y, (*NECTAR, 120))
    return cv.image()


def _count(lo, hi):
    return {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}


def _looting(hi):
    return {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting',
            'count': {'type': 'minecraft:uniform', 'min': 0, 'max': hi}}


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/gulper_pitcher.png')
    g.save(nectar(), g.ASSETS / 'textures/entity/gulper_pitcher_nectar.png')
    name = 'gulper_pitcher_spawn_egg'
    g.save(egg_icon(("#3A6224", "#64963A", "#80B04A"), "#A82E34", name="gulper_pitcher"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/gulper_pitcher.json', {'type': 'minecraft:entity', 'pools': [
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:vine', 'functions': [_count(1, 3), _looting(1)]}]},
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:slime_ball', 'functions': [_count(0, 1), _looting(1)]}]},
        {'rolls': 1, 'conditions': [{'condition': 'minecraft:killed_by_player'},
                                    {'condition': 'minecraft:random_chance', 'chance': 0.15}],
         'entries': [{'type': 'minecraft:item', 'name': 'minecraft:pitcher_pod'}]}],
        'random_sequence': 'wildercord:entities/gulper_pitcher'})
