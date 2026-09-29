package dev.wildercord.gametest;

import dev.wildercord.cast.Cast;
import dev.wildercord.cast.Climate;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.Runebound;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.config.Config;
import dev.wildercord.config.WildercordConfig;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.runesmith.ContractRules;
import dev.wildercord.runesmith.Contracts;
import dev.wildercord.spell.Bestiary;
import dev.wildercord.spell.ClimateRules;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Creature affinities and elemental climate, in a real world: frost hits a blaze half as hard again as
 * a husk and shows "Weak!" over it, fire is resisted by a hoglin, can't touch a blaze and a snow golem
 * shrugs off frost without a flinch, a Shatter breaks through a resistance, a Runebound resists its own
 * element, and the Bestiary writes each down. Then the climate: fire burns hotter in the Nether and void
 * bites harder in the End (the HUD is told, and filmed), and the config's two switches turn it all off.
 *
 * <p>Effects are applied straight to the creature ({@link Effects#apply}, where every shape's hit ends),
 * so each number is exact. Every creature has 200 health, so none dies mid-check, and is marked never to
 * roll as a Runebound. Runs in the full suite; {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY}
 * and {@code WILDERCORD_SHOWCASE} skip it.</p>
 */
public class WildercordAffinitiesTest implements FabricClientGameTest {
	/** The platform in the Overworld: high enough that no terrain gets in the way (a superflat plains world, at noon). */
	private static final BlockPos STAGE = new BlockPos(0, 180, 0);
	/** A room carved out of the Nether, and a platform over the End's void, far from the dragon. */
	private static final BlockPos NETHER_STAGE = new BlockPos(0, 100, 0);
	private static final BlockPos END_STAGE = new BlockPos(400, 100, 400);
	private static final String TAG = "wildercord.affinities";

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule natural_regeneration false");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			stage(world);
			context.waitTicks(10);
			List<String> failures = new ArrayList<>();
			failures.addAll(on(world, WildercordAffinitiesTest::weakness));
			cleanup(context, world);
			failures.addAll(immuneContracts(context, world));
			cleanup(context, world);
			failures.addAll(on(world, WildercordAffinitiesTest::resistance));
			cleanup(context, world);
			failures.addAll(climate(context, world));
			cleanup(context, world);
			failures.addAll(switchedOff(context, world));
			cleanup(context, world);
			if (!failures.isEmpty()) {
				throw new AssertionError("Creature affinities and climate went wrong:\n  " + String.join("\n  ", failures));
			}
		}
	}

	// ------------------------------------------------------------------ affinities

	/** Frost on a blaze (weak to it) against frost on a husk (not): half as hard again, a "Weak!" over it, and the Bestiary knows. */
	private static List<String> weakness(ServerPlayer player) {
		List<String> out = new ArrayList<>();
		ServerLevel level = player.level();
		Vec3 base = Vec3.atBottomCenterOf(STAGE);
		LivingEntity husk = spawn(level, EntityTypes.HUSK, base.add(-2, 0, 3));
		LivingEntity blaze = spawn(level, EntityTypes.BLAZE, base.add(2, 0, 3));
		float plain = hit(player, husk, Runes.FROST);
		float weak = hit(player, blaze, Runes.FROST);
		if (plain <= 0) {
			return List.of("frost should hurt a husk (it took " + plain + ")");
		}
		double ratio = weak / plain;
		// Vanilla's fivefold freezing damage on blazes gives way to the table's +50%.
		if (ratio < 1.45 || ratio > 1.55) {
			out.add("frost should hit a blaze 50% harder than a husk (" + weak + " against " + plain + ", x" + ratio + ")");
		}
		boolean callout = level.getEntitiesOfClass(Display.TextDisplay.class, blaze.getBoundingBox().inflate(2, 3, 2),
			d -> d.getText().getContents() instanceof TranslatableContents t && t.getKey().equals("affinity.wildercord.weak")).size() == 1;
		if (!callout) {
			out.add("a weakness struck should show \"Weak!\" over the blaze");
		}
		if (!Heart.discovered(player, Bestiary.key("minecraft:blaze", Bestiary.Kind.WEAK, "frost"))) {
			out.add("the blaze's weakness to frost should go in the Bestiary");
		}
		if (!Heart.discovered(player, Bestiary.metKey("minecraft:blaze")) || !Heart.discovered(player, Bestiary.metKey("minecraft:husk"))) {
			out.add("the blaze and the husk should both be in the Bestiary as met");
		}
		// A snow golem is made of snow: frost does nothing, and it doesn't even flinch.
		LivingEntity golem = spawn(level, EntityTypes.SNOW_GOLEM, base.add(0, 0, 5));
		float onGolem = hit(player, golem, Runes.FROST);
		if (onGolem > 0 || golem.hurtTime > 0) {
			out.add("a snow golem should be immune to frost, without flinching (took " + onGolem + ", hurt time " + golem.hurtTime + ")");
		}
		if (!Heart.discovered(player, Bestiary.key("minecraft:snow_golem", Bestiary.Kind.IMMUNE, "frost"))) {
			out.add("the snow golem's immunity should go in the Bestiary");
		}
		return out;
	}

	/**
	 * Frost can't hurt a snow golem, so frost cast at one doesn't count toward a frost contract (it would be a
	 * target that never runs out); cast at a husk, it does. Each in a tick of its own, as casts are credited.
	 */
	private static List<String> immuneContracts(ClientGameTestContext context, TestSingleplayerContext world) {
		List<String> out = new ArrayList<>();
		int[] ids = on(world, player -> {
			ServerLevel level = player.level();
			Vec3 base = Vec3.atBottomCenterOf(STAGE);
			player.setAttached(Contracts.BOARD, new ContractRules.Board(Contracts.day(player), List.of(
				new ContractRules.Contract(ContractRules.ELEMENT_CASTS, "frost", 20, 0, false, "blank_rune:4"))));
			return new int[] {spawn(level, EntityTypes.SNOW_GOLEM, base.add(-2, 0, 4)).getId(), spawn(level, EntityTypes.HUSK, base.add(2, 0, 4)).getId()};
		});
		context.waitTicks(1);
		int onGolem = on(world, player -> {
			Contracts.onCast(player, List.of(Runes.TOUCH, Runes.FROST));
			hit(player, (LivingEntity) player.level().getEntity(ids[0]), Runes.FROST);
			return Contracts.board(player).contracts().getFirst().progress();
		});
		if (onGolem != 0) {
			out.add("frost on a snow golem, which it can't hurt, shouldn't count toward a frost contract");
		}
		context.waitTicks(1);
		int onHusk = on(world, player -> {
			Contracts.onCast(player, List.of(Runes.TOUCH, Runes.FROST));
			hit(player, (LivingEntity) player.level().getEntity(ids[1]), Runes.FROST);
			return Contracts.board(player).contracts().getFirst().progress();
		});
		if (onHusk != 1) {
			out.add("frost on a husk should count toward a frost contract (has " + onHusk + ")");
		}
		on(world, player -> {
			player.removeAttached(Contracts.BOARD);
			return null;
		});
		return out;
	}

	/**
	 * Fire on a hoglin (resists it) against a husk: half. On a blaze: nothing, and the Bestiary says
	 * immune. A Shatter on the hoglin breaks through its resistance; a Runebound husk carrying fire
	 * resists fire, though its kind doesn't.
	 */
	private static List<String> resistance(ServerPlayer player) {
		List<String> out = new ArrayList<>();
		ServerLevel level = player.level();
		Vec3 base = Vec3.atBottomCenterOf(STAGE);
		float plain = hit(player, spawn(level, EntityTypes.HUSK, base.add(-3, 0, 3)), Runes.FIRE);
		LivingEntity hoglin = spawn(level, EntityTypes.HOGLIN, base.add(0, 0, 4));
		float resisted = hit(player, hoglin, Runes.FIRE);
		double ratio = resisted / plain;
		if (plain <= 0 || ratio < 0.45 || ratio > 0.56) {
			out.add("a hoglin should take half of fire, next to a husk (" + resisted + " against " + plain + ")");
		}
		if (!Heart.discovered(player, Bestiary.key("minecraft:hoglin", Bestiary.Kind.RESISTS, "fire"))) {
			out.add("the hoglin's resistance to fire should go in the Bestiary");
		}
		LivingEntity blaze = spawn(level, EntityTypes.BLAZE, base.add(3, 0, 3));
		float onBlaze = hit(player, blaze, Runes.FIRE);
		if (onBlaze > 0) {
			out.add("fire shouldn't touch a blaze (it took " + onBlaze + ")");
		}
		if (!Heart.discovered(player, Bestiary.key("minecraft:blaze", Bestiary.Kind.IMMUNE, "fire"))) {
			out.add("the blaze's immunity to burning should go in the Bestiary");
		}
		// Frozen, then burnt: a Shatter lands in full, resistance or not.
		LivingEntity frozenHusk = spawn(level, EntityTypes.HUSK, base.add(-3, 0, 6));
		LivingEntity frozenHoglin = spawn(level, EntityTypes.HOGLIN, base.add(0, 0, 7));
		Reactions.mark(frozenHusk, Reactions.Mark.FROZEN);
		Reactions.mark(frozenHoglin, Reactions.Mark.FROZEN);
		float shatterHusk = hit(player, frozenHusk, Runes.FIRE);
		float shatterHoglin = hit(player, frozenHoglin, Runes.FIRE);
		double pierced = shatterHoglin / shatterHusk;
		if (shatterHusk <= plain || pierced < 0.95 || pierced > 1.08) {
			out.add("a Shatter should break through the hoglin's resistance (" + shatterHoglin + " against a husk's " + shatterHusk + ")");
		}
		// A Runebound husk carrying a Fire Bolt resists fire.
		Mob bound = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
		if (bound == null) {
			return List.of("couldn't make a husk");
		}
		bound.snapTo(base.x + 3, base.y, base.z + 7, 180, 0);
		bound.setNoAi(true);
		bound.addTag(TAG);
		bound.addTag("wildercord.rolled");
		Runebound.bind(bound, List.of(Runes.BOLT, Runes.FIRE), false);
		level.addFreshEntity(bound);
		sturdy(bound);
		double runebound = hit(player, bound, Runes.FIRE) / plain;
		if (runebound < 0.45 || runebound > 0.56) {
			out.add("a Runebound should resist its own element (took x" + runebound + " of a husk's fire)");
		}
		if (Heart.discovered(player, Bestiary.key("minecraft:husk", Bestiary.Kind.RESISTS, "fire"))) {
			out.add("a Runebound's resistance is its Cord's, not its kind's: it shouldn't go in the Bestiary");
		}
		return out;
	}

	// ------------------------------------------------------------------ climate

	/** Fire in the Nether (+20%) and void in the End (+20%), against the same on the Overworld platform; the HUD is told. */
	private static List<String> climate(ClientGameTestContext context, TestSingleplayerContext world) {
		List<String> out = new ArrayList<>();
		double[] home = on(world, player -> {
			Vec3 base = Vec3.atBottomCenterOf(STAGE);
			ServerLevel level = player.level();
			return new double[] {hit(player, spawn(level, EntityTypes.HUSK, base.add(-2, 0, 3)), Runes.FIRE),
				hit(player, spawn(level, EntityTypes.HUSK, base.add(2, 0, 3)), Runes.SONIC_BOOM),
				ClimateRules.factor(Climate.conditions(player), "fire"), ClimateRules.factor(Climate.conditions(player), "void")};
		});
		cleanup(context, world);

		travel(context, world, Level.NETHER, NETHER_STAGE);
		String nether = on(world, player -> {
			if (!Climate.conditions(player).contains(ClimateRules.Condition.NETHER)) {
				return "in the Nether, the climate should say so (has " + Climate.conditions(player) + ")";
			}
			float fire = hit(player, spawn(player.level(), EntityTypes.HUSK, Vec3.atBottomCenterOf(NETHER_STAGE).add(0, 0, 3)), Runes.FIRE);
			double expected = 1.2 / home[2];
			double ratio = fire / home[0];
			return Math.abs(ratio - expected) < 0.03 ? null
				: "fire should burn 20% hotter in the Nether (" + fire + " against " + home[0] + " at home: x" + ratio + ", wanted x" + expected + ")";
		});
		if (nether != null) {
			out.add(nether);
		}
		// Within a second the HUD hears of it: fire favoured, frost hindered.
		context.waitTicks(30);
		Set<ClimateRules.Condition> shown = context.computeOnClient(mc -> Climate.shown());
		if (!shown.contains(ClimateRules.Condition.NETHER)) {
			out.add("the HUD should be told the player is in the Nether (it shows " + shown + ")");
		}
		context.takeScreenshot(TestScreenshotOptions.of("affinities_hud_nether").disableCounterPrefix());
		cleanup(context, world);

		travel(context, world, Level.END, END_STAGE);
		world.getServer().runCommand("execute in minecraft:the_end run kill @e[type=minecraft:ender_dragon]");
		String end = on(world, player -> {
			float boom = hit(player, spawn(player.level(), EntityTypes.HUSK, Vec3.atBottomCenterOf(END_STAGE).add(0, 0, 3)), Runes.SONIC_BOOM);
			double expected = 1.2 / home[3];
			double ratio = boom / home[1];
			return Math.abs(ratio - expected) < 0.03 ? null
				: "void should bite 20% harder in the End (" + boom + " against " + home[1] + " at home: x" + ratio + ", wanted x" + expected + ")";
		});
		if (end != null) {
			out.add(end);
		}
		cleanup(context, world);
		travel(context, world, Level.OVERWORLD, STAGE);
		return out;
	}

	// ------------------------------------------------------------------ the config's switches

	/** With creature_affinities and elemental_climate off: a hoglin takes fire like a husk, and the Nether changes nothing. */
	private static List<String> switchedOff(ClientGameTestContext context, TestSingleplayerContext world) {
		List<String> out = new ArrayList<>();
		Path path = Config.path();
		String original;
		try {
			original = Files.exists(path) ? Files.readString(path, StandardCharsets.UTF_8) : null;
		} catch (IOException e) {
			return List.of("couldn't read " + path + ": " + e);
		}
		try {
			String off = WildercordConfig.DEFAULTS.toJson().replace("\"creature_affinities\": true", "\"creature_affinities\": false")
				.replace("\"elemental_climate\": true", "\"elemental_climate\": false");
			if (!off.contains("\"creature_affinities\": false") || !off.contains("\"elemental_climate\": false")) {
				return List.of("the default config should list creature_affinities and elemental_climate");
			}
			write(path, off);
			world.getServer().runCommand("wildercord reload");
			context.waitTicks(2);
			double[] home = on(world, player -> {
				Vec3 base = Vec3.atBottomCenterOf(STAGE);
				ServerLevel level = player.level();
				return new double[] {hit(player, spawn(level, EntityTypes.HUSK, base.add(-2, 0, 3)), Runes.FIRE),
					hit(player, spawn(level, EntityTypes.HOGLIN, base.add(2, 0, 3)), Runes.FIRE)};
			});
			if (Config.get().creatureAffinities() || Config.get().elementalClimate()) {
				out.add("/wildercord reload should read creature_affinities and elemental_climate: false");
			}
			double ratio = home[1] / home[0];
			if (ratio < 0.95 || ratio > 1.07) {
				out.add("with creature_affinities off, a hoglin should take fire like a husk (" + home[1] + " against " + home[0] + ")");
			}
			cleanup(context, world);
			travel(context, world, Level.NETHER, NETHER_STAGE);
			String nether = on(world, player -> {
				float fire = hit(player, spawn(player.level(), EntityTypes.HUSK, Vec3.atBottomCenterOf(NETHER_STAGE).add(0, 0, 3)), Runes.FIRE);
				return Math.abs(fire / home[0] - 1.0) < 0.03 ? null
					: "with elemental_climate off, fire in the Nether should hit as at home (" + fire + " against " + home[0] + ")";
			});
			if (nether != null) {
				out.add(nether);
			}
			context.waitTicks(30);
			Set<ClimateRules.Condition> shown = context.computeOnClient(mc -> Climate.shown());
			if (!shown.isEmpty()) {
				out.add("with elemental_climate off, the HUD should show no climate (it shows " + shown + ")");
			}
			cleanup(context, world);
			travel(context, world, Level.OVERWORLD, STAGE);
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
		return out;
	}

	// ------------------------------------------------------------------ helpers

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static <T> T on(TestSingleplayerContext world, Function<ServerPlayer, T> step) {
		return world.getServer().computeOnServer(server -> step.apply(player(server)));
	}

	/** Touch and {@code effect}, applied by the player straight to {@code target}: the damage it took. */
	private static float hit(ServerPlayer player, LivingEntity target, RuneDef effect) {
		SpellPlan.EffectNode node = SpellCompiler.compile(List.of(Runes.TOUCH, effect)).root().groups.getFirst().effects.getFirst();
		float before = target.getHealth();
		Effects.apply(new Cast(player), node, new Cast.Hit(List.<Entity>of(target), target.getBoundingBox().getCenter(), player.getLookAngle(),
			player.getEyePosition(), null, null, false));
		return before - target.getHealth();
	}

	/** A creature standing still where it's put, with 200 health, never a random Runebound. */
	private static <T extends Mob> T spawn(ServerLevel level, EntityType<T> type, Vec3 at) {
		T mob = type.create(level, EntitySpawnReason.COMMAND);
		if (mob == null) {
			throw new AssertionError("couldn't make a " + type);
		}
		mob.snapTo(at.x, at.y, at.z, 180, 0);
		mob.setNoAi(true);
		mob.addTag(TAG);
		// Never a random Runebound (it would have more health and cast back): tests pick their monsters.
		mob.addTag("wildercord.rolled");
		level.addFreshEntity(mob);
		sturdy(mob);
		return mob;
	}

	private static void sturdy(LivingEntity mob) {
		AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			health.setBaseValue(200);
		}
		mob.setHealth(mob.getMaxHealth());
	}

	/** An Echo Cord, every rune known, and the player hovering in creative flight over a stone platform. */
	private static void stage(TestSingleplayerContext world) {
		platform(world, Level.OVERWORLD, STAGE);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
			Spellbook book = Spellbooks.get(player).withStarterGiven();
			for (RuneDef rune : Runes.all()) {
				book = book.learn(rune.id());
			}
			Spellbooks.set(player, book);
			// A spell threaded, so the HUD shows its name with the climate marks after it.
			SpellCaster.edit(player, 0, List.of(Runes.BOLT.id(), Runes.FIRE.id()));
		});
		stand(world, Level.OVERWORLD, STAGE);
	}

	/** A 17x17 stone floor with 6 blocks of air over it. */
	private static void platform(TestSingleplayerContext world, ResourceKey<Level> dimension, BlockPos at) {
		String in = "execute in " + dimension.identifier() + " run ";
		int x = at.getX();
		int y = at.getY();
		int z = at.getZ();
		world.getServer().runCommand(in + "fill " + (x - 8) + " " + (y - 1) + " " + (z - 8) + " " + (x + 8) + " " + (y - 1) + " " + (z + 8) + " minecraft:stone");
		world.getServer().runCommand(in + "fill " + (x - 8) + " " + y + " " + (z - 8) + " " + (x + 8) + " " + (y + 6) + " " + (z + 8) + " minecraft:air");
	}

	/** The player at the middle of the platform in {@code dimension}, facing south (+Z), toward the creatures. */
	private static void stand(TestSingleplayerContext world, ResourceKey<Level> dimension, BlockPos at) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.CREATIVE);
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
			player.teleportTo(server.getLevel(dimension), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, Set.<Relative>of(), 0.0F, 8.0F, false);
			player.setDeltaMovement(Vec3.ZERO);
		});
	}

	/** Takes the player to {@code dimension}, lets its ground load, and clears a platform there. */
	private static void travel(ClientGameTestContext context, TestSingleplayerContext world, ResourceKey<Level> dimension, BlockPos at) {
		stand(world, dimension, at.above(20));
		context.waitTicks(40);
		platform(world, dimension, at);
		stand(world, dimension, at);
		context.waitTicks(10);
	}

	private static void cleanup(ClientGameTestContext context, TestSingleplayerContext world) {
		for (String dimension : List.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end")) {
			world.getServer().runCommand("execute in " + dimension + " run kill @e[tag=" + TAG + "]");
			world.getServer().runCommand("execute in " + dimension + " run kill @e[type=item]");
		}
		context.waitTicks(25);
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
