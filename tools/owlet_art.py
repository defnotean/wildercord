"""Hushwing owlet: its skin (HushwingOwletModel's 64x32 box-UV layout), spawn egg, loot, spawn ground and field-guide text."""
from bobcat_art import egg_icon
from item_art import hexc, ramp
from monster_art import faces, smooth
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.hushwing_owlet': 'Hushwing Owlet',
 'item.wildercord.hushwing_owlet_spawn_egg': 'Hushwing Owlet Spawn Egg',
 'guide.wildercord.hushwing_owlet.hint': 'In the pale garden, a little grey puff of a bird that will not stop looking at the trees.',
 'guide.wildercord.hushwing_owlet': 'A round pale-grey fledgling owl of the pale garden, all fluff, with two little ear tufts, a '
                                    'pale face and great round amber eyes. It can\'t fly yet, only waddle and flutter, and it '
                                    'falls soft from any height. By day it sleeps where it stands, fluffed up with its eyes '
                                    'shut; by night it watches, turning its head right round over its back to follow you. '
                                    'The creakings hate it: whenever one stirs within its sight it fixes it with its great eyes '
                                    'and hoots, and a creaking it stares at can\'t move, just as when you look at one yourself. '
                                    'Raw rabbit, chicken and cod tempt it and breed it. Feed one and it trusts you a while, '
                                    'waddles along after you, and at night hoots out whatever is stalking you, so that it shows '
                                    'through the dark a few seconds.',
}

DOWN = ramp("#6E6A64", "#8C8780", "#A8A39A", "#C4BFB4", "#DEDAD0")
DISC = ramp("#B8B0A0", "#CCC4B4", "#DED8CA", "#ECE8DC", "#F8F6EE")
DARK = ramp("#3A3632", "#4C4842", "#5E5952", "#726C64", "#88827A")
EYE, RING, PUPIL, GLINT = hexc("#E8A630"), hexc("#B87418"), hexc("#140E0A"), hexc("#FFF6E0")
BEAK = ramp("#4A443C", "#5E574C", "#7A7264", "#958C7C", "#B0A894")
FOOT = ramp("#6A6050", "#807460", "#9A8C74", "#B0A288", "#C6B89C")

BODY = box(0, 0, 6, 5, 5)
HEAD = box(24, 0, 6, 5, 5)
TUFT = box(48, 0, 1, 2, 1)
BEAK_BOX = box(48, 4, 1, 1, 1)
LIDS = box(48, 8, 6, 2, 1)
WING = box(0, 12, 1, 4, 4)
LEG = box(12, 12, 1, 2, 1)
TAIL = box(20, 12, 3, 1, 2)

LIGHT = {"top": 1, "bottom": -1, "front": 0, "back": -1, "right": 0, "left": -1}


def shade(cv, area, tones, seed, face, cell=1.6, base=2, flecks=0.86):
    """Soft down, lit from above, mottled, with dark flecks here and there."""
    x0, y0, w, h = area
    for y in range(h):
        for x in range(w):
            t = base + LIGHT.get(face, 0) - (1 if smooth(x0 + x, y0 + y, seed, cell) < 0.3 else 0)
            c = tones[max(0, min(4, t))]
            if noise(x0 + x, y0 + y, seed) > flecks:
                c = DARK[1 + (1 if noise(x0 + x, y0 + y, seed + 1) > 0.5 else 0)]
            cv.put(x0 + x, y0 + y, c)


def feathered(cv, part, seed):
    for name, area in faces(part):
        shade(cv, area, DOWN, seed + len(name), name)


def skin():
    cv = Sheet(64, 32)
    feathered(cv, BODY, 10)
    # A paler breast, barred.
    x0, y0, w, h = BODY["front"]
    for y in range(h):
        for x in range(1, w - 1):
            cv.put(x0 + x, y0 + y, DOWN[2] if (y + (x // 2)) % 3 == 0 else DISC[2])
    feathered(cv, HEAD, 20)
    # The face disc: pale, rimmed dark, with two great amber eyes and the beak between them.
    x0, y0, w, h = HEAD["front"]
    for y in range(h):
        for x in range(w):
            edge = x in (0, w - 1) or y == h - 1
            cv.put(x0 + x, y0 + y, DARK[3] if edge else DISC[3 if (x + y) % 2 else 2])
    # Two great eyes wide apart either side of the beak: amber, the pupil to the inside, ringed darker below.
    for ex, inner in ((0, 1), (4, 4)):
        for dx in (0, 1):
            cv.put(x0 + ex + dx, y0 + 1, EYE)
            cv.put(x0 + ex + dx, y0 + 2, RING if dx + ex != inner else EYE)
        cv.put(x0 + inner, y0 + 1, PUPIL)
    # The brow, a little V of dark between the eyes, and a dark ridge down to the beak.
    cv.put(x0 + 1, y0, DARK[3])
    cv.put(x0 + 4, y0, DARK[3])
    cv.put(x0 + 2, y0 + 1, DARK[3])
    cv.put(x0 + 3, y0 + 1, DARK[3])
    for name, area in faces(TUFT):
        shade(cv, area, DOWN, 30 + len(name), name, base=1, flecks=0.7)
    for name, area in faces(BEAK_BOX):
        shade(cv, area, BEAK, 40 + len(name), name, flecks=2)
    # The lids: soft grey down, a dark seam where they meet.
    for name, area in faces(LIDS):
        shade(cv, area, DOWN, 50 + len(name), name, base=3, flecks=2)
    x0, y0, w, h = LIDS["front"]
    for x in range(w):
        cv.put(x0 + x, y0 + 1, DARK[2] if x not in (2, 3) else DARK[3])
    # The wings: darker, barred with pale.
    for name, area in faces(WING):
        x0, y0, w, h = area
        for y in range(h):
            for x in range(w):
                t = 1 + LIGHT.get(name, 0)
                c = DOWN[max(0, min(4, t))] if y % 2 else DOWN[3]
                if noise(x0 + x, y0 + y, 60) > 0.85:
                    c = DARK[2]
                cv.put(x0 + x, y0 + y, c)
    for name, area in faces(LEG):
        shade(cv, area, FOOT, 70 + len(name), name, flecks=2)
    for name, area in faces(TAIL):
        shade(cv, area, DOWN, 80 + len(name), name, base=1, flecks=0.75)
    return cv.image()


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/wildlife/hushwing_owlet.png')
    name = 'hushwing_owlet_spawn_egg'
    g.save(egg_icon(("#8C8780", "#A8A39A", "#DEDAD0"), "#E8A630", name="hushwing_owlet"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/hushwing_owlet.json', {'type': 'minecraft:entity', 'pools': [
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:feather', 'functions': [
            {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 0, 'max': 2}},
            {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting',
             'count': {'type': 'minecraft:uniform', 'min': 0, 'max': 1}}]}]}],
        'random_sequence': 'wildercord:entities/hushwing_owlet'})
    g.write_json(g.DATA / 'tags/block/spawns_on/hushwing_owlet.json', {'replace': False, 'values': [
        'minecraft:pale_moss_block', 'minecraft:grass_block', 'minecraft:dirt', 'minecraft:coarse_dirt', 'minecraft:podzol',
        'minecraft:moss_block']})
