package dev.wildercord.cast;

import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Fused effects of frost, made only at the Fusion Altar (see {@code spell.Fusions}); {@link FusedEffects}
 * hands each of them here. Their look is in {@link FusedFrostVfx}; what stays on a creature and answers
 * what happens to it (Frostbloom, Geode, Black Ice's shatter, Cryostasis) is in {@link FusedFrostWards}.
 * Numbers match the rune descriptions in {@code Runes}.
 * <ul>
 *   <li><b>Blizzard</b> (frost and wind): a storm over the ground that slows, chills and bites.</li>
 *   <li><b>Frostbloom</b> (frost and life): regeneration, and whatever strikes the ally is frozen stiff.</li>
 *   <li><b>Black Ice</b> (frost and void): frozen and weakened, and brittle: one that dies shatters.</li>
 *   <li><b>Rime Seal</b> (frost and arcane): a seal on the ground that freezes whoever stands on it.</li>
 *   <li><b>Cryostasis</b> (frost and time): an ally sealed in ice, untouchable, healing; not again for 10 seconds.</li>
 *   <li><b>Frostbite</b> (frost and blood): a bite, and cold that sets in beat by beat until it freezes.</li>
 *   <li><b>Absolute Zero</b> (frost and frost): slowed to a crawl, or frozen solid if already cold.</li>
 *   <li><b>Fossilize</b> (earth and time): slowed more each second, then stone, then cracked.</li>
 *   <li><b>Geode</b> (earth and arcane): crystal armour that cuts whatever strikes it.</li>
 * </ul>
 */
final class FusedFrost {
	private FusedFrost() {}

	/** At most this many targets get a lingering part of their own, so one hit can't flood the server (as in FusedEffects). */
	static final int MAX_TARGETS = 8;
	/** At most this many enemies an area of these runes touches each time it pulses. */
	static final int MAX_IN_AREA = 16;

	/**
	 * The lingering effects running on each creature (Frostbite, Fossilize), by rune, target and
	 * caster: a newer one from the same caster takes over from the older instead of running beside it.
	 */
	private static final Map<String, Object> RUNNING = new HashMap<>();
	/** Until when (game time) Absolute Zero can't freeze each creature solid again (see {@link FusedFrostRules#ZERO_LOCKOUT_TICKS}). */
	private static final Map<java.util.UUID, Long> ZERO_LOCKED = new HashMap<>();

	/**
	 * A Blizzard or Rime Seal one caster laid: where, and until when. Cast again on its own spot (a Zone's next
	 * pulse, an Echo, a Split copy landing beside it), it keeps going instead of a second one stacking on it.
	 */
	private static final class Area {
		final UUID caster;
		final ServerLevel level;
		final Vec3 centre;
		long until;
		/** Kept going, never past this: three times its length. */
		final long cap;

		Area(Cast cast, Vec3 centre, int ticks) {
			this.caster = cast.caster.getUUID();
			this.level = cast.level;
			this.centre = centre;
			long now = cast.level.getGameTime();
			this.until = now + ticks;
			this.cap = now + 3L * ticks;
		}
	}

	private static final List<Area> BLIZZARDS = new ArrayList<>();
	private static final List<Area> SEALS = new ArrayList<>();

	/** Registers anything these effects listen for (damage, deaths, ticks); called once at startup. */
	static void init() {
		FusedFrostWards.init();
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			RUNNING.clear();
			ZERO_LOCKED.clear();
			BLIZZARDS.clear();
			SEALS.clear();
		});
	}

	/** Does {@code node}'s effect if it's one of these, and says whether it was. */
	static boolean apply(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, List<LivingEntity> helped, List<LivingEntity> harmed,
			double power, double duration, int amplify) {
		if (!Effects.builtIn(node.effect)) {
			return false;
		}
		switch (node.effect.path()) {
			case "blizzard" -> blizzard(cast, hit, 3.0 * SpellNumbers.effectRadius(node), power, Effects.ticks(4, duration));
			case "frostbloom" -> helped.forEach(t -> frostbloom(cast, t, duration, amplify));
			case "black_ice" -> {
				for (int i = 0; i < harmed.size(); i++) {
					blackIce(cast, harmed.get(i), power, duration, i < MAX_TARGETS);
				}
			}
			case "rime_seal" -> rimeSeal(cast, hit, 1.5 * SpellNumbers.effectRadius(node), power, duration);
			case "cryostasis" -> first(helped).forEach(t -> FusedFrostWards.cryostasis(cast, t, Math.min(Effects.ticks(2, duration), FusedFrostRules.SEAL_MAX_TICKS), 6 * power));
			case "frostbite" -> {
				for (int i = 0; i < harmed.size(); i++) {
					frostbite(cast, harmed.get(i), power, duration, i < MAX_TARGETS);
				}
			}
			case "absolute_zero" -> {
				for (int i = 0; i < harmed.size(); i++) {
					absoluteZero(cast, harmed.get(i), power, duration, i < MAX_TARGETS);
				}
			}
			case "fossilize" -> first(harmed).forEach(t -> fossilize(cast, t, power, duration));
			case "geode" -> helped.forEach(t -> geode(cast, t, power, duration, amplify));
			default -> {
				return false;
			}
		}
		return true;
	}

	// ------------------------------------------------------------------ Blizzard (frost and wind)

	/**
	 * Blizzard: a storm {@code radius} round where it lands for {@code ticks}. Twice a second every
	 * enemy in it is slowed (Slowness II) and chilled (brittle for Shatter, frost on its skin); once a
	 * second it takes 1 damage. Cast again on its own storm, the storm keeps going (up to three times its
	 * length) instead of a second one howling on top of it.
	 */
	private static void blizzard(Cast cast, Cast.Hit hit, double radius, double power, int ticks) {
		ServerLevel level = cast.level;
		Vec3 centre = hit.self() ? cast.caster.position() : CastEngine.ground(level, hit.point().add(0, 0.5, 0));
		if (renewed(BLIZZARDS, cast, centre, ticks)) {
			return;
		}
		Area storm = new Area(cast, centre, ticks);
		BLIZZARDS.add(storm);
		DamageSource cold = frost(cast);
		FusedFrostVfx.blizzardOpen(level, centre, radius, ticks);
		lasting(cast, storm, BLIZZARDS, 5, tick -> {
			FusedFrostVfx.blizzard(level, centre, radius, tick);
			if (tick % 10 != 0) {
				return;
			}
			boolean bite = tick % 20 == 0;
			for (LivingEntity t : inside(cast, centre, radius, 3.2)) {
				t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 1, false, true), cast.caster);
				chill(t, 30);
				Reactions.mark(t, Reactions.Mark.FROZEN, 30);
				if (bite) {
					Effects.hurt(cast, t, cold, power);
					FusedFrostVfx.blizzardBite(level, t);
				}
			}
		}, () -> {
			if (cast.alive()) {
				FusedFrostVfx.blizzardEnd(level, centre, radius);
			}
		});
	}

	// ------------------------------------------------------------------ Frostbloom (frost and life)

	/** Frostbloom: Regeneration II for 5 seconds, and for 8 whatever strikes the ally gets Slowness III for 2 (see {@link FusedFrostWards}). */
	private static void frostbloom(Cast cast, LivingEntity t, double duration, int amplify) {
		t.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Effects.ticks(5, duration), Math.min(3, 1 + Math.max(0, amplify)), false, true));
		boolean fresh = FusedFrostWards.frostbloom(cast, t, Effects.ticks(8, duration), Effects.ticks(2, duration));
		if (fresh || !cast.passive) {
			FusedFrostVfx.frostbloom(cast.level, t);
		}
	}

	// ------------------------------------------------------------------ Black Ice (frost and void)

	/**
	 * Black Ice: frozen for 1.5 seconds (1 on players) and Weakness II for 5; and, if {@code brittle},
	 * one that dies within 5 seconds shatters (see {@link FusedFrostWards#brittle}).
	 */
	private static void blackIce(Cast cast, LivingEntity t, double power, double duration, boolean brittle) {
		int hold = Effects.ticks(t instanceof Player ? 1 : 1.5, duration);
		t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Effects.ticks(5, duration), 1, false, true), cast.caster);
		freeze(t, hold);
		FusedFrostVfx.blackIce(cast.level, t, hold);
		if (brittle) {
			FusedFrostWards.brittle(cast, t, Effects.ticks(5, duration), power);
		}
	}

	// ------------------------------------------------------------------ Rime Seal (frost and arcane)

	/**
	 * Rime Seal: a seal {@code radius} round where it lands, for 6 seconds. An enemy that stands on it
	 * for a second freezes solid for 1.5 seconds (1 on players) and takes 3 damage; each only once. Written
	 * again on its own seal, the seal lasts longer (up to three times) instead of a second one lying on it.
	 */
	private static void rimeSeal(Cast cast, Cast.Hit hit, double radius, double power, double duration) {
		ServerLevel level = cast.level;
		Vec3 centre = hit.self() ? cast.caster.position() : CastEngine.ground(level, hit.point().add(0, 0.5, 0));
		int ticks = Effects.ticks(6, duration);
		if (renewed(SEALS, cast, centre, ticks)) {
			return;
		}
		Area seal = new Area(cast, centre, ticks);
		SEALS.add(seal);
		DamageSource cold = frost(cast);
		// When each enemy standing on it now was first seen there, and who it has already frozen.
		Map<UUID, Integer> since = new HashMap<>();
		Set<UUID> sealed = new HashSet<>();
		FusedFrostVfx.rimeSealOpen(level, centre, radius, ticks);
		lasting(cast, seal, SEALS, 5, tick -> {
			if (tick % 10 == 0) {
				FusedFrostVfx.rimeSeal(level, centre, radius, tick);
			}
			Set<UUID> standing = new HashSet<>();
			for (LivingEntity t : standingOn(cast, centre, radius)) {
				UUID id = t.getUUID();
				if (sealed.contains(id)) {
					continue;
				}
				standing.add(id);
				int first = since.computeIfAbsent(id, k -> tick);
				if (!FusedFrostRules.stoodLongEnough(first, tick)) {
					FusedFrostVfx.rimeCharge(level, t, (tick - first) / 5);
					continue;
				}
				sealed.add(id);
				int hold = Effects.ticks(t instanceof Player ? 1 : 1.5, duration);
				freeze(t, hold);
				Effects.hurt(cast, t, cold, 3 * power);
				FusedFrostVfx.rimeFreeze(level, t, centre, hold);
			}
			// Stepping off starts the count again.
			since.keySet().retainAll(standing);
		}, () -> {
			if (cast.alive()) {
				FusedFrostVfx.rimeSealClose(level, centre, radius);
			}
		});
	}

	/** Enemies standing on a seal lying at {@code centre}: on or just above it, not flying over it. */
	private static List<LivingEntity> standingOn(Cast cast, Vec3 centre, double radius) {
		List<LivingEntity> out = new ArrayList<>();
		for (Entity e : cast.level.getEntities((Entity) null, new AABB(centre, centre).inflate(radius + 1, 2.0, radius + 1),
				e -> Targets.canHarm(cast.caster, e))) {
			double dy = e.getY() - centre.y;
			if (dy >= -0.6 && dy <= 1.3 && horizontal(e.position(), centre) <= radius + e.getBbWidth() / 2 && out.size() < MAX_IN_AREA) {
				out.add((LivingEntity) e);
			}
		}
		return out;
	}

	// ------------------------------------------------------------------ Frostbite (frost and blood)

	/**
	 * Frostbite: 3 damage, then 1 a second for 5 seconds as the cold sets in (Slowness I, II, then III),
	 * and at the end a freeze of a second. The same caster biting again starts the cold over. Every target
	 * is bitten; only if {@code sets} does the cold set in (the first few of a crowd).
	 */
	private static void frostbite(Cast cast, LivingEntity t, double power, double duration, boolean sets) {
		ServerLevel level = cast.level;
		DamageSource cold = frost(cast);
		FusedFrostVfx.frostbite(level, t, cast.caster.getEyePosition());
		Effects.hurt(cast, t, cold, 3 * power);
		slow(cast, t, 0, 30);
		chill(t, 30);
		if (!sets) {
			return;
		}
		int beats = FusedFrostRules.frostbiteBeats(duration);
		String key = key("frostbite", t, cast);
		Object token = claim(key);
		for (int i = 1; i <= beats; i++) {
			int beat = i;
			Scheduler.later(beat * 20, Effects.carryContext(() -> {
				if (!current(key, token)) {
					return;
				}
				if (!cast.alive() || !onHand(cast, t)) {
					release(key, token);
					return;
				}
				int stage = FusedFrostRules.frostbiteStage(beat, beats);
				Effects.lingering(() -> {
					slow(cast, t, stage, 30);
					chill(t, 40);
					FusedFrostVfx.frostbiteBeat(level, t, stage);
					Effects.hurt(cast, t, cold, power);
				});
				if (beat == beats) {
					release(key, token);
					if (t.isAlive()) {
						int hold = Effects.ticks(1, duration);
						freeze(t, hold);
						FusedFrostVfx.frostbiteFreeze(level, t, hold);
					}
				}
			}));
		}
	}

	// ------------------------------------------------------------------ Absolute Zero (frost and frost)

	/**
	 * Absolute Zero: Slowness IV for 3 seconds; a target that was already slowed or frozen (Slowness
	 * of any kind, a frozen mark, frost through it, held by a freeze) freezes solid for 2 seconds (1 on
	 * players) and takes 7 damage, then can't be frozen solid again until 3 seconds after it thaws (it's only
	 * slowed). Every target is struck; only if {@code show} is it drawn in full (the first few of a crowd).
	 */
	private static void absoluteZero(Cast cast, LivingEntity t, double power, double duration, boolean show) {
		long now = cast.level.getGameTime();
		boolean cold = alreadyCold(t) && now >= ZERO_LOCKED.getOrDefault(t.getUUID(), Long.MIN_VALUE);
		t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, Effects.ticks(3, duration), 3, false, true), cast.caster);
		if (!cold) {
			if (show) {
				FusedFrostVfx.absoluteZero(cast.level, t);
			}
			return;
		}
		int hold = Effects.ticks(t instanceof Player ? 1 : 2, duration);
		if (ZERO_LOCKED.size() > 256) {
			ZERO_LOCKED.values().removeIf(until -> until <= now);
		}
		ZERO_LOCKED.put(t.getUUID(), FusedFrostRules.zeroLockedUntil(now, hold));
		freeze(t, hold);
		if (show) {
			FusedFrostVfx.absoluteZeroSolid(cast.level, t, hold);
		}
		Effects.hurt(cast, t, frost(cast), 7 * power);
	}

	/** Whether a creature is already slowed or frozen, for Absolute Zero. */
	static boolean alreadyCold(LivingEntity t) {
		return t.hasEffect(MobEffects.SLOWNESS) || Reactions.has(t, Reactions.Mark.FROZEN) || t.isFullyFrozen()
			|| t.hasAttached(WildercordAttachments.FROZEN_UNTIL);
	}

	// ------------------------------------------------------------------ Fossilize (earth and time)

	/**
	 * Fossilize: Slowness I, II, then III, a second each; then stone, held for 2 seconds (1 on
	 * players), and when the stone cracks, 6 damage. Fossilizing it again (the same caster) starts it over.
	 */
	private static void fossilize(Cast cast, LivingEntity t, double power, double duration) {
		ServerLevel level = cast.level;
		int step = Math.max(1, Effects.ticks(1, duration));
		String key = key("fossilize", t, cast);
		Object token = claim(key);
		slow(cast, t, 0, step + 5);
		FusedFrostVfx.fossilizeStage(level, t, 0, step);
		for (int i = 1; i < 3; i++) {
			int stage = i;
			Scheduler.later(stage * step, Effects.carryContext(() -> {
				if (!current(key, token)) {
					return;
				}
				if (!cast.alive() || !onHand(cast, t)) {
					release(key, token);
					return;
				}
				slow(cast, t, stage, step + 5);
				FusedFrostVfx.fossilizeStage(level, t, stage, step);
			}));
		}
		Scheduler.later(3 * step, Effects.carryContext(() -> {
			if (!current(key, token)) {
				return;
			}
			if (!cast.alive() || !onHand(cast, t)) {
				release(key, token);
				return;
			}
			int hold = Effects.ticks(t instanceof Player ? 1 : 2, duration);
			Spirits.hold(t, hold);
			still(t);
			FusedFrostVfx.fossilizeStone(level, t, hold);
			Scheduler.later(hold, Effects.carryContext(() -> {
				if (!current(key, token)) {
					return;
				}
				release(key, token);
				if (cast.alive() && onHand(cast, t)) {
					FusedFrostVfx.fossilizeCrack(level, t);
					Effects.hurt(cast, t, level.damageSources().indirectMagic(cast.caster, cast.caster), 6 * power);
				}
			}));
		}));
	}

	// ------------------------------------------------------------------ Geode (earth and arcane)

	/** Geode: Resistance II for 5 seconds, and for as long whatever strikes the ally is cut for 2 damage (see {@link FusedFrostWards}). */
	private static void geode(Cast cast, LivingEntity t, double power, double duration, int amplify) {
		int ticks = Effects.ticks(5, duration);
		t.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, ticks, Math.min(3, 1 + Math.max(0, amplify)), false, true));
		boolean fresh = FusedFrostWards.geode(cast, t, ticks, 2 * power);
		if (fresh || !cast.passive) {
			FusedFrostVfx.geode(cast.level, t);
		}
	}

	// ------------------------------------------------------------------ helpers

	private static List<LivingEntity> first(List<LivingEntity> targets) {
		return targets.size() <= MAX_TARGETS ? targets : targets.subList(0, MAX_TARGETS);
	}

	private static DamageSource frost(Cast cast) {
		return cast.level.damageSources().source(DamageTypes.FREEZE, cast.caster);
	}

	private static boolean onHand(Cast cast, LivingEntity t) {
		return t.isAlive() && !t.isRemoved() && t.level() == cast.level;
	}

	/** Freezes with the shared freeze (so it plays with Shatter and thaws after a restart), and stops a mob where it stands. */
	private static void freeze(LivingEntity t, int ticks) {
		Spirits.freeze(t, ticks);
		still(t);
	}

	/** Stops a creature where it stands (bosses are never moved). */
	private static void still(LivingEntity t) {
		if (!Spirits.isBoss(t)) {
			t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
			t.needsSync = true;
		}
	}

	private static void slow(Cast cast, LivingEntity t, int amplifier, int ticks) {
		t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, amplifier, false, true), cast.caster);
	}

	/** Frost on the skin, well short of frozen solid: it creeps back on for {@code ticks} more. */
	private static void chill(LivingEntity t, int ticks) {
		t.setTicksFrozen(Math.max(t.getTicksFrozen(), Math.min(t.getTicksRequiredToFreeze() - 1, t.getTicksFrozen() + ticks)));
	}

	/** Every enemy in a column {@code radius} round {@code centre} and {@code height} tall (by its feet). */
	private static List<LivingEntity> inside(Cast cast, Vec3 centre, double radius, double height) {
		List<LivingEntity> out = new ArrayList<>();
		for (Entity e : cast.level.getEntities((Entity) null, new AABB(centre, centre).inflate(radius + 1, 0, radius + 1).expandTowards(0, height, 0)
				.move(0, -1, 0), e -> Targets.canHarm(cast.caster, e))) {
			if (horizontal(e.position(), centre) <= radius + e.getBbWidth() / 2 && out.size() < MAX_IN_AREA) {
				out.add((LivingEntity) e);
			}
		}
		return out;
	}

	private static double horizontal(Vec3 a, Vec3 b) {
		double dx = a.x - b.x;
		double dz = a.z - b.z;
		return Math.sqrt(dx * dx + dz * dz);
	}

	/**
	 * Whether this caster already has one of {@code areas} lying within 2 blocks of {@code at}: if so it's kept
	 * going {@code ticks} longer (never past three times its length) instead of another being laid on it.
	 */
	private static boolean renewed(List<Area> areas, Cast cast, Vec3 at, int ticks) {
		long now = cast.level.getGameTime();
		// One whose loop was lost (a task that failed) is forgotten soon after its time.
		areas.removeIf(a -> now > a.cap + 100);
		UUID caster = cast.caster.getUUID();
		for (Area a : areas) {
			if (a.caster.equals(caster) && a.level == cast.level && a.centre.distanceToSqr(at) <= 4.0) {
				a.until = Math.min(a.cap, Math.max(a.until, now + ticks));
				return true;
			}
		}
		return false;
	}

	/**
	 * Runs {@code step} every {@code every} ticks (the first at once) while {@code area} lasts and the cast does,
	 * one step waiting at a time, then {@code end}, and forgets the area. What they deal is lingering damage, which a
	 * Shield can block but not parry.
	 */
	private static void lasting(Cast cast, Area area, List<Area> areas, int every, IntConsumer step, Runnable end) {
		int[] tick = {0};
		Runnable[] next = new Runnable[1];
		next[0] = Effects.carryContext(() -> {
			if (!cast.alive() || area.level.getGameTime() >= area.until) {
				areas.remove(area);
				Effects.lingering(end);
				return;
			}
			int at = tick[0];
			Effects.lingering(() -> step.accept(at));
			tick[0] += every;
			Scheduler.later(every, next[0]);
		});
		Scheduler.later(1, next[0]);
	}

	private static String key(String rune, LivingEntity t, Cast cast) {
		return rune + ":" + t.getUUID() + ":" + cast.caster.getUUID();
	}

	/** Starts (or restarts) a lingering effect; the token stays current until a newer one replaces it. */
	private static Object claim(String key) {
		Object token = new Object();
		RUNNING.put(key, token);
		return token;
	}

	private static boolean current(String key, Object token) {
		return RUNNING.get(key) == token;
	}

	private static void release(String key, Object token) {
		RUNNING.remove(key, token);
	}

	/**
	 * The effect context (Execute, Unison, the element) as it is now, while an effect is being
	 * applied: the consumer runs any task inside it later, so damage dealt from a listener (a Geode's
	 * shards, a shatter) still counts as this effect's.
	 */
	static Consumer<Runnable> context() {
		Runnable[] slot = new Runnable[1];
		Runnable wrapped = Effects.carryContext(() -> {
			if (slot[0] != null) {
				slot[0].run();
			}
		});
		return task -> {
			Runnable outer = slot[0];
			slot[0] = task;
			try {
				wrapped.run();
			} finally {
				slot[0] = outer;
			}
		};
	}
}
