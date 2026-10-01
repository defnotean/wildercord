"""Generates Wildercord's item art, item models, English text and recipes.

Reads the rune roster straight from Runes.java so the two can never drift apart.
Run from the project root:  python tools/generate_assets.py
"""
import hashlib
import json
import math
import re
import sys
from pathlib import Path

from PIL import Image

try:  # Hand-drawn icons (tools/item_art.py); the procedural stones below are only a fallback.
    import item_art
except ImportError:
    item_art = None

import sigil_art  # Magic circles and the GUI sprites that came with them.
import world_art  # Scrolls, pages, the dummy, the Wellstone, Rune Seals, the lectern and the two skins.
import dungeon_assets  # The dimension dungeons: their bosses' skins, trophies, altar, loot and worldgen.
import affinity_data  # Creature affinities (entity type tags) and elemental climate: their tags and text.

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
ASSETS = RES / "assets/wildercord"
DATA = RES / "data/wildercord"

FAMILY_COLOR = {"shape": (0x40, 0xC8, 0xBE), "modifier": (0xF0, 0xC4, 0x40), "link": (0xA0, 0x64, 0xF0)}
ELEMENT_COLOR = {
    "fire": (0xF0, 0x6E, 0x32), "frost": (0x8C, 0xDC, 0xFF), "storm": (0xFF, 0xE6, 0x50), "wind": (0xC8, 0xF0, 0xDC),
    "earth": (0xB4, 0x8C, 0x5A), "life": (0x6E, 0xDC, 0x64), "void": (0xB4, 0x5A, 0xF0), "arcane": (0xE6, 0x78, 0xDC),
    "time": (0xF2, 0xD9, 0x8A), "blood": (0xD2, 0x28, 0x3C),
}

# ---------------------------------------------------------------- roster

# Innate runes: one wakes in each caster at the 1st Circle. Never crafted, never found.
INNATE = {"blood_thread", "kindling", "twin_star", "borrowed_time", "gale_mantle", "stoneform", "mirrorfrost", "fortune", "phantom", "stormheart"}


def read_fusions():
    """Fused runes, from Fusions.java: path -> (element, element). Never crafted or found, only fused at the altar."""
    src = (ROOT / "src/main/java/dev/wildercord/spell/Fusions.java").read_text(encoding="utf-8")
    consts = dict(re.findall(r'public static final RuneDef (\w+) = \w+\("(\w+)"', (ROOT / "src/main/java/dev/wildercord/spell/Runes.java").read_text(encoding="utf-8")))
    return {consts[c]: (a, b) for a, b, c in re.findall(r'new Recipe\("(\w+)", "(\w+)", Runes\.(\w+)\)', src)}


def read_signatures():
    """Signature fusions, from Fusions.java: path -> (rune path, rune path), the two particular effects that make it
    (in place of their elements' fusion). Never crafted or found either."""
    src = (ROOT / "src/main/java/dev/wildercord/spell/Fusions.java").read_text(encoding="utf-8")
    consts = dict(re.findall(r'public static final RuneDef (\w+) = \w+\("(\w+)"', (ROOT / "src/main/java/dev/wildercord/spell/Runes.java").read_text(encoding="utf-8")))
    return {consts[c]: (consts[a], consts[b]) for a, b, c in re.findall(r'new Signature\(Runes\.(\w+), Runes\.(\w+), Runes\.(\w+)\)', src)}


def _signature_elements(signatures):
    """A signature rune's two elements: its two runes' (for the circles, and for everything any fused rune is kept out of)."""
    src = (ROOT / "src/main/java/dev/wildercord/spell/Runes.java").read_text(encoding="utf-8")
    element = dict(re.findall(r'= effect\("(\w+)", "[^"]+", \d, [\d.]+, "(\w+)"', src))
    return {path: (element[a], element[b]) for path, (a, b) in signatures.items()}


# The element fusions (every pair of the ten elements), and the signature fusions (two particular runes each).
ELEMENT_FUSIONS = read_fusions()
SIGNATURES = read_signatures()
# Every rune made at the Fusion Altar, with its two elements: never crafted, found, sold or rolled.
FUSED = {**ELEMENT_FUSIONS, **_signature_elements(SIGNATURES)}
def rune_sources():
    """Parses RuneSources.java: [(source id, where it is, [rune paths])], in the order they're listed."""
    src = (ROOT / "src/main/java/dev/wildercord/spell/RuneSources.java").read_text(encoding="utf-8")
    consts = rune_constants()
    out = []
    for sid, where, body in re.findall(r'= source\("([\w:]+)", "([^"]+)", ([^\n]*)\);', src):
        out.append((sid, where, [consts[c] for c in re.findall(r"Runes\.(\w+)", body)]))
    return out


def found_only():
    """The runes of the world: found in particular places, never crafted."""
    return {path for _, _, paths in rune_sources() for path in paths}


def read_runes():
    src = (ROOT / "src/main/java/dev/wildercord/spell/Runes.java").read_text(encoding="utf-8")
    runes = []
    for m in re.finditer(r'= (shape|effect|modifier|link)\("(\w+)", "([^"]+)", (\d), ([^\n]*)\);', src):
        family, path, name, tier, rest = m.groups()
        element = ""
        if family == "effect":
            element = re.match(r'[\d.]+, "(\w+)"', rest).group(1)
        desc = re.findall(r'"((?:[^"\\]|\\.)*)"', rest)
        runes.append({"family": family, "path": path, "name": name, "tier": int(tier), "element": element, "desc": desc[-1] if desc else ""})
    assert len(runes) >= 40, f"parsed only {len(runes)} runes"
    return runes


# ---------------------------------------------------------------- pixel helpers


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def darken(c, t):
    return mix(c, (0, 0, 0), t)


def lighten(c, t):
    return mix(c, (255, 255, 255), t)


def mask_for(family):
    def inside(x, y):
        cx, cy = x + 0.5 - 8, y + 0.5 - 8
        if family == "shape":
            return cx * cx + cy * cy <= 6.7 ** 2
        if family == "effect":
            return abs(cx) + abs(cy) <= 8.6 and cx * cx + cy * cy <= 7.4 ** 2
        if family == "modifier":
            if not (-6 <= cx <= 6 and -6 <= cy <= 6):
                return False
            ex, ey = max(abs(cx) - 4, 0), max(abs(cy) - 4, 0)
            return ex * ex + ey * ey <= 2.2 ** 2
        # link: a hexagonal tablet
        return abs(cy) <= 6.6 and abs(cx) + 0.55 * abs(cy) <= 7.4
    return [[inside(x, y) for x in range(16)] for y in range(16)]


def glyph_for(path):
    """A mirrored 5x7 glyph, unique per rune, chosen to look like a rune rather than noise."""
    for salt in range(200):
        h = hashlib.sha256(f"{path}:{salt}".encode()).digest()
        bits = [(h[i // 8] >> (i % 8)) & 1 for i in range(21)]
        grid = [[0] * 5 for _ in range(7)]
        for row in range(7):
            for col in range(3):
                v = bits[row * 3 + col]
                grid[row][col] = v
                grid[row][4 - col] = v
        filled = sum(map(sum, grid))
        middle = sum(grid[r][2] for r in range(7))
        rows_used = sum(1 for r in grid if any(r))
        if 13 <= filled <= 21 and middle >= 3 and rows_used >= 6:
            return grid
    return [[1 if c == 2 else 0 for c in range(5)] for _ in range(7)]


QUESTION = ["01110", "10001", "00010", "00100", "00100", "00000", "00100"]


def stone(family, accent, glyph, body=(0x2E, 0x29, 0x40)):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    mask = mask_for(family)

    def m(x, y):
        return 0 <= x < 16 and 0 <= y < 16 and mask[y][x]

    for y in range(16):
        for x in range(16):
            if not m(x, y):
                continue
            edge = not (m(x - 1, y) and m(x + 1, y) and m(x, y - 1) and m(x, y + 1))
            if edge:
                c = darken(accent, 0.55)
            elif not m(x - 1, y - 1) or not m(x, y - 2):
                c = lighten(body, 0.16)
            elif not m(x + 1, y + 1) or not m(x, y + 2):
                c = darken(body, 0.3)
            else:
                c = body
            px[x, y] = c + (255,)
    if glyph:
        gx, gy = 6 - 1, 4
        cells = [(gx + c, gy + r) for r in range(7) for c in range(5) if glyph[r][c]]
        # Soft glow around the glyph, then the glyph itself with a bright core.
        for x, y in cells:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if m(nx, ny) and (nx, ny) not in cells:
                    r, g, b, a = px[nx, ny]
                    px[nx, ny] = mix((r, g, b), accent, 0.28) + (255,)
        for x, y in cells:
            if m(x, y):
                px[x, y] = accent + (255,)
        for x, y in cells:
            if m(x, y) and (x, y - 1) in cells and (x, y + 1) in cells:
                px[x, y] = lighten(accent, 0.35) + (255,)
    return img


def cord_image(cord, beads, pendant, alpha=255):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()

    def curve(x):
        return 2.5 + 9.0 * (1 - ((x - 7.5) / 5.8) ** 2)

    for sx in range(18 * 4):
        x = 1.6 + sx / 4.0 * (12.0 / 17)
        if not (1.7 <= x <= 13.3):
            continue
        y = curve(x)
        ix, iy = int(x + 0.5), int(y + 0.5)
        if 0 <= ix < 16 and 0 <= iy < 16:
            px[ix, iy] = cord + (alpha,)
            if iy + 1 < 16 and px[ix, iy + 1][3] == 0:
                px[ix, iy + 1] = darken(cord, 0.45) + (alpha,)
    for bx, col in zip((4.2, 10.8), beads):
        ix, iy = int(bx + 0.5), int(curve(bx) + 0.5)
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            if 0 <= ix + dx < 16 and 0 <= iy + dy - 1 < 16:
                px[ix + dx, iy + dy - 1] = (lighten(col, 0.3) if (dx, dy) == (0, 0) else col) + (alpha,)
    # Pendant rune at the bottom.
    cx, cy = 7, 11
    for dy in range(-1, 3):
        for dx in range(-1, 3):
            if abs(dx - 0.5) + abs(dy - 0.5) <= 2.1:
                c = pendant
                if (dx, dy) == (0, 0):
                    c = lighten(pendant, 0.45)
                elif dx == 2 or dy == 2:
                    c = darken(pendant, 0.35)
                px[cx + dx, cy + dy] = c + (alpha,)
    return img


CORDS = {
    "twine": ((0xC8, 0xA8, 0x78), [(0x8A, 0x6A, 0x48), (0x8A, 0x6A, 0x48)], (0x9A, 0x96, 0xA8)),
    "copper": ((0xD8, 0x84, 0x50), [(0x40, 0xC8, 0xBE), (0x40, 0xC8, 0xBE)], (0xF0, 0x6E, 0x32)),
    "amethyst": ((0xA8, 0x78, 0xE8), [(0xF0, 0xC4, 0x40), (0xF0, 0xC4, 0x40)], (0xC8, 0x90, 0xFF)),
    "echo": ((0x1E, 0x6E, 0x78), [(0x60, 0xF0, 0xE8), (0x60, 0xF0, 0xE8)], (0x30, 0xD8, 0xE0)),
}

# ---------------------------------------------------------------- writers


def loot_format(x):
    """
    A loot table written the older way ({@code "functions": [{"function": ...}]},
    {@code "conditions": [{"condition": ...}]}) in the form Minecraft 26.3 reads: one {@code "modifier"}
    (or a list of them) and one {@code "condition"} (several joined by {@code minecraft:all_of}), each
    named by {@code "type"}. 26.3 silently ignores the old keys, so a rune's set_components would never
    run and the rune would come out blank.
    """
    if isinstance(x, list):
        return [loot_format(v) for v in x]
    if not isinstance(x, dict):
        return x
    out = {}
    for key, value in x.items():
        if key == "functions":
            mods = [_typed(f, "function") for f in value]
            if mods:
                out["modifier"] = mods[0] if len(mods) == 1 else mods
        elif key == "conditions":
            conds = [_typed(c, "condition") for c in value]
            if conds:
                out["condition"] = conds[0] if len(conds) == 1 else {"type": "minecraft:all_of", "terms": conds}
        else:
            out[key] = loot_format(value)
    return out


def _typed(entry, old_key):
    entry = dict(entry)
    entry["type"] = entry.pop(old_key)
    return loot_format(entry)


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    if "loot_table" in path.parts:
        data = loot_format(data)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def save(img, path, preview=None):
    """Saves one image, or a list of frames as an animated strip with its .mcmeta."""
    path.parent.mkdir(parents=True, exist_ok=True)
    frames = img if isinstance(img, list) else [img]
    meta = path.with_name(path.name + ".mcmeta")
    if len(frames) == 1:
        frames[0].save(path)
        if meta.exists():
            meta.unlink()
        return
    w, h = frames[0].size
    strip = Image.new("RGBA", (w, h * len(frames)), (0, 0, 0, 0))
    for i, frame in enumerate(frames):
        strip.paste(frame, (0, i * h))
    strip.save(path)
    meta.write_text(json.dumps({"animation": {"frametime": getattr(item_art, "ANIMATION_FRAMETIME", 3)}}, indent=2) + "\n", encoding="utf-8", newline="\n")


def item_model(path, texture):
    write_json(ASSETS / f"models/item/{path}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"wildercord:item/{texture}"}})


def main():
    import cinnamon_art  # Rebuild both companion skins and her hand-painted red toy with the rest of the pack.
    item_model("cinnamon_toy", "cinnamon_toy")
    write_json(ASSETS / "items/cinnamon_toy.json", {"model": {"type": "minecraft:model", "model": "wildercord:item/cinnamon_toy"}})
    write_json(DATA / "recipe/cinnamon_toy.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": ["minecraft:bone", "minecraft:red_dye", "minecraft:slime_ball"], "result": {"id": "wildercord:cinnamon_toy"}})
    unlock_advancement("wildercord:cinnamon_toy", "minecraft:bone")
    runes = read_runes()
    tex = ASSETS / "textures/item"

    # Runes.
    cases = []
    for r in runes:
        accent = FAMILY_COLOR[r["family"]] if r["family"] != "effect" else ELEMENT_COLOR[r["element"]]
        # Innate runes wear the Tier IV treatment (a slow pulse): they're one of a kind.
        art_tier = 4 if r["path"] in INNATE else r["tier"]
        art = item_art.rune_icon(r["path"], r["family"], r["element"], art_tier) if item_art else stone(r["family"], accent, glyph_for(r["path"]))
        save(art, tex / f"rune/{r['path']}.png")
        item_model(f"rune/{r['path']}", f"rune/{r['path']}")
        cases.append({"when": f"wildercord:{r['path']}", "model": {"type": "minecraft:model", "model": f"wildercord:item/rune/{r['path']}"}})
    save(item_art.silent_rune_icon() if item_art else stone("shape", (0x77, 0x72, 0x88), [[int(c) for c in row] for row in QUESTION], body=(0x24, 0x22, 0x2C)), tex / "rune/silent.png")
    item_model("rune/silent", "rune/silent")
    write_json(ASSETS / "items/rune.json", {"model": {
        "type": "minecraft:select", "property": "minecraft:component", "component": "wildercord:rune", "cases": cases,
        "fallback": {"type": "minecraft:model", "model": "wildercord:item/rune/silent"}}})

    save(item_art.blank_rune_icon() if item_art else stone("shape", (0x9A, 0x96, 0xA8), None, body=(0x6B, 0x67, 0x80)), tex / "blank_rune.png")
    item_model("blank_rune", "blank_rune")
    write_json(ASSETS / "items/blank_rune.json", {"model": {"type": "minecraft:model", "model": "wildercord:item/blank_rune"}})

    for key, (cord, beads, pendant) in CORDS.items():
        save(item_art.cord_icon(key) if item_art else cord_image(cord, beads, pendant), tex / f"{key}_cord.png")
        item_model(f"{key}_cord", f"{key}_cord")
        write_json(ASSETS / f"items/{key}_cord.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{key}_cord"}})

    # Mana Crystal, potion-effect icons and the Cord screen's mana badge.
    if item_art and hasattr(item_art, "mana_crystal_icon"):
        save(item_art.mana_crystal_icon(), tex / "mana_crystal.png")
        save(item_art.clarity_effect_icon(), ASSETS / "textures/mob_effect/clarity.png")
        save(item_art.mana_effect_icon(), ASSETS / "textures/mob_effect/mana.png")
        save(item_art.mana_badge_icon(), ASSETS / "textures/gui/sprites/cord/mana_badge.png")
        save(item_art.heart_badge_icon(), ASSETS / "textures/gui/sprites/cord/heart_badge.png")
        save(item_art.warded_effect_icon(), ASSETS / "textures/mob_effect/warded.png")
        save(item_art.ward_badge_icon(), ASSETS / "textures/gui/sprites/cord/ward_badge.png")
    else:
        crystal = stone("effect", (0x9C, 0x7C, 0xFF), None, body=(0x5A, 0x3C, 0xC8))
        save(crystal, tex / "mana_crystal.png")
        for name in ("clarity", "mana", "warded"):
            icon18 = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
            icon18.paste(crystal, (1, 1))
            save(icon18, ASSETS / f"textures/mob_effect/{name}.png")
        save(crystal.resize((12, 12), Image.NEAREST), ASSETS / "textures/gui/sprites/cord/mana_badge.png")
    item_model("mana_crystal", "mana_crystal")
    write_json(ASSETS / "items/mana_crystal.json", {"model": {"type": "minecraft:model", "model": "wildercord:item/mana_crystal"}})

    # Empty Cord slot icon (vanilla draws it in the GUI atlas).
    icon = item_art.cord_slot_icon() if item_art else cord_image((0xFF, 0xFF, 0xFF), [(0xFF, 0xFF, 0xFF)] * 2, (0xFF, 0xFF, 0xFF), alpha=90)
    save(icon, ASSETS / "textures/gui/sprites/container/slot/cord.png")

    write_lang(runes)
    write_recipes(runes)
    write_mana_data()
    sigil_art.main()
    import shield_art  # The Shield's cells, shards and cracks.
    shield_art.main()
    import mote_art  # Soft lights and vapours: motes, butterflies of light, steam and smoke.
    mote_art.main()
    import material_art
    material_art.main()
    import physical_art
    physical_art.main()
    import circle_art  # Every rune's own ring and emblem for magic circles (imported here: it reads the runes from this file).
    circle_art.main()
    import wear_art  # The Cord players wear on the wrist.
    wear_art.main()
    write_new_content(runes)
    write_advancements(runes)
    write_world_events()
    affinity_data.write_tags(write_json, DATA)
    import runesmith_art  # The Runesmith: its desk, its outfit and its trades.
    runesmith_art.main()
    write_familiar_content()
    write_gear_content()
    write_backpack_content()
    dungeon_assets.main(sys.modules[__name__], runes)
    import hearth_art
    hearth_art.write(sys.modules[__name__])
    import relic_art
    relic_art.write(sys.modules[__name__])
    print(f"generated art for {len(runes)} runes, {len(CORDS)} cords")


# ---------------------------------------------------------------- text


# Magic that changes the world (cast/WorldMagic, spell/WorldRules): each interaction's line in a rune's tooltip.
WORLD_MAGIC_LANG = {
    "tooltip.wildercord.world.ignite": "Where it lands: sets grass and leaves alight, lights candles and campfires, melts snow and ice, boils water into blinding steam. It lights TNT too: stand clear",
    "tooltip.wildercord.world.freeze": "Where it lands: freezes water into ice you can walk on, cools lava into a crust that melts back after 25 seconds, puts out fires, campfires and candles",
    "tooltip.wildercord.world.conduct": "Where it lands: in water, shocks every foe in the same water; scrapes a stage of oxidation off copper and powers lightning rods. It may charge a creeper: careful",
    "tooltip.wildercord.world.gust": "Where it lands: knocks arrows and fireballs away, blows out small fires and candles, scatters loose items",
    "tooltip.wildercord.world.heave": "Where it lands: the ground heaves up, throwing foes standing on it",
    "tooltip.wildercord.world.bloom": "Where it lands: grass and flowers bloom, crops grow, and a weakened zombie villager starts to be cured",
    "tooltip.wildercord.world.draw": "Where it lands: draws loose items and experience in; an enderman struck can't teleport for 5 seconds",
    "tooltip.wildercord.world.age": "Where it lands: crops and saplings grow, copper weathers, young animals grow up faster, and a furnace jumps ahead in its smelting",
    "tooltip.wildercord.world.shimmer": "Where it lands: bookshelves and enchanting tables shimmer, and invisible creatures show for 3 seconds",
    "tooltip.wildercord.world.feed": "Where it lands: nether wart and crimson fungus grow",
}


# The reactions that joined the first five (cast/Reactions, spell/ReactionRules): their names as they go
# off, their Grimoire lines, their contracts, and the lines in rune tooltips that say which runes play a part.
REACTIONS_LANG = {
    "reaction.wildercord.overload": "Overload!",
    "reaction.wildercord.fracture": "Fracture!",
    "reaction.wildercord.blight": "Blight!",
    "reaction.wildercord.unweave": "Unweave!",
    "reaction.wildercord.rupture": "Rupture!",
    "reaction.wildercord.elapse": "Elapse!",
    "reaction.wildercord.overload.desc": "Storm on a burning target: +30% damage, and the flames burst: 4 damage to every foe within 3 blocks, thrown back. The fire goes out.",
    "reaction.wildercord.fracture.desc": "Earth on a frozen target: +40% damage, the ice cracks, and for 5 seconds every spell hits it 20% harder.",
    "reaction.wildercord.blight.desc": "Life on a shadowed target (hexed, blinded, withered...): rot bursts out, 3 damage and poison to it and up to 5 foes around it, and you heal 1 for each (once a second at most).",
    "reaction.wildercord.unweave.desc": "Arcane on a target with two marks or more: every mark comes undone, +30% damage for each (up to +120%).",
    "reaction.wildercord.rupture.desc": "Wind on a bleeding target: +50% damage, the wound tears open for 4 more through armour, and you heal 2 (once a second at most).",
    "reaction.wildercord.elapse.desc": "Time on a burning, poisoned or withering target: all the damage still to come lands at once, half again as much (3 to 16).",
    "contract.wildercord.reaction.overload": "Overload",
    "contract.wildercord.reaction.fracture": "Fracture",
    "contract.wildercord.reaction.blight": "Blight",
    "contract.wildercord.reaction.unweave": "Unweave",
    "contract.wildercord.reaction.rupture": "Rupture",
    "contract.wildercord.reaction.elapse": "Elapse",
    "tooltip.wildercord.mark.shadowed": "Leaves foes shadowed: life damage on them sets off Blight",
    "tooltip.wildercord.mark.bleeding": "Leaves foes bleeding: wind damage on them sets off Rupture",
    "tooltip.wildercord.trigger.overload": "On a burning foe it sets off Overload",
    "tooltip.wildercord.trigger.fracture": "On a frozen foe it sets off Fracture",
    "tooltip.wildercord.trigger.blight": "On a shadowed foe (hexed, blinded, withered...) it sets off Blight",
    "tooltip.wildercord.trigger.unweave": "On a foe with two marks or more (burning, frozen, bleeding...) it sets off Unweave",
    "tooltip.wildercord.trigger.rupture": "On a bleeding foe (cut by Bleed, Rend, Cleave...) it sets off Rupture",
    "tooltip.wildercord.trigger.elapse": "On a burning, poisoned or withering foe it sets off Elapse",
}


def write_lang(runes):
    lang = {
        "tooltip.wildercord.armor.emberweave": "Each worn piece softens spells by 3% while you sprint and move (12% with four pieces).",
        "tooltip.wildercord.armor.rimebound": "Begin crouching to guard for 0.6 seconds: 4% spell protection per piece. Recharges in 4 seconds.",
        "tooltip.wildercord.armor.stonebound": "Each piece gives 4% spell protection and 10% less spell knockback, but 3% less movement speed.",
        "tooltip.wildercord.armor.mirror_thread": "Begin crouching to guard for 0.6 seconds. Soften one spell by half and return a fragment (at most 2 damage). Recharges in 5 seconds.",
        "message.wildercord.armor_guard": "Guard ready - catch the next spell!",
        **{f"item.wildercord.{kind}_{slot}": f"{kind.title()} {slot.title()}" for kind in ("emberweave", "rimebound", "stonebound") for slot in ("helmet", "chestplate", "leggings", "boots")},
        "item.wildercord.mirror_thread_mantle": "Mirror-thread Mantle",
        "entity.wildercord.cinnamon": "Cinnamon",
        "item.wildercord.cinnamon_toy": "Cinnamon's Red Bone",
        "message.wildercord.cinnamon.toy": "Cinnamon wiggles with her favourite red bone.",
        "message.wildercord.cinnamon.owner": "Cinnamon is waiting for her owner. Set owner in wildercord-cinnamon.json.",
        "message.wildercord.cinnamon.pet": "Cinnamon leans into your hand.",
        "message.wildercord.cinnamon.sit": "Cinnamon will sit. She keeps this choice when you rejoin or travel.",
        "message.wildercord.cinnamon.follow": "Cinnamon will follow you.",
        "itemGroup.wildercord": "Wildercord",
        "item.wildercord.rune": "Rune",
        "item.wildercord.rune.named": "%s Rune",
        "item.wildercord.rune.silent": "Silent Rune",
        "item.wildercord.blank_rune": "Blank Rune",
        "item.wildercord.twine_cord": "Twine Cord",
        "item.wildercord.copper_cord": "Copper Cord",
        "item.wildercord.amethyst_cord": "Amethyst Cord",
        "item.wildercord.echo_cord": "Echo Cord",
        "entity.wildercord.rune_bolt": "Rune Bolt",
        "family.wildercord.shape": "Shape",
        "family.wildercord.effect": "Effect",
        "family.wildercord.modifier": "Modifier",
        "family.wildercord.link": "Link",
        "tooltip.wildercord.tier": "Tier %s",
        "tooltip.wildercord.learn": "Right-click to learn it forever",
        "tooltip.wildercord.silent": "This game doesn't know this rune (%s): it's from a different Wildercord version or an add-on that isn't installed. It stays safe and wakes up once you have it.",
        "tooltip.wildercord.cord.stats": "%s sockets per spell · %s spell(s) · %s mana",
        "tooltip.wildercord.cord.wear": "Wear it in the Cord slot above your offhand",
        "message.wildercord.already_known": "You already know %s. A Runesmith will buy it back, or reroll two of a tier",
        "message.wildercord.learned": "Learned %s! Press K to thread it into a spell",
        "message.wildercord.no_cord": "Wear a Cord first: the slot above your offhand (E)",
        "message.wildercord.spell_empty": "Spell %s is empty. Press K to thread runes",
        "message.wildercord.cooldown": "Recharging... %ss",
        "message.wildercord.no_mana": "Not enough mana (%s/%s)",
        "message.wildercord.selected": "Spell %s",
        "message.wildercord.first_cord": "Your Cord hums. You learned Self, Bolt and Push, and spell 1 is ready: R casts, V switches spell, K opens your Cord.",
        "message.wildercord.blink_far": "Too far to blink (max 32 blocks)",
        "command.wildercord.learnall": "Learned all %s runes",
        "command.wildercord.learned": "Learned %s",
        "command.wildercord.unknown": "Unknown rune: %s",
        "command.wildercord.spell_set": "Spell %s set (%s runes)",
        "command.wildercord.reset": "Forgot every rune and spell",
        "key.category.wildercord.wildercord": "Wildercord",
        "key.wildercord.cast": "Cast spell (hold to charge)",
        "key.wildercord.next_spell": "Next spell (hold for the wheel)",
        "key.wildercord.open_cord": "Open Cord",
        "screen.wildercord.cord": "Cord",
        "screen.wildercord.stats": "%s sockets · %s · Tier %s",
        "screen.wildercord.spells.one": "%s spell",
        "screen.wildercord.spells.many": "%s spells",
        "screen.wildercord.too_costly": "Costs more than this Cord's %s mana, so it can't be cast.",
        "screen.wildercord.help.title": "Threading spells",
        "screen.wildercord.help.1": "Click a rune below to add it to the selected spell",
        "screen.wildercord.help.2": "Drag a rune onto a socket to insert it there",
        "screen.wildercord.help.3": "Drag threaded runes to reorder them, or off the Cord to remove",
        "screen.wildercord.help.4": "Click a threaded rune to take it out",
        "screen.wildercord.help.5": "Gold marks show what each modifier changes",
        "screen.wildercord.help.6": "Scroll over the Codex or the readout to see more",
        "screen.wildercord.help.keys": "%s casts (hold to charge) · %s switches spell (hold for the wheel) · %s opens this screen",
        "screen.wildercord.row_locked": "Needs a %s",
        "screen.wildercord.row_kept": "%s runes kept",
        "screen.wildercord.quiet_silent": "This game doesn't know this rune (a different Wildercord version or a missing add-on), so it stays quiet",
        "screen.wildercord.quiet_socket": "Past the last socket of your %s (%s): kept, but quiet",
        "screen.wildercord.quiet_tier": "Too strong for this Cord: needs a %s. Kept, but quiet",
        "screen.wildercord.quiet_unlearned": "You haven't learned this rune",
        "screen.wildercord.needs_cord": "Needs a %s (Tier %s)",
        "screen.wildercord.socket_hint": "Click to remove · drag to move",
        "screen.wildercord.more_sockets": "more sockets on bigger Cords",
        "message.wildercord.spell_needs": "Spell %s needs a %s",
        "message.wildercord.sockets_full": "Your %s holds %s runes per spell",
        "message.wildercord.not_learned": "You haven't learned %s",
        "message.wildercord.too_strong": "%s is too strong for this Cord: it needs a %s",
        "tooltip.wildercord.cord.tier": "Holds runes up to Tier %s",
        "tooltip.wildercord.needs_cord": "Needs a %s or better",
        "screen.wildercord.no_cord": "You aren't wearing a Cord",
        "screen.wildercord.no_cord_hint": "Put one in the slot above your offhand (E)",
        "screen.wildercord.locked_socket": "Needs a bigger Cord",
        "screen.wildercord.locked_spell": "Spell %s needs a bigger Cord",
        "screen.wildercord.tab.all": "All",
        "screen.wildercord.tab.shape": "Shapes",
        "screen.wildercord.tab.effect": "Effects",
        "screen.wildercord.tab.modifier": "Modifiers",
        "screen.wildercord.tab.link": "Links",
        "screen.wildercord.known": "%s/%s known",
        "screen.wildercord.codex_hint": "Click to add · drag onto a socket",
        "screen.wildercord.codex_empty": "No runes here yet",
        "screen.wildercord.empty_spell": "Empty spell. Click runes below to thread them.",
        "screen.wildercord.cost": "%s mana · %ss cooldown · Cord holds %s",
        "screen.wildercord.cost_health": "%s health (Blood Price) · %ss cooldown",
        "screen.wildercord.too_costly_health": "Costs more health than you have (%s): trim the spell or drop Blood Price",
        "screen.wildercord.modifier_hint": "Changes the closest rune on its left that it can affect",
        "item.wildercord.mana_crystal": "Mana Crystal",
        "tooltip.wildercord.mana_crystal": "+%s max mana, forever (up to %s crystals)",
        "tooltip.wildercord.mana_crystal.use": "Right-click to absorb it",
        "message.wildercord.crystal_used": "Max mana +%s (%s/%s crystals)",
        "message.wildercord.crystals_full": "Your mana can't grow further with crystals (%s/%s)",
        "effect.wildercord.clarity": "Clarity",
        "effect.wildercord.mana": "Mana",
        "item.minecraft.potion.effect.wildercord_clarity": "Potion of Clarity",
        "item.minecraft.splash_potion.effect.wildercord_clarity": "Splash Potion of Clarity",
        "item.minecraft.lingering_potion.effect.wildercord_clarity": "Lingering Potion of Clarity",
        "item.minecraft.tipped_arrow.effect.wildercord_clarity": "Arrow of Clarity",
        "item.minecraft.potion.effect.wildercord_mana": "Potion of Mana",
        "item.minecraft.splash_potion.effect.wildercord_mana": "Splash Potion of Mana",
        "item.minecraft.lingering_potion.effect.wildercord_mana": "Lingering Potion of Mana",
        "item.minecraft.tipped_arrow.effect.wildercord_mana": "Arrow of Mana",
        "effect.wildercord.warded": "Warded",
        "item.minecraft.potion.effect.wildercord_warded": "Potion of Warding",
        "item.minecraft.splash_potion.effect.wildercord_warded": "Splash Potion of Warding",
        "item.minecraft.lingering_potion.effect.wildercord_warded": "Lingering Potion of Warding",
        "item.minecraft.tipped_arrow.effect.wildercord_warded": "Arrow of Warding",
        "enchantment.wildercord.warding": "Warding",
        "enchantment.wildercord.warding.desc": "Spells hurt 8% less per level, up to 80% with Protection's share. Can't share a piece with Protection",
        "enchantment.wildercord.reservoir": "Reservoir",
        "enchantment.wildercord.wellspring": "Wellspring",
        "enchantment.wildercord.siphon": "Siphon",
        "enchantment.wildercord.potency": "Potency",
        "enchantment.wildercord.celerity": "Celerity",
        "enchantment.wildercord.thrift": "Thrift",
        "enchantment.wildercord.persistence": "Persistence",
        "enchantment.wildercord.potency.desc": "Spells hit 8% harder per level",
        "enchantment.wildercord.celerity.desc": "Spell cooldowns 8% shorter per level",
        "enchantment.wildercord.thrift.desc": "Spells and passives cost 7% less mana per level",
        "enchantment.wildercord.persistence.desc": "Spell effects last 20% longer per level",
        "tooltip.wildercord.not_craftable": "Tier IV: can't be crafted, only found",
        "screen.wildercord.page.spells": "Spells",
        "screen.wildercord.page.passives": "Passives",
        "screen.wildercord.not_sustainable": "Can't be a passive: only lasting buffs, wards and Orbit auras",
        "screen.wildercord.passive_locked": "Opens with the %s Heart Circle",
        "screen.wildercord.passive.on": "On",
        "screen.wildercord.passive.off": "Off",
        "screen.wildercord.passive.switch": "Click to switch this passive on or off",
        "screen.wildercord.passive.upkeep": "%s/s",
        "screen.wildercord.passive.upkeep_hint": "Mana per second to keep it running. Passives have no cooldown.",
        "screen.wildercord.quiet_passive_socket": "Passives hold %s runes",
        "screen.wildercord.passive.summary": "Passives drain %s mana/s · you regenerate %s/s",
        "screen.wildercord.passive.locked_hint": "Form Heart Circles to open passive slots: the 1st and 5th Circle each open one. Hover the heart above to see your progress.",
        "screen.wildercord.passive.empty": "Empty passive. Thread runes that can be sustained.",
        "screen.wildercord.passive.rules": "Self or Orbit, then lasting buffs (Swift, Stoneskin, Infinity, Reflect...). Damage like Shock or Dismantle needs an Orbit to carry it. No links and no cooldown: it costs mana every second instead.",
        "screen.wildercord.passive.header": "Passive · %s mana/s · renews every %ss · no cooldown",
        "screen.wildercord.passive.header_off": "Passive (off) · %s mana/s when on · renews every %ss",
        "screen.wildercord.mana.max_circles": "+%s from %s Heart Circles",
        "screen.wildercord.mana.regen_circles": "+%s/s from Heart Circles",
        "screen.wildercord.mana.upkeep": "-%s/s to passives",
        "screen.wildercord.mana.way.circles": "Form Heart Circles by casting and meditating (hover the heart)",
        "screen.wildercord.heart.none": "Heart: no circles yet",
        "screen.wildercord.heart.title": "Heart: %s Circle",
        "screen.wildercord.heart.bonus": "+%s max mana · +%s mana/s · +%s%% spell power",
        "screen.wildercord.heart.passives": "Passive slots: %s of %s",
        "screen.wildercord.heart.perk.3": "%s Circle · Mana Skin: a fifth of damage taken is paid with mana",
        "screen.wildercord.heart.perk.5": "%s Circle · Flow: cooldowns 15%% shorter",
        "screen.wildercord.heart.perk.7": "%s Circle · Overflow: spells cast at full mana hit 30%% harder",
        "screen.wildercord.heart.perk.8": "%s Circle · Archmage: spells cost 15%% less mana",
        "screen.wildercord.heart.complete": "Your heart is complete: an Archmage's eight circles.",
        "screen.wildercord.heart.next": "Next: the %s Circle",
        "screen.wildercord.heart.condense": "Mana condensed from casting: %s / %s",
        "screen.wildercord.heart.ready": "Ready! Meditate (sneak and stand still) for 10 seconds without getting hurt to form it.",
        "screen.wildercord.heart.need.runes": "Know %s runes (%s)",
        "screen.wildercord.heart.need.cord": "Wear a %s or better",
        "screen.wildercord.heart.need.kills": "Defeat %s monsters with spells (%s)",
        "screen.wildercord.heart.need.boss": "Help slay a boss (Wither, Warden, Elder Guardian, Ender Dragon, the Archivist, the Cinder Warden, the Star-Eater or the Tide Scribe)",
        "message.wildercord.circle_broken": "Your concentration broke: the circle unravels",
        "screen.wildercord.heart.how": "Mana spent casting spells condenses in your heart. Once it's ready, meditate to form the circle.",
        "message.wildercord.circle_ready": "Your heart is ready to form the %s Circle. Meditate (sneak and stand still) for 10 seconds without getting hurt to form it.",
        "message.wildercord.circle_formed": "%s Circle formed: +%s max mana, +%s mana/s and +%s%% spell power.",
        "message.wildercord.passive_slot": "Passive slots open: %s. Thread them on the Cord screen's Passives page.",
        "message.wildercord.perk.3": "Mana Skin: a fifth of the damage you take is now paid with mana.",
        "message.wildercord.perk.5": "Flow: your cooldowns are 15%% shorter.",
        "message.wildercord.perk.7": "Overflow: spells cast at full mana hit 30%% harder.",
        "message.wildercord.perk.8": "Archmage: your spells cost 15%% less mana.",
        "message.wildercord.boss_breakthrough": "A boss has fallen. Your heart can now break through to the 7th Circle.",
        "message.wildercord.passive_locked": "That passive slot opens with the %s Heart Circle",
        "message.wildercord.passive_full": "Passives hold %s runes",
        "message.wildercord.not_sustainable": "%s can't be a passive",
        "message.wildercord.passive_on": "Passive %s on",
        "message.wildercord.passive_off": "Passive %s off",
        "message.wildercord.time_held": "Time stopped · %s damage held (%s hits)",
        "title.wildercord.circle": "%s Circle",
        "title.wildercord.circle.1": "A ring of mana forms around your heart",
        "title.wildercord.circle.2": "Your mana runs deeper",
        "title.wildercord.circle.3": "Mana Skin awakens",
        "title.wildercord.circle.4": "Your circles turn as one",
        "title.wildercord.circle.5": "Flow: the circles quicken",
        "title.wildercord.circle.6": "Your heart burns brighter",
        "title.wildercord.circle.7": "Overflow: mana spills from you",
        "title.wildercord.circle.8": "Archmage",
        "command.wildercord.circles": "Heart Circles set to %s",
        "command.wildercord.condensed": "Condensed %s mana",
        "screen.wildercord.mana.title": "Mana",
        "screen.wildercord.mana.max": "Max mana: %s",
        "screen.wildercord.mana.max_cord": "  %s from your %s",
        "screen.wildercord.mana.max_crystals": "  +%s from %s Mana Crystal(s)",
        "screen.wildercord.mana.max_reservoir": "  +%s from Reservoir %s",
        "screen.wildercord.mana.regen": "Regeneration: %s per second",
        "screen.wildercord.mana.regen_cord": "  %s/s from your Cord",
        "screen.wildercord.mana.regen_wellspring": "  +%s%% from Wellspring %s",
        "screen.wildercord.mana.regen_clarity": "  +%s%% from Clarity",
        "screen.wildercord.mana.regen_meditation": "  +%s%% while meditating",
        "screen.wildercord.mana.siphon": "Siphon %s: +%s mana per creature your spells hit",
        "screen.wildercord.mana.ways": "Ways to grow your mana",
        "screen.wildercord.mana.way.crystals": "Mana Crystals: +%s max mana each, up to %s (craft or find in deep ruins)",
        "screen.wildercord.mana.way.enchant": "Enchant your Cord: Reservoir, Wellspring, Siphon",
        "screen.wildercord.mana.way.potions": "Brew Clarity (amethyst shard) or Mana (lapis lazuli)",
        "screen.wildercord.mana.way.meditate": "Meditate: sneak and stand still to regenerate twice as fast",
        "entity.wildercord.spirit_wolf": "Spirit Wolf",
        "category.wildercord.shape.personal": "Personal",
        "category.wildercord.shape.direct": "Direct",
        "category.wildercord.shape.projectile": "Projectile",
        "category.wildercord.shape.area": "Area",
        "category.wildercord.shape.lingering": "Lingering",
        "category.wildercord.effect.damage": "Damage",
        "category.wildercord.effect.control": "Control",
        "category.wildercord.effect.support": "Support",
        "category.wildercord.effect.movement": "Movement",
        "category.wildercord.effect.time": "Time",
        "category.wildercord.effect.world": "World",
        "category.wildercord.effect.summon": "Summon",
        "category.wildercord.modifier.power": "Power",
        "category.wildercord.modifier.area": "Area",
        "category.wildercord.modifier.timing": "Timing",
        "category.wildercord.modifier.projectile": "Projectile",
        "category.wildercord.modifier.circle": "Circle disciplines",
        "category.wildercord.link.timing": "Timing",
        "category.wildercord.link.trigger": "Trigger",
        "category.wildercord.link.reactive": "Reactive",
        "category.wildercord.link.condition": "Condition",
        "screen.wildercord.search": "Search...",
        "screen.wildercord.matches": "%s runes",
        "screen.wildercord.chips_hint": "Pick a family to filter by category",
        "screen.wildercord.chip.all": "All",
        "screen.wildercord.no_results": "No runes match \"%s\"",
        "screen.wildercord.help.7": "Just type to search the Codex (Ctrl+F). Esc clears the search",
        "reaction.wildercord.shatter": "Shatter!",
        "reaction.wildercord.conduct": "Conduct!",
        "reaction.wildercord.wildfire": "Wildfire!",
        "reaction.wildercord.implode": "Implode!",
        "reaction.wildercord.collapse": "Collapse!",
        "reaction.wildercord.blackspark": "Blackspark!",
        "reaction.wildercord.combo": "Combo!",
        "message.wildercord.no_health": "Blood Price needs more than %s health",
        "message.wildercord.swap_blocked": "No room to trade places",
        "message.wildercord.light_blocked": "No room for a light there",
        "message.wildercord.silenced": "You are silenced and cannot cast",
        "message.wildercord.charge_interrupted": "Your charge is cut short",
        "message.wildercord.zipper_no_wall": "There's no wall in front of you to unzip",
        "message.wildercord.zipper_unbreakable": "That can't be unzipped",
        "message.wildercord.zipper_thick": "Too thick to unzip (6 blocks at most)",
        "message.wildercord.too_many_birds": "You can only keep %s Thunderbirds at once",
        "message.wildercord.rewind_dimension": "Rewind can't reach into another dimension",
        "message.wildercord.time_resumes": "Time resumes: %s damage from %s hits lands at once",
        "message.wildercord.reversal": "Reversal! Death turned back",
        "message.wildercord.deaths_door": "Death was cheated too recently: nothing turns it back again for %s s",
        "message.wildercord.rebirth_resting": "Too soon to be reborn again (%s s)",
        "message.wildercord.time_skip_nowhere": "Nowhere safe ahead to skip to",
        "message.wildercord.warded": "These walls are warded: the only way in is through the dungeon",
        "message.wildercord.soar_fading": "The wind beneath you is fading...",
        "message.wildercord.soar_ended": "The wind sets you down gently",
        "message.wildercord.soar_grounded": "Grounded! The wind is torn out from under you",
        "message.wildercord.soar_warded": "The ward stills the wind here",
        "message.wildercord.soar_travelled": "The wind doesn't follow you between worlds",
        "message.wildercord.soar_already": "You can already fly",
        "message.wildercord.soar_soaring": "Already soaring",
        "message.wildercord.soar_soaring_ally": "%s is already soaring",
        "message.wildercord.soar_resting": "Your wings need rest: %s s",
        "message.wildercord.soar_resting_ally": "%s's wings need rest: %s s",
        "entity.wildercord.shadow_hound": "Shadow Hound",
        "modmenu.descriptionTranslation.wildercord": "Thread simple runes onto a Cord in any order, then cast the whole sequence with one key.",
    }
    for i in range(1, 5):
        lang[f"key.wildercord.cast_{i}"] = f"Cast spell {i}"
    for element in ELEMENT_COLOR:
        lang[f"element.wildercord.{element}"] = element.capitalize()
    for r in runes:
        lang[f"rune.wildercord.{r['path']}"] = r["name"]
        lang[f"rune.wildercord.{r['path']}.desc"] = r["desc"]
    lang.update(source_lang(runes))
    lang.update(NEW_LANG)
    lang.update(WORLD_MAGIC_LANG)
    lang.update(REACTIONS_LANG)
    lang.update(affinity_data.LANG)
    lang.update(PLAYER_AFFINITY_LANG)
    lang.update(WORLD_LANG)
    lang.update(PARRY_AND_WILD_LANG)
    lang.update(advancement_lang())
    lang.update(familiar_lang())
    lang.update(GEAR_LANG)
    lang.update(BACKPACK_LANG)
    lang.update(DUNGEON_LANG)
    lang.update(TRAVEL_LANG)
    lang.update(LOADOUT_LANG)
    lang.update(SIGNATURE_LANG)
    lang.update(DEFENCE_LANG)
    lang.update(PERFORMANCE_LANG)
    # In rune order, not set order: set order changes from run to run and the file must not.
    for path in (r["path"] for r in runes if r["path"] in INNATE):
        lang[f"rune.wildercord.{path}.found"] = "Innate: wakes in one caster's heart at the 1st Circle"
        lang.pop(f"rune.wildercord.{path}.craft", None)
    for path in (r["path"] for r in runes if r["path"] in ELEMENT_FUSIONS):
        a, b = ELEMENT_FUSIONS[path]
        lang[f"rune.wildercord.{path}.found"] = f"Fused at a Fusion Altar: any {a.title()} effect + any {b.title()} effect"
    names = {r["path"]: r["name"] for r in runes}
    for path in (r["path"] for r in runes if r["path"] in SIGNATURES):
        a, b = SIGNATURES[path]
        lang[f"rune.wildercord.{path}.found"] = f"A signature fusion at a Fusion Altar: {names[a]} + {names[b]}, those two runes only"
    write_recipe_doc(runes)
    write_fusion_doc(runes)
    write_json(ASSETS / "lang/en_us.json", lang)


# ---------------------------------------------------------------- where each rune comes from (for tooltips)

LOOT_TABLE_NAMES = {
    "SIMPLE_DUNGEON": "Dungeons", "ABANDONED_MINESHAFT": "Mineshafts", "SHIPWRECK_TREASURE": "Shipwrecks",
    "BURIED_TREASURE": "Buried treasure", "DESERT_PYRAMID": "Desert pyramids", "TRIAL_CHAMBERS_REWARD_COMMON": "Trial vaults",
    "TRIAL_CHAMBERS_REWARD_RARE": "Trial vaults", "TRIAL_CHAMBERS_REWARD_OMINOUS_RARE": "Ominous vaults",
    "ANCIENT_CITY": "Ancient cities", "END_CITY_TREASURE": "End cities", "STRONGHOLD_LIBRARY": "Stronghold libraries",
    "BASTION_TREASURE": "Bastions", "WOODLAND_MANSION": "Woodland mansions", "TRAIL_RUINS_ARCHAEOLOGY_RARE": "Trail ruins (brushing)",
}


ITEM_NAMES = {"minecraft:tnt": "TNT"}
BOSSES = {"WARDEN", "WITHER", "ELDER_GUARDIAN", "ENDER_DRAGON"}
MOB_PLURALS = {"ENDERMAN": "Endermen", "WITCH": "Witches"}


def item_name(item_id):
    if item_id in ITEM_NAMES:
        return ITEM_NAMES[item_id]
    if item_id.startswith("#"):
        return "any " + item_id.split(":")[1].replace("_", " ")
    return item_id.split(":")[1].replace("_", " ").title()


def mob_source(mob, chance):
    name = mob.replace("_", " ").title()
    if mob in BOSSES:
        return f"the {name}" + ("" if chance >= 100 else f" ({chance}%)")
    return f"{MOB_PLURALS.get(mob, name + 's')} ({chance}%)"


def recipe_text(ingredients):
    counts = {}
    for item in ingredients[1:]:  # the Blank Rune is implied
        counts[item] = counts.get(item, 0) + 1
    return ", ".join((f"{n}x " if n > 1 else "") + item_name(i) for i, n in counts.items())


def rune_constants():
    src = (ROOT / "src/main/java/dev/wildercord/spell/Runes.java").read_text(encoding="utf-8")
    return dict(re.findall(r'public static final RuneDef (\w+) = \w+\("(\w+)"', src))


def loot_sources():
    found = _loot_sources()
    # Every Tier IV rune can also come from an Archive or a dimension dungeon: their vaults, or their bosses.
    for path in _tier_four_paths():
        found.setdefault(path, []).extend(["Archive vaults", "the Archivist"])
    # The runes of the world: every place RuneSources lists, first (it's where they're really from).
    for sid, where, paths in rune_sources():
        text = f"{where} ({ADEPT_FIND_CHANCE}%)" if sid == "runebound_adept" else where
        for path in paths:
            places = found.setdefault(path, [])
            if text not in places:
                places.append(text)
    # The dimension dungeons' chests hold runes of their elements.
    # (Only their common runes: never an innate, fused or found-only one, as the chests' own pools.)
    for path, places in dungeon_assets.sources(read_runes(), set(INNATE) | found_only() | set(FUSED)).items():
        for place in places:
            if place not in found.setdefault(path, []):
                found[path].append(place)
    return found


def _tier_four_paths():
    world = found_only()
    return [r["path"] for r in read_runes() if r["tier"] == 4 and r["path"] not in world and r["path"] not in FUSED]


def _adept_find_chance():
    src = (ROOT / "src/main/java/dev/wildercord/content/WildercordLoot.java").read_text(encoding="utf-8")
    m = re.search(r"ADEPT_FIND_CHANCE = (\d+);", src)
    return int(m.group(1)) if m else 0


ADEPT_FIND_CHANCE = _adept_find_chance()


def _loot_sources():
    """Parses WildercordLoot.java: rune path -> list of places it drops, so tooltips never drift from the loot."""
    src = (ROOT / "src/main/java/dev/wildercord/content/WildercordLoot.java").read_text(encoding="utf-8")
    consts = rune_constants()
    named_lists = {}
    for name, body in re.findall(r"List<RuneDef> (\w+) = List\.of\((.*?)\);", src, re.S):
        named_lists[name] = re.findall(r"Runes\.(\w+)", body)
    found = {}

    def add(const, where):
        path = consts[const]
        if where not in found.setdefault(path, []):
            found[path].append(where)

    for table, chance, body in re.findall(r"(?:RUNE|ARCHAEOLOGY)_POOLS\.put\(BuiltInLootTables\.(\w+), new RunePool\((\d+),\s*(.*?)\)\);", src, re.S):
        consts_in = named_lists.get(body.strip(), None) or re.findall(r"Runes\.(\w+)", body)
        for c in consts_in:
            add(c, LOOT_TABLE_NAMES.get(table, table.replace("_", " ").title()))
    # Fishing: the sea list comes up in treasure catches and magic waters, as the runes found only by fishing do (same
    # words as their source). Named first, so it shows even when a rune drops in more places than a tooltip lists.
    fishing = next((where for sid, where, _ in rune_sources() if sid == "fishing"), "Fishing")
    for c in named_lists.get("SEA", []):
        places = found.setdefault(consts[c], [])
        if fishing not in places:
            places.insert(0, fishing)
    for mob, drops in re.findall(r"Map\.entry\(EntityTypes\.(\w+), List\.of\((.*?)\)\)(?=,\s*\n|\s*\n\s*\);)", src, re.S):
        for chance, c in re.findall(r"Map\.entry\((\d+), Runes\.(\w+)\)", drops):
            add(c, mob_source(mob, int(chance)))
    dragon = re.search(r"for \(RuneDef rune : List\.of\((.*?)\)\)", src, re.S)
    if dragon:
        for c in re.findall(r"Runes\.(\w+)", dragon.group(1)):
            add(c, "the Ender Dragon")
    return found


def write_recipe_doc(runes):
    """docs/RECIPES.md: every recipe by tier, and where the Tier IV runes are found."""
    found = loot_sources()
    extras = {1: "nothing extra", 2: "2 Lapis Lazuli and a Gold Ingot", 3: "a Mana Crystal and a Diamond"}
    lines = ["# Rune recipes", "",
             "Generated by `tools/generate_assets.py` from the same data the game uses. Every rune recipe is shapeless:",
             "a **Blank Rune** plus the items below, plus a cost that grows with the tier:", ""]
    for tier in (1, 2, 3):
        lines.append(f"- Tier {'I' * tier if tier < 4 else 'IV'}: {extras[tier]}")
    lines += ["", "Recipes appear in the crafting recipe book once you hold a Blank Rune",
              "(Blank Rune: 4 Cobblestone around 1 Lapis Lazuli, makes 4). Tier IV runes can't be crafted.", ""]
    world = found_only()
    for tier in (1, 2, 3):
        rs = [r for r in runes if r["tier"] == tier and r["path"] not in INNATE and r["path"] not in FUSED and r["path"] not in world]
        lines += [f"## Tier {['I', 'II', 'III'][tier - 1]} ({len(rs)} runes, + {extras[tier]})", "",
                  "| Rune | Family | Items |", "|---|---|---|"]
        for r in sorted(rs, key=lambda r: (r["family"], r["name"])):
            items = recipe_text(["wildercord:blank_rune", *RUNE_RECIPES[r["path"]]])
            lines.append(f"| {r['name']} | {r['family'].title()} | {items} |")
        lines.append("")
    t4 = [r for r in runes if r["tier"] == 4 and r["path"] not in world and r["path"] not in FUSED]
    lines += [f"## Tier IV ({len(t4)} runes, found only)", "", "| Rune | Family | Found |", "|---|---|---|"]
    for r in sorted(t4, key=lambda r: (r["family"], r["name"])):
        lines.append(f"| {r['name']} | {r['family'].title()} | {', '.join(found.get(r['path'], ['?']))} |")
    fused = [r for r in runes if r["path"] in ELEMENT_FUSIONS]
    lines += ["", f"## Fused runes ({len(fused)}, made only at the Fusion Altar)", "",
              "Any two effects of the ten elements (one element with itself too), an amethyst shard and 3 XP levels.",
              "Any effect of an element counts.", "",
              "| Rune | Elements | Does |", "|---|---|---|"]
    for r in fused:
        a, b = ELEMENT_FUSIONS[r["path"]]
        lines.append(f"| {r['name']} | {a.title()} + {b.title()} | {r['desc']} |")
    lines += signature_table(runes, "## Signature fusions ({n}, made only at the Fusion Altar)",
                             ["Two particular effects, an amethyst shard and 3 XP levels. The pair makes its signature rune "
                              "instead of its elements' fusion; any other effects of those elements still make that."])
    lines += ["", "The Fusion Altar itself: 4 Amethyst Blocks, 4 Deepslate Tiles and a Lodestone "
              "(tiles in the corners, the lodestone in the middle)."]
    worldly = [r for r in runes if r["path"] in world]
    lines += ["", f"## Runes of the world ({len(worldly)} runes, found only)", "",
              "Never crafted, whatever their tier: each is found only in its own places (vanilla structures, a biome by",
              "Attunement, Wildercord's dungeons and bosses, world events). Attunement: meditate with a Blank Rune in hand",
              "in the right biome, under the right conditions, for 20 seconds.", "",
              "| Rune | Family | Tier | Found |", "|---|---|---|---|"]
    for r in sorted(worldly, key=lambda r: (r["family"], r["tier"], r["name"])):
        lines.append(f"| {r['name']} | {r['family'].title()} | {['I', 'II', 'III', 'IV'][r['tier'] - 1]} | {', '.join(found.get(r['path'], ['?']))} |")
    innate = sorted((r for r in runes if r["path"] in INNATE), key=lambda r: r["name"])
    lines += ["", f"## Innate runes ({len(innate)}, never crafted or found)", "",
              "One wakes in each caster's heart at the 1st Circle, chosen at random, and grows with every circle.", "",
              "| Rune | Element | Does |", "|---|---|---|"]
    for r in innate:
        lines.append(f"| {r['name']} | {r['element'].title()} | {r['desc']} |")
    lines += ["", "Chests favour low tiers: next to each other in a pool, Tier I runes are 8x as likely as Tier IV,",
              "Tier II 5x and Tier III 2x.", ""]
    (ROOT / "docs/RECIPES.md").write_text("\n".join(lines), encoding="utf-8", newline="\n")


ELEMENTS = ["fire", "frost", "storm", "wind", "earth", "life", "void", "arcane", "time", "blood"]


def write_fusion_doc(runes):
    """docs/features/fusion-altar.md: the grid of what every pair of elements fuses into, and what each does
    (written between the <!-- fusions --> markers, so the rest of the guide is hand-written)."""
    names = {r["path"]: r["name"] for r in runes}
    descs = {r["path"]: r["desc"] for r in runes}
    by_pair = {}
    for path, (a, b) in ELEMENT_FUSIONS.items():
        by_pair[frozenset((a, b))] = path
    lines = ["<!-- fusions:start (generated by tools/generate_assets.py) -->", "",
             "| | " + " | ".join(e.title() for e in ELEMENTS) + " |", "|---" * (len(ELEMENTS) + 1) + "|"]
    for a in ELEMENTS:
        cells = []
        for b in ELEMENTS:
            path = by_pair.get(frozenset((a, b)))
            cells.append(names[path] if path else "")
        lines.append(f"| **{a.title()}** | " + " | ".join(cells) + " |")
    lines += ["", "| Elements | Makes | Does |", "|---|---|---|"]
    for path, (a, b) in ELEMENT_FUSIONS.items():
        pair = a.title() if a == b else f"{a.title()} + {b.title()}"
        lines.append(f"| {pair} | **{names[path]}** | {descs[path]} |")
    lines += signature_table(runes, "### Signature fusions ({n})",
                             ["Each of these pairs of particular runes makes its own rune, in place of the element fusion in the grid above "
                              "(which any other effects of those two elements still make)."])
    lines += ["", "<!-- fusions:end -->"]
    doc = ROOT / "docs/features/fusion-altar.md"
    text = doc.read_text(encoding="utf-8")
    start = text.index("<!-- fusions:start")
    end = text.index("<!-- fusions:end -->") + len("<!-- fusions:end -->")
    doc.write_text(text[:start] + "\n".join(lines) + text[end:], encoding="utf-8", newline="\n")


# ---------------------------------------------------------------- signature fusions (Fusions.SIGNATURES)

def signature_table(runes, heading, intro):
    """The signature fusions as a Markdown table under {@code heading} ({n}: how many), for the generated docs:
    the two runes, what they make, what it counts as, its tier and its mana, what it does, and which element
    fusion the pair would otherwise make. Nothing at all while there are none."""
    if not SIGNATURES:
        return []
    by = {r["path"]: r for r in runes}
    src = (ROOT / "src/main/java/dev/wildercord/spell/Runes.java").read_text(encoding="utf-8")
    cost = dict(re.findall(r'= effect\("(\w+)", "[^"]+", \d, ([\d.]+),', src))
    by_pair = {frozenset(pair): path for path, pair in ELEMENT_FUSIONS.items()}
    lines = ["", heading.format(n=len(SIGNATURES)), "", *intro, "",
             "| Runes | Makes | Counts as | Tier | Mana | Does | In place of |", "|---|---|---|---|---|---|---|"]
    for path, (a, b) in SIGNATURES.items():
        r = by[path]
        over = by_pair.get(frozenset((by[a]["element"], by[b]["element"])))
        mana = float(cost[path])
        lines.append(f"| {by[a]['name']} + {by[b]['name']} | **{r['name']}** | {r['element'].title()} | {['I', 'II', 'III', 'IV'][r['tier'] - 1]} "
                     f"| {int(mana) if mana == int(mana) else mana} | {r['desc']} | {by[over]['name'] if over else ''} |")
    return lines


# The Fusion Altar's and the Grimoire's words for signature fusions.
SIGNATURE_LANG = {
    "screen.wildercord.altar.kind.signature": "Signature fusion",
    "screen.wildercord.altar.signature_line": "Born of %s and %s themselves, not just their elements",
    "screen.wildercord.grimoire.signatures": "Signature fusions (%s of %s)",
    "screen.wildercord.grimoire.signature_how": "%s and %s, with an amethyst shard, at a Fusion Altar: those two runes only",
    "screen.wildercord.grimoire.signature_over": "The pair makes this instead of %s",
    "screen.wildercord.grimoire.signature_hint": "(a rune of %s, and one of %s)",
    "screen.wildercord.grimoire.signature_unknown": "Not found yet. Two particular runes of these elements make it, "
                                                    "in place of their elements' own fusion: try pairs at a Fusion Altar.",
    "toast.wildercord.signature": "Signature fusion: %s",
    # The signature runes' messages to their caster.
    "message.wildercord.bloomstep_far": "Too far to step (32 blocks at most)",
    "message.wildercord.thunderstep_far": "Too far to strike (24 blocks at most)",
    "message.wildercord.step_nowhere": "Nowhere safe to set foot there",
}


def source_lang(runes):
    lang = {}
    found = loot_sources()
    for r in runes:
        if r["path"] in RUNE_RECIPES:
            lang[f"rune.wildercord.{r['path']}.craft"] = "Craft: Blank Rune, " + recipe_text(rune_ingredients(r))
        places = found.get(r["path"], [])
        if places:
            shown = places[:4] + (["and more"] if len(places) > 4 else [])
            lang[f"rune.wildercord.{r['path']}.found"] = "Found: " + ", ".join(shown)
    return lang


# ---------------------------------------------------------------- recipes

# Key ingredients from docs/DESIGN.md. Found-only runes (Zone, Split, Chain, Rain, Homing, On Kill,
# Explode, Blink, Lightning, boss runes) have no recipe on purpose.
RUNE_RECIPES = {
    "touch": ["minecraft:leather"],
    "feather_fall": ["minecraft:feather", "minecraft:feather"],
    "swift": ["minecraft:sugar", "minecraft:sugar"],
    "night_eye": ["minecraft:glow_berries"],
    "heal": ["minecraft:glistering_melon_slice"],
    "harm": ["minecraft:fermented_spider_eye"],
    "light": ["minecraft:torch", "minecraft:torch"],
    "grow": ["minecraft:bone_meal", "minecraft:bone_meal"],
    "amplify": ["minecraft:gold_ingot"],
    "extend": ["minecraft:redstone", "minecraft:redstone"],
    "delay": ["minecraft:clock"],
    "beam": ["minecraft:spyglass"],
    "burst": ["minecraft:gunpowder", "minecraft:gunpowder"],
    "shield": ["minecraft:shield"],
    "launch": ["minecraft:wind_charge"],
    "dash": ["minecraft:rabbit_foot"],
    "pull": ["minecraft:fishing_rod"],
    "fire": ["minecraft:blaze_powder"],
    "frost": ["minecraft:powder_snow_bucket"],
    "break": ["minecraft:iron_pickaxe"],
    "widen": ["minecraft:amethyst_shard", "minecraft:amethyst_shard"],
    "quicken": ["minecraft:breeze_rod"],
    "pierce": ["minecraft:arrow", "minecraft:arrow"],
    "bounce": ["minecraft:slime_block"],
    "on_hit": ["minecraft:target"],
    "on_land": ["minecraft:hay_block"],
    "echo": ["minecraft:echo_shard", "minecraft:echo_shard"],
    # Expansion runes.
    "arc": ["minecraft:snowball", "minecraft:snowball"],
    "shock": ["minecraft:lightning_rod"],
    "haste": ["minecraft:golden_pickaxe"],
    "reveal": ["minecraft:glow_ink_sac"],
    "frugal": ["minecraft:emerald"],
    "cone": ["minecraft:fire_charge"],
    "trail": ["minecraft:glowstone_dust", "minecraft:glowstone_dust"],
    "regrowth": ["minecraft:ghast_tear"],
    "cleanse": ["minecraft:milk_bucket"],
    "stoneskin": ["minecraft:armadillo_scute"],
    "root": ["minecraft:vine", "minecraft:vine"],
    "veil": ["minecraft:golden_carrot", "minecraft:fermented_spider_eye"],
    "empower": ["minecraft:iron_sword"],
    "levitate": ["minecraft:phantom_membrane"],
    "linger": ["minecraft:honey_bottle"],
    "volley": ["minecraft:crossbow"],
    "pulse": ["minecraft:repeater"],
    "on_hurt": ["minecraft:cactus"],
    # Batch 3.
    "ring": ["minecraft:bell"],
    "pillar": ["minecraft:pointed_dripstone", "minecraft:pointed_dripstone"],
    "wave": ["minecraft:kelp", "minecraft:kelp"],
    "mine": ["minecraft:tripwire_hook"],
    "venom": ["minecraft:poisonous_potato"],
    "thunderclap": ["minecraft:goat_horn"],
    "blind": ["minecraft:ink_sac"],
    "chill": ["minecraft:ice"],
    "silence": ["#minecraft:wool"],
    "fireward": ["minecraft:magma_cream"],
    "nourish": ["minecraft:bread"],
    "tidebreath": ["minecraft:pufferfish"],
    "leap": ["minecraft:slime_ball"],
    "grapple": ["minecraft:lead"],
    "harvest": ["minecraft:wheat", "minecraft:wheat"],
    "icepath": ["minecraft:packed_ice"],
    "collect": ["minecraft:hopper"],
    "excavate": ["minecraft:iron_shovel"],
    "focus": ["minecraft:glass_pane", "minecraft:gold_nugget"],
    "rapid": ["minecraft:sugar", "minecraft:redstone"],
    "if_sneaking": ["minecraft:leather_boots"],
    # Batch 4.
    "crescent": ["minecraft:iron_sword", "minecraft:feather"],
    "barrage": ["minecraft:leather", "minecraft:iron_ingot"],
    "blitz": ["minecraft:rabbit_foot", "minecraft:sugar"],
    "dismantle": ["minecraft:shears"],
    "aftershock": ["minecraft:piston", "minecraft:cobblestone"],
    "ripple": ["minecraft:sunflower", "minecraft:glowstone_dust"],
    "repel": ["minecraft:wind_charge", "minecraft:wind_charge"],
    "decree": ["minecraft:writable_book"],
    "weigh": ["minecraft:iron_block"],
    "shackle": ["minecraft:iron_chain", "minecraft:iron_chain"],
    "bubble": ["minecraft:water_bucket", "minecraft:slime_ball"],
    "overdrive": ["minecraft:blaze_powder", "minecraft:redstone"],
    "swap": ["minecraft:ender_pearl", "minecraft:ender_pearl"],
    "zipper": ["minecraft:iron_nugget", "minecraft:iron_nugget", "minecraft:string"],
    "rampart": ["minecraft:packed_mud", "minecraft:packed_mud"],
    "accelerate": ["minecraft:clock", "minecraft:sugar"],
    "foresight": ["minecraft:spyglass"],
    "restore": ["minecraft:iron_ingot", "minecraft:glistering_melon_slice"],
    "primer": ["minecraft:tnt", "minecraft:pink_dye"],
    "reflect": ["minecraft:shield", "minecraft:glass_pane"],
    "thunderbird": ["minecraft:feather", "minecraft:lightning_rod"],
    "execute": ["minecraft:iron_axe"],
    "blood_price": ["minecraft:ghast_tear", "minecraft:redstone"],
    "if_airborne": ["minecraft:feather", "minecraft:phantom_membrane"],
    # Tier III: the rest of them, so every Tier I-III rune can be crafted (Tier III also takes a
    # Mana Crystal and a Diamond, added in write_recipes).
    "zone": ["minecraft:redstone_block"],
    "rain": ["minecraft:pointed_dripstone", "minecraft:water_bucket"],
    "wall": ["minecraft:obsidian", "minecraft:obsidian"],
    "orbit": ["minecraft:ender_eye"],
    "totem": ["minecraft:emerald_block"],
    "orb": ["minecraft:slime_block"],
    "lightning": ["minecraft:copper_block", "minecraft:glowstone"],
    "blink": ["minecraft:ender_pearl", "minecraft:chorus_fruit"],
    "explode": ["minecraft:tnt", "minecraft:fire_charge"],
    "freeze": ["minecraft:blue_ice", "minecraft:blue_ice"],
    "meteor": ["minecraft:magma_block", "minecraft:fire_charge"],
    "tremor": ["minecraft:deepslate_bricks", "minecraft:tnt"],
    "gravity_well": ["minecraft:ender_eye", "minecraft:crying_obsidian"],
    "smite": ["minecraft:glowstone", "minecraft:golden_carrot"],
    "inferno": ["minecraft:blaze_rod", "minecraft:magma_block"],
    "cleave": ["minecraft:diamond_axe"],
    "blackspark": ["minecraft:black_dye", "minecraft:glowstone"],
    "resonance": ["minecraft:iron_nugget", "minecraft:hay_block"],
    "blackflame": ["minecraft:soul_campfire", "minecraft:black_dye"],
    "shadowstep": ["minecraft:ender_pearl", "minecraft:ink_sac"],
    "time_skip": ["minecraft:clock", "minecraft:ender_pearl"],
    "shades": ["minecraft:bone", "minecraft:bone", "minecraft:black_dye"],
    "split": ["minecraft:prismarine_crystals", "minecraft:prismarine_crystals"],
    "homing": ["minecraft:compass"],
    "chain": ["minecraft:iron_chain", "minecraft:redstone"],
    "overcharge": ["minecraft:glowstone", "minecraft:glowstone"],
    "vow": ["minecraft:paper", "minecraft:gold_block"],
    "on_kill": ["minecraft:bone_block"],
    "on_low_health": ["minecraft:golden_apple"],
    "combo": ["minecraft:repeater", "minecraft:repeater"],
    "imbue": ["minecraft:experience_bottle"],
    # Batch 6: sparks, energy balls and beams.
    "spark": ["minecraft:flint", "minecraft:glowstone_dust"],
    "ray": ["minecraft:glass_pane", "minecraft:glowstone_dust"],
    "nova": ["minecraft:gunpowder", "minecraft:glowstone_dust"],
    "wisp": ["minecraft:glow_berries", "minecraft:amethyst_shard"],
    "comet": ["minecraft:fire_charge", "minecraft:gunpowder"],
    "ricochet": ["minecraft:slime_ball", "minecraft:snowball"],
    "cluster": ["minecraft:gunpowder", "minecraft:gunpowder", "minecraft:amethyst_shard"],
    "lance": ["minecraft:spyglass", "minecraft:blaze_rod"],
    "sweep": ["minecraft:spyglass", "minecraft:string"],
    "prism": ["minecraft:prismarine_crystals", "minecraft:glass"],
    "stream": ["minecraft:spyglass", "minecraft:redstone_torch"],
    # Batch 6: protection.
    "barrier": ["minecraft:glass", "minecraft:amethyst_shard"],
    "brace": ["minecraft:cobblestone", "minecraft:iron_ingot"],
    "anchor": ["minecraft:iron_chain", "minecraft:cobblestone"],
    "bramble": ["minecraft:sweet_berries", "minecraft:cactus"],
    "frostward": ["minecraft:snowball", "minecraft:leather"],
    "cushion": ["#minecraft:wool", "minecraft:feather"],
    "deflect": ["minecraft:shield", "minecraft:wind_charge"],
    "haven": ["minecraft:shield", "minecraft:glistering_melon_slice"],
    # Batch 6: mining and building.
    "chisel": ["minecraft:stone_pickaxe"],
    "glimmer": ["minecraft:glow_lichen"],
    "prune": ["minecraft:shears", "#minecraft:saplings"],
    "tunnel": ["minecraft:iron_pickaxe", "minecraft:rail"],
    "vein": ["minecraft:iron_pickaxe", "minecraft:raw_iron"],
    "smelt": ["minecraft:furnace", "minecraft:blaze_powder"],
    "fell": ["minecraft:iron_axe", "#minecraft:logs"],
    "span": ["minecraft:magenta_stained_glass", "minecraft:magenta_stained_glass"],
    # Batch 6: a simple spell for every element.
    "ember": ["minecraft:coal", "minecraft:flint"],
    "icicle": ["minecraft:ice", "minecraft:pointed_dripstone"],
    "pelt": ["minecraft:gravel", "minecraft:cobblestone"],
    "windcut": ["minecraft:feather", "minecraft:flint"],
    "leech": ["minecraft:spider_eye", "minecraft:redstone"],
    "hex": ["minecraft:fermented_spider_eye", "minecraft:ink_sac"],
    "rend": ["minecraft:iron_nugget", "minecraft:bone"],
    "countdown": ["minecraft:clock", "minecraft:gunpowder"],
    "jolt": ["minecraft:lightning_rod", "minecraft:iron_ingot"],
    "bleed": ["minecraft:shears", "minecraft:redstone"],
    "coldsnap": ["minecraft:packed_ice", "minecraft:snow_block"],
    "flashfire": ["minecraft:blaze_powder", "minecraft:gunpowder"],
    "banish": ["minecraft:ender_pearl", "minecraft:popped_chorus_fruit"],
    "cyclone": ["minecraft:wind_charge", "minecraft:breeze_rod"],
    # Starters, so a lost Cord owner can always craft them back.
    "self": ["minecraft:glass_pane"],
    "bolt": ["minecraft:arrow"],
    "push": ["minecraft:piston"],
}

# The second batch of new runes (Runes.java, "new runes (batch 2)"): their own items, kept apart from the rest.
NEW_RUNES_2_RECIPES = {
    "glaive": ["minecraft:iron_axe", "minecraft:string"],
    "imprint": ["minecraft:clay_ball", "minecraft:gunpowder"],
    "latch": ["minecraft:lead", "minecraft:amethyst_shard"],
    "kindred": ["minecraft:cake"],
    "thirst": ["minecraft:spider_eye", "minecraft:glass_bottle"],
    "belated": ["minecraft:clock", "minecraft:cobweb"],
    "on_reaction": ["minecraft:brewing_stand"],
    "on_weakness": ["minecraft:fermented_spider_eye", "minecraft:target"],
    "spellbrand": ["minecraft:book", "minecraft:gunpowder"],
    "gash": ["minecraft:flint", "minecraft:rotten_flesh"],
    "prospect": ["minecraft:stone_pickaxe", "minecraft:amethyst_shard"],
    "searing_edge": ["minecraft:iron_sword", "minecraft:blaze_powder"],
    "flash_freeze": ["minecraft:packed_ice", "minecraft:water_bucket"],
    "drowse": ["minecraft:spore_blossom", "minecraft:honey_bottle"],
    "galvanize": ["minecraft:lightning_rod", "minecraft:redstone"],
    "prolong": ["minecraft:clock", "minecraft:redstone", "minecraft:redstone"],
    "umbra": ["minecraft:ink_sac", "minecraft:flint"],
    "disarm": ["minecraft:wind_charge", "minecraft:fishing_rod"],
}
RUNE_RECIPES.update(NEW_RUNES_2_RECIPES)
# Flight (Runes.java, after batch 2): wings of wind from a phantom's membrane, a feather and a breeze's rod.
RUNE_RECIPES["soar"] = ["minecraft:phantom_membrane", "minecraft:feather", "minecraft:breeze_rod"]
RUNE_RECIPES["strata_rise"] = ["minecraft:stone", "minecraft:packed_mud", "minecraft:flint"]
RUNE_RECIPES["tidal_lift"] = ["minecraft:prismarine_shard", "minecraft:kelp", "minecraft:clay_ball"]
RUNE_RECIPES["wind_steps"] = ["minecraft:feather", "minecraft:breeze_rod", "minecraft:string"]
RUNE_RECIPES.update({
    "needle_circle": ["minecraft:flint", "minecraft:iron_nugget"],
    "bloom_circle": ["minecraft:pink_petals", "minecraft:bone_meal"],
    "gyre_circle": ["minecraft:feather", "minecraft:copper_ingot"],
    "anchor_circle": ["minecraft:iron_ingot", "minecraft:stone"],
    "reservoir_circle": ["minecraft:glass_bottle", "minecraft:lapis_lazuli"],
    "crucible_circle": ["minecraft:blaze_powder", "minecraft:brick"],
    "confluence_circle": ["minecraft:amethyst_shard", "minecraft:prismarine_shard"],
    "pilgrim_circle": ["minecraft:compass", "minecraft:feather"],
    "vigil_circle": ["minecraft:spider_eye", "minecraft:iron_nugget"],
    "mercy_circle": ["minecraft:honey_bottle", "minecraft:poppy"],
    "tempest_circle": ["minecraft:breeze_rod", "minecraft:copper_ingot"],
    "eclipse_circle": ["minecraft:ender_pearl", "minecraft:gold_nugget"],
})


def rune_result(path):
    return {"id": "wildercord:rune", "components": {"wildercord:rune": f"wildercord:{path}"}}


# What every recipe adds on top of its themed items, by rune tier: higher tiers cost more.
TIER_CATALYSTS = {
    1: [],
    2: ["minecraft:lapis_lazuli", "minecraft:lapis_lazuli", "minecraft:gold_ingot"],
    3: ["wildercord:mana_crystal", "minecraft:diamond"],
}


def rune_ingredients(rune):
    return ["wildercord:blank_rune", *RUNE_RECIPES[rune["path"]], *TIER_CATALYSTS[rune["tier"]]]


def unlock_advancement(recipe_id, trigger_item):
    """Puts a recipe in the recipe book once the player holds trigger_item, like vanilla's."""
    name = recipe_id.split(":")[1]
    write_json(DATA / f"advancement/recipes/misc/{name}.json", {
        "parent": "minecraft:recipes/root",
        "criteria": {
            "has_item": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": trigger_item}]}},
            "has_the_recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipes": recipe_id}},
        },
        "requirements": [["has_the_recipe", "has_item"]],
        "rewards": {"recipes": [recipe_id]},
    })


def write_recipes(runes):
    import armor_art
    armor_art.write(sys.modules[__name__])
    out = DATA / "recipe"
    by_path = {r["path"]: r for r in runes}
    # The runes of the world are found only in their own places (RuneSources.java): never crafted.
    world = found_only()
    for path in RUNE_RECIPES:
        assert path in by_path, path
    for r in runes:
        # Every Tier I-III rune can be crafted; Tier IV is found only (bosses and rare chests).
        assert (r["path"] in RUNE_RECIPES) == (r["tier"] <= 3 and r["path"] not in INNATE and r["path"] not in FUSED and r["path"] not in world), f"{r['path']} (tier {r['tier']})"
    for path in RUNE_RECIPES:
        ingredients = rune_ingredients(by_path[path])
        assert len(ingredients) <= 9, path
        write_json(out / f"rune_{path}.json", {
            "type": "minecraft:crafting_shapeless", "category": "misc",
            "ingredients": ingredients, "result": rune_result(path)})
        unlock_advancement(f"wildercord:rune_{path}", "wildercord:blank_rune")
    unlock_advancement("wildercord:blank_rune", "minecraft:lapis_lazuli")
    unlock_advancement("wildercord:twine_cord", "wildercord:blank_rune")
    unlock_advancement("wildercord:copper_cord", "wildercord:twine_cord")
    unlock_advancement("wildercord:amethyst_cord", "wildercord:copper_cord")
    unlock_advancement("wildercord:echo_cord", "wildercord:amethyst_cord")
    unlock_advancement("wildercord:mana_crystal", "minecraft:amethyst_shard")
    write_json(out / "blank_rune.json", {
        "type": "minecraft:crafting_shaped", "category": "misc",
        "key": {"C": "minecraft:cobblestone", "L": "minecraft:lapis_lazuli"},
        "pattern": [" C ", "CLC", " C "], "result": {"id": "wildercord:blank_rune", "count": 4}})
    write_json(out / "twine_cord.json", {
        "type": "minecraft:crafting_shaped", "category": "equipment",
        "key": {"S": "minecraft:string", "B": "wildercord:blank_rune"},
        "pattern": ["S S", " S ", " B "], "result": {"id": "wildercord:twine_cord"}})
    upgrades = [("copper", "twine", ["minecraft:copper_ingot"] * 4 + ["minecraft:amethyst_shard"]),
                ("amethyst", "copper", ["minecraft:amethyst_shard"] * 4 + ["minecraft:gold_ingot"] * 2),
                ("echo", "amethyst", ["minecraft:echo_shard"] * 2 + ["minecraft:netherite_scrap"])]
    # A shapeless recipe that keeps the old Cord's enchantments, name and the rest (UpgradeRecipe.java).
    for new, old, extra in upgrades:
        write_json(out / f"{new}_cord.json", {
            "type": "wildercord:upgrade", "category": "equipment",
            "ingredients": [f"wildercord:{old}_cord", *extra], "result": {"id": f"wildercord:{new}_cord"}})


# ---------------------------------------------------------------- mana data

ENCHANTMENTS = {
    # id: (max level, weight, anvil cost, min cost base, per level)
    "reservoir": (3, 10, 2, 5, 10),
    "wellspring": (3, 10, 2, 5, 10),
    "siphon": (2, 4, 4, 15, 12),
    # Spell enchantments: stronger, faster, cheaper, longer.
    "potency": (3, 5, 4, 10, 10),
    "celerity": (3, 5, 4, 10, 10),
    "thrift": (3, 5, 4, 10, 10),
    "persistence": (2, 5, 4, 10, 12),
}

POTIONS = ["clarity", "long_clarity", "strong_clarity", "mana", "strong_mana", "warded", "long_warded", "strong_warded"]
BREWS = [("minecraft:awkward", "minecraft:amethyst_shard", "clarity"),
         ("wildercord:clarity", "minecraft:redstone", "long_clarity"),
         ("wildercord:clarity", "minecraft:glowstone_dust", "strong_clarity"),
         ("minecraft:awkward", "minecraft:lapis_lazuli", "mana"),
         ("wildercord:mana", "minecraft:glowstone_dust", "strong_mana"),
         # The Potion of Warding (Warded: less damage from spells) brews from tinted glass, which keeps light out:
         # Wildercord's magic is light written in the air.
         ("minecraft:awkward", "minecraft:tinted_glass", "warded"),
         ("wildercord:warded", "minecraft:redstone", "long_warded"),
         ("wildercord:warded", "minecraft:glowstone_dust", "strong_warded")]

# Warding, the armour enchantment against spells (SpellDefence.java does what it does). A protection against one kind
# of harm, like Fire or Blast Protection: the same costs and rarity, and it joins their set, so it can't sit beside
# Protection on one piece.
WARDING = {
    "anvil_cost": 2,
    "description": {"translate": "enchantment.wildercord.warding"},
    "exclusive_set": "#minecraft:exclusive_set/armor",
    "max_cost": {"base": 18, "per_level_above_first": 8},
    "max_level": 4,
    "min_cost": {"base": 10, "per_level_above_first": 8},
    "slots": ["armor"],
    "supported_items": "#minecraft:enchantable/armor",
    "weight": 5}


def brewing(name, container_in, potion_in, reagent, container_out, potion_out):
    write_json(DATA / f"recipe/brewing/{name}.json", {
        "type": "minecraft:brewing",
        "input": {"item": container_in, "potion_contents": {"potions": potion_in}},
        "output": {"components": {"minecraft:potion_contents": {"potion": potion_out}}, "id": container_out},
        "reagent": {"item": reagent}})


def write_mana_data():
    cords = [f"wildercord:{k}_cord" for k in CORDS]
    write_json(DATA / "tags/item/enchantable/cord.json", {"values": cords})
    for eid, (max_level, weight, anvil, base, per) in ENCHANTMENTS.items():
        write_json(DATA / f"enchantment/{eid}.json", {
            "anvil_cost": anvil,
            "description": {"translate": f"enchantment.wildercord.{eid}"},
            "max_cost": {"base": base + 30, "per_level_above_first": per},
            "max_level": max_level,
            "min_cost": {"base": base, "per_level_above_first": per},
            "slots": ["any"],
            "supported_items": "#wildercord:enchantable/cord",
            "weight": weight})
    write_json(DATA / "enchantment/warding.json", WARDING)
    write_json(RES / "data/minecraft/tags/enchantment/exclusive_set/armor.json", {"replace": False, "values": ["wildercord:warding"]})
    # Joining non_treasure puts them in the enchanting table, villager trades and random loot.
    write_json(RES / "data/minecraft/tags/enchantment/non_treasure.json",
               {"replace": False, "values": [f"wildercord:{e}" for e in [*ENCHANTMENTS, "warding"]]})

    for container in ("potion", "splash_potion", "lingering_potion"):
        item = f"minecraft:{container}"
        for potion_in, reagent, potion_out in BREWS:
            short = potion_in.split(":")[1]
            brewing(f"{container}_{short}_{reagent.split(':')[1]}", item, potion_in, reagent, item, f"wildercord:{potion_out}")
    for potion in POTIONS:
        brewing(f"potion_{potion}_gunpowder", "minecraft:potion", f"wildercord:{potion}", "minecraft:gunpowder", "minecraft:splash_potion", f"wildercord:{potion}")
        brewing(f"splash_potion_{potion}_dragon_breath", "minecraft:splash_potion", f"wildercord:{potion}", "minecraft:dragon_breath", "minecraft:lingering_potion", f"wildercord:{potion}")

    write_json(DATA / "recipe/mana_crystal.json", {
        "type": "minecraft:crafting_shaped", "category": "misc",
        "key": {"L": "minecraft:lapis_lazuli", "A": "minecraft:amethyst_shard", "D": "minecraft:diamond"},
        "pattern": ["LAL", "ADA", "LAL"], "result": {"id": "wildercord:mana_crystal"}})


# ---------------------------------------------------------------- standing up to spells (SpellDefence.java)

DEFENCE_LANG = {
    "message.wildercord.spellguard": "Your spellguard held: one heart left. It recharges in %ss",
    "screen.wildercord.defence.title": "Spell defence",
    "screen.wildercord.defence.total": "Spells hurt you %s%% less",
    "screen.wildercord.defence.armour": "  %s%% from your armour (against a 5-heart spell)",
    "screen.wildercord.defence.enchant": "  %s%% from Warding and Protection",
    "screen.wildercord.defence.warded": "  %s%% from Warded %s",
    "screen.wildercord.defence.focus": "  %s%% from Focus of Resolve",
    "screen.wildercord.defence.resistance": "  %s%% from Resistance %s",
    "screen.wildercord.defence.none": "  Nothing yet: see the ways below",
    "screen.wildercord.defence.guard_ready": "Spellguard: ready",
    "screen.wildercord.defence.guard_recharging": "Spellguard: recharging (%ss)",
    "screen.wildercord.defence.guard_off": "Spellguard: off on this server",
    "screen.wildercord.defence.guard_rule": "One spell can't take you from %s%% health or more to dead: it leaves you on one heart, then recharges in %ss",
    "screen.wildercord.defence.ways": "Ways to stand up to spells",
    "screen.wildercord.defence.way.armour": "Armour: about half its worth against magic and frost, all of it against fire, lightning and blasts",
    "screen.wildercord.defence.way.warding": "Warding (armour enchantment): 8% less per level, but not on a piece with Protection",
    "screen.wildercord.defence.way.potion": "Potion of Warding (tinted glass): 20% less per level",
    "screen.wildercord.defence.way.focus": "Focus of Resolve: 20% less damage, 15% weaker spell effects",
    "screen.wildercord.defence.way.shield": "A Shield spell stops a spell outright, and a parry turns it back",
    "screen.wildercord.defence.way.resistance": "Resistance and Protection work against spells too",
}

# Casting as a performance: overchannel, the beat, sigil tracing and incantations (cast.Charging, client.SigilTrace,
# client.fx.Incantations, client.CastingOptions).
PERFORMANCE_LANG = {
    "message.wildercord.overchannel.stage": "Overchannel %s",
    "message.wildercord.overchannel.beat": "on the beat",
    "message.wildercord.overchannel.steady": "steadied %s%%",
    "message.wildercord.overchannel.power": " (+%s%% power)",
    "message.wildercord.overchannel.backfire": "The channel tears loose! The gathered mana scatters and your hands shake",
    "hud.wildercord.trace_hint": "Hold %s to steady your hands and trace",
    "screen.wildercord.casting.tracing": "Sigil tracing: %s",
    "screen.wildercord.casting.tracing.tip": "Hold sneak while charging to steady your hands: the camera holds still and the mouse traces the spell's glyph. A good trace steadies an overchannel and adds a little power. Turned off, nothing is lost.",
    "screen.wildercord.casting.assist": "Trace assist: %s",
    "screen.wildercord.casting.assist.none": "None",
    "screen.wildercord.casting.assist.light": "Light",
    "screen.wildercord.casting.assist.strong": "Strong",
    "screen.wildercord.casting.assist.tip": "How far off the glyph's line still counts as on it, and how strongly the tracing point is drawn back onto it.",
    "screen.wildercord.casting.incantations": "Incantations: %s",
    "screen.wildercord.casting.incantations.all": "Shown",
    "screen.wildercord.casting.incantations.hide_mine": "Hide mine",
    "screen.wildercord.casting.incantations.hide_others": "Hide others'",
    "screen.wildercord.casting.incantations.none": "Hidden",
    "screen.wildercord.casting.incantations.tip": "The syllables that rise from a charging caster, one for each rune: anyone close enough can read the spell coming. Hidden ones are silent too.",
}


# ---------------------------------------------------------------- the Archive, the Grimoire and friends

NEW_LANG = {
    # Shield and Imbue.
    "message.wildercord.shield_up": "Shield up: it stops any spell of %s mana or less",
    "message.wildercord.shield_blocked": "Your Shield stopped a spell",
    "message.wildercord.shield_shattered": "Your Shield shattered!",
    "hud.wildercord.shield": "Shield %s · %ss",
    "message.wildercord.imbue_nothing": "Nothing after Imbue to store",
    "message.wildercord.imbue_no_target": "Imbue needs a block to touch, or Self for the item in your hand (or, empty-handed, the block you look at)",
    "message.wildercord.imbue_empty_hand": "Hold an item to imbue",
    "message.wildercord.imbue_cannot": "That can't hold a spell",
    "message.wildercord.imbue_protected": "You can't imbue a block here",
    "message.wildercord.imbued_item": "%s holds %s (%s charges), %s",
    "message.wildercord.imbued_block": "Glyph written: %s (%s charges)",
    "message.wildercord.imbue_spent": "The magic in your %s is spent",
    "message.wildercord.imbue_no_aim": "Look at something to release it at",
    "message.wildercord.imbue_cooling": "Your imbued magic is still recharging (%ss)",
    "message.wildercord.imbue_faded": "The magic in %s has faded: its maker has imbued newer things since",
    "message.wildercord.imbue_oldest_faded": "Your oldest imbued item's magic fades: you can keep %s at a time",
    "tooltip.wildercord.imbued": "Imbued: %s",
    "tooltip.wildercord.imbued_charges": "%s charges, %s",
    "tooltip.wildercord.imbued.shot": "released where its arrows land",
    "tooltip.wildercord.imbued.worn": "released at whatever hurts you",
    "tooltip.wildercord.imbued.tool": "released at each block it breaks and what it strikes",
    "tooltip.wildercord.imbued.weapon": "released at what it strikes",
    "tooltip.wildercord.imbued.use": "use it to release it at what you look at",
    "tooltip.wildercord.imbued.place": "place it and the block becomes a glyph that holds it",
    "screen.wildercord.refused.passive": "%s can't be a passive",
    "screen.wildercord.refused.take_out": "Click a rune on the Cord to take it out, or drag it off.",
    # The Fusion Altar, rune ranks and Knots
    "block.wildercord.fusion_altar": "Fusion Altar",
    "container.wildercord.fusion_altar": "Fusion Altar",
    "item.wildercord.knot": "Knot",
    "item.wildercord.woven_rune": "Woven Rune",
    "category.wildercord.effect.fusion": "Woven fusions",
    "item.wildercord.rootbound_relic": "Rootbound Relic",
    "item.wildercord.stormglass_relic": "Stormglass Relic",
    "item.wildercord.rootbound_relic.desc": "Heal and root nearby monsters. Recharges in 30 seconds.",
    "item.wildercord.stormglass_relic.desc": "A burst of wind repels monsters and grants speed and slow falling. Recharges in 30 seconds.",
    "item.wildercord.knot.named": "Knot: %s",
    "item.wildercord.rune.ranked": "%s Rune %s",
    "family.wildercord.knot": "Knot",
    "category.wildercord.knot.knot": "Knots",
    "screen.wildercord.tab.knot": "Knots",
    "tooltip.wildercord.rank": "Rank %s: +%s%% power, at the same mana",
    "tooltip.wildercord.knot.holds": "A whole spell tied into one rune (%s runes):",
    "tooltip.wildercord.knot.rules": "Takes one socket and costs %s%% less mana than the runes inside",
    "tooltip.wildercord.knot.learn": "Right-click to learn it: anyone can, even without knowing the runes inside",
    "screen.wildercord.knot_holds": "Holds: %s",
    "message.wildercord.ranked_up": "%s is now rank %s, in every spell it's threaded in",
    "message.wildercord.learned_ranked": "Learned %s at rank %s! Press K to thread it into a spell",
    "message.wildercord.altar_xp": "That takes %s XP levels",
    "screen.wildercord.altar.levels": "%s levels",
    "screen.wildercord.altar.fuse": "Fuse",
    "screen.wildercord.altar.tie": "Tie Knot",
    "screen.wildercord.altar.kind.none": "The altar waits",
    "screen.wildercord.altar.kind.upgrade": "Upgrade",
    "screen.wildercord.altar.kind.combine": "Combine",
    "screen.wildercord.altar.kind.weave": "Weave exact effects",
    "screen.wildercord.altar.kind.knot": "Tie a Knot: pick a spell",
    "screen.wildercord.altar.how.1": "Three of a rune: rank it up",
    "screen.wildercord.altar.how.2": "Two effects: shard fuses elements, block weaves the pair",
    "screen.wildercord.altar.how.3": "A Blank Rune and string: tie a spell into a Knot",
    "screen.wildercord.altar.upgrade_line": "+%s%% power at the same mana, in every spell it's threaded in",
    "screen.wildercord.altar.combine_line": "A new effect, born of %s and %s",
    "screen.wildercord.altar.cost": "Costs %s XP levels",
    "screen.wildercord.altar.need_xp": "Needs %s XP levels",
    "screen.wildercord.altar.take_result": "Take the last result out first",
    "screen.wildercord.altar.no_spell": "None of your spells can be tied yet",
    "screen.wildercord.grimoire.fusions": "Fusions (%s of %s)",
    "screen.wildercord.grimoire.fusion": "%s (%s + %s)",
    "screen.wildercord.grimoire.fusion_how": "Any %s effect and any %s effect, with an amethyst shard, at a Fusion Altar",
    "screen.wildercord.grimoire.fusion_hint": "(one is %s)",
    "screen.wildercord.grimoire.fusion_unknown": "Not found yet. Try two effects of different elements at a Fusion Altar.",
    "toast.wildercord.fusion": "Fusion: %s",
    "message.wildercord.version_mismatch": "This server runs Wildercord %s, and you have %s. Runes one of them doesn't know show as Silent Runes: install %s to match.",
    "message.wildercord.version_server_old": "This server runs an older Wildercord than yours (%s). Runes it doesn't know show as Silent Runes: the server needs updating, or install the version it runs.",
    "message.wildercord.version_old": "This server runs Wildercord %s, newer than yours. Update to %s, or runes your version doesn't know will show as Silent Runes.",
    # The fused runes' messages to their caster.
    "message.wildercord.cryostasis_wait": "Too soon to seal %s in ice again (%ss)",
    "message.wildercord.cryostasis_wait_self": "Too soon to seal yourself in ice again (%ss)",
    "message.wildercord.cryostasis_sealed": "You can't cast from inside the ice",
    "message.wildercord.second_wind": "Second Wind!",
    "message.wildercord.second_wind_spent": "Second Wind has saved them already: again in %s s",
    "message.wildercord.soulbond_alone": "Soulbond needs an ally to bind you to",
    "message.wildercord.soulbond_far": "Too far away to bind",
    "message.wildercord.soulbond_unhurt": "A soul that can't be hurt can't share wounds",
    "message.wildercord.transfusion_self": "Transfusion needs an ally to give to",
    "message.wildercord.transfusion_weak": "Too little blood left to give",
    "message.wildercord.transfusion_whole": "They're already whole",
    "message.wildercord.sanguine_rite_weak": "The rite needs more blood than you can spare",
    "subtitles.wildercord.altar_open": "Fusion Altar hums",
    "subtitles.wildercord.altar_fuse": "Runes fuse",
    "subtitles.wildercord.altar_knot": "Knot is tied",
    # Tags, named for recipe viewers.
    "tag.item.wildercord.enchantable.cord": "Enchantable Cords",
    "message.wildercord.overcast_too_costly": "Too costly to overcast: %s mana is more than %s times what your Cord holds",
    "command.wildercord.innate": "Your innate rune is now %s",
    "command.wildercord.runebound": "Bound a Cord to the nearest monster",
    "command.wildercord.no_monster": "No monster within 16 blocks",
    # Items and blocks
    "item.wildercord.spell_scroll": "Spell Scroll",
    "item.wildercord.spell_scroll.named": "Scroll of %s",
    "item.wildercord.torn_page": "Torn Page",
    "item.wildercord.training_dummy": "Training Dummy",
    "block.wildercord.wellstone": "Wellstone",
    "block.wildercord.rune_seal": "Rune Seal",
    "block.wildercord.archive_lectern": "Archive Lectern",
    "entity.wildercord.archivist": "The Archivist",
    "entity.wildercord.training_dummy": "Training Dummy",
    "entity.wildercord.training_dummy.dps": "DPS %s · %s total",
    "entity.wildercord.afterimage": "%s's Afterimage",
    "tooltip.wildercord.scroll_blank": "Inscribe a spell from the Cord screen",
    "tooltip.wildercord.scroll_author": "Inscribed by %s",
    "tooltip.wildercord.scroll_use": "Right-click to cast it once. No Cord needed",
    "tooltip.wildercord.torn_page": "Right-click to read the riddle of a secret spell",
    "tooltip.wildercord.training_dummy": "Place it and try your spells: it shows every hit and your damage per second. Sneak and punch it to pick it up",
    "category.wildercord.effect.innate": "Innate",
    # Casting
    "message.wildercord.overcast_prompt": "Not enough mana (%s/%s). Cast again to overcast: your %s Circle cracks for 3 minutes",
    "message.wildercord.overcast": "Overcast! Your %s Circle cracks. It mends in %s minutes",
    "message.wildercord.mended": "Your cracked circles have mended.",
    "message.wildercord.rhythm": "\u266A Rhythm x%s (+%s%% power)",
    "message.wildercord.charge_fizzled": "The charge fizzles",
    "message.wildercord.interrupted": "Your spell is cut off",
    "message.wildercord.leaning": "Your magic leans toward %s, your deepest affinity: your circles take its colour.",
    "message.wildercord.no_targets": "Nothing to strike nearby",
    "message.wildercord.reborn": "Reborn in flame!",
    "message.wildercord.innate": "Your innate rune awakens: %s. It's in your Codex, and it grows with every circle.",
    "message.wildercord.innate_item": "An innate rune can't be learned from an item",
    "message.wildercord.debt_forgiven": "Borrowed time repaid in full",
    "message.wildercord.nothing_borrowed": "No hurt to borrow back",
    "message.wildercord.borrowed": "Borrowed %s health. Slay something, or it comes back",
    "message.wildercord.nothing_to_mirror": "No spell has hit you lately",
    "title.wildercord.secret": "A secret spell",
    "title.wildercord.innate": "Innate rune awakened",
    "reaction.wildercord.ignite": "Ignite!",
    "reaction.wildercord.collision": "Collision!",
    "reaction.wildercord.unison": "Unison!",
    "reaction.wildercord.shatter.desc": "Fire on a frozen target: +60% damage, and the ice bursts.",
    "reaction.wildercord.conduct.desc": "Storm on a wet target (in water or rain, or still dripping): +50% damage, arcing to two more.",
    "reaction.wildercord.wildfire.desc": "Fire on a target just thrown by wind: flames spread to everything around it.",
    "reaction.wildercord.implode.desc": "A blast where enemies were just pulled together: 50% wider, 30% harder.",
    "reaction.wildercord.collapse.desc": "Repel on enemies just pulled in: double damage.",
    "message.wildercord.domain_clash": "Domain clash!",
    "message.wildercord.domain_holds": "Your domain holds",
    "message.wildercord.domain_shattered": "Your domain shatters",
    "chat.wildercord.spell_cost": "%s mana · %ss cooldown",
    "chat.wildercord.spell_copy": "Click to copy the code, then paste it in the Cord screen",
    # Ley lines and the Wellstone
    "message.wildercord.ley_first": "You stand on a ley line, where the world's mana runs close to the surface. Mana flows twice as fast here, and circles form twice as quickly. A Wellstone set on one becomes a well for everyone near it.",
    "message.wildercord.ley_on": "A ley line runs beneath you",
    "screen.wildercord.mana.regen_ley": "+%s%% on a ley line",
    "screen.wildercord.mana.regen_well": "+%s%% from a Wellstone nearby",
    "screen.wildercord.mana.way.ley": "Stand on a ley line (Cord-wearers see its violet motes), or set a Wellstone on one",
    # The Archive
    "message.wildercord.archivist_wakes": "Pages stir. The Archivist rises from its lectern.",
    "message.wildercord.archivist_rewrites.2": "The Archivist tears out a page and rewrites its Cord.",
    "message.wildercord.archivist_rewrites.3": "The Archivist burns its last pages. Its Cord blazes with new runes.",
    "boss.wildercord.archivist_casting": "%s \u00B7 casting %s",
    "boss.wildercord.archivist_rewriting": "%s \u00B7 rewriting its Cord",
    "message.wildercord.page_nothing": "Nothing on this page you don't already know",
    "message.wildercord.page_read": "The page is torn, but a riddle survives:",
    "message.wildercord.page_map": "In the margin, a map: an Archive lies about %s blocks to the %s.",
    "direction.wildercord.north": "north", "direction.wildercord.northeast": "northeast", "direction.wildercord.east": "east",
    "direction.wildercord.southeast": "southeast", "direction.wildercord.south": "south", "direction.wildercord.southwest": "southwest",
    "direction.wildercord.west": "west", "direction.wildercord.northwest": "northwest",
    # Scrolls and codes
    "message.wildercord.scroll_needs": "Inscribing a scroll takes paper and an ink sac",
    "message.wildercord.scroll_mana": "Inscribing costs twice the spell's mana (%s/%s)",
    "message.wildercord.inscribed": "Inscribed: %s",
    "message.wildercord.code_copied": "Copied %s",
    "message.wildercord.code_none": "No spell code (wc:...) on the clipboard",
    "message.wildercord.code_loaded": "Spell loaded from the code",
    "message.wildercord.code_partial": "Loaded, but %s rune(s) left out: not known, too strong, or no socket free",
    # Toasts, the wheel and the Cord screen
    "toast.wildercord.grimoire": "New in your Grimoire",
    "toast.wildercord.riddle": "A riddle, found",
    "toast.wildercord.riddle_hint": "See the Grimoire",
    "screen.wildercord.wheel": "Spell wheel",
    "screen.wildercord.wheel.empty": "(empty)",
    "screen.wildercord.page.grimoire": "Grimoire",
    "screen.wildercord.tool.rename": "Rename",
    "screen.wildercord.tool.rename.hint": "Give this spell a name. Enter saves; an empty name goes back to the automatic one",
    "screen.wildercord.tool.copy": "Copy spell code",
    "screen.wildercord.tool.copy.hint": "Paste the code in chat and everyone can read the spell",
    "screen.wildercord.tool.paste": "Paste spell code",
    "screen.wildercord.tool.paste.hint": "Load a copied code into this spell (runes you know, that this Cord holds)",
    "screen.wildercord.tool.scroll": "Inscribe a scroll",
    "screen.wildercord.tool.scroll.hint": "Write this spell onto a scroll anyone can cast once. Takes paper, an ink sac and twice the mana",
    "screen.wildercord.secret_line": "Secret spell: %s",
    "screen.wildercord.grimoire.heart": "Your heart",
    "screen.wildercord.grimoire.innate": "Innate rune: %s",
    "screen.wildercord.grimoire.innate_none": "Innate rune: wakes at the 1st Circle",
    "screen.wildercord.grimoire.leaning_none": "Leaning: none yet (one affinity at I or more, clearly ahead of the rest)",
    "screen.wildercord.grimoire.leaning": "Leaning: %s (your deepest affinity)",
    "screen.wildercord.grimoire.leaning_hint": "Your magic leans toward your deepest affinity once it reaches I and leads the next by a quarter: your Heart Circles and charging circles take its colour. The power comes from the affinity itself.",
    "screen.wildercord.grimoire.reactions": "Reactions (%s of %s)",
    "screen.wildercord.grimoire.secrets": "Secret spells (%s of %s)",
    "screen.wildercord.grimoire.secret_unknown": "Some exact rune sequences are secret spells. Torn Pages hold their riddles.",
    "screen.wildercord.grimoire.feats": "Feats (%s of %s)",
    "screen.wildercord.heart.cracked": "Cracked by overcasting: %s circle(s), mending in %s",
    "screen.wildercord.heart.innate": "Innate rune: %s (+%s%% from your circles)",
    "screen.wildercord.heart.leaning": "Leaning: %s (affinity %s)",
    "screen.wildercord.heart.need.reactions": "Set off %s different reactions (%s)",
    "screen.wildercord.heart.need.runebound": "Slay %s Runebound (%s)",
    "screen.wildercord.heart.need.secrets": "Find %s secret spells (%s)",
    "screen.wildercord.heart.need.feat": "%s: %s",
    # World events: mana storms, fallen stars and rift sieges (cast/events)
    "message.wildercord.storm_start": "A mana storm rages overhead: mana flows twice as fast, spells cost a quarter less, and any may surge",
    "message.wildercord.storm_left": "You leave the mana storm",
    "message.wildercord.storm_end": "The mana storm passes",
    "message.wildercord.surge.bigger": "Surge! Your spell swells with the storm's mana",
    "message.wildercord.surge.echo": "Surge! Your spell echoes",
    "message.wildercord.surge.element": "Surge! A stray spark of %s rides along",
    "message.wildercord.surge.backfire": "Surge! The storm's mana kicks back",
    "message.wildercord.storm_rune": "The storm's mana crystallises in your hands: %s",
    "message.wildercord.star_falls": "A star falls to the %s!",
    "message.wildercord.star_guarded": "Runebound rise to guard the Fallen Star",
    "message.wildercord.star_held": "The star's guardians still stand (%s)",
    "message.wildercord.star_looted": "The Fallen Star breaks open and crumbles to dust",
    "message.wildercord.star_peaceful": "The Fallen Star won't open while the world is at peace",
    "message.wildercord.rift_opens": "A rift tears open to the %s!",
    "message.wildercord.rift_wave": "Wave %s of %s pours out of the rift",
    "message.wildercord.rift_last_wave": "The last wave pours out of the rift, and something worse with it",
    "message.wildercord.riftcaller": "The Riftcaller steps through",
    "message.wildercord.rift_struck": "%s strikes the rift (%s of %s elements)",
    "message.wildercord.rift_raw": "The rift is too raw to seal yet: hold it until the second wave",
    "message.wildercord.rift_sealed": "The rift is sealed!",
    "message.wildercord.rift_won": "The rift collapses: every wave is beaten!",
    "message.wildercord.rift_fades": "The rift closes on its own, and takes its monsters with it",
    "boss.wildercord.rift": "Rift siege \u00B7 wave %s of %s",
    "entity.wildercord.riftcaller": "Riftcaller",
    "block.wildercord.fallen_star": "Fallen Star",
    "command.wildercord.event.no_ley": "No ley line within 48 blocks (add 'here' to start one where you stand)",
    "command.wildercord.event.storm": "A mana storm gathers for %s seconds",
    "command.wildercord.event.no_room": "There's no room for that nearby",
    "command.wildercord.event.star": "A star is falling: it lands at %s %s %s",
    "command.wildercord.event.peaceful": "Rift sieges and fallen stars need a difficulty above Peaceful",
    "command.wildercord.event.rift_open": "A rift is already open in this world",
    "command.wildercord.event.rift": "A rift tears open",
    "command.wildercord.event.off": "World events are switched off in the server's config (features.world_events)",
    "command.wildercord.event.overworld": "World events only happen in the Overworld",
    "subtitles.wildercord.storm_start": "Mana storm gathers",
    "subtitles.wildercord.storm_end": "Mana storm passes",
    "subtitles.wildercord.storm_arc": "Ley line crackles",
    "subtitles.wildercord.surge": "Spell surges",
    "subtitles.wildercord.star_fall": "Star falls",
    "subtitles.wildercord.star_impact": "Star lands",
    "subtitles.wildercord.rift_open": "Rift tears open",
    "subtitles.wildercord.rift_close": "Rift seals",
    "subtitles.wildercord.rift_wave": "Monsters pour from a rift",
    # Sound subtitles (the sounds and sounds.json come from tools/sound_art.py)
    "subtitles.wildercord.cast_fire": "Fire spell cast", "subtitles.wildercord.impact_fire": "Fire spell hits",
    "subtitles.wildercord.cast_frost": "Frost spell cast", "subtitles.wildercord.impact_frost": "Frost spell hits",
    "subtitles.wildercord.cast_storm": "Storm spell cast", "subtitles.wildercord.impact_storm": "Storm spell hits",
    "subtitles.wildercord.cast_wind": "Wind spell cast", "subtitles.wildercord.impact_wind": "Wind spell hits",
    "subtitles.wildercord.cast_earth": "Earth spell cast", "subtitles.wildercord.impact_earth": "Earth spell hits",
    "subtitles.wildercord.cast_life": "Life spell cast", "subtitles.wildercord.impact_life": "Life spell hits",
    "subtitles.wildercord.cast_void": "Void spell cast", "subtitles.wildercord.impact_void": "Void spell hits",
    "subtitles.wildercord.cast_arcane": "Arcane spell cast", "subtitles.wildercord.impact_arcane": "Arcane spell hits",
    "subtitles.wildercord.cast_time": "Time spell cast", "subtitles.wildercord.impact_time": "Time spell hits",
    "subtitles.wildercord.cast_blood": "Blood spell cast", "subtitles.wildercord.impact_blood": "Blood spell hits",
    "subtitles.wildercord.charge_loop": "Spell charges",
    "subtitles.wildercord.charge_full": "Spell fully charged",
    "subtitles.wildercord.release": "Charged spell released",
    "subtitles.wildercord.circle_open": "Magic circle opens",
    "subtitles.wildercord.beam_fire": "Beam fires",
    "subtitles.wildercord.orb_hum": "Orb hums",
    "subtitles.wildercord.kit.cast": "Spell is cast",
    "subtitles.wildercord.kit.hit": "Spell strikes",
    "subtitles.wildercord.kit.field": "Magic hums",
    "subtitles.wildercord.kit.tell": "Rune answers",
    "subtitles.wildercord.shield_up": "Shield rises",
    "subtitles.wildercord.shield_break": "Shield shatters",
    "subtitles.wildercord.shield_block": "Spell rings off a shield",
    "subtitles.wildercord.imbue": "Magic is imbued",
    "subtitles.wildercord.domain_open": "Domain opens",
    "subtitles.wildercord.domain_close": "Domain collapses",
    "subtitles.wildercord.blink": "Caster blinks",
    "subtitles.wildercord.magic_break": "Spell breaks a block",
    "subtitles.wildercord.rune_thread": "Rune threaded",
    "subtitles.wildercord.rune_unthread": "Rune unthreaded",
    "subtitles.wildercord.wheel_open": "Spell wheel opens",
    "subtitles.wildercord.wheel_hover": "Spell wheel ticks",
    "subtitles.wildercord.wheel_select": "Spell chosen",
    "subtitles.wildercord.discovery": "Grimoire discovery",
    "subtitles.wildercord.circle_formed": "Heart Circle forms",
    "subtitles.wildercord.overcast": "Heart Circle cracks",
    # The Runesmith and its contracts.
    "block.wildercord.scribing_desk": "Scribing Desk",
    "entity.wildercord.villager.runesmith": "Runesmith",
    "contract.wildercord.runebound": "Defeat %s Runebound with %s spells",
    "contract.wildercord.reaction": "Set off %s %s reactions",
    "contract.wildercord.ley": "Cast %s spells on a ley line",
    "contract.wildercord.imbue": "Imbue a weapon and use all its charges",
    "contract.wildercord.spell_kills": "Slay %s monsters with spells",
    "contract.wildercord.element_casts": "Cast %s %s spells",
    "contract.wildercord.reaction.shatter": "Shatter",
    "contract.wildercord.reaction.conduct": "Conduct",
    "contract.wildercord.reaction.wildfire": "Wildfire",
    "contract.wildercord.reaction.implode": "Implode",
    "contract.wildercord.reaction.collapse": "Collapse",
    "message.wildercord.contract_board": "Today's contracts",
    "screen.wildercord.contracts": "Today's Contracts",
    "screen.wildercord.grimoire.duels": "Duels",
    "screen.wildercord.grimoire.duel_record": "%s won, %s lost",
    "screen.wildercord.grimoire.duel_hint": "Challenge another caster with /duel <player>. Nobody dies in a duel.",
    "screen.wildercord.contracts.refresh": "New ones at dawn, in %s",
    "screen.wildercord.contracts.refresh_still": "New ones at the next dawn",
    "screen.wildercord.contracts.none": "No contracts today",
    "screen.wildercord.contracts.handed_in": "Handed in:",
    "screen.wildercord.contracts.hint": "Finish one, then come back to the desk to hand it in.",
    "screen.wildercord.contracts.claimed": "Handed in",
    "screen.wildercord.contracts.ready": "Done: hand it in at the desk",
    "screen.wildercord.contracts.reward": "Reward",
    "message.wildercord.contract_progress": "%s: %s/%s",
    "message.wildercord.contract_done": "Contract complete: %s. Hand it in at a Scribing Desk",
    "message.wildercord.contract_reward": "Reward: %s",
    "message.wildercord.contract_reward_rune": "a Tier %s rune",
    "message.wildercord.contract_rune": "Your contract pays a %s rune",
    "message.wildercord.contract_refresh": "New contracts every dawn. Finished ones are handed in here",
    # Duels.
    "message.wildercord.duel_disabled": "Duels are turned off here",
    "message.wildercord.duel_self": "You can't duel yourself",
    "message.wildercord.duel_busy": "One of you is already in a duel",
    "message.wildercord.duel_challenged": "%s challenges you to a duel!",
    "message.wildercord.duel_accept": "[Accept]",
    "message.wildercord.duel_decline": "[Decline]",
    "message.wildercord.duel_accept_hover": "Duel %s",
    "message.wildercord.duel_decline_hover": "Turn the challenge down",
    "message.wildercord.duel_sent": "You challenged %s to a duel. They have 30 seconds to answer",
    "message.wildercord.duel_no_challenge": "%s hasn't challenged you, or the challenge ran out",
    "message.wildercord.duel_declined": "%s declined your duel",
    "message.wildercord.duel_you_declined": "You declined %s's duel",
    "message.wildercord.duel_too_far": "You must be within 40 blocks of %s to duel",
    "message.wildercord.duel_not_alive": "%s can't duel right now",
    "message.wildercord.duel_begins": "Duel with %s! Nobody dies, and afterwards the harm you did each other is undone. Stay within 40 blocks",
    "message.wildercord.duel_hurt": "%s was hurt too recently to start a duel",
    "message.wildercord.duel_pvp": "%s was fighting another player too recently to start a duel",
    "message.wildercord.duel_cooldown": "%s duelled too recently: wait a little before the next one",
    "message.wildercord.duel_wait": "Wait a few seconds before challenging again",
    "message.wildercord.duel_interrupted": "The duel was called off: someone else joined the fight",
    "message.wildercord.duel_fight": "Fight!",
    "message.wildercord.duel_victory": "Victory!",
    "message.wildercord.duel_defeat": "Defeated",
    "message.wildercord.duel_draw": "The duel ended in a draw",
    "message.wildercord.duel_result.knockout": "%s defeated %s in a duel",
    "message.wildercord.duel_result.left_area": "%s won the duel: %s left the arena",
    "message.wildercord.duel_result.logged_off": "%s won the duel: %s left the game",
    "message.wildercord.duel_result.died": "%s won the duel: %s fell to something else",
    "message.wildercord.duel_stats": "%s: %s wins, %s losses in duels",
    "gamerule.wildercord.allow_duels": "Allow duels",
    "gamerule.wildercord.allow_duels.description": "Whether players can challenge each other with /duel.",
    # Chorus casting.
    "reaction.wildercord.chorus": "Chorus!",
}

# Parrying with a Shield, and wild magic on an overcast (cast/Shields, cast/WildSurge, spell/WildMagic).
PARRY_AND_WILD_LANG = {
    "message.wildercord.parried": "Parried!",
    "message.wildercord.parried_you": "Your spell was parried!",
    "subtitles.wildercord.shield_parry": "Spell parried",
    "message.wildercord.wild_surge": "Wild magic! %s",
    "message.wildercord.surge.twice": "The spell goes off twice",
    "message.wildercord.surge.element": "The spell turns to %s",
    "message.wildercord.surge.grand": "The spell swells to twice its size",
    "message.wildercord.surge.butterflies": "The spell bursts into butterflies of light",
    "message.wildercord.surge.heal_all": "The spell heals everyone near instead",
    "message.wildercord.surge.blink": "The spell flings you aside",
    "message.wildercord.surge.wisps": "Wisps pour out with the spell",
    "message.wildercord.surge.levitate": "Gravity flips around you",
    "message.wildercord.surge.stray": "The spell goes off at %s",
    "message.wildercord.surge.slow_time": "Time slows around you",
    "message.wildercord.surge.backfire": "The spell backfires",
    "message.wildercord.surge.free_recast": "Your next spell within 3 seconds is free",
    "message.wildercord.surge.free_used": "Free recast!",
    "message.wildercord.surge.ward": "A Shield of light settles on you",
}

# ---------------------------------------------------------------- familiars and Cord cosmetics

FAMILIAR_LANG = {
    "entity.wildercord.wisp": "Wisp",
    "entity.wildercord.wisp.familiar": "%s's %s Wisp",
    "entity.wildercord.wisp.of": "%s Wisp",
    "item.wildercord.wisp_lantern": "Wisp Lantern",
    "tooltip.wildercord.wisp_lantern": "Use: send your familiar home, or call it out again. Sneak-use: call the next",
    "tooltip.wildercord.wisp_lantern.empty": "No familiars yet: strike a wild wisp with magic of its own element to bond with it",
    "tooltip.wildercord.wisp_lantern.bond": "%s (%s, %s): %s",
    "tooltip.wildercord.wisp_lantern.level": "level %s, %s/%s",
    "tooltip.wildercord.wisp_lantern.level_max": "level %s",
    "tooltip.wildercord.wisp_lantern.out": "out with you",
    "tooltip.wildercord.wisp_lantern.waiting": "waiting at a Wellstone",
    "tooltip.wildercord.wisp_lantern.resting": "in the lantern",
    "message.wildercord.wisp.drinks": "The %s Wisp drinks your magic (%s of %s)",
    "message.wildercord.wisp.spooked": "The wisp shies from %s magic: it answers only to %s",
    "message.wildercord.wisp.full": "You already keep %s familiars",
    "message.wildercord.wisp.not_yours": "Only its owner can name a familiar",
    "message.wildercord.wisp.bonded": "A %s Wisp bonds with you: it's your familiar now",
    "message.wildercord.wisp.bonded_hint": "It floats at your shoulder, quickens your mana and helps in its own small way. Sneak and use it to tell it to stay or follow (near an awake Wellstone it will wait there); a Wisp Lantern calls out your others. A name tag names it.",
    "message.wildercord.wisp.home": "%s returns to your lantern",
    "message.wildercord.wisp.out": "%s comes out (level %s)",
    "message.wildercord.wisp.waiting": "%s will wait at the Wellstone",
    "message.wildercord.wisp.stay": "%s stays here",
    "message.wildercord.wisp.follow": "%s follows you",
    "message.wildercord.wisp.level": "%s grows to level %s: +%s%% mana regeneration, a spell every %s seconds",
    "message.wildercord.wisp.lantern_empty": "Your lantern is empty: bond with a wild wisp first",
    "message.wildercord.cosmetic.locked": "That style isn't yours yet",
    "message.wildercord.cosmetic.need": "Needs %s %s",
    "message.wildercord.cosmetic.bought": "Unlocked: %s",
    "screen.wildercord.page.cosmetics": "Cosmetics",
    "screen.wildercord.cosmetics.material": "Beads",
    "screen.wildercord.cosmetics.glow": "Glow",
    "screen.wildercord.cosmetics.trail": "Cast trail",
    "screen.wildercord.cosmetics.unlocked": "Yours: click to wear it",
    "screen.wildercord.cosmetics.need_circle": "Unlocks at the %s Heart Circle",
    "screen.wildercord.cosmetics.need_feat": "Unlocks with the feat %s: %s",
    "screen.wildercord.cosmetics.need_boss": "Unlocks when you help slay a boss",
    "screen.wildercord.cosmetics.buy": "Click to buy: %s %s (you have %s)",
    "screen.wildercord.cosmetics.wearing": "Your Cord",
    "screen.wildercord.cosmetics.worn.material": "Beads: %s",
    "screen.wildercord.cosmetics.worn.glow": "Glow: %s",
    "screen.wildercord.cosmetics.worn.trail": "Cast trail: %s",
    "screen.wildercord.cosmetics.hint": "Point at an option to see it on your wrist. Everyone near you sees your Cord as you style it.",
    "cosmetic.wildercord.material.glass": "Glass",
    "cosmetic.wildercord.material.glass.desc": "Clear glass beads, lit through by each rune's colour.",
    "cosmetic.wildercord.material.gold": "Gold",
    "cosmetic.wildercord.material.gold.desc": "Polished gold beads with a warm light at their hearts.",
    "cosmetic.wildercord.material.obsidian": "Obsidian",
    "cosmetic.wildercord.material.obsidian.desc": "Beads of volcanic glass, dark as night, their light burning brightest against it.",
    "cosmetic.wildercord.material.amethyst": "Amethyst",
    "cosmetic.wildercord.material.amethyst.desc": "Faceted amethyst beads that catch the light.",
    "cosmetic.wildercord.material.bone": "Bone",
    "cosmetic.wildercord.material.bone.desc": "Carved bone beads, a trophy from a Runebound.",
    "cosmetic.wildercord.material.prismarine": "Prismarine",
    "cosmetic.wildercord.material.prismarine.desc": "Sea-green prismarine beads, glowing like deep water.",
    "cosmetic.wildercord.trail.none": "None",
    "cosmetic.wildercord.trail.none.desc": "Your casts leave nothing behind.",
    "cosmetic.wildercord.trail.sparks": "Sparks",
    "cosmetic.wildercord.trail.sparks.desc": "Crackling sparks leap from your hand as you cast.",
    "cosmetic.wildercord.trail.petals": "Petals",
    "cosmetic.wildercord.trail.petals.desc": "Blossom petals drift from your hand as you cast.",
    "cosmetic.wildercord.trail.snow": "Snow",
    "cosmetic.wildercord.trail.snow.desc": "Snowflakes swirl from your hand as you cast.",
    "cosmetic.wildercord.trail.embers": "Embers",
    "cosmetic.wildercord.trail.embers.desc": "Embers rise from your hand as you cast.",
    "cosmetic.wildercord.trail.stars": "Stars",
    "cosmetic.wildercord.trail.stars.desc": "Motes of starlight stream from your hand as you cast.",
    "cosmetic.wildercord.glow.spell": "Your spells' colours",
    "cosmetic.wildercord.glow.spell.desc": "Each bead glows in the colour of its rune.",
    "subtitles.wildercord.wisp_ambient": "Wisp chimes",
    "subtitles.wildercord.wisp_chime": "Wisp drinks magic",
    "subtitles.wildercord.wisp_bond": "Wisp bonds",
    "subtitles.wildercord.wisp_cast": "Familiar casts",
    "subtitles.wildercord.wisp_level": "Familiar grows stronger",
}

GLOW_DYES = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue",
             "brown", "green", "red", "black"]


def familiar_lang():
    lang = dict(FAMILIAR_LANG)
    for dye in GLOW_DYES:
        name = dye.replace("_", " ")
        lang[f"cosmetic.wildercord.glow.{dye}"] = name.capitalize()
        lang[f"cosmetic.wildercord.glow.{dye}.desc"] = f"Every bead glows {name}, whatever its rune."
    return lang


def write_familiar_content():
    import familiar_art  # The wisp, the Wisp Lantern, the beads' materials and the Cosmetics page's icons.
    familiar_art.main()
    write_json(DATA / "recipe/wisp_lantern.json", {
        "type": "minecraft:crafting_shaped", "category": "misc",
        "key": {"A": "minecraft:amethyst_shard", "G": "minecraft:glass_pane", "L": "minecraft:lantern"},
        "pattern": [" A ", "GLG", " A "], "result": {"id": "wildercord:wisp_lantern"}})
    unlock_advancement("wildercord:wisp_lantern", "minecraft:lantern")


# The dimension dungeons (see tools/dungeon_assets.py and docs/features/dungeons.md).
DUNGEON_LANG = {
    "block.wildercord.dungeon_altar": "Dungeon Altar",
    "boss.wildercord.casting": "%s \u00B7 casting %s",
    "boss.wildercord.status": "%s \u00B7 %s",
    # The Ember Sanctum and the Cinder Warden
    "entity.wildercord.cinder_warden": "The Cinder Warden",
    "item.wildercord.cinder_heart": "Cinder Heart",
    "item.wildercord.cinder_heart.lore": "Still warm. It beat in the Cinder Warden's chest.",
    "item.wildercord.cinder_heart.use": "Hold it up: fire can't touch you for 3 minutes (then it rests for 5)",
    "message.wildercord.cinder_warden_wakes": "The coals stir. The Cinder Warden climbs out of its forge.",
    "message.wildercord.cinder_warden_phase.2": "The Cinder Warden stokes its forge, and its keepers climb out of the fire.",
    "message.wildercord.cinder_warden_phase.3": "The Cinder Warden's core roars white-hot. Its keepers return, stronger.",
    "message.wildercord.cinder_warden_immune": "Its armour turns that aside. Only a reaction breaks through: freeze it, then burn it",
    "boss.wildercord.shifting.cinder_warden": "%s \u00B7 stoking its forge",
    "boss.wildercord.cinder_warden_cracked": "cracked open!",
    # The Astral Observatory and the Star-Eater
    "entity.wildercord.star_eater": "The Star-Eater",
    "item.wildercord.astral_lens": "Astral Lens",
    "item.wildercord.astral_lens.lore": "The Star-Eater grew it round its eye. The dark looks back through it.",
    "item.wildercord.astral_lens.use": "Hold it up: see in the dark for 3 minutes and fall like starlight for 1 (then it rests for 5)",
    "message.wildercord.star_eater_wakes": "The stars in the floor go dark. The Star-Eater opens its eye.",
    "message.wildercord.star_eater_phase.2": "The Star-Eater swallows the light. Its shield grows back stronger.",
    "message.wildercord.star_eater_phase.3": "The Star-Eater eclipses. Its shield is harder than ever.",
    "message.wildercord.star_eater_reflects": "Turned back! Its shield stops any spell of %s mana or less: cast one that costs more, or knock its star shards back into it",
    "message.wildercord.star_eater_open": "The Star-Eater's shield shatters. Strike now!",
    "boss.wildercord.shifting.star_eater": "%s \u00B7 eclipsing",
    "boss.wildercord.star_eater_shielded": "shield up (%s mana)",
    "boss.wildercord.star_eater_open": "shield broken!",
    "boss.wildercord.star_eater_volley": "loosing star shards",
    # The Drowned Scriptorium and the Tide Scribe
    "entity.wildercord.tide_scribe": "The Tide Scribe",
    "block.wildercord.clockwork_control": "Clockwork Control",
    "block.wildercord.greenhouse_heart": "Greenhouse Heart",
    "block.wildercord.sky_anchor": "Sky Anchor",
    "message.wildercord.clock_paused": "The mechanism pauses. Stored attacks release after the warning circle.",
    "screen.wildercord.altar.preview_mana": "Base effect mana: %s (shape, rank and modifiers change the final cost)",
    "screen.wildercord.altar.preview_material": "Formation ingredient: %s",
    "message.wildercord.library_name": "Give your build a name.",
    "screen.wildercord.grimoire.woven": "Exact woven pairs learned: %s",
    "screen.wildercord.grimoire.woven_how": "Fuse any two exact effects with an Amethyst Block and 3 XP levels. Both effects share one socket; their combined mana cost still applies.",
    "item.wildercord.keepers_hourglass": "Keeper's Hourglass",
    "item.wildercord.keepers_hourglass.desc": "Off-hand: spell duration +30%, spell power -15%.",
    "item.wildercord.living_seedpod": "Living Seedpod",
    "item.wildercord.living_seedpod.desc": "Off-hand: mana cost -10%, spell power -15%, cooldown +10%.",
    "item.wildercord.sky_feather": "Sky Feather",
    "item.wildercord.sky_feather.desc": "Off-hand: cooldown -15%, mana cost +15%.",
    "screen.wildercord.profile.performance": "Performance",
    "screen.wildercord.profile.balanced": "Balanced",
    "screen.wildercord.profile.cinematic": "Cinematic",
    "screen.wildercord.profile.benchmark": "Benchmark this scene for 30 seconds",
    "message.wildercord.frame_start": "Frame sampling started. Run the same scene for 30 seconds; pausing or changing worlds cancels it. Results appear in chat and logs.",
    "trial.wildercord.precision": "Precision: five casts and five dummy hits, each at most 6 damage",
    "trial.wildercord.variety": "Variety: four different shapes and five dummy hits",
    "trial.wildercord.fusion": "Fusion: three different fusions and five dummy hits",
    "message.wildercord.trial_start_hint": "Enter the practice dimension in Survival to start precision, variety or fusion trials.",
    "message.wildercord.trial_started": "%s. You have 90 seconds and a total spell budget of 100 mana.",
    "message.wildercord.trial_ended": "Trial ended: time expired, you left the arena or you fell. Try again any time.",
    "message.wildercord.trial_precision_failed": "Precision trial ended: a hit exceeded 6 damage.",
    "message.wildercord.trial_mana_failed": "Trial ended: you spent more than 100 mana.",
    "message.wildercord.trial_won": "Trial complete: %s · %s mana. The first completion awards a Torn Page.",
    "message.wildercord.aftermath": "You discover a %s echo and receive its temporary boon.",
    "aftermath.wildercord.star": "fallen star",
    "aftermath.wildercord.rift": "sealed rift",
    "aftermath.wildercord.storm": "mana storm",
    "block.wildercord.runic_hearth": "Runic Hearth",
    "project.wildercord.lantern": "Reading Lantern (charge with fire or arcane)",
    "project.wildercord.garden": "Bloom Planter (charge with life)",
    "project.wildercord.chime": "Ward Chime (charge with wind)",
    "project.wildercord.ritual": "Threefold Ritual",
    "message.wildercord.hearth_owner": "The hearth's owner configures its project. Teammates may charge it.",
    "message.wildercord.hearth_status": "%s · %s / 3 charges. Configure with amethyst, wheat, a feather or an ender pearl; use a book for research.",
    "message.wildercord.hearth_ritual": "Ritual begun: cast three different elements at the hearth. One player finishes in 20 seconds; two contributing teammates finish in 10. Remain nearby.",
    "message.wildercord.hearth_complete": "The threefold ritual blooms! Participants regenerate, and the owner receives a Torn Page.",
    "screen.wildercord.notebook": "Rune research & build library",
    "screen.wildercord.notebook.name": "Build name",
    "screen.wildercord.notebook.slot": "Spell slot %s",
    "screen.wildercord.notebook.save": "Save slot",
    "screen.wildercord.notebook.load": "Load build",
    "screen.wildercord.notebook.delete": "Delete build",
    "screen.wildercord.notebook.build": "%s · %s runes",
    "screen.wildercord.notebook.shapes": "Try five spell shapes: %s / 5 · Torn Page",
    "screen.wildercord.notebook.fusions": "Cast three fusions: %s / 3 · two Blank Runes",
    "screen.wildercord.notebook.garden": "Cleanse a greenhouse heart: %s · Mana Crystal",
    "screen.wildercord.notebook.saved": "Saved builds: %s / 24 · select a name below",
    "screen.wildercord.notebook.done": "Complete",
    "screen.wildercord.notebook.pending": "Pending",
    "screen.wildercord.notebook.hint": "Fusion hint",
    "message.wildercord.control_lock": "Casting sealed briefly. Repeated seals cannot extend it; a recovery window follows.",
    "message.wildercord.familiar_role": "Familiar job: %s. Its job replaces elemental assistance; choose Companion to restore it.",
    "role.wildercord.companion": "Companion",
    "role.wildercord.scout": "Scout",
    "role.wildercord.guardian": "Guardian",
    "role.wildercord.gardener": "Gardener",
    "message.wildercord.familiar_harvest": "Your familiar has found ripe crops nearby.",
    "message.wildercord.library_empty": "That spell slot is empty.",
    "message.wildercord.library_full": "Your library holds 24 builds. Delete one before adding another.",
    "message.wildercord.library_missing": "No build with that name is saved.",
    "message.wildercord.library_locked": "That spell slot needs a stronger Cord or the Fifth Page tome.",
    "message.wildercord.library_fit": "That build exceeds this Cord's sockets or rune tier.",
    "message.wildercord.library_unknown": "Learn every rune in that build before loading it.",
    "message.wildercord.library_saved": "Saved build: %s.",
    "message.wildercord.library_loaded": "Loaded %s into spell slot %s.",
    "message.wildercord.library_deleted": "Deleted build: %s.",
    "message.wildercord.library_list": "Spell library: %s / 24 builds.",
    "message.wildercord.library_row": "%s · %s runes",
    "message.wildercord.research_shapes": "Five different shapes tested! Research awards a Torn Page.",
    "message.wildercord.research_fusions": "Three different fusions tested! Research awards two Blank Runes.",
    "message.wildercord.research_garden": "Living Greenhouse restored! Research awards a Mana Crystal.",
    "message.wildercord.research_board": "Research notebook: shapes %s / 5 · fusions %s / 3 · garden %s",
    "message.wildercord.research_hint": "Experiment: fuse %s with %s using an Amethyst Shard and %s XP levels.",
    "message.wildercord.clock_stored": "Clockwork stores %s damage. Use the control to release it into the gallery.",
    "message.wildercord.garden_hint": "Life magic or bone meal restores this heart. Shears harvest it instead. Choose once.",
    "message.wildercord.garden_growth": "The heart recovers: %s / 3.",
    "message.wildercord.garden_cleanse": "The heart is cleansed. A moss path grows across the garden.",
    "message.wildercord.garden_harvest": "You harvest the heart's stored mana. Its path remains broken.",
    "message.wildercord.sky_shift": "The stepping stones shift. Wind magic can turn the anchor too.",
    "message.wildercord.sky_blocked": "The stepping stones cannot shift: clear their next landing spaces.",
    "entity.wildercord.root_guardian": "The Root Guardian",
    "entity.wildercord.storm_conductor": "The Storm Conductor",
    "message.wildercord.root_guardian_wakes": "The heartwood stirs. Prune its three bindings with shears, or burn them away!",
    "message.wildercord.root_pruned": "Root bindings remaining: %s",
    "message.wildercord.root_guardian_phase": "Fresh roots wrap the guardian's heart.",
    "message.wildercord.storm_conductor_wakes": "The copper coils sing. Stand beside the sparking arena rod to ground the charge!",
    "message.wildercord.storm_grounded": "Charge grounded! The conductor's core is exposed.",
    "message.wildercord.storm_conductor_phase": "The charge shifts. Find the next sparking rod!",
    "boss.wildercord.root_open": "heart exposed!",
    "boss.wildercord.root_bound": "%s root bindings - prune or burn",
    "boss.wildercord.storm_grounded": "grounded - core exposed!",
    "boss.wildercord.storm_charged": "charged - seek the sparking rod",
    "boss.wildercord.shifting.root_guardian": "%s · growing fresh roots",
    "boss.wildercord.shifting.storm_conductor": "%s · rerouting the charge",
    "item.wildercord.drowned_quill": "Drowned Quill",
    "item.wildercord.drowned_quill.lore": "The Tide Scribe's quill. It has never once been dry.",
    "item.wildercord.drowned_quill.use": "Hold it up: breathe and swim like the drowned for 3 minutes (then it rests for 5)",
    "message.wildercord.tide_scribe_wakes": "Ink blooms in the water. The Tide Scribe rises from its core.",
    "message.wildercord.tide_scribe_phase.2": "The Tide Scribe calls the tide, and the drowned answer.",
    "message.wildercord.tide_scribe_phase.3": "The Tide Scribe writes the deep into the room.",
    "message.wildercord.tide_scribe_stranded": "The ice closes round the Tide Scribe. It's stranded!",
    "message.wildercord.tide_rises": "The tide is rising: get up out of the water, or freeze it",
    "message.wildercord.tide_conducts": "The water carries your storm to everything in it (you too, if you're wading)",
    "boss.wildercord.shifting.tide_scribe": "%s \u00B7 calling the tide",
    "boss.wildercord.tide_rising": "the tide is rising",
    "boss.wildercord.tide_high": "the arena is flooded",
    "boss.wildercord.tide_ebbing": "the tide is going out",
    "boss.wildercord.tide_scribe_stranded": "stranded in the ice!",
    # Sound subtitles
    "subtitles.wildercord.boss_phase": "A boss gathers its power",
    "subtitles.wildercord.boss_rise": "A boss wakes",
    "subtitles.wildercord.warden_ambient": "Cinder Warden rumbles",
    "subtitles.wildercord.warden_hurt": "Cinder Warden cracks",
    "subtitles.wildercord.warden_death": "Cinder Warden goes cold",
    "subtitles.wildercord.warden_slam": "Cinder Warden slams the floor",
    "subtitles.wildercord.warden_immune": "Spell glances off armour",
    "subtitles.wildercord.star_eater_ambient": "Star-Eater hums",
    "subtitles.wildercord.star_eater_hurt": "Star-Eater cracks",
    "subtitles.wildercord.star_eater_death": "Star-Eater collapses",
    "subtitles.wildercord.star_eater_reflect": "Spell turned back",
    "subtitles.wildercord.shard_parry": "Star shard parried",
    "subtitles.wildercord.tide_scribe_ambient": "Tide Scribe murmurs",
    "subtitles.wildercord.tide_scribe_hurt": "Tide Scribe hurts",
    "subtitles.wildercord.tide_scribe_death": "Tide Scribe sinks",
    "subtitles.wildercord.tide_rise": "The tide rises",
    "subtitles.wildercord.tide_ebb": "The tide ebbs",
}

# The travel commands: homes, warps, waypoints, teleport requests, /back, /spawn and /rtp (dev.wildercord.travel).
TRAVEL_LANG = {
    # Shared by every teleport
    "message.wildercord.travel.disabled": "Travel commands are turned off on this server",
    "message.wildercord.travel.duel": "You can't teleport during a duel",
    "message.wildercord.travel.cooldown": "You can use %s again in %s seconds",
    "message.wildercord.travel.bad_name": "Names can only use letters, numbers, - and _ (up to %s of them)",
    "message.wildercord.travel.no_world": "That place is in a world that doesn't exist any more",
    "message.wildercord.travel.unsafe": "There's no safe ground to land on near %s",
    "message.wildercord.travel.countdown": "Teleporting to %s in %s... stand still",
    "message.wildercord.travel.arrived": "Teleported to %s",
    "message.wildercord.travel.cancel.moved": "Teleport cancelled: you moved",
    "message.wildercord.travel.cancel.hurt": "Teleport cancelled: you were hurt",
    "message.wildercord.travel.cancel.duel": "Teleport cancelled: you're in a duel",
    "message.wildercord.travel.place": "%s, %s, %s in %s",
    "message.wildercord.travel.go_hover": "Teleport to %s\n%s",
    "message.wildercord.travel.where.home_default": "your home",
    "message.wildercord.travel.where.home": "your home %s",
    "message.wildercord.travel.where.warp": "%s",
    "message.wildercord.travel.where.spawn": "spawn",
    "message.wildercord.travel.where.back": "where you were",
    "message.wildercord.travel.where.random": "somewhere new",
    # Homes
    "message.wildercord.travel.home_set_default": "Home set! /home brings you back here",
    "message.wildercord.travel.home_set": "Home %1$s set! /home %1$s brings you back here",
    "message.wildercord.travel.home_moved_default": "Your home is here now",
    "message.wildercord.travel.home_moved": "Home %s is here now",
    "message.wildercord.travel.homes_full": "You already have %s homes, the most you can have. Set one of them again to move it, or remove one with /delhome",
    "message.wildercord.travel.homes_off": "Homes are turned off on this server",
    "message.wildercord.travel.homes_none": "You haven't set a home yet. Stand where you'd like it and type /sethome",
    "message.wildercord.travel.home_missing": "You don't have a home called %s",
    "message.wildercord.travel.home_deleted": "Home %s removed",
    "message.wildercord.travel.home_unsafe": "It isn't safe to land at %s any more, and there's no safe ground near it",
    "message.wildercord.travel.homes_list": "Your homes (%s of %s): ",
    # Warps
    "message.wildercord.travel.warp_set": "Warp %1$s set here. Anyone can go to it with /warp %1$s",
    "message.wildercord.travel.warp_moved": "Warp %s moved here",
    "message.wildercord.travel.warp_missing": "There's no warp called %s",
    "message.wildercord.travel.warp_deleted": "Warp %s removed",
    "message.wildercord.travel.warps_none": "There are no warps yet",
    "message.wildercord.travel.warps_list": "Warps (%s): ",
    # /back, /spawn and /rtp
    "message.wildercord.travel.back_none": "There's nowhere to go back to yet",
    "message.wildercord.travel.back_hint": "Type /back to return to where you died",
    "message.wildercord.travel.back_hover": "Go back to where you died",
    "message.wildercord.travel.rtp_failed": "Couldn't find a safe spot this time. Try again",
    "message.wildercord.travel.rtp_landed": "You landed at %s, %s, %s",
    # Teleport requests
    "message.wildercord.travel.tpa_self": "You can't send a teleport request to yourself",
    "message.wildercord.travel.tpa_closed": "%s isn't taking teleport requests",
    "message.wildercord.travel.tpa_busy": "%s is in a duel right now",
    "message.wildercord.travel.tpa_asked_to": "%s would like to teleport to you.",
    "message.wildercord.travel.tpa_asked_here": "%s would like you to teleport to them.",
    "message.wildercord.travel.tpa_accept": "[Accept]",
    "message.wildercord.travel.tpa_deny": "[Deny]",
    "message.wildercord.travel.tpa_accept_hover": "Accept %s's request",
    "message.wildercord.travel.tpa_deny_hover": "Turn the request down",
    "message.wildercord.travel.tpa_sent": "Request sent to %s. They have %s seconds to answer (/tpcancel takes it back)",
    "message.wildercord.travel.tpa_none": "You don't have any teleport requests",
    "message.wildercord.travel.tpa_none_from": "%s hasn't sent you a request, or it ran out",
    "message.wildercord.travel.tpa_gone": "That player isn't online any more",
    "message.wildercord.travel.tpa_accepted": "%s accepted your teleport request",
    "message.wildercord.travel.tpa_you_accepted": "You accepted %s's request",
    "message.wildercord.travel.tpa_denied": "%s turned down your teleport request",
    "message.wildercord.travel.tpa_you_denied": "You turned down %s's request",
    "message.wildercord.travel.tpa_expired_from": "Your teleport request to %s ran out",
    "message.wildercord.travel.tpa_expired_to": "The teleport request from %s ran out",
    "message.wildercord.travel.tpa_cancelled": "You took back your request to %s",
    "message.wildercord.travel.tpa_withdrawn": "%s took back their teleport request",
    "message.wildercord.travel.tpa_nothing_out": "You don't have any requests waiting",
    "message.wildercord.travel.tpa_left": "%s left, so their teleport request was dropped",
    "message.wildercord.travel.tpa_off": "Teleport requests are off: nobody can send you one. /tptoggle turns them back on",
    "message.wildercord.travel.tpa_on": "Teleport requests are on again",
    "message.wildercord.travel.tpa_unsafe": "There's no safe ground near %s to land on",
    # Waypoints
    "message.wildercord.travel.waypoint_added": "Waypoint %s added at %s. Show the way: %s",
    "message.wildercord.travel.waypoint_moved": "Waypoint %s moved to %s. Show the way: %s",
    "message.wildercord.travel.waypoint_track_hover": "Track %s",
    "message.wildercord.travel.waypoints_full": "You have %s waypoints, the most you can keep. Remove one with /waypoint remove",
    "message.wildercord.travel.waypoint_missing": "You don't have a waypoint called %s",
    "message.wildercord.travel.waypoint_removed": "Waypoint %s removed",
    "message.wildercord.travel.waypoints_none": "You don't have any waypoints yet. /waypoint add <name> marks where you're standing",
    "message.wildercord.travel.waypoints_list": "Your waypoints (%s): ",
    "message.wildercord.travel.waypoint_hover": "%s\nClick to track it",
    "message.wildercord.travel.waypoint_tracking": "Tracking %s: follow the arrow in the top corner. /waypoint untrack hides it",
    "message.wildercord.travel.waypoint_untracked": "Stopped tracking %s",
    "message.wildercord.travel.waypoint_not_tracking": "You aren't tracking a waypoint",
    "message.wildercord.travel.waypoint_share_self": "You can't share a waypoint with yourself",
    "message.wildercord.travel.waypoint_share_wait": "You can share another waypoint with %s in %s seconds",
    "message.wildercord.travel.waypoint_shared": "Shared %s with %s",
    "message.wildercord.travel.waypoint_offer": "%s shared a waypoint with you: %s, at %s.",
    "message.wildercord.travel.waypoint_offer_add": "[Add]",
    "message.wildercord.travel.waypoint_offer_hover": "Add %s to your waypoints",
    # The waypoint line on the HUD
    "hud.wildercord.waypoint.blocks": "%s blocks",
    "hud.wildercord.waypoint.here": "you're here",
    "hud.wildercord.waypoint.elsewhere": "in %s",
    # Worlds, by name
    "dimension.wildercord.overworld": "the Overworld",
    "dimension.wildercord.the_nether": "the Nether",
    "dimension.wildercord.the_end": "the End",
}

# Loadouts: saved Cord setups, from the Cord screen's panel, the quick-switch key and /loadout.
LOADOUT_LANG = {
    "key.wildercord.next_loadout": "Next loadout",
    "key.wildercord.magic_settings": "Magic visual settings",
    "message.wildercord.practice.restart": "The practice dimension is not loaded. Restart the world after installing this version.",
    "message.wildercord.practice.enter": "Practice arena: use your spells on the dummies. /wildercord practice moving, stress <1-24>, benchmark, or leave.",
    "message.wildercord.practice.benchmark": "Cast for 10 seconds. The result reports server particle deliveries, decorations limited, and plan cache work.",
    # The server's answers (above the hotbar, or in chat for /loadout)
    "message.wildercord.loadout.saved": "Saved your Cord as %s (%s of %s)",
    "message.wildercord.loadout.replaced": "Saved your Cord over %s",
    "message.wildercord.loadout.renamed": "Renamed %s to %s",
    "message.wildercord.loadout.deleted": "Deleted the loadout %s. Your Cord keeps what it holds",
    "message.wildercord.loadout.loaded": "Loaded %s",
    "message.wildercord.loadout.loaded_quiet": "Loaded %s. %s runes stay quiet: not learned, too strong for this Cord, or past its sockets",
    "message.wildercord.loadout.switched": "Loadout: %s (%s of %s)",
    "message.wildercord.loadout.switched_quiet": "Loadout: %s (%s of %s). %s runes stay quiet",
    "message.wildercord.loadout.bad_name": "Give the loadout a name (up to %s letters)",
    "message.wildercord.loadout.name_taken": "You already have a loadout called %s",
    "message.wildercord.loadout.full": "You have %s loadouts, the most you can keep. Save over one, or delete one",
    "message.wildercord.loadout.gone": "That loadout isn't there any more",
    "message.wildercord.loadout.missing": "You don't have a loadout called %s",
    "message.wildercord.loadout.none": "You haven't saved a loadout yet. Open your Cord (%s) and click the list badge to save one",
    "message.wildercord.loadout.dead": "You can't change loadouts right now",
    "message.wildercord.loadout.charging": "You can't change loadouts while charging a spell",
    "message.wildercord.loadout.duel": "You can't change loadouts during a duel",
    "message.wildercord.loadout.sealed": "You can't change loadouts while sealed in ice",
    "message.wildercord.loadout.list": "Your loadouts (%s of %s): ",
    "message.wildercord.loadout.list_hover": "Load %s",
    "message.wildercord.loadout.list_none": "You haven't saved a loadout yet. /loadout save <name> saves your whole Cord",
    # The Cord screen's badge and panel
    "screen.wildercord.loadouts.badge": "Loadouts",
    "screen.wildercord.loadouts.badge.hint": "Save your whole Cord (every spell and its name, your passives and the selected spell) and swap between setups: one for fighting, one for mining, one for exploring",
    "screen.wildercord.loadouts.badge.count": "%s of %s saved",
    "screen.wildercord.loadouts.badge.keys": "Ctrl+L opens them. Next loadout key: %s",
    "screen.wildercord.loadouts.title": "Loadouts (%s of %s)",
    "screen.wildercord.loadouts.close": "Close (Esc)",
    "screen.wildercord.loadouts.load": "Load",
    "screen.wildercord.loadouts.load.hint": "Put this loadout on your Cord. Runes you don't know or your Cord can't hold stay quiet, and every spell that changes starts its cooldown",
    "screen.wildercord.loadouts.save_here": "Save current here",
    "screen.wildercord.loadouts.save_here.hint": "Replace this loadout with your Cord as it is now. Click twice to be sure",
    "screen.wildercord.loadouts.rename": "Rename",
    "screen.wildercord.loadouts.rename.hint": "Type a new name. Enter saves, Esc cancels",
    "screen.wildercord.loadouts.delete": "Delete",
    "screen.wildercord.loadouts.delete.hint": "Forget this loadout (your Cord keeps what it holds now). Click twice to be sure",
    "screen.wildercord.loadouts.confirm_save": "Click again to save over %s",
    "screen.wildercord.loadouts.confirm_delete": "Click again to delete %s",
    "screen.wildercord.loadouts.save_new": "+ Save current as new",
    "screen.wildercord.loadouts.save_new.hint": "Save your spells, their names, your passives and the selected spell as a new loadout",
    "screen.wildercord.loadouts.default_name": "Loadout %s",
    "screen.wildercord.loadouts.typing": "Enter saves · Esc cancels",
    "screen.wildercord.loadouts.note": "Runes you don't know or your Cord can't hold stay quiet",
    "screen.wildercord.loadouts.keys": "\u2191\u2193 pick · Enter load · Ctrl+R rename · Del delete",
    "screen.wildercord.loadouts.current": "Last loaded",
    "screen.wildercord.loadouts.spell": "Spell %s: ",
    "screen.wildercord.loadouts.spell_selected": "Spell %s (selected): ",
    "screen.wildercord.loadouts.passive": "Passive %s: ",
    "screen.wildercord.loadouts.passive_off": "Passive %s (off): ",
    "screen.wildercord.loadouts.nothing": "Nothing threaded",
}


# Players' own affinities with the elements: the Grimoire's section, its tooltips, the readout, messages and toasts.
PLAYER_AFFINITY_LANG = {
    "message.wildercord.affinity.first": "An affinity with %s wakes in you: I. Its spells hit %s%% harder.",
    "message.wildercord.affinity.rise": "Your affinity with %s deepens: %s. Its spells hit %s%% harder.",
    "message.wildercord.affinity.resist": "Your affinity with %s deepens: %s. Its spells hit %s%% harder, and you shrug off %s%% of it from others' spells.",
    "message.wildercord.affinity.mastered": "Your affinity with %s deepens: V. Its spells hit %s%% harder and cost %s%% less, and you shrug off %s%% of it from others' spells.",
    "toast.wildercord.affinity_new": "An affinity awakens",
    "toast.wildercord.affinity": "Your affinity deepens",
    "toast.wildercord.affinity_level": "%s %s",
    # The Grimoire page
    "screen.wildercord.grimoire.affinities": "Affinities",
    "screen.wildercord.grimoire.affinities_hint": "What you do grows your affinity with each element: casting it, setting off its reactions, and everyday things that fit it. Every level makes its spells stronger; from III you shrug off some of it from others' spells, and at V its spells cost less. Hover an element for what raises it.",
    "screen.wildercord.grimoire.affinities_off": "Affinities are switched off on this server: they don't grow, and give nothing.",
    "screen.wildercord.grimoire.affinity_none": "%s: none yet",
    "screen.wildercord.grimoire.affinity_level": "%s %s",
    "screen.wildercord.affinity.title": "%s affinity: %s",
    "screen.wildercord.affinity.title_none": "%s affinity: none yet",
    "screen.wildercord.affinity.power": "+%s%% power with %s spells",
    "screen.wildercord.affinity.resist": "Shrug off %s%% of %s from others' spells",
    "screen.wildercord.affinity.cheaper": "%s spells cost %s%% less",
    "screen.wildercord.affinity.next": "Next, %s at %s / %s:",
    "screen.wildercord.affinity.mastered": "As deep as an affinity goes",
    "screen.wildercord.affinity.raised_by": "Raised by:",
    "screen.wildercord.affinity.source": "\u2022 %s (up to %s a day)",
    "screen.wildercord.affinity.source_tail": "\u2022 %s (%s a day, then %s%% as fast)",
    "screen.wildercord.affinity.allowance": "A day is an in-game day, and sleeping doesn't bring the next one sooner.",
    # The readout, under a spell's cost
    "screen.wildercord.affinity.readout": "%s affinity %s: +%s%% power",
    "screen.wildercord.affinity.readout_mastered": "%s affinity %s: +%s%% power, %s%% less mana",
    # What raises each (%s is the element's name, for the three every element shares)
    "affinity.wildercord.source.cast": "Casting %s spells, by the mana they cost",
    "affinity.wildercord.source.reaction": "Setting off reactions with %s in them",
    "affinity.wildercord.source.bestiary": "Finding creatures weak to %s",
    "affinity.wildercord.source.smelt": "Smelting: taking what a furnace, blast furnace or smoker made",
    "affinity.wildercord.source.fire_kill": "Killing with fire",
    "affinity.wildercord.source.burning": "Being on fire, and living through it",
    "affinity.wildercord.source.lava": "Standing near lava in the Nether",
    "affinity.wildercord.source.fish": "Catching fish",
    "affinity.wildercord.source.cold": "Time in snowy, frozen lands, or on ice",
    "affinity.wildercord.source.frozen": "Being frozen stiff in powder snow, and living through it",
    "affinity.wildercord.source.thunder": "Time out under a thunderstorm",
    "affinity.wildercord.source.struck": "Being struck by lightning, and living through it",
    "affinity.wildercord.source.rod": "A lightning rod struck near you",
    "affinity.wildercord.source.glide": "Flying with an elytra, per 100 blocks",
    "affinity.wildercord.source.fall": "Living through a long fall",
    "affinity.wildercord.source.heights": "Time high over the world (above y 200)",
    "affinity.wildercord.source.stone": "Mining natural stone with a pickaxe",
    "affinity.wildercord.source.ore": "Mining ores",
    "affinity.wildercord.source.deep": "Time deep underground (below y 0)",
    "affinity.wildercord.source.harvest": "Harvesting ripe crops",
    "affinity.wildercord.source.breed": "Breeding animals",
    "affinity.wildercord.source.heal": "Healing others with spells",
    "affinity.wildercord.source.tame": "Taming animals",
    "affinity.wildercord.source.end": "Time in the End",
    "affinity.wildercord.source.void_kill": "Slaying endermen, endermites and shulkers",
    "affinity.wildercord.source.pearl": "Throwing ender pearls",
    "affinity.wildercord.source.deep_dark": "Time in the deep dark",
    "affinity.wildercord.source.enchant": "Enchanting items",
    "affinity.wildercord.source.meditate": "Meditating (sneak and stand still)",
    "affinity.wildercord.source.page": "Reading Torn Pages",
    "affinity.wildercord.source.rune": "Learning new runes",
    "affinity.wildercord.source.ley": "Standing on ley lines",
    "affinity.wildercord.source.night_watch": "Staying out under the sky a whole night",
    "affinity.wildercord.source.age": "Aging the world with time magic",
    "affinity.wildercord.source.clock": "Carrying a clock",
    "affinity.wildercord.source.melee_kill": "Kills in melee",
    "affinity.wildercord.source.blood_price": "Paying Blood Price, per health",
    "affinity.wildercord.source.heavy_hit": "Taking a heavy blow, and living through it",
}

ARCHIVE_LAND = ["#minecraft:is_taiga", "#minecraft:is_jungle", "#minecraft:is_forest", "#minecraft:is_savanna", "#minecraft:is_badlands",
                "minecraft:plains", "minecraft:sunflower_plains", "minecraft:snowy_plains", "minecraft:desert", "minecraft:meadow",
                "minecraft:cherry_grove", "minecraft:snowy_taiga", "minecraft:grove"]


# The runes of the world: tooltips, the Grimoire, Attunement and the Blank Rune.
WORLD_LANG = {
    "tooltip.wildercord.found_only": "Can't be crafted: a rune of the world, found only in its own places",
    "tooltip.wildercord.blank_rune.attune": "Some lands hold a rune of their own. Meditate there with a Blank Rune in hand",
    "tooltip.wildercord.blank_rune.attuned": "Attuned so far: %s of %s (see the Grimoire)",
    "message.wildercord.attune_begin": "The Blank Rune stirs: the land here holds a rune. Keep still...",
    "message.wildercord.attune_stage.1": "The blank drinks in the land",
    "message.wildercord.attune_stage.2": "A shape rises in the stone",
    "message.wildercord.attune_stage.3": "The rune brightens: almost there",
    "message.wildercord.attuned": "Attuned! The Blank Rune became %s",
    "message.wildercord.attune_broken": "The attunement breaks off",
    "message.wildercord.attune_quiet": "The Blank Rune stays quiet here",
    "message.wildercord.attune_not_now": "The land here holds a rune, but it isn't the time for it",
    "message.wildercord.attune_resting": "This land gave you its rune today: it rests for about %s more minutes",
    "message.wildercord.manatide_wait": "The storm in you hasn't settled: drink again in %ss",
    "message.wildercord.current_dry": "The current needs water or rain to carry you",
    "message.wildercord.fishing_magic": "Something magical was tangled in your line!",
    "screen.wildercord.grimoire.attunements": "Attunements (%s of %s)",
    "screen.wildercord.grimoire.attunement": "%s: %s",
    "screen.wildercord.grimoire.attune_hint": "Meditate with a Blank Rune in hand where this riddle points",
    "screen.wildercord.grimoire.attune_resting": "Resting: ready again in about %s minutes",
    "screen.wildercord.grimoire.attune_ready": "Ready: a land gives you its rune once a day",
    "screen.wildercord.grimoire.world": "Runes of the world (%s of %s known)",
    "screen.wildercord.grimoire.world_hint": "Found only here, never crafted",
    "screen.wildercord.grimoire.world_unknown": "??? (a Tier %s %s)",
    "toast.wildercord.attuned": "A rune of the land",
}


def write_found_loot(runes):
    """The Riftcaller's loot table, from RuneSources.java: a rune is picked by tier, like any chest. (The dungeons'
    vaults and bosses are written by dungeon_assets.py; fallen stars, rifts and mana storms pick their runes in code.)"""
    tier = {r["path"]: r["tier"] for r in runes}
    sources = {sid: paths for sid, _, paths in rune_sources()}

    def found_pool(sid, rolls=1):
        return {"rolls": rolls, "entries": [rune_entry(p, {1: 8, 2: 5, 3: 2}.get(tier[p], 1)) for p in sources[sid]]}

    def chance(pool, odds):
        return dict(pool, conditions=[{"condition": "minecraft:random_chance", "chance": odds}])

    # The dungeons' vaults and bosses are written by dungeon_assets.py, with these same pools of their own runes.
    for boss, dungeon in (("riftcaller", "rift"),):
        write_json(DATA / f"loot_table/entities/{boss}.json", {"type": "minecraft:entity", "pools": [
            found_pool(boss),
            chance(found_pool(dungeon), 0.5),
            {"rolls": 1, "entries": [item_entry("wildercord:mana_crystal", 1, 1, 3)]},
        ]})


def rune_entry(path, weight):
    return {"type": "minecraft:item", "name": "wildercord:rune", "weight": weight,
            "functions": [{"function": "minecraft:set_components", "components": {"wildercord:rune": f"wildercord:{path}"}}]}


def item_entry(item, weight, low=1, high=1):
    entry = {"type": "minecraft:item", "name": item, "weight": weight}
    if high > 1:
        entry["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}}]
    return entry


def write_new_content(runes):
    tex = ASSETS / "textures"
    # ---- items
    save(world_art.spell_scroll_icon(), tex / "item/spell_scroll.png")
    save(world_art.torn_page_icon(), tex / "item/torn_page.png")
    save(world_art.training_dummy_icon(), tex / "item/training_dummy.png")
    for name in ("spell_scroll", "torn_page", "training_dummy"):
        item_model(name, name)
        write_json(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{name}"}})

    # ---- the Wellstone: dim off a ley line, glowing on one
    well = world_art.wellstone_textures()
    for key, image in well.items():
        save(image, tex / f"block/{key}.png")
    for state in ("", "_active"):
        write_json(ASSETS / f"models/block/wellstone{state}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "side": f"wildercord:block/wellstone_side{state}", "top": f"wildercord:block/wellstone_top{state}", "bottom": "wildercord:block/wellstone_bottom"}})
    write_json(ASSETS / "blockstates/wellstone.json", {"variants": {
        "active=false": {"model": "wildercord:block/wellstone"}, "active=true": {"model": "wildercord:block/wellstone_active"}}})
    write_json(ASSETS / "items/wellstone.json", {"model": {"type": "minecraft:model", "model": "wildercord:block/wellstone_active"}})

    # ---- Rune Seals: one model per element, dark and lit
    seals = world_art.rune_seal_textures()
    variants = {}
    for element, (unlit, lit) in seals.items():
        save(unlit, tex / f"block/rune_seal_{element}.png")
        save(lit, tex / f"block/rune_seal_{element}_lit.png")
        for suffix in ("", "_lit"):
            write_json(ASSETS / f"models/block/rune_seal_{element}{suffix}.json", {"parent": "minecraft:block/cube_all",
                "textures": {"all": f"wildercord:block/rune_seal_{element}{suffix}"}})
        variants[f"element={element},lit=false"] = {"model": f"wildercord:block/rune_seal_{element}"}
        variants[f"element={element},lit=true"] = {"model": f"wildercord:block/rune_seal_{element}_lit"}
    write_json(ASSETS / "blockstates/rune_seal.json", {"variants": variants})
    write_json(ASSETS / "items/rune_seal.json", {"model": {"type": "minecraft:model", "model": "wildercord:block/rune_seal_arcane_lit"}})

    # ---- the Archive Lectern: a squat plinth with an open book on top
    lectern = world_art.archive_lectern_textures()
    for key, image in lectern.items():
        save(image, tex / f"block/archive_lectern_{key}.png")
    face = lambda t, uv=(2, 2, 14, 14): {"texture": f"#{t}", "uv": list(uv)}
    write_json(ASSETS / "models/block/archive_lectern.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": "wildercord:block/archive_lectern_side", "top": "wildercord:block/archive_lectern_top",
                     "side": "wildercord:block/archive_lectern_side", "front": "wildercord:block/archive_lectern_front",
                     "bottom": "wildercord:block/archive_lectern_bottom"},
        "elements": [{"from": [2, 0, 2], "to": [14, 14, 14], "faces": {
            "down": face("bottom"), "up": face("top"),
            "north": face("front", (2, 2, 14, 16)), "south": face("side", (2, 2, 14, 16)),
            "west": face("side", (2, 2, 14, 16)), "east": face("side", (2, 2, 14, 16))}}]})
    write_json(ASSETS / "blockstates/archive_lectern.json", {"variants": {
        "awake=false": {"model": "wildercord:block/archive_lectern"}, "awake=true": {"model": "wildercord:block/archive_lectern"}}})
    write_json(ASSETS / "items/archive_lectern.json", {"model": {"type": "minecraft:model", "model": "wildercord:block/archive_lectern"}})

    # ---- the Fusion Altar: a deepslate foot, a pillar and an amethyst table top (faces take their natural uv)
    altar = world_art.fusion_altar_textures()
    for key, image in altar.items():
        save(image, tex / f"block/fusion_altar_{key}.png")
    write_json(ASSETS / "models/block/fusion_altar.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": "wildercord:block/fusion_altar_side", "top": "wildercord:block/fusion_altar_top",
                     "side": "wildercord:block/fusion_altar_side", "bottom": "wildercord:block/fusion_altar_bottom"},
        "elements": [
            {"from": [1, 0, 1], "to": [15, 3, 15], "faces": {d: {"texture": "#bottom" if d in ("down", "up") else "#side"}
                                                          for d in ("down", "up", "north", "south", "west", "east")}},
            {"from": [3, 3, 3], "to": [13, 10, 13], "faces": {d: {"texture": "#side"} for d in ("north", "south", "west", "east")}},
            {"from": [0, 10, 0], "to": [16, 14, 16], "faces": {"down": {"texture": "#bottom"}, "up": {"texture": "#top"},
                                                            **{d: {"texture": "#side"} for d in ("north", "south", "west", "east")}}},
        ]})
    write_json(ASSETS / "blockstates/fusion_altar.json", {"variants": {"": {"model": "wildercord:block/fusion_altar"}}})
    write_json(ASSETS / "items/fusion_altar.json", {"model": {"type": "minecraft:model", "model": "wildercord:block/fusion_altar"}})
    write_json(DATA / "loot_table/blocks/fusion_altar.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "wildercord:fusion_altar"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    write_json(DATA / "recipe/fusion_altar.json", {
        "type": "minecraft:crafting_shaped", "category": "misc",
        "key": {"A": "minecraft:amethyst_block", "L": "minecraft:lodestone", "T": "minecraft:deepslate_tiles"},
        "pattern": ["TAT", "ALA", "TAT"], "result": {"id": "wildercord:fusion_altar"}})
    unlock_advancement("wildercord:fusion_altar", "wildercord:blank_rune")

    # ---- the Knot: a whole spell tied into one rune
    save(item_art.knot_icon(), tex / "item/knot.png")
    item_model("knot", "knot")
    write_json(ASSETS / "items/knot.json", {"model": {"type": "minecraft:model", "model": "wildercord:item/knot"}})
    item_model("woven_rune", "knot")
    write_json(ASSETS / "items/woven_rune.json", {"model": {"type": "minecraft:model", "model": "wildercord:item/woven_rune"}})

    # ---- entity skins
    save(world_art.archivist_texture(), tex / "entity/archivist.png")
    save(world_art.dummy_texture(), tex / "entity/training_dummy.png")
    for name, image in world_art.creature_textures().items():
        save(image, tex / f"entity/{name}.png")

    # ---- loot
    write_json(DATA / "loot_table/blocks/wellstone.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "wildercord:wellstone"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    world = found_only()
    rollable = [r for r in runes if r["path"] not in INNATE and r["path"] not in FUSED and r["path"] not in world]
    archive_finds = next(paths for sid, _, paths in rune_sources() if sid == "archive")
    third = [r["path"] for r in rollable if r["tier"] == 3]
    second = [r["path"] for r in rollable if r["tier"] == 2]
    fourth = [r["path"] for r in rollable if r["tier"] == 4]
    write_json(DATA / "loot_table/chests/archive_library.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 3}, "entries": [rune_entry(p, 3) for p in second] + [rune_entry(p, 2) for p in third]
            + [rune_entry(p, 1) for p in archive_finds]},
        {"rolls": {"type": "minecraft:uniform", "min": 1, "max": 2}, "entries": [
            item_entry("wildercord:torn_page", 6), item_entry("wildercord:mana_crystal", 3), item_entry("wildercord:blank_rune", 4, 2, 5),
            item_entry("minecraft:book", 4, 1, 3), item_entry("minecraft:lapis_lazuli", 4, 3, 9), item_entry("minecraft:amethyst_shard", 3, 2, 6)]},
    ]})
    write_json(DATA / "loot_table/chests/archive_vault.json", {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [rune_entry(p, 1) for p in fourth]},
        {"rolls": {"type": "minecraft:uniform", "min": 1, "max": 2}, "entries": [rune_entry(p, 1) for p in third]},
        {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 3}, "entries": [
            item_entry("wildercord:mana_crystal", 4, 1, 2), item_entry("wildercord:torn_page", 4), item_entry("minecraft:diamond", 2, 1, 3),
            item_entry("minecraft:gold_ingot", 3, 2, 6), item_entry("minecraft:echo_shard", 1)]},
    ]})

    write_found_loot(runes)

    # ---- recipes
    write_json(DATA / "recipe/wellstone.json", {
        "type": "minecraft:crafting_shaped", "category": "building",
        "key": {"A": "minecraft:amethyst_block", "M": "wildercord:mana_crystal", "D": "minecraft:polished_deepslate", "T": "minecraft:deepslate_tiles"},
        "pattern": ["DAD", "AMA", "TTT"], "result": {"id": "wildercord:wellstone"}})
    unlock_advancement("wildercord:wellstone", "wildercord:mana_crystal")
    write_json(DATA / "recipe/training_dummy.json", {
        "type": "minecraft:crafting_shaped", "category": "misc",
        "key": {"W": "minecraft:white_wool", "H": "minecraft:hay_block", "S": "minecraft:stick", "B": "minecraft:smooth_stone_slab"},
        "pattern": [" W ", "SHS", " B "], "result": {"id": "wildercord:training_dummy"}})
    unlock_advancement("wildercord:training_dummy", "wildercord:twine_cord")

    # ---- mining
    write_json(RES / "data/minecraft/tags/block/mineable/pickaxe.json", {"replace": False, "values": ["wildercord:wellstone", "wildercord:fusion_altar"]})
    # The Wither's skulls and charge break anything not in this tag (unbreakable or not).
    write_json(RES / "data/minecraft/tags/block/wither_immune.json", {"replace": False, "values": [
        "wildercord:rune_seal", "wildercord:archive_lectern", "wildercord:dungeon_altar", "wildercord:fallen_star"]})

    # ---- the Archive in the world
    write_json(DATA / "worldgen/structure/archive.json", {
        "type": "wildercord:archive",
        "biomes": "#wildercord:has_structure/archive",
        "spawn_overrides": {"monster": {"bounding_box": "piece", "spawns": [
            {"type": "minecraft:skeleton", "weight": 3, "count": 1},
            {"type": "minecraft:zombie", "weight": 3, "count": 1},
            {"type": "minecraft:witch", "weight": 1, "count": 1}]}},
        "step": "underground_structures",
        "terrain_adaptation": "none"})
    write_json(DATA / "worldgen/structure_set/archives.json", {
        "placement": {"type": "minecraft:random_spread", "salt": 20260927, "separation": 14, "spacing": 44},
        "structures": [{"structure": "wildercord:archive", "weight": 1}]})
    write_json(DATA / "tags/worldgen/biome/has_structure/archive.json", {"values": ARCHIVE_LAND})
    write_json(DATA / "tags/worldgen/structure/archive.json", {"values": ["wildercord:archive"]})


# ---------------------------------------------------------------- the advancement tab
#
# One Wildercord tab, from a first Blank Rune to Archmage. The criteria are the mod's own
# (registered in advancement/WildercordTriggers.java):
#   wildercord:feat {"feat": id}                     the Grimoire holds feat:<id>
#   wildercord:grimoire {"entry": key} | {"prefix": p, "count": n} | {"prefix": p, "all": true} | {"signatures": n}
#   wildercord:heart_circle {"level": n}             n circles formed, or more
#   wildercord:runes_known {"count": n} | {"all": true}
#   wildercord:cord {"tier": "copper"}               wearing that Cord or a better one
#   wildercord:moment {"moment": id}                 something just happened (Advancements.java)
# All but moments are checked against the player's state, and again whenever a player joins, so
# players who were already there get the advancements too.
#
# A new feat needs one line in ADVANCEMENTS below: feat_adv("<id>", "<parent>", <icon>). Its title and
# description default to the feat's name and text in Feats.java.

ADVANCEMENT_BACKGROUND = "wildercord:gui/advancements/backgrounds/wildercord"


def read_feats():
    """Feats.java: feat id -> (name, description), so feat advancements never drift from the Grimoire."""
    src = (ROOT / "src/main/java/dev/wildercord/spell/Feats.java").read_text(encoding="utf-8")
    ids = dict(re.findall(r'public static final String (\w+) = "(\w+)";', src))
    feats = {}
    for const, name, desc in re.findall(r'new Feat\((\w+), "((?:[^"\\]|\\.)*)", "((?:[^"\\]|\\.)*)"\)', src):
        feats[ids[const]] = (name, desc)
    assert len(feats) >= 19, f"parsed only {len(feats)} feats"
    return feats


def rune(path):
    return {"id": "wildercord:rune", "components": {"wildercord:rune": f"wildercord:{path}"}}


def item(item_id):
    return {"id": item_id if ":" in item_id else f"wildercord:{item_id}"}


# Criteria.
def feat(feat_id):
    return {"trigger": "wildercord:feat", "conditions": {"feat": feat_id}}


def moment(name):
    return {"trigger": "wildercord:moment", "conditions": {"moment": name}}


def circle(level):
    return {"trigger": "wildercord:heart_circle", "conditions": {"level": level}}


def runes_known(count=None):
    return {"trigger": "wildercord:runes_known", "conditions": {"all": True} if count is None else {"count": count}}


def cord(tier):
    return {"trigger": "wildercord:cord", "conditions": {"tier": tier}}


def grimoire(entry=None, prefix=None, count=None, every=False, signatures=None):
    conditions = {}
    if entry:
        conditions["entry"] = entry
    if signatures is not None:
        conditions["signatures"] = signatures
    if prefix is not None:
        conditions["prefix"] = prefix
    if count is not None:
        conditions["count"] = count
    if every:
        conditions["all"] = True
    return {"trigger": "wildercord:grimoire", "conditions": conditions}


def has_item(item_id):
    return {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": item_id}]}}


def in_structure(structure):
    return {"trigger": "minecraft:location", "conditions": {"player": {
        "type": "minecraft:entity_properties", "entity": "this", "predicate": {"minecraft:location": {"structures": structure}}}}}


# Rewards: experience always; the big ones add a loot table from ADVANCEMENT_REWARDS.
ADVANCEMENT_REWARDS = {
    "blank_runes": ("wildercord:blank_rune", 8),
    "mana_crystal": ("wildercord:mana_crystal", 1),
    "mana_crystals": ("wildercord:mana_crystal", 3),
}

ADVANCEMENTS = []   # (id, parent, icon, title, description, criteria, options), in tree order


def adv(path, parent, icon, title, description, criteria, frame="task", xp=0, loot=(), hidden=False, any_of=False, **extra):
    """One advancement. criteria is one criterion (named after the advancement) or a dict of them;
    they're all needed unless any_of."""
    ADVANCEMENTS.append({"path": path, "parent": parent, "icon": icon, "title": title, "description": description,
                         "criteria": criteria, "frame": frame, "xp": xp, "loot": list(loot), "hidden": hidden,
                         "any_of": any_of, **extra})


def feat_adv(feat_id, parent, icon, title=None, description=None, branch=None, **options):
    """An advancement for a Grimoire feat, in its parent's branch unless another is named; the title
    and description default to Feats.java's."""
    name, text = FEATS[feat_id] if feat_id in FEATS else (feat_id.replace("_", " ").title(), "")
    path = f"{branch or parent.split('/')[0]}/{feat_id}"
    adv(path, parent, icon, title or name, description or text.rstrip("."), feat(feat_id), **options)


FEATS = read_feats()

# ---- the root
adv("root", None, item("blank_rune"), "Wildercord", "Craft a Blank Rune, or wear a Cord",
    {"blank_rune": has_item("wildercord:blank_rune"), "cord": cord("twine")}, any_of=True,
    background=ADVANCEMENT_BACKGROUND, show_toast=False, announce_to_chat=False)

# ---- Cords
adv("cords/twine", "root", item("twine_cord"), "Tied On", "Wear a Twine Cord in the slot above your offhand", cord("twine"), xp=10)
adv("cords/copper", "cords/twine", item("copper_cord"), "Copper Wire", "Wear a Copper Cord", cord("copper"), xp=20)
adv("cords/amethyst", "cords/copper", item("amethyst_cord"), "Singing Stone", "Wear an Amethyst Cord", cord("amethyst"), frame="goal", xp=50)
adv("cords/echo", "cords/amethyst", item("echo_cord"), "Echoes of the Deep", "Wear an Echo Cord", cord("echo"), frame="goal", xp=100, loot=["blank_runes"])

# ---- Casting
adv("casting/first_cast", "cords/twine", rune("bolt"), "First Words", "Cast a spell from your Cord", moment("cast"), xp=10)
feat_adv("charged", "casting/first_cast", rune("overcharge"), description="Hold the cast key until a spell is fully charged, then let it go", xp=15)
feat_adv("rhythm", "casting/charged", rune("pulse"), description="Chain three casts, each just as the last comes off cooldown", xp=25)
adv("casting/long_cast", "casting/first_cast", rune("extend"), "Mouthful", "Cast a spell of six runes or more", moment("long_cast"), xp=15)
feat_adv("long_spell_kill", "casting/long_cast", rune("execute"), description="Slay a monster with a spell of six runes or more", frame="goal", xp=50)
feat_adv("overcast", "casting/first_cast", rune("overdrive"), description="Crack a Heart Circle to cast a spell you can't pay for", xp=25)
adv("casting/passive", "casting/first_cast", rune("swift"), "Second Nature", "Keep a passive spell running", moment("passive"), xp=25)
feat_adv("collision", "casting/first_cast", rune("comet"), description="Shoot an enemy's spell out of the air with your own", xp=25)
feat_adv("leaning", "casting/first_cast", rune("ember"), description="Grow one affinity so far past the rest that your magic leans toward it", xp=25)
feat_adv("scroll", "casting/first_cast", item("spell_scroll"), description="Inscribe a spell onto a scroll from the Cord screen", xp=15)

# ---- Heart Circles
CIRCLE_ORDINALS = ["1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th"]
CIRCLE_ADVANCEMENTS = [  # title, icon, frame, experience, reward
    ("Heartbeat", rune("spark"), "task", 20, ()),
    ("Deeper Wells", rune("focus"), "task", 30, ()),
    ("Mana Skin", rune("stoneskin"), "task", 40, ()),
    ("Turning as One", rune("orbit"), "task", 50, ()),
    ("Flow", rune("quicken"), "goal", 75, ()),
    ("Brighter Still", rune("prism"), "goal", 100, ["blank_runes"]),
    ("Overflow", rune("amplify"), "goal", 150, ["mana_crystal"]),
    ("Archmage", item("mana_crystal"), "challenge", 500, ["mana_crystals"]),
]
for n, (title, icon, frame, xp, loot) in enumerate(CIRCLE_ADVANCEMENTS, start=1):
    parent = "casting/first_cast" if n == 1 else f"heart/circle_{n - 1}"
    description = f"Form your {CIRCLE_ORDINALS[n - 1]} Heart Circle" + (": meditate once your heart is ready" if n == 1 else "")
    if n == 8:
        description = "Form your 8th Heart Circle and become an Archmage"
    adv(f"heart/circle_{n}", parent, icon, title, description, circle(n), frame=frame, xp=xp, loot=loot)
feat_adv("innate", "heart/circle_1", rune("twin_star"), description="Awaken your innate rune at the 1st Circle", xp=25)
feat_adv("mirror", "heart/innate", rune("mirrorfrost"), description="Turn an enemy's own spell back on them", hidden=True, xp=50)

# ---- Discovery
adv("discovery/runes_10", "root", rune("light"), "Lettered", "Know 10 runes", runes_known(10), xp=10)
adv("discovery/runes_50", "discovery/runes_10", rune("reveal"), "Well Read", "Know 50 runes", runes_known(50), frame="goal", xp=50)
adv("discovery/runes_100", "discovery/runes_50", rune("foresight"), "Walking Codex", "Know 100 runes", runes_known(100), frame="goal", xp=100, loot=["blank_runes"])
adv("discovery/runes_all", "discovery/runes_100", rune("decree"), "Every Word", "Know every rune you can find or make, and your own innate one", runes_known(), frame="challenge", xp=500, loot=["mana_crystals"])
REACTION_ADVANCEMENTS = {  # reaction -> (title, icon rune, description)
    "shatter": ("Shatter", "freeze", "Hit a frozen foe with fire and shatter the ice"),
    "conduct": ("Conduct", "lightning", "Strike a wet foe with storm magic"),
    "wildfire": ("Wildfire", "inferno", "Set fire to a foe the wind has just thrown"),
    "implode": ("Implode", "gravity_well", "Blast enemies that have just been pulled together"),
    "collapse": ("Collapse", "repel", "Repel enemies that have just been pulled in"),
    "overload": ("Overload", "plasma", "Strike a burning foe with storm magic and blow its flames apart"),
    "fracture": ("Fracture", "pelt", "Crack a frozen foe with earth magic"),
    "blight": ("Blight", "venom", "Strike a foe that void has shadowed with life magic, and let the rot spread"),
    "unweave": ("Unweave", "prismatic_burst", "Strike a foe carrying two marks or more with arcane magic"),
    "rupture": ("Rupture", "bleed", "Strike a bleeding foe with wind magic"),
    "elapse": ("Elapse", "countdown", "Strike a burning, poisoned or withering foe with time magic"),
}
for reaction, (title, icon, description) in REACTION_ADVANCEMENTS.items():
    adv(f"discovery/{reaction}", "discovery/runes_10", rune(icon), title, description, grimoire(entry=f"reaction:{reaction}"), xp=15)
adv("discovery/all_reactions", "discovery/runes_10", rune("prism"), "Chain Reaction", "Set off every element reaction",
    grimoire(prefix="reaction:", every=True), frame="goal", xp=100, loot=["blank_runes"])
adv("discovery/torn_page", "discovery/runes_10", item("torn_page"), "Marginalia", "Read the riddle on a Torn Page", grimoire(prefix="hint:"), xp=15)
adv("discovery/secret", "discovery/torn_page", rune("veil"), "Hidden Words", "Find a secret spell", grimoire(prefix="secret:"), frame="goal", xp=50, hidden=True)
adv("discovery/all_secrets", "discovery/secret", rune("echo"), "Nothing Left Unsaid", "Find every secret spell",
    grimoire(prefix="secret:", every=True), frame="challenge", xp=300, loot=["mana_crystals"], hidden=True)
adv("discovery/grimoire", "discovery/all_secrets", item("spell_scroll"), "Every Page Filled", "Fill the Grimoire: every reaction and secret spell, and every feat any caster can earn alone",
    grimoire(prefix="", every=True), frame="challenge", xp=500, loot=["mana_crystals", "blank_runes"], hidden=True)

# ---- the World
feat_adv("ley_line", "root", rune("vein"), description="Stand on a ley line, where the world's mana runs close to the surface", branch="world", xp=10)
feat_adv("wellstone", "world/ley_line", item("wellstone"), description="Wake a Wellstone by setting it on a ley line", xp=30)
adv("world/archive", "world/ley_line", item("minecraft:chiseled_bookshelf"), "The Buried Library", "Find an Archive", in_structure("wildercord:archive"), xp=25)
feat_adv("seal", "world/archive", item("rune_seal"), description="Open a Rune Seal door in an Archive", xp=30)
feat_adv("archivist", "world/seal", item("archive_lectern"), description="Defeat the Archivist", frame="challenge", xp=500, loot=["mana_crystals"])
feat_adv("runebound", "root", rune("rend"), description="Slay a Runebound, a monster that casts spells", branch="world", xp=15)
adv("world/runebound_adept", "world/runebound", rune("cleave"), "Adept's End", "Slay a Runebound Adept", moment("runebound_adept"), frame="goal", xp=50)
feat_adv("clash", "world/runebound", rune("zone"), description="Shatter another caster's Domain with your own", frame="goal", xp=50)
feat_adv("unison", "world/runebound", rune("chain"), description="Strike a foe with another element at the same moment as another caster", frame="goal", xp=50)

# ---- Shields and imbuing
feat_adv("spellguard", "casting/first_cast", rune("shield"), description="Stop a spell with your Shield", branch="shields", xp=20)
feat_adv("shieldbreaker", "shields/spellguard", rune("break"), description="Shatter a Shield with a stronger spell", xp=25)
feat_adv("imbue", "casting/first_cast", rune("imbue"), description="Imbue a spell into an item or a block", branch="shields", xp=20)
feat_adv("parry", "shields/spellguard", rune("deflect"), description="Raise a Shield at the last moment and turn a spell back on its caster", xp=25)
feat_adv("wild_surge", "casting/first_cast", rune("resonance"), description="Overcast a spell and watch it twist into something else", xp=25)
feat_adv("conductor", "discovery/conduct", rune("jolt"), description="Shock five creatures at once through the water they stand in", branch="world", xp=25)
feat_adv("icebridge", "discovery/shatter", rune("icepath"), description="Walk across water you froze with a spell", branch="world", xp=15)
feat_adv("stormcaller", "world/ley_line", rune("lightning"), description="Cast 20 spells under a mana storm", xp=30)
feat_adv("stargazer", "world/ley_line", rune("starfall"), description="Loot a Fallen Star", frame="goal", xp=40)
feat_adv("reeled_in", "world/ley_line", rune("tidehook"), description="Fish a rune out of open water", xp=20)
feat_adv("riftwarden", "world/runebound", rune("banish"), description="Close a rift", frame="goal", xp=50)
feat_adv("chorus", "casting/first_cast", rune("echo"), description="Cast the same spell with other casters at the same moment", branch="world", xp=30)
feat_adv("kindred", "world/ley_line", rune("glimmer"), description="Bond with a wisp", xp=25)
feat_adv("menagerie", "world/kindred", rune("resonance"), description="Bond with a wisp of every element", frame="challenge", xp=150)
feat_adv("upgrade", "discovery/runes_10", rune("amplify"), description="Rank up a rune at the Fusion Altar", branch="altar", xp=20)
feat_adv("combine", "altar/upgrade", rune("prism"), description="Fuse two effects into a new one at the Fusion Altar", xp=25)
feat_adv("knot", "altar/combine", rune("chain"), description="Tie a whole spell into one rune", frame="goal", xp=50)
# Signature fusions: counted apart from the element fusions (a signature's key shares their "fusion:" prefix).
if SIGNATURES:
    adv("altar/signature", "altar/combine", rune("frostwire" if "frostwire" in SIGNATURES else next(iter(SIGNATURES))), "Signature",
        "Fuse two particular effects into a signature rune of their own", grimoire(signatures=1), xp=30)
    adv("altar/signatures", "altar/signature", rune("cometfall" if "cometfall" in SIGNATURES else next(iter(SIGNATURES))), "Hallmarks",
        f"Find {min(5, len(SIGNATURES))} signature fusions", grimoire(signatures=min(5, len(SIGNATURES))), frame="goal", xp=75, loot=["blank_runes"])
feat_adv("cinder_warden", "world/archivist", rune("inferno"), description="Defeat the Cinder Warden in its Ember Sanctum", frame="challenge", xp=300)
feat_adv("star_eater", "world/archivist", rune("eclipse"), description="Defeat the Star-Eater in its Astral Observatory", frame="challenge", xp=300)
feat_adv("tide_scribe", "world/archivist", rune("tidecall"), description="Defeat the Tide Scribe in its Drowned Scriptorium", frame="challenge", xp=300)
feat_adv("root_guardian", "world/archivist", rune("rootsnare"), description="Prune the Root Guardian and defeat it in the Rootbound Maze", frame="challenge", xp=300)
feat_adv("storm_conductor", "world/archivist", rune("thunderclap"), description="Ground the Storm Conductor and defeat it in the Storm Spire", frame="challenge", xp=300)
adv("shields/glyph", "shields/imbue", rune("mine"), "Tripwire", "Have one of your glyphs go off", moment("glyph"), xp=25)


def advancement_lang():
    lang = {}
    for a in ADVANCEMENTS:
        key = "advancements.wildercord." + a["path"].replace("/", ".")
        lang[key + ".title"] = a["title"]
        lang[key + ".description"] = a["description"]
    return lang


def write_advancements(runes):
    known = {r["path"] for r in runes}
    paths = [a["path"] for a in ADVANCEMENTS]
    assert len(paths) == len(set(paths)), "two advancements share an id"
    for a in ADVANCEMENTS:
        assert a["parent"] is None or a["parent"] in paths, f"{a['path']}: no parent {a['parent']}"
        component = a["icon"].get("components", {}).get("wildercord:rune")
        assert component is None or component.split(":")[1] in known, f"{a['path']}: no rune {component}"
    # Every feat in the Grimoire should have an advancement (AdvancementTreeTest fails the build without one).
    granted = {c["conditions"]["feat"] for a in ADVANCEMENTS
               for c in (a["criteria"].values() if "trigger" not in a["criteria"] else [a["criteria"]]) if c["trigger"] == "wildercord:feat"}
    for missing in sorted(set(FEATS) - granted):
        print(f"warning: the feat {missing} has no advancement: add a feat_adv line to ADVANCEMENTS")

    # Advancements this tool wrote before that no longer exist go (the recipe-book unlocks stay).
    for old in (DATA / "advancement").rglob("*.json"):
        if "recipes" not in old.relative_to(DATA / "advancement").parts:
            old.unlink()
    for name, (item_id, count) in ADVANCEMENT_REWARDS.items():
        entry = {"type": "minecraft:item", "name": item_id}
        if count > 1:
            entry["functions"] = [{"function": "minecraft:set_count", "count": count}]
        write_json(DATA / f"loot_table/advancement_reward/{name}.json", {"type": "minecraft:advancement_reward", "pools": [{"rolls": 1, "entries": [entry]}]})

    for a in ADVANCEMENTS:
        key = "advancements.wildercord." + a["path"].replace("/", ".")
        criteria = a["criteria"] if "trigger" not in a["criteria"] else {a["path"].split("/")[-1]: a["criteria"]}
        display = {"icon": a["icon"], "title": {"translate": key + ".title"}, "description": {"translate": key + ".description"}}
        if a["frame"] != "task":
            display["frame"] = a["frame"]
        for option in ("background", "show_toast", "announce_to_chat"):
            if option in a:
                display[option] = a[option]
        if a["hidden"]:
            display["hidden"] = True
        data = {}
        if a["parent"]:
            data["parent"] = f"wildercord:{a['parent']}"
        data["criteria"] = criteria
        data["display"] = display
        data["requirements"] = [list(criteria)] if a["any_of"] else [[name] for name in criteria]
        rewards = {}
        if a["xp"]:
            rewards["experience"] = a["xp"]
        if a["loot"]:
            rewards["loot"] = [f"wildercord:advancement_reward/{name}" for name in a["loot"]]
        if rewards:
            data["rewards"] = rewards
        write_json(DATA / f"advancement/{a['path']}.json", data)
    save(world_art.advancement_background(), ASSETS / "textures/gui/advancements/backgrounds/wildercord.png")
def write_world_events():
    """The Fallen Star (cast/events): its pulsing starstone faces, a squat block model and its blockstate. No item."""
    import event_art
    tex = ASSETS / "textures"
    for key, image in event_art.fallen_star_textures().items():
        save(image, tex / f"block/{key}.png")
    face = lambda t, uv: {"texture": f"#{t}", "uv": list(uv)}
    side = (2, 3, 14, 14)
    write_json(ASSETS / "models/block/fallen_star.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": "wildercord:block/fallen_star_side", "side": "wildercord:block/fallen_star_side",
                     "top": "wildercord:block/fallen_star_top", "bottom": "wildercord:block/fallen_star_bottom"},
        "elements": [{"from": [2, 0, 2], "to": [14, 11, 14], "faces": {
            "down": face("bottom", (2, 2, 14, 14)), "up": face("top", (2, 2, 14, 14)),
            "north": face("side", side), "south": face("side", side), "west": face("side", side), "east": face("side", side)}}]})
    write_json(ASSETS / "blockstates/fallen_star.json", {"variants": {"": {"model": "wildercord:block/fallen_star"}}})
# ---------------------------------------------------------------- casting gear: staffs, the tome and foci
# (the numbers live in src/main/java/dev/wildercord/gear/GearDef.java; the art in gear_art.py)

GEAR_ELEMENTS = ["fire", "frost", "storm", "wind", "earth", "life", "void", "arcane", "time", "blood"]
FOCI = ["haste", "thrift", "the_deep_well", "echoes", "resolve", "reprieve", "grounding"]

# A staff: its core (two of it, up the diagonal), two of its element's material, and a Mana Crystal at the head.
STAFF_MATERIALS = {
    "fire": "minecraft:blaze_powder", "frost": "minecraft:packed_ice", "storm": "minecraft:lightning_rod",
    "wind": "minecraft:wind_charge", "earth": "minecraft:mossy_cobblestone", "life": "minecraft:glistering_melon_slice",
    "void": "minecraft:ender_pearl", "arcane": "minecraft:amethyst_shard", "time": "minecraft:clock", "blood": "minecraft:nether_wart",
}
STAFF_CORES = {"fire": "minecraft:blaze_rod"}

# Foci: a Mana Crystal set in what the focus is for. The tome and greater staffs are found, never made.
FOCUS_RECIPES = {
    "haste": ({"F": "minecraft:feather", "S": "minecraft:sugar", "G": "minecraft:gold_ingot"}, [" F ", "SCS", " G "]),
    "thrift": ({"E": "minecraft:emerald", "G": "minecraft:gold_ingot"}, [" E ", "GCG", " G "]),
    "the_deep_well": ({"L": "minecraft:lapis_block", "D": "minecraft:polished_deepslate"}, [" L ", "DCD", " D "]),
    "echoes": ({"E": "minecraft:echo_shard", "A": "minecraft:amethyst_shard"}, [" E ", "ACA", " A "]),
    "resolve": ({"A": "minecraft:amethyst_shard", "I": "minecraft:iron_ingot"}, [" I ", "ACA", " I "]),
    "reprieve": ({"A": "minecraft:amethyst_shard", "T": "minecraft:clock"}, [" A ", "ACA", " T "]),
    "grounding": ({"I": "minecraft:iron_ingot", "L": "minecraft:lightning_rod"}, [" L ", "ICI", " I "]),
}

GEAR_LANG = {
    "item.wildercord.tome_of_the_fifth_page": "Tome of the Fifth Page",
    "item.wildercord.focus_of_haste": "Focus of Haste",
    "item.wildercord.focus_of_thrift": "Focus of Thrift",
    "item.wildercord.focus_of_the_deep_well": "Focus of the Deep Well",
    "item.wildercord.focus_of_echoes": "Focus of Echoes",
    "item.wildercord.focus_of_resolve": "Focus of Resolve",
    "item.wildercord.focus_of_reprieve": "Focus of Reprieve",
    "item.wildercord.focus_of_grounding": "Focus of Grounding",
    "tooltip.wildercord.gear.reprieve": "Delay 35% of a large spell hit into four real wounds. 8s recharge; 10% weaker spells.",
    "tooltip.wildercord.gear.grounding": "Resist 70% of a spell push or pull and gain a 2s escape gust. 8s recharge; 10% weaker spells.",
    "message.wildercord.reprieve.held": "Reprieve held %s damage; the debt begins in 1s",
    "message.wildercord.reprieve.debt": "Reprieve: %s damage remains",
    "message.wildercord.grounding": "Grounded! Escape gust released; recharging for 8s",
    "tooltip.wildercord.gear.staff": "%s spells: +%s%% power, %s%% less mana",
    "tooltip.wildercord.gear.tome": "A fifth spell, to thread and cast",
    "tooltip.wildercord.gear.haste": "Charged casts fill %s%% faster",
    "tooltip.wildercord.gear.thrift": "Spells cost %s%% less mana, but hit %s%% softer",
    "tooltip.wildercord.gear.deep_well": "+%s max mana",
    "tooltip.wildercord.gear.echoes": "A %s%% chance that a spell echoes: it goes off again, free",
    "tooltip.wildercord.gear.resolve": "Spells hurt you %s%% less, but your spell effects are %s%% weaker",
    "tooltip.wildercord.gear.flourish": "A charged spell of its element leaves it with a flourish",
    "tooltip.wildercord.gear.slot": "Goes in your inventory's %s",
    "tooltip.wildercord.gear.either_hand": "With the slot empty, it works held in either hand",
    "tooltip.wildercord.gear.off_hand": "With the slot empty, it works held in your off-hand",
    "gear_slot.wildercord.staff": "Staff slot",
    "gear_slot.wildercord.staff.hint": "Any staff, a greater one too",
    "gear_slot.wildercord.focus": "Focus slot",
    "gear_slot.wildercord.focus.hint": "Any focus",
    "gear_slot.wildercord.tome": "Tome slot",
    "gear_slot.wildercord.tome.hint": "The Tome of the Fifth Page",
    "screen.wildercord.gear.title": "Casting gear",
    "screen.wildercord.gear.piece": "  %s: %s",
    "screen.wildercord.gear.readout": "%s: %s",
    "screen.wildercord.server_cost": "This server's rules: spells cost x%s mana",
    "screen.wildercord.mana.max_gear": "  +%s from a Focus of the Deep Well",
    "screen.wildercord.mana.regen_server": "  x%s from this server's rules",
    "screen.wildercord.mana.way.gear": "A Focus of the Deep Well in your focus slot: +50 max mana",
    "screen.wildercord.tome_row": "The tome's spell: it casts while the tome is in its slot or your off-hand",
    "message.wildercord.tome_needed": "Spell 5 is the tome's: put the Tome of the Fifth Page in its slot (or hold it in your off-hand)",
    "command.wildercord.reloaded": "Reloaded %s",
    "command.wildercord.config_warning": "Config: %s",
    "key.wildercord.cast_5": "Cast spell 5 (the tome's)",
}
for _element in GEAR_ELEMENTS:
    GEAR_LANG[f"item.wildercord.{_element}_staff"] = f"{_element.capitalize()} Staff"
    GEAR_LANG[f"item.wildercord.greater_{_element}_staff"] = f"Greater {_element.capitalize()} Staff"


def write_gear_content():
    import gear_art
    tex = ASSETS / "textures/item"
    pieces = []
    for element in GEAR_ELEMENTS:
        pieces.append((f"{element}_staff", gear_art.staff_icon(element)))
        pieces.append((f"greater_{element}_staff", gear_art.staff_icon(element, True)))
    pieces.append(("tome_of_the_fifth_page", gear_art.tome_icon()))
    for focus in FOCI:
        pieces.append((f"focus_of_{focus}", gear_art.focus_icon(focus.replace("the_", ""))))
    # The hint each empty gear slot shows (one per slot in gear/GearSlot.java).
    for kind in gear_art.SLOT_ICONS:
        save(gear_art.slot_icon(kind), ASSETS / f"textures/gui/sprites/container/slot/gear_{kind}.png")
    for path, art in pieces:
        save(art if len(art) > 1 else art[0], tex / f"{path}.png")
        # Staffs are held like tools (angled in the hand); the tome and foci like any other item.
        parent = "minecraft:item/handheld" if path.endswith("_staff") else "minecraft:item/generated"
        write_json(ASSETS / f"models/item/{path}.json", {"parent": parent, "textures": {"layer0": f"wildercord:item/{path}"}})
        write_json(ASSETS / f"items/{path}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{path}"}})

    for element in GEAR_ELEMENTS:
        recipe_id = f"wildercord:{element}_staff"
        write_json(DATA / f"recipe/{element}_staff.json", {
            "type": "minecraft:crafting_shaped", "category": "equipment",
            "key": {"C": "wildercord:mana_crystal", "E": STAFF_MATERIALS[element], "R": STAFF_CORES.get(element, "minecraft:stick")},
            "pattern": [" EC", " RE", "R  "], "result": {"id": recipe_id}})
        unlock_advancement(recipe_id, "wildercord:mana_crystal")
    for focus, (key, pattern) in FOCUS_RECIPES.items():
        recipe_id = f"wildercord:focus_of_{focus}"
        write_json(DATA / f"recipe/focus_of_{focus}.json", {
            "type": "minecraft:crafting_shaped", "category": "equipment",
            "key": {"C": "wildercord:mana_crystal", **key}, "pattern": pattern, "result": {"id": recipe_id}})
        unlock_advancement(recipe_id, "wildercord:mana_crystal")


# ---------------------------------------------------------------- backpacks
# (the rules live in src/main/java/dev/wildercord/backpack/; the art in backpack_art.py)

# Each backpack, smallest first, with its colour before it's dyed (as in BackpackTier.java).
BACKPACKS = {"backpack": 0xA06540, "reinforced_backpack": 0x8C5A36, "runewoven_backpack": 0x6048A8}
# The bigger two are made from the one below them and keep what's in it (UpgradeRecipe.java): the old backpack
# comes first.
BACKPACK_UPGRADES = {
    "reinforced_backpack": ["wildercord:backpack", "minecraft:chest", *["minecraft:iron_ingot"] * 4, *["minecraft:leather"] * 2],
    "runewoven_backpack": ["wildercord:reinforced_backpack", "wildercord:mana_crystal", "minecraft:echo_shard",
                           *["minecraft:amethyst_shard"] * 4, *["wildercord:blank_rune"] * 2],
}

BACKPACK_LANG = {
    "item.wildercord.backpack": "Backpack",
    "item.wildercord.reinforced_backpack": "Reinforced Backpack",
    "item.wildercord.runewoven_backpack": "Runewoven Backpack",
    "tooltip.wildercord.backpack.fill": "Holds %s of %s stacks",
    "tooltip.wildercord.backpack.full": "Full: %s of %s stacks",
    "tooltip.wildercord.backpack.open": "Use it to open it, or wear it in the Backpack slot and press %s",
    "gear_slot.wildercord.backpack": "Backpack slot",
    "gear_slot.wildercord.backpack.hint": "A backpack, worn on your back: %s opens it",
    "key.wildercord.open_backpack": "Open backpack",
    "message.wildercord.backpack.none": "You aren't wearing a backpack: put one in the Backpack slot, on the tray above your inventory (E)",
    "screen.wildercord.backpack.open_here": "Open: it stays in this slot until you close it",
}


def write_backpack_content():
    import backpack_art
    backpack_art.main()
    for path, colour in BACKPACKS.items():
        # Two layers like leather armour: the first takes the backpack's colour (a dye's, or its own), the second doesn't.
        write_json(ASSETS / f"models/item/{path}.json", {"parent": "minecraft:item/generated", "textures": {
            "layer0": f"wildercord:item/{path}", "layer1": f"wildercord:item/{path}_overlay"}})
        write_json(ASSETS / f"items/{path}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{path}",
                                                             "tints": [{"type": "minecraft:dye", "default": (0xFF000000 | colour) - (1 << 32)}]}})
        # Dyed like leather armour, keeping what's inside; a cauldron washes the dye out.
        write_json(DATA / f"recipe/{path}_dyed.json", {
            "type": "minecraft:crafting_dye", "category": "equipment", "group": "dyed_backpack",
            "dye": "#minecraft:dyes", "target": f"wildercord:{path}", "result": {"id": f"wildercord:{path}"}})
        unlock_advancement(f"wildercord:{path}_dyed", f"wildercord:{path}")
    write_json(DATA / "recipe/backpack.json", {
        "type": "minecraft:crafting_shaped", "category": "equipment",
        "key": {"S": "minecraft:string", "L": "minecraft:leather", "C": "minecraft:chest"},
        "pattern": [" S ", "LCL", "LLL"], "result": {"id": "wildercord:backpack"}})
    unlock_advancement("wildercord:backpack", "minecraft:leather")
    for path, ingredients in BACKPACK_UPGRADES.items():
        write_json(DATA / f"recipe/{path}.json", {
            "type": "wildercord:upgrade", "category": "equipment",
            "ingredients": ingredients, "result": {"id": f"wildercord:{path}"}})
        unlock_advancement(f"wildercord:{path}", ingredients[0])
    backpacks = [f"wildercord:{path}" for path in BACKPACKS]
    write_json(DATA / "tags/item/backpacks.json", {"values": backpacks})
    # Nothing that holds items goes in a backpack (Backpacks.fitsInside); a data pack can add other mods' containers here.
    write_json(DATA / "tags/item/not_for_backpacks.json", {"values": ["#wildercord:backpacks", "#minecraft:shulker_boxes", "#minecraft:bundles"]})
    write_json(RES / "data/minecraft/tags/item/cauldron_can_remove_dye.json", {"replace": False, "values": backpacks})


if __name__ == "__main__":
    main()
