"""Stdlib checks for client game test impact selection over a fake class graph.

Run with: python -m unittest discover -s tools -p 'test_affected_tests.py' -v
These tests do not run jdeps, Gradle or Minecraft.
"""
import struct
import unittest

import affected_tests as at

TESTS = ["t.AlphaTest", "t.BetaTest", "t.GammaTest"]
GRAPH = {
    "t.AlphaTest": ["m.Rules", "t.Fixture"],
    "t.BetaTest": ["c.Renderer"],
    "t.GammaTest": ["t.GammaTest$1"],
    "t.GammaTest$1": ["m.Leaf"],
    "t.Fixture": ["m.Deep"],
    "c.Renderer": ["m.Rules"],
    "m.Init": ["m.Handler", "c.Renderer"],
    "m.Mixin": ["m.Hooked"],
}
ROOTS = ["m.Init", "m.Mixin"]


def java(*classes, path="src/main/java/x.java"):
    return {"path": path, "kind": "java", "reason": "Java source", "classes": list(classes)}


def select(*changes, **kw):
    kw.setdefault("runtime_roots", ROOTS)
    kw.setdefault("always_all", ROOTS)
    return at.select_tests(TESTS, GRAPH, list(changes), **kw)


class ClassifyTests(unittest.TestCase):
    def test_java_sources_in_client_sets(self):
        for s in ("main", "client", "gametest"):
            self.assertEqual(at.classify_path(f"src/{s}/java/dev/A.java")[0], "java")

    def test_docs_wiki_tools_select_nothing(self):
        for p in ("docs/testing/x.md", "wiki/Home.md", "tools/affected_tests.py", "README.md",
                  ".github/workflows/build.yml", "src/test/java/dev/UnitTest.java", "gitbook/faq.md"):
            self.assertEqual(at.classify_path(p)[0], "none", p)

    def test_resources_and_build_inputs_select_all(self):
        for p in ("src/main/resources/assets/wildercord/textures/item/x.png",
                  "src/main/resources/assets/wildercord/models/item/x.json",
                  "src/main/resources/assets/wildercord/lang/en_us.json",
                  "src/main/resources/data/wildercord/recipe/x.json",
                  "src/gametest/resources/fabric.mod.json", "src/main/resources/wildercord.mixins.json",
                  "src/main/resources/wildercord.accesswidener", "build.gradle", "gradle.properties",
                  "settings.gradle", "gradle/wrapper/gradle-wrapper.properties", "profiles/x.json",
                  "src/main/java/dev/notes.txt"):
            kind, reason = at.classify_path(p)
            self.assertEqual(kind, "all", p)
            self.assertTrue(reason, p)

    def test_windows_separators(self):
        self.assertEqual(at.classify_path("src\\client\\java\\dev\\A.java")[0], "java")


class SelectionTests(unittest.TestCase):
    def test_direct_and_transitive_dependents_in_descriptor_order(self):
        result = select(java("m.Rules"))
        self.assertEqual(result["mode"], "selected")
        self.assertEqual(result["selected"], ["t.AlphaTest", "t.BetaTest"])
        self.assertEqual(result["explanations"]["t.BetaTest"], ["t.BetaTest", "c.Renderer", "m.Rules"])
        self.assertEqual(result["explanations"]["t.AlphaTest"], ["t.AlphaTest", "m.Rules"])

    def test_gametest_helper_selects_only_its_users(self):
        result = select(java("t.Fixture"))
        self.assertEqual(result["selected"], ["t.AlphaTest"])

    def test_nested_and_anonymous_classes_count(self):
        self.assertEqual(select(java("t.GammaTest", "t.GammaTest$1"))["selected"], ["t.GammaTest"])
        self.assertEqual(select(java("m.Leaf"))["explanations"]["t.GammaTest"],
                         ["t.GammaTest", "t.GammaTest$1", "m.Leaf"])

    def test_changed_test_explains_itself(self):
        self.assertEqual(select(java("t.BetaTest"))["explanations"]["t.BetaTest"], ["t.BetaTest"])

    def test_resource_change_selects_all_even_with_java(self):
        result = select(java("t.Fixture"), {"path": "src/main/resources/assets/a.png", "kind": "all",
                                             "reason": "resources"})
        self.assertEqual(result["mode"], "all")
        self.assertEqual(result["selected"], TESTS)
        self.assertIn("src/main/resources/assets/a.png", result["reasons"][0])

    def test_docs_only_selects_none(self):
        result = select({"path": "wiki/Home.md", "kind": "none", "reason": "wiki"})
        self.assertEqual((result["mode"], result["selected"]), ("none", []))

    def test_mixin_or_entrypoint_change_selects_all(self):
        self.assertEqual(select(java("m.Mixin"))["mode"], "all")
        self.assertEqual(select(java("m.Init"))["mode"], "all")

    def test_runtime_only_class_selects_all(self):
        result = select(java("m.Handler"))
        self.assertEqual(result["mode"], "all")
        self.assertIn("runtime hooks", result["reasons"][0])

    def test_runtime_reachable_class_warns_or_selects_all_when_strict(self):
        normal = select(java("c.Renderer"))
        self.assertEqual(normal["selected"], ["t.BetaTest"])
        self.assertTrue(any("m.Init -> c.Renderer" in w for w in normal["warnings"]))
        self.assertEqual(select(java("c.Renderer"), strict=True)["mode"], "all")

    def test_dead_class_selects_none_with_warning(self):
        result = select(java("m.Orphan"))
        self.assertEqual(result["mode"], "none")
        self.assertTrue(any("m.Orphan" in w for w in result["warnings"]))

    def test_uncompiled_java_file_warns(self):
        result = select(java(path="src/main/java/Gone.java"))
        self.assertEqual(result["mode"], "none")
        self.assertTrue(any("Gone.java" in w for w in result["warnings"]))

    def test_cycles_terminate(self):
        graph = {"t.AlphaTest": ["a"], "a": ["b"], "b": ["a", "c"]}
        result = at.select_tests(["t.AlphaTest"], graph, [java("c")])
        self.assertEqual(result["explanations"]["t.AlphaTest"], ["t.AlphaTest", "a", "b", "c"])


class JdepsParseTests(unittest.TestCase):
    def test_keeps_project_edges_only(self):
        text = "\n".join([
            "client -> java.base",
            "client -> build\\classes\\java\\main",
            "   dev.A                                -> dev.B                       main",
            "   dev.A                                -> java.lang.Object            java.base",
            "   dev.A$Inner -> dev.C not found",
            "   dev.VeryLongClassNameThatOverflowsTheColumn -> dev.B",
            "   dev.B                                -> dev.B                       client",
        ])
        known = {"dev.A", "dev.B", "dev.A$Inner", "dev.VeryLongClassNameThatOverflowsTheColumn"}
        self.assertEqual(at.parse_jdeps(text, known), {
            "dev.A": ["dev.B"], "dev.VeryLongClassNameThatOverflowsTheColumn": ["dev.B"]})


class ClassFileTests(unittest.TestCase):
    @staticmethod
    def utf8(text):
        raw = text.encode()
        return b"\x01" + struct.pack(">H", len(raw)) + raw

    def test_reads_name_and_source_file_across_wide_constants(self):
        pool = [self.utf8("dev/x/Outer$1"), b"\x07" + struct.pack(">H", 1),
                b"\x05" + struct.pack(">q", 7),  # long takes slots 3 and 4
                self.utf8("SourceFile"), self.utf8("Outer.java")]
        method = struct.pack(">HHHH", 0, 0, 0, 1) + struct.pack(">HI", 5, 2) + b"\x00\x00"
        data = (b"\xca\xfe\xba\xbe" + struct.pack(">HHH", 0, 69, 7) + b"".join(pool)
                + struct.pack(">HHHH", 0x21, 2, 0, 0) + struct.pack(">H", 0)
                + struct.pack(">H", 1) + method
                + struct.pack(">H", 1) + struct.pack(">HIH", 5, 2, 6))
        self.assertEqual(at.class_info(data), ("dev.x.Outer$1", "Outer.java"))


if __name__ == "__main__":
    unittest.main()
