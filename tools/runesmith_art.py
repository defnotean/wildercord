"""The Runesmith: the Scribing Desk's faces, the villager's outfit, and its trades as data.

Writes textures/block/scribing_desk_*.png, textures/entity/villager/profession/runesmith.png
(and the zombie villager's), the desk's model, blockstate, loot, recipe and tags, and every
Runesmith trade: data/wildercord/villager_trade/runesmith/<level>/*.json, the trade sets and
their tags, plus one rune on the wandering trader's shelf.
Run from the project root:  python tools/generate_assets.py  (it runs this)
"""
from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from item_art import Canvas, hexc, mix  # noqa: E402
from world_art import AM, DO, DS, GLOW, Sheet, box, noise, sprite  # noqa: E402
from generate_assets import ASSETS, DATA, RES, save, unlock_advancement, write_json  # noqa: E402

# ============================================================== the Scribing Desk

DESK_PAL = {
    # dark oak, lightest to darkest
    "H": DO[6], "h": DO[5], "w": DO[4], "W": DO[3], "u": DO[2], "U": DO[1], "X": DO[0],
    # deepslate for the base
    "S": DS[6], "s": DS[5], "t": DS[4], "T": DS[3], "q": DS[2], "Q": DS[1],
    # amethyst inlay and its glow
    "a": AM[2], "A": AM[4], "v": GLOW[4], "V": GLOW[6],
    # parchment, ink and a gold rule
    "P": hexc("#EADFBE"), "p": hexc("#D2C298"), "g": hexc("#B8A474"), "i": hexc("#5A3A8A"),
    "y": hexc("#C8962E"), "Y": hexc("#F2CE6A"),
    # the quill
    "f": hexc("#F4F0E8"), "F": hexc("#C9C2B4"),
}

# The slanted writing surface (rows 1-13 show): a rune sheet pinned in a frame, a quill beside it.
DESK_TOP = """
    WWWWWWWWWWWWWWWW
    WhhhhhhhhhhhhhhW
    WhyyyyyyyyyyyyuW
    WhyPPPPpPPPPyfuW
    WhyPiPvPpiPPyfuW
    WhyPPPPpPPiPyFuW
    WhyPvVvppPPPyfuW
    WhyPPVpPPvPPyFuW
    WhyPvVvPpPiPyfuW
    WhyPPpPPPPPPyFuW
    WhyPiPPvPiPgyfuW
    WhyPPPPpPPPPyXuW
    WhyyyyyyyyyyyyuW
    WhuuuuuuuuuuuuUW
    WhuuuuuuuuuuuuUW
    WWWWWWWWWWWWWWWW
"""

# The board's edges (rows 0-8) and the column's sides (rows 8-16, turned on their side in the model).
DESK_SIDES = """
    WWWWWWWWWWWWWWWW
    hYYYYYYYYYYYYYYh
    wwwwwwwwwwwwwwww
    UUUUUUUUUUUUUUUU
    WWWWWWWWWWWWWWWW
    hyyyyyyyyyyyyyyh
    wwwwwwwwwwwwwwww
    UUUUUUUUUUUUUUUU
    WWhhhhhhhhhhhhWW
    WwwwwwwwwwwwwwUW
    WwUaaAvVvAaaUwUW
    WwUaaAvVvAaaUwUW
    WwwwwwwwwwwwwwUW
    WhhhhhhhhhhhhhUW
    WwwwwwwwwwwwwwUW
    WUUUUUUUUUUUUUUW
"""

# The column: its front (left half, 13 rows show) carries a glowing rune; its back (right half) is plain.
DESK_FRONT = """
    WhhhhhhWWhhhhhhW
    WwwwwwuWWwwwwwuW
    WwUUUUuWWwUUUUuW
    WwUaVaUWWwUwwUuW
    WwUvAvUWWwUwwUuW
    WwUaVaUWWwUwwUuW
    WwUAvAUWWwUwwUuW
    WwUaVaUWWwUwwUuW
    WwUvAvUWWwUwwUuW
    WwUaVaUWWwUwwUuW
    WwUUUUuWWwUUUUuW
    WwwwwwuWWwwwwwuW
    WhhhhhhWWhhhhhhW
    WwwwwwuWWwwwwwuW
    WwwwwwuWWwwwwwuW
    WUUUUUUWWUUUUUUW
"""

# The base: a deepslate plinth with an amethyst ring set into it, and its edges (rows 6-8, 14-16).
DESK_BASE = """
    SSSSSSSSSSSSSSSS
    StttttttttttttqS
    SttttaaaaaatttqS
    StttaAvvvvAatqqS
    SttaAvqqqqvAaqqS
    SttavqTTTTqvaqqS
    StsavqTttTqvaTqS
    SttavqTttTqvaTqS
    SttavqTTTTqvaqqS
    SttaAvqqqqvAaqqS
    StttaAvvvvAatqqS
    SttttaaaaaatqqqS
    StttttttttttqqqS
    SqqqqqqqqqqqqqqS
    TTTTTTTTTTTTTTTT
    QQQQQQQQQQQQQQQQ
"""

DESK_BOTTOM = """
    UUUUUUUUUUUUUUUU
    UwwwwwwwwwwwwwwU
    UwWWWWWWWWWWWWwU
    UwWuuuuuuuuuuWwU
    UwWuWWWWWWWWuWwU
    UwWuWuuuuuuWuWwU
    UwWuWuWWWWuWuWwU
    UwWuWuWWWWuWuWwU
    UwWuWuuuuuuWuWwU
    UwWuWWWWWWWWuWwU
    UwWuuuuuuuuuuWwU
    UwWWWWWWWWWWWWwU
    UwwwwwwwwwwwwwwU
    UUUUUUUUUUUUUUUU
    UUUUUUUUUUUUUUUU
    UUUUUUUUUUUUUUUU
"""


def _grain(cv: Canvas, seed: int):
    """Break up flat runs of dark oak with a sparse, darker fleck."""
    oak = {DO[2], DO[3], DO[4], DO[5]}
    for y in range(16):
        for x in range(16):
            c = cv.get(x, y)
            if c in oak and noise(x, y, seed) > 0.87:
                cv.put(x, y, mix(c, (0, 0, 0), 0.2))


def scribing_desk_textures() -> dict:
    out = {}
    for key, text, seed in (("top", DESK_TOP, 31), ("sides", DESK_SIDES, 32), ("front", DESK_FRONT, 33),
                            ("base", DESK_BASE, 34), ("bottom", DESK_BOTTOM, 35)):
        cv = sprite(text, DESK_PAL)
        _grain(cv, seed)
        out[key] = cv.image()
    return out


# ============================================================== the Runesmith's outfit

ROBE = [hexc("#1E1034"), hexc("#2C1850"), hexc("#3B2268"), hexc("#4C2E84"), hexc("#6040A0")]
TRIM = [hexc("#8A6420"), hexc("#C8962E"), hexc("#F2CE6A")]
RUNE = [GLOW[3], GLOW[5], GLOW[6]]

# A villager's head overlay (the hood), and its robe: 64x64, vanilla's villager layout.
HOOD = box(32, 0, 8, 10, 8)
ROBE_BOX = box(0, 38, 8, 20, 6)
ARMS = box(44, 22, 4, 8, 4)
ARMS_MID = box(40, 38, 8, 4, 4)


def _cloth(sheet: Sheet, area, seed: int, lo=1, hi=3):
    """Robe cloth: the middle tone in soft vertical folds (a shadow line, a lit line), a rare fleck."""
    x0, y0, w, h = area
    mid = (lo + hi) // 2
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            fold = (x - x0) % 4
            tone = lo if fold == 0 else hi if fold == 2 and (y - y0) % 7 != 3 else mid
            if noise(x, y, seed) > 0.94:
                tone = max(lo, tone - 1)
            sheet.put(x, y, ROBE[tone])


def _glyphs(sheet: Sheet, area, seed: int, every: int = 5):
    """Small glowing rune marks stitched into the cloth."""
    x0, y0, w, h = area
    for y in range(y0 + 1, y0 + h - 1, every):
        for x in range(x0 + 1, x0 + w - 1):
            if noise(x, y, seed) > 0.8:
                sheet.put(x, y, RUNE[1])
                sheet.put(x, y + 1, RUNE[0])


def runesmith_outfit(zombie: bool = False) -> Image.Image:
    sheet = Sheet(64, 64)
    # The hood: closed over the top, sides and back; the front only frames the brow.
    for face in ("top", "right", "left", "back"):
        _cloth(sheet, HOOD[face], 41 if face != "back" else 42, 0, 2)
    fx, fy, fw, fh = HOOD["front"]
    for x in range(fx, fx + fw):
        sheet.put(x, fy, ROBE[2])
        sheet.put(x, fy + 1, TRIM[1] if x % 2 == 0 else TRIM[2])
    for y in range(fy, fy + fh):
        sheet.put(fx, y, ROBE[1])
        sheet.put(fx + fw - 1, y, ROBE[1])
    # A single rune sewn on the hood's crown.
    tx, ty, _, _ = HOOD["top"]
    for dx, dy in ((3, 3), (4, 3), (3, 4), (4, 4), (2, 3), (5, 4)):
        sheet.put(tx + dx, ty + dy, RUNE[2] if (dx, dy) in ((3, 3), (4, 4)) else RUNE[1])

    # The robe: violet, a gold band down the front and round the hem, runes on the cloth.
    for face in ("top", "bottom", "right", "front", "left", "back"):
        _cloth(sheet, ROBE_BOX[face], 50 + len(face), 1, 3)
    for face in ("right", "front", "left", "back"):
        x0, y0, w, h = ROBE_BOX[face]
        _glyphs(sheet, ROBE_BOX[face], 60 + len(face))
        for x in range(x0, x0 + w):
            sheet.put(x, y0 + h - 1, TRIM[0])
            sheet.put(x, y0 + h - 2, TRIM[1])
            # A belt, with a rune pouch hanging from it at the front.
            sheet.put(x, y0 + 6, TRIM[0])
    x0, y0, w, h = ROBE_BOX["front"]
    for y in range(y0, y0 + h - 2):
        sheet.put(x0 + 3, y, TRIM[1])
        sheet.put(x0 + 4, y, TRIM[2] if y % 3 else TRIM[1])
    for dx, dy, c in ((1, 7, ROBE[0]), (2, 7, ROBE[0]), (1, 8, AM[3]), (2, 8, RUNE[2]), (1, 9, ROBE[0]), (2, 9, ROBE[0])):
        sheet.put(x0 + dx, y0 + dy, c)
    # Sleeves: violet with gold cuffs, over the folded arms.
    for area in (ARMS, ARMS_MID):
        for face in ("top", "bottom", "right", "front", "left", "back"):
            _cloth(sheet, area[face], 70 + len(face), 1, 3)
    for face in ("right", "front", "left", "back"):
        x0, y0, w, h = ARMS[face]
        for x in range(x0, x0 + w):
            sheet.put(x, y0 + h - 2, TRIM[1])
    img = sheet.image()
    if zombie:
        # Faded and grimy: the same robe after a long while in the dark.
        px = img.load()
        for y in range(img.height):
            for x in range(img.width):
                r, g, b, a = px[x, y]
                if a:
                    grey = (r + g + b) // 3
                    px[x, y] = (round(r * 0.6 + grey * 0.25), round(g * 0.65 + grey * 0.25 + 6), round(b * 0.6 + grey * 0.2), a)
    return img


# ============================================================== trades

def trade(wants, gives, max_uses, xp, also=None, modifier=None):
    data = {"wants": wants, "gives": gives, "max_uses": max_uses, "xp": xp, "reputation_discount": 0.05}
    if also:
        data["additional_wants"] = also
    if modifier:
        data["given_item_modifier"] = modifier
    return data


def item(item_id, n=1):
    return {"id": item_id, "count": n} if n != 1 else {"id": item_id}


def emeralds(n):
    return item("minecraft:emerald", n)


def rune_trade(wants, lo, hi, max_uses, xp, also=None):
    """A rune of Tier lo..hi, picked at random as the villager stocks it."""
    return trade(wants, {"id": "wildercord:rune"}, max_uses, xp, also=also,
                 modifier={"type": "wildercord:random_rune", "min_tier": lo, "max_tier": hi})


# level -> {name: trade}. Each level offers two of its trades, picked when the villager reaches it,
# and every rune on the shelf is rolled then: two Runesmiths seldom stock the same runes.
RUNESMITH_TRADES = {
    1: {
        "emerald_blank_runes": trade(emeralds(1), item("wildercord:blank_rune", 4), 12, 2),
        "lapis_emerald": trade(item("minecraft:lapis_lazuli", 12), emeralds(1), 16, 2),
        "paper_emerald": trade(item("minecraft:paper", 24), emeralds(1), 16, 2),
        "emerald_tier_one_rune": rune_trade(emeralds(4), 1, 1, 4, 3),
    },
    2: {
        "emerald_tier_one_rune": rune_trade(emeralds(5), 1, 1, 4, 5),
        "emerald_tier_two_rune": rune_trade(emeralds(9), 2, 2, 3, 10),
        "amethyst_shard_emerald": trade(item("minecraft:amethyst_shard", 8), emeralds(1), 12, 10),
        "emerald_paper": trade(emeralds(1), item("minecraft:paper", 12), 12, 5),
    },
    3: {
        "emerald_tier_two_rune": rune_trade(emeralds(10), 2, 2, 3, 15),
        "emerald_and_book_torn_page": trade(emeralds(14), item("wildercord:torn_page"), 2, 15, also=item("minecraft:book")),
        "emerald_ink_sac": trade(emeralds(1), item("minecraft:ink_sac", 4), 12, 5),
    },
    4: {
        "emerald_and_amethyst_mana_crystal": trade(emeralds(22), item("wildercord:mana_crystal"), 3, 20, also=item("minecraft:amethyst_shard", 4)),
        "emerald_rune": rune_trade(emeralds(12), 1, 2, 3, 20),
        "emerald_torn_page": trade(emeralds(16), item("wildercord:torn_page"), 2, 20),
    },
    5: {
        "emerald_and_diamond_tier_three_rune": rune_trade(emeralds(32), 3, 3, 2, 30, also=item("minecraft:diamond")),
        "emerald_mana_crystal": trade(emeralds(20), item("wildercord:mana_crystal"), 3, 30),
    },
}


def write_trades():
    for level, trades in RUNESMITH_TRADES.items():
        ids = []
        for name, data in trades.items():
            write_json(DATA / f"villager_trade/runesmith/{level}/{name}.json", data)
            ids.append(f"wildercord:runesmith/{level}/{name}")
        write_json(DATA / f"tags/villager_trade/runesmith/level_{level}.json", {"values": ids})
        write_json(DATA / f"trade_set/runesmith/level_{level}.json", {"amount": 2, "trades": f"#wildercord:runesmith/level_{level}"})
    # Now and then a wandering trader has a rune for sale (one of its uncommon wares).
    write_json(DATA / "villager_trade/wandering_trader/emerald_rune.json", rune_trade(emeralds(8), 1, 2, 1, 1))
    write_json(RES / "data/minecraft/tags/villager_trade/wandering_trader/uncommon.json",
               {"replace": False, "values": ["wildercord:wandering_trader/emerald_rune"]})


# ============================================================== everything

def main():
    tex = ASSETS / "textures"
    for key, image in scribing_desk_textures().items():
        save(image, tex / f"block/scribing_desk_{key}.png")
    write_json(ASSETS / "models/block/scribing_desk.json", {"parent": "minecraft:block/lectern", "textures": {
        "particle": "wildercord:block/scribing_desk_sides", "bottom": "wildercord:block/scribing_desk_bottom",
        "base": "wildercord:block/scribing_desk_base", "front": "wildercord:block/scribing_desk_front",
        "sides": "wildercord:block/scribing_desk_sides", "top": "wildercord:block/scribing_desk_top"}})
    write_json(ASSETS / "blockstates/scribing_desk.json", {"variants": {
        "facing=north": {"model": "wildercord:block/scribing_desk"},
        "facing=east": {"model": "wildercord:block/scribing_desk", "y": 90},
        "facing=south": {"model": "wildercord:block/scribing_desk", "y": 180},
        "facing=west": {"model": "wildercord:block/scribing_desk", "y": 270}}})
    write_json(ASSETS / "items/scribing_desk.json", {"model": {"type": "minecraft:model", "model": "wildercord:block/scribing_desk"}})

    # The villager's outfit (a hood covers the whole head, so no type's hat shows through).
    for kind, zombie in (("villager", False), ("zombie_villager", True)):
        path = tex / f"entity/{kind}/profession/runesmith.png"
        save(runesmith_outfit(zombie), path)
        path.with_name("runesmith.png.mcmeta").write_text('{\n  "villager": {\n    "hat": "full"\n  }\n}\n', encoding="utf-8", newline="\n")

    write_json(DATA / "loot_table/blocks/scribing_desk.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "wildercord:scribing_desk"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    write_json(DATA / "recipe/scribing_desk.json", {
        "type": "minecraft:crafting_shaped", "category": "misc",
        "key": {"S": "#minecraft:wooden_slabs", "B": "minecraft:bookshelf", "R": "wildercord:blank_rune"},
        "pattern": ["SSS", "RBR", " S "], "result": {"id": "wildercord:scribing_desk"}})
    unlock_advancement("wildercord:scribing_desk", "wildercord:blank_rune")
    write_json(RES / "data/minecraft/tags/block/mineable/axe.json", {"replace": False, "values": ["wildercord:scribing_desk"]})
    # Unemployed villagers look for any job site in this tag.
    write_json(RES / "data/minecraft/tags/point_of_interest_type/acquirable_job_site.json", {"replace": False, "values": ["wildercord:runesmith"]})
    write_trades()


if __name__ == "__main__":
    main()
