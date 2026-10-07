#!/usr/bin/env python3
"""Reproduce the old-player-pose reference from one immutable pre-feature commit.

Example (only Java 25 and JUnit are needed; no Minecraft, Gradle or native client):
  python3 tools/freeze_mirror_riposte_baseline.py \
    --jdk /workspace/shared/toolchains/jdk-25.0.4.1+1 \
    --junit /workspace/shared/wildercord_standalone/deps/junit-platform-console-standalone-1.11.4.jar

The production source list below is materialized with `git show COMMIT:path` in a
new temporary directory. Neither the working-tree sourcepath nor cached project
classes is ever on the compiler or runtime classpath. The current test supplies
only the documented sample encoder, not the implementation under test. The commit
is deliberately not configurable. --check reproduces and compares without writes.
The original gzip bytes are transported as canonical base64 text parts, with no
whitespace, wrapping, or line terminators. Compression and plain hashes remain pinned.
"""

import argparse
import base64
import gzip
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile


COMMIT = "6d45adbad840287b46cec34d73cd700b178ea246"
TEST = "src/test/java/dev/wildercord/aura/MastersMirrorRipostePreservationTest.java"
RESOURCE = "src/test/resources/dev/wildercord/aura/mirror-riposte-legacy-poses.tsv.gz"
MANIFEST = "src/test/resources/dev/wildercord/aura/mirror-riposte-legacy-poses.json"
HELPER = "src/test/java/dev/wildercord/aura/FrozenPoseTextFixture.java"
STORAGE = RESOURCE + ".base64"
PART_BYTES = 65_536
GZIP_BYTES = 1434382
GZIP_SHA256 = "117a7b9fb58dfb75f67704dc2b15d3ce7961bac87e5900d26ece8f24deac9dce"
PLAIN_BYTES = 4820859
PLAIN_SHA256 = "aed585513da1b8118effa02a47a442d18d84a4aaab49d4ba17d1a2996ce555e6"
SOURCES = sorted(
    [f"src/main/java/dev/wildercord/aura/{name}.java" for name in (
        "MastersArtAnimation", "MastersStyleAnimation", "ArticulatedCombatPose",
        "MastersArtRules", "MastersStyleRules", "ArtRules", "AuraRules", "StringRules", "SwordString")]
    + [f"src/main/java/dev/wildercord/aura/world/{name}.java" for name in (
        "GaleRepriseRules", "MasterAnimationRules", "StoneFractureRules", "StoneMarchRules",
        "MastersRules", "MasterPursuitRules", "EmberWakeRules", "EmberKilnRules")]
    + ["src/main/java/dev/wildercord/cast/SpellDefenceRules.java",
       "src/main/java/dev/wildercord/spell/MasteryRules.java",
       "src/main/java/dev/wildercord/spell/Parry.java"]
)


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def run(command, cwd):
    result = subprocess.run(command, cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if result.returncode:
        raise SystemExit(result.stderr.decode("utf-8", errors="replace"))
    return result.stdout


def text_parts(compressed):
    encoded = base64.b64encode(compressed)
    return {f"part-{index:04d}.b64": encoded[offset:offset + PART_BYTES]
            for index, offset in enumerate(range(0, len(encoded), PART_BYTES))}


def verify_parts(directory, parts):
    # Enumerate the actual directory. Probing only the expected names misses extras.
    if not directory.is_dir() or directory.is_symlink():
        raise SystemExit("Frozen text fixture directory is missing or not canonical")
    actual = {path.name for path in directory.iterdir()}
    if actual != set(parts):
        raise SystemExit("Frozen text fixture has missing or extra parts")
    encoded = bytearray()
    for name, expected in parts.items():
        path = directory / name
        if path.is_symlink() or not path.is_file():
            raise SystemExit(f"Frozen text fixture part is not a regular file: {name}")
        with path.open("rb") as handle:
            content = handle.read(len(expected) + 1)
        if content != expected:
            raise SystemExit(f"Frozen text fixture part differs: {name}")
        encoded.extend(content)
    compressed = base64.b64decode(encoded, validate=True)
    if base64.b64encode(compressed) != encoded:
        raise SystemExit("Frozen text fixture base64 is not canonical")
    if len(compressed) != GZIP_BYTES or sha256(compressed) != GZIP_SHA256:
        raise SystemExit("Frozen text fixture changed the original gzip bytes")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jdk", type=Path, required=True, help="Java 25 JDK root")
    parser.add_argument("--junit", type=Path, required=True, help="JUnit platform standalone JAR")
    parser.add_argument("--check", action="store_true", help="Verify exact reproducibility without writing")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    jdk, junit = args.jdk.resolve(), args.junit.resolve()
    if not junit.is_file():
        parser.error("The specified JUnit JAR does not exist")
    source_receipts = []
    encoder = (root / TEST).read_bytes()
    helper = (root / HELPER).read_bytes()
    with tempfile.TemporaryDirectory(prefix="mirror-riposte-frozen-") as directory:
        work = Path(directory)
        sources, classes = work / "frozen-sources", work / "classes"
        classes.mkdir()
        files = []
        for relative in SOURCES:
            # The immutable Git object is the ONLY production source of expected values.
            content = run(["git", "show", f"{COMMIT}:{relative}"], root)
            target = sources / relative.removeprefix("src/main/java/")
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(content)
            files.append(str(target))
            source_receipts.append({"path": relative, "sha256": sha256(content), "bytes": len(content)})
        encoder_path = work / "MastersMirrorRipostePreservationTest.java"
        encoder_path.write_bytes(encoder)
        helper_path = work / "FrozenPoseTextFixture.java"
        helper_path.write_bytes(helper)
        run([str(jdk / "bin/javac"), "--release", "25", "-encoding", "UTF-8",
             "-classpath", str(junit), "-sourcepath", str(sources), "-d", str(classes),
             *files, str(encoder_path), str(helper_path)], work)
        plain = run([str(jdk / "bin/java"), "-Xmx512m", "-cp", os.pathsep.join((str(classes), str(junit))),
                     "dev.wildercord.aura.MastersMirrorRipostePreservationTest", "--export-frozen"], work)
        # No filename or clock fields: repeat invocations produce exactly the same fixture.
        compressed = gzip.compress(plain, compresslevel=9, mtime=0)
    # These receipts predate the storage migration. Never silently redefine goldens.
    if len(plain) != PLAIN_BYTES or sha256(plain) != PLAIN_SHA256:
        raise SystemExit("Immutable exporter differs from the pinned original plain fixture")
    if len(compressed) != GZIP_BYTES or sha256(compressed) != GZIP_SHA256:
        raise SystemExit("Compressor differs from the pinned original gzip bytes; do not update the golden")
    parts = text_parts(compressed)
    rows = plain.decode("utf-8").splitlines()[2:]
    metadata = {
        "schema": "mirror-riposte-preservation-v1",
        "baselineCommit": COMMIT,
        "baselineTree": run(["git", "rev-parse", f"{COMMIT}^{{tree}}"], root).decode().strip(),
        "provenance": "Every production source was read with git show <baselineCommit>:<path>, then compiled in isolation. No edited production source or cached project class was used.",
        "scope": "Old player IDs 0 through 21 inclusive; Classic body/view and articulated world/view; right and left hands. Unsupported articulated IDs preserve neutral fallback.",
        "encoding": "Each palette SHA-256 covers DataOutputStream big-endian IEEE-754 floatToIntBits values, booleans, ints and modified UTF-8 strings. Matrices are all 16 column-major float entries. Signed zero is preserved; NaNs use Java canonical float bits.",
        "palettes": {
            "classicBody": "Support and neutral identity; immutable canonical timing; weight, lower, forward, all six body joints; body/shoulder pivots for each hand; blade tilt and bounded head.",
            "classicView": "Raw hand plus weighted transform/grip for (inverseArmHeight,yawDelta,pitchDelta) inputs (0,0,0),(.35,27,-17),(1,-71,62),(-.5,0,0),(1.5,0,0).",
            "articulatedWorld": "Support and neutral identity; weight/phase; all joint names, parents, bind/local transforms and world matrices; both explicit socket matrices.",
            "articulatedView": "Weight/phase/origin; every joint local transform and world matrix; cameraPoint at (0,0,0) and (1,2,-3); both explicit socket matrices."
        },
        "sampling": {
            "canonical": "Timings read from immutable MastersArtRules/MastersStyleRules. Every 1/8 tick from zero through expiry inclusive, plus every 1/16 of the full duration.",
            "alternateTimings": [[1, 1], [20, 30], [60, 120]],
            "alternateAges": "Every 1/16 of the full duration plus the same boundary and invalid-age sets.",
            "boundaries": "0, .65*windup, windup, windup+1, windup+min(4,.25*recovery), expiry. Crackle adds both gap beats and settle; Echo adds follow/return/second release/settle; Skyfall adds hold/answer/follow.",
            "boundaryNeighborhood": "Exact edge, Math.nextDown(edge), Math.nextUp(edge), edge-.001F, edge+.001F. TreeSet<Float> removes duplicates without merging signed zero.",
            "invalidAndExpiryAges": "-1, -.001F, -Float.MIN_VALUE, -0.0F, 0, Float.MIN_VALUE, NaN, -Infinity, +Infinity, expiry, expiry+1, 200.",
            "invalidTimings": [[0, 12], [-1, 12], [61, 12], [4, 0], [4, -1], [4, 121], [2147483647, 12], [4, 2147483647], [-2147483648, -2147483648]],
            "invalidTimingAges": ["0", "1", "NaN"],
            "rows": len(rows),
            "rowsPerClip": {str(move): sum(line.startswith(f"{move}\t") for line in rows) for move in range(22)}
        },
        "fixture": {"path": STORAGE, "hashScope": "Decoded original gzip bytes", "sha256": sha256(compressed), "bytes": len(compressed),
                    "uncompressedSha256": sha256(plain), "uncompressedBytes": len(plain)},
        "storage": {
            "format": "canonical-base64-parts-v1", "decodedFixturePath": RESOURCE,
            "partBytes": PART_BYTES, "encodedBytes": sum(map(len, parts.values())),
            "encodedSha256": sha256(b"".join(parts.values())),
            "parts": [{"path": name, "bytes": len(data), "sha256": sha256(data)}
                      for name, data in parts.items()],
            "description": "Concatenate the ordered parts, then decode canonical RFC 4648 base64. ASCII UTF-8, no whitespace or line terminators. Every non-final part is exactly 65536 bytes. The decoded gzip bytes are identical to the original binary fixture."
        },
        "loader": {"path": HELPER, "sha256": sha256(helper)},
        "encoder": {"path": TEST, "sha256": sha256(encoder)},
        "productionSources": source_receipts,
        "javaRuntime": subprocess.run([str(jdk / "bin/java"), "-version"], check=True,
                                       stdout=subprocess.PIPE, stderr=subprocess.STDOUT).stdout.decode().strip(),
        "verificationLimits": "Pure standalone geometry/pose sampling only. No Minecraft renderer, native client, Gradle build or pixel capture is implied. Exact-bit references were generated with the recorded Java runtime."
    }
    manifest = (json.dumps(metadata, indent=2, sort_keys=True) + "\n").encode()
    if args.check:
        if (root / RESOURCE).exists():
            raise SystemExit("Obsolete binary fixture must not coexist with canonical text parts")
        verify_parts(root / STORAGE, parts)
        previous = json.loads((root / MANIFEST).read_text())
        for field in ("baselineCommit", "baselineTree", "fixture", "storage", "productionSources", "encoder", "loader", "sampling"):
            if previous[field] != metadata[field]:
                raise SystemExit(f"Frozen manifest differs in {field}")
    else:
        directory = root / STORAGE
        # Existing resources must already match, including the closed inventory.
        # Never erase unexpected files or repair silently changed golden parts.
        if directory.exists():
            verify_parts(directory, parts)
        else:
            directory.mkdir(parents=True)
            for name, data in parts.items():
                (directory / name).write_bytes(data)
        if (root / RESOURCE).exists():
            if (root / RESOURCE).read_bytes() != compressed:
                raise SystemExit("Original binary fixture differs from immutable regeneration")
            (root / RESOURCE).unlink()
        (root / MANIFEST).write_bytes(manifest)
    print(json.dumps({"mode": "verified" if args.check else "generated", "rows": len(rows),
                      "resource": STORAGE, "sha256": sha256(compressed), "uncompressedSha256": sha256(plain)}, indent=2))


if __name__ == "__main__":
    main()
