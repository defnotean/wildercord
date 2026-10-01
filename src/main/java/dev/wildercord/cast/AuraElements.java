package dev.wildercord.cast;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * What a blade's aura shares with spells (see {@code aura.AuraCombat}): its element meets a creature's affinity (weak, resists,
 * immune) and the elemental climate exactly as a spell of that element would, and sets off the element reactions waiting on
 * the creature's marks (a frozen foe shatters under an Ember blade, a windswept one catches into Wildfire, a soaked one
 * conducts a Thunder blade's charge). It goes through the same code, with the striker as the caster of a cast of its own.
 */
public final class AuraElements {
	private AuraElements() {}

	/**
	 * What {@code element} multiplies an aura blow on {@code target} by: the reactions it sets off, then the target's affinity
	 * and the climate (a reaction breaks through a resistance, as Shatter's does). 1 for no element.
	 */
	public static double bonus(LivingEntity striker, LivingEntity target, DamageSource source, String element) {
		if (element == null || element.isEmpty() || !(striker.level() instanceof net.minecraft.server.level.ServerLevel)) {
			return 1.0;
		}
		Cast cast = new Cast(striker);
		double multiplier = Reactions.hit(cast, target, element);
		multiplier *= switch (element) {
			case "fire" -> Reactions.fire(cast, target);
			case "storm" -> Reactions.storm(cast, target);
			default -> 1.0;
		};
		multiplier *= Affinities.multiplier(cast, target, source, element);
		return multiplier;
	}
}
