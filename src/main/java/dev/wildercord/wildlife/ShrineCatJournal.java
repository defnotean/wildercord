package dev.wildercord.wildlife;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.saveddata.SavedData;
import dev.wildercord.compat.McSavedData;
import dev.wildercord.compat.McSavedDataType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Durable per-owner identity registry for tamed {@link ShrineCat} companions ({@code Yuki}, {@code Odie},
 * {@code Freya}, {@code Kuro}), ensuring at most one canonical body exists per owner and variant.
 */
final class ShrineCatJournal extends SavedData {
	record Entry(UUID entityId, String dimension, BlockPos lastPos, boolean sitting, boolean loafing,
			boolean retired, boolean transferring) {
		static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
			UUIDUtil.CODEC.fieldOf("entity").forGetter(Entry::entityId),
			Codec.STRING.fieldOf("dimension").forGetter(Entry::dimension),
			BlockPos.CODEC.fieldOf("last_pos").forGetter(Entry::lastPos),
			Codec.BOOL.optionalFieldOf("sitting", false).forGetter(Entry::sitting),
			Codec.BOOL.optionalFieldOf("loafing", false).forGetter(Entry::loafing),
			Codec.BOOL.optionalFieldOf("retired", false).forGetter(Entry::retired),
			Codec.BOOL.optionalFieldOf("transferring", false).forGetter(Entry::transferring)
		).apply(i, Entry::new));

		static Entry snapshot(ShrineCat cat) {
			return new Entry(
				cat.getUUID(),
				cat.level().dimension().location().toString(),
				cat.blockPosition().immutable(),
				cat.isOrderedToSit(),
				cat.isLoafing(),
				false,
				false
			);
		}

		Entry beginTransfer(ShrineCat cat) {
			return new Entry(
				entityId,
				cat.level().dimension().location().toString(),
				cat.blockPosition().immutable(),
				cat.isOrderedToSit(),
				cat.isLoafing(),
				false,
				true
			);
		}

		Entry retire(ShrineCat cat) {
			return new Entry(
				entityId,
				cat.level().dimension().location().toString(),
				cat.blockPosition().immutable(),
				cat.isOrderedToSit(),
				cat.isLoafing(),
				true,
				false
			);
		}
	}

	private static final Codec<Map<String, Entry>> VARIANT_MAP_CODEC = Codec.unboundedMap(Codec.STRING, Entry.CODEC);
	private static final Codec<ShrineCatJournal> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.unboundedMap(UUIDUtil.STRING_CODEC, VARIANT_MAP_CODEC)
			.optionalFieldOf("companions", Map.of())
			.forGetter(ShrineCatJournal::encodeEntries)
	).apply(i, ShrineCatJournal::new));
	private static final McSavedDataType<ShrineCatJournal> TYPE = new McSavedDataType<>("shrine_cat_companions",
		ShrineCatJournal::new,
		CODEC
	);

	private static final Map<UUID, EnumMap<ShrineCatRules.Variant, ShrineCat>> LIVE = new HashMap<>();

	private final Map<UUID, EnumMap<ShrineCatRules.Variant, Entry>> entries = new HashMap<>();

	ShrineCatJournal() {}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		return (CompoundTag) CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), this)
				.result().orElseThrow();
	}

	private ShrineCatJournal(Map<UUID, Map<String, Entry>> raw) {
		for (var e : raw.entrySet()) {
			EnumMap<ShrineCatRules.Variant, Entry> byVar = new EnumMap<>(ShrineCatRules.Variant.class);
			for (var ve : e.getValue().entrySet()) {
				byVar.put(ShrineCatRules.Variant.byKey(ve.getKey()), ve.getValue());
			}
			if (!byVar.isEmpty()) {
				entries.put(e.getKey(), byVar);
			}
		}
	}

	private Map<UUID, Map<String, Entry>> encodeEntries() {
		Map<UUID, Map<String, Entry>> out = new HashMap<>(entries.size());
		for (var e : entries.entrySet()) {
			Map<String, Entry> byKey = new HashMap<>(e.getValue().size());
			for (var ve : e.getValue().entrySet()) {
				byKey.put(ve.getKey().key, ve.getValue());
			}
			out.put(e.getKey(), byKey);
		}
		return out;
	}

	static void init() {
		ServerLifecycleEvents.SERVER_STARTING.register(server -> LIVE.clear());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> LIVE.clear());
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof ShrineCat cat) {
				onEntityLoad(cat, level);
			}
		});
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
			if (entity instanceof ShrineCat cat) {
				onEntityUnload(cat, level);
			}
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ShrineCat cat && entity.level() instanceof ServerLevel level) {
				onAfterDeath(cat, level.getServer());
			}
		});
	}

	static ShrineCatJournal of(MinecraftServer server) {
		return McSavedData.computeIfAbsent(server.overworld().getDataStorage(), TYPE);
	}

	Entry get(UUID owner, ShrineCatRules.Variant variant) {
		EnumMap<ShrineCatRules.Variant, Entry> byVar = entries.get(owner);
		return byVar == null ? null : byVar.get(variant);
	}

	void put(UUID owner, ShrineCatRules.Variant variant, Entry entry) {
		EnumMap<ShrineCatRules.Variant, Entry> byVar = entries.computeIfAbsent(owner, k -> new EnumMap<>(ShrineCatRules.Variant.class));
		if (!entry.equals(byVar.put(variant, entry))) {
			setDirty();
		}
	}

	static UUID ownerUuid(ShrineCat cat) {
		return cat.getOwnerUUID();
	}

	static boolean canTame(MinecraftServer server, UUID owner, ShrineCatRules.Variant variant) {
		pruneDeadLive(owner, variant);
		Entry entry = of(server).get(owner, variant);
		return ShrineCatIdentityRules.canTameVariant(entry == null ? null : entry.entityId(), entry != null && entry.retired());
	}

	static void registerTamed(MinecraftServer server, UUID owner, ShrineCat cat) {
		ShrineCatRules.Variant variant = cat.variant();
		LIVE.computeIfAbsent(owner, k -> new EnumMap<>(ShrineCatRules.Variant.class)).put(variant, cat);
		of(server).put(owner, variant, Entry.snapshot(cat));
	}

	static ShrineCat liveCat(UUID owner, ShrineCatRules.Variant variant) {
		pruneDeadLive(owner, variant);
		EnumMap<ShrineCatRules.Variant, ShrineCat> byVar = LIVE.get(owner);
		return byVar == null ? null : byVar.get(variant);
	}

	static void snapshotLive(MinecraftServer server, UUID owner, ShrineCat cat) {
		if (!cat.isTame() || cat.isRemoved()) return;
		ShrineCatJournal journal = of(server);
		Entry entry = journal.get(owner, cat.variant());
		if (entry == null || entry.entityId().equals(cat.getUUID())) {
			journal.put(owner, cat.variant(), Entry.snapshot(cat));
		}
	}

	private static void pruneDeadLive(UUID owner, ShrineCatRules.Variant variant) {
		EnumMap<ShrineCatRules.Variant, ShrineCat> byVar = LIVE.get(owner);
		if (byVar == null) return;
		ShrineCat live = byVar.get(variant);
		if (live != null && (!live.isAlive() || live.isRemoved())) {
			byVar.remove(variant);
		}
	}

	private static boolean removeLiveIfSame(UUID owner, ShrineCatRules.Variant variant, ShrineCat cat) {
		EnumMap<ShrineCatRules.Variant, ShrineCat> byVar = LIVE.get(owner);
		if (byVar == null) return false;
		if (byVar.get(variant) == cat) {
			byVar.remove(variant);
			return true;
		}
		return false;
	}

	private static void onEntityLoad(ShrineCat cat, ServerLevel level) {
		if (!cat.isTame()) return;
		UUID owner = ownerUuid(cat);
		if (owner == null) return;

		MinecraftServer server = level.getServer();
		ShrineCatJournal journal = of(server);
		ShrineCatRules.Variant variant = cat.variant();
		Entry entry = journal.get(owner, variant);

		ShrineCatIdentityRules.LoadResolution res = ShrineCatIdentityRules.resolveLoadedBody(
			cat.getUUID(),
			entry == null ? null : entry.entityId(),
			entry != null && entry.retired(),
			entry != null && entry.transferring()
		);
		if (res == ShrineCatIdentityRules.LoadResolution.DISCARD_DUPLICATE) {
			cat.discard();
			return;
		}

		EnumMap<ShrineCatRules.Variant, ShrineCat> byVar = LIVE.computeIfAbsent(owner, k -> new EnumMap<>(ShrineCatRules.Variant.class));
		ShrineCat previous = byVar.get(variant);
		if (previous != null && previous != cat && !previous.isRemoved()) {
			if (res == ShrineCatIdentityRules.LoadResolution.ADOPT_TRANSFER) {
				previous.discard();
			} else {
				cat.discard();
				return;
			}
		}
		byVar.put(variant, cat);
		journal.put(owner, variant, Entry.snapshot(cat));
	}

	private static void onEntityUnload(ShrineCat cat, ServerLevel level) {
		if (!cat.isTame()) return;
		UUID owner = ownerUuid(cat);
		if (owner == null) return;

		ShrineCatRules.Variant variant = cat.variant();
		boolean wasLive = removeLiveIfSame(owner, variant, cat);
		ShrineCatJournal journal = of(level.getServer());
		Entry entry = journal.get(owner, variant);
		if (entry == null || !entry.entityId().equals(cat.getUUID())) {
			return;
		}
		Entity.RemovalReason reason = cat.getRemovalReason();
		if (reason == Entity.RemovalReason.CHANGED_DIMENSION) {
			journal.put(owner, variant, entry.beginTransfer(cat));
		} else if (wasLive && (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED)
				&& ShrineCatIdentityRules.mayRetire(cat.getUUID(), entry.entityId(), entry.retired())) {
			journal.put(owner, variant, entry.retire(cat));
		} else if (!entry.retired() && !entry.transferring()) {
			journal.put(owner, variant, Entry.snapshot(cat));
		}
	}

	private static void onAfterDeath(ShrineCat cat, MinecraftServer server) {
		if (!cat.isTame()) return;
		UUID owner = ownerUuid(cat);
		if (owner == null) return;

		ShrineCatRules.Variant variant = cat.variant();
		removeLiveIfSame(owner, variant, cat);
		ShrineCatJournal journal = of(server);
		Entry entry = journal.get(owner, variant);
		if (entry != null && ShrineCatIdentityRules.mayRetire(cat.getUUID(), entry.entityId(), entry.retired())) {
			journal.put(owner, variant, entry.retire(cat));
		}
	}
}
