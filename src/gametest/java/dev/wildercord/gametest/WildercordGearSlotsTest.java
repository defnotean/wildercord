package dev.wildercord.gametest;

import dev.wildercord.cast.SpellCaster;
import dev.wildercord.client.CordScreen;
import dev.wildercord.client.render.GearLook;
import dev.wildercord.content.CordTier;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.gear.Gear;
import dev.wildercord.gear.GearBonuses;
import dev.wildercord.gear.GearDef;
import dev.wildercord.gear.GearItems;
import dev.wildercord.gear.GearSlot;
import dev.wildercord.gear.GearSlots;
import dev.wildercord.gear.SpellSlots;
import dev.wildercord.menu.GearInventorySlot;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The casting-gear slots in the inventory, in a real world: the real menu takes gear by click, shift-click
 * and swap (and by the creative inventory's own packet), only the piece a slot is for fits it, one each;
 * a slotted staff changes a spell's cost and power with nothing in the hands, the tome in its slot opens the
 * fifth spell; the pieces drop on death (Curse of Vanishing destroys its own) and stay under keepInventory,
 * never twice, and survive a dimension change, a respawn and a save; the client (and other entities'
 * renderers) see them. The real mouse works the survival screen, in its tray above the panel, with the
 * recipe book open or shut. Then the wearer is filmed from the front, the back and both sides with each
 * piece, into {@code build/run/clientGameTest/screenshots/gear_slots_*}.
 *
 * <p>Runs in the full suite; skipped with {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} or
 * {@code WILDERCORD_SHOWCASE}. {@code WILDERCORD_GEARSLOTS_ONLY=looks} (or {@code checks}) runs half of it.</p>
 */
public class WildercordGearSlotsTest implements FabricClientGameTest {
	/** The Cord screen takes its own buttons (0 left); the container screens take Minecraft's (1 left). */
	private static final int CORD_LEFT = com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
	private static final int LEFT = com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		String only = System.getenv("WILDERCORD_GEARSLOTS_ONLY");
		boolean checks = only == null || !only.equals("looks");
		boolean looks = only == null || !only.equals("checks");
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
			List<Runnable> steps = new ArrayList<>();
			if (checks) {
				steps.add(() -> menuClicks(world));
				steps.add(() -> creativePacket(world));
				steps.add(() -> spellsThroughASlot(context, world));
				steps.add(() -> tomeSlot(context, world));
				steps.add(() -> realMouse(context, world));
				steps.add(() -> seenByTheClient(context, world));
				steps.add(() -> deaths(context, world));
				steps.add(() -> dimensionChange(context, world));
			}
			if (looks) {
				steps.add(() -> screens(context, world));
				steps.add(() -> wearing(context, world));
			}
			for (Runnable step : steps) {
				try {
					step.run();
				} catch (AssertionError e) {
					failures.add(e.getMessage());
				}
				resetScreen(context, world);
			}
			// Left in the slots for the save round trip below.
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				player.setGameMode(GameType.SURVIVAL);
				fill(player, savedStaff(server), gear(GearDef.THRIFT), gear(GearDef.TOME));
			});
			context.waitTicks(10);
			save = world.getWorldSave();
		}
		if (checks) {
			try (TestSingleplayerContext again = save.open()) {
				context.waitTicks(40);
				try {
					afterASave(context, again);
				} catch (AssertionError e) {
					failures.add(e.getMessage());
				}
			}
		}
		if (!failures.isEmpty()) {
			throw new AssertionError("The gear slots went wrong:\n  " + String.join("\n  ", failures));
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

	private static List<String> ids(RuneDef... runes) {
		return java.util.Arrays.stream(runes).map(RuneDef::id).toList();
	}

	private static ItemStack gear(GearDef def) {
		return new ItemStack(GearItems.get(def));
	}

	private static ItemStack savedStaff(MinecraftServer server) {
		ItemStack staff = gear(GearDef.staff("storm"));
		staff.set(DataComponents.CUSTOM_NAME, Component.literal("Old Faithful"));
		staff.enchant(server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING), 1);
		return staff;
	}

	private static void fill(ServerPlayer player, ItemStack staff, ItemStack focus, ItemStack tome) {
		GearSlots.set(player, GearSlot.STAFF, staff);
		GearSlots.set(player, GearSlot.FOCUS, focus);
		GearSlots.set(player, GearSlot.TOME, tome);
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

	/** A survival player with a Cord, every rune, empty hands, empty inventory and gear slots, and full mana, on cleared ground. */
	private static Vec3 ready(ServerPlayer player, ItemStack cord) {
		player.setGameMode(GameType.SURVIVAL);
		player.getInventory().clearContent();
		player.containerMenu.setCarried(ItemStack.EMPTY);
		player.inventoryMenu.setCarried(ItemStack.EMPTY);
		for (GearSlot slot : GearSlot.all()) {
			GearSlots.clear(player, slot);
		}
		for (EquipmentSlot equipment : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
			player.setItemSlot(equipment, ItemStack.EMPTY);
		}
		player.setShiftKeyDown(false);
		Spellbooks.setCord(player, cord);
		Spellbook book = Spellbooks.get(player).withStarterGiven();
		for (RuneDef rune : Runes.all()) {
			book = book.learn(rune.id());
		}
		for (int i = 0; i < SpellSlots.ALL; i++) {
			book = book.withSpell(i, List.of());
		}
		Spellbooks.set(player, book.withSelected(0));
		player.setAttached(WildercordAttachments.CIRCLES, 0);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
		ServerLevel level = player.level();
		BlockPos spot = player.blockPosition();
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spot.getX(), spot.getZ());
		Vec3 at = new Vec3(spot.getX() + 0.5, y, spot.getZ() + 0.5);
		// Clear air around and in front, whatever the world generated there.
		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -4; dz <= 4; dz++) {
				for (int dy = 0; dy <= 3; dy++) {
					level.setBlockAndUpdate(BlockPos.containing(at.x + dx, at.y + dy, at.z + dz), Blocks.AIR.defaultBlockState());
				}
			}
		}
		player.teleportTo(level, at.x, at.y, at.z, Set.<Relative>of(), 0, 0, false);
		Spellbooks.setMana(player, Mana.max(player));
		return at;
	}

	private static float castFresh(ServerPlayer player, int requested) {
		int spell = requested < 0 ? Spellbooks.get(player).selected() : requested;
		Spellbooks.setReadyAt(player, spell, 0);
		player.removeAttached(WildercordAttachments.RHYTHM);
		Spellbooks.setMana(player, Mana.max(player));
		float before = Spellbooks.mana(player);
		long now = player.level().getGameTime();
		SpellCaster.cast(player, requested);
		return Spellbooks.readyAt(player, spell) > now ? before - Spellbooks.mana(player) : -1;
	}

	private static int slotIndex(AbstractContainerMenu menu, GearSlot kind) {
		for (Slot slot : menu.slots) {
			if (slot instanceof GearInventorySlot gear && gear.kind() == kind) {
				return slot.index;
			}
		}
		throw new AssertionError("no " + kind + " slot in the menu (" + menu.slots.size() + " slots)");
	}

	private static long count(ServerPlayer player, Item item) {
		long n = player.getInventory().getNonEquipmentItems().stream().filter(s -> s.is(item)).count();
		return n + GearSlots.equipped(player).values().stream().filter(s -> s.is(item)).count();
	}

	private static List<ItemEntity> drops(ServerPlayer player) {
		return player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(12));
	}

	private static long dropped(ServerPlayer player, Item item) {
		return drops(player).stream().filter(e -> e.getItem().is(item)).count();
	}

	private static void clearDrops(ServerPlayer player) {
		drops(player).forEach(Entity::discard);
	}

	// ------------------------------------------------------------------ the real menu

	private static void menuClicks(TestSingleplayerContext world) {
		String problem = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ready(player, new ItemStack(WildercordItems.ECHO_CORD));
			AbstractContainerMenu menu = player.inventoryMenu;
			int staffAt = slotIndex(menu, GearSlot.STAFF);
			int focusAt = slotIndex(menu, GearSlot.FOCUS);
			int tomeAt = slotIndex(menu, GearSlot.TOME);
			if (!(staffAt > 46 && focusAt == staffAt + 1 && tomeAt == focusAt + 1)) {
				return "the gear slots should follow the Cord slot (46), in order: staff " + staffAt + ", focus " + focusAt + ", tome " + tomeAt;
			}
			for (GearSlot kind : GearSlot.all()) {
				if (menu.getSlot(slotIndex(menu, kind)).getMaxStackSize() != 1) {
					return "the " + kind.id() + " slot should hold one";
				}
			}
			var inventory = player.getInventory();
			inventory.setItem(9, gear(GearDef.staff("fire")));
			inventory.setItem(10, gear(GearDef.HASTE));
			inventory.setItem(11, gear(GearDef.TOME));
			inventory.setItem(12, new ItemStack(Items.STONE));
			inventory.setItem(13, gear(GearDef.staff("frost")));

			// Picking a staff up and putting it down in the staff slot.
			menu.clicked(9, 0, ContainerInput.PICKUP, player);
			if (!menu.getCarried().is(GearItems.get(GearDef.staff("fire")))) {
				return "a click should pick the Fire Staff up (carrying " + menu.getCarried() + ")";
			}
			menu.clicked(staffAt, 0, ContainerInput.PICKUP, player);
			if (!menu.getCarried().isEmpty() || !GearSlots.get(player, GearSlot.STAFF).is(GearItems.get(GearDef.staff("fire")))) {
				return "clicking the staff slot with a staff should slot it (carrying " + menu.getCarried() + ", slot holds " + GearSlots.get(player, GearSlot.STAFF) + ")";
			}
			if (!inventory.getItem(9).isEmpty() || !menu.getSlot(staffAt).getItem().is(GearItems.get(GearDef.staff("fire")))) {
				return "the staff should be in the slot and out of its inventory place";
			}

			// A focus won't go in the staff slot or the tome's; it goes in the focus slot.
			menu.clicked(10, 0, ContainerInput.PICKUP, player);
			menu.clicked(staffAt, 0, ContainerInput.PICKUP, player);
			menu.clicked(tomeAt, 0, ContainerInput.PICKUP, player);
			if (!menu.getCarried().is(GearItems.get(GearDef.HASTE)) || GearSlots.get(player, GearSlot.STAFF).isEmpty()
					|| !GearSlots.get(player, GearSlot.TOME).isEmpty()) {
				return "a focus shouldn't go in the staff or tome slot, or swap the staff out (carrying " + menu.getCarried() + ")";
			}
			menu.clicked(focusAt, 0, ContainerInput.PICKUP, player);
			if (!menu.getCarried().isEmpty() || !GearSlots.get(player, GearSlot.FOCUS).is(GearItems.get(GearDef.HASTE))) {
				return "a focus should go in the focus slot";
			}

			// Shift-click sends the tome to its slot.
			menu.clicked(11, 0, ContainerInput.QUICK_MOVE, player);
			if (!GearSlots.get(player, GearSlot.TOME).is(GearItems.get(GearDef.TOME)) || !inventory.getItem(11).isEmpty()) {
				return "shift-clicking the tome should slot it";
			}
			// A second staff can't push the first out by shift-click; and stone isn't gear.
			menu.clicked(13, 0, ContainerInput.QUICK_MOVE, player);
			menu.clicked(12, 0, ContainerInput.QUICK_MOVE, player);
			if (!GearSlots.get(player, GearSlot.STAFF).is(GearItems.get(GearDef.staff("fire")))) {
				return "shift-clicking a second staff shouldn't replace the slotted one";
			}
			if (count(player, GearItems.get(GearDef.staff("frost"))) != 1 || count(player, Items.STONE) != 1) {
				return "the frost staff and the stone should still be in the inventory";
			}
			if (GearSlots.equipped(player).size() != 3) {
				return "only the three pieces of gear should be in the slots (" + GearSlots.equipped(player) + ")";
			}

			// Shift-click out, then the frost staff takes the empty slot by shift-click.
			menu.clicked(staffAt, 0, ContainerInput.QUICK_MOVE, player);
			if (!GearSlots.get(player, GearSlot.STAFF).isEmpty() || count(player, GearItems.get(GearDef.staff("fire"))) != 1) {
				return "shift-clicking a slotted staff should send it back to the inventory";
			}
			int frostAt = -1;
			for (int i = 9; i < 45; i++) {
				if (menu.getSlot(i).getItem().is(GearItems.get(GearDef.staff("frost")))) {
					frostAt = i;
				}
			}
			menu.clicked(frostAt, 0, ContainerInput.QUICK_MOVE, player);
			if (!GearSlots.get(player, GearSlot.STAFF).is(GearItems.get(GearDef.staff("frost")))) {
				return "shift-clicking the frost staff should put it in the empty staff slot";
			}

			// A hotbar key swaps the staff in the slot for the one in the hotbar; a stone in the hotbar can't go in.
			inventory.setItem(0, gear(GearDef.staff("fire")));
			inventory.setItem(1, new ItemStack(Items.STONE));
			menu.clicked(staffAt, 1, ContainerInput.SWAP, player);
			if (!GearSlots.get(player, GearSlot.STAFF).is(GearItems.get(GearDef.staff("frost"))) || !inventory.getItem(1).is(Items.STONE)) {
				return "a stone shouldn't swap into the staff slot";
			}
			menu.clicked(staffAt, 0, ContainerInput.SWAP, player);
			if (!GearSlots.get(player, GearSlot.STAFF).is(GearItems.get(GearDef.staff("fire")))
					|| !inventory.getItem(0).is(GearItems.get(GearDef.staff("frost")))) {
				return "a hotbar key should swap the slotted staff with the one in the hotbar";
			}

			// Taking it out with a plain click, then into an empty hand slot's worth of cursor.
			menu.clicked(staffAt, 0, ContainerInput.PICKUP, player);
			if (!menu.getCarried().is(GearItems.get(GearDef.staff("fire"))) || !GearSlots.get(player, GearSlot.STAFF).isEmpty()) {
				return "a click on a slotted staff should pick it up";
			}
			menu.setCarried(ItemStack.EMPTY);

			// One each: a stack of several sets one.
			GearSlots.set(player, GearSlot.STAFF, new ItemStack(GearItems.get(GearDef.staff("fire")), 5));
			if (GearSlots.get(player, GearSlot.STAFF).getCount() != 1) {
				return "a slot should keep one piece";
			}
			if (GearSlots.set(player, GearSlot.FOCUS, gear(GearDef.staff("fire"))) || GearSlots.set(player, GearSlot.TOME, new ItemStack(Items.STONE))) {
				return "the API shouldn't slot a piece in a slot it doesn't fit";
			}
			if (GearSlots.slotFor(gear(GearDef.greaterStaff("void"))).orElse(null) != GearSlot.STAFF || GearSlots.slotFor(new ItemStack(Items.STICK)).isPresent()) {
				return "a greater staff fits the staff slot, and a stick nothing";
			}
			return null;
		});
		check(problem == null, problem);
	}

	/** Creative mode edits its slots on the client and sends them in a packet the server accepts only for slots it knows. */
	private static void creativePacket(TestSingleplayerContext world) {
		String problem = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ready(player, new ItemStack(WildercordItems.ECHO_CORD));
			player.setGameMode(GameType.CREATIVE);
			int focusAt = slotIndex(player.inventoryMenu, GearSlot.FOCUS);
			player.connection.handleSetCreativeModeSlot(new ServerboundSetCreativeModeSlotPacket(focusAt, gear(GearDef.ECHOES)));
			if (!GearSlots.get(player, GearSlot.FOCUS).is(GearItems.get(GearDef.ECHOES))) {
				return "the creative inventory's packet should fill the focus slot";
			}
			player.connection.handleSetCreativeModeSlot(new ServerboundSetCreativeModeSlotPacket(focusAt, gear(GearDef.staff("fire"))));
			if (!GearSlots.get(player, GearSlot.FOCUS).is(GearItems.get(GearDef.ECHOES))) {
				return "the creative packet shouldn't put a staff in the focus slot";
			}
			player.connection.handleSetCreativeModeSlot(new ServerboundSetCreativeModeSlotPacket(focusAt, ItemStack.EMPTY));
			if (!GearSlots.get(player, GearSlot.FOCUS).isEmpty()) {
				return "the creative packet should empty the slot";
			}
			player.setGameMode(GameType.SURVIVAL);
			player.connection.handleSetCreativeModeSlot(new ServerboundSetCreativeModeSlotPacket(focusAt, gear(GearDef.ECHOES)));
			if (!GearSlots.get(player, GearSlot.FOCUS).isEmpty()) {
				return "a survival player mustn't fill a slot with the creative packet";
			}
			return null;
		});
		check(problem == null, problem);
	}

	// ------------------------------------------------------------------ what a slotted piece does

	private static void spellsThroughASlot(ClientGameTestContext context, TestSingleplayerContext world) {
		float[] damage = new float[2];
		int[] costs = new int[2];
		float[] slotSpent = new float[1];
		int[] targetId = new int[1];
		String problem = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			Vec3 at = ready(player, new ItemStack(WildercordItems.ECHO_CORD));
			SpellCaster.edit(player, 0, ids(Runes.TOUCH, Runes.FIRE));
			SpellCaster.edit(player, 1, ids(Runes.TOUCH, Runes.FROST));
			Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
			if (husk == null) {
				return "couldn't make a husk to cast at";
			}
			husk.snapTo(at.x, at.y, at.z + 2.2, 180, 0);
			husk.setNoAi(true);
			husk.addTag("wildercord.rolled");
			level.addFreshEntity(husk);
			targetId[0] = husk.getId();

			SpellCompiler.Compiled fire = SpellCompiler.compile(SpellCaster.activeRunes(Spellbooks.get(player), 0, CordTier.ECHO));
			costs[0] = Heart.manaCost(player, fire);
			castFresh(player, 0);
			return null;
		});
		check(problem == null, problem);
		context.waitTicks(4);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Mob husk = (Mob) player.level().getEntity(targetId[0]);
			damage[0] = husk.getMaxHealth() - husk.getHealth();
			husk.setHealth(husk.getMaxHealth());
			husk.clearFire();

			// The staff in its slot, nothing in either hand.
			GearSlots.set(player, GearSlot.STAFF, gear(GearDef.staff("fire")));
			costs[1] = Heart.manaCost(player, SpellCompiler.compile(SpellCaster.activeRunes(Spellbooks.get(player), 0, CordTier.ECHO)));
			slotSpent[0] = castFresh(player, 0);
		});
		context.waitTicks(4);
		problem = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			Mob husk = (Mob) player.level().getEntity(targetId[0]);
			damage[1] = husk.getMaxHealth() - husk.getHealth();
			husk.discard();
			if (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) {
				return "the hands should be empty";
			}
			if (slotSpent[0] < 0 || damage[0] <= 0) {
				return "Touch · Fire should cast and hurt the husk (spent " + slotSpent[0] + ", plain damage " + damage[0] + ")";
			}
			double ratio = damage[1] / damage[0];
			if (ratio < 1.15 || ratio > 1.25) {
				return "a Fire Staff in its slot should make fire hit 20% harder with nothing held (" + damage[0] + " -> " + damage[1] + ")";
			}
			SpellCompiler.Compiled fire = SpellCompiler.compile(SpellCaster.activeRunes(Spellbooks.get(player), 0, CordTier.ECHO));
			if (costs[1] != Math.min((int) Math.ceil(fire.cost() * GearDef.STAFF_COST - 1e-9), costs[0] - 1) || Math.round(slotSpent[0]) != costs[1]) {
				return "a Fire Staff in its slot should take 10% off a fire spell's cost (" + costs[0] + " -> " + costs[1] + ", spent " + slotSpent[0] + ")";
			}
			SpellCompiler.Compiled frost = SpellCompiler.compile(SpellCaster.activeRunes(Spellbooks.get(player), 1, CordTier.ECHO));
			GearSlots.clear(player, GearSlot.STAFF);
			int frostPlain = Heart.manaCost(player, frost);
			GearSlots.set(player, GearSlot.STAFF, gear(GearDef.staff("fire")));
			if (Heart.manaCost(player, frost) != frostPlain) {
				return "a Fire Staff shouldn't discount a frost spell";
			}
			// A Frost Staff held takes no part while the Fire Staff holds the slot; with the slot empty it discounts as before.
			player.setItemInHand(InteractionHand.MAIN_HAND, gear(GearDef.staff("frost")));
			if (Heart.manaCost(player, frost) != frostPlain) {
				return "a held staff shouldn't count while the staff slot is filled";
			}
			GearSlots.clear(player, GearSlot.STAFF);
			if (Heart.manaCost(player, frost) >= frostPlain) {
				return "with the staff slot empty, a held Frost Staff should still discount a frost spell";
			}
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			// A focus in its slot: Deep Well's mana, Haste's charge speed.
			int mana = Mana.max(player);
			GearSlots.set(player, GearSlot.FOCUS, gear(GearDef.DEEP_WELL));
			if (Mana.max(player) != mana + GearDef.DEEP_WELL_MANA) {
				return "a Focus of the Deep Well in its slot should add " + GearDef.DEEP_WELL_MANA + " max mana (" + mana + " -> " + Mana.max(player) + ")";
			}
			GearSlots.set(player, GearSlot.FOCUS, gear(GearDef.HASTE));
			if (Math.abs(Gear.chargeSpeed(player) - GearDef.HASTE_SPEED) > 1e-9) {
				return "a Focus of Haste in its slot should speed charging up";
			}
			player.setItemInHand(InteractionHand.OFF_HAND, gear(GearDef.THRIFT));
			if (Math.abs(Gear.chargeSpeed(player) - GearDef.HASTE_SPEED) > 1e-9 || Gear.of(player).cost(Set.of()) != 1.0) {
				return "a held focus shouldn't count while the focus slot is filled";
			}
			return null;
		});
		check(problem == null, problem);
	}

	private static void tomeSlot(ClientGameTestContext context, TestSingleplayerContext world) {
		String problem = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ready(player, new ItemStack(WildercordItems.TWINE_CORD));
			List<String> heal = ids(Runes.SELF, Runes.HEAL);
			if (SpellCaster.edit(player, SpellSlots.TOME, heal) == null) {
				return "the fifth spell should be locked without the tome";
			}
			GearSlots.set(player, GearSlot.TOME, gear(GearDef.TOME));
			if (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty() || !Gear.tome(player)) {
				return "the tome in its slot should count with the hands empty";
			}
			if (SpellCaster.edit(player, SpellSlots.TOME, heal) != null || !Spellbooks.get(player).spells().get(SpellSlots.TOME).equals(heal)) {
				return "with the tome in its slot, the fifth spell should take runes";
			}
			SpellCaster.select(player, 1);
			if (Spellbooks.get(player).selected() != SpellSlots.TOME) {
				return "switching spell should go on to the tome's (selected " + (Spellbooks.get(player).selected() + 1) + ")";
			}
			if (castFresh(player, -1) < 0) {
				return "the tome's spell should cast while the tome is in its slot";
			}
			// The tome and a focus together.
			GearSlots.set(player, GearSlot.FOCUS, gear(GearDef.HASTE));
			GearBonuses both = Gear.of(player);
			if (!both.fifthSpell() || both.chargeSpeed() != GearDef.HASTE_SPEED) {
				return "the tome and a focus should apply together";
			}
			return null;
		});
		check(problem == null, problem);
		context.waitTicks(5);
		context.runOnClient(mc -> mc.gui.setScreen(new CordScreen()));
		context.waitTicks(5);
		check(context.computeOnClient(mc -> mc.gui.screen() instanceof CordScreen), "the Cord screen should open");
		check(context.computeOnClient(mc -> Gear.tome(mc.player)), "the client should see the tome in its slot");
		double[] row = context.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).rowPoint(SpellSlots.TOME));
		double scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		context.getInput().setCursorPos(row[0] * scale, row[1] * scale);
		context.waitTicks(1);
		context.getInput().pressMouse(CORD_LEFT);
		context.waitTicks(3);
		check(context.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).editingRow()) == SpellSlots.TOME,
			"the tome's row should show in the Cord screen while the tome is in its slot");
		context.runOnClient(mc -> mc.gui.setScreen(null));
		// Taken out of its slot, the tome closes its spell again.
		String closed = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			GearSlots.clear(player, GearSlot.TOME);
			return Gear.tome(player) || castFresh(player, SpellSlots.TOME) >= 0 ? "the tome's spell shouldn't cast with the tome out of its slot" : null;
		});
		check(closed == null, closed);
	}

	// ------------------------------------------------------------------ the mouse on the survival screen

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
	private static double[] pixel(ClientGameTestContext context, java.util.function.Predicate<Slot> which) {
		return pixel(context, "container slot", which);
	}

	private static double[] pixel(ClientGameTestContext context, String stage, java.util.function.Predicate<Slot> which) {
		return context.computeOnClient(mc -> {
			check(mc.player != null && mc.player.isAlive() && mc.gui.screen() instanceof AbstractContainerScreen<?>,
				stage + " requires a living player and an open container; screen="
					+ (mc.gui.screen() == null ? "null" : mc.gui.screen().getClass().getSimpleName())
					+ "; " + playerState(mc.player));
			AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) mc.gui.screen();
			for (Slot slot : screen.getMenu().slots) {
				if (which.test(slot)) {
					double scale = mc.getWindow().getGuiScale();
					return new double[]{(intField(screen, "leftPos") + slot.x + 8) * scale, (intField(screen, "topPos") + slot.y + 8) * scale};
				}
			}
			return null;
		});
	}

	private static java.util.function.Predicate<Slot> gearSlot(GearSlot kind) {
		return slot -> slot.container instanceof GearInventorySlot.GearContainer c && c.kind() == kind;
	}

	private static java.util.function.Predicate<Slot> inventorySlot(int index) {
		return slot -> slot.container == slotsInventory() && slot.getContainerSlot() == index;
	}

	private static net.minecraft.world.Container slotsInventory() {
		return net.minecraft.client.Minecraft.getInstance().player.getInventory();
	}

	private static void click(ClientGameTestContext context, double[] at, boolean shift) {
		check(at != null, "nothing to click there");
		context.getInput().setCursorPos(at[0], at[1]);
		context.waitTicks(1);
		if (shift) {
			context.getInput().holdShift();
			context.waitTicks(1);
		}
		context.getInput().pressMouse(LEFT);
		if (shift) {
			context.getInput().releaseShift();
		}
		context.waitTicks(4);
	}

	/** A shift-click: the screen's own call, as the game sends it (the test input can't hold Shift through a mouse press). */
	private static void shiftClick(ClientGameTestContext context, java.util.function.Predicate<Slot> which) {
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

	private static void realMouse(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ready(player, new ItemStack(WildercordItems.ECHO_CORD));
			clearDrops(player);
			var inventory = player.getInventory();
			inventory.setItem(9, gear(GearDef.staff("fire")));
			inventory.setItem(10, gear(GearDef.HASTE));
			inventory.setItem(11, gear(GearDef.TOME));
			inventory.setItem(12, gear(GearDef.staff("frost")));
		});
		context.waitTicks(5);
		context.runOnClient(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
		context.waitTicks(5);
		check(context.computeOnClient(mc -> mc.gui.screen() instanceof InventoryScreen), "the survival inventory should open");
		for (GearSlot kind : GearSlot.all()) {
			check(pixel(context, gearSlot(kind)) != null, "the survival screen should have a " + kind.id() + " slot");
		}
		boolean recipeBookOpen = false;
		for (int round = 0; round < 2; round++) {
			String where = recipeBookOpen ? " (recipe book open)" : "";
			// Pick a staff up and put it in the staff slot with the mouse.
			click(context, pixel(context, inventorySlot(9)), false);
			check(world.getServer().computeOnServer(server -> player(server).containerMenu.getCarried().is(GearItems.get(GearDef.staff("fire")))),
				"a click on the staff in the inventory should pick it up" + where);
			click(context, pixel(context, gearSlot(GearSlot.STAFF)), false);
			check(world.getServer().computeOnServer(server -> GearSlots.get(player(server), GearSlot.STAFF).is(GearItems.get(GearDef.staff("fire")))
					&& player(server).containerMenu.getCarried().isEmpty()), "a click on the staff slot with a staff should slot it" + where);
			// A focus carried over the staff slot is refused, and isn't thrown out of the window by the click.
			click(context, pixel(context, inventorySlot(10)), false);
			click(context, pixel(context, gearSlot(GearSlot.STAFF)), false);
			String refused = world.getServer().computeOnServer(server -> {
				ServerPlayer player = player(server);
				if (!player.containerMenu.getCarried().is(GearItems.get(GearDef.HASTE)) || !GearSlots.get(player, GearSlot.STAFF).is(GearItems.get(GearDef.staff("fire")))) {
					return "a focus over the staff slot should be refused and stay carried (carrying " + player.containerMenu.getCarried() + ")";
				}
				return drops(player).isEmpty() ? null : "clicking a gear slot on the tray mustn't throw the carried item out";
			});
			check(refused == null, refused + where);
			// The tray's own padding is inside the window too.
			double[] staffSlot = pixel(context, gearSlot(GearSlot.STAFF));
			double scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
			click(context, new double[]{staffSlot[0] - 10 * scale - 2 * scale, staffSlot[1] - 6 * scale}, false);
			String padding = world.getServer().computeOnServer(server -> {
				ServerPlayer player = player(server);
				return player.containerMenu.getCarried().is(GearItems.get(GearDef.HASTE)) && drops(player).isEmpty() ? null
					: "a click on the tray's padding shouldn't throw the carried focus out (carrying " + player.containerMenu.getCarried() + ")";
			});
			check(padding == null, padding + where);
			click(context, pixel(context, gearSlot(GearSlot.FOCUS)), false);
			check(world.getServer().computeOnServer(server -> GearSlots.get(player(server), GearSlot.FOCUS).is(GearItems.get(GearDef.HASTE))),
				"the focus should go in the focus slot" + where);
			// Shift-click the tome in, and the staff out.
			shiftClick(context, inventorySlot(11));
			check(world.getServer().computeOnServer(server -> GearSlots.get(player(server), GearSlot.TOME).is(GearItems.get(GearDef.TOME))),
				"a shift-click on the tome should slot it" + where);
			if (!recipeBookOpen) {
				context.getInput().setCursorPos(4, 4);
				context.waitTicks(3);
				shot(context, "inventory_filled");
				click(context, pixel(context, gearSlot(GearSlot.STAFF)), false);
				click(context, pixel(context, inventorySlot(9)), false);
				context.getInput().setCursorPos(pixel(context, gearSlot(GearSlot.FOCUS))[0], pixel(context, gearSlot(GearSlot.FOCUS))[1]);
				context.waitTicks(4);
				shot(context, "inventory_hover_item");
			}
			shiftClick(context, gearSlot(GearSlot.STAFF));
			check(world.getServer().computeOnServer(server -> GearSlots.get(player(server), GearSlot.STAFF).isEmpty()), "a shift-click on the staff slot should take it out" + where);
			// Put things back for the next round, then open the recipe book.
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				player.containerMenu.setCarried(ItemStack.EMPTY);
				for (GearSlot kind : GearSlot.all()) {
					GearSlots.clear(player, kind);
				}
				player.getInventory().clearContent();
				player.getInventory().setItem(9, gear(GearDef.staff("fire")));
				player.getInventory().setItem(10, gear(GearDef.HASTE));
				player.getInventory().setItem(11, gear(GearDef.TOME));
			});
			context.waitTicks(4);
			if (round == 0) {
				toggleRecipeBook(context);
				recipeBookOpen = true;
			}
		}
		// Slots follow the window as the recipe book moves it: the screen was drawn with the book open.
		shot(context, "inventory_recipe_book");
		toggleRecipeBook(context);
	}

	private static void toggleRecipeBook(ClientGameTestContext context) {
		double[] button = context.computeOnClient(mc -> {
			AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) mc.gui.screen();
			double scale = mc.getWindow().getGuiScale();
			return new double[]{(intField(screen, "leftPos") + 104 + 10) * scale, (mc.getWindow().getGuiScaledHeight() / 2 - 22 + 9) * scale};
		});
		click(context, button, false);
		context.getInput().setCursorPos(4, 4);
		context.waitTicks(4);
	}

	// ------------------------------------------------------------------ the client sees it

	private static void seenByTheClient(ClientGameTestContext context, TestSingleplayerContext world) {
		try {
			seenByTheClientChecks(context, world);
		} finally {
			world.getServer().runOnServer(server -> {
				player(server).setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				player(server).level().getEntitiesOfClass(Mannequin.class, player(server).getBoundingBox().inflate(64)).forEach(Entity::discard);
			});
			context.waitTicks(3);
		}
	}

	private static void seenByTheClientChecks(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ready(player, new ItemStack(WildercordItems.ECHO_CORD));
			fill(player, gear(GearDef.staff("frost")), gear(GearDef.ECHOES), gear(GearDef.TOME));
			// Another wearer, whom the player only sees: a mannequin with gear on.
			Mannequin other = EntityTypes.MANNEQUIN.create(player.level(), EntitySpawnReason.COMMAND);
			other.snapTo(player.getX() + 3, player.getY(), player.getZ(), 90, 0);
			other.addTag("wildercord.gearslots");
			player.level().addFreshEntity(other);
			GearSlots.set(other, GearSlot.STAFF, gear(GearDef.greaterStaff("void")));
		});
		context.waitTicks(10);
		String problem = context.computeOnClient(mc -> {
			Map<GearSlot, ItemStack> worn = GearSlots.equipped(mc.player);
			if (worn.size() != 3 || !worn.get(GearSlot.STAFF).is(GearItems.get(GearDef.staff("frost"))) || !worn.get(GearSlot.TOME).is(GearItems.get(GearDef.TOME))) {
				return "the client should see the player's three pieces (" + worn + ")";
			}
			Entity other = null;
			for (Entity entity : mc.level.entitiesForRendering()) {
				if (entity instanceof Mannequin mannequin) {
					other = mannequin;
				}
			}
			if (other == null) {
				return "the client should have the mannequin";
			}
			if (!GearSlots.get(other, GearSlot.STAFF).is(GearItems.get(GearDef.greaterStaff("void")))) {
				return "another wearer's gear should be synced to whoever sees them, not just to themselves (sees " + GearSlots.equipped(other) + ")";
			}
			// What the renderers draw: the state each extracts carries the worn pieces.
			var self = mc.getEntityRenderDispatcher().getRenderer(mc.player).createRenderState(mc.player, 0);
			var theirs = mc.getEntityRenderDispatcher().getRenderer(other).createRenderState(other, 0);
			List<GearLook.Piece> selfPieces = self.getData(GearLook.PIECES);
			List<GearLook.Piece> theirPieces = theirs.getData(GearLook.PIECES);
			if (selfPieces == null || selfPieces.size() != 3 || theirPieces == null || theirPieces.size() != 1) {
				return "the renderers should extract the worn pieces (self " + (selfPieces == null ? 0 : selfPieces.size()) + ", other "
					+ (theirPieces == null ? 0 : theirPieces.size()) + ")";
			}
			return null;
		});
		check(problem == null, problem);
		// Client and server work out the same gear.
		check(world.getServer().computeOnServer(server -> Gear.of(player(server)).pieces().size() == 3), "the server should count three pieces");
		check(context.computeOnClient(mc -> Gear.of(mc.player).equals(Gear.of(mc.player)) && Gear.of(mc.player).pieces().size() == 3), "the client should count three pieces");
		// A copy of the staff in a hand is the one drawn: the slotted one isn't drawn twice.
		world.getServer().runOnServer(server -> player(server).setItemInHand(InteractionHand.MAIN_HAND, gear(GearDef.staff("frost"))));
		context.waitTicks(5);
		int pieces = context.computeOnClient(mc -> {
			List<GearLook.Piece> list = mc.getEntityRenderDispatcher().getRenderer(mc.player).createRenderState(mc.player, 0).getData(GearLook.PIECES);
			return list == null ? 0 : list.size();
		});
		check(pieces == 2, "the staff shouldn't be drawn on the back while a copy is in hand (" + pieces + " pieces drawn)");
	}

	// ------------------------------------------------------------------ death, respawn and dimensions

	private static void deaths(ClientGameTestContext context, TestSingleplayerContext world) {
		for (boolean keep : new boolean[]{false, true}) {
			world.getServer().runCommand("gamerule keep_inventory " + keep);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				ready(player, new ItemStack(WildercordItems.ECHO_CORD));
				clearDrops(player);
				ItemStack focus = gear(GearDef.HASTE);
				focus.enchant(server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.VANISHING_CURSE), 1);
				fill(player, gear(GearDef.staff("fire")), focus, gear(GearDef.TOME));
				player.getInventory().setItem(0, new ItemStack(Items.STONE));
				player.kill(player.level());
			});
			context.waitTicks(5);
			String died = world.getServer().computeOnServer(server -> {
				ServerPlayer old = player(server);
				long staff = dropped(old, GearItems.get(GearDef.staff("fire")));
				long tome = dropped(old, GearItems.get(GearDef.TOME));
				long focus = dropped(old, GearItems.get(GearDef.HASTE));
				long stone = dropped(old, Items.STONE);
				if (keep) {
					return staff + tome + focus + stone != 0 || GearSlots.equipped(old).size() != 3
						? "with keepInventory nothing should drop, and the slots stay filled (dropped " + staff + tome + focus + stone + ")" : null;
				}
				if (staff != 1 || tome != 1 || stone != 1) {
					return "a death should drop each piece of gear once, like the inventory (staff " + staff + ", tome " + tome + ", stone " + stone + ")";
				}
				if (focus != 0) {
					return "a Curse of Vanishing piece should be destroyed rather than dropped";
				}
				return GearSlots.any(old) ? "the dead player's slots should be empty once the gear dropped" : null;
			});
			check(died == null, died);
			context.runOnClient(mc -> {
				mc.player.respawn();
				mc.gui.setScreen(null);
			});
			context.waitTicks(10);
			String alive = world.getServer().computeOnServer(server -> {
				ServerPlayer player = player(server);
				long staff = count(player, GearItems.get(GearDef.staff("fire"))) + dropped(player, GearItems.get(GearDef.staff("fire")));
				long tome = count(player, GearItems.get(GearDef.TOME)) + dropped(player, GearItems.get(GearDef.TOME));
				long focus = count(player, GearItems.get(GearDef.HASTE)) + dropped(player, GearItems.get(GearDef.HASTE));
				if (staff != 1 || tome != 1) {
					return "after respawning there should be exactly one Fire Staff and one Tome in the world for the player (staff " + staff + ", tome " + tome + ")";
				}
				if (keep) {
					if (GearSlots.equipped(player).size() != 3 || focus != 1) {
						return "under keepInventory the respawned player should have all three pieces, the vanishing one too (" + GearSlots.equipped(player) + ")";
					}
				} else if (GearSlots.any(player) || focus != 0) {
					return "the respawned player's slots should start empty after a death that dropped the gear (" + GearSlots.equipped(player) + ")";
				}
				return null;
			});
			check(alive == null, alive + (keep ? " [keepInventory]" : ""));
			int seen = context.computeOnClient(mc -> GearSlots.equipped(mc.player).size());
			check(seen == (keep ? 3 : 0), "the client should see " + (keep ? 3 : 0) + " pieces after respawning, not " + seen);
		}
		world.getServer().runCommand("gamerule keep_inventory false");
	}

	private static void dimensionChange(ClientGameTestContext context, TestSingleplayerContext world) {
		Vec3 home = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			Vec3 at = ready(player, new ItemStack(WildercordItems.ECHO_CORD));
			clearDrops(player);
			fill(player, gear(GearDef.greaterStaff("void")), gear(GearDef.THRIFT), gear(GearDef.TOME));
			// A dimension-sync test needs a real landing, regardless of the Nether terrain at y=100.
			ServerLevel nether = server.getLevel(Level.NETHER);
			check(nether != null, "the dimension fixture needs the Nether");
			for (int x = -1; x <= 1; x++) {
				for (int z = -1; z <= 1; z++) {
					nether.setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState());
					for (int y = 100; y <= 102; y++) {
						nether.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
					}
				}
			}
			return at;
		});
		context.waitTicks(5);
		checkGrounded(context, world, "dimension departure", home);
		world.getServer().runCommand("execute in minecraft:the_nether run tp @p 0 100 0");
		context.waitFor(mc -> mc.level != null && mc.level.dimension() == Level.NETHER, 1200);
		context.waitTicks(20);
		checkGrounded(context, world, "Nether arrival", new Vec3(0.5, 100, 0.5));
		String there = world.getServer().computeOnServer(server -> GearSlots.equipped(player(server)).size() == 3 && drops(player(server)).isEmpty() ? null
			: "the gear should stay in its slots through a dimension change (" + GearSlots.equipped(player(server)) + ")");
		check(there == null, there);
		check(context.computeOnClient(mc -> GearSlots.equipped(mc.player).size()) == 3, "the client should see the gear after a dimension change");
		// Return to the prepared ground, not y=100 above the flat world's y=-60 floor. Teleporting
		// a falling player down in the next ready() call clears velocity but preserves fall distance.
		world.getServer().runCommand("execute in minecraft:overworld run tp @p " + home.x + " " + home.y + " " + home.z);
		context.waitFor(mc -> mc.level != null && mc.level.dimension() == Level.OVERWORLD, 1200);
		context.waitTicks(20);
		checkGrounded(context, world, "Overworld return", home);
		check(context.computeOnClient(mc -> GearSlots.equipped(mc.player).size()) == 3, "the client should see the gear after coming back");
		check(world.getServer().computeOnServer(server -> GearSlots.equipped(player(server)).size() == 3), "the gear should be in its slots after coming back");
	}

	private static String playerState(net.minecraft.world.entity.player.Player player) {
		return player == null ? "player=null" : "dimension=" + player.level().dimension() + ", position=" + player.position()
			+ ", health=" + player.getHealth() + ", onGround=" + player.onGround() + ", fallDistance=" + player.fallDistance
			+ ", velocity=" + player.getDeltaMovement();
	}

	private static void checkGrounded(ClientGameTestContext context, TestSingleplayerContext world, String stage, Vec3 expected) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			BlockPos below = player.blockPosition().below();
			String state = playerState(player) + ", support=" + player.level().getBlockState(below);
			org.slf4j.LoggerFactory.getLogger("gear slots").info("{}: {}", stage, state);
			check(player.isAlive() && player.onGround() && player.fallDistance == 0
				&& player.level().getBlockState(below).isCollisionShapeFullBlock(player.level(), below)
				&& (expected == null || player.position().distanceToSqr(expected) < 0.01),
				stage + " requires a living player on the prepared ground; " + state + ", expected=" + expected);
		});
		context.runOnClient(mc -> check(mc.player != null && mc.player.isAlive(), stage + " client: " + playerState(mc.player)));
	}

	private static void afterASave(ClientGameTestContext context, TestSingleplayerContext world) {
		String saved = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			Map<GearSlot, ItemStack> worn = GearSlots.equipped(player);
			if (worn.size() != 3) {
				return "the three pieces should be in their slots after the world is saved and opened again (" + worn + ")";
			}
			ItemStack staff = worn.get(GearSlot.STAFF);
			ItemStack expected = savedStaff(server);
			if (!ItemStack.matches(staff, expected) || !"Old Faithful".equals(staff.getHoverName().getString())) {
				return "the staff should come back with its name and enchantment (" + staff + " with " + staff.getComponents() + ")";
			}
			if (!worn.get(GearSlot.FOCUS).is(GearItems.get(GearDef.THRIFT)) || !worn.get(GearSlot.TOME).is(GearItems.get(GearDef.TOME))) {
				return "the focus and tome should come back too (" + worn + ")";
			}
			return null;
		});
		check(saved == null, saved);
		context.waitTicks(10);
		check(context.computeOnClient(mc -> GearSlots.equipped(mc.player).size()) == 3, "the client should see the pieces after opening the saved world");
	}

	// ------------------------------------------------------------------ looks

	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(mc -> mc.gui.toastManager().clear());
		context.takeScreenshot(TestScreenshotOptions.of("gear_slots_" + name).disableCounterPrefix());
	}

	private static void screens(ClientGameTestContext context, TestSingleplayerContext world) {
		checkGrounded(context, world, "inventory screenshots before setup", null);
		world.getServer().runCommand("gamerule keep_inventory false");
		for (boolean full : new boolean[]{false, true}) {
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				ready(player, new ItemStack(WildercordItems.ECHO_CORD));
				if (full) {
					fill(player, gear(GearDef.staff("arcane")), gear(GearDef.THRIFT), gear(GearDef.TOME));
				}
				player.getInventory().setItem(9, gear(GearDef.greaterStaff("void")));
			});
			context.waitTicks(5);
			String tag = full ? "filled" : "empty";
			checkGrounded(context, world, "survival_" + tag + " setup", null);
			context.runOnClient(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
			context.waitTicks(8);
			context.getInput().setCursorPos(4, 4);
			context.waitTicks(3);
			shot(context, "survival_" + tag);
			// Hovering a slot: what an empty one is for, and the piece in a full one.
			double[] focus = pixel(context, "survival_" + tag + " focus hover", gearSlot(GearSlot.FOCUS));
			context.getInput().setCursorPos(focus[0], focus[1]);
			context.waitTicks(4);
			shot(context, "survival_" + tag + "_hover");
			double[] staff = pixel(context, "survival_" + tag + " staff hover", gearSlot(GearSlot.STAFF));
			context.getInput().setCursorPos(staff[0], staff[1]);
			context.waitTicks(4);
			shot(context, "survival_" + tag + "_hover_staff");
			// Carrying a piece over the tray.
			if (!full) {
				click(context, pixel(context, inventorySlot(9)), false);
				context.getInput().setCursorPos(staff[0], staff[1]);
				context.waitTicks(4);
				shot(context, "survival_carrying");
				click(context, staff, false);
			}
			context.runOnClient(mc -> mc.gui.setScreen(null));
			context.waitTicks(3);
			world.getServer().runOnServer(server -> player(server).containerMenu.setCarried(ItemStack.EMPTY));
			// The creative inventory's Survival Inventory tab.
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
			context.getInput().setCursorPos(4, 4);
			context.waitTicks(3);
			shot(context, "creative_" + tag);
			double[] creativeFocus = pixel(context, "creative_" + tag + " focus hover", gearSlot(GearSlot.FOCUS));
			context.getInput().setCursorPos(creativeFocus[0], creativeFocus[1]);
			context.waitTicks(4);
			shot(context, "creative_" + tag + "_hover");
			context.runOnClient(mc -> mc.gui.setScreen(null));
			context.waitTicks(3);
		}
	}

	/** A fixed camera looking at a wearer, with the HUD hidden (as the showcase films). */
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
				org.slf4j.LoggerFactory.getLogger("gear slots").info("camera {} yaw {} looking at player {} (client)", camera.position(), camera.getYRot(), mc.player.position());
				if (!mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			}
		});
		context.waitTicks(3);
	}

	/** The wearer from the front, the back and both sides, looking south from {@code at}. */
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

	/** A mannequin (drawn by the same renderer as a player, and seen from outside as other players are) wearing gear, filmed all round. */
	private static void wearing(ClientGameTestContext context, TestSingleplayerContext world) {
		Vec3[] stage = new Vec3[1];
		int[] id = new int[1];
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			stage[0] = ready(player, new ItemStack(WildercordItems.ECHO_CORD));
			Mannequin dummy = EntityTypes.MANNEQUIN.create(player.level(), EntitySpawnReason.COMMAND);
			dummy.snapTo(stage[0].x, stage[0].y, stage[0].z, 0, 0);
			dummy.addTag("wildercord.gearslots");
			player.level().addFreshEntity(dummy);
			id[0] = dummy.getId();
			// The player steps well away, out of every shot.
			player.teleportTo(player.level(), stage[0].x - 40, stage[0].y, stage[0].z, Set.<Relative>of(), 0, 0, false);
		});
		context.waitTicks(10);
		Vec3 at = stage[0];
		java.util.function.Consumer<java.util.function.Consumer<Mannequin>> edit = change -> world.getServer().runOnServer(server -> {
			Entity entity = player(server).level().getEntity(id[0]);
			change.accept((Mannequin) entity);
		});
		for (Object[] piece : new Object[][]{
				{"staff", GearSlot.STAFF, gear(GearDef.staff("fire"))},
				{"greater_staff", GearSlot.STAFF, gear(GearDef.greaterStaff("void"))},
				{"focus", GearSlot.FOCUS, gear(GearDef.HASTE)},
				{"focus_well", GearSlot.FOCUS, gear(GearDef.DEEP_WELL)},
				{"tome", GearSlot.TOME, gear(GearDef.TOME)}}) {
			edit.accept(dummy -> {
				for (GearSlot slot : GearSlot.all()) {
					GearSlots.clear(dummy, slot);
				}
				GearSlots.set(dummy, (GearSlot) piece[1], (ItemStack) piece[2]);
			});
			context.waitTicks(4);
			around(context, world, at, "look_" + piece[0], true);
		}
		// All three at once, in armour, with elytra, and with the staff also held.
		edit.accept(dummy -> fill2(dummy, gear(GearDef.staff("storm")), gear(GearDef.THRIFT), gear(GearDef.TOME)));
		context.waitTicks(4);
		around(context, world, at, "look_all", true);
		edit.accept(dummy -> {
			dummy.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
			dummy.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
			dummy.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
			dummy.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
		});
		context.waitTicks(6);
		around(context, world, at, "look_armoured", true);
		edit.accept(dummy -> dummy.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA)));
		context.waitTicks(6);
		around(context, world, at, "look_elytra", true);
		edit.accept(dummy -> {
			for (EquipmentSlot equipment : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
				dummy.setItemSlot(equipment, ItemStack.EMPTY);
			}
			dummy.setItemSlot(EquipmentSlot.MAINHAND, gear(GearDef.staff("storm")));
		});
		context.waitTicks(6);
		around(context, world, at, "look_staff_in_hand", false);
		edit.accept(dummy -> dummy.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY));
		context.waitTicks(3);
		// A closer look at each end: the shoulder and the hip.
		director(context, world, at.add(-0.9, 1.9, 1.2), at.add(-0.2, 1.5, 0));
		shot(context, "look_close_shoulder");
		director(context, world, at.add(0.9, 1.0, 1.4), at.add(0.2, 0.9, 0));
		shot(context, "look_close_hip");
		world.getServer().runOnServer(server -> player(server).level().getEntitiesOfClass(Mannequin.class, player(server).getBoundingBox().inflate(64)
			.move(40, 0, 0)).forEach(Entity::discard));

		// The player themselves, in their own third-person camera: the gear on you, sneaking, and the paper doll's view is in screens().
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.teleportTo(player.level(), at.x, at.y, at.z, Set.<Relative>of(), 0, 0, false);
			fill(player, gear(GearDef.staff("storm")), gear(GearDef.THRIFT), gear(GearDef.TOME));
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
		world.getServer().runOnServer(server -> player(server).setShiftKeyDown(true));
		context.waitTicks(8);
		shot(context, "self_sneaking_front");
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		context.waitTicks(4);
		shot(context, "self_sneaking_back");
		world.getServer().runOnServer(server -> {
			player(server).setShiftKeyDown(false);
			for (GearSlot slot : GearSlot.all()) {
				GearSlots.clear(player(server), slot);
			}
		});
		context.waitTicks(3);
	}

	private static void fill2(Entity wearer, ItemStack staff, ItemStack focus, ItemStack tome) {
		GearSlots.set(wearer, GearSlot.STAFF, staff);
		GearSlots.set(wearer, GearSlot.FOCUS, focus);
		GearSlots.set(wearer, GearSlot.TOME, tome);
	}
}
