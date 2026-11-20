"""Version-independent chest loot tables for aura sites and sanctuaries."""


def _uniform(lo, hi):
    return {"type": "minecraft:uniform", "min": lo, "max": hi}


def _item(name, weight=None, count=None, enchant=False):
    entry = {"type": "minecraft:item", "name": name}
    if weight is not None:
        entry["weight"] = weight
    functions = []
    if count is not None:
        functions.append({"count": _uniform(*count), "function": "minecraft:set_count"})
    if enchant:
        functions.append({"function": "minecraft:enchant_randomly"})
    if functions:
        entry["functions"] = functions
    return entry


def _pool(rolls, entries):
    return {
        "rolls": _uniform(*rolls) if isinstance(rolls, tuple) else rolls,
        "entries": [_item(*entry) for entry in entries],
    }


CHEST_POOLS = {
    "ashen_glade": [
        ((3, 6), [
            ("minecraft:arrow", 4, (6, 16)),
            ("minecraft:charcoal", 4, (3, 10)),
            ("minecraft:leather", 3, (2, 6)),
            ("minecraft:cooked_mutton", 3, (2, 5)),
            ("minecraft:iron_ingot", 3, (1, 4)),
            ("minecraft:gold_nugget", 3, (4, 10)),
            ("minecraft:emerald", 2, (1, 3)),
            ("minecraft:blaze_powder", 1, (1, 2)),
            ("minecraft:golden_apple", 1, (1, 1)),
            ("minecraft:bow", 1),
        ]),
        ((0, 1), [("wildercord:aura_shard",)]),
    ],
    "barrow_field": [
        ((3, 6), [
            ("minecraft:candle", 4, (2, 6)),
            ("minecraft:bone", 4, (3, 9)),
            ("minecraft:iron_nugget", 4, (4, 12)),
            ("minecraft:bread", 3, (2, 4)),
            ("minecraft:gold_nugget", 3, (3, 9)),
            ("minecraft:soul_torch", 3, (2, 6)),
            ("minecraft:chainmail_helmet", 1),
            ("minecraft:chainmail_chestplate", 1),
            ("minecraft:iron_sword", 1),
            ("minecraft:emerald", 2, (1, 3)),
            ("minecraft:golden_apple", 1),
        ]),
        ((0, 1), [("wildercord:aura_shard",)]),
    ],
    "bell_chapel": [
        ((3, 6), [
            ("minecraft:prismarine_shard", 4, (3, 9)),
            ("minecraft:prismarine_crystals", 3, (2, 6)),
            ("minecraft:candle", 3, (1, 4)),
            ("minecraft:gold_nugget", 3, (4, 10)),
            ("minecraft:iron_ingot", 3, (1, 4)),
            ("minecraft:emerald", 2, (1, 3)),
            ("minecraft:nautilus_shell", 2, (1, 2)),
            ("minecraft:golden_apple", 1, (1, 1)),
            ("minecraft:heart_of_the_sea", 1),
        ]),
        ((0, 1), [("wildercord:aura_shard",)]),
    ],
    "glacier_den": [
        ((3, 6), [
            ("minecraft:packed_ice", 4, (2, 8)),
            ("minecraft:blue_ice", 2, (1, 4)),
            ("minecraft:snowball", 4, (4, 16)),
            ("minecraft:arrow", 4, (4, 12)),
            ("minecraft:cooked_salmon", 3, (1, 4)),
            ("minecraft:iron_ingot", 3, (1, 5)),
            ("minecraft:raw_gold", 3, (1, 4)),
            ("minecraft:gold_ingot", 2, (1, 3)),
            ("minecraft:diamond", 1, (1, 2)),
            ("minecraft:leather_boots", 2),
            ("minecraft:powder_snow_bucket", 1),
            ("minecraft:spyglass", 1),
            ("minecraft:compass", 1),
            ("wildercord:aura_shard", 1, (1, 2)),
        ]),
    ],
    "mojito_grove": [
        ((3, 6), [
            ("minecraft:bone", 5, (3, 9)),
            ("minecraft:cooked_beef", 4, (2, 6)),
            ("minecraft:lead", 3, (1, 2)),
            ("minecraft:name_tag", 2),
            ("minecraft:gold_nugget", 3, (4, 12)),
            ("minecraft:emerald", 2, (1, 4)),
        ]),
        (1, [
            ("minecraft:wolf_armor", 1),
            ("minecraft:golden_apple", 2),
            ("minecraft:iron_sword", 3, None, True),
        ]),
    ],
    "silk_hollow": [
        ((3, 6), [
            ("minecraft:string", 5, (4, 12)),
            ("minecraft:torch", 4, (4, 12)),
            ("minecraft:rail", 3, (4, 10)),
            ("minecraft:coal", 4, (3, 10)),
            ("minecraft:bread", 3, (1, 4)),
            ("minecraft:iron_ingot", 3, (1, 5)),
            ("minecraft:raw_gold", 2, (1, 3)),
            ("minecraft:spider_eye", 2, (1, 3)),
            ("minecraft:lead", 1),
            ("minecraft:name_tag", 1),
            ("minecraft:iron_pickaxe", 1),
            ("wildercord:aura_shard", 1, (1, 2)),
        ]),
    ],
    "wayside_shrine": [
        ((3, 5), [
            ("minecraft:bread", 4, (1, 3)),
            ("minecraft:candle", 4, (1, 4)),
            ("minecraft:apple", 3, (1, 3)),
            ("minecraft:honey_bottle", 2, (1, 2)),
            ("minecraft:gold_nugget", 4, (2, 8)),
            ("minecraft:emerald", 2, (1, 3)),
            ("minecraft:poppy", 2, (1, 3)),
            ("minecraft:dandelion", 2, (1, 3)),
            ("minecraft:paper", 2, (1, 4)),
            ("minecraft:glow_berries", 1, (1, 4)),
        ]),
        ((0, 1), [
            ("minecraft:golden_apple", 2),
            ("minecraft:name_tag", 2),
            ("minecraft:book", 3, None, True),
        ]),
    ],
}


def write(g):
    """Write all 7 aura site chest loot tables via generate_assets module g."""
    for site_id, pools in CHEST_POOLS.items():
        g.write_json(g.DATA / f"loot_table/chests/{site_id}.json", {
            "type": "minecraft:chest",
            "pools": [_pool(rolls, entries) for rolls, entries in pools],
        })
