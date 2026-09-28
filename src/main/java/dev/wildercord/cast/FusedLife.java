package dev.wildercord.cast;

import dev.wildercord.spell.SpellPlan;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Fused effects of life and blood, made only at the Fusion Altar (see {@code spell.Fusions}); {@link FusedEffects}
 * hands each of them here. Their look is in {@link FusedLifeVfx}. Numbers match the rune descriptions in
 * {@code Runes}.
 */
final class FusedLife {
	private FusedLife() {}

	/** Registers anything these effects listen for (damage, deaths, ticks); called once at startup. */
	static void init() {
	}

	/** Does {@code node}'s effect if it's one of these, and says whether it was. */
	static boolean apply(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, List<LivingEntity> helped, List<LivingEntity> harmed,
			double power, double duration, int amplify) {
		switch (node.effect.path()) {
			case "zephyr", "crimson_mist", "soulbond", "second_wind", "transfusion",
				"lifebloom", "bonespur", "sanguine_rite" -> {
				// Not written yet.
				return true;
			}
			default -> {
				return false;
			}
		}
	}
}
