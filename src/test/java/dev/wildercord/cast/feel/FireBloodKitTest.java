package dev.wildercord.cast.feel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every kit sound the fire and blood code plays exists in the kit (a typo would be skipped in game, logged once, never heard), and every one of theirs is played. */
class FireBloodKitTest {
	/** {@code Feels.sound(level, at, "name", ...)}, a local {@code snd(level, at, "name", ...)} and {@code .sound(Phase.X, "name")}. */
	private static final Pattern CALL = Pattern.compile("(?:Feels\\.sound|\\bsnd|\\.sound)\\((?:[^\"\\n;]*?)\"((?:fire|blood)_[a-z_]+)\"");

	private static Set<String> kit() throws IOException {
		try (InputStream in = FireBloodKitTest.class.getResourceAsStream("/assets/wildercord/kit_sounds.json")) {
			JsonObject events = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("events");
			return new HashSet<>(events.keySet());
		}
	}

	private static Set<String> named() throws IOException {
		Set<String> named = new HashSet<>();
		try (Stream<Path> files = Files.walk(Path.of("src/main/java/dev/wildercord"))) {
			for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
				Matcher m = CALL.matcher(Files.readString(file));
				while (m.find()) {
					named.add(m.group(1));
				}
			}
		}
		return named;
	}

	@Test
	void everyKitSoundTheFireAndBloodCodeNamesExists() throws IOException {
		Set<String> kit = kit();
		Set<String> named = named();
		assertFalse(named.isEmpty());
		for (String name : named) {
			assertTrue(kit.contains(name), "'" + name + "' is played but is not in the kit (tools/feel/fire.py, blood.py)");
		}
	}

	@Test
	void everyFireAndBloodKitSoundIsPlayedSomewhere() throws IOException {
		Set<String> played = named();
		for (String name : kit()) {
			// blood_slice_heavy is one of the two worked examples the kit shipped with (a heavier cut, for whoever needs one).
			if ((name.startsWith("fire_") || name.startsWith("blood_")) && !name.equals("blood_slice_heavy")) {
				assertTrue(played.contains(name), name + " is in the kit but nothing plays it");
			}
		}
	}
}
