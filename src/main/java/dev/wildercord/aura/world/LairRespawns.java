package dev.wildercord.aura.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.cast.BossRules;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Persists pending respawns for defeated lair guardians so players returning to a cleared lair
 * after {@link BossRules#BOSS_RESPAWN_TICKS} encounter a fresh guardian bound to the same arena.
 */
public final class LairRespawns extends SavedData {
	private static final int CHECK_INTERVAL = 40;
	private static final double WATCH_RANGE_SQR = 96.0 * 96.0;
	private static final double EXISTING_RANGE = 48.0;

	public record Entry(
		String bossId,
		BlockPos home,
		float yaw,
		BlockPos boundsMin,
		BlockPos boundsMax,
		int floorY,
		float anchor,
		BlockPos hook,
		List<BlockPos> extraA,
		List<BlockPos> extraB,
		long respawnAt
	) {
		static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("boss").forGetter(Entry::bossId),
			BlockPos.CODEC.fieldOf("home").forGetter(Entry::home),
			Codec.FLOAT.fieldOf("yaw").forGetter(Entry::yaw),
			BlockPos.CODEC.fieldOf("bounds_min").forGetter(Entry::boundsMin),
			BlockPos.CODEC.fieldOf("bounds_max").forGetter(Entry::boundsMax),
			Codec.INT.fieldOf("floor_y").forGetter(Entry::floorY),
			Codec.FLOAT.fieldOf("anchor").forGetter(Entry::anchor),
			BlockPos.CODEC.fieldOf("hook").forGetter(Entry::hook),
			BlockPos.CODEC.listOf().fieldOf("extra_a").forGetter(Entry::extraA),
			BlockPos.CODEC.listOf().fieldOf("extra_b").forGetter(Entry::extraB),
			Codec.LONG.fieldOf("respawn_at").forGetter(Entry::respawnAt)
		).apply(i, Entry::new));

		public static Entry simple(String bossId, BlockPos home, float yaw, BlockPos min, BlockPos max, long respawnAt) {
			return new Entry(bossId, home.immutable(), yaw, min.immutable(), max.immutable(), 0, 0.0F, BlockPos.ZERO, List.of(), List.of(), respawnAt);
		}
	}

	private static final Codec<LairRespawns> CODEC = Entry.CODEC.listOf().xmap(LairRespawns::new, LairRespawns::entries);
	private static final SavedDataType<LairRespawns> TYPE = new SavedDataType<>(Wildercord.id("lair_respawns"), LairRespawns::new, CODEC, null);

	private final List<Entry> pending = new ArrayList<>();

	public LairRespawns() {}

	private LairRespawns(List<Entry> entries) {
		pending.addAll(entries);
	}

	private List<Entry> entries() {
		return List.copyOf(pending);
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % CHECK_INTERVAL != 0) {
				return;
			}
			for (ServerLevel level : server.getAllLevels()) {
				tickLevel(level);
			}
		});
	}

	/** Schedules a bound guardian to respawn in its lair after {@link BossRules#BOSS_RESPAWN_TICKS}. */
	public static void schedule(ServerLevel level, Entry entry) {
		LairRespawns data = level.getDataStorage().computeIfAbsent(TYPE);
		data.pending.removeIf(existing -> existing.bossId().equals(entry.bossId()) && existing.home().equals(entry.home()));
		data.pending.add(entry);
		data.setDirty();
	}

	/** Number of pending lair respawns in {@code level} (for tests). */
	public static int pendingCount(ServerLevel level) {
		LairRespawns data = level.getDataStorage().get(TYPE);
		return data == null ? 0 : data.pending.size();
	}

	/** Checks pending lair respawns in {@code level} and wakes any due guardians near an active player. */
	public static void tickLevel(ServerLevel level) {
		if (!Guardians.enabled() || level.getDifficulty() == Difficulty.PEACEFUL) {
			return;
		}
		LairRespawns data = level.getDataStorage().get(TYPE);
		if (data == null || data.pending.isEmpty()) {
			return;
		}
		long now = level.getGameTime();
		boolean changed = false;
		Iterator<Entry> it = data.pending.iterator();
		while (it.hasNext()) {
			Entry entry = it.next();
			if (now < entry.respawnAt() || !level.isPositionEntityTicking(entry.home()) || !playerNear(level, entry.home())) {
				continue;
			}
			EntityType<? extends Mob> type = typeFor(entry.bossId());
			if (type == null) {
				it.remove();
				changed = true;
				continue;
			}
			if (!level.getEntities(type, new AABB(entry.home()).inflate(EXISTING_RANGE), Mob::isAlive).isEmpty()) {
				it.remove();
				changed = true;
				continue;
			}
			if (spawn(level, type, entry)) {
				it.remove();
				changed = true;
			}
		}
		if (changed) {
			data.setDirty();
		}
	}

	private static boolean playerNear(ServerLevel level, BlockPos home) {
		Vec3 center = Vec3.atCenterOf(home);
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator() && player.position().distanceToSqr(center) <= WATCH_RANGE_SQR) {
				return true;
			}
		}
		return false;
	}

	private static EntityType<? extends Mob> typeFor(String bossId) {
		return switch (bossId) {
			case "rootcoil_serpent" -> Guardians.SERPENT;
			case "lantern_bearer" -> Guardians.LANTERN_BEARER;
			case "hollow_matron" -> Guardians.HOLLOW_MATRON;
			case "ashen_stag" -> Guardians.ASHEN_STAG;
			case "frost_wyrmling" -> Guardians.FROST_WYRMLING;
			case "mojito" -> Guardians.MOJITO;
			case "drowned_bell_warden" -> Guardians.BELL_WARDEN;
			default -> null;
		};
	}

	private static boolean spawn(ServerLevel level, EntityType<? extends Mob> type, Entry entry) {
		Mob boss = type.create(level, EntitySpawnReason.TRIGGERED);
		if (boss == null) {
			return false;
		}
		BlockPos home = entry.home();
		boss.snapTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, entry.yaw(), 0.0F);
		boss.finalizeSpawn(level, level.getCurrentDifficultyAt(home), EntitySpawnReason.TRIGGERED, null);
		BoundingBox box = new BoundingBox(
			entry.boundsMin().getX(), entry.boundsMin().getY(), entry.boundsMin().getZ(),
			entry.boundsMax().getX(), entry.boundsMax().getY(), entry.boundsMax().getZ()
		);
		switch (entry.bossId()) {
			case "rootcoil_serpent" -> ((RootcoilSerpent) boss).bind(home, entry.yaw());
			case "lantern_bearer" -> ((LanternBearer) boss).bind(home, entry.yaw(), box, entry.hook(), entry.extraA());
			case "hollow_matron" -> {
				HollowMatron matron = (HollowMatron) boss;
				matron.bind(home, entry.floorY(), entry.anchor(), entry.yaw(), box);
				matron.nestle(entry.extraA(), entry.extraB());
			}
			case "ashen_stag" -> ((AshenStag) boss).bind(home, entry.yaw(), box);
			case "frost_wyrmling" -> ((FrostWyrmling) boss).bind(home, entry.yaw(), box);
			case "mojito" -> ((Mojito) boss).bind(home, entry.yaw(), box);
			case "drowned_bell_warden" -> ((DrownedBellWarden) boss).bind(home, entry.yaw(), box);
			default -> {
				return false;
			}
		}
		if (!level.addFreshEntityWithPassengers(boss)) {
			return false;
		}
		level.sendParticles(ParticleTypes.ENCHANT, home.getX() + 0.5, home.getY() + 1.5, home.getZ() + 0.5, 24, 1.2, 0.8, 1.2, 0.1);
		level.playSound(null, home, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.2F, 0.7F);
		return true;
	}
}
