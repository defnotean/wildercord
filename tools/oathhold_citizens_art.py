"""Procedural pixel art, block models, spawn egg, and English text for Oathhold's 16 Citizen Archetypes and Adventurer's Guild Request Board."""
from __future__ import annotations

from PIL import Image

from item_art import Canvas, hexc, mix
from town_art import WAX, planks
from world_art import box, noise

LANG = {
    'entity.wildercord.oathhold_citizen': 'Oathhold Citizen',
    'item.wildercord.oathhold_citizen_spawn_egg': 'Oathhold Citizen Spawn Egg',
    'block.wildercord.guild_request_board': "Adventurer's Guild Request Board",
    'container.wildercord.guild_request_board': "Adventurer's Guild Requests (F–S)",
}

SKIN_TONES = (
    ((238, 198, 168), (218, 174, 142), (186, 140, 108)),
    ((224, 176, 134), (198, 148, 108), (168, 118, 82)),
    ((186, 132, 92), (158, 108, 72), (128, 84, 54)),
    ((124, 82, 54), (98, 64, 40), (76, 48, 30)),
)

# (archetype_id, primary_rgb, secondary_rgb, accent_rgb, hair_rgb, eye_rgb, hat_rgb, skin_idx)
PROFILES = (
    ('guild_receptionist', '#1B6E4A', '#F2E7D0', '#E8B838', '#4A2B18', '#226655', '#E8B838', 0),
    ('guild_master', '#7D1B22', '#26242E', '#F5C438', '#D8D4DE', '#E89028', '#F5C438', 1),
    ('blacksmith', '#3D373B', '#6A4128', '#C87638', '#2A221E', '#4A4038', '#4E4448', 2),
    ('alchemist', '#285E6E', '#D8E8D2', '#64D870', '#5E3828', '#40C8B0', '#2E7258', 0),
    ('baker', '#D6B88A', '#F4F0E8', '#C64830', '#6A4024', '#406080', '#FAF8F4', 1),
    ('clothier', '#52347A', '#E6D8EC', '#E8B838', '#382028', '#7850A8', '#684496', 0),
    ('fishmonger', '#28527C', '#B8D4E6', '#E09038', '#3E2B1E', '#3A7EA8', '#204264', 2),
    ('scholar', '#263C74', '#E8DEC8', '#D8AC38', '#2A2428', '#4878D0', '#324A88', 0),
    ('banker', '#1E3C32', '#E8DFCC', '#FFD44A', '#443228', '#388858', '#2A4E42', 1),
    ('arena_promoter', '#8E1E28', '#E8D8BE', '#FFC83A', '#1E1A20', '#D03838', '#FFD048', 2),
    ('artisan', '#6B4E34', '#D4C2A2', '#A8947A', '#4A3220', '#527080', '#7E5E40', 1),
    ('cleric', '#E6E2DC', '#263864', '#F4C642', '#705848', '#4888D8', '#F4C642', 0),
    ('watchman', '#3A4658', '#1A5E3E', '#D8AE38', '#2A221E', '#4C749E', '#707C8E', 2),
    ('sweeper', '#5C4B3B', '#9E8C72', '#C49A54', '#58422C', '#5C6C54', '#745E46', 3),
    ('townsfolk', '#4A6858', '#C8BA9E', '#8E643B', '#482F1E', '#486850', '#685038', 1),
    ('noble', '#5E244C', '#ECE2D0', '#F0C040', '#2A1D28', '#7848A0', '#7A3064', 0),
)


def _tone(rgb_tuple, factor: float):
    return (
        max(0, min(255, int(rgb_tuple[0] * factor))),
        max(0, min(255, int(rgb_tuple[1] * factor))),
        max(0, min(255, int(rgb_tuple[2] * factor))),
        255,
    )


def _paint_box(img: Image.Image, u: int, v: int, w: int, h: int, d: int, base_rgb, accent_rgb=None, seed: int = 7, belt: bool = False):
    px = img.load()
    faces = box(u, v, w, h, d)
    shade = {'top': 1.10, 'bottom': 0.72, 'front': 1.0, 'right': 0.90, 'left': 0.90, 'back': 0.84}
    for fname, (x0, y0, fw, fh) in faces.items():
        if fw <= 0 or fh <= 0:
            continue
        f_base = shade.get(fname, 0.92)
        for ly in range(fh):
            for lx in range(fw):
                jitter = 1.0 + (noise(x0 + lx, y0 + ly, seed) - 0.5) * 0.08
                col = base_rgb
                if belt and fname != 'top' and fname != 'bottom' and ly == fh - 3:
                    col = accent_rgb if accent_rgb else base_rgb
                elif accent_rgb and fname == 'front' and (lx == 0 or lx == fw - 1) and ly < 2:
                    col = accent_rgb
                px[x0 + lx, y0 + ly] = _tone(col, f_base * jitter)


def citizen_texture(archetype_id: str, prim_hex: str, sec_hex: str, acc_hex: str,
                    hair_hex: str, eye_hex: str, hat_hex: str, skin_idx: int, idx: int) -> Image.Image:
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    px = img.load()
    skin_hi, skin_mid, skin_lo = SKIN_TONES[skin_idx % len(SKIN_TONES)]
    prim = hexc(prim_hex)
    sec = hexc(sec_hex)
    acc = hexc(acc_hex)
    hair = hexc(hair_hex)
    eye = hexc(eye_hex)
    hat = hexc(hat_hex)
    boots = (44, 34, 28)

    # Head base (0, 0, 8, 8, 8)
    _paint_box(img, 0, 0, 8, 8, 8, skin_mid, seed=10 + idx)
    head_faces = box(0, 0, 8, 8, 8)
    fx, fy, fw, fh = head_faces['front']
    for lx in range(fw):
        px[fx + lx, fy] = _tone(hair, 1.0)
        if lx == 0 or lx == fw - 1:
            px[fx + lx, fy + 1] = _tone(hair, 0.92)
    # Eyes & subtle mouth
    px[fx + 1, fy + 4] = (246, 246, 246, 255)
    px[fx + 2, fy + 4] = _tone(eye, 1.0)
    px[fx + 5, fy + 4] = _tone(eye, 1.0)
    px[fx + 6, fy + 4] = (246, 246, 246, 255)
    px[fx + 3, fy + 6] = (*skin_lo, 255)
    px[fx + 4, fy + 6] = (*skin_lo, 255)
    for fside in ('top', 'back', 'left', 'right'):
        sx, sy, sw, sh = head_faces[fside]
        rows = sh if fside in ('top', 'back') else 4
        for ly in range(rows):
            for lx in range(sw):
                px[sx + lx, sy + ly] = _tone(hair, 1.04 if fside == 'top' else 0.88)

    # Role headwear / hat overlay (32, 0, 8, 8, 8)
    if archetype_id != 'townsfolk':
        hat_faces = box(32, 0, 8, 8, 8)
        for fname, (hx0, hy0, hw, hh) in hat_faces.items():
            if fname == 'bottom':
                continue
            for ly in range(hh):
                for lx in range(hw):
                    if fname == 'front' and ly > 2:
                        continue
                    c = acc if (ly == 2 and fname != 'top') else hat
                    px[hx0 + lx, hy0 + ly] = _tone(c, 1.02 if fname == 'top' else 0.94)

    # Torso (16, 16, 8, 12, 4) with shirt collar, waistcoat & belt
    _paint_box(img, 16, 16, 8, 12, 4, prim, accent_rgb=acc, seed=40 + idx, belt=True)
    bfx, bfy, _, _ = box(16, 16, 8, 12, 4)['front']
    for lx in (3, 4):
        px[bfx + lx, bfy] = _tone(sec, 1.0)
        px[bfx + lx, bfy + 1] = _tone(sec, 0.96)
        px[bfx + lx, bfy + 2] = _tone(sec, 0.92)
    px[bfx + 3, bfy + 9] = _tone(acc, 1.08)
    px[bfx + 4, bfy + 9] = _tone(acc, 1.08)

    # Tunic skirt / apron overlay (40, 16, 8, 6, 4)
    _paint_box(img, 40, 16, 8, 6, 4, mix(prim, sec, 0.28), accent_rgb=acc, seed=60 + idx)

    # Legs with trousers & sturdy dark-leather boots
    for (lu, lv) in ((0, 16), (16, 48)):
        _paint_box(img, lu, lv, 4, 12, 4, mix(prim, (42, 40, 48), 0.55), seed=80 + idx)
        for fname, (ax0, ay0, aw, ah) in box(lu, lv, 4, 12, 4).items():
            if fname == 'top':
                continue
            start_y = 0 if fname == 'bottom' else ah - 3
            for ly in range(start_y, ah):
                for lx in range(aw):
                    px[ax0 + lx, ay0 + ly] = _tone(boots, 1.0)

    # Arms with rolled cuffs & skin hands
    for (au, av) in ((40, 32), (32, 48)):
        _paint_box(img, au, av, 4, 12, 4, mix(prim, sec, 0.22), seed=100 + idx)
        for fname, (ax0, ay0, aw, ah) in box(au, av, 4, 12, 4).items():
            if fname == 'top':
                continue
            start_y = 0 if fname == 'bottom' else ah - 3
            for ly in range(start_y, ah):
                for lx in range(aw):
                    c = acc if (ly == ah - 4 and fname != 'bottom') else skin_hi
                    px[ax0 + lx, ay0 + ly] = _tone(c, 0.96)

    # Held props: prop_rod (0, 32, 1, 1, 10), prop_head (8, 32, 3, 4, 2), ledger (20, 32, 5, 6, 1)
    _paint_box(img, 0, 32, 1, 1, 10, (112, 74, 42), seed=120 + idx)
    _paint_box(img, 8, 32, 3, 4, 2, acc if archetype_id in ('watchman', 'arena_promoter') else (150, 158, 168), seed=130 + idx)
    _paint_box(img, 20, 32, 5, 6, 1, (108, 42, 32), accent_rgb=acc, seed=140 + idx)

    return img


def guild_board_front() -> Image.Image:
    cv = planks(47)
    dark = hexc('#2E1E12')
    gold = hexc('#F2C242')
    emerald = hexc('#1E8C54')
    parchment = hexc('#EFE4C6')
    ink = hexc('#3A2E26')

    for i in range(16):
        cv.put(i, 0, dark)
        cv.put(i, 15, dark)
        cv.put(0, i, dark)
        cv.put(15, i, dark)
    # Emerald & gold Guild crest banner across top center
    for x in range(4, 12):
        cv.put(x, 1, emerald)
        cv.put(x, 2, gold if x in (6, 9) else emerald)
    cv.put(7, 2, gold)
    cv.put(8, 2, gold)

    # Four ranked quest parchments pinned on the wooden noticeboard
    notices = ((2, 4, 6, 8, WAX), (9, 4, 13, 8, hexc('#3874D0')),
               (2, 10, 6, 14, hexc('#E09A28')), (9, 10, 13, 14, emerald))
    for n, (x0, y0, x1, y1, seal) in enumerate(notices):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                cv.put(x, y, mix(parchment, (255, 255, 255), 0.15 if y == y0 else 0.0))
        for x in range(x0 + 1, x1):
            cv.put(x, y0 + 2, mix(ink, parchment, 0.35))
            if y0 + 3 < y1:
                cv.put(x, y0 + 3, mix(ink, parchment, 0.45))
        cv.put((x0 + x1) // 2, y0, seal)
    return cv.image()


def citizen_egg_icon() -> Image.Image:
    cv = Canvas()
    base = hexc('#1E744C')
    spot = hexc('#F0C444')
    outline = hexc('#103824')
    for y in range(2, 15):
        ry = (y - 8.5) / 6.2
        half_w = max(1, int(round(4.6 * ((1.0 - ry * ry * 0.85) ** 0.5) * (0.82 if y < 6 else 1.0))))
        for x in range(8 - half_w, 8 + half_w + 1):
            edge = x == 8 - half_w or x == 8 + half_w or y == 2 or y == 14
            spotted = (x in (6, 9, 10) and y in (6, 7, 10, 11))
            cv.put(x, y, outline if edge else spot if spotted else base)
    cv.put(7, 5, mix(base, (255, 255, 255), 0.45))
    return cv.image()


def write(g):
    for idx, (aid, p, s, a, hair, eye, hat, skin_idx) in enumerate(PROFILES):
        img = citizen_texture(aid, p, s, a, hair, eye, hat, skin_idx, idx)
        g.save(img, g.ASSETS / f'textures/entity/oathhold_citizen_{aid}.png')

    egg_name = 'oathhold_citizen_spawn_egg'
    g.save(citizen_egg_icon(), g.ASSETS / f'textures/item/{egg_name}.png')
    g.item_model(egg_name, egg_name)
    g.write_json(g.ASSETS / f'items/{egg_name}.json',
                 {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{egg_name}'}})

    g.save(guild_board_front(), g.ASSETS / 'textures/block/guild_request_board_front.png')
    g.save(planks(53).image(), g.ASSETS / 'textures/block/guild_request_board_side.png')
    face = {'texture': '#front'}
    side = {'texture': '#side'}
    g.write_json(g.ASSETS / 'models/block/guild_request_board.json', {
        'parent': 'minecraft:block/block',
        'textures': {
            'particle': 'wildercord:block/guild_request_board_side',
            'front': 'wildercord:block/guild_request_board_front',
            'side': 'wildercord:block/guild_request_board_side',
        },
        'elements': [{
            'from': [0, 0, 6],
            'to': [16, 16, 10],
            'faces': {
                'north': {**face, 'uv': [0, 0, 16, 16]},
                'south': {**face, 'uv': [0, 0, 16, 16]},
                'east': {**side, 'uv': [6, 0, 10, 16]},
                'west': {**side, 'uv': [6, 0, 10, 16]},
                'up': {**side, 'uv': [0, 6, 16, 10]},
                'down': {**side, 'uv': [0, 6, 16, 10]},
            },
        }],
    })
    g.write_json(g.ASSETS / 'blockstates/guild_request_board.json', {
        'variants': {
            f'facing={d}': {'model': 'wildercord:block/guild_request_board', **({'y': r} if r else {})}
            for d, r in (('north', 0), ('east', 90), ('south', 180), ('west', 270))
        }
    })
    g.write_json(g.ASSETS / 'items/guild_request_board.json', {
        'model': {'type': 'minecraft:model', 'model': 'wildercord:block/guild_request_board'}
    })
