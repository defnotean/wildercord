package dev.wildercord.cast;

import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sneak and stand still while wearing a Cord to meditate: after a second your mana
 * regenerates twice as fast, until you move or stand up. Checked every 5 ticks by the server.
 */
public final class Meditation {
	private Meditation() {}

	/** Checks (of 5 ticks each) the player must hold still before meditation starts. */
	private static final int SETTLE_CHECKS = 4;

	private record State(Vec3 lastPos, int stillChecks) {}

	private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

	public static void tick(ServerPlayer player) {
		boolean wearing = Spellbooks.tier(player) != null;
		State previous = STATES.get(player.getUUID());
		Vec3 pos = player.position();
		boolean still = previous != null && previous.lastPos().distanceToSqr(pos) < 0.0025;
		boolean posed = wearing && player.isShiftKeyDown() && player.onGround() && !player.isUsingItem() && !player.isSpectator();
		int checks = posed && still ? (previous == null ? 0 : previous.stillChecks()) + 1 : 0;
		STATES.put(player.getUUID(), new State(pos, checks));

		boolean meditating = checks >= SETTLE_CHECKS;
		boolean was = player.getAttachedOrElse(WildercordAttachments.MEDITATING, false);
		if (meditating != was) {
			player.setAttached(WildercordAttachments.MEDITATING, meditating);
			if (meditating) {
				Fx.sound(player.level(), pos, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5F, 0.8F);
			}
		}
		if (meditating && checks % 2 == 0) {
			show(player);
		}
	}

	/** A slow ring of soft lights turning round the player's feet, and glyphs drifting in toward them. */
	private static void show(ServerPlayer player) {
		ServerLevel level = player.level();
		Vec3 feet = player.position().add(0, 0.12, 0);
		double spin = level.getGameTime() * 0.05;
		for (int i = 0; i < 5; i++) {
			double a = spin + Math.PI * 2 * i / 5;
			Motes.glow(level, feet.add(Math.cos(a) * 1.1, 0, Math.sin(a) * 1.1), i % 2 == 0 ? 0xB8A8FF : 0xE4DCFF, 0.13, 14, new Vec3(0, 0.012, 0), 0.0);
		}
		for (int i = 0; i < 2; i++) {
			double a = level.getRandom().nextDouble() * Math.PI * 2;
			Vec3 from = feet.add(Math.cos(a) * 1.4, 0.3 + level.getRandom().nextDouble() * 0.6, Math.sin(a) * 1.4);
			Vec3 dir = feet.add(0, 0.8, 0).subtract(from);
			Fx.send(level, ParticleTypes.ENCHANT, from.x, from.y, from.z, 0, dir.x, dir.y, dir.z, 0.6);
		}
	}

	public static void forget(UUID player) {
		STATES.remove(player);
	}

	static void clear() {
		STATES.clear();
	}
}
