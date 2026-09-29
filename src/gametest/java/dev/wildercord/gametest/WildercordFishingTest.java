package dev.wildercord.gametest;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.Fishing;
import dev.wildercord.cast.LeyWalker;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.cast.events.ManaStorm;
import dev.wildercord.cast.events.WorldEvents;
import dev.wildercord.content.RuneItem;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.content.WildercordLoot;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneSources;
import dev.wildercord.spell.Runes;
import dev.wildercord.world.LeyLines;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Runes from fishing, in a real game. The fishing loot tables are rolled thousands of times through the server's own
 * loot API with a proper fishing context (a real bobber as {@code this}, where it floats as {@code origin}): about 4
 * treasure catches in 11 come up a rune of the sea list (or one of the two found only by fishing) and 1 in 11 a Torn
 * Page; a catch in plain water never brings up a tangled rune, while one under a mana storm does about 12% of the time,
 * on a ley line and in a thunderstorm about 5%, and never out of open water; the first rune fished earns Reeled In.
 * Then the two fishing runes are cast: Tidehook reels a husk in and soaks it, and Current carries the player through a
 * channel of water, up into the rain and down again without fall damage, and does nothing on dry land.
 *
 * <p>Everything happens on a stone stage with a water channel beside it, high in the air over ocean (filled in with
 * /fillbiome, so rain falls there) and away from any ley line. Runs in the full suite; skipped by
 * {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and {@code WILDERCORD_SHOWCASE}.</p>
 */
public class WildercordFishingTest implements FabricClientGameTest {
	private static final int TREASURE_ROLLS = 3000;
	private static final int CATCH_ROLLS = 2000;
	private static final int STAGE_Y = 240;

	/** Where the stage is: chosen once the world is up, somewhere near spawn that no ley line runs under. */
	private static BlockPos stage;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runOnServer(server -> server.setWeatherParameters(6000, 0, false, false));
			stage = world.getServer().computeOnServer(WildercordFishingTest::findStage);
			build(world);
			context.waitTicks(20);
			List<String> failures = new ArrayList<>();
			run(failures, "treasure", () -> treasure(world));
			run(failures, "plain water", () -> plainWater(world));
			run(failures, "mana storm", () -> storm(context, world));
			run(failures, "ley line", () -> leyLine(world));
			run(failures, "thunderstorm", () -> thunder(context, world));
			run(failures, "Tidehook", () -> tidehook(context, world));
			run(failures, "Current in water", () -> currentInWater(context, world));
			run(failures, "Current on dry land", () -> currentDry(context, world));
			if (!failures.isEmpty()) {
				throw new AssertionError("Fishing went wrong:\n  " + String.join("\n  ", failures));
			}
		}
	}

	private interface Check {
		String run();
	}

	private static void run(List<String> failures, String name, Check check) {
		String failure = check.run();
		if (failure != null) {
			failures.add(name + ": " + failure);
		}
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static List<String> ids(RuneDef... runes) {
		return java.util.Arrays.stream(runes).map(RuneDef::id).toList();
	}

	private static String pct(double share) {
		return String.format("%.1f%%", share * 100);
	}

	// ------------------------------------------------------------------ the stage

	/** Near spawn, the first spot where no ley line runs under the channel's middle (where every bobber floats). */
	private static BlockPos findStage(MinecraftServer server) {
		ServerPlayer player = player(server);
		long seed = LeyWalker.seed(player.level());
		BlockPos from = player.blockPosition();
		for (int r = 0; r <= 48; r += 4) {
			for (int dx = -r; dx <= r; dx += 4) {
				for (int dz = -r; dz <= r; dz += 4) {
					BlockPos at = new BlockPos(from.getX() + dx, STAGE_Y, from.getZ() + dz);
					Vec3 bob = bobberAt(at);
					if (LeyLines.strength(seed, bob.x, bob.z) < 0.05) {
						return at;
					}
				}
			}
		}
		return new BlockPos(from.getX(), STAGE_Y, from.getZ());
	}

	/** Where every bobber floats: on the channel's water, halfway along it. */
	private static Vec3 bobberAt(BlockPos stage) {
		return new Vec3(stage.getX() - 6.5, STAGE_Y + 1.9, stage.getZ() + 0.5);
	}

	private static Vec3 bobber() {
		return bobberAt(stage);
	}

	/**
	 * A 25x25 stone stage with open sky over it; along its west side a glass channel of water 5 wide, 3 deep and 21
	 * long; ocean all round (so rain falls here, not snow); an Echo Cord, every rune known, plenty of mana.
	 */
	private static void build(TestSingleplayerContext world) {
		int x = stage.getX();
		int y = stage.getY();
		int z = stage.getZ();
		world.getServer().runCommand("fill " + (x - 12) + " " + (y - 1) + " " + (z - 12) + " " + (x + 12) + " " + (y - 1) + " " + (z + 12) + " minecraft:stone");
		world.getServer().runCommand("fill " + (x - 12) + " " + y + " " + (z - 12) + " " + (x + 12) + " " + (y + 40) + " " + (z + 12) + " minecraft:air");
		world.getServer().runCommand("fill " + (x - 10) + " " + (y - 2) + " " + (z - 11) + " " + (x - 4) + " " + (y + 1) + " " + (z + 11) + " minecraft:glass");
		world.getServer().runCommand("fill " + (x - 9) + " " + (y - 1) + " " + (z - 10) + " " + (x - 5) + " " + (y + 1) + " " + (z + 10) + " minecraft:water");
		world.getServer().runCommand("fillbiome " + (x - 13) + " " + (y - 5) + " " + (z - 13) + " " + (x + 13) + " " + (y + 30) + " " + (z + 13) + " minecraft:ocean");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.SURVIVAL);
			Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
			Spellbook book = Spellbooks.get(player).withStarterGiven();
			for (RuneDef rune : Runes.all()) {
				book = book.learn(rune.id());
			}
			Spellbooks.set(player, book);
			player.setAttached(WildercordAttachments.CRYSTALS, Mana.MAX_CRYSTALS);
			stand(player, stage.getX() + 2.5, stage.getZ() + 0.5, 0.0F, 0.0F);
		});
	}

	/** The player at a spot on the stage (or in the channel), facing {@code yaw} (0 is south) and {@code pitch}. */
	private static void stand(ServerPlayer player, double x, double z, float yaw, float pitch) {
		player.teleportTo(player.level(), x, STAGE_Y, z, Set.<Relative>of(), yaw, pitch, false);
		player.setDeltaMovement(Vec3.ZERO);
		player.setHealth(player.getMaxHealth());
		player.removeAllEffects();
		player.resetFallDistance();
	}

	/** Threads spell 1 with these runes and casts it at once with full mana. Returns what went wrong, or null. */
	private static String cast(ServerPlayer player, RuneDef... runes) {
		SpellCaster.edit(player, 0, List.of());
		SpellCaster.edit(player, 0, ids(runes));
		if (!Spellbooks.get(player).spells().getFirst().equals(ids(runes))) {
			return "the spell didn't thread (has " + Spellbooks.get(player).spells().getFirst() + ")";
		}
		Spellbooks.setReadyAt(player, 0, 0);
		Spellbooks.setMana(player, Mana.max(player));
		long now = player.level().getGameTime();
		SpellCaster.cast(player, 0);
		return Spellbooks.readyAt(player, 0) > now ? null : "it didn't cast";
	}

	// ------------------------------------------------------------------ rolling the loot

	/** A bobber floating at {@code at}, fishing open water (as a fresh one does), cast by {@code owner} or nobody. */
	private static FishingHook hook(ServerLevel level, Vec3 at, ServerPlayer owner) {
		FishingHook hook = owner == null ? new FishingHook(EntityTypes.FISHING_BOBBER, level) : new FishingHook(owner, level, 0, 0);
		hook.setPos(at.x, at.y, at.z);
		return hook;
	}

	/** One catch from {@code table}, as a rod reeling in {@code hook} brings it up. */
	private static List<ItemStack> roll(ServerLevel level, ResourceKey<LootTable> table, Vec3 at, FishingHook hook) {
		LootParams params = new LootParams.Builder(level)
			.withParameter(LootContextParams.ORIGIN, at)
			.withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD))
			.withParameter(LootContextParams.THIS_ENTITY, hook)
			.create(LootContextParamSets.FISHING);
		return level.getServer().reloadableRegistries().getLootTable(table).getRandomItems(params);
	}

	private static Set<RuneDef> fishable() {
		Set<RuneDef> out = new HashSet<>(WildercordLoot.seaRunes());
		out.addAll(RuneSources.FISHING.runes());
		return out;
	}

	/** What a run of catches brought up: rolls with a tangled rune, of them one found only by fishing, or what went wrong. */
	private record Tangled(int rolls, int tangled, int world, String failure) {
		double share() {
			return (double) tangled / rolls;
		}
	}

	/** Rolls the main fishing table {@code rolls} times: how many catches had a rune tangled in the line as well. */
	private static Tangled tangled(ServerLevel level, Vec3 at, FishingHook hook, int rolls) {
		Set<RuneDef> fishable = fishable();
		int tangled = 0;
		int world = 0;
		for (int i = 0; i < rolls; i++) {
			List<ItemStack> catches = roll(level, BuiltInLootTables.FISHING, at, hook);
			if (catches.isEmpty() || catches.size() > 2) {
				return new Tangled(rolls, tangled, world, "a catch should be one thing, or one and a tangled rune (got " + catches + ")");
			}
			if (catches.size() == 2) {
				RuneDef rune = RuneItem.runeOf(catches.get(1)).orElse(null);
				if (rune == null || !catches.get(1).is(WildercordItems.RUNE)) {
					return new Tangled(rolls, tangled, world, "what comes up tangled in the line should be a rune (got " + catches.get(1) + ")");
				}
				if (!fishable.contains(rune)) {
					return new Tangled(rolls, tangled, world, rune.name() + " shouldn't come up on a fishing line");
				}
				tangled++;
				if (RuneSources.FISHING.runes().contains(rune)) {
					world++;
				}
			}
		}
		return new Tangled(rolls, tangled, world, null);
	}

	// ------------------------------------------------------------------ treasure

	private static String treasure(TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			FishingHook hook = hook(level, bobber(), null);
			Set<RuneDef> fishable = fishable();
			Set<RuneDef> seen = new HashSet<>();
			int runes = 0;
			int pages = 0;
			int found = 0;
			for (int i = 0; i < TREASURE_ROLLS; i++) {
				for (ItemStack stack : roll(level, BuiltInLootTables.FISHING_TREASURE, bobber(), hook)) {
					if (stack.is(WildercordItems.TORN_PAGE)) {
						pages++;
					} else if (stack.is(WildercordItems.RUNE)) {
						RuneDef rune = RuneItem.runeOf(stack).orElse(null);
						if (rune == null) {
							return "a treasure rune came out blank (a Silent Rune)";
						}
						if (!fishable.contains(rune)) {
							return rune.name() + " shouldn't come up in a treasure catch";
						}
						runes++;
						seen.add(rune);
						if (RuneSources.FISHING.runes().contains(rune)) {
							found++;
						}
					}
				}
			}
			double runeShare = (double) runes / TREASURE_ROLLS;
			double pageShare = (double) pages / TREASURE_ROLLS;
			double worldShare = runes == 0 ? 0 : (double) found / runes;
			if (runeShare < 0.31 || runeShare > 0.42) {
				return "about 4 treasure catches in 11 should be a rune (" + pct(runeShare) + " of " + TREASURE_ROLLS + ")";
			}
			if (pageShare < 0.06 || pageShare > 0.125) {
				return "about 1 treasure catch in 11 should be a Torn Page (" + pct(pageShare) + ")";
			}
			if (worldShare < 0.12 || worldShare > 0.26) {
				return "Tidehook and Current should be about 2 in 11 of the treasure runes (" + pct(worldShare) + ")";
			}
			if (!seen.contains(Runes.TIDEHOOK) || !seen.contains(Runes.CURRENT)) {
				return "both runes found only by fishing should turn up in treasure (saw " + seen.stream().map(RuneDef::name).toList() + ")";
			}
			if (!seen.contains(Runes.TIDEBREATH) || !seen.contains(Runes.BUBBLE)) {
				return "the sea list's runes should turn up (saw " + seen.stream().map(RuneDef::name).toList() + ")";
			}
			return null;
		});
	}

	// ------------------------------------------------------------------ magic waters

	/** Clear weather, no storm, no ley line: never a tangled rune. */
	private static String plainWater(TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			if (WorldEvents.stormAt(level, bobber()) != null || Fishing.nearLey(level, bobber()) || Fishing.inThunder(level, bobber())) {
				return "the stage should be plain water (storm " + (WorldEvents.stormAt(level, bobber()) != null) + ", ley "
					+ Fishing.nearLey(level, bobber()) + ", thunder " + Fishing.inThunder(level, bobber()) + ")";
			}
			Tangled run = tangled(level, bobber(), hook(level, bobber(), null), CATCH_ROLLS);
			if (run.failure() != null) {
				return run.failure();
			}
			return run.tangled() == 0 ? null : "plain water should never tangle a rune in the line (" + run.tangled() + " in " + CATCH_ROLLS + ")";
		});
	}

	private static String storm(ClientGameTestContext context, TestSingleplayerContext world) {
		boolean started = world.getServer().computeOnServer(server -> WorldEvents.startStorm(player(server).level(), player(server), true) != null);
		if (!started) {
			return "a mana storm should start over the stage";
		}
		context.waitTicks(25);
		String failure = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			if (!Fishing.underStorm(level, bobber())) {
				return "the storm should rage over the bobber";
			}
			Tangled run = tangled(level, bobber(), hook(level, bobber(), null), CATCH_ROLLS);
			if (run.failure() != null) {
				return run.failure();
			}
			if (run.share() < 0.08 || run.share() > 0.16) {
				return "under a mana storm about 12% of catches should bring up a tangled rune (" + pct(run.share()) + " of " + CATCH_ROLLS + ")";
			}
			double worldShare = (double) run.world() / Math.max(1, run.tangled());
			if (worldShare < 0.17 || worldShare > 0.46) {
				return "Tidehook and Current should be about 1 in 3 of the tangled runes (" + pct(worldShare) + ")";
			}
			// Out of open water (a pond, a hole), never, storm or no storm.
			FishingHook pond = hook(level, bobber(), null);
			try {
				java.lang.reflect.Field open = FishingHook.class.getDeclaredField("openWater");
				open.setAccessible(true);
				open.setBoolean(pond, false);
			} catch (ReflectiveOperationException e) {
				return "couldn't make a bobber that isn't in open water: " + e;
			}
			Tangled closed = tangled(level, bobber(), pond, 500);
			if (closed.failure() != null) {
				return closed.failure();
			}
			if (closed.tangled() > 0) {
				return "a bobber out of open water should never tangle a rune, even under a storm (" + closed.tangled() + " in 500)";
			}
			// The angler's own bobber: the first rune up earns Reeled In.
			FishingHook own = hook(level, bobber(), player);
			boolean caught = false;
			for (int i = 0; i < 400 && !caught; i++) {
				caught = roll(level, BuiltInLootTables.FISHING, bobber(), own).size() == 2;
			}
			if (!caught) {
				return "400 catches under a storm should bring up at least one tangled rune";
			}
			return Heart.discovered(player, "feat:" + Feats.REELED_IN) ? null : "fishing up a rune should earn Reeled In";
		});
		world.getServer().runOnServer(server -> WorldEvents.storms().forEach(ManaStorm::stop));
		context.waitTicks(5);
		world.getServer().runCommand("kill @e[type=item]");
		return failure;
	}

	/** Out on a ley line (anywhere near spawn one runs strong), about 5% of catches. */
	private static String leyLine(TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			long seed = LeyWalker.seed(level);
			Vec3 line = null;
			for (int dx = -1024; dx <= 1024 && line == null; dx += 4) {
				for (int dz = -1024; dz <= 1024 && line == null; dz += 4) {
					double x = stage.getX() + dx + 0.5;
					double z = stage.getZ() + dz + 0.5;
					if (LeyLines.strength(seed, x, z) >= LeyWalker.ON_LINE) {
						line = new Vec3(x, 63.9, z);
					}
				}
			}
			if (line == null) {
				// Ley lines are thin and come and go; a world without one this near spawn is rare, and not a failure.
				Wildercord.LOGGER.warn("No ley line within 1024 blocks of the stage: the ley line catch wasn't checked");
				return null;
			}
			if (!Fishing.nearLey(level, line) || Fishing.underStorm(level, line)) {
				return "the bobber should be on a ley line, not under a storm";
			}
			Tangled run = tangled(level, line, hook(level, line, null), CATCH_ROLLS);
			if (run.failure() != null) {
				return run.failure();
			}
			return run.share() >= 0.025 && run.share() <= 0.08 ? null
				: "on a ley line about 5% of catches should bring up a tangled rune (" + pct(run.share()) + " of " + CATCH_ROLLS + ")";
		});
	}

	/** A thunderstorm over the stage: about 5% of catches; and Current carries the player up into the rain and down without harm. */
	private static String thunder(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> server.setWeatherParameters(0, 6000, true, true));
		// Rain and thunder build up over a few seconds.
		context.waitTicks(140);
		String failure = world.getServer().computeOnServer(server -> {
			ServerLevel level = player(server).level();
			if (!level.isThundering() || !Fishing.inThunder(level, bobber())) {
				return "the thunderstorm should rain on the bobber (thundering " + level.isThundering() + ")";
			}
			Tangled run = tangled(level, bobber(), hook(level, bobber(), null), CATCH_ROLLS);
			if (run.failure() != null) {
				return run.failure();
			}
			return run.share() >= 0.025 && run.share() <= 0.08 ? null
				: "in a thunderstorm about 5% of catches should bring up a tangled rune (" + pct(run.share()) + " of " + CATCH_ROLLS + ")";
		});
		if (failure == null) {
			failure = currentInRain(context, world);
		}
		world.getServer().runOnServer(server -> server.setWeatherParameters(6000, 0, false, false));
		context.waitTicks(140);
		return failure;
	}

	// ------------------------------------------------------------------ the fishing runes

	/** Straight up out of the rain, then down again: no fall damage on landing, and fall damage back afterwards. */
	private static String currentInRain(ClientGameTestContext context, TestSingleplayerContext world) {
		double[] start = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			stand(player, stage.getX() + 2.5, stage.getZ() + 0.5, 0.0F, -90.0F);
			return new double[] {player.getY()};
		});
		context.waitTicks(10);
		String cast = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			if (!player.isInWaterOrRain()) {
				return "the rain should be falling on the player";
			}
			player.setHealth(player.getMaxHealth());
			return cast(player, Runes.SELF, Runes.CURRENT);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(12);
		double high = world.getServer().computeOnServer(server -> player(server).getY());
		if (high - start[0] < 6.0) {
			return "in the rain a current should carry the player up (rose " + String.format("%.1f", high - start[0]) + " blocks)";
		}
		context.waitTicks(80);
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			AttributeInstance fall = player.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER);
			if (!player.onGround()) {
				return "the player should have landed again";
			}
			if (player.getHealth() < player.getMaxHealth() - 0.5F) {
				return "a current's rider should land without fall damage (health " + player.getHealth() + ")";
			}
			if (fall != null && fall.getModifier(Wildercord.id("current")) != null) {
				return "fall damage should come back once the rider has landed";
			}
			return null;
		});
	}

	/** Beam · Tidehook at a husk 10 blocks off: 4 damage, soaked, and reeled in to the caster's feet. */
	private static String tidehook(ClientGameTestContext context, TestSingleplayerContext world) {
		int husk = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			stand(player, stage.getX() + 6.5, stage.getZ() - 8.5, 0.0F, 0.0F);
			Mob mob = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
			if (mob == null) {
				return -1;
			}
			mob.snapTo(stage.getX() + 6.5, STAGE_Y, stage.getZ() + 1.5, 180, 0);
			// With its wits (so it falls and slides like any creature) but no legs of its own.
			mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
			mob.addTag("wildercord.fishing");
			// Never a random Runebound (it would have more health and cast back): tests pick their monsters.
			mob.addTag("wildercord.rolled");
			level.addFreshEntity(mob);
			return mob.getId();
		});
		if (husk < 0) {
			return "couldn't make the husk";
		}
		context.waitTicks(5);
		String cast = world.getServer().computeOnServer(server -> cast(player(server), Runes.BEAM, Runes.TIDEHOOK));
		if (cast != null) {
			world.getServer().runCommand("kill @e[tag=wildercord.fishing]");
			return cast;
		}
		context.waitTicks(35);
		String failure = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			if (!(player.level().getEntity(husk) instanceof LivingEntity mob) || !mob.isAlive()) {
				return "the husk should survive a Tidehook (4 damage)";
			}
			double distance = Math.hypot(mob.getX() - player.getX(), mob.getZ() - player.getZ());
			if (distance > 4.5) {
				return "the husk should be reeled in to the caster (still " + String.format("%.1f", distance) + " blocks off, from 10)";
			}
			if (mob.getHealth() > mob.getMaxHealth() - 3.0F) {
				return "the hook should deal 4 damage (health " + mob.getHealth() + ")";
			}
			return Reactions.has(mob, Reactions.Mark.SOAKED) ? null : "the husk should be left soaked";
		});
		world.getServer().runCommand("kill @e[tag=wildercord.fishing]");
		world.getServer().runCommand("kill @e[type=item]");
		context.waitTicks(10);
		return failure;
	}

	/** Self · Current in the channel: the player is carried along the water, and fall damage comes back after. */
	private static String currentInWater(ClientGameTestContext context, TestSingleplayerContext world) {
		double[] start = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			stand(player, stage.getX() - 6.5, stage.getZ() - 8.5, 0.0F, 0.0F);
			return new double[] {player.getX(), player.getZ()};
		});
		context.waitTicks(10);
		String cast = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			if (!player.isInWater()) {
				return "the player should stand in the channel's water";
			}
			return cast(player, Runes.SELF, Runes.CURRENT);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(12);
		String moved = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			double along = player.getZ() - start[1];
			if (along < 5.0) {
				return "a current should carry the player along the water (moved " + String.format("%.1f", along) + " blocks south)";
			}
			return Math.abs(player.getX() - start[0]) < 2.0 ? null : "the current should carry the player the way they look";
		});
		if (moved != null) {
			return moved;
		}
		context.waitTicks(20);
		return world.getServer().computeOnServer(server -> {
			AttributeInstance fall = player(server).getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER);
			return fall == null || fall.getModifier(Wildercord.id("current")) == null ? null : "fall damage should come back once the rider is in the water";
		});
	}

	/** Self · Current on the stone, in clear weather: it fizzles, and the player stays put. */
	private static String currentDry(ClientGameTestContext context, TestSingleplayerContext world) {
		double[] start = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			stand(player, stage.getX() + 2.5, stage.getZ() + 0.5, 0.0F, 0.0F);
			return new double[] {player.getX(), player.getZ()};
		});
		context.waitTicks(10);
		String cast = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			if (player.isInWaterOrRain()) {
				return "the stage should be dry by now";
			}
			return cast(player, Runes.SELF, Runes.CURRENT);
		});
		if (cast != null) {
			return cast;
		}
		context.waitTicks(12);
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			double moved = Math.hypot(player.getX() - start[0], player.getZ() - start[1]);
			if (moved > 1.0) {
				return "on dry land Current should fizzle, not move the player (moved " + String.format("%.1f", moved) + ")";
			}
			AttributeInstance fall = player.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER);
			return fall == null || fall.getModifier(Wildercord.id("current")) == null ? null : "a fizzled Current shouldn't touch fall damage";
		});
	}
}
