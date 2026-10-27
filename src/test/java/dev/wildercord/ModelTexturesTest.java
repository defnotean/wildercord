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

	@Test
	void forgedWeaponsAndStaffsHaveValid3dHeldModels() throws IOException {
		List<String> weapons = new ArrayList<>(List.of("oathkeeper", "lumenedge", "bulwark_maul", "skyrend_glaive", "fetchblade"));
		for (String el : List.of("fire", "frost", "storm", "wind", "earth", "life", "void", "arcane", "time", "blood")) {
			weapons.add(el + "_staff");
			weapons.add("greater_" + el + "_staff");
		}
		for (String name : weapons) {
			Path itemDef = ASSETS.resolve("wildercord/items/" + name + ".json");
			Path guiModel = ASSETS.resolve("wildercord/models/item/" + name + ".json");
			Path heldModel = ASSETS.resolve("wildercord/models/item/" + name + "_in_hand.json");
			assertTrue(Files.exists(itemDef), "missing item definition: " + itemDef);
			assertTrue(Files.exists(guiModel), "missing GUI model: " + guiModel);
			assertTrue(Files.exists(heldModel), "missing 3D held model: " + heldModel);

			JsonObject defJson = JsonParser.parseString(Files.readString(itemDef)).getAsJsonObject().getAsJsonObject("model");
			assertTrue("minecraft:select".equals(defJson.get("type").getAsString()), name + " should select between GUI sprite and 3D held model");
			String fallback = defJson.getAsJsonObject("fallback").get("model").getAsString();
			assertTrue(("wildercord:item/" + name + "_in_hand").equals(fallback), name + " fallback should point to _in_hand model");

			JsonObject held = JsonParser.parseString(Files.readString(heldModel)).getAsJsonObject();
			JsonObject display = held.getAsJsonObject("display");
			for (String pose : List.of("firstperson_righthand", "firstperson_lefthand", "thirdperson_righthand", "thirdperson_lefthand", "ground", "fixed", "head")) {
				assertTrue(display.has(pose), name + "_in_hand missing display pose: " + pose);
			}
			var elements = held.getAsJsonArray("elements");
			assertTrue(elements.size() >= 14, name + "_in_hand should have a detailed multi-box voxel build");
			boolean hasGlow = false;
			for (JsonElement elem : elements) {
				JsonObject box = elem.getAsJsonObject();
				if (box.has("light_emission") && box.get("light_emission").getAsInt() > 0) {
					hasGlow = true;
				}
				for (String bound : List.of("from", "to")) {
					for (JsonElement coord : box.getAsJsonArray(bound)) {
						double v = coord.getAsDouble();
						assertTrue(v >= -16.0 && v <= 32.0, name + "_in_hand coordinate out of [-16, 32]: " + v);
					}
				}
				for (Map.Entry<String, JsonElement> face : box.getAsJsonObject("faces").entrySet()) {
					for (JsonElement uv : face.getValue().getAsJsonObject().getAsJsonArray("uv")) {
						double u = uv.getAsDouble();
						assertTrue(u >= 0.0 && u <= 16.0, name + "_in_hand UV out of [0, 16]: " + u);
					}
				}
			}
			assertTrue(hasGlow, name + "_in_hand should have at least one emissive rune/gem element");
		}
	}
}
