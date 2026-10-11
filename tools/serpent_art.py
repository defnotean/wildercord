"""The Rootcoil Serpent's grove: a jungle sinkhole, a cavern with a water ring, and the tree the serpent coils round."""

LANG = {}

GROVE_BIOMES = ['minecraft:jungle', 'minecraft:sparse_jungle', 'minecraft:bamboo_jungle']


def write(g):
    g.write_json(g.DATA / 'worldgen/structure/serpent_grove.json', {
        'type': 'wildercord:dungeon', 'dungeon': 'serpent_grove', 'biomes': '#wildercord:has_structure/serpent_grove',
        'spawn_overrides': {}, 'step': 'surface_structures', 'terrain_adaptation': 'none'})
    g.write_json(g.DATA / 'worldgen/structure_set/serpent_groves.json', {
        'placement': {'type': 'minecraft:random_spread', 'salt': 202610111, 'spacing': 44, 'separation': 16},
        'structures': [{'structure': 'wildercord:serpent_grove', 'weight': 1}]})
    g.write_json(g.DATA / 'tags/worldgen/biome/has_structure/serpent_grove.json', {'replace': False, 'values': GROVE_BIOMES})
    g.write_json(g.DATA / 'tags/worldgen/structure/serpent_grove.json', {'replace': False, 'values': ['wildercord:serpent_grove']})
    # What the last party to come down left in the tunnel: their supplies, not the serpent's.
    g.write_json(g.DATA / 'loot_table/chests/serpent_grove.json', {'type': 'minecraft:chest', 'pools': [
        {'rolls': {'type': 'minecraft:uniform', 'min': 3, 'max': 5}, 'entries': [
            {'type': 'minecraft:item', 'name': item, 'weight': w,
             'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]}
            for item, w, lo, hi in [('minecraft:arrow', 4, 4, 12), ('minecraft:bone', 4, 2, 6), ('minecraft:cocoa_beans', 3, 2, 5),
                                    ('minecraft:melon_slice', 3, 2, 6), ('minecraft:gold_nugget', 3, 3, 9), ('minecraft:emerald', 2, 1, 3),
                                    ('minecraft:string', 3, 2, 5), ('minecraft:golden_apple', 1, 1, 1)]]},
        {'rolls': {'type': 'minecraft:uniform', 'min': 0, 'max': 1}, 'entries': [{'type': 'minecraft:item', 'name': 'wildercord:aura_shard'}]}]})
