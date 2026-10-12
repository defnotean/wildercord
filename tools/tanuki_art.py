"""Blossom tanuki: its skin (BlossomTanukiModel's 64x32 box-UV layout), spawn egg, loot, spawn ground and field-guide text."""
from bobcat_art import egg_icon
from item_art import hexc, ramp
from monster_art import faces, smooth
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.blossom_tanuki': 'Blossom Tanuki',
 'item.wildercord.blossom_tanuki_spawn_egg': 'Blossom Tanuki Spawn Egg',
 'guide.wildercord.blossom_tanuki.hint': 'In the cherry groves, a bush that wasn\'t there a moment ago.',
 'guide.wildercord.blossom_tanuki': 'A plump little raccoon-dog of the cherry groves, grey-brown with a dark mask, a round belly '
                                    'and a big bushy ringed tail. Run at it, or let a wolf, a fox or a monster come near, and it '
                                    'slaps a cherry leaf on its head, holds dead still a moment and then, in a puff of petals, '
                                    'turns into a little blossom bush. It stays a bush until a while after the danger has gone, '
                                    'then pops back out; strike the bush and it bursts out and bolts. Under a full moon the '
                                    'tanukis sit up on their haunches and drum their bellies, pom-pom, pom-pom, all night long. '
                                    'Sweet berries and apples tempt it and breed it. Feed one and it trusts you a while and '
                                    'trots after you, and if you crouch beside it a moment it hops up and puts its leaf on your '
                                    'head: for ten seconds no one can see you.',
}

FUR = ramp("#4E443A", "#6A5D4E", "#857664", "#A0917C", "#BAAC96")
DARK = ramp("#1E1A16", "#2C2620", "#3A332B", "#4A4238", "#5C5348")
CREAM = ramp("#A89A82", "#C4B69C", "#D8CCB4", "#E8DECA", "#F4EEDE")
LEAF = ramp("#8A3E5E", "#B85C80", "#D880A0", "#EEA8C0", "#FAD0DE")
STEM = hexc("#5E7A34")
EYE, GLINT, NOSE = hexc("#120E0A"), hexc("#F6EEDC"), hexc("#141010")

BODY = box(0, 0, 6, 5, 8)
HEAD = box(28, 0, 5, 4, 4)
SNOUT = box(46, 0, 3, 2, 2)
EAR = box(56, 0, 2, 2, 1)
LEAF_BOX = box(46, 5, 4, 0, 4)
TAIL = box(0, 14, 3, 3, 7)
LEG = box(20, 14, 2, 3, 2)

LIGHT = {"top": 1, "bottom": -1, "front": 0, "back": -1, "right": 0, "left": -1}


def shade(cv, area, tones, seed, face, cell=1.8, base=2, flecks=0.88):
    """Thick fur, lit from above, in soft clumps, with darker guard hairs here and there."""
    x0, y0, w, h = area
    for y in range(h):
        for x in range(w):
            t = base + LIGHT.get(face, 0) - (1 if smooth(x0 + x, y0 + y, seed, cell) < 0.3 else 0)
            c = tones[max(0, min(4, t))]
            if noise(x0 + x, y0 + y, seed) > flecks:
                c = DARK[3 + (1 if noise(x0 + x, y0 + y, seed + 1) > 0.5 else 0)]
            cv.put(x0 + x, y0 + y, c)


def furred(cv, part, seed, tones=FUR, **kw):
    for name, area in faces(part):
        shade(cv, area, tones, seed + len(name), name, **kw)


def skin():
    cv = Sheet(64, 32)
    furred(cv, BODY, 10)
    # A dark saddle across the shoulders, and a pale round belly underneath.
    x0, y0, w, h = BODY["top"]
    for y in range(h - 3, h):
        for x in range(w):
            if noise(x0 + x, y0 + y, 12) < 0.75:
                cv.put(x0 + x, y0 + y, DARK[3])
    furred(cv, {"bottom": BODY["bottom"]}, 14, tones=CREAM, base=2, flecks=2)
    for side in ("left", "right"):
        x0, y0, w, h = BODY[side]
        for x in range(w):
            cv.put(x0 + x, y0 + h - 1, CREAM[2])
    furred(cv, HEAD, 20)
    # The face: a pale brow, a dark mask across the eyes, pale cheeks below.
    x0, y0, w, h = HEAD["front"]
    for x in range(w):
        cv.put(x0 + x, y0, CREAM[3] if x != 2 else FUR[3])
        cv.put(x0 + x, y0 + 1, DARK[1] if x != 2 else FUR[2])
        cv.put(x0 + x, y0 + 2, DARK[2] if x in (0, 1, 3, 4) else CREAM[2])
        cv.put(x0 + x, y0 + 3, CREAM[3])
    for ex in (1, 3):
        cv.put(x0 + ex, y0 + 1, EYE)
    cv.put(x0 + 0, y0 + 1, DARK[1])
    cv.put(x0 + 1, y0 + 2, DARK[0])
    cv.put(x0 + 3, y0 + 2, DARK[0])
    # A glint in each eye: one pixel in, so it reads as looking forward.
    cv.put(x0 + 1, y0 + 1, EYE)
    furred(cv, SNOUT, 30, tones=CREAM, flecks=2)
    x0, y0, w, h = SNOUT["front"]
    cv.put(x0 + 1, y0, NOSE)
    cv.put(x0 + 0, y0, CREAM[2])
    cv.put(x0 + 2, y0, CREAM[2])
    x0, y0, w, h = SNOUT["top"]
    for x in range(w):
        cv.put(x0 + x, y0 + h - 1, NOSE if x == 1 else CREAM[3])
    # The ears: dark rimmed, pale inside.
    furred(cv, EAR, 40, tones=DARK, base=2, flecks=2)
    x0, y0, w, h = EAR["front"]
    for x in range(w):
        cv.put(x0 + x, y0 + 1, CREAM[1])
    # The leaf: a cherry leaf, pink, a vein down the middle and a little green stem.
    for name in ("top", "bottom"):
        x0, y0, w, h = LEAF_BOX[name]
        for y in range(h):
            for x in range(w):
                edge = (x in (0, w - 1)) and (y in (0, h - 1))
                if edge and not (x == 0 and y == h - 1):
                    continue
                c = LEAF[3 if (x + y) % 3 else 2]
                if x == y or x == y + 1:
                    c = LEAF[1]
                cv.put(x0 + x, y0 + y, c)
        cv.put(x0, y0 + h - 1, STEM)
    # The tail: big and bushy, ringed dark, dark at the tip.
    for name, area in faces(TAIL):
        x0, y0, w, h = area
        for y in range(h):
            for x in range(w):
                along = x if name in ("left", "right", "top", "bottom") else None
                if name in ("top", "bottom"):
                    along = y
                ring = along is not None and along % 3 == 2
                tones = DARK if ring else FUR
                t = (1 if ring else 3) + LIGHT.get(name, 0)
                c = tones[max(0, min(4, t))]
                if noise(x0 + x, y0 + y, 50) > 0.85:
                    c = DARK[3]
                cv.put(x0 + x, y0 + y, c)
    x0, y0, w, h = TAIL["back"]
    for y in range(h):
        for x in range(w):
            cv.put(x0 + x, y0 + y, DARK[1 + (x + y) % 2])
    furred(cv, LEG, 60, tones=DARK, base=2, flecks=2)
    return cv.image()


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/wildlife/blossom_tanuki.png')
    name = 'blossom_tanuki_spawn_egg'
    g.save(egg_icon(("#4E443A", "#857664", "#BAAC96"), "#E8A0B8", name="blossom_tanuki"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/blossom_tanuki.json', {'type': 'minecraft:entity', 'pools': [],
        'random_sequence': 'wildercord:entities/blossom_tanuki'})
    g.write_json(g.DATA / 'tags/block/spawns_on/blossom_tanuki.json', {'replace': False, 'values': [
        'minecraft:grass_block', 'minecraft:dirt', 'minecraft:coarse_dirt', 'minecraft:podzol', 'minecraft:moss_block']})
