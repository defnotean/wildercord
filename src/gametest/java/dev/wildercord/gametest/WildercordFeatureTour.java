package dev.wildercord.gametest;

import dev.wildercord.cast.Archivist;
import dev.wildercord.cast.Charging;
import dev.wildercord.cast.Imbuing;
import dev.wildercord.cast.Shields;
import dev.wildercord.cast.LeyWalker;
import dev.wildercord.cast.Runebound;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.cast.TrainingDummy;
import dev.wildercord.cast.WildercordEntities;
import dev.wildercord.client.CordScreen;
import dev.wildercord.client.SpellWheelScreen;
import dev.wildercord.client.WildercordKeys;
import dev.wildercord.content.Imbued;
import dev.wildercord.content.WildercordBlocks;
import dev.wildercord.content.WildercordComponents;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.Secrets;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.world.LeyLines;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/**
 * A tour of the features added together (charged casting, the spell wheel, secret spells, the
 * Grimoire, Runebound, domain clashes, the practice dummy, overcasting, innate runes, ley lines,
 * the Wellstone, the Archive and the Archivist): it exercises each one in a normal world, checks
 * what can be checked, and screenshots the rest into build/run/clientGameTest/screenshots/tour_*.
 */
public class WildercordFeatureTour implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		// WILDERCORD_CORDS_ONLY=1 runs only the Cord screen test.
		if (System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).create()) {
			context.waitTicks(60);
			context.runOnClient(mc -> {
				mc.getWindow().setWindowed(1920, 1080);
				mc.options.guiScale().set(2);
				mc.resizeGui();
			});
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule spawn_mobs false");
			setup(world);
			context.waitTicks(20);
			charging(context, world);
			wheelAndScreens(context, world);
			secrets(context, world);
			shapes(context, world);
			runebound(context, world);
			dummy(context, world);
			shields(context, world);
			imbuing(context, world);
			clash(context, world);
			overcast(context, world);
			innate(context, world);
			leyAndWellstone(context, world);
			hero(context, world);
			archive(context, world);
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
		context.takeScreenshot(TestScreenshotOptions.of("tour_" + name).disableCounterPrefix());
	}

	private static void camera(ClientGameTestContext context, CameraType type) {
		context.runOnClient(mc -> mc.options.setCameraType(type));
	}

	/** Stands the player at a point, looking a given way. */
	private static void place(ServerPlayer player, Vec3 at, float yaw, float pitch) {
		player.teleportTo(player.level(), at.x, at.y, at.z, Set.<Relative>of(), yaw, pitch, false);
	}

	private static Vec3 ground(ServerLevel level, double x, double z) {
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
		return new Vec3(Math.floor(x) + 0.5, y, Math.floor(z) + 0.5);
	}

	/** No water over a 90x90 square around {@code center}. */
	private static boolean dry(ServerLevel level, BlockPos center) {
		for (int dx = -45; dx <= 45; dx += 30) {
			for (int dz = -45; dz <= 45; dz += 30) {
				BlockPos at = center.offset(dx, 0, dz);
				int h = level.getChunk(at).getHeight(Heightmap.Types.MOTION_BLOCKING, at.getX() & 15, at.getZ() & 15);
				if (!level.getFluidState(new BlockPos(at.getX(), h - 1, at.getZ())).isEmpty()) {
					return false;
				}
			}
		}
		return true;
	}

	/** Films from a fixed point: the client looks through an invisible marker placed there. */
	private static void director(ClientGameTestContext context, TestSingleplayerContext world, Vec3 eye, Vec3 target) {
		int id = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			level.getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class, player(server).getBoundingBox().inflate(128),
				e -> e.entityTags().contains("wildercord.camera")).forEach(net.minecraft.world.entity.Entity::discard);
			net.minecraft.world.entity.Display.TextDisplay camera = EntityTypes.TEXT_DISPLAY.create(level, EntitySpawnReason.COMMAND);
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
			net.minecraft.world.entity.Entity camera = mc.level.getEntity(id);
			if (camera != null) {
				beforeDirector = mc.options.getCameraType();
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				mc.setCameraEntity(camera);
				if (!mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
				mc.gui.toastManager().clear();
			}
		});
	}

	/** The camera mode to go back to after a director shot. */
	private static CameraType beforeDirector = CameraType.FIRST_PERSON;

	private static void cut(ClientGameTestContext context) {
		context.runOnClient(mc -> {
			mc.setCameraEntity(mc.player);
			mc.options.setCameraType(beforeDirector);
			if (mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
	}

	/** The middle of a cleared, flat 64x64 stage (grass, open sky), made in setup. */
	private static Vec3 stage;

	/** Flattens a square of ground: grass at y, dirt below, air above. */
	private static void clear(TestSingleplayerContext world, int cx, int y, int cz, int half) {
		for (int x0 = cx - half; x0 < cx + half; x0 += 16) {
			for (int z0 = cz - half; z0 < cz + half; z0 += 16) {
				int x1 = Math.min(cx + half - 1, x0 + 15);
				int z1 = Math.min(cz + half - 1, z0 + 15);
				world.getServer().runCommand("fill " + x0 + " " + (y + 1) + " " + z0 + " " + x1 + " " + (y + 40) + " " + z1 + " air");
				world.getServer().runCommand("fill " + x0 + " " + y + " " + z0 + " " + x1 + " " + y + " " + z1 + " grass_block");
				world.getServer().runCommand("fill " + x0 + " " + (y - 3) + " " + z0 + " " + x1 + " " + (y - 1) + " " + z1 + " dirt");
			}
		}
		world.getServer().runCommand("kill @e[type=item]");
		// No cows wandering into the shots.
		world.getServer().runCommand("kill @e[type=!player,type=!text_display]");
	}

	private static void setup(TestSingleplayerContext world) {
		int[] where = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			Vec3 at = ground(player.level(), player.getX(), player.getZ());
			return new int[] {(int) Math.floor(at.x), (int) at.y - 1, (int) Math.floor(at.z)};
		});
		clear(world, where[0], where[1], where[2], 32);
		stage = new Vec3(where[0] + 0.5, where[1] + 1, where[2] + 0.5);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.CREATIVE);
			Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
			Spellbook book = Spellbooks.get(player).withStarterGiven();
			for (RuneDef rune : Runes.all()) {
				book = book.learn(rune.id());
			}
			Spellbooks.set(player, book);
			player.setAttached(WildercordAttachments.CIRCLES, 6);
			player.setAttached(WildercordAttachments.INNATE, Runes.KINDLING.id());
			player.setAttached(WildercordAttachments.ELEMENT_CASTS, java.util.Map.of("fire", 64, "frost", 12));
			SpellCaster.edit(player, 0, ids(Runes.ZONE, Runes.FIRE, Runes.WIDEN, Runes.SHOCK, Runes.LINGER_MOD, Runes.ON_HIT, Runes.BURST));
			SpellCaster.edit(player, 1, ids(Runes.BOLT, Runes.FROST, Runes.SPLIT_MOD));
			SpellCaster.edit(player, 2, ids(Runes.SELF, Runes.SWIFT, Runes.STONESKIN));
			SpellCaster.edit(player, 3, ids(Runes.BEAM, Runes.LIGHTNING, Runes.CHAIN_MOD));
			SpellCaster.rename(player, 2, "Mountain Stride");
			Spellbooks.setMana(player, 400);
			place(player, stage, 0, 0);
		});
	}

	// ------------------------------------------------------------------ charging, the HUD and aim

	private static void charging(ClientGameTestContext context, TestSingleplayerContext world) {
		camera(context, CameraType.THIRD_PERSON_FRONT);
		world.getServer().runOnServer(server -> Charging.request(player(server), 0, true));
		context.waitTicks(12);
		shot(context, "charge_partial");
		context.waitTicks(28);
		shot(context, "charge_full");
		camera(context, CameraType.FIRST_PERSON);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			place(player, player.position(), player.getYRot(), 35);
		});
		context.waitTicks(6);
		shot(context, "charge_first_person");
		camera(context, CameraType.THIRD_PERSON_BACK);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			place(player, player.position(), player.getYRot(), 18);
			check(player.hasAttached(WildercordAttachments.CHARGE), "the charge should still be held");
			Charging.request(player, 0, false);
			check(!player.hasAttached(WildercordAttachments.CHARGE), "releasing ends the charge");
			check(Heart.discovered(player, "feat:" + Feats.CHARGED), "a full charge is a feat");
		});
		context.waitTicks(8);
		shot(context, "charge_released");
		// The circle under the cast, from above: a ring and icon per rune, readable spoke by spoke.
		Vec3 feet = world.getServer().computeOnServer(server -> player(server).position());
		director(context, world, feet.add(0.3, 4.6, -2.4), feet);
		shot(context, "spell_circle");
		cut(context);
		camera(context, CameraType.FIRST_PERSON);
		// A short spell makes a narrow panel: the charge readout must still sit clear of the mana count.
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			SpellCaster.edit(player, 3, List.of());
			SpellCaster.edit(player, 3, ids(Runes.BEAM, Runes.SHOCK));
			Spellbooks.setReadyAt(player, 3, 0);
			Spellbooks.set(player, Spellbooks.get(player).withSelected(3));
			Charging.request(player, 3, true);
		});
		context.waitTicks(Charging.FULL + 2);
		shot(context, "hud_charge_short");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Charging.request(player, 3, false);
			SpellCaster.edit(player, 3, List.of());
			SpellCaster.edit(player, 3, ids(Runes.BEAM, Runes.LIGHTNING, Runes.CHAIN_MOD));
			Spellbooks.set(player, Spellbooks.get(player).withSelected(0));
		});
		// Let that cast's beat pass, so the rhythm below starts from nothing.
		context.waitTicks(60);
		// The beat: cast the quick spell, and catch the HUD as its beat comes round.
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			place(player, player.position(), player.getYRot(), 0);
			Spellbooks.set(player, Spellbooks.get(player).withSelected(1));
			SpellCaster.cast(player, 1);
		});
		int cooldown = world.getServer().computeOnServer(server -> (int) (Spellbooks.readyAt(player(server), 1) - server.overworld().getGameTime()));
		context.waitTicks(Math.max(1, cooldown - 6));
		shot(context, "rhythm_approach");
		context.waitTicks(6);
		world.getServer().runOnServer(server -> SpellCaster.cast(player(server), 1));
		context.waitTicks(1);
		world.getServer().runOnServer(server -> check(player(server).getAttachedOrElse(WildercordAttachments.RHYTHM, WildercordAttachments.Rhythm.NONE).stacks() == 1,
			"casting on the beat starts a rhythm"));
		context.waitTicks(10);
	}

	// ------------------------------------------------------------------ the wheel, the Grimoire page, the readout tools

	private static void wheelAndScreens(ClientGameTestContext context, TestSingleplayerContext world) {
		// Held, the switch key opens the wheel. Let go without pointing and it stays open.
		context.getInput().setCursorPos(960, 540);
		context.getInput().holdKey(WildercordKeys.nextMapping());
		context.waitTicks(8);
		check(context.computeOnClient(mc -> mc.gui.screen() instanceof SpellWheelScreen), "holding the switch key opens the wheel");
		context.getInput().releaseKey(WildercordKeys.nextMapping());
		context.waitTicks(4);
		check(context.computeOnClient(mc -> mc.gui.screen() instanceof SpellWheelScreen wheel && wheel.toggled()),
			"let go without pointing, the wheel stays open");
		context.getInput().setCursorPos(960 + 120, 540);
		context.waitTicks(3);
		shot(context, "spell_wheel");
		// A number picks that spell and closes it.
		context.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_3);
		context.waitTicks(4);
		check(context.computeOnClient(mc -> mc.gui.screen() == null), "a number key chooses and closes the wheel");
		check(world.getServer().computeOnServer(server -> Spellbooks.get(player(server)).selected() == 2), "pressing 3 selects the third spell");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Spellbooks.set(player, Spellbooks.get(player).withSelected(2));
			for (String key : List.of("reaction:shatter", "reaction:conduct", "feat:" + Feats.RUNEBOUND, "hint:" + Secrets.SUNFALL.id(), "hint:" + Secrets.ZERO_HOUR.id())) {
				dev.wildercord.cast.Grimoire.unlock(player, key);
			}
		});
		context.waitTicks(10);
		shot(context, "toast");
		context.setScreen(CordScreen::new);
		// Long enough for the spell's circle beside the window to open fully.
		context.waitTicks(18);
		context.getInput().setCursorPos(4, 4);
		shot(context, "cord_named_spell");
		context.runOnClient(mc -> {
			try {
				var field = CordScreen.class.getDeclaredField("grimoirePage");
				field.setAccessible(true);
				field.set(mc.gui.screen(), true);
			} catch (ReflectiveOperationException e) {
				throw new AssertionError(e);
			}
		});
		context.waitTicks(3);
		shot(context, "grimoire_page");
		context.setScreen(() -> null);
		context.waitTicks(2);
	}

	// ------------------------------------------------------------------ secret spells

	private static void secrets(ClientGameTestContext context, TestSingleplayerContext world) {
		camera(context, CameraType.THIRD_PERSON_BACK);
		String[][] casts = {
			{"sunfall", "22"}, {"glacial_lance", "9"}, {"singularity", "40"}, {"starlight_cascade", "30"},
			{"tectonic_rise", "14"}, {"horizon_cut", "4"}, {"zero_hour", "20"}, {"petal_storm", "30"},
		};
		for (String[] cast : casts) {
			Secrets.Secret secret = Secrets.byId(cast[0]).orElseThrow();
			camera(context, CameraType.THIRD_PERSON_BACK);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				ServerLevel level = player.level();
				Vec3 at = stage.add(0, 0, -8);
				place(player, at, 0, secret.id().equals("sunfall") ? -12 : 4);
				if (!secret.id().equals("sunfall")) {
					// Found already: no title in the way of the spell.
					dev.wildercord.cast.Grimoire.unlock(player, secret.key());
				}
				// A few targets out in front.
				for (int i = 0; i < 4; i++) {
					Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
					if (husk != null) {
						Vec3 spot = secret.id().equals("glacial_lance")
							? new Vec3(at.x + (i % 2 == 0 ? -0.3 : 0.3), at.y, at.z + 6 + i * 3)
							: new Vec3(at.x + (i - 1.5) * 2.5, at.y, at.z + 10 + (i % 2) * 3);
						husk.snapTo(spot.x, spot.y, spot.z, 180, 0);
						husk.setNoAi(true);
						husk.addTag("wildercord.tour");
						level.addFreshEntity(husk);
					}
				}
				SpellCaster.edit(player, 0, List.of());
				SpellCaster.edit(player, 0, secret.runes().stream().map(RuneDef::id).toList());
				Spellbooks.setReadyAt(player, 0, 0);
				SpellCaster.cast(player, 0);
				check(Heart.discovered(player, secret.key()), secret.name() + " should be written into the Grimoire when first cast");
			});
			// Filmed from the side, except the two centred on the caster.
			if (!secret.id().equals("sunfall") && !secret.id().equals("petal_storm")) {
				Vec3 at = stage.add(0, 0, -8);
				director(context, world, at.add(-10, 5.5, 2), at.add(0, 1.2, 7));
			}
			context.waitTicks(Integer.parseInt(cast[1]));
			shot(context, "secret_" + cast[0]);
			cut(context);
			// Zero Hour's circles stay on the ground a few seconds: let them go before the next spell.
			context.waitTicks(secret.id().equals("zero_hour") ? 170 : 70);
			world.getServer().runCommand("kill @e[tag=wildercord.tour]");
			world.getServer().runCommand("kill @e[type=item]");
			world.getServer().runCommand("kill @e[type=experience_orb]");
			context.waitTicks(5);
		}
		// Petal Storm follows its caster for eight seconds: let it blow over before the next scene.
		context.waitTicks(90);
	}

	// ------------------------------------------------------------------ every shape

	/**
	 * Every shape, cast at a few husks and filmed from the side at its best moment. The long-lasting
	 * ones (Zone, Domain) come last, so they don't drift into the others' shots.
	 */
	private static void shapes(ClientGameTestContext context, TestSingleplayerContext world) {
		// shape, effect, ticks to wait, how far the camera stands (0 near, 1 middle, 2 far)
		Object[][] gallery = {
			{Runes.BOLT, Runes.FIRE, 4, 1}, {Runes.ARC, Runes.FROST, 9, 1}, {Runes.TOUCH, Runes.HARM, 2, 0}, {Runes.SELF, Runes.HEAL, 4, 0},
			{Runes.BEAM, Runes.SHOCK, 3, 1}, {Runes.CONE, Runes.FROST, 3, 1}, {Runes.CRESCENT, Runes.FIRE, 4, 1}, {Runes.BARRAGE, Runes.HARM, 7, 0},
			{Runes.BLITZ, Runes.SHOCK, 3, 1}, {Runes.ORB, Runes.WITHER, 10, 1}, {Runes.BURST, Runes.FIRE, 3, 1}, {Runes.RING, Runes.SHOCK, 6, 1},
			{Runes.PILLAR, Runes.HARM, 5, 1}, {Runes.WAVE, Runes.PUSH, 8, 1}, {Runes.WALL, Runes.FIRE, 12, 1}, {Runes.ORBIT, Runes.HARM, 14, 1},
			{Runes.SPARK, Runes.EMBER, 3, 1}, {Runes.RAY, Runes.JOLT, 2, 1}, {Runes.NOVA, Runes.COLDSNAP, 3, 0}, {Runes.WISP, Runes.HEX, 12, 1},
			{Runes.COMET, Runes.FLASHFIRE, 9, 1}, {Runes.RICOCHET, Runes.PELT, 12, 1}, {Runes.CLUSTER, Runes.ICICLE, 9, 1},
			{Runes.LANCE, Runes.WINDCUT, 3, 1}, {Runes.SWEEP, Runes.FLASHFIRE, 6, 1}, {Runes.PRISM, Runes.JOLT, 3, 1},
			{Runes.STREAM, Runes.LEECH, 12, 1}, {Runes.SELF, Runes.HAVEN, 8, 1}, {Runes.SELF, Runes.BARRIER, 5, 0},
			{Runes.RAIN, Runes.FROST, 16, 3}, {Runes.MINE, Runes.FIRE, 12, 1}, {Runes.TOTEM, Runes.HEAL, 24, 1}, {Runes.ZONE, Runes.FIRE, 22, 2},
			{Runes.DOMAIN, Runes.FROST, 34, 2},
		};
		for (Object[] entry : gallery) {
			RuneDef shape = (RuneDef) entry[0];
			RuneDef effect = (RuneDef) entry[1];
			int wait = (Integer) entry[2];
			int far = (Integer) entry[3];
			camera(context, CameraType.THIRD_PERSON_BACK);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				ServerLevel level = player.level();
				Vec3 at = stage.add(0, 0, -8);
				place(player, at, 0, 12);
				for (int i = 0; i < 3; i++) {
					Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
					if (husk != null) {
						// Close up for the shapes that strike what's in front of you, further for the rest.
						double near = far == 0 ? 1.8 : 6;
						husk.snapTo(at.x + (i - 1) * 1.6, at.y, at.z + near + i * 1.5, 180, 0);
						husk.setNoAi(true);
						husk.addTag("wildercord.tour");
						level.addFreshEntity(husk);
					}
				}
				SpellCaster.edit(player, 0, List.of());
				SpellCaster.edit(player, 0, ids(shape, effect));
				Spellbooks.setMana(player, 400);
				Spellbooks.setReadyAt(player, 0, 0);
				SpellCaster.cast(player, 0);
			});
			Vec3 at = stage.add(0, 0, -8);
			if (far == 0) {
				director(context, world, at.add(-3.6, 2.2, 3.2), at.add(0, 1.1, 1.8));
			} else if (far == 1) {
				director(context, world, at.add(-7.5, 3.6, 4.5), at.add(0, 1.0, 5));
			} else if (far == 2) {
				director(context, world, at.add(-12, 8, 2), at.add(0, 0, 7.5));
			} else {
				// Back and low enough to take in the sky circle too.
				director(context, world, at.add(-14, 4, 1), at.add(0, 5.5, 7.5));
			}
			context.waitTicks(Math.max(1, wait - 3));
			shot(context, "shape_" + shape.path() + (shape == Runes.SELF ? "_" + effect.path() : ""));
			cut(context);
			// The ones that stay a while get time to go, so they don't turn up in the next shot.
			boolean lingers = shape == Runes.WALL || shape == Runes.ORBIT || shape == Runes.TOTEM || shape == Runes.TRAIL;
			context.waitTicks(lingers ? 200 : 30);
			world.getServer().runCommand("kill @e[tag=wildercord.tour]");
			world.getServer().runCommand("kill @e[type=item]");
			world.getServer().runCommand("kill @e[type=experience_orb]");
			context.waitTicks(5);
		}
		// Let the Domain and Zone finish before the next scene.
		context.waitTicks(160);
	}

	// ------------------------------------------------------------------ the README's moving header

	/**
	 * Frames for the moving header at the top of the README (stitched by tools/make_gif.py): a spell
	 * charged and fired, facing the camera, then a Domain unfolding, seen from above.
	 */
	private static void hero(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("kill @e[type=item]");
		camera(context, CameraType.THIRD_PERSON_FRONT);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.CREATIVE);
			player.removeAllEffects();
			place(player, stage, 180, 8);
			SpellCaster.edit(player, 0, List.of());
			SpellCaster.edit(player, 0, ids(Runes.NOVA, Runes.FLASHFIRE, Runes.WIDEN, Runes.SHOCK, Runes.AMPLIFY, Runes.ON_HIT, Runes.BURST));
			Spellbooks.set(player, Spellbooks.get(player).withSelected(0));
			Spellbooks.setMana(player, 400);
			Spellbooks.setReadyAt(player, 0, 0);
		});
		context.waitTicks(10);
		world.getServer().runOnServer(server -> Charging.request(player(server), 0, true));
		int frame = 0;
		for (int i = 0; i < 18; i++) {
			context.waitTicks(2);
			shot(context, String.format(java.util.Locale.ROOT, "hero_%03d", frame++));
		}
		// The release, still facing the camera: a nova of fire bursting out round the caster, over husks.
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			for (int i = 0; i < 3; i++) {
				Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
				if (husk != null) {
					// Beside and behind the caster (who faces the camera), never between them and it.
					double a = Math.PI * (0.12 + 0.38 * i);
					husk.snapTo(stage.x + Math.cos(a) * 2.2, stage.y, stage.z + Math.sin(a) * 2.2, 180, 0);
					husk.setNoAi(true);
					husk.addTag("wildercord.tour");
					level.addFreshEntity(husk);
				}
			}
		});
		context.waitTicks(1);
		world.getServer().runOnServer(server -> Charging.request(player(server), 0, false));
		for (int i = 0; i < 8; i++) {
			context.waitTicks(2);
			shot(context, String.format(java.util.Locale.ROOT, "hero_%03d", frame++));
		}
		context.waitTicks(40);
		world.getServer().runCommand("kill @e[tag=wildercord.tour]");
		// A Domain unfolding over three husks, from above and to the side.
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			place(player, stage.add(0, 0, -8), 0, 20);
			for (int i = 0; i < 3; i++) {
				Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
				if (husk != null) {
					husk.snapTo(stage.x + (i - 1) * 2.2, stage.y, stage.z - 2 + (i % 2) * 2, 180, 0);
					husk.setNoAi(true);
					husk.addTag("wildercord.tour");
					level.addFreshEntity(husk);
				}
			}
			SpellCaster.edit(player, 0, List.of());
			SpellCaster.edit(player, 0, ids(Runes.DOMAIN, Runes.FROST, Runes.SHOCK));
			Spellbooks.setReadyAt(player, 0, 0);
		});
		director(context, world, stage.add(-11, 7, -9), stage.add(0, 0.5, -2));
		world.getServer().runOnServer(server -> SpellCaster.cast(player(server), 0));
		for (int i = 0; i < 16; i++) {
			context.waitTicks(3);
			shot(context, String.format(java.util.Locale.ROOT, "hero_%03d", frame++));
		}
		cut(context);
		context.waitTicks(200);
		world.getServer().runCommand("kill @e[tag=wildercord.tour]");
		camera(context, CameraType.FIRST_PERSON);
	}

	// ------------------------------------------------------------------ Runebound

	private static void runebound(ClientGameTestContext context, TestSingleplayerContext world) {
		// First, a close look at a Runebound's glowing marks, at night.
		world.getServer().runCommand("time set 18000");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			place(player, stage.add(0, 0, -10), 0, 0);
			Mob zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			if (zombie != null) {
				zombie.snapTo(stage.x, stage.y, stage.z, 200, 0);
				zombie.setNoAi(true);
				zombie.addTag("wildercord.tour");
				level.addFreshEntity(zombie);
				Runebound.bind(zombie, List.of(Runes.BOLT, Runes.FIRE, Runes.SPLIT_MOD), true);
			}
		});
		director(context, world, stage.add(-1.4, 1.7, 2.6), stage.add(0, 1.1, 0));
		context.waitTicks(10);
		shot(context, "runebound_marks");
		cut(context);
		world.getServer().runCommand("kill @e[tag=wildercord.tour]");
		world.getServer().runCommand("time set 6000");
		camera(context, CameraType.FIRST_PERSON);
		world.getServer().runOnServer(server -> {
			server.setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
			ServerPlayer player = player(server);
			player.setGameMode(GameType.SURVIVAL);
			player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 2400, 4, false, false));
			player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 2400, 2, false, false));
			ServerLevel level = player.level();
			player.setTicksFrozen(0);
			Vec3 at = stage.add(0, 0, -6);
			place(player, at, 0, 0);
			Mob caster = EntityTypes.PILLAGER.create(level, EntitySpawnReason.COMMAND);
			Vec3 spot = at.add(0, 0, 9);
			caster.snapTo(spot.x, spot.y, spot.z, 180, 0);
			caster.addTag("wildercord.tour");
			caster.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			level.addFreshEntity(caster);
			Runebound.bind(caster, List.of(Runes.BOLT, Runes.FROST, Runes.SPLIT_MOD), true);
			caster.setTarget(player);
		});
		// Wait for it to start a telegraph, then catch the circle and the lit nameplate.
		int waited = world.getServer().waitFor(server -> {
			List<Mob> mobs = player(server).level().getEntitiesOfClass(Mob.class, player(server).getBoundingBox().inflate(20), m -> m.entityTags().contains("wildercord.tour"));
			return !mobs.isEmpty() && mobs.getFirst().getCustomName() != null && mobs.getFirst().getCustomName().getString().startsWith("»");
		}, 400);
		context.waitTicks(8);
		shot(context, "runebound_telegraph");
		context.waitTicks(20);
		shot(context, "runebound_cast");
		world.getServer().runCommand("kill @e[tag=wildercord.tour]");
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
		world.getServer().runOnServer(server -> player(server).setGameMode(GameType.CREATIVE));
		context.waitTicks(5);
		check(waited >= 0, "a Runebound with a target should telegraph a cast");
	}

	// ------------------------------------------------------------------ the practice dummy

	private static void dummy(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			player.setTicksFrozen(0);
			player.removeAllEffects();
			Vec3 at = stage.add(16, 0, 12);
			place(player, at, 0, 2);
			TrainingDummy dummy = WildercordEntities.TRAINING_DUMMY.create(level, EntitySpawnReason.COMMAND);
			Vec3 spot = at.add(0, 0, 7);
			dummy.snapTo(spot.x, spot.y, spot.z, 180, 0);
			dummy.setYHeadRot(180);
			dummy.setYBodyRot(180);
			dummy.addTag("wildercord.tour");
			level.addFreshEntity(dummy);
		});
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			SpellCaster.edit(player, 0, List.of());
			SpellCaster.edit(player, 0, ids(Runes.BOLT, Runes.HARM));
			SpellCaster.edit(player, 1, List.of());
			SpellCaster.edit(player, 1, ids(Runes.TOUCH, Runes.HARM, Runes.AMPLIFY));
			SpellCaster.edit(player, 3, List.of());
			SpellCaster.edit(player, 3, ids(Runes.BEAM, Runes.SHOCK));
		});
		for (int i = 0; i < 5; i++) {
			int spell = new int[] {0, 3, 0, 3, 0}[i];
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				Spellbooks.setReadyAt(player, spell, 0);
				SpellCaster.cast(player, spell);
			});
			context.waitTicks(4);
		}
		// What the caster sees: the spell, not its own cast effects in their face.
		shot(context, "dummy_first_person");
		Vec3 at = stage.add(16, 0, 12);
		director(context, world, at.add(-4.5, 2.2, 3.2), at.add(0, 1.1, 5.2));
		shot(context, "dummy_numbers");
		cut(context);
		context.waitTicks(40);
		world.getServer().runCommand("kill @e[tag=wildercord.tour]");
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
	}

	// ------------------------------------------------------------------ Shield: a spell stopped, and a spell that breaks through

	private static final Vec3 SHIELD_SPOT = new Vec3(-16, 0, 6);
	/** How far ahead of the player the shielded husk stands: far enough to see its circles spawn in before the bolt arrives. */
	private static final double SHIELD_RANGE = 12;

	/** A husk standing still ahead of the player, with a Shield of this strength (the player's spells aim at it). */
	private static void shieldedHusk(TestSingleplayerContext world, float strength) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			Vec3 at = stage.add(SHIELD_SPOT);
			place(player, at, 0, 0);
			level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(30), m -> m.entityTags().contains("wildercord.tour")).forEach(Mob::discard);
			Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
			Vec3 spot = at.add(0, 0, SHIELD_RANGE);
			husk.snapTo(spot.x, spot.y, spot.z, 180, 0);
			husk.setYHeadRot(180);
			husk.setYBodyRot(180);
			husk.setNoAi(true);
			husk.addTag("wildercord.tour");
			level.addFreshEntity(husk);
			Shields.give(husk, strength, 1200, List.of(Runes.SELF.id(), Runes.SHIELD.id(), Runes.AMPLIFY.id()));
		});
	}

	/** A shielded husk, a spell cast at it, and a frame every tick of what follows. */
	private static void film(ClientGameTestContext context, TestSingleplayerContext world, float strength, int spell, String name, int frames) {
		shieldedHusk(world, strength);
		context.waitTicks(8);
		world.getServer().runOnServer(server -> {
			Spellbooks.setReadyAt(player(server), spell, 0);
			SpellCaster.cast(player(server), spell);
		});
		for (int i = 0; i < frames; i++) {
			context.waitTicks(1);
			shot(context, String.format(java.util.Locale.ROOT, "%s_%02d", name, i));
		}
		context.waitTicks(20);
	}

	private static Mob tourHusk(MinecraftServer server) {
		List<Mob> mobs = player(server).level().getEntitiesOfClass(Mob.class, player(server).getBoundingBox().inflate(30), m -> m.entityTags().contains("wildercord.tour"));
		return mobs.isEmpty() ? null : mobs.getFirst();
	}

	private static void shields(ClientGameTestContext context, TestSingleplayerContext world) {
		double bolt = SpellCompiler.compile(List.of(Runes.BOLT, Runes.HARM)).cost();
		List<RuneDef> heavy = List.of(Runes.BOLT, Runes.HARM, Runes.AMPLIFY, Runes.AMPLIFY, Runes.AMPLIFY, Runes.AMPLIFY);
		double big = SpellCompiler.compile(heavy).cost();
		// Your own, raised: its circles open in front of you, as many as its strength stacks.
		camera(context, CameraType.THIRD_PERSON_FRONT);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.removeAllEffects();
			place(player, stage.add(SHIELD_SPOT), 0, 0);
			SpellCaster.edit(player, 0, List.of());
			SpellCaster.edit(player, 0, ids(Runes.BOLT, Runes.HARM));
			SpellCaster.edit(player, 3, List.of());
			SpellCaster.edit(player, 3, heavy.stream().map(RuneDef::id).toList());
			SpellCaster.edit(player, 2, List.of());
			SpellCaster.edit(player, 2, ids(Runes.SELF, Runes.SHIELD, Runes.AMPLIFY, Runes.AMPLIFY));
			Spellbooks.setReadyAt(player, 2, 0);
			SpellCaster.cast(player, 2);
			WildercordAttachments.SpellShield own = player.getAttached(WildercordAttachments.SPELL_SHIELD);
			check(own != null && Math.abs(own.strength() - SpellCompiler.compile(List.of(Runes.SELF, Runes.SHIELD, Runes.AMPLIFY, Runes.AMPLIFY)).cost()) < 1e-3,
				"a Shield should be as strong as the spell that raised it cost");
		});
		context.waitTicks(8);
		shot(context, "shield_raised");
		camera(context, CameraType.FIRST_PERSON);
		world.getServer().runOnServer(server -> player(server).removeAttached(WildercordAttachments.SPELL_SHIELD));
		// A strong Shield (seven circles): the bolt shatters the front one, and the next holds.
		check(Shields.layers(50) == 7 && Shields.punched(50, bolt) == 1, "a 50-mana Shield stacks 7 circles, and a bolt breaks one");
		shieldedHusk(world, 50);
		Vec3 husk = stage.add(SHIELD_SPOT).add(0, 0, SHIELD_RANGE);
		director(context, world, husk.add(-4.4, 1.6, -4.2), husk.add(0, 1.0, -1.8));
		context.waitTicks(6);
		world.getServer().runOnServer(server -> {
			Spellbooks.setReadyAt(player(server), 0, 0);
			SpellCaster.cast(player(server), 0);
		});
		context.waitTicks(4);
		shot(context, "shield_appear");
		int blocked = world.getServer().waitFor(server -> {
			Mob mob = tourHusk(server);
			return mob != null && !mob.hasAttached(WildercordAttachments.SPELL_SHIELD);
		}, 60);
		context.waitTicks(3);
		shot(context, "shield_block");
		world.getServer().runOnServer(server -> {
			Mob mob = tourHusk(server);
			check(mob != null && mob.getHealth() >= mob.getMaxHealth(), "a spell costing no more than a Shield should be stopped by it");
		});
		check(blocked >= 0, "a bolt should reach the shielded husk");
		context.waitTicks(24);
		// A Shield a little weaker than a heavy bolt: every circle shatters, front to back, and the bolt goes through.
		shieldedHusk(world, (float) big - 1);
		context.waitTicks(6);
		world.getServer().runOnServer(server -> {
			Spellbooks.setReadyAt(player(server), 3, 0);
			SpellCaster.cast(player(server), 3);
		});
		int broke = world.getServer().waitFor(server -> {
			Mob mob = tourHusk(server);
			return mob != null && !mob.hasAttached(WildercordAttachments.SPELL_SHIELD);
		}, 60);
		context.waitTicks(5);
		shot(context, "shield_shatter");
		context.waitTicks(8);
		shot(context, "shield_shards");
		// Frames for the moving pictures (tools/make_gif.py): a strong Shield stopping a bolt, then one shattering.
		context.waitTicks(30);
		film(context, world, 50, 0, "gif_shield_block", 30);
		film(context, world, (float) big - 1, 3, "gif_shield_break", 44);
		cut(context);
		world.getServer().runOnServer(server -> {
			Mob mob = tourHusk(server);
			check(mob == null || mob.getHealth() < mob.getMaxHealth(), "a spell costing more than a Shield should break it and hit");
			check(Heart.discovered(player(server), "feat:" + Feats.SHIELDBREAKER), "breaking a Shield is a feat");
		});
		check(broke >= 0, "a bolt should reach the second shielded husk");
		context.waitTicks(30);
		world.getServer().runCommand("kill @e[tag=wildercord.tour]");
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
	}

	// ------------------------------------------------------------------ Imbue: a spell stored in a sword, and in a block

	/** Imbuing's limits: one shared cooldown, a spell that only helps goes to the holder, and only the newest few imbued items hold. */
	private static void imbueRules(ClientGameTestContext context, TestSingleplayerContext world) {
		List<ItemStack> sticks = new java.util.ArrayList<>();
		// The sword's release cools first.
		context.waitTicks(20);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			SpellCaster.edit(player, 0, List.of());
			SpellCaster.edit(player, 0, ids(Runes.SELF, Runes.IMBUE, Runes.BOLT, Runes.HARM));
			for (int i = 0; i <= Imbuing.MAX_ITEMS; i++) {
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
				Spellbooks.setReadyAt(player, 0, 0);
				SpellCaster.cast(player, 0);
				sticks.add(player.getMainHandItem());
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			}
			check(sticks.stream().allMatch(s -> s.has(WildercordComponents.IMBUED)), "every stick should take the spell");
			check(Imbuing.Ledger.of(level).count(player.getUUID()) == Imbuing.MAX_ITEMS, "only the newest " + Imbuing.MAX_ITEMS + " imbued items should count");
			// The first one's magic has faded: using it releases nothing and leaves a plain stick.
			player.setItemInHand(InteractionHand.MAIN_HAND, sticks.getFirst());
			player.gameMode.useItem(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND);
			check(!sticks.getFirst().has(WildercordComponents.IMBUED), "the oldest imbued item should fade once there are too many");
			// The newest releases, and then everything imbued waits out the spell's cooldown.
			ItemStack newest = sticks.getLast();
			player.setItemInHand(InteractionHand.MAIN_HAND, newest);
			player.gameMode.useItem(player, level, newest, InteractionHand.MAIN_HAND);
			check(newest.get(WildercordComponents.IMBUED).charges() == 2, "using an imbued item should release it");
			ItemStack other = sticks.get(sticks.size() - 2);
			player.setItemInHand(InteractionHand.MAIN_HAND, other);
			player.gameMode.useItem(player, level, other, InteractionHand.MAIN_HAND);
			check(other.get(WildercordComponents.IMBUED).charges() == 3, "another imbued item shouldn't release during the shared cooldown");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		});
		// A sword that holds only a Heal heals its wielder when it strikes, instead of the foe.
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			SpellCaster.edit(player, 0, List.of());
			SpellCaster.edit(player, 0, ids(Runes.SELF, Runes.IMBUE, Runes.HEAL));
			Spellbooks.setReadyAt(player, 0, 0);
			SpellCaster.cast(player, 0);
			Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
			Vec3 spot = player.position().add(player.getLookAngle().multiply(1, 0, 1).normalize().scale(1.6));
			husk.snapTo(spot.x, spot.y, spot.z, 180, 0);
			husk.setNoAi(true);
			husk.addTag("wildercord.tour");
			level.addFreshEntity(husk);
			player.setHealth(8);
		});
		// The shared cooldown from the sticks runs out first.
		context.waitTicks(60);
		world.getServer().runOnServer(server -> {
			Mob husk = tourHusk(server);
			if (husk != null) {
				player(server).attack(husk);
			}
		});
		context.waitTicks(4);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			check(player.getHealth() > 8, "a sword holding only a Heal should heal its wielder on a strike, has " + player.getHealth());
			player.setHealth(player.getMaxHealth());
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			Mob husk = tourHusk(server);
			if (husk != null) {
				husk.discard();
			}
		});
	}

	private static void imbuing(ClientGameTestContext context, TestSingleplayerContext world) {
		Vec3 at = stage.add(-16, 0, -12);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			place(player, at, 0, 0);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			SpellCaster.edit(player, 0, List.of());
			SpellCaster.edit(player, 0, ids(Runes.SELF, Runes.IMBUE, Runes.FIRE));
			Spellbooks.setReadyAt(player, 0, 0);
			SpellCaster.cast(player, 0);
			Imbued imbued = player.getMainHandItem().get(WildercordComponents.IMBUED);
			check(imbued != null && imbued.charges() == 3 && imbued.runes().equals(List.of(Runes.FIRE.id())), "Self Imbue Fire should imbue the held sword with 3 charges");
		});
		// From the front, so the glinting sword and the circle under the caster both show.
		camera(context, CameraType.THIRD_PERSON_FRONT);
		context.waitTicks(8);
		shot(context, "imbue_item");
		camera(context, CameraType.FIRST_PERSON);
		// A strike with it lets the fire go at what it hits.
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
			Vec3 spot = at.add(0, 0, 1.6);
			husk.snapTo(spot.x, spot.y, spot.z, 180, 0);
			husk.setNoAi(true);
			husk.addTag("wildercord.tour");
			level.addFreshEntity(husk);
		});
		context.waitTicks(2);
		world.getServer().runOnServer(server -> {
			Mob husk = tourHusk(server);
			if (husk != null) {
				player(server).attack(husk);
			}
		});
		context.waitTicks(4);
		world.getServer().runOnServer(server -> {
			Mob husk = tourHusk(server);
			check(husk != null && husk.isOnFire(), "striking with a sword imbued with Fire should set the target alight");
			Imbued imbued = player(server).getMainHandItem().get(WildercordComponents.IMBUED);
			check(imbued != null && imbued.charges() == 2, "a strike should spend one charge");
			husk.discard();
			player(server).setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		});
		imbueRules(context, world);
		// Any block holds magic: a plank imbued in hand, placed, is a glyph; broken by its maker, it comes back still imbued.
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS));
			Spellbooks.setReadyAt(player, 0, 0);
			SpellCaster.edit(player, 0, List.of());
			SpellCaster.edit(player, 0, ids(Runes.SELF, Runes.IMBUE, Runes.FROST));
			SpellCaster.cast(player, 0);
			ItemStack plank = player.getMainHandItem();
			check(plank.has(WildercordComponents.IMBUED) && Imbued.release(plank) == Imbued.Release.PLACE, "a block item in hand should take the magic");
			BlockPos under = BlockPos.containing(at.add(2, -1, 0));
			player.gameMode.useItemOn(player, level, plank, InteractionHand.MAIN_HAND,
				new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(under).add(0, 0.5, 0), net.minecraft.core.Direction.UP, under, false));
			BlockPos placed = under.above();
			check(level.getBlockState(placed).is(net.minecraft.world.level.block.Blocks.OAK_PLANKS), "the imbued plank should be placed");
			check(Imbuing.Glyphs.of(level).at(placed).map(Imbuing.Glyph::charges).orElse(0) == 3, "a placed imbued block should become a glyph with its charges");
			player.setGameMode(GameType.SURVIVAL);
			player.gameMode.destroyBlock(placed);
			player.setGameMode(GameType.CREATIVE);
		});
		context.waitTicks(3);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			BlockPos placed = BlockPos.containing(at.add(2, -1, 0)).above();
			boolean kept = !level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(placed).inflate(1.5),
				e -> e.getItem().is(Items.OAK_PLANKS) && e.getItem().has(WildercordComponents.IMBUED)).isEmpty();
			check(kept, "breaking your own glyph should give the block back still imbued");
			check(Imbuing.Glyphs.of(level).at(placed).isEmpty(), "the glyph goes with its block");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		});
		world.getServer().runCommand("kill @e[type=item]");
		// Glyphs share their maker's imbued cooldown: the Heal sword's release runs out first.
		context.waitTicks(40);
		// A glyph: Touch Imbue Frost on the ground ahead, then a husk steps onto it.
		Vec3 glyphAt = stage.add(-10, 0, -20);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			place(player, glyphAt, 0, 50);
			SpellCaster.edit(player, 1, List.of());
			SpellCaster.edit(player, 1, ids(Runes.TOUCH, Runes.IMBUE, Runes.FROST));
			Spellbooks.setReadyAt(player, 1, 0);
			SpellCaster.cast(player, 1);
		});
		BlockPos glyph = world.getServer().computeOnServer(server -> {
			List<Imbuing.Glyph> mine = Imbuing.Glyphs.of(player(server).level()).all().stream()
				.filter(g -> g.owner().equals(player(server).getUUID())).toList();
			return mine.isEmpty() ? null : mine.getLast().pos();
		});
		check(glyph != null, "Touch Imbue Frost on a block should write a glyph there");
		Vec3 top = Vec3.atBottomCenterOf(glyph.above());
		director(context, world, top.add(-2.4, 1.6, -2.2), top.add(0, 0.2, 0));
		context.waitTicks(12);
		shot(context, "imbue_glyph");
		world.getServer().runOnServer(server -> {
			ServerLevel level = player(server).level();
			Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
			husk.snapTo(top.x, top.y, top.z, 200, 0);
			husk.setNoAi(true);
			husk.addTag("wildercord.tour");
			level.addFreshEntity(husk);
		});
		// A frame every tick as it goes off (for tools/make_gif.py), the fifth kept as the still.
		for (int i = 0; i < 18; i++) {
			context.waitTicks(1);
			shot(context, String.format(java.util.Locale.ROOT, "gif_glyph_%02d", i));
			if (i == 4) {
				shot(context, "imbue_glyph_fires");
			}
		}
		cut(context);
		world.getServer().runOnServer(server -> {
			Mob husk = tourHusk(server);
			check(husk != null && husk.getHealth() < husk.getMaxHealth() && husk.getTicksFrozen() > 0, "a husk stepping on a Frost glyph should be frozen");
			int charges = Imbuing.Glyphs.of(player(server).level()).at(glyph).map(Imbuing.Glyph::charges).orElse(0);
			// It re-arms after a second, so a husk left standing on it through the burst may spend more than one.
			check(charges < SpellNumbers.IMBUE_CHARGES, "the glyph should have spent a charge, has " + charges);
		});
		world.getServer().runCommand("kill @e[tag=wildercord.tour]");
		world.getServer().runCommand("setblock " + glyph.getX() + " " + glyph.getY() + " " + glyph.getZ() + " grass_block");
		context.waitTicks(10);
	}

	// ------------------------------------------------------------------ a domain clash

	private static void clash(ClientGameTestContext context, TestSingleplayerContext world) {
		camera(context, CameraType.THIRD_PERSON_BACK);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			Vec3 at = stage.add(0, 0, -12);
			place(player, at, 0, -6);
			Mob witch = EntityTypes.WITCH.create(level, EntitySpawnReason.COMMAND);
			Vec3 spot = at.add(0, 0, 15);
			witch.snapTo(spot.x, spot.y, spot.z, 180, 0);
			witch.setNoAi(true);
			witch.addTag("wildercord.tour");
			level.addFreshEntity(witch);
			Runebound.bind(witch, List.of(Runes.DOMAIN, Runes.CHILL), false);
			Runebound.cast(level, witch, List.of(Runes.DOMAIN, Runes.CHILL), 0.8);
			SpellCaster.edit(player, 0, List.of());
			SpellCaster.edit(player, 0, ids(Runes.DOMAIN, Runes.HARM, Runes.VOW_MOD));
			Spellbooks.setReadyAt(player, 0, 0);
		});
		context.waitTicks(20);
		world.getServer().runOnServer(server -> SpellCaster.cast(player(server), 0));
		context.waitTicks(18);
		shot(context, "domain_clash");
		context.waitTicks(30);
		shot(context, "domain_shattered");
		world.getServer().runOnServer(server -> check(Heart.discovered(player(server), "feat:" + Feats.CLASH), "the stronger Domain should win the clash"));
		context.waitTicks(150);
		world.getServer().runCommand("kill @e[tag=wildercord.tour]");
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
	}

	// ------------------------------------------------------------------ overcasting and the innate rune

	private static void overcast(ClientGameTestContext context, TestSingleplayerContext world) {
		camera(context, CameraType.THIRD_PERSON_FRONT);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.SURVIVAL);
			place(player, stage, 180, 0);
			Spellbooks.setMana(player, 2);
			Spellbooks.setReadyAt(player, 2, 0);
			SpellCaster.cast(player, 2);
			check(Heart.cracked(player) == 0, "the first press only asks");
			SpellCaster.cast(player, 2);
			check(Heart.cracked(player) == 1, "the second press overcasts and cracks a circle");
			check(Heart.active(player) == 5, "a cracked circle stops counting");
		});
		context.waitTicks(4);
		shot(context, "overcast");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setAttached(WildercordAttachments.CRACKS, WildercordAttachments.Cracks.NONE);
			player.setGameMode(GameType.CREATIVE);
		});
		context.waitTicks(20);
	}

	private static void innate(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setAttached(WildercordAttachments.INNATE, "");
			player.setAttached(WildercordAttachments.CIRCLES, 0);
			dev.wildercord.cast.HeartCircles.form(player);
		});
		context.waitTicks(70);
		shot(context, "innate_awaken");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			check(!Heart.innate(player).isEmpty(), "the 1st Circle awakens an innate rune");
			check(Spellbooks.knows(player, Heart.innate(player)), "and it's learned");
			player.setAttached(WildercordAttachments.CIRCLES, 6);
		});
		context.waitTicks(40);
	}

	// ------------------------------------------------------------------ ley lines and the Wellstone

	private static void leyAndWellstone(ClientGameTestContext context, TestSingleplayerContext world) {
		camera(context, CameraType.FIRST_PERSON);
		Vec3 found = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			long seed = LeyWalker.seed(level);
			for (int r = 16; r < 900; r += 8) {
				for (int k = 0; k < 64; k++) {
					double a = Math.PI * 2 * k / 64;
					double x = player.getX() + Math.cos(a) * r;
					double z = player.getZ() + Math.sin(a) * r;
					if (LeyLines.strength(seed, x, z) > 0.85) {
						return new Vec3(x, 0, z);
					}
				}
			}
			return null;
		});
		check(found != null, "there should be a ley line within 900 blocks");
		// Go there and let the chunks load before touching the ground.
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
			player.level().getChunk(BlockPos.containing(found));
			place(player, ground(player.level(), found.x, found.z).add(0, 20, 0), 0, 60);
		});
		context.waitTicks(80);
		int[] ground = world.getServer().computeOnServer(server -> {
			// The block nearby where the line runs strongest: a narrow line can miss a rounded-off point.
			long seed = LeyWalker.seed(player(server).level());
			double bestX = found.x;
			double bestZ = found.z;
			double best = -1;
			for (int dx = -4; dx <= 4; dx++) {
				for (int dz = -4; dz <= 4; dz++) {
					double x = Math.floor(found.x) + dx + 0.5;
					double z = Math.floor(found.z) + dz + 0.5;
					double s = LeyLines.strength(seed, x, z);
					if (s > best) {
						best = s;
						bestX = x;
						bestZ = z;
					}
				}
			}
			Vec3 at = ground(player(server).level(), bestX, bestZ);
			return new int[] {(int) Math.floor(at.x), (int) at.y - 1, (int) Math.floor(at.z)};
		});
		clear(world, ground[0], ground[1], ground[2], 16);
		Vec3 heart = new Vec3(ground[0] + 0.5, ground[1] + 1, ground[2] + 0.5);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			long seed = LeyWalker.seed(level);
			// The Wellstone goes on the line, a few steps along it; the player stands on the line too.
			// The player always stands on the line's heart; the Wellstone goes where the line runs
			// strongest 2 to 5 blocks away (along the line, so it wakes).
			BlockPos centre = BlockPos.containing(heart);
			BlockPos well = centre.offset(3, 0, 0);
			double best = -1;
			for (int dx = -5; dx <= 5; dx++) {
				for (int dz = -5; dz <= 5; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					if (d < 2 || d > 5) {
						continue;
					}
					double s = LeyLines.strength(seed, centre.getX() + dx + 0.5, centre.getZ() + dz + 0.5);
					if (s > best) {
						best = s;
						well = centre.offset(dx, 0, dz);
					}
				}
			}
			level.setBlockAndUpdate(well, WildercordBlocks.WELLSTONE.defaultBlockState());
			Vec3 w = Vec3.atBottomCenterOf(well);
			Vec3 standAt = heart;
			Vec3 look = w.subtract(standAt);
			float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
			place(player, standAt, yaw, 25);
			player.getAbilities().flying = false;
			player.onUpdateAbilities();
		});
		context.waitTicks(80);
		shot(context, "wellstone");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			check(Heart.discovered(player, "feat:" + Feats.LEY_LINE), "standing on a ley line is noticed");
			check(player.getAttachedOrElse(WildercordAttachments.WELL_UNTIL, 0L) > server.overworld().getGameTime(), "an awake Wellstone boosts people near it");
		});
	}

	// ------------------------------------------------------------------ the Archive and the Archivist

	private static void archive(ClientGameTestContext context, TestSingleplayerContext world) {
		// Go there first: /place only builds in loaded chunks.
		BlockPos origin = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
			ServerLevel level = player.level();
			// Dry ground: an entrance at the edge of a lake films badly.
			BlockPos to = null;
			for (int k = 0; k < 12 && to == null; k++) {
				double a = k * Math.PI / 6;
				BlockPos candidate = BlockPos.containing(player.getX() + Math.cos(a) * 300, 0, player.getZ() + Math.sin(a) * 300);
				if (dry(level, candidate)) {
					to = candidate;
				}
			}
			if (to == null) {
				to = BlockPos.containing(player.getX() + 300, 0, player.getZ());
			}
			int y = level.getChunk(to).getHeight(Heightmap.Types.MOTION_BLOCKING, to.getX() & 15, to.getZ() & 15);
			place(player, new Vec3(to.getX() + 0.5, y + 30, to.getZ() + 0.5), 0, 60);
			return new BlockPos(to.getX(), y, to.getZ());
		});
		context.waitTicks(100);
		world.getServer().runCommand("place structure wildercord:archive " + origin.getX() + " " + origin.getY() + " " + origin.getZ());
		context.waitTicks(20);
		// Over the mouth of the stairway: its highest step.
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			BlockPos mouth = null;
			for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-100, -30, -100), origin.offset(100, 12, 100))) {
				if (level.isLoaded(pos) && level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.DEEPSLATE_BRICK_STAIRS)
						&& (mouth == null || pos.getY() > mouth.getY())) {
					mouth = pos.immutable();
				}
			}
			if (mouth != null) {
				Vec3 look = Vec3.atCenterOf(mouth);
				Vec3 eye = look.add(13, 15, -13);
				Vec3 d = look.subtract(eye);
				float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
				float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
				place(player, eye.subtract(0, player.getEyeHeight(), 0), yaw, pitch);
			}
		});
		context.waitTicks(40);
		shot(context, "archive_from_above");
		// Find its heart: the lectern.
		BlockPos lectern = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			for (int dx = -100; dx <= 100; dx++) {
				for (int dz = -100; dz <= 100; dz++) {
					for (int y = level.getMinY() + 1; y < origin.getY() + 10; y++) {
						BlockPos pos = new BlockPos(origin.getX() + dx, y, origin.getZ() + dz);
						if (level.isLoaded(pos) && level.getBlockState(pos).is(WildercordBlocks.ARCHIVE_LECTERN)) {
							return pos;
						}
					}
				}
			}
			return null;
		});
		check(lectern != null, "the Archive should generate, with its lectern");
		// All three seal doors are there (the first two were once walled over by their rooms).
		int seals = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			int n = 0;
			for (BlockPos pos : BlockPos.betweenClosed(lectern.offset(-96, -8, -96), lectern.offset(96, 8, 96))) {
				if (level.getBlockState(pos).is(WildercordBlocks.RUNE_SEAL)) {
					n++;
				}
			}
			return n;
		});
		check(seals >= 60, "the Archive should have all three seal doors (found " + seals + " seal blocks)");
		// A seal door, from a few steps back.
		BlockPos seal = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			BlockPos best = null;
			for (BlockPos pos : BlockPos.betweenClosed(lectern.offset(-96, -8, -96), lectern.offset(96, 8, 96))) {
				if (level.getBlockState(pos).is(WildercordBlocks.RUNE_SEAL) && (best == null || pos.distSqr(lectern) > best.distSqr(lectern))) {
					best = pos.immutable();
				}
			}
			return best;
		});
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 6000, 0, false, false));
			if (seal != null) {
				ServerLevel level = player.level();
				// The door lies across one axis; stand in front of it on the lectern's side... or the other.
				boolean alongX = level.getBlockState(seal.east()).is(WildercordBlocks.RUNE_SEAL) || level.getBlockState(seal.west()).is(WildercordBlocks.RUNE_SEAL);
				for (int side : new int[] {-1, 1}) {
					BlockPos stand = alongX ? seal.offset(0, 0, side * 5) : seal.offset(side * 5, 0, 0);
					BlockPos feet = stand;
					while (!level.getBlockState(feet.below()).isSolid() && feet.getY() > seal.getY() - 6) {
						feet = feet.below();
					}
					if (level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()) {
						float yaw = alongX ? (side < 0 ? 0 : 180) : (side < 0 ? -90 : 90);
						place(player, Vec3.atBottomCenterOf(feet), yaw, 0);
						break;
					}
				}
			}
		});
		context.waitTicks(20);
		shot(context, "archive_seal_door");
		// The arena, from its edge.
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Vec3 center = Vec3.atBottomCenterOf(lectern);
			for (int k = 0; k < 8; k++) {
				double a = Math.PI * 2 * k / 8;
				Vec3 spot = center.add(Math.cos(a) * 9, 0, Math.sin(a) * 9);
				BlockPos feet = BlockPos.containing(spot);
				if (player.level().getBlockState(feet).isAir() && player.level().getBlockState(feet.above()).isAir()) {
					float yaw = (float) Math.toDegrees(Math.atan2(-(center.x - spot.x), center.z - spot.z));
					place(player, spot, yaw, 10);
					break;
				}
			}
		});
		context.waitTicks(40);
		shot(context, "archive_arena");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.SURVIVAL);
			player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 2400, 4, false, false));
			player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 2400, 3, false, false));
		});
		world.getServer().waitFor(server -> {
			for (Archivist boss : player(server).level().getEntitiesOfClass(Archivist.class, player(server).getBoundingBox().inflate(40))) {
				if (boss.isCastingSpell()) {
					return true;
				}
			}
			return false;
		}, 400);
		context.waitTicks(12);
		shot(context, "archivist");
		Vec3 boss = world.getServer().computeOnServer(server -> {
			List<Archivist> found = player(server).level().getEntitiesOfClass(Archivist.class, player(server).getBoundingBox().inflate(40));
			return found.isEmpty() ? null : found.getFirst().position();
		});
		if (boss != null) {
			Vec3 player = world.getServer().computeOnServer(server -> player(server).position());
			Vec3 toward = player.subtract(boss).multiply(1, 0, 1).normalize();
			director(context, world, boss.add(toward.scale(4.2)).add(0, 2.4, 0), boss.add(0, 1.6, 0));
			context.waitTicks(4);
			shot(context, "archivist_close");
			cut(context);
		}
		world.getServer().runOnServer(server -> {
			ServerLevel level = player(server).level();
			check(!level.getEntitiesOfClass(Archivist.class, player(server).getBoundingBox().inflate(40)).isEmpty(), "the Archivist wakes when a player comes near");
		});
	}
}
