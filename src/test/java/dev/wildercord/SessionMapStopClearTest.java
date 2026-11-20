package dev.wildercord;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Per-player maps that outlive a world would leak memory and carry stale picks or game times into the next one. */
class SessionMapStopClearTest {
	@Test
	void mapsClearOnStop() throws Exception {
		var maps = Map.of(
				"cooking/CampPotBlock", "ServerState.resetOnStop(CHOSEN)",
				"town/BountyBoardBlock", "ServerState.resetOnStop(SHOWN)",
				"aura/arts/MethodsAFlavours", "ServerState.resetOnStop(SUNDER_ENDS)",
				"aura/world/TrainingGrounds", "ServerState.resetOnStop(SAMPLES)",
				"world/dungeons/DungeonWards", "ServerState.resetOnStop(TOLD)");
		for (var e : maps.entrySet()) {
			String source = Files.readString(Path.of("src/main/java/dev/wildercord/" + e.getKey() + ".java"));
			assertTrue(source.contains(e.getValue()), e.getKey() + " keeps its map after the server stops");
		}
		// The two boards' picks, ward notices and training samples are per player, so a leaver's entry should go with them.
		assertTrue(Files.readString(Path.of("src/main/java/dev/wildercord/cooking/CampPotBlock.java")).contains("ServerState.forgetOnDisconnect(CHOSEN)"));
		assertTrue(Files.readString(Path.of("src/main/java/dev/wildercord/town/BountyBoardBlock.java")).contains("ServerState.forgetOnDisconnect(SHOWN)"));
		assertTrue(Files.readString(Path.of("src/main/java/dev/wildercord/aura/world/TrainingGrounds.java")).contains("ServerState.forgetOnDisconnect(SAMPLES)"));
		assertTrue(Files.readString(Path.of("src/main/java/dev/wildercord/world/dungeons/DungeonWards.java")).contains("ServerState.forgetOnDisconnect(TOLD)"));
		assertTrue(Files.readString(Path.of("src/main/java/dev/wildercord/cooking/Meals.java")).contains("CampPotBlock.init();"));
		assertTrue(Files.readString(Path.of("src/main/java/dev/wildercord/town/Town.java")).contains("BountyBoardBlock.init();"));
		assertTrue(Files.readString(Path.of("src/main/java/dev/wildercord/aura/arts/MethodArts.java")).contains("MethodsAFlavours.init();"));

		String delve = Files.readString(Path.of("src/main/java/dev/wildercord/cast/DelveEffects.java"));
		assertTrue(delve.contains("MARKS.clear()") && delve.contains("RETURNING.clear()"));
		String explorer = Files.readString(Path.of("src/main/java/dev/wildercord/cast/ExplorerEffects.java"));
		assertTrue(explorer.contains("SHELL_BEFORE.clear()") && explorer.contains("DelveEffects.clear()") && explorer.contains("WayfarerEffects.clear()"));
		String wayfarer = Files.readString(Path.of("src/main/java/dev/wildercord/cast/WayfarerEffects.java"));
		assertTrue(wayfarer.contains("LOCATED.clear()") && wayfarer.contains("THRIFT.clear()"));
		String effects = Files.readString(Path.of("src/main/java/dev/wildercord/cast/Effects.java"));
		for (String map : java.util.List.of("RENT.clear()", "BRACED.clear()", "GLIDES.clear()", "CHILLS.clear()", "HEXED.clear()")) {
			assertTrue(effects.contains(map), "Effects missing " + map + " on stop");
		}
		String escorts = Files.readString(Path.of("src/main/java/dev/wildercord/town/BountyEscorts.java"));
		assertTrue(escorts.contains("ROADS.clear()") && escorts.contains("LOADED.clear()"));
	}
}
