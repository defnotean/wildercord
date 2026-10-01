package dev.wildercord.gametest;

import dev.wildercord.cast.Cast;
import dev.wildercord.cast.CastEngine;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.cast.events.WorldEvents;
import dev.wildercord.client.CordScreen;
import dev.wildercord.config.Config;
import dev.wildercord.config.WildercordConfig;
import dev.wildercord.content.CordTier;
import dev.wildercord.content.Imbued;
import dev.wildercord.content.WildercordComponents;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.gear.Gear;
import dev.wildercord.gear.GearDef;
import dev.wildercord.gear.GearItems;
import dev.wildercord.gear.SpellSlots;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Casting gear and the server config, in a real world: a Fire Staff makes a fire spell hit harder and
 * cost less; the Tome of the Fifth Page opens a fifth spell that can be threaded, selected and cast
 * (and gets a row in the Cord screen and a place on the spell wheel); a Cord crafted from an enchanted, named one keeps its
 * enchantments and name; an imbued crossbow's triple shot spends one charge and sends the spell with
 * one arrow; and config changes (spells may not edit blocks; world-changing magic and
 * world events switched off) take effect after {@code /wildercord reload}, and are undone by the next.
 *
 * <p>Runs in the full suite; skipped with {@code WILDERCORD_TOUR_ONLY} or {@code WILDERCORD_CORDS_ONLY}.</p>
 */
public class WildercordGearTest implements FabricClientGameTest {
	private static final int LEFT = com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("time set 18000");
			world.getServer().runCommand("gamerule spawn_mobs false");
			List<String> failures = new ArrayList<>();
			for (Runnable check : List.<Runnable>of(
					() -> fireStaff(context, world),
					() -> tome(context, world),
					() -> cordUpgrade(world),
					() -> imbuedTripleShot(context, world),
					() -> configReload(context, world))) {
				try {
					check.run();
				} catch (AssertionError e) {
					failures.add(e.getMessage());
				}
				context.runOnClient(mc -> mc.gui.setScreen(null));
				context.waitTicks(5);
			}
			if (!failures.isEmpty()) {
				throw new AssertionError("Casting gear or the config went wrong:\n  " + String.join("\n  ", failures));
			}
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

	/** A player in survival with a Cord, every rune, empty hands and full mana, standing on the ground looking south. */
	private static Vec3 ready(ServerPlayer player, ItemStack cord) {
		player.setGameMode(GameType.SURVIVAL);
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
		// Clear air in front, whatever the world generated there.
		for (int dz = 1; dz <= 4; dz++) {
			for (int dy = 0; dy <= 2; dy++) {
				level.setBlockAndUpdate(BlockPos.containing(at.x, at.y + dy, at.z + dz), Blocks.AIR.defaultBlockState());
			}
		}
		player.teleportTo(level, at.x, at.y, at.z, Set.<Relative>of(), 0, 0, false);
		Spellbooks.setMana(player, Mana.max(player));
		return at;
	}

	/** Casts a spell (-1: the selected one) fresh: off cooldown, full mana, no rhythm chain; returns the mana it took (-1 if it didn't go off). */
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

	// ------------------------------------------------------------------ a Fire Staff

	private static void fireStaff(ClientGameTestContext context, TestSingleplayerContext world) {
		float[] spent = new float[2];
		float[] damage = new float[2];
		int[] costs = new int[2];
		int[] targetId = new int[1];
		String result = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			Vec3 at = ready(player, new ItemStack(WildercordItems.ECHO_CORD));
			SpellCaster.edit(player, 0, ids(Runes.TOUCH, Runes.FIRE));
			Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
			if (husk == null) {
				return "couldn't make a husk to cast at";
			}
			husk.snapTo(at.x, at.y, at.z + 2.2, 180, 0);
			husk.setNoAi(true);
			// Never a random Runebound (it would have more health and cast back): tests pick their monsters.
			husk.addTag("wildercord.rolled");
			level.addFreshEntity(husk);
			targetId[0] = husk.getId();

			SpellCompiler.Compiled compiled = SpellCompiler.compile(SpellCaster.activeRunes(Spellbooks.get(player), 0, CordTier.ECHO));
			costs[0] = Heart.manaCost(player, compiled);
			spent[0] = castFresh(player, 0);
			return null;
		});
		check(result == null, result);
		context.waitTicks(4);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Mob husk = (Mob) player.level().getEntity(targetId[0]);
			damage[0] = husk.getMaxHealth() - husk.getHealth();
			husk.setHealth(husk.getMaxHealth());
			husk.clearFire();
			player.setItemInHand(InteractionHand.MAIN_HAND, gear(GearDef.staff("fire")));
			costs[1] = Heart.manaCost(player, SpellCompiler.compile(SpellCaster.activeRunes(Spellbooks.get(player), 0, CordTier.ECHO)));
			spent[1] = castFresh(player, 0);
		});
		context.waitTicks(4);
		result = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			Mob husk = (Mob) player.level().getEntity(targetId[0]);
			damage[1] = husk.getMaxHealth() - husk.getHealth();
			husk.discard();
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

			if (spent[0] < 0 || spent[1] < 0) {
				return "Touch · Fire should cast, with and without the staff (spent " + spent[0] + " and " + spent[1] + ")";
			}
			if (damage[0] <= 0) {
				return "Touch · Fire should hurt the husk in front (it took " + damage[0] + ")";
			}
			double ratio = damage[1] / damage[0];
			if (ratio < 1.15 || ratio > 1.25) {
				return "a Fire Staff should make fire hit 20% harder (" + damage[0] + " -> " + damage[1] + ")";
			}
			// 10% off, rounded up, and always at least one mana saved.
			SpellCompiler.Compiled compiled = SpellCompiler.compile(SpellCaster.activeRunes(Spellbooks.get(player), 0, CordTier.ECHO));
			if (costs[1] != Math.min((int) Math.ceil(compiled.cost() * GearDef.STAFF_COST - 1e-9), costs[0] - 1) || costs[1] >= costs[0]) {
				return "a Fire Staff should take 10% off a fire spell's cost (" + costs[0] + " -> " + costs[1] + ")";
			}
			if (Math.round(spent[0]) != costs[0] || Math.round(spent[1]) != costs[1]) {
				return "the mana spent should be the cost shown (" + spent[0] + "/" + costs[0] + ", " + spent[1] + "/" + costs[1] + ")";
			}
			// A frost spell gets nothing from it.
			SpellCaster.edit(player, 1, ids(Runes.TOUCH, Runes.FROST));
			SpellCompiler.Compiled frost = SpellCompiler.compile(SpellCaster.activeRunes(Spellbooks.get(player), 1, CordTier.ECHO));
			int frostPlain = Heart.manaCost(player, frost);
			player.setItemInHand(InteractionHand.OFF_HAND, gear(GearDef.staff("fire")));
			int frostStaff = Heart.manaCost(player, frost);
			player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
			return frostPlain == frostStaff ? null : "a Fire Staff shouldn't discount a frost spell (" + frostPlain + " -> " + frostStaff + ")";
		});
		check(result == null, result);
	}

	// ------------------------------------------------------------------ the Tome of the Fifth Page

	private static void tome(ClientGameTestContext context, TestSingleplayerContext world) {
		String result = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ready(player, new ItemStack(WildercordItems.TWINE_CORD));
			List<String> heal = ids(Runes.SELF, Runes.HEAL);
			// Without the tome: the fifth spell is locked, can't be threaded or cast.
			if (SpellCaster.edit(player, SpellSlots.TOME, heal) == null || !Spellbooks.get(player).spells().get(SpellSlots.TOME).isEmpty()) {
				return "the fifth spell should be locked without the tome";
			}
			Spellbooks.set(player, Spellbooks.get(player).withSpell(SpellSlots.TOME, heal));
			if (castFresh(player, SpellSlots.TOME) >= 0) {
				return "the tome's spell shouldn't cast without the tome in the off-hand";
			}
			// A tome in the main hand doesn't count either.
			player.setItemInHand(InteractionHand.MAIN_HAND, gear(GearDef.TOME));
			if (Gear.tome(player) || castFresh(player, SpellSlots.TOME) >= 0) {
				return "the tome should only open the fifth spell from the off-hand";
			}
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			player.setItemInHand(InteractionHand.OFF_HAND, gear(GearDef.TOME));
			Spellbooks.set(player, Spellbooks.get(player).withSpell(SpellSlots.TOME, List.of()));
			if (SpellCaster.edit(player, SpellSlots.TOME, heal) != null || !Spellbooks.get(player).spells().get(SpellSlots.TOME).equals(heal)) {
				return "with the tome in the off-hand, the fifth spell should take runes";
			}
			// Switching on from the Twine Cord's only spell goes to the tome's.
			SpellCaster.select(player, 1);
			if (Spellbooks.get(player).selected() != SpellSlots.TOME) {
				return "switching spell should go on to the tome's (selected " + (Spellbooks.get(player).selected() + 1) + ")";
			}
			if (castFresh(player, -1) < 0) {
				return "the tome's spell should cast while the tome is held";
			}
			return null;
		});
		check(result == null, result);

		// The Cord screen shows the tome's row, and it can be picked.
		context.waitTicks(5);
		context.runOnClient(mc -> mc.gui.setScreen(new CordScreen()));
		context.waitTicks(5);
		CordScreen screen = context.computeOnClient(mc -> mc.gui.screen() instanceof CordScreen s ? s : null);
		check(screen != null, "the Cord screen should open");
		check(context.computeOnClient(mc -> Gear.tome(mc.player)), "the client should see the tome in the off-hand");
		clickRow(context, 0);
		check(context.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).editingRow()) == 0, "spell 1 should be selectable");
		clickRow(context, SpellSlots.TOME);
		check(context.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).editingRow()) == SpellSlots.TOME,
			"the tome's row should show in the Cord screen, and be selectable");
		check(context.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).rowRunes(SpellSlots.TOME)).equals(ids(Runes.SELF, Runes.HEAL)),
			"the tome's row should hold the tome's spell");
		context.runOnClient(mc -> mc.gui.setScreen(null));

		// The spell wheel holds the tome's spell too, even on a one-spell Cord: 5 picks it.
		world.getServer().runOnServer(server -> Spellbooks.set(player(server), Spellbooks.get(player(server)).withSelected(0)));
		context.waitTicks(3);
		context.getInput().holdKey(dev.wildercord.client.WildercordKeys.nextMapping());
		context.waitTicks(8);
		check(context.computeOnClient(mc -> mc.gui.screen() instanceof dev.wildercord.client.SpellWheelScreen),
			"with the tome held, holding the switch key should open the spell wheel on a Twine Cord");
		context.getInput().releaseKey(dev.wildercord.client.WildercordKeys.nextMapping());
		context.waitTicks(4);
		context.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_5);
		context.waitTicks(4);
		check(context.computeOnClient(mc -> mc.gui.screen() == null), "a number key should choose and close the wheel");
		check(world.getServer().computeOnServer(server -> Spellbooks.get(player(server)).selected() == SpellSlots.TOME),
			"pressing 5 on the wheel should select the tome's spell");

		// Put away, the tome closes its spell again.
		String after = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
			if (castFresh(player, SpellSlots.TOME) >= 0) {
				return "the tome's spell shouldn't cast once the tome is put away";
			}
			// With the tome's spell still selected, holding the cast key charges the next open spell, as a tap casts it.
			Spellbooks.set(player, Spellbooks.get(player).withSpell(0, ids(Runes.SELF, Runes.HEAL)));
			Spellbooks.setReadyAt(player, 0, 0);
			dev.wildercord.cast.Charging.request(player, -1, true);
			WildercordAttachments.Charge charge = player.getAttached(WildercordAttachments.CHARGE);
			dev.wildercord.cast.Charging.request(player, -1, false);
			return charge == null || charge.spell() != 0 ? "holding the cast key with the tome put away should charge spell 1 (charging " + charge + ")" : null;
		});
		check(after == null, after);
	}

	private static void clickRow(ClientGameTestContext context, int row) {
		double[] gui = context.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).rowPoint(row));
		double scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		context.getInput().setCursorPos(gui[0] * scale, gui[1] * scale);
		context.waitTicks(1);
		context.getInput().pressMouse(LEFT);
		context.waitTicks(3);
	}

	// ------------------------------------------------------------------ a Cord upgraded, and an imbued crossbow

	/** Crafting the next Cord from an enchanted, named one: the new Cord keeps its enchantments and its name. */
	private static void cordUpgrade(TestSingleplayerContext world) {
		String problem = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			var enchantments = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
			ItemStack twine = new ItemStack(WildercordItems.TWINE_CORD);
			twine.enchant(enchantments.getOrThrow(Mana.RESERVOIR), 2);
			twine.set(DataComponents.CUSTOM_NAME, Component.literal("Old Faithful"));
			List<ItemStack> grid = new ArrayList<>(List.of(new ItemStack(Items.COPPER_INGOT), twine, new ItemStack(Items.COPPER_INGOT),
				new ItemStack(Items.AMETHYST_SHARD), new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.COPPER_INGOT)));
			while (grid.size() < 9) {
				grid.add(ItemStack.EMPTY);
			}
			CraftingInput input = CraftingInput.of(3, 3, grid);
			var recipe = server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
			if (recipe.isEmpty()) {
				return "a Twine Cord, 4 Copper Ingots and an Amethyst Shard should make something";
			}
			ItemStack made = recipe.get().value().assemble(input);
			if (!made.is(WildercordItems.COPPER_CORD)) {
				return "they should make a Copper Cord (made " + made + ")";
			}
			if (EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Mana.RESERVOIR), made) != 2) {
				return "the Copper Cord should keep the Twine Cord's Reservoir II";
			}
			if (!"Old Faithful".equals(made.getHoverName().getString())) {
				return "the Copper Cord should keep the Twine Cord's name (it's called " + made.getHoverName().getString() + ")";
			}
			return null;
		});
		check(problem == null, problem);
	}

	/**
	 * An imbued crossbow's triple shot (three arrows leaving it in the same tick, as Multishot fires
	 * them): one charge spent, and only one of the arrows carries the spell.
	 */
	private static void imbuedTripleShot(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ready(player, new ItemStack(WildercordItems.ECHO_CORD));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CROSSBOW));
			SpellCaster.edit(player, 0, ids(Runes.SELF, Runes.IMBUE, Runes.FIRE));
			castFresh(player, 0);
		});
		context.waitTicks(4);
		String problem = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			ItemStack crossbow = player.getMainHandItem();
			Imbued imbued = crossbow.get(WildercordComponents.IMBUED);
			if (imbued == null) {
				return "Self Imbue Fire should imbue the crossbow in hand";
			}
			List<Arrow> arrows = new ArrayList<>();
			for (int i = -1; i <= 1; i++) {
				Arrow arrow = new Arrow(level, player, new ItemStack(Items.ARROW), crossbow);
				arrow.shootFromRotation(player, player.getXRot(), player.getYRot() + i * 10, 0.0F, 3.0F, 0.0F);
				// Never a random Runebound (it would have more health and cast back): tests pick their monsters.
				arrow.addTag("wildercord.rolled");
				level.addFreshEntity(arrow);
				arrows.add(arrow);
			}
			long carrying = arrows.stream().filter(a -> a.hasAttached(WildercordAttachments.IMBUED_SHOT)).count();
			arrows.forEach(Arrow::discard);
			Imbued after = player.getMainHandItem().get(WildercordComponents.IMBUED);
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			if (carrying != 1) {
				return "one arrow of a triple shot should carry the imbued spell (" + carrying + " did)";
			}
			if (after == null || after.charges() != imbued.charges() - 1) {
				return "a triple shot should spend one charge (" + imbued.charges() + " -> " + (after == null ? 0 : after.charges()) + ")";
			}
			return null;
		});
		check(problem == null, problem);
	}

	// ------------------------------------------------------------------ the config, reloaded

	private static void configReload(ClientGameTestContext context, TestSingleplayerContext world) {
		Path path = Config.path();
		String original;
		try {
			original = Files.exists(path) ? Files.readString(path, StandardCharsets.UTF_8) : null;
		} catch (IOException e) {
			throw new AssertionError("couldn't read " + path + ": " + e);
		}
		try {
			String noEdits = WildercordConfig.DEFAULTS.toJson().replace("\"spells_edit_blocks\": true", "\"spells_edit_blocks\": false");
			check(noEdits.contains("\"spells_edit_blocks\": false"), "the default file should list spells_edit_blocks");
			write(path, noEdits);
			world.getServer().runCommand("wildercord reload");
			context.waitTicks(2);
			String blocked = world.getServer().computeOnServer(server -> {
				if (Config.get().spellsEditBlocks()) {
					return "/wildercord reload should read spells_edit_blocks: false";
				}
				return breakInFront(player(server)) ? "with spells_edit_blocks false, Break shouldn't mine anything" : null;
			});
			check(blocked == null, blocked);

			// World-changing magic and world events switched off: Break still mines (spells may edit blocks),
			// but frost freezes no water, and no event starts.
			String quiet = WildercordConfig.DEFAULTS.toJson().replace("\"world_changing_magic\": true", "\"world_changing_magic\": false")
				.replace("\"world_events\": true", "\"world_events\": false");
			check(quiet.contains("\"world_changing_magic\": false") && quiet.contains("\"world_events\": false"),
				"the default file should list world_changing_magic and world_events");
			write(path, quiet);
			world.getServer().runCommand("wildercord reload");
			context.waitTicks(2);
			String switchedOff = world.getServer().computeOnServer(server -> {
				ServerPlayer player = player(server);
				if (Config.get().worldChangingMagic() || Config.get().worldEvents()) {
					return "/wildercord reload should read world_changing_magic and world_events: false";
				}
				if (!breakInFront(player)) {
					return "with only world-changing magic off, Break should still mine";
				}
				if (frostOnWater(player)) {
					return "with world_changing_magic false, frost shouldn't freeze water";
				}
				if (WorldEvents.enabled() || WorldEvents.startStorm(player.level(), player, true) != null) {
					return "with world_events false, no mana storm should start";
				}
				return null;
			});
			check(switchedOff == null, switchedOff);

			// The server's cost multiplier prices a scroll too: twice what the spell costs to cast, as the Cord screen shows it.
			String costly = WildercordConfig.DEFAULTS.toJson().replace("\"cost_multiplier\": 1.0", "\"cost_multiplier\": 3.0");
			check(costly.contains("\"cost_multiplier\": 3.0"), "the default file should list mana.cost_multiplier");
			write(path, costly);
			world.getServer().runCommand("wildercord reload");
			context.waitTicks(2);
			String scroll = world.getServer().computeOnServer(server -> {
				ServerPlayer player = player(server);
				ready(player, new ItemStack(WildercordItems.ECHO_CORD));
				SpellCaster.edit(player, 0, ids(Runes.SELF, Runes.HEAL));
				player.getInventory().add(new ItemStack(Items.PAPER));
				player.getInventory().add(new ItemStack(Items.INK_SAC));
				float before = Spellbooks.mana(player);
				dev.wildercord.content.SpellScrollItem.inscribe(player, 0);
				float spent = before - Spellbooks.mana(player);
				SpellCompiler.Compiled heal = SpellCompiler.compile(List.of(Runes.SELF, Runes.HEAL));
				int cast = Heart.manaCost(player, heal);
				player.getInventory().clearContent();
				return cast > heal.manaCost() && Math.abs(spent - 2 * cast) < 0.01F ? null
					: "with cost_multiplier 3, a scroll should cost twice the spell's price of " + cast + " (it took " + spent + ")";
			});
			check(scroll == null, scroll);

			write(path, WildercordConfig.DEFAULTS.toJson());
			world.getServer().runCommand("wildercord reload");
			context.waitTicks(2);
			String allowed = world.getServer().computeOnServer(server ->
				breakInFront(player(server)) ? null : "after reloading the defaults, Break should mine again");
			check(allowed == null, allowed);
			String freezes = world.getServer().computeOnServer(server ->
				frostOnWater(player(server)) ? null : "after reloading the defaults, frost should freeze water again");
			check(freezes == null, freezes);
		} finally {
			try {
				if (original != null) {
					write(path, original);
				} else {
					Files.deleteIfExists(path);
				}
			} catch (AssertionError | IOException ignored) {
				// Best effort: the next start writes the defaults again.
			}
			world.getServer().runCommand("wildercord reload");
		}
	}

	/** Casts Touch · Break at a stone block in front of the player; true if it was mined. */
	private static boolean breakInFront(ServerPlayer player) {
		Vec3 at = ready(player, new ItemStack(WildercordItems.COPPER_CORD));
		BlockPos stone = BlockPos.containing(at.x, at.y + 1, at.z + 2);
		player.level().setBlockAndUpdate(stone, Blocks.STONE.defaultBlockState());
		SpellCaster.edit(player, 0, ids(Runes.TOUCH, Runes.BREAK));
		CastEngine.cast(new Cast(player), SpellCompiler.compile(List.of(Runes.TOUCH, Runes.BREAK)).root());
		return player.level().getBlockState(stone).isAir();
	}

	/** Lands Touch · Frost on a small walled pool in front of the player; true if any of its water froze. */
	private static boolean frostOnWater(ServerPlayer player) {
		Vec3 at = ready(player, new ItemStack(WildercordItems.COPPER_CORD));
		ServerLevel level = player.level();
		// The air just above the water's middle, four blocks ahead.
		BlockPos centre = BlockPos.containing(at.x, at.y, at.z + 4);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				boolean rim = Math.abs(dx) == 2 || Math.abs(dz) == 2;
				level.setBlockAndUpdate(centre.offset(dx, -2, dz), Blocks.STONE.defaultBlockState());
				level.setBlockAndUpdate(centre.offset(dx, -1, dz), rim ? Blocks.STONE.defaultBlockState() : Blocks.WATER.defaultBlockState());
				level.setBlockAndUpdate(centre.offset(dx, 0, dz), Blocks.AIR.defaultBlockState());
				level.setBlockAndUpdate(centre.offset(dx, 1, dz), Blocks.AIR.defaultBlockState());
			}
		}
		SpellPlan.Group group = SpellCompiler.compile(List.of(Runes.TOUCH, Runes.FROST)).root().groups.getFirst();
		Vec3 hit = Vec3.atCenterOf(centre);
		CastEngine.onHit(new Cast(player), group, new Cast.Hit(List.of(), hit, new Vec3(0, 0, 1), player.position(), null, null, false), null);
		boolean froze = false;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos water = centre.offset(dx, -1, dz);
				froze |= level.getBlockState(water).is(Blocks.FROSTED_ICE);
				level.setBlockAndUpdate(water, Blocks.STONE.defaultBlockState());
			}
		}
		return froze;
	}

	private static void write(Path path, String text) {
		try {
			Files.createDirectories(path.getParent());
			Files.writeString(path, text, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new AssertionError("couldn't write " + path + ": " + e);
		}
	}
}
