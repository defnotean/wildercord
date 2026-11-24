#!/usr/bin/env python3
"""
Generate Bedrock Edition Resource Pack (.mcpack), combined Add-on (.mcaddon),
and Java <-> Bedrock Geyser translator mappings directly from Wildercord assets.

Usage:
    python tools/bedrock_export.py [--no-sounds] [--output-dir artifacts/bedrock]
"""

from __future__ import annotations

import argparse
import hashlib
import io
import json
import pathlib
import re
import shutil
import uuid
import zipfile
from typing import Any

from PIL import Image

MOD_ID = "wildercord"
UUID_NS = uuid.NAMESPACE_DNS
IDENT_RE = re.compile(r"^[a-z0-9_.:/-]+$")
NON_SOLID_BLOCK_KEYWORDS = (
    "fern", "bloom", "moss", "reed", "glowcap", "portal", "eddy",
    "water", "scar", "glyph", "ash", "sand", "everfrost", "fulgurite",
    "breathmark", "drainhouse_mark", "weavers_silk", "lantern", "star",
)

NOTE_INSTRUMENTS = [
    "harp", "basedrum", "snare", "hat", "bass", "flute", "bell", "guitar",
    "chime", "xylophone", "iron_xylophone", "cow_bell", "didgeridoo", "bit",
    "banjo", "pling",
]


def repo_root() -> pathlib.Path:
    return pathlib.Path(__file__).resolve().parent.parent


def parse_version(root: pathlib.Path) -> list[int]:
    props = (root / "gradle.properties").read_text(encoding="utf-8")
    m = re.search(r"^mod_version\s*=\s*(.+)$", props, re.MULTILINE)
    raw = m.group(1).strip() if m else "0.16.0"
    nums = [int(x) for x in re.findall(r"\d+", raw.split("-")[0])[:3]]
    while len(nums) < 3:
        nums.append(0)
    return nums


def det_uuid(name: str) -> str:
    return str(uuid.uuid5(UUID_NS, name))


def sanitize_id(raw: str) -> str:
    clean = re.sub(r"[^a-z0-9_]", "_", raw.lower())
    return re.sub(r"_+", "_", clean).strip("_")


def read_json(path: pathlib.Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def write_json_bytes(data: Any) -> bytes:
    return (json.dumps(data, indent=2, ensure_ascii=False) + "\n").encode("utf-8")


def normalize_png_for_bedrock(src: pathlib.Path, crop_animated: bool) -> bytes:
    """
    Bedrock item icons must be square. Java animated sprite sheets have tall Nx(N*k)
    dimensions paired with .png.mcmeta files; crop frame 0 when needed so Bedrock
    never renders a squished strip.
    """
    raw = src.read_bytes()
    if not crop_animated:
        return raw
    mcmeta = pathlib.Path(str(src) + ".mcmeta")
    with Image.open(io.BytesIO(raw)) as img:
        w, h = img.size
        if h > w and (mcmeta.exists() or h % w == 0):
            frame = img.crop((0, 0, w, w))
            buf = io.BytesIO()
            frame.save(buf, format="PNG")
            return buf.getvalue()
    return raw


def guess_item_carrier(item_id: str) -> tuple[str, bool, str]:
    """Return (vanilla_java_carrier_item, handheld, creative_category)."""
    k = item_id.lower()
    if k.startswith("master_blade/"):
        return ("minecraft:netherite_sword", True, "EQUIPMENT")
    if k in ("three_bows", "keeper_testament", "last_oath") or k.endswith("_lesson") or k.startswith("battlefield_"):
        return ("minecraft:written_book", False, "ITEMS")
    if k.endswith("_helmet") or k.endswith("_helm") or k.endswith("_hat") or k.endswith("_hood") or k.endswith("_crown"):
        return ("minecraft:iron_helmet", False, "EQUIPMENT")
    if k.endswith("_chestplate") or k.endswith("_mantle") or k.endswith("_tunic") or k.endswith("_coat") or k.endswith("_robe"):
        return ("minecraft:iron_chestplate", False, "EQUIPMENT")
    if k.endswith("_leggings") or k.endswith("_greaves"):
        return ("minecraft:iron_leggings", False, "EQUIPMENT")
    if k.endswith("_boots") or k.endswith("_shoes"):
        return ("minecraft:iron_boots", False, "EQUIPMENT")
    if any(w in k for w in ("sword", "blade", "glaive", "katana", "spear", "staff", "dagger", "halberd", "scythe", "wand", "hammer")):
        return ("minecraft:iron_sword", True, "EQUIPMENT")
    if "bow" in k and "ribbon" not in k and "cinnamon_bow" not in k:
        return ("minecraft:bow", True, "EQUIPMENT")
    if "shield" in k or "buckler" in k or "aegis" in k:
        return ("minecraft:shield", True, "EQUIPMENT")
    if "pickaxe" in k:
        return ("minecraft:iron_pickaxe", True, "EQUIPMENT")
    if "axe" in k and "pickaxe" not in k:
        return ("minecraft:iron_axe", True, "EQUIPMENT")
    if any(w in k for w in ("meal", "stew", "soup", "bread", "feast", "pie", "tea", "cake", "ration", "broth", "dumpling", "saffron", "berries")):
        return ("minecraft:cooked_beef", False, "ITEMS")
    if k.endswith("_spawn_egg"):
        return ("minecraft:pig_spawn_egg", False, "NATURE")
    if k.endswith("_cord"):
        return ("minecraft:blaze_rod", True, "EQUIPMENT")
    if "backpack" in k:
        return ("minecraft:leather", False, "EQUIPMENT")
    if "scroll" in k or "tome" in k or "page" in k or "map" in k:
        return ("minecraft:paper", False, "ITEMS")
    return ("minecraft:paper", False, "ITEMS")


def guess_block_fallback(block_id: str) -> tuple[str, bool, int]:
    """Return (vanilla_fallback_block, solid, light_emission)."""
    k = block_id.lower()
    if "water" in k:
        return ("minecraft:water", False, 0)
    if "portal" in k:
        return ("minecraft:end_gateway", False, 15)
    if "lantern" in k or "marshlight" in k:
        return ("minecraft:soul_lantern", False, 12)
    if "fern" in k or "reed" in k or "bloom" in k:
        return ("minecraft:short_grass", False, 3)
    if "glowcap" in k:
        return ("minecraft:brown_mushroom", False, 10)
    if any(w in k for w in ("moss", "everfrost", "fulgurite", "scar", "eddy", "glyph", "ash", "sand", "breathmark", "drainhouse_mark", "weavers_silk")):
        return ("minecraft:glow_lichen", False, 4)
    if "lectern" in k or "desk" in k:
        return ("minecraft:lectern", True, 4)
    if "anvil" in k:
        return ("minecraft:anvil", True, 0)
    if "pot" in k or "altar" in k or "hearth" in k or "wellstone" in k:
        return ("minecraft:amethyst_block", True, 7)
    if "board" in k or "refuge" in k or "standard" in k or "crossing_bell" in k:
        return ("minecraft:oak_planks", True, 0)
    if "cinder" in k or "kiln" in k:
        return ("minecraft:magma_block", True, 7)
    if "rime" in k or "frost" in k:
        return ("minecraft:packed_ice", True, 2)
    if "root" in k or "coil" in k:
        return ("minecraft:mangrove_roots", True, 0)
    if "star" in k or "astrolabe" in k or "bell" in k:
        return ("minecraft:gilded_blackstone", True, 8)
    return ("minecraft:polished_deepslate", True, 0)


def guess_entity_carrier(entity_id: str) -> str:
    k = entity_id.lower()
    mapping = [
        (("cat", "cinnamon"), "minecraft:cat"),
        (("fox",), "minecraft:fox"),
        (("bobcat", "lynx", "jaguar", "cougar", "panther"), "minecraft:ocelot"),
        (("wolf", "pup", "hound", "hyena"), "minecraft:wolf"),
        (("whale",), "minecraft:dolphin"),
        (("mako", "shark", "leech", "eel", "jelly"), "minecraft:guardian"),
        (("nautilus", "dredger", "barnacle", "sentinel", "hulk"), "minecraft:drowned"),
        (("seahorse", "ray", "skyray"), "minecraft:allay"),
        (("duck", "puffin", "owlet", "owl", "bird", "hawk"), "minecraft:parrot"),
        (("rabbit", "hare"), "minecraft:rabbit"),
        (("tortoise", "turtle", "snail", "sporeback"), "minecraft:turtle"),
        (("frog", "toad"), "minecraft:frog"),
        (("panda",), "minecraft:panda"),
        (("stag", "deer", "elk", "ram", "steed", "horse"), "minecraft:horse"),
        (("golem", "warden", "titan", "juggernaut", "bulwark"), "minecraft:iron_golem"),
        (("archivist", "evoker", "lich", "sorcerer", "mage", "witch"), "minecraft:evoker"),
        (("knight", "bailiff", "captain", "master", "duelist", "swordsman", "champion"), "minecraft:vindicator"),
        (("villager", "merchant", "cartographer", "ferryman", "smith", "mentor", "keeper"), "minecraft:villager"),
        (("mimic",), "minecraft:shulker"),
        (("stoat", "otter", "ferret", "meerkat", "tanuki", "pika", "dormouse", "mole"), "minecraft:fox"),
        (("hedgehog", "porcupine", "armadillo"), "minecraft:armadillo"),
        (("penguin",), "minecraft:chicken"),
        (("capybara",), "minecraft:pig"),
        (("bolt", "projectile", "orb", "slash", "beam"), "minecraft:snowball"),
        (("dummy",), "minecraft:armor_stand"),
    ]
    for keys, target in mapping:
        if any(w in k for w in keys):
            return target
    return "minecraft:zombie"


def note_block_state_for_index(idx: int) -> str:
    instrument = NOTE_INSTRUMENTS[(idx // 50) % len(NOTE_INSTRUMENTS)]
    note = (idx // 2) % 25
    powered = "true" if (idx % 2) == 1 else "false"
    return f"minecraft:note_block[instrument={instrument},note={note},powered={powered}]"


def resolve_item_texture_relpath(assets: pathlib.Path, item_id: str) -> str | None:
    """Resolve Java assets/items/<id>.json -> texture file relative to assets/wildercord/textures/."""
    item_file = assets / "items" / f"{item_id}.json"
    candidates: list[str] = []
    if item_file.exists():
        data = read_json(item_file)
        m = data.get("model", {})
        if m.get("type") == "minecraft:model":
            ref = m.get("model", "")
            if ref.startswith(f"{MOD_ID}:"):
                candidates.append(ref.split(":", 1)[1])
    candidates.append(f"item/{item_id}")
    candidates.append(f"block/{item_id}")

    for model_rel in candidates:
        mp = assets / "models" / f"{model_rel}.json"
        if mp.exists():
            mj = read_json(mp)
            textures = mj.get("textures", {})
            for key in ("layer0", "all", "side", "front", "top", "particle"):
                val = textures.get(key)
                if isinstance(val, str) and val.startswith(f"{MOD_ID}:"):
                    tex_rel = val.split(":", 1)[1]
                    if (assets / "textures" / f"{tex_rel}.png").exists():
                        return tex_rel
            for val in textures.values():
                if isinstance(val, str) and val.startswith(f"{MOD_ID}:"):
                    tex_rel = val.split(":", 1)[1]
                    if (assets / "textures" / f"{tex_rel}.png").exists():
                        return tex_rel
    for prefix in ("item/", "block/"):
        if (assets / "textures" / f"{prefix}{item_id}.png").exists():
            return f"{prefix}{item_id}"
    return None


def resolve_block_textures(assets: pathlib.Path, block_id: str) -> dict[str, str]:
    """Find side/top/bottom/up/down/north/south/east/west textures for a blockstate."""
    bs_path = assets / "blockstates" / f"{block_id}.json"
    model_refs: list[str] = []
    if bs_path.exists():
        bs = read_json(bs_path)
        for v in (bs.get("variants") or {}).values():
            entry = v[0] if isinstance(v, list) else v
            if isinstance(entry, dict) and entry.get("model", "").startswith(f"{MOD_ID}:"):
                model_refs.append(entry["model"].split(":", 1)[1])
    model_refs.append(f"block/{block_id}")

    resolved: dict[str, str] = {}
    for mrel in model_refs:
        mp = assets / "models" / f"{mrel}.json"
        if not mp.exists():
            continue
        mj = read_json(mp)
        for k, v in (mj.get("textures") or {}).items():
            if k in ("particle",) and len(mj.get("textures", {})) > 1:
                continue
            if isinstance(v, str) and v.startswith(f"{MOD_ID}:"):
                trel = v.split(":", 1)[1]
                if (assets / "textures" / f"{trel}.png").exists():
                    resolved[k] = trel
        if resolved:
            break
    if not resolved:
        for cand in (f"block/{block_id}", f"block/{block_id}_side", f"block/{block_id}_top", f"block/residue/{block_id}"):
            if (assets / "textures" / f"{cand}.png").exists():
                resolved["all"] = cand
                break
    return resolved


def scan_lang_entities(lang: dict[str, str]) -> dict[str, str]:
    out: dict[str, str] = {}
    prefix = f"entity.{MOD_ID}."
    for k, v in lang.items():
        if k.startswith(prefix):
            rest = k[len(prefix):]
            if "." not in rest:
                out[rest] = v
    return out


def scan_code_ids(root: pathlib.Path) -> tuple[set[str], set[str], set[str]]:
    """Scan Java files to catch IDs that might not have generated JSONs."""
    java_root = root / "src" / "main" / "java" / "dev" / MOD_ID
    items, blocks, entities = set(), set(), set()
    id_pat = re.compile(r'(?:Wildercord\.id\(|register\()\s*"([a-z0-9_/]+)"')
    for path in java_root.rglob("*.java"):
        text = path.read_text(encoding="utf-8", errors="ignore")
        for line in text.splitlines():
            for m in id_pat.finditer(line):
                ident = m.group(1)
                if "ENTITY_TYPE" in line:
                    entities.add(ident)
                elif "BuiltInRegistries.BLOCK" in line or path.name.endswith("Blocks.java"):
                    blocks.add(ident)
                elif "BuiltInRegistries.ITEM" in line or path.name.endswith("Items.java"):
                    items.add(ident)
    return items, blocks, entities


def convert_lang_to_bedrock(lang: dict[str, str]) -> bytes:
    lines = ["## Wildercord Bedrock Edition localization"]
    for key in sorted(lang.keys()):
        val = str(lang[key]).replace("\r\n", "\n").replace("\n", "\\n")
        lines.append(f"{key}={val}")
    return ("\n".join(lines) + "\n").encode("utf-8")


def build_pack(root: pathlib.Path, out_dir: pathlib.Path, include_sounds: bool = True) -> dict[str, Any]:
    assets = root / "src" / "main" / "resources" / "assets" / MOD_ID
    version = parse_version(root)
    version_str = ".".join(str(x) for x in version)
    lang = read_json(assets / "lang" / "en_us.json")

    out_dir.mkdir(parents=True, exist_ok=True)
    rp_mcpack = out_dir / "wildercord-bedrock-rp.mcpack"
    mcaddon = out_dir / "wildercord-bedrock.mcaddon"
    mappings_out = out_dir / "wildercord-geyser-mappings.json"
    bundled_mappings = root / "src" / "main" / "resources" / "wildercord-bedrock-mappings.json"
    bundled_mappings.parent.mkdir(parents=True, exist_ok=True)

    rp_header_uuid = det_uuid("wildercord.bedrock.rp.header")
    rp_module_uuid = det_uuid("wildercord.bedrock.rp.module")
    bp_header_uuid = det_uuid("wildercord.bedrock.bp.header")
    bp_module_uuid = det_uuid("wildercord.bedrock.bp.module")

    rp_files: dict[str, bytes] = {}
    bp_files: dict[str, bytes] = {}

    # 1. RP Manifest + Icon + Lang
    rp_manifest = {
        "format_version": 2,
        "header": {
            "name": f"Wildercord Bedrock Pack v{version_str}",
            "description": "Wildercord magic, runes, gear, creatures, and audio resource pack for Java/Bedrock crossplay.",
            "uuid": rp_header_uuid,
            "version": version,
            "min_engine_version": [1, 21, 0],
        },
        "modules": [
            {
                "description": "Wildercord Bedrock Resources",
                "type": "resources",
                "uuid": rp_module_uuid,
                "version": version,
            }
        ],
    }
    rp_files["manifest.json"] = write_json_bytes(rp_manifest)

    icon_path = assets / "icon.png"
    if icon_path.exists():
        rp_files["pack_icon.png"] = icon_path.read_bytes()
        bp_files["pack_icon.png"] = icon_path.read_bytes()

    rp_files["texts/languages.json"] = write_json_bytes(["en_US"])
    rp_files["texts/en_US.lang"] = convert_lang_to_bedrock(lang)

    # 2. Item textures (normalize tall animated sheets to square top frame)
    item_texture_atlas: dict[str, dict[str, str]] = {}
    tex_root = assets / "textures"
    for png in sorted((tex_root / "item").rglob("*.png")):
        rel_no_ext = png.relative_to(tex_root).with_suffix("").as_posix()  # item/...
        bedrock_rel = f"textures/items/{MOD_ID}/" + png.relative_to(tex_root / "item").with_suffix("").as_posix()
        rp_files[f"{bedrock_rel}.png"] = normalize_png_for_bedrock(png, crop_animated=True)
        atlas_key = f"{MOD_ID}." + sanitize_id(png.relative_to(tex_root / "item").with_suffix("").as_posix())
        item_texture_atlas[atlas_key] = {"textures": bedrock_rel}

    # 3. Block textures (+ animated flipbooks) + entity textures
    terrain_texture_atlas: dict[str, dict[str, str]] = {}
    flipbooks: list[dict[str, Any]] = []
    for png in sorted((tex_root / "block").rglob("*.png")):
        bedrock_rel = f"textures/blocks/{MOD_ID}/" + png.relative_to(tex_root / "block").with_suffix("").as_posix()
        rp_files[f"{bedrock_rel}.png"] = normalize_png_for_bedrock(png, crop_animated=True)
        atlas_key = f"{MOD_ID}.block_" + sanitize_id(png.relative_to(tex_root / "block").with_suffix("").as_posix())
        terrain_texture_atlas[atlas_key] = {"textures": bedrock_rel}
        # Also expose block texture under item atlas for block-items without dedicated item PNG
        item_texture_atlas.setdefault(atlas_key, {"textures": bedrock_rel})

        mcmeta = pathlib.Path(str(png) + ".mcmeta")
        if mcmeta.exists():
            try:
                meta = read_json(mcmeta)
                ticks = int(meta.get("animation", {}).get("frametime", 2))
            except Exception:
                ticks = 2
            flipbooks.append({
                "flipbook_texture": bedrock_rel,
                "atlas_tile": atlas_key,
                "ticks_per_frame": max(1, ticks),
            })

    for png in sorted((tex_root / "entity").rglob("*.png")):
        bedrock_rel = f"textures/entity/{MOD_ID}/" + png.relative_to(tex_root / "entity").with_suffix("").as_posix()
        rp_files[f"{bedrock_rel}.png"] = png.read_bytes()

    # 4. Items & Runes mappings + BP item definitions
    code_items, code_blocks, code_entities = scan_code_ids(root)
    item_ids = sorted(
        {p.relative_to(assets / "items").with_suffix("").as_posix() for p in (assets / "items").rglob("*.json")}
        | {
            k[len(f"item.{MOD_ID}."):] for k in lang if k.startswith(f"item.{MOD_ID}.") and "." not in k[len(f"item.{MOD_ID}."):]
        }
    )

    items_map: dict[str, dict[str, Any]] = {}
    next_cmd = 10000

    for item_id in item_ids:
        safe_name = sanitize_id(item_id)
        full_java = f"{MOD_ID}:{item_id}"
        bedrock_id = f"{MOD_ID}:{safe_name}"
        item_model = f"{MOD_ID}:{item_id}"
        display_name = str(lang.get(f"item.{MOD_ID}.{item_id}", lang.get(f"block.{MOD_ID}.{item_id}", item_id.replace("/", " ").replace("_", " ").title())))

        carrier, handheld, category = guess_item_carrier(item_id)
        tex_rel = resolve_item_texture_relpath(assets, item_id)
        if tex_rel and tex_rel.startswith("item/"):
            icon_key = f"{MOD_ID}." + sanitize_id(tex_rel[len("item/"):])
        elif tex_rel and tex_rel.startswith("block/"):
            icon_key = f"{MOD_ID}.block_" + sanitize_id(tex_rel[len("block/"):])
        else:
            icon_key = f"{MOD_ID}.{safe_name}"
            if icon_key not in item_texture_atlas and (assets / "icon.png").exists():
                rp_files[f"textures/items/{MOD_ID}/_fallback.png"] = (assets / "icon.png").read_bytes()
                item_texture_atlas[f"{MOD_ID}._fallback"] = {"textures": f"textures/items/{MOD_ID}/_fallback"}
                icon_key = f"{MOD_ID}._fallback"

        if icon_key not in item_texture_atlas:
            fallback_key = f"{MOD_ID}._fallback"
            if fallback_key not in item_texture_atlas and (assets / "icon.png").exists():
                rp_files[f"textures/items/{MOD_ID}/_fallback.png"] = (assets / "icon.png").read_bytes()
                item_texture_atlas[fallback_key] = {"textures": f"textures/items/{MOD_ID}/_fallback"}
            icon_key = fallback_key

        items_map[full_java] = {
            "javaCarrier": carrier,
            "bedrockId": bedrock_id,
            "itemModel": item_model,
            "icon": icon_key,
            "customModelData": next_cmd,
            "displayName": display_name,
            "handheld": handheld,
            "creativeCategory": category,
            "allowOffhand": True,
        }
        next_cmd += 1

        bp_item_doc = {
            "format_version": "1.21.0",
            "minecraft:item": {
                "description": {
                    "identifier": bedrock_id,
                    "menu_category": {"category": "equipment" if category == "EQUIPMENT" else "items"},
                },
                "components": {
                    "minecraft:icon": icon_key,
                    "minecraft:display_name": {"value": display_name},
                    "minecraft:hand_equipped": handheld,
                },
            },
        }
        bp_files[f"items/{safe_name}.json"] = write_json_bytes(bp_item_doc)

    # 5. Per-rune mappings (from assets/items/rune.json select cases)
    runes_map: dict[str, dict[str, Any]] = {}
    rune_json = assets / "items" / "rune.json"
    if rune_json.exists():
        rj = read_json(rune_json)
        for case in (rj.get("model", {}).get("cases") or []):
            rune_id = str(case.get("when", ""))
            if not rune_id:
                continue
            rune_short = rune_id.split(":", 1)[1] if ":" in rune_id else rune_id
            safe_rune = "rune_" + sanitize_id(rune_short)
            icon_key = f"{MOD_ID}.{safe_rune}"
            model_ref = case.get("model", {}).get("model", "")
            tex_sub = model_ref.split(":item/", 1)[1] if ":item/" in model_ref else f"rune/{rune_short}"
            tex_candidate = f"{MOD_ID}." + sanitize_id(tex_sub)
            if tex_candidate in item_texture_atlas:
                icon_key = tex_candidate
            elif f"{MOD_ID}.rune" in item_texture_atlas:
                icon_key = f"{MOD_ID}.rune"

            display_name = str(lang.get(f"rune.{MOD_ID}.{rune_short}.name", f"Rune: {rune_short.replace('_', ' ').title()}"))
            runes_map[rune_id] = {
                "javaCarrier": "minecraft:paper",
                "bedrockId": f"{MOD_ID}:{safe_rune}",
                "itemModel": f"{MOD_ID}:{safe_rune}",
                "icon": icon_key,
                "customModelData": next_cmd,
                "displayName": display_name,
            }
            next_cmd += 1

    # 6. Blocks mappings + Bedrock blocks.json
    block_ids = sorted({p.stem for p in (assets / "blockstates").glob("*.json")} | {
        k[len(f"block.{MOD_ID}."):] for k in lang if k.startswith(f"block.{MOD_ID}.") and "." not in k[len(f"block.{MOD_ID}."):]
    })
    blocks_map: dict[str, dict[str, Any]] = {}
    rp_blocks_json: dict[str, Any] = {"format_version": [1, 1, 0]}

    solid_block_idx = 0
    for block_id in block_ids:
        safe_b = sanitize_id(block_id)
        full_b = f"{MOD_ID}:{block_id}"
        fallback_b, solid, light = guess_block_fallback(block_id)
        is_non_solid = not solid or any(k in block_id for k in NON_SOLID_BLOCK_KEYWORDS)
        if not is_non_solid:
            carrier_state = note_block_state_for_index(solid_block_idx)
            solid_block_idx += 1
        else:
            carrier_state = fallback_b

        tex_map = resolve_block_textures(assets, block_id)
        bedrock_textures: dict[str, str] = {}
        for face, trel in tex_map.items():
            sub = trel[len("block/"):] if trel.startswith("block/") else trel
            tkey = f"{MOD_ID}.block_" + sanitize_id(sub)
            if tkey in terrain_texture_atlas:
                bedrock_textures[face] = tkey
        default_tex = (
            bedrock_textures.get("all")
            or bedrock_textures.get("side")
            or bedrock_textures.get("front")
            or next(iter(bedrock_textures.values()), "")
        )
        blocks_map[full_b] = {
            "javaCarrierState": carrier_state,
            "javaFallbackBlock": fallback_b,
            "bedrockId": f"{MOD_ID}:{safe_b}",
            "displayName": str(lang.get(f"block.{MOD_ID}.{block_id}", block_id.replace("_", " ").title())),
            "solid": not is_non_solid,
            "lightEmission": light,
            "defaultTexture": default_tex,
            "textures": bedrock_textures,
        }
        if default_tex:
            rp_blocks_json[f"{MOD_ID}:{safe_b}"] = {"textures": default_tex, "sound": "stone"}

    # 7. Entities & Particles mappings
    lang_entities = scan_lang_entities(lang)
    all_entity_ids = sorted(set(lang_entities.keys()) | code_entities | {"rune_bolt", "afterimage", "sword_slash", "domain_anchor"})
    entities_map: dict[str, dict[str, str]] = {}
    for eid in all_entity_ids:
        entities_map[f"{MOD_ID}:{eid}"] = {
            "javaCarrierEntity": guess_entity_carrier(eid),
            "displayName": str(lang_entities.get(eid, eid.replace("_", " ").title())),
        }

    particles_map = {
        f"{MOD_ID}:sigil": "minecraft:enchant",
        f"{MOD_ID}:spell_circle": "minecraft:end_rod",
        f"{MOD_ID}:light": "minecraft:end_rod",
        f"{MOD_ID}:shield": "minecraft:electric_spark",
        f"{MOD_ID}:mote": "minecraft:firework",
        f"{MOD_ID}:ritual": "minecraft:witch",
        f"{MOD_ID}:life": "minecraft:happy_villager",
        f"{MOD_ID}:void_material": "minecraft:reverse_portal",
        f"{MOD_ID}:earth": "minecraft:wax_on",
        f"{MOD_ID}:airflow": "minecraft:cloud",
        f"{MOD_ID}:camp": "minecraft:flame",
        f"{MOD_ID}:material": "minecraft:crit",
        f"{MOD_ID}:mossveil_fiber": "minecraft:cherry_leaves",
    }

    # 8. Write RP texture & sound atlases
    rp_files["textures/item_texture.json"] = write_json_bytes({
        "resource_pack_name": MOD_ID,
        "texture_name": "atlas.items",
        "texture_data": item_texture_atlas,
    })
    rp_files["textures/terrain_texture.json"] = write_json_bytes({
        "resource_pack_name": MOD_ID,
        "texture_name": "atlas.terrain",
        "padding": 8,
        "num_mip_levels": 4,
        "texture_data": terrain_texture_atlas,
    })
    rp_files["blocks.json"] = write_json_bytes(rp_blocks_json)
    if flipbooks:
        rp_files["textures/flipbook_textures.json"] = write_json_bytes(flipbooks)

    sound_count = 0
    if include_sounds and (assets / "sounds.json").exists():
        raw_sounds = read_json(assets / "sounds.json")
        bedrock_sounds: dict[str, Any] = {}
        copied_ogg: set[pathlib.Path] = set()
        for evt_name, spec in raw_sounds.items():
            entries = []
            for s in spec.get("sounds", []):
                sname = s["name"] if isinstance(s, dict) else str(s)
                if sname.startswith(f"{MOD_ID}:"):
                    srel = sname.split(":", 1)[1]
                else:
                    srel = sname
                ogg_path = assets / "sounds" / f"{srel}.ogg"
                if ogg_path.exists():
                    dest_rel = f"sounds/{MOD_ID}/{srel}"
                    if ogg_path not in copied_ogg:
                        rp_files[f"{dest_rel}.ogg"] = ogg_path.read_bytes()
                        copied_ogg.add(ogg_path)
                    entries.append({"name": dest_rel, "volume": 1.0})
            if entries:
                bedrock_sounds[f"{MOD_ID}.{evt_name}"] = {
                    "category": "neutral",
                    "sounds": entries,
                }
                sound_count += 1
        rp_files["sounds/sound_definitions.json"] = write_json_bytes({
            "format_version": "1.20.20",
            "sound_definitions": bedrock_sounds,
        })

    # 9. Behavior Pack manifest (for .mcaddon bundle)
    bp_manifest = {
        "format_version": 2,
        "header": {
            "name": f"Wildercord Bedrock Behavior v{version_str}",
            "description": "Wildercord custom Bedrock item descriptors paired with the Wildercord Resource Pack.",
            "uuid": bp_header_uuid,
            "version": version,
            "min_engine_version": [1, 21, 0],
        },
        "modules": [
            {
                "description": "Wildercord Bedrock Behaviors",
                "type": "data",
                "uuid": bp_module_uuid,
                "version": version,
            }
        ],
        "dependencies": [{"uuid": rp_header_uuid, "version": version}],
    }
    bp_files["manifest.json"] = write_json_bytes(bp_manifest)

    # 10. Write RP .mcpack and combined .mcaddon
    with zipfile.ZipFile(rp_mcpack, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as zf:
        for rel, data in sorted(rp_files.items()):
            zf.writestr(rel, data)

    with zipfile.ZipFile(mcaddon, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as zf:
        for rel, data in sorted(rp_files.items()):
            zf.writestr(f"wildercord_rp/{rel}", data)
        for rel, data in sorted(bp_files.items()):
            zf.writestr(f"wildercord_bp/{rel}", data)

    # 11. Emit Geyser / Fabric bridge mappings.json
    mappings_doc = {
        "formatVersion": 1,
        "modVersion": version_str,
        "rpUuid": rp_header_uuid,
        "items": items_map,
        "runes": runes_map,
        "blocks": blocks_map,
        "entities": entities_map,
        "particles": particles_map,
    }
    mappings_bytes = write_json_bytes(mappings_doc)
    mappings_out.write_bytes(mappings_bytes)
    bundled_mappings.write_bytes(mappings_bytes)

    return {
        "rp_mcpack": rp_mcpack,
        "mcaddon": mcaddon,
        "mappings": mappings_out,
        "bundled_mappings": bundled_mappings,
        "items": len(items_map),
        "runes": len(runes_map),
        "blocks": len(blocks_map),
        "entities": len(entities_map),
        "particles": len(particles_map),
        "sounds": sound_count,
        "rp_mb": rp_mcpack.stat().st_size / (1024 * 1024),
        "mcaddon_mb": mcaddon.stat().st_size / (1024 * 1024),
    }


def validate_outputs(rp_path: pathlib.Path, mcaddon_path: pathlib.Path, mappings_path: pathlib.Path) -> list[str]:
    """Validate console Bedrock compatibility constraints and return a list of errors (empty = valid)."""
    errors: list[str] = []
    mappings = read_json(mappings_path)

    for section in ("items", "runes", "blocks"):
        for java_id, spec in mappings.get(section, {}).items():
            bid = spec.get("bedrockId", "")
            if not IDENT_RE.match(bid):
                errors.append(f"Invalid identifier in {section}.{java_id}: {bid}")

    with zipfile.ZipFile(rp_path, "r") as zf:
        names = set(zf.namelist())
        if "manifest.json" not in names:
            errors.append("Missing manifest.json in RP .mcpack")
        else:
            man = json.loads(zf.read("manifest.json"))
            uuid.UUID(man["header"]["uuid"])
            uuid.UUID(man["modules"][0]["uuid"])

        for jf in ("textures/item_texture.json", "textures/terrain_texture.json", "blocks.json"):
            if jf not in names:
                errors.append(f"Missing {jf} in RP .mcpack")
                continue
            doc = json.loads(zf.read(jf))
            if "texture_data" in doc:
                for key, val in doc["texture_data"].items():
                    tex = val.get("textures", "")
                    if f"{tex}.png" not in names:
                        errors.append(f"Missing texture file {tex}.png referenced by {jf}:{key}")

        for n in names:
            if n.endswith(".json"):
                try:
                    json.loads(zf.read(n))
                except Exception as exc:
                    errors.append(f"Invalid JSON {n}: {exc}")

    with zipfile.ZipFile(mcaddon_path, "r") as zf:
        names = set(zf.namelist())
        for req in ("wildercord_rp/manifest.json", "wildercord_bp/manifest.json"):
            if req not in names:
                errors.append(f"Missing {req} in .mcaddon")

    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description="Export Wildercord Bedrock pack and Geyser mappings.")
    parser.add_argument("--no-sounds", action="store_true", help="Omit .ogg sound assets for smaller downloads.")
    parser.add_argument("--output-dir", default="artifacts/bedrock", help="Output directory relative to repo root.")
    args = parser.parse_args()

    root = repo_root()
    out_dir = pathlib.Path(args.output_dir)
    if not out_dir.is_absolute():
        out_dir = root / out_dir

    summary = build_pack(root, out_dir, include_sounds=not args.no_sounds)
    errors = validate_outputs(summary["rp_mcpack"], summary["mcaddon"], summary["mappings"])
    print(
        f"Exported Wildercord Bedrock pack: items={summary['items']}, runes={summary['runes']}, "
        f"blocks={summary['blocks']}, entities={summary['entities']}, sounds={summary['sounds']}"
    )
    print(f"  RP (.mcpack):  {summary['rp_mcpack']} ({summary['rp_mb']:.2f} MB)")
    print(f"  Add-on (.mcaddon): {summary['mcaddon']} ({summary['mcaddon_mb']:.2f} MB)")
    print(f"  Mappings:      {summary['mappings']} & {summary['bundled_mappings']}")
    if errors:
        print(f"VALIDATION FAILED ({len(errors)} errors):")
        for e in errors[:20]:
            print("  -", e)
        return 1
    print("Console & Bedrock pack validation PASSED (0 errors).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
