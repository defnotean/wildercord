package dev.wildercord.gametest;

import dev.wildercord.cast.SpellCaster;
import dev.wildercord.client.CordScreen;
import dev.wildercord.config.Config;
import dev.wildercord.config.WildercordConfig;
import dev.wildercord.content.CordTier;
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
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
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
 * (and gets a row in the Cord screen and a place on the spell wheel); and a config change (spells may not edit blocks) takes effect
 * after {@code /wildercord reload}, and is undone by the next.
 *
 * <p>Runs in the full suite; skipped with {@code WILDERCORD_TOUR_ONLY} or {@code WILDERCORD_CORDS_ONLY}.</p>
 */
public class WildercordGearTest implements FabricClientGameTest {
	private static final int LEFT = 0;

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
			level.addFreshEntity(husk);

			SpellCompiler.Compiled compiled = SpellCompiler.compile(SpellCaster.activeRunes(Spellbooks.get(player), 0, CordTier.ECHO));
			int plainCost = Heart.manaCost(player, compiled);
			float plainSpent = castFresh(player, 0);
			float plainDamage = husk.getMaxHealth() - husk.getHealth();

			husk.setHealth(husk.getMaxHealth());
			husk.clearFire();
			player.setItemInHand(InteractionHand.MAIN_HAND, gear(GearDef.staff("fire")));
			int staffCost = Heart.manaCost(player, compiled);
			float staffSpent = castFresh(player, 0);
			float staffDamage = husk.getMaxHealth() - husk.getHealth();
			husk.discard();
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

			if (plainSpent < 0 || staffSpent < 0) {
				return "Touch · Fire should cast, with and without the staff (spent " + plainSpent + " and " + staffSpent + ")";
			}
			if (plainDamage <= 0) {
				return "Touch · Fire should hurt the husk in front (it took " + plainDamage + ")";
			}
			double ratio = staffDamage / plainDamage;
			if (ratio < 1.15 || ratio > 1.25) {
				return "a Fire Staff should make fire hit 20% harder (" + plainDamage + " -> " + staffDamage + ")";
			}
			// 10% off, rounded up, and always at least one mana saved.
			if (staffCost != Math.min((int) Math.ceil(compiled.cost() * GearDef.STAFF_COST - 1e-9), plainCost - 1) || staffCost >= plainCost) {
				return "a Fire Staff should take 10% off a fire spell's cost (" + plainCost + " -> " + staffCost + ")";
			}
			if (Math.round(plainSpent) != plainCost || Math.round(staffSpent) != staffCost) {
				return "the mana spent should be the cost shown (" + plainSpent + "/" + plainCost + ", " + staffSpent + "/" + staffCost + ")";
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
			return castFresh(player, SpellSlots.TOME) >= 0 ? "the tome's spell shouldn't cast once the tome is put away" : null;
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

			write(path, WildercordConfig.DEFAULTS.toJson());
			world.getServer().runCommand("wildercord reload");
			context.waitTicks(2);
			String allowed = world.getServer().computeOnServer(server ->
				breakInFront(player(server)) ? null : "after reloading the defaults, Break should mine again");
			check(allowed == null, allowed);
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
		castFresh(player, 0);
		return player.level().getBlockState(stone).isAir();
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
