package dev.wildercord.aura.world;

import dev.wildercord.cast.TemporaryBlocks;
import dev.wildercord.config.Config;
import dev.wildercord.config.WildercordConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Where duelists come from: now and then, by day, one wanders in near a player in the overworld, as the wandering trader does,
 * at one of three kinds of place:
 * <ul>
 *   <li><b>near a village</b>: a few blocks from its bell, if one is within reach;</li>
 *   <li><b>on a road</b>: a dirt path out in the land;</li>
 *   <li><b>at a small camp</b>: anywhere open, where it lights a campfire (a borrowed block, written down so it goes with the
 *       duelist and drops nothing; only where mobs may change the world, and the server allows camps).</li>
 * </ul>
 * Rarely ({@link AuraWorldRules#DUELIST_CHANCE} a minute at a rate of 1), never two near one another, and never more loaded at
 * once than the server allows. Each moves on after twenty minutes.
 */
public final class DuelistSpawner {
	private DuelistSpawner() {}

	/** The kind of place a duelist came to. */
	public enum Place { VILLAGE, ROAD, CAMP }

	/** Where a duelist came, and what kind of place it is. */
	public record Spot(BlockPos pos, Place place) {}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(DuelistSpawner::tick);
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % AuraWorldRules.DUELIST_PERIOD != 0) {
			return;
		}
		WildercordConfig.AuraWorldSettings settings = Config.get().auraWorld();
		ServerLevel level = server.overworld();
		if (!settings.duelistsSpawn() || !level.isBrightOutside() || !level.getGameRules().get(GameRules.SPAWN_MOBS)) {
			return;
		}
		List<ServerPlayer> players = level.players().stream().filter(p -> !p.isSpectator()).toList();
		RandomSource random = level.getRandom();
		if (players.isEmpty() || random.nextDouble() >= AuraWorldRules.chance(AuraWorldRules.DUELIST_CHANCE, settings.duelistSpawnRate())) {
			return;
		}
		if (loaded(server) >= settings.maxDuelists()) {
			return;
		}
		spawnNear(level, players.get(random.nextInt(players.size())), settings.duelistCamps(), random);
	}

	/** How many duelists are loaded across the server. */
	public static int loaded(MinecraftServer server) {
		int n = 0;
		for (ServerLevel level : server.getAllLevels()) {
			n += level.getEntities(AuraWorld.DUELIST, d -> d.isAlive()).size();
		}
		return n;
	}

	/**
	 * Brings a duelist in near {@code player}, at a village, a road or a camp, if there's room for one (none within
	 * {@link AuraWorldRules#DUELIST_APART} blocks) and a place to stand. Returns it, or null.
	 */
	public static @Nullable Duelist spawnNear(ServerLevel level, ServerPlayer player, boolean camps, RandomSource random) {
		if (!level.getEntities(AuraWorld.DUELIST, d -> d.isAlive() && d.distanceTo(player) < AuraWorldRules.DUELIST_APART).isEmpty()) {
			return null;
		}
		Spot spot = village(level, player.blockPosition(), random);
		if (spot == null) {
			spot = road(level, player.blockPosition(), random);
		}
		if (spot == null) {
			spot = camp(level, player.blockPosition(), random);
		}
		return spot == null ? null : spawnAt(level, spot, camps, random);
	}

	/** Brings a duelist to a spot found for it (and, at a camp, lights its fire). */
	public static @Nullable Duelist spawnAt(ServerLevel level, Spot spot, boolean camps, RandomSource random) {
		Duelist duelist = AuraWorld.DUELIST.create(level, EntitySpawnReason.EVENT);
		if (duelist == null) {
			return null;
		}
		BlockPos at = spot.pos();
		duelist.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360, 0);
		duelist.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.EVENT, null);
		if (spot.place() == Place.CAMP && camps && level.getGameRules().get(GameRules.MOB_GRIEFING)) {
			duelist.camp = lightFire(level, at, duelist.stayUntil + 200);
		}
		level.addFreshEntity(duelist);
		return duelist;
	}

	// ------------------------------------------------------------------ places

	/** A few blocks from the nearest village bell within 96 blocks, if there is one. */
	static @Nullable Spot village(ServerLevel level, BlockPos near, RandomSource random) {
		Optional<BlockPos> bell = level.getPoiManager().findClosest(p -> p.is(PoiTypes.MEETING), near, 96, PoiManager.Occupancy.ANY);
		if (bell.isEmpty()) {
			return null;
		}
		for (int i = 0; i < 16; i++) {
			double a = random.nextDouble() * Math.PI * 2;
			double r = 4 + random.nextDouble() * 6;
			BlockPos column = bell.get().offset((int) Math.round(Math.cos(a) * r), 0, (int) Math.round(Math.sin(a) * r));
			BlockPos stand = surface(level, column);
			if (stand != null && Math.abs(stand.getY() - bell.get().getY()) < 6) {
				return new Spot(stand, Place.VILLAGE);
			}
		}
		return null;
	}

	/** A dirt path 16 to 48 blocks from the player. */
	static @Nullable Spot road(ServerLevel level, BlockPos near, RandomSource random) {
		for (int i = 0; i < 40; i++) {
			BlockPos column = ring(near, 16, 48, random);
			if (!level.hasChunkAt(column)) {
				continue;
			}
			BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
			if (level.getBlockState(top.below()).is(Blocks.DIRT_PATH) && standable(level, top)) {
				return new Spot(top, Place.ROAD);
			}
		}
		return null;
	}

	/** Open ground under the sky 20 to 40 blocks from the player, room beside it for a fire. */
	static @Nullable Spot camp(ServerLevel level, BlockPos near, RandomSource random) {
		for (int i = 0; i < 24; i++) {
			BlockPos column = ring(near, 20, 40, random);
			BlockPos stand = surface(level, column);
			if (stand == null || !level.canSeeSky(stand)) {
				continue;
			}
			BlockState ground = level.getBlockState(stand.below());
			if (!ground.is(BlockTags.DIRT) && !ground.is(BlockTags.SAND) && !ground.is(Blocks.SNOW_BLOCK) && !ground.is(Blocks.GRAVEL)) {
				continue;
			}
			BlockPos fire = stand.offset(2, 0, 0);
			if (level.getBlockState(fire).isAir() && level.getBlockState(fire.below()).isFaceSturdy(level, fire.below(), net.minecraft.core.Direction.UP)) {
				return new Spot(stand, Place.CAMP);
			}
		}
		return null;
	}

	private static BlockPos ring(BlockPos near, int from, int to, RandomSource random) {
		double a = random.nextDouble() * Math.PI * 2;
		double r = from + random.nextDouble() * (to - from);
		return near.offset((int) Math.round(Math.cos(a) * r), 0, (int) Math.round(Math.sin(a) * r));
	}

	/** The spot on top of the ground in a column, if one can stand there: solid below, two blocks of air, no water. */
	static @Nullable BlockPos surface(ServerLevel level, BlockPos column) {
		if (!level.hasChunkAt(column)) {
			return null;
		}
		BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
		return standable(level, top) ? top : null;
	}

	private static boolean standable(ServerLevel level, BlockPos pos) {
		BlockPos below = pos.below();
		return level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP) && level.getBlockState(pos).isAir()
			&& level.getBlockState(pos.above()).isAir() && level.getFluidState(below).isEmpty();
	}

	/** A campfire two blocks from the duelist, borrowed until it goes (and a little after). Returns where, or null. */
	static @Nullable BlockPos lightFire(ServerLevel level, BlockPos stand, long until) {
		BlockPos fire = stand.offset(2, 0, 0);
		BlockState was = level.getBlockState(fire);
		if (!was.isAir()) {
			return null;
		}
		BlockState campfire = Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true).setValue(CampfireBlock.SIGNAL_FIRE, false);
		level.setBlockAndUpdate(fire, campfire);
		TemporaryBlocks.put(level, fire, campfire, was, until);
		return fire;
	}
}
