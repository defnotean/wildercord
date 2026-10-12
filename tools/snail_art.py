"""Capcrown snail: its skin, shell and caps (CapcrownSnailModel's 64x64 box-UV layout), spawn egg, loot, spawn ground and field-guide text."""
from bobcat_art import egg_icon
from item_art import hexc, ramp
from monster_art import faces, smooth
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.capcrown_snail': 'Capcrown Snail',
 'item.wildercord.capcrown_snail_spawn_egg': 'Capcrown Snail Spawn Egg',
 'guide.wildercord.capcrown_snail.hint': 'Out on the mycelium, one of the mushrooms is going somewhere, very slowly.',
 'guide.wildercord.capcrown_snail': 'A plump grey-violet snail of the mushroom fields whose shell has grown a mushroom cap, red '
                                    'and white-spotted or broad and brown. It glides over the mycelium with its eye stalks '
                                    'swaying, stops to nibble, and sheds a drift of spores off its cap now and then; in the rain '
                                    'it gets about a little quicker. Run at it, let something hostile near, strike it or poke it '
                                    'in the eye and it draws into its shell with a puff of spores, and blows hardly touch it '
                                    'there; after a while its eyes come out first to look, then the rest of it. Shears take its '
                                    'cap, two to four mushrooms of its colour, and leave the shell bare, and the cap grows back in '
                                    'a few minutes, sooner if you feed it. Red or brown mushrooms tempt it and breed it, and its '
                                    'young carry only little nubs where a cap will be. Feed one and it trusts you a while and '
                                    'glides along after you, as fast as a snail can.',
}

SKIN = ramp("#5E5464", "#746A7A", "#8A8090", "#A096A4", "#B8AEBA")
SOLE = ramp("#6E6670", "#827A84", "#968E98", "#AAA2AA", "#BEB6BE")
SHELL = ramp("#5A3E2A", "#74523A", "#8E6A4A", "#A8845E", "#C4A076")
WHORL = ramp("#3E2A1C", "#54392A", "#6A4A36", "#82604A", "#9A785E")
RED = ramp("#7E1A18", "#A0261E", "#BE3426", "#D84A32", "#E86444")
BROWN = ramp("#5A3E28", "#704E32", "#86623E", "#9C784C", "#B08C5C")
SPOT = ramp("#C8C0B0", "#DAD2C2", "#EAE4D6", "#F4F0E6", "#FCFAF4")
GILL = ramp("#B8A890", "#C8B8A0", "#D6C8B0", "#E2D6C0", "#EEE4D0")
EYE, GLINT = hexc("#141018"), hexc("#E8E4F0")

BODY = box(0, 0, 4, 3, 12)
NECK = box(32, 0, 3, 4, 3)
STALK = box(44, 0, 1, 3, 1)
EYE_BOX = box(48, 0, 2, 2, 2)
FEELER = box(56, 0, 1, 1, 2)
SHELL_BOX = box(0, 16, 6, 6, 6)
WHORL_BOX = box(24, 16, 1, 3, 3)
NUB = box(32, 16, 2, 1, 2)
RED_CAP = box(0, 28, 8, 3, 8)
RED_TOP = box(32, 28, 6, 1, 6)
BROWN_CAP = box(0, 40, 8, 2, 8)
BROWN_TOP = box(32, 40, 6, 1, 6)

LIGHT = {"top": 1, "bottom": -1, "front": 0, "back": -1, "right": 0, "left": -1}


def shade(cv, area, tones, seed, face, cell=2.5, base=2):
    """Soft mottling, lit from above."""
    x0, y0, w, h = area
    for y in range(h):
        for x in range(w):
            t = base + LIGHT.get(face, 0) - (1 if smooth(x0 + x, y0 + y, seed, cell) < 0.3 else 0)
            if noise(x0 + x, y0 + y, seed) > 0.92:
                t += 1
            cv.put(x0 + x, y0 + y, tones[max(0, min(4, t))])


def slime(cv, area, seed, face):
    """The soft body: grey-violet, a glossy sheen along the top, little grooves crossing it."""
    shade(cv, area, SOLE if face == "bottom" else SKIN, seed, face, cell=2)
    x0, y0, w, h = area
    for y in range(h):
        for x in range(w):
            if noise(x0 + x, y0 + y, seed + 3) > 0.94:
                cv.put(x0 + x, y0 + y, SKIN[4])
            elif (x + 2 * y) % 5 == 0 and noise(x0 + x, y0 + y, seed + 7) > 0.55:
                cv.put(x0 + x, y0 + y, SKIN[1])


def shell(cv):
    for name, area in faces(SHELL_BOX):
        shade(cv, area, SHELL, 30 + len(name), name)
        x0, y0, w, h = area
        if name in ("right", "left"):
            # The spiral: rings drawn in toward a dark centre on each side.
            cx, cy = (w - 1) / 2, (h - 1) / 2
            for y in range(h):
                for x in range(w):
                    r = max(abs(x - cx), abs(y - cy))
                    if int(r) % 2 == 1:
                        cv.put(x0 + x, y0 + y, WHORL[2 + (1 if name == "right" else 0)])
            cv.put(x0 + int(cx), y0 + int(cy), WHORL[0])
        elif name in ("top", "front", "back"):
            # Growth lines across the shell.
            for y in range(0, h, 2):
                for x in range(w):
                    if noise(x0 + x, y0 + y, 41) > 0.3:
                        cv.put(x0 + x, y0 + y, SHELL[1 + LIGHT.get(name, 0) + 1])
    for name, area in faces(WHORL_BOX):
        shade(cv, area, WHORL, 50 + len(name), name)
    x0, y0, w, h = WHORL_BOX["right"]
    cv.put(x0 + 1, y0 + 1, WHORL[0])
    x0, y0, w, h = WHORL_BOX["left"]
    cv.put(x0 + 1, y0 + 1, WHORL[0])
    for name, area in faces(NUB):
        shade(cv, area, SPOT, 60 + len(name), name, base=1)


def cap(cv, rim, top, tones, spots, seed):
    """A mushroom cap: a rim box and a domed top; red with white spots, or brown and plain with a paler edge. Pale gills below."""
    for part, s in ((rim, seed), (top, seed + 9)):
        for name, area in faces(part):
            if name == "bottom":
                x0, y0, w, h = area
                for y in range(h):
                    for x in range(w):
                        # Gills radiating from the middle.
                        d = abs(x - (w - 1) / 2) + abs(y - (h - 1) / 2)
                        cv.put(x0 + x, y0 + y, GILL[1 if (x + y) % 2 == 0 else 2] if d > 1.5 else GILL[0])
                continue
            shade(cv, area, tones, s + len(name), name, cell=3)
            x0, y0, w, h = area
            if spots:
                for y in range(h):
                    for x in range(w):
                        if noise(x0 + x, y0 + y, s + 21) > (0.91 if name == "top" else 0.9):
                            cv.put(x0 + x, y0 + y, SPOT[3 if name == "top" else 2])
                            if name == "top" and x + 1 < w:
                                cv.put(x0 + x + 1, y0 + y, SPOT[2])
            elif name != "top":
                # The brown cap's paler underturned edge.
                for x in range(w):
                    cv.put(x0 + x, y0 + h - 1, tones[4])


def skin():
    cv = Sheet(64, 64)
    for name, area in faces(BODY):
        slime(cv, area, 10 + len(name), name)
    # A pale frill along the lower edge of the foot.
    for name in ("right", "left", "front", "back"):
        x0, y0, w, h = BODY[name]
        for x in range(w):
            cv.put(x0 + x, y0 + h - 1, SOLE[3])
    for name, area in faces(NECK):
        slime(cv, area, 20 + len(name), name)
    # A little mouth low on the front of the neck.
    x0, y0, w, h = NECK["front"]
    cv.put(x0 + 1, y0 + h - 1, SKIN[0])
    for name, area in faces(STALK):
        slime(cv, area, 24 + len(name), name)
    for name, area in faces(EYE_BOX):
        shade(cv, area, SKIN, 26 + len(name), name, base=3)
    # The eyes: a dark bead on the front with a glint, dark across the top.
    x0, y0, w, h = EYE_BOX["front"]
    for y in range(h):
        for x in range(w):
            cv.put(x0 + x, y0 + y, EYE)
    cv.put(x0, y0, GLINT)
    x0, y0, w, h = EYE_BOX["top"]
    cv.put(x0, y0 + h - 1, EYE)
    cv.put(x0 + 1, y0 + h - 1, EYE)
    for name, area in faces(FEELER):
        shade(cv, area, SKIN, 28 + len(name), name, base=1)
    shell(cv)
    cap(cv, RED_CAP, RED_TOP, RED, True, 70)
    cap(cv, BROWN_CAP, BROWN_TOP, BROWN, False, 90)
    return cv.image()


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/wildlife/capcrown_snail.png')
    name = 'capcrown_snail_spawn_egg'
    g.save(egg_icon(("#5E5464", "#8A8090", "#B8AEBA"), "#BE3426", name="capcrown_snail"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/capcrown_snail.json', {'type': 'minecraft:entity', 'pools': [], 'random_sequence': 'wildercord:entities/capcrown_snail'})
    g.write_json(g.DATA / 'tags/block/spawns_on/capcrown_snail.json', {'replace': False, 'values': [
        '#minecraft:animals_spawnable_on', 'minecraft:mycelium', 'minecraft:grass_block', 'minecraft:moss_block', 'minecraft:podzol']})
