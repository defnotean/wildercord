#!/usr/bin/env python3
"""Build 36-bone articulated Thalassor, Abyssal Sovereign leviathan rig, textures, and 4 swim/attack clips."""
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
REF_IMG = ARTIFACT_DIR / "thalassor_abyssal_sovereign_turnaround_1795610550401.jpg"
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
    # Floating sea-dragon leviathan centered along -Z -> +Z
    bones = [
        {"id": "root", "parent": None, "pivot": [0, 0, 0]},
        {"id": "chest", "parent": "root", "pivot": [0, 20.0, 0.0]},
        {"id": "dorsal_sail", "parent": "chest", "pivot": [0, 31.0, 0.0]},
        {"id": "pelvic_right", "parent": "chest", "pivot": [8.5, 13.0, 7.0], "mirror_of": "pelvic_left"},
        {"id": "pelvic_left", "parent": "chest", "pivot": [-8.5, 13.0, 7.0], "mirror_of": "pelvic_right"},
        {"id": "neck", "parent": "chest", "pivot": [0, 22.0, -11.0]},
        {"id": "neck_crest", "parent": "neck", "pivot": [0, 31.0, -18.0]},
        {"id": "head", "parent": "neck", "pivot": [0, 24.0, -24.0]},
        {"id": "jaw", "parent": "head", "pivot": [0, 20.0, -28.0]},
        {"id": "crown_spire", "parent": "head", "pivot": [0, 33.0, -32.0]},
        {"id": "horn_right", "parent": "head", "pivot": [6.0, 32.0, -29.0], "mirror_of": "horn_left"},
        {"id": "horn_left", "parent": "head", "pivot": [-6.0, 32.0, -29.0], "mirror_of": "horn_right"},
        {"id": "cheek_fan_right", "parent": "head", "pivot": [8.5, 24.0, -29.0], "mirror_of": "cheek_fan_left"},
        {"id": "cheek_fan_left", "parent": "head", "pivot": [-8.5, 24.0, -29.0], "mirror_of": "cheek_fan_right"},
        # 2-joint Pectoral Wing-Fins
        {"id": "fin_right", "parent": "chest", "pivot": [11.0, 20.0, -4.0], "mirror_of": "fin_left"},
        {"id": "fin_left", "parent": "chest", "pivot": [-11.0, 20.0, -4.0], "mirror_of": "fin_right"},
        {"id": "wing_right", "parent": "fin_right", "pivot": [21.0, 20.0, -3.0], "mirror_of": "wing_left"},
        {"id": "wing_left", "parent": "fin_left", "pivot": [-21.0, 20.0, -3.0], "mirror_of": "wing_right"},
        {"id": "wing_tip_right", "parent": "wing_right", "pivot": [36.0, 20.0, -1.0], "mirror_of": "wing_tip_left"},
        {"id": "wing_tip_left", "parent": "wing_left", "pivot": [-36.0, 20.0, -1.0], "mirror_of": "wing_tip_right"},
        # 5-segment Serpentine Tail + Crescent Caudal Fan
        {"id": "tail_1", "parent": "chest", "pivot": [0, 20.0, 11.5]},
        {"id": "tail_2", "parent": "tail_1", "pivot": [0, 20.0, 28.0]},
        {"id": "tail_3", "parent": "tail_2", "pivot": [0, 20.0, 43.0]},
        {"id": "tail_4", "parent": "tail_3", "pivot": [0, 20.0, 57.0]},
        {"id": "tail_fan", "parent": "tail_4", "pivot": [0, 20.0, 70.0]},
    ]
    cubes = []

    # Broad Abyssal Chest, Golden Plastron Belly Scutes, Flank Runes, Dorsal Sail & Pelvic Fins
    cubes.append(make_cube("chest_armor", "chest", "abyss_scale", [-10.5, 11.5, -11.5], [10.5, 29.5, 11.5]))
    cubes.append(make_cube("belly_plastron_1", "chest", "plastron_gold", [-8.0, 9.0, -10.5], [8.0, 12.2, 10.5]))
    cubes.append(make_cube("belly_plastron_2", "chest", "plastron_gold", [-6.0, 7.2, -9.2], [6.0, 9.4, 9.2]))
    cubes.extend(mirror_cube("flank_rune_r", "flank_rune_l", "chest", "chest", "abyss_glow", [10.1, 16.0, -7.0], [10.9, 24.0, 6.0]))
    cubes.append(make_cube("dorsal_fin_tall", "dorsal_sail", "sea_fan_teal", [-1.0, 29.5, -10.0], [1.0, 43.5, 10.5]))
    cubes.append(make_cube("dorsal_spine_glow", "dorsal_sail", "abyss_glow", [-0.7, 36.0, -6.0], [0.7, 44.2, 8.0]))
    cubes.extend(mirror_cube("pelvic_fin_r", "pelvic_fin_l", "pelvic_right", "pelvic_left", "sea_fan_teal", [8.0, 11.5, 4.0], [17.0, 14.0, 11.0], rotation=[0, -16, -18]))

    # Neck & Translucent Neck Crest
    cubes.append(make_cube("neck_seg", "neck", "abyss_scale", [-7.5, 16.5, -26.0], [7.5, 29.5, -10.5]))
    cubes.append(make_cube("neck_belly", "neck", "plastron_gold", [-5.2, 14.8, -25.0], [5.2, 17.2, -11.0]))
    cubes.append(make_cube("neck_sail", "neck_crest", "sea_fan_teal", [-1.0, 28.5, -24.5], [1.0, 39.5, -12.0]))

    # Crowned Leviathan Skull, Snout, Cheek Fans, Branching Coral Antler Horns & Articulated Jaw + Fangs
    cubes.append(make_cube("skull_main", "head", "abyss_scale", [-8.0, 19.5, -40.0], [8.0, 32.0, -23.0]))
    cubes.append(make_cube("snout_box", "head", "abyss_scale", [-6.0, 20.0, -49.0], [6.0, 29.2, -39.5]))
    cubes.append(make_cube("eye_r", "head", "abyss_glow", [6.2, 24.5, -39.0], [8.4, 27.6, -34.0]))
    cubes.append(make_cube("eye_l", "head", "abyss_glow", [-8.4, 24.5, -39.0], [-6.2, 27.6, -34.0]))
    cubes.append(make_cube("crown_center", "crown_spire", "plastron_gold", [-1.4, 31.5, -34.0], [1.4, 41.5, -30.5], rotation=[-20, 0, 0]))
    cubes.extend(mirror_cube("horn_stem_r", "horn_stem_l", "horn_right", "horn_left", "coral_horn", [4.8, 31.2, -31.0], [7.2, 42.5, -28.6], rotation=[-22, 0, 22]))
    cubes.extend(mirror_cube("horn_tine_a_r", "horn_tine_a_l", "horn_right", "horn_left", "abyss_glow", [6.6, 38.5, -29.8], [11.8, 42.0, -27.8], rotation=[-18, 0, 48]))
    cubes.extend(mirror_cube("horn_tine_b_r", "horn_tine_b_l", "horn_right", "horn_left", "abyss_glow", [6.2, 41.5, -28.4], [8.8, 49.5, -26.2], rotation=[-20, 0, 14]))
    cubes.extend(mirror_cube("cheek_r_fin", "cheek_l_fin", "cheek_fan_right", "cheek_fan_left", "sea_fan_teal", [8.0, 20.0, -31.0], [9.4, 29.0, -20.5], rotation=[0, 26, 0]))

    cubes.append(make_cube("jaw_base", "jaw", "abyss_scale", [-5.5, 16.0, -47.5], [5.5, 20.2, -27.5]))
    for fx, fz in ((3.6, -46.5), (-3.6, -46.5), (4.0, -41.0), (-4.0, -41.0), (4.2, -35.0), (-4.2, -35.0)):
        cubes.append(make_cube(f"fang_{fx}_{fz}", "jaw", "ivory_fang", [fx - 0.8, 19.6, fz - 0.8], [fx + 0.8, 22.8, fz + 0.8]))

    # High-Arching Webbed Abyssal Wing-Fins (vaulted shoulder -> elevated wrist crest -> sweeping membrane)
    cubes.extend(mirror_cube("fin_r_arm", "fin_l_arm", "fin_right", "fin_left", "abyss_scale", [10.0, 19.0, -7.5], [23.0, 23.0, -1.5], rotation=[0, 16, 26]))
    cubes.extend(mirror_cube("wing_r_spar", "wing_l_spar", "wing_right", "wing_left", "abyss_scale", [21.0, 25.0, -6.5], [38.5, 28.4, -2.5], rotation=[0, 18, 20]))
    cubes.extend(mirror_cube("wing_r_strut", "wing_l_strut", "wing_right", "wing_left", "abyss_scale", [22.0, 14.0, 0.5], [36.5, 25.6, 2.5], rotation=[0, 18, 12]))
    cubes.extend(mirror_cube("wing_r_web", "wing_l_web", "wing_right", "wing_left", "sea_fan_teal", [20.5, 13.5, -2.5], [38.0, 26.5, 12.0], rotation=[0, 18, 14]))
    cubes.extend(mirror_cube("wing_r_tip_spar", "wing_l_tip_spar", "wing_tip_right", "wing_tip_left", "abyss_scale", [36.5, 22.5, -5.0], [56.0, 25.2, -1.5], rotation=[0, 22, -26]))
    cubes.extend(mirror_cube("wing_r_tip", "wing_l_tip", "wing_tip_right", "wing_tip_left", "sea_fan_teal", [36.0, 10.5, -3.0], [54.5, 23.5, 9.5], rotation=[0, 22, -22]))

    # 5-Segment Armored Tail + Crescent Caudal Tail Fan
    cubes.append(make_cube("t1_body", "tail_1", "abyss_scale", [-8.2, 12.5, 11.5], [8.2, 27.5, 28.5]))
    cubes.append(make_cube("t1_rune", "tail_1", "abyss_glow", [-1.2, 26.8, 14.0], [1.2, 30.5, 26.0]))
    cubes.append(make_cube("t2_body", "tail_2", "abyss_scale", [-6.5, 14.0, 28.0], [6.5, 26.0, 43.5]))
    cubes.append(make_cube("t2_fin", "tail_2", "sea_fan_teal", [-0.9, 25.5, 30.0], [0.9, 31.5, 42.0]))
    cubes.append(make_cube("t3_body", "tail_3", "abyss_scale", [-4.8, 15.5, 43.0], [4.8, 24.5, 57.5]))
    cubes.append(make_cube("t4_body", "tail_4", "abyss_scale", [-3.2, 16.8, 57.0], [3.2, 23.2, 70.5]))
    cubes.append(make_cube("tail_fan_up", "tail_fan", "sea_fan_teal", [-1.0, 20.0, 69.0], [1.0, 34.0, 86.0], rotation=[20, 0, 0]))
    cubes.append(make_cube("tail_fan_dn", "tail_fan", "sea_fan_teal", [-1.0, 6.0, 69.0], [1.0, 20.0, 86.0], rotation=[-20, 0, 0]))
    cubes.append(make_cube("tail_fan_glow", "tail_fan", "abyss_glow", [-0.8, 14.0, 72.0], [0.8, 26.0, 88.0]))

    materials = [
        {"id": "abyss_scale", "type": "opaque"},
        {"id": "plastron_gold", "type": "opaque"},
        {"id": "sea_fan_teal", "type": "opaque"},
        {"id": "coral_horn", "type": "opaque"},
        {"id": "ivory_fang", "type": "opaque"},
        {"id": "abyss_glow", "type": "emissive"},
    ]
    return {"creature_id": "thalassor", "ground_y": 0.0, "floating_entity": True, "materials": materials, "bones": bones, "cubes": cubes}


def _keys(dur: float, fps: int = 20):
    n = int(round(dur * fps))
    return [round(i / fps, 3) for i in range(n + 1)]


def build_animation_manifest(creature):
    bone_ids = [b["id"] for b in creature["bones"]]
    clips = []

    def loop_clip(cid, dur, kind, fn):
        tr = {}
        for t in _keys(dur):
            is_end = abs(t - dur) < 1e-4
            ph = 0.0 if (t == 0.0 or is_end) else 2.0 * math.pi * (t / dur)
            key = "0.0" if t == 0.0 else (f"{dur}" if is_end else f"{t:.2f}".rstrip("0").rstrip("."))
            fn(ph, key, tr)
        clips.append({"id": cid, "kind": kind, "duration": dur, "loop": "loop", "tracks": tr})

    def set_kf(tr, b, ch, k, v):
        tr.setdefault(b, {}).setdefault(ch, {})[k] = [round(float(x), 3) for x in v]

    def fn_swim(ph, k, tr):
        w = math.sin(ph)
        set_kf(tr, "chest", "position", k, [0, 1.5 * math.sin(ph * 0.5), 0])
        set_kf(tr, "chest", "rotation", k, [0, 5.0 * w, 0])
        set_kf(tr, "neck", "rotation", k, [0, -9.0 * w, 0])
        set_kf(tr, "head", "rotation", k, [2.5 * math.cos(ph), -7.0 * w, 0])
        set_kf(tr, "jaw", "rotation", k, [4.0 + 3.0 * math.sin(ph * 2), 0, 0])
        set_kf(tr, "fin_right", "rotation", k, [0, 0, -8.0 + 14.0 * w])
        set_kf(tr, "fin_left", "rotation", k, [0, 0, 8.0 - 14.0 * w])
        set_kf(tr, "wing_right", "rotation", k, [0, 0, 10.0 * math.sin(ph - 0.5)])
        set_kf(tr, "wing_left", "rotation", k, [0, 0, -10.0 * math.sin(ph - 0.5)])
        for idx, b in enumerate(("tail_1", "tail_2", "tail_3", "tail_4", "tail_fan")):
            set_kf(tr, b, "rotation", k, [0, (12.0 + 3.0 * idx) * math.sin(ph - 0.65 * (idx + 1)), 0])

    loop_clip("animation.thalassor.swim_idle", 2.4, "swim", fn_swim)

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
        "id": "animation.thalassor.maelstrom_spin", "kind": "attack", "duration": 1.8, "loop": "play_once",
        "beats": {"anticipation": 0.40, "strike": 0.85, "impact": 1.30, "recovery": 1.70},
        "tracks": bake(1.8, {
            "chest": [(0.0, [0, 0, 0], [0, 0, 0]), (0.40, [8, -28, -12], [0, 2, 0]), (0.90, [0, 180, 10], [0, 3, 0]), (1.40, [0, 340, 0], [0, 1, 0]), (1.80, [0, 360, 0], [0, 0, 0])],
            "fin_right": [(0.0, [0, 0, 0], None), (0.90, [0, 0, -28], None), (1.80, [0, 0, 0], None)],
            "fin_left": [(0.0, [0, 0, 0], None), (0.90, [0, 0, 28], None), (1.80, [0, 0, 0], None)],
            "tail_1": [(0.0, [0, 0, 0], None), (0.90, [0, -32, 0], None), (1.80, [0, 0, 0], None)],
        }),
    })
    clips.append({
        "id": "animation.thalassor.hydro_beam", "kind": "attack", "duration": 2.0, "loop": "play_once",
        "beats": {"anticipation": 0.45, "strike": 0.85, "impact": 1.45, "recovery": 1.90},
        "tracks": bake(2.0, {
            "chest": [(0.0, [0, 0, 0], [0, 0, 0]), (0.45, [14, 0, 0], [0, 2, 3]), (0.90, [-10, 0, 0], [0, 0, -3]), (2.00, [0, 0, 0], [0, 0, 0])],
            "neck": [(0.0, [0, 0, 0], None), (0.45, [18, 0, 0], None), (0.90, [-12, 0, 0], None), (2.00, [0, 0, 0], None)],
            "head": [(0.0, [0, 0, 0], None), (0.45, [12, 0, 0], None), (0.90, [-4, 0, 0], None), (2.00, [0, 0, 0], None)],
            "jaw": [(0.0, [4, 0, 0], None), (0.45, [28, 0, 0], None), (0.90, [42, 0, 0], None), (1.50, [38, 0, 0], None), (2.00, [4, 0, 0], None)],
            "fin_right": [(0.0, [0, 0, 0], None), (0.90, [0, -24, -22], None), (2.00, [0, 0, 0], None)],
            "fin_left": [(0.0, [0, 0, 0], None), (0.90, [0, 24, 22], None), (2.00, [0, 0, 0], None)],
        }),
    })
    clips.append({
        "id": "animation.thalassor.bite_lunge", "kind": "attack", "duration": 1.4, "loop": "play_once",
        "beats": {"anticipation": 0.35, "strike": 0.65, "impact": 0.85, "recovery": 1.30},
        "tracks": bake(1.4, {
            "chest": [(0.0, [0, 0, 0], [0, 0, 0]), (0.35, [10, 0, 0], [0, 1, 5]), (0.75, [-12, 0, 0], [0, -1, -7]), (1.40, [0, 0, 0], [0, 0, 0])],
            "neck": [(0.0, [0, 0, 0], None), (0.35, [14, 0, 0], None), (0.75, [-14, 0, 0], None), (1.40, [0, 0, 0], None)],
            "jaw": [(0.0, [4, 0, 0], None), (0.45, [44, 0, 0], None), (0.85, [2, 0, 0], None), (1.40, [4, 0, 0], None)],
            "tail_1": [(0.0, [0, 0, 0], None), (0.75, [14, 18, 0], None), (1.40, [0, 0, 0], None)],
        }),
    })
    return {"rig_bones": bone_ids, "joint_limits_deg": {}, "clips": clips}


def deterministic_noise(x: int, y: int, seed: int) -> float:
    n = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def paint_region(img_arr, x0, y0, w, h, mat, face_name, cid):
    palettes = {
        "abyss_scale": ((20, 26, 46), (38, 52, 86), (12, 14, 28), (58, 82, 128)),
        "plastron_gold": ((196, 174, 108), (232, 214, 148), (138, 118, 66), (250, 238, 188)),
        "sea_fan_teal": ((28, 122, 116), (56, 176, 166), (16, 76, 74), (96, 224, 208)),
        "coral_horn": ((38, 162, 148), (82, 222, 198), (22, 104, 96), (144, 248, 228)),
        "ivory_fang": ((228, 222, 204), (250, 246, 234), (174, 166, 148), (255, 255, 248)),
        "abyss_glow": ((48, 232, 255), (142, 250, 255), (18, 168, 204), (224, 255, 255)),
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
            if is_border and mat != "abyss_glow":
                col = [int(col[i] * 0.62 + dark[i] * 0.38) for i in range(3)]
            elif mat == "abyss_scale" and (dx + dy) % 5 == 0 and r > 0.72:
                col = list(accent)
            elif r > 0.88:
                col = [int(col[i] * 0.85 + hi[i] * 0.15) for i in range(3)]
            img_arr[py, px] = [col[0], col[1], col[2], 255]


def pack_and_paint_textures(cubes):
    diffuse = np.zeros((ATLAS_H, ATLAS_W, 4), dtype=np.uint8)
    glow = np.zeros((ATLAS_H, ATLAS_W, 4), dtype=np.uint8)
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
        is_emis = mat == "abyss_glow"
        face_uvs = {}
        for fname, (fw, fh) in {
            "north": (dx, dy), "south": (dx, dy),
            "east": (dz, dy), "west": (dz, dy),
            "up": (dx, dz), "down": (dx, dz),
        }.items():
            ux, vy, rw, rh = alloc(fw, fh)
            paint_region(diffuse, ux, vy, rw, rh, mat, fname, c["id"])
            if is_emis:
                paint_region(glow, ux, vy, rw, rh, mat, fname, c["id"])
            face_uvs[fname] = {"uv": [ux, vy, ux + fw, vy + fh], "texture": 0}
        cube_uvs[c["id"]] = face_uvs

    diff_img = Image.fromarray(diffuse, "RGBA")
    glow_img = Image.fromarray(glow, "RGBA")
    diff_img.save(OUT_DIR / "thalassor_diffuse.png")
    glow_img.save(OUT_DIR / "thalassor_glow.png")
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
            "from": c["from"], "to": c["to"], "autouv": 0, "color": 5,
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
        groups.append({
            "name": bid, "uuid": bone_uuids[bid], "export": True, "locked": False,
            "origin": b["pivot"], "rotation": [0, 0, 0], "color": 0, "children": [], "shade": True, "isOpen": True,
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
                        "channel": ch_name, "data_points": [{"x": str(vec[0]), "y": str(vec[1]), "z": str(vec[2])}],
                        "uuid": stable_uuid(f"kf:{cid}:{bname}:{ch_name}:{t_str}"), "time": float(t_str), "color": -1, "interpolation": "linear",
                    })
            animators[bone_uuids[bname]] = {"name": bname, "type": "bone", "keyframes": kfs}
        bb_anims.append({
            "uuid": stable_uuid("anim:" + cid), "name": cid, "loop": "loop" if clip["loop"] == "loop" else "once",
            "length": clip["duration"], "snapping": 20, "animators": animators,
        })

    return {
        "meta": {"format_version": "5.0", "model_format": "bedrock", "box_uv": False},
        "name": "thalassor", "model_identifier": "thalassor", "resolution": {"width": ATLAS_W, "height": ATLAS_H},
        "elements": elements, "groups": groups, "outliner": [node(r) for r in root_ids],
        "textures": [{"name": "thalassor_diffuse.png", "id": "0", "width": ATLAS_W, "height": ATLAS_H, "use_as_default": True, "visible": True, "internal": True, "source": img_to_data_url(diff_img)}],
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
            "identifier": "geometry.thalassor", "texture_width": ATLAS_W, "texture_height": ATLAS_H,
            "visible_bounds_width": 8.0, "visible_bounds_height": 4.0, "visible_bounds_offset": [0, 1.8, 0],
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

    diff_img, _, cube_uvs = pack_and_paint_textures(creature["cubes"])
    (OUT_DIR / "thalassor.bbmodel").write_text(json.dumps(to_bbmodel(creature, anim_manifest, diff_img, cube_uvs), indent=2) + "\n", encoding="utf-8")
    gp = OUT_DIR / "thalassor.geo.json"
    ajp = OUT_DIR / "thalassor.animation.json"
    gp.write_text(json.dumps(to_geo_json(creature, cube_uvs), indent=2) + "\n", encoding="utf-8")
    ajp.write_text(json.dumps(to_animation_json(anim_manifest), indent=2) + "\n", encoding="utf-8")
    print(f"Built Thalassor: {len(creature['bones'])} bones, {len(creature['cubes'])} cubes, {len(anim_manifest['clips'])} clips.")

    if "--render" in sys.argv:
        sys.path.insert(0, str(SCRATCH_RENDER))
        import bb_render
        gm = bb_render.GeoModel(gp, ajp, OUT_DIR / "thalassor_diffuse.png", OUT_DIR / "thalassor_glow.png")
        bb_render.render_suite("thalassor", gm, REF_IMG, [c["id"] for c in anim_manifest["clips"]], ARTIFACT_DIR, scale=2.1, center_y=22.0)
        print("Rendered Thalassor compare, contact sheet, and 4 GIFs.")


if __name__ == "__main__":
    main()
