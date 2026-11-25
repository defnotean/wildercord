#!/usr/bin/env python3
"""Build 40-bone articulated Kharvok, the Crucible Sovereign rig, textures, and 5 grounded 20 TPS animations."""
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
REF_IMG = ARTIFACT_DIR / "kharvok_turnaround_ref_1795609383213.jpg"
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
    # 40 bones: grounded 64px tall foundry sovereign aligned to kharvok_turnaround_ref.jpg
    bones = [
        {"id": "root", "parent": None, "pivot": [0, 0, 0]},
        {"id": "pelvis", "parent": "root", "pivot": [0, 22.0, 0.0]},
        {"id": "skirt_front", "parent": "pelvis", "pivot": [0, 21.5, -4.5]},
        {"id": "skirt_back", "parent": "pelvis", "pivot": [0, 21.5, 4.5]},
        {"id": "tasset_right", "parent": "pelvis", "pivot": [8.0, 22.0, 0.0], "mirror_of": "tasset_left"},
        {"id": "tasset_left", "parent": "pelvis", "pivot": [-8.0, 22.0, 0.0], "mirror_of": "tasset_right"},
        {"id": "thigh_right", "parent": "pelvis", "pivot": [5.0, 21.0, 0.0], "mirror_of": "thigh_left"},
        {"id": "thigh_left", "parent": "pelvis", "pivot": [-5.0, 21.0, 0.0], "mirror_of": "thigh_right"},
        {"id": "knee_right", "parent": "thigh_right", "pivot": [5.0, 12.0, -3.5], "mirror_of": "knee_left"},
        {"id": "knee_left", "parent": "thigh_left", "pivot": [-5.0, 12.0, -3.5], "mirror_of": "knee_right"},
        {"id": "shin_right", "parent": "thigh_right", "pivot": [5.0, 11.0, 0.0], "mirror_of": "shin_left"},
        {"id": "shin_left", "parent": "thigh_left", "pivot": [-5.0, 11.0, 0.0], "mirror_of": "shin_right"},
        {"id": "foot_right", "parent": "shin_right", "pivot": [5.0, 3.0, -0.5], "mirror_of": "foot_left"},
        {"id": "foot_left", "parent": "shin_left", "pivot": [-5.0, 3.0, -0.5], "mirror_of": "foot_right"},
        # Torso & Furnace Core
        {"id": "waist", "parent": "pelvis", "pivot": [0, 25.0, 0.0]},
        {"id": "chest", "parent": "waist", "pivot": [0, 33.0, 0.0]},
        {"id": "furnace_core", "parent": "chest", "pivot": [0, 29.5, -3.2]},
        {"id": "stack_right", "parent": "chest", "pivot": [5.0, 47.0, 6.0], "mirror_of": "stack_left"},
        {"id": "stack_left", "parent": "chest", "pivot": [-5.0, 47.0, 6.0], "mirror_of": "stack_right"},
        {"id": "back_chain_mid", "parent": "chest", "pivot": [0, 37.5, 7.0]},
        {"id": "pauldron_right", "parent": "chest", "pivot": [12.0, 46.5, 0.0], "mirror_of": "pauldron_left"},
        {"id": "pauldron_left", "parent": "chest", "pivot": [-12.0, 46.5, 0.0], "mirror_of": "pauldron_right"},
        {"id": "shoulder_cap_right", "parent": "pauldron_right", "pivot": [16.5, 45.5, 0.0], "mirror_of": "shoulder_cap_left"},
        {"id": "shoulder_cap_left", "parent": "pauldron_left", "pivot": [-16.5, 45.5, 0.0], "mirror_of": "shoulder_cap_right"},
        # Head & Crown
        {"id": "neck", "parent": "chest", "pivot": [0, 47.5, -0.5]},
        {"id": "head", "parent": "neck", "pivot": [0, 49.5, -0.5]},
        {"id": "jaw_grate", "parent": "head", "pivot": [0, 50.5, -4.5]},
        {"id": "visor_brow", "parent": "head", "pivot": [0, 55.5, -4.5]},
        {"id": "horn_right", "parent": "head", "pivot": [6.5, 57.5, -0.5], "mirror_of": "horn_left"},
        {"id": "horn_left", "parent": "head", "pivot": [-6.5, 57.5, -0.5], "mirror_of": "horn_right"},
        # Arms & Weapons
        {"id": "upper_arm_right", "parent": "chest", "pivot": [12.5, 44.0, 0.0], "mirror_of": "upper_arm_left"},
        {"id": "upper_arm_left", "parent": "chest", "pivot": [-12.5, 44.0, 0.0], "mirror_of": "upper_arm_right"},
        {"id": "forearm_right", "parent": "upper_arm_right", "pivot": [13.5, 34.0, 0.0], "mirror_of": "forearm_left"},
        {"id": "forearm_left", "parent": "upper_arm_left", "pivot": [-13.5, 34.0, 0.0], "mirror_of": "forearm_right"},
        {"id": "shackle_right", "parent": "forearm_right", "pivot": [13.5, 28.0, 0.0], "mirror_of": "shackle_left"},
        {"id": "shackle_left", "parent": "forearm_left", "pivot": [-13.5, 28.0, 0.0], "mirror_of": "shackle_right"},
        {"id": "anvil_maul", "parent": "forearm_right", "pivot": [13.5, 24.5, -2.5]},
        {"id": "maul_head", "parent": "anvil_maul", "pivot": [13.5, 11.5, -9.5]},
        {"id": "forge_shield", "parent": "forearm_left", "pivot": [-16.8, 28.0, -1.0]},
        {"id": "shield_chain", "parent": "forge_shield", "pivot": [-16.8, 17.5, -1.0]},
    ]

    cubes = []

    # Pelvis & armored waist belt / tassets
    cubes.append(make_cube("pelvis_block", "pelvis", "basalt_iron", [-8.5, 20.0, -4.5], [8.5, 25.0, 4.5]))
    cubes.append(make_cube("belt_band", "pelvis", "foundry_bronze", [-9.0, 22.0, -4.8], [9.0, 24.6, 4.8], inflate=0.05))
    cubes.append(make_cube("belt_buckle", "pelvis", "foundry_bronze", [-2.4, 21.6, -5.4], [2.4, 24.9, -4.4]))
    cubes.append(make_cube("belt_gem", "pelvis", "forge_ember_emissive", [-1.1, 22.5, -5.6], [1.1, 24.1, -5.2]))

    cubes.append(make_cube("skirt_f_plate", "skirt_front", "basalt_iron", [-5.0, 14.0, -5.1], [5.0, 22.0, -4.0]))
    cubes.append(make_cube("skirt_f_trim", "skirt_front", "foundry_bronze", [-5.2, 13.6, -5.3], [5.2, 15.4, -3.9], inflate=0.05))
    cubes.append(make_cube("skirt_f_seam", "skirt_front", "foundry_bronze", [-0.7, 15.2, -5.3], [0.7, 21.8, -4.7]))
    cubes.append(make_cube("skirt_b_plate", "skirt_back", "basalt_iron", [-6.0, 13.5, 4.0], [6.0, 22.0, 5.2]))
    cubes.append(make_cube("skirt_b_trim", "skirt_back", "foundry_bronze", [-6.2, 13.1, 4.1], [6.2, 14.9, 5.4], inflate=0.05))
    cubes.extend(mirror_cube("tasset_r_upper", "tasset_l_upper", "tasset_right", "tasset_left", "basalt_iron",
                             [7.6, 15.5, -4.2], [9.6, 22.2, 4.2], rotation=[0, 0, -8]))
    cubes.extend(mirror_cube("tasset_r_trim", "tasset_l_trim", "tasset_right", "tasset_left", "foundry_bronze",
                             [7.8, 15.0, -4.4], [9.9, 16.6, 4.4], inflate=0.04, rotation=[0, 0, -8]))
    cubes.extend(mirror_cube("tasset_r_lower", "tasset_l_lower", "tasset_right", "tasset_left", "charred_steel",
                             [8.0, 12.2, -3.4], [9.6, 15.8, 3.4], rotation=[0, 0, -11]))

    # Legs, knees, shins, and grounded foundry boots (Y = 0)
    cubes.extend(mirror_cube("thigh_r_body", "thigh_l_body", "thigh_right", "thigh_left", "basalt_iron", [2.0, 11.5, -3.6], [8.0, 21.2, 3.6]))
    cubes.extend(mirror_cube("thigh_r_strap", "thigh_l_strap", "thigh_right", "thigh_left", "foundry_bronze", [1.9, 13.0, -3.8], [8.1, 14.8, 3.8]))
    cubes.extend(mirror_cube("knee_r_rim", "knee_l_rim", "knee_right", "knee_left", "foundry_bronze", [2.6, 9.6, -4.9], [7.4, 14.2, -3.2]))
    cubes.extend(mirror_cube("knee_r_slab", "knee_l_slab", "knee_right", "knee_left", "charred_steel", [3.3, 10.3, -5.3], [6.7, 13.5, -4.3]))
    cubes.extend(mirror_cube("shin_r_greave", "shin_l_greave", "shin_right", "shin_left", "charred_steel", [2.1, 3.0, -3.8], [7.9, 11.6, 3.8]))
    cubes.extend(mirror_cube("shin_r_trim", "shin_l_trim", "shin_right", "shin_left", "foundry_bronze", [1.9, 8.8, -4.0], [8.1, 10.6, 4.0]))
    cubes.extend(mirror_cube("foot_r_sole", "foot_l_sole", "foot_right", "foot_left", "foundry_bronze", [1.6, 0.0, -5.2], [8.4, 2.2, 4.2]))
    cubes.extend(mirror_cube("foot_r_vamp", "foot_l_vamp", "foot_right", "foot_left", "basalt_iron", [2.0, 2.2, -4.8], [8.0, 4.8, 4.0]))
    cubes.extend(mirror_cube("foot_r_toe", "foot_l_toe", "foot_right", "foot_left", "foundry_bronze", [2.8, 1.0, -6.0], [7.2, 3.6, -4.5]))

    # Waist & Molten Furnace Ribcage + Chest Armor
    cubes.append(make_cube("waist_column", "waist", "basalt_iron", [-6.4, 24.5, -3.5], [6.4, 33.2, 3.5]))
    cubes.append(make_cube("furnace_core_box", "furnace_core", "forge_ember_emissive", [-5.4, 25.0, -4.6], [5.4, 32.8, 3.2]))
    for idx, ry in enumerate((27.0, 29.2, 31.4)):
        cubes.append(make_cube(f"rib_r_{idx}", "chest", "foundry_bronze", [1.4, ry, -5.3], [6.8, ry + 1.2, -4.0], rotation=[0, 0, -10]))
        cubes.append(make_cube(f"rib_l_{idx}", "chest", "foundry_bronze", [-6.8, ry, -5.3], [-1.4, ry + 1.2, -4.0], rotation=[0, 0, 10]))
    cubes.append(make_cube("chest_core_pillar", "chest", "basalt_iron", [-1.5, 25.0, -5.3], [1.5, 34.0, -4.1]))

    cubes.append(make_cube("chest_hull", "chest", "basalt_iron", [-10.0, 33.0, -5.6], [10.0, 47.0, 5.6]))
    cubes.append(make_cube("chest_breast_r", "chest", "charred_steel", [0.5, 34.5, -6.6], [9.4, 45.8, -5.0]))
    cubes.append(make_cube("chest_breast_l", "chest", "charred_steel", [-9.4, 34.5, -6.6], [-0.5, 45.8, -5.0]))
    cubes.append(make_cube("chest_collar", "chest", "foundry_bronze", [-8.2, 44.8, -6.4], [8.2, 47.6, 5.8], inflate=0.05))
    cubes.append(make_cube("chest_gorget_v", "chest", "foundry_bronze", [-3.0, 41.0, -6.9], [3.0, 46.8, -5.6]))
    cubes.append(make_cube("chest_back_plate", "chest", "charred_steel", [-8.6, 34.0, 5.0], [8.6, 45.8, 6.6]))

    # Twin back chimney smokestacks & draped foundry chain
    cubes.extend(mirror_cube("stack_r_base", "stack_l_base", "stack_right", "stack_left", "foundry_bronze", [2.6, 41.0, 5.2], [7.4, 46.5, 9.6]))
    cubes.extend(mirror_cube("stack_r_pipe", "stack_l_pipe", "stack_right", "stack_left", "charred_steel", [3.2, 46.5, 5.8], [6.8, 57.5, 9.0]))
    cubes.extend(mirror_cube("stack_r_cap", "stack_l_cap", "stack_right", "stack_left", "foundry_bronze", [2.7, 56.8, 5.3], [7.3, 59.0, 9.5]))
    cubes.extend(mirror_cube("stack_r_glow", "stack_l_glow", "stack_right", "stack_left", "forge_ember_emissive", [3.6, 58.6, 6.2], [6.4, 60.4, 8.6]))

    for ci, (cx, cy, cz, rz) in enumerate((
        (6.8, 41.8, 6.8, 34), (4.2, 39.2, 7.2, 20), (1.4, 37.6, 7.5, 8),
        (-1.4, 37.6, 7.5, -8), (-4.2, 39.2, 7.2, -20), (-6.8, 41.8, 6.8, -34)
    )):
        cubes.append(make_cube(f"bchain_{ci}", "back_chain_mid", "slag_chain",
                               [cx - 1.5, cy - 1.1, cz - 1.1], [cx + 1.5, cy + 1.1, cz + 1.1],
                               rotation=[0, 0, rz]))

    # Spiked Anvil Pauldrons (3-tier silhouette matching ref)
    cubes.extend(mirror_cube("pauldron_r_low", "pauldron_l_low", "pauldron_right", "pauldron_left", "foundry_bronze", [9.2, 41.8, -6.2], [17.5, 44.0, 6.2], rotation=[0, 0, -8]))
    cubes.extend(mirror_cube("pauldron_r_mid", "pauldron_l_mid", "pauldron_right", "pauldron_left", "basalt_iron", [9.6, 43.8, -5.8], [17.0, 48.8, 5.8], rotation=[0, 0, -6]))
    cubes.extend(mirror_cube("pauldron_r_top", "pauldron_l_top", "pauldron_right", "pauldron_left", "foundry_bronze", [9.8, 48.2, -5.4], [15.4, 50.4, 5.4], rotation=[0, 0, -4]))
    cubes.extend(mirror_cube("pauldron_r_seam", "pauldron_l_seam", "pauldron_right", "pauldron_left", "forge_ember_emissive", [10.0, 44.2, -6.0], [16.6, 45.2, 6.0], rotation=[0, 0, -6]))
    cubes.extend(mirror_cube("scap_r_horn", "scap_l_horn", "shoulder_cap_right", "shoulder_cap_left", "charred_steel", [16.0, 46.2, -2.0], [22.2, 49.4, 2.0], rotation=[0, 0, 14]))

    # Horned Basalt-and-Bronze Furnace Helm + Glowing Eyes + Visor Slits
    cubes.append(make_cube("neck_collar", "neck", "charred_steel", [-4.2, 46.8, -3.8], [4.2, 50.0, 3.8]))
    cubes.append(make_cube("head_helm", "head", "basalt_iron", [-5.4, 49.5, -5.4], [5.4, 59.5, 5.4]))
    cubes.append(make_cube("helm_crest", "head", "foundry_bronze", [-1.2, 54.0, -6.1], [1.2, 60.4, 5.8]))
    cubes.append(make_cube("visor_brow_plate", "visor_brow", "foundry_bronze", [-5.8, 55.2, -6.0], [5.8, 57.2, 5.7]))
    cubes.append(make_cube("jaw_grate_frame", "jaw_grate", "foundry_bronze", [-5.6, 49.4, -6.0], [5.6, 53.0, -4.4]))
    for gx in (-3.6, -1.2, 1.2, 3.6):
        cubes.append(make_cube(f"grate_slit_{gx}", "jaw_grate", "forge_ember_emissive", [gx - 0.5, 49.9, -6.2], [gx + 0.5, 52.3, -5.4]))
    cubes.append(make_cube("eye_r", "head", "forge_ember_emissive", [1.5, 53.4, -5.8], [4.4, 55.0, -4.9]))
    cubes.append(make_cube("eye_l", "head", "forge_ember_emissive", [-4.4, 53.4, -5.8], [-1.5, 55.0, -4.9]))
    cubes.extend(mirror_cube("horn_r_base", "horn_l_base", "horn_right", "horn_left", "foundry_bronze", [5.2, 55.6, -2.0], [10.4, 58.8, 2.0]))
    cubes.extend(mirror_cube("horn_r_tip", "horn_l_tip", "horn_right", "horn_left", "foundry_bronze", [8.4, 58.4, -1.6], [11.6, 63.8, 1.6], rotation=[0, 0, -10]))
    cubes.extend(mirror_cube("helm_pipe_r", "helm_pipe_l", "head", "head", "charred_steel", [2.2, 59.4, -1.2], [4.6, 62.6, 2.2]))
    cubes.extend(mirror_cube("helm_ember_r", "helm_ember_l", "head", "head", "forge_ember_emissive", [2.6, 62.4, -0.8], [4.2, 63.8, 1.6]))

    # Muscular Armored Arms & Chained Gauntlets
    cubes.extend(mirror_cube("uarm_r_core", "uarm_l_core", "upper_arm_right", "upper_arm_left", "basalt_iron", [10.0, 34.0, -3.2], [16.0, 44.5, 3.2]))
    cubes.extend(mirror_cube("uarm_r_cuff", "uarm_l_cuff", "upper_arm_right", "upper_arm_left", "foundry_bronze", [9.8, 34.2, -3.5], [16.2, 36.2, 3.5]))
    cubes.extend(mirror_cube("farm_r_gaunt", "farm_l_gaunt", "forearm_right", "forearm_left", "basalt_iron", [10.2, 23.0, -3.4], [16.4, 34.2, 3.4]))
    cubes.extend(mirror_cube("farm_r_band", "farm_l_band", "forearm_right", "forearm_left", "foundry_bronze", [10.0, 29.8, -3.7], [16.6, 32.0, 3.7]))
    cubes.extend(mirror_cube("shackle_r_ring", "shackle_l_ring", "shackle_right", "shackle_left", "slag_chain", [9.9, 26.2, -3.9], [16.8, 28.4, 3.9], inflate=0.05))
    cubes.extend(mirror_cube("fist_r", "fist_l", "forearm_right", "forearm_left", "foundry_bronze", [10.8, 21.8, -3.0], [16.0, 24.4, 3.0]))

    # Right Weapon: Huge Rune-Etched Crucible Greathammer (Anvil-Maul) held cleanly outside right leg
    cubes.append(make_cube("maul_shaft_up", "anvil_maul", "foundry_bronze", [13.5, 13.0, -3.5], [15.9, 32.5, -1.1]))
    cubes.append(make_cube("maul_shaft_low", "anvil_maul", "charred_steel", [13.7, 8.5, -3.3], [15.7, 15.0, -1.3]))
    cubes.append(make_cube("maul_pommel", "anvil_maul", "foundry_bronze", [13.2, 32.0, -3.8], [16.2, 34.8, -0.8]))
    cubes.append(make_cube("maul_head_body", "maul_head", "basalt_iron", [10.8, 0.8, -8.5], [18.8, 9.6, 3.8]))
    cubes.append(make_cube("maul_cap_front", "maul_head", "foundry_bronze", [10.4, 0.4, -9.4], [19.2, 10.0, -7.4]))
    cubes.append(make_cube("maul_cap_back", "maul_head", "foundry_bronze", [10.4, 0.4, 2.8], [19.2, 10.0, 4.8]))
    cubes.append(make_cube("maul_rune_r", "maul_head", "forge_ember_emissive", [18.5, 2.6, -6.4], [19.3, 7.8, 1.8]))
    cubes.append(make_cube("maul_rune_l", "maul_head", "forge_ember_emissive", [10.3, 2.6, -6.4], [11.1, 7.8, 1.8]))

    # Left Forearm: Forgeheart Censer / Heavy Foundry Shield with Glowing Core + Hanging Chain
    cubes.append(make_cube("shield_plate", "forge_shield", "charred_steel", [-18.4, 20.0, -6.8], [-15.8, 37.5, 5.2]))
    cubes.append(make_cube("shield_rim_top", "forge_shield", "foundry_bronze", [-18.8, 36.2, -5.8], [-15.5, 38.8, 4.2]))
    cubes.append(make_cube("shield_rim_bot", "forge_shield", "foundry_bronze", [-18.8, 18.6, -5.8], [-15.5, 21.2, 4.2]))
    cubes.append(make_cube("shield_boss", "forge_shield", "forge_ember_emissive", [-19.4, 24.2, -3.2], [-18.0, 33.4, 1.4]))
    for bar_z in (-2.0, 0.2):
        cubes.append(make_cube(f"shield_bar_{bar_z}", "forge_shield", "basalt_iron", [-19.7, 25.0, bar_z - 0.45], [-18.2, 32.6, bar_z + 0.45]))
    for li, ly in enumerate((16.5, 13.8, 11.2)):
        cubes.append(make_cube(f"shield_chain_{li}", "shield_chain", "slag_chain", [-17.5, ly, -1.6], [-15.9, ly + 2.2, 0.0], rotation=[0, 18 * (li % 2), 0]))

    materials = [
        {"id": "basalt_iron", "type": "opaque"},
        {"id": "charred_steel", "type": "opaque"},
        {"id": "foundry_bronze", "type": "opaque"},
        {"id": "slag_chain", "type": "opaque"},
        {"id": "forge_ember_emissive", "type": "emissive"},
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

    # 1. Idle (2.4s loop)
    dur_idle = 2.4
    t_idle = _sample_key(dur_idle)
    tr_idle = {b: {"rotation": {}, "position": {}} for b in (
        "pelvis", "waist", "chest", "head", "upper_arm_right", "forearm_right",
        "upper_arm_left", "forearm_left", "anvil_maul", "forge_shield",
        "back_chain_mid", "shield_chain", "skirt_front", "skirt_back"
    )}
    for t in t_idle:
        ph = 2.0 * math.pi * (t / dur_idle)
        ts = f"{t:.3f}".rstrip("0").rstrip(".") if "." in f"{t:.3f}" else str(t)
        if t == 0.0 or abs(t - dur_idle) < 1e-4:
            ph = 0.0
        s1, c1 = math.sin(ph), math.cos(ph)
        s2 = math.sin(ph * 2.0)
        key = "0.0" if t == 0.0 else (f"{dur_idle}" if abs(t - dur_idle) < 1e-4 else f"{t:.2f}".rstrip("0").rstrip("."))
        tr_idle["pelvis"]["position"][key] = [0.0, round(0.4 * s2, 3), 0.0]
        tr_idle["pelvis"]["rotation"][key] = [round(1.5 * s1, 3), round(2.0 * c1, 3), 0.0]
        tr_idle["waist"]["rotation"][key] = [round(-2.5 * s1, 3), round(-1.5 * c1, 3), 0.0]
        tr_idle["chest"]["rotation"][key] = [round(3.5 + 3.0 * s1, 3), round(2.5 * s1, 3), 0.0]
        tr_idle["head"]["rotation"][key] = [round(-2.0 * s1, 3), round(-3.5 * c1, 3), 0.0]
        tr_idle["upper_arm_right"]["rotation"][key] = [round(-6.0 + 3.5 * s1, 3), 0.0, round(-4.0 - 1.5 * c1, 3)]
        tr_idle["forearm_right"]["rotation"][key] = [round(-14.0 + 3.0 * c1, 3), 0.0, 0.0]
        tr_idle["anvil_maul"]["rotation"][key] = [round(5.0 + 2.5 * s1, 3), 0.0, 0.0]
        tr_idle["upper_arm_left"]["rotation"][key] = [round(-4.0 - 3.0 * s1, 3), 0.0, round(5.0 + 1.5 * c1, 3)]
        tr_idle["forearm_left"]["rotation"][key] = [round(-18.0 - 3.0 * c1, 3), 0.0, 0.0]
        tr_idle["forge_shield"]["rotation"][key] = [0.0, round(-6.0 + 3.0 * s1, 3), 0.0]
        tr_idle["back_chain_mid"]["rotation"][key] = [round(4.0 * s2, 3), 0.0, round(2.0 * s1, 3)]
        tr_idle["shield_chain"]["rotation"][key] = [round(5.0 * s1, 3), 0.0, round(4.0 * c1, 3)]
        tr_idle["skirt_front"]["rotation"][key] = [round(-3.0 - 2.0 * s2, 3), 0.0, 0.0]
        tr_idle["skirt_back"]["rotation"][key] = [round(3.0 + 2.0 * s2, 3), 0.0, 0.0]

    clips.append({
        "id": "animation.kharvok.idle",
        "kind": "idle",
        "duration": dur_idle,
        "loop": "loop",
        "tracks": {b: {k: v for k, v in ch.items() if v} for b, ch in tr_idle.items()},
    })

    # 2. Stride (1.2s heavy foundry march loop)
    dur_stride = 1.2
    tr_str = {b: {"rotation": {}, "position": {}} for b in (
        "pelvis", "thigh_right", "thigh_left", "shin_right", "shin_left", "foot_right", "foot_left",
        "waist", "chest", "head", "upper_arm_right", "forearm_right", "upper_arm_left", "forearm_left",
        "anvil_maul", "back_chain_mid", "shield_chain", "skirt_front", "skirt_back"
    )}
    for t in _sample_key(dur_stride):
        is_end = abs(t - dur_stride) < 1e-4
        ph = 0.0 if (t == 0.0 or is_end) else 2.0 * math.pi * (t / dur_stride)
        key = "0.0" if t == 0.0 else (f"{dur_stride}" if is_end else f"{t:.2f}".rstrip("0").rstrip("."))
        s1, c1 = math.sin(ph), math.cos(ph)
        s2 = math.sin(2.0 * ph)
        tr_str["pelvis"]["position"][key] = [round(1.1 * s1, 3), round(-0.6 + 0.8 * abs(s1), 3), round(-1.2 * abs(c1), 3)]
        tr_str["pelvis"]["rotation"][key] = [round(4.0, 3), round(7.0 * c1, 3), round(3.0 * s1, 3)]
        tr_str["thigh_right"]["rotation"][key] = [round(26.0 * s1, 3), 0.0, 0.0]
        tr_str["thigh_left"]["rotation"][key] = [round(-26.0 * s1, 3), 0.0, 0.0]
        tr_str["shin_right"]["rotation"][key] = [round(-10.0 + 18.0 * max(0.0, -c1), 3), 0.0, 0.0]
        tr_str["shin_left"]["rotation"][key] = [round(-10.0 + 18.0 * max(0.0, c1), 3), 0.0, 0.0]
        tr_str["foot_right"]["rotation"][key] = [round(-10.0 * s1, 3), 0.0, 0.0]
        tr_str["foot_left"]["rotation"][key] = [round(10.0 * s1, 3), 0.0, 0.0]
        tr_str["waist"]["rotation"][key] = [round(3.0 * s2, 3), round(-6.0 * c1, 3), 0.0]
        tr_str["chest"]["rotation"][key] = [round(6.0 + 3.0 * s2, 3), round(-9.0 * c1, 3), round(-2.5 * s1, 3)]
        tr_str["head"]["rotation"][key] = [round(-6.0, 3), round(7.0 * c1, 3), 0.0]
        tr_str["upper_arm_right"]["rotation"][key] = [round(-14.0 - 20.0 * s1, 3), 0.0, round(-6.0, 3)]
        tr_str["forearm_right"]["rotation"][key] = [round(-18.0 - 8.0 * c1, 3), 0.0, 0.0]
        tr_str["upper_arm_left"]["rotation"][key] = [round(-12.0 + 22.0 * s1, 3), 0.0, round(6.0, 3)]
        tr_str["forearm_left"]["rotation"][key] = [round(-22.0 + 8.0 * c1, 3), 0.0, 0.0]
        tr_str["anvil_maul"]["rotation"][key] = [round(-6.0 * c1, 3), 0.0, 0.0]
        tr_str["back_chain_mid"]["rotation"][key] = [round(8.0 + 6.0 * s2, 3), round(4.0 * s1, 3), 0.0]
        tr_str["shield_chain"]["rotation"][key] = [round(10.0 * c1, 3), 0.0, round(6.0 * s1, 3)]
        tr_str["skirt_front"]["rotation"][key] = [round(-8.0 - 8.0 * abs(s1), 3), 0.0, 0.0]
        tr_str["skirt_back"]["rotation"][key] = [round(8.0 + 8.0 * abs(s1), 3), 0.0, 0.0]

    clips.append({
        "id": "animation.kharvok.stride",
        "kind": "walk",
        "duration": dur_stride,
        "loop": "loop",
        "contralateral_pairs": [["thigh_right", "thigh_left"], ["upper_arm_right", "upper_arm_left"]],
        "tracks": {b: {k: v for k, v in ch.items() if v} for b, ch in tr_str.items()},
    })

    # Helper for interpolation across beat tables
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

    # 3. Anvil Shatter (1.55s = 31 ticks): windup torso coil -> overhead forward slam (-Z) -> ground shockwave -> recover
    dur_shatter = 1.55
    clips.append({
        "id": "animation.kharvok.anvil_shatter",
        "kind": "attack",
        "duration": dur_shatter,
        "loop": "play_once",
        "beats": {"anticipation": 0.45, "strike": 0.70, "impact": 0.85, "recovery": 1.40},
        "tracks": bake_keyframe_table(dur_shatter, {
            "pelvis": [
                (0.0, [0, 0, 0], [0, 0, 0]),
                (0.45, [6, -22, 0], [0, -1.2, 2.0]),
                (0.70, [-8, 14, 0], [0, 0.5, -2.5]),
                (0.85, [-12, 18, 0], [0, -3.2, -5.0]),
                (1.15, [-9, 12, 0], [0, -2.5, -3.5]),
                (1.55, [0, 0, 0], [0, 0, 0]),
            ],
            "thigh_right": [(0.0, [0, 0, 0], None), (0.45, [-12, 0, 0], None), (0.85, [28, 0, 0], None), (1.55, [0, 0, 0], None)],
            "shin_right": [(0.0, [0, 0, 0], None), (0.45, [-16, 0, 0], None), (0.85, [-26, 0, 0], None), (1.55, [0, 0, 0], None)],
            "thigh_left": [(0.0, [0, 0, 0], None), (0.45, [14, 0, 0], None), (0.85, [-20, 0, 0], None), (1.55, [0, 0, 0], None)],
            "waist": [(0.0, [0, 0, 0], None), (0.45, [8, -20, 0], None), (0.70, [-10, 16, 0], None), (0.85, [-14, 14, 0], None), (1.55, [0, 0, 0], None)],
            "chest": [(0.0, [0, 0, 0], None), (0.45, [10, -24, 0], None), (0.70, [-12, 22, 0], None), (0.85, [-16, 18, 0], None), (1.55, [0, 0, 0], None)],
            "head": [(0.0, [0, 0, 0], None), (0.45, [-12, 30, 0], None), (0.85, [18, -14, 0], None), (1.55, [0, 0, 0], None)],
            "upper_arm_right": [
                (0.0, [0, 0, 0], None), (0.45, [-142, 0, -18], None), (0.70, [110, 0, -6], None),
                (0.85, [76, 0, 10], None), (1.15, [68, 0, 8], None), (1.55, [0, 0, 0], None),
            ],
            "forearm_right": [(0.0, [-10, 0, 0], None), (0.45, [-34, 0, 0], None), (0.85, [14, 0, 0], None), (1.55, [-10, 0, 0], None)],
            "anvil_maul": [(0.0, [0, 0, 0], None), (0.45, [-20, 0, 0], None), (0.85, [28, 0, 0], None), (1.15, [26, 0, 0], None), (1.55, [0, 0, 0], None)],
            "upper_arm_left": [(0.0, [0, 0, 0], None), (0.45, [38, 18, 20], None), (0.85, [-32, -12, 18], None), (1.55, [0, 0, 0], None)],
            "forearm_left": [(0.0, [-15, 0, 0], None), (0.45, [24, 0, 0], None), (0.85, [18, 0, 0], None), (1.55, [-15, 0, 0], None)],
            "back_chain_mid": [(0.0, [0, 0, 0], None), (0.45, [-14, 0, 0], None), (0.85, [20, 0, 0], None), (1.55, [0, 0, 0], None)],
        }),
    })

    # 4. Chain Shackle (2.00s = 40 ticks): brace & sweep shield forward -> lock chains -> wrench back
    dur_shackle = 2.00
    clips.append({
        "id": "animation.kharvok.chain_shackle",
        "kind": "attack",
        "duration": dur_shackle,
        "loop": "play_once",
        "beats": {"anticipation": 0.45, "strike": 0.90, "impact": 1.25, "recovery": 1.80},
        "tracks": bake_keyframe_table(dur_shackle, {
            "pelvis": [(0.0, [0, 0, 0], [0, 0, 0]), (0.45, [-8, 18, 0], [0, -2.0, 1.5]), (1.25, [8, -14, 0], [0, -1.0, -3.0]), (2.00, [0, 0, 0], [0, 0, 0])],
            "waist": [(0.0, [0, 0, 0], None), (0.45, [-6, 18, 0], None), (1.25, [8, -16, 0], None), (2.00, [0, 0, 0], None)],
            "chest": [(0.0, [0, 0, 0], None), (0.45, [-8, 22, 0], None), (0.90, [-14, -22, 0], None), (1.25, [10, -24, 0], None), (2.00, [0, 0, 0], None)],
            "head": [(0.0, [0, 0, 0], None), (0.45, [8, 0, 0], None), (1.25, [14, 14, 0], None), (2.00, [0, 0, 0], None)],
            "upper_arm_left": [(0.0, [0, 0, 0], None), (0.45, [28, -22, 24], None), (0.90, [84, 14, 10], None), (1.25, [68, 20, 8], None), (2.00, [0, 0, 0], None)],
            "forearm_left": [(0.0, [-15, 0, 0], None), (0.90, [16, 0, 0], None), (1.25, [32, 0, 0], None), (2.00, [-15, 0, 0], None)],
            "forge_shield": [(0.0, [0, 0, 0], None), (0.90, [0, 52, 0], None), (1.25, [0, 44, 0], None), (2.00, [0, 0, 0], None)],
            "shield_chain": [(0.0, [0, 0, 0], None), (0.90, [24, 0, -18], None), (1.25, [-20, 0, 14], None), (2.00, [0, 0, 0], None)],
            "upper_arm_right": [(0.0, [0, 0, 0], None), (0.45, [-26, 0, -24], None), (0.90, [-52, 0, -34], None), (2.00, [0, 0, 0], None)],
            "stack_right": [(0.0, [0, 0, 0], None), (0.90, [-8, 0, 10], None), (2.00, [0, 0, 0], None)],
            "stack_left": [(0.0, [0, 0, 0], None), (0.90, [-8, 0, -10], None), (2.00, [0, 0, 0], None)],
        }),
    })

    # 5. Crucible Meltdown (2.90s = 58 ticks): deep crouch charge -> skyward roar -> twin-arm ground slam -> recovery
    dur_melt = 2.90
    clips.append({
        "id": "animation.kharvok.crucible_meltdown",
        "kind": "attack",
        "duration": dur_melt,
        "loop": "play_once",
        "beats": {"anticipation": 0.70, "strike": 1.95, "impact": 2.15, "recovery": 2.75},
        "tracks": bake_keyframe_table(dur_melt, {
            "pelvis": [(0.0, [0, 0, 0], [0, 0, 0]), (0.70, [-10, 0, 0], [0, -3.5, 0]), (1.60, [8, 0, 0], [0, 1.2, -1]), (2.15, [-14, 0, 0], [0, -4.0, -4]), (2.90, [0, 0, 0], [0, 0, 0])],
            "thigh_right": [(0.0, [0, 0, 0], None), (0.70, [26, 0, -8], None), (1.60, [-8, 0, 0], None), (2.15, [28, 0, 0], None), (2.90, [0, 0, 0], None)],
            "shin_right": [(0.0, [0, 0, 0], None), (0.70, [-36, 0, 0], None), (1.60, [10, 0, 0], None), (2.15, [-38, 0, 0], None), (2.90, [0, 0, 0], None)],
            "thigh_left": [(0.0, [0, 0, 0], None), (0.70, [26, 0, 8], None), (1.60, [-8, 0, 0], None), (2.15, [28, 0, 0], None), (2.90, [0, 0, 0], None)],
            "shin_left": [(0.0, [0, 0, 0], None), (0.70, [-36, 0, 0], None), (1.60, [10, 0, 0], None), (2.15, [-38, 0, 0], None), (2.90, [0, 0, 0], None)],
            "waist": [(0.0, [0, 0, 0], None), (0.70, [-12, 0, 0], None), (1.60, [10, 0, 0], None), (2.15, [-14, 0, 0], None), (2.90, [0, 0, 0], None)],
            "chest": [(0.0, [0, 0, 0], None), (0.70, [-14, 0, 0], None), (1.60, [12, 0, 0], None), (2.15, [-16, 0, 0], None), (2.90, [0, 0, 0], None)],
            "head": [(0.0, [0, 0, 0], None), (0.70, [18, 0, 0], None), (1.60, [18, 0, 0], None), (2.15, [20, 0, 0], None), (2.90, [0, 0, 0], None)],
            "upper_arm_right": [(0.0, [0, 0, 0], None), (0.70, [-18, 0, -26], None), (1.60, [-138, 0, -24], None), (2.15, [72, 0, 10], None), (2.90, [0, 0, 0], None)],
            "upper_arm_left": [(0.0, [0, 0, 0], None), (0.70, [-18, 0, 26], None), (1.60, [-132, 0, 24], None), (2.15, [72, 0, -8], None), (2.90, [0, 0, 0], None)],
            "anvil_maul": [(0.0, [0, 0, 0], None), (1.60, [-20, 0, 0], None), (2.15, [26, 0, 0], None), (2.90, [0, 0, 0], None)],
            "back_chain_mid": [(0.0, [0, 0, 0], None), (1.60, [18, 0, 0], None), (2.15, [-20, 0, 0], None), (2.90, [0, 0, 0], None)],
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


def paint_region(img_arr, x0, y0, w, h, mat, face_name, cid):
    palettes = {
        "basalt_iron": ((34, 36, 44), (58, 62, 76), (20, 22, 28), (82, 86, 102)),
        "charred_steel": ((52, 54, 66), (84, 88, 104), (30, 32, 40), (118, 122, 140)),
        "foundry_bronze": ((166, 114, 52), (218, 166, 86), (98, 62, 24), (242, 202, 124)),
        "slag_chain": ((72, 76, 88), (108, 114, 130), (40, 42, 50), (148, 156, 174)),
        "forge_ember_emissive": ((248, 116, 22), (255, 194, 68), (178, 52, 12), (255, 242, 172)),
    }
    base, hi, dark, accent = palettes[mat]
    seed = abs(hash((cid, face_name))) & 0xFFFF
    for dy in range(h):
        for dx in range(w):
            px, py = x0 + dx, y0 + dy
            if px >= img_arr.shape[1] or py >= img_arr.shape[0]:
                continue
            r = deterministic_noise(dx, dy, seed)
            col = list(base)
            if face_name == "up":
                col = [int(base[i] * 0.65 + hi[i] * 0.35) for i in range(3)]
            elif face_name == "down":
                col = [int(base[i] * 0.6 + dark[i] * 0.4) for i in range(3)]
            elif dy == 0:
                col = [int(base[i] * 0.65 + hi[i] * 0.35) for i in range(3)]
            is_border = (dx == 0 or dy == 0 or dx == w - 1 or dy == h - 1) and w >= 3 and h >= 3
            if is_border and mat != "forge_ember_emissive":
                col = [int(col[i] * 0.6 + dark[i] * 0.4) for i in range(3)]
            elif mat == "forge_ember_emissive":
                cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
                dist = max(abs(dx - cx) / max(w / 2.0, 1.0), abs(dy - cy) / max(h / 2.0, 1.0))
                col = list(accent if dist < 0.4 else (hi if dist < 0.78 else base))
            elif mat == "foundry_bronze":
                if (dx + dy) % 5 == 0 and r > 0.55:
                    col = list(accent)
                elif r < 0.22:
                    col = list(dark)
            elif r > 0.88:
                col = [int(col[i] * 0.88 + hi[i] * 0.12) for i in range(3)]
            elif r < 0.14:
                col = [int(col[i] * 0.88 + dark[i] * 0.12) for i in range(3)]
            img_arr[py, px] = [col[0], col[1], col[2], 255]


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
        is_emis = "emissive" in mat
        face_uvs = {}
        for fname, (fw, fh) in {
            "north": (dx, dy), "south": (dx, dy),
            "east": (dz, dy), "west": (dz, dy),
            "up": (dx, dz), "down": (dx, dz),
        }.items():
            ux, vy, rw, rh = alloc(fw, fh)
            paint_region(diffuse_arr, ux, vy, rw, rh, mat, fname, c["id"])
            if is_emis:
                paint_region(glow_arr, ux, vy, rw, rh, mat, fname, c["id"])
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
            "selected": cid == "animation.kharvok.anvil_shatter",
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
