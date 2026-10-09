package dev.wildercord.world.sites.travel;

import dev.wildercord.world.dungeons.DungeonPiece;
import dev.wildercord.world.sites.Sites;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

import java.util.List;
import java.util.Optional;

/**
 * What the travel sites share: a footing that reaches down to the ground, gable roofs, doors, beds, lecterns
 * holding a book, item frames and residents that stay near home. All peaceful: nothing here is warded.
 * Local frame as in {@link DungeonPiece}: x across, y up from the ground block, z inward (NORTH) from the entrance.
 */
abstract class TravelPiece extends DungeonPiece {
	static final BlockState DIRT = b(Blocks.DIRT), GRASS = b(Blocks.GRASS_BLOCK), COBBLE = b(Blocks.COBBLESTONE);

	TravelPiece(StructurePieceType type, int x, int y, int z, int width, int height, int depth, Direction facing) {
		super(type, x, y, z, width, height, depth, facing);
	}

	TravelPiece(StructurePieceType type, CompoundTag tag) {
		super(type, tag);
	}

	static BlockState b(Block block) { return block.defaultBlockState(); }

	@FunctionalInterface interface Maker { TravelPiece make(int x, int y, int z, Direction facing); }

	/**
	 * Dry, open, gently sloped land: the centre and four points {@code reach} away must be above sea level, out of
	 * water and within {@code slope} blocks of each other. The piece's ground block (local y 0) sits on the centre's surface.
	 */
	static Sites.Locator flat(Maker maker, int cx, int cz, int reach, int slope) {
		return c -> {
			TravelPiece piece = maker.make(c.chunkPos().getMinBlockX(), 0, c.chunkPos().getMinBlockZ(), Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
			BlockPos centre = piece.localPosition(cx, 0, cz);
			int sea = c.chunkGenerator().getSeaLevel(), surface = height(c, centre, Heightmap.Types.WORLD_SURFACE_WG);
			if (surface < sea || surface != height(c, centre, Heightmap.Types.OCEAN_FLOOR_WG)) return Optional.empty();
			for (int[] d : new int[][]{{-reach, -reach}, {reach, -reach}, {-reach, reach}, {reach, reach}}) {
				BlockPos at = centre.offset(d[0], 0, d[1]);
				int h = height(c, at, Heightmap.Types.WORLD_SURFACE_WG);
				if (h < sea || Math.abs(h - surface) > slope || h != height(c, at, Heightmap.Types.OCEAN_FLOOR_WG)) return Optional.empty();
			}
			piece.move(0, surface, 0);
			return Optional.of(new Structure.GenerationStub(centre.atY(surface), b -> b.addPiece(piece)));
		};
	}

	private static int height(Structure.GenerationContext c, BlockPos p, Heightmap.Types map) {
		return c.chunkGenerator().getFirstOccupiedHeight(p.getX(), p.getZ(), map, c.heightAccessor(), c.randomState());
	}

	@Override public List<BoundingBox> wardedBoxes() { return List.of(); }

	/** A level patch: {@code top} at y 0, {@code under} down to the real ground, and air above up to {@code clear}. */
	void footing(WorldGenLevel level, BoundingBox bb, int x0, int z0, int x1, int z1, BlockState top, BlockState under, int clear) {
		for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
			fillColumnDown(level, under, x, -1, z, bb);
			set(level, bb, top, x, 0, z);
		}
		if (clear > 0) fill(level, bb, x0, 1, z0, x1, clear, z1, AIR);
	}

	/** A pillar from {@code top} down to the ground. */
	void post(WorldGenLevel level, BoundingBox bb, BlockState state, int x, int top, int z) {
		fill(level, bb, x, 0, z, x, top, z, state);
		fillColumnDown(level, state, x, -1, z, bb);
	}

	/** A gable roof over walls x0..x1 by z0..z1 whose top course is at {@code y}, ridge running along x, one block of eaves front and back. */
	void gable(WorldGenLevel level, BoundingBox bb, int x0, int x1, int z0, int z1, int y, Block stairs, BlockState ridge, BlockState end) {
		for (int k = 0; ; k++) {
			int s = z0 - 1 + k, n = z1 + 1 - k, yy = y + k;
			if (s > n) break;
			if (s == n) { fill(level, bb, x0, yy, s, x1, yy, s, ridge); break; }
			for (int x = x0; x <= x1; x++) {
				set(level, bb, b(stairs).setValue(StairBlock.FACING, Direction.NORTH), x, yy, s);
				set(level, bb, b(stairs).setValue(StairBlock.FACING, Direction.SOUTH), x, yy, n);
			}
			if (k > 0) for (int z = s + 1; z < n; z++) { set(level, bb, end, x0, yy, z); set(level, bb, end, x1, yy, z); }
		}
	}

	void door(WorldGenLevel level, BoundingBox bb, Block door, int x, int y, int z, Direction facing) {
		BlockState state = b(door).setValue(DoorBlock.FACING, facing);
		set(level, bb, state.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), x, y, z);
		set(level, bb, state.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), x, y + 1, z);
	}

	/** A bed whose foot is at x, z and head one step {@code toward}. */
	void bed(WorldGenLevel level, BoundingBox bb, Block bed, int x, int y, int z, Direction toward) {
		BlockState state = b(bed).setValue(BlockStateProperties.HORIZONTAL_FACING, toward);
		set(level, bb, state.setValue(BlockStateProperties.BED_PART, BedPart.FOOT), x, y, z);
		set(level, bb, state.setValue(BlockStateProperties.BED_PART, BedPart.HEAD), x + toward.getStepX(), y, z - toward.getStepZ());
	}

	/** A lectern facing {@code facing} that already holds {@code book}. */
	void lectern(WorldGenLevel level, BoundingBox bb, int x, int y, int z, Direction facing, ItemStack book) {
		set(level, bb, b(Blocks.LECTERN).setValue(LecternBlock.FACING, facing).setValue(LecternBlock.HAS_BOOK, true), x, y, z);
		BlockPos at = getWorldPos(x, y, z);
		if (bb.isInside(at) && level.getBlockEntity(at) instanceof LecternBlockEntity lectern) lectern.setBook(book);
	}

	/** A local direction turned and mirrored as the piece is. */
	Direction world(Direction local) {
		return getRotation().rotate(getMirror().mirror(local));
	}

	/** An item frame in the air block x, y, z, hung on the wall behind it and facing {@code facing}. Placed once, by its chunk. */
	void frame(WorldGenLevel level, BoundingBox bb, int x, int y, int z, Direction facing, ItemStack item) {
		BlockPos at = getWorldPos(x, y, z).immutable();
		if (!bb.isInside(at)) return;
		ItemFrame frame = new ItemFrame(level.getLevel(), at, world(facing));
		frame.setItem(item, false);
		level.addFreshEntity(frame);
	}

	/** A peaceful resident that stays within {@code radius} of where it was placed and never despawns. */
	void resident(WorldGenLevel level, BoundingBox bb, EntityType<? extends Mob> type, int x, int y, int z, int radius) {
		BlockPos at = getWorldPos(x, y, z).immutable();
		if (!bb.isInside(at)) return;
		Mob mob = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (mob == null) return;
		mob.setPersistenceRequired();
		mob.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.STRUCTURE, null);
		mob.setHomeTo(at, radius);
		level.addFreshEntityWithPassengers(mob);
	}
}
