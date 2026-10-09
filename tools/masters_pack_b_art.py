"""Starlit, Hourglass and Crimson Masters (masters-b pack): language, master dress, blades and their item models."""

import masters_art

SCHOOLS = ("starlit", "hourglass", "crimson")

LANG = {
    "master.wildercord.school.starlit": "Starlit",
    "master.wildercord.school.hourglass": "Hourglass",
    "master.wildercord.school.crimson": "Crimson",
    "message.wildercord.master.starlit_lesson": "Starlit master: I mark four stars and they burst in the drawn order. Leave each star before its turn.",
    "message.wildercord.master.hourglass_lesson": "Hourglass master: my long thrust replays down the gold lane. Sidestep it and do not step back in.",
    "message.wildercord.master.crimson_lesson": "Crimson master: I bleed to strike four times and heal only when I land. Jump low, duck high, sidestep, then get clear.",
    "boss.wildercord.master.starlit_constellation": "Constellation",
    "boss.wildercord.master.hourglass_rewind": "Rewind",
    "boss.wildercord.master.crimson_frenzy": "Blood Frenzy",
    "message.wildercord.master.constellation_hint": "Leave each star before it bursts!",
    "message.wildercord.master.rewind_hint": "Sidestep, then stay off the gold lane!",
    "message.wildercord.master.frenzy_hint": "Jump, duck, sidestep, then get clear!",
    "message.wildercord.master.answer.jump": "Jump!",
    "message.wildercord.master.answer.duck": "Crouch!",
    "message.wildercord.master.answer.sidestep": "Sidestep!",
    "message.wildercord.master.answer.clear": "Get clear!",
}

# MasterTechniques ids 205 to 282 (after the masters-a tables), in MastersPackB.techniques order: Starlit, then Hourglass, then Crimson.
TECHNIQUES = [
    'first_light', 'twin_stars', 'falling_star', 'polaris', 'star_needle', 'night_arc', 'comet_tail', 'meteor_drop',
    'orbit', 'zenith', 'nadir', 'starlit_gate', 'pleiades', 'dusk_cross', 'moonrise', 'aurora_veil', 'sirius_point',
    'eclipse', 'nebula_turn', 'quasar_lance', 'starfall_rain', 'celestial_wheel', 'zodiac_chain', 'dawn_star',
    'lodestar', 'last_constellation',
    'grain_cut', 'second_hand', 'pendulum', 'tick_tock', 'sandfall', 'hour_hand', 'minute_hand', 'turning_glass',
    'long_hour', 'echo_step', 'dial_sweep', 'noon_bell', 'midnight_bell', 'sundial_turn', 'recurrence',
    'hourglass_flip', 'dune_clock', 'time_lapse', 'paused_blade', 'chime_spiral', 'sand_wake', 'measured_cross',
    'eternal_turn', 'borrowed_hour', 'clockwork_chain', 'last_grain',
    'first_blood', 'red_fang', 'vein_cut', 'heart_strike', 'crimson_tide', 'blood_moon', 'scarlet_rush', 'iron_taste',
    'bleeding_edge', 'wound_fan', 'red_rising', 'pulse_beat', 'hemorrhage', 'sanguine_wheel', 'rose_thorn',
    'claret_cross', 'fever_drive', 'vermilion_spiral', 'gore_gate', 'blood_rain', 'ruby_reply', 'carmine_hook',
    'heartbeat_chain', 'red_harvest', 'mortal_lunge', 'last_drop',
]
LANG.update({"boss.wildercord.master.technique." + key: " ".join(word if word in ("and", "of") else word.capitalize()
    for word in key.split("_")) for key in TECHNIQUES})


def write(g):
    for school in SCHOOLS:
        g.save(masters_art.master_texture(school), g.ASSETS / f"textures/entity/master/{school}.png")
        g.save(master_blade(school), g.ASSETS / f"textures/item/master_blade/{school}.png")
        g.write_json(g.ASSETS / f"models/item/master_blade/{school}.json", {
            "parent": "minecraft:item/handheld", "textures": {"layer0": f"wildercord:item/master_blade/{school}"}})
        g.write_json(g.ASSETS / f"items/master_blade/{school}.json", {
            "model": {"type": "minecraft:model", "model": f"wildercord:item/master_blade/{school}"}})


def master_blade(school):
    from item_art import hexc
    blade_icon, diagonal = masters_art._blade_icon, masters_art._diagonal
    gold, dim = hexc("#E8C468"), hexc("#987024")
    if school == "starlit":
        # Lodestar: night-blue steel with a silver edge and white star points along its fuller, a crescent silver guard.
        pal = {"E": hexc("#E8F0FF"), "N": hexc("#2C3A78"), "D": hexc("#18204A")}
        blade = diagonal({14: (6, 12, "E"), 15: (6, 13, lambda x: hexc("#FFFFFF") if x % 3 == 0 else pal["N"]), 16: (7, 13, "D")}, pal)
        silver = hexc("#D8E2F4")
        guard = [(3, 8, hexc("#8C9AC4")), (4, 9, silver), (5, 10, hexc("#FFFFFF")), (6, 11, silver), (7, 12, hexc("#8C9AC4"))]
        grip = [(4, 11, hexc("#24285A")), (3, 12, hexc("#363C80")), (2, 13, hexc("#24285A"))]
        return blade_icon(blade, guard, grip, (1, 14, silver), hexc("#0C0E22"))
    if school == "hourglass":
        # Sandglass: pale brass steel with a sand-gold fuller, a pinched glass-waist guard and a turning-weight pommel.
        pal = {"E": hexc("#FFF4D8"), "S": hexc("#D8B46A"), "B": hexc("#8A6A34")}
        blade = diagonal({14: (6, 12, "E"), 15: (6, 13, lambda x: hexc("#F2D98A") if x % 2 else pal["S"]), 16: (7, 13, "B")}, pal)
        guard = [(3, 8, dim), (4, 9, gold), (5, 10, hexc("#BFE4F0")), (6, 11, gold), (7, 12, dim)]
        grip = [(4, 11, hexc("#5A4024")), (3, 12, hexc("#7A5832")), (2, 13, hexc("#5A4024"))]
        return blade_icon(blade, guard, grip, (1, 14, gold), hexc("#20180E"))
    # Heartletter: a dark crimson blade with a bright red edge and a blood-groove, a black guard and a red tassel.
    pal = {"E": hexc("#FF8A8A"), "R": hexc("#A01C2C"), "D": hexc("#4A0E16")}
    blade = diagonal({14: (6, 12, "E"), 15: (6, 13, lambda x: hexc("#E03040") if x % 2 else pal["R"]), 16: (7, 13, "D")}, pal)
    black = hexc("#2A2026")
    guard = [(3, 8, black), (4, 9, hexc("#4A3A44")), (5, 10, gold), (6, 11, hexc("#4A3A44")), (7, 12, black)]
    grip = [(4, 11, hexc("#3A0E14")), (3, 12, hexc("#5E1820")), (2, 13, hexc("#3A0E14"))]
    tassel = [(0, 15, hexc("#E03040")), (1, 15, hexc("#A01C2C"))]
    return blade_icon(blade, guard, grip, (1, 14, gold), hexc("#14060A"), tassel)
