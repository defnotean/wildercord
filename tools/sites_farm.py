"""The farmstead sites pack: eight peaceful working places (dev.wildercord.world.sites.farm).

write(g) writes each site's structure, structure set, biome tag, chest loot and "find it" advancement through the
generate_assets module g, and returns the site ids. The pieces themselves are Java (FarmSites and its pieces).
"""

SALT = 2026100900 + 2 * 100  # pack 2: each site adds its own n

# id: (n, spacing, separation, terrain adaptation, biomes, advancement icon, title, description)
SITES = {
    "farm_windmill": (1, 44, 14, "beard_thin", ["plains", "sunflower_plains", "meadow", "savanna"],
                      "minecraft:wheat", "Grist for the Mill", "Find a windmill farmstead"),
    "farm_herbalist": (2, 40, 12, "beard_thin", ["forest", "flower_forest", "birch_forest", "old_growth_birch_forest", "cherry_grove"],
                       "minecraft:brewing_stand", "Simples", "Find an herbalist's cottage"),
    "farm_apiary": (3, 40, 12, "beard_thin", ["flower_forest", "meadow", "sunflower_plains", "cherry_grove"],
                    "minecraft:beehive", "Smoke and Honey", "Find a beekeeper's apiary"),
    "farm_orchard": (4, 46, 14, "beard_thin", ["plains", "forest", "birch_forest", "flower_forest"],
                     "minecraft:apple", "Behind the Wall", "Find a walled orchard"),
    "farm_shepherd": (5, 40, 12, "beard_thin", ["meadow", "windswept_hills", "plains", "savanna_plateau"],
                      "minecraft:white_wool", "Count the Flock", "Find a shepherd's hut"),
    "farm_mushroom_ring": (6, 36, 10, "none", ["dark_forest", "old_growth_spruce_taiga"],
                           "minecraft:red_mushroom", "Down Among the Caps", "Find a sunken mushroom ring"),
    "farm_granary": (7, 48, 16, "beard_thin", ["plains", "sunflower_plains", "savanna", "taiga"],
                     "minecraft:hay_block", "Harvest Home", "Find a granary and its harvest shrine"),
    "farm_scarecrow": (8, 42, 12, "beard_thin", ["plains", "savanna", "taiga"],
                       "minecraft:carved_pumpkin", "Do Not Linger", "Find an abandoned scarecrow field"),
}

# The night guard of the scarecrow field: the dead that rise from its furrows after dark.
NIGHT_GUARD = {"monster": {"bounding_box": "piece", "spawns": [
    {"type": "minecraft:zombie", "weight": 3, "count": 1},
    {"type": "minecraft:skeleton", "weight": 2, "count": 1},
]}}

LANG = {
    "advancements.wildercord.world.farm_sites.title": "Country Roads",
    "advancements.wildercord.world.farm_sites.description": "Find a farmstead, an orchard, an apiary or another working place in the wild",
    "sign.wildercord.farm_windmill.1": "The east field",
    "sign.wildercord.farm_windmill.2": "is dry. Press the",
    "sign.wildercord.farm_windmill.3": "well to water it.",
    "sign.wildercord.farm_apiary.1": "Keep the fire lit.",
    "sign.wildercord.farm_apiary.2": "Smoke calms the",
    "sign.wildercord.farm_apiary.3": "bees for harvest.",
    "sign.wildercord.farm_orchard.1": "What the garden",
    "sign.wildercord.farm_orchard.2": "gave, the roots",
    "sign.wildercord.farm_orchard.3": "keep. Dig there.",
    "sign.wildercord.farm_granary.1": "Share the harvest.",
    "sign.wildercord.farm_granary.2": "Take what you need,",
    "sign.wildercord.farm_granary.3": "ring the bell.",
    "sign.wildercord.farm_scarecrow.1": "Do not linger",
    "sign.wildercord.farm_scarecrow.2": "after dark.",
}
for _id, _s in SITES.items():
    LANG[f"advancements.wildercord.world.{_id}.title"] = _s[6]
    LANG[f"advancements.wildercord.world.{_id}.description"] = _s[7]


def _loot(g, rolls, runes, items, rune_weight=3):
    """One chest: a pool of everyday goods and a pool with one or two of the pack's runes."""
    return {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]},
         "entries": [g.item_entry(*i) for i in items]},
        {"rolls": {"type": "minecraft:uniform", "min": 1, "max": 2},
         "entries": [g.rune_entry(r, rune_weight) for r in runes] + [g.item_entry("wildercord:torn_page", 2)]},
    ]}


LOOT = {
    "farm_windmill": ((3, 6), ["tillage", "dewfall", "sow", "ripen", "ditchwater", "plowline", "tilth", "fieldsense"], [
        ("minecraft:wheat_seeds", 6, 3, 8), ("minecraft:bread", 5, 2, 4), ("minecraft:wheat", 5, 3, 9),
        ("minecraft:bone_meal", 4, 2, 6), ("minecraft:iron_hoe", 1), ("minecraft:bucket", 2)]),
    "farm_herbalist": ((3, 5), ["dew_drink", "quench", "clot", "heartsense", "sunbask", "savor", "slowburn", "berrybless"], [
        ("minecraft:glass_bottle", 5, 1, 3), ("minecraft:sweet_berries", 5, 2, 6), ("minecraft:glistering_melon_slice", 2),
        ("minecraft:nether_wart", 3, 1, 3), ("minecraft:blaze_powder", 1), ("minecraft:beetroot_seeds", 4, 2, 5)]),
    "farm_herbalist_loft": ((2, 4), ["berrybless", "wildflower", "savor"], [
        ("minecraft:dried_kelp", 5, 3, 8), ("minecraft:allium", 3), ("minecraft:cornflower", 3), ("minecraft:lily_of_the_valley", 3),
        ("minecraft:azalea", 2), ("minecraft:spore_blossom", 1)]),
    "farm_apiary": ((3, 5), ["pollinate", "hivehum", "calmsmoke", "honeydew", "wildflower", "beeline"], [
        ("minecraft:honeycomb", 5, 1, 4), ("minecraft:honey_bottle", 4, 1, 2), ("minecraft:glass_bottle", 4, 2, 4),
        ("minecraft:shears", 2), ("minecraft:campfire", 2), ("minecraft:sunflower", 3, 1, 2)]),
    "farm_orchard": ((3, 5), ["saplingrise", "saplingsow", "coppice", "leafshade", "sapflow", "leaffall"], [
        ("minecraft:apple", 6, 2, 5), ("minecraft:oak_sapling", 4, 1, 3), ("minecraft:cherry_sapling", 3, 1, 2),
        ("minecraft:birch_sapling", 3, 1, 3), ("minecraft:bone_meal", 3, 2, 5), ("minecraft:melon_seeds", 2, 1, 3)]),
    "farm_orchard_cache": ((2, 4), ["saplingrise", "barkhide", "feastday", "leafshade"], [
        ("minecraft:apple", 5, 2, 4), ("minecraft:golden_apple", 1), ("minecraft:azalea", 3, 1, 2),
        ("minecraft:flowering_azalea", 3, 1, 2), ("minecraft:emerald", 3, 1, 3)], 4),
    "farm_shepherd": ((3, 5), ["herdcall", "fleece", "fodder", "herdsense", "gentlehand", "barnwarmth", "trot", "courtship"], [
        ("minecraft:white_wool", 6, 2, 6), ("minecraft:shears", 2), ("minecraft:lead", 3, 1, 2), ("minecraft:mutton", 4, 1, 3),
        ("minecraft:wheat", 4, 2, 6), ("minecraft:string", 3, 2, 5)]),
    "farm_mushroom_ring": ((3, 5), ["compost", "fallow", "cloche", "stewpot", "hearthcook"], [
        ("minecraft:red_mushroom", 5, 2, 5), ("minecraft:brown_mushroom", 5, 2, 5), ("minecraft:bowl", 4, 1, 3),
        ("minecraft:mushroom_stew", 3), ("minecraft:mycelium", 2, 1, 2), ("minecraft:bone_meal", 3, 2, 5)]),
    "farm_granary": ((3, 6), ["feastday", "picnic", "hearthcook", "bakehouse", "stewpot", "savor"], [
        ("minecraft:bread", 6, 2, 5), ("minecraft:wheat", 5, 4, 10), ("minecraft:wheat_seeds", 4, 3, 8),
        ("minecraft:carrot", 4, 2, 5), ("minecraft:potato", 4, 2, 5), ("minecraft:hay_block", 2, 1, 2)]),
    "farm_granary_shrine": ((2, 4), ["feastday", "picnic", "hearthbond", "bakehouse"], [
        ("minecraft:pumpkin_pie", 5, 1, 3), ("minecraft:cake", 2), ("minecraft:cookie", 4, 3, 8), ("minecraft:candle", 3, 1, 3),
        ("minecraft:golden_carrot", 2, 1, 2), ("minecraft:emerald", 2, 1, 2)], 4),
    "farm_scarecrow": ((3, 5), ["scarecrow", "fieldsense", "thawfield", "gourdcall", "stalkrise", "cloche", "fallow"], [
        ("minecraft:wheat_seeds", 5, 2, 6), ("minecraft:pumpkin_seeds", 4, 1, 4), ("minecraft:melon_seeds", 3, 1, 4),
        ("minecraft:stone_hoe", 2), ("minecraft:rotten_flesh", 3, 1, 3), ("minecraft:carved_pumpkin", 2)]),
}


def write(g):
    for site, (n, spacing, separation, terrain, biomes, icon, title, description) in SITES.items():
        g.write_json(g.DATA / f"worldgen/structure/{site}.json", {
            "type": "wildercord:site", "site": site, "biomes": f"#wildercord:has_structure/{site}",
            "spawn_overrides": NIGHT_GUARD if site == "farm_scarecrow" else {},
            "step": "surface_structures", "terrain_adaptation": terrain})
        g.write_json(g.DATA / f"worldgen/structure_set/{site}.json", {
            "placement": {"type": "minecraft:random_spread", "salt": SALT + n, "spacing": spacing, "separation": separation},
            "structures": [{"structure": f"wildercord:{site}", "weight": 1}]})
        g.write_json(g.DATA / f"tags/worldgen/biome/has_structure/{site}.json", {"values": [f"minecraft:{b}" for b in biomes]})
        g.adv(f"world/{site}", "world/farm_sites", g.item(icon), title, description, g.in_structure(f"wildercord:{site}"), xp=20)
    for table, spec in LOOT.items():
        g.write_json(g.DATA / f"loot_table/chests/{table}.json", _loot(g, *spec))
    g.adv("world/farm_sites", "world/ley_line", g.item("minecraft:composter"), LANG["advancements.wildercord.world.farm_sites.title"],
          LANG["advancements.wildercord.world.farm_sites.description"],
          {site: g.in_structure(f"wildercord:{site}") for site in SITES}, any_of=True, xp=10)
    return list(SITES)
