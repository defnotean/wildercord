"""The Hollow Matron: a great dusky spider hanging under the nest in the Silk Hollow's hall; her skin, the glow of her eight eyes,
and her text."""
import numpy as np
from PIL import Image
from serpent_art import mixc, paint_box

LANG = {
    'entity.wildercord.hollow_matron': 'Hollow Matron',
    'guide.wildercord.hollow_matron': 'A great dusky spider, pale silk banding her legs and a bone-white mark on her back, eight red '
                                      'eyes over her fangs. She hangs head-down on her thread under the nest in the hall the miners '
                                      'broke into, and drops when someone comes far enough in. She bites and sweeps with her forelegs up '
                                      'close, leaps at anyone a little way off and spits web that slows at anyone keeping away. Her fangs '
                                      'turn blows from the front; her abdomen behind does not, and after a leap she is open. Wounded, she '
                                      'climbs one of the silk pillars, wakes her brood in the egg sacs and drops on whoever she marks; '
                                      'dust falls where she will land. Strike her pillar\'s silk while she is on it and she comes down hard.',
    'guide.wildercord.hollow_matron.hint': 'An abandoned mine in a forest or the hills, a headframe over its shaft, webs thickening below.',
    'boss.wildercord.hollow_matron.status': '%s — %s',
}
for name, text in {
    'hiss': 'Hollow Matron hisses', 'chitter': 'Hollow Matron chitters', 'creak': 'Silk creaks', 'drop': 'Hollow Matron drops',
    'land': 'Hollow Matron lands', 'step': 'Legs patter', 'bite': 'Fangs snap', 'sweep': 'Forelegs sweep', 'spit': 'Hollow Matron spits',
    'splat': 'Web splatters', 'pounce': 'Hollow Matron leaps', 'skitter': 'Hollow Matron skitters', 'hurt': 'Hollow Matron hurts',
    'death': 'Hollow Matron dies', 'climb': 'Hollow Matron climbs', 'hatch': 'Egg sac splits', 'tear': 'Silk tears',
}.items():
    LANG['subtitles.wildercord.kit.hollow_matron.' + name] = text
for name, text in {'watch': 'Watching', 'bite': 'Biting', 'sweep': 'Sweeping', 'spit': 'Spitting', 'pounce': 'Leaping',
                   'skitter': 'Skittering', 'recover': 'Spent',
                   'haste': 'Making for the silk', 'scale': 'Climbing', 'lurk': 'Lurking', 'plunge': 'Plunging',
                   'tumble': 'Falling'}.items():
    LANG['boss.wildercord.hollow_matron.move.' + name] = text
LANG['block.wildercord.weavers_silk'] = "Weaver's Silk"
LANG['key.wildercord.weavers_silk'] = "Put away / take out Weaver's Silk"
LANG['message.wildercord.weavers_silk.take'] = 'The silk winds round your wrists. Walk into a wall and you climb it; sneak and you cling. Press G (or /silk) to put it away.'
LANG['message.wildercord.weavers_silk.stowed'] = 'You wind the silk away. Walls no longer hold you.'
LANG['message.wildercord.weavers_silk.unstowed'] = 'You let the silk out. Walls hold you again.'
LANG['message.wildercord.weavers_silk.none'] = "You don't carry the Weaver's Silk."
LANG['message.wildercord.weavers_silk.bound'] = 'Her silk is already wound round your wrists.'
LANG['message.wildercord.weavers_silk.guarded'] = 'The Hollow Matron still keeps her hall.'
LANG['message.wildercord.weavers_silk.slip'] = 'The silk lets go.'

# Dusky grey-brown, darker underneath; pale silk; bone; red eyes; dark fangs.
DUSK, DUSK_DARK, DUSK_PALE = (92, 82, 72), (48, 42, 38), (128, 118, 104)
SILK, BONE = (196, 190, 178), (226, 220, 204)
FANG, FANG_TIP = (40, 30, 28), (120, 34, 30)
EYE, EYE_HOT = (150, 20, 18), (255, 70, 50)
# Her eight eyes on the head's front face, which starts at (47, 3): two big ones in the middle of a row of four, and four below.
EYES = [(47 + x, 3 + y) for x, y in ((1, 1), (2, 1), (3, 1), (4, 1), (0, 2), (2, 2), (3, 2), (5, 2))]
BIG = {(49, 4), (50, 4)}


def skin():
    rng = np.random.default_rng(20261012)
    px = np.zeros((128, 128, 3))
    alpha = np.zeros((128, 128))
    boxes = [  # corner, size (w, h, d), top, bottom, side fade
        ((0, 0), (9, 6, 11), DUSK, DUSK_DARK, (DUSK_PALE, DUSK_DARK)),       # cephalothorax
        ((44, 0), (6, 5, 3), DUSK, DUSK_DARK, (DUSK, DUSK_DARK)),            # head
        ((64, 0), (2, 4, 2), FANG, FANG, (FANG, FANG)),                      # fangs
        ((74, 0), (1, 1, 4), DUSK_PALE, DUSK, (DUSK_PALE, DUSK)),            # palps
        ((0, 20), (14, 12, 14), DUSK, DUSK_DARK, (DUSK, DUSK_DARK)),         # abdomen
        ((60, 20), (3, 3, 2), DUSK_DARK, DUSK_DARK, (DUSK, DUSK_DARK)),      # spinnerets
        ((60, 28), (1, 1, 16), SILK, SILK, (SILK, BONE)),                    # thread
        ((0, 50), (12, 2, 2), DUSK, DUSK_DARK, (DUSK, DUSK_DARK)),           # femurs
        ((0, 56), (14, 2, 2), DUSK, DUSK_DARK, (DUSK, DUSK_DARK)),           # tibias
    ]
    for (tu, tv), (w, h, d), top, bottom, side in boxes:
        paint_box(px, tu, tv, w, h, d, top, bottom, side, rng, cell=4)
        alpha[tv:tv + d + h, tu:tu + 2 * (d + w)] = 255
    # Pale silk bands round her legs: at each femur's tip, and two along each tibia, the tip palest.
    for x in (10, 11, 24, 25):
        px[50:54, x] = mixc(px[50:54, x], SILK, .75)
    for x in (6, 7, 13, 14, 21, 22, 29, 30):
        px[56:60, x] = mixc(px[56:60, x], SILK, .7)
    px[56:60, 15:17] = mixc(px[56:60, 15:17], FANG, .6)
    # Fine hairs flecked over her body and abdomen.
    for (x0, y0), (w, h) in (((0, 0), (40, 17)), ((0, 20), (56, 26))):
        fleck = rng.random((h, w)) < .1
        px[y0:y0 + h, x0:x0 + w][fleck] = mixc(px[y0:y0 + h, x0:x0 + w][fleck], DUSK_PALE, .5)
    # The bone-white mark on her back: a long diamond down the middle with a bar across it, chevrons fading off behind.
    top = px[20:34, 14:28]
    for y in range(14):
        for x in range(14):
            cx = abs(x - 6.5)
            if cx <= 3 - abs(y - 5) * .55 or (y == 5 and cx <= 4.5) or (y in (9, 11) and abs(cx - (y - 7) * .8) < .9):
                top[y, x] = mixc(top[y, x], BONE, .85 if y < 9 else .55)
    # Fangs tipped dark red.
    px[4:6, 64:72] = FANG_TIP
    for x, y in EYES:
        px[y, x] = EYE_HOT if (x, y) in BIG else EYE
    return Image.fromarray(np.dstack([px, alpha]).astype(np.uint8), 'RGBA')


def glow():
    """Her eyes alone, deep red, the middle pair brightest."""
    img = np.zeros((128, 128, 4), np.uint8)
    for x, y in EYES:
        img[y, x] = (*(EYE_HOT if (x, y) in BIG else (210, 40, 30)), 255)
    return Image.fromarray(img, 'RGBA')


def write(g):
    g.write_json(g.DATA / 'loot_table/entities/hollow_matron.json', {'type': 'minecraft:entity', 'pools': []})
    g.save(skin(), g.ASSETS / 'textures/entity/hollow_matron/skin.png')
    g.save(glow(), g.ASSETS / 'textures/entity/hollow_matron/glow.png')
    # Under the nest: a skein of her silk wound round a long bone lying on the floor, loose strands trailing off it.
    g.save(skein(), g.ASSETS / 'textures/block/weavers_silk.png')
    faces = lambda uv: {f: {'texture': '#s', 'uv': uv} for f in ('north', 'south', 'east', 'west', 'up', 'down')}
    g.write_json(g.ASSETS / 'models/block/weavers_silk.json', {
        'parent': 'minecraft:block/block', 'textures': {'particle': 'wildercord:block/weavers_silk', 's': 'wildercord:block/weavers_silk'},
        'elements': [
            # The bone, its knobbed ends showing either side of the skein.
            {'from': [2, 0, 7], 'to': [14, 2, 9], 'rotation': {'origin': [8, 1, 8], 'axis': 'y', 'angle': 22.5}, 'faces': faces([0, 12, 12, 14])},
            {'from': [1, 0, 6], 'to': [3, 3, 10], 'rotation': {'origin': [8, 1, 8], 'axis': 'y', 'angle': 22.5}, 'faces': faces([12, 12, 14, 15])},
            {'from': [13, 0, 6], 'to': [15, 3, 10], 'rotation': {'origin': [8, 1, 8], 'axis': 'y', 'angle': 22.5}, 'faces': faces([12, 12, 14, 15])},
            # The skein wound thick round its middle.
            {'from': [4, 0, 4], 'to': [12, 6, 12], 'rotation': {'origin': [8, 1, 8], 'axis': 'y', 'angle': 22.5}, 'faces': faces([0, 0, 8, 6])},
            {'from': [5, 6, 5], 'to': [11, 7, 11], 'rotation': {'origin': [8, 1, 8], 'axis': 'y', 'angle': 22.5}, 'faces': faces([0, 6, 6, 7])},
        ]})
    g.write_json(g.ASSETS / 'blockstates/weavers_silk.json', {'variants': {'': {'model': 'wildercord:block/weavers_silk'}}})
    g.write_json(g.ASSETS / 'items/weavers_silk.json', {'model': {'type': 'minecraft:model', 'model': 'wildercord:block/weavers_silk'}})


def skein():
    """Weaver's Silk, 16 x 16: wound silk in pale bands with darker strands crossing it (top), and old yellowed bone (bottom)."""
    rng = np.random.default_rng(20261014)
    px = np.zeros((16, 16, 4), np.uint8)
    for y in range(16):
        for x in range(16):
            if y < 8:
                # Wound turns, each a shade apart, crossed now and then by a strand pulled the other way.
                c = mixc(SILK, BONE, .3 + .4 * ((x + 2 * y) % 3 == 0) + .2 * rng.random())
                if (x - y) % 5 == 0:
                    c = mixc(c, DUSK_PALE, .45)
            else:
                c = mixc(BONE, (190, 176, 140), .25 + .4 * rng.random())
                if y in (12, 15) or rng.random() < .1:
                    c = mixc(c, DUSK_PALE, .35)
            px[y, x] = (*c, 255)
    return Image.fromarray(px, 'RGBA')
