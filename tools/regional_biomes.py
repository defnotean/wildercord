"""Authored regional biome data for Minecraft 26.3's attribute-based biome format."""

LANG = {
    "biome.wildercord.luminous_reedlands": "Luminous Reedlands",
    "journal.wildercord.entry.place.luminous_reedlands": "Luminous Reedlands. Pale banks, blue-green pools, and Moonreed opening to the night. The Tideward call these lights the lamps of the first crossing: leave a flower standing for the next traveller. Lantern Newts shelter in the shallows; Glimmerwings carry pollen between the reeds. Look for smoke above the water to find a fisher's stilt house.",
    "biome.wildercord.windscar_highlands": "Windscar Highlands",
    "journal.wildercord.entry.place.windscar_highlands": "Windscar Highlands. Gold-green grass follows the dry shelves between exposed stone ridges. The Marchkeepers say the wind never took a road; it only found the paths people had stopped tending. Take the ripe tassels of Windreed and leave its root for the next walker. Stonehorns graze the slopes, while a Galeclaw's warning carries farther than its footsteps. Watchtowers, standing stones and the Wind Gate can mark a way through the heights.",
    "biome.wildercord.belowkeeper_hollows": "Belowkeeper Hollows",
    "journal.wildercord.entry.place.belowkeeper_hollows": "Belowkeeper Hollows. Damp clay basins and copper-stained dripstone vaults where water carries the hum of the lower rock. Mara's listeners kept Glowcap beds under the stone arches so every watch had filter gills and steady light. Sporeback Snails, Mossveil Dormice and Rootmolt Striders share the dark, while Listening Stations, Drainhouses and the Bell Sanctum still hold the Belowkeepers' acoustic logs.",
    "biome.wildercord.emberwood_reach": "Emberwood Reach",
    "journal.wildercord.entry.place.emberwood_reach": "Emberwood Reach. Scorched oak woods and coarse-dirt firebreaks where Ashwin's charcoal burners cut timber for the southern forges. Harvest the cooled fronds of Cinder Fern and leave the root for the Cinder Bailiffs that patrol the ash beds. Cinderfoxes and Dune Cougars hunt the warm borders, while Charcoal Yards, Forge Dojos and the Kiln Court still keep the burners' tallies.",
}


def _spawn(kind, weight, low, high=None):
    count = low if high is None else {"type": "minecraft:uniform", "min_inclusive": low, "max_inclusive": high}
    return {"type": "minecraft:" + kind, "weight": weight, "count": count}


def reedlands():
    """Swamp terrain and feature order, with cool water, pale banks and wetland fauna.

    Wildercord's existing biome modifiers add moonreeds, newts, crabs and bitterns.
    Reusing the same placed features avoids reseeding decoration across other biomes.
    """
    return {
        "attributes": {
            "minecraft:audio/background_music": {"default": {
                "max_delay": 24000, "min_delay": 12000, "sound": "minecraft:music.overworld.swamp"}},
            "minecraft:gameplay/increased_fire_burnout": True,
            "minecraft:gameplay/natural_mob_spawns": {"modifier": "overlay", "argument": {
                "spawn_costs": {},
                "spawns_by_category": {
                    "ambient": [_spawn("bat", 10, 8)],
                    "creature": [_spawn("frog", 10, 2, 5)],
                    "monster": [_spawn("spider", 100, 4), _spawn("zombie", 95, 4),
                                _spawn("zombie_villager", 5, 1), _spawn("skeleton", 70, 4),
                                _spawn("creeper", 100, 4), _spawn("slime", 100, 4),
                                _spawn("enderman", 10, 1, 4), _spawn("witch", 5, 1),
                                _spawn("slime", 1, 1)],
                    "underground_water_creature": [_spawn("glow_squid", 10, 4, 6)],
                },
            }},
            "minecraft:visual/sky_color": "#84acc3",
            "minecraft:visual/water_fog_color": "#245257",
            "minecraft:visual/water_fog_end_distance": {"argument": 0.85, "modifier": "multiply"},
        },
        "carvers": ["minecraft:cave", "minecraft:cave_extra_underground", "minecraft:canyon"],
        "downfall": 0.95,
        "effects": {
            "dry_foliage_color": "#887354", "foliage_color": "#5b927f",
            "grass_color": "#78a994", "grass_color_modifier": "none", "water_color": "#3c8b91",
        },
        "features": [
            [],
            ["minecraft:lake_lava_underground", "minecraft:lake_lava_surface"],
            ["minecraft:amethyst_geode"],
            ["minecraft:fossil_upper", "minecraft:fossil_lower", "minecraft:monster_room", "minecraft:monster_room_deep"],
            [], [],
            ["minecraft:" + name for name in (
                "ore_dirt", "ore_gravel", "ore_granite_upper", "ore_granite_lower",
                "ore_diorite_upper", "ore_diorite_lower", "ore_andesite_upper", "ore_andesite_lower",
                "ore_tuff", "ore_coal_upper", "ore_coal_lower", "ore_iron_upper", "ore_iron_middle",
                "ore_iron_small", "ore_gold", "ore_gold_lower", "ore_redstone", "ore_redstone_lower",
                "ore_diamond", "ore_diamond_medium", "ore_diamond_large", "ore_diamond_buried",
                "ore_lapis", "ore_lapis_buried", "ore_copper", "underwater_magma", "disk_clay")],
            [],
            ["minecraft:spring_water", "minecraft:spring_lava"],
            ["minecraft:" + name for name in (
                "glow_lichen", "trees_swamp", "flower_swamp", "patch_grass_normal", "patch_dead_bush",
                "patch_waterlily", "brown_mushroom_swamp", "red_mushroom_swamp", "brown_mushroom_normal",
                "red_mushroom_normal", "patch_sugar_cane_swamp", "patch_pumpkin", "patch_firefly_bush_swamp",
                "patch_firefly_bush_near_water_swamp", "seagrass_swamp")],
            ["minecraft:freeze_top_layer"],
        ],
        "has_precipitation": True,
        "temperature": 0.65,
    }


def windscar():
    """Dry hill country with the parent's ordered ores, sparse trees and open grass.

    Windreed and native highland animals join through their existing biome modifiers.
    Keeping shared features in vanilla order prevents cross-biome decoration cycles.
    """
    return {
        "attributes": {
            "minecraft:audio/background_music": {"default": {
                "max_delay": 24000, "min_delay": 12000, "sound": "minecraft:music.overworld.meadow"}},
            "minecraft:gameplay/natural_mob_spawns": {"modifier": "overlay", "argument": {
                "spawn_costs": {},
                "spawns_by_category": {
                    "ambient": [_spawn("bat", 10, 8)],
                    "creature": [_spawn("sheep", 12, 4), _spawn("pig", 10, 4),
                                 _spawn("chicken", 10, 4), _spawn("cow", 8, 4), _spawn("llama", 5, 4, 6)],
                    "monster": [_spawn("spider", 100, 4), _spawn("zombie", 95, 4),
                                _spawn("zombie_villager", 5, 1), _spawn("skeleton", 100, 4),
                                _spawn("creeper", 100, 4), _spawn("slime", 100, 4),
                                _spawn("enderman", 10, 1, 4), _spawn("witch", 5, 1)],
                    "underground_water_creature": [_spawn("glow_squid", 10, 4, 6)],
                },
            }},
            "minecraft:visual/sky_color": "#91afc5",
            "minecraft:visual/water_fog_color": "#344b50",
        },
        "carvers": ["minecraft:cave", "minecraft:cave_extra_underground", "minecraft:canyon"],
        "downfall": 0.25,
        "effects": {
            "dry_foliage_color": "#a68d5d", "foliage_color": "#84936a",
            "grass_color": "#a5aa78", "grass_color_modifier": "none", "water_color": "#527c88",
        },
        "features": [
            [],
            ["minecraft:lake_lava_underground", "minecraft:lake_lava_surface"],
            ["minecraft:amethyst_geode"],
            ["minecraft:monster_room", "minecraft:monster_room_deep"],
            [], [],
            ["minecraft:" + name for name in (
                "ore_dirt", "ore_gravel", "ore_granite_upper", "ore_granite_lower",
                "ore_diorite_upper", "ore_diorite_lower", "ore_andesite_upper", "ore_andesite_lower",
                "ore_tuff", "ore_coal_upper", "ore_coal_lower", "ore_iron_upper", "ore_iron_middle",
                "ore_iron_small", "ore_gold", "ore_gold_lower", "ore_redstone", "ore_redstone_lower",
                "ore_diamond", "ore_diamond_medium", "ore_diamond_large", "ore_diamond_buried",
                "ore_lapis", "ore_lapis_buried", "ore_copper", "underwater_magma", "disk_sand",
                "disk_clay", "disk_gravel", "ore_emerald")],
            ["minecraft:ore_infested"],
            ["minecraft:spring_water", "minecraft:spring_lava"],
            ["minecraft:" + name for name in (
                "glow_lichen", "trees_windswept_hills", "patch_bush", "flower_default", "patch_grass_badlands",
                "brown_mushroom_normal", "red_mushroom_normal", "patch_pumpkin", "patch_sugar_cane",
                "patch_firefly_bush_near_water")],
            ["minecraft:freeze_top_layer"],
        ],
        "has_precipitation": True,
        "temperature": 0.35,
    }


def belowkeeper():
    """Subterranean dripstone-and-clay vaults with damp moss floors for Glowcap beds.

    Keeps vanilla dripstone cave ores/clusters and lush clay/moss features in exact vanilla
    order so cross-biome chunk decoration never cycles.
    """
    return {
        "attributes": {
            "minecraft:audio/background_music": {"default": {
                "max_delay": 24000, "min_delay": 12000, "sound": "minecraft:music.overworld.dripstone_caves"}},
            "minecraft:gameplay/natural_mob_spawns": {"modifier": "overlay", "argument": {
                "spawn_costs": {},
                "spawns_by_category": {
                    "ambient": [_spawn("bat", 10, 8)],
                    "creature": [],
                    "monster": [_spawn("spider", 100, 4), _spawn("zombie", 95, 4),
                                _spawn("zombie_villager", 5, 1), _spawn("skeleton", 100, 4),
                                _spawn("creeper", 100, 4), _spawn("slime", 100, 4),
                                _spawn("enderman", 10, 1, 4), _spawn("witch", 5, 1),
                                _spawn("drowned", 95, 4)],
                    "underground_water_creature": [_spawn("glow_squid", 10, 4, 6)],
                },
            }},
            "minecraft:visual/sky_color": "#7ba4ff",
            "minecraft:visual/water_fog_color": "#284a4e",
        },
        "carvers": ["minecraft:cave", "minecraft:cave_extra_underground", "minecraft:canyon"],
        "downfall": 0.6,
        "effects": {
            "dry_foliage_color": "#7b6c52", "foliage_color": "#5f8c74",
            "grass_color": "#6f9c82", "grass_color_modifier": "none", "water_color": "#3a7f86",
        },
        "features": [
            [],
            ["minecraft:lake_lava_underground", "minecraft:lake_lava_surface"],
            ["minecraft:amethyst_geode", "minecraft:large_dripstone"],
            ["minecraft:monster_room", "minecraft:monster_room_deep"],
            [], [],
            ["minecraft:" + name for name in (
                "ore_dirt", "ore_gravel", "ore_granite_upper", "ore_granite_lower",
                "ore_diorite_upper", "ore_diorite_lower", "ore_andesite_upper", "ore_andesite_lower",
                "ore_tuff", "ore_coal_upper", "ore_coal_lower", "ore_iron_upper", "ore_iron_middle",
                "ore_iron_small", "ore_gold", "ore_gold_lower", "ore_redstone", "ore_redstone_lower",
                "ore_diamond", "ore_diamond_medium", "ore_diamond_large", "ore_diamond_buried",
                "ore_lapis", "ore_lapis_buried", "ore_copper_large", "underwater_magma", "disk_sand",
                "disk_clay", "disk_gravel")],
            ["minecraft:dripstone_cluster", "minecraft:pointed_dripstone"],
            ["minecraft:spring_water", "minecraft:spring_lava"],
            ["minecraft:" + name for name in (
                "glow_lichen", "patch_tall_grass_2", "lush_caves_ceiling_vegetation", "cave_vines",
                "lush_caves_clay", "lush_caves_vegetation", "rooted_azalea_tree", "spore_blossom",
                "classic_vines_cave_feature")],
            ["minecraft:freeze_top_layer"],
        ],
        "has_precipitation": True,
        "temperature": 0.7,
    }


def emberwood():
    """Warm scorched oak-and-ash woodland with vanilla forest decoration order.

    Cinder Fern patches, Cinder Bailiffs, Cinderfoxes and Dune Cougars join through their
    existing biome modifiers. Keeping shared features in vanilla forest order prevents
    cross-biome decoration cycles.
    """
    return {
        "attributes": {
            "minecraft:audio/background_music": {"default": {
                "max_delay": 24000, "min_delay": 12000, "sound": "minecraft:music.overworld.forest"}},
            "minecraft:gameplay/natural_mob_spawns": {"modifier": "overlay", "argument": {
                "spawn_costs": {},
                "spawns_by_category": {
                    "ambient": [_spawn("bat", 10, 8)],
                    "creature": [_spawn("sheep", 12, 4), _spawn("pig", 10, 4),
                                 _spawn("chicken", 10, 4), _spawn("cow", 8, 4),
                                 _spawn("wolf", 5, 4)],
                    "monster": [_spawn("spider", 100, 4), _spawn("zombie", 95, 4),
                                _spawn("zombie_villager", 5, 1), _spawn("skeleton", 100, 4),
                                _spawn("creeper", 100, 4), _spawn("slime", 100, 4),
                                _spawn("enderman", 10, 1, 4), _spawn("witch", 5, 1)],
                    "underground_water_creature": [_spawn("glow_squid", 10, 4, 6)],
                },
            }},
            "minecraft:visual/sky_color": "#b49774",
            "minecraft:visual/water_fog_color": "#3d332a",
        },
        "carvers": ["minecraft:cave", "minecraft:cave_extra_underground", "minecraft:canyon"],
        "downfall": 0.35,
        "effects": {
            "dry_foliage_color": "#9b6438", "foliage_color": "#8f713f",
            "grass_color": "#9a8248", "grass_color_modifier": "none", "water_color": "#5d6a64",
        },
        "features": [
            [],
            ["minecraft:lake_lava_underground", "minecraft:lake_lava_surface"],
            ["minecraft:amethyst_geode"],
            ["minecraft:monster_room", "minecraft:monster_room_deep"],
            [], [],
            ["minecraft:" + name for name in (
                "ore_dirt", "ore_gravel", "ore_granite_upper", "ore_granite_lower",
                "ore_diorite_upper", "ore_diorite_lower", "ore_andesite_upper", "ore_andesite_lower",
                "ore_tuff", "ore_coal_upper", "ore_coal_lower", "ore_iron_upper", "ore_iron_middle",
                "ore_iron_small", "ore_gold", "ore_gold_lower", "ore_redstone", "ore_redstone_lower",
                "ore_diamond", "ore_diamond_medium", "ore_diamond_large", "ore_diamond_buried",
                "ore_lapis", "ore_lapis_buried", "ore_copper", "underwater_magma", "disk_sand",
                "disk_clay", "disk_gravel")],
            [],
            ["minecraft:spring_water", "minecraft:spring_lava"],
            ["minecraft:" + name for name in (
                "glow_lichen", "forest_flowers", "trees_birch_and_oak_leaf_litter", "flower_default",
                "patch_grass_forest", "brown_mushroom_normal", "red_mushroom_normal", "patch_pumpkin",
                "patch_sugar_cane", "patch_berry_bush", "patch_firefly_bush_near_water")],
            ["minecraft:freeze_top_layer"],
        ],
        "has_precipitation": True,
        "temperature": 0.9,
    }


def write(g):
    g.write_json(g.DATA / "worldgen/biome/luminous_reedlands.json", reedlands())
    g.write_json(g.DATA / "worldgen/biome/windscar_highlands.json", windscar())
    g.write_json(g.DATA / "worldgen/biome/belowkeeper_hollows.json", belowkeeper())
    g.write_json(g.DATA / "worldgen/biome/emberwood_reach.json", emberwood())
    # Appending to these tags preserves vanilla members and other packs' additions.
    for name, members in {
        "is_overworld": ["wildercord:luminous_reedlands", "wildercord:windscar_highlands", "wildercord:belowkeeper_hollows", "wildercord:emberwood_reach"],
        "is_swamp": ["wildercord:luminous_reedlands"],
        "is_hill": ["wildercord:windscar_highlands"],
        "is_forest": ["wildercord:emberwood_reach"],
    }.items():
        g.write_json(g.RES / "data/minecraft/tags/worldgen/biome" / (name + ".json"),
                     {"replace": False, "values": members})
