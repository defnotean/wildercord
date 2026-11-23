package dev.wildercord;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Effects that animate every tick or frame reuse what they looked up or built, instead of making it again each time. */
class ParticleFrameCacheTest {
	private static final Path CLIENT = Path.of("src/client/java/dev/wildercord/client");

	@Test
	void particlesLookUpTheirFramesOnce() throws Exception {
		for (String name : List.of("Camp", "Earth", "Life", "Void", "Material")) {
			String source = Files.readString(CLIENT.resolve("fx/" + name + "Particle.java"));
			assertTrue(source.contains("setSprite(frames["), name + "Particle no longer picks from its cached frames");
			assertFalse(source.contains("setSprite(SpellCircleParticle.particleSprite("), name + "Particle builds a sprite name every tick");
		}
	}

}
