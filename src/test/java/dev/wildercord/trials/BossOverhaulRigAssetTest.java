package dev.wildercord.trials;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies every Milestone 3/4/5 Bedrock rig (.geo.json + .animation.json) and its clips load from classpath resources. */
class BossOverhaulRigAssetTest {
	private static JsonObject readJson(String resourcePath) throws Exception {
		try (var stream = BossOverhaulRigAssetTest.class.getResourceAsStream(resourcePath)) {
			assertNotNull(stream, "Missing classpath resource: " + resourcePath);
			return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	@Test
	void allSixBossRigsHaveValidGeoAndExpectedClips() throws Exception {
		List<String[]> specs = List.of(
			new String[]{"trials/vaelkhor", "animation.vaelkhor.idle", "animation.vaelkhor.halberd_cleave", "animation.vaelkhor.leyline_cataclysm"},
			new String[]{"trials/kharvok", "animation.kharvok.idle", "animation.kharvok.anvil_shatter", "animation.kharvok.crucible_meltdown"},
			new String[]{"trials/nyxara", "animation.nyxara.idle", "animation.nyxara.crescent_sever", "animation.nyxara.total_eclipse"},
			new String[]{"overhaul/archivist", "animation.archivist.idle", "animation.archivist.cast", "animation.archivist.rewrite", "animation.archivist.trick_slam"},
			new String[]{"overhaul/thalassor", "animation.thalassor.swim_idle", "animation.thalassor.bite_lunge", "animation.thalassor.maelstrom_spin", "animation.thalassor.hydro_beam"},
			new String[]{"overhaul/cinder_warden", "animation.cinder_warden.idle", "animation.cinder_warden.walk", "animation.cinder_warden.hammerfall", "animation.cinder_warden.flare_roar"}
		);

		for (String[] spec : specs) {
			String stem = spec[0];
			JsonObject geo = readJson("/assets/wildercord/models/entity/" + stem + ".geo.json");
			assertEquals("1.12.0", geo.get("format_version").getAsString(), stem + " geo format_version");
			JsonArray geos = geo.getAsJsonArray("minecraft:geometry");
			assertNotNull(geos, stem + " minecraft:geometry");
			assertTrue(geos.size() >= 1, stem + " geometry empty");
			JsonArray bones = geos.get(0).getAsJsonObject().getAsJsonArray("bones");
			assertTrue(bones.size() >= 20, stem + " expected >=20 bones, got " + bones.size());

			JsonObject animRoot = readJson("/assets/wildercord/animations/entity/" + stem + ".animation.json");
			JsonObject clips = animRoot.getAsJsonObject("animations");
			assertNotNull(clips, stem + " animations object");
			for (int i = 1; i < spec.length; i++) {
				JsonElement clip = clips.get(spec[i]);
				assertNotNull(clip, stem + " missing clip: " + spec[i]);
				assertTrue(clip.getAsJsonObject().get("animation_length").getAsDouble() > 0.0, stem + " clip zero length: " + spec[i]);
			}
		}
	}
}
