"""Hand-drawn rune icons for the fused effects of storm and wind (merged into item_art.GLYPHS).

Same format as item_art.GLYPHS: a pictogram at most 9 wide and 9 tall, "." empty, "-" shade,
"#" main, "+" light, "*" core (the rune's element colours), "o" darkens the stone.
"""

GLYPHS: dict[str, str] = {
    # A black bolt: lightning drawn in darkness, only its edges lit, a spark where it lands.
    "riftbolt": """
        ....####.
        ...#kk#..
        ..#kk#...
        .#kkk####
        .####kk#.
        ...#kk#..
        ..#kk#...
        .#k#.....
        .*#......
    """,
    # A woven net, four knots of light caught in it.
    "stormweave": """
        #...#...#
        .#.#.#.#.
        ..*...*..
        .#.#.#.#.
        #...#...#
        .#.#.#.#.
        ..*...*..
        .#.#.#.#.
        #...#...#
    """,
    # A clock face whose hand is a bolt of lightning, breaking out through the rim.
    "stormclock": """
        ..#####..
        .#..+..#.
        #...+...#
        #+..*..+#
        #...+#..#
        .#...+##.
        ..###+#..
        .....#+..
        ....#+...
    """,
    # A heart split by a bolt of lightning: it stops.
    "heartstopper": """
        .##...##.
        #++#.#+*#
        #+++k++*#
        #++++k++#
        .#++k++#.
        ..#++k#..
        ...#k#...
        ....#....
    """,
    # A dark thundercloud throwing one great bolt down.
    "thunderhead": """
        ..##.##..
        .#--#--#.
        #-------#
        #-------#
        .#######.
        ...+**...
        ..+****..
        ....**...
        ...**....
    """,
    # Wind driving down onto the ground, which cracks under it.
    "downdraft": """
        ...#+#...
        ...#+#...
        ...#+#...
        .###+###.
        ..#+*+#..
        ...#*#...
        +...#...+
        #########
        -#o#-#o#-
    """,
    # Air rising off the ground, gust over gust.
    "updraft": """
        ....*....
        ...*+*...
        ..*+.+*..
        .#.....#.
        ...#+#...
        ..#+.+#..
        .#.....#.
        .........
        #########
    """,
    # A glyph on the ground, and the arc of what it throws skyward.
    "skyglyph": """
        .....+**.
        ......+*.
        .....#.+.
        ....#....
        ...#.....
        .#######.
        #+.***.+#
        .#######.
    """,
    # Thrown out, and snapped back home.
    "recoil": """
        *######..
        .......#.
        ........#
        ..#.....#
        .#*....#.
        #*+#####.
        .#*......
        ..#......
    """,
}
