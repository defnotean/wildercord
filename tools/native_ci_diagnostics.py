"""Read-only, bounded diagnostics for the Linux process tree a CI launcher owns.

Never inspect a global process list, command lines or environments. Diagnostics are
observations only: missing stacks or markers do not establish a pass or fail.
"""
from dataclasses import dataclass
import json
import os
from pathlib import Path
import selectors
import subprocess
import tempfile
import threading
import time

SCENE_PREFIX = "WILDERCORD_NATIVE_SCENE "
DIAGNOSTIC_PREFIX = "WILDERCORD_NATIVE_DIAGNOSTIC "
HEARTBEAT_SECONDS = 60
MAX_TARGETS = 4
MAX_STACK_BYTES = 256 * 1024
ATTACH_SECONDS = 10


def snapshot_thresholds(selection):
    # Leave headroom for setup and the existing 60/180 minute workflow limits.
    return (30 * 60, 45 * 60) if selection["kind"] == "suite" else (90 * 60, 150 * 60)


@dataclass(frozen=True)
class ProcessIdentity:
    pid: int
    parent: int
    group: int
    session: int
    started: int


def process_identity(pid, proc=Path("/proc")):
    try:
        # comm can contain spaces and parentheses; only the numeric stat fields matter.
        data = (proc / str(pid) / "stat").read_text()
        fields = data[data.rindex(")") + 2:].split()
        return ProcessIdentity(pid, int(fields[1]), int(fields[2]), int(fields[3]), int(fields[19]))
    except (OSError, ValueError, IndexError):
        return None


def owned_java_processes(root, proc=Path("/proc")):
    """Traverse only verified descendants in the launcher's private session/group."""
    if root is None or process_identity(root.pid, proc) != root:
        return []
    pending, seen, found = [root], set(), []
    while pending and len(seen) < 256:
        parent = pending.pop()
        if parent.pid in seen or process_identity(parent.pid, proc) != parent:
            continue
        seen.add(parent.pid)
        try:
            # A subprocess can be forked by any thread, not just the process leader.
            children = set()
            for task in sorted((proc / str(parent.pid) / "task").iterdir())[:256]:
                try:
                    children.update(int(pid) for pid in (task / "children").read_text().split())
                except (OSError, ValueError):
                    continue
        except OSError:
            continue
        for pid in sorted(children)[:256]:
            child = process_identity(pid, proc)
            if child is None or child.parent != parent.pid or child.group != root.pid or child.session != root.pid:
                continue
            pending.append(child)
            try:
                executable = (proc / str(pid) / "exe").readlink()
                if executable.name == "java":
                    found.append(child)
            except OSError:
                continue
    return sorted(set(found), key=lambda identity: identity.pid)[:MAX_TARGETS]


class SceneProgress:
    def __init__(self, entries, started):
        self.entries = set(entries)
        self.started = started
        self.last_output = started
        self.phase = "gradle/startup"
        self.suite = None
        self.scene_started = None
        self.last_scene = None

    def observe(self, line, now):
        self.last_output = now
        if SCENE_PREFIX not in line:
            return
        try:
            event = json.loads(line.split(SCENE_PREFIX, 1)[1])
        except (ValueError, TypeError):
            return
        if not isinstance(event, dict) or not isinstance(event.get("suite"), str) or event["suite"] not in self.entries:
            return
        suite, action, phase = event["suite"], event.get("event"), event.get("phase")
        if action == "start" and phase == "setup":
            self.suite, self.scene_started, self.phase = suite, now, phase
        elif action == "phase" and suite == self.suite and phase in ("run", "cleanup"):
            self.phase = phase
        elif action == "end" and suite == self.suite and phase in ("returned", "threw"):
            self.last_scene = {"suite": suite, "outcome": phase}
            self.suite, self.scene_started, self.phase = None, None, "between-scenes/gradle"

    def context(self, now):
        return {"elapsedSeconds": round(now - self.started, 3), "phase": self.phase,
                "currentSuite": self.suite,
                "sceneElapsedSeconds": round(now - self.scene_started, 3) if self.scene_started is not None else None,
                "outputQuietSeconds": round(now - self.last_output, 3), "lastScene": self.last_scene}


def capture_thread_stack(identity, root, destination, stop, proc=Path("/proc")):
    """Only Thread.print, with a ten-second/256 KiB cap; never a VM/env/heap dump."""
    # Revalidate ancestry and start time immediately before each attach (PID reuse).
    if identity not in owned_java_processes(root, proc):
        return "ownership-changed"
    try:
        executable = (proc / str(identity.pid) / "exe").readlink()
        jcmd = executable.parent / "jcmd"
        if executable.name != "java" or not jcmd.is_file():
            return "jcmd-unavailable"
        if process_identity(identity.pid, proc) != identity or stop.is_set():
            return "target-ended"
        with destination.open("wb") as output:
            child = subprocess.Popen([str(jcmd), str(identity.pid), "Thread.print", "-l"],
                                     stdin=subprocess.DEVNULL, stdout=subprocess.PIPE,
                                     stderr=subprocess.STDOUT, start_new_session=True)
            deadline, size, result = time.monotonic() + ATTACH_SECONDS, 0, "complete"
            try:
                with selectors.DefaultSelector() as selector:
                    selector.register(child.stdout, selectors.EVENT_READ)
                    while True:
                        if stop.is_set():
                            result = "launcher-ended"
                            break
                        if time.monotonic() >= deadline:
                            result = "attach-timeout"
                            break
                        # Never continue an attach if the verified JVM has ended/reused its PID.
                        if process_identity(identity.pid, proc) != identity:
                            result = "target-ended"
                            break
                        if not selector.select(timeout=0.1):
                            continue
                        data = os.read(child.stdout.fileno(), min(8192, MAX_STACK_BYTES - size))
                        if not data:
                            result = "complete" if child.wait(timeout=1) == 0 else "attach-failed"
                            break
                        output.write(data)
                        size += len(data)
                        if size >= MAX_STACK_BYTES:
                            result = "stack-byte-limit"
                            break
            finally:
                # Only the short-lived diagnostic command is ever stopped here, never the JVM.
                if child.poll() is None:
                    child.kill()
                child.wait(timeout=2)
                child.stdout.close()
            return result
    except (OSError, subprocess.SubprocessError):
        return "attach-unavailable"


class NativeDiagnostics:
    def __init__(self, process, selection, record,
                 directory=Path("build/run/clientGameTest/logs/ci-diagnostics"), clock=time.monotonic):
        self.process, self.record, self.directory, self.clock = process, record, directory, clock
        self.root = process_identity(process.pid)
        if self.root is not None and (self.root.group != process.pid or self.root.session != process.pid):
            self.root = None
        self.started = clock()
        self.progress = SceneProgress(selection["entries"], self.started)
        self.thresholds = snapshot_thresholds(selection)
        self.next_heartbeat = HEARTBEAT_SECONDS
        self.sampled = set()
        self.output_directory = None
        self.lock = threading.Lock()
        self.stop = threading.Event()
        self.thread = threading.Thread(target=self._watch, name="native-ci-diagnostics", daemon=True)

    def emit(self, event, now, **details):
        with self.lock:
            context = self.progress.context(now)
        self.record(DIAGNOSTIC_PREFIX + json.dumps({"event": event, **context, **details}, sort_keys=True))

    def observe(self, line):
        with self.lock:
            self.progress.observe(line, self.clock())

    def start(self):
        self.emit("watch-start", self.started, snapshotSeconds=self.thresholds)
        try:
            self.thread.start()
        except (OSError, RuntimeError) as exc:
            self.emit("diagnostic-unavailable", self.clock(), errorType=type(exc).__name__)

    def close(self):
        self.stop.set()
        if self.thread.ident is not None:
            self.thread.join()

    def poll(self, now):
        elapsed = now - self.started
        if elapsed >= self.next_heartbeat:
            self.next_heartbeat = (int(elapsed // HEARTBEAT_SECONDS) + 1) * HEARTBEAT_SECONDS
            self.emit("heartbeat", now)
        for threshold in self.thresholds:
            if elapsed < threshold or threshold in self.sampled or self.stop.is_set():
                continue
            self.sampled.add(threshold)
            self.emit("snapshot-start", now, thresholdSeconds=threshold)
            targets = owned_java_processes(self.root)
            if not targets:
                self.emit("snapshot-unavailable", self.clock(), reason="no-owned-java", thresholdSeconds=threshold)
                continue
            if self.output_directory is None:
                self.directory.mkdir(parents=True, exist_ok=True)
                self.output_directory = Path(tempfile.mkdtemp(prefix="run-", dir=self.directory))
            for target in targets:
                if self.stop.is_set():
                    break
                destination = self.output_directory / f"after-{threshold}s-pid-{target.pid}.txt"
                outcome = capture_thread_stack(target, self.root, destination, self.stop)
                self.emit("snapshot-target", self.clock(), thresholdSeconds=threshold, pid=target.pid,
                          outcome=outcome, stackFile=str(destination) if destination.exists() else None)

    def _watch(self):
        while not self.stop.wait(1):
            if self.process.poll() is not None:
                return
            try:
                self.poll(self.clock())
            except Exception as exc:
                # A missing /proc, attach tool or artifact directory cannot change the test outcome.
                self.emit("diagnostic-unavailable", self.clock(), errorType=type(exc).__name__)
