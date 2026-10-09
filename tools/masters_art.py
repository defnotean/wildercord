"""Masters of Tomorrow language and its spawn egg, generated with the existing aura art pipeline."""

LANG = {
    "master.wildercord.school.ember": "Ember",
    "master.wildercord.school.gale": "Gale",
    "master.wildercord.school.stone": "Stone",
    "message.wildercord.master.first_clear": "First clear recorded: %s. Your technique lesson is permanent.",
    "message.wildercord.master.first_clear_known": "First clear recorded: %s. You already know this school's technique lesson.",
    "message.wildercord.master.record_clear": "%s: cleared",
    "message.wildercord.master.record_open": "%s: not yet cleared",
    "entity.wildercord.sword_master": "Sword Master",
    "item.wildercord.sword_master_spawn_egg": "Sword Master Spawn Egg",
    "key.category.wildercord.masters_arts": "Wildercord: Master's Arts",
    "key.wildercord.spellcut": "Spellcut",
    "key.wildercord.rising_break": "Rising Break",
    "key.wildercord.driving_cut": "Driving Cut",
    "hud.wildercord.masters_aim_left": "Strike committed left",
    "hud.wildercord.masters_aim_right": "Strike committed right",
    "hud.wildercord.masters_aim_up": "Strike committed above",
    "hud.wildercord.masters_aim_down": "Strike committed below",
    "screen.wildercord.aura.masters_help": "Master's Arts",
    "screen.wildercord.aura.masters_spellcut": "%s: Spellcut",
    "screen.wildercord.aura.masters_rising_break": "%s: Rising Break",
    "screen.wildercord.aura.masters_driving_cut": "%s: Driving Cut",
    "screen.wildercord.aura.masters_rebind": "Rebind in Options > Controls > Key Binds > Wildercord: Master's Arts",
    "aura.wildercord.art.spellcut": "Spellcut",
    "aura.wildercord.art.spellcut.desc": "Edge: after a short windup, sever up to 3 incoming hostile bolts in front and slash nearby foes. 14 aura; 3 second rest.",
    "aura.wildercord.art.rising_break": "Rising Break",
    "aura.wildercord.art.rising_break.desc": "Form: a rising blade strike wears extra stance and lifts ordinary foes. 20 aura; 5 second rest.",
    "aura.wildercord.art.driving_cut": "Driving Cut",
    "aura.wildercord.art.driving_cut.desc": "Form: a committed 5-block thrust breaks a damaged held spell, including absorption, then seals casting for 1 second. Held spells share 8-second interrupt protection. 18 aura; 4 second rest.",
    "message.wildercord.masters_art.stage": "This art opens at %s.",
    "message.wildercord.masters_art.rest": "Recovering: %s seconds remaining.",
    "message.wildercord.masters_art.aura": "This art needs %s aura.",
    "message.wildercord.master.dialogue": "%s Master: A lethal trial for up to 8 challengers. Speak to me again within 10 seconds to accept.",
    "message.wildercord.master.challenge": "%s Master: The trial begins in 30 seconds. Others can speak to me twice to join. Stay within 24 blocks. Type /master ready to start sooner.",
    "message.wildercord.master.joined": "You joined the trial. Tip: use /party invite so your spells spare your allies.",
    "message.wildercord.master.ready": "The trial is starting.",
    "message.wildercord.master.unavailable": "Trials need Survival mode, a non-Peaceful world and no active duel or spar.",
    "message.wildercord.master.teacher_hint": "Sneak and speak to me to request a Master trial.",
    "message.wildercord.master.teacher_offer": "I can summon a Master for a lethal trial. Sneak and speak to me again to call them.",
    "message.wildercord.master.teacher_arrived": "The %s Master waits nearby for one minute. Speak to them twice to accept.",
    "message.wildercord.master.teacher_busy": "Finish your current duel or spar first.",
    "message.wildercord.master.teacher_unready": "Master trials open at Aura Form or Heart Circle VIII.",
    "message.wildercord.master.reserved": "This master is waiting for the player who called them.",
    "message.wildercord.master.occupied": "A trial is already active nearby.",
    "message.wildercord.master.obstructed": "Find clear, level ground for the arena.",
    "message.wildercord.master.no_trial": "No open trial nearby, or it is full.",
    "message.wildercord.master.waiting": "You're enrolled. The trial starts soon.",
    "message.wildercord.master.in_progress": "This trial has already begun.",
    "message.wildercord.master.victory": "You defeated the %s Master.",
    "boss.wildercord.master.status": "%s Master · %s",
    "boss.wildercord.master.prepare": "%s/%s challengers · Starts in %ss",
    "boss.wildercord.master.sweep": "Sweeping cut",
    "boss.wildercord.master.thrust": "Piercing thrust",
    "boss.wildercord.master.crescent": "Crescent volley",
    "boss.wildercord.master.break_cast": "Spellbreaker thrust",
    "boss.wildercord.master.pursuit_break": "Pursuit dash",
    "boss.wildercord.master.pursuit_strike": "Pursuit strike",
    "message.wildercord.master.pursuit_hint": "Leave the lane or parry!",
    "message.wildercord.master.pursuit_lesson": "Hold a spell too long and I will dash at you: release, sidestep or parry.",
    "boss.wildercord.master.crosswind_reprise": "Crosswind Reprise",
    "boss.wildercord.master.crosswind_reply": "Crosswind reply",
    "message.wildercord.master.crosswind_hint": "Sidestep or parry!",
    "message.wildercord.master.gale_lesson": "Gale master: I step aside, then strike where you stood. Leave the lane or parry, then counter.",
    "boss.wildercord.master.stone_fracture": "Stone Fracture",
    "boss.wildercord.master.fracture_reply": "Fracture reply",
    "message.wildercord.master.fracture_hint": "Sidestep or parry!",
    "message.wildercord.master.stone_lesson": "Stone master: Hit my back or axe my guard while I brace. Then dodge the reply and counter.",
    "boss.wildercord.master.kiln_ring": "Kiln Ring",
    "boss.wildercord.master.stone_fault_march": "Stone Fault March",
    "message.wildercord.master.march_hint": "Sidestep, jump the pulse, or step onto spent ground.",
    "message.wildercord.master.kiln_hint": "Get inside the blue rim, outside it, or jump the ring.",
    "boss.wildercord.master.cinder_wake": "Cinder Wake",
    "boss.wildercord.master.afterburn": "Afterburn",
    "message.wildercord.master.afterburn_hint": "Step out of the marked lanes!",
    "message.wildercord.master.ember_lesson": "Ember master: Leave my broad cut, then step out of the burning lanes.",
    "boss.wildercord.master.broken": "Stance broken",
    "boss.wildercord.master.recover": "Open to counters",
    "boss.wildercord.master.guard": "Guarding",
    "boss.wildercord.master.ready": "Reading the field",
    "boss.wildercord.master.breathing": "Catching breath",
}

# The masters' hundred named combos (MasterTechniques), Ember then Gale then Stone.
TECHNIQUES = [
    'kindling_cut', 'ember_cross', 'rising_flame', 'searing_lunge', 'hearth_breaker', 'cinder_flurry', 'flare_step',
    'scorch_line', 'wildfire_turn', 'bellows_drive', 'brand_and_burn', 'kiln_spiral', 'spark_scatter',
    'blaze_crescent', 'coalfall', 'furnace_gate', 'smoke_feint', 'ashen_reply', 'molten_arc', 'pyre_crash',
    'lantern_sweep', 'flash_point', 'burning_wheel', 'tinder_snap', 'crown_of_flame', 'ember_rain', 'firebrand_rush',
    'forge_hammer', 'dragon_coil', 'flicker_cut', 'slag_turn', 'sunset_draw', 'inferno_chain', 'last_cinder',
    'zephyr_point', 'crosswind_pair', 'gust_cut', 'swallow_turn', 'jade_needle', 'kite_string', 'squall_line',
    'updraft', 'feather_fall', 'tailwind_rush', 'cyclone', 'hawk_dive', 'reed_bend', 'whistling_reply',
    'skyward_cross', 'gale_lattice', 'drifting_palm', 'vortex', 'sparrow_flurry', 'headwind', 'shear', 'dust_devil',
    'storm_eye', 'thousand_leaves', 'needle_rain', 'wind_rider', 'crane_wing', 'slipstream', 'tempest_spiral',
    'breeze_feint', 'cloud_split', 'howling_cross', 'last_gale', 'boulder_drop', 'bedrock_sweep', 'quarry_cut',
    'landslide', 'obelisk_thrust', 'cairn_breaker', 'mountain_turn', 'ridge_line', 'granite_palm', 'avalanche_step',
    'fault_cross', 'iron_root', 'crag_hammer', 'monolith_drive', 'gravel_spin', 'tectonic_shift', 'keystone',
    'cliff_face', 'basalt_wall', 'marble_reply', 'sediment', 'quake_lunge', 'rockslide_chain', 'anvil_fall',
    'tor_spiral', 'shale_split', 'bastion', 'rift_open', 'stone_rain', 'menhir', 'deep_quarry', 'earthsplitter',
    'last_stone',
]
LANG["boss.wildercord.master.technique"] = "Technique"
LANG.update({"boss.wildercord.master.technique." + key: " ".join(word if word in ("and", "of") else word.capitalize()
    for word in key.split("_")) for key in TECHNIQUES})


def write(g):
    for school in SCHOOLS:
        g.save(master_texture(school), g.ASSETS / f"textures/entity/master/{school}.png")
        g.save(master_blade(school), g.ASSETS / f"textures/item/master_blade/{school}.png")
        g.write_json(g.ASSETS / f"models/item/master_blade/{school}.json", {
            "parent": "minecraft:item/handheld", "textures": {"layer0": f"wildercord:item/master_blade/{school}"}})
        g.write_json(g.ASSETS / f"items/master_blade/{school}.json", {
            "model": {"type": "minecraft:model", "model": f"wildercord:item/master_blade/{school}"}})
    g.save(master_egg(), g.ASSETS / "textures/item/sword_master_spawn_egg.png")
    g.write_json(g.ASSETS / "models/item/sword_master_spawn_egg.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "wildercord:item/sword_master_spawn_egg"},
    })
    g.write_json(g.ASSETS / "items/sword_master_spawn_egg.json", {
        "model": {"type": "minecraft:model", "model": "wildercord:item/sword_master_spawn_egg"},
    })


# ============================================================== the masters' own look
# A Sword Master wears the duelist's cut (MasterModel bakes the same 64x64 layer) but dressed as the head of a school: a face
# shown, not masked, a short greying beard; gilt round the hood; a lacquered mantle with rivets; the school's sigil embroidered
# on a deeper-dyed cloak with a gilt hem; plated bracers; a medallion on the sash. Its blade is the school's own, set on its
# sword by the item_model component, so it never drops or stacks as an item.

SCHOOLS = ("ember", "gale", "stone")


def _methods():
    import aura_art
    return {m: (c, h) for m, _, c, h in aura_art.METHODS}


def master_texture(method_id):
    import aura_world_art as w
    from monster_art import LIGHT, cells, faces, shade, smooth
    from item_art import mix
    from aura_art import EMBLEMS, rgb
    color, highlight = _methods()[method_id]
    image = w.duelist_texture(method_id, color, highlight).convert("RGBA")
    px = image.load()

    def put(x, y, c, a=255):
        px[x, y] = (c[0], c[1], c[2], a)

    c = rgb(color)
    hi = rgb(highlight)
    seed = sum(ord(ch) for ch in method_id) + 101
    gold = [w.shade(w.BRASS, i) for i in range(5)]
    dye = [w.tone(mix(c, (58, 44, 40), 0.38), k) for k in (0.38, 0.52, 0.68, 0.84, 0.98, 1.1)]
    # The face: shown, a short beard greying at the chin, a set mouth.
    fx, fy, _, _ = w.D_HEAD["front"]
    beard = [w.shade(w.HAIR, 2), (92, 86, 82), (128, 122, 116)]
    face = """
        ........
        ........
        ........
        ........
        ........
        ....n...
        BbbmmbbB
        BBbggbBB
    """
    pal = {"n": w.shade(w.SKIN, 1), "m": (70, 40, 34), "b": beard[0], "B": beard[0], "g": beard[2]}
    for y, row in enumerate(r.strip() for r in face.strip("\n").splitlines()):
        for x, ch in enumerate(row):
            if ch in pal:
                col = pal[ch]
                if ch == "B" and (x + y) % 2:
                    col = beard[1]
                put(fx + x, fy + y, col)
    # The hood: re-dyed deeper, its rim gilt.
    for name, area in faces(w.D_HOOD):
        for x, y, ppx, ppy in cells(area):
            if px[ppx, ppy][3]:
                t = 2 + LIGHT[name] + (1 if smooth(ppx, ppy, seed, 2.4) > 0.68 else 0)
                put(ppx, ppy, shade(dye, t))
    hx, hy, hw, hh = w.D_HOOD["front"]
    for y in range(hh):
        for x in range(hw):
            if (x in (0, 7) and y >= 1) or (y == 1 and 1 <= x <= 6):
                put(hx + x, hy + y, gold[4] if (x + y) % 3 == 0 else gold[3])
            elif 1 <= x <= 6 and y >= 2:
                px[hx + x, hy + y] = (0, 0, 0, 0)
    # The body: gilt lapels, the sash in the school's colour, a medallion at its centre.
    bx, by, bw, bh = w.D_BODY["front"]
    for y in range(7):
        for x in range(bw):
            if x == 3 - min(3, y // 2) or x == 4 + min(3, y // 2):
                put(bx + x, by + y, gold[3] if y % 3 else gold[4])
    for x, y, col in ((3, 7, gold[4]), (4, 7, gold[3]), (3, 8, gold[2]), (4, 8, gold[1])):
        put(bx + x, by + y, col)
    put(bx + 3, by + 7, hi)
    # The mantle: lacquered in the school's colour, a gilt edge along the shoulder and a row of rivets.
    for name, area in faces(w.D_MANTLE):
        x0, y0, mw, mh = area
        for x, y, ppx, ppy in cells(area):
            t = 3 + LIGHT[name] + (1 if y == 0 and name not in ("top", "bottom") else 0) \
                - (1 if smooth(ppx, ppy, seed + 9, 2.5) > 0.74 else 0)
            put(ppx, ppy, shade(dye, t))
        if name not in ("top", "bottom"):
            for x in range(mw):
                put(x0 + x, y0 + mh - 1, gold[3] if x % 2 else gold[2])
                if x % 3 == 1:
                    put(x0 + x, y0 + 1, gold[4])
        elif name == "top":
            for x in range(mw):
                put(x0 + x, y0 + mh - 1, gold[3])
    # The cloak: the deeper dye, a gilt hem, the school's sigil embroidered on its back (and the face toward the body).
    emblem = EMBLEMS[method_id]
    for name, area in faces(w.D_CLOAK):
        x0, y0, cw, ch = area
        for x, y, ppx, ppy in cells(area):
            if not px[ppx, ppy][3]:
                continue
            t = 2 + LIGHT[name] + (1 if smooth(ppx * 0.7, ppy * 0.25, seed + len(name), 2.4) > 0.68 else 0) \
                - (1 if (x + y) % 2 == 0 and smooth(ppx, ppy, seed + 3, 1.2) > 0.8 else 0)
            put(ppx, ppy, shade(dye, t))
        if name in ("front", "back"):
            for x in range(cw):
                for y in (ch - 2, ch - 3):
                    if px[x0 + x, y0 + y][3]:
                        put(x0 + x, y0 + y, gold[3] if y == ch - 3 else gold[2])
                if px[x0 + x, y0 + ch - 1][3]:
                    put(x0 + x, y0 + ch - 1, dye[1])
            for y in range(ch - 3):
                put(x0, y0 + y, w.tone(c, 0.7))
                put(x0 + cw - 1, y0 + y, w.tone(c, 0.7))
            ex, ey = x0 + (cw - 6) // 2, y0 + 4
            for y, row in enumerate(emblem):
                for x, mark in enumerate(row):
                    if mark == "#":
                        put(ex + x, ey + y, hi if y < 2 else w.tone(hi, 0.86))
                    elif mark == "+":
                        put(ex + x, ey + y, gold[4])
    # The forearms: plated bracers, gilt-edged, instead of the duelist's wraps.
    for name, area in faces(w.D_ARM):
        x0, y0, aw, _ = area
        if name in ("top", "bottom"):
            continue
        for x in range(aw):
            for y in range(6, 10):
                t = 3 + LIGHT[name] + (1 if y == 6 else -1 if y == 9 else 0)
                put(x0 + x, y0 + y, mix(shade(w.STEEL, t), c, 0.18))
            put(x0 + x, y0 + 6, gold[3])
            put(x0 + x, y0 + 9, gold[1])
    return image



def _blade_icon(blade, guard, grip, pommel, outline, tassel=None):
    """A 16x16 handheld blade on the forged weapons' diagonal (hilt bottom left, point top right), outlined in one dark pixel.
    blade: {(x, y): colour}; guard, grip: lists of (x, y, colour); pommel: (x, y, colour); tassel: hanging (x, y, colour)."""
    from item_art import Canvas
    cv = Canvas()
    filled = {}
    for (x, y), col in blade.items():
        filled[(x, y)] = col
    for x, y, col in guard + grip + [pommel] + (tassel or []):
        filled[(x, y)] = col
    for (x, y) in filled:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, y + dy)
            if n not in filled and 0 <= n[0] < 16 and 0 <= n[1] < 16:
                cv.put(n[0], n[1], outline)
    for (x, y), col in filled.items():
        cv.put(x, y, col)
    return cv.image()


def _diagonal(spans, palette):
    """Blade pixels by anti-diagonal: spans maps s = x + y to (first x, last x, palette key); later spans paint over earlier."""
    out = {}
    for s, (x0, x1, key) in spans.items():
        for x in range(x0, x1 + 1):
            out[(x, s - x)] = palette[key] if isinstance(key, str) else key(x)
    return out


def master_blade(school):
    from item_art import hexc
    if school == "ember":
        # Kilnbrand: dark forge-steel with a molten seam down its middle and a white-hot edge, an oxblood grip and a gilt guard.
        pal = {"E": hexc("#FFC46A"), "R": hexc("#F06E32"), "S": hexc("#3C3236"), "T": hexc("#FFF2C0")}
        blade = _diagonal({14: (6, 12, "E"), 15: (6, 13, lambda x: pal["T"] if x >= 13 else pal["R"] if x % 3 else hexc("#FFA048")),
                           16: (7, 13, "S")}, pal)
        gold, dark = hexc("#E8C468"), hexc("#987024")
        guard = [(3, 8, dark), (4, 9, gold), (5, 10, hexc("#FFE08A")), (6, 11, gold), (7, 12, dark)]
        grip = [(4, 11, hexc("#8E2A20")), (3, 12, hexc("#5E1A16")), (2, 13, hexc("#8E2A20"))]
        return _blade_icon(blade, guard, grip, (1, 14, gold), hexc("#1E1418"))
    if school == "gale":
        # Crosswind: a slender jade-steel blade with a bright edge, a diamond guard and a pale tassel trailing from the pommel.
        pal = {"E": hexc("#FFFFFF"), "J": hexc("#A8DCC6"), "D": hexc("#5E9884")}
        blade = _diagonal({14: (6, 12, "E"), 15: (6, 14, lambda x: pal["E"] if x == 14 else pal["J"]), 16: (7, 12, "D")}, pal)
        silver, gold, dim = hexc("#D8E2E4"), hexc("#E8C468"), hexc("#987024")
        guard = [(5, 10, hexc("#FFE08A")), (5, 9, gold), (4, 10, gold), (6, 10, dim), (5, 11, dim)]
        grip = [(4, 11, hexc("#2C5A52")), (3, 12, hexc("#3E7A6E")), (2, 13, hexc("#2C5A52"))]
        tassel = [(0, 15, hexc("#C8F0DC")), (1, 15, hexc("#8CC8B0"))]
        return _blade_icon(blade, guard, grip, (1, 14, silver), hexc("#14262A"), tassel)
    # Faultbreaker: a broad, square-pointed slab of stone-grey steel, a sandstone crack down its fuller, a heavy bronze guard.
    pal = {"E": hexc("#D8D2C4"), "M": hexc("#A49C8E"), "C": hexc("#E8C890"), "D": hexc("#6A6258")}
    blade = _diagonal({13: (5, 11, "E"), 14: (5, 12, "M"), 15: (6, 13, lambda x: hexc("#C8A06A") if x % 2 else pal["C"]),
                       16: (7, 13, "M"), 17: (8, 13, "D")}, pal)
    bronze, dark = hexc("#C29A3A"), hexc("#6E5018")
    guard = [(2, 8, dark), (3, 9, bronze), (4, 10, hexc("#E8C468")), (5, 11, bronze), (6, 12, bronze), (7, 13, dark),
             (3, 8, bronze), (6, 13, dark)]
    grip = [(3, 12, hexc("#4A2C1C")), (2, 13, hexc("#5E3A26"))]
    return _blade_icon(blade, guard, grip, (1, 14, bronze), hexc("#1C1A1E"))


def master_egg():
    """The Sword Master's spawn egg: a dark lacquered shell flecked with gilt, an upright blade with a gilt guard."""
    from fungal_art import egg_icon
    motif = """
        ................
        ................
        ........w.......
        .......wW.......
        .......wW.......
        .......wW.......
        .......wW.......
        .....GGGGGG.....
        ........r.......
        ........r.......
        .......GG.......
        ................
    """
    return egg_icon(("#1E1A22", "#3A3040", "#5A4C5E"), "#E8C468", motif,
                    {"w": "#FFFFFF", "W": "#A8B4C4", "G": "#E8C468", "r": "#8E2A20"}, 77)
