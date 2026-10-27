"""The Drowned Bell-Warden: a drowned giant kneeling in the sunken chapel with its cracked church bell worn over its head;
its skin, the glow of the eyes in the crack, and its text."""
import numpy as np
from PIL import Image
from serpent_art import mixc, paint_box

LANG = {
    'entity.wildercord.drowned_bell_warden': 'Drowned Bell-Warden',
    'guide.wildercord.drowned_bell_warden': 'A drowned giant wearing a cracked church bell over its head, kneeling in vigil before the '
                                           'broken bell-frame of a chapel sunk to the ocean floor. It rises when someone walks far enough '
                                           'up the nave, and rings anything hostile out of its hall. Leave the chapel and it kneels again, healed.',
    'guide.wildercord.drowned_bell_warden.hint': 'A chapel sunk whole to the deep ocean floor, kept dry by a moon pool under its belfry.',
    'boss.wildercord.drowned_bell_warden.status': '%s — %s',
}
for name, text in {
    'toll': 'Cracked bell tolls', 'breath': 'Bell-Warden breathes water', 'rise': 'Bell-Warden rises', 'challenge': 'Bell-Warden bellows',
    'hurt': 'Bell-Warden hurts', 'death': 'Bell-Warden falls', 'step': 'Bell-Warden treads',
    'swing': 'Clapper swings', 'hammer': 'Clapper strikes the floor', 'slam': 'Bell-Warden pounds the floor', 'charge': 'Bell-Warden charges',
    'backstep': 'Bell-Warden steps back', 'shell': 'Bell-Warden hides under its bell', 'ring': 'Bell rings under a blow',
    'gather': 'Bell-Warden raises its clapper', 'knell': 'Bell-Warden tolls its own bell',
}.items():
    LANG['subtitles.wildercord.kit.bell_warden.' + name] = text
# What the bar says it is doing, the way a Master's bar names its cut.
for name, text in {'watch': 'Watching', 'swing': 'Swinging', 'hammer': 'Hammering', 'slam': 'Pounding', 'charge': 'Charging',
                   'backstep': 'Stepping back', 'recover': 'Spent', 'shell': 'Under its bell', 'toll': 'Tolling'}.items():
    LANG['boss.wildercord.drowned_bell_warden.move.' + name] = text

LANG['block.wildercord.bell_shard'] = 'Bell Shard'
LANG['message.wildercord.bell_shard.touch'] = 'The shard hums under your hand. When you are near death, it will toll for you.'
LANG['message.wildercord.bell_shard.bound'] = 'The shard already tolls for you.'
LANG['message.wildercord.bell_shard.guarded'] = 'The Bell-Warden still keeps its vigil.'
LANG['message.wildercord.bell_shard.knell'] = 'The shard tolls for you!'

# Bronze gone green under the sea, pale drowned flesh, sodden rags, black iron, kelp.
BRONZE, BRONZE_DARK, VERDIGRIS, VERD_PALE = (128, 96, 54), (74, 54, 32), (70, 140, 116), (128, 186, 158)
FLESH, FLESH_DARK, RAG, RAG_DARK = (118, 146, 136), (70, 94, 90), (58, 64, 60), (36, 40, 40)
IRON, IRON_LIGHT, RUST = (62, 64, 70), (110, 112, 118), (110, 66, 40)
KELP, KELP_DARK, HOLLOW = (58, 104, 46), (34, 66, 30), (14, 18, 20)
EYE, EYE_HALO = (196, 255, 236), (40, 110, 96)
# The bell's front face, where the crack is: its corner and size on the skin.
FRONT, FRONT_SIZE = (16, 46), (16, 13)
# The eyes, in the front face's pixels (column, row).
EYES = ((4, 6), (5, 6), (10, 6), (11, 6))


def crack():
    """The crack's pixels in the front face: a jagged split from the crown down to the lip, opening wide across the eyes."""
    out = set()
    x = 8
    for y in range(13):
        out.add((x, y))
        if y in (5, 6, 7):
            for c in range(3, 13):
                if not (y != 6 and c in (3, 12)):
                    out.add((c, y))
        x += (1, 0, -1, 0, 1, -1, 0)[y % 7] if y not in (5, 6, 7) else 0
        x = max(6, min(10, x))
    return out


def skin():
    rng = np.random.default_rng(20261012)
    px = np.zeros((128, 128, 3))
    alpha = np.zeros((128, 128))
    boxes = [  # corner, size (w, h, d), top, bottom, side fade
        ((0, 0), (14, 12, 9), FLESH_DARK, RAG_DARK, (FLESH, RAG)),          # torso, rags round its middle
        ((48, 0), (6, 9, 6), FLESH, FLESH_DARK, (FLESH, FLESH_DARK)),       # upper arms
        ((48, 15), (6, 9, 6), FLESH, FLESH_DARK, (FLESH, FLESH_DARK)),      # forearms
        ((72, 0), (7, 6, 7), FLESH_DARK, FLESH_DARK, (FLESH, FLESH_DARK)),  # fists
        ((100, 0), (7, 6, 7), RAG, RAG_DARK, (RAG, RAG_DARK)),              # thighs, in rags
        ((100, 13), (7, 7, 7), RAG_DARK, FLESH_DARK, (RAG_DARK, FLESH_DARK)),  # shins
        ((72, 14), (7, 4, 7), IRON, IRON, (IRON_LIGHT, IRON)),              # chain wrapped round its arm
        ((0, 30), (16, 13, 16), VERDIGRIS, HOLLOW, (VERDIGRIS, BRONZE)),    # bell
        ((64, 30), (10, 3, 10), VERD_PALE, BRONZE_DARK, (VERDIGRIS, VERDIGRIS)),  # crown
        ((104, 30), (4, 3, 2), VERDIGRIS, BRONZE, (VERDIGRIS, BRONZE)),     # canon, the bell's hanging loop
        ((0, 60), (18, 2, 18), BRONZE, HOLLOW, (BRONZE, BRONZE_DARK)),      # lip; underneath, the bell's dark inside
        ((72, 60), (2, 6, 2), IRON, IRON, (IRON_LIGHT, IRON)),              # clapper's shank
        ((80, 60), (7, 8, 7), IRON, RUST, (IRON, RUST)),                    # clapper's ball
    ]
    for (tu, tv), (w, h, d), top, bottom, side in boxes:
        paint_box(px, tu, tv, w, h, d, top, bottom, side, rng, cell=4)
        alpha[tv:tv + d + h, tu:tu + 2 * (d + w)] = 255
    # Verdigris running down the bell's sides in streaks from the shoulder, and pale salt on the crown.
    tu, tv, w, h, d = 0, 30, 16, 13, 16
    for x in range(tu, tu + 2 * (d + w)):
        run = int(rng.integers(2, h))
        if rng.random() < 0.55:
            px[tv + d:tv + d + run, x] = mixc(px[tv + d:tv + d + run, x], VERD_PALE, 0.35 + 0.25 * rng.random())
    # Rag bands across the torso, and a rope belt.
    px[9 + 8:9 + 10, 0:46] = mixc(px[9 + 8:9 + 10, 0:46], RAG_DARK, 0.7)
    for x in range(0, 46, 3):
        px[9 + 10:9 + 12, x] = mixc(px[9 + 10:9 + 12, x], RAG_DARK, 0.5)
    # Chain links: alternating light and dark across the wrap.
    for x in range(72, 100):
        for y in range(14 + 7, 14 + 11):
            if (x + y) % 3 == 0:
                px[y, x] = IRON_LIGHT
            elif (x - y) % 4 == 0:
                px[y, x] = mixc(IRON, RUST, 0.5)
    # Knuckles and nails on the fists' fronts.
    for tu in (72,):
        px[7 + 1, tu + 7:tu + 14] = mixc(px[7 + 1, tu + 7:tu + 14], (170, 188, 176), 0.5)
    # The crack, and the eyes in it.
    fx, fy = FRONT
    for c, r in crack():
        px[fy + r, fx + c] = HOLLOW
    for c, r in EYES:
        px[fy + r, fx + c] = EYE
    img = Image.fromarray(np.dstack([px, alpha]).astype(np.uint8), 'RGBA')
    kelp(img, rng)
    return img


def kelp(img, rng):
    """Strands of kelp hanging off the bell's lip: a ragged leaf down each face of a flat plane, see-through round its edges."""
    a = np.array(img)
    for x0 in (112, 116):
        for y in range(36, 48):
            t = (y - 36) / 11
            width = 4 if y < 44 else (3 if y < 46 else 2)
            off = int(rng.integers(0, 2)) if width < 4 else 0
            for x in range(x0 + off, x0 + off + width):
                col = mixc(KELP, KELP_DARK, t * 0.6 + 0.3 * rng.random())
                a[y, x] = (*np.clip(col, 0, 255).astype(np.uint8), 255)
    return img.paste(Image.fromarray(a, 'RGBA'))


def glow():
    """Only the eyes glow, and a faint light round them inside the crack."""
    img = np.zeros((128, 128, 4), np.uint8)
    fx, fy = FRONT
    for c, r in crack():
        if r in (5, 6, 7):
            img[fy + r, fx + c] = (*EYE_HALO, 255)
    for c, r in EYES:
        img[fy + r, fx + c] = (*EYE, 255)
    return Image.fromarray(img, 'RGBA')


def write(g):
    one = lambda item, lo, hi: {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': item, 'functions': [
        {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]}]}
    g.write_json(g.DATA / 'loot_table/entities/drowned_bell_warden.json', {'type': 'minecraft:entity', 'pools': [
        one('wildercord:bell_shard', 1, 2), one('wildercord:aura_shard', 3, 4), one('wildercord:mana_crystal', 2, 4),
        one('wildercord:torn_page', 1, 2), one('minecraft:prismarine_crystals', 4, 8), one('minecraft:diamond', 2, 5),
    ]})
    g.save(skin(), g.ASSETS / 'textures/entity/drowned_bell_warden/skin.png')
    g.save(glow(), g.ASSETS / 'textures/entity/drowned_bell_warden/glow.png')
    # The shard on the altar: a curved piece of the bell's lip lying on its rim, a second piece leaning up against it.
    g.save(shard(), g.ASSETS / 'textures/block/bell_shard.png')
    faces = lambda uv: {f: {'texture': '#s', 'uv': uv} for f in ('north', 'south', 'east', 'west', 'up', 'down')}
    g.write_json(g.ASSETS / 'models/block/bell_shard.json', {
        'parent': 'minecraft:block/block', 'textures': {'particle': 'wildercord:block/bell_shard', 's': 'wildercord:block/bell_shard'},
        'elements': [
            {'from': [4, 0, 6], 'to': [12, 3, 10], 'faces': faces([0, 0, 8, 4])},
            {'from': [5, 0, 8], 'to': [11, 9, 10], 'rotation': {'origin': [8, 0, 9], 'axis': 'x', 'angle': -22.5}, 'faces': faces([8, 0, 14, 9])},
            {'from': [9, 0, 4], 'to': [12, 2, 7], 'rotation': {'origin': [10, 0, 5], 'axis': 'y', 'angle': 22.5}, 'faces': faces([0, 10, 3, 12])},
        ]})
    g.write_json(g.ASSETS / 'blockstates/bell_shard.json', {'variants': {'': {'model': 'wildercord:block/bell_shard'}}})
    g.write_json(g.ASSETS / 'items/bell_shard.json', {'model': {'type': 'minecraft:model', 'model': 'wildercord:block/bell_shard'}})


def shard():
    """The Bell Shard, 16 x 16: bronze gone green, worn bright along the rim, the broken edge raw and pale, a cold glow in the crack."""
    rng = np.random.default_rng(20261012)
    px = np.zeros((16, 16, 4), np.uint8)
    for y in range(16):
        for x in range(16):
            t = rng.random()
            c = mixc(BRONZE, BRONZE_DARK, .3 + .4 * t)
            if rng.random() < .3:
                c = mixc(c, VERDIGRIS, .5 + .4 * rng.random())
            px[y, x] = (*c, 255)
    # The rim worn bright, along the top of the lying piece and the leaning one.
    for x in range(0, 16):
        px[0, x, :3] = mixc(BRONZE, (220, 180, 110), .6)
    # The broken edge, raw bronze, and the crack glowing.
    for y in range(0, 9):
        px[y, 13, :3] = (214, 170, 100)
    for y, x in ((2, 10), (3, 10), (4, 11), (5, 11), (6, 10), (2, 3), (3, 4)):
        px[y, x, :3] = mixc(EYE_HALO, EYE, .5)
    return Image.fromarray(px, 'RGBA')
