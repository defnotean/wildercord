"""Aura, the swordsman's path: its manuals' covers, the blade's glow textures, aura armour's shell, its data and its English text.

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


# The player model's boxes on its skin (texture offset, then width, height, depth in pixels), as vanilla lays them out: the
# head, the body, the arms (four wide, or three for the slim model) and the legs. The shell is the same model a little larger.
def _player_boxes(slim):
    arm = 3 if slim else 4
    return [((0, 0), (8, 8, 8)), ((16, 16), (8, 12, 4)), ((40, 16), (arm, 12, 4)), ((32, 48), (arm, 12, 4)),
            ((0, 16), (4, 12, 4)), ((16, 48), (4, 12, 4))]


def _faces(u, v, w, h, d):
    """A box's six faces on its texture, as (x, y, width, height): top, bottom, then its four sides round from the right."""
    return [(u + d, v, w, d), (u + d + w, v, w, d), (u, v + d, d, h), (u + d, v + d, w, h), (u + d + w, v + d, d, h),
            (u + d + w + d, v + d, w, h)]


def shell(slim=False):
    """Aura armour's shell, laid over the player's skin layout (64x64), white for the client to tint: every face of the body
    clear and faint in its middle and bright toward its edges, so drawn over the body the shell reads as light gathering at
    its outline, the way a glow does, rather than a skin of colour."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for (u, v), (w, h, d) in _player_boxes(slim):
        for fx, fy, fw, fh in _faces(u, v, w, h, d):
            for y in range(fy, fy + fh):
                for x in range(fx, fx + fw):
                    # How far in from the face's nearest edge, in pixels (0 on the edge itself).
                    inward = min(x - fx, fx + fw - 1 - x, y - fy, fy + fh - 1 - y)
                    a = 0.16 + 0.84 * (0.5 ** (inward * 1.6))
                    v_ = 236 + int(19 * (0.5 ** inward))
                    img.putpixel((x, y), (v_, v_, 255, int(round(255 * min(1.0, a)))))
    return img


# ---------------------------------------------------------------- the body's aura and the technique banner

def _smooth(x):
    x = max(0.0, min(1.0, x))
    return x * x * (3 - 2 * x)


def _flame(img, ox, w, h, frame):
    """One tongue of fire, white for the client to tint, standing on the bottom of its cell: a rounded foot, widest a quarter
    of the way up and filling most of the cell, licking to a point that leans one way and the other through the four frames, its
    heart brighter low down."""
    import math
    phase = frame * math.pi / 2
    for py in range(h):
        y = 1.0 - (py + 0.5) / h  # 0 at the foot, 1 at the tip
        if y < 0.26:
            half = 0.86 * (y / 0.26) ** 0.5
        else:
            half = 0.86 * ((1 - y) / 0.74) ** 1.35
        half *= 0.93 + 0.07 * math.sin(phase * 1.3 + 1.1 + y * 2)
        centre = 0.2 * math.sin(y * 3.4 + phase) * y ** 1.2
        for px in range(w):
            x = (px + 0.5) / w * 2 - 1
            if half <= 1e-4:
                continue
            edge = 1 - abs(x - centre) / half
            if edge <= 0:
                continue
            a = _smooth(edge / 0.75) * (1 - 0.3 * y ** 2.2)
            core = max(0.0, 1 - abs(x - centre) / (0.42 * half)) * (1 - y) ** 1.1
            a = min(1.0, a * 0.72 + 0.4 * core)
            a *= _smooth(y / 0.08)
            img.putpixel((ox + px, py), (255, 255, 255, int(round(255 * a))))


def body():
    """The body's aura, white for the client to tint (render.AuraBodyLayer), its shapes side by side in one 128 by 64 sheet:
    a soft round haze (0..32), four frames of a tongue of fire (32..96, 16 each), a soft band for the mantle's ribbons (96..128,
    top 16 rows), a burning eye (96..112, the next 16) and the glow of aura pooling on the ground (96..128, the bottom 32)."""
    import math
    img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    # The haze: a soft round glow, brightest in the middle (the body hides that), its halo what shows round the silhouette.
    for py in range(64):
        v = (py + 0.5) / 64 * 2 - 1
        for px in range(32):
            u = (px + 0.5) / 32 * 2 - 1
            d = min(1.0, math.sqrt(u * u + v * v))
            a = (1 - d) ** 1.7
            img.putpixel((px, py), (255, 255, 255, int(round(255 * a))))
    for frame in range(4):
        _flame(img, 32 + frame * 16, 16, 64, frame)
    # The ribbon: a soft band across (v), the same all along (u).
    for py in range(16):
        v = (py + 0.5) / 16 * 2 - 1
        a = max(0.0, 1 - abs(v)) ** 1.4
        for px in range(96, 128):
            img.putpixel((px, py), (255, 255, 255, int(round(255 * a))))
    # A burning eye: a hot point inside a soft glow.
    for py in range(16, 32):
        v = (py - 16 + 0.5) / 16 * 2 - 1
        for px in range(96, 112):
            u = (px - 96 + 0.5) / 16 * 2 - 1
            d = math.sqrt(u * u + v * v)
            a = max(0.0, 1 - d) ** 1.3 * 0.75 + 0.6 * max(0.0, 1 - d / 0.38)
            img.putpixel((px, py), (255, 255, 255, int(round(255 * min(1.0, a)))))
    # Aura pooling on the ground: a soft glow with a faint ring at its edge.
    for py in range(32, 64):
        v = (py - 32 + 0.5) / 32 * 2 - 1
        for px in range(96, 128):
            u = (px - 96 + 0.5) / 32 * 2 - 1
            d = math.sqrt(u * u + v * v)
            a = max(0.0, 1 - d) ** 1.6 * 0.8 + 0.28 * math.exp(-((d - 0.74) / 0.09) ** 2)
            if d >= 1:
                a = 0
            img.putpixel((px, py), (255, 255, 255, int(round(255 * min(1.0, a)))))
    return img


def banner_band():
    """A technique banner's band (client.AuraBanners), white for the client to tint: a brush stroke laid from the left, full and
    a little streaky with the bristles, its right end cut on a slant and frayed into nothing; soft along its top and bottom."""
    import random
    w, h = 128, 32
    rng = random.Random(4091)
    streak = [0.82 + 0.18 * rng.random() for _ in range(h)]
    fray = [rng.random() * 0.1 for _ in range(h)]
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    for py in range(h):
        y = (py + 0.5) / h
        soft = _smooth(min(y, 1 - y) / 0.16)
        # The slanted end: further right at the top than the bottom, each bristle's own length.
        end = 0.8 + (0.5 - y) * 0.16 - fray[py]
        for px in range(w):
            x = (px + 0.5) / w
            tail = 1 - _smooth((x - (end - 0.2)) / 0.2)
            a = soft * streak[py] * tail
            img.putpixel((px, py), (255, 255, 255, int(round(255 * max(0.0, min(1.0, a))))))
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
    g.save(shell(slim=True), glow / "shell_slim.png")
    # The body's aura by stage (haze, flames, the mantle's ribbons, burning eyes, the pool at the feet) and the technique banner.
    g.save(body(), glow / "body.png")
    import aura_standard_art
    aura_standard_art.write(g)

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
    "message.wildercord.aura.learned_how": "Land full swings with a blade to gather aura, or sneak and stand still with it in hand to breathe it in. Open the Cord screen (press %s) and choose Aura to see your path.",
    "message.wildercord.aura.waiting": "Your aura presses against its limit: %s waits.",
    "message.wildercord.aura.waiting_how": "Break through: crouch still with your blade for 30 seconds at a ley crossing, a qualifying waterfall or an exposed mountain summit; or defeat a stronger foe with blade and aura alone within 60 seconds.",
    "message.wildercord.aura.practice_complete": "Practice has taught its 40 XP. Fight real hostile foes with fully recovered sword swings to keep progressing. Your Aura page shows the next breakthrough trial.",
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
    "screen.wildercord.aura.trial.stillness": "Stillness: crouch still with a blade for 30 seconds at a ley crossing, a waterfall or a mountain summit",
    "screen.wildercord.aura.progress_help": "How to rank up",
    "screen.wildercord.aura.xp_help": "Use your own aura weapon and let each attack fully recover. Fight hostile foes in Survival; animals, rapid weak swings and Creative attacks teach nothing.",
    "screen.wildercord.aura.practice_help": "Practice learned: %s / %s XP for your lifetime. Dummies, practice arenas and training grounds share this limit; ordinary breathing only restores aura.",
    "screen.wildercord.aura.threshold_help": "At full experience, complete one listed breakthrough trial. Extra fighting cannot skip the trial. Trials count only after the threshold is reached.",
    "screen.wildercord.aura.progress_fight": "Rank up: land full swings on hostile foes",
    "screen.wildercord.aura.progress_practice_full": "Practice complete: fight real hostile foes",
    "screen.wildercord.aura.progress_weapon": "Hold your own aura weapon to train",
    "screen.wildercord.aura.progress_disabled": "Aura is disabled by this server",
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
    # Sword strings: arts set off by a short run of ordinary swings. The five common arts, played by a breathing method without arts
    # of its own (each method's own follow the common ones, below).
    "aura.wildercord.art.first_art": "First Art",
    "aura.wildercord.art.first_art.desc": "Swing, swing, then a low swing (crouching): your blade looses an arc of aura in front of you, cutting the foes there in your element. The common form, played by a breathing method without arts of its own.",
    "aura.wildercord.art.second_art": "Second Art",
    "aura.wildercord.art.second_art.desc": "A leaping swing (in the air), then a low swing: a rising arc of aura that cuts the foes in front and throws them into the air (never a boss). The common form, played by a breathing method without arts of its own.",
    "aura.wildercord.art.third_art": "Third Art",
    "aura.wildercord.art.third_art.desc": "A counter (swing straight after a perfect Aura Guard): a cut of aura that staggers your foe again. The common form, played by a breathing method without arts of its own.",
    "aura.wildercord.art.fourth_art": "Fourth Art",
    "aura.wildercord.art.fourth_art.desc": "A step cut (swing straight after an Aura Step): a line of aura cut five blocks ahead, through every foe in it. The common form, played by a breathing method without arts of its own.",
    "aura.wildercord.art.final_art": "Final Art",
    "aura.wildercord.art.final_art.desc": "Three full swings (your blade recovered each time), then a low swing, with your aura full: a ring of aura round you that cuts every foe near and throws them back. The common form, played by a breathing method without arts of its own.",
    # Each breathing method's own arts, on the same five strings (aura.arts): Ember, Rime, Thunder, Gale and Stone.
    "aura.wildercord.art.kindling_draw": "Kindling Draw",
    "aura.wildercord.art.kindling_draw.desc": "A fast low draw-cut that sets the foes in front alight, and a line of fire racing on ahead along the ground that burns for three seconds, setting alight whatever walks into it. (The fire is only light: nothing in the world catches.)",
    "aura.wildercord.art.rising_cinders": "Rising Cinders",
    "aura.wildercord.art.rising_cinders.desc": "A rising slash that throws the foes in front into the air in a shower of embers, setting them alight. A moment later cinders rain off each one onto whoever stands near it.",
    "aura.wildercord.art.backdraft": "Backdraft",
    "aura.wildercord.art.backdraft.desc": "The counter: the blow your perfect guard caught comes back as a gout of flame. Whoever struck takes it whole (your blade's share and their own blow with it), the rest of the cone half, all set alight and thrown back.",
    "aura.wildercord.art.wildfire_rush": "Wildfire Rush",
    "aura.wildercord.art.wildfire_rush.desc": "Straight after your step, a second rush seven blocks on, cutting through every foe in the way and setting them alight. The ground behind you burns for four seconds.",
    "aura.wildercord.art.sunfall": "Sunfall",
    "aura.wildercord.art.sunfall.desc": "You leap, a sun gathers on your blade, and you come down in a blazing arc: everything near where you land is struck hard, set alight and thrown back, and a ring of fire stands round the spot for five seconds, searing whoever crosses it.",
    "aura.wildercord.art.frostbite": "Frostbite",
    "aura.wildercord.art.frostbite.desc": "A cut that crusts and slows the foes in front. A third crust within eight seconds freezes a foe solid for a moment (a player only briefly), frozen for a mage's Shatter too.",
    "aura.wildercord.art.hailfall": "Hailfall",
    "aura.wildercord.art.hailfall.desc": "A cut in front, and a cloud ahead (over the foe you struck) that rains hail for a second: every stone that lands on a foe strikes it and slows it, up to three on one.",
    "aura.wildercord.art.glacier_mirror": "Glacier Mirror",
    "aura.wildercord.art.glacier_mirror.desc": "The counter: whoever struck is frozen solid (a player only briefly) and the foes beside it chilled, and a mirror of ice stands before you for two and a half seconds, turning projectiles from in front back at whoever loosed them.",
    "aura.wildercord.art.skate": "Skate",
    "aura.wildercord.art.skate.desc": "Straight after your step, a glide eight blocks on along a path of ice you lay as you go (over water it freezes for real, and thaws in time, only where you may build). For five seconds the path speeds you and your allies and slows foes; a frozen foe in the way shatters, its ice cutting those near.",
    "aura.wildercord.art.winters_hush": "Winter's Hush",
    "aura.wildercord.art.winters_hush.desc": "A hush falls over a wide cone in front: everything in it is frozen solid (a player only briefly), and a moment and a half later everything still frozen shatters, hard, its ice cutting those near.",
    "aura.wildercord.art.crackle": "Crackle",
    "aura.wildercord.art.crackle.desc": "Three cuts on one foe faster than the eye, each throwing a spark of lightning to another foe near. The first breaks whatever it was winding up: a charge, a bow, a fuse.",
    "aura.wildercord.art.skyfall": "Skyfall",
    "aura.wildercord.art.skyfall.desc": "A bolt called down out of the sky a moment later on the foe you struck (or the ground ahead), striking everything under it, holding it still a moment, and arcing on to two more foes near. What it strikes is left ionised for a storm spell.",
    "aura.wildercord.art.static_riposte": "Static Riposte",
    "aura.wildercord.art.static_riposte.desc": "The counter: lightning through whoever struck, then leaping on to the nearest foe, and on again, up to four more, a little weaker each time, each one held still a moment.",
    "aura.wildercord.art.bolt_step": "Bolt Step",
    "aura.wildercord.art.bolt_step.desc": "Straight after your step, you blink from foe to foe, up to four near and in sight (those ahead first), with a cut on each that holds it still a moment.",
    "aura.wildercord.art.heavens_spear": "Heaven's Spear",
    "aura.wildercord.art.heavens_spear.desc": "You stand and gather lightning on your blade for a moment, then drive a lance of it eighteen blocks down your line of sight through everything in it, and the sky strikes each foe it ran through.",
    "aura.wildercord.art.cutting_breeze": "Cutting Breeze",
    "aura.wildercord.art.cutting_breeze.desc": "A blade of wind loosed off your cut, flying twelve blocks on past your sword's reach, cutting up to four foes and pushing them back.",
    "aura.wildercord.art.updraft": "Updraft",
    "aura.wildercord.art.updraft.desc": "The foes in front thrown high, and you rising after them, hanging a moment to follow. While they're up your blade lands a quarter harder on them (and spells bite them harder too).",
    "aura.wildercord.art.eye_of_the_storm": "Eye of the Storm",
    "aura.wildercord.art.eye_of_the_storm.desc": "The counter: a spinning cut all round you that throws every foe near back. For three seconds after you're the eye of a storm: projectiles turn aside before they reach you, and foes are blown back off you.",
    "aura.wildercord.art.tailwind": "Tailwind",
    "aura.wildercord.art.tailwind.desc": "Straight after your step, a long dash thirteen blocks on, the foes in the way cut and shoved aside, and the wind at your back: you run faster for five seconds, and your allies near the way faster still.",
    "aura.wildercord.art.hundred_winds": "Hundred Winds",
    "aura.wildercord.art.hundred_winds.desc": "For three seconds you're a whirlwind of cuts, drawing the foes round you in and cutting them over and over, and at its end throwing them all into the air.",
    "aura.wildercord.art.rockbreaker": "Rockbreaker",
    "aura.wildercord.art.rockbreaker.desc": "A heavy cut that shakes your foe's footing: it can't act for a moment, goes heavy-footed, and is cracked so spells bite it harder, the shock spilling onto those beside it.",
    "aura.wildercord.art.avalanche": "Avalanche",
    "aura.wildercord.art.avalanche.desc": "A slam where you come down (still in the air, you're driven down first), and a shockwave rolling out over the ground, striking everything it reaches, throwing it back and slowing it.",
    "aura.wildercord.art.unmoved": "Unmoved",
    "aura.wildercord.art.unmoved.desc": "The counter: whoever struck is hurled back by their own blow and stunned as they land, and you harden like stone for four seconds: blows land a fifth lighter and nothing knocks you back.",
    "aura.wildercord.art.landslide": "Landslide",
    "aura.wildercord.art.landslide.desc": "Straight after your step, a heavy charge eight blocks on that catches the foes in front and carries them along, throwing them on at its end. A foe driven into a wall is crushed against it.",
    "aura.wildercord.art.mountain_splitter": "Mountain Splitter",
    "aura.wildercord.art.mountain_splitter.desc": "An overhead strike that splits the ground in a line fifteen blocks ahead, stone bursting up along it in turn and throwing up whatever it reaches, hard. (The stone is only a shape: the ground is never broken.)",
    "aura.wildercord.art.thorn_lash": "Thorn Lash",
    "aura.wildercord.art.thorn_lash.desc": "A lash of thorned vine flicked out four and a half blocks, past your sword's reach, cutting up to three foes in front. The first it catches is rooted where it stands for a second and a half (it can still strike, not walk away; a player only briefly) and pricked by the thorns while it's held.",
    "aura.wildercord.art.blossom_fall": "Blossom Fall",
    "aura.wildercord.art.blossom_fall.desc": "A falling cut through the foes in front, and where it lands a carpet of blossom opens for four seconds: you and your allies on it are mended at once and a little more each second, and foes on it are slowed.",
    "aura.wildercord.art.rooted_parry": "Rooted Parry",
    "aura.wildercord.art.rooted_parry.desc": "The counter: roots seize whoever struck and hold it where it stands, thorns burst up round you and prick the foes beside you, and you mend by what your guard caught (three to six health).",
    "aura.wildercord.art.wild_growth": "Wild Growth",
    "aura.wildercord.art.wild_growth.desc": "Straight after your step, a second rush seven blocks on, the foes in the way cut and snagged by roots, and brambles springing up behind you for five seconds: foes in them are slowed and pricked, your allies in them mended.",
    "aura.wildercord.art.groves_heart": "Grove's Heart",
    "aura.wildercord.art.groves_heart.desc": "You plant your blade in the ground: roots burst up under every foe within six blocks, striking hard and binding them for two and a half seconds (a player only briefly), and a grove rises round you for eight seconds, mending you and your allies each second and slowing and pricking the foes still in it.",
    "aura.wildercord.art.void_cut": "Void Cut",
    "aura.wildercord.art.void_cut.desc": "A cut that tears a hole in the air at your blade's end and draws up to three foes in front, from as far as five and a half blocks, in to your feet: struck, slowed a moment and shadowed (a life spell on them sets off Blight). A boss is never drawn.",
    "aura.wildercord.art.collapse": "Collapse",
    "aura.wildercord.art.collapse.desc": "A falling cut that opens a well in the ground ahead: for most of a second it drags every foe within four and a half blocks in toward it (a player can always run out of it), then collapses on everything it gathered.",
    "aura.wildercord.art.null_parry": "Null Parry",
    "aura.wildercord.art.null_parry.desc": "The counter: the blow your guard caught is swallowed by the void, and whoever struck is cut and silenced: a creature can't cast, draw a bow or light a fuse for three seconds; a player can't cast, play an art or use the Aura key but to guard, for a second and a half. The foes beside you are shoved off.",
    "aura.wildercord.art.rift_step": "Rift Step",
    "aura.wildercord.art.rift_step.desc": "Straight after your step, you go into a rift and come out of another just past the nearest foe ahead (up to eight blocks), cutting it as you pass. The rifts' edges drag in and cut whoever stands near them.",
    "aura.wildercord.art.event_horizon": "Event Horizon",
    "aura.wildercord.art.event_horizon.desc": "You cut a black sphere into the air ahead. For two seconds it drags in every foe within seven blocks (a player can always run out of it) and grinds whatever it holds, then it crushes everything near it, hard.",
    "aura.wildercord.art.star_needle": "Star Needle",
    "aura.wildercord.art.star_needle.desc": "A thrust that looses three darts of starlight nine blocks on, each seeking a little toward a foe. Each that strikes sets a star on its foe (for your next Starlit art to burst) and gives you half an aura back.",
    "aura.wildercord.art.meteor_shower": "Meteor Shower",
    "aura.wildercord.art.meteor_shower.desc": "Stars brought down out of the sky over a circle ahead (round the foe you struck), the last a great one on its heart that sets stars on what it strikes. Each foe struck gives you an aura back.",
    "aura.wildercord.art.constellation_guard": "Constellation Guard",
    "aura.wildercord.art.constellation_guard.desc": "The counter: whoever struck is cut and set with a constellation of four stars that burst one by one a moment and a half later, each giving you aura back. Every foe near already carrying a star of yours bursts at once.",
    "aura.wildercord.art.comet_dash": "Comet Dash",
    "aura.wildercord.art.comet_dash.desc": "Straight after your step, a second rush eight blocks on trailing stars, the foes in the way cut and starred. A moment later the trail bursts star by star along its length, striking everything near it, an aura back for each foe it catches.",
    "aura.wildercord.art.nova": "Nova",
    "aura.wildercord.art.nova.desc": "You gather starlight on your blade for a moment, then it bursts out in a ring over everything within seven blocks, striking it hard and throwing it back; starred foes burst with it. You get aura back for every foe struck, up to half of what it cost.",
    "aura.wildercord.art.echo_cut": "Echo Cut",
    "aura.wildercord.art.echo_cut.desc": "A cut in front, and a golden afterimage left where you stood that strikes it again a moment later, the way you faced, at whatever stands there then.",
    "aura.wildercord.art.rewind_leap": "Rewind Leap",
    "aura.wildercord.art.rewind_leap.desc": "A falling cut that drags the foes in front in time (slowed hard for two seconds), then time snaps you back to where you leapt from, if you may still stand there and it's no more than ten blocks.",
    "aura.wildercord.art.stopped_moment": "Stopped Moment",
    "aura.wildercord.art.stopped_moment.desc": "The counter: whoever struck is held still in time for two and a half seconds (a player only briefly), then flung back and struck again by the moment's snap as time starts once more. The foes beside you are dragged a moment.",
    "aura.wildercord.art.blur": "Blur",
    "aura.wildercord.art.blur.desc": "Straight after your step, a rush seven blocks on faster than any other, the foes in the way cut. For three seconds time drags round you: foes near are slowed, projectiles crossing into it slow to a third, and you're quickened.",
    "aura.wildercord.art.thousand_moments": "Thousand Moments",
    "aura.wildercord.art.thousand_moments.desc": "Time stops round you: every foe within seven blocks is held still for two and a half seconds (a player only briefly) while your cuts gather on it, and when time moves again they all land at once, with half of every blow you struck it meanwhile.",
    "aura.wildercord.art.bloodletting": "Bloodletting",
    "aura.wildercord.art.bloodletting.desc": "A deep cut that opens a wound in up to three foes in front. It bleeds for three seconds, harder while they run, and you drink a quarter of what it bleeds back as health. (A wind spell on a bleeding foe sets off Rupture.)",
    "aura.wildercord.art.red_rain": "Red Rain",
    "aura.wildercord.art.red_rain.desc": "A falling cut that bursts where it lands, striking everything near, and a red rain falls there for two seconds: every foe under it bleeds, and you drink back a share of all of it as health.",
    "aura.wildercord.art.sanguine_parry": "Sanguine Parry",
    "aura.wildercord.art.sanguine_parry.desc": "The counter: the blow your guard caught becomes a wound in whoever struck it, cutting it and bleeding the blow's worth back out of it over three seconds, and you drink half of all of it back as health.",
    "aura.wildercord.art.frenzy": "Frenzy",
    "aura.wildercord.art.frenzy.desc": "Straight after your step, a second rush seven blocks on through the foes in the way. Each one cut, and each blow of yours that lands after, quickens your blade, up to a fifth faster, until five seconds from the rush.",
    "aura.wildercord.art.crimson_moon": "Crimson Moon",
    "aura.wildercord.art.crimson_moon.desc": "A gamble: it takes a quarter of your health (never leaving you under a heart, so it can never kill you), then a great arc of blood-light strikes every foe before you hard, opens wounds in them, and drinks back half of everything it deals, up to ten health.",
    "message.wildercord.aura.art.silenced": "Your aura is silenced: %s can't go",
    "message.wildercord.aura.silenced": "Your aura is silenced: only your guard answers",
    "message.wildercord.aura.art.blocked": "%s has nowhere to go",
    "toast.wildercord.aura.art": "New art: %s",
    "screen.wildercord.aura.method_arts": "Its own five arts",
    "screen.wildercord.aura.method_common": "Plays the common arts (it has none of its own)",
    "screen.wildercord.aura.art_other": "%s's art: breathe it to play this",
    "screen.wildercord.grimoire.arts": "Sword arts (%s of %s)",
    "screen.wildercord.grimoire.arts_hint": "Each breathing method's own arts, five a method, one a stage. An art goes in here the first time you play it.",
    "screen.wildercord.grimoire.art": "%s · %s",
    "screen.wildercord.grimoire.art_unknown": "Not played yet: %s, %s. Its string is on the Aura page's Sword strings tab.",
    "aura.wildercord.token.swing": "a swing",
    "aura.wildercord.token.full": "a full swing",
    "aura.wildercord.token.low": "a low swing (crouching)",
    "aura.wildercord.token.leap": "a leaping swing (in the air)",
    "aura.wildercord.token.run": "a running swing (sprinting)",
    "aura.wildercord.token.counter": "a counter (the first swing after a perfect guard)",
    "aura.wildercord.token.step": "a step cut (the first swing after an Aura Step)",
    "message.wildercord.aura.art.not_ready": "%s isn't ready yet",
    "message.wildercord.aura.art.no_aura": "%s needs %s aura",
    "message.wildercord.aura.art.condition": "%s can't be played right now",
    "message.wildercord.aura.art.full_pool": "%s needs your aura full",
    "toast.wildercord.aura.sword_string": "Your first sword string",
    "screen.wildercord.aura.arts": "Sword strings",
    "screen.wildercord.aura.arts_hint": "Swing in these patterns, each swing within %s s of your blade being ready again: a string played in time sets off its art. Swings at the air count only in a fight.",
    "screen.wildercord.aura.art_string": "String: %s",
    "screen.wildercord.aura.art_rest": "Rests %s s",
    "screen.wildercord.aura.art_resting": "Ready in %s s",
    "screen.wildercord.aura.art_needs_full": "Only with your aura full",
    "screen.wildercord.string_indicator": "Sword strings: %s",
    "screen.wildercord.string_indicator.crosshair": "by the crosshair",
    "screen.wildercord.string_indicator.hotbar": "by the hotbar",
    "screen.wildercord.string_indicator.hidden": "hidden",
    "screen.wildercord.string_indicator.tip": "Where the marks of a sword string you're playing show: a little below the crosshair, or above the aura bar. Hidden, the soft tick of each swing goes quiet too; a string completed or broken still sounds.",
    # Aura's feel: technique banners, and the visual settings for trails, the body's aura, impacts and banners.
    "aura.wildercord.banner.ordinal.1": "First Art",
    "aura.wildercord.banner.ordinal.2": "Second Art",
    "aura.wildercord.banner.ordinal.3": "Third Art",
    "aura.wildercord.banner.ordinal.4": "Fourth Art",
    "aura.wildercord.banner.ordinal.5": "Final Art",
    "aura.wildercord.banner.kicker": "%s · %s",
    "aura.wildercord.banner.technique": "Technique",
    "screen.wildercord.blade_trails": "Blade trails: %s",
    "screen.wildercord.blade_trails.full": "full",
    "screen.wildercord.blade_trails.subtle": "subtle",
    "screen.wildercord.blade_trails.off": "off",
    "screen.wildercord.blade_trails.tip": "The ribbon of light an aura blade leaves as it cuts. Subtle keeps the ribbon but drops its extras (motes, sparks, echoes) and other players' ordinary swings; techniques always show. Your own trail in first person is always thin and low.",
    "screen.wildercord.body_aura": "Body aura: %s",
    "screen.wildercord.body_aura.full": "full",
    "screen.wildercord.body_aura.calm": "calm",
    "screen.wildercord.body_aura.off": "off",
    "screen.wildercord.body_aura.tip": "The aura round a swordsman's body (a shimmer, wisps, a haze, a mantle, a corona), calm at rest and flaring in a fight. Calm keeps it at rest and drops its wisps and embers. Your own shows only as a faint glow at the bottom of the screen in first person.",
    "screen.wildercord.impact": "Impact: %s",
    "screen.wildercord.impact.full": "full",
    "screen.wildercord.impact.soft": "soft",
    "screen.wildercord.impact.off": "off",
    "screen.wildercord.impact.tip": "How hard aura's blows land on screen: a brief hit-stop for you and whoever you strike, a nudge of the view and a flash. Soft halves them; off drops the hit-stop and the nudge and keeps a small flash. Camera motion off drops the nudge too.",
    "screen.wildercord.banners": "Technique banners: %s",
    "screen.wildercord.banners.all": "everyone's",
    "screen.wildercord.banners.own": "yours only",
    "screen.wildercord.banners.off": "off",
    "screen.wildercord.banners.tip": "The name of an art or a Dominion, shown briefly as it goes off: yours by the left edge of the screen, other players' over their heads.",
    # Momentum, stance and finishers.
    "aura.wildercord.technique.momentum": "Momentum",
    "aura.wildercord.technique.momentum.desc": "A clean fight builds it: full swings that land, arts that land, perfect guards, and steps taken through an attack. A hit knocks some off, and out of a fight it ebbs away. At each quarter your arts cost less and strike harder; at its peak, your Final Art opens (and playing it spends some). It's the thin line under your aura bar.",
    "aura.wildercord.technique.finisher": "Finishers",
    "aura.wildercord.technique.finisher.desc": "Every foe has a stance that your blows wear down (arts more, perfect guards a great deal, Stone's most of all). When it breaks the foe is opened, staggered and marked with a gold seal: your next full swing on it is your method's finisher, which deals a share of what it has already lost and gives aura back. Bosses take far more to open, and players have a stance too.",
    "message.wildercord.aura.art.peak": "%s waits on your momentum's peak",
    "message.wildercord.aura.guard_broken": "Your guard is broken",
    "message.wildercord.aura.opened": "Your stance breaks: you're open!",
    "screen.wildercord.aura.art_needs_peak": "Only at the peak of your momentum",
    "screen.wildercord.aura.finisher_when": "on an opened foe",
    "screen.wildercord.aura.momentum_line": "Momentum tier %s: arts %s%% cheaper, %s%% stronger",
    "screen.wildercord.aura.momentum_peak": "Peak momentum: arts %s%% cheaper, %s%% stronger, Final Art open",
    "aura.wildercord.banner.finisher": "Finisher",
    "toast.wildercord.aura.peak_momentum": "Peak momentum",
    "toast.wildercord.aura.stance_break": "A stance broken",
    "toast.wildercord.aura.finisher": "Your first finisher",
    "aura.wildercord.finisher.pyrebrand": "Pyrebrand",
    "aura.wildercord.finisher.pyrebrand.desc": "Ember's finisher: an X of fire branded across the opened foe and a pillar of flame through it. It burns.",
    "aura.wildercord.finisher.winterbreak": "Winterbreak",
    "aura.wildercord.finisher.winterbreak.desc": "Rime's finisher: frost locks the opened foe for a breath, then bursts outward in shards, spires of ice round its feet. It's left chilled.",
    "aura.wildercord.finisher.skysunder": "Skysunder",
    "aura.wildercord.finisher.skysunder.desc": "Thunder's finisher: the blade brings the sky down, a bolt through the opened foe and arcs racing out over the ground. It's left ionised.",
    "aura.wildercord.finisher.windscour": "Windscour",
    "aura.wildercord.finisher.windscour.desc": "Gale's finisher: a spiral of wind wraps the opened foe and lifts it, crescents whirling round it.",
    "aura.wildercord.finisher.faultline": "Faultline",
    "aura.wildercord.finisher.faultline.desc": "Stone's finisher: an overhead cleave; the ground cracks round the opened foe and stone bursts up behind it. It's left cracked and slowed.",
    "aura.wildercord.finisher.thornbloom": "Thornbloom",
    "aura.wildercord.finisher.thornbloom.desc": "Verdant's finisher: roots seize the opened foe and a flower of light opens under it, its petals mending you and your allies near.",
    "aura.wildercord.finisher.nullfall": "Nullfall",
    "aura.wildercord.finisher.nullfall.desc": "Hollow's finisher: the world falls into a black point at the opened foe's heart, then bursts; foes near are drawn in.",
    "aura.wildercord.finisher.starbreak": "Starbreak",
    "aura.wildercord.finisher.starbreak.desc": "Starlit's finisher: a star kindles in the opened foe and bursts in rays of light, and gives back half again the aura of any other.",
    "aura.wildercord.finisher.hours_end": "Hour's End",
    "aura.wildercord.finisher.hours_end.desc": "Hourglass's finisher: a clock face stands behind the opened foe with its hands at the hour; the hour strikes, and a second cut falls where the first did.",
    "aura.wildercord.finisher.heartrend": "Heartrend",
    "aura.wildercord.finisher.heartrend.desc": "Crimson's finisher: a crimson crescent tears through the opened foe and back. It bleeds, and you drink a share of the blow.",
    "aura.wildercord.finisher.decisive_cut": "Decisive Cut",
    "aura.wildercord.finisher.decisive_cut.desc": "The finisher of a method without its own: two white-gold crescents crossing through the opened foe.",
    # Awakening, the spent state after it, and the Sovereign's awakened Dominion.
    "aura.wildercord.technique.awaken": "Awakening",
    "aura.wildercord.technique.awaken.desc": "Tap the Aura key, then press it again at once and hold it: with your aura full and your momentum at half or more, you awaken for 12 seconds at Edge, 16 at Form and 20 at Sovereign. Your aura blazes and your eyes burn, your arts cost nothing, your momentum holds at its peak (your Final Art with it), and you're a little faster and hit a little harder. Each finisher you land feeds it another second. When it ends the rest of your aura burns away and you're spent: slowed, and unable to gather any aura for 30 seconds. Rests for three minutes.",
    "aura.wildercord.awakening": "Awakening",
    "screen.wildercord.aura.key_tap_hold": "Tap %s, then hold",
    "screen.wildercord.aura.awakened_left": "Awakened: %s s",
    "screen.wildercord.aura.spent_left": "Spent: %s s",
    "screen.wildercord.aura.awakening_rests": "Awakening rests %s s",
    "screen.wildercord.aura.awakening_ready": "Awakening ready",
    "screen.wildercord.aura.awakening_pool": "Awakening: aura not full",
    "screen.wildercord.aura.awakening_momentum": "Awakening: momentum %s",
    "screen.wildercord.aura.awakening_waits": "Awakening waits",
    "message.wildercord.aura.awaken.off": "Awakening is off on this server",
    "message.wildercord.aura.awaken.stage": "You can awaken from Edge",
    "message.wildercord.aura.awaken.awakened": "You're awakened already",
    "message.wildercord.aura.awaken.spent": "You're spent: %s s",
    "message.wildercord.aura.awaken.resting": "Your aura can't awaken again yet: %s s",
    "message.wildercord.aura.awaken.no_weapon": "Your aura needs a blade in hand to awaken",
    "message.wildercord.aura.awaken.pool": "Your aura must be full to awaken (%s of %s)",
    "message.wildercord.aura.awaken.momentum": "Awakening needs momentum %s (you have %s)",
    "message.wildercord.aura.spent": "The awakening burns out: you're spent",
    "message.wildercord.aura.recovered": "Your aura stirs again",
    "toast.wildercord.aura.awakening": "Your first awakening",
    "toast.wildercord.aura.sovereign_dominion": "An awakened Dominion",
    "aura.wildercord.sovereign.kicker": "Awakened Dominion",
    "aura.wildercord.sovereign.throne_of_cinders": "Throne of Cinders",
    "aura.wildercord.sovereign.throne_of_cinders.desc": "Ember's awakened Dominion: the ground under it scorched and its rim a ring of flame. Every foe inside is set alight each second.",
    "aura.wildercord.sovereign.court_of_winter": "Court of Winter",
    "aura.wildercord.sovereign.court_of_winter.desc": "Rime's awakened Dominion: as it's raised every foe inside freezes solid for a moment (a player only briefly), and after it they're chilled hard.",
    "aura.wildercord.sovereign.seat_of_storms": "Seat of Storms",
    "aura.wildercord.sovereign.seat_of_storms.desc": "Thunder's awakened Dominion: each second a bolt falls on a foe inside, cutting into it and breaking whatever it was winding up.",
    "aura.wildercord.sovereign.windward_ground": "Windward Ground",
    "aura.wildercord.sovereign.windward_ground.desc": "Gale's awakened Dominion: every other second an updraft throws the foes inside into the air (your blows on them land harder there), and while you stand in it the wind turns shots aside from you.",
    "aura.wildercord.sovereign.unmoving_mountain": "Unmoving Mountain",
    "aura.wildercord.sovereign.unmoving_mountain.desc": "Stone's awakened Dominion: while you stand in it you're hardened, nothing moves you and you take less from every blow; each second it wears at the stance of every foe inside.",
    "aura.wildercord.sovereign.wildwood_court": "Wildwood Court",
    "aura.wildercord.sovereign.wildwood_court.desc": "Verdant's awakened Dominion: as it's raised roots seize every foe inside; each second it mends you and your allies standing in it.",
    "aura.wildercord.sovereign.sunken_hall": "Sunken Hall",
    "aura.wildercord.sovereign.sunken_hall.desc": "Hollow's awakened Dominion: as it's raised every foe inside is silenced, and the ground keeps drawing them in toward its heart (a player only leaned on).",
    "aura.wildercord.sovereign.field_of_stars": "Field of Stars",
    "aura.wildercord.sovereign.field_of_stars.desc": "Starlit's awakened Dominion: each second a star falls on a foe inside and marks it, and a marked one's star bursts for twice as much; your aura flows back three times as fast while you stand in it.",
    "aura.wildercord.sovereign.stilled_hour": "Stilled Hour",
    "aura.wildercord.sovereign.stilled_hour.desc": "Hourglass's awakened Dominion: as it's raised every foe inside is held still a moment; after it time drags inside, slowing foes hard and their shots to a crawl.",
    "aura.wildercord.sovereign.crimson_court": "Crimson Court",
    "aura.wildercord.sovereign.crimson_court.desc": "Crimson's awakened Dominion: every other second the foes inside bleed, and you drink a share of every blow you land on a foe inside.",
    "aura.wildercord.sovereign.sovereign_ground": "Sovereign Ground",
    "aura.wildercord.sovereign.sovereign_ground.desc": "The awakened Dominion of a method without its own: only stronger.",
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
