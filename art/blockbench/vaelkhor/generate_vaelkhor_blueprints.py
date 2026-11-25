#!/usr/bin/env python3
"""Generate authoritative anatomical and kinematic blueprints for Vaelkhor, the Leyline Sovereign."""
from __future__ import annotations

import json
import math
from pathlib import Path
import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
import matplotlib.patches as patches
import numpy as np

OUT_DIR = Path(__file__).resolve().parent
OUT_DIR.mkdir(parents=True, exist_ok=True)


def mirror_cube(cid_l: str, cid_r: str, bone_l: str, bone_r: str, mat: str, lo, hi, inflate: float = 0.0):
    cl = {
        "id": cid_l,
        "bone": bone_l,
        "material": mat,
        "from": list(lo),
        "to": list(hi),
    }
    cr = {
        "id": cid_r,
        "bone": bone_r,
        "material": mat,
        "from": [-hi[0], lo[1], lo[2]],
        "to": [-lo[0], hi[1], hi[2]],
    }
    if inflate:
        cl["inflate"] = inflate
        cr["inflate"] = inflate
    return [cl, cr]


def build_anatomy():
    bones = [
        {"id": "root", "parent": None, "pivot": [0, 0, 0]},
        {"id": "pelvis", "parent": "root", "pivot": [0, 26, 0]},
        {"id": "skirt_front", "parent": "pelvis", "pivot": [0, 24, -4]},
        {"id": "skirt_back", "parent": "pelvis", "pivot": [0, 24, 4]},
        {"id": "thigh_left", "parent": "pelvis", "pivot": [7.5, 25, 0], "mirror_of": "thigh_right"},
        {"id": "thigh_right", "parent": "pelvis", "pivot": [-7.5, 25, 0], "mirror_of": "thigh_left"},
        {"id": "shin_left", "parent": "thigh_left", "pivot": [7.5, 14, 1], "mirror_of": "shin_right"},
        {"id": "shin_right", "parent": "thigh_right", "pivot": [-7.5, 14, 1], "mirror_of": "shin_left"},
        {"id": "foot_left", "parent": "shin_left", "pivot": [7.5, 4, 0], "mirror_of": "foot_right"},
        {"id": "foot_right", "parent": "shin_right", "pivot": [-7.5, 4, 0], "mirror_of": "foot_left"},
        {"id": "tail_0", "parent": "pelvis", "pivot": [0, 25, 5]},
        {"id": "tail_1", "parent": "tail_0", "pivot": [0, 24, 13]},
        {"id": "tail_2", "parent": "tail_1", "pivot": [0, 23.5, 21]},
        {"id": "chest", "parent": "pelvis", "pivot": [0, 29, 0]},
        {"id": "core_crystal", "parent": "chest", "pivot": [0, 36, -1]},
        {"id": "wing_ring_left", "parent": "chest", "pivot": [4, 41, 6], "mirror_of": "wing_ring_right"},
        {"id": "wing_ring_right", "parent": "chest", "pivot": [-4, 41, 6], "mirror_of": "wing_ring_left"},
        {"id": "upper_arm_left", "parent": "chest", "pivot": [11.5, 42, 0], "mirror_of": "upper_arm_right"},
        {"id": "upper_arm_right", "parent": "chest", "pivot": [-11.5, 42, 0], "mirror_of": "upper_arm_left"},
        {"id": "upper_forearm_left", "parent": "upper_arm_left", "pivot": [11.5, 32, 0], "mirror_of": "upper_forearm_right"},
        {"id": "upper_forearm_right", "parent": "upper_arm_right", "pivot": [-11.5, 32, 0], "mirror_of": "upper_forearm_left"},
        {"id": "bell_lantern", "parent": "upper_forearm_left", "pivot": [11.5, 22, -1]},
        {"id": "halberd", "parent": "upper_forearm_right", "pivot": [-11.5, 22, -1]},
        {"id": "lower_arm_left", "parent": "chest", "pivot": [7.5, 33, -2], "mirror_of": "lower_arm_right"},
        {"id": "lower_arm_right", "parent": "chest", "pivot": [-7.5, 33, -2], "mirror_of": "lower_arm_left"},
        {"id": "lower_forearm_left", "parent": "lower_arm_left", "pivot": [7.5, 29, -7], "mirror_of": "lower_forearm_right"},
        {"id": "lower_forearm_right", "parent": "lower_arm_right", "pivot": [-7.5, 29, -7], "mirror_of": "lower_forearm_left"},
        {"id": "neck", "parent": "chest", "pivot": [0, 44, -1]},
        {"id": "head", "parent": "neck", "pivot": [0, 48, -2]},
        {"id": "jaw", "parent": "head", "pivot": [0, 50, -4]},
        {"id": "halo_ring", "parent": "head", "pivot": [0, 58, 4]},
    ]

    cubes = [
        # Pelvis & Vestments
        {"id": "pelvis_core", "bone": "pelvis", "material": "obsidian_armor", "from": [-6, 23, -4], "to": [6, 29, 5]},
        {"id": "pelvis_buckle", "bone": "pelvis", "material": "gilded_bronze", "from": [-3, 24, -5], "to": [3, 28, -4], "inflate": 0.1},
        {"id": "skirt_front_panel", "bone": "skirt_front", "material": "royal_vestment", "from": [-4, 12, -5], "to": [4, 24, -3.5]},
        {"id": "skirt_front_trim", "bone": "skirt_front", "material": "gilded_bronze", "from": [-2, 10, -5.2], "to": [2, 13, -3.4], "inflate": 0.05},
        {"id": "skirt_back_panel", "bone": "skirt_back", "material": "royal_vestment", "from": [-6, 9, 3.5], "to": [6, 24, 5.5]},
        {"id": "skirt_back_trim", "bone": "skirt_back", "material": "leyline_emissive", "from": [-3, 8, 4.0], "to": [3, 10, 5.6], "inflate": 0.05},
    ]

    # Digitigrade Strider Legs
    cubes += mirror_cube("thigh_l", "thigh_r", "thigh_left", "thigh_right", "obsidian_armor", [5, 14, -3], [10, 26, 3])
    cubes += mirror_cube("tasset_l", "tasset_r", "thigh_left", "thigh_right", "gilded_bronze", [5, 21, -4], [11, 27, 4], inflate=0.15)
    cubes += mirror_cube("shin_l", "shin_r", "shin_left", "shin_right", "verdigris_copper", [5.5, 4, -1], [9.5, 15, 3])
    cubes += mirror_cube("knee_spur_l", "knee_spur_r", "shin_left", "shin_right", "gilded_bronze", [6.5, 12, 3], [8.5, 18, 5])
    cubes += mirror_cube("foot_l_base", "foot_r_base", "foot_left", "foot_right", "obsidian_armor", [5, 0, -5], [10, 4, 2])
    cubes += mirror_cube("foot_l_claw_in", "foot_r_claw_in", "foot_left", "foot_right", "gilded_bronze", [5, 0, -7], [7, 2, -5])
    cubes += mirror_cube("foot_l_claw_out", "foot_r_claw_out", "foot_left", "foot_right", "gilded_bronze", [8, 0, -7], [10, 2, -5])

    # Pendulum Tail
    cubes += [
        {"id": "tail_seg0", "bone": "tail_0", "material": "obsidian_armor", "from": [-2.5, 22, 5], "to": [2.5, 26, 13]},
        {"id": "tail_seg1", "bone": "tail_1", "material": "verdigris_copper", "from": [-2, 22, 13], "to": [2, 25, 21]},
        {"id": "tail_cradle", "bone": "tail_2", "material": "gilded_bronze", "from": [-3, 21, 21], "to": [3, 26, 25]},
        {"id": "tail_plumb_crystal", "bone": "tail_2", "material": "leyline_emissive", "from": [-1.5, 22, 25], "to": [1.5, 25, 31]},
    ]

    # Open Reliquary Ribcage & Spindle Core
    cubes += [
        {"id": "spine_column", "bone": "chest", "material": "obsidian_armor", "from": [-3, 29, 1], "to": [3, 44, 6]},
        {"id": "collar_arch", "bone": "chest", "material": "gilded_bronze", "from": [-5, 40, -6], "to": [5, 44, 2]},
        {"id": "core_diamond", "bone": "core_crystal", "material": "leyline_emissive", "from": [-2.5, 33, -3.5], "to": [2.5, 39, 1.5]},
        {"id": "core_spindle", "bone": "core_crystal", "material": "gilded_bronze", "from": [-1, 30, -2], "to": [1, 42, 0]},
    ]
    cubes += mirror_cube("rib_upper_l", "rib_upper_r", "chest", "chest", "obsidian_armor", [3, 37, -5], [9, 40, 4])
    cubes += mirror_cube("rib_lower_l", "rib_lower_r", "chest", "chest", "verdigris_copper", [4, 32, -4], [8, 35, 3])
    cubes += mirror_cube("pauldron_l", "pauldron_r", "chest", "chest", "gilded_bronze", [7, 41, -5], [15, 47, 5], inflate=0.1)
    cubes += mirror_cube("pauldron_crest_l", "pauldron_crest_r", "chest", "chest", "verdigris_copper", [10, 47, -2], [14, 51, 3])

    # Split Astrolabe Arc-Wings
    cubes += mirror_cube("wing_strut_l", "wing_strut_r", "wing_ring_left", "wing_ring_right", "gilded_bronze", [4, 39, 6], [16, 43, 8])
    cubes += mirror_cube("wing_arc_top_l", "wing_arc_top_r", "wing_ring_left", "wing_ring_right", "verdigris_copper", [14, 43, 6.5], [23, 57, 8.5])
    cubes += mirror_cube("wing_arc_bot_l", "wing_arc_bot_r", "wing_ring_left", "wing_ring_right", "obsidian_armor", [13, 27, 6.5], [20, 39, 8.5])
    cubes += mirror_cube("wing_shard_cyan_l", "wing_shard_cyan_r", "wing_ring_left", "wing_ring_right", "leyline_emissive", [23, 46, 7], [27, 55, 8])
    cubes += mirror_cube("wing_shard_amber_l", "wing_shard_amber_r", "wing_ring_left", "wing_ring_right", "amber_emissive", [19, 29, 7], [23, 36, 8])

    # Primary Upper Arms + Weapons
    cubes += mirror_cube("u_arm_l", "u_arm_r", "upper_arm_left", "upper_arm_right", "obsidian_armor", [9, 32, -2], [14, 43, 3])
    cubes += mirror_cube("u_forearm_l", "u_forearm_r", "upper_forearm_left", "upper_forearm_right", "verdigris_copper", [9.5, 22, -3], [13.5, 32, 2])
    cubes += mirror_cube("u_gauntlet_l", "u_gauntlet_r", "upper_forearm_left", "upper_forearm_right", "gilded_bronze", [9, 21, -3.5], [14, 24, 2.5], inflate=0.1)

    # Left: Shattered Bronze & Cyan Bell-Lantern (parent: bell_lantern)
    cubes += [
        {"id": "bell_chain", "bone": "bell_lantern", "material": "gilded_bronze", "from": [11, 18, -3], "to": [12, 22, -2]},
        {"id": "bell_cap", "bone": "bell_lantern", "material": "gilded_bronze", "from": [9, 16, -5], "to": [14, 18, 0]},
        {"id": "bell_body", "bone": "bell_lantern", "material": "verdigris_copper", "from": [8.5, 10, -5.5], "to": [14.5, 16, 0.5]},
        {"id": "bell_lip", "bone": "bell_lantern", "material": "gilded_bronze", "from": [7.5, 8, -6.5], "to": [15.5, 10, 1.5]},
        {"id": "bell_core", "bone": "bell_lantern", "material": "leyline_emissive", "from": [10, 7, -4], "to": [13, 14, -1]},
    ]

    # Right: Leyline Tuning-Fork Halberd (parent: halberd)
    cubes += [
        {"id": "halberd_shaft", "bone": "halberd", "material": "obsidian_armor", "from": [-12.5, 4, -2], "to": [-10.5, 46, 0]},
        {"id": "halberd_pommel", "bone": "halberd", "material": "gilded_bronze", "from": [-13, 1, -2.5], "to": [-10, 4, 0.5]},
        {"id": "halberd_collar", "bone": "halberd", "material": "gilded_bronze", "from": [-13.5, 35, -3], "to": [-9.5, 39, 1]},
        {"id": "halberd_blade_back", "bone": "halberd", "material": "obsidian_armor", "from": [-12.5, 38, -8], "to": [-10.5, 48, -2]},
        {"id": "halberd_edge_cyan", "bone": "halberd", "material": "leyline_emissive", "from": [-12, 39, -12], "to": [-11, 47, -8]},
        {"id": "halberd_prong_tip", "bone": "halberd", "material": "gilded_bronze", "from": [-12.5, 48, -6], "to": [-10.5, 53, -1]},
    ]

    # Secondary Lower Weaver-Arms (forward/inward stance, zero upper-arm collision!)
    cubes += mirror_cube("l_arm_l", "l_arm_r", "lower_arm_left", "lower_arm_right", "obsidian_armor", [6, 29, -7], [9, 34, -2])
    cubes += mirror_cube("l_fore_l", "l_fore_r", "lower_forearm_left", "lower_forearm_right", "gilded_bronze", [6.5, 25, -13], [8.5, 29, -7])
    cubes += mirror_cube("l_orb_l", "l_orb_r", "lower_forearm_left", "lower_forearm_right", "leyline_emissive", [6.5, 25.5, -15], [8.5, 27.5, -13])

    # Neck, Crowned Stag-Skull, Hinged Jaw, Antlers & Armillary Halo
    cubes += [
        {"id": "neck_col", "bone": "neck", "material": "obsidian_armor", "from": [-3, 44, -4], "to": [3, 49, 2]},
        {"id": "skull_main", "bone": "head", "material": "obsidian_armor", "from": [-4.5, 48, -8], "to": [4.5, 56, 2]},
        {"id": "stag_snout", "bone": "head", "material": "bone_ivory", "from": [-2.5, 50, -14], "to": [2.5, 54, -8]},
        {"id": "brow_crown", "bone": "head", "material": "gilded_bronze", "from": [-5, 54, -9], "to": [5, 57, -2], "inflate": 0.1},
        {"id": "jaw_lower", "bone": "jaw", "material": "bone_ivory", "from": [-2.5, 48, -13], "to": [2.5, 50, -4]},
    ]
    cubes += mirror_cube("eye_l", "eye_r", "head", "head", "leyline_emissive", [2.5, 51.5, -8.5], [4.6, 53.5, -5.5])
    cubes += mirror_cube("antler_stem_l", "antler_stem_r", "head", "head", "gilded_bronze", [4, 55, -4], [7, 61, -1])
    cubes += mirror_cube("antler_fwd_l", "antler_fwd_r", "head", "head", "bone_ivory", [7, 59, -7], [11, 65, -3])
    cubes += mirror_cube("antler_top_l", "antler_top_r", "head", "head", "gilded_bronze", [6, 61, -1], [10, 68, 2])

    # Armillary Halo Ring (behind head, rotates on Z!)
    cubes += [
        {"id": "halo_top", "bone": "halo_ring", "material": "gilded_bronze", "from": [-6, 64, 3], "to": [6, 66, 5]},
        {"id": "halo_bottom", "bone": "halo_ring", "material": "gilded_bronze", "from": [-6, 50, 3], "to": [6, 52, 5]},
        {"id": "halo_left", "bone": "halo_ring", "material": "verdigris_copper", "from": [6, 52, 3], "to": [8, 64, 5]},
        {"id": "halo_right", "bone": "halo_ring", "material": "verdigris_copper", "from": [-8, 52, 3], "to": [-6, 64, 5]},
        {"id": "halo_node_n", "bone": "halo_ring", "material": "leyline_emissive", "from": [-1.5, 65, 2.5], "to": [1.5, 68, 5.5]},
        {"id": "halo_node_e", "bone": "halo_ring", "material": "amber_emissive", "from": [7, 56.5, 2.5], "to": [10, 59.5, 5.5]},
        {"id": "halo_node_w", "bone": "halo_ring", "material": "amber_emissive", "from": [-10, 56.5, 2.5], "to": [-7, 59.5, 5.5]},
    ]

    materials = [
        {"id": "obsidian_armor", "resolution": [128, 128], "render_mode": "default"},
        {"id": "gilded_bronze", "resolution": [128, 128], "render_mode": "default"},
        {"id": "verdigris_copper", "resolution": [128, 128], "render_mode": "default"},
        {"id": "royal_vestment", "resolution": [128, 128], "render_mode": "default"},
        {"id": "bone_ivory", "resolution": [128, 128], "render_mode": "default"},
        {"id": "leyline_emissive", "resolution": [128, 128], "render_mode": "emissive"},
        {"id": "amber_emissive", "resolution": [128, 128], "render_mode": "emissive"},
    ]

    return {
        "schema_version": "1.0",
        "project": "Vaelkhor, the Leyline Sovereign",
        "archetype": "towering_seraphic_clockwork_stag_boss",
        "target_format": "bedrock",
        "pixels_per_block": 16,
        "ground_y": 0,
        "floating_entity": False,
        "hitbox_blocks": {"width": 2.2, "height": 4.25, "length": 2.0},
        "materials": materials,
        "bones": bones,
        "cubes": cubes,
    }


def draw_creature_blueprint_sheet(anatomy):
    bones = anatomy["bones"]
    cubes = anatomy["cubes"]

    mat_colors = {
        "obsidian_armor": "#252b3b",
        "gilded_bronze": "#d8a83e",
        "verdigris_copper": "#389186",
        "royal_vestment": "#3f2b66",
        "bone_ivory": "#d8d1be",
        "leyline_emissive": "#38ecff",
        "amber_emissive": "#ffab2e",
    }

    fig, axes = plt.subplots(2, 2, figsize=(16, 13), facecolor="#111620")
    fig.suptitle(
        "VAELKHOR, THE LEYLINE SOVEREIGN — ANATOMICAL BLUEPRINT SHEET (16px = 1 Block, Ground Y=0, Forward -Z)",
        color="#e7f3ff", fontsize=14, fontweight="bold", y=0.97,
    )

    views = [
        (axes[0, 0], "FRONT VIEW (X vs Y, facing viewer)", 0, 1, (-32, 32), (-2, 72)),
        (axes[0, 1], "SIDE VIEW (Z vs Y, Forward = -Z Left)", 2, 1, (-22, 36), (-2, 72)),
        (axes[1, 0], "TOP VIEW (X vs Z, Forward = -Z Bottom)", 0, 2, (-32, 32), (-22, 36)),
    ]

    for ax, title, i0, i1, xlim, ylim in views:
        ax.set_facecolor("#181f2c")
        ax.set_title(title, color="#8de4ff", fontsize=11, pad=8)
        ax.set_xlim(*xlim)
        ax.set_ylim(*ylim)
        ax.set_aspect("equal")
        ax.grid(True, which="both", color="#273247", linewidth=0.6)
        ax.tick_params(colors="#8ba0bd", labelsize=8)
        if i1 == 1:
            ax.axhline(0, color="#67f28a", linewidth=1.6, linestyle="--", label="Ground Y=0")
        ax.axvline(0, color="#ff7272", linewidth=0.8, linestyle=":")

        for c in cubes:
            lo, hi = c["from"], c["to"]
            w = hi[i0] - lo[i0]
            h = hi[i1] - lo[i1]
            col = mat_colors.get(c["material"], "#999999")
            rect = patches.Rectangle((lo[i0], lo[i1]), w, h, facecolor=col, edgecolor="#0d1017", linewidth=0.8, alpha=0.85)
            ax.add_patch(rect)

        for b in bones:
            piv = b["pivot"]
            ax.plot(piv[i0], piv[i1], marker="+", markersize=7, color="#ff4757", markeredgewidth=1.5)

    # Panel 4: Bone Hierarchy Tree Summary
    ax_tree = axes[1, 1]
    ax_tree.set_facecolor("#151a24")
    ax_tree.axis("off")
    ax_tree.set_title("RIG HIERARCHY & JOINT PIVOTS (31 BONES)", color="#ffd466", fontsize=11, pad=8)

    lines = []
    children_map = {}
    for b in bones:
        children_map.setdefault(b["parent"], []).append(b)

    def walk(pid, depth=0):
        for b in children_map.get(pid, []):
            indent = "  " * depth
            p = b["pivot"]
            lines.append(f"{indent}- {b['id']}  [{p[0]:g}, {p[1]:g}, {p[2]:g}]")
            walk(b["id"], depth + 1)

    walk(None)
    ax_tree.text(
        0.03, 0.97, "\n".join(lines),
        transform=ax_tree.transAxes, va="top", ha="left",
        fontfamily="monospace", fontsize=8.0, color="#d7e4f2",
    )

    plt.tight_layout(rect=[0, 0.02, 1, 0.94])
    fig.savefig(OUT_DIR / "creature_blueprint_sheet.png", dpi=140, facecolor=fig.get_facecolor())
    plt.close(fig)


def build_kinematics(anatomy):
    rig_bones = [b["id"] for b in anatomy["bones"]]
    clips = [
        {
            "id": "animation.vaelkhor.idle",
            "kind": "idle",
            "loop": "loop",
            "duration": 2.4,
            "notes": "Regal breathing core pulse, astrolabe halo rotation, 4-arm counterweight float, astrolabe wing flutter.",
            "beats": {"breath_crest": 1.2},
        },
        {
            "id": "animation.vaelkhor.stride",
            "kind": "locomotion",
            "loop": "loop",
            "duration": 1.2,
            "notes": "Digitigrade stalking stride with 2f pelvis bob, knee shin bend, contralateral halberd/lantern counter-swing, head target lock.",
            "phase_offsets": {"thigh_left": 0.0, "thigh_right": 0.5, "upper_arm_left": 0.0, "upper_arm_right": 0.5},
        },
        {
            "id": "animation.vaelkhor.halberd_cleave",
            "kind": "attack",
            "loop": "once",
            "duration": 1.1,
            "notes": "Forward (-Z) dual-foot lunge cleave: 0.0-0.42s anticipation coil, 0.42-0.55s whip downswing, 0.55-0.72s grounded impact hold, 0.72-1.10s flourish recovery.",
            "beats": {"anticipation": 0.42, "strike": 0.55, "impact": 0.72, "recovery": 1.10},
        },
        {
            "id": "animation.vaelkhor.bell_toll_conduit",
            "kind": "attack",
            "loop": "once",
            "duration": 1.6,
            "notes": "Co-Op mechanic bell chime: raises left Bell-Lantern overhead & strikes it with right halberd butt while lower weaver-arms flare twin cyan conduit tethers and wings open wide.",
            "beats": {"anticipation": 0.55, "strike": 0.80, "impact": 1.10, "recovery": 1.60},
        },
        {
            "id": "animation.vaelkhor.leyline_cataclysm",
            "kind": "attack",
            "loop": "once",
            "duration": 1.8,
            "notes": "Phase 2 airborne levitation telegraph: hovers up +10px, opens lower jaw & ribs wide, spins halo, then plunges down at t=1.05s driving halberd + lantern into floor shockwave.",
            "beats": {"anticipation": 0.75, "strike": 1.05, "impact": 1.30, "recovery": 1.80},
        },
    ]
    return {"schema_version": "1.0", "rig_bones": rig_bones, "clips": clips}


def draw_animation_blueprint_sheet(kinematics):
    fig, axes = plt.subplots(3, 1, figsize=(14, 9.5), facecolor="#111620")
    fig.suptitle(
        "VAELKHOR — KINEMATIC DOPE SHEET & COMBAT TELEGRAPH ENVELOPES (20 TPS / 16px Scale)",
        color="#e7f3ff", fontsize=13, fontweight="bold", y=0.97,
    )

    # Plot 1: Stride Gait Curves
    ax0 = axes[0]
    ax0.set_facecolor("#181f2c")
    t_s = np.linspace(0, 1.2, 121)
    ph = 2 * math.pi * (t_s / 1.2)
    ax0.plot(t_s, 36 * np.sin(ph), label="thigh_left (deg)", color="#4ef2ff", linewidth=2)
    ax0.plot(t_s, -36 * np.sin(ph), label="thigh_right (deg)", color="#ffab2e", linewidth=2)
    ax0.plot(t_s, 28 * np.sin(ph - 1.1) - 18, label="shin_left lagged knee (deg)", color="#79f2a8", linewidth=1.6, linestyle="--")
    ax0.plot(t_s, 14 * np.cos(2 * ph), label="pelvis bob*10", color="#d38cff", linewidth=1.4)
    ax0.set_title("STRIDE GAIT PHASE GRAPH (1.20s Loop, Diagonal Arm Counter-Swing)", color="#8de4ff", fontsize=10)
    ax0.set_ylabel("Angle / Scale", color="#a9bdd4")
    ax0.grid(True, color="#263145")
    ax0.legend(loc="upper right", fontsize=8, facecolor="#111620", edgecolor="#384966", labelcolor="#dce9f7")
    ax0.tick_params(colors="#8ba0bd")

    # Plot 2: Attack Beat Envelopes (Cleave, Bell Toll, Cataclysm)
    ax1 = axes[1]
    ax1.set_facecolor("#181f2c")
    attacks = [
        ("halberd_cleave (1.10s)", 0.42, 0.55, 0.72, 1.10, "#ff5e6c"),
        ("bell_toll_conduit (1.60s)", 0.55, 0.80, 1.10, 1.60, "#4ef2ff"),
        ("leyline_cataclysm (1.80s)", 0.75, 1.05, 1.30, 1.80, "#ffab2e"),
    ]
    for row, (label, ant, strk, imp, dur, col) in enumerate(attacks):
        y = 2 - row
        ax1.barh(y, ant, left=0, height=0.52, color="#394c6e", edgecolor=col, linewidth=1.2)
        ax1.barh(y, strk - ant, left=ant, height=0.52, color=col, alpha=0.85)
        ax1.barh(y, imp - strk, left=strk, height=0.52, color="#ffd166", alpha=0.9)
        ax1.barh(y, dur - imp, left=imp, height=0.52, color="#2a3a52", edgecolor=col, linewidth=1.0)
        ax1.text(0.02, y, f"{label}  | Tell: 0->{ant}s  Strike->{strk}s  Hold->{imp}s  Recover->{dur}s",
                 va="center", ha="left", color="#ffffff", fontsize=8.5, fontweight="bold")
    ax1.set_yticks([])
    ax1.set_xlim(0, 1.95)
    ax1.set_xlabel("Seconds (20 TPS)", color="#a9bdd4")
    ax1.set_title("FIGHT TELEGRAPH & HIT-STOP TIMING BARS", color="#ffd466", fontsize=10)
    ax1.grid(True, axis="x", color="#263145")
    ax1.tick_params(colors="#8ba0bd")

    # Plot 3: Chain Overlap Delay Schematic
    ax2 = axes[2]
    ax2.set_facecolor("#181f2c")
    t_w = np.linspace(0, 2.4, 241)
    w = 2 * math.pi * (t_w / 2.4)
    ax2.plot(t_w, 8 * np.sin(w), label="tail_0 (0 deg lag)", color="#f0cf65", linewidth=1.8)
    ax2.plot(t_w, 14 * np.sin(w - 0.75), label="tail_1 (+43 deg lag)", color="#4ef2ff", linewidth=1.8)
    ax2.plot(t_w, 22 * np.sin(w - 1.50), label="tail_2 pendulum (+86 deg lag)", color="#ff7299", linewidth=1.8)
    ax2.plot(t_w, 12 * np.sin(w - 0.9), label="wing_ring overlap", color="#79f2a8", linewidth=1.4, linestyle=":")
    ax2.set_title("SECONDARY WAVE OVERLAP PROPAGATION (2.40s Idle Loop)", color="#8de4ff", fontsize=10)
    ax2.set_xlabel("Seconds", color="#a9bdd4")
    ax2.set_ylabel("Degrees", color="#a9bdd4")
    ax2.grid(True, color="#263145")
    ax2.legend(loc="upper right", fontsize=8, facecolor="#111620", edgecolor="#384966", labelcolor="#dce9f7")
    ax2.tick_params(colors="#8ba0bd")

    plt.tight_layout(rect=[0, 0.02, 1, 0.94])
    fig.savefig(OUT_DIR / "animation_blueprint_sheet.png", dpi=140, facecolor=fig.get_facecolor())
    plt.close(fig)


def main():
    anatomy = build_anatomy()
    (OUT_DIR / "anatomy_blueprint.json").write_text(json.dumps(anatomy, indent=2) + "\n", encoding="utf-8")
    draw_creature_blueprint_sheet(anatomy)
    kinematics = build_kinematics(anatomy)
    (OUT_DIR / "kinematic_blueprint.json").write_text(json.dumps(kinematics, indent=2) + "\n", encoding="utf-8")
    draw_animation_blueprint_sheet(kinematics)
    print(f"Wrote blueprints: {len(anatomy['bones'])} bones, {len(anatomy['cubes'])} cubes, {len(kinematics['clips'])} clips")


if __name__ == "__main__":
    main()
