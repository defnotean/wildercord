"""0.12 "Tempering" mounts: the Ridgeback Stag's skins (LumenStagModel's 128x64 layout, bare and saddled), spawn egg, loot,
spawn ground and field-guide text."""
from item_art import Canvas, hexc, mix
from monster_art import EGG
from world_art import noise
import wildlife_art as wa

LANG = {
    'entity.wildercord.ridgeback_stag': 'Ridgeback Stag',
    'item.wildercord.ridgeback_stag_spawn_egg': 'Ridgeback Stag Spawn Egg',
    'entity.wildercord.bonded_skyray': 'Bonded Skyray',
    'item.wildercord.skyray_bridle': 'Skyray Bridle',
    'tooltip.wildercord.skyray_bridle': 'Calls your skyray down from the open sky.',
    'tooltip.wildercord.skyray_bridle.ride': 'Use to ride. Fly where you look; sneak to step off.',
    'message.wildercord.tribulation.bridle': 'A skyray circles overhead, waiting for you. A Skyray Bridle falls from the storm.',
    'guide.wildercord.ridgeback_stag.hint': 'Look for a chestnut stag with a dark ridge down its back on plains, meadows and savannas.',
    'guide.wildercord.ridgeback_stag': 'A broad chestnut stag bred for the road: the Lumen Stag\N{RIGHT SINGLE QUOTATION MARK}s plainer cousin, with horn-coloured antlers and a dark ridge of mane down its spine. Climb on it until it stops throwing you, as you would a horse, then saddle it to ride. It eats wheat, apples, carrots and hay; two tamed stags fed golden apples or golden carrots raise a calf. The stablemaster at a Wayfarer Inn sells deeds to tame ones.',
}

COAT = wa.H("#4A2E1A", "#6A4426", "#8A5A34", "#A87A50", "#D2B48C")
STRIPE = hexc("#2E1C10")
SPOT = hexc("#C8A27A")
HORN = wa.H("#6E5A40", "#94805E", "#BCA882", "#E2D6B8")

LEATHER = [hexc(c) for c in ("#3A2010", "#5A321A", "#7A4626", "#955A32")]
GOLD = hexc("#E0B040")


def skin(saddled=False):
    """The stag skin with the ridgeback's colours swapped in; saddled, a leather saddle over the back with flaps down each side."""
    saved = {k: getattr(wa, k) for k in ('STAG_COAT', 'STAG_STRIPE', 'STAG_SPOT', 'STAG_CRYSTAL', 'STAG_BONE')}
    wa.STAG_COAT, wa.STAG_STRIPE, wa.STAG_SPOT, wa.STAG_CRYSTAL, wa.STAG_BONE = COAT, STRIPE, SPOT, HORN, HORN[0]
    try:
        img = wa.stag_skin()
    finally:
        for k, v in saved.items():
            setattr(wa, k, v)
    if not saddled:
        return img

    def put(x, y, c):
        img.putpixel((x, y), (*c[:3], 255))

    # The seat, on the body's top face (17..24 across, 0..16 along).
    for y in range(5, 12):
        for x in range(17, 25):
            c = LEATHER[2] if 7 <= y <= 9 else LEATHER[1]
            if y in (5, 11):
                c = LEATHER[0]
            if noise(x, y, 41) > 0.85:
                c = LEATHER[3]
            put(x, y, c)
    put(18, 5, GOLD)
    put(23, 5, GOLD)
    # The flaps and a girth down each side (right face 0..16, left 25..41; 17..24 down).
    for x0 in (0, 25):
        for y in range(17, 22):
            for x in range(x0 + 6, x0 + 12):
                put(x, y, LEATHER[1] if x in (x0 + 6, x0 + 11) or y == 21 else LEATHER[2])
        for y in range(22, 25):
            put(x0 + 8, y, LEATHER[0])
            put(x0 + 9, y, LEATHER[0])
        put(x0 + 8, 21, GOLD)
    # The girth under the belly (bottom face 25..32, 0..16).
    for x in range(25, 33):
        put(x, 8, LEATHER[0])
        put(x, 9, LEATHER[0])
    return img


def egg_icon():
    dark, mid, light = hexc("#4A2E1A"), hexc("#8A5A34"), hexc("#B8885A")
    outline = mix(dark, (0, 0, 0), 0.6)
    cv = Canvas()
    for y, row in enumerate(r.strip() for r in EGG.strip("\n").splitlines()):
        for x, ch in enumerate(row):
            if ch == "o":
                cv.put(x, y, outline)
            elif ch == "1":
                t = (x + y) / 26
                c = light if t < 0.42 else mid if t < 0.72 else dark
                if abs(x - 7.5) < 1 and y < 12:
                    c = STRIPE
                elif noise(x, y, 77) > 0.84:
                    c = hexc("#E2D6B8")
                cv.put(x, y, c)
    return cv.image()


SKY = [hexc(c) for c in ("#3A5A9A", "#6A9AE0", "#A8D0FF", "#E8F4FF")]


def bridle_icon():
    """A loop of dark leather reins round a glowing sky-blue crystal brow-piece, with a silver ring at each side."""
    cv = Canvas()
    # The reins: an oval loop of leather.
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) / 6.2) ** 2 + ((y - 8.5) / 5.6) ** 2
            if 0.62 <= d <= 1.0:
                t = 3 if y < 8 and x < 8 else 2 if y < 9 else 1
                if d > 0.92:
                    t = 0
                cv.put(x, y, LEATHER[t])
    # The silver rings where the reins meet the brow-piece.
    for x, y in ((2, 6), (13, 6), (2, 7), (13, 7)):
        cv.put(x, y, hexc("#C8CCD8") if y == 6 else hexc("#8A8E9C"))
    # The brow-piece: a skyray-shaped crystal across the top.
    shape = ((7, 2), (8, 2), (6, 3), (7, 3), (8, 3), (9, 3), (4, 4), (5, 4), (6, 4), (7, 4), (8, 4), (9, 4), (10, 4), (11, 4),
             (6, 5), (7, 5), (8, 5), (9, 5), (7, 6), (8, 6), (8, 7))
    for x, y in shape:
        edge = (x, y) in ((4, 4), (11, 4), (8, 7), (7, 6))
        cv.put(x, y, SKY[0] if edge else SKY[1] if y >= 5 else SKY[2])
    for x, y in ((7, 3), (8, 4)):
        cv.put(x, y, SKY[3])
    return cv.image()


def write(g):
    g.save(bridle_icon(), g.ASSETS / 'textures/item/skyray_bridle.png')
    g.write_json(g.ASSETS / 'models/item/skyray_bridle.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'wildercord:item/skyray_bridle'}})
    g.write_json(g.ASSETS / 'items/skyray_bridle.json', {'model': {'type': 'minecraft:model', 'model': 'wildercord:item/skyray_bridle'}})
    g.write_json(g.DATA / 'loot_table/entities/bonded_skyray.json', {'type': 'minecraft:entity', 'pools': [],
        'random_sequence': 'wildercord:entities/bonded_skyray'})
    g.save(skin(), g.ASSETS / 'textures/entity/wildlife/ridgeback_stag.png')
    g.save(skin(True), g.ASSETS / 'textures/entity/wildlife/ridgeback_stag_saddled.png')
    name = 'ridgeback_stag_spawn_egg'
    g.save(egg_icon(), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/ridgeback_stag.json', {'type': 'minecraft:entity', 'pools': [{
        'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:leather', 'functions': [
            {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 0, 'max': 2}},
            {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting', 'count': {'type': 'minecraft:uniform', 'min': 0, 'max': 1}}]}]}],
        'random_sequence': 'wildercord:entities/ridgeback_stag'})
