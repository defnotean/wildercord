"""The Tide Scribe's Ebb Ledger: permanent Low Tide recovery and optional bound copies."""

LANG = {
    "book.wildercord.reweave_lesson.1": "I kept the tide's accounts. Four debts, entered before the first was paid. You broke my flood and thought the ledger empty. Look again: an unwritten line is still owed.\n\nThe Tide Scribe\nAt the ebb",
    "book.wildercord.reweave_lesson.2": "A wall of water obeys the current. This account obeys its first payment. Draw Harm in a circle; then pull its unpaid marks into one narrow line. Keep the place and the hour. Change only the shape. What the warning swallows is spent.",
    "book.wildercord.reweave_lesson.3": "Thread Reweave + Harm. Pay 40 total base mana once. Rest: 160 ticks. Radius 2; beats 8, 28, 48, 68; ends at 80. Release, aim, cast again: one 7 x 1.25 lane, warned for 8 ticks. Lost beats stay lost.\n\nControls: Cord > Grimoire.",
    "message.wildercord.reweave_lesson.invitation": "Low Tide preserves The Ebb Ledger. With an active Twelfth Circle, open Cord > Grimoire > Master studies and read its three pages to learn Reweave. No return to the Scriptorium is needed.",
    "message.wildercord.reweave_lesson.quiet": "Only a Tide altar whose Scribe has fallen can offer a bound Ebb Ledger. Low Tide already keeps the lesson in your Grimoire.",
    "message.wildercord.reweave_lesson.locked": "Studying Reweave requires an active Twelfth Circle and your recorded Tide Scribe victory, Low Tide.",
    "message.wildercord.reweave_lesson.interrupted": "The reading closed. The Ebb Ledger is safe in your Grimoire; begin its three pages again with an active Twelfth Circle and Low Tide.",
    "message.wildercord.reweave_lesson.copied": "The Ebb Ledger is saved in your Grimoire. Read its three pages to learn Reweave; the bound book is optional.",
    "message.wildercord.reweave_lesson.uncopied": "Earn Low Tide by defeating the Tide Scribe. That permanent victory recovers The Ebb Ledger here, even if the old altar is gone.",
    "message.wildercord.reweave_lesson.learned": "Reweave learned. The Ebb Ledger and current controls are saved in your Grimoire.",
    "message.wildercord.reweave_lesson.practiced": "Reweave practice recorded: place the disc, rewrite the lane, reach beyond the old circle. Your Grimoire remembers the exercise.",
    "message.wildercord.reweave_lesson.retained": "Your inventory is full. The Ebb Ledger stays readable in your Grimoire, and the quiet Tide altar keeps your book entitlement until there is room.",
    "screen.wildercord.reweave_lesson.heading": "Master studies",
    "screen.wildercord.reweave_lesson.entry": "Reweave: The Ebb Ledger  [Read]",
    "screen.wildercord.reweave_lesson.copied": "Reweave: The Ebb Ledger  [Study]",
    "screen.wildercord.reweave_lesson.unknown": "Reweave: the Tide Scribe's unread ledger",
    "screen.wildercord.reweave_lesson.study_pending": "Read all three pages with active XII and Low Tide",
    "screen.wildercord.reweave_lesson.retrieve": "Read the saved pages and current controls here. A quiet Tide altar can give one original bound book and one replacement; losing either never removes this lesson.",
    "screen.wildercord.reweave_lesson.retrieve_copied": "Your permanent Low Tide victory recovers this lesson here. Read its three pages with active XII; no book, intact altar or second boss fight is needed.",
    "screen.wildercord.reweave_lesson.heart_known": "Reweave learned. The Ebb Ledger and current controls are in Grimoire > Master studies. Casting still requires active XII and Low Tide.",
    "screen.wildercord.reweave_lesson.heart_copied": "Low Tide preserves The Ebb Ledger. Read its three pages in Grimoire > Master studies with active XII to learn Reweave.",
    "screen.wildercord.reweave_lesson.opening": "Opening your saved ledger...",
    "screen.wildercord.reweave_lesson.next": "Next page",
    "screen.wildercord.reweave_lesson.learn": "Learn Reweave",
    "screen.wildercord.reweave_lesson.reading": "Studying the ledger · %s / %s",
    "screen.wildercord.reweave_lesson.saved": "Grimoire ledger · %s / %s",
    "screen.wildercord.reweave_lesson.learned": "Reweave learned · %s / %s",
    "screen.wildercord.reweave_lesson.scroll": "Scroll to read · %s / %s",
    "screen.wildercord.reweave_lesson.controls": "Open %s and thread only Reweave + Harm in an active Echo Cord slot. Select it with %s. Tap %s at visible floor within 8 blocks to place the disc. Release the key, aim along the ground and tap again to rewrite it once into a lane from the same center. Hold %s and make a fresh %s press to cancel. Switching slots or editing also cancels.",
    "screen.wildercord.reweave_lesson.limits": "Pay 40 total base mana on placement: 32 for Reweave + 8 for Harm, with normal discounts once. Rewriting costs nothing and grants no extra cast rewards. One field; 160-tick (8-second) shared rest. Keep active XII, Low Tide, your learned study and the original Echo Cord and slot. Clear sightlines, loaded ground and the same ward remain required. No modifiers, links, Knots, woven effects, passives, scroll storage or copied casts. No refunds.",
    "screen.wildercord.reweave_lesson.beats": "The first shape is a radius-2 ground disc. Its four Harm beats are fixed at ticks 8, 28, 48 and 68, each at half strength. Everything ends at tick 80 (4 seconds after placement). Rewriting changes that same field into one 7-block-long, 1.25-block-wide lane with an 8-tick warning. Beats lost during the warning, obstruction or a late tick are spent forever. The clock, remaining beats and paid damage budget never restart. A rewrite too late to leave a beat after the warning is refused.",
    "screen.wildercord.reweave_lesson.practice": "Optional exercise: put a Training Dummy on clear ground more than 2 and less than 7 blocks from your planned center. Tap %s at that center, release, aim toward the dummy and tap %s again early enough for a beat to remain after the warning. A real lane hit outside the old disc records the exercise. Normal mana and rest apply; no XP or extra reward is added. Practice is never required to use Reweave.",
    "screen.wildercord.reweave_lesson.practiced": "Practice remembered: disc → rewrite → reach beyond the circle",
    "screen.wildercord.reweave_lesson.practice_pending": "Optional: land a rewritten lane on a Dummy beyond the old disc",
}


def write(generator):
    """A distinct drowned ledger cover; no recipe or loot table can grant the study."""
    from fungal_art import book_icon

    cover = ("#101E24", "#173B42", "#28616A", "#204850", "#102D35")
    emblem = """
        .eeeee.
        e..h..e
        e.hhh.e
        e..h..e
        .eehee.
        ...h...
        .eeeee.
    """
    generator.save(book_icon(cover, emblem, "#75D0C1", "#DFD4A5"),
                   generator.ASSETS / "textures/item/reweave_lesson.png")
    generator.write_json(generator.ASSETS / "models/item/reweave_lesson.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "wildercord:item/reweave_lesson"},
    })
    generator.write_json(generator.ASSETS / "items/reweave_lesson.json", {
        "model": {"type": "minecraft:model", "model": "wildercord:item/reweave_lesson"},
    })
