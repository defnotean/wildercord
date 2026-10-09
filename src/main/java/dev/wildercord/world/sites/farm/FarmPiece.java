package dev.wildercord.world.sites.farm;

import dev.wildercord.world.dungeons.DungeonPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

import java.util.List;
import java.util.Optional;

/**
 * What the farmstead sites share: one piece each, its ground at local y {@link #ground()}, set on a dry spot no
 * steeper than it allows, cleared above and propped up below the way vanilla's scattered features are. Peaceful,
 * so nothing is warded. Every choice is a pure function of the spot.
 */
public abstract class FarmPiece extends DungeonPiece {
	protected FarmPiece(StructurePieceType type, int x, int y, int z, int width, int height, int depth, Direction facing) {
		super(type, x, y, z, width, height, depth, facing);
	}

	protected FarmPiece(StructurePieceType type, CompoundTag tag) {
		super(type, tag);
	}

	/** The local y the surrounding ground sits at. */
	protected int ground() {
		return 0;
	}

	@Override
	public List<BoundingBox> wardedBoxes() {
		return List.of();
	}

	@FunctionalInterface
	interface Factory {
		FarmPiece make(int x, int z, Direction facing);
	}

	/** Corners and centre must be dry land within {@code slope} blocks of each other; the piece sits on the centre's height. */
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c, Factory factory, int slope) {
		var piece = factory.make(c.chunkPos().getMinBlockX(), c.chunkPos().getMinBlockZ(), Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
		var b = piece.getBoundingBox();
		int[][] spots = {{b.minX(), b.minZ()}, {b.maxX(), b.minZ()}, {b.minX(), b.maxZ()}, {b.maxX(), b.maxZ()}, {(b.minX() + b.maxX()) / 2, (b.minZ() + b.maxZ()) / 2}};
		int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE, centre = 0;
		for (int[] s : spots) {
			int top = height(c, s[0], s[1], Heightmap.Types.WORLD_SURFACE_WG);
			if (top != height(c, s[0], s[1], Heightmap.Types.OCEAN_FLOOR_WG)) return Optional.empty();
			lo = Math.min(lo, top); hi = Math.max(hi, top); centre = top;
		}
		if (hi - lo > slope || centre <= c.heightAccessor().getMinY() + 8 || centre + b.getYSpan() >= c.heightAccessor().getMaxY()) return Optional.empty();
		piece.move(0, centre - piece.ground(), 0);
		var mid = new BlockPos((b.minX() + b.maxX()) / 2, centre, (b.minZ() + b.maxZ()) / 2);
		return Optional.of(new Structure.GenerationStub(mid, builder -> builder.addPiece(piece)));
	}

	private static int height(Structure.GenerationContext c, int x, int z, Heightmap.Types type) {
		return c.chunkGenerator().getFirstOccupiedHeight(x, z, type, c.heightAccessor(), c.randomState());
	}

	// ------------------------------------------------------------------ shorthand

	protected static BlockState s(Block block) {
		return block.defaultBlockState();
	}

	protected static BlockState facing(Block block, Direction d) {
		return block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, d);
	}

	protected static BlockState stairs(Block block, Direction d, boolean top) {
		return facing(block, d).setValue(BlockStateProperties.HALF, top ? Half.TOP : Half.BOTTOM);
	}

	protected static BlockState leaves(Block block) {
		return block.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true);
	}

	protected static BlockState crop(Block block, int age) {
		return block.defaultBlockState().setValue(BlockStateProperties.AGE_7, age);
	}

	protected static BlockState farmland(boolean wet) {
		return Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, wet ? 7 : 0);
	}

	protected static BlockState floorAttached(Block block, Direction d) {
		return facing(block, d).setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR);
	}

	// ------------------------------------------------------------------ groundwork

	/** Air over the whole footprint from {@code y0} up, and a solid {@code floor} under it with its gaps filled down to the ground. */
	protected void site(WorldGenLevel level, BoundingBox bb, BlockState floor, int y0) {
		fill(level, bb, 0, y0, 0, width - 1, height - 1, depth - 1, AIR);
		for (int x = 0; x < width; x++) for (int z = 0; z < depth; z++) {
			set(level, bb, floor, x, ground(), z);
			prop(level, bb, x, ground() - 1, z, Blocks.DIRT.defaultBlockState());
		}
	}

	/** Fills a column down from a spot until it meets ground, as vanilla props up its desert temples (at most 12 blocks). */
	protected void prop(WorldGenLevel level, BoundingBox bb, int x, int y, int z, BlockState state) {
		BlockPos.MutableBlockPos at = getWorldPos(x, y, z);
		if (!bb.isInside(at)) return;
		for (int n = 0; n < 12 && at.getY() > level.getMinY() + 1; n++, at.move(Direction.DOWN)) {
			var here = level.getBlockState(at);
			if (!here.isAir() && !here.liquid() && !here.canBeReplaced()) return;
			level.setBlock(at, state, 2);
		}
	}

	/** Four walls with no floor or ceiling. */
	protected void walls(WorldGenLevel level, BoundingBox bb, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
		fill(level, bb, x0, y0, z0, x1, y1, z0, state);
		fill(level, bb, x0, y0, z1, x1, y1, z1, state);
		fill(level, bb, x0, y0, z0, x0, y1, z1, state);
		fill(level, bb, x1, y0, z0, x1, y1, z1, state);
	}

	/** A ladder from {@code y0} to {@code y1}, its back on the wall behind {@code d}. */
	protected void ladder(WorldGenLevel level, BoundingBox bb, int x, int y0, int y1, int z, Direction d) {
		fill(level, bb, x, y0, z, x, y1, z, facing(Blocks.LADDER, d));
	}

	/** A standing sign turned to face the entrance (local south). */
	protected static BlockState standingSign() {
		return Blocks.OAK_SIGN.defaultBlockState().setValue(BlockStateProperties.ROTATION_16, 0);
	}

	protected static BlockState hangingLantern() {
		return Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true);
	}

	/** A door, both halves, opened or shut, facing {@code d} in the piece's frame. */
	protected void door(WorldGenLevel level, BoundingBox bb, Block block, int x, int y, int z, Direction d, boolean open) {
		var lower = facing(block, d).setValue(BlockStateProperties.OPEN, open).setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
		set(level, bb, lower, x, y, z);
		set(level, bb, lower.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), x, y + 1, z);
	}

	/** A waxed sign whose lines are language keys {@code sign.wildercord.<key>.1..n}. */
	protected void sign(WorldGenLevel level, BoundingBox bb, BlockState state, int x, int y, int z, String key, int lines) {
		BlockPos at = getWorldPos(x, y, z);
		if (!bb.isInside(at)) return;
		set(level, bb, state, x, y, z);
		if (level.getBlockEntity(at) instanceof SignBlockEntity sign) {
			var text = SignText.EMPTY.asMutable();
			for (int i = 0; i < lines; i++) text.setLine(i, Component.translatable("sign.wildercord." + key + "." + (i + 1)));
			sign.setText(text.asImmutable(), SignTextSlot.FRONT);
			sign.setWaxed(true);
		}
	}

	/** A beehive or nest, full of honey, with {@code bees} bees resting inside (they wake and fly by day). */
	protected void hive(WorldGenLevel level, BoundingBox bb, Block block, int x, int y, int z, Direction d, int bees) {
		BlockPos at = getWorldPos(x, y, z);
		if (!bb.isInside(at)) return;
		set(level, bb, facing(block, d).setValue(BlockStateProperties.LEVEL_HONEY, 5), x, y, z);
		if (level.getBlockEntity(at) instanceof BeehiveBlockEntity hive) {
			for (int i = 0; i < bees; i++) hive.storeBee(BeehiveBlockEntity.Occupant.create(200 + noise(x + i, y, z) * 4));
		}
	}

	/** A dispenser holding one water bucket: a well a button can wake. */
	protected void well(WorldGenLevel level, BoundingBox bb, int x, int y, int z, Direction d) {
		BlockPos at = getWorldPos(x, y, z);
		if (!bb.isInside(at)) return;
		set(level, bb, s(Blocks.DISPENSER).setValue(BlockStateProperties.FACING, d), x, y, z);
		if (level.getBlockEntity(at) instanceof DispenserBlockEntity dispenser) dispenser.setItem(4, new ItemStack(Items.WATER_BUCKET));
	}

	/** A leafy crown over a trunk: logs {@code trunk} high, persistent leaves around the top. */
	protected void tree(WorldGenLevel level, BoundingBox bb, int x, int y, int z, int trunk, BlockState log, BlockState leaf) {
		for (int dy = trunk - 2; dy <= trunk + 1; dy++) {
			int r = dy >= trunk ? 1 : 2;
			for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
				if (Math.abs(dx) == r && Math.abs(dz) == r && (r == 1 || noise(x + dx, y + dy, z + dz) < 60)) continue;
				set(level, bb, leaf, x + dx, y + dy, z + dz);
			}
		}
		fill(level, bb, x, y, z, x, y + trunk - 1, z, log);
	}

	/** A scarecrow: a post, a straw body with stick arms and a carved head looking toward {@code d}. */
	protected void scarecrow(WorldGenLevel level, BoundingBox bb, int x, int y, int z, Direction d) {
		set(level, bb, s(Blocks.SPRUCE_FENCE), x, y, z);
		set(level, bb, s(Blocks.HAY_BLOCK), x, y + 1, z);
		set(level, bb, facing(Blocks.CARVED_PUMPKIN, d), x, y + 2, z);
		boolean across = d.getAxis() == Direction.Axis.Z;
		set(level, bb, s(Blocks.SPRUCE_FENCE), across ? x - 1 : x, y + 1, across ? z : z - 1);
		set(level, bb, s(Blocks.SPRUCE_FENCE), across ? x + 1 : x, y + 1, across ? z : z + 1);
	}
}
