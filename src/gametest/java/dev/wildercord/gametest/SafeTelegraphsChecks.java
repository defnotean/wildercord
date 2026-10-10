package dev.wildercord.gametest;

import dev.wildercord.client.fx.MagicQuality;
import dev.wildercord.client.fx.SigilParticle;
import dev.wildercord.content.SigilOption;
import dev.wildercord.presentation.SafeColourRules;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * Colour-blind-safe telegraphs seen in game: a red warning circle and a green ward circle laid side by side on the ground,
 * drawn once as they are and once with the option on. Each pass is screenshotted, and each circle's drawn colour is checked:
 * its own with the option off, its Okabe-Ito swap with it on, and the two never the same colour. Run from
 * {@link WildercordWildlifeTest} after {@link WildercordTownChecks}, in a world of its own.
 */
public final class SafeTelegraphsChecks {
	private SafeTelegraphsChecks() {}

	private static final int WARNING = 0xFF3030;
	private static final int WARD = 0x40D060;

	private static void check(boolean ok, String why) {
		if (!ok) throw new AssertionError(why);
	}

	public static void run(ClientGameTestContext c) {
		boolean was = MagicQuality.safeTelegraphs;
		try (var world = c.worldBuilder().create()) {
			c.waitTicks(40);
			world.getServer().runCommand("gamerule minecraft:spawn_mobs false");
			world.getServer().runCommand("time set noon");
			world.getServer().runOnServer(server -> {
				ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
				for (int x = -10; x <= 10; x++) for (int z = -6; z <= 12; z++) {
					p.level().setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.SMOOTH_STONE.defaultBlockState());
					for (int y = 100; y <= 108; y++) p.level().setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
				}
				p.setGameMode(GameType.CREATIVE);
				p.getAbilities().flying = true;
				p.onUpdateAbilities();
				p.teleportTo(p.level(), .5, 106, -4.5, Set.of(), 0, 50, false);
				p.setDeltaMovement(Vec3.ZERO);
			});
			c.waitTicks(20);
			pass(c, false, "safe_telegraphs_off");
			pass(c, true, "safe_telegraphs_on");
		} finally {
			MagicQuality.safeTelegraphs = was;
		}
	}

	/** Lays both circles with the option set as given, checks the colours they are drawn in and screenshots them. */
	private static void pass(ClientGameTestContext c, boolean safe, String shot) {
		int[] drawn = c.computeOnClient(mc -> {
			MagicQuality.safeTelegraphs = safe;
			mc.particleEngine.clearParticles();
			int[] tints = new int[2];
			int[] colours = {WARNING, WARD};
			for (int i = 0; i < 2; i++) {
				// Flat on the ground (facing up), long-lived so both are open and steady when the shot is taken.
				var option = new SigilOption(SigilOption.CIRCLE, colours[i], 3.2F, 0, -90, 400, 0);
				var sigil = mc.particleEngine.createParticle(option, i == 0 ? -3.5 : 4.5, 100.05, 4.5, 0, 0, 0);
				check(sigil instanceof SigilParticle, "a sigil option makes a sigil");
				tints[i] = ((SigilParticle) sigil).tint();
			}
			return tints;
		});
		if (safe) {
			check(drawn[0] == SafeColourRules.safe(WARNING) && drawn[1] == SafeColourRules.safe(WARD), "with the option on, each circle is drawn in its safe colour");
		} else {
			check(drawn[0] == WARNING && drawn[1] == WARD, "with the option off, each circle keeps its own colour");
		}
		check(drawn[0] != drawn[1], "the warning and the ward are never drawn alike");
		c.waitTicks(30);
		c.takeScreenshot(TestScreenshotOptions.of(shot).disableCounterPrefix());
	}
}
