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
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;

/**
 * A crystal survey lab: a cairn and survey pole on the surface over a trapdoor, a ladder shaft 24 blocks down, and a geode
 * of basalt, calcite and amethyst carved whole around a plank platform with the surveyors' lectern and chests. The geode's
 * three-layer shell is set all round, so no cave water or lava gets in. Peaceful. Local y 33 is the ground.
 */
public final class CrystalLabPiece extends MinePiece {
	static final int WIDTH = 21, HEIGHT = 40, DEPTH = 21, SURFACE = 33, CX = 10, CY = 9, CZ = 10, PLATFORM = 6;

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
		// The surveyors' platform and their bench.
		for (int x = 0; x < WIDTH; x++) for (int z = 0; z < DEPTH; z++) if (hollow(x, PLATFORM, z)) set(level, bb, s(Blocks.DARK_OAK_PLANKS), x, PLATFORM, z);
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
		// The shaft: a stone collar through the rock, a calcite post for the ladder below it.
		fill(level, bb, CX - 1, 15, CZ - 1, CX + 1, SURFACE - 1, CZ + 1, s(Blocks.STONE_BRICKS));
		fill(level, bb, CX, y, CZ + 1, CX, 14, CZ + 1, s(Blocks.CALCITE));
		fill(level, bb, CX, y, CZ, CX, SURFACE - 1, CZ, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH));
		// The cairn: a ring of cobble round a trapdoor, the survey pole and its crystal.
		fill(level, bb, CX - 3, SURFACE + 1, CZ - 3, CX + 3, SURFACE + 6, CZ + 3, AIR);
		fill(level, bb, CX - 1, SURFACE, CZ - 1, CX + 1, SURFACE, CZ + 1, s(Blocks.COBBLESTONE));
		set(level, bb, s(Blocks.SPRUCE_TRAPDOOR).setValue(TrapDoorBlock.HALF, Half.TOP), CX, SURFACE, CZ);
		for (int dx : new int[] {-1, 1}) for (int dz : new int[] {-1, 1}) set(level, bb, s(Blocks.MOSSY_COBBLESTONE_WALL), CX + dx, SURFACE + 1, CZ + dz);
		set(level, bb, s(Blocks.COBBLESTONE), CX + 3, SURFACE, CZ);
		fill(level, bb, CX + 3, SURFACE + 1, CZ, CX + 3, SURFACE + 3, CZ, s(Blocks.SPRUCE_FENCE));
		set(level, bb, Blocks.LIGHTNING_ROD.weathering().unaffected().defaultBlockState(), CX + 3, SURFACE + 4, CZ);
		set(level, bb, s(Blocks.COBBLESTONE), CX - 3, SURFACE, CZ);
		set(level, bb, s(Blocks.TINTED_GLASS), CX - 3, SURFACE + 1, CZ);
		set(level, bb, s(Blocks.AMETHYST_CLUSTER), CX - 3, SURFACE + 2, CZ);
		set(level, bb, s(Blocks.LANTERN), CX, SURFACE + 1, CZ + 2);
	}
}
