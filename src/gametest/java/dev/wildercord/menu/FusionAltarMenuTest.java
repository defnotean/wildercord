package dev.wildercord.menu;

import dev.wildercord.content.RuneItem;
import dev.wildercord.content.WildercordBlocks;
import dev.wildercord.player.Heart;
import dev.wildercord.spell.Fusions;
import dev.wildercord.spell.Ranks;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * The Fusion Altar in the real game, clicked through the client: used, it opens its menu; three matching runes
 * shift-clicked in spread over the three sockets and rank up for their XP (refused without it); two effects and a shard
 * make their elements' fusion, which is refused while the last result is still waiting and then unlocks its Grimoire
 * entry; what can't go on the altar stays in the inventory; closing it hands back what was left on it; taking the
 * altar away closes the menu; and what was made and learned is kept through a save and reopen.
 */
public final class FusionAltarMenuTest implements FabricClientGameTest {
	private static final BlockPos ALTAR = new BlockPos(0, 101, 0);
	/** Menu index of hotbar slot {@code i}: after the five altar slots and the three inventory rows. */
	private static int hotbar(int i) {
		return FusionAltarMenu.RESULT + 1 + 27 + i;
	}

	private static RuneDef ranked, first, second;
	private static String recipeKey;

	private static void check(boolean x, String why) {
		if (!x) throw new AssertionError(why);
	}

	private static ServerPlayer p(MinecraftServer s) {
		return s.getPlayerList().getPlayers().getFirst();
	}

	private static FusionAltarMenu menu(MinecraftServer s) {
		return p(s).containerMenu instanceof FusionAltarMenu m ? m : null;
	}

	private static boolean onAltar(MinecraftServer s, int slot) {
		return !menu(s).input(slot).isEmpty();
	}

	/** How many of {@code rune} at {@code rank} the player carries. */
	private static int carried(ServerPlayer p, RuneDef rune, int rank) {
		int n = 0;
		for (int i = 0; i < 36; i++) {
			ItemStack stack = p.getInventory().getItem(i);
			if (RuneItem.runeOf(stack).filter(r -> r.is(rune.id())).isPresent() && RuneItem.rankOf(stack) == rank) n += stack.getCount();
		}
		return n;
	}

	private static void click(ClientGameTestContext c, int slot, ContainerInput input) {
		c.runOnClient(mc -> mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, slot, 0, input, mc.player));
		c.waitTicks(3);
	}

	private static void press(ClientGameTestContext c) {
		c.runOnClient(mc -> mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId, FusionAltarMenu.BUTTON_FUSE));
		c.waitTicks(3);
	}

	private static void open(ClientGameTestContext c) {
		c.runOnClient(mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(ALTAR).add(0, 0.375, 0), Direction.UP, ALTAR, false)));
		c.waitTicks(5);
	}

	@Override
	public void runTest(ClientGameTestContext c) {
		// An effect with ranks, and two effects that make their elements' fusion (not a signature).
		ranked = Runes.all().stream().filter(Ranks::rankable).findFirst().orElseThrow();
		var fusible = Runes.all().stream().filter(Fusions::fusible).toList();
		outer:
		for (RuneDef a : fusible) {
			for (RuneDef b : fusible) {
				if (!a.is(b.id()) && Fusions.recipe(a, b).filter(f -> f instanceof Fusions.Recipe).isPresent()) {
					first = a;
					second = b;
					break outer;
				}
			}
		}
		check(first != null, "Some pair of effects makes an element fusion");

		TestWorldSave save;
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(25);
			TestServerContext server = w.getServer();
			server.runCommand("gamerule spawn_mobs false");
			server.runOnServer(s -> {
				var l = s.overworld();
				for (int x = -3; x <= 3; x++) for (int z = -3; z <= 4; z++) {
					l.setBlock(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState(), 2);
					for (int y = 101; y <= 103; y++) l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
				}
				l.setBlock(ALTAR, WildercordBlocks.FUSION_ALTAR.defaultBlockState(), 3);
				p(s).setGameMode(GameType.SURVIVAL);
				p(s).teleportTo(l, 0.5, 101, 2.5, Set.<Relative>of(), 180, 30, false);
				var inv = p(s).getInventory();
				inv.clearContent();
				inv.setSelectedSlot(8);
				ItemStack three = RuneItem.stack(ranked);
				three.setCount(3);
				inv.setItem(0, three);
				inv.setItem(1, RuneItem.stack(first));
				inv.setItem(2, RuneItem.stack(second));
				inv.setItem(3, new ItemStack(Items.AMETHYST_SHARD));
				inv.setItem(4, new ItemStack(Items.COBBLESTONE, 4));
				p(s).giveExperienceLevels(1 - p(s).experienceLevel);
			});
			c.waitTicks(5);

			// Used, the altar opens its menu.
			open(c);
			server.runOnServer(s -> check(menu(s) != null, "Using the altar opens it"));
			check(c.computeOnClient(mc -> mc.player.containerMenu instanceof FusionAltarMenu), "The client shows the altar");

			// Three matching runes spread over the sockets and plan a rank-up.
			click(c, hotbar(0), ContainerInput.QUICK_MOVE);
			server.runOnServer(s -> {
				check(onAltar(s, 0) && onAltar(s, 1) && onAltar(s, 2), "A shift-click spreads three runes over the three sockets");
				var plan = menu(s).plan();
				check(plan.ready() && plan.kind() == Fusions.Kind.UPGRADE && plan.rank() == 2, "Three matching runes plan a rank-up, " + plan);
			});
			check(c.computeOnClient(mc -> mc.player.containerMenu instanceof FusionAltarMenu m && m.plan().kind() == Fusions.Kind.UPGRADE),
				"The screen shows the same plan");

			// Without the XP it is refused and nothing is used.
			press(c);
			server.runOnServer(s -> {
				check(menu(s).resultFree() && onAltar(s, 0) && onAltar(s, 1) && onAltar(s, 2), "Without the XP nothing is used");
				check(p(s).experienceLevel == 1, "Nor is any XP taken");
				p(s).giveExperienceLevels(10 - p(s).experienceLevel);
			});
			c.waitTicks(2);
			press(c);
			server.runOnServer(s -> {
				ItemStack made = menu(s).getSlot(FusionAltarMenu.RESULT).getItem();
				check(RuneItem.runeOf(made).filter(r -> r.is(ranked.id())).isPresent() && RuneItem.rankOf(made) == 2, "The rank-up makes a rank II rune");
				check(!onAltar(s, 0) && !onAltar(s, 1) && !onAltar(s, 2), "and uses the three runes");
				check(p(s).experienceLevel == 10 - Ranks.xpCost(2), "and its XP, level " + p(s).experienceLevel);
			});

			// Two effects and a shard plan their elements' fusion; it waits until the last result is taken.
			click(c, hotbar(1), ContainerInput.QUICK_MOVE);
			click(c, hotbar(2), ContainerInput.QUICK_MOVE);
			click(c, hotbar(3), ContainerInput.QUICK_MOVE);
			server.runOnServer(s -> {
				var plan = menu(s).plan();
				check(plan.ready() && plan.kind() == Fusions.Kind.COMBINE && plan.recipe() != null, "Two effects and a shard plan a fusion, " + plan);
				check(!menu(s).input(FusionAltarMenu.CATALYST).isEmpty(), "The shard goes in the catalyst slot");
				recipeKey = plan.recipe().key();
			});
			press(c);
			server.runOnServer(s -> {
				check(onAltar(s, 0) && onAltar(s, 1) && !menu(s).input(FusionAltarMenu.CATALYST).isEmpty(),
					"Nothing is used while the last result still waits");
				check(p(s).experienceLevel == 10 - Ranks.xpCost(2), "and no XP is taken");
			});
			click(c, FusionAltarMenu.RESULT, ContainerInput.QUICK_MOVE);
			server.runOnServer(s -> check(menu(s).resultFree() && carried(p(s), ranked, 2) == 1, "The rank II rune is taken into the inventory"));
			c.takeScreenshot(TestScreenshotOptions.of("fusion_altar_combine").disableCounterPrefix());
			press(c);
			server.runOnServer(s -> {
				var made = menu(s).getSlot(FusionAltarMenu.RESULT).getItem();
				var expected = Fusions.recipe(first, second).orElseThrow().result();
				check(RuneItem.runeOf(made).filter(r -> r.is(expected.id())).isPresent(), "The fusion makes " + expected.name());
				check(!onAltar(s, 0) && !onAltar(s, 1) && menu(s).input(FusionAltarMenu.CATALYST).isEmpty(), "and uses both effects and the shard");
				check(p(s).experienceLevel == 10 - Ranks.xpCost(2) - Fusions.COMBINE_XP, "and its XP, level " + p(s).experienceLevel);
				check(Heart.grimoire(p(s)).contains(recipeKey), "and writes the fusion in the Grimoire");
			});

			// What can't go on the altar stays in the inventory.
			click(c, hotbar(4), ContainerInput.QUICK_MOVE);
			server.runOnServer(s -> check(p(s).getInventory().getItem(4).getCount() == 4, "Cobblestone can't go on the altar"));

			// Closing it hands back what was left on it.
			click(c, FusionAltarMenu.RESULT, ContainerInput.QUICK_MOVE);
			int rankedSlot = server.computeOnServer(s -> {
				var inv = p(s).getInventory();
				for (int i = 0; i < 36; i++) {
					if (RuneItem.runeOf(inv.getItem(i)).filter(r -> r.is(ranked.id())).isPresent()) return i;
				}
				return -1;
			});
			check(rankedSlot >= 0, "The rank II rune is in the inventory");
			click(c, rankedSlot < 9 ? hotbar(rankedSlot) : FusionAltarMenu.RESULT + 1 + rankedSlot - 9, ContainerInput.QUICK_MOVE);
			server.runOnServer(s -> check(onAltar(s, 0) && carried(p(s), ranked, 2) == 0, "The rank II rune goes back on the altar"));
			c.runOnClient(mc -> mc.player.closeContainer());
			c.waitTicks(5);
			server.runOnServer(s -> {
				check(menu(s) == null, "The menu closes");
				check(carried(p(s), ranked, 2) == 1, "and what was on the altar comes back");
			});

			// Taking the altar away closes the menu.
			open(c);
			server.runOnServer(s -> {
				check(menu(s) != null, "The altar opens again");
				s.overworld().setBlock(ALTAR, Blocks.AIR.defaultBlockState(), 3);
			});
			c.waitTicks(5);
			server.runOnServer(s -> check(menu(s) == null, "Without its altar the menu closes"));
			check(c.computeOnClient(mc -> !(mc.player.containerMenu instanceof FusionAltarMenu)), "on the client too");
			save = w.getWorldSave();
		}

		try (var w = save.open()) {
			c.waitTicks(30);
			w.getServer().runOnServer(s -> {
				var expected = Fusions.recipe(first, second).orElseThrow().result();
				check(carried(p(s), ranked, 2) == 1 && carried(p(s), expected, 1) == 1, "What the altar made is kept through a save and reopen");
				check(Heart.grimoire(p(s)).contains(recipeKey), "and so is the Grimoire entry");
			});
		}
	}
}
