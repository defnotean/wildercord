#!/usr/bin/env python3
"""Build 40-bone bendable Vaelkhor rig & 20 TPS biomechanical/physics animations (exact 1:1 bind pose preserved)."""
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
VALIDATOR_CREATURE = Path("C:/Users/Eating/.gemini/config/skills/minecraft-creature-builder/scripts/validate_creature_manifest.py")
VALIDATOR_ANIM = Path("C:/Users/Eating/.gemini/config/skills/minecraft-creature-animator/scripts/validate_animation_manifest.py")

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
    # 40 bones: preserves 100% identical bind-pose cuboid coordinates while adding S-spine, 2-joint wings,
    # double-pendulum chain->lantern_cage, independent cloth stoles, and 4-segment tail whip!
    bones = [
        {"id": "root", "parent": None, "pivot": [0, 0, 0]},
        {"id": "pelvis", "parent": "root", "pivot": [0, 26.5, -1.0]},
        {"id": "skirt_front", "parent": "pelvis", "pivot": [0, 26.0, -4.5]},
        {"id": "skirt_back", "parent": "pelvis", "pivot": [0, 26.0, 2.5]},
        {"id": "thigh_right", "parent": "pelvis", "pivot": [4.5, 25.0, -1.0], "mirror_of": "thigh_left"},
        {"id": "thigh_left", "parent": "pelvis", "pivot": [-4.5, 25.0, -1.0], "mirror_of": "thigh_right"},
        {"id": "crus_right", "parent": "thigh_right", "pivot": [4.5, 14.8, -3.5], "mirror_of": "crus_left"},
        {"id": "crus_left", "parent": "thigh_left", "pivot": [-4.5, 14.8, -3.5], "mirror_of": "crus_right"},
        {"id": "shin_right", "parent": "crus_right", "pivot": [4.5, 8.7, 2.0], "mirror_of": "shin_left"},
        {"id": "shin_left", "parent": "crus_left", "pivot": [-4.5, 8.7, 2.0], "mirror_of": "shin_right"},
        {"id": "foot_right", "parent": "shin_right", "pivot": [4.5, 2.0, -0.5], "mirror_of": "foot_left"},
        {"id": "foot_left", "parent": "shin_left", "pivot": [-4.5, 2.0, -0.5], "mirror_of": "foot_right"},
        # 4-segment whip tail
        {"id": "tail_0", "parent": "pelvis", "pivot": [0, 23.5, 3.0]},
        {"id": "tail_1", "parent": "tail_0", "pivot": [0, 16.5, 8.5]},
        {"id": "tail_2", "parent": "tail_1", "pivot": [0, 9.8, 13.5]},
        {"id": "tail_3", "parent": "tail_2", "pivot": [0, 7.2, 15.5]},
        # S-curve torso: pelvis -> waist -> chest
        {"id": "waist", "parent": "pelvis", "pivot": [0, 28.5, 0.2]},
        {"id": "chest", "parent": "waist", "pivot": [0, 34.0, -0.2]},
        {"id": "core_crystal", "parent": "chest", "pivot": [0, 36.2, -4.2]},
        {"id": "stole_right", "parent": "chest", "pivot": [5.0, 43.8, -6.6], "mirror_of": "stole_left"},
        {"id": "stole_left", "parent": "chest", "pivot": [-5.0, 43.8, -6.6], "mirror_of": "stole_right"},
        # 2-joint billowing mechanical pinion wings
        {"id": "wing_ring_right", "parent": "chest", "pivot": [2.8, 40.5, 4.2], "mirror_of": "wing_ring_left"},
        {"id": "wing_ring_left", "parent": "chest", "pivot": [-2.8, 40.5, 4.2], "mirror_of": "wing_ring_right"},
        {"id": "wing_outer_right", "parent": "wing_ring_right", "pivot": [12.0, 44.2, 7.8], "mirror_of": "wing_outer_left"},
        {"id": "wing_outer_left", "parent": "wing_ring_left", "pivot": [-12.0, 44.2, 7.8], "mirror_of": "wing_outer_right"},
        # Upper weapon arms + double-pendulum chain -> lantern_cage
        {"id": "upper_arm_right", "parent": "chest", "pivot": [7.5, 42.2, -0.8], "mirror_of": "upper_arm_left"},
        {"id": "upper_arm_left", "parent": "chest", "pivot": [-7.5, 42.2, -0.8], "mirror_of": "upper_arm_right"},
        {"id": "upper_forearm_right", "parent": "upper_arm_right", "pivot": [12.0, 35.0, -1.4], "mirror_of": "upper_forearm_left"},
        {"id": "upper_forearm_left", "parent": "upper_arm_left", "pivot": [-12.0, 35.0, -1.4], "mirror_of": "upper_forearm_right"},
        {"id": "halberd", "parent": "upper_forearm_right", "pivot": [16.5, 33.4, -4.0]},
        {"id": "bell_lantern", "parent": "upper_forearm_left", "pivot": [-16.5, 32.2, -4.0]},
        {"id": "lantern_cage", "parent": "bell_lantern", "pivot": [-16.5, 24.4, -4.0]},
        # Forward-reaching lower weaver arms
        {"id": "lower_arm_right", "parent": "chest", "pivot": [5.8, 35.0, -1.0], "mirror_of": "lower_arm_left"},
        {"id": "lower_arm_left", "parent": "chest", "pivot": [-5.8, 35.0, -1.0], "mirror_of": "lower_arm_right"},
        {"id": "lower_forearm_right", "parent": "lower_arm_right", "pivot": [9.2, 29.6, -3.2], "mirror_of": "lower_forearm_left"},
        {"id": "lower_forearm_left", "parent": "lower_arm_left", "pivot": [-9.2, 29.6, -3.2], "mirror_of": "lower_forearm_right"},
        # Neck -> Skull -> Jaw & Concentric Bronze Halo
        {"id": "neck", "parent": "chest", "pivot": [0, 44.0, 0.0]},
        {"id": "head", "parent": "neck", "pivot": [0, 49.5, -3.5]},
        {"id": "jaw", "parent": "head", "pivot": [0, 48.0, -5.2]},
        {"id": "halo_ring", "parent": "head", "pivot": [0, 55.0, 4.2]},
    ]

    cubes = []

    # 1. PELVIS & TORN INDIGO KILT
    cubes += [
        make_cube("pelvis_rim_back", "pelvis", "gilded_bronze", [-5.4, 26.8, 1.0], [5.4, 29.0, 3.0]),
        make_cube("pelvis_rim_front", "pelvis", "gilded_bronze", [-5.4, 26.5, -5.0], [5.4, 28.8, -3.2]),
        make_cube("pelvis_rim_r", "pelvis", "gilded_bronze", [3.6, 26.6, -3.8], [5.4, 28.8, 1.6]),
        make_cube("pelvis_rim_l", "pelvis", "gilded_bronze", [-5.4, 26.6, -3.8], [-3.6, 28.8, 1.6]),
        make_cube("pelvis_basin_core", "pelvis", "obsidian_armor", [-3.8, 24.5, -3.4], [3.8, 27.6, 2.2]),
        make_cube("pelvis_buckle_green", "pelvis", "verdigris_copper", [-1.2, 26.2, -5.3], [1.2, 28.4, -4.4], inflate=0.06),
        make_cube("skirt_front_center", "skirt_front", "royal_vestment", [-2.0, 15.2, -5.5], [2.0, 26.5, -4.3], inflate=0.05),
        make_cube("skirt_back_cloth", "skirt_back", "royal_vestment", [-5.2, 16.5, 1.8], [5.2, 26.5, 3.0]),
    ]
    cubes += mirror_cube("pelvis_plate_green_r", "pelvis_plate_green_l", "pelvis", "pelvis", "verdigris_copper",
                         [1.5, 24.6, -4.6], [4.8, 26.8, -2.6])
    cubes += mirror_cube("skirt_front_side_r", "skirt_front_side_l", "skirt_front", "skirt_front", "royal_vestment",
                         [1.8, 18.0, -5.2], [5.5, 26.4, -3.4])
    cubes += mirror_cube("skirt_hip_flare_r", "skirt_hip_flare_l", "skirt_front", "skirt_front", "royal_vestment",
                         [4.6, 17.8, -4.4], [6.4, 26.2, 2.2], rotation=[0, 0, -6], origin=[5.2, 26.0, -1.0])

    # 2. 3-SEGMENT DIGITIGRADE Z-LEGS (Y = 0)
    cubes += mirror_cube("thigh_r", "thigh_l", "thigh_right", "thigh_left", "obsidian_armor",
                         [2.8, 14.6, -2.8], [6.2, 25.5, 0.8], rotation=[14, 0, 0], origin=[4.5, 25.0, -1.0])
    cubes += mirror_cube("knee_cap_r", "knee_cap_l", "crus_right", "crus_left", "verdigris_copper",
                         [2.4, 12.6, -5.6], [6.6, 17.0, -1.6], inflate=0.08, rotation=[-18, 0, 0], origin=[4.5, 14.8, -3.5])
    cubes += mirror_cube("crus_r", "crus_l", "crus_right", "crus_left", "obsidian_armor",
                         [2.9, 6.6, -5.1], [6.1, 15.2, -1.9], rotation=[-42, 0, 0], origin=[4.5, 14.8, -3.5])
    cubes += mirror_cube("hock_armor_r", "hock_armor_l", "shin_right", "shin_left", "verdigris_copper",
                         [2.6, 7.0, 0.2], [6.4, 10.6, 3.6], inflate=0.08, rotation=[18, 0, 0], origin=[4.5, 8.7, 2.0])
    cubes += mirror_cube("shin_r", "shin_l", "shin_right", "shin_left", "verdigris_copper",
                         [3.1, 1.5, 0.6], [5.9, 9.0, 3.4], rotation=[20, 0, 0], origin=[4.5, 8.7, 2.0])
    cubes += mirror_cube("shin_rib_r", "shin_rib_l", "shin_right", "shin_left", "gilded_bronze",
                         [3.6, 2.5, 0.2], [5.4, 8.0, 1.2], inflate=0.04, rotation=[20, 0, 0], origin=[4.5, 8.7, 2.0])
    cubes += mirror_cube("ankle_collar_r", "ankle_collar_l", "foot_right", "foot_left", "gilded_bronze",
                         [2.8, 1.5, -2.0], [6.2, 4.0, 1.4])
    cubes += mirror_cube("foot_bridge_r", "foot_bridge_l", "foot_right", "foot_left", "gilded_bronze",
                         [2.7, 0.0, -4.4], [6.3, 2.2, 1.6])
    cubes += mirror_cube("claw_in_r", "claw_in_l", "foot_right", "foot_left", "gilded_bronze",
                         [2.4, 0.0, -6.5], [3.6, 1.6, -4.2])
    cubes += mirror_cube("claw_mid_r", "claw_mid_l", "foot_right", "foot_left", "gilded_bronze",
                         [3.8, 0.0, -7.2], [5.2, 1.7, -4.2])
    cubes += mirror_cube("claw_out_r", "claw_out_l", "foot_right", "foot_left", "gilded_bronze",
                         [5.4, 0.0, -6.3], [6.6, 1.6, -4.2])
    cubes += mirror_cube("claw_heel_r", "claw_heel_l", "foot_right", "foot_left", "obsidian_armor",
                         [3.8, 0.0, 1.6], [5.2, 1.5, 3.2])

    # 3. 4-SEGMENT WHIP TAIL (tail_3 carries the pendulum crystal tip)
    cubes += [
        make_cube("tail_base_cap", "tail_0", "verdigris_copper", [-1.6, 22.8, 2.4], [1.6, 26.2, 4.8]),
        make_cube("tail_link0_bronze", "tail_0", "gilded_bronze", [-1.5, 19.2, 3.2], [1.5, 23.6, 6.2], rotation=[34, 0, 0], origin=[0, 23.5, 3.2]),
        make_cube("tail_link1_dark", "tail_0", "obsidian_armor", [-1.3, 15.8, 5.2], [1.3, 19.8, 8.2], rotation=[36, 0, 0], origin=[0, 20.0, 5.5]),
        make_cube("tail_link2_bronze", "tail_1", "gilded_bronze", [-1.4, 12.6, 7.6], [1.4, 16.6, 10.6], rotation=[38, 0, 0], origin=[0, 16.5, 8.5]),
        make_cube("tail_link3_dark", "tail_1", "obsidian_armor", [-1.2, 9.6, 10.4], [1.2, 13.4, 13.2], rotation=[42, 0, 0], origin=[0, 13.2, 10.8]),
        make_cube("tail_link4_bronze", "tail_2", "gilded_bronze", [-1.4, 6.6, 12.6], [1.4, 10.2, 15.6], rotation=[46, 0, 0], origin=[0, 9.8, 13.5]),
        make_cube("tail_crystal_cap", "tail_3", "gilded_bronze", [-1.9, 5.4, 14.6], [1.9, 8.6, 17.2], rotation=[48, 0, 0], origin=[0, 7.2, 15.5]),
        make_cube("tail_crystal_cyan", "tail_3", "leyline_emissive", [-1.7, 3.5, 16.2], [1.7, 7.2, 20.4], rotation=[48, 0, 0], origin=[0, 7.2, 15.5]),
    ]

    # 4. FLEXIBLE WAIST + OPEN 10-UNIT CHEST RIBCAGE + INDEPENDENT STOLES + CRYSTAL CORE
    cubes += [
        make_cube("waist_spine_low", "waist", "obsidian_armor", [-2.2, 28.0, -1.0], [2.2, 34.2, 2.2]),
        make_cube("back_spine_low", "waist", "gilded_bronze", [-1.3, 29.0, 2.0], [1.3, 34.2, 3.6]),
        make_cube("waist_spine_upper", "chest", "obsidian_armor", [-2.2, 34.0, -1.0], [2.2, 44.2, 2.2]),
        make_cube("back_spine_upper", "chest", "gilded_bronze", [-1.3, 34.0, 2.0], [1.3, 43.8, 3.6]),
        make_cube("collar_top_bronze", "chest", "gilded_bronze", [-6.2, 42.6, -6.2], [6.2, 44.2, 3.2]),
        make_cube("core_tip_vert", "core_crystal", "leyline_emissive", [-1.0, 31.2, -5.4], [1.0, 41.2, -3.2]),
        make_cube("core_mid_prism", "core_crystal", "leyline_emissive", [-1.9, 32.8, -6.1], [1.9, 39.6, -2.5], inflate=0.05),
        make_cube("core_belly_diamond", "core_crystal", "leyline_emissive", [-2.6, 34.6, -6.7], [2.6, 37.8, -1.9], inflate=0.08),
    ]
    for idx, ry in enumerate((41.8, 40.0, 38.2, 36.4, 34.6, 32.8)):
        w_side = 6.1 - max(0, idx - 1) * 0.35
        x_in_front = 1.8 if idx in (0, 5) else 2.5
        rib_bone = "waist" if idx == 5 else "chest"
        cubes += mirror_cube(f"rib_side_{idx}_r", f"rib_side_{idx}_l", rib_bone, rib_bone, "gilded_bronze",
                             [w_side - 1.2, ry, -6.4], [w_side, ry + 1.0, 2.8])
        cubes += mirror_cube(f"rib_front_{idx}_r", f"rib_front_{idx}_l", rib_bone, rib_bone, "gilded_bronze",
                             [x_in_front, ry, -6.6], [w_side - 0.8, ry + 1.0, -5.2])
        cubes += mirror_cube(f"rib_back_{idx}_r", f"rib_back_{idx}_l", rib_bone, rib_bone, "gilded_bronze",
                             [1.1, ry, 2.0], [w_side - 0.6, ry + 1.0, 3.2])

    cubes += mirror_cube("stole_front_r", "stole_front_l", "stole_right", "stole_left", "royal_vestment",
                         [3.6, 31.4, -7.4], [6.4, 44.2, -6.4], inflate=0.04)
    cubes += mirror_cube("stole_top_r", "stole_top_l", "chest", "chest", "royal_vestment",
                         [3.4, 43.6, -7.2], [6.6, 44.8, 3.8], inflate=0.06)
    cubes += [make_cube("stole_back_shawl", "chest", "royal_vestment", [-6.0, 41.2, 2.8], [6.0, 44.6, 4.0], inflate=0.05)]

    # 5. 2-JOINT ARTICULATED WINGS (wing_ring -> wing_outer)
    cubes += mirror_cube("wing_strut_r", "wing_strut_l", "wing_ring_right", "wing_ring_left", "verdigris_copper",
                         [1.5, 39.6, 3.2], [6.5, 42.2, 7.2])
    cubes += mirror_cube("wing_lead_dark_r", "wing_lead_dark_l", "wing_ring_right", "wing_ring_left", "obsidian_armor",
                         [9.2, 44.8, 6.4], [14.2, 47.6, 9.2], rotation=[0, 10, 26], origin=[9.2, 46.2, 7.8])
    cubes += mirror_cube("wing_lead_amber_r", "wing_lead_amber_l", "wing_outer_right", "wing_outer_left", "amber_emissive",
                         [13.4, 46.2, 6.5], [20.2, 49.4, 9.1], rotation=[0, 10, 24], origin=[13.4, 47.8, 7.8])
    arch_segs = [
        (3.5, 37.8, 7.8, 3.8, 12, False),
        (6.6, 39.2, 11.4, 3.6, 26, False),
        (9.6, 41.6, 14.0, 3.2, 42, False),
        (12.2, 44.8, 15.8, 2.8, 56, True),
    ]
    for idx, (ax0, ay0, ax1, ah, arz, is_outer) in enumerate(arch_segs):
        br = "wing_outer_right" if is_outer else "wing_ring_right"
        bl = "wing_outer_left" if is_outer else "wing_ring_left"
        cubes += mirror_cube(f"wing_arch_{idx}_r", f"wing_arch_{idx}_l", br, bl, "gilded_bronze",
                             [ax0, ay0, 7.0], [ax1, ay0 + ah * 0.55, 9.5], rotation=[0, 10, arz], origin=[ax0, ay0 + ah * 0.3, 8.2])
    feathers = [
        (9.8,  43.2, 18.2, 3.0, 18, True),
        (10.8, 41.0, 17.6, 2.8, 16, True),
        (10.6, 38.8, 16.4, 2.7, 14, False),
        (9.6,  36.6, 15.0, 2.6, 12, False),
        (8.6,  34.6, 13.4, 2.5, 10, False),
    ]
    for idx, (fx0, fy0, fx1, fh, frz, is_outer) in enumerate(feathers):
        br = "wing_outer_right" if is_outer else "wing_ring_right"
        bl = "wing_outer_left" if is_outer else "wing_ring_left"
        cubes += mirror_cube(f"wing_feather_{idx}_r", f"wing_feather_{idx}_l", br, bl, "verdigris_copper",
                             [fx0, fy0, 6.6], [fx1, fy0 + fh, 9.1], rotation=[0, 10, frz], origin=[fx0, fy0 + fh * 0.5, 7.8])

    # 6. FOUR ARMS + HALBERD + DOUBLE-PENDULUM CHAIN -> LANTERN_CAGE
    cubes += mirror_cube("shoulder_pauldron_r", "shoulder_pauldron_l", "chest", "chest", "gilded_bronze",
                         [5.6, 40.4, -3.8], [9.4, 43.8, 1.8], rotation=[0, 0, -16], origin=[7.2, 42.2, -1.0])
    cubes += mirror_cube("u_arm_r", "u_arm_l", "upper_arm_right", "upper_arm_left", "obsidian_armor",
                         [6.2, 34.0, -2.5], [9.4, 42.8, 0.7], rotation=[0, 0, -34], origin=[7.5, 42.2, -0.8])
    cubes += mirror_cube("u_fore_r", "u_fore_l", "upper_forearm_right", "upper_forearm_left", "verdigris_copper",
                         [11.4, 33.2, -3.2], [17.4, 36.2, -0.2], rotation=[0, 26, -12], origin=[12.0, 35.0, -1.4])
    cubes += mirror_cube("u_hand_r", "u_hand_l", "upper_forearm_right", "upper_forearm_left", "obsidian_armor",
                         [15.0, 31.8, -5.5], [18.0, 34.8, -2.5])

    cubes += [
        make_cube("halberd_shaft", "halberd", "obsidian_armor", [15.6, 7.5, -4.9], [17.4, 54.0, -3.1]),
        make_cube("halberd_butt_collar", "halberd", "gilded_bronze", [15.3, 6.8, -5.2], [17.7, 9.0, -2.8]),
        make_cube("halberd_butt_spear", "halberd", "leyline_emissive", [15.8, 0.2, -4.7], [17.2, 7.0, -3.3]),
        make_cube("halberd_ring_mid", "halberd", "gilded_bronze", [15.3, 44.2, -5.2], [17.7, 46.0, -2.8]),
        make_cube("halberd_socket", "halberd", "obsidian_armor", [15.5, 44.8, -5.0], [18.4, 52.8, -3.0]),
        make_cube("halberd_blade_u2", "halberd", "leyline_emissive", [16.5, 57.5, -4.6], [18.2, 62.5, -3.4], rotation=[0, 0, 22]),
        make_cube("halberd_blade_u1", "halberd", "leyline_emissive", [17.6, 52.2, -4.6], [20.4, 58.2, -3.4], rotation=[0, 0, 12]),
        make_cube("halberd_blade_mid", "halberd", "leyline_emissive", [18.2, 46.2, -4.6], [21.6, 52.6, -3.4]),
        make_cube("halberd_blade_d1", "halberd", "leyline_emissive", [17.6, 40.8, -4.6], [20.4, 46.6, -3.4], rotation=[0, 0, -14]),
        make_cube("halberd_blade_d2", "halberd", "leyline_emissive", [16.5, 36.8, -4.6], [18.2, 41.2, -3.4], rotation=[0, 0, -26]),
    ]

    for idx in range(4):
        cy0 = 24.4 + idx * 2.1
        cubes.append(make_cube(f"lantern_chain_{idx}", "bell_lantern", "obsidian_armor",
                               [-17.1, cy0, -4.5], [-15.9, cy0 + 2.4, -3.5],
                               rotation=[0, 45 if idx % 2 else 0, 0]))
    cubes += [
        make_cube("lantern_finial", "lantern_cage", "gilded_bronze", [-17.4, 23.6, -4.9], [-15.6, 25.0, -3.1]),
        make_cube("lantern_roof_upper", "lantern_cage", "gilded_bronze", [-18.4, 22.4, -5.9], [-14.6, 23.8, -2.1]),
        make_cube("lantern_roof_eaves", "lantern_cage", "gilded_bronze", [-19.6, 21.2, -7.1], [-13.4, 22.5, -0.9]),
        make_cube("lantern_bar_fr", "lantern_cage", "gilded_bronze", [-14.4, 15.8, -6.8], [-13.6, 21.3, -6.0]),
        make_cube("lantern_bar_fl", "lantern_cage", "gilded_bronze", [-19.4, 15.8, -6.8], [-18.6, 21.3, -6.0]),
        make_cube("lantern_bar_br", "lantern_cage", "gilded_bronze", [-14.4, 15.8, -2.0], [-13.6, 21.3, -1.2]),
        make_cube("lantern_bar_bl", "lantern_cage", "gilded_bronze", [-19.4, 15.8, -2.0], [-18.6, 21.3, -1.2]),
        make_cube("lantern_base_plate", "lantern_cage", "gilded_bronze", [-19.6, 14.6, -7.1], [-13.4, 16.0, -0.9]),
        make_cube("lantern_base_nub", "lantern_cage", "gilded_bronze", [-17.6, 13.8, -5.1], [-15.4, 14.8, -2.9]),
        make_cube("lantern_flame_body", "lantern_cage", "leyline_emissive", [-17.6, 16.6, -5.1], [-15.4, 20.2, -2.9]),
        make_cube("lantern_flame_tip", "lantern_cage", "leyline_emissive", [-17.1, 20.0, -4.6], [-15.9, 21.2, -3.4]),
    ]

    cubes += mirror_cube("l_arm_r", "l_arm_l", "lower_arm_right", "lower_arm_left", "obsidian_armor",
                         [4.6, 28.8, -2.3], [7.2, 35.4, 0.3], rotation=[22, 0, -32], origin=[5.8, 35.0, -1.0])
    cubes += mirror_cube("l_fore_r", "l_fore_l", "lower_forearm_right", "lower_forearm_left", "verdigris_copper",
                         [8.0, 28.2, -11.4], [10.6, 30.8, -2.8], rotation=[24, -12, 0], origin=[9.2, 29.6, -3.2])
    cubes += mirror_cube("l_hand_r", "l_hand_l", "lower_forearm_right", "lower_forearm_left", "obsidian_armor",
                         [7.9, 28.1, -13.6], [10.7, 30.9, -11.0], rotation=[24, -12, 0], origin=[9.2, 29.6, -3.2])

    # 7. NECK, STAG SKULL, ANTLERS & DOUBLE BRONZE HALO
    cubes += [
        make_cube("neck_col", "neck", "obsidian_armor", [-2.6, 43.2, -2.2], [2.6, 51.6, 2.8], rotation=[-18, 0, 0], origin=[0, 44.0, 0.0]),
        make_cube("skull_cranium", "head", "bone_ivory", [-3.0, 49.4, -8.0], [3.0, 53.8, -2.2], rotation=[16, 0, 0], origin=[0, 50.0, -3.5]),
        make_cube("skull_cheek_shadow", "head", "obsidian_armor", [-3.1, 48.8, -7.8], [3.1, 51.4, -2.4], inflate=-0.05, rotation=[16, 0, 0], origin=[0, 50.0, -3.5]),
        make_cube("stag_snout_bridge", "head", "bone_ivory", [-2.1, 47.4, -13.4], [2.1, 51.2, -7.4], rotation=[18, 0, 0], origin=[0, 49.5, -7.4]),
        make_cube("stag_teeth_rim", "head", "obsidian_armor", [-1.9, 46.8, -13.0], [1.9, 47.8, -7.5], rotation=[18, 0, 0], origin=[0, 49.5, -7.4]),
        make_cube("stag_nostrils_tip", "head", "obsidian_armor", [-1.6, 46.4, -14.2], [1.6, 48.6, -12.6], rotation=[18, 0, 0], origin=[0, 49.5, -7.4]),
        make_cube("jaw_lower", "jaw", "bone_ivory", [-1.8, 44.6, -12.8], [1.8, 46.6, -5.0], rotation=[14, 0, 0], origin=[0, 48.0, -5.2]),
    ]
    cubes += mirror_cube("eye_glow_r", "eye_glow_l", "head", "head", "leyline_emissive",
                         [1.7, 50.1, -8.6], [3.2, 51.5, -6.6], rotation=[16, 0, 0], origin=[0, 50.0, -3.5])
    cubes += mirror_cube("antler_stem_r", "antler_stem_l", "head", "head", "gilded_bronze",
                         [1.8, 53.0, -5.2], [3.2, 56.6, -3.8], rotation=[26, 0, -28], origin=[2.4, 53.2, -4.5])
    cubes += mirror_cube("antler_sweep_bar_r", "antler_sweep_bar_l", "head", "head", "gilded_bronze",
                         [3.6, 55.2, -3.6], [10.8, 56.6, -2.2], rotation=[0, 22, -6], origin=[3.6, 55.8, -3.0])
    cubes += mirror_cube("antler_brow_fork_r", "antler_brow_fork_l", "head", "head", "gilded_bronze",
                         [6.0, 55.6, -5.6], [7.3, 59.2, -4.2], rotation=[-12, 0, 0], origin=[6.6, 55.8, -4.8])
    cubes += mirror_cube("antler_mid_tine_r", "antler_mid_tine_l", "head", "head", "gilded_bronze",
                         [8.2, 55.8, -2.4], [9.5, 61.8, -1.0])
    cubes += mirror_cube("antler_outer_beam_r", "antler_outer_beam_l", "head", "head", "gilded_bronze",
                         [10.2, 55.8, -1.8], [11.6, 64.0, -0.4])

    hx, hy, hz = 0.0, 55.0, 4.2
    for ring_name, radius, seg_w, seg_h, seg_d, mat in (
        ("outer", 7.6, 3.2, 0.95, 0.85, "gilded_bronze"),
        ("inner", 5.6, 2.4, 0.78, 0.65, "gilded_bronze"),
    ):
        for i in range(16):
            ang_deg = i * 22.5
            rad = math.radians(ang_deg)
            cx = hx + radius * math.cos(rad)
            cy = hy + radius * math.sin(rad)
            cubes.append(make_cube(
                f"halo_{ring_name}_{i}",
                "halo_ring",
                mat,
                [cx - seg_h * 0.5, cy - seg_w * 0.5, hz - seg_d * 0.5],
                [cx + seg_h * 0.5, cy + seg_w * 0.5, hz + seg_d * 0.5],
                rotation=[0, 0, ang_deg],
                origin=[cx, cy, hz],
            ))
    cubes += mirror_cube("halo_clamp_r", "halo_clamp_l", "halo_ring", "halo_ring", "gilded_bronze",
                         [5.0, 53.6, 3.5], [8.2, 56.4, 4.9])

    materials = [
        {"id": "obsidian_armor", "resolution": [ATLAS_W, ATLAS_H], "render_mode": "default"},
        {"id": "gilded_bronze", "resolution": [ATLAS_W, ATLAS_H], "render_mode": "default"},
        {"id": "verdigris_copper", "resolution": [ATLAS_W, ATLAS_H], "render_mode": "default"},
        {"id": "royal_vestment", "resolution": [ATLAS_W, ATLAS_H], "render_mode": "default"},
        {"id": "bone_ivory", "resolution": [ATLAS_W, ATLAS_H], "render_mode": "default"},
        {"id": "leyline_emissive", "resolution": [ATLAS_W, ATLAS_H], "render_mode": "emissive"},
        {"id": "amber_emissive", "resolution": [ATLAS_W, ATLAS_H], "render_mode": "emissive"},
    ]

    return {
        "schema_version": "1.0",
        "project": "Vaelkhor, the Leyline Sovereign",
        "archetype": "towering_seraphic_clockwork_stag_boss",
        "target_format": "bedrock",
        "pixels_per_block": 16,
        "ground_y": 0,
        "floating_entity": False,
        "hitbox_blocks": {"width": 2.2, "height": 4.0, "length": 2.0},
        "materials": materials,
        "bones": bones,
        "cubes": cubes,
    }


# ==============================================================================
# 3D EULER FK & REAL GRAVITY PENDULUM + DENSE 20 TPS BIOMECHANICS SYNTHESIZER
# ==============================================================================

def euler_zyx_mat(rx_deg: float, ry_deg: float, rz_deg: float) -> np.ndarray:
    """Exact Blockbench Bedrock Three.js Euler('ZYX') matrix: Mz @ My @ Mx."""
    rx, ry, rz = math.radians(rx_deg), math.radians(ry_deg), math.radians(rz_deg)
    cx, sx = math.cos(rx), math.sin(rx)
    cy, sy = math.cos(ry), math.sin(ry)
    cz, sz = math.cos(rz), math.sin(rz)
    mx = np.array([[1.0, 0.0, 0.0], [0.0, cx, -sx], [0.0, sx, cx]])
    my = np.array([[cy, 0.0, sy], [0.0, 1.0, 0.0], [-sy, 0.0, cy]])
    mz = np.array([[cz, -sz, 0.0], [sz, cz, 0.0], [0.0, 0.0, 1.0]])
    return mz @ my @ mx


def chain_world_mat(rot_map: dict, parent_chain: list[str]) -> np.ndarray:
    R = np.eye(3)
    for b in parent_chain:
        rx, ry, rz = rot_map.get(b, (0.0, 0.0, 0.0))
        R = R @ euler_zyx_mat(rx, ry, rz)
    return R


def mat_to_euler_zyx(R: np.ndarray) -> tuple[float, float, float]:
    """Decompose 3x3 rotation matrix R = Mz @ My @ Mx into degrees (rx, ry, rz)."""
    s_y = max(-0.995, min(0.995, float(-R[2, 0])))
    ry = math.asin(s_y)
    rx = math.atan2(float(R[2, 1]), float(R[2, 2]))
    rz = math.atan2(float(R[1, 0]), float(R[0, 0]))
    return (math.degrees(rx), math.degrees(ry), math.degrees(rz))


def align_neg_y_to_local_dir(v_local: np.ndarray, prev_deg=None) -> tuple[float, float, float]:
    """Solve Euler ZYX (rx, 0, rz) so local down axis (0, -1, 0) points along v_local without branch jumps."""
    n = np.linalg.norm(v_local)
    if n < 1e-6:
        return (0.0, 0.0, 0.0)
    vx, vy, vz = v_local / n
    # For R = Rz @ Rx applied to [0, -1, 0]: [sin(rz)*cos(rx), -cos(rz)*cos(rx), -sin(rx)]
    rx1 = math.asin(max(-0.985, min(0.985, float(-vz))))
    cos_rx = max(0.08, math.cos(rx1))
    rz1 = math.atan2(float(vx) / cos_rx, float(-vy) / cos_rx)
    rx2 = (math.copysign(math.pi, rx1) - rx1) if abs(rx1) > 1e-6 else math.pi
    rz2 = rz1 - math.copysign(math.pi, rz1)
    cands = [(math.degrees(rx1), 0.0, math.degrees(rz1)), (math.degrees(rx2), 0.0, math.degrees(rz2))]
    if prev_deg is not None:
        best = min(cands, key=lambda c: abs(_wrap180(c[0] - prev_deg[0])) + abs(_wrap180(c[2] - prev_deg[2])))
        return (
            prev_deg[0] + _wrap180(best[0] - prev_deg[0]),
            0.0,
            prev_deg[2] + _wrap180(best[2] - prev_deg[2]),
        )
    return min(cands, key=lambda c: abs(c[2]) + 0.5 * abs(c[0]))


_SOLVER_STATE = {"lantern": None, "halberd": None}


def _wrap180(a: float) -> float:
    while a > 180.0:
        a -= 360.0
    while a < -180.0:
        a += 360.0
    return a


def aim_halberd_world(rot_map: dict, parent_chain: list[str], blade_dir_w, x_hint_w=(1.0, 0.0, 0.0)):
    """Solve exact local ZYX Euler angles for `halberd` so +Y blade aims along blade_dir_w without branch jumps."""
    yw = np.array(blade_dir_w, dtype=float)
    yw /= max(1e-6, float(np.linalg.norm(yw)))
    xh = np.array(x_hint_w, dtype=float)
    zw = np.cross(xh, yw)
    n_zw = float(np.linalg.norm(zw))
    if n_zw < 1e-4:
        zw = np.cross(np.array([0.0, 0.0, -1.0]), yw)
        n_zw = float(np.linalg.norm(zw))
    zw /= max(1e-6, n_zw)
    xw = np.cross(yw, zw)
    R_target_w = np.column_stack([xw, yw, zw])
    R_par = chain_world_mat(rot_map, parent_chain)
    R_local = R_par.T @ R_target_w
    rx1, ry1, rz1 = mat_to_euler_zyx(R_local)
    rx2 = _wrap180(rx1 + 180.0)
    ry2 = _wrap180(math.copysign(180.0, ry1) - ry1)
    rz2 = _wrap180(rz1 + 180.0)
    cands = [[rx1, ry1, rz1], [rx2, ry2, rz2]]
    prev = _SOLVER_STATE["halberd"]
    if prev is not None:
        best = min(cands, key=lambda c: sum(abs(_wrap180(c[i] - prev[i])) for i in range(3)))
        res = [prev[i] + _wrap180(best[i] - prev[i]) for i in range(3)]
    else:
        res = min(cands, key=lambda c: sum(abs(c[i]) for i in range(3)))
    _SOLVER_STATE["halberd"] = list(res)
    return res


def slerp_dir(d0, d1, u: float):
    v0 = np.array(d0, dtype=float)
    v1 = np.array(d1, dtype=float)
    v0 /= max(1e-6, float(np.linalg.norm(v0)))
    v1 /= max(1e-6, float(np.linalg.norm(v1)))
    dot = max(-0.9999, min(0.9999, float(np.dot(v0, v1))))
    if dot > 0.9995:
        res = (1.0 - u) * v0 + u * v1
        return res / max(1e-6, float(np.linalg.norm(res)))
    omega = math.acos(dot)
    so = math.sin(omega)
    return (math.sin((1.0 - u) * omega) / so) * v0 + (math.sin(u * omega) / so) * v1


def smooth5(u: float) -> float:
    u = max(0.0, min(1.0, u))
    return u * u * u * (10.0 + u * (-15.0 + 6.0 * u))


def lerp(a: float, b: float, u: float) -> float:
    return a + (b - a) * u


def lerp3(a, b, u: float):
    return [lerp(a[i], b[i], u) for i in range(3)]


def env_bell(t: float, t0: float, t1: float, peak: float = 1.0) -> float:
    if t <= t0 or t >= t1:
        return 0.0
    u = (t - t0) / (t1 - t0)
    return peak * math.sin(math.pi * smooth5(u))


def clamp_vec3(v, lims):
    if not lims:
        return [round(float(v[i]), 3) for i in range(3)]
    return [round(max(lims[i][0], min(lims[i][1], float(v[i]))), 3) for i in range(3)]


def compute_gravity_lantern(rot_map: dict, parent_chain: list[str], extra_world_swing_x: float = 0.0, extra_world_swing_z: float = 0.0, cage_lag_x: float = 0.0, cage_lag_z: float = 0.0, prev_bl=None):
    R_par = chain_world_mat(rot_map, parent_chain)
    # Target downward vector tilted by inertial pendulum swing in world space
    sx = math.radians(max(-65.0, min(65.0, extra_world_swing_z)))
    sz = math.radians(max(-65.0, min(65.0, extra_world_swing_x)))
    target_w = np.array([math.sin(sx), -math.cos(sx) * math.cos(sz), math.sin(sz)])
    v_loc = R_par.T @ target_w
    p_ref = prev_bl if prev_bl is not None else _SOLVER_STATE["lantern"]
    brx, bry, brz = align_neg_y_to_local_dir(v_loc, prev_deg=p_ref)
    _SOLVER_STATE["lantern"] = [brx, bry, brz]
    crx = max(-65.0, min(65.0, cage_lag_x))
    crz = max(-55.0, min(55.0, cage_lag_z))
    return [brx, bry, brz], [crx, 0.0, crz]


def build_animation_manifest(creature):
    rig_bones = [b["id"] for b in creature["bones"]]
    limits = {
        "root": {"rot": [[-15, 15], [-45, 45], [-15, 15]]},
        "pelvis": {"rot": [[-40, 40], [-60, 60], [-30, 30]]},
        "waist": {"rot": [[-35, 35], [-45, 45], [-25, 25]]},
        "chest": {"rot": [[-45, 45], [-75, 75], [-30, 30]]},
        "neck": {"rot": [[-35, 35], [-40, 40], [-25, 25]]},
        "head": {"rot": [[-45, 45], [-60, 60], [-30, 30]]},
        "jaw": {"rot": [[-48, 8], [-5, 5], [-5, 5]]},
        "halo_ring": {"rot": [[-25, 25], [-45, 45], [-360, 360]]},
        "core_crystal": {"rot": [[-25, 25], [-360, 360], [-25, 25]]},
        "stole_right": {"rot": [[-55, 55], [-25, 25], [-40, 40]]},
        "stole_left": {"rot": [[-55, 55], [-25, 25], [-40, 40]]},
        "skirt_front": {"rot": [[-35, 55], [-20, 20], [-25, 25]]},
        "skirt_back": {"rot": [[-55, 35], [-20, 20], [-25, 25]]},
        "thigh_right": {"rot": [[-65, 65], [-25, 25], [-25, 25]]},
        "thigh_left": {"rot": [[-65, 65], [-25, 25], [-25, 25]]},
        "crus_right": {"rot": [[-55, 55], [-15, 15], [-15, 15]]},
        "crus_left": {"rot": [[-55, 55], [-15, 15], [-15, 15]]},
        "shin_right": {"rot": [[-55, 55], [-15, 15], [-15, 15]]},
        "shin_left": {"rot": [[-55, 55], [-15, 15], [-15, 15]]},
        "foot_right": {"rot": [[-50, 60], [-20, 20], [-20, 20]]},
        "foot_left": {"rot": [[-50, 60], [-20, 20], [-20, 20]]},
        "tail_0": {"rot": [[-45, 35], [-45, 45], [-25, 25]]},
        "tail_1": {"rot": [[-45, 40], [-50, 50], [-25, 25]]},
        "tail_2": {"rot": [[-50, 45], [-55, 55], [-30, 30]]},
        "tail_3": {"rot": [[-55, 50], [-60, 60], [-30, 30]]},
        "wing_ring_right": {"rot": [[-40, 40], [-70, 70], [-55, 65]]},
        "wing_ring_left": {"rot": [[-40, 40], [-70, 70], [-65, 55]]},
        "wing_outer_right": {"rot": [[-35, 35], [-60, 60], [-45, 45]]},
        "wing_outer_left": {"rot": [[-35, 35], [-60, 60], [-45, 45]]},
        "upper_arm_right": {"rot": [[-135, 155], [-85, 85], [-65, 125]]},
        "upper_arm_left": {"rot": [[-135, 155], [-85, 85], [-125, 65]]},
        "upper_forearm_right": {"rot": [[-65, 125], [-75, 75], [-65, 65]]},
        "upper_forearm_left": {"rot": [[-65, 125], [-75, 75], [-65, 65]]},
        "halberd": {"rot": [[-360, 360], [-360, 360], [-360, 360]]},
        "bell_lantern": {"rot": [[-360, 360], [-65, 65], [-360, 360]]},
        "lantern_cage": {"rot": [[-75, 75], [-45, 45], [-65, 65]]},
        "lower_arm_right": {"rot": [[-55, 110], [-65, 65], [-55, 55]]},
        "lower_arm_left": {"rot": [[-55, 110], [-65, 65], [-55, 55]]},
        "lower_forearm_right": {"rot": [[-55, 95], [-55, 55], [-45, 45]]},
        "lower_forearm_left": {"rot": [[-55, 95], [-55, 55], [-45, 45]]},
    }

    dt = 0.05  # Exact 20 TPS

    def bake_clip(cid: str, kind: str, loop_mode: str, duration: float, eval_fn, beats=None, extra=None):
        _SOLVER_STATE["lantern"] = None
        _SOLVER_STATE["halberd"] = None
        steps = int(round(duration / dt))
        tracks_raw: dict[str, dict[str, dict[str, list[float]]]] = {}

        def add_val(b, ch, t_s, val):
            tracks_raw.setdefault(b, {}).setdefault(ch, {})[t_s] = val

        first_rots, first_poss = None, None
        for idx in range(steps + 1):
            t = round(idx * dt, 4)
            if idx == steps:
                t = round(duration, 4)
            t_s = f"{t:.2f}" if (t * 100) % 10 != 0 else f"{t:.1f}"
            rots, poss = eval_fn(min(t, duration))
            if loop_mode == "loop":
                if idx == 0:
                    first_rots = {k: list(v) for k, v in rots.items()}
                    first_poss = {k: list(v) for k, v in poss.items()}
                elif idx == steps:
                    rots = first_rots
                    poss = first_poss

            for b, v in rots.items():
                add_val(b, "rotation", t_s, clamp_vec3(v, limits.get(b, {}).get("rot")))
            for b, v in poss.items():
                add_val(b, "position", t_s, [round(float(x), 3) for x in v])

        clip = {"id": cid, "kind": kind, "loop": loop_mode, "duration": duration, "tracks": tracks_raw}
        if beats:
            clip["beats"] = beats
        if extra:
            clip.update(extra)
        return clip

    # ------------------------------------------------------------------
    # 1. IDLE (4.0s loop — calm monumental boss breathing, planted feet & halberd,
    #    subtle chest expansion, gentle lantern sway, zero twitching)
    # ------------------------------------------------------------------
    def eval_idle(t: float):
        dur = 4.0
        p = 2.0 * math.pi * (t / dur)
        p_lag = p - 0.55
        p_lag2 = p - 1.15
        br = 0.5 * (1.0 - math.cos(p))  # 0 -> 1 -> 0 slow chest breath

        sway = math.sin(p)
        rots = {
            "pelvis": [0.4 - 0.8 * math.cos(p), 0.0, 0.0],
            "waist": [-1.2 - 1.5 * math.cos(p), 0.0, 0.0],
            "chest": [-1.6 - 2.0 * math.cos(p_lag), 0.8 * sway, 0.0],
            "neck": [1.4 + 1.5 * math.cos(p_lag), 1.4 * math.sin(p_lag), 0.0],
            "head": [-0.6 + 1.1 * math.cos(p_lag2), 2.4 * math.sin(p_lag2), 0.0],
            "jaw": [-1.5 - 2.0 * br, 0.0, 0.0],
            "halo_ring": [1.5 * math.sin(p), 2.0 * math.cos(p), 3.5 * math.sin(p)],
            "core_crystal": [1.5 * math.cos(p), 16.0 * math.sin(p), 2.0 * math.sin(p)],
            "stole_right": [-1.5 - 2.2 * math.sin(p_lag2), 0.0, 1.5 + 1.2 * math.cos(p_lag)],
            "stole_left": [-1.5 - 2.2 * math.sin(p_lag2), 0.0, -1.5 - 1.2 * math.cos(p_lag)],
            "skirt_front": [1.5 + 1.8 * math.sin(p_lag), 0.0, -0.8 * sway],
            "skirt_back": [-2.0 - 2.0 * math.sin(p_lag), 0.0, -0.8 * sway],
            # Planted stance (minimal vertical knee settle keeping foot soles & halberd flat at Y=0)
            "thigh_right": [0.8 * br, 0.0, 0.0],
            "crus_right": [-1.6 * br, 0.0, 0.0],
            "shin_right": [0.8 * br, 0.0, 0.0],
            "foot_right": [0.0, 0.0, 0.0],
            "thigh_left": [0.8 * br, 0.0, 0.0],
            "crus_left": [-1.6 * br, 0.0, 0.0],
            "shin_left": [0.8 * br, 0.0, 0.0],
            "foot_left": [0.0, 0.0, 0.0],
            # Gentle respiratory pinion breathing
            "wing_ring_right": [0.8 * math.sin(p_lag), -3.0 - 2.4 * math.sin(p_lag), 1.5 + 2.2 * math.sin(p_lag)],
            "wing_outer_right": [1.2 * math.cos(p_lag2), -3.5 - 3.2 * math.sin(p_lag2), 2.0 * math.cos(p_lag2)],
            "wing_ring_left": [0.8 * math.sin(p_lag), 3.0 + 2.4 * math.sin(p_lag), -1.5 - 2.2 * math.sin(p_lag)],
            "wing_outer_left": [1.2 * math.cos(p_lag2), 3.5 + 3.2 * math.sin(p_lag2), -2.0 * math.cos(p_lag2)],
            # Sentinel guard: halberd held steady, left arm cradles lantern calmly
            "upper_arm_right": [1.2 * math.sin(p_lag), -1.0, 1.5 + 1.2 * math.cos(p)],
            "upper_forearm_right": [2.0 + 1.2 * math.sin(p_lag2), 2.0, 0.0],
            "halberd": [-0.8 * math.cos(p_lag2), 0.0, -1.2],
            "upper_arm_left": [2.0 + 1.5 * math.cos(p_lag), 2.0, -3.5 - 1.4 * math.sin(p)],
            "upper_forearm_left": [4.0 + 1.8 * math.sin(p_lag2), -2.0, 0.0],
            # Calm occult weaver poise (zero twitching)
            "lower_arm_right": [3.0 + 2.5 * math.sin(p_lag), -2.0 + 2.0 * math.cos(p), 2.0 * math.sin(p)],
            "lower_forearm_right": [4.0 + 2.5 * math.sin(p_lag2), -2.0, 0.0],
            "lower_arm_left": [3.0 + 2.5 * math.sin(p_lag + 0.6), 2.0 + 2.0 * math.cos(p), -2.0 * math.sin(p)],
            "lower_forearm_left": [4.0 + 2.5 * math.sin(p_lag2 + 0.6), 2.0, 0.0],
            # Slow lazy tail drift
            "tail_0": [-1.0 + 1.2 * math.cos(p), 2.0 * math.sin(p), 0.0],
            "tail_1": [-1.5 + 1.8 * math.cos(p - 0.6), 3.2 * math.sin(p - 0.6), 0.0],
            "tail_2": [0.8 + 2.4 * math.cos(p - 1.2), 4.4 * math.sin(p - 1.2), 0.0],
            "tail_3": [1.2 + 3.0 * math.cos(p - 1.8), 5.6 * math.sin(p - 1.8), 0.0],
        }
        bl, cage = compute_gravity_lantern(
            rots, ["pelvis", "waist", "chest", "upper_arm_left", "upper_forearm_left"],
            extra_world_swing_x=2.6 * math.cos(p_lag), extra_world_swing_z=3.0 * math.sin(p_lag2),
            cage_lag_x=-1.8 * math.sin(p_lag2), cage_lag_z=2.0 * math.cos(p_lag2),
        )
        rots["bell_lantern"] = bl
        rots["lantern_cage"] = cage

        poss = {
            "pelvis": [0.0, 0.42 * br, 0.0],
            "core_crystal": [0.0, 0.35 * math.sin(p_lag), 0.0],
        }
        return rots, poss

    # ------------------------------------------------------------------
    # 2. STRIDE (1.2s loop — heavy digitigrade stalk with high hock tuck,
    #    spine counter-twist, billowing cloth/wings, swinging pendulum lantern)
    # ------------------------------------------------------------------
    def eval_stride(t: float):
        dur = 1.2
        # Shift by pi/2 so t=0 starts on Right heel-strike / Left push-off (strictly contralateral)
        p = 2.0 * math.pi * (t / dur) + 0.5 * math.pi
        s = math.sin(p)
        c = math.cos(p)
        # Double-frequency vault: hip is LOWEST at wide double-support (p=pi/2,3pi/2 -> c2=-1)
        # and HIGHEST at mid-stance vertical pass (p=0,pi -> c2=+1).
        vault = 0.5 * (1.0 + math.cos(2.0 * p))

        def gait_leg(phase: float):
            sp = math.sin(phase)
            cp = math.cos(phase)
            # Swing phase is when th = 30*sp increases (cp > 0): leg sweeps back(-30) -> front(+30).
            # Stance phase is when cp < 0: foot pushes body forward from +30 -> -30.
            swing = max(0.0, cp)
            stance = max(0.0, -cp)
            th = 30.0 * sp
            cr = -10.0 - 42.0 * swing + 4.0 * stance
            sh = -4.0 - 28.0 * swing + 10.0 * sp * stance
            # Keep planted foot level on stance (-0.8*th), curl toes during swing
            ft = -0.8 * th * stance - 22.0 * swing + 12.0 * max(0.0, sp) * swing
            return th, cr, sh, ft

        tr, crr, shr, ftr = gait_leg(p)
        tl, crl, shl, ftl = gait_leg(p + math.pi)

        rots = {
            "pelvis": [-5.0 - 3.0 * (1.0 - vault), -11.0 * s, -5.5 * s],
            "waist": [-4.0 + 2.5 * vault, 9.0 * s, 3.5 * s],
            "chest": [-6.0 - 2.0 * math.cos(2.0 * p), 11.0 * math.sin(p - 0.35), 4.0 * s],
            "neck": [6.0 + 3.0 * vault, -5.0 * s, 0.0],
            "head": [6.0 - 3.0 * vault, -8.0 * s, -3.0 * s],
            "jaw": [-5.0 - 4.0 * (1.0 - vault), 0.0, 0.0],
            "halo_ring": [8.0 * vault, -14.0 * s, 18.0 * c],
            "core_crystal": [0.0, 45.0 * s, 0.0],
            "stole_right": [14.0 + 14.0 * math.sin(p - 0.6), 0.0, 8.0 + 5.0 * c],
            "stole_left": [14.0 - 14.0 * math.sin(p - 0.6), 0.0, -8.0 - 5.0 * c],
            "skirt_front": [16.0 + 10.0 * (1.0 - vault), 6.0 * s, -6.0 * c],
            "skirt_back": [-16.0 - 10.0 * (1.0 - vault), -6.0 * s, -6.0 * c],
            "thigh_right": [tr, 0.0, 3.0 * s],
            "crus_right": [crr, 0.0, 0.0],
            "shin_right": [shr, 0.0, 0.0],
            "foot_right": [ftr, 0.0, 0.0],
            "thigh_left": [tl, 0.0, 3.0 * s],
            "crus_left": [crl, 0.0, 0.0],
            "shin_left": [shl, 0.0, 0.0],
            "foot_left": [ftl, 0.0, 0.0],
            # Contralateral arms: Right (+X Halberd) swings OPPOSITE to Right leg (+tr -> -arm)
            "upper_arm_right": [-8.0 - 24.0 * s, 8.0 * c, 8.0 + 6.0 * s],
            "upper_forearm_right": [16.0 + 14.0 * math.sin(p - 0.5), 10.0, 0.0],
            "halberd": [8.0 + 16.0 * math.sin(p - 0.9), 0.0, -6.0],
            "upper_arm_left": [-8.0 + 26.0 * s, -8.0 * c, -14.0 - 4.0 * s],
            "upper_forearm_left": [18.0 - 14.0 * math.sin(p - 0.5), -10.0, 0.0],
            # Lower weaver arms weave rhythmically counter to upper arms
            "lower_arm_right": [10.0 + 22.0 * s, -8.0 * s, 6.0],
            "lower_forearm_right": [18.0 + 16.0 * math.sin(p - 0.7), 0.0, 0.0],
            "lower_arm_left": [10.0 - 22.0 * s, -8.0 * s, -6.0],
            "lower_forearm_left": [18.0 - 16.0 * math.sin(p - 0.7), 0.0, 0.0],
            # Rippling wing cadence
            "wing_ring_right": [-6.0, -14.0 - 14.0 * vault, 6.0 + 14.0 * math.sin(2.0 * p - 0.4)],
            "wing_outer_right": [4.0, -12.0 - 16.0 * math.sin(2.0 * p - 1.0), 10.0 * math.cos(2.0 * p - 1.0)],
            "wing_ring_left": [-6.0, 14.0 + 14.0 * vault, -6.0 - 14.0 * math.sin(2.0 * p - 0.4)],
            "wing_outer_left": [4.0, 12.0 + 16.0 * math.sin(2.0 * p - 1.0), -10.0 * math.cos(2.0 * p - 1.0)],
            # Broad sweeping counter-balance tail whip
            "tail_0": [-8.0 + 6.0 * vault, -16.0 * s, 0.0],
            "tail_1": [-10.0 + 8.0 * math.cos(2.0 * p - 0.5), -22.0 * math.sin(p - 0.6), 0.0],
            "tail_2": [-12.0 + 10.0 * math.cos(2.0 * p - 1.1), -28.0 * math.sin(p - 1.2), 0.0],
            "tail_3": [-10.0 + 12.0 * math.cos(2.0 * p - 1.7), -34.0 * math.sin(p - 1.8), 0.0],
        }
        bl, cage = compute_gravity_lantern(
            rots, ["pelvis", "waist", "chest", "upper_arm_left", "upper_forearm_left"],
            extra_world_swing_x=-24.0 * math.cos(p - 0.4), extra_world_swing_z=12.0 * s,
            cage_lag_x=16.0 * math.sin(p - 0.8), cage_lag_z=8.0 * c,
        )
        rots["bell_lantern"] = bl
        rots["lantern_cage"] = cage

        poss = {"pelvis": [0.8 * s, -0.5 + 2.2 * vault, 0.0]}
        return rots, poss

    # ------------------------------------------------------------------
    # 3. HALBERD_CLEAVE (1.55s once): Authentic HEMA Overhead-Diagonal Chop (*Oberhau/Zornhau*)
    #    1) Right Vom Tag chamber outside shoulder + choke grip down shaft
    #    2) Hip-led overhead arc with axe cutting edge leading forward-down
    #    3) 45-deg vertical-diagonal floor bite with lower-right weaver pommel leverage
    #    4) Weighted flank recovery back to sentinel guard
    # ------------------------------------------------------------------
    def eval_cleave(t: float):
        # Beats: chamber 0->0.48, overhead->chop 0.48->0.70, ground bite arrest 0.70->0.92, recovery 0.92->1.55
        b_bind = [0.0, 1.0, 0.0]
        b_chamber = [0.46, 0.78, 0.42]
        b_overhead = [0.16, 0.85, -0.50]
        b_bite = [-0.10, -0.56, -0.82]
        b_arrest = [-0.14, -0.54, -0.83]
        b_lift = [0.58, 0.72, -0.38]

        if t <= 0.48:
            u = smooth5(t / 0.48)
            pel_p = [0.0, lerp(0.0, -1.0, u), lerp(0.0, 3.2, u)]
            pel_r = [lerp(0, 3, u), lerp(0, 12, u), lerp(0, 4, u)]
            wai_r = [lerp(0, 3, u), lerp(0, 12, u), lerp(0, 4, u)]
            chs_r = [lerp(0, 4, u), lerp(0, 12, u), lerp(0, 5, u)]
            nck_r = [lerp(0, -3, u), lerp(0, -14, u), 0.0]
            hd_r  = [lerp(0, -4, u), lerp(0, -22, u), lerp(0, -5, u)]
            jaw_r = [lerp(-2, -18, u), 0.0, 0.0]
            # Right Vom Tag chamber at temple height (+rz=74, -ry=-20) — clear of antlers & inside frame!
            uar_r = [lerp(2, -26, u), lerp(-2, -20, u), lerp(2, 74, u)]
            ufr_r = [lerp(4, 16, u), lerp(4, -12, u), lerp(0, -8, u)]
            blade_dir = slerp_dir(b_bind, b_chamber, u)
            xhint = slerp_dir([1.0, 0.0, 0.0], [-0.15, -0.25, -0.95], u)
            slide = 10.0 * u
            lar_r = [lerp(4, 24, u), lerp(0, -24, u), lerp(0, 48, u)]
            lfr_r = [lerp(4, 26, u), lerp(0, -10, u), 0.0]
            ual_r = [lerp(4, 18, u), lerp(2, 16, u), lerp(-4, -22, u)]
            ufl_r = [lerp(6, 18, u), lerp(-4, 8, u), 0.0]
            swx, swz = lerp(0, -8, u), lerp(0, 6, u)
            cgx, cgz = lerp(0, 5, u), lerp(0, -4, u)
        elif t <= 0.70:
            u = ((t - 0.48) / 0.22) ** 2.1
            u_arc = (t - 0.48) / 0.22
            u_lead = smooth5(min(1.0, u_arc / 0.65))
            pel_p = [0.0, lerp(-1.0, -2.2, u_lead), lerp(3.2, -9.5, u_lead)]
            pel_r = [lerp(3, -10, u_lead), lerp(12, -12, u_lead), lerp(4, -5, u_lead)]
            wai_r = [lerp(3, -11, u), lerp(12, -10, u), lerp(4, -4, u)]
            chs_r = [lerp(4, -13, u), lerp(12, -12, u), lerp(5, -5, u)]
            nck_r = [lerp(-3, 14, u_lead), lerp(-14, 12, u_lead), 0.0]
            hd_r  = [lerp(-4, 16, u_lead), lerp(-22, 20, u_lead), lerp(-5, 5, u_lead)]
            jaw_r = [lerp(-18, -38, u), 0.0, 0.0]
            if u <= 0.42:
                v = u / 0.42
                uar_r = lerp3([-26, -20, 74], [16, -10, 72], v)
                ufr_r = lerp3([16, -12, -8], [18, 0, -4], v)
                blade_dir = slerp_dir(b_chamber, b_overhead, v)
                xhint = slerp_dir([-0.15, -0.25, -0.95], [-0.08, -0.75, -0.65], v)
            else:
                v = (u - 0.42) / 0.58
                uar_r = lerp3([16, -10, 72], [62, 14, 14], v)
                ufr_r = lerp3([18, 0, -4], [14, 10, 0], v)
                blade_dir = slerp_dir(b_overhead, b_bite, v)
                xhint = slerp_dir([-0.08, -0.75, -0.65], [-0.12, -0.96, 0.25], v)
            slide = 10.0
            lar_r = lerp3([24, -24, 48], [44, 14, 12], u)
            lfr_r = lerp3([26, -10, 0], [18, 8, 0], u)
            ual_r = [lerp(18, -28, u), lerp(16, 18, u), lerp(-22, -42, u)]
            ufl_r = [lerp(18, 12, u), lerp(8, 0, u), 0.0]
            swx, swz = lerp(-8, 22, u), lerp(6, -16, u)
            cgx, cgz = lerp(5, -12, u), lerp(-4, 9, u)
        elif t <= 0.92:
            u = (t - 0.70) / 0.22
            vib = math.sin(u * math.pi * 2.5) * math.exp(-3.5 * u)
            settle = smooth5(u)
            pel_p = [0.0, -2.2 - 0.4 * math.sin(math.pi * u) + 0.35 * vib, -9.5 + 1.2 * settle]
            pel_r = [-10.0 - 2.0 * math.sin(math.pi * u), -12.0 - 2.0 * settle, -5.0]
            wai_r = [-11.0 - 2.5 * vib, -10.0 - 2.0 * settle, -4.0]
            chs_r = [-13.0 + 2.0 * settle + 2.5 * vib, -12.0 - 2.0 * settle, -5.0]
            nck_r = [14.0 + 2.0 * vib, 12.0, 0.0]
            hd_r  = [16.0 - 3.0 * vib, 20.0, 5.0]
            jaw_r = [-38.0 + 10.0 * settle, 0.0, 0.0]
            uar_r = [62.0 - 4.0 * settle + 2.5 * vib, 14.0, 14.0 + 6.0 * settle]
            ufr_r = [14.0 + 4.0 * settle, 10.0, 0.0]
            blade_dir = slerp_dir(b_bite, b_arrest, settle)
            xhint = [-0.12, -0.96, 0.25]
            slide = 11.0 - 1.0 * settle
            lar_r = [44.0 - 4.0 * settle, 14.0, 12.0]
            lfr_r = [18.0, 8.0, 0.0]
            ual_r = [-28.0 + 10.0 * settle, 18.0, -42.0 + 8.0 * settle]
            ufl_r = [12.0 + 4.0 * settle, 0.0, 0.0]
            swx, swz = lerp(22, -12, u), lerp(-16, 10, u)
            cgx, cgz = lerp(-12, 8, u), lerp(9, -6, u)
        else:
            u = smooth5((t - 0.92) / 0.63)
            arc = math.sin(math.pi * u)
            pel_p = [0.0, lerp(-1.8, 0.0, u), lerp(-8.3, 0.0, u)]
            pel_r = [lerp(-10, 0, u), lerp(-14, 0, u), lerp(-5, 0, u)]
            wai_r = [lerp(-11, 0, u), lerp(-12, 0, u), lerp(-4, 0, u)]
            chs_r = [lerp(-11, 0, u), lerp(-14, 0, u), lerp(-5, 0, u)]
            nck_r = [lerp(14, 0, u), lerp(12, 0, u), 0.0]
            hd_r  = [lerp(13, 0, u), lerp(20, 0, u), lerp(5, 0, u)]
            jaw_r = [lerp(-28, -2, u), 0.0, 0.0]
            uar_r = [lerp(58, 2, u) - 16.0 * arc, lerp(14, -2, u) - 14.0 * arc, lerp(20, 2, u) + 32.0 * arc]
            ufr_r = [lerp(18, 4, u) + 10.0 * arc, lerp(10, 4, u), 0.0]
            if u <= 0.55:
                v = u / 0.55
                blade_dir = slerp_dir(b_arrest, b_lift, v)
                xhint = slerp_dir([-0.12, -0.96, 0.25], [0.85, 0.10, -0.50], v)
            else:
                v = (u - 0.55) / 0.45
                blade_dir = slerp_dir(b_lift, b_bind, v)
                xhint = slerp_dir([0.85, 0.10, -0.50], [1.0, 0.0, 0.0], v)
            slide = lerp(10.0, 0.0, u)
            lar_r = [lerp(40, 4, u), lerp(14, 0, u), lerp(12, 0, u)]
            lfr_r = [lerp(18, 4, u), lerp(8, 0, u), 0.0]
            ual_r = [lerp(-18, 4, u), lerp(18, 2, u), lerp(-34, -4, u)]
            ufl_r = [lerp(16, 6, u), lerp(0, -4, u), 0.0]
            swx, swz = -12.0 * (1.0 - u) + 5.0 * arc, 10.0 * (1.0 - u)
            cgx, cgz = 8.0 * (1.0 - u) - 4.0 * arc, -6.0 * (1.0 - u)

        lunge = smooth5(min(1.0, max(0.0, (t - 0.44) / 0.22))) if t <= 0.92 else (1.0 - smooth5((t - 0.92) / 0.63))
        coil = smooth5(min(1.0, t / 0.46)) if t <= 0.48 else max(0.0, 1.0 - (t - 0.48) / 0.24)
        tail_whip = env_bell(t, 0.46, 1.25, 1.0)
        wing_open = env_bell(t, 0.46, 1.25, 1.0)

        rots = {
            "pelvis": pel_r, "waist": wai_r, "chest": chs_r,
            "neck": nck_r, "head": hd_r, "jaw": jaw_r,
            "halo_ring": [10.0 * lunge, -12.0 * coil, 360.0 * smooth5(t / 1.55)],
            "core_crystal": [0.0, 360.0 * smooth5(t / 1.55), 0.0],
            "stole_right": [-4.0 + 24.0 * lunge - 12.0 * coil, 0.0, 12.0 * lunge],
            "stole_left": [-4.0 + 22.0 * lunge - 10.0 * coil, 0.0, -12.0 * lunge],
            "skirt_front": [6.0 + 24.0 * lunge, -10.0 * lunge, -6.0 * lunge],
            "skirt_back": [-10.0 - 20.0 * tail_whip, 0.0, 6.0 * lunge],
            "thigh_left": [36.0 * lunge + 14.0 * coil, 0.0, -6.0 * lunge],
            "crus_left": [-40.0 * lunge - 18.0 * coil, 0.0, 0.0],
            "shin_left": [-8.0 * lunge, 0.0, 0.0],
            "foot_left": [12.0 * lunge + 6.0 * coil, 0.0, 0.0],
            "thigh_right": [-28.0 * lunge - 16.0 * coil, 0.0, 6.0 * lunge],
            "crus_right": [-8.0 * lunge, 0.0, 0.0],
            "shin_right": [5.0 * lunge, 0.0, 0.0],
            "foot_right": [24.0 * lunge + 10.0 * coil, 0.0, 0.0],
            "upper_arm_right": uar_r, "upper_forearm_right": ufr_r,
            "upper_arm_left": ual_r, "upper_forearm_left": ufl_r,
            "lower_arm_right": lar_r, "lower_forearm_right": lfr_r,
            "lower_arm_left": [-20.0 * lunge + 16.0 * coil, 20.0 * lunge, -22.0 * lunge],
            "lower_forearm_left": [14.0 * coil, 0.0, 0.0],
            "wing_ring_right": [-14.0 * coil + 16.0 * wing_open, -16.0 * coil + 24.0 * wing_open, 14.0 * coil + 10.0 * wing_open],
            "wing_outer_right": [-6.0 * coil + 10.0 * env_bell(t, 0.54, 1.35), -8.0 * coil + 10.0 * env_bell(t, 0.54, 1.35), 8.0 * env_bell(t, 0.54, 1.35)],
            "wing_ring_left": [-14.0 * coil + 16.0 * wing_open, 16.0 * coil - 24.0 * wing_open, -14.0 * coil - 10.0 * wing_open],
            "wing_outer_left": [-6.0 * coil + 10.0 * env_bell(t, 0.54, 1.35), 8.0 * coil - 10.0 * env_bell(t, 0.54, 1.35), -8.0 * env_bell(t, 0.54, 1.35)],
            "tail_0": [8.0 * coil - 28.0 * tail_whip, -14.0 * tail_whip, 0.0],
            "tail_1": [10.0 * coil - 32.0 * env_bell(t, 0.52, 1.30), -20.0 * env_bell(t, 0.52, 1.30), 0.0],
            "tail_2": [12.0 * coil - 36.0 * env_bell(t, 0.57, 1.36), -26.0 * env_bell(t, 0.57, 1.36), 0.0],
            "tail_3": [14.0 * coil - 40.0 * env_bell(t, 0.62, 1.42), -32.0 * env_bell(t, 0.62, 1.42), 0.0],
        }
        hlb_r = aim_halberd_world(
            rots, ["pelvis", "waist", "chest", "upper_arm_right", "upper_forearm_right"],
            blade_dir, xhint,
        )
        rots["halberd"] = hlb_r
        hlb_pos = (euler_zyx_mat(*hlb_r) @ np.array([0.0, slide, 0.0])).tolist()
        bl, cage = compute_gravity_lantern(
            rots, ["pelvis", "waist", "chest", "upper_arm_left", "upper_forearm_left"],
            extra_world_swing_x=swx, extra_world_swing_z=swz,
            cage_lag_x=cgx, cage_lag_z=cgz,
        )
        rots["bell_lantern"] = bl
        rots["lantern_cage"] = cage
        return rots, {"pelvis": pel_p, "halberd": hlb_pos}

    # ------------------------------------------------------------------
    # 4. BELL_TOLL_CONDUIT (2.0s once): Hoists lantern cleanly in open left air (+ry outward) ->
    #    cock right wide -> clear forehand toll in front of chest -> crisp rebound so
    #    bronze lantern rings freely outward-left like a heavy temple bell!
    # ------------------------------------------------------------------
    def eval_bell(t: float):
        b_bind = [0.0, 1.0, 0.0]
        b_cock = [0.55, 0.80, -0.22]
        b_chime = [-0.94, -0.26, -0.22]
        b_poise = [0.45, 0.86, -0.20]

        if t <= 0.55:
            u = smooth5(t / 0.55)
            pel_p = [0.0, 1.2 * u, 0.4 * u]
            pel_r = [2.0 * u, 2.0 * u, 1.5 * u]
            wai_r = [2.0 * u, 2.0 * u, 1.5 * u]
            chs_r = [3.0 * u, 2.0 * u, 1.5 * u]
            hd_r  = [6.0 * u, 16.0 * u, 0.0]
            jaw_r = [-2.0 - 12.0 * u, 0.0, 0.0]
            # Stage lantern elevated in front-left open air (handL Y ~ 50, cage Y ~ 37, Z ~ -16)
            ual_r = [lerp(4, 72, u), lerp(2, -10, u), lerp(-4, -10, u)]
            ufl_r = [lerp(6, 20, u), lerp(-4, 10, u), 0.0]
            # Right cocks wide outside right flank
            uar_r = [lerp(2, 18, u), lerp(-2, -24, u), lerp(2, 42, u)]
            ufr_r = [lerp(4, 16, u), lerp(4, -14, u), 0.0]
            blade_dir = slerp_dir(b_bind, b_cock, u)
            xhint = slerp_dir([1.0, 0.0, 0.0], [0.85, -0.45, -0.25], u)
            slide = 0.0
            swx, swz = lerp(0, 4, u), lerp(0, -4, u)
            cgx, cgz = lerp(0, -3, u), 0.0
        elif t <= 0.82:
            u = ((t - 0.55) / 0.27) ** 1.85
            pel_p = [0.0, lerp(1.2, 1.8, u), lerp(0.4, -1.5, u)]
            pel_r = [lerp(2, 3, u), lerp(2, -6, u), lerp(1.5, -2, u)]
            wai_r = [lerp(2, 4, u), lerp(2, -6, u), lerp(1.5, -2, u)]
            chs_r = [lerp(3, 5, u), lerp(2, -8, u), lerp(1.5, -2, u)]
            hd_r  = [lerp(6, 12, u), lerp(16, 10, u), 0.0]
            jaw_r = [lerp(-14, -40, u), 0.0, 0.0]
            ual_r = [lerp(72, 74, u), lerp(-10, -8, u), lerp(-10, -10, u)]
            ufl_r = [20.0, 10.0, 0.0]
            # Right arm punches forward-across in front (-Z ~ -15) so collar/blade strikes cage dead-on at Y~36!
            uar_r = [lerp(18, 34, u), lerp(-24, 38, u), lerp(42, -8, u)]
            ufr_r = [lerp(16, 18, u), lerp(-14, 18, u), 0.0]
            blade_dir = slerp_dir(b_cock, b_chime, u)
            xhint = slerp_dir([0.85, -0.45, -0.25], [0.0, 0.15, -0.98], u)
            slide = 0.0
            kick = max(0.0, (t - 0.78) / 0.04)
            swx, swz = 4.0 - 8.0 * kick, -4.0 - 24.0 * kick
            cgx, cgz = -3.0 + 6.0 * kick, -12.0 * kick
        elif t <= 1.42:
            u = (t - 0.82) / 0.60
            reb = smooth5(min(1.0, (t - 0.82) / 0.24))
            bell_osc = math.cos(u * math.pi * 2.5) * math.exp(-2.6 * u)
            breath_pulse = math.sin(u * math.pi * 2.0) * 2.5
            pel_p = [0.0, 1.8 - 0.8 * smooth5(u) + 0.3 * math.sin(u * math.pi * 2.0), -1.5]
            pel_r = [3.0 - 1.5 * smooth5(u), -6.0, -2.0]
            wai_r = [4.0 + breath_pulse, -6.0, -2.0]
            chs_r = [5.0 + breath_pulse * 1.2, -8.0, -2.0]
            hd_r  = [12.0 - 4.0 * smooth5(u) + breath_pulse, lerp(10, 4, smooth5(u)), 0.0]
            jaw_r = [-40.0 + 10.0 * smooth5(u) + 3.0 * math.sin(u * math.pi * 4.0), 0.0, 0.0]
            # Opens left arm wide outward after strike so ringing bronze bell clears body in every view!
            ual_r = [lerp(74, 68, reb), lerp(-8, 22, reb) + 3.0 * bell_osc, lerp(-10, -32, reb) - 3.0 * bell_osc]
            ufl_r = [lerp(20, 12, reb), lerp(10, 6, reb), 0.0]
            uar_r = [lerp(34, 18, reb), lerp(38, -22, reb), lerp(-8, 42, reb)]
            ufr_r = [lerp(20, 14, reb), lerp(18, -8, reb), 0.0]
            blade_dir = slerp_dir(b_chime, b_poise, reb)
            xhint = slerp_dir([0.0, 0.15, -0.98], [0.85, 0.15, -0.45], reb)
            slide = 0.0
            swx, swz = -8.0 * bell_osc, -22.0 * bell_osc
            cgx, cgz = 6.0 * math.cos(u * math.pi * 2.5 - 0.6) * math.exp(-2.3 * u), -12.0 * math.cos(u * math.pi * 2.5 - 0.5) * math.exp(-2.3 * u)
        else:
            u = smooth5((t - 1.42) / 0.58)
            pel_p = [0.0, lerp(1.0, 0.0, u), lerp(-1.5, 0.0, u)]
            pel_r = [lerp(1.5, 0, u), lerp(-6, 0, u), lerp(-2, 0, u)]
            wai_r = [lerp(4, 0, u), lerp(-6, 0, u), lerp(-2, 0, u)]
            chs_r = [lerp(5, 0, u), lerp(-8, 0, u), lerp(-2, 0, u)]
            hd_r  = [lerp(8, 0, u), lerp(4, 0, u), 0.0]
            jaw_r = [lerp(-30, -2, u), 0.0, 0.0]
            ual_r = [lerp(68, 4, u), lerp(22, 2, u), lerp(-32, -4, u)]
            ufl_r = [lerp(12, 6, u), lerp(6, -4, u), 0.0]
            uar_r = [lerp(18, 2, u), lerp(-22, -2, u), lerp(42, 2, u)]
            ufr_r = [lerp(14, 4, u), lerp(-8, 4, u), 0.0]
            blade_dir = slerp_dir(b_poise, b_bind, u)
            xhint = slerp_dir([0.85, 0.15, -0.45], [1.0, 0.0, 0.0], u)
            slide = 0.0
            swx, swz = 0.0, 0.0
            cgx, cgz = 0.0, 0.0

        flare = env_bell(t, 0.45, 1.65, 1.0)
        weave = env_bell(t, 0.60, 1.60, 1.0)
        pulse_w = math.sin(max(0.0, t - 0.78) * 12.0) * flare
        rots = {
            "pelvis": pel_r, "waist": wai_r, "chest": chs_r,
            "neck": [hd_r[0] * 0.55, -hd_r[1] * 0.3, 0.0],
            "head": hd_r, "jaw": jaw_r,
            "halo_ring": [8.0 * flare, 0.0, 360.0 * smooth5(t / 2.0)],
            "core_crystal": [0.0, 360.0 * smooth5(t / 2.0), 0.0],
            "stole_right": [14.0 * flare + 4.0 * pulse_w, 0.0, 14.0 * flare],
            "stole_left": [14.0 * flare + 4.0 * pulse_w, 0.0, -14.0 * flare],
            "skirt_front": [12.0 * flare + 4.0 * pulse_w, 0.0, 0.0],
            "skirt_back": [-16.0 * flare - 4.0 * pulse_w, 0.0, 0.0],
            "thigh_right": [-8.0 * flare, 0.0, 5.0 * flare],
            "crus_right": [6.0 * flare, 0.0, 0.0],
            "shin_right": [-6.0 * flare, 0.0, 0.0],
            "foot_right": [10.0 * flare, 0.0, 0.0],
            "thigh_left": [-8.0 * flare, 0.0, -5.0 * flare],
            "crus_left": [6.0 * flare, 0.0, 0.0],
            "shin_left": [-6.0 * flare, 0.0, 0.0],
            "foot_left": [10.0 * flare, 0.0, 0.0],
            "upper_arm_right": uar_r, "upper_forearm_right": ufr_r,
            "upper_arm_left": ual_r, "upper_forearm_left": ufl_r,
            "lower_arm_right": [38.0 * weave + 5.0 * pulse_w, 10.0 * weave, 20.0 * weave],
            "lower_forearm_right": [-14.0 * weave + 6.0 * pulse_w, 12.0 * weave, 0.0],
            "lower_arm_left": [38.0 * weave + 5.0 * pulse_w, -10.0 * weave, -20.0 * weave],
            "lower_forearm_left": [-14.0 * weave + 6.0 * pulse_w, -12.0 * weave, 0.0],
            "wing_ring_right": [-10.0 * flare, -38.0 * flare - 5.0 * pulse_w, 24.0 * flare],
            "wing_outer_right": [-6.0 * flare, -12.0 * env_bell(t, 0.55, 1.70), 8.0 * env_bell(t, 0.55, 1.70)],
            "wing_ring_left": [-10.0 * flare, 38.0 * flare + 5.0 * pulse_w, -24.0 * flare],
            "wing_outer_left": [-6.0 * flare, 12.0 * env_bell(t, 0.55, 1.70), -8.0 * env_bell(t, 0.55, 1.70)],
            "tail_0": [-12.0 * flare, 12.0 * math.sin(t * 6.5) * flare, 0.0],
            "tail_1": [-16.0 * flare, 18.0 * math.sin(t * 6.5 - 0.6) * flare, 0.0],
            "tail_2": [-20.0 * flare, 24.0 * math.sin(t * 6.5 - 1.2) * flare, 0.0],
            "tail_3": [-24.0 * flare, 30.0 * math.sin(t * 6.5 - 1.8) * flare, 0.0],
        }
        hlb_r = aim_halberd_world(
            rots, ["pelvis", "waist", "chest", "upper_arm_right", "upper_forearm_right"],
            blade_dir, xhint,
        )
        rots["halberd"] = hlb_r
        hlb_pos = (euler_zyx_mat(*hlb_r) @ np.array([0.0, slide, 0.0])).tolist()
        bl, cage = compute_gravity_lantern(
            rots, ["pelvis", "waist", "chest", "upper_arm_left", "upper_forearm_left"],
            extra_world_swing_x=swx, extra_world_swing_z=swz,
            cage_lag_x=cgx, cage_lag_z=cgz,
        )
        rots["bell_lantern"] = bl
        rots["lantern_cage"] = cage
        return rots, {"pelvis": pel_p, "halberd": hlb_pos}

    # ------------------------------------------------------------------
    # 5. LEYLINE_CATACLYSM (2.0s once): Deep crouch gather -> floating seraphic ascent ->
    #    real downward-edge halberd meteor slam -> heavy rebound settle
    # ------------------------------------------------------------------
    def eval_cataclysm(t: float):
        b_bind = [0.0, 1.0, 0.0]
        b_crouch = [0.30, 0.92, -0.24]
        b_zenith = [0.44, 0.85, 0.28]
        b_crest = [0.14, 0.86, -0.49]
        b_slam = [-0.10, -0.56, -0.82]
        b_impact = [-0.14, -0.54, -0.83]
        b_lift = [0.58, 0.72, -0.38]

        if t <= 0.30:
            u = smooth5(t / 0.30)
            pel_p = [0.0, lerp(0, -2.0, u), lerp(0, 2.5, u)]
            pel_r = [lerp(0, -12, u), 0.0, 0.0]
            wai_r = [lerp(0, -12, u), 0.0, 0.0]
            chs_r = [lerp(0, -14, u), 0.0, 0.0]
            hd_r  = [lerp(0, 18, u), 0.0, 0.0]
            jaw_r = [lerp(-2, -12, u), 0.0, 0.0]
            th_x, cr_x, sh_x, ft_x = 34 * u, -42 * u, -8 * u, 16 * u
            uar_r = [lerp(2, 18, u), -4.0, lerp(2, 22, u)]
            ufr_r = [lerp(4, 24, u), 6.0, 0.0]
            blade_dir = slerp_dir(b_bind, b_crouch, u)
            xhint = [0.95, 0.0, -0.3]
            slide = 10.0 * u
            ual_r = [lerp(4, 18, u), 4.0, lerp(-4, -18, u)]
            ufl_r = [lerp(6, 24, u), -6.0, 0.0]
            w_open = -0.3 * u
            swx, swz, cgx, cgz = -10 * u, 0.0, 6 * u, 0.0
        elif t <= 0.85:
            u = smooth5((t - 0.30) / 0.55)
            pel_p = [0.0, lerp(-2.0, 5.8, u), lerp(2.5, 3.0, u)]
            pel_r = [lerp(-12, 12, u), 0.0, 0.0]
            wai_r = [lerp(-12, 10, u), 0.0, 0.0]
            chs_r = [lerp(-14, 14, u), 0.0, 0.0]
            hd_r  = [lerp(18, 12, u), 0.0, 0.0]
            jaw_r = [lerp(-12, -44, u), 0.0, 0.0]
            th_x, cr_x, sh_x, ft_x = lerp(34, 14, u), lerp(-42, -24, u), lerp(-8, 16, u), lerp(16, 40, u)
            uar_r = [lerp(18, -16, u), lerp(-4, -22, u), lerp(22, 92, u)]
            ufr_r = [lerp(24, 12, u), lerp(6, -10, u), 0.0]
            blade_dir = slerp_dir(b_crouch, b_zenith, u)
            xhint = [0.25, -0.25, -0.93]
            slide = 10.0
            ual_r = [lerp(18, -8, u), lerp(4, 18, u), lerp(-18, -78, u)]
            ufl_r = [lerp(24, 12, u), lerp(-6, 10, u), 0.0]
            w_open = lerp(-0.3, 1.0, u)
            swx, swz, cgx, cgz = lerp(-10, 8, u), lerp(0, -14, u), lerp(6, -8, u), lerp(0, 8, u)
        elif t <= 1.08:
            u = ((t - 0.85) / 0.23) ** 2.2
            pel_p = [0.0, lerp(5.8, -2.2, u), lerp(3.0, -8.5, u)]
            pel_r = [lerp(12, -14, u), lerp(0, -10, u), 0.0]
            wai_r = [lerp(10, -13, u), lerp(0, -10, u), 0.0]
            chs_r = [lerp(14, -15, u), lerp(0, -10, u), 0.0]
            hd_r  = [lerp(12, 22, u), lerp(0, 10, u), 0.0]
            jaw_r = [lerp(-44, -22, u), 0.0, 0.0]
            th_x, cr_x, sh_x, ft_x = lerp(14, 38, u), lerp(-24, -44, u), lerp(16, -10, u), lerp(40, 16, u)
            uar_r = [lerp(-16, 56, u), lerp(-22, 12, u), lerp(92, 12, u)]
            ufr_r = [lerp(12, 14, u), lerp(-10, 10, u), 0.0]
            if u <= 0.42:
                v = u / 0.42
                blade_dir = slerp_dir(b_zenith, b_crest, v)
                xhint = slerp_dir([0.25, -0.25, -0.93], [-0.10, -0.55, -0.83], v)
            else:
                v = (u - 0.42) / 0.58
                blade_dir = slerp_dir(b_crest, b_slam, v)
                xhint = slerp_dir([-0.10, -0.55, -0.83], [-0.12, -0.96, 0.25], v)
            slide = 10.0
            ual_r = [lerp(-8, 42, u), lerp(18, 16, u), lerp(-78, -28, u)]
            ufl_r = [lerp(12, 16, u), lerp(10, 6, u), 0.0]
            w_open = lerp(1.0, -0.5, u)
            swx, swz, cgx, cgz = lerp(8, -20, u), lerp(-14, -12, u), lerp(-8, 12, u), lerp(8, 8, u)
        elif t <= 1.44:
            u = (t - 1.08) / 0.36
            vib = math.sin(u * math.pi * 3.0) * math.exp(-3.0 * u)
            s = smooth5(u)
            pel_p = [0.0, -2.2 + 0.9 * s + 0.4 * vib, -8.5 + 1.5 * s]
            pel_r = [-14.0 + 3.0 * s - 2.5 * vib, -10.0, 0.0]
            wai_r = [-13.0 + 3.0 * s - 2.5 * vib, -10.0, 0.0]
            chs_r = [-15.0 + 3.0 * s + 2.5 * vib, -10.0, 0.0]
            hd_r  = [22.0 - 4.0 * s - 3.0 * vib, 10.0, 0.0]
            jaw_r = [-22.0 + 6.0 * vib, 0.0, 0.0]
            th_x, cr_x, sh_x, ft_x = 38 - 8 * s, -44 + 8 * s, -10 + 4 * s, 16
            uar_r = [56.0 - 5.0 * s + 3.0 * vib, 12.0, 12.0]
            ufr_r = [14.0, 10.0, 0.0]
            blade_dir = slerp_dir(b_slam, b_impact, s)
            xhint = [-0.12, -0.96, 0.25]
            slide = 10.0 - 1.0 * s
            ual_r = [42.0 - 6.0 * s + 4.0 * vib, 16.0, -28.0]
            ufl_r = [16.0, 6.0, 0.0]
            w_open = -0.5 + 0.5 * s + 0.25 * vib
            swx, swz = -18.0 * math.cos(u * math.pi * 2.5) * math.exp(-2.6 * u), -12.0 * math.exp(-2.6 * u)
            cgx, cgz = 12.0 * math.cos(u * math.pi * 2.5 - 0.6) * math.exp(-2.4 * u), 7.0 * math.exp(-2.4 * u)
        else:
            u = smooth5((t - 1.44) / 0.56)
            arc = math.sin(math.pi * u)
            pel_p = [0.0, lerp(-1.3, 0.0, u), lerp(-7.0, 0.0, u)]
            pel_r = [lerp(-11, 0, u), lerp(-10, 0, u), 0.0]
            wai_r = [lerp(-10, 0, u), lerp(-10, 0, u), 0.0]
            chs_r = [lerp(-12, 0, u), lerp(-10, 0, u), 0.0]
            hd_r  = [lerp(18, 0, u), lerp(10, 0, u), 0.0]
            jaw_r = [lerp(-22, -2, u), 0.0, 0.0]
            th_x, cr_x, sh_x, ft_x = lerp(30, 0, u), lerp(-36, 0, u), lerp(-6, 0, u), lerp(16, 0, u)
            uar_r = [lerp(51, 2, u) - 16 * arc, lerp(12, -2, u), lerp(12, 2, u) + 32 * arc]
            ufr_r = [lerp(14, 4, u) + 10 * arc, lerp(10, 4, u), 0.0]
            if u <= 0.55:
                v = u / 0.55
                blade_dir = slerp_dir(b_impact, b_lift, v)
                xhint = slerp_dir([-0.12, -0.96, 0.25], [0.85, 0.10, -0.50], v)
            else:
                v = (u - 0.55) / 0.45
                blade_dir = slerp_dir(b_lift, b_bind, v)
                xhint = slerp_dir([0.85, 0.10, -0.50], [1.0, 0.0, 0.0], v)
            slide = lerp(9.0, 0.0, u)
            ual_r = [lerp(36, 4, u), lerp(16, 2, u), lerp(-28, -4, u)]
            ufl_r = [lerp(16, 6, u), lerp(6, -4, u), 0.0]
            w_open = 0.0
            swx, swz, cgx, cgz = 0.0, 0.0, 0.0, 0.0

        splay = env_bell(t, 0.25, 1.15, 1.0)
        brace = env_bell(t, 0.82, 1.45, 1.0)
        rots = {
            "pelvis": pel_r, "waist": wai_r, "chest": chs_r,
            "neck": [hd_r[0] * 0.5, -hd_r[1] * 0.5, 0.0],
            "head": hd_r, "jaw": jaw_r,
            "halo_ring": [15.0 * splay, 0.0, 360.0 * smooth5(t / 2.0)],
            "core_crystal": [0.0, 360.0 * smooth5(t / 2.0), 0.0],
            "stole_right": [22.0 * max(0.0, -w_open) - 16.0 * max(0.0, w_open), 0.0, 22.0 * abs(w_open)],
            "stole_left": [22.0 * max(0.0, -w_open) - 16.0 * max(0.0, w_open), 0.0, -22.0 * abs(w_open)],
            "skirt_front": [24.0 * max(0.0, -w_open) - 10.0 * splay, 0.0, -14.0 * abs(w_open)],
            "skirt_back": [-24.0 * max(0.0, -w_open) - 12.0 * splay, 0.0, 14.0 * abs(w_open)],
            "thigh_right": [th_x, 0.0, 10.0 * splay], "crus_right": [cr_x, 0.0, 0.0], "shin_right": [sh_x, 0.0, 0.0], "foot_right": [ft_x, 0.0, 0.0],
            "thigh_left": [th_x, 0.0, -10.0 * splay], "crus_left": [cr_x, 0.0, 0.0], "shin_left": [sh_x, 0.0, 0.0], "foot_left": [ft_x, 0.0, 0.0],
            "upper_arm_right": uar_r, "upper_forearm_right": ufr_r,
            "upper_arm_left": ual_r, "upper_forearm_left": ufl_r,
            "lower_arm_right": [24.0 * w_open + 40.0 * brace, 24.0 * w_open + 12.0 * brace, 38.0 * max(0.0, w_open) + 12.0 * brace],
            "lower_forearm_right": [20.0 * abs(w_open) + 18.0 * brace, 0.0, 0.0],
            "lower_arm_left": [24.0 * w_open + 36.0 * max(0.0, -w_open), -24.0 * w_open, -38.0 * max(0.0, w_open)],
            "lower_forearm_left": [20.0 * abs(w_open), 0.0, 0.0],
            "wing_ring_right": [-8.0 * w_open, -40.0 * max(0.0, w_open) + 8.0 * max(0.0, -w_open), 28.0 * max(0.0, w_open) + 6.0 * max(0.0, -w_open)],
            "wing_outer_right": [-4.0 * max(0.0, w_open), -10.0 * max(0.0, w_open), 8.0 * max(0.0, w_open)],
            "wing_ring_left": [-8.0 * w_open, 40.0 * max(0.0, w_open) - 8.0 * max(0.0, -w_open), -28.0 * max(0.0, w_open) - 6.0 * max(0.0, -w_open)],
            "wing_outer_left": [-4.0 * max(0.0, w_open), 10.0 * max(0.0, w_open), -8.0 * max(0.0, w_open)],
            "tail_0": [18.0 * max(0.0, w_open) - 32.0 * max(0.0, -w_open), 0.0, 0.0],
            "tail_1": [16.0 * max(0.0, w_open) - 36.0 * env_bell(t, 0.92, 1.65), 0.0, 0.0],
            "tail_2": [14.0 * max(0.0, w_open) - 40.0 * env_bell(t, 0.98, 1.72), 0.0, 0.0],
            "tail_3": [12.0 * max(0.0, w_open) - 44.0 * env_bell(t, 1.04, 1.80), 0.0, 0.0],
        }
        hlb_r = aim_halberd_world(
            rots, ["pelvis", "waist", "chest", "upper_arm_right", "upper_forearm_right"],
            blade_dir, xhint,
        )
        rots["halberd"] = hlb_r
        hlb_pos = (euler_zyx_mat(*hlb_r) @ np.array([0.0, slide, 0.0])).tolist()
        bl, cage = compute_gravity_lantern(
            rots, ["pelvis", "waist", "chest", "upper_arm_left", "upper_forearm_left"],
            extra_world_swing_x=swx, extra_world_swing_z=swz,
            cage_lag_x=cgx, cage_lag_z=cgz,
        )
        rots["bell_lantern"] = bl
        rots["lantern_cage"] = cage
        return rots, {"pelvis": pel_p, "halberd": hlb_pos}

    clips = [
        bake_clip("animation.vaelkhor.idle", "idle", "loop", 4.0, eval_idle),
        bake_clip(
            "animation.vaelkhor.stride", "locomotion", "loop", 1.2, eval_stride,
            extra={"contralateral_pairs": [["thigh_left", "thigh_right"], ["upper_arm_left", "upper_arm_right"]]},
        ),
        bake_clip(
            "animation.vaelkhor.halberd_cleave", "attack", "play_once", 1.55, eval_cleave,
            beats={"anticipation": 0.48, "strike": 0.70, "impact": 0.92, "recovery": 1.55},
        ),
        bake_clip(
            "animation.vaelkhor.bell_toll_conduit", "attack", "play_once", 2.0, eval_bell,
            beats={"anticipation": 0.55, "strike": 0.82, "impact": 1.42, "recovery": 2.00},
        ),
        bake_clip(
            "animation.vaelkhor.leyline_cataclysm", "attack", "play_once", 2.0, eval_cataclysm,
            beats={"anticipation": 0.85, "strike": 1.08, "impact": 1.44, "recovery": 2.00},
        ),
    ]

    return {
        "schema_version": "1.0",
        "project": "Vaelkhor, the Leyline Sovereign Animations",
        "rig_id": "vaelkhor_leyline_sovereign",
        "fps": 20,
        "rig_bones": rig_bones,
        "joint_limits_deg": limits,
        "clips": clips,
    }


def deterministic_noise(x: int, y: int, seed: int) -> float:
    n = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def paint_region(img_arr: np.ndarray, x0: int, y0: int, w: int, h: int, mat: str, face_name: str, cid: str):
    palettes = {
        "obsidian_armor": ((26, 30, 42), (44, 48, 68), (15, 17, 25), (64, 52, 92)),
        "gilded_bronze": ((176, 126, 52), (224, 172, 74), (104, 68, 24), (242, 206, 114)),
        "verdigris_copper": ((50, 134, 122), (82, 182, 162), (28, 82, 74), (124, 224, 202)),
        "royal_vestment": ((64, 38, 112), (96, 58, 158), (36, 22, 66), (206, 164, 62)),
        "bone_ivory": ((208, 198, 176), (236, 228, 210), (140, 130, 112), (250, 246, 234)),
        "leyline_emissive": ((46, 228, 248), (128, 250, 255), (16, 168, 194), (232, 255, 255)),
        "amber_emissive": ((240, 156, 30), (255, 206, 86), (178, 92, 16), (255, 242, 176)),
    }
    base, hi, dark, accent = palettes[mat]
    seed = abs(hash((cid, face_name))) & 0xFFFF
    is_tattered = mat == "royal_vestment" and face_name in ("north", "south", "east", "west") and h >= 5
    for dy in range(h):
        for dx in range(w):
            px, py = x0 + dx, y0 + dy
            if px >= img_arr.shape[1] or py >= img_arr.shape[0]:
                continue
            r = deterministic_noise(dx, dy, seed)

            if is_tattered and ("skirt" in cid or "stole_front" in cid):
                notch = (dx * 5 + seed) % 4
                cut_rows = 2 if notch == 0 else (1 if notch in (1, 2) else 0)
                if dy >= h - cut_rows:
                    img_arr[py, px] = [0, 0, 0, 0]
                    continue

            is_border = (dx == 0 or dy == 0 or dx == w - 1 or dy == h - 1) and w >= 3 and h >= 3
            col = list(base)
            if face_name == "up":
                col = [int(base[i] * 0.65 + hi[i] * 0.35) for i in range(3)]
            elif face_name == "down":
                col = [int(base[i] * 0.6 + dark[i] * 0.4) for i in range(3)]
            elif dy <= max(0, h // 4):
                col = [int(base[i] * 0.72 + hi[i] * 0.28) for i in range(3)]

            if is_border:
                if dy == 0 and face_name not in ("down",):
                    col = [int(col[i] * 0.55 + hi[i] * 0.45) for i in range(3)]
                else:
                    col = [int(col[i] * 0.58 + dark[i] * 0.42) for i in range(3)]
            else:
                if mat == "royal_vestment":
                    if (dx % 2 == 0) and r > 0.35:
                        col = list(hi)
                    elif r < 0.2:
                        col = list(dark)
                elif mat in ("leyline_emissive", "amber_emissive"):
                    cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
                    dist = max(abs(dx - cx) / max(w / 2.0, 1.0), abs(dy - cy) / max(h / 2.0, 1.0))
                    if dist < 0.45:
                        col = list(accent)
                    elif dist < 0.78:
                        col = list(hi)
                elif mat == "gilded_bronze":
                    if (dx + dy) % 4 == 0 and r > 0.55:
                        col = list(accent)
                    elif r < 0.22:
                        col = [int(base[i] * 0.8 + dark[i] * 0.2) for i in range(3)]
                elif mat == "verdigris_copper":
                    if (dx == w // 2 or dy == h // 2) and w >= 4 and h >= 4:
                        col = [int(base[i] * 0.72 + dark[i] * 0.28) for i in range(3)]
                    elif r > 0.75:
                        col = list(hi)
                elif mat == "obsidian_armor":
                    if r > 0.84:
                        col = list(accent)
                    elif r < 0.25:
                        col = list(dark)
                elif mat == "bone_ivory":
                    if "snout" in cid and dy >= h - 2 and dx % 2 == 0:
                        col = list(dark)
                    elif r > 0.72:
                        col = list(hi)
                elif r > 0.76:
                    col = [min(255, int(c * 1.12)) for c in col]

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
            raise RuntimeError(f"Unified texture atlas overflow at {rw}x{rh}")
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
        faces_spec = {
            "north": (dx, dy), "south": (dx, dy),
            "east": (dz, dy), "west": (dz, dy),
            "up": (dx, dz), "down": (dx, dz),
        }
        face_uvs = {}
        for fname, (fw, fh) in faces_spec.items():
            ux, vy, rw, rh = alloc(fw, fh)
            paint_region(diffuse_arr, ux, vy, rw, rh, mat, fname, c["id"])
            if is_emis:
                paint_region(glow_arr, ux, vy, rw, rh, mat, fname, c["id"])
            face_uvs[fname] = {"uv": [ux, vy, ux + fw, vy + fh], "texture": 0}
        cube_uvs[c["id"]] = face_uvs

    diff_img = Image.fromarray(diffuse_arr, "RGBA")
    glow_img = Image.fromarray(glow_arr, "RGBA")
    diff_img.save(OUT_DIR / "vaelkhor_diffuse.png")
    glow_img.save(OUT_DIR / "vaelkhor_glow.png")
    return diff_img, glow_img, cube_uvs


def img_to_data_url(img: Image.Image) -> str:
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode("ascii")


def to_bbmodel(creature, anim_manifest, diff_img, cube_uvs):
    bone_uuids = {b["id"]: stable_uuid("bone:" + b["id"]) for b in creature["bones"]}
    cube_uuids = {c["id"]: stable_uuid("cube:" + c["id"]) for c in creature["cubes"]}

    mat_color_idx = {
        "obsidian_armor": 1, "gilded_bronze": 4, "verdigris_copper": 5,
        "royal_vestment": 7, "bone_ivory": 2, "leyline_emissive": 3, "amber_emissive": 4,
    }

    elements = []
    cubes_by_bone = {b["id"]: [] for b in creature["bones"]}
    bone_map = {b["id"]: b for b in creature["bones"]}

    for c in creature["cubes"]:
        cid = c["id"]
        bid = c["bone"]
        cubes_by_bone[bid].append(cube_uuids[cid])
        elem = {
            "name": cid, "box_uv": False, "render_order": "default", "locked": False,
            "export": True, "scope": 0, "allow_mirror_modeling": True,
            "from": c["from"], "to": c["to"], "autouv": 0,
            "color": mat_color_idx.get(c["material"], 0),
            "origin": c.get("origin", bone_map[bid]["pivot"]),
            "faces": cube_uvs[cid], "type": "cube", "uuid": cube_uuids[cid],
        }
        if c.get("rotation") is not None:
            elem["rotation"] = c["rotation"]
        if c.get("inflate"):
            elem["inflate"] = c["inflate"]
        elements.append(elem)

    groups = []
    child_bones = {b["id"]: [] for b in creature["bones"]}
    root_ids = []
    for b in creature["bones"]:
        bid = b["id"]
        if b["parent"] is None:
            root_ids.append(bid)
        else:
            child_bones[b["parent"]].append(bid)
        groups.append({
            "name": bid, "uuid": bone_uuids[bid], "export": True, "locked": False,
            "scope": 0, "selected": False, "visibility": True,
            "_static": {"properties": {}, "temp_data": {}},
            "origin": b["pivot"], "rotation": [0, 0, 0], "bedrock_binding": "",
            "color": 0, "children": [], "reset": False, "shade": True,
            "mirror_uv": False, "autouv": 0, "isOpen": True, "primary_selected": False,
        })

    def make_outliner_node(bid: str):
        kids = list(cubes_by_bone[bid])
        for sub in child_bones[bid]:
            kids.append(make_outliner_node(sub))
        return {"uuid": bone_uuids[bid], "isOpen": True, "children": kids}

    outliner = [make_outliner_node(r) for r in root_ids]

    textures = [
        {
            "name": "vaelkhor_diffuse.png", "path": "", "folder": "", "namespace": "",
            "id": "0", "group": "", "scope": 0, "width": ATLAS_W, "height": ATLAS_H,
            "uv_width": ATLAS_W, "uv_height": ATLAS_H, "particle": False,
            "use_as_default": True, "layers_enabled": False, "sync_to_project": "",
            "file_format": "png", "render_mode": "default", "render_sides": "auto",
            "wrap_mode": "limited", "pbr_channel": "color", "fps": 7,
            "force_translucent": False, "frame_time": 1, "frame_order_type": "loop",
            "frame_order": "", "frame_interpolate": False, "visible": True,
            "internal": True, "saved": False, "uuid": stable_uuid("tex:diffuse"),
            "source": img_to_data_url(diff_img),
        },
    ]

    bb_anims = []
    for clip in anim_manifest["clips"]:
        cid = clip["id"]
        animators = {}
        for bname, channels in clip["tracks"].items():
            buuid = bone_uuids[bname]
            kfs = []
            for ch_name, ch_map in channels.items():
                for t_str, vec in sorted(ch_map.items(), key=lambda kv: float(kv[0])):
                    kfs.append({
                        "channel": ch_name,
                        "data_points": [{"x": str(vec[0]), "y": str(vec[1]), "z": str(vec[2])}],
                        "uuid": stable_uuid(f"kf:{cid}:{bname}:{ch_name}:{t_str}"),
                        "time": float(t_str),
                        "color": -1,
                        "interpolation": "linear",
                    })
            animators[buuid] = {
                "name": bname, "type": "bone", "rotation_global": False,
                "quaternion_interpolation": False, "keyframes": kfs,
            }
        bb_anims.append({
            "uuid": stable_uuid("anim:" + cid), "name": cid,
            "loop": "loop" if clip["loop"] == "loop" else "once",
            "override": False, "length": clip["duration"], "snapping": 20,
            "selected": cid == "animation.vaelkhor.halberd_cleave",
            "saved": True, "path": "", "scope": 0, "anim_time_update": "",
            "blend_weight": "", "start_delay": "", "loop_delay": "", "animators": animators,
        })

    return {
        "meta": {"format_version": "5.0", "model_format": "bedrock", "box_uv": False},
        "name": "vaelkhor_leyline_sovereign",
        "model_identifier": "vaelkhor_leyline_sovereign",
        "visible_box": [3, 4.5, 0],
        "variable_placeholders": "", "multi_file_ruleset": "",
        "variable_placeholder_buttons": [], "bedrock_animation_mode": "entity",
        "timeline_setups": [], "unhandled_root_fields": {},
        "ai_used": False, "ai_agents": "",
        "resolution": {"width": ATLAS_W, "height": ATLAS_H},
        "elements": elements, "groups": groups, "outliner": outliner,
        "textures": textures, "animations": bb_anims,
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
                "identifier": "geometry.vaelkhor_leyline_sovereign",
                "texture_width": ATLAS_W, "texture_height": ATLAS_H,
                "visible_bounds_width": 4.2, "visible_bounds_height": 4.4,
                "visible_bounds_offset": [0, 2.2, 0],
            },
            "bones": bones_out,
        }],
    }


def to_animation_json(anim_manifest):
    anims_out = {}
    for clip in anim_manifest["clips"]:
        bones_out = {}
        for bname, channels in clip["tracks"].items():
            bdata = {}
            for ch_name, ch_map in channels.items():
                bdata[ch_name] = {
                    t_str: list(vec) for t_str, vec in sorted(ch_map.items(), key=lambda kv: float(kv[0]))
                }
            bones_out[bname] = bdata
        entry = {"animation_length": clip["duration"], "bones": bones_out}
        if clip["loop"] == "loop":
            entry["loop"] = True
        anims_out[clip["id"]] = entry
    return {"format_version": "1.8.0", "animations": anims_out}


def verify_fk_kinematics(creature, anim_manifest):
    bone_map = {b["id"]: b for b in creature["bones"]}
    bind_pts = {
        "hand_r": ("halberd", np.array([16.5, 33.4, -4.0])),
        "shaft_mid": ("halberd", np.array([16.5, 39.0, -4.0])),
        "blade_tip": ("halberd", np.array([18.2, 61.5, -4.0])),
        "butt_tip": ("halberd", np.array([16.5, 0.5, -4.0])),
        "hand_l": ("upper_forearm_left", np.array([-16.5, 32.2, -4.0])),
        "lantern_ctr": ("lantern_cage", np.array([-16.5, 18.0, -4.0])),
    }

    def eval_pose(clip_id: str, t_target: float):
        clip = next(c for c in anim_manifest["clips"] if c["id"] == clip_id)
        rots, poss = {}, {}
        for bid, chs in clip["tracks"].items():
            if "rotation" in chs:
                ts = sorted(chs["rotation"].keys(), key=float)
                best = min(ts, key=lambda s: abs(float(s) - t_target))
                rots[bid] = chs["rotation"][best]
            if "position" in chs:
                ts = sorted(chs["position"].keys(), key=float)
                best = min(ts, key=lambda s: abs(float(s) - t_target))
                poss[bid] = chs["position"][best]

        world_tr = {}
        def get_tr(bid):
            if bid in world_tr:
                return world_tr[bid]
            b = bone_map[bid]
            piv = np.array(b["pivot"], dtype=float)
            R_loc = euler_zyx_mat(*rots.get(bid, [0.0, 0.0, 0.0]))
            pos_loc = np.array(poss.get(bid, [0.0, 0.0, 0.0]), dtype=float)
            if b["parent"] is None:
                pw = pos_loc
                Rw = R_loc
            else:
                pp, Rp = get_tr(b["parent"])
                par_piv = np.array(bone_map[b["parent"]]["pivot"], dtype=float)
                pw = pp + Rp @ ((piv - par_piv) + pos_loc)
                Rw = Rp @ R_loc
            world_tr[bid] = (pw, Rw)
            return pw, Rw

        out = {}
        for k, (bid, pt) in bind_pts.items():
            pw, Rw = get_tr(bid)
            out[k] = pw + Rw @ (pt - np.array(bone_map[bid]["pivot"], dtype=float))
        _, R_hlb = get_tr("halberd")
        out["edge_dir"] = R_hlb @ np.array([1.0, 0.0, 0.0])
        return {k: [round(float(v), 2) for v in arr] for k, arr in out.items()}

    print("=== 3D FK KINEMATIC AUDIT ===")
    for cid, times in [
        ("animation.vaelkhor.halberd_cleave", [0.48, 0.60, 0.70, 1.15]),
        ("animation.vaelkhor.bell_toll_conduit", [0.55, 0.80, 1.05]),
        ("animation.vaelkhor.leyline_cataclysm", [0.85, 1.08]),
    ]:
        for tt in times:
            p = eval_pose(cid, tt)
            print(f"  {cid.split('.')[-1]}@{tt:.2f}s: handR={p['hand_r']} shaft={p['shaft_mid']} blade={p['blade_tip']} edge={p['edge_dir']} handL={p['hand_l']} cage={p['lantern_ctr']}")


def main():
    creature = build_creature_manifest()
    creature_path = OUT_DIR / "creature_manifest.json"
    creature_path.write_text(json.dumps(creature, indent=2) + "\n", encoding="utf-8")

    res_c = subprocess.run([sys.executable, str(VALIDATOR_CREATURE), str(creature_path)], check=False, text=True, capture_output=True)
    print(res_c.stdout.strip())
    if res_c.stderr:
        print(res_c.stderr.strip())
    if res_c.returncode != 0:
        sys.exit(res_c.returncode)

    anim_manifest = build_animation_manifest(creature)
    anim_path = OUT_DIR / "animation_manifest.json"
    anim_path.write_text(json.dumps(anim_manifest, indent=2) + "\n", encoding="utf-8")

    res_a = subprocess.run([sys.executable, str(VALIDATOR_ANIM), str(anim_path)], check=False, text=True, capture_output=True)
    print(res_a.stdout.strip())
    if res_a.stderr:
        print(res_a.stderr.strip())
    if res_a.returncode != 0:
        sys.exit(res_a.returncode)

    verify_fk_kinematics(creature, anim_manifest)

    diff_img, glow_img, cube_uvs = pack_and_paint_textures(creature["cubes"])
    bbmodel = to_bbmodel(creature, anim_manifest, diff_img, cube_uvs)
    (OUT_DIR / "vaelkhor_leyline_sovereign.bbmodel").write_text(json.dumps(bbmodel, indent=2) + "\n", encoding="utf-8")
    (OUT_DIR / "vaelkhor_leyline_sovereign.geo.json").write_text(json.dumps(to_geo_json(creature, cube_uvs), indent=2) + "\n", encoding="utf-8")
    (OUT_DIR / "vaelkhor_leyline_sovereign.animation.json").write_text(json.dumps(to_animation_json(anim_manifest), indent=2) + "\n", encoding="utf-8")
    print(f"Built 40-bone 20TPS fluid Vaelkhor: {len(creature['bones'])} bones, {len(creature['cubes'])} cubes, {len(anim_manifest['clips'])} dense clips.")


if __name__ == "__main__":
    main()
