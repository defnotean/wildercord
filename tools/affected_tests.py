#!/usr/bin/env python3
"""Static impact analysis for native client game tests.

Selects the registered ``fabric-client-gametest`` classes whose static class
closure reaches a changed class, so a developer re-runs only those in one client
launch (``./gradlew runClientGameTest -PaffectedSince=<ref>``).

The graph is jdeps ``-verbose:class`` bytecode edges over build/classes/java/
{main,client,gametest}, plus conservative source-import edges (javac inlines
compile-time constants, which leaves no bytecode reference). Resource, build and
mixin changes select every test. This is a fast iteration aid only: shared
state, events, registries and timing are invisible to static dependencies, so the
full CI suites stay the release gate.
"""
from __future__ import annotations

import argparse
from collections import deque
import fnmatch
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import struct
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
SOURCE_SETS = ("main", "client", "gametest")
ENTRYPOINT = "fabric-client-gametest"
CACHE_VERSION = 1

# Paths that cannot change client game test behaviour. Anything unlisted that is
# not Java source under a client source set selects every test.
NONE_PATTERNS = (
    ("docs/*", "documentation"), ("wiki/*", "wiki"), ("gitbook/*", "documentation"),
    ("tools/*", "tooling"), (".github/*", "CI workflow/tooling"), ("src/test/*", "JUnit-only sources"),
    ("*.md", "documentation"), ("LICENSE", "license"), (".gitignore", "git metadata"),
    (".gitattributes", "git metadata"), (".editorconfig", "editor settings"), (".gitbook.yaml", "documentation"),
)
ALL_PATTERNS = (
    ("build.gradle", "build script changes the launched client"),
    ("settings.gradle", "build settings change the launched client"),
    ("gradle.properties", "versions/properties change the launched client"),
    ("gradle/*", "Gradle wrapper changes the build"), ("gradlew*", "Gradle wrapper changes the build"),
    ("src/*/resources/fabric.mod.json", "mod descriptor (entrypoints, mixins, dependencies)"),
    ("src/*/resources/*.mixins.json", "mixin configuration applies to every run"),
    ("src/*/resources/*.accesswidener", "access widener applies to every run"),
    ("src/*/resources/*", "resources (assets, lang, data) are loaded by every client run; "
                          "no texture/model mapping is narrow enough to trust"),
)


# ---------------------------------------------------------------- change classification

def classify_path(path: str) -> tuple[str, str]:
    """Return (kind, reason): kind is 'java', 'all' or 'none'."""
    p = path.replace("\\", "/")
    parts = p.split("/")
    if len(parts) > 3 and parts[0] == "src" and parts[1] in SOURCE_SETS and parts[2] == "java":
        if p.endswith(".java"):
            return "java", "Java source"
        return "all", "non-Java file inside a client source set"
    for pattern, reason in NONE_PATTERNS:
        if fnmatch.fnmatchcase(p, pattern):
            return "none", reason
    for pattern, reason in ALL_PATTERNS:
        if fnmatch.fnmatchcase(p, pattern):
            return "all", reason
    return "all", "unclassified path; assumed to affect every client run"


def java_path_parts(path: str) -> tuple[str, str, str] | None:
    """('main', 'dev/wildercord/x', 'Foo.java') for src/main/java/dev/wildercord/x/Foo.java."""
    parts = path.replace("\\", "/").split("/")
    if len(parts) < 5 or parts[0] != "src" or parts[1] not in SOURCE_SETS or parts[2] != "java":
        return None
    return parts[1], "/".join(parts[3:-1]), parts[-1]


# ---------------------------------------------------------------- pure selection logic

def reverse_edges(graph: dict[str, list[str]]) -> dict[str, list[str]]:
    rev: dict[str, list[str]] = {}
    for src, targets in graph.items():
        for dst in targets:
            rev.setdefault(dst, []).append(src)
    return rev


def toward_changed(graph: dict[str, list[str]], changed: set[str]) -> dict[str, str | None]:
    """Multi-source reverse BFS: node -> next hop on a shortest path to a changed class."""
    rev = reverse_edges(graph)
    nxt: dict[str, str | None] = {c: None for c in changed}
    queue = deque(sorted(changed))
    while queue:
        node = queue.popleft()
        for pred in rev.get(node, ()):
            if pred not in nxt:
                nxt[pred] = node
                queue.append(pred)
    return nxt


def path_to_changed(nxt: dict[str, str | None], start: str) -> list[str]:
    path = [start]
    while nxt[path[-1]] is not None:
        path.append(nxt[path[-1]])
    return path


def reachable(graph: dict[str, list[str]], roots) -> dict[str, str | None]:
    """Forward BFS: reached node -> parent (None for roots)."""
    parent: dict[str, str | None] = {r: None for r in sorted(roots)}
    queue = deque(parent)
    while queue:
        node = queue.popleft()
        for dst in graph.get(node, ()):
            if dst not in parent:
                parent[dst] = node
                queue.append(dst)
    return parent


def path_from_root(parent: dict[str, str | None], node: str) -> list[str]:
    path = [node]
    while parent[path[-1]] is not None:
        path.append(parent[path[-1]])
    return path[::-1]


def select_tests(tests: list[str], graph: dict[str, list[str]], changes: list[dict],
                 runtime_roots=(), always_all=(), strict=False) -> dict:
    """Pure selection over a class graph.

    changes: [{path, kind: java|all|none, reason, classes: [...]}]
    runtime_roots: classes the loader reaches without a test reference (mod
      entrypoints, mixins). A changed class reached only from these selects all.
    always_all: changed classes that select everything (mixins, mod entrypoints).
    strict: also select all when a changed class is reachable from runtime_roots,
      since the running game may call it for any test (renderers, events).
    """
    reasons: list[str] = []
    warnings: list[str] = []
    changed: set[str] = set()
    for change in changes:
        if change["kind"] == "all":
            reasons.append(f"{change['path']}: {change['reason']}")
        elif change["kind"] == "java":
            if not change.get("classes"):
                warnings.append(f"{change['path']}: no compiled classes (deleted, or build is stale); "
                                "its dependents were changed too if it was removed")
            changed.update(change.get("classes", ()))
    always = set(always_all)
    for cls in sorted(changed & always):
        reasons.append(f"{cls}: mixin or mod entrypoint class applies to every run")
    nxt = toward_changed(graph, changed)
    explanations = {t: path_to_changed(nxt, t) for t in tests if t in nxt}
    hit = {p[-1] for p in explanations.values()}
    unreached = sorted(changed - hit)
    runtime = reachable(graph, runtime_roots) if changed else {}
    hooked = {}
    for cls in sorted(changed - always):
        if cls in runtime:
            hooked[cls] = path_from_root(runtime, cls)
    for cls in unreached:
        if cls in hooked:
            reasons.append(f"{cls}: reached only through runtime hooks (mod entrypoints/mixins), "
                           "not from any test class")
        elif cls not in always:
            warnings.append(f"{cls}: not statically reached by any test or runtime root")
    reached_hooked = [c for c in hooked if c in hit]
    if reached_hooked and strict:
        reasons += [f"{c}: strict mode; reachable from runtime hooks" for c in reached_hooked]
    elif reached_hooked and len(explanations) < len(tests):
        warnings.append(f"{len(reached_hooked)} changed class(es) are also reachable from mod entrypoints/mixins "
                        f"(e.g. {' -> '.join(hooked[reached_hooked[0]])}); tests that exercise them only through "
                        "the running game (rendering, events, registries) are not selected. Use --strict to select all.")
    if reasons:
        return {"mode": "all", "selected": list(tests), "reasons": reasons, "warnings": warnings,
                "explanations": {}, "changedClasses": sorted(changed), "total": len(tests),
                "runtimePaths": hooked}
    selected = [t for t in tests if t in explanations]
    return {"mode": "selected" if selected else "none", "selected": selected, "reasons": [],
            "warnings": warnings, "explanations": {t: explanations[t] for t in selected},
            "changedClasses": sorted(changed), "total": len(tests), "runtimePaths": hooked}


# ---------------------------------------------------------------- class files

def class_info(data: bytes) -> tuple[str, str | None]:
    """(binary class name, SourceFile attribute) from a class file."""
    if data[:4] != b"\xca\xfe\xba\xbe":
        raise ValueError("not a class file")
    count = struct.unpack_from(">H", data, 8)[0]
    utf8: dict[int, str] = {}
    classes: dict[int, int] = {}
    pos, i = 10, 1
    while i < count:
        tag = data[pos]
        if tag == 1:
            n = struct.unpack_from(">H", data, pos + 1)[0]
            utf8[i] = data[pos + 3:pos + 3 + n].decode("utf-8", "replace")
            pos += 3 + n
        elif tag == 7:
            classes[i] = struct.unpack_from(">H", data, pos + 1)[0]
            pos += 3
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            pos += 5
        elif tag in (5, 6):
            pos += 9
            i += 1
        elif tag in (8, 16, 19, 20):
            pos += 3
        elif tag == 15:
            pos += 4
        else:
            raise ValueError(f"unknown constant tag {tag}")
        i += 1
    this_class = struct.unpack_from(">H", data, pos + 2)[0]
    name = utf8[classes[this_class]].replace("/", ".")
    pos += 6
    pos += 2 + 2 * struct.unpack_from(">H", data, pos)[0]

    for _ in range(2):  # fields, methods
        members = struct.unpack_from(">H", data, pos)[0]
        pos += 2
        for _ in range(members):
            pos += 6
            attrs = struct.unpack_from(">H", data, pos)[0]
            pos += 2
            for _ in range(attrs):
                pos += 6 + struct.unpack_from(">I", data, pos + 2)[0]
    attrs = struct.unpack_from(">H", data, pos)[0]
    pos += 2
    for _ in range(attrs):
        attr_name = utf8.get(struct.unpack_from(">H", data, pos)[0])
        length = struct.unpack_from(">I", data, pos + 2)[0]
        if attr_name == "SourceFile":
            return name, utf8.get(struct.unpack_from(">H", data, pos + 6)[0])
        pos += 6 + length
    return name, None


def scan_classes(build_dir: Path) -> dict[str, dict]:
    """class name -> {set, file (class path), source (src/<set>/java/... path)}."""
    out: dict[str, dict] = {}
    for source_set in SOURCE_SETS:
        base = build_dir / "classes/java" / source_set
        if not base.is_dir():
            continue
        for cls in base.rglob("*.class"):
            if cls.name in ("module-info.class", "package-info.class"):
                continue
            name, source_file = class_info(cls.read_bytes())
            package_dir = cls.parent.relative_to(base).as_posix()
            if source_file is None:  # synthetic: fall back to the outer class name
                source_file = cls.name.split("$")[0].removesuffix(".class") + ".java"
            source = "/".join(x for x in ("src", source_set, "java", package_dir, source_file) if x and x != ".")
            out[name] = {"set": source_set, "source": source}
    return out


# ---------------------------------------------------------------- source edges

IMPORT_RE = re.compile(r"^\s*import\s+(static\s+)?([\w.]+?)(\.\*)?\s*;", re.M)
PACKAGE_RE = re.compile(r"^\s*package\s+([\w.]+)\s*;", re.M)
COMMENT_RE = re.compile(r"//[^\n]*|/\*.*?\*/", re.S)
IDENT_RE = re.compile(r"[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)*")


def source_edges(root: Path, classes: dict[str, dict]) -> dict[str, list[str]]:
    """Over-approximate source references (covers javac-inlined constants)."""
    top_level = {n for n in classes if "$" not in n}
    by_package: dict[str, dict[str, str]] = {}
    for name in top_level:
        pkg, _, simple = name.rpartition(".")
        by_package.setdefault(pkg, {})[simple] = name
    file_owner: dict[str, str] = {}
    for name, info in classes.items():
        owner = name.split("$")[0]
        if info["source"] not in file_owner or owner == name:
            file_owner[info["source"]] = owner if owner in classes else name
    edges: dict[str, list[str]] = {}
    for rel, owner in file_owner.items():
        path = root / rel
        if not path.is_file():
            continue
        text = path.read_text(encoding="utf-8", errors="replace")
        package = (PACKAGE_RE.search(text) or [None, ""])[1]
        refs: set[str] = set()
        visible = dict(by_package.get(package, {}))
        for static, target, star in IMPORT_RE.findall(text):
            if star and not static and target in by_package:
                visible.update(by_package[target])
                continue
            parts = target.split(".")
            for k in range(len(parts), 0, -1):
                candidate = ".".join(parts[:k])
                if candidate in top_level:
                    refs.add(candidate)
                    break
        body = COMMENT_RE.sub(" ", text)
        for token in set(IDENT_RE.findall(body)):
            head = token.split(".")[0]
            if head in visible:
                refs.add(visible[head])
            if token.startswith("dev."):
                parts = token.split(".")
                for k in range(len(parts), 1, -1):
                    if ".".join(parts[:k]) in top_level:
                        refs.add(".".join(parts[:k]))
                        break
        refs.discard(owner)
        if refs:
            edges[owner] = sorted(refs)
    return edges


# ---------------------------------------------------------------- jdeps

DEP_RE = re.compile(r"^\s+(\S+)\s+->\s+(\S+)")


def parse_jdeps(text: str, known: set[str]) -> dict[str, list[str]]:
    """Parse ``jdeps -verbose:class`` output, keeping project-to-project class edges."""
    edges: dict[str, set[str]] = {}
    for line in text.splitlines():
        match = DEP_RE.match(line)
        if not match:
            continue
        src, dst = match.groups()
        if src in known and dst in known and src != dst:
            edges.setdefault(src, set()).add(dst)
    return {k: sorted(v) for k, v in edges.items()}


def find_jdeps(explicit: str | None) -> str:
    candidates = []
    if explicit:
        candidates.append(explicit)
    if os.environ.get("JDEPS"):
        candidates.append(os.environ["JDEPS"])
    if os.environ.get("JAVA_HOME"):
        candidates.append(str(Path(os.environ["JAVA_HOME"]) / "bin/jdeps"))
    for base in (Path("C:/Program Files/Eclipse Adoptium"), Path.home() / ".gradle/jdks"):
        if base.is_dir():
            candidates += [str(p / "bin/jdeps") for p in sorted(base.glob("jdk-25*"), reverse=True)]
    if shutil.which("jdeps"):
        candidates.append(shutil.which("jdeps"))
    for candidate in candidates:
        for exe in (candidate, candidate + ".exe"):
            if not Path(exe).is_file():
                continue
            try:
                version = subprocess.run([exe, "--version"], capture_output=True, text=True, timeout=30).stdout
                if int(version.strip().split(".")[0]) >= 25:
                    return exe
            except (OSError, ValueError, subprocess.SubprocessError):
                continue
    raise SystemExit("No JDK 25+ jdeps found; pass --jdeps or set JAVA_HOME to the project's Java 25 JDK.")


def fingerprint(root: Path, build_dir: Path) -> str:
    digest = hashlib.sha256(str(CACHE_VERSION).encode())
    for source_set in SOURCE_SETS:
        for base in (build_dir / "classes/java" / source_set, root / "src" / source_set / "java"):
            if not base.is_dir():
                continue
            for path in sorted(base.rglob("*")):
                if path.suffix in (".class", ".java"):
                    stat = path.stat()
                    digest.update(f"{path.relative_to(root).as_posix()}\0{stat.st_mtime_ns}\0{stat.st_size}\n".encode())
    return digest.hexdigest()


def load_graph(root: Path, build_dir: Path, jdeps: str | None, log) -> dict:
    dirs = [build_dir / "classes/java" / s for s in SOURCE_SETS]
    missing = [d for d in dirs if not d.is_dir()]
    if missing:
        raise SystemExit("Compiled classes missing: " + ", ".join(str(d) for d in missing)
                         + "\nRun ./gradlew compileJava compileClientJava compileGametestJava first.")
    key = fingerprint(root, build_dir)
    cache = build_dir / "affected-tests/graph.json"
    try:
        cached = json.loads(cache.read_text(encoding="utf-8"))
        if cached.get("key") == key:
            return cached
    except (OSError, ValueError):
        pass
    log("Building class dependency graph (jdeps)...")
    classes = scan_classes(build_dir)
    tool = find_jdeps(jdeps)
    run = subprocess.run([tool, "-verbose:class", "-filter:none", *map(str, dirs)],
                         capture_output=True, text=True, encoding="utf-8", errors="replace")
    if run.returncode != 0:
        raise SystemExit(f"jdeps failed ({run.returncode}): {run.stderr.strip()[:2000]}")
    bytecode = parse_jdeps(run.stdout, set(classes))
    if classes and not bytecode:
        raise SystemExit("jdeps produced no project class edges; output format not recognised")
    graph = {"key": key, "classes": classes, "bytecode": bytecode, "source": source_edges(root, classes)}
    cache.parent.mkdir(parents=True, exist_ok=True)
    cache.write_text(json.dumps(graph), encoding="utf-8")
    return graph


def merged_graph(graph: dict) -> dict[str, list[str]]:
    merged: dict[str, set[str]] = {}
    for kind in ("bytecode", "source"):
        for src, targets in graph[kind].items():
            merged.setdefault(src, set()).update(targets)
    return {k: sorted(v) for k, v in merged.items()}


# ---------------------------------------------------------------- repository inputs

def git(root: Path, *args: str) -> list[str]:
    run = subprocess.run(["git", *args], cwd=root, capture_output=True, text=True)
    if run.returncode != 0:
        raise SystemExit(f"git {' '.join(args)} failed: {run.stderr.strip()}")
    return [line.strip() for line in run.stdout.splitlines() if line.strip()]


def changed_files(root: Path, since: str) -> list[str]:
    files = git(root, "diff", "--name-only", "--no-renames", since, "--")
    files += git(root, "ls-files", "--others", "--exclude-standard")
    return sorted(set(files))


def registered_tests(descriptor: Path) -> list[str]:
    entries = json.loads(descriptor.read_text(encoding="utf-8")).get("entrypoints", {}).get(ENTRYPOINT)
    if not entries:
        raise SystemExit(f"{descriptor} has no {ENTRYPOINT} entrypoints")
    return [e if isinstance(e, str) else e["value"] for e in entries]


def runtime_hooks(root: Path) -> tuple[set[str], set[str]]:
    """(mod entrypoint classes, mixin classes) declared by the main/client/gametest resources."""
    entry, mixins = set(), set()
    for source_set in SOURCE_SETS:
        resources = root / "src" / source_set / "resources"
        mod = resources / "fabric.mod.json"
        if mod.is_file():
            for name, values in json.loads(mod.read_text(encoding="utf-8")).get("entrypoints", {}).items():
                if name == ENTRYPOINT:
                    continue
                for value in values:
                    entry.add((value if isinstance(value, str) else value["value"]).split("::")[0])
        for config in resources.glob("*.mixins.json") if resources.is_dir() else ():
            data = json.loads(config.read_text(encoding="utf-8"))
            package = data.get("package", "")
            for section in ("mixins", "client", "server"):
                for value in data.get(section, []) or []:
                    mixins.add(f"{package}.{value}" if package else value)
    return entry, mixins


def describe_changes(files: list[str], classes: dict[str, dict]) -> list[dict]:
    by_source: dict[str, list[str]] = {}
    for name, info in classes.items():
        by_source.setdefault(info["source"], []).append(name)
    changes = []
    for path in files:
        kind, reason = classify_path(path)
        change = {"path": path, "kind": kind, "reason": reason}
        if kind == "java":
            change["classes"] = sorted(by_source.get(path.replace("\\", "/"), []))
        changes.append(change)
    return changes


def stale_sources(root: Path, build_dir: Path, changes: list[dict], classes: dict[str, dict]) -> list[str]:
    stale = []
    for change in changes:
        if change["kind"] != "java" or not change.get("classes") or not (root / change["path"]).is_file():
            continue
        src_mtime = (root / change["path"]).stat().st_mtime
        newest = 0.0
        for cls in change["classes"]:
            parts = java_path_parts(change["path"])
            class_file = build_dir / "classes/java" / parts[0] / (cls.replace(".", "/") + ".class")
            if class_file.is_file():
                newest = max(newest, class_file.stat().st_mtime)
        if newest and src_mtime > newest:
            stale.append(change["path"])
    return stale


# ---------------------------------------------------------------- CLI

def format_path(path: list[str], bytecode: dict[str, list[str]]) -> str:
    if len(path) == 1:
        return "(the test class itself changed)"
    text = path[0]
    for src, dst in zip(path, path[1:]):
        text += (" -> " if dst in bytecode.get(src, ()) else " ~> ") + dst
    return text


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument("--since", metavar="REF", help="git ref; diff plus untracked working changes")
    source.add_argument("--files", nargs="+", metavar="PATH", help="explicit changed paths (repo-relative)")
    parser.add_argument("--explain", action="store_true", help="show one dependency path per selected test")
    parser.add_argument("--json", action="store_true", help="machine-readable output")
    parser.add_argument("--build-dir", default="build", help="build output directory (build-alt for -PaltBuild)")
    parser.add_argument("--descriptor", default="src/gametest/resources/fabric.mod.json")
    parser.add_argument("--jdeps", help="path to a JDK 25+ jdeps")
    parser.add_argument("--strict", action="store_true",
                        help="select all when a changed class is also reachable from mod entrypoints/mixins")
    args = parser.parse_args(argv)

    root = ROOT
    build_dir = (root / args.build_dir).resolve()
    log = (lambda msg: print(msg, file=sys.stderr)) if args.json else print
    tests = registered_tests(root / args.descriptor)
    files = changed_files(root, args.since) if args.since else [f.replace("\\", "/") for f in args.files]
    needs_graph = any(classify_path(f)[0] == "java" for f in files)
    graph = load_graph(root, build_dir, args.jdeps, log) if needs_graph else {"classes": {}, "bytecode": {}, "source": {}}
    changes = describe_changes(files, graph["classes"])
    entry, mixins = runtime_hooks(root)
    result = select_tests(tests, merged_graph(graph), changes,
                          runtime_roots=sorted(entry | mixins), always_all=entry | mixins, strict=args.strict)
    result["warnings"] += [f"{p}: source is newer than its classes; recompile for an accurate graph"
                           for p in stale_sources(root, build_dir, changes, graph["classes"])]
    result["changes"] = changes
    result["since"] = args.since

    if args.json:
        print(json.dumps(result, indent=2))
        return 0
    print(f"Changed files ({len(files)}):")
    for change in changes:
        detail = f"{len(change['classes'])} classes" if change["kind"] == "java" else change["reason"]
        print(f"  [{change['kind']}] {change['path']} ({detail})")
    if result["mode"] == "all":
        print(f"Selection: ALL {len(tests)} client game test classes, because:")
        for reason in result["reasons"]:
            print(f"  - {reason}")
    elif result["mode"] == "none":
        print(f"Selection: none of {len(tests)} client game test classes")
    else:
        print(f"Selection: {len(result['selected'])} of {len(tests)} client game test classes")
        for test in result["selected"]:
            print(f"  {test}")
            if args.explain:
                print("      " + format_path(result["explanations"][test], graph["bytecode"]))
        if args.explain:
            print("  ('->' bytecode reference from jdeps, '~>' source-only reference such as an inlined constant)")
    for warning in result["warnings"]:
        print(f"warning: {warning}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
