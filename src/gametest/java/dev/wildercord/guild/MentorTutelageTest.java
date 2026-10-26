package dev.wildercord.guild;

import dev.wildercord.content.WildercordItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.UUID;

/**
 * A mentor's tutelage in the real game: an apprentice's circles pay a mentor who is away into the ledger rather than
 * losing the crystals, a circle formed again pays nothing more, and crystals held for a player are handed over when
 * they log in (here, a save and reopen).
 */
public final class MentorTutelageTest implements FabricClientGameTest {
	private static final UUID ELDER = UUID.fromString("3c7e2a91-5d08-4b6f-a2c4-8e1f9b0d7a63");

	private static void check(boolean x, String why) {
		if (!x) throw new AssertionError(why);
	}

	private static ServerPlayer p(MinecraftServer s) {
		return s.getPlayerList().getPlayers().getFirst();
	}

	@Override
	public void runTest(ClientGameTestContext c) {
		TestWorldSave save;
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(25);
			w.getServer().runCommand("gamerule spawn_mobs false");
			w.getServer().runOnServer(s -> {
				p(s).setGameMode(GameType.SURVIVAL);
				p(s).getInventory().clearContent();
				Mentors.Ledger ledger = Mentors.ledger(s);
				ledger.remember(ELDER, "Elder");
				ledger.take(ELDER, p(s).getUUID());

				// The mentor is away: what the apprentice's circles earn is held for them.
				Mentors.formed(p(s), 1);
				check(ledger.owedTo(ELDER) == MentorRules.tutelage(1), "the first circle's crystals are held, not lost (" + ledger.owedTo(ELDER) + ")");
				Mentors.formed(p(s), 1);
				check(ledger.owedTo(ELDER) == MentorRules.tutelage(1), "the same circle formed again pays nothing more");
				Mentors.formed(p(s), 2);
				check(ledger.owedTo(ELDER) == MentorRules.tutelage(1) + MentorRules.tutelage(2), "the next circle adds to it");

				// Crystals held for the player themself, to be handed over at their next login.
				ledger.owe(p(s).getUUID(), 3);
			});
			save = w.getWorldSave();
		}

		try (var w = save.open()) {
			c.waitTicks(30);
			w.getServer().runOnServer(s -> {
				Mentors.Ledger ledger = Mentors.ledger(s);
				int held = p(s).getInventory().countItem(WildercordItems.MANA_CRYSTAL);
				check(held == 3, "the crystals held for the player are handed over at login (has " + held + ")");
				check(ledger.owedTo(p(s).getUUID()) == 0, "and they're no longer owed");
				check(ledger.owedTo(ELDER) == MentorRules.tutelage(1) + MentorRules.tutelage(2), "the away mentor's crystals are kept through a reopen");
				Mentors.formed(p(s), 2);
				check(ledger.owedTo(ELDER) == MentorRules.tutelage(1) + MentorRules.tutelage(2), "a circle paid before a reopen isn't paid again after it");
			});
		}
	}
}
