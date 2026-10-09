"""The shapes pack: icons, recipes and Codex category names for the 41 field and kin shapes.

Merged into item_art.GLYPHS (icons) and generate_assets (RUNE_RECIPES, lang). Glyphs use the item_art
format: at most 9 wide and 9 tall, "." empty, "-" shade, "#" main, "+" light, "*" core, "o" darkens the
stone, "k" near-black, "w" white. Recipes list only the themed items: the Blank Rune and the tier's
catalysts are added by generate_assets (Tier II has room for 5 themed items, Tier III for 6).
"""

GLYPHS: dict[str, str] = {
    # ---------------------------------------------------------------- field shapes
    # Furrow: a ploughed row running away from you, a seed glowing in it.
    "furrow": """
        ...#...
        ..#+#..
        ...#...
        ..#*#..
        ...#...
        ..#+#..
        ...#...
    """,
    # Plot: a small square patch with a sprout in its middle.
    "plot": """
        #######
        #.....#
        #..+..#
        #.+*+..
        #..#..#
        #.....#
        #######
    """,
    # Seedbed: rows of seeds in a wide bed.
    "seedbed": """
        #########
        #.+.+.+.#
        #.......#
        #.+.*.+.#
        #.......#
        #.+.+.+.#
        #########
    """,
    # Shaft: a straight hole sunk down, light at its bottom.
    "shaft": """
        ##...##
        .#...#.
        .#...#.
        .#...#.
        .#...#.
        .#.*.#.
        .#####.
    """,
    # Stairwell: steps going down.
    "stairwell": """
        ##.......
        .##......
        ..##.....
        ...##....
        ....##...
        .....##..
        ......#*#
    """,
    # Corridor: a tunnel's mouth, two blocks tall.
    "corridor": """
        #######
        #.....#
        #.---.#
        #.-*-.#
        #.---.#
        #.....#
        #######
    """,
    # Seam: a line of stone struck across, a vein of light through it.
    "seam": """
        -------
        #######
        +*+*+*+
        #######
        -------
    """,
    # Facade: a wall face of blocks, windows lit.
    "facade": """
        #######
        #+#+#+#
        #######
        #+#*#+#
        #######
        #+#+#+#
        #######
    """,
    # Dome: a half-sphere shell over a glowing floor.
    "dome": """
        ..###..
        .#...#.
        #.....#
        #.....#
        #..*..#
        #######
    """,
    # Footing: the ground under your feet, a little figure standing on it.
    "footing": """
        ...#...
        ..###..
        ...#...
        ..#.#..
        #######
        #+*+*+#
        #######
    """,
    # Canopy: a roof held up over you.
    "canopy": """
        #######
        +++*+++
        .#...#.
        .#...#.
        .#...#.
        .#...#.
    """,
    # Shoreline: waves meeting a bank.
    "shoreline": """
        ....###
        ...#+++
        ..#++++
        .-.-.-#
        -.-*-.#
        .-.-.-#
        -.-.-.#
    """,
    # Perimeter: the outline of a square, nothing inside.
    "perimeter": """
        #+#+#+#
        +.....+
        #.....#
        +..*..+
        #.....#
        +.....+
        #+#+#+#
    """,
    # Spire: a tall thin column with a bright tip.
    "spire": """
        ...*...
        ..#+#..
        ...#...
        ...#...
        ...#...
        ...#...
        ..###..
        .#####.
    """,
    # Pit: a square hole, deeper in its middle.
    "pit": """
        #######
        #-----#
        #-ooo-#
        #-o*o-#
        #-ooo-#
        #-----#
        #######
    """,
    # Crossway: four arms from a centre.
    "crossway": """
        ...#...
        ...#...
        ...+...
        ##+*+##
        ...+...
        ...#...
        ...#...
    """,
    # Lodeseek: a gem found among stone, rays pointing to it.
    "lodeseek": """
        #.....#
        .#...#.
        ..-+-..
        ..+*+..
        ..-+-..
        .#...#.
        #.....#
    """,
    # Vault: a cube of blocks.
    "vault": """
        ..#####
        .#+++##
        #####+#
        #+*+#+#
        #+++#+.
        #####..
    """,
    # Lamplit: four lamps round a centre.
    "lamplit": """
        *.....*
        .......
        .......
        ...+...
        .......
        .......
        *.....*
    """,
    # Fissure: a zigzag crack.
    "fissure": """
        #......
        .#.....
        ..#....
        .#.....
        ..#*...
        ...#...
        ..#....
        ...#...
    """,
    # Spiral: a curl winding out from a centre.
    "spiral": """
        .#####.
        #.....#
        #.###.#
        #.#*#.#
        #.#...#
        #.#####
    """,
    # Rosette: a flower of points round a centre.
    "rosette": """
        ...+...
        .#.#.#.
        ..#.#..
        +#.*.#+
        ..#.#..
        .#.#.#.
        ...+...
    """,
    # Stepstones: stones leading off across water.
    "stepstones": """
        -.-.-.#
        .-.-.-.
        -.-.#.-
        .-.-.-.
        -.#.-.-
        .-.-.-.
        *.-.-.-
    """,
    # Causeway: a broad road laid ahead.
    "causeway": """
        .#+#.
        .#.#.
        .#+#.
        .#.#.
        .#*#.
        .#.#.
        .#+#.
    """,
    # Hedgerow: a row of bushes.
    "hedgerow": """
        .#..#..#.
        ###+###+#
        #+###*###
        .|..|..|.
    """,
    # Lattice: a checkerboard.
    "lattice": """
        #.#.#.#
        .#.#.#.
        #.#.#.#
        .#.*.#.
        #.#.#.#
        .#.#.#.
        #.#.#.#
    """,
    # Collapse: a slab falling, things below it.
    "collapse": """
        #######
        #+++++#
        #######
        ..-.-..
        .-.-.-.
        .......
        ..*.*..
    """,
    # Fan: a spread of rays from your feet.
    "fan": """
        #..#..#
        .#.#.#.
        ..###..
        ...*...
    """,
    # Bobber: a float on the water.
    "bobber": """
        ...#...
        ...#...
        ..#*#..
        ..#+#..
        -.-.-.-
        .-.-.-.
    """,
    # ---------------------------------------------------------------- kin shapes
    # Herd: three beasts' heads together.
    "herd": """
        #.#...#.#
        ###...###
        .#.#.#.#.
        ...###...
        ...#*#...
        ....#....
    """,
    # Fellowship: three figures arm in arm.
    "fellowship": """
        .#..#..#.
        ###+#+###
        .#..*..#.
        .#..#..#.
        #.#.#.#.#
    """,
    # Saddle: a seat with stirrups.
    "saddle": """
        #.....#
        ##+++##
        .#####.
        .#.*.#.
        .#...#.
        ##...##
    """,
    # Packbond: a paw and a hand joined.
    "packbond": """
        .#.#.....
        #.#.#....
        .###.....
        .###+*+#.
        ......##.
        ......##.
    """,
    # Nursery: a small figure under a large one's arm.
    "nursery": """
        ..#......
        .###.....
        ..#+++...
        ..#..+*+.
        .#.#..#..
        .#.#.#.#.
    """,
    # Shoal: fish together in water.
    "shoal": """
        .##<.....
        ##+#..##.
        .##..##*#
        ......##.
        .##......
        ##+#.....
        -.-.-.-.-
    """,
    # Rearguard: an arrow turning back over a shoulder.
    "rearguard": """
        ...###.
        ..#...#
        .*.....
        ###....
        .#.....
        .#.....
    """,
    # Grudge: a narrowed eye, marked.
    "grudge": """
        #.......#
        .#.....#.
        ..#####..
        .#+k*k+#.
        ..#####..
    """,
    # Sentinel: a shield before a smaller figure.
    "sentinel": """
        #####..
        #+*+#..
        #+++#.#
        .#+#.###
        ..#...#.
        .....#.#
    """,
    # Aureole: a halo of light round you.
    "aureole": """
        .+++++.
        +.....+
        +..#..+
        +.###.+
        +..#..+
        +.#*#.+
        .+++++.
    """,
    # Tether: a line from your hand to a creature and round it.
    "tether": """
        .....###.
        ....#.*.#
        ....#...#
        ...-.###.
        ..-......
        .-.......
        #........
    """,
    # Flock: birds in a V.
    "flock": """
        #.......#
        .#.....#.
        ..#...#..
        ...#.#...
        ....*....
    """,
}

# Characters item_art does not know are drawn as nothing: keep the glyphs to its alphabet.
for _name, _text in list(GLYPHS.items()):
    GLYPHS[_name] = _text.replace("|", "#").replace("<", "w")

RECIPES: dict[str, list[str]] = {
    # Field shapes: the tools and materials of the work each one does.
    "furrow": ["minecraft:wooden_hoe", "minecraft:wheat_seeds"],
    "plot": ["minecraft:dirt", "minecraft:wheat_seeds", "minecraft:stick"],
    "seedbed": ["minecraft:iron_hoe", "minecraft:wheat_seeds", "minecraft:beetroot_seeds", "minecraft:bone_meal"],
    "shaft": ["minecraft:iron_pickaxe", "minecraft:ladder"],
    "stairwell": ["minecraft:stone_stairs", "minecraft:iron_pickaxe", "minecraft:torch"],
    "corridor": ["minecraft:iron_pickaxe", "minecraft:rail", "minecraft:torch"],
    "seam": ["minecraft:stone_pickaxe", "minecraft:coal"],
    "facade": ["minecraft:bricks", "minecraft:glass_pane", "minecraft:stone_bricks"],
    "dome": ["minecraft:glass", "minecraft:glass", "minecraft:smooth_stone", "minecraft:amethyst_shard"],
    "footing": ["minecraft:cobblestone", "minecraft:leather_boots"],
    "canopy": ["minecraft:oak_slab", "minecraft:oak_leaves", "minecraft:oak_fence"],
    "shoreline": ["minecraft:sand", "minecraft:water_bucket", "minecraft:sugar_cane"],
    "perimeter": ["minecraft:oak_fence", "minecraft:oak_fence_gate", "minecraft:string"],
    "spire": ["minecraft:cobblestone_wall", "minecraft:end_rod"],
    "pit": ["minecraft:iron_shovel", "minecraft:gravel"],
    "crossway": ["minecraft:compass", "minecraft:gravel", "minecraft:stick"],
    "lodeseek": ["minecraft:compass", "minecraft:raw_iron", "minecraft:raw_copper", "minecraft:raw_gold"],
    "vault": ["minecraft:iron_pickaxe", "minecraft:chest", "minecraft:iron_block"],
    "lamplit": ["minecraft:lantern", "minecraft:glowstone_dust"],
    "fissure": ["minecraft:flint", "minecraft:iron_pickaxe", "minecraft:tnt"],
    "spiral": ["minecraft:nautilus_shell", "minecraft:amethyst_shard", "minecraft:redstone"],
    "rosette": ["minecraft:pink_petals", "minecraft:sunflower", "minecraft:glowstone_dust"],
    "stepstones": ["minecraft:cobblestone", "minecraft:lily_pad"],
    "causeway": ["minecraft:cobblestone_slab", "minecraft:iron_shovel", "minecraft:rail"],
    "hedgerow": ["minecraft:oak_sapling", "minecraft:sweet_berries", "minecraft:oak_leaves"],
    "lattice": ["minecraft:iron_bars", "minecraft:white_carpet", "minecraft:black_carpet"],
    "collapse": ["minecraft:anvil", "minecraft:tnt", "minecraft:gravel"],
    "fan": ["minecraft:feather", "minecraft:arrow", "minecraft:arrow"],
    "bobber": ["minecraft:fishing_rod", "minecraft:cod"],
    # Kin shapes: what the creatures they choose would answer to.
    "herd": ["minecraft:wheat", "minecraft:lead", "minecraft:hay_block"],
    "fellowship": ["minecraft:cake", "minecraft:golden_apple", "minecraft:emerald"],
    "saddle": ["minecraft:saddle"],
    "packbond": ["minecraft:bone", "minecraft:lead", "minecraft:name_tag"],
    "nursery": ["minecraft:egg", "minecraft:milk_bucket"],
    "shoal": ["minecraft:tropical_fish", "minecraft:salmon", "minecraft:prismarine_shard"],
    "rearguard": ["minecraft:shield", "minecraft:arrow", "minecraft:spyglass"],
    "grudge": ["minecraft:rotten_flesh", "minecraft:iron_sword", "minecraft:ink_sac"],
    "sentinel": ["minecraft:shield", "minecraft:iron_sword", "minecraft:bell", "minecraft:carved_pumpkin"],
    "aureole": ["minecraft:glowstone_dust", "minecraft:gold_nugget", "minecraft:feather"],
    "tether": ["minecraft:lead", "minecraft:ender_pearl", "minecraft:string"],
    "flock": ["minecraft:feather", "minecraft:phantom_membrane", "minecraft:wheat_seeds"],
}

LANG: dict[str, str] = {
    "category.wildercord.shape.field": "Field",
    "category.wildercord.shape.kin": "Kin",
}
