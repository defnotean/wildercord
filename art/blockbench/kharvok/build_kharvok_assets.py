#!/usr/bin/env python3
"""Build 37-bone articulated Kharvok, the Crucible Sovereign rig matching the video reference 1:1."""
from __future__ import annotations

import base64
import hashlib
import io
import json
import math
import subprocess
import sys
from pathlib import Path

import numpy as np
from PIL import Image

OUT_DIR = Path(__file__).resolve().parent
ARTIFACT_DIR = Path("C:/Users/Eating/.gemini/antigravity/brain/21001a81-2c68-4e5e-b8d9-25ca2149d964")
REF_IMG = ARTIFACT_DIR / "scratch" / "user_video" / "hero_frame.png"
VALIDATOR_CREATURE = Path("C:/Users/Eating/.gemini/config/skills/minecraft-creature-builder/scripts/validate_creature_manifest.py")
VALIDATOR_ANIM = Path("C:/Users/Eating/.gemini/config/skills/minecraft-creature-animator/scripts/validate_animation_manifest.py")
SCRATCH_RENDER = ARTIFACT_DIR / "scratch"

ATLAS_W, ATLAS_H = 256, 256


def stable_uuid(key: str) -> str:
    h = hashlib.md5(key.encode("utf-8")).hexdigest()
    return f"{h[0:8]}-{h[8:12]}-{h[12:16]}-{h[16:20]}-{h[20:32]}"


def cube_center(lo, hi):
    return [round((lo[i] + hi[i]) * 0.5, 3) for i in range(3)]


def make_cube(cid: str, bone: str, mat: str, lo, hi, *, inflate: float = 0.0, rotation=None, origin=None):
    c = {
        "id": cid,
        "bone": bone,
        "material": mat,
        "from": [round(float(x), 3) for x in lo],
        "to": [round(float(x), 3) for x in hi],
    }
    if inflate:
        c["inflate"] = round(float(inflate), 3)
    if rotation is not None:
        c["rotation"] = [round(float(x), 3) for x in rotation]
        c["origin"] = [round(float(x), 3) for x in (origin if origin is not None else cube_center(lo, hi))]
    elif origin is not None:
        c["origin"] = [round(float(x), 3) for x in origin]
    return c


def mirror_cube(cid_r: str, cid_l: str, bone_r: str, bone_l: str, mat: str, lo_r, hi_r, *, inflate: float = 0.0, rotation=None, origin=None):
    cr = make_cube(cid_r, bone_r, mat, lo_r, hi_r, inflate=inflate, rotation=rotation, origin=origin)
    lo_l = [-hi_r[0], lo_r[1], lo_r[2]]
    hi_l = [-lo_r[0], hi_r[1], hi_r[2]]
    rot_l = [rotation[0], -rotation[1], -rotation[2]] if rotation is not None else None
    orig_l = [-origin[0], origin[1], origin[2]] if origin is not None else None
    cl = make_cube(cid_l, bone_l, mat, lo_l, hi_l, inflate=inflate, rotation=rot_l, origin=orig_l)
    return [cr, cl]


def build_creature_manifest():
    # 37 bones: grounded 64px Crucible Sovereign matching video reference 1:1 (-Z forward, +X right, -X left)
    bones = [
        {"id": "root", "parent": None, "pivot": [0, 0, 0]},
        {"id": "pelvis", "parent": "root", "pivot": [0, 22.0, 0.0]},
        {"id": "skirt_front", "parent": "pelvis", "pivot": [0, 22.0, -4.5]},
        {"id": "skirt_back", "parent": "pelvis", "pivot": [0, 22.0, 4.5]},
        {"id": "tasset_right", "parent": "pelvis", "pivot": [8.0, 22.0, 0.0], "mirror_of": "tasset_left"},
        {"id": "tasset_left", "parent": "pelvis", "pivot": [-8.0, 22.0, 0.0], "mirror_of": "tasset_right"},
        {"id": "thigh_right", "parent": "pelvis", "pivot": [5.0, 21.0, 0.0], "mirror_of": "thigh_left"},
        {"id": "thigh_left", "parent": "pelvis", "pivot": [-5.0, 21.0, 0.0], "mirror_of": "thigh_right"},
        {"id": "knee_right", "parent": "thigh_right", "pivot": [5.0, 12.2, -3.8], "mirror_of": "knee_left"},
        {"id": "knee_left", "parent": "thigh_left", "pivot": [-5.0, 12.2, -3.8], "mirror_of": "knee_right"},
        {"id": "shin_right", "parent": "thigh_right", "pivot": [5.0, 11.2, 0.0], "mirror_of": "shin_left"},
        {"id": "shin_left", "parent": "thigh_left", "pivot": [-5.0, 11.2, 0.0], "mirror_of": "shin_right"},
        {"id": "foot_right", "parent": "shin_right", "pivot": [5.0, 3.2, -0.4], "mirror_of": "foot_left"},
        {"id": "foot_left", "parent": "shin_left", "pivot": [-5.0, 3.2, -0.4], "mirror_of": "foot_right"},
        # Torso, Furnace Hearth, Diagonal Chain & Asymmetric Right-Shoulder Smokestack
        {"id": "waist", "parent": "pelvis", "pivot": [0, 24.5, 0.0]},
        {"id": "chest", "parent": "waist", "pivot": [0, 33.5, 0.0]},
        {"id": "furnace_core", "parent": "chest", "pivot": [0, 29.0, -3.0]},
        {"id": "chest_chain", "parent": "chest", "pivot": [0, 39.5, -5.8]},
        {"id": "stack_right", "parent": "chest", "pivot": [7.2, 46.0, 5.2]},
        {"id": "pauldron_right", "parent": "chest", "pivot": [12.0, 46.0, 0.0], "mirror_of": "pauldron_left"},
        {"id": "pauldron_left", "parent": "chest", "pivot": [-12.0, 46.0, 0.0], "mirror_of": "pauldron_right"},
        {"id": "shoulder_cap_right", "parent": "pauldron_right", "pivot": [17.0, 46.5, 0.0], "mirror_of": "shoulder_cap_left"},
        {"id": "shoulder_cap_left", "parent": "pauldron_left", "pivot": [-17.0, 46.5, 0.0], "mirror_of": "shoulder_cap_right"},
        # Warhelm & Sweeping Curved Bull Horns
        {"id": "neck", "parent": "chest", "pivot": [0, 47.2, -0.4]},
        {"id": "head", "parent": "neck", "pivot": [0, 49.6, -0.4]},
        {"id": "jaw_grate", "parent": "head", "pivot": [0, 50.6, -4.6]},
        {"id": "visor_brow", "parent": "head", "pivot": [0, 55.6, -4.6]},
        {"id": "horn_right", "parent": "head", "pivot": [5.8, 56.5, -0.5], "mirror_of": "horn_left"},
        {"id": "horn_left", "parent": "head", "pivot": [-5.8, 56.5, -0.5], "mirror_of": "horn_right"},
        # Arms: Free Right Gauntlet (+X) & Left Crucible Greathammer (-X)
        {"id": "upper_arm_right", "parent": "chest", "pivot": [13.0, 44.0, 0.0], "mirror_of": "upper_arm_left"},
        {"id": "upper_arm_left", "parent": "chest", "pivot": [-13.0, 44.0, 0.0], "mirror_of": "upper_arm_right"},
        {"id": "forearm_right", "parent": "upper_arm_right", "pivot": [13.8, 34.0, 0.0], "mirror_of": "forearm_left"},
        {"id": "forearm_left", "parent": "upper_arm_left", "pivot": [-13.8, 34.0, 0.0], "mirror_of": "forearm_right"},
        {"id": "shackle_right", "parent": "forearm_right", "pivot": [13.8, 28.5, 0.0], "mirror_of": "shackle_left"},
        {"id": "shackle_left", "parent": "forearm_left", "pivot": [-13.8, 28.5, 0.0], "mirror_of": "shackle_right"},
        {"id": "anvil_maul", "parent": "forearm_left", "pivot": [-14.4, 22.8, -2.2]},
        {"id": "maul_head", "parent": "anvil_maul", "pivot": [-14.4, 7.5, -2.2]},
    ]

    cubes = []

    # Pelvis, Heavy Belt & 3-Panel Segmented Armored Fauld Kilt (matches video)
    cubes.append(make_cube("pelvis_block", "pelvis", "basalt_iron", [-8.5, 20.2, -4.6], [8.5, 25.0, 4.6]))
    cubes.append(make_cube("belt_band", "pelvis", "foundry_bronze", [-9.0, 22.4, -4.9], [9.0, 24.8, 4.9], inflate=0.05))
    cubes.append(make_cube("belt_buckle", "pelvis", "foundry_bronze", [-2.2, 22.0, -5.5], [2.2, 25.0, -4.5]))
    cubes.append(make_cube("skirt_f_top", "skirt_front", "bronze_plate", [-4.6, 17.5, -5.2], [4.6, 22.2, -4.0]))
    cubes.append(make_cube("skirt_f_bot", "skirt_front", "bronze_plate", [-4.0, 13.4, -5.0], [4.0, 17.8, -4.0]))
    cubes.append(make_cube("skirt_b_plate", "skirt_back", "bronze_plate", [-6.4, 13.6, 4.0], [6.4, 22.2, 5.2]))
    cubes.extend(mirror_cube("tasset_r_upper", "tasset_l_upper", "tasset_right", "tasset_left", "bronze_plate",
                             [4.8, 17.2, -5.0], [10.4, 22.2, 3.8], rotation=[0, 0, -10], origin=[7.6, 21.5, 0.0]))
    cubes.extend(mirror_cube("tasset_r_lower", "tasset_l_lower", "tasset_right", "tasset_left", "bronze_plate",
                             [5.2, 12.6, -4.8], [10.8, 17.6, 3.6], rotation=[0, 0, -12], origin=[8.0, 17.5, 0.0]))

    # Chunky Bronze-Rimmed Legs, Square Knee Plates, & Sabaton Boots (Y = 0)
    cubes.extend(mirror_cube("thigh_r_body", "thigh_l_body", "thigh_right", "thigh_left", "bronze_plate", [1.9, 11.8, -3.7], [8.1, 21.2, 3.7]))
    cubes.extend(mirror_cube("knee_r_slab", "knee_l_slab", "knee_right", "knee_left", "bronze_plate", [2.6, 10.0, -5.1], [7.4, 14.4, -3.2]))
    cubes.extend(mirror_cube("shin_r_greave", "shin_l_greave", "shin_right", "shin_left", "bronze_plate", [2.0, 3.2, -3.9], [8.0, 11.8, 3.9]))
    cubes.extend(mirror_cube("foot_r_sole", "foot_l_sole", "foot_right", "foot_left", "foundry_bronze", [1.6, 0.0, -5.4], [8.4, 2.0, 4.2]))
    cubes.extend(mirror_cube("foot_r_vamp", "foot_l_vamp", "foot_right", "foot_left", "bronze_plate", [1.9, 2.0, -5.0], [8.1, 4.6, 4.0]))

    # Open Blazing Molten Hearth Belly & Arched Rib Cage + Dark-Iron Breastplate
    cubes.append(make_cube("waist_column", "waist", "basalt_iron", [-6.8, 24.4, -3.4], [6.8, 33.4, 3.6]))
    cubes.append(make_cube("furnace_core_box", "furnace_core", "furnace_hearth", [-5.8, 24.8, -4.9], [5.8, 32.8, -1.0]))
    cubes.append(make_cube("chest_hull", "chest", "basalt_iron", [-9.8, 32.8, -5.4], [9.8, 46.8, 5.4]))
    cubes.append(make_cube("chest_breast_r", "chest", "bronze_plate", [0.8, 34.4, -6.5], [9.4, 46.2, -4.8]))
    cubes.append(make_cube("chest_breast_l", "chest", "bronze_plate", [-9.4, 34.4, -6.5], [-0.8, 46.2, -4.8]))
    cubes.append(make_cube("chest_center_rib", "chest", "basalt_iron", [-1.2, 26.2, -5.6], [1.2, 34.8, -4.3]))
    for rx_s, ry_l in ((2.0, 29.0), (2.2, 31.6)):
        cubes.append(make_cube(f"rib_r_{ry_l}", "chest", "bronze_plate", [rx_s, ry_l, -5.4], [8.0, ry_l + 1.5, -4.1], rotation=[0, 0, -14]))
        cubes.append(make_cube(f"rib_l_{ry_l}", "chest", "bronze_plate", [-8.0, ry_l, -5.4], [-rx_s, ry_l + 1.5, -4.1], rotation=[0, 0, 14]))
    cubes.append(make_cube("chest_back_plate", "chest", "bronze_plate", [-8.8, 33.6, 4.8], [8.8, 46.0, 6.4]))

    # Diagonal Heavy Chain Bandolier Across Chest (from character LEFT shoulder -X up -> RIGHT waist +X down)
    for ci in range(7):
        u = ci / 6.0
        lx = -8.4 + u * 16.8
        ly = 45.2 - u * 15.6
        cubes.append(make_cube(
            f"cchain_{ci}", "chest_chain", "slag_chain" if ci % 2 == 0 else "foundry_bronze",
            [lx - 1.5, ly - 1.2, -7.1], [lx + 1.5, ly + 1.2, -5.4],
            rotation=[0, 0, -38],
        ))

    # Smokestack chimney ember slightly recessed inside collar so it glows inside the rim without a bright block on top
    cubes.append(make_cube("stack_r_base", "stack_right", "bronze_plate", [4.8, 44.0, 3.6], [9.6, 51.0, 8.2], rotation=[0, 0, -8], origin=[7.2, 46.0, 5.8]))
    cubes.append(make_cube("stack_r_pipe", "stack_right", "charred_steel", [5.4, 50.0, 4.0], [9.4, 59.8, 8.0], rotation=[0, 0, -8], origin=[7.2, 46.0, 5.8]))
    cubes.append(make_cube("stack_r_lip", "stack_right", "foundry_bronze", [4.9, 59.0, 3.5], [9.9, 61.0, 8.5], rotation=[0, 0, -8], origin=[7.2, 46.0, 5.8]))
    cubes.append(make_cube("stack_r_glow", "stack_right", "forge_ember_emissive", [6.2, 60.4, 4.8], [8.6, 61.3, 7.2], rotation=[0, 0, -8], origin=[7.2, 46.0, 5.8]))

    # 3-Tier Stepped Bronze-Edged Pauldrons + Sharp Outward Spikes
    cubes.extend(mirror_cube("pauldron_r_top", "pauldron_l_top", "pauldron_right", "pauldron_left", "bronze_plate",
                             [9.6, 46.2, -5.4], [16.8, 49.6, 5.4], rotation=[0, 0, -8]))
    cubes.extend(mirror_cube("pauldron_r_mid", "pauldron_l_mid", "pauldron_right", "pauldron_left", "bronze_plate",
                             [10.2, 42.6, -5.0], [18.0, 46.4, 5.0], rotation=[0, 0, -10]))
    cubes.extend(mirror_cube("pauldron_r_low", "pauldron_l_low", "pauldron_right", "pauldron_left", "bronze_plate",
                             [10.6, 39.2, -4.6], [18.6, 42.8, 4.6], rotation=[0, 0, -12]))
    cubes.extend(mirror_cube("scap_r_base", "scap_l_base", "shoulder_cap_right", "shoulder_cap_left", "basalt_iron",
                             [15.6, 47.4, -1.8], [19.4, 50.2, 1.8], rotation=[0, 0, 14]))
    cubes.extend(mirror_cube("scap_r_tip", "scap_l_tip", "shoulder_cap_right", "shoulder_cap_left", "charred_steel",
                             [18.8, 48.0, -1.0], [21.6, 49.6, 1.0], rotation=[0, 0, 14]))

    # Warhelm (Bronze T-Bar Nose Crest, Blazing Slit Eyes, 4 Vertical Bronze Grate Bars) + Curved Bull Horns
    cubes.append(make_cube("neck_collar", "neck", "basalt_iron", [-4.2, 46.6, -3.8], [4.2, 49.8, 3.8]))
    cubes.append(make_cube("head_helm", "head", "bronze_plate", [-5.4, 49.4, -5.2], [5.4, 59.2, 5.2]))
    cubes.append(make_cube("visor_brow_plate", "visor_brow", "foundry_bronze", [-5.7, 55.6, -5.8], [5.7, 57.6, -4.4]))
    cubes.append(make_cube("nose_crest_t", "visor_brow", "foundry_bronze", [-1.0, 52.8, -6.0], [1.0, 57.6, -4.8]))
    cubes.append(make_cube("eye_r", "head", "forge_ember_emissive", [1.3, 53.6, -5.8], [4.5, 55.3, -4.8]))
    cubes.append(make_cube("eye_l", "head", "forge_ember_emissive", [-4.5, 53.6, -5.8], [-1.3, 55.3, -4.8]))
    cubes.append(make_cube("jaw_furnace_slit", "jaw_grate", "furnace_hearth", [-4.4, 49.7, -5.5], [4.4, 52.4, -4.6]))
    for gx in (-3.2, -1.1, 1.1, 3.2):
        cubes.append(make_cube(f"grate_bar_{gx}", "jaw_grate", "foundry_bronze", [gx - 0.55, 49.5, -5.9], [gx + 0.55, 52.7, -4.9]))
    # Sweeping 3-segment curved bronze bull horns
    cubes.extend(mirror_cube("horn_r_1", "horn_l_1", "horn_right", "horn_left", "foundry_bronze",
                             [5.2, 55.4, -1.8], [9.4, 58.6, 1.8], rotation=[0, 0, -10]))
    cubes.extend(mirror_cube("horn_r_2", "horn_l_2", "horn_right", "horn_left", "foundry_bronze",
                             [8.2, 57.6, -1.6], [11.6, 62.4, 1.4], rotation=[-12, 0, 14]))
    cubes.extend(mirror_cube("horn_r_3", "horn_l_3", "horn_right", "horn_left", "foundry_bronze",
                             [8.8, 61.6, -2.2], [11.2, 66.2, 0.2], rotation=[-20, 0, 26]))

    # Stepped Bronze-Edged Gauntlets & Arms (NO shield on right arm!)
    cubes.extend(mirror_cube("uarm_r_core", "uarm_l_core", "upper_arm_right", "upper_arm_left", "bronze_plate", [10.4, 34.0, -3.2], [16.4, 43.2, 3.2]))
    cubes.extend(mirror_cube("farm_r_top", "farm_l_top", "forearm_right", "forearm_left", "bronze_plate", [10.4, 29.8, -3.5], [16.8, 34.2, 3.5]))
    cubes.extend(mirror_cube("farm_r_mid", "farm_l_mid", "forearm_right", "forearm_left", "bronze_plate", [10.6, 26.0, -3.4], [16.6, 30.0, 3.4]))
    cubes.extend(mirror_cube("shackle_r_ring", "shackle_l_ring", "shackle_right", "shackle_left", "foundry_bronze",
                             [10.2, 24.4, -3.6], [17.0, 26.2, 3.6], inflate=0.05))
    cubes.extend(mirror_cube("fist_r", "fist_l", "forearm_right", "forearm_left", "basalt_iron", [11.0, 21.4, -3.0], [16.2, 24.6, 3.0]))

    # LEFT HAND (-X): Crucible Greathammer angled down-forward beside left knee, with 3 crisp glowing orange runes
    pivot_grip = [-14.4, 22.8, -2.2]
    hammer_rot = [26, -10, -10]
    cubes.append(make_cube("maul_pommel", "anvil_maul", "foundry_bronze", [-15.8, 25.8, -3.8], [-12.8, 28.6, -0.8], rotation=hammer_rot, origin=pivot_grip))
    cubes.append(make_cube("maul_shaft", "anvil_maul", "charred_steel", [-15.7, 11.4, -3.6], [-13.1, 26.2, -1.0], rotation=hammer_rot, origin=pivot_grip))
    cubes.append(make_cube("maul_collar", "anvil_maul", "foundry_bronze", [-16.0, 11.6, -3.9], [-12.6, 13.8, -0.7], rotation=hammer_rot, origin=pivot_grip))
    cubes.append(make_cube("maul_head_body", "maul_head", "maul_runed", [-18.6, 3.6, -8.4], [-10.2, 11.4, 3.8], rotation=hammer_rot, origin=pivot_grip))
    cubes.append(make_cube("maul_cap_front", "maul_head", "foundry_bronze", [-19.0, 3.2, -9.6], [-9.8, 11.8, -7.8], rotation=hammer_rot, origin=pivot_grip))
    cubes.append(make_cube("maul_cap_back", "maul_head", "foundry_bronze", [-19.0, 3.2, 3.2], [-9.8, 11.8, 5.0], rotation=hammer_rot, origin=pivot_grip))

    materials = [
        {"id": "basalt_iron", "type": "opaque"},
        {"id": "charred_steel", "type": "opaque"},
        {"id": "foundry_bronze", "type": "opaque"},
        {"id": "bronze_plate", "type": "opaque"},
        {"id": "slag_chain", "type": "opaque"},
        {"id": "forge_ember_emissive", "type": "emissive"},
        {"id": "furnace_hearth", "type": "emissive"},
        {"id": "maul_runed", "type": "emissive"},
    ]
    return {
        "creature_id": "kharvok_crucible_sovereign",
        "ground_y": 0.0,
        "floating_entity": False,
        "materials": materials,
        "bones": bones,
        "cubes": cubes,
    }


def _sample_key(dur: float, fps: int = 20):
    n = int(round(dur * fps))
    return [round(i / fps, 3) for i in range(n + 1)]


def build_animation_manifest(creature):
    bone_ids = [b["id"] for b in creature["bones"]]
    clips = []

    # 1. Idle (2.4s loop): towering imposing breathing, hammer resting low at left side, right gauntlet relaxed
    dur_idle = 2.4
    tr_idle = {b: {"rotation": {}, "position": {}} for b in (
        "pelvis", "waist", "chest", "head", "upper_arm_right", "forearm_right",
        "upper_arm_left", "forearm_left", "anvil_maul", "chest_chain", "skirt_front", "skirt_back"
    )}
    for t in _sample_key(dur_idle):
        is_end = abs(t - dur_idle) < 1e-4
        ph = 0.0 if (t == 0.0 or is_end) else 2.0 * math.pi * (t / dur_idle)
        s1, c1 = math.sin(ph), math.cos(ph)
        s2 = math.sin(ph * 2.0)
        key = "0.0" if t == 0.0 else (f"{dur_idle}" if is_end else f"{t:.2f}".rstrip("0").rstrip("."))
        tr_idle["pelvis"]["position"][key] = [0.0, round(0.35 * s2, 3), 0.0]
        tr_idle["pelvis"]["rotation"][key] = [round(1.2 * s1, 3), round(2.0 * c1, 3), 0.0]
        tr_idle["waist"]["rotation"][key] = [round(-1.5 * s1, 3), round(-1.5 * c1, 3), 0.0]
        tr_idle["chest"]["rotation"][key] = [round(-1.5 + 2.0 * s1, 3), round(2.0 * s1, 3), 0.0]
        tr_idle["head"]["rotation"][key] = [round(-1.5 * s1, 3), round(-2.5 * c1, 3), 0.0]
        tr_idle["upper_arm_right"]["rotation"][key] = [round(4.0 + 3.0 * s1, 3), 0.0, round(7.5 + 1.5 * c1, 3)]
        tr_idle["forearm_right"]["rotation"][key] = [round(12.0 + 3.0 * c1, 3), 0.0, 0.0]
        tr_idle["upper_arm_left"]["rotation"][key] = [round(4.0 - 2.0 * s1, 3), 0.0, round(-10.5 - 1.5 * c1, 3)]
        tr_idle["forearm_left"]["rotation"][key] = [round(8.0 - 2.0 * c1, 3), 0.0, 0.0]
        tr_idle["anvil_maul"]["rotation"][key] = [round(2.0 + 2.0 * s1, 3), 0.0, 0.0]
        tr_idle["chest_chain"]["rotation"][key] = [round(2.0 * s2, 3), 0.0, round(1.5 * s1, 3)]
        tr_idle["skirt_front"]["rotation"][key] = [round(3.0 + 2.0 * s2, 3), 0.0, 0.0]
        tr_idle["skirt_back"]["rotation"][key] = [round(-3.0 - 2.0 * s2, 3), 0.0, 0.0]

    clips.append({
        "id": "animation.kharvok.idle",
        "kind": "idle",
        "duration": dur_idle,
        "loop": "loop",
        "tracks": {b: {k: v for k, v in ch.items() if v} for b, ch in tr_idle.items()},
    })

    # 2. Stride (1.60s deliberate unstoppable juggernaut march matching the user's video 1:1!)
    dur_stride = 1.6
    tr_str = {b: {"rotation": {}, "position": {}} for b in (
        "pelvis", "thigh_right", "thigh_left", "shin_right", "shin_left", "foot_right", "foot_left",
        "waist", "chest", "head", "upper_arm_right", "forearm_right", "upper_arm_left", "forearm_left",
        "anvil_maul", "chest_chain", "skirt_front", "skirt_back", "tasset_right", "tasset_left"
    )}
    for t in _sample_key(dur_stride):
        is_end = abs(t - dur_stride) < 1e-4
        ph = 0.0 if (t == 0.0 or is_end) else 2.0 * math.pi * (t / dur_stride)
        key = "0.0" if t == 0.0 else (f"{dur_stride}" if is_end else f"{t:.2f}".rstrip("0").rstrip("."))
        s1, c1 = math.sin(ph), math.cos(ph)
        s2 = math.sin(2.0 * ph)
        c2 = math.cos(2.0 * ph)

        # Positive rx swings hanging limbs (-Y) forward (-Z); negative rx swings knees back
        def leg_kin(p_off: float):
            sp, cp = math.sin(ph + p_off), math.cos(ph + p_off)
            swing_lift = max(0.0, cp)
            thigh = 21.0 * sp
            shin = -6.0 - 22.0 * swing_lift
            foot = -6.0 * max(0.0, -sp) + 12.0 * max(0.0, sp) * swing_lift
            tasset = 4.0 + 16.0 * max(0.0, sp)
            return thigh, shin, foot, tasset

        tr_r, sh_r, ft_r, tas_r = leg_kin(0.0)
        tr_l, sh_l, ft_l, tas_l = leg_kin(math.pi)

        tr_str["pelvis"]["position"][key] = [round(0.75 * s1, 3), round(-0.5 + 0.9 * abs(s1), 3), 0.0]
        tr_str["pelvis"]["rotation"][key] = [round(-2.0, 3), round(5.5 * c1, 3), round(2.5 * s1, 3)]
        tr_str["thigh_right"]["rotation"][key] = [round(tr_r, 3), 0.0, 0.0]
        tr_str["thigh_left"]["rotation"][key] = [round(tr_l, 3), 0.0, 0.0]
        tr_str["shin_right"]["rotation"][key] = [round(sh_r, 3), 0.0, 0.0]
        tr_str["shin_left"]["rotation"][key] = [round(sh_l, 3), 0.0, 0.0]
        tr_str["foot_right"]["rotation"][key] = [round(ft_r, 3), 0.0, 0.0]
        tr_str["foot_left"]["rotation"][key] = [round(ft_l, 3), 0.0, 0.0]
        tr_str["tasset_right"]["rotation"][key] = [round(tas_r, 3), 0.0, round(-4.0 * s1, 3)]
        tr_str["tasset_left"]["rotation"][key] = [round(tas_l, 3), 0.0, round(-4.0 * s1, 3)]

        tr_str["waist"]["rotation"][key] = [round(1.0 * abs(c2), 3), round(-4.5 * c1, 3), 0.0]
        tr_str["chest"]["rotation"][key] = [round(-2.5 + 1.5 * abs(s2), 3), round(-6.5 * c1, 3), round(-1.8 * s1 + 1.5, 3)]
        tr_str["head"]["rotation"][key] = [round(3.5, 3), round(5.5 * c1, 3), 0.0]

        # Right arm free heavy stride (+rz flares outward +X); Left arm carries heavy Greathammer low & clear (-rz flares outward -X)
        tr_str["upper_arm_right"]["rotation"][key] = [round(4.0 - 14.0 * s1 - 5.0 * c1, 3), 0.0, round(7.5, 3)]
        tr_str["forearm_right"]["rotation"][key] = [round(12.0 + 6.0 * max(0.0, -s1), 3), 0.0, 0.0]
        tr_str["upper_arm_left"]["rotation"][key] = [round(4.0 + 9.0 * s1 + 4.0 * c1, 3), 0.0, round(-11.0, 3)]
        tr_str["forearm_left"]["rotation"][key] = [round(8.0 + 4.0 * c1, 3), 0.0, 0.0]
        tr_str["anvil_maul"]["rotation"][key] = [round(2.0 + 4.0 * c1, 3), 0.0, 0.0]

        tr_str["chest_chain"]["rotation"][key] = [round(2.5 * abs(s1), 3), 0.0, round(1.8 * c1, 3)]
        tr_str["skirt_front"]["rotation"][key] = [round(8.0 + 10.0 * abs(s1), 3), 0.0, 0.0]
        tr_str["skirt_back"]["rotation"][key] = [round(-8.0 - 8.0 * abs(s1), 3), 0.0, 0.0]

    clips.append({
        "id": "animation.kharvok.stride",
        "kind": "walk",
        "duration": dur_stride,
        "loop": "loop",
        "contralateral_pairs": [["thigh_right", "thigh_left"], ["upper_arm_right", "upper_arm_left"]],
        "tracks": {b: {k: v for k, v in ch.items() if v} for b, ch in tr_str.items()},
    })

    def bake_keyframe_table(dur: float, keyframes: dict[str, list[tuple[float, list[float], list[float] | None]]]):
        tracks = {}
        for bname, pts in keyframes.items():
            rot_map, pos_map = {}, {}
            for ts, r, p in pts:
                k = f"{ts:.2f}".rstrip("0").rstrip(".") if ts > 0 else "0.0"
                rot_map[k] = [round(float(x), 3) for x in r]
                if p is not None:
                    pos_map[k] = [round(float(x), 3) for x in p]
            tracks[bname] = {"rotation": rot_map}
            if pos_map:
                tracks[bname]["position"] = pos_map
        return tracks

    # 3. Anvil Shatter (1.55s): hoist left-hand Crucible Greathammer UP IN FRONT OVERHEAD (+154) -> downward SMACK
    dur_shatter = 1.55
    clips.append({
        "id": "animation.kharvok.anvil_shatter",
        "kind": "attack",
        "duration": dur_shatter,
        "loop": "play_once",
        "beats": {"anticipation": 0.45, "strike": 0.70, "impact": 0.85, "recovery": 1.40},
        "tracks": bake_keyframe_table(dur_shatter, {
            "pelvis": [(0.0, [0, 0, 0], [0, 0, 0]), (0.45, [6, 20, 0], [0, -1.0, 1.5]), (0.85, [-12, -16, 0], [0, -3.2, -4.5]), (1.55, [0, 0, 0], [0, 0, 0])],
            "thigh_left": [(0.0, [0, 0, 0], None), (0.85, [28, 0, 0], None), (1.55, [0, 0, 0], None)],
            "shin_left": [(0.0, [0, 0, 0], None), (0.85, [-28, 0, 0], None), (1.55, [0, 0, 0], None)],
            "thigh_right": [(0.0, [0, 0, 0], None), (0.85, [-20, 0, 0], None), (1.55, [0, 0, 0], None)],
            "waist": [(0.0, [0, 0, 0], None), (0.45, [8, 18, 0], None), (0.85, [-14, -14, 0], None), (1.55, [0, 0, 0], None)],
            "chest": [(0.0, [0, 0, 0], None), (0.45, [10, 22, 0], None), (0.85, [-16, -18, 0], None), (1.55, [0, 0, 0], None)],
            "head": [(0.0, [0, 0, 0], None), (0.45, [-10, -26, 0], None), (0.85, [18, 14, 0], None), (1.55, [0, 0, 0], None)],
            "upper_arm_left": [(0.0, [0, 0, 0], None), (0.45, [154, 0, 14], None), (0.70, [92, 0, 4], None), (0.85, [28, 0, -8], None), (1.15, [24, 0, -6], None), (1.55, [0, 0, 0], None)],
            "forearm_left": [(0.0, [8, 0, 0], None), (0.45, [28, 0, 0], None), (0.85, [14, 0, 0], None), (1.55, [8, 0, 0], None)],
            "anvil_maul": [(0.0, [0, 0, 0], None), (0.45, [14, 0, 0], None), (0.85, [22, 0, 0], None), (1.15, [20, 0, 0], None), (1.55, [0, 0, 0], None)],
            "upper_arm_right": [(0.0, [0, 0, 0], None), (0.45, [54, -18, 16], None), (0.85, [24, 10, 18], None), (1.55, [0, 0, 0], None)],
        }),
    })

    # 4. Chain Shackle (2.00s): free right gauntlet reaches high forward to yank forge chains while left holds hammer guard
    dur_shackle = 2.00
    clips.append({
        "id": "animation.kharvok.chain_shackle",
        "kind": "attack",
        "duration": dur_shackle,
        "loop": "play_once",
        "beats": {"anticipation": 0.45, "strike": 0.90, "impact": 1.25, "recovery": 1.80},
        "tracks": bake_keyframe_table(dur_shackle, {
            "pelvis": [(0.0, [0, 0, 0], [0, 0, 0]), (0.45, [-8, -16, 0], [0, -1.8, 1.5]), (1.25, [8, 14, 0], [0, -1.0, -3.0]), (2.00, [0, 0, 0], [0, 0, 0])],
            "waist": [(0.0, [0, 0, 0], None), (0.45, [-6, -16, 0], None), (1.25, [8, 16, 0], None), (2.00, [0, 0, 0], None)],
            "chest": [(0.0, [0, 0, 0], None), (0.45, [-8, -20, 0], None), (0.90, [10, 22, 0], None), (1.25, [10, 24, 0], None), (2.00, [0, 0, 0], None)],
            "head": [(0.0, [0, 0, 0], None), (0.45, [8, 0, 0], None), (1.25, [14, -14, 0], None), (2.00, [0, 0, 0], None)],
            "upper_arm_right": [(0.0, [0, 0, 0], None), (0.45, [118, -16, -18], None), (0.90, [78, 14, 10], None), (1.25, [38, 20, 14], None), (2.00, [0, 0, 0], None)],
            "forearm_right": [(0.0, [12, 0, 0], None), (0.90, [18, 0, 0], None), (1.25, [28, 0, 0], None), (2.00, [12, 0, 0], None)],
            "upper_arm_left": [(0.0, [0, 0, 0], None), (0.45, [22, 0, -16], None), (0.90, [32, 0, -22], None), (2.00, [0, 0, 0], None)],
            "stack_right": [(0.0, [0, 0, 0], None), (0.90, [-8, 0, 10], None), (2.00, [0, 0, 0], None)],
        }),
    })

    # 5. Crucible Meltdown (2.90s): crouch gather -> twin arms raised HIGH OVERHEAD IN FRONT -> twin ground smash
    dur_melt = 2.90
    clips.append({
        "id": "animation.kharvok.crucible_meltdown",
        "kind": "attack",
        "duration": dur_melt,
        "loop": "play_once",
        "beats": {"anticipation": 0.70, "strike": 1.95, "impact": 2.15, "recovery": 2.75},
        "tracks": bake_keyframe_table(dur_melt, {
            "pelvis": [(0.0, [0, 0, 0], [0, 0, 0]), (0.70, [-10, 0, 0], [0, -3.2, 0]), (1.60, [8, 0, 0], [0, 1.2, -1]), (2.15, [-14, 0, 0], [0, -3.8, -4]), (2.90, [0, 0, 0], [0, 0, 0])],
            "thigh_right": [(0.0, [0, 0, 0], None), (0.70, [26, 0, -8], None), (1.60, [-8, 0, 0], None), (2.15, [28, 0, 0], None), (2.90, [0, 0, 0], None)],
            "shin_right": [(0.0, [0, 0, 0], None), (0.70, [-36, 0, 0], None), (1.60, [10, 0, 0], None), (2.15, [-38, 0, 0], None), (2.90, [0, 0, 0], None)],
            "thigh_left": [(0.0, [0, 0, 0], None), (0.70, [26, 0, 8], None), (1.60, [-8, 0, 0], None), (2.15, [28, 0, 0], None), (2.90, [0, 0, 0], None)],
            "shin_left": [(0.0, [0, 0, 0], None), (0.70, [-36, 0, 0], None), (1.60, [10, 0, 0], None), (2.15, [-38, 0, 0], None), (2.90, [0, 0, 0], None)],
            "waist": [(0.0, [0, 0, 0], None), (0.70, [-12, 0, 0], None), (1.60, [10, 0, 0], None), (2.15, [-14, 0, 0], None), (2.90, [0, 0, 0], None)],
            "chest": [(0.0, [0, 0, 0], None), (0.70, [-14, 0, 0], None), (1.60, [12, 0, 0], None), (2.15, [-16, 0, 0], None), (2.90, [0, 0, 0], None)],
            "head": [(0.0, [0, 0, 0], None), (0.70, [18, 0, 0], None), (1.60, [18, 0, 0], None), (2.15, [20, 0, 0], None), (2.90, [0, 0, 0], None)],
            "upper_arm_right": [(0.0, [0, 0, 0], None), (0.70, [48, 0, 18], None), (1.60, [156, 0, -16], None), (1.95, [96, 0, 6], None), (2.15, [28, 0, 12], None), (2.90, [0, 0, 0], None)],
            "upper_arm_left": [(0.0, [0, 0, 0], None), (0.70, [48, 0, -18], None), (1.60, [154, 0, 16], None), (1.95, [96, 0, -6], None), (2.15, [28, 0, -12], None), (2.90, [0, 0, 0], None)],
            "anvil_maul": [(0.0, [0, 0, 0], None), (1.60, [14, 0, 0], None), (2.15, [22, 0, 0], None), (2.90, [0, 0, 0], None)],
        }),
    })

    return {
        "rig_bones": bone_ids,
        "joint_limits_deg": {},
        "clips": clips,
    }


def deterministic_noise(x: int, y: int, seed: int) -> float:
    n = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def _stamp_zig_runes(img_arr, x0, y0, w, h):
    """Paint 3 crisp 1px glowing orange forge-rune glyphs centered cleanly with dark iron margins."""
    core = [255, 224, 118, 255]
    halo = [232, 112, 18, 255]
    if w < 10 or h < 7:
        return
    # 3 distinct slender glyphs along Z (cols 3, 6, 9 on 12px face), height inside rows 2..6 (leaving >= 2px dark iron margin)
    cy = h // 2
    cols = (max(2, int(round(w * 0.25))), w // 2, min(w - 3, int(round(w * 0.75))))
    patterns = (
        [(0, -2), (-1, -1), (0, 0), (1, 1), (0, 2)],
        [(-1, -2), (0, -2), (0, -1), (-1, 0), (0, 1), (1, 2)],
        [(0, -2), (1, -1), (0, 0), (-1, 1), (0, 2)],
    )
    for gx, pts in zip(cols, patterns):
        for ox, oy in pts:
            px, py = gx + ox, cy + oy
            if 1 <= px < w - 1 and 1 <= py < h - 1:
                img_arr[y0 + py, x0 + px] = core
                if px + 1 < w - 1 and img_arr[y0 + py, x0 + px + 1, 3] == 0:
                    img_arr[y0 + py, x0 + px + 1] = halo


def paint_region(diff_arr, glow_arr, x0, y0, w, h, mat, face_name, cid):
    palettes = {
        "basalt_iron": ((28, 30, 36), (50, 52, 62), (16, 18, 22), (72, 76, 88)),
        "charred_steel": ((36, 38, 46), (62, 66, 78), (20, 22, 28), (92, 96, 110)),
        "foundry_bronze": ((132, 90, 40), (174, 126, 62), (82, 52, 20), (198, 152, 82)),
        "bronze_plate": ((30, 32, 38), (52, 54, 66), (18, 19, 24), (138, 94, 42)),
        "slag_chain": ((44, 47, 56), (72, 76, 88), (24, 26, 32), (104, 110, 124)),
        "forge_ember_emissive": ((248, 118, 18), (255, 188, 56), (176, 52, 10), (255, 234, 148)),
        "furnace_hearth": ((255, 132, 24), (255, 212, 82), (204, 64, 14), (255, 250, 214)),
        "maul_runed": ((30, 32, 38), (50, 52, 62), (18, 20, 24), (112, 76, 34)),
    }
    base, hi, dark, accent = palettes[mat]
    seed = abs(hash((cid, face_name))) & 0xFFFF
    for dy in range(h):
        for dx in range(w):
            px, py = x0 + dx, y0 + dy
            if px >= diff_arr.shape[1] or py >= diff_arr.shape[0]:
                continue
            r = deterministic_noise(dx, dy, seed)
            col = list(base)
            is_edge = (dx == 0 or dy == 0 or dx == w - 1 or dy == h - 1) and w >= 3 and h >= 3

            if mat == "furnace_hearth":
                cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
                dist = math.hypot((dx - cx) / max(w / 2.0, 1.0), (dy - cy) / max(h / 2.0, 1.0))
                col = list(accent if dist < 0.42 else (hi if dist < 0.78 else (base if dist < 1.05 else dark)))
                glow_arr[py, px] = [col[0], col[1], col[2], 255]
            elif mat == "forge_ember_emissive":
                cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
                dist = max(abs(dx - cx) / max(w / 2.0, 1.0), abs(dy - cy) / max(h / 2.0, 1.0))
                col = list(accent if dist < 0.4 else (hi if dist < 0.78 else base))
                glow_arr[py, px] = [col[0], col[1], col[2], 255]
            elif mat == "bronze_plate":
                if is_edge:
                    col = [138, 94, 42] if face_name in ("north", "east", "west", "south", "up") else list(dark)
                elif dx == 1 or dy == 1 or dx == w - 2 or dy == h - 2:
                    col = list(dark)
                elif face_name == "up":
                    col = [int(base[i] * 0.65 + hi[i] * 0.35) for i in range(3)]
                elif r > 0.86:
                    col = [int(col[i] * 0.86 + hi[i] * 0.14) for i in range(3)]
            elif mat == "maul_runed":
                if is_edge:
                    col = [96, 66, 30]
                elif r > 0.88:
                    col = list(hi)
            else:
                if face_name == "up":
                    col = [int(base[i] * 0.65 + hi[i] * 0.35) for i in range(3)]
                elif face_name == "down":
                    col = [int(base[i] * 0.6 + dark[i] * 0.4) for i in range(3)]
                if is_edge:
                    col = [int(col[i] * 0.6 + dark[i] * 0.4) for i in range(3)]
                elif mat == "foundry_bronze":
                    if (dx + dy) % 4 == 0 and r > 0.52:
                        col = list(accent)
                    elif r < 0.2:
                        col = list(dark)
                elif r > 0.88:
                    col = [int(col[i] * 0.88 + hi[i] * 0.12) for i in range(3)]
                elif r < 0.14:
                    col = [int(col[i] * 0.88 + dark[i] * 0.12) for i in range(3)]

            diff_arr[py, px] = [col[0], col[1], col[2], 255]

    if mat == "maul_runed" and face_name in ("east", "west"):
        _stamp_zig_runes(diff_arr, x0, y0, w, h)
        _stamp_zig_runes(glow_arr, x0, y0, w, h)


def pack_and_paint_textures(cubes):
    diffuse_arr = np.zeros((ATLAS_H, ATLAS_W, 4), dtype=np.uint8)
    glow_arr = np.zeros((ATLAS_H, ATLAS_W, 4), dtype=np.uint8)
    state = {"x": 1, "y": 1, "row_h": 0}

    def alloc(rw: int, rh: int):
        rw = max(1, int(math.ceil(rw)))
        rh = max(1, int(math.ceil(rh)))
        if state["x"] + rw + 1 > ATLAS_W:
            state["x"] = 1
            state["y"] += state["row_h"] + 1
            state["row_h"] = 0
        if state["y"] + rh + 1 > ATLAS_H:
            raise RuntimeError(f"Texture atlas overflow at {rw}x{rh}")
        ux, vy = state["x"], state["y"]
        state["x"] += rw + 1
        state["row_h"] = max(state["row_h"], rh)
        return ux, vy, rw, rh

    cube_uvs = {}
    for c in cubes:
        lo, hi = c["from"], c["to"]
        dx = max(1, round(hi[0] - lo[0]))
        dy = max(1, round(hi[1] - lo[1]))
        dz = max(1, round(hi[2] - lo[2]))
        mat = c["material"]
        face_uvs = {}
        for fname, (fw, fh) in {
            "north": (dx, dy), "south": (dx, dy),
            "east": (dz, dy), "west": (dz, dy),
            "up": (dx, dz), "down": (dx, dz),
        }.items():
            ux, vy, rw, rh = alloc(fw, fh)
            paint_region(diffuse_arr, glow_arr, ux, vy, rw, rh, mat, fname, c["id"])
            face_uvs[fname] = {"uv": [ux, vy, ux + fw, vy + fh], "texture": 0}
        cube_uvs[c["id"]] = face_uvs

    diff_img = Image.fromarray(diffuse_arr, "RGBA")
    glow_img = Image.fromarray(glow_arr, "RGBA")
    diff_img.save(OUT_DIR / "kharvok_diffuse.png")
    glow_img.save(OUT_DIR / "kharvok_glow.png")
    return diff_img, glow_img, cube_uvs


def img_to_data_url(img: Image.Image) -> str:
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode("ascii")


def to_bbmodel(creature, anim_manifest, diff_img, cube_uvs):
    bone_uuids = {b["id"]: stable_uuid("bone:" + b["id"]) for b in creature["bones"]}
    cube_uuids = {c["id"]: stable_uuid("cube:" + c["id"]) for c in creature["cubes"]}
    bone_map = {b["id"]: b for b in creature["bones"]}
    cubes_by_bone = {b["id"]: [] for b in creature["bones"]}
    elements = []
    for c in creature["cubes"]:
        cid, bid = c["id"], c["bone"]
        cubes_by_bone[bid].append(cube_uuids[cid])
        elem = {
            "name": cid, "box_uv": False, "render_order": "default", "locked": False,
            "export": True, "scope": 0, "allow_mirror_modeling": True,
            "from": c["from"], "to": c["to"], "autouv": 0, "color": 4,
            "origin": c.get("origin", bone_map[bid]["pivot"]),
            "faces": cube_uvs[cid], "type": "cube", "uuid": cube_uuids[cid],
        }
        if c.get("rotation") is not None:
            elem["rotation"] = c["rotation"]
        if c.get("inflate"):
            elem["inflate"] = c["inflate"]
        elements.append(elem)

    groups, child_bones, root_ids = [], {b["id"]: [] for b in creature["bones"]}, []
    for b in creature["bones"]:
        bid = b["id"]
        (root_ids if b["parent"] is None else child_bones[b["parent"]]).append(bid)
        groups.append({
            "name": bid, "uuid": bone_uuids[bid], "export": True, "locked": False,
            "scope": 0, "selected": False, "visibility": True,
            "_static": {"properties": {}, "temp_data": {}},
            "origin": b["pivot"], "rotation": [0, 0, 0], "bedrock_binding": "",
            "color": 0, "children": [], "reset": False, "shade": True,
            "mirror_uv": False, "autouv": 0, "isOpen": True, "primary_selected": False,
        })

    def node(bid):
        return {"uuid": bone_uuids[bid], "isOpen": True, "children": list(cubes_by_bone[bid]) + [node(k) for k in child_bones[bid]]}

    bb_anims = []
    for clip in anim_manifest["clips"]:
        cid = clip["id"]
        animators = {}
        for bname, channels in clip["tracks"].items():
            kfs = []
            for ch_name, ch_map in channels.items():
                for t_str, vec in sorted(ch_map.items(), key=lambda kv: float(kv[0])):
                    kfs.append({
                        "channel": ch_name,
                        "data_points": [{"x": str(vec[0]), "y": str(vec[1]), "z": str(vec[2])}],
                        "uuid": stable_uuid(f"kf:{cid}:{bname}:{ch_name}:{t_str}"),
                        "time": float(t_str), "color": -1, "interpolation": "linear",
                    })
            animators[bone_uuids[bname]] = {
                "name": bname, "type": "bone", "rotation_global": False,
                "quaternion_interpolation": False, "keyframes": kfs,
            }
        bb_anims.append({
            "uuid": stable_uuid("anim:" + cid), "name": cid,
            "loop": "loop" if clip["loop"] == "loop" else "once",
            "override": False, "length": clip["duration"], "snapping": 20,
            "selected": cid == "animation.kharvok.stride",
            "saved": True, "path": "", "scope": 0, "anim_time_update": "",
            "blend_weight": "", "start_delay": "", "loop_delay": "", "animators": animators,
        })

    return {
        "meta": {"format_version": "5.0", "model_format": "bedrock", "box_uv": False},
        "name": "kharvok_crucible_sovereign",
        "model_identifier": "kharvok_crucible_sovereign",
        "visible_box": [3.5, 4.5, 0],
        "variable_placeholders": "", "multi_file_ruleset": "",
        "variable_placeholder_buttons": [], "bedrock_animation_mode": "entity",
        "timeline_setups": [], "unhandled_root_fields": {},
        "ai_used": False, "ai_agents": "",
        "resolution": {"width": ATLAS_W, "height": ATLAS_H},
        "elements": elements, "groups": groups, "outliner": [node(r) for r in root_ids],
        "textures": [{
            "name": "kharvok_diffuse.png", "path": "", "folder": "", "namespace": "",
            "id": "0", "group": "", "scope": 0, "width": ATLAS_W, "height": ATLAS_H,
            "uv_width": ATLAS_W, "uv_height": ATLAS_H, "particle": False,
            "use_as_default": True, "layers_enabled": False, "sync_to_project": "",
            "file_format": "png", "render_mode": "default", "render_sides": "auto",
            "wrap_mode": "limited", "pbr_channel": "color", "fps": 7,
            "force_translucent": False, "frame_time": 1, "frame_order_type": "loop",
            "frame_order": "", "frame_interpolate": False, "visible": True,
            "internal": True, "saved": False, "uuid": stable_uuid("tex:diffuse"),
            "source": img_to_data_url(diff_img),
        }],
        "animations": bb_anims,
        "animation_variable_placeholders": "",
    }


def to_geo_json(creature, cube_uvs):
    cubes_by_bone = {b["id"]: [] for b in creature["bones"]}
    for c in creature["cubes"]:
        lo, hi = c["from"], c["to"]
        f_uv = {}
        for fn in ("north", "east", "south", "west", "up", "down"):
            ux, vy, u2, v2 = cube_uvs[c["id"]][fn]["uv"]
            f_uv[fn] = {"uv": [ux, vy], "uv_size": [u2 - ux, v2 - vy]}
        gc = {"origin": [lo[0], lo[1], lo[2]], "size": [hi[0] - lo[0], hi[1] - lo[1], hi[2] - lo[2]], "uv": f_uv}
        if c.get("rotation") is not None:
            gc["rotation"] = c["rotation"]
            gc["pivot"] = c["origin"]
        if c.get("inflate"):
            gc["inflate"] = c["inflate"]
        cubes_by_bone[c["bone"]].append(gc)

    bones_out = []
    for b in creature["bones"]:
        bo = {"name": b["id"], "pivot": b["pivot"]}
        if b["parent"] is not None:
            bo["parent"] = b["parent"]
        if cubes_by_bone[b["id"]]:
            bo["cubes"] = cubes_by_bone[b["id"]]
        bones_out.append(bo)

    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.kharvok_crucible_sovereign",
                "texture_width": ATLAS_W, "texture_height": ATLAS_H,
                "visible_bounds_width": 4.2, "visible_bounds_height": 4.5,
                "visible_bounds_offset": [0, 2.25, 0],
            },
            "bones": bones_out,
        }],
    }


def to_animation_json(anim_manifest):
    anims_out = {}
    for clip in anim_manifest["clips"]:
        bones_out = {}
        for bname, channels in clip["tracks"].items():
            bones_out[bname] = {
                ch_name: {t_str: list(vec) for t_str, vec in sorted(ch_map.items(), key=lambda kv: float(kv[0]))}
                for ch_name, ch_map in channels.items()
            }
        entry = {"animation_length": clip["duration"], "bones": bones_out}
        if clip["loop"] == "loop":
            entry["loop"] = True
        anims_out[clip["id"]] = entry
    return {"format_version": "1.8.0", "animations": anims_out}


def main():
    creature = build_creature_manifest()
    creature_path = OUT_DIR / "creature_manifest.json"
    creature_path.write_text(json.dumps(creature, indent=2) + "\n", encoding="utf-8")
    res_c = subprocess.run([sys.executable, str(VALIDATOR_CREATURE), str(creature_path)], check=False, text=True, capture_output=True)
    print(res_c.stdout.strip())
    if res_c.returncode != 0:
        print(res_c.stderr)
        sys.exit(res_c.returncode)

    anim_manifest = build_animation_manifest(creature)
    anim_path = OUT_DIR / "animation_manifest.json"
    anim_path.write_text(json.dumps(anim_manifest, indent=2) + "\n", encoding="utf-8")
    res_a = subprocess.run([sys.executable, str(VALIDATOR_ANIM), str(anim_path)], check=False, text=True, capture_output=True)
    print(res_a.stdout.strip())
    if res_a.returncode != 0:
        print(res_a.stderr)
        sys.exit(res_a.returncode)

    diff_img, _, cube_uvs = pack_and_paint_textures(creature["cubes"])
    bbmodel = to_bbmodel(creature, anim_manifest, diff_img, cube_uvs)
    (OUT_DIR / "kharvok_crucible_sovereign.bbmodel").write_text(json.dumps(bbmodel, indent=2) + "\n", encoding="utf-8")
    geo_path = OUT_DIR / "kharvok_crucible_sovereign.geo.json"
    anim_json_path = OUT_DIR / "kharvok_crucible_sovereign.animation.json"
    geo_path.write_text(json.dumps(to_geo_json(creature, cube_uvs), indent=2) + "\n", encoding="utf-8")
    anim_json_path.write_text(json.dumps(to_animation_json(anim_manifest), indent=2) + "\n", encoding="utf-8")
    print(f"Built Kharvok: {len(creature['bones'])} bones, {len(creature['cubes'])} cubes, {len(anim_manifest['clips'])} clips.")

    if "--render" in sys.argv:
        sys.path.insert(0, str(SCRATCH_RENDER))
        import bb_render
        gm = bb_render.GeoModel(geo_path, anim_json_path, OUT_DIR / "kharvok_diffuse.png", OUT_DIR / "kharvok_glow.png")
        bb_render.render_suite("kharvok", gm, REF_IMG, [c["id"] for c in anim_manifest["clips"]], ARTIFACT_DIR, scale=4.0, center_y=32.0)
        print("Rendered Kharvok turnaround compare, contact sheet, and 5 GIFs.")


if __name__ == "__main__":
    main()
