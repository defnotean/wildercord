"""The Rootcoil Serpent and its grove: a jungle sinkhole, a cavern with a water ring, the tree the serpent coils round, and
the serpent's own skin."""
import numpy as np
from PIL import Image

LANG = {
    'entity.wildercord.rootcoil_serpent': 'Rootcoil Serpent',
    'guide.wildercord.rootcoil_serpent': 'A serpent thirty blocks long, coiled round the tree at the heart of a flooded cavern. '
                                         'It sleeps until something comes within a few strides of its tree, then slides down to meet it. '
                                         'Leave the grove and it climbs back, healed.',
    'guide.wildercord.rootcoil_serpent.hint': 'A sinkhole in the jungle floor drops to a cavern with a ring of water round a mossy island.',
    'boss.wildercord.rootcoil_serpent.status': '%s — %s',
}
for name, text in {
    'hiss': 'Serpent hisses', 'wake': 'Serpent stirs', 'uncoil': 'Serpent uncoils', 'challenge': 'Serpent rears',
    'hurt': 'Serpent hurts', 'death': 'Serpent dies', 'strike': 'Serpent strikes', 'lunge': 'Serpent lunges',
    'constrict': 'Serpent coils tighten', 'sidestep': 'Serpent slips aside', 'dive': 'Serpent dives', 'burst': 'Serpent bursts from the water',
}.items():
    LANG['subtitles.wildercord.kit.serpent.' + name] = text

# What the bar says it is doing, the way a Master's bar names its cut.
for name, text in {
    'watch': 'Watching', 'strike': 'Striking', 'lunge': 'Lunging', 'constrict': 'Coiling to grab', 'squeeze': 'Constricting',
    'sidestep': 'Slipping aside', 'recover': 'Belly bare', 'dive': 'Diving', 'submerged': 'Circling below', 'burst': 'Bursting out',
}.items():
    LANG['boss.wildercord.rootcoil_serpent.move.' + name] = text
LANG['message.wildercord.rootcoil_serpent.caught'] = 'Caught in its coils! Jump or strafe to struggle free.'
LANG['message.wildercord.rootcoil_serpent.struggle'] = 'Struggling… %s/%s'
LANG['message.wildercord.rootcoil_serpent.free'] = 'You break free!'

GROVE_BIOMES = ['minecraft:jungle', 'minecraft:sparse_jungle', 'minecraft:bamboo_jungle']

# The skin's palette: dark olive along the spine, moss down the flanks, a pale ochre belly, bone horns.
SPINE, FLANK, SADDLE, BELLY, SCUTE = (38, 60, 30), (78, 118, 56), (28, 40, 22), (204, 190, 128), (160, 146, 92)
HORN, FANG, MOUTH, THROAT = (176, 160, 122), (236, 228, 204), (150, 62, 66), (92, 30, 36)


def write(g):
    g.write_json(g.DATA / 'worldgen/structure/serpent_grove.json', {
        'type': 'wildercord:dungeon', 'dungeon': 'serpent_grove', 'biomes': '#wildercord:has_structure/serpent_grove',
        'spawn_overrides': {}, 'step': 'surface_structures', 'terrain_adaptation': 'none'})
    g.write_json(g.DATA / 'worldgen/structure_set/serpent_groves.json', {
        'placement': {'type': 'minecraft:random_spread', 'salt': 202610111, 'spacing': 44, 'separation': 16},
        'structures': [{'structure': 'wildercord:serpent_grove', 'weight': 1}]})
    g.write_json(g.DATA / 'tags/worldgen/biome/has_structure/serpent_grove.json', {'replace': False, 'values': GROVE_BIOMES})
    g.write_json(g.DATA / 'tags/worldgen/structure/serpent_grove.json', {'replace': False, 'values': ['wildercord:serpent_grove']})
    # What the last party to come down left in the tunnel: their supplies, not the serpent's.
    g.write_json(g.DATA / 'loot_table/chests/serpent_grove.json', {'type': 'minecraft:chest', 'pools': [
        {'rolls': {'type': 'minecraft:uniform', 'min': 3, 'max': 5}, 'entries': [
            {'type': 'minecraft:item', 'name': item, 'weight': w,
             'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]}
            for item, w, lo, hi in [('minecraft:arrow', 4, 4, 12), ('minecraft:bone', 4, 2, 6), ('minecraft:cocoa_beans', 3, 2, 5),
                                    ('minecraft:melon_slice', 3, 2, 6), ('minecraft:gold_nugget', 3, 3, 9), ('minecraft:emerald', 2, 1, 3),
                                    ('minecraft:string', 3, 2, 5), ('minecraft:golden_apple', 1, 1, 1)]]},
        {'rolls': {'type': 'minecraft:uniform', 'min': 0, 'max': 1}, 'entries': [{'type': 'minecraft:item', 'name': 'wildercord:aura_shard'}]}]})
    # The serpent drops nothing itself: what it guards is the herb.
    g.write_json(g.DATA / 'loot_table/entities/rootcoil_serpent.json', {'type': 'minecraft:entity', 'pools': []})
    g.save(skin(), g.ASSETS / 'textures/entity/rootcoil_serpent/skin.png')
    g.save(glow(), g.ASSETS / 'textures/entity/rootcoil_serpent/glow.png')


def mixc(a, b, t):
    t = np.clip(t, 0, 1)[..., None]
    return np.asarray(a, float) * (1 - t) + np.asarray(b, float) * t


def scales(w, h, cell, rng):
    """Overlapping diamond scales over a w x h patch, tiling at the patch's edges: 1 at a scale's crown, 0 in the seams."""
    y, x = np.mgrid[0:h, 0:w].astype(float)
    row = np.floor(y / cell)
    u = (x / cell + 0.5 * (row % 2)) % 1 - 0.5
    v = (y / cell) % 1 - 0.5
    crown = 1 - (np.abs(u) + np.abs(v)) * 1.25
    return np.clip(crown + rng.normal(0, 0.08, (h, w)), 0, 1)


def body(rng):
    """The tube's band, 64 x 64: eight faces round (the spine down x = 16, the belly down x = 48), four segments tall,
    wrapping both ways so the segments and the seam run on unbroken."""
    y, x = np.mgrid[0:64, 0:64].astype(float)
    up = np.sin((x + 0.5) / 64 * 2 * np.pi)          # 1 along the spine, -1 under the belly
    base = mixc(FLANK, SPINE, (up - 0.1) / 0.8)
    # Dark diamonds down the spine, one per segment, and smaller blotches low on each flank between them.
    phase = (y % 16) - 7.5
    spine = np.minimum(np.abs(x + 0.5 - 16), 64 - np.abs(x + 0.5 - 16))
    diamond = np.abs(phase) / 7 + spine / 12
    base = mixc(base, SADDLE, np.clip((1 - diamond) * 6, 0, 0.8))
    rim = np.abs(diamond - 1) < 0.09
    base = np.where(rim[..., None], mixc(base, FLANK, 0.5) * 1.15, base)
    flank = np.minimum(np.abs(x + 0.5 - 0), np.abs(x + 0.5 - 32))
    flank = np.minimum(flank, np.abs(x + 0.5 - 64))
    blotch = np.abs(((y + 8) % 16) - 7.5) / 3.5 + flank / 4
    base = mixc(base, SADDLE, np.clip((1 - blotch) * 4, 0, 0.6))
    # Pale scutes under the belly: broad bands two rows apart with a darker seam.
    belly = np.clip((-up - 0.45) / 0.35, 0, 1)
    scute = mixc(BELLY, SCUTE, ((y % 4) == 3).astype(float))
    base = mixc(base, scute, belly)
    s = scales(64, 64, 4, rng)
    shade = (0.72 + 0.38 * s) * (1 - belly) + (0.88 + 0.14 * s) * belly
    rgb = base * shade[..., None]
    return np.clip(rgb, 0, 255)


def paint_box(px, tu, tv, w, h, d, top, bottom, side, rng, cell=3):
    """Fills one box's vanilla cube-layout region: `top` on the top face, `bottom` under it, the sides fading from one to the
    other top to bottom; all of it scaled."""
    regions = [((tu + d, tv), (w, d), top, None), ((tu + d + w, tv), (w, d), bottom, None),
               ((tu, tv + d), (d, h), None, side), ((tu + d, tv + d), (w, h), None, side),
               ((tu + d + w, tv + d), (d, h), None, side), ((tu + 2 * d + w, tv + d), (w, h), None, side)]
    for (x0, y0), (rw, rh), flat, fade in regions:
        if rw <= 0 or rh <= 0:
            continue
        if flat is not None:
            col = np.broadcast_to(np.asarray(flat, float), (rh, rw, 3))
        else:
            t = (np.arange(rh)[:, None] + 0.5) / rh * np.ones((1, rw))
            col = mixc(fade[0], fade[1], t)
        s = scales(rw, rh, cell, rng)
        px[y0:y0 + rh, x0:x0 + rw] = np.clip(col * (0.78 + 0.3 * s)[..., None], 0, 255)


def skin():
    rng = np.random.default_rng(20261011)
    px = np.zeros((64, 128, 3))
    alpha = np.zeros((64, 128))
    px[:, :64] = body(rng)
    alpha[:, :64] = 255
    boxes = [  # corner, size (w, h, d), top, bottom, side fade
        ((64, 0), (12, 8, 14), SPINE, BELLY, (SPINE, FLANK)),       # skull
        ((64, 22), (8, 5, 8), SPINE, BELLY, (FLANK, BELLY)),        # snout
        ((96, 32), (4, 2, 6), SADDLE, SPINE, (SADDLE, SPINE)),      # brow
        ((96, 22), (2, 2, 8), HORN, HORN, (HORN, (130, 116, 86))),  # horn
        ((120, 0), (1, 3, 1), FANG, FANG, (FANG, FANG)),            # fang
        ((64, 40), (10, 3, 20), MOUTH, BELLY, (FLANK, BELLY)),      # jaw: its top is the inside of the mouth
    ]
    for (tu, tv), (w, h, d), top, bottom, side in boxes:
        paint_box(px, tu, tv, w, h, d, top, bottom, side, rng)
        alpha[tv:tv + d + h, tu:tu + 2 * (d + w)] = 255
    # The skull's top: a dark blaze between the eyes running back to the neck.
    px[0:14, 64 + 14 + 4:64 + 14 + 8] = mixc(px[0:14, 82:86], SADDLE, 0.6)
    # The mouth's inside: a darker throat at the back of the jaw, the tongue groove down the middle.
    tu, tv, w, d = 64, 40, 10, 20
    px[tv:tv + 5, tu + d:tu + d + w] = THROAT
    px[tv:tv + d, tu + d + 4:tu + d + 6] = mixc(px[tv:tv + d, tu + d + 4:tu + d + 6], THROAT, 0.6)
    # Nostrils on the snout's front.
    tu, tv, w, d = 64, 22, 8, 8
    for nx in (tu + d + 1, tu + d + w - 2):
        px[tv + d + 1, nx] = SADDLE
    # Fangs taper: their tips a touch darker.
    px[3, 121] = (200, 190, 160)
    img = Image.fromarray(np.dstack([px, alpha]).astype(np.uint8), 'RGBA')
    return img


def glow():
    """Only the eyes glow: amber with a slit pupil, on the eye box's faces."""
    img = np.zeros((64, 128, 4), np.uint8)
    tu, tv, w, h, d = 120, 8, 1, 2, 3
    img[tv:tv + d + h, tu:tu + 2 * (d + w)] = (255, 196, 40, 255)
    # A dark slit down each side face's middle (the faces seen from the side are d x h).
    for x0 in (tu, tu + d + w):
        img[tv + d:tv + d + h, x0 + 1] = (40, 20, 0, 255)
    return Image.fromarray(img, 'RGBA')
