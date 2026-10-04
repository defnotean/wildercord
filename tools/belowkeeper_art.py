"""Belowkeeper restoration: the Drainhouse ledger desks and their materials, Mara's Empty Bell, the Rootbound Greaves
and the three ledger lore books, drawn like the rest of Wildercord: 16x16 pixel art lit from the top left, a few
deliberate tones per material, and block models whose UVs follow their own size."""
import json

from item_art import Canvas, blit, hexc
from world_art import Sheet, box

LANG={
 'block.wildercord.drainhouse_mark':'Belowkeeper Ledger',
 'item.wildercord.maras_empty_bell':"Mara's Empty Bell",'item.wildercord.maras_empty_bell.lore':'The clapper was removed so the roots could hear the rim.',
 'item.wildercord.maras_empty_bell.use':'Stand still and hold use for one second near one visible Rootmolt warning or grab. Moving or changing the held bell cancels commitment. Interrupts that creature; one wear, twelve-second shared rest and two heavy seconds. Restore or repair at the authentic Drainhouse alcove.',
 'item.wildercord.rootbound_greaves':'Rootbound Greaves','item.wildercord.rootbound_greaves.lore':'Small roots woven through leather remember one still place.',
 'item.wildercord.rootbound_greaves.use':'Wear in the feet slot. Crouch still on dry ground for 1.5 seconds to resist one Rootmolt hold after its physical damage. Eight extra wear, ten-second shared rest and two heavy seconds. Movement, jumping, swimming, sprinting or swapping boots breaks preparation.',
 'item.wildercord.drainhouse_threshold':'The Borrowed Third Breath','item.wildercord.drainhouse_garden':'Six Feet at the Table','item.wildercord.drainhouse_alcove':'What the Empty Bell Keeps',
 **{'message.wildercord.drainhouse.'+k:v for k,v in {'snail':'Crouch with an empty hand and gather dew from a rested Sporeback carrying a bead.','threshold':'Read the threshold ledger of an authentic Belowkeeper Drainhouse.','meal':'Stay within six blocks, with a clear view, while a living Rootmolt eats a mature Glowcap.','garden':'Read the damp garden ledger after that actual meal.','counter':'After seeing a meal, strike the living Rootmolt during its warning or rake, or sidestep the rake and stay nearby until it ends.','alcove':'Read the raised copper alcove ledger after proving that counter.','restore':'Hold Mycelial Dew and use the raised alcove desk with two dew, two dried gills and four copper ingots.','done':'The bell is yours. Return with one dew, one gill and one copper to mend eight wear after the desk rests a minute.','replica':'This ledger has no authentic Drainhouse provenance, or you cannot reach it.'}.items()},
 'book.wildercord.drainhouse.0.1':"Mara, Belowkeeper\n\nA drain is a promise: water goes where we guide it. The hill settled, but green gutters still catch rain. The ribs still hold the roof. This house did not fall at once.",
 'book.wildercord.drainhouse.0.2':"The third breath\n\nThe pale spiral leaves the cap standing. The six-footed keeper eats it and guards the bank. We called both helpers too soon. Watch the meal before giving a creature a name.",
 'book.wildercord.drainhouse.0.3':"A safe visit\n\nGently gather real dew from a snail first. Prepare a bud with Life; leave the spiral a clear path. See a living Rootmolt eat a mature cap within six blocks, then read the garden desk.",
 'book.wildercord.drainhouse.1.1':"Six feet at table\n\nThe cap vanished beneath breathing gills. No dew bead filled my jar. The plated stranger left, then turned when I neared its bank. Hunger had ended. Ownership had begun.",
 'book.wildercord.drainhouse.1.2':"A refused line\n\nHit a living keeper during its warning, rake or hold. Or step aside: stay near the marked line, keep a clear view and let the rake finish. Running far away proves no counter.",
 'book.wildercord.drainhouse.1.3':"The upper desk\n\nThe hands that saw the meal must prove a counter. A corpse cannot show a warning. Bring that witness to the raised copper alcove and read its ledger. We left the empty bell unfinished.",
 'book.wildercord.drainhouse.2.1':"The empty bell\n\nI took out the cracked clapper and tapped the rim. The roots loosened. Others called it a charm. I call it a brief, close interruption that can leave both creatures alive.",
 'book.wildercord.drainhouse.2.2':"Restore the rim\n\nRead all three desks; prove the meal and counter. Bring two dew beads, two harvested gills and four copper ingots. Use dew on the authentic upper desk. Each traveller earns one bell.",
 'book.wildercord.drainhouse.2.3':"Rootbound feet\n\nCraft greaves with boots, copper, gills and dew. Crouch still on dry ground; one hold is stopped after its wound. Bell and roots must rest. Mend at this desk with dew, gills and copper.",
 **{'subtitles.wildercord.kit.drainhouse.'+k:v for k,v in [('read','Belowkeeper ledger opens'),('restore','Copper rim is bound'),('bell','Empty bell answers roots'),('anchor','Woven roots hold footing')]}
}


# ============================================================== drawing helpers


def sprite(text, pal, size=16):
    """A size x size image from an ASCII block (every row exactly `size` wide); '.' and unknown keys stay clear."""
    rows = [r.strip() for r in text.strip("\n").splitlines() if r.strip()]
    assert len(rows) == size and all(len(r) == size for r in rows), [len(r) for r in rows]
    cv = Canvas(size)
    blit(cv, "\n".join(rows), {k: hexc(v) for k, v in pal.items()})
    return cv.image()


# ============================================================== block textures (16x16)

# Copper ramp shared by the plates, the bell and the clasp, with its verdigris.
COPPER = {"d": "#6A321E", "b": "#A2532F", "c": "#C46E43", "C": "#DE8A5A", "h": "#F2A97A", "r": "#FFD8B0", "s": "#7E3B22",
          "v": "#3F8C74", "V": "#6EC4A4"}

# Two riveted cut-copper plates, a seam between them, verdigris creeping in from the corners.
COPPER_TEX = """
hhhhhhhhhhhhhhhd
hCCCCCCCCCCCCCcd
hCrcccccccccrccd
hCscccccvvccsccd
hCcccccvVvcccccd
hCcccccccvcccccd
hcccccccccccccbd
dddddddddddddddd
hhhhhhhhhhhhhhhd
hCCCCCCCCCCCCCcd
hCrcccccccccrccd
hvscccccccccsccd
hVvcccccccccccvd
hvcccccccccccvVd
hcccccccccccccbd
dddddddddddddddd
"""

# Damp drain stone in two courses of big dressed blocks, a little moss in the joints.
STONE = {"k": "#3A4446", "s": "#5E6C6C", "m": "#7A8988", "l": "#97A5A2", "h": "#B4C0BB", "g": "#4F7A34", "G": "#78A445"}
STONE_TEX = """
hlllllllllhklllh
lmmmmmmmmmskmmml
lmmmmlmmmmskmmml
lmmmmmmmmmskmlmm
lmmmmmmmsmskmmmm
lmlmmmmmmmskmmms
smmmmmmmmmskmmms
kkkkGkkkkkkkgkkk
lllhkhllllllllll
mmmskmmmmmmmlmmm
mmmskmmmmmmmmmmm
mlmskmmmmmsmmmmm
mmmskmmmmmmmmmlm
mmmskmmlmmmmmmmm
ssmskmmmmmmmmmss
kgkkkkkkkGgkkkkk
"""

# Old leather, stitched round its edge.
LEATHER = {"e": "#3E2214", "d": "#5E361E", "m": "#8A5530", "l": "#A66B3D", "h": "#C4874F", "t": "#E8CC92"}
LEATHER_TEX = """
eeeeeeeeeeeeeeee
ehhhhhhhhhhhhhle
ehtltltltltltlde
eltmmmmmmmmmmtde
ehmmmmlmmmmmmmde
eltmmllmmmmmmtde
ehmmmmmmmmdmmmde
eltmmmmmmmmdmtde
ehmmmmmmmmmmmmde
eltmmmdmmmmmmtde
ehmmmmmdmmmlmmde
eltmmmmmmmmllmte
ehmmmmmmmmmmmmde
eltdtdtdtdtdtdde
elddddddddddddde
eeeeeeeeeeeeeeee
"""

# Page edges seen from the side: cream leaves with a shadow every few.
PAPER = {"p": "#F2E8C8", "q": "#DDCFA6", "s": "#B9A77A"}
PAPER_TEX = "\n".join(("qpppqppppqpppppq" if y % 4 == 0 else "pppppppppppppppp" if y % 4 == 1 else
                       "qqqqqqqqqqqqqqqq" if y % 4 == 2 else "ssssqsssssssqsss") for y in range(16))


def woven_root():
    """Roots woven over and under each other in four-pixel squares, like a basket grown rather than made."""
    dark, shadow, mid, light, hi = (hexc(c) for c in ("#2E1E12", "#5A3E24", "#86603A", "#A88050", "#CBA66E"))
    moss = hexc("#6E9A3A")
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            lx, ly = x % 4, y % 4
            across = (x // 4 + y // 4) % 2 == 0
            a, b = (lx, ly) if across else (ly, lx)
            # a runs along the root, b across it: two strands per square, each lit on its upper/left side.
            c = (hi if b % 2 == 0 else mid) if a not in (0, 3) else (light if b % 2 == 0 else shadow)
            if a == 0:
                c = dark if b % 2 else shadow
            cv.put(x, y, c)
    for x, y in ((5, 2), (13, 9), (2, 13), (10, 14)):
        cv.put(x, y, moss)
    return cv.image()


# Ledger pages: ruled paper with the room's drawing in the middle (what the models' page faces show: the front of the
# upright ledger takes the middle ten pixels, the open ledgers' tops the middle eight to ten).
PAGE = {"P": "#F8F0D6", "p": "#EEE3BF", "q": "#D9CA9C", "l": "#B8A272", "Q": "#C2B184"}
PAGE_TEX = """
PPPPPPPPPPPPPPPq
Ppppppppppppppqq
Pplllllllllllpqq
Pppppppppppppppq
Ppllpppppppllpqq
Pppppppppppppppq
Ppllpppppppllpqq
Pppppppppppppppq
Ppllpppppppllpqq
Pppppppppppppppq
Ppllpppppppllpqq
Pppppppppppppppq
PplllllllllllpqQ
PpppppppppppppqQ
PqqqqqqqqqqqqqqQ
qQQQQQQQQQQQQQQQ
"""
# Each room's drawing and where it sits on the page.
LEDGER_ART = [
    # The threshold: a drain arch with the water it still guides.
    ("""
        .kkkkk.
        kaAAAak
        kAkkkAk
        kAkkkAk
        kAwwwAk
        kawWwak
        .kwWwk.
        wWwWwWw
    """, 4, 3, {"k": "#4A3424", "a": "#9AA3A0", "A": "#D4DAD4", "w": "#3F8F9C", "W": "#7CCFD6"}),
    # The garden: a mature Glowcap, eaten by the six-footed keeper.
    ("""
        ..kkkk..
        .kGGgGk.
        kGGgGgGk
        kggggggk
        .kksSkk.
        ...sS...
        ...sS...
        .mmsSmm.
    """, 4, 3, {"k": "#2C5A48", "g": "#5FB88E", "G": "#A6EAC2", "s": "#EADAB0", "S": "#A8946C", "m": "#5E8A34"}),
    # The alcove: the empty bell, its clapper taken out.
    ("""
        ...kSk...
        ..kkdkk..
        .kdChcdk.
        .kdChcdk.
        .kdCccdk.
        .kdCccdk.
        kdCCcccdk
        kkkkkkkkk
    """, 3, 3, {"k": "#4A2A1A", "S": "#8E7A58", "d": "#8E4428", "h": "#FFD0A0", "C": "#E89A64", "c": "#C46E43"}),
]


def ledger_page(kind):
    art, ox, oy, pal = LEDGER_ART[kind]
    cv = Canvas()
    blit(cv, PAGE_TEX, {k: hexc(v) for k, v in PAGE.items()})
    blit(cv, art, {k: hexc(v) for k, v in pal.items()}, ox, oy)
    return cv.image()


# ============================================================== item icons (16x16)

# A lore book, drawn the way the other Wildercord lore books are: gold tabs on the spine, gold corners, the cover's own
# emblem and a band of pages under it.
BOOK = """
................
..oooooooooooo..
..osGhhhhhhhGo..
.Yyshcccccccdo..
..oshcccccccdo..
.Yyshcccccccdo..
..oshcccccccdo..
.Yyshcccccccdo..
..oshcccccccdo..
.Yyshcccccccdo..
..oshcccccccdo..
..osGdddddddGo..
..oSppppppppppo.
..oSPPPPPPPPPPo.
..ooooooooooooo.
................
"""
BOOK_GOLD = {"G": "#E8C46A", "y": "#D8A848", "Y": "#7A5A20", "p": "#F4EAC8", "P": "#C8B488"}
# Each book: cover (outline, spine, highlight, cover, shadow) and its 7x8 emblem at (5, 3).
BOOKS = {
    "drainhouse_threshold": ({"o": "#141C22", "s": "#2C3C48", "S": "#22303A", "h": "#6E8698", "c": "#4E6678", "d": "#38495A"}, """
        ..aaa..
        .aAAAa.
        aAkkkAa
        aAkkkAa
        aAwwwAa
        aawWwaa
        ..wWw..
        .wWwWw.
    """, {"a": "#A8B2B4", "A": "#E0E6E2", "k": "#1E2830", "w": "#3FA6B4", "W": "#9CEEF0"}),
    "drainhouse_garden": ({"o": "#14200E", "s": "#2C4220", "S": "#22341A", "h": "#6E9A48", "c": "#4E7834", "d": "#3A5A26"}, """
        ..ggg..
        .gGGWg.
        gGGgGGg
        ggggggg
        .dsssd.
        ...sS..
        ...sS..
        .mmsSm.
    """, {"g": "#4FB890", "G": "#8EE8BC", "W": "#E8FFF0", "d": "#2C6450", "s": "#EADAB0", "S": "#B8A47C", "m": "#9CD45A"}),
    "drainhouse_alcove": ({"o": "#200E08", "s": "#4A2414", "S": "#3A1C10", "h": "#A8643A", "c": "#86482A", "d": "#64341E"}, """
        ...l...
        ..lLl..
        ..bhb..
        .bhCcb.
        .bhCcb.
        .bhCcb.
        bhCCccb
        bkkkkkb
    """, {"l": "#3A2214", "L": "#C8A070", "b": "#7A3A20", "h": "#FFD0A0", "C": "#F2A06A", "c": "#C86E40", "k": "#2A140C"}),
}


def book_icon(name):
    cover, emblem, pal = BOOKS[name]
    cv = Canvas()
    blit(cv, BOOK, {k: hexc(v) for k, v in {**BOOK_GOLD, **cover}.items()})
    blit(cv, emblem, {k: hexc(v) for k, v in pal.items()}, 5, 3)
    return cv.image()


# Rootbound Greaves: leather boots with a woven-root cuff, roots wound down the shaft and copper toe caps.
GREAVES = """
................
................
.ooooo....ooooo.
.orRrRo..oRrRro.
.oRrRro..orRrRo.
.ohLlgo..ohLlgo.
.ohlgLo..oglLlo.
.oglLlo..ohLgLo.
.ohLlgoooohlLlgo
oohlLlLooohLlglo
ochCllldoohLllLo
occCllldooCCccco
oddddddoodddddo.
.oooooo..oooooo.
................
................
"""
GREAVES_PAL = {"o": "#2A180C", "r": "#E0BC80", "R": "#9A7040", "h": "#C08850", "L": "#9A6238", "l": "#7A4828", "g": "#7CB048",
               "c": "#C46E43", "C": "#F2A97A", "d": "#3E2618"}


# Mara's Empty Bell: a small copper hand bell on a leather-wrapped grip; no clapper hangs in its mouth.
BELL = """
................
......oooo......
.....oLLllo.....
......oLlo......
.....oobboo.....
....ohCCccbo....
...ohCCCcccbo...
...ohCCccccbo...
...ohCcccccbo...
...ohCcccvcbo...
...ohCccccVbo...
...ohCcccccbo...
..ohCCccccccbo..
.ohCCcccccccccbo
.obkkkkkkkkkkbo.
..oooooooooooo..
"""
BELL_PAL = {"o": "#2A120A", "L": "#A8784A", "l": "#6E4628", "h": "#FFD8B0", "C": "#F2A97A", "c": "#D07848",
            "b": "#8E4428", "k": "#1C0A06", "v": "#3F8C74", "V": "#6EC4A4"}


# ============================================================== Rootbound Greaves worn (64x32 humanoid layer)

def boots_skin():
    """The boots on the leg box (texture offset 0,16; 4x12x4): the lower half of each side, a woven-root cuff on top, a
    copper toe cap at the front and a dark sole underneath, shaded like vanilla's leather boots."""
    cv = Sheet(64, 32)
    leather = [hexc(c) for c in ("#3E2618", "#6E4224", "#8E5A32", "#B07A48", "#CC9A62")]
    root_hi, root_lo, moss = hexc("#CBA66E"), hexc("#86603A"), hexc("#78A445")
    copper = [hexc(c) for c in ("#7E3B22", "#C46E43", "#F2A97A")]
    sole = hexc("#24160C")
    side_tone = {"front": 0, "right": 0, "left": -1, "back": -1}
    for u in (0, 40):
        faces = box(u, 16, 4, 12, 4)
        for name, (x0, y0, w, h) in faces.items():
            if name == "top":
                continue
            if name == "bottom":
                for y in range(h):
                    for x in range(w):
                        cv.put(x0 + x, y0 + y, sole if x in (0, w - 1) or y in (0, h - 1) else leather[0])
                continue
            for y in range(6, h):
                for x in range(w):
                    t = 2 + side_tone[name]
                    if y == 6:
                        c = root_hi if (x + (name in ("left", "back"))) % 2 == 0 else root_lo
                    elif y == 7:
                        c = leather[0]
                    elif y == h - 1:
                        c = sole
                    else:
                        if x == 0:
                            t += 1
                        elif x == w - 1:
                            t -= 1
                        c = leather[max(0, min(4, t))]
                        # One root winds down each side.
                        if x == (y + (1 if name in ("front", "back") else 0)) % w and y < h - 2:
                            c = root_lo if y % 3 else moss
                    if name == "front" and y in (h - 3, h - 2):
                        c = copper[2] if x == 0 or y == h - 3 and x < 2 else copper[1] if y == h - 3 else copper[0]
                    cv.put(x0 + x, y0 + y, c)
    return cv.image()


# ============================================================== writing

MATERIALS = {"copper": lambda: sprite(COPPER_TEX, COPPER), "stone": lambda: sprite(STONE_TEX, STONE),
             "leather": lambda: sprite(LEATHER_TEX, LEATHER), "paper": lambda: sprite(PAPER_TEX, PAPER), "root": woven_root}


def element(a, b, material, page=None):
    """One model element, its UVs taken from its own position (so every face keeps vanilla's 16 texels per block); the
    page faces (`page`: 'up' or 'ns') show the ledger instead of the material."""
    faces = {}
    for f in ("up", "down", "north", "south", "east", "west"):
        tex = material
        if page == "up" and f == "up" or page == "ns" and f in ("north", "south"):
            tex = "ledger"
        faces[f] = {"texture": "#" + tex}
        if f == "down" and a[1] == 0:
            faces[f]["cullface"] = "down"
    return {"from": a, "to": b, "faces": faces}


def ledger_boxes(kind, restored):
    if kind == 0:
        # The threshold: a stone sill with an upright leather-bound ledger clamped under a copper cap.
        boxes = [element([2, 0, 2], [14, 3, 14], "stone"), element([3, 3, 5], [13, 13, 11], "leather", "ns"),
                 element([2, 12, 4], [14, 14, 12], "copper")]
    elif kind == 1:
        # The garden: a root-woven footing, a leather cover and its open pages between two copper posts.
        boxes = [element([2, 0, 2], [14, 3, 14], "root"), element([3, 3, 3], [13, 5, 13], "leather"),
                 element([4, 5, 4], [12, 7, 12], "paper", "up"), element([2, 3, 2], [4, 14, 4], "copper"),
                 element([12, 3, 12], [14, 14, 14], "copper")]
    else:
        # The alcove: a raised copper desk on stone, the ledger on it and two roots grown up its sides.
        boxes = [element([1, 0, 1], [15, 4, 15], "stone"), element([2, 4, 3], [14, 7, 13], "copper"),
                 element([3, 7, 4], [13, 9, 12], "paper", "up"), element([2, 7, 3], [4, 12, 5], "root"),
                 element([12, 7, 11], [14, 12, 13], "root")]
        if restored:
            # The restored bell rests on the open ledger.
            boxes += [element([6, 9, 6], [10, 12, 10], "copper"), element([5, 9, 5], [11, 10, 11], "copper")]
    return boxes


def write(g):
    for name, paint in MATERIALS.items():
        g.save(paint(), g.ASSETS / f"textures/block/drainhouse_{name}.png")
    for kind in range(3):
        g.save(ledger_page(kind), g.ASSETS / f"textures/block/drainhouse_ledger_{kind}.png")
    for kind in range(3):
        textures = {name: f"wildercord:block/drainhouse_{name}" for name in MATERIALS}
        textures["ledger"] = f"wildercord:block/drainhouse_ledger_{kind}"
        textures["particle"] = textures["stone"]
        for restored in (False, True):
            g.write_json(g.ASSETS / f"models/block/drainhouse_mark_{kind}_{int(restored)}.json",
                         {"parent": "minecraft:block/block", "textures": textures, "elements": ledger_boxes(kind, restored)})
    g.write_json(g.ASSETS / "blockstates/drainhouse_mark.json", {"variants": {
        f"kind={k},restored={str(r).lower()}": {"model": f"wildercord:block/drainhouse_mark_{k}_{int(r)}"} for k in range(3) for r in (False, True)}})
    # Flat icons like every other Wildercord item.
    g.save(sprite(BELL, BELL_PAL), g.ASSETS / "textures/item/maras_empty_bell.png")
    g.save(sprite(GREAVES, GREAVES_PAL), g.ASSETS / "textures/item/rootbound_greaves.png")
    for name in ("rootbound_greaves", "maras_empty_bell"):
        g.item_model(name, name)
        g.write_json(g.ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{name}"}})
    g.save(boots_skin(), g.ASSETS / "textures/entity/equipment/humanoid/rootbound.png")
    g.write_json(g.ASSETS / "equipment/rootbound.json", {"layers": {"humanoid": [{"texture": "wildercord:rootbound"}]}})
    g.write_json(g.DATA / "recipe/rootbound_greaves.json", {"type": "minecraft:crafting_shapeless", "category": "equipment",
        "ingredients": ["minecraft:leather_boots", "minecraft:copper_ingot", "wildercord:dried_glowcap_gills", "wildercord:mycelial_dew"],
        "result": {"id": "wildercord:rootbound_greaves", "count": 1}})
    g.unlock_advancement("wildercord:rootbound_greaves", "wildercord:dried_glowcap_gills")
    g.write_json(g.DATA / "worldgen/feature/belowkeeper_drainhouse.json", {"type": "wildercord:belowkeeper_drainhouse"})
    g.write_json(g.DATA / "worldgen/placed_feature/belowkeeper_drainhouse.json", {"feature": "wildercord:belowkeeper_drainhouse", "placement": [
        {"type": "minecraft:rarity_filter", "chance": 48}, {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": -32}, "max_inclusive": {"absolute": 40}}}]})
    g.write_json(g.DATA / "loot_table/blocks/drainhouse_mark.json", {"type": "minecraft:block", "pools": []})
    for name in BOOKS:
        g.save(book_icon(name), g.ASSETS / f"textures/item/{name}.png")
        g.item_model(name, name)
        g.write_json(g.ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{name}"}})
    # Keep the members the armour generators before this one already put in the tag.
    target = g.RES / "data/minecraft/tags/item/foot_armor.json"
    prior = json.loads(target.read_text()) if target.exists() else {"replace": False, "values": []}
    prior["values"] = sorted(set(prior["values"] + ["wildercord:rootbound_greaves"]))
    g.write_json(target, prior)
