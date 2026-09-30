package dev.wildercord.gametest;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.JsonOps;
import dev.wildercord.backpack.BackpackTier;
import dev.wildercord.backpack.Backpacks;
import dev.wildercord.client.BackpackScreen;
import dev.wildercord.client.WildercordKeys;
import dev.wildercord.client.render.GearLook;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.gear.GearDef;
import dev.wildercord.gear.GearItems;
import dev.wildercord.gear.GearSlot;
import dev.wildercord.gear.GearSlots;
import dev.wildercord.menu.BackpackMenu;
import dev.wildercord.menu.BackpackSlot;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Backpacks in a real world: crafted, and upgraded keeping what's inside (by the recipe, and at a real crafting
 * table); opened with the backpack key (worn) and by using one (in hand); items moved in and out with the real
 * mouse and shift-click; reopening keeps everything, and the server's copy matches the screen; nothing that
 * holds items goes in (a backpack, a shulker box, a bundle, a filled chest), by click, shift-click or number
 * key, nor through a hopper into a shulker box. Then every way to duplicate an open backpack fails: a number
 * key or F to swap it out, Q and Ctrl+Q on it, picking it up, and what a modified client could send (dropping
 * or swapping the held item while its menu is open), dying with it open, and closing the world with it open.
 * Dropped, it keeps its contents, and picked up again; burnt, it spills them (the Runewoven one doesn't burn).
 * Screenshots of the screens and the worn look go to {@code build/run/clientGameTest/screenshots/backpack_*}.
 *
 * <p>Runs in the full suite; skipped with {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} or
 * {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordBackpackTest implements FabricClientGameTest {
	private static final int LEFT = InputConstants.MOUSE_BUTTON_LEFT;
	private static final int RIGHT = InputConstants.MOUSE_BUTTON_RIGHT;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		TestWorldSave save;
		List<String> failures = new ArrayList<>();
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			context.runOnClient(mc -> {
				mc.getWindow().setWindowed(1280, 720);
				mc.options.guiScale().set(2);
				mc.resizeGui();
			});
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule keep_inventory false");
			List<Runnable> steps = List.of(
				() -> recipes(world),
				() -> craftingTable(world),
				() -> whatFits(context, world),
				() -> openWorn(context, world),
				() -> openInHand(context, world),
				() -> swapAndDropWhileOpen(context, world),
				() -> modifiedClient(context, world),
				() -> dyingWithItOpen(context, world),
				() -> droppedAndPickedUp(context, world),
				() -> inventoryKeyAndSlot(context, world),
				() -> screens(context, world),
				() -> wearing(context, world));
			for (Runnable step : steps) {
				try {
					step.run();
				} catch (AssertionError e) {
					failures.add(e.getMessage());
				}
				resetScreen(context, world);
			}
			// Closing the world with a worn backpack open, just after moving diamonds into it: they must come back in
			// the backpack, and only there.
			try {
				leaveWithItOpen(context, world);
			} catch (AssertionError e) {
				failures.add(e.getMessage());
			}
			save = world.getWorldSave();
		}
		try (TestSingleplayerContext again = save.open()) {
			context.waitTicks(40);
			try {
				afterLeaving(context, again);
			} catch (AssertionError e) {
				failures.add(e.getMessage());
			}
		}
		if (!failures.isEmpty()) {
			throw new AssertionError("Backpacks went wrong:\n  " + String.join("\n  ", failures));
		}
	}

	// ------------------------------------------------------------------ helpers

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static void check(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError(what);
		}
	}

	/** A backpack holding these stacks, from its first slot on. */
	private static ItemStack backpack(Item item, ItemStack... contents) {
		ItemStack stack = new ItemStack(item);
		stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(Arrays.asList(contents)));
		return stack;
	}

	/** What a backpack holds, slot by slot, for comparing and for messages. */
	private static String describe(ItemStack backpack) {
		return describe(Backpacks.contents(backpack));
	}

	private static String describe(List<ItemStack> items) {
		List<String> out = new ArrayList<>();
		for (int i = 0; i < items.size(); i++) {
			ItemStack stack = items.get(i);
			if (!stack.isEmpty()) {
				out.add(i + ":" + BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath() + "x" + stack.getCount());
			}
		}
		return String.join(" ", out);
	}

	/** How many of an item the player has anywhere: inventory, off-hand and armour, cursor, worn backpack, and inside every backpack they carry. */
	private static int owned(ServerPlayer player, Item item) {
		int n = count(player.containerMenu.getCarried(), item) + count(Backpacks.worn(player), item);
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			n += count(inventory.getItem(i), item);
		}
		return n;
	}

	/** How many of an item lie in the world near the player, loose or inside dropped backpacks. */
	private static int dropped(ServerPlayer player, Item item) {
		int n = 0;
		for (ItemEntity entity : drops(player)) {
			n += count(entity.getItem(), item);
		}
		return n;
	}

	/** A stack's count of an item, and what's inside it if it's a backpack. */
	private static int count(ItemStack stack, Item item) {
		int n = stack.is(item) ? stack.getCount() : 0;
		if (Backpacks.isBackpack(stack)) {
			for (ItemStack inside : Backpacks.contents(stack)) {
				n += inside.is(item) ? inside.getCount() : 0;
			}
		}
		return n;
	}

	private static List<ItemEntity> drops(ServerPlayer player) {
		return player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(16));
	}

	private static void clearDrops(ServerPlayer player) {
		drops(player).forEach(Entity::discard);
	}

	private static void resetScreen(ClientGameTestContext context, TestSingleplayerContext world) {
		context.runOnClient(mc -> {
			if (mc.gui.screen() != null) {
				mc.gui.setScreen(null);
			}
			if (mc.player != null) {
				mc.setCameraEntity(mc.player);
			}
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			if (mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		context.waitTicks(3);
	}

	/** A survival player with an empty inventory and Backpack slot, nothing open, on cleared ground, looking a little up at open sky. */
	private static Vec3 ready(ServerPlayer player) {
		if (player.containerMenu != player.inventoryMenu) {
			player.closeContainer();
		}
		player.setGameMode(GameType.SURVIVAL);
		player.getInventory().clearContent();
		player.containerMenu.setCarried(ItemStack.EMPTY);
		Backpacks.takeOff(player);
		for (GearSlot slot : GearSlot.all()) {
			GearSlots.clear(player, slot);
		}
		for (EquipmentSlot equipment : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
			player.setItemSlot(equipment, ItemStack.EMPTY);
		}
		player.removeAllEffects();
		player.setShiftKeyDown(false);
		ServerLevel level = player.level();
		BlockPos spot = player.blockPosition();
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spot.getX(), spot.getZ());
		Vec3 at = new Vec3(spot.getX() + 0.5, y, spot.getZ() + 0.5);
		for (int dx = -5; dx <= 5; dx++) {
			for (int dz = -5; dz <= 5; dz++) {
				for (int dy = 0; dy <= 4; dy++) {
					level.setBlockAndUpdate(BlockPos.containing(at.x + dx, at.y + dy, at.z + dz), Blocks.AIR.defaultBlockState());
				}
			}
		}
		player.teleportTo(level, at.x, at.y, at.z, Set.<Relative>of(), 0, -30, false);
		clearDrops(player);
		return at;
	}

	/** Selects a hotbar slot the way the player does, so the client and the server agree on it. */
	private static void select(ClientGameTestContext context, int slot) {
		context.getInput().pressKey(options -> options.keyHotbarSlots[slot]);
		context.waitTicks(3);
	}

	private static int intField(Object screen, String name) {
		try {
			java.lang.reflect.Field field = AbstractContainerScreen.class.getDeclaredField(name);
			field.setAccessible(true);
			return field.getInt(screen);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("couldn't read " + name + " off the screen: " + e);
		}
	}

	/** The middle of a slot on the open container screen, in window pixels. */
	private static double[] pixel(ClientGameTestContext context, Predicate<Slot> which) {
		return context.computeOnClient(mc -> {
			if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
				return null;
			}
			for (Slot slot : screen.getMenu().slots) {
				if (which.test(slot)) {
					double scale = mc.getWindow().getGuiScale();
					return new double[]{(intField(screen, "leftPos") + slot.x + 8) * scale, (intField(screen, "topPos") + slot.y + 8) * scale};
				}
			}
			return null;
		});
	}

	private static Container clientInventory() {
		return net.minecraft.client.Minecraft.getInstance().player.getInventory();
	}

	/** One of the player's own inventory slots on the open screen. */
	private static Predicate<Slot> inventorySlot(int index) {
		return slot -> slot.container == clientInventory() && slot.getContainerSlot() == index;
	}

	/** One of the open backpack's own slots. */
	private static Predicate<Slot> insideSlot(int index) {
		return slot -> slot.container != clientInventory() && !(slot.container instanceof BackpackSlot.WornContainer) && slot.getContainerSlot() == index;
	}

	private static Predicate<Slot> wornSlot() {
		return slot -> slot.container instanceof BackpackSlot.WornContainer;
	}

	private static void hover(ClientGameTestContext context, double[] at) {
		check(at != null, "nothing to point at there");
		context.getInput().setCursorPos(at[0], at[1]);
		context.waitTicks(2);
	}

	private static void click(ClientGameTestContext context, double[] at) {
		hover(context, at);
		context.getInput().pressMouse(LEFT);
		context.waitTicks(4);
	}

	/** A shift-click: the screen's own call, as the game sends it (the test input can't hold Shift through a mouse press). */
	private static void shiftClick(ClientGameTestContext context, Predicate<Slot> which) {
		context.runOnClient(mc -> {
			AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) mc.gui.screen();
			for (Slot slot : screen.getMenu().slots) {
				if (which.test(slot)) {
					mc.gameMode.handleContainerInput(screen.getMenu().containerId, slot.index, 0, ContainerInput.QUICK_MOVE, mc.player);
					return;
				}
			}
			throw new AssertionError("no such slot to shift-click");
		});
		context.waitTicks(4);
	}

	private static boolean backpackOpen(ClientGameTestContext context) {
		return context.computeOnClient(mc -> mc.gui.screen() instanceof BackpackScreen);
	}

	private static void waitForBackpack(ClientGameTestContext context, String what) {
		for (int i = 0; i < 40 && !backpackOpen(context); i++) {
			context.waitTicks(1);
		}
		check(backpackOpen(context), what);
		context.waitTicks(3);
	}

	/** The server's open backpack menu, or null. */
	private static BackpackMenu serverMenu(ServerPlayer player) {
		return player.containerMenu instanceof BackpackMenu menu ? menu : null;
	}

	/** What the open backpack screen shows in the backpack's own slots. */
	private static String shown(ClientGameTestContext context) {
		return context.computeOnClient(mc -> {
			BackpackMenu menu = ((BackpackScreen) mc.gui.screen()).getMenu();
			List<ItemStack> items = new ArrayList<>();
			for (int i = 0; i < menu.rows() * 9; i++) {
				items.add(menu.getSlot(i).getItem());
			}
			return describe(items);
		});
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(mc -> mc.gui.toastManager().clear());
		context.takeScreenshot(TestScreenshotOptions.of("backpack_" + name).disableCounterPrefix());
	}

	// ------------------------------------------------------------------ crafting

	private static List<ItemStack> grid(Object... items) {
		List<ItemStack> out = new ArrayList<>();
		for (Object item : items) {
			out.add(item instanceof ItemStack stack ? stack : item == null ? ItemStack.EMPTY : new ItemStack((Item) item));
		}
		while (out.size() < 9) {
			out.add(ItemStack.EMPTY);
		}
		return out;
	}

	private static ItemStack craft(MinecraftServer server, List<ItemStack> grid) {
		CraftingInput input = CraftingInput.of(3, 3, grid);
		var recipe = server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, player(server).level());
		return recipe.isEmpty() ? ItemStack.EMPTY : recipe.get().value().assemble(input);
	}

	/** The recipes: a Backpack from leather, string and a chest; each upgrade keeps the contents, colour and name; dyeing keeps them too. */
	private static void recipes(TestSingleplayerContext world) {
		String problem = world.getServer().computeOnServer(server -> {
			ItemStack made = craft(server, grid(null, Items.STRING, null, Items.LEATHER, Items.CHEST, Items.LEATHER, Items.LEATHER, Items.LEATHER, Items.LEATHER));
			if (!made.is(WildercordItems.BACKPACK) || Backpacks.used(made) != 0 || made.getMaxStackSize() != 1) {
				return "string over a chest in leather should make an empty Backpack that stacks to one (made " + made + ")";
			}
			ItemStack old = backpack(WildercordItems.BACKPACK, new ItemStack(Items.COBBLESTONE, 64), ItemStack.EMPTY, new ItemStack(Items.DIAMOND, 3));
			old.set(DataComponents.DYED_COLOR, new DyedItemColor(0xB02E26));
			old.set(DataComponents.CUSTOM_NAME, Component.literal("Trusty"));
			String before = describe(old);
			// Shapeless, the old backpack anywhere in the grid.
			ItemStack reinforced = craft(server, grid(Items.IRON_INGOT, Items.LEATHER, Items.CHEST, Items.IRON_INGOT, old.copy(), Items.IRON_INGOT, Items.LEATHER, Items.IRON_INGOT));
			if (!reinforced.is(WildercordItems.REINFORCED_BACKPACK)) {
				return "a Backpack, a chest, 4 iron and 2 leather should make a Reinforced Backpack (made " + reinforced + ")";
			}
			if (!describe(reinforced).equals(before) || DyedItemColor.getOrDefault(reinforced, 0) != DyedItemColor.getOrDefault(old, 0)
					|| !"Trusty".equals(reinforced.getHoverName().getString())) {
				return "the Reinforced Backpack should keep what was in the Backpack, its colour and its name (" + describe(reinforced) + " vs " + before + ")";
			}
			ItemStack runewoven = craft(server, grid(Items.AMETHYST_SHARD, WildercordItems.BLANK_RUNE, Items.AMETHYST_SHARD, reinforced, WildercordItems.MANA_CRYSTAL,
				Items.ECHO_SHARD, Items.AMETHYST_SHARD, WildercordItems.BLANK_RUNE, Items.AMETHYST_SHARD));
			if (!runewoven.is(WildercordItems.RUNEWOVEN_BACKPACK) || !describe(runewoven).equals(before)) {
				return "the Runewoven Backpack should be made from the Reinforced one and keep its contents (made " + runewoven + ": " + describe(runewoven) + ")";
			}
			if (!craft(server, grid(Items.AMETHYST_SHARD, WildercordItems.BLANK_RUNE, Items.AMETHYST_SHARD, old.copy(), WildercordItems.MANA_CRYSTAL,
					Items.ECHO_SHARD, Items.AMETHYST_SHARD, WildercordItems.BLANK_RUNE, Items.AMETHYST_SHARD)).isEmpty()) {
				return "the Runewoven Backpack shouldn't skip the Reinforced one";
			}
			ItemStack blue = craft(server, grid(runewoven, new ItemStack(Items.DYE.pick(DyeColor.BLUE))));
			if (!blue.is(WildercordItems.RUNEWOVEN_BACKPACK) || !describe(blue).equals(before) || !blue.has(DataComponents.DYED_COLOR)
					|| DyedItemColor.getOrDefault(blue, 0) == DyedItemColor.getOrDefault(runewoven, 0)) {
				return "dyeing a backpack should change its colour and keep what's in it (" + blue + ": " + describe(blue) + ")";
			}
			ItemStack dyed = craft(server, grid(new ItemStack(WildercordItems.BACKPACK), new ItemStack(Items.DYE.pick(DyeColor.LIME))));
			if (!dyed.is(WildercordItems.BACKPACK) || DyedItemColor.getOrDefault(dyed, 0) != DyedItemColor.applyDyes((DyedItemColor) null, List.of(DyeColor.LIME)).rgb() + 0xFF000000) {
				return "a plain Backpack should dye like leather (" + dyed + ")";
			}
			return null;
		});
		check(problem == null, problem);
	}

	/** The upgrade made at a real crafting table, taken out of the result slot: what was inside comes along, and the old backpack is used up. */
	private static void craftingTable(TestSingleplayerContext world) {
		String problem = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			Vec3 at = ready(player);
			ServerLevel level = player.level();
			BlockPos table = BlockPos.containing(at.x + 1, at.y, at.z);
			level.setBlockAndUpdate(table, Blocks.CRAFTING_TABLE.defaultBlockState());
			player.openMenu(new SimpleMenuProvider((id, inventory, who) -> new CraftingMenu(id, inventory, ContainerLevelAccess.create(level, table)), Component.literal("Crafting")));
			if (!(player.containerMenu instanceof CraftingMenu menu)) {
				return "the crafting table should open";
			}
			ItemStack old = backpack(WildercordItems.BACKPACK, new ItemStack(Items.EMERALD, 7), new ItemStack(Items.TORCH, 16));
			String before = describe(old);
			List<ItemStack> grid = grid(old, Items.CHEST, Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT, Items.LEATHER, Items.LEATHER);
			for (int i = 0; i < 9; i++) {
				menu.getSlot(1 + i).set(grid.get(i));
			}
			if (!menu.getSlot(0).getItem().is(WildercordItems.REINFORCED_BACKPACK)) {
				return "the table should offer a Reinforced Backpack (offers " + menu.getSlot(0).getItem() + ")";
			}
			menu.clicked(0, 0, ContainerInput.QUICK_MOVE, player);
			ItemStack made = ItemStack.EMPTY;
			for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
				if (stack.is(WildercordItems.REINFORCED_BACKPACK)) {
					made = stack;
				}
			}
			boolean usedUp = true;
			for (int i = 1; i <= 9; i++) {
				usedUp &= menu.getSlot(i).getItem().isEmpty();
			}
			player.closeContainer();
			level.setBlockAndUpdate(table, Blocks.AIR.defaultBlockState());
			if (made.isEmpty() || !describe(made).equals(before)) {
				return "the Reinforced Backpack taken from the table should hold what the Backpack held (" + describe(made) + " vs " + before + ")";
			}
			if (!usedUp || owned(player, WildercordItems.BACKPACK) != 0) {
				return "crafting should use the old Backpack up";
			}
			return owned(player, Items.EMERALD) == 7 ? null : "there should still be exactly 7 emeralds (" + owned(player, Items.EMERALD) + ")";
		});
		check(problem == null, problem);
	}

	// ------------------------------------------------------------------ what goes in

	/**
	 * Nothing that holds items fits: a backpack, a shulker box (empty or full), a bundle, a chest carrying its
	 * contents; an empty chest or furnace, and anything plain, does. A hopper can't feed a backpack into a shulker
	 * box either, and a dispenser throws one out whole.
	 */
	private static void whatFits(ClientGameTestContext context, TestSingleplayerContext world) {
		String problem = world.getServer().computeOnServer(server -> {
			ItemStack fullChest = new ItemStack(Items.CHEST);
			fullChest.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND))));
			ItemStack fullShulker = new ItemStack(Items.SHULKER_BOX);
			fullShulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND))));
			for (ItemStack refused : List.of(new ItemStack(WildercordItems.BACKPACK), new ItemStack(WildercordItems.REINFORCED_BACKPACK),
					new ItemStack(WildercordItems.RUNEWOVEN_BACKPACK), new ItemStack(Items.SHULKER_BOX), new ItemStack(Items.DYED_SHULKER_BOX.pick(DyeColor.BLUE)), fullShulker,
					new ItemStack(Items.BUNDLE), new ItemStack(Items.DYED_BUNDLE.pick(DyeColor.RED)), fullChest)) {
				if (Backpacks.fitsInside(refused)) {
					return refused + " (" + refused.getComponents() + ") shouldn't fit in a backpack";
				}
			}
			for (ItemStack fits : List.of(new ItemStack(Items.CHEST), new ItemStack(Items.FURNACE), new ItemStack(Items.STONE, 64), new ItemStack(Items.DIAMOND_SWORD),
					new ItemStack(WildercordItems.ECHO_CORD), ItemStack.EMPTY)) {
				if (!Backpacks.fitsInside(fits)) {
					return fits + " should fit in a backpack";
				}
			}
			ServerPlayer player = player(server);
			Vec3 at = ready(player);
			ServerLevel level = player.level();
			// A hopper over a shulker box, holding a backpack and some stone: the stone goes down, the backpack stays.
			BlockPos box = BlockPos.containing(at.x + 3, at.y, at.z + 3);
			level.setBlockAndUpdate(box, Blocks.SHULKER_BOX.defaultBlockState());
			level.setBlockAndUpdate(box.above(), Blocks.HOPPER.defaultBlockState());
			if (level.getBlockEntity(box.above()) instanceof HopperBlockEntity hopper) {
				hopper.setItem(0, backpack(WildercordItems.BACKPACK, new ItemStack(Items.DIAMOND, 2)));
				hopper.setItem(1, new ItemStack(Items.STONE, 1));
			}
			// A dispenser throwing a backpack out.
			BlockPos dispenser = BlockPos.containing(at.x - 3, at.y, at.z - 3);
			level.setBlockAndUpdate(dispenser, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.UP));
			if (level.getBlockEntity(dispenser) instanceof DispenserBlockEntity block) {
				block.setItem(0, backpack(WildercordItems.REINFORCED_BACKPACK, new ItemStack(Items.GOLD_INGOT, 5)));
			}
			level.setBlockAndUpdate(dispenser.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
			return null;
		});
		check(problem == null, problem);
		context.waitTicks(60);
		String machines = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			BlockPos box = null;
			for (BlockPos pos : BlockPos.betweenClosed(player.blockPosition().offset(-6, -2, -6), player.blockPosition().offset(6, 3, 6))) {
				if (level.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity) {
					box = pos.immutable();
				}
			}
			if (box == null || !(level.getBlockEntity(box) instanceof ShulkerBoxBlockEntity shulker) || !(level.getBlockEntity(box.above()) instanceof HopperBlockEntity hopper)) {
				return "the shulker box and its hopper should be there";
			}
			boolean stoneWent = false;
			boolean backpackWent = false;
			for (int i = 0; i < shulker.getContainerSize(); i++) {
				stoneWent |= shulker.getItem(i).is(Items.STONE);
				backpackWent |= Backpacks.isBackpack(shulker.getItem(i));
			}
			boolean stillInHopper = false;
			for (int i = 0; i < hopper.getContainerSize(); i++) {
				stillInHopper |= hopper.getItem(i).is(WildercordItems.BACKPACK);
			}
			if (!stoneWent || backpackWent || !stillInHopper) {
				return "a hopper should feed stone into a shulker box but not a backpack (stone went " + stoneWent + ", backpack went " + backpackWent + ")";
			}
			ItemEntity thrown = null;
			for (ItemEntity entity : drops(player)) {
				if (entity.getItem().is(WildercordItems.REINFORCED_BACKPACK)) {
					thrown = entity;
				}
			}
			if (thrown == null || count(thrown.getItem(), Items.GOLD_INGOT) != 5) {
				return "a dispenser should throw a backpack out with its 5 gold ingots still inside";
			}
			for (BlockPos pos : BlockPos.betweenClosed(player.blockPosition().offset(-6, -2, -6), player.blockPosition().offset(6, 3, 6))) {
				if (!level.getBlockState(pos).isAir() && pos.getY() >= player.blockPosition().getY()) {
					if (level.getBlockEntity(pos) instanceof Container container) {
						container.clearContent();
					}
					level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
				}
			}
			clearDrops(player);
			return null;
		});
		check(machines == null, machines);
	}

	// ------------------------------------------------------------------ opening, and moving things

	/** The worn backpack, opened with the backpack key: moving things in and out with the mouse, and reopening. */
	private static void openWorn(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ready(player);
			Backpacks.wear(player, backpack(WildercordItems.BACKPACK, new ItemStack(Items.TORCH, 16)));
			player.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 32));
			player.getInventory().setItem(10, new ItemStack(Items.BREAD, 20));
		});
		context.waitTicks(5);
		context.getInput().pressKey(WildercordKeys.backpackMapping());
		waitForBackpack(context, "the backpack key should open the worn backpack");
		int rows = context.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).getMenu().rows());
		int locked = context.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).getMenu().locked());
		String title = context.computeOnClient(mc -> mc.gui.screen().getTitle().getString());
		check(rows == 2 && locked == -1 && title.equals("Backpack"), "the worn Backpack should open with 2 rows, nothing locked, titled Backpack (" + rows + ", " + locked + ", " + title + ")");
		check(world.getServer().computeOnServer(server -> serverMenu(player(server)) != null), "the server should have the backpack open");
		check(shown(context).equals("0:torchx16"), "the screen should show the 16 torches inside (" + shown(context) + ")");
		// The diamonds in with the mouse: picked up, put down in the backpack's fourth slot.
		click(context, pixel(context, inventorySlot(9)));
		click(context, pixel(context, insideSlot(3)));
		String worn = world.getServer().computeOnServer(server -> describe(Backpacks.worn(player(server))));
		check(worn.equals("0:torchx16 3:diamondx32"), "clicking the diamonds into the backpack should put them in the worn backpack at once (it holds " + worn + ")");
		// The bread in by shift-click, the torches out the same way.
		shiftClick(context, inventorySlot(10));
		shiftClick(context, insideSlot(0));
		String server = world.getServer().computeOnServer(s -> describe(Backpacks.worn(player(s))));
		String screen = shown(context);
		check(server.equals("1:breadx20 3:diamondx32") && server.equals(screen),
			"shift-clicking bread in and torches out should leave bread and diamonds (server " + server + ", screen " + screen + ")");
		check(world.getServer().computeOnServer(s -> owned(player(s), Items.TORCH) == 16 && owned(player(s), Items.DIAMOND) == 32 && owned(player(s), Items.BREAD) == 20),
			"nothing should be gained or lost moving things about");
		// Closed with Escape and opened again with the key: the same things, where they were.
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitTicks(5);
		check(!backpackOpen(context) && world.getServer().computeOnServer(s -> serverMenu(player(s)) == null), "Escape should close the backpack");
		String afterClose = context.computeOnClient(mc -> describe(Backpacks.worn(mc.player)));
		check(afterClose.equals(server), "after closing, the client's copy of the worn backpack should match the server's (" + afterClose + " vs " + server + ")");
		context.getInput().pressKey(WildercordKeys.backpackMapping());
		waitForBackpack(context, "the backpack key should open it again");
		check(shown(context).equals(server), "reopened, the backpack should show the same things in the same slots (" + shown(context) + ")");
		// The backpack key closes it too, as the inventory key would.
		context.getInput().pressKey(WildercordKeys.backpackMapping());
		context.waitTicks(5);
		check(!backpackOpen(context), "the backpack key should close an open backpack");
		// With no backpack worn the key says so, and opens nothing.
		world.getServer().runOnServer(s -> Backpacks.takeOff(player(s)));
		context.waitTicks(3);
		context.getInput().pressKey(WildercordKeys.backpackMapping());
		context.waitTicks(5);
		check(!backpackOpen(context), "with nothing worn the backpack key shouldn't open anything");
	}

	/** A backpack in hand, opened by using it: its slot is locked, and nothing that holds items goes in, by click, shift-click or number key. */
	private static void openInHand(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ready(player);
			var inventory = player.getInventory();
			inventory.setItem(0, backpack(WildercordItems.REINFORCED_BACKPACK, new ItemStack(Items.EMERALD, 5)));
			ItemStack shulker = new ItemStack(Items.SHULKER_BOX);
			shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 64))));
			inventory.setItem(1, shulker);
			inventory.setItem(2, new ItemStack(Items.BUNDLE));
			inventory.setItem(3, new ItemStack(WildercordItems.BACKPACK));
			inventory.setItem(4, new ItemStack(Items.STONE, 64));
		});
		context.waitTicks(5);
		select(context, 0);
		context.getInput().pressMouse(RIGHT);
		waitForBackpack(context, "using a backpack in hand should open it");
		int rows = context.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).getMenu().rows());
		int locked = context.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).getMenu().locked());
		check(rows == 3 && locked == 0, "the Reinforced Backpack should open with 3 rows, its hotbar slot (0) locked (" + rows + ", " + locked + ")");
		// Each of these, clicked onto an empty backpack slot, is refused and stays on the cursor; put back where it was.
		for (int hotbar : new int[]{1, 2, 3}) {
			click(context, pixel(context, inventorySlot(hotbar)));
			click(context, pixel(context, insideSlot(8)));
			String refused = world.getServer().computeOnServer(server -> {
				ServerPlayer player = player(server);
				ItemStack carried = player.containerMenu.getCarried();
				ItemStack inside = serverMenu(player) == null ? ItemStack.EMPTY : serverMenu(player).getSlot(8).getItem();
				return carried.isEmpty() || !inside.isEmpty() ? "clicking " + (carried.isEmpty() ? inside : carried) + " onto a backpack slot should be refused" : null;
			});
			check(refused == null, refused);
			click(context, pixel(context, inventorySlot(hotbar)));
			// Shift-click: stays put.
			shiftClick(context, inventorySlot(hotbar));
			// Number key over a backpack slot: stays put.
			hover(context, pixel(context, insideSlot(5)));
			context.getInput().pressKey(options -> options.keyHotbarSlots[hotbar]);
			context.waitTicks(4);
		}
		shiftClick(context, inventorySlot(4));
		String inside = world.getServer().computeOnServer(server -> describe(player(server).getInventory().getItem(0)));
		check(inside.equals("0:emeraldx5 1:stonex64"), "only the stone should have gone in, beside the emeralds (holds " + inside + ")");
		String hotbar = world.getServer().computeOnServer(server -> {
			var inventory = player(server).getInventory();
			return inventory.getItem(1).is(Items.SHULKER_BOX) && inventory.getItem(2).is(Items.BUNDLE) && inventory.getItem(3).is(WildercordItems.BACKPACK)
				? null : "the shulker box, bundle and backpack should all still be in their hotbar slots";
		});
		check(hotbar == null, hotbar);
		context.getInput().setCursorPos(4, 4);
		context.waitTicks(2);
		shot(context, "screen_reinforced_in_hand");
	}

	/**
	 * With a backpack open from the hotbar: a number key can't swap it out from another slot, nor from its own; F
	 * can't send it to the off-hand; Q and Ctrl+Q don't throw it; a click or a shift-click doesn't move it. Then,
	 * opened from the off-hand, F can't swap it back. Each time the menu stays open and the backpack where it is.
	 */
	private static void swapAndDropWhileOpen(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ready(player);
			player.getInventory().setItem(0, backpack(WildercordItems.BACKPACK, new ItemStack(Items.EMERALD, 5)));
			player.getInventory().setItem(2, new ItemStack(Items.STONE, 3));
			player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH, 4));
		});
		context.waitTicks(5);
		select(context, 0);
		context.getInput().pressMouse(RIGHT);
		waitForBackpack(context, "the backpack should open from the hotbar");
		ItemStack[] opened = new ItemStack[1];
		world.getServer().runOnServer(server -> opened[0] = player(server).getInventory().getItem(0));
		String[] tries = {"key 1 over an empty slot", "key 3 over the backpack", "F over the backpack", "Q over the backpack",
			"Ctrl+Q over the backpack", "a click on the backpack", "a shift-click on the backpack"};
		for (String attempt : tries) {
			double[] self = pixel(context, inventorySlot(0));
			double[] empty = pixel(context, inventorySlot(20));
			switch (attempt) {
				case "key 1 over an empty slot" -> {
					hover(context, empty);
					context.getInput().pressKey(options -> options.keyHotbarSlots[0]);
				}
				case "key 3 over the backpack" -> {
					hover(context, self);
					context.getInput().pressKey(options -> options.keyHotbarSlots[2]);
				}
				case "F over the backpack" -> {
					hover(context, self);
					context.getInput().pressKey(options -> options.keySwapOffhand);
				}
				case "Q over the backpack" -> {
					hover(context, self);
					context.getInput().pressKey(options -> options.keyDrop);
				}
				case "Ctrl+Q over the backpack" -> {
					hover(context, self);
					context.getInput().holdControl();
					context.getInput().pressKey(options -> options.keyDrop);
					context.getInput().releaseControl();
				}
				case "a click on the backpack" -> click(context, self);
				default -> shiftClick(context, inventorySlot(0));
			}
			context.waitTicks(5);
			String problem = world.getServer().computeOnServer(server -> {
				ServerPlayer player = player(server);
				if (player.getInventory().getItem(0) != opened[0]) {
					return attempt + " moved the open backpack (slot 0 holds " + player.getInventory().getItem(0) + ")";
				}
				if (serverMenu(player) == null) {
					return attempt + " closed the backpack";
				}
				if (!player.containerMenu.getCarried().isEmpty() || !drops(player).isEmpty()) {
					return attempt + " picked up or threw something (carrying " + player.containerMenu.getCarried() + ", " + drops(player).size() + " on the ground)";
				}
				if (!player.getOffhandItem().is(Items.TORCH) || owned(player, Items.EMERALD) != 5 || owned(player, Items.STONE) != 3) {
					return attempt + " changed something it shouldn't (off-hand " + player.getOffhandItem() + ")";
				}
				return null;
			});
			check(problem == null, problem);
			check(backpackOpen(context), attempt + " closed the screen");
		}
		shot(context, "screen_locked_slot");
		hover(context, pixel(context, inventorySlot(0)));
		context.waitTicks(3);
		shot(context, "screen_locked_slot_tooltip");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitTicks(4);

		// From the off-hand: F anywhere can't swap it out.
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ready(player);
			player.setItemInHand(InteractionHand.OFF_HAND, backpack(WildercordItems.BACKPACK, new ItemStack(Items.EMERALD, 5)));
			player.getInventory().setItem(0, ItemStack.EMPTY);
		});
		context.waitTicks(5);
		select(context, 0);
		context.getInput().pressMouse(RIGHT);
		waitForBackpack(context, "a backpack in the off-hand should open when used");
		int locked = context.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).getMenu().locked());
		check(locked == Inventory.SLOT_OFFHAND, "the off-hand's backpack should be the locked one (" + locked + ")");
		world.getServer().runOnServer(server -> opened[0] = player(server).getOffhandItem());
		for (int target : new int[]{0, 9, 20}) {
			hover(context, pixel(context, inventorySlot(target)));
			context.getInput().pressKey(options -> options.keySwapOffhand);
			context.waitTicks(4);
		}
		String offhand = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			return player.getOffhandItem() == opened[0] && serverMenu(player) != null && owned(player, Items.EMERALD) == 5 ? null
				: "F shouldn't swap an open backpack out of the off-hand (off-hand " + player.getOffhandItem() + ")";
		});
		check(offhand == null, offhand);
	}

	/**
	 * What a modified client could send while the menu is open, which the game's own client never does: throwing
	 * the held backpack, then taking the emeralds out of the still-open screen; swapping it to the off-hand, then
	 * the same. The backpack leaves with everything in it, the menu takes no more clicks and closes, and there
	 * are never more emeralds than there were.
	 */
	private static void modifiedClient(ClientGameTestContext context, TestSingleplayerContext world) {
		for (String how : new String[]{"thrown", "swapped"}) {
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				ready(player);
				player.getInventory().setItem(0, backpack(WildercordItems.BACKPACK, new ItemStack(Items.EMERALD, 5), new ItemStack(Items.DIAMOND, 2)));
			});
			context.waitTicks(5);
			select(context, 0);
			context.getInput().pressMouse(RIGHT);
			waitForBackpack(context, "the backpack should open (" + how + ")");
			String problem = world.getServer().computeOnServer(server -> {
				ServerPlayer player = player(server);
				AbstractContainerMenu menu = player.containerMenu;
				if (!(menu instanceof BackpackMenu)) {
					return "the server should have the backpack open";
				}
				ServerboundPlayerActionPacket.Action action = how.equals("thrown") ? ServerboundPlayerActionPacket.Action.DROP_ITEM
					: ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND;
				player.connection.handlePlayerAction(new ServerboundPlayerActionPacket(action, BlockPos.ZERO, Direction.DOWN));
				// The emeralds, grabbed out of the menu (as a click would) straight after.
				menu.clicked(0, 0, ContainerInput.PICKUP, player);
				menu.clicked(1, 0, ContainerInput.QUICK_MOVE, player);
				if (!menu.getCarried().isEmpty()) {
					return "once the backpack was " + how + ", the menu shouldn't hand out what's in it (carrying " + menu.getCarried() + ")";
				}
				return null;
			});
			check(problem == null, problem);
			context.waitTicks(5);
			String after = world.getServer().computeOnServer(server -> {
				ServerPlayer player = player(server);
				if (serverMenu(player) != null) {
					return "the menu should close once the backpack was " + how;
				}
				int emeralds = owned(player, Items.EMERALD) + dropped(player, Items.EMERALD);
				int diamonds = owned(player, Items.DIAMOND) + dropped(player, Items.DIAMOND);
				if (emeralds != 5 || diamonds != 2) {
					return "there should still be exactly 5 emeralds and 2 diamonds in the world once the backpack was " + how + " (" + emeralds + ", " + diamonds + ")";
				}
				ItemStack backpack = how.equals("thrown") ? drops(player).stream().map(ItemEntity::getItem).filter(Backpacks::isBackpack).findFirst().orElse(ItemStack.EMPTY)
					: player.getOffhandItem();
				return describe(backpack).equals("0:emeraldx5 1:diamondx2") ? null
					: "the " + how + " backpack should still hold the 5 emeralds and 2 diamonds (" + describe(backpack) + ")";
			});
			check(after == null, after);
			check(!backpackOpen(context), "the screen should close once the backpack was " + how);
		}
	}

	/** Dying with the worn backpack open, just after putting diamonds in: it drops once, with the diamonds in it, and the menu is gone. */
	private static void dyingWithItOpen(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ready(player);
			Backpacks.wear(player, backpack(WildercordItems.RUNEWOVEN_BACKPACK, new ItemStack(Items.EMERALD, 5)));
			player.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 12));
		});
		context.waitTicks(5);
		context.getInput().pressKey(WildercordKeys.backpackMapping());
		waitForBackpack(context, "the worn Runewoven Backpack should open");
		check(context.computeOnClient(mc -> ((BackpackScreen) mc.gui.screen()).getMenu().rows()) == 4, "the Runewoven Backpack should have 4 rows");
		click(context, pixel(context, inventorySlot(9)));
		click(context, pixel(context, insideSlot(20)));
		world.getServer().runOnServer(server -> player(server).kill(player(server).level()));
		context.waitTicks(5);
		String died = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			long backpacks = drops(player).stream().filter(e -> Backpacks.isBackpack(e.getItem())).count();
			if (backpacks != 1 || !Backpacks.worn(player).isEmpty()) {
				return "the worn backpack should drop once and leave the slot empty (" + backpacks + " dropped)";
			}
			ItemStack dropped = drops(player).stream().map(ItemEntity::getItem).filter(Backpacks::isBackpack).findFirst().orElseThrow();
			if (!describe(dropped).equals("0:emeraldx5 20:diamondx12")) {
				return "the dropped backpack should hold the emeralds and the diamonds just put in (" + describe(dropped) + ")";
			}
			int diamonds = owned(player, Items.DIAMOND) + dropped(player, Items.DIAMOND);
			return diamonds == 12 ? null : "there should be exactly 12 diamonds after the death (" + diamonds + ")";
		});
		check(died == null, died);
		context.runOnClient(mc -> {
			mc.player.respawn();
			mc.gui.setScreen(null);
		});
		context.waitTicks(10);
		String respawned = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			return serverMenu(player) == null && Backpacks.worn(player).isEmpty() ? null : "the respawned player should have no backpack worn or open";
		});
		check(respawned == null, respawned);
		world.getServer().runOnServer(server -> clearDrops(player(server)));
	}

	/** Dropped with Q in the world, a backpack lies there whole; picked up, it comes back whole. Burnt, it spills; the Runewoven one doesn't burn. */
	private static void droppedAndPickedUp(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ready(player);
			player.getInventory().setItem(0, backpack(WildercordItems.REINFORCED_BACKPACK, new ItemStack(Items.GOLD_INGOT, 9), new ItemStack(Items.APPLE, 3)));
		});
		context.waitTicks(5);
		select(context, 0);
		context.getInput().pressKey(options -> options.keyDrop);
		context.waitTicks(10);
		String lying = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			List<ItemEntity> backpacks = drops(player).stream().filter(e -> Backpacks.isBackpack(e.getItem())).toList();
			if (backpacks.size() != 1 || !player.getInventory().getItem(0).isEmpty()) {
				return "Q should throw the backpack (" + backpacks.size() + " on the ground)";
			}
			return describe(backpacks.getFirst().getItem()).equals("0:gold_ingotx9 1:applex3") ? null
				: "the thrown backpack should hold its gold and apples (" + describe(backpacks.getFirst().getItem()) + ")";
		});
		check(lying == null, lying);
		// Walk onto it once it can be picked up.
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			for (ItemEntity entity : drops(player)) {
				entity.setNoPickUpDelay();
				entity.teleportTo(player.getX(), player.getY(), player.getZ());
			}
		});
		context.waitTicks(20);
		String back = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
				if (stack.is(WildercordItems.REINFORCED_BACKPACK)) {
					return describe(stack).equals("0:gold_ingotx9 1:applex3") ? null : "picked up, the backpack should still hold its gold and apples (" + describe(stack) + ")";
				}
			}
			return "the backpack should be picked up again";
		});
		check(back == null, back);
		// Into fire: the leather one burns and spills what it held; the Runewoven one is untouched.
		String burnt = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			clearDrops(player);
			ItemEntity leather = new ItemEntity(level, player.getX() + 2, player.getY(), player.getZ(), backpack(WildercordItems.BACKPACK, new ItemStack(Items.IRON_INGOT, 4)));
			ItemEntity runewoven = new ItemEntity(level, player.getX() - 2, player.getY(), player.getZ(), backpack(WildercordItems.RUNEWOVEN_BACKPACK, new ItemStack(Items.IRON_INGOT, 6)));
			leather.setNeverPickUp();
			runewoven.setNeverPickUp();
			level.addFreshEntity(leather);
			level.addFreshEntity(runewoven);
			leather.hurtServer(level, level.damageSources().lava(), 100);
			runewoven.hurtServer(level, level.damageSources().lava(), 100);
			int loose = 0;
			for (ItemEntity entity : drops(player)) {
				if (entity.getItem().is(Items.IRON_INGOT)) {
					loose += entity.getItem().getCount();
				}
			}
			String result = leather.isAlive() || loose != 4 ? "a burnt Backpack should spill its 4 iron ingots (" + loose + " loose)"
				: !runewoven.isAlive() || count(runewoven.getItem(), Items.IRON_INGOT) != 6 ? "the Runewoven Backpack shouldn't burn" : null;
			clearDrops(player);
			return result;
		});
		check(burnt == null, burnt);
	}

	/**
	 * In the survival inventory: shift-click puts a backpack on and takes it off, the backpack key opens the worn one
	 * from there, and the slot, empty and full, and a backpack's tooltip, are filmed.
	 */
	private static void inventoryKeyAndSlot(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ready(player);
			fill(player);
		});
		context.waitTicks(5);
		context.runOnClient(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
		context.waitTicks(6);
		check(pixel(context, wornSlot()) != null, "the survival inventory should have a Backpack slot");
		context.getInput().setCursorPos(4, 4);
		context.waitTicks(2);
		shot(context, "inventory_slot_empty");
		hover(context, pixel(context, wornSlot()));
		context.waitTicks(3);
		shot(context, "inventory_slot_empty_hover");
		hover(context, pixel(context, inventorySlot(9)));
		context.waitTicks(3);
		shot(context, "inventory_backpack_tooltip");
		shiftClick(context, inventorySlot(9));
		String worn = world.getServer().computeOnServer(server -> describe(Backpacks.worn(player(server))));
		check(worn.startsWith("0:cobblestonex64"), "shift-clicking the backpack should put it on (worn holds " + worn + ")");
		check(world.getServer().computeOnServer(server -> player(server).getInventory().getItem(9).isEmpty()), "the backpack should leave its inventory slot");
		context.getInput().setCursorPos(4, 4);
		context.waitTicks(2);
		shot(context, "inventory_slot_filled");
		hover(context, pixel(context, wornSlot()));
		context.waitTicks(3);
		shot(context, "inventory_slot_filled_hover");
		// The backpack key, in the inventory.
		context.getInput().setCursorPos(4, 4);
		context.getInput().pressKey(WildercordKeys.backpackMapping());
		waitForBackpack(context, "the backpack key should open the worn backpack from the inventory");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitTicks(4);
		context.runOnClient(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
		context.waitTicks(6);
		shiftClick(context, wornSlot());
		check(world.getServer().computeOnServer(server -> Backpacks.worn(player(server)).isEmpty() && owned(player(server), Items.COBBLESTONE) == 64),
			"shift-clicking the Backpack slot should take it off, contents and all");
		// A second backpack can't push the worn one out by shift-click.
		world.getServer().runOnServer(server -> {
			Backpacks.wear(player(server), new ItemStack(WildercordItems.RUNEWOVEN_BACKPACK));
			player(server).getInventory().setItem(12, new ItemStack(WildercordItems.BACKPACK));
		});
		context.waitTicks(4);
		shiftClick(context, inventorySlot(12));
		check(world.getServer().computeOnServer(server -> Backpacks.worn(player(server)).is(WildercordItems.RUNEWOVEN_BACKPACK)),
			"shift-clicking a second backpack shouldn't replace the worn one");
		// The creative Survival Inventory tab has it too.
		context.runOnClient(mc -> mc.gui.setScreen(null));
		world.getServer().runOnServer(server -> player(server).setGameMode(GameType.CREATIVE));
		context.waitTicks(5);
		context.runOnClient(mc -> {
			try {
				java.lang.reflect.Field selected = CreativeModeInventoryScreen.class.getDeclaredField("selectedTab");
				selected.setAccessible(true);
				selected.set(null, BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(CreativeModeTabs.INVENTORY).value());
			} catch (ReflectiveOperationException e) {
				throw new AssertionError("couldn't pick the Survival Inventory tab: " + e);
			}
			mc.gui.setScreen(new InventoryScreen(mc.player));
		});
		context.waitTicks(10);
		check(pixel(context, wornSlot()) != null, "the creative Survival Inventory tab should have the Backpack slot");
		context.getInput().setCursorPos(4, 4);
		context.waitTicks(2);
		shot(context, "creative_slot");
		world.getServer().runOnServer(server -> player(server).setGameMode(GameType.SURVIVAL));
	}

	/** Something in every slot of the tray, and a backpack in the inventory. */
	private static void fill(ServerPlayer player) {
		GearSlots.set(player, GearSlot.STAFF, new ItemStack(GearItems.get(GearDef.staff("arcane"))));
		GearSlots.set(player, GearSlot.FOCUS, new ItemStack(GearItems.get(GearDef.THRIFT)));
		GearSlots.set(player, GearSlot.TOME, new ItemStack(GearItems.get(GearDef.TOME)));
		ItemStack pack = backpack(WildercordItems.BACKPACK, new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.TORCH, 32), new ItemStack(Items.BREAD, 12),
			new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.OAK_LOG, 40), new ItemStack(Items.COAL, 20), new ItemStack(Items.ARROW, 64));
		player.getInventory().setItem(9, pack);
	}

	// ------------------------------------------------------------------ looks

	/** Each backpack's screen, with things in it. */
	private static void screens(ClientGameTestContext context, TestSingleplayerContext world) {
		for (Item item : List.of(WildercordItems.BACKPACK, WildercordItems.REINFORCED_BACKPACK, WildercordItems.RUNEWOVEN_BACKPACK)) {
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				ready(player);
				List<ItemStack> contents = new ArrayList<>(List.of(new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.TORCH, 32), ItemStack.EMPTY,
					new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.OAK_LOG, 40), new ItemStack(Items.COAL, 20), ItemStack.EMPTY, new ItemStack(Items.BREAD, 12)));
				if (item != WildercordItems.BACKPACK) {
					contents.addAll(List.of(ItemStack.EMPTY, new ItemStack(Items.REDSTONE, 48), new ItemStack(Items.GOLD_INGOT, 9), ItemStack.EMPTY, ItemStack.EMPTY,
						new ItemStack(Items.DIAMOND, 5), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.ARROW, 64)));
				}
				Backpacks.wear(player, backpack(item, contents.toArray(ItemStack[]::new)));
				player.getInventory().setItem(0, new ItemStack(Items.DIAMOND_SWORD));
				player.getInventory().setItem(1, new ItemStack(Items.COOKED_BEEF, 16));
				player.getInventory().setItem(9, new ItemStack(Items.STONE, 64));
			});
			context.waitTicks(5);
			context.getInput().pressKey(WildercordKeys.backpackMapping());
			waitForBackpack(context, "the worn " + item + " should open");
			context.getInput().setCursorPos(4, 4);
			context.waitTicks(2);
			shot(context, "screen_" + BuiltInRegistries.ITEM.getKey(item).getPath());
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitTicks(4);
		}
	}

	/** A fixed camera looking at a wearer, with the HUD hidden. */
	private static void director(ClientGameTestContext context, TestSingleplayerContext world, Vec3 eye, Vec3 target) {
		int id = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			level.getEntitiesOfClass(Display.TextDisplay.class, player(server).getBoundingBox().inflate(160),
				e -> e.entityTags().contains("wildercord.camera")).forEach(Entity::discard);
			Display.TextDisplay camera = EntityTypes.TEXT_DISPLAY.create(level, EntitySpawnReason.COMMAND);
			Vec3 d = target.subtract(eye);
			float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
			float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
			camera.snapTo(eye.x, eye.y, eye.z, yaw, pitch);
			camera.addTag("wildercord.camera");
			level.addFreshEntity(camera);
			return camera.getId();
		});
		context.waitTicks(3);
		context.runOnClient(mc -> {
			Entity camera = mc.level.getEntity(id);
			if (camera != null) {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				mc.setCameraEntity(camera);
				if (!mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			}
		});
		context.waitTicks(3);
	}

	/** The wearer from the front and the back (and both sides), looking south from {@code at}. */
	private static void around(ClientGameTestContext context, TestSingleplayerContext world, Vec3 at, String name, boolean sides) {
		double distance = 2.3;
		Vec3 target = at.add(0, 1.05, 0);
		Vec3[] eyes = {at.add(0, 1.5, distance), at.add(0, 1.5, -distance), at.add(distance, 1.5, 0), at.add(-distance, 1.5, 0)};
		String[] names = {"front", "back", "left", "right"};
		for (int i = 0; i < (sides ? 4 : 2); i++) {
			director(context, world, eyes[i], target);
			shot(context, name + "_" + names[i]);
		}
	}

	/** A mannequin (drawn like a player, and seen from outside as other players are) wearing each backpack, then with a staff, a cape, armour and elytra. */
	private static void wearing(ClientGameTestContext context, TestSingleplayerContext world) {
		Vec3[] stage = new Vec3[1];
		int[] id = new int[1];
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			stage[0] = ready(player);
			Mannequin dummy = EntityTypes.MANNEQUIN.create(player.level(), EntitySpawnReason.COMMAND);
			dummy.snapTo(stage[0].x, stage[0].y, stage[0].z, 0, 0);
			dummy.addTag("wildercord.backpack");
			player.level().addFreshEntity(dummy);
			id[0] = dummy.getId();
			player.teleportTo(player.level(), stage[0].x - 40, stage[0].y, stage[0].z, Set.<Relative>of(), 0, 0, false);
		});
		context.waitTicks(10);
		Vec3 at = stage[0];
		java.util.function.Consumer<java.util.function.Consumer<Mannequin>> edit = change -> world.getServer().runOnServer(server -> {
			change.accept((Mannequin) player(server).level().getEntity(id[0]));
		});
		for (Item item : List.of(WildercordItems.BACKPACK, WildercordItems.REINFORCED_BACKPACK, WildercordItems.RUNEWOVEN_BACKPACK)) {
			edit.accept(dummy -> Backpacks.wear(dummy, backpack(item, new ItemStack(Items.DIAMOND))));
			context.waitTicks(4);
			around(context, world, at, "look_" + BuiltInRegistries.ITEM.getKey(item).getPath(), true);
		}
		// What another player sees: the look, and nothing of what's inside.
		String seen = context.computeOnClient(mc -> {
			Entity other = mc.level.getEntity(id[0]);
			if (other == null) {
				return "the client should have the mannequin";
			}
			ItemStack look = Backpacks.look(other);
			if (!look.is(WildercordItems.RUNEWOVEN_BACKPACK) || Backpacks.used(look) != 0 || !Backpacks.worn(other).isEmpty()) {
				return "others should see which backpack is worn but not what's in it (look " + look + ", holds " + Backpacks.used(look) + ")";
			}
			GearLook.Pack pack = mc.getEntityRenderDispatcher().getRenderer(other).createRenderState(other, 0).getData(GearLook.PACK);
			return pack != null && pack.tier() == BackpackTier.RUNEWOVEN ? null : "the renderer should carry the worn backpack into the render state";
		});
		check(seen == null, seen);
		// Dyed red, with a staff strapped over it.
		edit.accept(dummy -> {
			ItemStack red = backpack(WildercordItems.REINFORCED_BACKPACK);
			red.set(DataComponents.DYED_COLOR, new DyedItemColor(0xB02E26));
			Backpacks.wear(dummy, red);
			GearSlots.set(dummy, GearSlot.STAFF, new ItemStack(GearItems.get(GearDef.staff("fire"))));
		});
		context.waitTicks(4);
		around(context, world, at, "look_staff", true);
		// A cape under it.
		edit.accept(dummy -> {
			ResolvableProfile profile = ResolvableProfile.CODEC.parse(JsonOps.INSTANCE,
				JsonParser.parseString("{\"cape\": \"wildercord-gametest:entity/test_cape\"}")).getOrThrow();
			dummy.setComponent(DataComponents.PROFILE, profile);
			GearSlots.clear(dummy, GearSlot.STAFF);
		});
		context.waitTicks(10);
		around(context, world, at, "look_cape", true);
		edit.accept(dummy -> GearSlots.set(dummy, GearSlot.STAFF, new ItemStack(GearItems.get(GearDef.greaterStaff("void")))));
		context.waitTicks(4);
		around(context, world, at, "look_cape_staff", false);
		// Over a chestplate, with the staff.
		edit.accept(dummy -> {
			dummy.setComponent(DataComponents.PROFILE, Mannequin.DEFAULT_PROFILE);
			dummy.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
			dummy.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
			Backpacks.wear(dummy, backpack(WildercordItems.BACKPACK));
		});
		context.waitTicks(6);
		around(context, world, at, "look_armoured", true);
		// Over folded elytra.
		edit.accept(dummy -> {
			dummy.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
			dummy.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
			GearSlots.clear(dummy, GearSlot.STAFF);
			Backpacks.wear(dummy, backpack(WildercordItems.RUNEWOVEN_BACKPACK));
		});
		context.waitTicks(6);
		around(context, world, at, "look_elytra", true);
		// Invisible: nothing is drawn.
		edit.accept(dummy -> dummy.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 400, 0, false, false)));
		context.waitTicks(6);
		around(context, world, at, "look_invisible", false);
		world.getServer().runOnServer(server -> player(server).level().getEntitiesOfClass(Mannequin.class, player(server).getBoundingBox().inflate(64)
			.move(40, 0, 0)).forEach(Entity::discard));

		// The player, in their own third-person camera, with a backpack and a staff.
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.teleportTo(player.level(), at.x, at.y, at.z, Set.<Relative>of(), 0, 0, false);
			Backpacks.wear(player, backpack(WildercordItems.REINFORCED_BACKPACK));
			GearSlots.set(player, GearSlot.STAFF, new ItemStack(GearItems.get(GearDef.staff("storm"))));
		});
		context.waitTicks(10);
		context.runOnClient(mc -> {
			mc.setCameraEntity(mc.player);
			if (mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		});
		context.waitTicks(6);
		shot(context, "self_back");
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		context.waitTicks(4);
		shot(context, "self_front");
		world.getServer().runOnServer(server -> {
			Backpacks.takeOff(player(server));
			GearSlots.clear(player(server), GearSlot.STAFF);
		});
	}

	// ------------------------------------------------------------------ leaving with it open

	private static void leaveWithItOpen(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ready(player);
			Backpacks.wear(player, backpack(WildercordItems.BACKPACK, new ItemStack(Items.EMERALD, 5)));
			player.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 7));
		});
		context.waitTicks(5);
		context.getInput().pressKey(WildercordKeys.backpackMapping());
		waitForBackpack(context, "the worn backpack should open before leaving");
		click(context, pixel(context, inventorySlot(9)));
		click(context, pixel(context, insideSlot(4)));
		check(world.getServer().computeOnServer(server -> describe(Backpacks.worn(player(server)))).equals("0:emeraldx5 4:diamondx7"),
			"the diamonds should be in the worn backpack before leaving");
		// Left open: the world closes with the screen still up.
	}

	private static void afterLeaving(ClientGameTestContext context, TestSingleplayerContext world) {
		String problem = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			String worn = describe(Backpacks.worn(player));
			if (!worn.equals("0:emeraldx5 4:diamondx7")) {
				return "after leaving with it open, the worn backpack should hold the emeralds and the diamonds put in (" + worn + ")";
			}
			int diamonds = owned(player, Items.DIAMOND);
			return diamonds == 7 ? null : "there should be exactly 7 diamonds after leaving and coming back (" + diamonds + ")";
		});
		check(problem == null, problem);
		context.waitTicks(10);
		String look = context.computeOnClient(mc -> Backpacks.look(mc.player).is(WildercordItems.BACKPACK) && describe(Backpacks.worn(mc.player)).equals("0:emeraldx5 4:diamondx7")
			? null : "the client should see the worn backpack and what's in it after coming back");
		check(look == null, look);
	}
}
