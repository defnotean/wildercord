package dev.wildercord.gametest;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.HeartCircles;
import dev.wildercord.cast.Innates;
import dev.wildercord.client.CordScreen;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Circles;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import java.util.List;

/** The real server earns circles 9–20, refuses missing milestones and preserves old and new saves. */
public final class WildercordHeartCirclesTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		dev.wildercord.cast.ManaSkinChecks.run(context);
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule fall_damage false");
			world.getServer().runCommand("time set 6000");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setGameMode(GameType.SURVIVAL);
				Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
				player.setAttached(WildercordAttachments.CIRCLES, 8);
				player.setAttached(WildercordAttachments.CONDENSED, 80000);
				player.setAttached(WildercordAttachments.CRACKS, WildercordAttachments.Cracks.NONE);
				roundTrip(player, 8, 80000);
				check(!Heart.ready(player), "The original Archmage save does not skip the ninth-circle requirements");
				fullyExperienced(player);
				for (int n = 9; n <= Circles.MAX; n++) {
					player.setAttached(WildercordAttachments.CONDENSED, Circles.condenseNeeded(n) - 1);
					HeartCircles.form(player);
					check(Heart.circles(player) == n - 1, "Circle " + n + " refuses insufficient mana");
					player.setAttached(WildercordAttachments.CONDENSED, Circles.condenseNeeded(n));
					check(Heart.ready(player), "Circle " + n + " is earnable from existing content");
					for (var need : Circles.requirements(n)) {
						removeRequirement(player, need);
						check(!Heart.ready(player), "Circle " + n + " checks " + need);
						HeartCircles.form(player);
						check(Heart.circles(player) == n - 1, "Direct formation cannot bypass " + need);
						fullyExperienced(player);
					}
					if (n == 20) {
						player.setAttached(WildercordAttachments.ON_LEY, false);
						for (int i = 0; i < 39; i++) HeartCircles.tick(player, true);
						check(Heart.circles(player) == 19, "The twentieth circle needs the full meditation");
						HeartCircles.tick(player, false);
						HeartCircles.tick(player, true);
						check(Heart.circles(player) == 19, "Interrupted meditation starts over");
						for (int i = 1; i < 40; i++) HeartCircles.tick(player, true);
					} else {
						HeartCircles.form(player);
					}
					check(Heart.circles(player) == n, "Earned circle " + n + " forms");
					var advancement = server.getAdvancements().get(Wildercord.id("heart/circle_" + n));
					check(advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone(),
						"Circle " + n + " grants its generated advancement");
				}
				check(!Heart.ready(player), "There is no twenty-first circle");
				Spellbooks.setMana(player, 7);
				HeartCircles.form(player);
				check(Heart.circles(player) == 20 && Spellbooks.mana(player) == 7, "Repeated formation at the cap gives no free refill");
				roundTrip(player, 20, Circles.condenseNeeded(20));
				check(Mana.of(player).circles() == 20, "Mana includes all twenty circles");
				check(Math.abs(Heart.bonuses(player).power() - 1.6) < 1e-6, "Twenty circles add sixty percent spell power");
				check(Math.abs(Innates.scale(player) - 2.2) < 1e-6, "Innate scaling continues through twenty");
				int full = Mana.max(player);
				player.setAttached(WildercordAttachments.CRACKS, new WildercordAttachments.Cracks(1, player.level().getGameTime() + 3600));
				check(Heart.active(player) == 19 && Mana.max(player) == full - 15, "Cracked outer circle loses its mana bonus");
				check(Math.abs(Heart.bonuses(player).power() - 1.57) < 1e-6, "Cracked outer circle loses its spell power");
				player.setAttached(WildercordAttachments.CRACKS, new WildercordAttachments.Cracks(13, player.level().getGameTime() + 3600));
				check(Heart.active(player) == 7 && Heart.bonuses(player).cost() == 1.0, "Archmage still needs eight working circles");
				player.setAttached(WildercordAttachments.CRACKS, WildercordAttachments.Cracks.NONE);
				player.setAttached(WildercordAttachments.CIRCLES, Integer.MAX_VALUE);
				check(Heart.circles(player) == 20 && Mana.max(player) == full, "Malformed save counts cannot grant extra mana");
				player.setAttached(WildercordAttachments.CIRCLES, 19);
				Spellbooks.setCord(player, ItemStack.EMPTY);
				HeartCircles.form(player);
				check(Heart.circles(player) == 19, "Formation requires a worn Cord");
				Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
				player.setGameMode(GameType.SPECTATOR);
				HeartCircles.form(player);
				check(Heart.circles(player) == 19, "Spectators cannot form circles");
				player.setGameMode(GameType.SURVIVAL);
				HeartCircles.form(player);
				int condensed = Heart.condensed(player);
				HeartCircles.condense(player, Float.NaN);
				HeartCircles.condense(player, Float.POSITIVE_INFINITY);
				check(Heart.condensed(player) == condensed, "Nonfinite payments do not corrupt progression");
				HeartCircles.condense(player, Float.MAX_VALUE);
				check(Heart.condensed(player) == Integer.MAX_VALUE, "Lifetime mana saturates without overflow");
				player.setAttached(WildercordAttachments.CONDENSED, Circles.condenseNeeded(20));
			});
			context.waitTicks(15);
			context.runOnClient(mc -> {
				check(Heart.circles(mc.player) == 20, "The owner receives the twentieth circle");
				check(Heart.condensed(mc.player) == Circles.condenseNeeded(20), "The owner receives lifetime mana");
				check(Math.abs(Heart.bonuses(mc.player).power() - 1.6) < 1e-6, "Client and server bonuses agree");
				mc.getWindow().setWindowed(1920, 1080);
				mc.options.guiScale().set(2);
				mc.resizeGui();
				mc.gui.hud.clearTitles();
				mc.gui.toastManager().clear();
			});
			tooltip(context, "heart_twenty_complete");
			world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().setAttached(WildercordAttachments.CIRCLES, 19));
			context.waitTicks(5);
			tooltip(context, "heart_twenty_requirements");
			context.setScreen(() -> null);
		}
	}

	private static void fullyExperienced(ServerPlayer player) {
		Spellbooks.set(player, new Spellbook(Runes.all().stream().map(r -> r.id()).toList(), List.of(), 0, true));
		player.setAttached(WildercordAttachments.GRIMOIRE, Feats.everyEntry());
		player.setAttached(WildercordAttachments.SPELL_KILLS, 1000);
		player.setAttached(WildercordAttachments.RUNEBOUND_SLAIN, 100);
	}

	private static void removeRequirement(ServerPlayer player, Circles.Requirement need) {
		switch (need.need()) {
			case RUNES -> Spellbooks.set(player, Spellbook.EMPTY);
			case KILLS -> player.setAttached(WildercordAttachments.SPELL_KILLS, need.amount() - 1);
			case RUNEBOUND -> player.setAttached(WildercordAttachments.RUNEBOUND_SLAIN, need.amount() - 1);
			case REACTIONS -> player.setAttached(WildercordAttachments.GRIMOIRE, Heart.grimoire(player).stream().filter(k -> !k.startsWith("reaction:")).toList());
			case SECRETS -> player.setAttached(WildercordAttachments.GRIMOIRE, Heart.grimoire(player).stream().filter(k -> !k.startsWith("secret:")).toList());
			case FEAT -> player.setAttached(WildercordAttachments.GRIMOIRE, Heart.grimoire(player).stream().filter(k -> !k.equals("feat:" + need.feat())).toList());
			default -> throw new AssertionError("Test needs a fixture for " + need);
		}
	}

	private static void roundTrip(ServerPlayer player, int circles, int condensed) {
		var saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.level().registryAccess());
		player.saveWithoutId(saved);
		player.setAttached(WildercordAttachments.CIRCLES, 0);
		player.setAttached(WildercordAttachments.CONDENSED, 0);
		player.load(TagValueInput.create(ProblemReporter.DISCARDING, player.level().registryAccess(), saved.buildResult()));
		check(Heart.circles(player) == circles && Heart.condensed(player) == condensed,
			"Player attachments preserve " + circles + " circles and " + condensed + " condensed mana on save/load");
	}

	private static void tooltip(ClientGameTestContext context, String name) {
		context.setScreen(CordScreen::new);
		context.waitTicks(3);
		int[] pos = new int[2];
		context.runOnClient(mc -> {
			int scale = (int) mc.getWindow().getGuiScale();
			int left = (mc.getWindow().getGuiScaledWidth() - 372) / 2;
			int top = (mc.getWindow().getGuiScaledHeight() - 292) / 2;
			pos[0] = (left + 372 - 27 - 36 + 7) * scale;
			pos[1] = (top + 14) * scale;
		});
		context.getInput().setCursorPos(pos[0], pos[1]);
		context.waitTicks(3);
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	private static void check(boolean value, String message) {
		if (!value) throw new AssertionError(message);
	}
}
