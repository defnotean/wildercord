#!/usr/bin/env python3
"""Build 40-bone articulated Nyxara, the Eclipse Sovereign rig, textures, and 5 grounded 20 TPS animations."""
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
REF_IMG = ARTIFACT_DIR / "nyxara_eclipse_sovereign_turnaround_final_1795610332598.jpg"
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
        "id": cid, "bone": bone, "material": mat,
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
    # 40 bones aligned to nyxara_eclipse_sovereign_turnaround_final.jpg (height ~64px + upper halo to ~68px)
    bones = [
        {"id": "root", "parent": None, "pivot": [0, 0, 0]},
        {"id": "pelvis", "parent": "root", "pivot": [0, 24.5, 0.0]},
        {"id": "skirt_front", "parent": "pelvis", "pivot": [0, 24.0, -3.5]},
        {"id": "skirt_back", "parent": "pelvis", "pivot": [0, 24.0, 3.5]},
        {"id": "skirt_right", "parent": "pelvis", "pivot": [5.5, 24.0, 0.0], "mirror_of": "skirt_left"},
        {"id": "skirt_left", "parent": "pelvis", "pivot": [-5.5, 24.0, 0.0], "mirror_of": "skirt_right"},
        {"id": "thigh_right", "parent": "pelvis", "pivot": [3.5, 23.0, 0.0], "mirror_of": "thigh_left"},
        {"id": "thigh_left", "parent": "pelvis", "pivot": [-3.5, 23.0, 0.0], "mirror_of": "thigh_right"},
        {"id": "knee_right", "parent": "thigh_right", "pivot": [3.5, 12.5, -2.5], "mirror_of": "knee_left"},
        {"id": "knee_left", "parent": "thigh_left", "pivot": [-3.5, 12.5, -2.5], "mirror_of": "knee_right"},
        {"id": "shin_right", "parent": "thigh_right", "pivot": [3.5, 11.5, 0.0], "mirror_of": "shin_left"},
        {"id": "shin_left", "parent": "thigh_left", "pivot": [-3.5, 11.5, 0.0], "mirror_of": "shin_right"},
        {"id": "foot_right", "parent": "shin_right", "pivot": [3.5, 2.5, -0.5], "mirror_of": "foot_left"},
        {"id": "foot_left", "parent": "shin_left", "pivot": [-3.5, 2.5, -0.5], "mirror_of": "foot_right"},
        # Slender Celestial Torso & Astrolabe Core
        {"id": "waist", "parent": "pelvis", "pivot": [0, 27.0, 0.0]},
        {"id": "chest", "parent": "waist", "pivot": [0, 35.0, 0.0]},
        {"id": "astral_core", "parent": "chest", "pivot": [0, 38.5, -3.6]},
        # Flowing Bifurcated Cloak
        {"id": "cloak_top", "parent": "chest", "pivot": [0, 44.0, 3.8]},
        {"id": "cloak_tail_right", "parent": "cloak_top", "pivot": [4.5, 38.0, 5.2], "mirror_of": "cloak_tail_left"},
        {"id": "cloak_tail_left", "parent": "cloak_top", "pivot": [-4.5, 38.0, 5.2], "mirror_of": "cloak_tail_right"},
        {"id": "cloak_hem_right", "parent": "cloak_tail_right", "pivot": [6.5, 21.0, 6.2], "mirror_of": "cloak_hem_left"},
        {"id": "cloak_hem_left", "parent": "cloak_tail_left", "pivot": [-6.5, 21.0, 6.2], "mirror_of": "cloak_hem_right"},
        # Floating Concentric Golden Astrolabe Halo Rings
        {"id": "halo_outer", "parent": "chest", "pivot": [0, 53.0, 4.8]},
        {"id": "halo_inner", "parent": "chest", "pivot": [0, 53.0, 4.2]},
        # Head, Silver Mask, Crescent Brow Crown
        {"id": "neck", "parent": "chest", "pivot": [0, 45.5, 0.0]},
        {"id": "head", "parent": "neck", "pivot": [0, 47.5, 0.0]},
        {"id": "crescent_crown", "parent": "head", "pivot": [0, 55.5, -4.4]},
        {"id": "brow_wing_right", "parent": "head", "pivot": [4.8, 52.0, -3.5], "mirror_of": "brow_wing_left"},
        {"id": "brow_wing_left", "parent": "head", "pivot": [-4.8, 52.0, -3.5], "mirror_of": "brow_wing_right"},
        # Ceremonial Shoulders, Arms, & Dual Crescent Eclipse Twinblades
        {"id": "pauldron_right", "parent": "chest", "pivot": [8.5, 44.5, 0.0], "mirror_of": "pauldron_left"},
        {"id": "pauldron_left", "parent": "chest", "pivot": [-8.5, 44.5, 0.0], "mirror_of": "pauldron_right"},
        {"id": "upper_arm_right", "parent": "chest", "pivot": [8.5, 43.0, 0.0], "mirror_of": "upper_arm_left"},
        {"id": "upper_arm_left", "parent": "chest", "pivot": [-8.5, 43.0, 0.0], "mirror_of": "upper_arm_right"},
        {"id": "forearm_right", "parent": "upper_arm_right", "pivot": [9.2, 34.0, 0.0], "mirror_of": "forearm_left"},
        {"id": "forearm_left", "parent": "upper_arm_left", "pivot": [-9.2, 34.0, 0.0], "mirror_of": "forearm_right"},
        {"id": "blade_right", "parent": "forearm_right", "pivot": [10.0, 26.0, -1.5], "mirror_of": "blade_left"},
        {"id": "blade_left", "parent": "forearm_left", "pivot": [-10.0, 26.0, -1.5], "mirror_of": "blade_right"},
        {"id": "blade_r_tip", "parent": "blade_right", "pivot": [12.5, 16.0, -6.5], "mirror_of": "blade_l_tip"},
        {"id": "blade_l_tip", "parent": "blade_left", "pivot": [-12.5, 16.0, -6.5], "mirror_of": "blade_r_tip"},
        {"id": "eclipse_orb", "parent": "halo_inner", "pivot": [0, 64.0, 4.5]},
    ]

    cubes = []

    # Pelvis, Sash & Pleated Astral Tassets
    cubes.append(make_cube("pelvis_block", "pelvis", "void_obsidian", [-5.8, 22.5, -3.2], [5.8, 26.8, 3.2]))
    cubes.append(make_cube("sash_belt", "pelvis", "silver_trim", [-6.2, 24.4, -3.6], [6.2, 26.2, 3.6], inflate=0.05))
    cubes.append(make_cube("sash_clasp", "pelvis", "star_gold", [-1.6, 24.0, -4.1], [1.6, 26.5, -3.1]))

    cubes.append(make_cube("skirt_f_center", "skirt_front", "twilight_indigo", [-3.2, 13.5, -3.8], [3.2, 24.5, -2.9]))
    cubes.append(make_cube("skirt_f_trim", "skirt_front", "silver_trim", [-3.4, 13.0, -4.0], [3.4, 14.5, -2.8]))
    cubes.append(make_cube("skirt_b_panel", "skirt_back", "twilight_indigo", [-4.5, 14.0, 2.8], [4.5, 24.5, 3.8]))
    cubes.extend(mirror_cube("skirt_r_pleat", "skirt_l_pleat", "skirt_right", "skirt_left", "twilight_indigo", [3.6, 13.0, -3.0], [6.2, 24.6, 3.0], rotation=[0, 0, -9]))
    cubes.extend(mirror_cube("skirt_r_edge", "skirt_l_edge", "skirt_right", "skirt_left", "silver_trim", [5.7, 12.8, -3.1], [6.5, 24.0, 3.1], rotation=[0, 0, -9]))

    # Slender Articulated Greaves & Grounded Sabatons (Y = 0)
    cubes.extend(mirror_cube("thigh_r_body", "thigh_l_body", "thigh_right", "thigh_left", "void_obsidian", [1.4, 12.0, -2.4], [5.4, 23.0, 2.4]))
    cubes.extend(mirror_cube("knee_r_crest", "knee_l_crest", "knee_right", "knee_left", "silver_trim", [1.8, 10.6, -3.3], [5.0, 14.2, -1.9]))
    cubes.extend(mirror_cube("shin_r_greave", "shin_l_greave", "shin_right", "shin_left", "void_obsidian", [1.5, 3.2, -2.5], [5.3, 12.0, 2.5]))
    cubes.extend(mirror_cube("shin_r_trim", "shin_l_trim", "shin_right", "shin_left", "silver_trim", [1.7, 4.0, -2.8], [5.1, 10.5, -2.0]))
    cubes.extend(mirror_cube("foot_r_heel", "foot_l_heel", "foot_right", "foot_left", "void_obsidian", [1.5, 0.0, -1.8], [5.3, 3.4, 2.8]))
    cubes.extend(mirror_cube("foot_r_toe", "foot_l_toe", "foot_right", "foot_left", "silver_trim", [1.8, 0.0, -4.4], [5.0, 2.4, -1.6]))

    # Torso, Breastplate, Glowing Swirl Astrolabe Core
    cubes.append(make_cube("waist_core", "waist", "void_obsidian", [-4.6, 26.5, -2.8], [4.6, 35.2, 2.8]))
    cubes.append(make_cube("waist_ribs_gold", "waist", "star_gold", [-4.8, 28.0, -3.0], [4.8, 30.0, 3.0]))
    cubes.append(make_cube("chest_cuirass", "chest", "void_obsidian", [-6.6, 35.0, -3.6], [6.6, 45.2, 3.6]))
    cubes.append(make_cube("chest_mantle_plate", "chest", "twilight_indigo", [-7.2, 39.2, -4.1], [7.2, 45.6, 4.0]))
    cubes.append(make_cube("chest_gold_collar", "chest", "star_gold", [-5.4, 43.8, -4.4], [5.4, 46.0, 4.2], inflate=0.05))
    cubes.append(make_cube("core_bezel", "astral_core", "silver_trim", [-3.1, 36.2, -4.6], [3.1, 42.2, -3.5]))
    cubes.append(make_cube("core_orb", "astral_core", "astral_emissive", [-2.2, 37.1, -5.0], [2.2, 41.3, -4.0]))

    # Flowing Bifurcated Cloak
    cubes.append(make_cube("cloak_shawl", "cloak_top", "twilight_indigo", [-7.6, 41.0, 3.0], [7.6, 45.8, 5.4]))
    cubes.append(make_cube("cloak_shawl_gold", "cloak_top", "star_gold", [-6.8, 43.2, 4.8], [6.8, 45.4, 5.7]))
    cubes.extend(mirror_cube("cloak_u_r", "cloak_u_l", "cloak_tail_right", "cloak_tail_left", "twilight_indigo", [1.6, 21.0, 4.2], [8.8, 42.0, 5.8], rotation=[6, 0, -6]))
    cubes.extend(mirror_cube("cloak_m_r", "cloak_m_l", "cloak_tail_right", "cloak_tail_left", "void_obsidian", [2.6, 22.0, 4.8], [5.4, 41.0, 6.1], rotation=[6, 0, -6]))
    cubes.extend(mirror_cube("cloak_l_r", "cloak_l_l", "cloak_hem_right", "cloak_hem_left", "twilight_indigo", [2.2, 8.0, 5.4], [9.8, 22.0, 7.0], rotation=[8, 0, -8]))
    cubes.extend(mirror_cube("cloak_trim_r", "cloak_trim_l", "cloak_hem_right", "cloak_hem_left", "astral_emissive", [8.8, 8.2, 5.5], [10.0, 21.0, 6.8], rotation=[8, 0, -8]))

    # Floating Concentric Golden Astrolabe Halo Rings (16 overlapping segments each for clean circle)
    for i in range(16):
        ang = math.radians(i * 22.5)
        ox, oy = math.cos(ang) * 11.2, 53.0 + math.sin(ang) * 11.2
        cubes.append(make_cube(f"halo_out_{i}", "halo_outer", "star_gold",
                               [ox - 1.2, oy - 2.3, 4.3], [ox + 1.2, oy + 2.3, 5.5],
                               rotation=[0, 0, i * 22.5], origin=[ox, oy, 4.9]))
        ix, iy = math.cos(ang) * 7.8, 53.0 + math.sin(ang) * 7.8
        cubes.append(make_cube(f"halo_in_{i}", "halo_inner", "star_gold",
                               [ix - 0.9, iy - 1.7, 3.8], [ix + 0.9, iy + 1.7, 4.8],
                               rotation=[0, 0, i * 22.5], origin=[ix, iy, 4.3]))
    cubes.append(make_cube("halo_apex_star", "eclipse_orb", "astral_emissive", [-1.4, 62.6, 3.6], [1.4, 65.4, 5.4]))

    # Head, Silver Mask & Crescent Brow Crown
    cubes.append(make_cube("neck_post", "neck", "void_obsidian", [-2.4, 45.0, -2.2], [2.4, 48.0, 2.2]))
    cubes.append(make_cube("head_helm", "head", "void_obsidian", [-4.2, 47.4, -4.2], [4.2, 56.2, 4.2]))
    cubes.append(make_cube("mask_face", "head", "silver_trim", [-3.8, 47.8, -4.6], [3.8, 54.0, -3.5]))
    cubes.append(make_cube("mask_slot", "head", "void_obsidian", [-0.9, 47.7, -4.8], [0.9, 51.4, -4.0]))
    cubes.append(make_cube("eye_r", "head", "astral_emissive", [1.3, 51.6, -4.8], [3.4, 53.1, -4.0]))
    cubes.append(make_cube("eye_l", "head", "astral_emissive", [-3.4, 51.6, -4.8], [-1.3, 53.1, -4.0]))
    cubes.append(make_cube("crown_band", "crescent_crown", "star_gold", [-4.5, 54.2, -4.6], [4.5, 55.6, 4.5], inflate=0.04))
    cubes.append(make_cube("crescent_horn_r", "crescent_crown", "star_gold", [1.2, 55.2, -5.0], [3.4, 59.0, -3.8], rotation=[0, 0, 18]))
    cubes.append(make_cube("crescent_horn_l", "crescent_crown", "star_gold", [-3.4, 55.2, -5.0], [-1.2, 59.0, -3.8], rotation=[0, 0, -18]))
    cubes.extend(mirror_cube("brow_wing_r", "brow_wing_l", "brow_wing_right", "brow_wing_left", "silver_trim", [4.0, 50.0, -3.8], [5.1, 53.8, 1.5]))

    # Ceremonial Shoulders, Arms, Gauntlets
    cubes.extend(mirror_cube("pauldron_r_main", "pauldron_l_main", "pauldron_right", "pauldron_left", "twilight_indigo", [6.2, 42.0, -4.2], [11.6, 46.8, 4.2], rotation=[0, 0, -8]))
    cubes.extend(mirror_cube("pauldron_r_gold", "pauldron_l_gold", "pauldron_right", "pauldron_left", "star_gold", [6.6, 46.2, -4.4], [11.2, 47.8, 4.4], rotation=[0, 0, -8]))
    cubes.extend(mirror_cube("uarm_r", "uarm_l", "upper_arm_right", "upper_arm_left", "void_obsidian", [7.0, 34.0, -2.3], [11.0, 43.0, 2.3]))
    cubes.extend(mirror_cube("farm_r", "farm_l", "forearm_right", "forearm_left", "silver_trim", [7.4, 25.4, -2.5], [11.4, 34.2, 2.5]))
    cubes.extend(mirror_cube("farm_r_cuff", "farm_l_cuff", "forearm_right", "forearm_left", "twilight_indigo", [7.2, 28.0, -2.7], [11.6, 33.2, 2.7]))

    # Dual Curved Crescent Eclipse Twinblades
    cubes.extend(mirror_cube("hilt_r", "hilt_l", "blade_right", "blade_left", "star_gold", [9.2, 23.0, -4.5], [10.8, 27.6, 1.5], rotation=[38, 0, 12], origin=[10.0, 25.5, -1.5]))
    cubes.extend(mirror_cube("blade_r_mid", "blade_l_mid", "blade_right", "blade_left", "void_obsidian", [10.4, 15.5, -6.2], [11.8, 24.0, -0.8], rotation=[32, 0, 16], origin=[11.0, 20.0, -3.5]))
    cubes.extend(mirror_cube("blade_r_edge", "blade_l_edge", "blade_right", "blade_left", "astral_emissive", [10.7, 15.8, -6.8], [11.5, 23.8, -5.6], rotation=[32, 0, 16], origin=[11.0, 20.0, -3.5]))
    cubes.extend(mirror_cube("blade_r_sweep", "blade_l_sweep", "blade_r_tip", "blade_l_tip", "silver_trim", [11.6, 11.2, -10.8], [12.9, 17.5, -4.2], rotation=[52, 0, 22], origin=[12.2, 14.5, -7.2]))
    cubes.extend(mirror_cube("blade_r_rune", "blade_l_rune", "blade_r_tip", "blade_l_tip", "astral_emissive", [11.8, 12.2, -11.4], [12.7, 16.5, -9.8], rotation=[52, 0, 22], origin=[12.2, 14.5, -7.2]))

    materials = [
        {"id": "void_obsidian", "type": "opaque"},
        {"id": "twilight_indigo", "type": "opaque"},
        {"id": "silver_trim", "type": "opaque"},
        {"id": "star_gold", "type": "opaque"},
        {"id": "astral_emissive", "type": "emissive"},
    ]
    return {
        "creature_id": "nyxara_eclipse_sovereign",
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

    # 1. Idle (2.4s loop): graceful hover-breathe + counter-rotating astrolabe halos + cape sway
    dur_idle = 2.4
    tr_idle = {b: {"rotation": {}, "position": {}} for b in (
        "pelvis", "waist", "chest", "head", "halo_outer", "halo_inner",
        "upper_arm_right", "forearm_right", "upper_arm_left", "forearm_left",
        "cloak_tail_right", "cloak_tail_left", "cloak_hem_right", "cloak_hem_left"
    )}
    for t in _sample_key(dur_idle):
        is_end = abs(t - dur_idle) < 1e-4
        ph = 0.0 if (t == 0.0 or is_end) else 2.0 * math.pi * (t / dur_idle)
        key = "0.0" if t == 0.0 else (f"{dur_idle}" if is_end else f"{t:.2f}".rstrip("0").rstrip("."))
        s1, c1 = math.sin(ph), math.cos(ph)
        tr_idle["pelvis"]["position"][key] = [0.0, round(0.45 * math.sin(2.0 * ph), 3), 0.0]
        tr_idle["pelvis"]["rotation"][key] = [round(-1.5 * s1, 3), round(2.5 * c1, 3), 0.0]
        tr_idle["waist"]["rotation"][key] = [round(2.0 * s1, 3), round(-2.0 * c1, 3), 0.0]
        tr_idle["chest"]["rotation"][key] = [round(-3.0 + 2.5 * s1, 3), round(2.5 * s1, 3), 0.0]
        tr_idle["head"]["rotation"][key] = [round(2.0 * s1, 3), round(-4.0 * c1, 3), 0.0]
        tr_idle["halo_outer"]["rotation"][key] = [0.0, 0.0, round(15.0 * s1, 3)]
        tr_idle["halo_inner"]["rotation"][key] = [0.0, 0.0, round(-22.0 * s1, 3)]
        tr_idle["upper_arm_right"]["rotation"][key] = [round(6.0 + 4.0 * s1, 3), 0.0, round(-8.0 - 2.0 * c1, 3)]
        tr_idle["forearm_right"]["rotation"][key] = [round(10.0 + 3.0 * c1, 3), 0.0, 0.0]
        tr_idle["upper_arm_left"]["rotation"][key] = [round(6.0 - 4.0 * s1, 3), 0.0, round(8.0 + 2.0 * c1, 3)]
        tr_idle["forearm_left"]["rotation"][key] = [round(10.0 - 3.0 * c1, 3), 0.0, 0.0]
        tr_idle["cloak_tail_right"]["rotation"][key] = [round(4.0 + 4.0 * s1, 3), 0.0, round(-2.0 * c1, 3)]
        tr_idle["cloak_tail_left"]["rotation"][key] = [round(4.0 - 4.0 * s1, 3), 0.0, round(2.0 * c1, 3)]
        tr_idle["cloak_hem_right"]["rotation"][key] = [round(5.0 * c1, 3), 0.0, 0.0]
        tr_idle["cloak_hem_left"]["rotation"][key] = [round(-5.0 * c1, 3), 0.0, 0.0]

    clips.append({
        "id": "animation.nyxara.idle",
        "kind": "idle",
        "duration": dur_idle,
        "loop": "loop",
        "tracks": {b: {k: v for k, v in ch.items() if v} for b, ch in tr_idle.items()},
    })

    # 2. Stride (1.0s smooth sword-dancer glide step loop)
    dur_stride = 1.0
    tr_str = {b: {"rotation": {}, "position": {}} for b in (
        "pelvis", "thigh_right", "thigh_left", "shin_right", "shin_left", "foot_right", "foot_left",
        "waist", "chest", "head", "halo_outer", "halo_inner",
        "upper_arm_right", "forearm_right", "upper_arm_left", "forearm_left",
        "cloak_tail_right", "cloak_tail_left", "skirt_front"
    )}
    for t in _sample_key(dur_stride):
        is_end = abs(t - dur_stride) < 1e-4
        ph = 0.0 if (t == 0.0 or is_end) else 2.0 * math.pi * (t / dur_stride)
        key = "0.0" if t == 0.0 else (f"{dur_stride}" if is_end else f"{t:.2f}".rstrip("0").rstrip("."))
        s1, c1 = math.sin(ph), math.cos(ph)
        s2 = math.sin(2.0 * ph)
        tr_str["pelvis"]["position"][key] = [round(0.8 * s1, 3), round(-0.4 + 0.7 * abs(s1), 3), round(-1.0 * abs(c1), 3)]
        tr_str["pelvis"]["rotation"][key] = [round(-4.0, 3), round(8.0 * c1, 3), round(2.5 * s1, 3)]
        tr_str["thigh_right"]["rotation"][key] = [round(28.0 * s1, 3), 0.0, 0.0]
        tr_str["thigh_left"]["rotation"][key] = [round(-28.0 * s1, 3), 0.0, 0.0]
        tr_str["shin_right"]["rotation"][key] = [round(-8.0 - 18.0 * max(0.0, -c1), 3), 0.0, 0.0]
        tr_str["shin_left"]["rotation"][key] = [round(-8.0 - 18.0 * max(0.0, c1), 3), 0.0, 0.0]
        tr_str["foot_right"]["rotation"][key] = [round(-10.0 * s1, 3), 0.0, 0.0]
        tr_str["foot_left"]["rotation"][key] = [round(10.0 * s1, 3), 0.0, 0.0]
        tr_str["waist"]["rotation"][key] = [round(-4.0, 3), round(-8.0 * c1, 3), 0.0]
        tr_str["chest"]["rotation"][key] = [round(-6.0, 3), round(-10.0 * c1, 3), round(-2.0 * s1, 3)]
        tr_str["head"]["rotation"][key] = [round(8.0, 3), round(10.0 * c1, 3), 0.0]
        tr_str["halo_outer"]["rotation"][key] = [0.0, 0.0, round(24.0 * s1, 3)]
        tr_str["halo_inner"]["rotation"][key] = [0.0, 0.0, round(-32.0 * s1, 3)]
        tr_str["upper_arm_right"]["rotation"][key] = [round(-12.0 - 20.0 * c1, 3), 0.0, round(-12.0, 3)]
        tr_str["forearm_right"]["rotation"][key] = [round(16.0, 3), 0.0, 0.0]
        tr_str["upper_arm_left"]["rotation"][key] = [round(-12.0 + 20.0 * c1, 3), 0.0, round(12.0, 3)]
        tr_str["forearm_left"]["rotation"][key] = [round(16.0, 3), 0.0, 0.0]
        tr_str["cloak_tail_right"]["rotation"][key] = [round(14.0 + 6.0 * s2, 3), 0.0, 0.0]
        tr_str["cloak_tail_left"]["rotation"][key] = [round(14.0 + 6.0 * s2, 3), 0.0, 0.0]
        tr_str["skirt_front"]["rotation"][key] = [round(10.0 + 8.0 * abs(s1), 3), 0.0, 0.0]

    clips.append({
        "id": "animation.nyxara.stride",
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

    # 3. Crescent Sever (1.50s = 30 ticks): coil twin blades -> explosive crossed forward (-Z) scissor slash -> flourish recover
    dur_sever = 1.50
    clips.append({
        "id": "animation.nyxara.crescent_sever",
        "kind": "attack",
        "duration": dur_sever,
        "loop": "play_once",
        "beats": {"anticipation": 0.45, "strike": 0.65, "impact": 0.85, "recovery": 1.35},
        "tracks": bake_keyframe_table(dur_sever, {
            "pelvis": [(0.0, [0, 0, 0], [0, 0, 0]), (0.45, [2, -22, 0], [0, -1.2, 1.5]), (0.85, [-10, 18, 0], [0, -2.0, -4.5]), (1.50, [0, 0, 0], [0, 0, 0])],
            "thigh_right": [(0.0, [0, 0, 0], None), (0.85, [24, 0, 0], None), (1.50, [0, 0, 0], None)],
            "shin_right": [(0.0, [0, 0, 0], None), (0.85, [-22, 0, 0], None), (1.50, [0, 0, 0], None)],
            "thigh_left": [(0.0, [0, 0, 0], None), (0.85, [-18, 0, 0], None), (1.50, [0, 0, 0], None)],
            "waist": [(0.0, [0, 0, 0], None), (0.45, [3, -18, 0], None), (0.85, [-12, 16, 0], None), (1.50, [0, 0, 0], None)],
            "chest": [(0.0, [0, 0, 0], None), (0.45, [4, -20, 0], None), (0.85, [-14, 18, 0], None), (1.50, [0, 0, 0], None)],
            "head": [(0.0, [0, 0, 0], None), (0.45, [-4, 28, 0], None), (0.85, [14, -18, 0], None), (1.50, [0, 0, 0], None)],
            "upper_arm_right": [(0.0, [0, 0, 0], None), (0.45, [142, -20, -32], None), (0.65, [86, 18, -10], None), (0.85, [28, 28, 16], None), (1.50, [0, 0, 0], None)],
            "forearm_right": [(0.0, [10, 0, 0], None), (0.45, [28, 0, 0], None), (0.85, [12, 0, 0], None), (1.50, [10, 0, 0], None)],
            "blade_right": [(0.0, [0, 0, 0], None), (0.85, [32, 0, 12], None), (1.50, [0, 0, 0], None)],
            "upper_arm_left": [(0.0, [0, 0, 0], None), (0.45, [142, 20, 32], None), (0.65, [86, -18, 10], None), (0.85, [28, -28, -16], None), (1.50, [0, 0, 0], None)],
            "forearm_left": [(0.0, [10, 0, 0], None), (0.45, [28, 0, 0], None), (0.85, [12, 0, 0], None), (1.50, [10, 0, 0], None)],
            "blade_left": [(0.0, [0, 0, 0], None), (0.85, [32, 0, -12], None), (1.50, [0, 0, 0], None)],
            "halo_outer": [(0.0, [0, 0, 0], None), (0.85, [0, 0, 90], None), (1.50, [0, 0, 0], None)],
            "cloak_tail_right": [(0.0, [0, 0, 0], None), (0.85, [28, 0, -14], None), (1.50, [0, 0, 0], None)],
            "cloak_tail_left": [(0.0, [0, 0, 0], None), (0.85, [28, 0, 14], None), (1.50, [0, 0, 0], None)],
        }),
    })

    # 4. Prism Convergence (2.00s = 40 ticks): float upward slightly + cross arms over chest core + flare halo rings
    dur_prism = 2.00
    clips.append({
        "id": "animation.nyxara.prism_convergence",
        "kind": "attack",
        "duration": dur_prism,
        "loop": "play_once",
        "beats": {"anticipation": 0.45, "strike": 0.90, "impact": 1.35, "recovery": 1.85},
        "tracks": bake_keyframe_table(dur_prism, {
            "pelvis": [(0.0, [0, 0, 0], [0, 0, 0]), (0.45, [-6, 0, 0], [0, -2.0, 0]), (1.10, [8, 0, 0], [0, 3.0, 0]), (2.00, [0, 0, 0], [0, 0, 0])],
            "chest": [(0.0, [0, 0, 0], None), (0.45, [-8, 0, 0], None), (1.10, [10, 0, 0], None), (2.00, [0, 0, 0], None)],
            "head": [(0.0, [0, 0, 0], None), (1.10, [14, 0, 0], None), (2.00, [0, 0, 0], None)],
            "halo_outer": [(0.0, [0, 0, 0], None), (1.10, [0, 0, 180], None), (2.00, [0, 0, 360], None)],
            "halo_inner": [(0.0, [0, 0, 0], None), (1.10, [0, 0, -180], None), (2.00, [0, 0, -360], None)],
            "upper_arm_right": [(0.0, [0, 0, 0], None), (0.45, [48, 28, 12], None), (1.10, [82, -24, -64], None), (2.00, [0, 0, 0], None)],
            "upper_arm_left": [(0.0, [0, 0, 0], None), (0.45, [48, -28, -12], None), (1.10, [82, 24, 64], None), (2.00, [0, 0, 0], None)],
            "cloak_tail_right": [(0.0, [0, 0, 0], None), (1.10, [32, 0, -24], None), (2.00, [0, 0, 0], None)],
            "cloak_tail_left": [(0.0, [0, 0, 0], None), (1.10, [32, 0, 24], None), (2.00, [0, 0, 0], None)],
        }),
    })

    # 5. Total Eclipse (2.80s = 56 ticks): ascend/kneel ritual -> twin blades raised skyward -> forward astral shockwave plunge
    dur_eclipse = 2.80
    clips.append({
        "id": "animation.nyxara.total_eclipse",
        "kind": "attack",
        "duration": dur_eclipse,
        "loop": "play_once",
        "beats": {"anticipation": 0.65, "strike": 1.85, "impact": 2.10, "recovery": 2.65},
        "tracks": bake_keyframe_table(dur_eclipse, {
            "pelvis": [(0.0, [0, 0, 0], [0, 0, 0]), (0.65, [-8, 0, 0], [0, -3.0, 0]), (1.50, [10, 0, 0], [0, 4.0, 0]), (2.10, [-14, 0, 0], [0, -4.0, -4.0]), (2.80, [0, 0, 0], [0, 0, 0])],
            "thigh_right": [(0.0, [0, 0, 0], None), (0.65, [22, 0, -8], None), (1.50, [-6, 0, 0], None), (2.10, [26, 0, 0], None), (2.80, [0, 0, 0], None)],
            "shin_right": [(0.0, [0, 0, 0], None), (0.65, [-30, 0, 0], None), (2.10, [-34, 0, 0], None), (2.80, [0, 0, 0], None)],
            "thigh_left": [(0.0, [0, 0, 0], None), (0.65, [22, 0, 8], None), (1.50, [-6, 0, 0], None), (2.10, [26, 0, 0], None), (2.80, [0, 0, 0], None)],
            "shin_left": [(0.0, [0, 0, 0], None), (0.65, [-30, 0, 0], None), (2.10, [-34, 0, 0], None), (2.80, [0, 0, 0], None)],
            "chest": [(0.0, [0, 0, 0], None), (0.65, [-14, 0, 0], None), (1.50, [14, 0, 0], None), (2.10, [-18, 0, 0], None), (2.80, [0, 0, 0], None)],
            "head": [(0.0, [0, 0, 0], None), (1.50, [20, 0, 0], None), (2.10, [20, 0, 0], None), (2.80, [0, 0, 0], None)],
            "halo_outer": [(0.0, [0, 0, 0], None), (1.50, [0, 0, 180], None), (2.80, [0, 0, 360], None)],
            "halo_inner": [(0.0, [0, 0, 0], None), (1.50, [0, 0, -180], None), (2.80, [0, 0, -360], None)],
            "upper_arm_right": [(0.0, [0, 0, 0], None), (0.65, [52, 20, 12], None), (1.50, [158, 0, -20], None), (1.85, [96, 0, -6], None), (2.10, [30, 0, 12], None), (2.80, [0, 0, 0], None)],
            "upper_arm_left": [(0.0, [0, 0, 0], None), (0.65, [52, -20, -12], None), (1.50, [158, 0, 20], None), (1.85, [96, 0, 6], None), (2.10, [30, 0, -12], None), (2.80, [0, 0, 0], None)],
            "cloak_tail_right": [(0.0, [0, 0, 0], None), (1.50, [34, 0, -18], None), (2.10, [20, 0, -10], None), (2.80, [0, 0, 0], None)],
            "cloak_tail_left": [(0.0, [0, 0, 0], None), (1.50, [34, 0, 18], None), (2.10, [20, 0, 10], None), (2.80, [0, 0, 0], None)],
        }),
    })

    return {"rig_bones": bone_ids, "joint_limits_deg": {}, "clips": clips}


def deterministic_noise(x: int, y: int, seed: int) -> float:
    n = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def paint_region(img_arr, x0, y0, w, h, mat, face_name, cid):
    palettes = {
        "void_obsidian": ((24, 22, 40), (44, 40, 72), (14, 12, 24), (72, 64, 114)),
        "twilight_indigo": ((64, 48, 122), (98, 76, 174), (38, 28, 76), (138, 108, 224)),
        "silver_trim": ((188, 198, 224), (230, 238, 255), (122, 130, 158), (248, 250, 255)),
        "star_gold": ((214, 174, 82), (248, 214, 122), (138, 104, 38), (255, 242, 176)),
        "astral_emissive": ((182, 114, 255), (222, 182, 255), (122, 64, 212), (235, 244, 255)),
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
                col = [int(base[i] * 0.68 + hi[i] * 0.32) for i in range(3)]
            is_border = (dx == 0 or dy == 0 or dx == w - 1 or dy == h - 1) and w >= 3 and h >= 3
            if is_border and mat != "astral_emissive":
                col = [int(col[i] * 0.62 + dark[i] * 0.38) for i in range(3)]
            elif mat == "astral_emissive":
                cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
                dist = max(abs(dx - cx) / max(w / 2.0, 1.0), abs(dy - cy) / max(h / 2.0, 1.0))
                col = list(accent if dist < 0.4 else (hi if dist < 0.78 else base))
            elif mat == "twilight_indigo" and (dx % 3 == 0) and r > 0.52:
                col = list(hi)
            elif r > 0.88:
                col = [int(col[i] * 0.85 + hi[i] * 0.15) for i in range(3)]
            elif r < 0.14:
                col = [int(col[i] * 0.85 + dark[i] * 0.15) for i in range(3)]
            img_arr[py, px] = [col[0], col[1], col[2], 255]


def pack_and_paint_textures(cubes):
    diffuse_arr = np.zeros((ATLAS_H, ATLAS_W, 4), dtype=np.uint8)
    glow_arr = np.zeros((ATLAS_H, ATLAS_W, 4), dtype=np.uint8)
    state = {"x": 1, "y": 1, "row_h": 0}

    def alloc(rw: int, rh: int):
        rw, rh = max(1, int(math.ceil(rw))), max(1, int(math.ceil(rh)))
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
        dx, dy, dz = max(1, round(hi[0] - lo[0])), max(1, round(hi[1] - lo[1])), max(1, round(hi[2] - lo[2]))
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
    diff_img.save(OUT_DIR / "nyxara_diffuse.png")
    glow_img.save(OUT_DIR / "nyxara_glow.png")
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
            "from": c["from"], "to": c["to"], "autouv": 0, "color": 7,
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
            "selected": cid == "animation.nyxara.crescent_sever",
            "saved": True, "path": "", "scope": 0, "anim_time_update": "",
            "blend_weight": "", "start_delay": "", "loop_delay": "", "animators": animators,
        })

    return {
        "meta": {"format_version": "5.0", "model_format": "bedrock", "box_uv": False},
        "name": "nyxara_eclipse_sovereign",
        "model_identifier": "nyxara_eclipse_sovereign",
        "visible_box": [3.5, 4.5, 0],
        "variable_placeholders": "", "multi_file_ruleset": "",
        "variable_placeholder_buttons": [], "bedrock_animation_mode": "entity",
        "timeline_setups": [], "unhandled_root_fields": {},
        "ai_used": False, "ai_agents": "",
        "resolution": {"width": ATLAS_W, "height": ATLAS_H},
        "elements": elements, "groups": groups, "outliner": [node(r) for r in root_ids],
        "textures": [{
            "name": "nyxara_diffuse.png", "path": "", "folder": "", "namespace": "",
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
                "identifier": "geometry.nyxara_eclipse_sovereign",
                "texture_width": ATLAS_W, "texture_height": ATLAS_H,
                "visible_bounds_width": 4.2, "visible_bounds_height": 4.6,
                "visible_bounds_offset": [0, 2.3, 0],
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
    (OUT_DIR / "nyxara_eclipse_sovereign.bbmodel").write_text(json.dumps(bbmodel, indent=2) + "\n", encoding="utf-8")
    geo_path = OUT_DIR / "nyxara_eclipse_sovereign.geo.json"
    anim_json_path = OUT_DIR / "nyxara_eclipse_sovereign.animation.json"
    geo_path.write_text(json.dumps(to_geo_json(creature, cube_uvs), indent=2) + "\n", encoding="utf-8")
    anim_json_path.write_text(json.dumps(to_animation_json(anim_manifest), indent=2) + "\n", encoding="utf-8")
    print(f"Built Nyxara: {len(creature['bones'])} bones, {len(creature['cubes'])} cubes, {len(anim_manifest['clips'])} clips.")

    if "--render" in sys.argv:
        sys.path.insert(0, str(SCRATCH_RENDER))
        import bb_render
        gm = bb_render.GeoModel(geo_path, anim_json_path, OUT_DIR / "nyxara_diffuse.png", OUT_DIR / "nyxara_glow.png")
        bb_render.render_suite("nyxara", gm, REF_IMG, [c["id"] for c in anim_manifest["clips"]], ARTIFACT_DIR, scale=3.8, center_y=33.0)
        print("Rendered Nyxara turnaround compare, contact sheet, and 5 GIFs.")


if __name__ == "__main__":
    main()
