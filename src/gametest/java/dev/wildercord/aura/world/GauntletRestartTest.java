package dev.wildercord.aura.world;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;

/** A Master gauntlet left running when the world closes doesn't follow its player into the next session. */
public final class GauntletRestartTest implements FabricClientGameTest {
	private static void check(boolean x, String why) { if (!x) throw new AssertionError(why); }
	private static ServerPlayer p(MinecraftServer s) { return s.getPlayerList().getPlayers().getFirst(); }

	public void runTest(ClientGameTestContext c) {
		TestWorldSave save;
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(30);
			w.getServer().runCommand("gamerule spawn_mobs false");
			w.getServer().runOnServer(s -> {
				s.setDifficulty(Difficulty.NORMAL, true);
				var p = p(s);
				p.setGameMode(GameType.SURVIVAL);
				p.setAttached(MasterVictories.RECORD, new MasterVictoryRules.Progress(MasterVictoryRules.ALL));
				check(MasterGauntlet.start(p) == 1, "A player who has beaten every Master opens a gauntlet");
				check(MasterGauntlet.running(p), "The gauntlet is running");
			});
			c.waitTicks(5);
			save = w.getWorldSave();
		}
		try (var w = save.open()) {
			c.waitTicks(30);
			w.getServer().runOnServer(s -> check(!MasterGauntlet.running(p(s)), "The gauntlet from the last session is gone"));
		}
	}
}
