package dev.wildercord.town;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
/** A raid on an inn belongs to its own dimension: the same spot in the Nether is not under raid, and ending raids clears it. */
public final class InnRaidDimensionTest implements FabricClientGameTest {
	private static void check(boolean x, String why) { if (!x) throw new AssertionError(why); }
	public void runTest(ClientGameTestContext c) {
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(25);
			w.getServer().runCommand("gamerule spawn_mobs false");
			BlockPos inn = new BlockPos(0, 100, 0);
			w.getServer().runOnServer(s -> {
				var p = s.getPlayerList().getPlayers().getFirst();
				p.setGameMode(GameType.SURVIVAL);
				InnRaid.begin(p, inn, 0);
				check(InnRaid.active(s.overworld(), inn), "The raid starts on the Overworld inn");
				check(!InnRaid.active(s.getLevel(Level.NETHER), inn), "The same spot in the Nether is not under raid");
				InnRaid.begin(p, inn, 0);
			});
			c.waitTicks(10);
			c.takeScreenshot(TestScreenshotOptions.of("inn_raid_bar").disableCounterPrefix());
			w.getServer().runOnServer(s -> {
				InnRaid.endAll(s);
				check(!InnRaid.active(s.overworld(), inn), "Ending every raid clears the inn");
				check(InnRaid.saved(s) == 0, "Nothing is left to resume");
			});
		}
	}
}
