"""The hearth pack (fx-passive): icons, recipes and messages for the gentle long-lasting utility runes
(see cast/HearthEffects.java). Icons are merged into item_art.GLYPHS, recipes into
generate_assets.RUNE_RECIPES and messages into the lang file.

Glyph format as item_art.GLYPHS: at most 9 by 9, "." empty, "-" shade, "#" main, "+" light, "*" core,
"o" darkens the stone, "k" near-black, "w" white.
"""

GLYPHS: dict[str, str] = {
    # Slowburn: a small steady flame over a full bowl.
    "slowburn": """
        ....*....
        ...*+*...
        ...+#+...
        ....#....
        .........
        #.......#
        .#+++++#.
        ..#####..
        .........
    """,
    # Camp Ward: a tent under a ring.
    "camp_ward": """
        ..#####..
        .#.....#.
        #...*...#
        #..#+#..#
        #.#+*+#.#
        .#######.
        ..#...#..
        .........
        .........
    """,
    # Warm Cloak: a hooded cloak with a glowing clasp.
    "warm_cloak": """
        ...###...
        ..#+++#..
        ..#+.+#..
        .#++*++#.
        .#+++++#.
        #+++++++#
        #+++-+++#
        ###...###
        .........
    """,
    # Softsole: a sole over three furrows.
    "softsole": """
        ...###...
        ..#+++#..
        ..#+*+#..
        ..#+++#..
        ...###...
        .........
        -#-#-#-#-
        #-#-#-#-#
        .........
    """,
    # Softfoot: a footprint fading into shadow.
    "softfoot": """
        ......##.
        .....#++#
        .....#++#
        ....-#+#.
        ...-.-#..
        ..-......
        .##......
        #++#.....
        .##......
    """,
    # Hollow Pocket: a pouch with a dark mouth.
    "hollow_pocket": """
        ...#.#...
        ..#####..
        .#kkkkk#.
        #+okkko+#
        #++ooo++#
        #+++*+++#
        #+++++++#
        .#+++++#.
        ..#####..
    """,
    # Lodestar: a star over a stone.
    "lodestar": """
        ....*....
        ...*w*...
        ..*w*w*..
        ...*w*...
        ....*....
        .........
        .#######.
        #+++o+++#
        #########
    """,
    # Homeward: a door with an arrow coming home.
    "homeward": """
        ...###...
        ..#+++#..
        .#+++++#.
        .#+*+++#.
        *#+***+#.
        .#+*+++#.
        .#+++++#.
        .#+++o+#.
        .#######.
    """,
    # Gravefinder: a headstone with a mote over it.
    "gravefinder": """
        ....*....
        .........
        ..#####..
        .#+++++#.
        .#++#++#.
        .#+###+#.
        .#++#++#.
        .#+++++#.
        #########
    """,
    # Skyread: a cloud with sun behind it.
    "skyread": """
        .....*.*.
        ......*..
        ..##.*.*.
        .#++##...
        #++++++#.
        #+++++++#
        .#######.
        ..+..+...
        .+..+....
    """,
    # Lullaby: a crescent moon and a note.
    "lullaby": """
        ...###...
        ..#++....
        .#++.....
        .#+...#..
        .#+...##.
        .#++..#..
        ..#++*#..
        ...###...
        .........
    """,
    # Steedsong: a horseshoe with wind lines.
    "steedsong": """
        .#.....#.
        #+#...#+#
        #+#...#+#
        #+#...#+#
        #+#...#+#
        .#+#.#+#.
        ..#+*+#..
        ...###...
        -.-.-.-.-
    """,
    # Glidewind: spread wings riding a gust.
    "glidewind": """
        .........
        #.......#
        ##.....##
        #+#...#+#
        .#+#*#+#.
        ..#+++#..
        ...#+#...
        -.-.-.-.-
        .-.-.-.-.
    """,
    # Waymark: a tall pillar of light on a base.
    "waymark": """
        ....w....
        ....*....
        ....*....
        ...+*+...
        ....*....
        ....*....
        ...+*+...
        ..#####..
        .#######.
    """,
    # Ember Rest: crossed logs and a low flame.
    "ember_rest": """
        ....*....
        ...*+*...
        ..*+#+*..
        ...+#+...
        .........
        #.......#
        .##...##.
        ...###...
        .##...##.
    """,
    # Orbcall: little orbs spiralling inward.
    "orbcall": """
        *.......*
        .+.....+.
        ...#.#...
        ..#+*+#..
        ...*w*...
        ..#+*+#..
        ...#.#...
        .+.....+.
        *.......*
    """,
    # Tinker's Hum: a hammer over an anvil.
    "tinker_hum": """
        ..####...
        ..#++#...
        ..####...
        ....#....
        ....#..*.
        .#######.
        ..#+++#..
        ...#+#...
        .#######.
    """,
    # Lantern Soul: a lantern with a little soul flame.
    "lantern_soul": """
        ....#....
        ...###...
        ..#+++#..
        ..#+*+#..
        ..#*w*#..
        ..#+*+#..
        ..#+++#..
        ...###...
        .........
    """,
    # Keenkeep: a pickaxe with a sparkle on its edge.
    "keenkeep": """
        .#####...
        #+++++#*.
        ....#+#..
        ...#+#.#.
        ..#+#....
        .#+#.....
        #+#......
        ##.......
        .........
    """,
    # Landread: a hill with an eye above.
    "landread": """
        ..#####..
        .#+*w*+#.
        ..#####..
        .........
        ....#....
        ...#+#...
        ..#+++#..
        .#+++++#.
        #########
    """,
    # Rally Light: a beacon beam over a pyramid.
    "rally_light": """
        ....w....
        ....*....
        ...+*+...
        ....*....
        ...###...
        ..#+*+#..
        .#+++++#.
        #+++++++#
        #########
    """,
    # Dew Drink: a drop over a cupped leaf.
    "dew_drink": """
        ....#....
        ...#+#...
        ..#+*+#..
        ..#+++#..
        ...###...
        .........
        #.......#
        .#+++++#.
        ..#####..
    """,
    # Sunbask: a sun with rays.
    "sunbask": """
        +...+...+
        .+..+..+.
        ..#####..
        ..#*w*#..
        ++#www#++
        ..#*w*#..
        ..#####..
        .+..+..+.
        +...+...+
    """,
    # Currentkin: a dolphin's leap over a wave.
    "currentkin": """
        .........
        ...###...
        ..#+++#..
        .#+*..+#.
        .#+....#.
        #+.....#.
        .........
        -#--#--#-
        #-##-##-#
    """,
    # Surefoot: a boot climbing a step.
    "surefoot": """
        .........
        ...##....
        ...#+#...
        ...#+#...
        ..#++##..
        ..#####..
        ....#####
        ....#+++#
        #########
    """,
    # Long Arm: a hand at the end of a long reach.
    "long_arm": """
        ......#.#
        .....#+#+
        .....#+++
        ....#+++#
        ...#+#...
        ..#+#....
        .#+#.....
        #+#......
        ##.......
    """,
    # Nightwatch: an open eye under a bell.
    "nightwatch": """
        ...###...
        ..#+++#..
        ..#+++#..
        .#######.
        ....*....
        .........
        ..#####..
        .#+*k*+#.
        ..#####..
    """,
    # Trailblaze: crumbs leading off.
    "trailblaze": """
        .......*.
        .........
        .....+...
        .........
        ...+.....
        .........
        .+.......
        #........
        ##.......
    """,
    # Hearthpath: a house with a warm window.
    "hearthpath": """
        ....#....
        ...#+#...
        ..#+++#..
        .#+++++#.
        #########
        .#+++++#.
        .#+*+++#.
        .#+++o+#.
        .#######.
    """,
    # Lostfind: a dropped gem with a seeking ring.
    "lostfind": """
        ..#####..
        .#.....#.
        #...*...#
        #..*w*..#
        #...*...#
        .#.....#.
        ..#####.#
        ........#
        .......##
    """,
    # Stillwell: still water in a well.
    "stillwell": """
        .#######.
        ..#...#..
        ..#...#..
        .........
        #.......#
        #+*+*+*+#
        #+++++++#
        #+++++++#
        #########
    """,
    # Starchart: a compass rose.
    "starchart": """
        ....#....
        ....*....
        ...#+#...
        .#.+*+.#.
        #*+*w*+*#
        .#.+*+.#.
        ...#+#...
        ....*....
        ....#....
    """,
    # Petward: a paw under a shield.
    "petward": """
        .#######.
        #+++++++#
        #+#.+.#+#
        #+.....+#
        #+.###.+#
        .#+###+#.
        ..#+++#..
        ...#+#...
        ....#....
    """,
    # Whistle: a whistle with notes.
    "whistle": """
        ......+..
        .....+...
        ...+..+..
        .......+.
        .####....
        #+++*#...
        #+++++###
        .#++++++#
        ..#######
    """,
    # Luckcharm: a four-leaf clover.
    "luckcharm": """
        ..##.##..
        .#++#++#.
        .#++#++#.
        ..##*##..
        .#++#++#.
        .#++#++#.
        ..##.##..
        ....#....
        .....#...
    """,
    # Smoke Signal: puffs rising from a fire.
    "smoke_signal": """
        ...--....
        ..-++-...
        ...--.--.
        .....-++-
        ...--.--.
        ..-++-...
        ...--....
        ...*+*...
        ..#####..
    """,
    # Wayfarer's Hymn: a walking staff and a note.
    "wayfarer_hymn": """
        ..##.....
        .#+#..#..
        .##...##.
        ..#...#..
        ..#.###..
        ..#.###..
        ..#......
        ..#......
        .###.....
    """,
    # Steedmend: a horseshoe with a heart.
    "steedmend": """
        .#.....#.
        #+#...#+#
        #+#.#.#+#
        #+#*+*#+#
        #+#.*.#+#
        .#+#.#+#.
        ..#+++#..
        ...###...
        .........
    """,
    # Dynamo Stride: a boot with a spark.
    "dynamo_stride": """
        ......*..
        .....*...
        ...##*...
        ...#+#*..
        ...#+#...
        ..#++##..
        ..#+++###
        ..#++++++
        ..#######
    """,
    # Tarry: an hourglass with slow sand.
    "tarry": """
        #########
        .#+++++#.
        ..#+++#..
        ...#*#...
        ....*....
        ...#.#...
        ..#.*.#..
        .#.+*+.#.
        #########
    """,
    # Clot: a drop sealed by a band.
    "clot": """
        ....#....
        ...#+#...
        ..#+++#..
        .#+++++#.
        .#######.
        .#+*+*+#.
        .#######.
        ..#+++#..
        ...###...
    """,
    # Heartsense: a heart with pulse lines.
    "heartsense": """
        .##...##.
        #++#.#++#
        #+++#+++#
        #+++*+++#
        .#+++++#.
        ..#+++#..
        ...#+#...
        ....#....
        -.-.-.-.-
    """,
    # Quench: a flame under a falling drop.
    "quench": """
        ....#....
        ...#+#...
        ...###...
        .........
        ....-....
        ...-+-...
        ..-+#+-..
        ..-###-..
        ...---...
    """,
    # Hearthbond: two hands joined round a heart.
    "hearthbond": """
        .........
        ..##.##..
        .#*+#+*#.
        .#+***+#.
        ##.#+#.##
        #+#.#.#+#
        #++#.#++#
        .#+++++#.
        ..#####..
    """,
    # Springseek: a dowsing fork over a drop.
    "springseek": """
        #.......#
        .#.....#.
        ..#...#..
        ...#.#...
        ....#....
        ....#....
        ...#+#...
        ..#+*+#..
        ...###...
    """,
    # Savor: a bowl with rising steam.
    "savor": """
        ..-..-...
        ...-..-..
        ..-..-...
        .........
        #.......#
        #+++*+++#
        .#+++++#.
        ..#####..
        ...###...
    """,
    # Deepwarn: a warning mark over a drop.
    "deepwarn": """
        ....#....
        ...#*#...
        ...#*#...
        ..#.*.#..
        ..#...#..
        .#..*..#.
        .#######.
        ###...###
        #k#...#k#
    """,
    # Enderhush: a calm closed eye.
    "enderhush": """
        .........
        .........
        .#######.
        #+++++++#
        .#*****#.
        ..#####..
        ...k.k...
        ..k...k..
        .........
    """,
}

# Themed items for each craftable rune (tier catalysts are added on top). Homeward (tier 4) is found, not crafted.
RECIPES: dict[str, list[str]] = {
    "slowburn": ["minecraft:bread", "minecraft:charcoal"],
    "camp_ward": ["minecraft:campfire", "minecraft:white_wool"],
    "warm_cloak": ["minecraft:leather", "minecraft:blaze_powder"],
    "softsole": ["minecraft:feather", "minecraft:wheat_seeds"],
    "softfoot": ["minecraft:white_wool", "minecraft:ender_pearl"],
    "hollow_pocket": ["minecraft:bundle", "minecraft:ender_pearl"],
    "lodestar": ["minecraft:lodestone"],
    "gravefinder": ["minecraft:bone", "minecraft:compass"],
    "skyread": ["minecraft:feather", "minecraft:clock"],
    "lullaby": ["minecraft:white_bed", "minecraft:phantom_membrane"],
    "steedsong": ["minecraft:saddle", "minecraft:sugar"],
    "glidewind": ["minecraft:phantom_membrane", "minecraft:wind_charge"],
    "waymark": ["minecraft:torch", "minecraft:stick", "minecraft:glowstone_dust"],
    "ember_rest": ["minecraft:campfire", "minecraft:glistering_melon_slice"],
    "orbcall": ["minecraft:experience_bottle"],
    "tinker_hum": ["minecraft:anvil", "minecraft:iron_ingot", "minecraft:copper_ingot"],
    "lantern_soul": ["minecraft:lantern", "minecraft:soul_torch"],
    "keenkeep": ["minecraft:grindstone", "minecraft:flint"],
    "landread": ["minecraft:dirt", "minecraft:spyglass"],
    "rally_light": ["minecraft:beacon"],
    "dew_drink": ["minecraft:glass_bottle", "minecraft:lily_pad"],
    "sunbask": ["minecraft:sunflower", "minecraft:glowstone_dust"],
    "currentkin": ["minecraft:cod", "minecraft:prismarine_crystals"],
    "surefoot": ["minecraft:leather_boots", "minecraft:cobblestone_stairs"],
    "long_arm": ["minecraft:stick", "minecraft:stick", "minecraft:string", "minecraft:iron_nugget"],
    "nightwatch": ["minecraft:bell", "minecraft:spider_eye"],
    "trailblaze": ["minecraft:bread", "minecraft:lime_dye"],
    "hearthpath": ["minecraft:compass", "minecraft:red_bed"],
    "lostfind": ["minecraft:gold_nugget", "minecraft:spyglass"],
    "stillwell": ["minecraft:water_bucket", "minecraft:lapis_lazuli"],
    "starchart": ["minecraft:map", "minecraft:compass"],
    "petward": ["minecraft:bone", "minecraft:shield"],
    "whistle": ["minecraft:goat_horn"],
    "luckcharm": ["minecraft:rabbit_foot", "minecraft:emerald"],
    "smoke_signal": ["minecraft:campfire", "minecraft:hay_block"],
    "wayfarer_hymn": ["minecraft:note_block", "minecraft:sugar", "minecraft:rabbit_hide"],
    "steedmend": ["minecraft:golden_carrot", "minecraft:hay_block"],
    "dynamo_stride": ["minecraft:redstone", "minecraft:copper_ingot", "minecraft:leather_boots"],
    "tarry": ["minecraft:clock", "minecraft:honey_bottle"],
    "clot": ["minecraft:milk_bucket", "minecraft:spider_eye"],
    "heartsense": ["minecraft:fermented_spider_eye", "minecraft:redstone"],
    "quench": ["minecraft:water_bucket", "minecraft:snowball", "minecraft:ice"],
    "hearthbond": ["minecraft:golden_apple", "minecraft:string"],
    "springseek": ["minecraft:stick", "minecraft:clay_ball", "minecraft:glass_bottle"],
    "savor": ["minecraft:cooked_beef", "minecraft:honey_bottle"],
    "deepwarn": ["minecraft:magma_cream", "minecraft:spyglass"],
    "enderhush": ["minecraft:carved_pumpkin", "minecraft:ender_pearl"],
}

LANG: dict[str, str] = {
    "container.wildercord.hollow_pocket": "Hollow Pocket",
    "message.wildercord.hearth.no_mount": "No mount or animal to sing to",
    "message.wildercord.hearth.no_pets": "No pets heard you",
    "message.wildercord.hearth.pets": "%s pets came to you",
    "message.wildercord.hearth.lullaby": "%s of %s asleep",
    "message.wildercord.hearth.waymark": "Waymark set (%s of %s)",
    "message.wildercord.hearth.lodestar_set": "Lodestar set at %s, %s, %s",
    "message.wildercord.hearth.no_lodestar": "You have no lodestar",
    "message.wildercord.hearth.lodestar_far": "Your lodestar is too far, or in another world",
    "message.wildercord.hearth.homeward_rest": "Homeward needs %s more seconds of rest",
    "message.wildercord.hearth.homeward_start": "Hold still...",
    "message.wildercord.hearth.homeward_broken": "Homeward broke",
    "message.wildercord.hearth.homeward_blocked": "Something blocks your lodestar",
    "message.wildercord.hearth.star": "You're at %s, %s, %s facing %s. Spawn is %s blocks away",
    "message.wildercord.hearth.no_death": "You haven't died yet",
    "message.wildercord.hearth.death_far": "Last death: %s blocks",
    "message.wildercord.hearth.home_far": "Home: %s blocks",
    "message.wildercord.hearth.water_far": "Water: %s blocks",
    "message.wildercord.hearth.no_water": "No water within 24 blocks",
    "message.wildercord.hearth.no_items": "No loose items within 32 blocks",
    "message.wildercord.hearth.other_world": "That's in another world",
    "message.wildercord.hearth.weather_clear": "Clear skies for about %s min",
    "message.wildercord.hearth.weather_rain": "Rain for about %s min",
    "message.wildercord.hearth.weather_storm": "Storm for about %s min",
    "message.wildercord.hearth.time": "It's about %s o'clock. Moon phase %s of 8",
    "message.wildercord.hearth.land": "%s, height %s, light %s",
    "message.wildercord.hearth.slime": "Slimes spawn in this chunk",
    "message.wildercord.hearth.hunted": "Something is hunting you",
    "message.wildercord.hearth.lava": "Lava below!",
    "message.wildercord.hearth.drop": "A long drop ahead!",
    "message.wildercord.hearth.bond_low": "%s needs help!",
}
