package dev.wildercord.aura.arts;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.ArtRules;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.cast.Statuses;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * What an art leaves on a creature for a while, and what the rest of aura asks about it:
 * <ul>
 * <li><b>Glacier Mirror</b>'s ice: projectiles from in front turn back at whoever loosed them ({@link #deflection}, asked by
 * {@code AuraGuard.deflection} when no perfect guard turned it);</li>
 * <li><b>Eye of the Storm</b>'s wind: projectiles turn aside before they reach its swordsman, from any side;</li>
 * <li><b>Updraft</b>'s juggle: while a foe it threw is still in the air, its swordsman's coated blows on it land harder
 * ({@link #juggle}, asked by {@code AuraCombat.coat});</li>
 * <li><b>Unmoved</b>'s stone: its swordsman hardened (Resistance, and nothing knocks them back) until it wears off;</li>
 * <li><b>Frostbite</b>'s crusts on a foe, which freeze it at the third;</li>
 * <li><b>Null Parry</b>'s silence: a player can't cast, play an art or use the Aura key (but to guard) while it lasts
 * ({@link #silenced}, asked by {@code SwordStrings.check} and {@code Aura.press}); a creature can't cast, draw a bow or light a
 * fuse;</li>
 * <li><b>Starlit</b>'s stars on a foe, for the next Starlit art to burst ({@link #star}, {@link #starred});</li>
 * <li><b>Frenzy</b>'s quickened blade, a stack for each cut and each blow that lands after, gone when it runs out;</li>
 * <li><b>Thousand Moments</b>' foes stopped in time, and what its swordsman's blows store on them meanwhile;</li>
 * <li>where each Hourglass swordsman last left the ground, for <b>Rewind Leap</b> to snap them back to.</li>
 * </ul>
 */
public final class ArtWards {
	private ArtWards() {}

	/** Glacier Mirror's exact original-body lifetime, independent of the other wards. */
	public static final class Mirror {
		private final ServerPlayer player;
		private final UUID id;
		private final ReleasedArtOwner owner;
		private final long until;
		private Mirror(ServerPlayer player, ReleasedArtOwner owner, int ticks) {
			this.player = player; this.id = player.getUUID(); this.owner = owner; this.until = owner.level().getGameTime() + ticks;
		}
		/** Expiry and replacement end this pane, without retiring a reflection already released by it. */
		public boolean active() {
			boolean active = owner.valid() && owner.level().getGameTime() <= until && MIRROR.get(id) == this;
			if (!active) MIRROR.remove(id, this);
			return active;
		}
	}
	private static final Map<UUID, Mirror> MIRROR = new HashMap<>();
	private static final Map<UUID, Long> EYE = new HashMap<>();
	private static final HardeningLeases<ServerPlayer> HARDENED = new HardeningLeases<>();
	/** A foe Updraft threw: by the foe, its thrower and until when. */
	private record Juggle(UUID owner, long until) {}

	private static final Map<UUID, Juggle> JUGGLED = new HashMap<>();
	/** Frostbite's crusts on each foe: how many, and when the last landed. */
	private static final Map<UUID, long[]> CRUSTS = new HashMap<>();

	/** Who is silenced, until when (game time), and the creature itself (a silenced creature is kept from its bow and its fuse). */
	private record Silenced(LivingEntity who, long until) {}

	private static final Map<UUID, Silenced> SILENCED = new HashMap<>();
	/** When each player was last silenced by an art (no art silences them again for {@link ArtRules#SILENCE_REST}). */
	private static final Map<UUID, Long> SILENCED_AT = new HashMap<>();

	/** A star on a foe: whose, until when, and the foe (for its glint). */
	private record Star(UUID owner, LivingEntity foe, long until) {}

	private static final Map<UUID, Star> STARS = new HashMap<>();

	/** A swordsman's frenzy: how many stacks, until when. */
	private record Frenzied(int stacks, long until) {}

	private static final Map<UUID, Frenzied> FRENZY = new HashMap<>();

	/** A foe stopped by Thousand Moments: whose, until when, and the damage its swordsman's blows have stored on it. */
	private static final class Stopped {
		final UUID owner;
		final long until;
		double stored;

		Stopped(UUID owner, long until) {
			this.owner = owner;
			this.until = until;
		}
	}

	private static final Map<UUID, Stopped> STOPPED = new HashMap<>();

	/** Where each Hourglass swordsman last stood on the ground, and where and when they last left it. */
	private static final Map<UUID, Vec3> GROUND = new HashMap<>();
	private record Leap(Vec3 from, long at) {}

	private static final Map<UUID, Leap> LEFT = new HashMap<>();

	private static final Identifier UNMOVED = Wildercord.id("art_unmoved");
	private static final Identifier FRENZY_SPEED = Wildercord.id("art_frenzy");

	static void init() {
		ServerTickEvents.END_SERVER_TICK.register(ArtWards::tick);
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player) retireHardening(player, null);
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			retireHardening(oldPlayer, null);
			syncHardening(newPlayer);
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> retireHardening(handler.player, null));
		// A frenzied swordsman's blows feed the frenzy; a blow on a foe Thousand Moments stopped is stored to land again.
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			if (taken > 0 && source.getEntity() instanceof ServerPlayer player && source.getDirectEntity() == player
					&& (source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.SPEAR))) {
				blowLanded(player, entity, taken);
			}
		});
	}

	// ------------------------------------------------------------------ projectiles

	public static void mirror(ServerPlayer player, int ticks) {
		mirror(player, ReleasedArtOwner.capture(player), ticks);
	}

	static Mirror mirror(ServerPlayer player, ReleasedArtOwner owner, int ticks) {
		if (!owner.owns(player)) throw new IllegalArgumentException("A mirror belongs to its original body");
		Mirror mirror = new Mirror(player, owner, ticks);
		if (owner.valid()) MIRROR.put(player.getUUID(), mirror);
		return mirror;
	}

	public static void eye(ServerPlayer player, int ticks) {
		EYE.put(player.getUUID(), player.level().getGameTime() + ticks);
	}

	public static boolean mirrored(Player player) {
		Mirror mirror = MIRROR.get(player.getUUID());
		return mirror != null && mirror.player == player && mirror.active();
	}

	public static boolean inEye(Player player) {
		Long until = EYE.get(player.getUUID());
		return until != null && player.level().getGameTime() <= until;
	}

	/**
	 * How an art's ward turns a projectile about to hit {@code player}, or null when none does: Glacier Mirror's ice sends one from
	 * in front back at whoever loosed it, a quarter faster (it's the swordsman's from then on); the Eye of the Storm's wind turns one
	 * from any side aside, slowed, past them.
	 */
	public static ProjectileDeflection deflection(ServerPlayer player, Projectile projectile) {
		Mirror mirror = MIRROR.get(player.getUUID());
		if (mirror != null && mirror.player == player && mirror.active()
			&& projectile.level() == mirror.owner.level() && dev.wildercord.aura.AuraGuard.facing(player, projectile.position())) {
			Entity shooter = projectile.getOwner();
			double speed = Math.min(3.0, Math.max(0.6, projectile.getDeltaMovement().length()) * 1.25);
			RimeArts.mirrorTurns(player, projectile.position());
			Scheduler.later(1, () -> {
				// A legitimate last-tick reflection still transfers ownership after the ward expires.
				if (mirror.owner.valid() && !projectile.isRemoved() && projectile.level() == mirror.owner.level()
					&& mirror.owner.level().getEntity(projectile.getUUID()) == projectile) {
					projectile.setOwner(player);
				}
			});
			return (turned, by, random, power) -> {
				if (!mirror.owner.valid() || turned != projectile || turned.isRemoved() || turned.level() != mirror.owner.level()) return;
				Vec3 at = turned.position();
				Vec3 aim = shooter != null && shooter.isAlive() && shooter.level() == turned.level() && shooter.distanceTo(player) < 32
					? shooter.getBoundingBox().getCenter().subtract(at) : player.getViewVector(1.0F);
				if (aim.lengthSqr() < 1.0E-4) {
					aim = player.getViewVector(1.0F);
				}
				turned.setDeltaMovement(aim.normalize().scale(speed));
				turned.needsSync = true;
			};
		}
		if (inEye(player)) {
			GaleArts.eyeTurns(player, projectile.position());
			return (turned, by, random, power) -> {
				Vec3 v = turned.getDeltaMovement();
				Vec3 flat = new Vec3(v.x, 0, v.z);
				if (flat.lengthSqr() < 1.0E-4) {
					flat = new Vec3(1, 0, 0);
				}
				// Swung round the swordsman, the way the wind turns: square to its path, and up a little.
				Vec3 side = new Vec3(-flat.z, 0, flat.x).normalize();
				double speed = Math.max(0.3, v.length() * 0.6);
				turned.setDeltaMovement(side.scale(speed).add(flat.normalize().scale(speed * 0.4)).add(0, 0.25, 0));
				turned.needsSync = true;
			};
		}
		return null;
	}

	// ------------------------------------------------------------------ the juggle

	/** {@code foe} was thrown by {@code owner}'s Updraft: their coated blows land harder while it's still in the air. */
	public static void juggled(ServerPlayer owner, LivingEntity foe, int ticks) {
		JUGGLED.put(foe.getUUID(), new Juggle(owner.getUUID(), owner.level().getGameTime() + ticks));
	}

	/** What {@code player}'s coated blow on {@code target} is multiplied by for the juggle: {@link ArtRules#UPDRAFT_JUGGLE} or 1. */
	public static double juggle(ServerPlayer player, LivingEntity target) {
		if (JUGGLED.isEmpty()) {
			return 1.0;
		}
		Juggle juggle = JUGGLED.get(target.getUUID());
		if (juggle == null || !juggle.owner().equals(player.getUUID()) || player.level().getGameTime() > juggle.until() || target.onGround()) {
			return 1.0;
		}
		return ArtRules.UPDRAFT_JUGGLE;
	}

	// ------------------------------------------------------------------ hardened

	/** A prepared Unmoved grant, owned by the admitted original body and consumed only after payment finalizes. */
	public static final class Hardened {
		private final ServerPlayer player;
		private final HardeningLeases.Lease<ServerPlayer> lease;
		private final int ticks;

		private Hardened(ServerPlayer player, ReleasedArtOwner owner, long acceptedAt, int ticks) {
			if (!owner.owns(player)) throw new IllegalArgumentException("Hardening belongs to its original body");
			this.player = player; this.ticks = ticks;
			lease = HARDENED.receipt(player, owner.level(), HardeningLeases.Source.UNMOVED,
				acceptedAt, ticks, owner::valid, owner.level()::getGameTime);
		}

		/** One finalized paid dispatch grants one native Resistance effect and one nonrefreshable knockback lease. */
		public boolean grant() {
			if (!HARDENED.grant(lease)) { syncHardening(player); return false; }
			player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, ticks, 0, false, true, true));
			syncHardening(player);
			return HARDENED.active(lease);
		}

		public boolean active() {
			boolean active = HARDENED.active(lease);
			syncHardening(player);
			return active;
		}

		/** Retiring this grant leaves another grant or Unmoving Mountain's refresh alone. */
		public void retire() {
			HARDENED.retire(lease);
			syncHardening(player);
		}
	}

	/** Captures a receipt without granting a ward, through {@code acceptedAt + ticks} inclusive in the original world's game time. */
	public static Hardened unmoved(ServerPlayer player, ReleasedArtOwner owner, long acceptedAt, int ticks) {
		return new Hardened(player, owner, acceptedAt, ticks);
	}

	/** Unmoving Mountain's existing refresh: Resistance I and knockback immunity for {@code ticks}. */
	public static void harden(ServerPlayer player, int ticks) {
		ReleasedArtOwner owner = ReleasedArtOwner.capture(player);
		var lease = HARDENED.receipt(player, owner.level(), HardeningLeases.Source.MOUNTAIN,
			owner.level().getGameTime(), ticks, owner::valid, owner.level()::getGameTime);
		if (!HARDENED.grant(lease)) { syncHardening(player); return; }
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, ticks, 0, false, true, true));
		syncHardening(player);
	}

	public static boolean hardened(Player player) {
		return player instanceof ServerPlayer original && syncHardening(original);
	}

	/** Reconciles the single modifier on this exact body, never on a UUID-resolved replacement. */
	private static boolean syncHardening(ServerPlayer player) {
		boolean active = HARDENED.active(player);
		AttributeInstance resist = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		if (resist != null) {
			if (active) {
				if (resist.getModifier(UNMOVED) == null)
					resist.addOrUpdateTransientModifier(new AttributeModifier(UNMOVED, 1.0, AttributeModifier.Operation.ADD_VALUE));
			} else if (resist.getModifier(UNMOVED) != null) resist.removeModifier(UNMOVED);
		}
		return active;
	}

	/** World cleanup shares the actual departure boundary; delayed outer callbacks must never retire a fresh return grant. */
	static void worldChanging(ServerPlayer player) { retireHardening(player, null); }
	static void worldChanged(ServerPlayer player) { syncHardening(player); }

	private static void retireHardening(ServerPlayer player, Object level) {
		HARDENED.retire(player, level);
		syncHardening(player);
	}

	// ------------------------------------------------------------------ crusts

	/** One more crust on {@code foe}: how many it carries now (fresh ones only, at most {@link ArtRules#CRUSTS_TO_FREEZE}). */
	public static int crust(LivingEntity foe) {
		long now = foe.level().getGameTime();
		long[] had = CRUSTS.get(foe.getUUID());
		int n = ArtRules.crusts(had == null ? 0 : (int) had[0], had == null ? Long.MIN_VALUE / 4 : had[1], now);
		if (n >= ArtRules.CRUSTS_TO_FREEZE) {
			CRUSTS.remove(foe.getUUID());
		} else {
			CRUSTS.put(foe.getUUID(), new long[] {n, now});
		}
		if (CRUSTS.size() > 256) {
			CRUSTS.values().removeIf(c -> now - c[1] > ArtRules.CRUST_MEMORY);
		}
		return n;
	}

	/** How many crusts {@code foe} carries now. */
	public static int crusts(LivingEntity foe) {
		long[] had = CRUSTS.get(foe.getUUID());
		return had == null || foe.level().getGameTime() - had[1] > ArtRules.CRUST_MEMORY ? 0 : (int) had[0];
	}

	// ------------------------------------------------------------------ silence

	/**
	 * Silences {@code foe} for {@code ticks} (Null Parry). A player can't cast (the mod's own cast lock, which holds a player two
	 * seconds at most and then leaves them two to answer), play an art or use the Aura key but to guard, for at most
	 * {@link ArtRules#SILENCE_PLAYER_TICKS}, and no art silences them again for {@link ArtRules#SILENCE_REST}. A creature can't cast
	 * (a Runebound's telegraph breaks), draw a bow or crossbow, or light its fuse. A boss only has what it was winding up broken.
	 * Returns how long it holds (0 for none).
	 */
	public static int silence(LivingEntity foe, int ticks) {
		return silence(foe, ticks, () -> true);
	}

	/** A counter may retire during an interruption callback, before any subsequent silence mutation. */
	public static int silence(LivingEntity foe, int ticks, BooleanSupplier permission) {
		if (!permission.getAsBoolean() || !foe.isAlive()) {
			return 0;
		}
		boolean player = foe instanceof Player;
		int t = ArtRules.silence(ticks, player, ArtKit.boss(foe));
		long now = foe.level().getGameTime();
		if (t > 0 && player) {
			Long last = SILENCED_AT.get(foe.getUUID());
			if (last != null && now - last < ArtRules.SILENCE_REST && now >= last) {
				t = 0;
			} else {
				SILENCED_AT.put(foe.getUUID(), now);
			}
		}
		Statuses.interrupt(foe);
		if (!permission.getAsBoolean() || t <= 0) {
			return 0;
		}
		Statuses.silence(foe, t);
		if (!permission.getAsBoolean()) return 0;
		SILENCED.put(foe.getUUID(), new Silenced(foe, now + t));
		quiet(foe, permission);
		return t;
	}

	/** Whether {@code entity} is silenced by an art now (a player: no arts, no Aura key but the guard). */
	public static boolean silenced(LivingEntity entity) {
		Silenced s = SILENCED.get(entity.getUUID());
		return s != null && entity.level().getGameTime() < s.until();
	}

	/** A silenced creature kept quiet: its fuse put out, its bow or crossbow lowered. */
	private static void quiet(LivingEntity foe) {
		quiet(foe, () -> true);
	}

	private static void quiet(LivingEntity foe, BooleanSupplier permission) {
		if (!permission.getAsBoolean()) return;
		if (foe instanceof Creeper creeper && creeper.getSwellDir() > 0) {
			creeper.setSwellDir(-1);
			if (!permission.getAsBoolean()) return;
		}
		if (!(foe instanceof Player) && foe.isUsingItem()) {
			foe.stopUsingItem();
		}
	}

	// ------------------------------------------------------------------ stars

	/** Sets a star of {@code owner}'s on {@code foe} (a Starlit art), for {@link ArtRules#STAR_TICKS}: the next Starlit art bursts it. */
	public static void star(ServerPlayer owner, LivingEntity foe) {
		if (foe.isAlive()) {
			STARS.put(foe.getUUID(), new Star(owner.getUUID(), foe, owner.level().getGameTime() + ArtRules.STAR_TICKS));
		}
	}

	/** Whether {@code foe} carries a star of {@code owner}'s now. */
	public static boolean starred(ServerPlayer owner, LivingEntity foe) {
		Star star = STARS.get(foe.getUUID());
		return star != null && star.owner().equals(owner.getUUID()) && owner.level().getGameTime() <= star.until();
	}

	/** Takes {@code foe}'s star of {@code owner}'s away to burst it; returns whether it had one. */
	public static boolean burstStar(ServerPlayer owner, LivingEntity foe) {
		if (!starred(owner, foe)) {
			return false;
		}
		STARS.remove(foe.getUUID());
		return true;
	}

	// ------------------------------------------------------------------ frenzy

	/**
	 * Adds {@code stacks} to {@code player}'s frenzy (Frenzy's rush began it, each blow that lands after adds one), up to
	 * {@link ArtRules#FRENZY_STACKS}: their blade swings {@link ArtRules#frenzy} faster until it runs out. {@code begin} starts its
	 * time again ({@link ArtRules#FRENZY_TIME}); a blow only adds to the frenzy already burning. Returns the stacks it has now.
	 */
	public static int frenzy(ServerPlayer player, int stacks, boolean begin) {
		long now = player.level().getGameTime();
		Frenzied had = FRENZY.get(player.getUUID());
		boolean burning = had != null && now <= had.until();
		if (!begin && !burning) {
			return 0;
		}
		int next = Math.min(ArtRules.FRENZY_STACKS, (burning ? had.stacks() : 0) + Math.max(0, stacks));
		long until = begin || !burning ? now + ArtRules.FRENZY_TIME : had.until();
		FRENZY.put(player.getUUID(), new Frenzied(next, until));
		AttributeInstance speed = player.getAttribute(Attributes.ATTACK_SPEED);
		if (speed != null) {
			speed.addOrUpdateTransientModifier(new AttributeModifier(FRENZY_SPEED, ArtRules.frenzy(next), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
		return next;
	}

	/** How many stacks of frenzy {@code player} has now. */
	public static int frenzyStacks(Player player) {
		Frenzied f = FRENZY.get(player.getUUID());
		return f == null || player.level().getGameTime() > f.until() ? 0 : f.stacks();
	}

	private static void calm(ServerPlayer player) {
		AttributeInstance speed = player.getAttribute(Attributes.ATTACK_SPEED);
		if (speed != null && speed.getModifier(FRENZY_SPEED) != null) {
			speed.removeModifier(FRENZY_SPEED);
		}
	}

	// ------------------------------------------------------------------ moments stopped

	/** {@code foe} stopped in time by {@code owner}'s Thousand Moments until {@code until}: their blows on it are stored meanwhile. */
	public static void stop(ServerPlayer owner, LivingEntity foe, long until) {
		STOPPED.put(foe.getUUID(), new Stopped(owner.getUUID(), until));
	}

	/** The damage {@code owner}'s blows stored on {@code foe} while it was stopped, taken (0 if none). */
	public static double release(ServerPlayer owner, LivingEntity foe) {
		Stopped s = STOPPED.get(foe.getUUID());
		if (s == null || !s.owner.equals(owner.getUUID())) {
			return 0;
		}
		STOPPED.remove(foe.getUUID());
		return s.stored;
	}

	/** Whether {@code foe} is stopped in time by {@code owner}'s Thousand Moments now. */
	public static boolean stopped(ServerPlayer owner, LivingEntity foe) {
		Stopped s = STOPPED.get(foe.getUUID());
		return s != null && s.owner.equals(owner.getUUID()) && owner.level().getGameTime() <= s.until;
	}

	/** A swordsman's own blow landed on {@code target} for {@code taken}: their frenzy grows, and a stopped foe stores a share. */
	private static void blowLanded(ServerPlayer player, LivingEntity target, float taken) {
		if (!FRENZY.isEmpty() && frenzyStacks(player) > 0 && ArtKit.harmable(player, target)) {
			frenzy(player, 1, false);
		}
		if (!STOPPED.isEmpty()) {
			Stopped s = STOPPED.get(target.getUUID());
			if (s != null && s.owner.equals(player.getUUID()) && player.level().getGameTime() <= s.until) {
				s.stored = ArtRules.stored(s.stored, taken, ArtKit.weapon(player) * ArtRules.THOUSAND_STORE_MAX * ArtKit.scale());
				HourglassArts.storedLook(player, target);
			}
		}
	}

	// ------------------------------------------------------------------ where a leap began

	/**
	 * Where {@code player} last left the ground, if it was within {@link ArtRules#REWIND_MEMORY} ticks (an Hourglass swordsman's,
	 * kept from Flow up), or null.
	 */
	public static Vec3 leapedFrom(ServerPlayer player) {
		Leap leap = LEFT.get(player.getUUID());
		return leap == null || player.level().getGameTime() - leap.at() > ArtRules.REWIND_MEMORY ? null : leap.from();
	}

	/** Notes an Hourglass swordsman's footing each tick: where they stand, and where they left the ground. */
	private static void footing(ServerPlayer player, long now) {
		if (player.onGround()) {
			GROUND.put(player.getUUID(), player.position());
		} else {
			Vec3 ground = GROUND.remove(player.getUUID());
			if (ground != null) {
				LEFT.put(player.getUUID(), new Leap(ground, now));
			}
		}
	}

	// ------------------------------------------------------------------ upkeep

	private static void tick(MinecraftServer server) {
		long tickCount = server.getTickCount();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (dev.wildercord.aura.Aura.stage(player) >= dev.wildercord.aura.AuraRules.FLOW
					&& HourglassArts.METHOD.equals(dev.wildercord.aura.Aura.data(player).method())) {
				footing(player, player.level().getGameTime());
			}
		}
		if (!SILENCED.isEmpty()) {
			for (Iterator<Map.Entry<UUID, Silenced>> it = SILENCED.entrySet().iterator(); it.hasNext(); ) {
				Silenced s = it.next().getValue();
				if (s.who().isRemoved() || !s.who().isAlive() || s.who().level().getGameTime() >= s.until()) {
					it.remove();
				} else {
					quiet(s.who());
				}
			}
		}
		if (!STARS.isEmpty() && tickCount % 10 == 0) {
			for (Iterator<Map.Entry<UUID, Star>> it = STARS.entrySet().iterator(); it.hasNext(); ) {
				Star star = it.next().getValue();
				if (star.foe().isRemoved() || !star.foe().isAlive() || star.foe().level().getGameTime() > star.until()) {
					it.remove();
				} else {
					StarlitArts.starLook(star.foe(), star.until() - star.foe().level().getGameTime());
				}
			}
		}
		if (!FRENZY.isEmpty()) {
			for (Iterator<Map.Entry<UUID, Frenzied>> it = FRENZY.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<UUID, Frenzied> e = it.next();
				ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
				if (player == null) {
					it.remove();
					continue;
				}
				long now = player.level().getGameTime();
				if (now > e.getValue().until() || !player.isAlive()) {
					calm(player);
					it.remove();
				} else if ((now + player.getId()) % Math.max(4, 14 - 2 * e.getValue().stacks()) == 0) {
					CrimsonArts.frenzyLook(player, e.getValue().stacks());
				}
			}
		}
		if (!STOPPED.isEmpty() && tickCount % 20 == 0) {
			STOPPED.values().removeIf(s -> s.until < server.overworld().getGameTime() - 100);
		}
		for (ServerPlayer player : HARDENED.bodies()) {
			if (!syncHardening(player)) {
				if (player.isAlive() && !player.isRemoved()
					&& server.getPlayerList().getPlayer(player.getUUID()) == player)
					Feels.sound(player.level(), player.position(), "earth_creak", 0.4F, 1.2F);
			} else if ((player.level().getGameTime() + player.getId()) % 10 == 0) {
				StoneArts.hardenedLook(player);
			}
		}
		if (server.getTickCount() % 100 == 0) {
			long now = server.overworld().getGameTime();
			// Snapshot: an expired old mirror may only remove its own identity.
			for (Mirror mirror : java.util.List.copyOf(MIRROR.values())) mirror.active();
			EYE.values().removeIf(until -> until < now - 200);
			JUGGLED.values().removeIf(j -> j.until() < now - 200);
		}
	}

	static void forget(UUID id) {
		MIRROR.remove(id);
		EYE.remove(id);
		// UUID-only legacy cleanup must not revoke a replacement body's fresh lease.
		// Exact-body lifecycle callbacks synchronously retire the departing body's leases.
		for (ServerPlayer player : HARDENED.bodies()) if (player.getUUID().equals(id)) syncHardening(player);
		JUGGLED.values().removeIf(j -> j.owner().equals(id));
		SILENCED.remove(id);
		SILENCED_AT.remove(id);
		FRENZY.remove(id);
		GROUND.remove(id);
		LEFT.remove(id);
		STARS.values().removeIf(s -> s.owner().equals(id));
		STOPPED.values().removeIf(s -> s.owner.equals(id));
	}

	static void clear() {
		MIRROR.clear();
		EYE.clear();
		for (ServerPlayer player : HARDENED.bodies()) retireHardening(player, null);
		JUGGLED.clear();
		CRUSTS.clear();
		SILENCED.clear();
		SILENCED_AT.clear();
		STARS.clear();
		FRENZY.clear();
		STOPPED.clear();
		GROUND.clear();
		LEFT.clear();
	}
}
