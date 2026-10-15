package dev.wildercord.world.sites.emberwood;

import dev.wildercord.world.kilnwidow.KilnWidowContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Emberwood Kiln Court: an open ceramic kiln court in the Emberwood Reach where The Kiln Widow waits.
 * Four coarse-dirt and brick firebreak causeways meet at a central kiln plinth; the north stoking shelter
 * holds the Kiln Heartstone, a lectern with The Cooled Kiln, and a supply chest.
 */
public final class KilnCourtPiece extends EmberwoodPiece {
	public static final int DECK = 2;

	public KilnCourtPiece(int x, int y, int z, Direction facing) {
		super(EmberwoodSites.COURT_PIECE, x, y, z, 35, 12, 35, facing);
	}
	public KilnCourtPiece(CompoundTag tag) { super(EmberwoodSites.COURT_PIECE, tag); }

	public BlockPos plinth() { return world(17, DECK, 17); }
	public BlockPos heartstone() { return world(17, DECK + 1, 4); }

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox clip, ChunkPos chunk, BlockPos pivot) {
		// Replaying a clipped piece must not erase the saved heartstone identity or refill an opened chest.
		for (int x = 0; x <= 34; x++) for (int y = DECK + 1; y <= 11; y++) for (int z = 0; z <= 34; z++) {
			if (!furniture(x, y, z)) set(level, clip, AIR, x, y, z);
		}
		// Circular ceramic kiln terrace and four firebreak causeways, with scorched ash sectors between them.
		for (int x = 2; x <= 32; x++) for (int z = 2; z <= 32; z++) {
			double dist = Math.hypot(x - 17, z - 17);
			if (dist > 15.5) continue;
			footing(level, clip, x, z, x, z, DECK - 1);
			boolean causeway = Math.abs(x - 17) <= 2 || Math.abs(z - 17) <= 2;
			boolean draftRing = dist >= 3.2 && dist <= 6.4;
			boolean outerRing = dist >= 12.5 && dist <= 15.0;
			boolean center = Math.abs(x - 17) <= 3 && Math.abs(z - 17) <= 3;
			int n = noise(x, DECK, z);
			BlockState floor;
			if (center) floor = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
			else if (causeway) floor = n < 30 ? COARSE_DIRT : MUD_BRICKS;
			else if (draftRing) floor = (x + z & 1) == 0 ? BRICKS : TERRACOTTA;
			else if (outerRing) floor = n < 35 ? MUD_BRICKS : STONE_BRICK;
			else floor = n < 35 ? COARSE_DIRT : n < 70 ? BROWN_TERRACOTTA : TERRACOTTA;
			set(level, clip, floor, x, DECK, z);
		}
		// Stepped approach on all four compass arms so the court never drops off sharply.
		for (int i = 15; i <= 19; i++) {
			for (int step : new int[]{1, 33}) {
				footing(level, clip, i, step, i, step, DECK - 1);
				footing(level, clip, step, i, step, i, DECK - 1);
				set(level, clip, MUD_BRICKS, i, DECK - 1, step);
				set(level, clip, MUD_BRICKS, step, DECK - 1, i);
			}
		}
		// Central Kiln Plinth (17, DECK, 17).
		for (int x = 15; x <= 19; x++) for (int z = 15; z <= 19; z++) {
			set(level, clip, (x == 15 || x == 19 || z == 15 || z == 19) ? CHISELED_BRICK : Blocks.POLISHED_DEEPSLATE.defaultBlockState(), x, DECK, z);
		}
		set(level, clip, Blocks.LODESTONE.defaultBlockState(), 17, DECK, 17);

		// Eight brick-and-copper kiln flue stacks around the perimeter.
		for (int[] c : new int[][]{{6, 6}, {28, 6}, {6, 28}, {28, 28}, {12, 3}, {22, 3}, {12, 31}, {22, 31}}) {
			for (int y = DECK + 1; y <= DECK + 3; y++) set(level, clip, y == DECK + 2 ? COPPER : BRICKS, c[0], y, c[1]);
			set(level, clip, Blocks.MUD_BRICK_WALL.defaultBlockState(), c[0], DECK + 4, c[1]);
			set(level, clip, Blocks.LANTERN.defaultBlockState(), c[0], DECK + 5, c[1]);
		}

		// North stoking shelter holding the Kiln Heartstone, instruction lectern, and supply chest.
		for (int x : new int[]{14, 20}) for (int z : new int[]{2, 6}) {
			for (int y = DECK + 1; y <= DECK + 3; y++) set(level, clip, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), x, y, z);
		}
		for (int x = 14; x <= 20; x++) for (int z = 2; z <= 6; z++) {
			set(level, clip, (x == 14 || x == 20 || z == 2 || z == 6) ? Blocks.BRICK_SLAB.defaultBlockState() : DARK_OAK, x, DECK + 4, z);
		}
		set(level, clip, CHISELED_BRICK, 17, DECK, 4);
		if (!(level.getBlockEntity(heartstone()) instanceof dev.wildercord.world.kilnwidow.KilnHeartstoneEntity)) {
			set(level, clip, KilnWidowContent.HEARTSTONE.defaultBlockState(), 17, DECK + 1, 4);
		}
		lectern(level, clip, 15, DECK + 1, 4, Direction.SOUTH,
			EmberwoodSites.book("kiln_court", "The Cooled Kiln", "Ashwin, Kiln Keeper"));
		chest(level, clip, 19, DECK + 1, 4, Direction.SOUTH, EmberwoodSites.loot(EmberwoodSites.KILN_COURT), 59L);
	}

	private static boolean furniture(int x, int y, int z) {
		return y == DECK + 1 && z == 4 && (x == 15 || x == 17 || x == 19);
	}
}
