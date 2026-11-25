"""Trial II (Ashen Crucible & Kharvok) assets: block/entity textures, Bedrock rig/anim, relics, advancements, and English text."""
from __future__ import annotations

import json
from pathlib import Path

from PIL import Image

LANG = {
    "entity.wildercord.kharvok": "Kharvok, the Crucible Sovereign",
    "block.wildercord.crucible_vent": "Crucible Quench Vent",
    "item.wildercord.crucible_vent.lore": "Pressurized foundry valve ignited by Kharvok during Chain Shackle.",
    "item.wildercord.crucible_maul": "Crucible Greathammer",
    "item.wildercord.crucible_maul.lore": "Slag-iron warmaul tempered in molten caldera veins.",
    "item.wildercord.crucible_maul.use": "Right-click to slam the ground in a fiery shockwave, launching enemies skyward.",
    "item.wildercord.forgeheart_censer": "Forgeheart Censer",
    "item.wildercord.forgeheart_censer.lore": "Smoldering bronze brazier that bolsters party vigor amidst molten fury.",
    "item.wildercord.forgeheart_censer.use": "Right-click to pulse Fire Resistance, Strength, and healing across nearby allies.",
    "boss.wildercord.kharvok.status": "%s — %s",
    "boss.wildercord.kharvok.idle": "Stoking the Furnace",
    "boss.wildercord.kharvok.stride": "Marching",
    "boss.wildercord.kharvok.anvil_shatter": "Anvil Shatter!",
    "boss.wildercord.kharvok.chain_shackle": "Chain Shackle — Quench the vents!",
    "boss.wildercord.kharvok.crucible_meltdown": "Crucible Meltdown — Cool the rings!",
    "boss.wildercord.kharvok.staggered": "Furnace Quenched — Staggered!",
    "message.wildercord.trial.vent_idle": "This Crucible Vent is dormant.",
    "message.wildercord.trial.vent_quenched": "Crucible Vent quenched (%s remaining)!",
    "message.wildercord.trial.vent_broken": "All Crucible Vents quenched — Kharvok staggers!",
    "message.wildercord.forgeheart_censer.cooldown": "Forgeheart Censer is reheating (%ss).",
    "message.wildercord.forgeheart_censer.pulse": "Forgeheart Censer erupts — allies granted Fire Resistance & Strength!",
    "advancements.wildercord.world.defeat_kharvok.title": "Crucible Sovereign Quenched",
    "advancements.wildercord.world.defeat_kharvok.description": "Defeat Kharvok, the Crucible Sovereign inside the Ashen Crucible",
}


def _round_numbers(obj):
    if isinstance(obj, float):
        return round(obj, 4)
    if isinstance(obj, list):
        return [_round_numbers(x) for x in obj]
    if isinstance(obj, dict):
        return {k: _round_numbers(v) for k, v in obj.items()}
    return obj


def _write_compact_json(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, separators=(",", ":"), ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")


def _icon_maul():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    shaft = (146, 98, 42, 255)
    iron = (36, 38, 46, 255)
    bronze = (212, 160, 72, 255)
    ember = (255, 138, 34, 255)
    gold = (255, 224, 118, 255)
    for i in range(2, 12):
        px[i, 15 - i] = bronze if i % 3 == 0 else shaft
    for y in range(2, 8):
        for x in range(8, 15):
            if abs((x - 11) + (y - 4.5)) <= 3.2:
                if x == 11 and y in (4, 5):
                    px[x, y] = gold
                elif abs(x - 11) <= 1 and abs(y - 4.5) <= 1.5:
                    px[x, y] = ember
                elif y in (2, 7) or x in (8, 14):
                    px[x, y] = bronze
                else:
                    px[x, y] = iron
    return img


def _icon_censer():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    bronze = (206, 146, 62, 255)
    bronze_d = (128, 82, 32, 255)
    ember = (255, 128, 28, 255)
    gold = (255, 228, 132, 255)
    for y in range(1, 5):
        px[7, y] = bronze if y % 2 else bronze_d
        px[8, y] = bronze_d
    for y in range(5, 13):
        half = 2 if y in (5, 12) else (4 if 7 <= y <= 10 else 3)
        for x in range(8 - half, 8 + half):
            if 7 <= y <= 10 and 6 <= x <= 9:
                px[x, y] = gold if 8 <= y <= 9 and 7 <= x <= 8 else ember
            elif x in (8 - half, 7 + half) or y in (5, 12):
                px[x, y] = bronze_d
            else:
                px[x, y] = bronze
    return img


def _tex_crucible_vent():
    img = Image.new("RGBA", (16, 16), (34, 36, 44, 255))
    px = img.load()
    bronze = (188, 130, 56, 255)
    ember = (255, 140, 34, 255)
    gold = (255, 218, 114, 255)
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15) or x in (3, 12) and 3 <= y <= 12:
                px[x, y] = bronze
            elif 4 <= x <= 11 and 4 <= y <= 11:
                if x in (6, 9) and 5 <= y <= 10:
                    px[x, y] = (34, 36, 44, 255)
                else:
                    px[x, y] = gold if (6 <= y <= 9 and 7 <= x <= 8) else ember
            elif (x + y) % 5 == 0:
                px[x, y] = (52, 56, 68, 255)
    return img


def write(g):
    src_dir = g.ROOT / "art/blockbench/kharvok"
    with Image.open(src_dir / "kharvok_diffuse.png") as diff:
        g.save(diff.convert("RGBA"), g.ASSETS / "textures/entity/trials/kharvok.png")
    with Image.open(src_dir / "kharvok_glow.png") as glow:
        g.save(glow.convert("RGBA"), g.ASSETS / "textures/entity/trials/kharvok_glow.png")

    geo_data = _round_numbers(json.loads((src_dir / "kharvok_crucible_sovereign.geo.json").read_text(encoding="utf-8")))
    anim_data = _round_numbers(json.loads((src_dir / "kharvok_crucible_sovereign.animation.json").read_text(encoding="utf-8")))
    _write_compact_json(g.ASSETS / "models/entity/trials/kharvok.geo.json", geo_data)
    _write_compact_json(g.ASSETS / "animations/entity/trials/kharvok.animation.json", anim_data)

    g.save(_tex_crucible_vent(), g.ASSETS / "textures/block/crucible_vent.png")
    g.write_json(g.ASSETS / "blockstates/crucible_vent.json", {"variants": {"": {"model": "wildercord:block/crucible_vent"}}})
    g.write_json(g.ASSETS / "models/block/crucible_vent.json", {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": "wildercord:block/crucible_vent"},
    })
    g.write_json(g.ASSETS / "models/item/crucible_vent.json", {"parent": "wildercord:block/crucible_vent"})
    g.write_json(g.ASSETS / "items/crucible_vent.json", {"model": {"type": "minecraft:model", "model": "wildercord:item/crucible_vent"}})

    for iid, img, parent in (
        ("crucible_maul", _icon_maul(), "minecraft:item/handheld"),
        ("forgeheart_censer", _icon_censer(), "minecraft:item/generated"),
    ):
        g.save(img, g.ASSETS / f"textures/item/{iid}.png")
        g.write_json(g.ASSETS / f"models/item/{iid}.json", {"parent": parent, "textures": {"layer0": f"wildercord:item/{iid}"}})
        g.write_json(g.ASSETS / f"items/{iid}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{iid}"}})

    entity_cond = (
        [{"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type": "wildercord:kharvok"}}]
        if g.profile.is_26_2
        else {"type": "minecraft:entity_properties", "entity": "this", "predicate": {"minecraft:entity_type": "wildercord:kharvok"}}
    )
    g.adv(
        "world/defeat_kharvok",
        "world/trial_ashen_crucible",
        g.item("wildercord:crucible_maul"),
        LANG["advancements.wildercord.world.defeat_kharvok.title"],
        LANG["advancements.wildercord.world.defeat_kharvok.description"],
        {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": entity_cond}},
        frame="challenge",
        xp=150,
    )
