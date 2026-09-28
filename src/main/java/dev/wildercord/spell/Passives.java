package dev.wildercord.spell;

import java.util.List;
import java.util.Set;

/**
 * Passive spells: up to two short spells that stay on all the time, paid for with mana every
 * second instead of a cost and a cooldown. Only runes that make sense sustained are allowed, so
 * a passive can be a lasting buff or a guardian aura but never a machine gun or a free death save.
 *
 * <ul>
 *   <li>Shapes: Self (the default) or Orbit.</li>
 *   <li>Effects: lasting buffs and wards; damage effects only through an Orbit. Nothing
 *       that renewing would break: no heals, absorption, death saves, dodges or summons.</li>
 *   <li>Modifiers: Amplify, Extend, Frugal, Widen, Focus, Quicken. No links.</li>
 * </ul>
 */
public final class Passives {
	private Passives() {}

	public static final int MAX = 2;
	/** Runes per passive, at most (and never more than the Cord's sockets). */
	public static final int SOCKETS = 5;
	/** Mana per second for each point of the passive's cost. */
	public static final double UPKEEP_PER_COST = 0.12;
	/** How often a Self passive is renewed, in ticks. */
	public static final int SELF_INTERVAL = 40;

	private static final Set<String> SHAPES = Set.of("self", "orbit");
	private static final Set<String> BUFFS = Set.of("feather_fall", "swift", "night_eye", "haste", "regrowth", "stoneskin", "empower",
		"fireward", "tidebreath", "leap", "infinity", "reflect", "accelerate", "overdrive", "anchor", "frostward", "cushion");
	/** Damage and control that an Orbit may carry. */
	private static final Set<String> AURA = Set.of("harm", "shock", "fire", "frost", "chill", "venom", "dismantle", "ripple", "aftershock", "push",
		"ember", "icicle", "pelt", "windcut");
	private static final Set<String> MODIFIERS = Set.of("amplify", "extend", "frugal", "widen", "focus", "quicken");

	/** Circles needed for each passive slot: the 1st and the 5th. */
	public static int slots(int circles) {
		return circles >= 5 ? 2 : circles >= 1 ? 1 : 0;
	}

	/** The circle a passive slot opens at (slot 0 = the 1st Circle, slot 1 = the 5th). */
	public static int circleFor(int slot) {
		return slot <= 0 ? 1 : 5;
	}

	/** Whether a rune may ever be part of a passive. Built-in runes only: add-ons opt in later. */
	public static boolean allowed(RuneDef rune) {
		if (!rune.id().startsWith("wildercord:")) {
			return false;
		}
		String path = rune.path();
		return switch (rune.family()) {
			case SHAPE -> SHAPES.contains(path);
			case EFFECT -> BUFFS.contains(path) || AURA.contains(path);
			case MODIFIER -> MODIFIERS.contains(path);
			case LINK, KNOT -> false;
		};
	}

	/** Damage and control effects need an Orbit to carry them. */
	public static boolean needsAura(RuneDef rune) {
		return rune.family() == RuneFamily.EFFECT && AURA.contains(rune.path());
	}

	/** Why these runes can't be a passive (in plain English), or null if they can. */
	public static String problem(List<RuneDef> runes) {
		boolean aura = false;
		int shapes = 0;
		for (RuneDef rune : runes) {
			if (!allowed(rune)) {
				return rune.name() + " can't be sustained as a passive.";
			}
			if (rune.family() == RuneFamily.SHAPE) {
				shapes++;
				aura |= !rune.is(Runes.SELF.id());
			}
		}
		if (shapes > 1) {
			return "A passive has one shape at most.";
		}
		for (RuneDef rune : runes) {
			if (needsAura(rune) && !aura) {
				return rune.name() + " needs an Orbit to carry it in a passive.";
			}
		}
		return null;
	}

	/** Mana per second to keep a passive of this cost running. */
	public static double upkeep(double cost) {
		return cost * UPKEEP_PER_COST;
	}

	/** Ticks between renewals: Self every 2 seconds, Orbit whenever its orbs run out. */
	public static int interval(SpellPlan.Segment root) {
		if (root == null || root.groups.isEmpty()) {
			return SELF_INTERVAL;
		}
		SpellPlan.Group g = root.groups.getFirst();
		if (g.shape.is(Runes.ORBIT.id())) {
			return SpellNumbers.orbitSeconds(g) * 20;
		}
		return SELF_INTERVAL;
	}
}
