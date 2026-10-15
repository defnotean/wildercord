"""Worldgen, loot, advancements, books, and resident dialogue for the Belowkeeper Hollows site pack."""

SITES = [
    ("belowkeeper_listening_station", 32, 11, 2026101603, ["wildercord:belowkeeper_hollows", "minecraft:dripstone_caves", "minecraft:lush_caves"],
     "wildercord:glowcap", "Where the Hollows Listen",
     "Find a Belowkeeper Listening Station and its Glowcap basin in the Belowkeeper Hollows",
     [("minecraft:bread", 3, 6, 12), ("minecraft:glow_berries", 2, 5, 10), ("minecraft:coal", 3, 6, 10),
      ("minecraft:copper_ingot", 3, 6, 10), ("minecraft:clay_ball", 3, 6, 8), ("wildercord:glowcap", 2, 4, 12),
      ("wildercord:dried_glowcap_gills", 1, 2, 8)],
     [("hollowsense", 3), ("gloomsight", 3), ("lumenpath", 3), ("headlamp", 2), ("caveward", 2)]),
    ("belowkeeper_bell_sanctum", 36, 12, 2026101604, ["wildercord:belowkeeper_hollows"],
     "wildercord:listening_belfry", "The Quiet Eye",
     "Find a Belowkeeper Bell Sanctum in the Belowkeeper Hollows",
     [("minecraft:copper_ingot", 3, 6, 10), ("minecraft:amethyst_shard", 2, 5, 10), ("minecraft:compass", 1, 1, 8),
      ("wildercord:glowcap", 2, 4, 10), ("wildercord:mycelial_dew", 1, 2, 8)],
     [("deepsound", 3), ("plumbline", 3), ("oretally", 3), ("shoreup", 2), ("torchfall", 2)]),
]

LANG = {
    "advancements.wildercord.world.belowkeeper_listening_station.title": "Where the Hollows Listen",
    "advancements.wildercord.world.belowkeeper_listening_station.description": "Find a Belowkeeper Listening Station and its Glowcap basin in the Belowkeeper Hollows",
    "advancements.wildercord.world.belowkeeper_bell_sanctum.title": "The Quiet Eye",
    "advancements.wildercord.world.belowkeeper_bell_sanctum.description": "Find a Belowkeeper Bell Sanctum in the Belowkeeper Hollows",
    "journal.wildercord.entry.place.belowkeeper_listening_station": "Belowkeeper Listening Station: a vaulted deepslate chamber with a damp Glowcap basin and acoustic shutter lamp.",
    "journal.wildercord.entry.place.belowkeeper_bell_sanctum": "Belowkeeper Bell Sanctum: a copper-and-deepslate resonance court where The Hollow Bell waits.",
    "entity.wildercord.belowkeeper_listener": "Belowkeeper Listener",
    "dialogue.wildercord.speaker.belowkeeper_listener": "Belowkeeper Listener",
    "journal.wildercord.entry.person.belowkeeper_listener": "Belowkeeper Listener: tends the deep listening stations, Glowcap basins, and acoustic shutter logs in the Belowkeeper Hollows.",
    "journal.wildercord.entry.learned.belowkeeper_shutter": "Set a Belowkeeper Listening Station shutter lamp so its pressure reading stays visible down the tunnel.",
    "dialogue.wildercord.belowkeeper_listener.greet": "Step softly on the grates. Down here, the stone hears your boots before I do.",
    "dialogue.wildercord.belowkeeper_listener.glowcap": "We keep a damp moss-and-clay basin along the west wall. Harvest only mature Glowcaps so the next watch has filter gills.",
    "dialogue.wildercord.belowkeeper_listener.pressure": "Mara taught us to listen for three things in the Hollows: dripping water, settling roofs, and the long hum of the deeper chambers.",
    "dialogue.wildercord.belowkeeper_listener.hollow_bell": "Beyond the older delves stands a copper-lined Bell Sanctum. Its Hollow Bell still tolls if you ring the Listening Belfry.",
    "dialogue.wildercord.belowkeeper_listener.deep_records": "Check the buried Crystal Survey Labs, Deepslate Vaults, or the Bell Sanctum itself. Their ledgers record how the deep pressure rings.",
    "dialogue.wildercord.belowkeeper_listener.station_gauge": "Our northeast pillar carries an acoustic shutter lamp. Throw its lever and stand beside the lit lamp so the tunnel watch knows the roof holds.",
    "dialogue.wildercord.belowkeeper_listener.gauge_return": "The shutter lamp is lit. Anyone coming up from the lower galleries will know the air is clear.",
    "dialogue.wildercord.belowkeeper_listener.bell_return": "You carry the Quiet Bell's compass. You stood in the eye beneath the bronze mantle while the sanctum rang.",
    "book.wildercord.belowkeeper_station.1": "We built the listening stations where dripstone meets damp clay. Water carries the hum of the lower rock long before a seam gives way.",
    "book.wildercord.belowkeeper_station.2": "Keep the west basin lined with moss and clay under a solid arch. Mature Glowcaps give both light and dried gills for a Cave Breather.",
    "book.wildercord.belowkeeper_station.3": "The surveyors in the Crystal Labs and Deepslate Vaults measured the same deep resonance that gathers inside our Bell Sanctum.",
    "book.wildercord.belowkeeper_station.4": "Before you leave a station, flip the lever on the northeast shutter lamp. A steady light tells the next watch that the gallery is quiet.",
    "book.wildercord.bell_sanctum.1": "This sanctum hangs The Hollow Bell above a copper-and-deepslate resonance ring. It was cast to test how pressure waves travel through stone.",
    "book.wildercord.bell_sanctum.2": "Ring the Listening Belfry only when ready. Stand inside the quiet eye beneath the bell during its toll, step clear of its clapper drop, and hold the damped lane during its chord.",
}


def item_entry(item, low, high, weight):
    entry = {"type": "minecraft:item", "name": item, "weight": weight}
    if low != 1 or high != 1:
        entry["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}}]
    return entry


def write(gen):
    known = {r["path"] for r in gen.read_runes()}
    banned = gen.found_only() | set(gen.INNATE) | set(gen.FUSED)
    ids = []
    for site_id, spacing, separation, salt, biomes, icon, title, desc, supplies, runes in SITES:
        ids.append(site_id)
        for path, _weight in runes:
            assert path in known, f"{site_id}: unknown rune {path}"
            assert path not in banned, f"{site_id}: rune {path} is found-only, innate or fused"
        gen.write_json(gen.DATA / f"tags/worldgen/biome/has_structure/{site_id}.json", {"replace": False, "values": biomes})
        gen.write_json(gen.DATA / f"worldgen/structure/{site_id}.json", {
            "type": "wildercord:site",
            "biomes": f"#wildercord:has_structure/{site_id}",
            "step": "underground_structures",
            "spawn_overrides": {},
            "terrain_adaptation": "none",
            "site": site_id,
        })
        gen.write_json(gen.DATA / f"worldgen/structure_set/{site_id}.json", {
            "structures": [{"structure": f"wildercord:{site_id}", "weight": 1}],
            "placement": {
                "type": "minecraft:random_spread",
                "spacing": spacing,
                "separation": separation,
                "salt": salt,
            },
        })
        gen.write_json(gen.DATA / f"loot_table/chests/{site_id}.json", {
            "type": "minecraft:chest",
            "pools": [
                {
                    "rolls": {"type": "minecraft:uniform", "min": 1, "max": 2},
                    "entries": [gen.rune_entry(path, weight) for path, weight in runes],
                },
                {
                    "rolls": {"type": "minecraft:uniform", "min": 3, "max": 5},
                    "entries": [item_entry(item, lo, hi, w) for item, lo, hi, w in supplies],
                },
            ],
        })
        gen.adv(f"world/{site_id}", "world/ley_line", gen.item(icon), title, desc, gen.in_structure(f"wildercord:{site_id}"), xp=20)
    return ids
