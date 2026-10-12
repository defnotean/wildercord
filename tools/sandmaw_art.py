"""Sandmaw Wyrm: its skin (SandmawWyrmModel's 128x64 box-UV layout), spawn egg, loot and field-guide text."""
from bobcat_art import egg_icon
from item_art import hexc, ramp
from monster_art import faces, smooth
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.sandmaw_wyrm': 'Sandmaw Wyrm',
 'item.wildercord.sandmaw_wyrm_spawn_egg': 'Sandmaw Wyrm Spawn Egg',
 'guide.wildercord.sandmaw_wyrm.hint': 'A furrow in the dunes that ran toward me, and then the ground began to shake.',
 'guide.wildercord.sandmaw_wyrm': 'A great ringed worm of the deserts and badlands, one of the elites of the wilds, sand-hided, '
                                  'with a ridge of spines down its back and a mouth ringed with four jaw-plates. It lies '
                                  'under the sand and feels footsteps from far off, though barely a sneak. Under the sand '
                                  'nothing can touch it; a furrow trails it as it swims after you. When the ground beneath '
                                  'you rumbles, get off it: it bursts up and flings all about it into the air. Up, it rears '
                                  'back and slams its head down along a line before it; step aside, and while its head is '
                                  'stuck in the sand your blows land half again as hard. Keep your distance and it spits a '
                                  'gout of sand that blinds and slows. Up a while, or hurt enough, it dives back under '
                                  'and the hunt begins again.',
}

HIDE = ramp("#7A5A34", "#9A7444", "#B89058", "#D0AA70", "#E4C68E")
BELLY = ramp("#A08860", "#BCA474", "#D2BC8A", "#E2D0A2", "#F0E2BC")
MAW = ramp("#2A1010", "#4A1C1A", "#6E2C26", "#94403A", "#B85A50")
TOOTH = ramp("#B0A488", "#CCC2A6", "#E2DAC2", "#F2ECDA", "#FFFBEE")
HORN = ramp("#3A2614", "#54381E", "#6E4C2A", "#8A6438", "#A67E4C")

SEGS = [box(0, 0, 12, 10, 12), box(48, 0, 11, 10, 11), box(0, 22, 10, 10, 10), box(40, 22, 9, 10, 9)]
HEAD = box(76, 22, 10, 8, 10)
JAW_SIDE = box(0, 44, 1, 6, 4)
JAW_LIP = box(12, 44, 4, 1, 4)
SPINE = box(30, 44, 2, 3, 2)

LIGHT = {"top": 1, "bottom": -2, "front": 0, "back": -1, "right": 0, "left": -1}


def hide(cv, part, seed, tones=HIDE, base=2, names=()):
    """Rough sand-scoured hide, lit from above, mottled in soft patches and freckled with grit."""
    for name, (x0, y0, w, h) in faces(part, *names):
        for y in range(h):
            for x in range(w):
                t = base + LIGHT.get(name, 0)
                s = smooth(x0 + x, y0 + y, seed, 3.0)
                if s > 0.72:
                    t -= 1
                elif s < 0.2:
                    t += 1
                if noise(x0 + x, y0 + y, seed) > 0.93:
                    t -= 1
                cv.put(x0 + x, y0 + y, tones[max(0, min(4, t))])


def ringed(cv, part, seed):
    """A segment: hide on its sides and back, a pale ridged belly-plate before, and dark creases ringing it top and
    bottom where it folds into the next."""
    hide(cv, part, seed)
    hide(cv, part, seed + 1, tones=BELLY, base=2, names=("front",))
    for name in ("front", "back", "right", "left"):
        x0, y0, w, h = part[name]
        for x in range(w):
            cv.put(x0 + x, y0, HIDE[0])
            cv.put(x0 + x, y0 + h - 1, HIDE[0])
            cv.put(x0 + x, y0 + 1, HIDE[1] if name != "front" else BELLY[1])
            if name != "front":
                # A worn band across its middle.
                if noise(x0 + x, y0, seed + 5) > 0.25:
                    cv.put(x0 + x, y0 + h // 2, HIDE[3])
        if name == "front":
            for y in range(2, h - 1, 3):
                for x in range(1, w - 1):
                    cv.put(x0 + x, y0 + y, BELLY[1])


def skin():
    cv = Sheet(128, 64)
    for i, seg in enumerate(SEGS):
        ringed(cv, seg, 10 + i * 7)

    # The head: blunt and hide-covered, its crown darker and scarred; the front is all mouth, a dark throat ringed with
    # teeth, row on row.
    hide(cv, HEAD, 50)
    x0, y0, w, h = HEAD["top"]
    for y in range(h):
        for x in range(w):
            if noise(x0 + x, y0 + y, 51) > 0.85:
                cv.put(x0 + x, y0 + y, HIDE[1])
    x0, y0, w, h = HEAD["front"]
    cx, cy = (w - 1) / 2, (h - 1) / 2
    for y in range(h):
        for x in range(w):
            d = max(abs(x - cx) / (w / 2), abs(y - cy) / (h / 2))
            if d > 0.92:
                c = HIDE[1]
            elif d > 0.75:
                c = TOOTH[3] if (x + y) % 2 == 0 else MAW[3]
            elif d > 0.5:
                c = MAW[2] if (x * 3 + y) % 4 else TOOTH[2]
            elif d > 0.25:
                c = MAW[1]
            else:
                c = MAW[0]
            cv.put(x0 + x, y0 + y, c)

    # The jaw-plates: hide outside, raw red within, a row of teeth along their tips.
    for part in (JAW_SIDE, JAW_LIP):
        hide(cv, part, 60, base=2)
        inner = ("right", "left") if part is JAW_SIDE else ("top", "bottom")
        for name, (x0, y0, w, h) in faces(part, *inner):
            for y in range(h):
                for x in range(w):
                    cv.put(x0 + x, y0 + y, MAW[2 + (noise(x0 + x, y0 + y, 61) > 0.6)])
        for name, (x0, y0, w, h) in faces(part, "front"):
            for y in range(h):
                for x in range(w):
                    cv.put(x0 + x, y0 + y, TOOTH[3 + (noise(x0 + x, y0 + y, 62) > 0.5)])
        # The teeth at the tip of the inner face.
        for name in inner:
            x0, y0, w, h = part[name]
            if name in ("right", "left"):
                for y in range(h):
                    cv.put(x0, y0 + y, TOOTH[4] if y % 2 == 0 else TOOTH[2])
            else:
                for x in range(w):
                    cv.put(x0 + x, y0, TOOTH[4] if x % 2 == 0 else TOOTH[2])

    # The spines: dark horn, paler toward their tips.
    hide(cv, SPINE, 70, tones=HORN, base=2)
    x0, y0, w, h = SPINE["top"]
    for y in range(h):
        for x in range(w):
            cv.put(x0 + x, y0 + y, HORN[4])
    return cv.image()


def _count(lo, hi):
    return {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}


def _looting(hi):
    return {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting',
            'count': {'type': 'minecraft:uniform', 'min': 0, 'max': hi}}


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/sandmaw_wyrm.png')
    name = 'sandmaw_wyrm_spawn_egg'
    g.save(egg_icon(("#9A7444", "#B89058", "#D0AA70"), "#6E2C26", name="sandmaw_wyrm"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/sandmaw_wyrm.json', {'type': 'minecraft:entity', 'pools': [
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:bone', 'functions': [_count(3, 6), _looting(2)]}]},
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:gold_ingot', 'functions': [_count(2, 4), _looting(1)]}]},
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:emerald', 'functions': [_count(1, 3)]}]},
        {'rolls': 1, 'conditions': [{'condition': 'minecraft:random_chance', 'chance': 0.35}],
         'entries': [{'type': 'minecraft:item', 'name': 'minecraft:diamond'}]}],
        'random_sequence': 'wildercord:entities/sandmaw_wyrm'})
