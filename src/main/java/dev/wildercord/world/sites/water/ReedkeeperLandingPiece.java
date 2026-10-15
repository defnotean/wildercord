package dev.wildercord.world.sites.water;

import dev.wildercord.wildlife.MoonreedBlock;
import dev.wildercord.wildlife.WetlandGarden;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.Optional;

/** A Tideward survey house and open bank garden, raised above shallow Reedlands water. */
public final class ReedkeeperLandingPiece extends WaterPiece {
	public static final int WIDTH = 19, DEPTH = 18, HEIGHT = 18, BASE = 6, DECK = BASE + 1;
	private boolean residentPlaced;

	public ReedkeeperLandingPiece(int x, int y, int z, Direction facing) {
		super(WaterSites.REEDKEEPER_LANDING, x, y, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public ReedkeeperLandingPiece(CompoundTag tag) {
		super(WaterSites.REEDKEEPER_LANDING, tag);
		residentPlaced = tag.getBooleanOr("ReedkeeperPlaced", false);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		super.addAdditionalSaveData(context, tag);
		tag.putBoolean("ReedkeeperPlaced", residentPlaced);
	}

	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext context) {
		var chunk = context.chunkPos();
		var piece = new ReedkeeperLandingPiece(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(), Direction.Plane.HORIZONTAL.getRandomDirection(context.random()));
		int sea = context.chunkGenerator().getSeaLevel();
		boolean pool = false;
		for (int[] corner : new int[][] {{2, 2}, {16, 2}, {2, 15}, {16, 15}, {9, 8}}) {
			var at = piece.getWorldPos(corner[0], 0, corner[1]);
			int top = surface(context, at);
			if (top < sea || top > sea + 2 || floor(context, at) < sea - BASE) return Optional.empty();
			pool |= water(context, at, 1, BASE);
		}
		if (!pool) return Optional.empty();
		piece.move(0, sea - 1 - BASE, 0);
		return stub(piece, piece.localPosition(9, DECK, 8));
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
		BoundingBox clip, ChunkPos chunk, BlockPos reference) {
		var planks = Blocks.SPRUCE_PLANKS.defaultBlockState();
		var log = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
		clear(level, clip, 2, DECK + 1, 1, 16, HEIGHT - 1, 16);
		fill(level, clip, 2, DECK, 2, 16, DECK, 16, planks);
		for (int x : new int[] {2, 9, 16}) for (int z : new int[] {2, 8, 16}) post(level, clip, log, x, z, DECK - 1);
		for (int x = 7; x <= 9; x++) wet(level, clip, stair(Blocks.SPRUCE_STAIRS, Direction.NORTH), x, BASE, 1);
		for (int z = 2; z <= 16; z++) {
			set(level, clip, Blocks.SPRUCE_FENCE.defaultBlockState(), 2, DECK + 1, z);
			set(level, clip, Blocks.SPRUCE_FENCE.defaultBlockState(), 16, DECK + 1, z);
		}
		for (int x = 2; x <= 16; x++) {
			set(level, clip, Blocks.SPRUCE_FENCE.defaultBlockState(), x, DECK + 1, 16);
			if (x < 7 || x > 9) set(level, clip, Blocks.SPRUCE_FENCE.defaultBlockState(), x, DECK + 1, 2);
		}
		for (int x : new int[] {3, 15}) {
			fill(level, clip, x, DECK + 1, 3, x, DECK + 3, 3, log);
			set(level, clip, Blocks.LANTERN.defaultBlockState(), x, DECK + 4, 3);
		}
		// The survey house, with a continuous two-block-high entrance from the landing.
		fill(level, clip, 3, DECK + 1, 8, 11, DECK + 4, 15, Blocks.MUD_BRICKS.defaultBlockState());
		fill(level, clip, 4, DECK + 1, 9, 10, DECK + 4, 14, AIR);
		for (int x : new int[] {3, 11}) for (int z : new int[] {8, 15}) fill(level, clip, x, DECK + 1, z, x, DECK + 4, z, log);
		fill(level, clip, 7, DECK + 1, 8, 8, DECK + 2, 8, AIR);
		for (int z : new int[] {10, 13}) for (int x : new int[] {3, 11}) set(level, clip, Blocks.GLASS_PANE.defaultBlockState(), x, DECK + 2, z);
		gable(level, clip, 2, 12, 7, 16, DECK + 5, Blocks.DARK_PRISMARINE_STAIRS, Blocks.DARK_PRISMARINE.defaultBlockState(), planks, 8, 15);
		set(level, clip, Blocks.GLASS.defaultBlockState(), 7, DECK + 6, 8);
		// A covered workbench and seats face the open landing, leaving the central path clear.
		fill(level, clip, 4, DECK + 1, 5, 5, DECK + 1, 5, Blocks.SPRUCE_SLAB.defaultBlockState());
		set(level, clip, Blocks.CRAFTING_TABLE.defaultBlockState(), 4, DECK + 1, 6);
		for (int x : new int[] {3, 6}) fill(level, clip, x, DECK + 1, 6, x, DECK + 3, 6, Blocks.SPRUCE_FENCE.defaultBlockState());
		fill(level, clip, 3, DECK + 4, 5, 6, DECK + 4, 7, Blocks.SPRUCE_SLAB.defaultBlockState());
		set(level, clip, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 5, DECK + 3, 6);
		for (int z : new int[] {4, 5, 6}) set(level, clip, stair(Blocks.SPRUCE_STAIRS, Direction.EAST), 14, DECK + 1, z);
		set(level, clip, Blocks.BARREL.defaultBlockState(), 15, DECK + 1, 8);
		set(level, clip, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 7, DECK + 4, 11);
		set(level, clip, Blocks.CARTOGRAPHY_TABLE.defaultBlockState(), 4, DECK + 1, 10);
		set(level, clip, Blocks.BARREL.defaultBlockState(), 4, DECK + 1, 13);
		set(level, clip, Blocks.BOOKSHELF.defaultBlockState(), 4, DECK + 1, 14);
		lectern(level, clip, 5, DECK + 1, 14, Direction.SOUTH, "The First Crossing", "book.wildercord.reedkeeper", 4, "Tideward surveyors");
		chest(level, clip, 10, DECK + 1, 10, WaterSites.REEDKEEPER_LOOT, Direction.WEST, 309);
		var bed = Blocks.BED.cyan().defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH);
		set(level, clip, bed.setValue(BedBlock.PART, BedPart.FOOT), 9, DECK + 1, 13);
		set(level, clip, bed.setValue(BedBlock.PART, BedPart.HEAD), 9, DECK + 1, 14);
		// A deliberately cultivated open-sky garden, distinct from natural biome decoration.
		fill(level, clip, 13, DECK, 10, 15, DECK, 14, Blocks.MUD.defaultBlockState());
		for (int z = 10; z <= 14; z++) set(level, clip, Blocks.WATER.defaultBlockState(), 14, DECK, z);
		for (int x : new int[] {13, 15}) for (int z : new int[] {11, 13}) {
			set(level, clip, WetlandGarden.REED.defaultBlockState().setValue(MoonreedBlock.AGE, 1), x, DECK + 1, z);
		}
		var home = localPosition(7, DECK + 1, 11);
		if (!residentPlaced && clip.isInside(home)) residentPlaced = ReedkeeperResident.spawn(level, home);
	}
}
