package dev.wildercord.cast;

import dev.wildercord.spell.ReactionRules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Element reactions: effects leave short-lived marks on what they hit, and a later effect of
 * the right element that meets a mark sets off a bonus.
 * <ul>
 *   <li><b>Shatter</b>: fire damage on a frozen target deals +60% and bursts the ice.</li>
 *   <li><b>Conduct</b>: storm damage on a wet target (water, rain, or dripping: see {@link WorldMagic#wet}) deals +50% and arcs to two more enemies.</li>
 *   <li><b>Wildfire</b>: fire on a target just thrown by wind spreads flames to enemies around it.</li>
 *   <li><b>Implode</b>: a blast where enemies were just pulled together grows 50% wider and hits 30% harder.</li>
 *   <li><b>Collapse</b>: Repel on an enemy just pulled in deals double damage.</li>
 * </ul>
 * And the newer six, so that every element takes part (numbers in {@link ReactionRules}): <b>Overload</b>
 * (storm on a burning target), <b>Fracture</b> (earth on a frozen one), <b>Blight</b> (life on a shadowed
 * one), <b>Unweave</b> (arcane on one with two marks or more), <b>Rupture</b> (wind on a bleeding one) and
 * <b>Elapse</b> (time on one burning, poisoned or withering). Fire and storm reactions are asked for by the
 * effects that deal that damage ({@link #fire}, {@link #storm}); the rest go off for any spell damage of
 * their element, through {@link #hit} in {@code Effects.hurt}.
 */
public final class Reactions {
	private Reactions() {}

	public enum Mark {
		FROZEN(80),
		WINDSWEPT(50),
		PULLED(50),
		/** Left by a popped Bubble: counts as wet for Conduct. */
		SOAKED(100),
		/** Resonance's cursed mark. */
		RESONANT(200),
		/** Wet from Tidebreath or steam: counts as wet for Conduct, dulls fire and speeds frost (see {@link WorldMagic}). */
		WET(100),
		/** Left by Fracture: every spell hits it harder while it lasts. */
		CRACKED(ReactionRules.CRACKED_TICKS),
		/** Left by void's curses and darkness (Hex, Blind, Wither...): life damage on it sets off Blight. */
		SHADOWED(ReactionRules.SHADOWED_TICKS),
		/** Left by blood's cuts (Bleed, Rend, Cleave...): wind damage on it sets off Rupture. */
		BLEEDING(ReactionRules.BLEEDING_TICKS),
		/** Left by Launch, Levitate, Updraft and Cyclone: any spell hits it harder while it's off the ground (see {@link Statuses#airborne}). */
		AIRBORNE(40),
		/** Left by arcane's Harm and Reveal (see {@link Exposed}): counts as one mark for Unweave and Prismatic Burst. */
		EXPOSED(Exposed.HARM_TICKS),
		/** Left by Plasma: counts as wet for Conduct (and only for Conduct: fire is not dulled, Unweave does not count it). */
		IONISED(100);

		final int ticks;

		Mark(int ticks) {
			this.ticks = ticks;
		}
	}

	private static final Map<UUID, Map<Mark, Long>> MARKS = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> LAST_CALLOUT = new ConcurrentHashMap<>();
	/** When each caster last healed from Blight or Rupture, by "caster:reaction". */
	private static final Map<String, Long> HEALED = new ConcurrentHashMap<>();
	/** When a reaction last went off on each creature (the Cinder Warden only yields to reactions). */
	private static final Map<UUID, Long> REACTED = new ConcurrentHashMap<>();
	/**
	 * When an Overload last threw each creature: a crowd of burning foes blowing apart at once throws each
	 * of them once, not once for every neighbour (which added up to launching them sky-high).
	 */
	private static final Map<UUID, Long> THROWN = new ConcurrentHashMap<>();

	public static void mark(Entity target, Mark mark) {
		mark(target, mark, mark.ticks);
	}

	public static void mark(Entity target, Mark mark, int ticks) {
		boolean fresh = !has(target, mark);
		long until = target.level().getGameTime() + ticks;
		MARKS.computeIfAbsent(target.getUUID(), k -> new EnumMap<>(Mark.class)).merge(mark, until, Math::max);
		// Marks are seen: a halo in the mark's colour while it lasts (see MarkHalos).
		dev.wildercord.cast.feel.MarkHalos.marked(target, mark, fresh);
		// Whatever is pulled (Pull, Gravity Well, a vortex, a rift...) is dragged out of the sky: a soaring player loses the wind.
		if (mark == Mark.PULLED && target instanceof net.minecraft.world.entity.LivingEntity living) {
			Soar.ground(living);
		}
	}

	/** The marks {@code target} carries right now. */
	public static java.util.Set<Mark> marks(Entity target) {
		Map<Mark, Long> marks = MARKS.get(target.getUUID());
		java.util.Set<Mark> live = java.util.EnumSet.noneOf(Mark.class);
		if (marks != null) {
			long now = target.level().getGameTime();
			marks.forEach((mark, until) -> {
				if (until >= now) {
					live.add(mark);
				}
			});
		}
		return live;
	}

	public static boolean has(Entity target, Mark mark) {
		Map<Mark, Long> marks = MARKS.get(target.getUUID());
		if (marks == null) {
			return false;
		}
		Long until = marks.get(mark);
		return until != null && until >= target.level().getGameTime();
	}

	/** Whether a reaction went off on {@code target} within the last {@code ticks} ticks (0: this tick). */
	public static boolean reactedWithin(Entity target, int ticks) {
		Long at = REACTED.get(target.getUUID());
		return at != null && target.level().getGameTime() - at <= ticks;
	}

	private static void reacted(Entity target) {
		REACTED.put(target.getUUID(), target.level().getGameTime());
		SET_OFF.merge(target.getUUID(), 1, Integer::sum);
	}

	/**
	 * How many reactions have gone off on each creature lately, counting up: On Reaction compares it before
	 * and after the group it watches lands, so only what that group set off counts. Dropped with {@link #REACTED}.
	 */
	private static final Map<UUID, Integer> SET_OFF = new ConcurrentHashMap<>();

	/** How many reactions have gone off on {@code target} lately (see {@link #SET_OFF}). */
	public static int count(Entity target) {
		return SET_OFF.getOrDefault(target.getUUID(), 0);
	}

	public static void clear(Entity target, Mark mark) {
		Map<Mark, Long> marks = MARKS.get(target.getUUID());
		if (marks != null) {
			marks.remove(mark);
		}
	}

	/** Called for fire-element damage: returns the damage multiplier after Shatter / Wildfire. */
	public static double fire(Cast cast, LivingEntity target) {
		double multiplier = 1.0;
		ServerLevel level = cast.level;
		if (has(target, Mark.FROZEN)) {
			clear(target, Mark.FROZEN);
			target.setTicksFrozen(0);
			// The ice bursts: a Freeze, Glacier or Black Ice hold ends with it.
			Spirits.thawNow(target);
			multiplier *= 1.6;
			reacted(target);
			Vec3 c = target.getBoundingBox().getCenter();
			ReactionVfx.shatter(level, target);
			Fx.sound(level, c, SoundEvents.GLASS_BREAK, 1.0F, 0.7F);
			callout(cast, "shatter", 0x8CDCFF);
			Residues.reaction(cast, "frost", target);
		}
		if (has(target, Mark.WINDSWEPT)) {
			clear(target, Mark.WINDSWEPT);
			reacted(target);
			for (Entity e : level.getEntities(target, target.getBoundingBox().inflate(3.0), e -> Targets.canHarm(cast.caster, e))) {
				LivingEntity other = (LivingEntity) e;
				reacted(other);
				other.igniteForSeconds(4);
				Effects.hurt(cast, other, level.damageSources().source(DamageTypes.IN_FIRE, cast.caster), 3);
				ReactionVfx.wildfireLeap(level, target, other);
			}
			ReactionVfx.wildfire(level, target);
			callout(cast, "wildfire", 0xF06E32);
			Residues.reaction(cast, "fire", target);
		}
		return multiplier;
	}

	/** Called for storm-element damage: returns the damage multiplier after Conduct / Overload. */
	public static double storm(Cast cast, LivingEntity target) {
		return conduct(cast, target) * overload(cast, target);
	}

	/** Conduct: storm on a wet target arcs on to two more enemies. */
	private static double conduct(Cast cast, LivingEntity target) {
		if (!WorldMagic.wet(target) && !has(target, Mark.IONISED)) {
			return 1.0;
		}
		ServerLevel level = cast.level;
		reacted(target);
		int arcs = 0;
		for (Entity e : level.getEntities(target, target.getBoundingBox().inflate(5.0), e -> Targets.canHarm(cast.caster, e))) {
			if (arcs++ >= 2) {
				break;
			}
			LivingEntity other = (LivingEntity) e;
			Vfx.shockArc(level, target.getBoundingBox().getCenter(), other.getBoundingBox().getCenter());
			Effects.hurt(cast, other, level.damageSources().source(DamageTypes.LIGHTNING_BOLT, cast.caster), 4);
		}
		ReactionVfx.conduct(level, target);
		callout(cast, "conduct", 0xFFE650);
		Residues.reaction(cast, "storm", target);
		return 1.5;
	}

	/**
	 * Overload: storm on a burning target blows its flames apart. Every other enemy within 3 blocks takes
	 * 4 and is thrown back (no block is harmed), and the fire goes out.
	 */
	private static double overload(Cast cast, LivingEntity target) {
		if (!target.isOnFire()) {
			return 1.0;
		}
		ServerLevel level = cast.level;
		target.clearFire();
		reacted(target);
		Vec3 c = target.getBoundingBox().getCenter();
		List<LivingEntity> struck = new ArrayList<>();
		for (Entity e : level.getEntities(target, target.getBoundingBox().inflate(ReactionRules.OVERLOAD_RADIUS), e -> Targets.canHarm(cast.caster, e))) {
			if (e.getBoundingBox().getCenter().distanceTo(c) <= ReactionRules.OVERLOAD_RADIUS + e.getBbWidth() / 2) {
				struck.add((LivingEntity) e);
			}
		}
		ReactionVfx.overload(level, target, struck);
		long now = level.getGameTime();
		for (LivingEntity other : struck) {
			reacted(other);
			Effects.hurt(cast, other, level.damageSources().explosion(cast.caster, cast.caster), ReactionRules.OVERLOAD_DAMAGE);
			Long thrown = THROWN.put(other.getUUID(), now);
			if (thrown == null || thrown != now) {
				Vec3 away = Effects.horizontal(other.position().subtract(target.position()), cast.caster.getLookAngle());
				Effects.push(other, away.scale(1.1).add(0, 0.45, 0));
			}
		}
		callout(cast, ReactionRules.OVERLOAD, ReactionRules.color(ReactionRules.OVERLOAD));
		Residues.reaction(cast, "storm", target);
		return ReactionRules.OVERLOAD_BONUS;
	}

	/** Called for blasts: returns the radius multiplier after Implode (damage bonus is radius-based too). */
	public static double blast(Cast cast, Vec3 center, double radius) {
		boolean pulled = false;
		for (Entity e : cast.level.getEntities((Entity) null, new AABB(center, center).inflate(radius), e -> Targets.canHarm(cast.caster, e))) {
			if (has(e, Mark.PULLED)) {
				clear(e, Mark.PULLED);
				pulled = true;
			}
		}
		if (!pulled) {
			return 1.0;
		}
		for (Entity e : cast.level.getEntities((Entity) null, new AABB(center, center).inflate(radius * 1.5), e -> Targets.canHarm(cast.caster, e))) {
			reacted(e);
		}
		ReactionVfx.implode(cast.level, center, radius);
		callout(cast, "implode", 0xB45AF0);
		Residues.reaction(cast, "void", center);
		return 1.5;
	}

	/**
	 * Called for Repel: enemies just pulled in meet a force pushing out. Returns the damage
	 * multiplier after Collapse.
	 */
	public static double collapse(Cast cast, LivingEntity target) {
		if (!has(target, Mark.PULLED)) {
			return 1.0;
		}
		clear(target, Mark.PULLED);
		reacted(target);
		TechniqueVfx.collapse(cast.level, target.getBoundingBox().getCenter());
		callout(cast, "collapse", 0xB45AF0);
		Residues.reaction(cast, "void", target);
		return 2.0;
	}

	// ------------------------------------------------------------------ the reactions of any spell damage

	/** Set while one of these reactions deals its own damage, so that damage never sets off another. */
	private static boolean reacting;

	/**
	 * Called for every point of spell damage (from {@code Effects.hurt}), with the element of the effect
	 * dealing it: Cracked's extra, then whatever that element's damage sets off on the marks it meets
	 * (earth Fracture, life Blight, arcane Unweave, wind Rupture, time Elapse). Returns the damage
	 * multiplier.
	 */
	public static double hit(Cast cast, LivingEntity target, String element) {
		double multiplier = Statuses.airborneFactor(target);
		if (multiplier > 1.0) {
			StatusVfx.airborneBite(cast.level, target);
		}
		if (has(target, Mark.CRACKED)) {
			multiplier *= ReactionRules.CRACKED_BONUS;
			ReactionVfx.crackedBite(cast.level, target);
		}
		if (reacting || element.isEmpty()) {
			return multiplier;
		}
		reacting = true;
		try {
			multiplier *= switch (element) {
				case "earth" -> fracture(cast, target);
				case "life" -> blight(cast, target);
				case "arcane" -> unweave(cast, target);
				case "wind" -> rupture(cast, target);
				case "time" -> elapse(cast, target);
				default -> 1.0;
			};
		} finally {
			reacting = false;
		}
		return multiplier;
	}

	/** Fracture: earth on a frozen target cracks the ice through: +40%, it thaws, and it's left cracked. */
	private static double fracture(Cast cast, LivingEntity target) {
		if (!has(target, Mark.FROZEN)) {
			return 1.0;
		}
		clear(target, Mark.FROZEN);
		target.setTicksFrozen(0);
		// The ice cracks through: a Freeze, Glacier or Black Ice hold ends with it.
		Spirits.thawNow(target);
		mark(target, Mark.CRACKED);
		reacted(target);
		ReactionVfx.fracture(cast.level, target);
		callout(cast, ReactionRules.FRACTURE, ReactionRules.color(ReactionRules.FRACTURE));
		Residues.reaction(cast, "earth", target);
		return ReactionRules.FRACTURE_BONUS;
	}

	/**
	 * Blight: life on a shadowed target turns the darkness to rot. It and up to five other enemies within
	 * 4 blocks take 3 damage and are poisoned, and the caster heals 1 for each (once a second at most).
	 */
	private static double blight(Cast cast, LivingEntity target) {
		if (!has(target, Mark.SHADOWED)) {
			return 1.0;
		}
		ServerLevel level = cast.level;
		clear(target, Mark.SHADOWED);
		reacted(target);
		List<LivingEntity> rotting = new ArrayList<>();
		rotting.add(target);
		for (Entity e : level.getEntities(target, target.getBoundingBox().inflate(ReactionRules.BLIGHT_RADIUS), e -> Targets.canHarm(cast.caster, e))) {
			if (rotting.size() >= ReactionRules.BLIGHT_REACH) {
				break;
			}
			if (e.distanceTo(target) <= ReactionRules.BLIGHT_RADIUS) {
				rotting.add((LivingEntity) e);
			}
		}
		ReactionVfx.blight(level, target, rotting, cast.caster);
		for (LivingEntity t : rotting) {
			reacted(t);
			t.addEffect(new MobEffectInstance(MobEffects.POISON, ReactionRules.BLIGHT_POISON_TICKS, 0, false, true), cast.caster);
			Effects.hurt(cast, t, level.damageSources().indirectMagic(cast.caster, cast.caster), ReactionRules.BLIGHT_DAMAGE);
		}
		heal(cast, ReactionRules.BLIGHT, ReactionRules.BLIGHT_HEAL * rotting.size());
		callout(cast, ReactionRules.BLIGHT, ReactionRules.color(ReactionRules.BLIGHT));
		Residues.reaction(cast, "life", target);
		return 1.0;
	}

	/** Unweave: arcane on a target with two marks or more undoes them all: +30% for each (up to four). */
	private static double unweave(Cast cast, LivingEntity target) {
		if (countMarks(target) < ReactionRules.UNWEAVE_MIN_MARKS) {
			return 1.0;
		}
		List<Integer> used = useMarks(target);
		reacted(target);
		ReactionVfx.unweave(cast.level, target, used);
		callout(cast, ReactionRules.UNWEAVE, ReactionRules.color(ReactionRules.UNWEAVE));
		Residues.reaction(cast, "arcane", target);
		return ReactionRules.unweave(used.size());
	}

	/** Rupture: wind on a bleeding target tears the wound open: +50%, 4 more through armour, and the caster heals 2 (once a second at most). */
	private static double rupture(Cast cast, LivingEntity target) {
		if (!has(target, Mark.BLEEDING)) {
			return 1.0;
		}
		ServerLevel level = cast.level;
		clear(target, Mark.BLEEDING);
		reacted(target);
		ReactionVfx.rupture(level, target, cast.caster);
		Effects.hurt(cast, target, level.damageSources().indirectMagic(cast.caster, cast.caster), ReactionRules.RUPTURE_DAMAGE);
		heal(cast, ReactionRules.RUPTURE, ReactionRules.RUPTURE_HEAL);
		callout(cast, ReactionRules.RUPTURE, ReactionRules.color(ReactionRules.RUPTURE));
		Residues.reaction(cast, "wind", target);
		return ReactionRules.RUPTURE_BONUS;
	}

	/**
	 * Elapse: time on a target that's burning, poisoned or withering passes their time at once. All the
	 * damage they had left, half again, lands now (3 to 16), and they end.
	 */
	private static double elapse(Cast cast, LivingEntity target) {
		MobEffectInstance poison = target.getEffect(MobEffects.POISON);
		MobEffectInstance wither = target.getEffect(MobEffects.WITHER);
		// Fire that can't hurt it (Fire Resistance) had nothing left to deal.
		int fire = target.isOnFire() && !target.hasEffect(MobEffects.FIRE_RESISTANCE) ? target.getRemainingFireTicks() : 0;
		double damage = ReactionRules.elapse(ReactionRules.lingering(fire, ticksLeft(poison), poison == null ? 0 : poison.getAmplifier(),
			ticksLeft(wither), wither == null ? 0 : wither.getAmplifier()));
		if (damage <= 0) {
			return 1.0;
		}
		ServerLevel level = cast.level;
		if (fire > 0) {
			target.clearFire();
		}
		if (poison != null) {
			target.removeEffect(MobEffects.POISON);
		}
		if (wither != null) {
			target.removeEffect(MobEffects.WITHER);
		}
		reacted(target);
		ReactionVfx.elapse(level, target, fire > 0, poison != null, wither != null);
		Effects.hurt(cast, target, level.damageSources().indirectMagic(cast.caster, cast.caster), damage);
		callout(cast, ReactionRules.ELAPSE, ReactionRules.color(ReactionRules.ELAPSE));
		Residues.reaction(cast, "time", target);
		return 1.0;
	}

	/** Heals the caster for a reaction, unless that reaction already healed them within the last second. */
	private static void heal(Cast cast, String reaction, float amount) {
		LivingEntity caster = cast.caster;
		String key = caster.getUUID() + ":" + reaction;
		long now = cast.level.getGameTime();
		Long last = HEALED.get(key);
		if (!caster.isAlive() || last != null && last <= now && now - last < ReactionRules.HEAL_EVERY) {
			return;
		}
		HEALED.put(key, now);
		caster.heal(amount);
	}

	/** How long an effect has left to run: an endless one counts as a minute, none as nothing. */
	private static int ticksLeft(MobEffectInstance effect) {
		if (effect == null) {
			return 0;
		}
		return effect.isInfiniteDuration() ? 1200 : effect.getDuration();
	}

	// ------------------------------------------------------------------ every mark at once (Unweave)

	/** How many marks a target carries: burning, frozen, windswept, pulled, soaked, wet, cracked, shadowed, bleeding. */
	static int countMarks(LivingEntity t) {
		int n = t.isOnFire() ? 1 : 0;
		for (Mark mark : WOVEN) {
			if (has(t, mark)) {
				n++;
			}
		}
		return n;
	}

	/** The marks Unweave counts and undoes (Resonance's own mark aside), after burning. */
	private static final List<Mark> WOVEN = List.of(Mark.FROZEN, Mark.WINDSWEPT, Mark.PULLED, Mark.SOAKED, Mark.WET, Mark.CRACKED, Mark.SHADOWED,
		Mark.BLEEDING, Mark.EXPOSED);

	/** Uses up every mark on {@code t} and says which, as their element's colours in that order. */
	private static List<Integer> useMarks(LivingEntity t) {
		List<Integer> used = new ArrayList<>();
		if (t.isOnFire()) {
			t.clearFire();
			used.add(ElementFx.FIRE.primary());
		}
		for (Mark mark : WOVEN) {
			if (!has(t, mark)) {
				continue;
			}
			clear(t, mark);
			if (mark == Mark.FROZEN) {
				t.setTicksFrozen(0);
			}
			used.add(switch (mark) {
				case FROZEN -> ElementFx.FROST.primary();
				case WINDSWEPT -> ElementFx.WIND.accent();
				case PULLED -> ElementFx.VOID.primary();
				case SOAKED -> 0x2F6BFF;
				case WET -> 0x7CCBF2;
				case CRACKED -> ElementFx.EARTH.primary();
				case SHADOWED -> ElementFx.VOID.secondary();
				case EXPOSED -> ElementFx.ARCANE.primary();
				default -> ElementFx.BLOOD.primary();
			});
		}
		return used;
	}

	/** Tells the caster what they set off, at most once a second. */
	static void callout(Cast cast, String reaction, int color) {
		LivingEntity caster = cast.caster;
		Grimoire.reaction(caster, reaction);
		dev.wildercord.runesmith.Contracts.onReaction(caster, reaction);
		long now = cast.level.getGameTime();
		Long last = LAST_CALLOUT.get(caster.getUUID());
		if (last != null && now - last < 20) {
			return;
		}
		LAST_CALLOUT.put(caster.getUUID(), now);
		Casters.tell(caster, Component.translatable("reaction.wildercord." + reaction).withColor(color).withStyle(ChatFormatting.BOLD));
	}

	/** Drops marks for entities that no longer exist, so the table can't grow forever. */
	public static void sweep(long gameTime) {
		MARKS.values().removeIf(marks -> {
			marks.values().removeIf(until -> until < gameTime);
			return marks.isEmpty();
		});
		LAST_CALLOUT.values().removeIf(last -> gameTime - last > 100 || last > gameTime);
		REACTED.values().removeIf(at -> gameTime - at > 100 || at > gameTime);
		SET_OFF.keySet().retainAll(REACTED.keySet());
		HEALED.values().removeIf(at -> gameTime - at > 100 || at > gameTime);
		THROWN.values().removeIf(at -> gameTime - at > 100 || at > gameTime);
		Statuses.sweep(gameTime);
	}

	static void clear() {
		MARKS.clear();
		LAST_CALLOUT.clear();
		REACTED.clear();
		SET_OFF.clear();
		HEALED.clear();
		THROWN.clear();
		Statuses.clear();
		reacting = false;
	}
}
