"""Hand-drawn rune icons for the second batch of new runes (merged into item_art.GLYPHS).

Same format as item_art.GLYPHS: a pictogram at most 9 wide and 9 tall, "." empty, "-" shade,
"#" main, "+" light, "*" core (the stone's own glow colours), "o" darkens the stone, "k" near-black,
"w" white, "r"/"R"/"p" blood.
"""

GLYPHS: dict[str, str] = {
    # ---------------------------------------------------------------- shapes
    # Glaive: two curved blades round a bright hub, caught mid-spin.
    "glaive": """
        ..###....
        .#+++#...
        #+#..##..
        .#....#..
        ...*+*...
        ..#....#.
        ..##..#+#
        ...#+++#.
        ....###..
    """,
    # Imprint: a print pressed into a ring on the ground, glowing where it will burst.
    "imprint": """
        ..#####..
        .#.....#.
        #.+.+.+.#
        #.*.*.*.#
        #.......#
        #..+*+..#
        #.+***+.#
        .#.+*+.#.
        ..#####..
    """,
    # Latch: a thread of light running up to a clasp closed round its catch.
    "latch": """
        .....###.
        ....#+*+#
        ....#*.*#
        ...-#+*+#
        ..-..###.
        .-.......
        *........
    """,
    # ---------------------------------------------------------------- modifiers
    # Kindred: two figures, joined by a line of light.
    "kindred": """
        .#.....#.
        #*#...#*#
        .#.....#.
        ###+++###
        .#.....#.
        .#.....#.
        #.#...#.#
    """,
    # Thirst: a drop of blood falling into a cup.
    "thirst": """
        ....R....
        ...RpR...
        ...RRR...
        ....r....
        .#.....#.
        .#+...+#.
        ..#+++#..
        ...###...
    """,
    # Belated: an hourglass whose last grain hangs, not yet fallen, with the trail of its wait.
    "belated": """
        #######..
        .#+++#...
        ..#+#....
        ...*...+.
        ..#.#...+
        .#.*.#.+.
        #######..
    """,
    # ---------------------------------------------------------------- links
    # On Reaction: two strands meeting in a burst.
    "on_reaction": """
        #...+...#
        .#..*..#.
        ..#.*.#..
        ...***...
        +**.*.**+
        ...***...
        ..#.*.#..
        .#..*..#.
        #...+...#
    """,
    # On Weakness: a ring split by a jagged crack, lit where it gave.
    "on_weakness": """
        ..##.##..
        .#..#..#.
        #...#...#
        #..#....#
        #...*...#
        #....#..#
        .#..#..#.
        ..##.##..
    """,
    # ---------------------------------------------------------------- effects
    # Spellbrand: a seal of four points round a waiting spark.
    "spellbrand": """
        ...#.#...
        ..#+*+#..
        .#+...+#.
        #+.*.*.+#
        .*..#..*.
        #+.*.*.+#
        .#+...+#.
        ..#+*+#..
        ...#.#...
    """,
    # Gash: a long ragged cut, still dripping.
    "gash": """
        .......#.
        ......#+.
        .....#*..
        ....#*...
        ...#*....
        ..#+..R..
        .#+...R..
        #-...RpR.
        .....RRR.
    """,
    # Prospect: a nugget of ore with the ground's ring running out round it.
    "prospect": """
        ....+....
        ..+...+..
        .+.###.+.
        +.#*+*#.+
        ..#+*+#..
        +.#*+*#.+
        .+.###.+.
        ..+...+..
        ....+....
    """,
    # Searing Edge: a blade with flames licking up it.
    "searing_edge": """
        .......#.
        ......#*#
        .....#*#.
        ..+.#*#..
        .+*#*#...
        ..#*#+...
        .#-#.....
        #-#......
        .#.......
    """,
    # Flash Freeze: a drop of water freezing solid from its heart outward.
    "flash_freeze": """
        ....#....
        ...#+#...
        ..#+*+#..
        .#+*w*+#.
        .#*www*#.
        .#+*w*+#.
        ..#+*+#..
        ...###...
    """,
    # Drowse: a sleepy "z" over a nodding flower.
    "drowse": """
        ....####.
        ......#..
        .....#...
        ....####.
        .+.+.....
        ..*......
        .+#+.....
        ..#......
        ..#......
    """,
    # Galvanize: a bolt coming down onto a block, which lights up.
    "galvanize": """
        ....##...
        ...##....
        ..####...
        ...##....
        ..##.....
        .#######.
        .#+++++#.
        .#+*+*+#.
        .#######.
    """,
    # Prolong: a clock whose hand stretches out past its rim.
    "prolong": """
        ..###....
        .#.+.#...
        #..*..#..
        #..*+++++
        #.....#.+
        .#...#...
        ..###....
    """,
    # Umbra: a mouth of darkness closing on a last point of light.
    "umbra": """
        ..####...
        .#kkkk#..
        #kk..kk#.
        #k....+..
        #k...+*+.
        #k....+..
        #kk..kk#.
        .#kkkk#..
        ..####...
    """,
    # Disarm: a sword tumbling up out of a gust.
    "disarm": """
        ......#..
        .....#*#.
        ....#*#..
        .+.#*#...
        +.#-#....
        .+.#.....
        .##..##..
        #..##..#.
    """,
}
