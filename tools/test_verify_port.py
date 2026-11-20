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
            "public class Helper { public void ping(int v) {} }\n"
        ),
        "net/minecraft/world/Target.java": (
            "package net.minecraft.world;\n"
            "public class Target {\n"
            "    private int secretValue;\n"
            "    public void oneCall(Helper h) { h.ping(1); }\n"
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


if __name__ == "__main__":
    unittest.main()
