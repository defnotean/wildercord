package dev.wildercord.world.sites.mine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;

/**
 * A crystal survey lab: a calcite lab house and a watchtower in a walled forecourt broken by amethyst outcrops, the house
 * floor's hatch over a ladder shaft 24 blocks down, and a geode of basalt, calcite and amethyst carved whole around a plank
 * platform with the surveyors' lectern and chests. The geode's three-layer shell is set all round, so no cave water or lava
 * gets in. Peaceful. Local y 33 is the ground.
 */
public final class CrystalLabPiece extends MinePiece {
	static final int WIDTH = 21, HEIGHT = 52, DEPTH = 21, SURFACE = 33, CX = 10, CY = 9, CZ = 10, PLATFORM = 6;

	public CrystalLabPiece(int x, int y, int z, Direction facing) {
		super(MineSites.CRYSTAL_LAB, x, y, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public CrystalLabPiece(CompoundTag tag) {
		super(MineSites.CRYSTAL_LAB, tag);
	}

	/** Under dry ground, above the deep dark, and not where the ground's own water or lava lies. */
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		var chunk = c.chunkPos();
		var piece = new CrystalLabPiece(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(), Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
		BlockPos top = piece.getWorldPos(CX, 0, CZ);
		if (wet(c, top)) return Optional.empty();
		int surface = ground(c, top), base = surface - SURFACE;
		if (base < c.heightAccessor().getMinY() + 8 || deepDark(c, top.getX(), base + CY, top.getZ())) return Optional.empty();
		for (BlockPos at : new BlockPos[] {top, piece.getWorldPos(1, 0, CZ), piece.getWorldPos(19, 0, CZ), piece.getWorldPos(CX, 0, 1), piece.getWorldPos(CX, 0, 19)}) {
			if (fluidIn(c, at, base, base + 19)) return Optional.empty();
		}
		piece.move(0, base, 0);
		return Optional.of(new Structure.GenerationStub(new BlockPos(top.getX(), surface, top.getZ()), b -> b.addPiece(piece)));
	}

	private static double d(int x, int y, int z) {
		return Math.sqrt((x - CX) * (x - CX) + (y - CY) * (y - CY) + (z - CZ) * (z - CZ));
	}

	static boolean hollow(int x, int y, int z) {
		return d(x, y, z) <= 6.8;
	}

	private static boolean amethyst(int x, int y, int z) {
		double d = d(x, y, z);
		return d > 6.8 && d <= 7.7;
	}

	/** A platform cell on the rim, where a neighbour falls outside the hollow. */
	private static boolean rim(int x, int z) {
		return hollow(x, PLATFORM, z) && (!hollow(x + 1, PLATFORM, z) || !hollow(x - 1, PLATFORM, z) || !hollow(x, PLATFORM, z + 1) || !hollow(x, PLATFORM, z - 1));
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb, ChunkPos chunk,
			BlockPos reference) {
		// The geode: smooth basalt, calcite, amethyst (some of it budding), hollow inside.
		for (int x = 0; x < WIDTH; x++) for (int y = 0; y <= 19; y++) for (int z = 0; z < DEPTH; z++) {
			double d = d(x, y, z);
			if (d > 9.5) continue;
			BlockState state = d > 8.6 ? s(Blocks.SMOOTH_BASALT) : d > 7.7 ? s(Blocks.CALCITE)
				: d > 6.8 ? s(noise(x, y, z) < 12 ? Blocks.BUDDING_AMETHYST : Blocks.AMETHYST_BLOCK)
				: y < PLATFORM && amethyst(x, y - 1, z) && noise(z, y, x) < 35 ? s(Blocks.AMETHYST_CLUSTER) : AIR;
			set(level, bb, state, x, y, z);
		}
		// The surveyors' platform: dark oak boards inside a stripped-log rim, a calcite cross laid into it, on four posts.
		for (int x = 0; x < WIDTH; x++) for (int z = 0; z < DEPTH; z++) {
			if (!hollow(x, PLATFORM, z)) continue;
			boolean cross = (x == CX || z == CZ) && !(x == CX && z == CZ) && Math.abs(x - CX) + Math.abs(z - CZ) <= 3;
			set(level, bb, rim(x, z) ? s(Blocks.STRIPPED_DARK_OAK_LOG) : cross ? s(Blocks.CALCITE) : s(Blocks.DARK_OAK_PLANKS), x, PLATFORM, z);
			if (rim(x, z)) set(level, bb, s(Blocks.DARK_OAK_FENCE), x, PLATFORM + 1, z);
		}
		for (int dx : new int[] {-3, 3}) for (int dz : new int[] {-3, 3}) {
			for (int y = 0; y < PLATFORM; y++) if (hollow(CX + dx, y, CZ + dz)) set(level, bb, s(Blocks.DARK_OAK_LOG), CX + dx, y, CZ + dz);
		}
		// The bench: lectern, table and stand where they always were, the survey's shelves along the west rim.
		int y = PLATFORM + 1;
		set(level, bb, s(Blocks.LECTERN), 7, y, 12);
		set(level, bb, s(Blocks.CARTOGRAPHY_TABLE), 8, y, 7);
		set(level, bb, s(Blocks.BREWING_STAND), 13, y, 8);
		chest(level, bb, 13, y, 10, MineSites.LAB, Direction.WEST, 431);
		chest(level, bb, 10, y, 14, MineSites.LAB, Direction.SOUTH, 432);
		set(level, bb, s(Blocks.LANTERN), 7, y, 9);
		set(level, bb, s(Blocks.LANTERN), 13, y, 12);
		set(level, bb, s(Blocks.TINTED_GLASS), 6, y, 10);
		set(level, bb, s(Blocks.AMETHYST_CLUSTER), 6, y + 1, 10);
		for (int z : new int[] {8, 12}) {
			set(level, bb, s(Blocks.BOOKSHELF), 5, y, z);
			set(level, bb, s(Blocks.BOOKSHELF), 5, y + 1, z);
			set(level, bb, s(Blocks.AMETHYST_CLUSTER), 5, y + 2, z);
		}
		// Lanterns on long chains from the crown of the geode, one over each quarter of the platform.
		hangingLantern(level, bb, CX - 4, 14, CZ, 3, false);
		hangingLantern(level, bb, CX + 4, 14, CZ, 3, false);
		hangingLantern(level, bb, CX, 14, CZ - 4, 3, false);
		hangingLantern(level, bb, CX, 14, CZ + 4, 3, false);
		// The shaft: a stone collar through the rock with a lit course every few blocks, a calcite post for the ladder below it.
		fill(level, bb, CX - 1, 15, CZ - 1, CX + 1, SURFACE - 1, CZ + 1, s(Blocks.STONE_BRICKS));
		for (int sy = 18; sy < SURFACE - 1; sy += 5) set(level, bb, s(Blocks.GLOWSTONE), CX - 1, sy, CZ);
		set(level, bb, s(Blocks.CHISELED_STONE_BRICKS), CX - 1, 15, CZ - 1);
		set(level, bb, s(Blocks.CHISELED_STONE_BRICKS), CX + 1, 15, CZ - 1);
		fill(level, bb, CX, y, CZ + 1, CX, 14, CZ + 1, s(Blocks.CALCITE));
		fill(level, bb, CX, y, CZ, CX, SURFACE - 1, CZ, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH));
		// The surface: a walled flagstone forecourt cleared of trees, a path in from the south between lantern posts.
		Palette flags = Palette.of(Blocks.STONE_BRICKS, 5, Blocks.MOSSY_STONE_BRICKS, 2, Blocks.CRACKED_STONE_BRICKS, 2, Blocks.COBBLESTONE, 1);
		clearCanopy(level, bb, SURFACE + 1);
		fill(level, bb, 1, SURFACE + 1, 0, 19, SURFACE + 18, 20, AIR);
		footing(level, bb, 2, 1, 18, 19, SURFACE, s(Blocks.COBBLESTONE));
		fill(level, bb, 2, SURFACE, 1, 18, SURFACE, 19, flags);
		for (int x = 2; x <= 18; x++) for (int z = 1; z <= 19; z++) {
			boolean edge = x == 2 || x == 18 || z == 1 || z == 19;
			if (!edge || (z == 1 && Math.abs(x - CX) <= 2) || noise(x, z) < 25) continue;
			set(level, bb, noise(z, x) < 40 ? s(Blocks.MOSSY_COBBLESTONE) : s(Blocks.COBBLESTONE), x, SURFACE + 1, z);
			if (noise(x, SURFACE, z) < 30) set(level, bb, s(Blocks.STONE_BRICK_SLAB), x, SURFACE + 2, z);
		}
		for (int z = 0; z <= 6; z++) for (int x = CX - 1; x <= CX + 1; x++) {
			set(level, bb, x == CX && noise(x, z) < 35 ? s(Blocks.DIRT_PATH) : s(Blocks.POLISHED_ANDESITE), x, SURFACE, z);
		}
		for (int dx : new int[] {-2, 2}) for (int z : new int[] {2, 5}) {
			fill(level, bb, CX + dx, SURFACE + 1, z, CX + dx, SURFACE + 2, z, s(Blocks.SPRUCE_FENCE));
			set(level, bb, s(Blocks.LANTERN), CX + dx, SURFACE + 3, z);
		}
		// The lab house round the hatch: a stone-brick plinth course, calcite walls between basalt posts, amethyst-glass
		// windows over slab sills and a steep deepslate gable roof, the doorway on the south under a stair lintel.
		int x0 = 5, z0 = 7, x1 = 15, z1 = 14;
		footing(level, bb, x0, z0, x1, z1, SURFACE, s(Blocks.STONE_BRICKS));
		fill(level, bb, x0, SURFACE, z0, x1, SURFACE, z1, s(Blocks.STONE_BRICKS));
		fill(level, bb, x0 + 1, SURFACE, z0 + 1, x1 - 1, SURFACE, z1 - 1, s(Blocks.SPRUCE_PLANKS));
		walls(level, bb, x0, SURFACE + 1, z0, x1, SURFACE + 1, z1, Palette.of(Blocks.STONE_BRICKS, 4, Blocks.MOSSY_STONE_BRICKS, 1));
		walls(level, bb, x0, SURFACE + 2, z0, x1, SURFACE + 5, z1, Palette.of(Blocks.CALCITE, 1));
		walls(level, bb, x0, SURFACE + 6, z0, x1, SURFACE + 6, z1, Palette.of(Blocks.STRIPPED_SPRUCE_LOG, 1));
		for (int px : new int[] {x0, CX, x1}) for (int pz : new int[] {z0, z1}) fill(level, bb, px, SURFACE + 1, pz, px, SURFACE + 6, pz, s(Blocks.POLISHED_BASALT));
		for (int pz : new int[] {z0, z1}) for (int wx : new int[] {7, 8, 12, 13}) {
			fill(level, bb, wx, SURFACE + 3, pz, wx, SURFACE + 4, pz, s(Blocks.STAINED_GLASS.purple()));
			set(level, bb, s(Blocks.STONE_BRICK_SLAB), wx, SURFACE + 2, pz == z0 ? pz - 1 : pz + 1);
		}
		for (int px : new int[] {x0, x1}) for (int wz : new int[] {10, 11}) fill(level, bb, px, SURFACE + 3, wz, px, SURFACE + 4, wz, s(Blocks.STAINED_GLASS.purple()));
		fill(level, bb, CX, SURFACE + 1, z0, CX, SURFACE + 3, z0, AIR);
		set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true), CX - 1, SURFACE + 4, z0 - 1);
		set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, true), CX + 1, SURFACE + 4, z0 - 1);
		set(level, bb, s(Blocks.CHISELED_STONE_BRICKS), CX, SURFACE + 4, z0 - 1);
		set(level, bb, s(Blocks.AMETHYST_BLOCK), CX, SURFACE + 4, z0);
		set(level, bb, s(Blocks.LANTERN), CX - 2, SURFACE + 1, z0 - 1);
		set(level, bb, s(Blocks.LANTERN), CX + 2, SURFACE + 1, z0 - 1);
		int ridge = gableRoof(level, bb, x0, z0, x1, z1, SURFACE + 6, true, Blocks.DEEPSLATE_TILE_STAIRS, s(Blocks.DEEPSLATE_TILES),
			Palette.of(Blocks.CALCITE, 4, Blocks.SMOOTH_BASALT, 1));
		for (int ex : new int[] {x0 - 1, x1 + 1}) for (int ez : new int[] {10, 11}) set(level, bb, s(Blocks.AMETHYST_CLUSTER), ex, ridge + 1, ez);
		for (int ex : new int[] {x0, x1}) for (int ez : new int[] {10, 11}) set(level, bb, s(Blocks.STAINED_GLASS.purple()), ex, SURFACE + 8, ez);
		// A chimney off the furnace corner, standing proud of the ridge.
		fill(level, bb, x1 - 1, SURFACE + 1, z1 - 1, x1 - 1, ridge + 2, z1 - 1, Palette.of(Blocks.COBBLESTONE, 3, Blocks.MOSSY_COBBLESTONE, 1));
		set(level, bb, s(Blocks.STONE_BRICK_WALL), x1 - 1, ridge + 3, z1 - 1);
		set(level, bb, s(Blocks.BLAST_FURNACE), x1 - 1, SURFACE + 1, z1 - 2);
		// Inside: the deepslate-ringed hatch over the ladder, crystal benches along the walls, lanterns from the tie beam.
		fill(level, bb, CX - 1, SURFACE, CZ - 1, CX + 1, SURFACE, CZ + 1, s(Blocks.POLISHED_DEEPSLATE));
		set(level, bb, s(Blocks.SPRUCE_TRAPDOOR).setValue(TrapDoorBlock.HALF, Half.TOP), CX, SURFACE, CZ);
		for (int bx = x0 + 1; bx <= x0 + 3; bx++) set(level, bb, topSlab(Blocks.SPRUCE_SLAB), bx, SURFACE + 1, z1 - 1);
		set(level, bb, s(Blocks.AMETHYST_CLUSTER), x0 + 1, SURFACE + 2, z1 - 1);
		set(level, bb, s(Blocks.TINTED_GLASS), x0 + 3, SURFACE + 2, z1 - 1);
		set(level, bb, s(Blocks.SMITHING_TABLE), x0 + 1, SURFACE + 1, z0 + 1);
		set(level, bb, s(Blocks.STONECUTTER), x0 + 2, SURFACE + 1, z0 + 1);
		set(level, bb, s(Blocks.BOOKSHELF), x0 + 1, SURFACE + 1, 10);
		set(level, bb, s(Blocks.BOOKSHELF), x0 + 1, SURFACE + 1, 11);
		set(level, bb, s(Blocks.LECTERN), x1 - 1, SURFACE + 1, z0 + 1);
		barrel(level, bb, x1 - 2, SURFACE + 1, z0 + 1, null, 0);
		fill(level, bb, x0 + 1, SURFACE + 6, CZ, x1 - 1, SURFACE + 6, CZ, s(Blocks.STRIPPED_SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		hangingLantern(level, bb, 7, SURFACE + 5, CZ, 0, false);
		hangingLantern(level, bb, 13, SURFACE + 5, CZ, 0, false);
		// The watchtower at the north-east corner: stone brick on basalt quoins, a ladder to a railed lookout under an
		// amethyst-crowned cap.
		int tx0 = 15, tz0 = 14, tx1 = 19, tz1 = 18, top = SURFACE + 11;
		footing(level, bb, tx0, tz0, tx1, tz1, SURFACE, s(Blocks.STONE_BRICKS));
		fill(level, bb, tx0, SURFACE, tz0, tx1, SURFACE, tz1, s(Blocks.STONE_BRICKS));
		walls(level, bb, tx0, SURFACE + 1, tz0, tx1, top - 1, tz1, Palette.of(Blocks.STONE_BRICKS, 5, Blocks.MOSSY_STONE_BRICKS, 1, Blocks.CRACKED_STONE_BRICKS, 1));
		fill(level, bb, tx0 + 1, SURFACE + 1, tz0 + 1, tx1 - 1, top - 2, tz1 - 1, AIR);
		for (int px : new int[] {tx0, tx1}) for (int pz : new int[] {tz0, tz1}) fill(level, bb, px, SURFACE + 1, pz, px, top + 2, pz, s(Blocks.POLISHED_BASALT));
		fill(level, bb, 17, SURFACE + 1, tz0, 17, SURFACE + 2, tz0, AIR);
		set(level, bb, s(Blocks.CHISELED_STONE_BRICKS), 17, SURFACE + 3, tz0);
		for (int wy : new int[] {SURFACE + 4, SURFACE + 8}) {
			set(level, bb, s(Blocks.STAINED_GLASS.purple()), 17, wy, tz1);
			set(level, bb, s(Blocks.STAINED_GLASS.purple()), tx1, wy, 16);
		}
		fill(level, bb, tx0 + 1, top - 1, tz0 + 1, tx1 - 1, top - 1, tz1 - 1, s(Blocks.SPRUCE_PLANKS));
		fill(level, bb, 17, SURFACE + 1, 17, 17, top - 2, 17, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH));
		set(level, bb, s(Blocks.SPRUCE_TRAPDOOR).setValue(TrapDoorBlock.HALF, Half.TOP), 17, top - 1, 17);
		for (int x = tx0; x <= tx1; x++) for (int z = tz0; z <= tz1; z++) {
			boolean corner = (x == tx0 || x == tx1) && (z == tz0 || z == tz1), edge = x == tx0 || x == tx1 || z == tz0 || z == tz1;
			if (edge && !corner) set(level, bb, s(Blocks.SPRUCE_FENCE), x, top, z);
		}
		hipRoof(level, bb, tx0 + 1, tz0 + 1, tx1 - 1, tz1 - 1, top + 3, Blocks.DEEPSLATE_TILE_STAIRS, s(Blocks.DEEPSLATE_TILES), s(Blocks.CALCITE));
		set(level, bb, s(Blocks.AMETHYST_CLUSTER), 17, top + 6, 16);
		hangingLantern(level, bb, 17, top + 2, 16, 0, false);
		// Crystal outcrops breaking up through the yard: basalt and calcite collars round amethyst spurs.
		outcrop(level, bb, 4, 4, 4);
		outcrop(level, bb, 16, 4, 3);
		outcrop(level, bb, 4, 17, 5);
		outcrop(level, bb, 12, 17, 2);
		// The survey pole by the west wall, the sighting glass on its cairn to the east, and stores by the door.
		set(level, bb, s(Blocks.COBBLESTONE), 3, SURFACE + 1, 10);
		fill(level, bb, 3, SURFACE + 2, 10, 3, SURFACE + 5, 10, s(Blocks.SPRUCE_FENCE));
		set(level, bb, Blocks.LIGHTNING_ROD.weathering().unaffected().defaultBlockState(), 3, SURFACE + 6, 10);
		set(level, bb, s(Blocks.COBBLESTONE), 17, SURFACE + 1, 10);
		set(level, bb, s(Blocks.TINTED_GLASS), 17, SURFACE + 2, 10);
		set(level, bb, s(Blocks.AMETHYST_CLUSTER), 17, SURFACE + 3, 10);
		barrel(level, bb, CX + 3, SURFACE + 1, z0 - 1, null, 0);
	}

	/** An amethyst spur {@code h} high out of a basalt and calcite collar, with buds on its shoulders. */
	private void outcrop(WorldGenLevel level, BoundingBox bb, int x, int z, int h) {
		for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
			if (dx == 0 && dz == 0) continue;
			boolean side = dx == 0 || dz == 0;
			set(level, bb, side ? s(Blocks.CALCITE) : s(Blocks.SMOOTH_BASALT), x + dx, SURFACE, z + dz);
			if (side && noise(x + dx, z + dz) < 60) set(level, bb, s(Blocks.SMOOTH_BASALT), x + dx, SURFACE + 1, z + dz);
		}
		fill(level, bb, x, SURFACE, z, x, SURFACE + h, z, s(Blocks.AMETHYST_BLOCK));
		set(level, bb, s(Blocks.AMETHYST_CLUSTER), x, SURFACE + h + 1, z);
		if (h > 2) {
			set(level, bb, s(Blocks.AMETHYST_BLOCK), x + 1, SURFACE + 2, z);
			set(level, bb, s(Blocks.LARGE_AMETHYST_BUD), x + 1, SURFACE + 3, z);
			set(level, bb, s(Blocks.AMETHYST_BLOCK), x, SURFACE + 2, z - 1);
			set(level, bb, s(Blocks.MEDIUM_AMETHYST_BUD), x, SURFACE + 3, z - 1);
		}
	}
}
