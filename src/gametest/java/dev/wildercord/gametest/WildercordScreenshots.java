package dev.wildercord.gametest;

import dev.wildercord.cast.SpellCaster;
import dev.wildercord.client.CordScreen;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.List;

/**
 * Renders the spell HUD and the Cord screen at every GUI scale and at a few window sizes,
 * so layout problems (text spilling, overlaps, clipping) show up in screenshots instead of
 * in play. Screenshots land in build/run/clientGameTest/screenshots.
 */
public class WildercordScreenshots implements FabricClientGameTest {
	private static final int[][] SIZES = {{1920, 1080}, {1280, 720}, {854, 480}};

	@Override
	public void runTest(ClientGameTestContext context) {
		// WILDERCORD_TOUR_ONLY=1 skips these and runs only the feature tour, for quick visual checks.
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setGameMode(GameType.SURVIVAL);
				ItemStack cord = new ItemStack(WildercordItems.ECHO_CORD);
				var enchantments = server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
				cord.enchant(enchantments.getOrThrow(dev.wildercord.player.Mana.RESERVOIR), 2);
				cord.enchant(enchantments.getOrThrow(dev.wildercord.player.Mana.WELLSPRING), 1);
				Spellbooks.setCord(player, cord);
				player.setAttached(dev.wildercord.player.WildercordAttachments.CRYSTALS, 3);
				player.addEffect(new net.minecraft.world.effect.MobEffectInstance(dev.wildercord.content.WildercordEffects.CLARITY, 6000));
				Spellbook book = Spellbooks.get(player).withStarterGiven();
				for (RuneDef rune : Runes.all()) {
					book = book.learn(rune.id());
				}
				Spellbooks.set(player, book);
				SpellCaster.edit(player, 0, ids(Runes.BOLT, Runes.HOMING_MOD, Runes.FIRE, Runes.AMPLIFY, Runes.SPLIT_MOD, Runes.ON_HIT,
					Runes.BURST, Runes.EXPLODE, Runes.WIDEN, Runes.DELAY, Runes.RAIN, Runes.LIGHTNING));
				SpellCaster.edit(player, 1, ids(Runes.SELF, Runes.HEAL, Runes.SWIFT, Runes.NIGHT_EYE, Runes.SHIELD, Runes.AMPLIFY, Runes.EXTEND, Runes.DELAY, Runes.SELF, Runes.FEATHER_FALL));
				SpellCaster.edit(player, 2, ids(Runes.BEAM, Runes.LIGHTNING, Runes.CHAIN_MOD));
				SpellCaster.edit(player, 3, ids(Runes.CONE, Runes.FIRE, Runes.LINGER_MOD, Runes.PULSE, Runes.ORBIT, Runes.SHOCK, Runes.FRUGAL_MOD,
					Runes.ON_HURT, Runes.GRAVITY_WELL, Runes.DELAY, Runes.METEOR, Runes.FREEZE));
				Spellbooks.setMana(player, 180);
			});
			context.waitTicks(10);
			// The mana badge's tooltip, at 1920x1080 and GUI scale 2 (window pixel = 2x GUI unit).
			context.runOnClient(mc -> {
				mc.getWindow().setWindowed(1920, 1080);
				mc.options.guiScale().set(2);
				mc.resizeGui();
			});
			context.waitTicks(5);
			context.setScreen(CordScreen::new);
			context.waitTicks(3);
			context.getInput().setCursorPos((320 + 275 + 7) * 2, (144 + 7 + 7) * 2);
			context.waitTicks(3);
			context.takeScreenshot(TestScreenshotOptions.of("mana_tooltip").disableCounterPrefix());
			// Search: type a query and see the grouped, filtered Codex.
			context.getInput().setCursorPos(4, 4);
			context.getInput().typeChars("fire");
			context.waitTicks(3);
			context.takeScreenshot(TestScreenshotOptions.of("search_fire").disableCounterPrefix());
			// Clear the search, then pick the Effects tab to see its category chips.
			context.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.getKey(new net.minecraft.client.input.KeyEvent(
				com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE, 0, 0)));
			context.waitTicks(2);
			// Effects tab: panel left 294 + tab x (12 + All 32 + Shapes 54) ~ 400 GUI units, row y 124 + 120 + 6.
			context.getInput().setCursorPos((294 + 12 + 34 + 54 + 20) * 2, (124 + 120 + 6) * 2);
			context.getInput().pressMouse(0);
			context.waitTicks(3);
			context.getInput().setCursorPos(4, 4);
			context.waitTicks(2);
			context.takeScreenshot(TestScreenshotOptions.of("effects_chips").disableCounterPrefix());
			context.setScreen(() -> null);
			context.waitTicks(2);
			for (int[] size : SIZES) {
				context.runOnClient(mc -> mc.getWindow().setWindowed(size[0], size[1]));
				context.waitTicks(5);
				for (int scale = 1; scale <= 4; scale++) {
					int guiScale = scale;
					context.runOnClient(mc -> {
						mc.options.guiScale().set(guiScale);
						mc.resizeGui();
					});
					context.getInput().setCursorPos(2, 2);
					context.waitTicks(3);
					String tag = size[0] + "x" + size[1] + "_scale" + scale;
					context.takeScreenshot(TestScreenshotOptions.of("hud_" + tag).disableCounterPrefix());
					context.setScreen(CordScreen::new);
					context.waitTicks(3);
					context.takeScreenshot(TestScreenshotOptions.of("cord_" + tag).disableCounterPrefix());
					context.setScreen(() -> null);
					context.waitTicks(2);
				}
			}
			batch4Screens(context, world);
			castEverything(context, world);
			mechanicsChecks(context, world);
			heartAndPassives(context, world);
			starterChips(context, world);
		}
	}

	/** A Blood Price spell (health cost in the header and HUD) and a search for the Time category. */
	private static void batch4Screens(ClientGameTestContext context, TestSingleplayerContext world) {
		context.runOnClient(mc -> {
			mc.getWindow().setWindowed(1920, 1080);
			mc.options.guiScale().set(2);
			mc.resizeGui();
		});
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			SpellCaster.edit(player, 0, ids(Runes.BARRAGE, Runes.BLOOD_PRICE_MOD, Runes.STASIS, Runes.HARM, Runes.EXECUTE_MOD, Runes.COMBO,
				Runes.BLITZ, Runes.VOW_MOD, Runes.CLEAVE));
			Spellbooks.set(player, Spellbooks.get(player).withSelected(0));
		});
		context.waitTicks(5);
		context.getInput().setCursorPos(2, 2);
		context.takeScreenshot(TestScreenshotOptions.of("hud_blood_price").disableCounterPrefix());
		context.setScreen(CordScreen::new);
		context.waitTicks(3);
		// Real key presses only produce characters while text input is on (typeChars below bypasses that).
		context.runOnClient(mc -> {
			boolean enabled;
			try {
				var field = com.mojang.blaze3d.platform.TextInputManager.class.getDeclaredField("textInputEnabled");
				field.setAccessible(true);
				enabled = (boolean) field.get(mc.textInputManager());
			} catch (ReflectiveOperationException e) {
				throw new AssertionError("Couldn't read TextInputManager.textInputEnabled", e);
			}
			check(enabled, "The Cord screen must turn on text input, or players can't type in the search box");
		});
		context.takeScreenshot(TestScreenshotOptions.of("cord_batch4").disableCounterPrefix());
		context.getInput().typeChars("time");
		context.waitTicks(3);
		context.takeScreenshot(TestScreenshotOptions.of("search_time").disableCounterPrefix());
		context.setScreen(() -> null);
		context.waitTicks(2);
	}

	/**
	 * Casts every batch 4 and batch 6 rune at a few husks, so any error in the new spell code fails the
	 * test instead of a play session. A handful of screenshots show the effects in flight.
	 */
	private static void castEverything(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			server.setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			player.setGameMode(GameType.CREATIVE);
			net.minecraft.server.level.ServerLevel level = player.level();
			player.teleportTo(level, player.getX(), player.getY(), player.getZ(), java.util.Set.of(), -90.0F, 5.0F, false);
		});
		context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
		context.waitTicks(5);
		Object[][] casts = {
			{null, 24, new RuneDef[] {Runes.CONE, Runes.DISMANTLE, Runes.EXECUTE_MOD}},
			{null, 60, new RuneDef[] {Runes.CRESCENT, Runes.CLEAVE}},
			{"fx_barrage_stasis", 14, new RuneDef[] {Runes.BARRAGE, Runes.STASIS, Runes.HARM}},
			{null, 70, new RuneDef[] {Runes.BEAM, Runes.BLACKSPARK}},
			{"fx_orb", 16, new RuneDef[] {Runes.ORB, Runes.BLACKFLAME}},
			{null, 50, new RuneDef[] {Runes.BLITZ, Runes.RIPPLE}},
			{null, 50, new RuneDef[] {Runes.BOLT, Runes.PRIMER}},
			{null, 50, new RuneDef[] {Runes.BURST, Runes.RESONANCE}},
			{"fx_hollow", 8, new RuneDef[] {Runes.BEAM, Runes.HOLLOW}},
			{null, 40, new RuneDef[] {Runes.BOLT, Runes.PULL, Runes.REPEL}},
			{null, 40, new RuneDef[] {Runes.BEAM, Runes.AFTERSHOCK}},
			{null, 50, new RuneDef[] {Runes.BURST, Runes.DECREE}},
			{null, 40, new RuneDef[] {Runes.BEAM, Runes.WEIGH}},
			{null, 40, new RuneDef[] {Runes.BEAM, Runes.SHACKLE}},
			{"fx_bubble", 20, new RuneDef[] {Runes.BEAM, Runes.BUBBLE}},
			{null, 50, new RuneDef[] {Runes.BEAM, Runes.SHOCK}},
			{null, 40, new RuneDef[] {Runes.SELF, Runes.INFINITY, Runes.REVERSAL, Runes.REFLECT, Runes.OVERDRIVE}},
			{null, 40, new RuneDef[] {Runes.SELF, Runes.FORESIGHT, Runes.RESTORE, Runes.ACCELERATE}},
			{null, 30, new RuneDef[] {Runes.BEAM, Runes.SWAP}},
			{null, 30, new RuneDef[] {Runes.BEAM, Runes.SHADOWSTEP}},
			{null, 30, new RuneDef[] {Runes.SELF, Runes.TIME_SKIP}},
			{null, 30, new RuneDef[] {Runes.SELF, Runes.REWIND}},
			{null, 30, new RuneDef[] {Runes.SELF, Runes.ZIPPER}},
			{"fx_rampart", 12, new RuneDef[] {Runes.BEAM, Runes.RAMPART}},
			{null, 30, new RuneDef[] {Runes.SELF, Runes.SHADES}},
			{"fx_thunderbird", 40, new RuneDef[] {Runes.SELF, Runes.THUNDERBIRD}},
			{null, 30, new RuneDef[] {Runes.BOLT, Runes.VOW_MOD, Runes.HARM}},
			{null, 30, new RuneDef[] {Runes.BOLT, Runes.BLOOD_PRICE_MOD, Runes.HARM}},
			{null, 20, new RuneDef[] {Runes.SELF, Runes.IF_AIRBORNE, Runes.SWIFT}},
			{null, 20, new RuneDef[] {Runes.SELF, Runes.SWIFT, Runes.COMBO, Runes.BLITZ, Runes.CLEAVE}},
			{null, 20, new RuneDef[] {Runes.SELF, Runes.SWIFT, Runes.COMBO, Runes.BLITZ, Runes.CLEAVE}},
			{null, 30, new RuneDef[] {Runes.SELF, Runes.SWIFT, Runes.COMBO, Runes.BLITZ, Runes.CLEAVE}},
			{"fx_domain", 30, new RuneDef[] {Runes.DOMAIN, Runes.HARM}},
			// Batch 6: sparks, energy balls and beams.
			{null, 30, new RuneDef[] {Runes.SPARK, Runes.HARM}},
			{null, 30, new RuneDef[] {Runes.SPARK, Runes.VOLLEY_MOD, Runes.EMBER}},
			{null, 30, new RuneDef[] {Runes.RAY, Runes.SHOCK}},
			{"fx_nova", 6, new RuneDef[] {Runes.NOVA, Runes.PUSH}},
			{null, 60, new RuneDef[] {Runes.WISP, Runes.ICICLE}},
			{"fx_comet", 8, new RuneDef[] {Runes.COMET, Runes.FLASHFIRE}},
			{null, 60, new RuneDef[] {Runes.RICOCHET, Runes.PELT}},
			{null, 50, new RuneDef[] {Runes.CLUSTER, Runes.WINDCUT}},
			{"fx_lance", 4, new RuneDef[] {Runes.LANCE, Runes.HARM}},
			{"fx_sweep", 6, new RuneDef[] {Runes.SWEEP, Runes.JOLT}},
			{"fx_prism", 3, new RuneDef[] {Runes.PRISM, Runes.LEECH}},
			{null, 40, new RuneDef[] {Runes.STREAM, Runes.BLEED}},
			// Batch 6: protection.
			{null, 30, new RuneDef[] {Runes.SELF, Runes.BARRIER, Runes.BRACE, Runes.ANCHOR, Runes.BRAMBLE}},
			{null, 30, new RuneDef[] {Runes.SELF, Runes.FROSTWARD, Runes.CUSHION, Runes.DEFLECT}},
			{"fx_haven", 20, new RuneDef[] {Runes.SELF, Runes.HAVEN}},
			// Batch 6: mining and building, against the wall in front of the stage.
			{null, 20, new RuneDef[] {Runes.RAY, Runes.CHISEL}},
			{null, 30, new RuneDef[] {Runes.RAY, Runes.TUNNEL}},
			{null, 30, new RuneDef[] {Runes.RAY, Runes.VEIN}},
			{null, 20, new RuneDef[] {Runes.RAY, Runes.SMELT}},
			{null, 30, new RuneDef[] {Runes.RAY, Runes.FELL}},
			{null, 20, new RuneDef[] {Runes.RAY, Runes.GLIMMER}},
			{null, 20, new RuneDef[] {Runes.SELF, Runes.PRUNE}},
			{null, 12, new RuneDef[] {Runes.SELF, Runes.SPAN}},
			// Batch 6: a simple spell for every element.
			{null, 50, new RuneDef[] {Runes.BOLT, Runes.HEX, Runes.REND, Runes.COUNTDOWN}},
			{null, 40, new RuneDef[] {Runes.BEAM, Runes.COLDSNAP}},
			{null, 30, new RuneDef[] {Runes.BEAM, Runes.BANISH}},
			{"fx_cyclone", 20, new RuneDef[] {Runes.BEAM, Runes.CYCLONE}},
		};
		for (Object[] step : casts) {
			String shot = (String) step[0];
			int wait = (Integer) step[1];
			RuneDef[] runes = (RuneDef[]) step[2];
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				resetStage(player);
				wall(player, wallFor(java.util.Arrays.asList(runes)));
				SpellCaster.edit(player, 3, ids(runes));
				Spellbooks.setReadyAt(player, 3, 0);
				SpellCaster.cast(player, 3);
			});
			context.waitTicks(wait);
			if (shot != null) {
				context.takeScreenshot(TestScreenshotOptions.of(shot).disableCounterPrefix());
				context.waitTicks(40);
			}
		}
		// Let every lingering effect (Domain, Thunderbird, Rampart) run out.
		context.waitTicks(260);
		context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
	}

	private static final double[] STAGE = new double[3];

	/** Puts the player back on the stage facing east, with three husks 5-7 blocks ahead and a wall for Zipper. */
	private static void resetStage(ServerPlayer player) {
		net.minecraft.server.level.ServerLevel level = player.level();
		if (STAGE[1] == 0) {
			STAGE[0] = player.getX();
			STAGE[1] = player.getY();
			STAGE[2] = player.getZ();
		}
		player.teleportTo(level, STAGE[0], STAGE[1], STAGE[2], java.util.Set.of(), -90.0F, 5.0F, false);
		player.setHealth(player.getMaxHealth());
		var box = new net.minecraft.world.phys.AABB(STAGE[0], STAGE[1] - 2, STAGE[2] - 6, STAGE[0] + 12, STAGE[1] + 6, STAGE[2] + 6);
		List<net.minecraft.world.entity.monster.zombie.Husk> husks = level.getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Husk.class, box);
		for (int i = husks.size(); i < 3; i++) {
			var husk = net.minecraft.world.entity.EntityTypes.HUSK.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
			if (husk != null) {
				husk.snapTo(STAGE[0] + 5 + i, STAGE[1], STAGE[2] + (i - 1) * 1.5, 90.0F, 0.0F);
				husk.setPersistenceRequired();
				husk.setNoAi(false);
				level.addFreshEntity(husk);
			}
		}
	}

	/**
	 * What the wall in front of the stage is made of for these runes: stone for Zipper and the
	 * mining runes, iron ore for Vein, logs under living leaves for Fell, or nothing (null).
	 */
	private static net.minecraft.world.level.block.state.BlockState wallFor(List<RuneDef> runes) {
		if (runes.contains(Runes.VEIN)) {
			return net.minecraft.world.level.block.Blocks.IRON_ORE.defaultBlockState();
		}
		if (runes.contains(Runes.FELL)) {
			return net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState();
		}
		for (RuneDef rune : List.of(Runes.ZIPPER, Runes.CHISEL, Runes.TUNNEL, Runes.SMELT, Runes.GLIMMER)) {
			if (runes.contains(rune)) {
				return net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
			}
		}
		return null;
	}

	/** A 2-thick wall right in front of the stage (for Zipper to step through, or to mine), or clears it; logs get leaves on top. */
	private static void wall(ServerPlayer player, net.minecraft.world.level.block.state.BlockState block) {
		net.minecraft.server.level.ServerLevel level = player.level();
		var air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
		boolean logs = block != null && block.is(net.minecraft.tags.BlockTags.LOGS);
		for (int dx = 2; dx <= 3; dx++) {
			for (int dy = 0; dy <= 3; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					var pos = net.minecraft.core.BlockPos.containing(STAGE[0] + dx, STAGE[1] + dy, STAGE[2] + dz);
					var leaves = net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState();
					level.setBlockAndUpdate(pos, block == null ? air : dy < 3 ? block : logs ? leaves : air);
				}
			}
		}
	}

	/**
	 * Checks that the trickier batch 4 mechanics actually work, not just that they don't crash:
	 * Stasis holds damage until it ends, Reflect hurts the attacker, Foresight dodges, Reversal
	 * cheats a death, Blood Price takes health instead of mana, and Combo fires every third cast.
	 */
	private static void mechanicsChecks(ClientGameTestContext context, TestSingleplayerContext world) {
		var server = world.getServer();
		java.util.UUID[] huskId = new java.util.UUID[1];
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			net.minecraft.server.level.ServerLevel level = player.level();
			var everything = player.getBoundingBox().inflate(48.0);
			level.getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Husk.class, everything).forEach(net.minecraft.world.entity.Entity::discard);
			level.getEntitiesOfClass(net.minecraft.world.entity.animal.wolf.Wolf.class, everything).forEach(net.minecraft.world.entity.Entity::discard);
			player.setGameMode(GameType.SURVIVAL);
			player.teleportTo(level, STAGE[0], STAGE[1], STAGE[2], java.util.Set.of(), -90.0F, 5.0F, false);
			player.removeAllEffects();
			player.setHealth(player.getMaxHealth());
			var husk = net.minecraft.world.entity.EntityTypes.HUSK.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
			husk.snapTo(STAGE[0] + 6, STAGE[1], STAGE[2], 90.0F, 0.0F);
			husk.setPersistenceRequired();
			level.addFreshEntity(husk);
			huskId[0] = husk.getUUID();
		});
		context.waitTicks(3);

		// Stasis holds every kind of hit until time moves again: magic, sonic, explosions, delayed blows.
		RuneDef[][] attacks = {
			{Runes.BEAM, Runes.HARM}, {Runes.BEAM, Runes.SONIC_BOOM}, {Runes.BEAM, Runes.CLEAVE},
			{Runes.BEAM, Runes.METEOR}, {Runes.BEAM, Runes.PRIMER}, {Runes.BEAM, Runes.DISMANTLE}};
		for (RuneDef[] attack : attacks) {
			huskId[0] = server.computeOnServer(WildercordScreenshots::freshHusk);
			context.waitTicks(3);
			server.runOnServer(s -> castAs(s, 2, Runes.BEAM, Runes.STASIS));
			context.waitTicks(2);
			float before = server.computeOnServer(s -> health(s, huskId[0]));
			server.runOnServer(s -> castAs(s, 1, attack));
			context.waitTicks(45);
			float during = server.computeOnServer(s -> health(s, huskId[0]));
			check(during == before, attack[1].name() + " should be held by Stasis, but health went " + before + " -> " + during);
			context.waitTicks(70);
			float after = server.computeOnServer(s -> health(s, huskId[0]));
			check(after < before, attack[1].name() + " held by Stasis should land when it ends, but health is " + after);
		}
		// Threaded after the damage, Stasis still goes first.
		huskId[0] = server.computeOnServer(WildercordScreenshots::freshHusk);
		context.waitTicks(3);
		float full = server.computeOnServer(s -> health(s, huskId[0]));
		server.runOnServer(s -> castAs(s, 2, Runes.BEAM, Runes.SONIC_BOOM, Runes.STASIS));
		context.waitTicks(3);
		float held = server.computeOnServer(s -> health(s, huskId[0]));
		check(held == full, "Stasis threaded after Sonic Boom should still hold it, but health went " + full + " -> " + held);
		context.waitTicks(110);
		huskId[0] = server.computeOnServer(WildercordScreenshots::freshHusk);
		context.waitTicks(3);

		// Reflect: the husk hurts the player and takes some of it back.
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			var husk = husk(s, huskId[0]);
			castAs(s, 2, Runes.SELF, Runes.REFLECT);
			float huskBefore = husk.getHealth();
			player.hurtServer(player.level(), player.level().damageSources().mobAttack(husk), 4.0F);
			check(husk.getHealth() < huskBefore, "Reflect should hurt the attacker");
		});

		// Foresight: the next two attacks miss.
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			var husk = husk(s, huskId[0]);
			player.setHealth(player.getMaxHealth());
			castAs(s, 2, Runes.SELF, Runes.FORESIGHT);
			player.setInvulnerableTime(0);
			player.hurtServer(player.level(), player.level().damageSources().mobAttack(husk), 4.0F);
			check(player.getHealth() == player.getMaxHealth(), "Foresight should dodge the first attack");
		});

		// Reversal: a killing blow leaves the player at half health.
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			player.setHealth(player.getMaxHealth());
			castAs(s, 2, Runes.SELF, Runes.REVERSAL);
			player.setInvulnerableTime(0);
			player.hurtServer(player.level(), player.level().damageSources().generic(), 1000.0F);
			check(player.isAlive() && Math.abs(player.getHealth() - player.getMaxHealth() / 2) < 0.01F,
				"Reversal should save the player at half health, health is " + player.getHealth());
		});
		context.waitTicks(40);

		// Blood Price: 3 health (1 per 5 mana, rounded up), no mana.
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			player.removeAllEffects();
			player.setHealth(player.getMaxHealth());
			Spellbooks.setMana(player, 100);
			castAs(s, 1, Runes.BOLT, Runes.BLOOD_PRICE_MOD, Runes.HARM);
			check(Math.abs(player.getHealth() - (player.getMaxHealth() - 3)) < 0.01F, "Blood Price should cost 3 health, health is " + player.getHealth());
			check(Spellbooks.mana(player) >= 99.0F, "Blood Price shouldn't spend mana");
		});

		// Combo: the rest fires on the third cast only.
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			player.removeAllEffects();
			for (int i = 1; i <= 3; i++) {
				castAs(s, 0, Runes.SELF, Runes.COMBO, Runes.SWIFT);
				boolean fast = player.hasEffect(net.minecraft.world.effect.MobEffects.SPEED);
				check(fast == (i == 3), "Combo cast " + i + ": expected Speed " + (i == 3) + " but was " + fast);
			}
		});
		server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().setGameMode(GameType.CREATIVE));
	}

	/** Threads a spell into a slot and casts it right away, with the cooldown cleared and mana topped up. */
	private static void castAs(net.minecraft.server.MinecraftServer server, int spell, RuneDef... runes) {
		ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
		SpellCaster.edit(player, spell, ids(runes));
		Spellbooks.setReadyAt(player, spell, 0);
		if (!java.util.Arrays.asList(runes).contains(Runes.BLOOD_PRICE_MOD)) {
			Spellbooks.setMana(player, 380);
		}
		SpellCaster.cast(player, spell);
	}

	/** Clears any husks and spawns a fresh one 6 blocks in front of the stage; returns its id. */
	private static java.util.UUID freshHusk(net.minecraft.server.MinecraftServer server) {
		ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
		net.minecraft.server.level.ServerLevel level = player.level();
		level.getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Husk.class, player.getBoundingBox().inflate(48.0))
			.forEach(net.minecraft.world.entity.Entity::discard);
		player.teleportTo(level, STAGE[0], STAGE[1], STAGE[2], java.util.Set.of(), -90.0F, 5.0F, false);
		var husk = net.minecraft.world.entity.EntityTypes.HUSK.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
		husk.snapTo(STAGE[0] + 6, STAGE[1], STAGE[2], 90.0F, 0.0F);
		husk.setPersistenceRequired();
		level.addFreshEntity(husk);
		return husk.getUUID();
	}

	/** The husk's health, or -1 once it's dead or gone. */
	private static float health(net.minecraft.server.MinecraftServer server, java.util.UUID id) {
		var entity = server.getPlayerList().getPlayers().getFirst().level().getEntity(id);
		return entity instanceof net.minecraft.world.entity.LivingEntity living && living.isAlive() ? living.getHealth() : -1.0F;
	}

	private static net.minecraft.world.entity.LivingEntity husk(net.minecraft.server.MinecraftServer server, java.util.UUID id) {
		ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
		var entity = player.level().getEntity(id);
		check(entity instanceof net.minecraft.world.entity.LivingEntity living && living.isAlive(), "The test husk is gone");
		return (net.minecraft.world.entity.LivingEntity) entity;
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}

	/**
	 * Heart Circles and passives: forming a circle raises max mana, passives cost mana every second
	 * and keep their buff up, and the new screens (the Passives page, the heart tooltip) render.
	 */
	private static void heartAndPassives(ClientGameTestContext context, TestSingleplayerContext world) {
		var server = world.getServer();
		// A circle forms once enough mana has condensed.
		int[] maxBefore = new int[1];
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			player.setGameMode(GameType.SURVIVAL);
			player.setAttached(dev.wildercord.player.WildercordAttachments.CIRCLES, 0);
			player.setAttached(dev.wildercord.player.WildercordAttachments.CONDENSED, 0);
			check(!dev.wildercord.player.Heart.ready(player), "An empty heart shouldn't be ready for a circle");
			player.setAttached(dev.wildercord.player.WildercordAttachments.CONDENSED, 599);
			check(!dev.wildercord.player.Heart.ready(player), "599 condensed mana isn't enough for the 1st Circle");
			player.setAttached(dev.wildercord.player.WildercordAttachments.CONDENSED, 600);
			check(dev.wildercord.player.Heart.ready(player), "600 condensed mana should make the heart ready for the 1st Circle");
			maxBefore[0] = dev.wildercord.player.Mana.max(player);
			dev.wildercord.cast.HeartCircles.form(player);
			check(dev.wildercord.player.Heart.circles(player) == 1, "Forming should give the 1st Circle");
			check(dev.wildercord.player.Mana.max(player) == maxBefore[0] + dev.wildercord.spell.Circles.MANA_PER_CIRCLE, "The 1st Circle adds max mana");
			check(!dev.wildercord.player.Heart.ready(player), "The 2nd Circle needs its breakthrough, not just mana");
			// Jump to seven circles and form the eighth, for the ring screenshot.
			player.setAttached(dev.wildercord.player.WildercordAttachments.CIRCLES, 7);
			player.setAttached(dev.wildercord.player.WildercordAttachments.CONDENSED, 99999);
			dev.wildercord.cast.HeartCircles.form(player);
		});
		context.runOnClient(mc -> {
			mc.getWindow().setWindowed(1920, 1080);
			mc.options.guiScale().set(2);
			mc.resizeGui();
			mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
		});
		context.waitTicks(4);
		context.takeScreenshot(TestScreenshotOptions.of("fx_circles").disableCounterPrefix());
		context.waitTicks(20);

		// Passives: a Self buff and an Orbit at once (the 8th Circle opens both slots).
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			check(SpellCaster.editPassive(player, 0, ids(Runes.SELF, Runes.REVERSAL)) != null, "Reversal must not be allowed as a passive");
			check(SpellCaster.editPassive(player, 2, ids(Runes.ORBIT, Runes.SHOCK)) != null, "There are only two passive slots");
			check(SpellCaster.editPassive(player, 0, ids(Runes.SELF, Runes.SWIFT)) == null, "Self · Swift should be a valid passive");
			check(SpellCaster.editPassive(player, 1, ids(Runes.ORBIT, Runes.DISMANTLE)) == null, "Orbit · Dismantle should be a valid passive");
			player.removeAllEffects();
			Spellbooks.setMana(player, 100);
		});
		context.waitTicks(45);
		float withPassives = server.computeOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			check(player.hasEffect(net.minecraft.world.effect.MobEffects.SPEED), "The Self · Swift passive should keep Speed up");
			float gained = Spellbooks.mana(player) - 100;
			Spellbooks.setMana(player, 100);
			for (int slot = 0; slot < dev.wildercord.spell.Passives.MAX; slot++) {
				SpellCaster.togglePassive(player, slot);
			}
			return gained;
		});
		context.takeScreenshot(TestScreenshotOptions.of("fx_passives").disableCounterPrefix());
		context.waitTicks(45);
		float withoutPassives = server.computeOnServer(s -> Spellbooks.mana(s.getPlayerList().getPlayers().getFirst()) - 100);
		check(withoutPassives - withPassives > 3, "Passives should drain mana: gained " + withPassives + " with them on, " + withoutPassives + " off");
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			for (int slot = 0; slot < dev.wildercord.spell.Passives.MAX; slot++) {
				SpellCaster.togglePassive(player, slot);
			}
			player.setGameMode(GameType.CREATIVE);
		});
		context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));

		// The Passives page and the heart tooltip.
		context.setScreen(CordScreen::new);
		context.waitTicks(3);
		int[] click = new int[4];
		context.runOnClient(mc -> {
			int scale = (int) mc.getWindow().getGuiScale();
			int left = (mc.getWindow().getGuiScaledWidth() - 372) / 2;
			int top = (mc.getWindow().getGuiScaledHeight() - 292) / 2;
			int nameW = mc.font.width(net.minecraft.network.chat.Component.translatable("item.wildercord.echo_cord"));
			int spellsW = mc.font.width(net.minecraft.network.chat.Component.translatable("screen.wildercord.page.spells")) + 10;
			click[0] = (left + 13 + nameW + 8 + spellsW + 2 + 12) * scale;
			click[1] = (top + 13) * scale;
			click[2] = (left + 372 - 27 - 36 + 7) * scale;
			click[3] = (top + 14) * scale;
		});
		context.getInput().setCursorPos(click[0], click[1]);
		context.getInput().pressMouse(0);
		context.waitTicks(3);
		context.getInput().setCursorPos(4, 4);
		context.waitTicks(2);
		context.takeScreenshot(TestScreenshotOptions.of("cord_passives").disableCounterPrefix());
		context.getInput().setCursorPos(click[2], click[3]);
		context.waitTicks(3);
		context.takeScreenshot(TestScreenshotOptions.of("heart_tooltip").disableCounterPrefix());
		context.setScreen(() -> null);
		context.waitTicks(2);
	}

	/** A brand-new player knows only the starters: the Effects tab should show just the categories they have. */
	private static void starterChips(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			Spellbook book = Spellbook.EMPTY.withStarterGiven();
			for (String id : Runes.STARTER) {
				book = book.learn(id);
			}
			Spellbooks.set(player, book);
		});
		context.runOnClient(mc -> {
			mc.getWindow().setWindowed(1920, 1080);
			mc.options.guiScale().set(2);
			mc.resizeGui();
		});
		context.waitTicks(5);
		context.setScreen(CordScreen::new);
		context.waitTicks(3);
		context.getInput().setCursorPos((294 + 12 + 34 + 54 + 20) * 2, (124 + 120 + 6) * 2);
		context.getInput().pressMouse(0);
		context.waitTicks(3);
		context.getInput().setCursorPos(4, 4);
		context.waitTicks(2);
		context.takeScreenshot(TestScreenshotOptions.of("starter_chips").disableCounterPrefix());
		context.setScreen(() -> null);
		context.waitTicks(2);
	}

	private static List<String> ids(RuneDef... runes) {
		return java.util.Arrays.stream(runes).map(RuneDef::id).toList();
	}
}
