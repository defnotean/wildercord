package dev.wildercord;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Minecraft 26.3 reads a loot table's modifiers from {@code "modifier"} and its conditions from
 * {@code "condition"}, each named by {@code "type"}, and silently ignores the older {@code "functions"}
 * and {@code "conditions"} keys. A table written the old way still loads, but a rune's set_components
 * never runs and every rune comes out blank (a Silent Rune), so none may use them.
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
			assertTrue(!json.contains("\"functions\"") && !json.contains("\"conditions\"") && !json.contains("\"function\""),
				table + " uses the pre-26.3 loot format, which 26.3 ignores");
		}
	}
}
