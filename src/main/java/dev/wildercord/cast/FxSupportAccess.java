package dev.wildercord.cast;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** The few package-private spell helpers the support pack (dev.wildercord.cast.packs) needs, passed through unchanged. */
public final class FxSupportAccess {
	private FxSupportAccess() {}

	/** {@link Effects#ticks}: seconds of an effect at this duration (a passive's are kept short). */
	public static int ticks(double seconds, double duration) {
		return Effects.ticks(seconds, duration);
	}

	/** {@link Effects#push}: a spell's shove, which parties, anchors and knockback resistance all have their say on. */
	public static void push(LivingEntity target, Vec3 impulse) {
		Effects.push(target, impulse);
	}

	/** {@link Effects#horizontal}. */
	public static Vec3 horizontal(Vec3 v, Vec3 fallback) {
		return Effects.horizontal(v, fallback);
	}

	/** {@link DeathsDoor#saved}: {@code entity} was just saved from death, so nothing saves it again for a while. */
	public static void saved(Entity entity) {
		DeathsDoor.saved(entity, false);
	}
}
