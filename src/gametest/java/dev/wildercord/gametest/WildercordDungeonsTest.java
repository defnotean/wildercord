package dev.wildercord.gametest;

import dev.wildercord.cast.CinderWarden;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.Shields;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.cast.StarEater;
import dev.wildercord.cast.TideScribe;
import dev.wildercord.content.WildercordBlocks;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.content.dungeons.DungeonAltarBlock;
import dev.wildercord.content.dungeons.DungeonAltarBlockEntity;
import dev.wildercord.player.Heart;
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
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The three dimension dungeons, each built where it belongs with {@code /place structure}: the
 * Ember Sanctum in the Nether, the Astral Observatory in the End and the Drowned Scriptorium in the
 * Overworld (on land here, which the command allows). Each is checked for its altar, its three seal
 * doors, its chests and the stone it's built from; then its boss is woken and its one mechanic
 * tried with real spells: the Cinder Warden shrugs off plain blows and single spells and breaks
 * under a reaction; the Star-Eater turns a spell back while shielded, loses its shield to a parried
 * shard and to a costlier spell; the Tide Scribe's flood carries a storm to everything wading in it,
 * and freezes under frost.
 *
 * <p>Runs in the full suite; skipped when {@code WILDERCORD_TOUR_ONLY} or {@code WILDERCORD_CORDS_ONLY} is set.</p>
 */
public class WildercordDungeonsTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).create()) {
			context.waitTicks(60);
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule spawn_mobs false");
			setup(world);
			List<String> failures = new ArrayList<>();
			try {
				emberSanctum(context, world);
			} catch (AssertionError e) {
				failures.add("Ember Sanctum: " + e.getMessage());
			}
			try {
				astralObservatory(context, world);
			} catch (AssertionError e) {
				failures.add("Astral Observatory: " + e.getMessage());
			}
			try {
				drownedScriptorium(context, world);
			} catch (AssertionError e) {
				failures.add("Drowned Scriptorium: " + e.getMessage());
			}
			if (!failures.isEmpty()) {
				throw new AssertionError("The dungeons went wrong:\n  " + String.join("\n  ", failures));
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

	private static void setup(TestSingleplayerContext world) {
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
		});
	}

	/** Sends the player to another dimension (or elsewhere in this one), hovering in creative flight. */
	private static void travel(TestSingleplayerContext world, ResourceKey<Level> dimension, Vec3 at) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = server.getLevel(dimension);
			player.setGameMode(GameType.CREATIVE);
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
			player.teleportTo(level, at.x, at.y, at.z, Set.<Relative>of(), 0, 0, false);
		});
	}

	/** Stands the player at {@code feet}, looking at the middle of {@code target}. */
	private static void aimFrom(ServerPlayer player, Vec3 feet, Entity target) {
		Vec3 eye = feet.add(0, player.getEyeHeight(), 0);
		Vec3 d = target.getBoundingBox().getCenter().subtract(eye);
		float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
		float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
		player.teleportTo((ServerLevel) target.level(), feet.x, feet.y, feet.z, Set.<Relative>of(), yaw, pitch, false);
	}

	/** Threads a spell onto the first slot and casts it, ready and with mana to spare. */
	private static void cast(ServerPlayer player, RuneDef... runes) {
		SpellCaster.edit(player, 0, List.of());
		SpellCaster.edit(player, 0, ids(runes));
		Spellbooks.setReadyAt(player, 0, 0);
		Spellbooks.setMana(player, 400);
		SpellCaster.cast(player, 0);
	}

	/** The dungeon altar of {@code kind} within {@code radius} blocks (horizontally) of {@code around}, found by its block entity. */
	private static BlockPos findAltar(ServerLevel level, BlockPos around, int radius, DungeonAltarBlock.Kind kind) {
		int c0x = (around.getX() - radius) >> 4;
		int c1x = (around.getX() + radius) >> 4;
		int c0z = (around.getZ() - radius) >> 4;
		int c1z = (around.getZ() + radius) >> 4;
		for (int cx = c0x; cx <= c1x; cx++) {
			for (int cz = c0z; cz <= c1z; cz++) {
				if (!level.hasChunk(cx, cz)) {
					continue;
				}
				for (BlockEntity entity : level.getChunk(cx, cz).getBlockEntities().values()) {
					if (entity instanceof DungeonAltarBlockEntity && entity.getBlockState().getValue(DungeonAltarBlock.KIND) == kind) {
						return entity.getBlockPos();
					}
				}
			}
		}
		return null;
	}

	/** How many of each block there are around the altar (72 across either way, 14 down and 22 up: every dungeon fits). */
	private static Map<Block, Integer> census(ServerLevel level, BlockPos altar) {
		Map<Block, Integer> counts = new HashMap<>();
		for (BlockPos pos : BlockPos.betweenClosed(altar.offset(-72, -14, -72), altar.offset(72, 22, 72))) {
			BlockState state = level.getBlockState(pos);
			if (!state.isAir()) {
				counts.merge(state.getBlock(), 1, Integer::sum);
			}
		}
		return counts;
	}

	/** The parts every dungeon has: three seal doors and three chests. */
	private static void doorsAndChests(Map<Block, Integer> census) {
		int seals = census.getOrDefault(WildercordBlocks.RUNE_SEAL, 0);
		check(seals >= 60, "it should have three seal doors (found " + seals + " seal blocks)");
		int chests = census.getOrDefault(Blocks.CHEST, 0);
		check(chests >= 3, "it should have a hall chest and two in the vault (found " + chests + ")");
	}

	private static boolean has(Map<Block, Integer> census, Block block) {
		return census.getOrDefault(block, 0) > 0;
	}

	/** Builds a dungeon with /place, in its dimension, and finds its altar. */
	private static BlockPos build(ClientGameTestContext context, TestSingleplayerContext world, ResourceKey<Level> dimension, String id,
			BlockPos at, DungeonAltarBlock.Kind kind) {
		String dim = dimension.identifier().toString();
		travel(world, dimension, Vec3.atCenterOf(at).add(0, 20, 0));
		world.getServer().runCommand("execute in " + dim + " run forceload add " + (at.getX() - 96) + " " + (at.getZ() - 96) + " "
			+ (at.getX() + 96) + " " + (at.getZ() + 96));
		// Every chunk it could reach must be there before the command will build into them.
		world.getServer().waitFor(server -> {
			ServerLevel level = server.getLevel(dimension);
			for (int cx = (at.getX() - 96) >> 4; cx <= (at.getX() + 96) >> 4; cx++) {
				for (int cz = (at.getZ() - 96) >> 4; cz <= (at.getZ() + 96) >> 4; cz++) {
					if (!level.hasChunk(cx, cz)) {
						return false;
					}
				}
			}
			return true;
		}, 2400);
		context.waitTicks(20);
		world.getServer().runCommand("execute in " + dim + " run place structure wildercord:" + id + " " + at.getX() + " " + at.getY() + " " + at.getZ());
		context.waitTicks(40);
		BlockPos altar = world.getServer().computeOnServer(server -> findAltar(server.getLevel(dimension), at, 112, kind));
		check(altar != null, "/place structure wildercord:" + id + " should build it, with its altar");
		// Its boss is woken by hand in each test, so the altar mustn't wake another when the player comes near.
		world.getServer().runOnServer(server -> {
			ServerLevel level = server.getLevel(dimension);
			level.setBlock(altar, level.getBlockState(altar).setValue(DungeonAltarBlock.AWAKE, true), Block.UPDATE_ALL);
		});
		return altar;
	}

	private static void done(TestSingleplayerContext world, ResourceKey<Level> dimension) {
		String dim = dimension.identifier().toString();
		world.getServer().runCommand("execute in " + dim + " run kill @e[type=!player]");
		world.getServer().runCommand("execute in " + dim + " run forceload remove all");
	}

	// ------------------------------------------------------------------ the Ember Sanctum and the Cinder Warden

	private static void emberSanctum(ClientGameTestContext context, TestSingleplayerContext world) {
		ResourceKey<Level> nether = Level.NETHER;
		BlockPos altar = build(context, world, nether, "ember_sanctum", new BlockPos(8, 64, 8), DungeonAltarBlock.Kind.CINDER);
		world.getServer().runOnServer(server -> {
			ServerLevel level = server.getLevel(nether);
			Map<Block, Integer> census = census(level, altar);
			doorsAndChests(census);
			check(has(census, Blocks.GILDED_BLACKSTONE), "it should be built with gilded blackstone");
			check(has(census, Blocks.MAGMA_BLOCK), "it should have magma in its floors");
			check(has(census, Blocks.LAVA), "it should have lava channels");
			check(has(census, Blocks.CAMPFIRE), "its braziers should burn");
			check(level.getBlockState(altar).getValue(DungeonAltarBlock.KIND) == DungeonAltarBlock.Kind.CINDER, "its altar should be the forge-altar");
		});
		// The Warden: plain blows do nothing, a single-element spell a tenth, a reaction the lot.
		world.getServer().runOnServer(server -> {
			ServerLevel level = server.getLevel(nether);
			CinderWarden.rise(level, altar);
		});
		context.waitTicks(10);
		int wardenId = world.getServer().computeOnServer(server -> {
			ServerLevel level = server.getLevel(nether);
			List<CinderWarden> found = level.getEntitiesOfClass(CinderWarden.class, new AABB(altar).inflate(8));
			check(!found.isEmpty(), "the Cinder Warden should rise from its altar");
			CinderWarden warden = found.getFirst();
			warden.setNoAi(true);
			ServerPlayer player = player(server);
			player.setGameMode(GameType.SURVIVAL);
			aimFrom(player, Spots.stand(level, warden.position(), 6), warden);
			float before = warden.getHealth();
			warden.hurtServer(level, level.damageSources().playerAttack(player), 20);
			check(warden.getHealth() == before, "a plain blow shouldn't hurt the Cinder Warden (it lost " + (before - warden.getHealth()) + ")");
			return warden.getId();
		});
		float before = health(world, nether, wardenId);
		world.getServer().runOnServer(server -> cast(player(server), Runes.BOLT, Runes.FIRE));
		world.getServer().waitFor(server -> health(server, nether, wardenId) < before, 60);
		context.waitTicks(10);
		float single = before - health(world, nether, wardenId);
		check(single > 0 && single < 3, "a single Fire Bolt should get a tenth through its armour (it lost " + single + ")");
		// Frozen, then burnt: Shatter.
		float mid = health(world, nether, wardenId);
		world.getServer().runOnServer(server -> {
			Entity warden = server.getLevel(nether).getEntity(wardenId);
			Reactions.mark(warden, Reactions.Mark.FROZEN);
			cast(player(server), Runes.BOLT, Runes.FIRE);
		});
		world.getServer().waitFor(server -> health(server, nether, wardenId) < mid, 60);
		context.waitTicks(10);
		float reacted = mid - health(world, nether, wardenId);
		check(reacted >= 4 && reacted > single * 4, "a Shatter should break through its armour (it lost " + reacted + ", a plain bolt " + single + ")");
		world.getServer().runOnServer(server -> {
			check(Heart.discovered(player(server), "reaction:shatter"), "setting off Shatter on it should go in the Grimoire");
			player(server).setGameMode(GameType.CREATIVE);
		});
		done(world, nether);
	}

	private static float health(TestSingleplayerContext world, ResourceKey<Level> dimension, int id) {
		return world.getServer().computeOnServer(server -> health(server, dimension, id));
	}

	private static float health(MinecraftServer server, ResourceKey<Level> dimension, int id) {
		Entity entity = server.getLevel(dimension).getEntity(id);
		return entity instanceof LivingEntity living ? living.getHealth() : 0;
	}

	// ------------------------------------------------------------------ the Astral Observatory and the Star-Eater

	private static void astralObservatory(ClientGameTestContext context, TestSingleplayerContext world) {
		ResourceKey<Level> end = Level.END;
		BlockPos altar = build(context, world, end, "astral_observatory", new BlockPos(8, 64, 8), DungeonAltarBlock.Kind.ASTRAL);
		world.getServer().runCommand("execute in minecraft:the_end run kill @e[type=minecraft:ender_dragon]");
		world.getServer().runOnServer(server -> {
			ServerLevel level = server.getLevel(end);
			Map<Block, Integer> census = census(level, altar);
			doorsAndChests(census);
			check(has(census, Blocks.PURPUR_BLOCK), "it should be built of purpur");
			check(has(census, Blocks.END_ROD), "it should be lit with end rods");
			check(has(census, Blocks.PEARLESCENT_FROGLIGHT), "its floor should be a star map");
			check(has(census, Blocks.STAINED_GLASS.purple()), "it should have a glass dome");
		});
		world.getServer().runOnServer(server -> StarEater.rise(server.getLevel(end), altar));
		context.waitTicks(10);
		int eaterId = world.getServer().computeOnServer(server -> {
			ServerLevel level = server.getLevel(end);
			List<StarEater> found = level.getEntitiesOfClass(StarEater.class, new AABB(altar).inflate(10));
			check(!found.isEmpty(), "the Star-Eater should rise over its altar");
			StarEater eater = found.getFirst();
			eater.setNoAi(true);
			check(eater.shielded(), "the Star-Eater should wake with its shard shield up");
			ServerPlayer player = player(server);
			player.setGameMode(GameType.SURVIVAL);
			player.setHealth(player.getMaxHealth());
			aimFrom(player, Spots.stand(level, eater.position(), 7), eater);
			return eater.getId();
		});
		// A light spell is turned back on whoever cast it.
		double light = SpellCompiler.compile(List.of(Runes.BOLT, Runes.HARM)).cost();
		float strength = world.getServer().computeOnServer(server -> ((StarEater) server.getLevel(end).getEntity(eaterId)).shieldStrength());
		check(light <= strength, "a Harm Bolt (" + light + " mana) should weigh no more than its shield (" + strength + ")");
		world.getServer().runOnServer(server -> cast(player(server), Runes.BOLT, Runes.HARM));
		int reflected = world.getServer().waitFor(server -> ((StarEater) server.getLevel(end).getEntity(eaterId)).reflections() > 0, 60);
		check(reflected >= 0, "a spell cast at the shielded Star-Eater should be turned back");
		world.getServer().waitFor(server -> player(server).getHealth() < player(server).getMaxHealth(), 80);
		world.getServer().runOnServer(server -> {
			StarEater eater = (StarEater) server.getLevel(end).getEntity(eaterId);
			check(eater.getHealth() >= eater.getMaxHealth(), "a reflected spell shouldn't hurt the Star-Eater");
			check(eater.shielded(), "its shield should hold after turning a spell back");
			check(player(server).getHealth() < player(server).getMaxHealth(), "the reflected spell should come back and hit its caster");
			player(server).setGameMode(GameType.CREATIVE);
		});
		// A parried star shard flies home and breaks a weakened shield.
		world.getServer().runOnServer(server -> {
			StarEater eater = (StarEater) server.getLevel(end).getEntity(eaterId);
			eater.setNoAi(false);
			Shields.give(eater, 12, 20 * 60, List.of(Runes.SHIELD.id()));
			eater.throwShard(player(server));
			check(eater.parryFirst(player(server)), "a star shard should be in flight to parry");
		});
		int parried = world.getServer().waitFor(server -> !((StarEater) server.getLevel(end).getEntity(eaterId)).shielded(), 100);
		check(parried >= 0, "a parried shard should fly home and break a 12-mana shield");
		world.getServer().runOnServer(server -> {
			StarEater eater = (StarEater) server.getLevel(end).getEntity(eaterId);
			check(eater.parries() >= 1, "the parry should count");
			eater.setNoAi(true);
		});
		// A spell that costs more than the shield breaks it and goes through.
		List<RuneDef> heavy = List.of(Runes.BOLT, Runes.HARM, Runes.AMPLIFY, Runes.AMPLIFY, Runes.AMPLIFY, Runes.AMPLIFY);
		double big = SpellCompiler.compile(heavy).cost();
		check(big > 20, "the heavy bolt (" + big + " mana) should outweigh a 20-mana shield");
		float full = world.getServer().computeOnServer(server -> {
			StarEater eater = (StarEater) server.getLevel(end).getEntity(eaterId);
			Shields.give(eater, 20, 20 * 60, List.of(Runes.SHIELD.id()));
			aimFrom(player(server), Spots.stand(server.getLevel(end), eater.position(), 7), eater);
			return eater.getHealth();
		});
		world.getServer().runOnServer(server -> cast(player(server), heavy.toArray(RuneDef[]::new)));
		int broke = world.getServer().waitFor(server -> !((StarEater) server.getLevel(end).getEntity(eaterId)).shielded(), 60);
		context.waitTicks(10);
		check(broke >= 0, "a spell costing more than its shield should break it");
		check(health(world, end, eaterId) < full, "the spell that broke its shield should go through and hurt it");
		done(world, end);
	}

	// ------------------------------------------------------------------ the Drowned Scriptorium and the Tide Scribe

	private static void drownedScriptorium(ClientGameTestContext context, TestSingleplayerContext world) {
		ResourceKey<Level> overworld = Level.OVERWORLD;
		// Well away from where the world began; land or sea, the command builds it all the same.
		BlockPos origin = new BlockPos(408, 64, 408);
		BlockPos altar = build(context, world, overworld, "drowned_scriptorium", origin, DungeonAltarBlock.Kind.TIDE);
		world.getServer().runOnServer(server -> {
			ServerLevel level = server.getLevel(overworld);
			Map<Block, Integer> census = census(level, altar);
			doorsAndChests(census);
			check(has(census, Blocks.PRISMARINE_BRICKS), "it should be built of prismarine");
			check(has(census, Blocks.SEA_LANTERN), "it should be lit with sea lanterns");
			check(has(census, Blocks.CONDUIT), "a conduit should hang over its core");
			check(has(census, Blocks.KELP) || has(census, Blocks.KELP_PLANT), "its shelves should hold kelp");
			check(pitWater(level, altar) == 0, "the arena should start dry (found " + pitWater(level, altar) + " water: " + pitWaterWhere(level, altar) + ")");
		});
		world.getServer().runOnServer(server -> TideScribe.rise(server.getLevel(overworld), altar));
		context.waitTicks(10);
		int[] ids = world.getServer().computeOnServer(server -> {
			ServerLevel level = server.getLevel(overworld);
			List<TideScribe> found = level.getEntitiesOfClass(TideScribe.class, new AABB(altar).inflate(8));
			check(!found.isEmpty(), "the Tide Scribe should rise from its core");
			TideScribe scribe = found.getFirst();
			scribe.setNoAi(true);
			scribe.forceTide(true);
			check(pitWater(level, altar) > 300, "the tide should flood the pit (found " + pitWater(level, altar) + " water)");
			scribe.teleportTo(altar.getX() + 3.5, altar.getY() + 0.2, altar.getZ() + 0.5);
			// Someone else wading in the flood: a pig.
			Pig pig = EntityTypes.PIG.create(level, EntitySpawnReason.COMMAND);
			check(pig != null, "a pig should spawn");
			pig.snapTo(altar.getX() - 3.5, altar.getY() + 0.2, altar.getZ() + 0.5, 0, 0);
			pig.setNoAi(true);
			level.addFreshEntity(pig);
			return new int[] {scribe.getId(), pig.getId()};
		});
		context.waitTicks(10);
		// Stand dry, on a pedestal, and send a storm into the water.
		world.getServer().runOnServer(server -> {
			ServerLevel level = server.getLevel(overworld);
			TideScribe scribe = (TideScribe) level.getEntity(ids[0]);
			check(scribe.isInWater(), "the Tide Scribe should be in the flood");
			BlockPos pedestal = null;
			for (BlockPos pos : BlockPos.betweenClosed(altar.offset(-9, 2, -9), altar.offset(9, 2, 9))) {
				if (level.getBlockState(pos).is(Blocks.SEA_LANTERN) && level.getBlockState(pos.above()).isAir() && pos.distSqr(altar) > 36) {
					pedestal = pos.immutable();
					break;
				}
			}
			check(pedestal != null, "the pit should have pedestals to stand on");
			aimFrom(player(server), Vec3.atBottomCenterOf(pedestal.above()), scribe);
		});
		float scribeBefore = health(world, overworld, ids[0]);
		float pigBefore = health(world, overworld, ids[1]);
		world.getServer().runOnServer(server -> cast(player(server), Runes.BOLT, Runes.SHOCK));
		int conducted = world.getServer().waitFor(server -> ((TideScribe) server.getLevel(overworld).getEntity(ids[0])).conductions() > 0, 60);
		context.waitTicks(5);
		check(conducted >= 0, "a Shock Bolt into the flooded arena should be carried by the water");
		check(health(world, overworld, ids[1]) < pigBefore, "the shock should reach the pig wading in the flood too");
		float lost = scribeBefore - health(world, overworld, ids[0]);
		check(lost >= 10, "the Tide Scribe should take the brunt of a shock through its own water (it lost " + lost + ")");
		// Frost freezes the flood, and strands the Scribe if the ice closes round it (once it's over the shock's stagger).
		context.waitTicks(50);
		world.getServer().runOnServer(server -> {
			TideScribe scribe = (TideScribe) server.getLevel(overworld).getEntity(ids[0]);
			aimFrom(player(server), player(server).position(), scribe);
			cast(player(server), Runes.BOLT, Runes.FROST);
		});
		int froze = world.getServer().waitFor(server -> ((TideScribe) server.getLevel(overworld).getEntity(ids[0])).icedCells() > 0, 60);
		check(froze >= 0, "a Frost Bolt into the flood should freeze it into ice");
		world.getServer().runOnServer(server -> {
			ServerLevel level = server.getLevel(overworld);
			TideScribe scribe = (TideScribe) level.getEntity(ids[0]);
			check(scribe.stranded(), "ice closing round the Tide Scribe should strand it");
			scribe.forceTide(false);
			check(pitWater(level, altar) == 0, "the ebb should drain the pit, ice and all (found " + pitWater(level, altar) + " water)");
		});
		done(world, overworld);
	}

	/** Where the water in the pit is, relative to the altar, and what holds it (for a failure message). */
	private static String pitWaterWhere(ServerLevel level, BlockPos altar) {
		StringBuilder out = new StringBuilder();
		int r = (int) Math.ceil(TideScribe.PIT_RADIUS);
		for (BlockPos pos : BlockPos.betweenClosed(altar.offset(-r, 0, -r), altar.offset(r, 1, r))) {
			BlockState state = level.getBlockState(pos);
			if (inPit(pos, altar) && (state.getFluidState().is(FluidTags.WATER) || state.is(Blocks.ICE))) {
				BlockPos d = pos.subtract(altar);
				out.append(String.format("[%d,%d,%d %s] ", d.getX(), d.getY(), d.getZ(), state.getBlock().getName().getString()));
			}
		}
		return out.toString().trim();
	}

	/** Inside the round pit (the square around it takes in the wall, and the sea past it). */
	private static boolean inPit(BlockPos pos, BlockPos altar) {
		double dx = pos.getX() - altar.getX();
		double dz = pos.getZ() - altar.getZ();
		return dx * dx + dz * dz <= TideScribe.PIT_RADIUS * TideScribe.PIT_RADIUS;
	}

	/** Water (and ice) in the pit's two flood layers. */
	private static int pitWater(ServerLevel level, BlockPos altar) {
		int n = 0;
		int r = (int) Math.ceil(TideScribe.PIT_RADIUS);
		for (BlockPos pos : BlockPos.betweenClosed(altar.offset(-r, 0, -r), altar.offset(r, 1, r))) {
			BlockState state = level.getBlockState(pos);
			if (inPit(pos, altar) && (state.getFluidState().is(FluidTags.WATER) || state.is(Blocks.ICE))) {
				n++;
			}
		}
		return n;
	}

	/** Where to stand: a spot {@code distance} blocks from a point, on the same level, facing it. */
	private static final class Spots {
		private Spots() {}

		static Vec3 stand(ServerLevel level, Vec3 near, double distance) {
			for (int k = 0; k < 8; k++) {
				double a = Math.PI * 2 * k / 8;
				Vec3 spot = new Vec3(Math.floor(near.x + Math.cos(a) * distance) + 0.5, Math.floor(near.y), Math.floor(near.z + Math.sin(a) * distance) + 0.5);
				for (int dy = 3; dy >= -4; dy--) {
					BlockPos feet = BlockPos.containing(spot.x, spot.y + dy, spot.z);
					if (level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir() && !level.getBlockState(feet.below()).isAir()) {
						return Vec3.atBottomCenterOf(feet);
					}
				}
			}
			return near.add(distance, 0, 0);
		}
	}
}
