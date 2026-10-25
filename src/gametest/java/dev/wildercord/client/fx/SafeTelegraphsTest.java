package dev.wildercord.client.fx;

import dev.wildercord.content.SpellCircleOption;
import dev.wildercord.presentation.SafeColourRules;
import dev.wildercord.spell.Runes;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;

/** Colour-blind-safe telegraphs reach spell circles (and so boss telegraphs), not only sigils. */
public final class SafeTelegraphsTest implements FabricClientGameTest {
	private static final int RED = 0xFF2020, GREEN = 0x30E040;

	public void runTest(ClientGameTestContext c) {
		boolean old = c.computeOnClient(mc -> MagicQuality.safeTelegraphs);
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(30);
			w.getServer().runCommand("gamerule spawn_mobs false");
			w.getServer().runCommand("time set 6000");
			c.runOnClient(mc -> {
				MagicQuality.safeTelegraphs = false;
				check(circle(mc, RED, -2).color == RED, "With the option off a red circle keeps its red");
				MagicQuality.safeTelegraphs = true;
				check(circle(mc, RED, -2).color == SafeColourRules.VERMILLION, "With the option on a red circle turns vermillion");
				check(circle(mc, GREEN, 2).color == SafeColourRules.BLUISH_GREEN, "With the option on a green circle turns bluish green");
				mc.player.setXRot(50);
				mc.player.setYRot(0);
			});
			c.runOnClient(mc -> {
				MagicQuality.safeTelegraphs = false;
				mc.particleEngine.add(circle(mc, RED, -2));
				mc.particleEngine.add(circle(mc, GREEN, 2));
			});
			shot(c, "safe_telegraphs_off");
			c.runOnClient(mc -> {
				MagicQuality.safeTelegraphs = true;
				mc.particleEngine.clearParticles();
				mc.particleEngine.add(circle(mc, RED, -2));
				mc.particleEngine.add(circle(mc, GREEN, 2));
			});
			shot(c, "safe_telegraphs_on");
		} finally {
			c.runOnClient(mc -> MagicQuality.safeTelegraphs = old);
		}
	}

	/** A long-lived flat circle four blocks in front of the player, {@code side} blocks to its left or right. */
	private static SpellCircleParticle circle(net.minecraft.client.Minecraft mc, int colour, double side) {
		var at = mc.player.position();
		return new SpellCircleParticle(mc.level, at.x - side, at.y + 0.05, at.z + 4,
			new SpellCircleOption(List.of(Runes.FIRE.id()), colour, 1.6F, 0, -90, 400));
	}

	private static void shot(ClientGameTestContext c, String name) {
		c.waitTicks(25);
		c.runOnClient(mc -> { mc.getWindow().setWindowed(1280, 720); mc.resizeGui(); mc.gui.toastManager().clear(); mc.gui.hud.getChat().clearMessages(false); });
		c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	private static void check(boolean yes, String why) { if (!yes) throw new AssertionError(why); }
}
