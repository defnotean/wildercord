package dev.wildercord.presentation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.wildercord.presentation.CombatPresentationOptions.Animation;
import dev.wildercord.presentation.CombatPresentationOptions.Camera;
import dev.wildercord.presentation.CombatPresentationOptions.Draft;
import dev.wildercord.presentation.CombatPresentationOptions.Effective;
import dev.wildercord.presentation.CombatPresentationOptions.Launch;
import dev.wildercord.presentation.CombatPresentationOptions.LaunchOverride;
import dev.wildercord.presentation.CombatPresentationOptions.Saved;
import dev.wildercord.presentation.CombatPresentationOptions.Warning;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static dev.wildercord.presentation.CombatPresentationOptions.parse;
import static dev.wildercord.presentation.CombatPresentationOptions.resolve;
import static org.junit.jupiter.api.Assertions.*;

class CombatPresentationOptionsTest {

	@Test
	void missingGroupKeepsClassicAndExistingCameraUntilAnExplicitApply() {
		var parsed = parse(null);
		assertEquals(Saved.LEGACY, parsed.saved());
		assertEquals(Warning.NONE, parsed.warning());
		assertEquals(new Effective(false, false, false, false, false), resolve(parsed.saved(), Launch.NONE));
		assertNotEquals(Saved.RESET, parsed.saved(), "Opening an old config must not silently reset its camera");
	}

	@ParameterizedTest
	@CsvSource({
		"classic, stable, CLASSIC, STABLE",
		"classic, default, CLASSIC, DEFAULT",
		"articulated, stable, ARTICULATED, STABLE",
		"articulated, default, ARTICULATED, DEFAULT"
	})
	void readsVersionOneChoicesIndependently(String animation, String camera,
		Animation expectedAnimation, Camera expectedCamera) {
		var parsed = parse(JsonParser.parseString("{\"version\":1,\"animation\":\"" + animation
			+ "\",\"camera\":\"" + camera + "\"}"));
		assertEquals(new Saved(expectedAnimation, expectedCamera), parsed.saved());
		assertEquals(Warning.NONE, parsed.warning());
	}

	@ParameterizedTest
	@ValueSource(strings = {"{\"animation\":\"classic\"}", "{\"version\":1,\"animation\":\"classic\"}"})
	void oldGroupWithoutCameraRetainsMigrationState(String json) {
		var parsed = parse(JsonParser.parseString(json));
		assertEquals(Saved.LEGACY, parsed.saved());
		assertEquals(Warning.NONE, parsed.warning());
		assertFalse(resolve(parsed.saved(), Launch.NONE).stableCamera());
	}

	@Test
	void oldArticulatedGroupWithoutCameraDoesNotChooseStableCameraImplicitly() {
		var parsed = parse(JsonParser.parseString("{\"animation\":\"articulated\"}"));
		assertEquals(new Saved(Animation.ARTICULATED, null), parsed.saved());
		assertEquals(Warning.NONE, parsed.warning());
		assertEquals(new Effective(true, false, true, true, true), resolve(parsed.saved(), Launch.NONE));
	}

	@ParameterizedTest
	@ValueSource(strings = {"null", "true", "1", "\"articulated\"", "[]", "{}",
		"{\"animation\":true}", "{\"animation\":1}", "{\"animation\":null}",
		"{\"animation\":\"future\"}", "{\"animation\":\"ARTICULATED\"}"})
	void malformedGroupOrAnimationUsesClassicWithWarning(String json) {
		var parsed = parse(JsonParser.parseString(json));
		assertEquals(Saved.LEGACY, parsed.saved());
		assertEquals(Warning.INVALID_SAVED, parsed.warning());
	}

	@ParameterizedTest
	@ValueSource(strings = {"null", "true", "\"1\"", "0", "-1", "0.5", "[]", "{}"})
	void invalidVersionRejectsTheEntireGroup(String version) {
		var parsed = parse(JsonParser.parseString("{\"version\":" + version
			+ ",\"animation\":\"articulated\",\"camera\":\"stable\"}"));
		assertEquals(Saved.LEGACY, parsed.saved());
		assertEquals(Warning.INVALID_SAVED, parsed.warning());
	}

	@ParameterizedTest
	@ValueSource(strings = {"2", "999999999999999999999999999", "1.5", "1e99"})
	void newerVersionIsRecognizedWithoutInterpretingItsOptions(String version) {
		var parsed = parse(JsonParser.parseString("{\"version\":" + version
			+ ",\"animation\":\"articulated\",\"camera\":\"stable\"}"));
		assertEquals(Saved.LEGACY, parsed.saved());
		assertEquals(Warning.FUTURE_VERSION, parsed.warning());
	}

	@Test
	void equivalentNumericVersionOneAndUnknownFieldsRemainCompatible() {
		var parsed = parse(JsonParser.parseString("""
			{"version":1.0,"animation":"articulated","camera":"default","future":{"mode":7}}
			"""));
		assertEquals(new Saved(Animation.ARTICULATED, Camera.DEFAULT), parsed.saved());
		assertEquals(Warning.NONE, parsed.warning());
	}

	@ParameterizedTest
	@ValueSource(strings = {"null", "true", "7", "[]", "{}", "\"future\"", "\"STABLE\""})
	void invalidCameraFallsBackToStableWithoutLosingValidAnimation(String camera) {
		var parsed = parse(JsonParser.parseString("{\"version\":1,\"animation\":\"articulated\",\"camera\":"
			+ camera + "}"));
		assertEquals(new Saved(Animation.ARTICULATED, Camera.STABLE), parsed.saved());
		assertEquals(Warning.INVALID_SAVED, parsed.warning());
	}

	@Test
	void invalidAnimationDoesNotDiscardExplicitCameraChoice() {
		var parsed = parse(JsonParser.parseString("{\"animation\":\"unknown\",\"camera\":\"default\"}"));
		assertEquals(new Saved(Animation.CLASSIC, Camera.DEFAULT), parsed.saved());
		assertEquals(Warning.INVALID_SAVED, parsed.warning());
	}

	@Test
	void absentLaunchFlagsDoNotCreateAnOverride() {
		assertEquals(LaunchOverride.ABSENT, LaunchOverride.parse(null));
		assertFalse(LaunchOverride.ABSENT.present());
		assertTrue(LaunchOverride.ABSENT.choose(true));
		assertFalse(LaunchOverride.ABSENT.choose(false));
		assertFalse(Launch.NONE.any());
		assertFalse(Launch.NONE.invalid());
	}

	@ParameterizedTest
	@ValueSource(strings = {"true", "TRUE", "True", "tRuE"})
	void explicitTrueOverridesEitherSavedValue(String value) {
		var parsed = LaunchOverride.parse(value);
		assertEquals(LaunchOverride.TRUE, parsed);
		assertTrue(parsed.present());
		assertTrue(parsed.choose(false));
		assertTrue(parsed.choose(true));
	}

	@ParameterizedTest
	@ValueSource(strings = {"false", "FALSE", "False", "fAlSe"})
	void explicitFalseOverridesEitherSavedValue(String value) {
		var parsed = LaunchOverride.parse(value);
		assertEquals(LaunchOverride.FALSE, parsed);
		assertTrue(parsed.present());
		assertFalse(parsed.choose(false));
		assertFalse(parsed.choose(true));
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "yes", "1", " true", "true ", "\tfalse", "null"})
	void invalidLaunchValuesRemainExplicitFalseForLegacyCompatibility(String value) {
		var parsed = LaunchOverride.parse(value);
		assertEquals(LaunchOverride.INVALID, parsed);
		assertTrue(parsed.present());
		assertFalse(parsed.choose(false));
		assertFalse(parsed.choose(true));
	}

	@Test
	void everyIndividualLaunchFlagContributesToOverrideAndInvalidStatus() {
		for (int field = 0; field < 5; field++) {
			for (LaunchOverride value : new LaunchOverride[] {LaunchOverride.TRUE, LaunchOverride.FALSE, LaunchOverride.INVALID}) {
				LaunchOverride[] flags = {LaunchOverride.ABSENT, LaunchOverride.ABSENT, LaunchOverride.ABSENT, LaunchOverride.ABSENT, LaunchOverride.ABSENT};
				flags[field] = value;
				Launch launch = new Launch(flags[0], flags[1], flags[2], flags[3], flags[4]);
				assertTrue(launch.any(), "Flag " + field + " must appear in the launch warning");
				assertEquals(value == LaunchOverride.INVALID, launch.invalid());
			}
		}
	}

	@Test
	void explicitLegacyMasterLaunchRetainsOriginalAdapterDefaults() {
		assertEquals(new Effective(true, true, false, false, true),
			resolve(Saved.LEGACY, launch(LaunchOverride.TRUE, LaunchOverride.ABSENT, LaunchOverride.ABSENT, LaunchOverride.ABSENT, LaunchOverride.ABSENT)));
	}

	@Test
	void savedArticulatedProfileEnablesFullRendererWithoutRequiringLaunchFlags() {
		assertEquals(new Effective(true, true, true, true, true),
			resolve(new Saved(Animation.ARTICULATED, Camera.STABLE), Launch.NONE));
		assertEquals(new Effective(true, false, true, true, true),
			resolve(new Saved(Animation.ARTICULATED, Camera.DEFAULT), Launch.NONE));
	}

	@Test
	void savedCameraRemainsIndependentOfRendererAndMasterOverrides() {
		for (Animation animation : Animation.values()) {
			for (LaunchOverride master : LaunchOverride.values()) {
				Launch flags = launch(master, LaunchOverride.ABSENT, LaunchOverride.ABSENT, LaunchOverride.ABSENT, LaunchOverride.ABSENT);
				assertTrue(resolve(new Saved(animation, Camera.STABLE), flags).stableCamera());
				assertFalse(resolve(new Saved(animation, Camera.DEFAULT), flags).stableCamera());
			}
		}
	}

	@Test
	void explicitMasterFalseOrInvalidDisablesEveryRendererAdapterButNotSavedCamera() {
		for (LaunchOverride master : new LaunchOverride[] {LaunchOverride.FALSE, LaunchOverride.INVALID}) {
			assertEquals(new Effective(false, true, false, false, false),
				resolve(new Saved(Animation.ARTICULATED, Camera.STABLE),
					launch(master, LaunchOverride.ABSENT, LaunchOverride.TRUE, LaunchOverride.TRUE, LaunchOverride.TRUE)));
		}
	}

	@Test
	void explicitCameraFlagWorksEvenWhenRendererIsDisabled() {
		assertEquals(new Effective(false, true, false, false, false),
			resolve(Saved.LEGACY, launch(LaunchOverride.FALSE, LaunchOverride.TRUE, LaunchOverride.TRUE, LaunchOverride.TRUE, LaunchOverride.TRUE)));
		for (LaunchOverride camera : new LaunchOverride[] {LaunchOverride.FALSE, LaunchOverride.INVALID}) {
			assertFalse(resolve(Saved.RESET,
				launch(LaunchOverride.ABSENT, camera, LaunchOverride.ABSENT, LaunchOverride.ABSENT, LaunchOverride.ABSENT)).stableCamera());
		}
	}

	@Test
	void legacyArmorAndArmsRequireTheirOwnFlags() {
		assertEquals(new Effective(true, true, true, false, true),
			resolve(Saved.LEGACY, launch(LaunchOverride.TRUE, LaunchOverride.ABSENT, LaunchOverride.TRUE, LaunchOverride.ABSENT, LaunchOverride.ABSENT)));
		assertEquals(new Effective(true, true, true, true, true),
			resolve(Saved.LEGACY, launch(LaunchOverride.TRUE, LaunchOverride.ABSENT, LaunchOverride.TRUE, LaunchOverride.TRUE, LaunchOverride.ABSENT)));
		assertEquals(new Effective(true, true, false, false, true),
			resolve(Saved.LEGACY, launch(LaunchOverride.TRUE, LaunchOverride.ABSENT, LaunchOverride.ABSENT, LaunchOverride.TRUE, LaunchOverride.ABSENT)));
	}

	@Test
	void explicitAdapterFlagsOverrideSavedProfileWithNestedArmorGate() {
		Saved saved = new Saved(Animation.ARTICULATED, Camera.STABLE);
		for (LaunchOverride disabled : new LaunchOverride[] {LaunchOverride.FALSE, LaunchOverride.INVALID}) {
			assertEquals(new Effective(true, true, false, false, true),
				resolve(saved, launch(LaunchOverride.ABSENT, LaunchOverride.ABSENT, disabled, LaunchOverride.TRUE, LaunchOverride.ABSENT)));
			assertEquals(new Effective(true, true, true, false, true),
				resolve(saved, launch(LaunchOverride.ABSENT, LaunchOverride.ABSENT, LaunchOverride.ABSENT, disabled, LaunchOverride.ABSENT)));
			assertEquals(new Effective(true, true, true, true, false),
				resolve(saved, launch(LaunchOverride.ABSENT, LaunchOverride.ABSENT, LaunchOverride.ABSENT, LaunchOverride.ABSENT, disabled)));
		}
	}

	@Test
	void resolutionNeverRewritesTheSavedPreference() {
		Saved saved = new Saved(Animation.ARTICULATED, Camera.DEFAULT);
		assertEquals(new Effective(false, true, false, false, false),
			resolve(saved, launch(LaunchOverride.FALSE, LaunchOverride.TRUE, LaunchOverride.FALSE, LaunchOverride.FALSE, LaunchOverride.FALSE)));
		assertEquals(new Saved(Animation.ARTICULATED, Camera.DEFAULT), saved);
		assertEquals(new Effective(true, false, true, true, true), resolve(saved, Launch.NONE));
	}

	@Test
	void encodingPreservesUnknownMembersWithoutMutatingPreviousJson() {
		JsonObject previous = JsonParser.parseString("""
			{"version":1,"animation":"classic","camera":"default","future":{"levels":[2,3]}}
			""").getAsJsonObject();
		JsonObject before = previous.deepCopy();
		JsonObject encoded = CombatPresentationOptions.encode(new Saved(Animation.ARTICULATED, Camera.STABLE), previous);
		assertEquals(before, previous);
		assertEquals(previous.get("future"), encoded.get("future"));
		assertNotSame(previous.get("future"), encoded.get("future"));
		assertEquals(1, encoded.get("version").getAsInt());
		assertEquals("articulated", encoded.get("animation").getAsString());
		assertEquals("stable", encoded.get("camera").getAsString());
		assertEquals(new Saved(Animation.ARTICULATED, Camera.STABLE), parse(encoded).saved());
		encoded.getAsJsonObject("future").addProperty("later", true);
		assertEquals(before, previous, "Nested preserved JSON must not alias the original");
	}

	@Test
	void encodingLegacyMigrationStateRemovesPreviouslyExplicitCamera() {
		JsonObject encoded = CombatPresentationOptions.encode(Saved.LEGACY,
			JsonParser.parseString("{\"version\":1,\"animation\":\"articulated\",\"camera\":\"stable\",\"extra\":4}"));
		assertFalse(encoded.has("camera"));
		assertEquals(4, encoded.get("extra").getAsInt());
		assertEquals(Saved.LEGACY, parse(encoded).saved());
	}

	@Test
	void encodingCanReplaceMalformedGroupWithAValidSelection() {
		JsonObject encoded = CombatPresentationOptions.encode(Saved.RESET, JsonParser.parseString("[]"));
		assertEquals(Saved.RESET, parse(encoded).saved());
		assertEquals(Warning.NONE, parse(encoded).warning());
	}

	@Test
	void openingLegacyDraftStagesStableCameraWithoutChangingLiveOrSavedValues() {
		Saved saved = Saved.LEGACY;
		Draft draft = new Draft(saved);
		assertTrue(draft.firstCamera());
		assertEquals(Animation.CLASSIC, draft.animation());
		assertEquals(Camera.STABLE, draft.camera());
		assertFalse(draft.dirty());
		assertEquals(Saved.RESET, draft.selection());
		assertEquals(new Effective(false, false, false, false, false), resolve(saved, Launch.NONE));
	}

	@Test
	void draftToggleAndCancelDoNotAlterOriginalPreferenceOrAnotherDraft() {
		Saved saved = new Saved(Animation.CLASSIC, Camera.DEFAULT);
		Draft draft = new Draft(saved);
		draft.toggleAnimation();
		draft.toggleCamera();
		assertTrue(draft.dirty());
		assertEquals(new Saved(Animation.ARTICULATED, Camera.STABLE), draft.selection());
		assertEquals(new Saved(Animation.CLASSIC, Camera.DEFAULT), saved);
		Draft reopenedAfterCancel = new Draft(saved);
		assertFalse(reopenedAfterCancel.dirty());
		assertEquals(saved, reopenedAfterCancel.selection());
		assertEquals(new Effective(false, false, false, false, false), resolve(saved, Launch.NONE));
	}

	@Test
	void explicitCameraCanReturnToOriginalWithoutLeavingDraftDirty() {
		Draft draft = new Draft(new Saved(Animation.ARTICULATED, Camera.DEFAULT));
		assertFalse(draft.firstCamera());
		draft.toggleCamera();
		assertTrue(draft.dirty());
		draft.toggleCamera();
		assertFalse(draft.dirty());
		draft.toggleAnimation();
		assertTrue(draft.dirty());
		draft.toggleAnimation();
		assertFalse(draft.dirty());
	}

	@Test
	void touchingLegacyCameraMakesItsStableChoiceExplicitEvenAfterTwoToggles() {
		Draft draft = new Draft(Saved.LEGACY);
		draft.toggleCamera();
		assertEquals(Camera.DEFAULT, draft.camera());
		draft.toggleCamera();
		assertEquals(Camera.STABLE, draft.camera());
		assertTrue(draft.dirty());
		assertEquals(Saved.RESET, draft.selection());
		assertNull(Saved.LEGACY.camera());
	}

	@Test
	void resetOnlyStagesClassicStableAndIsAnExplicitApplyEvenAtDefaultValues() {
		for (Saved saved : new Saved[] {Saved.LEGACY, Saved.RESET, new Saved(Animation.ARTICULATED, Camera.DEFAULT)}) {
			Draft draft = new Draft(saved);
			draft.reset();
			assertTrue(draft.dirty());
			assertEquals(Saved.RESET, draft.selection());
			Draft reopenedAfterCancel = new Draft(saved);
			assertEquals(saved.animation(), reopenedAfterCancel.animation());
			assertFalse(reopenedAfterCancel.dirty());
		}
	}

	@Test
	void selectedValuesAreAnImmutableSnapshotForApply() {
		Draft draft = new Draft(Saved.LEGACY);
		draft.toggleAnimation();
		Saved selection = draft.selection();
		draft.toggleCamera();
		assertEquals(new Saved(Animation.ARTICULATED, Camera.STABLE), selection);
		assertEquals(new Saved(Animation.ARTICULATED, Camera.DEFAULT), draft.selection());
		Draft reopenedAfterApply = new Draft(selection);
		assertFalse(reopenedAfterApply.dirty());
		assertFalse(reopenedAfterApply.firstCamera());
		assertEquals(selection, reopenedAfterApply.selection());
	}

	private static Launch launch(LaunchOverride master, LaunchOverride camera, LaunchOverride armor, LaunchOverride arms, LaunchOverride shell) {
		return new Launch(master, camera, armor, arms, shell);
	}
}
