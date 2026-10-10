"""0.12 "Tempering" camp cooking: the Camp Pot (block, model, recipe, loot) and its meals (icons, models, lang, tag)."""
from item_art import Canvas, hexc, mix
from world_art import noise

# id: (name, broth colour, garnish colour)
MEALS = {
    'hearty_stew': ('Hearty Stew', '#8A4A22', '#E08A2A'),
    'forager_soup': ('Forager Soup', '#9A7A52', '#C83A2A'),
    'hunters_skewer': ('Hunter\N{RIGHT SINGLE QUOTATION MARK}s Skewer', '#B0603A', '#F09A3A'),
    'salmon_chowder': ('Salmon Chowder', '#E8D8B8', '#F07A5A'),
    'pumpkin_porridge': ('Pumpkin Porridge', '#E08A28', '#F8D27A'),
    'sweetberry_tart': ('Sweetberry Tart', '#D8A868', '#C8203A'),
    'glowberry_broth': ('Glowberry Broth', '#C8A040', '#FFE070'),
    'miners_hash': ('Miner\N{RIGHT SINGLE QUOTATION MARK}s Hash', '#A8783A', '#6A4A2A'),
    'emberroot_curry': ('Emberroot Curry', '#D86A18', '#FFC040'),
    'frostfin_soup': ('Frostfin Soup', '#9AC8D8', '#E8F4FF'),
    'ruby_borscht': ('Ruby Borscht', '#A8183A', '#F0E0D8'),
    'honeyed_ham': ('Honeyed Ham', '#E8A040', '#F0C8A8'),
    'scholars_tea': ('Scholar\N{RIGHT SINGLE QUOTATION MARK}s Tea', '#9A68C8', '#E0C8FF'),
    'wanderers_stew': ('Wanderer\N{RIGHT SINGLE QUOTATION MARK}s Stew', '#7A5A38', '#E8C890'),
    'mutton_pottage': ('Mutton Pottage', '#9A6A48', '#D8C8A8'),
    'fishers_bouillabaisse': ('Fisher\N{RIGHT SINGLE QUOTATION MARK}s Bouillabaisse', '#D86848', '#F0A060'),
    'mana_risotto': ('Mana Risotto', '#E8E0C8', '#3A5AC8'),
    'starlit_consomme': ('Starlit Consomm\N{LATIN SMALL LETTER E WITH ACUTE}', '#5A3A78', '#F8E8FF'),
    'spiced_compote': ('Spiced Compote', '#B8402A', '#5A3018'),
    'duelists_broth': ('Duelist\N{RIGHT SINGLE QUOTATION MARK}s Broth', '#E8C870', '#F8F0D0'),
    'archmages_feast': ('Archmage\N{RIGHT SINGLE QUOTATION MARK}s Feast', '#C89A2A', '#B888F8'),
}

LANG = {
    'block.wildercord.camp_pot': 'Camp Pot',
    'effect.wildercord.nourished': 'Nourished',
    'effect.wildercord.focused': 'Focused',
    'tooltip.wildercord.meal': 'A camp meal:',
    'message.wildercord.pot.cookbook': 'The Camp Pot cookbook (a bowl in hand cooks; sneak with an empty hand to pick a meal):',
    'message.wildercord.pot.cold': 'The pot is cold: set it over a lit campfire, fire, magma or lava.',
    'message.wildercord.pot.nothing': 'Nothing in your pack makes a meal yet. Open the cookbook with an empty hand.',
    'message.wildercord.pot.chosen': 'The pot will cook: %s',
    'message.wildercord.pot.cooked': 'Cooked: %s',
}
for _id, (_name, _broth, _garnish) in MEALS.items():
    LANG[f'item.wildercord.{_id}'] = _name

BOWL = [hexc(c) for c in ('#3A2614', '#5A3A1E', '#7A5230', '#946638')]


def meal_icon(meal_id, broth, garnish):
    """A wooden bowl seen from the side and a little above, full to the rim with broth and flecked with its garnish."""
    cv = Canvas()
    broth, garnish = hexc(broth), hexc(garnish)
    seed = sum(ord(c) for c in meal_id)
    # The broth's surface: an ellipse across the top of the bowl.
    for y in range(4, 9):
        for x in range(2, 14):
            dx, dy = (x - 7.5) / 6.0, (y - 6.5) / 2.6
            if dx * dx + dy * dy <= 1:
                c = mix(broth, (255, 255, 255), 0.18) if y <= 5 else broth
                if noise(x, y, seed) > 0.8:
                    c = mix(broth, (0, 0, 0), 0.2)
                cv.put(x, y, c)
    for (x, y) in ((4, 6), (7, 5), (10, 6), (9, 7), (6, 7), (11, 5)):
        if noise(x, y, seed + 7) > 0.3:
            cv.put(x, y, garnish)
    cv.put(8, 6, mix(garnish, (255, 255, 255), 0.3))
    # The bowl: a rim and a rounded body narrowing to a foot.
    for x in range(1, 15):
        cv.put(x, 8, BOWL[3] if x < 12 else BOWL[2])
    widths = {9: (1, 14), 10: (2, 13), 11: (2, 13), 12: (3, 12), 13: (4, 11), 14: (5, 10)}
    for y, (a, b) in widths.items():
        for x in range(a, b + 1):
            t = (x - a) / max(1, b - a)
            c = BOWL[2] if t < 0.35 else BOWL[1] if t < 0.8 else BOWL[0]
            if x in (a, b) or y == 14:
                c = BOWL[0]
            cv.put(x, y, c)
    return cv.image()


IRON = [hexc(c) for c in ('#2A2C30', '#3E4146', '#55585E', '#6E7279', '#8A8E95')]


def pot_side():
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            c = IRON[2] if noise(x, y, 11) < 0.7 else IRON[1]
            if y in (0, 1):
                c = IRON[3]
            if y == 15:
                c = IRON[0]
            if x in (0, 15):
                c = IRON[1]
            cv.put(x, y, c)
    # A soot line low on the pot, and two rivets either side of a band.
    for x in range(16):
        if noise(x, 14, 3) > 0.4:
            cv.put(x, 13, IRON[0])
        cv.put(x, 4, IRON[1])
    cv.put(3, 4, IRON[4])
    cv.put(12, 4, IRON[4])
    return cv.image()


def pot_top():
    cv = Canvas()
    stew = hexc('#8A4A22')
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            rim = x in (1, 14) or y in (1, 14)
            if edge:
                cv.put(x, y, IRON[1])
            elif rim:
                cv.put(x, y, IRON[3])
            else:
                c = stew if noise(x, y, 5) < 0.75 else mix(stew, (255, 220, 160), 0.35)
                cv.put(x, y, c)
    return cv.image()


def pot_bottom():
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv.put(x, y, IRON[0] if noise(x, y, 9) < 0.6 else IRON[1])
    return cv.image()


def write(g):
    for meal_id, (_name, broth, garnish) in MEALS.items():
        g.save(meal_icon(meal_id, broth, garnish), g.ASSETS / f'textures/item/{meal_id}.png')
        g.item_model(meal_id, meal_id)
        g.write_json(g.ASSETS / f'items/{meal_id}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{meal_id}'}})
    g.write_json(g.DATA / 'tags/item/meals.json', {'replace': False, 'values': [f'wildercord:{m}' for m in MEALS]})

    g.save(pot_side(), g.ASSETS / 'textures/block/camp_pot_side.png')
    g.save(pot_top(), g.ASSETS / 'textures/block/camp_pot_top.png')
    g.save(pot_bottom(), g.ASSETS / 'textures/block/camp_pot_bottom.png')
    faces = lambda: {d: {'texture': '#side'} for d in ('north', 'south', 'east', 'west')}
    body = {'from': [2, 0, 2], 'to': [14, 9, 14], 'faces': {**faces(), 'down': {'texture': '#bottom'}, 'up': {'texture': '#top'}}}
    rim = {'from': [1, 9, 1], 'to': [15, 11, 15], 'faces': {**{d: {'texture': '#side', 'uv': [0, 0, 16, 2]} for d in ('north', 'south', 'east', 'west')},
           'up': {'texture': '#top'}, 'down': {'texture': '#bottom'}}}
    g.write_json(g.ASSETS / 'models/block/camp_pot.json', {'parent': 'minecraft:block/block', 'textures': {
        'particle': 'wildercord:block/camp_pot_side', 'side': 'wildercord:block/camp_pot_side', 'top': 'wildercord:block/camp_pot_top',
        'bottom': 'wildercord:block/camp_pot_bottom'}, 'elements': [body, rim]})
    g.write_json(g.ASSETS / 'blockstates/camp_pot.json', {'variants': {'': {'model': 'wildercord:block/camp_pot'}}})
    g.write_json(g.ASSETS / 'items/camp_pot.json', {'model': {'type': 'minecraft:model', 'model': 'wildercord:block/camp_pot'}})
    g.write_json(g.DATA / 'loot_table/blocks/camp_pot.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'bonus_rolls': 0,
        'entries': [{'type': 'minecraft:item', 'name': 'wildercord:camp_pot'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
        'random_sequence': 'wildercord:blocks/camp_pot'})
    g.write_json(g.DATA / 'recipe/camp_pot.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc',
        'pattern': ['I I', 'IBI', ' I '], 'key': {'I': 'minecraft:iron_ingot', 'B': 'minecraft:bowl'}, 'result': {'id': 'wildercord:camp_pot', 'count': 1}})
    g.unlock_advancement('wildercord:camp_pot', 'minecraft:iron_ingot')
