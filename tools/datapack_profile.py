"""Versioned datapack schema adapter keyed off gradle.properties' minecraft_version.

Generators call DataPackProfile methods instead of duplicating schema branches across Python files.
"""
from dataclasses import dataclass
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parent.parent

SPAWNER_CATEGORIES = (
    "ambient",
    "axolotls",
    "creature",
    "misc",
    "monster",
    "underground_water_creature",
    "water_ambient",
    "water_creature",
)


def read_mc_version(root=None):
    """Read minecraft_version from gradle.properties."""
    props_path = (ROOT if root is None else Path(root)) / "gradle.properties"
    text = props_path.read_text(encoding="utf-8")
    for raw in text.splitlines():
        line = raw.strip()
        if line.startswith("minecraft_version="):
            return line.split("=", 1)[1].strip()
    raise ValueError(f"minecraft_version not found in {props_path}")


def parse_mc_tuple(mc_version: str) -> tuple[int, ...]:
    """Parse a supported release Minecraft version string into a numeric tuple."""
    clean = mc_version.strip()
    if not clean or any(not part.isdigit() for part in clean.split(".")):
        raise ValueError(f"Unsupported minecraft_version '{mc_version}'; expected numeric dot-separated version")
    parts = tuple(int(part) for part in clean.split("."))
    if len(parts) < 2:
        raise ValueError(f"Unsupported minecraft_version '{mc_version}'; expected major.minor")
    major, minor = parts[0], parts[1]
    if major >= 26 and minor >= 1:
        return parts
    if major == 1 and minor in (20, 21):
        return parts
    raise ValueError(
        f"Unsupported minecraft_version '{mc_version}'; expected 26.1+ or 1.20.x/1.21.x"
    )


def _flatten_conditions(value):
    if isinstance(value, list):
        out = []
        for item in value:
            out.extend(_flatten_conditions(item))
        return out
    if isinstance(value, dict) and value.get("type") == "minecraft:all_of":
        return _flatten_conditions(value.get("terms", []))
    return [value]


@dataclass(frozen=True)
class DataPackProfile:
    """Datapack schema adapter for a target Minecraft release (e.g. '1.21.11', '26.1', '26.2', '26.3')."""

    version: str

    @classmethod
    def for_version(cls, mc_version):
        """Return the profile matching an exact or patch-prefixed Minecraft version string."""
        clean = mc_version.strip()
        parts = parse_mc_tuple(clean)
        major, minor = parts[0], parts[1]
        if major >= 26:
            return cls(f"{major}.{minor}")
        return cls(clean)

    @classmethod
    def from_gradle(cls, root=None):
        """Instantiate the profile for the workspace's gradle.properties minecraft_version."""
        return cls.for_version(read_mc_version(root))

    @property
    def version_tuple(self) -> tuple[int, ...]:
        return parse_mc_tuple(self.version)

    @property
    def before_26_3(self) -> bool:
        return self.version_tuple[:2] < (26, 3)

    @property
    def before_26_2(self) -> bool:
        return self.version_tuple[:2] < (26, 2)

    @property
    def is_obfuscated(self) -> bool:
        return self.version_tuple[:2] < (26, 1)

    @property
    def is_26_2(self):
        return self.before_26_3

    # ---------------------------------------------------------------- loot tables

    def loot_table(self, data):
        """Normalize loot table JSON (pools, entries, functions/modifier, conditions/condition)."""
        if isinstance(data, list):
            return [self.loot_table(v) for v in data]
        if not isinstance(data, dict):
            return data
        if self.is_26_2:
            return self._loot_to_262(data)
        return self._loot_to_263(data)

    def _loot_to_262(self, obj):
        out = {}
        for key, value in obj.items():
            if key == "modifier":
                mods = value if isinstance(value, list) else [value]
                if mods:
                    out["functions"] = [self._loot_entry_to_262(m, "function") for m in mods]
            elif key == "condition" and isinstance(value, (dict, list)):
                conds = _flatten_conditions(value)
                if conds:
                    out["conditions"] = [self._loot_entry_to_262(c, "condition") for c in conds]
            else:
                out[key] = self.loot_table(value)
        return out

    def _loot_entry_to_262(self, entry, name_key):
        if not isinstance(entry, dict):
            return entry
        entry = dict(entry)
        if "type" in entry and name_key not in entry:
            # Hoist the discriminator to the front: that is the order the asset
            # generators author these entries in, so a converted file and a
            # generated one have to come out byte-identical.
            entry = {name_key: entry.pop("type"), **entry}
        return self.loot_table(entry)

    def _loot_to_263(self, obj):
        out = {}
        for key, value in obj.items():
            if key == "functions":
                mods = [self._loot_entry_to_263(f, "function") for f in value]
                if mods:
                    out["modifier"] = mods[0] if len(mods) == 1 else mods
            elif key == "conditions":
                conds = [self._loot_entry_to_263(c, "condition") for c in value]
                if conds:
                    out["condition"] = conds[0] if len(conds) == 1 else {"type": "minecraft:all_of", "terms": conds}
            elif key == "condition" and isinstance(value, list):
                conds = [self._loot_entry_to_263(c, "condition") for c in value]
                if conds:
                    out["condition"] = conds[0] if len(conds) == 1 else {"type": "minecraft:all_of", "terms": conds}
            else:
                out[key] = self.loot_table(value)
        return out

    def _loot_entry_to_263(self, entry, old_key):
        if not isinstance(entry, dict):
            return entry
        entry = dict(entry)
        if old_key in entry and "type" not in entry:
            entry["type"] = entry.pop(old_key)
        return self.loot_table(entry)

    def exploration_map(self, destination_tag, decoration, *, zoom=2, search_radius=None, skip_existing_chunks=True):
        """Build an exploration_map loot modifier with version-appropriate destination tag prefix."""
        bare = destination_tag.removeprefix("#")
        dest = bare if self.is_26_2 else f"#{bare}"
        entry = {
            "function": "minecraft:exploration_map",
            "destination": dest,
            "decoration": decoration,
            "zoom": zoom,
        }
        if search_radius is not None:
            entry["search_radius"] = search_radius
        entry["skip_existing_chunks"] = skip_existing_chunks
        return entry

    def block_property_condition(self, block, properties):
        """Build a block-state loot condition (block_state_property in 26.2 vs match_block in 26.3)."""
        if self.is_26_2:
            return {"condition": "minecraft:block_state_property", "block": block, "properties": dict(properties)}
        return {"condition": "minecraft:match_block", "blocks": block, "state": dict(properties)}

    # ---------------------------------------------------------------- advancements

    def recipe_unlock_advancement(self, recipe_id, trigger_item):
        """Build a vanilla-style recipe unlock advancement payload."""
        recipe_cond = {"recipe": recipe_id} if self.is_26_2 else {"recipes": recipe_id}
        return {
            "parent": "minecraft:recipes/root",
            "criteria": {
                "has_item": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": trigger_item}]}},
                "has_the_recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": recipe_cond},
            },
            "requirements": [["has_the_recipe", "has_item"]],
            "rewards": {"recipes": [recipe_id]},
        }

    def in_structure_criterion(self, structure):
        """Build a location advancement trigger checking the player is inside a structure."""
        predicate = {"entity": "this", "predicate": {"minecraft:location": {"structures": structure}}}
        if self.is_26_2:
            player = [{"condition": "minecraft:entity_properties", **predicate}]
        else:
            player = {"type": "minecraft:entity_properties", **predicate}
        return {"trigger": "minecraft:location", "conditions": {"player": player}}

    # ---------------------------------------------------------------- worldgen features

    @property
    def feature_subdir(self):
        return "worldgen/configured_feature" if self.is_26_2 else "worldgen/feature"

    @property
    def stale_feature_subdir(self):
        return "worldgen/feature" if self.is_26_2 else "worldgen/configured_feature"

    def clean_stale_feature_dir(self, data_dir):
        """Remove configured_feature/ or feature/ belonging to the other Minecraft version."""
        stale = Path(data_dir) / self.stale_feature_subdir
        if stale.exists():
            shutil.rmtree(stale)

    def unit_feature_json(self, feature_type):
        """Configured feature JSON for a zero-config feature."""
        if self.is_26_2:
            return {"type": feature_type, "config": {}}
        return {"type": feature_type}

    def simple_block_feature_json(self, block_id, properties=None):
        """Configured simple_block feature JSON across SimpleStateProvider (26.2) vs inline state (26.3)."""
        if self.is_26_2:
            state = {"Name": block_id}
            if properties:
                state["Properties"] = dict(properties)
            return {
                "type": "minecraft:simple_block",
                "config": {"to_place": {"type": "minecraft:simple_state_provider", "state": state}},
            }
        to_place = {"id": block_id}
        if properties:
            to_place["properties"] = dict(properties)
        return {"type": "minecraft:simple_block", "to_place": to_place}

    def placed_offset_modifier(self, x_provider, y_provider, z_provider=None):
        """PlacedFeature offset modifier (random_offset in 26.2 vs offset in 26.3)."""
        if self.is_26_2:
            return {"type": "minecraft:random_offset", "xz_spread": x_provider, "y_spread": y_provider}
        z = x_provider if z_provider is None else z_provider
        return {"type": "minecraft:offset", "x": x_provider, "y": y_provider, "z": z}

    # ---------------------------------------------------------------- spawns & biomes

    def spawner_entry(self, entity_type, weight, min_count, max_count=None):
        """Mob spawn entry for biome spawners and structure spawn_overrides."""
        hi = min_count if max_count is None else max_count
        if self.is_26_2:
            return {"type": entity_type, "weight": weight, "minCount": min_count, "maxCount": hi}
        if min_count == hi:
            count = min_count
        else:
            count = {"type": "minecraft:uniform", "min_inclusive": min_count, "max_inclusive": hi}
        return {"type": entity_type, "weight": weight, "count": count}

    def apply_biome_spawns(self, biome, spawns_by_category, spawn_costs=None):
        """Attach mob spawns either at top level (26.2) or via natural_mob_spawns attribute (26.3)."""
        costs = {} if spawn_costs is None else dict(spawn_costs)
        if self.is_26_2:
            out = {}
            for key, value in biome.items():
                out[key] = value
                if key == "attributes":
                    out["spawn_costs"] = costs
                    out["spawners"] = {cat: list(spawns_by_category.get(cat, [])) for cat in SPAWNER_CATEGORIES}
            if "attributes" not in out:
                out["spawn_costs"] = costs
                out["spawners"] = {cat: list(spawns_by_category.get(cat, [])) for cat in SPAWNER_CATEGORIES}
            return out

        out = dict(biome)
        attrs = {}
        inserted = False
        spawn_attr = {
            "modifier": "overlay",
            "argument": {
                "spawn_costs": costs,
                "spawns_by_category": {k: list(v) for k, v in spawns_by_category.items()},
            },
        }
        for key, value in biome.get("attributes", {}).items():
            if not inserted and key.startswith("minecraft:visual/"):
                attrs["minecraft:gameplay/natural_mob_spawns"] = spawn_attr
                inserted = True
            attrs[key] = value
        if not inserted:
            attrs["minecraft:gameplay/natural_mob_spawns"] = spawn_attr
        out["attributes"] = attrs
        return out

    # ---------------------------------------------------------------- trades, gear, brewing

    def villager_item_modifier_field(self, modifier):
        """Build the given_item_modifiers (26.2) or given_item_modifier (26.3) trade entry."""
        if self.is_26_2:
            entry = {("function" if k == "type" else k): v for k, v in modifier.items()}
            return {"given_item_modifiers": [entry]}
        entry = {("type" if k == "function" else k): v for k, v in modifier.items()}
        return {"given_item_modifier": entry}

    def glaive_swing_component(self):
        """Return the smithing component key/value for skyrend_glaive swing vs attack animation."""
        key = "minecraft:swing_animation" if self.is_26_2 else "minecraft:attack_animation"
        return key, {"type": "whack", "duration": 10}

    @property
    def brewing_subdir(self):
        return "brewing" if self.is_26_2 else "recipe/brewing"

    @property
    def stale_brewing_subdir(self):
        return "recipe/brewing" if self.is_26_2 else "brewing"

    def clean_stale_brewing_dir(self, data_dir):
        """Remove brewing/ or recipe/brewing/ belonging to the other Minecraft version."""
        stale = Path(data_dir) / self.stale_brewing_subdir
        if stale.exists():
            shutil.rmtree(stale)
