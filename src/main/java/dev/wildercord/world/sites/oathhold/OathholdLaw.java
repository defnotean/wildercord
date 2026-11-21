package dev.wildercord.world.sites.oathhold;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.ServerState;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.OathboundBannerMarshal;
import dev.wildercord.aura.world.OathboundBastion;
import dev.wildercord.aura.world.OathboundBladeChampion;
import dev.wildercord.aura.world.OathboundHalberdier;
import dev.wildercord.town.WayfarerKeeper;
import dev.wildercord.wildlife.SakuraVillager;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Oathhold city registry, criminal law enforcement, and neutral-by-default Oathbound Knight targeting.
 * Oathbound High Knights patrol Oathhold in peace and defend against monsters; they pursue a player
 * only when the player is Wanted for a crime (assault, murder, theft, vandalism), is attacked directly,
 * or enters a sanctioned Colosseum Arena Bout.
 */
public final class OathholdLaw {
	private OathholdLaw() {}

	public static final String ARENA_DUELIST_TAG = "wildercord.arena_duelist";

	public record CrimeRecord(int bountyEmeralds, long wantedUntil, String lastCrime) {
		public static final CrimeRecord CLEAN = new CrimeRecord(0, 0L, "none");
		public static final Codec<CrimeRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.optionalFieldOf("bounty", 0).forGetter(CrimeRecord::bountyEmeralds),
			Codec.LONG.optionalFieldOf("wanted_until", 0L).forGetter(CrimeRecord::wantedUntil),
			Codec.STRING.optionalFieldOf("last_crime", "none").forGetter(CrimeRecord::lastCrime)
		).apply(i, CrimeRecord::new));
	}

	public static final AttachmentType<CrimeRecord> CRIME = AttachmentRegistry.create(Wildercord.id("oathhold_crime"),
		builder -> builder.initializer(() -> CrimeRecord.CLEAN).persistent(CrimeRecord.CODEC));

	private static final Codec<OathholdOrigin.Origin> ORIGIN_CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.fieldOf("ox").forGetter(OathholdOrigin.Origin::originX),
		Codec.INT.fieldOf("by").forGetter(OathholdOrigin.Origin::baseY),
		Codec.INT.fieldOf("oz").forGetter(OathholdOrigin.Origin::originZ)
	).apply(i, OathholdOrigin.Origin::new));

	public static final class CityLedger extends SavedData {
		private static final Codec<CityLedger> CODEC = RecordCodecBuilder.create(i -> i.group(
			ORIGIN_CODEC.listOf().optionalFieldOf("cities", List.of()).forGetter(l -> l.cities),
			Codec.LONG.listOf().optionalFieldOf("placed", List.of()).forGetter(l -> new ArrayList<>(l.placed))
		).apply(i, CityLedger::new));
		static final SavedDataType<CityLedger> TYPE = new SavedDataType<>(Wildercord.id("oathhold_law"), CityLedger::new, CODEC, null);

		private final List<OathholdOrigin.Origin> cities = new ArrayList<>();
		private final LongSet placed = new LongOpenHashSet();

		public CityLedger() {}

		private CityLedger(List<OathholdOrigin.Origin> cities, List<Long> placed) {
			this.cities.addAll(cities);
			this.placed.addAll(placed);
		}

		public boolean addCity(OathholdOrigin.Origin o) {
			for (OathholdOrigin.Origin existing : cities) {
				if (existing.originX() == o.originX() && existing.originZ() == o.originZ()) {
					return false;
				}
			}
			cities.add(o);
			setDirty();
			return true;
		}

		public void markPlaced(long posLong) {
			if (placed.add(posLong)) setDirty();
		}

		public boolean isPlaced(long posLong) {
			return placed.contains(posLong);
		}

		public boolean consumePlaced(long posLong) {
			boolean removed = placed.remove(posLong);
			if (removed) setDirty();
			return removed;
		}

		public Optional<OathholdOrigin.Origin> find(int wx, int wz) {
			for (OathholdOrigin.Origin o : cities) {
				if (OathholdLawRules.isInsideCityWalls(o.originX(), o.originZ(), wx, wz)) {
					return Optional.of(o);
				}
			}
			return Optional.empty();
		}
	}

	private static final Queue<OathholdOrigin.Origin> PENDING_CITIES = new ConcurrentLinkedQueue<>();
	private static final Map<UUID, Long> LAST_THEFT_WARN = new HashMap<>();

	public static void init() {
		ServerState.forgetOnDisconnect(LAST_THEFT_WARN);
		ServerState.resetOnStop(LAST_THEFT_WARN);
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> PENDING_CITIES.clear());

		ServerTickEvents.END_SERVER_TICK.register(OathholdLaw::flushPendingCities);

		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (!(world instanceof ServerLevel level) || !(player instanceof ServerPlayer server)) {
				return InteractionResult.PASS;
			}
			BlockPos pos = hit.getBlockPos();
			BlockState state = level.getBlockState(pos);
			CityLedger ledger = ledger(level.getServer());
			if (!server.isSpectator() && server.getItemInHand(hand).getItem() instanceof BlockItem) {
				ledger.markPlaced(pos.relative(hit.getDirection()).asLong());
			}
			if (!server.isCreative() && !server.isSpectator()
					&& (state.getBlock() instanceof ChestBlock || state.getBlock() instanceof BarrelBlock)
					&& findContainingCity(level, pos).isPresent()
					&& !ledger.isPlaced(pos.asLong())
					&& hasWitness(level, pos, 24.0)) {
				long now = level.getGameTime();
				long last = LAST_THEFT_WARN.getOrDefault(server.getUUID(), -100L);
				if (now - last >= 40L) {
					LAST_THEFT_WARN.put(server.getUUID(), now);
					recordCrime(server, OathholdLawRules.CrimeKind.THEFT);
				}
			}
			return InteractionResult.PASS;
		});

		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> {
			if (!(world instanceof ServerLevel level) || !(player instanceof ServerPlayer server)) {
				return true;
			}
			if (OathholdQuests.onBlockBroken(level, server, pos, state)) {
				return true;
			}
			if (server.isCreative() || server.isSpectator()) {
				return true;
			}
			if (findContainingCity(level, pos).isEmpty()) {
				return true;
			}
			CityLedger ledger = ledger(level.getServer());
			if (ledger.consumePlaced(pos.asLong())) {
				return true;
			}
			if (state.isAir() || state.canBeReplaced()) {
				return true;
			}
			recordCrime(server, OathholdLawRules.CrimeKind.VANDALISM);
			return false;
		});

		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			if (entity.level() instanceof ServerLevel level) {
				onEntityAttacked(level, entity, source);
			}
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(entity.level() instanceof ServerLevel)) return;
			if (entity instanceof ServerPlayer dead && isKnightEntity(source.getEntity())) {
				clearCrime(dead, false);
				dead.sendSystemMessage(Component.literal("[Oathhold Watch] Your bounty was settled by defeat.").withStyle(ChatFormatting.GRAY));
				return;
			}
			if (source.getEntity() instanceof ServerPlayer killer && !killer.isCreative() && !killer.isSpectator() && isProtectedPerson(entity)) {
				recordCrime(killer, OathholdLawRules.CrimeKind.MURDER);
			}
		});
	}

	/** Queues a discovered/generated Oathhold origin from a worldgen worker thread. */
	public static void rememberCity(WorldGenLevel worldGenLevel, int originX, int baseY, int originZ) {
		PENDING_CITIES.add(new OathholdOrigin.Origin(originX, baseY, originZ));
	}

	/** Immediately registers a city origin on the server thread (also used in tests). */
	public static void registerCityNow(ServerLevel level, int originX, int baseY, int originZ) {
		ledger(level.getServer()).addCity(new OathholdOrigin.Origin(originX, baseY, originZ));
	}

	private static void flushPendingCities(MinecraftServer server) {
		if (PENDING_CITIES.isEmpty()) return;
		CityLedger ledger = ledger(server);
		for (OathholdOrigin.Origin o; (o = PENDING_CITIES.poll()) != null; ) {
			ledger.addCity(o);
		}
	}

	public static CityLedger ledger(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(CityLedger.TYPE);
	}

	public static Optional<OathholdOrigin.Origin> findContainingCity(ServerLevel level, BlockPos pos) {
		if (!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
			return Optional.empty();
		}
		return ledger(level.getServer()).find(pos.getX(), pos.getZ());
	}

	public static void onEntityAttacked(ServerLevel level, LivingEntity victim, DamageSource source) {
		if (!(source.getEntity() instanceof ServerPlayer attacker) || attacker.isCreative() || attacker.isSpectator()) {
			return;
		}
		if (!isProtectedPerson(victim)) {
			return;
		}
		recordCrime(attacker, OathholdLawRules.CrimeKind.ASSAULT);
	}

	public static boolean isProtectedPerson(net.minecraft.world.entity.Entity e) {
		if (e.entityTags().contains(ARENA_DUELIST_TAG)) {
			return false;
		}
		return e instanceof OathholdCitizen
			|| e instanceof Villager
			|| e instanceof WayfarerKeeper
			|| e instanceof SakuraVillager
			|| e instanceof IronGolem
			|| isKnightEntity(e);
	}

	public static boolean isKnightEntity(net.minecraft.world.entity.Entity e) {
		return e instanceof OathboundBastion
			|| e instanceof OathboundBladeChampion
			|| e instanceof OathboundHalberdier
			|| e instanceof OathboundBannerMarshal;
	}

	public static boolean isWanted(ServerPlayer player) {
		CrimeRecord rec = player.getAttachedOrElse(CRIME, CrimeRecord.CLEAN);
		long now = player.level().getServer().overworld().getGameTime();
		return OathholdLawRules.isWanted(rec.bountyEmeralds(), rec.wantedUntil(), now);
	}

	public static int bountyOf(ServerPlayer player) {
		if (!isWanted(player)) return 0;
		return player.getAttachedOrElse(CRIME, CrimeRecord.CLEAN).bountyEmeralds();
	}

	public static void recordCrime(ServerPlayer criminal, OathholdLawRules.CrimeKind crime) {
		if (crime == OathholdLawRules.CrimeKind.NONE || criminal.isCreative() || criminal.isSpectator()) return;
		ServerLevel overworld = criminal.level().getServer().overworld();
		long now = overworld.getGameTime();
		CrimeRecord cur = criminal.getAttachedOrElse(CRIME, CrimeRecord.CLEAN);
		int baseBounty = OathholdLawRules.isWanted(cur.bountyEmeralds(), cur.wantedUntil(), now) ? cur.bountyEmeralds() : 0;
		int nextBounty = OathholdLawRules.updatedBounty(baseBounty, crime);
		long nextUntil = OathholdLawRules.updatedWantedUntil(cur.wantedUntil(), now);
		criminal.setAttached(CRIME, new CrimeRecord(nextBounty, nextUntil, crime.id));

		criminal.sendSystemMessage(Component.literal(
			"[Oathhold Watch] Crime committed (" + crime.id + ")! Bounty is now " + nextBounty
				+ " Emeralds — Oathbound Knights are hostile! Sneak-pay a Watchman/Prelate or run /adventurer payfine.")
			.withStyle(ChatFormatting.RED));
		criminal.level().playSound(null, criminal.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 0.55F, 1.25F);
		alertNearbyKnights((ServerLevel) criminal.level(), criminal);
	}

	public static void alertNearbyKnights(ServerLevel level, ServerPlayer criminal) {
		AABB box = criminal.getBoundingBox().inflate(44.0);
		for (Mob knight : level.getEntitiesOfClass(Mob.class, box,
				m -> isKnightEntity(m) && m.isAlive() && !m.entityTags().contains(ARENA_DUELIST_TAG))) {
			knight.setTarget(criminal);
		}
	}

	public static void clearCrime(ServerPlayer player, boolean pacifyNearbyKnights) {
		player.setAttached(CRIME, CrimeRecord.CLEAN);
		if (pacifyNearbyKnights && player.level() instanceof ServerLevel level) {
			AABB box = player.getBoundingBox().inflate(64.0);
			for (Mob knight : level.getEntitiesOfClass(Mob.class, box, m -> isKnightEntity(m) && m.isAlive())) {
				if (knight.getTarget() == player) {
					knight.setTarget(null);
				}
				if (knight.getLastHurtByMob() == player) {
					knight.setLastHurtByMob(null);
				}
				if (knight.getLastHurtByPlayer() == player) {
					knight.setLastHurtByPlayer((Player) null, 0);
				}
			}
		}
	}

	public static boolean tryPayFine(ServerPlayer player) {
		int bounty = bountyOf(player);
		if (bounty <= 0) {
			player.sendSystemMessage(Component.literal("[Oathhold Watch] You have no active bounty in Oathhold.").withStyle(ChatFormatting.GREEN));
			return false;
		}
		int have = player.getInventory().countItem(Items.EMERALD);
		if (have < bounty) {
			player.sendSystemMessage(Component.literal("[Oathhold Watch] Restitution requires " + bounty + " Emeralds (you carry " + have + ").")
				.withStyle(ChatFormatting.YELLOW));
			return false;
		}
		int need = bounty;
		var inv = player.getInventory();
		for (int s = 0; s < inv.getContainerSize() && need > 0; s++) {
			ItemStack st = inv.getItem(s);
			if (st.is(Items.EMERALD)) {
				int take = Math.min(need, st.getCount());
				st.shrink(take);
				need -= take;
			}
		}
		inv.setChanged();
		clearCrime(player, true);
		player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.9F, 1.2F);
		player.sendSystemMessage(Component.literal("[Oathhold Watch] Paid " + bounty + " Emeralds in restitution. You are no longer Wanted!")
			.withStyle(ChatFormatting.GREEN));
		return true;
	}

	private static boolean hasWitness(ServerLevel level, BlockPos pos, double radius) {
		AABB box = new AABB(pos).inflate(radius);
		return !level.getEntitiesOfClass(Mob.class, box,
			m -> m.isAlive() && (m instanceof OathholdCitizen || isKnightEntity(m))).isEmpty();
	}

	/** True when {@code knight} is permitted to initiate combat against {@code player}. */
	public static boolean shouldKnightAttack(Mob knight, ServerPlayer player) {
		if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
			return false;
		}
		if (knight.entityTags().contains(ARENA_DUELIST_TAG)) {
			return knight.entityTags().contains(OathholdQuests.challengerTag(player.getUUID()));
		}
		return isWanted(player);
	}

	/**
	 * Replaces unconditional {@code NearestAttackableTargetGoal<Player>} on Oathbound Knights:
	 * knights remain neutral until a player commits a crime or steps into a sanctioned Arena bout against them.
	 */
	public static final class LawEnforcementTargetGoal extends TargetGoal {
		private ServerPlayer candidate;
		private int scanCooldown;

		public LawEnforcementTargetGoal(Mob knight) {
			super(knight, false);
		}

		@Override
		public boolean canUse() {
			if (!(mob.level() instanceof ServerLevel level)) return false;
			if (scanCooldown > 0) {
				scanCooldown--;
				return false;
			}
			scanCooldown = 10;
			double maxDistSqr = getFollowDistance() * getFollowDistance();
			ServerPlayer best = null;
			double bestDist = Double.MAX_VALUE;
			for (ServerPlayer p : level.players()) {
				double d2 = p.distanceToSqr(mob);
				if (d2 <= maxDistSqr && shouldKnightAttack(mob, p) && mob.getSensing().hasLineOfSight(p) && d2 < bestDist) {
					best = p;
					bestDist = d2;
				}
			}
			this.candidate = best;
			return best != null;
		}

		@Override
		public void start() {
			mob.setTarget(candidate);
			super.start();
		}

		@Override
		public boolean canContinueToUse() {
			LivingEntity current = mob.getTarget();
			if (!(current instanceof ServerPlayer sp) || !current.isAlive()) {
				return false;
			}
			if (sp.isCreative() || sp.isSpectator()) {
				return false;
			}
			if (!shouldKnightAttack(mob, sp) && mob.getLastHurtByMob() != sp) {
				return false;
			}
			return current.distanceToSqr(mob) <= getFollowDistance() * getFollowDistance();
		}

		@Override
		public void stop() {
			candidate = null;
			super.stop();
		}
	}

	/** Allows Oathbound Knights to patrol and slay hostile wild monsters near walls and roads. */
	public static final class CityMonsterDefenseGoal extends TargetGoal {
		private LivingEntity targetMonster;
		private int scanCooldown;

		public CityMonsterDefenseGoal(Mob knight) {
			super(knight, true);
		}

		@Override
		public boolean canUse() {
			if (mob.entityTags().contains(ARENA_DUELIST_TAG)) return false;
			if (!(mob.level() instanceof ServerLevel level)) return false;
			if (scanCooldown > 0) {
				scanCooldown--;
				return false;
			}
			scanCooldown = 15;
			AABB box = mob.getBoundingBox().inflate(Math.min(28.0, getFollowDistance()));
			TargetingConditions cond = TargetingConditions.forCombat().range(28.0);
			LivingEntity best = null;
			double bestDist = Double.MAX_VALUE;
			for (Mob candidate : level.getEntitiesOfClass(Mob.class, box,
					m -> m != mob && m.isAlive() && m instanceof Enemy && !isKnightEntity(m))) {
				if (cond.test(level, mob, candidate)) {
					double d2 = candidate.distanceToSqr(mob);
					if (d2 < bestDist) {
						best = candidate;
						bestDist = d2;
					}
				}
			}
			this.targetMonster = best;
			return best != null;
		}

		@Override
		public void start() {
			mob.setTarget(targetMonster);
			super.start();
		}
	}
}
