package dev.wildercord;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Inn raids are keyed by dimension as well as the inn's middle, so the same spot in two dimensions is two inns. */
class InnRaidKeyTest {
	@Test
	void raidsAreKeyedByDimension() throws Exception {
		String source = Files.readString(Path.of("src/main/java/dev/wildercord/town/InnRaid.java"));
		assertTrue(source.contains("Map<Key, InnRaid> ACTIVE"), "raids are keyed by a dimension-aware key");
		assertTrue(source.contains("record Key(ResourceKey<Level> dimension, long centre)"), "the key holds the dimension");
		assertFalse(source.contains("ACTIVE.containsKey(centre.asLong())"), "no lookup by position alone");
	}
}
