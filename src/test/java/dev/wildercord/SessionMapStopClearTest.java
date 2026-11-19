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
				"aura/arts/MethodsAFlavours", "ServerState.resetOnStop(SUNDER_ENDS)");
		for (var e : maps.entrySet()) {
			String source = Files.readString(Path.of("src/main/java/dev/wildercord/" + e.getKey() + ".java"));
			assertTrue(source.contains(e.getValue()), e.getKey() + " keeps its map after the server stops");
		}
		// The two boards' picks are per player, so a leaver's entry should go with them.
		assertTrue(Files.readString(Path.of("src/main/java/dev/wildercord/cooking/CampPotBlock.java")).contains("ServerState.forgetOnDisconnect(CHOSEN)"));
		assertTrue(Files.readString(Path.of("src/main/java/dev/wildercord/town/BountyBoardBlock.java")).contains("ServerState.forgetOnDisconnect(SHOWN)"));
		assertTrue(Files.readString(Path.of("src/main/java/dev/wildercord/cooking/Meals.java")).contains("CampPotBlock.init();"));
		assertTrue(Files.readString(Path.of("src/main/java/dev/wildercord/town/Town.java")).contains("BountyBoardBlock.init();"));
		assertTrue(Files.readString(Path.of("src/main/java/dev/wildercord/aura/arts/MethodArts.java")).contains("MethodsAFlavours.init();"));
	}
}
