package dev.wildercord;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Renderers that pick a texture by name every frame look it up in a cache instead of building a new Identifier each time. */
class TextureIdCacheTest {
	private static final Path CLIENT = Path.of("src/client/java/dev/wildercord/client");

	@Test
	void perFrameTexturesAreCached() throws Exception {
		for (String file : List.of("GuiSpellCircle.java", "render/CordLayer.java", "render/RuneMarksLayer.java",
				"cosmetic/CordStyleLook.java", "auraworld/MasterRenderer.java")) {
			String source = Files.readString(CLIENT.resolve(file));
			assertTrue(source.contains("computeIfAbsent"), file + " no longer caches its textures");
			assertFalse(source.matches("(?s).*return Wildercord\\.id\\(\"textures/[^\"]*\" \\+.*"), file + " builds a texture id on every call");
			assertFalse(source.contains("Identifier skin = Wildercord.id(") || source.contains("Identifier texture = Wildercord.id("), file + " builds a texture id every frame");
		}
	}
}
