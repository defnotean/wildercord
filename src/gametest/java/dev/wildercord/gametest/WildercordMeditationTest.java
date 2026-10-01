package dev.wildercord.gametest;

import dev.wildercord.cast.Fx;
import dev.wildercord.content.RitualOption;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.Set;

/** First-person meditation at eight circles, formation, and late-stage rune attunement. */
public final class WildercordMeditationTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule fall_damage false");
			world.getServer().runCommand("time set 6000");
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().getFirst();
				player.setGameMode(GameType.CREATIVE);
				player.teleportTo(player.level(), 0.5, 160, 0.5, Set.<Relative>of(), 0, 0, false);
			});
			context.waitTicks(30);
			world.getServer().runCommand("fill -12 159 -12 12 159 20 polished_deepslate");
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().getFirst();
				player.teleportTo(player.level(), 0.5, 160, 0.5, Set.<Relative>of(), 0, 0, false);
				player.setGameMode(GameType.SURVIVAL);
				player.fallDistance = 0;
				player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
				Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
				player.setAttached(WildercordAttachments.CIRCLES, 8);
				Spellbooks.setMana(player, 100);
			});
			context.runOnClient(mc -> {
				mc.getWindow().setWindowed(1280, 720);
				mc.options.guiScale().set(2);
				mc.resizeGui();
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				mc.options.toggleCrouch().set(false);
				mc.gui.toastManager().clear();
			});
			context.runOnClient(mc -> mc.options.keyShift.setDown(true));
			context.waitTicks(50);
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().getFirst();
				check(player.isAlive(), "The visual review player must be alive");
				check(Mana.of(player).meditating(), "Sneaking still must activate meditation: sneak=" + player.isShiftKeyDown()
					+ ", grounded=" + player.onGround() + ", position=" + player.position() + ", cord=" + Spellbooks.tier(player));
				check(Spellbooks.mana(player) > 100, "Meditation must still restore mana");
			});
			context.runOnClient(mc -> { mc.gui.toastManager().clear(); mc.gui.hud.clearTitles(); });
			shot(context, "meditation_eight_circles_first_person");
			context.runOnClient(mc -> mc.player.setXRot(55));
			context.waitTicks(5);
			shot(context, "meditation_feet");
			context.runOnClient(mc -> mc.player.setXRot(0));
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(WildercordAttachments.CIRCLES, 0);
				player.setAttached(WildercordAttachments.CONDENSED, 600);
			});
			context.waitTicks(100);
			shot(context, "meditation_forming_first_person");
			context.waitTicks(110);
			world.getServer().runOnServer(server -> check(Heart.circles(server.getPlayerList().getPlayers().getFirst()) == 1,
				"Meditation must still form the first circle"));
			context.waitTicks(90);
			world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst()
				.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(WildercordItems.BLANK_RUNE)));
			for (int i = 0; i < 12; i++) {
				world.getServer().runOnServer(server -> {
					var player = server.getPlayerList().getPlayers().getFirst();
					Fx.sendAll(player.level(), new RitualOption(player.getId(), "wildercord:fire", 0xFF8030, 0.9F), player.position(), 1, 0, 0);
				});
				context.waitTicks(5);
			}
			shot(context, "meditation_attunement_first_person");
			context.runOnClient(mc -> mc.options.keyShift.setDown(false));
			context.waitTicks(15);
			world.getServer().runOnServer(server -> check(!Mana.of(server.getPlayerList().getPlayers().getFirst()).meditating(),
				"Standing up must end meditation"));
		}
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	private static void check(boolean value, String message) {
		if (!value) throw new AssertionError(message);
	}
}
