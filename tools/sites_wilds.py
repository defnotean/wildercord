"""The wild places (sites-wilds pack): elemental ruins in the biomes where each element is strongest, the Nether and
the End too. Writes each site's structure, structure set, biome tag, loot and advancement. Built by
src/main/java/dev/wildercord/world/sites/wilds/."""

SALT = 2026100900 + 600

# id: (name, step, biomes, spacing, separation, advancement title, advancement text, icon item)
SITES = {
    "wilds_venom_ziggurat": ("Venom Ziggurat", "surface_structures",
                             ["minecraft:jungle", "minecraft:bamboo_jungle", "minecraft:sparse_jungle"], 36, 12,
                             "Poison Darts", "Find a Venom Ziggurat in the jungle", "minecraft:mossy_stone_bricks"),
    "wilds_dune_temple": ("Dune Temple", "surface_structures", ["minecraft:desert"], 34, 12,
                          "Shifting Sands", "Find a Dune Temple in the desert", "minecraft:chiseled_sandstone"),
    "wilds_rime_monastery": ("Rime Monastery", "surface_structures",
                             ["minecraft:snowy_plains", "minecraft:snowy_taiga", "minecraft:ice_spikes", "minecraft:grove"], 34, 12,
                             "The Cold Bell", "Find a Rime Monastery in the snow", "minecraft:bell"),
    "wilds_iron_gatehouse": ("Iron Gatehouse", "surface_structures",
                             ["minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands"], 40, 14,
                             "The Last Gate", "Find an Iron Gatehouse in the badlands", "minecraft:iron_bars"),
    "wilds_dawn_pavilion": ("Dawn Pavilion", "surface_structures", ["minecraft:cherry_grove"], 28, 10,
                            "First Light", "Find a Dawn Pavilion in a cherry grove", "minecraft:cherry_log"),
    "wilds_echo_post": ("Echo Post", "underground_structures", ["minecraft:deep_dark"], 30, 10,
                        "Listening In", "Find an Echo Post in the deep dark", "minecraft:calibrated_sculk_sensor"),
    "wilds_ember_outpost": ("Ember Outpost", "underground_structures",
                            ["minecraft:crimson_forest", "minecraft:warped_forest", "minecraft:nether_wastes"], 32, 12,
                            "Fire on the Tower", "Find an Ember Outpost in the Nether", "minecraft:gilded_blackstone"),
    "wilds_void_lantern": ("Void Lantern", "surface_structures",
                           ["minecraft:end_highlands", "minecraft:end_midlands", "minecraft:end_barrens"], 40, 14,
                           "Light at the Edge", "Find a Void Lantern on the End's outer islands", "minecraft:end_rod"),
}

# id: (breathing method, vault runes, hall runes, vault extras, hall extras)
# Runes come from the site's elements; extras are (item, weight, low, high).
LOOT = {
    "wilds_venom_ziggurat": ("venom", ["venom", "vinelash", "rootsnare", "sporebloom", "remedy", "veil"], ["bramble", "glowvine", "cleanse"],
                             [("minecraft:emerald", 3, 2, 5), ("minecraft:cocoa_beans", 2, 3, 8)],
                             [("minecraft:bamboo", 3, 4, 12), ("minecraft:spider_eye", 2, 1, 3), ("minecraft:arrow", 2, 4, 10)]),
    "wilds_dune_temple": ("dune", ["sandstorm", "sandbar", "mire", "shield", "glidewind", "fair_wind"], ["landread", "surefoot", "swift"],
                          [("minecraft:gold_ingot", 3, 2, 5), ("minecraft:emerald", 2, 1, 4)],
                          [("minecraft:bone", 3, 2, 6), ("minecraft:gold_nugget", 3, 3, 9), ("minecraft:rotten_flesh", 2, 2, 5)]),
    "wilds_rime_monastery": ("rime", ["rime_seal", "freeze", "coldsnap", "hoarfrost", "foresight", "moon_reading"], ["chill", "icepath", "frostward"],
                             [("minecraft:blue_ice", 2, 1, 3), ("minecraft:candle", 2, 1, 3)],
                             [("minecraft:sweet_berries", 3, 2, 6), ("minecraft:snowball", 3, 4, 12), ("minecraft:book", 2, 1, 2)]),
    "wilds_iron_gatehouse": ("iron", ["tremor", "shackle", "shield", "magnetize", "galvanize", "keenkeep"], ["chisel", "prospect", "storm_glass"],
                             [("minecraft:iron_ingot", 4, 3, 8), ("minecraft:shield", 1, 1, 1)],
                             [("minecraft:iron_nugget", 3, 4, 12), ("minecraft:arrow", 3, 4, 12), ("minecraft:red_terracotta", 1, 4, 8)]),
    "wilds_dawn_pavilion": ("dawn", ["halo", "lodestar", "lumenpath", "bloom", "regrowth", "sun_reading"], ["light", "waymark", "heal"],
                            [("minecraft:honey_bottle", 2, 1, 3), ("minecraft:golden_apple", 1, 1, 1)],
                            [("minecraft:cherry_sapling", 3, 1, 3), ("minecraft:pink_petals", 3, 2, 6), ("minecraft:bread", 2, 2, 4)]),
    "wilds_echo_post": ("echo", ["echolocate", "hush", "enderhush", "softfoot", "glidewind", "deepsound"], ["whistle", "softsole", "deepwarn"],
                        [("minecraft:echo_shard", 2, 1, 2), ("minecraft:amethyst_shard", 3, 2, 6)],
                        [("minecraft:amethyst_shard", 3, 1, 4), ("minecraft:white_wool", 2, 2, 6), ("minecraft:paper", 3, 2, 6)]),
    "wilds_ember_outpost": ("ember", ["blazecall", "fire", "fireward", "fortress_sense", "bleed", "warcry"], ["ember", "kindling", "lava_sense"],
                            [("minecraft:gold_ingot", 3, 2, 6), ("minecraft:blaze_rod", 2, 1, 3)],
                            [("minecraft:gold_nugget", 3, 4, 12), ("minecraft:nether_wart", 3, 2, 6), ("minecraft:magma_cream", 2, 1, 3)]),
    "wilds_void_lantern": ("hollow", ["blink", "pull", "void_step", "spire_sense", "hollow_pocket", "starlight_tether"], ["hex", "anchor", "warp_step"],
                           [("minecraft:ender_pearl", 3, 2, 4), ("minecraft:chorus_fruit", 2, 2, 6)],
                           [("minecraft:chorus_fruit", 3, 2, 5), ("minecraft:ender_pearl", 2, 1, 2), ("minecraft:popped_chorus_fruit", 2, 2, 6)]),
}

LANG = {}
for _id, _site in SITES.items():
    LANG[f"advancements.wildercord.world.{_id}.title"] = _site[5]
    LANG[f"advancements.wildercord.world.{_id}.description"] = _site[6]


def _manual(method):
    return {"type": "minecraft:item", "name": "wildercord:manual_page", "weight": 1,
            "functions": [{"function": "minecraft:set_components", "components": {"wildercord:breathing_method": method}}]}


def _vault(g, method, runes, extras):
    return {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [_manual(method)]},
        {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 3}, "entries": [g.rune_entry(r, 3) for r in runes]},
        {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "entries": [g.item_entry(*e) for e in extras] + [
            g.item_entry("wildercord:aura_shard", 2),
            g.item_entry("wildercord:blank_rune", 3, 2, 4),
            g.item_entry("wildercord:mana_crystal", 2, 1, 2),
            g.item_entry("wildercord:spell_scroll", 1)]},
    ]}


def _hall(g, runes, extras):
    return {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [g.rune_entry(r, 3) for r in runes]},
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 5}, "entries": [g.item_entry(*e) for e in extras] + [
            g.item_entry("wildercord:blank_rune", 3, 1, 2),
            g.item_entry("wildercord:torn_page", 2),
            g.item_entry("minecraft:torch", 2, 2, 6)]},
    ]}


def write(g):
    known = {r["path"]: r["element"] for r in g.read_runes()}
    for site_id, (name, step, biomes, spacing, separation, title, text, icon) in SITES.items():
        g.write_json(g.DATA / f"worldgen/structure/{site_id}.json", {
            "type": "wildercord:site", "site": site_id, "biomes": f"#wildercord:has_structure/{site_id}",
            "spawn_overrides": {}, "step": step, "terrain_adaptation": "none"})
        n = list(SITES).index(site_id) + 1
        placement = {"type": "minecraft:random_spread", "salt": SALT + n, "separation": separation, "spacing": spacing}
        if site_id == "wilds_echo_post":
            # Keep the listening post well clear of ancient cities and their shriekers.
            placement["exclusion_zone"] = {"other_set": "minecraft:ancient_cities", "chunk_count": 8}
        g.write_json(g.DATA / f"worldgen/structure_set/{site_id}.json", {
            "placement": placement, "structures": [{"structure": f"wildercord:{site_id}", "weight": 1}]})
        g.write_json(g.DATA / f"tags/worldgen/biome/has_structure/{site_id}.json", {"replace": False, "values": biomes})

        method, vault_runes, hall_runes, vault_extras, hall_extras = LOOT[site_id]
        for r in vault_runes + hall_runes:
            assert r in known, f"{site_id}: no rune {r}"
        g.write_json(g.DATA / f"loot_table/chests/{site_id}.json", _vault(g, method, vault_runes, vault_extras))
        g.write_json(g.DATA / f"loot_table/chests/{site_id}_hall.json", _hall(g, hall_runes, hall_extras))
        g.adv(f"world/{site_id}", "world/archive", g.item(icon), title, text, g.in_structure(f"wildercord:{site_id}"), xp=20)

    # The ziggurat's dart dispensers: poison arrows.
    g.write_json(g.DATA / "loot_table/chests/wilds_venom_ziggurat_darts.json", {"type": "minecraft:chest", "pools": [
        {"rolls": 2, "entries": [{"type": "minecraft:item", "name": "minecraft:tipped_arrow", "functions": [
            {"function": "minecraft:set_potion", "id": "minecraft:poison"},
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 3, "max": 6}}]}]}]})
    # What the dune temple's drifts hide, for a brush.
    g.write_json(g.DATA / "loot_table/chests/wilds_dune_temple_sand.json", {"type": "minecraft:archaeology", "pools": [
        {"rolls": 1, "entries": [
            g.rune_entry("sandbar", 1), g.rune_entry("landread", 1),
            g.item_entry("wildercord:torn_page", 2),
            g.item_entry("minecraft:gold_nugget", 3, 2, 5),
            g.item_entry("minecraft:arms_up_pottery_sherd", 1),
            g.item_entry("minecraft:bone", 3)]}]})
    return list(SITES)
