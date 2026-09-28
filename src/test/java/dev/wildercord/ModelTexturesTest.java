package dev.wildercord;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every texture our block and item models point at has to exist, or the game draws the magenta and
 * black checkerboard in its place (the Astral Observatory's altar once asked for vanilla's
 * {@code purpur_pillar}, which 26.3 calls {@code purpur_pillar_side}). Our own textures are looked up
 * in the resources; vanilla's in the Minecraft jar on the classpath.
 */
class ModelTexturesTest {
	private static final Path ASSETS = Path.of("src/main/resources/assets");

	@Test
	void everyModelTextureExists() throws IOException {
		assertTrue(getClass().getClassLoader().getResource("assets/minecraft/textures/block/stone.png") != null,
			"vanilla's textures should be on the test classpath");
		List<String> missing = new ArrayList<>();
		List<Path> models;
		try (Stream<Path> files = Files.walk(ASSETS.resolve("wildercord/models"))) {
			models = files.filter(p -> p.toString().endsWith(".json")).toList();
		}
		for (Path model : models) {
			JsonObject json = JsonParser.parseString(Files.readString(model)).getAsJsonObject();
			if (!json.has("textures")) {
				continue;
			}
			for (Map.Entry<String, JsonElement> texture : json.getAsJsonObject("textures").entrySet()) {
				String ref = texture.getValue().getAsString();
				if (ref.startsWith("#")) {
					continue;
				}
				String namespace = ref.contains(":") ? ref.substring(0, ref.indexOf(':')) : "minecraft";
				String path = "assets/" + namespace + "/textures/" + ref.substring(ref.indexOf(':') + 1) + ".png";
				boolean found = namespace.equals("minecraft")
					? getClass().getClassLoader().getResource(path) != null
					: Files.exists(Path.of("src/main/resources").resolve(path));
				if (!found) {
					missing.add(ASSETS.relativize(model) + " -> " + ref);
				}
			}
		}
		assertTrue(missing.isEmpty(), "models point at textures that don't exist:\n  " + String.join("\n  ", missing));
	}
}
