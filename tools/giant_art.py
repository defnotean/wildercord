"""0.13 roaming giants: the names they wake with, the messages they wake and fall to, and the Giant's Heart they drop."""
from item_art import Canvas, blit, hexc

LANG = {
    'monster.wildercord.giant.matriarch': 'Frost Matriarch',
    'monster.wildercord.giant.colossus': 'Ash Colossus',
    'monster.wildercord.giant.elder': 'Elder Bramblewalker',
    'message.wildercord.giant.wakes': 'The ground shakes: %s has woken to the %s.',
    'message.wildercord.giant.falls': '%s has fallen.',
    'item.wildercord.giant_heart': "Giant's Heart",
}

# A heart as big as a fist, still warm: deep red with a glowing core and a stub of vessel on top.
HEART = """
......vv..vv....
.....vVv.vVv....
...oo.vv.vv.oo..
..oRRo.vv..oRRo.
.oRRrroooooRRrro
.oRrrrrrrrrrrrro
.oRrrcCCcrrrrrro
.oRrrcCWCcrrrdro
.oRrrrcCcrrrrdro
..oRrrrrrrrrddo.
...oRrrrrrrddo..
....oRrrrrddo...
.....oRrrddo....
......oRddo.....
.......odo......
........o.......
"""
COLOURS = {
    'o': hexc("#3A0B10"), 'R': hexc("#D8475A"), 'r': hexc("#A81E32"), 'd': hexc("#6E1220"),
    'c': hexc("#E06A3A"), 'C': hexc("#FFB060"), 'W': hexc("#FFF0C0"),
    'v': hexc("#7A2A3A"), 'V': hexc("#B85468"),
}


def heart_icon():
    cv = Canvas()
    blit(cv, HEART, COLOURS)
    return cv.image()


def write(g):
    g.save(heart_icon(), g.ASSETS / 'textures/item/giant_heart.png')
    g.write_json(g.ASSETS / 'models/item/giant_heart.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'wildercord:item/giant_heart'}})
    g.write_json(g.ASSETS / 'items/giant_heart.json', {'model': {'type': 'minecraft:model', 'model': 'wildercord:item/giant_heart'}})
