"""Trial III (Astral Eclipse Vault & Nyxara) assets: textures, Bedrock rig/anim, relics, advancements, and English text."""
from __future__ import annotations

import json
from pathlib import Path

from PIL import Image

LANG = {
    "entity.wildercord.nyxara": "Nyxara, the Eclipse Sovereign",
    "block.wildercord.astral_prism": "Astral Alignment Prism",
    "item.wildercord.astral_prism.lore": "Crystalline conduit attuned during Nyxara's Prism Convergence.",
    "item.wildercord.eclipse_twinblade": "Eclipse Twinblade",
    "item.wildercord.eclipse_twinblade.lore": "Twin-crescent void steel etched with starlit silver script.",
    "item.wildercord.eclipse_twinblade.use": "Right-click to dash forward in a sweeping crescent slash.",
    "item.wildercord.astrolabe_dial": "Astrolabe Dial",
    "item.wildercord.astrolabe_dial.lore": "Spinning gilded star-compass calibrated to Sovereign leylines.",
    "item.wildercord.astrolabe_dial.use": "Right-click to grant Swiftness II, Night Vision, Absorption, and healing to nearby allies.",
    "boss.wildercord.nyxara.status": "%s — %s",
    "boss.wildercord.nyxara.idle": "Observing the Orbit",
    "boss.wildercord.nyxara.stride": "Gliding",
    "boss.wildercord.nyxara.crescent_sever": "Crescent Sever!",
    "boss.wildercord.nyxara.prism_convergence": "Prism Convergence — Align the prisms!",
    "boss.wildercord.nyxara.total_eclipse": "Total Eclipse — Stand in the rings!",
    "boss.wildercord.nyxara.staggered": "Orbit Shattered — Staggered!",
    "message.wildercord.trial.prism_idle": "This Astral Prism is dormant.",
    "message.wildercord.trial.prism_aligned": "Astral Prism aligned (%s remaining)!",
    "message.wildercord.trial.prism_broken": "All Astral Prisms aligned — Nyxara staggers!",
    "message.wildercord.astrolabe_dial.cooldown": "Astrolabe Dial is recalibrating (%ss).",
    "message.wildercord.astrolabe_dial.pulse": "Astrolabe Dial chimes — allies hastened and shielded!",
    "advancements.wildercord.world.defeat_nyxara.title": "Eclipse Sovereign Eclipsed",
    "advancements.wildercord.world.defeat_nyxara.description": "Defeat Nyxara, the Eclipse Sovereign in the Astral Eclipse Vault",
    "advancements.wildercord.world.sovereigns_mantle.title": "Tri-Seal Sovereign",
    "advancements.wildercord.world.sovereigns_mantle.description": "Forge Sovereign's Mantle from the Leyline, Crucible, and Eclipse Trial Seals",
    "advancements.wildercord.world.apex_sovereign.title": "Apex Sovereign",
    "advancements.wildercord.world.apex_sovereign.description": "Conquer an Empowered Apex Co-Op Trial and claim the Sovereign Crown",
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


def _icon_twinblade():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    gold = (224, 184, 82, 255)
    void = (28, 24, 48, 255)
    silver = (206, 214, 242, 255)
    vio = (182, 114, 255, 255)
    for i in range(4, 12):
        px[i, 15 - i] = gold
    for x, y in ((11, 2), (12, 2), (13, 3), (13, 4), (12, 5), (11, 3), (12, 4)):
        px[x, y] = silver if x == 13 or y == 2 else (vio if x == 12 else void)
    for x, y in ((2, 11), (2, 12), (3, 13), (4, 13), (5, 12), (3, 11), (4, 12)):
        px[x, y] = silver if y == 13 or x == 2 else (vio if y == 12 else void)
    return img


def _icon_astrolabe():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    gold = (226, 184, 78, 255)
    indigo = (66, 52, 128, 255)
    vio = (186, 124, 255, 255)
    cyan = (156, 238, 255, 255)
    for y in range(2, 14):
        for x in range(2, 14):
            r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 4.6 <= r <= 6.2:
                px[x, y] = gold
            elif 2.4 <= r < 4.6:
                px[x, y] = indigo if (x + y) % 2 == 0 else vio
            elif r < 2.4:
                px[x, y] = cyan if r < 1.3 else vio
    px[7, 1] = px[8, 1] = gold
    return img


def _tex_astral_prism():
    img = Image.new("RGBA", (16, 16), (26, 24, 42, 255))
    px = img.load()
    gold = (218, 178, 78, 255)
    silver = (196, 206, 234, 255)
    vio = (184, 116, 255, 255)
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                px[x, y] = gold
            elif abs(x - 7.5) + abs(y - 7.5) <= 4.2:
                px[x, y] = (232, 240, 255, 255) if (x in (7, 8) and y in (7, 8)) else vio
            elif (x + y) % 4 == 0:
                px[x, y] = silver
    return img


def write(g):
    src_dir = g.ROOT / "art/blockbench/nyxara"
    with Image.open(src_dir / "nyxara_diffuse.png") as diff:
        g.save(diff.convert("RGBA"), g.ASSETS / "textures/entity/trials/nyxara.png")
    with Image.open(src_dir / "nyxara_glow.png") as glow:
        g.save(glow.convert("RGBA"), g.ASSETS / "textures/entity/trials/nyxara_glow.png")

    geo_data = _round_numbers(json.loads((src_dir / "nyxara_eclipse_sovereign.geo.json").read_text(encoding="utf-8")))
    anim_data = _round_numbers(json.loads((src_dir / "nyxara_eclipse_sovereign.animation.json").read_text(encoding="utf-8")))
    _write_compact_json(g.ASSETS / "models/entity/trials/nyxara.geo.json", geo_data)
    _write_compact_json(g.ASSETS / "animations/entity/trials/nyxara.animation.json", anim_data)

    g.save(_tex_astral_prism(), g.ASSETS / "textures/block/astral_prism.png")
    g.write_json(g.ASSETS / "blockstates/astral_prism.json", {"variants": {"": {"model": "wildercord:block/astral_prism"}}})
    g.write_json(g.ASSETS / "models/block/astral_prism.json", {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": "wildercord:block/astral_prism"},
    })
    g.write_json(g.ASSETS / "models/item/astral_prism.json", {"parent": "wildercord:block/astral_prism"})
    g.write_json(g.ASSETS / "items/astral_prism.json", {"model": {"type": "minecraft:model", "model": "wildercord:item/astral_prism"}})

    for iid, img, parent in (
        ("eclipse_twinblade", _icon_twinblade(), "minecraft:item/handheld"),
        ("astrolabe_dial", _icon_astrolabe(), "minecraft:item/generated"),
    ):
        g.save(img, g.ASSETS / f"textures/item/{iid}.png")
        g.write_json(g.ASSETS / f"models/item/{iid}.json", {"parent": parent, "textures": {"layer0": f"wildercord:item/{iid}"}})
        g.write_json(g.ASSETS / f"items/{iid}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{iid}"}})

    entity_cond = (
        [{"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type": "wildercord:nyxara"}}]
        if g.profile.is_26_2
        else {"type": "minecraft:entity_properties", "entity": "this", "predicate": {"minecraft:entity_type": "wildercord:nyxara"}}
    )
    g.adv(
        "world/defeat_nyxara",
        "world/trial_astral_eclipse",
        g.item("wildercord:eclipse_twinblade"),
        LANG["advancements.wildercord.world.defeat_nyxara.title"],
        LANG["advancements.wildercord.world.defeat_nyxara.description"],
        {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": entity_cond}},
        frame="challenge",
        xp=150,
    )
    g.adv(
        "world/sovereigns_mantle",
        "world/claim_trial_treasury",
        g.item("wildercord:sovereigns_mantle"),
        LANG["advancements.wildercord.world.sovereigns_mantle.title"],
        LANG["advancements.wildercord.world.sovereigns_mantle.description"],
        g.has_item("wildercord:sovereigns_mantle"),
        frame="challenge",
        xp=200,
    )
    g.adv(
        "world/apex_sovereign",
        "world/sovereigns_mantle",
        g.item("wildercord:sovereign_crown"),
        LANG["advancements.wildercord.world.apex_sovereign.title"],
        LANG["advancements.wildercord.world.apex_sovereign.description"],
        g.has_item("wildercord:sovereign_crown"),
        frame="challenge",
        xp=250,
    )
