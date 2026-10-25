package dev.wildercord.backpack;

import dev.wildercord.client.WildercordKeys;
import dev.wildercord.menu.BackpackMenu;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * An open backpack in the real game, clicked through the client: a backpack in hand opens with its rows, a shift-click
 * puts a stack in and the backpack holds it at once, another backpack or a bundle is kept out, the open backpack can't
 * be picked up or swapped away by its number key, moving it some other way closes the menu with what it held, the worn
 * one opens with the backpack key, and what is in it is kept through a save and reopen.
 */
public final class BackpackMenuTest implements FabricClientGameTest {
	private static final int ROWS = BackpackTier.BACKPACK.rows;
	/** Menu index of hotbar slot {@code i}: after the backpack's rows and the three inventory rows. */
	private static int hotbar(int i) {
		return ROWS * 9 + 27 + i;
	}

	private static void check(boolean x, String why) {
		if (!x) throw new AssertionError(why);
	}

	private static ServerPlayer p(MinecraftServer s) {
		return s.getPlayerList().getPlayers().getFirst();
	}

	private static BackpackMenu menu(MinecraftServer s) {
		return p(s).containerMenu instanceof BackpackMenu m ? m : null;
	}

	private static int inside(ItemStack backpack, net.minecraft.world.item.Item item) {
		int n = 0;
		for (ItemStack stack : Backpacks.contents(backpack)) if (stack.is(item)) n += stack.getCount();
		return n;
	}

	private static void click(ClientGameTestContext c, int slot, int button, ContainerInput input) {
		c.runOnClient(mc -> mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, slot, button, input, mc.player));
		c.waitTicks(3);
	}

	@Override
	public void runTest(ClientGameTestContext c) {
		TestWorldSave save;
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(25);
			TestServerContext server = w.getServer();
			server.runCommand("gamerule spawn_mobs false");
			server.runOnServer(s -> {
				p(s).setGameMode(GameType.SURVIVAL);
				var inv = p(s).getInventory();
				inv.clearContent();
				inv.setSelectedSlot(0);
				inv.setItem(0, new ItemStack(BackpackItem.BACKPACK));
				inv.setItem(1, new ItemStack(Items.COBBLESTONE, 16));
				inv.setItem(2, new ItemStack(BackpackItem.REINFORCED_BACKPACK));
				inv.setItem(3, new ItemStack(Items.BUNDLE));
				inv.setItem(4, new ItemStack(Items.APPLE, 5));
			});
			c.waitTicks(5);

			// Used from the hand it opens with its rows, its own hotbar slot locked.
			c.runOnClient(mc -> mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND));
			c.waitTicks(5);
			server.runOnServer(s -> {
				check(menu(s) != null, "Using a backpack opens it");
				check(menu(s).rows() == ROWS && menu(s).locked() == 0, "It opens with its rows and its own slot locked, locked " + menu(s).locked());
			});
			check(c.computeOnClient(mc -> mc.player.containerMenu instanceof BackpackMenu), "The client shows the backpack");

			// A shift-click puts the stack in, and the backpack in hand holds it at once.
			click(c, hotbar(1), 0, ContainerInput.QUICK_MOVE);
			server.runOnServer(s -> {
				check(p(s).getInventory().getItem(1).isEmpty(), "The cobblestone leaves the inventory");
				check(inside(p(s).getInventory().getItem(0), Items.COBBLESTONE) == 16, "The backpack holds it at once, had "
					+ inside(p(s).getInventory().getItem(0), Items.COBBLESTONE));
			});

			// Another backpack and a bundle are kept out, by shift-click and by hand.
			click(c, hotbar(2), 0, ContainerInput.QUICK_MOVE);
			click(c, hotbar(3), 0, ContainerInput.QUICK_MOVE);
			server.runOnServer(s -> {
				check(p(s).getInventory().getItem(2).is(BackpackItem.REINFORCED_BACKPACK), "No backpack goes in a backpack");
				check(p(s).getInventory().getItem(3).is(Items.BUNDLE), "No bundle goes in a backpack");
			});
			click(c, hotbar(2), 0, ContainerInput.PICKUP);
			click(c, 5, 0, ContainerInput.PICKUP);
			click(c, hotbar(2), 0, ContainerInput.PICKUP);
			server.runOnServer(s -> {
				check(p(s).getInventory().getItem(2).is(BackpackItem.REINFORCED_BACKPACK) && menu(s).getCarried().isEmpty(),
					"A backpack put down on its slots goes back where it was");
				check(Backpacks.used(p(s).getInventory().getItem(0)) == 1, "Only the cobblestone is inside");
			});

			// The open backpack stays put: it can't be picked up, or swapped away with its number key.
			click(c, hotbar(0), 0, ContainerInput.PICKUP);
			click(c, 3, 0, ContainerInput.SWAP);
			server.runOnServer(s -> {
				check(menu(s).getCarried().isEmpty(), "The open backpack can't be picked up");
				check(p(s).getInventory().getItem(0).is(BackpackItem.BACKPACK), "Nor swapped out by its number key");
			});
			c.takeScreenshot(TestScreenshotOptions.of("backpack_open").disableCounterPrefix());

			// Moved some other way, it takes no more clicks and the menu closes with what it held.
			ItemStack[] moved = new ItemStack[1];
			server.runOnServer(s -> {
				var inv = p(s).getInventory();
				moved[0] = inv.getItem(0);
				inv.setItem(0, ItemStack.EMPTY);
				inv.setItem(8, moved[0]);
			});
			c.waitTicks(3);
			server.runOnServer(s -> {
				check(menu(s) == null, "A backpack moved while open closes its menu");
				check(inside(p(s).getInventory().getItem(8), Items.COBBLESTONE) == 16, "It keeps what it held");
			});
			check(c.computeOnClient(mc -> !(mc.player.containerMenu instanceof BackpackMenu)), "The client's screen closes too");

			// Worn, it opens with the backpack key and takes the apples.
			server.runOnServer(s -> {
				var inv = p(s).getInventory();
				check(Backpacks.wear(p(s), inv.getItem(8)), "A backpack can be worn");
				inv.setItem(8, ItemStack.EMPTY);
			});
			c.waitTicks(3);
			c.runOnClient(mc -> WildercordKeys.openBackpack());
			c.waitTicks(5);
			server.runOnServer(s -> check(menu(s) != null && menu(s).locked() == -1, "The backpack key opens the worn backpack"));
			click(c, hotbar(4), 0, ContainerInput.QUICK_MOVE);
			c.runOnClient(mc -> mc.player.closeContainer());
			c.waitTicks(5);
			server.runOnServer(s -> {
				ItemStack worn = Backpacks.worn(p(s));
				check(inside(worn, Items.APPLE) == 5 && inside(worn, Items.COBBLESTONE) == 16, "The worn backpack keeps both stacks");
				check(p(s).getInventory().countItem(Items.APPLE) == 0, "The apples left the inventory");
			});
			save = w.getWorldSave();
		}

		try (var w = save.open()) {
			c.waitTicks(30);
			w.getServer().runOnServer(s -> {
				ItemStack worn = Backpacks.worn(p(s));
				check(worn.is(BackpackItem.BACKPACK), "The worn backpack is kept through a save and reopen");
				check(inside(worn, Items.APPLE) == 5 && inside(worn, Items.COBBLESTONE) == 16, "And so is what is in it");
			});
		}
	}
}
