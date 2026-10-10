package dev.wildercord.town;

import dev.wildercord.aura.Aura;
import dev.wildercord.monster.Tempering;
import dev.wildercord.monster.TemperingRules;
import dev.wildercord.player.Heart;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bandits raid a Wayfarer Inn ({@link InnRaidRules}): at night, while a traveller the keepers know is inside, waves of
 * pillagers and vindicators come for it, the last led by a Bandit Captain. Every traveller within reach defends it together.
 * A raid outlasts a restart: when the server stops its bandits go and where it stood is saved in the overworld's data
 * ({@code wildercord:inn_raids}). When it starts again, the raid takes up the wave it had reached, fought afresh, once a
 * defender is back, and gives up after {@link InnRaidRules#RESUME_GRACE} if none comes. A bandit left behind by a crash
 * vanishes when it loads.
 */
public final class InnRaid {
	private static final int COLOR = 0xE0A060;
	public static final String TAG = "wildercord.inn_raid";
	private static final List<EntityType<? extends Mob>> BANDITS = List.of(EntityTypes.PILLAGER, EntityTypes.VINDICATOR, EntityTypes.PILLAGER);

	/** Raids by the inn's middle. */
	private static final Map<Long, InnRaid> ACTIVE = new HashMap<>();

	private final ServerLevel level;
	private final Vec3 centre;
	private final BountyRules.Tier tier;
	private final long day;
	private final ServerBossEvent bar;
	private final List<Mob> mobs = new ArrayList<>();
	private int wave;
	private int pending;
	private long nextWaveAt;
	private long waveDeadline;
	private boolean breathing;
	/** For a raid carried over a restart: until when it waits for a defender. */
	private long graceUntil;

	/** One raid as saved over a restart. */
	public record Saved(Identifier dimension, long centre, int tier, long day, int wave) {
		static final Codec<Saved> CODEC = RecordCodecBuilder.create(i -> i.group(
			Identifier.CODEC.fieldOf("dimension").forGetter(Saved::dimension),
			Codec.LONG.fieldOf("centre").forGetter(Saved::centre),
			Codec.INT.fieldOf("tier").forGetter(Saved::tier),
			Codec.LONG.fieldOf("day").forGetter(Saved::day),
			Codec.INT.fieldOf("wave").forGetter(Saved::wave)
		).apply(i, Saved::new));
	}

	/** The raids under way when the server last stopped. */
	public static final class Ledger extends SavedData {
		static final Codec<Ledger> CODEC = Saved.CODEC.listOf().optionalFieldOf("raids", List.of()).xmap(Ledger::new, l -> l.raids).codec();
		static final SavedDataType<Ledger> TYPE = new SavedDataType<>(Wildercord.id("inn_raids"), Ledger::new, CODEC, null);

		private List<Saved> raids;

		public Ledger() {
			this(List.of());
		}

		private Ledger(List<Saved> raids) {
			this.raids = List.copyOf(raids);
		}

		public List<Saved> raids() {
			return raids;
		}

		void set(List<Saved> raids) {
			this.raids = List.copyOf(raids);
			setDirty();
		}
	}

	private static Ledger ledger(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Ledger.TYPE);
	}

	private InnRaid(ServerLevel level, Vec3 centre, BountyRules.Tier tier, long day) {
		this.level = level;
		this.centre = centre;
		this.tier = tier;
		this.day = day;
		this.bar = new ServerBossEvent(UUID.randomUUID(), title(1), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
		this.nextWaveAt = level.getGameTime() + 100;
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (!ACTIVE.isEmpty()) ACTIVE.values().removeIf(r -> !r.tick());
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(InnRaid::suspend);
		ServerLifecycleEvents.SERVER_STARTED.register(InnRaid::resume);
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Mob mob && mob.entityTags().contains(TAG) && !owned(mob)) mob.discard();
		});
	}

	/** Whether a raid owns {@code mob}; a bandit reloaded with its chunk takes the place of the copy that unloaded. */
	private static boolean owned(Mob mob) {
		for (InnRaid raid : ACTIVE.values()) {
			for (int i = 0; i < raid.mobs.size(); i++) {
				if (raid.mobs.get(i).getUUID().equals(mob.getUUID())) {
					raid.mobs.set(i, mob);
					return true;
				}
			}
		}
		return false;
	}

	/** The server is stopping: every raid's bandits go, and where each raid stood is saved for {@link #resume}. */
	public static void suspend(MinecraftServer server) {
		List<Saved> saved = new ArrayList<>();
		for (Map.Entry<Long, InnRaid> e : ACTIVE.entrySet()) {
			InnRaid r = e.getValue();
			saved.add(new Saved(r.level.dimension().identifier(), e.getKey(), r.tier.ordinal(), r.day, r.wave));
			r.clear();
		}
		ACTIVE.clear();
		ledger(server).set(saved);
	}

	/** Ends every raid with nothing saved, as if each had been beaten off without pay. */
	public static void endAll(MinecraftServer server) {
		for (InnRaid r : ACTIVE.values()) r.clear();
		ACTIVE.clear();
		ledger(server).set(List.of());
	}

	/** How many raids wait in the saved data for the server to start. */
	public static int saved(MinecraftServer server) {
		return ledger(server).raids().size();
	}

	/** The server has started: each raid saved by {@link #suspend} takes up its wave again once a defender is back. */
	public static void resume(MinecraftServer server) {
		Ledger ledger = ledger(server);
		BountyRules.Tier[] tiers = BountyRules.Tier.values();
		for (Saved s : ledger.raids()) {
			ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, s.dimension()));
			if (level == null || ACTIVE.containsKey(s.centre())) continue;
			BountyRules.Tier tier = tiers[Math.max(0, Math.min(tiers.length - 1, s.tier()))];
			InnRaid raid = new InnRaid(level, Vec3.atBottomCenterOf(BlockPos.of(s.centre())), tier, s.day());
			raid.wave = InnRaidRules.resumeFrom(tier, s.wave()) - 1;
			raid.breathing = raid.wave > 0;
			raid.graceUntil = level.getGameTime() + InnRaidRules.RESUME_GRACE;
			ACTIVE.put(s.centre(), raid);
		}
		if (!ledger.raids().isEmpty()) ledger.set(List.of());
	}

	/** Whether the inn with its middle at {@code centre} is being raided. */
	public static boolean active(BlockPos centre) {
		return ACTIVE.containsKey(centre.asLong());
	}

	/** {@code player} is inside the inn with its middle at {@code centre}: at night, now and then, the bandits come. */
	public static void chance(ServerPlayer player, BlockPos centre) {
		ServerLevel level = player.level();
		if (level.isBrightOutside() || level.getDifficulty() == Difficulty.PEACEFUL || player.isCreative() || active(centre)) return;
		Town.Standing standing = Town.standing(player);
		long today = level.getGameTime() / BountyRules.DAY;
		if (!InnRaidRules.due(standing.tier(), standing.lastRaidDay(), today)) return;
		if (level.getRandom().nextDouble() >= InnRaidRules.CHANCE) return;
		begin(player, centre, today);
	}

	/** Begins a raid on the inn with its middle at {@code centre}, sized to {@code player}'s standing. */
	public static void begin(ServerPlayer player, BlockPos centre, long today) {
		if (active(centre)) return;
		InnRaid raid = new InnRaid(player.level(), Vec3.atBottomCenterOf(centre), BountyRules.Tier.of(Math.max(Town.standing(player).reputation(),
			InnRaidRules.MIN_TIER.needs)), today);
		ACTIVE.put(centre.asLong(), raid);
		raid.open();
	}

	private Component title(int n) {
		return Component.translatable("boss.wildercord.inn_raid", n, InnRaidRules.waves(tier));
	}

	private List<ServerPlayer> defenders() {
		List<ServerPlayer> list = new ArrayList<>();
		for (ServerPlayer player : level.players()) {
			if (player.isAlive() && !player.isSpectator() && player.position().distanceTo(centre) <= InnRaidRules.RADIUS) list.add(player);
		}
		return list;
	}

	private void open() {
		level.playSound(null, BlockPos.containing(centre), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 4F, 1F);
		for (ServerPlayer player : defenders()) {
			Town.set(player, Town.standing(player).raided(day));
			player.sendSystemMessage(Component.translatable("message.wildercord.inn_raid.begin", InnRaidRules.waves(tier)).withColor(COLOR));
		}
	}

	/** Every server tick; false once it's over. */
	private boolean tick() {
		long now = level.getGameTime();
		List<ServerPlayer> defenders = defenders();
		if (defenders.isEmpty() && now < graceUntil) {
			nextWaveAt = Math.max(nextWaveAt, now + 100);
			return true;
		}
		if (graceUntil > 0 && !defenders.isEmpty()) {
			graceUntil = 0;
			for (ServerPlayer player : defenders) {
				player.sendSystemMessage(Component.translatable("message.wildercord.inn_raid.resumed", wave + 1, InnRaidRules.waves(tier)).withColor(COLOR));
			}
		}
		if (defenders.isEmpty()) {
			fail("fled");
			return false;
		}
		if (level.getDifficulty() == Difficulty.PEACEFUL) {
			fail("fled");
			return false;
		}
		if (now % 20 == 0) {
			for (ServerPlayer player : defenders) bar.addPlayer(player);
			for (ServerPlayer player : List.copyOf(bar.getPlayers())) {
				if (!defenders.contains(player)) bar.removePlayer(player);
			}
		}
		int alive = 0;
		for (Mob mob : mobs) {
			if (mob.isAlive() && !mob.isRemoved()) {
				alive++;
				if (mob.getTarget() == null || !mob.getTarget().isAlive()) mob.setTarget(nearest(defenders, mob));
			}
		}
		if (now % 10 == 0 && wave > 0) {
			int size = InnRaidRules.waveSize(tier, wave) + (InnRaidRules.lastWave(tier, wave) ? 1 : 0);
			bar.setName(title(wave));
			bar.setProgress(Math.max(0F, Math.min(1F, (alive + pending) / (float) size)));
		}
		if (wave > 0 && !breathing && pending == 0 && alive == 0) {
			if (InnRaidRules.lastWave(tier, wave)) {
				win(defenders);
				return false;
			}
			breathing = true;
			nextWaveAt = now + InnRaidRules.BREATH_TICKS;
			for (ServerPlayer player : defenders) {
				player.sendOverlayMessage(Component.translatable("message.wildercord.inn_raid.breath", wave, InnRaidRules.waves(tier)).withColor(COLOR));
			}
		}
		if (wave > 0 && !breathing && now > waveDeadline) {
			fail("time");
			return false;
		}
		if ((wave == 0 || breathing) && now >= nextWaveAt) {
			breathing = false;
			spawnWave(wave + 1, now, defenders);
		}
		return true;
	}

	private static ServerPlayer nearest(List<ServerPlayer> defenders, Mob mob) {
		ServerPlayer best = null;
		for (ServerPlayer player : defenders) {
			if (player.isCreative()) continue;
			if (best == null || player.distanceToSqr(mob) < best.distanceToSqr(mob)) best = player;
		}
		return best;
	}

	private void spawnWave(int n, long now, List<ServerPlayer> defenders) {
		wave = n;
		waveDeadline = now + InnRaidRules.WAVE_TICKS;
		int size = InnRaidRules.waveSize(tier, n);
		boolean last = InnRaidRules.lastWave(tier, n);
		level.playSound(null, BlockPos.containing(centre), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 3F, 1.1F + 0.05F * n);
		for (ServerPlayer player : defenders) {
			player.sendOverlayMessage(Component.translatable(last ? "message.wildercord.inn_raid.last" : "message.wildercord.inn_raid.wave",
				n, InnRaidRules.waves(tier)).withColor(COLOR));
		}
		// They come together from one side.
		double side = level.getRandom().nextDouble() * Math.PI * 2;
		pending = size + (last ? 1 : 0);
		for (int i = 0; i < size; i++) {
			EntityType<? extends Mob> type = BANDITS.get((i + n) % BANDITS.size());
			dev.wildercord.cast.Scheduler.later(1 + i * 6, () -> emerge(type, side, false));
		}
		if (last) dev.wildercord.cast.Scheduler.later(20 + size * 6, () -> emerge(EntityTypes.VINDICATOR, side, true));
	}

	private void emerge(EntityType<? extends Mob> type, double side, boolean captain) {
		pending = Math.max(0, pending - 1);
		if (!ACTIVE.containsValue(this)) return;
		Mob mob = type.create(level, EntitySpawnReason.EVENT);
		if (mob == null) return;
		RandomSource random = level.getRandom();
		BlockPos at = null;
		for (int attempt = 0; attempt < 12 && at == null; attempt++) {
			double angle = side + (random.nextDouble() - 0.5) * 1.2;
			double distance = InnRaidRules.SPAWN_NEAR + random.nextDouble() * (InnRaidRules.SPAWN_FAR - InnRaidRules.SPAWN_NEAR);
			BlockPos column = BlockPos.containing(centre.x + Math.cos(angle) * distance, centre.y, centre.z + Math.sin(angle) * distance);
			if (!level.isPositionEntityTicking(column)) continue;
			BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
			if (!level.getFluidState(ground).isEmpty() || !level.getFluidState(ground.below()).isEmpty()) continue;
			mob.snapTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, (float) Math.toDegrees(angle) + 90, 0);
			if (level.noCollision(mob)) at = ground;
		}
		if (at == null) return;
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.EVENT, null);
		mob.setCanPickUpLoot(false);
		mob.setPersistenceRequired();
		mob.addTag(TAG);
		if (captain) {
			int threat = 0;
			for (ServerPlayer player : defenders()) threat = Math.max(threat, TemperingRules.threat(Heart.circles(player), Aura.stage(player)));
			TemperingRules.Elite[] kinds = TemperingRules.Elite.values();
			Tempering.champion(mob, Math.max(BountyRules.ELITE_THREAT_MIN, threat), kinds[random.nextInt(kinds.length)], InnRaidRules.CAPTAIN_HEALTH,
				Component.translatable("entity.wildercord.bandit_captain").withColor(COLOR));
		}
		mobs.add(mob);
		level.addFreshEntity(mob);
		mob.setTarget(nearest(defenders(), mob));
		if (captain) {
			for (ServerPlayer player : defenders()) {
				player.sendOverlayMessage(Component.translatable("message.wildercord.inn_raid.captain").withColor(COLOR));
			}
		}
	}

	private void win(List<ServerPlayer> defenders) {
		bar.removeAllPlayers();
		level.playSound(null, BlockPos.containing(centre), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1F, 1F);
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, centre.x, centre.y + 2, centre.z, 40, 4, 2, 4, 0);
		int emeralds = InnRaidRules.emeralds(tier);
		int reputation = InnRaidRules.reputation(tier);
		for (ServerPlayer player : defenders) {
			Town.set(player, Town.standing(player).plus(reputation));
			ItemStack stack = new ItemStack(Items.EMERALD, emeralds);
			if (!player.getInventory().add(stack)) player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
			player.sendSystemMessage(Component.translatable("message.wildercord.inn_raid.won", emeralds, reputation).withColor(COLOR));
		}
	}

	private void fail(String why) {
		for (ServerPlayer player : List.copyOf(bar.getPlayers())) {
			player.sendSystemMessage(Component.translatable("message.wildercord.inn_raid.failed." + why).withColor(0xE07A5F));
		}
		clear();
	}

	private void clear() {
		bar.removeAllPlayers();
		for (Mob mob : mobs) {
			if (mob.isAlive() && !mob.isRemoved()) {
				level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY() + 1, mob.getZ(), 8, 0.3, 0.5, 0.3, 0.02);
				mob.discard();
			}
		}
		mobs.clear();
	}
}
