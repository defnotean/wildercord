"""Hand-drawn rune icons for the fused effects of frost (merged into item_art.GLYPHS).

Same format as item_art.GLYPHS: a pictogram at most 9 wide and 9 tall, "." empty, "-" shade,
"#" main, "+" light, "*" core (the rune's element colours), "o" darkens the stone.
"""

GLYPHS: dict[str, str] = {
    # snowflakes driven between two gusts of wind, each curling at its end
    "blizzard": """
        #####....
        .....#.+.
        ..+.#.+*+
        .+*+...+.
        ..+......
        ....#####
        .+.#.....
        +*+.#....
        .+...###.
    """,
    # a lotus of ice opening: a tall middle petal, two either side, a cup of frost under them
    "frostbloom": """
        ....+....
        ...+*+...
        .+.+*+.+.
        +*+#*#+*+
        .+*#*#*+.
        ..#***#..
        ...###...
    """,
    # a shard of black ice cracking through, splinters flying off it
    "black_ice": """
        ....#....
        ...#k#..+
        ..#kkk#..
        ..#kk*#.+
        .#kk*kk#.
        .#k*kkk#.
        .#kk*kk#.
        ..#kkk#..
        ...###...
    """,
    # a six-pointed frost seal, ice at its heart
    "rime_seal": """
        ....+....
        ...+.+...
        +#######+
        .#+...+#.
        ..#.*.#..
        .#+...+#.
        +#######+
        ...+.+...
        ....+....
    """,
    # a clock stopped inside a cocoon of ice
    "cryostasis": """
        ...###...
        ..#+++#..
        .#+.*.+#.
        .#+.*.+#.
        .#+.**+#.
        .#+...+#.
        .#+...+#.
        ..#+++#..
        ...###...
    """,
    # jaws of frost closing on a drop of blood
    "frostbite": """
        .+++++++.
        .#+#+#+#.
        ..#.#.#..
        ....R....
        ...RRR...
        ...RpR...
        ..#.R.#..
        .#+#+#+#.
        .+++++++.
    """,
    # a crown of ice spikes bursting out of the ground, frozen solid
    "absolute_zero": """
        ....*....
        .+..+..+.
        .#..#..#.
        .#.+#+.#.
        +#.#*#.#+
        ##+#*#+##
        #########
    """,
    # an ammonite: a coiled shell turned to stone, its ribs round the rim
    "fossilize": """
        ..#####..
        .#+#+#+#.
        #+.....+#
        ##.###.##
        #+.#*#.+#
        ##.#.#.##
        #+...#.+#
        .#+#+#+#.
        ..#####..
    """,
    # a geode split open: a shell of rock lined with crystal, a cluster of it grown up into the dark hollow
    "geode": """
        ..#####..
        .#-+*+-#.
        #-*kkk*-#
        #+kkkkk+#
        #*kk*kk*#
        #+k*+*k+#
        #-*+*+*-#
        .#-+*+-#.
        ..#####..
    """,
}
