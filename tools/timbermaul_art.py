"""Timbermaul Bear: its skin (TimbermaulBearModel's 128x64 box-UV layout), spawn egg, loot and field-guide text."""
from bobcat_art import egg_icon
from item_art import hexc, ramp
from monster_art import faces, fur, moss, shade, smooth
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.timbermaul_bear': 'Timbermaul Bear',
 'item.wildercord.timbermaul_bear_spawn_egg': 'Timbermaul Bear Spawn Egg',
 'guide.wildercord.timbermaul_bear.hint': 'Moss on its back and old claw scars down its flanks. It stood up when it saw me, and huffed.',
 'guide.wildercord.timbermaul_bear': 'A great brown bear of the taigas, one of the champions of the wilds, half again a bear\'s '
                                     'size, with moss grown thick on its back and a ridge of dark hackles down its spine. '
                                     'Come near and it stands up and huffs; back off and it lets you be. Fight it and it '
                                     'never makes the same move twice running. It draws a paw up and out to one side and '
                                     'rakes it across its front: back off, or get behind it. It draws its head back, jaws '
                                     'wide, and snaps down a short lane before it: step aside. It rears up tall and crushes '
                                     'down on a fan before it: get out of the fan, or strike it hard while it is up and '
                                     'topple it onto its back, where your blows land half again as hard. It crouches with '
                                     'its rump wiggling and pounces on a marked spot: leave the spot, and it lies sprawled '
                                     'where it lands. It lowers its head and paws the ground, then charges along a line: '
                                     'step aside, and if it runs into a tree or a wall it stands dazed. It tears a '
                                     'mossy log out of the forest floor and sends it rolling down a wide lane: jump it. '
                                     'Stand behind it and it digs and kicks a spray of earth and stones back at you: get '
                                     'out from behind it. Half beaten it rears and roars, shoving all about it back, and '
                                     'fights on roused, quicker, its hackles up and its moves in pairs; and now it rears '
                                     'with its forelegs spread wide and falls on whoever stands before it, pinning them and '
                                     'mauling. Strike it hard to break free, or back away before it falls.',
}

FUR = ramp("#24160D", "#382416", "#4E3320", "#68472C", "#84603C")
GRIZZLE = ramp("#3E2A1A", "#5C4028", "#7E5C3A", "#A07C52", "#C2A070")
HACKLE = ramp("#110A06", "#1C120C", "#291B12", "#38261A", "#4A3424")
MOSS = ramp("#2A3E16", "#3C541E", "#516E26", "#6A8A30", "#86A642")
MUZZLE = ramp("#5E4630", "#7A5E42", "#987A58", "#B49670", "#CCB28A")
MAW = ramp("#2A1010", "#4A1C1A", "#6E2C26", "#94403A", "#B85A50")
CLAW = ramp("#16120E", "#26201A", "#3E362C", "#5E5446", "#827666")
TOOTH = hexc("#EAE2CC")
EYE = hexc("#E0A030")
SCAR = hexc("#8C6A54")

BODY = box(0, 0, 12, 11, 20)
HUMP = box(64, 0, 10, 4, 7)
HACKLES = box(100, 0, 3, 2, 10)
MOSS_MAT = box(64, 12, 9, 1, 12)
HEAD = box(0, 32, 8, 7, 6)
SNOUT = box(28, 32, 4, 3, 4)
JAW = box(44, 32, 4, 1, 4)
EAR = box(60, 32, 2, 2, 1)
TAIL = box(68, 32, 3, 3, 2)
FORELEG = box(0, 46, 4, 10, 4)
HIND_LEG = box(16, 46, 4, 10, 5)


def coat(cv, part, seed, tones=FUR, base=2, names=(), along_y=False):
    """Its coat: short fur stroked one way, lit from above."""
    for name, area in faces(part, *names):
        fur(cv, area, tones, seed, name, base=base, along_y=along_y)


def grizzled(cv, part, seed, names=()):
    """Pale tips scattered through the fur, thicker on the faces that catch the light."""
    for name, (x0, y0, w, h) in faces(part, *names):
        for y in range(h):
            for x in range(w):
                n = noise(x0 + x, y0 + y, seed)
                if n > (0.72 if name == "top" else 0.82):
                    cv.put(x0 + x, y0 + y, GRIZZLE[3 if n > 0.9 else 2])


def scars(cv, area, seed, flip=False):
    """Three old claw marks raked down a flank at a slant, pale and hairless."""
    x0, y0, w, h = area
    start = 6 + int(noise(x0, y0, seed) * 4)
    for k in range(3):
        for i in range(6):
            x = start + k * 2 + (i if not flip else -i) // 2
            y = 2 + i
            if 0 <= x < w and 0 <= y < h - 1:
                cv.put(x0 + x, y0 + y, SCAR)


def claws(cv, part):
    """A dark pad underneath, and a row of claws along the bottom of the front face."""
    for name, (x0, y0, w, h) in faces(part, "bottom"):
        for y in range(h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, CLAW[1 + (noise(x0 + x, y0 + y, 81) > 0.6)])
    x0, y0, w, h = part["front"]
    for x in range(w):
        cv.put(x0 + x, y0 + h - 1, CLAW[3] if x % 2 == 0 else CLAW[1])
        if x % 2 == 0:
            cv.put(x0 + x, y0 + h - 2, CLAW[2])
    for name in ("right", "left", "back"):
        x0, y0, w, h = part[name]
        for x in range(w):
            cv.put(x0 + x, y0 + h - 1, FUR[0])


def skin():
    cv = Sheet(128, 64)

    # The body: dark fur along its length, a darker stripe down the spine and a darker belly, a paler chest, and old
    # claw scars down either flank.
    coat(cv, BODY, 10, along_y=True, names=("top", "bottom"))
    coat(cv, BODY, 11, names=("front", "back", "right", "left"))
    x0, y0, w, h = BODY["top"]
    for y in range(h):
        for x in range(w // 2 - 2, w // 2 + 2):
            cv.put(x0 + x, y0 + y, shade(FUR, 1 + (smooth(x0 + x, y0 + y, 12, 2.0) > 0.7)))
    grizzled(cv, BODY, 13, names=("top", "right", "left"))
    x0, y0, w, h = BODY["front"]
    for y in range(3, h):
        for x in range(2, w - 2):
            if smooth(x0 + x, y0 + y, 14, 2.5) > 0.35:
                cv.put(x0 + x, y0 + y, shade(FUR, 3))
    scars(cv, BODY["right"], 15)
    scars(cv, BODY["left"], 16, flip=True)

    # The hump over its shoulders, grizzled; the hackles, near black and stiff along its spine.
    coat(cv, HUMP, 20, base=3)
    grizzled(cv, HUMP, 21)
    coat(cv, HACKLES, 22, tones=HACKLE, along_y=True)
    for name, (x0, y0, w, h) in faces(HACKLES, "top"):
        for y in range(h):
            cv.put(x0 + 1, y0 + y, HACKLE[3 if y % 3 else 4])

    # The moss: cushions of it over brown, a few bare patches where the bark of its back shows through.
    coat(cv, MOSS_MAT, 30, base=1)
    for name, area in faces(MOSS_MAT):
        moss(cv, area, MOSS, 31, name, cover=0.9 if name == "top" else 0.7)

    # The head: fur, darker over the brow; small dark eyes set above the snout with a gleam of amber in each.
    coat(cv, HEAD, 40)
    grizzled(cv, HEAD, 41, names=("top",))
    x0, y0, w, h = HEAD["front"]
    for x in range(w):
        cv.put(x0 + x, y0 + 1, shade(FUR, 1))
    for x in (1, w - 2):
        cv.put(x0 + x, y0 + 2, EYE)
        cv.put(x0 + x + (1 if x == 1 else -1), y0 + 2, FUR[0])

    # The snout: pale and short-furred, a dark nose at its tip, the roof of the mouth raw red underneath with a row of
    # teeth along its front edge.
    coat(cv, SNOUT, 50, tones=MUZZLE)
    x0, y0, w, h = SNOUT["front"]
    for x in range(1, w - 1):
        cv.put(x0 + x, y0, CLAW[0])
        cv.put(x0 + x, y0 + 1, CLAW[1] if x == 1 else CLAW[0])
    x0, y0, w, h = SNOUT["bottom"]
    for y in range(h):
        for x in range(w):
            cv.put(x0 + x, y0 + y, MAW[2 + (noise(x0 + x, y0 + y, 51) > 0.6)])
    x0, y0, w, h = SNOUT["front"]
    for x in range(w):
        cv.put(x0 + x, y0 + h - 1, TOOTH if x % 3 != 1 else MAW[1])

    # The jaw: pale beneath, a red tongue inside, teeth along its front.
    coat(cv, JAW, 55, tones=MUZZLE, base=1)
    x0, y0, w, h = JAW["top"]
    for y in range(h):
        for x in range(w):
            cv.put(x0 + x, y0 + y, MAW[3 if 0 < x < w - 1 and y > 0 else 1])
    x0, y0, w, h = JAW["front"]
    for x in range(w):
        cv.put(x0 + x, y0, TOOTH if x % 3 != 1 else MAW[1])

    # Ears and tail: dark fur, the ears paler inside.
    coat(cv, EAR, 60, base=1)
    x0, y0, w, h = EAR["front"]
    for y in range(h):
        for x in range(w):
            cv.put(x0 + x, y0 + y, MUZZLE[1])
    coat(cv, TAIL, 65)

    # The legs: fur stroked down them, darker toward the paws, which are black-padded and clawed.
    for part, seed in ((FORELEG, 70), (HIND_LEG, 75)):
        coat(cv, part, seed, along_y=True)
        for name, (x0, y0, w, h) in faces(part, "front", "back", "right", "left"):
            for y in range(h - 4, h):
                for x in range(w):
                    cv.put(x0 + x, y0 + y, shade(FUR, 1 + (smooth(x0 + x, y0 + y, seed + 1, 2.0) > 0.6)))
        claws(cv, part)
    return cv.image()


def _count(lo, hi):
    return {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}


def _looting(hi):
    return {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting',
            'count': {'type': 'minecraft:uniform', 'min': 0, 'max': hi}}


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/timbermaul_bear.png')
    name = 'timbermaul_bear_spawn_egg'
    g.save(egg_icon(("#382416", "#4E3320", "#68472C"), "#516E26", name="timbermaul_bear"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/timbermaul_bear.json', {'type': 'minecraft:entity', 'pools': [
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:leather', 'functions': [_count(4, 8), _looting(3)]}]},
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:bone', 'functions': [_count(2, 4), _looting(2)]}]},
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:moss_block', 'functions': [_count(1, 3)]}]},
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:emerald', 'functions': [_count(2, 4), _looting(1)]}]},
        {'rolls': 1, 'conditions': [{'condition': 'minecraft:random_chance', 'chance': 0.5}],
         'entries': [{'type': 'minecraft:item', 'name': 'minecraft:diamond', 'functions': [_count(1, 2)]}]}],
        'random_sequence': 'wildercord:entities/timbermaul_bear'})
