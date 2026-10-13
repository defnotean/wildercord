"""Owned Windows VPS build/focused checks with exact source and retained evidence.

This runner does not establish full-client or multiplayer acceptance. It shares
the existing two-client checkout lock so compilation cannot overlap its clients.
GitHub Actions is deliberately not an execution provider for this command.
"""
from __future__ import annotations

import argparse
import ctypes
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import uuid
import xml.etree.ElementTree as ET

from client_suites import DESCRIPTOR, ROOT, _whole_class_completion, scene_marker
from client_shard_plan import read_json
from native.launch_two_clients import supervisor_lock
from run_client_ci import BackendFailures
from native_evidence import read_text as read_completed_log

MAX_LOG = 32 * 1024 * 1024
JOB_HANDLE = None


def own_windows_job():
    """Put this runner and future children in a kill-on-close Windows job.

    The handle intentionally stays open until process exit. Closing it early
    would kill this runner too. Windows then closes it even on SSH interruption,
    ending only processes descended from this runner, never the live server.
    """
    if os.name != "nt":
        raise ValueError("This runner requires the configured Windows VPS")
    from ctypes import wintypes as w

    class BasicLimits(ctypes.Structure):
        _fields_ = [("processTime", ctypes.c_int64), ("jobTime", ctypes.c_int64),
                    ("flags", w.DWORD), ("minWorkingSet", ctypes.c_size_t),
                    ("maxWorkingSet", ctypes.c_size_t), ("activeProcesses", w.DWORD),
                    ("affinity", ctypes.c_size_t), ("priority", w.DWORD), ("scheduling", w.DWORD)]

    class IoCounters(ctypes.Structure):
        _fields_ = [(name, ctypes.c_uint64) for name in
                    ("readOps", "writeOps", "otherOps", "readBytes", "writeBytes", "otherBytes")]

    class ExtendedLimits(ctypes.Structure):
        _fields_ = [("basic", BasicLimits), ("io", IoCounters),
                    ("processMemory", ctypes.c_size_t), ("jobMemory", ctypes.c_size_t),
                    ("peakProcessMemory", ctypes.c_size_t), ("peakJobMemory", ctypes.c_size_t)]

    kernel = ctypes.WinDLL("kernel32", use_last_error=True)
    kernel.CreateJobObjectW.argtypes = [ctypes.c_void_p, w.LPCWSTR]
    kernel.CreateJobObjectW.restype = w.HANDLE
    kernel.SetInformationJobObject.argtypes = [w.HANDLE, ctypes.c_int, ctypes.c_void_p, w.DWORD]
    kernel.SetInformationJobObject.restype = w.BOOL
    kernel.GetCurrentProcess.restype = w.HANDLE
    kernel.AssignProcessToJobObject.argtypes = [w.HANDLE, w.HANDLE]
    kernel.AssignProcessToJobObject.restype = w.BOOL
    kernel.CloseHandle.argtypes = [w.HANDLE]
    kernel.CloseHandle.restype = w.BOOL
    handle = kernel.CreateJobObjectW(None, None)
    if not handle:
        raise ctypes.WinError(ctypes.get_last_error())
    limits = ExtendedLimits()
    limits.basic.flags = 0x2000  # JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE
    if not kernel.SetInformationJobObject(handle, 9, ctypes.byref(limits), ctypes.sizeof(limits)):
        error = ctypes.get_last_error()
        kernel.CloseHandle(handle)
        raise ctypes.WinError(error)
    if not kernel.AssignProcessToJobObject(handle, kernel.GetCurrentProcess()):
        error = ctypes.get_last_error()
        kernel.CloseHandle(handle)
        raise ctypes.WinError(error)
    global JOB_HANDLE
    JOB_HANDLE = handle


def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True, timeout=20).strip()


def source_identity(expected):
    if not re.fullmatch(r"[0-9a-f]{40}", expected):
        raise ValueError("Expected head must be a full commit SHA")
    head = git("rev-parse", "HEAD")
    if head != expected or git("status", "--porcelain"):
        raise ValueError("Checks require the exact clean committed candidate")
    return head


def native_evidence(log, suite, launch_id, processed):
    """A successful exit alone cannot certify that the selected class ran."""
    issues = []
    entries = processed.get("entrypoints", {}).get("fabric-client-gametest")
    if entries != [suite]:
        issues.append("Processed descriptor differs from the one requested class")
    completed, lifecycle = _whole_class_completion(log, [suite], "VPS focused class", launch_id=launch_id)
    issues.extend(lifecycle)
    if len(re.findall(r"^BUILD SUCCESSFUL(?: in .+)?$", log, re.MULTILINE)) != 1 or "BUILD FAILED" in log:
        issues.append("Missing unique successful Gradle completion")
    if "[TEST SKIP]" in log:
        issues.append("A skipped class is not focused gameplay acceptance")
    boundaries = []
    for line in log.splitlines():
        try:
            if scene_marker(line, launch_id=launch_id) is not None:
                boundaries.append("scene")
        except (ValueError, KeyError, TypeError, OverflowError):
            boundaries.append("invalid-scene")
        if re.fullmatch(r"BUILD SUCCESSFUL(?: in .+)?", line):
            boundaries.append("build")
    if boundaries != ["scene"] * 4 + ["build"]:
        issues.append("Class completion must precede successful build completion")
    return {"completedEntries": completed, "verificationIssues": issues}


def unit_results(directory):
    reports = sorted(directory.glob("TEST-*.xml"))
    if not reports:
        raise ValueError("No JUnit reports were produced")
    totals = dict.fromkeys(("tests", "failures", "errors", "skipped"), 0)
    for path in reports:
        root = ET.parse(path).getroot()
        if root.tag != "testsuite":
            raise ValueError("Unexpected JUnit report root")
        for field in totals:
            count = int(root.attrib[field])
            if count < 0:
                raise ValueError("Negative JUnit count")
            totals[field] += count
    if not totals["tests"] or any(totals[field] for field in ("failures", "errors", "skipped")):
        raise ValueError("JUnit did not complete with all tests passing: " + str(totals))
    return totals


def invoke(command, log, environment, deadline):
    remaining = deadline - time.monotonic()
    if remaining <= 0:
        raise TimeoutError("VPS check elapsed deadline exhausted")
    with log.open("xb") as output:
        process = subprocess.Popen(command, cwd=ROOT, env=environment, stdin=subprocess.DEVNULL,
                                   stdout=output, stderr=subprocess.STDOUT,
                                   creationflags=subprocess.CREATE_NO_WINDOW)
        # On timeout the caller writes failure evidence and exits. Its Windows
        # job then closes all descendants, including no-daemon Gradle children.
        failures, offset, tail = BackendFailures(), 0, ""
        while True:
            remaining = deadline - time.monotonic()
            if remaining <= 0:
                raise TimeoutError("VPS check elapsed deadline exhausted")
            try:
                code = process.wait(timeout=min(1, remaining))
                break
            except subprocess.TimeoutExpired:
                if log.stat().st_size > MAX_LOG:
                    raise ValueError("Command log exceeds its bounded verification size")
                if log.name == "native.log":
                    with log.open("rb") as observer:
                        observer.seek(offset)
                        chunk = observer.read(1024 * 1024)
                        offset = observer.tell()
                    tail = (tail + chunk.decode("utf-8", errors="replace"))[-1024 * 1024:]
                    if failures.observe(tail):
                        raise RuntimeError("Both native graphics backends failed; no gameplay acceptance")
                    tail = tail[-1024:]
    if code != 0:
        raise RuntimeError(f"Command exited {code}; see {log.name}")
    if log.stat().st_size > MAX_LOG:
        raise ValueError("Command log exceeds its bounded verification size")
    return {"command": command, "pid": process.pid, "exit": code, "log": log.name,
            "logSha256": hashlib.sha256(log.read_bytes()).hexdigest()}


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mode", choices=("build", "focused"))
    parser.add_argument("--head", required=True)
    parser.add_argument("--computer", required=True, help="Expected VPS COMPUTERNAME")
    parser.add_argument("--suite", help="One registered native class for focused mode")
    parser.add_argument("--accepted-eula", type=Path, help="Previously accepted server EULA file")
    parser.add_argument("--timeout", type=int, default=3600, help="Total seconds; maximum 10800")
    options = parser.parse_args(argv)
    if not 1 <= options.timeout <= 10800:
        parser.error("Timeout must be between 1 and 10800 seconds")
    if os.environ.get("COMPUTERNAME", "").casefold() != options.computer.casefold():
        parser.error("This is not the requested VPS computer")
    if any(key.startswith("GITHUB_") for key in os.environ):
        parser.error("GitHub Actions/environment cannot run the VPS checks")
    overrides = [key for key in os.environ if key.startswith("WILDERCORD_") or key in
                 {"JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "GRADLE_OPTS", "JAVA_OPTS"}]
    if overrides:
        parser.error("Inherited test filters or JVM injections are not accepted: " + ", ".join(sorted(overrides)))
    if options.mode == "focused":
        if options.suite not in read_json(DESCRIPTOR)["entrypoints"]["fabric-client-gametest"]:
            parser.error("Focused suite must name one registered client class")
        if options.accepted_eula is None or not any(line.strip().lower() == "eula=true" for line in
                options.accepted_eula.read_text(encoding="utf-8-sig").splitlines()):
            parser.error("Focused native runs need an existing accepted EULA file")
    elif options.suite or options.accepted_eula:
        parser.error("Build mode does not accept native selectors or EULA inputs")

    own_windows_job()
    with supervisor_lock(ROOT):
        head = source_identity(options.head)
        launch = str(uuid.uuid4())
        evidence = ROOT / "artifacts/review/vps" / launch
        evidence.mkdir(parents=True, exist_ok=False)
        report = {"schemaVersion": 1, "executor": "windows-vps", "computer": options.computer,
                  "head": head, "launchId": launch, "mode": options.mode, "suite": options.suite,
                  "started": datetime.now(timezone.utc).isoformat(), "runnerPid": os.getpid(),
                  "status": "running", "fullClientGate": "unverified", "commands": []}
        result = evidence / "result.json"
        result.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
        print(str(result), flush=True)
        environment = os.environ.copy()
        deadline = time.monotonic() + options.timeout
        gradle = [str(ROOT / "gradlew.bat")]
        flags = ["--no-daemon", "--max-workers=2", "--console=plain"]
        try:
            if options.mode == "build":
                commands = [("build", gradle + ["build", "compileGametestJava", *flags]),
                            ("assets", [sys.executable, "tools/check_generated_assets.py"]),
                            ("guide", [sys.executable, "tools/player_docs.py", "--check"])]
            else:
                environment["WILDERCORD_FULL_CLIENT_LAUNCH_ID"] = launch
                environment.setdefault("ALSOFT_DRIVERS", "null")
                report["eulaSource"] = str(options.accepted_eula.resolve())
                report["eulaSha256"] = hashlib.sha256(options.accepted_eula.read_bytes()).hexdigest()
                commands = [("native", gradle + ["runClientGameTest", "-PfocusedSuite=" + options.suite,
                                                "-Pwildercord.eulaAccepted=true", *flags])]
            for name, command in commands:
                report["step"] = name
                result.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
                report["commands"].append(invoke(command, evidence / (name + ".log"), environment, deadline))
            source_identity(head)
            if options.mode == "build":
                report["unitTests"] = unit_results(ROOT / "build/test-results/test")
            else:
                log_path = evidence / "native.log"
                if log_path.stat().st_size > MAX_LOG:
                    raise ValueError("Native log exceeds its bounded verification size")
                report.update(native_evidence(read_completed_log(log_path, MAX_LOG),
                    options.suite, launch, read_json(ROOT / "build/resources/gametest/fabric.mod.json")))
                if report["verificationIssues"]:
                    raise ValueError("Incomplete focused native evidence")
            report["status"] = "passed"
        except Exception as error:
            report.update(status="failed", error=type(error).__name__ + ": " + str(error))
        finally:
            report["finished"] = datetime.now(timezone.utc).isoformat()
            result.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
        print(json.dumps(report), flush=True)
        return 0 if report["status"] == "passed" else 1


if __name__ == "__main__":
    sys.exit(main())
