"""Pitchback Beetle: its shell (PitchbackBeetleModel's 64x64 box-UV layout), the glow of its resin sac and eyes, spawn egg, loot and field-guide text."""
from bobcat_art import egg_icon
from item_art import hexc, ramp
from monster_art import faces, smooth
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.pitchback_beetle': 'Pitchback Beetle',
 'item.wildercord.pitchback_beetle_spawn_egg': 'Pitchback Beetle Spawn Egg',
 'guide.wildercord.pitchback_beetle.hint': 'Under the spruces at night, a faint amber glow low to the ground, and a smell of pitch.',
 'guide.wildercord.pitchback_beetle': 'A great dark bark beetle of the taiga, longer than your arm, with a ridged black shell, '
                                      'hooked red mandibles, long feelers, and swelling out behind its wing cases a sac of amber '
                                      'resin that glows faintly in the dark. It keeps its distance: from a few blocks off it rears '
                                      'up, the sac swelling, and lobs a glob of resin in an arc. One glob gums you up and slows '
                                      'you, and a second while you are still gummed sticks you fast where you stand. Come too '
                                      'close and it backs away, snapping. But fire bursts the sac in a flash of flame that catches '
                                      'everything near it, the beetle too, and with its sac empty it has nothing left to throw and '
                                      'comes on with its mandibles until the sac fills again. Fire hurts it twice as hard besides.',
}

SHELL = ramp("#0E0B0A", "#1A1412", "#271E1A", "#362A24", "#4A3A30")
SHEEN = hexc("#5E4A3A")
JOINT = ramp("#2A1810", "#3C2216", "#52301E", "#6A3E26", "#824E30")
JAW = ramp("#3A0E0A", "#5A1810", "#7A2418", "#963222", "#B0442E")
RESIN = ramp("#7A3A08", "#A85A10", "#D0841C", "#E8A830", "#F8CC58")
EYE = hexc("#E8A030")

THORAX = box(0, 0, 8, 5, 5)
HEAD = box(26, 0, 6, 4, 4)
MANDIBLE = box(46, 0, 1, 1, 4)
FEELER = box(56, 0, 1, 7, 1)
ABDOMEN = box(0, 10, 10, 5, 10)
SAC = box(40, 10, 6, 5, 6)
CASE = box(0, 25, 5, 3, 11)
LEG = box(32, 25, 6, 1, 1)
SHIN = box(46, 25, 1, 8, 1)

LIGHT = {"top": 1, "bottom": -1, "front": 0, "back": -1, "right": 0, "left": -1}


def shell(cv, part, seed, tones=SHELL, base=2):
    """Hard dark chitin, lit from above, with a faint pitted sheen."""
    for name, (x0, y0, w, h) in faces(part):
        for y in range(h):
            for x in range(w):
                t = base + LIGHT.get(name, 0) + (1 if smooth(x0 + x, y0 + y, seed, 2.5) > 0.75 else 0)
                c = tones[max(0, min(4, t))]
                if noise(x0 + x, y0 + y, seed) > 0.93:
                    c = tones[max(0, min(4, t - 1))]
                cv.put(x0 + x, y0 + y, c)


def skin():
    cv = Sheet(64, 64)
    shell(cv, THORAX, 10)
    # The thorax's top: a ridge down the middle and a sheen at the front.
    x0, y0, w, h = THORAX["top"]
    for y in range(h):
        cv.put(x0 + w // 2 - 1, y0 + y, SHELL[4])
    for x in range(1, w - 1):
        cv.put(x0 + x, y0 + h - 1, SHEEN if x % 3 else SHELL[4])
    shell(cv, HEAD, 20)
    x0, y0, w, h = HEAD["front"]
    cv.put(x0, y0 + 1, EYE)
    cv.put(x0 + w - 1, y0 + 1, EYE)
    for name in ("right", "left"):
        sx, sy, sw, sh = HEAD[name]
        cv.put(sx + (0 if name == "right" else sw - 1), sy + 1, EYE)
    shell(cv, MANDIBLE, 30, JAW)
    x0, y0, w, h = MANDIBLE["front"]
    cv.put(x0, y0, JAW[0])
    shell(cv, FEELER, 34, JOINT)
    for name in ("front", "back", "right", "left"):
        x0, y0, w, h = FEELER[name]
        for y in range(0, h, 2):
            cv.put(x0, y0 + y, JOINT[1])
    # The abdomen under the wing cases: banded segments, paler joints between.
    shell(cv, ABDOMEN, 40)
    for name in ("bottom", "right", "left"):
        x0, y0, w, h = ABDOMEN[name]
        if name == "bottom":
            for y in range(1, h, 2):
                for x in range(w):
                    cv.put(x0 + x, y0 + y, JOINT[1])
        else:
            for x in range(1, w, 2):
                for y in range(h):
                    cv.put(x0 + x, y0 + y, JOINT[0])
    # The wing cases: ridged lengthwise, a sheen along the top of each ridge.
    shell(cv, CASE, 50)
    x0, y0, w, h = CASE["top"]
    for y in range(h):
        for x in range(w):
            if x % 2 == 1:
                cv.put(x0 + x, y0 + y, SHELL[0])
            elif noise(x0 + x, y0 + y, 51) > 0.6:
                cv.put(x0 + x, y0 + y, SHEEN)
    for name in ("right", "left", "back", "front"):
        x0, y0, w, h = CASE[name]
        for x in range(w):
            cv.put(x0 + x, y0, SHELL[3])
    # The resin sac: translucent amber with darker veins and bright bubbles.
    for name, (x0, y0, w, h) in faces(SAC):
        for y in range(h):
            for x in range(w):
                t = 2 + LIGHT.get(name, 0) + (1 if smooth(x0 + x, y0 + y, 61, 2.0) > 0.6 else 0) - (1 if smooth(x0 + x, y0 + y, 62, 1.5) > 0.82 else 0)
                c = RESIN[max(0, min(4, t))]
                if noise(x0 + x, y0 + y, 63) > 0.92:
                    c = hexc("#FFE89A")
                cv.put(x0 + x, y0 + y, c)
    # Legs: dark, jointed, with reddish-brown joints.
    shell(cv, LEG, 70, base=1)
    shell(cv, SHIN, 74, base=1)
    for name in ("front", "back", "right", "left"):
        x0, y0, w, h = SHIN[name]
        cv.put(x0, y0, JOINT[2])
        cv.put(x0, y0 + h - 1, JOINT[1])
    return cv.image()


def glow():
    cv = Sheet(64, 64)
    for name, (x0, y0, w, h) in faces(SAC):
        for y in range(h):
            for x in range(w):
                a = 150 + int(90 * smooth(x0 + x, y0 + y, 61, 2.0))
                cv.put(x0 + x, y0 + y, (*RESIN[4], min(255, a)))
    x0, y0, w, h = HEAD["front"]
    cv.put(x0, y0 + 1, (*EYE, 200))
    cv.put(x0 + w - 1, y0 + 1, (*EYE, 200))
    return cv.image()


def _count(lo, hi):
    return {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}


def _looting(hi):
    return {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting',
            'count': {'type': 'minecraft:uniform', 'min': 0, 'max': hi}}


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/pitchback_beetle.png')
    g.save(glow(), g.ASSETS / 'textures/entity/pitchback_beetle_glow.png')
    name = 'pitchback_beetle_spawn_egg'
    g.save(egg_icon(("#1A1412", "#271E1A", "#362A24"), "#E8A830", name="pitchback_beetle"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/pitchback_beetle.json', {'type': 'minecraft:entity', 'pools': [
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:resin_clump', 'functions': [_count(1, 3), _looting(1)]}]},
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:string', 'functions': [_count(0, 1)]}]}],
        'random_sequence': 'wildercord:entities/pitchback_beetle'})
