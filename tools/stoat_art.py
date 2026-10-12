"""Snowcap stoat: its two coats (SnowcapStoatModel's 64x32 box-UV layout), spawn egg, loot, spawn ground and field-guide text."""
from bobcat_art import egg_icon, fill
from item_art import hexc, mix, ramp
from monster_art import faces, fur
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.snowcap_stoat': 'Snowcap Stoat',
 'item.wildercord.snowcap_stoat_spawn_egg': 'Snowcap Stoat Spawn Egg',
 'guide.wildercord.snowcap_stoat.hint': 'Look across the snowy plains, taigas and slopes for something long and quick standing up to look at you.',
 'guide.wildercord.snowcap_stoat': 'A long, slender little hunter of the cold country, white as the snow where it snows and brown above a cream '
                                   'belly where it does not, the tip of its tail black either way. Come near and it stands up tall on its hind '
                                   'legs to look you over. It hunts rabbits: it creeps up, then breaks into a war dance of twisting hops that '
                                   'holds the rabbit spellbound, and darts in. Sometimes it dances for no reason at all. On snow it dives in '
                                   'headfirst and pops out somewhere else, and that is how it gets away when struck. Eggs or raw chicken tempt '
                                   'it and breed it; feed one and it trusts you a while, bounding along after you.',
}

WINTER = ramp("#B9C2CC", "#CFD6DE", "#E2E7EC", "#EEF2F5", "#F8FAFB")
WINTER_BELLY = ramp("#C8C6BE", "#D8D6CC", "#E6E3D8", "#F0EDE2", "#F8F6EE")
SUMMER = ramp("#4A3220", "#5E4029", "#744F33", "#8A603E", "#9E734C")
SUMMER_BELLY = ramp("#C2AE88", "#D2BF98", "#E0CEA8", "#EADBB8", "#F2E6C8")
TIP = ramp("#0E0C0C", "#1A1716", "#2A2624")
EYE, SHINE = hexc("#0A0808"), hexc("#C8CCD2")
NOSE_C, NOSE_SHINE = hexc("#18120F"), hexc("#5E5650")
EAR_IN = hexc("#D8A8A0")
PAW = ramp("#9C9890", "#B4B0A8", "#CCC8C0")

BODY = box(0, 0, 4, 4, 10)
HEAD = box(30, 0, 4, 3, 4)
SNOUT = box(48, 0, 2, 2, 2)
NOSE = box(58, 0, 1, 1, 1)
EAR = box(48, 6, 2, 2, 1)
LEG = box(30, 10, 2, 3, 2)
TAIL = box(0, 16, 2, 2, 6)
TAIP = box(18, 16, 2, 2, 3)


def underside(cv, area, belly, seed, rows):
    """The lower `rows` rows of a side face, the belly colour coming up into it in a soft, uneven line."""
    x0, y0, w, h = area
    for x in range(w):
        edge = rows - (1 if noise(x0 + x, y0, seed) > 0.6 else 0)
        for y in range(h - edge, h):
            cv.put(x0 + x, y0 + y, belly[3 if y > h - edge else 2])


def skin(winter):
    coat, belly = (WINTER, WINTER_BELLY) if winter else (SUMMER, SUMMER_BELLY)
    cv = Sheet(64, 32)
    # The coat: the back and sides, short fur stroked toward the tail; the belly pale.
    for name, area in faces(BODY):
        fur(cv, area, belly if name == "bottom" else coat, 10 + len(name), name, base=2, along_y=name in ("top", "bottom"))
    for name in ("right", "left", "front"):
        underside(cv, BODY[name], belly, 7 + len(name), 2 if name != "front" else 3)
    # The head: the coat over the crown, pale cheeks and chin, beady eyes with a gleam.
    for part, seed in ((HEAD, 2), (SNOUT, 3), (EAR, 4)):
        for name, area in faces(part):
            fur(cv, area, belly if name == "bottom" else coat, seed * 10 + len(name), name, base=2)
    for name in ("right", "left", "front"):
        underside(cv, HEAD[name], belly, 21 + len(name), 1)
    x0, y0, w, h = HEAD["front"]
    for ex in (0, 3):
        cv.put(x0 + ex, y0 + 1, EYE)
    cv.put(x0, y0, SHINE if not winter else coat[1])
    cv.put(x0 + 3, y0, SHINE if not winter else coat[1])
    for name in ("right", "left"):
        x0, y0, w, h = HEAD[name]
        cv.put(x0 + (w - 1 if name == "right" else 0), y0 + 1, EYE)
    x0, y0, w, h = SNOUT["front"]
    for x in range(w):
        cv.put(x0 + x, y0 + h - 1, belly[3])
    for name, area in faces(NOSE):
        fill(cv, area, NOSE_C)
    x0, y0, w, h = NOSE["front"]
    cv.put(x0, y0, NOSE_SHINE)
    # Ears: rosy inside, the rims a shade darker.
    x0, y0, w, h = EAR["front"]
    for x in range(w):
        cv.put(x0 + x, y0, EAR_IN)
    # Legs in the coat, small pale paws.
    for name, area in faces(LEG):
        fur(cv, area, coat, 40 + len(name), name, base=2)
    for name in ("front", "right", "left", "back"):
        x0, y0, w, h = LEG[name]
        for x in range(w):
            cv.put(x0 + x, y0 + h - 1, PAW[1] if winter else belly[2])
    fill(cv, LEG["bottom"], PAW[0])
    # The tail in the coat, then its black tip, the coat feathering a pixel into the black.
    for name, area in faces(TAIL):
        fur(cv, area, coat, 50 + len(name), name, base=2)
    for name, area in faces(TAIP):
        x0, y0, w, h = area
        for y in range(h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, TIP[1 if noise(x0 + x, y0 + y, 9) > 0.5 else 0])
    for name in ("top", "bottom", "right", "left"):
        x0, y0, w, h = TAIP[name]
        if name in ("right", "left"):
            col = x0 + (w - 1 if name == "right" else 0)
            for y in range(h):
                if noise(col, y0 + y, 11) > 0.5:
                    cv.put(col, y0 + y, mix(coat[1], TIP[2], 0.6))
        else:
            for x in range(w):
                if noise(x0 + x, y0 + h - 1, 13) > 0.5:
                    cv.put(x0 + x, y0 + h - 1, mix(coat[1], TIP[2], 0.6))
    return cv.image()


def write(g):
    g.save(skin(True), g.ASSETS / 'textures/entity/wildlife/snowcap_stoat.png')
    g.save(skin(False), g.ASSETS / 'textures/entity/wildlife/snowcap_stoat_summer.png')
    name = 'snowcap_stoat_spawn_egg'
    g.save(egg_icon(("#B9C2CC", "#E2E7EC", "#F8FAFB"), "#1A1716", name="snowcap_stoat"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/snowcap_stoat.json', {'type': 'minecraft:entity', 'pools': [], 'random_sequence': 'wildercord:entities/snowcap_stoat'})
    g.write_json(g.DATA / 'tags/block/spawns_on/snowcap_stoat.json', {'replace': False, 'values': [
        '#minecraft:animals_spawnable_on', 'minecraft:grass_block', 'minecraft:snow_block', 'minecraft:snow', 'minecraft:podzol',
        'minecraft:coarse_dirt', 'minecraft:stone']})
