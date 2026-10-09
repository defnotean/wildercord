package dev.wildercord.world.sites.wilds;

import dev.wildercord.world.dungeons.DungeonPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

import java.util.Optional;

/**
 * What the wild sites share. Each is one piece whose floor is its own y=0; its box reaches {@link #SINK} blocks lower,
 * so the footing it pours down to the ground (or into a cave floor) stays inside the box. The locators find a floor
 * the way the site needs: open dry ground, a cave's floor, or an End island's top.
 */
abstract class WildsPiece extends DungeonPiece {
	/** How far a site's footing reaches below its floor. */
	static final int SINK = 6;

	WildsPiece(StructurePieceType type, int x, int z, int width, int height, int depth, Direction facing) {
		super(type, x, 0, z, width, height + SINK, depth, facing);
	}

	WildsPiece(StructurePieceType type, CompoundTag tag) {
		super(type, tag);
	}

	/** Local y=0 is the floor: the box's lowest {@link #SINK} layers are footing. */
	@Override protected int getWorldY(int y) { return super.getWorldY(y + SINK); }

	static BlockState s(Block block) { return block.defaultBlockState(); }

	/** Solid footing under a rectangle, from the box's bottom up to just under the floor. */
	protected void footing(WorldGenLevel level, BoundingBox bb, int x0, int z0, int x1, int z1, BlockState state) {
		fill(level, bb, x0, -SINK, z0, x1, -1, z1, state);
	}

	protected void clear(WorldGenLevel level, BoundingBox bb, int x0, int y0, int z0, int x1, int y1, int z1) {
		fill(level, bb, x0, y0, z0, x1, y1, z1, AIR);
	}

	/** The local (dx, dz) step that points to the world's east, where the sun rises. */
	protected int[] towardSunrise() {
		BlockPos o = localPosition(0, 0, 0);
		for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
			if (localPosition(d[0], 0, d[1]).getX() > o.getX()) return d;
		}
		return new int[]{1, 0};
	}

	// ------------------------------------------------------------------ where a site may stand

	static Direction facing(Structure.GenerationContext c) {
		return Direction.Plane.HORIZONTAL.getRandomDirection(c.random());
	}

	/**
	 * Dry, fairly level ground under the whole footprint (its corners and middle no more than {@code spread} apart and
	 * none under water), at least at {@code minFloor}. The floor sits at the middle height: higher ground is cut into,
	 * lower ground is filled by the footing.
	 */
	static Optional<Structure.GenerationStub> surface(Structure.GenerationContext c, WildsPiece piece, int spread, int minFloor) {
		BoundingBox b = piece.getBoundingBox();
		int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
		for (int[] p : new int[][]{{b.minX(), b.minZ()}, {b.maxX(), b.minZ()}, {b.minX(), b.maxZ()}, {b.maxX(), b.maxZ()},
				{(b.minX() + b.maxX()) / 2, (b.minZ() + b.maxZ()) / 2}}) {
			int top = height(c, p[0], p[1], Heightmap.Types.WORLD_SURFACE_WG), ground = height(c, p[0], p[1], Heightmap.Types.OCEAN_FLOOR_WG);
			if (top != ground) return Optional.empty();
			lo = Math.min(lo, ground);
			hi = Math.max(hi, ground);
		}
		if (hi - lo > spread || lo < minFloor) return Optional.empty();
		return place(c, piece, (lo + hi) / 2);
	}

	/** On the overworld's dry land, above the sea. */
	static Optional<Structure.GenerationStub> surface(Structure.GenerationContext c, WildsPiece piece, int spread) {
		return surface(c, piece, spread, c.chunkGenerator().getSeaLevel() + 1);
	}

	/**
	 * The floor of an open cave between {@code minFloor} and {@code maxFloor}: solid footing (not lava) with at least
	 * {@code headroom} open blocks above it in the middle and at the entrance (local x {@code doorX}, z=0), and ground near that height at every corner.
	 */
	static Optional<Structure.GenerationStub> cave(Structure.GenerationContext c, WildsPiece piece, int doorX, int minFloor, int maxFloor, int headroom) {
		BoundingBox b = piece.getBoundingBox();
		BlockPos mid = new BlockPos((b.minX() + b.maxX()) / 2, 0, (b.minZ() + b.maxZ()) / 2), front = piece.localPosition(doorX, 0, 0);
		NoiseColumn centre = column(c, mid.getX(), mid.getZ()), entrance = column(c, front.getX(), front.getZ());
		NoiseColumn[] corners = {column(c, b.minX(), b.minZ()), column(c, b.maxX(), b.minZ()), column(c, b.minX(), b.maxZ()), column(c, b.maxX(), b.maxZ())};
		for (int y = maxFloor; y >= minFloor; y--) {
			if (!floorAt(centre, y, headroom) || !floorAt(entrance, y, 3)) continue;
			boolean footed = true;
			for (NoiseColumn corner : corners) {
				boolean any = false;
				for (int k = 0; k <= 3 && !any; k++) any = solid(corner.getBlock(y - k));
				footed &= any && corner.getBlock(y + 1).getFluidState().isEmpty();
			}
			if (footed) return place(c, piece, y);
		}
		return Optional.empty();
	}

	private static boolean floorAt(NoiseColumn column, int y, int headroom) {
		if (!solid(column.getBlock(y))) return false;
		for (int k = 1; k <= headroom; k++) {
			BlockState above = column.getBlock(y + k);
			if (!above.isAir()) return false;
		}
		return true;
	}

	private static boolean solid(BlockState state) {
		return !state.isAir() && state.getFluidState().isEmpty();
	}

	private static Optional<Structure.GenerationStub> place(Structure.GenerationContext c, WildsPiece piece, int floor) {
		BoundingBox b = piece.getBoundingBox();
		if (floor - SINK < c.heightAccessor().getMinY() || floor - SINK + b.getYSpan() - 1 > c.heightAccessor().getMaxY()) return Optional.empty();
		piece.move(0, floor - SINK - b.minY(), 0);
		BoundingBox moved = piece.getBoundingBox();
		BlockPos at = new BlockPos((moved.minX() + moved.maxX()) / 2, floor + 1, (moved.minZ() + moved.maxZ()) / 2);
		return Optional.of(new Structure.GenerationStub(at, builder -> builder.addPiece(piece)));
	}

	private static int height(Structure.GenerationContext c, int x, int z, Heightmap.Types type) {
		return c.chunkGenerator().getFirstOccupiedHeight(x, z, type, c.heightAccessor(), c.randomState());
	}

	private static NoiseColumn column(Structure.GenerationContext c, int x, int z) {
		return c.chunkGenerator().getBaseColumn(x, z, c.heightAccessor(), c.randomState());
	}
}
