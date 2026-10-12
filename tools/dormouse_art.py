"""Glowberry dormouse: its skin and the glow of its cheeks (GlowberryDormouseModel's 64x32 box-UV layout), spawn egg, loot, spawn ground and field-guide text."""
from bobcat_art import egg_icon
from item_art import hexc, ramp
from monster_art import faces, smooth
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.glowberry_dormouse': 'Glowberry Dormouse',
 'item.wildercord.glowberry_dormouse_spawn_egg': 'Glowberry Dormouse Spawn Egg',
 'guide.wildercord.glowberry_dormouse.hint': 'Down in the lush caves, the glow berries on the lowest vines keep going missing.',
 'guide.wildercord.glowberry_dormouse': 'A tiny round golden mouse of the lush caves, with great round ears, big dark eyes and a '
                                        'long fluffy tail. By day it sleeps curled in a ball on the moss with its tail wrapped '
                                        'round over its nose, and nothing short of a blow or a berry wakes it. By night it '
                                        'scurries about under the cave vines, and wherever a glow berry hangs low enough it '
                                        'stands up on its hind legs, plucks it and stuffs it in its cheeks, which swell and glow '
                                        'with what they hold, three at most; left to itself it eats them, one now and then. Run '
                                        'near it and it bolts, but creep up and it lets you. Glow berries tempt it and breed it. '
                                        'Feed one and it trusts you a while, scurries along after you, and now and then brings '
                                        'a berry out of its cheeks and leaves it at your feet.',
}

FUR = ramp("#8A4E1E", "#A8642A", "#C47E36", "#D89A4A", "#E8B464")
BELLY = ramp("#BFA27E", "#D2B894", "#E2CCA8", "#EEDCBC", "#F8ECD4")
PINK = ramp("#A86A62", "#C08078", "#D4968C", "#E2ACA0", "#F0C4B8")
EYE, GLINT = hexc("#120C0A"), hexc("#F2ECE4")
BERRY = hexc("#FFC048")

BODY = box(0, 0, 5, 4, 6)
HEAD = box(24, 0, 4, 4, 4)
EAR = box(42, 0, 2, 2, 1)
NOSE = box(42, 4, 1, 1, 1)
CHEEK = box(50, 0, 2, 2, 2)
TAIL = box(0, 12, 2, 2, 5)
BRUSH = box(16, 12, 3, 3, 4)
LEG = box(32, 12, 1, 2, 1)
PAW = box(38, 12, 1, 2, 1)

LIGHT = {"top": 1, "bottom": -1, "front": 0, "back": -1, "right": 0, "left": -1}


def shade(cv, area, tones, seed, face, cell=2.0, base=2):
    """Soft fur, lit from above, with a few lighter hairs."""
    x0, y0, w, h = area
    for y in range(h):
        for x in range(w):
            t = base + LIGHT.get(face, 0) - (1 if smooth(x0 + x, y0 + y, seed, cell) < 0.3 else 0)
            if noise(x0 + x, y0 + y, seed) > 0.9:
                t += 1
            cv.put(x0 + x, y0 + y, tones[max(0, min(4, t))])


def furred(cv, part, seed):
    """Golden above, fading to cream down the sides, cream beneath."""
    for name, area in faces(part):
        if name == "bottom":
            shade(cv, area, BELLY, seed + len(name), name, base=2)
            continue
        shade(cv, area, FUR, seed + len(name), name)
        if name in ("top",):
            continue
        x0, y0, w, h = area
        for y in range(h):
            if y >= h - max(1, h // 3):
                for x in range(w):
                    cv.put(x0 + x, y0 + y, BELLY[2 + (1 if noise(x0 + x, y0 + y, seed + 3) > 0.6 else 0)])


def skin():
    cv = Sheet(64, 32)
    furred(cv, BODY, 10)
    furred(cv, HEAD, 20)
    # The face: two great dark eyes with a glint each, and the muzzle pale.
    x0, y0, w, h = HEAD["front"]
    for y in (1, 2):
        cv.put(x0, y0 + y, EYE)
        cv.put(x0 + 3, y0 + y, EYE)
    cv.put(x0, y0 + 1, GLINT)
    cv.put(x0 + 3, y0 + 1, GLINT)
    cv.put(x0 + 1, y0 + 3, BELLY[3])
    cv.put(x0 + 2, y0 + 3, BELLY[3])
    # The eyes wrap round onto the sides of the head, at the front edge.
    for name, col in (("right", 3), ("left", 0)):
        sx, sy, sw, sh = HEAD[name]
        cv.put(sx + col, sy + 1, EYE)
        cv.put(sx + col, sy + 2, EYE)
    for name, area in faces(EAR):
        shade(cv, area, FUR, 30 + len(name), name, base=3)
    # Pink inside the ears.
    for name in ("front",):
        x0, y0, w, h = EAR[name]
        for y in range(h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, PINK[3 if y == 0 else 2])
    for name, area in faces(NOSE):
        shade(cv, area, PINK, 40 + len(name), name, base=2)
    x0, y0, w, h = NOSE["front"]
    cv.put(x0, y0, PINK[0])
    for name, area in faces(CHEEK):
        shade(cv, area, FUR, 50 + len(name), name, base=3)
    furred(cv, TAIL, 60)
    for name, area in faces(BRUSH):
        shade(cv, area, FUR, 70 + len(name), name, cell=1.5, base=2)
    # The brush's tip, paler.
    x0, y0, w, h = BRUSH["back"]
    for y in range(h):
        for x in range(w):
            cv.put(x0 + x, y0 + y, FUR[4] if (x + y) % 2 == 0 else BELLY[3])
    for part, s in ((LEG, 80), (PAW, 90)):
        for name, area in faces(part):
            shade(cv, area, PINK, s + len(name), name, base=2)
    return cv.image()


def glow():
    cv = Sheet(64, 32)
    # The berries showing through its cheeks.
    for name, area in faces(CHEEK):
        x0, y0, w, h = area
        for y in range(h):
            for x in range(w):
                a = 200 if name not in ("bottom", "back") else 120
                if noise(x0 + x, y0 + y, 99) > 0.75:
                    a = 255
                cv.put(x0 + x, y0 + y, (*BERRY, a))
    return cv.image()


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/wildlife/glowberry_dormouse.png')
    g.save(glow(), g.ASSETS / 'textures/entity/wildlife/glowberry_dormouse_glow.png')
    name = 'glowberry_dormouse_spawn_egg'
    g.save(egg_icon(("#A8642A", "#C47E36", "#E8B464"), "#FFC048", name="glowberry_dormouse"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/glowberry_dormouse.json', {'type': 'minecraft:entity', 'pools': [], 'random_sequence': 'wildercord:entities/glowberry_dormouse'})
    g.write_json(g.DATA / 'tags/block/spawns_on/glowberry_dormouse.json', {'replace': False, 'values': [
        'minecraft:moss_block', 'minecraft:clay', 'minecraft:rooted_dirt', 'minecraft:grass_block', 'minecraft:stone',
        'minecraft:deepslate', 'minecraft:tuff']})
