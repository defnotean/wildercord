"""Tussock pika: its skin (TussockPikaModel's 64x32 box-UV layout), spawn egg, loot, spawn ground and field-guide text."""
from bobcat_art import egg_icon
from item_art import hexc, ramp
from monster_art import faces, smooth
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.tussock_pika': 'Tussock Pika',
 'item.wildercord.tussock_pika_spawn_egg': 'Tussock Pika Spawn Egg',
 'guide.wildercord.tussock_pika.hint': 'Up on the meadow, a tuft of grass went scurrying past on four tiny feet.',
 'guide.wildercord.tussock_pika': 'A tiny round pika of the meadows and high stony slopes, grey-brown and all fluff, with big '
                                  'round ears, a busy little nose and no tail to speak of. All day it goes haying: it scurries '
                                  'off, nibbles through a stalk of grass and carries it home crosswise in its mouth to add to its '
                                  'pile. On a sunny day it climbs onto bare rock and basks there, flattened out with its eyes '
                                  'shut, until something comes too near. Wheat tempts it and breeds it. Fed, it trusts you a '
                                  'while and scampers after you, and its haying turns to flowers: it nibbles one off and brings '
                                  'it to your feet.',
}

FUR = ramp("#5A4A3A", "#74624E", "#8E7A62", "#A8947A", "#C2AE94")
PALE = ramp("#9A8C78", "#B4A68E", "#CABCA4", "#DCD0BA", "#ECE2CE")
DARK = ramp("#241C16", "#33281F", "#44372B", "#56473A", "#6A5A4A")
GRASS = ramp("#2E5A1E", "#3E7428", "#528E32", "#6AA83E", "#86C050")
PETAL = ramp("#9A3A6A", "#BC4E84", "#DA6CA0", "#EE92BA", "#FAB8D2")
EYE, GLINT, NOSE = hexc("#0E0A08"), hexc("#E8E0D0"), hexc("#3A2620")
CENTER = hexc("#F2C232")

BODY = box(0, 0, 6, 5, 7)
HEAD = box(28, 0, 5, 4, 4)
EAR = box(48, 0, 3, 3, 1)
LEG = box(0, 14, 2, 2, 2)
TUFT = box(10, 14, 7, 1, 1)
STEM = box(28, 10, 5, 1, 1)
BLOOM = box(42, 10, 2, 2, 2)
LID = box(52, 10, 0, 1, 1)

LIGHT = {"top": 1, "bottom": -1, "front": 0, "back": -1, "right": 0, "left": -1}


def fluff(cv, part, seed, tones=FUR, base=2, cell=1.4):
    """Soft dense fluff, lit from above, in little tufts with paler tips among them."""
    for name, (x0, y0, w, h) in faces(part):
        for y in range(h):
            for x in range(w):
                t = base + LIGHT.get(name, 0) + (1 if smooth(x0 + x, y0 + y, seed, cell) > 0.7 else 0)
                n = noise(x0 + x, y0 + y, seed)
                if n > 0.9:
                    t -= 1
                elif n < 0.1:
                    t += 1
                cv.put(x0 + x, y0 + y, tones[max(0, min(4, t))])


def skin():
    cv = Sheet(64, 32)
    fluff(cv, BODY, 10)
    # A pale cream belly.
    fluff(cv, {"bottom": BODY["bottom"]}, 12, tones=PALE, base=2)
    fluff(cv, HEAD, 20)
    # The face: a paler muzzle, a dark little nose and a split lip under it.
    x0, y0, w, h = HEAD["front"]
    for y in range(2, h):
        for x in range(1, w - 1):
            cv.put(x0 + x, y0 + y, PALE[2] if y == 2 else PALE[1])
    cv.put(x0 + 2, y0 + 2, NOSE)
    cv.put(x0 + 2, y0 + 3, DARK[1])
    # Bright round black eyes, high and toward the front, each with a glint.
    for name in ("right", "left"):
        sx, sy, sw, sh = HEAD[name]
        ex = sw - 2 if name == "right" else 1
        cv.put(sx + ex, sy + 1, EYE)
        cv.put(sx + ex + (1 if name == "right" else -1), sy + 1, FUR[1])
        cv.put(sx + ex, sy, GLINT if name == "right" else FUR[3])
        # Paler cheeks below the eye.
        for x in range(sw):
            cv.put(sx + x, sy + sh - 1, PALE[1])
    # Big round ears, dark-rimmed, pinkish-pale inside.
    fluff(cv, EAR, 40, base=2)
    for name in ("front", "back"):
        x0, y0, w, h = EAR[name]
        for y in range(h):
            for x in range(w):
                rim = x in (0, w - 1) or y == 0
                if name == "front":
                    cv.put(x0 + x, y0 + y, DARK[3] if rim else PALE[3])
                elif rim:
                    cv.put(x0 + x, y0 + y, DARK[4])
    for name in ("top", "right", "left"):
        x0, y0, w, h = EAR[name]
        for y in range(h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, DARK[4])
    # The lids it shuts while basking: its own fur, a shade darker.
    for name, (x0, y0, w, h) in faces(LID):
        if w and h:
            cv.put(x0, y0, FUR[1])
    # Tiny feet, paler toward the toes.
    fluff(cv, LEG, 60, tones=PALE, base=1)
    # A tuft of cut grass, green and streaky.
    for name, (x0, y0, w, h) in faces(TUFT):
        for y in range(h):
            for x in range(w):
                t = 2 + LIGHT.get(name, 0) + (1 if noise(x0 + x, y0 + y, 70) > 0.6 else 0) - (1 if (x0 + x) % 3 == 0 else 0)
                cv.put(x0 + x, y0 + y, GRASS[max(0, min(4, t))])
    # A flower: a green stem and a pink bloom with a gold heart.
    for name, (x0, y0, w, h) in faces(STEM):
        for y in range(h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, GRASS[2 + LIGHT.get(name, 0)])
    for name, (x0, y0, w, h) in faces(BLOOM):
        for y in range(h):
            for x in range(w):
                t = 2 + LIGHT.get(name, 0) + (1 if noise(x0 + x, y0 + y, 74) > 0.6 else 0)
                cv.put(x0 + x, y0 + y, PETAL[max(0, min(4, t))])
    x0, y0, w, h = BLOOM["top"]
    cv.put(x0, y0, CENTER)
    cv.put(x0 + 1, y0 + 1, CENTER)
    return cv.image()


def _count(lo, hi):
    return {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/wildlife/tussock_pika.png')
    name = 'tussock_pika_spawn_egg'
    g.save(egg_icon(("#74624E", "#8E7A62", "#C2AE94"), "#33281F", name="tussock_pika"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/tussock_pika.json', {'type': 'minecraft:entity', 'pools': [
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:wheat', 'functions': [_count(0, 1)]}]}],
        'random_sequence': 'wildercord:entities/tussock_pika'})
    g.write_json(g.DATA / 'tags/block/spawns_on/tussock_pika.json', {'replace': False, 'values': [
        'minecraft:grass_block', 'minecraft:stone', 'minecraft:andesite', 'minecraft:granite', 'minecraft:diorite',
        'minecraft:gravel', 'minecraft:calcite', 'minecraft:snow_block']})
