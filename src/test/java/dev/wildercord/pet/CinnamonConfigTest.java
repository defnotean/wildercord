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
		assertEquals(CinnamonConfig.EMPTY, CinnamonConfig.parse("{\"owners\":[]}"));
		assertNull(CinnamonConfig.parse("{\"owner\":\"PlayerName\"}").ownerId());
		assertEquals("@singleplayer", CinnamonConfig.parse("{\"owner\":\"@singleplayer\"}").owner());
	}
	@Test void parsesAndDeduplicatesMultipleOwners() {
		UUID id1 = UUID.randomUUID();
		UUID id2 = UUID.randomUUID();
		var c = CinnamonConfig.parse("{\"owner\":\"defnotean\",\"owners\":[\"MyPPSad\",\"DEFNOTEAN\",\"" + id1 + "\",\"\"]}");
		assertEquals(3, c.owners().size());
		assertEquals("defnotean", c.owner());
		assertTrue(c.matches(id2, "defnotean", false));
		assertTrue(c.matches(id2, "myppSad", false));
		assertTrue(c.matches(id1, "someoneElse", false));
		assertFalse(c.matches(id2, "stranger", false));
	}
	@Test void rejectsOversizedOrMalformedOwnerLists() {
		StringBuilder many = new StringBuilder("{\"owners\":[");
		for (int i = 0; i <= CinnamonJournal.MAX_OWNERS; i++) {
			if (i > 0) many.append(',');
			many.append("\"player").append(i).append('"');
		}
		many.append("]}");
		assertThrows(RuntimeException.class, () -> CinnamonConfig.parse(many.toString()));
		for (String bad : new String[]{"\"notAnArray\"", "{}", "[42]", "[null]"}) {
			assertThrows(RuntimeException.class, () -> CinnamonConfig.parse("{\"owners\":" + bad + "}"));
		}
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
