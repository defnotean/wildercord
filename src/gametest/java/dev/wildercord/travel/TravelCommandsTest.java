package dev.wildercord.travel;

import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * Homes, warps and {@code /back} as a survival player meets them: the warmup lands them where the home was set, the
 * cooldown refuses a second trip, moving or being hurt during the warmup cancels it, the home limit holds, and
 * {@code /back} returns to where the last teleport left from.
 */
public final class TravelCommandsTest implements FabricClientGameTest {
	private static final int A = 0, B = 40;

	private static void check(boolean x, String why) { if (!x) throw new AssertionError(why); }
	private static ServerPlayer p(MinecraftServer s) { return s.getPlayerList().getPlayers().getFirst(); }
	private static void put(MinecraftServer s, double x) {
		p(s).teleportTo(s.overworld(), x, 101, 0.5, Set.<Relative>of(), 0, 0, false);
		p(s).setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
	}
	private static boolean near(MinecraftServer s, double x) { return Math.abs(p(s).getX() - x) < 2 && Math.abs(p(s).getZ() - 0.5) < 2; }

	public void runTest(ClientGameTestContext c) {
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(25);
			w.getServer().runCommand("gamerule spawn_mobs false");
			w.getServer().runCommand("time set day");
			w.getServer().runOnServer(s -> {
				var l = s.overworld();
				for (int x = -4; x <= B + 8; x++) for (int z = -4; z <= 4; z++) {
					l.setBlock(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState(), 2);
					for (int y = 101; y <= 104; y++) l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
				}
				p(s).setGameMode(GameType.SURVIVAL);
				Teleports.resetCooldowns(p(s));
				put(s, A + 0.5);
			});
			c.waitTicks(5);

			int warmup = Travel.config().warmupSeconds() * TravelRules.TICKS_PER_SECOND;
			check(warmup > 0 && Travel.config().cooldownSeconds() > 0, "the test expects the default warmup and cooldown");

			// Set a home at A, walk to B, and go home through the warmup.
			w.getServer().runOnServer(s -> {
				check(Homes.set(p(s), "base") == 1, "/sethome base works");
				check(Travel.data(p(s)).homes().containsKey("base"), "the home is saved on the player");
				put(s, B + 0.5);
			});
			c.waitTicks(5);
			w.getServer().runOnServer(s -> {
				check(Homes.go(p(s), "base", false) == 1, "/home base starts");
				check(Teleports.pending(p(s)), "a survival player waits out the warmup");
				check(near(s, B + 0.5), "nothing moves before the warmup ends");
			});
			c.waitTicks(warmup + 5);
			w.getServer().runOnServer(s -> {
				check(!Teleports.pending(p(s)), "the warmup is over");
				check(near(s, A + 0.5), "the player landed at home (x=" + p(s).getX() + ")");
				check(Travel.data(p(s)).back().map(spot -> Math.abs(spot.x() - (B + 0.5)) < 1).orElse(false), "/back remembers B");
				check(Teleports.cooldown(p(s), Teleports.Kind.HOME) > 0, "/home is cooling down");
				check(Homes.go(p(s), "base", false) == 0 && !Teleports.pending(p(s)), "the cooldown refuses a second trip");
			});
			c.takeScreenshot(TestScreenshotOptions.of("travel_home_landed").withSize(854, 480));

			// A warp: moving during the warmup cancels it, so does being hurt.
			w.getServer().runOnServer(s -> {
				check(Warps.set(p(s), "plaza") == 1, "/setwarp plaza works");
				check(Warps.of(s).names().contains("plaza"), "the warp is saved with the world");
				put(s, B + 0.5);
			});
			c.waitTicks(5);
			w.getServer().runOnServer(s -> check(Warps.go(p(s), "plaza", false) == 1 && Teleports.pending(p(s)), "/warp plaza starts"));
			c.waitTicks(10);
			w.getServer().runOnServer(s -> put(s, B + 3.5));
			c.waitTicks(3);
			w.getServer().runOnServer(s -> {
				check(!Teleports.pending(p(s)), "moving cancelled the warmup");
				check(near(s, B + 3.5), "a cancelled warp goes nowhere");
				check(Teleports.cooldown(p(s), Teleports.Kind.WARP) == 0, "a cancelled warp costs no cooldown");
				check(Warps.go(p(s), "plaza", false) == 1 && Teleports.pending(p(s)), "/warp plaza starts again");
			});
			c.waitTicks(10);
			w.getServer().runOnServer(s -> {
				dev.wildercord.cast.Effects.readyToHurt(p(s));
				check(p(s).hurtServer(s.overworld(), p(s).damageSources().generic(), 1F), "the hit landed");
			});
			c.waitTicks(3);
			w.getServer().runOnServer(s -> {
				check(!Teleports.pending(p(s)), "being hurt cancelled the warmup");
				check(near(s, B + 3.5), "a hurt traveller stays put");
			});

			// Operators skip the warmup; /back returns to where that teleport left from.
			w.getServer().runOnServer(s -> {
				check(Warps.go(p(s), "plaza", true) == 1 && !Teleports.pending(p(s)), "an operator's warp is instant");
				check(near(s, A + 0.5), "the operator landed at the plaza");
				check(Teleports.back(p(s), true) == 1, "/back works");
				check(near(s, B + 3.5), "/back returned to where the warp left from (x=" + p(s).getX() + ")");
			});

			// The home limit, deleting, and an unknown name.
			w.getServer().runOnServer(s -> {
				int max = Travel.config().maxHomes();
				for (int i = 1; i < max; i++) check(Homes.set(p(s), "spare" + i) == 1, "home " + (i + 1) + " of " + max + " fits");
				check(Homes.set(p(s), "onetoomany") == 0, "a home over the limit is refused");
				check(Homes.set(p(s), "base") == 1, "moving a home that exists still works at the limit");
				check(Homes.list(p(s)) == max, "/homes lists every home");
				check(Homes.delete(p(s), "base") == 1 && !Travel.data(p(s)).homes().containsKey("base"), "/delhome forgets it");
				check(Homes.go(p(s), "base", true) == 0, "/home to a deleted home fails");
				check(Warps.delete(p(s), "plaza") == 1 && Warps.of(s).get("plaza") == null, "/delwarp removes it");
				check(Warps.go(p(s), "plaza", true) == 0, "/warp to a removed warp fails");
				Teleports.resetCooldowns(p(s));
			});
		}
	}
}
