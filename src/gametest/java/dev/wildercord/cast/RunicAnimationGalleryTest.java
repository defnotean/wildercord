package dev.wildercord.cast;

import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneFamily;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Captures the production rune animation at three moments for every castable rune. */
public class RunicAnimationGalleryTest implements FabricClientGameTest {
	private record Panel(RuneDef rune, boolean landing) {
		String label() { return rune.name() + (landing ? " · impact" : " · release"); }
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!"1".equals(System.getenv("WILDERCORD_ANIMATION_GALLERY"))) {
			dev.wildercord.Wildercord.LOGGER.info("[TEST SKIP] RunicAnimationGalleryTest: set WILDERCORD_ANIMATION_GALLERY=1 to capture the full authored gallery");
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			context.runOnClient(mc -> {
				mc.getWindow().setWindowed(1920, 1080);
				mc.options.guiScale().set(2);
				mc.resizeGui();
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
			});
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("fill -8 196 12 8 207 12 minecraft:black_concrete");
			world.getServer().runCommand("fill -8 195 -2 8 195 12 minecraft:stone");
			world.getServer().runCommand("fill -1 200 -1 1 200 1 minecraft:barrier");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setGameMode(GameType.CREATIVE);
				player.teleportTo(player.level(), 0.5, 201, 0.5, Set.<Relative>of(), 0, 0, false);
				player.setDeltaMovement(Vec3.ZERO);
			});
			context.waitTicks(20);
			List<RuneDef> castable = Runes.all().stream()
				.filter(r -> r.family() == RuneFamily.SHAPE || r.family() == RuneFamily.EFFECT)
				.sorted(Comparator.comparing((RuneDef r) -> r.family() == RuneFamily.SHAPE ? "" : r.element())
					.thenComparing(RuneDef::name)).toList();
			List<Panel> panels = new ArrayList<>();
			for (RuneDef rune : castable) {
				panels.add(new Panel(rune, false));
			}
			for (RuneDef rune : castable) {
				if (rune.family() == RuneFamily.EFFECT) {
					panels.add(new Panel(rune, true));
				}
			}
			StringBuilder gallery = new StringBuilder("""
			<!doctype html><html lang="en"><meta charset="utf-8"><title>Wildercord · Rune animation gallery</title>
			<style>body{background:#12121b;color:#f4eee2;font:16px system-ui;margin:2rem auto;max-width:1300px;padding:0 1rem}
			a{color:#93e6df}details{border:1px solid #474056;border-radius:12px;margin:1rem 0;padding:1rem;background:#20202c}
			summary{cursor:pointer;font-weight:700} .frames{display:flex;gap:.5rem;margin-top:1rem}.frames a{width:33%}.frames img{width:100%;border-radius:7px}
			ol{display:grid;grid-template-columns:repeat(3,1fr);gap:.4rem 1rem;padding-left:1.5rem}li{padding:.25rem}
			</style><h1>Wildercord rune animations</h1><p>These are screenshots from the running Minecraft client.
			Each set shows the same nine runes three, six and ten ticks after release or impact.
			The numbered key follows the three by three grid from top left to bottom right. Click a frame for full resolution.</p>
			""");
			Path output = null;
			int batchLimit = Integer.parseInt(System.getenv().getOrDefault("WILDERCORD_ANIMATION_GALLERY_BATCHES", "999"));
			for (int start = 0, batch = 0; start < panels.size() && batch < batchLimit; start += 9, batch++) {
				List<Panel> page = panels.subList(start, Math.min(start + 9, panels.size()));
				int n = batch;
				world.getServer().runOnServer(server -> {
					ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
					Cast cast = new Cast(player);
					for (int i = 0; i < page.size(); i++) {
						Panel panel = page.get(i);
						double x = (i % 3 - 1) * 2.5 + 0.5;
						double y = 201.6 + (1 - i / 3) * 2.1;
						RunicAnimations.show(player.level(), panel.rune(), new Vec3(x, y, 9),
							new Vec3(0, 0, -1), 0.76, panel.landing(), cast);
					}
				});
				context.waitTicks(3);
				String stem = "animation_" + String.format("%03d", n);
				Path first = context.takeScreenshot(TestScreenshotOptions.of(stem + "_t03").disableCounterPrefix());
				if (output == null) output = first.getParent();
				context.waitTicks(3);
				context.takeScreenshot(TestScreenshotOptions.of(stem + "_t06").disableCounterPrefix());
				context.waitTicks(4);
				context.takeScreenshot(TestScreenshotOptions.of(stem + "_t10").disableCounterPrefix());
				context.waitTicks(20);
				gallery.append("<details").append(batch == 0 ? " open" : "").append("><summary>Set ")
					.append(batch + 1).append(" · ").append(page.getFirst().landing() ? "impact" : "release")
					.append(" · ").append(escape(page.getFirst().rune().name())).append(" through ")
					.append(escape(page.getLast().rune().name())).append("</summary><div class=frames>");
				for (String time : List.of("03", "06", "10")) {
					String file = stem + "_t" + time + ".png";
					gallery.append("<a href=\"").append(file).append("\"><img loading=lazy src=\"")
						.append(file).append("\" alt=\"Frame at tick ").append(time).append("\"></a>");
				}
				gallery.append("</div><ol>");
				for (Panel panel : page) gallery.append("<li>").append(escape(panel.label())).append("</li>");
				gallery.append("</ol></details>");
			}
			gallery.append("</html>");
			if (output == null) throw new AssertionError("No animation screenshots were taken");
			try {
				Files.writeString(output.resolve("animation-gallery.html"), gallery.toString());
			} catch (IOException e) {
				throw new AssertionError("Could not write the animation gallery", e);
			}
		}
	}

	private static String escape(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
