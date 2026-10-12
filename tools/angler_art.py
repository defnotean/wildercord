"""Glowlure Angler: its skin and its lure's glow (GlowlureAnglerModel's 64x64 box-UV layout), spawn egg, loot and
field-guide text."""
from bobcat_art import egg_icon
from item_art import hexc, ramp
from monster_art import faces, smooth
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.glowlure_angler': 'Glowlure Angler',
 'item.wildercord.glowlure_angler_spawn_egg': 'Glowlure Angler Spawn Egg',
 'guide.wildercord.glowlure_angler.hint': 'In the lush caves, a glow berry hung in the dark where no vine grew.',
 'guide.wildercord.glowlure_angler': 'A broad flat cave salamander of the lush caves, eyeless and pale moss-green, with a '
                                     'mouth as wide as its body. From its brow a stalk bends forward to hang a glowing bulb '
                                     'before its jaws, just like a glow berry. It lies flat and still in the dark with only '
                                     'the lure swaying, and whoever stares at the lure finds themselves drifting toward it; '
                                     'look away, or sneak, and the pull breaks. Come too close and its jaw drops wide, then '
                                     'it lunges and snaps: the bite is deep and leaves you slowed. Step aside and it misses, '
                                     'sprawling winded with its jaw hanging, and your blows land half again as hard. Slow on '
                                     'land, it soon gives up a chase and settles back down to wait.',
}

SKIN = ramp("#3E5236", "#526A46", "#68845A", "#7E9C6E", "#98B486")
BELLY = ramp("#8E9A78", "#A6B08E", "#BCC4A4", "#CED4B6", "#E0E4CA")
MOSS = ramp("#2C3E22", "#3A5230", "#4A663C", "#5E7C4A", "#76945C")
MOUTH = ramp("#2A1418", "#3E1E22", "#5A2C30", "#7A3E40", "#9A5454")
TOOTH = ramp("#9A9480", "#B4AE98", "#CCC6AE", "#E0DAC4", "#F2EEDC")
LURE = ramp("#B86A1C", "#DA8A28", "#F2AA3A", "#FCC85A", "#FFE494")
BLIND = hexc("#B6BEA6")

BODY = box(0, 0, 8, 4, 10)
STALK = box(40, 0, 1, 5, 1)
REACH = box(46, 0, 1, 1, 4)
BULB = box(40, 8, 3, 3, 3)
SKULL = box(0, 14, 9, 4, 6)
JAW = box(30, 14, 9, 2, 6)
TEETH = box(0, 26, 8, 1, 1)
LEG = box(0, 30, 2, 3, 2)
TAIL = box(0, 36, 4, 3, 10)
TIP = box(30, 36, 2, 2, 6)

LIGHT = {"top": 1, "bottom": -1, "front": 0, "back": -1, "right": 0, "left": -1}


def hide(cv, part, seed, tones=SKIN, base=2, names=()):
    """Soft damp skin, lit from above, blotched with darker moss-green patches and pale freckles."""
    for name, (x0, y0, w, h) in faces(part, *names):
        for y in range(h):
            for x in range(w):
                t = base + LIGHT.get(name, 0)
                if smooth(x0 + x, y0 + y, seed, 2.4) > 0.66:
                    t -= 1
                n = noise(x0 + x, y0 + y, seed)
                if n > 0.94:
                    t += 2
                elif n < 0.06:
                    t -= 1
                cv.put(x0 + x, y0 + y, tones[max(0, min(4, t))])


def bulb(cv):
    """The lure: a round amber glow, brightest at its heart."""
    for name, (x0, y0, w, h) in faces(BULB):
        for y in range(h):
            for x in range(w):
                centre = x == w // 2 and y == h // 2
                cv.put(x0 + x, y0 + y, LURE[4] if centre else LURE[3 if (x + y) % 2 else 2])


def skin():
    cv = Sheet(64, 64)
    hide(cv, BODY, 10)
    hide(cv, BODY, 12, tones=BELLY, names=("bottom",))
    # Moss-dark warty blotches running down the spine.
    x0, y0, w, h = BODY["top"]
    for y in range(h):
        for x in (3, 4):
            if noise(x0 + x, y0 + y, 14) > 0.35:
                cv.put(x0 + x, y0 + y, MOSS[2 if y % 3 else 1])

    hide(cv, SKULL, 20)
    hide(cv, SKULL, 22, tones=BELLY, base=1, names=("bottom",))
    # Two pale blind eye-spots, filmed over, on the skull's sides near the front.
    for name in ("right", "left"):
        sx, sy, sw, sh = SKULL[name]
        ex = 1 if name == "right" else sw - 2
        cv.put(sx + ex, sy + 1, BLIND)
    # The lip line along the front and sides, where the skull meets the jaw.
    x0, y0, w, h = SKULL["front"]
    for x in range(w):
        cv.put(x0 + x, y0 + h - 1, MOSS[1])
    hide(cv, JAW, 30, base=1)
    hide(cv, JAW, 32, tones=BELLY, base=2, names=("bottom",))
    # Inside the jaw: a dark red mouth, a fringe of pale teeth round its rim.
    x0, y0, w, h = JAW["top"]
    for y in range(h):
        for x in range(w):
            rim = x == 0 or x == w - 1 or y == 0
            cv.put(x0 + x, y0 + y, TOOTH[3] if rim and (x + y) % 2 == 0 else MOUTH[1 + (1 if noise(x0 + x, y0 + y, 34) > 0.7 else 0)])
    x0, y0, w, h = SKULL["bottom"]
    for y in range(h):
        for x in range(w):
            cv.put(x0 + x, y0 + y, MOUTH[2] if 0 < x < w - 1 and y > 0 else BELLY[1])
    for name, (x0, y0, w, h) in faces(TEETH):
        for y in range(h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, TOOTH[3] if x % 2 == 0 else TOOTH[1])

    # The stalk: skin-coloured at the root, paling and warming toward the bulb.
    hide(cv, STALK, 40, base=2)
    hide(cv, REACH, 42, tones=BELLY, base=2)
    bulb(cv)

    hide(cv, LEG, 50)
    for name, (x0, y0, w, h) in faces(LEG, "front", "back", "right", "left"):
        for x in range(w):
            cv.put(x0 + x, y0 + h - 1, MOSS[1])
    hide(cv, TAIL, 60)
    hide(cv, TAIL, 62, tones=BELLY, names=("bottom",))
    hide(cv, TIP, 64)
    return cv.image()


def glow():
    """Only what glows: the lure."""
    cv = Sheet(64, 64)
    bulb(cv)
    for name, (x0, y0, w, h) in faces(REACH):
        if w >= 4:
            cv.put(x0, y0, (*LURE[1], 150))
        elif w >= 1 and h >= 1:
            cv.put(x0, y0, (*LURE[1], 110))
    return cv.image()


def _count(lo, hi):
    return {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}


def _looting(hi):
    return {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting',
            'count': {'type': 'minecraft:uniform', 'min': 0, 'max': hi}}


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/glowlure_angler.png')
    g.save(glow(), g.ASSETS / 'textures/entity/glowlure_angler_glow.png')
    name = 'glowlure_angler_spawn_egg'
    g.save(egg_icon(("#526A46", "#68845A", "#7E9C6E"), "#FCC85A", name="glowlure_angler"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/glowlure_angler.json', {'type': 'minecraft:entity', 'pools': [
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:glow_berries', 'functions': [_count(1, 2), _looting(1)]}]},
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:slime_ball', 'functions': [_count(0, 1)]}]}],
        'random_sequence': 'wildercord:entities/glowlure_angler'})
