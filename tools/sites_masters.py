"""
The Master halls and shrines (sites-masters pack): eight training places of the breathing schools, each keeping one of
two related schools. Writes every site's worldgen, loot, advancement and text; generate_assets.py finds this file itself.
The buildings are dev.wildercord.world.sites.masters.
"""

# id: (schools, title, short description, icon, biomes, spacing, separation, step, terrain adaptation)
SITES = {
    'master_forge_dojo': (('ember', 'crimson'), 'Forge Dojo', 'Find a Master dojo in the badlands', 'minecraft:campfire',
        ['minecraft:badlands', 'minecraft:wooded_badlands', 'minecraft:eroded_badlands'], 44, 16, 'surface_structures', 'beard_thin'),
    'master_wind_gate': (('gale', 'thunder'), 'Wind Gate', 'Find the Master gate on the windswept heights', 'minecraft:lightning_rod',
        ['minecraft:windswept_hills', 'minecraft:windswept_forest', 'minecraft:windswept_savanna', 'minecraft:savanna_plateau'], 46, 16, 'surface_structures', 'beard_thin'),
    'master_quarry_hall': (('stone', 'iron'), 'Quarry Hall', 'Find a Master hall among the stony hills', 'minecraft:stonecutter',
        ['minecraft:windswept_gravelly_hills', 'minecraft:stony_peaks', 'minecraft:meadow'], 46, 16, 'surface_structures', 'beard_thin'),
    'master_waterfall_shrine': (('rime', 'tide'), 'Waterfall Shrine', 'Find the frozen fall of a Master shrine', 'minecraft:blue_ice',
        ['minecraft:snowy_plains', 'minecraft:snowy_taiga', 'minecraft:grove', 'minecraft:snowy_slopes', 'minecraft:ice_spikes'], 48, 16, 'surface_structures', 'beard_thin'),
    'master_root_temple': (('verdant', 'venom'), 'Root Temple', 'Find a Master temple wrapped in roots', 'minecraft:mangrove_roots',
        ['minecraft:jungle', 'minecraft:sparse_jungle', 'minecraft:bamboo_jungle', 'minecraft:dark_forest'], 44, 14, 'surface_structures', 'beard_thin'),
    'master_sundial_court': (('dune', 'hourglass'), 'Sundial Court', 'Find a Master court in the desert', 'minecraft:chiseled_sandstone',
        ['minecraft:desert'], 48, 16, 'surface_structures', 'beard_thin'),
    'master_star_terrace': (('starlit', 'dawn'), 'Star Terrace', 'Find the Master terrace that reads the stars', 'minecraft:spyglass',
        ['minecraft:cherry_grove', 'minecraft:sunflower_plains', 'minecraft:plains'], 52, 18, 'surface_structures', 'beard_thin'),
    'master_resonance_chamber': (('hollow', 'echo'), 'Resonance Chamber', 'Find the deep Master chamber under the bell', 'minecraft:bell',
        ['minecraft:deep_dark', 'minecraft:dripstone_caves', 'minecraft:lush_caves'], 50, 18, 'underground_structures', 'none'),
}
SALT_BASE = 2026100900 + 100 * 1

# Three runes each school's reward chest may hold, in its own element.
RUNES = {
    'ember': ['ember', 'cinderbrand', 'flashfire'], 'crimson': ['bleed', 'leech', 'gash'],
    'gale': ['windcut', 'razorgale', 'dash'], 'thunder': ['shock', 'jolt', 'thunderclap'],
    'stone': ['tremor', 'aftershock', 'citadel'], 'iron': ['ironhold', 'brace', 'bonespur'],
    'rime': ['chill', 'frostbite', 'icicle'], 'tide': ['current', 'bubble', 'frost'],
    'verdant': ['rootsnare', 'bloom', 'heal'], 'venom': ['venom', 'bramble', 'sporebloom'],
    'dune': ['dust_devil', 'geode', 'brace'], 'hourglass': ['haste', 'stasis', 'rewind'],
    'starlit': ['light', 'lodestar', 'cometfall'], 'dawn': ['halo', 'grace', 'beacon_swell'],
    'hollow': ['hollow', 'blink', 'pull'], 'echo': ['echolocate', 'resonant_shriek', 'push'],
}

TRIALS = {
    'master_forge_dojo': 'The vault behind the hall is sealed. Strike the seal with the school\'s own element to open it.',
    'master_wind_gate': 'The prize waits on the gate\'s beam. Climb the posts, one after another, without falling.',
    'master_quarry_hall': 'A Runebound guard keeps the pit. Beat it in the ring and the chest beneath is yours.',
    'master_waterfall_shrine': 'The prize rests high in the cliff. Hop the ledges over the pool to reach the alcove.',
    'master_root_temple': 'A Runebound guard keeps the root pit. Beat it in the ring and the chest beneath is yours.',
    'master_sundial_court': 'The shrine behind the gnomon is sealed. Strike the seal with the school\'s own element to open it.',
    'master_star_terrace': 'The reading room is sealed. Strike the seal with the school\'s own element to open it.',
    'master_resonance_chamber': 'The vault at the back is sealed. Strike the seal with the school\'s own element to open it.',
}

SCHOOL_LORE = {
    'ember': 'The Ember school trains by the forge. Its students learn to breathe like bellows and strike while the steel is hot.',
    'crimson': 'The Crimson school trains in the heat and the dark. It teaches that every wound you give can feed your next cut.',
    'gale': 'The Gale school trains on the high wind. Its students learn to move first and land light.',
    'thunder': 'The Thunder school trains under the storm. It teaches one fast, bright strike, and then another.',
    'stone': 'The Stone school trains in the quarry. Its students learn to stand still, take the blow and answer it.',
    'iron': 'The Iron school trains with hammer and anvil. It teaches that a blade is only as good as its temper.',
    'rime': 'The Rime school trains in the cold. Its students learn a slow breath and a cut that numbs.',
    'tide': 'The Tide school trains by the water. It teaches to pull back, then come in like a wave.',
    'verdant': 'The Verdant school trains among the roots. Its students learn to mend as they fight.',
    'venom': 'The Venom school trains in the deep green. It teaches a small cut that keeps on working.',
    'dune': 'The Dune school trains in the sand. Its students learn to grind an enemy down, grain by grain.',
    'hourglass': 'The Hourglass school trains by the sundial. It teaches that the right moment is worth more than strength.',
    'starlit': 'The Starlit school trains under the night sky. Its students learn to read the stars and strike by their light.',
    'dawn': 'The Dawn school trains at first light. It teaches a bright, clean strike that blinds as it cuts.',
    'hollow': 'The Hollow school trains in the deep. Its students learn to pull the fight toward them.',
    'echo': 'The Echo school trains where sound carries. It teaches a strike that rings, and rings again.',
}


def _text():
    lang = {'book.wildercord.master_site.challenge':
            'To face the %1$s Master, first reach Aura Form or Heart Circle VIII. '
            'Then sneak and use a wandering %1$s duelist, and do it again within 10 seconds. '
            'Or type %2$s.'}
    for school, text in SCHOOL_LORE.items():
        lang[f'book.wildercord.master_site.{school}'] = text
    for site, (schools, title, desc, *_rest) in SITES.items():
        lang[f'book.wildercord.{site}.trial'] = TRIALS[site]
        lang[f'advancements.wildercord.sites.{site}.title'] = title
        lang[f'advancements.wildercord.sites.{site}.description'] = desc
    return lang


LANG = _text()


def _components(item, comp, value, count=None):
    fns = [{'function': 'minecraft:set_components', 'components': {comp: value}}]
    if count:
        fns.append({'function': 'minecraft:set_count', 'count': count})
    return {'type': 'minecraft:item', 'name': item, 'functions': fns}


def _item(name, weight, lo=1, hi=1):
    e = {'type': 'minecraft:item', 'name': name, 'weight': weight}
    if hi > 1:
        e['functions'] = [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]
    return e


def _chance(entry, percent):
    return {'rolls': 1, 'conditions': [{'condition': 'minecraft:random_chance', 'chance': percent / 100}], 'entries': [entry]}


def reward(school):
    """A school's reward: its pages, maybe its manual, maybe a technique scroll, its runes, and a few basics."""
    method = 'wildercord:breathing_method'
    runes = [dict(_components('wildercord:rune', 'wildercord:rune', f'wildercord:{r}'), weight=1) for r in RUNES[school]]
    scroll = {'type': 'minecraft:item', 'name': 'wildercord:technique_scroll', 'functions': [{'function': 'wildercord:random_technique_part', 'source': ''}]}
    return {'type': 'minecraft:chest', 'pools': [
        {'rolls': 1, 'entries': [_components('wildercord:manual_page', method, school, {'type': 'minecraft:uniform', 'min': 1, 'max': 2})]},
        _chance(_components('wildercord:breathing_manual', method, school), 33),
        _chance(scroll, 30),
        {'rolls': {'type': 'minecraft:uniform', 'min': 1, 'max': 2}, 'entries': runes},
        {'rolls': {'type': 'minecraft:uniform', 'min': 2, 'max': 4}, 'entries': [
            _item('minecraft:experience_bottle', 3, 1, 3), _item('minecraft:iron_ingot', 3, 1, 4), _item('minecraft:gold_ingot', 2, 1, 3),
            _item('minecraft:bread', 3, 2, 5), _item('minecraft:emerald', 2, 1, 3), _item('minecraft:diamond', 1)]},
    ]}


def supplies():
    """A hall's shared supplies: food, light and the odd practice arrow."""
    return {'type': 'minecraft:chest', 'pools': [
        {'rolls': {'type': 'minecraft:uniform', 'min': 3, 'max': 6}, 'entries': [
            _item('minecraft:bread', 4, 1, 4), _item('minecraft:cooked_mutton', 3, 1, 3), _item('minecraft:apple', 3, 1, 3),
            _item('minecraft:torch', 3, 4, 10), _item('minecraft:arrow', 3, 4, 12), _item('minecraft:string', 2, 1, 4),
            _item('minecraft:paper', 2, 1, 4), _item('minecraft:iron_nugget', 2, 3, 9), _item('minecraft:book', 1)]},
    ]}


def write(g):
    data = g.DATA
    for n, (site, (schools, title, desc, icon, biomes, spacing, separation, step, adapt)) in enumerate(SITES.items(), start=1):
        g.write_json(data / f'worldgen/structure/{site}.json', {'type': 'wildercord:site', 'site': site, 'biomes': f'#wildercord:has_structure/{site}',
            'spawn_overrides': {}, 'step': step, 'terrain_adaptation': adapt})
        g.write_json(data / f'worldgen/structure_set/{site}.json', {'placement': {'type': 'minecraft:random_spread', 'salt': SALT_BASE + n,
            'spacing': spacing, 'separation': separation}, 'structures': [{'structure': f'wildercord:{site}', 'weight': 1}]})
        g.write_json(data / f'tags/worldgen/biome/has_structure/{site}.json', {'replace': False, 'values': biomes})
        g.write_json(data / f'loot_table/chests/{site}.json', supplies())
        for school in schools:
            g.write_json(data / f'loot_table/chests/{site}_{school}.json', reward(school))
        # Through adv(): write_advancements (which runs after this) clears and rewrites the whole advancement folder.
        # Its lang keys are in LANG, since advancement_lang() has already run by now.
        g.adv(f'sites/{site}', 'root', g.item(icon), title, desc, g.in_structure(f'wildercord:{site}'), xp=30)
    return list(SITES)
