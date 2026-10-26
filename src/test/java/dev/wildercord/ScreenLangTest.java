package dev.wildercord;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The Cord, Fusion Altar and lesson screens read their sentences from the lang file, so a translation reaches them. */
class ScreenLangTest {
	private static final Path CLIENT = Path.of("src/client/java/dev/wildercord/client");

	@Test
	void theirKeysExist() throws Exception {
		var language = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/wildercord/lang/en_us.json"))).getAsJsonObject();
		for (String key : List.of("screen.wildercord.altar.dual_line", "screen.wildercord.cord.unfinished", "screen.wildercord.cord.lesson_needs",
				"screen.wildercord.cord.excise_needs", "screen.wildercord.cord.reweave_needs", "screen.wildercord.cord.relay_needs",
				"screen.wildercord.lesson_pack.circle")) {
			assertTrue(language.has(key), key + " is missing from en_us.json");
		}
		assertEquals("Unfinished %s · cannot cast", language.get("screen.wildercord.cord.unfinished").getAsString());
		assertEquals(6, language.get("screen.wildercord.cord.lesson_needs").getAsString().split("%s", -1).length - 1, "lesson_needs takes six arguments");
	}

	@Test
	void noScreenHardCodesThem() throws Exception {
		String cord = Files.readString(CLIENT.resolve("CordScreen.java"));
		for (String english : List.of("Component.literal(\"Unfinished", " needs \" +", "needs Rootbound study", "needs Ebb Ledger study", "needs its Archive lesson")) {
			assertFalse(cord.contains(english), "CordScreen still hard-codes " + english);
		}
		assertFalse(Files.readString(CLIENT.resolve("FusionAltarScreen.java")).contains("\"Both effects in one socket"));
		assertFalse(Files.readString(CLIENT.resolve("PackLessonScreen.java")).contains("Component.literal(\"Circle "));
	}
}
