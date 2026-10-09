package dev.wildercord.pet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Identity and recovery journal. An absent registered body is never permission to manufacture another one. */
final class CinnamonJournal extends SavedData {
	static final int MAX_OWNERS = 64;
	record Care(boolean sitting, boolean bow, double scale, long growthUntil, float damage, long recoveryUntil, long immuneUntil) {
		static final Codec<Care> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.BOOL.optionalFieldOf("sitting", false).forGetter(Care::sitting),
			Codec.BOOL.optionalFieldOf("bow", false).forGetter(Care::bow),
			Codec.DOUBLE.optionalFieldOf("scale", 1D).forGetter(Care::scale),
			Codec.LONG.optionalFieldOf("growth_until", 0L).forGetter(Care::growthUntil),
			Codec.FLOAT.optionalFieldOf("damage", 0F).forGetter(Care::damage),
			Codec.LONG.optionalFieldOf("recovery_until", 0L).forGetter(Care::recoveryUntil),
			Codec.LONG.optionalFieldOf("immune_until", 0L).forGetter(Care::immuneUntil)
		).apply(i, Care::new));
		static Care of(CinnamonDog dog) {
			return new Care(dog.savedSitting(), dog.wearingBow(), dog.growthScale(), dog.growthExpiresAt(),
				dog.accumulatedDamage(), dog.recoveryExpiresAt(), dog.damageImmuneUntil());
		}
		void restore(CinnamonDog dog) { dog.restoreState(sitting, bow, scale, growthUntil, damage, recoveryUntil, immuneUntil); }
	}
	record Entry(UUID entity, String dimension, int chunkX, int chunkZ, Care care, long whistleUntil, boolean retired) {
		static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
			UUIDUtil.CODEC.fieldOf("entity").forGetter(Entry::entity),
			Codec.STRING.fieldOf("dimension").forGetter(Entry::dimension),
			Codec.INT.fieldOf("chunk_x").forGetter(Entry::chunkX),
			Codec.INT.fieldOf("chunk_z").forGetter(Entry::chunkZ),
			Care.CODEC.fieldOf("care").forGetter(Entry::care),
			Codec.LONG.optionalFieldOf("whistle_until", 0L).forGetter(Entry::whistleUntil),
			Codec.BOOL.optionalFieldOf("retired", false).forGetter(Entry::retired)
		).apply(i, Entry::new));
		Entry snapshot(CinnamonDog dog) {
			return new Entry(entity, dog.level().dimension().identifier().toString(), dog.chunkPosition().x(),
				dog.chunkPosition().z(), Care.of(dog), whistleUntil, retired);
		}
		Entry cooldown(long until) { return new Entry(entity, dimension, chunkX, chunkZ, care, until, retired); }
		Entry retire() { return new Entry(entity, dimension, chunkX, chunkZ, care, whistleUntil, true); }
	}
	private static final Codec<CinnamonJournal> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.unboundedMap(UUIDUtil.STRING_CODEC, Entry.CODEC).optionalFieldOf("companions", Map.of()).forGetter(j -> j.entries)
	).apply(i, CinnamonJournal::new));
	private static final SavedDataType<CinnamonJournal> TYPE = new SavedDataType<>(Wildercord.id("cinnamon_companions"), CinnamonJournal::new, CODEC, null);
	private final Map<UUID, Entry> entries = new HashMap<>();
	CinnamonJournal() {}
	private CinnamonJournal(Map<UUID, Entry> entries) { this.entries.putAll(entries); }
	static CinnamonJournal of(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(TYPE); }
	Entry get(UUID owner) { return entries.get(owner); }
	boolean canRegister(UUID owner) { return entries.containsKey(owner) || entries.size() < MAX_OWNERS; }
	void put(UUID owner, Entry entry) {
		if (!canRegister(owner)) return;
		if (!entry.equals(entries.put(owner, entry))) setDirty();
	}
	/** Used only when initial addFreshEntity positively failed before a body ever existed. */
	void cancelInitial(UUID owner, UUID entity) {
		Entry e = entries.get(owner);
		if (e != null && e.entity.equals(entity)) { entries.remove(owner); setDirty(); }
	}
}
