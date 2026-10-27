"""Mojito: a giant black standard poodle with a white bib and beard, a gold chain collar and a greatsword held in his mouth, guarding a
misty grove; his skin, the mint glow of his eyes, his blade's runes and his magic, his loot and his text."""
import numpy as np
from PIL import Image
from serpent_art import mixc

LANG = {
    'entity.wildercord.mojito': 'Mojito',
    'guide.wildercord.mojito': 'A black poodle grown as tall as a house, his coat in tight curls, a white bib on his chest and a gold chain '
                               'about his neck, asleep curled up before a greatsword in a misty grove. Come far enough in and he takes the '
                               'sword up in his mouth, shakes himself out and barks. He sweeps the blade across whoever is in front of '
                               'him, spins round at whoever gets behind, leaps on those who back off and looses mint-green magic from '
                               'his sides at those who run. His brow turns blows; his flanks do not. Worn down, he gets the zoomies: '
                               'round and round, the grass torn up behind him, until he flops over panting.',
    'guide.wildercord.mojito.hint': 'A misty grove ringed by old trees, a greatsword lying in the grass at its heart.',
    'boss.wildercord.mojito.status': '%s — %s',
    'item.wildercord.mojito_fetchblade': "Mojito's Fetchblade",
}
for name, text in {'watch': 'Watching', 'slash': 'Slashing', 'spin': 'Spinning', 'leap': 'Leaping', 'bolts': 'Casting',
                   'hop': 'Hopping clear', 'recover': 'Panting', 'zoomies': 'Zoomies!', 'flop': 'Flopped over'}.items():
    LANG['boss.wildercord.mojito.move.' + name] = text

# Black curls with a little grey to the highlights; white curls on his chest and chin; a brown tinge about the cheeks.
BLACK, CURL, SHADE = (30, 28, 33), (62, 58, 66), (14, 13, 16)
WHITE, WHITE_SHADE = (236, 232, 224), (176, 170, 166)
CHEEK = (104, 66, 42)
NOSE, NOSE_SHINE = (12, 11, 13), (70, 70, 78)
EYE, EYE_SHINE = (44, 24, 14), (124, 255, 178)
TONGUE, TONGUE_DARK, MOUTH = (238, 122, 142), (196, 84, 104), (110, 44, 56)
GOLD, GOLD_DARK, GOLD_SHINE = (214, 170, 58), (138, 98, 28), (255, 230, 130)
STEEL, STEEL_DARK, STEEL_EDGE = (172, 182, 196), (116, 124, 138), (226, 232, 240)
IRON, LEATHER, LEATHER_DARK = (66, 68, 78), (96, 62, 38), (62, 40, 26)
MINT, MINT_CORE, MINT_DIM = (124, 255, 178), (226, 255, 238), (60, 170, 112)


def faces(tu, tv, w, h, d):
    """A box's six faces in the vanilla cube layout: name -> (x, y, width, height)."""
    return {'top': (tu + d, tv, w, d), 'bottom': (tu + d + w, tv, w, d), 'right': (tu, tv + d, d, h),
            'front': (tu + d, tv + d, w, h), 'left': (tu + d + w, tv + d, d, h), 'back': (tu + 2 * d + w, tv + d, w, h)}


class Skin:
    def __init__(self, seed):
        self.rng = np.random.default_rng(seed)
        self.px = np.zeros((128, 128, 3))
        self.alpha = np.zeros((128, 128))

    def coat(self, x, y, w, h, base=BLACK, curl=CURL, shade=SHADE):
        """Tight curls: each 2 x 2 cell a little ring, lit at one corner and shadowed at the opposite one, jittered so no two match."""
        for j in range(h):
            for i in range(w):
                c = mixc(base, shade, self.rng.random() * 0.35)
                cx, cy = (i + (j // 2) % 2) % 2, j % 2
                if cx == 0 and cy == 0 and self.rng.random() < 0.8:
                    c = mixc(base, curl, 0.6 + 0.4 * self.rng.random())
                elif cx == 1 and cy == 1 and self.rng.random() < 0.6:
                    c = mixc(base, shade, 0.7)
                self.px[y + j, x + i] = c
        self.alpha[y:y + h, x:x + w] = 255

    def flat(self, x, y, w, h, col, vary=0.08):
        for j in range(h):
            for i in range(w):
                self.px[y + j, x + i] = np.clip(np.asarray(col, float) * (1 - vary + 2 * vary * self.rng.random()), 0, 255)
        self.alpha[y:y + h, x:x + w] = 255

    def box(self, tu, tv, w, h, d, **colours):
        """Every face of a box in curls of one colour (black unless `base` etc. say otherwise)."""
        for x, y, fw, fh in faces(tu, tv, w, h, d).values():
            self.coat(x, y, fw, fh, **colours)

    def tint(self, x, y, w, h, col, t):
        self.px[y:y + h, x:x + w] = mixc(self.px[y:y + h, x:x + w], col, t)

    def image(self):
        return Image.fromarray(np.dstack([np.clip(self.px, 0, 255), self.alpha]).astype(np.uint8), 'RGBA')


WHITE_COAT = {'base': WHITE, 'curl': (255, 255, 252), 'shade': WHITE_SHADE}
# Where the eyes sit on the front of his head, either side above the snout; and the runes down the flat of the blade.
EYES = ((9, 40), (10, 40), (13, 40), (14, 40))
RUNES = [(1 + x, 62) for x in range(2, 23, 3)] + [(26 + x, 62) for x in range(2, 23, 3)]
GEM = ((98, 48), (99, 48), (102, 48), (103, 48))


def skin():
    s = Skin(20261012)
    s.box(0, 0, 9, 9, 18)                                   # body
    s.box(60, 0, 7, 8, 2, **WHITE_COAT)                     # the white bib down his chest
    s.box(84, 0, 6, 10, 6)                                  # neck
    x, y, w, h = faces(84, 0, 6, 10, 6)['front']
    s.coat(x, y + h - 3, w, 3, **WHITE_COAT)                # the bib running up his throat
    # The gold chain collar: links alternating side-on and face-on all the way round.
    for x, y, w, h in faces(84, 16, 7, 2, 7).values():
        for j in range(h):
            for i in range(w):
                s.px[y + j, x + i] = GOLD_SHINE if (i + j) % 3 == 0 else GOLD if (i + j) % 3 == 1 else GOLD_DARK
        s.alpha[y:y + h, x:x + w] = 255
    s.box(0, 30, 8, 7, 8)                                   # head
    for side in ('right', 'left'):                          # brown-tinged cheeks
        x, y, w, h = faces(0, 30, 8, 7, 8)[side]
        s.tint(x, y + 4, w, h - 4, CHEEK, 0.32)
    x, y, w, h = faces(0, 30, 8, 7, 8)['front']
    s.tint(x, y + 4, w, 3, CHEEK, 0.3)
    for (ex, ey) in EYES:
        s.px[ey, ex] = EYE
    s.px[40, 9] = s.px[40, 13] = EYE_SHINE
    s.box(32, 30, 7, 4, 7, curl=(84, 80, 90))               # the topknot, fluffier
    s.box(60, 30, 4, 4, 5)                                  # snout
    x, y, w, h = faces(60, 30, 4, 4, 5)['front']
    s.flat(x, y, w, 2, NOSE)
    s.px[y, x + 1] = NOSE_SHINE
    x, y, w, h = faces(60, 30, 4, 4, 5)['bottom']
    s.coat(x, y, w, h, **WHITE_COAT)                        # white under the chin
    for side in ('right', 'left'):
        x, y, w, h = faces(60, 30, 4, 4, 5)[side]
        s.tint(x, y + 2, w, 2, CHEEK, 0.35)
    # The lower jaw: the inside of the mouth on top, white beneath and round its edges.
    s.flat(78, 30, 16, 6, WHITE)
    s.flat(82, 30, 4, 5, MOUTH)
    s.box(78, 37, 3, 2, 3, **WHITE_COAT)                    # beard
    s.box(94, 30, 2, 8, 4, curl=(76, 72, 82))               # ears
    s.flat(108, 30, 6, 4, TONGUE)
    s.flat(109, 31, 1, 3, TONGUE_DARK, 0)
    s.flat(112, 31, 1, 3, TONGUE_DARK, 0)
    s.box(0, 45, 4, 7, 4)                                   # thighs
    s.box(16, 45, 3, 6, 3)                                  # shins
    s.box(28, 45, 4, 2, 4)                                  # paws
    x, y, w, h = faces(28, 45, 4, 2, 4)['bottom']
    s.flat(x, y, w, h, (40, 38, 44))
    s.box(44, 45, 3, 6, 3)                                  # tail
    s.box(56, 45, 4, 4, 4, curl=(84, 80, 90))               # its pom
    # The sword: leather-wrapped grip, an iron guard edged with gold, a gold pommel set with a mint stone, a long steel blade.
    for x, y, w, h in faces(72, 45, 6, 1, 1).values():
        for i in range(w):
            s.px[y:y + h, x + i] = LEATHER if i % 2 else LEATHER_DARK
        s.alpha[y:y + h, x:x + w] = 255
    for x, y, w, h in faces(88, 45, 1, 4, 2).values():
        s.flat(x, y, w, h, IRON)
        s.flat(x, y, w, 1, GOLD, 0.05)
    for x, y, w, h in faces(96, 45, 2, 2, 2).values():
        s.flat(x, y, w, h, GOLD)
    for x, y in GEM:
        s.px[y, x] = MINT
    for tu, w, h in ((0, 24, 3), (52, 3, 2)):
        for name, (x, y, fw, fh) in faces(tu, 60, w, h, 1).items():
            if name in ('top', 'bottom', 'right', 'left'):
                s.flat(x, y, fw, fh, STEEL_EDGE, 0.03)
                continue
            s.flat(x, y, fw, fh, STEEL, 0.04)
            s.flat(x, y, fw, 1, STEEL_EDGE, 0.02)
            s.flat(x, y + fh - 1, fw, 1, STEEL_EDGE, 0.02)
            if fh >= 3:
                s.flat(x, y + 1, fw, 1, STEEL_DARK, 0.04)  # the fuller
    for x, y in RUNES:
        s.px[y, x] = MINT
    # The magic at his sides: mint, white at the heart.
    for x, y, w, h in faces(64, 60, 4, 4, 4).values():
        s.flat(x, y, w, h, MINT, 0.1)
        s.flat(x + 1, y + 1, max(1, w - 2), max(1, h - 2), MINT_CORE, 0.03)
    return s.image()


def glow():
    """His eyes, a soft mint; the runes down his blade and the stone in its pommel; and the magic at his sides, all of it."""
    img = np.zeros((128, 128, 4), np.uint8)
    for x, y in EYES:
        img[y, x] = (*MINT_DIM, 255)
    for x, y in RUNES + list(GEM):
        img[y, x] = (*MINT, 255)
    for x, y, w, h in faces(64, 60, 4, 4, 4).values():
        img[y:y + h, x:x + w] = (*MINT, 255)
        img[y + 1:y + h - 1, x + 1:x + w - 1] = (*MINT_CORE, 255)
    return Image.fromarray(img, 'RGBA')


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/mojito/skin.png')
    g.save(glow(), g.ASSETS / 'textures/entity/mojito/glow.png')
    one = lambda item, lo, hi, chance=None: {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': item, 'functions': [
        {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]}],
        **({'conditions': [{'condition': 'minecraft:random_chance', 'chance': chance}]} if chance else {})}
    g.write_json(g.DATA / 'loot_table/entities/mojito.json', {'type': 'minecraft:entity', 'pools': [
        one('minecraft:bone', 6, 12), one('minecraft:emerald', 4, 9), one('minecraft:diamond', 2, 4),
        one('wildercord:aura_shard', 2, 3), one('wildercord:mana_crystal', 2, 3), one('wildercord:torn_page', 1, 2),
        # His sword, every time: aura-forged with Fetchblade so it gains more aura, cuts wider, and bounds ahead on use.
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:netherite_sword', 'functions': [
            {'function': 'minecraft:set_name', 'target': 'item_name', 'name': {'translate': 'item.wildercord.mojito_fetchblade'}},
            {'function': 'minecraft:set_components', 'components': {
                'wildercord:aura_forged': 'fetchblade',
                'minecraft:rarity': 'epic',
            }},
        ]}]},
    ]})
