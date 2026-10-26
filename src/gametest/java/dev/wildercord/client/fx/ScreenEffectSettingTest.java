package dev.wildercord.client.fx;

import dev.wildercord.player.WildercordAttachments;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;

/** Reduced flash never raises vanilla's screen-effect setting, and at 0% the strain vignette is gone. */
public final class ScreenEffectSettingTest implements FabricClientGameTest {
	public void runTest(ClientGameTestContext c) {
		boolean oldFlash = c.computeOnClient(mc -> MagicQuality.reducedFlash);
		double oldScale = c.computeOnClient(mc -> mc.options.screenEffectScale().get());
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(30);
			w.getServer().runCommand("gamerule spawn_mobs false");
			w.getServer().runCommand("time set 6000");
			c.runOnClient(mc -> {
				MagicQuality.reducedFlash = true;
				mc.options.screenEffectScale().set(1.0);
				check(ScreenEffects.scale() == 0.15F, "Reduced flash caps a full setting at 15% (" + ScreenEffects.scale() + ")");
				mc.options.screenEffectScale().set(0.0);
				check(ScreenEffects.scale() == 0, "Reduced flash leaves vanilla's 0% off (" + ScreenEffects.scale() + ")");
				MagicQuality.reducedFlash = false;
				check(ScreenEffects.scale() == 0, "0% is off without reduced flash too");
				// A charge in its second of three stages, held on this client only, so the vignette draws.
				long now = mc.level.getGameTime();
				mc.player.setAttached(WildercordAttachments.CHARGE, new WildercordAttachments.Charge(0, now, List.of(), 60, 3, 2, now, false));
				mc.options.screenEffectScale().set(1.0);
			});
			shot(c, "strain_vignette_full");
			c.runOnClient(mc -> mc.options.screenEffectScale().set(0.0));
			shot(c, "strain_vignette_off");
			c.runOnClient(mc -> mc.player.removeAttached(WildercordAttachments.CHARGE));
		} finally {
			c.runOnClient(mc -> { MagicQuality.reducedFlash = oldFlash; mc.options.screenEffectScale().set(oldScale); });
		}
	}

	private static void shot(ClientGameTestContext c, String name) {
		c.waitTicks(3);
		c.runOnClient(mc -> { mc.getWindow().setWindowed(1280, 720); mc.resizeGui(); mc.gui.toastManager().clear(); mc.gui.hud.getChat().clearMessages(false); });
		c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	private static void check(boolean yes, String why) { if (!yes) throw new AssertionError(why); }
}
