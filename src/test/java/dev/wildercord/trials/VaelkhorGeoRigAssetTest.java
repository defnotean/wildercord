package dev.wildercord.trials;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies the generated Vaelkhor Bedrock rig, animations, textures, and worldgen files stay intact. */
class VaelkhorGeoRigAssetTest {
	private static Path res(String rel) {
		Path p = Path.of("src/main/resources", rel);
		return Files.exists(p) ? p : Path.of("../src/main/resources", rel);
	}

	@Test
	void geoAndAnimationClipsMatch40BoneModel() throws IOException {
		JsonObject geo = JsonParser.parseString(Files.readString(res("assets/wildercord/models/entity/trials/vaelkhor.geo.json"))).getAsJsonObject();
		var bones = geo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
		assertEquals(40, bones.size(), "Expected 40 bones in Vaelkhor rig");
		int cubes = 0;
		for (var el : bones) {
			var b = el.getAsJsonObject();
			if (b.has("cubes")) cubes += b.getAsJsonArray("cubes").size();
		}
		assertEquals(211, cubes, "Expected 211 cubes in Vaelkhor rig");

		JsonObject anims = JsonParser.parseString(Files.readString(res("assets/wildercord/animations/entity/trials/vaelkhor.animation.json")))
			.getAsJsonObject().getAsJsonObject("animations");
		for (String clip : List.of(
			"animation.vaelkhor.idle",
			"animation.vaelkhor.stride",
			"animation.vaelkhor.halberd_cleave",
			"animation.vaelkhor.bell_toll_conduit",
			"animation.vaelkhor.leyline_cataclysm"
		)) {
			assertTrue(anims.has(clip), "Missing animation clip: " + clip);
		}
		assertTrue(Files.exists(res("assets/wildercord/textures/entity/trials/vaelkhor.png")));
		assertTrue(Files.exists(res("assets/wildercord/textures/entity/trials/vaelkhor_glow.png")));
		assertTrue(Files.exists(res("data/wildercord/worldgen/structure/trial_leyline_sanctum.json")));
	}
}
