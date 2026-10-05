#!/usr/bin/env python3
"""Source projection preflight; never native rendering or screenshot acceptance.

Compile/run tools/ExportMastersFirstPerson.java against the source being reviewed,
then pass its JSON with --poses and the official client jar with --minecraft-client.
Optional --reference-poses + --native-root calibrate the unmodified reference source
against its native sidecars/PNGs and actual logged phase ages. --continuous adds a
quarter-tick sweep. Requires numpy and Pillow; writes only the requested JSON report.
"""
import argparse
import hashlib
import io
import json
import math
import re
from pathlib import Path
from zipfile import ZipFile

import numpy as np
from PIL import Image, ImageDraw, ImageFont


def translation(x, y, z):
    matrix = np.eye(4)
    matrix[:3, 3] = (x, y, z)
    return matrix


def rotation(axis, degrees):
    angle = math.radians(degrees)
    c, s = math.cos(angle), math.sin(angle)
    matrix = np.eye(4)
    i, j = {"x": (1, 2), "y": (2, 0), "z": (0, 1)}[axis]
    matrix[i, i] = matrix[j, j] = c
    matrix[i, j], matrix[j, i] = -s, s
    return matrix


def projection(width=1280, height=720):
    # Pinned 26.3 hand camera, reversed depth. Native metadata validates all 16 elements.
    matrix = np.zeros((4, 4))
    matrix[0, 0] = 1 / math.tan(math.radians(35)) / (width / height)
    matrix[1, 1] = 1 / math.tan(math.radians(35))
    matrix[2, 2], matrix[2, 3] = .05 / (2048 - .05), .05 * 2048 / (2048 - .05)
    matrix[3, 2] = -1
    return matrix


def hand_matrix(context):
    hand, grip = context["view"]["transform"], context["view"]["grip"]
    return (translation(hand["x"], hand["y"], hand["z"])
            @ translation(grip["x"], grip["y"], grip["z"])
            @ rotation("y", hand["yaw"]) @ rotation("x", hand["pitch"]) @ rotation("z", hand["roll"]))


def item_matrix(left=False):
    # Exact official handheld display. ItemTransform.apply mirrors the authored left context.
    return (translation((-1.13 if left else 1.13) / 16, 3.2 / 16, 1.13 / 16)
            @ rotation("y", -90) @ rotation("z", 25) @ np.diag([.68, .68, .68, 1])
            @ translation(-.5, -.5, -.5))


def mesh(texture):
    cyan, blade, opaque = [], [], []
    for y in range(16):
        for x in range(16):
            r, g, b, alpha = map(int, texture[y, x])
            if not alpha:
                continue
            vertices = np.array([[u / 16, 1 - v / 16, z, 1] for z in [7.5 / 16, 8.5 / 16]
                                 for v in [y, y + 1] for u in [x, x + 1]])
            faces = [vertices[indices] for indices in [[0, 1, 3, 2], [4, 5, 7, 6], [0, 1, 5, 4],
                                                       [0, 2, 6, 4], [1, 3, 7, 5], [2, 3, 7, 6]]]
            opaque.extend(faces)
            if g > 75 and r < g * .7 and b > g * .65 and b < g * 1.3:
                cyan.extend(faces[:2])
            if y < 8 and x >= 6:
                blade.extend(faces[:2])
    return cyan, blade, opaque


def clip(vertices, distance):
    result = []
    if not len(vertices):
        return result
    previous, previous_distance = vertices[-1], distance(vertices[-1])
    for current in vertices:
        current_distance = distance(current)
        if (current_distance >= 0) != (previous_distance >= 0):
            result.append(previous + (current - previous) * (previous_distance / (previous_distance - current_distance)))
        if current_distance >= 0:
            result.append(current)
        previous, previous_distance = current, current_distance
    return result


def mask(matrix, quads, width=1280, height=720, hud_top=642):
    image = Image.new("1", (width, height))
    draw = ImageDraw.Draw(image)
    for quad in quads:
        vertices = quad @ matrix.T
        # Clip, rather than discard polygons crossing the camera/viewport planes.
        for distance in [lambda v: v[3] - .05, lambda v: v[0] + v[3], lambda v: v[3] - v[0],
                         lambda v: v[1] + v[3], lambda v: v[3] - v[1]]:
            vertices = clip(vertices, distance)
            if len(vertices) < 3:
                break
        if len(vertices) < 3:
            continue
        vertices = np.array(vertices)
        screen = np.array([(vertices[:, 0] / vertices[:, 3] + 1) * width / 2,
                           (1 - vertices[:, 1] / vertices[:, 3]) * height / 2]).T
        draw.polygon([tuple(point) for point in screen], fill=1)
    pixels = np.array(image)
    pixels[hud_top:] = False
    return pixels


def measure(row, context, meshes):
    matrix = projection() @ hand_matrix(context) @ item_matrix(context["left"])
    cyan, blade = (mask(matrix, quads) for quads in meshes[:2])
    opaque = mask(matrix, meshes[2])
    camera = hand_matrix(context) @ item_matrix(context["left"])
    nearest_depth = float(-(np.concatenate(meshes[2]) @ camera.T)[:, 2].max())
    y, x = np.where(cyan)
    span = float(max(x.max() - x.min() + 1, y.max() - y.min() + 1) / 2) if len(x) else 0
    # Deliberately a geometry margin, not the native cyan-component policy: lighting, occlusion
    # and aura can fragment these footprints. An upper-blade footprint excludes guard-only poses.
    return {"cyanFaceFootprint": int(cyan.sum()), "upperBladeFaceFootprint": int(blade.sum()),
            "spanUpperBoundGui": span, "geometryMarginPass": bool(cyan.sum() >= 512 and blade.sum() >= 512 and span >= 32),
            "nearestOpaqueDepth": nearest_depth, "nearPlaneMargin": nearest_depth - .05,
            "reticleCore32px": int(opaque[344:376, 624:656].sum()),
            "aimCorridor64px": int(opaque[328:392, 608:672].sum())}


def read_samples(path):
    data = json.loads(path.read_text())
    if data["kind"] != "source_projection_samples_not_native_frames":
        raise ValueError("Expected the source pose exporter format")
    return {(row["move"], row["age"]): row for row in data["samples"]}


def calibrate(root, reference, meshes):
    log = (root / "masters-native.log").read_text()
    ages = {name + "_" + phase: float(age) for name, phase, age in
            re.findall(r"MASTERS_CAPTURE_PHASE name=(\S+) phase=(\S+) age=([\d.]+)", log)}
    results = []
    screenshots = root / "build/run/clientGameTest/screenshots"
    for path in sorted(screenshots.glob("masters_art_*_first_*.json")):
        data = json.loads(path.read_text())
        if data["name"] not in ages:
            continue  # Settled is not invented as a phase sample.
        move = int(re.fullmatch(r"masters_art_(\d+)_first_\w+", data["name"])[1])
        row = reference[move, ages[data["name"]]]
        observed = np.array(data["handProjection"]).reshape(4, 4).T
        context = min(row["views"], key=lambda view: np.max(np.abs(projection(data["width"], data["height"])
                                                                   @ hand_matrix(view) - observed)))
        delta = float(np.max(np.abs(projection(data["width"], data["height"]) @ hand_matrix(context) - observed)))
        hud_top = math.floor(data["hud"]["top"] * data["height"] / data["hud"]["guiHeight"])
        predicted = mask(observed @ item_matrix(context["left"]), meshes[2], data["width"], data["height"], hud_top)
        actual = np.array(Image.open(path.with_suffix(".png")).convert("RGB"), dtype=float)
        r, g, b = actual[:, :, 0], actual[:, :, 1], actual[:, :, 2]
        cyan = (g > 75) & (r < g * .7) & (b > g * .65) & (b < g * 1.3)
        cyan[hud_top:] = False
        outside = int((cyan & ~predicted).sum())
        if delta > 1e-5 or outside or int(cyan.sum()) != data["pixels"]["cyan"]:
            raise AssertionError(f"Native calibration mismatch: {path.name}, matrix={delta}, outside={outside}")
        results.append({"name": data["name"], "actualLoggedAge": row["age"], "matrixMaxError": delta,
                        "left": context["left"], "yawDelta": context["yawDelta"], "pitchDelta": context["pitchDelta"],
                        "actualCyan": int(cyan.sum()), "actualCyanOutsideOpaqueProjection": outside,
                        "pngSha256": hashlib.sha256(path.with_suffix(".png").read_bytes()).hexdigest()})
    if not results:
        raise AssertionError("No genuine native phase metadata was calibrated")
    return results


def contact_sheets(directory, samples, meshes):
    """Flat geometric diagrams from source only. Never load or edit native capture pixels."""
    directory.mkdir(parents=True, exist_ok=True)
    font = ImageFont.load_default(size=15)
    small = ImageFont.load_default(size=12)
    title = ImageFont.load_default(size=23)
    paths = []
    for page in range(3):
        sheet = Image.new("RGB", (1120, 1180), "#f4f5f7")
        draw = ImageDraw.Draw(sheet)
        draw.text((22, 14), "OFFLINE SOURCE GEOMETRY / NOT A NATIVE CAPTURE", font=title, fill="#8d2727")
        draw.text((22, 47), "Right hand / FOV 70 / pitch offset -12 degrees / actual native after-images still required", font=font, fill="#303843")
        for row_index, move in enumerate(range(page * 5, page * 5 + 5)):
            info = samples[move, 0]
            windup, recovery = info["windup"], info["recovery"]
            top = 90 + row_index * 216
            draw.text((15, top + 8), f"{move}: {info['name']}", font=font, fill="#263642")
            for column, (phase, age) in enumerate(zip(["windup", "active", "recovery"],
                                                     [windup // 2 + .5, windup + .5, windup + recovery // 2 + .5])):
                row = samples[move, age]
                context = next(view for view in row["views"] if not view["left"] and view["pitchDelta"] == -12)
                matrix = projection() @ hand_matrix(context) @ item_matrix()
                opaque = mask(matrix, meshes[2], hud_top=720)
                cyan = mask(matrix, meshes[0], hud_top=720)
                panel = Image.new("RGB", (1280, 720), "#e4e9ed")
                pen = ImageDraw.Draw(panel)
                pen.rectangle((0, 642, 1279, 719), fill="#a8b0b9")
                pen.rectangle((608, 328, 671, 391), outline="#cf7777", width=6)
                pen.line((624, 360, 656, 360), fill="#b62b2b", width=6)
                pen.line((640, 344, 640, 376), fill="#b62b2b", width=6)
                panel.paste("#263a49", mask=Image.fromarray(opaque))
                panel.paste("#2fbcb1", mask=Image.fromarray(cyan))
                left = 198 + column * 305
                sheet.paste(panel.resize((298, 168), Image.Resampling.NEAREST), (left, top + 24))
                draw.text((left, top + 3), f"{phase} / source age {age}", font=font, fill="#263642")
                nearest = float(-(np.concatenate(meshes[2]) @ (hand_matrix(context) @ item_matrix()).T)[:, 2].max())
                draw.text((left, top + 194), f"nearest depth {nearest:.3f} / near plane .050", font=small, fill="#354452")
        path = directory / f"source-geometry-{page + 1}.png"
        sheet.save(path)
        paths.append(str(path))
    return paths


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--poses", type=Path, required=True)
    parser.add_argument("--minecraft-client", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--native-root", type=Path)
    parser.add_argument("--reference-poses", type=Path)
    parser.add_argument("--continuous", action="store_true")
    parser.add_argument("--contact-sheet-dir", type=Path)
    args = parser.parse_args()
    samples = read_samples(args.poses)
    with ZipFile(args.minecraft_client) as jar:
        raw = jar.read("assets/minecraft/textures/item/diamond_sword.png")
        texture = np.array(Image.open(io.BytesIO(raw)).convert("RGBA"))
    if texture.shape != (16, 16, 4):
        raise ValueError("Expected the pinned official 16x16 diamond sword texture")
    meshes = mesh(texture)
    report = {"kind": "source_geometry_preflight_not_native_acceptance", "posesSha256": hashlib.sha256(args.poses.read_bytes()).hexdigest(),
              "textureSha256": hashlib.sha256(raw).hexdigest(), "phases": [], "nativeCalibration": [],
              "limits": ["Geometry footprints are not shaded framebuffer pixels or native component spans.",
                         "Canonical 1280x720 scale-2 HUD, equipped hand, FOV70; no camera bob, equip, resource-pack or Iris coverage.",
                         "Actual native screenshots and their unchanged gates remain required for every changed form."]}
    if args.native_root:
        if not args.reference_poses:
            parser.error("--native-root requires matching unmodified --reference-poses")
        report["nativeCalibration"] = calibrate(args.native_root, read_samples(args.reference_poses), meshes)
    for move in range(15):
        info = samples[move, 0]
        windup, recovery = info["windup"], info["recovery"]
        ages = [windup // 2 + .5, windup + .5, windup + recovery // 2 + .5]
        for phase, age in zip(["windup", "active", "recovery"], ages):
            row = samples[move, age]
            for context in row["views"]:
                report["phases"].append({"move": move, "name": row["name"], "sourcePhase": phase, "sourceAge": age,
                                         **{key: context[key] for key in ("left", "yawDelta", "pitchDelta")}, **measure(row, context, meshes)})
    if args.continuous:
        checked, failures, diagnostics = 0, [], []
        for row in samples.values():
            for context in row["views"]:
                if context["left"]:
                    continue
                result = measure(row, context, meshes)
                checked += 1
                diagnostics.append({"move": row["move"], "sourceAge": row["age"], "windup": row["windup"],
                                    "pitchDelta": context["pitchDelta"], **result})
                if not result["geometryMarginPass"]:
                    failures.append({"move": row["move"], "sourceAge": row["age"], "pitchDelta": context["pitchDelta"], **result})
        report["continuous"] = {"sourceSamplesChecked": checked, "failures": failures, "diagnostics": diagnostics}
    if args.contact_sheet_dir:
        report["sourceGeometryContactSheets"] = contact_sheets(args.contact_sheet_dir, samples, meshes)
    failures = [row for row in report["phases"] if not row["geometryMarginPass"]]
    report["phaseFailures"] = failures
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, indent=2) + "\n")
    print(f"Source phase contexts: {len(report['phases'])}; geometry-margin failures: {len(failures)}; calibrated native frames: {len(report['nativeCalibration'])}")
    if args.continuous:
        print(f"Continuous source contexts: {report['continuous']['sourceSamplesChecked']}; geometry-margin failures: {len(report['continuous']['failures'])}")
    if failures or report.get("continuous", {}).get("failures"):
        raise SystemExit(1)


if __name__ == "__main__":
    main()
