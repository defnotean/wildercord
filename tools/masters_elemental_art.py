"""The Rime, Thunder, Verdant and Hollow Sword Masters: names, signature banners, hints, lessons, their 104 named techniques,
and each school's master skin and blade. Technique keys are read from ElementalMasters.java so the names never drift."""
import re
from pathlib import Path

SCHOOLS = ("rime", "thunder", "verdant", "hollow")

LANG = {
    "master.wildercord.school.rime": "Rime",
    "master.wildercord.school.thunder": "Thunder",
    "master.wildercord.school.verdant": "Verdant",
    "master.wildercord.school.hollow": "Hollow",
    "boss.wildercord.master.rime_lattice": "Rime Lattice",
    "boss.wildercord.master.thunder_chain": "Thunder Chain",
    "boss.wildercord.master.verdant_bloom": "Verdant Bloom",
    "boss.wildercord.master.hollow_pull": "Hollow Pull",
    "message.wildercord.master.rime_hint": "Step off the frost cross, or jump as it freezes.",
    "message.wildercord.master.thunder_hint": "Leave the rod circles and spread out from allies.",
    "message.wildercord.master.verdant_hint": "Leave the petal ring, or jump as it blooms.",
    "message.wildercord.master.hollow_hint": "Sprint against the pull and stay outside the inner ring.",
    "message.wildercord.master.rime_lesson": "Rime master: My frost marks a cross under you. Keep moving before it freezes.",
    "message.wildercord.master.thunder_lesson": "Thunder master: I plant three rods. Leave each circle before its bolt, and never crowd an ally.",
    "message.wildercord.master.verdant_lesson": "Verdant master: My bloom rings the ground you stand on. Leave it before it opens, or be rooted.",
    "message.wildercord.master.hollow_lesson": "Hollow master: My well drags you in. Sprint against it and stay outside the core.",
}

_SOURCE = Path(__file__).resolve().parent.parent / "src/main/java/dev/wildercord/aura/world/ElementalMasters.java"


def technique_keys():
    """Every named technique key, in table order (Rime, Thunder, Verdant, Hollow)."""
    text = _SOURCE.read_text(encoding="utf-8")
    keys = []
    for school in ("RIME", "THUNDER", "VERDANT", "HOLLOW"):
        block = re.search(r"case " + school + r' -> """\n(.*?)"""', text, re.S)
        if block is None:
            raise SystemExit(f"ElementalMasters.java: no technique table for {school}")
        for line in block.group(1).splitlines():
            line = line.strip()
            if line:
                keys.append(line.split()[0])
    return keys


LANG.update({"boss.wildercord.master.technique." + key: " ".join(word if word in ("and", "of") else word.capitalize()
    for word in key.split("_")) for key in technique_keys()})


def write(g):
    import masters_art
    for school in SCHOOLS:
        g.save(masters_art.master_texture(school), g.ASSETS / f"textures/entity/master/{school}.png")
        g.save(master_blade(school), g.ASSETS / f"textures/item/master_blade/{school}.png")
        g.write_json(g.ASSETS / f"models/item/master_blade/{school}.json", {
            "parent": "minecraft:item/handheld", "textures": {"layer0": f"wildercord:item/master_blade/{school}"}})
        g.write_json(g.ASSETS / f"items/master_blade/{school}.json", {
            "model": {"type": "minecraft:model", "model": f"wildercord:item/master_blade/{school}"}})


def master_blade(school):
    from item_art import hexc
    from masters_art import _blade_icon, _diagonal
    if school == "rime":
        # Hoarline: a pale ice-steel blade frosted white along its edge, a snowflake-pointed silver guard and a blue grip.
        pal = {"E": hexc("#FFFFFF"), "I": hexc("#BCE8FA"), "D": hexc("#6EA8C8")}
        blade = _diagonal({14: (6, 12, "E"), 15: (6, 13, lambda x: pal["E"] if x % 4 == 0 else pal["I"]), 16: (7, 13, "D")}, pal)
        silver, dim = hexc("#E6F2F8"), hexc("#8AA6B8")
        guard = [(3, 8, dim), (4, 9, silver), (5, 10, hexc("#FFFFFF")), (6, 11, silver), (7, 12, dim), (5, 9, silver)]
        grip = [(4, 11, hexc("#2E5A7A")), (3, 12, hexc("#3E78A0")), (2, 13, hexc("#2E5A7A"))]
        return _blade_icon(blade, guard, grip, (1, 14, silver), hexc("#14222E"))
    if school == "thunder":
        # Stormrod: a straight dark-steel blade split by a jagged yellow bolt, a brass crossguard and a storm-grey grip.
        pal = {"E": hexc("#FFFBE0"), "Y": hexc("#FFE650"), "S": hexc("#3A3C46")}
        blade = _diagonal({14: (6, 13, "S"), 15: (6, 13, lambda x: pal["Y"] if x % 2 else pal["E"]), 16: (7, 13, "S")}, pal)
        brass, dark = hexc("#E8C468"), hexc("#987024")
        guard = [(3, 8, dark), (4, 9, brass), (5, 10, hexc("#FFE890")), (6, 11, brass), (7, 12, dark)]
        grip = [(4, 11, hexc("#4A4C58")), (3, 12, hexc("#6A6C7A")), (2, 13, hexc("#4A4C58"))]
        return _blade_icon(blade, guard, grip, (1, 14, brass), hexc("#16161E"))
    if school == "verdant":
        # Thornbloom: a leaf-shaped green-steel blade with a bright vein, a curled wooden guard and a living vine wrap.
        pal = {"E": hexc("#E8FFB0"), "G": hexc("#6EDC64"), "D": hexc("#3A8A3A")}
        blade = _diagonal({13: (6, 10, "G"), 14: (5, 12, "E"), 15: (6, 13, "G"), 16: (7, 13, "D")}, pal)
        wood, dark = hexc("#8A6A3A"), hexc("#5A4020")
        guard = [(4, 9, wood), (5, 10, hexc("#B08A50")), (6, 11, wood), (3, 9, dark), (6, 12, dark)]
        grip = [(4, 11, hexc("#2E6A2E")), (3, 12, hexc("#4E9A40")), (2, 13, hexc("#2E6A2E"))]
        tassel = [(0, 15, hexc("#F0A0C8")), (1, 15, hexc("#6EDC64"))]
        return _blade_icon(blade, guard, grip, (1, 14, wood), hexc("#142214"), tassel)
    # Wellsong: a black-violet blade with a hollow fuller of pale light, a ring guard and a dusk-purple grip.
    pal = {"E": hexc("#E0B0FF"), "V": hexc("#B45AF0"), "B": hexc("#2A1A3A")}
    blade = _diagonal({14: (6, 12, "V"), 15: (6, 13, lambda x: pal["E"] if x in (8, 11) else pal["B"]), 16: (7, 13, "V")}, pal)
    silver, dim = hexc("#C8B8E0"), hexc("#6A5A88")
    guard = [(4, 9, silver), (5, 10, dim), (6, 11, silver), (4, 10, silver), (5, 9, silver), (6, 10, silver)]
    grip = [(4, 11, hexc("#4A2A6A")), (3, 12, hexc("#6A3A90")), (2, 13, hexc("#4A2A6A"))]
    return _blade_icon(blade, guard, grip, (1, 14, silver), hexc("#120A1A"))
