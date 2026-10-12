"""Reefgrasp Octopus: its skin (ReefgraspOctopusModel's 64x64 box-UV layout), spawn egg, loot and field-guide text."""
from bobcat_art import egg_icon
from item_art import hexc, ramp
from monster_art import faces, smooth
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.reefgrasp_octopus': 'Reefgrasp Octopus',
 'item.wildercord.reefgrasp_octopus_spawn_egg': 'Reefgrasp Octopus Spawn Egg',
 'guide.wildercord.reefgrasp_octopus.hint': 'On the seafloor, a patch of sand with arms, and two gold eyes watching you swim over.',
 'guide.wildercord.reefgrasp_octopus': 'A big mottled octopus of the seas, ruddy and warty, its eight long arms ringed with pale '
                                       'suckers and its gold eyes slotted. It lies on the seafloor with its arms spread, faded to '
                                       'the colour of the sand, and waits. Swim close and its arms rise, then reach out and wrap '
                                       'round you: held, you are drawn down to it and squeezed, and the breath goes out of you '
                                       'far faster than it should, until it lets go, you hurt it enough that it does, or you get '
                                       'out of the water. Strike it and it squirts a cloud of ink that blinds everyone near, lets '
                                       'go of whatever it held and jets off backwards, to settle somewhere else and hide again. '
                                       'Roused, it comes after you and bites with its beak. Out of the water it dries out.',
}

SKIN = ramp("#5A1E14", "#7E2E1C", "#A04428", "#BE5E36", "#D67C4C")
WART = ramp("#3E140E", "#4C1A12", "#E09A6A", "#EEB48A", "#F6CCA6")
SUCKER = ramp("#B07C64", "#C8957C", "#DEB096", "#ECC6AE", "#F6DCC8")
EYE = hexc("#D8A828")
EYE_RIM = hexc("#8A5A1A")
PUPIL = hexc("#0A0604")
SIPHON = ramp("#6A2418", "#8A3420", "#A84A2C", "#C2603A", "#D47A50")

HEAD = box(0, 0, 7, 5, 7)
EYE_BOX = box(28, 0, 2, 2, 2)
SIPHON_BOX = box(28, 6, 2, 2, 3)
ARM = box(40, 0, 2, 6, 2)
TIP = box(48, 0, 1, 6, 1)
MANTLE = box(0, 12, 8, 8, 9)

LIGHT = {"top": 1, "bottom": -1, "front": 0, "back": -1, "right": 0, "left": -1}


def mottle(cv, part, seed, base=2, warts=0.86):
    """Mottled ruddy skin, blotched darker, with pale warts here and there."""
    for name, (x0, y0, w, h) in faces(part):
        for y in range(h):
            for x in range(w):
                px, py = x0 + x, y0 + y
                t = base + LIGHT.get(name, 0)
                blotch = smooth(px, py, seed, 2.0)
                if blotch > 0.68:
                    t -= 1
                elif blotch < 0.25:
                    t += 1
                c = SKIN[max(0, min(4, t))]
                if noise(px, py, seed + 1) > warts:
                    c = WART[2 + (1 if noise(px, py, seed + 2) > 0.5 else 0)]
                cv.put(px, py, c)


def arm(cv, part, seed):
    """An arm: mottled skin on its back, a row of pale suckers down its underside."""
    mottle(cv, part, seed, warts=0.92)
    # The arm's underside is its 'front' face (local -z), which faces down and inward on the model.
    x0, y0, w, h = part["front"]
    for y in range(h):
        for x in range(w):
            cv.put(x0 + x, y0 + y, SUCKER[3] if (x + y) % 2 == 0 else SUCKER[1])
    x0, y0, w, h = part["bottom"]
    for y in range(h):
        for x in range(w):
            cv.put(x0 + x, y0 + y, SUCKER[2])


def skin():
    cv = Sheet(64, 64)
    mottle(cv, HEAD, 10)
    mottle(cv, MANTLE, 20, warts=0.8)
    # A paler ridge down the top of the mantle.
    x0, y0, w, h = MANTLE["top"]
    for y in range(h):
        cv.put(x0 + w // 2, y0 + y, SKIN[4])
    # The head's underside: pale, the beak a dark point in the middle.
    x0, y0, w, h = HEAD["bottom"]
    for y in range(h):
        for x in range(w):
            cv.put(x0 + x, y0 + y, SUCKER[1])
    cv.put(x0 + w // 2, y0 + h // 2, PUPIL)
    cv.put(x0 + w // 2, y0 + h // 2 - 1, WART[0])
    # The eyes: gold, a dark slot of a pupil across the front, a ruddy lid over the top.
    for name, (x0, y0, w, h) in faces(EYE_BOX):
        for y in range(h):
            for x in range(w):
                c = EYE_RIM if name in ("back", "bottom") else EYE
                if name == "top":
                    c = SKIN[3]
                cv.put(x0 + x, y0 + y, c)
    for name in ("front", "right", "left"):
        x0, y0, w, h = EYE_BOX[name]
        for x in range(w):
            cv.put(x0 + x, y0 + 1, PUPIL)
    # The siphon: a darker fleshy tube, open at its back end.
    mottle(cv, SIPHON_BOX, 30, base=1)
    x0, y0, w, h = SIPHON_BOX["back"]
    for y in range(h):
        for x in range(w):
            cv.put(x0 + x, y0 + y, WART[0])
    arm(cv, ARM, 40)
    arm(cv, TIP, 50)
    return cv.image()


def _count(lo, hi):
    return {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}


def _looting(hi):
    return {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting',
            'count': {'type': 'minecraft:uniform', 'min': 0, 'max': hi}}


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/reefgrasp_octopus.png')
    name = 'reefgrasp_octopus_spawn_egg'
    g.save(egg_icon(("#7E2E1C", "#A04428", "#BE5E36"), "#D8A828", name="reefgrasp_octopus"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/reefgrasp_octopus.json', {'type': 'minecraft:entity', 'pools': [
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:ink_sac', 'functions': [_count(1, 3), _looting(1)]}]},
        {'rolls': 1, 'conditions': [{'condition': 'minecraft:random_chance_with_enchanted_bonus', 'enchantment': 'minecraft:looting',
                                     'unenchanted_chance': 0.05, 'enchanted_chance': {'type': 'minecraft:linear', 'base': 0.08, 'per_level_above_first': 0.03}}],
         'entries': [{'type': 'minecraft:item', 'name': 'minecraft:nautilus_shell'}]}],
        'random_sequence': 'wildercord:entities/reefgrasp_octopus'})
