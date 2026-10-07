"""One explicit PR-head diagnostic request; never full or focused release acceptance.

An active request must be the sole change in a single-parent commit immediately
following sourceSha. Later source commits do not launch it. An explicit GitHub
rerun of that same request is allowed and records its new run attempt.
No request-supplied classes, paths, commands, environment, seeds or retry counts.
"""
import argparse
from contextlib import contextmanager
from datetime import datetime, timezone
import hashlib
from itertools import islice
import json
import os
from pathlib import Path
import re
import stat
import subprocess
import sys

from client_suites import (ROOT, EXIT_PREFIX, REQUEST_PREFIX, PROGRESSION_ENTRIES,
                           progression_completion, reweave_player_completion, REWEAVE_PLAYER_ENTRIES,
                           stone_owner_negative_completion, STONE_OWNER_NEGATIVE_ENTRIES,
                           gale_ballistic_completion, GALE_BALLISTIC_ENTRY, GALE_BALLISTIC_SUITE,
                           movement_foundations_completion, MOVEMENT_FOUNDATION_ENTRIES, STONE_VELOCITY_ENTRY, REED_REFUGE_ENTRY,
                           excise_completion, EXCISE_ENTRIES, life_excise_completion, LIFE_EXCISE_ENTRIES,
                           stone_march_completion, STONE_MARCH_ENTRIES,
                           stone_march_visual_completion, STONE_MARCH_VISUAL_ENTRIES, STONE_MARCH_DIAGNOSTICS,
                           _whole_class_completion, stone_march_seeds, whole_class_seeds,
                           exact_json_marker, select_entries, selection_issues)
import run_client_ci
from native_ci_diagnostics import SCENE_PREFIX
from native import run_stone_hinge_peer as stone_peer

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
         "gale-vault-ballistic": GALE_BALLISTIC_SUITE,
         "movement-foundations": "diagnostic-movement-foundations", "excise": "diagnostic-life-excise",
         "stone-fault-march": "stone-fault-march",
         "stone-fault-march-visuals": "stone-fault-march-visuals",
         "stone-hinge-peer": "stone-hinge-peer",
         **{name: name for name in STONE_MARCH_DIAGNOSTICS}}
PEER_CASE = "stone-hinge-peer"
# Disposable game worlds/caches never live beneath the upload directory.
PEER_OUTPUT = "build/native/stone-hinge-peer"
PEER_EVIDENCE = "stone-hinge-peer"
FIXED_ENV = {"LIBGL_ALWAYS_SOFTWARE": "1", "SDL_VIDEO_FORCE_EGL": "1", "ALSOFT_DRIVERS": "null"}
DISALLOWED_ENV = ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "GRADLE_OPTS", "JAVA_OPTS")
SEED_PREFIX = "WILDERCORD_NATIVE_WORLD "
SEEDS = {
    "gale-vault-ballistic": {GALE_BALLISTIC_ENTRY: None},
    "stone-fault-march": dict.fromkeys(STONE_MARCH_ENTRIES),
    **{name: dict.fromkeys(entries) for name, entries in STONE_MARCH_DIAGNOSTICS.items()},
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

CASE_FILES["gale-vault-ballistic"] = (
    "src/gametest/java/dev/wildercord/gametest/galevault/GaleVaultProbe.java",
    "src/gametest/java/dev/wildercord/gametest/galevault/GaleVaultFallTrace.java",
    "src/gametest/java/dev/wildercord/gametest/galevault/GaleVaultFallTraceChecks.java",
    *("src/gametest/java/dev/wildercord/gametest/galevault/mixin/" + name + ".java" for name in (
        "GaleVaultLivingMixin", "GaleVaultMobMixin", "GaleVaultEntityMixin", "GaleVaultMasterMixin")),
    "src/gametest/resources/gale-vault-ballistic-gametest.mixins.json",
    "src/gametest/java/dev/wildercord/gametest/mixin/NativeSceneTraceMixin.java",
    "src/gametest/java/dev/wildercord/gametest/NativeJvmDiagnostics.java",
    "src/gametest/resources/native-diagnostics-gametest.mixins.json",
    "src/main/java/dev/wildercord/aura/world/SwordMaster.java",
    "src/main/java/dev/wildercord/aura/world/AuraWorld.java",
    "src/main/java/dev/wildercord/aura/world/MastersRules.java",
)

# Reuse the same gameplay/capture sources and bind the visual render instrumentation.
# The two selected whole-class sources are added by current(), as for other cases.
CASE_FILES["stone-fault-march-visuals"] = (*CASE_FILES["stone-fault-march"],
    'src/client/java/dev/wildercord/client/auraworld/MasterModel.java',
    'src/client/java/dev/wildercord/client/auraworld/MasterRenderer.java',
    'src/client/java/dev/wildercord/client/auraworld/AuraFighterRenderState.java',
    'src/client/java/dev/wildercord/client/combat/ArticulatedCombat.java',
    'src/client/java/dev/wildercord/client/combat/ArticulatedRig.java',
    'src/client/java/dev/wildercord/client/fx/HitStop.java',
    'src/client/java/dev/wildercord/client/fx/MagicQuality.java',
    'src/client/java/dev/wildercord/client/fx/ScreenEffects.java',
    'src/gametest/resources/masters-capture-gametest.mixins.json',
    *("src/gametest/java/dev/wildercord/gametest/mixin/" + name + ".java" for name in (
        "MastersHandRenderProbeMixin", "MastersBodyRenderProbeMixin", "MastersHudRenderProbeMixin",
        "MastersNpcRenderProbeMixin", "MastersNpcModelProbeMixin", "MastersNpcWarningProbeMixin")),
)

for march_case in STONE_MARCH_DIAGNOSTICS:
    CASE_FILES[march_case] = CASE_FILES["stone-fault-march-visuals"]

CASE_FILES["movement-foundations"] = (*CASE_FILES["stone-hinge-owner-negative"],
    "src/main/java/dev/wildercord/wildlife/LanternNewt.java",
    "src/main/java/dev/wildercord/wildlife/NewtPathNavigation.java",
    "src/gametest/java/dev/wildercord/wildlife/EcologyReturnProbe.java",
    "src/gametest/java/dev/wildercord/wildlife/EcologyReturnProbeChecks.java",
    "src/gametest/java/dev/wildercord/gametest/stonehinge/StoneHingeVelocityExperiment.java",
)

# Existing owner/master instrumentation now references these peer helpers even
# when the paired entrypoint is not selected. Record the complete dependency
# closure for each affected diagnostic without changing its selection.
STONE_SHARED_FILES = (
    "src/gametest/java/dev/wildercord/gametest/stonehinge/StoneHingeOwnerProbe.java",
    "src/gametest/java/dev/wildercord/gametest/stonehinge/StoneHingeVelocityExperiment.java",
    "src/gametest/java/dev/wildercord/gametest/stonehinge/peer/StoneHingePeerProbe.java",
    "src/gametest/java/dev/wildercord/gametest/stonehinge/peer/StoneHingeNaturalRelease.java",
    "src/gametest/java/dev/wildercord/gametest/stonehinge/peer/StoneHingeNaturalMotion.java",
    "src/gametest/java/dev/wildercord/gametest/stonehinge/peer/StoneHingeNativeDispatch.java",
    *("src/gametest/java/dev/wildercord/gametest/stonehinge/mixin/" + name + ".java" for name in (
        "StoneHingeOwnerClientProbeMixin", "StoneHingeOwnerSendProbeMixin",
        "StoneHingeOwnerServerProbeMixin", "StoneHingeOwnerPositionSendProbeMixin")),
)
for _case in ("stone-hinge-owner-negative", "movement-foundations", "progression-feasibility"):
    CASE_FILES[_case] = tuple(dict.fromkeys((*CASE_FILES[_case], *STONE_SHARED_FILES)))
CASE_FILES[PEER_CASE] = (
    *CASE_FILES["stone-hinge-owner-negative"],
    "src/gametest/java/dev/wildercord/aura/StoneHingePeerCases.java",
    "src/gametest/java/dev/wildercord/gametest/stonehinge/peer/StoneHingePeerFailure.java",
    *("src/gametest/java/dev/wildercord/gametest/stonehinge/mixin/" + name + ".java" for name in (
        "StoneHingePeerTrackerMixin", "StoneHingePeerSendMixin", "StoneHingePeerClientMixin",
        "StoneHingeNaturalStepMixin",
        "StoneHingeConnectionAccess", "StoneHingeServerConnectionAccess")),
    "src/gametest/resources/stone-hinge-peer-gametest.mixins.json",
    stone_peer.CONTRACT,
    "tools/native/run_stone_hinge_peer.py",
    "tools/native/stone_hinge_relay.py",
    "tools/native/launch_two_clients.py",
    "tools/native/run_paired_matrix.py",
    "tools/native/export_two_client_launch.init.gradle",
    "tools/check_stone_hinge_native_contract.py",
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
    selection = (peer_selection() if request["case"] == PEER_CASE else
                 select_entries(suite=CASES[request["case"]]) if request["case"] else None)
    if request["case"] == PEER_CASE:
        stone_peer.load_contract(ROOT)
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
    if request["case"] == "gale-vault-ballistic" and not exact_json_marker(json.dumps(selection), {
            "kind": "diagnostic", "name": GALE_BALLISTIC_SUITE,
            "count": 1, "entries": [GALE_BALLISTIC_ENTRY]}):
        raise ValueError("Gale Vault ballistic must retain exactly its whole registered diagnostic class")
    if request["case"] == "movement-foundations" and selection != {
            "kind": "diagnostic", "name": CASES["movement-foundations"],
            "count": 2, "entries": list(MOVEMENT_FOUNDATION_ENTRIES)}:
        raise ValueError("Movement foundations requires its exact two complete classes in order")
    if request["case"] == "excise" and selection != {
            "kind": "diagnostic", "name": CASES["excise"], "count": 5, "entries": list(LIFE_EXCISE_ENTRIES)}:
        raise ValueError("Life and Excise diagnostic must retain all five whole registered classes")
    if request["case"] in STONE_MARCH_DIAGNOSTICS and not exact_json_marker(json.dumps(selection), {
            "kind": "diagnostic", "name": request["case"],
            "count": len(STONE_MARCH_DIAGNOSTICS[request["case"]]),
            "entries": list(STONE_MARCH_DIAGNOSTICS[request["case"]])}):
        raise ValueError("Stone Fault March diagnostic must retain exactly its whole registered classes in order")
    paths = [*CONFIG_FILES, *CASE_FILES.get(request["case"], ())]
    if selection:
        paths += ["src/gametest/java/" + entry.replace(".", "/") + ".java" for entry in selection["entries"]]
    if request["case"] == "wall-turn":
        paths.append("src/gametest/java/dev/wildercord/aura/WallRelayChecks.java")
    return {"schemaVersion": 1, "scope": "diagnostic", "fullClientGate": "unverified",
            "focusedClientGate": "unverified", "request": request, "state": state,
            **({"ordinaryAttack": False, "physicsNativePending": True}
               if request["case"] == "gale-vault-ballistic" else {}),
            "provenance": provenance, "selection": selection,
            "sourceFilesSha256": {path: digest(ROOT / path) for path in paths},
            "configuration": {"environment": FIXED_ENV, "nativeStepTimeoutMinutes": 50,
                              "jobTimeoutMinutes": 60, "automaticRetries": 0,
                              "seedPolicy": "Original fixtures and Fabric settings, no overrides",
                              "declaredWorldSeeds": SEEDS.get(request["case"], {})},
            "basis": "Diagnostic evidence only. All four aggregate shards, all three required Masters parts, articulated and connected release gates remain required."}


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
    if data["request"]["case"] == PEER_CASE:
        return run_peer(data, env)
    marker = ROOT / OUTPUT / "launch.json"
    # Exclusive creation prevents an accidental second invocation in the same job.
    with marker.open("x", encoding="utf-8") as output:
        json.dump({"startedAt": datetime.now(timezone.utc).isoformat(),
                   "provenance": data["provenance"], "command": run_client_ci.launch_command(data["selection"])}, output)
    return run_client_ci.main(["--suite", CASES[data["request"]["case"]], "--log", str(ROOT / LOG)],
                              diagnostic_provenance=receipt(data))


def receipt(data):
    return {key: data[key] for key in ("request", "provenance", "sourceFilesSha256", "configuration")}


def peer_selection():
    """An explicit supervised case, never an exception to the single-client roster."""
    return {"kind": "diagnostic", "name": PEER_CASE, "count": 1,
            "entries": [stone_peer.ENTRYPOINT], "supervisor": "tools/native/run_stone_hinge_peer.py",
            "caseCount": 29, "profiles": list(stone_peer.PROFILES), "totalTimeoutSeconds": 1200,
            "maxConcurrentJvms": 2, "heapMiBPerJvm": 2048}


def peer_command():
    return ["xvfb-run", "-a", "-s", "-screen 0 1280x720x24 +extension GLX +render -noreset",
            sys.executable, "tools/native/run_stone_hinge_peer.py", "--output", PEER_OUTPUT,
            "--total-timeout", "1200"]


def peer_environment(data, env):
    check_environment(env)
    provenance = data["provenance"]
    if (data["selection"] != peer_selection() or env.get("GITHUB_ACTIONS") != "true"
            or env.get("NATIVE_DIAGNOSTIC_HEAD_SHA") != provenance["headSha"]):
        raise ValueError("Supervised diagnostic requires the exact fixed selection and validated CI head")
    # Do not rewrite GITHUB_SHA: GitHub's workflow merge and actual checkout are
    # distinct identities. No user/request supplied WILDERCORD_* value is allowed.
    return {**env, "WILDERCORD_PR_HEAD_SHA": provenance["headSha"]}


@contextmanager
def peer_validation_environment(env):
    """The shared exported-command validator reads the explicit PR association."""
    previous = os.environ.get("WILDERCORD_PR_HEAD_SHA")
    os.environ["WILDERCORD_PR_HEAD_SHA"] = env["WILDERCORD_PR_HEAD_SHA"]
    try:
        yield
    finally:
        if previous is None:
            os.environ.pop("WILDERCORD_PR_HEAD_SHA", None)
        else:
            os.environ["WILDERCORD_PR_HEAD_SHA"] = previous


def safe_evidence_path(root, relative):
    """Never resolve through a symlink, device, pipe, or caller-provided path."""
    relative = Path(relative)
    if relative.is_absolute() or ".." in relative.parts or not relative.parts:
        raise ValueError("Unsafe evidence path")
    path = root
    if root.is_symlink():
        raise ValueError("Linked evidence root")
    for part in relative.parts:
        path = path / part
        if path.is_symlink():
            raise ValueError("Linked evidence path: " + relative.as_posix())
    if path.exists() and not stat.S_ISREG(path.stat().st_mode):
        raise ValueError("Non-regular evidence path: " + relative.as_posix())
    return path


def run_peer(data, env):
    launch_env = peer_environment(data, env)
    source = stone_peer.provenance(ROOT, launch_env)
    marker = safe_evidence_path(ROOT, OUTPUT + "/launch.json")
    with marker.open("x", encoding="utf-8") as output:
        json.dump({"startedAt": datetime.now(timezone.utc).isoformat(), "requestReceipt": receipt(data),
                   "provenance": source, "command": peer_command()}, output)
    # A fresh supervisor output plus an exclusive launch receipt prevents replay
    # or a second native invocation in this job. The supervisor owns its deadline
    # and teardown; xvfb only supplies the existing virtual display.
    log_path = safe_evidence_path(ROOT, LOG)
    with log_path.open("x", encoding="utf-8") as log:
        log.write(REQUEST_PREFIX + json.dumps(receipt(data), sort_keys=True) + "\n")
        log.flush()
        process = subprocess.run(peer_command(), cwd=ROOT, env=launch_env, stdin=subprocess.DEVNULL,
                                 stdout=log, stderr=subprocess.STDOUT, check=False)
        log.write(EXIT_PREFIX + str(process.returncode) + "\n")
    return process.returncode


def peer_evidence_names(profile=None):
    if profile is None:
        return {"matrix-result.json", "matrix-result.partial.json", "stone-hinge-peer-launch.json",
                "stone-hinge-peer-descriptor.json", "export.log"}
    names = {"result.json", "result.partial.json", "launch-proof.json", "launch-proof.partial.json",
             "relay.json", "relay.partial.json", "host.log", "peer.log"}
    ipc = set(stone_peer.TERMINALS) | {"server-ready.properties", "relay-ready.properties",
           "host-connected.properties", "peer-connected.properties", "server-connected.properties",
           "host-failure.properties", "peer-failure.properties"}
    for index in range(len(stone_peer.roster(profile))):
        ipc.update(f"case-{index:02d}-{suffix}" for suffix in (
            "identity.json", "host.json", "peer.json", "arm.properties", "armed.properties",
            "ready.properties", "seen.properties", "passed.properties"))
    return names | {"ipc/" + name for name in ipc}


def preserve_peer_evidence():
    """Copy only fixed bounded text artifacts; never enumerate or upload work/."""
    result = {"files": [], "issues": [], "excluded": ["work/**"],
              "maxProfileBytes": stone_peer.MAX_EVIDENCE, "maxRootBytes": 24 * 1024 * 1024}
    groups = [("", peer_evidence_names(), result["maxRootBytes"]),
              *((profile + "/", peer_evidence_names(profile), stone_peer.MAX_EVIDENCE)
                for profile in stone_peer.PROFILES)]
    for prefix, names, remaining in groups:
        # Small failure reports are retained before potentially oversized logs.
        for name in sorted(names, key=lambda value: (value.endswith(".log"), value)):
            relative = prefix + name
            try:
                path = safe_evidence_path(ROOT, PEER_OUTPUT + "/" + relative)
                if not path.exists():
                    continue
                size = path.stat().st_size
                maximum = (MAX_LOG_BYTES if name.endswith(".log") else
                           MAX_SNAPSHOT_BYTES if name.endswith(".properties") else 2 * 1024 * 1024)
                with path.open("rb") as source:
                    raw = source.read(min(maximum, remaining) + 1)
                raw = raw[:min(maximum, remaining)]
                destination = safe_evidence_path(ROOT, OUTPUT + "/" + PEER_EVIDENCE + "/" + relative)
                destination.parent.mkdir(parents=True, exist_ok=True)
                destination.write_bytes(raw)
                remaining -= len(raw)
                truncated = size != len(raw)
                result["files"].append({"path": PEER_EVIDENCE + "/" + relative, "bytes": size,
                                        "retainedBytes": len(raw), "truncated": truncated,
                                        "retainedSha256": hashlib.sha256(raw).hexdigest()})
                if truncated:
                    result["issues"].append("Truncated supervised evidence: " + relative)
            except (ValueError, OSError) as error:
                result["issues"].append(str(error))
    return result


def verify_peer_retention(evidence):
    """A pass must be reproducible from the exact bounded files being uploaded."""
    required = peer_evidence_names() - {"matrix-result.partial.json"}
    for profile in stone_peer.PROFILES:
        required |= {profile + "/" + name for name in peer_evidence_names(profile)
                     if not name.endswith((".partial.json", "-failure.properties"))}
    files = {item["path"].removeprefix(PEER_EVIDENCE + "/"): item for item in evidence["files"]}
    if set(files) != required:
        raise ValueError("Retained evidence is incomplete or includes a failure/unfinished receipt")
    # Recollection must not upload stale files left by an earlier failed attempt.
    # Inspect only the seven fixed evidence directories, with bounded entry counts.
    directories = {}
    for relative in required:
        parts = Path(relative).parts
        for end in range(len(parts)):
            directories.setdefault("/".join(parts[:end]), set()).add(parts[end])
    for relative, expected in directories.items():
        path = ROOT / OUTPUT / PEER_EVIDENCE / relative
        if path.is_symlink():
            raise ValueError("Linked retained evidence directory")
        with os.scandir(path) as entries:
            actual = {entry.name for entry in islice(entries, len(expected) + 1)}
        if actual != expected:
            raise ValueError("Unexpected or missing retained evidence path: " + relative)
    for relative, item in files.items():
        for prefix in (PEER_OUTPUT, OUTPUT + "/" + PEER_EVIDENCE):
            path = safe_evidence_path(ROOT, prefix + "/" + relative)
            # Each retained file was already capped at 16 MiB (2 MiB for JSON,
            # 256 KiB for properties); never hash an unbounded changed source.
            with path.open("rb") as stream:
                raw = stream.read(item["retainedBytes"] + 1)
            if (len(raw) != item["bytes"] or item["truncated"]
                    or hashlib.sha256(raw).hexdigest() != item["retainedSha256"]):
                raise ValueError("Supervised evidence changed during retention: " + relative)


def validate_peer_result(data, env):
    """Recheck the exact export and all 29 native chains after owned cleanup."""
    launch_env = peer_environment(data, env)
    source = stone_peer.provenance(ROOT, launch_env)
    launch_receipt = stone_peer.read_json(safe_evidence_path(ROOT, OUTPUT + "/launch.json"))
    if (set(launch_receipt) != {"startedAt", "requestReceipt", "provenance", "command"}
            or launch_receipt["requestReceipt"] != receipt(data) or launch_receipt["provenance"] != source
            or launch_receipt["command"] != peer_command()):
        raise ValueError("Supervised launch request/source/run/command receipt differs")
    datetime.fromisoformat(launch_receipt["startedAt"])
    base = ROOT / PEER_OUTPUT
    launch_path = safe_evidence_path(ROOT, PEER_OUTPUT + "/stone-hinge-peer-launch.json")
    launch = stone_peer.read_json(launch_path, 2 * 1024 * 1024)
    with peer_validation_environment(launch_env):
        launch_hash = stone_peer.validate_launch(launch, launch_path, ROOT)
    descriptor_path = safe_evidence_path(ROOT, PEER_OUTPUT + "/stone-hinge-peer-descriptor.json")
    descriptor = stone_peer.read_json(descriptor_path)
    if (digest(descriptor_path) != launch["descriptorSha256"]
            or descriptor.get("entrypoints", {}).get("fabric-client-gametest") != [stone_peer.ENTRYPOINT]):
        raise ValueError("Retained descriptor must contain exactly the exported peer entrypoint")
    report = stone_peer.read_json(safe_evidence_path(ROOT, PEER_OUTPUT + "/matrix-result.json"), 2 * 1024 * 1024)
    if report.get("error") is not None:
        raise ValueError("Supervised matrix retained an error")
    expected = {"schemaVersion": 1, "suite": PEER_CASE, "status": "passed", **stone_peer.FLAGS,
                "totalTimeoutSeconds": 1200, "exportCeilingSeconds": 180, "profileCeilingSeconds": 300,
                "finalValidationCeilingSeconds": 60, "reservedCleanupSeconds": 60,
                "maxConcurrentJvms": 2, "heapMiBPerJvm": 2048, "provenance": source,
                "launchSha256": launch_hash, "descriptorSha256": launch["descriptorSha256"],
                "runtimeSha256": launch["runtimeSha256"]}
    for key, value in expected.items():
        if report.get(key) != value or type(report.get(key)) is not type(value):
            raise ValueError("Supervised result changed or failed: " + key)
    for field, maximum in (("seconds", 1200), ("exportSeconds", 180), ("finalValidationSeconds", 60)):
        if not 0 <= stone_peer.number(report.get(field), field) <= maximum:
            raise ValueError("Supervised result exceeded " + field)
    profiles = report.get("profiles")
    if (not isinstance(profiles, list) or len(profiles) != 3
            or [item.get("profile") for item in profiles] != list(stone_peer.PROFILES)):
        raise ValueError("All three exact ordered supervised profiles are required")
    seen = set()
    for item in profiles:
        profile = item["profile"]
        result_path = safe_evidence_path(ROOT, PEER_OUTPUT + "/" + profile + "/result.json")
        result = stone_peer.read_json(result_path, 2 * 1024 * 1024)
        if (result.get("hostUuid") != stone_peer.s.PROFILES["host"][1]
                or result.get("peerUuid") != stone_peer.s.PROFILES["peer"][1]):
            raise ValueError("Supervised profile changed its fixed native player identities")
        if item != {"profile": profile, "status": "passed", "nonce": result.get("nonce"), "resultSha256": digest(result_path)}:
            raise ValueError("Completed supervised profile hash/status/nonce changed")
        profile_base = base / profile
        _, files = stone_peer.evidence_files(profile_base)
        if not set(files) <= peer_evidence_names(profile):
            raise ValueError("Unexpected supervised evidence path")
        # Validator binds source, descriptor, roles, UUIDs, nonces, PIDs, exits,
        # cleanup, all case witnesses/packet chains, and all four FIFO hashes.
        stone_peer.validate_profile(profile_base, result, launch, launch_hash, source, seen)
        if result.get("nativeCompleted") is not True:
            raise ValueError("Native profile never completed")
    if len(seen) != 3:
        raise ValueError("Every supervised profile requires a fresh nonce")
    return report


def collect_peer(data, env):
    evidence = preserve_peer_evidence()
    issues = list(evidence["issues"])
    log = ""
    try:
        path = safe_evidence_path(ROOT, LOG)
        with path.open("rb") as source:
            raw = source.read(MAX_LOG_BYTES + 1)
        if len(raw) > MAX_LOG_BYTES:
            issues.append("Supervised launcher log exceeds the bounded evidence size")
        raw = raw[:MAX_LOG_BYTES]
        safe_evidence_path(ROOT, OUTPUT + "/native.log").write_bytes(raw)
        log = raw.decode("utf-8-sig", errors="replace")
        markers = [line[len(REQUEST_PREFIX):] for line in log.splitlines() if line.startswith(REQUEST_PREFIX)]
        if len(markers) != 1 or json.loads(markers[0], object_pairs_hook=unique_object) != receipt(data):
            raise ValueError("Supervised launcher request/source/run receipt differs")
        if [line[len(EXIT_PREFIX):] for line in log.splitlines() if line.startswith(EXIT_PREFIX)] != ["0"]:
            raise ValueError("Supervised launcher did not complete exactly once with exit zero")
    except (ValueError, OSError, TypeError) as error:
        issues.append(str(error))
    try:
        data["supervisedResult"] = validate_peer_result(data, env)
        verify_peer_retention(evidence)
    except (ValueError, OSError, KeyError, TypeError, AttributeError, subprocess.SubprocessError) as error:
        issues.append("Supervised evidence unverified: " + str(error))
    data.update(diagnosticOutcome="unverified" if issues else "passed", verificationIssues=issues,
                evidence=evidence, **stone_peer.FLAGS, admissionGate="NOT_PROVEN", paidFormGate="NOT_PROVEN",
                collectedAt=datetime.now(timezone.utc).isoformat())
    write_json("diagnostic-result.json", data)
    print("Diagnostic outcome: " + data["diagnosticOutcome"] + "; full and focused release gates remain unverified")
    return 1 if issues else 0


def observed_seeds(log, case):
    if case == "gale-vault-ballistic":
        return whole_class_seeds(log, [GALE_BALLISTIC_ENTRY], "Gale Vault ballistic")
    if case in STONE_MARCH_DIAGNOSTICS:
        return stone_march_seeds(log, STONE_MARCH_DIAGNOSTICS[case])
    found, issues = {}, []
    strict = case in ("progression-feasibility", "reweave-player", "stone-hinge-owner-negative",
                      "movement-foundations", "excise", "stone-fault-march-visuals")
    for line in log.splitlines():
        if SEED_PREFIX not in line:
            continue
        try:
            marker = json.loads(line.split(SEED_PREFIX, 1)[1],
                                object_pairs_hook=unique_object if strict else dict)
            if strict and (not isinstance(marker, dict) or set(marker) != {"suite", "seed"}):
                raise ValueError()
            entry, seed = marker["suite"], marker["seed"]
            if entry not in SEEDS[case] or not isinstance(seed, str) or not re.fullmatch(r"-?[0-9]{1,19}", seed):
                raise ValueError()
            if (strict or case in ("wall-turn", "kiln-ring", "ecology-return", "stasis-gallery", "stone-fault-march")) and entry in found:
                issues.append("Repeated native world seed marker for " + entry)
            if strict and not -(2 ** 63) <= int(seed) < 2 ** 63:
                raise ValueError()
            found.setdefault(entry, set()).add(seed)
        except (ValueError, KeyError, TypeError):
            issues.append("Invalid native world seed marker")
    for entry, expected in SEEDS[case].items():
        values = found.get(entry, set())
        if len(values) != 1 or expected is not None and values != {expected}:
            issues.append("Missing or inconsistent observed world seed for " + entry)
    if case == "stone-fault-march-visuals" and list(found) != list(STONE_MARCH_VISUAL_ENTRIES):
        issues.append("Stone Fault March visuals requires exactly both original world seeds in class order")
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
    if data["request"]["case"] == PEER_CASE:
        return collect_peer(data, env)
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
    if data["request"]["case"] == "gale-vault-ballistic":
        try:
            launch_path = ROOT / OUTPUT / "launch.json"
            if launch_path.is_symlink():
                raise ValueError("Linked launch receipt")
            with launch_path.open("rb") as stream:
                raw_launch = stream.read(64 * 1024 + 1)
            if len(raw_launch) > 64 * 1024:
                raise ValueError("Oversized launch receipt")
            launch = json.loads(raw_launch, object_pairs_hook=unique_object)
            expected = {"startedAt": launch["startedAt"], "provenance": data["provenance"],
                        "command": run_client_ci.launch_command(data["selection"])}
            if (not exact_json_marker(raw_launch, expected)
                    or datetime.fromisoformat(launch["startedAt"]).tzinfo is None):
                raise ValueError("Stale or malformed launch receipt")
        except (ValueError, OSError, KeyError, TypeError):
            issues.append("Gale Vault ballistic launch receipt must match this run and its fixed command")
    issues.extend(selection_issues(log, data["selection"]))
    markers = [line[len(REQUEST_PREFIX):] for line in log.splitlines() if line.startswith(REQUEST_PREFIX)]
    try:
        matches = len(markers) == 1 and (
            exact_json_marker(markers[0], receipt(data))
            if data["request"]["case"] in (*STONE_MARCH_DIAGNOSTICS, "gale-vault-ballistic")
            else json.loads(markers[0]) == receipt(data))
        if not matches:
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
    if data["request"]["case"] == "gale-vault-ballistic":
        data["completedEntries"], _ = gale_ballistic_completion(log)
    if data["request"]["case"] == "movement-foundations":
        data["completedEntries"], _ = movement_foundations_completion(log)
        data.update(movementGate="NOT_PROVEN", gameplayEnabled=False, peerGate="NOT_PROVEN",
                    latencyGate="NOT_PROVEN", admissionGate="NOT_PROVEN")
    if data["request"]["case"] == "excise":
        data["completedEntries"], _ = life_excise_completion(log)
    if data["request"]["case"] == "stone-fault-march":
        data["completedEntries"], _ = stone_march_completion(log)
    if data["request"]["case"] in STONE_MARCH_DIAGNOSTICS:
        data["completedEntries"], _ = _whole_class_completion(
            log, STONE_MARCH_DIAGNOSTICS[data["request"]["case"]], "Stone Fault March diagnostic")
    successful = "BUILD SUCCESSFUL" in log and "BUILD FAILED" not in log and not issues
    if data["request"]["case"] == "gale-vault-ballistic":
        data.update(ordinaryAttack=False, physicsNativePending=not successful)
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
