package dev.wildercord.gametest;

import dev.wildercord.cast.Cast;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.PlayerAffinities;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.Runebound;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.client.CordScreen;
import dev.wildercord.client.GrimoireToast;
import dev.wildercord.config.Config;
import dev.wildercord.config.WildercordConfig;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.PlayerAffinity;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Players' own affinities, in a real world: casting fire spells grows fire (by the mana spent) and nothing
 * else; mining stone grows earth until the day's allowance for stone is full, and ores still count past it;
 * reaching level I writes the Grimoire entry, condenses its mana and shows the toast; the power bonus lands
 * on that element's effects only; from level III a Runebound's fire lands softer on the player (though not
 * through a reaction); and {@code features.player_affinity} switches all of it off. Then the Grimoire page
 * with a spread of affinities is filmed ({@code affinity_grimoire}).
 *
 * <p>The player is in survival (creative players earn nothing), with 200 health so no hit ends the test.
 * Every creature has 200 health too and never rolls as a random Runebound. Runs in the full suite;
 * {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and {@code WILDERCORD_SHOWCASE} skip it.</p>
 */
public class WildercordPlayerAffinityTest implements FabricClientGameTest {
	/** A stone platform high over a superflat world, at noon, so nothing about the climate changes between hits. */
	private static final BlockPos STAGE = new BlockPos(0, 180, 0);
	private static final String TAG = "wildercord.player_affinity";

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
			world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			stage(world);
			context.waitTicks(10);
			List<String> failures = new ArrayList<>();
			failures.addAll(casting(context, world));
			cleanup(context, world);
			failures.addAll(on(world, WildercordPlayerAffinityTest::mining));
			cleanup(context, world);
			failures.addAll(levelUp(context, world));
			cleanup(context, world);
			failures.addAll(on(world, WildercordPlayerAffinityTest::power));
			cleanup(context, world);
			failures.addAll(on(world, WildercordPlayerAffinityTest::resistance));
			cleanup(context, world);
			failures.addAll(switchedOff(context, world));
			cleanup(context, world);
			grimoirePage(context, world);
			if (!failures.isEmpty()) {
				throw new AssertionError("Player affinities went wrong:\n  " + String.join("\n  ", failures));
			}
		}
	}

	// ------------------------------------------------------------------ growing

	/** Ten Fire Bolts cast for real: fire grows by a tenth of a point for each mana spent, frost not at all; then a Frost Bolt grows frost. */
	private static List<String> casting(ClientGameTestContext context, TestSingleplayerContext world) {
		List<String> out = new ArrayList<>();
		double expected = on(world, player -> {
			fresh(player, Map.of());
			SpellCaster.edit(player, 0, List.of(Runes.BOLT.id(), Runes.FIRE.id()));
			double mana = 0;
			for (int i = 0; i < 10; i++) {
				Spellbooks.setMana(player, Mana.max(player));
				Spellbooks.setReadyAt(player, 0, 0);
				float before = Spellbooks.mana(player);
				SpellCaster.cast(player, 0);
				mana += before - Spellbooks.mana(player);
			}
			return mana * PlayerAffinity.Source.CAST.points;
		});
		context.waitTicks(2);
		int[] fireAndFrost = on(world, player -> new int[] {points(player, "fire"), points(player, "frost")});
		if (expected <= 0) {
			out.add("casting Fire Bolt ten times should have spent some mana");
		} else if (Math.abs(fireAndFrost[0] - Math.floor(expected)) > 1) {
			out.add("ten Fire Bolts should grow fire by a tenth of the mana spent (about " + expected + "; has " + fireAndFrost[0] + ")");
		}
		if (fireAndFrost[1] != 0) {
			out.add("casting fire shouldn't grow frost (has " + fireAndFrost[1] + ")");
		}
		int frost = on(world, player -> {
			SpellCaster.edit(player, 1, List.of(Runes.BOLT.id(), Runes.FROST.id()));
			for (int i = 0; i < 5; i++) {
				Spellbooks.setMana(player, Mana.max(player));
				Spellbooks.setReadyAt(player, 1, 0);
				SpellCaster.cast(player, 1);
			}
			return points(player, "frost");
		});
		if (frost <= 0) {
			out.add("casting Frost Bolt should grow frost (has " + frost + ")");
		}
		return out;
	}

	/**
	 * Two hundred stone blocks mined with a pickaxe fill the day's stone allowance (15 points) and no more;
	 * an ore still counts past it; stone broken by hand counts for nothing.
	 */
	private static List<String> mining(ServerPlayer player) {
		List<String> out = new ArrayList<>();
		fresh(player, Map.of());
		ServerLevel level = player.level();
		BlockPos at = STAGE.offset(2, 0, 2);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
		for (int i = 0; i < 200; i++) {
			level.setBlockAndUpdate(at, Blocks.STONE.defaultBlockState());
			player.gameMode.destroyBlock(at);
		}
		int stone = points(player, "earth");
		int cap = (int) Math.round(PlayerAffinity.Source.STONE.daily);
		if (Math.abs(stone - cap) > 1) {
			out.add("200 stone should fill the day's stone allowance, " + cap + " points, and no more (has " + stone + ")");
		}
		level.setBlockAndUpdate(at, Blocks.IRON_ORE.defaultBlockState());
		player.gameMode.destroyBlock(at);
		int ore = points(player, "earth");
		if (ore <= stone) {
			out.add("an ore should still count once stone's allowance is full (" + stone + " before, " + ore + " after)");
		}
		fresh(player, Map.of());
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		for (int i = 0; i < 20; i++) {
			level.setBlockAndUpdate(at, Blocks.STONE.defaultBlockState());
			player.gameMode.destroyBlock(at);
		}
		if (points(player, "earth") != 0) {
			out.add("stone broken without a pickaxe shouldn't count (has " + points(player, "earth") + ")");
		}
		level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
		return out;
	}

	/** Fire from 99 points to past 100: level I, its Grimoire entry and mana, and the toast on the client. */
	private static List<String> levelUp(ClientGameTestContext context, TestSingleplayerContext world) {
		List<String> out = new ArrayList<>();
		context.runOnClient(mc -> mc.gui.toastManager().clear());
		// Frost close behind, so fire reaching I doesn't also bring a leaning (and its feat's mana).
		int condensed = on(world, player -> {
			fresh(player, Map.of("fire", PlayerAffinity.threshold(1) - 1, "frost", PlayerAffinity.threshold(1) - 5));
			return Heart.condensed(player);
		});
		boolean gained = on(world, player -> PlayerAffinities.gain(player, PlayerAffinity.Source.CAST, "fire", 20));
		context.waitTicks(5);
		String state = on(world, player -> {
			List<String> wrong = new ArrayList<>();
			if (Heart.affinityLevel(player, "fire") != 1) {
				wrong.add("fire should have reached level I (has " + points(player, "fire") + " points)");
			}
			if (!Heart.discovered(player, PlayerAffinity.KEY_PREFIX + "fire")) {
				wrong.add("reaching fire I should write it in the Grimoire");
			}
			if (Heart.condensed(player) - condensed != PlayerAffinity.REWARD) {
				wrong.add("fire I should condense " + PlayerAffinity.REWARD + " mana (condensed " + (Heart.condensed(player) - condensed) + ")");
			}
			return String.join("; ", wrong);
		});
		if (!gained) {
			out.add("20 mana of fire should earn something");
		}
		if (!state.isEmpty()) {
			out.add(state);
		}
		boolean toast = context.computeOnClient(mc -> mc.gui.toastManager().getToast(GrimoireToast.class, GrimoireToast.token("fire", 1)) != null);
		if (!toast) {
			out.add("reaching fire I should show its toast");
		}
		context.takeScreenshot(TestScreenshotOptions.of("affinity_toast").disableCounterPrefix());
		// A second gain of the same size is nothing new: no second entry.
		on(world, player -> PlayerAffinities.gain(player, PlayerAffinity.Source.CAST, "fire", 20));
		return out;
	}

	// ------------------------------------------------------------------ what it gives

	/** Fire III: the player's fire hits a husk 9% harder, and their frost (no affinity) exactly as before. */
	private static List<String> power(ServerPlayer player) {
		List<String> out = new ArrayList<>();
		Vec3 base = Vec3.atBottomCenterOf(STAGE);
		ServerLevel level = player.level();
		fresh(player, Map.of());
		float fire = hit(player, spawn(level, EntityTypes.HUSK, base.add(-3, 0, 3)), Runes.FIRE);
		float frost = hit(player, spawn(level, EntityTypes.HUSK, base.add(-1, 0, 3)), Runes.FROST);
		fresh(player, Map.of("fire", PlayerAffinity.threshold(3)));
		float fireIII = hit(player, spawn(level, EntityTypes.HUSK, base.add(1, 0, 3)), Runes.FIRE);
		float frostNow = hit(player, spawn(level, EntityTypes.HUSK, base.add(3, 0, 3)), Runes.FROST);
		double wanted = PlayerAffinity.power(3);
		if (fire <= 0 || Math.abs(fireIII / fire - wanted) > 0.02) {
			out.add("fire III should make fire hit x" + wanted + " (" + fireIII + " against " + fire + ")");
		}
		if (frost <= 0 || Math.abs(frostNow / frost - 1.0) > 0.01) {
			out.add("fire's affinity shouldn't touch frost (" + frostNow + " against " + frost + ")");
		}
		return out;
	}

	/**
	 * A Runebound's fire on the player: the same at fire II, 10% softer at III and 20% at V; and a Shatter
	 * (the player frozen first) breaks through, as a reaction breaks through any resistance.
	 */
	private static List<String> resistance(ServerPlayer player) {
		List<String> out = new ArrayList<>();
		Mob caster = runebound(player.level(), Vec3.atBottomCenterOf(STAGE).add(0, 0, 3));
		fresh(player, Map.of());
		float plain = hitPlayer(player, caster, Runes.FIRE, false);
		float shatter = hitPlayer(player, caster, Runes.FIRE, true);
		fresh(player, Map.of("fire", PlayerAffinity.threshold(2)));
		float two = hitPlayer(player, caster, Runes.FIRE, false);
		fresh(player, Map.of("fire", PlayerAffinity.threshold(3)));
		float three = hitPlayer(player, caster, Runes.FIRE, false);
		float shatterThree = hitPlayer(player, caster, Runes.FIRE, true);
		fresh(player, Map.of("fire", PlayerAffinity.threshold(5)));
		float five = hitPlayer(player, caster, Runes.FIRE, false);
		if (plain <= 0) {
			return List.of("a Runebound's fire should hurt the player (took " + plain + ")");
		}
		if (Math.abs(two / plain - 1.0) > 0.02) {
			out.add("fire II shouldn't resist anything yet (" + two + " against " + plain + ")");
		}
		if (Math.abs(three / plain - PlayerAffinity.damageTaken(3)) > 0.03) {
			out.add("fire III should shrug off 10% of a Runebound's fire (" + three + " against " + plain + ")");
		}
		if (Math.abs(five / plain - PlayerAffinity.damageTaken(5)) > 0.03) {
			out.add("fire V should shrug off 20% of a Runebound's fire (" + five + " against " + plain + ")");
		}
		if (shatter <= plain || Math.abs(shatterThree / shatter - 1.0) > 0.03) {
			out.add("a Shatter should break through fire III (" + shatterThree + " against " + shatter + " with none)");
		}
		return out;
	}

	/** With features.player_affinity off: fire III gives no power and no resistance, and nothing earns points. */
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
			String off = WildercordConfig.DEFAULTS.toJson().replace("\"player_affinity\": true", "\"player_affinity\": false");
			if (!off.contains("\"player_affinity\": false")) {
				return List.of("the default config should list player_affinity");
			}
			write(path, off);
			world.getServer().runCommand("wildercord reload");
			context.waitTicks(2);
			String wrong = on(world, player -> {
				List<String> problems = new ArrayList<>();
				if (Config.get().playerAffinity()) {
					problems.add("/wildercord reload should read player_affinity: false");
				}
				Vec3 base = Vec3.atBottomCenterOf(STAGE);
				ServerLevel level = player.level();
				fresh(player, Map.of());
				float plain = hit(player, spawn(level, EntityTypes.HUSK, base.add(-2, 0, 3)), Runes.FIRE);
				Mob caster = runebound(level, base.add(0, 0, 5));
				float taken = hitPlayer(player, caster, Runes.FIRE, false);
				fresh(player, Map.of("fire", PlayerAffinity.threshold(5)));
				float mastered = hit(player, spawn(level, EntityTypes.HUSK, base.add(2, 0, 3)), Runes.FIRE);
				float takenMastered = hitPlayer(player, caster, Runes.FIRE, false);
				if (Math.abs(mastered / plain - 1.0) > 0.01) {
					problems.add("with affinities off, fire V should give no power (" + mastered + " against " + plain + ")");
				}
				if (Math.abs(takenMastered / taken - 1.0) > 0.01) {
					problems.add("with affinities off, fire V should resist nothing (" + takenMastered + " against " + taken + ")");
				}
				if (PlayerAffinities.gain(player, PlayerAffinity.Source.FISH, "", 5) || points(player, "frost") != 0) {
					problems.add("with affinities off, nothing should earn points");
				}
				// The price of a Fire Bolt isn't cut either.
				SpellCompiler.Compiled compiled = SpellCompiler.compile(List.of(Runes.BOLT, Runes.FIRE));
				if (Heart.manaCost(player, compiled) != compiled.manaCost()) {
					problems.add("with affinities off, fire V shouldn't make Fire Bolt cheaper (" + Heart.manaCost(player, compiled) + " against " + compiled.manaCost() + ")");
				}
				return String.join("; ", problems);
			});
			if (!wrong.isEmpty()) {
				out.add(wrong);
			}
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
		// Back on: fire V makes a Fire Bolt 10% cheaper.
		String cheaper = on(world, player -> {
			SpellCompiler.Compiled compiled = SpellCompiler.compile(List.of(Runes.BOLT, Runes.FIRE));
			int full = Heart.manaCost(player, compiled);
			fresh(player, Map.of());
			int plain = Heart.manaCost(player, compiled);
			return full < plain ? null : "fire V should make Fire Bolt cheaper (" + full + " against " + plain + ")";
		});
		if (cheaper != null) {
			out.add(cheaper);
		}
		return out;
	}

	// ------------------------------------------------------------------ the Grimoire page

	/** A spread of affinities, from none to V, on the Grimoire page. */
	private static void grimoirePage(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			Map<String, Integer> spread = new HashMap<>();
			spread.put("fire", 2600);
			spread.put("frost", 720);
			spread.put("storm", 40);
			spread.put("earth", 10400);
			spread.put("life", 150);
			spread.put("arcane", 1250);
			spread.put("blood", 4600);
			fresh(player, spread);
			return null;
		});
		context.waitTicks(5);
		context.setScreen(CordScreen::new);
		context.waitTicks(5);
		context.runOnClient(mc -> {
			if (mc.gui.screen() instanceof CordScreen screen) {
				screen.showPage(2);
			}
		});
		context.getInput().setCursorPos(4, 4);
		context.waitTicks(5);
		context.takeScreenshot(TestScreenshotOptions.of("affinity_grimoire").disableCounterPrefix());
		context.setScreen(() -> null);
		context.waitTicks(2);
	}

	// ------------------------------------------------------------------ helpers

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static <T> T on(TestSingleplayerContext world, Function<ServerPlayer, T> step) {
		return world.getServer().computeOnServer(server -> step.apply(player(server)));
	}

	private static int points(ServerPlayer player, String element) {
		return Heart.affinity(player).getOrDefault(element, 0);
	}

	/** The player's affinities set to exactly {@code points}, with a fresh day's allowances. */
	private static void fresh(ServerPlayer player, Map<String, Integer> points) {
		player.setAttached(WildercordAttachments.AFFINITY, Map.copyOf(points));
		player.removeAttached(WildercordAttachments.AFFINITY_TALLY);
		player.clearFire();
		player.setHealth(player.getMaxHealth());
	}

	/** Touch and {@code effect}, applied by the player straight to {@code target}: the damage it took. */
	private static float hit(ServerPlayer player, LivingEntity target, RuneDef effect) {
		SpellPlan.EffectNode node = SpellCompiler.compile(List.of(Runes.TOUCH, effect)).root().groups.getFirst().effects.getFirst();
		float before = target.getHealth();
		Effects.apply(new Cast(player), node, new Cast.Hit(List.<Entity>of(target), target.getBoundingBox().getCenter(), player.getLookAngle(),
			player.getEyePosition(), null, null, false));
		return before - target.getHealth();
	}

	/** Touch and {@code effect}, applied by {@code caster} to the player (frozen first, for a Shatter): the damage the player took. */
	private static float hitPlayer(ServerPlayer player, Mob caster, RuneDef effect, boolean frozen) {
		SpellPlan.EffectNode node = SpellCompiler.compile(List.of(Runes.TOUCH, effect)).root().groups.getFirst().effects.getFirst();
		player.clearFire();
		player.setHealth(player.getMaxHealth());
		if (frozen) {
			Reactions.mark(player, Reactions.Mark.FROZEN);
		}
		float before = player.getHealth();
		Effects.apply(new Cast(caster), node, new Cast.Hit(List.<Entity>of(player), player.getBoundingBox().getCenter(), caster.getLookAngle(),
			caster.getEyePosition(), null, null, false));
		float taken = before - player.getHealth();
		player.clearFire();
		player.setTicksFrozen(0);
		player.setHealth(player.getMaxHealth());
		return taken;
	}

	/** A Runebound husk carrying a Fire Bolt, standing still. */
	private static Mob runebound(ServerLevel level, Vec3 at) {
		Mob bound = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
		if (bound == null) {
			throw new AssertionError("couldn't make a husk");
		}
		bound.snapTo(at.x, at.y, at.z, 0, 0);
		bound.setNoAi(true);
		bound.addTag(TAG);
		bound.addTag("wildercord.rolled");
		Runebound.bind(bound, List.of(Runes.BOLT, Runes.FIRE), false);
		level.addFreshEntity(bound);
		sturdy(bound);
		return bound;
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

	/** An Echo Cord, every rune known, and the player in survival on a stone platform with 200 health. */
	private static void stage(TestSingleplayerContext world) {
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("fill " + (x - 8) + " " + (y - 1) + " " + (z - 8) + " " + (x + 8) + " " + (y - 1) + " " + (z + 8) + " minecraft:stone");
		world.getServer().runCommand("fill " + (x - 8) + " " + y + " " + (z - 8) + " " + (x + 8) + " " + (y + 6) + " " + (z + 8) + " minecraft:air");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
			Spellbook book = Spellbooks.get(player).withStarterGiven();
			for (RuneDef rune : Runes.all()) {
				book = book.learn(rune.id());
			}
			Spellbooks.set(player, book);
			player.setGameMode(GameType.SURVIVAL);
			sturdy(player);
			player.teleportTo(server.overworld(), x + 0.5, y, z + 0.5, Set.<Relative>of(), 0.0F, 8.0F, false);
			player.setDeltaMovement(Vec3.ZERO);
		});
	}

	private static void cleanup(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("kill @e[tag=" + TAG + "]");
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=wildercord:rune_bolt]");
		context.waitTicks(20);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.clearFire();
			player.setHealth(player.getMaxHealth());
		});
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
