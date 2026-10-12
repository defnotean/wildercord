"""The Frost Wyrmling: a young drake of the glaciers, curled asleep on its hoard in a hall of ice; its skin, the glow of its eyes and the
frost at the tips of its horns and spines, and its text."""
import numpy as np
from PIL import Image
from serpent_art import mixc, paint_box

LANG = {
    'entity.wildercord.frost_wyrmling': 'Frost Wyrmling',
    'guide.wildercord.frost_wyrmling': 'A young drake of the glaciers, pale blue and frosted white, curled asleep on a hoard in a hall '
                                       'of ice. It wakes when someone comes far enough in, spreads its wings and roars, and keeps its hall '
                                       'clear of anything hostile. It bites, sweeps its tail, beats its wings to drive you off, breathes '
                                       'frost that slows and chills, and pounces from a distance. Its horned brow turns blows; its flanks '
                                       'do not, and it is open a moment after it lands. Wounded, it rears up and brings its forefeet down, and lances of ice '
                                       'burst up along cracks racing out across the floor: step off the crack before they reach you.',
    'guide.wildercord.frost_wyrmling.hint': 'The Glacier Den: a crevasse cut down into the ice under the snowy mountains, a hall of ice past it.',
    'boss.wildercord.frost_wyrmling.status': '%s — %s',
}
for name, text in {
    'roar': 'Frost Wyrmling roars', 'breath': 'Frost Wyrmling breathes frost', 'rise': 'Frost Wyrmling uncurls',
    'snarl': 'Frost Wyrmling snarls', 'step': 'Claws crunch on snow', 'bite': 'Frost Wyrmling bites', 'tail': 'Tail sweeps',
    'wing': 'Wings beat', 'frost': 'Frost spreads', 'land': 'Frost Wyrmling lands', 'bound': 'Frost Wyrmling leaps aside',
    'hurt': 'Frost Wyrmling hurts', 'death': 'Frost Wyrmling falls', 'lance': 'Ice lances burst up',
}.items():
    LANG['subtitles.wildercord.kit.frost_wyrmling.' + name] = text
# What the bar says it is doing, the way a Master's bar names its cut.
for name, text in {'watch': 'Watching', 'bite': 'Biting', 'tail': 'Sweeping', 'wing': 'Beating its wings', 'breath': 'Frost Breath',
                   'pounce': 'Pouncing', 'bound': 'Bounding', 'recover': 'Spent', 'lances': 'Rime Lances'}.items():
    LANG['boss.wildercord.frost_wyrmling.move.' + name] = text

# Icy blue scales, deeper along the back; a pale frost-white belly; horns and spines like old ice; pale, veined wing membranes.
SCALE, SCALE_DEEP, SCALE_PALE = (128, 172, 206), (70, 108, 152), (176, 210, 232)
BELLY, BELLY_SHADE = (226, 238, 246), (186, 206, 222)
HORN, HORN_DARK = (214, 232, 244), (140, 170, 196)
CLAW, MOUTH = (60, 70, 90), (120, 52, 70)
MEMBRANE, MEMBRANE_VEIN = (196, 222, 240), (132, 170, 204)
FROST, FROST_HOT = (150, 220, 255), (220, 248, 255)
# The eyes, on the head's side faces next to the front edges.
EYES = ((56, 36), (57, 36), (65, 36), (66, 36))


def tips():
    """The pixels that glint with frost: the top of each horn and each spine."""
    return [(117 + c, r) for c in range(2) for r in range(2)] + [(119 + c, r) for c in range(2) for r in range(2)] + \
           [(118, 10), (119, 10)]


def skin():
    rng = np.random.default_rng(20261012)
    px = np.zeros((128, 128, 3))
    alpha = np.zeros((128, 128))
    boxes = [  # corner, size (w, h, d), top, bottom, side fade
        ((0, 0), (10, 8, 20), SCALE_DEEP, BELLY, (SCALE, BELLY_SHADE)),       # body: deep blue down the back, a pale belly
        ((0, 28), (6, 6, 8), SCALE_DEEP, BELLY, (SCALE, BELLY_SHADE)),        # neck
        ((28, 28), (5, 5, 7), SCALE_DEEP, BELLY, (SCALE, BELLY_SHADE)),       # upper neck
        ((52, 28), (7, 6, 6), SCALE_DEEP, BELLY_SHADE, (SCALE, SCALE_PALE)),  # head
        ((78, 28), (5, 3, 5), SCALE, BELLY_SHADE, (SCALE, SCALE_PALE)),       # snout
        ((98, 28), (4, 2, 5), MOUTH, BELLY, (SCALE_PALE, BELLY)),             # jaw: its top is the inside of the mouth
        ((116, 0), (2, 6, 2), HORN, HORN_DARK, (HORN, HORN_DARK)),            # horns
        ((116, 10), (1, 3, 2), HORN, HORN_DARK, (HORN, HORN_DARK)),           # spines
        ((0, 44), (7, 6, 9), SCALE_DEEP, BELLY, (SCALE, BELLY_SHADE)),        # tail
        ((32, 44), (5, 4, 9), SCALE_DEEP, BELLY, (SCALE, BELLY_SHADE)),
        ((60, 44), (3, 3, 9), SCALE, BELLY, (SCALE, SCALE_PALE)),
        ((84, 44), (6, 1, 4), HORN, HORN_DARK, (HORN, HORN_DARK)),            # the tail's frosted fin
        ((0, 60), (4, 8, 4), SCALE, SCALE, (SCALE, SCALE_DEEP)),              # forelegs
        ((16, 60), (5, 8, 5), SCALE, SCALE, (SCALE, SCALE_DEEP)),             # haunches
        ((36, 60), (3, 7, 3), SCALE_DEEP, SCALE_DEEP, (SCALE_DEEP, SCALE)),   # shins
        ((48, 60), (4, 2, 4), SCALE_PALE, CLAW, (SCALE_PALE, CLAW)),          # feet
        ((0, 76), (8, 2, 2), SCALE_DEEP, SCALE, (SCALE_DEEP, SCALE)),         # wing arms
        ((40, 76), (9, 1, 1), SCALE_DEEP, SCALE, (SCALE_DEEP, SCALE)),
    ]
    for (tu, tv), (w, h, d), top, bottom, side in boxes:
        paint_box(px, tu, tv, w, h, d, top, bottom, side, rng, cell=3)
        alpha[tv:tv + d + h, tu:tu + 2 * (d + w)] = 255
    # The membranes: pale, with veins fanning back from the arm, a darker trailing edge; both faces.
    for tu, tv, w, d in ((0, 82, 8, 11), (40, 82, 9, 10)):
        for face in (tu + d, tu + d + w):
            for z in range(d):
                for x in range(w):
                    c = mixc(MEMBRANE, MEMBRANE_VEIN, .25 * rng.random() + .5 * z / d)
                    if x % 3 == 1 or z == d - 1:
                        c = mixc(c, MEMBRANE_VEIN, .6)
                    px[tv + z, face + x] = c
                    alpha[tv + z, face + x] = 255
    # Frost crusted unevenly over the back and the flanks.
    for (x0, y0), (w, h) in (((20, 0), (10, 20)), ((0, 20), (60, 8)), ((8, 28), (6, 8)), ((0, 53), (32, 6))):
        crust = rng.random((h, w)) < .14
        px[y0:y0 + h, x0:x0 + w][crust] = mixc(px[y0:y0 + h, x0:x0 + w][crust], BELLY, .55)
    for x, y in tips():
        px[y, x] = FROST_HOT
    for x, y in EYES:
        px[y, x] = FROST
    return Image.fromarray(np.dstack([px, alpha]).astype(np.uint8), 'RGBA')


def glow():
    """The eyes burn a cold blue; frost glints at the tips of its horns and spines."""
    img = np.zeros((128, 128, 4), np.uint8)
    for x, y in tips():
        img[y, x] = (*FROST, 255)
    for x, y in EYES:
        img[y, x] = (*FROST_HOT, 255)
    return Image.fromarray(img, 'RGBA')


def write(g):
    g.write_json(g.DATA / 'loot_table/entities/frost_wyrmling.json', {'type': 'minecraft:entity', 'pools': []})
    g.save(skin(), g.ASSETS / 'textures/entity/frost_wyrmling/skin.png')
    g.save(glow(), g.ASSETS / 'textures/entity/frost_wyrmling/glow.png')
