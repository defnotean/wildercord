"""Hand-drawn rune icons for the hearth pack's links and modifiers (merged into item_art.GLYPHS).

Same format as item_art.GLYPHS: a pictogram at most 9 wide and 9 tall, "." empty, "-" shade,
"#" main, "+" light, "*" core, "o" darkens the stone. Every row of one pictogram is the same width.
"""

GLYPHS: dict[str, str] = {
    # ---------------------------------------------------------------- gathering modifiers
    # Tidy: a pack with its flap open and a block dropping in.
    "tidy": """
        ...+...
        ...*...
        .#####.
        #+...+#
        #.....#
        #.....#
        .#####.
    """,
    # Replanting: a sprout rising from a seed in a furrow.
    "replanting": """
        ..+.+..
        ...#...
        ..+#+..
        ...#...
        ...*...
        -------
        #######
    """,
    # Kilned: a lump of ore over a flame, turned into an ingot.
    "kilned": """
        .#####.
        #+++++#
        .#####.
        .......
        ..+.+..
        .+*#*+.
        ..###..
    """,
    # Silken: a whole block wrapped in a thread.
    "silken": """
        +......
        .+#####
        .#+...#
        .#.+..#
        .#..+.#
        .#...+#
        .#####+
    """,
    # Windfall: three gems tumbling out of a broken block.
    "windfall": """
        .+...+.
        .*...*.
        ...+...
        ...*...
        #.....#
        ##...##
        #######
    """,
    # Veinfollow: a crooked vein of ore running through stone.
    "veinfollow": """
        *......
        .*.....
        ..**...
        ....*..
        ....*..
        .....**
        .......
    """,
    # Timbering: a felled trunk with its rings showing.
    "timbering": """
        ...#...
        ..###..
        .##+##.
        ..#*#..
        ..###..
        ..###..
        .#####.
    """,
    # Level Ground: a flat line the arrow will not pass below.
    "level_ground": """
        ...+...
        ...#...
        .+.#.+.
        ..###..
        ...*...
        #######
        -------
    """,
    # Steady: a block held still by two braces.
    "steady": """
        #.....#
        #.###.#
        ##+++##
        #.+*+.#
        ##+++##
        #.###.#
        #.....#
    """,
    # Damp: a raindrop over a dying flame.
    "damp": """
        ...+...
        ..+*+..
        ..+++..
        .......
        ..-.-..
        .-#-#-.
        .-----.
    """,
    # Magnetic: a horseshoe magnet drawing in a mote.
    "magnetic": """
        .##.##.
        .##.##.
        .#...#.
        .#.*.#.
        .#...#.
        ..###..
        .......
    """,
    # Sowing: a hand scattering seeds over furrows.
    "sowing": """
        ..###..
        .#+++#.
        ..*.*..
        .*...*.
        .......
        -------
        #######
    """,
    # Furrowing: a hoe drawing parallel furrows.
    "furrowing": """
        #####..
        ...#...
        ...#...
        ...#...
        #.#.#.#
        -#-#-#-
        #######
    """,
    # Fertile: a crop shooting up one stage, a spark at its head.
    "fertile": """
        ...*...
        ..+#+..
        .+.#.+.
        ...#...
        .+.#.+.
        ...#...
        #######
    """,
    # Torchset: a torch with a bright flame.
    "torchset": """
        ...+...
        ..+*+..
        ...*...
        ...#...
        ...#...
        ...#...
        ...#...
    """,
    # Ore Sensing: an eye over three glints in the dark.
    "ore_sensing": """
        ..###..
        .#+*+#.
        ..###..
        .......
        *.....*
        ...*...
        ooooooo
    """,
    # Fetching: a dropped bone drawn back along an arrow.
    "fetching": """
        +.....+
        .#####.
        +.....+
        .......
        ..+....
        .###*##
        ..+....
    """,
    # ---------------------------------------------------------------- power and reach
    # Bountiful: a cluster of experience orbs.
    "bountiful": """
        ...+...
        ..+*+..
        ...+...
        .+...+.
        +*+.+*+
        .+...+.
        .......
    """,
    # Culling: a fang over a sickle's curve.
    "culling": """
        ..###..
        .#...#.
        #......
        #...*..
        #..*#..
        .#.....
        ..###..
    """,
    # Headhunting: a crosshair on the tallest of three marks.
    "headhunting": """
        ...+...
        ..#*#..
        .+#*#+.
        ..#*#..
        ...+...
        #.....#
        #.....#
    """,
    # Hallowed: a halo over a skull's crown.
    "hallowed": """
        .+++++.
        .......
        ..###..
        .#*.*#.
        .#####.
        ..#.#..
        .......
    """,
    # Tapering: bars stepping down from left to right.
    "tapering": """
        *......
        #......
        ##.....
        ###....
        ####...
        #####..
        ######.
    """,
    # Pooled: one drop splitting into equal bowls.
    "pooled": """
        ...*...
        ...#...
        ..#.#..
        .#...#.
        .......
        +#+.+#+
        .#...#.
    """,
    # Sunlit: a sun with long rays.
    "sunlit": """
        +..+..+
        .+.#.+.
        ..###..
        +##*##+
        ..###..
        .+.#.+.
        +..+..+
    """,
    # Gentle: a hand held flat over a small creature.
    "gentle": """
        #######
        .......
        .......
        ..##...
        .#*##..
        .####..
        .#..#..
    """,
    # Sparing: two figures, a line kept between them.
    "sparing": """
        .#...#.
        ###.###
        .#.+.#.
        .#.+.#.
        ##.+.##
        ...+...
        .......
    """,
    # Soothing: a calm wave under a resting note.
    "soothing": """
        ...##..
        ...#.#.
        ...#...
        .###...
        .##....
        .......
        +-+-+-+
    """,
    # Cushioned: a feather drifting onto a soft pad.
    "cushioned": """
        ....+#.
        ...+#..
        ..+#...
        .+#....
        .......
        .+++++.
        #######
    """,
    # ---------------------------------------------------------------- tending modifiers
    # Mending: a pick with a bright stitch along its handle.
    "mending": """
        .#####.
        #..#..#
        ...+...
        ...#...
        ...+...
        ...#...
        ...*...
    """,
    # Nourishing: a drumstick.
    "nourishing": """
        ..###..
        .#+++#.
        .#+*+#.
        ..###..
        ...#...
        ..#.#..
        .......
    """,
    # Purifying: a bottle with the murk lifting out of it.
    "purifying": """
        .-.-.-.
        ..-.-..
        ..###..
        ...#...
        ..#+#..
        .#+*+#.
        .#####.
    """,
    # Matchmaking: two hearts side by side.
    "matchmaking": """
        .......
        ##.##..
        #*#*#..
        .###.##
        ..#.#*#
        ....###
        .....#.
    """,
    # Fleecing: shears over a tuft of wool.
    "fleecing": """
        #.....#
        .#...#.
        ..#.#..
        ...*...
        .+++++.
        +++++++
        .+++++.
    """,
    # Inward: arrows folding in to one point.
    "inward": """
        #.....#
        .#...#.
        ..+.+..
        ...*...
        ..+.+..
        .#...#.
        #.....#
    """,
    # Selfless: arrows leaving an empty centre.
    "selfless": """
        +.....+
        .#...#.
        ..#.#..
        ...o...
        ..#.#..
        .#...#.
        +.....+
    """,
    # Triage: a cross over a falling bar.
    "triage": """
        ..###..
        ..#*#..
        #######
        #**+**#
        #######
        ..#*#..
        ..###..
    """,
    # ---------------------------------------------------------------- conditions
    # If Night: a crescent moon and a star.
    "if_night": """
        ..###..
        .##....
        ##...+.
        ##..+*+
        ##...+.
        .##....
        ..###..
    """,
    # If Day: a sun over the horizon.
    "if_day": """
        .......
        ..+++..
        .+***+.
        +*****+
        #######
        -------
        .......
    """,
    # If Raining: a cloud dropping rain.
    "if_raining": """
        ..###..
        .#####.
        #######
        .......
        .+.+.+.
        +.+.+..
        .+.+.+.
    """,
    # If Underground: a figure under a roof of stone.
    "if_underground": """
        #######
        ooooooo
        .......
        ...*...
        ..###..
        ...#...
        ..#.#..
    """,
    # If Alone: one figure in an empty ring.
    "if_alone": """
        ..###..
        .#...#.
        #..*..#
        #.###.#
        #..#..#
        .#...#.
        ..###..
    """,
    # If Near Ally: two figures, hand in hand.
    "if_near_ally": """
        .*...*.
        ###.###
        .#+++#.
        .#...#.
        ##...##
        .......
        .......
    """,
    # If Unhurt: a whole heart.
    "if_unhurt": """
        .......
        .##.##.
        #++#++#
        #+***+#
        .#***#.
        ..#*#..
        ...#...
    """,
    # If Holding Tool: a pickaxe in a hand.
    "if_holding_tool": """
        .#####.
        #..#..#
        ...#...
        ...#...
        ..***..
        ..#*#..
        ...#...
    """,
    # If Brimming: a flask full past halfway.
    "if_brimming": """
        ...#...
        ..#.#..
        .#...#.
        #-----#
        #+***+#
        #+***+#
        .#####.
    """,
    # If In Fields: three rows of crops.
    "if_in_fields": """
        +.+.+.+
        #.#.#.#
        #.#.#.#
        *.*.*.*
        #.#.#.#
        -------
        #######
    """,
    # ---------------------------------------------------------------- watchers
    # On Mine: a pick striking a block, a spark flying.
    "on_mine": """
        ###...+
        ..#..+.
        ..##...
        ..#.##.
        ....#*#
        ....###
        .......
    """,
    # On Harvest: a ripe ear of wheat and a sickle.
    "on_harvest": """
        ...*...
        ..*#*..
        ..*#*..
        ...#...
        #..#...
        .#.#...
        ..###..
    """,
    # On Catch: a hook with a fish on it.
    "on_catch": """
        ...#...
        ...#...
        ...#...
        .#.#...
        .#+#...
        ..##*#.
        ....###
    """,
    # On Sprint: a running figure with speed lines.
    "on_sprint": """
        ....*..
        ...###.
        +-..#..
        ...###.
        +-.#.#.
        ..#...#
        .......
    """,
    # On Splash: a drop striking water, a crown of spray.
    "on_splash": """
        ...*...
        ...+...
        .+...+.
        +.+.+.+
        .......
        #######
        -------
    """,
    # On Mount: a saddle on a horse's back.
    "on_mount": """
        .....##
        ..*..#.
        .###.#.
        #######
        #######
        .#...#.
        .#...#.
    """,
    # On Wake: a bed with a sun peeking over it.
    "on_wake": """
        ....+*+
        .....+.
        .......
        #......
        #++####
        #######
        #.....#
    """,
}
