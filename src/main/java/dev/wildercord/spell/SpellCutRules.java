package dev.wildercord.spell;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Which travelling spells a blade may sever. Helpful spells are never collateral targets of a spell cut. */
public final class SpellCutRules {
	private SpellCutRules() {}

	/** A harmful payload or harmful on-hit continuation; a purely helpful continuation remains safe. */
	public static boolean harmful(SpellPlan.Group group, SpellPlan.Link link) {
		if (group != null && group.effects.stream().anyMatch(effect -> (effect.effect.kind() == EffectKind.HARMFUL || effect.effect.kind() == EffectKind.MOVEMENT))) return true;
		Set<SpellPlan.Segment> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		return link != null && (harmful(link.next, seen, 0) || harmful(link.echoPrefix, seen, 0));
	}

	private static boolean harmful(SpellPlan.Segment segment, Set<SpellPlan.Segment> seen, int depth) {
		if (segment == null || depth >= 16 || !seen.add(segment)) return false;
		for (SpellPlan.Group group : segment.groups) {
			if (group.effects.stream().anyMatch(effect -> (effect.effect.kind() == EffectKind.HARMFUL || effect.effect.kind() == EffectKind.MOVEMENT))) return true;
		}
		return segment.link != null && (harmful(segment.link.next, seen, depth + 1) || harmful(segment.link.echoPrefix, seen, depth + 1));
	}

	/** Public cutting entry points also enforce reach, a frontal cone and actual incoming movement. */
	public static boolean approaching(double distance, double facing, double incoming) {
		return Double.isFinite(distance) && distance >= 0 && distance <= 4
			&& Double.isFinite(facing) && facing >= 0.5 && Double.isFinite(incoming) && incoming >= 0.6;
	}

	// ---- moves pack
	/**
	 * The learned Spell Cut counter: a swing while a hostile spell is in flight. Inside {@link #approaching} it severs the spell;
	 * a swing at one still out of the cut, or already past the blade, misses and owes a recovery. Nothing near costs nothing.
	 */
	public static final double COUNTER_ARM = 9;
	public static final int COUNTER_COST = 14, COUNTER_MISS_COST = 6, COUNTER_RECOVERY = 6, COUNTER_MISS_RECOVERY = 20;
	public enum Timing { NONE, MISS, CUT }
	public static Timing counter(double distance, double facing, double incoming) {
		if (approaching(distance, facing, incoming)) return Timing.CUT;
		return Double.isFinite(distance) && distance >= 0 && distance <= COUNTER_ARM && Double.isFinite(facing) && facing >= 0
			&& Double.isFinite(incoming) && incoming >= 0.3 ? Timing.MISS : Timing.NONE;
	}
	/** The best answer among every spell in reach: one cut is enough, any near miss otherwise owes the miss recovery. */
	public static Timing best(Timing a, Timing b) { return a == Timing.CUT || b == Timing.CUT ? Timing.CUT : a == Timing.MISS || b == Timing.MISS ? Timing.MISS : Timing.NONE; }
	public static int cost(Timing timing) { return timing == Timing.CUT ? COUNTER_COST : timing == Timing.MISS ? COUNTER_MISS_COST : 0; }
	public static int recovery(Timing timing) { return timing == Timing.CUT ? COUNTER_RECOVERY : timing == Timing.MISS ? COUNTER_MISS_RECOVERY : 0; }
}
