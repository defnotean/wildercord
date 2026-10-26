package dev.wildercord;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Cooldowns keyed on the server tick count forget their ticks when the server stops, because the count restarts. */
class CooldownRestartResetTest {
	@Test
	void tickCooldownsClearOnStop() throws Exception {
		for (String name : List.of("BellShard", "Rimescale", "CinderAntler")) {
			String source = Files.readString(Path.of("src/main/java/dev/wildercord/aura/world/" + name + ".java"));
			assertTrue(source.contains("SERVER_STOPPED.register(server -> { LAST.clear();"), name + " keeps last-trigger ticks across a server restart");
		}
	}
}
