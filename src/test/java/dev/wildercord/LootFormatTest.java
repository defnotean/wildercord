package dev.wildercord;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies loot tables match the active Minecraft version's codec schema ({@code "functions"}/
 * {@code "conditions"} in 26.2 vs {@code "modifier"}/{@code "condition"} in 26.3), since silently
 * ignored keys yield blank runes.
 */
class LootFormatTest {
	@Test
	void everyLootTableUsesTheCurrentFormat() throws IOException {
		List<Path> tables;
		try (Stream<Path> files = Files.walk(Path.of("src/main/resources/data/wildercord/loot_table"))) {
			tables = files.filter(p -> p.toString().endsWith(".json")).toList();
		}
		assertTrue(!tables.isEmpty(), "there should be loot tables to check");
		boolean is262 = TestDataProfile.is26_2();
		for (Path table : tables) {
			String json = Files.readString(table);
			if (is262) {
				assertTrue(!json.contains("\"modifier\""),
					table + " uses the 26.3 loot format, which 26.2 ignores");
			} else {
				assertTrue(!json.contains("\"functions\"") && !json.contains("\"conditions\"") && !json.contains("\"function\""),
					table + " uses the pre-26.3 loot format, which 26.3 ignores");
			}
		}
	}
}
