package dev.wildercord.gametest;

import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Runebound;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.config.Config;
import dev.wildercord.content.WildercordEffects;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.monster.BogBubble;
import dev.wildercord.monster.BogWitchFrog;
import dev.wildercord.monster.Bramblewalker;
import dev.wildercord.monster.GeodeCrawler;
import dev.wildercord.monster.Gloomstalker;
import dev.wildercord.monster.ManaOoze;
import dev.wildercord.monster.MonsterContent;
import dev.wildercord.monster.MonsterMagic;
import dev.wildercord.monster.MonsterSpawns;
import dev.wildercord.monster.ThunderwingHarpy;
import dev.wildercord.monster.WildMonster;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * The six monsters of the wilds in a real world: each is spawned and its signature tried for real. A Bramblewalker's lash
 * roots the player and fire sends it running; a Gloomstalker hides in the dark, shows under glowing and a fire spell, and
 * crouches and pounces; a Thunderwing Harpy shrieks and dives, calls lightning that lands through the spell defences
 * (softer under the Potion of Warding, and held by the spellguard), and falls under an earth spell; a Geode Crawler curls
 * up, shrugs off a plain blow, cracks under a pickaxe and a shock, and rolls; a Bog Witch-Frog swallows a chicken and
 * spits a bubble of poison that lands on the player, and a bubble struck in the air pops harmlessly; a Mana Ooze drinks
 * spells, grows, splits when overfed and burns under fire. Then their biome spawn entries and spawn rules are checked
 * (and the server switch), each rolls Runebound with a spell that compiles, and Peaceful sends them all away.
 *
 * <p>Every monster is filmed close up from a fixed camera (portraits in each pose, and live moments mid-attack):
 * screenshots go to build/run/clientGameTest/screenshots/monster_*.</p>
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordMonstersTest implements FabricClientGameTest {
	/** Where everything happens: the player's first footing, the stages laid out east of it. */
	private static Vec3 base;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			context.runOnClient(mc -> {
				mc.getWindow().setWindowed(1600, 900);
				mc.options.guiScale().set(2);
				mc.resizeGui();
			});
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule natural_regeneration false");
			setup(world);
			context.waitTicks(10);
			List<String> failures = new ArrayList<>();
			section(failures, "spawns", () -> spawns(world));
			section(failures, "Bramblewalker", () -> bramblewalker(context, world));
			section(failures, "Gloomstalker", () -> gloomstalker(context, world));
			section(failures, "Thunderwing Harpy", () -> harpy(context, world));
			section(failures, "Geode Crawler", () -> crawler(context, world));
			section(failures, "Bog Witch-Frog", () -> frog(context, world));
			section(failures, "Mana Ooze", () -> ooze(context, world));
			section(failures, "Runebound", () -> runebound(world));
			section(failures, "Peaceful", () -> peaceful(context, world));
			cut(context);
			if (!failures.isEmpty()) {
				throw new AssertionError("The monsters went wrong:\n  " + String.join("\n  ", failures));
			}
		}
	}

	// ------------------------------------------------------------------ helpers

	private interface Section {
		void run();
	}

	private static void section(List<String> failures, String name, Section section) {
		try {
			section.run();
		} catch (AssertionError | RuntimeException e) {
			failures.add(name + ": " + e.getMessage());
			dev.wildercord.Wildercord.LOGGER.error("Monsters test, {}", name, e);
		}
	}

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

	private static void shot(ClientGameTestContext context, String name) {
		context.takeScreenshot(TestScreenshotOptions.of("monster_" + name).disableCounterPrefix());
	}

	/** The middle of stage {@code i}: 40 blocks apart, east of where the player started. */
	private static Vec3 stage(int i) {
		return base.add(40 * i, 0, 0);
	}

	private static void place(ServerPlayer player, Vec3 at, float yaw, float pitch) {
		player.teleportTo(player.level(), at.x, at.y, at.z, Set.<Relative>of(), yaw, pitch, false);
	}

	/** Where to film a monster facing north from: ahead of it and off to one side, {@code distance} away and {@code height} up. */
	private static Vec3 threeQuarter(Vec3 spot, double distance, double height) {
		double a = Math.toRadians(215);
		return spot.add(-Math.sin(a) * distance, height, Math.cos(a) * distance);
	}

	private static float yaw(Vec3 from, Vec3 to) {
		Vec3 d = to.subtract(from);
		return (float) Math.toDegrees(Math.atan2(-d.x, d.z));
	}

	private static float pitch(Vec3 from, Vec3 to) {
		Vec3 d = to.subtract(from);
		return (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
	}

	private static void setup(TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.CREATIVE);
			base = Vec3.atBottomCenterOf(player.blockPosition());
			Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
			Spellbook book = Spellbooks.get(player).withStarterGiven();
			for (RuneDef rune : Runes.all()) {
				book = book.learn(rune.id());
			}
			Spellbooks.set(player, book.withSelected(0));
			SpellCaster.edit(player, 0, ids(Runes.BEAM, Runes.HARM));
			SpellCaster.edit(player, 1, ids(Runes.BEAM, Runes.FIRE));
			SpellCaster.edit(player, 2, ids(Runes.BEAM, Runes.SHOCK));
			SpellCaster.edit(player, 3, ids(Runes.BEAM, Runes.ROOT));
		});
	}

	/** Casts one of the player's beams from {@code from} at a point, cooldown and mana reset. */
	private static void castAt(TestSingleplayerContext world, int spell, Vec3 from, Vec3 at) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Vec3 eye = from.add(0, player.getEyeHeight(), 0);
			place(player, from, yaw(eye, at), pitch(eye, at));
			Spellbooks.setReadyAt(player, spell, 0);
			Spellbooks.setMana(player, Mana.max(player));
			SpellCaster.cast(player, spell);
		});
	}

	private static void heal(TestSingleplayerContext world, GameType mode) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(mode);
			player.removeAllEffects();
			player.clearFire();
			player.setHealth(player.getMaxHealth());
			player.getFoodData().setFoodLevel(20);
		});
	}

	/** A monster at {@code at}, facing {@code yaw}, never a random Runebound (tests pick their monsters), kept from despawning. */
	private static <T extends Mob> T spawn(ServerLevel level, EntityType<T> type, Vec3 at, float yaw, boolean ai) {
		T mob = type.create(level, EntitySpawnReason.COMMAND);
		mob.snapTo(at.x, at.y, at.z, yaw, 0);
		mob.setYHeadRot(yaw);
		mob.setYBodyRot(yaw);
		mob.addTag("wildercord.rolled");
		mob.setPersistenceRequired();
		mob.setNoAi(!ai);
		level.addFreshEntity(mob);
		return mob;
	}

	@SuppressWarnings("unchecked")
	private static <T extends Entity> T entity(TestSingleplayerContext world, int id, Class<T> type) {
		return world.getServer().computeOnServer(server -> {
			Entity e = player(server).level().getEntity(id);
			return type.isInstance(e) ? (T) e : null;
		});
	}

	/** Waits up to {@code ticks} for {@code test} (asked on the server each tick); returns whether it came true. */
	private static boolean await(ClientGameTestContext context, TestSingleplayerContext world, int ticks, Function<MinecraftServer, Boolean> test) {
		for (int i = 0; i < ticks; i++) {
			if (world.getServer().computeOnServer(test::apply)) {
				return true;
			}
			context.waitTicks(1);
		}
		return world.getServer().computeOnServer(test::apply);
	}

	/** Lays a stage: a floor of {@code floor} 15 blocks across round {@code centre}, cleared above it. */
	private static void build(TestSingleplayerContext world, Vec3 centre, String floor) {
		BlockPos c = BlockPos.containing(centre);
		world.getServer().runCommand(String.format("fill %d %d %d %d %d %d air", c.getX() - 7, c.getY(), c.getZ() - 4, c.getX() + 7, c.getY() + 12, c.getZ() + 14));
		world.getServer().runCommand(String.format("fill %d %d %d %d %d %d %s", c.getX() - 7, c.getY() - 1, c.getZ() - 4, c.getX() + 7, c.getY() - 1, c.getZ() + 14, floor));
	}

	/** Films from a fixed point: the client looks through an invisible marker placed there. */
	private static void director(ClientGameTestContext context, TestSingleplayerContext world, Vec3 eye, Vec3 target) {
		int id = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			level.getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class, new AABB(base, base).inflate(400),
				e -> e.entityTags().contains("wildercord.camera")).forEach(Entity::discard);
			net.minecraft.world.entity.Display.TextDisplay camera = EntityTypes.TEXT_DISPLAY.create(level, EntitySpawnReason.COMMAND);
			camera.snapTo(eye.x, eye.y, eye.z, yaw(eye, target), pitch(eye, target));
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
				mc.gui.toastManager().clear();
			}
		});
	}

	private static void cut(ClientGameTestContext context) {
		context.runOnClient(mc -> {
			mc.setCameraEntity(mc.player);
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			if (mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
	}

	/** A portrait of a still monster in each of {@code poses} (flags held), filmed from {@code eye}. */
	private static void portraits(ClientGameTestContext context, TestSingleplayerContext world, int id, Vec3 eye, Vec3 look, String name, int[] poses,
			String[] names) {
		director(context, world, eye, look);
		for (int i = 0; i < poses.length; i++) {
			int flags = poses[i];
			world.getServer().runOnServer(server -> {
				if (player(server).level().getEntity(id) instanceof WildMonster monster) {
					monster.holdPose(flags);
				}
			});
			context.waitTicks(i == 0 ? 20 : 14);
			shot(context, name + names[i]);
		}
	}

	private static void clear(TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerLevel level = player(server).level();
			for (Entity e : level.getEntities((Entity) null, new AABB(base, base).inflate(400), e -> e instanceof WildMonster || e instanceof ManaOoze
					|| e instanceof BogBubble || e instanceof Chicken || e instanceof net.minecraft.world.entity.AreaEffectCloud)) {
				e.discard();
			}
		});
	}

	// ------------------------------------------------------------------ spawns

	private static void spawns(TestSingleplayerContext world) {
		String failure = world.getServer().computeOnServer(server -> {
			Registry<Biome> biomes = server.registryAccess().lookupOrThrow(Registries.BIOME);
			Function<ResourceKey<Biome>, Holder<Biome>> biome = key -> biomes.getOrThrow(key);
			Object[][] expected = {
				{Biomes.DARK_FOREST, MonsterContent.BRAMBLEWALKER, true}, {Biomes.FOREST, MonsterContent.BRAMBLEWALKER, true},
				{Biomes.PLAINS, MonsterContent.BRAMBLEWALKER, false}, {Biomes.DESERT, MonsterContent.BRAMBLEWALKER, false},
				{Biomes.DARK_FOREST, MonsterContent.GLOOMSTALKER, true}, {Biomes.PLAINS, MonsterContent.GLOOMSTALKER, true},
				{Biomes.JAGGED_PEAKS, MonsterContent.THUNDERWING_HARPY, true}, {Biomes.WINDSWEPT_HILLS, MonsterContent.THUNDERWING_HARPY, true},
				{Biomes.PLAINS, MonsterContent.THUNDERWING_HARPY, false},
				{Biomes.PLAINS, MonsterContent.GEODE_CRAWLER, true}, {Biomes.DRIPSTONE_CAVES, MonsterContent.GEODE_CRAWLER, true},
				{Biomes.SWAMP, MonsterContent.BOG_WITCH_FROG, true}, {Biomes.MANGROVE_SWAMP, MonsterContent.BOG_WITCH_FROG, true},
				{Biomes.FOREST, MonsterContent.BOG_WITCH_FROG, false},
				{Biomes.PLAINS, MonsterContent.MANA_OOZE, true}, {Biomes.LUSH_CAVES, MonsterContent.MANA_OOZE, true},
				{Biomes.DEEP_DARK, MonsterContent.MANA_OOZE, false}, {Biomes.DEEP_DARK, MonsterContent.GLOOMSTALKER, false},
				{Biomes.MUSHROOM_FIELDS, MonsterContent.GEODE_CRAWLER, false}};
			for (Object[] row : expected) {
				@SuppressWarnings("unchecked")
				ResourceKey<Biome> key = (ResourceKey<Biome>) row[0];
				@SuppressWarnings("unchecked")
				EntityType<? extends Mob> type = (EntityType<? extends Mob>) row[1];
				boolean in = MonsterSpawns.spawnsIn(biome.apply(key), type);
				if (in != (Boolean) row[2]) {
					return EntityType.getKey(type) + (in ? " shouldn't" : " should") + " be among " + key.identifier() + "'s monsters (it has "
						+ MonsterSpawns.monstersIn(biome.apply(key)) + ")";
				}
			}
			for (EntityType<? extends Mob> type : MonsterContent.types()) {
				if (SpawnPlacements.getPlacementType(type) != SpawnPlacementTypes.ON_GROUND) {
					return EntityType.getKey(type) + " should have its spawn rule registered";
				}
				if (type.isAllowedInPeaceful()) {
					return EntityType.getKey(type) + " shouldn't be allowed on Peaceful";
				}
			}
			ServerLevel level = player(server).level();
			BlockPos low = BlockPos.containing(base);
			// A harpy keeps to the peaks: never down here, whatever the light.
			if (SpawnPlacements.checkSpawnRules(MonsterContent.THUNDERWING_HARPY, level, EntitySpawnReason.NATURAL, low, level.getRandom())) {
				return "a harpy shouldn't spawn at y " + low.getY();
			}
			// Daylight on open ground: no Gloomstalker, no Bramblewalker.
			for (int i = 0; i < 20; i++) {
				if (SpawnPlacements.checkSpawnRules(MonsterContent.BRAMBLEWALKER, level, EntitySpawnReason.NATURAL, low, level.getRandom())) {
					return "a Bramblewalker shouldn't spawn in broad daylight";
				}
			}
			return null;
		});
		check(failure == null, failure);

		// The server's switch, read back at once: a creature switched off never spawns on its own.
		java.nio.file.Path path = Config.path();
		String before;
		try {
			before = Files.readString(path, StandardCharsets.UTF_8);
			com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(before).getAsJsonObject();
			json.getAsJsonObject("monsters").addProperty("bramblewalker", false);
			Files.writeString(path, json.toString(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new AssertionError("couldn't change the config: " + e);
		}
		try {
			String off = world.getServer().computeOnServer(server -> {
				Config.reload(server);
				if (Config.get().monsters().spawns("bramblewalker")) {
					return "the switch should have stopped Bramblewalkers";
				}
				if (!Config.get().monsters().spawns("gloomstalker")) {
					return "switching one off shouldn't stop the others";
				}
				ServerLevel level = player(server).level();
				return SpawnPlacements.checkSpawnRules(MonsterContent.BRAMBLEWALKER, level, EntitySpawnReason.NATURAL, BlockPos.containing(base),
					level.getRandom()) ? "a Bramblewalker switched off shouldn't pass its spawn rule" : null;
			});
			check(off == null, off);
		} finally {
			try {
				Files.writeString(path, before, StandardCharsets.UTF_8);
			} catch (Exception e) {
				throw new AssertionError("couldn't put the config back: " + e);
			}
			world.getServer().runOnServer(Config::reload);
		}
	}

	// ------------------------------------------------------------------ the Bramblewalker

	private static void bramblewalker(ClientGameTestContext context, TestSingleplayerContext world) {
		Vec3 at = stage(0);
		build(world, at, "grass_block");
		// A grove behind it.
		BlockPos c = BlockPos.containing(at);
		world.getServer().runCommand("place feature minecraft:oak %d %d %d".formatted(c.getX() - 4, c.getY(), c.getZ() + 11));
		world.getServer().runCommand("place feature minecraft:fancy_oak %d %d %d".formatted(c.getX() + 5, c.getY(), c.getZ() + 12));
		Vec3 spot = at.add(0, 0, 6);
		Vec3 eye = threeQuarter(spot, 4.0, 2.0);
		int id = world.getServer().computeOnServer(server -> spawn(player(server).level(), MonsterContent.BRAMBLEWALKER, spot, 180, false).getId());
		portraits(context, world, id, eye, spot.add(0, 1.1, 0), "bramblewalker", new int[] {0, WildMonster.WINDUP, WildMonster.ACTING},
			new String[] {"", "_rear", "_lash"});

		// For real: it rears, and its vine roots the player where they stand.
		heal(world, GameType.SURVIVAL);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			place(player, at, 0, 10);
			if (player.level().getEntity(id) instanceof Bramblewalker walker) {
				walker.holdPose(0);
				walker.setNoAi(false);
				walker.setTarget(player);
			}
		});
		director(context, world, at.add(-6, 3.2, 2.5), at.add(0, 1.0, 3.5));
		check(await(context, world, 100, server -> player(server).level().getEntity(id) instanceof Bramblewalker w && w.windingUp()),
			"a Bramblewalker within its vine's reach should rear for the lash");
		context.waitTicks(8);
		shot(context, "bramblewalker_rear_live");
		check(await(context, world, 20, server -> player(server).level().getEntity(id) instanceof Bramblewalker w && w.state(WildMonster.ACTING)),
			"its lash should crack");
		shot(context, "bramblewalker_lash_live");
		check(await(context, world, 10, server -> MonsterMagic.rooted(player(server))), "its lash should root the player");
		float hurt = world.getServer().computeOnServer(server -> player(server).getHealth());
		check(hurt < 20, "the lash should sting (health " + hurt + ")");

		// Fire: it panics and runs.
		double near = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.CREATIVE);
			Bramblewalker walker = (Bramblewalker) player.level().getEntity(id);
			walker.setTarget(null);
			walker.igniteForSeconds(5);
			return walker.distanceTo(player);
		});
		context.waitTicks(40);
		String running = world.getServer().computeOnServer(server -> {
			Bramblewalker walker = (Bramblewalker) player(server).level().getEntity(id);
			if (walker == null || !walker.isAlive()) {
				return null;
			}
			if (!walker.fleeing()) {
				return "a burning Bramblewalker should be fleeing";
			}
			double far = walker.distanceTo(player(server));
			return far > near + 1.5 ? null : "a burning Bramblewalker should run away (from " + String.format("%.1f", near) + " to " + String.format("%.1f", far) + ")";
		});
		director(context, world, at.add(-8, 5, -4), at.add(0, 0.5, 4));
		shot(context, "bramblewalker_burning");
		check(running == null, running);
		clear(world);
	}

	// ------------------------------------------------------------------ the Gloomstalker

	private static void gloomstalker(ClientGameTestContext context, TestSingleplayerContext world) {
		Vec3 at = stage(1);
		world.getServer().runCommand("time set 18000");
		build(world, at, "podzol");
		BlockPos c = BlockPos.containing(at);
		world.getServer().runCommand("place feature minecraft:dark_oak %d %d %d".formatted(c.getX() - 5, c.getY(), c.getZ() + 11));
		world.getServer().runCommand("place feature minecraft:dark_oak %d %d %d".formatted(c.getX() + 4, c.getY(), c.getZ() + 12));
		Vec3 spot = at.add(0, 0, 6);
		Vec3 eye = threeQuarter(spot, 2.9, 1.2);
		heal(world, GameType.CREATIVE);
		world.getServer().runOnServer(server -> place(player(server), at.add(0, 0, -3), 0, 0));
		int id = world.getServer().computeOnServer(server -> spawn(player(server).level(), MonsterContent.GLOOMSTALKER, spot, 180, false).getId());
		portraits(context, world, id, eye, spot.add(0, 0.5, 0), "gloomstalker", new int[] {0, WildMonster.WINDUP, WildMonster.ACTING, WildMonster.STUNNED,
			WildMonster.VEILED}, new String[] {"", "_crouch", "_leap", "_sprawl", "_hidden"});

		// For real: in the dark, with nobody close, it hides.
		world.getServer().runOnServer(server -> {
			Gloomstalker stalker = (Gloomstalker) player(server).level().getEntity(id);
			stalker.holdPose(0);
			stalker.setNoAi(false);
			place(player(server), at.add(0, 0, -3), 0, 0);
		});
		check(await(context, world, 40, server -> ((Gloomstalker) player(server).level().getEntity(id)).veiled()),
			"a Gloomstalker in the dark, with nobody close, should hide");
		context.waitTicks(25);
		shot(context, "gloomstalker_hidden_live");
		// Glowing shows it, and only while it lasts.
		world.getServer().runOnServer(server -> ((Gloomstalker) player(server).level().getEntity(id)).addEffect(new MobEffectInstance(MobEffects.GLOWING, 60)));
		check(await(context, world, 12, server -> !((Gloomstalker) player(server).level().getEntity(id)).veiled()), "glowing should show a Gloomstalker");
		world.getServer().runOnServer(server -> ((Gloomstalker) player(server).level().getEntity(id)).removeEffect(MobEffects.GLOWING));
		check(await(context, world, 16, server -> ((Gloomstalker) player(server).level().getEntity(id)).veiled()),
			"with the glow gone, it should hide again");
		// A fire spell lays it bare for a while.
		Vec3 stalkerAt = world.getServer().computeOnServer(server -> player(server).level().getEntity(id).getBoundingBox().getCenter());
		castAt(world, 1, at.add(0, 0, -3), stalkerAt);
		check(await(context, world, 10, server -> !((Gloomstalker) player(server).level().getEntity(id)).veiled()), "a fire spell should reveal it");
		context.waitTicks(60);
		boolean still = world.getServer().computeOnServer(server -> !((Gloomstalker) player(server).level().getEntity(id)).veiled());
		check(still, "a light spell should keep it in view for seconds, not a moment");
		shot(context, "gloomstalker_revealed");

		// For real: it crouches (eyes flaring), pounces, and slinks off.
		heal(world, GameType.SURVIVAL);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			place(player, at, 0, 0);
			Gloomstalker stalker = (Gloomstalker) player.level().getEntity(id);
			stalker.clearFire();
			stalker.setHealth(stalker.getMaxHealth());
			stalker.setTarget(player);
		});
		director(context, world, at.add(-7, 4.5, -5), at.add(0, 0.4, 2));
		boolean crouched = await(context, world, 200, server -> {
			Gloomstalker stalker = (Gloomstalker) player(server).level().getEntity(id);
			return stalker != null && stalker.crouching();
		});
		check(crouched, "a hunting Gloomstalker should crouch to pounce");
		context.waitTicks(6);
		shot(context, "gloomstalker_crouch_live");
		check(await(context, world, 20, server -> ((Gloomstalker) player(server).level().getEntity(id)).pouncing()), "after crouching it should pounce");
		context.waitTicks(3);
		shot(context, "gloomstalker_pounce_live");
		clear(world);
		world.getServer().runCommand("time set 6000");
	}

	// ------------------------------------------------------------------ the Thunderwing Harpy

	private static void harpy(ClientGameTestContext context, TestSingleplayerContext world) {
		Vec3 at = stage(2);
		BlockPos c = BlockPos.containing(at);
		build(world, at, "stone");
		// A crag behind it.
		world.getServer().runCommand("fill %d %d %d %d %d %d stone".formatted(c.getX() - 6, c.getY(), c.getZ() + 12, c.getX() + 6, c.getY() + 4, c.getZ() + 14));
		world.getServer().runCommand("fill %d %d %d %d %d %d snow_block".formatted(c.getX() - 6, c.getY() + 5, c.getZ() + 13, c.getX() + 6, c.getY() + 5,
			c.getZ() + 14));
		Vec3 spot = at.add(0, 2.0, 6);
		Vec3 eye = threeQuarter(spot, 3.8, 1.0);
		int id = world.getServer().computeOnServer(server -> spawn(player(server).level(), MonsterContent.THUNDERWING_HARPY, spot, 180, false).getId());
		portraits(context, world, id, eye, spot.add(0, 0.6, 0), "thunderwing_harpy", new int[] {0, WildMonster.WINDUP, WildMonster.ACTING, WildMonster.ALT},
			new String[] {"", "_shriek", "_dive", "_call"});

		// Earth drags it down.
		world.getServer().runOnServer(server -> ((WildMonster) player(server).level().getEntity(id)).holdPose(0));
		Vec3 harpyAt = world.getServer().computeOnServer(server -> player(server).level().getEntity(id).getBoundingBox().getCenter());
		castAt(world, 3, at, harpyAt);
		check(await(context, world, 6, server -> ((ThunderwingHarpy) player(server).level().getEntity(id)).grounded()), "an earth spell should ground a harpy");
		context.waitTicks(20);
		shot(context, "thunderwing_harpy_grounded");

		// For real: it shrieks, and dives.
		heal(world, GameType.SURVIVAL);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			place(player, at, 0, 0);
			ThunderwingHarpy harpy = (ThunderwingHarpy) player.level().getEntity(id);
			harpy.discard();
		});
		int live = world.getServer().computeOnServer(server -> {
			ThunderwingHarpy harpy = spawn(player(server).level(), MonsterContent.THUNDERWING_HARPY, at.add(3, 7, 5), 180, true);
			harpy.setTarget(player(server));
			harpy.shriekNow(player(server).level());
			return harpy.getId();
		});
		director(context, world, at.add(-8, 4, -3), at.add(1, 3.5, 3));
		context.waitTicks(10);
		shot(context, "thunderwing_harpy_shriek_live");
		check(await(context, world, 20, server -> ((ThunderwingHarpy) player(server).level().getEntity(live)).diving()), "after its shriek it should dive");
		context.waitTicks(3);
		shot(context, "thunderwing_harpy_dive_live");
		check(await(context, world, 40, server -> !((ThunderwingHarpy) player(server).level().getEntity(live)).diving()), "a dive should end");
		boolean landed = world.getServer().computeOnServer(server -> player(server).getHealth() < 20
			|| ((ThunderwingHarpy) player(server).level().getEntity(live)).grounded());
		check(landed, "a dive should either strike the player or leave the harpy grounded");

		// Its lightning lands through the spell defences: softer under the Potion of Warding.
		float plain = lightning(context, world, live, false);
		float warded = lightning(context, world, live, true);
		check(plain > 0, "a harpy's lightning should hurt a player standing on its mark");
		check(warded < plain - 0.2F, "the Potion of Warding should soften a harpy's lightning (" + plain + " plain, " + warded + " warded)");
		// And the spellguard holds against it: no single spell of theirs takes a player from full health to dead.
		heal(world, GameType.SURVIVAL);
		String guard = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ThunderwingHarpy harpy = (ThunderwingHarpy) player.level().getEntity(live);
			MonsterMagic.hurt(player.level(), harpy, player, 200);
			return player.isAlive() && player.getHealth() <= 2.5F ? null
				: "the spellguard should leave a full-health player on one heart (health " + player.getHealth() + ", alive " + player.isAlive() + ")";
		});
		check(guard == null, guard);
		clear(world);
	}

	/** Lets a harpy call lightning on the player where they stand; returns how much health it took. */
	private static float lightning(ClientGameTestContext context, TestSingleplayerContext world, int id, boolean warded) {
		heal(world, GameType.SURVIVAL);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			if (warded) {
				player.addEffect(new MobEffectInstance(WildercordEffects.WARDED, 600, 1));
			}
			ThunderwingHarpy harpy = (ThunderwingHarpy) player.level().getEntity(id);
			Effects.readyToHurt(player);
			harpy.callNow(player.level(), player);
		});
		context.waitTicks(4);
		if (!warded) {
			shot(context, "thunderwing_harpy_lightning_mark");
		}
		context.waitTicks(28);
		if (!warded) {
			shot(context, "thunderwing_harpy_lightning");
		}
		return world.getServer().computeOnServer(server -> player(server).getMaxHealth() - player(server).getHealth());
	}

	// ------------------------------------------------------------------ the Geode Crawler

	private static void crawler(ClientGameTestContext context, TestSingleplayerContext world) {
		Vec3 at = stage(3);
		BlockPos c = BlockPos.containing(at);
		build(world, at, "deepslate");
		world.getServer().runCommand("fill %d %d %d %d %d %d calcite".formatted(c.getX() - 6, c.getY(), c.getZ() + 12, c.getX() + 6, c.getY() + 3, c.getZ() + 13));
		world.getServer().runCommand("fill %d %d %d %d %d %d amethyst_block".formatted(c.getX() - 4, c.getY(), c.getZ() + 11, c.getX() + 4, c.getY() + 2,
			c.getZ() + 11));
		world.getServer().runCommand("setblock %d %d %d amethyst_cluster".formatted(c.getX() - 2, c.getY() + 3, c.getZ() + 11));
		world.getServer().runCommand("setblock %d %d %d large_amethyst_bud".formatted(c.getX() + 2, c.getY() + 3, c.getZ() + 11));
		Vec3 spot = at.add(0, 0, 6);
		Vec3 eye = threeQuarter(spot, 2.8, 1.4);
		int id = world.getServer().computeOnServer(server -> spawn(player(server).level(), MonsterContent.GEODE_CRAWLER, spot, 180, false).getId());
		portraits(context, world, id, eye, spot.add(0, 0.4, 0), "geode_crawler", new int[] {0, WildMonster.GUARD, WildMonster.GUARD | WildMonster.WINDUP,
			WildMonster.STUNNED}, new String[] {"", "_curled", "_rattle", "_dazed"});

		// Struck, it curls up; curled, a plain blow barely scratches it; a pickaxe cracks it open.
		heal(world, GameType.CREATIVE);
		String curl = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			GeodeCrawler crawler = (GeodeCrawler) level.getEntity(id);
			crawler.holdPose(0);
			crawler.setNoAi(false);
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			crawler.hurtServer(level, player.damageSources().playerAttack(player), 3);
			if (!crawler.curled()) {
				return "a struck Geode Crawler should curl up";
			}
			Effects.readyToHurt(crawler);
			float before = crawler.getHealth();
			crawler.hurtServer(level, player.damageSources().playerAttack(player), 10);
			float plain = before - crawler.getHealth();
			if (plain <= 0 || plain > 3) {
				return "curled, a plain blow of 10 should do little (it did " + plain + ")";
			}
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
			Effects.readyToHurt(crawler);
			before = crawler.getHealth();
			crawler.hurtServer(level, player.damageSources().playerAttack(player), 10);
			float cracked = before - crawler.getHealth();
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			if (cracked < 8) {
				return "a pickaxe should crack it for full damage (it did " + cracked + ")";
			}
			return crawler.dazed() ? null : "a cracked Geode Crawler should be dazed";
		});
		check(curl == null, curl);
		// A shock cracks it too.
		int second = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			level.getEntity(id).discard();
			GeodeCrawler crawler = spawn(level, MonsterContent.GEODE_CRAWLER, spot, 180, true);
			crawler.curl(level, 200);
			return crawler.getId();
		});
		context.waitTicks(2);
		Vec3 shellAt = world.getServer().computeOnServer(server -> player(server).level().getEntity(second).getBoundingBox().getCenter());
		castAt(world, 2, at, shellAt);
		check(await(context, world, 6, server -> ((GeodeCrawler) player(server).level().getEntity(second)).dazed()), "a shock should crack a curled crawler");

		// For real: hit, it curls, rattles and rolls at the player.
		heal(world, GameType.SURVIVAL);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			level.getEntity(second).discard();
			place(player, at, 0, 0);
			GeodeCrawler crawler = spawn(level, MonsterContent.GEODE_CRAWLER, at.add(0, 0, 7), 180, true);
			crawler.setTarget(player);
			crawler.curl(level, 10);
		});
		director(context, world, at.add(-6, 2.8, 0), at.add(0, 0.3, 4));
		check(await(context, world, 50, server -> !level(server).getEntitiesOfClass(GeodeCrawler.class, new AABB(at, at).inflate(20), GeodeCrawler::rolling)
			.isEmpty()), "a curled crawler with a target should rattle, then roll at it");
		context.waitTicks(4);
		shot(context, "geode_crawler_roll_live");
		clear(world);
	}

	private static ServerLevel level(MinecraftServer server) {
		return player(server).level();
	}

	// ------------------------------------------------------------------ the Bog Witch-Frog

	private static void frog(ClientGameTestContext context, TestSingleplayerContext world) {
		Vec3 at = stage(4);
		BlockPos c = BlockPos.containing(at);
		build(world, at, "mud");
		world.getServer().runCommand("fill %d %d %d %d %d %d water".formatted(c.getX() - 6, c.getY() - 1, c.getZ() + 9, c.getX() + 6, c.getY() - 1, c.getZ() + 14));
		world.getServer().runCommand("fill %d %d %d %d %d %d lily_pad replace air".formatted(c.getX() - 5, c.getY(), c.getZ() + 10, c.getX() - 3, c.getY(),
			c.getZ() + 11));
		world.getServer().runCommand("fill %d %d %d %d %d %d lily_pad replace air".formatted(c.getX() + 3, c.getY(), c.getZ() + 12, c.getX() + 4, c.getY(),
			c.getZ() + 12));
		world.getServer().runCommand("place feature minecraft:mangrove %d %d %d".formatted(c.getX() + 5, c.getY(), c.getZ() + 8));
		Vec3 spot = at.add(0, 0, 5);
		Vec3 eye = threeQuarter(spot, 3.3, 1.6);
		int id = world.getServer().computeOnServer(server -> spawn(player(server).level(), MonsterContent.BOG_WITCH_FROG, spot, 180, false).getId());
		portraits(context, world, id, eye, spot.add(0, 0.6, 0), "bog_witch_frog", new int[] {0, WildMonster.WINDUP, WildMonster.ALT, WildMonster.GUARD},
			new String[] {"", "_swell", "_mouth", "_gulp"});

		// It swallows a chicken whole, and its next bubble is fatter.
		heal(world, GameType.CREATIVE);
		int chicken = world.getServer().computeOnServer(server -> {
			ServerLevel level = level(server);
			((BogWitchFrog) level.getEntity(id)).holdPose(0);
			((BogWitchFrog) level.getEntity(id)).setNoAi(false);
			Chicken snack = EntityTypes.CHICKEN.create(level, EntitySpawnReason.COMMAND);
			snack.snapTo(spot.x + 3, spot.y, spot.z, 0, 0);
			snack.setNoAi(true);
			level.addFreshEntity(snack);
			return snack.getId();
		});
		check(await(context, world, 120, server -> level(server).getEntity(chicken) == null && ((BogWitchFrog) level(server).getEntity(id)).engorged()),
			"a Bog Witch-Frog should snatch and swallow a chicken nearby");
		shot(context, "bog_witch_frog_gulp_live");

		// For real: its throat swells, it lobs a bubble of poison, and the bubble bursts on the player.
		heal(world, GameType.SURVIVAL);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			place(player, at.add(0, 0, -4), 0, 0);
			((BogWitchFrog) level(server).getEntity(id)).setTarget(player);
		});
		director(context, world, at.add(-7, 3.5, -2), at.add(0, 1.2, 1));
		check(await(context, world, 80, server -> ((BogWitchFrog) level(server).getEntity(id)).swelling()), "a frog with a target in range should swell to spit");
		context.waitTicks(10);
		shot(context, "bog_witch_frog_swell_live");
		check(await(context, world, 20, server -> !level(server).getEntitiesOfClass(BogBubble.class, new AABB(at, at).inflate(20)).isEmpty()),
			"after swelling it should spit a bubble");
		context.waitTicks(6);
		shot(context, "bog_bubble_flight");
		check(await(context, world, 50, server -> level(server).getEntitiesOfClass(BogBubble.class, new AABB(at, at).inflate(20)).isEmpty()),
			"the bubble should burst");
		shot(context, "bog_bubble_burst");
		String poisoned = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			return player.hasEffect(MobEffects.POISON) && player.getHealth() < player.getMaxHealth() ? null
				: "the bubble should poison and hurt the player it lands on (health " + player.getHealth() + ", poisoned " + player.hasEffect(MobEffects.POISON) + ")";
		});
		check(poisoned == null, poisoned);

		// A bubble struck in the air pops harmlessly.
		boolean popped = world.getServer().computeOnServer(server -> {
			ServerLevel level = level(server);
			BogBubble bubble = new BogBubble(MonsterContent.BOG_BUBBLE, level);
			bubble.setOwner(level.getEntity(id));
			bubble.setPos(at.x + 4, at.y + 4, at.z);
			bubble.setNoGravity(true);
			level.addFreshEntity(bubble);
			bubble.hurtServer(level, level.damageSources().generic(), 1);
			return bubble.isRemoved();
		});
		check(popped, "a bubble struck in the air should pop");
		clear(world);
	}

	// ------------------------------------------------------------------ the Mana Ooze

	private static void ooze(ClientGameTestContext context, TestSingleplayerContext world) {
		Vec3 at = stage(5);
		BlockPos c = BlockPos.containing(at);
		world.getServer().runCommand("time set 18000");
		build(world, at, "deepslate_tiles");
		world.getServer().runCommand("fill %d %d %d %d %d %d deepslate".formatted(c.getX() - 6, c.getY(), c.getZ() + 11, c.getX() + 6, c.getY() + 4, c.getZ() + 12));
		Vec3 spot = at.add(0, 0, 5);
		Vec3 eye = threeQuarter(spot, 2.8, 1.3);
		heal(world, GameType.CREATIVE);
		int portrait = world.getServer().computeOnServer(server -> {
			ManaOoze ooze = spawn(level(server), MonsterContent.MANA_OOZE, spot, 180, false);
			ooze.setSize(3, true);
			return ooze.getId();
		});
		director(context, world, eye, spot.add(0, 0.6, 0));
		context.waitTicks(20);
		shot(context, "mana_ooze");
		world.getServer().runOnServer(server -> ((ManaOoze) level(server).getEntity(portrait)).drink(level(server), 21, null));
		context.waitTicks(4);
		shot(context, "mana_ooze_full");

		// It drinks spells (no harm, fuller), and grows; fire burns it.
		int id = world.getServer().computeOnServer(server -> {
			level(server).getEntity(portrait).discard();
			ManaOoze ooze = spawn(level(server), MonsterContent.MANA_OOZE, spot, 180, true);
			ooze.setSize(2, true);
			return ooze.getId();
		});
		Vec3 heart = world.getServer().computeOnServer(server -> level(server).getEntity(id).getBoundingBox().getCenter());
		float health = world.getServer().computeOnServer(server -> ((ManaOoze) level(server).getEntity(id)).getHealth());
		castAt(world, 0, at, heart);
		context.waitTicks(3);
		String drank = world.getServer().computeOnServer(server -> {
			ManaOoze ooze = (ManaOoze) level(server).getEntity(id);
			if (ooze.getHealth() < health) {
				return "a spell should sink into a Mana Ooze without harming it (health " + health + " -> " + ooze.getHealth() + ")";
			}
			return ooze.fullness() > 0 || ooze.getSize() > 2 ? null : "a spell should fill a Mana Ooze";
		});
		check(drank == null, drank);
		shot(context, "mana_ooze_drinks");
		for (int i = 0; i < 8 && world.getServer().computeOnServer(server -> ((ManaOoze) level(server).getEntity(id)).getSize() < 3); i++) {
			Vec3 now = world.getServer().computeOnServer(server -> level(server).getEntity(id).getBoundingBox().getCenter());
			castAt(world, 0, at, now);
			context.waitTicks(3);
		}
		int size = world.getServer().computeOnServer(server -> ((ManaOoze) level(server).getEntity(id)).getSize());
		check(size >= 3, "fed enough spells, a Mana Ooze should grow (size " + size + ")");
		shot(context, "mana_ooze_grown");
		float before = world.getServer().computeOnServer(server -> ((ManaOoze) level(server).getEntity(id)).getHealth());
		Vec3 now = world.getServer().computeOnServer(server -> level(server).getEntity(id).getBoundingBox().getCenter());
		castAt(world, 1, at, now);
		context.waitTicks(3);
		float after = world.getServer().computeOnServer(server -> level(server).getEntity(id) instanceof ManaOoze o ? o.getHealth() : 0.0F);
		check(after < before, "fire should burn a Mana Ooze, never be drunk (health " + before + " -> " + after + ")");

		// Overfed at its biggest, it bursts into two.
		String split = world.getServer().computeOnServer(server -> {
			ServerLevel level = level(server);
			ManaOoze ooze = (ManaOoze) level.getEntity(id);
			if (ooze == null) {
				return "the ooze burned away before it could be overfed";
			}
			ooze.clearFire();
			ooze.setSize(4, true);
			ooze.drink(level, dev.wildercord.monster.MonsterRules.oozeCapacity(4) - 0.5F, null);
			ooze.drink(level, 2, null);
			List<ManaOoze> left = level.getEntitiesOfClass(ManaOoze.class, new AABB(at, at).inflate(20));
			if (!ooze.isRemoved()) {
				return "overfed at its biggest, a Mana Ooze should burst";
			}
			return left.size() == 2 && left.stream().allMatch(o -> o.getSize() == 2) ? null
				: "it should burst into two halves (there are " + left.size() + ")";
		});
		check(split == null, split);
		context.waitTicks(4);
		shot(context, "mana_ooze_split");
		clear(world);
		world.getServer().runCommand("time set 6000");
	}

	// ------------------------------------------------------------------ Runebound

	private static void runebound(TestSingleplayerContext world) {
		String failure = world.getServer().computeOnServer(server -> {
			ServerLevel level = level(server);
			for (EntityType<? extends Mob> type : MonsterContent.types()) {
				Mob mob = spawn(level, type, base.add(0, 0, -8), 0, false);
				Runebound.bind(mob, false);
				List<RuneDef> spell = Runebound.spellOf(mob);
				mob.discard();
				if (spell.isEmpty()) {
					return EntityType.getKey(type) + " should roll Runebound with a spell";
				}
				if (SpellCompiler.compile(spell).isEmpty()) {
					return EntityType.getKey(type) + "'s Runebound spell " + spell + " should compile";
				}
			}
			return null;
		});
		check(failure == null, failure);
	}

	// ------------------------------------------------------------------ Peaceful

	private static void peaceful(ClientGameTestContext context, TestSingleplayerContext world) {
		List<Integer> ids = world.getServer().computeOnServer(server -> {
			List<Integer> out = new ArrayList<>();
			int i = 0;
			for (EntityType<? extends Mob> type : MonsterContent.types()) {
				out.add(spawn(level(server), type, base.add(-6 + 2.5 * i++, 0, -6), 0, true).getId());
			}
			return out;
		});
		context.waitTicks(5);
		world.getServer().runCommand("difficulty peaceful");
		context.waitTicks(10);
		String failure = world.getServer().computeOnServer(server -> {
			ServerLevel level = level(server);
			for (int id : ids) {
				Entity e = level.getEntity(id);
				if (e != null && !e.isRemoved()) {
					return EntityType.getKey(e.getType()) + " should leave on Peaceful";
				}
			}
			for (EntityType<? extends Mob> type : MonsterContent.types()) {
				if (SpawnPlacements.checkSpawnRules(type, level, EntitySpawnReason.NATURAL, BlockPos.containing(base), level.getRandom())) {
					return EntityType.getKey(type) + " shouldn't pass its spawn rule on Peaceful";
				}
			}
			return null;
		});
		world.getServer().runCommand("difficulty normal");
		check(failure == null, failure);
	}
}
