"""Bloomclaw Mantis: its skin (BloomclawMantisModel's 64x64 box-UV layout), spawn egg, loot and field-guide text."""
from bobcat_art import egg_icon
from item_art import hexc, ramp
from monster_art import faces, smooth
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.bloomclaw_mantis': 'Bloomclaw Mantis',
 'item.wildercord.bloomclaw_mantis_spawn_egg': 'Bloomclaw Mantis Spawn Egg',
 'guide.wildercord.bloomclaw_mantis.hint': 'Among the flowers, one blossom that turned its head to watch me pass.',
 'guide.wildercord.bloomclaw_mantis': 'An orchid mantis of the flower forests, meadows and plains, white flushed with pink, '
                                      'its walking legs lobed like petals, so that perched among the flowers it passes for '
                                      'one more blossom, rocking gently in the breeze. Whoever comes near it notices, unless '
                                      'they come sneaking, and then only from close by; it turns its head to them and creeps '
                                      'after them. Within reach it rears in its threat display, forelegs raised and wings '
                                      'flared wide, then its forelegs snap out. Caught, you are held fast before it and '
                                      'nibbled until it lets go, but one good hard blow breaks its grip and leaves it '
                                      'staggering. Step aside from the strike and it stumbles. Staggering or stumbling, it '
                                      'takes your blows half again as hard.',
}

PETAL = ramp("#C890A8", "#DEAEC2", "#EECAD8", "#F8E0EA", "#FFF4F8")
BLUSH = ramp("#A85A7C", "#C47494", "#DA90AC", "#EAAEC4", "#F6CCDA")
GREEN = ramp("#6A8A4E", "#86A664", "#A2C07C", "#BCD694", "#D4E8AE")
EYE = ramp("#6A2A40", "#8E3A56", "#B4506E", "#D06A88", "#E890A8")
SPINE = hexc("#8A4A64")

ABDOMEN = box(0, 0, 5, 3, 8)
THORAX = box(30, 0, 2, 2, 7)
HEAD = box(0, 14, 4, 3, 2)
EYEBOX = box(14, 14, 1, 1, 1)
CREST = box(20, 14, 2, 1, 1)
WING = box(30, 12, 3, 0, 7)
FEMUR = box(0, 22, 1, 5, 1)
TIBIA = box(6, 22, 1, 4, 1)
LEG = box(0, 30, 1, 7, 1)
LOBE = box(6, 30, 3, 2, 1)

LIGHT = {"top": 1, "bottom": -1, "front": 0, "back": -1, "right": 0, "left": -1}


def shell(cv, part, seed, tones=PETAL, base=2, names=()):
    """Smooth waxy white, lit from above, in soft pink-flushed patches."""
    for name, (x0, y0, w, h) in faces(part, *names):
        for y in range(h):
            for x in range(w):
                t = base + LIGHT.get(name, 0)
                if smooth(x0 + x, y0 + y, seed, 2.6) > 0.7:
                    t -= 1
                if noise(x0 + x, y0 + y, seed) > 0.95:
                    t += 1
                cv.put(x0 + x, y0 + y, tones[max(0, min(4, t))])


def skin():
    cv = Sheet(64, 64)
    # The abdomen: broad and flat, white at its middle, flushing pink toward its lobed edges, with faint green banding
    # beneath, where a flower's sepals would be.
    shell(cv, ABDOMEN, 10)
    x0, y0, w, h = ABDOMEN["top"]
    for y in range(h):
        for x in range(w):
            edge = min(x, w - 1 - x)
            cv.put(x0 + x, y0 + y, (BLUSH[3] if edge == 0 else PETAL[3 if edge == 1 else 4]) if y % 3 else BLUSH[3 - (edge == 2)])
    for name in ("right", "left"):
        x0, y0, w, h = ABDOMEN[name]
        for x in range(w):
            if x % 2 == 0:
                cv.put(x0 + x, y0 + h - 1, BLUSH[2])
    shell(cv, ABDOMEN, 12, tones=GREEN, base=3, names=("bottom",))

    # The thorax: long and thin, a pink seam down its back.
    shell(cv, THORAX, 20)
    x0, y0, w, h = THORAX["top"]
    for y in range(h):
        cv.put(x0 + (y % 2), y0 + y, BLUSH[3])

    # The head: a little triangle, pale, a blush at its brow and a green mouth below; great pink eyes at its corners.
    shell(cv, HEAD, 30, base=3)
    x0, y0, w, h = HEAD["front"]
    for x in range(w):
        cv.put(x0 + x, y0, BLUSH[3])
    cv.put(x0 + 1, y0 + h - 1, GREEN[2])
    cv.put(x0 + 2, y0 + h - 1, GREEN[2])
    for name, (x0, y0, w, h) in faces(EYEBOX):
        cv.put(x0, y0, EYE[4] if name == "top" else EYE[2])
    x0, y0, w, h = EYEBOX["front"]
    cv.put(x0, y0, EYE[0])
    shell(cv, CREST, 34, tones=GREEN, base=2)

    # The wings: pale and pearly, veined in pink, flushed at their tips.
    for name, (x0, y0, w, h) in faces(WING):
        for y in range(h):
            for x in range(w):
                vein = x == 1 or (y % 3 == 2 and x != 1)
                tip = y >= h - 2
                c = BLUSH[3] if vein else (BLUSH[4] if tip else PETAL[4 if name == "top" else 3])
                cv.put(x0 + x, y0 + y, c)

    # The forelegs: pale, a row of dark spines down their inner edges.
    shell(cv, FEMUR, 40, base=3)
    shell(cv, TIBIA, 42, base=3)
    for part in (FEMUR, TIBIA):
        x0, y0, w, h = part["back"]
        for y in range(h):
            cv.put(x0, y0 + y, SPINE if y % 2 == 0 else PETAL[2])
    x0, y0, w, h = TIBIA["front"]
    cv.put(x0, y0 + h - 1, SPINE)

    # The walking legs: thin, faintly green at their feet; their lobes are petals, white flushing pink to the edge.
    shell(cv, LEG, 50, base=3)
    for name, (x0, y0, w, h) in faces(LEG, "front", "back", "right", "left"):
        cv.put(x0, y0 + h - 1, GREEN[2])
        cv.put(x0, y0 + h - 2, GREEN[3])
    for name, (x0, y0, w, h) in faces(LOBE):
        for y in range(h):
            for x in range(w):
                edge = x == 0 or x == w - 1 or y == h - 1
                cv.put(x0 + x, y0 + y, BLUSH[3] if edge else PETAL[4])
    return cv.image()


def _count(lo, hi):
    return {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}


def _looting(hi):
    return {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting',
            'count': {'type': 'minecraft:uniform', 'min': 0, 'max': hi}}


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/bloomclaw_mantis.png')
    name = 'bloomclaw_mantis_spawn_egg'
    g.save(egg_icon(("#DEAEC2", "#F8E0EA", "#FFF4F8"), "#C47494", name="bloomclaw_mantis"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/bloomclaw_mantis.json', {'type': 'minecraft:entity', 'pools': [
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:pink_petals', 'functions': [_count(1, 3), _looting(1)]}]},
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:pink_dye', 'functions': [_count(0, 1)]}]}],
        'random_sequence': 'wildercord:entities/bloomclaw_mantis'})
