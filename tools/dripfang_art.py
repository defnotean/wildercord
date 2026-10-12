"""Dripfang Lurker: its shell (DripfangLurkerModel's 64x32 box-UV layout), spawn egg, loot and field-guide text."""
from bobcat_art import egg_icon
from item_art import hexc, ramp
from monster_art import faces, smooth
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.dripfang_lurker': 'Dripfang Lurker',
 'item.wildercord.dripfang_lurker_spawn_egg': 'Dripfang Lurker Spawn Egg',
 'guide.wildercord.dripfang_lurker.hint': 'Down among the dripstone, one of the stalactites overhead had legs.',
 'guide.wildercord.dripfang_lurker': 'A long pale cave crawler of the dripstone caves, its shell the dull brown-grey of '
                                     'dripstone, tapering back into a stone spike. It climbs to the roof and hangs there tail '
                                     'down among the stalactites with its legs folded tight, and passes for one of them. Walk '
                                     'right underneath and it lets go, spike first: the landing hurts and leaves you slowed. '
                                     'Its only tell is a leg twitching now and then, and a faint click, for those who look up. '
                                     'Strike it down off the roof first and it lands on its back, legs waving, and your blows '
                                     'land half again as hard until it rights itself. Left alone a while, it climbs back up.',
}

SHELL = ramp("#4A3E34", "#65564A", "#82725F", "#9E8E78", "#B8A992")
STONE = ramp("#3E3832", "#57504A", "#726A62", "#8E867C", "#AAA298")
BELLY = ramp("#8C8274", "#A69C8C", "#BEB4A2", "#D2C8B6", "#E2DAC8")
DARK = ramp("#1A1612", "#26201A", "#342C24", "#443A30", "#56493C")
FANG = ramp("#8A7E66", "#A89A7C", "#C4B694", "#DCD0AE", "#EEE4C6")
EYE = hexc("#6E7A6A")
GLINT = hexc("#B6C4AC")

BODY = box(0, 0, 5, 3, 8)
HEAD = box(28, 0, 4, 3, 3)
FANG_BOX = box(44, 0, 1, 2, 1)
RIDGE = box(50, 0, 1, 1, 2)
ABDOMEN = box(0, 12, 4, 3, 5)
SPIKE = box(20, 12, 2, 2, 4)
TIP = box(34, 12, 1, 1, 2)
LEG = box(0, 22, 7, 1, 1)

LIGHT = {"top": 1, "bottom": -1, "front": 0, "back": -1, "right": 0, "left": -1}


def shell(cv, part, seed, tones=SHELL, base=2, bands=0):
    """Dull mineral shell, lit from above, mottled in soft clusters, with darker plate seams every `bands` pixels."""
    for name, (x0, y0, w, h) in faces(part):
        for y in range(h):
            for x in range(w):
                t = base + LIGHT.get(name, 0) + (1 if smooth(x0 + x, y0 + y, seed, 2.2) > 0.68 else 0)
                n = noise(x0 + x, y0 + y, seed)
                if n > 0.9:
                    t -= 1
                elif n < 0.08:
                    t += 1
                # Plate seams run across the shell, along its length.
                if bands and name in ("top", "right", "left", "bottom"):
                    along = y if name in ("top", "bottom") else x
                    if along % bands == bands - 1:
                        t -= 1
                cv.put(x0 + x, y0 + y, tones[max(0, min(4, t))])


def drip(cv, part, seed):
    """Wet streaks running down a dripstone face: paler lines with a darker edge."""
    for name, (x0, y0, w, h) in faces(part, "top", "right", "left", "bottom"):
        for x in range(w):
            if noise(x0 + x, y0, seed) > 0.62:
                for y in range(h):
                    cv.put(x0 + x, y0 + y, STONE[4] if y % 3 else STONE[3])


def skin():
    cv = Sheet(64, 32)
    shell(cv, BODY, 10, bands=3)
    # The belly is pale and soft, segmented.
    x0, y0, w, h = BODY["bottom"]
    for y in range(h):
        for x in range(w):
            t = 2 + (1 if smooth(x0 + x, y0 + y, 12, 2) > 0.6 else 0) - (1 if y % 2 == 1 else 0)
            cv.put(x0 + x, y0 + y, BELLY[t])
    shell(cv, RIDGE, 14, tones=STONE, base=3)

    shell(cv, HEAD, 20)
    # The face: dark mouth below, four small dull eyes in a row across the brow.
    x0, y0, w, h = HEAD["front"]
    for x in range(w):
        cv.put(x0 + x, y0 + h - 1, DARK[1])
    for x in range(w):
        cv.put(x0 + x, y0, EYE if x in (0, 3) else (GLINT if x == 1 else EYE))
    for name in ("right", "left"):
        sx, sy, sw, sh = HEAD[name]
        cv.put(sx + (0 if name == "right" else sw - 1), sy, EYE)
    # Hooked pale fangs, darker at the tips.
    shell(cv, FANG_BOX, 30, tones=FANG, base=2)
    for name, (x0, y0, w, h) in faces(FANG_BOX, "front", "back", "right", "left"):
        cv.put(x0, y0 + h - 1, DARK[3])

    # The abdomen, then the spike and its tip: shell giving way to plain wet dripstone.
    shell(cv, ABDOMEN, 40, bands=2)
    x0, y0, w, h = ABDOMEN["bottom"]
    for y in range(h):
        for x in range(w):
            cv.put(x0 + x, y0 + y, BELLY[1 + (y % 2)])
    shell(cv, SPIKE, 50, tones=STONE, base=2)
    drip(cv, SPIKE, 52)
    shell(cv, TIP, 54, tones=STONE, base=3)

    # Thin jointed legs, darker at the knee and the foot.
    shell(cv, LEG, 60, tones=SHELL, base=1)
    for name, (x0, y0, w, h) in faces(LEG, "top", "bottom", "front", "back"):
        if w >= 7:
            cv.put(x0 + 3, y0, DARK[3])
            cv.put(x0 + w - 1, y0, DARK[2])
    return cv.image()


def _count(lo, hi):
    return {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}


def _looting(hi):
    return {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting',
            'count': {'type': 'minecraft:uniform', 'min': 0, 'max': hi}}


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/dripfang_lurker.png')
    name = 'dripfang_lurker_spawn_egg'
    g.save(egg_icon(("#65564A", "#82725F", "#9E8E78"), "#D2C8B6", name="dripfang_lurker"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/dripfang_lurker.json', {'type': 'minecraft:entity', 'pools': [
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:pointed_dripstone', 'functions': [_count(0, 1), _looting(1)]}]},
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:spider_eye', 'functions': [_count(0, 1)]}]}],
        'random_sequence': 'wildercord:entities/dripfang_lurker'})
