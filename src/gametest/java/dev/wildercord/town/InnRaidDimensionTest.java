package dev.wildercord.town;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
/**
 * A raid on an inn belongs to its own dimension: the same spot in the Nether is not under raid, and ending raids clears
 * it. The player stands at the inn, inside the raid's radius, so the raid keeps going and its bar is on screen.
 */
public final class InnRaidDimensionTest implements FabricClientGameTest {
	private static void check(boolean x, String why) { if (!x) throw new AssertionError(why); }
	public void runTest(ClientGameTestContext c) {
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(25);
			w.getServer().runCommand("gamerule spawn_mobs false");
			w.getServer().runCommand("difficulty normal");
			BlockPos inn = new BlockPos(0, 100, 0);
			w.getServer().runOnServer(s -> {
				var p = s.getPlayerList().getPlayers().getFirst();
				for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) s.overworld().setBlock(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState(), 2);
				p.setGameMode(GameType.SURVIVAL);
				p.teleportTo(s.overworld(), 0.5, 100, 4.5, Set.<Relative>of(), 180, 0, false);
				InnRaid.begin(p, inn, 0);
				check(InnRaid.active(s.overworld(), inn), "The raid starts on the Overworld inn");
				check(!InnRaid.active(s.getLevel(Level.NETHER), inn), "The same spot in the Nether is not under raid");
				InnRaid.begin(p, inn, 0);
			});
			c.waitTicks(30);
			w.getServer().runOnServer(s -> check(InnRaid.active(s.overworld(), inn), "A defender inside the radius keeps the raid going"));
			c.takeScreenshot(TestScreenshotOptions.of("inn_raid_bar").disableCounterPrefix());
			w.getServer().runOnServer(s -> {
				InnRaid.endAll(s);
				check(!InnRaid.active(s.overworld(), inn), "Ending every raid clears the inn");
				check(InnRaid.saved(s) == 0, "Nothing is left to resume");
			});
		}
	}
}
