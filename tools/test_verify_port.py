"""Unit tests for tools/verify_port.py using synthetic JARs compiled with javac."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parent))
import verify_port


def find_javac():
    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        for name in ("javac.exe", "javac"):
            cand = Path(java_home) / "bin" / name
            if cand.is_file():
                return str(cand)
    return shutil.which("javac")


JAVAC = find_javac()
JAVAP = verify_port.find_javap()


def build_synthetic_mc_jar(tmp: Path) -> Path:
    src_dir = tmp / "mc_src"
    cls_dir = tmp / "mc_cls"
    cls_dir.mkdir(parents=True, exist_ok=True)
    files = {
        "net/minecraft/world/Helper.java": (
            "package net.minecraft.world;\n"
            "public class Helper { public Helper() {} public Helper(int x) {} public void ping(int v) {} }\n"
        ),
        "net/minecraft/world/Target.java": (
            "package net.minecraft.world;\n"
            "public class Target {\n"
            "    private int secretValue;\n"
            "    public void oneCall(Helper h) { h.ping(1); new Helper(3); }\n"
            "    public void twoCalls(Helper h) { h.ping(1); h.ping(2); }\n"
            "    public void twoInts(int a, int b) {}\n"
            "    private void hiddenCall(String s) {}\n"
            "}\n"
        ),
        "net/minecraft/world/level/levelgen/placement/PlacementModifierType.java": (
            "package net.minecraft.world.level.levelgen.placement;\n"
            "public class PlacementModifierType {\n"
            "    public static final String COUNT = \"count\";\n"
            "    public static void init() { String a = \"count\"; String b = \"biome\"; }\n"
            "}\n"
        ),
        "net/minecraft/world/level/levelgen/blockpredicates/BlockPredicateType.java": (
            "package net.minecraft.world.level.levelgen.blockpredicates;\n"
            "public class BlockPredicateType {\n"
            "    public static void init() { String a = \"solid\"; }\n"
            "}\n"
        ),
        "net/minecraft/world/level/storage/loot/functions/LootItemFunctions.java": (
            "package net.minecraft.world.level.storage.loot.functions;\n"
            "public class LootItemFunctions {\n"
            "    public static void init() { String a = \"set_count\"; String b = \"exploration_map\"; }\n"
            "}\n"
        ),
        "net/minecraft/world/level/storage/loot/predicates/LootItemConditions.java": (
            "package net.minecraft.world.level.storage.loot.predicates;\n"
            "public class LootItemConditions {\n"
            "    public static void init() { String a = \"random_chance\"; }\n"
            "}\n"
        ),
        "net/minecraft/core/component/DataComponents.java": (
            "package net.minecraft.core.component;\n"
            "public class DataComponents {\n"
            "    public static void init() { String a = \"custom_data\"; }\n"
            "}\n"
        ),
        "net/minecraft/world/item/crafting/RecipeSerializers.java": (
            "package net.minecraft.world.item.crafting;\n"
            "public class RecipeSerializers {\n"
            "    public static void init() { String a = \"crafting_shaped\"; }\n"
            "}\n"
        ),
    }
    java_paths = []
    for rel, code in files.items():
        p = src_dir / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(code, encoding="utf-8")
        java_paths.append(str(p))
    proc = subprocess.run([JAVAC, "-g", "-d", str(cls_dir), *java_paths], capture_output=True, text=True)
    if proc.returncode != 0:
        raise RuntimeError(proc.stderr)
    jar_path = tmp / "synthetic-mc.jar"
    with zipfile.ZipFile(jar_path, "w") as zf:
        for cls in cls_dir.rglob("*.class"):
            zf.write(cls, cls.relative_to(cls_dir).as_posix())
    return jar_path


def write_fixture_repo(repo: Path, mixin_code: str, placed_mod="minecraft:count",
                       stale_feat=False, map_dest="wildercord:maps"):
    (repo / "gradle.properties").write_text("minecraft_version=26.2\n", encoding="utf-8")
    mjson = repo / "src/main/resources/wildercord.mixins.json"
    mjson.parent.mkdir(parents=True, exist_ok=True)
    mjson.write_text(json.dumps({
        "required": True,
        "package": "dev.wildercord.mixin",
        "mixins": ["SampleMixin"],
        "injectors": {"defaultRequire": 1},
    }), encoding="utf-8")
    msrc = repo / "src/main/java/dev/wildercord/mixin/SampleMixin.java"
    msrc.parent.mkdir(parents=True, exist_ok=True)
    msrc.write_text(mixin_code, encoding="utf-8")

    data = repo / "src/main/resources/data/wildercord"
    (data / "worldgen/configured_feature").mkdir(parents=True, exist_ok=True)
    if stale_feat:
        (data / "worldgen/feature").mkdir(parents=True, exist_ok=True)

    pf = data / "worldgen/placed_feature/sample.json"
    pf.parent.mkdir(parents=True, exist_ok=True)
    pf.write_text(json.dumps({
        "feature": "wildercord:sample",
        "placement": [
            {"type": placed_mod, "count": 1},
            {"type": "minecraft:count", "predicate": {"type": "minecraft:solid"}},
        ],
    }), encoding="utf-8")

    lt = data / "loot_table/chests/sample.json"
    lt.parent.mkdir(parents=True, exist_ok=True)
    lt.write_text(json.dumps({
        "pools": [{
            "entries": [{
                "type": "minecraft:item",
                "name": "minecraft:map",
                "functions": [{"function": "minecraft:exploration_map", "destination": map_dest}],
            }],
            "conditions": [{"condition": "minecraft:random_chance", "chance": 0.5}],
        }],
    }), encoding="utf-8")

    rec = data / "recipe/sample.json"
    rec.parent.mkdir(parents=True, exist_ok=True)
    rec.write_text(json.dumps({
        "type": "minecraft:crafting_shaped",
        "result": {"id": "minecraft:stick", "components": {"minecraft:custom_data": {}}},
    }), encoding="utf-8")


VALID_MIXIN = """package dev.wildercord.mixin;

import net.minecraft.world.Target;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(Target.class)
public interface SampleMixin {
    @Shadow int secretValue = 0;

    @Accessor("secretValue")
    int getSecret();

    @Invoker("hiddenCall")
    void callHidden(String s);

    @WrapOperation(method = "oneCall", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/Helper;ping(I)V"), require = 1, expect = 1, allow = 1)
    static void wrapPing() {}
}
"""


@unittest.skipUnless(JAVAC and JAVAP, "Requires JDK javac and javap")
class VerifyPortTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls._tmp = tempfile.TemporaryDirectory()
        cls.jar = build_synthetic_mc_jar(Path(cls._tmp.name))

    @classmethod
    def tearDownClass(cls):
        cls._tmp.cleanup()

    def test_valid_mixin_and_datapack_passes(self):
        with tempfile.TemporaryDirectory() as d:
            repo = Path(d)
            write_fixture_repo(repo, VALID_MIXIN)
            errors = verify_port.verify(repo, mc_jars=[self.jar])
            self.assertEqual(errors, [])

    def test_missing_target_method_fails(self):
        bad = VALID_MIXIN.replace('method = "oneCall"', 'method = "missingMethod"')
        with tempfile.TemporaryDirectory() as d:
            repo = Path(d)
            write_fixture_repo(repo, bad)
            errors = verify_port.verify(repo, mc_jars=[self.jar])
            self.assertTrue(any("missingMethod" in e for e in errors), errors)

    def test_allow_count_exceeded_fails(self):
        bad = VALID_MIXIN.replace('method = "oneCall"', 'method = "twoCalls"')
        with tempfile.TemporaryDirectory() as d:
            repo = Path(d)
            write_fixture_repo(repo, bad)
            errors = verify_port.verify(repo, mc_jars=[self.jar])
            self.assertTrue(any("allow 1" in e for e in errors), errors)

    def test_ambiguous_local_args_only_fails(self):
        bad = VALID_MIXIN + """
    class Inner {
        @Inject(method = "twoInts", at = @At("HEAD"))
        void hook(@com.llamalad7.mixinextras.sugar.Local(argsOnly = true) int val) {}
    }
"""
        with tempfile.TemporaryDirectory() as d:
            repo = Path(d)
            write_fixture_repo(repo, bad)
            errors = verify_port.verify(repo, mc_jars=[self.jar])
            self.assertTrue(any("ambiguous" in e for e in errors), errors)

    def test_new_constructor_descriptor_checked(self):
        good = VALID_MIXIN + """
    @Inject(method = "oneCall", at = @At(value = "NEW", target = "(I)Lnet/minecraft/world/Helper;"), require = 1, expect = 1, allow = 1)
    static void wrapNew() {}
"""
        bad = VALID_MIXIN + """
    @Inject(method = "oneCall", at = @At(value = "NEW", target = "(Ljava/lang/String;)Lnet/minecraft/world/Helper;"), require = 1)
    static void wrapBadNew() {}
"""
        with tempfile.TemporaryDirectory() as d:
            repo = Path(d)
            write_fixture_repo(repo, good)
            self.assertEqual(verify_port.verify(repo, mc_jars=[self.jar]), [])
            write_fixture_repo(repo, bad)
            errors = verify_port.verify(repo, mc_jars=[self.jar])
            self.assertTrue(any("constructor missing" in e for e in errors), errors)

    def test_stale_feature_dir_hash_map_and_unknown_modifier_fail(self):
        with tempfile.TemporaryDirectory() as d:
            repo = Path(d)
            write_fixture_repo(repo, VALID_MIXIN, placed_mod="minecraft:bogus_modifier",
                               stale_feat=True, map_dest="#wildercord:maps")
            errors = verify_port.verify(repo, mc_jars=[self.jar])
            joined = "\n".join(errors)
            self.assertIn("Stale feature directory", joined)
            self.assertIn("must not start with '#'", joined)
            self.assertIn("bogus_modifier", joined)

    def test_candidate_hints_on_renamed_targets(self):
        bad_method = VALID_MIXIN.replace('method = "oneCall"', 'method = "oneCal"')
        with tempfile.TemporaryDirectory() as d:
            repo = Path(d)
            write_fixture_repo(repo, bad_method)
            errors = verify_port.verify(repo, mc_jars=[self.jar])
            joined = "\n".join(errors)
            self.assertIn("candidates:", joined)
            self.assertIn("Target#oneCall", joined)

        bad_field = VALID_MIXIN.replace("int secretValue = 0;", "int secretVal = 0;")
        with tempfile.TemporaryDirectory() as d:
            repo = Path(d)
            write_fixture_repo(repo, bad_field)
            errors = verify_port.verify(repo, mc_jars=[self.jar])
            joined = "\n".join(errors)
            self.assertIn("secretValue", joined)

    def test_mod_targeted_mixin_verified_when_compiled(self):
        with tempfile.TemporaryDirectory() as d:
            tmp = Path(d)
            mod_src = tmp / "mod_src" / "dev/wildercord/client/ViewModel.java"
            mod_cls = tmp / "mod_cls"
            mod_cls.mkdir(parents=True, exist_ok=True)
            mod_src.parent.mkdir(parents=True, exist_ok=True)
            mod_src.write_text(
                "package dev.wildercord.client;\n"
                "import net.minecraft.world.Helper;\n"
                "public class ViewModel { public void render(Helper h) { h.ping(7); } }\n",
                encoding="utf-8",
            )
            proc = subprocess.run(
                [JAVAC, "-cp", str(self.jar), "-d", str(mod_cls), str(mod_src)],
                capture_output=True, text=True,
            )
            self.assertEqual(proc.returncode, 0, proc.stderr)

            repo = tmp / "repo"
            repo.mkdir(parents=True, exist_ok=True)
            mod_mixin = """package dev.wildercord.mixin;

import dev.wildercord.client.ViewModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(value = ViewModel.class, remap = false)
public interface SampleMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/Helper;ping(I)V"), require = 1, expect = 1, allow = 1)
    static void wrapPing() {}
}
"""
            write_fixture_repo(repo, mod_mixin)
            self.assertEqual(
                verify_port.verify(repo, mc_jars=[self.jar], mod_classes=[mod_cls]),
                [],
            )

            broken_mod_mixin = mod_mixin.replace('method = "render"', 'method = "draw"')
            write_fixture_repo(repo, broken_mod_mixin)
            errors = verify_port.verify(repo, mc_jars=[self.jar], mod_classes=[mod_cls])
            self.assertTrue(any("draw" in e and "ViewModel#render" in e for e in errors), errors)

    def test_ordinal_package_private_static_and_multi_interface_extends(self):
        with tempfile.TemporaryDirectory() as d:
            tmp = Path(d)
            mc_src = tmp / "mc2_src"
            mc_cls = tmp / "mc2_cls"
            mc_cls.mkdir(parents=True, exist_ok=True)
            iface_files = {
                "net/minecraft/world/LeftIfc.java": "package net.minecraft.world;\npublic interface LeftIfc { boolean leftCheck(); }\n",
                "net/minecraft/world/RightIfc.java": "package net.minecraft.world;\npublic interface RightIfc { boolean rightCheck(); }\n",
                "net/minecraft/world/JoinedIfc.java": "package net.minecraft.world;\npublic interface JoinedIfc extends LeftIfc, RightIfc {}\n",
                "net/minecraft/world/UseJoined.java": (
                    "package net.minecraft.world;\n"
                    "public class UseJoined {\n"
                    "    public boolean probe(JoinedIfc j, Helper h) { h.ping(1); h.ping(2); return j.leftCheck() && j.rightCheck(); }\n"
                    "}\n"
                ),
            }
            paths = []
            for rel, code in iface_files.items():
                p = mc_src / rel
                p.parent.mkdir(parents=True, exist_ok=True)
                p.write_text(code, encoding="utf-8")
                paths.append(str(p))
            proc = subprocess.run([JAVAC, "-cp", str(self.jar), "-g", "-d", str(mc_cls), *paths], capture_output=True, text=True)
            self.assertEqual(proc.returncode, 0, proc.stderr)
            extra_jar = tmp / "extra-mc.jar"
            with zipfile.ZipFile(extra_jar, "w") as zf:
                for cls in mc_cls.rglob("*.class"):
                    zf.write(cls, cls.relative_to(mc_cls).as_posix())

            mod_src = tmp / "mod_src" / "dev/wildercord/cast/PkgHelper.java"
            mod_cls = tmp / "mod_cls"
            mod_cls.mkdir(parents=True, exist_ok=True)
            mod_src.parent.mkdir(parents=True, exist_ok=True)
            mod_src.write_text(
                "package dev.wildercord.cast;\n"
                "public final class PkgHelper { static void step() {} }\n",
                encoding="utf-8",
            )
            proc = subprocess.run([JAVAC, "-d", str(mod_cls), str(mod_src)], capture_output=True, text=True)
            self.assertEqual(proc.returncode, 0, proc.stderr)

            repo = tmp / "repo"
            repo.mkdir(parents=True, exist_ok=True)
            mixin_code = """package dev.wildercord.mixin;

import net.minecraft.world.UseJoined;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(UseJoined.class)
public interface SampleMixin {
    @WrapOperation(method = "probe", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/Helper;ping(I)V", ordinal = 0), require = 1, expect = 1, allow = 1)
    static void wrapFirstPing() {}

    @WrapOperation(method = "probe", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/JoinedIfc;rightCheck()Z"), require = 1, expect = 1, allow = 1)
    static boolean wrapRight() { return true; }
}
"""
            write_fixture_repo(repo, mixin_code)
            pkg_mixin = repo / "src/main/java/dev/wildercord/mixin/PkgMixin.java"
            pkg_mixin.write_text(
                "package dev.wildercord.mixin;\n"
                "import dev.wildercord.cast.PkgHelper;\n"
                "import org.spongepowered.asm.mixin.Mixin;\n"
                "import org.spongepowered.asm.mixin.injection.At;\n"
                "import org.spongepowered.asm.mixin.injection.Inject;\n"
                "@Mixin(value = PkgHelper.class, remap = false)\n"
                "public interface PkgMixin { @Inject(method = \"step\", at = @At(\"HEAD\"), require = 1) static void onStep() {} }\n",
                encoding="utf-8",
            )
            mjson = repo / "src/main/resources/wildercord.mixins.json"
            mjson.write_text(json.dumps({
                "required": True, "package": "dev.wildercord.mixin",
                "mixins": ["SampleMixin", "PkgMixin"], "injectors": {"defaultRequire": 1},
            }), encoding="utf-8")
            self.assertEqual(
                verify_port.verify(repo, mc_jars=[self.jar, extra_jar], mod_classes=[mod_cls]),
                [],
            )

            out_of_range_ord = mixin_code.replace("ordinal = 0", "ordinal = 5")
            write_fixture_repo(repo, out_of_range_ord)
            mjson.write_text(json.dumps({
                "required": True, "package": "dev.wildercord.mixin",
                "mixins": ["SampleMixin"], "injectors": {"defaultRequire": 1},
            }), encoding="utf-8")
            errors = verify_port.verify(repo, mc_jars=[self.jar, extra_jar], mod_classes=[mod_cls])
            self.assertTrue(any("matched 0 instructions" in e for e in errors), errors)


class VerifyPortStaticTests(unittest.TestCase):
    def test_parse_javap_handles_multi_interface_extends_and_package_private_static(self):
        inspector = verify_port.JarInspector.__new__(verify_port.JarInspector)
        inspector.class_entries = {
            "net/minecraft/world/level/LevelReader",
            "net/minecraft/world/level/BlockAndLightGetter",
            "net/minecraft/world/level/CollisionGetter",
            "dev/wildercord/cast/Wards",
        }
        inspector._cache = {}
        inspector._parse_javap_output("""
Compiled from "LevelReader.java"
public interface net.minecraft.world.level.LevelReader extends net.minecraft.world.level.BlockAndLightGetter,net.minecraft.world.level.CollisionGetter {
  public default boolean isEmptyBlock(net.minecraft.core.BlockPos);
    descriptor: (Lnet/minecraft/core/BlockPos;)Z
}
public final class dev.wildercord.cast.Wards {
  static void foresight(dev.wildercord.cast.Cast, net.minecraft.world.entity.LivingEntity, int, int);
    descriptor: (Ldev/wildercord/cast/Cast;Lnet/minecraft/world/entity/LivingEntity;II)V
  static {};
    descriptor: ()V
}
""")
        lr = inspector._cache["net/minecraft/world/level/LevelReader"]
        self.assertEqual(lr["super"], "net/minecraft/world/level/BlockAndLightGetter")
        self.assertIn("net/minecraft/world/level/CollisionGetter", lr["interfaces"])
        wards = inspector._cache["dev/wildercord/cast/Wards"]
        self.assertIn(
            ("foresight", "(Ldev/wildercord/cast/Cast;Lnet/minecraft/world/entity/LivingEntity;II)V"),
            wards["methods"],
        )
        self.assertEqual(
            verify_port._effective_at_count([2], 0),
            1,
        )
        self.assertEqual(
            verify_port._effective_at_count([2], 2),
            0,
        )
    def test_find_mc_jars_loom_maven_layouts(self):
        with tempfile.TemporaryDirectory() as d:
            tmp = Path(d)
            gh = tmp / "gradle_home"
            merged_dir = gh / "caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-abc/1.21.11-loom.mappings.1_21_11-named"
            merged_dir.mkdir(parents=True)
            sources = merged_dir / "minecraft-merged-abc-1.21.11-sources.jar"
            jar = merged_dir / "minecraft-merged-abc-1.21.11.jar"
            sources.write_bytes(b"src")
            # A real zip carrying the Mojang-mapped marker, or find_mc_jars rejects it.
            with zipfile.ZipFile(jar, "w") as archive:
                archive.writestr(verify_port._MOJANG_MARKERS[0], b"")
            # _gradle_bases() also looks at Path.home()/.gradle, so a developer machine that has
            # ever built 1.21.11 would answer this from its own cache. Point HOME at the fixture too.
            prev = {k: os.environ.get(k) for k in ("GRADLE_USER_HOME", "HOME", "USERPROFILE")}
            try:
                os.environ["GRADLE_USER_HOME"] = str(gh)
                os.environ["HOME"] = str(tmp / "no_home")
                os.environ["USERPROFILE"] = str(tmp / "no_home")
                found = verify_port.find_mc_jars(tmp / "repo", "1.21.11")
            finally:
                for key, value in prev.items():
                    if value is None:
                        os.environ.pop(key, None)
                    else:
                        os.environ[key] = value
            self.assertEqual(found, [jar])

    def test_compat_contract_and_descriptor_alignment_checks(self):
        with tempfile.TemporaryDirectory() as d:
            repo = Path(d)
            (repo / "gradle.properties").write_text(
                "minecraft_version=26.2\nloader_version=0.19.5\njava_version=25\n"
                "fabric_api_version=0.161.0+26.2\nsodium_version=mc26.2-0.9.2\niris_version=1.11.4+26.2\n",
                encoding="utf-8",
            )
            fmod = repo / "src/main/resources/fabric.mod.json"
            fmod.parent.mkdir(parents=True, exist_ok=True)
            fmod.write_text(json.dumps({
                "schemaVersion": 1, "id": "wildercord", "version": "${version}",
                "depends": {"fabricloader": ">=0.19.5", "minecraft": "~26.2", "java": ">=25", "fabric-api": ">=0.161.0+26.2"},
            }), encoding="utf-8")
            mjson = repo / "src/main/resources/wildercord.mixins.json"
            mjson.write_text(json.dumps({"required": True, "package": "dev.wildercord.mixin", "compatibilityLevel": "JAVA_25", "mixins": []}), encoding="utf-8")
            lock = repo / "profiles/dependencies.lock.json"
            lock.parent.mkdir(parents=True, exist_ok=True)
            lock.write_text(json.dumps({
                "minecraft": "26.2", "fabric_loader": "0.19.5",
                "dependencies": [
                    {"project": "fabric-api", "version": "0.161.0+26.2", "game_versions": ["26.2"]},
                    {"project": "sodium", "version": "mc26.2-0.9.2", "game_versions": ["26.2"]},
                    {"project": "iris", "version": "1.11.4+26.2", "game_versions": ["26.2"]},
                ],
            }), encoding="utf-8")
            self.assertEqual(verify_port.verify_descriptor_alignment(repo), [])

            mjson.write_text(json.dumps({"required": True, "package": "dev.wildercord.mixin", "compatibilityLevel": "JAVA_21", "mixins": []}), encoding="utf-8")
            errs = verify_port.verify_descriptor_alignment(repo)
            self.assertTrue(any("compatibilityLevel" in e for e in errs), errs)

            compat_file = repo / "src/main/java/dev/wildercord/compat/McEntities.java"
            compat_file.parent.mkdir(parents=True, exist_ok=True)
            compat_file.write_text(
                "package dev.wildercord.compat;\npublic final class McEntities { private static int leakedCache; }\n",
                encoding="utf-8",
            )
            cerrs = verify_port.verify_compat_contract(repo)
            self.assertTrue(any("mutable non-final static field" in e for e in cerrs), cerrs)
            self.assertTrue(any("Missing required compat seam file" in e for e in cerrs), cerrs)


if __name__ == "__main__":
    unittest.main()
