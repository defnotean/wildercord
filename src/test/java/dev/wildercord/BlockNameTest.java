package dev.wildercord;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Every block the mod draws has a name, so the debug screen and block-info mods never show a raw key. */
class BlockNameTest {
	@Test
	void everyBlockWithABlockstateHasAName() throws Exception {
		var language = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/wildercord/lang/en_us.json"))).getAsJsonObject();
		try (var paths = Files.list(Path.of("src/main/resources/assets/wildercord/blockstates"))) {
			for (var path : paths.toList()) {
				String block = path.getFileName().toString().replace(".json", "");
				assertTrue(language.has("block.wildercord." + block), block + " has no block.wildercord name");
			}
		}
	}
}
