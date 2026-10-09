package dev.wildercord.presentation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.wildercord.presentation.CombatPresentationOptions.Animation;
import dev.wildercord.presentation.CombatPresentationOptions.Camera;
import dev.wildercord.presentation.CombatPresentationOptions.Draft;
import dev.wildercord.presentation.CombatPresentationOptions.Launch;
import dev.wildercord.presentation.CombatPresentationOptions.Saved;
import dev.wildercord.presentation.CombatPresentationOptions.Warning;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static dev.wildercord.presentation.CombatPresentationOptions.GROUP;
import static dev.wildercord.presentation.CombatPresentationOptions.parse;
import static dev.wildercord.presentation.CombatPresentationOptions.resolve;
import static org.junit.jupiter.api.Assertions.*;

class VisualPreferencesFileTest {

	@TempDir
	Path directory;

	@Test
	void freshConfigReadIsSideEffectFreeAndRetainsClassicLegacyCamera() throws IOException {
		Path path = directory.resolve("config/nested/wildercord-visuals.json");
		VisualPreferencesFile file = new VisualPreferencesFile(path);
		assertEquals(path, file.path());
		JsonObject read = file.read();
		assertTrue(read.isEmpty());
		assertEquals(Saved.LEGACY, parse(read.get(GROUP)).saved());
		assertFalse(resolve(parse(read.get(GROUP)).saved(), Launch.NONE).articulated());
		assertFalse(resolve(parse(read.get(GROUP)).saved(), Launch.NONE).stableCamera());
		assertFalse(Files.exists(path.getParent()), "Reading defaults must not create directories or a file");
	}

	@Test
	void oldFileWithoutCombatGroupReadsAndSavesLegacyPreferencesWithoutMigration() throws IOException {
		Path path = write("{\"reduceFlash\":true,\"unrelated\":{\"value\":4}}");
		VisualPreferencesFile file = new VisualPreferencesFile(path);
		assertEquals(Saved.LEGACY, parse(file.read().get(GROUP)).saved());
		file.write(json("{\"reduceFlash\":false}"), null);
		JsonObject restarted = new VisualPreferencesFile(path).read();
		assertFalse(restarted.has(GROUP));
		assertFalse(restarted.get("reduceFlash").getAsBoolean());
		assertEquals(4, restarted.getAsJsonObject("unrelated").get("value").getAsInt());
		assertNoTemporaryFiles();
	}

	@Test
	void applyCreatesDirectoriesAndPersistsAnExplicitSelectionAcrossRestart() throws IOException {
		Path path = directory.resolve("config/nested/wildercord-visuals.json");
		VisualPreferencesFile file = new VisualPreferencesFile(path);
		Draft draft = new Draft(Saved.LEGACY);
		draft.toggleAnimation();
		Saved selected = draft.selection();
		file.write(json("{\"reduceFlash\":true,\"auraQuality\":\"high\"}"), selected);

		VisualPreferencesFile restarted = new VisualPreferencesFile(path);
		JsonObject read = restarted.read();
		assertEquals(new Saved(Animation.ARTICULATED, Camera.STABLE), parse(read.get(GROUP)).saved());
		assertEquals(Warning.NONE, parse(read.get(GROUP)).warning());
		assertTrue(read.get("reduceFlash").getAsBoolean());
		assertEquals("high", read.get("auraQuality").getAsString());
		assertTrue(resolve(parse(read.get(GROUP)).saved(), Launch.NONE).armorArms());
		assertFalse(new Draft(parse(read.get(GROUP)).saved()).dirty());
		try (var children = Files.list(path.getParent())) {
			assertEquals(List.of(path), children.toList(), "An atomic save must remove its temporary file");
		}
	}

	@Test
	void persistedCameraAndRendererRemainIndependentAcrossRepeatedSaves() throws IOException {
		Path path = directory.resolve("wildercord-visuals.json");
		for (Animation animation : Animation.values()) {
			for (Camera camera : Camera.values()) {
				Saved saved = new Saved(animation, camera);
				new VisualPreferencesFile(path).write(new JsonObject(), saved);
				Saved read = parse(new VisualPreferencesFile(path).read().get(GROUP)).saved();
				assertEquals(saved, read);
				assertEquals(animation == Animation.ARTICULATED, resolve(read, Launch.NONE).articulated());
				assertEquals(camera == Camera.STABLE, resolve(read, Launch.NONE).stableCamera());
			}
		}
		assertNoTemporaryFiles();
	}

	@Test
	void launchOverrideNeverBecomesPersistedSelection() throws IOException {
		Path path = directory.resolve("wildercord-visuals.json");
		Saved selected = new Saved(Animation.ARTICULATED, Camera.DEFAULT);
		Launch override = new Launch(CombatPresentationOptions.LaunchOverride.FALSE,
			CombatPresentationOptions.LaunchOverride.TRUE, CombatPresentationOptions.LaunchOverride.FALSE,
			CombatPresentationOptions.LaunchOverride.FALSE, CombatPresentationOptions.LaunchOverride.FALSE);
		assertFalse(resolve(selected, override).articulated());
		assertTrue(resolve(selected, override).stableCamera());
		new VisualPreferencesFile(path).write(new JsonObject(), selected);
		Saved restarted = parse(new VisualPreferencesFile(path).read().get(GROUP)).saved();
		assertEquals(selected, restarted);
		assertTrue(resolve(restarted, Launch.NONE).articulated());
		assertFalse(resolve(restarted, Launch.NONE).stableCamera());
	}

	@Test
	void cancelAndResetRemainStagedUntilTheFileIsExplicitlyApplied() throws IOException {
		Path path = write("""
			{"reduceFlash":false,"combat_presentation":{"version":1,"animation":"articulated","camera":"default"}}
			""");
		String original = Files.readString(path);
		VisualPreferencesFile file = new VisualPreferencesFile(path);
		Saved live = parse(file.read().get(GROUP)).saved();
		Draft cancelled = new Draft(live);
		cancelled.toggleAnimation();
		cancelled.toggleCamera();
		assertTrue(cancelled.dirty());
		assertEquals(original, Files.readString(path));
		assertEquals(live, parse(new VisualPreferencesFile(path).read().get(GROUP)).saved());

		Draft reset = new Draft(live);
		reset.reset();
		assertEquals(original, Files.readString(path));
		assertEquals(new Saved(Animation.ARTICULATED, Camera.DEFAULT), live);
		file.write(new JsonObject(), reset.selection());
		assertEquals(Saved.RESET, parse(new VisualPreferencesFile(path).read().get(GROUP)).saved());
		assertFalse(new Draft(parse(file.read().get(GROUP)).saved()).dirty());
	}

	@Test
	void applyRereadsAndPreservesUnknownTopLevelAndGroupFieldsAddedAfterOpening() throws IOException {
		Path path = write("""
			{"reduceFlash":false,"unknown":{"old":[1,2],"nullValue":null},"combat_presentation":{"version":1,"animation":"classic","camera":"default","groupExtra":{"keep":true,"nullValue":null},"nullGroupField":null}}
			""");
		VisualPreferencesFile file = new VisualPreferencesFile(path);
		Draft draft = new Draft(parse(file.read().get(GROUP)).saved());
		draft.toggleAnimation();
		JsonObject externalEdit = file.read();
		externalEdit.add("addedWhileOpen", json("{\"nested\":[1,{\"text\":\"é雪\"}]}"));
		externalEdit.getAsJsonObject(GROUP).addProperty("addedGroupField", "later");
		Files.writeString(path, externalEdit.toString());

		JsonObject legacy = json("{\"reduceFlash\":true,\"legacyExtra\":{\"setting\":3}}");
		JsonObject legacyBefore = legacy.deepCopy();
		file.write(legacy, draft.selection());
		JsonObject after = new VisualPreferencesFile(path).read();
		assertEquals(legacyBefore, legacy, "Saving must not mutate the caller's legacy settings object");
		assertEquals(externalEdit.get("unknown"), after.get("unknown"));
		assertEquals(externalEdit.get("addedWhileOpen"), after.get("addedWhileOpen"));
		assertEquals(externalEdit.getAsJsonObject(GROUP).get("groupExtra"), after.getAsJsonObject(GROUP).get("groupExtra"));
		assertTrue(after.getAsJsonObject(GROUP).has("nullGroupField"));
		assertTrue(after.getAsJsonObject(GROUP).get("nullGroupField").isJsonNull());
		assertEquals("later", after.getAsJsonObject(GROUP).get("addedGroupField").getAsString());
		assertEquals(legacy.get("legacyExtra"), after.get("legacyExtra"));
		assertTrue(after.get("reduceFlash").getAsBoolean());
		assertEquals(new Saved(Animation.ARTICULATED, Camera.DEFAULT), parse(after.get(GROUP)).saved());
		assertNoTemporaryFiles();
	}

	@Test
	void legacyOnlySavePreservesExistingCombatSettingsAndUnknownFields() throws IOException {
		Path path = write("""
			{"reduceFlash":false,"unknown":null,"combat_presentation":{"version":1,"animation":"articulated","camera":"default","extra":[true,4]}}
			""");
		VisualPreferencesFile file = new VisualPreferencesFile(path);
		JsonObject before = file.read();
		file.write(json("{\"reduceFlash\":true}"), null);
		JsonObject after = new VisualPreferencesFile(path).read();
		assertEquals(before.get(GROUP), after.get(GROUP));
		assertEquals(before.get("unknown"), after.get("unknown"));
		assertTrue(after.has("unknown"));
		assertTrue(after.get("reduceFlash").getAsBoolean());
	}

	@Test
	void futureCombatGroupSurvivesLegacyOnlySaveExactlyAsJson() throws IOException {
		Path path = write("""
			{"reduceFlash":false,"outside":{"retain":8,"nullValue":null},"combat_presentation":{"version":200,"animation":{"new":"schema"},"camera":["future",null],"futureOnly":{"blob":[1,2,3],"nullValue":null},"nullGroupField":null}}
			""");
		VisualPreferencesFile file = new VisualPreferencesFile(path);
		JsonObject before = file.read();
		assertEquals(Warning.FUTURE_VERSION, parse(before.get(GROUP)).warning());
		file.write(json("{\"reduceFlash\":true}"), null);
		JsonObject after = new VisualPreferencesFile(path).read();
		assertEquals(before.get(GROUP), after.get(GROUP));
		assertEquals(before.get("outside"), after.get("outside"));
		assertTrue(after.get("reduceFlash").getAsBoolean());
		assertEquals(Warning.FUTURE_VERSION, parse(after.get(GROUP)).warning());
		assertNoTemporaryFiles();
	}

	@Test
	void futureVersionRefusesCombatOverwriteIncludingResetWithoutChangingAnyOriginalBytes() throws IOException {
		Path path = write("""
			{ "reduceFlash" : false, "combat_presentation" : { "version": 2, "newStyle": "keep me" } }
			""");
		String original = Files.readString(path);
		for (Saved selection : new Saved[] {Saved.LEGACY, Saved.RESET, new Saved(Animation.ARTICULATED, Camera.DEFAULT)}) {
			assertThrows(IOException.class, () -> new VisualPreferencesFile(path).write(json("{\"reduceFlash\":true}"), selection));
			assertEquals(original, Files.readString(path), "A refused combat save must not partially apply legacy fields");
		}
		assertNoTemporaryFiles();
	}

	@Test
	void futureVersionAddedAfterScreenOpensIsStillProtectedAtApply() throws IOException {
		Path path = write("{\"combat_presentation\":{\"version\":1,\"animation\":\"classic\"}}");
		VisualPreferencesFile file = new VisualPreferencesFile(path);
		Draft draft = new Draft(parse(file.read().get(GROUP)).saved());
		draft.toggleAnimation();
		String newer = "{\"combat_presentation\":{\"version\":3,\"newChoice\":\"keep\"}}";
		Files.writeString(path, newer);
		assertThrows(IOException.class, () -> file.write(new JsonObject(), draft.selection()));
		assertEquals(newer, Files.readString(path));
		assertTrue(draft.dirty(), "A failed Apply must keep the user's staged selection available");
		assertNoTemporaryFiles();
	}

	@ParameterizedTest
	@ValueSource(strings = {"null", "[]", "true", "17", "\"text\""})
	void malformedCombatGroupIsPreservedByLegacySaveAndRepairableByExplicitApply(String group) throws IOException {
		Path path = write("{\"reduceFlash\":false,\"combat_presentation\":" + group + "}");
		VisualPreferencesFile file = new VisualPreferencesFile(path);
		assertEquals(Warning.INVALID_SAVED, parse(file.read().get(GROUP)).warning());
		file.write(json("{\"reduceFlash\":true}"), null);
		assertEquals(JsonParser.parseString(group), file.read().get(GROUP));
		file.write(new JsonObject(), Saved.RESET);
		assertEquals(Saved.RESET, parse(new VisualPreferencesFile(path).read().get(GROUP)).saved());
		assertEquals(Warning.NONE, parse(file.read().get(GROUP)).warning());
		assertTrue(file.read().get("reduceFlash").getAsBoolean());
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "{", "{\"unfinished\":", "[1,2]", "null", "true", "7", "\"text\""})
	void malformedOrNonObjectFileCannotBeOverwrittenByEitherSavePath(String original) throws IOException {
		Path path = write(original);
		VisualPreferencesFile file = new VisualPreferencesFile(path);
		assertThrows(IOException.class, file::read);
		assertThrows(IOException.class, () -> file.write(json("{\"reduceFlash\":true}"), null));
		assertEquals(original, Files.readString(path));
		assertThrows(IOException.class, () -> file.write(new JsonObject(), Saved.RESET));
		assertEquals(original, Files.readString(path));
		assertNoTemporaryFiles();
	}

	@Test
	void malformedFileWrittenAfterScreenOpensCannotBeClobberedByApply() throws IOException {
		Path path = write("{}");
		VisualPreferencesFile file = new VisualPreferencesFile(path);
		Draft draft = new Draft(parse(file.read().get(GROUP)).saved());
		draft.toggleAnimation();
		String externalBrokenEdit = "{\"manualEdit\": ";
		Files.writeString(path, externalBrokenEdit);
		assertThrows(IOException.class, () -> file.write(new JsonObject(), draft.selection()));
		assertEquals(externalBrokenEdit, Files.readString(path));
		assertTrue(draft.dirty());
		assertNoTemporaryFiles();
	}

	@Test
	void unreadableUtf8BytesArePreservedByBothSavePaths() throws IOException {
		Path path = directory.resolve("wildercord-visuals.json");
		byte[] original = {'{', '"', 'x', '"', ':', '"', (byte) 0xC3, (byte) 0x28, '"', '}'};
		Files.write(path, original);
		VisualPreferencesFile file = new VisualPreferencesFile(path);
		assertThrows(IOException.class, file::read);
		assertThrows(IOException.class, () -> file.write(json("{\"reduceFlash\":true}"), null));
		assertArrayEquals(original, Files.readAllBytes(path));
		assertThrows(IOException.class, () -> file.write(new JsonObject(), Saved.RESET));
		assertArrayEquals(original, Files.readAllBytes(path));
		assertNoTemporaryFiles();
	}

	@Test
	void unreadableDestinationDirectoryAndItsContentsArePreserved() throws IOException {
		Path path = directory.resolve("wildercord-visuals.json");
		Files.createDirectory(path);
		Path retained = path.resolve("original-data.txt");
		Files.writeString(retained, "preserve this content");
		VisualPreferencesFile file = new VisualPreferencesFile(path);
		assertThrows(IOException.class, file::read);
		assertThrows(IOException.class, () -> file.write(new JsonObject(), Saved.RESET));
		assertThrows(IOException.class, () -> file.write(json("{\"reduceFlash\":true}"), null));
		assertTrue(Files.isDirectory(path));
		assertEquals("preserve this content", Files.readString(retained));
		try (var children = Files.list(path)) {
			assertEquals(List.of(retained), children.toList());
		}
		assertNoTemporaryFiles();
	}

	@Test
	void impossibleParentPathFailsWriteAndPreservesOriginalBlockingFile() throws IOException {
		Path blockingFile = directory.resolve("config");
		Files.writeString(blockingFile, "existing user content");
		Path path = blockingFile.resolve("wildercord-visuals.json");
		VisualPreferencesFile file = new VisualPreferencesFile(path);
		assertThrows(IOException.class, () -> file.write(json("{\"reduceFlash\":true}"), Saved.RESET));
		assertEquals("existing user content", Files.readString(blockingFile));
		assertFalse(Files.exists(path));
		try (var children = Files.list(directory)) {
			assertEquals(List.of(blockingFile), children.toList());
		}
	}

	@Test
	void failingDestinationMoveCleansTemporaryFileAndPreservesSiblingData() throws IOException {
		Path retained = directory.resolve("original-data.txt");
		Files.writeString(retained, "untouched");
		// A path component beyond the filesystem limit fails the destination move even as root.
		Path impossibleDestination = directory.resolve("x".repeat(300) + ".json");
		assertThrows(IOException.class, () -> new VisualPreferencesFile(impossibleDestination).write(new JsonObject(), Saved.RESET));
		assertEquals("untouched", Files.readString(retained));
		assertFalse(Files.exists(impossibleDestination));
		try (var children = Files.list(directory)) {
			assertEquals(List.of(retained), children.toList(), "Failed replacement must remove its temporary file");
		}
	}

	private Path write(String content) throws IOException {
		Path path = directory.resolve("wildercord-visuals.json");
		Files.writeString(path, content);
		return path;
	}

	private static JsonObject json(String text) {
		return JsonParser.parseString(text).getAsJsonObject();
	}

	private void assertNoTemporaryFiles() throws IOException {
		try (var files = Files.walk(directory)) {
			assertTrue(files.noneMatch(path -> path.getFileName().toString().startsWith("wildercord-visuals-")
				&& path.getFileName().toString().endsWith(".tmp")), "Save must not leave temporary files behind");
		}
	}
}
