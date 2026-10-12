"""Rimecoat Wolverine: its coat (RimecoatWolverineModel's 64x64 box-UV layout), spawn egg, loot and field-guide text."""
from bobcat_art import egg_icon
from item_art import hexc, ramp
from monster_art import faces, smooth
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.rimecoat_wolverine': 'Rimecoat Wolverine',
 'item.wildercord.rimecoat_wolverine_spawn_egg': 'Rimecoat Wolverine Spawn Egg',
 'monster.wildercord.rimecoat_wolverine.stole': 'A wolverine snatched your %s!',
 'guide.wildercord.rimecoat_wolverine.hint': 'Out on the snowy plains, a low dark shape that seems more interested in your pack than in you.',
 'guide.wildercord.rimecoat_wolverine': 'A low, broad, shaggy wolverine of the snowy plains, dark brown with a pale band along each '
                                        'flank and across its brow, its coat stiff with rime, and a thief. Close in it growls, then '
                                        'darts in, bites, and comes away with whatever you were eating, or whatever food it can get '
                                        'its teeth into from your pack, and runs. Far enough off it stops to eat it, and what it eats '
                                        'heals it. Hit it as it runs and it may drop what it carries; kill it and you get it back. Food '
                                        'on the ground draws it too, so a thrown porkchop buys you some peace. With no food to take it '
                                        'just bites. But cornered and badly hurt it drops whatever it carries and turns on you, biting '
                                        'twice as fast.',
}

FUR = ramp("#1A100A", "#2A1A10", "#3A2618", "#4C3222", "#5E402C")
BAND = ramp("#6E5236", "#8A6A48", "#A6865E", "#BEA078", "#D2B892")
MUZZLE = ramp("#2A221C", "#3E342C", "#544840", "#6A5E54", "#82766A")
RIME = hexc("#E4EEF6")
RIME_SHADE = hexc("#B8C8D8")
NOSE = hexc("#0C0808")
EYE = hexc("#080606")
GLINT = hexc("#C8D0D8")

BODY = box(0, 0, 8, 7, 14)
HEAD = box(0, 22, 6, 5, 5)
MUZ = box(22, 22, 4, 3, 3)
EAR = box(36, 22, 2, 2, 1)
LEG = box(0, 32, 3, 6, 3)
TAIL = box(14, 32, 3, 3, 6)

LIGHT = {"top": 1, "bottom": -1, "front": 0, "back": -1, "right": 0, "left": -1}


def shag(cv, part, seed, tones=FUR, base=2, rime=0.0):
    """Long coarse fur in vertical strands, lit from above, with rime caught on the tips."""
    for name, (x0, y0, w, h) in faces(part):
        for y in range(h):
            for x in range(w):
                t = base + LIGHT.get(name, 0) + (1 if smooth(x0 + x, (y0 + y) // 2, seed, 2.0) > 0.7 else 0)
                if noise(x0 + x, y0 + y, seed) > 0.85:
                    t -= 1
                c = tones[max(0, min(4, t))]
                if rime and name in ("top", "right", "left", "front", "back") and noise(x0 + x, y0 + y, seed + 5) > 1 - rime * (1.6 if name == "top" else 0.6):
                    c = RIME if noise(x0 + x, y0 + y, seed + 6) > 0.4 else RIME_SHADE
                cv.put(x0 + x, y0 + y, c)


def skin():
    cv = Sheet(64, 64)
    shag(cv, BODY, 10, rime=0.12)
    # The pale band along each flank, from shoulder to rump, a little ragged.
    for name in ("right", "left"):
        x0, y0, w, h = BODY[name]
        for x in range(w):
            mid = 2 + (1 if smooth(x0 + x, y0, 12, 2.0) > 0.6 else 0)
            for y in range(mid, mid + 2):
                if 1 <= x < w - 1:
                    cv.put(x0 + x, y0 + y, BAND[2 + (1 if noise(x0 + x, y0 + y, 13) > 0.6 else 0)])
            if noise(x0 + x, y0, 14) > 0.75 and 1 <= x < w - 1:
                cv.put(x0 + x, y0 + mid + 2, BAND[1])
    # The rump and chest go darker, the belly darkest.
    for name in ("bottom",):
        x0, y0, w, h = BODY[name]
        for y in range(h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, FUR[0] if noise(x0 + x, y0 + y, 15) > 0.3 else FUR[1])
    # The head: dark, with the pale brow band across the top and down to the ears.
    shag(cv, HEAD, 20, rime=0.06)
    x0, y0, w, h = HEAD["front"]
    for x in range(w):
        cv.put(x0 + x, y0, BAND[2])
    cv.put(x0 + 1, y0 + 2, EYE)
    cv.put(x0 + w - 2, y0 + 2, EYE)
    cv.put(x0 + 1, y0 + 1, GLINT)
    cv.put(x0 + w - 2, y0 + 1, GLINT)
    x0, y0, w, h = HEAD["top"]
    for x in range(w):
        for y in range(h - 2, h):
            cv.put(x0 + x, y0 + y, BAND[2 + (1 if noise(x0 + x, y0 + y, 21) > 0.5 else 0)])
    for name in ("right", "left"):
        x0, y0, w, h = HEAD[name]
        for x in range(w):
            cv.put(x0 + x, y0, BAND[1])
    # The muzzle: greyer, a black nose, a dark mouth line.
    shag(cv, MUZ, 30, MUZZLE)
    x0, y0, w, h = MUZ["front"]
    cv.put(x0 + 1, y0, NOSE)
    cv.put(x0 + 2, y0, NOSE)
    for x in range(w):
        cv.put(x0 + x, y0 + h - 1, FUR[0])
    shag(cv, EAR, 40, BAND, base=1)
    x0, y0, w, h = EAR["front"]
    for x in range(w):
        cv.put(x0 + x, y0 + h - 1, FUR[1])
    # Legs: near black, the paws darkest with pale claws.
    shag(cv, LEG, 50, base=1)
    for name in ("front", "back", "right", "left"):
        x0, y0, w, h = LEG[name]
        for x in range(w):
            cv.put(x0 + x, y0 + h - 1, FUR[0])
        if name == "front":
            for x in range(w):
                cv.put(x0 + x, y0 + h - 1, BAND[3] if x % 2 == 0 else FUR[0])
    # The tail: bushy, dark at the tip, rime along the top.
    shag(cv, TAIL, 60, rime=0.1)
    x0, y0, w, h = TAIL["back"]
    for y in range(h):
        for x in range(w):
            cv.put(x0 + x, y0 + y, FUR[0])
    return cv.image()


def _count(lo, hi):
    return {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}


def _looting(hi):
    return {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting',
            'count': {'type': 'minecraft:uniform', 'min': 0, 'max': hi}}


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/rimecoat_wolverine.png')
    name = 'rimecoat_wolverine_spawn_egg'
    g.save(egg_icon(("#2A1A10", "#3A2618", "#4C3222"), "#BEA078", name="rimecoat_wolverine"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/rimecoat_wolverine.json', {'type': 'minecraft:entity', 'pools': [
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:leather', 'functions': [_count(1, 2), _looting(1)]}]},
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:snowball', 'functions': [_count(0, 2)]}]}],
        'random_sequence': 'wildercord:entities/rimecoat_wolverine'})
