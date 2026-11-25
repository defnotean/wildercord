#!/usr/bin/env python3
"""Build 32-bone articulated Cinder Warden rig, dual emissive layers (magma glow + eyes), and 6 clips."""
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
REF_IMG = ARTIFACT_DIR / "cinder_warden_turnaround_v2_1795610573766.jpg"
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
    c = {"id": cid, "bone": bone, "material": mat, "from": [round(float(x), 3) for x in lo], "to": [round(float(x), 3) for x in hi]}
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
    # ~3 blocks tall (48-50px), grounded boots on Y = 0
    bones = [
        {"id": "root", "parent": None, "pivot": [0, 0, 0]},
        {"id": "pelvis", "parent": "root", "pivot": [0, 20.0, 0.0]},
        {"id": "thigh_right", "parent": "pelvis", "pivot": [4.5, 19.0, 0.0], "mirror_of": "thigh_left"},
        {"id": "thigh_left", "parent": "pelvis", "pivot": [-4.5, 19.0, 0.0], "mirror_of": "thigh_right"},
        {"id": "shin_right", "parent": "thigh_right", "pivot": [4.5, 9.5, 0.0], "mirror_of": "shin_left"},
        {"id": "shin_left", "parent": "thigh_left", "pivot": [-4.5, 9.5, 0.0], "mirror_of": "shin_right"},
        {"id": "foot_right", "parent": "shin_right", "pivot": [4.5, 2.5, 0.0], "mirror_of": "foot_left"},
        {"id": "foot_left", "parent": "shin_left", "pivot": [-4.5, 2.5, 0.0], "mirror_of": "foot_right"},
        {"id": "chest", "parent": "pelvis", "pivot": [0, 23.0, 0.0]},
        {"id": "magma_core", "parent": "chest", "pivot": [0, 27.0, -4.0]},
        {"id": "back_vent_right", "parent": "chest", "pivot": [4.0, 32.0, 5.5], "mirror_of": "back_vent_left"},
        {"id": "back_vent_left", "parent": "chest", "pivot": [-4.0, 32.0, 5.5], "mirror_of": "back_vent_right"},
        {"id": "pauldron_right", "parent": "chest", "pivot": [10.0, 39.0, 0.0], "mirror_of": "pauldron_left"},
        {"id": "pauldron_left", "parent": "chest", "pivot": [-10.0, 39.0, 0.0], "mirror_of": "pauldron_right"},
        {"id": "head", "parent": "chest", "pivot": [0, 38.0, -2.5]},
        {"id": "upper_arm_right", "parent": "chest", "pivot": [11.5, 36.0, 0.0], "mirror_of": "upper_arm_left"},
        {"id": "upper_arm_left", "parent": "chest", "pivot": [-11.5, 36.0, 0.0], "mirror_of": "upper_arm_right"},
        {"id": "forearm_right", "parent": "upper_arm_right", "pivot": [12.5, 25.5, 0.0], "mirror_of": "forearm_left"},
        {"id": "forearm_left", "parent": "upper_arm_left", "pivot": [-12.5, 25.5, 0.0], "mirror_of": "forearm_right"},
        {"id": "gauntlet_right", "parent": "forearm_right", "pivot": [12.5, 19.0, 0.0], "mirror_of": "gauntlet_left"},
        {"id": "gauntlet_left", "parent": "forearm_left", "pivot": [-12.5, 19.0, 0.0], "mirror_of": "gauntlet_right"},
        {"id": "chain_r0", "parent": "gauntlet_right", "pivot": [12.5, 13.0, -2.0], "mirror_of": "chain_l0"},
        {"id": "chain_l0", "parent": "gauntlet_left", "pivot": [-12.5, 13.0, -2.0], "mirror_of": "chain_r0"},
        {"id": "chain_r1", "parent": "chain_r0", "pivot": [12.5, 7.5, -2.0], "mirror_of": "chain_l1"},
        {"id": "chain_l1", "parent": "chain_l0", "pivot": [-12.5, 7.5, -2.0], "mirror_of": "chain_r1"},
    ]
    cubes = []

    # Pelvis, Bolted Furnace Belt, Tassets & Pillar Greaves resting on Y = 0
    cubes.append(make_cube("pelvis_plate", "pelvis", "black_iron", [-8.5, 18.0, -4.8], [8.5, 23.0, 4.8]))
    cubes.append(make_cube("belt_brass", "pelvis", "brass_bronze", [-9.0, 20.5, -5.2], [9.0, 23.4, 5.2], inflate=0.05))
    cubes.append(make_cube("belt_buckle", "pelvis", "brass_bronze", [-2.4, 19.8, -5.8], [2.4, 24.0, -4.8]))
    cubes.append(make_cube("belt_inset", "pelvis", "dark_iron", [-1.4, 20.8, -6.0], [1.4, 23.0, -5.4]))
    cubes.extend(mirror_cube("thigh_r", "thigh_l", "thigh_right", "thigh_left", "dark_iron", [1.8, 9.5, -3.5], [7.8, 19.0, 3.5]))
    cubes.extend(mirror_cube("knee_r", "knee_l", "shin_right", "shin_left", "black_iron", [2.2, 8.8, -4.6], [7.4, 12.6, -2.4]))
    cubes.extend(mirror_cube("shin_r", "shin_l", "shin_right", "shin_left", "black_iron", [1.9, 2.5, -3.8], [7.7, 9.8, 3.8]))
    cubes.extend(mirror_cube("foot_r", "foot_l", "foot_right", "foot_left", "black_iron", [1.5, 0.0, -5.2], [8.1, 2.8, 4.0]))

    # Cracked Magma Furnace Chest + Hinged Back Steam Vents
    cubes.append(make_cube("magma_heart", "magma_core", "magma_glow", [-6.6, 23.8, -4.8], [6.6, 34.8, 4.4]))
    cubes.append(make_cube("chest_main", "chest", "black_iron", [-9.2, 23.0, -5.5], [9.2, 39.2, 5.5]))
    cubes.append(make_cube("chest_plate_r", "chest", "dark_iron", [3.0, 26.0, -6.3], [9.4, 38.2, -4.4]))
    cubes.append(make_cube("chest_plate_l", "chest", "dark_iron", [-9.4, 26.0, -6.3], [-3.0, 38.2, -4.4]))
    cubes.append(make_cube("chest_collar", "chest", "black_iron", [-7.5, 36.0, -6.2], [7.5, 39.8, 5.6]))
    cubes.append(make_cube("chest_crack_glow", "magma_core", "magma_glow", [-3.6, 23.6, -5.9], [3.6, 33.8, -4.6]))
    cubes.extend(mirror_cube("bvent_r", "bvent_l", "back_vent_right", "back_vent_left", "black_iron", [1.6, 28.0, 4.8], [8.2, 36.5, 6.8], rotation=[10, 0, 0]))
    cubes.extend(mirror_cube("bvent_r_g", "bvent_l_g", "back_vent_right", "back_vent_left", "magma_glow", [2.6, 29.0, 6.2], [7.2, 33.5, 7.0], rotation=[10, 0, 0]))

    # Small Grim Helm Sunk Low Between Colossal Sloped Pauldrons
    cubes.append(make_cube("helm_box", "head", "black_iron", [-4.4, 36.2, -6.8], [4.4, 44.2, 1.8]))
    cubes.append(make_cube("helm_brow", "head", "dark_iron", [-4.7, 41.2, -7.2], [4.7, 43.2, 2.0]))
    cubes.append(make_cube("helm_nose", "head", "black_iron", [-1.0, 36.4, -7.2], [1.0, 41.4, -6.0]))
    cubes.append(make_cube("eye_r", "head", "eye_glow", [1.3, 39.5, -7.1], [3.5, 41.0, -6.2]))
    cubes.append(make_cube("eye_l", "head", "eye_glow", [-3.5, 39.5, -7.1], [-1.3, 41.0, -6.2]))

    cubes.extend(mirror_cube("pauldron_r_base", "pauldron_l_base", "pauldron_right", "pauldron_left", "black_iron", [8.0, 35.5, -5.8], [17.8, 43.5, 5.8], rotation=[0, 0, -14]))
    cubes.extend(mirror_cube("pauldron_r_fin", "pauldron_l_fin", "pauldron_right", "pauldron_left", "dark_iron", [11.0, 39.0, -5.2], [13.5, 50.2, 5.2], rotation=[0, 0, -18]))
    cubes.extend(mirror_cube("pauldron_r_vent", "pauldron_l_vent", "pauldron_right", "pauldron_left", "magma_glow", [11.6, 37.8, -5.9], [16.4, 42.4, 5.9], rotation=[0, 0, -14]))

    # Huge Low-Hanging Gauntleted Arms + Spikes + Swinging Chains
    cubes.extend(mirror_cube("uarm_r", "uarm_l", "upper_arm_right", "upper_arm_left", "black_iron", [8.8, 25.5, -3.4], [15.8, 36.2, 3.4]))
    cubes.extend(mirror_cube("farm_r", "farm_l", "forearm_right", "forearm_left", "dark_iron", [9.2, 19.0, -3.5], [15.8, 25.8, 3.5]))
    cubes.extend(mirror_cube("gaunt_r", "gaunt_l", "gauntlet_right", "gauntlet_left", "black_iron", [8.8, 12.2, -4.2], [16.4, 20.4, 4.2]))
    cubes.extend(mirror_cube("gspike_r_top", "gspike_l_top", "gauntlet_right", "gauntlet_left", "iron_chain", [16.0, 17.0, -1.6], [18.4, 19.2, 1.6]))
    cubes.extend(mirror_cube("gspike_r_bot", "gspike_l_bot", "gauntlet_right", "gauntlet_left", "iron_chain", [16.0, 13.4, -1.6], [18.4, 15.6, 1.6]))
    cubes.extend(mirror_cube("chain_r0_l", "chain_l0_l", "chain_r0", "chain_l0", "iron_chain", [11.4, 7.4, -3.2], [13.6, 13.0, -1.0]))
    cubes.extend(mirror_cube("chain_r1_l", "chain_l1_l", "chain_r1", "chain_l1", "iron_chain", [11.4, 2.2, -3.2], [13.6, 7.6, -1.0]))

    materials = [
        {"id": "black_iron", "type": "opaque"},
        {"id": "dark_iron", "type": "opaque"},
        {"id": "brass_bronze", "type": "opaque"},
        {"id": "iron_chain", "type": "opaque"},
        {"id": "magma_glow", "type": "emissive"},
        {"id": "eye_glow", "type": "emissive"},
    ]
    return {"creature_id": "cinder_warden", "ground_y": 0.0, "floating_entity": False, "materials": materials, "bones": bones, "cubes": cubes}


def _keys(dur: float, fps: int = 20):
    n = int(round(dur * fps))
    return [round(i / fps, 3) for i in range(n + 1)]


def build_animation_manifest(creature):
    bone_ids = [b["id"] for b in creature["bones"]]
    clips = []

    def loop_clip(cid, dur, kind, fn, extra=None):
        tr = {}
        for t in _keys(dur):
            is_end = abs(t - dur) < 1e-4
            ph = 0.0 if (t == 0.0 or is_end) else 2.0 * math.pi * (t / dur)
            key = "0.0" if t == 0.0 else (f"{dur}" if is_end else f"{t:.2f}".rstrip("0").rstrip("."))
            fn(ph, key, tr)
        entry = {"id": cid, "kind": kind, "duration": dur, "loop": "loop", "tracks": tr}
        if extra:
            entry.update(extra)
        clips.append(entry)

    def set_kf(tr, b, ch, k, v):
        tr.setdefault(b, {}).setdefault(ch, {})[k] = [round(float(x), 3) for x in v]

    def fn_idle(ph, k, tr):
        s1, c1 = math.sin(ph), math.cos(ph)
        set_kf(tr, "chest", "rotation", k, [-4.0 + 2.0 * s1, 2.0 * c1, 0])
        set_kf(tr, "upper_arm_right", "rotation", k, [4.0 + 3.0 * s1, 0, -4.0])
        set_kf(tr, "upper_arm_left", "rotation", k, [4.0 - 3.0 * s1, 0, 4.0])
        set_kf(tr, "chain_r0", "rotation", k, [6.0 * s1, 0, 4.0 * c1])
        set_kf(tr, "chain_l0", "rotation", k, [-6.0 * s1, 0, -4.0 * c1])

    loop_clip("animation.cinder_warden.idle", 2.4, "idle", fn_idle)

    def fn_walk(ph, k, tr):
        s1, c1 = math.sin(ph), math.cos(ph)
        set_kf(tr, "pelvis", "position", k, [1.2 * s1, -0.5 + 0.7 * abs(s1), 0])
        set_kf(tr, "pelvis", "rotation", k, [-4.0, 6.0 * c1, 3.0 * s1])
        set_kf(tr, "thigh_right", "rotation", k, [24.0 * s1, 0, 0])
        set_kf(tr, "thigh_left", "rotation", k, [-24.0 * s1, 0, 0])
        set_kf(tr, "shin_right", "rotation", k, [-8.0 - 14.0 * max(0.0, -c1), 0, 0])
        set_kf(tr, "shin_left", "rotation", k, [-8.0 - 14.0 * max(0.0, c1), 0, 0])
        set_kf(tr, "chest", "rotation", k, [-8.0, -8.0 * c1, -2.5 * s1])
        set_kf(tr, "upper_arm_right", "rotation", k, [-10.0 - 20.0 * c1, 0, -5.0])
        set_kf(tr, "upper_arm_left", "rotation", k, [-10.0 + 20.0 * c1, 0, 5.0])
        set_kf(tr, "chain_r0", "rotation", k, [14.0 * c1, 0, 6.0 * s1])
        set_kf(tr, "chain_l0", "rotation", k, [-14.0 * c1, 0, -6.0 * s1])

    loop_clip("animation.cinder_warden.walk", 1.4, "walk", fn_walk,
              extra={"contralateral_pairs": [["thigh_right", "thigh_left"], ["upper_arm_right", "upper_arm_left"]]})

    def bake(dur, tbl):
        tr = {}
        for b, pts in tbl.items():
            rm, pm = {}, {}
            for ts, r, p in pts:
                k = f"{ts:.2f}".rstrip("0").rstrip(".") if ts > 0 else "0.0"
                rm[k] = [round(float(x), 3) for x in r]
                if p is not None:
                    pm[k] = [round(float(x), 3) for x in p]
            tr[b] = {"rotation": rm}
            if pm:
                tr[b]["position"] = pm
        return tr

    clips.append({
        "id": "animation.cinder_warden.hammerfall", "kind": "attack", "duration": 1.6, "loop": "play_once",
        "beats": {"anticipation": 0.45, "strike": 0.75, "impact": 0.95, "recovery": 1.45},
        "tracks": bake(1.6, {
            "pelvis": [(0.0, [0, 0, 0], [0, 0, 0]), (0.45, [6, 0, 0], [0, -1, 1]), (0.95, [-14, 0, 0], [0, -3.5, -4]), (1.60, [0, 0, 0], [0, 0, 0])],
            "chest": [(0.0, [0, 0, 0], None), (0.45, [12, 0, 0], None), (0.95, [-22, 0, 0], None), (1.60, [0, 0, 0], None)],
            "upper_arm_right": [(0.0, [0, 0, 0], None), (0.45, [-142, 0, -14], None), (0.95, [68, 0, 12], None), (1.60, [0, 0, 0], None)],
            "upper_arm_left": [(0.0, [0, 0, 0], None), (0.45, [-142, 0, 14], None), (0.95, [68, 0, -12], None), (1.60, [0, 0, 0], None)],
            "chain_r0": [(0.0, [0, 0, 0], None), (0.95, [-35, 0, 0], None), (1.60, [0, 0, 0], None)],
            "chain_l0": [(0.0, [0, 0, 0], None), (0.95, [-35, 0, 0], None), (1.60, [0, 0, 0], None)],
        }),
    })
    clips.append({
        "id": "animation.cinder_warden.chain_sweep", "kind": "attack", "duration": 1.6, "loop": "play_once",
        "beats": {"anticipation": 0.40, "strike": 0.75, "impact": 1.05, "recovery": 1.45},
        "tracks": bake(1.6, {
            "pelvis": [(0.0, [0, 0, 0], [0, 0, 0]), (0.40, [0, -24, 0], None), (1.05, [-8, 28, 0], [0, -1.5, -2]), (1.60, [0, 0, 0], [0, 0, 0])],
            "chest": [(0.0, [0, 0, 0], None), (0.40, [0, -28, 0], None), (1.05, [-10, 34, 0], None), (1.60, [0, 0, 0], None)],
            "upper_arm_right": [(0.0, [0, 0, 0], None), (0.40, [20, 0, -48], None), (1.05, [55, 0, -68], None), (1.60, [0, 0, 0], None)],
            "upper_arm_left": [(0.0, [0, 0, 0], None), (0.40, [20, 0, 48], None), (1.05, [55, 0, 68], None), (1.60, [0, 0, 0], None)],
            "chain_r0": [(0.0, [0, 0, 0], None), (1.05, [40, 0, -35], None), (1.60, [0, 0, 0], None)],
            "chain_l0": [(0.0, [0, 0, 0], None), (1.05, [40, 0, 35], None), (1.60, [0, 0, 0], None)],
        }),
    })
    clips.append({
        "id": "animation.cinder_warden.flare_roar", "kind": "attack", "duration": 2.2, "loop": "play_once",
        "beats": {"anticipation": 0.55, "strike": 1.25, "impact": 1.55, "recovery": 2.05},
        "tracks": bake(2.2, {
            "chest": [(0.0, [0, 0, 0], None), (0.55, [-16, 0, 0], None), (1.35, [20, 0, 0], None), (2.20, [0, 0, 0], None)],
            "head": [(0.0, [0, 0, 0], None), (1.35, [24, 0, 0], None), (2.20, [0, 0, 0], None)],
            "back_vent_right": [(0.0, [0, 0, 0], None), (1.35, [-22, 0, -14], None), (2.20, [0, 0, 0], None)],
            "back_vent_left": [(0.0, [0, 0, 0], None), (1.35, [-22, 0, 14], None), (2.20, [0, 0, 0], None)],
            "upper_arm_right": [(0.0, [0, 0, 0], None), (0.55, [45, 25, 18], None), (1.35, [28, -28, -58], None), (2.20, [0, 0, 0], None)],
            "upper_arm_left": [(0.0, [0, 0, 0], None), (0.55, [45, -25, -18], None), (1.35, [28, 28, 58], None), (2.20, [0, 0, 0], None)],
        }),
    })
    clips.append({
        "id": "animation.cinder_warden.spent_slump", "kind": "idle", "duration": 2.0, "loop": "loop",
        "tracks": bake(2.0, {
            "pelvis": [(0.0, [-8, 0, 0], [0, -3.0, 0]), (1.0, [-10, 0, 0], [0, -3.4, 0]), (2.0, [-8, 0, 0], [0, -3.0, 0])],
            "chest": [(0.0, [-18, 0, 0], None), (1.0, [-20, 0, 0], None), (2.0, [-18, 0, 0], None)],
            "head": [(0.0, [-18, 0, 0], None), (1.0, [-22, 0, 0], None), (2.0, [-18, 0, 0], None)],
            "upper_arm_right": [(0.0, [26, 0, -8], None), (1.0, [28, 0, -10], None), (2.0, [26, 0, -8], None)],
            "upper_arm_left": [(0.0, [26, 0, 8], None), (1.0, [28, 0, 10], None), (2.0, [26, 0, 8], None)],
        }),
    })
    return {"rig_bones": bone_ids, "joint_limits_deg": {}, "clips": clips}


def deterministic_noise(x: int, y: int, seed: int) -> float:
    n = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def paint_region(img_arr, x0, y0, w, h, mat, face_name, cid):
    palettes = {
        "black_iron": ((36, 36, 42), (62, 62, 72), (20, 20, 26), (86, 86, 98)),
        "dark_iron": ((52, 50, 58), (82, 80, 92), (30, 28, 34), (108, 106, 120)),
        "brass_bronze": ((166, 128, 62), (212, 168, 88), (104, 78, 34), (242, 206, 122)),
        "iron_chain": ((78, 80, 90), (118, 122, 134), (44, 46, 52), (156, 160, 174)),
        "magma_glow": ((250, 126, 24), (255, 202, 76), (186, 58, 12), (255, 244, 178)),
        "eye_glow": ((255, 146, 36), (255, 214, 96), (196, 76, 18), (255, 246, 192)),
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
            is_border = (dx == 0 or dy == 0 or dx == w - 1 or dy == h - 1) and w >= 3 and h >= 3
            if is_border and "glow" not in mat:
                col = [int(col[i] * 0.62 + dark[i] * 0.38) for i in range(3)]
            elif mat == "magma_glow":
                cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
                dist = max(abs(dx - cx) / max(w / 2.0, 1.0), abs(dy - cy) / max(h / 2.0, 1.0))
                col = list(accent if dist < 0.45 else (hi if dist < 0.78 else base))
            elif r > 0.88:
                col = [int(col[i] * 0.85 + hi[i] * 0.15) for i in range(3)]
            img_arr[py, px] = [col[0], col[1], col[2], 255]


def pack_and_paint_textures(cubes):
    diffuse = np.zeros((ATLAS_H, ATLAS_W, 4), dtype=np.uint8)
    glow = np.zeros((ATLAS_H, ATLAS_W, 4), dtype=np.uint8)
    eyes = np.zeros((ATLAS_H, ATLAS_W, 4), dtype=np.uint8)
    state = {"x": 1, "y": 1, "row_h": 0}

    def alloc(rw: int, rh: int):
        rw, rh = max(1, int(math.ceil(rw))), max(1, int(math.ceil(rh)))
        if state["x"] + rw + 1 > ATLAS_W:
            state["x"] = 1
            state["y"] += state["row_h"] + 1
            state["row_h"] = 0
        if state["y"] + rh + 1 > ATLAS_H:
            raise RuntimeError(f"Atlas overflow at {rw}x{rh}")
        ux, vy = state["x"], state["y"]
        state["x"] += rw + 1
        state["row_h"] = max(state["row_h"], rh)
        return ux, vy, rw, rh

    cube_uvs = {}
    for c in cubes:
        lo, hi = c["from"], c["to"]
        dx, dy, dz = max(1, round(hi[0] - lo[0])), max(1, round(hi[1] - lo[1])), max(1, round(hi[2] - lo[2]))
        mat = c["material"]
        face_uvs = {}
        for fname, (fw, fh) in {
            "north": (dx, dy), "south": (dx, dy),
            "east": (dz, dy), "west": (dz, dy),
            "up": (dx, dz), "down": (dx, dz),
        }.items():
            ux, vy, rw, rh = alloc(fw, fh)
            paint_region(diffuse, ux, vy, rw, rh, mat, fname, c["id"])
            if mat == "magma_glow":
                paint_region(glow, ux, vy, rw, rh, mat, fname, c["id"])
            elif mat == "eye_glow":
                paint_region(eyes, ux, vy, rw, rh, mat, fname, c["id"])
            face_uvs[fname] = {"uv": [ux, vy, ux + fw, vy + fh], "texture": 0}
        cube_uvs[c["id"]] = face_uvs

    diff_img = Image.fromarray(diffuse, "RGBA")
    glow_img = Image.fromarray(glow, "RGBA")
    eyes_img = Image.fromarray(eyes, "RGBA")
    diff_img.save(OUT_DIR / "cinder_warden_diffuse.png")
    glow_img.save(OUT_DIR / "cinder_warden_glow.png")
    eyes_img.save(OUT_DIR / "cinder_warden_eyes.png")
    Image.alpha_composite(glow_img, eyes_img).save(OUT_DIR / "cinder_warden_glow_preview.png")
    return diff_img, cube_uvs


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
            "from": c["from"], "to": c["to"], "autouv": 0, "color": 1,
            "origin": c.get("origin", bone_map[bid]["pivot"]),
            "faces": cube_uvs[cid], "type": "cube", "uuid": cube_uuids[cid],
        }
        if c.get("rotation") is not None:
            elem["rotation"] = c["rotation"]
        elements.append(elem)

    groups, child_bones, root_ids = [], {b["id"]: [] for b in creature["bones"]}, []
    for b in creature["bones"]:
        bid = b["id"]
        (root_ids if b["parent"] is None else child_bones[b["parent"]]).append(bid)
        groups.append({"name": bid, "uuid": bone_uuids[bid], "export": True, "origin": b["pivot"], "rotation": [0, 0, 0], "color": 0, "children": [], "shade": True, "isOpen": True})

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
                    kfs.append({"channel": ch_name, "data_points": [{"x": str(vec[0]), "y": str(vec[1]), "z": str(vec[2])}],
                                "uuid": stable_uuid(f"kf:{cid}:{bname}:{ch_name}:{t_str}"), "time": float(t_str), "color": -1, "interpolation": "linear"})
            animators[bone_uuids[bname]] = {"name": bname, "type": "bone", "keyframes": kfs}
        bb_anims.append({"uuid": stable_uuid("anim:" + cid), "name": cid, "loop": "loop" if clip["loop"] == "loop" else "once",
                         "length": clip["duration"], "snapping": 20, "animators": animators})

    return {
        "meta": {"format_version": "5.0", "model_format": "bedrock", "box_uv": False},
        "name": "cinder_warden", "model_identifier": "cinder_warden", "resolution": {"width": ATLAS_W, "height": ATLAS_H},
        "elements": elements, "groups": groups, "outliner": [node(r) for r in root_ids],
        "textures": [{"name": "cinder_warden_diffuse.png", "id": "0", "width": ATLAS_W, "height": ATLAS_H, "use_as_default": True, "visible": True, "internal": True, "source": img_to_data_url(diff_img)}],
        "animations": bb_anims,
    }


def to_geo_json(creature, cube_uvs):
    cubes_by_bone = {b["id"]: [] for b in creature["bones"]}
    for c in creature["cubes"]:
        lo, hi = c["from"], c["to"]
        f_uv = {fn: {"uv": [cube_uvs[c["id"]][fn]["uv"][0], cube_uvs[c["id"]][fn]["uv"][1]],
                     "uv_size": [cube_uvs[c["id"]][fn]["uv"][2] - cube_uvs[c["id"]][fn]["uv"][0],
                                 cube_uvs[c["id"]][fn]["uv"][3] - cube_uvs[c["id"]][fn]["uv"][1]]}
                for fn in ("north", "east", "south", "west", "up", "down")}
        gc = {"origin": [lo[0], lo[1], lo[2]], "size": [hi[0] - lo[0], hi[1] - lo[1], hi[2] - lo[2]], "uv": f_uv}
        if c.get("rotation") is not None:
            gc["rotation"] = c["rotation"]
            gc["pivot"] = c["origin"]
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
        "minecraft:geometry": [{"description": {
            "identifier": "geometry.cinder_warden", "texture_width": ATLAS_W, "texture_height": ATLAS_H,
            "visible_bounds_width": 3.5, "visible_bounds_height": 3.5, "visible_bounds_offset": [0, 1.75, 0],
        }, "bones": bones_out}],
    }


def to_animation_json(anim_manifest):
    anims_out = {}
    for clip in anim_manifest["clips"]:
        entry = {
            "animation_length": clip["duration"],
            "bones": {bname: {ch: {t: list(v) for t, v in sorted(cm.items(), key=lambda kv: float(kv[0]))} for ch, cm in chs.items()}
                      for bname, chs in clip["tracks"].items()},
        }
        if clip["loop"] == "loop":
            entry["loop"] = True
        anims_out[clip["id"]] = entry
    return {"format_version": "1.8.0", "animations": anims_out}


def main():
    creature = build_creature_manifest()
    cp = OUT_DIR / "creature_manifest.json"
    cp.write_text(json.dumps(creature, indent=2) + "\n", encoding="utf-8")
    res_c = subprocess.run([sys.executable, str(VALIDATOR_CREATURE), str(cp)], check=False, text=True, capture_output=True)
    print(res_c.stdout.strip())
    if res_c.returncode != 0:
        sys.exit(res_c.returncode)

    anim_manifest = build_animation_manifest(creature)
    ap = OUT_DIR / "animation_manifest.json"
    ap.write_text(json.dumps(anim_manifest, indent=2) + "\n", encoding="utf-8")
    res_a = subprocess.run([sys.executable, str(VALIDATOR_ANIM), str(ap)], check=False, text=True, capture_output=True)
    print(res_a.stdout.strip())
    if res_a.returncode != 0:
        sys.exit(res_a.returncode)

    diff_img, cube_uvs = pack_and_paint_textures(creature["cubes"])
    (OUT_DIR / "cinder_warden.bbmodel").write_text(json.dumps(to_bbmodel(creature, anim_manifest, diff_img, cube_uvs), indent=2) + "\n", encoding="utf-8")
    gp = OUT_DIR / "cinder_warden.geo.json"
    ajp = OUT_DIR / "cinder_warden.animation.json"
    gp.write_text(json.dumps(to_geo_json(creature, cube_uvs), indent=2) + "\n", encoding="utf-8")
    ajp.write_text(json.dumps(to_animation_json(anim_manifest), indent=2) + "\n", encoding="utf-8")
    print(f"Built Cinder Warden: {len(creature['bones'])} bones, {len(creature['cubes'])} cubes, {len(anim_manifest['clips'])} clips.")

    if "--render" in sys.argv:
        sys.path.insert(0, str(SCRATCH_RENDER))
        import bb_render
        gm = bb_render.GeoModel(gp, ajp, OUT_DIR / "cinder_warden_diffuse.png", OUT_DIR / "cinder_warden_glow_preview.png")
        bb_render.render_suite("cinder_warden", gm, REF_IMG, [c["id"] for c in anim_manifest["clips"]], ARTIFACT_DIR, scale=4.5, center_y=26.0)
        print("Rendered Cinder Warden compare, contact sheet, and 6 GIFs.")


if __name__ == "__main__":
    main()
