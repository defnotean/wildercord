"""Cinnabar panda: its coat (CinnabarPandaModel's 64x64 box-UV layout), spawn egg, loot, spawn ground and field-guide text."""
from bobcat_art import egg_icon, fill
from item_art import hexc, mix, ramp
from monster_art import faces, fur
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.cinnabar_panda': 'Cinnabar Panda',
 'item.wildercord.cinnabar_panda_spawn_egg': 'Cinnabar Panda Spawn Egg',
 'guide.wildercord.cinnabar_panda.hint': 'Look through the cherry groves, bamboo and old-growth taiga for something red curled up asleep at noon.',
 'guide.wildercord.cinnabar_panda': 'A small red panda, rust-red above and black below, with a white mask, white-rimmed ears and a great '
                                    'ringed tail. Come near and it rears up on its hind legs with its forepaws spread wide, to look as big '
                                    'as it can. It washes its face with its paws, sniffs out ripe sweet berry bushes and sits up to eat '
                                    'them, and through the bright middle of the day it curls up and naps with its tail over its face. Two '
                                    'together now and then tumble over and over; cubs do it on their own. Sweet berries or bamboo tempt '
                                    'it and breed it; feed one and it trusts you a while, trotting along after you.',
}

COAT = ramp("#6E2810", "#8A3416", "#A4421E", "#BA5226", "#CE6430")
RING = ramp("#A4602E", "#BC7436", "#D08A44", "#DEA058", "#E8B46C")
BLACK = ramp("#120E0D", "#1C1614", "#28201C", "#342A24", "#40342C")
WHITE = ramp("#CEC6BA", "#DED6CA", "#ECE6DA", "#F6F2EA", "#FCFAF4")
TEAR = hexc("#5A1E0C")
EYE, SHINE = hexc("#0A0706"), hexc("#7A6A62")
NOSE_C, NOSE_SHINE = hexc("#141010"), hexc("#4E4440")

BODY = box(0, 0, 6, 5, 9)
HEAD = box(32, 0, 6, 5, 4)
SNOUT = box(32, 10, 3, 2, 2)
NOSE = box(44, 10, 1, 1, 1)
EAR = box(52, 0, 2, 2, 1)
LEG = box(52, 4, 2, 5, 2)
TAIL = box(0, 16, 3, 3, 6)
TIP = box(20, 16, 3, 3, 5)


def underside(cv, area, tones, seed, rows):
    """The lower `rows` rows of a side face, another colour coming up into it in a soft, uneven line."""
    x0, y0, w, h = area
    for x in range(w):
        edge = rows - (1 if noise(x0 + x, y0, seed) > 0.6 else 0)
        for y in range(h - edge, h):
            cv.put(x0 + x, y0 + y, tones[2 if y > h - edge else 3])


def rings(cv, part, start, seed):
    """A tail section in rings, rust and pale ochre by turns along its length (`start` is how far along the tail it begins)."""
    for name, area in faces(part):
        x0, y0, w, h = area
        for y in range(h):
            for x in range(w):
                if name in ("right", "left"):
                    z = start + (x if name == "left" else w - 1 - x)
                elif name in ("top", "bottom"):
                    z = start + y
                else:
                    z = start + (0 if name == "front" else 99)
                if z >= 99:
                    tones = BLACK if start > 0 else COAT
                else:
                    tones = RING if (z // 2) % 2 == 1 else COAT
                    if start > 0 and z >= start + 3:
                        tones = COAT if (z // 2) % 2 == 0 else RING
                t = 2 + (1 if noise(x0 + x, y0 + y, seed) > 0.7 else 0) - (1 if name == "bottom" else 0)
                cv.put(x0 + x, y0 + y, tones[t])
    # The very end of the tail darkens.
    if start > 0:
        for name in ("right", "left", "top", "bottom"):
            x0, y0, w, h = part[name]
            for y in range(h):
                for x in range(w):
                    z = (x if name == "left" else w - 1 - x) if name in ("right", "left") else y
                    if z == 4:
                        cv.put(x0 + x, y0 + y, mix(COAT[0], BLACK[2], 0.5))


def skin():
    cv = Sheet(64, 64)
    # The body: rust-red over the back and sides, black beneath and on the chest, short fur stroked toward the tail.
    for name, area in faces(BODY):
        fur(cv, area, BLACK if name == "bottom" else COAT, 10 + len(name), name, base=2, along_y=name in ("top", "bottom"))
    for name in ("right", "left"):
        underside(cv, BODY[name], BLACK, 7 + len(name), 2)
    fur(cv, BODY["front"], BLACK, 19, "front", base=2)
    x0, y0, w, h = BODY["front"]
    for x in range(w):
        cv.put(x0 + x, y0, COAT[2])
    # The head: rust over the crown, black at the throat, the white mask on the face.
    for name, area in faces(HEAD):
        fur(cv, area, BLACK if name == "bottom" else COAT, 20 + len(name), name, base=3)
    for name in ("right", "left"):
        underside(cv, HEAD[name], WHITE, 21 + len(name), 2)
        x0, y0, w, h = HEAD[name]
        cv.put(x0 + (w - 1 if name == "right" else 0), y0 + 2, EYE)
    x0, y0, w, h = HEAD["front"]
    face = [
        "cccccc",
        "cwccwc",
        "ceccec",
        "wtccTw",
        "wtwwtw",
    ]
    for y, row in enumerate(face):
        for x, ch in enumerate(row):
            if ch == "w":
                cv.put(x0 + x, y0 + y, WHITE[3 if y < 3 else 2])
            elif ch == "e":
                cv.put(x0 + x, y0 + y, EYE)
            elif ch in "tT":
                cv.put(x0 + x, y0 + y, TEAR)
    cv.put(x0 + 1, y0 + 2, EYE)
    cv.put(x0 + 4, y0 + 2, EYE)
    cv.put(x0 + 1, y0 + 1, WHITE[4])
    # The muzzle all white, the nose black with a gleam.
    for name, area in faces(SNOUT):
        fur(cv, area, WHITE, 30 + len(name), name, base=2)
    for name, area in faces(NOSE):
        fill(cv, area, NOSE_C)
    x0, y0, w, h = NOSE["front"]
    cv.put(x0, y0, NOSE_SHINE)
    # Ears: white-rimmed, dark inside.
    for name, area in faces(EAR):
        fur(cv, area, WHITE if name != "back" else COAT, 40 + len(name), name, base=3)
    x0, y0, w, h = EAR["front"]
    cv.put(x0, y0 + 1, BLACK[2])
    cv.put(x0 + 1, y0 + 1, BLACK[3])
    # Legs black right down to the paws.
    for name, area in faces(LEG):
        fur(cv, area, BLACK, 50 + len(name), name, base=2)
    for name in ("front", "right", "left", "back"):
        x0, y0, w, h = LEG[name]
        cv.put(x0, y0, mix(COAT[1], BLACK[2], 0.5))
    # The great ringed tail.
    rings(cv, TAIL, 0, 61)
    rings(cv, TIP, 6, 67)
    return cv.image()


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/wildlife/cinnabar_panda.png')
    name = 'cinnabar_panda_spawn_egg'
    g.save(egg_icon(("#8A3416", "#BA5226", "#CE6430"), "#F2ECE2", name="cinnabar_panda"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/cinnabar_panda.json', {'type': 'minecraft:entity', 'pools': [], 'random_sequence': 'wildercord:entities/cinnabar_panda'})
    g.write_json(g.DATA / 'tags/block/spawns_on/cinnabar_panda.json', {'replace': False, 'values': [
        '#minecraft:animals_spawnable_on', 'minecraft:grass_block', 'minecraft:podzol', 'minecraft:coarse_dirt', 'minecraft:moss_block',
        'minecraft:dirt', 'minecraft:rooted_dirt']})
