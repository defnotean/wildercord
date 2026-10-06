"""Bounded supervisor regression tests; no Minecraft process is started."""
import ast
import contextlib
import copy
import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import types
import unittest
from unittest.mock import patch
import uuid

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("supervisor", ROOT / "tools/native/launch_two_clients.py")
supervisor = importlib.util.module_from_spec(spec)
spec.loader.exec_module(supervisor)
CONTRACT = json.loads((ROOT / supervisor.CONTRACT).read_text())
ORIGINAL_CASES = [
    spell + "_" + case
    for spell in ("BREAK_CAST", "DRIVING_CUT")
    for case in ("HEALTH", "FULL_ABSORPTION", "MANA_SKIN", "REVERSAL", "TOTEM", "GUARD", "STEP", "WARD",
                 "RESISTANCE", "REJECTED", "REPLACED", "EQUAL_TOKEN", "NEW_CHARGE", "IDLE", "WINDUP_REPLACEMENT")
] + ["SHARED_BREAK_CAST_TO_DRIVING_CUT", "SHARED_DRIVING_CUT_TO_BREAK_CAST", "IDLE_SEAL_RECOVERY",
     "CHARGED_SEAL_RECOVERY", "NONPLAYER"]
COUNTER_CASES = ["COUNTER_QUIETUS_PAID_BOLT", "COUNTER_QUIETUS_THIRTEEN_EXISTING", "COUNTER_REFLECTED_RESPAWN_NULLCATCH"]
PRESERVED_CASES = ORIGINAL_CASES + COUNTER_CASES
SPECTATOR_CASES = ["SPECTATOR_BOLT_VENOM", "SPECTATOR_SPARK_VENOM", "SPECTATOR_RAY_VENOM", "SPECTATOR_TOUCH_VENOM"]
EXPECTED_CASES = PRESERVED_CASES + SPECTATOR_CASES


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data))


class Fixture(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name).resolve()
        (self.root / ".gitignore").write_text("build/\nbuild-alt/\n.gradle/\n")
        write_json(self.root / supervisor.CONTRACT, CONTRACT)
        (self.root / "source.txt").write_text("reviewed source")
        subprocess.run(["git", "init", "-q", str(self.root)], check=True)
        for args in (("add", "."), ("-c", "user.name=Test", "-c", "user.email=test@example.invalid", "commit", "-qm", "fixture")):
            subprocess.run(["git", "-C", str(self.root), *args], check=True)
        self.head = supervisor.source_head(self.root)
        self.descriptor = self.root / "build/resources/gametest/fabric.mod.json"
        write_json(self.descriptor, {"entrypoints": {"fabric-client-gametest": [supervisor.ENTRYPOINT]}})
        shutil.copyfile(self.root / supervisor.CONTRACT, self.descriptor.parent / Path(supervisor.CONTRACT).name)
        self.java = self.root / "build/toolchain/bin/java"
        self.java.parent.mkdir(parents=True)
        self.java.write_text("test input only; never executed")
        self.config = self.root / ".gradle/loom-cache/launch.cfg"
        self.config.parent.mkdir(parents=True)
        self.config.write_text("commonProperties\n\tfabric.development=true\nclientArgs\n")
        self.classes = self.root / "build/classes/java/gametest"
        self.classes.mkdir(parents=True)
        (self.classes / "Fixture.class").write_bytes(b"compiled fixture")
        self.launch = {
            "schemaVersion": 2, "suite": "cast-receipt", "sourceHead": self.head, "checkoutSha": self.head, "prHeadSha": "", "project": str(self.root),
            "descriptor": str(self.descriptor), "descriptorSha256": supervisor.digest(self.descriptor),
            "contract": supervisor.CONTRACT, "contractSha256": supervisor.digest(self.root / supervisor.CONTRACT),
            "java": str(self.java), "main": supervisor.MAIN,
            "classpath": [str(self.descriptor.parent), str(self.classes)],
            "jvm": ["-Dfabric.dli.env=client", "-Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotClient", "-Dfabric.dli.config=" + str(self.config), "-Xmx8G"],
            "args": ["--gameDir", str(self.root / "build/old-game")], "environment": {"ALSOFT_DRIVERS": "null"},
            "runtimeSha256": {str(p): supervisor.digest(p) for p in (self.java, self.descriptor.parent, self.classes, self.config)},
        }
        self.launch_path = self.root / "build/native/launch.json"
        write_json(self.launch_path, self.launch)
        java_patch = patch.object(supervisor, "configured_java", return_value=self.java)
        java_patch.start(); self.addCleanup(java_patch.stop)
        self.identity = {"nonce": "fresh-nonce", "suite": "cast-receipt", "sourceHead": self.head, "checkoutSha": self.head, "prHeadSha": "",
                         "descriptorSha256": self.launch["descriptorSha256"], "hostUuid": supervisor.PROFILES["host"][1],
                         "peerUuid": supervisor.PROFILES["peer"][1], "runIdentity": "123-2-fresh-nonce"}

    def validate(self):
        return supervisor.validate_launch(self.launch, self.launch_path, self.root)

    def update_config(self, text):
        self.config.write_text(text)
        self.launch["runtimeSha256"][str(self.config)] = supervisor.digest(self.config)

    def options(self):
        return types.SimpleNamespace(launch=self.launch_path, output=self.root / "build/native/run", suite="cast-receipt", profile="aura", timeout=1, accepted_eula=None)


class ContractTests(Fixture):
    def test_exact_42_case_roster_retains_all_38_and_offline_profiles(self):
        contract = supervisor.load_contract(self.root)
        cases = contract["cases"]
        self.assertEqual(len(cases), 42)
        self.assertEqual(cases, EXPECTED_CASES)
        self.assertEqual(cases[:35], ORIGINAL_CASES)
        self.assertEqual(cases[:38], PRESERVED_CASES)
        self.assertEqual(cases[35:38], COUNTER_CASES)
        self.assertEqual(cases[38:], SPECTATOR_CASES)
        self.assertEqual(contract["expectedCount"], 42)
        self.assertEqual(contract["limits"], {"maxJvms": 2, "maxHeapMiBPerJvm": 2048, "maxCases": 42, "maxTimeoutSeconds": 900})
        for name, expected in supervisor.PROFILES.values():
            raw = bytearray(hashlib.md5(("OfflinePlayer:" + name).encode()).digest())
            raw[6] = raw[6] & 15 | 48; raw[8] = raw[8] & 63 | 128
            self.assertEqual(str(uuid.UUID(bytes=bytes(raw))), expected)
        self.assertNotEqual(supervisor.PROFILES["host"][1], supervisor.PROFILES["peer"][1])

    def test_exporter_binds_the_same_ordered_roster(self):
        exporter = (ROOT / "tools/native/export_two_client_launch.init.gradle").read_text()
        roster = re.search(r"def expectedCases = (\[.*?\])", exporter, re.DOTALL)
        self.assertIsNotNone(roster)
        self.assertEqual(ast.literal_eval(roster[1]), EXPECTED_CASES)
        self.assertIn("contract.cases != expectedCases", exporter)

    def test_every_case_is_mandatory_and_cannot_be_substituted(self):
        for index, case in enumerate(EXPECTED_CASES):
            for replacement in ([], ["FOREIGN_CASE"], [EXPECTED_CASES[(index + 1) % len(EXPECTED_CASES)]]):
                value = copy.deepcopy(CONTRACT)
                value["cases"][index:index + 1] = replacement
                # A consistent count must not legitimize dropping any preserved or spectator case.
                value["expectedCount"] = len(value["cases"])
                value["limits"]["maxCases"] = len(value["cases"])
                write_json(self.root / supervisor.CONTRACT, value)
                with self.subTest(case=case, replacement=replacement), self.assertRaisesRegex(ValueError, "exact ordered 42-case roster"):
                    supervisor.load_contract(self.root)

    def test_original_counter_spectator_and_boundary_order_cannot_change(self):
        for first, second in ((0, 1), (34, 35), (35, 36), (36, 37), (37, 38), (38, 39), (40, 41)):
            value = copy.deepcopy(CONTRACT)
            value["cases"][first], value["cases"][second] = value["cases"][second], value["cases"][first]
            write_json(self.root / supervisor.CONTRACT, value)
            with self.subTest(first=first, second=second), self.assertRaisesRegex(ValueError, "exact ordered 42-case roster"):
                supervisor.load_contract(self.root)

    def test_legacy_count_invalid_rosters_and_relaxed_limits_are_rejected(self):
        mutations = [("expectedCount", 35), ("expectedCount", 38), ("cases", ORIGINAL_CASES), ("cases", PRESERVED_CASES), ("cases", None),
                     ("cases", [None] + EXPECTED_CASES[1:]), ("cases", [{}] + EXPECTED_CASES[1:])]
        for field, replacement in mutations:
            value = copy.deepcopy(CONTRACT)
            value[field] = replacement
            write_json(self.root / supervisor.CONTRACT, value)
            with self.subTest(field=field, replacement=replacement), self.assertRaisesRegex(ValueError, "exact ordered 42-case roster"):
                supervisor.load_contract(self.root)
        for field, replacement in (("maxJvms", 3), ("maxHeapMiBPerJvm", 4096), ("maxCases", 35), ("maxCases", 38), ("maxTimeoutSeconds", 901)):
            value = copy.deepcopy(CONTRACT)
            value["limits"][field] = replacement
            write_json(self.root / supervisor.CONTRACT, value)
            with self.subTest(limit=field), self.assertRaisesRegex(ValueError, "resource limits"):
                supervisor.load_contract(self.root)

    def test_contract_cannot_change_profiles_count_or_witness_paths(self):
        for field in ("peerProfile", "profiles", "cases", "terminalWitnesses"):
            value = copy.deepcopy(CONTRACT)
            if field == "peerProfile":
                value[field]["offlineUuid"] = "foreign"
            elif field == "profiles":
                value[field]["aura"]["username"] = "foreign"
            else:
                value[field].append("EXTRA")
            write_json(self.root / supervisor.CONTRACT, value)
            with self.subTest(field=field), self.assertRaises(ValueError):
                supervisor.load_contract(self.root)

    def test_cli_rejects_legacy_flags_arbitrary_commands_and_incomplete_moon_selection(self):
        base = ["build/launch.json", "--output", "build/out", "--suite", "cast-receipt", "--profile", "aura"]
        for extra in (["--require-unity"], ["--command", "sh"], ["--java", "sh"], ["--case", "anything"], ["--suite", "moon"]):
            with contextlib.redirect_stderr(io.StringIO()), self.subTest(extra=extra), self.assertRaises(SystemExit):
                supervisor.parse_args(base + extra)


class ProvenanceTests(Fixture):
    def test_clean_exact_export_is_accepted(self):
        self.assertEqual(len(self.validate()), 64)

    def test_dirty_tracked_and_untracked_source_are_rejected(self):
        (self.root / "source.txt").write_text("unreviewed")
        with self.assertRaisesRegex(ValueError, "clean checkout"):
            self.validate()
        (self.root / "source.txt").write_text("reviewed source")
        (self.root / "untracked.java").write_text("new source")
        with self.assertRaisesRegex(ValueError, "clean checkout"):
            self.validate()

    def test_checkout_and_pr_heads_are_distinct_and_bound(self):
        pr_head = "a" * 40
        self.launch["prHeadSha"] = pr_head
        with patch.dict(os.environ, {"WILDERCORD_PR_HEAD_SHA": pr_head}):
            self.validate()
        with patch.dict(os.environ, {"WILDERCORD_PR_HEAD_SHA": "b" * 40}):
            with self.assertRaisesRegex(ValueError, "explicit workflow metadata"):
                self.validate()
        self.launch["checkoutSha"] = pr_head
        with self.assertRaisesRegex(ValueError, "Checkout association"):
            self.validate()

    def test_missing_or_invalid_pr_association_is_rejected(self):
        for bad in (None, "main", "a" * 39, "A" * 40):
            self.launch["prHeadSha"] = bad
            with self.subTest(prHeadSha=bad), self.assertRaisesRegex(ValueError, "Malformed PR-head"):
                self.validate()
        self.launch["prHeadSha"] = ""
        with patch.dict(os.environ, {"WILDERCORD_PR_HEAD_SHA": ""}):
            self.validate()

    def test_wrong_head_suite_project_main_or_java_are_rejected(self):
        for key, bad in (("schemaVersion", 1), ("sourceHead", "0" * 40), ("suite", "moon"), ("project", str(self.root.parent)), ("main", "foreign.Main"), ("java", "/bin/sh")):
            old = self.launch[key]; self.launch[key] = bad
            with self.subTest(key=key), self.assertRaises(ValueError):
                self.validate()
            self.launch[key] = old

    def test_altered_descriptor_or_compiled_classes_are_rejected(self):
        original = self.descriptor.read_bytes()
        self.descriptor.write_bytes(original + b" ")
        with self.assertRaisesRegex(ValueError, "descriptor changed"):
            self.validate()
        self.descriptor.write_bytes(original)
        (self.classes / "Fixture.class").write_bytes(b"other runtime bytes")
        with self.assertRaisesRegex(ValueError, "Runtime input changed"):
            self.validate()

    def test_rehashed_descriptor_cannot_add_another_entrypoint(self):
        write_json(self.descriptor, {"entrypoints": {"fabric-client-gametest": [supervisor.ENTRYPOINT, "foreign.Main"]}})
        self.launch["descriptorSha256"] = supervisor.digest(self.descriptor)
        with self.assertRaisesRegex(ValueError, "exactly"):
            self.validate()

    def test_processed_contract_must_equal_source_contract(self):
        target = self.descriptor.parent / Path(supervisor.CONTRACT).name
        target.write_text("{}")
        with self.assertRaisesRegex(ValueError, "Processed contract"):
            self.validate()

    def test_contract_path_and_hash_remain_bound_to_source(self):
        for key, bad in (("contract", "build/alternate-contract.json"), ("contractSha256", "0" * 64)):
            original = self.launch[key]
            self.launch[key] = bad
            with self.subTest(key=key), self.assertRaisesRegex(ValueError, "Source contract changed"):
                self.validate()
            self.launch[key] = original

    def test_java_and_launch_config_bytes_remain_hash_bound(self):
        for path in (self.java, self.config):
            original = path.read_bytes()
            path.write_bytes(original + b"\n")
            with self.subTest(path=path.name), self.assertRaisesRegex(ValueError, "Runtime input changed"):
                self.validate()
            path.write_bytes(original)

    def test_jvm_and_environment_injection_is_rejected(self):
        for argument in ("-javaagent:/tmp/agent.jar", "@/tmp/args", "-jar", "-Djava.system.class.loader=foreign.Loader", "-Dwildercord.mp.role=peer", "-Dfabric.addMods=/tmp/foreign.jar"):
            self.launch["jvm"].append(argument)
            with self.subTest(argument=argument), self.assertRaises(ValueError):
                self.validate()
            self.launch["jvm"].pop()
        self.launch["environment"] = {"JAVA_TOOL_OPTIONS": "foreign"}
        with self.assertRaisesRegex(ValueError, "environment"):
            self.validate()

    def test_hidden_loom_config_cannot_override_role_game_dir_or_connect_target(self):
        for config in ("commonProperties\n\twildercord.mp.role=peer\n", "clientArgs\n\t--server\n\tremote.invalid\n", "clientArgs\n\t--gameDir\n\t" + str(self.root / "build/other") + "\n", "clientProperties\n\tjava.library.path=/tmp/foreign\n"):
            self.update_config(config)
            with self.subTest(config=config), self.assertRaises(ValueError):
                self.validate()

    def test_native_paths_introduced_by_config_are_hash_bound(self):
        native = self.root / "build/natives"; native.mkdir()
        library = native / "library.so"; library.write_bytes(b"original")
        self.update_config("clientProperties\n\tjava.library.path=" + str(native) + "\n")
        with self.assertRaisesRegex(ValueError, "every runtime input"):
            self.validate()
        self.launch["runtimeSha256"][str(native)] = supervisor.digest(native)
        self.validate()
        library.write_bytes(b"changed")
        with self.assertRaisesRegex(ValueError, "Runtime input changed"):
            self.validate()

    def test_remap_classpath_contents_cannot_escape_allowed_roots(self):
        remap = self.root / "build/remap.txt"; remap.write_text("/tmp/foreign.jar")
        self.update_config("commonProperties\n\tfabric.remapClasspathFile=" + str(remap) + "\n")
        self.launch["runtimeSha256"][str(remap)] = supervisor.digest(remap)
        with self.assertRaisesRegex(ValueError, "escapes"):
            self.validate()

    def test_real_loom118_java25_flags_and_config_are_accepted(self):
        self.launch["jvm"] += ["-XX:StackShadowPages=32", "--sun-misc-unsafe-memory-access=allow", "--enable-native-access=ALL-UNNAMED"]
        jar = self.root / "build/common.jar"; jar.write_bytes(b"test game jar")
        self.update_config("commonProperties\n\tfabric.development=true\n\tfabric.defaultModDistributionNamespace=official\n\tfabric.defaultMixinRemapType=static\n\tfabric.gameJarPath=" + str(jar) + "\nclientArgs\n\t--assetIndex\n\t26.3\n\t--assetsDir\n\t" + str(self.root / "build/assets") + "\n")
        self.launch["runtimeSha256"][str(jar)] = supervisor.digest(jar)
        self.validate()

    def test_unknown_arguments_and_unbounded_dimensions_are_rejected(self):
        for args in (["--server", "remote.invalid"], ["--gameDir", "/tmp/foreign"], ["--gameDir"], ["--width", "9000"]):
            self.launch["args"] = args
            with self.subTest(args=args), self.assertRaises(ValueError):
                self.validate()

    def test_fingerprints_must_cover_every_input(self):
        del self.launch["runtimeSha256"][str(self.java)]
        with self.assertRaisesRegex(ValueError, "every runtime input"):
            self.validate()

    def test_output_paths_cannot_escape_or_dirty_source(self):
        with self.assertRaises(ValueError):
            supervisor.output_path(self.root / "src/output", self.root)
        target = self.root / "build/escape"; target.symlink_to(self.root.parent, target_is_directory=True)
        with self.assertRaises(ValueError):
            supervisor.output_path(target / "output", self.root)
        with self.assertRaisesRegex(ValueError, "Git-ignored"):
            supervisor.ignored_output(self.root / "artifacts/unignored", self.root)

    def test_exclusive_lock_and_lock_path_confinement(self):
        with supervisor.supervisor_lock(self.root):
            with self.assertRaisesRegex(RuntimeError, "Another"):
                with supervisor.supervisor_lock(self.root):
                    self.fail("Concurrent lock acquired")
        with supervisor.supervisor_lock(self.root):
            pass
        shutil.rmtree(self.root / ".gradle")
        (self.root / ".gradle").symlink_to(self.root.parent, target_is_directory=True)
        with self.assertRaisesRegex(ValueError, "escapes"):
            with supervisor.supervisor_lock(self.root):
                self.fail("Escaping lock acquired")

    def test_eula_requires_prior_file_or_both_explicit_ci_flags(self):
        with patch.dict(os.environ, {}, clear=True):
            with self.assertRaises(ValueError):
                supervisor.accepted_eula(None, self.root)
            os.environ["CI_MINECRAFT_EULA_ACCEPTED"] = "true"
            with self.assertRaises(ValueError):
                supervisor.accepted_eula(None, self.root)
            os.environ["GITHUB_ACTIONS"] = "true"
            self.assertIn(b"eula=true", supervisor.accepted_eula(None, self.root)[0])
        eula = self.root / "build/eula.txt"; eula.write_text("eula=false")
        with self.assertRaises(ValueError):
            supervisor.accepted_eula(eula, self.root)
        eula.write_text("# Previously accepted\neula=true\n")
        self.assertEqual(supervisor.accepted_eula(eula, self.root)[0], eula.read_bytes())

    def test_owned_command_replaces_heap_profiles_and_game_directory(self):
        for role, (name, native_uuid) in supervisor.PROFILES.items():
            game = self.root / "build/run" / role
            args = supervisor.command(self.launch, role, game, game.parent / "ipc", self.identity, 120)
            self.assertEqual([arg for arg in args if arg.startswith("-Xmx")], ["-Xmx2G"])
            for flag, expected in (("--username", name), ("--uuid", native_uuid), ("--gameDir", str(game)), ("--accessToken", "0")):
                self.assertEqual(args[args.index(flag) + 1], expected)
            self.assertIn("-Dwildercord.mp.timeoutSeconds=120", args)
            self.assertIn("-Djava.io.tmpdir=" + str(game / "tmp"), args)

    def test_ambient_java_injection_is_removed_and_caches_are_owned(self):
        game = self.root / "build/run/host"
        with patch.dict(os.environ, {"JAVA_TOOL_OPTIONS": "bad", "JDK_JAVA_OPTIONS": "bad", "WILDERCORD_CASE": "bad", "LD_PRELOAD": "bad", "DISPLAY": ":99"}):
            env = supervisor.environment(self.launch, game)
        for key in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "WILDERCORD_CASE", "LD_PRELOAD"):
            self.assertNotIn(key, env)
        self.assertEqual(env["DISPLAY"], ":99")
        self.assertEqual(env["TMPDIR"], str(game / "tmp"))
        self.assertEqual(env["XDG_CACHE_HOME"], str(game / "cache"))


class WitnessTests(Fixture):
    def setUp(self):
        super().setUp()
        self.ipc = self.root / "build/ipc"; self.ipc.mkdir()
        self.jobs = [("host", types.SimpleNamespace(pid=1001)), ("peer", types.SimpleNamespace(pid=1002))]
        for filename, role in supervisor.TERMINALS.items():
            values = {**self.identity, "role": role, "pid": "1001" if role == "host" else "1002", "cases": ",".join(CONTRACT["cases"])}
            (self.ipc / filename).write_text("# Java properties\n" + "\n".join(key + "=" + value for key, value in values.items()) + "\n")

    def validate_witnesses(self):
        return supervisor.validate_witnesses(self.ipc, self.identity, CONTRACT["cases"], self.jobs)

    def test_all_three_exact_witnesses_are_required(self):
        self.assertEqual(len(self.validate_witnesses()), 3)
        (self.ipc / "peer-disconnected.properties").unlink()
        with self.assertRaisesRegex(ValueError, "Missing"):
            self.validate_witnesses()

    def test_wrong_identity_role_foreign_pid_partial_cases_and_duplicates_fail(self):
        target = self.ipc / "peer-cast-receipt-passed.properties"
        original = target.read_text()
        for key in (*self.identity, "role", "pid", "cases"):
            target.write_text("\n".join(key + "=wrong" if line.startswith(key + "=") else line for line in original.splitlines()))
            with self.subTest(key=key), self.assertRaisesRegex(ValueError, "mismatched " + key):
                self.validate_witnesses()
        target.write_text(original + "nonce=fresh-nonce\n")
        with self.assertRaisesRegex(ValueError, "duplicate"):
            self.validate_witnesses()

    def test_every_terminal_rejects_legacy_missing_replaced_or_reordered_added_cases(self):
        bad_rosters = [ORIGINAL_CASES, PRESERVED_CASES, EXPECTED_CASES + ["EXTRA_CASE"],
                       ORIGINAL_CASES + list(reversed(COUNTER_CASES)) + SPECTATOR_CASES,
                       PRESERVED_CASES + list(reversed(SPECTATOR_CASES))]
        for index in range(35, 42):
            bad_rosters.append(EXPECTED_CASES[:index] + EXPECTED_CASES[index + 1:])
            bad_rosters.append(EXPECTED_CASES[:index] + ["FOREIGN_CASE"] + EXPECTED_CASES[index + 1:])
        for filename in supervisor.TERMINALS:
            target = self.ipc / filename
            original = target.read_text()
            for cases in bad_rosters:
                target.write_text("\n".join("cases=" + ",".join(cases) if line.startswith("cases=") else line for line in original.splitlines()))
                with self.subTest(filename=filename, cases=cases), self.assertRaisesRegex(ValueError, "mismatched cases"):
                    self.validate_witnesses()
            target.write_text(original)
        self.assertEqual(len(self.validate_witnesses()), 3)

    def test_identical_pids_and_symlink_witnesses_fail(self):
        self.jobs[1][1].pid = 1001
        with self.assertRaisesRegex(ValueError, "distinct"):
            self.validate_witnesses()
        self.jobs[1][1].pid = 1002
        target = self.ipc / "peer-disconnected.properties"
        target.rename(self.ipc / "saved.properties")
        target.symlink_to(self.ipc / "saved.properties")
        with self.assertRaisesRegex(ValueError, "symlink"):
            self.validate_witnesses()


class FakeProcess:
    def __init__(self, pid, code=None, ignore_terminate=False):
        self.pid, self.returncode = pid, code
        self.ignore_terminate = ignore_terminate
        self.terminated = self.killed = False

    def poll(self):
        return self.returncode

    def terminate(self):
        self.terminated = True
        if not self.ignore_terminate:
            self.returncode = -15

    def kill(self):
        self.killed = True; self.returncode = -9

    def wait(self, timeout):
        if self.returncode is None:
            raise subprocess.TimeoutExpired("owned client", timeout)
        return self.returncode


class LifecycleTests(Fixture):
    def fake_run(self, processes, publish=False, failure=None):
        options = self.options()
        launched = []
        def spawn(argv, **kwargs):
            self.assertFalse(kwargs["shell"])
            self.assertEqual(argv[0], str(self.java)); self.assertEqual(len(argv), 2)
            self.assertEqual(kwargs["stdin"], subprocess.DEVNULL)
            kwargs["stdout"].write(b"owned diagnostic\n")
            if failure is not None and len(launched) == 1:
                raise failure
            process = processes[len(launched)]; launched.append(process)
            if len(launched) == 2 and publish:
                proof = json.loads((options.output / "launch-proof.json").read_text())
                identity = {key: proof[key] for key in supervisor.IDENTITY}
                for filename, role in supervisor.TERMINALS.items():
                    values = {**identity, "role": role, "pid": str(processes[0 if role == "host" else 1].pid), "cases": ",".join(CONTRACT["cases"])}
                    (options.output / "ipc" / filename).write_text("\n".join(key + "=" + value for key, value in values.items()))
            return process
        with patch.dict(os.environ, {"GITHUB_ACTIONS": "true", "CI_MINECRAFT_EULA_ACCEPTED": "true", "GITHUB_RUN_ID": "123", "GITHUB_RUN_ATTEMPT": "2"}), patch.object(supervisor, "ignored_output"), patch.object(supervisor, "source_head", return_value=self.head), patch.object(supervisor, "validate_launch", return_value="a" * 64), patch.object(supervisor.subprocess, "Popen", side_effect=spawn):
            return supervisor.run(options, self.root)

    def test_complete_pair_preserves_exact_ci_identity_and_both_logs(self):
        processes = [FakeProcess(1101, 0), FakeProcess(1102, 0)]
        report = self.fake_run(processes, publish=True)
        self.assertEqual(report["status"], "passed")
        self.assertEqual(report["cases"], EXPECTED_CASES)
        self.assertTrue(report["runIdentity"].startswith("123-2-"))
        self.assertEqual(len(report["processes"]), 2)
        for role in ("host", "peer"):
            self.assertEqual((self.options().output / (role + ".log")).read_text(), "owned diagnostic\n")
        self.assertFalse(any(process.terminated for process in processes))

    def test_missing_witness_preserves_failure_result_and_logs(self):
        with self.assertRaises(ValueError):
            self.fake_run([FakeProcess(1101, 0), FakeProcess(1102, 0)])
        report = json.loads((self.options().output / "result.json").read_text())
        self.assertEqual(report["status"], "failed")
        self.assertIn("witness", report["error"])
        self.assertTrue((self.options().output / "host.log").is_file())

    def test_preflight_failure_preserves_result_without_starting_java(self):
        with patch.dict(os.environ, {"GITHUB_ACTIONS": "false", "CI_MINECRAFT_EULA_ACCEPTED": "false"}):
            with self.assertRaisesRegex(ValueError, "EULA"):
                supervisor.run(self.options(), self.root)
        report = json.loads((self.options().output / "result.json").read_text())
        self.assertEqual(report["stage"], "preflight")
        self.assertEqual(report["processes"], {})
        self.assertEqual(report["status"], "failed")

    def test_legacy_roster_fails_preflight_before_starting_java(self):
        for roster in (ORIGINAL_CASES, PRESERVED_CASES):
            with self.subTest(legacy_count=len(roster)):
                value = copy.deepcopy(CONTRACT)
                value["cases"] = roster
                value["expectedCount"] = value["limits"]["maxCases"] = len(roster)
                write_json(self.root / supervisor.CONTRACT, value)
                options = self.options()
                options.output = self.root / "build/native" / ("legacy-" + str(len(roster)))
                with patch.object(supervisor, "ignored_output"), patch.object(supervisor.subprocess, "Popen") as spawn, self.assertRaisesRegex(ValueError, "exact ordered 42-case roster"):
                    supervisor.run(options, self.root)
                spawn.assert_not_called()
                report = json.loads((options.output / "result.json").read_text())
                self.assertEqual(report["stage"], "preflight")
                self.assertEqual(report["processes"], {})
                self.assertEqual(report["status"], "failed")

    def test_peer_start_failure_stops_only_the_started_owned_host(self):
        host, unrelated = FakeProcess(1101), FakeProcess(9999)
        with self.assertRaises(OSError):
            self.fake_run([host, unrelated], failure=OSError("peer start failed"))
        self.assertTrue(host.terminated)
        self.assertFalse(unrelated.terminated)
        report = json.loads((self.options().output / "result.json").read_text())
        self.assertEqual(set(report["processes"]), {"host"})

    def test_nonzero_exit_stops_other_owned_child(self):
        host, peer = FakeProcess(1101, 3), FakeProcess(1102)
        with self.assertRaisesRegex(ValueError, "process failed"):
            self.fake_run([host, peer])
        self.assertFalse(host.terminated)
        self.assertTrue(peer.terminated)

    def test_deadline_kills_resistant_owned_process_and_retains_failure(self):
        host, peer, unrelated = FakeProcess(1101, ignore_terminate=True), FakeProcess(1102), FakeProcess(9999)
        with patch.object(supervisor.time, "sleep"), patch.object(supervisor.time, "monotonic", side_effect=[0, 0, 0, 2, 3]):
            with self.assertRaises(TimeoutError):
                self.fake_run([host, peer])
        self.assertTrue(host.terminated and host.killed)
        self.assertTrue(peer.terminated)
        self.assertFalse(unrelated.terminated or unrelated.killed)
        report = json.loads((self.options().output / "result.json").read_text())
        self.assertIn("TimeoutError", report["error"])

    def test_late_zero_exit_is_not_accepted_after_the_deadline(self):
        with patch.object(supervisor.time, "monotonic", side_effect=[0, 0, 0, 2, 3]):
            with self.assertRaises(TimeoutError):
                self.fake_run([FakeProcess(1101, 0), FakeProcess(1102, 0)], publish=True)
        report = json.loads((self.options().output / "result.json").read_text())
        self.assertEqual(report["status"], "failed")

    def test_expired_preflight_budget_never_starts_a_client(self):
        with patch.object(supervisor.time, "monotonic", side_effect=[0, 2, 3]):
            with self.assertRaises(TimeoutError):
                self.fake_run([])
        report = json.loads((self.options().output / "result.json").read_text())
        self.assertEqual(report["processes"], {})

    def test_invalid_deadlines_or_reused_output_fail_before_spawning(self):
        options = self.options()
        for timeout in (0, -1, 901):
            options.timeout = timeout
            with patch.object(supervisor.subprocess, "Popen") as spawn, self.assertRaises(ValueError):
                supervisor.run(options, self.root)
            spawn.assert_not_called()
        options.timeout = 10
        options.output.mkdir(parents=True)
        with patch.object(supervisor.subprocess, "Popen") as spawn, self.assertRaisesRegex(ValueError, "fresh"):
            supervisor.run(options, self.root)
        spawn.assert_not_called()


if __name__ == "__main__":
    unittest.main()
