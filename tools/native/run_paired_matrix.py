"""One bounded job: both fixed 54-case counter profiles, then four paired Moon views.

No retries, parallel groups, arbitrary selections or runnable command input.
Success must finish both exports, validation and client cleanup inside the elapsed
budget. Expiry permits only bounded cleanup/reporting grace inside the 51m step.
"""
from __future__ import annotations

import argparse
from contextlib import contextmanager
import json
import math
import os
from pathlib import Path
import re
import signal
import subprocess
import sys
import time
import types
import uuid

try:
    from . import launch_two_clients as s
except ImportError:
    import launch_two_clients as s

ROOT = s.ROOT
# Six sequential full ceilings total 2520 seconds. Keep 480 seconds for both
# exports/validation, plus one minute of cleanup grace inside the 51m CI step.
MAX_TOTAL_SECONDS = 50 * 60
GROUPS = (
    *({"id": f"cast-receipt-{skin}", "suite": s.SUITE, "profile": f"aura-{skin}", "case": None, "observerAngle": None}
      for skin in ("wide", "slim")),
    *({"id": f"moon-{skin}-{angle}", "suite": s.MOON_SUITE, "profile": f"moon-{skin}",
       "case": s.MOON_CASE, "observerAngle": angle}
      for skin in ("wide", "slim") for angle in s.MOON_ANGLES),
)


def encode(value):
    return (json.dumps(value, indent=2, sort_keys=True, allow_nan=False) + "\n").encode()


def write_report(path, value):
    temporary = path.with_suffix(".partial.json")
    temporary.write_bytes(encode(value))
    temporary.replace(path)


def read_json(path, maximum=2 * 1024 * 1024):
    s.require(path.is_file() and not path.is_symlink() and path.stat().st_size <= maximum,
              "Missing, linked or oversized JSON evidence: " + str(path))
    return json.loads(path.read_text())


def safe_path(root, relative):
    relative = Path(relative)
    s.require(not relative.is_absolute() and ".." not in relative.parts and relative.parts,
              "Evidence path must be a safe relative path")
    path = root / relative
    for parent in (path, *path.parents):
        if parent == root:
            break
        s.require(not parent.is_symlink(), "Symlink evidence path: " + str(relative))
    return s.inside(path, [root.resolve()])


def provenance(root, environ=None):
    env = os.environ if environ is None else environ
    head = s.source_head(root)
    pr = env.get("WILDERCORD_PR_HEAD_SHA", "")
    s.require(not pr or re.fullmatch(r"[a-f0-9]{40}", pr), "Invalid PR head")
    result = {"sourceHead": head, "checkoutSha": head, "prHeadSha": pr,
              "runId": env.get("GITHUB_RUN_ID"), "runAttempt": env.get("GITHUB_RUN_ATTEMPT"),
              "job": env.get("GITHUB_JOB"), "repository": env.get("GITHUB_REPOSITORY"),
              "eventName": env.get("GITHUB_EVENT_NAME")}
    if env.get("GITHUB_ACTIONS") == "true":
        s.require(env.get("GITHUB_SHA") == head, "CI checkout SHA differs from HEAD")
        s.require(all(re.fullmatch(r"[1-9][0-9]*", result[k] or "") for k in ("runId", "runAttempt")),
                  "Missing exact CI run/attempt")
        s.require(result["job"] == "connected-combat-native" and re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", result["repository"] or ""),
                  "Missing existing paired job/repository identity")
        if result["eventName"] == "pull_request":
            event = read_json(Path(env.get("GITHUB_EVENT_PATH", "")), 8 * 1024 * 1024)
            s.require(pr and event.get("pull_request", {}).get("head", {}).get("sha") == pr,
                      "PR head does not match the actual workflow event")
        else:
            s.require(result["eventName"] == "push" and not pr, "Unsupported CI event/PR provenance")
        result["runUrl"] = f"https://github.com/{result['repository']}/actions/runs/{result['runId']}/attempts/{result['runAttempt']}"
    return result


def remaining(deadline, now=time.monotonic):
    value = deadline - now()
    s.require(math.isfinite(value) and value >= 1, "Paired matrix elapsed budget exhausted")
    return value


@contextmanager
def elapsed_guard(seconds):
    """Interrupt exports, preflight hashes, receipt decode and active clients alike."""
    s.require(hasattr(signal, "setitimer"), "The bounded matrix requires a POSIX deadline timer")
    s.require(signal.getitimer(signal.ITIMER_REAL) == (0.0, 0.0), "An existing alarm owns this process")
    def expired(signum, frame):
        raise TimeoutError("Paired matrix elapsed deadline expired")
    previous = signal.signal(signal.SIGALRM, expired)
    signal.setitimer(signal.ITIMER_REAL, seconds)
    try:
        yield
    finally:
        signal.setitimer(signal.ITIMER_REAL, 0)
        signal.signal(signal.SIGALRM, previous)


def export(root, suite, path, log_path, deadline):
    command = [str(root / "gradlew"), "-I", "tools/native/export_two_client_launch.init.gradle",
               "exportTwoClientLaunch", "-PtwoClientSuite=" + suite,
               "-PtwoClientLaunch=" + str(path), "--no-daemon", "--stacktrace", "--console=plain"]
    process = None
    with log_path.open("xb") as log:
        try:
            remaining(deadline)
            process = subprocess.Popen(command, cwd=root, stdin=subprocess.DEVNULL,
                                       stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
            code = process.wait(timeout=remaining(deadline))
            s.require(code == 0, "Launch export failed for " + suite + ": " + str(code))
        finally:
            if process is not None:
                # A Gradle launcher may exit while its no-daemon JVM is still
                # shutting down. Reap the entire owned export process group.
                handlers = {sig: signal.signal(sig, signal.SIG_IGN)
                            for sig in (signal.SIGALRM, signal.SIGINT, signal.SIGTERM)}
                try:
                    try:
                        os.killpg(process.pid, signal.SIGTERM)
                    except ProcessLookupError:
                        pass
                    try:
                        process.wait(timeout=5)
                    except subprocess.TimeoutExpired:
                        os.killpg(process.pid, signal.SIGKILL)
                        process.wait(timeout=5)
                    try:
                        os.killpg(process.pid, signal.SIGKILL)
                    except ProcessLookupError:
                        pass
                finally:
                    for sig, handler in handlers.items():
                        signal.signal(sig, handler)


def selected(group):
    return s.selection(group["suite"], group["profile"], group["case"], group["observerAngle"])


def validate_group(base, group, identity, seen_nonces):
    result = read_json(base / "result.json")
    selection = selected(group)
    s.require(result.get("status") == "passed" and not result.get("cleanupErrors"), "Group did not finish with clean owned clients")
    for key in ("sourceHead", "checkoutSha", "prHeadSha"):
        s.require(result.get(key) == identity[key], "Group changed " + key)
    expected_ci = {"GITHUB_RUN_ID": identity["runId"], "GITHUB_RUN_ATTEMPT": identity["runAttempt"], "GITHUB_JOB": identity["job"]}
    s.require(result.get("ci") == expected_ci, "Group changed CI run identity")
    nonce = result.get("nonce")
    s.require(isinstance(nonce, str) and str(uuid.UUID(nonce)) == nonce and nonce not in seen_nonces,
              "Missing, duplicate or invalid launch nonce")
    s.require(result.get("runIdentity") == "-".join([identity["runId"] or "local", identity["runAttempt"] or "0", nonce]),
              "Group run/nonce binding differs")
    for key in ("suite", "profile", "cases"):
        s.require(result.get(key) == selection[key], "Group changed fixed " + key)
    s.require(type(result.get("timeoutSeconds")) is int and result["timeoutSeconds"] == selection["maxTimeoutSeconds"]
              and result.get("heapMiBPerJvm") == 2048, "Group resource limits changed")
    for key in ("descriptorSha256", "launchSha256"):
        s.require(re.fullmatch(r"[a-f0-9]{64}", result.get(key, "")), "Missing group export/descriptor hash")
    processes = result.get("processes", {})
    s.require(set(processes) == {"host", "peer"} and all(type(v.get("pid")) is int and v["pid"] > 0 and v.get("exit") == 0 for v in processes.values()),
              "Two clean native process exits are required")
    proof = read_json(base / "launch-proof.json")
    keys = (*s.IDENTITY, "cases", "ci", "profile", "launchSha256", "heapMiBPerJvm", "timeoutSeconds")
    s.require(proof.get("status") == "started" and all(proof.get(k) == result.get(k) for k in keys), "Launch proof and result provenance differ")
    witness_identity = {k: result[k] for k in s.IDENTITY}
    for role in ("host", "peer"):
        s.require(result[role + "Uuid"] == selection["profiles"][role][1], "Unexpected native profile UUID")
    if group["suite"] == s.MOON_SUITE:
        for key in ("case", "expectedSkin", "observerAngle"):
            s.require(result.get(key) == selection[key] and proof.get(key) == selection[key], "Moon selection provenance differs")
            witness_identity[key] = selection[key]
        s.require(result.get("serverReleaseFrameCorrespondenceVerified") is False
                  and result.get("releaseImageDamageOrderVerified") is True and result.get("pixelQualityReviewed") is False,
                  "Moon result overstates its proof")
    else:
        s.require(result.get("expectedSkin") == proof.get("expectedSkin") == selection["expectedSkin"],
                  "Counter selection provenance differs")
        s.require(result.get("counterOwnerPeerGeometryVerified") is True and result.get("counterPhaseCaptures") == 64
                  and result.get("phaseBasis") == s.COUNTER_PHASE_BASIS
                  and result.get("pixelQualityReviewed") is False and result.get("serverReleaseFrameCorrespondenceVerified") is False,
                  "Counter result overstates or lacks its full geometric proof")
    jobs = [(role, types.SimpleNamespace(pid=value["pid"])) for role, value in processes.items()]
    witnesses = s.validate_witnesses(base / "ipc", witness_identity, selection["cases"], jobs, selection)
    s.require(result.get("witnesses") == witnesses, "Missing exact completed witness ledger")
    seen_nonces.add(nonce)
    return result


def failed_group_cleanup(base, group):
    """Only a fresh report from this in-process supervisor can prove all owned JVMs reaped."""
    try:
        result = read_json(base / "result.json")
        if result.get("launcherPid") != os.getpid() or result.get("suite") != group["suite"] or result.get("profile") != group["profile"] or result.get("cleanupErrors"):
            return False
        processes = result.get("processes")
        if not isinstance(processes, dict) or not set(processes) <= {"host", "peer"}:
            return False
        if not processes:
            return result.get("stage") == "preflight"
        return all(isinstance(p, dict) and type(p.get("pid")) is int and p["pid"] > 0 and type(p.get("exit")) is int for p in processes.values())
    except (ValueError, OSError, TypeError):
        return False


def run(options, root=ROOT):
    root = root.resolve()
    s.require(type(options.total_timeout) is int and 1 <= options.total_timeout <= MAX_TOTAL_SECONDS, "Invalid total elapsed budget")
    base = s.output_path(options.output, root)
    s.require(not base.exists(), "Matrix output must be fresh")
    s.ignored_output(base, root)
    started = time.monotonic()
    deadline = started + options.total_timeout
    base.mkdir(parents=True)
    path = base / "matrix-result.json"
    report = {"schemaVersion": 1, "status": "failed", "totalTimeoutSeconds": options.total_timeout,
              "maxConcurrentJvms": 2, "heapMiBPerJvm": 2048, "groups": [{**g, "status": "not_started"} for g in GROUPS],
              "serverReleaseFrameCorrespondenceVerified": False, "pixelQualityReviewed": False,
              "releaseImageDamageOrderVerified": False, "counterOwnerPeerGeometryVerified": False}
    write_report(path, report)
    old_handlers = {}
    try:
        def interrupted(signum, frame):
            raise InterruptedError("Matrix received signal " + str(signum))
        for signum in (signal.SIGINT, signal.SIGTERM):
            old_handlers[signum] = signal.signal(signum, interrupted)
        with elapsed_guard(remaining(deadline)), s.supervisor_lock(root):
            report["provenance"] = provenance(root)
            s.accepted_eula(options.accepted_eula, root)
            write_report(path, report)
            launches, nonces = {}, set()
            failures = []
            blocked = None
            for entry, group in zip(report["groups"], GROUPS):
                if blocked is not None:
                    entry.update(status="unverified", executed=False, error=blocked)
                    write_report(path, report)
                    continue
                try:
                    remaining(deadline)
                except ValueError as error:
                    blocked = str(error)
                    entry.update(status="unverified", executed=False, error=blocked)
                    failures.append(error); write_report(path, report)
                    continue
                timeout = selected(group)["maxTimeoutSeconds"]
                if remaining(deadline) < timeout:
                    reason = "Insufficient remaining budget for the full " + str(timeout) + "-second group ceiling"
                    entry.update(status="unverified", executed=False, error=reason)
                    failures.append(ValueError(reason)); write_report(path, report)
                    continue
                entry.update(status="running", executed=False, startedSeconds=round(time.monotonic() - started, 3))
                write_report(path, report)
                suite = group["suite"]
                if suite not in launches:
                    launch_path = base / (suite + "-launch.json")
                    export(root, suite, launch_path, base / (suite + "-export.log"), deadline)
                    remaining(deadline)
                    launch = read_json(launch_path)
                    s.validate_launch(launch, launch_path, root, selected(group))
                    descriptor = base / (suite + "-descriptor.json")
                    descriptor.write_bytes(Path(launch["descriptor"]).read_bytes())
                    launches[suite] = launch_path
                launch_path = launches[suite]
                timeout = selected(group)["maxTimeoutSeconds"]
                if remaining(deadline) < timeout:
                    reason = "Insufficient remaining budget for the full " + str(timeout) + "-second group ceiling"
                    entry.update(status="unverified", executed=False, error=reason)
                    failures.append(ValueError(reason)); write_report(path, report)
                    continue
                child_options = types.SimpleNamespace(launch=launch_path, output=base / group["id"], suite=suite,
                    profile=group["profile"], case=group["case"], observer_angle=group["observerAngle"],
                    timeout=timeout, accepted_eula=options.accepted_eula)
                try:
                    s.run(child_options, root, already_locked=True)
                    remaining(deadline)
                    result = validate_group(child_options.output, group, report["provenance"], nonces)
                    entry.update(status="passed", executed=True, nonce=result["nonce"], runIdentity=result["runIdentity"],
                                 resultSha256=s.digest(child_options.output / "result.json"), finishedSeconds=round(time.monotonic() - started, 3))
                except Exception as error:
                    cleanup = failed_group_cleanup(child_options.output, group)
                    try:
                        native_result = read_json(child_options.output / "result.json")
                        executed = bool(native_result.get("processes")) and native_result.get("launcherPid") == os.getpid()
                    except (ValueError, OSError, TypeError):
                        executed = False
                    entry.update(status="failed", executed=executed,
                                 error=type(error).__name__ + ": " + str(error), cleanupVerified=cleanup,
                                 finishedSeconds=round(time.monotonic() - started, 3))
                    if (child_options.output / "result.json").is_file():
                        entry["resultSha256"] = s.digest(child_options.output / "result.json")
                    failures.append(error)
                    if isinstance(error, InterruptedError) or not cleanup:
                        blocked = "Prior group was interrupted or owned JVM cleanup is unverified: " + group["id"]
                    elif provenance(root) != report["provenance"]:
                        blocked = "Source/run provenance changed after failed group: " + group["id"]
                    # Continue safe, independently bounded groups only after this supervisor proves cleanup.
                write_report(path, report)
            if failures:
                report["groupFailures"] = [entry["id"] for entry in report["groups"] if entry["status"] != "passed"]
                raise failures[0]
            remaining(deadline)
            counter_paths = [base / group["id"] for group in GROUPS if group["suite"] == s.SUITE]
            report["counterProfileUnion"] = s.validate_counter_profile_union(counter_paths)
            remaining(deadline)
            s.require(report["counterProfileUnion"]["sourceHead"] == report["provenance"]["sourceHead"],
                      "Counter union source differs from matrix provenance")
            s.require(provenance(root) == report["provenance"], "Matrix provenance changed during execution")
            s.require(len(nonces) == len(GROUPS) and all(g["status"] == "passed" for g in report["groups"]), "Matrix is incomplete")
            remaining(deadline)
            report.update(status="passed", releaseImageDamageOrderVerified=True, counterOwnerPeerGeometryVerified=True)
    except BaseException as error:
        report["error"] = type(error).__name__ + ": " + str(error)
        for entry in report["groups"]:
            if entry["status"] == "running":
                entry.update(status="failed" if entry.get("executed") else "unverified", error="Matrix stopped before verified completion: " + type(error).__name__)
            elif entry["status"] == "not_started":
                entry.update(status="unverified", executed=False, error="Matrix stopped before safe launch: " + type(error).__name__)
        raise
    finally:
        for signum in old_handlers:
            signal.signal(signum, signal.SIG_IGN)
        report["seconds"] = round(time.monotonic() - started, 3)
        write_report(path, report)
        for signum, handler in old_handlers.items():
            signal.signal(signum, handler)
    return report


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=Path("build/native/paired-matrix"))
    parser.add_argument("--total-timeout", type=int, default=MAX_TOTAL_SECONDS)
    parser.add_argument("--accepted-eula", type=Path)
    options = parser.parse_args(argv)
    if not 1 <= options.total_timeout <= MAX_TOTAL_SECONDS:
        parser.error("Total elapsed budget must be 1.." + str(MAX_TOTAL_SECONDS) + " seconds")
    return options


if __name__ == "__main__":
    try:
        run(parse_args())
    except (ValueError, RuntimeError, OSError, subprocess.SubprocessError) as error:
        print("Paired native matrix failed: " + str(error), file=sys.stderr)
        raise SystemExit(1)
