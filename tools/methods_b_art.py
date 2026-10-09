"""The methods-b pack: the Echo, Dawn and Venom breathing methods and their Sword Masters. Their manual colours and emblems, their
English text, and their Masters' looks and blades. tools/aura_art.py folds this in at its end, so every generator that reads its
METHODS and EMBLEMS (manuals, duelists, the Masters) draws these three too.

Nothing here imports aura_art at module level: aura_art imports this module while it is still loading.
"""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

# (id, element, colour, highlight): Echo silver-violet, Dawn white gold and rose, Venom acid green on black.
METHODS = [
    ("echo", "wind", 0x9A7CE8, 0xE4E0F4),
    ("dawn", "arcane", 0xFFD98C, 0xFFE0EA),
    ("venom", "life", 0x8EE03C, 0xD8FF8A),
]

EMBLEMS = {
    # Rings spreading from a struck point.
    "echo": [
        "#...#.",
        ".#.#..",
        "..+...",
        ".#.#..",
        "#...#.",
    ],
    # A sun rising over the horizon.
    "dawn": [
        "#.#.#.",
        ".###..",
        "##+##.",
        "......",
        "#####.",
    ],
    # A serpent's S with its fangs.
    "venom": [
        "#.#...",
        ".###..",
        "#.....",
        ".###..",
        "....#.",
    ],
}

_METHOD_TEXT = {
    "echo": ("Echo Breath", "Breathe as a struck bell breathes: once, and then again, softer.",
             "Echo: a coated blow may ring again a moment later, and from Edge its foe reels."),
    "dawn": ("Dawn Breath", "Breathe in the first light, and give it back brighter.",
             "Dawn: a coated blow lights its foe up and burns the undead. From Flow it mends you a little."),
    "venom": ("Venom Breath", "Breathe as the serpent does: slow, cold, and ready.",
              "Venom: a coated blow may leave a toxin that stacks. From Edge every blow does, and weakens."),
}

_ARTS = {
    "ringing_cut": ("Ringing Cut", "Swing, swing, then a low swing (crouching): an arc of sound in front. A moment later its ring strikes the same foes again."),
    "resonant_chord": ("Resonant Chord", "A leaping swing (in the air), then a low swing: a cone of sound ahead. Foes in it are struck, reel and slow."),
    "counterpoint": ("Counterpoint", "A counter (swing straight after a perfect Aura Guard): a ring of sound all round throws foes back reeling, then rings again."),
    "reverb_step": ("Reverb Step", "A step cut (swing straight after an Aura Step): a dash that cuts the foes in the way, then its echo cuts them again."),
    "grand_resonance": ("Grand Resonance", "Three full swings, then a low swing, with your aura full: a great toll round you, and three echoes after it, each further out."),
    "first_light": ("First Light", "Swing, swing, then a low swing (crouching): a bright cut in front. Its foes are lit up, and the undead burn harder."),
    "sunrise_arc": ("Sunrise Arc", "A leaping swing (in the air), then a low swing: a rising arc of light ahead. Its foes are blinded a moment and lit up."),
    "halo_guard": ("Halo Guard", "A counter (swing straight after a perfect Aura Guard): a halo round you. You mend, and the foes near are blinded and thrown back."),
    "dawnbreak_rush": ("Dawnbreak Rush", "A step cut (swing straight after an Aura Step): a dash down a line of light, cutting and lighting the foes in the way."),
    "noon_zenith": ("Noon Zenith", "Three full swings, then a low swing, with your aura full: a sun overhead, then its noon falls on every foe near, blinding them."),
    "fang_strike": ("Fang Strike", "Swing, swing, then a low swing (crouching): a quick bite in front. Its toxin stacks with the next bite."),
    "spitting_cobra": ("Spitting Cobra", "A leaping swing (in the air), then a low swing: a spray of venom ahead, poisoning and weakening all it wets."),
    "shed_skin": ("Shed Skin", "A counter (swing straight after a perfect Aura Guard): a cut and a slither back. The husk left behind poisons whoever stands in it."),
    "serpent_slither": ("Serpent Slither", "A step cut (swing straight after an Aura Step): a weaving dash. Every foe in the way is bitten and weakened."),
    "hydra_coil": ("Hydra Coil", "Three full swings, then a low swing, with your aura full: five heads strike all round in turn, each bite stacking the toxin higher."),
}

_SCHOOLS = {"echo": "Echo", "dawn": "Dawn", "venom": "Venom"}

_LESSONS = {
    "echo": "Echo master: my blows ring twice. Leave the lane before the second toll, then counter.",
    "dawn": "Dawn master: when my sun gathers, turn your eyes away. Then dodge and counter.",
    "venom": "Venom master: my coil closes on all sides but one. Find the gap, then counter.",
}

_HINTS = {
    "toll_hint": "Leave the lane! It tolls twice.",
    "glare_hint": "Look away from the light!",
    "coil_hint": "Find the gap in the coil!",
    "answer.arc": "Step back or parry!",
    "answer.arc_low": "Jump it or parry!",
    "answer.arc_high": "Crouch under it or parry!",
    "answer.lane": "Sidestep the lane or parry!",
    "answer.circle": "Get out of reach or parry!",
}


def technique_keys():
    """Every technique key in the pack's three tables, read from MethodsBMasters.java (the tables live there)."""
    src = (ROOT / "src/main/java/dev/wildercord/aura/world/MethodsBMasters.java").read_text(encoding="utf-8")
    keys = []
    for table in re.findall(r'_TABLE\s*=\s*"""(.*?)"""', src, re.S):
        keys += [line.split()[0] for line in table.strip().splitlines() if line.strip()]
    return keys


def _title(key):
    return " ".join(word if word in ("and", "of", "the") else word.capitalize() for word in key.split("_"))


def _lang():
    out = {}
    for method_id, (name, lore, flavour) in _METHOD_TEXT.items():
        out[f"aura.wildercord.method.{method_id}"] = name
        out[f"aura.wildercord.method.{method_id}.lore"] = lore
        out[f"aura.wildercord.method.{method_id}.flavour"] = flavour
    for art, (name, desc) in _ARTS.items():
        out[f"aura.wildercord.art.{art}"] = name
        out[f"aura.wildercord.art.{art}.desc"] = desc
    for school, name in _SCHOOLS.items():
        out[f"master.wildercord.school.{school}"] = name
        out[f"message.wildercord.master.methods_b_{school}_lesson"] = _LESSONS[school]
    for key, text in _HINTS.items():
        out[f"message.wildercord.master.methods_b.{key}"] = text
    for key in technique_keys():
        out[f"boss.wildercord.master.technique.{key}"] = _title(key)
    return out


LANG = _lang()


def write(g):
    """The pack Masters' looks and blades. Their manuals and duelists come from aura_art and aura_world_art's own loops."""
    import masters_art
    for school in _SCHOOLS:
        g.save(masters_art.master_texture(school), g.ASSETS / f"textures/entity/master/{school}.png")
        g.save(master_blade(school), g.ASSETS / f"textures/item/master_blade/{school}.png")
        g.write_json(g.ASSETS / f"models/item/master_blade/{school}.json", {
            "parent": "minecraft:item/handheld", "textures": {"layer0": f"wildercord:item/master_blade/{school}"}})
        g.write_json(g.ASSETS / f"items/master_blade/{school}.json", {
            "model": {"type": "minecraft:model", "model": f"wildercord:item/master_blade/{school}"}})


def master_blade(school):
    from item_art import hexc
    from masters_art import _blade_icon, _diagonal
    if school == "echo":
        # Bellsong: a silver blade with violet rings set down its fuller, a round bell guard.
        pal = {"E": hexc("#F4F2FA"), "S": hexc("#C8C4D8"), "D": hexc("#6E5CA8")}
        blade = _diagonal({14: (6, 12, "E"), 15: (6, 13, lambda x: hexc("#9A7CE8") if x % 2 else pal["S"]), 16: (7, 13, "D")}, pal)
        silver, dim = hexc("#E4E0F4"), hexc("#7A70A0")
        guard = [(4, 9, silver), (5, 10, hexc("#FFFFFF")), (4, 10, silver), (5, 9, dim), (6, 11, silver), (3, 9, dim)]
        grip = [(4, 11, hexc("#3E2E6A")), (3, 12, hexc("#5A44A0")), (2, 13, hexc("#3E2E6A"))]
        return _blade_icon(blade, guard, grip, (1, 14, silver), hexc("#18142A"), [(0, 15, hexc("#9A7CE8"))])
    if school == "dawn":
        # Daybreak: a white-gold blade with a rose edge, a sunburst guard.
        pal = {"E": hexc("#FFE0EA"), "G": hexc("#FFE9B0"), "D": hexc("#C89A48")}
        blade = _diagonal({14: (6, 12, "E"), 15: (6, 14, lambda x: hexc("#FFFFFF") if x == 14 else pal["G"]), 16: (7, 13, "D")}, pal)
        gold, dark = hexc("#FFD98C"), hexc("#A07428")
        guard = [(3, 8, dark), (4, 9, gold), (5, 10, hexc("#FFF4C8")), (6, 11, gold), (7, 12, dark), (4, 10, hexc("#F0A0B8")),
                 (5, 9, hexc("#F0A0B8"))]
        grip = [(4, 11, hexc("#E8B8C4")), (3, 12, hexc("#C08090")), (2, 13, hexc("#E8B8C4"))]
        return _blade_icon(blade, guard, grip, (1, 14, gold), hexc("#2A1E14"))
    # Fangsteel: a black blade, curved like a fang, with an acid-green edge and a serpent-head guard.
    pal = {"E": hexc("#D8FF8A"), "G": hexc("#8EE03C"), "B": hexc("#1E2418")}
    blade = _diagonal({14: (6, 12, "E"), 15: (6, 13, lambda x: pal["G"] if x >= 12 else pal["B"]), 16: (7, 12, "B")}, pal)
    green, dark = hexc("#5EA828"), hexc("#20301A")
    guard = [(4, 9, green), (5, 10, hexc("#D8FF8A")), (6, 11, green), (3, 8, dark), (7, 12, dark)]
    grip = [(4, 11, hexc("#2A2A22")), (3, 12, hexc("#3E5A26")), (2, 13, hexc("#2A2A22"))]
    return _blade_icon(blade, guard, grip, (1, 14, green), hexc("#0C0E0A"))
