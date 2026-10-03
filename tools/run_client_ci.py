"""Run the full native client gate; stop a headless backend error dialog promptly.

Only the process group started by this tool is stopped. A single failed backend is
not fatal: Minecraft may initialize another one. The log is retained on failure.
"""
import argparse
import os
from pathlib import Path
import signal
import subprocess
import sys


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


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--log", type=Path, default=Path("client-game-tests.log"))
    parser.add_argument("--check-log", type=Path,
                        help="Diagnose an existing log; does not establish test success")
    args = parser.parse_args()
    failures = BackendFailures()
    if args.check_log:
        fatal = any(failures.observe(line) for line in
                    args.check_log.read_text(encoding="utf-8-sig", errors="replace").splitlines())
        print("Both graphics backends failed" if fatal else "No dual-backend failure detected")
        return 2 if fatal else 0
    if not sys.platform.startswith("linux"):
        parser.error("This CI launcher requires Linux; use Gradle directly for local tests")
    args.log.parent.mkdir(parents=True, exist_ok=True)
    process = subprocess.Popen(
        ["xvfb-run", "-a", "-s", "-screen 0 1280x720x24 +extension GLX +render -noreset",
         "./gradlew", "runClientGameTest", "--no-daemon", "--stacktrace", "--console=plain"],
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
        encoding="utf-8", errors="replace", start_new_session=True)
    try:
        with args.log.open("w", encoding="utf-8", newline="\n") as log:
            for line in process.stdout:
                sys.stdout.write(line)
                sys.stdout.flush()
                log.write(line)
                log.flush()
                if failures.observe(line):
                    message = "CI graphics startup failed: both OpenGL and Vulkan failed; no gameplay pass is claimed.\n"
                    print(message, end="", flush=True)
                    log.write(message)
                    log.flush()
                    stop_owned_group(process)
                    return 2
        return process.wait()
    finally:
        if process.poll() is None:
            stop_owned_group(process)


if __name__ == "__main__":
    sys.exit(main())
