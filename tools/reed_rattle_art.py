"""Handmade bank instrument: a worn clay seed chamber, reed handle and tied moonreed fibres."""
from PIL import Image, ImageDraw

LANG = {
    'item.wildercord.reed_rattle': 'Reed Rattle',
    'item.wildercord.reed_rattle.lore': 'Smooth river pebbles answer the reed crown inside a little clay chamber.',
    'item.wildercord.reed_rattle.use': 'Crouch and use on a visible Reedback Crab within three blocks while its claws warn. Six seconds of calm; 20-second rest; 48 uses. Cannot stop a committed sweep.',
    'message.wildercord.reed_rattle.crouch': 'Lower yourself first: crouch to answer the raised claws.',
    'message.wildercord.reed_rattle.reach': 'Move within three blocks, with a clear view of the crab.',
    'message.wildercord.reed_rattle.rest': 'Let the reed fibres rest: about %s seconds remain.',
    'message.wildercord.reed_rattle.warning': 'Answer raised warning claws; a committed sweep cannot be stopped.',
    'message.wildercord.reed_rattle.crab_rest': 'This crab has already answered recently. Give its reed crown time to settle.',
    'message.wildercord.reed_rattle.settle': 'The claws lower. Six quiet seconds to pass its bank.',
    'subtitles.wildercord.kit.wetland.reed_rattle': 'Clay rattle shakes',
}


def write(g):
    palettes = {'clay': (175, 112, 64), 'reed': (151, 146, 71), 'floss': (206, 220, 188),
                'dark': (65, 53, 33), 'pebble': (109, 127, 119)}
    for material, base in palettes.items():
        im = Image.new('RGBA', (32, 32)); px = im.load()
        for y in range(32):
            for x in range(32):
                fleck = ((x * 19 + y * 31 + x * y * 7) % 17 - 8) / 90
                edge = .82 if x in (0, 31) or y in (0, 31) else 1
                c = tuple(int(min(255, v * (edge + fleck))) for v in base)
                if material == 'reed':
                    c = tuple(int(v * (1.2 if x % 8 == 2 else .76 if x % 8 == 0 else 1)) for v in c)
                    if y in (6, 7, 24, 25): c = (87, 98, 48) if y in (7, 25) else (204, 189, 104)
                elif material == 'clay':
                    if y in (4, 26) and x % 8 < 4: c = (220, 170, 100)
                    if (x + y * 3) % 41 == 0: c = (109, 74, 43)
                    if y in (11, 12) and x % 8 in (2, 3, 4): c = (80, 107, 73)
                elif material == 'floss' and (x + y) % 7 < 2: c = (246, 243, 213)
                px[x, y] = (*c, 255)
        g.save(im, g.ASSETS / f'textures/item/reed_rattle_{material}.png')

    # A stepped rounded vessel, exposed pebbles, bamboo nodes and real fibre ties.
    elements = []
    def box(start, end, material):
        elements.append({'from': start, 'to': end, 'faces': {face: {'texture': '#' + material, 'uv': [0, 0, 16, 16]}
                         for face in ['up', 'down', 'north', 'south', 'east', 'west']}})
    def octagon(bottom, top, material, radius=3):
        inner = radius - 1
        box([8-radius, bottom, 8-inner], [8+radius, top, 8+inner], material)
        box([8-inner, bottom, 8-radius], [8+inner, top, 8+radius], material)
        # Rotated little prisms fill the corner wedges, leaving real beveled edges.
        half = 2 ** .5 / 2
        for x in [8-inner, 8+inner]:
            for z in [8-inner, 8+inner]:
                box([x-half, bottom, z-half], [x+half, top, z+half], material)
                elements[-1]['rotation']={'origin':[x,bottom,z],'axis':'y','angle':45}
    box([7, 1, 7], [9, 9, 9], 'reed')
    for y in [2.5, 5.5]: box([6.8, y, 6.8], [9.2, y + .55, 9.2], 'reed')
    box([6, 8, 6], [10, 9.5, 10], 'clay')
    octagon(9.5, 13, 'clay')
    box([5.6, 13, 5.6], [10.4, 14.3, 10.4], 'clay')
    box([6.5, 14.3, 6.5], [9.5, 14.6, 9.5], 'dark')
    for x, y, z in [(7, 14.5, 7), (8.4, 14.4, 8), (7.3, 14.4, 8.4)]: box([x, y, z], [x + .9, y + .5, z + .8], 'pebble')
    for y in [7.3, 8.1]: box([6.65, y, 6.65], [9.35, y + .35, 9.35], 'floss')
    box([6.3, 8, 6.45], [7.2, 8.8, 7.15], 'floss')
    box([5.6, 6.1, 6.7], [6.1, 8.3, 7.1], 'floss')
    box([5.3, 5.4, 6.5], [6.4, 6.2, 7.3], 'reed')
    # Thin reed bands crossing the seed chamber, an olive chevron on its face.
    for y in [10, 12]: octagon(y, y+.35, 'reed',3.05)
    for x, y in [(6.2, 11.3), (7.1, 10.8), (8, 10.8), (8.9, 11.3)]: box([x, y, 4.8], [x + .8, y + .45, 5], 'reed')
    g.write_json(g.ASSETS / 'models/item/reed_rattle.json', {
        'parent': 'minecraft:item/handheld', 'gui_light': 'front',
        'textures': {'particle': 'wildercord:item/reed_rattle_clay', **{m: f'wildercord:item/reed_rattle_{m}' for m in palettes}},
        'elements': elements,
        'display': {'gui': {'rotation': [18, -30, -25], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
                    'firstperson_righthand': {'rotation': [5, -20, 10], 'translation': [0, -1, -1], 'scale': [.5, .5, .5]},
                    'firstperson_lefthand': {'rotation': [5, 20, -10], 'translation': [0, -1, -1], 'scale': [.5, .5, .5]}}})
    g.write_json(g.ASSETS / 'items/reed_rattle.json', {'model': {'type': 'minecraft:model', 'model': 'wildercord:item/reed_rattle'}})
    g.write_json(g.DATA / 'recipe/reed_rattle.json', {'type': 'minecraft:crafting_shaped', 'category': 'equipment',
        'pattern': [' FC', 'SBC', ' B '], 'key': {'F': 'wildercord:moonreed_floss', 'C': 'minecraft:clay_ball',
        'S': 'minecraft:string', 'B': 'minecraft:bamboo'}, 'result': {'id': 'wildercord:reed_rattle'}})
    g.unlock_advancement('wildercord:reed_rattle', 'wildercord:moonreed_floss')
