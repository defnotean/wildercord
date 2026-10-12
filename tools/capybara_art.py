"""Mirebath capybara: its skin (MirebathCapybaraModel's 64x32 box-UV layout), spawn egg, loot, spawn ground and field-guide text."""
from bobcat_art import egg_icon
from item_art import hexc, ramp
from monster_art import faces, smooth
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.mirebath_capybara': 'Mirebath Capybara',
 'item.wildercord.mirebath_capybara_spawn_egg': 'Mirebath Capybara Spawn Egg',
 'guide.wildercord.mirebath_capybara.hint': 'In the swamps, something round and brown sat in the water with its eyes shut.',
 'guide.wildercord.mirebath_capybara': 'A big, round, unhurried rodent of the swamps and jungles, russet-brown, with a blunt '
                                       'square head, tiny ears and no tail to speak of. Every so often it ambles off to the water '
                                       'and settles in for a long soak, sunk to its chin with its eyes shut. Anyone who soaks in '
                                       'the water beside it soaks up some of its calm and slowly heals. Hand it an apple while it '
                                       'bathes and it balances it on its head; while it wears it the bath washes away poison, '
                                       'hunger and nausea too, and when it gets out it eats the apple. Melon slices and sugar '
                                       'cane tempt it and breed it, and its young climb up onto any grown one\'s back and ride '
                                       'there a while.',
}

FUR = ramp("#5A3A22", "#744C2E", "#8E603C", "#A6764C", "#BC8C60")
DARK = ramp("#2A1C12", "#3A271A", "#4A3222", "#5C3F2A", "#6E4C34")
RED = ramp("#6A1414", "#921E1C", "#B82C26", "#D84A3A", "#EE7A60")
LEAF, STEM = hexc("#4E8A2E"), hexc("#4A3220")
EYE, NOSE = hexc("#120C08"), hexc("#1E140E")

BODY = box(0, 0, 7, 6, 11)
HEAD = box(36, 0, 5, 5, 7)
EAR = box(56, 12, 2, 1, 1)
LEG = box(0, 17, 2, 4, 2)
APPLE = box(8, 17, 3, 3, 3)
LID = box(20, 17, 0, 1, 1)

LIGHT = {"top": 1, "bottom": -1, "front": 0, "back": -1, "right": 0, "left": -1}


def coat(cv, part, seed, tones=FUR, base=2, cell=1.6):
    """Coarse, shaggy fur, lit from above, in short streaks with darker hairs among them."""
    for name, (x0, y0, w, h) in faces(part):
        for y in range(h):
            for x in range(w):
                t = base + LIGHT.get(name, 0) - (1 if smooth(x0 + x, y0 + y * 2, seed, cell) < 0.3 else 0)
                n = noise(x0 + x, y0 + y, seed)
                if n > 0.88:
                    t -= 1
                elif n < 0.06:
                    t += 1
                cv.put(x0 + x, y0 + y, tones[max(0, min(4, t))])


def skin():
    cv = Sheet(64, 32)
    coat(cv, BODY, 10)
    # A paler, thinner belly.
    coat(cv, {"bottom": BODY["bottom"]}, 12, base=3)
    coat(cv, HEAD, 20)
    # The face: a broad dark muzzle, two nostrils, and a little split lip.
    x0, y0, w, h = HEAD["front"]
    for y in range(1, h):
        for x in range(w):
            cv.put(x0 + x, y0 + y, DARK[3] if y < 3 else DARK[2])
    cv.put(x0 + 1, y0 + 2, NOSE)
    cv.put(x0 + 3, y0 + 2, NOSE)
    cv.put(x0 + 2, y0 + 4, NOSE)
    # The muzzle's darkness runs back along the sides of its face, and the eyes sit high and well back.
    for name, eye in (("right", 2), ("left", 4)):
        sx, sy, sw, sh = HEAD[name]
        front = sw - 1 if name == "right" else 0
        step = -1 if name == "right" else 1
        for i in range(2):
            for y in range(2, sh):
                cv.put(sx + front + step * i, sy + y, DARK[3])
        cv.put(sx + eye, sy + 1, EYE)
    # The top of its muzzle, darker towards the nose.
    x0, y0, w, h = HEAD["top"]
    for x in range(w):
        cv.put(x0 + x, y0, DARK[3])
    # Tiny ears, dark.
    coat(cv, EAR, 40, tones=DARK, base=3)
    # The lids it shuts in the bath: its own fur, a shade darker.
    for name, (x0, y0, w, h) in faces(LID):
        cv.put(x0, y0, FUR[1])
    # Short legs, darker toward the feet.
    coat(cv, LEG, 60, tones=DARK, base=3)
    # The apple: red, shiny on top, with a stem and a leaf.
    for name, (x0, y0, w, h) in faces(APPLE):
        for y in range(h):
            for x in range(w):
                t = 2 + LIGHT.get(name, 0) + (1 if noise(x0 + x, y0 + y, 70) > 0.75 else 0)
                cv.put(x0 + x, y0 + y, RED[max(0, min(4, t))])
    x0, y0, w, h = APPLE["top"]
    cv.put(x0 + 1, y0 + 1, STEM)
    cv.put(x0 + 2, y0 + 1, LEAF)
    cv.put(x0, y0, RED[4])
    return cv.image()


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/wildlife/mirebath_capybara.png')
    name = 'mirebath_capybara_spawn_egg'
    g.save(egg_icon(("#5A3A22", "#8E603C", "#BC8C60"), "#3A271A", name="mirebath_capybara"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/mirebath_capybara.json', {'type': 'minecraft:entity', 'pools': [],
        'random_sequence': 'wildercord:entities/mirebath_capybara'})
    g.write_json(g.DATA / 'tags/block/spawns_on/mirebath_capybara.json', {'replace': False, 'values': [
        'minecraft:grass_block', 'minecraft:dirt', 'minecraft:coarse_dirt', 'minecraft:podzol', 'minecraft:mud',
        'minecraft:moss_block', 'minecraft:mangrove_roots', 'minecraft:muddy_mangrove_roots']})
