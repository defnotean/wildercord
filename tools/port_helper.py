"""Developer & agent CLI helpers for cross-version Minecraft porting (Python stdlib only).

Subcommands:
  status        Print active Minecraft/Java profile, descriptor alignment, and discovered Loom JARs
  sync-deps     Resolve matching Fabric Loader + Modrinth dependencies and optionally write pins
  search-class  Case-insensitive class search across the active Loom Minecraft JARs
  inspect       Inspect a Minecraft or compiled mod class via javap with optional member/bytecode filters
  diff-surface  Verify branch diffs remain isolated inside Wildercord's four porting surfaces
"""
from __future__ import annotations

import argparse
import fnmatch
import json
from pathlib import Path
import re
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request

sys.path.insert(0, str(Path(__file__).resolve().parent))
from datapack_profile import DataPackProfile, parse_mc_tuple, read_mc_version
import verify_port

ROOT = Path(__file__).resolve().parent.parent

FABRIC_META_LOADER_URL = "https://meta.fabricmc.net/v2/versions/loader/{mc}"
MODRINTH_PROJECT_VERSIONS_URL = "https://api.modrinth.com/v2/project/{pid}/version"
MODRINTH_PROJECTS = (
    ("fabric-api", "P7dR8mSH", "fabric_api_version"),
    ("sodium", "AANobbMI", "sodium_version"),
    ("iris", "YL57xq9U", "iris_version"),
)

SURFACE_PATTERNS: tuple[tuple[str, tuple[str, ...]], ...] = (
    (
        "build-descriptors",
        (
            "gradle.properties",
            "build.gradle",
            "settings.gradle",
            "profiles/dependencies.lock.json",
            "src/*/resources/fabric.mod.json",
            "src/*/resources/*.mixins.json",
            "src/*/resources/*.accesswidener",
            "gradle/*",
            "gradlew*",
        ),
    ),
    (
        "datapack-and-resources",
        (
            "tools/datapack_profile.py",
            "tools/test_datapack_profile.py",
            "src/main/resources/data/*",
            "src/main/resources/assets/*",
        ),
    ),
    (
        "compat-seam",
        (
            "src/main/java/dev/wildercord/compat/*",
            "src/client/java/dev/wildercord/client/compat/*",
            "src/gametest/java/dev/wildercord/gametest/compat/*",
            "src/test/java/dev/wildercord/TestDataProfile.java",
        ),
    ),
    (
        "mixins",
        (
            "src/*/java/*/mixin/*",
            "src/client/java/dev/wildercord/client/FirstPersonAvatarState.java",
            "src/client/java/dev/wildercord/client/MastersHandMotionState.java",
        ),
    ),
    (
        "shared-docs-and-tools",
        (
            "docs/*",
            "AGENTS.md",
            "tools/*",
            "README.md",
            "wiki/*",
            "gitbook/*",
        ),
    ),
)


def _write_lf(path: Path, text: str) -> None:
    normalized = text.replace("\r\n", "\n")
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write(normalized)


def _fetch_json(url: str):
    req = urllib.request.Request(
        url,
        headers={
            "User-Agent": "wildercord-port-helper/1.0 (+https://github.com/defnotean/wildercord)",
            "Accept": "application/json",
        },
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read().decode("utf-8"))


def target_java_version(mc_version: str) -> str:
    return "25" if parse_mc_tuple(mc_version)[:2] >= (26, 1) else "21"


def _pick_loader_version(mc_version: str, explicit_loader: str | None) -> str:
    if explicit_loader:
        return explicit_loader
    url = FABRIC_META_LOADER_URL.format(mc=urllib.parse.quote(mc_version, safe=""))
    entries = _fetch_json(url)
    if not isinstance(entries, list) or not entries:
        raise RuntimeError(f"Fabric Meta returned no loader versions for Minecraft '{mc_version}'")
    for entry in entries:
        loader = entry.get("loader", {})
        if loader.get("stable"):
            return loader["version"]
    return entries[0]["loader"]["version"]


def _valid_modrinth_candidates(versions: list[dict], mc_version: str) -> list[dict]:
    type_rank = {"release": 0, "beta": 1, "alpha": 2}
    valid = []
    for idx, entry in enumerate(versions):
        if mc_version not in entry.get("game_versions", []):
            continue
        if "fabric" not in entry.get("loaders", []):
            continue
        files = entry.get("files", [])
        jar_file = next((f for f in files if f.get("primary") and f.get("filename", "").endswith(".jar")), None)
        if jar_file is None:
            jar_file = next((f for f in files if f.get("filename", "").endswith(".jar")), None)
        if jar_file is None:
            continue
        hashes = jar_file.get("hashes", {})
        url = jar_file.get("url", "")
        size = jar_file.get("size")
        if (
            not url.startswith("https://cdn.modrinth.com/data/")
            or not re.fullmatch(r"[0-9a-f]{40}", str(hashes.get("sha1", "")))
            or not re.fullmatch(r"[0-9a-f]{128}", str(hashes.get("sha512", "")))
            or not isinstance(size, int)
            or size <= 0
        ):
            continue
        vtype = entry.get("version_type", "alpha")
        valid.append((type_rank.get(vtype, 3), idx, entry, jar_file))
    valid.sort(key=lambda item: (item[0], item[1]))
    return [{"version": item[2], "file": item[3]} for item in valid]


def _query_modrinth_project(project_id: str, mc_version: str) -> list[dict]:
    params = urllib.parse.urlencode({
        "loaders": json.dumps(["fabric"]),
        "game_versions": json.dumps([mc_version]),
    })
    url = MODRINTH_PROJECT_VERSIONS_URL.format(pid=project_id) + "?" + params
    data = _fetch_json(url)
    if not isinstance(data, list):
        raise RuntimeError(f"Unexpected Modrinth response for project {project_id}")
    return _valid_modrinth_candidates(data, mc_version)


def _find_by_version_id(candidates: list[dict], version_id: str) -> dict | None:
    for cand in candidates:
        if cand["version"].get("id") == version_id:
            return cand
    return None


def _select_compatible_triple(fapi_cands: list[dict], sodium_cands: list[dict], iris_cands: list[dict]) -> dict[str, dict]:
    if not fapi_cands:
        raise RuntimeError("No compatible Fabric API release found on Modrinth")
    if not sodium_cands:
        raise RuntimeError("No compatible Sodium release found on Modrinth")
    if not iris_cands:
        raise RuntimeError("No compatible Iris release found on Modrinth")

    allowed_pids = {"P7dR8mSH", "AANobbMI", "YL57xq9U"}
    for iris_item in iris_cands:
        iv = iris_item["version"]
        req_deps = [d for d in iv.get("dependencies", []) if d.get("dependency_type") == "required"]
        if any(d.get("project_id") not in allowed_pids for d in req_deps if d.get("project_id")):
            continue
        sodium_req = [d for d in req_deps if d.get("project_id") == "AANobbMI" or d.get("version_id") in {c["version"]["id"] for c in sodium_cands}]
        if sodium_req and sodium_req[0].get("version_id"):
            chosen_sodium = _find_by_version_id(sodium_cands, sodium_req[0]["version_id"])
            if chosen_sodium is None:
                continue
        else:
            chosen_sodium = sodium_cands[0]

        fapi_req = [d for d in req_deps if d.get("project_id") == "P7dR8mSH"]
        if fapi_req and fapi_req[0].get("version_id"):
            chosen_fapi = _find_by_version_id(fapi_cands, fapi_req[0]["version_id"])
            if chosen_fapi is None:
                continue
        else:
            chosen_fapi = fapi_cands[0]

        chosen = {
            "fabric-api": chosen_fapi,
            "sodium": chosen_sodium,
            "iris": iris_item,
        }
        chosen_ids = {item["version"]["id"] for item in chosen.values()}
        valid_reqs = True
        for item in chosen.values():
            for dep in item["version"].get("dependencies", []):
                if dep.get("dependency_type") != "required":
                    continue
                vid = dep.get("version_id")
                pid = dep.get("project_id")
                if vid and vid not in chosen_ids:
                    valid_reqs = False
                elif pid and pid not in allowed_pids:
                    valid_reqs = False
        if valid_reqs:
            return chosen

    raise RuntimeError("Could not resolve a mutually compatible (fabric-api, sodium, iris) triple")


def resolve_dependencies(mc_version: str, explicit_loader: str | None = None) -> dict:
    parse_mc_tuple(mc_version)
    loader_ver = _pick_loader_version(mc_version, explicit_loader)
    java_ver = target_java_version(mc_version)
    fapi_cands = _query_modrinth_project("P7dR8mSH", mc_version)
    sodium_cands = _query_modrinth_project("AANobbMI", mc_version)
    iris_cands = _query_modrinth_project("YL57xq9U", mc_version)
    chosen = _select_compatible_triple(fapi_cands, sodium_cands, iris_cands)

    selected_ids = {proj: chosen[proj]["version"]["id"] for proj, _, _ in MODRINTH_PROJECTS}
    lock_deps = []
    props_updates = {
        "minecraft_version": mc_version,
        "loader_version": loader_ver,
        "java_version": java_ver,
    }
    for proj, _, prop_name in MODRINTH_PROJECTS:
        v = chosen[proj]["version"]
        f = chosen[proj]["file"]
        props_updates[prop_name] = v["version_number"]
        req_versions = []
        for dep in v.get("dependencies", []):
            if dep.get("dependency_type") != "required":
                continue
            vid = dep.get("version_id")
            if vid:
                req_versions.append(vid)
            elif dep.get("project_id") == "AANobbMI":
                req_versions.append(selected_ids["sodium"])
            elif dep.get("project_id") == "P7dR8mSH":
                req_versions.append(selected_ids["fabric-api"])
        lock_deps.append({
            "project": proj,
            "version": v["version_number"],
            "version_id": v["id"],
            "filename": f["filename"],
            "url": f["url"],
            "size": int(f["size"]),
            "hashes": {
                "sha512": f["hashes"]["sha512"],
                "sha1": f["hashes"]["sha1"],
            },
            "game_versions": list(v["game_versions"]),
            "loaders": list(v["loaders"]),
            "required_versions": req_versions,
        })

    lock = {
        "minecraft": mc_version,
        "fabric_loader": loader_ver,
        "dependencies": lock_deps,
    }
    return {"properties": props_updates, "lock": lock}


def _update_gradle_properties(path: Path, updates: dict[str, str]) -> None:
    lines = path.read_text(encoding="utf-8").splitlines()
    seen = set()
    out = []
    for line in lines:
        stripped = line.strip()
        if stripped and not stripped.startswith("#") and "=" in line:
            key = line.split("=", 1)[0].strip()
            if key in updates:
                out.append(f"{key}={updates[key]}")
                seen.add(key)
                continue
        out.append(line)
    for key, val in updates.items():
        if key not in seen:
            out.append(f"{key}={val}")
    _write_lf(path, "\n".join(out) + "\n")


def _update_fabric_mod_json(path: Path, updates: dict[str, str]) -> None:
    raw = path.read_text(encoding="utf-8")
    data = json.loads(raw)
    deps = data.setdefault("depends", {})
    deps["fabricloader"] = ">=" + updates["loader_version"]
    deps["minecraft"] = "~" + updates["minecraft_version"]
    deps["java"] = ">=" + updates["java_version"]
    deps["fabric-api"] = ">=" + updates["fabric_api_version"]
    indent = "\t" if "\n\t" in raw else 2
    _write_lf(path, json.dumps(data, indent=indent) + "\n")


def _update_mixins_compatibility(root: Path, java_ver: str) -> list[str]:
    expected = f"JAVA_{java_ver}"
    updated = []
    for mf in sorted((root / "src").rglob("*.mixins.json")):
        raw = mf.read_text(encoding="utf-8")
        data = json.loads(raw)
        if data.get("compatibilityLevel") == expected:
            continue
        replaced, count = re.subn(
            r'("compatibilityLevel"\s*:\s*")[^"]*(")',
            rf"\g<1>{expected}\2",
            raw,
            count=1,
        )
        if count == 1:
            _write_lf(mf, replaced if replaced.endswith("\n") else replaced + "\n")
        else:
            data["compatibilityLevel"] = expected
            indent = "\t" if "\n\t" in raw else 2
            _write_lf(mf, json.dumps(data, indent=indent) + "\n")
        updated.append(mf.relative_to(root).as_posix())
    return updated


def apply_dependency_updates(root: Path, resolved: dict) -> list[str]:
    changed = []
    props_path = root / "gradle.properties"
    _update_gradle_properties(props_path, resolved["properties"])
    changed.append("gradle.properties")

    fmod_path = root / "src/main/resources/fabric.mod.json"
    if fmod_path.is_file():
        _update_fabric_mod_json(fmod_path, resolved["properties"])
        changed.append("src/main/resources/fabric.mod.json")

    changed.extend(_update_mixins_compatibility(root, resolved["properties"]["java_version"]))

    lock_path = root / "profiles/dependencies.lock.json"
    lock_path.parent.mkdir(parents=True, exist_ok=True)
    _write_lf(lock_path, json.dumps(resolved["lock"], indent=2) + "\n")
    changed.append("profiles/dependencies.lock.json")
    return changed


def _load_inspector(root: Path, explicit_jars: list[str] | None, include_mod: bool = True) -> verify_port.JarInspector:
    mc_version = read_mc_version(root)
    jars = verify_port.find_mc_jars(root, mc_version, explicit_jars)
    javap = verify_port.find_javap()
    if not jars:
        raise RuntimeError(f"No Minecraft Loom JAR found for {mc_version}; populate Loom cache or pass --mc-jar")
    if not javap:
        raise RuntimeError("javap executable not found; set JAVA_HOME to a JDK installation")
    entries = list(jars)
    if include_mod:
        entries.extend(verify_port.find_mod_class_dirs(root))
    return verify_port.JarInspector(entries, javap)


def search_classes(inspector: verify_port.JarInspector, query: str, limit: int = 30) -> list[str]:
    low = query.strip().replace(".", "/").lower()
    include_anon = "$" in query
    matches = []
    for cls in sorted(inspector.class_entries):
        if not include_anon and re.search(r"\$\d+$", cls):
            continue
        simple = cls.rsplit("/", 1)[-1].rsplit("$", 1)[-1].lower()
        if simple == low:
            rank = 0
        elif cls.lower() == low:
            rank = 0
        elif cls.lower().endswith("/" + low):
            rank = 1
        elif low in simple:
            rank = 2
        elif low in cls.lower():
            rank = 3
        else:
            continue
        matches.append((rank, cls))
    matches.sort(key=lambda item: (item[0], item[1]))
    dotted = [item[1].replace("/", ".") for item in matches]
    return dotted if limit <= 0 else dotted[:limit]


def resolve_single_class(inspector: verify_port.JarInspector, query: str) -> tuple[str | None, list[str]]:
    norm = query.strip()
    if norm.startswith("L") and norm.endswith(";"):
        norm = norm[1:-1]
    norm = norm.replace(".", "/")
    if inspector.has_class(norm):
        return norm, []
    simple = norm.rsplit("/", 1)[-1].lower()
    exact_simple = sorted(
        c for c in inspector.class_entries
        if not re.search(r"\$\d+$", c)
        and c.rsplit("/", 1)[-1].lower() == simple
    )
    if len(exact_simple) == 1:
        return exact_simple[0], []
    if len(exact_simple) > 1:
        return None, [c.replace("/", ".") for c in exact_simple[:20]]
    return None, [c.replace("/", ".") for c in inspector.suggest_classes(norm, limit=10)]


def filter_javap_output(raw_output: str, member_substr: str | None) -> str:
    if not member_substr:
        return raw_output.strip()
    low = member_substr.lower()
    lines = raw_output.splitlines()
    header: list[str] = []
    blocks: list[list[str]] = []
    cur_block: list[str] | None = None
    in_header = True
    footer = "}"
    member_start_re = re.compile(r"^  \S")
    for line in lines:
        if line == "}":
            if cur_block is not None:
                blocks.append(cur_block)
                cur_block = None
            continue
        if in_header:
            if member_start_re.match(line):
                in_header = False
                cur_block = [line]
            else:
                header.append(line)
            continue
        if member_start_re.match(line):
            if cur_block is not None:
                blocks.append(cur_block)
            cur_block = [line]
        elif cur_block is not None:
            cur_block.append(line)
    if cur_block is not None:
        blocks.append(cur_block)

    matched = []
    for block in blocks:
        sig_line = block[0]
        if low in sig_line.lower():
            matched.append("\n".join(block))
            continue
        if low in ("<init>", "init") and re.search(r"\bdescriptor:\s+\([^)]*\)V\b", "\n".join(block[:4])):
            # Only treat <init> specially when the signature looks like a constructor.
            before_paren = sig_line.split("(", 1)[0].strip().split()
            if before_paren and "." not in before_paren[-1] and len(before_paren) <= 2:
                matched.append("\n".join(block))

    out = list(header)
    if matched:
        out.extend(matched)
    else:
        out.append(f"  // No members matching '{member_substr}'")
    out.append(footer)
    return "\n".join(out)


def classify_surface_path(path: str) -> str:
    p = path.replace("\\", "/")
    for surface, patterns in SURFACE_PATTERNS:
        for pat in patterns:
            if fnmatch.fnmatchcase(p, pat):
                return surface
    return "out-of-surface"


def audit_diff_surface(root: Path, base_ref: str) -> dict[str, list[str]]:
    proc = subprocess.run(
        ["git", "-C", str(root), "diff", "--name-only", f"{base_ref}..HEAD"],
        capture_output=True,
        text=True,
        encoding="utf-8",
    )
    if proc.returncode != 0:
        raise RuntimeError(f"git diff failed against '{base_ref}': {proc.stderr.strip()}")
    grouped: dict[str, list[str]] = {name: [] for name, _ in SURFACE_PATTERNS}
    grouped["out-of-surface"] = []
    for raw in proc.stdout.splitlines():
        path = raw.strip()
        if not path:
            continue
        grouped[classify_surface_path(path)].append(path)
    return grouped


def cmd_status(args) -> int:
    root = args.root.resolve()
    props = verify_port.read_gradle_properties(root)
    mc_ver = props.get("minecraft_version", "")
    profile = DataPackProfile.for_version(mc_ver) if mc_ver else None
    jars = verify_port.find_mc_jars(root, mc_ver) if mc_ver else []
    mod_dirs = verify_port.find_mod_class_dirs(root)
    alignment = verify_port.verify_descriptor_alignment(root)
    payload = {
        "root": str(root),
        "minecraft_version": mc_ver,
        "java_version": props.get("java_version"),
        "expected_java_version": target_java_version(mc_ver) if mc_ver else None,
        "datapack_profile": {
            "normalized": profile.version,
            "before_26_3": profile.before_26_3,
            "before_26_2": profile.before_26_2,
            "is_obfuscated": profile.is_obfuscated,
        } if profile else None,
        "javap": verify_port.find_javap(),
        "mc_jars": [str(p) for p in jars],
        "mod_class_dirs": [str(p) for p in mod_dirs],
        "descriptor_alignment_errors": alignment,
    }
    print(json.dumps(payload, indent=2))
    return 0


def cmd_sync_deps(args) -> int:
    root = args.root.resolve()
    mc_ver = args.mc or read_mc_version(root)
    resolved = resolve_dependencies(mc_ver, explicit_loader=args.loader)
    if args.write:
        updated = apply_dependency_updates(root, resolved)
        resolved["updated_files"] = updated
    print(json.dumps(resolved, indent=2))
    return 0


def cmd_search_class(args) -> int:
    inspector = _load_inspector(args.root.resolve(), args.mc_jars)
    hits = search_classes(inspector, args.query, limit=args.limit)
    if not hits:
        print(f"No classes matching '{args.query}'", file=sys.stderr)
        return 1
    for hit in hits:
        print(hit)
    return 0


def cmd_inspect(args) -> int:
    inspector = _load_inspector(args.root.resolve(), args.mc_jars)
    target, suggestions = resolve_single_class(inspector, args.class_query)
    if target is None:
        if suggestions:
            print(f"Class '{args.class_query}' did not resolve uniquely; candidates:", file=sys.stderr)
            for s in suggestions:
                print(f"  {s}", file=sys.stderr)
            return 2
        print(f"Class not found: '{args.class_query}'", file=sys.stderr)
        return 1
    if args.supertypes:
        print("Supertypes: " + " -> ".join(c.replace("/", ".") for c in inspector.supertypes(target)))
    flags = ("-c", "-p", "-s") if args.code else ("-p", "-s")
    raw = inspector._run_javap([target.replace("/", ".")], extra_flags=flags)
    print(filter_javap_output(raw, args.member))
    return 0


def cmd_diff_surface(args) -> int:
    grouped = audit_diff_surface(args.root.resolve(), args.base)
    for group_name, files in grouped.items():
        if not files:
            continue
        print(f"[{group_name}] ({len(files)})")
        for f in files:
            print(f"  {f}")
    out = grouped["out-of-surface"]
    if out and not args.allow_identifier_migration:
        print(
            f"ERROR: {len(out)} file(s) modified outside the four porting surfaces; route MC API calls through compat/",
            file=sys.stderr,
        )
        return 1
    return 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)

    p_status = sub.add_parser("status", help="Show version profile, descriptor alignment, and discovered JARs")
    p_status.add_argument("--root", type=Path, default=ROOT)

    p_sync = sub.add_parser("sync-deps", help="Resolve Fabric Loader + Modrinth dependency pins")
    p_sync.add_argument("--root", type=Path, default=ROOT)
    p_sync.add_argument("--mc", help="Target Minecraft version (defaults to gradle.properties)")
    p_sync.add_argument("--loader", help="Explicit Fabric Loader version override")
    p_sync.add_argument("--write", action="store_true", help="Write gradle.properties, fabric.mod.json, mixins, and lockfile")

    p_search = sub.add_parser("search-class", help="Search class names inside cached Loom/mod JARs")
    p_search.add_argument("query", help="Case-insensitive class name or substring")
    p_search.add_argument("--limit", type=int, default=30)
    p_search.add_argument("--mc-jar", action="append", dest="mc_jars")
    p_search.add_argument("--root", type=Path, default=ROOT)

    p_inspect = sub.add_parser("inspect", help="Inspect fields, methods, descriptors, and bytecode of a class")
    p_inspect.add_argument("class_query", help="Full or simple class name")
    p_inspect.add_argument("--member", help="Filter members by case-insensitive substring")
    p_inspect.add_argument("--code", action="store_true", help="Include javap bytecode instructions (-c)")
    p_inspect.add_argument("--supertypes", action="store_true", help="Print resolved superclass/interface chain")
    p_inspect.add_argument("--mc-jar", action="append", dest="mc_jars")
    p_inspect.add_argument("--root", type=Path, default=ROOT)

    p_diff = sub.add_parser("diff-surface", help="Audit changed files against the four porting surfaces")
    p_diff.add_argument("--base", default="main", help="Git ref to diff against (default: main)")
    p_diff.add_argument("--allow-identifier-migration", action="store_true",
                        help="Allow cross-codebase renames (e.g. ResourceLocation on 1.21.x)")
    p_diff.add_argument("--root", type=Path, default=ROOT)

    args = parser.parse_args(argv)
    try:
        if args.command == "status":
            return cmd_status(args)
        if args.command == "sync-deps":
            return cmd_sync_deps(args)
        if args.command == "search-class":
            return cmd_search_class(args)
        if args.command == "inspect":
            return cmd_inspect(args)
        if args.command == "diff-surface":
            return cmd_diff_surface(args)
    except (RuntimeError, ValueError, urllib.error.URLError) as err:
        print(f"ERROR: {err}", file=sys.stderr)
        return 1
    return 1


if __name__ == "__main__":
    sys.exit(main())
