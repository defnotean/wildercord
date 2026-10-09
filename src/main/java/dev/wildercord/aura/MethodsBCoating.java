package dev.wildercord.aura;

import dev.wildercord.aura.arts.MethodsBKit;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.config.Config;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Echo, Dawn and Venom's passives on a coated blow (called from {@code AuraCombat}'s passives, for a method with no built-in
 * flavour): Echo's blow may ring again a moment later (from Edge leaving its foe reeling), Dawn's lights its foe up and burns the
 * undead a little more (from Flow mending a little now and then), Venom's leaves a toxin that stacks (every blow from Edge, and
 * weakens). The numbers are {@link MethodsBArtRules}'.
 */
public final class MethodsBCoating {
	private MethodsBCoating() {}

	private static final Map<UUID, Long> GLINTED = new HashMap<>();
	/** True while an echo lands, so an echo never echoes itself. */
	private static boolean echoing;

	/** Whether {@code method} is one of the pack's (its passive worked here). */
	public static boolean handles(String method) {
		return MethodsBPack.METHODS.contains(method);
	}

	public static void flavour(ServerPlayer player, String method, LivingEntity target, float taken, int stage, long now) {
		if (target == null || !target.isAlive()) return;
		ServerLevel level = player.level();
		switch (method) {
			case MethodsBArtRules.ECHO -> {
				if (echoing || level.getRandom().nextDouble() >= MethodsBArtRules.echoChance(stage)) return;
				double damage = Math.max(1.0, taken * MethodsBArtRules.ECHO_SHARE) * Config.get().aura().damageScale();
				int reel = MethodsBArtRules.echoReel(stage);
				Scheduler.later(MethodsBArtRules.ECHO_COAT_DELAY, () -> {
					if (!player.isAlive() || player.level() != level || !target.isAlive() || target.level() != level || target.distanceTo(player) > 12) return;
					echoing = true;
					try {
						Feels.sound(level, target.getBoundingBox().getCenter(), "aura_echo_impact", 0.5F, 1.4F);
						if (AuraCombat.projected(player, target, damage, false) > 0) {
							AuraFx.impact(player, target, AuraFxRules.Weight.LIGHT);
							MethodsBKit.reel(player, target, reel);
						}
					} finally {
						echoing = false;
					}
				});
			}
			case MethodsBArtRules.DAWN -> {
				MethodsBKit.glow(player, target, MethodsBArtRules.dawnGlow(stage));
				if (MethodsBKit.undead(target) && taken > 0) {
					AuraCombat.projected(player, target, taken * MethodsBArtRules.DAWN_UNDEAD_SHARE * Config.get().aura().damageScale(), false);
				}
				double glint = MethodsBArtRules.dawnGlint(stage);
				Long last = GLINTED.get(player.getUUID());
				if (glint > 0 && (last == null || now - last >= MethodsBArtRules.DAWN_REST)) {
					GLINTED.put(player.getUUID(), now);
					player.heal((float) glint);
				}
			}
			case MethodsBArtRules.VENOM -> {
				if (level.getRandom().nextDouble() >= MethodsBArtRules.venomChance(stage)) return;
				MethodsBKit.toxin(player, target, MethodsBArtRules.venomTicks(stage));
				if (stage >= AuraRules.EDGE) MethodsBKit.weaken(player, target, MethodsBArtRules.venomTicks(stage));
			}
			default -> {
			}
		}
	}

	public static void forget(UUID id) {
		GLINTED.remove(id);
	}
}
