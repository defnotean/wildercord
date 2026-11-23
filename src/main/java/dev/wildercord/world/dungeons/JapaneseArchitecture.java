package dev.wildercord.world.dungeons;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;

/**
 * Shared Japanese architectural builders used across {@code SakuraVillagePiece},
 * {@code CatSanctuaryPiece}, and {@code LanternBathhousePiece} so every structure renders
 * crisp Myojin Torii gates, ishidoro stone lanterns, weeping sakura trees, upturned
 * irimoya/pagoda eaves, shoji screens, and tea-house checkpoints from one source.
 */
public final class JapaneseArchitecture {
	private JapaneseArchitecture() {}

	@FunctionalInterface
	public interface BlockPlacer {
		void set(int x, int y, int z, BlockState state);
	}

	public static BlockState stair(Block stairs, Direction facing, boolean top) {
		return stairs.defaultBlockState()
			.setValue(StairBlock.FACING, facing)
			.setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
	}

	public static BlockState slab(Block slabBlock, SlabType type) {
		return slabBlock.defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE, type);
	}

	public static BlockState openTrapdoor(Block trapdoor, Direction facing) {
		return trapdoor.defaultBlockState()
			.setValue(TrapDoorBlock.FACING, facing)
			.setValue(TrapDoorBlock.OPEN, true)
			.setValue(TrapDoorBlock.HALF, Half.BOTTOM);
	}

	/** Grand vermilion Myojin Torii gate with stone plinths, twin lintels, upturned deepslate wings & optional gold tablet. */
	public static void toriiGate(BlockPlacer p, int cx, int baseY, int z, int halfSpan, int topY, boolean goldTablet) {
		BlockState red = Blocks.RED_TERRACOTTA.defaultBlockState();
		BlockState plinth = Blocks.DEEPSLATE_TILES.defaultBlockState();
		BlockState cap = Blocks.POLISHED_ANDESITE.defaultBlockState();
		int lx = cx - halfSpan;
		int rx = cx + halfSpan;

		for (int x : new int[]{lx, rx}) {
			p.set(x, baseY, z, plinth);
			p.set(x, baseY + 1, z, plinth);
			for (int y = baseY + 2; y <= topY - 1; y++) {
				p.set(x, y, z, red);
			}
			p.set(x, topY, z, plinth);
		}
		// Lower nuki tie-beam extending 1 block beyond each pillar.
		for (int x = lx - 1; x <= rx + 1; x++) {
			p.set(x, topY - 2, z, red);
		}
		// Upper kasagi curved lintel with upturned wing tips.
		for (int x = lx - 1; x <= rx + 1; x++) {
			p.set(x, topY, z, plinth);
		}
		p.set(lx - 2, topY, z, stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.WEST, false));
		p.set(rx + 2, topY, z, stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.EAST, false));
		p.set(lx - 2, topY + 1, z, slab(Blocks.DEEPSLATE_TILE_SLAB, SlabType.BOTTOM));
		p.set(rx + 2, topY + 1, z, slab(Blocks.DEEPSLATE_TILE_SLAB, SlabType.BOTTOM));
		if (goldTablet) {
			p.set(cx, topY - 1, z, Blocks.GOLD_BLOCK.defaultBlockState());
		} else {
			p.set(cx, topY - 1, z, cap);
		}
	}

	/** Carved mossy ishidoro stone garden lantern: plinth, post, glowing core, and stone umbrella cap. */
	public static void ishidoro(BlockPlacer p, int x, int baseY, int z) {
		p.set(x, baseY, z, Blocks.POLISHED_ANDESITE.defaultBlockState());
		p.set(x, baseY + 1, z, Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState());
		p.set(x, baseY + 2, z, Blocks.OCHRE_FROGLIGHT.defaultBlockState());
		p.set(x, baseY + 3, z, slab(Blocks.POLISHED_ANDESITE_SLAB, SlabType.BOTTOM));
	}

	/** Sweeping multi-branch weeping cherry blossom tree with drooping petal fringes. */
	public static void weepingCherryTree(BlockPlacer p, int cx, int baseY, int cz, int height, boolean wide) {
		BlockState log = Blocks.CHERRY_LOG.defaultBlockState();
		BlockState leaves = Blocks.CHERRY_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
		for (int y = baseY; y < baseY + height; y++) {
			p.set(cx, y, cz, log);
		}
		// Diagonal upper limbs.
		int topY = baseY + height;
		p.set(cx + 1, topY - 1, cz, log);
		p.set(cx - 1, topY - 1, cz + 1, log);
		p.set(cx, topY, cz - 1, log);

		int r = wide ? 3 : 2;
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				for (int dy = -1; dy <= 2; dy++) {
					int d2 = dx * dx + dz * dz + dy * dy * 2;
					if (d2 <= (wide ? 10 : 6)) {
						p.set(cx + dx, topY + dy, cz + dz, leaves);
					}
				}
				// Weeping drooping outer petal tips around rim.
				int ring = dx * dx + dz * dz;
				if (ring >= (wide ? 6 : 4) && ring <= (wide ? 10 : 6) && Math.floorMod(dx * 7 + dz * 13, 3) != 0) {
					p.set(cx + dx, topY - 2, cz + dz, leaves);
					if (wide && Math.floorMod(dx + dz * 5, 2) == 0) {
						p.set(cx + dx, topY - 3, cz + dz, leaves);
					}
				}
			}
		}
	}

	/** Timber-framed shoji & wainscot wall panels around a rectangular room perimeter. */
	public static void shojiPerimeter(BlockPlacer p, int x0, int y0, int z0, int x1, int y1, int z1,
			BlockState pillar, BlockState rail, BlockState baseTrim, BlockState paper, int bayStep) {
		for (int y = y0; y <= y1; y++) {
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					boolean onX = x == x0 || x == x1;
					boolean onZ = z == z0 || z == z1;
					if (!onX && !onZ) {
						continue;
					}
					boolean corner = onX && onZ;
					boolean post = (onZ && (x - x0) % bayStep == 0) || (onX && (z - z0) % bayStep == 0);
					BlockState chosen;
					if (corner || post) {
						chosen = pillar;
					} else if (y == y0) {
						chosen = baseTrim;
					} else if (y == y1 || y == y0 + 3) {
						chosen = rail;
					} else {
						chosen = paper;
					}
					p.set(x, y, z, chosen);
				}
			}
		}
	}

	/** Ornate pagoda / irimoya eave skirt with double overhanging ring, upturned corner tips & hanging corner lanterns. */
	public static void upturnedEaveSkirt(BlockPlacer p, int x0, int z0, int x1, int z1, int y,
			Block stairs, Block slabBlock, BlockState trimUnder, boolean doubleOverhang) {
		for (int x = x0 + 1; x < x1; x++) {
			p.set(x, y, z0, stair(stairs, Direction.NORTH, false));
			p.set(x, y, z1, stair(stairs, Direction.SOUTH, false));
			if (doubleOverhang) {
				p.set(x, y + 1, z0 + 1, trimUnder);
				p.set(x, y + 1, z1 - 1, trimUnder);
			}
		}
		for (int z = z0 + 1; z < z1; z++) {
			p.set(x0, y, z, stair(stairs, Direction.EAST, false));
			p.set(x1, y, z, stair(stairs, Direction.WEST, false));
			if (doubleOverhang) {
				p.set(x0 + 1, y + 1, z, trimUnder);
				p.set(x1 - 1, y + 1, z, trimUnder);
			}
		}
		// Outer corner mitered stairs + upturned eave horn tips + hanging lanterns.
		p.set(x0, y, z0, stair(stairs, Direction.NORTH, false).setValue(StairBlock.SHAPE, StairsShape.OUTER_RIGHT));
		p.set(x1, y, z0, stair(stairs, Direction.NORTH, false).setValue(StairBlock.SHAPE, StairsShape.OUTER_LEFT));
		p.set(x0, y, z1, stair(stairs, Direction.SOUTH, false).setValue(StairBlock.SHAPE, StairsShape.OUTER_LEFT));
		p.set(x1, y, z1, stair(stairs, Direction.SOUTH, false).setValue(StairBlock.SHAPE, StairsShape.OUTER_RIGHT));

		for (int[] c : new int[][]{{x0 - 1, z0 - 1, x0, z0}, {x1 + 1, z0 - 1, x1, z0}, {x0 - 1, z1 + 1, x0, z1}, {x1 + 1, z1 + 1, x1, z1}}) {
			p.set(c[0], y, c[1], slab(slabBlock, SlabType.TOP));
			p.set(c[2], y - 1, c[3], Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
		}
	}

	/** Broad two-ring curved dark pagoda roof apron for monumental tiered pagoda stories. */
	public static void pagodaTierRoofApron(BlockPlacer p, int x0, int z0, int x1, int z1, int y,
			Block stairs, Block slabBlock, BlockState underTrim) {
		upturnedEaveSkirt(p, x0, z0, x1, z1, y, stairs, slabBlock, underTrim, true);
		int ox0 = x0 - 1, oz0 = z0 - 1, ox1 = x1 + 1, oz1 = z1 + 1, oy = y - 1;
		for (int x = ox0 + 1; x < ox1; x++) {
			p.set(x, oy, oz0, stair(stairs, Direction.NORTH, false));
			p.set(x, oy, oz1, stair(stairs, Direction.SOUTH, false));
		}
		for (int z = oz0 + 1; z < oz1; z++) {
			p.set(ox0, oy, z, stair(stairs, Direction.EAST, false));
			p.set(ox1, oy, z, stair(stairs, Direction.WEST, false));
		}
		p.set(ox0, oy, oz0, stair(stairs, Direction.NORTH, false).setValue(StairBlock.SHAPE, StairsShape.OUTER_RIGHT));
		p.set(ox1, oy, oz0, stair(stairs, Direction.NORTH, false).setValue(StairBlock.SHAPE, StairsShape.OUTER_LEFT));
		p.set(ox0, oy, oz1, stair(stairs, Direction.SOUTH, false).setValue(StairBlock.SHAPE, StairsShape.OUTER_LEFT));
		p.set(ox1, oy, oz1, stair(stairs, Direction.SOUTH, false).setValue(StairBlock.SHAPE, StairsShape.OUTER_RIGHT));
		for (int[] c : new int[][]{{ox0 - 1, oz0 - 1, ox0, oz0}, {ox1 + 1, oz0 - 1, ox1, oz0}, {ox0 - 1, oz1 + 1, ox0, oz1}, {ox1 + 1, oz1 + 1, ox1, oz1}}) {
			p.set(c[0], oy, c[1], slab(slabBlock, SlabType.TOP));
			p.set(c[2], oy - 1, c[3], Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
		}
	}

	/** Open-front Tea-House Checkpoint Pavilion matching Floors 1–3 interior references (never encloses airtight walls). */
	public static void teaCheckpointPavilion(BlockPlacer p, int x0, int floorY, int z0, int x1, int z1,
			BlockState postBlock, BlockState roofBlock, BlockState carpetBlock, Direction openTowardRoom) {
		int y = floorY + 1;
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				p.set(x, floorY, z, Blocks.BAMBOO_PLANKS.defaultBlockState());
				if (x > x0 && x < x1 && z > z0 && z < z1) {
					p.set(x, y, z, carpetBlock);
				}
				p.set(x, y + 4, z, roofBlock);
			}
		}
		// Four corner timber posts + open folding byobu screens on the outer wall side only (walking stays wide open!).
		for (int px : new int[]{x0, x1}) {
			for (int pz : new int[]{z0, z1}) {
				for (int yy = y; yy <= y + 3; yy++) {
					p.set(px, yy, pz, postBlock);
				}
			}
		}
		int backZ = openTowardRoom == Direction.NORTH ? z0 : z1;
		for (int x = x0 + 1; x <= x1 - 1; x++) {
			p.set(x, y, backZ, openTrapdoor(Blocks.SPRUCE_TRAPDOOR, openTowardRoom));
			p.set(x, y + 1, backZ, openTrapdoor(Blocks.SPRUCE_TRAPDOOR, openTowardRoom));
		}
		// Central tea hearth campfire & suspended iron cauldron kettle.
		int cx = (x0 + x1) / 2;
		int cz = (z0 + z1) / 2;
		p.set(cx, y, cz, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
		p.set(cx, y + 2, cz, Blocks.CAULDRON.defaultBlockState());
		p.set(cx, y + 3, cz, Blocks.IRON_CHAIN.defaultBlockState());
		p.set(x0 + 1, y + 3, z0 + 1, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
		p.set(x1 - 1, y + 3, z1 - 1, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
	}
}
