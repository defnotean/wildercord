"""Hand-drawn rune icons for the fused effects of void, arcane and time (merged into item_art.GLYPHS).

Same format as item_art.GLYPHS: a pictogram at most 9 wide and 9 tall, "." empty, "-" shade,
"#" main, "+" light, "*" core (the rune's element colours), "o" darkens the stone.
"""

GLYPHS: dict[str, str] = {
    # Entropy: a clock face coming apart, its right side fraying away into loose motes.
    "entropy": """
        ..####...
        .#....#..
        #...*..+.
        #...*...-
        #..*...+.
        #.......-
        .#....+..
        ..##-..-.
        ......+..
    """,
    # Devour: a maw of darkness, fangs along its jaws, closing on a drop of blood.
    "devour": """
        .####....
        #kkkk#...
        kkk+k#...
        kkkk+..R.
        kkk#..RpR
        kkkk+.RRR
        kkk+k#...
        #kkkk#...
        .####....
    """,
    # Timesteal: a clock whose long hand reaches out past its rim to a glowing star and draws it in.
    "timesteal": """
        +.+......
        .w.......
        +.+.###..
        ...*...#.
        ..#.*...#
        ..#..*+.#
        ..#.....#
        ...#...#.
        ....###..
    """,
    # Hemomancy: a drop of blood with the runes it paid for circling it.
    "hemomancy": """
        ....R....
        .p..R..p.
        ...RRR...
        ..RRpRR..
        p.RRpRR.p
        ..RRRRR..
        ...RRR...
        .p.....p.
        ....p....
    """,
    # Reckoning: a round seal with a count of wounds in it, four strokes struck through.
    "reckoning": """
        ..#####..
        .#.....#.
        #.+.+.+*#
        #.+.+*+.#
        #.+*+.+.#
        #*+.+.+.#
        .#.....#.
        ..#####..
    """,
    # Singularity: a black hole in its tilted disk of light.
    "singularity": """
        .......++
        .....+*#.
        ...#kk#..
        ..#kkkk#.
        .#kkkkk#.
        .#kkkk#..
        ..#kk#...
        .#*+.....
        ++.......
    """,
    # Prismatic Burst: a rainbow, an arch for every mark, over a burst of light.
    "prismatic_burst": """
        ..*****..
        .*+++++*.
        *+#####+*
        *+#...#+*
        *+#.w.#+*
        *+#www#+*
        *+#.w.#+*
    """,
    # Chronoshift: a cog inside an arrow turning forward round it.
    "chronoshift": """
        ..++++*..
        .+.....*.
        +..#.#...
        +.#####..
        +..#*#..+
        +.#####.+
        +..#.#..+
        .+.....+.
        ..+++++..
    """,
}
