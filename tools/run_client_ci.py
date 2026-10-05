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

from native_ci_diagnostics import DIAGNOSTIC_PREFIX, NativeDiagnostics

from client_suites import EXIT_PREFIX, SELECTION_PREFIX, select_entries


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


def launch_command(selection):
    gradle = ["./gradlew", "runClientGameTest", "--no-daemon", "--stacktrace", "--console=plain"]
    if selection["kind"] == "shard":
        shard, shards = selection["shard"].split("/")
        gradle += [f"-PciShard={shard}", f"-PciShards={shards}"]
    elif selection["kind"] == "suite":
        gradle += [f"-PciSuite={selection['name']}"]
    return ["xvfb-run", "-a", "-s",
            "-screen 0 1280x720x24 +extension GLX +render -noreset", *gradle]


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--log", type=Path, default=Path("client-game-tests.log"))
    selectors = parser.add_mutually_exclusive_group()
    selectors.add_argument("--shard", metavar="I/N",
                           help="Run only the I-th of N contiguous parts of the full descriptor (1-based)")
    selectors.add_argument("--suite", help="Run one named focused group from client_suite_catalog.json")
    selectors.add_argument("--check-log", type=Path,
                           help="Diagnose an existing log; does not establish test success")
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

        record(SELECTION_PREFIX + json.dumps(selection, sort_keys=True))
        process = subprocess.Popen(
            launch_command(selection), stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
            encoding="utf-8", errors="replace", start_new_session=True)
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
                    record(EXIT_PREFIX + "2")
                    return 2
            result = process.wait()
            record(EXIT_PREFIX + str(result))
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
