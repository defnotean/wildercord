"""Black bobcat: its skin (BlackBobcatModel's 64x64 box-UV layout), spawn egg, loot, spawn ground and field-guide text."""
from item_art import Canvas, hexc, mix, ramp
from monster_art import EGG, faces, fur, shade, smooth
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.black_bobcat': 'Black Bobcat',
 'item.wildercord.black_bobcat_spawn_egg': 'Black Bobcat Spawn Egg',
 'guide.wildercord.black_bobcat.hint': 'Look for a tufted black cat in dark forests and old taigas.',
 'guide.wildercord.black_bobcat': 'A melanistic wildcat as big as a polar bear, with tufted ears, a bobbed tail and pale gold eyes. A wild one keeps to itself, hunts rabbits and chickens and turns on whatever hurts it. Offer it fish, raw or cooked: one time in three it becomes yours. A tame bobcat follows you, sits when you tell it to, joins your fights and heals on more fish. Two tame bobcats fed fish raise a kitten that is yours too.',
}

# ============================================================== the bobcat (BlackBobcatModel, 64x64)

COAT = ramp("#09080C", "#100F15", "#17161E", "#201F29", "#2B2A36", "#383746", "#4A4958")
SMOKE = ramp("#1C1B22", "#26252E", "#32313C", "#3F3E4A", "#4E4D5A")
EYE = hexc("#D6C45A")
EYE_RIM = hexc("#8E8A3A")
PUPIL = hexc("#050506")
NOSE = hexc("#2C2228")
WHISKER = hexc("#8A8A96")
EAR_SPOT = hexc("#6E6E7C")
EAR_INNER = hexc("#2E262C")

BODY = box(0, 0, 10, 8, 14)
HEAD = box(0, 22, 7, 6, 6)
MUZZLE = box(26, 22, 3, 2, 3)
RUFF = box(0, 34, 10, 3, 2)
EAR = box(40, 22, 2, 3, 1)
TUFT = box(48, 22, 1, 2, 1)
LEG = box(0, 40, 4, 8, 4)
TAIL = box(18, 40, 3, 3, 5)


def rosettes(cv, area, seed):
    """Faint darker rosettes under the black, the ghost of a spotted coat that shows only in good light."""
    x0, y0, w, h = area
    for y in range(h):
        for x in range(w):
            if smooth(x0 + x, y0 + y, seed, 2.2) > 0.74:
                c = cv.get(x0 + x, y0 + y)
                if c is not None:
                    cv.put(x0 + x, y0 + y, mix(c, COAT[0], 0.55))


def skin():
    """A black coat with a smoky underside, faint rosettes, a pale muzzle with grey whiskers, gold-green eyes with slit pupils,
    black ear tufts with a grey spot behind each ear, dark paws and a darker tip to the bob."""
    cv = Sheet(64, 64)
    for part, seed in ((BODY, 1), (HEAD, 2), (RUFF, 3), (LEG, 4), (TAIL, 5), (EAR, 6)):
        for name, area in faces(part):
            fur(cv, area, COAT, seed * 10 + len(name), name, base=2, along_y=part is BODY and name in ("top", "bottom"))
    for name, area in faces(BODY, "top", "right", "left"):
        rosettes(cv, area, 31 + len(name))
    fur(cv, BODY["bottom"], SMOKE, 41, "bottom", base=3, along_y=True)
    for name, area in faces(TUFT):
        x0, y0, w, h = area
        for y in range(h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, COAT[0])
    # A grey spot on the back of each ear and a dark inner cup.
    x0, y0, w, h = EAR["back"]
    for y in range(1, h):
        for x in range(w):
            cv.put(x0 + x, y0 + y, EAR_SPOT if y == 1 else mix(EAR_SPOT, COAT[2], 0.4))
    x0, y0, w, h = EAR["front"]
    for y in range(1, h):
        for x in range(w):
            cv.put(x0 + x, y0 + y, EAR_INNER)
    # The muzzle a shade lighter, a dark nose, grey whisker pores.
    for name, area in faces(MUZZLE):
        fur(cv, area, SMOKE, 51 + len(name), name, base=2)
    x0, y0, w, h = MUZZLE["front"]
    cv.put(x0 + 1, y0, NOSE)
    cv.put(x0, y0 + 1, WHISKER)
    cv.put(x0 + 2, y0 + 1, WHISKER)
    # Eyes: gold-green, a dark slit on the inner side, a faint pale brow line above.
    x0, y0, w, h = HEAD["front"]
    for ex, px in ((1, 2), (5, 4)):
        cv.put(x0 + ex, y0 + 2, EYE)
        cv.put(x0 + px, y0 + 2, PUPIL)
        cv.put(x0 + ex, y0 + 3, EYE_RIM)
        cv.put(x0 + ex, y0 + 1, COAT[4])
    # Cheek ruff: lighter tips at its lower edge.
    for name in ("front", "right", "left"):
        x0, y0, w, h = RUFF[name]
        for x in range(w):
            if noise(x0 + x, y0, 7) > 0.4:
                cv.put(x0 + x, y0 + h - 1, SMOKE[2])
    # Dark paws.
    for name in ("front", "back", "right", "left"):
        x0, y0, w, h = LEG[name]
        for x in range(w):
            cv.put(x0 + x, y0 + h - 1, COAT[0])
    for name, area in faces(LEG, "bottom"):
        x0, y0, w, h = area
        for y in range(h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, COAT[0])
    # The bob darkens to its tip.
    x0, y0, w, h = TAIL["back"]
    for y in range(h):
        for x in range(w):
            cv.put(x0 + x, y0 + y, COAT[0])
    return cv.image()


# ============================================================== spawn egg

EGG_TONES = ("#1A1922", "#2A2934", "#3C3B4A")
EGG_SPOT = "#C8B84E"


def egg_icon():
    dark, mid, light = (hexc(c) for c in EGG_TONES)
    outline = mix(dark, (0, 0, 0), 0.6)
    cv = Canvas()
    for y, row in enumerate(r.strip() for r in EGG.strip("\n").splitlines()):
        for x, ch in enumerate(row):
            if ch == "o":
                cv.put(x, y, outline)
            elif ch == "1":
                t = (x + y) / 26
                c = light if t < 0.42 else mid if t < 0.72 else dark
                if noise(x, y, len("black_bobcat")) > 0.82:
                    c = hexc(EGG_SPOT)
                cv.put(x, y, c)
    return cv.image()


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/wildlife/black_bobcat.png')
    name = 'black_bobcat_spawn_egg'
    g.save(egg_icon(), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/black_bobcat.json', {'type': 'minecraft:entity', 'pools': [{
        'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:leather', 'functions': [
            {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 0, 'max': 1}},
            {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting', 'count': {'type': 'minecraft:uniform', 'min': 0, 'max': 1}}]}]}],
        'random_sequence': 'wildercord:entities/black_bobcat'})
    g.write_json(g.DATA / 'tags/block/spawns_on/black_bobcat.json', {'replace': False, 'values': [
        '#minecraft:wolves_spawnable_on', 'minecraft:grass_block', 'minecraft:podzol', 'minecraft:coarse_dirt']})
