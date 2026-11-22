package dev.wildercord.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;

/**
 * The owner-chain walk 1.21.5 puts on {@code OwnableEntity}. 1.21.4 only offers
 * {@code getOwnerUUID()} and the {@code getOwner()} default, so the climb lives here.
 */
public final class McOwnable {

	private McOwnable() {}

	/** The nearest living owner up the chain, or null. Mirrors 1.21.5's {@code getRootOwner()}. */
	public static LivingEntity rootOwner(OwnableEntity ownable) {
		LivingEntity owner = null;
		OwnableEntity current = ownable;
		while (true) {
			LivingEntity next = current.getOwner();
			if (next == null) {
				break;
			}
			owner = next;
			if (!(next instanceof OwnableEntity nextOwnable)) {
				break;
			}
			current = nextOwnable;
		}
		return owner;
	}
}
