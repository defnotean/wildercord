package dev.wildercord.lore;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * One player's lore journal: entries in the order they were found, and each discovery quest's stage.
 * Pure data with its codecs, so saves and sync can be unit tested. Missing or broken fields read as empty,
 * so a save from before the journal opens with an empty journal.
 */
public record LoreJournalData(List<String> entries, Map<String, Integer> quests) {
	public static final int MAX_ENTRIES = 512;
	public static final int MAX_ENTRY_LENGTH = 96;
	public static final int MAX_QUESTS = 64;
	public static final LoreJournalData EMPTY = new LoreJournalData(List.of(), Map.of());

	public LoreJournalData {
		LinkedHashSet<String> kept = new LinkedHashSet<>();
		if (entries != null) {
			for (String entry : entries) {
				if (entry != null && !entry.isEmpty() && entry.length() <= MAX_ENTRY_LENGTH && kept.size() < MAX_ENTRIES) kept.add(entry);
			}
		}
		entries = List.copyOf(kept);
		TreeMap<String, Integer> stages = new TreeMap<>();
		if (quests != null) {
			for (Map.Entry<String, Integer> quest : quests.entrySet()) {
				if (quest.getKey() == null || quest.getKey().isEmpty() || quest.getKey().length() > 48 || quest.getValue() == null) continue;
				int stage = Math.clamp(quest.getValue(), LoreQuestRules.UNKNOWN, LoreQuestRules.DONE);
				if (stage != LoreQuestRules.UNKNOWN && stages.size() < MAX_QUESTS) stages.put(quest.getKey(), stage);
			}
		}
		quests = java.util.Collections.unmodifiableMap(stages);
	}

	public static final Codec<LoreJournalData> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.STRING.listOf().lenientOptionalFieldOf("entries", List.of()).forGetter(LoreJournalData::entries),
		Codec.unboundedMap(Codec.STRING, Codec.INT).lenientOptionalFieldOf("quests", Map.of()).forGetter(LoreJournalData::quests)
	).apply(i, LoreJournalData::new));

	public static final StreamCodec<ByteBuf, LoreJournalData> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.stringUtf8(MAX_ENTRY_LENGTH).apply(ByteBufCodecs.list(MAX_ENTRIES)), LoreJournalData::entries,
		ByteBufCodecs.map(HashMap::new, ByteBufCodecs.stringUtf8(48), ByteBufCodecs.VAR_INT, MAX_QUESTS), d -> new HashMap<>(d.quests()),
		LoreJournalData::new);

	public boolean has(String entry) {
		return entries.contains(entry);
	}

	public boolean hasPrefix(String prefix) {
		for (String entry : entries) if (entry.startsWith(prefix)) return true;
		return false;
	}

	/** The journal with {@code entry} written at the end, or this journal if it is already there (or the journal is full). */
	public LoreJournalData with(String entry) {
		if (entry == null || entry.isEmpty() || entry.length() > MAX_ENTRY_LENGTH || has(entry) || entries.size() >= MAX_ENTRIES) return this;
		List<String> next = new ArrayList<>(entries);
		next.add(entry);
		return new LoreJournalData(next, quests);
	}

	public int stage(String quest) {
		return quests.getOrDefault(quest, LoreQuestRules.UNKNOWN);
	}

	public LoreJournalData withQuests(Map<String, Integer> next) {
		return new LoreJournalData(entries, next);
	}
}
