"""White Rabbit skin (WhiteRabbitModel 64x32 box-UV layout), animated swirling burrow portal, spawn egg, loot, spawn ground, and English text."""
from __future__ import annotations

import math

from item_art import Canvas, hexc, mix, ramp
from monster_art import faces
from world_art import Sheet, box, noise

LANG = {
    'entity.wildercord.white_rabbit': 'White Rabbit',
    'item.wildercord.white_rabbit_spawn_egg': 'White Rabbit Spawn Egg',
    'block.wildercord.rabbit_hole_portal': 'Rabbit-Hole Portal',
    'guide.wildercord.white_rabbit.hint': 'Keep your eyes sharp in the woods for a rushing white rabbit carrying a brass pocket watch.',
    'guide.wildercord.white_rabbit': (
        'A snowy white forest rabbit wearing a crimson collar ribbon and a brass pocket watch. '
        'Follow its sparkling trail through the woods as it hops toward an ancient hollow oak tree and leaps down inside. '
        'Step into the swirling Rabbit-Hole Portal to tumble either into a hidden underground cave, a stocked Wonderland treasure vault, '
        'or an abyssal colosseum where every Wildercord boss awakens at once.'
    ),
    'message.wildercord.white_rabbit.leading': 'A White Rabbit checks its pocket watch and bounds toward a hollow ancient tree...',
    'message.wildercord.rabbit_hole.cave': 'You tumble down the dark burrow into a quiet subterranean cavern!',
    'message.wildercord.rabbit_hole.treasure': 'You tumble through twisted roots into a glittering treasure vault!',
    'message.wildercord.rabbit_hole.boss_rush': 'You plummet into the Deep Colosseum — ALL BOSSES AWAKEN AT ONCE!',
    'message.wildercord.rabbit_hole.returned': 'You climb back up through the burrow into the forest.',
}

FUR = ramp('#CFCAD8', '#DFDCE8', '#ECEBF4', '#F6F5FC', '#FFFFFF')
BELLY = ramp('#DCD7E2', '#EAE6EF', '#F4F2F8', '#FAF9FD', '#FFFFFF')
PINK = ramp('#D88496', '#E498A8', '#F0AAB9', '#F8C0CC', '#FFD6DF')
RIBBON = ramp('#7C1420', '#9E1E2C', '#C42838', '#DE3E4E', '#F06472')
BRASS = ramp('#8A6218', '#B48422', '#D8A830', '#F0C848', '#FCE484')
EYE = hexc('#D81E38')
GLINT = hexc('#FFFFFF')
WATCH_FACE = hexc('#F7F1DC')
WATCH_HAND = hexc('#2A2018')

# Exact 64x32 UV layout matching WhiteRabbitModel
BODY = box(0, 0, 5, 5, 8)
HEAD = box(26, 0, 4, 4, 4)
MUZZLE = box(42, 0, 3, 2, 2)
EAR = box(52, 0, 2, 6, 1)
HAUNCH = box(0, 13, 2, 4, 5)
FOOT = box(14, 13, 2, 1, 5)
FORELEG = box(28, 13, 2, 5, 2)
TAIL = box(36, 13, 3, 3, 2)
COLLAR = box(26, 20, 5, 2, 2)
WATCH = box(42, 20, 3, 3, 1)

LIGHT = {'top': 1, 'bottom': -1, 'front': 0, 'back': -1, 'right': 0, 'left': -1}


def _shade_part(cv: Sheet, part: dict, tones: tuple, seed: int, belly_bottom: bool = False):
    for fname, area in faces(part):
        pal = BELLY if (belly_bottom and fname == 'bottom') else tones
        x0, y0, w, h = area
        for y in range(h):
            for x in range(w):
                idx = 2 + LIGHT.get(fname, 0)
                if noise(x0 + x, y0 + y, seed) > 0.72:
                    idx += 1
                elif noise(x0 + x, y0 + y, seed + 5) < 0.18:
                    idx -= 1
                cv.put(x0 + x, y0 + y, pal[max(0, min(len(pal) - 1, idx))])


def skin():
    cv = Sheet(64, 32)
    _shade_part(cv, BODY, FUR, 11, belly_bottom=True)
    _shade_part(cv, HEAD, FUR, 23, belly_bottom=True)
    _shade_part(cv, MUZZLE, BELLY, 31)
    _shade_part(cv, HAUNCH, FUR, 41)
    _shade_part(cv, FOOT, BELLY, 53)
    _shade_part(cv, FORELEG, BELLY, 61)
    _shade_part(cv, TAIL, BELLY, 73)
    _shade_part(cv, EAR, FUR, 83)
    _shade_part(cv, COLLAR, RIBBON, 97)
    _shade_part(cv, WATCH, BRASS, 109)

    # Ruby white-rabbit eyes + nose tip
    fx, fy, fw, fh = HEAD['front']
    cv.put(fx, fy + 2, EYE)
    cv.put(fx, fy + 1, GLINT)
    cv.put(fx + 3, fy + 2, EYE)
    cv.put(fx + 3, fy + 1, GLINT)
    for fname, col in (('right', 3), ('left', 0)):
        sx, sy, _, _ = HEAD[fname]
        cv.put(sx + col, sy + 2, EYE)
    mx, my, _, _ = MUZZLE['front']
    cv.put(mx + 1, my, PINK[2])

    # Pink inner ear strip
    ex, ey, ew, eh = EAR['front']
    for y in range(1, eh):
        cv.put(ex, ey + y, PINK[2 if y < eh - 1 else 1])

    # White dial & tiny clock hands inside brass pocket watch
    wx, wy, _, _ = WATCH['front']
    cv.put(wx + 1, wy + 1, WATCH_FACE)
    cv.put(wx + 1, wy + 2, WATCH_HAND)
    cv.put(wx + 2, wy + 1, WATCH_HAND)

    return cv.image()


def rabbit_hole_portal_frames():
    """Animated 6-frame swirling deep burrow vortex for the Hollow Ancient Tree portal."""
    frames = []
    outer = hexc('#090512')
    indigo = hexc('#281448')
    violet = hexc('#5C2B9E')
    bright = hexc('#A45CE6')
    gold = hexc('#F5D062')
    frames_count = 6
    for frame in range(frames_count):
        cv = Canvas(16)
        phase = (frame / frames_count) * math.pi * 2.0
        for y in range(16):
            for x in range(16):
                dx = x + 0.5 - 8.0
                dy = y + 0.5 - 8.0
                r = math.hypot(dx, dy)
                if r >= 7.8:
                    cv.put(x, y, outer)
                    continue
                ang = math.atan2(dy, dx)
                swirl = math.sin(ang * 3.0 - r * 0.9 + phase)
                if r < 1.9:
                    cv.put(x, y, gold if swirl > -0.2 else bright)
                elif r < 4.2:
                    cv.put(x, y, bright if swirl > 0.25 else (violet if swirl > -0.3 else indigo))
                else:
                    cv.put(x, y, violet if swirl > 0.35 else (indigo if r < 6.4 else outer))
        frames.append(cv.image())
    return frames


def white_rabbit_egg():
    cv = Canvas()
    base = hexc('#F2F0FA')
    rim = hexc('#8E869E')
    ribbon = hexc('#C42838')
    gold = hexc('#F0C848')
    for y in range(2, 15):
        ry = (y - 8.5) / 6.2
        half_w = max(1, int(round(4.6 * ((1.0 - ry * ry * 0.85) ** 0.5) * (0.82 if y < 6 else 1.0))))
        for x in range(8 - half_w, 8 + half_w + 1):
            edge = x == 8 - half_w or x == 8 + half_w or y == 2 or y == 14
            if edge:
                col = rim
            elif y in (8, 9):
                col = gold if x in (7, 8) and y == 9 else ribbon
            else:
                col = mix(base, (255, 255, 255), 0.3 if y < 6 else 0.0)
            cv.put(x, y, col)
    return cv.image()


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/wildlife/white_rabbit.png')

    g.save(rabbit_hole_portal_frames(), g.ASSETS / 'textures/block/rabbit_hole_portal.png')
    g.write_json(g.ASSETS / 'models/block/rabbit_hole_portal.json', {
        'parent': 'minecraft:block/cube_all',
        'textures': {'all': 'wildercord:block/rabbit_hole_portal'},
    })
    g.write_json(g.ASSETS / 'blockstates/rabbit_hole_portal.json', {
        'variants': {'': {'model': 'wildercord:block/rabbit_hole_portal'}},
    })
    g.write_json(g.ASSETS / 'items/rabbit_hole_portal.json', {
        'model': {'type': 'minecraft:model', 'model': 'wildercord:block/rabbit_hole_portal'},
    })

    egg_name = 'white_rabbit_spawn_egg'
    g.save(white_rabbit_egg(), g.ASSETS / f'textures/item/{egg_name}.png')
    g.item_model(egg_name, egg_name)
    g.write_json(g.ASSETS / f'items/{egg_name}.json', {
        'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{egg_name}'},
    })

    g.write_json(g.DATA / 'loot_table/entities/white_rabbit.json', {
        'type': 'minecraft:entity',
        'pools': [],
        'random_sequence': 'wildercord:entities/white_rabbit',
    })
    g.write_json(g.DATA / 'tags/block/spawns_on/white_rabbit.json', {
        'replace': False,
        'values': [
            'minecraft:grass_block',
            'minecraft:podzol',
            'minecraft:coarse_dirt',
            'minecraft:moss_block',
            'minecraft:dirt',
        ],
    })
