"""Static pre-flight verifier for cross-version Minecraft ports (Python stdlib only).

Inspects all Mixins referenced in src/*/resources/*.mixins.json against the active
Minecraft Loom JAR using `javap -c -p -s`, and validates datapack layout/registry
references against DataPackProfile and the Minecraft JAR's registered IDs.
"""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parent))
from datapack_profile import DataPackProfile, read_mc_version

ROOT = Path(__file__).resolve().parent.parent

INJECTOR_NAMES = (
    "Inject",
    "WrapOperation",
    "WrapMethod",
    "ModifyExpressionValue",
    "ModifyReturnValue",
    "Redirect",
    "ModifyArg",
    "ModifyVariable",
)

MC_OWNERS = ("net/minecraft/", "com/mojang/blaze3d/", "com/mojang/math/", "com/mojang/renderpearl/")
EXTERNAL_SKIP_PREFIXES = ("dev/wildercord/", "net/fabricmc/")

PRIMITIVE_DESCS = {
    "boolean": "Z",
    "byte": "B",
    "short": "S",
    "char": "C",
    "int": "I",
    "long": "J",
    "float": "F",
    "double": "D",
    "void": "V",
}

REGISTRY_SOURCES = {
    "placement": (
        "net.minecraft.world.level.levelgen.placement.PlacementModifierType",
        "net.minecraft.world.level.levelgen.placement.PlacementModifier",
    ),
    "block_predicate": (
        "net.minecraft.world.level.levelgen.blockpredicates.BlockPredicateType",
        "net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate",
    ),
    "loot_function": (
        "net.minecraft.world.level.storage.loot.functions.LootItemFunctions",
    ),
    "loot_condition": (
        "net.minecraft.world.level.storage.loot.predicates.LootItemConditions",
        "net.minecraft.world.level.storage.loot.predicates.LootItemCondition",
    ),
    "data_component": (
        "net.minecraft.core.component.DataComponents",
    ),
    "recipe_serializer": (
        "net.minecraft.world.item.crafting.RecipeSerializers",
        "net.minecraft.world.item.crafting.RecipeSerializer",
    ),
}


def find_javap():
    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        for name in ("javap.exe", "javap"):
            cand = Path(java_home) / "bin" / name
            if cand.is_file():
                return str(cand)
    return shutil.which("javap")


def find_mc_jars(root: Path, mc_version: str, explicit_jars=None):
    if explicit_jars:
        return [Path(j) for j in explicit_jars if Path(j).is_file()]
    candidates = []
    gradle_home = Path(os.environ.get("GRADLE_USER_HOME") or (Path.home() / ".gradle"))
    for base in (gradle_home, Path.home() / ".gradle"):
        merged = base / "caches" / "fabric-loom" / mc_version / "minecraft-client.jar"
        if merged.is_file() and merged not in candidates:
            candidates.append(merged)
            return candidates
    loom_maven = root / ".gradle" / "loom-cache" / "minecraftMaven"
    if loom_maven.is_dir():
        for jar in sorted(loom_maven.rglob(f"minecraft-*-{mc_version}.jar")):
            if not jar.name.endswith("-sources.jar"):
                candidates.append(jar)
    return candidates


def strip_comments(source: str) -> str:
    """Replace // and /* ... */ comments with spaces while keeping string/char literals."""
    out = []
    i = 0
    n = len(source)
    while i < n:
        ch = source[i]
        if ch in ('"', "'"):
            quote = ch
            out.append(ch)
            i += 1
            while i < n:
                c = source[i]
                out.append(c)
                if c == "\\" and i + 1 < n:
                    i += 1
                    out.append(source[i])
                elif c == quote:
                    i += 1
                    break
                i += 1
        elif ch == "/" and i + 1 < n and source[i + 1] == "/":
            out.append("  ")
            i += 2
            while i < n and source[i] != "\n":
                out.append(" ")
                i += 1
        elif ch == "/" and i + 1 < n and source[i + 1] == "*":
            out.append("  ")
            i += 2
            while i + 1 < n and not (source[i] == "*" and source[i + 1] == "/"):
                out.append("\n" if source[i] == "\n" else " ")
                i += 1
            if i + 1 < n:
                out.append("  ")
                i += 2
        else:
            out.append(ch)
            i += 1
    return "".join(out)


def _strip_angle_generics(text: str) -> str:
    """Remove balanced <...> type argument clauses from class headers."""
    out = []
    depth = 0
    for ch in text:
        if ch == "<":
            depth += 1
        elif ch == ">":
            if depth > 0:
                depth -= 1
        elif depth == 0:
            out.append(ch)
    return "".join(out)


def extract_balanced(text: str, open_idx: int, open_ch="(", close_ch=")") -> tuple[str, int]:
    depth = 0
    i = open_idx
    n = len(text)
    in_str = None
    while i < n:
        ch = text[i]
        if in_str:
            if ch == "\\" and i + 1 < n:
                i += 2
                continue
            if ch == in_str:
                in_str = None
        else:
            if ch in ('"', "'"):
                in_str = ch
            elif ch == open_ch:
                depth += 1
            elif ch == close_ch:
                depth -= 1
                if depth == 0:
                    return text[open_idx + 1:i], i
        i += 1
    raise ValueError("Unbalanced delimiter in Java source")


def split_top_level(text: str, sep=",") -> list[str]:
    parts = []
    depth_p = depth_b = depth_s = depth_a = 0
    in_str = None
    start = 0
    i = 0
    n = len(text)
    while i < n:
        ch = text[i]
        if in_str:
            if ch == "\\" and i + 1 < n:
                i += 2
                continue
            if ch == in_str:
                in_str = None
        else:
            if ch in ('"', "'"):
                in_str = ch
            elif ch == "(":
                depth_p += 1
            elif ch == ")":
                depth_p -= 1
            elif ch == "{":
                depth_b += 1
            elif ch == "}":
                depth_b -= 1
            elif ch == "[":
                depth_s += 1
            elif ch == "]":
                depth_s -= 1
            elif ch == "<":
                depth_a += 1
            elif ch == ">":
                if depth_a > 0:
                    depth_a -= 1
            elif ch == sep and depth_p == depth_b == depth_s == depth_a == 0:
                piece = text[start:i].strip()
                if piece:
                    parts.append(piece)
                start = i + 1
        i += 1
    tail = text[start:].strip()
    if tail:
        parts.append(tail)
    return parts


def parse_named_args(body: str) -> dict[str, str]:
    args = {}
    for item in split_top_level(body, ","):
        if "=" in item:
            k, v = item.split("=", 1)
            args[k.strip()] = v.strip()
        else:
            args.setdefault("value", item.strip())
    return args


def unquote_strings(expr: str) -> list[str]:
    """Extract string literals from a single string or {...} array expression."""
    out = []
    i = 0
    n = len(expr)
    while i < n:
        if expr[i] == '"':
            j = i + 1
            buf = []
            while j < n:
                if expr[j] == "\\" and j + 1 < n:
                    buf.append(expr[j + 1])
                    j += 2
                elif expr[j] == '"':
                    break
                else:
                    buf.append(expr[j])
                    j += 1
            out.append("".join(buf))
            i = j + 1
        else:
            i += 1
    return out


class JarInspector:
    """Batched `javap -c -p -s` inspector backed by one or more JARs."""

    def __init__(self, jars: list[Path], javap_bin: str):
        self.jars = jars
        self.classpath = os.pathsep.join(str(j) for j in jars)
        self.javap = javap_bin
        self.class_entries: set[str] = set()
        for jar in jars:
            with zipfile.ZipFile(jar) as zf:
                for entry in zf.namelist():
                    if entry.endswith(".class"):
                        self.class_entries.add(entry[:-6])
        self._cache: dict[str, dict] = {}
        self._strings_cache: dict[str, set[str]] = {}

    def has_class(self, binary_name: str) -> bool:
        return binary_name in self.class_entries

    def _run_javap(self, dotted_classes: list[str], extra_flags=("-c", "-p", "-s")) -> str:
        outputs = []
        chunk = []
        chunk_chars = 0
        # Keep each command line well below Windows CreateProcess 32 KiB limit.
        for cls in dotted_classes:
            if chunk and chunk_chars + len(cls) + 1 > 12000:
                proc = subprocess.run(
                    [self.javap, *extra_flags, "-cp", self.classpath, *chunk],
                    capture_output=True,
                    text=True,
                    encoding="utf-8",
                    errors="replace",
                )
                if proc.returncode != 0:
                    raise RuntimeError(f"javap failed: {proc.stderr.strip()}")
                outputs.append(proc.stdout)
                chunk = []
                chunk_chars = 0
            chunk.append(cls)
            chunk_chars += len(cls) + 1
        if chunk:
            proc = subprocess.run(
                [self.javap, *extra_flags, "-cp", self.classpath, *chunk],
                capture_output=True,
                text=True,
                encoding="utf-8",
                errors="replace",
            )
            if proc.returncode != 0:
                raise RuntimeError(f"javap failed: {proc.stderr.strip()}")
            outputs.append(proc.stdout)
        return "\n".join(outputs)

    def ensure_loaded(self, binary_names: list[str], include_interfaces: bool = True):
        pending = []
        seen = set()
        for name in binary_names:
            b = name.replace(".", "/")
            if b not in self._cache and self.has_class(b) and b not in seen:
                seen.add(b)
                pending.append(b)
        while pending:
            batch = pending
            pending = []
            out = self._run_javap([b.replace("/", ".") for b in batch])
            self._parse_javap_output(out)
            # Mark any class whose output was skipped cleanly so we never re-query
            for b in batch:
                meta = self._cache.get(b)
                if not meta:
                    continue
                parents = ([meta["super"]] if meta["super"] else []) + (meta["interfaces"] if include_interfaces else [])
                for sup in parents:
                    if sup not in self._cache and self.has_class(sup) and sup not in seen:
                        seen.add(sup)
                        pending.append(sup)

    def _parse_javap_output(self, output: str):
        cur_binary = None
        cur_dotted = None
        cur_method = None
        pending_member = None
        in_code = False

        header_re = re.compile(
            r"^(?:(?:public|protected|private|abstract|final|static|strictfp|sealed|non-sealed)\s+)*"
            r"(?:class|interface|enum|@interface)\s+(\S+)(?:\s+extends\s+(\S+))?(?:\s+implements\s+([^{]+))?\s*\{"
        )
        # Strict 2-space indent ensures 4-space `    descriptor: Lfoo/Bar;` lines never match member_re!
        member_re = re.compile(r"^  (?=\S)(?!(?:static|synchronized|\}|\{))(.+);\s*$")
        desc_re = re.compile(r"^    descriptor:\s+(\S+)\s*$")
        method_insn_re = re.compile(
            r'//\s+(?:Interface)?Method\s+(?:([A-Za-z0-9_/$]+)\.)?(?:"?(<init>|<clinit>)"?|"([^"]+)"|([A-Za-z0-9_$]+)):(\([^)]*\)\S+)'
        )
        field_insn_re = re.compile(
            r"//\s+Field\s+(?:([A-Za-z0-9_/$]+)\.)?([A-Za-z0-9_$]+):(\S+)"
        )
        new_insn_re = re.compile(r"\bnew\s+#\d+\s+//\s+class\s+([A-Za-z0-9_/$]+)")

        for raw in output.splitlines():
            line = raw.rstrip("\r\n")
            if line.startswith("Compiled from "):
                continue
            if line and not line[0].isspace() and line.endswith("{"):
                clean_header = _strip_angle_generics(line)
                hm = header_re.match(clean_header)
                if hm:
                    cur_dotted = hm.group(1)
                    cur_binary = cur_dotted.replace(".", "/")
                    sup = hm.group(2).replace(".", "/") if hm.group(2) else None
                    ifaces = []
                    if hm.group(3):
                        for part in hm.group(3).split(","):
                            p = part.strip().replace(".", "/")
                            if p:
                                ifaces.append(p)
                    self._cache[cur_binary] = {
                        "super": sup,
                        "interfaces": ifaces,
                        "fields": set(),
                        "methods": {},
                        "strings": set(),
                    }
                    cur_method = None
                    pending_member = None
                    in_code = False
                continue
            if cur_binary is None:
                continue
            if line == "}":
                cur_binary = None
                cur_dotted = None
                cur_method = None
                pending_member = None
                in_code = False
                continue

            if line == "  static {};":
                pending_member = ("method", "<clinit>")
                in_code = False
                continue

            mm = member_re.match(line)
            if mm:
                sig = mm.group(1).strip()
                in_code = False
                cur_method = None
                if "(" in sig:
                    before_paren = _strip_angle_generics(sig.split("(", 1)[0]).strip()
                    tok = before_paren.split()[-1]
                    if tok == cur_dotted or tok.endswith("." + cur_dotted.rsplit(".", 1)[-1]):
                        mname = "<init>"
                    else:
                        mname = tok.rsplit(".", 1)[-1]
                    pending_member = ("method", mname)
                else:
                    before_eq = _strip_angle_generics(sig.split("=", 1)[0]).strip()
                    fname = before_eq.split()[-1]
                    pending_member = ("field", fname)
                continue

            dm = desc_re.match(line)
            if dm and pending_member is not None:
                kind, name = pending_member
                desc = dm.group(1)
                meta = self._cache[cur_binary]
                if kind == "field":
                    meta["fields"].add(name)
                    cur_method = None
                else:
                    key = (name, desc)
                    meta["methods"].setdefault(key, [])
                    cur_method = key
                pending_member = None
                continue

            if line == "    Code:":
                in_code = True
                continue

            if in_code and cur_method is not None:
                if line.startswith("    ") and not line.startswith("      ") and ":" not in line:
                    in_code = False
                    continue
                meta = self._cache[cur_binary]
                if "// String " in line:
                    meta["strings"].add(line.split("// String ", 1)[1].strip())
                insns = meta["methods"][cur_method]
                mi = method_insn_re.search(line)
                if mi:
                    owner = (mi.group(1) or cur_binary).replace(".", "/")
                    name = mi.group(2) or mi.group(3) or mi.group(4)
                    desc = mi.group(5)
                    insns.append(("INVOKE", owner, name, desc))
                    continue
                fi = field_insn_re.search(line)
                if fi:
                    owner = (fi.group(1) or cur_binary).replace(".", "/")
                    name = fi.group(2)
                    desc = fi.group(3)
                    insns.append(("FIELD", owner, name, desc))
                    continue
                ni = new_insn_re.search(line)
                if ni:
                    owner = ni.group(1).replace(".", "/")
                    insns.append(("NEW", owner, "", ""))

    def supertypes(self, binary_name: str) -> list[str]:
        """BFS over class and its superclasses/interfaces present in the JAR."""
        if binary_name not in self._cache and self.has_class(binary_name):
            self.ensure_loaded([binary_name])
        order = []
        seen = set()
        queue = [binary_name]
        while queue:
            cur = queue.pop(0)
            if cur in seen or not self.has_class(cur):
                continue
            seen.add(cur)
            order.append(cur)
            if cur not in self._cache:
                self.ensure_loaded([cur])
            meta = self._cache.get(cur)
            if meta:
                if meta["super"]:
                    queue.append(meta["super"])
                queue.extend(meta["interfaces"])
        return order

    def is_subtype(self, child: str, parent: str) -> bool:
        if child == parent:
            return True
        return parent in self.supertypes(child)

    def has_field(self, binary_name: str, field_name: str) -> bool:
        for sup in self.supertypes(binary_name):
            meta = self._cache.get(sup)
            if meta and field_name in meta["fields"]:
                return True
        return False

    def find_methods(self, binary_name: str, method_name: str, descriptor: str | None = None):
        matches = []
        seen_sigs = set()
        for sup in self.supertypes(binary_name):
            meta = self._cache.get(sup)
            if not meta:
                continue
            for (name, desc), insns in meta["methods"].items():
                if name == method_name and (descriptor is None or desc == descriptor):
                    sig = (sup, name, desc)
                    if sig not in seen_sigs:
                        seen_sigs.add(sig)
                        matches.append((sup, name, desc, insns))
        return matches

    def preload_string_constants(self, dotted_classes: list[str]):
        missing = [c for c in dotted_classes if c.replace(".", "/") not in self._cache and self.has_class(c.replace(".", "/"))]
        if missing:
            self.ensure_loaded([c.replace(".", "/") for c in missing], include_interfaces=False)

    def string_constants(self, dotted_class: str) -> set[str]:
        binary = dotted_class.replace(".", "/")
        meta = self._cache.get(binary)
        return set(meta["strings"]) if meta else set()


def parse_imports(source: str):
    pkg = ""
    pm = re.search(r"\bpackage\s+([A-Za-z0-9_.]+)\s*;", source)
    if pm:
        pkg = pm.group(1)
    exact = {}
    wildcards = []
    for im in re.finditer(r"\bimport\s+(?:static\s+)?([A-Za-z0-9_.*]+)\s*;", source):
        path = im.group(1)
        if path.endswith(".*"):
            wildcards.append(path[:-2])
        else:
            exact[path.rsplit(".", 1)[-1]] = path
    return pkg, exact, wildcards


def resolve_java_type_binary(type_expr: str, pkg: str, exact: dict[str, str], wildcards: list[str], inspector: JarInspector | None) -> str:
    clean = type_expr.replace(" ", "").rstrip(".")
    parts = clean.split(".")
    first_upper = next((idx for idx, p in enumerate(parts) if p and p[0].isupper()), None)
    if first_upper is not None and first_upper > 0:
        pkg_part = "/".join(parts[:first_upper])
        cls_part = "$".join(parts[first_upper:])
        return f"{pkg_part}/{cls_part}"
    head = parts[0]
    rest = parts[1:]
    if head in exact:
        base = exact[head].replace(".", "/")
        return base + ("$" + "$".join(rest) if rest else "")
    for cand_pkg in ([pkg] if pkg else []) + wildcards:
        cand = f"{cand_pkg.replace('.', '/')}/{head}" + ("$" + "$".join(rest) if rest else "")
        if inspector is None or inspector.has_class(cand):
            return cand
    if pkg:
        return f"{pkg.replace('.', '/')}/{head}" + ("$" + "$".join(rest) if rest else "")
    return head + ("$" + "$".join(rest) if rest else "")


def resolve_mixin_targets(source: str, pkg: str, exact: dict[str, str], wildcards: list[str], inspector: JarInspector | None) -> list[str]:
    m = re.search(r"@Mixin\s*\(", source)
    if not m:
        return []
    body, _ = extract_balanced(source, m.end() - 1, "(", ")")
    args = parse_named_args(body)
    targets = []
    if "targets" in args:
        for s in unquote_strings(args["targets"]):
            targets.append(s.replace(".", "/"))
    val = args.get("value", "")
    if val:
        inner = val.strip()
        if inner.startswith("{") and inner.endswith("}"):
            inner = inner[1:-1]
        for piece in split_top_level(inner, ","):
            piece = piece.strip()
            if piece.endswith(".class"):
                cls_expr = piece[:-6].strip()
                targets.append(resolve_java_type_binary(cls_expr, pkg, exact, wildcards, inspector))
    return targets


def parse_method_spec(spec: str) -> tuple[str, str | None]:
    s = spec.strip()
    if s.startswith("L") and ";" in s and ("(" not in s or s.index(";") < s.index("(")):
        s = s.split(";", 1)[1]
    if "(" in s:
        idx = s.index("(")
        return s[:idx], s[idx:]
    return s, None


def parse_jvm_target(target: str):
    """Parse Lowner;name(desc)ret or Lowner;name:desc into (kind, owner, name, desc)."""
    if ";" not in target:
        return None
    owner_part, rest = target.split(";", 1)
    owner = owner_part[1:] if owner_part.startswith("L") else owner_part
    if "(" in rest:
        idx = rest.index("(")
        return ("INVOKE", owner, rest[:idx], rest[idx:])
    if ":" in rest:
        name, desc = rest.split(":", 1)
        return ("FIELD", owner, name, desc)
    return ("NEW", owner, rest, None)


def parse_param_descriptors(method_desc: str) -> list[str]:
    assert method_desc.startswith("(")
    params = []
    i = 1
    while i < len(method_desc) and method_desc[i] != ")":
        start = i
        while method_desc[i] == "[":
            i += 1
        if method_desc[i] == "L":
            end = method_desc.index(";", i)
            i = end + 1
        else:
            i += 1
        params.append(method_desc[start:i])
    return params


def java_param_matches_jvm_desc(java_type: str, jvm_desc: str, pkg: str, exact: dict[str, str], wildcards: list[str], inspector: JarInspector | None) -> bool:
    clean = _strip_angle_generics(java_type).strip()
    arrays = 0
    while clean.endswith("[]"):
        arrays += 1
        clean = clean[:-2].strip()
    prefix = "[" * arrays
    if clean in PRIMITIVE_DESCS:
        return jvm_desc == prefix + PRIMITIVE_DESCS[clean]
    if not jvm_desc.startswith(prefix + "L") or not jvm_desc.endswith(";"):
        return False
    target_bin = jvm_desc[len(prefix) + 1:-1]
    if "." in clean:
        resolved = resolve_java_type_binary(clean, pkg, exact, wildcards, inspector)
        if resolved == target_bin:
            return True
    if clean in exact and exact[clean].replace(".", "/") == target_bin:
        return True
    target_simple = target_bin.rsplit("/", 1)[-1].rsplit("$", 1)[-1]
    clean_simple = clean.rsplit(".", 1)[-1].rsplit("$", 1)[-1]
    return target_simple == clean_simple


def is_mc_class(binary_name: str) -> bool:
    if binary_name.startswith(EXTERNAL_SKIP_PREFIXES):
        return False
    return any(binary_name.startswith(p) for p in MC_OWNERS)


def verify_mixins(root: Path, inspector: JarInspector | None) -> list[str]:
    errors = []
    mixin_cfgs = sorted((root / "src").rglob("*.mixins.json"))
    if not mixin_cfgs:
        errors.append("No *.mixins.json files found under src/")
        return errors

    mixin_records = []
    needed_classes = []

    for mf in mixin_cfgs:
        cfg = json.loads(mf.read_text(encoding="utf-8"))
        pkg = cfg["package"]
        pkg_path = pkg.replace(".", "/")
        java_root = mf.parents[1] / "java" / pkg_path
        default_req = int(cfg.get("injectors", {}).get("defaultRequire", 0))
        for section in ("mixins", "client", "server"):
            for entry in cfg.get(section, []):
                src_path = java_root / (entry.replace(".", "/") + ".java")
                if not src_path.is_file():
                    errors.append(f"{mf.relative_to(root)}: missing mixin source {src_path.relative_to(root)}")
                    continue
                if inspector is None:
                    continue
                raw = src_path.read_text(encoding="utf-8")
                src = strip_comments(raw)
                file_pkg, exact, wildcards = parse_imports(src)
                targets = resolve_mixin_targets(src, file_pkg, exact, wildcards, inspector)
                if not targets:
                    errors.append(f"{src_path.relative_to(root)}: could not resolve @Mixin target")
                    continue
                mc_targets = []
                for t in targets:
                    if t.startswith(EXTERNAL_SKIP_PREFIXES):
                        continue
                    if not inspector.has_class(t):
                        errors.append(f"{src_path.relative_to(root)}: @Mixin target class missing from MC JAR: {t}")
                    else:
                        mc_targets.append(t)
                        needed_classes.append(t)
                if mc_targets:
                    mixin_records.append((src_path, src, file_pkg, exact, wildcards, mc_targets, default_req))

    if inspector is None:
        return errors

    for src_path, src, *_ in mixin_records:
        for atm in re.finditer(r"@At\s*\(", src):
            try:
                at_body, _ = extract_balanced(src, atm.end() - 1, "(", ")")
            except ValueError:
                continue
            at_args = parse_named_args(at_body)
            if "target" in at_args:
                for ts in unquote_strings(at_args["target"]):
                    parsed = parse_jvm_target(ts)
                    if parsed and is_mc_class(parsed[1]):
                        needed_classes.append(parsed[1])
                    elif ts.startswith("(") and ")" in ts:
                        ret_part = ts.split(")", 1)[1].strip()
                        if ret_part.startswith("L") and ";" in ret_part:
                            owner = ret_part[1:ret_part.index(";")]
                            if is_mc_class(owner):
                                needed_classes.append(owner)
                    elif ts.startswith("L") and ";" in ts:
                        owner = ts[1:ts.index(";")]
                        if is_mc_class(owner):
                            needed_classes.append(owner)
                    elif "/" in ts and ts.startswith(MC_OWNERS):
                        needed_classes.append(ts.replace(".", "/"))

    inspector.ensure_loaded(needed_classes)

    inj_pattern = re.compile(r"@(" + "|".join(INJECTOR_NAMES) + r")\s*\(")
    shadow_pattern = re.compile(r"@(Shadow|Accessor|Invoker)\b")
    local_ann_re = re.compile(r"@(?:[A-Za-z0-9_.]+\.)?Local\s*\(")
    strip_ann_re = re.compile(r"@[A-Za-z0-9_.]+\s*(?:\([^)]*\))?")

    for src_path, src, file_pkg, exact, wildcards, mc_targets, default_req in mixin_records:
        rel = str(src_path.relative_to(root))

        for sm in shadow_pattern.finditer(src):
            ann_kind = sm.group(1)
            pos = sm.end()
            explicit = None
            ann_args = {}
            while pos < len(src) and src[pos].isspace():
                pos += 1
            if pos < len(src) and src[pos] == "(":
                ann_body, end_p = extract_balanced(src, pos, "(", ")")
                ann_args = parse_named_args(ann_body)
                if "value" in ann_args:
                    explicit_strs = unquote_strings(ann_args["value"])
                    if explicit_strs:
                        explicit = explicit_strs[0]
                elif not ann_args:
                    explicit_strs = unquote_strings(ann_body)
                    if explicit_strs:
                        explicit = explicit_strs[0]
                pos = end_p + 1
            rest = src[pos:pos + 400]
            decl_chunk = re.split(r"[;{]", rest, maxsplit=1)[0]
            cleaned = strip_ann_re.sub(" ", decl_chunk)
            cleaned = _strip_angle_generics(cleaned)
            if ann_kind == "Shadow":
                if "(" in cleaned:
                    before_p = cleaned.split("(", 1)[0].strip()
                    member_name = before_p.split()[-1]
                    if not any(inspector.find_methods(t, member_name) for t in mc_targets):
                        errors.append(f"{rel}: @Shadow method {member_name}() not found on {mc_targets}")
                else:
                    before_eq = cleaned.split("=", 1)[0].strip()
                    if before_eq:
                        member_name = before_eq.split()[-1]
                        if not any(inspector.has_field(t, member_name) for t in mc_targets):
                            errors.append(f"{rel}: @Shadow field {member_name} not found on {mc_targets}")
            elif ann_args.get("remap") == "false":
                continue
            elif ann_kind == "Accessor":
                if "(" in cleaned:
                    mname = cleaned.split("(", 1)[0].strip().split()[-1]
                    if explicit:
                        cands = [explicit]
                    else:
                        if mname.startswith(("get", "set")) and len(mname) > 3:
                            stem = mname[3:]
                        elif mname.startswith("is") and len(mname) > 2:
                            stem = mname[2:]
                        else:
                            stem = mname
                        cands = [stem[0].lower() + stem[1:], stem] if stem else [mname]
                    if not any(any(inspector.has_field(t, c) for c in cands) for t in mc_targets):
                        errors.append(f"{rel}: @Accessor field {cands[0]} not found on {mc_targets}")
            elif ann_kind == "Invoker":
                if "(" in cleaned:
                    mname = cleaned.split("(", 1)[0].strip().split()[-1]
                    if explicit:
                        cands = [explicit]
                    else:
                        if mname.startswith("call") and len(mname) > 4:
                            stem = mname[4:]
                            cands = [stem[0].lower() + stem[1:], stem]
                        elif mname.startswith("invoke") and len(mname) > 6:
                            stem = mname[6:]
                            cands = [stem[0].lower() + stem[1:], stem]
                        else:
                            cands = [mname]
                    if not any(any(inspector.find_methods(t, c) for c in cands) for t in mc_targets):
                        errors.append(f"{rel}: @Invoker method {cands[0]}() not found on {mc_targets}")

        for im in inj_pattern.finditer(src):
            body, close_idx = extract_balanced(src, im.end() - 1, "(", ")")
            args = parse_named_args(body)
            require_val = int(args["require"]) if "require" in args and args["require"].lstrip("-").isdigit() else default_req
            expect_val = int(args["expect"]) if "expect" in args and args["expect"].isdigit() else None
            allow_val = int(args["allow"]) if "allow" in args and args["allow"].isdigit() else None
            remap_false = args.get("remap") == "false"

            method_specs = unquote_strings(args.get("method", ""))
            if not method_specs:
                continue

            matched_methods = []
            for spec in method_specs:
                m_name, m_desc = parse_method_spec(spec)
                found_for_spec = []
                for t in mc_targets:
                    found_for_spec.extend(inspector.find_methods(t, m_name, m_desc))
                if not found_for_spec:
                    if require_val != 0 and not remap_false:
                        errors.append(f"{rel}: injector target method '{spec}' not found on {mc_targets}")
                else:
                    matched_methods.extend(found_for_spec)

            primary_ats = []
            if "at" in args:
                at_expr = args["at"]
                for atm in re.finditer(r"@At\s*\(", at_expr):
                    at_body, _ = extract_balanced(at_expr, atm.end() - 1, "(", ")")
                    primary_ats.append(parse_named_args(at_body))

            for at_args in primary_ats:
                at_vals = unquote_strings(at_args.get("value", ""))
                at_val = at_vals[0] if at_vals else ""
                if at_val not in ("INVOKE", "FIELD", "NEW") or "target" not in at_args:
                    continue
                target_strs = unquote_strings(at_args["target"])
                if not target_strs:
                    continue
                t_raw = target_strs[0]
                if at_val == "NEW":
                    if t_raw.startswith("(") and ")" in t_raw:
                        args_part, ret_part = t_raw.split(")", 1)
                        new_owner = ret_part.strip()
                        if new_owner.startswith("L"):
                            new_owner = new_owner[1:]
                        new_owner = new_owner.rstrip(";").replace(".", "/")
                        ctor_desc = args_part + ")V"
                        if is_mc_class(new_owner):
                            if not inspector.has_class(new_owner):
                                errors.append(f"{rel}: @At(\"NEW\") target class missing from MC JAR: {new_owner}")
                                continue
                            inspector.ensure_loaded([new_owner])
                            if not inspector.find_methods(new_owner, "<init>", ctor_desc):
                                errors.append(f"{rel}: @At(\"NEW\") constructor missing in JAR: {new_owner}.<init>{ctor_desc}")
                                continue
                        if matched_methods:
                            cnt = sum(
                                1 for _, _, _, insns in matched_methods
                                for ik, iowner, iname, idesc in insns
                                if ik == "INVOKE" and iowner == new_owner and iname == "<init>" and idesc == ctor_desc
                            )
                            if cnt == 0 and require_val != 0:
                                errors.append(f"{rel}: @At(\"NEW\", target=\"{t_raw}\") matched 0 instructions in {method_specs}")
                            if expect_val is not None and cnt < expect_val:
                                errors.append(f"{rel}: @At(\"NEW\") count {cnt} < expect {expect_val} in {method_specs}")
                            if allow_val is not None and cnt > allow_val:
                                errors.append(f"{rel}: @At(\"NEW\") count {cnt} > allow {allow_val} in {method_specs}")
                        continue
                    new_owner = t_raw.split(";", 1)[0]
                    if new_owner.startswith("L"):
                        new_owner = new_owner[1:]
                    new_owner = new_owner.replace(".", "/")
                    if is_mc_class(new_owner) and not inspector.has_class(new_owner):
                        errors.append(f"{rel}: @At(\"NEW\") target class missing from MC JAR: {new_owner}")
                    if matched_methods:
                        cnt = sum(
                            1 for _, _, _, insns in matched_methods
                            for ik, iowner, _, _ in insns
                            if ik == "NEW" and iowner == new_owner
                        )
                        if cnt == 0 and require_val != 0:
                            errors.append(f"{rel}: @At(\"NEW\", target=\"{t_raw}\") matched 0 instructions in {method_specs}")
                        if expect_val is not None and cnt < expect_val:
                            errors.append(f"{rel}: @At(\"NEW\") count {cnt} < expect {expect_val} in {method_specs}")
                        if allow_val is not None and cnt > allow_val:
                            errors.append(f"{rel}: @At(\"NEW\") count {cnt} > allow {allow_val} in {method_specs}")
                    continue

                parsed = parse_jvm_target(t_raw)
                if not parsed:
                    continue
                _, t_owner, t_name, t_desc = parsed
                if is_mc_class(t_owner):
                    if not inspector.has_class(t_owner):
                        errors.append(f"{rel}: @At target owner missing from MC JAR: {t_owner}")
                        continue
                    inspector.ensure_loaded([t_owner])
                    if at_val == "INVOKE":
                        if not inspector.find_methods(t_owner, t_name, t_desc):
                            errors.append(f"{rel}: @At(\"INVOKE\") target missing in JAR: {t_raw}")
                            continue
                    elif at_val == "FIELD":
                        if not inspector.has_field(t_owner, t_name):
                            errors.append(f"{rel}: @At(\"FIELD\") target missing in JAR: {t_raw}")
                            continue

                if matched_methods:
                    cnt = 0
                    for _, _, _, insns in matched_methods:
                        for ik, iowner, iname, idesc in insns:
                            if ik != at_val or iname != t_name:
                                continue
                            if t_desc is not None and idesc != t_desc:
                                continue
                            if (iowner == t_owner
                                    or (inspector.has_class(iowner) and inspector.has_class(t_owner)
                                        and (inspector.is_subtype(iowner, t_owner) or inspector.is_subtype(t_owner, iowner)))):
                                cnt += 1
                    if cnt == 0 and require_val != 0:
                        errors.append(f"{rel}: @At(\"{at_val}\", target=\"{t_raw}\") matched 0 instructions in {method_specs}")
                    if expect_val is not None and cnt < expect_val:
                        errors.append(f"{rel}: @At(\"{at_val}\", target=\"{t_raw}\") count {cnt} < expect {expect_val} in {method_specs}")
                    if allow_val is not None and cnt > allow_val:
                        errors.append(f"{rel}: @At(\"{at_val}\", target=\"{t_raw}\") count {cnt} > allow {allow_val} in {method_specs}")

            after_ann = src[close_idx + 1:close_idx + 1200]
            paren_m = re.search(r"\(", after_ann)
            if paren_m and matched_methods:
                try:
                    param_text, _ = extract_balanced(after_ann, paren_m.start(), "(", ")")
                except ValueError:
                    param_text = ""
                for param in split_top_level(param_text, ","):
                    lm = local_ann_re.search(param)
                    if not lm:
                        continue
                    lbody, _ = extract_balanced(param, lm.end() - 1, "(", ")")
                    largs = parse_named_args(lbody)
                    if largs.get("argsOnly") != "true":
                        continue
                    if "ordinal" in largs or "index" in largs:
                        continue
                    p_rem = strip_ann_re.sub(" ", param)
                    p_rem = re.sub(r"\bfinal\b", " ", p_rem).strip()
                    tokens = p_rem.rsplit(None, 1)
                    if len(tokens) < 2:
                        continue
                    java_type = tokens[0]
                    for _, mname, mdesc, _ in matched_methods:
                        pdescs = parse_param_descriptors(mdesc)
                        matching = sum(
                            1 for pd in pdescs
                            if java_param_matches_jvm_desc(java_type, pd, file_pkg, exact, wildcards, inspector)
                        )
                        if matching > 1:
                            errors.append(
                                f"{rel}: @Local(argsOnly = true) {java_type} is ambiguous "
                                f"({matching} matching params) on {mname}{mdesc}; specify ordinal or index"
                            )

    return errors


def _walk_loot_functions(node, errors, rel):
    if isinstance(node, list):
        for item in node:
            yield from _walk_loot_functions(item, errors, rel)
        return
    if not isinstance(node, dict):
        return
    for key in ("functions", "given_item_modifiers"):
        val = node.get(key)
        if isinstance(val, list):
            for entry in val:
                if isinstance(entry, dict):
                    fn = entry.get("function") or entry.get("type")
                    if isinstance(fn, str) and fn.startswith("minecraft:"):
                        yield ("function", fn.removeprefix("minecraft:"), rel)
    for key in ("modifier", "given_item_modifier"):
        val = node.get(key)
        items = val if isinstance(val, list) else ([val] if isinstance(val, dict) else [])
        for entry in items:
            if isinstance(entry, dict):
                fn = entry.get("function") or entry.get("type")
                if isinstance(fn, str) and fn.startswith("minecraft:"):
                    yield ("function", fn.removeprefix("minecraft:"), rel)
    conds = []
    if isinstance(node.get("conditions"), list):
        conds.extend(node["conditions"])
    if isinstance(node.get("condition"), dict):
        conds.append(node["condition"])
    elif isinstance(node.get("condition"), list):
        conds.extend(node["condition"])
    if isinstance(node.get("terms"), list) and ("type" in node or "condition" in node):
        conds.extend(node.get("terms", []))
    if isinstance(node.get("term"), dict):
        conds.append(node["term"])
    for c in conds:
        if isinstance(c, dict):
            cname = c.get("condition") or c.get("type")
            if isinstance(cname, str) and cname.startswith("minecraft:"):
                yield ("condition", cname.removeprefix("minecraft:"), rel)
            yield from _walk_loot_functions(c, errors, rel)
    if "components" in node and isinstance(node["components"], dict):
        for comp in node["components"]:
            clean = comp.removeprefix("!")
            if clean.startswith("minecraft:"):
                yield ("component", clean.removeprefix("minecraft:"), rel)
    for k, v in node.items():
        if k not in ("conditions", "condition", "components"):
            yield from _walk_loot_functions(v, errors, rel)


def _walk_block_predicates(node, rel):
    if isinstance(node, list):
        for item in node:
            yield from _walk_block_predicates(item, rel)
        return
    if not isinstance(node, dict):
        return
    ptype = node.get("type")
    if isinstance(ptype, str) and ptype.startswith("minecraft:"):
        yield (ptype.removeprefix("minecraft:"), rel)
    for key in ("predicate", "predicates"):
        if key in node:
            yield from _walk_block_predicates(node[key], rel)


def verify_datapacks(root: Path, profile: DataPackProfile, inspector: JarInspector | None) -> list[str]:
    errors = []
    data_dir = root / "src" / "main" / "resources" / "data" / "wildercord"
    if not data_dir.is_dir():
        return errors

    expected_feat = data_dir / profile.feature_subdir
    stale_feat = data_dir / profile.stale_feature_subdir
    if stale_feat.exists():
        errors.append(f"Stale feature directory present for {profile.version}: {stale_feat.relative_to(root)}")
    if not expected_feat.is_dir():
        errors.append(f"Expected feature directory missing for {profile.version}: {expected_feat.relative_to(root)}")

    stale_brew = data_dir / profile.stale_brewing_subdir
    if stale_brew.exists():
        errors.append(f"Stale brewing directory present for {profile.version}: {stale_brew.relative_to(root)}")

    loot_ids = []
    loot_dirs = [data_dir / "loot_table", data_dir / "villager_trade"]
    for ldir in loot_dirs:
        if not ldir.is_dir():
            continue
        for p in sorted(ldir.rglob("*.json")):
            rel = str(p.relative_to(root))
            data = json.loads(p.read_text(encoding="utf-8"))
            stack = [data]
            while stack:
                cur = stack.pop()
                if isinstance(cur, list):
                    stack.extend(cur)
                elif isinstance(cur, dict):
                    fn = cur.get("function") or cur.get("type")
                    if fn == "minecraft:exploration_map" and isinstance(cur.get("destination"), str):
                        dest = cur["destination"]
                        if profile.is_26_2 and dest.startswith("#"):
                            errors.append(f"{rel}: 26.2 exploration_map destination must not start with '#': {dest}")
                        elif not profile.is_26_2 and not dest.startswith("#"):
                            errors.append(f"{rel}: {profile.version} exploration_map destination must start with '#': {dest}")
                    stack.extend(cur.values())
            loot_ids.extend(_walk_loot_functions(data, errors, rel))

    if inspector is None:
        return errors

    all_reg_classes = [cls for cls_list in REGISTRY_SOURCES.values() for cls in cls_list]
    inspector.preload_string_constants(all_reg_classes)
    reg_sets = {}
    for key, cls_list in REGISTRY_SOURCES.items():
        s = set()
        for cls in cls_list:
            s |= inspector.string_constants(cls)
        reg_sets[key] = s

    for kind, ident, rel in loot_ids:
        if kind == "function" and reg_sets["loot_function"] and ident not in reg_sets["loot_function"]:
            errors.append(f"{rel}: unknown minecraft:{ident} loot function for {profile.version}")
        elif kind == "condition" and reg_sets["loot_condition"] and ident not in reg_sets["loot_condition"]:
            errors.append(f"{rel}: unknown minecraft:{ident} loot condition for {profile.version}")
        elif kind == "component" and reg_sets["data_component"] and ident not in reg_sets["data_component"]:
            errors.append(f"{rel}: unknown minecraft:{ident} data component for {profile.version}")

    placed_dir = data_dir / "worldgen" / "placed_feature"
    if placed_dir.is_dir():
        for p in sorted(placed_dir.rglob("*.json")):
            rel = str(p.relative_to(root))
            data = json.loads(p.read_text(encoding="utf-8"))
            for mod in data.get("placement", []):
                if not isinstance(mod, dict):
                    continue
                mtype = mod.get("type")
                if isinstance(mtype, str) and mtype.startswith("minecraft:"):
                    mid = mtype.removeprefix("minecraft:")
                    if reg_sets["placement"] and mid not in reg_sets["placement"]:
                        errors.append(f"{rel}: unknown placement modifier minecraft:{mid} for {profile.version}")
                if "predicate" in mod:
                    for bid, brel in _walk_block_predicates(mod["predicate"], rel):
                        if reg_sets["block_predicate"] and bid not in reg_sets["block_predicate"]:
                            errors.append(f"{brel}: unknown block predicate minecraft:{bid} for {profile.version}")

    recipe_dir = data_dir / "recipe"
    if recipe_dir.is_dir():
        for p in sorted(recipe_dir.rglob("*.json")):
            if "brewing" in p.relative_to(recipe_dir).parts:
                continue
            rel = str(p.relative_to(root))
            data = json.loads(p.read_text(encoding="utf-8"))
            rtype = data.get("type")
            if isinstance(rtype, str) and rtype.startswith("minecraft:"):
                rid = rtype.removeprefix("minecraft:")
                if reg_sets["recipe_serializer"] and rid not in reg_sets["recipe_serializer"]:
                    errors.append(f"{rel}: unknown recipe serializer minecraft:{rid} for {profile.version}")
            stack = [data]
            while stack:
                cur = stack.pop()
                if isinstance(cur, list):
                    stack.extend(cur)
                elif isinstance(cur, dict):
                    if "components" in cur and isinstance(cur["components"], dict):
                        for comp in cur["components"]:
                            clean = comp.removeprefix("!")
                            if clean.startswith("minecraft:"):
                                cid = clean.removeprefix("minecraft:")
                                if reg_sets["data_component"] and cid not in reg_sets["data_component"]:
                                    errors.append(f"{rel}: unknown item component minecraft:{cid} for {profile.version}")
                    stack.extend(cur.values())

    return errors


def verify(root: Path | None = None, mc_jars=None, allow_missing_jar: bool = False) -> list[str]:
    base = ROOT if root is None else Path(root)
    mc_version = read_mc_version(base)
    profile = DataPackProfile.for_version(mc_version)

    jars = find_mc_jars(base, mc_version, mc_jars)
    javap = find_javap()
    inspector = None

    if jars and javap:
        inspector = JarInspector(jars, javap)
    elif not allow_missing_jar:
        missing_what = "Minecraft Loom JAR" if not jars else "javap executable"
        return [f"Missing {missing_what} for {mc_version}; pass --mc-jar or run with Loom cache populated"]

    errors = []
    errors.extend(verify_mixins(base, inspector))
    errors.extend(verify_datapacks(base, profile, inspector))
    return errors


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--mc-jar", action="append", dest="mc_jars", help="Path to merged MC client JAR (repeatable)")
    parser.add_argument("--allow-missing-jar", action="store_true",
                        help="Skip bytecode checks when no local Loom JAR or javap is present")
    args = parser.parse_args(argv)
    errors = verify(args.root, mc_jars=args.mc_jars, allow_missing_jar=args.allow_missing_jar)
    if errors:
        for err in errors:
            print("ERROR:", err, file=sys.stderr)
        return 1
    mode = "bytecode + datapack" if find_mc_jars(args.root, read_mc_version(args.root), args.mc_jars) and find_javap() else "structural datapack"
    print(f"Port verification passed ({read_mc_version(args.root)}, {mode}).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
