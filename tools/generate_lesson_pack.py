"""The first authored lesson pack: Tollgate (X), Lifeline (XIV) and Conduit (XVIII), each studied from a boss feat."""
LESSONS = {
    "tollgate": dict(name="Tollgate", title="The Warden's Threshold", feat="Tempered", boss="the Cinder Warden", numeral="X", word="Tenth"),
    "lifeline": dict(name="Lifeline", title="The Thread Between Stars", feat="Starbreaker", boss="the Star Eater", numeral="XIV", word="Fourteenth"),
    "conduit": dict(name="Conduit", title="Notes on a Grounded Storm", feat="Grounded", boss="the Storm Conductor", numeral="XVIII", word="Eighteenth"),
}
PAGES = {
    "tollgate": (
        "The Warden's Threshold\n\nThe Warden never chased. It drew one line of heat and let us choose to cross. Most of us chose badly, once.",
        "A threshold is not a wall. Draw a low gate on the ground. The first time each foe crosses, it is halted and thrown back to the side it came from. Three tolls and the gate is spent.",
        "Thread Wall + Tollgate. Look at open ground within ten blocks and press Cast. Thirty base mana; ten seconds' rest. A foe may jump it, go around it, or wait it out. Allies pass freely.",
    ),
    "lifeline": (
        "The Thread Between Stars\n\nThe Eater pulled stars apart. We learned to hold them together: one thread, one friend, one pull.",
        "Press Cast at an ally in sight to tie a thread. Press again to reel them to safe ground beside you. A friend who crouches holds fast and refuses the pull.",
        "Thread Beam + Lifeline. Twenty-eight base mana for the thread; the reel is free. It snaps past twenty-four blocks or after eight seconds. Ten seconds' rest.",
    ),
    "conduit": (
        "Notes on a Grounded Storm\n\nThe Conductor fell when we learned to ground its rods. Rods work for us too, if nothing stands on them.",
        "Press Cast at open ground within twenty blocks to plant a rod. Press again to spark for a moment, then arrive on it. A blow during the spark breaks it.",
        "Thread Pillar + Conduit. Thirty-two base mana per rod; arriving is free. A foe beside the rod grounds it. Keep it in sight and within thirty-two blocks. Fifteen seconds' rest.",
    ),
}
BEATS = {
    "tollgate": "Counterplay: jump the gate, go around its five blocks, or wait out six seconds. Each foe is tolled once; allies, pets and party members pass. Claims and dungeon wards refuse the ground.",
    "lifeline": "Counterplay: the ally crouches to refuse. The reel needs sight, safe ground beside you and ground you may change. Distance, death, a world change or leaving the party snaps it.",
    "conduit": "Counterplay: stand within a block and a half of the rod to ground it, or hit the caster during the spark. Arrival needs sight, a safe landing and the same dungeon ward.",
}
PRACTICE = {
    "tollgate": "Practice: set a gate between you and a foe and let it cross. Press %s on the ground; the first toll records this exercise.",
    "lifeline": "Practice: tie a thread to a party member or your pet, then press %s again to reel them in. The first reel records this exercise.",
    "conduit": "Practice: plant a rod on a ledge, then press %s again and hold still. The first arrival records this exercise.",
}
LANG = {
    "screen.wildercord.lesson_pack.controls": "Open %s and thread only the lesson's two runes in an ordinary Echo Cord row. Select it with %s. %s answers fresh presses of %s. %s + %s lifts what you placed.",
    "screen.wildercord.lesson_pack.retrieve": "Read this study and its current controls. Your Grimoire keeps it forever.",
    "screen.wildercord.lesson_pack.retrieve_copied": "Your saved boss victory recovers this study. Read its three pages with the active Circle it names.",
    "screen.wildercord.lesson_pack.study_pending": "Read three pages with the named Circle active",
    "screen.wildercord.lesson_pack.opening": "Opening your saved study...",
    "screen.wildercord.lesson_pack.next": "Next page",
    "screen.wildercord.lesson_pack.learn": "Learn %s",
    "screen.wildercord.lesson_pack.reading": "Studying %s · %s / %s",
    "screen.wildercord.lesson_pack.saved": "Saved %s study · %s / %s",
    "screen.wildercord.lesson_pack.learned": "%s learned · %s / %s",
    "screen.wildercord.lesson_pack.scroll": "Scroll to read · %s / %s",
    "screen.wildercord.lesson_pack.practiced": "Practice remembered",
    "message.wildercord.lifeline_lesson.offered": "%s threaded a Lifeline to you. Crouch now to refuse the pull.",
    "message.wildercord.lifeline_lesson.refused": "You refused the Lifeline.",
}
for _path, _l in LESSONS.items():
    _key = _path + "_lesson"
    LANG.update({
        f"rune.wildercord.{_path}.found": f"Found: {_l['title']}, retrievable in Grimoire after {_l['feat']}; study with active Circle {_l['numeral']}",
        f"book.wildercord.{_key}.1": PAGES[_path][0],
        f"book.wildercord.{_key}.2": PAGES[_path][1],
        f"book.wildercord.{_key}.3": PAGES[_path][2],
        f"message.wildercord.{_key}.locked": f"Studying {_l['name']} needs active Circle {_l['numeral']} and {_l['feat']}.",
        f"message.wildercord.{_key}.uncopied": f"{_l['feat']}, earned against {_l['boss']}, unlocks this study.",
        f"message.wildercord.{_key}.interrupted": f"The reading closed. {_l['title']} stays in your Grimoire.",
        f"message.wildercord.{_key}.learned": f"{_l['name']} learned. Its study stays in Grimoire > Master studies.",
        f"message.wildercord.{_key}.practiced": f"First {_l['name']} done. Your Grimoire remembers the exercise.",
        f"screen.wildercord.{_key}.entry": f"{_l['name']}: {_l['title']} [Read]",
        f"screen.wildercord.{_key}.copied": f"{_l['name']}: {_l['title']} [Study]",
        f"screen.wildercord.{_key}.unknown": f"{_l['name']}: an unread study",
        f"screen.wildercord.{_key}.invitation": f"Earn {_l['feat']} against {_l['boss']}, then study with an active {_l['word']} Circle.",
        f"screen.wildercord.{_key}.practice_pending": "Practice: not yet done",
        f"screen.wildercord.{_key}.practiced": "Practice: done",
        f"screen.wildercord.{_key}.beats": BEATS[_path],
        f"screen.wildercord.{_key}.practice": PRACTICE[_path],
    })


def circles(bands, marks):
    # Authored rings and emblems: a broken threshold bar, one tied strand, a grounded rod. Never hash-assigned.
    bands["tollgate"] = {(x, 12) for x in range(16) if x not in (7, 8)} | {(2, y) for y in range(8, 12)} | {(13, y) for y in range(8, 12)} | {(1, 3), (14, 3)}
    marks["tollgate"] = {(x, 10) for x in range(4, 12)} | {(4, y) for y in range(5, 10)} | {(11, y) for y in range(5, 10)} | {(7, 4), (8, 4), (6, 3), (9, 3)}
    bands["lifeline"] = {(x, x) for x in range(2, 14) if x not in (7, 8)} | {(1, 2), (2, 1), (13, 14), (14, 13), (0, 15), (15, 0)}
    marks["lifeline"] = {(3, 3), (4, 3), (3, 4), (12, 12), (11, 12), (12, 11)} | {(x, 8) for x in range(5, 11)} | {(8, y) for y in range(5, 8)} | {(6, 6), (10, 10)}
    bands["conduit"] = {(7, y) for y in range(1, 15) if y not in (5, 6)} | {(x, 15) for x in range(3, 13)} | {(4, 14), (11, 14), (0, 0), (15, 1)}
    marks["conduit"] = {(7, 2), (8, 3), (7, 4), (8, 5), (7, 6), (8, 7), (9, 8)} | {(x, 13) for x in range(5, 11)} | {(5, 12), (10, 12), (3, 9), (12, 9)}
