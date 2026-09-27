"""Generates Wildercord's item art, item models, English text and recipes.

Reads the rune roster straight from Runes.java so the two can never drift apart.
Run from the project root:  python tools/generate_assets.py
"""
import hashlib
import json
import math
import re
from pathlib import Path

from PIL import Image

try:  # Hand-drawn icons (tools/item_art.py); the procedural stones below are only a fallback.
    import item_art
except ImportError:
    item_art = None

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


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


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
    meta.write_text(json.dumps({"animation": {"frametime": getattr(item_art, "ANIMATION_FRAMETIME", 3)}}, indent=2) + "\n", encoding="utf-8")


def item_model(path, texture):
    write_json(ASSETS / f"models/item/{path}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"wildercord:item/{texture}"}})


def main():
    runes = read_runes()
    tex = ASSETS / "textures/item"

    # Runes.
    cases = []
    for r in runes:
        accent = FAMILY_COLOR[r["family"]] if r["family"] != "effect" else ELEMENT_COLOR[r["element"]]
        art = item_art.rune_icon(r["path"], r["family"], r["element"], r["tier"]) if item_art else stone(r["family"], accent, glyph_for(r["path"]))
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
    else:
        crystal = stone("effect", (0x9C, 0x7C, 0xFF), None, body=(0x5A, 0x3C, 0xC8))
        save(crystal, tex / "mana_crystal.png")
        for name in ("clarity", "mana"):
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
    print(f"generated art for {len(runes)} runes, {len(CORDS)} cords")


# ---------------------------------------------------------------- text


def write_lang(runes):
    lang = {
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
        "tooltip.wildercord.silent": "Its add-on is missing (%s). It stays safe and wakes up when the add-on returns.",
        "tooltip.wildercord.cord.stats": "%s sockets per spell · %s spell(s) · %s mana",
        "tooltip.wildercord.cord.wear": "Wear it in the Cord slot above your offhand",
        "message.wildercord.already_known": "You already know %s",
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
        "key.wildercord.cast": "Cast spell",
        "key.wildercord.next_spell": "Next spell",
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
        "screen.wildercord.help.keys": "%s casts · %s switches spell · %s opens this screen",
        "screen.wildercord.row_locked": "Needs a %s",
        "screen.wildercord.row_kept": "%s runes kept",
        "screen.wildercord.quiet_silent": "Its add-on is missing, so it stays quiet",
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
        "screen.wildercord.not_sustainable": "Can't be a passive: only lasting buffs, wards and Orbit or Stand auras",
        "screen.wildercord.passive_locked": "Opens with the %s Heart Circle",
        "screen.wildercord.passive.on": "On",
        "screen.wildercord.passive.off": "Off",
        "screen.wildercord.passive.switch": "Click to switch this passive on or off",
        "screen.wildercord.passive.upkeep": "%s/s",
        "screen.wildercord.passive.upkeep_hint": "Mana per second to keep it running. Passives have no cooldown.",
        "screen.wildercord.quiet_passive_socket": "Passives hold %s runes",
        "screen.wildercord.passive.summary": "Passives drain %s mana/s · you regenerate %s/s",
        "screen.wildercord.passive.locked_hint": "Form Heart Circles to open passive slots: the 1st and 5th Circle each open one. Hover the heart above to see your progress.",
        "screen.wildercord.passive.stand_slow": "As a passive, the Stand strikes %sx less often.",
        "screen.wildercord.passive.empty": "Empty passive. Thread runes that can be sustained.",
        "screen.wildercord.passive.rules": "Self, Orbit or Stand, then lasting buffs (Swift, Stoneskin, Infinity, Reflect...). Damage like Shock or Dismantle needs Orbit or Stand to carry it. No links and no cooldown: it costs mana every second instead.",
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
        "screen.wildercord.heart.breakthrough.runes_15": "Breakthrough: know 15 runes",
        "screen.wildercord.heart.breakthrough.amethyst": "Breakthrough: wear an Amethyst Cord or better",
        "screen.wildercord.heart.breakthrough.boss": "Breakthrough: help slay a boss (Wither, Warden, Elder Guardian or Ender Dragon)",
        "screen.wildercord.heart.breakthrough.archmage": "Breakthrough: wear an Echo Cord and know 100 runes",
        "screen.wildercord.heart.ready": "Ready! Meditate (sneak and stand still) for 10 seconds without getting hurt to form it.",
        "screen.wildercord.heart.need.runes": "Know %s runes (%s)",
        "screen.wildercord.heart.need.cord": "Wear a %s or better",
        "screen.wildercord.heart.need.kills": "Defeat %s monsters with spells (%s)",
        "screen.wildercord.heart.need.boss": "Help slay a boss (Wither, Warden, Elder Guardian or Ender Dragon)",
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
        "message.wildercord.zipper_no_wall": "There's no wall in front of you to unzip",
        "message.wildercord.zipper_unbreakable": "That can't be unzipped",
        "message.wildercord.zipper_thick": "Too thick to unzip (6 blocks at most)",
        "message.wildercord.too_many_birds": "You can only keep %s Thunderbirds at once",
        "message.wildercord.rewind_dimension": "Rewind can't reach into another dimension",
        "message.wildercord.time_resumes": "Time resumes: %s damage from %s hits lands at once",
        "message.wildercord.reversal": "Reversal! Death turned back",
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
    write_recipe_doc(runes)
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

    for table, chance, body in re.findall(r"RUNE_POOLS\.put\(BuiltInLootTables\.(\w+), new RunePool\((\d+),\s*(.*?)\)\);", src, re.S):
        consts_in = named_lists.get(body.strip(), None) or re.findall(r"Runes\.(\w+)", body)
        for c in consts_in:
            add(c, LOOT_TABLE_NAMES.get(table, table.replace("_", " ").title()))
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
    for tier in (1, 2, 3):
        rs = [r for r in runes if r["tier"] == tier]
        lines += [f"## Tier {['I', 'II', 'III'][tier - 1]} ({len(rs)} runes, + {extras[tier]})", "",
                  "| Rune | Family | Items |", "|---|---|---|"]
        for r in sorted(rs, key=lambda r: (r["family"], r["name"])):
            items = recipe_text(["wildercord:blank_rune", *RUNE_RECIPES[r["path"]]])
            lines.append(f"| {r['name']} | {r['family'].title()} | {items} |")
        lines.append("")
    t4 = [r for r in runes if r["tier"] == 4]
    lines += [f"## Tier IV ({len(t4)} runes, found only)", "", "| Rune | Family | Found |", "|---|---|---|"]
    for r in sorted(t4, key=lambda r: (r["family"], r["name"])):
        lines.append(f"| {r['name']} | {r['family'].title()} | {', '.join(found.get(r['path'], ['?']))} |")
    lines += ["", "Chests favour low tiers: next to each other in a pool, Tier I runes are 8x as likely as Tier IV,",
              "Tier II 5x and Tier III 2x.", ""]
    (ROOT / "docs/RECIPES.md").write_text("\n".join(lines), encoding="utf-8")


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
    "crescent": ["minecraft:iron_sword"],
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
    "stand": ["minecraft:armor_stand", "minecraft:soul_lantern"],
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
    # Starters, so a lost Cord owner can always craft them back.
    "self": ["minecraft:glass_pane"],
    "bolt": ["minecraft:arrow"],
    "push": ["minecraft:piston"],
}


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
    out = DATA / "recipe"
    by_path = {r["path"]: r for r in runes}
    for path in RUNE_RECIPES:
        assert path in by_path, path
    for r in runes:
        # Every Tier I-III rune can be crafted; Tier IV is found only (bosses and rare chests).
        assert (r["path"] in RUNE_RECIPES) == (r["tier"] <= 3), f"{r['path']} (tier {r['tier']})"
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
    for new, old, extra in upgrades:
        write_json(out / f"{new}_cord.json", {
            "type": "minecraft:crafting_shapeless", "category": "equipment",
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

POTIONS = ["clarity", "long_clarity", "strong_clarity", "mana", "strong_mana"]
BREWS = [("minecraft:awkward", "minecraft:amethyst_shard", "clarity"),
         ("wildercord:clarity", "minecraft:redstone", "long_clarity"),
         ("wildercord:clarity", "minecraft:glowstone_dust", "strong_clarity"),
         ("minecraft:awkward", "minecraft:lapis_lazuli", "mana"),
         ("wildercord:mana", "minecraft:glowstone_dust", "strong_mana")]


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
    # Joining non_treasure puts them in the enchanting table, villager trades and random loot.
    write_json(RES / "data/minecraft/tags/enchantment/non_treasure.json",
               {"replace": False, "values": [f"wildercord:{e}" for e in ENCHANTMENTS]})

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


if __name__ == "__main__":
    main()
