package dev.wildercord.cast.events;

import dev.wildercord.cast.Cast;
import dev.wildercord.cast.Runebound;
import dev.wildercord.config.Config;
import dev.wildercord.spell.RuneDef;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Dynamic world events: the server rolls them on its own near players, in loaded ground only, and
 * an operator can start any of them with {@code /wildercord event <mana_storm|starfall|rift> [here]}.
 * <ul>
 *   <li>{@link ManaStorm}: over a ley line, every few days; faster mana, cheaper spells, surges.</li>
 *   <li>{@link FallenStars}: rarely, at night; a guarded star with a rare rune in a small crater.</li>
 *   <li>{@link RiftSiege}: rarely, at night near a settled place; three waves of Runebound.</li>
 * </ul>
 * Every number is in {@link EventRules}. Everything is server-side and temporary: nothing about an
 * event is saved but a fallen star (its rune and its crater, so it can be filled back in) and when
 * each kind of event may next come ({@link EventLedger}), and anything an event spawned that
 * outlives it (after a restart, say) is removed as its chunk loads. The hostile ones never roll on
 * Peaceful, and give nothing there: a rift closes, and a star won't open. A server can switch every
 * event off in its config ({@code features.world_events}): then none starts, not even from the
 * command, and any already under way runs its course.
 */
public final class WorldEvents {
	private WorldEvents() {}

	/** On everything an event spawns: removed as it loads unless its event is still running. */
	public static final String TAG = "wildercord.event";

	static final List<ManaStorm> STORMS = new ArrayList<>();
	static final List<RiftSiege> RIFTS = new ArrayList<>();
	/** Entities spawned by events still running, this session. */
	private static final Set<UUID> LIVE = new HashSet<>();

	public static void init() {
		EventSounds.init();
		EventContent.init();
		EventCommand.init();
		// Loading the class registers the storm attachment (synced, so both sides need it).
		ManaStorm.STORM_UNTIL.identifier();
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity.entityTags().contains(TAG) && !LIVE.contains(entity.getUUID())) {
				entity.discard();
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(WorldEvents::tick);
		// Who fought an event's monsters (hurt one, or was hurt by one), and which of them were killed.
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
			if (!RIFTS.isEmpty() && damage > 0) {
				fought(entity, source);
			}
		});
		ServerLivingEntityEvents.AFTER_DEATH.register(WorldEvents::died);
		// Nothing may be hung on (or taken from) a rift's invisible stand.
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
			entity.entityTags().contains(RiftSiege.ANCHOR_TAG) ? InteractionResult.FAIL : InteractionResult.PASS);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			for (RiftSiege rift : RIFTS) {
				rift.forget();
			}
			STORMS.clear();
			RIFTS.clear();
			LIVE.clear();
			FallenStars.clear();
		});
	}

	private static void tick(MinecraftServer server) {
		if (!STORMS.isEmpty()) {
			STORMS.removeIf(storm -> !storm.tick(storm.level.getGameTime()));
		}
		if (!RIFTS.isEmpty()) {
			for (Iterator<RiftSiege> it = RIFTS.iterator(); it.hasNext(); ) {
				RiftSiege rift = it.next();
				if (!rift.tick(rift.level.getGameTime())) {
					it.remove();
				}
			}
		}
		if (EventRules.ENABLED && server.getTickCount() % EventRules.CHECK_INTERVAL == 0 && enabled()) {
			roll(server);
		}
	}

	/** Whether world events may start at all: the server's config can switch them off ({@code features.world_events}). */
	public static boolean enabled() {
		return Config.get().worldEvents();
	}

	// ------------------------------------------------------------------ rolling

	/** Every 30 seconds: each player's surroundings may start an event (Overworld only). */
	private static void roll(MinecraftServer server) {
		ServerLevel level = server.overworld();
		RandomSource random = level.getRandom();
		long now = level.getGameTime();
		boolean night = EventRules.night(level.getOverworldClockTime());
		boolean hostile = level.getDifficulty() != Difficulty.PEACEFUL;
		EventLedger ledger = EventLedger.of(server);
		for (ServerPlayer player : new ArrayList<>(level.players())) {
			if (player.isSpectator()) {
				continue;
			}
			if (STORMS.size() < EventRules.MAX_STORMS && stormAt(level, player.position()) == null && random.nextDouble() < EventRules.stormChance()) {
				startStorm(level, player, false);
			}
			if (night && hostile && !FallenStars.any(level) && now >= ledger.next(EventLedger.star(level.dimension()))
					&& random.nextDouble() < EventRules.starChance()) {
				startStar(level, player, false);
			}
			if (night && hostile && riftIn(level) == null && now >= ledger.next(EventLedger.rift(level.dimension()))
					&& random.nextDouble() < EventRules.riftChance() && settled(level, player.blockPosition())) {
				startRift(level, player, false);
			}
		}
	}

	// ------------------------------------------------------------------ starting (the command, the tests and the rolls)

	/**
	 * A mana storm over the ley line nearest {@code player} (or, {@code here}, right over them).
	 * Null if there's no ley line near enough, or the region had one lately (unless {@code here}),
	 * or world events are switched off.
	 */
	public static ManaStorm startStorm(ServerLevel level, ServerPlayer player, boolean here) {
		if (!enabled()) {
			return null;
		}
		Vec3 centre = here ? player.position() : ManaStorm.leyHeart(level, player.blockPosition(), EventRules.STORM_SEARCH);
		if (centre == null) {
			return null;
		}
		long now = level.getGameTime();
		EventLedger ledger = EventLedger.of(level.getServer());
		String region = EventLedger.storm(EventRules.region((int) Math.floor(centre.x), (int) Math.floor(centre.z)));
		if (!here && now < ledger.next(region)) {
			return null;
		}
		ledger.setNext(region, now + EventRules.STORM_REGION_COOLDOWN, now);
		ManaStorm storm = new ManaStorm(level, centre, now, EventRules.stormTicks(level.getRandom().nextDouble()));
		STORMS.add(storm);
		storm.begin();
		return storm;
	}

	/** A star falls near {@code player} (or, {@code here}, just in front of them). Returns where it lands, or null (world events switched off, say). */
	public static BlockPos startStar(ServerLevel level, ServerPlayer player, boolean here) {
		if (!enabled()) {
			return null;
		}
		BlockPos land = FallenStars.fall(level, player, here);
		if (land != null) {
			long now = level.getGameTime();
			EventLedger.of(level.getServer()).setNext(EventLedger.star(level.dimension()), now + EventRules.STAR_COOLDOWN, now);
		}
		return land;
	}

	/** A rift opens near {@code player} (or, {@code here}, a few blocks in front of them). Null if there was no room, on Peaceful, or with world events switched off. */
	public static RiftSiege startRift(ServerLevel level, ServerPlayer player, boolean here) {
		if (!enabled() || level.getDifficulty() == Difficulty.PEACEFUL) {
			return null;
		}
		Vec3 base = here ? RiftSiege.spotHere(level, player) : RiftSiege.spotNear(level, player);
		if (base == null) {
			return null;
		}
		long now = level.getGameTime();
		EventLedger.of(level.getServer()).setNext(EventLedger.rift(level.dimension()), now + EventRules.RIFT_COOLDOWN, now);
		RiftSiege rift = new RiftSiege(level, base, player.position());
		RIFTS.add(rift);
		rift.begin();
		return rift;
	}

	// ------------------------------------------------------------------ what's going on

	/** The storm over this point, or null. */
	public static ManaStorm stormAt(Level level, Vec3 at) {
		for (ManaStorm storm : STORMS) {
			if (storm.level == level && storm.covers(at)) {
				return storm;
			}
		}
		return null;
	}

	/** The rift open in this world, or null. */
	public static RiftSiege riftIn(Level level) {
		for (RiftSiege rift : RIFTS) {
			if (rift.level == level) {
				return rift;
			}
		}
		return null;
	}

	public static List<ManaStorm> storms() {
		return List.copyOf(STORMS);
	}

	public static List<RiftSiege> rifts() {
		return List.copyOf(RIFTS);
	}

	/**
	 * From {@code Effects.apply}: a spell's effect landed. A player's spell of an element striking an
	 * open rift counts toward sealing it.
	 */
	public static void onSpell(Cast cast, Cast.Hit hit, String element) {
		if (RIFTS.isEmpty() || element.isEmpty() || !(cast.caster instanceof ServerPlayer player)) {
			return;
		}
		for (RiftSiege rift : RIFTS) {
			if (rift.level == cast.level && rift.struckBy(hit)) {
				rift.strike(player, element);
			}
		}
	}

	/** Something was hurt: if an event's monster and a player were on either end of it, the player fought. */
	private static void fought(LivingEntity victim, DamageSource source) {
		Entity attacker = source.getEntity();
		if (victim instanceof ServerPlayer player) {
			if (attacker != null && attacker.entityTags().contains(TAG)) {
				for (RiftSiege rift : RIFTS) {
					rift.fought(player, attacker.getUUID());
				}
			}
		} else if (attacker instanceof ServerPlayer player && victim.entityTags().contains(TAG)) {
			for (RiftSiege rift : RIFTS) {
				rift.fought(player, victim.getUUID());
			}
		}
	}

	/** Something died: an event's monster was killed (not sent away, not lost), and a Riftcaller leaves its spoils. */
	private static void died(LivingEntity entity, DamageSource source) {
		if (!entity.entityTags().contains(TAG) || !(entity.level() instanceof ServerLevel level)) {
			return;
		}
		for (RiftSiege rift : RIFTS) {
			rift.killed(entity.getUUID());
		}
		FallenStars.guardKilled(entity.getUUID());
		if (entity.entityTags().contains(RiftSiege.RIFTCALLER_TAG)) {
			RiftSiege.riftcallerLoot(level, entity, source);
		}
	}

	/**
	 * Whether a place looks lived in: enough chests, barrels, furnaces, lecterns, crafters, brewing
	 * stands, enchanting tables and signs in the loaded chunks around it (a base, or a village).
	 */
	static boolean settled(ServerLevel level, BlockPos at) {
		int found = 0;
		int cx = at.getX() >> 4;
		int cz = at.getZ() >> 4;
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx + dx, cz + dz);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity || be instanceof AbstractFurnaceBlockEntity
							|| be instanceof LecternBlockEntity || be instanceof CrafterBlockEntity || be instanceof BrewingStandBlockEntity || be instanceof EnchantingTableBlockEntity
							|| be instanceof SignBlockEntity) {
						if (++found >= EventRules.SETTLED_BLOCK_ENTITIES) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	// ------------------------------------------------------------------ helpers for the events

	/**
	 * Spawns a Runebound for an event: marked so it's removed if it outlives its event, never
	 * despawning while the event runs, and turned toward {@code target}. {@code spell} null picks one
	 * that suits it. Only where the ground is loaded and ticking.
	 */
	static Mob spawnRunebound(ServerLevel level, EntityType<? extends Mob> type, Vec3 at, boolean adept, ServerPlayer target, List<RuneDef> spell) {
		BlockPos pos = BlockPos.containing(at);
		if (!level.isPositionEntityTicking(pos)) {
			return null;
		}
		Mob mob = type.create(level, EntitySpawnReason.EVENT);
		if (mob == null) {
			return null;
		}
		float yaw = target == null ? level.getRandom().nextFloat() * 360 : (float) Math.toDegrees(Math.atan2(-(target.getX() - at.x), target.getZ() - at.z));
		mob.snapTo(at.x, at.y, at.z, yaw, 0);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, mob instanceof Zombie ? new Zombie.ZombieGroupData(false, false) : null);
		// It never picks anything up: it goes when its event does, and whatever it carried (a fallen player's gear) would go with it.
		mob.setCanPickUpLoot(false);
		if (spell == null) {
			Runebound.bind(mob, adept);
		} else {
			Runebound.bind(mob, spell, adept);
		}
		mob.setPersistenceRequired();
		mark(mob);
		level.addFreshEntity(mob);
		if (target != null && !target.isCreative()) {
			mob.setTarget(target);
		}
		return mob;
	}

	static Mob spawnRunebound(ServerLevel level, EntityType<? extends Mob> type, Vec3 at, boolean adept, ServerPlayer target) {
		return spawnRunebound(level, type, at, adept, target, null);
	}

	/** Marks an entity as an event's (call before adding it to the world). */
	static void mark(Entity entity) {
		entity.addTag(TAG);
		LIVE.add(entity.getUUID());
	}

	/** An event's monster goes back where it came from, in a puff of violet, leaving anything it picked up. */
	static void vanish(ServerLevel level, Entity entity) {
		Vec3 at = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, 20, 0.3, 0.5, 0.3, 0.05);
		level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 6, 0.3, 0.5, 0.3, 0.03);
		if (entity instanceof Mob mob) {
			mob.dropPreservedEquipment(level);
		}
		LIVE.remove(entity.getUUID());
		entity.discard();
	}

	static void forget(UUID id) {
		LIVE.remove(id);
	}

	/**
	 * Where a monster could stand near {@code near}: loaded, ticking ground that's solid and dry, with
	 * room above, within a few blocks up or down. Null if there's none.
	 */
	static Vec3 standingSpot(ServerLevel level, Vec3 near) {
		int x = (int) Math.floor(near.x);
		int z = (int) Math.floor(near.z);
		BlockPos probe = new BlockPos(x, (int) Math.floor(near.y), z);
		if (!level.isLoaded(probe) || !level.isPositionEntityTicking(probe)) {
			return null;
		}
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		BlockPos feet = new BlockPos(x, y, z);
		if (Math.abs(y - near.y) > 6 || !level.getFluidState(feet.below()).isEmpty() || !level.getBlockState(feet.below()).isSolid()
				|| !level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
				|| !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
			return null;
		}
		return Vec3.atBottomCenterOf(feet);
	}

	/** Plays a sound for one player only, from where they stand. */
	static void soundFor(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
		player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.AMBIENT,
			player.getX(), player.getY(), player.getZ(), volume, pitch, player.getRandom().nextLong()));
	}

	/**
	 * A sound heard from far off: close by it plays where it happens; further away it plays from
	 * the right direction, a little way off, fainter with distance.
	 */
	static void farSound(ServerLevel level, Vec3 at, SoundEvent sound, double range, float pitch) {
		for (ServerPlayer player : level.players()) {
			double d = player.position().distanceTo(at);
			if (d > range) {
				continue;
			}
			Vec3 from = at;
			float volume = 1.0F;
			if (d > 24) {
				from = player.position().add(at.subtract(player.position()).normalize().scale(16));
				volume = (float) (1.0 - 0.75 * (d - 24) / (range - 24));
			}
			player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.AMBIENT,
				from.x, from.y, from.z, volume, pitch, player.getRandom().nextLong()));
		}
	}

	/** A particle every player in the world sees from up to 400 blocks (a falling star, a beacon of starlight). */
	static void far(ServerLevel level, ParticleOptions particle, Vec3 at) {
		for (ServerPlayer player : level.players()) {
			if (player.position().distanceToSqr(at) <= 400 * 400 && player.getEyePosition().distanceToSqr(at) > 2.0) {
				level.sendParticles(player, particle, true, true, at.x, at.y, at.z, 1, 0, 0, 0, 0);
			}
		}
	}

	/** Which way something lies from a player: north, northeast... */
	static Component direction(Vec3 from, Vec3 to) {
		double angle = Math.toDegrees(Math.atan2(to.x - from.x, -(to.z - from.z)));
		String[] names = {"north", "northeast", "east", "southeast", "south", "southwest", "west", "northwest"};
		return Component.translatable("direction.wildercord." + names[Math.floorMod((int) Math.round(angle / 45.0), 8)]);
	}
}
