"""Rootbound's one authored study, and a severed living-knot icon."""
LANG = {
    "rune.wildercord.excise.found": "Found: The Root That Outlived Its Gardener, retrievable in Grimoire after Heartwood; study with active Circle XVI",
    "book.wildercord.excise_lesson.1": "The Root That Outlived Its Gardener\n\nThe Guardian fell, yet one root kept drinking. Its binding had a knot of its own. I cut leaves until winter before I learned to look where the sap still turned.\n\nThe Rootbound Gardener",
    "book.wildercord.excise_lesson.2": "You pruned the Guardian's bindings to earn Heartwood. Carry that patience into a living field. Find its small turning knot; keep your sight on it while the thread tightens. Cut there, and tomorrow's pulses cease. Yesterday's venom remains in the blood. A neighboring root keeps its own life.",
    "book.wildercord.excise_lesson.3": "Thread Beam + Excise. With an active Sixteenth Circle, hold Cast on a hostile Zone knot within twelve blocks for sixteen ticks. Pay thirty-six base mana once. Both a clean cut and a broken thread demand recovery. A cut keeps no claim on another root.\n\nYour Grimoire keeps this lesson when the garden is gone.",
    "message.wildercord.excise_lesson.invitation": "Heartwood preserves Rootbound study. With an active Sixteenth Circle, read The Root That Outlived Its Gardener in Grimoire > Master studies.",
    "message.wildercord.excise_lesson.locked": "Studying Excise requires active Circle XVI and your saved Heartwood victory.",
    "message.wildercord.excise_lesson.interrupted": "The reading closed. Rootbound remains in your Grimoire; begin its three pages again with active XVI and Heartwood.",
    "message.wildercord.excise_lesson.copied": "Rootbound is saved in your Grimoire. The bound book is optional; read all three pages here to learn Excise.",
    "message.wildercord.excise_lesson.uncopied": "Heartwood, earned by pruning and defeating the Root Guardian, recovers this lesson even if the old garden is gone.",
    "message.wildercord.excise_lesson.learned": "Excise learned. Rootbound and its current controls remain in Grimoire > Master studies.",
    "message.wildercord.excise_lesson.practiced": "Your first living field knot is cut. Your Grimoire remembers the exercise.",
    "message.wildercord.excise_lesson.retained": "Your inventory is full. Rootbound stays readable here; reopen it when there is room for the optional book.",
    "screen.wildercord.excise_lesson.heading": "Master studies",
    "screen.wildercord.excise_lesson.entry": "Excise: Rootbound  [Read]",
    "screen.wildercord.excise_lesson.copied": "Excise: Rootbound  [Study]",
    "screen.wildercord.excise_lesson.unknown": "Excise: the Gardener's unread Rootbound study",
    "screen.wildercord.excise_lesson.study_pending": "Read three pages with active XVI and Heartwood",
    "screen.wildercord.excise_lesson.retrieve": "Read The Root That Outlived Its Gardener and current controls. Your Grimoire offers one original book and one replacement; losing either never removes the lesson.",
    "screen.wildercord.excise_lesson.retrieve_copied": "Your saved Heartwood victory recovers Rootbound here. Study with active XVI; no book, intact altar or second Guardian kill is needed.",
    "screen.wildercord.excise_lesson.opening": "Opening your saved Rootbound study...",
    "screen.wildercord.excise_lesson.next": "Next page",
    "screen.wildercord.excise_lesson.learn": "Learn Excise",
    "screen.wildercord.excise_lesson.reading": "Studying Rootbound · %s / %s",
    "screen.wildercord.excise_lesson.saved": "Saved Rootbound study · %s / %s",
    "screen.wildercord.excise_lesson.learned": "Excise learned · %s / %s",
    "screen.wildercord.excise_lesson.scroll": "Scroll to read · %s / %s",
    "screen.wildercord.excise_lesson.controls": "Open %s and thread only Beam + Excise in an ordinary Echo Cord row. Select it with %s. Hold %s on one visible hostile Zone knot within twelve blocks. Release to cancel. %s + %s also cancels. The same rebound Cast control appears in the live progress bar.",
    "screen.wildercord.excise_lesson.limits": "One payment: 36 base mana with normal discounts once. Sixteen-tick commitment; twelve-tick recovery on success or cancellation; 240-tick shared rest. No overcast, refund, second payment, retarget, links, modifiers, Knots, woven effects, passive, scroll, imbued or copied use. Retain your original Cord, row, active XVI and Heartwood.",
    "screen.wildercord.excise_lesson.beats": "Damage, broken sight, moving more than one block, opening a menu, losing focus or changing body/world cancels the paid cut. Your target must still be hostile and in the same resolved ward. Only harmful native Zones without anchored links show eligible knots. Cutting one stops its future pulses; Split siblings, applied venom/fire and earlier secondary magic keep their original lives.",
    "screen.wildercord.excise_lesson.practice": "Find a hostile caster's Zone, keep outside its reach if possible, and aim at its small living knot. Hold %s until the thread closes; after recovery, release %s before another try. Your first successful cut records this optional exercise. Each real cut earns bounded utility mastery; practice-room caps and repetition limits apply.",
    "screen.wildercord.excise_lesson.practiced": "Practice remembered: one living knot, one deliberate cut",
}


def circles(bands, marks):
    # A interrupted strand and two pruned root forks: explicitly authored, never assigned by a hash.
    bands["excise"] = {(x, 7) for x in range(16) if x not in (6, 7, 8)} | {(x, 9) for x in range(16) if x not in (6, 7, 8)} | {(5, 6), (5, 10), (9, 6), (9, 10), (2, 5), (13, 11)}
    marks["excise"] = {(7, y) for y in range(2, 14) if y not in (7, 8)} | {(x, y) for x, y in [(3, 4), (4, 5), (5, 6), (6, 6), (8, 6), (9, 5), (10, 4), (11, 3), (3, 12), (4, 11), (5, 10), (6, 9), (8, 9), (9, 10), (10, 11), (11, 12), (10, 7), (11, 7), (12, 7)]}


def write(generator):
    from fungal_art import book_icon
    cover = ("#17231A", "#314A2D", "#55713E", "#3A5731", "#1F3020")
    emblem = """
        e..h..e
        .e.h.e.
        ..ehe..
        .....hh
        ..ehe..
        .e.h.e.
        e..h..e
    """
    generator.save(book_icon(cover, emblem, "#C3DC8D", "#E4D2A1"), generator.ASSETS / "textures/item/excise_lesson.png")
    generator.write_json(generator.ASSETS / "models/item/excise_lesson.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "wildercord:item/excise_lesson"}})
    generator.write_json(generator.ASSETS / "items/excise_lesson.json", {"model": {"type": "minecraft:model", "model": "wildercord:item/excise_lesson"}})
