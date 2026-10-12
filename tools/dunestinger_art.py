"""Dunestinger: its carapace (DunestingerModel's 64x64 box-UV layout), the glint of its eyes and barb, spawn egg, loot and field-guide text."""
from bobcat_art import egg_icon
from item_art import hexc, ramp
from monster_art import faces, smooth
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.dunestinger': 'Dunestinger',
 'item.wildercord.dunestinger_spawn_egg': 'Dunestinger Spawn Egg',
 'guide.wildercord.dunestinger.hint': 'Out on the sand, two little beads of eyes and the tip of something curved, just showing.',
 'guide.wildercord.dunestinger': 'A sand-coloured scorpion of the deserts and badlands, broad and low, with two heavy pincers and '
                                 'a tail of four segments curled up over its back to a bulbed sting. Several lie buried together '
                                 'in the sand with only their eyes and the tip of the sting showing, and keep perfectly still. Come '
                                 'near and they burst out of the sand all at once; run at them and they feel it further off, creep '
                                 'up crouched and you can get much closer. Up close it snaps its pincers, and a little further off '
                                 'it draws its sting back over its head, the barb glowing, and drives it down: that is the blow to '
                                 'dodge, for it poisons. Hurt badly on sand, it sinks under, and nothing can touch it there, and '
                                 'a moment later it comes up again right beside you. Left alone a while on sand, it buries itself '
                                 'again.',
}

SHELL = ramp("#7A5A2E", "#9C7840", "#BC9656", "#D4B070", "#E8CC90")
BAND = ramp("#4E3820", "#644A2A", "#7C5E36", "#947244", "#AA8654")
BELLY = ramp("#A08A62", "#B8A276", "#CEB88C", "#DECAA0", "#ECDAB4")
STING = ramp("#1E140E", "#2E2016", "#40301E", "#544028", "#6A5234")
EYE = hexc("#0A0806")
GLINT, BARB_HOT = hexc("#F0E0B0"), hexc("#FF8A30")

CARAPACE = box(0, 0, 8, 4, 9)
HEAD = box(36, 0, 6, 3, 3)
EYE_BOX = box(56, 0, 1, 2, 1)
TAIL1 = box(0, 14, 4, 3, 4)
TAIL2 = box(16, 14, 3, 3, 4)
TAIL3 = box(30, 14, 3, 3, 4)
TAIL4 = box(44, 14, 2, 2, 4)
BULB = box(0, 22, 2, 2, 3)
BARB = box(10, 22, 1, 1, 2)
ARM = box(0, 28, 2, 2, 4)
FOREARM = box(12, 28, 2, 2, 3)
CLAW = box(22, 28, 3, 2, 4)
JAW = box(36, 28, 2, 1, 3)
LEG = box(0, 36, 5, 1, 1)
SHIN = box(12, 36, 1, 6, 1)

LIGHT = {"top": 1, "bottom": -1, "front": 0, "back": -1, "right": 0, "left": -1}


def plate(cv, area, seed, face, tones=SHELL, bands=0):
    """Chitin: smooth and mottled, a little glossy along the top, crossed by darker bands where the plates overlap."""
    x0, y0, w, h = area
    for y in range(h):
        for x in range(w):
            t = 2 + LIGHT.get(face, 0) - (1 if smooth(x, y, seed, 2.5) < 0.3 else 0)
            if face == "top" and noise(x0 + x, y0 + y, seed) > 0.9:
                t += 1
            c = tones[max(0, min(4, t))]
            if face == "bottom":
                c = BELLY[max(0, min(4, t))]
            cv.put(x0 + x, y0 + y, c)
    if bands and face in ("top", "right", "left"):
        # Bands run across the length: rows on the top face, columns on the sides.
        for b in range(1, bands + 1):
            if face == "top":
                yy = y0 + b * h // (bands + 1)
                for x in range(w):
                    cv.put(x0 + x, yy, BAND[2 + (1 if noise(x0 + x, yy, seed) > 0.7 else 0)])
            else:
                xx = x0 + b * w // (bands + 1)
                for y in range(h):
                    cv.put(xx, y0 + y, BAND[2])


def skin():
    cv = Sheet(64, 64)
    for name, area in faces(CARAPACE):
        plate(cv, area, 11 + len(name), name, bands=3)
    # A keel down the middle of the back.
    x0, y0, w, h = CARAPACE["top"]
    for y in range(h):
        cv.put(x0 + w // 2 - 1, y0 + y, SHELL[1])
    for name, area in faces(HEAD):
        plate(cv, area, 21 + len(name), name)
    # The front of the head plate: dark mouthparts low in the middle.
    x0, y0, w, h = HEAD["front"]
    for x in range(2, w - 2):
        cv.put(x0 + x, y0 + h - 1, STING[2])
    for name, area in faces(EYE_BOX):
        plate(cv, area, 31, name, tones=BAND)
    x0, y0, w, h = EYE_BOX["top"]
    cv.put(x0, y0, EYE)
    for name in ("front", "right", "left", "back"):
        x0, y0, w, h = EYE_BOX[name]
        cv.put(x0, y0, EYE)
    # The tail: each segment banded at its joint, darkening toward the sting.
    for i, part in enumerate((TAIL1, TAIL2, TAIL3, TAIL4)):
        for name, area in faces(part):
            plate(cv, area, 40 + i * 7 + len(name), name, tones=SHELL if i < 2 else BAND, bands=1)
    for name, area in faces(BULB):
        x0, y0, w, h = area
        for y in range(h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, STING[2 + LIGHT.get(name, 0) + (1 if (x + y) % 3 == 0 else 0)])
    for name, area in faces(BARB):
        x0, y0, w, h = area
        for y in range(h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, STING[0 if name == "back" else 1])
    # Pincers: the arms in shell, the claws heavier and darker toward the tips, the jaws dark-edged.
    for part, seed in ((ARM, 60), (FOREARM, 64)):
        for name, area in faces(part):
            plate(cv, area, seed + len(name), name)
    for name, area in faces(CLAW):
        plate(cv, area, 70 + len(name), name, bands=1)
        if name in ("right", "left"):
            x0, y0, w, h = area
            tip = x0 if name == "right" else x0 + w - 1
            for y in range(h):
                cv.put(tip, y0 + y, BAND[1])
    x0, y0, w, h = CLAW["front"]
    for x in range(w):
        cv.put(x0 + x, y0 + h - 1, STING[2])
    for name, area in faces(JAW):
        plate(cv, area, 80 + len(name), name, tones=BAND)
    # Legs: thin, jointed, banded at the knee.
    for name, area in faces(LEG):
        plate(cv, area, 90 + len(name), name)
    for name, area in faces(SHIN):
        x0, y0, w, h = area
        for y in range(h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, (BAND if y in (0, h - 1) else SHELL)[2 + LIGHT.get(name, 0)])
    return cv.image()


def eyes():
    cv = Sheet(64, 64)
    # A glint on each eye bead.
    for name in ("top", "front", "right", "left", "back"):
        x0, y0, w, h = EYE_BOX[name]
        cv.put(x0, y0, (*GLINT, 150))
    # The barb, hot at its point, warm up into the bulb.
    for name, area in faces(BARB):
        x0, y0, w, h = area
        for y in range(h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, (*BARB_HOT, 255 if name == "back" else 200))
    for name in ("back", "top", "bottom"):
        x0, y0, w, h = BULB[name]
        for x in range(w):
            cv.put(x0 + x, y0 + (h - 1 if name != "back" else 0), (*BARB_HOT, 90))
    return cv.image()


def _count(lo, hi):
    return {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}


def _looting(hi):
    return {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting',
            'count': {'type': 'minecraft:uniform', 'min': 0, 'max': hi}}


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/dunestinger.png')
    g.save(eyes(), g.ASSETS / 'textures/entity/dunestinger_eyes.png')
    name = 'dunestinger_spawn_egg'
    g.save(egg_icon(("#9C7840", "#D4B070", "#E8CC90"), "#2E2016", name="dunestinger"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/dunestinger.json', {'type': 'minecraft:entity', 'pools': [
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:spider_eye', 'functions': [_count(0, 1), _looting(1)]}]},
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:string', 'functions': [_count(0, 2), _looting(1)]}]}],
        'random_sequence': 'wildercord:entities/dunestinger'})
