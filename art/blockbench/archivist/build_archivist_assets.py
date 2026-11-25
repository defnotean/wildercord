#!/usr/bin/env python3
"""Build 34-bone articulated Archivist rig, dual emissive layers (eyes + runes), and 5 clips."""
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
REF_IMG = ARTIFACT_DIR / "archivist_turnaround_v3_1795610578887.jpg"
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
    # ~3 blocks tall (48-51px), floating_entity: False grounded robe hem at Y=0
    bones = [
        {"id": "root", "parent": None, "pivot": [0, 0, 0]},
        {"id": "pelvis", "parent": "root", "pivot": [0, 14.0, 0.0]},
        {"id": "hem_front", "parent": "pelvis", "pivot": [0, 14.0, -4.5]},
        {"id": "hem_back", "parent": "pelvis", "pivot": [0, 14.0, 4.5]},
        {"id": "hem_right", "parent": "pelvis", "pivot": [5.5, 14.0, 0.0], "mirror_of": "hem_left"},
        {"id": "hem_left", "parent": "pelvis", "pivot": [-5.5, 14.0, 0.0], "mirror_of": "hem_right"},
        {"id": "waist", "parent": "pelvis", "pivot": [0, 22.0, 0.0]},
        {"id": "chest", "parent": "waist", "pivot": [0, 30.0, 0.0]},
        {"id": "stole_right", "parent": "chest", "pivot": [3.0, 33.0, -4.5], "mirror_of": "stole_left"},
        {"id": "stole_left", "parent": "chest", "pivot": [-3.0, 33.0, -4.5], "mirror_of": "stole_right"},
        {"id": "mantle", "parent": "chest", "pivot": [0, 38.0, 0.0]},
        {"id": "head", "parent": "chest", "pivot": [0, 39.5, 0.0]},
        {"id": "hood_tip", "parent": "head", "pivot": [0, 49.0, 4.5]},
        {"id": "upper_arm_right", "parent": "chest", "pivot": [7.5, 37.5, 0.0], "mirror_of": "upper_arm_left"},
        {"id": "upper_arm_left", "parent": "chest", "pivot": [-7.5, 37.5, 0.0], "mirror_of": "upper_arm_right"},
        {"id": "forearm_right", "parent": "upper_arm_right", "pivot": [8.5, 29.0, 0.0], "mirror_of": "forearm_left"},
        {"id": "forearm_left", "parent": "upper_arm_left", "pivot": [-8.5, 29.0, 0.0], "mirror_of": "forearm_right"},
        {"id": "sleeve_right", "parent": "forearm_right", "pivot": [8.5, 23.0, 0.0], "mirror_of": "sleeve_left"},
        {"id": "sleeve_left", "parent": "forearm_left", "pivot": [-8.5, 23.0, 0.0], "mirror_of": "sleeve_right"},
        # Orbiting floating grimoire + 3 loose pages
        {"id": "tome", "parent": "chest", "pivot": [0, 28.0, -10.0]},
        {"id": "tome_right", "parent": "tome", "pivot": [0.5, 28.0, -10.0]},
        {"id": "tome_left", "parent": "tome", "pivot": [-0.5, 28.0, -10.0]},
        {"id": "tome_page", "parent": "tome", "pivot": [0.0, 28.8, -10.0]},
        {"id": "orbit", "parent": "chest", "pivot": [0, 37.0, 0.0]},
        {"id": "page_0", "parent": "orbit", "pivot": [12.0, 40.0, -4.0]},
        {"id": "page_1", "parent": "orbit", "pivot": [-12.0, 38.0, -3.0]},
        {"id": "page_2", "parent": "orbit", "pivot": [0.0, 42.0, 11.0]},
        {"id": "sigil_crest", "parent": "chest", "pivot": [0, 33.5, -4.2]},
    ]
    cubes = []

    # Lower flared robe & trailing golden embroidered hem touching Y=0
    cubes.append(make_cube("skirt_upper", "pelvis", "robe_navy", [-6.0, 8.0, -4.2], [6.0, 22.0, 4.2]))
    cubes.append(make_cube("hem_f_panel", "hem_front", "robe_navy", [-6.8, 0.0, -5.2], [6.8, 10.0, -3.8]))
    cubes.append(make_cube("hem_f_trim", "hem_front", "brass_gold", [-7.0, 0.0, -5.4], [7.0, 1.5, -3.6], inflate=0.04))
    cubes.append(make_cube("hem_b_panel", "hem_back", "robe_navy", [-7.2, 0.0, 3.8], [7.2, 11.0, 5.6]))
    cubes.append(make_cube("hem_b_trim", "hem_back", "brass_gold", [-7.4, 0.0, 3.9], [7.4, 1.6, 5.8], inflate=0.04))
    cubes.extend(mirror_cube("hem_r_flare", "hem_l_flare", "hem_right", "hem_left", "robe_navy", [5.2, 0.0, -4.6], [7.6, 11.0, 4.6]))
    cubes.extend(mirror_cube("hem_r_gold", "hem_l_gold", "hem_right", "hem_left", "brass_gold", [5.4, 0.0, -4.8], [7.8, 1.4, 4.8], inflate=0.04))

    # Waist, chest, layered stole with glowing runes, chestplate sigil, and layered shoulder mantle
    cubes.append(make_cube("waist_sash", "waist", "robe_slate", [-5.4, 21.5, -3.8], [5.4, 30.2, 3.8]))
    cubes.append(make_cube("chest_core", "chest", "robe_navy", [-6.2, 29.5, -4.2], [6.2, 39.2, 4.2]))
    cubes.extend(mirror_cube("stole_r_cloth", "stole_l_cloth", "stole_right", "stole_left", "robe_slate", [1.6, 6.0, -4.9], [4.4, 38.5, -3.8]))
    cubes.extend(mirror_cube("stole_r_rune", "stole_l_rune", "stole_right", "stole_left", "rune_glow", [2.1, 8.0, -5.1], [3.9, 36.5, -4.6]))
    cubes.append(make_cube("chest_sigil", "sigil_crest", "eye_glow", [-1.6, 32.0, -4.7], [1.6, 35.2, -3.9]))
    cubes.append(make_cube("mantle_shawl", "mantle", "robe_navy", [-8.4, 36.5, -4.8], [8.4, 40.4, 4.8]))
    cubes.append(make_cube("mantle_gold", "mantle", "brass_gold", [-8.6, 36.2, -5.0], [8.6, 37.5, 5.0], inflate=0.05))
    cubes.append(make_cube("back_embroidery", "chest", "brass_gold", [-2.4, 6.0, 4.1], [2.4, 38.0, 4.7]))

    # Deep peaked hood cowl shadowing a dark void face + glowing cyan eyes
    cubes.append(make_cube("face_void", "head", "void_dark", [-4.0, 39.6, -4.0], [4.0, 47.6, 3.4]))
    cubes.append(make_cube("hood_top", "head", "robe_navy", [-5.4, 47.2, -5.4], [5.4, 50.8, 5.2]))
    cubes.append(make_cube("hood_back", "head", "robe_navy", [-5.4, 39.5, 2.8], [5.4, 48.0, 5.4]))
    cubes.extend(mirror_cube("hood_side_r", "hood_side_l", "head", "head", "robe_navy", [3.8, 39.6, -5.0], [5.6, 48.0, 4.2]))
    cubes.append(make_cube("hood_brim_gold", "head", "brass_gold", [-5.2, 46.8, -5.9], [5.2, 48.8, -4.2]))
    cubes.extend(mirror_cube("hood_clasp_r", "hood_clasp_l", "head", "head", "brass_gold", [4.8, 42.8, -3.4], [6.2, 46.2, -0.2]))
    cubes.append(make_cube("hood_peak", "hood_tip", "robe_navy", [-3.4, 49.0, 1.5], [3.4, 52.8, 7.0], rotation=[-18, 0, 0]))
    cubes.append(make_cube("eye_r", "head", "eye_glow", [1.0, 43.2, -4.2], [3.2, 44.6, -3.2]))
    cubes.append(make_cube("eye_l", "head", "eye_glow", [-3.2, 43.2, -4.2], [-1.0, 44.6, -3.2]))

    # Wide Bell-Sleeved Arms
    cubes.extend(mirror_cube("uarm_r", "uarm_l", "upper_arm_right", "upper_arm_left", "robe_navy", [5.8, 28.5, -2.5], [9.8, 38.2, 2.5]))
    cubes.extend(mirror_cube("uarm_r_pad", "uarm_l_pad", "upper_arm_right", "upper_arm_left", "robe_slate", [6.0, 35.8, -2.8], [10.4, 38.8, 2.8]))
    cubes.extend(mirror_cube("farm_r", "farm_l", "forearm_right", "forearm_left", "robe_navy", [6.0, 21.0, -3.0], [10.4, 29.0, 3.0]))
    cubes.extend(mirror_cube("sleeve_r_flare", "sleeve_l_flare", "sleeve_right", "sleeve_left", "robe_navy", [6.2, 16.5, -3.4], [10.8, 23.0, 3.4]))
    cubes.extend(mirror_cube("sleeve_r_trim", "sleeve_l_trim", "sleeve_right", "sleeve_left", "brass_gold", [6.1, 16.2, -3.6], [10.9, 17.6, 3.6], inflate=0.04))
    cubes.extend(mirror_cube("hand_r", "hand_l", "forearm_right", "forearm_left", "void_dark", [6.8, 19.2, -1.6], [9.6, 22.0, 1.6]))

    # Open Brass Grimoire + Turning Page + 3 Orbiting Loose Pages
    cubes.append(make_cube("tome_spine", "tome", "brass_gold", [-0.8, 25.5, -13.0], [0.8, 31.0, -7.2], rotation=[34, 0, 0], origin=[0, 28, -10]))
    cubes.append(make_cube("tome_r_cov", "tome_right", "leather_brown", [0.4, 25.6, -13.0], [6.4, 30.8, -7.2], rotation=[34, 0, -24], origin=[0.5, 28.0, -10]))
    cubes.append(make_cube("tome_r_pg", "tome_right", "rune_glow", [0.6, 26.4, -12.6], [5.8, 30.5, -7.6], rotation=[34, 0, -24], origin=[0.5, 28.0, -10]))
    cubes.append(make_cube("tome_l_cov", "tome_left", "leather_brown", [-6.4, 25.6, -13.0], [-0.4, 30.8, -7.2], rotation=[34, 0, 24], origin=[-0.5, 28.0, -10]))
    cubes.append(make_cube("tome_l_pg", "tome_left", "rune_glow", [-5.8, 26.4, -12.6], [-0.6, 30.5, -7.6], rotation=[34, 0, 24], origin=[-0.5, 28.0, -10]))
    cubes.append(make_cube("tome_turn_pg", "tome_page", "parchment", [-0.3, 27.0, -12.4], [0.3, 31.2, -7.8], rotation=[34, 0, 8], origin=[0, 28.8, -10]))

    cubes.append(make_cube("orb_p0_b", "page_0", "parchment", [10.0, 38.0, -5.0], [14.0, 43.0, -4.4], rotation=[10, -25, 8]))
    cubes.append(make_cube("orb_p0_g", "page_0", "rune_glow", [10.6, 38.8, -5.15], [13.4, 42.2, -4.25], rotation=[10, -25, 8]))
    cubes.append(make_cube("orb_p1_b", "page_1", "parchment", [-14.0, 36.0, -3.5], [-10.0, 41.0, -2.9], rotation=[-12, 22, -6]))
    cubes.append(make_cube("orb_p1_g", "page_1", "rune_glow", [-13.4, 36.8, -3.65], [-10.6, 40.2, -2.75], rotation=[-12, 22, -6]))
    cubes.append(make_cube("orb_p2_b", "page_2", "parchment", [-2.0, 39.5, 10.6], [2.0, 44.5, 11.2], rotation=[8, 0, 12]))
    cubes.append(make_cube("orb_p2_g", "page_2", "rune_glow", [-1.4, 40.3, 10.45], [1.4, 43.7, 11.35], rotation=[8, 0, 12]))

    materials = [
        {"id": "robe_navy", "type": "opaque"},
        {"id": "robe_slate", "type": "opaque"},
        {"id": "brass_gold", "type": "opaque"},
        {"id": "leather_brown", "type": "opaque"},
        {"id": "parchment", "type": "opaque"},
        {"id": "void_dark", "type": "opaque"},
        {"id": "eye_glow", "type": "emissive"},
        {"id": "rune_glow", "type": "emissive"},
    ]
    return {"creature_id": "archivist", "ground_y": 0.0, "floating_entity": False, "materials": materials, "bones": bones, "cubes": cubes}


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

    def fn_idle(ph, k, tr):
        s1, c1 = math.sin(ph), math.cos(ph)
        set_kf(tr, "chest", "position", k, [0, 0.4 * s1, 0])
        set_kf(tr, "chest", "rotation", k, [-2.0 + 1.5 * s1, 2.0 * c1, 0])
        set_kf(tr, "head", "rotation", k, [2.0 * c1, -3.0 * c1, 0])
        set_kf(tr, "upper_arm_right", "rotation", k, [8.0 + 4.0 * s1, 0, -5.0])
        set_kf(tr, "upper_arm_left", "rotation", k, [8.0 - 4.0 * s1, 0, 5.0])
        set_kf(tr, "tome", "position", k, [0, 0.9 * s1, 0])
        set_kf(tr, "tome_page", "rotation", k, [0, 0, 35.0 * s1])
        set_kf(tr, "orbit", "rotation", k, [0, 360.0 * (ph / (2 * math.pi)) if ph > 0 else 0, 0])

    # Keep loop seam exact at 0/dur for orbit (use sin wave or linear 0->0? wait: validate_animation_manifest requires start==end for loop! So modulate orbit Y by +/-24° or keep discrete!)
    def fn_idle_clean(ph, k, tr):
        s1, c1 = math.sin(ph), math.cos(ph)
        set_kf(tr, "chest", "position", k, [0, 0.4 * s1, 0])
        set_kf(tr, "chest", "rotation", k, [-2.0 + 1.5 * s1, 2.0 * c1, 0])
        set_kf(tr, "head", "rotation", k, [2.0 * c1, -3.0 * c1, 0])
        set_kf(tr, "upper_arm_right", "rotation", k, [8.0 + 4.0 * s1, 0, -5.0])
        set_kf(tr, "upper_arm_left", "rotation", k, [8.0 - 4.0 * s1, 0, 5.0])
        set_kf(tr, "tome", "position", k, [0, 0.9 * s1, 0])
        set_kf(tr, "tome_page", "rotation", k, [0, 0, 35.0 * s1])
        set_kf(tr, "orbit", "rotation", k, [0, 22.0 * s1, 4.0 * c1])

    loop_clip("animation.archivist.idle", 2.4, "idle", fn_idle_clean)

    def fn_walk(ph, k, tr):
        s1, c1 = math.sin(ph), math.cos(ph)
        set_kf(tr, "pelvis", "position", k, [0, 0.8 + 0.5 * abs(s1), 0])
        set_kf(tr, "pelvis", "rotation", k, [-5.0, 4.0 * c1, 2.0 * s1])
        set_kf(tr, "hem_front", "rotation", k, [8.0 + 6.0 * abs(s1), 0, 0])
        set_kf(tr, "hem_back", "rotation", k, [12.0 + 6.0 * s1, 0, 0])
        set_kf(tr, "chest", "rotation", k, [-4.0, -4.0 * c1, 0])
        set_kf(tr, "head", "rotation", k, [7.0, 3.0 * c1, 0])
        set_kf(tr, "upper_arm_right", "rotation", k, [16.0 - 10.0 * c1, 0, -6.0])
        set_kf(tr, "upper_arm_left", "rotation", k, [16.0 + 10.0 * c1, 0, 6.0])
        set_kf(tr, "tome", "position", k, [0, 0.8 * s1, -1.0])
        set_kf(tr, "orbit", "rotation", k, [0, 28.0 * s1, 0])

    loop_clip("animation.archivist.walk", 1.2, "walk", fn_walk)

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
        "id": "animation.archivist.cast", "kind": "attack", "duration": 1.4, "loop": "play_once",
        "beats": {"anticipation": 0.35, "strike": 0.70, "impact": 0.90, "recovery": 1.30},
        "tracks": bake(1.4, {
            "chest": [(0.0, [0, 0, 0], None), (0.35, [8, 0, 0], [0, 0.8, 0]), (0.85, [-10, 0, 0], [0, 0, -2]), (1.40, [0, 0, 0], [0, 0, 0])],
            "head": [(0.0, [0, 0, 0], None), (0.35, [12, 0, 0], None), (0.85, [-6, 0, 0], None), (1.40, [0, 0, 0], None)],
            "upper_arm_right": [(0.0, [0, 0, 0], None), (0.70, [118, 0, -18], None), (1.40, [0, 0, 0], None)],
            "upper_arm_left": [(0.0, [0, 0, 0], None), (0.70, [118, 0, 18], None), (1.40, [0, 0, 0], None)],
            "tome": [(0.0, [0, 0, 0], [0, 0, 0]), (0.70, [-18, 0, 0], [0, 8.0, -3.0]), (1.40, [0, 0, 0], [0, 0, 0])],
            "orbit": [(0.0, [0, 0, 0], None), (0.70, [0, 90, 0], None), (1.40, [0, 0, 0], None)],
        }),
    })
    clips.append({
        "id": "animation.archivist.rewrite", "kind": "attack", "duration": 2.0, "loop": "play_once",
        "beats": {"anticipation": 0.45, "strike": 1.00, "impact": 1.40, "recovery": 1.85},
        "tracks": bake(2.0, {
            "chest": [(0.0, [0, 0, 0], [0, 0, 0]), (1.00, [14, 0, 0], [0, 2.5, 0]), (2.00, [0, 0, 0], [0, 0, 0])],
            "head": [(0.0, [0, 0, 0], None), (1.00, [22, 0, 0], None), (2.00, [0, 0, 0], None)],
            "upper_arm_right": [(0.0, [0, 0, 0], None), (1.00, [82, 0, -68], None), (2.00, [0, 0, 0], None)],
            "upper_arm_left": [(0.0, [0, 0, 0], None), (1.00, [82, 0, 68], None), (2.00, [0, 0, 0], None)],
            "tome": [(0.0, [0, 0, 0], [0, 0, 0]), (1.00, [-24, 0, 0], [0, 16.0, 2.0]), (2.00, [0, 0, 0], [0, 0, 0])],
            "orbit": [(0.0, [0, 0, 0], None), (1.00, [0, 160, 0], None), (2.00, [0, 0, 0], None)],
        }),
    })
    clips.append({
        "id": "animation.archivist.trick_slam", "kind": "attack", "duration": 1.4, "loop": "play_once",
        "beats": {"anticipation": 0.35, "strike": 0.65, "impact": 0.85, "recovery": 1.30},
        "tracks": bake(1.4, {
            "pelvis": [(0.0, [0, 0, 0], [0, 0, 0]), (0.35, [4, -18, 0], [0, 0, 1]), (0.85, [-10, 18, 0], [0, -1.5, -4]), (1.40, [0, 0, 0], [0, 0, 0])],
            "chest": [(0.0, [0, 0, 0], None), (0.35, [6, -20, 0], None), (0.85, [-14, 20, 0], None), (1.40, [0, 0, 0], None)],
            "upper_arm_right": [(0.0, [0, 0, 0], None), (0.35, [148, -14, -18], None), (0.65, [86, 10, 4], None), (0.85, [30, 18, 12], None), (1.40, [0, 0, 0], None)],
            "upper_arm_left": [(0.0, [0, 0, 0], None), (0.35, [72, 12, 14], None), (0.85, [36, 0, 18], None), (1.40, [0, 0, 0], None)],
            "tome_right": [(0.0, [0, 0, 0], None), (0.85, [0, 0, 24], None), (1.40, [0, 0, 0], None)],
            "tome_left": [(0.0, [0, 0, 0], None), (0.85, [0, 0, -24], None), (1.40, [0, 0, 0], None)],
        }),
    })
    return {"rig_bones": bone_ids, "joint_limits_deg": {}, "clips": clips}


def deterministic_noise(x: int, y: int, seed: int) -> float:
    n = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def paint_region(img_arr, x0, y0, w, h, mat, face_name, cid):
    palettes = {
        "robe_navy": ((28, 34, 58), (48, 58, 94), (16, 20, 36), (72, 86, 132)),
        "robe_slate": ((42, 52, 82), (68, 84, 126), (24, 30, 50), (92, 114, 162)),
        "brass_gold": ((188, 152, 72), (232, 194, 102), (118, 90, 36), (250, 222, 146)),
        "leather_brown": ((78, 48, 32), (110, 72, 48), (48, 28, 18), (146, 98, 64)),
        "parchment": ((222, 210, 178), (244, 234, 204), (176, 162, 132), (255, 248, 226)),
        "void_dark": ((10, 10, 16), (18, 18, 28), (6, 6, 10), (26, 26, 40)),
        "eye_glow": ((86, 232, 255), (172, 248, 255), (42, 176, 212), (234, 255, 255)),
        "rune_glow": ((72, 224, 248), (158, 244, 255), (32, 164, 198), (222, 252, 255)),
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
            elif mat == "rune_glow":
                if (dx + dy * 2) % 3 == 0:
                    col = list(accent)
                elif r < 0.28:
                    img_arr[py, px] = [0, 0, 0, 0]
                    continue
            elif r > 0.88:
                col = [int(col[i] * 0.85 + hi[i] * 0.15) for i in range(3)]
            img_arr[py, px] = [col[0], col[1], col[2], 255]


def pack_and_paint_textures(cubes):
    diffuse = np.zeros((ATLAS_H, ATLAS_W, 4), dtype=np.uint8)
    eyes = np.zeros((ATLAS_H, ATLAS_W, 4), dtype=np.uint8)
    runes = np.zeros((ATLAS_H, ATLAS_W, 4), dtype=np.uint8)
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
            if mat == "eye_glow":
                paint_region(eyes, ux, vy, rw, rh, mat, fname, c["id"])
            elif mat == "rune_glow":
                paint_region(runes, ux, vy, rw, rh, mat, fname, c["id"])
            face_uvs[fname] = {"uv": [ux, vy, ux + fw, vy + fh], "texture": 0}
        cube_uvs[c["id"]] = face_uvs

    diff_img = Image.fromarray(diffuse, "RGBA")
    eyes_img = Image.fromarray(eyes, "RGBA")
    runes_img = Image.fromarray(runes, "RGBA")
    diff_img.save(OUT_DIR / "archivist_diffuse.png")
    eyes_img.save(OUT_DIR / "archivist_eyes.png")
    runes_img.save(OUT_DIR / "archivist_runes.png")
    combined_glow = Image.alpha_composite(runes_img, eyes_img)
    combined_glow.save(OUT_DIR / "archivist_glow_preview.png")
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
            "from": c["from"], "to": c["to"], "autouv": 0, "color": 3,
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
                        "channel": ch_name, "data_points": [{"x": str(vec[0]), "y": str(vec[1]), "z": str(vec[2])}],
                        "uuid": stable_uuid(f"kf:{cid}:{bname}:{ch_name}:{t_str}"), "time": float(t_str), "color": -1, "interpolation": "linear",
                    })
            animators[bone_uuids[bname]] = {"name": bname, "type": "bone", "rotation_global": False, "quaternion_interpolation": False, "keyframes": kfs}
        bb_anims.append({
            "uuid": stable_uuid("anim:" + cid), "name": cid, "loop": "loop" if clip["loop"] == "loop" else "once",
            "override": False, "length": clip["duration"], "snapping": 20, "selected": cid == "animation.archivist.cast",
            "saved": True, "path": "", "scope": 0, "anim_time_update": "", "blend_weight": "", "start_delay": "", "loop_delay": "", "animators": animators,
        })

    return {
        "meta": {"format_version": "5.0", "model_format": "bedrock", "box_uv": False},
        "name": "archivist", "model_identifier": "archivist",
        "visible_box": [2.5, 3.5, 0], "resolution": {"width": ATLAS_W, "height": ATLAS_H},
        "elements": elements, "groups": groups, "outliner": [node(r) for r in root_ids],
        "textures": [{
            "name": "archivist_diffuse.png", "path": "", "folder": "", "namespace": "", "id": "0",
            "width": ATLAS_W, "height": ATLAS_H, "uv_width": ATLAS_W, "uv_height": ATLAS_H,
            "use_as_default": True, "visible": True, "internal": True, "uuid": stable_uuid("tex:diffuse"),
            "source": img_to_data_url(diff_img),
        }],
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
        "minecraft:geometry": [{"description": {
            "identifier": "geometry.archivist", "texture_width": ATLAS_W, "texture_height": ATLAS_H,
            "visible_bounds_width": 3.0, "visible_bounds_height": 3.6, "visible_bounds_offset": [0, 1.8, 0],
        }, "bones": bones_out}],
    }


def to_animation_json(anim_manifest):
    anims_out = {}
    for clip in anim_manifest["clips"]:
        entry = {
            "animation_length": clip["duration"],
            "bones": {
                bname: {ch: {t: list(v) for t, v in sorted(cm.items(), key=lambda kv: float(kv[0]))} for ch, cm in chs.items()}
                for bname, chs in clip["tracks"].items()
            },
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
    (OUT_DIR / "archivist.bbmodel").write_text(json.dumps(to_bbmodel(creature, anim_manifest, diff_img, cube_uvs), indent=2) + "\n", encoding="utf-8")
    gp = OUT_DIR / "archivist.geo.json"
    ajp = OUT_DIR / "archivist.animation.json"
    gp.write_text(json.dumps(to_geo_json(creature, cube_uvs), indent=2) + "\n", encoding="utf-8")
    ajp.write_text(json.dumps(to_animation_json(anim_manifest), indent=2) + "\n", encoding="utf-8")
    print(f"Built Archivist: {len(creature['bones'])} bones, {len(creature['cubes'])} cubes, {len(anim_manifest['clips'])} clips.")

    if "--render" in sys.argv:
        sys.path.insert(0, str(SCRATCH_RENDER))
        import bb_render
        gm = bb_render.GeoModel(gp, ajp, OUT_DIR / "archivist_diffuse.png", OUT_DIR / "archivist_glow_preview.png")
        bb_render.render_suite("archivist", gm, REF_IMG, [c["id"] for c in anim_manifest["clips"]], ARTIFACT_DIR, scale=4.6, center_y=27.0)
        print("Rendered Archivist compare, contact sheet, and 5 GIFs.")


if __name__ == "__main__":
    main()
