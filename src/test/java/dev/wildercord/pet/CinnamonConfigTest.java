package dev.wildercord.pet;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class CinnamonConfigTest {
	@Test void parsesCompleteConfiguration() {
		UUID id = UUID.randomUUID();
		var c = CinnamonConfig.parse("{\"owner\":\" " + id + " \",\"bell\":false}");
		assertEquals(id.toString(), c.owner()); assertEquals(id, c.ownerId()); assertFalse(c.bell());
	}
	@Test void preservesDefaultAndNamedModes() {
		assertEquals(CinnamonConfig.EMPTY, CinnamonConfig.parse("{\"owner\":\"\"}"));
		assertNull(CinnamonConfig.parse("{\"owner\":\"PlayerName\"}").ownerId());
		assertEquals("@singleplayer", CinnamonConfig.parse("{\"owner\":\"@singleplayer\"}").owner());
	}
	@Test void malformedRemainderCannotMutatePreviouslyParsedValue() {
		var previous = CinnamonConfig.parse("{\"owner\":\"original\",\"bell\":true}");
		for (String bell : new String[]{"{}", "[]", "null", "\"false\"", "42"}) {
			assertThrows(RuntimeException.class, () -> CinnamonConfig.parse("{\"owner\":\"replacement\",\"bell\":" + bell + "}"));
			assertEquals("original", previous.owner()); assertTrue(previous.bell());
		}
	}
	@Test void truncatedWritesAndInvalidOwnerNeverProduceAnAuthority() {
		for (String json : new String[]{"", "{", "{\"owner\":", "{}", "{\"owner\":null}", "{\"owner\":42}", "{\"owner\":true}", "[]"})
			assertThrows(RuntimeException.class, () -> CinnamonConfig.parse(json), json);
	}
}
