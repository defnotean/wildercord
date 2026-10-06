"""The Archive's original Relay lesson, retained as a book and a Grimoire reading."""

LANG = {
    "book.wildercord.relay_lesson.1": "The Archive's register says its last copyists stood inside the arena's circle. The Archivist copied every word. The margin survived because nobody called it a word.\n\nIlyra Venn\nThird Copyist",
    "book.wildercord.relay_lesson.2": "I was not inside the circle. I left my mark there and stepped between the shelves. The spell departed from the mark. When a fallen book hid it, the line closed. The register calls this cowardice. I call it a second place to stand.",
    "book.wildercord.relay_lesson.3": "Thread Relay + Harm, Frost or Shock. Cast sets a focus within 8 blocks. Release the key, move, aim; Cast again within 4s. Cost: 24 + effect, once; no refund. Rest: 8s. Keep both sightlines clear.\n\nCord > Grimoire > Relay: controls and practice.",
    "message.wildercord.relay_lesson.invitation": "The quiet Archive Lectern holds an Archmage's lesson: The Margin Between Places. Return after your Archivist victory to copy it, then read its three pages now or later in your Grimoire.",
    "message.wildercord.relay_lesson.quiet": "The Archive has not fallen quiet. Its lectern cannot be read while the Archivist is abroad.",
    "message.wildercord.relay_lesson.locked": "This margin answers an active Eighth Circle and the recorded Archivist victory, The Last Page.",
    "message.wildercord.relay_lesson.interrupted": "The reading closed. Your verified copy is safe; resume from the Grimoire with an active Eighth Circle.",
    "message.wildercord.relay_lesson.copied": "The Margin Between Places is copied into your Grimoire. Read its three pages here or later to learn Relay; copying alone does not teach it.",
    "message.wildercord.relay_lesson.uncopied": "First copy the lesson at a quiet, spent Archive Lectern with an active Eighth Circle and The Last Page.",
    "message.wildercord.relay_lesson.learned": "Relay Circle learned. The Margin Between Places is saved in your Grimoire, with controls and an optional Training Dummy exercise.",
    "message.wildercord.relay_lesson.retained": "Your inventory is full. The whole lesson is safe in your Grimoire; the lectern will keep your book copy until there is room.",
    "message.wildercord.relay_lesson.practiced": "Relay practice recorded: place, move, aim, release. Your Grimoire remembers the exercise.",
    "screen.wildercord.relay_lesson.heading": "Master studies",
    "screen.wildercord.relay_lesson.copied": "Relay Circle: copied lesson  [Study]",
    "screen.wildercord.relay_lesson.heart_copied": "Archive lesson copied. Finish its three pages in Grimoire > Master studies to learn Relay.",
    "screen.wildercord.relay_lesson.study_pending": "Copied, not yet learned: read all three pages",
    "screen.wildercord.relay_lesson.retrieve_copied": "Your verified Archive copy is saved. Study it here with active VIII and The Last Page; no return or second boss fight is needed.",
    "screen.wildercord.relay_lesson.opening": "Opening your saved lesson...",
    "screen.wildercord.relay_lesson.entry": "Relay Circle: The Margin Between Places  [Read]",
    "screen.wildercord.relay_lesson.unknown": "Relay Circle: an unread Archive margin",
    "screen.wildercord.relay_lesson.retrieve": "Read all three pages and current controls here. A lost physical book never removes the lesson. The quiet lectern also keeps one replacement copy.",
    "screen.wildercord.relay_lesson.heart_known": "Relay Circle learned. Read its story, current controls and optional exercise in the Grimoire's Master studies.",
    "screen.wildercord.relay_lesson.next": "Next page",
    "screen.wildercord.relay_lesson.learn": "Learn Relay",
    "screen.wildercord.relay_lesson.reading": "Copied lesson · %s / %s",
    "screen.wildercord.relay_lesson.saved": "Grimoire copy · %s / %s",
    "screen.wildercord.relay_lesson.learned": "Relay learned · %s / %s",
    "screen.wildercord.relay_lesson.scroll": "Scroll to read · %s / %s",
    "screen.wildercord.relay_lesson.controls": "Equip: open %s and thread Relay + one Harm, Frost or Shock in an active Echo Cord slot. Select it with %s. Tap %s at visible floor to place a focus; release the key, move and aim, then tap again within 4 seconds. Sneak + a fresh cast press cancels; changing slots or editing also cancels.",
    "screen.wildercord.relay_lesson.limits": "The full cost is paid on placement: 24 base mana + the effect, with normal discounts once. Release costs nothing extra. No refunds. One focus, 8-second shared rest, 6-tick lane warning, 10-tick recovery. Output is 90% of normal effect strength. Caster-to-focus plus focus-to-target may total at most 16 blocks; keep both paths clear. Active VIII and your original Echo Cord/slot remain required. No modifiers, links, Knots, woven effects, passives, scroll storage or copied casts.",
    "screen.wildercord.relay_lesson.practice": "Optional exercise: set a Training Dummy on open ground. Aim beside it and tap %s to place. Let go, move at least one block, aim at the dummy and tap %s again before four seconds pass. A real hit records the exercise. Normal mana and rest apply; no XP or extra reward is added. You may use Relay immediately without doing this exercise.",
    "screen.wildercord.relay_lesson.practiced": "Practice remembered: place → move → aim → release",
    "screen.wildercord.relay_lesson.practice_pending": "Optional: hit a Training Dummy after placing and moving",
}


def write(generator):
    """Generate an individual book model without introducing a lootable teaching item."""
    from fungal_art import book_icon

    cover = ("#111927", "#27394B", "#3B5B6B", "#263D50", "#172839")
    emblem = """
        .ee....
        e..e...
        e..eh..
        .ee..h.
        ...he.e
        ...e..e
        ....ee.
    """
    generator.save(book_icon(cover, emblem, "#E8C46A", "#A3ECE3"),
                   generator.ASSETS / "textures/item/relay_lesson.png")
    generator.write_json(generator.ASSETS / "models/item/relay_lesson.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "wildercord:item/relay_lesson"},
    })
    generator.write_json(generator.ASSETS / "items/relay_lesson.json", {
        "model": {"type": "minecraft:model", "model": "wildercord:item/relay_lesson"},
    })
