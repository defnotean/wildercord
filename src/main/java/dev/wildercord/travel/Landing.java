package dev.wildercord.travel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Finding somewhere safe to land, in the world: it tells {@link SafeSpots} what each block is, checks
 * a saved spot exactly first (so a home set on a slab lands on the slab), and otherwise searches round
 * it. Also finds the ground for {@code /spawn} and a random spot for {@code /rtp}.
 */
final class Landing {
	private Landing() {}

	/** How far below the feet something must hold a player up. */
	private static final double SUPPORT = 0.3;

	/**
	 * Where {@code player} can stand at or near {@code at} in {@code level}: {@code at} itself if it's
	 * safe, else the nearest safe block nearby, else null. Creative and spectator players land exactly
	 * where they asked (nothing can hurt them) unless {@code strict}, which also refuses water and leaves.
	 */
	static Vec3 near(ServerLevel level, Vec3 at, ServerPlayer player, boolean strict) {
		if (!level.getWorldBorder().isWithinBounds(at.x, at.z)) {
			return null;
		}
		if (!strict && (player.isCreative() || player.isSpectator())) {
			return at;
		}
		if (exact(level, at, player, strict)) {
			return at;
		}
		int[] found = SafeSpots.find(grid(level, player), Mth.floor(at.x), Mth.floor(at.y + 1.0E-3), Mth.floor(at.z), strict);
		return found == null ? null : feet(level, found[0], found[1], found[2]);
	}

	/** The ground at the top of column {@code x}, {@code z} (loading its chunk), if it's somewhere to stand. */
	static Vec3 surface(ServerLevel level, int x, int z, ServerPlayer player, boolean strict) {
		level.getChunk(x >> 4, z >> 4);
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		if (y <= level.getMinY() || y >= level.getMaxY()) {
			return null;
		}
		return near(level, new Vec3(x + 0.5, y, z + 0.5), player, strict);
	}

	/**
	 * A random safe spot on the surface within {@code radius} of {@code centre}: never water, lava, the
	 * top of a tree or inside anything. Oceans and rivers are passed over without loading anything, and
	 * at most {@link TravelRules#RTP_CHUNK_LOADS} chunks are loaded. Null if nothing turned up.
	 */
	static Vec3 random(ServerLevel level, BlockPos centre, int radius, RandomSource random, ServerPlayer player) {
		int loads = 0;
		for (int attempt = 0; attempt < TravelRules.RTP_TRIES && loads < TravelRules.RTP_CHUNK_LOADS; attempt++) {
			int[] xz = TravelRules.randomPoint(random.nextDouble(), random.nextDouble(), centre.getX(), centre.getZ(), radius);
			int x = xz[0];
			int z = xz[1];
			if (!level.getWorldBorder().isWithinBounds(x - 8, z - 8) || !level.getWorldBorder().isWithinBounds(x + 8, z + 8)) {
				continue;
			}
			// The biome is known without loading (or generating) the chunk.
			Holder<Biome> biome = level.getBiome(new BlockPos(x, level.getSeaLevel(), z));
			if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)) {
				continue;
			}
			loads++;
			Vec3 spot = surface(level, x, z, player, true);
			if (spot != null) {
				return spot;
			}
		}
		return null;
	}

	/** What a block is, as far as landing goes. */
	static SafeSpots.Cell cell(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		FluidState fluid = state.getFluidState();
		if (fluid.is(FluidTags.LAVA) || dangerous(state)) {
			return SafeSpots.Cell.DANGER;
		}
		if (state.is(BlockTags.LEAVES)) {
			return SafeSpots.Cell.LEAVES;
		}
		if (!state.getCollisionShape(level, pos).isEmpty()) {
			return SafeSpots.Cell.FLOOR;
		}
		return fluid.is(FluidTags.WATER) ? SafeSpots.Cell.WATER : SafeSpots.Cell.OPEN;
	}

	/** Blocks that hurt, trap or carry off whoever lands in or on them. */
	private static boolean dangerous(BlockState state) {
		return state.is(BlockTags.FIRE) || CampfireBlock.isLitCampfire(state) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CACTUS)
			|| state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE) || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.COBWEB)
			|| state.is(Blocks.POINTED_DRIPSTONE) || state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL) || state.is(Blocks.END_GATEWAY);
	}

	/**
	 * Whether {@code player} can stand exactly at {@code at}: not inside anything, held up by something
	 * just below, nothing harmful in or under them, and their head out of water (strict: no water or
	 * leaves at all).
	 */
	static boolean exact(ServerLevel level, Vec3 at, ServerPlayer player, boolean strict) {
		AABB box = box(player, at);
		if (!level.noCollision(player, box)) {
			return false;
		}
		if (level.noCollision(player, new AABB(box.minX, at.y - SUPPORT, box.minZ, box.maxX, at.y, box.maxZ))) {
			return false;
		}
		int x0 = Mth.floor(box.minX);
		int x1 = Mth.floor(box.maxX - 1.0E-6);
		int z0 = Mth.floor(box.minZ);
		int z1 = Mth.floor(box.maxZ - 1.0E-6);
		int y0 = Mth.floor(at.y - SUPPORT);
		int y1 = Mth.floor(box.maxY - 1.0E-6);
		for (BlockPos pos : BlockPos.betweenClosed(x0, y0, z0, x1, y1, z1)) {
			SafeSpots.Cell cell = cell(level, pos);
			if (cell == SafeSpots.Cell.DANGER || strict && (cell == SafeSpots.Cell.WATER || cell == SafeSpots.Cell.LEAVES)) {
				return false;
			}
		}
		return cell(level, BlockPos.containing(at.x, at.y + player.getEyeHeight(Pose.STANDING), at.z)) != SafeSpots.Cell.WATER;
	}

	/** Where a player's feet go standing in block {@code x}, {@code y}, {@code z}: its middle, on top of whatever is below. */
	static Vec3 feet(ServerLevel level, int x, int y, int z) {
		BlockPos below = new BlockPos(x, y - 1, z);
		VoxelShape shape = level.getBlockState(below).getCollisionShape(level, below);
		double top = shape.isEmpty() ? y : below.getY() + shape.max(Direction.Axis.Y);
		return new Vec3(x + 0.5, Math.max(top, y - 1.0), z + 0.5);
	}

	private static AABB box(ServerPlayer player, Vec3 feet) {
		return player.getDimensions(Pose.STANDING).makeBoundingBox(feet);
	}

	/** The world as {@link SafeSpots} sees it, with the exact check of {@code player}'s body as the last word. */
	private static SafeSpots.Grid grid(ServerLevel level, ServerPlayer player) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		return new SafeSpots.Grid() {
			@Override
			public SafeSpots.Cell cell(int x, int y, int z) {
				return Landing.cell(level, cursor.set(x, y, z));
			}

			@Override
			public boolean fits(int x, int y, int z) {
				Vec3 at = feet(level, x, y, z);
				return level.getWorldBorder().isWithinBounds(at.x, at.z) && level.noCollision(player, box(player, at));
			}
		};
	}
}
