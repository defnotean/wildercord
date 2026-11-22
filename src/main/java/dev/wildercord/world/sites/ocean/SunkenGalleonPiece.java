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

			// Side hull planking, copper ribs, prismarine keel barnacles & gunwale railing
			for (int y = BASE + 1; y <= deckTop; y++) {
				BlockState wall = (y == BASE + 4 || y == deckTop) ? COPPER_WEATHERED
					: ((x % 4 == 0) ? STRIPPED_SPRUCE : DARK_OAK_PLANKS);
				set(level, bb, wall, x, y, zMid - halfW);
				set(level, bb, wall, x, y, zMid + halfW);
			}
			// Carved wooden bulwark rail & gunwale cap along Weather Deck & Quarterdeck
			if (x >= 5 && x <= 42 && x % 2 == 0) {
				set(level, bb, Blocks.SPRUCE_FENCE.defaultBlockState().trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), x, deckTop + 1, zMid - halfW);
				set(level, bb, Blocks.SPRUCE_FENCE.defaultBlockState().trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), x, deckTop + 1, zMid + halfW);
			} else if (x >= 5 && x <= 42) {
				set(level, bb, slab(Blocks.DARK_OAK_SLAB, false).trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), x, deckTop + 1, zMid - halfW);
				set(level, bb, slab(Blocks.DARK_OAK_SLAB, false).trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), x, deckTop + 1, zMid + halfW);
			}
			// Bow & Stern bulkheads
			if (x == 2 || x == 43) {
				fill(level, bb, x, BASE, zMid - halfW, x, deckTop, zMid + halfW, DARK_OAK_PLANKS);
			}
		}

		// Ornate Golden Figurehead Prow & Stepped Bowsprit pointing west
		set(level, bb, GOLD, 1, BASE + 8, zMid);
		set(level, bb, GOLD, 0, BASE + 9, zMid);
		set(level, bb, SEA_LANTERN, 0, BASE + 10, zMid);
		for (int i = 0; i < 6; i++) {
			set(level, bb, STRIPPED_SPRUCE, 2 - i / 2, BASE + 9 + (i + 1) / 2, zMid);
			if (i >= 2 && i <= 4) {
				set(level, bb, Blocks.WOOL.white().defaultBlockState(), 2 - i / 2, BASE + 10 + (i + 1) / 2, zMid);
			}
		}
		// Heavy sunken bow anchor on seabed
		for (int y = BASE; y <= BASE + 8; y++) {
			set(level, bb, Blocks.IRON_CHAIN.defaultBlockState().trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), 4, y, zMid);
		}
		fill(level, bb, 3, BASE, zMid - 1, 5, BASE, zMid + 1, DARK_PRISMARINE);

		// Port hull breach overgrown with prismarine, brain coral & glowing sea pickles
		for (int x = 18; x <= 21; x++) {
			for (int y = BASE + 5; y <= BASE + 7; y++) {
				set(level, bb, WATER, x, y, zMid - 6);
			}
			set(level, bb, PRISMARINE_BRICKS, x, BASE + 4, zMid - 6);
		}
		set(level, bb, Blocks.BRAIN_CORAL_BLOCK.defaultBlockState(), 19, BASE + 4, zMid - 6);
		set(level, bb, Blocks.TUBE_CORAL_BLOCK.defaultBlockState(), 20, BASE + 4, zMid - 6);
		set(level, bb, Blocks.FIRE_CORAL_BLOCK.defaultBlockState(), 17, BASE + 3, zMid - 6);

		// Twin Fore/After Companionway Stairs & Quarterdeck Transition
		for (int dy = 0; dy < 3; dy++) {
			set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.EAST, false).trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), 31 + dy, BASE + 8 + dy, zMid - 2);
			set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.EAST, false).trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), 31 + dy, BASE + 8 + dy, zMid + 2);
		}

		// Bilge cargo barrels & treasure chest
		barrel(level, bb, 15, BASE + 1, zMid - 2, Direction.UP, OceanSites.GALLEON_LOOT, 11L);
		barrel(level, bb, 16, BASE + 1, zMid + 2, Direction.UP, OceanSites.GALLEON_LOOT, 12L);
		chest(level, bb, 28, BASE + 1, zMid, Direction.WEST, OceanSites.GALLEON_LOOT, 13L);

		// Main Gun Deck cannons with dark-oak carriages + froglights + port/starboard gunport trim
		for (int gx : new int[] {14, 20, 26, 31}) {
			set(level, bb, stair(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, false).trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), gx, BASE + 5, zMid - 3);
			set(level, bb, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.NORTH), gx, BASE + 5, zMid - 4);
			set(level, bb, stair(Blocks.DARK_OAK_STAIRS, Direction.NORTH, false).trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), gx, BASE + 5, zMid + 3);
			set(level, bb, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.SOUTH), gx, BASE + 5, zMid + 4);
			set(level, bb, Blocks.OCHRE_FROGLIGHT.defaultBlockState(), gx, BASE + 7, zMid);
		}

		// Cargo hatch + helm wheel on Weather Deck & Poop Deck
		fill(level, bb, 21, BASE + 7, zMid - 1, 24, BASE + 7, zMid + 1, Blocks.OAK_TRAPDOOR.defaultBlockState());
		fill(level, bb, 21, BASE + 8, zMid - 1, 24, BASE + 8, zMid + 1, WATER);
		set(level, bb, Blocks.SPRUCE_FENCE.defaultBlockState().trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), 36, BASE + 11, zMid);
		set(level, bb, GOLD, 36, BASE + 12, zMid);

		// Ornate Gilded Stern Captain's Cabin (x=34..43) + Gallery Windows & Hanging Lanterns
		fill(level, bb, 34, BASE + 8, zMid - 5, 34, BASE + 10, zMid + 5, DARK_OAK_PLANKS);
		set(level, bb, WATER, 34, BASE + 8, zMid);
		set(level, bb, WATER, 34, BASE + 9, zMid);
		for (int gz = zMid - 4; gz <= zMid + 4; gz++) {
			set(level, bb, GOLD, 44, BASE + 8, gz);
			set(level, bb, GOLD, 44, BASE + 10, gz);
			set(level, bb, (Math.abs(gz - zMid) % 2 == 1) ? YELLOW_GLASS : COPPER_OXIDIZED, 43, BASE + 9, gz);
		}
		set(level, bb, Blocks.OCHRE_FROGLIGHT.defaultBlockState(), 44, BASE + 11, zMid - 4);
		set(level, bb, Blocks.OCHRE_FROGLIGHT.defaultBlockState(), 44, BASE + 11, zMid + 4);
		set(level, bb, Blocks.OCHRE_FROGLIGHT.defaultBlockState(), 44, BASE + 11, zMid);
		set(level, bb, Blocks.CARTOGRAPHY_TABLE.defaultBlockState(), 38, BASE + 8, zMid);
		set(level, bb, Blocks.LECTERN.defaultBlockState(), 40, BASE + 8, zMid - 2);
		set(level, bb, Blocks.BOOKSHELF.defaultBlockState(), 42, BASE + 8, zMid - 3);
		set(level, bb, Blocks.BOOKSHELF.defaultBlockState(), 42, BASE + 8, zMid + 3);
		chest(level, bb, 41, BASE + 8, zMid + 2, Direction.WEST, OceanSites.GALLEON_LOOT, 14L);

		// 3 Tall Masts, Crow's Nests, Diagonal Rigging & Billowing Tattered Sails
		buildMast(level, bb, 12, BASE + 8, 23, 4, zMid);
		buildMast(level, bb, 23, BASE + 8, 27, 5, zMid);
		buildMast(level, bb, 35, BASE + 11, 25, 4, zMid);

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
		int actualTop = Math.min(HEIGHT - 1, topY);
		for (int y = baseY; y <= actualTop; y++) {
			set(level, bb, STRIPPED_SPRUCE, mx, y, zMid);
		}
		// Dark oak crow's nest platform below top yard
		int nestY = Math.max(baseY + 4, actualTop - 3);
		fill(level, bb, mx - 1, nestY, zMid - 1, mx + 1, nestY, zMid + 1, DARK_OAK_PLANKS);
		set(level, bb, Blocks.OCHRE_FROGLIGHT.defaultBlockState(), mx, actualTop, zMid);
		int sailLow = baseY + 3;
		int sailHigh = Math.min(HEIGHT - 2, actualTop - 2);
		for (int z = zMid - halfYard; z <= zMid + halfYard; z++) {
			set(level, bb, Blocks.SPRUCE_FENCE.defaultBlockState().trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), mx, sailHigh, z);
			for (int y = sailLow; y < sailHigh; y++) {
				if ((mx + y * 2 + z) % 6 == 0) continue; // Tattered battle holes
				int bellyOffset = (Math.abs(z - zMid) <= halfYard - 1 && y > sailLow && y < sailHigh - 1) ? 2 : 1;
				BlockState cloth = ((y + z) % 4 == 0) ? MOSS
					: (((mx + y) % 3 == 0) ? GREEN_WOOL : Blocks.WOOL.white().defaultBlockState());
				set(level, bb, cloth, mx + bellyOffset, y, z);
			}
		}
		// Diagonal chain shrouds from crow's nest down to gunwales
		for (int i = 0; i < 4; i++) {
			set(level, bb, Blocks.IRON_CHAIN.defaultBlockState().trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), mx, nestY - i, zMid - 2 - i);
			set(level, bb, Blocks.IRON_CHAIN.defaultBlockState().trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), mx, nestY - i, zMid + 2 + i);
		}
	}
}
