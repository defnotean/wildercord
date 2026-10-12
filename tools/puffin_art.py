"""Stonecrest puffin: its plumage (StonecrestPuffinModel's 64x64 box-UV layout), spawn egg, loot, spawn ground and field-guide text."""
from bobcat_art import egg_icon, fill
from item_art import hexc, mix, ramp
from monster_art import faces, feathers, fur
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.stonecrest_puffin': 'Stonecrest Puffin',
 'item.wildercord.stonecrest_puffin_spawn_egg': 'Stonecrest Puffin Spawn Egg',
 'guide.wildercord.stonecrest_puffin.hint': 'On the stony shores, something small and solemn with a bill far too big for it is fishing.',
 'guide.wildercord.stonecrest_puffin': 'A stout little seabird of the stony shores and beaches, black above and white below, with a '
                                       'white face and a great deep bill banded to an orange-red tip. It waddles about the rocks with '
                                       'its wings held out for balance, preens, gives its wings a quick shake, and stands bill to bill '
                                       'with its mate, the two of them rattling their bills together. It whirrs out over the water in '
                                       'short hops and comes down slow; afloat it bobs on the swell and dives, flying down under the '
                                       'water with its wings, and two times in three it comes up with a row of little silver fish '
                                       'crosswise in its bill. Those go to a puffling if there is one about, and otherwise it eats them '
                                       'itself. At night it sleeps with its head tucked back. Raw cod or salmon tempts it and breeds it; '
                                       'feed one and it trusts you a while, waddling along after you, and the next fish it catches it '
                                       'brings back and drops at your feet.',
}

BLACK = ramp("#0E0E12", "#18181E", "#22222A", "#2E2E38", "#3C3C48")
WHITE = ramp("#B8B8B4", "#CECECA", "#E0E0DC", "#EEEEEA", "#FAFAF6")
GREY = ramp("#7A7C80", "#8E9094", "#A2A4A8", "#B4B6BA", "#C6C8CC")
BILL_BASE = hexc("#5A6878")
BILL_LINE = hexc("#F0C83A")
BILL_RED = ramp("#A8321E", "#C44428", "#DC5A30", "#EA7038", "#F28A48")
FOOT = ramp("#B8481E", "#D45A24", "#E8702C", "#F28838", "#F8A050")
SILVER = ramp("#5C6C78", "#7C8C98", "#9CACB4", "#BCC8CE", "#DCE4E8")
EYE, RING = hexc("#0A0A0C"), hexc("#D8402A")

BODY = box(0, 0, 5, 6, 6)
HEAD = box(24, 0, 4, 4, 4)
BEAK = box(42, 0, 2, 3, 2)
TIP = box(42, 6, 2, 2, 1)
WING = box(0, 14, 1, 5, 4)
TAIL = box(12, 14, 3, 1, 2)
LEG = box(24, 10, 1, 2, 1)
FOOT_BOX = box(30, 10, 2, 1, 3)
FISH = box(0, 24, 6, 1, 1)
FISH2 = box(0, 27, 5, 1, 1)


def skin():
    cv = Sheet(64, 64)
    # The body: black over the back and top, white down the front and beneath.
    for name, area in faces(BODY):
        tones = WHITE if name in ("front", "bottom") else BLACK
        feathers(cv, area, tones, 10 + len(name), name, base=2, row=3) if tones is BLACK else fur(cv, area, tones, 10 + len(name), name, base=2)
    # The sides: black above, the white of the breast curving up toward the front.
    for name in ("right", "left"):
        x0, y0, w, h = BODY[name]
        for x in range(w):
            z = (w - 1 - x) if name == "right" else x  # 0 at the front
            white_from = 2 + z // 2 + (1 if noise(x0 + x, y0, 5) > 0.7 else 0)
            for y in range(white_from, h):
                cv.put(x0 + x, y0 + y, WHITE[2 if y > white_from else 1])
    # A black collar across the top of the breast.
    x0, y0, w, h = BODY["front"]
    for x in range(w):
        cv.put(x0 + x, y0, BLACK[2])
    # The head: black crown, nape and collar, a white face disc, the eye ringed red.
    for name, area in faces(HEAD):
        fur(cv, area, BLACK, 20 + len(name), name, base=2)
    x0, y0, w, h = HEAD["front"]
    for y in range(1, h - 1):
        for x in range(w):
            cv.put(x0 + x, y0 + y, WHITE[3 if y == 1 else 2])
    for name in ("right", "left"):
        x0, y0, w, h = HEAD[name]
        for x in range(w):
            z = (w - 1 - x) if name == "right" else x
            for y in range(1, h - 1 if z < 3 else 1):
                cv.put(x0 + x, y0 + y, mix(WHITE[2], GREY[2], 0.2 if y == 2 else 0))
        # The eye, ringed red, toward the front, with the dark line running back from it.
        ex = x0 + (w - 2 if name == "right" else 1)
        cv.put(ex, y0 + 1, EYE)
        cv.put(ex + (1 if name == "right" else -1), y0 + 1, RING)
        cv.put(x0 + (w - 3 if name == "right" else 2), y0 + 1, GREY[1])
    # The bill: blue-grey at the base, a yellow line, then orange-red banded to the tip.
    for name, area in faces(BEAK):
        x0, y0, w, h = area
        for y in range(h):
            for x in range(w):
                if name in ("right", "left"):
                    z = (w - 1 - x) if name == "right" else x  # 0 at the tip end
                    c = BILL_RED[2 + (y == 0)] if z == 0 else BILL_BASE
                    if z == 1 and y == 0:
                        c = BILL_LINE
                elif name == "back":
                    c = BILL_LINE
                else:
                    c = BILL_RED[2] if name == "front" else mix(BILL_BASE, BILL_RED[2], 0.5)
                cv.put(x0 + x, y0 + y, c)
    # Grooves across the red, the way a puffin's bill is banded.
    for name in ("right", "left"):
        x0, y0, w, h = BEAK[name]
        z0 = x0 + (w - 1 if name == "right" else 0)
        cv.put(z0, y0 + 1, BILL_RED[1])
    for name, area in faces(TIP):
        x0, y0, w, h = area
        for y in range(h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, BILL_RED[3 if y == 0 else 2] if (x + y) % 3 else BILL_RED[1])
    # Wings: black, feathered in rows, a touch lighter at the tips.
    for name, area in faces(WING):
        feathers(cv, area, BLACK, 40 + len(name), name, base=2, row=2)
    # Tail: black.
    for name, area in faces(TAIL):
        feathers(cv, area, BLACK, 50 + len(name), name, base=1, row=2)
    # Legs and broad webbed feet: bright orange.
    for name, area in faces(LEG):
        fill(cv, area, FOOT[2 if name != "back" else 1])
    for name, area in faces(FOOT_BOX):
        x0, y0, w, h = area
        for y in range(h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, FOOT[3 if name == "top" and (x + y) % 2 else 2])
    # The fish: little silver sand eels, darker along the back, an eye at one end.
    for part, seed in ((FISH, 60), (FISH2, 64)):
        for name, area in faces(part):
            x0, y0, w, h = area
            for y in range(h):
                for x in range(w):
                    tone = 1 if name == "top" else 3 if name == "bottom" else 2 + (1 if noise(x0 + x, y0 + y, seed) > 0.7 else 0)
                    cv.put(x0 + x, y0 + y, SILVER[tone])
        x0, y0, w, h = part["front"]
        cv.put(x0, y0, EYE)
        x0, y0, w, h = part["back"]
        cv.put(x0 + w - 1, y0, EYE)
    return cv.image()


def write(g):
    g.save(skin(), g.ASSETS / 'textures/entity/wildlife/stonecrest_puffin.png')
    name = 'stonecrest_puffin_spawn_egg'
    g.save(egg_icon(("#18181E", "#2E2E38", "#EEEEEA"), "#E8702C", name="stonecrest_puffin"), g.ASSETS / f'textures/item/{name}.png')
    g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
    g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
    g.write_json(g.DATA / 'loot_table/entities/stonecrest_puffin.json', {'type': 'minecraft:entity', 'pools': [], 'random_sequence': 'wildercord:entities/stonecrest_puffin'})
    g.write_json(g.DATA / 'tags/block/spawns_on/stonecrest_puffin.json', {'replace': False, 'values': [
        '#minecraft:animals_spawnable_on', 'minecraft:stone', 'minecraft:gravel', 'minecraft:sand', 'minecraft:cobblestone',
        'minecraft:andesite', 'minecraft:granite', 'minecraft:diorite', 'minecraft:snow_block', 'minecraft:grass_block']})
