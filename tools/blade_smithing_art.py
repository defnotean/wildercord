"""0.13 blade smithing: Master's Steel, the sliver a Sword Master's blade leaves, and the words for tempering with it.

The numbers live in src/main/java/dev/wildercord/aura/BladeSmithingRules.java.
"""
from item_art import Canvas, blit, hexc

LANG = {
    'item.wildercord.master_steel': "Master's Steel",
    'item.wildercord.master_steel.use': 'Temper your bonded blade with it at an anvil',
    'message.wildercord.master_steel': "The Master's blade leaves you %s Master's Steel.",
    'tooltip.wildercord.blade_temper': 'Tempered %s: coated blows +%s%%',
}

# A broken-off tip of a Master's blade: bright folded steel with a ripple of temper colours along its edge, and a gold notch
# where it snapped from the guard.
STEEL = """
................
.............oo.
............oWo.
...........oWso.
..........oWsdo.
.........oWsdo..
........oWsbo...
.......oWsbo....
......oWspo.....
.....oWspo......
....oWsgo.......
...oWsgo........
..oGGgo.........
.oGYGo..........
.oGGo...........
..oo............
"""
COLOURS = {
    'o': hexc('#1E1C2A'), 'W': hexc('#FFFFFF'), 's': hexc('#C8CCDA'), 'd': hexc('#7A7E92'),
    'b': hexc('#6A8CE8'), 'p': hexc('#B07AE8'), 'g': hexc('#E8B85A'),
    'G': hexc('#B8862A'), 'Y': hexc('#FFE08A'),
}


def steel_icon():
    cv = Canvas()
    blit(cv, STEEL, COLOURS)
    return cv.image()


def write(g):
    g.save(steel_icon(), g.ASSETS / 'textures/item/master_steel.png')
    g.write_json(g.ASSETS / 'models/item/master_steel.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'wildercord:item/master_steel'}})
    g.write_json(g.ASSETS / 'items/master_steel.json', {'model': {'type': 'minecraft:model', 'model': 'wildercord:item/master_steel'}})
