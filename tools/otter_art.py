"""Brook otter: its coat (BrookOtterModel's 64x64 box-UV layout), the shell, pebble and fish it plays with, spawn egg, loot, spawn
ground and field-guide text."""
from bobcat_art import egg_icon, fill
from item_art import hexc, mix, ramp
from monster_art import faces, fur
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.brook_otter': 'Brook Otter',
 'item.wildercord.brook_otter_spawn_egg': 'Brook Otter Spawn Egg',
 'guide.wildercord.brook_otter.hint': 'Watch the rivers and swamps for something brown floating on its back.',
 'guide.wildercord.brook_otter': 'A sleek brown otter of the rivers and swamps, cream at the throat, with a thick tail tapering to a point. '
                                 'It swims fast and low, and now and then rolls onto its back to float at the surface with its paws folded '
                                 'on its chest. Floating, it brings up a shell and knocks it open on a pebble on its belly. Two otters '
                                 'floating near each other drift together and hold paws so they will not float apart, and a pup climbs '
                                 'onto a floating parent to ride on its belly. It hunts cod and salmon, and floats on its back to eat them. '
                                 'Ashore it sometimes sits up and juggles a pebble from paw to paw. Raw cod or salmon tempts it and breeds '
                                 'it; feed one and it trusts you a while, follows you about, and now and then drops a little gift at your feet.',
}

COAT = ramp("#3E2616", "#4E321E", "#5E3E26", "#6E4A2E", "#7E5838")
BELLY = ramp("#8E6E50", "#A6865F", "#BC9C74", "#CDB08A", "#DCC29E")
CREAM = ramp("#C8B08C", "#D6BE9A", "#E2CCA8", "#ECD8B6", "#F4E4C6")
EYE, SHINE = hexc("#0A0706"), hexc("#8C8078")
NOSE_C, NOSE_SHINE = hexc("#1A1210"), hexc("#5A4C46")
WHISKER = hexc("#E8DCC8")
PAW = ramp("#2A1A10", "#36241A", "#443024")
SHELL = ramp("#6E6A64", "#8E8880", "#AAA49A", "#C6C0B4", "#DCD6CA")
PEBBLE = ramp("#3C3A38", "#4C4A47", "#5E5B58")
FISH = ramp("#4E5E68", "#6E8290", "#94A6B0", "#BAC8CE", "#DCE4E6")

BODY = box(0, 0, 6, 5, 12)
HEAD = box(38, 0, 5, 4, 4)
SNOUT = box(38, 10, 3, 2, 2)
NOSE = box(50, 10, 2, 1, 1)
EAR = box(56, 10, 1, 1, 1)
TAIL = box(0, 18, 4, 3, 6)
TIP = box(20, 18, 3, 2, 6)
LEG = box(40, 18, 2, 2, 2)
SHELL_BOX = box(0, 28, 3, 1, 3)
FISH_BOX = box(16, 28, 1, 2, 6)
FIN = box(32, 28, 1, 3, 2)
PEBBLE_BOX = box(40, 28, 1, 1, 1)


def underside(cv, area, belly, seed, rows):
    """The lower `rows` rows of a side face, the paler belly coming up into it in a soft, uneven line."""
    x0, y0, w, h = area
    for x in range(w):
        edge = rows - (1 if noise(x0 + x, y0, seed) > 0.6 else 0)
        for y in range(h - edge, h):
            cv.put(x0 + x, y0 + y, belly[3 if y > h - edge else 2])


def skin():
    cv = Sheet(64, 64)
    # The coat: a rich brown, sleek and stroked toward the tail; the belly a paler, warmer brown; cream at the throat and chest.
    for name, area in faces(BODY):
        fur(cv, area, BELLY if name == "bottom" else COAT, 10 + len(name), name, base=2, along_y=name in ("top", "bottom"))
    for name in ("right", "left"):
        underside(cv, BODY[name], BELLY, 7 + len(name), 1)
    x0, y0, w, h = BODY["bottom"]
    # The bottom face runs head to tail down its length: the cream throat patch spreads over the front third.
    for y in range(h // 3 + 1):
        for x in range(w):
            if y < h // 3 or noise(x0 + x, y0 + y, 5) > 0.5:
                cv.put(x0 + x, y0 + y, CREAM[2 + (1 if noise(x0 + x, y0 + y, 6) > 0.6 else 0)])
    fur(cv, BODY["front"], CREAM, 31, "front", base=2)
    # The head: brown over the crown, cream cheeks, chin and muzzle, small dark eyes, white whisker dots.
    for part, seed in ((HEAD, 2), (SNOUT, 3), (EAR, 4)):
        for name, area in faces(part):
            fur(cv, area, CREAM if name == "bottom" else COAT, seed * 10 + len(name), name, base=2)
    for name in ("right", "left", "front"):
        underside(cv, HEAD[name], CREAM, 21 + len(name), 1)
    x0, y0, w, h = HEAD["front"]
    for ex in (0, w - 1):
        cv.put(x0 + ex, y0 + 1, EYE)
    cv.put(x0, y0, SHINE)
    cv.put(x0 + w - 1, y0, SHINE)
    for name in ("right", "left"):
        x0, y0, w, h = HEAD[name]
        cv.put(x0 + (w - 1 if name == "right" else 0), y0 + 1, EYE)
    for name, area in faces(SNOUT):
        if name != "top":
            fur(cv, area, CREAM, 40 + len(name), name, base=2)
    for name in ("right", "left"):
        x0, y0, w, h = SNOUT[name]
        cv.put(x0 + (0 if name == "right" else w - 1), y0 + h - 1, WHISKER)
    x0, y0, w, h = SNOUT["front"]
    cv.put(x0, y0 + 1, WHISKER)
    cv.put(x0 + w - 1, y0 + 1, WHISKER)
    for name, area in faces(NOSE):
        fill(cv, area, NOSE_C)
    x0, y0, w, h = NOSE["front"]
    cv.put(x0, y0, NOSE_SHINE)
    for name, area in faces(EAR):
        fill(cv, area, COAT[1])
    # Short dark legs and webbed paws.
    for name, area in faces(LEG):
        fur(cv, area, COAT, 50 + len(name), name, base=1)
    for name in ("front", "right", "left", "back"):
        x0, y0, w, h = LEG[name]
        for x in range(w):
            cv.put(x0 + x, y0 + h - 1, PAW[1])
    fill(cv, LEG["bottom"], PAW[0])
    # The thick tail, darkening toward its tip.
    for name, area in faces(TAIL):
        fur(cv, area, COAT, 60 + len(name), name, base=2, along_y=name in ("top", "bottom"))
    for name, area in faces(TIP):
        fur(cv, area, COAT, 70 + len(name), name, base=1, along_y=name in ("top", "bottom"))
    # A grey river shell ridged in pale bands; a dark pebble; a silver fish, darker along its back.
    for name, area in faces(SHELL_BOX):
        x0, y0, w, h = area
        for y in range(h):
            for x in range(w):
                ridge = (x + y) % 2 == 0
                cv.put(x0 + x, y0 + y, SHELL[3 if ridge else 1] if name in ("top", "bottom") else SHELL[2])
    for name, area in faces(PEBBLE_BOX):
        fill(cv, area, PEBBLE[1])
    for name, area in faces(FISH_BOX) + faces(FIN):
        x0, y0, w, h = area
        for y in range(h):
            for x in range(w):
                c = FISH[1] if name == "top" else FISH[3] if name == "bottom" else FISH[2 + (1 if y >= h // 2 else 0) - (1 if y == 0 else 0)]
                if noise(x0 + x, y0 + y, 81) > 0.75:
                    c = mix(c, FISH[4], 0.5)
                cv.put(x0 + x, y0 + y, c)
    x0, y0, w, h = FISH_BOX["front"]
    cv.put(x0, y0, EYE)
    return cv.image()


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/wildlife/brook_otter.png')
    name = 'brook_otter_spawn_egg'
    g.save(egg_icon(("#3E2616", "#5E3E26", "#7E5838"), "#E2CCA8", name="brook_otter"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/brook_otter.json', {'type': 'minecraft:entity', 'pools': [], 'random_sequence': 'wildercord:entities/brook_otter'})
    g.write_json(g.DATA / 'tags/block/spawns_on/brook_otter.json', {'replace': False, 'values': [
        '#minecraft:animals_spawnable_on', 'minecraft:grass_block', 'minecraft:sand', 'minecraft:gravel', 'minecraft:dirt',
        'minecraft:clay', 'minecraft:mud', 'minecraft:coarse_dirt']})
