"""The 0.13 magic additions: the mage-hunter's skin, its spawn egg and loot, the Ritual Tablet, and their English text. Called by
generate_assets.py (write(g), and LANG merged into en_us.json).

The skin is painted on the vanilla illager layout (64x64): head 8x10x8 at (0, 0), hat 8x12x8 at (32, 0), nose 2x4x2 at
(24, 0), body 8x12x6 at (16, 20), the long coat 8x20x6 at (0, 38), the crossed arms at (44, 22) and (40, 38), the legs
4x12x4 at (0, 22) and the free arms 4x12x4 at (40, 46). The hat layer is drawn as a deep hood, open at the face.

Run this file on its own to render a review image into build/art-preview/magic_art.png.
"""
from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from item_art import Canvas, hexc, mix  # noqa: E402
from world_art import Sheet, box, noise  # noqa: E402
import monster_art  # noqa: E402

LIGHT = {"top": 1, "bottom": -2, "front": 0, "back": -1, "right": 0, "left": -1}

SKIN = [hexc(c) for c in ("#5E6A60", "#748074", "#8A968A", "#9EAA9C")]
HOOD = [hexc(c) for c in ("#16141E", "#211E2C", "#2C2838", "#383348")]
COAT = [hexc(c) for c in ("#3A3A40", "#4A4A52", "#5A5A63", "#6A6A74")]
LEATHER = [hexc(c) for c in ("#2A1E16", "#3A2A1E", "#4A3626", "#5A4430")]
TROUSER = [hexc(c) for c in ("#1E1E24", "#26262E", "#30303A", "#3A3A46")]
RUNE = hexc("#6A4E8A")
RUNE_DIM = hexc("#4A3866")
BUCKLE = hexc("#A89A6A")

HEAD = box(0, 0, 8, 10, 8)
HAT = box(32, 0, 8, 12, 8)
NOSE = box(24, 0, 2, 4, 2)
BODY = box(16, 20, 8, 12, 6)
JACKET = box(0, 38, 8, 20, 6)
ARMS_A = box(44, 22, 4, 8, 4)
ARMS_B = box(40, 38, 8, 4, 4)
LEG = box(0, 22, 4, 12, 4)
ARM = box(40, 46, 4, 12, 4)


def cloth(cv, area, tones, seed, face, base=1):
    """Woven cloth: a faint weave and soft folds, lit like vanilla mobs."""
    x0, y0, w, h = area
    for y in range(h):
        for x in range(w):
            t = base + LIGHT[face]
            n = noise(x0 + x, y0 + y, seed)
            if n > 0.82:
                t += 1
            elif n < 0.14:
                t -= 1
            if (x + 2 * y) % 5 == 0 and face in ("front", "back", "right", "left"):
                t -= 1
            cv.put(x0 + x, y0 + y, tones[max(0, min(len(tones) - 1, t))])


def paint(cv, part, tones, seed, base=1):
    for name, area in part.items():
        if area[2] > 0 and area[3] > 0:
            cloth(cv, area, tones, seed + len(name), name, base)


def rune_marks(cv, area, seed, count):
    """Broken runes stitched into the coat: short strokes, half of them cut through."""
    x0, y0, w, h = area
    for i in range(count):
        n = noise(i, seed, seed * 7)
        x = x0 + 1 + int(n * max(1, w - 3))
        y = y0 + 2 + int(noise(seed, i, 3) * max(1, h - 5))
        cv.put(x, y, RUNE)
        cv.put(x, y + 1, RUNE)
        cv.put(x + 1, y + 1, RUNE_DIM)
        cv.put(x + 1, y + 2, RUNE)


def mage_hunter_texture():
    cv = Sheet(64, 64)
    paint(cv, HEAD, SKIN, 1)
    paint(cv, NOSE, SKIN, 2)
    # The face: a heavy brow, narrow pale eyes, a scar across the left cheek, a grim mouth.
    fx, fy, fw, fh = HEAD["front"]
    for x in range(8):
        cv.put(fx + x, fy + 3, SKIN[0])
    for x, c in ((1, hexc("#EDE6C8")), (2, hexc("#2A3A2A")), (5, hexc("#2A3A2A")), (6, hexc("#EDE6C8"))):
        cv.put(fx + x, fy + 4, c)
    cv.put(fx + 6, fy + 5, hexc("#6A5450"))
    cv.put(fx + 7, fy + 6, hexc("#6A5450"))
    for x in range(2, 6):
        cv.put(fx + x, fy + 8, SKIN[0])
    # The hood: deep cloth, open at the face; the opening's edge a shade lighter, and dark inside.
    paint(cv, HAT, HOOD, 3)
    hx, hy, hw, hh = HAT["front"]
    for y in range(2, hh):
        for x in range(1, hw - 1):
            cv.put(hx + x, hy + y, None)
    for y in range(1, hh):
        cv.put(hx, hy + y, HOOD[3])
        cv.put(hx + hw - 1, hy + y, HOOD[3])
    for x in range(hw):
        cv.put(hx + x, hy + 1, HOOD[3])
    bx, by, bw, bh = HAT["bottom"]
    for y in range(bh):
        for x in range(bw):
            cv.put(bx + x, by + y, None)
    # A rune burnt into the hood's crown, and the cut through it.
    tx, ty, tw, th = HAT["top"]
    for (x, y) in ((3, 2), (4, 3), (3, 4), (4, 5)):
        cv.put(tx + x, ty + y, RUNE_DIM)
    # Leather jerkin under the coat, and its belt.
    paint(cv, BODY, LEATHER, 4)
    # The long coat: grey wool, a belt with a brass buckle, broken runes down the front and the back.
    paint(cv, JACKET, COAT, 5)
    for name in ("front", "back", "right", "left"):
        x0, y0, w, h = JACKET[name]
        for x in range(w):
            cv.put(x0 + x, y0 + 8, LEATHER[1])
        if name == "front":
            cv.put(x0 + w // 2 - 1, y0 + 8, BUCKLE)
            cv.put(x0 + w // 2, y0 + 8, BUCKLE)
            for y in range(9, h):
                cv.put(x0 + w // 2, y0 + y, COAT[0])
        if name in ("front", "back"):
            rune_marks(cv, JACKET[name], 11 + len(name), 3)
    # Sleeves, gloved hands.
    for part, seed in ((ARMS_A, 6), (ARMS_B, 7), (ARM, 8)):
        paint(cv, part, COAT, seed)
    for name in ("front", "back", "right", "left"):
        x0, y0, w, h = ARM[name]
        for y in range(h - 3, h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, LEATHER[2] if (x + y) % 3 else LEATHER[1])
        cv.put(x0, y0 + h - 4, LEATHER[0])
    x0, y0, w, h = ARM["bottom"]
    for y in range(h):
        for x in range(w):
            cv.put(x0 + x, y0 + y, LEATHER[1])
    for name in ("front", "back", "right", "left"):
        x0, y0, w, h = ARMS_B[name]
        for x in range(min(2, w)):
            for y in range(h):
                cv.put(x0 + x, y0 + y, LEATHER[2])
            for y in range(h):
                cv.put(x0 + w - 1 - x, y0 + y, LEATHER[2])
    # Dark trousers and boots.
    paint(cv, LEG, TROUSER, 9)
    for name in ("front", "back", "right", "left"):
        x0, y0, w, h = LEG[name]
        for y in range(h - 4, h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, LEATHER[1] if y > h - 4 else LEATHER[2])
    return cv.image()


EGG_MOTIF = """
    ................
    ................
    ................
    ................
    ......hhhh......
    .....hhhhhh.....
    .....h.ee.h.....
    .....h....h.....
    ................
    .....r..r.......
    ......r..r......
    ................
    ................
    ................
    ................
    ................
"""


def egg_icon():
    monster_art.EGGS.setdefault("mage_hunter", (("#2A2A30", "#45454E", "#5E5E68"), "#6A4E8A", EGG_MOTIF,
                                                {"h": "#16141E", "e": "#EDE6C8", "r": "#8A6AB0"}))
    return monster_art.egg_icon("mage_hunter")


LOOT = {"type": "minecraft:entity", "random_sequence": "wildercord:entities/mage_hunter", "pools": [
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:emerald", "modifier": [
        {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}},
        {"type": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting",
         "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}]}]},
    {"rolls": 1, "condition": {"type": "minecraft:all_of", "terms": [
        {"type": "minecraft:killed_by_player"},
        {"type": "minecraft:random_chance_with_enchanted_bonus", "enchantment": "minecraft:looting", "unenchanted_chance": 0.25,
         "enchanted_chance": {"type": "minecraft:linear", "base": 0.3, "per_level_above_first": 0.05}}]},
     "entries": [{"type": "minecraft:item", "name": "wildercord:mana_crystal"}]},
]}

STONE = [hexc(c) for c in ("#3E3A46", "#55505E", "#6C6678", "#857F92", "#A29CB0")]
GLOW = [hexc(c) for c in ("#7A4EC0", "#A47AE8", "#D8C0FF")]


def tablet_icon():
    """A slate tablet with a rounded head, a ritual circle carved into it and glowing violet."""
    cv = Canvas()
    for y in range(1, 15):
        for x in range(3, 13):
            if y < 4 and (x - 7.5) ** 2 + (y - 4.5) ** 2 > 22:
                continue
            edge = x in (3, 12) or y == 14 or (y < 4 and (x - 7.5) ** 2 + (y - 4.5) ** 2 > 14)
            t = 0 if edge else 2 + (1 if noise(x, y, 31) > 0.7 else 0) - (1 if noise(x, y, 37) < 0.2 else 0)
            if not edge and (x == 4 or y == 13):
                t = 1
            if not edge and x == 11 and y < 13:
                t = 1
            cv.put(x, y, STONE[t])
    # Light catching the rounded head.
    for x in (6, 7, 8, 9):
        cv.put(x, 2 if x in (7, 8) else 3, STONE[4])
    # The circle: a ring of eight glowing points with a star at its heart.
    ring = ((7, 5), (8, 5), (10, 7), (10, 8), (8, 10), (7, 10), (5, 8), (5, 7), (9, 6), (6, 6), (9, 9), (6, 9))
    for x, y in ring:
        cv.put(x, y, GLOW[1] if (x + y) % 2 else GLOW[0])
    for x, y in ((7, 7), (8, 8)):
        cv.put(x, y, GLOW[2])
    for x, y in ((8, 7), (7, 8)):
        cv.put(x, y, GLOW[1])
    # A line of runes below.
    for x in (5, 7, 9):
        cv.put(x, 12, GLOW[0])
    cv.put(6, 12, STONE[1])
    return cv.image()


TABLET_RECIPE = {"type": "minecraft:crafting_shaped", "category": "misc",
                 "key": {"A": "minecraft:amethyst_shard", "S": "minecraft:polished_deepslate", "B": "minecraft:book",
                         "G": "minecraft:gold_ingot"},
                 "pattern": [" A ", "SBS", "GSG"],
                 "result": {"id": "wildercord:ritual_tablet"}}

LANG = {
    "entity.wildercord.mage_hunter": "Mage-Hunter",
    "item.wildercord.mage_hunter_spawn_egg": "Mage-Hunter Spawn Egg",
    "message.wildercord.mage_hunter.one": "Someone has been following the smell of your magic. A mage-hunter is coming.",
    "message.wildercord.mage_hunter.band": "Someone has been following the smell of your magic. Mage-hunters are coming.",
    "message.wildercord.mage_hunter.drained": "The hunter's axe took %s mana",
    "item.wildercord.ritual_tablet": "Ritual Tablet",
    "tooltip.wildercord.ritual_tablet.set": "Ritual: %s",
    "tooltip.wildercord.ritual_tablet.cost": "%s mana, shared by the circle · Circle %s · consumes %s",
    "tooltip.wildercord.ritual_tablet.use": "Hold use to channel. Sneak-use to choose another ritual.",
    "ritual.wildercord.bounty": "Bounty",
    "ritual.wildercord.bounty.desc": "Every crop within 12 blocks grows three stages",
    "ritual.wildercord.clear_skies": "Clear Skies",
    "ritual.wildercord.clear_skies.desc": "The rain stops, and the sky stays clear for a day",
    "ritual.wildercord.call_storm": "Call Storm",
    "ritual.wildercord.call_storm.desc": "A thunderstorm breaks, and rages a quarter of a day",
    "ritual.wildercord.sanctuary": "Sanctuary",
    "ritual.wildercord.sanctuary.desc": "For five minutes, monsters within 32 blocks are weakened, forget you and are pushed out",
    "ritual.wildercord.dawn": "Dawn",
    "ritual.wildercord.dawn.desc": "The night ends, and the sun rises at once",
    "message.wildercord.ritual.chosen": "Ritual: %s (Circle %s)",
    "message.wildercord.ritual.done": "The ritual of %s is worked.",
    "message.wildercord.ritual.shared": "You gave %s mana to the ritual",
    "message.wildercord.ritual.no_cord": "A ritual needs a Cord to channel it",
    "message.wildercord.ritual.low_circle": "Only a heart of %s circles can lead this ritual",
    "message.wildercord.ritual.no_reagent": "This ritual consumes %2$s, and you have none",
    "message.wildercord.ritual.low_mana": "The circle holds too little mana for this ritual. Gather more casters within 8 blocks",
    "message.wildercord.ritual.wrong_place": "This ritual can't be worked here, or now",
    "message.wildercord.ritual.nothing_to_do": "The ritual found nothing to work on",
}


def write(g):
    g.save(mage_hunter_texture(), g.ASSETS / "textures/entity/mage_hunter.png")
    egg = "mage_hunter_spawn_egg"
    g.save(egg_icon(), g.ASSETS / f"textures/item/{egg}.png")
    g.item_model(egg, egg)
    g.write_json(g.ASSETS / f"items/{egg}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{egg}"}})
    g.write_json(g.DATA / "loot_table/entities/mage_hunter.json", LOOT)
    g.save(tablet_icon(), g.ASSETS / "textures/item/ritual_tablet.png")
    g.item_model("ritual_tablet", "ritual_tablet")
    g.write_json(g.ASSETS / "items/ritual_tablet.json", {"model": {"type": "minecraft:model", "model": "wildercord:item/ritual_tablet"}})
    g.write_json(g.DATA / "recipe/ritual_tablet.json", TABLET_RECIPE)


def preview(out_dir):
    skin = mage_hunter_texture().resize((512, 512), Image.NEAREST)
    egg = egg_icon().resize((128, 128), Image.NEAREST)
    sheet = Image.new("RGBA", (660, 528), (44, 40, 52, 255))
    sheet.paste(skin, (8, 8), skin)
    sheet.paste(egg, (528, 8), egg)
    tablet = tablet_icon().resize((128, 128), Image.NEAREST)
    sheet.paste(tablet, (528, 144), tablet)
    out = Path(out_dir) / "magic_art.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    return str(out)


if __name__ == "__main__":
    print(preview(Path(__file__).resolve().parent.parent / "build/art-preview"))
