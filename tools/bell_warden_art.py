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
}.items():
    LANG['subtitles.wildercord.kit.bell_warden.' + name] = text
# What the bar says it is doing, the way a Master's bar names its cut.
for name, text in {'watch': 'Watching'}.items():
    LANG['boss.wildercord.drowned_bell_warden.move.' + name] = text

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
        ((48, 0), (6, 18, 6), FLESH, FLESH_DARK, (FLESH, FLESH_DARK)),      # arms
        ((72, 0), (7, 6, 7), FLESH_DARK, FLESH_DARK, (FLESH, FLESH_DARK)),  # fists
        ((100, 0), (7, 12, 7), RAG, FLESH_DARK, (RAG, FLESH_DARK)),         # legs
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
    g.write_json(g.DATA / 'loot_table/entities/drowned_bell_warden.json', {'type': 'minecraft:entity', 'pools': []})
    g.save(skin(), g.ASSETS / 'textures/entity/drowned_bell_warden/skin.png')
    g.save(glow(), g.ASSETS / 'textures/entity/drowned_bell_warden/glow.png')
