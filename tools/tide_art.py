"""The fishing, water and coast pack (fx-fish): rune icons, recipes and message text.

Glyphs use item_art.GLYPHS' format: at most 9 wide and 9 tall, "." empty, "-" shade, "#" main,
"+" light, "*" core, "o" darkens the stone, "k" near-black, "w" white.
Ocean's Favor is Tier IV: found only, so it has no recipe.
"""

GLYPHS: dict[str, str] = {
    # A hook with a wriggling lure.
    "angler_lure": """
        ....#....
        ....#....
        ....#....
        ..*.#....
        .*+*#..#.
        ..*.#..#.
        .....##..
    """,
    # A four-leaf clover over a hook.
    "bait_blessing": """
        ..+.+....
        .+*+*+...
        ..+.+....
        ....#....
        ....#..#.
        .....##..
    """,
    # A reel with its line wound in.
    "reeling_tide": """
        ..###....
        .#+*+#...
        .#*.*#---
        .#+*+#...
        ..###....
    """,
    # Three little fish, one bright.
    "school_sight": """
        .#.......
        ###+.....
        .#...*...
        ....***+.
        .#...*...
        ###+.....
        .#.......
    """,
    # A rod with a small cross of mending.
    "tackle_mend": """
        .......#.
        ......#..
        .+...#...
        +*+.#....
        .+.#.....
        ..#......
        .#.......
    """,
    # A bell over a bobber.
    "bobber_bell": """
        ...###...
        ..#+++#..
        ..#+++#..
        .#######.
        ....*....
        ...-#-...
        ...-#-...
    """,
    # An eye over a ripple.
    "water_reading": """
        ..#####..
        .#+.*.+#.
        ..#####..
        .........
        -+-.-+-..
        ..-+-.-+-
    """,
    # A leaping dolphin.
    "dolphin_call": """
        ....###..
        ..##+++#.
        .#++*..#.
        #+#......
        ##.......
        .--.-.--.
    """,
    # An axolotl with frilled gills.
    "axolotl_kinship": """
        +.+......
        .###.....
        +#*##....
        .####+#..
        ...#.#.##
    """,
    # Fish swimming in to a point.
    "shoal_herd": """
        #.......#
        .#.....#.
        ..#.*.#..
        ...***...
        ..#.*.#..
        .#.....#.
        #.......#
    """,
    # A fish over a wave, set back.
    "refloat": """
        ...#.....
        ..###+...
        ...#.....
        ....*....
        .-+-+-+-.
        -+-+-+-+-
    """,
    # A blade through reeds.
    "reed_cut": """
        .#..#..#.
        .#..#..#.
        *+*+*+*+*
        .#..#..#.
        .#..#..#.
        ##.##.##.
    """,
    # A twisted cloth dripping.
    "wring": """
        ##.......
        .##+.....
        ..+##+...
        ....+##..
        .......##
        .*...*...
        ...*.....
    """,
    # A bucket with water rising.
    "spring_draw": """
        ..*+*+*..
        .#+*+*+#.
        .#+*+*+#.
        ..#+*+#..
        ..#####..
    """,
    # A cauldron brimming over.
    "brimming": """
        .+*+*+*+.
        #+*+*+*+#
        #.......#
        .#.....#.
        ..#####..
        ..#...#..
    """,
    # A crowned wave.
    "oceans_favor": """
        .*.*.*.*.
        .*******.
        .........
        ..###....
        .#+++#...
        #+...+#.#
        .......#.
    """,
    # A bell of glass round an air bubble.
    "diving_bell": """
        ...###...
        ..#...#..
        .#..*..#.
        .#.*w*.#.
        .#..*..#.
        .#######.
    """,
    # A lantern under waves.
    "tide_lantern": """
        -+-+-+-+-
        ....#....
        ...###...
        ..#*w*#..
        ..#***#..
        ...###...
    """,
    # Water poured on a flame.
    "sluice": """
        #######..
        ......#..
        ......*..
        .....*.*.
        ....-+#+-
        ....+###+
    """,
    # A block with drops soaking in.
    "soak_through": """
        ..*...*..
        ....*....
        #########
        #+-+-+-+#
        #-+-+-+-#
        #########
    """,
    # A cloud with rain.
    "rain_cloud": """
        ...###...
        .##+++##.
        #+++++++#
        .#######.
        ..*.*.*..
        .*.*.*...
    """,
    # A glass with a storm inside.
    "storm_glass": """
        ...###...
        ...#.#...
        ..#.*.#..
        ..#*+*#..
        ..#.*.#..
        ..#####..
    """,
    # Kelp swaying in a song.
    "kelpsong": """
        ..#...#..
        .#...#...
        ..#...#..
        ...#...#.
        ..#...#..
        .*.*.*.*.
    """,
    # A branch of coral, half bright.
    "coral_mend": """
        #...#...#
        .#..#..#.
        ..#.*.*..
        ...***...
        ....*....
        ....*....
    """,
    # Turtle eggs in sand.
    "nest_tend": """
        ..#...#..
        .#+#.#+#.
        .#*#.#*#.
        ..#.#.#..
        ...#+#...
        ---#*#---
    """,
    # Lily pads stepping across.
    "lily_path": """
        .......##
        ......#+#
        ....##...
        ...#+#...
        .##......
        #+#......
    """,
    # A path of sand above the water.
    "sandbar": """
        .........
        .#######.
        #+++++++#
        -+-+-+-+-
        +-+-+-+-+
    """,
    # A drill through ice.
    "ice_auger": """
        ...###...
        ....#....
        ++++#++++
        ++++*++++
        ....*....
        -+-+-+-+-
    """,
    # A buoy on the waves.
    "tide_marker": """
        ....*....
        ...*w*...
        ....#....
        ...###...
        ..#+++#..
        -+-+-+-+-
    """,
    # An arrow pointing to land.
    "shore_sense": """
        .....#...
        ......#..
        #######*.
        ......#..
        .....#...
        -+-+-.###
    """,
    # A line dropped to the bottom.
    "fathom": """
        -+-+-+-+-
        ....#....
        ....#....
        ....#....
        ....*....
        #########
    """,
    # A sunken chest shining.
    "wreck_sense": """
        .*.....*.
        ..#####..
        .#+++++#.
        .###*###.
        .#+++++#.
        .#######.
    """,
    # A net drawing things in.
    "drift_net": """
        #.#.#.#.#
        .#.#.#.#.
        #.#.#.#.#
        .#.#*#.#.
        ..#*w*#..
        ...###...
    """,
    # A boat on a rope.
    "mooring_call": """
        *........
        .*.......
        ..*......
        ...*.....
        #...*...#
        .#######.
        -+-+-+-+-
    """,
    # A filled sail.
    "fair_wind": """
        ....#....
        ....#+...
        ....#++..
        ....#+++.
        ....#....
        .#######.
        ..#####..
    """,
    # Bubbles rising.
    "upwell": """
        ....*....
        ...*.*...
        ....#....
        ..+.#.+..
        .+..#..+.
        ....#....
    """,
    # A plumb diving down.
    "sounding": """
        ....#....
        .+..#..+.
        ..+.#.+..
        ....#....
        ...*.*...
        ....*....
    """,
    # A curved leap over a wave.
    "porpoise": """
        ..###....
        .#...#...
        #.....#..
        *......#.
        .......*.
        -+-+-+-+-
    """,
    # A foot on the water.
    "skimstep": """
        ...##....
        ...##....
        ..###....
        .####+...
        #########
        -+-+-+-+-
    """,
    # A skate blade.
    "skaters_edge": """
        ..##.....
        ..###....
        ..#####..
        .#######.
        .........
        ++++++++*
    """,
    # A bubble with air.
    "air_pocket": """
        ...###...
        ..#+++#..
        .#+w+.+#.
        .#+++.+#.
        ..#+++#..
        ...###...
    """,
    # A shield of bubbles.
    "drown_ward": """
        .#######.
        .#..*..#.
        .#.*.*.#.
        .#..*..#.
        ..#...#..
        ...###...
    """,
    # A pearl in an open shell.
    "pearl_sight": """
        .#######.
        ..#...#..
        ...*w*...
        ..#***#..
        .#######.
    """,
    # Wind lines over a wave.
    "sea_breeze": """
        ######...
        ......#..
        ####+....
        .........
        -+-+-+-+-
    """,
    # A cloud of ink.
    "inkveil": """
        ..kk.kk..
        .kkkkkkk.
        kk-kkk-kk
        .kkkkkkk.
        ..k.k.k..
    """,
    # A turtle shell.
    "shellback": """
        ...###...
        ..#+#+#..
        .#+#*#+#.
        .#######.
        ##.....##
    """,
    # Dew drops falling into a bottle.
    "dewcatch": """
        .*...*...
        ...*.....
        ...#.#...
        ...#.#...
        ..#+++#..
        ..#***#..
        ...###...
    """,
    # A pickaxe with bubbles.
    "divers_hands": """
        .#####...
        #....#...
        ....#.#..
        ...#...*.
        ..#...*..
        .#.....*.
    """,
}

# Tier I-III recipes: the themed items besides the blank rune (tier catalysts are added on top).
RECIPES: dict[str, list[str]] = {
    "angler_lure": ["minecraft:string", "minecraft:cod"],
    "bait_blessing": ["minecraft:rabbit_foot", "minecraft:salmon"],
    "reeling_tide": ["minecraft:string", "minecraft:stick", "minecraft:kelp"],
    "school_sight": ["minecraft:tropical_fish", "minecraft:glow_ink_sac"],
    "tackle_mend": ["minecraft:string", "minecraft:string", "minecraft:bone_meal"],
    "bobber_bell": ["minecraft:string", "minecraft:gold_nugget", "minecraft:feather"],
    "water_reading": ["minecraft:prismarine_crystals", "minecraft:paper"],
    "dolphin_call": ["minecraft:cod", "minecraft:nautilus_shell"],
    "axolotl_kinship": ["minecraft:tropical_fish", "minecraft:clay_ball"],
    "shoal_herd": ["minecraft:kelp", "minecraft:cod", "minecraft:salmon"],
    "refloat": ["minecraft:kelp", "minecraft:lily_pad"],
    "reed_cut": ["minecraft:sugar_cane", "minecraft:flint"],
    "wring": ["minecraft:sponge"],
    "spring_draw": ["minecraft:bucket", "minecraft:kelp"],
    "brimming": ["minecraft:cauldron", "minecraft:kelp"],
    "diving_bell": ["minecraft:glass", "minecraft:glass", "minecraft:heart_of_the_sea"],
    "tide_lantern": ["minecraft:sea_pickle", "minecraft:glowstone_dust"],
    "sluice": ["minecraft:kelp", "minecraft:clay_ball", "minecraft:charcoal"],
    "soak_through": ["minecraft:clay_ball", "minecraft:sand"],
    "rain_cloud": ["minecraft:white_wool", "minecraft:kelp"],
    "storm_glass": ["minecraft:glass_bottle", "minecraft:copper_ingot"],
    "kelpsong": ["minecraft:kelp", "minecraft:bone_meal"],
    "coral_mend": ["minecraft:bone_meal", "minecraft:prismarine_crystals"],
    "nest_tend": ["minecraft:seagrass", "minecraft:sand"],
    "lily_path": ["minecraft:lily_pad", "minecraft:lily_pad"],
    "sandbar": ["minecraft:sandstone", "minecraft:sand"],
    "ice_auger": ["minecraft:ice", "minecraft:flint"],
    "tide_marker": ["minecraft:sea_pickle", "minecraft:string"],
    "shore_sense": ["minecraft:compass", "minecraft:sand"],
    "fathom": ["minecraft:string", "minecraft:iron_nugget"],
    "wreck_sense": ["minecraft:prismarine_shard", "minecraft:spyglass"],
    "drift_net": ["minecraft:string", "minecraft:string", "minecraft:kelp"],
    "mooring_call": ["minecraft:oak_boat", "minecraft:lead"],
    "fair_wind": ["minecraft:white_wool", "minecraft:feather"],
    "upwell": ["minecraft:soul_sand", "minecraft:kelp"],
    "sounding": ["minecraft:magma_block", "minecraft:kelp"],
    "porpoise": ["minecraft:cod", "minecraft:feather"],
    "skimstep": ["minecraft:lily_pad", "minecraft:feather"],
    "skaters_edge": ["minecraft:ice", "minecraft:sugar"],
    "air_pocket": ["minecraft:glass_bottle", "minecraft:kelp"],
    "drown_ward": ["minecraft:pufferfish", "minecraft:glass_bottle"],
    "pearl_sight": ["minecraft:prismarine_crystals", "minecraft:nautilus_shell"],
    "sea_breeze": ["minecraft:feather", "minecraft:milk_bucket"],
    "inkveil": ["minecraft:ink_sac", "minecraft:ink_sac"],
    "shellback": ["minecraft:turtle_scute", "minecraft:kelp"],
    "dewcatch": ["minecraft:glass_bottle", "minecraft:fern"],
    "divers_hands": ["minecraft:prismarine_shard", "minecraft:iron_pickaxe"],
}

# Messages the pack's runes send.
LANG: dict[str, str] = {
    "message.wildercord.tide.no_bobber": "Cast a fishing line first",
    "message.wildercord.tide.lured": "A fish draws near",
    "message.wildercord.tide.open_water": "Open water: treasure can bite here",
    "message.wildercord.tide.closed_water": "Not open water: no treasure here",
    "message.wildercord.tide.rain_helps": "Rain is speeding up bites",
    "message.wildercord.tide.no_rain": "No rain on the water",
    "message.wildercord.tide.depth": "Water depth: %s blocks",
    "message.wildercord.tide.no_water": "No water there",
    "message.wildercord.tide.shore": "Dry land %s blocks away",
    "message.wildercord.tide.no_shore": "No dry land within 48 blocks",
    "message.wildercord.tide.marker": "Buoy set at %s, %s, %s",
    "message.wildercord.tide.rain_ends": "Rain ends in about %s min",
    "message.wildercord.tide.rain_starts": "Rain comes in about %s min",
    "message.wildercord.tide.no_weather": "The weather here never turns",
    "message.wildercord.tide.not_in_water": "You need to be in water",
    "message.wildercord.tide.not_in_boat": "You need to be in a boat",
    "message.wildercord.tide.nothing": "Nothing here to work on",
    "message.wildercord.tide.no_rod": "Hold your fishing rod",
}
