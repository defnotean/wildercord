"""Run the full, sharded or named focused native gate; stop backend dialogs promptly.

Only the process group started by this tool is stopped. A single failed backend is
not fatal: Minecraft may initialize another one. The log is retained on failure.
"""
import argparse
import json
import os
from pathlib import Path
import signal
import subprocess
import sys
import threading
import uuid

from native_ci_diagnostics import DIAGNOSTIC_PREFIX, NativeDiagnostics

from client_suites import EXIT_PREFIX, REQUEST_PREFIX, SELECTION_PREFIX, select_entries

MOON_ENTRY = "dev.wildercord.client.combat.CrimsonMoonCaptureTest"
MOON_NONCE = "WILDERCORD_MOON_RECEIPT_NONCE"
MOON_LAUNCH = Path("artifacts/review/moon-owner-launch.json")


def moon_launch_environment(selection, environment, nonce_factory=uuid.uuid4):
    """One fresh owned-launch nonce; inherited values never certify this capture."""
    if MOON_ENTRY not in selection["entries"]:
        return None, None
    child = dict(environment)
    nonce = str(nonce_factory())
    if str(uuid.UUID(nonce)) != nonce:
        raise ValueError("Moon launch nonce must be a canonical UUID")
    child[MOON_NONCE] = nonce
    receipt = {"schemaVersion": 1, "purpose": "owner-fp-native-diagnostic",
               "nonce": nonce, "selection": selection,
               "checkoutSha": environment.get("GITHUB_SHA"),
               "runId": environment.get("GITHUB_RUN_ID"),
               "runAttempt": environment.get("GITHUB_RUN_ATTEMPT"),
               "remoteObserverCoverage": False, "serverReleaseFrameCorrespondenceVerified": False}
    return child, receipt


class BackendFailures:
    def __init__(self):
        self.failed = set()

    def observe(self, line):
        for backend in ("OpenGL", "Vulkan"):
            if f"Failed to create backend {backend}" in line:
                self.failed.add(backend)
        return len(self.failed) == 2


def stop_owned_group(process):
    os.killpg(process.pid, signal.SIGTERM)
    try:
        process.wait(timeout=10)
    except subprocess.TimeoutExpired:
        os.killpg(process.pid, signal.SIGKILL)
        process.wait()


def launch_command(selection, *, shaders=False):
    gradle = ["./gradlew", "runClientGameTest", "--no-daemon", "--stacktrace", "--console=plain"]
    if selection["kind"] == "shard":
        shard, shards = selection["shard"].split("/")
        gradle += [f"-PciShard={shard}", f"-PciShards={shards}"]
    elif selection["kind"] in ("suite", "diagnostic", "required-part"):
        gradle += [f"-PciSuite={selection['name']}"]
    if shaders:
        gradle += ["-Pshaders"]
    return ["xvfb-run", "-a", "-s",
            "-screen 0 1280x720x24 +extension GLX +render -noreset", *gradle]


def main(argv=None, *, diagnostic_provenance=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--log", type=Path, default=Path("client-game-tests.log"))
    selectors = parser.add_mutually_exclusive_group()
    selectors.add_argument("--shard", metavar="I/N",
                           help="Run one pinned explicit full-client group (1/4, 2/4, 3/4 or 4/4)")
    selectors.add_argument("--suite", help="Run one named focused group from client_suite_catalog.json")
    selectors.add_argument("--check-log", type=Path,
                           help="Diagnose an existing log; does not establish test success")
    parser.add_argument("--shaders", action="store_true", help="Enable the separate Iris shader profile")
    args = parser.parse_args(argv)
    try:
        selection = select_entries(suite=args.suite, shard=args.shard)
    except (ValueError, KeyError, OSError) as exc:
        parser.error(str(exc))
    failures = BackendFailures()
    if args.check_log:
        fatal = any(failures.observe(line) for line in
                    args.check_log.read_text(encoding="utf-8-sig", errors="replace").splitlines())
        print("Both graphics backends failed" if fatal else "No dual-backend failure detected")
        return 2 if fatal else 0
    if not sys.platform.startswith("linux"):
        parser.error("This CI launcher requires Linux; use Gradle directly for local tests")
    args.log.parent.mkdir(parents=True, exist_ok=True)
    with args.log.open("w", encoding="utf-8", newline="\n") as log:
        output_lock = threading.Lock()

        def record(message):
            with output_lock:
                print(message, flush=True)
                log.write(message + "\n")
                log.flush()

        full_provenance = None
        full_environment = None
        if selection["kind"] in ("full", "shard"):
            from full_client_ci import (PROVENANCE_PREFIX, OWNED_EXIT_PREFIX, LAUNCH_ENV,
                                        current_identity, execution_profile, provenance)
            try:
                full_environment = dict(os.environ)
                full_provenance = provenance(selection, current_identity(os.environ),
                    execution_profile(os.environ, shaders=args.shaders), str(uuid.uuid4()))
                full_environment[LAUNCH_ENV] = full_provenance["launchId"]
            except (ValueError, OSError, subprocess.CalledProcessError) as exc:
                parser.error(str(exc))
        record(SELECTION_PREFIX + json.dumps(selection, sort_keys=True))
        if full_provenance is not None:
            record(PROVENANCE_PREFIX + json.dumps(full_provenance, sort_keys=True))

        def record_exit(result):
            record(EXIT_PREFIX + str(result))
            if full_provenance is not None:
                record(OWNED_EXIT_PREFIX + json.dumps({"launchId": full_provenance["launchId"], "exitCode": result}, sort_keys=True))
        if selection["kind"] == "required-part":
            from masters_required_ci import PART_PREFIX, launch_provenance
            try:
                provenance = launch_provenance(selection, os.environ)
            except (ValueError, OSError, subprocess.CalledProcessError) as exc:
                parser.error(str(exc))
            record(PART_PREFIX + json.dumps(provenance, sort_keys=True))
        if diagnostic_provenance is not None:
            if selection["kind"] != "diagnostic":
                parser.error("Request provenance is only supported for diagnostic selections")
            record(REQUEST_PREFIX + json.dumps(diagnostic_provenance, sort_keys=True))
        environment, moon_receipt = moon_launch_environment(selection, full_environment if full_environment is not None else os.environ)
        launch_options = {"env": full_environment} if full_environment is not None else {}
        if moon_receipt is not None:
            MOON_LAUNCH.parent.mkdir(parents=True, exist_ok=True)
            MOON_LAUNCH.write_text(json.dumps(moon_receipt, indent=2) + "\n", encoding="utf-8")
            record("WILDERCORD_MOON_OWNER_LAUNCH " + json.dumps(moon_receipt, sort_keys=True))
            launch_options["env"] = environment
        process = subprocess.Popen(
            launch_command(selection, shaders=args.shaders), stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
            encoding="utf-8", errors="replace", start_new_session=True, **launch_options)
        diagnostics = None
        observe_progress = True

        def diagnostic_failure(stage, exc):
            record(DIAGNOSTIC_PREFIX + json.dumps({"event": "diagnostic-unavailable",
                                                  "stage": stage, "errorType": type(exc).__name__}))

        try:
            try:
                diagnostics = NativeDiagnostics(process, selection, record)
                diagnostics.start()
            except Exception as exc:
                diagnostic_failure("start", exc)
            for line in process.stdout:
                if diagnostics is not None and observe_progress:
                    try:
                        diagnostics.observe(line)
                    except Exception as exc:
                        observe_progress = False
                        diagnostic_failure("observe", exc)
                with output_lock:
                    sys.stdout.write(line)
                    sys.stdout.flush()
                    log.write(line)
                    log.flush()
                if failures.observe(line):
                    record("CI graphics startup failed: both OpenGL and Vulkan failed; no gameplay pass is claimed.")
                    stop_owned_group(process)
                    record_exit(2)
                    return 2
            result = process.wait()
            record_exit(result)
            return result
        finally:
            if diagnostics is not None:
                try:
                    diagnostics.close()
                except Exception as exc:
                    diagnostic_failure("close", exc)
            if process.poll() is None:
                stop_owned_group(process)


if __name__ == "__main__":
    sys.exit(main())
