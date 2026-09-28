"""Hand-drawn rune icons for the fused effects of life and blood (merged into item_art.GLYPHS).

Same format as item_art.GLYPHS: a pictogram at most 9 wide and 9 tall, "." empty, "-" shade,
"#" main, "+" light, "*" core (the rune's element colours), "o" darkens the stone.
"""

GLYPHS: dict[str, str] = {
    # two gusts curling on the breeze, blossoms carried along with them
    "zephyr": """
        .....###.
        ....#...#
        ........#
        .#######.
        .........
        #####..+.
        .....#+*+
        .+....#+.
        +*+.##...
    """,
    # bands of haze rolling over, drops of blood falling out of it
    "crimson_mist": """
        ..##..##.
        .#..##..#
        .........
        #..##..##
        .##..##..
        .........
        ..#...#..
        .#*#.#*#.
        .###.###.
    """,
    # two souls, and the twisting tether between them
    "soulbond": """
        .###.....
        #+*+#....
        .###+....
        ....#+...
        ...+.#...
        ....#+...
        .....+###
        ....#+*+#
        .....###.
    """,
    # an hourglass, its last grains of life settled at the bottom
    "second_wind": """
        #########
        .#.....#.
        ..#...#..
        ...#+#...
        ....*....
        ...#+#...
        ..#+*+#..
        .#+***+#.
        #########
    """,
    # a drained heart pouring itself into a full one
    "transfusion": """
        ##.##....
        #---#....
        .#-#.+...
        ..#...+..
        .......+.
        ...##.##+
        ..#*+#+*#
        ...#+++#.
        ....#+#..
    """,
    # a five-petalled blossom opening, seen from above
    "lifebloom": """
        ....#....
        ...#*#...
        .#.#+#.#.
        #*#+*+#*#
        .##***##.
        ..#+*+#..
        .#*#.#*#.
        ..#...#..
    """,
    # spurs of bone bursting from cracked ground, blood on their points
    "bonespur": """
        ..r......
        ..*...R..
        .#*...*..
        .#+..#*.r
        .#+..#+.*
        #+*..#+#+
        #++#.#+#+
        #########
        -..-.-..-
    """,
    # a blood sigil, a lance of it driven into its heart
    "sanguine_rite": """
        .......+*
        ..####+*+
        .#....*+.
        #....*..#
        #..#*#..#
        #.#***#.#
        #..###..#
        .#.....#.
        ..#####..
    """,
}
