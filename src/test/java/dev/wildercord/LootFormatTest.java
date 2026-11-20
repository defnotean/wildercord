package dev.wildercord;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Minecraft 26.2 reads a loot table's functions from {@code "functions"} and its conditions from
 * {@code "conditions"}, each named by {@code "function"} or {@code "condition"}, and silently ignores
 * the {@code "modifier"} key.
 */
class LootFormatTest {
	@Test
	void everyLootTableUsesTheCurrentFormat() throws IOException {
		List<Path> tables;
		try (Stream<Path> files = Files.walk(Path.of("src/main/resources/data/wildercord/loot_table"))) {
			tables = files.filter(p -> p.toString().endsWith(".json")).toList();
		}
		assertTrue(!tables.isEmpty(), "there should be loot tables to check");
		for (Path table : tables) {
			String json = Files.readString(table);
			assertTrue(!json.contains("\"modifier\""),
				table + " uses the 26.3 loot format, which 26.2 ignores");
		}
	}
}
