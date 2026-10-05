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
}
