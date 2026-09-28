package dev.wildercord.gametest;

import dev.wildercord.cast.SpellCaster;
import dev.wildercord.client.CordScreen;
import dev.wildercord.client.cosmetic.CordStyleScreen;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.cosmetic.CordCosmetics;
import dev.wildercord.cosmetic.CordStyles;
import dev.wildercord.familiar.Bonds;
import dev.wildercord.familiar.FamiliarContent;
import dev.wildercord.familiar.Familiars;
import dev.wildercord.familiar.Wisp;
import dev.wildercord.familiar.WispRules;
import dev.wildercord.familiar.WispSpawner;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/**
 * Familiars and Cord cosmetics in a real world: a wild wisp shies from the wrong element and bonds
 * after three spells of its own; it follows its owner across the map, casts its little spell at a
 * monster hunting them, learns from the kill, stays and follows on a sneaking click, and goes home
 * to the lantern and comes out again. Then a Cord style is bought and worn, and the client (who
 * sees everyone's) is checked to have it, and the Cosmetics page is opened from the Cord screen and
 * used with the real mouse. Screenshots go to build/run/clientGameTest/screenshots/familiar_*.
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY} and {@code WILDERCORD_CORDS_ONLY}.</p>
 */
public class WildercordFamiliarTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			context.runOnClient(mc -> {
				mc.getWindow().setWindowed(1920, 1080);
				mc.options.guiScale().set(2);
				mc.resizeGui();
			});
			// Night, still, and no mobs (or wild wisps) arriving on their own.
			world.getServer().runCommand("time set 18000");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("weather clear");
			setup(world);
			context.waitTicks(10);
			int wispId = taming(context, world);
			following(context, world, wispId);
			helping(context, world);
			stayAndLantern(context, world);
			cosmetics(context, world);
		}
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static List<String> ids(RuneDef... runes) {
		return java.util.Arrays.stream(runes).map(RuneDef::id).toList();
	}

	private static void check(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError(what);
		}
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.takeScreenshot(TestScreenshotOptions.of("familiar_" + name).disableCounterPrefix());
	}

	private static void place(ServerPlayer player, Vec3 at, float yaw, float pitch) {
		player.teleportTo(player.level(), at.x, at.y, at.z, Set.<Relative>of(), yaw, pitch, false);
	}

	private static Wisp wisp(MinecraftServer server, int id) {
		return player(server).level().getEntity(id) instanceof Wisp wisp ? wisp : null;
	}

	private static void setup(TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.CREATIVE);
			Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
			Spellbook book = Spellbooks.get(player).withStarterGiven();
			for (RuneDef rune : Runes.all()) {
				book = book.learn(rune.id());
			}
			Spellbooks.set(player, book.withSelected(0));
			SpellCaster.edit(player, 0, ids(Runes.BURST, Runes.FIRE));
			SpellCaster.edit(player, 1, ids(Runes.BURST, Runes.FROST));
			Spellbooks.setMana(player, Mana.max(player));
			place(player, player.position(), 0, 10);
		});
	}

	/** Casts one of the player's spells (a Burst, so it reaches whatever is around them) with its cooldown and mana reset. */
	private static void cast(TestSingleplayerContext world, int spell) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Spellbooks.setReadyAt(player, spell, 0);
			Spellbooks.setMana(player, Mana.max(player));
			SpellCaster.cast(player, spell);
		});
	}

	// ------------------------------------------------------------------ taming

	private static int taming(ClientGameTestContext context, TestSingleplayerContext world) {
		int id = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			Vec3 at = player.position().add(0, 1.2, 2.5);
			Wisp wisp = WispSpawner.spawn(player.level(), BlockPos.containing(at), "fire");
			check(wisp != null, "a wisp should spawn");
			// Held still for the taming, so it can't drift out of the Burst.
			wisp.setNoAi(true);
			wisp.snapTo(at.x, at.y, at.z, 180, 0);
			return wisp.getId();
		});
		context.waitTicks(10);
		shot(context, "wild");

		// Frost spooks a Fire wisp: it stays wild.
		cast(world, 1);
		context.waitTicks(12);
		world.getServer().runOnServer(server -> {
			Wisp wisp = wisp(server, id);
			check(wisp != null && wisp.wild(), "a spell of the wrong element shouldn't tame a wisp");
		});

		// Three Fire spells, far enough apart to count, bond it.
		for (int i = 0; i < WispRules.TAMING_HITS; i++) {
			cast(world, 0);
			context.waitTicks(WispRules.MIN_GAP_TICKS + 4);
			if (i < WispRules.TAMING_HITS - 1) {
				world.getServer().runOnServer(server -> {
					Wisp wisp = wisp(server, id);
					check(wisp != null && wisp.wild(), "a wisp shouldn't bond before " + WispRules.TAMING_HITS + " offerings");
				});
			}
		}
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Wisp wisp = wisp(server, id);
			check(wisp != null && !wisp.wild(), "three Fire spells should bond a Fire wisp");
			check(wisp.isOwnedBy(player), "the bonded wisp should be the caster's");
			check(wisp.mode() == Wisp.FOLLOW, "a new familiar should follow");
			Bonds bonds = Familiars.get(player);
			check(bonds.bonds().size() == 1 && bonds.out().equals(wisp.bondId()), "the bond should be saved on the player, and out (has " + bonds + ")");
			check(Heart.discovered(player, "feat:" + Feats.KINDRED), "a first familiar should earn Kindred");
			check(wisp.hasCustomName() && wisp.getCustomName().getString().contains(player.getGameProfile().name()), "a familiar should show its owner's name");
			check(!wisp.canBeHitByProjectile(), "a familiar shouldn't catch arrows meant for its owner");
			wisp.setNoAi(false);
		});
		// The client sees the bond too (it's synced to the owner for the lantern's tooltip).
		context.waitTicks(5);
		int bonded = context.computeOnClient(mc -> Familiars.get(mc.player).bonds().size());
		check(bonded == 1, "the client should see its familiar (sees " + bonded + ")");
		return id;
	}

	// ------------------------------------------------------------------ following

	private static void following(ClientGameTestContext context, TestSingleplayerContext world, int id) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			place(player, player.position().add(12, 0, 0), -90, 10);
		});
		context.waitTicks(60);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Wisp wisp = Familiars.live(player);
			check(wisp != null, "the familiar should still be out");
			double distance = wisp.distanceTo(player);
			check(distance < 3.5, "the familiar should follow to its owner's shoulder (it's " + String.format("%.1f", distance) + " blocks away)");
		});
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		context.waitTicks(10);
		shot(context, "following");
	}

	// ------------------------------------------------------------------ its little spell, and learning from a fight

	private static void helping(ClientGameTestContext context, TestSingleplayerContext world) {
		int husk = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			Husk mob = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
			Vec3 at = player.position().add(0, 0, 5);
			mob.snapTo(at.x, at.y, at.z, 180, 0);
			mob.setNoAi(true);
			mob.setTarget(player);
			level.addFreshEntity(mob);
			return mob.getId();
		});
		// A Fire familiar's ember bolt lands within its first few seconds out.
		boolean hit = false;
		for (int i = 0; i < 16 && !hit; i++) {
			context.waitTicks(10);
			hit = world.getServer().computeOnServer(server -> player(server).level().getEntity(husk) instanceof Husk mob
				&& (mob.getHealth() < mob.getMaxHealth() || mob.isOnFire()));
		}
		check(hit, "the Fire familiar should cast an ember bolt at a monster hunting its owner");
		shot(context, "ember_bolt");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			int before = Familiars.get(player).outBond().xp();
			if (player.level().getEntity(husk) instanceof Husk mob && mob.isAlive()) {
				mob.hurtServer(player.level(), player.damageSources().playerAttack(player), 1000);
			}
			int after = Familiars.get(player).outBond().xp();
			check(after > before, "slaying a monster together should teach the familiar (xp " + before + " -> " + after + ")");
		});
		world.getServer().runCommand("kill @e[type=husk]");
	}

	// ------------------------------------------------------------------ stay, follow, and the lantern

	private static void stayAndLantern(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Wisp wisp = Familiars.live(player);
			player.setShiftKeyDown(true);
			wisp.interact(player, InteractionHand.MAIN_HAND, wisp.position());
			check(wisp.mode() == Wisp.STAY, "a sneaking click should tell the familiar to stay");
			wisp.interact(player, InteractionHand.MAIN_HAND, wisp.position());
			check(wisp.mode() == Wisp.FOLLOW, "another should tell it to follow");
			player.setShiftKeyDown(false);

			ItemStack lantern = new ItemStack(FamiliarContent.WISP_LANTERN);
			player.setItemInHand(InteractionHand.MAIN_HAND, lantern);
			lantern.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
			check(Familiars.get(player).out().isEmpty(), "the lantern should send the familiar home");
			check(wisp.isRemoved(), "a familiar sent home should leave the world");
			lantern.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
			check(!Familiars.get(player).out().isEmpty(), "the lantern should call it out again");
		});
		context.waitTicks(20);
		world.getServer().runOnServer(server -> {
			Wisp wisp = Familiars.live(player(server));
			check(wisp != null && wisp.isAlive() && wisp.mode() == Wisp.FOLLOW, "the familiar called out should be in the world, following");
			player(server).setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		});
	}

	// ------------------------------------------------------------------ Cord cosmetics

	private static void cosmetics(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			check(!CordCosmetics.wear(player, new CordStyles.Style("gold", "spell", "none")), "a style not bought yet can't be worn");
			// An unlock is for good, so it costs its materials even in creative.
			player.getInventory().clearContent();
			check(!CordCosmetics.buy(player, "material:gold"), "gold beads shouldn't be free in creative");
			check(!CordCosmetics.progress(player).bought().contains("material:gold"), "a failed purchase shouldn't unlock anything");
			player.getInventory().add(new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT, 4));
			player.getInventory().add(new ItemStack(net.minecraft.world.item.Items.DYE.cyan()));
			check(CordCosmetics.buy(player, "material:gold"), "gold beads should be bought with four gold ingots");
			check(player.getInventory().countItem(net.minecraft.world.item.Items.GOLD_INGOT) == 0, "buying gold beads should take the ingots, in creative too");
			check(CordCosmetics.buy(player, "glow:cyan"), "a cyan glow should be bought");
			player.setAttached(WildercordAttachments.CIRCLES, 1);
			check(CordCosmetics.wear(player, CordCosmetics.style(player).withTrail("sparks")), "the 1st Circle should unlock the sparks trail");
			check(CordCosmetics.style(player).equals(new CordStyles.Style("gold", "cyan", "sparks")), "the style should be saved (has " + CordCosmetics.style(player) + ")");
		});
		context.waitTicks(10);
		CordStyles.Style seen = context.computeOnClient(mc -> CordCosmetics.style(mc.player));
		check(seen.equals(new CordStyles.Style("gold", "cyan", "sparks")), "the client should see the Cord style (sees " + seen + ")");

		// The wrist, from the front, just after a cast (the beads flare and the sparks fly).
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		cast(world, 0);
		context.waitTicks(3);
		shot(context, "cord_style");
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));

		// The Cosmetics page, opened from the Cord screen's tab with the real mouse.
		context.setScreen(CordScreen::new);
		context.waitTicks(5);
		double scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		double[] tab = context.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).pagePoint(3));
		context.getInput().setCursorPos(tab[0] * scale, tab[1] * scale);
		context.waitTicks(1);
		context.getInput().pressMouse(0);
		context.waitTicks(5);
		boolean page = context.computeOnClient(mc -> mc.gui.screen() instanceof CordStyleScreen);
		check(page, "the Cosmetics tab should open the Cosmetics page");
		double[] obsidian = context.computeOnClient(mc -> ((CordStyleScreen) mc.gui.screen()).optionPoint("material:obsidian"));
		context.getInput().setCursorPos(obsidian[0] * scale, obsidian[1] * scale);
		context.waitTicks(5);
		shot(context, "cosmetics_page");
		// Glass is free: clicking it wears it.
		double[] glass = context.computeOnClient(mc -> ((CordStyleScreen) mc.gui.screen()).optionPoint("material:glass"));
		context.getInput().setCursorPos(glass[0] * scale, glass[1] * scale);
		context.waitTicks(1);
		context.getInput().pressMouse(0);
		context.waitTicks(5);
		String material = world.getServer().computeOnServer(server -> CordCosmetics.style(player(server)).material());
		check(material.equals("glass"), "clicking glass beads should wear them (wears " + material + ")");
		context.runOnClient(mc -> mc.gui.setScreen(null));
		context.waitTicks(3);
	}
}
