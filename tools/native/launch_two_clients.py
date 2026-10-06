"""Bounded two-JVM cast-receipt gate; no arbitrary commands or suite selectors.

First export with -I tools/native/export_two_client_launch.init.gradle
exportTwoClientLaunch -PtwoClientSuite=cast-receipt. Then launch that exact export
with --suite cast-receipt --profile aura --output build/native/receipts.
The historical proposal's --require-* launcher flags are no longer supported;
its optional bare Gradle descriptor selectors remain available separately.
"""
from __future__ import annotations

import argparse
from contextlib import contextmanager
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import signal
import subprocess
import sys
import time
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
# Keep the original 35 identities in place; additions are mandatory and ordered.
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
)
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


def load_contract(root):
    contract = json.loads((root / CONTRACT).read_text())
    require(contract.get("schemaVersion") == 1 and contract.get("suite") == SUITE and contract.get("entrypoint") == ENTRYPOINT,
            "Unexpected source-controlled suite contract")
    cases = contract.get("cases")
    require(cases == list(CASES) and contract.get("expectedCount") == len(CASES),
            "Cast contract requires the exact ordered 38-case roster")
    require(contract.get("limits") == {"maxJvms": 2, "maxHeapMiBPerJvm": 2048, "maxCases": len(CASES), "maxTimeoutSeconds": MAX_TIMEOUT},
            "Unexpected cast contract resource limits")
    require(contract.get("terminalWitnesses") == list(TERMINALS), "Unexpected terminal paths")
    require(contract.get("profiles", {}).get("aura") == {"username": PROFILES["host"][0], "offlineUuid": PROFILES["host"][1]}, "Invalid fixed host profile")
    require(contract.get("peerProfile") == {"username": PROFILES["peer"][0], "offlineUuid": PROFILES["peer"][1]}, "Invalid fixed peer profile")
    return contract


def validate_launch(launch, path, root):
    require(launch.get("schemaVersion") == 2 and launch.get("suite") == SUITE, "Re-export the allowlisted cast-receipt suite")
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
    require(json.loads(descriptor.read_text())["entrypoints"]["fabric-client-gametest"] == [ENTRYPOINT], "Descriptor must select exactly the supervised fixture")
    require(launch.get("contract") == CONTRACT and launch.get("contractSha256") == digest(root / CONTRACT), "Source contract changed after export")
    require(digest(descriptor.parent / Path(CONTRACT).name) == digest(root / CONTRACT), "Processed contract differs from source contract")
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


def command(launch, role, game, ipc, identity, timeout):
    replace = {"--gameDir": str(game), "--username": PROFILES[role][0], "--uuid": PROFILES[role][1],
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
    return [*jvm, "-cp", os.pathsep.join(launch["classpath"]), MAIN, *args]


def read_properties(path):
    require(path.is_file() and not path.is_symlink(), "Missing or symlink terminal witness: " + str(path))
    require(path.stat().st_size <= 65536, "Oversized terminal witness")
    result = {}
    for line in path.read_text(encoding="iso-8859-1").splitlines():
        if not line.strip() or line.lstrip().startswith(("#", "!")):
            continue
        key, separator, value = line.partition("=")
        require(separator and key not in result, "Malformed or duplicate witness property")
        result[key] = value
    return result


def validate_witnesses(ipc, identity, cases, jobs):
    pids = {role: process.pid for role, process in jobs}
    require(set(pids) == {"host", "peer"} and len(set(pids.values())) == 2, "Exactly two distinct owned JVMs required")
    for filename, role in TERMINALS.items():
        values = read_properties(ipc / filename)
        for key, value in {**identity, "role": role, "pid": str(pids[role]), "cases": ",".join(cases)}.items():
            require(values.get(key) == value, filename + " has mismatched " + key)
    return list(TERMINALS)


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


def environment(launch, game):
    env = os.environ.copy()
    for key in list(env):
        if key in {"JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "CLASSPATH", "LD_PRELOAD", "LD_LIBRARY_PATH"} or key.startswith(("WILDERCORD_", "DYLD_")):
            env.pop(key, None)
    env.update(launch["environment"])
    env.update({key: str(game / "tmp") for key in ("TMPDIR", "TEMP", "TMP")})
    env.update({"XDG_CACHE_HOME": str(game / "cache"), "MESA_SHADER_CACHE_DIR": str(game / "cache/mesa")})
    return env


def run(options, root=ROOT):
    root = root.resolve()
    require(options.suite == SUITE and options.profile == "aura", "Only cast-receipt with the fixed aura profile is supported")
    require(1 <= options.timeout <= MAX_TIMEOUT, "Deadline must be between 1 and 900 seconds")
    launch_path, base = output_path(options.launch, root), output_path(options.output, root)
    require(not base.exists(), "Output must be fresh")
    ignored_output(base, root)
    with supervisor_lock(root):
        base.mkdir(parents=True, exist_ok=False)
        jobs, logs, handlers = [], [], {}
        started = time.monotonic()
        report = {"status": "failed", "stage": "preflight", "suite": SUITE, "profile": "aura", "timeoutSeconds": options.timeout,
                  "heapMiBPerJvm": 2048, "launcherPid": os.getpid(), "processes": {}}
        try:
            contract = load_contract(root)
            eula, report["eulaSource"] = accepted_eula(options.accepted_eula, root)
            require(launch_path.stat().st_size <= 2 * 1024 * 1024, "Oversized launch export")
            launch = json.loads(launch_path.read_text(encoding="utf-8-sig"))
            report["launchSha256"] = validate_launch(launch, launch_path, root)
            for item in launch["classpath"]:
                runtime = Path(item).resolve()
                require(base != runtime and base not in runtime.parents and runtime not in base.parents, "Game output overlaps runtime inputs")
            nonce = str(uuid.uuid4())
            ci = {key: os.environ.get(key) for key in ("GITHUB_RUN_ID", "GITHUB_RUN_ATTEMPT", "GITHUB_JOB")}
            for key in ("GITHUB_RUN_ID", "GITHUB_RUN_ATTEMPT"):
                require(ci[key] is None or re.fullmatch(r"[0-9]+", ci[key]), "Malformed CI run identity")
            identity = {"nonce": nonce, "suite": SUITE, "sourceHead": launch["sourceHead"], "checkoutSha": launch["checkoutSha"],
                        "prHeadSha": launch["prHeadSha"], "descriptorSha256": launch["descriptorSha256"],
                        "hostUuid": PROFILES["host"][1], "peerUuid": PROFILES["peer"][1],
                        "runIdentity": "-".join([ci["GITHUB_RUN_ID"] or "local", ci["GITHUB_RUN_ATTEMPT"] or "0", nonce])}
            report.update({**identity, "cases": contract["cases"], "ci": ci, "stage": "clients"})
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
                argfile.write_text("\n".join(java_quote(arg) for arg in command(launch, role, game, ipc, identity, options.timeout)) + "\n", encoding="utf-8")
                log = (base / (role + ".log")).open("wb"); logs.append(log)
                if time.monotonic() - started >= options.timeout:
                    raise TimeoutError("Supervisor deadline expired before client launch")
                process = subprocess.Popen([launch["java"], "@" + str(argfile)], cwd=game, env=environment(launch, game),
                                           stdin=subprocess.DEVNULL, stdout=log, stderr=subprocess.STDOUT, shell=False)
                jobs.append((role, process))
            while True:
                states = {role: process.poll() for role, process in jobs}
                failures = {role: code for role, code in states.items() if code not in (None, 0)}
                require(not failures, "Actual client process failed: " + str(failures))
                require(not any((ipc / (role + "-failure.properties")).exists() for role in PROFILES), "Client published a failure witness")
                if time.monotonic() - started >= options.timeout:
                    raise TimeoutError("Two clients exceeded the finite supervisor deadline")
                if all(code == 0 for code in states.values()):
                    break
                time.sleep(.25)
            report["witnesses"] = validate_witnesses(ipc, identity, contract["cases"], jobs)
            require(source_head(root) == identity["sourceHead"], "Source changed during execution")
            require(digest(Path(launch["descriptor"])) == identity["descriptorSha256"], "Descriptor changed during execution")
            report["status"] = "passed"
        except BaseException as error:
            report["error"] = type(error).__name__ + ": " + str(error)
            raise
        finally:
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
        print("Two-client cast receipts passed: " + str(base / "result.json"))
        return report


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("launch", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--suite", choices=(SUITE,), required=True)
    parser.add_argument("--profile", choices=("aura",), required=True)
    parser.add_argument("--timeout", type=int, default=900)
    parser.add_argument("--accepted-eula", type=Path)
    return parser.parse_args(argv)


if __name__ == "__main__":
    try:
        run(parse_args())
    except (ValueError, RuntimeError, OSError, subprocess.SubprocessError) as error:
        print("Two-client supervisor failed: " + str(error), file=sys.stderr)
        raise SystemExit(1)
