package dev.wildercord.lore;

import com.mojang.serialization.JsonOps;
import dev.wildercord.aura.world.MastersRules;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Save compatibility and authored dialogue for the schools added after the original three. */
class MasterJournalIdentityTest {
	private static final List<String> SCHOOL_ORDER = List.of("ember", "gale", "stone", "rime", "thunder", "verdant",
		"hollow", "starlit", "hourglass", "crimson", "tide", "iron", "dune", "echo", "dawn", "venom");

	@Test
	void everyRegisteredSchoolUsesItsOwnVoice() {
		assertEquals(MastersRules.SCHOOLS, SCHOOL_ORDER.size());
		for (int school = 0; school < SCHOOL_ORDER.size(); school++) {
			String method = MastersRules.schoolId(school);
			assertTrue(MastersRules.knownSchool(school));
			assertEquals(SCHOOL_ORDER.get(school), method, "saved school number " + school);
			assertEquals("master." + method + ".victory", LoreEntries.line("master", method, "victory"));
			assertEquals("victory:" + method, LoreEntries.canonicalEntry("victory:school_" + school));
		}
	}

	@Test
	void oldVictoriesNormalizeInPlaceWithoutChangingQuestStages() {
		Map<String, Integer> quests = Map.of("masters_ledger", LoreQuestRules.DONE, "scout_wall", LoreQuestRules.ACTIVE);
		LoreJournalData data = new LoreJournalData(List.of("place:tomb", "victory:school_3", "said:master.any.victory",
			"victory:rime", "victory:school_15"), quests);
		assertEquals(List.of("place:tomb", "victory:rime", "said:master.any.victory", "victory:venom"), data.entries());
		assertEquals(quests, data.quests(), "migration must not reopen completed quests or pay rewards");
		assertSame(data, data.with("victory:school_3"), "legacy writes deduplicate against canonical entries");
		assertSame(data, data.with("victory:rime"));
		assertEquals(data, new LoreJournalData(data.entries(), data.quests()), "migration is idempotent");
	}

	@Test
	void persistedOldSaveAndOwnerSyncUseCanonicalEntries() {
		var saved = com.google.gson.JsonParser.parseString("""
			{"entries":["victory:school_10","victory:school_13"],"quests":{"masters_ledger":2}}
			""");
		LoreJournalData loaded = LoreJournalData.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow();
		assertEquals(List.of("victory:tide", "victory:echo"), loaded.entries());
		assertEquals(LoreQuestRules.DONE, loaded.stage("masters_ledger"));
		var buffer = Unpooled.buffer();
		try {
			LoreJournalData.STREAM_CODEC.encode(buffer, loaded);
			assertEquals(loaded, LoreJournalData.STREAM_CODEC.decode(buffer));
		} finally {
			buffer.release();
		}
	}

	@Test
	void migrationPreservesFullJournalsAndUnknownEntries() {
		var entries = new java.util.ArrayList<String>();
		for (int i = 0; i < LoreJournalData.MAX_ENTRIES - 1; i++) entries.add("addon:entry_" + i);
		entries.add("victory:school_14");
		LoreJournalData full = new LoreJournalData(entries, Map.of());
		assertEquals(LoreJournalData.MAX_ENTRIES, full.entries().size());
		assertEquals("victory:dawn", full.entries().getLast(), "full journals still repair existing entries");
		for (String entry : List.of("victory:school_16", "victory:school_-1", "victory:school_03",
			"victory:school_addon", "victory:school_9999999999999999", "said:master.any.victory")) {
			assertEquals(entry, LoreEntries.canonicalEntry(entry), "do not invent an identity for " + entry);
		}
		assertNull(LoreEntries.canonicalEntry(null));
		assertEquals("school_16", MastersRules.schoolId(16));
		assertEquals("school_-1", MastersRules.schoolId(-1));
	}
}
