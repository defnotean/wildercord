package dev.wildercord.gametest;

import dev.wildercord.client.CordScreen;
import dev.wildercord.config.Config;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.config.WildercordConfig;
import dev.wildercord.player.Heart;
import dev.wildercord.spell.FieldGuide;
import dev.wildercord.wildlife.Cinderfox;
import dev.wildercord.wildlife.Glimmerwing;
import dev.wildercord.wildlife.LumenStag;
import dev.wildercord.wildlife.MossbackTortoise;
import dev.wildercord.wildlife.Rimehare;
import dev.wildercord.wildlife.Skyray;
import dev.wildercord.wildlife.Wildlife;
import dev.wildercord.wildlife.WildlifeRules;
import dev.wildercord.wildlife.WildlifeSpawns;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Magical wildlife in a real world: every creature's spawn entries are in its biomes (and not elsewhere), its spawn
 * rule holds to the creatures config, and each does what it's for: a stag bolts from someone walking up, trusts a
 * calm sneaking player and sheds one antler a day for them, and curses whoever kills it; a cinderfox tames with rabbit,
 * sits, spark-bites a fire-weak creature harder and gives a tuft to the brush once a day; two tortoises fed melon raise
 * a baby, and one struck hides and takes less; a rimehare bolts unless berries are held out; a skyray climbs to its
 * cruise and sheds a membrane; glimmerwings find a lantern; and creatures met go into the Grimoire. Then each is
 * photographed close up by day and by night ({@code wildlife_<creature>_day} / {@code _night}, plus a few poses).
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and
 * {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordWildlifeTest implements FabricClientGameTest {
	/** The stage: a wide grass platform in the sky, its top at this height. */
	private static final int GROUND = 100;
	private static final Vec3 STAG = new Vec3(-30.5, GROUND, 0.5);
	private static final Vec3 TORTOISE = new Vec3(-15.5, GROUND, 0.5);
	private static final Vec3 FOX = new Vec3(0.5, GROUND, 0.5);
	private static final Vec3 HARE = new Vec3(15.5, GROUND, 0.5);
	private static final Vec3 MOTHS = new Vec3(30.5, GROUND, 0.5);
	private static final Vec3 SKY = new Vec3(0.5, GROUND + 4, 20.5);

	private final List<String> failures = new ArrayList<>();

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		Path config = Config.path();
		String before = read(config);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			context.runOnClient(mc -> {
				mc.getWindow().setWindowed(1600, 900);
				mc.options.guiScale().set(2);
				mc.resizeGui();
				mc.options.setCameraType(CameraType.FIRST_PERSON);
			});
			setUp(context, world);

			biomes(world);
			spawnRules(context, world, config, before);
			stag(context, world);
			cinderfox(context, world);
			tortoise(context, world);
			rimehare(context, world);
			skyray(context, world);
			glimmerwing(context, world);
			fieldGuide(context, world);

			photographs(context, world);
		} finally {
			write(config, before);
			context.runOnClient(mc -> {
				mc.setCameraEntity(mc.player);
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				if (mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
		}
		if (!failures.isEmpty()) {
			throw new AssertionError("Wildlife: " + String.join("; ", failures));
		}
	}

	private void check(boolean ok, String what) {
		if (!ok) {
			failures.add(what);
		}
	}

	// ------------------------------------------------------------------ the stage

	private static void setUp(ClientGameTestContext context, TestSingleplayerContext world) {
		var server = world.getServer();
		server.runCommand("gamerule spawn_mobs false");
		server.runCommand("gamerule advance_time false");
		server.runCommand("gamerule advance_weather false");
		server.runCommand("weather clear");
		server.runCommand("time set 6000");
		server.runCommand("difficulty peaceful");
		// A broad lawn in the sky, with a strip of each creature's own ground.
		server.runCommand("fill -48 " + (GROUND - 2) + " -32 48 " + (GROUND - 2) + " 32 minecraft:dirt");
		server.runCommand("fill -48 " + (GROUND - 1) + " -32 48 " + (GROUND - 1) + " 32 minecraft:grass_block");
		server.runCommand("fill -19 " + (GROUND - 1) + " -4 -12 " + (GROUND - 1) + " 4 minecraft:moss_block");
		server.runCommand("fill -4 " + (GROUND - 1) + " -4 4 " + (GROUND - 1) + " 4 minecraft:sand");
		server.runCommand("fill 11 " + (GROUND - 1) + " -4 19 " + (GROUND - 1) + " 4 minecraft:snow_block");
		server.runCommand("fill 11 " + GROUND + " -4 19 " + GROUND + " 4 minecraft:snow");
		// A little dressing round each spot, for the pictures.
		for (int[] p : new int[][] {{-33, -2}, {-28, -3}, {-26, 2}, {-34, 3}, {27, -2}, {33, 2}, {29, 3}, {34, -3}}) {
			server.runCommand("setblock " + p[0] + " " + GROUND + " " + p[1] + " minecraft:" + (p[0] < 0 ? "short_grass" : "oxeye_daisy"));
		}
		server.runCommand("setblock -27 " + GROUND + " -1 minecraft:fern");
		server.runCommand("setblock 32 " + GROUND + " -1 minecraft:cornflower");
		server.runCommand("setblock -3 " + GROUND + " -3 minecraft:dead_bush");
		server.runCommand("setblock 4 " + GROUND + " -4 minecraft:cactus");
		server.runCommand("setblock 30 " + GROUND + " -1 minecraft:oak_fence");
		server.runCommand("setblock 30 " + (GROUND + 1) + " -1 minecraft:lantern");
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			player.setGameMode(GameType.SURVIVAL);
			player.teleportTo(player.level(), 0.5, GROUND, 14.5, Set.<Relative>of(), 180, 10, false);
		});
		context.waitTicks(40);
	}

	// ------------------------------------------------------------------ spawning

	/** Every creature is in each of its biomes' spawn lists, in the right pool, and not in a biome that isn't its. */
	private void biomes(TestSingleplayerContext world) {
		List<String> problems = world.getServer().computeOnServer(server -> {
			List<String> out = new ArrayList<>();
			var registry = server.registryAccess().lookupOrThrow(Registries.BIOME);
			for (WildlifeRules.Kind kind : WildlifeRules.ALL) {
				EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.fromNamespaceAndPath("wildercord", kind.id()));
				for (String id : kind.biomes()) {
					var biome = registry.get(ResourceKey.create(Registries.BIOME, Identifier.parse(id)));
					if (biome.isEmpty()) {
						out.add(id + " isn't a biome");
						continue;
					}
					if (!spawns(biome.get().value(), kind, type)) {
						out.add(kind.id() + " isn't in " + id + "'s spawns");
					}
				}
				var desert = registry.get(ResourceKey.create(Registries.BIOME, Identifier.parse(kind.id().equals("cinderfox") ? "minecraft:ocean" : "minecraft:desert")));
				if (desert.isPresent() && spawns(desert.get().value(), kind, type)) {
					out.add(kind.id() + " spawns somewhere it doesn't belong");
				}
			}
			return out;
		});
		failures.addAll(problems);
	}

	private static boolean spawns(Biome biome, WildlifeRules.Kind kind, EntityType<?> type) {
		// In 26.3 a biome's spawns are one of its environment attributes.
		MobSpawnSettings settings = biome.getAttributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY);
		for (var entry : settings.getMobsToSpawn(WildlifeSpawns.category(kind)).unwrap()) {
			MobSpawnSettings.SpawnerData data = entry.value();
			if (data.type() == type) {
				return entry.weight() >= kind.weight();
			}
		}
		return false;
	}

	/**
	 * A natural spawn on the right ground passes the rule (a tortoise always, a stag a share of the time); the wrong ground
	 * or the creatures switch off (reloaded) stops it; a spawn egg's spawn is never held to it.
	 */
	private void spawnRules(ClientGameTestContext context, TestSingleplayerContext world, Path config, String before) {
		String result = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			BlockPos moss = BlockPos.containing(TORTOISE);
			BlockPos sand = BlockPos.containing(FOX);
			var random = level.getRandom();
			if (!SpawnPlacements.checkSpawnRules(Wildlife.MOSSBACK_TORTOISE, level, EntitySpawnReason.NATURAL, moss, random)) {
				return "a tortoise should be able to spawn on moss in daylight";
			}
			if (SpawnPlacements.checkSpawnRules(Wildlife.RIMEHARE, level, EntitySpawnReason.NATURAL, sand, random)) {
				return "a rimehare shouldn't spawn on sand";
			}
			int stags = 0;
			for (int i = 0; i < 300; i++) {
				if (SpawnPlacements.checkSpawnRules(Wildlife.LUMEN_STAG, level, EntitySpawnReason.NATURAL, BlockPos.containing(STAG), random)) {
					stags++;
				}
			}
			if (stags < 50 || stags > 160) {
				return "a stag should pass about a third of good spawn tries, passed " + stags + " of 300";
			}
			return null;
		});
		if (result != null) {
			failures.add(result);
		}
		String off = WildercordConfig.DEFAULTS.toJson().replaceFirst("(\"creatures\": \\{[^}]*?\"wildlife\": )true", "$1false");
		check(!off.equals(WildercordConfig.DEFAULTS.toJson()), "the default config should list creatures.wildlife");
		write(config, off);
		world.getServer().runCommand("wildercord reload");
		context.waitTicks(2);
		String switched = world.getServer().computeOnServer(server -> {
			if (Config.get().wildlife().enabled()) {
				return "/wildercord reload should read creatures.wildlife: false";
			}
			ServerLevel level = player(server).level();
			for (int i = 0; i < 20; i++) {
				if (SpawnPlacements.checkSpawnRules(Wildlife.MOSSBACK_TORTOISE, level, EntitySpawnReason.NATURAL, BlockPos.containing(TORTOISE), level.getRandom())) {
					return "with wildlife switched off, a tortoise still passed its spawn rule";
				}
			}
			if (!SpawnPlacements.checkSpawnRules(Wildlife.MOSSBACK_TORTOISE, level, EntitySpawnReason.SPAWN_ITEM_USE, BlockPos.containing(TORTOISE), level.getRandom())) {
				return "a spawn egg should work whatever the creatures config says";
			}
			return null;
		});
		if (switched != null) {
			failures.add(switched);
		}
		write(config, before);
		world.getServer().runCommand("wildercord reload");
		context.waitTicks(2);
	}

	// ------------------------------------------------------------------ the lumen stag

	private void stag(ClientGameTestContext context, TestSingleplayerContext world) {
		// Walked up to openly, it bolts.
		int bolter = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			put(player, STAG.add(0, 0, 8), 180);
			LumenStag stag = spawn(Wildlife.LUMEN_STAG, player.level(), STAG, 0, false);
			return stag.getId();
		});
		context.waitTicks(60);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			LumenStag stag = (LumenStag) player.level().getEntity(bolter);
			check(stag != null && stag.distanceTo(player) > 10.5, "a stag should bolt from a player walking up openly (distance "
				+ (stag == null ? "?" : String.format("%.1f", stag.distanceTo(player))) + ")");
			if (stag != null) {
				stag.discard();
			}
		});

		// A calm, sneaking player is watched, then trusted: it sheds an antler for them, once.
		context.getInput().holdKey(options -> options.keyShift);
		context.waitTicks(5);
		int trusting = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			put(player, STAG.add(0, 0, 2.6), 180);
			LumenStag stag = spawn(Wildlife.LUMEN_STAG, player.level(), STAG, 0, false);
			return stag.getId();
		});
		context.waitTicks(20);
		world.getServer().runOnServer(server -> {
			LumenStag stag = (LumenStag) player(server).level().getEntity(trusting);
			check(stag != null && stag.stance(player(server)) == WildlifeRules.Stance.TRUST, "a stag should trust a calm, sneaking player close by (stance "
				+ (stag == null ? "?" : stag.stance(player(server))) + ", sneaking " + player(server).isShiftKeyDown() + ")");
		});
		context.waitTicks(WildlifeRules.STAG_TRUST_TICKS + 30);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			LumenStag stag = (LumenStag) player.level().getEntity(trusting);
			// Shed at the player's feet, it's usually picked up at once.
			int antlers = count(player.level(), STAG, Wildlife.LUMEN_ANTLER) + player.getInventory().countItem(Wildlife.LUMEN_ANTLER);
			check(stag != null && stag.shedToday(), "a trusted stag should shed today's antler");
			check(antlers == 1, "a trusted stag should leave one antler, found " + antlers);
		});
		context.waitTicks(WildlifeRules.STAG_TRUST_TICKS + 30);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			int antlers = count(player.level(), STAG, Wildlife.LUMEN_ANTLER) + player.getInventory().countItem(Wildlife.LUMEN_ANTLER);
			check(antlers == 1, "a stag sheds once a day, found " + antlers + " antlers");
			LumenStag stag = (LumenStag) player.level().getEntity(trusting);
			if (stag != null) {
				// The next day it may shed again.
				check(stag.shed(player.level(), null) == null, "a stag that shed today shouldn't shed again");
			}
			clear(player.level(), STAG, 6);
		});
		context.getInput().releaseKey(options -> options.keyShift);
		context.waitTicks(5);

		// Killing one leaves nothing, and brings bad luck.
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			put(player, STAG.add(0, 0, 3), 180);
			LumenStag stag = spawn(Wildlife.LUMEN_STAG, player.level(), STAG, 0, true);
			LumenStag witness = spawn(Wildlife.LUMEN_STAG, player.level(), STAG.add(5, 0, 0), 0, true);
			stag.hurtServer(player.level(), player.damageSources().playerAttack(player), 1000);
			check(!stag.isAlive(), "a stag struck that hard should die");
			check(player.hasEffect(MobEffects.UNLUCK), "killing a lumen stag should bring Bad Luck");
			check(witness.frightened(), "a stag that sees another killed should be frightened");
			check(witness.stance(player) == WildlifeRules.Stance.FLEE, "a frightened stag trusts nobody");
			witness.discard();
			player.removeEffect(MobEffects.UNLUCK);
		});
		context.waitTicks(5);
		world.getServer().runOnServer(server -> {
			ServerLevel level = player(server).level();
			check(level.getEntitiesOfClass(ItemEntity.class, new AABB(BlockPos.containing(STAG)).inflate(4)).isEmpty(), "a dead stag should leave nothing");
		});
	}

	// ------------------------------------------------------------------ the cinderfox

	private void cinderfox(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			put(player, FOX.add(0, 0, 3), 180);
			Cinderfox fox = spawn(Wildlife.CINDERFOX, level, FOX, 0, true);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RABBIT, 64));
			for (int i = 0; i < 40 && !fox.isTame(); i++) {
				fox.mobInteract(player, InteractionHand.MAIN_HAND);
			}
			check(fox.isTame() && fox.isOwnedBy(player), "a cinderfox fed rabbit should be tamed within forty");
			check(fox.isOrderedToSit(), "a cinderfox just tamed should sit");

			// Its bite: a polar bear (weak to fire) catches and takes half again; a cow doesn't.
			LivingEntity bear = spawn(EntityTypes.POLAR_BEAR, level, FOX.add(1.5, 0, 0), 0, true);
			LivingEntity cow = spawn(EntityTypes.COW, level, FOX.add(-1.5, 0, 0), 0, true);
			float bearBefore = bear.getHealth();
			float cowBefore = cow.getHealth();
			fox.doHurtTarget(level, bear);
			fox.doHurtTarget(level, cow);
			check(Math.abs((bearBefore - bear.getHealth()) - 4.5F) < 0.01F, "a spark-bite should hurt a fire-weak polar bear 4.5, hurt it "
				+ (bearBefore - bear.getHealth()));
			check(bear.isOnFire(), "a spark-bite should set a fire-weak creature alight");
			check(Math.abs((cowBefore - cow.getHealth()) - 3F) < 0.01F && !cow.isOnFire(), "a cow should take an ordinary bite (took "
				+ (cowBefore - cow.getHealth()) + ")");
			bear.discard();
			cow.discard();

			// Brushed, once a day it gives a tuft.
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BRUSH));
			fox.mobInteract(player, InteractionHand.MAIN_HAND);
			fox.mobInteract(player, InteractionHand.MAIN_HAND);
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		});
		context.waitTicks(3);
		world.getServer().runOnServer(server -> {
			ServerLevel level = player(server).level();
			check(count(level, FOX, Wildlife.EMBER_TUFT) == 1, "brushing a tame cinderfox twice in a day should give one Ember Tuft, gave "
				+ count(level, FOX, Wildlife.EMBER_TUFT));
			clear(level, FOX, 6);
		});
	}

	// ------------------------------------------------------------------ the mossback tortoise

	private void tortoise(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			put(player, TORTOISE.add(0, 0, 4), 180);
			MossbackTortoise a = spawn(Wildlife.MOSSBACK_TORTOISE, level, TORTOISE.add(-0.9, 0, 0), 90, false);
			MossbackTortoise b = spawn(Wildlife.MOSSBACK_TORTOISE, level, TORTOISE.add(0.9, 0, 0), 270, false);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MELON_SLICE, 8));
			a.mobInteract(player, InteractionHand.MAIN_HAND);
			b.mobInteract(player, InteractionHand.MAIN_HAND);
			check(a.isInLove() && b.isInLove(), "two tortoises fed melon should fall in love");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		});
		boolean baby = false;
		for (int i = 0; i < 20 && !baby; i++) {
			context.waitTicks(20);
			baby = world.getServer().computeOnServer(server -> player(server).level().getEntitiesOfClass(MossbackTortoise.class,
				new AABB(BlockPos.containing(TORTOISE)).inflate(8), MossbackTortoise::isBaby).size() == 1);
		}
		check(baby, "two tortoises in love should raise one baby");
		world.getServer().runOnServer(server -> {
			ServerLevel level = player(server).level();
			clear(level, TORTOISE, 8);
			// Struck, it hides, and a second blow lands softly.
			MossbackTortoise shell = spawn(Wildlife.MOSSBACK_TORTOISE, level, TORTOISE, 0, true);
			float full = shell.getHealth();
			shell.hurtServer(level, level.damageSources().magic(), 10);
			check(shell.hidden(), "a struck tortoise should hide in its shell");
			shell.damageCooldownTime = 0;
			shell.hurtServer(level, level.damageSources().magic(), 10);
			float taken = full - shell.getHealth();
			check(Math.abs(taken - 14) < 0.01F, "in its shell a tortoise should take 40% (10 then 4), took " + taken);
			shell.dropScute(level);
		});
		context.waitTicks(3);
		world.getServer().runOnServer(server -> {
			ServerLevel level = player(server).level();
			check(count(level, TORTOISE, Wildlife.MOSSBACK_SCUTE) >= 1, "a tortoise should let a scute go");
			clear(level, TORTOISE, 8);
		});
	}

	// ------------------------------------------------------------------ the rimehare

	private void rimehare(ClientGameTestContext context, TestSingleplayerContext world) {
		int bolter = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			put(player, HARE.add(0, 0, 5), 180);
			return spawn(Wildlife.RIMEHARE, player.level(), HARE, 0, false).getId();
		});
		context.waitTicks(50);
		int tempted = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			Entity hare = player.level().getEntity(bolter);
			check(hare != null && hare.distanceTo(player) > 8, "a rimehare should bolt from a player who comes near (distance "
				+ (hare == null ? "?" : String.format("%.1f", hare.distanceTo(player))) + ")");
			if (hare != null) {
				hare.discard();
			}
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SWEET_BERRIES));
			return spawn(Wildlife.RIMEHARE, player.level(), HARE, 0, false).getId();
		});
		context.waitTicks(50);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Entity hare = player.level().getEntity(tempted);
			check(hare != null && hare.distanceTo(player) < 5.5, "a rimehare should stay for a player holding out sweet berries (distance "
				+ (hare == null ? "?" : String.format("%.1f", hare.distanceTo(player))) + ")");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			clear(player.level(), HARE, 16);
		});
	}

	// ------------------------------------------------------------------ the skyray

	private void skyray(ClientGameTestContext context, TestSingleplayerContext world) {
		int ray = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			put(player, new Vec3(0.5, GROUND, 14.5), 180);
			Skyray skyray = spawn(Wildlife.SKYRAY, player.level(), SKY.add(0, 8, 0), 0, false);
			skyray.setLoop(SKY, 10, 0.5);
			return skyray.getId();
		});
		context.waitTicks(200);
		world.getServer().runOnServer(server -> {
			ServerLevel level = player(server).level();
			Skyray skyray = (Skyray) level.getEntity(ray);
			check(skyray != null && skyray.getY() > GROUND + 15, "a skyray should climb toward its cruise (y "
				+ (skyray == null ? "?" : String.format("%.1f", skyray.getY())) + ")");
			if (skyray != null) {
				check(!skyray.onGround(), "a skyray never lands");
				ItemEntity membrane = skyray.shedMembrane(level);
				check(membrane.getItem().is(Wildlife.SKYRAY_MEMBRANE), "a skyray should shed a membrane");
				membrane.discard();
				skyray.discard();
			}
		});
	}

	// ------------------------------------------------------------------ the glimmerwing

	private void glimmerwing(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("time set 18000");
		int moth = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			put(player, MOTHS.add(0, 0, 9), 180);
			return spawn(Wildlife.GLIMMERWING, player.level(), MOTHS.add(6, 2, 0), 0, false).getId();
		});
		double nearest = Double.MAX_VALUE;
		for (int i = 0; i < 25 && nearest > 3.2; i++) {
			context.waitTicks(20);
			nearest = world.getServer().computeOnServer(server -> {
				Entity found = player(server).level().getEntity(moth);
				return found == null ? Double.MAX_VALUE : found.position().distanceTo(MOTHS.add(0, 1.5, -1));
			});
		}
		check(nearest <= 3.2, "a glimmerwing should find the lantern and circle it (nearest " + String.format("%.1f", nearest) + ")");
		world.getServer().runOnServer(server -> clear(player(server).level(), MOTHS, 12));
		world.getServer().runCommand("time set 6000");
	}

	// ------------------------------------------------------------------ the field guide

	private void fieldGuide(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			put(player, HARE.add(0, 0, 6), 180);
			spawn(Wildlife.RIMEHARE, player.level(), HARE, 0, true);
		});
		context.waitTicks(45);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			List<String> grimoire = Heart.grimoire(player);
			check(grimoire.contains(FieldGuide.key("wildercord:rimehare")), "a rimehare seen up close should go into the Grimoire's field guide");
			check(grimoire.contains(FieldGuide.key("wildercord:lumen_stag")), "the stags met earlier should be in the field guide");
			clear(player.level(), HARE, 8);
			Spellbooks.setCord(player, new ItemStack(WildercordItems.TWINE_CORD));
		});
		// The Grimoire's field guide: met creatures by name, the rest as hints.
		context.waitTicks(5);
		context.setScreen(CordScreen::new);
		context.waitTicks(5);
		context.runOnClient(mc -> {
			if (mc.gui.screen() instanceof CordScreen screen) {
				screen.showFieldGuide();
			}
			mc.gui.toastManager().clear();
		});
		context.getInput().setCursorPos(4, 4);
		context.waitTicks(5);
		context.takeScreenshot(TestScreenshotOptions.of("wildlife_field_guide").disableCounterPrefix());
		context.setScreen(() -> null);
		context.waitTicks(2);
	}

	// ------------------------------------------------------------------ pictures

	private void photographs(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.SPECTATOR);
			put(player, new Vec3(0.5, GROUND + 2, 12.5), 180);
			ServerLevel level = player.level();
			spawn(Wildlife.LUMEN_STAG, level, STAG, 88, true);
			MossbackTortoise tortoise = spawn(Wildlife.MOSSBACK_TORTOISE, level, TORTOISE, 95, true);
			tortoise.setGarden(WildlifeRules.Garden.SWAMP);
			spawn(Wildlife.CINDERFOX, level, FOX, 82, true);
			// On top of the snow, not in it.
			spawn(Wildlife.RIMEHARE, level, HARE.add(0, 0.125, 0), 85, true);
			for (int i = 0; i < 5; i++) {
				double a = i * 1.26;
				Glimmerwing moth = spawn(Wildlife.GLIMMERWING, level, MOTHS.add(Math.cos(a) * 0.9, 1.2 + 0.35 * Math.sin(a * 2), -1 + Math.sin(a) * 0.9),
					(float) Math.toDegrees(a) + 90, true);
				moth.setVariant(i % 3);
			}
			spawn(Wildlife.SKYRAY, level, SKY, 196, true);
		});
		context.waitTicks(20);
		for (String time : new String[] {"day", "night"}) {
			world.getServer().runCommand("time set " + (time.equals("day") ? 6000 : 18000));
			context.waitTicks(5);
			shot(context, world, "wildlife_lumen_stag_" + time, STAG.add(-2.6, 1.75, 3.0), STAG.add(0, 1.1, 0));
			shot(context, world, "wildlife_mossback_tortoise_" + time, TORTOISE.add(-2.0, 2.0, 2.5), TORTOISE.add(0, 0.5, 0));
			shot(context, world, "wildlife_cinderfox_" + time, FOX.add(-1.25, 0.8, 1.55), FOX.add(0, 0.4, 0));
			shot(context, world, "wildlife_rimehare_" + time, HARE.add(-1.0, 0.7, 1.2), HARE.add(0, 0.35, 0));
			shot(context, world, "wildlife_glimmerwing_" + time, MOTHS.add(-1.3, 2.0, 1.0), MOTHS.add(0, 1.3, -1));
			// From above and behind, its wings spread across the picture and the stars on its back.
			shot(context, world, "wildlife_skyray_" + time, SKY.add(-0.9, 2.5, 3.2), SKY.add(0, -0.1, -0.5));
		}

		// A few poses: the stag bowing over its shed antler, a tortoise in its shell, a cinderfox sitting at night.
		world.getServer().runOnServer(server -> {
			ServerLevel level = player(server).level();
			for (LumenStag stag : level.getEntitiesOfClass(LumenStag.class, new AABB(BlockPos.containing(STAG)).inflate(4))) {
				stag.shed(level, null);
				stag.setPoseState(LumenStag.GRAZING, 400);
			}
			for (MossbackTortoise tortoise : level.getEntitiesOfClass(MossbackTortoise.class, new AABB(BlockPos.containing(TORTOISE)).inflate(4))) {
				tortoise.setNoAi(false);
				tortoise.hurtServer(level, level.damageSources().magic(), 1);
				tortoise.setNoAi(true);
			}
			for (Cinderfox fox : level.getEntitiesOfClass(Cinderfox.class, new AABB(BlockPos.containing(FOX)).inflate(4))) {
				fox.setOrderedToSit(true);
				fox.setInSittingPose(true);
			}
		});
		context.waitTicks(25);
		shot(context, world, "wildlife_lumen_stag_grazing_night", STAG.add(-2.6, 1.75, 3.0), STAG.add(0, 0.9, 0));
		shot(context, world, "wildlife_mossback_tortoise_hidden_night", TORTOISE.add(-2.0, 2.0, 2.5), TORTOISE.add(0, 0.4, 0));
		shot(context, world, "wildlife_cinderfox_sitting_night", FOX.add(-1.25, 0.8, 1.55), FOX.add(0, 0.42, 0));
		world.getServer().runCommand("time set 6000");
		context.waitTicks(5);
		// The way most will first see one: from below, against the sky.
		shot(context, world, "wildlife_skyray_below_day", SKY.add(-1.7, -3.0, 2.2), SKY.add(0, 0, 0));
		shot(context, world, "wildlife_cinderfox_sitting_day", FOX.add(-1.25, 0.8, 1.55), FOX.add(0, 0.42, 0));
		shot(context, world, "wildlife_lumen_stag_grazing_day", STAG.add(-2.6, 1.75, 3.0), STAG.add(0, 0.9, 0));
		cut(context);
	}

	private static void shot(ClientGameTestContext context, TestSingleplayerContext world, String name, Vec3 eye, Vec3 target) {
		director(context, world, eye, target);
		context.waitTicks(4);
		context.runOnClient(mc -> mc.gui.toastManager().clear());
		context.waitTicks(1);
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	/** Films from a fixed point: the client looks through an invisible marker placed there (as the feature tour does). */
	private static void director(ClientGameTestContext context, TestSingleplayerContext world, Vec3 eye, Vec3 target) {
		int id = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			level.getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class, player(server).getBoundingBox().inflate(128),
				e -> e.entityTags().contains("wildercord.camera")).forEach(Entity::discard);
			net.minecraft.world.entity.Display.TextDisplay camera = EntityTypes.TEXT_DISPLAY.create(level, EntitySpawnReason.COMMAND);
			Vec3 d = target.subtract(eye);
			float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
			float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
			camera.snapTo(eye.x, eye.y, eye.z, yaw, pitch);
			camera.addTag("wildercord.camera");
			camera.addTag("wildercord.rolled");
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
	}

	private static void cut(ClientGameTestContext context) {
		context.runOnClient(mc -> {
			mc.setCameraEntity(mc.player);
			if (mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
	}

	// ------------------------------------------------------------------ helpers

	private static <T extends Mob> T spawn(EntityType<T> type, ServerLevel level, Vec3 at, float yaw, boolean still) {
		T mob = type.create(level, EntitySpawnReason.COMMAND);
		if (mob == null) {
			throw new AssertionError("couldn't create " + type);
		}
		mob.snapTo(at.x, at.y, at.z, yaw, 0);
		mob.setYHeadRot(yaw);
		mob.setYBodyRot(yaw);
		mob.setNoAi(still);
		mob.setPersistenceRequired();
		level.addFreshEntity(mob);
		return mob;
	}

	private static void put(ServerPlayer player, Vec3 at, float yaw) {
		player.teleportTo(player.level(), at.x, at.y, at.z, Set.<Relative>of(), yaw, 10, false);
	}

	private static int count(ServerLevel level, Vec3 near, Item item) {
		return level.getEntitiesOfClass(ItemEntity.class, new AABB(BlockPos.containing(near)).inflate(6), e -> e.getItem().is(item)).stream()
			.mapToInt(e -> e.getItem().getCount()).sum();
	}

	/** Clears creatures and dropped items round a spot. */
	private static void clear(ServerLevel level, Vec3 near, int radius) {
		AABB box = new AABB(BlockPos.containing(near)).inflate(radius);
		level.getEntitiesOfClass(Entity.class, box, e -> e instanceof ItemEntity || Wildlife.TYPES.contains(e.getType())
			|| e.getType() == EntityTypes.POLAR_BEAR || e.getType() == EntityTypes.COW).forEach(Entity::discard);
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static String read(Path path) {
		try {
			return Files.exists(path) ? Files.readString(path, StandardCharsets.UTF_8) : null;
		} catch (IOException e) {
			throw new AssertionError("couldn't read " + path, e);
		}
	}

	private static void write(Path path, String text) {
		try {
			if (text == null) {
				Files.deleteIfExists(path);
				return;
			}
			Files.createDirectories(path.getParent());
			Files.writeString(path, text, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new AssertionError("couldn't write " + path, e);
		}
	}
}
