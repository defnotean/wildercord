"""The Siltcrest Bittern: its skin (SiltcrestModel, 256x128), spawn egg, loot table and English text.

The skin is painted face by face with monster_art's painters, like the established creatures: a streaked buff and brown
wetland heron with a dark crown and moustache stripe, layered folded wings ending in dark barred flight feathers, a
yellow divided bill and long green-yellow legs. Each box keeps its own island on the sheet (the texOffs in
SiltcrestModel.createLayer), so the layout never changes.
"""
import json

from item_art import Canvas, hexc, mix, ramp
from monster_art import EGG, LIGHT, cells, faces, feathers, fill, long_feathers, shade
from world_art import Sheet, box, noise

# Box layout from SiltcrestModel.createLayer: texOffs (u, v) and box size (w, h, d).
S_BODY = box(0, 0, 8, 7, 10)
S_BREAST = box(40, 0, 6, 6, 2)
S_NECK = box(60, 0, 2, 6, 3)
S_UPPER = box(76, 0, 2, 5, 2)
S_HEAD = box(90, 0, 4, 4, 5)
S_BILL = box(114, 0, 1, 1, 6)
S_LOWER_BILL = box(132, 0, 1, 1, 4)
S_CREST = box(148, 0, 1, 2, 3)
S_EYES = [box(160, 0, 1, 1, 1), box(168, 0, 1, 1, 1)]
S_WINGS = [box(0, 24, 2, 7, 8), box(24, 24, 2, 7, 8)]
S_FEATHERS = [box(48 + i * 42 + k * 14, 24, 1, 5, 4) for i in range(2) for k in range(3)]
S_TAIL = box(138, 24, 4, 1, 5)
S_TIP = box(158, 24, 3, 1, 4)
S_THROAT = box(68, 48, 2, 3, 1)
S_LEGS = [box(0, 48, 1, 4, 1), box(8, 48, 1, 4, 1)]
S_ANKLES = [box(16, 48, 1, 4, 1), box(24, 48, 1, 4, 1)]
S_TOES = [box(32, 48, 3, 1, 4), box(48, 48, 3, 1, 4)]

BUFF = ramp("#5E4020", "#82602F", "#A88044", "#C9A05C", "#E0BC7A", "#F0D49C")
BACK = ramp("#2E1E0E", "#4A3218", "#664622", "#84602E", "#A27A3E", "#BE9654")
STREAK = ramp("#22160A", "#36240F", "#4C3416", "#62451E")
FLIGHT = ramp("#1C130A", "#2C1E10", "#3E2B16", "#54391C", "#6C4C26")
BILL = ramp("#5C4A12", "#8E7420", "#C2A032", "#E2C450", "#F2DC7E")
LEG = ramp("#34421A", "#4E6224", "#6E8430", "#90A63E", "#B2C45A")
EYE = hexc("#F2C838")
PUPIL = hexc("#140C06")

# Which side strip faces outward: wing_0 sits at -x (its "right" strip), wing_1 at +x (its "left" strip).
# On a side strip the front column is the one next to the front face: the last column of "right", the first of "left".


def front_col(name, w):
    return lambda x: (w - 1 - x) if name == "right" else x


def streaks(cv, area, face, every=2, seed=0, start=0, tone=1):
    """Long dark vertical streaks down a pale face: the bittern's camouflage."""
    x0, y0, w, h = area
    for x, y, px, py in cells(area):
        if (x + seed) % every == 0 and y >= start and noise(px, py, seed + 3) > 0.18:
            cv.put(px, py, shade(STREAK, tone + 1 + LIGHT[face]))


def bars(cv, area, face, seed, gap=3, tones=STREAK):
    """Dark centres down the back feathers, set in staggered rows: the bittern's streaked mantle."""
    x0, y0, w, h = area
    for x, y, px, py in cells(area):
        r = y // gap
        k = (x + (r % 2) * 2) % 4
        if k == 2 and y % gap in (0, 1) and noise(px, py, seed) > 0.12:
            cv.put(px, py, shade(tones, 1 + (y % gap) + LIGHT[face]))


def paint_skin():
    cv = Sheet(256, 128)
    # Body: a streaked brown back, buff underparts.
    for name, area in faces(S_BODY):
        if name in ("top", "right", "left", "back"):
            feathers(cv, area, BACK, 11 + len(name), name, base=3)
            bars(cv, area, name, 13 + len(name))
        else:
            feathers(cv, area, BUFF, 17 + len(name), name, base=3, row=2)
            streaks(cv, area, name, every=2 if name == "front" else 3, seed=len(name))
    # Breast: pale buff hung with long brown streaks.
    for name, area in faces(S_BREAST):
        feathers(cv, area, BUFF, 21 + len(name), name, base=4, row=2, flip=True)
        if name in ("front", "right", "left"):
            streaks(cv, area, name, every=2, seed=1, start=1)
    # Neck and upper neck: buff in front with streaks, brown down the nape, a dark moustache stripe on each side.
    for part in (S_NECK, S_UPPER):
        for name, area in faces(part):
            x0, y0, w, h = area
            if name == "back":
                feathers(cv, area, BACK, 31 + len(name), name, base=3, row=2)
            elif name in ("right", "left"):
                fc = front_col(name, w)
                for x, y, px, py in cells(area):
                    k = fc(x)
                    t = 4 + LIGHT[name] - (1 if (y + k) % 3 == 0 else 0)
                    c = shade(BUFF, t) if k < w - 1 else shade(BACK, 3 + LIGHT[name])
                    if k == 1 and w > 2:
                        c = shade(STREAK, 2)
                    cv.put(px, py, c)
            else:
                feathers(cv, area, BUFF, 35 + len(name), name, base=4, row=2, flip=True)
                if name == "front":
                    streaks(cv, area, name, every=2, seed=0)
    for name, area in faces(S_THROAT):
        feathers(cv, area, BUFF, 41, name, base=4, row=2)
        if name == "front":
            x0, y0, w, h = area
            for y in range(h):
                cv.put(x0 + (y % 2), y0 + y, shade(STREAK, 3))
    # Head: buff face, a near-black crown, a dark stripe from the bill base back under the eye.
    for name, area in faces(S_HEAD):
        x0, y0, w, h = area
        if name in ("top", "back"):
            feathers(cv, area, STREAK, 51 + len(name), name, base=2, row=2)
        elif name in ("right", "left"):
            fc = front_col(name, w)
            for x, y, px, py in cells(area):
                k = fc(x)
                c = shade(BUFF, 4 + LIGHT[name] - (1 if noise(px, py, 53) > 0.8 else 0))
                if y == 0:
                    c = shade(STREAK, 2)
                elif y == 1 and k >= 3:
                    c = shade(STREAK, 3)
                elif y == 2 and k == 0:
                    c = shade(STREAK, 1)                  # the line from the gape
                elif y == 3 and k in (1, 2):
                    c = shade(STREAK, 1)                  # the moustache stripe
                cv.put(px, py, c)
        elif name == "front":
            for x, y, px, py in cells(area):
                c = shade(STREAK, 2) if y == 0 else shade(BUFF, 4 if y < 3 else 3)
                cv.put(px, py, c)
        else:
            feathers(cv, area, BUFF, 57, name, base=3, row=2)
    for name, area in faces(S_CREST):
        feathers(cv, area, STREAK, 61 + len(name), name, base=2, row=1)
    # The divided bill: a darker ridge along the upper mandible, both halves dagger-yellow, a dark tip.
    for part, base in ((S_BILL, 3), (S_LOWER_BILL, 4)):
        for name, area in faces(part):
            x0, y0, w, h = area
            for x, y, px, py in cells(area):
                t = base + LIGHT[name]
                if part is S_BILL and name == "top":
                    t = 1
                c = shade(BILL, t)
                if name in ("right", "left") and front_col(name, w)(x) == 0:
                    c = shade(BILL, 1)
                if name == "front":
                    c = shade(BILL, 1)
                cv.put(px, py, c)
    for eye in S_EYES:
        for name, area in faces(eye):
            fill(cv, area, PUPIL if name in ("right", "left") else EYE)
    # Wings: rows of streaked coverts at the shoulder, darker barred flight feathers behind.
    for i, wing in enumerate(S_WINGS):
        for name, area in faces(wing):
            x0, y0, w, h = area
            if name in ("right", "left"):
                fc = front_col(name, w)
                feathers(cv, area, BACK, 71 + i, name if name == "right" else "front", base=3, row=2)
                for x, y, px, py in cells(area):
                    k = fc(x)
                    if k >= 5:                            # the folded primaries
                        t = 3 - (1 if (k + y) % 2 else 0) - (1 if y >= h - 1 else 0)
                        c = shade(FLIGHT, t)
                        if y % 3 == 1:
                            c = shade(BUFF, 2)            # buff bars across the dark feathers
                        cv.put(px, py, c)
                    elif y == h - 1 and k % 2 == 0:
                        cv.put(px, py, shade(BUFF, 3))    # pale feather edges along the fold
                    elif k == 0 and y < 3:
                        cv.put(px, py, shade(BACK, 5))    # the lit shoulder
            elif name == "top":
                feathers(cv, area, BACK, 77 + i, name, base=3, row=2)
            else:
                feathers(cv, area, BACK, 79 + i, name, base=2, row=2)
    for n, part in enumerate(S_FEATHERS):
        for name, area in faces(part):
            if name in ("right", "left"):
                long_feathers(cv, area, FLIGHT, 81 + n, "front", along_x=False, tips=BUFF[3], tip_band=1)
                x0, y0, w, h = area
                for x in range(w):
                    if (x + n) % 2 == 0:
                        cv.put(x0 + x, y0 + 2, shade(BUFF, 2))
            else:
                fill(cv, area, shade(FLIGHT, 2 + LIGHT[name]))
    # Tail: short and brown with dark bars, pale beneath.
    for part in (S_TAIL, S_TIP):
        for name, area in faces(part):
            x0, y0, w, h = area
            if name == "bottom":
                feathers(cv, area, BUFF, 91, "front", base=3, row=2)
            elif name == "top":
                for x, y, px, py in cells(area):
                    c = shade(BACK, 4 - (1 if x % 2 else 0))
                    if y % 2 == 1:
                        c = shade(STREAK, 2)
                    cv.put(px, py, c)
            else:
                fill(cv, area, shade(BACK, 2 + LIGHT[name]))
    # Legs: long, green-yellow and scaled, lighter on the shin's lit side.
    for part in S_LEGS + S_ANKLES + S_TOES:
        for name, area in faces(part):
            for x, y, px, py in cells(area):
                t = 3 + LIGHT[name] - (1 if y % 2 and part in S_ANKLES else 0)
                cv.put(px, py, shade(LEG, t))
    for toes in S_TOES:
        x0, y0, w, h = toes["top"]
        for y in range(h):
            cv.put(x0 + 1, y0 + y, shade(LEG, 4))
        x0, y0, w, h = toes["front"]
        for x in (0, w - 1):
            cv.put(x0 + x, y0, shade(STREAK, 1))          # claws
    return cv.image()


# ============================================================== spawn egg (monster_art.EGG, the established egg look)

EGG_SHELL = ("#B08A58", "#D6BA86", "#F0DCB0")          # pale buff: dark, mid, light
# One motif, the bittern's dark crown cap, and a few short brown streak marks (no speckle noise).
EGG_MOTIF = """
    ................
    ................
    .......kk.......
    ......kkkk......
    ................
    ................
    .........s......
    ....s....s......
    ....s...........
    ...........s....
    .......s...s....
    .......s........
    .....s..........
    ................
    ................
    ................
"""
EGG_PAL = {"k": "#3A2612", "s": "#6E4824"}


def egg_icon():
    dark, mid, light = (hexc(c) for c in EGG_SHELL)
    cv = Canvas()
    outline = mix(dark, (0, 0, 0), 0.55)
    for y, row in enumerate(r.strip() for r in EGG.strip("\n").splitlines()):
        for x, ch in enumerate(row):
            if ch == "o":
                cv.put(x, y, outline)
            elif ch == "1":
                # Lit from the top left, darker to the bottom right, as monster_art.egg_icon shades its shells.
                t = (x + y) / 26
                cv.put(x, y, light if t < 0.42 else mid if t < 0.72 else dark)
    for y, row in enumerate(r.strip() for r in EGG_MOTIF.strip("\n").splitlines()):
        for x, ch in enumerate(row):
            if ch in EGG_PAL and cv.get(x, y) is not None:
                cv.put(x, y, hexc(EGG_PAL[ch]))
    return cv.image()


LANG = {'entity.wildercord.siltcrest_bittern': 'Siltcrest Bittern', 'item.wildercord.siltcrest_bittern_spawn_egg': 'Siltcrest Bittern Spawn Egg', 'guide.wildercord.siltcrest_bittern.hint': 'A quiet reed bird stalks wild cod and salmon at dusk. Crouch nearby; rain sends it toward a covered bank.', 'guide.wildercord.siltcrest_bittern': 'The bittern slowly coils its neck before one short fish strike. It leaves a small pond with at least two eligible fish and rests its appetite after a meal. Bucket fish, named fish and persistent fish are excluded. Offer raw cod or salmon while crouching for close observation; feeding creates no material or breeding reward. An admitted Tidebreath from an actual ally can interrupt a hunt into visible preening; wild birds are not automatically spell allies. Dry cover two blocks above a shallow bank can shelter it during rain or daylight.', 'subtitles.wildercord.kit.bittern.boom': 'Bittern throat booms', 'subtitles.wildercord.kit.bittern.catch': 'Bittern bill clacks in water', 'subtitles.wildercord.kit.bittern.rustle': 'Bittern feathers rustle'}


def _json(path, data):
    path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8', newline='\n')


def write(root):
    resources = root
    root = root / 'assets/wildercord'
    for sub in ('textures/entity', 'textures/item', 'items', 'models/item'):
        (root / sub).mkdir(parents=True, exist_ok=True)
    paint_skin().save(root / 'textures/entity/siltcrest_bittern.png')
    egg_icon().save(root / 'textures/item/siltcrest_bittern_spawn_egg.png')
    _json(root / 'items/siltcrest_bittern_spawn_egg.json', {'model': {'type': 'minecraft:model', 'model': 'wildercord:item/siltcrest_bittern_spawn_egg'}})
    _json(root / 'models/item/siltcrest_bittern_spawn_egg.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'wildercord:item/siltcrest_bittern_spawn_egg'}})
    (resources / 'data/wildercord/loot_table/entities').mkdir(parents=True, exist_ok=True)
    _json(resources / 'data/wildercord/loot_table/entities/siltcrest_bittern.json', {'type': 'minecraft:entity', 'pools': []})
