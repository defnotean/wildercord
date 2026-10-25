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
			assertFalse(source.matches("(?s).*setSprite\(SpellCircleParticle\.particleSprite\(.*"), name + "Particle builds a sprite name every tick");
		}
	}

	@Test
	void theSerpentReusesItsRingBuffers() throws Exception {
		String source = Files.readString(CLIENT.resolve("auraworld/SerpentRenderer.java"));
		assertFalse(source.contains("new float[n][SIDES+1][]"), "the serpent allocates a ring of arrays every frame");
		assertTrue(source.contains("if(rings.length<size)"), "the serpent's ring buffer is no longer reused");
	}
}
