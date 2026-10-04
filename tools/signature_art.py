"""Hand-drawn rune icons for the signature fusions (merged into item_art.GLYPHS).

Same format as item_art.GLYPHS: a pictogram at most 9 wide and 9 tall, "." empty, "-" shade,
"#" main, "+" light, "*" core (the rune's own element's colours), "o" darkens the stone.

A signature rune is made from two particular runes, and its icon shows both: its own element in the
stone's colours, and its partner element in that element's glow, two tones each (main, then light),
added to item_art's fixed extra colours (EXTRA) when item_art loads this file:

    fire "f" "F"   frost "c" "C"   storm "z" "Z"   wind "g" "G"   earth "e" "E"
    life "l" "L"   void "u" "U"    arcane "m" "M"  time "t" "T"   blood: item_art's own "r" "R" "p"
"""

import sys

# Each partner element's two tones: the index of its glow ramp (item_art.ELEMENT) for main and light.
PARTNER_KEYS = {
    "fire": ("f", "F"), "frost": ("c", "C"), "storm": ("z", "Z"), "wind": ("g", "G"), "earth": ("e", "E"),
    "life": ("l", "L"), "void": ("u", "U"), "arcane": ("m", "M"), "time": ("t", "T"),
}


def _add_colours() -> None:
    """Adds the partner tones to the item_art module loading this file (never replacing a colour it has)."""
    for name in ("item_art", "__main__"):
        art = sys.modules.get(name)
        extra = getattr(art, "EXTRA", None) if art is not None else None
        element = getattr(art, "ELEMENT", None) if art is not None else None
        if isinstance(extra, dict) and isinstance(element, dict):
            for key, (main, light) in PARTNER_KEYS.items():
                glow = element[key]["glow"]
                extra.setdefault(main, glow[1])
                extra.setdefault(light, glow[2])


_add_colours()

GLYPHS: dict[str, str] = {
    # Frostwire (storm; frost): a bolt of lightning wired between two snowflakes.
    "frostwire": """
        .c.c.....
        ..C...+..
        .c.c.+#..
        ....+#...
        ...+**#..
        ...#*#...
        ..#+..c.c
        .#.....C.
        #.....c.c
    """,
    # Seethe (fire; frost): a flame boiling inside a bubble of water, steam curling off the top.
    "seethe": """
        ..w...w..
        ...w.w...
        ..ccccc..
        .c..+..c.
        c..+*+..c
        c.+*#*+.c
        c.+###+.c
        .c.....c.
        ..ccccc..
    """,
    # Bloomstep (life; void): a flower opening in a doorway of darkness.
    "bloomstep": """
        ..uuuuu..
        .u.....u.
        u...+...u
        u..+*+..u
        u...+...u
        u.#.#.#.u
        u..###..u
        u...#...u
        uU.....Uu
    """,
    # Skyburst (fire; wind): a burst of flame at the top, the gust that carried it up swirling below.
    "skyburst": """
        +.#.*.#.+
        .#.***.#.
        #.**+**.#
        .#.***.#.
        +.#.*.#.+
        ....g....
        ...g.G...
        ..G...g..
        .g..G..g.
    """,
    # Stitchtime (life; time): a heart held in a clock.
    "stitchtime": """
        ..ttttt..
        .t.....t.
        t.##.##.t
        t#++#++#t
        t#+***+#t
        t.#+*+#.t
        t..#+#..T
        .t..#..t.
        ..ttttt..
    """,
    # Parasite (blood; life): a drop of blood, and a green worm crawling into it.
    "parasite": """
        ...#.....
        ..#+#....
        ..#+#....
        .#+*+#...
        .#+*+#.l.
        ..###.lLl
        .....lLl.
        ....lLl..
        ...lL....
    """,
    # Razorgale (wind; blood): a whirl of wind, drops of blood flung off it.
    "razorgale": """
        ...###...
        .##...#..
        .#..#..#.
        #..#.#.#.
        #.#.R.#..
        #.#..#..R
        .#.##..p.
        ..#....#.
        ...####..
    """,
    # Doomclock (time; fire): a clock with a hand of fire, a fuse spark over it.
    "doomclock": """
        ....F.f..
        .....f...
        ..#####..
        .#..*..#.
        #...*...#
        #...*ff.#
        #.......#
        .#.....#.
        ..#####..
    """,
    # Thunderstep (storm; void): a bolt of lightning coming down into a pool of darkness.
    "thunderstep": """
        ....+#...
        ...+#....
        ..+##+...
        ...##....
        ..#*.....
        .#*......
        uuUuUuu..
        .uUuUu...
        ..uuu....
    """,
    # Halo (arcane; life): a ring of light, and a beam of it falling onto a sprouting leaf.
    "halo": """
        .+#####+.
        +.......+
        .+#####+.
        ....*....
        ...l*l...
        ..l.*.l..
        .l..*..l.
        ....L....
        ...LLL...
    """,
    # Thunderquake (earth; storm): lightning striking down into cracked ground.
    "thunderquake": """
        ....zZ...
        ...zZ....
        ..zZ.....
        ...zZ....
        .#..zZ.#.
        #.#..z#.#
        ###*##*##
        #-##-##-#
        #########
    """,
    # Cometfall (arcane; fire): a four-pointed star falling, trailing a tail of fire.
    "cometfall": """
        F........
        .fF......
        ..fFf....
        ...fF.#..
        ....F+*+.
        ....#***#
        .....+*+.
        ......#..
    """,
    # Riposte (time; arcane): a clock cut through by a slash of light.
    "riposte": """
        ..#####..
        .#.....M.
        #.....M.#
        #.*..M..#
        #..*M...#
        #..M*...#
        .#M....#.
        .M#####..
        M........
    """,
    # Dust Devil (wind; earth): a funnel of wind narrowing to the ground, sand whirling in it.
    "dust_devil": """
        #########
        .+++++++.
        ..#####..
        ...+++.e.
        ...###.E.
        ..e.#.e..
        ...E+....
        .e..#..E.
        ...e*e...
    """,
    # Malison (void; arcane): an eye of darkness in a seal, runes of arcane light round it.
    "malison": """
        m...M...m
        ...###...
        ..#kkk#..
        .#k*m*k#.
        M#k*m*k#M
        .#kkkkk#.
        ..#####..
        .........
        m...M...m
    """,
    # Avalanche (frost; earth): ice and stone falling onto a heap of snow and rock.
    "avalanche": """
        .+..e..+.
        .*.....*.
        .+..E..+.
        ....e....
        ..+...e..
        .+*+.eEe.
        +***+eEEe
        ####eeeee
        #########
    """,
    # Springbed (frost; life): a basin brimming with water, a sprout drinking from it.
    "springbed": """
        .LL...LL.
        ..Ll.lL..
        ....l....
        #...l...#
        #++*+*++#
        #+++++++#
        .#+++++#.
        ..#####..
    """,
    # Cinder Sieve (fire; void): a lump of coal burning on a sieve, sparks falling through its mesh.
    "cinder_sieve": """
        ....*....
        ...+#+...
        ..k+*+k..
        .kkk#kkk.
        .........
        uuUuUuUuu
        .u.u.u.u.
        ..+...*..
        ...*.+...
    """,
    # Ashen Mercy (life; fire): a heart closed round a guttering flame, its last smoke rising.
    "ashen_mercy": """
        ...o.....
        ....o....
        ...o.....
        .##.o.##.
        #..#.#..#
        #..fFf..#
        .#.fff.#.
        ..#...#..
        ...#.#...
    """,
    # Clockroot (earth; time): a clock face held fast over the ground by spreading roots.
    "clockroot": """
        ..TTTTT..
        .T..t..T.
        T...t...T
        T...ttt.T
        .T.....T.
        #.TTTTT.#
        .#..#..#.
        ..#.#.#..
        .#.#.#.#.
    """,
    # Skylatch (wind; void): a figure hanging in the air from a latch of darkness, the gust below it.
    "skylatch": """
        ..uuUuu..
        ..u...u..
        ....u....
        ...###...
        .#######.
        ...###...
        ...#.#...
        .+..+..+.
        .-..-..-.
    """,
    # Thresherwind (wind; life): a gust sweeping through ripe wheat, the cut ears thrown up ahead of it.
    "thresherwind": """
        .L.......
        LlL.##...
        .l.#++##.
        .l#....+#
        .L..L....
        LlLLlL.L.
        .l..l.LlL
        .l..l..l.
        .l..l..l.
    """,
    # Nullcatch (void; arcane): a hollow of darkness rimmed with arcane light, an arrow falling into it.
    "nullcatch": """
        +.+......
        .#.......
        +.#......
        ...*.....
        M...*...M
        m.#kkk#.m
        m#kkkkk#m
        .m#kkk#m.
        ..mmmmm..
    """,
    # Second Bell (time; storm): two bells side by side, a spark leaping between them as they ring.
    "second_bell": """
        ..#...#..
        .#+#.#+#.
        .#+-.#+-.
        #+--z+--#
        #+--Z+--#
        #########
        ..*...*..
    """,
    # Red Ledger (blood; arcane): a ruled ledger page, a drop of blood blotted across its entries.
    "red_ledger": """
        mmmmmmm..
        m.....m..
        mMMMM.m..
        m.....m#.
        mMMM.m#+#
        m....#+*+
        mMM..#***
        m.....###
        mmmmmmm..
    """,
    # Quietus (arcane; time): an hourglass stopped, a spark of spell caught fast in its upper bulb.
    "quietus": """
        ttttttttt
        .t+*+*+t.
        ..t***t..
        ...t*t...
        ....t....
        ...t.t...
        ..t...t..
        .t.....t.
        ttttttttt
    """,
    # Blood Escrow (blood; arcane): a drop of blood held safe inside a shield of arcane light.
    "blood_escrow": """
        .mmmmmmm.
        m...#...m
        m..#+#..m
        m.#+*+#.m
        m.#+++#.m
        .m.###.m.
        ..m...m..
        ...mMm...
    """,
    # Frost Molt (frost; life): a plate of ice peeling off a green leaf, the leaf beneath it clean.
    "frost_molt": """
        ....###..
        ...#+*+#.
        ..#+**+#.
        .l.#+++#.
        lLl.###..
        lLLl.....
        lLLLl....
        .lLl.....
        ..l......
    """,
    # Pulse Ferry (life; time): a heart carried across in a little golden ferry.
    "pulse_ferry": """
        ..##.##..
        .#++#++#.
        .#+***+#.
        ..#+*+#..
        ...#+#...
        t...#...t
        ttttttttt
        .ttTtTtt.
    """,
    # Last Lantern (arcane; time): a lantern holding its light, a hand of time turning it back.
    "last_lantern": """
        ...###...
        ...#.#...
        ..#####..
        T.#+*+#..
        t.#***#..
        t.#+*+#..
        .t#####..
        ..ttt....
    """,
    # Pocket Current (frost; void): a curl of water carrying a stray item down into a pouch of darkness.
    "pocket_current": """
        .++++....
        +....+...
        +.ww..+..
        .+.....+.
        .......+.
        .uuuuuuu.
        .u.....u.
        .uU...Uu.
        ..uuuuu..
    """,
    # Wayline (arcane; void): waypoints strung along a line out to a grapple hooked on the far ground.
    "wayline": """
        ......u.u
        .......u.
        ....+..U.
        ...+*+.u.
        ....+.-..
        .+..-....
        +*+-.....
        .+.......
    """,
    # Night Seam (void; arcane): a seam opened through dark stone, arcane light glinting out of it.
    "night_seam": """
        ####.####
        #+++m#++#
        #+##.M#-#
        ###.m.###
        #+#M.#++#
        #-#.m.#-#
        ###.M.###
    """,
    # Shard Compass (arcane; earth): a compass whose needle points out to a shard of ore.
    "shard_compass": """
        ......e..
        .....eEe.
        ..###.e..
        .#...+...
        #...*..#.
        #..*...#.
        #.-....#.
        .#....#..
        ..####...
    """,
}
