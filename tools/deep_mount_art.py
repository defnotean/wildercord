"""0.13 water and burrowing mounts: the Reefback Turtle's and Delver Mole's skins (ReefbackTurtleModel's and DelverMoleModel's
128x64 box-UV layouts, bare and saddled), spawn eggs, loot, spawn ground and field-guide text."""
from item_art import Canvas, hexc, mix, ramp
from monster_art import EGG, chitin, faces, fill, fur, mottle, shade, smooth
from world_art import Sheet, box, noise

LANG = {
    'entity.wildercord.reefback_turtle': 'Reefback Turtle',
    'item.wildercord.reefback_turtle_spawn_egg': 'Reefback Turtle Spawn Egg',
    'guide.wildercord.reefback_turtle.hint': 'Look for a great sea turtle with coral on its shell on beaches and mangrove shores.',
    'guide.wildercord.reefback_turtle': 'A sea turtle big enough to carry a rider, its shell crusted with coral. Ashore it is slow, but in water it swims the way its rider looks, diving or climbing, never throws them off under water and keeps them breathing. Climb on it until it stops throwing you, as you would a horse, then saddle it to ride. Kelp and seagrass heal it and calm a wild one; two tamed reefbacks fed golden apples or golden carrots raise a hatchling.',
    'entity.wildercord.delver_mole': 'Delver Mole',
    'item.wildercord.delver_mole_spawn_egg': 'Delver Mole Spawn Egg',
    'guide.wildercord.delver_mole.hint': 'Look for a huge velvet-black mole with a pink star of a nose on plains, meadows and in forests.',
    'guide.wildercord.delver_mole': 'A mole the size of a pony, with spade claws and a twitching pink star for a nose. Tame it as you would a horse and saddle it, and it digs you a tunnel as it goes through anything a shovel takes that is no harder than grass: dirt, sand, gravel, clay, snow. Ride straight to tunnel level; look down to dig down and up to dig up. It drops what it digs, and never digs where you could not dig yourself. Carrots, potatoes and beetroot heal it and calm a wild one; golden food readies two tame ones to raise a pup.',
}

LEATHER = [hexc(c) for c in ("#3A2010", "#5A321A", "#7A4626", "#955A32")]
GOLD = hexc("#E0B040")
BEAD = hexc("#0A0A0E")
GLINT = hexc("#FFFFFF")

# ============================================================== the reefback (ReefbackTurtleModel, 128x64)

SHELL = ramp("#16362E", "#1F4A3E", "#2A5E4E", "#3A7462", "#4E8C78", "#6AA690")
TURTLE_SKIN = ramp("#2C4A3A", "#3A5E48", "#4A7458", "#5E8A6A", "#7AA284")
SPOTS = ramp("#7A8A5A", "#94A270", "#B0BC8A", "#C8D2A4", "#DCE4BC")
BELLY = ramp("#8A7A48", "#A8965A", "#C4B070", "#DCC88A", "#ECDCA6")
CORAL = ramp("#8A2A3A", "#C04A4E", "#E8705E", "#F89A7A", "#FFC4A4")
CORAL_TIPS = ramp("#C86A3A", "#E8904A", "#F8B05A", "#FFD07A", "#FFE8A8")
BEAK = hexc("#3A3428")

T_SHELL = box(0, 0, 16, 6, 20)
T_RIDGE = box(0, 26, 10, 2, 14)
T_BELLY = box(0, 42, 14, 2, 18)
T_HEAD = box(72, 0, 6, 5, 7)
T_BEAK = box(98, 0, 4, 2, 2)
T_NECK = box(72, 12, 4, 4, 5)
T_FRONT = box(72, 22, 12, 1, 5)
T_BACK = box(72, 30, 7, 1, 5)
T_TAIL = box(110, 0, 2, 2, 4)
T_CORAL = box(96, 30, 3, 3, 3)

FACE_LIGHT = {"top": 1, "bottom": -2, "back": -1, "left": -1}


def scutes(cv, area, tones, seed, face, base=2):
    """A turtle's shell: staggered plates each lighter at the heart, split by dark seams, with barnacle flecks."""
    x0, y0, w, h = area
    for y in range(h):
        for x in range(w):
            px, py = x0 + x, y0 + y
            cx = (x + (y // 5 % 2) * 3) % 6
            cy = y % 5
            t = base + FACE_LIGHT.get(face, 0)
            if cx == 0 or cy == 0:
                t -= 2
            elif cx in (2, 3) and cy in (2, 3):
                t += 1
            if smooth(px, py, seed, 2.5) > 0.78:
                t += 1
            c = shade(tones, t)
            if noise(px, py, seed + 3) > 0.985:
                c = hexc("#C8C4B0")
            cv.put(px, py, c)


def turtle_skin(saddled=False):
    """A teal shell of plated scutes with coral knobs, sea-green skin with pale spots, a cream belly plate and beady eyes."""
    cv = Sheet(128, 64)
    for name, area in faces(T_SHELL):
        scutes(cv, area, SHELL, 3 + len(name), name)
    for name, area in faces(T_RIDGE):
        scutes(cv, area, SHELL, 9 + len(name), name, base=3)
    for name, area in faces(T_BELLY):
        chitin(cv, area, BELLY, 13 + len(name), name, base=2, seam=4, vertical=name in ("top", "bottom"))
    for part, seed in ((T_HEAD, 21), (T_NECK, 22), (T_FRONT, 23), (T_BACK, 24), (T_TAIL, 25)):
        for name, area in faces(part):
            mottle(cv, area, TURTLE_SKIN, SPOTS, seed * 3 + len(name), name, base=2, blotchiness=0.2)
    for name, area in faces(T_CORAL):
        mottle(cv, area, CORAL, CORAL_TIPS, 31 + len(name), name, base=2, blotchiness=0.3)
    for name, area in faces(T_BEAK):
        fill(cv, area, BEAK)
    # Beady eyes on each side of the head, each with a glint, and a darker line for the mouth.
    for name in ("right", "left"):
        x0, y0, w, h = T_HEAD[name]
        ex = x0 + (1 if name == "right" else w - 2)
        cv.put(ex, y0 + 1, BEAD)
        cv.put(ex, y0 + 2, BEAD)
        cv.put(ex + (1 if name == "right" else -1), y0 + 1, GLINT)
    x0, y0, w, h = T_HEAD["front"]
    for x in range(w):
        cv.put(x0 + x, y0 + h - 2, TURTLE_SKIN[0])
    # Pale leading edges on the flippers.
    for part in (T_FRONT, T_BACK):
        x0, y0, w, h = part["top"]
        for x in range(w):
            cv.put(x0 + x, y0, SPOTS[3])
    if saddled:
        saddle(cv, T_RIDGE, T_SHELL)
    return cv.image()


# ============================================================== the delver (DelverMoleModel, 128x64)

MOLE = ramp("#0E0A10", "#17121A", "#211A25", "#2D2432", "#3B3042", "#4C4054")
PINK = ramp("#A04A62", "#C86A82", "#E8909E", "#F8B6C0", "#FFD4DA")
CLAW = ramp("#9A8E78", "#C4B89C", "#E6DCC2", "#F4EEDC")

M_BODY = box(0, 0, 14, 10, 18)
M_HEAD = box(64, 0, 8, 6, 7)
M_SNOUT = box(94, 0, 4, 3, 4)
M_STAR = box(110, 0, 6, 5, 1)
M_ARM = box(0, 28, 4, 6, 4)
M_CLAW = box(16, 28, 6, 2, 6)
M_LEG = box(40, 28, 3, 4, 4)
M_FOOT = box(54, 28, 4, 1, 5)
M_TAIL = box(72, 28, 2, 2, 6)


def mole_skin(saddled=False):
    """Velvet black fur with a soft sheen down the back, a pink snout and star of a nose, ivory spade claws, pink feet and
    tail, and tiny glinting eyes."""
    cv = Sheet(128, 64)
    for part, seed in ((M_BODY, 41), (M_HEAD, 42), (M_ARM, 43), (M_LEG, 44)):
        for name, area in faces(part):
            fur(cv, area, MOLE, seed * 5 + len(name), name, base=2, along_y=part is M_BODY and name in ("top", "bottom"))
    for part, seed in ((M_SNOUT, 51), (M_STAR, 52), (M_TAIL, 53), (M_FOOT, 54)):
        for name, area in faces(part):
            mottle(cv, area, PINK, PINK, seed * 3 + len(name), name, base=2, blotchiness=0.15)
    for name, area in faces(M_CLAW):
        chitin(cv, area, CLAW, 61 + len(name), name, base=1, seam=2, vertical=True)
    # The star: brighter pink rays round two dark nostrils.
    x0, y0, w, h = M_STAR["front"]
    for x, y in ((0, 0), (5, 0), (0, 4), (5, 4), (2, 0), (3, 4), (0, 2), (5, 2)):
        cv.put(x0 + x, y0 + y, PINK[4])
    cv.put(x0 + 2, y0 + 2, PINK[0])
    cv.put(x0 + 3, y0 + 2, PINK[0])
    # Tiny eyes, nearly lost in the fur, each with a glint.
    x0, y0, w, h = M_HEAD["front"]
    for ex in (1, w - 2):
        cv.put(x0 + ex, y0 + 2, BEAD)
        cv.put(x0 + ex, y0 + 1, MOLE[5])
    cv.put(x0 + 1, y0 + 2, mix(BEAD, GLINT, 0.6))
    cv.put(x0 + w - 2, y0 + 2, mix(BEAD, GLINT, 0.6))
    # A silvery sheen along the back.
    x0, y0, w, h = M_BODY["top"]
    for y in range(h):
        for x in range(w // 2 - 2, w // 2 + 2):
            if smooth(x0 + x, y0 + y, 77, 2) > 0.5:
                cv.put(x0 + x, y0 + y, MOLE[5])
    if saddled:
        saddle(cv, M_BODY, M_BODY)
    return cv.image()


# ============================================================== shared

def saddle(cv, seat, sides):
    """A leather saddle on the seat's top face, with flaps and a girth down the sides' faces and under the belly."""
    x0, y0, w, h = seat["top"]
    sx, sy = x0 + w // 2 - 3, y0 + h // 2 - 4
    for y in range(8):
        for x in range(6):
            c = LEATHER[0] if y in (0, 7) or x in (0, 5) else LEATHER[2] if 2 <= y <= 5 else LEATHER[1]
            if noise(sx + x, sy + y, 41) > 0.88:
                c = LEATHER[3]
            cv.put(sx + x, sy + y, c)
    cv.put(sx + 1, sy, GOLD)
    cv.put(sx + 4, sy, GOLD)
    for name in ("right", "left"):
        x0, y0, w, h = sides[name]
        mx = x0 + w // 2
        flap = min(4, h)
        for y in range(flap):
            for x in range(mx - 2, mx + 2):
                cv.put(x, y0 + y, LEATHER[1] if y == flap - 1 else LEATHER[2])
        for y in range(flap, h):
            cv.put(mx - 1, y0 + y, LEATHER[0])
            cv.put(mx, y0 + y, LEATHER[0])
        cv.put(mx - 1, y0 + flap - 1, GOLD)
    x0, y0, w, h = sides["bottom"]
    for x in range(w):
        cv.put(x0 + x, y0 + h // 2, LEATHER[0])


def egg_icon(name, dark, mid, light, spot):
    dark, mid, light, spot = hexc(dark), hexc(mid), hexc(light), hexc(spot)
    outline = mix(dark, (0, 0, 0), 0.6)
    cv = Canvas()
    for y, row in enumerate(r.strip() for r in EGG.strip("\n").splitlines()):
        for x, ch in enumerate(row):
            if ch == "o":
                cv.put(x, y, outline)
            elif ch == "1":
                t = (x + y) / 26
                c = light if t < 0.42 else mid if t < 0.72 else dark
                if noise(x, y, len(name)) > 0.82:
                    c = spot
                cv.put(x, y, c)
    return cv.image()


CREATURES = (
    ('reefback_turtle', ("#1F4A3E", "#3A7462", "#6AA690", "#E8705E"), 'minecraft:turtle_scute',
     ['minecraft:sand', 'minecraft:mud', 'minecraft:gravel']),
    ('delver_mole', ("#17121A", "#2D2432", "#4C4054", "#E8909E"), 'minecraft:leather',
     ['minecraft:grass_block', 'minecraft:dirt', 'minecraft:coarse_dirt', 'minecraft:podzol']),
)


def write(g):
    g.save(turtle_skin(), g.ASSETS / 'textures/entity/wildlife/reefback_turtle.png')
    g.save(turtle_skin(True), g.ASSETS / 'textures/entity/wildlife/reefback_turtle_saddled.png')
    g.save(mole_skin(), g.ASSETS / 'textures/entity/wildlife/delver_mole.png')
    g.save(mole_skin(True), g.ASSETS / 'textures/entity/wildlife/delver_mole_saddled.png')
    for creature, colours, drop, ground in CREATURES:
        name = f'{creature}_spawn_egg'
        g.save(egg_icon(creature, *colours), g.ASSETS / f'textures/item/{name}.png')
        g.write_json(g.ASSETS / f'models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{name}'}})
        g.write_json(g.ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{name}'}})
        g.write_json(g.DATA / f'loot_table/entities/{creature}.json', {'type': 'minecraft:entity', 'pools': [{
            'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': drop, 'functions': [
                {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 0, 'max': 1}},
                {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting', 'count': {'type': 'minecraft:uniform', 'min': 0, 'max': 1}}]}]}],
            'random_sequence': f'wildercord:entities/{creature}'})
        g.write_json(g.DATA / f'tags/block/spawns_on/{creature}.json', {'replace': False, 'values': ground})
    # A saddle goes only on what vanilla's tag names, so every Wildercord mount joins it; the reefback breathes water.
    tags = g.DATA.parent / 'minecraft/tags/entity_type'
    mounts = ['wildercord:ridgeback_stag', 'wildercord:reefback_turtle', 'wildercord:delver_mole']
    g.write_json(tags / 'can_equip_saddle.json', {'replace': False, 'values': mounts})
    g.write_json(tags / 'can_wear_horse_armor.json', {'replace': False, 'values': mounts})
    g.write_json(tags / 'can_breathe_under_water.json', {'replace': False, 'values': ['wildercord:reefback_turtle']})
