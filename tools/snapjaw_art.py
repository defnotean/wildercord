"""Snapjaw Turtle: its shell and hide (SnapjawTurtleModel's 128x64 box-UV layout), spawn egg, loot and field-guide text."""
from bobcat_art import egg_icon
from item_art import hexc, ramp
from monster_art import faces, smooth
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.snapjaw_turtle': 'Snapjaw Turtle',
 'item.wildercord.snapjaw_turtle_spawn_egg': 'Snapjaw Turtle Spawn Egg',
 'guide.wildercord.snapjaw_turtle.hint': 'On the riverbed, something like a weedy rock with its mouth open, and a little pink worm wriggling inside.',
 'guide.wildercord.snapjaw_turtle': 'A great old snapping turtle of the rivers, broad as a doorway, its shell ridged in three rows '
                                    'of spikes and furred with green weed, with a great hooked beak and, on its tongue, a little '
                                    'pink worm of flesh that wriggles. It lies still on the riverbed with its jaws open and waits. '
                                    'Whatever comes close enough it snaps at, quicker than you would think, and if its jaws close '
                                    'they hold on: you are held fast and worried every second or so, until it lets go, you hurt it '
                                    'enough that it does, or you are pulled free. A snap that misses leaves it a moment with its '
                                    'jaws shut on water. Its shell turns most of a blow from above or behind, so strike it face on, '
                                    'at its jaws. Once roused it comes after you, slowly, and badly hurt it pulls into its shell a '
                                    'while, where little can touch it.',
}

SHELL = ramp("#1C1E12", "#2A2C1A", "#3A3A22", "#4C4A2C", "#605C38")
SEAM = hexc("#121208")
WEED = ramp("#1E3A14", "#2C5018", "#3E6820", "#56822C", "#70A03A")
HIDE = ramp("#2A2622", "#3A3530", "#4C4640", "#5E5850", "#726A60")
BELLY = ramp("#5A4E30", "#76683E", "#8E7E4C", "#A6945C", "#BCA86E")
HORN = ramp("#141210", "#22201C", "#32302A", "#46423A", "#5A564C")
MOUTH = ramp("#5A3A3A", "#704848", "#865A58", "#9C6C68", "#B08078")
LURE = ramp("#A83050", "#C8406A", "#E05A84", "#F07CA0", "#FCA4BE")
EYE = hexc("#C8A020")
PUPIL = hexc("#0A0806")

SHELL_BOX = box(0, 0, 18, 6, 20)
HEAD = box(76, 0, 7, 6, 7)
BEAK = box(104, 0, 5, 2, 4)
JAW = box(76, 13, 5, 2, 5)
LURE_BOX = box(96, 13, 1, 1, 3)
PLASTRON = box(0, 26, 14, 2, 16)
LEG = box(60, 26, 5, 4, 5)
TAIL = box(80, 26, 3, 3, 8)
RIDGE = box(0, 44, 2, 2, 18)

LIGHT = {"top": 1, "bottom": -1, "front": 0, "back": -1, "right": 0, "left": -1}


def hide(cv, part, seed, tones=HIDE, base=2):
    """Rough pebbled skin, lit from above."""
    for name, (x0, y0, w, h) in faces(part):
        for y in range(h):
            for x in range(w):
                t = base + LIGHT.get(name, 0) + (1 if smooth(x0 + x, y0 + y, seed, 1.5) > 0.7 else 0)
                if noise(x0 + x, y0 + y, seed) > 0.8:
                    t -= 1
                cv.put(x0 + x, y0 + y, tones[max(0, min(4, t))])


def plates(cv, part, seed, weed=0.0):
    """The shell: scutes with dark seams between, each a little raised in the middle, weed grown over the top."""
    for name, (x0, y0, w, h) in faces(part):
        cell = 5 if name in ("top", "bottom") else 4
        for y in range(h):
            for x in range(w):
                cx, cy = x % cell, y % cell
                edge = min(cx, cell - 1 - cx, cy, cell - 1 - cy)
                t = 1 + LIGHT.get(name, 0) + min(edge, 2) // 1
                if noise(x0 + x, y0 + y, seed) > 0.85:
                    t -= 1
                c = SHELL[max(0, min(4, t))]
                if cx == 0 or cy == 0:
                    c = SEAM
                if weed and name in ("top", "front", "back", "right", "left"):
                    lush = smooth(x0 + x, y0 + y, seed + 3, 3.0)
                    if lush > 1 - weed * (1.0 if name == "top" else 0.5):
                        c = WEED[max(0, min(4, 1 + int((lush - 0.5) * 6) + (1 if noise(x0 + x, y0 + y, seed + 4) > 0.6 else 0)))]
                cv.put(x0 + x, y0 + y, c)


def skin():
    cv = Sheet(128, 64)
    plates(cv, SHELL_BOX, 10, weed=0.45)
    # The shell's rim: a lighter, sawtoothed edge round its sides.
    for name in ("front", "back", "right", "left"):
        x0, y0, w, h = SHELL_BOX[name]
        for x in range(w):
            cv.put(x0 + x, y0 + h - 1, SHELL[3] if x % 3 else SEAM)
    # The ridges: knobbed spikes, weed in the hollows.
    for name, (x0, y0, w, h) in faces(RIDGE):
        for y in range(h):
            for x in range(w):
                along = (x if name in ("right", "left") else y)
                knob = along % 3 == 1
                t = (3 if knob else 1) + LIGHT.get(name, 0)
                c = SHELL[max(0, min(4, t))]
                if not knob and noise(x0 + x, y0 + y, 31) > 0.5:
                    c = WEED[1 + (1 if noise(x0 + x, y0 + y, 32) > 0.5 else 0)]
                cv.put(x0 + x, y0 + y, c)
    # The plastron: dull yellow plates.
    for name, (x0, y0, w, h) in faces(PLASTRON):
        for y in range(h):
            for x in range(w):
                t = 2 + (1 if name == "bottom" else 0) + (1 if smooth(x0 + x, y0 + y, 40, 2) > 0.7 else 0)
                c = BELLY[max(0, min(4, t))]
                if name == "bottom" and (x % 7 == 0 or y % 5 == 0):
                    c = BELLY[0]
                cv.put(x0 + x, y0 + y, c)
    # The head: rough grey hide, pale on the roof of the mouth, eyes set on the sides.
    hide(cv, HEAD, 50)
    x0, y0, w, h = HEAD["bottom"]
    for y in range(h):
        for x in range(w):
            cv.put(x0 + x, y0 + y, MOUTH[2 + (1 if noise(x0 + x, y0 + y, 51) > 0.6 else 0)])
    for name, ex in (("right", 1), ("left", None)):
        x0, y0, w, h = HEAD[name]
        # The front of each side face is at its right edge for 'left' and its left edge for 'right'.
        col = x0 + (w - 2 if name == "right" else 1)
        cv.put(col, y0 + 2, EYE)
        cv.put(col + (1 if name == "right" else -1), y0 + 2, PUPIL)
        cv.put(col, y0 + 1, HIDE[0])
        cv.put(col + (1 if name == "right" else -1), y0 + 1, HIDE[0])
    x0, y0, w, h = HEAD["top"]
    for x in range(w):
        for y in range(h):
            if noise(x0 + x, y0 + y, 52) > 0.75:
                cv.put(x0 + x, y0 + y, HIDE[4])
    # The beak: dark horn, the hooked tip darkest, pale inside.
    hide(cv, BEAK, 60, HORN)
    x0, y0, w, h = BEAK["front"]
    for x in range(w):
        cv.put(x0 + x, y0 + h - 1, HORN[0])
    cv.put(x0 + w // 2, y0 + h - 1, PUPIL)
    x0, y0, w, h = BEAK["bottom"]
    for y in range(h):
        for x in range(w):
            cv.put(x0 + x, y0 + y, MOUTH[1])
    # The lower jaw: horn outside, the floor of its mouth pale pink.
    hide(cv, JAW, 70, HORN)
    x0, y0, w, h = JAW["top"]
    for y in range(h):
        for x in range(w):
            cv.put(x0 + x, y0 + y, MOUTH[3 if 1 <= x < w - 1 and y >= 1 else 2])
    # The lure: a bright pink worm, banded.
    for name, (x0, y0, w, h) in faces(LURE_BOX):
        for y in range(h):
            for x in range(w):
                along = x if name in ("right", "left") else y
                cv.put(x0 + x, y0 + y, LURE[3 if along % 2 == 0 else 2] if name != "bottom" else LURE[1])
    # Legs: scaled hide with pale claws at the toes.
    hide(cv, LEG, 80)
    for name in ("front", "right", "left", "back"):
        x0, y0, w, h = LEG[name]
        for x in range(w):
            cv.put(x0 + x, y0 + h - 1, HORN[0] if x % 2 else BELLY[3])
    # The tail: hide, with a ridge of dark knobs down its top.
    hide(cv, TAIL, 90)
    x0, y0, w, h = TAIL["top"]
    for y in range(h):
        cv.put(x0 + w // 2, y0 + y, SHELL[0] if y % 2 else SHELL[3])
    return cv.image()


def _count(lo, hi):
    return {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}


def _looting(hi):
    return {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting',
            'count': {'type': 'minecraft:uniform', 'min': 0, 'max': hi}}


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/snapjaw_turtle.png')
    name = 'snapjaw_turtle_spawn_egg'
    g.save(egg_icon(("#2A2C1A", "#3A3A22", "#4C4A2C"), "#56822C", name="snapjaw_turtle"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/snapjaw_turtle.json', {'type': 'minecraft:entity', 'pools': [
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:turtle_scute', 'functions': [_count(0, 1), _looting(1)]}]},
        {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:kelp', 'functions': [_count(1, 3)]}]},
        {'rolls': 1, 'conditions': [{'condition': 'minecraft:random_chance_with_enchanted_bonus', 'enchantment': 'minecraft:looting',
                                     'unenchanted_chance': 0.25, 'enchanted_chance': {'type': 'minecraft:linear', 'base': 0.35, 'per_level_above_first': 0.1}}],
         'entries': [{'type': 'minecraft:item', 'name': 'minecraft:cod', 'functions': [_count(1, 2)]}]}],
        'random_sequence': 'wildercord:entities/snapjaw_turtle'})
