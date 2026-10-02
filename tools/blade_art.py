"""The bonded blade: the Aura page's Blade tab's pictures (each tier's emblem, each trait's glyph, the ley crossing and the kneeling
swordsman of the ceremony's steps), and every word the bond says. Called by generate_assets.py (write(g) with the rest of the pack, LANG
with the language file).

The glyphs are drawn by code at 16x16 in white and greys and given a dark outline, as the technique glyphs are (technique_art.Grid), so
the page can tint them: a tier's emblem in the blade's own colour, a trait's in gold once chosen.

    Bonded       a blade upright, a ring round it at the guard
    Named        the blade, three marks beside it up its length
    Awakened     the blade, tongues of aura licking off both edges
    Soulforged   the blade, a ring at the guard and a star at its point

    python tools/blade_art.py      renders a review sheet into build/art-preview/blade_art.png
"""
import math
from pathlib import Path

from PIL import Image

import technique_art as ta

ROOT = Path(__file__).resolve().parent.parent

TIERS = ["bonded", "named", "awakened", "soulforged"]
# The traits, in the page's order (aura.BladeRules.BUILT_IN_TRAITS): kept in step by BladeRulesTest.
TRAITS = ["well_worn", "closing_stroke", "sundering_steel", "riposte", "wind_step", "long_crescent", "inkbound", "second_blaze",
          "mountainfeller", "gravewarden", "last_light", "moonwake", "rallying_steel"]


def _blade(g, top=1, bottom=14, x=7.5, v=4):
    """An upright blade: a point at the top, a two-pixel blade, a crossguard and a grip."""
    g.poly([(x - 1.2, bottom - 4.5), (x - 1.2, top + 2.5), (x + 0.5, top), (x + 2.2, top + 2.5), (x + 2.2, bottom - 4.5)], v)
    g.line(x - 2.5, bottom - 4, x + 3.5, bottom - 4, 3)
    g.line(x + 0.5, bottom - 3, x + 0.5, bottom, 2)


def tier(t):
    g = ta.Grid()
    if t == "bonded":
        g.arc(8, 9, 5.6, 0, 360, 2)
        _blade(g)
    elif t == "named":
        _blade(g, x=6.0)
        for i, y in enumerate((3, 6, 9)):
            g.px(12, y, 4)
            g.px(13, y + 1, 3)
            g.px(12, y + 1, 2)
    elif t == "awakened":
        _blade(g)
        for side in (-1, 1):
            for k, y in enumerate((4, 7, 10)):
                x = 8 + side * (3 + (k % 2))
                g.line(x, y + 1, x + side * 1, y - 1, 3)
                g.px(x + side, y - 2, 2)
    else:
        g.arc(8, 10, 5.0, 0, 360, 2)
        _blade(g, top=3)
        g.px(8, 0, 4)
        g.line(6, 1, 10, 1, 3)
        g.px(8, 2, 4)
        g.px(6, 0, 2)
        g.px(10, 0, 2)
    return g.image()


def trait(t):
    g = ta.Grid()
    if t == "well_worn":
        # The same stroke made again and again: three arcs nested.
        for r in (3, 5, 7):
            g.arc(3, 13, r + 1, -90, 0, 4 if r == 7 else 3)
        g.disc(3, 13, 1.2, 4)
    elif t == "closing_stroke":
        # A blade coming down on a burst.
        g.line(2, 2, 10, 10, 4)
        g.line(3, 2, 11, 10, 3)
        for a in range(0, 360, 45):
            g.line(11 + 1.2 * math.cos(math.radians(a)), 11 + 1.2 * math.sin(math.radians(a)), 11 + 3.6 * math.cos(math.radians(a)),
                   11 + 3.6 * math.sin(math.radians(a)), 3)
    elif t == "sundering_steel":
        # A shield cracked through.
        g.poly([(3, 2), (13, 2), (13, 8), (8, 14), (3, 8)], 2)
        g.line(9, 2, 7, 6, 4)
        g.line(7, 6, 9, 9, 4)
        g.line(9, 9, 7, 13, 4)
    elif t == "riposte":
        # Two blades crossed, a spark where they meet.
        g.line(2, 13, 13, 2, 3)
        g.line(2, 2, 13, 13, 3)
        g.disc(7.5, 7.5, 1.6, 4)
        for x, y in ((7, 3), (3, 7), (12, 7), (7, 12)):
            g.px(x, y, 4)
    elif t == "wind_step":
        # A foot's print, three lines of wind behind it.
        g.disc(10, 5, 2.2, 4)
        g.disc(10, 10, 2.6, 4)
        g.px(10, 13, 3)
        for y in (4, 8, 12):
            g.line(1, y, 5, y, 3)
    elif t == "long_crescent":
        # A long crescent flying, streaks after it.
        g.arc(12, 8, 6, 130, 230, 4)
        g.arc(13, 8, 6, 140, 220, 3)
        for y in (5, 8, 11):
            g.line(1, y, 3 + (y % 2), y, 2)
    elif t == "inkbound":
        # A brush laid across a blade.
        _blade(g, x=5.0)
        g.line(13, 2, 6, 11, 3)
        g.line(14, 3, 7, 12, 2)
        g.disc(6, 12, 1.1, 4)
    elif t == "second_blaze":
        # Two flames, the second springing up after the first.
        g.poly([(4, 14), (2, 9), (4, 6), (5, 9), (7, 4), (8, 10), (7, 14)], 3)
        g.poly([(9, 14), (8, 9), (10, 5), (11, 8), (13, 2), (14, 9), (13, 14)], 4)
    elif t == "mountainfeller":
        # A peak, cut across by a slash.
        g.poly([(1, 14), (7, 3), (10, 8), (12, 6), (15, 14)], 2)
        g.line(2, 11, 14, 5, 4)
        g.line(2, 12, 14, 6, 3)
    elif t == "gravewarden":
        # A gravestone, a blade standing before it.
        g.poly([(3, 14), (3, 5), (5, 3), (9, 3), (11, 5), (11, 14)], 2)
        g.line(6, 6, 8, 6, 1)
        g.line(7, 5, 7, 9, 1)
        _blade(g, top=6, bottom=15, x=12.0)
    elif t == "last_light":
        # A heart, a small flame burning out of it.
        g.disc(5, 7, 2.6, 3)
        g.disc(10, 7, 2.6, 3)
        g.poly([(2.6, 8), (12.4, 8), (7.5, 14)], 3)
        g.poly([(7, 7), (6, 4), (7.5, 1), (9, 4), (8, 7)], 4)
    elif t == "moonwake":
        # A crescent moon, a star by it.
        g.disc(7, 8, 5.5, 4)
        for y in range(16):
            for x in range(16):
                if (x - 9.5) ** 2 + (y - 6.5) ** 2 <= 4.6 ** 2:
                    g.clear(x, y)
        g.px(13, 11, 4)
        g.px(12, 11, 2)
        g.px(14, 11, 2)
        g.px(13, 10, 2)
        g.px(13, 12, 2)
    elif t == "rallying_steel":
        # A pennant flying from a blade.
        _blade(g, x=3.0)
        g.poly([(5, 2), (14, 4), (5, 8)], 3)
        g.line(5, 2, 13, 4, 4)
    else:
        g.arc(8, 7, 4, 180, 360, 3)
        g.arc(8, 7, 4, 0, 60, 3)
        g.line(9, 10, 8, 11, 3)
        g.px(8, 13, 4)
    return g.image()


def crossing():
    """A ley crossing: two winding lines meeting, a bright point where they cross."""
    g = ta.Grid()
    for i in range(16):
        g.px(i, 7.5 + 2.2 * math.sin(i * 0.45), 3)
        g.px(7.5 + 2.2 * math.sin(i * 0.45 + 1.3), i, 2)
    g.disc(7.5, 7.5, 1.8, 4)
    return g.image()


def kneel():
    """A swordsman kneeling in the breathing stance, the blade held point down before them."""
    g = ta.Grid()
    g.disc(6, 3, 1.8, 4)
    g.poly([(4, 5), (8, 5), (8, 10), (4, 10)], 3)
    g.poly([(4, 10), (10, 10), (10, 12), (4, 12)], 3)
    g.line(9, 12, 9, 14, 3)
    g.line(4, 12, 4, 14, 2)
    g.line(8, 6, 11, 8, 3)
    # The blade, point down.
    g.line(12, 4, 12, 13, 4)
    g.line(11, 6, 13, 6, 3)
    return g.image()


def write(g):
    sprites = g.ASSETS / "textures/gui/sprites/aura/blade"
    for t in TIERS:
        g.save(tier(t), sprites / f"tier_{t}.png")
    for t in TRAITS + ["unknown"]:
        g.save(trait(t), sprites / f"trait_{t}.png")
    g.save(crossing(), sprites / "crossing.png")
    g.save(kneel(), sprites / "kneel.png")


# ============================================================== English

TIER_TEXT = {
    "bonded": ("Bonded", "Kept through death and never breaking; only you can pick it up, take it from a chest or use its aura, and it comes home "
                         "from anyone else's hands. It gathers resonance in every fight."),
    "named": ("Named", "It takes a name, its own or yours, and blows with it draw a tenth more aura."),
    "awakened": ("Awakened", "It offers three traits drawn from how you fight, and takes the one you choose."),
    "soulforged": ("Soulforged", "Its trait half again as strong, blows a fifth more aura, its fullest look, and its traits offered once more "
                                 "(change free, once)."),
}

# Each trait: (name, what it does, a short line for its card, why it's offered: its habit counted).
TRAIT_TEXT = {
    "well_worn": ("Well-Worn Verse", "Its favourite art costs 15% less aura and rests 15% less with it in hand.",
                  "Its favourite art cheaper, and back sooner.", "%s played %s times"),
    "closing_stroke": ("Closing Stroke", "Finishers with it give back half again as much aura, and build 4 more momentum.",
                       "Finishers give back more aura and momentum.", "From %s finishers"),
    "sundering_steel": ("Sundering Steel", "Blows, arts and slashes with it wear a stance 12% harder (6% on a player).",
                        "Stances break sooner under it.", "From %s stances broken"),
    "riposte": ("Riposte", "For two seconds after a perfect guard, your next coated blow lands 15% harder (half that on a player).",
                "The blow after a perfect guard lands harder.", "From %s perfect guards"),
    "wind_step": ("Wind Step", "Aura Step costs a quarter less and rests 15% less with it in hand.", "Aura Step cheaper and back sooner.",
                  "From %s Aura Steps"),
    "long_crescent": ("Long Crescent", "Aura Slash costs a fifth less and flies a fifth further.", "Aura Slash cheaper and further.",
                      "From %s slashes loosed"),
    "inkbound": ("Inkbound Steel", "Techniques of your own cost 10% less with it and rank a fifth faster.", "Techniques cheaper; they rank faster.",
                 "From %s techniques landed"),
    "second_blaze": ("Second Blaze", "Your awakening comes back a quarter sooner, and you're spent a quarter less long after it.",
                     "Awakening back sooner; less spent.", "From %s awakenings"),
    "mountainfeller": ("Mountainfeller", "Against bosses and Runebound foes, coated blows and arts land 10% harder and wear their stance 15% harder.",
                       "Harder on bosses and mighty foes.", "From %s bosses and %s mighty foes"),
    "gravewarden": ("Gravewarden", "Coated blows with it land 12% harder on the undead.", "Harder on the undead.", "From %s undead felled"),
    "last_light": ("Last Light", "Below a third of your health, you take 8% less from foes.", "Less harm taken when you're nearly down.",
                   "From %s foes felled near death"),
    "moonwake": ("Moonwake", "At night, blows with it draw a quarter more aura.", "More aura from blows at night.", "From %s foes felled by night"),
    "rallying_steel": ("Rallying Steel", "Your finishers give allied swordsmen within 10 blocks 5 momentum.", "Your finishers rally your allies.",
                       "From %s foes felled beside allies"),
}


def _traits():
    out = {}
    for t, (name, desc, short, habit) in TRAIT_TEXT.items():
        k = f"aura.wildercord.blade_trait.{t}"
        out[k] = name
        out[k + ".desc"] = desc
        out[k + ".short"] = short
        out[k + ".habit"] = habit
    return out


def _tiers():
    out = {}
    for t, (name, gives) in TIER_TEXT.items():
        out[f"aura.wildercord.blade.tier.{t}"] = name
        out[f"screen.wildercord.aura.blade.tier_gives.{t}"] = gives
    out["aura.wildercord.blade.tier.none"] = "Unbonded"
    return out


LANG = {
    # ---- the banner and the moments
    "aura.wildercord.blade.bonded": "Bonded",
    "aura.wildercord.blade.passed_to_you": "Passed to You",
    "aura.wildercord.blade.passed_on": "Passed On",
    "aura.wildercord.blade.somewhere": "an unknown land",
    "message.wildercord.aura.blade.kindling": "The blade answers. Hold still...",
    "message.wildercord.aura.blade.joining": "The ley lines run into you and the blade...",
    "message.wildercord.aura.blade.sealing": "The bond takes hold...",
    "message.wildercord.aura.blade.slipped": "The bond slipped away: it asks stillness to the end",
    "message.wildercord.aura.blade.left_hand": "The blade left your hand, and the bond with it",
    "message.wildercord.aura.blade.bonded": "%s is bonded to you, at a ley crossing in %s, on day %s. Its story begins here.",
    "message.wildercord.aura.blade.bonded_how": "It's kept through death and only ever yours. It grows with every fight: a name at Named, a trait at "
                                                "Awakened, its fullest at Soulforged. See the Aura page's Blade tab.",
    "message.wildercord.aura.blade.already": "Another blade is bonded to you: release it first (the Aura page's Blade tab)",
    "message.wildercord.aura.blade.off": "Bonded blades don't work on this server",
    "message.wildercord.aura.blade.edge": "A blade is bonded from Edge",
    "message.wildercord.aura.blade.not_bondable": "Only a sword, axe, spear or mace can be bonded",
    "message.wildercord.aura.blade.not_with_you": "Your bonded blade isn't with you",
    "message.wildercord.aura.blade.name_tier": "Your blade takes a name at Named",
    "message.wildercord.aura.blade.name_empty": "Give it a name it can be shown by",
    "message.wildercord.aura.blade.named": "Your blade is named %s.",
    "message.wildercord.aura.blade.trait_not_offered": "Your blade doesn't offer that",
    "message.wildercord.aura.blade.trait_levels": "Changing your blade's trait costs %s experience levels",
    "message.wildercord.aura.blade.traited": "%s takes the trait %s.",
    "message.wildercord.aura.blade.released": "You release %s. It's only steel again, and remembers whose it was.",
    "message.wildercord.aura.blade.slips": "%s is bonded to %s. It slips from your grasp and goes home.",
    "message.wildercord.aura.blade.came_home": "%s slipped from %s's grasp and came home to you.",
    "message.wildercord.aura.blade.waited": "%s waited for you, and comes back to your hand.",
    "message.wildercord.aura.blade.from_the_void": "%s fell out of the world, and found its way home to you.",
    "message.wildercord.aura.blade.passing": "The blade's light reaches for your disciple. Hold still...",
    "message.wildercord.aura.blade.passing_to": "%s is passing their blade to you. Stay kneeling...",
    "message.wildercord.aura.blade.pass_broken": "The passing broke: both must hold still to its end",
    "message.wildercord.aura.blade.pass_nobody": "There's nobody to pass it to",
    "message.wildercord.aura.blade.pass_bonded": "They're bonded to a blade of their own",
    "message.wildercord.aura.blade.pass_no_path": "Only a swordsman can be bonded to a blade",
    "message.wildercord.aura.blade.pass_not_disciple": "A blade is only passed to a disciple",
    "message.wildercord.aura.blade.passed_to_you": "%s is yours now, passed to you by %s. Its story goes on with you.",
    "message.wildercord.aura.blade.passed_on": "You pass %s to %s. Its story goes on with them.",
    "message.wildercord.aura.blade.passed_sleeps": "It gives you what your stage allows, and wakes the rest as you grow into it.",
    "message.wildercord.aura.blade.tier.named": "%s stirs: it's Named.",
    "message.wildercord.aura.blade.tier.named.how": "It has taken a name of its own. Give it another on the Aura page's Blade tab, if you wish.",
    "message.wildercord.aura.blade.tier.awakened": "%s awakens.",
    "message.wildercord.aura.blade.tier.awakened.how": "It offers three traits drawn from how you fight: choose one on the Aura page's Blade tab.",
    "message.wildercord.aura.blade.tier.soulforged": "%s is Soulforged.",
    "message.wildercord.aura.blade.tier.soulforged.how": "Its trait is half again as strong, and it offers its traits once more: keep yours, or "
                                                         "take another free.",
    # ---- the HUD
    "screen.wildercord.aura.blade.rite_bond": "Bonding: %ss",
    "screen.wildercord.aura.blade.rite_pass": "Passing the blade: %ss",
    # ---- the Blade tab
    "screen.wildercord.aura.blade": "Blade",
    "screen.wildercord.aura.blade.off": "Bonded blades don't work on this server.",
    "screen.wildercord.aura.blade.bond_title": "Bond a blade",
    "screen.wildercord.aura.blade.before_edge": "A swordsman bonds one blade from %s: a blade that grows with every fight, kept through death and "
                                                "only ever theirs.",
    "screen.wildercord.aura.blade.how": "Kneel in the breathing stance at a ley crossing with the blade in hand and hold still: in ten seconds it's "
                                        "bonded to you. You can bond one blade at a time.",
    "screen.wildercord.aura.blade.step_blade": "Hold the blade you mean to bond: a sword, an axe, a spear or the mace (aura-forged ones too).",
    "screen.wildercord.aura.blade.step_crossing": "Find a ley crossing, a place of power where two ley lines meet (the ley lines show them).",
    "screen.wildercord.aura.blade.step_kneel": "Kneel in the breathing stance and stay still while the ley lines run into the blade.",
    "screen.wildercord.aura.blade.hand_none": "Nothing in your hand can be bonded.",
    "screen.wildercord.aura.blade.hand_yes": "%s, in your hand, can be bonded.",
    "screen.wildercord.aura.blade.hand_no": "%s can't be bonded: only a sword, axe, spear or mace, one at a time, that wears.",
    "screen.wildercord.aura.blade.hand_bonded": "%s is already bonded.",
    "screen.wildercord.aura.blade.gives_title": "What a bonded blade is",
    "screen.wildercord.aura.blade.gives_kept": "Kept through death and never breaking: worn to its last point it stays notched until you mend it.",
    "screen.wildercord.aura.blade.gives_yours": "Only yours: nobody else can pick it up, take it from a chest, or use its aura, and in anyone "
                                                "else's hands it comes straight home to you. It can't burn, rot away or be lost to the void.",
    "screen.wildercord.aura.blade.gives_grows": "It grows with every fight you win with it (its resonance), takes on your aura's colour, and "
                                                "keeps its story.",
    "screen.wildercord.aura.blade.ladder_bond": "the ceremony",
    "screen.wildercord.aura.blade.ladder_asks": "%s, %s",
    "screen.wildercord.aura.blade.unnamed": "Unnamed",
    "screen.wildercord.aura.blade.tier_line": "%s, bonded on day %s",
    "screen.wildercord.aura.blade.sleeps": "Waits for you to reach %s to give all it has",
    "screen.wildercord.aura.blade.resonance": "Resonance",
    "screen.wildercord.aura.blade.resonance_tip": "Gathered in every fight with it in hand: foes felled (more for stronger ones and bosses), arts, "
                                                  "techniques and finishers that land, stances broken, perfect guards, awakenings, duels won, "
                                                  "and breakthroughs carried through. One foe can only give so much.",
    "screen.wildercord.aura.blade.waits_resonance": "More resonance toward %s",
    "screen.wildercord.aura.blade.waits_stage": "%s waits for you to reach %s",
    "screen.wildercord.aura.blade.waits_boss": "Soulforged waits for a boss felled with it",
    "screen.wildercord.aura.blade.waits_top": "As far as a blade can grow",
    "screen.wildercord.aura.blade.name_later": "It takes a name at Named (%s resonance): one of its own, or yours.",
    "screen.wildercord.aura.blade.name_label": "Name",
    "screen.wildercord.aura.blade.name_hint": "Click to name it",
    "screen.wildercord.aura.blade.suggest": "Suggest",
    "screen.wildercord.aura.blade.suggest_tip": "A name of its own, from its story: your method, your Way, where it was bonded, what it has felled, "
                                                "its favourite art. Click again for another.",
    "screen.wildercord.aura.blade.name_it": "Name it",
    "screen.wildercord.aura.blade.trait_title": "Its trait",
    "screen.wildercord.aura.blade.trait_choose": "Choose its trait",
    "screen.wildercord.aura.blade.trait_later": "It takes a trait at Awakened (%s resonance, from %s), drawn from how you fight.",
    "screen.wildercord.aura.blade.trait_reading": "What it reads in how you fight so far:",
    "screen.wildercord.aura.blade.trait_faint": "nothing yet",
    "screen.wildercord.aura.blade.trait_click": "Click to take it",
    "screen.wildercord.aura.blade.trait_change": "Changing costs %s levels: click twice",
    "screen.wildercord.aura.blade.trait_free_again": "Soulforged: change once, free",
    "screen.wildercord.aura.blade.traits_off": "Traits don't work on this server",
    "screen.wildercord.aura.blade.story": "Its story",
    "screen.wildercord.aura.blade.away": "Your blade isn't with you.",
    "screen.wildercord.aura.blade.last_with_you": "It was last with you at %s, %s, %s in %s.",
    "screen.wildercord.aura.blade.away_how": "A bonded blade is never lost: it lies where you left it (it can't burn or rot away, and nobody else "
                                             "can pick it up or take it from a chest), it comes home from anyone else's hands and from the void, "
                                             "and a death keeps it with you.",
    "screen.wildercord.aura.blade.dim.overworld": "the Overworld",
    "screen.wildercord.aura.blade.dim.nether": "the Nether",
    "screen.wildercord.aura.blade.dim.end": "the End",
    "screen.wildercord.aura.blade.release": "Release the bond",
    "screen.wildercord.aura.blade.release_sure": "Click to release",
    "screen.wildercord.aura.blade.release_note": "Releasing lets it go for good, to bond another.",
    "screen.wildercord.aura.blade.release_tip": "The blade is only steel again (remembering whose it was), its growth and gifts gone; bond it or "
                                                "another anew at a ley crossing.",
    # ---- the tooltip
    "tooltip.wildercord.bonded_blade.whose": "%s blade, bonded to %s",
    "tooltip.wildercord.bonded_blade.resonance": "%s / %s toward %s",
    "tooltip.wildercord.bonded_blade.resonance_top": "Resonance %s",
    "tooltip.wildercord.bonded_blade.waits_stage": "Waits for you to reach %s",
    "tooltip.wildercord.bonded_blade.waits_boss": "Waits for a boss felled with it",
    "tooltip.wildercord.bonded_blade.trait": "Trait: %s",
    "tooltip.wildercord.bonded_blade.well_worn": "%s (%s)",
    "tooltip.wildercord.bonded_blade.trait_waits": "A trait waits to be chosen (the Aura page)",
    "tooltip.wildercord.bonded_blade.notched": "Notched: it won't break, but mend it",
    "tooltip.wildercord.bonded_blade.not_yours": "Bonded to %s: it carries no aura for you, and goes home",
    "tooltip.wildercord.bonded_blade.shift": "Hold Shift for its story",
    "tooltip.wildercord.bonded_blade.bonded_on": "Bonded on day %s at a ley crossing in %s (%s, %s)",
    "tooltip.wildercord.bonded_blade.named_on": "Named %s on day %s",
    "tooltip.wildercord.bonded_blade.lineage": "Carried before by %s",
    "tooltip.wildercord.bonded_blade.count.kills": "Foes felled %s",
    "tooltip.wildercord.bonded_blade.count.bosses": "Bosses %s",
    "tooltip.wildercord.bonded_blade.count.arts": "Arts %s",
    "tooltip.wildercord.bonded_blade.count.finishers": "Finishers %s",
    "tooltip.wildercord.bonded_blade.count.techniques": "Techniques %s",
    "tooltip.wildercord.bonded_blade.count.guards": "Perfect guards %s",
    "tooltip.wildercord.bonded_blade.count.duels": "Duels won %s",
    "tooltip.wildercord.bonded_blade.favourite": "Played most: %s (%s)",
    "tooltip.wildercord.bonded_blade.deeds": "Its deeds",
    "tooltip.wildercord.bonded_blade.deed_day": "Day %s: %s",
    "tooltip.wildercord.bonded_blade.deed.boss": "felled %s",
    "tooltip.wildercord.bonded_blade.deed.breakthrough": "carried through to %s",
    "tooltip.wildercord.bonded_blade.deed.tier": "became %s",
    "tooltip.wildercord.bonded_blade.deed.duel": "won a duel against a duelist of %s",
    "tooltip.wildercord.bonded_blade.deed.peerless": "%s reached Peerless",
    "tooltip.wildercord.bonded_blade.deed.way": "walked the %s",
    "tooltip.wildercord.bonded_blade.deed.passed": "passed from %s to %s",
    "tooltip.wildercord.bonded_blade.deed.named": "named %s",
    "tooltip.wildercord.bonded_blade.former": "Once bonded to %s as %s (%s)",
    "tooltip.wildercord.bonded_blade.former_unnamed": "Once bonded to %s%s (%s)",
    # ---- the Grimoire
    "toast.wildercord.aura.bond": "A Bonded Blade",
    "toast.wildercord.aura.blade_named": "A Named Blade",
    "toast.wildercord.aura.blade_awakened": "An Awakened Blade",
    "toast.wildercord.aura.blade_soulforged": "A Soulforged Blade",
    # ---- operators
    "command.wildercord.aura.blade.state": "%s: %s, resonance %s, trait %s",
    "command.wildercord.aura.blade.none": "No bonded blade",
    "command.wildercord.aura.blade.not_carried": "Their bonded blade isn't with them",
    "command.wildercord.aura.blade.cant_bond": "Hold a bondable blade, from Edge, with no bond already standing",
    "command.wildercord.aura.blade.unknown_trait": "No trait called %s",
    "command.wildercord.aura.blade.cant_pass": "Can't pass it: %s",
}
LANG.update(_traits())
LANG.update(_tiers())


def preview():
    sheet = Image.new("RGBA", (64 * 10, 64 * 2), (40, 34, 52, 255))
    icons = [tier(t) for t in TIERS] + [trait(t) for t in TRAITS] + [trait("unknown"), crossing(), kneel()]
    for i, icon in enumerate(icons):
        sheet.alpha_composite(icon.resize((64, 64), Image.NEAREST), ((i % 10) * 64, (i // 10) * 64))
    out = ROOT / "build/art-preview/blade_art.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    print("wrote", out)


if __name__ == "__main__":
    preview()
