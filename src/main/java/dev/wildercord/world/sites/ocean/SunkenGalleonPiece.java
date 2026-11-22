package dev.wildercord.world.sites.ocean;

import dev.wildercord.ocean.OceanEntities;
import dev.wildercord.ocean.OceanRules;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Sunken Sovereign Galleon (46x28x15):
 * Waterlogged 3-deck warship resting on the seabed with Lower Bilge Hold, Main Gun Deck,
 * Weather & Poop Deck (3 tattered mossy masts), Stern Captain's Cabin, and hull breach.
 */
public final class SunkenGalleonPiece extends OceanPiece {
	public static final int WIDTH = OceanRules.GALLEON_LENGTH;
	public static final int HEIGHT = OceanRules.GALLEON_HEIGHT;
	public static final int DEPTH = OceanRules.GALLEON_WIDTH;
	public static final int BASE = 3;

	public SunkenGalleonPiece(int x, int y, int z, Direction facing) {
		super(OceanSites.SUNKEN_GALLEON, x, y, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public SunkenGalleonPiece(CompoundTag tag) {
		super(OceanSites.SUNKEN_GALLEON, tag);
	}

	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext ctx) {
		ChunkPos cp = ctx.chunkPos();
		int x = cp.getMinBlockX();
		int z = cp.getMinBlockZ();
		int floor = oceanFloorY(ctx, x + WIDTH / 2, z + DEPTH / 2);
		int sea = ctx.chunkGenerator().getSeaLevel();
		if (sea - floor < 18) return Optional.empty();
		SunkenGalleonPiece piece = new SunkenGalleonPiece(x, floor - BASE, z, Direction.EAST);
		return Optional.of(new Structure.GenerationStub(new BlockPos(x + WIDTH / 2, floor, z + DEPTH / 2), b -> b.addPiece(piece)));
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunkPos, BlockPos reference) {
		int zMid = 7;

		// Build stepped 3-deck hull from bow (x=2) to stern (x=43)
		for (int x = 2; x <= 43; x++) {
			int halfW = hullHalfWidth(x);
			int deckTop = x >= 34 ? BASE + 11 : BASE + 8;

			// Keel & ballast footing
			fill(level, bb, x, BASE - 1, zMid - Math.max(1, halfW - 1), x, BASE, zMid + Math.max(1, halfW - 1), DARK_PRISMARINE);
			for (int z = zMid - halfW; z <= zMid + halfW; z++) {
				fillColumnDown(level, COBBLED_DEEPSLATE, x, BASE - 1, z, bb);
			}

			// Lower bilge floor & Gun Deck floor
			fill(level, bb, x, BASE, zMid - halfW + 1, x, BASE, zMid + halfW - 1, COBBLED_DEEPSLATE);
			fill(level, bb, x, BASE + 4, zMid - halfW + 1, x, BASE + 4, zMid + halfW - 1, SPRUCE_PLANKS);
			fill(level, bb, x, deckTop - 1, zMid - halfW + 1, x, deckTop - 1, zMid + halfW - 1, SPRUCE_PLANKS);

			// Clear water interior decks before placing walls
			fill(level, bb, x, BASE + 1, zMid - halfW + 1, x, deckTop - 2, zMid + halfW - 1, WATER);

			// Side hull planking + weathered copper band
			for (int y = BASE + 1; y <= deckTop; y++) {
				BlockState wall = (y == BASE + 4 || y == deckTop) ? COPPER_WEATHERED
					: ((x % 4 == 0) ? STRIPPED_SPRUCE : DARK_OAK_PLANKS);
				set(level, bb, wall, x, y, zMid - halfW);
				set(level, bb, wall, x, y, zMid + halfW);
			}
			// Bow & Stern bulkheads
			if (x == 2 || x == 43) {
				fill(level, bb, x, BASE, zMid - halfW, x, deckTop, zMid + halfW, DARK_OAK_PLANKS);
			}
		}

		// Bowsprit pointing west
		for (int i = 0; i < 4; i++) {
			set(level, bb, STRIPPED_SPRUCE, 1 - i / 2, BASE + 9 + i / 2, zMid);
		}
		// Bow anchor chain
		for (int y = BASE + 1; y <= BASE + 8; y++) {
			set(level, bb, Blocks.IRON_CHAIN.defaultBlockState(), 4, y, zMid);
		}

		// Port hull breach overgrown with prismarine & coral
		for (int x = 18; x <= 21; x++) {
			for (int y = BASE + 5; y <= BASE + 7; y++) {
				set(level, bb, WATER, x, y, zMid - 6);
			}
			set(level, bb, PRISMARINE_BRICKS, x, BASE + 4, zMid - 6);
		}

		// Bilge cargo barrels & treasure chest
		barrel(level, bb, 15, BASE + 1, zMid - 2, Direction.UP, OceanSites.GALLEON_LOOT, 11L);
		barrel(level, bb, 16, BASE + 1, zMid + 2, Direction.UP, OceanSites.GALLEON_LOOT, 12L);
		chest(level, bb, 28, BASE + 1, zMid, Direction.WEST, OceanSites.GALLEON_LOOT, 13L);

		// Main Gun Deck cannons + froglights
		for (int gx : new int[] {14, 20, 26, 31}) {
			set(level, bb, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.NORTH), gx, BASE + 5, zMid - 4);
			set(level, bb, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.SOUTH), gx, BASE + 5, zMid + 4);
			set(level, bb, Blocks.OCHRE_FROGLIGHT.defaultBlockState(), gx, BASE + 7, zMid);
		}

		// Cargo hatch on Weather Deck
		fill(level, bb, 22, BASE + 7, zMid - 1, 24, BASE + 7, zMid + 1, Blocks.OAK_TRAPDOOR.defaultBlockState());

		// Stern Captain's Cabin (x=34..42)
		fill(level, bb, 34, BASE + 8, zMid - 5, 34, BASE + 10, zMid + 5, DARK_OAK_PLANKS);
		set(level, bb, WATER, 34, BASE + 8, zMid);
		set(level, bb, WATER, 34, BASE + 9, zMid);
		set(level, bb, YELLOW_GLASS, 43, BASE + 9, zMid - 2);
		set(level, bb, YELLOW_GLASS, 43, BASE + 9, zMid + 2);
		set(level, bb, Blocks.CARTOGRAPHY_TABLE.defaultBlockState(), 38, BASE + 8, zMid);
		set(level, bb, Blocks.LECTERN.defaultBlockState(), 40, BASE + 8, zMid - 2);
		set(level, bb, Blocks.BOOKSHELF.defaultBlockState(), 42, BASE + 8, zMid - 3);
		set(level, bb, Blocks.BOOKSHELF.defaultBlockState(), 42, BASE + 8, zMid + 3);
		chest(level, bb, 41, BASE + 8, zMid + 2, Direction.WEST, OceanSites.GALLEON_LOOT, 14L);

		// 3 Masts & Tattered Mossy Sails
		buildMast(level, bb, 12, BASE + 8, 22, 4, zMid);
		buildMast(level, bb, 23, BASE + 8, 26, 5, zMid);
		buildMast(level, bb, 35, BASE + 11, 24, 4, zMid);

		// Galleon undead captain + patrolling shark
		spawnMob(level, bb, OceanEntities.BARNACLE_HULK_DREDGER, 27, BASE + 8, zMid, m -> {});
		spawnMob(level, bb, OceanEntities.CORAL_SPINE_MAKO, 19, BASE + 6, zMid - 5, m -> {});
	}

	private static int hullHalfWidth(int x) {
		if (x < 6) return 2 + (x - 2) / 2;
		if (x < 12) return 4 + (x - 6) / 3;
		if (x > 39) return 5;
		return 6;
	}

	private void buildMast(WorldGenLevel level, BoundingBox bb, int mx, int baseY, int topY, int halfYard, int zMid) {
		for (int y = baseY; y <= Math.min(HEIGHT - 1, topY); y++) {
			set(level, bb, STRIPPED_SPRUCE, mx, y, zMid);
		}
		int sailLow = baseY + 4;
		int sailHigh = Math.min(HEIGHT - 2, topY - 2);
		for (int z = zMid - halfYard; z <= zMid + halfYard; z++) {
			set(level, bb, Blocks.SPRUCE_FENCE.defaultBlockState(), mx, sailHigh, z);
			for (int y = sailLow; y < sailHigh; y++) {
				if ((mx + y + z) % 5 == 0) continue; // Tattered sail holes
				set(level, bb, ((y + z) % 3 == 0) ? MOSS : GREEN_WOOL, mx + 1, y, z);
			}
		}
	}
}
