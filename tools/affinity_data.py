"""Creature affinities and elemental climate: the entity type tags and the English text.

Called from generate_assets.py. Each tag is data/wildercord/tags/entity_type/affinity/<name>.json, read by
cast/Affinities.java: weak_to_<element> (+50% from that element), resists_<element> (half) and
immune_to_<element> (nothing). A datapack can add to any of them, or replace one, without touching code.
Only the tags that have creatures in them are written; the rest simply don't exist (which is empty).
"""

ELEMENTS = ["fire", "frost", "storm", "wind", "earth", "life", "void", "arcane", "time", "blood"]

# The Nether's creatures: at home in fire, lost in the cold.
NETHER = ["minecraft:blaze", "minecraft:magma_cube", "minecraft:ghast", "minecraft:strider", "minecraft:wither_skeleton",
          "minecraft:hoglin", "minecraft:zoglin", "minecraft:piglin", "minecraft:piglin_brute", "minecraft:zombified_piglin"]
# Creatures of the End slip through space: void can't hold them, time can.
END = ["minecraft:enderman", "minecraft:endermite", "minecraft:shulker"]
# Built things with no blood in them: golems of iron and copper.
METAL = ["minecraft:iron_golem", "minecraft:copper_golem"]

AFFINITIES = {
    # The cold's own creatures thaw and melt; the Creaking is dry wood.
    "weak_to_fire": ["minecraft:stray", "minecraft:snow_golem", "minecraft:polar_bear", "minecraft:creaking",
                     # Moths to a flame; a hare of the frost.
                     "wildercord:glimmerwing", "wildercord:rimehare", "wildercord:bramblewalker", "wildercord:mana_ooze"],
    # The Nether's creatures, and anything that lives in water (being wet doesn't dull fire on them any further).
    "resists_fire": NETHER + ["#minecraft:aquatic", "minecraft:drowned", "wildercord:cinder_warden", "wildercord:cinderfox"],
    # The Nether's creatures, and slimes, whose jelly sets hard.
    "weak_to_frost": NETHER + ["minecraft:slime", "wildercord:cinder_warden",
                               # An ember-tailed fox, and a cold-blooded tortoise.
                               "wildercord:cinderfox", "wildercord:mossback_tortoise", "wildercord:thunderwing_harpy", "wildercord:bog_witch_frog"],
    "resists_frost": ["minecraft:stray", "minecraft:polar_bear", "wildercord:tide_scribe", "wildercord:rimehare"],
    # A snow golem is made of the stuff.
    "immune_to_frost": ["minecraft:snow_golem"],
    # Metal conducts. (Water creatures are wet already, so Conduct covers them; the Tide Scribe takes five times a shock
    # through its own flood, which is weakness enough.)
    "weak_to_storm": METAL + ["wildercord:skyray", "wildercord:geode_crawler"],  # and a skyray, high in the storm's own sky
    # Lightning only charges a creeper up.
    "resists_storm": ["minecraft:creeper", "wildercord:thunderwing_harpy"],
    # Small and light, or all wing: blown about.
    "weak_to_wind": ["#minecraft:arthropod", "minecraft:phantom", "wildercord:glimmerwing"],
    "resists_wind": ["minecraft:breeze", "wildercord:skyray", "wildercord:mossback_tortoise", "wildercord:thunderwing_harpy"],  # born to it; too heavy to shift
    "weak_to_earth": ["minecraft:breeze", "wildercord:thunderwing_harpy"],
    # Solid metal, and bouncing jelly.
    "resists_earth": METAL + ["minecraft:slime", "minecraft:magma_cube", "wildercord:mossback_tortoise", "wildercord:bramblewalker", "wildercord:geode_crawler"],  # and a shell like stone
    # Life's harm burns the undead (the Tide Scribe is a drowned sorcerer), and the Star-Eater, a knot of void.
    "weak_to_life": ["#minecraft:undead", "wildercord:tide_scribe", "wildercord:star_eater"],
    # Venomous things shrug off venom; a witch drinks her own remedies.
    "resists_life": ["#minecraft:arthropod", "minecraft:witch", "wildercord:bramblewalker", "wildercord:bog_witch_frog"],
    # What's written can be unwritten.
    "weak_to_void": ["wildercord:archivist", "wildercord:lumen_stag"],  # and a stag's light, snuffed
    "resists_void": END + ["minecraft:ender_dragon", "minecraft:wither", "minecraft:warden", "wildercord:star_eater", "wildercord:gloomstalker"],
    # A summoned spirit comes apart under raw magic.
    "weak_to_arcane": ["minecraft:vex", "wildercord:gloomstalker"],
    # Fellow spellcasters.
    "resists_arcane": ["minecraft:evoker", "minecraft:illusioner", "wildercord:archivist",
                       # Creatures half made of magic themselves.
                       "wildercord:lumen_stag", "wildercord:glimmerwing", "wildercord:geode_crawler"],
    "weak_to_time": END,
    # Blood magic can't draw on the bloodless: bones, metal, snow, living fire and wind, and spirits.
    "resists_blood": ["#minecraft:skeletons", "minecraft:wither"] + METAL + ["minecraft:snow_golem", "minecraft:blaze", "minecraft:breeze", "minecraft:vex"],
}

LANG = {
    "affinity.wildercord.weak": "Weak!",
    "affinity.wildercord.resisted": "Resisted",
    "affinity.wildercord.immune": "Immune",
    "toast.wildercord.bestiary": "%s: weak to %s",
    "screen.wildercord.grimoire.climate": "Where you stand",
    "screen.wildercord.grimoire.climate_none": "Nothing here favours or hinders an element",
    "screen.wildercord.grimoire.climate_line": "%s: %s",
    "screen.wildercord.grimoire.climate_hint": "Where you fight changes how hard each element hits. The marks beside your spell's name on the HUD show it.",
    "screen.wildercord.grimoire.bestiary": "Bestiary (%s met)",
    "screen.wildercord.grimoire.bestiary_none": "Strike a creature with a spell to begin",
    "screen.wildercord.grimoire.bestiary_line": "%s · weak: %s · resists: %s",
    "screen.wildercord.grimoire.bestiary_nothing": "none",
    "screen.wildercord.grimoire.bestiary_immune_to": "%s (immune)",
    "screen.wildercord.grimoire.bestiary_weak": "Weak to %s: takes 50%% more from it",
    "screen.wildercord.grimoire.bestiary_resists": "Resists %s: takes half from it",
    "screen.wildercord.grimoire.bestiary_immune": "Immune to %s: takes nothing from it",
    "screen.wildercord.grimoire.bestiary_unknown": "%s more to find: strike it with other elements",
    "screen.wildercord.grimoire.bestiary_complete": "Everything about it is known",
    "screen.wildercord.grimoire.bestiary_hint": "A resistance doesn't hold against a reaction set off on it",
    "climate.wildercord.nether": "The Nether",
    "climate.wildercord.end": "The End",
    "climate.wildercord.thunder": "A thunderstorm overhead",
    "climate.wildercord.rain": "Rain",
    "climate.wildercord.snow": "Snow and frost",
    "climate.wildercord.heat": "Hot, dry land",
    "climate.wildercord.night": "Night, under the sky",
    "climate.wildercord.sun": "Sunlight",
    "climate.wildercord.deep": "Deep underground",
    "climate.wildercord.ley": "A ley line or a mana storm",
}


def write_tags(write_json, data):
    """Writes each affinity tag that has creatures in it."""
    for name, values in AFFINITIES.items():
        kind, element = name.rsplit("_", 1)
        assert kind in ("weak_to", "resists", "immune_to") and element in ELEMENTS, name
        write_json(data / f"tags/entity_type/affinity/{name}.json", {"values": values})
