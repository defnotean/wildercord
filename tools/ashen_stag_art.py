"""The Ashen Stag: a great stag burnt black, bedded in the ash of a burnt glade; its skin, the glow of its ember cracks, eyes and
antler tips, and its text."""
import numpy as np
from PIL import Image
from serpent_art import mixc, paint_box

LANG = {
    'entity.wildercord.ashen_stag': 'Ashen Stag',
    'guide.wildercord.ashen_stag': 'A great stag burnt black, embers glowing in the cracks of its hide and its antlers still '
                                   'smouldering, bedded in the ash before the stump of the tree that burned. It gets up when someone '
                                   'comes far enough into its glade, and runs off anything hostile that strays in. Its brow turns blows; '
                                   'its flanks and back do not, and a charge that ends in a trunk leaves it stuck there. Wounded, it '
                                   'charges again and again, leaving the ash burning in its tracks.',
    'guide.wildercord.ashen_stag.hint': 'A burnt clearing in a forest or taiga, ringed by dead trees, a smouldering stump at its heart.',
    'boss.wildercord.ashen_stag.status': '%s — %s',
}
for name, text in {
    'bellow': 'Ashen Stag bellows', 'breath': 'Ashen Stag breathes', 'rise': 'Ashen Stag gets up', 'snort': 'Ashen Stag snorts',
    'step': 'Hooves fall', 'gore': 'Antlers gore', 'kick': 'Ashen Stag kicks', 'stomp': 'Hooves crash down', 'paw': 'Ashen Stag paws the ash',
    'crash': 'Antlers crash into wood', 'strain': 'Ashen Stag strains', 'bound': 'Ashen Stag bounds', 'hurt': 'Ashen Stag hurts',
    'death': 'Ashen Stag falls', 'smoulder': 'Embers smoulder',
}.items():
    LANG['subtitles.wildercord.kit.ashen_stag.' + name] = text
LANG['block.wildercord.cinder_antler'] = 'Cinder Antler'
LANG['message.wildercord.cinder_antler.take'] = 'The antler is warm in your hand. Run, and the first thing you strike will be gored.'
LANG['message.wildercord.cinder_antler.bound'] = 'The antler already runs with you.'
LANG['message.wildercord.cinder_antler.guarded'] = 'The Ashen Stag still keeps its glade.'
LANG['message.wildercord.cinder_antler.gore'] = 'Gored!'
# What the bar says it is doing, the way a Master's bar names its cut.
for name, text in {'watch': 'Watching', 'gore': 'Goring', 'kick': 'Kicking', 'stomp': 'Rearing', 'charge': 'Charging',
                   'bound': 'Bounding', 'recover': 'Spent', 'stuck': 'Stuck fast', 'rush': 'Cinder Rush'}.items():
    LANG['boss.wildercord.ashen_stag.move.' + name] = text

# Charred bark-black hide, grey ash, white ash about the muzzle and belly, burnt antler, embers.
CHAR, CHAR_DARK, CHAR_GREY = (44, 38, 36), (22, 19, 18), (78, 72, 68)
ASH, ASH_WHITE = (150, 144, 138), (206, 200, 192)
ANTLER, ANTLER_DARK, HOOF = (92, 74, 60), (52, 40, 32), (30, 26, 24)
EMBER, EMBER_HOT, EMBER_DIM = (232, 98, 28), (255, 200, 90), (120, 40, 12)
# Where cracks run, as (corner, size) patches on the skin: the body's sides and back, the neck, the haunches and forelegs.
CRACKED = [((0, 24), (24, 10)), ((34, 24), (24, 10)), ((24, 0), (10, 24)), ((24, 24), (10, 10)), ((58, 24), (10, 10)),
           ((6, 40), (5, 13)), ((17, 40), (5, 13)), ((12, 61), (18, 9)), ((0, 59), (12, 8))]
# The eyes, on the sides of the head (east and west faces), and the antler tips.
# (each side face meets the front face at one edge, so the eyes sit a couple of pixels in from it).
EYES = ((30, 44), (31, 44), (40, 44), (41, 44))


def cracks(rng):
    """The ember cracks: jagged runs across each cracked patch, a few to a patch, each pixel with how hot it burns (0-1)."""
    out = {}
    for (x0, y0), (w, h) in CRACKED:
        for _ in range(max(1, w * h // 28)):
            x, y = int(rng.integers(0, w)), int(rng.integers(0, h))
            heat = 0.6 + 0.4 * rng.random()
            for _ in range(int(rng.integers(5, 12))):
                out[(x0 + x, y0 + y)] = max(out.get((x0 + x, y0 + y), 0), heat)
                if rng.random() < 0.6:
                    y += 1 if rng.random() < 0.7 else -1
                else:
                    x += 1 if rng.random() < 0.5 else -1
                x, y = max(0, min(w - 1, x)), max(0, min(h - 1, y))
                heat *= 0.9
    return out


def tips():
    """The antler pixels that smoulder: the top of every tine and fork."""
    out = []
    for tu, h in ((80, 4), (88, 5)):
        out += [(tu + 1, 0)] + [(tu + c, 1 + r) for c in range(4) for r in range(2)]
    return out


def skin():
    rng = np.random.default_rng(20261011)
    px = np.zeros((128, 128, 3))
    alpha = np.zeros((128, 128))
    boxes = [  # corner, size (w, h, d), top, bottom, side fade
        ((0, 0), (10, 10, 24), CHAR, ASH_WHITE, (CHAR, CHAR_GREY)),          # body: black along the back, ash under the belly
        ((0, 34), (5, 13, 6), CHAR, CHAR_DARK, (CHAR, CHAR_DARK)),           # neck
        ((24, 34), (6, 6, 9), CHAR_DARK, ASH, (CHAR, CHAR_GREY)),            # head
        ((56, 34), (4, 4, 4), ASH, ASH_WHITE, (ASH, ASH_WHITE)),             # muzzle, gone white with ash
        ((72, 34), (2, 4, 1), CHAR, CHAR, (CHAR_DARK, CHAR_GREY)),           # ears
        ((64, 56), (6, 8, 2), CHAR_GREY, ASH, (CHAR_GREY, ASH)),             # the shaggy ruff at its throat
        ((0, 56), (3, 8, 3), CHAR, CHAR, (CHAR, CHAR_DARK)),                 # forearms
        ((12, 56), (4, 9, 5), CHAR, CHAR, (CHAR, CHAR_DARK)),                # haunches
        ((32, 56), (2, 6, 2), CHAR_DARK, CHAR_DARK, (CHAR_DARK, CHAR_GREY)), # shins
        ((40, 56), (3, 2, 3), HOOF, HOOF, (HOOF, CHAR_DARK)),                # hooves
        ((52, 56), (3, 4, 2), ASH_WHITE, ASH, (CHAR, ASH_WHITE)),            # tail, white beneath
        ((68, 0), (2, 10, 2), ANTLER, ANTLER_DARK, (ANTLER, ANTLER_DARK)),   # antler beams
        ((80, 0), (1, 4, 1), ANTLER, ANTLER, (ANTLER, ANTLER_DARK)),         # tines
        ((88, 0), (1, 5, 1), ANTLER, ANTLER, (ANTLER, ANTLER_DARK)),         # forks
    ]
    for (tu, tv), (w, h, d), top, bottom, side in boxes:
        paint_box(px, tu, tv, w, h, d, top, bottom, side, rng, cell=4)
        alpha[tv:tv + d + h, tu:tu + 2 * (d + w)] = 255
    # Ash dusted unevenly over everything that is black.
    for (x0, y0), (w, h) in CRACKED:
        dust = rng.random((h, w)) < 0.12
        px[y0:y0 + h, x0:x0 + w][dust] = mixc(px[y0:y0 + h, x0:x0 + w][dust], ASH, 0.45)
    for (x, y), heat in cracks(np.random.default_rng(7)).items():
        px[y, x] = mixc(EMBER_DIM, EMBER, heat)
    for x, y in tips():
        px[y, x] = EMBER
    for x, y in EYES:
        px[y, x] = EMBER_HOT
    return Image.fromarray(np.dstack([px, alpha]).astype(np.uint8), 'RGBA')


def glow():
    """The cracks burn, hottest where they start; the eyes and the antler tips burn brightest."""
    img = np.zeros((128, 128, 4), np.uint8)
    for (x, y), heat in cracks(np.random.default_rng(7)).items():
        img[y, x] = (*np.clip(mixc(EMBER_DIM, EMBER, heat), 0, 255).astype(np.uint8), 255)
    for x, y in tips():
        img[y, x] = (*EMBER_HOT, 255)
    for x, y in EYES:
        img[y, x] = (*EMBER_HOT, 255)
    return Image.fromarray(img, 'RGBA')


def write(g):
    one = lambda item, lo, hi: {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': item, 'functions': [
        {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]}]}
    g.write_json(g.DATA / 'loot_table/entities/ashen_stag.json', {'type': 'minecraft:entity', 'pools': [
        one('wildercord:cinder_antler', 1, 2), one('wildercord:aura_shard', 2, 3), one('wildercord:mana_crystal', 2, 3),
        one('wildercord:cinder_ash', 2, 5), one('minecraft:blaze_powder', 2, 4),
    ]})
    g.save(skin(), g.ASSETS / 'textures/entity/ashen_stag/skin.png')
    g.save(glow(), g.ASSETS / 'textures/entity/ashen_stag/glow.png')
    # The antler by the stump: a shed beam lying in the ash, its burr at one end, three tines standing up off it, ember-tipped.
    g.save(antler(), g.ASSETS / 'textures/block/cinder_antler.png')

    def faces(uv, up=None):
        out = {f: {'texture': '#s', 'uv': uv} for f in ('north', 'south', 'east', 'west', 'up', 'down')}
        if up:
            out['up'] = {'texture': '#s', 'uv': up}
        return out
    tine = lambda x0, h, lean: {'from': [x0, 2, 7.5], 'to': [x0 + 1, 2 + h, 8.5], 'rotation': {'origin': [x0 + .5, 2, 8], 'axis': 'z', 'angle': lean},
                                'faces': faces([12, 0, 13, h], [12, 0, 13, 1])}
    g.write_json(g.ASSETS / 'models/block/cinder_antler.json', {
        'parent': 'minecraft:block/block', 'textures': {'particle': 'wildercord:block/cinder_antler', 's': 'wildercord:block/cinder_antler'},
        'elements': [
            {'from': [2, 0, 6], 'to': [4, 3, 10], 'faces': faces([0, 4, 2, 7])},
            {'from': [4, 0, 7], 'to': [14, 2, 9], 'faces': faces([0, 0, 10, 2])},
            tine(6, 4, 22.5), tine(9, 6, 0), tine(12, 4, -22.5),
        ]})
    g.write_json(g.ASSETS / 'blockstates/cinder_antler.json', {'variants': {'': {'model': 'wildercord:block/cinder_antler'}}})
    g.write_json(g.ASSETS / 'items/cinder_antler.json', {'model': {'type': 'minecraft:model', 'model': 'wildercord:block/cinder_antler'}})


def antler():
    """The Cinder Antler, 16 x 16: burnt antler mottled with ash, the burr rough and dark, embers in its cracks and at every tip."""
    rng = np.random.default_rng(20261013)
    px = np.zeros((16, 16, 4), np.uint8)
    for y in range(16):
        for x in range(16):
            c = mixc(ANTLER, ANTLER_DARK, .2 + .5 * rng.random())
            if rng.random() < .2:
                c = mixc(c, ASH, .4)
            px[y, x] = (*c, 255)
    # The burr, rough and darker.
    for y in range(4, 8):
        for x in range(0, 3):
            px[y, x, :3] = mixc(ANTLER_DARK, CHAR_DARK, .5 * rng.random())
    # A crack glowing along the beam, and the tine tips burning.
    for x, y in ((2, 1), (3, 1), (4, 0), (7, 1), (8, 1)):
        px[y, x, :3] = EMBER
    px[0, 12, :3] = EMBER_HOT
    px[1, 12, :3] = EMBER
    return Image.fromarray(px, 'RGBA')
