package dev.wildercord.wildlife;

import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * The first ride on a freshly tamed Ridgeback Stag used to crash the server: the ride's "last seen" tick started at
 * {@code Long.MIN_VALUE}, so the fresh-ride branch never ran and the bond tick measured from a null position. Tame one,
 * ride it a few blocks, get off and on again, and the bond must have grown with nothing thrown.
 */
public final class StagFirstRideTest implements FabricClientGameTest {
	private static UUID id;

	private static void check(boolean x, String why) { if (!x) throw new AssertionError(why); }
	private static ServerPlayer p(MinecraftServer s) { return s.getPlayerList().getPlayers().getFirst(); }
	private static RidgebackStag stag(MinecraftServer s) { return (RidgebackStag) s.overworld().getEntity(id); }

	public void runTest(ClientGameTestContext c) {
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(25);
			w.getServer().runCommand("gamerule spawn_mobs false");
			w.getServer().runCommand("time set day");
			w.getServer().runOnServer(s -> {
				var l = s.overworld();
				for (int x = -4; x <= 4; x++) for (int z = -4; z <= 40; z++) {
					l.setBlock(new BlockPos(x, 100, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
					for (int y = 101; y <= 105; y++) l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
				}
				p(s).setGameMode(GameType.SURVIVAL);
				p(s).teleportTo(l, 0.5, 101, 0.5, Set.<Relative>of(), 0, 10, false);
				var stag = MountContent.RIDGEBACK_STAG.create(l, EntitySpawnReason.COMMAND);
				stag.setPos(0.5, 101, 1.5);
				l.addFreshEntity(stag);
				stag.tameWithName(p(s));
				stag.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
				id = stag.getUUID();
				check(MountBonds.owns(p(s), stag), "the stag belongs to whoever tamed it");
				check(MountBonds.bond(stag, p(s)).points() == 0, "a fresh stag has no bond yet");
				check(p(s).startRiding(stag), "the owner can climb on");
			});

			// The first ride: walk the stag forward 0.4 blocks a tick for two seconds of bond ticks.
			for (int t = 0; t < 50; t++) {
				int step = t;
				w.getServer().runOnServer(s -> stag(s).setPos(0.5, 101, 1.5 + step * 0.4));
				c.waitTick();
			}
			int first = w.getServer().computeOnServer(s -> {
				check(p(s).getVehicle() == stag(s), "still riding after the first ride's bond ticks");
				return MountBonds.bond(stag(s), p(s)).points();
			});
			check(first > 0, "riding a tamed stag grows the bond (had " + first + ")");
			c.takeScreenshot(TestScreenshotOptions.of("stag_first_ride").withSize(854, 480));

			// Off and on again: the second ride starts fresh from where the stag now stands.
			w.getServer().runOnServer(s -> p(s).stopRiding());
			c.waitTicks(10);
			w.getServer().runOnServer(s -> check(p(s).startRiding(stag(s)), "the owner can climb back on"));
			for (int t = 0; t < 30; t++) {
				int step = t;
				w.getServer().runOnServer(s -> stag(s).setPos(0.5, 101, 21.5 + step * 0.4));
				c.waitTick();
			}
			w.getServer().runOnServer(s -> {
				check(p(s).getVehicle() == stag(s), "still riding after the second ride");
				check(MountBonds.bond(stag(s), p(s)).points() >= first, "the bond kept what the first ride earned");
				p(s).stopRiding();
			});
		}
	}
}
