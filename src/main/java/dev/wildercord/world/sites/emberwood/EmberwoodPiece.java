package dev.wildercord.world.sites.emberwood;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

/** Shared geometry and furnishing helpers for Emberwood Reach structure pieces. */
public abstract class EmberwoodPiece extends StructurePiece {
	protected static final BlockState AIR = Blocks.AIR.defaultBlockState();
	protected static final BlockState TERRACOTTA = Blocks.TERRACOTTA.defaultBlockState();
	protected static final BlockState BROWN_TERRACOTTA = Blocks.PACKED_MUD.defaultBlockState();
	protected static final BlockState MUD_BRICKS = Blocks.MUD_BRICKS.defaultBlockState();
	protected static final BlockState BRICKS = Blocks.BRICKS.defaultBlockState();
	protected static final BlockState STONE_BRICK = Blocks.STONE_BRICKS.defaultBlockState();
	protected static final BlockState CRACKED_BRICK = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
	protected static final BlockState CHISELED_BRICK = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
	protected static final BlockState COARSE_DIRT = Blocks.COARSE_DIRT.defaultBlockState();
	protected static final BlockState PODZOL = Blocks.PODZOL.defaultBlockState();
	protected static final BlockState DARK_OAK = Blocks.DARK_OAK_PLANKS.defaultBlockState();
	protected static final BlockState COPPER = Blocks.CUT_COPPER.waxed().exposed().defaultBlockState();

	protected EmberwoodPiece(StructurePieceType type, int x, int y, int z, int width, int height, int depth, Direction facing) {
		super(type, 0, StructurePiece.makeBoundingBox(x, y, z, facing, width, height, depth));
		setOrientation(facing);
	}

	protected EmberwoodPiece(StructurePieceType type, CompoundTag tag) {
		super(type, tag);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {}

	@Override
	protected void placeBlock(WorldGenLevel level, BlockState state, int x, int y, int z, BoundingBox clip) {
		BlockPos at = world(x, y, z);
		if (!clip.isInside(at)) return;
		if (!state.hasBlockEntity() && level.getBlockState(at).hasBlockEntity()) {
			level.getBlockEntity(at);
			level.getChunk(at).removeBlockEntity(at);
		}
		super.placeBlock(level, state, x, y, z, clip);
		if (state.hasBlockEntity()) level.getBlockEntity(at);
	}

	protected final void set(WorldGenLevel level, BoundingBox clip, BlockState state, int x, int y, int z) {
		placeBlock(level, state, x, y, z, clip);
	}

	protected final void fill(WorldGenLevel level, BoundingBox clip, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
		generateBox(level, clip, x0, y0, z0, x1, y1, z1, state, state, false);
	}

	protected final BlockPos world(int x, int y, int z) {
		return getWorldPos(x, y, z);
	}

	public final BlockPos localPosition(int x, int y, int z) {
		return getWorldPos(x, y, z);
	}

	protected final void footing(WorldGenLevel level, BoundingBox clip, int x0, int z0, int x1, int z1, int topY) {
		for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
			int n = noise(x, topY, z);
			BlockState cap = n < 30 ? MUD_BRICKS : n < 65 ? TERRACOTTA : STONE_BRICK;
			set(level, clip, cap, x, topY, z);
			fillColumnDown(level, n < 45 ? TERRACOTTA : Blocks.STONE.defaultBlockState(), x, topY - 1, z, clip);
		}
	}

	protected final void door(WorldGenLevel level, BoundingBox clip, Block doorBlock, int x, int y, int z, Direction facing) {
		BlockState lower = doorBlock.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
		BlockState upper = doorBlock.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
		set(level, clip, lower, x, y, z);
		set(level, clip, upper, x, y + 1, z);
	}

	protected final void bed(WorldGenLevel level, BoundingBox clip, Block bedBlock, int fx, int y, int fz, Direction headDir) {
		int hx = fx + headDir.getStepX(), hz = fz + headDir.getStepZ();
		set(level, clip, bedBlock.defaultBlockState().setValue(BedBlock.FACING, headDir).setValue(BedBlock.PART, BedPart.FOOT), fx, y, fz);
		set(level, clip, bedBlock.defaultBlockState().setValue(BedBlock.FACING, headDir).setValue(BedBlock.PART, BedPart.HEAD), hx, y, hz);
	}

	protected final void chest(WorldGenLevel level, BoundingBox clip, int x, int y, int z, Direction facing, ResourceKey<LootTable> loot, long salt) {
		BlockPos p = world(x, y, z);
		if (!clip.isInside(p) || level.getBlockEntity(p) instanceof ChestBlockEntity) return;
		set(level, clip, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), x, y, z);
		if (level.getBlockEntity(p) instanceof ChestBlockEntity c) {
			c.setLootTable(loot, p.asLong() ^ salt);
		}
	}

	protected final void lectern(WorldGenLevel level, BoundingBox clip, int x, int y, int z, Direction facing, ItemStack book) {
		BlockPos p = world(x, y, z);
		if (!clip.isInside(p) || level.getBlockEntity(p) instanceof LecternBlockEntity) return;
		set(level, clip, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, facing).setValue(LecternBlock.HAS_BOOK, true), x, y, z);
		if (level.getBlockEntity(p) instanceof LecternBlockEntity l) {
			l.setBook(book);
		}
	}

	protected static int noise(int x, int y, int z) {
		int h = x * 73428767 ^ y * 912931 ^ z * 42317861;
		h ^= (h >>> 13);
		return (h * 1274126177 >>> 24) & 127;
	}
}
