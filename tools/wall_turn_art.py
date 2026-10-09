"""Authored Wall Turn lesson cover, form icon and complete player-facing text."""

LANG = {
    "key.category.wildercord.master_forms": "Wildercord: Master forms",
    "key.wildercord.master_form": "Equipped Master form",
    "screen.wildercord.master_forms.title": "Master form",
    "screen.wildercord.master_forms.open": "Open the one Master-form slot and retained lessons. Current form key: %s",
    "screen.wildercord.master_forms.wall_turn": "Wall Turn",
    "screen.wildercord.master_forms.slot": "One Master-form slot. Written techniques keep their own slots.",
    "screen.wildercord.master_forms.locked": "Learn at Sovereign after a recorded Gale Master clear. Sneak-use a wandering teacher with an empty hand. Old clears count; any breathing method works.",
    "screen.wildercord.master_forms.learned": "Learned permanently from a wandering teacher. Equip while grounded and out of combat, after the shared rest.",
    "screen.wildercord.master_forms.controls": "Beside a wall in the air: press %s to brace. Let go, then press again while moving to kick away. Sneak drops; menus cancel.",
    "screen.wildercord.master_forms.limits": "Half-second brace; one kick up to 4 blocks across and 1.25 up. Land safely before another. Hits interrupt; falling still hurts.",
    "screen.wildercord.master_forms.cost": "20 Aura, even awakened. 6s shared rest; 0.5s landing recovery.",
    "screen.wildercord.master_forms.equip": "Equip Wall Turn",
    "screen.wildercord.master_forms.unequip": "Empty Master-form slot",
    "screen.wildercord.master_forms.book_title": "The Scout's Unfinished Step",
    "screen.wildercord.master_forms.author": "Iven Reed, road tutor",
    "screen.wildercord.master_forms.previous": "Previous",
    "screen.wildercord.master_forms.next": "Next",
    "screen.wildercord.master_forms.back": "Back",
    "screen.wildercord.master_forms.source": "Find a teacher",
    "screen.wildercord.master_forms.overview": "Overview",
    "screen.wildercord.master_forms.source_help": "Reach Sovereign and record a legitimate Gale Master clear; old clears count. Sneak-use a wandering teacher with an empty main hand, read the illustrated lesson and accept it. Any breathing method works. Your story remains here and in the Grimoire, even with a full inventory.\n\nOrdinary admitted swings and spells end an active brace or kick. Aura arts wait through landing plus 0.5s recovery. If landing is impossible, art commitment ends after at most 6s plus 0.5s recovery; Wall Turn itself still needs a true safe landing. Rebind the form in Options > Controls > Key Binds > Wildercord: Master forms.",
    "screen.wildercord.master_forms.accept": "Accept the Wall Turn lesson",
    "screen.wildercord.master_forms.page": "Page %s / %s",
    "screen.wildercord.master_forms.practice": "Practice: jump by a safe wall. %s, release, press, land. Costs real Aura; no XP.",
    "screen.wildercord.master_forms.diagram.0": "1 Brace",
    "screen.wildercord.master_forms.diagram.1": "2 Kick away",
    "screen.wildercord.master_forms.diagram.2": "3 True landing",
    "screen.wildercord.master_forms.readback": "Read all three pages here on hover, or open Aura > Master form for the illustrated lesson and current controls.",
    "screen.wildercord.master_forms.chapter": "Retained lesson, page %s",
    "book.wildercord.wall_turn.1": "The ledger says the scout outran pursuit. The Returned Step memorial says she turned back for the last marcher. Neither mentions the watchtower wall. She planted one hand on cold stone and changed which way the road went. I copied the scuff; they copied her footprints.",
    "book.wildercord.wall_turn.2": "'I hung there too long. Their arrow found my cloak. The landing hurt.' The note was sewn inside her boot. Later Marchkeepers called it certainty. I teach her hesitation too: one wall, one turn, one true landing. Seek the Returned Step memorial for the other account.",
    "book.wildercord.wall_turn.3": "Iven's charcoal lesson: brace beside a visible wall in the air, release your form key, then press again to kick away. The pause is exposed. Land safely before another turn. Spend 20 Aura; rest 6 seconds. Stone, Anchor and old falls still stop or hurt you.",
    "message.wildercord.wall_turn.teacher_hint": "I can teach a Sovereign with a recorded Gale clear the Wall Turn. Sneak-use me with an empty hand for the story and lesson.",
    "message.wildercord.wall_turn.learned": "Wall Turn learned. Open the Cord screen > Aura > Master form to equip, read the retained story and see your current binding. Your method is unchanged.",
    "message.wildercord.wall_turn.practiced": "Wall Turn: brace, kick and safe landing completed. The lesson remains in your Grimoire and Master-form page.",
    "message.wildercord.wall_turn.locked": "Wall Turn waits on Sovereign, a recorded Gale clear and the wandering teacher's lesson.",
    "message.wildercord.wall_turn.unavailable": "Wall Turn needs an equipped form, held Aura weapon and free movement. Flight, control effects and other commitments prevent it.",
    "message.wildercord.wall_turn.equip_wait": "Equip on safe ground, out of combat, after your form's shared rest and landing recovery.",
    "message.wildercord.wall_turn.landing": "Land on safe, solid ground before another Wall Turn.",
    "message.wildercord.wall_turn.rest": "The Master-form slot is still resting.",
    "message.wildercord.wall_turn.aura": "Wall Turn requires 20 Aura, including while awakened.",
    "message.wildercord.wall_turn.wall": "Jump beside a visible solid wall, with room for your whole body.",
    "hud.wildercord.wall_turn.locked": "Wall Turn locked",
    "hud.wildercord.wall_turn.unequipped": "Master-form slot empty",
    "hud.wildercord.wall_turn.brace": "Braced: release, then press %s to kick · Sneak to drop",
    "hud.wildercord.wall_turn.kick": "Wall Turn: kick committed",
    "hud.wildercord.wall_turn.recovery": "Wall Turn: landing recovery",
    "hud.wildercord.wall_turn.landing": "Wall Turn: land safely before form reuse",
    "hud.wildercord.wall_turn.descent": "Wall Turn: land for Aura arts · %3$s s maximum",
    "hud.wildercord.wall_turn.rest": "Wall Turn: %2$s s rest",
    "hud.wildercord.wall_turn.ready": "%s: Wall Turn · 20 Aura",
}


def write(g):
    """The lesson on the shared lesson-book cover, as Relay, Reweave and Excise: Gale's sky-teal, a wall and the kick's arc."""
    from fungal_art import book_icon
    cover = ("#0F1E22", "#1F4247", "#3E7A72", "#2B5A57", "#173336")
    emblem = """
        e...hh.
        e..h..h
        e.h...h
        eh.....
        eh.....
        e......
        eeeeeee
    """
    g.save(book_icon(cover, emblem, "#C9D8C6", "#EFD589"), g.ASSETS / "textures/item/wall_turn_lesson.png")
    g.write_json(g.ASSETS / "models/item/wall_turn_lesson.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "wildercord:item/wall_turn_lesson"}})
    g.write_json(g.ASSETS / "items/wall_turn_lesson.json", {"model": {"type": "minecraft:model", "model": "wildercord:item/wall_turn_lesson"}})
