package dev.wildercord.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.fabric.impl.client.gametest.threading.ThreadingImpl;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Actual paused/unpaused save-close-reopen, including cleanup of a failing body. No synthetic saved state. */
public final class NativeSingleplayerCloseChecks {
	private static final BlockPos MARKER = new BlockPos(1, 130, 1);
	private NativeSingleplayerCloseChecks() { }

	public static void verify(ClientGameTestContext context) {
		for (boolean paused : new boolean[] {false, true}) {
			var bodyFailure = new RuntimeException("deliberate native singleplayer close body failure");
			TestWorldSave save;
			IntegratedServer original;
			Thread serverThread;
			var world = context.worldBuilder().create();
			save = world.getWorldSave();
			original = context.computeOnClient(mc -> mc.getSingleplayerServer());
			serverThread = world.getServer().computeOnServer(server -> Thread.currentThread());
			try (world) {
				world.getServer().runOnServer(server -> {
					server.overworld().setBlock(MARKER, Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
					server.getPlayerList().getPlayers().getFirst().getInventory().setItem(8, new ItemStack(Items.DIAMOND, paused ? 37 : 29));
				});
				context.setScreen(() -> paused ? new Screen(Component.literal("Native close pause control")) { } : null);
				context.waitTicks(3);
				check(context.computeOnClient(mc -> mc.isPaused()) == paused, "Client reaches the requested pause state");
				check(world.getServer().computeOnServer(server -> ((IntegratedServer) server).isPaused()) == paused,
					"Integrated server reaches the requested pause state");
				if (!paused) throw bodyFailure;
			} catch (RuntimeException failure) {
				if (paused || failure != bodyFailure) throw failure;
				check(failure.getSuppressed().length == 0, "Real close retains the exact deliberate primary failure without a cleanup failure");
			}
			closed(context, original, serverThread);
			try (var reopened = save.open()) {
				check(context.computeOnClient(mc -> mc.getSingleplayerServer()) != original, "Reopening starts a distinct integrated server");
				reopened.getServer().runOnServer(server -> {
					check(server.overworld().getBlockState(MARKER).is(Blocks.DIAMOND_BLOCK), "Native stop saved the world marker");
					var stack = server.getPlayerList().getPlayers().getFirst().getInventory().getItem(8);
					check(stack.is(Items.DIAMOND) && stack.getCount() == (paused ? 37 : 29), "Native stop saved the original player's inventory");
				});
			}
			check(!NativeSingleplayerClose.isActive(), "Reopened close disarms its scope");
			System.out.println("WILDERCORD_NATIVE_SINGLEPLAYER_CLOSE paused=" + paused + " saved=true stopped=true reopened=true primaryFailureControl=" + !paused);
		}
	}

	private static void closed(ClientGameTestContext context, IntegratedServer original, Thread serverThread) {
		check(!original.isRunning() && original.isShutdown(), "Original integrated server really halted and shut down");
		context.waitFor(mc -> !serverThread.isAlive(), 200);
		context.runOnClient(mc -> check(mc.level == null && mc.getSingleplayerServer() == null && !ThreadingImpl.isServerRunning,
			"Actual Fabric close completed client and server teardown"));
		var receipt = NativeSingleplayerClose.lastReceipt();
		check(receipt != null && receipt.submissions() == 1 && receipt.completed(), "Exactly one native shutdown task completed");
		check(!NativeSingleplayerClose.isActive(), "Successful real close disarms its scope");
	}

	private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
