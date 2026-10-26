package dev.wildercord.cast;

import dev.wildercord.content.RuneSealBlock;
import dev.wildercord.content.WildercordBlocks;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.util.Set;

/**
 * A rune-seal door whose elements weren't all lit in time goes dark again, and one that was opening when the
 * server stopped (its staggered dissolve lost with the scheduler) still dissolves when its seals' saved ticks
 * come due, instead of standing half-open for good.
 */
public final class RuneSealDoorTest implements FabricClientGameTest {
	private static final int Z = 6;

	private static void check(boolean x, String why) { if (!x) throw new AssertionError(why); }

	private static RuneSealBlock.Element element(int x, int y) {
		return (x + y) % 2 == 0 ? RuneSealBlock.Element.FIRE : RuneSealBlock.Element.FROST;
	}

	/** The door: three wide, three high, fire and frost in a checker. */
	private static void build(ServerLevel l) {
		for (int x = -1; x <= 1; x++) for (int y = 101; y <= 103; y++) {
			l.setBlock(new BlockPos(x, y, Z), WildercordBlocks.RUNE_SEAL.defaultBlockState().setValue(RuneSealBlock.ELEMENT, element(x, y)), 2);
		}
	}

	/** Lights the fire seals or all of them, each with a short lit timer, as a door does between casts or after a restart. */
	private static void light(ServerLevel l, boolean fireOnly) {
		for (int x = -1; x <= 1; x++) for (int y = 101; y <= 103; y++) {
			BlockPos pos = new BlockPos(x, y, Z);
			if (!fireOnly || element(x, y) == RuneSealBlock.Element.FIRE) {
				l.setBlock(pos, l.getBlockState(pos).setValue(RuneSealBlock.LIT, true), 2);
				l.scheduleTick(pos, WildercordBlocks.RUNE_SEAL, 5 + x + 1);
			}
		}
	}

	private static int count(MinecraftServer s, boolean litOnly) {
		int n = 0;
		for (int x = -1; x <= 1; x++) for (int y = 101; y <= 103; y++) {
			var state = s.overworld().getBlockState(new BlockPos(x, y, Z));
			if (state.is(WildercordBlocks.RUNE_SEAL) && (!litOnly || state.getValue(RuneSealBlock.LIT))) n++;
		}
		return n;
	}

	public void runTest(ClientGameTestContext c) {
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(25);
			w.getServer().runCommand("gamerule spawn_mobs false");
			w.getServer().runCommand("time set day");
			w.getServer().runOnServer(s -> {
				var l = s.overworld();
				for (int x = -4; x <= 4; x++) for (int z = -2; z <= Z + 2; z++) {
					l.setBlock(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState(), 2);
					for (int y = 101; y <= 105; y++) l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
				}
				build(l);
				var p = s.getPlayerList().getPlayers().getFirst();
				p.setGameMode(GameType.SURVIVAL);
				p.teleportTo(l, 0.5, 101, 1.5, Set.<Relative>of(), 0, 0, false);
			});
			c.waitTicks(5);

			// Only the fire seals lit: the frost ones were never found, so the fire ones go dark and the door stands.
			w.getServer().runOnServer(s -> light(s.overworld(), true));
			c.waitTicks(2);
			check(w.getServer().computeOnServer(s -> count(s, true)) == 5, "the five fire seals are lit");
			c.takeScreenshot(TestScreenshotOptions.of("rune_seal_half_lit").withSize(854, 480));
			c.waitTicks(10);
			w.getServer().runOnServer(s -> {
				check(count(s, false) == 9, "a door not wholly lit stays shut (" + count(s, false) + " seals)");
				check(count(s, true) == 0, "its lit seals went dark again");
			});

			// Every seal lit, and no dissolve waiting (as after a server stop mid-opening): the saved ticks open it.
			w.getServer().runOnServer(s -> light(s.overworld(), false));
			c.waitTicks(15);
			check(w.getServer().computeOnServer(s -> count(s, false)) == 0, "a wholly lit door whose opening was cut short still dissolves");
			c.takeScreenshot(TestScreenshotOptions.of("rune_seal_opened").withSize(854, 480));
		}
	}
}
