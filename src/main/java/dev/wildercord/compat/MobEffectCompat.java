package dev.wildercord.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;

/**
 * 1.21.10 has no fabric mob-effect events (they arrive with a later fabric-api): {@code MobEffectAddCompatMixin}
 * notes every effect that really lands, and listeners like the duels register here instead.
 */
public final class MobEffectCompat {
	private MobEffectCompat() {}

	private static final List<BiConsumer<MobEffectInstance, Entity>> AFTER_ADD = new ArrayList<>();

	public static void addAfterAdd(BiConsumer<MobEffectInstance, Entity> hook) {
		AFTER_ADD.add(hook);
	}

	/** Called by the mixin once the effect is on the entity. */
	public static void fireAfterAdd(MobEffectInstance effect, Entity entity) {
		for (BiConsumer<MobEffectInstance, Entity> hook : AFTER_ADD) {
			hook.accept(effect, entity);
		}
	}
}
