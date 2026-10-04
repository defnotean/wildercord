"""The Sporeback Snail: its skin, Mycelial Dew, Fungal Poultice, Mara's journal, the spawn egg and the English text.

A patient cave spiral: a glossy cream shell in three offset whorls, each broad side cut with its spiral groove, on a
soft wet lilac foot, with long eye stalks and a mint dew bead on the crown. The skin is painted face by face on
SporebackModel's box layout (128x64), lit like vanilla mobs; the icons are 16x16 ASCII grids in the house style.
"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from fungal_art import book_icon, egg_icon, sprite  # noqa: E402
from item_art import grid, hexc, ramp  # noqa: E402
from monster_art import LIGHT, cells, faces, fill, shade, smooth  # noqa: E402
from world_art import Sheet, box, noise  # noqa: E402

LANG={
 'entity.wildercord.sporeback_snail':'Sporeback Snail','item.wildercord.sporeback_snail_spawn_egg':'Sporeback Snail Spawn Egg',
 'item.wildercord.mycelial_dew':'Mycelial Dew','item.wildercord.mycelial_dew.lore':'A single cool bead, gathered after a patient fungal visit.','item.wildercord.mycelial_dew.use':'Combine with paper and a brown mushroom for Fungal Poultice.',
 'item.wildercord.fungal_poultice':'Fungal Poultice','item.wildercord.fungal_poultice.lore':'Belowkeepers call its pale glow borrowed sight.','item.wildercord.fungal_poultice.use':'Use for 30 seconds of Night Vision; Slowness for the first 6 seconds. Cannot refresh active Night Vision.',
 'item.wildercord.sporeback_journal':'The Patient Spiral',
 'message.wildercord.sporeback.quiet':'Crouch with an empty hand to gather a ready dew bead.',
 'message.wildercord.sporeback.wait':'The spiral has no ready dew. Let it browse living fungi and recover in peace.',
 'message.wildercord.sporeback.poultice_rest':'Your sight is already clear; the poultice is kept.',
 'guide.wildercord.sporeback_snail':'A peaceful cave browser with an offset spiral shell. Find it on moss or clay beneath Y48 in lush or dripstone caves. It visits mushrooms without eating the block, then holds one dew bead. Crouch empty-handed to gather; each snail rests two minutes between gatherings and must browse again. Fire and physical harm retract its antennae and prevent gathering. Life produces a brief spore answer without minting dew. Brightness sends it toward nearby cover. No breeding or unique death loot.',
 'guide.wildercord.sporeback_snail.hint':'Below the moss banks, watch pale spiral shells beside mushrooms. Their antennae move slowly.',
 'book.wildercord.sporeback.1':'Mara, Belowkeeper\n\nI thought the pale spiral was another stone. Then its eyes opened. It followed the damp bank to a brown mushroom and rested its mouth against the cap. The mushroom stayed. A cool bead appeared above the shell.',
 'book.wildercord.sporeback.2':'Borrowed sight\n\nWe gather kneeling, with bare hands. A frightened spiral closes its doors. No fire, no knocking. Even a second living chant cannot hurry the bead. Leave the mushroom and the snail both standing; return after its rest.',
 'book.wildercord.sporeback.3':'The first stairs\n\nFold dew into paper with brown mushroom pulp. Darkness parts for half a minute, but the first steps feel heavy. Carry a torch as well. The Belowkeepers never mistake a glimpse for a map; deeper doors still have to be found.',
 **{'subtitles.wildercord.kit.sporeback.'+n:t for n,t in [('call','Sporeback exhales'),('browse','Sporeback browses'),('hide','Spiral closes'),('answer','Spores answer'),('gather','Dew is gathered')]}}


# ============================================================== the skin (SporebackModel, 128x64)

SHELL = ramp("#5A4636", "#86694E", "#B4966E", "#D8C29A", "#F0E4C6", "#FFF8EA")
CARAMEL = ramp("#5A3820", "#7E5030", "#A86E40", "#C88E58", "#E0AC74", "#F4CC98")
GROOVE = ramp("#3A2838", "#5A3E58")
FOOT = ramp("#2E2240", "#4A3866", "#6A548E", "#8C74B2", "#B09AD2", "#D6C6EE")
SOLE = ramp("#5E5470", "#7C7090", "#9C90AE")
DEW = ramp("#2E8A72", "#6CD8B4", "#B8FFE0", "#F2FFF8")
EYE = hexc("#120C1A")

S_FOOT = box(0, 0, 8, 2, 13)
S_HEAD = box(44, 0, 5, 3, 5)
S_STALK = box(66, 0, 1, 4, 1)
S_TIP = box(74, 0, 2, 2, 2)
S_SHELL = box(0, 18, 8, 8, 8)
S_WHORL = box(36, 18, 6, 6, 6)
S_CROWN = box(64, 18, 4, 4, 4)
S_DEW = box(86, 18, 2, 2, 2)

# The spiral on each broad side of a whorl: 'g' the groove between turns, '.' the glossy band.
SPIRALS = {
    8: """
        ........
        .gggggg.
        ......g.
        .gggg.g.
        .g..g.g.
        .g....g.
        .gggggg.
        ........
    """,
    6: """
        ......
        .gggg.
        .g..g.
        .g.gg.
        .g....
        ......
    """,
    4: """
        ....
        .gg.
        ..g.
        ....
    """,
}


def shell_face(cv, area, face, size, seed):
    x0, y0, w, h = area
    light = LIGHT[face]
    if face in ("right", "left"):
        rows = grid(SPIRALS[size])
        for x, y, px, py in cells(area):
            ch = (rows[y][::-1] if face == "left" else rows[y])[x]
            if ch == "g":
                cv.put(px, py, GROOVE[0] if (x + y) % 4 else GROOVE[1])
                continue
            # Each band is lit from the top left and rolls into shadow toward the bottom right.
            t = 3 + light + (1 if x + y < size // 2 else 0) - (1 if x + y > size + 2 else 0)
            cv.put(px, py, shade(SHELL, t))
        # The gloss: a bright catch-light high on the outer band.
        cv.put(x0 + (1 if face == "right" else w - 2), y0, SHELL[5])
        cv.put(x0 + (2 if face == "right" else w - 3), y0, SHELL[5])
        return
    if face == "bottom":
        fill(cv, area, SHELL[1])
        return
    # Front, back and top: the growth ridges of the shell, lit along their upper edge.
    for x, y, px, py in cells(area):
        along = y if face != "top" else x
        t = 3 + light - (2 if along % 3 == 2 else 0) + (1 if along % 3 == 0 else 0)
        if noise(px, py, seed) > 0.93:
            t += 1
        # Every other growth band is a warm caramel, as on a real banded shell.
        cv.put(px, py, shade(CARAMEL if (along // 3) % 2 and along % 3 == 1 else SHELL, t))


def body(cv, area, face, seed, tones=FOOT):
    """Soft wet skin: gentle lilac mottling, darker toward the sole, with a few bright moist glints on top."""
    x0, y0, w, h = area
    for x, y, px, py in cells(area):
        n = smooth(px, py, seed, 2.5)
        t = 3 + LIGHT[face] + (1 if n > 0.7 else 0) - (1 if n < 0.25 else 0)
        if face not in ("top", "bottom") and y == h - 1:
            t -= 1
        cv.put(px, py, shade(tones, t))
        if face == "top" and noise(px, py, seed + 5) > 0.9:
            cv.put(px, py, tones[5])
    if face not in ("top", "bottom"):
        # The wet sheen along the upper edge of the flank.
        for x in range(w):
            if (x + seed) % 3:
                cv.put(x0 + x, y0, shade(tones, 4 + LIGHT[face]))


def skin():
    cv = Sheet(128, 64)
    for name, area in faces(S_FOOT):
        if name == "bottom":
            # The sole: smooth and pale, a lighter muscle band down its middle.
            for x, y, px, py in cells(area):
                cv.put(px, py, SOLE[2] if x in (3, 4) else SOLE[0] if x in (0, 7) else SOLE[1])
        else:
            body(cv, area, name, 905 + len(name))
    for name, area in faces(S_HEAD):
        body(cv, area, name, 925 + len(name))
    fx, fy, fw, fh = S_HEAD["front"]
    # A small soft mouth under the snout, and the paler lip around it.
    for x in range(1, 4):
        cv.put(fx + x, fy + 2, FOOT[1])
    cv.put(fx + 2, fy + 1, FOOT[4])
    for name, area in faces(S_STALK):
        body(cv, area, name, 940 + len(name))
    for name, area in faces(S_TIP):
        body(cv, area, name, 950 + len(name))
    # The eyes on the stalk tips: dark beads with a white catch-light.
    tx, ty, tw, th = S_TIP["front"]
    cv.put(tx, ty, EYE)
    cv.put(tx + 1, ty, hexc("#FFFFFF"))
    cv.put(tx, ty + 1, EYE)
    cv.put(tx + 1, ty + 1, EYE)
    for part, size, seed in ((S_SHELL, 8, 961), (S_WHORL, 6, 971), (S_CROWN, 4, 981)):
        for name, area in faces(part):
            shell_face(cv, area, name, size, seed + len(name))
    # The crown's apex: a dark point ringed in pale shell.
    ax, ay, aw, ah = S_CROWN["top"]
    cv.put(ax + 1, ay + 1, SHELL[5])
    cv.put(ax + 2, ay + 2, GROOVE[1])
    for name, area in faces(S_DEW):
        x0, y0, w, h = area
        for x, y, px, py in cells(area):
            t = 1 + (1 if name == "top" else 0) + (1 if x == 0 and y == 0 else 0) - (1 if name == "bottom" else 0)
            cv.put(px, py, DEW[t])
    return cv.image()


# ============================================================== icons (16x16)

MYCELIAL_DEW = """
    ................
    .......hM.......
    .......hM.......
    ......hMMm......
    ......hMMm......
    .....hWMMMm.....
    .....hWMMMm.....
    ....hWWMMMmd....
    ....hWMMMMmd....
    ...hMMMMMMMmd...
    ...hMMMMsMMmd...
    ...hMMMMMMMmd...
    ...mMMMMMMmdd...
    ....mMMMMmdd....
    .....dddddd.....
    ................
"""
DEW_PAL = {"h": "#D8FFEE", "W": "#FFFFFF", "M": "#7EDCB8", "m": "#4EB094", "d": "#2E7E6E", "s": "#F6FFC8"}

FUNGAL_POULTICE = """
    ................
    ......bBBb......
    .....bBBBbk.....
    ....bBBbbbbk....
    ....kkkSskkk....
    ..PPPPPSsPPPPP..
    ..PwwwwtTwwwwq..
    ..PwwwwtTwwwwq..
    ..PwwwwtTwwwgq..
    ..ttttttTTtttT..
    ..PgwwwtTwwwwq..
    ..PwwwwtTwwwwq..
    ..PwwwwtTwwwwq..
    ..qqqqqtTqqqqq..
    ................
    ................
"""
POULTICE_PAL = {"B": "#C8946A", "b": "#9A6A44", "k": "#64422A", "S": "#F2E6CA", "s": "#C4B08A",
                "P": "#FBF4DC", "w": "#E6D8B2", "q": "#B8A47C", "g": "#C8D8A8", "t": "#D0A660", "T": "#946C34"}

SPIRAL_EMBLEM = """
    ..eeee.
    .e....e
    e..ee.e
    e.e.he.
    e.e....
    .e...e.
    ..eee..
"""
LILAC_BOOK = ("#1E142C", "#3A2850", "#A88CCC", "#7E64A6", "#57437C")

EGG_MOTIF = """
    ................
    ................
    .......D........
    ................
    .....vvvv.......
    ....v....v......
    ....v.vv..v.....
    ....v.v.v.v.....
    ....v..v..v.....
    .....v...v......
    ......vvv.......
    ................
    ................
    ................
    ................
    ................
"""
EGG_PAL = {"D": "#B8FFE0", "v": "#6A4E8A", "L": "#A88CCC", "l": "#7E64A6"}


def write(g):
    g.save(skin(), g.ASSETS / "textures/entity/wildlife/sporeback_snail.png")
    icons = {
        "mycelial_dew": sprite(MYCELIAL_DEW, DEW_PAL, ring=0.62).image(),
        "fungal_poultice": sprite(FUNGAL_POULTICE, POULTICE_PAL, ring=0.62).image(),
        "sporeback_journal": book_icon(LILAC_BOOK, SPIRAL_EMBLEM, "#F4E8C8", "#B8FFE0"),
        "sporeback_snail_spawn_egg": egg_icon(("#9C8668", "#D2C09C", "#F2E8D0"), "#B8A27E", EGG_MOTIF, EGG_PAL, 23),
    }
    for name, im in icons.items():
        g.save(im, g.ASSETS / f"textures/item/{name}.png")
        g.write_json(g.ASSETS / f"models/item/{name}.json",
                     {"parent": "minecraft:item/generated", "textures": {"layer0": f"wildercord:item/{name}"}})
        g.write_json(g.ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{name}"}})
    g.write_json(g.DATA/'recipe/fungal_poultice.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':['wildercord:mycelial_dew','minecraft:paper','minecraft:brown_mushroom'],'result':{'id':'wildercord:fungal_poultice','count':1}})
    g.unlock_advancement('wildercord:fungal_poultice','wildercord:mycelial_dew')
    g.write_json(g.DATA/'loot_table/entities/sporeback_snail.json',{'type':'minecraft:entity','pools':[]})
