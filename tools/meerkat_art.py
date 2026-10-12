"""Sunwatch meerkat: its coat (SunwatchMeerkatModel's 64x64 box-UV layout), spawn egg, loot, spawn ground and field-guide text."""
from bobcat_art import egg_icon, fill
from item_art import hexc, mix, ramp
from monster_art import faces, fur
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.sunwatch_meerkat': 'Sunwatch Meerkat',
 'item.wildercord.sunwatch_meerkat_spawn_egg': 'Sunwatch Meerkat Spawn Egg',
 'guide.wildercord.sunwatch_meerkat.hint': 'Out in the desert and savanna, one of them is always watching. Come crouched, or not at all.',
 'guide.wildercord.sunwatch_meerkat': 'A slim little sandy digger of the deserts and savannas, banded across the back and dark round the '
                                      'eyes, that lives in a mob. One at a time goes up to the highest spot near and stands sentry, tall '
                                      'on its hind legs and leaning on its tail, its head turning this way and that, while the rest dig '
                                      'with their noses down and sit up with their bellies to the morning sun; at night they all sleep in '
                                      'a heap. Walk up in plain sight, or let a monster come near, and the sentry barks: the whole mob '
                                      'dives down their holes, where nothing can touch them, and a while later their heads come up to '
                                      'peek. Still there, and down they go again. Come crouched and they do not see you until you are '
                                      'right there. Spider eyes or eggs tempt it and breed it; feed one and it trusts you a while, '
                                      'trotting along after you.',
}

COAT = ramp("#7A5E3C", "#987A52", "#B49668", "#C8AC80", "#D8C096")
BAND = ramp("#4A3420", "#5C4428", "#6E5434", "#806440", "#90744C")
PALE = ramp("#B8A486", "#CCBA9C", "#DCCCB0", "#E8DCC4", "#F2E8D4")
DARK = ramp("#1A140E", "#261E16", "#32281E", "#3E3226", "#4A3C2E")
EYE, SHINE = hexc("#0A0806"), hexc("#8A7A66")
NOSE_C, NOSE_SHINE = hexc("#1A1410"), hexc("#5A4C40")

BODY = box(0, 0, 4, 4, 8)
HEAD = box(28, 0, 3, 3, 3)
SNOUT = box(28, 8, 2, 1, 2)
NOSE = box(40, 0, 1, 1, 1)
EAR = box(40, 4, 1, 1, 1)
FRONT = box(48, 0, 1, 4, 1)
HIND = box(48, 8, 2, 4, 2)
TAIL = box(0, 16, 1, 1, 6)
TIP = box(16, 16, 1, 1, 4)


def bands(cv, area, seed, across_y):
    """Faint dark bands across the back: every few pixels along the body a ragged darker stripe."""
    x0, y0, w, h = area
    for y in range(h):
        for x in range(w):
            along = y if across_y else x
            if along % 3 == 1 and noise(x0 + x, y0 + y, seed) > 0.3:
                cv.put(x0 + x, y0 + y, BAND[2 + (1 if noise(x0 + x, y0 + y, seed + 1) > 0.6 else 0)])


def belly(cv, area, seed, rows):
    """The lower `rows` rows of a side face going pale, in a soft uneven line."""
    x0, y0, w, h = area
    for x in range(w):
        edge = rows - (1 if noise(x0 + x, y0, seed) > 0.6 else 0)
        for y in range(h - edge, h):
            cv.put(x0 + x, y0 + y, PALE[2 if y > h - edge else 1])


def skin():
    cv = Sheet(64, 64)
    # The body: sandy over the back and sides, banded across the back, pale beneath.
    for name, area in faces(BODY):
        fur(cv, area, PALE if name == "bottom" else COAT, 10 + len(name), name, base=2, along_y=name in ("top", "bottom"))
    bands(cv, BODY["top"], 13, True)
    for name in ("right", "left"):
        bands(cv, BODY[name], 15 + len(name), False)
        belly(cv, BODY[name], 7 + len(name), 1)
    # The chest (the front face, seen when it stands up) pale.
    fur(cv, BODY["front"], PALE, 19, "front", base=2)
    # The head: sandy, pale round the muzzle, the eyes set in dark patches.
    for name, area in faces(HEAD):
        fur(cv, area, PALE if name == "bottom" else COAT, 20 + len(name), name, base=3)
    x0, y0, w, h = HEAD["front"]
    face = [
        "ccc",
        "dpd",
        "ppp",
    ]
    for y, row in enumerate(face):
        for x, ch in enumerate(row):
            if ch == "d":
                cv.put(x0 + x, y0 + y, EYE)
            elif ch == "p":
                cv.put(x0 + x, y0 + y, PALE[3])
    for name in ("right", "left"):
        x0, y0, w, h = HEAD[name]
        # The dark eye patch wraps round onto the side, and the cheek is pale.
        front = 0 if name == "right" else w - 1
        cv.put(x0 + (w - 1 - front), y0 + 1, DARK[2])
        cv.put(x0 + (w - 1 - front), y0 + 2, PALE[2])
        cv.put(x0 + 1, y0 + 2, PALE[2])
    # The muzzle pale, darkening to the nose.
    for name, area in faces(SNOUT):
        fur(cv, area, PALE, 30 + len(name), name, base=2)
    x0, y0, w, h = SNOUT["top"]
    for x in range(w):
        cv.put(x0 + x, y0 + h - 1, mix(PALE[2], DARK[3], 0.3))
    for name, area in faces(NOSE):
        fill(cv, area, NOSE_C)
    x0, y0, w, h = NOSE["front"]
    cv.put(x0, y0, NOSE_SHINE)
    # Ears: small and dark.
    for name, area in faces(EAR):
        fill(cv, area, DARK[3] if name != "front" else DARK[1])
    # Legs: sandy, darkening to the paws.
    for part, seed in ((FRONT, 50), (HIND, 54)):
        for name, area in faces(part):
            fur(cv, area, COAT, seed + len(name), name, base=2)
            x0, y0, w, h = area
            if name not in ("top", "bottom"):
                for x in range(w):
                    cv.put(x0 + x, y0 + h - 1, DARK[3])
        fill(cv, part["bottom"], DARK[2])
    # The long thin tail: sandy, its tip black.
    for name, area in faces(TAIL):
        fur(cv, area, COAT, 60 + len(name), name, base=2)
    for name, area in faces(TIP):
        fur(cv, area, COAT, 66 + len(name), name, base=2)
        x0, y0, w, h = area
        if name in ("right", "left"):
            for y in range(h):
                for x in range(w):
                    z = x if name == "left" else w - 1 - x
                    if z >= 2:
                        cv.put(x0 + x, y0 + y, DARK[2 if z == 3 else 3])
        elif name in ("top", "bottom"):
            for y in range(2, h):
                for x in range(w):
                    cv.put(x0 + x, y0 + y, DARK[2])
    fill(cv, TIP["back"], DARK[1])
    return cv.image()


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/wildlife/sunwatch_meerkat.png')
    name = 'sunwatch_meerkat_spawn_egg'
    g.save(egg_icon(("#987A52", "#B49668", "#D8C096"), "#32281E", name="sunwatch_meerkat"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/sunwatch_meerkat.json', {'type': 'minecraft:entity', 'pools': [], 'random_sequence': 'wildercord:entities/sunwatch_meerkat'})
    g.write_json(g.DATA / 'tags/block/spawns_on/sunwatch_meerkat.json', {'replace': False, 'values': [
        '#minecraft:animals_spawnable_on', 'minecraft:sand', 'minecraft:red_sand', 'minecraft:grass_block', 'minecraft:coarse_dirt',
        'minecraft:dirt', 'minecraft:sandstone']})
