"""Co-Op Trials and Vaelkhor assets: copies/normalizes Vaelkhor's Blockbench rig, textures, and animations,
and generates procedural 16x16 pixel art, Sovereign armor equipment layers, block/item models,
recipes, advancements, and English text.
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
    "item.wildercord.sovereign_halberd.use": "Sweeps enemies ahead in a wide cleaving arc when swung (empowered when wearing Sovereign's Mantle).",
    "item.wildercord.leyline_bell_charm": "Leyline Bell Charm",
    "item.wildercord.leyline_bell_charm.lore": "A chained bronze bell-censer humming with steadied leyline chords.",
    "item.wildercord.leyline_bell_charm.use": "Right-click to chime a cleansing sanctuary pulse that heals and fortifies nearby allies.",
    "item.wildercord.leyline_sigil": "Leyline Trial Sigil",
    "item.wildercord.leyline_sigil.lore": "Keyed crest etched to reawaken a resting Leyline Sanctum.",
    "item.wildercord.leyline_sigil.use": "Use on a resting Trial Gate to bypass the respawn cooldown immediately.",
    "item.wildercord.leyline_seal": "Leyline Trial Seal",
    "item.wildercord.leyline_seal.lore": "Triumph crest torn from Vaelkhor's Sanctum, humming with violet-cyan resonance.",
    "item.wildercord.leyline_seal.use": "Combine with the Crucible and Eclipse Seals to forge Sovereign's Mantle, or sneak-offer at a Trial Gate for Apex Mode.",
    "item.wildercord.crucible_seal": "Crucible Trial Seal",
    "item.wildercord.crucible_seal.lore": "Molten bronze stamp tempered inside Kharvok's Ashen Crucible.",
    "item.wildercord.crucible_seal.use": "Combine with the Leyline and Eclipse Seals to forge Sovereign's Mantle, or sneak-offer at a Trial Gate for Apex Mode.",
    "item.wildercord.eclipse_seal": "Eclipse Trial Seal",
    "item.wildercord.eclipse_seal.lore": "Starlit lunar crest claimed from Nyxara's Astral Eclipse Vault.",
    "item.wildercord.eclipse_seal.use": "Combine with the Leyline and Crucible Seals to forge Sovereign's Mantle, or sneak-offer at a Trial Gate for Apex Mode.",
    "item.wildercord.sovereigns_mantle": "Sovereign's Mantle",
    "item.wildercord.sovereigns_mantle.lore": "Tri-Seal regalia woven from Leyline, Crucible, and Eclipse authority.",
    "item.wildercord.sovereigns_mantle.use": "While worn: projects a 12-block party Resistance & Regeneration aura. Shift-use or hold sneak with Trial relics to unleash Sovereign's Convergence, or challenge Trials in Apex Mode.",
    "item.wildercord.sovereign_crown": "Sovereign Crown",
    "item.wildercord.sovereign_crown.lore": "Gilded obsidian diadem awarded only to challengers who conquer an Apex Co-Op Trial.",
    "item.wildercord.sovereign_crown.use": "Wear to bear the crest of an Apex Sovereign.",
    "item.wildercord.trial_gate.lore": "Pedestal anchor that seals the sanctum and summons its Sovereign guardian for your party.",
    "item.wildercord.trial_treasury.lore": "Sanctum reliquary yielding instanced rewards for every trial contributor.",
    "item.wildercord.leyline_conduit.lore": "Resonant pylon channeled by Vaelkhor during Bell Toll Conduit.",
    "boss.wildercord.vaelkhor.status": "%s — %s",
    "boss.wildercord.vaelkhor.idle": "Channeling",
    "boss.wildercord.vaelkhor.stride": "Advancing",
    "boss.wildercord.vaelkhor.halberd_cleave": "Halberd Cleave!",
    "boss.wildercord.vaelkhor.bell_toll_conduit": "Bell Toll Conduit — Dampen the pylons!",
    "boss.wildercord.vaelkhor.leyline_cataclysm": "Leyline Cataclysm — Stand in the rings!",
    "boss.wildercord.vaelkhor.staggered": "Resonance Shattered — Staggered!",
    "message.wildercord.trial.decorative": "This Trial Gate stands outside an authentic Trial Sanctum.",
    "message.wildercord.trial.quiet": "Trials require Survival or Adventure mode above Peaceful.",
    "message.wildercord.trial.active": "A Sovereign Guardian is already engaging challengers in this Sanctum!",
    "message.wildercord.trial.cooldown": "The Sanctum rests for %s (or offer a Leyline Trial Sigil to awaken it now).",
    "message.wildercord.trial.started": "Co-Op Trial begun (%s challenger(s))! Watch the Sovereign's telegraphs and interactive objectives.",
    "message.wildercord.trial.started_apex": "APEX Co-Op Trial begun (%s challenger(s))! Sovereign's Mantle / Seal amplified the encounter!",
    "message.wildercord.trial.conduit_idle": "This Leyline Conduit is dormant.",
    "message.wildercord.trial.conduit_dampened": "Conduit dampened (%s remaining)!",
    "message.wildercord.trial.conduit_broken": "All conduits dampened — Vaelkhor staggers!",
    "message.wildercord.trial.victory": "Trial Sovereign defeated! Every contributor may claim the Trial Treasury.",
    "message.wildercord.trial.victory_apex": "APEX Trial Sovereign defeated! Sovereign Crown & empowered rewards unlocked in the Treasury!",
    "message.wildercord.trial.treasury_locked": "Defeat the Trial Sovereign to unseal the Trial Treasury.",
    "message.wildercord.trial.treasury_not_eligible": "Only contributors from this trial clear may claim from this Treasury.",
    "message.wildercord.trial.treasury_claimed": "You have already claimed your share from this Trial Treasury.",
    "message.wildercord.trial.treasury_awarded": "Claimed your Sovereign Trial Treasury reward!",
    "message.wildercord.bell_charm.cooldown": "The Bell Charm is still settling (%ss).",
    "message.wildercord.bell_charm.chime": "Leyline Bell Charm resonates — allies cleansed and granted Regeneration & Resistance!",
    "message.wildercord.mantle.cooldown": "Sovereign's Convergence is recharging.",
    "message.wildercord.mantle.convergence": "Sovereign's Convergence! Enemies staggered and allies fortified.",
    "command.wildercord.sanctum.placed": "Placed %s Trial Sanctum near (%s, %s, %s).",
    "command.wildercord.sanctum.unknown": "Unknown Trial Sanctum kind '%s' (expected leyline, crucible, or eclipse).",
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
    for x, y in ((13, 1), (14, 1), (14, 2), (12, 2), (13, 2)):
        px[x, y] = gold if x == 14 and y == 1 else cyan
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
    for y in range(1, 4):
        px[7, y] = gold if y % 2 == 1 else gold_d
        px[8, y] = gold_d
    px[6, 1] = px[9, 1] = gold
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


def _icon_seal(rim_col, mid_col, core_col):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    obs = (26, 26, 38, 255)
    for y in range(1, 15):
        for x in range(1, 15):
            r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 5.4 <= r <= 6.8:
                px[x, y] = rim_col
            elif 3.6 <= r < 5.4:
                px[x, y] = mid_col if (x + y) % 2 == 0 else obs
            elif r < 3.6:
                px[x, y] = core_col if r < 2.1 else mid_col
    for k in (2, 13):
        px[7, k] = px[8, k] = px[k, 7] = px[k, 8] = core_col
    return img


def _icon_mantle():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    gold = (224, 182, 74, 255)
    gold_d = (146, 108, 36, 255)
    obs = (28, 30, 44, 255)
    obs_l = (52, 56, 78, 255)
    cyan = (96, 245, 224, 255)
    ember = (248, 148, 56, 255)
    vio = (176, 108, 246, 255)
    for y in range(2, 15):
        for x in range(2, 14):
            if y == 2 and (5 <= x <= 10):
                continue
            if y <= 5 and (x in (2, 13)):
                px[x, y] = gold
            elif y == 3 or (y == 4 and x in (4, 11)):
                px[x, y] = gold if y == 3 else obs_l
            elif y >= 6 and x in (2, 13):
                continue
            else:
                px[x, y] = obs_l if (x + y) % 3 == 0 else obs
    # Trim & Tri-Seal chestplate crest
    for x in range(4, 12):
        px[x, 14] = gold_d
    px[7, 6] = px[8, 6] = gold
    px[6, 7] = px[9, 7] = cyan
    px[7, 7] = ember
    px[8, 7] = vio
    px[7, 8] = px[8, 8] = cyan
    return img


def _icon_crown():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    gold = (232, 192, 82, 255)
    gold_d = (150, 110, 38, 255)
    obs = (32, 32, 46, 255)
    cyan = (96, 245, 224, 255)
    ember = (248, 148, 56, 255)
    vio = (176, 108, 246, 255)
    for x in range(2, 14):
        px[x, 11] = gold_d
        px[x, 10] = gold
        px[x, 9] = obs if x % 2 == 0 else gold
    for sx, h, gem in ((3, 6, ember), (7, 4, cyan), (8, 4, cyan), (12, 6, vio)):
        for y in range(h, 10):
            px[sx, y] = gold
        px[sx, h] = gem
    px[5, 7] = px[10, 7] = gold
    px[5, 8] = px[10, 8] = gold_d
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


def _armor_box(u, v, w, h, d):
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + d + w + d, v + d, w, h),
    }


def _write_sovereign_equipment(g):
    g.write_json(g.ASSETS / "equipment/sovereign.json", {
        "layers": {
            layer: [{"texture": "wildercord:sovereign"}]
            for layer in ("humanoid", "humanoid_leggings")
        }
    })
    humanoid = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = humanoid.load()
    gold = (224, 182, 74, 255)
    gold_d = (146, 108, 36, 255)
    obs = (30, 32, 48, 255)
    obs_h = (54, 58, 82, 255)
    cyan = (96, 245, 224, 255)
    for part, (u, v, w, h, d) in (
        ("helm", (0, 0, 8, 8, 8)),
        ("chest", (16, 16, 8, 12, 4)),
        ("arms", (40, 16, 4, 12, 4)),
        ("boots", (0, 16, 4, 12, 4)),
    ):
        for face, (x0, y0, fw, fh) in _armor_box(u, v, w, h, d).items():
            for ly in range(fh):
                for lx in range(fw):
                    if part == "helm":
                        # Crown band + frontal spikes leave face visible
                        if face == "front" and ly >= 3:
                            continue
                        col = gold if ly in (0, 2) else (cyan if (face == "front" and lx in (3, 4) and ly == 1) else obs)
                    elif part == "chest":
                        if face == "front" and 3 <= lx <= 4 and 3 <= ly <= 6:
                            col = cyan
                        elif ly == 0 or ly == fh - 1 or (face not in ("top", "bottom") and ly == 8):
                            col = gold
                        else:
                            col = obs_h if (lx + ly) % 3 == 0 else obs
                    elif part == "arms":
                        col = gold if ly in (0, 1, fh - 2) else (cyan if ly == 4 and face == "front" else obs)
                    else:
                        col = gold_d if ly >= fh - 2 else obs
                    px[x0 + lx, y0 + ly] = col
    g.save(humanoid, g.ASSETS / "textures/entity/equipment/humanoid/sovereign.png")

    legs = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px_l = legs.load()
    for _, (x0, y0, fw, fh) in {**_armor_box(0, 16, 4, 12, 4), **_armor_box(16, 16, 8, 12, 4)}.items():
        for ly in range(fh):
            for lx in range(fw):
                px_l[x0 + lx, y0 + ly] = gold_d if ly <= 1 else obs
    g.save(legs, g.ASSETS / "textures/entity/equipment/humanoid_leggings/sovereign.png")


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

    icons = {
        "sovereign_halberd": (_icon_halberd(), "minecraft:item/handheld"),
        "leyline_bell_charm": (_icon_bell_charm(), "minecraft:item/generated"),
        "leyline_sigil": (_icon_sigil(), "minecraft:item/generated"),
        "leyline_seal": (_icon_seal((224, 184, 78, 255), (146, 86, 224, 255), (96, 245, 224, 255)), "minecraft:item/generated"),
        "crucible_seal": (_icon_seal((216, 152, 58, 255), (186, 68, 36, 255), (255, 208, 92, 255)), "minecraft:item/generated"),
        "eclipse_seal": (_icon_seal((218, 204, 132, 255), (92, 74, 168, 255), (234, 198, 255, 255)), "minecraft:item/generated"),
        "sovereigns_mantle": (_icon_mantle(), "minecraft:item/generated"),
        "sovereign_crown": (_icon_crown(), "minecraft:item/generated"),
    }
    for iid, (img, parent) in icons.items():
        g.save(img, g.ASSETS / f"textures/item/{iid}.png")
        g.write_json(g.ASSETS / f"models/item/{iid}.json", {"parent": parent, "textures": {"layer0": f"wildercord:item/{iid}"}})
        g.write_json(g.ASSETS / f"items/{iid}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{iid}"}})

    _write_sovereign_equipment(g)

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

    g.write_json(g.DATA / "recipe/sovereigns_mantle.json", {
        "type": "minecraft:crafting_shaped",
        "category": "equipment",
        "pattern": ["LCE", "SNS", " M "],
        "key": {
            "L": "wildercord:leyline_seal",
            "C": "wildercord:crucible_seal",
            "E": "wildercord:eclipse_seal",
            "S": "minecraft:echo_shard",
            "N": "minecraft:netherite_chestplate",
            "M": "wildercord:mana_crystal",
        },
        "result": {"id": "wildercord:sovereigns_mantle"},
    })
    g.unlock_advancement("wildercord:sovereigns_mantle", "wildercord:leyline_seal")

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
