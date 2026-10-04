"""Handmade bank instrument: a clay seed chamber, reed handle and tied moonreed fibres, as a flat 16x16 handheld item."""
from item_art import Canvas, blit, hexc

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


# A clay seed chamber banded with woven reed, tied with moonreed floss onto a jointed reed handle. Held like a tool.
RATTLE = """
    ................
    .........oooo...
    .......oohhlmoo.
    .......ohWhllmo.
    ......ohhllmmmdo
    ......oGGgGGgGgo
    ......ogggggggeo
    ......ollmmmmddo
    .......ommmdddo.
    ......ooomdddoo.
    .....oFfooooo...
    ....oRro........
    ...onNo.........
    ..oRro..........
    .oRro...........
    ..oo............
"""
RATTLE_PAL = {"o": "#3A1A0E", "W": "#FFE0B0", "h": "#EDA766", "l": "#D2834A", "m": "#B0612F", "d": "#844422",
              "G": "#B4C858", "g": "#7A9634", "e": "#4E6420", "R": "#D2D27A", "r": "#8E923C", "n": "#7A762E",
              "N": "#4E4A1C", "F": "#F6F2DE", "f": "#C4C4A8"}


def icon():
    cv = Canvas()
    blit(cv, RATTLE, {k: hexc(v) for k, v in RATTLE_PAL.items()})
    return cv.image()


def write(g):
    g.save(icon(), g.ASSETS / 'textures/item/reed_rattle.png')
    g.write_json(g.ASSETS / 'models/item/reed_rattle.json', {'parent': 'minecraft:item/handheld', 'textures': {'layer0': 'wildercord:item/reed_rattle'}})
    g.write_json(g.ASSETS / 'items/reed_rattle.json', {'model': {'type': 'minecraft:model', 'model': 'wildercord:item/reed_rattle'}})
    g.write_json(g.DATA / 'recipe/reed_rattle.json', {'type': 'minecraft:crafting_shaped', 'category': 'equipment',
        'pattern': [' FC', 'SBC', ' B '], 'key': {'F': 'wildercord:moonreed_floss', 'C': 'minecraft:clay_ball',
        'S': 'minecraft:string', 'B': 'minecraft:bamboo'}, 'result': {'id': 'wildercord:reed_rattle'}})
    g.unlock_advancement('wildercord:reed_rattle', 'wildercord:moonreed_floss')
