"""Brookbill duck: its skin and its duckling's (BrookbillDuckModel's 64x32 box-UV layout), spawn egg, loot, spawn ground
and field-guide text."""
from bobcat_art import egg_icon
from item_art import hexc, ramp
from monster_art import faces, smooth
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.brookbill_duck': 'Brookbill Duck',
 'item.wildercord.brookbill_duck_spawn_egg': 'Brookbill Duck Spawn Egg',
 'guide.wildercord.brookbill_duck.hint': 'On the river, a line of little yellow shapes paddled along behind a brown one.',
 'guide.wildercord.brookbill_duck': 'A plump speckled brown duck of the rivers, ponds and swamps, with a bright blue patch on '
                                    'each wing, an orange bill and orange feet. It floats high on the water and paddles about, '
                                    'and now and then up-ends, tail to the sky, to dabble for something on the bottom. Climbing '
                                    'out it shakes itself off; out in the rain it flaps its wings for the joy of it. Its '
                                    'ducklings, yellow balls of fluff with a brown stripe down the back, follow it in a single '
                                    'file line. A duckling hatched by breeding imprints on the first face it sees, so whoever '
                                    'bred it becomes its mother and has a line of ducklings at their heels until they grow up. '
                                    'Feeding a duckling seeds does the same. Seeds tempt and breed the grown ducks.',
}

BROWN = ramp("#4A3420", "#634830", "#7E5E40", "#9A7754", "#B6936C")
BUFF = ramp("#7A6044", "#957857", "#AE916C", "#C6AA84", "#DCC29E")
DARK = ramp("#2A1E14", "#3A2A1C", "#4C3826", "#5E4632", "#70563E")
SPECULUM = ramp("#142A6A", "#1E3C8E", "#2C52B0", "#4A70CC", "#6E92E0")
BILL = ramp("#7A3E10", "#A0561A", "#C47024", "#E08C34", "#F2A64C")
FOOT = ramp("#8A4A14", "#AA601C", "#C87A28", "#E09438", "#F0AE52")
DOWN = ramp("#B89A30", "#D2B43C", "#E6CA50", "#F2DC6C", "#FAEA92")
STRIPE = ramp("#4A3A1C", "#5E4A26", "#725C32", "#86703E", "#9A844C")
EYE, WHITE = hexc("#0A0806"), hexc("#F4F0E6")

BODY = box(0, 0, 6, 5, 8)
NECK = box(28, 0, 2, 4, 2)
HEAD = box(36, 0, 4, 4, 5)
BILL_BOX = box(0, 14, 3, 1, 3)
TAIL = box(16, 14, 4, 1, 3)
LEG = box(32, 14, 1, 2, 1)
FOOT_BOX = box(40, 14, 2, 1, 2)
WING = box(0, 19, 1, 4, 6)

LIGHT = {"top": 1, "bottom": -1, "front": 0, "back": -1, "right": 0, "left": -1}


def speckled(cv, part, seed, tones, base=2, names=(), dots=0.82):
    """Brown feathers, lit from above, each one edged paler so the whole coat looks scalloped and speckled."""
    for name, (x0, y0, w, h) in faces(part, *names):
        for y in range(h):
            for x in range(w):
                t = base + LIGHT.get(name, 0)
                if smooth(x0 + x, y0 + y, seed, 1.6) > 0.7:
                    t += 1
                n = noise(x0 + x, y0 + y, seed)
                if n > dots:
                    t -= 2
                elif n < 0.1:
                    t += 1
                cv.put(x0 + x, y0 + y, tones[max(0, min(4, t))])


def plain(cv, part, tones, base=2, names=()):
    for name, (x0, y0, w, h) in faces(part, *names):
        for y in range(h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, tones[max(0, min(4, base + LIGHT.get(name, 0)))])


def eyes(cv, tones, stripe=True):
    """A dark eye on each side of the head, with a dark stripe running back through it on the grown duck."""
    for name in ("right", "left"):
        sx, sy, sw, sh = HEAD[name]
        ex = 1 if name == "right" else sw - 2
        if stripe:
            for x in range(sw):
                cv.put(sx + x, sy + 2, tones[0])
        cv.put(sx + ex, sy + 1, EYE)
        cv.put(sx + ex + (1 if name == "right" else -1), sy + 1, WHITE if not stripe else tones[1])


def bill(cv):
    """An orange bill, darker blotched along the top, with a dark nail at the tip."""
    plain(cv, BILL_BOX, BILL)
    x0, y0, w, h = BILL_BOX["top"]
    for y in range(h):
        for x in range(w):
            if noise(x0 + x, y0 + y, 70) > 0.6:
                cv.put(x0 + x, y0 + y, BILL[1])
    x0, y0, w, h = BILL_BOX["front"]
    cv.put(x0 + w // 2, y0, DARK[1])


def skin():
    cv = Sheet(64, 32)
    speckled(cv, BODY, 10, BROWN)
    speckled(cv, BODY, 12, BUFF, names=("bottom", "front"), dots=0.88)
    speckled(cv, NECK, 14, BROWN)
    # A buff face with a dark crown and a dark stripe back through the eye.
    speckled(cv, HEAD, 20, BUFF, dots=0.9)
    speckled(cv, HEAD, 22, DARK, names=("top",), dots=0.95)
    for name in ("right", "left", "front", "back"):
        x0, y0, w, h = HEAD[name]
        for x in range(w):
            cv.put(x0 + x, y0, DARK[2])
    eyes(cv, DARK)
    bill(cv)
    speckled(cv, TAIL, 30, BROWN, base=1)
    x0, y0, w, h = TAIL["top"]
    for x in range(w):
        cv.put(x0 + x, y0 + h - 1, BUFF[3])
    # Wings: speckled brown, a band of shining blue near the back edge, edged white either side.
    speckled(cv, WING, 40, BROWN, base=1)
    for name in ("right", "left"):
        x0, y0, w, h = WING[name]
        for y in range(1, h):
            cv.put(x0 + w - 3, y0 + y, WHITE)
            cv.put(x0 + w - 2, y0 + y, SPECULUM[2 + (y % 2)])
            cv.put(x0 + w - 1, y0 + y, WHITE)
    plain(cv, LEG, FOOT)
    plain(cv, FOOT_BOX, FOOT)
    return cv.image()


def duckling():
    """A duckling: yellow down all over, a brown stripe down its back and over its crown, a dark line through the eye."""
    cv = Sheet(64, 32)
    for part, seed in ((BODY, 50), (NECK, 51), (HEAD, 52), (WING, 53), (TAIL, 54)):
        for name, (x0, y0, w, h) in faces(part):
            for y in range(h):
                for x in range(w):
                    t = 2 + LIGHT.get(name, 0) + (1 if smooth(x0 + x, y0 + y, seed, 1.3) > 0.62 else 0)
                    cv.put(x0 + x, y0 + y, DOWN[max(0, min(4, t))])
    # The brown back: the whole top of the body and the outside of the wings, darker along the spine.
    for name, (x0, y0, w, h) in faces(BODY, "top", "back"):
        for y in range(h):
            for x in range(w):
                t = 2 + (1 if smooth(x0 + x, y0 + y, 55, 1.3) > 0.62 else 0)
                cv.put(x0 + x, y0 + y, STRIPE[t])
    for name in ("right", "left"):
        x0, y0, w, h = BODY[name]
        for x in range(w):
            cv.put(x0 + x, y0, STRIPE[3])
        x0, y0, w, h = WING[name]
        for y in range(h // 2):
            for x in range(w):
                cv.put(x0 + x, y0 + y, STRIPE[3])
    for name, (x0, y0, w, h) in faces(HEAD, "top", "back"):
        for y in range(h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, STRIPE[2])
    for name in ("right", "left"):
        sx, sy, sw, sh = HEAD[name]
        ex = 1 if name == "right" else sw - 2
        for x in range(sw):
            cv.put(sx + x, sy, STRIPE[2])
        cv.put(sx + ex, sy + 1, EYE)
        cv.put(sx + ex + (1 if name == "right" else -1), sy + 1, STRIPE[3])
    plain(cv, BILL_BOX, ramp("#3A3020", "#504430", "#685A40", "#806E50", "#968460"))
    plain(cv, LEG, STRIPE)
    plain(cv, FOOT_BOX, STRIPE)
    return cv.image()


def _count(lo, hi):
    return {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/wildlife/brookbill_duck.png')
    g.save(duckling(), g.ASSETS / 'textures/entity/wildlife/brookbill_duckling.png')
    name = 'brookbill_duck_spawn_egg'
    g.save(egg_icon(("#634830", "#7E5E40", "#9A7754"), "#2C52B0", name="brookbill_duck"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/brookbill_duck.json', {'type': 'minecraft:entity', 'pools': [
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:feather', 'functions': [_count(0, 2)]}]},
        {'rolls': 1, 'conditions': [{'condition': 'minecraft:random_chance', 'chance': 0.5}],
         'entries': [{'type': 'minecraft:item', 'name': 'minecraft:chicken'}]}],
        'random_sequence': 'wildercord:entities/brookbill_duck'})
    g.write_json(g.DATA / 'tags/block/spawns_on/brookbill_duck.json', {'replace': False, 'values': [
        'minecraft:grass_block', 'minecraft:dirt', 'minecraft:mud', 'minecraft:sand', 'minecraft:gravel', 'minecraft:clay',
        'minecraft:podzol']})
