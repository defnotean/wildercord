package dev.wildercord.client.fx;

import dev.wildercord.player.Spellbooks;
import dev.wildercord.world.LeyLines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Ley lines, as a Cord-wearer sees them: pale violet motes rising slowly from the ground along
 * each vein. Worked out on the client from the ley seed the server sends at login, so nothing is
 * streamed and only players with a Cord see them.
 */
public final class LeyMotes {
	private LeyMotes() {}

	private static long seed;
	private static boolean known;

	public static void setSeed(long leySeed) {
		seed = leySeed;
		known = true;
	}

	public static boolean known() {
		return known;
	}

	public static long seed() {
		return seed;
	}

	/** Ley strength where the player stands: 0 to 1 (0 until the seed arrives). */
	public static double strengthAt(LocalPlayer player) {
		return known && player.level().dimension() == Level.OVERWORLD ? LeyLines.strength(seed, player.getX(), player.getZ()) : 0;
	}

	public static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (!known || player == null || mc.level == null || player.level().dimension() != Level.OVERWORLD || Spellbooks.tier(player) == null) {
			return;
		}
		RandomSource random = player.getRandom();
		for (int i = 0; i < 140; i++) {
			double x = player.getX() + (random.nextDouble() - 0.5) * 56;
			double z = player.getZ() + (random.nextDouble() - 0.5) * 56;
			double strength = LeyLines.strength(seed, x, z);
			if (strength < 0.3 || random.nextDouble() > strength) {
				continue;
			}
			int y = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
			if (Math.abs(y - player.getY()) > 24) {
				continue;
			}
			if (random.nextInt(3) == 0) {
				mc.level.addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xC0B8A0FF), x, y + 0.1, z, 0, 0.02, 0);
			} else {
				mc.level.addParticle(new DustParticleOptions(strength > 0.7 ? 0xE0D4FF : 0xA890F0, 0.6F + (float) strength * 0.5F), x, y + 0.15 + random.nextDouble() * 0.6, z, 0, 0.01, 0);
			}
		}
	}
}
