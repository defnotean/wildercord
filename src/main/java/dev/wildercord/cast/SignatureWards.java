package dev.wildercord.cast;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The signature fusions that stay on a creature and answer what happens to it: Stitchtime (every wound counted,
 * then healed back), Doomclock (every blow winds the clock, then it bursts), Riposte (the next blows sidestepped and
 * answered) and Malison's curse (passed on when its bearer dies). Each is kept by the creature's UUID with the game
 * time it ends, so a lost task, a death, a logout or a restart can't leave one behind, and the listeners cost a map
 * lookup while none are in play. Numbers are in {@link SignatureRules}.
 */
final class SignatureWards {
	private SignatureWards() {}

	/** A Stitchtime on an ally: whose, when it began and ends, what it has counted, and the most it heals back. */
	private static final class Stitch {
		final LivingEntity ally;
		final Cast cast;
		final long began;
		long until;
		double counted;
		double cap;

		Stitch(LivingEntity ally, Cast cast, long began, long until, double cap) {
			this.ally = ally;
			this.cast = cast;
			this.began = began;
			this.until = until;
			this.cap = cap;
		}
	}

	/** A Doomclock ticking on a creature: whose, until when, how tight it's wound, and where it last was. */
	private static final class Clock {
		final LivingEntity target;
		final Cast cast;
		final long until;
		final double power;
		final double radius;
		final Consumer<Runnable> context;
		/** Everyone the cast has touched, so the burst takes new creatures out of its budget, and old ones not again. */
		final Set<Entity> touched;
		double wound;
		long lastWound = Long.MIN_VALUE;
		Vec3 at;

		Clock(LivingEntity target, Cast cast, long until, double power, double radius, Collection<Entity> touched) {
			this.target = target;
			this.cast = cast;
			this.until = until;
			this.power = power;
			this.radius = radius;
			this.context = FusedFrost.context();
			this.touched = new HashSet<>(touched);
			this.at = target.getBoundingBox().getCenter();
		}
	}

	/** A Riposte on an ally: whose, until when, the blows it still answers, and how hard. */
	private static final class Guard {
		final LivingEntity ally;
		final Cast cast;
		final long until;
		final double power;
		final Consumer<Runnable> context;
		int blows = SignatureRules.RIPOSTE_BLOWS;

		Guard(LivingEntity ally, Cast cast, long until, double power) {
			this.ally = ally;
			this.cast = cast;
			this.until = until;
			this.power = power;
			this.context = FusedFrost.context();
		}

		boolean over(long now) {
			return blows <= 0 || now > until || ally.isRemoved() || !ally.isAlive() || ally.level() != cast.level || !cast.alive();
		}
	}

	/** Malison's curse on a creature: whose, until when, and how far it reaches when it passes on. */
	private record Curse(LivingEntity target, Cast cast, long until, double reach, Consumer<Runnable> context) {}

	private static final Map<UUID, Stitch> STITCHES = new HashMap<>();
	private static final Map<UUID, Clock> CLOCKS = new HashMap<>();
	private static final Map<UUID, Guard> GUARDS = new HashMap<>();
	private static final Map<UUID, Curse> CURSES = new HashMap<>();
	/** Set while an answer is being dealt, so an answer never sets off another (two Ripostes can't trade blows forever). */
	private static boolean answering;
	/** Set while a clock bursts, so its own burst never winds another clock it catches into bursting sooner. */
	private static boolean bursting;

	static void init() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(SignatureWards::allowDamage);
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> afterDamage(entity, source, damage, blocked));
		ServerLivingEntityEvents.AFTER_DEATH.register(SignatureWards::afterDeath);
		ServerTickEvents.END_SERVER_TICK.register(SignatureWards::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			STITCHES.clear();
			CLOCKS.clear();
			GUARDS.clear();
			CURSES.clear();
			answering = false;
			bursting = false;
		});
	}

	// ------------------------------------------------------------------ putting them on

	/**
	 * Stitchtime: heals {@code ally} 4 now and counts every wound they take for {@code ticks}; when the time is up it
	 * heals it all back, 12 at most. On an ally already stitched, the stitch runs on (never past three times its
	 * length from when it began), keeping what it counted, and the heal back may reach the higher of the two caps.
	 */
	static void stitch(Cast cast, LivingEntity ally, double power, int ticks) {
		long now = cast.level.getGameTime();
		ally.heal((float) (SignatureRules.STITCH_HEAL * power));
		double cap = SignatureRules.STITCH_CAP * power;
		Stitch old = STITCHES.get(ally.getUUID());
		if (old != null && old.ally == ally && now < old.until) {
			old.until = SignatureRules.stitchUntil(old.began, old.until, now, ticks);
			old.cap = Math.max(old.cap, cap);
			SignatureVfx.stitchRenew(cast.level, ally);
			return;
		}
		STITCHES.put(ally.getUUID(), new Stitch(ally, cast, now, now + ticks, cap));
		SignatureVfx.stitch(cast.level, ally);
	}

	/**
	 * Doomclock: a clock on {@code target} for {@code ticks}, bursting within {@code radius} when it runs out. On a
	 * creature that already has one ticking, it's a blow that winds that one instead of a second clock.
	 */
	static void doomclock(Cast cast, LivingEntity target, double power, int ticks, double radius, Collection<Entity> touched) {
		long now = cast.level.getGameTime();
		Clock old = CLOCKS.get(target.getUUID());
		if (old != null && old.target == target && now < old.until) {
			windUp(old, now);
			return;
		}
		CLOCKS.put(target.getUUID(), new Clock(target, cast, now + ticks, power, radius, touched));
		SignatureVfx.doomclock(cast.level, target, ticks);
	}

	/** Riposte on {@code ally} for {@code ticks}: its next 2 blows sidestepped and answered. A newer one takes over, fresh. */
	static void riposte(Cast cast, LivingEntity ally, double power, int ticks) {
		boolean fresh = !GUARDS.containsKey(ally.getUUID());
		GUARDS.put(ally.getUUID(), new Guard(ally, cast, cast.level.getGameTime() + ticks, power));
		if (fresh || !cast.passive) {
			SignatureVfx.riposte(cast.level, ally);
		}
	}

	/** Malison's curse on {@code target} until {@code ticks} from now: if it dies cursed, the curse passes on within {@code reach}. */
	static void curse(Cast cast, LivingEntity target, int ticks, double reach) {
		long until = cast.level.getGameTime() + ticks;
		Curse old = CURSES.get(target.getUUID());
		if (old != null && old.target() == target) {
			until = Math.max(until, old.until());
		}
		CURSES.put(target.getUUID(), new Curse(target, cast, until, reach, FusedFrost.context()));
	}

	// ------------------------------------------------------------------ events

	/** Riposte: a blow from an enemy is sidestepped, and answered. */
	private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		if (GUARDS.isEmpty() || answering) {
			return true;
		}
		Guard guard = GUARDS.get(entity.getUUID());
		if (guard == null || guard.ally != entity || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || !(entity.level() instanceof ServerLevel level)) {
			return true;
		}
		if (guard.over(level.getGameTime())) {
			GUARDS.remove(entity.getUUID(), guard);
			return true;
		}
		LivingEntity striker = striker(source, entity);
		// Only a blow someone struck is seen coming: a fall, a fire, the void aren't.
		if (striker == null || !Targets.canHarm(guard.cast.caster, striker)) {
			return true;
		}
		guard.blows--;
		if (guard.blows <= 0) {
			GUARDS.remove(entity.getUUID(), guard);
		}
		sidestep(entity, striker);
		SignatureVfx.riposteDodge(level, entity, striker);
		if (striker.level() == level && striker.distanceToSqr(entity) <= SignatureRules.RIPOSTE_REACH * SignatureRules.RIPOSTE_REACH) {
			answering = true;
			try {
				guard.context.accept(() -> Effects.hurt(guard.cast, striker, SignatureFusions.magic(guard.cast), SignatureRules.RIPOSTE_DAMAGE * guard.power));
			} finally {
				answering = false;
			}
		}
		return false;
	}

	/** A step aside from the blow, along the ground: never up into the air, never toward the striker. */
	private static void sidestep(LivingEntity ally, LivingEntity striker) {
		Vec3 from = Effects.horizontal(ally.position().subtract(striker.position()), ally.getLookAngle());
		Vec3 aside = new Vec3(-from.z, 0, from.x).scale(ally.getRandom().nextBoolean() ? 0.55 : -0.55);
		Effects.push(ally, aside.add(from.scale(0.15)).add(0, 0.1, 0));
	}

	/** Stitchtime counts the wound; Doomclock winds on the blow. */
	private static void afterDamage(LivingEntity entity, DamageSource source, float damage, boolean blocked) {
		if (blocked || damage <= 0 || STITCHES.isEmpty() && CLOCKS.isEmpty() || !(entity.level() instanceof ServerLevel level)) {
			return;
		}
		long now = level.getGameTime();
		Stitch stitch = STITCHES.get(entity.getUUID());
		if (stitch != null && stitch.ally == entity && now < stitch.until) {
			stitch.counted += damage;
			SignatureVfx.stitchCount(level, entity);
		}
		Clock clock = CLOCKS.get(entity.getUUID());
		if (clock != null && clock.target == entity && now < clock.until && !bursting) {
			windUp(clock, now);
		}
	}

	/** A blow winds a clock 2 tighter (at most four a second, and 12 in all). */
	private static void windUp(Clock clock, long now) {
		if (!SignatureRules.winds(clock.lastWound, now) || clock.wound >= SignatureRules.DOOMCLOCK_WOUND) {
			return;
		}
		clock.lastWound = now;
		clock.wound = SignatureRules.wind(clock.wound);
		SignatureVfx.doomclockWind(clock.cast.level, clock.target, clock.wound);
	}

	/** Deaths: the dead lose their wards; a cursed one passes its curse on. A clock still ticks where its bearer fell. */
	private static void afterDeath(LivingEntity entity, DamageSource source) {
		UUID id = entity.getUUID();
		if (!STITCHES.isEmpty()) {
			STITCHES.remove(id);
		}
		if (!GUARDS.isEmpty()) {
			GUARDS.remove(id);
		}
		if (CURSES.isEmpty()) {
			return;
		}
		Curse curse = CURSES.remove(id);
		if (curse == null || curse.target() != entity || !(entity.level() instanceof ServerLevel level) || level.getGameTime() > curse.until()) {
			return;
		}
		Vec3 at = entity.getBoundingBox().getCenter();
		int left = SignatureRules.malisonLeft(curse.until(), level.getGameTime());
		// A tick later, clear of the death.
		Scheduler.later(1, () -> curse.context().accept(() -> passOn(curse, level, at, left)));
	}

	/** Malison's curse passes on: to up to 3 enemies within its reach, for the time it had left. Those don't pass it on again. */
	private static void passOn(Curse curse, ServerLevel level, Vec3 at, int ticks) {
		Cast cast = curse.cast();
		if (!cast.alive() || cast.level != level) {
			return;
		}
		List<LivingEntity> heirs = new ArrayList<>();
		for (LivingEntity t : SignatureFusions.nearest(cast, at, curse.reach(), SignatureFusions.MAX_IN_AREA)) {
			if (heirs.size() < SignatureRules.MALISON_SPREAD && t != curse.target() && t.isAlive()) {
				heirs.add(t);
			}
		}
		heirs = heirs.subList(0, cast.takeEntities(heirs.size()));
		if (heirs.isEmpty()) {
			return;
		}
		SignatureVfx.malisonPass(level, at, heirs);
		for (LivingEntity t : heirs) {
			Effects.hex(cast, t, ticks);
		}
	}

	/** Who struck: the creature behind the blow (an archer, a caster) or, failing that, the one that landed it. */
	private static LivingEntity striker(DamageSource source, LivingEntity victim) {
		LivingEntity found = null;
		if (source.getEntity() instanceof LivingEntity causing) {
			found = causing;
		} else if (source.getDirectEntity() instanceof LivingEntity direct) {
			found = direct;
		}
		return found == null || found == victim || !found.isAlive() ? null : found;
	}

	// ------------------------------------------------------------------ ticking

	private static void tick(MinecraftServer server) {
		if (!STITCHES.isEmpty()) {
			tickStitches();
		}
		if (!CLOCKS.isEmpty()) {
			tickClocks();
		}
		if (server.getTickCount() % 20 == 0) {
			if (!GUARDS.isEmpty()) {
				tickGuards();
			}
			if (!CURSES.isEmpty()) {
				CURSES.values().removeIf(c -> c.target().isRemoved() || !(c.target().level() instanceof ServerLevel level) || level.getGameTime() > c.until());
			}
		}
	}

	/** Every tick: stitches whose time is up heal back all they counted; the gone ones are dropped. */
	private static void tickStitches() {
		for (Iterator<Stitch> it = STITCHES.values().iterator(); it.hasNext(); ) {
			Stitch stitch = it.next();
			LivingEntity ally = stitch.ally;
			if (ally.isRemoved() || !ally.isAlive() || !(ally.level() instanceof ServerLevel level) || level != stitch.cast.level) {
				it.remove();
				continue;
			}
			if (level.getGameTime() < stitch.until) {
				continue;
			}
			it.remove();
			double back = SignatureRules.stitchBack(stitch.counted, stitch.cap);
			if (back > 0) {
				ally.heal((float) back);
			}
			SignatureVfx.stitchClose(level, ally, back);
		}
	}

	/** Every tick: each clock follows its bearer, and one whose time is up bursts (where its bearer fell, if it did). */
	private static void tickClocks() {
		List<Clock> due = new ArrayList<>();
		for (Iterator<Clock> it = CLOCKS.values().iterator(); it.hasNext(); ) {
			Clock clock = it.next();
			LivingEntity t = clock.target;
			ServerLevel level = clock.cast.level;
			if (!clock.cast.alive()) {
				it.remove();
				continue;
			}
			// Carried off into another world, or gone without dying: it stops.
			if (t.level() != level || t.isRemoved() && !t.isDeadOrDying()) {
				it.remove();
				continue;
			}
			if (t.isAlive()) {
				clock.at = t.getBoundingBox().getCenter();
			}
			long left = clock.until - level.getGameTime();
			if (left <= 0) {
				it.remove();
				due.add(clock);
			} else if (left % 10 == 0 && t.isAlive()) {
				SignatureVfx.doomclockTick(level, t, (int) left, clock.wound);
			}
		}
		for (Clock clock : due) {
			clock.context.accept(() -> burst(clock));
		}
	}

	/** A clock at zero: 8 damage and all it was wound, to its bearer (if it lives) and every enemy within its radius. */
	private static void burst(Clock clock) {
		Cast cast = clock.cast;
		ServerLevel level = cast.level;
		SignatureVfx.doomclockBurst(level, clock.at, clock.radius, clock.wound);
		double damage = SignatureRules.doomclockBurst(clock.wound) * clock.power;
		bursting = true;
		try {
			for (LivingEntity t : SignatureFusions.take(cast, SignatureFusions.nearest(cast, clock.at, clock.radius, SignatureFusions.MAX_IN_AREA), clock.touched)) {
				Effects.hurt(cast, t, SignatureFusions.magic(cast), damage);
				Vec3 away = t.getBoundingBox().getCenter().subtract(clock.at);
				if (!Spirits.isBoss(t)) {
					Effects.push(t, (away.lengthSqr() < 1.0E-4 ? new Vec3(0, 1, 0) : away.normalize()).scale(0.6).add(0, 0.3, 0));
				}
			}
		} finally {
			bursting = false;
		}
	}

	/** Once a second: each Riposte's glimmer, and the ended ones dropped. */
	private static void tickGuards() {
		for (Iterator<Guard> it = GUARDS.values().iterator(); it.hasNext(); ) {
			Guard guard = it.next();
			if (!(guard.ally.level() instanceof ServerLevel level) || guard.over(level.getGameTime())) {
				it.remove();
				continue;
			}
			SignatureVfx.riposteAura(level, guard.ally, guard.blows);
		}
	}
}
