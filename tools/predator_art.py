"""0.13 tameable predators, the black bobcat's kin: the Frost Lynx and the Dune Cougar. Their skins are the bobcat's
(bobcat_art.skin) in palettes of their own, finished here: the lynx's spots, the cougar's long tail (BlackBobcatModel's
long-tail build, two 3x3x5 lengths at 44,50 and 38,42). Also their spawn eggs, loot, spawn ground and field-guide text."""
from bobcat_art import BODY, CHEEKS, EAR, HEAD, LEG, MUZZLE, egg_icon, fill, skin
from item_art import hexc, mix, ramp
from monster_art import faces, fur, smooth
from world_art import box, noise

LANG = {
    'entity.wildercord.frost_lynx': 'Frost Lynx',
    'item.wildercord.frost_lynx_spawn_egg': 'Frost Lynx Spawn Egg',
    'guide.wildercord.frost_lynx.hint': 'Look for a spotted silver cat in the snowy taigas and groves.',
    'guide.wildercord.frost_lynx': 'A silver, dark-spotted cat of the snowfields with long black ear tufts and pale green eyes, kin to the black bobcat and nearly as big. Cold never touches it. Offer it raw rabbit or chicken: one time in three it becomes yours. Its bite chills, slowing what it bit and frosting it over. A tame lynx follows, sits when told, fights beside you and heals on more of what tamed it.',
    'entity.wildercord.dune_cougar': 'Dune Cougar',
    'item.wildercord.dune_cougar_spawn_egg': 'Dune Cougar Spawn Egg',
    'guide.wildercord.dune_cougar.hint': 'Look for a long tawny cat on the savannas and badlands.',
    'guide.wildercord.dune_cougar': 'A long, tawny cat of the savannas and badlands with a cream muzzle, amber eyes and a dark-tipped rope of a tail, as big as the black bobcat. It is warier than its kin: raw beef, mutton or pork wins it over only one time in four. A tame cougar standing near you watches over you, and every monster close by glows for a while, picked out by its eye.',
}

# ============================================================== the Frost Lynx

LYNX = dict(
    coat=ramp("#24221F", "#5C5852", "#7F7A72", "#9C968C", "#B5AFA5", "#CBC6BC", "#E0DCD3"),
    smoke=ramp("#B9B5AE", "#CAC6BF", "#D9D6D0", "#E6E4DF", "#F2F1EE"),
    eye=hexc("#B9D77A"), eye_deep=hexc("#7FA24A"),
    nose=hexc("#9A6E70"), bean=hexc("#5E4A4C"), whisker=hexc("#F4F4F2"),
    ear_spot=hexc("#EDEBE6"), ear_inner=hexc("#8A7477"), tuft=hexc("#141312"),
)
SPOT = hexc("#3A3631")


def spots(cv, area, seed, size=1.6, edge=0.72):
    """Scattered dark spots, small and crisp, a little softened at their rims."""
    x0, y0, w, h = area
    for y in range(h):
        for x in range(w):
            v = smooth(x0 + x, y0 + y, seed, size)
            if v > edge:
                c = cv.get(x0 + x, y0 + y)
                if c is not None:
                    cv.put(x0 + x, y0 + y, SPOT if v > edge + 0.06 else mix(c, SPOT, 0.5))


def lynx():
    """The bobcat's build in silver: dark spots down the back, flanks and legs, a pale ruff of cheeks, a black-tipped bob and
    long black tufts."""
    cv = skin(LYNX, sheet=True)
    for name, area in faces(BODY, "top", "right", "left", "back"):
        spots(cv, area, 70 + len(name))
    for name, area in faces(LEG, "front", "right", "left", "back"):
        spots(cv, area, 80 + len(name), size=1.3, edge=0.82)
    spots(cv, HEAD["top"], 77, size=1.2, edge=0.84)
    for name, area in faces(CHEEKS):
        fur(cv, area, LYNX["smoke"], 90 + len(name), name, base=3)
    # Dark lines running back from the outer corners of the eyes, as a lynx has.
    x0, y0, w, h = HEAD["front"]
    for x, y in ((0, 5), (0, 6), (11, 5), (11, 6)):
        cv.put(x0 + x, y0 + y, SPOT)
    return cv.image()


# ============================================================== the Dune Cougar

COUGAR = dict(
    coat=ramp("#3A2614", "#7A532D", "#93673A", "#AB7D49", "#BF925B", "#D1A66F", "#E0BB88"),
    smoke=ramp("#C9B08D", "#D8C2A2", "#E4D2B6", "#EEE1CB", "#F7EFE1"),
    eye=hexc("#E2A93F"), eye_deep=hexc("#A9701E"),
    nose=hexc("#B07466"), bean=hexc("#5A3A30"), whisker=hexc("#F2EADB"),
    ear_spot=hexc("#3A2A20"), ear_inner=hexc("#9C6E5C"), rosettes=False,
)
COUGAR["tuft"] = COUGAR["coat"][4]
TAIL_NEAR = box(44, 50, 3, 3, 5)
TAIL_FAR = box(38, 42, 3, 3, 5)
DARK = hexc("#2A1E16")


def cougar():
    """The bobcat's build in tawny: a cream muzzle, chin and chest, dark marks at the sides of the muzzle, dark-backed ears, no
    rosettes to speak of, and a long tail fading to a dark tip."""
    cv = skin(COUGAR, sheet=True)
    for name, area in faces(TAIL_NEAR):
        fur(cv, area, COUGAR["coat"], 100 + len(name), name, base=3, along_y=name in ("top", "bottom"))
    for name, area in faces(TAIL_FAR):
        fur(cv, area, COUGAR["coat"], 110 + len(name), name, base=3, along_y=name in ("top", "bottom"))
    # The far length darkens towards its tip, and its end is dark through.
    for name in ("top", "bottom", "right", "left"):
        x0, y0, w, h = TAIL_FAR[name]
        along_x = name in ("right", "left")
        n = w if along_x else h
        for i in range(n):
            t = i / max(1, n - 1)
            if t < 0.45:
                continue
            for j in range(h if along_x else w):
                x, y = (i, j) if along_x else (j, i)
                # The tip (the +z end) is at x = 0 on the right face, x = w - 1 on the left, and y = 0 on the top and bottom.
                if name == "right":
                    x = w - 1 - x
                elif not along_x:
                    y = h - 1 - y
                c = cv.get(x0 + x, y0 + y)
                if c is not None:
                    cv.put(x0 + x, y0 + y, mix(c, DARK, (t - 0.45) / 0.55 * 0.85))
    fill(cv, TAIL_FAR["back"], DARK)
    for name in ("bottom",):
        fur(cv, TAIL_NEAR[name], COUGAR["smoke"], 120, name, base=2, along_y=True)
    # The muzzle's dark whisker marks, and a pale chin.
    x0, y0, w, h = MUZZLE["front"]
    for y in (1, 2):
        cv.put(x0, y0 + y, DARK)
        cv.put(x0 + w - 1, y0 + y, DARK)
    for name in ("right", "left"):
        x0, y0, w, h = MUZZLE[name]
        for y in range(1, h):
            cv.put(x0 + w // 2, y0 + y, mix(DARK, COUGAR["coat"][2], 0.35))
    x0, y0, w, h = EAR["back"]
    fill(cv, (x0, y0, w, h), mix(DARK, COUGAR["coat"][1], 0.3))
    # Pale eyebrow marks, as a cougar has.
    x0, y0, w, h = HEAD["front"]
    for x in (2, 3, 8, 9):
        cv.put(x0 + x, y0 + 3, COUGAR["smoke"][3])
    return cv.image()


# ============================================================== eggs, loot and ground

EGGS = {
    'frost_lynx': (("#8E897F", "#B3ADA2", "#D9D5CC"), "#2E2B27"),
    'dune_cougar': (("#8A5E33", "#AE7F4A", "#D0A26C"), "#F0E2C8"),
}
GROUND = {
    'frost_lynx': ['minecraft:snow_block', 'minecraft:snow', 'minecraft:grass_block', 'minecraft:podzol', 'minecraft:coarse_dirt',
                   'minecraft:stone', 'minecraft:powder_snow'],
    'dune_cougar': ['minecraft:grass_block', 'minecraft:coarse_dirt', 'minecraft:dirt', 'minecraft:red_sand', 'minecraft:sand',
                    'minecraft:terracotta', '#minecraft:terracotta'],
}


def write(g):
    for name, image in (('frost_lynx', lynx()), ('dune_cougar', cougar())):
        g.save(image, g.ASSETS / f'textures/entity/wildlife/{name}.png')
        egg = f'{name}_spawn_egg'
        tones, spot = EGGS[name]
        g.save(egg_icon(tones, spot, name), g.ASSETS / f'textures/item/{egg}.png')
        g.write_json(g.ASSETS / f'models/item/{egg}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'wildercord:item/{egg}'}})
        g.write_json(g.ASSETS / f'items/{egg}.json', {'model': {'type': 'minecraft:model', 'model': f'wildercord:item/{egg}'}})
        g.write_json(g.DATA / f'loot_table/entities/{name}.json', {'type': 'minecraft:entity', 'pools': [{
            'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'minecraft:leather', 'functions': [
                {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 0, 'max': 2}},
                {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting', 'count': {'type': 'minecraft:uniform', 'min': 0, 'max': 1}}]}]}],
            'random_sequence': f'wildercord:entities/{name}'})
        g.write_json(g.DATA / f'tags/block/spawns_on/{name}.json', {'replace': False, 'values': GROUND[name]})
