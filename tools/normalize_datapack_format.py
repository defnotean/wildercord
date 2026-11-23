"""Rewrite checked-in datapack JSON into the active branch's Minecraft schema.

Main authors loot tables and recipe-unlock advancements in the 26.3 schema
(``modifier``/``condition``/``recipes``). Every pre-26.3 branch needs the older
schema (``functions``/``conditions``/``recipe``) or Minecraft silently ignores
the keys and the drops never appear. ``DataPackProfile`` already knows both
shapes; this walks the shipped resources and normalizes them, so a down-port
can fix a whole wave of new content with one command instead of hand edits.

Usage:
    python tools/normalize_datapack_format.py            # rewrite in place
    python tools/normalize_datapack_format.py --check    # report only, exit 1
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

from datapack_profile import DataPackProfile

DATA = Path("src/main/resources/data/wildercord")
RECIPE_UNLOCK_TRIGGER = "minecraft:recipe_unlocked"


def _player_condition(profile: DataPackProfile, node):
    """Swap the advancement ``player`` condition: a list of conditions before 26.3, one object after."""
    if not isinstance(node, dict) or "player" not in node:
        return node
    value = node["player"]
    items = value if isinstance(value, list) else [value]
    converted = []
    for item in items:
        if not isinstance(item, dict):
            converted.append(item)
            continue
        item = dict(item)
        if profile.is_26_2:
            if "type" in item and "condition" not in item:
                item["condition"] = item.pop("type")
        elif "condition" in item and "type" not in item:
            item["type"] = item.pop("condition")
        converted.append(item)
    if profile.is_26_2:
        node["player"] = converted
    else:
        node["player"] = converted[0] if len(converted) == 1 else {"type": "minecraft:all_of", "terms": converted}
    return node


def _recipe_unlock_condition(profile: DataPackProfile, node):
    """Swap the recipe_unlocked condition key to the active schema, recursively."""
    if isinstance(node, list):
        return [_recipe_unlock_condition(profile, item) for item in node]
    if not isinstance(node, dict):
        return node
    out = dict(node)
    if out.get("trigger") == RECIPE_UNLOCK_TRIGGER and isinstance(out.get("conditions"), dict):
        conditions = dict(out["conditions"])
        if profile.is_26_2:
            if "recipes" in conditions:
                conditions["recipe"] = conditions.pop("recipes")
        elif "recipe" in conditions:
            conditions["recipes"] = conditions.pop("recipe")
        out["conditions"] = conditions
    if isinstance(out.get("conditions"), dict):
        out["conditions"] = _player_condition(profile, out["conditions"])
    return {key: _recipe_unlock_condition(profile, value) for key, value in out.items()}


def normalize_loot(profile: DataPackProfile, data):
    """Return the loot table JSON in the active schema."""
    return profile.loot_table(data)


def normalize_advancement(profile: DataPackProfile, data):
    """Return the advancement JSON with a schema-correct recipe_unlocked condition."""
    return _recipe_unlock_condition(profile, data)


def _json_bytes(data) -> bytes:
    return (json.dumps(data, indent=2, ensure_ascii=False) + "\n").encode("utf-8")


def rewrite_tree(root: Path, profile: DataPackProfile, relative: str, transform, check: bool) -> list[str]:
    """Apply ``transform`` to every JSON file under ``relative``; return the files that differ."""
    base = root / relative
    if not base.exists():
        return []
    changed = []
    for path in sorted(base.rglob("*.json")):
        original = json.loads(path.read_text(encoding="utf-8"))
        normalized = transform(profile, original)
        if normalized == original:
            continue
        changed.append(path.relative_to(root).as_posix())
        if not check:
            path.write_bytes(_json_bytes(normalized))
    return changed


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=".", help="Repository root (default: current directory)")
    parser.add_argument("--check", action="store_true", help="Report what needs rewriting without writing")
    args = parser.parse_args(argv)

    root = Path(args.root).resolve()
    profile = DataPackProfile.from_gradle(root)
    trees = [
        (DATA / "loot_table", normalize_loot),
        (DATA / "advancement", normalize_advancement),
    ]
    total = 0
    for relative, transform in trees:
        changed = rewrite_tree(root, profile, relative.as_posix(), transform, args.check)
        for name in changed:
            print(f"{'would rewrite' if args.check else 'rewrote'}: {name}")
        total += len(changed)
    print(f"profile: {profile.version}  files: {total}")
    return 1 if (args.check and total) else 0


if __name__ == "__main__":
    sys.exit(main())
