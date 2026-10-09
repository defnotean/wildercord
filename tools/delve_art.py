"""The delving pack (fx-mine): 48 working runes for mines, masonry, light, light redstone and hauling.

Their behaviour is cast/DelveEffects.java and the numbers spell/DelveRules.java. This file holds their icons (merged into
item_art.GLYPHS), their crafting themes (merged into generate_assets.RUNE_RECIPES), the loot-table names their pools
use and the words their messages show (merged into en_us.json).

Glyph format as item_art.GLYPHS: at most 9 wide and 9 tall, "." empty, "-" shade, "#" main, "+" light, "*" core.
"""

# Themed items per rune (the Blank Rune and the tier's catalysts are added by generate_assets). Tier IV runes
# (motherlode, delvemark) are never crafted: they come from the Archives like every Tier IV rune.
RECIPES: dict[str, list[str]] = {
    "stairdelve": ["minecraft:stone_pickaxe", "minecraft:cobblestone_stairs"],
    "riser": ["minecraft:stone_pickaxe", "minecraft:ladder"],
    "plumbline": ["minecraft:string", "minecraft:iron_nugget", "minecraft:cobblestone"],
    "siftfall": ["minecraft:gravel", "minecraft:sand"],
    "gangue": ["minecraft:iron_pickaxe", "minecraft:tuff", "minecraft:raw_iron"],
    "orepluck": ["minecraft:iron_pickaxe", "minecraft:raw_copper", "minecraft:raw_gold"],
    "luckstrike": ["minecraft:iron_pickaxe", "minecraft:rabbit_foot", "minecraft:emerald"],
    "silklift": ["minecraft:string", "minecraft:string", "minecraft:iron_pickaxe"],
    "deepsound": ["minecraft:note_block"],
    "oretally": ["minecraft:raw_iron", "minecraft:coal", "minecraft:paper"],
    "lavaseal": ["minecraft:water_bucket", "minecraft:obsidian"],
    "deepway": ["minecraft:diamond_pickaxe", "minecraft:torch", "minecraft:rail"],
    "hollowsense": ["minecraft:echo_shard"],
    "kilnbake": ["minecraft:furnace", "minecraft:coal", "minecraft:clay_ball"],
    "blockpack": ["minecraft:crafting_table", "minecraft:iron_nugget"],
    "unpack": ["minecraft:crafting_table", "minecraft:flint"],
    "millstone": ["minecraft:grindstone"],
    "toolmend": ["minecraft:anvil", "minecraft:iron_ingot", "minecraft:grindstone"],
    "levelground": ["minecraft:iron_shovel", "minecraft:iron_pickaxe"],
    "holefill": ["minecraft:dirt", "minecraft:cobblestone", "minecraft:oak_planks"],
    "shoreup": ["minecraft:oak_log", "minecraft:gravel"],
    "stilt": ["minecraft:packed_mud", "minecraft:scaffolding"],
    "plankway": ["minecraft:oak_planks", "minecraft:oak_planks", "minecraft:string"],
    "polish": ["minecraft:polished_andesite", "minecraft:sand"],
    "brickwork": ["minecraft:stonecutter", "minecraft:brick"],
    "agestone": ["minecraft:moss_block", "minecraft:cracked_stone_bricks"],
    "concreteset": ["minecraft:white_concrete_powder", "minecraft:water_bucket"],
    "chalkline": ["minecraft:bone_meal", "minecraft:string"],
    "pitfloor": ["minecraft:packed_mud", "minecraft:packed_mud", "minecraft:scaffolding"],
    "torchfall": ["minecraft:torch", "minecraft:torch", "minecraft:coal"],
    "gloomsight": ["minecraft:spider_eye", "minecraft:glow_ink_sac"],
    "lumenpath": ["minecraft:glowstone_dust", "minecraft:glowstone_dust", "minecraft:string"],
    "snuffout": ["minecraft:water_bucket", "minecraft:ink_sac"],
    "headlamp": ["minecraft:lantern", "minecraft:leather_helmet"],
    "leverflip": ["minecraft:lever"],
    "buttonpush": ["minecraft:stone_button"],
    "doorcall": ["minecraft:oak_door", "minecraft:redstone"],
    "chestsort": ["minecraft:chest", "minecraft:comparator"],
    "stow": ["minecraft:chest", "minecraft:hopper"],
    "restock": ["minecraft:barrel", "minecraft:hopper"],
    "stocktake": ["minecraft:chest", "minecraft:paper"],
    "unburden": ["minecraft:barrel", "minecraft:chest"],
    "lodepull": ["minecraft:lodestone", "minecraft:hopper"],
    "caveward": ["minecraft:turtle_helmet", "minecraft:sand", "minecraft:gravel"],
    "floorlay": ["minecraft:smooth_stone", "minecraft:smooth_stone", "minecraft:oak_planks"],
    "packtidy": ["minecraft:bundle"],
}

# The loot tables the pack's pools use (WildercordLoot), as tooltips name them.
LOOT_TABLE_NAMES = {
    "VILLAGE_TOOLSMITH": "Village toolsmiths",
    "VILLAGE_MASON": "Village masons",
    "VILLAGE_TEMPLE": "Village temples",
}

LANG = {
    "message.wildercord.delve_stopped": "Stopped short of lava, water or a drop",
    "message.wildercord.delve_not_ore": "That isn't an ore",
    "message.wildercord.deepsound_found": "%s, %s blocks down",
    "message.wildercord.deepsound_none": "No ore in the 16 blocks below",
    "message.wildercord.oretally": "Ores near: %s",
    "message.wildercord.oretally_none": "No ores near",
    "message.wildercord.deepway_wall": "Aim at a wall",
    "message.wildercord.hollowsense": "Open cave %s, %s blocks off",
    "message.wildercord.hollowsense_none": "No open cave near",
    "message.wildercord.delve_dir.up": "above",
    "message.wildercord.delve_dir.down": "below",
    "message.wildercord.delve_dir.east": "east",
    "message.wildercord.delve_dir.west": "west",
    "message.wildercord.delve_dir.north": "north",
    "message.wildercord.delve_dir.south": "south",
    "message.wildercord.blockpack_none": "Nothing to pack",
    "message.wildercord.unpack_none": "No storage blocks to unpack",
    "message.wildercord.millstone_none": "No cobblestone or gravel to grind",
    "message.wildercord.toolmend_none": "Nothing to mend, or no material for it",
    "message.wildercord.delve_no_blocks": "No stone, dirt or planks in your pack",
    "message.wildercord.delve_blocked": "No room",
    "message.wildercord.chalkline": "%s blocks across, %s up",
    "message.wildercord.delve_no_torches": "No torches in your pack",
    "message.wildercord.delve_no_chest": "Aim at a chest, barrel or shulker box",
    "message.wildercord.delve_stowed": "Stowed %s",
    "message.wildercord.delve_restocked": "Restocked %s",
    "message.wildercord.stocktake": "In store: %s",
    "message.wildercord.stocktake_none": "No stock near",
    "message.wildercord.delvemark_set": "Mark set",
    "message.wildercord.delvemark_far": "Too far from your mark",
    "message.wildercord.delvemark_hold": "Hold still...",
    "message.wildercord.delvemark_broken": "Return broken",
}

GLYPHS: dict[str, str] = {
    # Stair Delve: steps going down to the right.
    "stairdelve": """
        #......
        ##.....
        .##....
        ..##...
        ...##..
        ....##.
        .....+*
    """,
    # Riser: steps climbing to the right, a bright top.
    "riser": """
        .....+*
        ....##.
        ...##..
        ..##...
        .##....
        ##.....
        #......
    """,
    # Plumb Line: a bob on a string over a shaft.
    "plumbline": """
        ...+...
        ...#...
        ...#...
        ..###..
        ..#*#..
        ...#...
        #.....#
    """,
    # Siftfall: grains falling out of a ceiling.
    "siftfall": """
        #######
        .#.#.#.
        .......
        ..+.+..
        .......
        .+.*.+.
        -------
    """,
    # Gangue: rock cut away round a standing ore.
    "gangue": """
        -.-.-.-
        .......
        -.###.-
        ..#*#..
        -.###.-
        .......
        -.-.-.-
    """,
    # Ore Pluck: a nugget lifted out of a wall.
    "orepluck": """
        ###....
        #.#..+.
        #.#.+*+
        #.#..+.
        #.#....
        #.#....
        ###....
    """,
    # Luckstrike: a four-leaf over an ore.
    "luckstrike": """
        .+...+.
        +*+.+*+
        .+.#.+.
        ...#...
        .#####.
        .#*#*#.
        .#####.
    """,
    # Silklift: a whole block held in a silk loop.
    "silklift": """
        ..+++..
        .+...+.
        +.###.+
        +.#*#.+
        +.###.+
        .+...+.
        ..+++..
    """,
    # Deepsound: rings under the ground, an ore at the bottom.
    "deepsound": """
        #######
        .......
        ..+++..
        .+...+.
        +.....+
        ...*...
        ..-#-..
    """,
    # Ore Tally: tally marks beside an ore.
    "oretally": """
        .......
        #.#.#.#
        #.#.#.#
        #####.#
        #.#.#.#
        .......
        ..+*+..
    """,
    # Lava Seal: a wave of lava capped by a block.
    "lavaseal": """
        #######
        #+++++#
        #######
        .......
        .*.*.*.
        *.*.*.*
        -------
    """,
    # Deepway: a square tunnel mouth with two torches.
    "deepway": """
        +.....+
        *#####*
        .#...#.
        .#...#.
        .#...#.
        .#####.
        .......
    """,
    # Hollow Sense: an eye looking into a cave mouth.
    "hollowsense": """
        ..###..
        .#...#.
        #..+..#
        #.+*+.#
        #..+..#
        #.....#
        -------
    """,
    # Kiln Bake: a block over flames.
    "kilnbake": """
        .#####.
        .#+++#.
        .#####.
        .......
        .*.*.*.
        *+*+*+*
        -------
    """,
    # Block Pack: nine dots into one block.
    "blockpack": """
        +.+.+..
        +.+.+..
        +.+.+..
        ...#...
        ..###..
        ..#*#..
        ..###..
    """,
    # Unpack: one block into nine dots.
    "unpack": """
        ..###..
        ..#*#..
        ..###..
        ...#...
        +.+.+..
        +.+.+..
        +.+.+..
    """,
    # Millstone: a round stone with a hole and grit below.
    "millstone": """
        ..###..
        .#...#.
        #..*..#
        .#...#.
        ..###..
        .......
        .-.-.-.
    """,
    # Tool Mend: a pickaxe with a spark on its head.
    "toolmend": """
        .####.*
        #....#.
        ...#...
        ..#....
        .#.....
        #......
        .......
    """,
    # Levelground: a flat line with lumps above it shaved off.
    "levelground": """
        .-...-.
        -#-.-#-
        .......
        #######
        #+++++#
        #######
        .......
    """,
    # Holefill: a pit filled to the top.
    "holefill": """
        ##...##
        ##...##
        ##+++##
        ##+*+##
        ##+++##
        #######
        .......
    """,
    # Shore Up: a prop under a hanging block.
    "shoreup": """
        .#####.
        .#+++#.
        .#####.
        ...#...
        ...#...
        ...#...
        .#####.
    """,
    # Stilt: a column lifting a little figure.
    "stilt": """
        ...*...
        ..+#+..
        ...#...
        ..###..
        ..###..
        ..###..
        -------
    """,
    # Plankway: a bridge of planks over a gap.
    "plankway": """
        .......
        #+#+#+#
        #######
        #.....#
        #.....#
        .......
        -.....-
    """,
    # Polish: a block with a shine across it.
    "polish": """
        #######
        #....+#
        #...+.#
        #..*..#
        #.+...#
        #+....#
        #######
    """,
    # Brickwork: a wall of bricks.
    "brickwork": """
        #######
        ..#...#
        #######
        #...#..
        #######
        ..#*..#
        #######
    """,
    # Agestone: a block with moss on top and a crack down it.
    "agestone": """
        +++++++
        #+#+#+#
        #..#..#
        #..#..#
        #.#...#
        #.#*..#
        #######
    """,
    # Concrete Set: powder grains settling into a solid block.
    "concreteset": """
        .-.-.-.
        -.-.-.-
        .......
        #######
        #+++++#
        #+*+*+#
        #######
    """,
    # Chalk Line: two posts and a bright taut line.
    "chalkline": """
        #.....#
        #+++++#
        #.....#
        #.....#
        #.....#
        *.....*
        -.....-
    """,
    # Pit Floor: a floor over an open pit.
    "pitfloor": """
        .......
        #######
        #+*+*+#
        .......
        -.....-
        -.....-
        -------
    """,
    # Torchfall: three torches falling into place.
    "torchfall": """
        *..*..*
        +..+..+
        #..#..#
        #..#..#
        .......
        .......
        -------
    """,
    # Gloomsight: an eye over dark floor marks.
    "gloomsight": """
        ..###..
        .#.*.#.
        ..###..
        .......
        +.+.+.+
        .......
        -------
    """,
    # Lumen Path: lights along a line.
    "lumenpath": """
        ......*
        .....-.
        ....*..
        ...-...
        ..*....
        .-.....
        *......
    """,
    # Snuff Out: a flame with a bar through it.
    "snuffout": """
        ...+...
        ..+#+..
        .+###+.
        #######
        .+#*#+.
        ..###..
        .......
    """,
    # Headlamp: a head with a beam out of the brow.
    "headlamp": """
        .###...
        #*#++++
        #.#....
        #.#++++
        .###...
        ..#....
        .###...
    """,
    # Lever Flip: a lever on its base.
    "leverflip": """
        .....*.
        ....+..
        ...#...
        ..#....
        .#####.
        .#####.
        .......
    """,
    # Button Push: a small button pressed with an arrow.
    "buttonpush": """
        ...#...
        ...#...
        .#####.
        ..###..
        ...#...
        .#+*+#.
        #######
    """,
    # Doorcall: a door ajar.
    "doorcall": """
        ####...
        #..##..
        #..#.#.
        #.*#.#.
        #..#.#.
        #..##..
        ####...
    """,
    # Chest Sort: a chest with neat rows inside.
    "chestsort": """
        #######
        #+++++#
        #######
        #--*--#
        #-----#
        #######
        .......
    """,
    # Stow: an arrow into a chest.
    "stow": """
        ...#...
        ...#...
        .#####.
        ..###..
        #######
        #..*..#
        #######
    """,
    # Restock: an arrow out of a chest.
    "restock": """
        ...*...
        ..###..
        .#####.
        ...#...
        #######
        #..+..#
        #######
    """,
    # Stocktake: a chest with a list beside it.
    "stocktake": """
        ....###
        ....#-#
        ....#-#
        ###.###
        #*#....
        ###....
        .......
    """,
    # Unburden: a heavy pack tipping out.
    "unburden": """
        .###...
        #####..
        #####..
        .###.+.
        ....+.+
        ...+.*.
        ....+..
    """,
    # Lodepull: drops drifting to a lodestone.
    "lodepull": """
        +.....+
        .+...+.
        ..+.+..
        ...*...
        ..###..
        .#####.
        .......
    """,
    # Caveward: a helmet under falling grit.
    "caveward": """
        -.-.-.-
        .-.-.-.
        .......
        ..###..
        .#+*+#.
        #######
        .......
    """,
    # Delvemark: a flag over a marked spot.
    "delvemark": """
        .*#....
        .###...
        .####..
        .#.....
        .#.....
        +#+....
        .+.....
    """,
    # Motherlode: a cluster of bright ores.
    "motherlode": """
        .#...#.
        #*#.#*#
        .#.#.#.
        ..#*#..
        .#.#.#.
        #*#.#*#
        .#...#.
    """,
    # Floorlay: a floor of tiles seen from above.
    "floorlay": """
        #######
        #+#+#+#
        #######
        #+#*#+#
        #######
        #+#+#+#
        #######
    """,
    # Pack Tidy: a bag with stacks joined.
    "packtidy": """
        ..#+#..
        .#...#.
        #.###.#
        #.#*#.#
        #.###.#
        #.....#
        .#####.
    """,
}
