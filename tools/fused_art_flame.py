"""Hand-drawn rune icons for the fused effects of flame and stone (merged into item_art.GLYPHS).

Same format as item_art.GLYPHS: a pictogram at most 9 wide and 9 tall, "." empty, "-" shade,
"#" main, "+" light, "*" core (the rune's element colours), "o" darkens the stone.
"""

GLYPHS: dict[str, str] = {
    # A phoenix rising, wings swept up high, a bright head and body, its tail feathers split.
    "phoenix_pyre": """
        #.......#
        ##.....##
        ###.*.###
        +###*###+
        .+#***#+.
        ..+#*#+..
        ...#*#...
        ..#-.-#..
        .#.....#.
    """,
    # Black fire: flames licking up out of a dark pit's mouth.
    "hellmouth": """
        ..+...+..
        .+#.+.#+.
        .#+#*#+#.
        ..#*+*#..
        .#kkkkk#.
        #kkkkkkk#
        #-kkkkk-#
        .##kkk##.
        ...###...
    """,
    # A star of flame, and two of its motes flying off.
    "starfire": """
        +...*....
        ...+*+...
        .#++*++#.
        ..#***#..
        ..+***+..
        .#+#.#+#.
        .#.....#.
        ......+..
        .....+*+.
    """,
    # A flame in a clock ring whose arrow brings it round again.
    "everburn": """
        ..#####..
        .#.....#.
        #...+...#
        #..+#*..#
        #.+***#.#
        #..#*#..#
        .#.....+.
        ..###.***
        .......+.
    """,
    # Blood on the boil: a drop full of bubbles, more rising off it.
    "bloodboil": """
        .+...+...
        ...+...+.
        ....#....
        ...#+#...
        ..#+*+#..
        .#+.+.+#.
        .#*+.+*#.
        .#+*+*+#.
        ..#####..
    """,
    # One great flame, and fire leaping from it to catch two more.
    "conflagration": """
        ....+....
        ...+#-...
        ..-#*#.-.
        .-.#*#..-
        +..***..+
        #+.#*#.+#
        #*#...#*#
        .#.....#.
    """,
    # A standing column of stone in drums, capital and plinth, something thrown off its top.
    "monolith": """
        ....*....
        ...+*+...
        .#######.
        ..#+++#..
        ..#---#..
        ..#+++#..
        ..#---#..
        ..#+++#..
        .#######.
    """,
    # A horseshoe magnet, a spark crackling between its poles.
    "magnetize": """
        ..+.+.+..
        .*.+.+.*.
        .*#...#*.
        .##...##.
        .##...##.
        .##...##.
        .###.###.
        ..#####..
        ...###...
    """,
    # The ground caving into a funnel of darkness, everything drawn in.
    "sinkhole": """
        .+.....+.
        ..-...-..
        #########
        #kkkkkkk#
        .#kkkkk#.
        ..#kkk#..
        ...#k#...
        ....#....
    """,
}
