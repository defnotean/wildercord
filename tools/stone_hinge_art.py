"""Authored Stone Hinge lesson cover and complete player-facing text."""

LANG = {
    "screen.wildercord.master_forms.stone_hinge": "Stone Hinge",
    "screen.wildercord.master_forms.switch": "Show %s",
    "screen.wildercord.master_forms.stone_hinge.locked": "Learn at Sovereign after a recorded Stone Master clear. Sneak-use a wandering Stone teacher with an empty hand. Old clears count; any breathing method works.",
    "screen.wildercord.master_forms.stone_hinge.testing": "Locked: in testing. Stone Hinge is still in testing; an operator can enable it (aura.experimental_stone_hinge). A form already learned stays learned.",
    "screen.wildercord.master_forms.stone_hinge.learned": "Learned permanently from a wandering teacher. It shares the one Master-form slot with Wall Turn. Equip on safe ground, out of combat.",
    "screen.wildercord.master_forms.stone_hinge.controls": "On the ground, hold one strafe key and press %s to plant. A melee blow from in front of where you planted (within about 60 degrees) is turned toward that side; turning the camera afterwards does not change that.",
    "screen.wildercord.master_forms.stone_hinge.limits": "0.3s plant, then a 0.6s catch. You barely move while planted. Damage still lands; only the shove turns. Arrows, blasts and blows from the side or behind push as usual, and so do mobs with extra knockback (attribute or enchantment) or attacks that skip the normal melee hit.",
    "screen.wildercord.master_forms.stone_hinge.cost": "20 Aura, even awakened. 6s shared rest from the plant; 0.5s recovery after.",
    "screen.wildercord.master_forms.stone_hinge.equip": "Equip Stone Hinge",
    "screen.wildercord.master_forms.stone_hinge.accept": "Accept the Stone Hinge lesson",
    "screen.wildercord.master_forms.stone_hinge.book_title": "The Gatepost Ledger",
    "screen.wildercord.master_forms.stone_hinge.author": "Maren Holt, quarry warden",
    "screen.wildercord.master_forms.stone_hinge.source_help": "Reach Sovereign and record a legitimate Stone Master clear; old clears count. Sneak-use a wandering Stone teacher with an empty main hand, read the lesson and accept it. Your copy stays here and in the Grimoire, even with a full inventory.\n\nThe plant fails on open drops, hazards, ward edges and the world border, before Aura is paid. If the side lane closes during the catch, the blow pushes you normally. Moving off the plant, jumping or a menu ends it with no refund.",
    "screen.wildercord.master_forms.stone_hinge.practice": "Practice: stand on flat ground, hold a strafe key, press %s, and take a blow from the front. Costs real Aura; no XP.",
    "screen.wildercord.master_forms.stone_hinge.diagram.0": "1 Plant",
    "screen.wildercord.master_forms.stone_hinge.diagram.1": "2 Catch",
    "screen.wildercord.master_forms.stone_hinge.diagram.2": "3 Turn aside",
    "book.wildercord.stone_hinge.1": "The quarry gate had one post left when the slide came. Maren put her back to it and let the rubble push her sideways instead of down. The ledger lists the post as repaired. It does not list her.",
    "book.wildercord.stone_hinge.2": "'A gate does not stop the wind. It swings.' She taught the cutters to stand like the post: feet set, weight low, one side open. The force still arrives. You only choose where it goes.",
    "book.wildercord.stone_hinge.3": "Maren's lesson: on firm ground, hold left or right and press your form key to plant. Take the blow from the front while braced and it turns toward the side you held. Spend 20 Aura; rest 6 seconds. The hit still hurts.",
    "message.wildercord.stone_hinge.teacher_hint": "I can teach a Sovereign with a recorded Stone clear the Stone Hinge. Sneak-use me with an empty hand for the story and lesson.",
    "message.wildercord.stone_hinge.need_stone": "Stone Hinge requires proving yourself against the Stone Master first.",
    "message.wildercord.stone_hinge.learned": "Stone Hinge learned. Open the Cord screen > Aura > Master form to equip it and read the retained story. Your method is unchanged.",
    "message.wildercord.stone_hinge.locked": "Stone Hinge waits on Sovereign, a recorded Stone clear and the wandering teacher's lesson.",
    "message.wildercord.stone_hinge.unavailable": "Stone Hinge needs the equipped form, a held Aura weapon and free movement.",
    "message.wildercord.stone_hinge.rest": "The Master-form slot is still resting.",
    "message.wildercord.stone_hinge.aura": "Stone Hinge requires 20 Aura, including while awakened.",
    "message.wildercord.stone_hinge.ground": "Stand still on safe, solid ground to plant. Sneaking does not count.",
    "message.wildercord.stone_hinge.strafe": "Hold exactly one strafe key to choose the side.",
    "message.wildercord.stone_hinge.lane": "That side is unsafe: a drop, hazard, ward edge or the world border.",
    "message.wildercord.stone_hinge.testing": "Stone Hinge is still in testing; an operator can enable it.",
    "hud.wildercord.stone_hinge.brace": "Stone Hinge: planting",
    "hud.wildercord.stone_hinge.catch": "Stone Hinge: catching a frontal blow",
    "hud.wildercord.stone_hinge.recovery": "Stone Hinge: recovery",
    "hud.wildercord.stone_hinge.rest": "Stone Hinge: %2$s s rest",
    "hud.wildercord.stone_hinge.testing": "Stone Hinge: in testing",
    "hud.wildercord.stone_hinge.ready": "%s + strafe: Stone Hinge · 20 Aura",
}


def write(g):
    """The lesson on the shared lesson-book cover: Stone's quarry umber, a gatepost and the turned path."""
    from fungal_art import book_icon
    cover = ("#1E1812", "#3F3326", "#7A6446", "#5A4A35", "#2E2519")
    emblem = """
        .e.....
        .e.hhh.
        .e....h
        .e....h
        .e...h.
        .e.....
        eeeeeee
    """
    g.save(book_icon(cover, emblem, "#D8CCB4", "#E8C46A"), g.ASSETS / "textures/item/stone_hinge_lesson.png")
    g.write_json(g.ASSETS / "models/item/stone_hinge_lesson.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "wildercord:item/stone_hinge_lesson"}})
    g.write_json(g.ASSETS / "items/stone_hinge_lesson.json", {"model": {"type": "minecraft:model", "model": "wildercord:item/stone_hinge_lesson"}})
