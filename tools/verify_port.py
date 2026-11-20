"""Static pre-flight verifier for cross-version Minecraft ports (Python stdlib only).

Inspects all Mixins referenced in src/*/resources/*.mixins.json against the active
Minecraft Loom JAR using `javap -c -p -s`, and validates datapack layout/registry
references against DataPackProfile and the Minecraft JAR's registered IDs.
"""
from __future__ import annotations

import argparse
import difflib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parent))
from datapack_profile import DataPackProfile, parse_mc_tuple, read_mc_version

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
EXTERNAL_SKIP_PREFIXES = ("net/fabricmc/",)
MOD_PREFIX = "dev/wildercord/"

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

COMPAT_CONTRACT: dict[str, tuple[tuple[str, int], ...]] = {
    "src/main/java/dev/wildercord/compat/McEntities.java": (
        ("drop", 3), ("giveOrDrop", 2), ("dropSilently", 2),
        ("placeItemBackInInventory", 2), ("swing", 3), ("isSwinging", 1),
        ("swingProgress", 2), ("syncVelocity", 1), ("setPermanentlyInvulnerable", 2),
        ("isPermanentlyInvulnerable", 1), ("saveHurtCooldown", 1),
        ("restoreHurtCooldown", 2), ("clearHurtCooldown", 1),
        ("damageCooldownTicks", 1), ("isEnderman", 1), ("isEndermanAngryAt", 3),
        ("pacifyEnderman", 1), ("projectileDeflection", 1),
    ),
    "src/main/java/dev/wildercord/compat/CompatCubeMob.java": (
        ("applyCubeMobHealth", 1), ("cubeAttackGoal", 0),
    ),
    "src/main/java/dev/wildercord/compat/McBlocksAndItems.java": (
        ("immovablePushReaction", 0), ("poppedPushReaction", 0), ("isOre", 1),
        ("isSapling", 1), ("isValidBonemealTarget", 4), ("performBonemeal", 5),
        ("douseCampfire", 4), ("primeTnt", 3), ("destroyBlockLevelEvent", 0),
        ("sound", 1), ("cookingFuelProperties", 3), ("registerFuel", 2),
        ("compostableMediumProperties", 1), ("registerCompostable", 2),
        ("containerItems", 1), ("containerCount", 1), ("bundleItems", 1),
        ("bundleWith", 2), ("countMatchingItems", 3), ("removeMatchingItems", 4),
        ("writeWaxedSign", 2), ("appendFacingSignLineAndGlow", 3),
        ("setBothSidesGlowing", 1), ("dyedBlockFamilies", 0),
        ("brownWoolStairs", 0), ("setGlaiveSwingAnimation", 2),
    ),
    "src/main/java/dev/wildercord/compat/SimpleWorldFeature.java": (
        ("place", 4),
    ),
    "src/main/java/dev/wildercord/compat/McWorldgen.java": (
        ("registerFeature", 2), ("addSpawn", 6), ("biomeSpawnTypes", 2),
        ("dappledForestBiome", 0), ("biomeSetWithOptional", 2),
        ("canValidBiomeExistAtChunkCenter", 1), ("getNoiseBiome", 4),
        ("startsForStructure", 4), ("startsForStructure", 3),
        ("findNearestStructure", 5),
    ),
    "src/main/java/dev/wildercord/compat/McLootAndTriggers.java": (
        ("exactPoolRolls", 1), ("getOptionalParam", 2), ("simpleTriggerCodec", 1),
    ),
    "src/main/java/dev/wildercord/compat/WildercordBrewing.java": (),
    "src/client/java/dev/wildercord/client/compat/RenderCompat.java": (
        ("submitModel", 8), ("submitModel", 9), ("rotate", 2), ("rotateDegrees", 3),
        ("startTextInput", 2), ("stopTextInput", 2), ("swingProgress", 1),
        ("setSwingProgress", 2), ("overrideStabWithWhackGlaive", 1),
        ("clearSwingForCustomRig", 1), ("swingObservation", 1),
        ("isStabSwingType", 1), ("hasActiveStabSwing", 1),
        ("activeMainHandSwingType", 1), ("livingModel", 1),
        ("wispGlowPipeline", 0), ("auraBodyGlowPipeline", 0),
        ("glowParticlePipeline", 0), ("darkParticlePipeline", 0),
        ("wispGlowRenderType", 1), ("auraBodyGlowRenderType", 1),
        ("glowParticleLayer", 0), ("darkParticleLayer", 0),
    ),
    "src/client/java/dev/wildercord/client/compat/CullableMobRenderer.java": (
        ("shouldRenderEntity", 6), ("superShouldRenderEntity", 6),
        ("cullingBounds", 2), ("superCullingBounds", 2),
    ),
    "src/client/java/dev/wildercord/client/compat/CullableEntityRenderer.java": (
        ("shouldRenderEntity", 6), ("superShouldRenderEntity", 6),
        ("cullingBounds", 2), ("superCullingBounds", 2),
    ),
    "src/client/java/dev/wildercord/client/compat/FirstPersonHandView.java": (
        ("of", 2), ("of", 1), ("equipKnown", 0), ("mainHandEquipping", 0),
        ("mainHandItem", 0), ("mainHandHeight", 0), ("oldMainHandHeight", 0),
        ("mainHandSwapScale", 0), ("isScoping", 0), ("hasMainHandMapData", 1),
        ("isFallbackItemInvalid", 1), ("initialMainHandRenderState", 0),
        ("rendererObject", 0), ("stateObject", 0), ("rendererClassName", 0),
    ),
    "src/gametest/java/dev/wildercord/gametest/compat/GametestCompat.java": (
        ("keyboardKey", 1), ("setFullscreen", 2), ("renderFrame", 2),
        ("extractMainCamera", 3), ("findNearestStructure", 5),
        ("generateStructureStart", 3), ("findBiomeHorizontal", 10),
        ("biomeHasMob", 3), ("configuredFeatureValue", 1),
        ("dummyStructureContext", 2), ("entityPositionSyncTarget", 1),
        ("moveEntityDeltaTarget", 2), ("syncVelocityFlag", 1),
        ("narrationOutput", 1), ("particlePacketCount", 1),
        ("swingMainHandWithItem", 1),
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


def read_gradle_properties(root: Path) -> dict[str, str]:
    props_path = root / "gradle.properties"
    out: dict[str, str] = {}
    if not props_path.is_file():
        return out
    for raw in props_path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        out[k.strip()] = v.strip()
    return out


def _gradle_bases() -> list[Path]:
    raw_bases = []
    if os.environ.get("GRADLE_USER_HOME"):
        raw_bases.append(Path(os.environ["GRADLE_USER_HOME"]))
    raw_bases.append(Path.home() / ".gradle")
    bases: list[Path] = []
    seen = set()
    for base in raw_bases:
        res = base.resolve()
        if res not in seen:
            seen.add(res)
            bases.append(base)
    return bases


def _valid_mc_jar(path: Path) -> bool:
    name = path.name
    return (
        path.is_file()
        and name.endswith(".jar")
        and not name.endswith(("-sources.jar", "-javadoc.jar", "-javadocs.jar"))
    )


def find_mc_jars(root: Path, mc_version: str, explicit_jars=None):
    if explicit_jars:
        return [Path(j) for j in explicit_jars if Path(j).exists()]

    bases = _gradle_bases()
    for base in bases:
        merged = base / "caches" / "fabric-loom" / mc_version / "minecraft-client.jar"
        if merged.is_file():
            return [merged]

    for base in bases:
        mc_root = base / "caches" / "fabric-loom" / "minecraftMaven" / "net" / "minecraft"
        if not mc_root.is_dir():
            continue
        merged_dirs = sorted(mc_root.glob("minecraft-merged*"))
        merged_jars = []
        for mdir in merged_dirs:
            for ver_dir in sorted(mdir.glob(f"{mc_version}*")):
                if not ver_dir.is_dir():
                    continue
                for jar in sorted(ver_dir.glob("*.jar")):
                    if _valid_mc_jar(jar):
                        merged_jars.append(jar)
        if merged_jars:
            named = [j for j in merged_jars if "named" in str(j) or "loom.mappings" in str(j)]
            return [named[0]] if named else [merged_jars[0]]

    loom_maven = root / ".gradle" / "loom-cache" / "minecraftMaven"
    if loom_maven.is_dir():
        project_jars = []
        for jar in sorted(loom_maven.rglob("*.jar")):
            if not _valid_mc_jar(jar):
                continue
            if jar.name.startswith("minecraft-") and (
                f"-{mc_version}" in jar.name or jar.parent.name.startswith(mc_version)
            ):
                project_jars.append(jar)
        if project_jars:
            merged_proj = [j for j in project_jars if "minecraft-merged" in j.name]
            if merged_proj:
                named = [j for j in merged_proj if "named" in str(j)]
                return [named[0]] if named else [merged_proj[0]]
            return project_jars

    candidates = []
    for base in bases:
        mc_root = base / "caches" / "fabric-loom" / "minecraftMaven" / "net" / "minecraft"
        if not mc_root.is_dir():
            continue
        for prefix in ("minecraft-common*", "minecraft-clientonly*"):
            for pdir in sorted(mc_root.glob(prefix)):
                for ver_dir in sorted(pdir.glob(f"{mc_version}*")):
                    if not ver_dir.is_dir():
                        continue
                    for jar in sorted(ver_dir.glob("*.jar")):
                        if _valid_mc_jar(jar) and jar not in candidates:
                            candidates.append(jar)
    return candidates


def find_mod_class_dirs(root: Path, explicit_dirs=None) -> list[Path]:
    if explicit_dirs is not None:
        return [Path(d) for d in explicit_dirs if Path(d).exists()]
    dirs = []
    for source_set in ("main", "client", "gametest"):
        cls_dir = root / "build" / "classes" / "java" / source_set
        if cls_dir.is_dir():
            dirs.append(cls_dir)
    return dirs


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
    """Batched `javap -c -p -s` inspector backed by JARs and/or compiled class directories."""

    def __init__(self, jars: list[Path], javap_bin: str):
        self.jars = jars
        self.classpath = os.pathsep.join(str(j) for j in jars)
        self.javap = javap_bin
        self.class_entries: set[str] = set()
        for entry_path in jars:
            if entry_path.is_dir():
                for cls in entry_path.rglob("*.class"):
                    self.class_entries.add(cls.relative_to(entry_path).as_posix()[:-6])
            elif entry_path.is_file():
                with zipfile.ZipFile(entry_path) as zf:
                    for entry in zf.namelist():
                        if entry.endswith(".class"):
                            self.class_entries.add(entry[:-6])
        self.has_mod_classes = any(c.startswith(MOD_PREFIX) for c in self.class_entries)
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
            r"(?:class|interface|enum|@interface)\s+(\S+)(.*)\{\s*$"
        )
        # Strict 2-space indent ensures 4-space `    descriptor: Lfoo/Bar;` lines never match member_re!
        member_re = re.compile(r"^  (?=\S)(?!(?:static\s*\{|\}|\{))(.+);\s*$")
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
                    tail = hm.group(2).strip()
                    ext_part = ""
                    impl_part = ""
                    if re.search(r"\bimplements\b", tail):
                        ext_part, impl_part = re.split(r"\bimplements\b", tail, maxsplit=1)
                    else:
                        ext_part = tail
                    ext_part = re.sub(r"^extends\s+", "", ext_part.strip())
                    ext_types = [p.strip().replace(".", "/") for p in ext_part.split(",") if p.strip()]
                    impl_types = [p.strip().replace(".", "/") for p in impl_part.split(",") if p.strip()]
                    sup = ext_types[0] if ext_types else None
                    ifaces = ext_types[1:] + impl_types
                    self._cache[cur_binary] = {
                        "super": sup,
                        "interfaces": ifaces,
                        "fields": set(),
                        "field_descs": {},
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
                    meta["field_descs"][name] = desc
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

    def suggest_classes(self, missing_binary: str, limit: int = 5) -> list[str]:
        simple = missing_binary.replace(".", "/").rsplit("/", 1)[-1].rsplit("$", 1)[-1].lower()
        visible = [c for c in sorted(self.class_entries) if not re.search(r"\$\d+$", c)]
        exact_or_sub = []
        for c in visible:
            csimple = c.rsplit("/", 1)[-1].rsplit("$", 1)[-1].lower()
            if csimple == simple or simple in csimple:
                exact_or_sub.append(c)
        if len(exact_or_sub) < limit:
            by_simple: dict[str, list[str]] = {}
            for c in visible:
                csimple = c.rsplit("/", 1)[-1].rsplit("$", 1)[-1].lower()
                by_simple.setdefault(csimple, []).append(c)
            for match in difflib.get_close_matches(simple, list(by_simple), n=limit, cutoff=0.55):
                for c in by_simple[match]:
                    if c not in exact_or_sub:
                        exact_or_sub.append(c)
        return exact_or_sub[:limit]

    def suggest_methods(self, binary_name: str, missing_name: str, limit: int = 6) -> list[str]:
        out: list[str] = []
        seen: set[str] = set()
        candidates_by_name: dict[str, list[str]] = {}
        low = missing_name.lower()
        for sup in self.supertypes(binary_name):
            if sup == "java/lang/Object":
                continue
            meta = self._cache.get(sup)
            if not meta:
                continue
            for (name, desc) in sorted(meta["methods"]):
                sig = f"{sup}#{name}{desc}"
                if name == missing_name and sig not in seen:
                    seen.add(sig)
                    out.append(sig)
                candidates_by_name.setdefault(name.lower(), []).append(sig)
        if len(out) < limit:
            for lname, sigs in candidates_by_name.items():
                if low in lname or lname in low:
                    for sig in sigs:
                        if sig not in seen:
                            seen.add(sig)
                            out.append(sig)
        if len(out) < limit:
            for match in difflib.get_close_matches(low, list(candidates_by_name), n=limit, cutoff=0.5):
                for sig in candidates_by_name[match]:
                    if sig not in seen:
                        seen.add(sig)
                        out.append(sig)
        if not out:
            own = self._cache.get(binary_name)
            if own:
                for (name, desc) in sorted(own["methods"]):
                    if name not in ("<init>", "<clinit>") and not name.startswith("lambda$"):
                        out.append(f"{binary_name}#{name}{desc}")
                        if len(out) >= limit:
                            break
        return out[:limit]

    def suggest_fields(self, binary_name: str, missing_name: str, limit: int = 6) -> list[str]:
        out: list[str] = []
        seen: set[str] = set()
        candidates_by_name: dict[str, list[str]] = {}
        low = missing_name.lower()
        for sup in self.supertypes(binary_name):
            if sup == "java/lang/Object":
                continue
            meta = self._cache.get(sup)
            if not meta:
                continue
            for fname in sorted(meta["fields"]):
                desc = meta.get("field_descs", {}).get(fname, "")
                label = f"{sup}.{fname}:{desc}" if desc else f"{sup}.{fname}"
                candidates_by_name.setdefault(fname.lower(), []).append(label)
        for lname, labels in candidates_by_name.items():
            if low == lname or low in lname or lname in low:
                for label in labels:
                    if label not in seen:
                        seen.add(label)
                        out.append(label)
        if len(out) < limit:
            for match in difflib.get_close_matches(low, list(candidates_by_name), n=limit, cutoff=0.5):
                for label in candidates_by_name[match]:
                    if label not in seen:
                        seen.add(label)
                        out.append(label)
        if not out:
            own = self._cache.get(binary_name)
            if own:
                for fname in sorted(own["fields"]):
                    desc = own.get("field_descs", {}).get(fname, "")
                    out.append(f"{binary_name}.{fname}:{desc}" if desc else f"{binary_name}.{fname}")
                    if len(out) >= limit:
                        break
        return out[:limit]


def _with_candidates(message: str, candidates: list[str]) -> str:
    if not candidates:
        return message
    return f"{message} (candidates: {', '.join(candidates)})"


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


def is_inspectable_class(binary_name: str, inspector: JarInspector) -> bool:
    return is_mc_class(binary_name) or (
        binary_name.startswith(MOD_PREFIX) and inspector.has_mod_classes
    )


def _method_candidates(inspector: JarInspector, targets: list[str], missing_name: str) -> list[str]:
    out: list[str] = []
    for t in targets:
        for cand in inspector.suggest_methods(t, missing_name):
            if cand not in out:
                out.append(cand)
    return out[:6]


def _field_candidates(inspector: JarInspector, targets: list[str], missing_name: str) -> list[str]:
    out: list[str] = []
    for t in targets:
        for cand in inspector.suggest_fields(t, missing_name):
            if cand not in out:
                out.append(cand)
    return out[:6]


def _effective_at_count(per_method_counts: list[int], ordinal_val: int | None) -> int:
    if ordinal_val is None:
        return sum(per_method_counts)
    return sum(1 for c in per_method_counts if c > ordinal_val)


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
                active_targets = []
                for t in targets:
                    if t.startswith(EXTERNAL_SKIP_PREFIXES):
                        continue
                    if t.startswith(MOD_PREFIX) and not inspector.has_mod_classes:
                        continue
                    if not inspector.has_class(t):
                        where = "MC JAR" if is_mc_class(t) else "JAR/compiled classes"
                        errors.append(_with_candidates(
                            f"{src_path.relative_to(root)}: @Mixin target class missing from {where}: {t}",
                            inspector.suggest_classes(t),
                        ))
                    else:
                        active_targets.append(t)
                        needed_classes.append(t)
                if active_targets:
                    mixin_records.append((src_path, src, file_pkg, exact, wildcards, active_targets, default_req))

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
                    if parsed and is_inspectable_class(parsed[1], inspector):
                        needed_classes.append(parsed[1])
                    elif ts.startswith("(") and ")" in ts:
                        ret_part = ts.split(")", 1)[1].strip()
                        if ret_part.startswith("L") and ";" in ret_part:
                            owner = ret_part[1:ret_part.index(";")]
                            if is_inspectable_class(owner, inspector):
                                needed_classes.append(owner)
                    elif ts.startswith("L") and ";" in ts:
                        owner = ts[1:ts.index(";")]
                        if is_inspectable_class(owner, inspector):
                            needed_classes.append(owner)
                    elif "/" in ts and (
                        ts.startswith(MC_OWNERS)
                        or (ts.startswith(MOD_PREFIX) and inspector.has_mod_classes)
                    ):
                        needed_classes.append(ts.replace(".", "/"))

    inspector.ensure_loaded(needed_classes)

    inj_pattern = re.compile(r"@(" + "|".join(INJECTOR_NAMES) + r")\s*\(")
    shadow_pattern = re.compile(r"@(Shadow|Accessor|Invoker)\b")
    local_ann_re = re.compile(r"@(?:[A-Za-z0-9_.]+\.)?Local\s*\(")
    strip_ann_re = re.compile(r"@[A-Za-z0-9_.]+\s*(?:\([^)]*\))?")

    for src_path, src, file_pkg, exact, wildcards, target_classes, default_req in mixin_records:
        rel = str(src_path.relative_to(root))
        all_mod_targets = all(t.startswith(MOD_PREFIX) for t in target_classes)

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
                    if not any(inspector.find_methods(t, member_name) for t in target_classes):
                        errors.append(_with_candidates(
                            f"{rel}: @Shadow method {member_name}() not found on {target_classes}",
                            _method_candidates(inspector, target_classes, member_name),
                        ))
                else:
                    before_eq = cleaned.split("=", 1)[0].strip()
                    if before_eq:
                        member_name = before_eq.split()[-1]
                        if not any(inspector.has_field(t, member_name) for t in target_classes):
                            errors.append(_with_candidates(
                                f"{rel}: @Shadow field {member_name} not found on {target_classes}",
                                _field_candidates(inspector, target_classes, member_name),
                            ))
            elif ann_args.get("remap") == "false" and not all_mod_targets:
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
                    if not any(any(inspector.has_field(t, c) for c in cands) for t in target_classes):
                        errors.append(_with_candidates(
                            f"{rel}: @Accessor field {cands[0]} not found on {target_classes}",
                            _field_candidates(inspector, target_classes, cands[0]),
                        ))
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
                    if not any(any(inspector.find_methods(t, c) for c in cands) for t in target_classes):
                        errors.append(_with_candidates(
                            f"{rel}: @Invoker method {cands[0]}() not found on {target_classes}",
                            _method_candidates(inspector, target_classes, cands[0]),
                        ))

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
                for t in target_classes:
                    found_for_spec.extend(inspector.find_methods(t, m_name, m_desc))
                if not found_for_spec:
                    if require_val != 0 and (all_mod_targets or not remap_false):
                        errors.append(_with_candidates(
                            f"{rel}: injector target method '{spec}' not found on {target_classes}",
                            _method_candidates(inspector, target_classes, m_name),
                        ))
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
                ord_raw = at_args.get("ordinal", "").strip()
                ordinal_val = int(ord_raw) if ord_raw.isdigit() else None
                if at_val == "NEW":
                    if t_raw.startswith("(") and ")" in t_raw:
                        args_part, ret_part = t_raw.split(")", 1)
                        new_owner = ret_part.strip()
                        if new_owner.startswith("L"):
                            new_owner = new_owner[1:]
                        new_owner = new_owner.rstrip(";").replace(".", "/")
                        ctor_desc = args_part + ")V"
                        if is_inspectable_class(new_owner, inspector):
                            if not inspector.has_class(new_owner):
                                errors.append(_with_candidates(
                                    f"{rel}: @At(\"NEW\") target class missing from MC JAR: {new_owner}",
                                    inspector.suggest_classes(new_owner),
                                ))
                                continue
                            inspector.ensure_loaded([new_owner])
                            if not inspector.find_methods(new_owner, "<init>", ctor_desc):
                                errors.append(_with_candidates(
                                    f"{rel}: @At(\"NEW\") constructor missing in JAR: {new_owner}.<init>{ctor_desc}",
                                    inspector.suggest_methods(new_owner, "<init>"),
                                ))
                                continue
                        if matched_methods:
                            per_counts = [
                                sum(
                                    1 for ik, iowner, iname, idesc in insns
                                    if ik == "INVOKE" and iowner == new_owner and iname == "<init>" and idesc == ctor_desc
                                )
                                for _, _, _, insns in matched_methods
                            ]
                            cnt = _effective_at_count(per_counts, ordinal_val)
                            if cnt == 0 and require_val != 0:
                                errors.append(_with_candidates(
                                    f"{rel}: @At(\"NEW\", target=\"{t_raw}\") matched 0 instructions in {method_specs}",
                                    inspector.suggest_methods(new_owner, "<init>") if inspector.has_class(new_owner) else [],
                                ))
                            if expect_val is not None and cnt < expect_val:
                                errors.append(f"{rel}: @At(\"NEW\") count {cnt} < expect {expect_val} in {method_specs}")
                            if allow_val is not None and cnt > allow_val:
                                errors.append(f"{rel}: @At(\"NEW\") count {cnt} > allow {allow_val} in {method_specs}")
                        continue
                    new_owner = t_raw.split(";", 1)[0]
                    if new_owner.startswith("L"):
                        new_owner = new_owner[1:]
                    new_owner = new_owner.replace(".", "/")
                    if is_inspectable_class(new_owner, inspector) and not inspector.has_class(new_owner):
                        errors.append(_with_candidates(
                            f"{rel}: @At(\"NEW\") target class missing from MC JAR: {new_owner}",
                            inspector.suggest_classes(new_owner),
                        ))
                    if matched_methods:
                        per_counts = [
                            sum(
                                1 for ik, iowner, _, _ in insns
                                if ik == "NEW" and iowner == new_owner
                            )
                            for _, _, _, insns in matched_methods
                        ]
                        cnt = _effective_at_count(per_counts, ordinal_val)
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
                if is_inspectable_class(t_owner, inspector):
                    if not inspector.has_class(t_owner):
                        errors.append(_with_candidates(
                            f"{rel}: @At target owner missing from MC JAR: {t_owner}",
                            inspector.suggest_classes(t_owner),
                        ))
                        continue
                    inspector.ensure_loaded([t_owner])
                    if at_val == "INVOKE":
                        if not inspector.find_methods(t_owner, t_name, t_desc):
                            errors.append(_with_candidates(
                                f"{rel}: @At(\"INVOKE\") target missing in JAR: {t_raw}",
                                inspector.suggest_methods(t_owner, t_name),
                            ))
                            continue
                    elif at_val == "FIELD":
                        if not inspector.has_field(t_owner, t_name):
                            errors.append(_with_candidates(
                                f"{rel}: @At(\"FIELD\") target missing in JAR: {t_raw}",
                                inspector.suggest_fields(t_owner, t_name),
                            ))
                            continue

                if matched_methods:
                    per_counts = []
                    for _, _, _, insns in matched_methods:
                        mcnt = 0
                        for ik, iowner, iname, idesc in insns:
                            if ik != at_val or iname != t_name:
                                continue
                            if t_desc is not None and idesc != t_desc:
                                continue
                            if (iowner == t_owner
                                    or (inspector.has_class(iowner) and inspector.has_class(t_owner)
                                        and (inspector.is_subtype(iowner, t_owner) or inspector.is_subtype(t_owner, iowner)))):
                                mcnt += 1
                        per_counts.append(mcnt)
                    cnt = _effective_at_count(per_counts, ordinal_val)
                    if cnt == 0 and require_val != 0:
                        hint = (
                            inspector.suggest_methods(t_owner, t_name)
                            if at_val == "INVOKE" and inspector.has_class(t_owner)
                            else inspector.suggest_fields(t_owner, t_name)
                            if at_val == "FIELD" and inspector.has_class(t_owner)
                            else []
                        )
                        errors.append(_with_candidates(
                            f"{rel}: @At(\"{at_val}\", target=\"{t_raw}\") matched 0 instructions in {method_specs}",
                            hint,
                        ))
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


COMPAT_METHOD_DECL_RE = re.compile(
    r"^\s*(?:(?:public|protected|private|static|final|abstract|synchronized|native|default)\s+)*"
    r"(?:@[A-Za-z0-9_.]+\s+)*[A-Za-z0-9_$.\[\]]+\s+([A-Za-z0-9_$]+)\s*\("
)
FORBIDDEN_CACHE_RE = re.compile(r"\b(?:WeakHashMap|IdentityHashMap|ConcurrentHashMap|HashMap|WeakReference)\b")


def _depth_one_statements(src: str) -> list[str]:
    """Extract top-level class member headers at brace depth 1 (method/nested bodies omitted)."""
    statements = []
    buf: list[str] = []
    depth = 0
    in_str: str | None = None
    i = 0
    n = len(src)
    while i < n:
        ch = src[i]
        if in_str:
            if ch == "\\" and i + 1 < n:
                i += 2
                continue
            if ch == in_str:
                in_str = None
        elif ch in ('"', "'"):
            in_str = ch
        elif ch == "{":
            if depth == 1:
                stmt = "".join(buf).strip()
                if stmt:
                    statements.append(stmt)
                buf = []
            depth += 1
        elif ch == "}":
            if depth > 0:
                depth -= 1
            if depth == 1:
                buf = []
        elif ch == ";" and depth == 1:
            stmt = "".join(buf).strip()
            if stmt:
                statements.append(stmt)
            buf = []
        elif depth == 1:
            buf.append(ch)
        i += 1
    return statements


def verify_compat_contract(root: Path) -> list[str]:
    compat_main = root / "src" / "main" / "java" / "dev" / "wildercord" / "compat"
    if not compat_main.is_dir():
        return []
    errors: list[str] = []
    for path in sorted((root / "src").rglob("compat/*.java")):
        src = strip_comments(path.read_text(encoding="utf-8"))
        m = FORBIDDEN_CACHE_RE.search(src)
        if m:
            errors.append(
                f"{path.relative_to(root)}: forbidden static/map cache reference '{m.group(0)}' in compat/"
            )

    strip_param_ann_re = re.compile(r"@[A-Za-z0-9_.]+(?:\([^)]*\))?\s*")
    for rel_path, expected_sigs in COMPAT_CONTRACT.items():
        path = root / rel_path
        if not path.is_file():
            errors.append(f"Missing required compat seam file: {rel_path}")
            continue
        src = strip_comments(path.read_text(encoding="utf-8"))
        found_sigs: set[tuple[str, int]] = set()
        for stmt in _depth_one_statements(src):
            before_eq = _strip_angle_generics(stmt.split("=", 1)[0]).strip()
            if "(" not in before_eq:
                if re.search(r"\b(?:class|interface|enum|record)\b", before_eq):
                    continue
                if re.search(r"\bstatic\b", before_eq) and not re.search(r"\bfinal\b", before_eq):
                    errors.append(
                        f"{rel_path}: mutable non-final static field not allowed in core compat seam: {before_eq}"
                    )
                continue
            cleaned_header = _strip_angle_generics(strip_param_ann_re.sub(" ", stmt))
            if not re.search(r"\b(?:public|protected)\b", cleaned_header.split("(", 1)[0]):
                continue
            m = COMPAT_METHOD_DECL_RE.match(cleaned_header)
            if m:
                name = m.group(1)
                try:
                    body, _ = extract_balanced(cleaned_header, m.end() - 1, "(", ")")
                except ValueError:
                    continue
                arity = len(split_top_level(body, ",")) if body.strip() else 0
                found_sigs.add((name, arity))
        for req_name, req_arity in expected_sigs:
            if (req_name, req_arity) not in found_sigs:
                errors.append(
                    f"{rel_path}: missing required compat seam method {req_name}({req_arity} args)"
                )
    return errors


def verify_descriptor_alignment(root: Path) -> list[str]:
    props = read_gradle_properties(root)
    if not props:
        return []
    errors: list[str] = []
    java_ver = props.get("java_version")
    if java_ver:
        expected_compat = f"JAVA_{java_ver}"
        for mf in sorted((root / "src").rglob("*.mixins.json")):
            cfg = json.loads(mf.read_text(encoding="utf-8"))
            if cfg.get("compatibilityLevel") != expected_compat:
                errors.append(
                    f"{mf.relative_to(root)}: compatibilityLevel '{cfg.get('compatibilityLevel')}' "
                    f"!= '{expected_compat}' from gradle.properties"
                )

    fmod_path = root / "src" / "main" / "resources" / "fabric.mod.json"
    if fmod_path.is_file() and {"minecraft_version", "loader_version", "java_version", "fabric_api_version"} <= props.keys():
        fmod = json.loads(fmod_path.read_text(encoding="utf-8"))
        deps = fmod.get("depends", {})
        expected_deps = {
            "fabricloader": ">=" + props["loader_version"],
            "minecraft": "~" + props["minecraft_version"],
            "java": ">=" + props["java_version"],
            "fabric-api": ">=" + props["fabric_api_version"],
        }
        for key, exp in expected_deps.items():
            if deps.get(key) != exp:
                errors.append(
                    f"src/main/resources/fabric.mod.json: depends.{key} is '{deps.get(key)}', expected '{exp}'"
                )

    lock_path = root / "profiles" / "dependencies.lock.json"
    if lock_path.is_file() and {
        "minecraft_version", "loader_version", "fabric_api_version", "sodium_version", "iris_version"
    } <= props.keys():
        lock = json.loads(lock_path.read_text(encoding="utf-8-sig"))
        if lock.get("minecraft") != props["minecraft_version"]:
            errors.append(
                f"profiles/dependencies.lock.json: minecraft '{lock.get('minecraft')}' "
                f"!= '{props['minecraft_version']}'"
            )
        if lock.get("fabric_loader") != props["loader_version"]:
            errors.append(
                f"profiles/dependencies.lock.json: fabric_loader '{lock.get('fabric_loader')}' "
                f"!= '{props['loader_version']}'"
            )
        pins = {entry.get("project"): entry for entry in lock.get("dependencies", []) if isinstance(entry, dict)}
        for proj, prop_key in (
            ("fabric-api", "fabric_api_version"),
            ("sodium", "sodium_version"),
            ("iris", "iris_version"),
        ):
            pin = pins.get(proj)
            if not pin:
                errors.append(f"profiles/dependencies.lock.json: missing '{proj}' entry")
                continue
            if pin.get("version") != props[prop_key]:
                errors.append(
                    f"profiles/dependencies.lock.json: {proj} version '{pin.get('version')}' != '{props[prop_key]}'"
                )
            if props["minecraft_version"] not in pin.get("game_versions", []):
                errors.append(
                    f"profiles/dependencies.lock.json: {proj} game_versions {pin.get('game_versions')} "
                    f"does not include {props['minecraft_version']}"
                )
    return errors


def verify(root: Path | None = None, mc_jars=None, allow_missing_jar: bool = False, mod_classes=None) -> list[str]:
    base = ROOT if root is None else Path(root)
    mc_version = read_mc_version(base)
    profile = DataPackProfile.for_version(mc_version)

    jars = find_mc_jars(base, mc_version, mc_jars)
    mod_dirs = find_mod_class_dirs(base, mod_classes)
    javap = find_javap()
    inspector = None

    if jars and javap:
        inspector = JarInspector(jars + mod_dirs, javap)
    elif not allow_missing_jar:
        missing_what = "Minecraft Loom JAR" if not jars else "javap executable"
        return [f"Missing {missing_what} for {mc_version}; pass --mc-jar or run with Loom cache populated"]

    errors = []
    errors.extend(verify_descriptor_alignment(base))
    errors.extend(verify_compat_contract(base))
    errors.extend(verify_mixins(base, inspector))
    errors.extend(verify_datapacks(base, profile, inspector))
    return errors


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--mc-jar", action="append", dest="mc_jars", help="Path to merged MC client JAR (repeatable)")
    parser.add_argument("--mod-classes", action="append", dest="mod_classes", help="Compiled mod class directory (repeatable)")
    parser.add_argument("--allow-missing-jar", action="store_true",
                        help="Skip bytecode checks when no local Loom JAR or javap is present")
    args = parser.parse_args(argv)
    errors = verify(
        args.root,
        mc_jars=args.mc_jars,
        mod_classes=args.mod_classes,
        allow_missing_jar=args.allow_missing_jar,
    )
    if errors:
        for err in errors:
            print("ERROR:", err, file=sys.stderr)
        return 1
    mc_version = read_mc_version(args.root)
    has_jar = bool(find_mc_jars(args.root, mc_version, args.mc_jars) and find_javap())
    has_mod = bool(find_mod_class_dirs(args.root, args.mod_classes))
    if has_jar and has_mod:
        mode = "bytecode (mc + mod) + datapack"
    elif has_jar:
        mode = "bytecode + datapack"
    else:
        mode = "structural datapack"
    print(f"Port verification passed ({mc_version}, {mode}).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
