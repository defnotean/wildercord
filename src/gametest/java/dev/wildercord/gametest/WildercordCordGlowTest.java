package dev.wildercord.gametest;

import dev.wildercord.client.CastingPose;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * The Cord's beads glow only for a moment after it goes on: put on in view (third person, facing the
 * camera, looking down on the wrist), they're lit a second later and dark five seconds later, at night
 * and by day, with nothing on the Cord shining at night then. A player drawn without the renderer's say
 * (a fresh render state) shows them dark too.
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY}
 * and {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordCordGlowTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		int fov = context.computeOnClient(mc -> mc.options.fov().get());
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("time set 18000");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				player.setGameMode(GameType.SURVIVAL);
				// The camera close in, so the wrist fills the picture.
				player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.CAMERA_DISTANCE).setBaseValue(2.2);
				Spellbook book = Spellbooks.get(player).withStarterGiven();
				for (RuneDef rune : Runes.all()) {
					book = book.learn(rune.id());
				}
				Spellbooks.set(player, book.withSpell(0, ids(Runes.BOLT, Runes.FIRE, Runes.FROST, Runes.SHOCK)).withSelected(0));
			});
			// Seen without a Cord first, so putting it on is seen.
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
				mc.options.fov().set(30);
				// Looking up, so the camera in front looks down past the face to the wrist.
				mc.player.setXRot(-60);
				if (!mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
			context.waitTicks(20);
			shot(context, "cord_glow_none");
			world.getServer().runOnServer(server -> Spellbooks.setCord(player(server), new ItemStack(WildercordItems.ECHO_CORD)));
			// The look is sent within half a second.
			context.waitTicks(20);
			float lit = glow(context);
			check(lit > 0.5F, "the beads should glow just after the Cord goes on (glow " + lit + ")");
			shot(context, "cord_glow_on");
			context.waitTicks(100);
			float later = glow(context);
			check(later == 0, "the beads should stop glowing a few seconds after the Cord goes on (glow " + later + ")");
			// Nothing on the Cord may shine at night once the glow is off: no outline round it (it once had a
			// white one, drawn like the Glowing effect's, all the time) and no lit beads.
			int brightest = brightest(shot(context, "cord_glow_off_night"));
			check(brightest < 200, "nothing should shine on the Cord at night once its glow is off (brightest pixel " + brightest + ")");
			world.getServer().runCommand("time set 6000");
			context.waitTicks(10);
			shot(context, "cord_glow_off_day");
			float fresh = context.computeOnClient(mc -> ((CastingPose) new AvatarRenderState()).wildercord$glow());
			check(fresh == 0, "a player drawn without the renderer's say should show its beads dark (glow " + fresh + ")");
		} finally {
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				mc.options.fov().set(fov);
				if (mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
		}
	}

	private static Path shot(ClientGameTestContext context, String name) {
		context.runOnClient(mc -> mc.gui.toastManager().clear());
		context.waitTicks(1);
		return context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	/** The brightest channel of any pixel in a screenshot. */
	private static int brightest(Path screenshot) {
		try {
			BufferedImage image = ImageIO.read(screenshot.toFile());
			int max = 0;
			for (int y = 0; y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth(); x++) {
					int rgb = image.getRGB(x, y);
					max = Math.max(max, Math.max((rgb >> 16) & 0xFF, Math.max((rgb >> 8) & 0xFF, rgb & 0xFF)));
				}
			}
			return max;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** The glow the renderer gives the local player's beads right now. */
	private static float glow(ClientGameTestContext context) {
		return context.computeOnClient(mc -> {
			var state = mc.getEntityRenderDispatcher().getRenderer(mc.player).createRenderState(mc.player, 0);
			return ((CastingPose) state).wildercord$glow();
		});
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static void check(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError(what);
		}
	}

	private static List<String> ids(RuneDef... runes) {
		return Arrays.stream(runes).map(RuneDef::id).toList();
	}
}
