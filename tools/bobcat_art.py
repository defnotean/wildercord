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

COAT = ramp("#0B0A0F", "#131219", "#1B1A23", "#25242F", "#31303D", "#3F3E4D", "#52515F")
SMOKE = ramp("#2A2932", "#35343F", "#42414D", "#51505D", "#62616E")
EYE = hexc("#E8CF5C")
EYE_DEEP = hexc("#B8962E")
PUPIL = hexc("#07070A")
SHINE = hexc("#FFFFFF")
NOSE = hexc("#8A5562")
BEAN = hexc("#6E4250")
WHISKER = hexc("#8C8C98")
EAR_SPOT = hexc("#7A7A88")
EAR_INNER = hexc("#5A3F4A")

BODY = box(0, 0, 12, 9, 13)
HEAD = box(0, 23, 12, 10, 9)
CHEEKS = box(0, 42, 15, 4, 4)
MUZZLE = box(42, 23, 4, 3, 2)
CHEST = box(42, 28, 9, 7, 2)
LEG = box(0, 50, 5, 5, 5)
PAW = box(20, 50, 6, 2, 6)
TAIL = box(44, 50, 4, 4, 4)
EAR = box(50, 0, 4, 3, 2)
EAR_TIP = box(50, 5, 2, 2, 1)
TUFT = box(56, 5, 1, 2, 1)


def rosettes(cv, area, seed, coat=COAT):
    """Faint darker rosettes under the black, the ghost of a spotted coat that shows only in good light."""
    x0, y0, w, h = area
    for y in range(h):
        for x in range(w):
            if smooth(x0 + x, y0 + y, seed, 2.2) > 0.74:
                c = cv.get(x0 + x, y0 + y)
                if c is not None:
                    cv.put(x0 + x, y0 + y, mix(c, coat[0], 0.55))


def fill(cv, area, colour):
    x0, y0, w, h = area
    for y in range(h):
        for x in range(w):
            cv.put(x0 + x, y0 + y, colour)


def eye(cv, x0, y0, mirror, iris=EYE, deep=EYE_DEEP):
    """A big round gold eye: a wide dark pupil (a kitten's, not a hunter's slit) with a white glint."""
    rows = ("IIi", "IWP", "iPP")
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            px = x0 + (2 - x if mirror else x)
            cv.put(px, y0 + y, {"I": iris, "i": deep, "W": SHINE, "P": PUPIL}[ch])


BOBCAT = dict(coat=COAT, smoke=SMOKE, eye=EYE, eye_deep=EYE_DEEP, nose=NOSE, bean=BEAN, whisker=WHISKER, ear_spot=EAR_SPOT,
              ear_inner=EAR_INNER, tuft=COAT[0])


def skin(palette=None, sheet=False):
    """A soft black coat with a smoky chest, cheeks and muzzle, faint rosettes, big round gold eyes with glints, a rosy nose,
    long black ear tufts over rosy ear cups with a grey spot behind, chunky dark paws with rosy toe beans and a fluffy bob.
    Its kin (tools/predator_art.py) pass a palette of their own over the bobcat's, and take the sheet back to finish."""
    p = {**BOBCAT, **(palette or {})}
    COAT, SMOKE, NOSE, BEAN, WHISKER, EAR_SPOT, EAR_INNER = (p[k] for k in ("coat", "smoke", "nose", "bean", "whisker", "ear_spot", "ear_inner"))
    cv = Sheet(64, 64)
    for part, seed in ((BODY, 1), (HEAD, 2), (LEG, 4), (PAW, 7), (TAIL, 5), (EAR, 6), (EAR_TIP, 8)):
        for name, area in faces(part):
            fur(cv, area, COAT, seed * 10 + len(name), name, base=3, along_y=part is BODY and name in ("top", "bottom"))
    for name, area in faces(BODY, "top", "right", "left") if p.get("rosettes", True) else ():
        rosettes(cv, area, 31 + len(name), COAT)
    fur(cv, BODY["bottom"], SMOKE, 41, "bottom", base=3, along_y=True)
    for part, seed in ((CHEST, 9), (MUZZLE, 11)):
        for name, area in faces(part):
            fur(cv, area, SMOKE, seed * 10 + len(name), name, base=2)
    for name, area in faces(CHEEKS):
        fur(cv, area, COAT, 30 + len(name), name, base=3)
    # Fluffy lighter tips along the lower edge of the cheeks and chest.
    for part in (CHEEKS, CHEST):
        for name in ("front", "right", "left"):
            x0, y0, w, h = part[name]
            for x in range(w):
                if noise(x0 + x, y0, 7) > 0.35:
                    cv.put(x0 + x, y0 + h - 1, SMOKE[4])
    # The face: big eyes low on the head, a lighter brow tick above each.
    x0, y0, w, h = HEAD["front"]
    eye(cv, x0 + 1, y0 + 4, False, p["eye"], p["eye_deep"])
    eye(cv, x0 + 8, y0 + 4, True, p["eye"], p["eye_deep"])
    cv.put(x0 + 2, y0 + 3, COAT[6])
    cv.put(x0 + 9, y0 + 3, COAT[6])
    for x in range(4, 8):
        cv.put(x0 + x, y0 + 7, SMOKE[1])
    # The muzzle: a small rosy nose on top, a tiny mouth line and whisker pores.
    x0, y0, w, h = MUZZLE["front"]
    cv.put(x0 + 1, y0, NOSE)
    cv.put(x0 + 2, y0, NOSE)
    cv.put(x0 + 1, y0 + 2, SMOKE[0])
    cv.put(x0 + 2, y0 + 2, SMOKE[0])
    cv.put(x0, y0 + 1, WHISKER)
    cv.put(x0 + 3, y0 + 1, WHISKER)
    x0, y0, w, h = MUZZLE["top"]
    cv.put(x0 + 1, y0 + h - 1, NOSE)
    cv.put(x0 + 2, y0 + h - 1, NOSE)
    for name in ("right", "left"):
        x0, y0, w, h = CHEEKS[name]
        cv.put(x0 + 1, y0 + 1, WHISKER)
        cv.put(x0 + 2, y0 + 2, WHISKER)
    # Ears: rosy inner cup, a grey spot behind, black tufts.
    x0, y0, w, h = EAR["front"]
    for y in range(h):
        for x in range(1, w - 1):
            cv.put(x0 + x, y0 + y, EAR_INNER)
    x0, y0, w, h = EAR_TIP["front"]
    for x in range(w):
        cv.put(x0 + x, y0 + h - 1, mix(EAR_INNER, COAT[2], 0.4))
    x0, y0, w, h = EAR["back"]
    for y in range(1, h - 1):
        for x in range(w):
            cv.put(x0 + x, y0 + y, EAR_SPOT if y == 1 else mix(EAR_SPOT, COAT[2], 0.45))
    for name, area in faces(TUFT):
        fill(cv, area, p["tuft"])
    # Big paws: darker toes split into three, rosy beans beneath.
    for name in ("front", "right", "left", "back"):
        x0, y0, w, h = PAW[name]
        for x in range(w):
            cv.put(x0 + x, y0, COAT[2])
        if name == "front":
            for x in (1, 3, 5):
                if x < w:
                    cv.put(x0 + x, y0 + h - 1, COAT[0])
    x0, y0, w, h = PAW["bottom"]
    fill(cv, (x0, y0, w, h), COAT[1])
    for bx, by in ((1, 0), (2, 0), (3, 0), (4, 0)):
        cv.put(x0 + bx, y0 + by + 1 if bx in (1, 4) else y0 + by, BEAN)
    for bx in range(2, 4):
        for by in range(3, 5):
            cv.put(x0 + bx, y0 + by, BEAN)
    # A fluffy bob: lighter fluff underneath, a black tip.
    fur(cv, TAIL["bottom"], SMOKE, 61, "bottom", base=3)
    fill(cv, TAIL["back"], COAT[0])
    x0, y0, w, h = TAIL["back"]
    for x in range(1, w - 1):
        cv.put(x0 + x, y0 + h - 1, SMOKE[2])
    return cv if sheet else cv.image()


# ============================================================== spawn egg

EGG_TONES = ("#1A1922", "#2A2934", "#3C3B4A")
EGG_SPOT = "#C8B84E"


def egg_icon(tones=EGG_TONES, spot=EGG_SPOT, name="black_bobcat"):
    dark, mid, light = (hexc(c) for c in tones)
    outline = mix(dark, (0, 0, 0), 0.6)
    cv = Canvas()
    for y, row in enumerate(r.strip() for r in EGG.strip("\n").splitlines()):
        for x, ch in enumerate(row):
            if ch == "o":
                cv.put(x, y, outline)
            elif ch == "1":
                t = (x + y) / 26
                c = light if t < 0.42 else mid if t < 0.72 else dark
                if noise(x, y, len(name)) > 0.82:
                    c = hexc(spot)
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
