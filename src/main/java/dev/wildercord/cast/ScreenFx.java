package dev.wildercord.cast;

import dev.wildercord.net.WildercordNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Screen effects that make big magic felt: the camera shakes when something huge lands nearby, the
 * view kicks when a charged spell leaves your hands, a heavy hit lands with a punch, and a Domain
 * tints the edges of the screen of everyone inside it. Each player can turn shaking down in their
 * own settings (the vanilla "Screen Effect Scale").
 */
public final class ScreenFx {
	private ScreenFx() {}

	/** Shakes the camera of everyone within {@code radius} of {@code at}, weaker further away. */
	public static void shake(ServerLevel level, Vec3 at, float strength, double radius) {
		for (ServerPlayer player : level.players()) {
			double d = player.position().distanceTo(at);
			if (d <= radius) {
				float s = (float) (strength * (1 - d / radius));
				if (s > 0.03F) {
					send(player, WildercordNetworking.ScreenFx.SHAKE, Math.min(1, s), 8 + Math.round(10 * s));
				}
			}
		}
	}

	/** The view kicks outward: a charged spell released ({@code strength} 0 to 1: how full the charge was). */
	public static void kick(LivingEntity caster, float strength) {
		if (caster instanceof ServerPlayer player) {
			send(player, WildercordNetworking.ScreenFx.KICK, strength, 10);
		}
	}

	/** A heavy hit lands: a short sharp punch for whoever cast it. */
	public static void punch(LivingEntity caster, float strength) {
		if (caster instanceof ServerPlayer player) {
			send(player, WildercordNetworking.ScreenFx.PUNCH, Math.min(1, strength), 5);
		}
	}

	/** Tints the edges of a player's screen in {@code color} for {@code ticks} (renewed while inside a Domain). */
	public static void tint(ServerPlayer player, int color, int ticks) {
		send(player, WildercordNetworking.ScreenFx.TINT, Float.intBitsToFloat(color & 0xFFFFFF), ticks);
	}

	private static void send(ServerPlayer player, int kind, float strength, int ticks) {
		ServerPlayNetworking.send(player, new WildercordNetworking.ScreenFx(kind, strength, ticks));
	}
}
