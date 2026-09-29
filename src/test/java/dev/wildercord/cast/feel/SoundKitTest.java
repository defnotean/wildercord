package dev.wildercord.cast.feel;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The generated sound files agree with each other (no ffmpeg needed: it reads what tools/feel/build.py wrote). */
class SoundKitTest {
	private static JsonObject read(String path) throws IOException {
		try (InputStream in = SoundKitTest.class.getResourceAsStream(path)) {
			assertNotNull(in, path + " is missing");
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	private static boolean exists(String path) {
		return SoundKitTest.class.getResource(path) != null;
	}

	@Test
	void everyKitEventIsInSoundsJsonWithItsFilesAndASubtitle() throws IOException {
		JsonObject kit = read("/assets/wildercord/kit_sounds.json").getAsJsonObject("events");
		JsonObject sounds = read("/assets/wildercord/sounds.json");
		JsonObject lang = read("/assets/wildercord/lang/en_us.json");
		assertTrue(kit.size() > 0);
		for (String name : kit.keySet()) {
			assertTrue(name.matches("[a-z][a-z0-9_]*"), name);
			JsonObject event = sounds.getAsJsonObject(name);
			assertNotNull(event, name + " is in kit_sounds.json but not sounds.json: run python tools/feel/build.py --merge");
			assertTrue(lang.has(event.get("subtitle").getAsString()), name + " has no subtitle text");
			for (JsonElement entry : event.getAsJsonArray("sounds")) {
				String file = entry.isJsonObject() ? entry.getAsJsonObject().get("name").getAsString() : entry.getAsString();
				assertTrue(exists("/assets/wildercord/sounds/" + file.substring(file.indexOf(':') + 1) + ".ogg"), name + ": " + file + ".ogg is missing");
			}
		}
	}

	@Test
	void everySoundEventInSoundsJsonHasItsFiles() throws IOException {
		JsonObject sounds = read("/assets/wildercord/sounds.json");
		for (String name : sounds.keySet()) {
			for (JsonElement entry : sounds.getAsJsonObject(name).getAsJsonArray("sounds")) {
				String file = entry.isJsonObject() ? entry.getAsJsonObject().get("name").getAsString() : entry.getAsString();
				assertTrue(exists("/assets/wildercord/sounds/" + file.substring(file.indexOf(':') + 1) + ".ogg"), name + ": " + file + ".ogg is missing");
			}
		}
	}
}
