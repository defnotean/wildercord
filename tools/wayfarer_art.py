"""The wayfarer's runes (the fx-explore pack): their icons, recipes and player text.

Behaviour is in src/main/java/dev/wildercord/cast/WayfarerEffects.java, numbers in spell/WayfarerRules.java.
item_art merges GLYPHS, and generate_assets merges RECIPES (into RUNE_RECIPES) and LANG.

GLYPHS use item_art's format: a pictogram at most 9 wide and 9 tall, "." empty, "-" shade, "#" main,
"+" light, "*" core (the stone's own glow colours), "o" darkens the stone, "k" near-black, "w" white.
"""

GLYPHS: dict[str, str] = {
    # Land Reading: a hill line with a marker flag planted on its crest.
    "land_reading": """
        ....+....
        ....#*...
        ....#**..
        ....#....
        ...###...
        ..#+++#..
        .#+...+#.
        #+.....+#
        ---------
    """,
    # Depth Sounding: a plumb line dropping through rock strata to a bright point.
    "depth_sounding": """
        ...###...
        ....#....
        ----#----
        ....#....
        ----#----
        ....#....
        ----+----
        ...+*+...
        ....+....
    """,
    # Spawn Bearing: a compass needle pointing to a star.
    "spawn_bearing": """
        ....*....
        ...*+*...
        ....*....
        ....#....
        ...#+#...
        ..#+.+#..
        ...#+#...
        ....#....
        ....-....
    """,
    # Home Bearing: a little house with an arrow pointing to its door.
    "home_bearing": """
        ....#....
        ...#+#...
        ..#+++#..
        .#+++++#.
        ..#...#..
        ..#.*.#..
        ..#.*.#..
        ..#####..
        ....+....
    """,
    # Grave Bearing: a headstone with a cross and a faint arrow beneath.
    "grave_bearing": """
        ..#####..
        .#+++++#.
        .#++*++#.
        .#+***+#.
        .#++*++#.
        .#+++++#.
        .#+++++#.
        #########
        -.......-
    """,
    # Portal Reckoning: two portal frames, small and large, joined by a dotted line.
    "portal_reckoning": """
        ###......
        #*#......
        #*#......
        ###.+....
        ....+.###
        .....+#*#
        ......#*#
        ......#*#
        ......###
    """,
    # Slime Sense: a slime cube with a bright eye, sitting on a chunk grid.
    "slime_sense": """
        .........
        ..#####..
        .#+++++#.
        .#+k+k+#.
        .#+++++#.
        .#++*++#.
        ..#####..
        -.-.-.-.-
        .-.-.-.-.
    """,
    # Sky Reading: a cloud with rain falling from it.
    "sky_reading": """
        ...###...
        ..#+++#..
        .#+++++##
        #+++++++#
        .#######.
        ..*..*...
        ...*..*..
        ..*..*...
        .........
    """,
    # Moon Reading: a crescent moon with a small star.
    "moon_reading": """
        ...###...
        ..##.....
        .##....*.
        .##......
        .##......
        .##......
        ..##.....
        ...###...
        .........
    """,
    # Sun Reading: a sun on a horizon with rays.
    "sun_reading": """
        ....+....
        .+..+..+.
        ..+###+..
        ..#***#..
        ++#***#++
        ..#***#..
        ..+###+..
        ---------
        .........
    """,
    # Lux Reading: an eye with light rays from above.
    "lux_reading": """
        .+..+..+.
        ..+.+.+..
        .........
        ..#####..
        .#+...+#.
        #+.*k*.+#
        .#+...+#.
        ..#####..
        .........
    """,
    # Dust Line: a dotted line between two pegs.
    "chalk_line": """
        .........
        #.......#
        #+......#
        #.+.....#
        #..+....#
        #...+...#
        #....+..#
        #.....+*#
        #.......#
    """,
    # Village Sense: a bell under a peaked roof.
    "village_sense": """
        ....#....
        ...#+#...
        ..#+++#..
        .#.....#.
        ...#*#...
        ..#***#..
        ..#***#..
        .#######.
        ....+....
    """,
    # Ruin Sense: broken pillars and a fallen block.
    "ruin_sense": """
        .........
        .##...#..
        .#+...#+.
        .#+...#+.
        .#+.*.#+.
        .#+...##.
        .#+.##...
        #########
        -.-.-.-.-
    """,
    # Shipwreck Sense: a tilted mast over waves.
    "shipwreck_sense": """
        ....#....
        ....#+...
        ....#++..
        ....#+++.
        ....#....
        .#######.
        ..#***#..
        -+-+-+-+-
        +-+-+-+-+
    """,
    # Portal Sense: a broken portal frame with crying obsidian.
    "portal_sense": """
        .###.##..
        .#.....#.
        .#.***.#.
        .#.***.#.
        ...***.#.
        .#.***...
        .#.....#.
        .##.####.
        .........
    """,
    # Fortress Sense: nether brick towers with a flame between.
    "fortress_sense": """
        #.#...#.#
        ###...###
        #+#.*.#+#
        #+#***#+#
        #+#.*.#+#
        #########
        #+#+#+#+#
        #########
        .........
    """,
    # Stronghold Compass: an ender eye on a compass rose.
    "stronghold_compass": """
        ....#....
        ....#....
        ...###...
        ..#+*+#..
        ###*k*###
        ..#+*+#..
        ...###...
        ....#....
        ....#....
    """,
    # Spire Sense: an end city spire with a purple pennant.
    "spire_sense": """
        ....#**..
        ....#*...
        ...###...
        ...#+#...
        ..##+##..
        ..#+++#..
        .##+++##.
        .#+++++#.
        #########
    """,
    # Trail Blaze: an end rod upright with a glow.
    "trail_blaze": """
        ....+....
        ...+*+...
        ....w....
        ....w....
        ....w....
        ....w....
        ....w....
        ...###...
        ..#####..
    """,
    # Relic Sense: a pot shard glowing in sand.
    "relic_sense": """
        .+.....+.
        ..+...+..
        ...###...
        ..#*+*#..
        ..#+*+#..
        ...###...
        ---------
        -.-.-.-.-
        .-.-.-.-.
    """,
    # Spawner Sense: a caged block with a flame inside.
    "spawner_sense": """
        #########
        #.#.#.#.#
        #########
        #.#.*.#.#
        ###***###
        #.#***#.#
        #########
        #.#.#.#.#
        #########
    """,
    # Steady Brush: a brush sweeping across a block.
    "steady_brush": """
        .......##
        ......#+#
        .....#+#.
        ....#+#..
        ..+##+...
        .+++#....
        +++++....
        .+++.-.-.
        .-.-.-.-.
    """,
    # Appraise: a magnifier over an emerald.
    "appraise": """
        ..####...
        .#....#..
        #..**..#.
        #.*++*.#.
        #..**..#.
        .#....#..
        ..####+..
        ......++.
        .......++
    """,
    # Trade Renew: a looping arrow round a crate.
    "trade_renew": """
        ..++++...
        .+....+..
        +......+.
        +.####.++
        +.#**#..+
        +.#**#..+
        ..####..+
        .+.....+.
        ..+++++..
    """,
    # Haggle: two hands clasped, an emerald above.
    "haggle": """
        ....*....
        ...*+*...
        ....*....
        .........
        ##.....##
        #+#...#+#
        .#+###+#.
        ..#+++#..
        ...###...
    """,
    # Folk Call: a bell ringing with sound arcs.
    "folk_call": """
        ....#....
        ...###...
        +.#+++#.+
        .+#+++#+.
        +.#+++#.+
        .#+++++#.
        #########
        ....*....
        .........
    """,
    # Folk Census: four little heads in a row with a tally.
    "folk_census": """
        .........
        .#.#.#.#.
        #+##+##+#
        .#.#.#.#.
        .........
        .+.+.+.+.
        .+.+.+.+.
        .+.+.+.+.
        *********
    """,
    # Lapis Thrift: a lapis gem with a return arrow.
    "lapis_thrift": """
        ....#....
        ...#*#...
        ..#***#..
        .#**+**#.
        ..#***#..
        ...#*#...
        +...#...+
        .+.....+.
        ..+++++..
    """,
    # Quickbrew: a brewing bottle with speed lines.
    "quickbrew": """
        ....##...
        ....##...
        ...#..#..
        +.#....#.
        .+#****#.
        +.#****#.
        .+#****#.
        ...####..
        .........
    """,
    # Potion Steep: a bottle with a clock face.
    "potion_steep": """
        ...##....
        ...##....
        ..#..#...
        .#....#..
        .#*+**#..
        .#*+++#..
        .#****#..
        ..####...
        .........
    """,
    # Lore Reading: an open book with a star above.
    "lore_reading": """
        ....*....
        ...*+*...
        ....*....
        .........
        ##.....##
        #+##.##+#
        #++#.#++#
        #+++#+++#
        ####.####
    """,
    # Shelf Count: a bookshelf with counted books.
    "shelf_count": """
        #########
        #+.+.+.+#
        #+*+*+.+#
        #########
        #.+.+*+.#
        #.+*+.+.#
        #########
        ..#...#..
        .........
    """,
    # Beacon Swell: a beacon with its beam spreading wide.
    "beacon_swell": """
        +...*...+
        .+..*..+.
        ..+.*.+..
        ...+*+...
        ...###...
        ..#*w*#..
        ..#***#..
        .#######.
        #########
    """,
    # Dye Wash: a splash of colour over a block.
    "dye_wash": """
        ....*....
        ...***...
        ..*****..
        ..*****..
        ...***...
        .........
        .#######.
        .#+++++#.
        .#######.
    """,
    # Checker Dye: a checkered square.
    "checker_dye": """
        #########
        #*+*+*+*#
        #+*+*+*+#
        #*+*+*+*#
        #+*+*+*+#
        #*+*+*+*#
        #+*+*+*+#
        #*+*+*+*#
        #########
    """,
    # Glyph Carve: a sign on a post with a star carved in.
    "glyph_carve": """
        #########
        #+++++++#
        #+++*+++#
        #++***++#
        #+++*+++#
        #########
        ....#....
        ....#....
        ....#....
    """,
    # Lamplighter: a candle with a bright flame.
    "lamplighter": """
        ....*....
        ...*+*...
        ...*+*...
        ....#....
        ...###...
        ...#+#...
        ...#+#...
        ...#+#...
        ..#####..
    """,
    # Douse: a candle with smoke curling up.
    "snuff_out": """
        ....-....
        .....-...
        ....-....
        ...-.....
        ....#....
        ...###...
        ...#+#...
        ...#+#...
        ..#####..
    """,
    # Sign Glow: a sign with glowing lines.
    "sign_glow": """
        +.......+
        #########
        #.*****.#
        #.......#
        #.*****.#
        #########
        ....#....
        ....#....
        ....#....
    """,
    # Frame Veil: an item frame half faded.
    "frame_veil": """
        #########
        #+-+-+-+#
        #-.....-#
        #+..*..+#
        #-.***.-#
        #+..*..+#
        #-.....-#
        #+-+-+-+#
        -.-.-.-.-
    """,
    # Stand Pose: an armor stand with arms raised.
    "stand_pose": """
        ....#....
        ...#*#...
        #...#...#
        .#.###.#.
        ..#+#+#..
        ....#....
        ...#.#...
        ..#...#..
        .#######.
    """,
    # Lava Crust: a basalt slab over lava waves.
    "lava_crust": """
        .........
        .........
        #########
        #+++++++#
        #########
        *.*.*.*.*
        .*.*.*.*.
        *********
        .........
    """,
    # Void Step: a platform floating over the void with stars.
    "void_step": """
        .........
        ....#....
        ...#+#...
        ..#####..
        .#+++++#.
        ..#####..
        .*.....*.
        ...*.*...
        *.......*
    """,
    # Lava Sense: an eye over a lava drop.
    "lava_sense": """
        ..#####..
        .#+...+#.
        #+.*k*.+#
        .#+...+#.
        ..#####..
        ....*....
        ...***...
        ..*****..
        ...***...
    """,
    # Gold Parley: a gold ingot held between two tusks.
    "gold_parley": """
        #.......#
        #+.....+#
        .#+...+#.
        .........
        ..#####..
        .#*****#.
        #*******#
        #########
        .........
    """,
}

# Tier I-III recipes: a Blank Rune, these themed items, then generate_assets' tier catalysts.
RECIPES: dict[str, list[str]] = {
    "land_reading": ["minecraft:dirt", "minecraft:compass"],
    "depth_sounding": ["minecraft:pointed_dripstone", "minecraft:string", "minecraft:cobbled_deepslate"],
    "spawn_bearing": ["minecraft:compass", "minecraft:feather"],
    "home_bearing": ["minecraft:compass", "minecraft:white_wool"],
    "grave_bearing": ["minecraft:compass", "minecraft:bone", "minecraft:soul_sand"],
    "portal_reckoning": ["minecraft:obsidian", "minecraft:map"],
    "slime_sense": ["minecraft:slime_ball", "minecraft:compass"],
    "sky_reading": ["minecraft:feather", "minecraft:glass_bottle"],
    "moon_reading": ["minecraft:clock", "minecraft:glow_ink_sac"],
    "sun_reading": ["minecraft:clock", "minecraft:sunflower", "minecraft:paper"],
    "lux_reading": ["minecraft:torch", "minecraft:glass_pane"],
    "chalk_line": ["minecraft:bone_meal", "minecraft:string", "minecraft:flint"],
    "village_sense": ["minecraft:map", "minecraft:emerald"],
    "ruin_sense": ["minecraft:map", "minecraft:brush"],
    "shipwreck_sense": ["minecraft:map", "minecraft:oak_boat"],
    "portal_sense": ["minecraft:map", "minecraft:obsidian"],
    "fortress_sense": ["minecraft:map", "minecraft:nether_bricks", "minecraft:blaze_powder"],
    "stronghold_compass": ["minecraft:compass", "minecraft:ender_pearl", "minecraft:blaze_powder"],
    "spire_sense": ["minecraft:map", "minecraft:chorus_fruit", "minecraft:end_stone"],
    "trail_blaze": ["minecraft:torch", "minecraft:stick", "minecraft:flint"],
    "relic_sense": ["minecraft:brush", "minecraft:sand"],
    "spawner_sense": ["minecraft:rotten_flesh", "minecraft:iron_bars"],
    "steady_brush": ["minecraft:brush", "minecraft:feather", "minecraft:gravel"],
    "appraise": ["minecraft:emerald", "minecraft:glass_pane"],
    "trade_renew": ["minecraft:emerald", "minecraft:clock", "minecraft:barrel"],
    "haggle": ["minecraft:emerald", "minecraft:emerald", "minecraft:white_banner"],
    "folk_call": ["minecraft:emerald", "minecraft:note_block"],
    "folk_census": ["minecraft:emerald", "minecraft:paper"],
    "lapis_thrift": ["minecraft:lapis_lazuli", "minecraft:book"],
    "quickbrew": ["minecraft:blaze_powder", "minecraft:glass_bottle", "minecraft:sugar"],
    "potion_steep": ["minecraft:glass_bottle", "minecraft:nether_wart", "minecraft:redstone"],
    "lore_reading": ["minecraft:book", "minecraft:feather"],
    "shelf_count": ["minecraft:book", "minecraft:oak_planks"],
    "beacon_swell": ["minecraft:glowstone_dust", "minecraft:iron_ingot", "minecraft:prismarine_crystals"],
    "dye_wash": ["minecraft:red_dye", "minecraft:yellow_dye", "minecraft:blue_dye"],
    "checker_dye": ["minecraft:white_dye", "minecraft:black_dye"],
    "glyph_carve": ["minecraft:oak_sign", "minecraft:flint"],
    "lamplighter": ["minecraft:candle", "minecraft:flint"],
    "snuff_out": ["minecraft:candle", "minecraft:feather"],
    "sign_glow": ["minecraft:oak_sign", "minecraft:glow_ink_sac"],
    "frame_veil": ["minecraft:item_frame", "minecraft:glass_pane"],
    "stand_pose": ["minecraft:armor_stand", "minecraft:stick"],
    "lava_crust": ["minecraft:basalt", "minecraft:snowball", "minecraft:magma_cream"],
    "void_step": ["minecraft:end_stone", "minecraft:feather", "minecraft:ender_pearl"],
    "lava_sense": ["minecraft:magma_cream", "minecraft:spider_eye"],
    "gold_parley": ["minecraft:gold_ingot", "minecraft:gold_nugget", "minecraft:porkchop"],
}

_W = "message.wildercord.wayfarer."

LANG: dict[str, str] = {
    _W + "land_reading": "%1$s, %2$s (%3$s). Ground at y %4$s, %5$s against sea level.",
    _W + "warmth.frozen": "frozen", _W + "warmth.cool": "cool", _W + "warmth.mild": "mild", _W + "warmth.hot": "hot",
    _W + "depth_sounding.cave": "Open cave %s blocks below.",
    _W + "depth_sounding.lava": "Lava %s blocks below!",
    _W + "depth_sounding.solid": "Solid rock for 64 blocks down.",
    _W + "spawn_bearing": "World spawn: %s, about %s blocks.",
    _W + "spawn_bearing.elsewhere": "World spawn is in another realm.",
    _W + "home_bearing": "Home: %s, about %s blocks.",
    _W + "home_bearing.none": "You have no bed or anchor set.",
    _W + "home_bearing.elsewhere": "Your home is in another realm.",
    _W + "grave_bearing": "Where you fell: %s, about %s blocks.",
    _W + "grave_bearing.none": "You have not fallen yet.",
    _W + "grave_bearing.elsewhere": "Where you fell is in another realm.",
    _W + "portal_reckoning.nether": "In the Nether this is x %s, z %s.",
    _W + "portal_reckoning.overworld": "In the Overworld this is x %s, z %s.",
    _W + "portal_reckoning.none": "This realm has no twin.",
    _W + "slime_sense.yes": "Slimes can spawn below in this chunk.",
    _W + "slime_sense.no": "No slimes spawn in this chunk.",
    _W + "slime_sense.none": "Slime chunks are only in the Overworld.",
    _W + "sky_reading.clear": "Clear sky, rain in about %s min.",
    _W + "sky_reading.held": "Clear sky, held for about %s min.",
    _W + "sky_reading.rain": "Rain, clearing in about %s min.",
    _W + "sky_reading.storm": "Thunderstorm, clearing in about %s min.",
    _W + "sky_reading.none": "There is no sky to read here.",
    _W + "moon_reading": "The moon is %s; %s nights to full.",
    _W + "moon_reading.none": "There is no moon here.",
    _W + "moon.0": "full", _W + "moon.1": "waning gibbous", _W + "moon.2": "at last quarter", _W + "moon.3": "a waning crescent",
    _W + "moon.4": "new", _W + "moon.5": "a waxing crescent", _W + "moon.6": "at first quarter", _W + "moon.7": "waxing gibbous",
    _W + "sun_reading.day": "Day, %s. Dusk in about %s min.",
    _W + "sun_reading.night": "Night, %s. Dawn in about %s min.",
    _W + "sun_reading.none": "There is no sun here.",
    _W + "lux_reading.lit": "Block light %s, sky light %s.",
    _W + "lux_reading.dark": "Block light %s, sky light %s: monsters may spawn here.",
    _W + "chalk_line": "Chalk line: %s blocks.",
    _W + "locate_resting": "Your senses rest for %s more seconds.",
    _W + "village_sense": "Village: %s, about %s blocks.",
    _W + "village_sense.none": "No village within reach.",
    _W + "village_sense.realm": "No villages here.",
    _W + "ruin_sense": "Trail ruins: %s, about %s blocks.",
    _W + "ruin_sense.none": "No trail ruins within reach.",
    _W + "ruin_sense.realm": "Trail ruins are only in the Overworld.",
    _W + "shipwreck_sense": "Shipwreck: %s, about %s blocks.",
    _W + "shipwreck_sense.none": "No shipwreck within reach.",
    _W + "shipwreck_sense.realm": "Shipwrecks are only in the Overworld.",
    _W + "portal_sense": "Ruined portal: %s, about %s blocks.",
    _W + "portal_sense.none": "No ruined portal within reach.",
    _W + "portal_sense.realm": "No ruined portals here.",
    _W + "fortress_sense": "Fortress: %s, about %s blocks.",
    _W + "fortress_sense.none": "No fortress within reach.",
    _W + "fortress_sense.realm": "Fortresses are only in the Nether.",
    _W + "stronghold_compass": "The stronghold lies %s.",
    _W + "stronghold_compass.none": "No stronghold within reach.",
    _W + "stronghold_compass.realm": "Strongholds are only in the Overworld.",
    _W + "spire_sense": "End city: %s, about %s blocks.",
    _W + "spire_sense.none": "No End city within reach.",
    _W + "spire_sense.realm": "End cities are only in the End.",
    _W + "trail_blaze.blocked": "No room for a blaze there.",
    _W + "relic_sense": "%s suspicious blocks nearby.",
    _W + "relic_sense.none": "No suspicious blocks nearby.",
    _W + "spawner_sense": "%s spawners nearby; nearest %s, about %s blocks.",
    _W + "spawner_sense.none": "No spawners nearby.",
    _W + "steady_brush": "Brushing %s blocks.",
    _W + "steady_brush.no_brush": "Hold a brush.",
    _W + "steady_brush.none": "Nothing here to brush.",
    _W + "appraise": "%s, level %s: %s of %s trades sold out.",
    _W + "appraise.none": "No villager there.",
    _W + "appraise.idle": "That villager has no trade.",
    _W + "restock": "Their trades are restocked.",
    _W + "restock.done": "They already restocked today.",
    _W + "folk_call": "%s villagers come.",
    _W + "folk_census": "%s with a trade, %s without, %s nitwits, %s children, %s golems.",
    _W + "lapis_thrift": "Your next enchantment gives a lapis back.",
    _W + "lapis_thrift.refund": "A lapis comes back to you.",
    _W + "quickbrew": "%s brewing stands quicken.",
    _W + "quickbrew.none": "No brewing stand nearby.",
    _W + "potion_steep": "%s effects steeped longer.",
    _W + "potion_steep.none": "No good effect to steep.",
    _W + "lore_reading": "%s: %s enchantments, anvil cost %s.",
    _W + "lore_reading.costly": "%s: %s enchantments, anvil cost %s: too expensive to work.",
    _W + "lore_reading.empty": "Hold the item to read.",
    _W + "shelf_count": "%s of 15 bookshelves feed it; %s blocked.",
    _W + "shelf_count.none": "No enchanting table nearby.",
    _W + "beacon_swell": "%s beacons reach farther.",
    _W + "beacon_swell.none": "No beacon nearby.",
    _W + "dye_wash": "%s blocks dyed.",
    _W + "dye_wash.no_dye": "Hold a dye in your other hand.",
    _W + "dye_wash.none": "Nothing here takes that dye.",
    _W + "glyph_carve": "Glyph carved.",
    _W + "glyph_carve.none": "No sign there.",
    _W + "glyph_carve.sealed": "That sign is waxed or protected.",
    _W + "glyph_carve.full": "That side of the sign is full.",
    _W + "lamplighter": "%s lit.",
    _W + "lamplighter.none": "Nothing to light nearby.",
    _W + "snuff_out": "%s put out.",
    _W + "snuff_out.none": "Nothing to put out nearby.",
    _W + "sign_glow": "%s signs glow.",
    _W + "sign_glow.none": "No signs nearby.",
    _W + "frame_veil.hidden": "%s frames hidden.",
    _W + "frame_veil.shown": "%s frames shown.",
    _W + "frame_veil.none": "No filled frames nearby.",
    _W + "stand_pose": "%s armor stands posed.",
    _W + "stand_pose.none": "No armor stands nearby.",
    _W + "lava_crust.none": "No lava to crust there.",
    _W + "void_step.none": "There is no room under you.",
    _W + "lava_sense": "%s lava within reach; nearest %s, %s blocks away, height %s.",
    _W + "lava_sense.none": "No lava within %s blocks.",
    _W + "gold_parley": "%s piglins calm down.",
    _W + "gold_parley.none": "No piglin is angry at you.",
}
