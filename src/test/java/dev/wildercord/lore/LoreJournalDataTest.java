package dev.wildercord.lore;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The journal's saved and synced forms: round trips, old saves, damaged saves and limits. */
class LoreJournalDataTest {
	private static LoreJournalData read(String json) {
		return LoreJournalData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
	}

	@Test
	void roundTripsThroughSaveAndSync() {
		LoreJournalData data = LoreJournalData.EMPTY.with("place:tomb").with("duelist:ember").with("said:duelist.ember.greet")
			.withQuests(Map.of("kiln_gate", LoreQuestRules.ACTIVE, "keeper_openings", LoreQuestRules.DONE));
		JsonElement saved = LoreJournalData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
		assertEquals(data, LoreJournalData.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow());
		ByteBuf buf = Unpooled.buffer();
		LoreJournalData.STREAM_CODEC.encode(buf, data);
		assertEquals(data, LoreJournalData.STREAM_CODEC.decode(buf));
		assertEquals(List.of("place:tomb", "duelist:ember", "said:duelist.ember.greet"), data.entries(), "entries keep the order they were found");
	}

	@Test
	void oldSavesAndDamagedFieldsReadAsEmpty() {
		assertEquals(LoreJournalData.EMPTY, read("{}"), "a save from before the journal");
		assertEquals(LoreJournalData.EMPTY, read("{\"entries\": 5, \"quests\": \"broken\"}"), "damaged fields fall back to empty");
		LoreJournalData partial = read("{\"entries\": [\"place:tomb\"], \"quests\": {\"kiln_gate\": 9, \"scout_wall\": 0, \"different_hands\": -1}}");
		assertEquals(List.of("place:tomb"), partial.entries());
		assertEquals(Map.of("kiln_gate", LoreQuestRules.DONE), partial.quests(), "stages clamp; unknown stages are not stored");
	}

	@Test
	void entriesAreUniqueBoundedAndImmutable() {
		LoreJournalData data = LoreJournalData.EMPTY.with("place:tomb");
		assertSame(data, data.with("place:tomb"), "a repeated entry changes nothing");
		assertSame(data, data.with(""));
		assertSame(data, data.with("x".repeat(LoreJournalData.MAX_ENTRY_LENGTH + 1)));
		assertTrue(data.has("place:tomb"));
		assertTrue(data.hasPrefix("place:"));
		assertFalse(data.hasPrefix("master:"));
		assertThrows(UnsupportedOperationException.class, () -> data.entries().add("x"));
		List<String> many = new ArrayList<>();
		for (int i = 0; i < LoreJournalData.MAX_ENTRIES + 50; i++) many.add("said:line" + i);
		many.add("said:line1");
		LoreJournalData full = new LoreJournalData(many, null);
		assertEquals(LoreJournalData.MAX_ENTRIES, full.entries().size());
		assertSame(full, full.with("place:new"), "a full journal stays as it is rather than failing");
		assertEquals(LoreQuestRules.UNKNOWN, full.stage("kiln_gate"));
		ByteBuf buf = Unpooled.buffer();
		LoreJournalData.STREAM_CODEC.encode(buf, full);
		assertEquals(full, LoreJournalData.STREAM_CODEC.decode(buf), "a full journal still syncs");
	}
}
