"""Ways: the four Ways' emblems (GUI sprites the Aura page tints with each Way's colour), the Crossroads Incense (its icon, its
recipe), and every word the Ways say. Called by generate_assets.py (write(g) with the rest of the pack, LANG with the language file).

The emblems are drawn in white and greys with a dark outline, 16x16, so the page can tint them (a Way's colour, dimmed for a locked
node): a sword planted point-down for the Blade, a shield with a cross for the Bulwark, a crescent over three fading strides for the
Shadowstep, a swallowtail pennant on its pole for the Banner, and a plain diamond for an add-on's Way without its own. The incense is a
bronze censer with a lit stick and four curls of smoke, one in each Way's colour. Everything is drawn by code and seeded, like the
rest of the mod's art.

    python tools/way_art.py      renders a review sheet into build/art-preview/way_art.png
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent

# The built-in Ways (aura.WayRules): id, colour. Kept in step by WayRulesTest.
WAYS = [
    ("blade", 0xFF6A58),
    ("bulwark", 0x6FB4FF),
    ("shadowstep", 0xB394FF),
    ("banner", 0xFFC94A),
]

# Grey levels for the emblems (tinted on the page): white, highlight, mid, deep, and the outline.
GREYS = {
    "W": (255, 255, 255, 255),
    "H": (226, 226, 230, 255),
    "M": (178, 178, 186, 255),
    "D": (122, 122, 132, 255),
    "o": (34, 30, 42, 255),
}

EMBLEMS = {
    # A sword planted point-down... drawn point up as it stands in the light: blade, guard, grip, pommel.
    "blade": """
        .......oo.......
        ......oWHo......
        ......oWHo......
        ......oWHo......
        ......oWHo......
        ......oWHo......
        ......oWHo......
        ......oWHo......
        ......oWHo......
        ...ooooWHoooo...
        ...oWWWWHHHMo...
        ...ooooMMoooo...
        ......oDDo......
        ......oDDo......
        ......oWHo......
        .......oo.......
    """,
    # A heater shield with a cross on its face.
    "bulwark": """
        ................
        ..oooooooooooo..
        ..oWWWWWWWWWHo..
        ..oWHHHHWHHHMo..
        ..oWHHHHWHHHMo..
        ..oWWWWWWWWWMo..
        ..oWHHHHWHHHMo..
        ..oWHHHHWHHHMo..
        ...oWHHHWHHMo...
        ...oWHHHWHHMo...
        ....oWHHWHMo....
        .....oWHWMo.....
        ......oWMo......
        .......oo.......
        ................
        ................
    """,
    # A crescent over three strides, fading behind as afterimages do.
    "shadowstep": """
        ................
        ........oooo....
        ......ooWWHo....
        .....oWHoo......
        ....oWHo........
        ....oWHo........
        ....oWHo........
        .....oWHoo......
        ......ooWWHo....
        ........oooo....
        ................
        ...oDo.oMo.oWo..
        ..oDo.oMo.oWo...
        .oDo.oMo.oWo....
        ..o...o...o.....
        ................
    """,
    # A swallowtail pennant on its pole, a finial on top.
    "banner": """
        ..ooo...........
        ..oWo...........
        ..oWoooooooooo..
        ..oWoWWWWWWWWHo.
        ..oWoWHHHHHHHo..
        ..oWoWHHHHHHo...
        ..oWoWHHHHHo....
        ..oWoWHHHHHHo...
        ..oWoWHHHHHHHo..
        ..oWoWWWWWWWWHo.
        ..oWoooooooooo..
        ..oWo...........
        ..oMo...........
        ..oMo...........
        ..oDo...........
        ..ooo...........
    """,
    # An add-on's Way without an emblem of its own: a plain diamond.
    "unknown": """
        ................
        .......oo.......
        ......oWHo......
        .....oWHHMo.....
        ....oWHHHHMo....
        ...oWHHHHHHMo...
        ..oWHHHHHHHHMo..
        ..oMHHHHHHHHDo..
        ...oMHHHHHHDo...
        ....oMHHHHDo....
        .....oMHHDo.....
        ......oMDo......
        .......oo.......
        ................
        ................
        ................
    """,
}


def _grid(text):
    rows = [r.strip() for r in text.strip("\n").split("\n")]
    return [r for r in rows if r]


def emblem(way_id):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(_grid(EMBLEMS.get(way_id, EMBLEMS["unknown"]))):
        for x, ch in enumerate(row[:16]):
            if ch in GREYS:
                img.putpixel((x, y), GREYS[ch])
    return img


def _rgb(c):
    return (c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF


def _soft(c, t):
    """A colour softened toward white (smoke)."""
    r, g, b = _rgb(c)
    return (round(r + (255 - r) * t), round(g + (255 - g) * t), round(b + (255 - b) * t))


INCENSE = """
    .R..B......V..Y.
    .R..B......V..Y.
    ..R..B....V..Y..
    ...R.B....V.Y...
    ....R.B..V.Y....
    .....RB..VY.....
    ......RBVY......
    ......oEEo......
    .......SS.......
    .......Ss.......
    ..okkkkSSkkkko..
    ..ommmmmmmmmmo..
    ...ommmmmmmmo...
    ....oddddddo....
    .....oooooo.....
    ......offo......
"""


def incense():
    pal = {
        "R": _soft(WAYS[0][1], 0.15) + (255,),
        "B": _soft(WAYS[1][1], 0.15) + (255,),
        "V": _soft(WAYS[2][1], 0.15) + (255,),
        "Y": _soft(WAYS[3][1], 0.1) + (255,),
        "E": (255, 196, 96, 255),
        "S": (92, 64, 44, 255),
        "s": (122, 88, 60, 255),
        "k": (232, 196, 112, 255),
        "m": (176, 128, 64, 255),
        "d": (118, 82, 42, 255),
        "f": (86, 60, 34, 255),
        "o": (40, 30, 26, 255),
    }
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(_grid(INCENSE)):
        for x, ch in enumerate(row[:16]):
            if ch in pal:
                img.putpixel((x, y), pal[ch])
    return img


def write(g):
    sprites = g.ASSETS / "textures/gui/sprites/aura"
    for way_id in [w for w, _ in WAYS] + ["unknown"]:
        g.save(emblem(way_id), sprites / f"way_{way_id}.png")
    tex = g.ASSETS / "textures/item"
    g.save(incense(), tex / "crossroads_incense.png")
    g.item_model("crossroads_incense", "crossroads_incense")
    g.write_json(g.ASSETS / "items/crossroads_incense.json", {"model": {"type": "minecraft:model", "model": "wildercord:item/crossroads_incense"}})
    # Two Aura Shards (a fallen knight's), an amethyst shard and blaze powder: a fight and a journey into the Nether.
    g.write_json(g.DATA / "recipe/crossroads_incense.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": ["wildercord:aura_shard", "wildercord:aura_shard", "minecraft:amethyst_shard", "minecraft:blaze_powder"],
        "result": {"id": "wildercord:crossroads_incense"}})
    g.unlock_advancement("wildercord:crossroads_incense", "wildercord:aura_shard")


# ============================================================== English

def _nodes():
    nodes = {
        "blade_edge": ("Keen Edge",
                       "Clean hits build momentum a third faster.",
                       "it pierces: it cuts on through a held guard instead of stopping there, through ten foes instead of six, and in a clash "
                       "it has an edge: a little more time on each beat, and an even struggle is its."),
        "blade_form": ("Cascade",
                       "Finishers add two fifths more (a player's cap still holds).",
                       "a finisher opens the nearest creature within five blocks whose stance is half worn or more (never a player or a boss), so a "
                       "worn-down crowd falls one after another."),
        "blade_sovereign": ("Storm of Edges",
                            "Each finisher feeds your awakening two seconds instead of one, up to eight.",
                            "while you're awakened, Aura Slash costs nothing and is ready again in a second."),
        "bulwark_edge": ("Wide Guard",
                         "Your stance takes two fifths less wear, and a hit knocks a third less momentum off you.",
                         "it covers every side, its perfect moment lasts half again as long, a held guard throws a third of each blow it catches "
                         "back at the striker, and it turns shots back at whoever loosed them (a little aura each)."),
        "bulwark_form": ("Living Wall",
                         "Aura armour takes a third of each blow instead of a quarter, and costs a fifth less.",
                         "it challenges: creatures within its reach that hunt your allies turn on you, and a creature that strikes your raised guard "
                         "staggers."),
        "bulwark_sovereign": ("Unbroken",
                              "While you're awakened nothing breaks your stance and everything a foe deals you is a fifth weaker (less against a "
                              "player's blows); spent, you aren't slowed.",
                              "it's a bastion: a foe's shots crossing into it from outside are turned back at its edge, and the creatures inside are "
                              "slowed a step more."),
        "shadowstep_edge": ("Slip",
                            "Blows and arts that land on a foe from behind wear its stance half again as fast.",
                            "a perfect guard against a blow slips you behind whoever struck, facing its back, so your counter falls there."),
        "shadowstep_form": ("Afterimage",
                            "For two seconds after an Aura Step every blow you land counts as from behind.",
                            "it leaves an afterimage where you set off that strikes a moment later, cutting every foe beside it."),
        "shadowstep_sovereign": ("Thousand Shadows",
                                 "A finisher landed from behind readies your Aura Step at once.",
                                 "while you're awakened it costs nothing, is ready again in a second, and its afterimage strikes twice."),
        "banner_edge": ("Battle Cry",
                        "A third of the momentum you build is built by your allied swordsmen near you too.",
                        "every finisher lets out a rallying cry: you and your allies near you take a sixth less from foes for five seconds, and "
                        "allied swordsmen build momentum and share half the aura it gave back."),
        "banner_form": ("Rallying Presence",
                        "A quarter of the aura you gather flows to your allied swordsmen near you.",
                        "while it presses on a foe, you and your allies within its reach take a tenth less from foes."),
        "banner_sovereign": ("Shelter",
                             "Allied swordsmen near you lose a third less momentum to hits, and your awakening holds their momentum at the second "
                             "tier while it burns.",
                             "it shelters the party: you and your allies inside take a fifth less from foes, your allied swordsmen gather aura there "
                             "as you do, and their momentum doesn't ebb."),
    }
    out = {}
    for node_id, (name, passive, change) in nodes.items():
        out[f"aura.wildercord.way_node.{node_id}"] = name
        out[f"aura.wildercord.way_node.{node_id}.passive"] = passive
        out[f"aura.wildercord.way_node.{node_id}.change"] = change[0].upper() + change[1:]
    return out


LANG = {
    # ---- the Ways
    "aura.wildercord.way.blade": "Way of the Blade",
    "aura.wildercord.way.blade.short": "Blade",
    "aura.wildercord.way.blade.creed": "Strike first, strike through, and finish what you start.",
    "aura.wildercord.way.bulwark": "Way of the Bulwark",
    "aura.wildercord.way.bulwark.short": "Bulwark",
    "aura.wildercord.way.bulwark.creed": "Stand where you are, and let nothing through.",
    "aura.wildercord.way.shadowstep": "Way of the Shadowstep",
    "aura.wildercord.way.shadowstep.short": "Shadowstep",
    "aura.wildercord.way.shadowstep.creed": "Be where the blow isn't, and strike where the eye isn't.",
    "aura.wildercord.way.banner": "Way of the Banner",
    "aura.wildercord.way.banner.short": "Banner",
    "aura.wildercord.way.banner.creed": "No one fights alone while your banner stands.",
    "aura.wildercord.way.kicker": "Your Way",
    "aura.wildercord.crossroads": "The Crossroads",
    "aura.wildercord.crossroads.kicker": "Choose your Way",
    # ---- the crossroads
    "message.wildercord.aura.way.crossroads": "Your path forks. A standard of light rises for each Way: strike the one you would walk.",
    "message.wildercord.aura.way.crossroads_how": "One strike leans toward a Way; strike it again to walk it. The Aura page's Way tab shows what each gives.",
    "message.wildercord.aura.way.waiting": "Your blade has come to its crossroads, but you walk no Way yet.",
    "message.wildercord.aura.way.waiting_how": "Hold the breathing stance a few seconds and the crossroads will rise round you.",
    "message.wildercord.aura.way.no_room": "The crossroads needs open ground round you to rise. Find some, and breathe in the stance.",
    "message.wildercord.aura.way.with_blade": "Strike the standard with your blade",
    "message.wildercord.aura.way.lean": "%s answers. Strike it again to walk it.",
    "message.wildercord.aura.way.chosen": "You walk the %s.",
    "message.wildercord.aura.way.node_now": "  %s (%s): yours now",
    "message.wildercord.aura.way.node_later": "  %s: yours at %s",
    "message.wildercord.aura.way.node_waking": "  %s (%s): wakes as you walk your new Way",
    "message.wildercord.aura.way.faded": "The crossroads fades. Hold the breathing stance to call it again.",
    "message.wildercord.aura.way.woke": "%s has woken: your new Way's %s node is in force.",
    "message.wildercord.aura.way.woke_later": "%s has woken: it will be yours at %s.",
    "message.wildercord.aura.way.unbound": "The incense burns your Way out of you: you no longer walk the %s.",
    "message.wildercord.aura.way.incense_off": "Ways don't work on this server",
    "message.wildercord.aura.way.incense_stage": "Only a swordsman who has reached Edge walks a Way",
    "message.wildercord.aura.way.incense_none": "You walk no Way to unbind. Hold the breathing stance to call the crossroads",
    "message.wildercord.aura.way.incense_place": "The incense won't catch here: burn it at a place of power",
    "hud.wildercord.crossroads.hint": "The crossroads: strike a standard to choose your Way (%ss)",
    "hud.wildercord.crossroads.lean": "Strike to lean toward it",
    "hud.wildercord.crossroads.walk": "Strike again to walk it",
    # ---- the Aura page
    "screen.wildercord.aura.way": "Way",
    "screen.wildercord.aura.way.walking": "Your Way: %s",
    "screen.wildercord.aura.way.none_yet": "No Way yet. Hold the breathing stance a few seconds and the crossroads rises: strike the standard of the Way you would walk.",
    "screen.wildercord.aura.way.before_edge": "Your Way is chosen at the crossroads, which rises when you break through to %s. Here is what each gives.",
    "screen.wildercord.aura.way.state.open": "Yours if you choose this Way",
    "screen.wildercord.aura.way.state.chosen": "In force",
    "screen.wildercord.aura.way.state.waking": "Waking: earn experience walking your new Way",
    "screen.wildercord.aura.way.state.upcoming": "Yours at a later stage",
    "screen.wildercord.aura.way.state.locked": "Another Way's",
    "screen.wildercord.aura.way.node_title": "%s · %s",
    "screen.wildercord.aura.way.detail.chosen": "In force: your Way's",
    "screen.wildercord.aura.way.detail.open": "Yours if you walk the %s",
    "screen.wildercord.aura.way.detail.locked": "Not yours: it belongs to the %s",
    "screen.wildercord.aura.way.detail.waking": "Waking: %s more experience walking your new Way",
    "screen.wildercord.aura.way.detail.upcoming": "Yours when you reach %s",
    "screen.wildercord.aura.way.passive": "Passive: %s",
    "screen.wildercord.aura.way.changes": "Changes %s. %s",
    "screen.wildercord.aura.way.change_line": "Changes: %s",
    "screen.wildercord.aura.way.change": "To change Way, burn a %s at a place of power.",
    "screen.wildercord.aura.way.how": "A Way can be changed later: a %s at a place of power.",
    # ---- the incense
    "item.wildercord.crossroads_incense": "Crossroads Incense",
    "item.wildercord.crossroads_incense.lore": "Its smoke curls four ways at once, and none of them is yours.",
    "item.wildercord.crossroads_incense.use": "Burned at a place of power, it unbinds your Way and calls the crossroads to choose again.",
    # ---- the Grimoire
    "toast.wildercord.aura.crossroads": "The Crossroads",
    "toast.wildercord.aura.way": "A Way Chosen",
    "toast.wildercord.aura.way_blade": "Way of the Blade",
    "toast.wildercord.aura.way_bulwark": "Way of the Bulwark",
    "toast.wildercord.aura.way_shadowstep": "Way of the Shadowstep",
    "toast.wildercord.aura.way_banner": "Way of the Banner",
    "toast.wildercord.aura.way_cascade": "Cascade",
    "toast.wildercord.aura.way_slip": "Slip",
    "toast.wildercord.aura.way_afterimage": "Afterimage",
    "toast.wildercord.aura.way_cry": "Battle Cry",
    # ---- operators
    "command.wildercord.aura.way.no_crossroads": "No crossroads rose (it needs Edge or above, no Way walked, and room round you)",
    "command.wildercord.aura.way.unknown": "No Way called %s",
    "command.wildercord.aura.way.state": "Way: %s",
}
LANG.update(_nodes())


def preview():
    sheet = Image.new("RGBA", (64 * 7, 64), (40, 34, 52, 255))
    for i, (way_id, color) in enumerate(WAYS + [("unknown", 0xE8D8B0)]):
        icon = emblem(way_id)
        tinted = Image.new("RGBA", icon.size)
        r0, g0, b0 = _rgb(color)
        for y in range(16):
            for x in range(16):
                r, g, b, a = icon.getpixel((x, y))
                tinted.putpixel((x, y), (r * r0 // 255, g * g0 // 255, b * b0 // 255, a))
        sheet.alpha_composite(tinted.resize((64, 64), Image.NEAREST), (i * 64, 0))
    sheet.alpha_composite(incense().resize((64, 64), Image.NEAREST), (6 * 64, 0))
    out = ROOT / "build/art-preview"
    out.mkdir(parents=True, exist_ok=True)
    sheet.save(out / "way_art.png")
    print(f"wrote {out / 'way_art.png'}")


if __name__ == "__main__":
    preview()
