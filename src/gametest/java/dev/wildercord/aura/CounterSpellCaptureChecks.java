package dev.wildercord.aura;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Runs the observer's adversarial identity, nesting, result, exception and payment controls before native scenarios. */
final class CounterSpellCaptureChecks {
	private CounterSpellCaptureChecks() {}
	static void run(ServerPlayer owner, LivingEntity target, Object action) {
		CounterSpellCapture.negativeControls(owner, target, action);
	}
}
