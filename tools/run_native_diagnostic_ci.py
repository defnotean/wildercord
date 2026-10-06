"""One explicit PR-head diagnostic request; never full or focused release acceptance.

An active request must be the sole change in a single-parent commit immediately
following sourceSha. Later source commits do not launch it. An explicit GitHub
rerun of that same request is allowed and records its new run attempt.
No request-supplied classes, paths, commands, environment, seeds or retry counts.
"""
import argparse
from datetime import datetime, timezone
import hashlib
from itertools import islice
import json
import os
from pathlib import Path
import re
import subprocess
import sys

from client_suites import (ROOT, EXIT_PREFIX, REQUEST_PREFIX, PROGRESSION_ENTRIES,
                           progression_completion, reweave_player_completion, REWEAVE_PLAYER_ENTRIES,
                           stone_owner_negative_completion, STONE_OWNER_NEGATIVE_ENTRIES,
                           movement_foundations_completion, MOVEMENT_FOUNDATION_ENTRIES, STONE_VELOCITY_ENTRY, REED_REFUGE_ENTRY,
                           excise_completion, EXCISE_ENTRIES, life_excise_completion, LIFE_EXCISE_ENTRIES,
                           stone_march_completion, STONE_MARCH_ENTRIES, select_entries, selection_issues)
import run_client_ci
from native_ci_diagnostics import SCENE_PREFIX

REQUEST = ".github/native-diagnostic-request.json"
OUTPUT = "artifacts/review/native-diagnostic"
LOG = "artifacts/review/native-diagnostic-raw.log"
MAX_REQUEST_BYTES = 512
MAX_LOG_BYTES = 16 * 1024 * 1024
MAX_SNAPSHOTS = 16
MAX_SNAPSHOT_BYTES = 256 * 1024
SNAPSHOTS = "build/run/clientGameTest/logs/ci-diagnostics"
CASES = {"wetland": "diagnostic-wetland", "aura-fx": "diagnostic-aura-fx",
         "battlefields-generation": "diagnostic-battlefields-generation",
         "siltcrest-presentation": "diagnostic-siltcrest-presentation",
         "fungal-nursery": "diagnostic-fungal-nursery",
         "wall-turn": "diagnostic-wall-turn",
         "kiln-ring": "diagnostic-kiln-ring",
         "ecology-return": "diagnostic-ecology-return",
         "stasis-gallery": "diagnostic-stasis-gallery",
         "progression-feasibility": "diagnostic-progression-feasibility",
         "reweave-player": "diagnostic-reweave-player",
         "stone-hinge-owner-negative": "diagnostic-stone-hinge-owner-negative",
         "movement-foundations": "diagnostic-movement-foundations", "excise": "diagnostic-life-excise",
         "stone-fault-march": "stone-fault-march"}
FIXED_ENV = {"LIBGL_ALWAYS_SOFTWARE": "1", "SDL_VIDEO_FORCE_EGL": "1", "ALSOFT_DRIVERS": "null"}
DISALLOWED_ENV = ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "GRADLE_OPTS", "JAVA_OPTS")
SEED_PREFIX = "WILDERCORD_NATIVE_WORLD "
SEEDS = {
    "stone-fault-march": dict.fromkeys(STONE_MARCH_ENTRIES),
    "excise": {LIFE_EXCISE_ENTRIES[1]: None, LIFE_EXCISE_ENTRIES[2]: None,
               EXCISE_ENTRIES[0] + "#lesson": None, EXCISE_ENTRIES[0]: None},
    "movement-foundations": {STONE_VELOCITY_ENTRY: None, REED_REFUGE_ENTRY: None,
                             REED_REFUGE_ENTRY + "#reopen": None},
    "stone-hinge-owner-negative": {STONE_OWNER_NEGATIVE_ENTRIES[0] + "#" + name: None for name in
                                   ("ordinary-native-hit-control", "server-step-plus-motion-negative")},
    "reweave-player": {"dev.wildercord.cast.ReweavePlayableTest#lesson": None,
                       "dev.wildercord.cast.ReweavePlayableTest#input": None},
    "progression-feasibility": dict.fromkeys(PROGRESSION_ENTRIES),
    "stasis-gallery": {"dev.wildercord.gametest.WildercordScreenshots": None},
    "ecology-return": {"dev.wildercord.wildlife.RootmoltCounterTest": None,
                       "dev.wildercord.wildlife.ReedRefugeTest": None,
                       "dev.wildercord.wildlife.ReedRefugeTest#reopen": None,
                       "dev.wildercord.wildlife.SiltcrestBankReturnTest": None},
    "wetland": {"dev.wildercord.wildlife.WetlandPlacementContractTest": None,
                "dev.wildercord.wildlife.WetlandGardenTest": None,
                "dev.wildercord.wildlife.WetlandTerrainAbsenceTest": "-7620530482425397421",
                "dev.wildercord.wildlife.WetlandTerrainTest": "-7620530482425397421"},
    "aura-fx": {"dev.wildercord.gametest.WildercordAuraFxTest": None},
    "battlefields-generation": {"dev.wildercord.world.dungeons.BattlefieldsTest": "4424506075848880372"},
    # Separate disposable worlds: observe each seed without changing the fixture RNG.
    "fungal-nursery": {"dev.wildercord.wildlife.FungalNurseryTest": None},
    "siltcrest-presentation": {"dev.wildercord.wildlife.SiltcrestPresentationTest#full": None,
                              "dev.wildercord.wildlife.SiltcrestPresentationTest#minimal": None},
    "wall-turn": {"dev.wildercord.aura.WallTurnLessonTest": None,
                  "dev.wildercord.aura.WallTurnSafetyTest": None,
                  "dev.wildercord.aura.WallTurnCommitmentTest": None,
                  "dev.wildercord.aura.WallTurnCommitmentTest#relay": None},
    "kiln-ring": {"dev.wildercord.aura.world.EmberKilnTest": None,
                  "dev.wildercord.aura.world.EmberKilnPresentationTest": None,
                  "dev.wildercord.aura.world.EmberKilnOpponentViewTest": None},
}
CONFIG_FILES = (REQUEST, ".github/workflows/build.yml", "tools/client_suite_catalog.json",
                "src/gametest/resources/fabric.mod.json", "build.gradle", "gradle.properties",
                "gradle/wrapper/gradle-wrapper.properties", "tools/run_native_diagnostic_ci.py",
                "tools/run_client_ci.py", "tools/client_suites.py", "tools/native_ci_diagnostics.py",
                "src/gametest/java/dev/wildercord/gametest/NativeHaltHandshake.java",
                "src/gametest/java/dev/wildercord/gametest/NativeSingleplayerClose.java",
                "src/gametest/java/dev/wildercord/gametest/mixin/NativeClientPhaseAccess.java",
                "src/gametest/java/dev/wildercord/gametest/mixin/NativeIntegratedServerHaltMixin.java",
                "src/gametest/java/dev/wildercord/gametest/mixin/NativeSingleplayerCloseScopeMixin.java",
                "src/gametest/resources/native-singleplayer-close-gametest.mixins.json")


# Include every new proof helper and exact test-only instrumentation used by this case.
CASE_FILES = {"stone-hinge-owner-negative": (
    "src/gametest/java/dev/wildercord/gametest/stonehinge/StoneHingeOwnerProbe.java",
    "src/gametest/java/dev/wildercord/gametest/stonehinge/StoneHingeImpulseProbe.java",
    *("src/gametest/java/dev/wildercord/gametest/stonehinge/mixin/" + name + ".java" for name in (
        "StoneHingePlayerProbeMixin", "StoneHingeMobProbeMixin", "StoneHingeKnockbackProbeMixin",
        "StoneHingeMasterProbeMixin", "StoneHingeAuraSourceProbeMixin", "StoneHingeOwnerClientProbeMixin",
        "StoneHingeOwnerSendProbeMixin", "StoneHingeOwnerServerProbeMixin", "StoneHingeOwnerPositionSendProbeMixin")),
    "src/gametest/resources/stone-hinge-proof-gametest.mixins.json",
    "src/gametest/resources/native-diagnostics-gametest.mixins.json",
    "src/gametest/java/dev/wildercord/gametest/mixin/NativeSceneTraceMixin.java",
), "ecology-return": (
    "src/main/java/dev/wildercord/wildlife/LanternNewt.java",
    "src/main/java/dev/wildercord/wildlife/NewtPathNavigation.java",
), "reweave-player": (
    "src/main/java/dev/wildercord/cast/ReweaveFields.java",
    "src/main/java/dev/wildercord/cast/ReweaveState.java",
    "src/main/java/dev/wildercord/net/ReweaveInput.java",
    "src/main/java/dev/wildercord/mixin/ReweavePlayerModeMixin.java",
    "src/main/resources/wildercord.mixins.json",
    "src/main/java/dev/wildercord/content/ReweaveLesson.java",
    "src/main/java/dev/wildercord/player/MasterStudies.java",
    "src/main/java/dev/wildercord/spell/MasterStudyRules.java",
    "src/main/java/dev/wildercord/spell/ReweaveRules.java",
    "src/client/java/dev/wildercord/client/ReweaveClient.java",
    "src/client/java/dev/wildercord/client/ReweaveLessonScreen.java",
    "src/gametest/java/dev/wildercord/content/ReweaveLessonChecks.java",
    "tools/generate_reweave_lesson.py",
), "progression-feasibility": (
    "src/main/java/dev/wildercord/cast/ReweaveFields.java",
    "src/main/java/dev/wildercord/spell/ReweaveRules.java",
    "src/test/java/dev/wildercord/spell/ReweaveRulesTest.java",
    "src/gametest/java/dev/wildercord/aura/world/StoneHingeMasterReleaseChecks.java",
    "src/gametest/java/dev/wildercord/gametest/stonehinge/StoneHingeImpulseProbe.java",
    *("src/gametest/java/dev/wildercord/gametest/stonehinge/mixin/" + name + ".java" for name in (
        "StoneHingePlayerProbeMixin", "StoneHingeMobProbeMixin", "StoneHingeKnockbackProbeMixin",
        "StoneHingeMasterProbeMixin", "StoneHingeAuraSourceProbeMixin")),
    "src/gametest/resources/stone-hinge-proof-gametest.mixins.json",
    "src/gametest/resources/native-diagnostics-gametest.mixins.json",
    "src/gametest/java/dev/wildercord/gametest/mixin/NativeSceneTraceMixin.java",
    "tools/check_stone_hinge_native_contract.py",
)}

CASE_FILES["stone-fault-march"] = (
    'src/main/java/dev/wildercord/aura/world/StoneMarch.java',
    'src/main/java/dev/wildercord/aura/world/StoneMarchRules.java',
    'src/main/java/dev/wildercord/aura/world/SwordMaster.java',
    'src/main/java/dev/wildercord/aura/world/MasterMoveCatalog.java',
    'src/main/java/dev/wildercord/aura/world/MasterAnimationRules.java',
    'src/main/java/dev/wildercord/aura/ArticulatedCombatPose.java',
    'src/gametest/java/dev/wildercord/aura/world/StoneMarchFixture.java',
    'src/gametest/java/dev/wildercord/gametest/MastersNpcCaptureProbe.java',
    'src/gametest/java/dev/wildercord/gametest/mixin/NativeSceneTraceMixin.java',
    'src/gametest/resources/native-diagnostics-gametest.mixins.json',
)

CASE_FILES["movement-foundations"] = (*CASE_FILES["stone-hinge-owner-negative"],
    "src/main/java/dev/wildercord/wildlife/LanternNewt.java",
    "src/main/java/dev/wildercord/wildlife/NewtPathNavigation.java",
    "src/gametest/java/dev/wildercord/wildlife/EcologyReturnProbe.java",
    "src/gametest/java/dev/wildercord/wildlife/EcologyReturnProbeChecks.java",
    "src/gametest/java/dev/wildercord/gametest/stonehinge/StoneHingeVelocityExperiment.java",
)

# Fixed authored slice paths, bound to the exact source commit; no request-provided paths.
CASE_FILES["excise"] = (
    'src/gametest/java/dev/wildercord/gametest/mixin/ExciseCutProbeMixin.java',
    'src/gametest/resources/excise-cut-gametest.mixins.json',
    'src/client/java/dev/wildercord/client/CordScreen.java',
    'src/client/java/dev/wildercord/client/ExciseClient.java',
    'src/client/java/dev/wildercord/client/ExciseLessonScreen.java',
    'src/client/java/dev/wildercord/client/WildercordClient.java',
    'src/client/java/dev/wildercord/client/WildercordKeys.java',
    'src/gametest/java/dev/wildercord/content/ExciseLessonChecks.java',
    'src/main/java/dev/wildercord/Wildercord.java',
    'src/main/java/dev/wildercord/aura/Aura.java',
    'src/main/java/dev/wildercord/aura/AuraDominion.java',
    'src/main/java/dev/wildercord/aura/AuraGuard.java',
    'src/main/java/dev/wildercord/aura/AuraSlash.java',
    'src/main/java/dev/wildercord/aura/AuraStep.java',
    'src/main/java/dev/wildercord/aura/Awakening.java',
    'src/main/java/dev/wildercord/aura/MasterForms.java',
    'src/main/java/dev/wildercord/aura/MastersArts.java',
    'src/main/java/dev/wildercord/aura/SwordStrings.java',
    'src/main/java/dev/wildercord/aura/Unity.java',
    'src/main/java/dev/wildercord/cast/CastEngine.java',
    'src/main/java/dev/wildercord/cast/CastLock.java',
    'src/main/java/dev/wildercord/cast/Charging.java',
    'src/main/java/dev/wildercord/cast/ExciseCasting.java',
    'src/main/java/dev/wildercord/cast/ExciseState.java',
    'src/main/java/dev/wildercord/cast/Imbuing.java',
    'src/main/java/dev/wildercord/cast/Mastery.java',
    'src/main/java/dev/wildercord/cast/NativeZoneEmitters.java',
    'src/main/java/dev/wildercord/cast/PassiveCaster.java',
    'src/main/java/dev/wildercord/cast/RelayCircles.java',
    'src/main/java/dev/wildercord/cast/ReweaveFields.java',
    'src/main/java/dev/wildercord/cast/SpellCaster.java',
    'src/main/java/dev/wildercord/cast/SpellChat.java',
    'src/main/java/dev/wildercord/cast/feel/LifeFeels.java',
    'src/main/java/dev/wildercord/content/ExciseLesson.java',
    'src/main/java/dev/wildercord/content/Imbued.java',
    'src/main/java/dev/wildercord/content/RuneItem.java',
    'src/main/java/dev/wildercord/content/ScrollSpell.java',
    'src/main/java/dev/wildercord/content/SpellScrollItem.java',
    'src/main/java/dev/wildercord/mixin/PlayerAuraMixin.java',
    'src/main/java/dev/wildercord/mixin/ReweavePlayerModeMixin.java',
    'src/main/java/dev/wildercord/net/ExciseCores.java',
    'src/main/java/dev/wildercord/net/ExciseInput.java',
    'src/main/java/dev/wildercord/net/WildercordNetworking.java',
    'src/main/java/dev/wildercord/player/Heart.java',
    'src/main/java/dev/wildercord/player/MasterStudies.java',
    'src/main/java/dev/wildercord/player/Spellbook.java',
    'src/main/java/dev/wildercord/player/Spellbooks.java',
    'src/main/java/dev/wildercord/spell/ExciseRules.java',
    'src/main/java/dev/wildercord/spell/Fusions.java',
    'src/main/java/dev/wildercord/spell/Knots.java',
    'src/main/java/dev/wildercord/spell/MasterStudyRules.java',
    'src/main/java/dev/wildercord/spell/RuneSources.java',
    'src/main/java/dev/wildercord/spell/Runes.java',
    'src/main/java/dev/wildercord/spell/SpellCodes.java',
    'src/main/java/dev/wildercord/spell/SpellCompiler.java',
    'src/main/java/dev/wildercord/spell/SpellNames.java',
    'src/main/resources/assets/wildercord/animations/rune_choreography.txt',
    'src/main/resources/assets/wildercord/items/excise_lesson.json',
    'src/main/resources/assets/wildercord/items/rune.json',
    'src/main/resources/assets/wildercord/lang/en_us.json',
    'src/main/resources/assets/wildercord/models/item/excise_lesson.json',
    'src/main/resources/assets/wildercord/models/item/rune/excise.json',
    'src/main/resources/assets/wildercord/textures/item/excise_lesson.png',
    'src/main/resources/assets/wildercord/textures/item/rune/excise.png',
    'src/main/resources/assets/wildercord/textures/item/rune/excise.png.mcmeta',
    'src/main/resources/assets/wildercord/textures/particle/circle/excise_band.png',
    'src/main/resources/assets/wildercord/textures/particle/circle/excise_mark.png',
    'src/test/java/dev/wildercord/cast/feel/FeelTest.java',
    'src/test/java/dev/wildercord/spell/EveryRuneCompilationTest.java',
    'src/test/java/dev/wildercord/spell/ExciseRulesTest.java',
    'src/test/java/dev/wildercord/spell/RuneChoreographyTest.java',
)

CASE_FILES["excise"] += (
    "src/main/java/dev/wildercord/aura/RuneEtchingRules.java",
    "src/main/java/dev/wildercord/aura/RuneEtchings.java",
    "src/main/java/dev/wildercord/cast/Inscriptions.java",
    "src/gametest/java/dev/wildercord/client/fx/LifeRuntimePartitionChecks.java",
    "src/gametest/java/dev/wildercord/aura/RuneEtchingsTest.java",
    "src/gametest/java/dev/wildercord/cast/RunicAnimationGalleryTest.java",
    "src/gametest/java/dev/wildercord/gametest/WildercordMagicReleaseTest.java",
    "src/gametest/java/dev/wildercord/gametest/WildercordScreenshots.java",
)


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("Duplicate request field")
        result[key] = value
    return result


def parse_request(raw):
    if len(raw) > MAX_REQUEST_BYTES:
        raise ValueError("Diagnostic request exceeds 512 bytes")
    data = json.loads(raw, object_pairs_hook=unique_object)
    if not isinstance(data, dict) or set(data) != {"schemaVersion", "case", "sourceSha"}:
        raise ValueError("Request requires exactly schemaVersion, case and sourceSha")
    if type(data["schemaVersion"]) is not int or data["schemaVersion"] != 1:
        raise ValueError("Unsupported diagnostic request schema")
    case, sha = data["case"], data["sourceSha"]
    if case is None:
        if sha is not None:
            raise ValueError("Disabled request requires null sourceSha")
    elif not isinstance(case, str) or case not in CASES:
        raise ValueError("Diagnostic case must be null or one of: " + ", ".join(CASES))
    elif not isinstance(sha, str) or not re.fullmatch(r"[0-9a-f]{40}", sha):
        raise ValueError("Active request requires a full lowercase sourceSha")
    return data


def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True).strip()


def digest(path):
    result = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(65536), b""):
            result.update(block)
    return result.hexdigest()


def live_head(number):
    reference = "refs/pull/" + number + "/head"
    remote = subprocess.check_output(["git", "ls-remote", "--exit-code", "origin", reference],
                                     cwd=ROOT, text=True, timeout=30,
                                     env={**os.environ, "GIT_TERMINAL_PROMPT": "0"}).split()
    if len(remote) != 2 or remote[1] != reference or not re.fullmatch(r"[0-9a-f]{40}", remote[0]):
        raise ValueError("Cannot verify the current public PR head")
    return remote[0]


def identity(env, *, observe_live=True):
    sha = git("rev-parse", "HEAD")
    head = env.get("NATIVE_DIAGNOSTIC_HEAD_SHA", "")
    if not re.fullmatch(r"[0-9a-f]{40}", head) or sha != head:
        raise ValueError("Checkout must match the exact requested PR head SHA")
    if env.get("GITHUB_EVENT_NAME") != "pull_request" or env.get("GITHUB_JOB") != "native-diagnostic":
        raise ValueError("Diagnostic execution requires the PR native-diagnostic job")
    if git("status", "--porcelain", "--untracked-files=normal"):
        raise ValueError("Diagnostic checkout must have no tracked or untracked source changes")
    run, attempt, number = (env.get(key, "") for key in
                            ("GITHUB_RUN_ID", "GITHUB_RUN_ATTEMPT", "NATIVE_DIAGNOSTIC_PR"))
    if not all(re.fullmatch(r"[1-9][0-9]*", value) for value in (run, attempt, number)):
        raise ValueError("Missing run, attempt or PR provenance")
    # A GitHub rerun retains the old event SHA. Observe the live public PR ref so
    # an old request cannot silently rerun after an unrelated source commit.
    return {"checkoutSha": sha, "headSha": head, "workflowSha": env.get("GITHUB_SHA"),
            "observedLiveHeadSha": live_head(number) if observe_live else None,
            "repository": env.get("GITHUB_REPOSITORY"), "pullRequest": number,
            "runId": run, "runAttempt": attempt, "job": "native-diagnostic"}


def request_state(request, provenance, parents, changed):
    if request["case"] is None:
        return "disabled"
    if provenance.get("observedLiveHeadSha") != provenance.get("headSha"):
        return "inactive-stale-pr-head"
    if parents != [request["sourceSha"]] or changed != [REQUEST]:
        return "inactive-stale-request"
    return "ready"


def check_environment(env):
    if any(key.startswith("WILDERCORD_") for key in env) or any(env.get(key) for key in DISALLOWED_ENV):
        raise ValueError("Diagnostic job refuses test-skip selectors and external JVM/Gradle options")
    if any(env.get(key) != value for key, value in FIXED_ENV.items()):
        raise ValueError("Diagnostic job requires its fixed graphics/audio environment")


def current(env, *, observed_head=None):
    path = ROOT / REQUEST
    if path.is_symlink() or not path.is_file():
        raise ValueError("Diagnostic request must be a committed regular file")
    with path.open("rb") as request_file:
        request = parse_request(request_file.read(MAX_REQUEST_BYTES + 1))
    provenance = identity(env, observe_live=request["case"] is not None and observed_head is None)
    if observed_head is not None:
        provenance["observedLiveHeadSha"] = observed_head
    parents = git("rev-list", "--parents", "-n", "1", "HEAD").split()[1:]
    changed = git("diff-tree", "--no-commit-id", "--name-only", "-r", "HEAD").splitlines()
    state = request_state(request, provenance, parents, changed)
    selection = select_entries(suite=CASES[request["case"]]) if request["case"] else None
    if selection is not None and selection["kind"] != "diagnostic":
        raise ValueError("Allowlisted case must remain explicitly diagnostic in the catalog")
    if request["case"] == "progression-feasibility" and selection != {
            "kind": "diagnostic", "name": CASES["progression-feasibility"],
            "count": 2, "entries": list(PROGRESSION_ENTRIES)}:
        raise ValueError("Progression feasibility requires exactly both complete allowlisted classes in order")
    if request["case"] == "reweave-player" and selection != {
            "kind": "diagnostic", "name": CASES["reweave-player"],
            "count": 1, "entries": list(REWEAVE_PLAYER_ENTRIES)}:
        raise ValueError("Reweave player diagnostic must retain exactly its whole registered class")
    if request["case"] == "stone-hinge-owner-negative" and selection != {
            "kind": "diagnostic", "name": CASES["stone-hinge-owner-negative"],
            "count": 1, "entries": list(STONE_OWNER_NEGATIVE_ENTRIES)}:
        raise ValueError("Stone owner negative control must retain its whole registered class")
    if request["case"] == "movement-foundations" and selection != {
            "kind": "diagnostic", "name": CASES["movement-foundations"],
            "count": 2, "entries": list(MOVEMENT_FOUNDATION_ENTRIES)}:
        raise ValueError("Movement foundations requires its exact two complete classes in order")
    if request["case"] == "excise" and selection != {
            "kind": "diagnostic", "name": CASES["excise"], "count": 5, "entries": list(LIFE_EXCISE_ENTRIES)}:
        raise ValueError("Life and Excise diagnostic must retain all five whole registered classes")
    paths = [*CONFIG_FILES, *CASE_FILES.get(request["case"], ())]
    if selection:
        paths += ["src/gametest/java/" + entry.replace(".", "/") + ".java" for entry in selection["entries"]]
    if request["case"] == "wall-turn":
        paths.append("src/gametest/java/dev/wildercord/aura/WallRelayChecks.java")
    return {"schemaVersion": 1, "scope": "diagnostic", "fullClientGate": "unverified",
            "focusedClientGate": "unverified", "request": request, "state": state,
            "provenance": provenance, "selection": selection,
            "sourceFilesSha256": {path: digest(ROOT / path) for path in paths},
            "configuration": {"environment": FIXED_ENV, "nativeStepTimeoutMinutes": 50,
                              "jobTimeoutMinutes": 60, "automaticRetries": 0,
                              "seedPolicy": "Original fixtures and Fabric settings, no overrides",
                              "declaredWorldSeeds": SEEDS.get(request["case"], {})},
            "basis": "Diagnostic evidence only. All four aggregate and both focused release gates remain required."}


def write_json(name, value):
    path = ROOT / OUTPUT / name
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def prepare(env):
    data = current(env)
    write_json("request-provenance.json", data)
    if env.get("GITHUB_OUTPUT"):
        with open(env["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
            output.write("enabled=" + str(data["state"] == "ready").lower() + "\n")
    print("Native diagnostic request: " + data["state"] + "; no release acceptance claimed")
    return 0


def prepared(env, *, for_collection=False):
    previous = json.loads((ROOT / OUTPUT / "request-provenance.json").read_text())
    # Collection must survive cancellation or remote-read failure after launch.
    # Only reporting may reuse the recorded observation; launching always rechecks.
    data = current(env, observed_head=previous["provenance"]["observedLiveHeadSha"] if for_collection else None)
    if data != previous or data["state"] != "ready":
        raise ValueError("Prepared diagnostic request/provenance is stale or inactive")
    return data


def run(env):
    data = prepared(env)
    check_environment(env)
    marker = ROOT / OUTPUT / "launch.json"
    # Exclusive creation prevents an accidental second invocation in the same job.
    with marker.open("x", encoding="utf-8") as output:
        json.dump({"startedAt": datetime.now(timezone.utc).isoformat(),
                   "provenance": data["provenance"], "command": run_client_ci.launch_command(data["selection"])}, output)
    return run_client_ci.main(["--suite", CASES[data["request"]["case"]], "--log", str(ROOT / LOG)],
                              diagnostic_provenance=receipt(data))


def receipt(data):
    return {key: data[key] for key in ("request", "provenance", "sourceFilesSha256", "configuration")}


def observed_seeds(log, case):
    found, issues = {}, []
    for line in log.splitlines():
        if SEED_PREFIX not in line:
            continue
        try:
            marker = json.loads(line.split(SEED_PREFIX, 1)[1],
                                object_pairs_hook=unique_object if case in ("progression-feasibility", "reweave-player", "stone-hinge-owner-negative", "movement-foundations", "excise") else dict)
            if case in ("progression-feasibility", "reweave-player", "stone-hinge-owner-negative", "movement-foundations", "excise") and (not isinstance(marker, dict) or set(marker) != {"suite", "seed"}):
                raise ValueError()
            entry, seed = marker["suite"], marker["seed"]
            if entry not in SEEDS[case] or not isinstance(seed, str) or not re.fullmatch(r"-?[0-9]{1,19}", seed):
                raise ValueError()
            if case in ("wall-turn", "kiln-ring", "ecology-return", "stasis-gallery", "progression-feasibility", "reweave-player", "stone-hinge-owner-negative", "movement-foundations", "excise", "stone-fault-march") and entry in found:
                issues.append("Repeated native world seed marker for " + entry)
            if case in ("progression-feasibility", "reweave-player", "stone-hinge-owner-negative", "movement-foundations", "excise") and not -(2 ** 63) <= int(seed) < 2 ** 63:
                raise ValueError()
            found.setdefault(entry, set()).add(seed)
        except (ValueError, KeyError, TypeError):
            issues.append("Invalid native world seed marker")
    for entry, expected in SEEDS[case].items():
        values = found.get(entry, set())
        if len(values) != 1 or expected is not None and values != {expected}:
            issues.append("Missing or inconsistent observed world seed for " + entry)
    return {entry: sorted(values) for entry, values in found.items()}, issues


def preserve_snapshots():
    """Keep the two existing observers' bounded text files, never arbitrary paths."""
    source = ROOT / SNAPSHOTS
    result = {"files": [], "omitted": [], "maxFiles": MAX_SNAPSHOTS,
              "maxBytesPerFile": MAX_SNAPSHOT_BYTES}
    if not source.is_dir() or any((ROOT / Path(*Path(SNAPSHOTS).parts[:end])).is_symlink()
                                  for end in range(1, len(Path(SNAPSHOTS).parts) + 1)):
        return result
    # Both observers have fixed names and two known thresholds. Reject symlinks.
    for directory in sorted(islice(source.iterdir(), 64)):
        if directory.is_symlink() or not directory.is_dir() or not re.fullmatch(r"(?:run-|jvm-)[A-Za-z0-9_-]+", directory.name):
            continue
        for path in sorted(islice(directory.iterdir(), 64)):
            if path.is_symlink() or not path.is_file() or not re.fullmatch(r"(?:after-(?:1800|2700)s-pid-[0-9]+|snapshot-(?:1800|2700))\.txt", path.name):
                continue
            relative = path.relative_to(source).as_posix()
            if len(result["files"]) >= MAX_SNAPSHOTS:
                if len(result["omitted"]) < MAX_SNAPSHOTS:
                    result["omitted"].append({"path": relative, "reason": "file-count limit"})
                continue
            size = path.stat().st_size
            with path.open("rb") as stream:
                raw = stream.read(MAX_SNAPSHOT_BYTES)
            destination = ROOT / OUTPUT / "snapshots" / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_bytes(raw)
            result["files"].append({"path": "snapshots/" + relative, "bytes": size,
                                    "retainedBytes": len(raw), "truncated": size > len(raw),
                                    "sha256": digest(path)})
    return result


def collect(env):
    data = prepared(env, for_collection=True)
    path = ROOT / LOG
    issues, log = [], ""
    if path.is_file() and not path.is_symlink():
        size = path.stat().st_size
        # Preserve a bounded tail on oversized logs, but never accept truncated evidence.
        with path.open("rb") as source:
            source.seek(max(0, size - MAX_LOG_BYTES))
            raw = source.read(MAX_LOG_BYTES)
        (ROOT / OUTPUT / "native.log").write_bytes(raw)
        data["log"] = {"bytes": size, "sha256": digest(path), "retainedBytes": len(raw),
                       "truncated": size > MAX_LOG_BYTES}
        log = raw.decode("utf-8-sig", errors="replace")
        if size > MAX_LOG_BYTES:
            issues.append("Native log exceeded bounded evidence size; retained tail cannot establish success")
    else:
        issues.append("Native log missing; native execution unverified")
    if not (ROOT / OUTPUT / "launch.json").is_file():
        issues.append("Native launch receipt missing")
    issues.extend(selection_issues(log, data["selection"]))
    markers = [line[len(REQUEST_PREFIX):] for line in log.splitlines() if line.startswith(REQUEST_PREFIX)]
    try:
        if len(markers) != 1 or json.loads(markers[0]) != receipt(data):
            issues.append("Native log request/head/run/config provenance does not match this invocation")
    except ValueError:
        issues.append("Native log request provenance is invalid")
    seeds, seed_issues = observed_seeds(log, data["request"]["case"])
    issues.extend(seed_issues)
    if data["request"]["case"] == "progression-feasibility":
        data["completedEntries"], _ = progression_completion(log)
    if data["request"]["case"] == "reweave-player":
        data["completedEntries"], _ = reweave_player_completion(log)
    if data["request"]["case"] == "stone-hinge-owner-negative":
        data["completedEntries"], _ = stone_owner_negative_completion(log)
        data.update(movementGate="NOT_PROVEN", gameplayEnabled=False)
    if data["request"]["case"] == "movement-foundations":
        data["completedEntries"], _ = movement_foundations_completion(log)
        data.update(movementGate="NOT_PROVEN", gameplayEnabled=False, peerGate="NOT_PROVEN",
                    latencyGate="NOT_PROVEN", admissionGate="NOT_PROVEN")
    if data["request"]["case"] == "excise":
        data["completedEntries"], _ = life_excise_completion(log)
    if data["request"]["case"] == "stone-fault-march":
        data["completedEntries"], _ = stone_march_completion(log)
    successful = "BUILD SUCCESSFUL" in log and "BUILD FAILED" not in log and not issues
    data.update(diagnosticOutcome="passed" if successful else "unverified",
                observedWorldSeeds=seeds, verificationIssues=issues,
                snapshots=preserve_snapshots(),
                launcherExits=[line[len(EXIT_PREFIX):] for line in log.splitlines() if line.startswith(EXIT_PREFIX)],
                collectedAt=datetime.now(timezone.utc).isoformat())
    try:
        head = live_head(data["provenance"]["pullRequest"])
        data["headAtCollection"] = {"sha": head, "status": "unchanged" if head == data["provenance"]["headSha"] else "superseded"}
    except (ValueError, OSError, KeyError, subprocess.SubprocessError):
        data["headAtCollection"] = {"status": "unavailable", "basis": "Evidence remains bound to the recorded launch revision"}
    write_json("diagnostic-result.json", data)
    print("Diagnostic outcome: " + data["diagnosticOutcome"] + "; full and focused release gates remain unverified")
    return 0 if successful else 1


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    modes = parser.add_mutually_exclusive_group(required=True)
    for name in ("prepare", "run", "collect"):
        modes.add_argument("--" + name, action="store_true")
    args = parser.parse_args(argv)
    os.chdir(ROOT)
    try:
        return (prepare if args.prepare else run if args.run else collect)(os.environ)
    except (ValueError, OSError, KeyError, subprocess.SubprocessError) as exc:
        parser.exit(2, "Native diagnostic refused: " + str(exc) + "\n")


if __name__ == "__main__":
    sys.exit(main())
