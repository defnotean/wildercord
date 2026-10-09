"""Tide, Iron and Dune Breath (the methods-a pack): their words, and their Sword Masters' words and art.

The manuals' icons come from tools/aura_art.py (its METHODS and EMBLEMS, one methods-a block each); the sounds from
tools/feel/aura_methods_a.py. generate_assets.py adds LANG and calls write() in one methods-a block.
"""

# ---------------------------------------------------------------- the methods

_METHODS = {
    "tide": ("Tide Breath", "Breathe as the sea breathes: in with the ebb, out with the flood, and never against the current.",
             "Tide: a coated blow pushes its foe back and soaks it (its fire out, a moment slowed), a little more at each stage."),
    "iron": ("Iron Breath", "Breathe as the forge's bellows breathe: deep, steady, and every breath feeding the fire in the steel.",
             "Iron: a coated blow cracks its foe's armour for three seconds, more at each stage."),
    "dune": ("Dune Breath", "Breathe as the desert wind breathes: dry, patient, and carrying the sand wherever it goes.",
             "Dune: a coated blow may throw grit in its foe's eyes, blinding it a moment (a creature loses you), a better chance at each stage."),
}

_ARTS = {
    "riptide_cut": ("Riptide Cut",
                    "Swing, swing, then a low swing: a wide cut of water that soaks the foes in front (fire out, a moment slowed) and the current drags them in toward you."),
    "breaker": ("Breaker",
                "A leaping swing (in the air), then a low swing: a wave rolls seven blocks out ahead, cutting and throwing back everything in it, and leaves the ground behind it wet for four seconds: foes in it are slowed, and fire goes out."),
    "whirlpool": ("Whirlpool",
                  "The counter (swing straight after a perfect Aura Guard): a whirlpool opens on the ground ahead for three seconds, drawing every foe near to its heart and cutting them each second."),
    "surge": ("Surge",
              "Straight after your step, a wave carries you eight blocks on, cutting every foe in the way and carrying them along with you, soaked."),
    "maelstrom": ("Maelstrom",
                  "With your aura full, after three full swings and a low swing: the sea rises round you, five waves rolling over every foe near, and then it crashes down and throws them all away."),
    "sunder_cut": ("Sunder Cut",
                   "Swing, swing, then a low swing: one heavy cut through the plates that cracks the armour of the foes in front (four points, for five seconds)."),
    "anvil_fall": ("Anvil Fall",
                   "A leaping swing, then a low swing: you rise and come down like a hammer on an anvil, striking everything near where you land, holding it still a moment and cracking its armour."),
    "bulwark": ("Bulwark",
                "The counter: your guard sets like iron for three seconds. Blows land weaker and don't move you, and foes that come near are shoved back. Foes round you as it's raised are cut and thrown back."),
    "forge_charge": ("Forge Charge",
                     "Straight after your step, a shoulder-first charge seven blocks on through the line, cutting every foe in the way and cracking its armour."),
    "worldforge": ("Worldforge",
                   "With your aura full, after three full swings and a low swing: a hammer of the world falls where you stand. The foe you face is struck hardest, every foe near is cut, thrown back and its armour cracked wide for eight seconds, and your guard sets like iron."),
    "grit_flick": ("Grit Flick",
                   "Swing, swing, then a low swing: a flick of the blade throws sand in the eyes of the foes in front, blinding them a moment (a creature loses you) and slowing them."),
    "quicksand": ("Quicksand",
                  "A leaping swing, then a low swing: the ground ahead turns to quicksand for four seconds, swallowing the feet of every foe in it (slowed hard, drawn to its heart) and grinding at them."),
    "sandveil": ("Sandveil",
                 "The counter: a burst of sand round you blinds every foe near, and for a moment you move quickly and nothing quite finds you."),
    "dune_runner": ("Dune Runner",
                    "Straight after your step, a run seven blocks on across shifting sand: every foe in the way is cut, its footing gives way (slowed, sinking) and it's turned aside."),
    "sea_of_sand": ("Sea of Sand",
                    "With your aura full, after three full swings and a low swing: the desert rises round you. Gusts of sand blind and sink every foe near, and then the dunes close over them."),
}

_SOVEREIGNS = {
    "drowning_tide": ("Drowning Tide",
                      "Tide's awakened Dominion: the ground is under water. Each second the foes inside are pushed toward its rim and soaked, and while you stand in it no fire holds on you."),
    "anvil_court": ("Anvil Court",
                    "Iron's awakened Dominion: each second a hammer of sparks cracks the armour of every foe inside and strikes one of them, and while you stand in it you're hardened against blows."),
    "shifting_sea": ("Shifting Sea",
                     "Dune's awakened Dominion: the ground is shifting sand. Each second every foe inside sinks (slowed hard) and one of them is blinded by the blowing grit."),
}

_FINISHERS = {
    "undertow": ("Undertow", "Tide's finisher: a wave breaks over the foe and the undertow drags it away, soaked."),
    "quench": ("Quench", "Iron's finisher: a hammer of sparks falls on the foe and its armour cracks wide for five seconds."),
    "dust_devil": ("Dust Devil", "Dune's finisher: a spinning column of sand round the foe, which comes out of it blinded."),
}


def _lang():
    out = {}
    for method_id, (name, lore, flavour) in _METHODS.items():
        out[f"aura.wildercord.method.{method_id}"] = name
        out[f"aura.wildercord.method.{method_id}.lore"] = lore
        out[f"aura.wildercord.method.{method_id}.flavour"] = flavour
    for art_id, (name, desc) in _ARTS.items():
        out[f"aura.wildercord.art.{art_id}"] = name
        out[f"aura.wildercord.art.{art_id}.desc"] = desc
    for sovereign_id, (name, desc) in _SOVEREIGNS.items():
        out[f"aura.wildercord.sovereign.{sovereign_id}"] = name
        out[f"aura.wildercord.sovereign.{sovereign_id}.desc"] = desc
    for finisher_id, (name, desc) in _FINISHERS.items():
        out[f"aura.wildercord.finisher.{finisher_id}"] = name
        out[f"aura.wildercord.finisher.{finisher_id}.desc"] = desc
    out.update({
        "element.wildercord.brine": "Brine",
        "element.wildercord.metal": "Metal",
        "element.wildercord.sand": "Sand",
        "screen.wildercord.aura.writing.element.current": "pushes foes back %s blocks and soaks them",
        "screen.wildercord.aura.writing.element.forge": "cracks %s armour off its foes",
        "screen.wildercord.aura.writing.element.grit": "blinds its foes %s s",
    })
    return out


# ---------------------------------------------------------------- the Sword Masters

MASTERS = ("tide", "iron", "dune")

# Their named techniques (MethodsAMasters' tables), in table order: ids 101 onward after the original hundred.
TECHNIQUES = {
    "tide": ["tide_cut", "ebb_and_flow", "spring_tide", "breakwater", "foam_needle", "undertow_hook", "rising_swell",
             "crashing_surf", "whirl_of_the_deep", "neap_reply", "coral_lance", "spindrift", "tidal_bore", "kelp_snare",
             "moon_pull", "riptide_turn", "salt_spray", "harbour_gate", "deep_current", "storm_surge", "leviathan_coil",
             "last_tide"],
    "iron": ["iron_cut", "hammer_and_tongs", "bellows_thrust", "quench_drop", "rivet_line", "ingot_press", "temper_turn",
             "slag_sweep", "plate_breaker", "mail_cutter", "crucible", "tongs_hook", "forge_lance", "annealing_reply",
             "spark_shower", "cold_iron", "rampart_breaker", "smelter_rise", "riveted_cross", "blast_furnace",
             "steel_verdict", "last_ingot"],
    "dune": ["grit_cut", "sirocco", "mirage_step", "dune_crest", "sand_lash", "dust_needle", "scarab_hook", "sinking_cut",
             "haboob", "oasis_reply", "dry_lightning", "caravan_line", "dune_slide", "scorpion_tail", "dervish_turn",
             "desert_wind", "shifting_dune", "sunbaked_cross", "simoom_spiral", "buried_gate", "erg_storm", "last_dune"],
}


def _title(key):
    return " ".join(word if word in ("and", "of", "the") else word.capitalize() for word in key.split("_"))


def _master_lang():
    out = {
        "master.wildercord.school.tide": "Tide",
        "master.wildercord.school.iron": "Iron",
        "master.wildercord.school.dune": "Dune",
        "message.wildercord.master.tide_lesson": "Tide master: Step inside my wave, then back out before the undertow pulls.",
        "message.wildercord.master.iron_lesson": "Iron master: Leave the spot my hammer marks, then jump as the anvil rings.",
        "message.wildercord.master.dune_lesson": "Dune master: Sidestep my sand lane, then step back into it before the storm.",
        "boss.wildercord.master.tide_undertow_ring": "Undertow Ring",
        "boss.wildercord.master.iron_anvil_verdict": "Anvil Verdict",
        "boss.wildercord.master.dune_shifting_sands": "Shifting Sands",
        "message.wildercord.master.undertow_hint": "Step inside the wave, then back out past the undertow!",
        "message.wildercord.master.anvil_hint": "Leave the marked spot, then jump the ringing ground!",
        "message.wildercord.master.sands_hint": "Sidestep the lane, then step back into the pale sand!",
    }
    for keys in TECHNIQUES.values():
        out.update({"boss.wildercord.master.technique." + key: _title(key) for key in keys})
    return out


LANG = _lang()
LANG.update(_master_lang())


def _master_blade(school):
    """Each Master's blade on the forged weapons' diagonal: Tidecaller, Anvilbrand and Sandglass."""
    from item_art import hexc
    from masters_art import _blade_icon, _diagonal
    if school == "tide":
        # Tidecaller: a curved sea-green blade with a foam-white edge and a wave of brighter steel along its back.
        pal = {"E": hexc("#F2FFFA"), "T": hexc("#58C2BE"), "D": hexc("#1E6E78")}
        blade = _diagonal({14: (6, 12, "E"), 15: (6, 13, lambda x: pal["E"] if x == 13 else pal["T"] if x % 3 else hexc("#9EE8DC")),
                           16: (7, 13, "D")}, pal)
        silver, dim = hexc("#D8E8EC"), hexc("#6E8C94")
        guard = [(4, 9, dim), (5, 10, silver), (6, 11, dim), (5, 9, hexc("#9EE8DC"))]
        grip = [(4, 11, hexc("#1A4A58")), (3, 12, hexc("#2A6474")), (2, 13, hexc("#1A4A58"))]
        tassel = [(0, 15, hexc("#D8FFF6")), (1, 15, hexc("#58C2BE"))]
        return _blade_icon(blade, guard, grip, (1, 14, silver), hexc("#0E2228"), tassel)
    if school == "iron":
        # Anvilbrand: a thick, straight bar of blued steel with a glowing orange temper line and a square iron guard.
        pal = {"E": hexc("#E4ECF2"), "M": hexc("#9AA6B2"), "D": hexc("#4E5862")}
        blade = _diagonal({13: (5, 11, "E"), 14: (5, 12, "M"), 15: (6, 13, lambda x: hexc("#FF9A3C") if x % 2 else hexc("#FFC07A")),
                           16: (7, 13, "M"), 17: (8, 13, "D")}, pal)
        iron, dark = hexc("#7A848E"), hexc("#3A4048")
        guard = [(2, 8, dark), (3, 9, iron), (4, 10, hexc("#B8C2CC")), (5, 11, iron), (6, 12, iron), (7, 13, dark)]
        grip = [(3, 12, hexc("#3A2A22")), (2, 13, hexc("#523C2E"))]
        return _blade_icon(blade, guard, grip, (1, 14, iron), hexc("#16181C"))
    # Sandglass: a narrow, slightly tapered blade the colour of sunlit sand, a pale edge and a wrapped ochre grip.
    pal = {"E": hexc("#FFF4D2"), "S": hexc("#E2B462"), "D": hexc("#9A6A2A")}
    blade = _diagonal({14: (6, 12, "E"), 15: (6, 14, lambda x: pal["E"] if x == 14 else pal["S"]), 16: (7, 12, "D")}, pal)
    gold, dim = hexc("#E8C468"), hexc("#987024")
    guard = [(5, 10, hexc("#FFE08A")), (5, 9, gold), (4, 10, gold), (6, 10, dim), (5, 11, dim)]
    grip = [(4, 11, hexc("#B07A34")), (3, 12, hexc("#8A5A22")), (2, 13, hexc("#B07A34"))]
    return _blade_icon(blade, guard, grip, (1, 14, gold), hexc("#2A1C10"))


def write(g):
    """The methods' manuals are tools/aura_art.py's; this draws the Tide, Iron and Dune Masters and their blades."""
    from masters_art import master_texture
    for school in MASTERS:
        g.save(master_texture(school), g.ASSETS / f"textures/entity/master/{school}.png")
        g.save(_master_blade(school), g.ASSETS / f"textures/item/master_blade/{school}.png")
        g.write_json(g.ASSETS / f"models/item/master_blade/{school}.json", {
            "parent": "minecraft:item/handheld", "textures": {"layer0": f"wildercord:item/master_blade/{school}"}})
        g.write_json(g.ASSETS / f"items/master_blade/{school}.json", {
            "model": {"type": "minecraft:model", "model": f"wildercord:item/master_blade/{school}"}})
