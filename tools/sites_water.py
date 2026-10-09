"""The water sites pack: eight places on rivers, swamps, coasts, the sea floor and frozen lakes.

Read by generate_assets.py, which finds every tools/sites_*.py. For each site this writes its structure, its
structure set, the biomes it may appear in, its chest's loot and its advancement; LANG holds their text and the
pages of the books left in three of them. The buildings themselves are Java: dev.wildercord.world.sites.water.
"""

# id: (biomes, spacing, separation, salt, advancement icon, title, description)
SITES = {
    "water_stilt_smokehouse": (["minecraft:swamp", "minecraft:river"], 36, 12, 2026101201,
                               "minecraft:smoker", "Smoke on the Water", "Find a smokehouse on stilts over a swamp or river"),
    "water_lighthouse": (["minecraft:beach", "minecraft:snowy_beach", "minecraft:stony_shore"], 44, 16, 2026101202,
                         "minecraft:redstone_lamp", "Keeper of the Light", "Find a lighthouse on the shore"),
    "water_watermill": (["minecraft:river"], 32, 10, 2026101203,
                        "minecraft:grindstone", "Run of the Mill", "Find a watermill on a river bank"),
    "water_ice_camp": (["minecraft:frozen_river", "minecraft:frozen_ocean"], 36, 12, 2026101204,
                       "minecraft:packed_ice", "Through the Ice", "Find an ice fishing camp on frozen water"),
    "water_pier_boathouse": (["minecraft:beach", "minecraft:snowy_beach", "minecraft:stony_shore"], 40, 14, 2026101205,
                             "minecraft:spruce_boat", "Safe Harbour", "Find a pier and boathouse on the shore"),
    "water_sunken_shrine": (["minecraft:warm_ocean", "minecraft:lukewarm_ocean", "minecraft:deep_lukewarm_ocean"], 48, 16, 2026101206,
                            "minecraft:conduit", "Shrine of the Tide", "Find the shrine on the floor of a warm sea"),
    "water_netweaver_village": (["minecraft:mangrove_swamp"], 40, 14, 2026101207,
                                "minecraft:loom", "Cast a Wide Net", "Find the net-weavers' huts in a mangrove swamp"),
    "water_tidepool_grotto": (["minecraft:stony_shore"], 36, 12, 2026101208,
                              "minecraft:sea_pickle", "Pools Left by the Tide", "Find a tidepool grotto on a stony shore"),
}

LANG = {
    "book.wildercord.water_lighthouse.1": "Keeper's log. Fog again. Lit the lamp at dusk and kept it till dawn. Two boats came home by it.",
    "book.wildercord.water_lighthouse.2": "The lamp is wired to a lever by it. If I am ever gone, any hand can throw it and light the way.",
    "book.wildercord.water_lighthouse.3": "What I kept I kept below. Look for the cracked stone by the wall, and the ladder under it.",
    "book.wildercord.water_sunken_shrine.1": "Here the Tide is honoured. Breathe as the sea breathes: in with the swell, out with the ebb.",
    "book.wildercord.water_sunken_shrine.2": "The way in is under the wall and up through the pool. The conduit gives breath to those who come.",
    "book.wildercord.water_tidepool_grotto.1": "Each tide fills the pools and leaves gifts. I count the pickles and the fish and write them here.",
    "book.wildercord.water_tidepool_grotto.2": "Something drowned keeps the deep pool. I keep what I found up on the ledge, where it cannot reach.",
}
for _sid, (_b, _s, _sep, _salt, _icon, _title, _desc) in SITES.items():
    LANG[f"advancements.wildercord.world.{_sid}.title"] = _title
    LANG[f"advancements.wildercord.world.{_sid}.description"] = _desc


def _tide_page():
    return {"type": "minecraft:item", "name": "wildercord:manual_page", "weight": 1,
            "functions": [{"function": "minecraft:set_components", "components": {"wildercord:breathing_method": "tide"}}]}


def _treasure_map(weight):
    return {"type": "minecraft:item", "name": "minecraft:map", "weight": weight, "functions": [
        {"function": "minecraft:exploration_map", "destination": "#minecraft:on_treasure_maps", "decoration": "minecraft:red_x",
         "zoom": 1, "skip_existing_chunks": False},
        {"function": "minecraft:set_name", "name": {"translate": "filled_map.buried_treasure"}, "target": "item_name"}]}


def _loot(g, runes, gear, extra=None, rune_rolls=1, gear_rolls=(2, 4)):
    """A chest: a rune from this site's own family, a few things for the trade, and any extras."""
    pools = [
        {"rolls": rune_rolls, "entries": [g.rune_entry(path, weight) for path, weight in runes]},
        {"rolls": {"type": "minecraft:uniform", "min": gear_rolls[0], "max": gear_rolls[1]}, "entries": gear},
    ]
    if extra:
        pools += extra
    return {"type": "minecraft:chest", "pools": pools}


def _chance(pool, odds):
    return dict(pool, conditions=[{"condition": "minecraft:random_chance", "chance": odds}])


def write(g):
    e = g.item_entry
    loot = {
        "water_stilt_smokehouse": _loot(g, [("angler_lure", 3), ("bait_blessing", 3), ("bobber_bell", 3), ("reeling_tide", 2), ("tackle_mend", 2)], [
            e("minecraft:cooked_cod", 4, 2, 6), e("minecraft:cooked_salmon", 3, 2, 5), e("minecraft:fishing_rod", 2),
            e("minecraft:string", 3, 2, 6), e("minecraft:dried_kelp", 3, 3, 9), e("minecraft:charcoal", 2, 1, 4)]),
        "water_lighthouse": _loot(g, [("tide_lantern", 3), ("storm_glass", 3), ("shore_sense", 3), ("tide_marker", 2), ("fathom", 2), ("wreck_sense", 2)], [
            e("minecraft:spyglass", 2), e("minecraft:compass", 2), e("minecraft:glowstone_dust", 3, 2, 6),
            e("minecraft:candle", 3, 1, 3), e("minecraft:paper", 3, 2, 5), e("minecraft:emerald", 2, 1, 3)],
            extra=[_chance({"rolls": 1, "entries": [_treasure_map(1)]}, 0.5)]),
        "water_watermill": _loot(g, [("spring_draw", 3), ("brimming", 3), ("sluice", 3), ("wring", 2), ("reed_cut", 2)], [
            e("minecraft:bread", 4, 2, 5), e("minecraft:wheat", 3, 3, 8), e("minecraft:bucket", 2),
            e("minecraft:sugar", 2, 1, 4), e("minecraft:fishing_rod", 1), e("minecraft:emerald", 2, 1, 2)]),
        "water_ice_camp": _loot(g, [("ice_auger", 3), ("water_reading", 3), ("school_sight", 3)], [
            e("minecraft:fishing_rod", 3), e("minecraft:cooked_cod", 3, 2, 5), e("minecraft:leather_boots", 1),
            e("minecraft:leather_helmet", 1), e("minecraft:campfire", 1), e("minecraft:snowball", 2, 2, 8),
            e("minecraft:pufferfish", 1)]),
        "water_pier_boathouse": _loot(g, [("mooring_call", 3), ("drift_net", 3), ("lily_path", 3), ("sandbar", 2), ("dolphin_call", 2)], [
            e("minecraft:lead", 2), e("minecraft:oak_boat", 1), e("minecraft:string", 3, 2, 6),
            e("minecraft:fishing_rod", 2), e("minecraft:cooked_salmon", 3, 2, 4), e("minecraft:nautilus_shell", 1)],
            extra=[_chance({"rolls": 1, "entries": [_treasure_map(1)]}, 0.35)]),
        "water_sunken_shrine": _loot(g, [("oceans_favor", 2), ("diving_bell", 3), ("tidebreath", 3), ("kelpsong", 3), ("coral_mend", 2)], [
            e("minecraft:prismarine_shard", 3, 2, 6), e("minecraft:prismarine_crystals", 3, 2, 5), e("minecraft:nautilus_shell", 2),
            e("minecraft:heart_of_the_sea", 1), e("minecraft:sea_lantern", 2, 1, 2), e("wildercord:aura_shard", 2)],
            extra=[{"rolls": 1, "entries": [_tide_page()]}], rune_rolls=2),
        "water_netweaver_village": _loot(g, [("drift_net", 3), ("reed_cut", 3), ("shoal_herd", 2), ("kelpsong", 2)], [
            e("minecraft:string", 4, 3, 8), e("minecraft:lead", 2), e("minecraft:fishing_rod", 2),
            e("minecraft:cooked_cod", 3, 2, 4), e("minecraft:mangrove_propagule", 2, 1, 3), e("minecraft:emerald", 2, 1, 3)]),
        "water_tidepool_grotto": _loot(g, [("refloat", 3), ("axolotl_kinship", 3), ("nest_tend", 3), ("coral_mend", 2), ("tidehook", 1)], [
            e("minecraft:sea_pickle", 3, 1, 4), e("minecraft:nautilus_shell", 2), e("minecraft:glow_ink_sac", 2, 1, 3),
            e("minecraft:tropical_fish", 2, 1, 2), e("wildercord:aura_shard", 1)],
            extra=[_chance({"rolls": 1, "entries": [_tide_page()]}, 0.5)]),
    }
    for sid, (biomes, spacing, separation, salt, icon, title, desc) in SITES.items():
        g.write_json(g.DATA / f"worldgen/structure/{sid}.json", {
            "type": "wildercord:site", "site": sid, "biomes": f"#wildercord:has_structure/{sid}",
            "spawn_overrides": {}, "step": "surface_structures", "terrain_adaptation": "none"})
        g.write_json(g.DATA / f"worldgen/structure_set/{sid}.json", {
            "placement": {"type": "minecraft:random_spread", "salt": salt, "separation": separation, "spacing": spacing},
            "structures": [{"structure": f"wildercord:{sid}", "weight": 1}]})
        g.write_json(g.DATA / f"tags/worldgen/biome/has_structure/{sid}.json", {"values": biomes})
        g.write_json(g.DATA / f"loot_table/chests/{sid}.json", loot[sid])
        g.adv(f"world/{sid}", "world/ley_line", g.item(icon), title, desc, g.in_structure(f"wildercord:{sid}"), xp=20)
    return list(SITES)
