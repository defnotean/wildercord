"""Floe penguin: its skin and its chick's (FloePenguinModel's 64x32 box-UV layout), spawn egg, loot, spawn ground and
field-guide text."""
from bobcat_art import egg_icon
from item_art import hexc, ramp
from monster_art import faces, smooth
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.floe_penguin': 'Floe Penguin',
 'item.wildercord.floe_penguin_spawn_egg': 'Floe Penguin Spawn Egg',
 'guide.wildercord.floe_penguin.hint': 'Out on the ice, something black and white flopped onto its belly and went sailing past.',
 'guide.wildercord.floe_penguin': 'A small round penguin of the frozen shores, black of back and head and white of belly, with '
                                  'a white ring round each eye, a stubby beak and pink feet. On land it waddles, rocking from '
                                  'foot to foot; in a hurry on ice or snow it flops onto its belly and toboggans along, far '
                                  'faster than it can walk. In the water it is quick and sleek. Now and then it goes down to '
                                  'open water, dives, and comes up with a fish crosswise in its beak: it gulps it down itself, '
                                  'unless there is a chick about, which it brings it to. At night the colony gathers in a tight '
                                  'huddle and dozes, heads tucked in, and anyone standing in among them is kept warm. Raw cod '
                                  'and salmon tempt it and breed it. Fed, it trusts you a while and follows you about, and its '
                                  'catch is yours: it brings the fish to your feet.',
}

BLACK = ramp("#121419", "#1B1E25", "#262A33", "#323743", "#414755")
WHITE = ramp("#B4BAC4", "#CDD2DA", "#E0E4EA", "#EEF1F5", "#FAFBFD")
BEAK = ramp("#1A1612", "#2A221C", "#3A2E26", "#C2603A", "#E07C48")
FOOT = ramp("#8A4E50", "#A86266", "#C47A7C", "#DA9494", "#ECB0AE")
DOWN = ramp("#5C6068", "#73777F", "#8A8E96", "#A2A6AC", "#BABDC2")
DOWN_DARK = ramp("#2A2C32", "#363940", "#44474F", "#53575F", "#646870")
COD = ramp("#6A5A3E", "#857250", "#A08C66", "#BBA880", "#D4C49C")
SALMON = ramp("#6E2A26", "#8E3A32", "#AE4E40", "#C86A52", "#DC8A6E")
EYE, GLINT = hexc("#0A0A0C"), hexc("#E8ECF2")

BODY = box(0, 0, 6, 9, 5)
HEAD = box(24, 0, 5, 4, 4)
BEAK_BOX = box(44, 0, 2, 1, 3)
FLIPPER = box(24, 10, 1, 6, 3)
FOOT_BOX = box(34, 10, 2, 1, 3)
TAIL = box(46, 10, 3, 1, 2)
COD_BOX = box(0, 16, 5, 2, 1)
SALMON_BOX = box(0, 20, 5, 2, 1)

LIGHT = {"top": 1, "bottom": -1, "front": 0, "back": -1, "right": 0, "left": -1}


def sleek(cv, part, seed, tones, base=2, names=()):
    """Short sleek feathers, lit from above, with a faint sheen in soft streaks."""
    for name, (x0, y0, w, h) in faces(part, *names):
        for y in range(h):
            for x in range(w):
                t = base + LIGHT.get(name, 0) + (1 if smooth(x0 + x, y0 + y, seed, 1.8) > 0.74 else 0)
                if noise(x0 + x, y0 + y, seed) > 0.93:
                    t -= 1
                cv.put(x0 + x, y0 + y, tones[max(0, min(4, t))])


def plain(cv, part, tones, base=2, names=()):
    for name, (x0, y0, w, h) in faces(part, *names):
        for y in range(h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, tones[max(0, min(4, base + LIGHT.get(name, 0)))])


def fishes(cv):
    """A cod and a salmon to carry, crosswise: head at one end, a dark eye, a fin, a forked tail at the other."""
    for part, tones in ((COD_BOX, COD), (SALMON_BOX, SALMON)):
        sleek(cv, part, 80, tones)
        for name in ("front", "back"):
            x0, y0, w, h = part[name]
            for x in range(w):
                cv.put(x0 + x, y0 + 1, tones[3] if x else tones[2])
            cv.put(x0 + (0 if name == "front" else w - 1), y0, tones[1])
            cv.put(x0 + (1 if name == "front" else w - 2), y0, EYE)
            cv.put(x0 + (w - 1 if name == "front" else 0), y0, tones[0])
            cv.put(x0 + (w - 1 if name == "front" else 0), y0 + 1, tones[0])


def skin():
    cv = Sheet(64, 32)
    # Black back, white front: the belly is the front face, and the white wraps a little way round the sides.
    sleek(cv, BODY, 10, BLACK)
    sleek(cv, BODY, 12, WHITE, names=("front", "bottom"))
    for name in ("right", "left"):
        x0, y0, w, h = BODY[name]
        edge = w - 1 if name == "right" else 0
        for y in range(1, h):
            cv.put(x0 + edge, y0 + y, WHITE[1])
    # The white bib stops in a curve under the chin, with black coming down a little at the top corners.
    x0, y0, w, h = BODY["front"]
    for x in range(w):
        cv.put(x0 + x, y0, BLACK[2])
    cv.put(x0, y0 + 1, BLACK[2])
    cv.put(x0 + w - 1, y0 + 1, BLACK[2])

    # An all-black head with a white ring round each eye.
    sleek(cv, HEAD, 20, BLACK)
    for name in ("right", "left"):
        sx, sy, sw, sh = HEAD[name]
        ex = sw - 2 if name == "right" else 1
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                if dx or dy:
                    px = ex + dx
                    if 0 <= px < sw:
                        cv.put(sx + px, sy + 1 + dy, WHITE[3])
        cv.put(sx + ex, sy + 1, EYE)
    x0, y0, w, h = HEAD["front"]
    cv.put(x0, y0 + 1, WHITE[3])
    cv.put(x0 + w - 1, y0 + 1, WHITE[3])
    # A stubby dark beak, with orange at its base and along its gape.
    plain(cv, BEAK_BOX, BEAK, base=1)
    for name in ("right", "left"):
        x0, y0, w, h = BEAK_BOX[name]
        cv.put(x0 + (0 if name == "right" else w - 1), y0, BEAK[4])
    x0, y0, w, h = BEAK_BOX["bottom"]
    for y in range(h):
        cv.put(x0, y0 + y, BEAK[3])
        cv.put(x0 + 1, y0 + y, BEAK[3])

    # Flippers: black outside, white underneath and along the inner face.
    sleek(cv, FLIPPER, 30, BLACK)
    for name in ("left",):
        sleek(cv, FLIPPER, 32, WHITE, names=(name,))
    for name in ("right", "left"):
        x0, y0, w, h = FLIPPER[name]
        for x in range(w):
            cv.put(x0 + x, y0 + h - 1, BLACK[3] if name == "right" else WHITE[1])
    # Pink feet, darker claws at the toes.
    plain(cv, FOOT_BOX, FOOT)
    x0, y0, w, h = FOOT_BOX["top"]
    for x in range(w):
        cv.put(x0 + x, y0, FOOT[1])
    sleek(cv, TAIL, 40, BLACK)
    fishes(cv)
    return cv.image()


def chick():
    """A chick: grey down all over, darker on the head, with a pale face round its eyes."""
    cv = Sheet(64, 32)
    for name, (x0, y0, w, h) in faces(BODY):
        for y in range(h):
            for x in range(w):
                t = 2 + LIGHT.get(name, 0) + (1 if smooth(x0 + x, y0 + y, 50, 1.3) > 0.62 else 0) - (1 if noise(x0 + x, y0 + y, 51) > 0.85 else 0)
                cv.put(x0 + x, y0 + y, DOWN[max(0, min(4, t))])
    for name, (x0, y0, w, h) in faces(HEAD):
        for y in range(h):
            for x in range(w):
                t = 2 + LIGHT.get(name, 0) + (1 if smooth(x0 + x, y0 + y, 52, 1.3) > 0.62 else 0)
                cv.put(x0 + x, y0 + y, DOWN_DARK[max(0, min(4, t))])
    for name in ("right", "left"):
        sx, sy, sw, sh = HEAD[name]
        ex = sw - 2 if name == "right" else 1
        for dx in (-1, 0, 1):
            for dy in (0, 1):
                if 0 <= ex + dx < sw:
                    cv.put(sx + ex + dx, sy + 1 + dy, DOWN[3])
        cv.put(sx + ex, sy + 1, EYE)
        cv.put(sx + ex, sy, GLINT if name == "right" else DOWN_DARK[3])
    plain(cv, BEAK_BOX, BEAK, base=1)
    for name, (x0, y0, w, h) in faces(FLIPPER):
        for y in range(h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, DOWN[1 + LIGHT.get(name, 0) + 1])
    plain(cv, FOOT_BOX, FOOT, base=1)
    plain(cv, TAIL, DOWN, base=1)
    fishes(cv)
    return cv.image()


def _count(lo, hi):
    return {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/wildlife/floe_penguin.png')
    g.save(chick(), g.ASSETS / 'textures/entity/wildlife/floe_penguin_chick.png')
    name = 'floe_penguin_spawn_egg'
    g.save(egg_icon(("#1B1E25", "#262A33", "#414755"), "#EEF1F5", name="floe_penguin"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/floe_penguin.json', {'type': 'minecraft:entity', 'pools': [
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:feather', 'functions': [_count(0, 1)]}]},
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:cod', 'functions': [_count(0, 1)]}]}],
        'random_sequence': 'wildercord:entities/floe_penguin'})
    g.write_json(g.DATA / 'tags/block/spawns_on/floe_penguin.json', {'replace': False, 'values': [
        'minecraft:snow_block', 'minecraft:snow', 'minecraft:ice', 'minecraft:packed_ice', 'minecraft:gravel', 'minecraft:sand',
        'minecraft:stone', 'minecraft:grass_block']})
