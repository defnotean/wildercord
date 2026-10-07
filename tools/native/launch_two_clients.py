"""Bounded two-JVM cast-receipt/Moon gates; no arbitrary commands or suite selectors.

First export with -I tools/native/export_two_client_launch.init.gradle
exportTwoClientLaunch -PtwoClientSuite=cast-receipt. Then launch that exact export
with --suite cast-receipt --profile aura --output build/native/receipts.
The historical proposal's --require-* launcher flags are no longer supported;
its optional bare Gradle descriptor selectors remain available separately.
"""
from __future__ import annotations

import argparse
from contextlib import contextmanager, nullcontext
import hashlib
import json
import math
import os
from pathlib import Path
import re
import shutil
import signal
import subprocess
import sys
import time
import types
import uuid

ROOT = Path(__file__).resolve().parents[2]
SUITE = "cast-receipt"
CONTRACT = "src/gametest/resources/cast-receipt-native-contract.json"
ENTRYPOINT = "dev.wildercord.aura.world.ConnectedCastReceiptTest"
MAIN = "net.fabricmc.devlaunchinjector.Main"
PROFILES = {"host": ("WCAuraHost", "de9c2fe8-3188-30b6-a721-9ed8940474eb"),
            "peer": ("WCAuraPeer", "315e5fc2-3894-3190-83b4-ab47825b72ac")}
TERMINALS = {"host-cast-receipt-passed.properties": "host",
             "peer-cast-receipt-passed.properties": "peer", "peer-disconnected.properties": "peer"}
IDENTITY = ("nonce", "suite", "sourceHead", "checkoutSha", "prHeadSha", "descriptorSha256", "hostUuid", "peerUuid", "runIdentity")
MAX_TIMEOUT = 900
# Keep the original 46 identities in place; eight counter visual cases are additive.
CASES = (
    "BREAK_CAST_HEALTH",
    "BREAK_CAST_FULL_ABSORPTION",
    "BREAK_CAST_MANA_SKIN",
    "BREAK_CAST_REVERSAL",
    "BREAK_CAST_TOTEM",
    "BREAK_CAST_GUARD",
    "BREAK_CAST_STEP",
    "BREAK_CAST_WARD",
    "BREAK_CAST_RESISTANCE",
    "BREAK_CAST_REJECTED",
    "BREAK_CAST_REPLACED",
    "BREAK_CAST_EQUAL_TOKEN",
    "BREAK_CAST_NEW_CHARGE",
    "BREAK_CAST_IDLE",
    "BREAK_CAST_WINDUP_REPLACEMENT",
    "DRIVING_CUT_HEALTH",
    "DRIVING_CUT_FULL_ABSORPTION",
    "DRIVING_CUT_MANA_SKIN",
    "DRIVING_CUT_REVERSAL",
    "DRIVING_CUT_TOTEM",
    "DRIVING_CUT_GUARD",
    "DRIVING_CUT_STEP",
    "DRIVING_CUT_WARD",
    "DRIVING_CUT_RESISTANCE",
    "DRIVING_CUT_REJECTED",
    "DRIVING_CUT_REPLACED",
    "DRIVING_CUT_EQUAL_TOKEN",
    "DRIVING_CUT_NEW_CHARGE",
    "DRIVING_CUT_IDLE",
    "DRIVING_CUT_WINDUP_REPLACEMENT",
    "SHARED_BREAK_CAST_TO_DRIVING_CUT",
    "SHARED_DRIVING_CUT_TO_BREAK_CAST",
    "IDLE_SEAL_RECOVERY",
    "CHARGED_SEAL_RECOVERY",
    "NONPLAYER",
    "COUNTER_QUIETUS_PAID_BOLT",
    "COUNTER_QUIETUS_THIRTEEN_EXISTING",
    "COUNTER_REFLECTED_RESPAWN_NULLCATCH",
    "SPECTATOR_BOLT_VENOM",
    "SPECTATOR_SPARK_VENOM",
    "SPECTATOR_RAY_VENOM",
    "SPECTATOR_TOUCH_VENOM",
    "LIFE_SELF_HEAL_FIRST_PERSON",
    "LIFE_SELF_HEAL_THIRD_PERSON",
    "LIFE_SELF_HEAL_CAMERA_TRANSITIONS",
    "LIFE_SELF_SECOND_WIND_REDUCED_FLASH",
    "COUNTER_UNMOVED_CLASSIC_RIGHT",
    "COUNTER_UNMOVED_CLASSIC_LEFT",
    "COUNTER_UNMOVED_ARTICULATED_RIGHT",
    "COUNTER_UNMOVED_ARTICULATED_LEFT",
    "COUNTER_NULL_PARRY_CLASSIC_RIGHT",
    "COUNTER_NULL_PARRY_CLASSIC_LEFT",
    "COUNTER_NULL_PARRY_ARTICULATED_RIGHT",
    "COUNTER_NULL_PARRY_ARTICULATED_LEFT",
)
LIFE_CASES = CASES[42:46]
COUNTER_CASES = CASES[46:]
COUNTER_PHASES = ("WINDUP", "ACTIVE", "FOLLOW", "RECOVERY")
COUNTER_MIXINS = ("counter-peer-gametest.mixins.json",)
COUNTER_PHASE_BASIS = "actual_post_hitstop_source_palette"
COUNTER_SOURCE_FIELDS = ("Entity", "Move", "StartTick", "Windup", "Recovery", "Yaw", "Pitch")
COUNTER_ADMISSION_FIELDS = tuple("admission" + key for key in (
    "SchemaVersion", "Generation", "PeerArmedSha256", "Art", "SentMarks", "RequestMarks", "PerformMarks",
    "ServerBodyUuid", "ServerBodyEntity", "ServerLevel", "ServerConnectionIdentity",
    "ClientBodyUuid", "ClientBodyEntity", "ClientLevel", "ClientConnectionIdentity",
    "Busy", "ExciseBlocking", "Performed", "Trace", "Check", "Refusal", "ObserverFailure", "NativeFailure",
    "ArmedTick", "SentTick", "RequestTick", "ReturnTick", "PaidTick", "Paid", "AuraBefore", "AuraAfter", "Rest",
    "Cost", "SpendLeft", "Backlash", "OriginalBody", "PendingAtEntry", "ActionIdentity",
    "SourceActionIdentity", "ReturnActionIdentity", "ReturnCommitted", *("Source" + key for key in COUNTER_SOURCE_FIELDS)))
COUNTER_ADMISSION_CONTRACT = {
    "schemaVersion": 1, "witnessPattern": "case-{46..53}-admission.properties", "exactWitnessCount": 8,
    "requiredProperties": list(COUNTER_ADMISSION_FIELDS), "acceptedDigestProperty": "admissionReceiptSha256",
    "receivedSourceProperties": ["receivedSource" + key for key in COUNTER_SOURCE_FIELDS],
    "sharedInitialNativeTickBudget": 12, "nativeStatus": "unverified",
}
COUNTER_PROFILES = {
    "aura-wide": ("WCMoonWide2", "efc39378-3f97-37a3-bf28-b62b82f0bafa", "wide"),
    "aura-slim": ("WCMoonSlim", "0167ff94-ec08-3235-8727-7c1376ad1e7b", "slim"),
}
MOON_SUITE = "moon"
MOON_MAX_TIMEOUT = 180
MOON_CONTRACT = "src/gametest/resources/crimson-moon-native-contract.json"
MOON_ENTRYPOINT = "dev.wildercord.client.combat.CrimsonMoonMultiplayerCaptureTest"
MOON_CASE = "articulated_netherite_funded_right_release"
MOON_PROFILES = {
    "moon-wide": ("WCMoonWide2", "efc39378-3f97-37a3-bf28-b62b82f0bafa", "wide"),
    "moon-slim": ("WCMoonSlim", "0167ff94-ec08-3235-8727-7c1376ad1e7b", "slim"),
}
MOON_ANGLES = ("front_oblique", "reverse_oblique")
MOON_TERMINALS = {"host-moon-passed.properties": "host", "peer-moon-passed.properties": "peer",
                  "peer-disconnected.properties": "peer"}
MOON_MIXINS = ("crimson-moon-multiplayer-gametest.mixins.json",)
MOON_CLOCK_WITNESSES = {"case-00-clock-initial.properties": "peer", "case-00-clock-rendezvous.properties": "host",
                        "case-00-clock-ack.properties": "peer", "case-00-clock-ready.properties": "host"}
MOON_CASE_WITNESSES = ("case-00-ready.properties", *MOON_CLOCK_WITNESSES, "case-00-accepted.properties", "case-00-release.properties",
                       "host-case-00-observed.properties", "peer-case-00-observed.properties", "case-00-passed.properties")


def selection(suite=SUITE, profile="aura", case=None, observer_angle=None):
    if suite == SUITE:
        require(profile in ("aura", *COUNTER_PROFILES) and case is None and observer_angle is None,
                "Cast receipts require a fixed aura profile and the complete 54-case roster")
        profiles = PROFILES if profile == "aura" else {"host": COUNTER_PROFILES[profile][:2], "peer": PROFILES["peer"]}
        return {"suite": SUITE, "profile": profile, "contract": CONTRACT, "entrypoint": ENTRYPOINT,
                "cases": list(CASES), "profiles": profiles, "terminals": TERMINALS, "mixins": COUNTER_MIXINS,
                "maxTimeoutSeconds": MAX_TIMEOUT, "expectedSkin": None if profile == "aura" else COUNTER_PROFILES[profile][2]}
    require(suite == MOON_SUITE and profile in MOON_PROFILES and case == MOON_CASE and observer_angle in MOON_ANGLES,
            "Moon requires its one fixed release case, an original wide/slim profile and a declared observer angle")
    username, owner_uuid, skin = MOON_PROFILES[profile]
    return {"suite": MOON_SUITE, "profile": profile, "contract": MOON_CONTRACT, "entrypoint": MOON_ENTRYPOINT,
            "cases": [MOON_CASE], "profiles": {"host": (username, owner_uuid), "peer": PROFILES["peer"]},
            "terminals": MOON_TERMINALS, "mixins": MOON_MIXINS, "case": case, "expectedSkin": skin,
            "observerAngle": observer_angle, "maxTimeoutSeconds": MOON_MAX_TIMEOUT}


PATH_PROPERTIES = {"fabric.dli.config", "fabric.remapClasspathFile", "fabric.addMods", "fabric.classPathGroups",
                   "java.library.path", "org.lwjgl.librarypath", "log4j.configurationFile",
                   "fabric.client.gametest.testModResourcesPath", "fabric.gameJarPath", "fabric.gameJarPath.client"}
MULTIPLE_PATHS = {"fabric.addMods", "fabric.classPathGroups", "java.library.path", "org.lwjgl.librarypath"}
FIXED_PROPERTIES = {
    "fabric.dli.env": {"client"}, "fabric.dli.main": {"net.fabricmc.loader.impl.launch.knot.KnotClient"},
    "fabric.development": {"true"}, "fabric.client.gametest": {"", "true"},
    "fabric.client.gametest.modid": {"wildercord-gametest"},
    "fabric.client.gametest.disableJoinAsyncStackTraces": {"", "true", "false"},
    "log4j2.formatMsgNoLookups": {"true"}, "fabric.log.disableAnsi": {"true", "false"},
    "fabric.defaultModDistributionNamespace": {"official", "intermediary", "named"},
    "fabric.defaultMixinRemapType": {"static", "dynamic", "mixin", "none"},
    "file.encoding": {"UTF-8"}, "stdout.encoding": {"UTF-8"}, "stderr.encoding": {"UTF-8"},
    "java.awt.headless": {"false"},
}
JVM_FLAGS = {"-ea", "-XstartOnFirstThread", "-XX:+UseG1GC", "-XX:StackShadowPages=32",
             "--sun-misc-unsafe-memory-access=allow", "--enable-native-access=ALL-UNNAMED"}
GAME_OPTIONS = {"--assetIndex", "--assetsDir", "--gameDir", "--username", "--uuid", "--accessToken",
                "--version", "--versionType", "--userType", "--width", "--height"}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def inside(path, roots):
    resolved = Path(path).resolve()
    require(any(resolved == root or root in resolved.parents for root in roots),
            "Path escapes allowed roots: " + str(path))
    return resolved


def output_path(path, root):
    return inside(path, [root / "build", root / "build-alt", root / "artifacts"])


def digest(path):
    """Shared with the Gradle exporter: sorted relative name, NUL, file hash, LF."""
    path = Path(path)
    require(not path.is_symlink(), "Symlink runtime input: " + str(path))
    result = hashlib.sha256()
    if path.is_dir():
        for child in sorted(path.rglob("*"), key=lambda p: p.relative_to(path).as_posix()):
            require(not child.is_symlink(), "Symlink runtime input: " + str(child))
            if child.is_file():
                result.update(child.relative_to(path).as_posix().encode() + b"\0")
                result.update(digest(child).encode() + b"\n")
    else:
        with path.open("rb") as stream:
            for block in iter(lambda: stream.read(1024 * 1024), b""):
                result.update(block)
    return result.hexdigest()


def git(root, *args):
    return subprocess.run(["git", "-C", str(root), *args], check=True, text=True,
                          stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=15).stdout.strip()


def source_head(root):
    require(not git(root, "status", "--porcelain", "--untracked-files=normal"),
            "Source provenance requires a clean checkout, including untracked source")
    return git(root, "rev-parse", "HEAD")


def ignored_output(path, root):
    result = subprocess.run(["git", "-C", str(root), "check-ignore", "-q", str(path)], timeout=15)
    require(result.returncode == 0, "Evidence output must be Git-ignored; use build/native")


def configured_java():
    name = "java.exe" if os.name == "nt" else "java"
    java_home = os.environ.get("JAVA_HOME")
    candidate = str(Path(java_home) / "bin" / name) if java_home else shutil.which(name)
    require(candidate, "A configured Java toolchain is required")
    return Path(candidate).resolve()


def runtime_roots(root):
    cache = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle")).resolve()
    return [root, cache / "caches", cache / "jdks"]


def property_paths(key, value):
    if key == "fabric.dli.config":
        value = re.sub(r"@@([0-9a-fA-F]{1,4})", lambda match: chr(int(match[1], 16)), value)
    separator = "," if key == "log4j.configurationFile" else os.pathsep
    values = value.split(separator) if key in MULTIPLE_PATHS or key == "log4j.configurationFile" else [value]
    return [Path(item) for item in values if item]


def validate_jvm(arguments, roots, launcher=True):
    require(isinstance(arguments, list) and len(arguments) <= 256, "Unbounded JVM arguments")
    properties = {}
    for argument in arguments:
        require(isinstance(argument, str) and not any(c in argument for c in "\0\r\n"), "Malformed JVM argument")
        if re.fullmatch(r"-Xm[sx]\d+[kKmMgG]?", argument) or argument in JVM_FLAGS:
            require(launcher, "Launch config may contain only properties")
            continue
        require(argument.startswith("-D"), "Unapproved JVM option: " + argument)
        key, _, value = argument[2:].partition("=")
        require(key not in properties, "Duplicate JVM property: " + key)
        properties[key] = value
        if key in PATH_PROPERTIES:
            require(value, "Empty JVM path: " + key)
            for path in property_paths(key, value):
                require(path.is_absolute(), "Runtime paths must be absolute")
                inside(path, roots)
        else:
            require(key in FIXED_PROPERTIES and value in FIXED_PROPERTIES[key], "Unapproved JVM property: " + key)
    if launcher:
        require(properties.get("fabric.dli.env") == "client" and properties.get("fabric.dli.main") == "net.fabricmc.loader.impl.launch.knot.KnotClient",
                "Expected the fixed offline Fabric client launcher")
        require("fabric.dli.config" in properties, "Missing Fabric launch config")
    else:
        require(not any(key.startswith("fabric.dli.") for key in properties), "Config cannot override its own launcher")
    return properties


def validate_game_args(arguments, roots, config=False):
    require(isinstance(arguments, list) and len(arguments) <= 64 and len(arguments) % 2 == 0, "Malformed or unbounded game arguments")
    seen = set()
    for index in range(0, len(arguments), 2):
        flag, value = arguments[index:index + 2]
        require(flag in GAME_OPTIONS and flag not in seen, "Unapproved or duplicate game option: " + str(flag))
        require(not config or flag in {"--assetIndex", "--assetsDir"}, "Loom config cannot override supervised game settings")
        seen.add(flag)
        require(isinstance(value, str) and not any(c in value for c in "\0\r\n"), "Malformed game argument")
        if flag in {"--gameDir", "--assetsDir"}:
            require(Path(value).is_absolute(), "Expected absolute game path")
            inside(value, roots)
        if flag in {"--width", "--height"}:
            require(value.isdigit() and 1 <= int(value) <= 1920, "Unbounded window dimensions")


def launch_config(path, roots):
    """Parse Fabric dev-launch-injector's actual common/client section format."""
    require(path.stat().st_size <= 65536, "Oversized launch config")
    section, props, args = None, [], []
    for raw in path.read_text(encoding="utf-8").splitlines():
        if not raw.strip():
            continue
        if raw[0] not in " \t":
            section = raw.strip()
            require(section in {"commonProperties", "clientProperties", "serverProperties", "commonArgs", "clientArgs", "serverArgs"}, "Unknown launch config section")
        elif section in {"commonProperties", "clientProperties"}:
            key, _, value = raw.strip().partition("=")
            props.append("-D" + key.strip() + "=" + value.strip())
        elif section in {"commonArgs", "clientArgs"}:
            args.append(raw.strip())
        else:
            require(section in {"serverProperties", "serverArgs"}, "Config value has no section")
    validate_game_args(args, roots, config=True)
    return validate_jvm(props, roots, launcher=False), args


def load_contract(root, selected=None):
    selected = selected or selection()
    contract = json.loads((root / selected["contract"]).read_text())
    require(contract.get("schemaVersion") == 1 and contract.get("suite") == selected["suite"]
            and contract.get("entrypoint") == selected["entrypoint"], "Unexpected source-controlled suite contract")
    cases = contract.get("cases")
    require(cases == selected["cases"] and contract.get("expectedCount") == len(cases),
            "Cast contract requires the exact ordered 54-case roster" if selected["suite"] == SUITE
            else "Moon contract requires the one exact fixed release case")
    require(contract.get("limits") == {"maxJvms": 2, "maxHeapMiBPerJvm": 2048, "maxCases": len(cases), "maxTimeoutSeconds": selected["maxTimeoutSeconds"]},
            "Unexpected contract resource limits")
    require(contract.get("terminalWitnesses") == list(selected["terminals"]), "Unexpected terminal paths")
    profiles = ({"aura": {"username": PROFILES["host"][0], "offlineUuid": PROFILES["host"][1]},
                 **{key: {"username": value[0], "offlineUuid": value[1], "expectedSkin": value[2]} for key, value in COUNTER_PROFILES.items()}}
                if selected["suite"] == SUITE else
                {key: {"username": value[0], "offlineUuid": value[1], "skin": value[2]} for key, value in MOON_PROFILES.items()})
    require(contract.get("profiles") == profiles, "Invalid fixed host profiles")
    require(contract.get("peerProfile") == {"username": PROFILES["peer"][0], "offlineUuid": PROFILES["peer"][1]}, "Invalid fixed peer profile")
    if selected["suite"] == SUITE:
        require(contract.get("requiredMixins") == list(COUNTER_MIXINS), "Counter passive hooks must be mandatory")
        visuals = contract.get("counterVisuals", {})
        require(visuals.get("casePrefixCount") == 46 and visuals.get("phases") == list(COUNTER_PHASES)
                and visuals.get("requiredProfiles") == list(COUNTER_PROFILES), "Counter contract must bind the complete additive phase/profile union")
        require(visuals.get("admission") == COUNTER_ADMISSION_CONTRACT, "Counter contract requires the exact native admission proof closure")
    if selected["suite"] == MOON_SUITE:
        require(contract.get("observerAngles") == list(MOON_ANGLES) and contract.get("requiredMixins") == list(MOON_MIXINS),
                "Unexpected Moon angle or passive-hook contract")
        require(contract.get("requiredWitnessProperties") == list(IDENTITY) + ["case", "expectedSkin", "observerAngle", "role", "pid", "cases"],
                "Moon witnesses must bind every selection and provenance field")
        require(contract.get("caseWitnesses") == list(MOON_CASE_WITNESSES), "Moon requires the complete ordered action/image ledger")
        require(contract.get("proofFlags") == {"releaseImageDamageOrderVerified": "required true on final case and terminals",
                "serverReleaseFrameCorrespondenceVerified": False, "pixelQualityReviewed": False},
                "Moon proof flags must retain the causal interval and native review limits")
    return contract


def validate_launch(launch, path, root, selected=None):
    selected = selected or selection()
    require(launch.get("schemaVersion") == 2 and launch.get("suite") == selected["suite"], "Re-export the selected allowlisted suite")
    require(launch.get("project") == str(root) and launch.get("sourceHead") == source_head(root), "Export belongs to a different checkout or stale HEAD")
    require(launch.get("checkoutSha") == launch["sourceHead"], "Checkout association differs from the verified source HEAD")
    pr_head = launch.get("prHeadSha")
    require(isinstance(pr_head, str) and (not pr_head or re.fullmatch(r"[a-f0-9]{40}", pr_head)), "Malformed PR-head association")
    require(pr_head == os.environ.get("WILDERCORD_PR_HEAD_SHA", ""), "PR-head association differs from explicit workflow metadata")
    require(launch.get("main") == MAIN, "Only the fixed Fabric client launcher may execute")
    roots = runtime_roots(root)
    java = Path(launch["java"]).resolve()
    require(java == configured_java() or roots[2] in java.parents, "Java is not the configured toolchain")
    require(java.name in {"java", "java.exe"} and java.is_file(), "Invalid Java executable")
    descriptor = inside(launch["descriptor"], [root / "build", root / "build-alt"])
    require(descriptor.name == "fabric.mod.json" and digest(descriptor) == launch["descriptorSha256"], "Generated descriptor changed after export")
    descriptor_data = json.loads(descriptor.read_text())
    require(descriptor_data["entrypoints"]["fabric-client-gametest"] == [selected["entrypoint"]], "Descriptor must select exactly the supervised fixture")
    if selected["suite"] == MOON_SUITE:
        require(launch.get("maxTimeoutSeconds") == MOON_MAX_TIMEOUT, "Moon export must bind the 180-second ceiling")
        require("crimson-moon-owner-gametest.mixins.json" not in descriptor_data.get("mixins", []), "Paired Moon hooks must not be injected twice")
    for name in selected["mixins"]:
        require(descriptor_data.get("mixins", []).count(name) == 1, "Selected passive hooks must be registered exactly once")
        require(digest(descriptor.parent / name) == digest(root / "src/gametest/resources" / name), "Selected passive hook descriptor changed")
    contract = selected["contract"]
    require(launch.get("contract") == contract and launch.get("contractSha256") == digest(root / contract), "Source contract changed after export")
    require(digest(descriptor.parent / Path(contract).name) == digest(root / contract), "Processed contract differs from source contract")
    props = validate_jvm(launch["jvm"], roots)
    config_props, config_args = launch_config(property_paths("fabric.dli.config", props["fabric.dli.config"])[0], roots)
    validate_game_args(config_args + launch["args"], roots)
    require(launch.get("environment") in ({}, {"ALSOFT_DRIVERS": "null"}), "Unapproved exported environment")
    classpath = launch["classpath"]
    require(isinstance(classpath, list) and 0 < len(classpath) <= 1024 and len(set(classpath)) == len(classpath), "Invalid bounded classpath")
    require(str(descriptor.parent) in classpath, "Selected descriptor is absent from classpath")
    expected = {str(java)}
    for item in classpath:
        require(Path(item).is_absolute(), "Classpath entries must be absolute")
        expected.add(str(inside(item, roots)))
    combined = {**props, **config_props}
    for key, value in combined.items():
        if key in PATH_PROPERTIES:
            expected.update(str(inside(item, roots)) for item in property_paths(key, value))
    if "fabric.remapClasspathFile" in combined:
        remap = Path(combined["fabric.remapClasspathFile"])
        require(remap.stat().st_size <= 1024 * 1024, "Oversized remap classpath")
        items = remap.read_text().strip().split(os.pathsep)
        require(len(items) <= 1024, "Unbounded remap classpath")
        expected.update(str(inside(item, roots)) for item in items if item)
    fingerprints = launch.get("runtimeSha256", {})
    require(isinstance(fingerprints, dict) and expected == fingerprints.keys() and len(fingerprints) <= 2048, "Fingerprints must cover every runtime input")
    for item, expected_hash in fingerprints.items():
        if Path(item).resolve() != java:
            inside(item, roots)
        require(isinstance(expected_hash, str) and re.fullmatch(r"[0-9a-f]{64}", expected_hash) and digest(item) == expected_hash,
                "Runtime input changed after export: " + item)
    return hashlib.sha256(path.read_bytes()).hexdigest()


def java_quote(value):
    return '"' + str(value).replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n').replace('\r', '\\r') + '"'


def command(launch, role, game, ipc, identity, timeout, selected=None):
    selected = selected or selection()
    replace = {"--gameDir": str(game), "--username": selected["profiles"][role][0], "--uuid": selected["profiles"][role][1],
               "--accessToken": "0", "--width": "960", "--height": "540"}
    args = []
    for index in range(0, len(launch["args"]), 2):
        if launch["args"][index] not in replace:
            args.extend(launch["args"][index:index + 2])
    for key, value in replace.items():
        args.extend([key, value])
    jvm = [arg for arg in launch["jvm"] if not re.match(r"-Xm[sx]", arg)]
    jvm += ["-Xms256m", "-Xmx2G", "-Djava.io.tmpdir=" + str(game / "tmp")]
    for key, value in {**identity, "role": role, "directory": str(ipc), "timeoutSeconds": str(timeout)}.items():
        jvm.append("-Dwildercord.mp." + key + "=" + value)
    if selected["suite"] == MOON_SUITE:
        for key in ("case", "expectedSkin", "observerAngle"):
            jvm.append("-Dwildercord.moon." + key + "=" + selected[key])
    elif selected.get("expectedSkin"):
        jvm.append("-Dwildercord.counter.expectedSkin=" + selected["expectedSkin"])
    return [*jvm, "-cp", os.pathsep.join(launch["classpath"]), MAIN, *args]


def read_properties(path):
    require(path.is_file() and not path.is_symlink(), "Missing or symlink terminal witness: " + str(path))
    require(path.stat().st_size <= 65536, "Oversized terminal witness")
    raw = path.read_bytes().decode("iso-8859-1")
    # Properties.load recognizes only CR/LF physical boundaries. Python splitlines()
    # also splits NEL, VT, FF and information separators, which can hide a Java value.
    # Native Properties.store escapes controls and never emits continued physical lines.
    require(not any((ord(char) < 32 and char not in "\r\n") or 127 <= ord(char) <= 159 for char in raw),
            "Unsupported raw control in native witness")
    result = {}
    for line in re.split(r"\r\n|\r|\n", raw):
        if not line.strip(" ") or line.lstrip(" ").startswith(("#", "!")):
            continue
        key, separator, value = line.partition("=")
        require(separator and re.fullmatch(r"[A-Za-z][A-Za-z0-9_.-]*", key) and key not in result,
                "Malformed, noncanonical or duplicate witness property")
        require(not value.startswith(" ") and re.fullmatch(r"(?:[^\\]|\\(?:[tnrf\\:= #!]|u[0-9a-fA-F]{4}))*", value),
                "Noncanonical or continued native witness value")
        result[key] = value
    return result


def validate_witnesses(ipc, identity, cases, jobs, selected=None):
    selected = selected or selection()
    require(cases == selected["cases"], "Unexpected completed case ledger")
    pids = {role: process.pid for role, process in jobs}
    require(set(pids) == {"host", "peer"} and len(set(pids.values())) == 2, "Exactly two distinct owned JVMs required")
    for filename, role in selected["terminals"].items():
        values = read_properties(ipc / filename)
        for key, value in {**identity, "role": role, "pid": str(pids[role]), "cases": ",".join(cases)}.items():
            require(values.get(key) == value, filename + " has mismatched " + key)
    if selected["suite"] == MOON_SUITE:
        validate_moon_evidence(ipc, identity, pids)
    else:
        require(cases == list(CASES), "Terminal evidence must cover the exact ordered 54-case roster")
        validate_life_witnesses(ipc, identity, pids)
        validate_counter_witnesses(ipc, identity, pids, selected)
    return list(selected["terminals"])


def witness(ipc, filename, role, identity, pids):
    values = read_properties(ipc / filename)
    expected = {**identity, "role": role, "pid": str(pids[role]), "cases": MOON_CASE,
                "serverReleaseFrameCorrespondenceVerified": "false"}
    for key, value in expected.items():
        require(values.get(key) == value, filename + " has mismatched " + key)
    return values


def decimal(value, label, positive=False):
    require(isinstance(value, str) and re.fullmatch(r"0|[1-9][0-9]{0,18}", value), "Invalid " + label)
    number = int(value)
    require(number <= 2**63 - 1 and (not positive or number > 0), "Invalid " + label)
    return number


def finite(value, label):
    require(isinstance(value, (int, float, str)) and not isinstance(value, bool), "Invalid " + label)
    try:
        number = float(value)
    except (ValueError, OverflowError) as error:
        raise ValueError("Invalid " + label) from error
    require(math.isfinite(number), "Nonfinite " + label)
    return number


def evidence_file(game, relative, maximum, label):
    require(isinstance(relative, str) and relative and not any(c in relative for c in "\0\r\n")
            and not Path(relative).is_absolute(), "Invalid " + label + " relative path")
    path = inside(game / relative, [game])
    require(path.is_file() and not path.is_symlink() and path.stat().st_size <= maximum, "Missing or oversized " + label)
    return path


def moon_frame(value, kind, accepted):
    fields = {"activation", "move", "left", "master", "yaw", "pitch", "tilt", "footwork", "velocity", "pose"}
    require(isinstance(value, dict) and set(value) == fields, "Missing complete Moon " + kind + " frame identity")
    require(type(value["activation"]) is int and value["activation"] == accepted and type(value["move"]) is int
            and value["move"] == 19 and value["left"] is False and value["master"] is False
            and type(value["footwork"]) is bool, "Wrong nested Moon action/hand identity")
    for key in ("yaw", "pitch", "tilt"):
        require(type(value[key]) in (int, float), "Invalid numeric Moon frame " + key)
        finite(value[key], "Moon frame " + key)
    require(isinstance(value["pose"], str) and isinstance(value["velocity"], str), "Missing concrete Moon pose/velocity identifiers")
    if kind == "classic":
        require(value["footwork"] is False and value["velocity"] == "n/a" and re.fullmatch(r"[a-f0-9]{64}", value["pose"]),
                "Invalid Classic Moon frame schema")
    else:
        velocity = value["velocity"]
        # Player frames intentionally use Double.NaN when no master footwork velocity is supplied.
        require(velocity == "NaN" or re.fullmatch(r"0x[0-9a-f]+\.[0-9a-f]+p-?[0-9]+", velocity), "Invalid articulated Moon velocity encoding")
        if velocity != "NaN":
            require(math.isfinite(float.fromhex(velocity)) and float.fromhex(velocity) >= 0, "Invalid articulated Moon velocity")
        require(value["tilt"] == 0 and re.fullmatch(r"ACTIVE:[a-f0-9]{64}:[a-f0-9]{64}", value["pose"]),
                "Missing active articulated body/view pose identities")


def moon_image(game, role, values, identity, action, release_sha):
    """Rebind actual bytes and numeric comparisons; human material/silhouette review remains separate."""
    receipt = evidence_file(game, values.get("receiptRelativePath"), 8 * 1024 * 1024, "Moon receipt")
    require(digest(receipt) == values.get("receiptSha256"), "Moon receipt bytes changed")
    record = json.loads(receipt.read_text(encoding="utf-8"))
    require(record.get("schemaVersion") == 3 and record.get("verified") is True and not record.get("failures"), "Missing verified paired Moon numeric receipt")
    require(record.get("launchNonce") == identity["nonce"], "Stale Moon image nonce")
    require(record.get("scopeCleanupVerified") is True and record.get("releaseObservedBeforeSource") is True,
            "Missing source-scope cleanup or causal release observation")
    require(record.get("serverReleaseFrameCorrespondenceVerified") is False and record.get("pixelQualityReviewed") is False,
            "An image alone cannot claim future damage ordering or human pixel review")
    expected = record.get("expected", {})
    view = "fp" if role == "host" else "remote"
    require(expected.get("name") == values.get("screenshotName") and expected.get("view") == view
            and expected.get("owner") == action["actorEntity"] and expected.get("uuid") == identity["hostUuid"]
            and expected.get("activation") == action["acceptedTick"] and expected.get("mode") == "articulated"
            and expected.get("hand") == "RIGHT" and str(expected.get("skin", "")).lower() == identity["expectedSkin"]
            and expected.get("armor") is True and expected.get("shell") is True and expected.get("phase") == "ACTIVE"
            and type(expected.get("owner")) is int and type(expected.get("activation")) is int
            and finite(expected.get("age"), "requested source age") == 10,
            "Moon screenshot belongs to a different actor, action, view or appearance")
    copy = record.get("copy", {})
    observation = copy.get("observations", {})
    sequences = [copy.get(key) for key in ("extractSequence", "renderSequence", "copySequence")]
    require(all(type(value) is int and value > 0 for value in sequences) and sequences[0] < sequences[1] < sequences[2],
            "Moon extraction, render and GPU-copy order is incomplete")
    observer = identity["hostUuid"] if role == "host" else identity["peerUuid"]
    observer_entity = action["actorEntity"] if role == "host" else action["observerEntity"]
    require(observation.get("owner") == identity["hostUuid"] and observation.get("observerUuid") == observer
            and observation.get("observerEntity") == str(observer_entity) and observation.get("pairedRole") == role
            and observation.get("observerAngle") == identity["observerAngle"]
            and observation.get("observerCoverage") == ("false" if role == "host" else "true"),
            "Moon actor/observer provenance differs from the actual connected roles")
    require(observation.get("releaseReceiptSha256") == release_sha
            and observation.get("serverReleaseTick") == str(action["releaseTick"]), "Image is not associated with the exact immutable release receipt")
    read_sequence = decimal(observation.get("releaseReadSequence"), "release-read sequence", True)
    source_sequence = decimal(observation.get("sourceFrameSequence"), "source-frame sequence", True)
    require(read_sequence < source_sequence < sequences[0], "Release read, source frame, extraction, render and copy must form one strict sequence")
    age = finite(values.get("actualSourceAge"), "Moon source age")
    require(10 <= age < 11 and abs(finite(observation.get("actualSourceAge"), "observed source age") - age) <= .0001,
            "A release image must bind the strict rendered ACTIVE window")
    passes = copy.get("passes")
    require(isinstance(passes, list) and 0 < len(passes) <= 256, "Missing or unbounded Moon passes")
    kinds = {p.get("kind") for p in passes}
    required_kinds = {"view_submit", "view_deferred", "view_item"} if role == "host" else {"body_submit", "body", "world_item"}
    require(len(passes) == 3 and kinds == required_kinds, "Moon requires exactly one of each role-specific geometry/item pass")
    texture = observation.get("connectedSkinTexture")
    require(isinstance(texture, str) and re.fullmatch(r"[a-z0-9_.-]+:[a-z0-9/._-]+", texture)
            and str(observation.get("connectedSkinModel", "")).lower() == identity["expectedSkin"],
            "Missing concrete original connected skin texture/model")
    material = decimal(observation.get("originalSkinMaterialIdentity"), "original skin material", True)
    require(observation.get("submittedSkinMaterialIdentity") == str(material), "Submitted skin material changed")
    if role == "host":
        require(all(observation.get(key) == "true" for key in ("ordinaryHandAdmission", "handEquipKnown", "handSameItem"))
                and observation.get("handEquipping") == "false", "Missing eligible ordinary first-person hand witness")
    else:
        require(observation.get("worldHandEligible") == "true", "Missing eligible ordinary world hand witness")
    first = None
    for render in passes:
        binding, geometry = render.get("binding", {}), render.get("geometry", {})
        palette = binding.get("palette", {})
        require(render.get("segmented") is True and render.get("rigid") is False, "Fixed Moon slice requires articulated segmented passes")
        moon_frame(palette.get("classic"), "classic", action["acceptedTick"])
        moon_frame(palette.get("articulated"), "articulated", action["acceptedTick"])
        require(binding.get("matched") is True and binding.get("owner") == action["actorEntity"]
                and binding.get("uuid") == identity["hostUuid"] and binding.get("hand") == "RIGHT"
                and str(binding.get("skin", "")).lower() == identity["expectedSkin"]
                and palette.get("activation") == action["acceptedTick"] and palette.get("move") == 19
                and palette.get("phase") == "ACTIVE" and abs(finite(palette.get("age"), "pass source age") - age) <= .0001,
                "A submitted pass changed actor, complete frame or appearance")
        require(binding.get("texture") == texture and type(binding.get("skinMaterial")) is int and binding["skinMaterial"] == material,
                "A submitted pass lost original skin/material binding")
        stable = {key: binding.get(key) for key in ("source", "state", "model", "skinMaterial", "palette", "texture", "skin", "hand")}
        if role == "peer":
            require(type(binding.get("item")) is int and binding["item"] > 0, "Unbound remote held item")
            stable["item"] = binding["item"]
        if first is None:
            first = stable
        require(stable == first and all(type(binding.get(key)) is int and binding[key] > 0 for key in ("source", "state", "model")),
                "Moon pass identity changed after extraction")
        if "item" in render.get("kind", ""):
            require(type(binding.get("item")) is int and binding["item"] > 0, "Unbound held item")
        elif role == "host":
            require(type(binding.get("item")) is int and binding["item"] == 0, "Unexpected item identity on a first-person geometry pass")
        expected_values, actual_values = geometry.get("expected"), geometry.get("actual")
        require(isinstance(expected_values, list) and isinstance(actual_values, list) and 0 < len(expected_values) == len(actual_values) <= 1024,
                "Missing actual geometry comparisons")
        error = max(abs(finite(a, "expected geometry") - finite(b, "actual geometry")) for a, b in zip(expected_values, actual_values))
        require(geometry.get("matched") is True and error <= .00025 and abs(finite(geometry.get("maxError"), "geometry error") - error) <= .000001,
                "Actual Moon geometry does not match its independent source oracle")
    image = record.get("image", {})
    require(image.get("relativeImagePath") == values.get("pngRelativePath"), "Returned PNG path differs from the witness")
    png = evidence_file(game, values.get("pngRelativePath"), 64 * 1024 * 1024, "Moon PNG")
    data = png.read_bytes()
    require(len(data) == image.get("pngBytes") and hashlib.sha256(data).hexdigest() == image.get("pngSha256") == values.get("pngSha256"),
            "Returned Moon PNG bytes changed")
    # Reuse the already bounded decoder; no alternate image parser or weaker CRC policy.
    import importlib.util
    path = Path(__file__).resolve().parents[1] / "verify_articulated_render_receipts.py"
    spec = importlib.util.spec_from_file_location("wildercord_moon_image_decoder", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    pixels = module.image_evidence(data)
    require(pixels == image.get("decodedPixels") == record.get("callbackPixels")
            and pixels["sha256"] == values.get("callbackPixelSha256"), "Moon callback pixels differ from decoded unchanged PNG")
    require(copy.get("width") == pixels["width"] and copy.get("height") == pixels["height"], "Moon copy target dimensions changed")
    return record


def validate_moon_clock(ipc, identity, pids, actors, accepted_tick):
    """Verify the immutable pre-input rendezvous, independently of later pacing witnesses."""
    phases = {}
    for filename, role in MOON_CLOCK_WITNESSES.items():
        values = witness(ipc, filename, role, identity, pids)
        require(all(values.get(key) == value for key, value in actors.items()),
                filename + " changed the Moon clock actor/observer pair")
        phases[filename] = values
    initial, rendezvous, ack, ready = phases.values()
    client = decimal(initial.get("clockInitialClientTick"), "Moon initial client tick")
    server = decimal(rendezvous.get("clockInitialServerTick"), "Moon initial server tick")
    target = decimal(rendezvous.get("clockRendezvousTick"), "Moon clock rendezvous tick")
    require(target == max(client, server), "Moon clock rendezvous must equal the maximum initial clock")
    shared = {"clockInitialClientTick": str(client), "clockInitialServerTick": str(server),
              "clockRendezvousTick": str(target)}
    for label, values in (("rendezvous", rendezvous), ("ack", ack), ("ready", ready)):
        require(all(values.get(key) == value for key, value in shared.items()),
                "Moon clock " + label + " changed the immutable initial clocks or rendezvous")
    require(rendezvous.get("clockServerTick") == str(target) and ack.get("clockClientTick") == str(target)
            and ready.get("clockServerTick") == str(target) and ready.get("clockClientTick") == str(target),
            "Moon clocks must rendezvous exactly without overshoot")
    upstream = {}
    for label, values in (("initial", initial), ("rendezvous", rendezvous), ("ack", ack), ("ready", ready)):
        require(all(values.get(key) == value for key, value in upstream.items()),
                "Moon clock " + label + " changed an upstream receipt hash")
        upstream["clock" + label.capitalize() + "Sha256"] = digest(ipc / ("case-00-clock-" + label + ".properties"))
    require(target < accepted_tick, "Moon action must be accepted after the completed clock rendezvous")
    return {"clockRendezvousTick": str(target), "clockReadySha256": upstream["clockReadySha256"]}


def validate_moon_evidence(ipc, identity, pids):
    ready = witness(ipc, "case-00-ready.properties", "host", identity, pids)
    accepted = witness(ipc, "case-00-accepted.properties", "host", identity, pids)
    released = witness(ipc, "case-00-release.properties", "host", identity, pids)
    passed = witness(ipc, "case-00-passed.properties", "host", identity, pids)
    action = {"actorEntity": decimal(ready.get("actorEntity"), "actor entity", True),
              "observerEntity": decimal(ready.get("observerEntity"), "observer entity", True),
              "acceptedTick": decimal(accepted.get("acceptedTick"), "acceptance tick"),
              "releaseTick": decimal(released.get("releaseTick"), "release tick")}
    require(action["actorEntity"] != action["observerEntity"], "Remote image must observe a distinct actual player")
    require(ready.get("actorUuid") == identity["hostUuid"] and ready.get("observerUuid") == identity["peerUuid"], "Wrong ready actor/observer pair")
    actors = {"actorEntity": str(action["actorEntity"]), "observerEntity": str(action["observerEntity"]),
              "actorUuid": identity["hostUuid"], "observerUuid": identity["peerUuid"]}
    clock = validate_moon_clock(ipc, identity, pids, actors, action["acceptedTick"])
    common = {**actors, **clock, "acceptedTick": str(action["acceptedTick"]), "move": "19", "windup": "10", "recovery": "20"}
    for label, values in (("accepted", accepted), ("released", released), ("passed", passed)):
        require(all(values.get(key) == value for key, value in common.items()), "Moon " + label + " action changed")
    require(action["releaseTick"] == action["acceptedTick"] + 10 and released.get("completionOffset") == "10"
            and released.get("marks") == "FULL,FULL,FULL,LOW", "Moon must complete the actual Final once at +10")
    release_sha = digest(ipc / "case-00-release.properties")
    observed = {}
    for role in ("host", "peer"):
        filename = role + "-case-00-observed.properties"
        values = witness(ipc, filename, role, identity, pids)
        require(all(values.get(key) == value for key, value in common.items()) and values.get("releaseReceiptSha256") == release_sha,
                "Observed image belongs to a different accepted release")
        require(values.get("view") == ("fp" if role == "host" else "remote")
                and values.get("releaseObservedBeforeSource") == "true" and values.get("scopeCleanupVerified") == "true"
                and values.get("serverReleaseFrameCorrespondenceVerified") == "false" and values.get("releaseImageDamageOrderVerified") == "false",
                "Observed image overstates its proof or lacks causal cleanup")
        moon_image(ipc.parent / role, role, values, identity, action, release_sha)
        require(passed.get(role + "ObservedBeforeFirstDamage") == "true" and passed.get(role + "ObservedWitnessSha256") == digest(ipc / filename),
                "Real first damage did not witness the immutable completed " + role + " image")
        for field in ("PngSha256", "CallbackPixelSha256", "ReceiptSha256"):
            require(passed.get(role + field) == values.get(field[0].lower() + field[1:]), "Final case changed the " + role + " image binding")
        observed[role] = values
    require(passed.get("serverReleaseFrameCorrespondenceVerified") == "false" and passed.get("releaseImageDamageOrderVerified") == "true"
            and passed.get("completionOffset") == "10"
            and passed.get("releaseTick") == str(action["releaseTick"]), "Missing accurately labeled causal release/image/damage order")
    damage = decimal(passed.get("firstDamageTick"), "first damage tick")
    delay = decimal(passed.get("firstDamageDelayTicks"), "first damage delay", True)
    age = decimal(passed.get("firstDamageAgeTicks"), "first damage age", True)
    require(1 <= delay <= 5 and 11 <= age <= 15 and damage == action["releaseTick"] + delay
            and damage == action["acceptedTick"] + age, "Moon direct damage changed its original acceptance/release-relative timing")
    for filename, role in MOON_TERMINALS.items():
        values = witness(ipc, filename, role, identity, pids)
        require(all(values.get(key) == passed.get(key) for key in common), "Terminal changed the accepted Moon action")
        require(values.get("serverReleaseFrameCorrespondenceVerified") == "false" and values.get("releaseImageDamageOrderVerified") == "true",
                "Terminal must claim only the proven causal interval, not exact server-frame correspondence")
        require(all(values.get(key) == passed.get(key) for key in ("releaseTick", "firstDamageTick", "firstDamageAgeTicks", "firstDamageDelayTicks")),
                "Terminal changed the proven release/damage interval")
    require(observed["host"]["pngRelativePath"] != observed["peer"]["pngRelativePath"] or observed["host"]["pngSha256"] != observed["peer"]["pngSha256"],
            "Owner and observer cannot reuse the same image evidence")


def counter_case(case):
    require(case in COUNTER_CASES, "Unknown fixed counter case")
    move = 24 if case.startswith("COUNTER_UNMOVED_") else 25
    return {"move": move, "windup": 6 if move == 24 else 4, "recovery": 16 if move == 24 else 14,
            "mode": "articulated" if "_ARTICULATED_" in case else "classic", "hand": case.rsplit("_", 1)[1]}


def counter_phase(move, age):
    age = finite(age, "counter actual source age")
    require(move in (24, 25), "Unknown counter action")
    windup, recovery = (6, 16) if move == 24 else (4, 14)
    return ("NONE" if age < 0 or age >= windup + recovery else "WINDUP" if age < windup else
            "ACTIVE" if age < windup + 1 else "FOLLOW" if age < windup + recovery / 2 else "RECOVERY")


def counter_frame(value, kind, action, phase):
    fields = {"activation", "move", "left", "master", "yaw", "pitch", "tilt", "footwork", "velocity", "pose"}
    require(isinstance(value, dict) and set(value) == fields, "Missing complete counter " + kind + " frame")
    require(type(value["activation"]) is int and value["activation"] == action["acceptedTick"]
            and type(value["move"]) is int and value["move"] == action["move"]
            and value["left"] is (action["hand"] == "LEFT") and value["master"] is False
            and value["footwork"] is False, "Wrong nested counter action or hand")
    for key in ("yaw", "pitch", "tilt"):
        require(type(value[key]) in (int, float), "Invalid counter numeric frame " + key)
        finite(value[key], "counter frame " + key)
    require(isinstance(value["pose"], str), "Missing concrete counter pose")
    if kind == "classic":
        require(value["velocity"] == "n/a" and re.fullmatch(r"[a-f0-9]{64}", value["pose"]), "Invalid counter Classic frame")
    else:
        # FOLLOW is the early recovery subdivision of the actual articulated RECOVERY palette.
        native_phase = "RECOVERY" if phase == "FOLLOW" else phase
        require(value["tilt"] == 0 and value["velocity"] == "NaN"
                and re.fullmatch(native_phase + r":[a-f0-9]{64}:[a-f0-9]{64}", value["pose"]),
                "Wrong counter articulated source palette phase")


def counter_json(path):
    def object_pairs(pairs):
        value = {}
        for key, item in pairs:
            require(key not in value, "Duplicate counter JSON property: " + key)
            value[key] = item
        return value
    def invalid_constant(value):
        raise ValueError("Nonfinite counter JSON constant: " + value)
    value = json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=object_pairs, parse_constant=invalid_constant)
    require(isinstance(value, dict), "Counter JSON must be an object")
    return value


def counter_evidence_file(game, relative, maximum, label):
    require(isinstance(relative, str) and relative and not Path(relative).is_absolute()
            and ".." not in Path(relative).parts, "Invalid counter evidence path")
    raw = game / relative
    require(not any(path.is_symlink() for path in (raw, *raw.parents)), "Symlink counter evidence path")
    path = evidence_file(game, relative, maximum, label)
    require(path.stat().st_nlink == 1, "Hard-linked counter evidence is forbidden")
    return path


def counter_image(game, role, values, identity, action, accepted_sha):
    """Recompute source/geometry/pixel associations; this is never human silhouette review."""
    receipt = counter_evidence_file(game, values.get("receiptRelativePath"), 8 * 1024 * 1024, "counter receipt")
    require(digest(receipt) == values.get("receiptSha256"), "Counter receipt bytes changed")
    record = counter_json(receipt)
    require(type(record.get("schemaVersion")) is int and record["schemaVersion"] == 1 and record.get("verified") is True and record.get("failures") == [],
            "Missing verified counter numeric receipt")
    require(record.get("launchNonce") == identity["nonce"] and record.get("scopeCleanupVerified") is True
            and record.get("acceptanceObservedBeforeSource") is True, "Stale counter nonce or incomplete source scope")
    require(record.get("phaseBasis") == COUNTER_PHASE_BASIS and record.get("pixelQualityReviewed") is False
            and record.get("serverReleaseFrameCorrespondenceVerified") is False, "Counter receipt overstates phase or pixel proof")
    expected, phase = record.get("expected", {}), values["phase"]
    require(isinstance(expected, dict), "Missing counter expected identity")
    view, skin = ("fp" if role == "host" else "remote"), values["skin"].lower()
    name = "counter_peer_" + ("unmoved" if action["move"] == 24 else "null_parry") + "_" + action["mode"] + "_" + action["hand"].lower() + "_" + role + "_" + phase.lower()
    fields = {"name": name, "view": view, "owner": action["actorEntity"], "uuid": identity["hostUuid"],
              "activation": action["acceptedTick"], "mode": action["mode"], "hand": action["hand"],
              "phase": phase, **{key: action[key] for key in ("move", "windup", "recovery")}}
    require(all(expected.get(key) == value for key, value in fields.items()) and values.get("screenshotName") == name
            and str(expected.get("skin", "")).lower() == skin and expected.get("armor") is False and expected.get("shell") is False
            and all(type(expected.get(key)) is int for key in ("owner", "activation", "move", "windup", "recovery")),
            "Counter screenshot changed actor, action, phase, camera or original appearance")
    copy = record.get("copy", {})
    require(isinstance(copy, dict), "Missing render copy object")
    observation = copy.get("observations", {})
    require(isinstance(observation, dict), "Missing render observation object")
    sequences = [copy.get(key) for key in ("extractSequence", "renderSequence", "copySequence")]
    require(all(type(value) is int and value > 0 for value in sequences) and sequences[0] < sequences[1] < sequences[2],
            "Counter extraction, render and GPU-copy order is incomplete")
    observer = identity["hostUuid"] if role == "host" else identity["peerUuid"]
    observer_entity = action["actorEntity"] if role == "host" else action["observerEntity"]
    required = {"owner": identity["hostUuid"], "observerUuid": observer, "observerEntity": str(observer_entity),
                "pairedRole": role, "observerCoverage": "false" if role == "host" else "true",
                "sourceHead": identity["sourceHead"], "runIdentity": identity["runIdentity"], "acceptedReceiptSha256": accepted_sha}
    require(all(observation.get(key) == value for key, value in required.items()), "Counter render provenance or accepted receipt changed")
    read_sequence = decimal(observation.get("acceptedReadSequence"), "counter acceptance-read sequence", True)
    source_sequence = decimal(observation.get("sourceFrameSequence"), "counter source-frame sequence", True)
    require(read_sequence < source_sequence < sequences[0], "Counter acceptance must precede actual source extraction")
    age = finite(values.get("actualSourceAge"), "counter witness source age")
    require(counter_phase(action["move"], age) == phase
            and abs(finite(observation.get("actualSourceAge"), "counter observed age") - age) <= .0001,
            "Counter capture is outside its actual post-HitStop source window")
    texture = observation.get("connectedSkinTexture")
    require(isinstance(texture, str) and re.fullmatch(r"[a-z0-9_.-]+:[a-z0-9/._-]+", texture)
            and str(observation.get("connectedSkinModel", "")).lower() == skin, "Missing original connected counter skin")
    material = 0
    if role == "peer" or action["mode"] == "articulated":
        material = decimal(observation.get("originalSkinMaterialIdentity"), "counter original skin material", True)
        require(observation.get("submittedSkinMaterialIdentity") == str(material), "Counter submitted material changed")
    if role == "host":
        require(all(observation.get(key) == "true" for key in ("ordinaryHandAdmission", "handEquipKnown", "handSameItem"))
                and observation.get("handEquipping") == "false", "Counter lacks eligible ordinary native hand")
    else:
        require(observation.get("worldHandEligible") == "true", "Counter lacks ordinary world item")
    passes = copy.get("passes")
    required_kinds = ({"body_submit", "body", "world_item"} if role == "peer" else
                      {"view_submit", "view_deferred", "view_item"} if action["mode"] == "articulated" else
                      {"classic_transform", "native_item"})
    require(isinstance(passes, list) and len(passes) == len(required_kinds) and all(isinstance(p, dict) for p in passes)
            and {p.get("kind") for p in passes} == required_kinds, "Counter requires exact geometry and item pass union")
    geometry_shapes = {"classic_transform": ("observed_classic_before_after", 16), "native_item": ("actual_native_item_matrix", 16),
                       "view_submit": ("view_root_matrix", 16), "view_deferred": ("deferred_view_locals", 180),
                       "view_item": ("actual_socket_item_matrix", 16), "body_submit": ("world_root_identity", 17),
                       "body": ("deferred_body_locals_and_retained_outer_root", 197 if action["mode"] == "articulated" else 71),
                       "world_item": ("actual_world_socket_item_matrix", 16)}
    first = None
    for render in passes:
        binding, geometry = render.get("binding", {}), render.get("geometry", {})
        require(isinstance(binding, dict) and isinstance(geometry, dict), "Missing counter binding/geometry objects")
        palette = binding.get("palette", {})
        require(isinstance(palette, dict), "Missing counter source palette")
        articulated = action["mode"] == "articulated"
        require(render.get("segmented") is articulated and render.get("rigid") is (not articulated), "Counter backend changed")
        counter_frame(palette.get("classic"), "classic", action, phase)
        if articulated:
            counter_frame(palette.get("articulated"), "articulated", action, phase)
        else:
            require(palette.get("articulated") is None, "Classic counter retained a stale articulated frame")
        require(binding.get("matched") is True and type(binding.get("owner")) is int and binding["owner"] == action["actorEntity"]
                and binding.get("uuid") == identity["hostUuid"] and binding.get("hand") == action["hand"]
                and str(binding.get("skin", "")).lower() == skin and binding.get("texture") == texture
                and type(binding.get("skinMaterial")) is int and binding["skinMaterial"] == material
                and type(palette.get("activation")) is int and palette["activation"] == action["acceptedTick"]
                and type(palette.get("move")) is int and palette["move"] == action["move"] and palette.get("phase") == phase
                and type(palette.get("age")) in (int, float) and abs(finite(palette["age"], "counter palette age") - age) <= .0001,
                "Counter pass changed source action, appearance or actual phase")
        stable = {key: binding.get(key) for key in ("source", "state", "model", "skinMaterial", "palette", "texture", "skin", "hand")}
        if role == "peer":
            stable["item"] = binding.get("item")
        if first is None:
            first = stable
        require(stable == first and all(type(binding.get(key)) is int and binding[key] > 0 for key in ("source", "state", "model")),
                "Counter pass identity changed after extraction")
        item_expected = role == "peer" or "item" in render["kind"]
        require(type(binding.get("item")) is int and (binding["item"] > 0 if item_expected else binding["item"] == 0),
                "Counter held-item identity changed")
        operation, count = geometry_shapes[render["kind"]]
        expected_values, actual_values = geometry.get("expected"), geometry.get("actual")
        require(geometry.get("operation") == operation and isinstance(expected_values, list) and isinstance(actual_values, list)
                and len(expected_values) == len(actual_values) == count
                and all(type(x) in (int, float) for x in expected_values + actual_values), "Counter lacks complete actual geometry")
        error = max(abs(finite(a, "counter expected geometry") - finite(b, "counter actual geometry")) for a, b in zip(expected_values, actual_values))
        require(geometry.get("matched") is True and error <= .00025 and type(geometry.get("maxError")) in (int, float)
                and abs(finite(geometry["maxError"], "counter geometry error") - error) <= .000001,
                "Actual counter geometry does not match its independent source oracle")
    draws = copy.get("draws")
    require(isinstance(draws, list) and len(draws) == 1 and isinstance(draws[0], dict), "Counter requires exactly one actual deep item draw")
    draw = draws[0]
    item_pass = next(p for p in passes if p["kind"].endswith("item"))
    require(draw.get("binding") == item_pass["binding"] and type(draw.get("quads")) is int and 0 < draw["quads"] <= 512,
            "Counter deep item draw changed original source or has no geometry")
    expected_context = ("FIRST_PERSON_" if role == "host" else "THIRD_PERSON_") + action["hand"] + "_HAND"
    require(draw.get("displayContext") == expected_context and draw.get("anchorKind") == "stock_diamond_hilt",
            "Counter deep item draw changed hand, camera or actual hilt basis")
    side = -1 if action["hand"] == "LEFT" else 1
    remote = role == "peer"
    display = {"displayRotation": [0., -90. * side, (55. if remote else 25.) * side],
               "displayTranslation": [0., 4./16, .5/16] if remote else [1.13/16, 3.2/16, 1.13/16],
               "displayScale": [.85 if remote else .68] * 3,
               "displayLocal": [1.,0.,0.,0.,0.,1.,0.,0.,0.,0.,1.,0.,0.,0.,0.,1.]}
    for key, wanted in display.items():
        actual = draw.get(key)
        require(isinstance(actual, list) and len(actual) == len(wanted)
                and all(type(x) in (int, float) for x in actual)
                and max(abs(finite(x, "counter native display") - y) for x, y in zip(actual, wanted)) <= .00025,
                "Counter original diamond display changed")
    geometry = draw.get("geometry", {})
    wanted, actual = geometry.get("expected"), geometry.get("actual")
    require(geometry.get("operation") == "actual_displayed_item_matrix" and isinstance(wanted, list) and isinstance(actual, list)
            and len(wanted) == len(actual) == 16 and all(type(x) in (int,float) for x in wanted + actual),
            "Counter deep item draw lacks actual displayed matrix")
    error = max(abs(finite(x, "counter deep matrix")-finite(y, "counter deep matrix")) for x,y in zip(wanted,actual))
    require(geometry.get("matched") is True and error <= .00025
            and abs(finite(geometry.get("maxError"), "counter deep error")-error) <= .000001, "Counter displayed item matrix differs")
    hilt = []
    for key in ("anchorExpected", "anchorActual"):
        points = draw.get(key)
        require(isinstance(points, list) and len(points) == 3 and all(type(x) in (int,float) for x in points), "Counter displayed hilt missing")
        hilt.append([finite(x, "counter displayed hilt") for x in points])
    require(max(abs(x-y) for x,y in zip(*hilt)) <= .00025, "Counter actual displayed hilt differs")
    positions = draw.get("emittedQuadPositions")
    require(isinstance(positions, list) and len(positions) == draw["quads"] * 12
            and all(type(x) in (int,float) for x in positions), "Counter emitted item vertices missing")
    import struct
    try:
        raw_positions = b"".join(struct.pack(">f", finite(x, "counter emitted item vertex")) for x in positions)
    except (OverflowError, struct.error) as error:
        raise ValueError("Counter emitted item vertex overflows") from error
    require(hashlib.sha256(raw_positions).hexdigest() == draw.get("emittedQuadSha256"), "Counter emitted item vertices changed")
    require(all(type(draw.get(k)) is int and draw[k] > 0 for k in ("stackIdentity", "collectorIdentity", "entryStackIdentity", "entryCollectorIdentity"))
            and draw["stackIdentity"] == draw["entryStackIdentity"] and draw["collectorIdentity"] == draw["entryCollectorIdentity"],
            "Counter deep item draw lost original stack or collector identity")
    image = record.get("image", {})
    require(isinstance(image, dict), "Missing counter image object")
    require(image.get("relativeImagePath") == values.get("pngRelativePath"), "Counter PNG path changed")
    png = counter_evidence_file(game, values.get("pngRelativePath"), 64 * 1024 * 1024, "counter PNG")
    data = png.read_bytes()
    require(type(image.get("pngBytes")) is int and len(data) == image["pngBytes"]
            and hashlib.sha256(data).hexdigest() == image.get("pngSha256") == values.get("pngSha256"), "Counter PNG bytes changed")
    import importlib.util
    path = Path(__file__).resolve().parents[1] / "verify_articulated_render_receipts.py"
    spec = importlib.util.spec_from_file_location("wildercord_counter_image_decoder", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    pixels = module.image_evidence(data)
    require(pixels == image.get("decodedPixels") == record.get("callbackPixels")
            and pixels["sha256"] == values.get("callbackPixelSha256"), "Counter callback pixels differ from decoded unchanged PNG")
    require(type(copy.get("width")) is int and type(copy.get("height")) is int
            and copy["width"] == pixels["width"] and copy["height"] == pixels["height"], "Counter copy target changed")
    return record


def counter_property(value):
    """Decode the escapes emitted by Java Properties.store, never accept dangling escapes."""
    result, index = [], 0
    escapes = {"t": "\t", "n": "\n", "r": "\r", "f": "\f", "\\": "\\", ":": ":", "=": "=", " ": " ", "#": "#", "!": "!"}
    while index < len(value):
        char = value[index]; index += 1
        if char == "\\":
            require(index < len(value), "Dangling counter property escape")
            char = value[index]; index += 1
            if char == "u":
                code = value[index:index + 4]
                require(re.fullmatch(r"[0-9a-fA-F]{4}", code), "Malformed counter Unicode escape")
                char = chr(int(code, 16)); index += 4
            else:
                require(char in escapes, "Unknown counter property escape")
                char = escapes[char]
        result.append(char)
    return "".join(result)


def counter_admission(admission, accepted, action, identity, index, armed_sha, ready_tick, admission_sha):
    common_fields = set(IDENTITY) | {"role", "pid", "case", "actorEntity", "observerEntity", "actorUuid", "observerUuid",
                                     "move", "windup", "recovery", "mode", "hand", "skin", "cameraNative"}
    require(set(admission) == common_fields | set(COUNTER_ADMISSION_FIELDS), "Missing or extra counter admission properties")
    accepted_fields = {"admissionReceiptSha256", "acceptedTick", "caughtTick", "caughtAttackerUuid", "paid", "payments", "restUntil",
                       "clockReadySha256", "clockRendezvousTick", "armedSha256"}
    require(set(accepted) == set(admission) | accepted_fields, "Missing or extra accepted admission properties")
    require(all(accepted.get(key) == value for key, value in admission.items())
            and accepted.get("admissionReceiptSha256") == admission_sha, "Accepted contradicts immutable counter admission receipt")
    generation = identity["nonce"] + f":case-{index:02d}"
    expected = {"SchemaVersion": "1", "Generation": generation, "PeerArmedSha256": armed_sha,
                "Art": "unmoved" if action["move"] == 24 else "null_parry", "Check": "ACCEPTED", "Refusal": "NONE",
                "Busy": "false", "ExciseBlocking": "false", "Performed": "true", "OriginalBody": "true",
                "ObserverFailure": "NONE", "NativeFailure": "NONE", "PendingAtEntry": "NONE", "ReturnCommitted": "true", "Backlash": "false"}
    expected["Trace"] = f"generation={generation}, sends=1, requests=1, check=ACCEPTED, refusal=NONE, payments=1, sources=1, returns=1, committed=true"
    for side in ("Server", "Client"):
        expected.update({side + "BodyUuid": identity["hostUuid"], side + "BodyEntity": str(action["actorEntity"]), side + "Level": "minecraft:overworld"})
        connection = decimal(admission.get("admission" + side + "ConnectionIdentity"), "counter native connection identity", True)
        require(connection <= 2**32 - 1, "Invalid counter native connection identity")
    require(all(admission.get("admission" + key) == value for key, value in expected.items()), "Counter native admission failed or changed original generation/body/session")
    marks = admission["admissionSentMarks"]
    match = re.fullmatch(r"\[([1-9][0-9]{0,2})\]", marks)
    require(match and 0 < int(match[1]) < 128 and int(match[1]) & 32
            and admission["admissionRequestMarks"] == marks, "Counter admission request differs from original earned-counter send")
    # Native performer consumes server-observed marks; they need not equal the client hint bit-for-bit.
    performed = re.fullmatch(r"\[([1-9][0-9]{0,2})\]", admission["admissionPerformMarks"])
    require(performed and 0 < int(performed[1]) < 128 and int(performed[1]) & 32, "Counter native performer lacks the actual earned-counter mark")
    paid_tick = action["acceptedTick"]
    ticks = {key: decimal(admission["admission" + key], "counter admission " + key) for key in ("ArmedTick", "SentTick", "RequestTick", "ReturnTick", "PaidTick", "Rest")}
    require(ready_tick <= ticks["ArmedTick"] <= ticks["RequestTick"] == ticks["ReturnTick"] == ticks["PaidTick"] == paid_tick
            and decimal(accepted["caughtTick"], "counter original caught tick") <= ticks["ArmedTick"]
            and ticks["Rest"] > paid_tick and admission["admissionRest"] == accepted["restUntil"],
            "Counter admission must retain original pre-input readiness and synchronous native payment/return")
    paid, cost, before, after, left = (finite(admission["admission" + key], "counter admission " + key) for key in ("Paid", "Cost", "AuraBefore", "AuraAfter", "SpendLeft"))
    require(paid > 0 and cost > 0 and before >= cost and after >= 0 and abs(cost - paid) < .0001
            and abs(before - after - cost) < .001 and abs(left - after) < .001
            and abs(paid - finite(accepted["paid"], "counter actual payment")) < .0001,
            "Counter admission changed the actual native full-price payment")
    action_id = decimal(admission["admissionActionIdentity"], "counter pending action identity", True)
    require(action_id <= 2**32 - 1 and admission["admissionActionIdentity"] == admission["admissionSourceActionIdentity"] == admission["admissionReturnActionIdentity"],
            "Counter admission lost the original paid pending token at source/return")
    source = {"Entity": action["actorEntity"], "Move": action["move"], "StartTick": paid_tick, "Windup": action["windup"], "Recovery": action["recovery"]}
    require(all(admission["admissionSource" + key] == str(value) for key, value in source.items()), "Counter admission broadcast differs from original paid action")
    for key in ("Yaw", "Pitch"):
        finite(admission["admissionSource" + key], "counter admission source " + key)


def validate_counter_witnesses(ipc, identity, pids, selected=None):
    selected = selected or selection()
    require(identity["hostUuid"] == selected["profiles"]["host"][1] and identity["peerUuid"] == PROFILES["peer"][1],
            "Counter witness roles differ from the fixed selected profiles")
    # Reopen after both owned JVMs exit. Even malformed, linked, foreign or post-terminal rejection artifacts forbid success.
    require(not any(ipc.glob("*counter-render-failure*")), "Sticky counter render rejection artifact is present")
    admission_files = {f"case-{index:02d}-admission.properties" for index in range(46, 54)}
    require({path.name for path in ipc.iterdir() if "admission" in path.name.lower()} == admission_files,
            "Missing or extra counter admission artifacts")
    used = set()
    received_sources = {"host": set(), "peer": set()}
    previous_end = -1
    for index, case in enumerate(COUNTER_CASES, 46):
        def read(suffix, role):
            filename = f"case-{index:02d}-{suffix}.properties"
            if suffix.endswith(("-observed", "-rendered")):
                filename = role + "-" + filename
            values = {key: counter_property(value) for key, value in read_properties(ipc / filename).items()}
            require((ipc / filename).stat().st_nlink == 1, "Hard-linked counter witness is forbidden")
            for key, value in {**identity, "role": role, "pid": str(pids[role]), "case": case}.items():
                require(values.get(key) == value, filename + " has mismatched " + key)
            return values
        prepare, armed, admission, accepted = read("prepare", "host"), read("armed", "peer"), read("admission", "host"), read("accepted", "host")
        action = {**counter_case(case), "actorEntity": decimal(prepare.get("actorEntity"), "counter actor entity", True),
                  "observerEntity": decimal(prepare.get("observerEntity"), "counter observer entity", True),
                  "acceptedTick": decimal(accepted.get("acceptedTick"), "counter accepted tick")}
        require(action["acceptedTick"] > previous_end, "Counter admission replays or overlaps a previous case action")
        previous_end = action["acceptedTick"] + action["windup"] + action["recovery"]
        require(action["actorEntity"] != action["observerEntity"], "Counter source and observer must be distinct connected bodies")
        common = {key: str(value) for key, value in action.items() if key != "acceptedTick"}
        common.update(actorUuid=identity["hostUuid"], observerUuid=identity["peerUuid"])
        skin = prepare.get("skin", "").lower()
        require(skin in ("wide", "slim") and (not selected.get("expectedSkin") or skin == selected["expectedSkin"]),
                "Counter original skin differs from fixed selected profile")
        require(armed.get("skin", "").lower() == skin and prepare.get("cameraNative") == armed.get("cameraNative") == "true",
                "Counter handshake needs unchanged original skin and native cameras")
        caught = decimal(accepted.get("caughtTick"), "counter caught tick")
        attacker = accepted.get("caughtAttackerUuid")
        require(isinstance(attacker, str) and re.fullmatch(r"[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}", attacker)
                and attacker not in (identity["hostUuid"], identity["peerUuid"]), "Counter must bind the concrete hostile caught attacker")
        require(0 <= action["acceptedTick"] - caught <= 16 and accepted.get("payments") == "1"
                and finite(accepted.get("paid"), "counter paid amount") > 0
                and accepted.get("armedSha256") == digest(ipc / f"case-{index:02d}-armed.properties"),
                "Counter requires one real paid acceptance after peer arming")
        initial = read("clock-initial", "peer")
        rendezvous = read("clock-rendezvous", "host")
        ack = read("clock-ack", "peer")
        clock_ready = read("clock-ready", "host")
        target = decimal(rendezvous.get("clockRendezvousTick"), "counter clock rendezvous")
        initial_tick = decimal(initial.get("clockClientTick"), "counter initial client clock")
        initial_server = decimal(rendezvous.get("clockInitialServerTick"), "counter initial server clock")
        require(target == max(initial_tick, initial_server) and action["acceptedTick"] >= target,
                "Counter acceptance must follow a natural pre-input rendezvous")
        initial_sha = digest(ipc / f"case-{index:02d}-clock-initial.properties")
        rendezvous_sha = digest(ipc / f"case-{index:02d}-clock-rendezvous.properties")
        ack_sha = digest(ipc / f"case-{index:02d}-clock-ack.properties")
        ready_sha = digest(ipc / f"case-{index:02d}-clock-ready.properties")
        require(all(value.get("clockRendezvousTick") == str(target) and value.get("clockInitialSha256") == initial_sha
                    and value.get("clockInitialServerTick") == str(initial_server) for value in (rendezvous, ack, clock_ready))
                and ack.get("clockRendezvousSha256") == clock_ready.get("clockRendezvousSha256") == rendezvous_sha
                and rendezvous.get("clockServerTick") == clock_ready.get("clockServerTick") == str(target)
                and ack.get("clockClientTick") == clock_ready.get("clockClientTick") == str(target)
                and clock_ready.get("clockAckSha256") == ack_sha
                and armed.get("clockReadySha256") == accepted.get("clockReadySha256") == ready_sha
                and accepted.get("clockRendezvousTick") == str(target),
                "Counter pre-input clocks or immutable clock receipt chain changed")
        accepted_sha = digest(ipc / f"case-{index:02d}-accepted.properties")
        admission_sha = digest(ipc / f"case-{index:02d}-admission.properties")
        counter_admission(admission, accepted, action, identity, index, digest(ipc / f"case-{index:02d}-armed.properties"), target, admission_sha)
        require(admission["skin"].lower() == skin and admission["cameraNative"] == "true", "Counter admission changed native appearance/camera")
        outcomes = [read("ready", "host"), read("seen", "peer"), read("passed", "host")]
        outcome = outcomes[0]
        outcome_fields = ("releaseTick", "payments", "completions", "restUntil", "counterTargetUuid", "counterTargetEntity",
                          "counterTargetHealthBefore", "counterTargetHealth", "directPrimaryHits", "primaryHitTick")
        require(all(all(value.get(key) == outcome.get(key) for key in outcome_fields) for value in outcomes[1:]),
                "Counter owner, actual peer and final outcome disagree")
        target_entity = decimal(outcome.get("counterTargetEntity"), "counter caught target entity", True)
        require(target_entity not in (action["actorEntity"], action["observerEntity"])
                and outcome.get("counterTargetUuid") == attacker
                and decimal(outcome.get("releaseTick"), "counter release tick") == action["acceptedTick"] + action["windup"]
                and outcome.get("payments") == outcome.get("completions") == outcome.get("directPrimaryHits") == "1"
                and outcome.get("restUntil") == admission["admissionRest"]
                and outcome.get("primaryHitTick") == outcome.get("releaseTick")
                and finite(outcome.get("counterTargetHealthBefore"), "counter target original health") == 200
                and 0 < finite(outcome.get("counterTargetHealth"), "counter target actual health") < 200,
                "Counter outcome must preserve one original paid release and actual surviving damaged target")
        all_values = [prepare, armed, admission, accepted, initial, rendezvous, ack, clock_ready, *outcomes]
        source_identities, source_connections = {}, {}
        for role in ("host", "peer"):
            for phase in COUNTER_PHASES:
                rendered = read(phase + "-rendered", role)
                require(rendered.get("acceptedTick") == str(action["acceptedTick"]) and rendered.get("phase") == phase
                        and rendered.get("renderedBeforeReadback") == "true", "Counter phase requires its same-action native render barrier")
                values = read(phase + "-observed", role)
                all_values.extend((rendered, values))
                require(values.get("acceptedTick") == str(action["acceptedTick"]) and values.get("acceptedReceiptSha256") == accepted_sha
                        and values.get("phase") == phase and values.get("view") == ("fp" if role == "host" else "remote")
                        and values.get("skin", "").lower() == skin, "Counter observed phase changed accepted action or appearance")
                require(values.get("scopeCleanupVerified") == values.get("acceptanceObservedBeforeSource") == "true"
                        and values.get("pixelQualityReviewed") == "false", "Counter observed phase lacks cleanup or overstates review")
                for field in ("receiptRelativePath", "pngRelativePath"):
                    key = (role, field, str(Path(values.get(field, ""))))
                    require(key not in used, "Counter phases cannot reuse the same evidence file")
                    used.add(key)
                record = counter_image(ipc.parent / role, role, values, identity, action, accepted_sha)
                observation = record["copy"]["observations"]
                admission_binding = {"admissionReceiptSha256": admission_sha, "admissionGeneration": admission["admissionGeneration"]}
                require(all(values.get(key) == value and observation.get(key) == value for key, value in admission_binding.items()),
                        "Counter owner/peer phase lost the immutable native admission binding")
                require(all(observation.get("receivedSource" + key) == admission["admissionSource" + key] for key in COUNTER_SOURCE_FIELDS)
                        and observation.get("receivedOwnerUuid") == identity["hostUuid"]
                        and observation.get("receivedOwnerEntity") == str(action["actorEntity"])
                        and observation.get("receivedLevel") == admission["admissionServerLevel"],
                        "Counter owner/peer actual received full source differs from native admission")
                connection = decimal(observation.get("receivedConnectionIdentity"), "counter actual receiving connection identity", True)
                require(connection <= 2**32 - 1 and (role != "host" or str(connection) == admission["admissionClientConnectionIdentity"]),
                        "Counter owner received source on a different original connection")
                if role == "host":
                    # These are the original owner's clock readings; never compare the client's send clock to the server clock.
                    require(decimal(observation.get("tick"), "counter owner capture tick") >= decimal(admission["admissionSentTick"], "counter owner send tick"),
                            "Counter owner capture predates its original client send")
                require(role not in source_connections or source_connections[role] == connection,
                        "Counter receiving connection changed between phases")
                source_connections[role] = connection
                source_id = decimal(record["copy"]["observations"].get("acceptedSourceIdentity"), "first native received source identity", True)
                require(source_id < decimal(record["copy"]["observations"].get("acceptedReadSequence"), "source acceptance read", True),
                        "First native source must precede capture admission")
                require(source_id not in received_sources[role], "Native received source identity reused by another case")
                require(role not in source_identities or source_identities[role] == source_id, "Native received source object replaced between phases")
                source_identities[role] = source_id
        for role, source_id in source_identities.items():
            received_sources[role].add(source_id)
        for values in all_values:
            require(all(values.get(key) == value for key, value in common.items()), "Counter handshake or evidence changed actual actors/action")


def validate_counter_profile_union(run_paths):
    """Reopen both complete native runs, never count repeated variants as extra mechanics cases."""
    require(isinstance(run_paths, (list, tuple)) and len(run_paths) == 2, "Counter union requires exactly wide and slim runs")
    profiles, nonces, runs, provenance = set(), set(), set(), None
    for run_path in run_paths:
        base = Path(run_path).resolve()
        path = base / "result.json"
        require(path.is_file() and not path.is_symlink() and path.stat().st_size <= 2 * 1024 * 1024, "Missing bounded counter result")
        report = counter_json(path)
        profile = report.get("profile")
        require(profile in COUNTER_PROFILES and profile not in profiles, "Counter union requires distinct fixed wide and slim profiles")
        selected = selection(SUITE, profile)
        require(report.get("suite") == SUITE and report.get("status") == "passed" and report.get("cases") == list(CASES)
                and report.get("expectedSkin") == selected["expectedSkin"], "Counter union needs every exact 54-case native run")
        require(report.get("counterOwnerPeerGeometryVerified") is True and report.get("phaseBasis") == COUNTER_PHASE_BASIS
                and report.get("counterPhaseCaptures") == 64 and report.get("pixelQualityReviewed") is False
                and report.get("serverReleaseFrameCorrespondenceVerified") is False, "Counter union requires accurately scoped geometric proof")
        require(type(report.get("timeoutSeconds")) is int and report["timeoutSeconds"] == MAX_TIMEOUT
                and report.get("heapMiBPerJvm") == 2048 and not report.get("cleanupErrors")
                and 0 <= finite(report.get("seconds"), "counter elapsed time") <= report["timeoutSeconds"], "Counter union exceeded resource limits")
        identity = {key: report.get(key) for key in IDENTITY}
        require(all(isinstance(value, str) for value in identity.values()) and re.fullmatch(r"[a-f0-9]{40}", identity["sourceHead"])
                and identity["checkoutSha"] == identity["sourceHead"] and re.fullmatch(r"[a-f0-9]{64}", identity["descriptorSha256"])
                and (not identity["prHeadSha"] or re.fullmatch(r"[a-f0-9]{40}", identity["prHeadSha"])),
                "Counter union lacks exact source provenance")
        require(str(uuid.UUID(identity["nonce"])) == identity["nonce"] and identity["nonce"] not in nonces
                and identity["runIdentity"] and identity["runIdentity"] not in runs, "Counter union reused a launch identity")
        source = tuple(identity[key] for key in ("sourceHead", "checkoutSha", "prHeadSha", "descriptorSha256"))
        if provenance is None:
            provenance = source
        require(source == provenance, "Counter variants must share the exact source and descriptor")
        processes = report.get("processes")
        require(isinstance(processes, dict) and set(processes) == {"host", "peer"}
                and all(isinstance(p, dict) and type(p.get("pid")) is int and p["pid"] > 0
                        and type(p.get("exit")) is int and p["exit"] == 0 for p in processes.values()), "Counter union requires two successful owned JVMs")
        jobs = [(role, types.SimpleNamespace(pid=value["pid"])) for role, value in processes.items()]
        validate_witnesses(base / "ipc", identity, list(CASES), jobs, selected)
        profiles.add(profile); nonces.add(identity["nonce"]); runs.add(identity["runIdentity"])
    require(profiles == set(COUNTER_PROFILES), "Counter union requires both original skin widths")
    return {"profiles": list(COUNTER_PROFILES), "sourceHead": provenance[0], "cases": list(CASES),
            "counterPhaseCaptures": 128, "counterOwnerPeerGeometryVerified": True,
            "phaseBasis": COUNTER_PHASE_BASIS, "pixelQualityReviewed": False,
            "serverReleaseFrameCorrespondenceVerified": False}


def life_count(values, key):
    value = values.get(key)
    require(isinstance(value, str) and re.fullmatch(r"0|[1-9][0-9]{0,2}", value)
            and int(value) <= 128, "Malformed bounded Life measurement: " + key)
    return int(value)


def validate_life_witnesses(ipc, identity, pids):
    """New phases only: never relax or replace any of the original 42 terminal case identities."""
    for index, case in enumerate(LIFE_CASES, 42):
        phases = {}
        for phase, role in (("prepare", "host"), ("armed", "peer"), ("captured", "peer"),
                            ("ready", "host"), ("seen", "peer"), ("passed", "host")):
            filename = f"case-{index:02d}-{phase}.properties"
            values = read_properties(ipc / filename)
            for key, value in {**identity, "role": role, "pid": str(pids[role]), "case": case}.items():
                require(values.get(key) == value, filename + " has mismatched " + key)
            phases[phase] = values
        prepare, armed, captured, ready, seen = (phases[name] for name in ("prepare", "armed", "captured", "ready", "seen"))
        for field in ("lifeOwnerEntity", "lifeViewerEntity"):
            require(re.fullmatch(r"[1-9][0-9]{0,9}", prepare.get(field, "")), "Malformed Life body identity: " + field)
        require(prepare["lifeOwnerEntity"] != prepare["lifeViewerEntity"], "Life source and viewer need distinct connected bodies")
        for values in (prepare, armed, captured):
            for key, value in {"lifeSource": identity["hostUuid"], "lifeViewerUuid": identity["peerUuid"],
                               "lifeOwnerEntity": prepare["lifeOwnerEntity"], "lifeViewerEntity": prepare["lifeViewerEntity"]}.items():
                require(values.get(key) == value, "Life handshake has mismatched " + key)
        require(armed.get("lifeCameraNative") == captured.get("lifeCameraNative") == "true"
                and captured.get("lifeCapture") == "resident", "Life capture requires the armed native camera and resident particles")
        for values in (ready, seen):
            require(values.get("actorEntity") == prepare["lifeOwnerEntity"]
                    and values.get("recipientEntity") == prepare["lifeViewerEntity"], "Life ready/seen changed an actual body identity")
        require(ready.get("caseLife") == case and ready.get("lifeMoment") == "APPLY"
                and ready.get("lifeSource") == ready.get("lifeOwnerSource") == seen.get("lifePeerSource") == identity["hostUuid"],
                "Life measurements have malformed role/source or original moment")
        require(ready.get("lifeOwnerCameraNative") == seen.get("lifePeerCameraNative") == "true", "Life measurements require native cameras")
        full, minimal = life_count(ready, "lifeFullPieces"), life_count(ready, "lifeMinimalPieces")
        require(0 < minimal <= full, "Life recipes must be nonempty and bounded")
        first = case == LIFE_CASES[0]
        own, unique, extracted = (life_count(ready, "lifeOwner" + suffix) for suffix in ("Pieces", "Unique", "Extracted"))
        require(own == (0 if first else full) and unique == own
                and (extracted == 0 if first else 0 < extracted <= own), "Life owner sample contradicts native camera clearance or duplicates the recipe")
        peer, unique, extracted = (life_count(seen, "lifePeer" + suffix) for suffix in ("Pieces", "Unique", "Extracted"))
        require(peer == minimal and unique == peer and 0 < extracted <= peer, "Independent Life peer sample must contain one complete Minimal recipe")
        for values, prefix in ((ready, "lifeOwner"), (seen, "lifePeer")):
            time_count, reduced = life_count(values, prefix + "Time"), life_count(values, prefix + "ReducedSamples")
            if case == LIFE_CASES[-1]:
                require(time_count > 0 and reduced > 0, "Both Life roles must witness Time support and reduced-flash samples")


@contextmanager
def supervisor_lock(root):
    path = inside(root / ".gradle/two-client-supervisor.lock", [root])
    path.parent.mkdir(exist_ok=True)
    with path.open("a+b") as lock:
        if os.name == "nt":
            import msvcrt
            lock.seek(0)
            if not lock.read(1):
                lock.write(b"0"); lock.flush()
            lock.seek(0)
            try:
                msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1)
            except OSError as error:
                raise RuntimeError("Another supervisor owns this checkout") from error
        else:
            import fcntl
            try:
                fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            except BlockingIOError as error:
                raise RuntimeError("Another supervisor owns this checkout") from error
        try:
            yield
        finally:
            if os.name == "nt":
                lock.seek(0); msvcrt.locking(lock.fileno(), msvcrt.LK_UNLCK, 1)
            else:
                fcntl.flock(lock, fcntl.LOCK_UN)


def stop_owned(jobs):
    errors = []
    for role, process in jobs:
        if process.poll() is None:
            try:
                process.terminate()
            except OSError as error:
                errors.append(role + " terminate: " + str(error))
    for role, process in jobs:
        try:
            process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            try:
                process.kill()
                process.wait(timeout=5)
            except (OSError, subprocess.TimeoutExpired) as error:
                errors.append(role + " kill/wait: " + str(error))
        except OSError as error:
            errors.append(role + " wait: " + str(error))
    return errors


def accepted_eula(path, root):
    if path:
        path = inside(path, [root])
        content = path.read_bytes()
        require(any(line.strip().lower() == "eula=true" for line in content.decode("utf-8-sig").splitlines()), "Source is not a previously accepted EULA")
        return content, "previously-accepted-file"
    require(os.environ.get("GITHUB_ACTIONS") == "true" and os.environ.get("CI_MINECRAFT_EULA_ACCEPTED") == "true",
            "Local runs need an accepted EULA file; disposable CI requires both explicit acceptance flags")
    return b"# Owner-authorized disposable GitHub Actions run\neula=true\n", "owner-authorized-disposable-ci"


def environment(launch, game, receipt_nonce=None, counter_nonce=None):
    env = os.environ.copy()
    for key in list(env):
        if key in {"JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "CLASSPATH", "LD_PRELOAD", "LD_LIBRARY_PATH"} or key.startswith(("WILDERCORD_", "DYLD_")):
            env.pop(key, None)
    env.update(launch["environment"])
    env.update({key: str(game / "tmp") for key in ("TMPDIR", "TEMP", "TMP")})
    env.update({"XDG_CACHE_HOME": str(game / "cache"), "MESA_SHADER_CACHE_DIR": str(game / "cache/mesa")})
    if receipt_nonce is not None:
        require(str(uuid.UUID(receipt_nonce)) == receipt_nonce, "Receipt nonce must be canonical")
        env["WILDERCORD_MOON_RECEIPT_NONCE"] = receipt_nonce
    if counter_nonce is not None:
        require(str(uuid.UUID(counter_nonce)) == counter_nonce, "Counter receipt nonce must be canonical")
        env["WILDERCORD_COUNTER_RECEIPT_NONCE"] = counter_nonce
    return env


def run(options, root=ROOT, *, already_locked=False):
    root = root.resolve()
    selected = selection(options.suite, options.profile, getattr(options, "case", None), getattr(options, "observer_angle", None))
    require(1 <= options.timeout <= selected["maxTimeoutSeconds"],
            "Deadline must be between 1 and " + str(selected["maxTimeoutSeconds"]) + " seconds for " + selected["suite"])
    launch_path, base = output_path(options.launch, root), output_path(options.output, root)
    require(not base.exists(), "Output must be fresh")
    ignored_output(base, root)
    # The fixed matrix runner retains this same lock across exports and all six
    # sequential groups. This internal parameter has no command-line equivalent.
    with nullcontext() if already_locked else supervisor_lock(root):
        base.mkdir(parents=True, exist_ok=False)
        jobs, logs, handlers = [], [], {}
        started = time.monotonic()
        report = {"status": "failed", "stage": "preflight", "suite": selected["suite"], "profile": selected["profile"], "timeoutSeconds": options.timeout,
                  "heapMiBPerJvm": 2048, "launcherPid": os.getpid(), "processes": {}}
        try:
            contract = load_contract(root, selected)
            eula, report["eulaSource"] = accepted_eula(options.accepted_eula, root)
            require(launch_path.stat().st_size <= 2 * 1024 * 1024, "Oversized launch export")
            launch = json.loads(launch_path.read_text(encoding="utf-8-sig"))
            report["launchSha256"] = validate_launch(launch, launch_path, root, selected)
            for item in launch["classpath"]:
                runtime = Path(item).resolve()
                require(base != runtime and base not in runtime.parents and runtime not in base.parents, "Game output overlaps runtime inputs")
            nonce = str(uuid.uuid4())
            ci = {key: os.environ.get(key) for key in ("GITHUB_RUN_ID", "GITHUB_RUN_ATTEMPT", "GITHUB_JOB")}
            for key in ("GITHUB_RUN_ID", "GITHUB_RUN_ATTEMPT"):
                require(ci[key] is None or re.fullmatch(r"[0-9]+", ci[key]), "Malformed CI run identity")
            identity = {"nonce": nonce, "suite": selected["suite"], "sourceHead": launch["sourceHead"], "checkoutSha": launch["checkoutSha"],
                        "prHeadSha": launch["prHeadSha"], "descriptorSha256": launch["descriptorSha256"],
                        "hostUuid": selected["profiles"]["host"][1], "peerUuid": selected["profiles"]["peer"][1],
                        "runIdentity": "-".join([ci["GITHUB_RUN_ID"] or "local", ci["GITHUB_RUN_ATTEMPT"] or "0", nonce])}
            if selected["suite"] == MOON_SUITE:
                identity.update({key: selected[key] for key in ("case", "expectedSkin", "observerAngle")})
            report.update({**identity, "cases": contract["cases"], "ci": ci, "stage": "clients"})
            if selected["suite"] == SUITE and selected.get("expectedSkin"):
                report["expectedSkin"] = selected["expectedSkin"]
            ipc = base / "ipc"; ipc.mkdir()
            (base / "launch-proof.json").write_text(json.dumps({**report, "status": "started"}, indent=2) + "\n")
            def interrupted(signum, frame):
                raise InterruptedError("Supervisor received signal " + str(signum))
            for signum in (signal.SIGINT, signal.SIGTERM):
                handlers[signum] = signal.signal(signum, interrupted)
            for role in ("host", "peer"):
                game = base / role; game.mkdir(); (game / "tmp").mkdir(); (game / "cache").mkdir()
                if role == "host":
                    (game / "eula.txt").write_bytes(eula)
                argfile = game / "java.args"
                argfile.write_text("\n".join(java_quote(arg) for arg in command(launch, role, game, ipc, identity, options.timeout, selected)) + "\n", encoding="utf-8")
                log = (base / (role + ".log")).open("wb"); logs.append(log)
                if time.monotonic() - started >= options.timeout:
                    raise TimeoutError("Supervisor deadline expired before client launch")
                process = subprocess.Popen([launch["java"], "@" + str(argfile)], cwd=game, env=environment(launch, game, nonce if selected["suite"] == MOON_SUITE else None,
                                               nonce if selected["suite"] == SUITE else None),
                                           stdin=subprocess.DEVNULL, stdout=log, stderr=subprocess.STDOUT, shell=False)
                jobs.append((role, process))
            while True:
                states = {role: process.poll() for role, process in jobs}
                failures = {role: code for role, code in states.items() if code not in (None, 0)}
                require(not failures, "Actual client process failed: " + str(failures))
                require(not any((ipc / (role + "-failure.properties")).exists() for role in PROFILES), "Client published a failure witness")
                if selected["suite"] == SUITE:
                    require(not any(ipc.glob("*counter-render-failure*")), "Client published a sticky counter rejection")
                if time.monotonic() - started >= options.timeout:
                    raise TimeoutError("Two clients exceeded the finite supervisor deadline")
                if all(code == 0 for code in states.values()):
                    break
                time.sleep(.25)
            report["witnesses"] = validate_witnesses(ipc, identity, contract["cases"], jobs, selected)
            if selected["suite"] == MOON_SUITE:
                report.update(releaseImageDamageOrderVerified=True, serverReleaseFrameCorrespondenceVerified=False,
                              pixelQualityReviewed=False, proofScope="rendered ACTIVE and causal release/image/damage interval")
            else:
                report.update(counterOwnerPeerGeometryVerified=True, phaseBasis=COUNTER_PHASE_BASIS,
                              counterPhaseCaptures=len(COUNTER_CASES) * len(COUNTER_PHASES) * 2,
                              serverReleaseFrameCorrespondenceVerified=False, pixelQualityReviewed=False)
                if selected.get("expectedSkin"):
                    report["expectedSkin"] = selected["expectedSkin"]
            require(source_head(root) == identity["sourceHead"], "Source changed during execution")
            require(digest(Path(launch["descriptor"])) == identity["descriptorSha256"], "Descriptor changed during execution")
            report["status"] = "passed"
        except BaseException as error:
            report["error"] = type(error).__name__ + ": " + str(error)
            raise
        finally:
            if already_locked:
                # The matrix's one-shot elapsed alarm must not interrupt a
                # terminate/wait/kill sequence or lose partial evidence. The
                # matrix rechecks elapsed time immediately after we return.
                handlers[signal.SIGALRM] = signal.getsignal(signal.SIGALRM)
            for signum in handlers:
                signal.signal(signum, signal.SIG_IGN)
            errors = stop_owned(jobs)
            if errors:
                report.update(status="failed", cleanupErrors=errors)
            for log in logs:
                log.close()
            report["processes"] = {role: {"pid": process.pid, "exit": process.returncode} for role, process in jobs}
            report["seconds"] = round(time.monotonic() - started, 3)
            (base / "result.json").write_text(json.dumps(report, indent=2) + "\n")
            for signum, handler in handlers.items():
                signal.signal(signum, handler)
        require(report["status"] == "passed", "Owned client cleanup failed; see result.json")
        print("Two-client " + selected["suite"] + " receipts passed: " + str(base / "result.json"))
        return report


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("launch", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--suite", choices=(SUITE, MOON_SUITE), required=True)
    parser.add_argument("--profile", choices=("aura", *COUNTER_PROFILES, *MOON_PROFILES), required=True)
    parser.add_argument("--case", choices=(MOON_CASE,))
    parser.add_argument("--observer-angle", choices=MOON_ANGLES)
    parser.add_argument("--timeout", type=int, help="Seconds; default and ceiling: cast-receipt 900, moon 180")
    parser.add_argument("--accepted-eula", type=Path)
    options = parser.parse_args(argv)
    try:
        selected = selection(options.suite, options.profile, options.case, options.observer_angle)
        if options.timeout is None:
            options.timeout = selected["maxTimeoutSeconds"]
        require(1 <= options.timeout <= selected["maxTimeoutSeconds"], "Timeout exceeds the selected suite ceiling")
    except ValueError as error:
        parser.error(str(error))
    return options


if __name__ == "__main__":
    try:
        run(parse_args())
    except (ValueError, RuntimeError, OSError, subprocess.SubprocessError) as error:
        print("Two-client supervisor failed: " + str(error), file=sys.stderr)
        raise SystemExit(1)
