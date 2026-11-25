"""Co-Op Trials and Vaelkhor assets: copies/normalizes Vaelkhor's Blockbench rig, textures, and animations,
and generates procedural 16x16 pixel art, block/item models, recipes, advancements, and English text.
"""
from __future__ import annotations

import json
from pathlib import Path

from PIL import Image

LANG = {
    "entity.wildercord.vaelkhor": "Vaelkhor, the Leyline Sovereign",
    "block.wildercord.trial_gate": "Leyline Trial Gate",
    "block.wildercord.trial_treasury": "Leyline Trial Treasury",
    "block.wildercord.leyline_conduit": "Leyline Conduit",
    "item.wildercord.sovereign_halberd": "Sovereign Halberd",
    "item.wildercord.sovereign_halberd.lore": "Forged from gilded bronze and leyline obsidian tempered in sovereign resonance.",
    "item.wildercord.sovereign_halberd.use": "Sweeps enemies ahead in a wide cleaving arc when swung.",
    "item.wildercord.leyline_bell_charm": "Leyline Bell Charm",
    "item.wildercord.leyline_bell_charm.lore": "A chained bronze bell-censer humming with steadied leyline chords.",
    "item.wildercord.leyline_bell_charm.use": "Right-click to chime a sanctuary pulse that shields and mends nearby allies.",
    "item.wildercord.leyline_sigil": "Leyline Trial Sigil",
    "item.wildercord.leyline_sigil.lore": "Keyed crest etched to reawaken a resting Leyline Sanctum.",
    "item.wildercord.leyline_sigil.use": "Use on a resting Trial Gate to bypass the respawn cooldown immediately.",
    "item.wildercord.trial_gate.lore": "Pedestal anchor that seals the sanctum and summons Vaelkhor for your party.",
    "item.wildercord.trial_treasury.lore": "Sanctum reliquary yielding instanced rewards for every trial contributor.",
    "item.wildercord.leyline_conduit.lore": "Resonant pylon channeled by Vaelkhor during Bell Toll Conduit.",
    "boss.wildercord.vaelkhor.status": "%s — %s",
    "boss.wildercord.vaelkhor.idle": "Channeling",
    "boss.wildercord.vaelkhor.stride": "Advancing",
    "boss.wildercord.vaelkhor.halberd_cleave": "Halberd Cleave!",
    "boss.wildercord.vaelkhor.bell_toll_conduit": "Bell Toll Conduit — Dampen the pylons!",
    "boss.wildercord.vaelkhor.leyline_cataclysm": "Leyline Cataclysm — Stand in the rings!",
    "boss.wildercord.vaelkhor.staggered": "Resonance Shattered — Staggered!",
    "message.wildercord.trial.decorative": "This Trial Gate stands outside an authentic Leyline Sanctum.",
    "message.wildercord.trial.quiet": "Trials require Survival or Adventure mode above Peaceful.",
    "message.wildercord.trial.active": "Vaelkhor is already engaging challengers in the Sanctum!",
    "message.wildercord.trial.cooldown": "The Sanctum rests for %s (or offer a Leyline Trial Sigil to awaken Vaelkhor now).",
    "message.wildercord.trial.started": "Co-Op Trial begun (%s challenger(s))! Watch Vaelkhor's halberd arcs, conduits, and cataclysm rings.",
    "message.wildercord.trial.conduit_idle": "This Leyline Conduit is dormant.",
    "message.wildercord.trial.conduit_dampened": "Conduit dampened (%s remaining)!",
    "message.wildercord.trial.conduit_broken": "All conduits dampened — Vaelkhor staggers!",
    "message.wildercord.trial.victory": "Vaelkhor defeated! Every contributor may claim the Trial Treasury.",
    "message.wildercord.trial.treasury_locked": "Defeat Vaelkhor to unseal the Trial Treasury.",
    "message.wildercord.trial.treasury_not_eligible": "Only contributors from this trial clear may claim from this Treasury.",
    "message.wildercord.trial.treasury_claimed": "You have already claimed your share from this Trial Treasury.",
    "message.wildercord.trial.treasury_awarded": "Claimed your Sovereign Trial Treasury reward!",
    "message.wildercord.bell_charm.cooldown": "The Bell Charm is still settling (%ss).",
    "message.wildercord.bell_charm.chime": "Leyline Bell Charm resonates — allies granted Regeneration & Resistance!",
    "advancements.wildercord.world.defeat_vaelkhor.title": "Leyline Sovereign Broken",
    "advancements.wildercord.world.defeat_vaelkhor.description": "Defeat Vaelkhor, the Leyline Sovereign in a Co-Op Trial Sanctum",
    "advancements.wildercord.world.claim_trial_treasury.title": "Shares of the Sanctum",
    "advancements.wildercord.world.claim_trial_treasury.description": "Claim an instanced reward from a Trial Treasury",
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
    text = json.dumps(data, separators=(",", ":"), ensure_ascii=False) + "\n"
    path.write_text(text, encoding="utf-8", newline="\n")


def _icon_halberd():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    shaft = (196, 152, 62, 255)
    shaft_d = (128, 92, 34, 255)
    obs = (28, 30, 42, 255)
    obs_h = (64, 68, 92, 255)
    cyan = (96, 245, 224, 255)
    gold = (240, 204, 98, 255)
    for i in range(1, 14):
        px[i, 15 - i] = shaft if i % 2 == 0 else shaft_d
    # Spear tip at top-right (13, 2)
    for x, y in ((13, 1), (14, 1), (14, 2), (12, 2), (13, 2)):
        px[x, y] = gold if x == 14 and y == 1 else cyan
    # Crescent axe blade left of upper shaft
    blade = [
        (8, 2), (9, 2), (10, 2),
        (7, 3), (8, 3), (9, 3), (10, 3), (11, 3),
        (7, 4), (8, 4), (9, 4), (10, 4),
        (7, 5), (8, 5), (9, 5),
        (8, 6), (9, 6),
    ]
    for bx, by in blade:
        px[bx, by] = cyan if bx == 7 else (obs_h if by == 3 else obs)
    px[10, 4] = gold
    px[11, 5] = gold
    return img


def _icon_bell_charm():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    gold = (224, 182, 76, 255)
    gold_d = (140, 102, 34, 255)
    verd = (64, 154, 134, 255)
    cyan = (102, 248, 224, 255)
    # Chain & top loop
    for y in range(1, 4):
        px[7, y] = gold if y % 2 == 1 else gold_d
        px[8, y] = gold_d
    px[6, 1] = px[9, 1] = gold
    # Bell body
    for y in range(4, 12):
        half = 2 if y < 6 else (3 if y < 10 else 4)
        for x in range(8 - half, 8 + half):
            if y in (6, 7) and 7 <= x <= 8:
                px[x, y] = cyan
            elif x == 8 - half or x == 7 + half:
                px[x, y] = gold_d
            elif y >= 10:
                px[x, y] = verd
            else:
                px[x, y] = gold
    # Clapper crystal
    px[7, 12] = px[8, 12] = cyan
    px[7, 13] = px[8, 13] = (180, 255, 244, 255)
    return img


def _icon_sigil():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    gold = (224, 184, 78, 255)
    obs = (32, 30, 48, 255)
    cyan = (96, 245, 224, 255)
    white = (225, 255, 250, 255)
    for y in range(2, 14):
        for x in range(2, 14):
            d = abs(x - 7.5) + abs(y - 7.5)
            if 5.0 <= d <= 6.8:
                px[x, y] = gold
            elif d < 5.0:
                px[x, y] = white if d < 1.6 else (cyan if d < 3.2 else obs)
    return img


def _block_tex(mode):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    px = img.load()
    obs = (28, 30, 42, 255)
    obs2 = (42, 46, 62, 255)
    gold = (212, 168, 62, 255)
    cyan = (92, 238, 218, 255)
    vio = (146, 86, 224, 255)
    for y in range(16):
        for x in range(16):
            border = x in (0, 15) or y in (0, 15)
            inner = x in (1, 14) or y in (1, 14)
            c = obs2 if ((x + y) % 5 == 0) else obs
            if mode == "gate":
                if border:
                    c = gold
                elif inner:
                    c = (64, 52, 26, 255)
                elif abs(x - 7.5) + abs(y - 7.5) <= 3.5:
                    c = cyan if (x in (7, 8) or y in (7, 8)) else vio
            elif mode == "treasury":
                if border or (y in (6, 7) and 1 <= x <= 14):
                    c = gold
                elif 6 <= x <= 9 and 5 <= y <= 9:
                    c = cyan
                elif inner:
                    c = (52, 42, 74, 255)
            elif mode == "conduit":
                if border:
                    c = (66, 134, 120, 255)
                elif x in (7, 8) or (y in (7, 8) and 3 <= x <= 12):
                    c = cyan
                elif (x + y) % 4 == 0 and inner:
                    c = gold
            px[x, y] = c
    return img


def write(g):
    src_dir = g.ROOT / "art/blockbench/vaelkhor"
    with Image.open(src_dir / "vaelkhor_diffuse.png") as diff:
        g.save(diff.convert("RGBA"), g.ASSETS / "textures/entity/trials/vaelkhor.png")
    with Image.open(src_dir / "vaelkhor_glow.png") as glow:
        g.save(glow.convert("RGBA"), g.ASSETS / "textures/entity/trials/vaelkhor_glow.png")
    g.save(Image.new("RGBA", (2, 2), (255, 255, 255, 255)), g.ASSETS / "textures/entity/trials/vaelkhor_mark.png")

    geo_data = _round_numbers(json.loads((src_dir / "vaelkhor_leyline_sovereign.geo.json").read_text(encoding="utf-8")))
    anim_data = _round_numbers(json.loads((src_dir / "vaelkhor_leyline_sovereign.animation.json").read_text(encoding="utf-8")))
    _write_compact_json(g.ASSETS / "models/entity/trials/vaelkhor.geo.json", geo_data)
    _write_compact_json(g.ASSETS / "animations/entity/trials/vaelkhor.animation.json", anim_data)

    # Blocks: trial_gate, trial_treasury, leyline_conduit
    block_modes = {
        "trial_gate": "gate",
        "trial_treasury": "treasury",
        "leyline_conduit": "conduit",
    }
    for bid, mode in block_modes.items():
        g.save(_block_tex(mode), g.ASSETS / f"textures/block/{bid}.png")
        g.write_json(g.ASSETS / f"blockstates/{bid}.json", {"variants": {"": {"model": f"wildercord:block/{bid}"}}})
        g.write_json(g.ASSETS / f"models/block/{bid}.json", {
            "parent": "minecraft:block/cube_all",
            "textures": {"all": f"wildercord:block/{bid}"},
        })
        g.write_json(g.ASSETS / f"models/item/{bid}.json", {"parent": f"wildercord:block/{bid}"})
        g.write_json(g.ASSETS / f"items/{bid}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{bid}"}})

    # Items
    icons = {
        "sovereign_halberd": (_icon_halberd(), "minecraft:item/handheld"),
        "leyline_bell_charm": (_icon_bell_charm(), "minecraft:item/generated"),
        "leyline_sigil": (_icon_sigil(), "minecraft:item/generated"),
    }
    for iid, (img, parent) in icons.items():
        g.save(img, g.ASSETS / f"textures/item/{iid}.png")
        g.write_json(g.ASSETS / f"models/item/{iid}.json", {"parent": parent, "textures": {"layer0": f"wildercord:item/{iid}"}})
        g.write_json(g.ASSETS / f"items/{iid}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{iid}"}})

    # Craftable reset sigil
    g.write_json(g.DATA / "recipe/leyline_sigil.json", {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": [" A ", "GCG", " E "],
        "key": {
            "A": "minecraft:amethyst_shard",
            "G": "minecraft:gold_ingot",
            "C": "wildercord:mana_crystal",
            "E": "minecraft:echo_shard",
        },
        "result": {"id": "wildercord:leyline_sigil"},
    })
    g.unlock_advancement("wildercord:leyline_sigil", "wildercord:mana_crystal")

    def entity_cond(entity_id):
        if g.profile.is_26_2:
            return [{"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type": entity_id}}]
        return {"type": "minecraft:entity_properties", "entity": "this", "predicate": {"minecraft:entity_type": entity_id}}

    g.adv(
        "world/defeat_vaelkhor",
        "world/trial_leyline_sanctum",
        g.item("wildercord:sovereign_halberd"),
        LANG["advancements.wildercord.world.defeat_vaelkhor.title"],
        LANG["advancements.wildercord.world.defeat_vaelkhor.description"],
        {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": entity_cond("wildercord:vaelkhor")}},
        frame="challenge",
        xp=150,
    )
    g.adv(
        "world/claim_trial_treasury",
        "world/defeat_vaelkhor",
        g.item("wildercord:leyline_bell_charm"),
        LANG["advancements.wildercord.world.claim_trial_treasury.title"],
        LANG["advancements.wildercord.world.claim_trial_treasury.description"],
        {
            "halberd": g.has_item("wildercord:sovereign_halberd"),
            "bell_charm": g.has_item("wildercord:leyline_bell_charm"),
        },
        any_of=True,
        frame="goal",
        xp=50,
    )
