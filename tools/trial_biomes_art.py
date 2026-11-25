"""Custom biome flora, resources, creatures (Leyweaver Moth & Caldera Cinderhound),
and Sanctum Wayfinder assets, geo/animation rigs, loot tables, recipes, and features.
"""
from __future__ import annotations

import json
from pathlib import Path

from PIL import Image

LANG = {
    "block.wildercord.leybloom_cluster": "Leybloom Cluster",
    "item.wildercord.leybloom_cluster.lore": "Luminous azure crystal bloom rooted along Astral Ley-Glade spires.",
    "block.wildercord.caldera_slag_vent": "Caldera Slag Vent",
    "item.wildercord.caldera_slag_vent.lore": "Smoldering basalt fumarole formed over the magma vents of Cinder Calderas.",
    "item.wildercord.astral_ley_shard": "Astral Ley Shard",
    "item.wildercord.astral_ley_shard.lore": "Resonant turquoise crystal sheared from Leybloom Clusters and Leyweaver Moths.",
    "item.wildercord.caldera_slag_core": "Caldera Slag Core",
    "item.wildercord.caldera_slag_core.lore": "Heavy molten-iron slag core forged inside volcanic caldera vents.",
    "item.wildercord.sanctum_wayfinder": "Sanctum Wayfinder",
    "item.wildercord.sanctum_wayfinder.lore": "Tri-needle resonance compass attuned to hidden Co-Op Trial Sanctums.",
    "tooltip.wildercord.wayfinder.target": "Target: %s",
    "tooltip.wildercord.wayfinder.hint": "Right-click to pulse bearing; sneak-right-click to cycle Sanctum",
    "tooltip.wildercord.wayfinder.target.trial_leyline_sanctum": "Leyline Sanctum (Trial I)",
    "tooltip.wildercord.wayfinder.target.trial_ashen_crucible": "Ashen Crucible (Trial II)",
    "tooltip.wildercord.wayfinder.target.trial_astral_eclipse": "Astral Eclipse Vault (Trial III)",
    "message.wildercord.wayfinder.mode": "Wayfinder attuned to: %s",
    "message.wildercord.wayfinder.unavailable": "That Trial Sanctum is not awakened in this world yet.",
    "message.wildercord.wayfinder.missing": "No %s detected within survey range.",
    "message.wildercord.wayfinder.found": "%s detected ~%s blocks %s.",
    "entity.wildercord.leyweaver_moth": "Leyweaver Moth",
    "item.wildercord.leyweaver_moth_spawn_egg": "Leyweaver Moth Spawn Egg",
    "guide.wildercord.leyweaver_moth": "Gentle winged spirits drawn to leyline spires inside Astral Ley-Glades. Sheds luminous dust and occasionally drops Astral Ley Shards.",
    "guide.wildercord.leyweaver_moth.hint": "Seek turquoise spires inside high-weirdness dark forests (Astral Ley-Glades).",
    "entity.wildercord.caldera_cinderhound": "Caldera Cinderhound",
    "item.wildercord.caldera_cinderhound_spawn_egg": "Caldera Cinderhound Spawn Egg",
    "guide.wildercord.caldera_cinderhound": "Magma-veined basalt hounds prowling volcanic Cinder Calderas. Fire-immune predators yielding Caldera Slag Cores for Trial crafting.",
    "guide.wildercord.caldera_cinderhound.hint": "Search volcanic basalt basins inside high-weirdness eroded badlands (Cinder Calderas).",
}


def _box_uv_face(u, v, w, h):
    return {"uv": [u, v], "uv_size": [w, h]}


def _cube(origin, size, u, v, inflate=0.0, pivot=None, rot=None):
    w, h, d = int(size[0]), int(size[1]), int(size[2])
    uv = {
        "north": _box_uv_face(u + d, v + d, w, h),
        "east": _box_uv_face(u + d + w, v + d, d, h),
        "south": _box_uv_face(u + d + w + d, v + d, w, h),
        "west": _box_uv_face(u, v + d, d, h),
        "up": _box_uv_face(u + d, v, w, d),
        "down": _box_uv_face(u + d + w, v, w, d),
    }
    c = {"origin": list(origin), "size": list(size), "uv": uv}
    if inflate != 0.0:
        c["inflate"] = inflate
    if pivot is not None:
        c["pivot"] = list(pivot)
    if rot is not None:
        c["rotation"] = list(rot)
    return c


def _fill_rect(px, x0, y0, w, h, color):
    for yy in range(max(0, y0), min(64, y0 + h)):
        for xx in range(max(0, x0), min(64, x0 + w)):
            px[xx, yy] = color


def _leyweaver_moth_assets(g):
    geo = {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.leyweaver_moth",
                "texture_width": 64, "texture_height": 64,
                "visible_bounds_width": 2, "visible_bounds_height": 2, "visible_bounds_offset": [0, 0.75, 0],
            },
            "bones": [
                {"name": "root", "pivot": [0, 0, 0]},
                {"name": "body", "parent": "root", "pivot": [0, 9, 0], "cubes": [
                    _cube([-2, 7, -4], [4, 4, 8], 0, 0),
                    _cube([-1.5, 7.5, 4], [3, 3, 4], 0, 14),
                ]},
                {"name": "head", "parent": "body", "pivot": [0, 9, -4], "cubes": [
                    _cube([-2, 8, -7], [4, 3, 3], 24, 0),
                    _cube([-2, 11, -7.5], [1, 3, 1], 40, 0, rot=[20, 0, -15], pivot=[-1.5, 11, -7]),
                    _cube([1, 11, -7.5], [1, 3, 1], 40, 0, rot=[20, 0, 15], pivot=[1.5, 11, -7]),
                ]},
                {"name": "wing_left", "parent": "body", "pivot": [-2, 10.5, -1], "cubes": [
                    _cube([-12, 10, -4], [10, 1, 7], 0, 26),
                    _cube([-10, 9.5, 3], [7, 1, 5], 0, 36),
                ]},
                {"name": "wing_right", "parent": "body", "pivot": [2, 10.5, -1], "cubes": [
                    _cube([2, 10, -4], [10, 1, 7], 0, 26),
                    _cube([3, 9.5, 3], [7, 1, 5], 0, 36),
                ]},
            ],
        }],
    }
    anim = {
        "format_version": "1.8.0",
        "animations": {
            "animation.leyweaver_moth.idle": {
                "loop": True, "animation_length": 0.8,
                "bones": {
                    "body": {"position": {"0.0": [0, 0, 0], "0.4": [0, 1.5, 0], "0.8": [0, 0, 0]}},
                    "wing_left": {"rotation": {"0.0": [0, 0, -24], "0.4": [0, 0, 22], "0.8": [0, 0, -24]}},
                    "wing_right": {"rotation": {"0.0": [0, 0, 24], "0.4": [0, 0, -22], "0.8": [0, 0, 24]}},
                },
            },
            "animation.leyweaver_moth.walk": {
                "loop": True, "animation_length": 0.48,
                "bones": {
                    "body": {"position": {"0.0": [0, 0.5, 0], "0.24": [0, 2.2, 0], "0.48": [0, 0.5, 0]},
                             "rotation": {"0.0": [10, 0, 0], "0.24": [4, 0, 0], "0.48": [10, 0, 0]}},
                    "wing_left": {"rotation": {"0.0": [0, 0, -36], "0.24": [0, 0, 32], "0.48": [0, 0, -36]}},
                    "wing_right": {"rotation": {"0.0": [0, 0, 36], "0.24": [0, 0, -32], "0.48": [0, 0, 36]}},
                },
            },
        },
    }
    diff = Image.new("RGBA", (64, 64), (24, 34, 62, 255))
    glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    dpx, gpx = diff.load(), glow.load()
    _fill_rect(dpx, 0, 0, 24, 24, (36, 48, 86, 255))
    _fill_rect(dpx, 24, 0, 16, 10, (46, 62, 104, 255))
    _fill_rect(dpx, 40, 0, 8, 8, (226, 184, 78, 255))
    _fill_rect(dpx, 0, 26, 36, 20, (30, 128, 138, 255))
    # Glowing eyes & cyan wing veins
    _fill_rect(gpx, 27, 4, 2, 2, (124, 245, 224, 255))
    _fill_rect(gpx, 31, 4, 2, 2, (124, 245, 224, 255))
    _fill_rect(gpx, 8, 34, 18, 2, (96, 245, 224, 255))
    _fill_rect(gpx, 8, 43, 14, 2, (236, 198, 92, 255))
    return geo, anim, diff, glow


def _caldera_cinderhound_assets(g):
    geo = {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.caldera_cinderhound",
                "texture_width": 64, "texture_height": 64,
                "visible_bounds_width": 2, "visible_bounds_height": 2, "visible_bounds_offset": [0, 0.75, 0],
            },
            "bones": [
                {"name": "root", "pivot": [0, 0, 0]},
                {"name": "body", "parent": "root", "pivot": [0, 8, 0], "cubes": [
                    _cube([-4, 5, -6], [8, 6, 12], 0, 0),
                    _cube([-3, 11, -4], [6, 1.5, 8], 0, 20),
                ]},
                {"name": "head", "parent": "body", "pivot": [0, 9, -6], "cubes": [
                    _cube([-3.5, 6.5, -12], [7, 5.5, 6], 36, 0),
                    _cube([-2, 6, -15], [4, 3, 3], 36, 14),
                    _cube([-3, 12, -8], [1.5, 3, 2], 54, 14),
                    _cube([1.5, 12, -8], [1.5, 3, 2], 54, 14),
                ]},
                {"name": "tail", "parent": "body", "pivot": [0, 9.5, 6], "cubes": [
                    _cube([-1.5, 8.5, 6], [3, 2.5, 6], 24, 22, rot=[18, 0, 0], pivot=[0, 9.5, 6]),
                ]},
                {"name": "leg_fl", "parent": "root", "pivot": [-3, 6, -4], "cubes": [_cube([-4.5, 0, -5.5], [3, 6, 3], 0, 32)]},
                {"name": "leg_fr", "parent": "root", "pivot": [3, 6, -4], "cubes": [_cube([1.5, 0, -5.5], [3, 6, 3], 0, 32)]},
                {"name": "leg_bl", "parent": "root", "pivot": [-3, 6, 4], "cubes": [_cube([-4.5, 0, 2.5], [3, 6, 3], 14, 32)]},
                {"name": "leg_br", "parent": "root", "pivot": [3, 6, 4], "cubes": [_cube([1.5, 0, 2.5], [3, 6, 3], 14, 32)]},
            ],
        }],
    }
    anim = {
        "format_version": "1.8.0",
        "animations": {
            "animation.caldera_cinderhound.idle": {
                "loop": True, "animation_length": 2.0,
                "bones": {
                    "body": {"position": {"0.0": [0, 0, 0], "1.0": [0, 0.35, 0], "2.0": [0, 0, 0]}},
                    "head": {"rotation": {"0.0": [0, 0, 0], "1.0": [4, 0, 0], "2.0": [0, 0, 0]}},
                    "tail": {"rotation": {"0.0": [0, -10, 0], "1.0": [0, 10, 0], "2.0": [0, -10, 0]}},
                },
            },
            "animation.caldera_cinderhound.walk": {
                "loop": True, "animation_length": 0.8,
                "bones": {
                    "body": {"position": {"0.0": [0, 0, 0], "0.2": [0, 0.5, 0], "0.4": [0, 0, 0], "0.6": [0, 0.5, 0], "0.8": [0, 0, 0]}},
                    "leg_fl": {"rotation": {"0.0": [28, 0, 0], "0.4": [-28, 0, 0], "0.8": [28, 0, 0]}},
                    "leg_br": {"rotation": {"0.0": [28, 0, 0], "0.4": [-28, 0, 0], "0.8": [28, 0, 0]}},
                    "leg_fr": {"rotation": {"0.0": [-28, 0, 0], "0.4": [28, 0, 0], "0.8": [-28, 0, 0]}},
                    "leg_bl": {"rotation": {"0.0": [-28, 0, 0], "0.4": [28, 0, 0], "0.8": [-28, 0, 0]}},
                    "tail": {"rotation": {"0.0": [6, -14, 0], "0.4": [6, 14, 0], "0.8": [6, -14, 0]}},
                },
            },
        },
    }
    diff = Image.new("RGBA", (64, 64), (36, 30, 34, 255))
    glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    dpx, gpx = diff.load(), glow.load()
    _fill_rect(dpx, 0, 0, 42, 20, (42, 34, 38, 255))
    _fill_rect(dpx, 0, 20, 24, 12, (218, 92, 26, 255))
    _fill_rect(dpx, 36, 0, 26, 20, (54, 42, 44, 255))
    # Magma ridge + eyes glow
    _fill_rect(gpx, 4, 22, 16, 8, (255, 138, 36, 255))
    _fill_rect(gpx, 43, 8, 2, 2, (255, 196, 64, 255))
    _fill_rect(gpx, 49, 8, 2, 2, (255, 196, 64, 255))
    return geo, anim, diff, glow


def _tex_block_leybloom():
    img = Image.new("RGBA", (16, 16), (22, 28, 46, 255))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if (x in (7, 8) and y >= 2) or (x in (4, 5, 10, 11) and y >= 6):
                px[x, y] = (108, 246, 226, 255) if (x + y) % 3 != 0 else (188, 255, 248, 255)
            elif y >= 13:
                px[x, y] = (46, 54, 78, 255)
            elif abs(x - 7.5) <= 4.5 and y >= 9:
                px[x, y] = (64, 152, 168, 255)
    return img


def _tex_block_vent():
    img = Image.new("RGBA", (16, 16), (34, 32, 36, 255))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if d <= 2.5:
                px[x, y] = (255, 148, 38, 255) if (x + y) % 2 == 0 else (230, 88, 24, 255)
            elif d <= 4.5:
                px[x, y] = (62, 54, 58, 255)
            elif (x * 3 + y * 5) % 7 == 0:
                px[x, y] = (24, 22, 26, 255)
    return img


def _icon_ley_shard():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for i in range(10):
        px[8 - i // 3, 2 + i] = (128, 250, 232, 255)
        px[9 - i // 3, 2 + i] = (54, 178, 176, 255)
    for x in range(5, 11):
        px[x, 12] = (188, 255, 248, 255)
    return img


def _icon_slag_core():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(4, 12):
        for x in range(4, 12):
            r = (x - 7.5) ** 2 + (y - 7.5) ** 2
            if r <= 13:
                px[x, y] = (255, 178, 56, 255) if r <= 4.5 else (62, 46, 48, 255)
    return img


def _icon_wayfinder():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    gold = (224, 178, 72, 255)
    dark = (36, 40, 58, 255)
    cyan = (108, 246, 226, 255)
    amber = (255, 132, 46, 255)
    for y in range(2, 14):
        for x in range(2, 14):
            r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 4.6 <= r <= 5.8:
                px[x, y] = gold
            elif r < 4.6:
                px[x, y] = dark
    for y in range(3, 8):
        px[7, y] = cyan
        px[8, y] = cyan
    for y in range(8, 12):
        px[7, y] = amber
        px[8, y] = amber
    px[7, 7] = (255, 255, 255, 255)
    px[8, 8] = (255, 255, 255, 255)
    return img


def _icon_egg(base, spots):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(2, 14):
        rx = 4.6 if y >= 6 else 2.2 + (y - 2) * 0.6
        for x in range(2, 14):
            if ((x - 7.5) / rx) ** 2 + ((y - 8.0) / 5.6) ** 2 <= 1.0:
                px[x, y] = spots if (x * 5 + y * 7) % 11 in (0, 3) else base
    return img


def _write_compact(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, separators=(",", ":"), ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")


def write(g):
    moth_geo, moth_anim, moth_diff, moth_glow = _leyweaver_moth_assets(g)
    hound_geo, hound_anim, hound_diff, hound_glow = _caldera_cinderhound_assets(g)

    for name, geo, anim, diff, glow in (
        ("leyweaver_moth", moth_geo, moth_anim, moth_diff, moth_glow),
        ("caldera_cinderhound", hound_geo, hound_anim, hound_diff, hound_glow),
    ):
        g.save(diff, g.ASSETS / f"textures/entity/trials/{name}.png")
        g.save(glow, g.ASSETS / f"textures/entity/trials/{name}_glow.png")
        _write_compact(g.ASSETS / f"models/entity/trials/{name}.geo.json", geo)
        _write_compact(g.ASSETS / f"animations/entity/trials/{name}.animation.json", anim)

    # Blocks
    g.save(_tex_block_leybloom(), g.ASSETS / "textures/block/leybloom_cluster.png")
    g.save(_tex_block_vent(), g.ASSETS / "textures/block/caldera_slag_vent.png")

    g.write_json(g.ASSETS / "blockstates/leybloom_cluster.json", {"variants": {"": {"model": "wildercord:block/leybloom_cluster"}}})
    g.write_json(g.ASSETS / "blockstates/caldera_slag_vent.json", {"variants": {"": {"model": "wildercord:block/caldera_slag_vent"}}})

    g.write_json(g.ASSETS / "models/block/leybloom_cluster.json", {
        "textures": {"all": "wildercord:block/leybloom_cluster", "particle": "wildercord:block/leybloom_cluster"},
        "elements": [
            {"from": [4, 0, 4], "to": [12, 3, 12], "faces": {f: {"texture": "#all"} for f in ("north", "south", "east", "west", "up", "down")}},
            {"from": [6, 3, 6], "to": [10, 12, 10], "faces": {f: {"texture": "#all"} for f in ("north", "south", "east", "west", "up", "down")}},
            {"from": [3.5, 2, 7], "to": [6.5, 8, 10], "faces": {f: {"texture": "#all"} for f in ("north", "south", "east", "west", "up", "down")}},
            {"from": [9.5, 2, 5.5], "to": [12.5, 7, 8.5], "faces": {f: {"texture": "#all"} for f in ("north", "south", "east", "west", "up", "down")}},
        ],
    })
    g.write_json(g.ASSETS / "models/block/caldera_slag_vent.json", {
        "textures": {"all": "wildercord:block/caldera_slag_vent", "particle": "wildercord:block/caldera_slag_vent"},
        "elements": [
            {"from": [1, 0, 1], "to": [15, 6, 15], "faces": {f: {"texture": "#all"} for f in ("north", "south", "east", "west", "up", "down")}},
            {"from": [3, 6, 3], "to": [13, 12, 13], "faces": {f: {"texture": "#all"} for f in ("north", "south", "east", "west", "up", "down")}},
            {"from": [5, 12, 5], "to": [11, 14, 11], "faces": {f: {"texture": "#all"} for f in ("north", "south", "east", "west", "up", "down")}},
        ],
    })
    for bid in ("leybloom_cluster", "caldera_slag_vent"):
        g.write_json(g.ASSETS / f"models/item/{bid}.json", {"parent": f"wildercord:block/{bid}"})
        g.write_json(g.ASSETS / f"items/{bid}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{bid}"}})

    # Block loot tables
    for blk, drop in (("leybloom_cluster", "wildercord:astral_ley_shard"), ("caldera_slag_vent", "wildercord:caldera_slag_core")):
        g.write_json(g.DATA / f"loot_table/blocks/{blk}.json", {
            "type": "minecraft:block",
            "pools": [{
                "rolls": 1,
                "entries": [{"type": "minecraft:item", "name": drop,
                             "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}]}],
                "conditions": [{"condition": "minecraft:survives_explosion"}],
            }],
        })

    # Entity loot tables
    g.write_json(g.DATA / "loot_table/entities/leyweaver_moth.json", {
        "type": "minecraft:entity",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "wildercord:astral_ley_shard",
                   "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}]}]}],
    })
    g.write_json(g.DATA / "loot_table/entities/caldera_cinderhound.json", {
        "type": "minecraft:entity",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "wildercord:caldera_slag_core",
                   "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}]}]}],
    })

    # Items
    icons = {
        "astral_ley_shard": _icon_ley_shard(),
        "caldera_slag_core": _icon_slag_core(),
        "sanctum_wayfinder": _icon_wayfinder(),
        "leyweaver_moth_spawn_egg": _icon_egg((32, 64, 96, 255), (108, 246, 226, 255)),
        "caldera_cinderhound_spawn_egg": _icon_egg((46, 36, 38, 255), (255, 136, 36, 255)),
    }
    for iid, img in icons.items():
        g.save(img, g.ASSETS / f"textures/item/{iid}.png")
        g.write_json(g.ASSETS / f"models/item/{iid}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"wildercord:item/{iid}"}})
        g.write_json(g.ASSETS / f"items/{iid}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{iid}"}})

    # Worldgen features & placed features
    for fid in ("ley_spire_patch", "caldera_fumarole_patch"):
        g.write_json(g.DATA / f"{g.profile.feature_subdir}/{fid}.json", g.profile.unit_feature_json(f"wildercord:{fid}"))
        g.write_json(g.DATA / f"worldgen/placed_feature/{fid}.json", {
            "feature": f"wildercord:{fid}",
            "placement": [
                {"type": "minecraft:rarity_filter", "chance": 6},
                {"type": "minecraft:in_square"},
                {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"},
                {"type": "minecraft:biome"},
            ],
        })

    # Recipes
    g.write_json(g.DATA / "recipe/sanctum_wayfinder.json", {
        "type": "minecraft:crafting_shaped",
        "category": "equipment",
        "pattern": [" L ", "ACA", " S "],
        "key": {
            "L": "wildercord:astral_ley_shard",
            "A": "minecraft:amethyst_shard",
            "C": "minecraft:compass",
            "S": "wildercord:caldera_slag_core",
        },
        "result": {"id": "wildercord:sanctum_wayfinder"},
    })
    g.unlock_advancement("wildercord:sanctum_wayfinder", "wildercord:astral_ley_shard")

    g.write_json(g.DATA / "recipe/leyline_sigil_from_biomes.json", {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": [" L ", "ECE", " S "],
        "key": {
            "L": "wildercord:astral_ley_shard",
            "E": "minecraft:gold_ingot",
            "C": "wildercord:mana_crystal",
            "S": "wildercord:caldera_slag_core",
        },
        "result": {"id": "wildercord:leyline_sigil"},
    })
    g.unlock_advancement("wildercord:leyline_sigil_from_biomes", "wildercord:astral_ley_shard")
