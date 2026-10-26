package dev.wildercord.world.sites.water;

import dev.wildercord.wildlife.MoonreedBlock;
import dev.wildercord.wildlife.WetlandGarden;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
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
		// The deck's edge is trimmed with corbels, and its corners carry short lantern posts.
		for (int v = 3; v <= 15; v++) {
			wet(level, clip, stair(Blocks.SPRUCE_STAIRS, Direction.EAST, true), 2, DECK - 1, v);
			wet(level, clip, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, true), 16, DECK - 1, v);
			wet(level, clip, stair(Blocks.SPRUCE_STAIRS, Direction.SOUTH, true), v, DECK - 1, 16);
			if (v < 7 || v > 9) wet(level, clip, stair(Blocks.SPRUCE_STAIRS, Direction.NORTH, true), v, DECK - 1, 2);
		}
		for (int x : new int[] {2, 16}) for (int z : new int[] {2, 16}) {
			fill(level, clip, x, DECK + 1, z, x, DECK + 2, z, log);
			set(level, clip, Blocks.LANTERN.defaultBlockState(), x, DECK + 3, z);
		}
		for (int x : new int[] {3, 15}) {
			fill(level, clip, x, DECK + 1, 3, x, DECK + 3, 3, log);
			set(level, clip, Blocks.LANTERN.defaultBlockState(), x, DECK + 4, 3);
		}
		// The survey house: a packed-mud footing course, mud-brick walls in a stripped-log frame with a top plate,
		// and a continuous two-block-high entrance from the landing under a log lintel.
		Palette mud = Palette.of(Blocks.MUD_BRICKS, 8, Blocks.PACKED_MUD, 2);
		walls(level, clip, 3, DECK + 2, 8, 11, DECK + 3, 15, mud);
		walls(level, clip, 3, DECK + 1, 8, 11, DECK + 1, 15, Palette.of(Blocks.PACKED_MUD, 3, Blocks.MUD, 1));
		var beamX = log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X); var beamZ = log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
		fill(level, clip, 4, DECK + 4, 8, 10, DECK + 4, 8, beamX); fill(level, clip, 4, DECK + 4, 15, 10, DECK + 4, 15, beamX);
		fill(level, clip, 3, DECK + 4, 9, 3, DECK + 4, 14, beamZ); fill(level, clip, 11, DECK + 4, 9, 11, DECK + 4, 14, beamZ);
		fill(level, clip, 4, DECK + 1, 9, 10, DECK + 4, 14, AIR);
		for (int x : new int[] {3, 11}) for (int z : new int[] {8, 15}) fill(level, clip, x, DECK + 1, z, x, DECK + 4, z, log);
		for (int x : new int[] {3, 11}) fill(level, clip, x, DECK + 1, 12, x, DECK + 3, 12, log);
		fill(level, clip, 7, DECK + 1, 8, 8, DECK + 2, 8, AIR);
		fill(level, clip, 6, DECK + 3, 8, 9, DECK + 3, 8, beamX);
		// Tall windows: two panes high, with a sill and window box on the garden side.
		for (int z : new int[] {10, 13}) for (int x : new int[] {3, 11}) fill(level, clip, x, DECK + 2, z, x, DECK + 3, z, Blocks.GLASS_PANE.defaultBlockState());
		for (int z : new int[] {10, 13}) {
			set(level, clip, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, true), 12, DECK + 1, z);
			set(level, clip, Blocks.POTTED_ALLIUM.defaultBlockState(), 12, DECK + 2, z == 10 ? 9 : 14);
		}
		fill(level, clip, 12, DECK + 1, 9, 12, DECK + 1, 9, Blocks.SPRUCE_SLAB.defaultBlockState());
		fill(level, clip, 12, DECK + 1, 14, 12, DECK + 1, 14, Blocks.SPRUCE_SLAB.defaultBlockState());
		// A steep prismarine gable over corbelled eaves; a tie beam carries the lantern inside.
		gable(level, clip, 2, 12, 7, 16, DECK + 5, Blocks.DARK_PRISMARINE_STAIRS, Blocks.DARK_PRISMARINE.defaultBlockState(), planks, 8, 15);
		for (int z = 7; z <= 16; z++) {
			set(level, clip, stair(Blocks.DARK_PRISMARINE_STAIRS, Direction.EAST, true), 2, DECK + 4, z);
			set(level, clip, stair(Blocks.DARK_PRISMARINE_STAIRS, Direction.WEST, true), 12, DECK + 4, z);
		}
		for (int x = 4; x <= 10; x++) for (int z : new int[] {8, 15}) if (x != 7) set(level, clip, beamX, x, DECK + 5, z);
		fill(level, clip, 4, DECK + 5, 11, 10, DECK + 5, 11, beamX);
		set(level, clip, Blocks.GLASS.defaultBlockState(), 7, DECK + 6, 8);
		set(level, clip, Blocks.GLASS.defaultBlockState(), 7, DECK + 6, 15);
		// A verandah on log posts shelters the workbench and the door, leaving the central path clear.
		fill(level, clip, 4, DECK + 1, 5, 5, DECK + 1, 5, Blocks.SPRUCE_SLAB.defaultBlockState());
		set(level, clip, Blocks.CRAFTING_TABLE.defaultBlockState(), 4, DECK + 1, 6);
		for (int x : new int[] {3, 6, 10}) fill(level, clip, x, DECK + 1, 6, x, DECK + 3, 6, x == 6 ? Blocks.SPRUCE_FENCE.defaultBlockState() : log);
		fill(level, clip, 3, DECK + 4, 5, 10, DECK + 4, 7, Blocks.SPRUCE_SLAB.defaultBlockState());
		fill(level, clip, 3, DECK + 4, 6, 10, DECK + 4, 6, beamX);
		set(level, clip, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 5, DECK + 3, 6);
		set(level, clip, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 8, DECK + 3, 6);
		for (int x : new int[] {5, 10}) banner(level, clip, Blocks.WALL_BANNER.pick(DyeColor.BLUE), x, DECK + 3, 7, Direction.SOUTH);
		set(level, clip, Blocks.FLOWER_POT.defaultBlockState(), 5, DECK + 2, 5);
		for (int z : new int[] {4, 5, 6}) set(level, clip, stair(Blocks.SPRUCE_STAIRS, Direction.EAST), 14, DECK + 1, z);
		set(level, clip, Blocks.SPRUCE_SLAB.defaultBlockState(), 13, DECK + 1, 5);
		set(level, clip, Blocks.CANDLE.defaultBlockState(), 13, DECK + 2, 5);
		set(level, clip, Blocks.BARREL.defaultBlockState(), 15, DECK + 1, 8);
		set(level, clip, Blocks.BARREL.defaultBlockState(), 15, DECK + 2, 8);
		// The survey room: maps, shelves, a rug and a lantern on the tie beam.
		set(level, clip, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 7, DECK + 4, 11);
		set(level, clip, Blocks.CARTOGRAPHY_TABLE.defaultBlockState(), 4, DECK + 1, 10);
		set(level, clip, Blocks.BARREL.defaultBlockState(), 4, DECK + 1, 13);
		set(level, clip, Blocks.BOOKSHELF.defaultBlockState(), 4, DECK + 1, 14);
		fill(level, clip, 4, DECK + 2, 13, 4, DECK + 2, 14, Blocks.BOOKSHELF.defaultBlockState());
		set(level, clip, Blocks.FLOWER_POT.defaultBlockState(), 4, DECK + 2, 10);
		set(level, clip, Blocks.LOOM.defaultBlockState(), 4, DECK + 1, 11);
		fill(level, clip, 6, DECK + 1, 10, 8, DECK + 1, 12, Blocks.CARPET.blue().defaultBlockState());
		set(level, clip, Blocks.CANDLE.defaultBlockState(), 10, DECK + 1, 14);
		lectern(level, clip, 5, DECK + 1, 14, Direction.SOUTH, "The First Crossing", "book.wildercord.reedkeeper", 4, "Tideward surveyors");
		chest(level, clip, 10, DECK + 1, 10, WaterSites.REEDKEEPER_LOOT, Direction.WEST, 309);
		var bed = Blocks.BED.cyan().defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH);
		set(level, clip, bed.setValue(BedBlock.PART, BedPart.FOOT), 9, DECK + 1, 13);
		set(level, clip, bed.setValue(BedBlock.PART, BedPart.HEAD), 9, DECK + 1, 14);
		// A deliberately cultivated open-sky garden, distinct from natural biome decoration, with a log kerb and a composter.
		fill(level, clip, 13, DECK, 10, 15, DECK, 14, Blocks.MUD.defaultBlockState());
		for (int z = 10; z <= 14; z++) set(level, clip, Blocks.WATER.defaultBlockState(), 14, DECK, z);
		for (int x : new int[] {13, 15}) for (int z : new int[] {11, 13}) {
			set(level, clip, WetlandGarden.REED.defaultBlockState().setValue(MoonreedBlock.AGE, 1), x, DECK + 1, z);
		}
		fill(level, clip, 13, DECK, 9, 15, DECK, 9, beamX); fill(level, clip, 13, DECK, 15, 15, DECK, 15, beamX);
		set(level, clip, Blocks.COMPOSTER.defaultBlockState(), 13, DECK + 1, 15);
		set(level, clip, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3), 15, DECK + 1, 15);
		var home = localPosition(7, DECK + 1, 11);
		if (!residentPlaced && clip.isInside(home)) residentPlaced = ReedkeeperResident.spawn(level, home);
	}
}
