"""Aura, the swordsman's path: its manuals' covers, the blade's glow textures, its data and its English text.

Called by generate_assets.py (write(g) with the rest of the pack, LANG with the language file). Everything here is drawn by
code, like the rest of the mod's art: 16x16 pixel covers lit from the top left with a one-pixel outline, and soft white
textures for the blade's glow that each client tints with the aura's colour.

    python tools/aura_art.py      renders a review sheet of every manual into build/art-preview/aura_manuals.png
"""
import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent

# The ten built-in methods (aura.BreathingMethods): id, element, colour, highlight. Kept in step by AuraLangTest.
METHODS = [
    ("ember", "fire", 0xF06E32, 0xFFD060),
    ("rime", "frost", 0x8CDCFF, 0xE6FAFF),
    ("thunder", "storm", 0xFFE650, 0xFFFBE0),
    ("gale", "wind", 0xC8F0DC, 0xFFFFFF),
    ("stone", "earth", 0xC8A06A, 0xF0D8A8),
    ("verdant", "life", 0x6EDC64, 0xE8FFB0),
    ("hollow", "void", 0xB45AF0, 0xE0B0FF),
    ("starlit", "arcane", 0xE678DC, 0xFFD8FA),
    ("hourglass", "time", 0xF2D98A, 0xFFF8E0),
    ("crimson", "blood", 0xD2283C, 0xFF6474),
]


def rgb(c):
    return (c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF


def mix(a, b, t):
    return tuple(int(round(a[i] * (1 - t) + b[i] * t)) for i in range(3))


def shade(c, k):
    return tuple(max(0, min(255, int(round(v * k)))) for v in c)


# ---------------------------------------------------------------- the manuals

# A closed book, its cover in the method's colour: a dark spine down the left, a sunken panel holding the method's emblem,
# a gold band tied round it low down, and the cream edge of its pages along the right and the foot.
COVER = """
................
...ooooooooooo..
..oSHHHHHHHHHHo.
..oSHccccccccCpo
..oSCcEEEEEEcCpo
..oSCcEEEEEEcCpo
..oSCcEEEEEEcCpo
..oSCcEEEEEEcCpo
..oSCcEEEEEEcCpo
..oSCccccccccCpo
..oSBbBBBBBBBbpo
..oSCCCCCCCCCCpo
..oSCCCCCCCCCLpo
..oSLLLLLLLLLLPo
...oPPPPPPPPPPPo
....ooooooooooo.
"""

# Each method's emblem, 6x5, in the panel (x 6-11, y 4-8). '#' is the emblem in the highlight, '+' a softer stroke.
EMBLEMS = {
    "ember": [
        "..#...",
        "..##..",
        ".###+.",
        ".####.",
        "..##..",
    ],
    "rime": [
        "#.#.#.",
        ".###..",
        "#####.",
        ".###..",
        "#.#.#.",
    ],
    "thunder": [
        "...##.",
        "..##..",
        ".####.",
        "..##..",
        ".##...",
    ],
    "gale": [
        "####..",
        "....#.",
        ".###..",
        "#.....",
        ".####.",
    ],
    "stone": [
        "......",
        "..#...",
        ".###..",
        "#####.",
        "#+###.",
    ],
    "verdant": [
        "...##.",
        "..###.",
        ".###..",
        "##+...",
        "#.....",
    ],
    "hollow": [
        ".###..",
        "#...#.",
        "#.+.#.",
        "#...#.",
        ".###..",
    ],
    "starlit": [
        "..#...",
        "..#...",
        "#####.",
        ".#.#..",
        "#...#.",
    ],
    "hourglass": [
        "#####.",
        ".###..",
        "..#...",
        ".#+#..",
        "#####.",
    ],
    "crimson": [
        "..#...",
        ".###..",
        ".###..",
        "####+.",
        ".###..",
    ],
    "unknown": [
        ".###..",
        "#...#.",
        "...#..",
        "......",
        "..#...",
    ],
}

PAGE = (0xEC, 0xE0, 0xC4)
PAGE_SHADOW = (0xBF, 0xAE, 0x88)
BAND = (0xD8, 0xB0, 0x5A)
BAND_DARK = (0x9A, 0x74, 0x30)


def manual_icon(method_id, color, highlight):
    cover = rgb(color)
    # Light covers (gale, rime) are deepened a little so the book reads as a book.
    lum = 0.3 * cover[0] + 0.59 * cover[1] + 0.11 * cover[2]
    base = shade(cover, 0.62 if lum > 190 else 0.8)
    pal = {
        "o": shade(base, 0.32),
        "S": shade(base, 0.55),
        "H": mix(base, (255, 255, 255), 0.22),
        "C": base,
        "L": shade(base, 0.78),
        "c": shade(base, 0.6),
        "E": shade(base, 0.48),
        "B": BAND,
        "b": BAND_DARK,
        "p": PAGE,
        "P": PAGE_SHADOW,
    }
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rows = [r for r in COVER.strip("\n").split("\n")]
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x, y), pal[ch] + (255,))
    glow = rgb(highlight)
    soft = mix(glow, pal["E"], 0.45)
    for y, row in enumerate(EMBLEMS.get(method_id, EMBLEMS["unknown"])):
        for x, ch in enumerate(row):
            if ch == "#":
                img.putpixel((6 + x, 4 + y), glow + (255,))
            elif ch == "+":
                img.putpixel((6 + x, 4 + y), soft + (255,))
    return img


# ---------------------------------------------------------------- the blade's glow

def white(size=4):
    """A plain white texture: the haze and ripples are drawn by vertex colour, soft at their edges."""
    return Image.new("RGBA", (size, size), (255, 255, 255, 255))


def soft():
    """The haze round the whole weapon: a soft, long glow, brightest along its middle and fading to nothing at its edges and
    ends, so drawn stretched along the blade it reads as aura hanging round it rather than a shape."""
    w, h = 32, 64
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    for y in range(h):
        v = (y + 0.5) / h * 2 - 1
        for x in range(w):
            u = (x + 0.5) / w * 2 - 1
            d = min(1.0, (u * u + v * v * 0.55) ** 0.5)
            a = (1 - d) ** 1.7
            img.putpixel((x, y), (255, 255, 255, int(round(255 * a))))
    return img


def crystal():
    """The Edge's crystal blade, white for the client to tint: bright facet edges, a ridge down the middle, a clear body
    with faint inner planes, a few glints. 32 across (u: from one edge of a face to the other) by 64 along the blade."""
    w, h = 32, 64
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    for y in range(h):
        along = y / (h - 1)
        for x in range(w):
            u = x / (w - 1)
            edge = max(0.0, 1.0 - min(u, 1 - u) * 9.0)
            ridge = max(0.0, 1.0 - abs(u - 0.5) * 14.0)
            plane = 0.12 if (x + y // 3) % 11 == 0 else 0.0
            a = 0.34 + 0.5 * edge + 0.32 * ridge + plane
            # Toward the point the crystal clears.
            a *= 0.75 + 0.25 * (1 - along)
            v = int(round(225 + 30 * max(edge, ridge)))
            img.putpixel((x, y), (v, v, 255 if v > 240 else v + 6, int(round(255 * min(1.0, a)))))
    for gx, gy in ((9, 12), (22, 30), (13, 47), (19, 7)):
        for dx, dy, a in ((0, 0, 255), (1, 0, 200), (-1, 0, 200), (0, 1, 200), (0, -1, 200)):
            img.putpixel((gx + dx, gy + dy), (255, 255, 255, a))
    return img


def shell():
    """Aura armour's shell, laid over the player's skin layout (64x64), white for the client to tint: a faint even skin with a
    lattice of brighter diamonds woven through it, so the shell reads as a mesh of aura rather than a flat colour."""
    w = h = 64
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    for y in range(h):
        for x in range(w):
            # Two sets of diagonals, four pixels apart: a diamond lattice.
            d1 = (x + y) % 4
            d2 = (x - y) % 4
            line = 1.0 if d1 == 0 or d2 == 0 else 0.0
            cross = 1.0 if d1 == 0 and d2 == 0 else 0.0
            a = 0.28 + 0.42 * line + 0.3 * cross
            v = 235 + int(20 * line)
            img.putpixel((x, y), (v, v, 255, int(round(255 * min(1.0, a)))))
    return img


# ---------------------------------------------------------------- writing it out

def write(g):
    tex = g.ASSETS / "textures/item/breathing_manual"
    cases = []
    for method_id, element, color, highlight in METHODS:
        g.save(manual_icon(method_id, color, highlight), tex / f"{method_id}.png")
        g.item_model(f"breathing_manual/{method_id}", f"breathing_manual/{method_id}")
        cases.append({"when": method_id, "model": {"type": "minecraft:model", "model": f"wildercord:item/breathing_manual/{method_id}"}})
    g.save(manual_icon("unknown", 0x8A84A0, 0xD8D0F0), tex / "unknown.png")
    g.item_model("breathing_manual/unknown", "breathing_manual/unknown")
    g.write_json(g.ASSETS / "items/breathing_manual.json", {"model": {
        "type": "minecraft:select", "property": "minecraft:component", "component": "wildercord:breathing_method", "cases": cases,
        "fallback": {"type": "minecraft:model", "model": "wildercord:item/breathing_manual/unknown"}}})

    glow = g.ASSETS / "textures/entity/aura"
    g.save(white(), glow / "haze.png")
    g.save(crystal(), glow / "crystal.png")
    g.save(soft(), glow / "soft.png")
    g.save(shell(), glow / "shell.png")

    # What carries aura: servers and add-ons extend it.
    g.write_json(g.DATA / "tags/item/aura_weapons.json", {"replace": False, "values": [
        "#minecraft:swords", "#minecraft:axes", "#minecraft:spears", "minecraft:trident", "minecraft:mace"]})
    # Aura off the blade (the slash, a spark): armour applies; against players it goes through the spell defences in code.
    g.write_json(g.DATA / "damage_type/aura.json", {"exhaustion": 0.1, "message_id": "wildercord.aura", "scaling": "when_caused_by_living_non_player"})
    # Masters hand their methods down: a manual at the weaponsmith's and the cleric's last level.
    for profession, emeralds in (("weaponsmith", 24), ("cleric", 22)):
        g.write_json(g.DATA / f"villager_trade/aura/{profession}_manual.json", {
            "additional_wants": {"id": "minecraft:book"},
            "given_item_modifier": {"type": "wildercord:random_breathing_method", "source": profession},
            "gives": {"id": "wildercord:breathing_manual"},
            "max_uses": 2,
            "reputation_discount": 0.05,
            "wants": {"count": emeralds, "id": "minecraft:emerald"},
            "xp": 30,
        })
        g.write_json(g.RES / f"data/minecraft/tags/villager_trade/{profession}/level_5.json",
                     {"replace": False, "values": [f"wildercord:aura/{profession}_manual"]})


# ---------------------------------------------------------------- English

def _methods_lang():
    names = {
        "ember": ("Ember Breath", "Breathe as a banked fire breathes: slow, deep, and hot at the heart.",
                  "Ember: from Flow a coated blow may set its foe alight, and from Edge it always does."),
        "rime": ("Rime Breath", "Breathe as frost settles: so still the air forgets to move.",
                 "Rime: a coated blow slows its foe, a little longer at each stage."),
        "thunder": ("Thunder Breath", "Breathe in the moment before the storm breaks, and hold it there.",
                    "Thunder: a coated blow may throw a spark to another foe nearby."),
        "gale": ("Gale Breath", "Breathe as the high wind breathes: never twice in the same place.",
                 "Gale: a little more speed while you're in a fight, more at each stage."),
        "stone": ("Stone Breath", "Breathe as the mountain does, once a century, and do not move.",
                  "Stone: blows push you back less with an aura weapon in hand."),
        "verdant": ("Verdant Breath", "Breathe as the green things do, taking in the light and giving it back.",
                    "Verdant: a coated blow mends you a little."),
        "hollow": ("Hollow Breath", "Breathe out until nothing is left, and let the hollow pull.",
                   "Hollow: foes near the one you strike are drawn toward it."),
        "starlit": ("Starlit Breath", "Breathe in time with the far lights, and they lend you theirs.",
                    "Starlit: your aura gathers faster, from blows and the breathing stance alike."),
        "hourglass": ("Hourglass Breath", "Breathe in the space between one grain falling and the next.",
                      "Hourglass: a little more attack speed with an aura weapon in hand."),
        "crimson": ("Crimson Breath", "Breathe to the beat of the blood, and the blood answers.",
                    "Crimson: a coated blow drinks a little of what it takes, for a little more aura."),
    }
    out = {}
    for method_id, (name, lore, flavour) in names.items():
        out[f"aura.wildercord.method.{method_id}"] = name
        out[f"aura.wildercord.method.{method_id}.lore"] = lore
        out[f"aura.wildercord.method.{method_id}.flavour"] = flavour
    return out


LANG = {
    "item.wildercord.breathing_manual": "Breathing Manual",
    "item.wildercord.breathing_manual.named": "%s Manual",
    "tooltip.wildercord.breathing_manual.element": "Its aura carries %s",
    "tooltip.wildercord.breathing_manual.use": "Use to learn this breathing method",
    "tooltip.wildercord.breathing_manual.unknown": "Its pages have faded past reading",
    **_methods_lang(),
    "aura.wildercord.stage.none": "No aura",
    "aura.wildercord.stage.glow": "Glow",
    "aura.wildercord.stage.flow": "Flow",
    "aura.wildercord.stage.edge": "Edge",
    "aura.wildercord.stage.form": "Form",
    "aura.wildercord.stage.sovereign": "Sovereign",
    "aura.wildercord.stage.glow.desc": "Aura coats the blade: coated blows land harder. In the breathing stance you sense hostile creatures nearby.",
    "aura.wildercord.stage.flow.desc": "Aura flows: every aura weapon sweeps, wider and further, and your blade can guard.",
    "aura.wildercord.stage.edge.desc": "Aura sets into a blade of its own: more reach, part of each blow through armour, and the slash.",
    "aura.wildercord.stage.form.desc": "Aura leaves the body: you can step through the air in an instant, aura armours you, and weaker foes falter before you.",
    "aura.wildercord.stage.sovereign.desc": "Your aura claims the ground itself: raise a Dominion, where foes are slowed and weakened and your blows chain.",
    "aura.wildercord.technique.coat": "Aura Coat",
    "aura.wildercord.technique.coat.desc": "With aura held, every blow of an aura weapon is coated: it lands 10% harder and carries your method's element, for half a point of aura.",
    "aura.wildercord.technique.sense": "Aura Sense",
    "aura.wildercord.technique.sense.desc": "In the breathing stance, each breath outlines the hostile creatures within 16 blocks, for you alone.",
    "aura.wildercord.technique.sweep": "Flowing Cut",
    "aura.wildercord.technique.sweep.desc": "A full, steady swing of any aura weapon sweeps, reaching wider and further and carrying more of the blow.",
    "aura.wildercord.technique.guard": "Aura Guard",
    "aura.wildercord.technique.guard.desc": "Sneak and press the Aura key: hold sneak to keep it up (two seconds at most). It halves blows and arrows from in front, paying aura for what it takes. Raised just as the blow comes, it's a perfect guard: the blow is turned and its attacker staggered, an arrow flies back, a spell is parried.",
    "aura.wildercord.technique.edge": "Crystal Edge",
    "aura.wildercord.technique.edge.desc": "A blade of solid aura: a block more reach, and a quarter of each coated blow goes through armour.",
    "aura.wildercord.technique.slash": "Aura Slash",
    "aura.wildercord.technique.slash.desc": "Tap the Aura key: the blade looses a crescent of aura that cuts everything in its path, carrying your element. Against players it meets their spell defences.",
    "aura.wildercord.technique.marks": "Aura Marks",
    "aura.wildercord.technique.marks.desc": "An elemental strike sometimes leaves its element's reaction mark (frozen, windswept, shadowed, bleeding, exposed, burning or poisoned) for a mage's spell to set off. Earth, storm and time leave none. A better chance at each stage.",
    "aura.wildercord.technique.spellblade": "Spellblade",
    "aura.wildercord.technique.spellblade.desc": "Cast a spell while sneaking with a blade in hand: it flows into the blade instead of leaving. Your next Aura Slash within 5 seconds carries it, landing it on the first foes it cuts. You pay both prices. Unused, it leaves as cast.",
    "aura.wildercord.technique.step": "Aura Step",
    "aura.wildercord.technique.step.desc": "Double-tap the Aura key: aura carries you about six blocks in an instant, the way you're moving, leaving afterimages behind. Nothing can touch you for a moment. It never passes through walls or a dungeon's warded doors.",
    "aura.wildercord.technique.armour": "Aura Armour",
    "aura.wildercord.technique.armour.desc": "While you hold 20 aura or more, a faint shell of aura takes a quarter of the harm that reaches you, paying aura for it.",
    "aura.wildercord.technique.intent": "Intent",
    "aura.wildercord.technique.intent.desc": "With a blade in hand, weaker hostile creatures nearby (less health than you, or a lower aura stage) are slowed and falter. Other players feel it as a shadow at the edge of sight and a slight slow.",
    "aura.wildercord.technique.dominion": "Dominion",
    "aura.wildercord.technique.dominion.desc": "Hold the Aura key: a circle of your aura six blocks across holds for 8 seconds. Foes inside are slowed and hit weaker, your blows on them chain to another foe inside, and your aura flows back twice as fast. Rests for 90 seconds.",
    "key.wildercord.aura": "Aura (tap: slash, sneak: guard, double-tap: step, hold: dominion)",
    "message.wildercord.aura.step_blocked": "There's no room to step that way",
    "message.wildercord.aura.dominion": "Dominion",
    "message.wildercord.aura.dominion_resting": "Your Dominion gathers again: %s s",
    "message.wildercord.aura.spellblade": "The spell flows into your blade: Aura Slash to loose it",
    "message.wildercord.aura.tempest_begins": "The storm breaks over the ley lines: hold still",
    "message.wildercord.aura.guardian_begins": "A guardian: fell it with your blade alone",
    "message.wildercord.aura.waiting_how_top": "Break through: hold the breathing stance where ley lines cross through a thunderstorm, or fell a dungeon's guardian (or any boss) with your blade alone.",
    "screen.wildercord.aura.trial.tempest": "The tempest: hold the breathing stance where ley lines cross, under a thunderstorm and open to the sky (45 s for Form, 60 s for Sovereign)",
    "screen.wildercord.aura.trial.guardian": "A guardian: fell a boss, a dungeon's guardian or another, by blade alone, within three minutes",
    "screen.wildercord.aura.trial.duel": "A duel: win an aura duel against a duelist",
    "screen.wildercord.aura.key_spellblade": "Sneak + %s, then %s",
    "screen.wildercord.aura.spellblade_held": "Spell on the blade: %s s",
    "screen.wildercord.aura.dominion_left": "Dominion: %s s",
    "toast.wildercord.aura.dominion": "Dominion",
    "toast.wildercord.aura.spellblade": "Spellblade",
    "message.wildercord.aura.disabled": "Aura is switched off on this server",
    "message.wildercord.aura.no_method": "You haven't learned a breathing method: read a Breathing Manual",
    "message.wildercord.aura.no_weapon": "Your aura needs a blade in hand (a sword, axe, spear, trident or mace)",
    "message.wildercord.aura.not_yet": "%s opens at %s",
    "message.wildercord.aura.backlash": "Your aura gutters out: you're spent",
    "message.wildercord.aura.beat": "A breath on the beat",
    "message.wildercord.aura.perfect_guard": "Perfect guard!",
    "message.wildercord.aura.already": "You already breathe %s",
    "message.wildercord.aura.switch_confirm": "You breathe %1$s. Switching to %2$s keeps your stage but loses the road to your next breakthrough, and empties your aura. Read the manual again to switch.",
    "message.wildercord.aura.learned": "You learn %s.",
    "message.wildercord.aura.switched": "You turn to %s.",
    "message.wildercord.aura.learned_subtitle": "Breathing method learned",
    "message.wildercord.aura.switched_subtitle": "Breathing method changed",
    "message.wildercord.aura.learned_how": "Land full swings with a blade to gather aura, or sneak and stand still with it in hand to breathe it in. Open the Cord screen (or press K) and choose Aura to see your path.",
    "message.wildercord.aura.waiting": "Your aura presses against its limit: %s waits.",
    "message.wildercord.aura.waiting_how": "Break through: hold the breathing stance for half a minute where ley lines cross, or fell a foe stronger than you with your blade alone.",
    "message.wildercord.aura.stillness_begins": "The ley lines hum under you: hold still",
    "message.wildercord.aura.trial_begins": "A worthy foe: fell it with your blade alone",
    "message.wildercord.aura.trial_tainted": "Magic touched that fight: it doesn't count",
    "message.wildercord.aura.breakthrough": "Breakthrough: your aura reaches %s.",
    "message.wildercord.aura.breakthrough_subtitle": "Breakthrough",
    "toast.wildercord.aura": "Aura",
    "toast.wildercord.aura.glow": "Glow: your blade wakes",
    "toast.wildercord.aura.flow": "Breakthrough: Flow",
    "toast.wildercord.aura.edge": "Breakthrough: Edge",
    "toast.wildercord.aura.form": "Breakthrough: Form",
    "toast.wildercord.aura.sovereign": "Breakthrough: Sovereign",
    "toast.wildercord.aura.perfect_guard": "Perfect Guard",
    "screen.wildercord.aura.title": "Aura",
    "screen.wildercord.aura.badge": "Aura: the swordsman's path",
    "screen.wildercord.aura.none": "You haven't learned a breathing method.",
    "screen.wildercord.aura.none_hint": "Breathing Manuals are found in old battlegrounds (trial chambers, strongholds, ancient cities, the Archive and expedition vaults) and sold by master weaponsmiths and clerics.",
    "screen.wildercord.aura.disabled": "Aura is switched off on this server.",
    "screen.wildercord.aura.method": "Method: %s",
    "screen.wildercord.aura.element": "Element: %s",
    "screen.wildercord.aura.stage": "Stage: %s",
    "screen.wildercord.aura.held": "Aura: %s / %s",
    "screen.wildercord.aura.xp": "Experience: %s / %s",
    "screen.wildercord.aura.xp_max": "Experience: %s",
    "screen.wildercord.aura.road": "The road to %s",
    "screen.wildercord.aura.road_closed": "%s isn't open yet",
    "screen.wildercord.aura.ready": "A breakthrough waits",
    "screen.wildercord.aura.trials": "Trials",
    "screen.wildercord.aura.trial.stillness": "Stillness: hold the breathing stance for 30 seconds where ley lines cross",
    "screen.wildercord.aura.trial.stronger_foe": "A stronger foe: fell a boss, a Runebound or a creature with twice your health, by blade alone, within a minute",
    "screen.wildercord.aura.trial.progress": "Stillness held: %s s of %s",
    "screen.wildercord.aura.trial.foe_left": "Worthy foe: %s s left",
    "screen.wildercord.aura.techniques": "Techniques",
    "screen.wildercord.aura.locked": "Opens at %s",
    "screen.wildercord.aura.cost": "%s aura",
    "screen.wildercord.aura.key_tap": "Tap %s",
    "screen.wildercord.aura.key_sneak": "Sneak + %s",
    "screen.wildercord.aura.key_double": "Double-tap %s",
    "screen.wildercord.aura.key_hold": "Hold %s",
    "screen.wildercord.aura.passive": "Always",
    "screen.wildercord.aura.stance": "Breathing stance",
    "screen.wildercord.aura.stance_how": "Sneak and stand still with a blade in hand. Let sneak up and press it again as the ring closes: a breath on the beat draws in more.",
    "screen.wildercord.aura.flavour": "Passive",
    "command.wildercord.aura.state": "Aura: %s, %s, %s experience, %s / %s aura",
    "command.wildercord.aura.no_method": "No such breathing method (or none learned yet)",
    "death.attack.wildercord.aura": "%1$s was cut down by %2$s's aura",
    "death.attack.wildercord.aura.player": "%1$s was cut down by %2$s's aura",
    "death.attack.wildercord.aura.item": "%1$s was cut down by %2$s's aura through %3$s",
}


def preview():
    sheet = Image.new("RGBA", (16 * 11 * 4, 16 * 4), (40, 34, 52, 255))
    for i, (method_id, _, color, highlight) in enumerate(METHODS + [("unknown", "", 0x8A84A0, 0xD8D0F0)]):
        icon = manual_icon(method_id, color, highlight).resize((64, 64), Image.NEAREST)
        sheet.alpha_composite(icon, (i * 64, 0))
    out = ROOT / "build/art-preview"
    out.mkdir(parents=True, exist_ok=True)
    sheet.save(out / "aura_manuals.png")
    crystal().resize((128, 256), Image.NEAREST).save(out / "aura_crystal.png")
    print(f"wrote {out / 'aura_manuals.png'}")


if __name__ == "__main__":
    preview()
