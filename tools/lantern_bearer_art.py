"""The Lantern-Bearer: a gaunt hooded spirit in grave-rags, with a gravedigger's spade and a soul lantern on a crook; its skin, the glow
of its eyes and its lantern's flame, and its text."""
import numpy as np
from PIL import Image
from serpent_art import mixc, paint_box

LANG = {
    'entity.wildercord.lantern_bearer': 'Lantern-Bearer',
    'guide.wildercord.lantern_bearer': 'A gaunt hooded spirit in grave-rags that lies in the open grave under the gibbet of the Barrow '
                                       'Field. By day it is not there at all. After dark, when someone comes in among the graves, it rises, '
                                       'takes down the soul lantern hanging over its grave and wails. It reaps with its spade in front of '
                                       'it, hooks anyone keeping away and drags them in on its crook, looses soul wisps after anyone who '
                                       'runs, and drives its spade into the ground among anyone crowding it. Struck, it may go to smoke '
                                       'and drift away; its tools turn some of a blow from the front, and it is open a moment after an '
                                       'attack. Dawn sends it back down into its grave.',
    'guide.wildercord.lantern_bearer.hint': 'The Barrow Field: an old battlefield on the plains, meadows and savannas, with a gibbet at '
                                            'its heart. Come after dark.',
    'boss.wildercord.lantern_bearer.status': '%s — %s',
}
for name, text in {
    'roar': 'Lantern-Bearer wails', 'rise': 'Lantern-Bearer rises from the grave', 'step': 'Rags drag over grass',
    'reap': 'Spade reaps', 'hook': 'Crook lashes out', 'chain': 'Lantern chain rattles', 'wisp': 'Soul wisps loosed',
    'burst': 'Soul wisp bursts', 'slam': 'Spade strikes the earth', 'fade': 'Lantern-Bearer goes to smoke',
    'hurt': 'Lantern-Bearer moans', 'death': 'Lantern-Bearer sinks into the earth',
}.items():
    LANG['subtitles.wildercord.kit.lantern_bearer.' + name] = text
# What the bar says it is doing, the way a Master's bar names its cut.
for name, text in {'watch': 'Watching', 'reap': 'Reaping', 'hook': 'Hooking', 'wisps': 'Soul Wisps', 'slam': 'Grave Strike',
                   'fade': 'Gone to smoke', 'recover': 'Spent'}.items():
    LANG['boss.wildercord.lantern_bearer.move.' + name] = text

# Grave-rags, ashen grey going black at the hem and in the folds; bone-pale hands; the hood's hollow black; old wood and rusted iron;
# the lantern's soul-fire, pale cyan.
RAG, RAG_DARK, RAG_PALE = (78, 76, 80), (34, 32, 38), (116, 112, 112)
MANTLE, MANTLE_DARK = (60, 56, 62), (26, 24, 30)
BONE, BONE_DARK = (206, 198, 176), (150, 140, 118)
HOLLOW = (8, 8, 12)
WOOD, WOOD_DARK = (96, 70, 46), (58, 40, 26)
IRON, IRON_DARK, RUST = (98, 100, 104), (46, 48, 54), (128, 72, 42)
SOUL, SOUL_HOT = (90, 220, 230), (200, 250, 255)
# The eyes, two points of soul-fire in the hood's hollow.
EYES = ((48, 10), (52, 10))


def cage_panes():
    """The panes of the lantern's cage that let its flame through: each side face, inside its iron frame."""
    return [(x, y) for x0 in (100, 104, 108, 112) for y in range(45, 48) for x in range(x0 + 1, x0 + 3)]


def skin():
    rng = np.random.default_rng(20261012)
    px = np.zeros((128, 128, 3))
    alpha = np.zeros((128, 128))
    boxes = [  # corner, size (w, h, d), top, bottom, side fade
        ((0, 0), (9, 12, 6), MANTLE, RAG_DARK, (RAG, RAG_DARK)),                 # torso
        ((40, 36), (11, 3, 7), MANTLE, MANTLE_DARK, (MANTLE, MANTLE_DARK)),      # mantle over the shoulders
        ((0, 20), (10, 15, 8), RAG_DARK, RAG_DARK, (RAG, RAG_DARK)),             # the skirt of rags
        ((40, 0), (7, 7, 7), MANTLE, HOLLOW, (RAG, MANTLE_DARK)),                # hood
        ((70, 24), (8, 2, 1), MANTLE, MANTLE_DARK, (MANTLE, MANTLE_DARK)),       # its brim
        ((92, 24), (4, 2, 3), MANTLE, MANTLE_DARK, (MANTLE, MANTLE_DARK)),       # its peak
        ((40, 20), (3, 9, 3), RAG, RAG_DARK, (RAG, RAG_DARK)),                   # upper arms
        ((56, 20), (3, 6, 3), RAG, RAG_DARK, (RAG_PALE, RAG_DARK)),              # sleeves
        ((56, 30), (2, 3, 2), BONE, BONE_DARK, (BONE, BONE_DARK)),               # hands
        ((100, 0), (1, 20, 1), WOOD, WOOD_DARK, (WOOD, WOOD_DARK)),              # spade haft
        ((106, 0), (5, 6, 1), IRON, RUST, (IRON, RUST)),                         # spade blade, rust creeping up it
        ((120, 0), (1, 28, 1), WOOD, WOOD_DARK, (WOOD_DARK, WOOD)),              # crook pole
        ((100, 30), (1, 1, 5), WOOD, WOOD_DARK, (WOOD, WOOD_DARK)),              # crook tip
        ((116, 40), (1, 2, 1), IRON, IRON_DARK, (IRON, IRON_DARK)),              # chain
        ((100, 40), (4, 5, 4), IRON_DARK, IRON_DARK, (IRON, IRON_DARK)),         # cage
        ((100, 52), (5, 1, 5), IRON, IRON_DARK, (IRON, IRON_DARK)),              # cap
        ((100, 60), (3, 1, 3), IRON_DARK, IRON_DARK, (IRON, IRON_DARK)),         # foot
    ]
    for (tu, tv), (w, h, d), top, bottom, side in boxes:
        paint_box(px, tu, tv, w, h, d, top, bottom, side, rng, cell=2)
        alpha[tv:tv + d + h, tu:tu + 2 * (d + w)] = 255
    # The hood's front face is a hollow black, a rag rim round its edge.
    px[7:14, 47:54] = HOLLOW
    for x in range(47, 54):
        px[7, x] = mixc(RAG, MANTLE_DARK, .3 * rng.random())
    for y in range(7, 14):
        px[y, 47] = mixc(RAG, MANTLE_DARK, .4 + .3 * rng.random())
        px[y, 53] = mixc(RAG, MANTLE_DARK, .4 + .3 * rng.random())
    # The cape: rags hanging long down its back, streaked, the hem torn into ragged tongues.
    for y in range(21):
        for x in range(20):
            streak = .5 + .5 * np.sin(x * 1.7 + rng.random())
            px[y, 70 + x] = mixc(RAG, RAG_DARK, .2 + .2 * streak + .4 * y / 21 + .1 * rng.random())
            alpha[y, 70 + x] = 255
    for x in range(20):
        alpha[21 - int(rng.integers(0, 6)):21, 70 + x] = 0
    # The skirt's hem torn and holed; the tatter-tongues under it.
    for x in range(36):
        alpha[43 - int(rng.integers(0, 3)):43, x] = 0
    holes = rng.random((15, 36)) < .03
    alpha[28:43, 0:36][holes] = 0
    for y in range(46, 52):
        for x in range(16):
            px[y, x] = mixc(RAG_DARK, RAG, .2 + .4 * rng.random())
            alpha[y, x] = 255 if rng.random() > .12 * (y - 46) else 0
    # Pale mends across the torso and the mantle's edge.
    for (x0, y0), (w, h) in (((6, 6), (9, 12)), ((40, 43), (36, 3))):
        mend = rng.random((h, w)) < .1
        px[y0:y0 + h, x0:x0 + w][mend] = mixc(px[y0:y0 + h, x0:x0 + w][mend], RAG_PALE, .5)
    for x, y in cage_panes():
        px[y, x] = SOUL
    for x, y in EYES:
        px[y, x] = SOUL
    return Image.fromarray(np.dstack([px, alpha]).astype(np.uint8), 'RGBA')


def glow():
    """The eyes burn soul-blue, and the lantern's flame shows through its panes."""
    img = np.zeros((128, 128, 4), np.uint8)
    for x, y in cage_panes():
        img[y, x] = (*SOUL, 255)
    for x, y in EYES:
        img[y, x] = (*SOUL_HOT, 255)
    return Image.fromarray(img, 'RGBA')


def write(g):
    g.write_json(g.DATA / 'loot_table/entities/lantern_bearer.json',
                 {'type': 'minecraft:entity', 'pools': [], 'random_sequence': 'wildercord:entities/lantern_bearer'})
    g.save(skin(), g.ASSETS / 'textures/entity/lantern_bearer/skin.png')
    g.save(glow(), g.ASSETS / 'textures/entity/lantern_bearer/glow.png')
    # It is a dead thing: Smite bites it, and whatever harms the undead harms it.
    tags = g.DATA.parent / 'minecraft/tags/entity_type'
    g.write_json(tags / 'undead.json', {'replace': False, 'values': ['wildercord:lantern_bearer']})
    g.write_json(tags / 'sensitive_to_smite.json', {'replace': False, 'values': ['wildercord:lantern_bearer']})
