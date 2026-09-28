package dev.wildercord.cast;

import dev.wildercord.spell.SpellPlan;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Fused effects of flame and stone, made only at the Fusion Altar (see {@code spell.Fusions}); {@link FusedEffects}
 * hands each of them here. Their look is in {@link FusedFlameVfx}. Numbers match the rune descriptions in
 * {@code Runes}.
 */
final class FusedFlame {
	private FusedFlame() {}

	/** Registers anything these effects listen for (damage, deaths, ticks); called once at startup. */
	static void init() {
	}

	/** Does {@code node}'s effect if it's one of these, and says whether it was. */
	static boolean apply(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, List<LivingEntity> helped, List<LivingEntity> harmed,
			double power, double duration, int amplify) {
		switch (node.effect.path()) {
			case "phoenix_pyre", "hellmouth", "starfire", "everburn", "bloodboil",
				"conflagration", "monolith", "magnetize", "sinkhole" -> {
				// Not written yet.
				return true;
			}
			default -> {
				return false;
			}
		}
	}
}
