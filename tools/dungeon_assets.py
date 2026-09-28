"""The dimension dungeons' assets: the bosses' skins and glow layers, the trophies' icons, the
altar's models, the loot tables and the worldgen data. Called by generate_assets.py (which owns the
English text: DUNGEON_LANG there); run that, not this.

Skins are painted face by face on each model's box-UV layout, like world_art.py's, but from rules
rather than ASCII: iron plates with seams and rivets, magma in the cracks between them, chain links,
and so on. Glow layers are pale-to-bright on transparency, drawn emissive in game. Every choice is
seeded, so a run always writes the same files.
"""
from __future__ import annotations

import math
import random

from item_art import Canvas, blit, hexc, mix, ramp
from world_art import Sheet, box, fill, noise

# ============================================================== shared painting


def shade(tones, t):
    return tones[max(0, min(len(tones) - 1, t))]


def faces(area_box, *names):
    return [area_box[n] for n in names]


def plates(cv, area, tones, seed, seam=5, rivets=True, base=2, vertical=False):
    """Iron plating: a mottled face split by seams every `seam` pixels, lit from the top, with rivets
    at the seam corners."""
    x0, y0, w, h = area
    for y in range(h):
        for x in range(w):
            along = x if vertical else y
            t = base
            if along % seam == 0:
                t -= 1
            elif along % seam == 1:
                t += 1
            n = noise(x0 + x, y0 + y, seed)
            if n < 0.1:
                t -= 1
            elif n > 0.93:
                t += 1
            if y == 0:
                t += 1
            elif y == h - 1:
                t -= 1
            cv.put(x0 + x, y0 + y, shade(tones, t))
            if rivets and along % seam == 2 and (x if not vertical else y) % 4 == 1:
                cv.put(x0 + x, y0 + y, shade(tones, base + 2))


def crack_paths(area, seed, count, length):
    """Random-walk cracks inside a face: a set of (x, y) in face coordinates."""
    x0, y0, w, h = area
    rng = random.Random(seed)
    out = set()
    for _ in range(count):
        x, y = rng.randrange(w), rng.randrange(h)
        dx, dy = rng.choice(((1, 0), (-1, 0), (0, 1), (0, -1)))
        for _ in range(length):
            out.add((x, y))
            if rng.random() < 0.35:
                dx, dy = rng.choice(((1, 0), (-1, 0), (0, 1), (0, -1), (dx, dy)))
            x = max(0, min(w - 1, x + dx))
            y = max(0, min(h - 1, y + dy))
    return out


def paint_set(cv, area, pixels, colour):
    x0, y0, _, _ = area
    for x, y in pixels:
        cv.put(x0 + x, y0 + y, colour)


def halo(cv, area, pixels, colour):
    """Rings a set of face pixels with `colour`, inside the face, where nothing's painted yet."""
    x0, y0, w, h = area
    for x, y in pixels:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < w and 0 <= ny < h and (nx, ny) not in pixels and cv.get(x0 + nx, y0 + ny) is None:
                cv.put(x0 + nx, y0 + ny, colour)


def disc(cx, cy, r):
    return {(x, y) for x in range(int(cx - r - 1), int(cx + r + 2)) for y in range(int(cy - r - 1), int(cy + r + 2))
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r}


# ============================================================== the Cinder Warden (CinderWardenModel, 128x128)

IRON = ramp("#141012", "#1E1A1C", "#2A2426", "#3A3234", "#4A4042", "#5E5254", "#76686A")
CHAIN = ramp("#26262C", "#3A3A42", "#56565F", "#74747E", "#9A9AA6")
GOLD = ramp("#5A3E10", "#8A6A1E", "#C89A2E", "#E8C25A")
EMBER = {"deep": hexc("#5A1406"), "low": hexc("#8A2A0A"), "mid": hexc("#C8400E"), "hot": hexc("#FF7A1E")}
GLOW_HOT = (255, 190, 90, 255)
GLOW_MID = (255, 120, 40, 230)
GLOW_HALO = (255, 90, 30, 80)

W_TORSO = box(0, 0, 18, 18, 11)
W_HEAD = box(60, 0, 9, 9, 9)
W_PAULDRON = box(60, 18, 9, 6, 10)
W_ARM = box(98, 0, 7, 16, 7)
W_GAUNTLET = box(96, 34, 8, 7, 8)
W_LEG = box(0, 30, 8, 20, 8)
W_BELT = box(32, 34, 17, 5, 10)
W_CHAIN = box(0, 60, 2, 12, 2)

# Where the magma shows, face by face: (area, seed, cracks, length).
W_CRACKS = [
    (W_TORSO["front"], 11, 7, 9), (W_TORSO["back"], 12, 5, 8), (W_TORSO["right"], 13, 3, 7), (W_TORSO["left"], 14, 3, 7),
    (W_ARM["front"], 21, 2, 7), (W_ARM["right"], 22, 2, 6), (W_ARM["left"], 23, 2, 6), (W_ARM["back"], 24, 2, 6),
    (W_LEG["front"], 31, 2, 8), (W_LEG["right"], 32, 2, 6), (W_LEG["left"], 33, 2, 6), (W_LEG["back"], 34, 1, 6),
    (W_PAULDRON["top"], 41, 2, 6), (W_GAUNTLET["front"], 51, 2, 5), (W_HEAD["top"], 61, 1, 5),
]
W_CORE = (9, 7, 3.4)          # on the torso's front: centre x, y and radius of the molten core
# the helm's front: a T-shaped visor slit with two ember eyes
W_VISOR = """
    .........
    .........
    .#######.
    .#e###e#.
    ....#....
    ....#....
    ....#....
    .........
    .........
"""


def cinder_warden_texture():
    cv = Sheet(128, 128)
    for part, seed, seam, vertical in ((W_TORSO, 1, 6, False), (W_HEAD, 2, 4, False), (W_PAULDRON, 3, 3, False),
                                       (W_ARM, 4, 5, False), (W_GAUNTLET, 5, 3, True), (W_LEG, 6, 5, False)):
        for name, area in part.items():
            plates(cv, area, IRON, seed * 10 + len(name), seam=seam, vertical=vertical, base=3 if name == "top" else 2)
    # The belt: dark iron with a gilded buckle and trim.
    for name, area in W_BELT.items():
        plates(cv, area, IRON, 70 + len(name), seam=3, base=1)
        x0, y0, w, h = area
        for x in range(w):
            cv.put(x0 + x, y0, GOLD[1])
            cv.put(x0 + x, y0 + h - 1, GOLD[0])
    bx, by, bw, bh = W_BELT["front"]
    fill(cv, (bx + bw // 2 - 2, by + 1, 5, bh - 2), GOLD[2])
    fill(cv, (bx + bw // 2 - 1, by + 2, 3, bh - 4), GOLD[3])
    # The pauldrons' gilded rims.
    for name in ("front", "right", "left", "back"):
        x0, y0, w, h = W_PAULDRON[name]
        for x in range(w):
            cv.put(x0 + x, y0 + h - 1, GOLD[1] if x % 2 else GOLD[2])
    # Chain links.
    for name, area in W_CHAIN.items():
        x0, y0, w, h = area
        for y in range(h):
            for x in range(w):
                link = (y // 2) % 2
                cv.put(x0 + x, y0 + y, CHAIN[3 if (x + y) % 2 == link else 1] if y % 2 == 0 else CHAIN[2])
    # The magma between the plates: dark red in the skin (the glow layer lights it).
    for area, seed, count, length in W_CRACKS:
        paths = crack_paths(area, seed, count, length)
        paint_set(cv, area, paths, EMBER["mid"])
    tx, ty, _, _ = W_TORSO["front"]
    for (x, y) in disc(W_CORE[0], W_CORE[1], W_CORE[2]):
        cv.put(tx + x, ty + y, EMBER["hot"])
    for (x, y) in disc(W_CORE[0], W_CORE[1], W_CORE[2] + 1.2) - disc(W_CORE[0], W_CORE[1], W_CORE[2]):
        cv.put(tx + x, ty + y, IRON[4])
    # The helm's visor: a black slit.
    hx, hy, _, _ = W_HEAD["front"]
    for y, row in enumerate(line for line in (l.strip() for l in W_VISOR.strip("\n").splitlines()) if line):
        for x, ch in enumerate(row):
            if ch in "#e":
                cv.put(hx + x, hy + y, IRON[0] if ch == "#" else EMBER["low"])
    return cv.image()


def cinder_warden_glow_texture():
    """What burns: the magma in its cracks and its core, bright on transparency."""
    cv = Sheet(128, 128)
    for area, seed, count, length in W_CRACKS:
        paths = crack_paths(area, seed, count, length)
        paint_set(cv, area, paths, GLOW_MID)
        halo(cv, area, paths, GLOW_HALO)
    tx, ty, _, _ = W_TORSO["front"]
    core = disc(W_CORE[0], W_CORE[1], W_CORE[2])
    for (x, y) in core:
        d = math.hypot(x - W_CORE[0], y - W_CORE[1]) / W_CORE[2]
        cv.put(tx + x, ty + y, (255, int(250 - 110 * d), int(200 - 170 * d), 255))
    halo(cv, W_TORSO["front"], core, (255, 120, 40, 120))
    return cv.image()


def cinder_warden_eyes_texture():
    cv = Sheet(128, 128)
    hx, hy, _, _ = W_HEAD["front"]
    for y, row in enumerate(line for line in (l.strip() for l in W_VISOR.strip("\n").splitlines()) if line):
        for x, ch in enumerate(row):
            if ch == "e":
                cv.put(hx + x, hy + y, (255, 236, 150, 255))
            elif ch == "#":
                cv.put(hx + x, hy + y, (255, 110, 30, 70))
    return cv.image()


# ============================================================== trophies (16x16)

CINDER_HEART = """
    ................
    ......CC........
    .....C..C.......
    ....C....C......
    ...ooo..ooo.....
    ..oMMMooMMMo....
    .oMHHMMMMmmMo...
    .oMHWHMMmmmMo...
    .oMHHMMMMmmMo...
    ..oMMMMMMmmo....
    ...oMMmMmmo.....
    ....oMMmmo......
    .....oMmo.......
    ......oo........
    ................
    ................
"""
CINDER_HEART_PAL = {"o": hexc("#1E1A1C"), "M": hexc("#C8400E"), "m": hexc("#8A2A0A"), "H": hexc("#FF9A3A"),
                    "W": hexc("#FFE6A0"), "C": hexc("#74747E")}


def trophy_icon(text, pal):
    cv = Canvas(16)
    blit(cv, text, pal)
    return cv.image()


# ============================================================== writing it all out


def elements_runes(runes, elements, tiers, innate):
    return [r["path"] for r in runes if r["element"] in elements and r["tier"] in tiers and r["path"] not in innate and r["family"] == "effect"]


# The dungeons: their structure data, where they grow, and what their chests and bosses hold.
DUNGEONS = {
    "ember_sanctum": {
        "boss": "cinder_warden", "trophy": "cinder_heart", "elements": ("fire", "earth"), "name": "the Ember Sanctum",
        "step": "underground_structures", "biomes": ["minecraft:nether_wastes", "minecraft:basalt_deltas", "minecraft:crimson_forest"],
        "spacing": 36, "separation": 12, "salt": 20260928,
        "spawns": [("minecraft:wither_skeleton", 3), ("minecraft:blaze", 1), ("minecraft:magma_cube", 1)],
        "extras": [("minecraft:blaze_powder", 4, 2, 6), ("minecraft:magma_cream", 3, 1, 4), ("minecraft:gold_ingot", 3, 2, 6),
                   ("minecraft:netherite_scrap", 1, 1, 1)],
    },
}


def main(g, runes):
    """Writes everything. `g` is generate_assets (for its writers and the rune list's helpers)."""
    tex = g.ASSETS / "textures"
    data = g.DATA
    # ---- skins
    g.save(cinder_warden_texture(), tex / "entity/cinder_warden.png")
    g.save(cinder_warden_glow_texture(), tex / "entity/cinder_warden_glow.png")
    g.save(cinder_warden_eyes_texture(), tex / "entity/cinder_warden_eyes.png")
    # ---- trophies
    icons = {"cinder_heart": trophy_icon(CINDER_HEART, CINDER_HEART_PAL)}
    for name, image in icons.items():
        g.save(image, tex / f"item/{name}.png")
        g.item_model(name, name)
        g.write_json(g.ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{name}"}})
    # ---- the altar: one model per kind, borrowing the dungeons' own stone
    kinds = {"cinder": ("minecraft:block/polished_blackstone_bricks", "minecraft:block/magma", "minecraft:block/polished_blackstone"),
             "astral": ("minecraft:block/purpur_pillar", "minecraft:block/end_portal_frame_top", "minecraft:block/end_stone_bricks"),
             "tide": ("minecraft:block/prismarine_bricks", "minecraft:block/sea_lantern", "minecraft:block/dark_prismarine")}
    face = lambda t, uv=(2, 2, 14, 14): {"texture": f"#{t}", "uv": list(uv)}
    variants = {}
    for kind, (side, top, bottom) in kinds.items():
        g.write_json(g.ASSETS / f"models/block/dungeon_altar_{kind}.json", {
            "parent": "minecraft:block/block",
            "textures": {"particle": side, "top": top, "side": side, "bottom": bottom},
            "elements": [{"from": [2, 0, 2], "to": [14, 14, 14], "faces": {
                "down": face("bottom"), "up": face("top"), "north": face("side", (2, 2, 14, 16)), "south": face("side", (2, 2, 14, 16)),
                "west": face("side", (2, 2, 14, 16)), "east": face("side", (2, 2, 14, 16))}}]})
        for awake in ("false", "true"):
            variants[f"awake={awake},kind={kind}"] = {"model": f"wildercord:block/dungeon_altar_{kind}"}
    g.write_json(g.ASSETS / "blockstates/dungeon_altar.json", {"variants": variants})
    g.write_json(g.ASSETS / "items/dungeon_altar.json", {"model": {"type": "minecraft:model", "model": "wildercord:block/dungeon_altar_cinder"}})

    fourth = [r["path"] for r in runes if r["tier"] == 4 and r["path"] not in g.INNATE]
    for key, d in DUNGEONS.items():
        second_third = elements_runes(runes, d["elements"], (2, 3), g.INNATE)
        third = elements_runes(runes, d["elements"], (3,), g.INNATE) or second_third
        # ---- the guarded hall's chest: runes of its elements, and what a keeper of the place would hoard
        g.write_json(data / f"loot_table/chests/{key}_hall.json", {"type": "minecraft:chest", "pools": [
            {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 3}, "entries": [g.rune_entry(p, 3) for p in second_third]},
            {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 3}, "entries": [
                g.item_entry("wildercord:torn_page", 5), g.item_entry("wildercord:mana_crystal", 3), g.item_entry("wildercord:blank_rune", 4, 2, 5),
                g.item_entry("minecraft:lapis_lazuli", 4, 3, 9)] + [g.item_entry(i, w, lo, hi) for i, w, lo, hi in d["extras"]]},
        ]})
        # ---- the vault: a Tier IV rune, runes of its elements, and treasure
        g.write_json(data / f"loot_table/chests/{key}_vault.json", {"type": "minecraft:chest", "pools": [
            {"rolls": 1, "entries": [g.rune_entry(p, 1) for p in fourth]},
            # Exclusive runes: the pool the location-exclusive runes of this dungeon go into. Until they exist it holds
            # Tier III runes of the dungeon's elements.
            {"rolls": 1, "entries": [g.rune_entry(p, 1) for p in third]},
            {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "entries": [
                g.item_entry("wildercord:mana_crystal", 4, 1, 2), g.item_entry("wildercord:torn_page", 4), g.item_entry("minecraft:diamond", 2, 1, 3),
                g.item_entry("minecraft:gold_ingot", 3, 2, 6), g.item_entry("minecraft:echo_shard", 1)] +
                [g.item_entry(i, w, lo, hi) for i, w, lo, hi in d["extras"]]},
        ]})
        # ---- the boss (its Tier IV rune, one its killer doesn't know, is dropped in code)
        g.write_json(data / f"loot_table/entities/{d['boss']}.json", {"type": "minecraft:entity", "pools": [
            {"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"wildercord:{d['trophy']}"}]},
            {"rolls": 1, "entries": [g.item_entry("wildercord:mana_crystal", 1, 3, 3)]},
            {"rolls": 1, "entries": [g.item_entry("wildercord:torn_page", 1)]},
            {"rolls": 1, "entries": [g.rune_entry(p, 1) for p in second_third]},
            # Exclusive runes: this boss's own rune goes here once the location-exclusive runes exist; for now, a Tier
            # III rune of its elements.
            {"rolls": 1, "entries": [g.rune_entry(p, 1) for p in third]},
        ]})
        # ---- where it grows
        g.write_json(data / f"worldgen/structure/{key}.json", {
            "type": "wildercord:dungeon", "dungeon": key, "biomes": f"#wildercord:has_structure/{key}",
            "spawn_overrides": {"monster": {"bounding_box": "piece", "spawns": [
                {"type": t, "weight": w, "count": 1} for t, w in d["spawns"]]}} if d["spawns"] else {},
            "step": d["step"], "terrain_adaptation": "none"})
        g.write_json(data / f"worldgen/structure_set/{key}.json", {
            "placement": {"type": "minecraft:random_spread", "salt": d["salt"], "separation": d["separation"], "spacing": d["spacing"]},
            "structures": [{"structure": f"wildercord:{key}", "weight": 1}]})
        g.write_json(data / f"tags/worldgen/biome/has_structure/{key}.json", {"values": d["biomes"]})
    g.write_json(data / "tags/worldgen/structure/dungeon.json", {"values": [f"wildercord:{k}" for k in DUNGEONS]})


def sources(runes, innate):
    """Rune path -> the dungeons whose chests hold it, for the tooltips' 'Found:' line."""
    found = {}
    for key, d in DUNGEONS.items():
        name = d["name"][4:] if d["name"].startswith("the ") else d["name"]
        for path in elements_runes(runes, d["elements"], (2, 3), innate):
            found.setdefault(path, []).append(name)
    return found
