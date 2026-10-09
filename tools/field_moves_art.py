"""The moves pack: three parry follow-ups, two aerial forms and Spell Cut. Lesson covers and all player-facing text."""

_FORMS = {
    "ember_riposte": {
        "name": "Ember Riposte",
        "locked": "Learn at Form after a recorded Ember Master clear, from a wandering Ember teacher.",
        "controls": "After a perfect guard catches a hit, press %s within a moment while holding forward: a short dash and a cut.",
        "limits": "Up to 2.4 blocks toward the attacker. Walls and ledges stop it with no cut. No slot needed. No invulnerability.",
        "cost": "12 Aura. 0.6s recovery, 1.2s if stopped.",
        "diagram": ("1 Catch", "2 Close", "3 Cut"),
        "book": (
            "Hessa never lunged first. She let the raider swing, took it on the flat of her blade, and only then went. 'A blocked man is a man with his arms in the wrong place,' she said. 'Use that.'",
            "The answer is short. You have just caught a blow, and your feet are still under you. Two steps, no more, and a cut while their weapon is still out wide. Miss the moment and the moment is gone.",
            "Hessa's lesson: when a perfect guard catches a hit, press your form key with forward held. You close up to 2.4 blocks and cut. Spend 12 Aura. Your guard drops for it.",
        ),
        "practiced": "Ember Riposte: catch, close and cut completed.",
        "hint": "Catch a blow on a perfect guard and I can show you how to answer it. Sneak-use me with an empty hand.",
        "hud": ("Ember Riposte: set", "Ember Riposte: committed", "Ember Riposte: recovering"),
    },
    "gale_shove": {
        "name": "Gale Shove",
        "locked": "Learn at Form after a recorded Gale Master clear, from a wandering Gale teacher.",
        "controls": "After a perfect guard catches a hit, press %s within a moment while holding a strafe key: push the attacker away.",
        "limits": "Hits the attacker if still in reach, else the nearest foe ahead. A push and 3s of Weakness, no damage. No slot needed.",
        "cost": "10 Aura. 0.5s recovery.",
        "diagram": ("1 Catch", "2 Square up", "3 Push"),
        "book": (
            "On the river road the danger is not the first cut. It is the second man behind the first. Iven taught us to make room before we made anything else.",
            "Take the hit on your guard, then open your free hand and lean in with your whole weight. It is not a blow. It is a door closing. They stumble back, their grip loose, and you have space to think.",
            "Iven's lesson: when a perfect guard catches a hit, press your form key with a strafe key held. You push the attacker back and weaken them for 3 seconds. Spend 10 Aura.",
        ),
        "practiced": "Gale Shove: catch and push completed.",
        "hint": "I can teach you to make room after a block. Sneak-use me with an empty hand.",
        "hud": ("Gale Shove: set", "Gale Shove: committed", "Gale Shove: recovering"),
    },
    "stone_break": {
        "name": "Stone Break",
        "locked": "Learn at Form after a recorded Stone Master clear, from a wandering Stone teacher.",
        "controls": "After a perfect guard catches a hit, press %s within a moment with no move key: lift the blade and bring it down.",
        "limits": "The slowest answer: a long, plain wind-up. A heavy blow, 2s of Slowness, and it drops a player's raised guard. No slot needed.",
        "cost": "16 Aura. 0.9s recovery.",
        "diagram": ("1 Catch", "2 Lift high", "3 Bring down"),
        "book": (
            "Quarry wardens do not dance. Orla stood where she stood and let the trouble come to her. When it did, she caught it, and then she lifted her blade over her head like a pick.",
            "Everyone sees it coming. That is fine. A foe who just struck your guard is still recovering from it, and a raised guard of their own will not hold under the weight. Do not try it on someone fresh.",
            "Orla's lesson: when a perfect guard catches a hit, press your form key with no move key held. You lift, then strike hard and slow the foe. Spend 16 Aura.",
        ),
        "practiced": "Stone Break: catch, lift and strike completed.",
        "hint": "I can teach you what to do after you catch a blow. Sneak-use me with an empty hand.",
        "hud": ("Stone Break: lifting", "Stone Break: committed", "Stone Break: recovering"),
    },
    "air_step": {
        "name": "Air Step",
        "locked": "Learn at Form after a recorded Gale Master clear, from a Gale teacher, after Wall Turn and Reed Slip.",
        "controls": "In the air, press %s: a short lift where you look. Once per jump or fall.",
        "limits": "Under a block of lift. Ceilings and wards stop it. You still take the fall you owe. Lands, splashes or stalls.",
        "cost": "15 Aura. 3s shared rest; 0.4s recovery on land, 0.3s in water, 1.2s if stalled.",
        "diagram": ("1 Leave ground", "2 Step up", "3 Land"),
        "book": (
            "I never meant to teach this. A student slipped off the ferry rail and, without thinking, kicked off the wind itself. He landed on the deck. He also landed badly, and I will not pretend otherwise.",
            "You do not fly. You find one more step where there was none, once, and then you are falling again with everything you owed. Over water, you get wet. Into a wall, you get nowhere.",
            "Iven's lesson: in the air, press your form key. You rise a little where you look. Spend 15 Aura; rest 3 seconds. Only once per time off the ground.",
        ),
        "practiced": "Air Step: step and landing completed.",
        "hint": "I can teach you a step on the wind. Sneak-use me with an empty hand.",
        "hud": ("Air Step", "Air Step: rising", "Air Step: landing"),
        "ready": "%s in the air: Air Step · 15 Aura",
    },
    "plunge": {
        "name": "Plunging Strike",
        "locked": "Learn at Sovereign after a recorded Stone Master clear, from a Stone teacher, after Stone Break.",
        "controls": "In the air with 2 clear blocks below, press %s: hang for a beat, then drive straight down. Landing strikes around you.",
        "limits": "Hits foes within 2.5 blocks on solid ground. Water splashes it out; hazards or a long fall stall it. Fall damage still counts.",
        "cost": "20 Aura. 6s shared rest; 1s recovery on land, 1.2s if stalled.",
        "diagram": ("1 Hang", "2 Drive down", "3 Land hard"),
        "book": (
            "Orla once came down a quarry face on a rope that broke. She landed on a raider. She said it was the most useful thing that had happened all year, and she spent the next month learning to do it on purpose.",
            "The hang is the warning. For a breath you hold still in the air, blade high. Then you drop like a stone. Land on rock and the ground shakes. Land in water and you have just had a bath.",
            "Orla's lesson: in the air with room below, press your form key. You hang, dive, and strike all around where you land. Spend 20 Aura; rest 6 seconds.",
        ),
        "practiced": "Plunging Strike: dive and landing completed.",
        "hint": "Sovereigns may learn to fall with purpose. Sneak-use me with an empty hand.",
        "hud": ("Plunging Strike: hanging · Sneak to stop", "Plunging Strike: diving", "Plunging Strike: recovering"),
        "ready": "%s in the air: Plunging Strike · 20 Aura",
    },
    "spell_cut": {
        "name": "Spell Cut",
        "locked": "Learn at Form after a recorded Ember Master clear, from an Ember teacher, after Ember Riposte.",
        "controls": "Hold an Aura weapon and attack just as a hostile spell bolt reaches you: cut it from the air.",
        "limits": "Close and facing it: the bolt is cut. Too early or too wide: a miss that still costs. No slot needed.",
        "cost": "14 Aura and 0.3s recovery on a cut; 6 Aura and 1s recovery on a miss.",
        "diagram": ("1 Watch", "2 Wait", "3 Cut"),
        "book": (
            "Fire mages troubled the kiln more than raiders did. Hessa had no magic. She had a blade and very good timing, and she noticed that a bolt of fire is just a thing in the air, like anything else.",
            "Swing too early and you cut nothing but you are still off balance. Swing late and you are already burning. Wait until it is almost on you, face it squarely, and cut through the middle.",
            "Hessa's lesson: hold an Aura weapon and attack as a hostile spell bolt arrives. Close and square, it is cut for 14 Aura. Too soon or too wide, it is a miss for 6 Aura and a longer recovery.",
        ),
        "practiced": "Spell Cut: a spell cut from the air.",
        "hint": "I can teach you to cut a spell from the air. Sneak-use me with an empty hand.",
        "hud": ("Spell Cut", "Spell Cut", "Spell Cut: off balance"),
    },
}

LANG = {
    "screen.wildercord.field_forms.free": "Needs no slot: it works with any equipped form, or none.",
    "message.wildercord.field_form.air_only": "This form starts in the air.",
    "message.wildercord.field_form.air_blocked": "No room to step up.",
    "message.wildercord.field_form.plunge_blocked": "Not enough clear air below to dive.",
    "message.wildercord.field_form.follow_blocked": "No room to answer.",
    "hud.wildercord.follow.window": "Opening! %s to answer",
    "hud.wildercord.field_form.land": "Landed",
    "hud.wildercord.field_form.splash": "Splashed down",
    "hud.wildercord.field_form.sever": "Spell cut",
    "hud.wildercord.field_form.miss": "Missed the spell",
}
for _key, _f in _FORMS.items():
    _s = "screen.wildercord.field_forms." + _key
    LANG[_s] = _f["name"]
    LANG[_s + ".locked"] = _f["locked"]
    LANG[_s + ".learned"] = "Learned. " + ("Equip on safe ground, out of combat, after the shared rest." if "ready" in _f else "Always ready; no slot needed.")
    LANG[_s + ".controls"] = _f["controls"]
    LANG[_s + ".limits"] = _f["limits"]
    LANG[_s + ".cost"] = _f["cost"]
    for _i, _d in enumerate(_f["diagram"]):
        LANG[_s + ".diagram." + str(_i)] = _d
    LANG["screen.wildercord.field_forms.switch." + _key] = _f["name"] + " page"
    for _i, _page in enumerate(_f["book"]):
        LANG["book.wildercord." + _key + "." + str(_i + 1)] = _page
    LANG["message.wildercord.field_form.learned." + _key] = _f["name"] + " learned. Read it in the Cord screen > Aura > Master form > Field forms."
    LANG["message.wildercord.field_form.practiced." + _key] = _f["practiced"]
    LANG["message.wildercord.field_form.teacher_hint." + _key] = _f["hint"]
    LANG["message.wildercord.field_form.need_clear." + _key] = "Clear a Master's trial of my school first."
    _set, _travel, _recovery = _f["hud"]
    LANG["hud.wildercord." + _key + ".set"] = _set
    LANG["hud.wildercord." + _key + ".travel"] = _travel
    LANG["hud.wildercord." + _key + ".recovery"] = _recovery
    LANG["hud.wildercord." + _key + ".rest"] = _f["name"] + ": %2$s s rest"
    LANG["hud.wildercord." + _key + ".ready"] = _f.get("ready", _f["name"] + ": ready")
# The book and screen titles come from FormDashLessons.cover(); the screen pages read them from here too.
_TITLES = {
    "ember_riposte": ("The Answer After the Block", "Hessa Vane, kiln guard"),
    "spell_cut": ("Cutting the Thread", "Hessa Vane, kiln guard"),
    "gale_shove": ("Make Room", "Iven Reed, road tutor"),
    "air_step": ("A Step on Nothing", "Iven Reed, road tutor"),
    "stone_break": ("The Wall Leans Back", "Orla Cairn, quarry warden"),
    "plunge": ("Falling With Purpose", "Orla Cairn, quarry warden"),
}
for _key, (_title, _author) in _TITLES.items():
    LANG["screen.wildercord.field_forms." + _key + ".book_title"] = _title
    LANG["screen.wildercord.field_forms." + _key + ".author"] = _author


def write(g):
    """Six covers on the shared lesson book, in each school's colours, each with a glyph for its move."""
    from fungal_art import book_icon
    ember = ("#22120E", "#4A2218", "#8A3A22", "#6A2C1C", "#341A12")
    gale = ("#12201A", "#284634", "#4E7C58", "#3A6446", "#1C3426")
    stone = ("#1A1A1C", "#3A3A40", "#7A7A80", "#5A5A60", "#2A2A2E")
    lessons = {
        "ember_riposte": (ember, """
            .......
            .e.....
            .e..h..
            .e.hhh.
            .e..h..
            .e.....
            .......
        """, "#F0C8A0", "#F3A04A"),
        "spell_cut": (ember, """
            h......
            .h..e..
            ..he...
            ..eh...
            .e..h..
            .....h.
            .......
        """, "#F0C8A0", "#B8A0F0"),
        "gale_shove": (gale, """
            .......
            ...e...
            ..ee.h.
            .eee.hh
            ..ee.h.
            ...e...
            .......
        """, "#D0E0C4", "#EFD589"),
        "air_step": (gale, """
            ...h...
            ..hhh..
            ...h...
            .......
            .eeeee.
            .......
            .eeeee.
        """, "#D0E0C4", "#EFD589"),
        "stone_break": (stone, """
            ..hhh..
            ...h...
            ...h...
            ...h...
            .......
            .eeeee.
            eeeeeee
        """, "#D8D8DC", "#E8C46A"),
        "plunge": (stone, """
            ...h...
            ...h...
            ...h...
            .h.h.h.
            ..hhh..
            ...h...
            eeeeeee
        """, "#D8D8DC", "#E8C46A"),
    }
    for name, (cover, emblem, ink, accent) in lessons.items():
        g.save(book_icon(cover, emblem, ink, accent), g.ASSETS / f"textures/item/{name}_lesson.png")
        g.write_json(g.ASSETS / f"models/item/{name}_lesson.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"wildercord:item/{name}_lesson"}})
        g.write_json(g.ASSETS / f"items/{name}_lesson.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{name}_lesson"}})
