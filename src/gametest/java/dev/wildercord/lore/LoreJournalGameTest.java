package dev.wildercord.lore;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.client.lore.LoreJournalScreen;
import dev.wildercord.content.WildercordItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.List;

/**
 * One discovery quest end to end in a real world: a real Grimoire discovery gives the clue through the journal's own
 * poll, a second discovery completes it and pays once, the journal syncs to the client, and the real H key opens
 * the journal screen, shows the lead as done, switches tabs and closes it again.
 */
public final class LoreJournalGameTest implements FabricClientGameTest {
	private static final String QUEST = "different_hands";

	private static void check(boolean ok, String message) {
		if (!ok) throw new AssertionError(message);
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static void press(ClientGameTestContext c, int key) {
		try {
			c.getInput().pressKey(key);
			c.waitTicks(2);
		} finally {
			c.getInput().releaseKey(key);
		}
		c.waitTicks(3);
	}

	private static void shot(ClientGameTestContext c, String name) {
		c.runOnClient(mc -> {
			mc.gui.toastManager().clear();
			mc.gui.hud.getChat().clearMessages(false);
		});
		c.waitTicks(3);
		c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	@Override
	public void runTest(ClientGameTestContext c) {
		try (TestSingleplayerContext world = c.worldBuilder().create()) {
			c.waitTicks(40);
			int[] before = world.getServer().computeOnServer(s -> {
				ServerPlayer p = player(s);
				p.setGameMode(GameType.SURVIVAL);
				return new int[] {p.getInventory().countItem(AuraWorld.AURA_SHARD), p.getInventory().countItem(WildercordItems.BLANK_RUNE),
					p.totalExperience, LoreJournal.data(p).stage(QUEST)};
			});
			check(before[3] == LoreQuestRules.UNKNOWN, "A new character starts with an empty journal");
			check(c.computeOnClient(mc -> LoreJournal.data(mc.player).entries().isEmpty()), "The client sees the empty journal");

			// Trigger: the tournament board's real discovery. The journal's own poll writes the clue.
			world.getServer().runOnServer(s -> Grimoire.unlock(player(s), "aura:tournament"));
			world.getServer().waitFor(s -> LoreJournal.data(player(s)).stage(QUEST) == LoreQuestRules.ACTIVE, 200);
			c.waitFor(mc -> LoreJournal.data(mc.player).stage(QUEST) == LoreQuestRules.ACTIVE, 100);
			check(world.getServer().computeOnServer(s -> LoreJournal.data(player(s)).has("place:tournament")), "The place is written in the journal");
			check(world.getServer().computeOnServer(s -> player(s).getInventory().countItem(AuraWorld.AURA_SHARD)) == before[0],
				"A clue alone pays nothing");

			// Objective: a duel won, recorded as the real Grimoire entry.
			world.getServer().runOnServer(s -> Grimoire.unlock(player(s), "aura:duelist"));
			world.getServer().waitFor(s -> LoreJournal.data(player(s)).stage(QUEST) == LoreQuestRules.DONE, 200);
			int[] after = world.getServer().computeOnServer(s -> {
				ServerPlayer p = player(s);
				return new int[] {p.getInventory().countItem(AuraWorld.AURA_SHARD), p.getInventory().countItem(WildercordItems.BLANK_RUNE), p.totalExperience};
			});
			LoreQuestRules.Quest quest = LoreQuestRules.byId(QUEST);
			check(after[0] == before[0] + quest.shards(), "The Aura Shards were paid: " + after[0]);
			check(after[1] == before[1] + quest.runes(), "The Blank Runes were paid: " + after[1]);
			check(after[2] >= before[2] + quest.xp(), "The experience was paid: " + after[2]);
			// Later polls never pay twice.
			world.getServer().runOnServer(s -> LoreJournal.poll(player(s)));
			world.getServer().runOnServer(s -> LoreJournal.poll(player(s)));
			check(world.getServer().computeOnServer(s -> player(s).getInventory().countItem(AuraWorld.AURA_SHARD)) == after[0], "The reward is paid once");
			c.waitFor(mc -> LoreJournal.data(mc.player).stage(QUEST) == LoreQuestRules.DONE, 100);

			// The journal opens on its real key and shows the finished lead.
			c.runOnClient(mc -> mc.gui.setScreen(null));
			c.waitTicks(3);
			press(c, InputConstants.KEY_H);
			c.waitForScreen(LoreJournalScreen.class);
			c.runOnClient(mc -> ((LoreJournalScreen) mc.gui.screen()).showTab("quests"));
			c.waitTicks(3);
			String done = Component.translatable("screen.wildercord.lore_journal.done", Component.translatable(quest.key("title"))).getString();
			List<String> leads = c.computeOnClient(mc -> ((LoreJournalScreen) mc.gui.screen()).text("quests"));
			check(leads.contains(done), "The lead shows as done: " + leads);
			check(!done.contains("journal.wildercord"), "The lead's English is loaded: " + done);
			shot(c, "lore_journal_leads");
			press(c, InputConstants.KEY_RIGHT);
			check(c.computeOnClient(mc -> ((LoreJournalScreen) mc.gui.screen()).tab()).equals("places"), "Arrow keys switch tabs");
			List<String> places = c.computeOnClient(mc -> ((LoreJournalScreen) mc.gui.screen()).text("places"));
			check(places.contains(Component.translatable("journal.wildercord.entry.place.tournament").getString()), "The place entry is listed: " + places);
			shot(c, "lore_journal_places");
			press(c, InputConstants.KEY_H);
			check(c.computeOnClient(mc -> mc.gui.screen() == null), "The journal key closes the journal");
		} finally {
			c.getInput().releaseKey(InputConstants.KEY_H);
			c.runOnClient(mc -> mc.gui.setScreen(null));
		}
	}
}
