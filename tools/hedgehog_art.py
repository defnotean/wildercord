"""Thistle hedgehog: its skin (ThistleHedgehogModel's 64x64 box-UV layout), spawn egg, loot, spawn ground and field-guide text."""
from bobcat_art import egg_icon, fill
from item_art import hexc, mix, ramp
from monster_art import faces, fur, smooth
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.thistle_hedgehog': 'Thistle Hedgehog',
 'item.wildercord.thistle_hedgehog_spawn_egg': 'Thistle Hedgehog Spawn Egg',
 'guide.wildercord.thistle_hedgehog.hint': 'Look in the grass of meadows, plains and forests for something small and spiky snuffling about.',
 'guide.wildercord.thistle_hedgehog': 'A small round forager with a coat of cream-tipped quills, a twitching black nose and, more often than not, '
                                      'a thistle bloom, a leaf or a berry caught on its back. Run at it, let something hostile near or strike '
                                      'it, and it curls into a ball: struck like that it takes little and its quills prick back. Give it a '
                                      'moment and it peeks out, then uncurls. Sweet berries tempt it and breed it, and its young are tiny '
                                      'hoglets. Feed one and it trusts you a while: it follows you about, no longer minds your running, and '
                                      'often brings you something it found.',
}

QUILL = ramp("#2E2119", "#3E2D21", "#523B2A", "#684C36", "#7E5F45")
TIP = ramp("#B79E7E", "#D2BC9A", "#E8D8BC")
FACE = ramp("#8E7258", "#A88A6C", "#C2A483", "#D6BC9C", "#E6D2B6")
BELLY_TONES = ramp("#9C8064", "#B39678", "#C8AC8C", "#D9C0A2", "#E6D2B8")
FOOT = ramp("#3A2C24", "#4E3C30", "#64503F")
EYE, SHINE = hexc("#0A0806"), hexc("#B9B4AE")
NOSE_C, NOSE_SHINE = hexc("#14100E"), hexc("#5C5652")
CHEEK = hexc("#D99C8C")
EAR_IN = hexc("#C98F82")
STEM_C, BLOOM_C, BLOOM_TIP = ramp("#3E6B30", "#5B8A40"), ramp("#7A3F94", "#9A58B4", "#BC7ED2"), hexc("#E0B4EC")
LEAF_C = ramp("#3F7A2E", "#58983C", "#78B652")
BERRY_C = ramp("#7C1622", "#B02838", "#D84A58")

BELLY = box(0, 0, 6, 4, 8)
MANTLE = box(0, 12, 8, 6, 9)
CREST = box(0, 27, 6, 1, 7)
HEAD = box(34, 0, 6, 5, 3)
SNOUT = box(34, 8, 3, 3, 3)
NOSE = box(46, 8, 1, 1, 1)
EAR = box(52, 0, 2, 2, 1)
LEG = box(34, 16, 2, 2, 2)
STEM = box(44, 16, 1, 2, 1)
BLOOM = box(48, 16, 2, 2, 2)
LEAF = box(34, 22, 4, 1, 3)
BERRY = box(48, 22, 2, 2, 2)


def quills(cv, area, seed, light=0):
    """Rows of quills laid back: dark roots, a brown shaft, a cream tip, staggered row to row so they bristle."""
    x0, y0, w, h = area
    for y in range(h):
        for x in range(w):
            k = (x + (y // 2) * 2) % 3
            band = smooth(x0 + x, y0 + y, seed, 2.5)
            t = 2 + light + (1 if band > 0.66 else 0) - (1 if band < 0.3 else 0)
            if y % 2 == 0 and k == 0:
                c = TIP[min(2, max(0, 1 + light + (1 if noise(x0 + x, y0 + y, seed) > 0.6 else 0)))]
            elif k == 2:
                c = QUILL[max(0, t - 2)]
            else:
                c = QUILL[max(0, min(4, t))]
            cv.put(x0 + x, y0 + y, c)


def skin():
    cv = Sheet(64, 64)
    # The belly and face: soft, short, cream-tan fur.
    for name, area in faces(BELLY):
        fur(cv, area, BELLY_TONES, 10 + len(name), name, base=2, along_y=name in ("top", "bottom"))
    for part, seed in ((HEAD, 2), (SNOUT, 3), (EAR, 4)):
        for name, area in faces(part):
            fur(cv, area, FACE, seed * 10 + len(name), name, base=2)
    # The quills: brighter on top, deeper down the sides and behind.
    for part, seed in ((MANTLE, 21), (CREST, 23)):
        for name, area in faces(part):
            quills(cv, area, seed + len(name), {"top": 1, "back": -1, "bottom": -2}.get(name, 0))
    # Beneath the mantle the quills thin to brown skin, the belly fur showing through at the edge.
    x0, y0, w, h = MANTLE["bottom"]
    fill(cv, (x0, y0, w, h), QUILL[1])
    # Where the mantle meets the face, a fringe of short pale fur.
    x0, y0, w, h = MANTLE["front"]
    for x in range(w):
        cv.put(x0 + x, y0 + h - 1, FACE[3] if noise(x0 + x, y0, 5) > 0.3 else FACE[2])
    # The face: beady black eyes with a gleam above, rosy cheeks, a darker mask fading up into the quills.
    x0, y0, w, h = HEAD["front"]
    for x in range(w):
        cv.put(x0 + x, y0, FACE[1])
    for ex in (1, 4):
        cv.put(x0 + ex, y0 + 1, SHINE)
        cv.put(x0 + ex, y0 + 2, EYE)
    cv.put(x0, y0 + 3, CHEEK)
    cv.put(x0 + 5, y0 + 3, CHEEK)
    x0, y0, w, h = HEAD["top"]
    for x in range(w):
        for y in range(h):
            cv.put(x0 + x, y0 + y, mix(FACE[1], QUILL[3], 0.4 + 0.2 * y))
    # The snout darkens toward its tip, a little mouth line under it.
    for name in ("front", "top", "right", "left"):
        x0, y0, w, h = SNOUT[name]
        for y in range(h):
            for x in range(w):
                c = cv.get(x0 + x, y0 + y)
                if c is not None and name != "front":
                    cv.put(x0 + x, y0 + y, mix(c, FACE[0], 0.25 * (x if name == "left" else w - 1 - x if name == "right" else h - 1 - y) / max(1, w - 1)))
    x0, y0, w, h = SNOUT["front"]
    cv.put(x0 + 1, y0 + 2, FACE[0])
    for name, area in faces(NOSE):
        fill(cv, area, NOSE_C)
    x0, y0, w, h = NOSE["front"]
    cv.put(x0, y0, NOSE_SHINE)
    # Ears: rosy inside.
    x0, y0, w, h = EAR["front"]
    for x in range(w):
        cv.put(x0 + x, y0 + h - 1, EAR_IN)
    # Stubby dark feet, lighter toes.
    for name, area in faces(LEG):
        fur(cv, area, FOOT, 40 + len(name), name, base=1)
    for name in ("front", "right", "left", "back"):
        x0, y0, w, h = LEG[name]
        for x in range(w):
            cv.put(x0 + x, y0 + h - 1, FOOT[2])
    # What rides in its quills: a thistle (green stem, purple bloom bristling paler at the top), a leaf, a berry.
    for name, area in faces(STEM):
        x0, y0, w, h = area
        for y in range(h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, STEM_C[(x + y) % 2])
    for name, area in faces(BLOOM):
        x0, y0, w, h = area
        for y in range(h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, BLOOM_TIP if name == "top" and (x + y) % 2 == 0 else BLOOM_C[2 if y == 0 else (x + y) % 2])
        if name in ("front", "right", "left", "back"):
            for x in range(w):
                cv.put(x0 + x, y0 + h - 1, STEM_C[1])
    for name, area in faces(LEAF):
        x0, y0, w, h = area
        for y in range(h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, LEAF_C[1 if name == "top" else 0])
        if name == "top":
            for x in range(w):
                cv.put(x0 + x, y0 + h // 2, LEAF_C[2])
    for name, area in faces(BERRY):
        x0, y0, w, h = area
        for y in range(h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, BERRY_C[0 if name == "bottom" else 1])
        cv.put(x0, y0, BERRY_C[2])
    return cv.image()


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/wildlife/thistle_hedgehog.png')
    name = 'thistle_hedgehog_spawn_egg'
    g.save(egg_icon(("#3E2D21", "#684C36", "#D2BC9A"), "#9A58B4", name="thistle_hedgehog"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/thistle_hedgehog.json', {'type': 'minecraft:entity', 'pools': [], 'random_sequence': 'wildercord:entities/thistle_hedgehog'})
    g.write_json(g.DATA / 'tags/block/spawns_on/thistle_hedgehog.json', {'replace': False, 'values': [
        '#minecraft:animals_spawnable_on', 'minecraft:grass_block', 'minecraft:podzol', 'minecraft:coarse_dirt', 'minecraft:moss_block']})
