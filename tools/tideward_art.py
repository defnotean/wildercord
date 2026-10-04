"""The Tideward kit: Reedwater Waders, Dewglass Spectacles and the Bank Surveyor's Line.

Icons are 16x16 ASCII sprites in the established item style (item_art.Canvas/blit): a one-pixel dark outline tinted
to the object, lit from the top left, a few tones per material. The armour layers keep vanilla's 64x32 humanoid
layout and are shaded like vanilla's. The Surveyor's Line keeps its small 3D held model (bank_surveyors_line_held,
chosen by display context), whose wood, thread and brass textures are clean vanilla-like 16x16 materials.
"""
from PIL import Image, ImageDraw

from item_art import Canvas, blit, hexc, ramp
from world_art import Sheet, box

LANG = {
    'item.wildercord.reedwater_waders': 'Reedwater Waders',
    'item.wildercord.reedwater_waders.lore': 'Turned cuffs remember the banks, not the depths.',
    'item.wildercord.reedwater_waders.use': 'Feet slot. Walk on real shallow-water support without sprinting for a modest water movement aid. One extra wear per second of actual movement; five walking seconds require two seconds of shared rest. No breathing, damage or root immunity.',
    'item.wildercord.dewglass_spectacles': 'Dewglass Spectacles',
    'item.wildercord.dewglass_spectacles.lore': 'Tamsin repaired the left hinge with reed wire.',
    'item.wildercord.dewglass_spectacles.use': 'Head slot, no armour. Crouch and look at a visible Reedback warning within eight blocks for half a second. Physical reed notches identify raised claws. One wear and three seconds of shared rest. Walls, hidden animals and committed sweeps refuse.',
    'item.wildercord.bank_surveyors_line': "Bank Surveyor's Line",
    'item.wildercord.bank_surveyors_line.lore': 'The spool measures a crossing; it does not make one.',
    'item.wildercord.bank_surveyors_line.use': 'Use two visible supported banks within five seconds, up to eight blocks apart and one step high. Surveys a short direct shallow-water or dry route. Two wear, four-second shared rest, five-second private braid. Sneak-use cancels. Protected, deep, obstructed or unloaded cells refuse.',
    **{'message.wildercord.bank_line.' + k: v for k, v in {
        'rest': 'The line needs a moment to settle.',
        'bank': 'Choose a nearby visible solid bank with two clear body blocks.',
        'first': 'First peg observed. Choose the other bank within five seconds.',
        'crossing': 'No clear supported crossing: check depth, clearance, protection and distance.'}.items()},
    **{'subtitles.wildercord.kit.tideward.' + k: v for k, v in {
        'weave_rest': 'Reed cuffs draw tight',
        'glass_warning': 'Mended glass hinge ticks',
        'spool_commit': 'Braided line unwinds'}.items()},
}


def sprite(art, pal):
    cv = Canvas()
    blit(cv, art, {k: hexc(v) for k, v in pal.items()})
    return cv


# ============================================================== icons (16x16)

# One tall wader, toe to the left: a woven reed cuff, waxed olive leather, a copper-buckled strap, a dark sole.
WADER = """
    ...oooooo
    ...oYYYYo
    ...oyyyyo
    ...oggggo
    ...ohlmdo
    ...osCsso
    ...ohlmdo
    ..ohhlmdo
    .ohhllmdo
    ohhllmmdo
    oSSSSSSSo
    .ooooooo.
"""
WADER_FRONT = {"o": "#1C2410", "Y": "#F0DE96", "y": "#CDB862", "g": "#8E7A34", "h": "#94AE56", "l": "#6E8A38",
               "m": "#54702A", "d": "#3C5220", "s": "#5A3A1C", "C": "#F0A858", "S": "#3A2814"}
WADER_BACK = {"o": "#1C2410", "Y": "#CDB862", "y": "#AE9A48", "g": "#7A6828", "h": "#6E8A38", "l": "#54702A",
              "m": "#3C5220", "d": "#2C3C18", "s": "#4A2E16", "C": "#C88440", "S": "#2E2010"}


def waders_icon():
    cv = Canvas()
    blit(cv, WADER, {k: hexc(v) for k, v in WADER_BACK.items()}, 6, 1)
    blit(cv, WADER, {k: hexc(v) for k, v in WADER_FRONT.items()}, 0, 3)
    return cv.image()


# Round brass spectacles with dewy glass, the temple arms folded up behind them.
SPECTACLES = """
    ................
    ................
    ..o..........o..
    .oAo........oAo.
    .oAo........oAo.
    .oAo........oAo.
    .oAo........oAo.
    ..ooo......ooo..
    .oRRRo.oo.oRRRo.
    oRWGgRoBBoRWGgRo
    oRGggrooooRGggro
    oRgggro..oRgggro
    .orrro....orrro.
    ..ooo......ooo..
    ................
    ................
"""


def spectacles_icon():
    pal = {"o": "#2A1C0C", "A": "#D8A848", "R": "#F2CC6C", "r": "#A8782C", "B": "#E8BC5C", "b": "#B08434",
           "W": "#F4FFFF", "G": "#A8E6E2", "g": "#5EAEB8"}
    return sprite(SPECTACLES, pal).image()


# A wooden line winder: two bars with brass caps, a thick band of wound line, a crossbar below; the line runs out to a
# brass peg.
SPOOL = """
    ................
    ...oooo.oooo....
    ...oYyo.oYyo....
    ...obbo.obbo....
    ...oWdo.oWdo....
    ..oTTTTTTTTso...
    ..ottttttttso...
    ..oTTTTTTTTso...
    ..ottttttttso...
    ..oTTTTTTTTsol..
    ..ossssssssso.l.
    ...oWdo.oWdooYYo
    ...oWwWWWWdo.oyo
    ...oWddddddo.obo
    ...ooooooooo..o.
    ................
"""


def spool_icon():
    pal = {"o": "#2A1A0C", "W": "#D2A468", "w": "#B4844C", "d": "#7A5228", "T": "#F4ECD0", "t": "#D8CCA4",
           "s": "#A09068", "l": "#E4D8B0", "Y": "#F6D676", "y": "#D2A240", "b": "#8E6420"}
    return sprite(SPOOL, pal).image()


# ============================================================== armour layers (vanilla 64x32 humanoid layout)

def armour(kind):
    s = Sheet(64, 32)
    if kind == 'reedwater':
        # Hip waders on the leg box (texOffs 0,16; 4x12x4): woven reed cuff, waxed olive leather, strap, sole.
        cuff = ramp("#8E7A34", "#CDB862", "#F0DE96")
        leather = ramp("#2C3C18", "#3C5220", "#54702A", "#6E8A38", "#94AE56")
        strap, buckle, sole = hexc("#5A3A1C"), hexc("#F0A858"), hexc("#3A2814")
        faces = box(0, 16, 4, 12, 4)
        for name in ('right', 'front', 'left', 'back'):
            x0, y0, w, h = faces[name]
            edge = {'front': 3, 'right': 2, 'left': 2, 'back': 1}[name]
            for y in range(h):
                for x in range(w):
                    if y < 3:
                        c = cuff[2 if y == 0 else 1 if (x + y) % 2 else 2]
                        if y == 2 and x % 2 == 1:
                            c = cuff[0]
                    elif y == 3:
                        c = cuff[0]
                    elif y == 7:
                        c = buckle if (name in ('front', 'right') and x == 1) else strap
                    elif y == h - 1:
                        c = sole
                    else:
                        t = edge + (1 if y == 4 else -1 if y == h - 2 else 0)
                        if name == 'back' and x == 1 and y > 4:
                            t -= 1                        # the back seam
                        c = leather[max(0, min(4, t))]
                    s.put(x0 + x, y0 + y, c)
        x0, y0, w, h = faces['bottom']
        for y in range(h):
            for x in range(w):
                s.put(x0 + x, y0 + y, sole)
    else:
        # Spectacles on the head box (texOffs 0,0; 8x8x8): brass rims round the eyes, a glint over each eye's white
        # (the pupils stay visible), temple arms back to the ears. No opaque face box.
        rim, rim_dark, glint = hexc("#F2CC6C"), hexc("#A8782C"), hexc("#C8F4F2")
        faces = box(0, 0, 8, 8, 8)
        x0, y0, _, _ = faces['front']
        for x, y, c in [(1, 3, rim), (2, 3, rim), (5, 3, rim), (6, 3, rim),
                        (0, 4, rim), (3, 4, rim), (4, 4, rim), (7, 4, rim_dark),
                        (1, 5, rim_dark), (2, 5, rim_dark), (5, 5, rim_dark), (6, 5, rim_dark),
                        (1, 4, glint), (6, 4, glint)]:
            s.put(x0 + x, y0 + y, c)
        for name in ('right', 'left'):
            x0, y0, w, _ = faces[name]
            for k in range(6):
                x = (w - 1 - k) if name == 'right' else k
                s.put(x0 + x, y0 + 4, rim if k < 5 else rim_dark)
            x = (w - 6) if name == 'right' else 5
            s.put(x0 + x, y0 + 5, rim_dark)               # the hook behind the ear
    return s.image()


# ============================================================== particles (unchanged material sprites)

def particle(name):
    im = Image.new('RGBA', (16, 16))
    d = ImageDraw.Draw(im)
    if name == 'notch':
        d.polygon([(3, 14), (4, 3), (8, 1), (11, 5), (10, 13), (8, 15)], fill='#53644B')
        d.polygon([(4, 12), (5, 4), (8, 2), (8, 14)], fill='#D8D2A2')
        d.line((6, 5, 6, 11), fill='#7D8A5E')
        d.line((8, 6, 10, 7), fill='#9CAC77')
        d.point((5, 4), fill='#EFE6B8')
    elif name == 'braid':
        for x in range(2, 14):
            y = 7 + (x % 4 // 2)
            d.point((x, y), fill='#D5CA99')
            d.point((x, y + 1), fill='#718258')
            d.point((x, y - 1), fill='#9EAB76')
    else:
        d.polygon([(6, 2), (10, 2), (11, 5), (9, 7), (9, 13), (7, 15), (6, 12)], fill='#594D35')
        d.polygon([(7, 3), (9, 3), (9, 6), (8, 8), (8, 13), (7, 12)], fill='#DFCA8F')
        d.line((5, 6, 11, 6), fill='#A59461')
    return im


# ============================================================== held-model materials (16x16, vanilla-like)

WOOD = ramp("#6E4A26", "#8E6434", "#AC7C44", "#C49458", "#DCB070")
THREAD = ramp("#9C8C64", "#C4B48A", "#DCD0AA", "#F2EAD0")
BRASS = ramp("#7A5418", "#A87A28", "#D2A23C", "#EEC65E", "#FFE69A")
# Plank-like grain: a tone per column, a few long darker grain lines and two small knots.
WOOD_COLS = [3, 2, 2, 1, 2, 3, 3, 2, 2, 2, 1, 2, 3, 2, 2, 1]


def spool_material(name):
    im = Image.new('RGBA', (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            if name == 'wood':
                t = WOOD_COLS[x]
                if x in (3, 10, 15) and (y + x) % 7 < 5:
                    t = 0 if x != 15 else 1
                if (x, y) in ((6, 4), (7, 4), (12, 11)):
                    t = 0
                if (x, y) in ((6, 3), (12, 10)):
                    t = 4
                c = WOOD[t]
            elif name == 'thread':
                # Wound line: bands of turns, each lit along its top, a groove between them.
                r = y % 4
                c = THREAD[3 if r == 0 else 2 if r == 1 else 1 if r == 2 else 0]
                if r == 1 and (x + y // 4) % 5 == 0:
                    c = THREAD[1]
            else:
                # A brass plate with a bevel lit from the top left.
                t = 2
                if x == 0 or y == 0:
                    t = 4
                elif x == 15 or y == 15:
                    t = 0
                elif x == 1 or y == 1:
                    t = 3
                elif x == 14 or y == 14:
                    t = 1
                elif x + y in (9, 10):
                    t = 3                                  # a sheen across the face
                c = BRASS[t]
            px[x, y] = (*c, 255)
    return im


def held_spool():
    # Three wooden fork bars, bound spool waist, short closing peg. Separate material faces.
    def element(a, b, material):
        return {'from': a, 'to': b, 'faces': {f: {'uv': [0, 0, 16, 16], 'texture': '#' + material} for f in ('north', 'south', 'east', 'west', 'up', 'down')}}
    return {'textures': {'particle': 'wildercord:item/tideward_spool_wood', **{n: 'wildercord:item/tideward_spool_' + n for n in ('wood', 'thread', 'brass')}},
            'elements': [element([5, 3, 7], [7, 14, 9], 'wood'), element([10, 3, 7], [12, 14, 9], 'wood'), element([7, 5, 7], [10, 7, 9], 'wood'),
                         element([4.7, 7, 6.7], [12.3, 10.8, 9.3], 'thread'), element([11.8, 11, 7], [13, 12, 9], 'brass'), element([6, 2, 7], [11, 3.5, 9], 'brass')],
            'display': {'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [1, 1, -2], 'scale': [.7, .7, .7]},
                        'firstperson_lefthand': {'rotation': [0, 90, -25], 'translation': [1, 1, -2], 'scale': [.7, .7, .7]},
                        'thirdperson_righthand': {'rotation': [0, 90, 0], 'translation': [0, 2, 0], 'scale': [.65, .65, .65]},
                        'thirdperson_lefthand': {'rotation': [0, -90, 0], 'translation': [0, 2, 0], 'scale': [.65, .65, .65]}}}


def write(g):
    for name, draw in [('reedwater_waders', waders_icon), ('dewglass_spectacles', spectacles_icon), ('bank_surveyors_line', spool_icon)]:
        g.save(draw(), g.ASSETS / f'textures/item/{name}.png')
        g.item_model(name, name)
        g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    for name in ('reedwater', 'dewglass'):
        g.save(armour(name), g.ASSETS / f'textures/entity/equipment/humanoid/{name}.png')
        g.write_json(g.ASSETS / f'equipment/{name}.json', {'layers': {'humanoid': [{'texture': f'wildercord:{name}'}]}})
    for name in ('notch', 'braid', 'peg'):
        g.save(particle(name), g.ASSETS / f'textures/particle/tideward_{name}.png')
    for name in ('wood', 'thread', 'brass'):
        g.save(spool_material(name), g.ASSETS / f'textures/item/tideward_spool_{name}.png')
    g.write_json(g.ASSETS / 'models/item/bank_surveyors_line_held.json', held_spool())
    # Actual26.3 spyglass descriptor verifies these display contexts and separate held model.
    g.write_json(g.ASSETS / 'items/bank_surveyors_line.json', {'model': {'type': 'minecraft:select', 'property': 'minecraft:display_context',
        'cases': [{'when': ['gui', 'ground', 'fixed', 'on_shelf'], 'model': {'type': 'minecraft:model', 'model': 'wildercord:item/bank_surveyors_line'}}],
        'fallback': {'type': 'minecraft:model', 'model': 'wildercord:item/bank_surveyors_line_held'}}})
    g.write_json(g.DATA / 'tags/item/repairs_reedwater.json', {'replace': False, 'values': ['wildercord:moonreed_floss']})
    g.write_json(g.DATA / 'tags/item/repairs_dewglass.json', {'replace': False, 'values': ['minecraft:glass_pane']})
    import json
    for tag, item in [('foot_armor', 'reedwater_waders'), ('head_armor', 'dewglass_spectacles')]:
        target = g.RES / f'data/minecraft/tags/item/{tag}.json'
        prior = json.loads(target.read_text(encoding='utf-8')) if target.exists() else {'replace': False, 'values': []}
        prior['values'] = sorted(set(prior['values'] + [f'wildercord:{item}']))
        g.write_json(target, prior)
    recipes = {
        'reedwater_waders': ['minecraft:leather_boots', 'wildercord:moonreed_floss', 'wildercord:windreed_braid', 'minecraft:copper_ingot'],
        'dewglass_spectacles': ['minecraft:glass_pane', 'minecraft:copper_ingot', 'wildercord:moonreed_floss'],
        'bank_surveyors_line': ['minecraft:stick', 'minecraft:copper_ingot', 'wildercord:windreed_braid', 'wildercord:moonreed_floss']}
    for name, inputs in recipes.items():
        g.write_json(g.DATA / f'recipe/{name}.json', {'type': 'minecraft:crafting_shapeless', 'category': 'equipment', 'ingredients': inputs, 'result': {'id': f'wildercord:{name}', 'count': 1}})
        g.unlock_advancement(f'wildercord:{name}', 'wildercord:moonreed_floss')
