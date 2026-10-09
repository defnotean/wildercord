package dev.wildercord.world.sites.mine;

import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/**
 * A collapsed delve: a rubble mound and a broken headframe over a stair down to a propped antechamber, kept by one Runebound
 * miner. At its back the roof came down: a plug of gravel that keeps falling as it is dug (until the roof is shored up, or
 * dug from the side), and behind it a trapped villager beside the crew's cache. Local y 14 is the ground.
 */
public final class CollapsedDelvePiece extends MinePiece {
	static final int WIDTH = 17, HEIGHT = 22, DEPTH = 26, SURFACE = 14, FLOOR = 3;

	public CollapsedDelvePiece(int x, int y, int z, Direction facing) {
		super(MineSites.COLLAPSED_DELVE, x, y, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public CollapsedDelvePiece(CompoundTag tag) {
		super(MineSites.COLLAPSED_DELVE, tag);
	}

	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		var chunk = c.chunkPos();
		var piece = new CollapsedDelvePiece(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(), Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
		BlockPos entrance = piece.getWorldPos(8, 0, 1), back = piece.getWorldPos(8, 0, 22);
		if (wet(c, entrance) || wet(c, back)) return Optional.empty();
		int surface = ground(c, entrance);
		if (ground(c, back) < surface - 3) return Optional.empty();
		int base = surface - SURFACE;
		if (fluidIn(c, piece.getWorldPos(8, 0, 16), base, base + 10)) return Optional.empty();
		piece.move(0, base, 0);
		return Optional.of(new Structure.GenerationStub(new BlockPos(entrance.getX(), surface, entrance.getZ()), b -> b.addPiece(piece)));
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb, ChunkPos chunk,
			BlockPos reference) {
		BlockState cobble = s(Blocks.COBBLESTONE), log = s(Blocks.SPRUCE_LOG), gravel = s(Blocks.GRAVEL), brick = s(Blocks.STONE_BRICKS);
		// The rubble mound and the broken headframe over the stair.
		for (int x = 2; x <= 14; x++) for (int z = 0; z <= 9; z++) {
			int h = 3 - (int) dist(x, z, 8, 5) / 2 + (noise(x, z) < 30 ? 1 : 0);
			for (int y = SURFACE + 1; y <= SURFACE + h; y++) set(level, bb, noise(x, y, z) < 40 ? gravel : cobble, x, y, z);
		}
		fill(level, bb, 6, SURFACE + 1, 4, 6, SURFACE + 6, 4, log);
		fill(level, bb, 10, SURFACE + 1, 4, 10, SURFACE + 3, 4, log);
		fill(level, bb, 7, SURFACE + 3, 6, 11, SURFACE + 3, 6, s(Blocks.SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		// The stair: each step boxed in cobble, then dug out.
		for (int i = 0; i <= 9; i++) {
			int z = 2 + i, step = SURFACE - 1 - i;
			fill(level, bb, 6, step - 1, z, 10, step + 5, z, cobble);
			fill(level, bb, 7, step + 1, z, 9, step + 4, z, AIR);
			fill(level, bb, 7, step, z, 9, step, z, s(Blocks.STONE_BRICK_STAIRS).setValue(StairBlock.FACING, Direction.SOUTH));
		}
		fill(level, bb, 7, SURFACE, 0, 9, SURFACE + 4, 1, AIR);
		fill(level, bb, 6, SURFACE - 1, 0, 10, SURFACE - 1, 1, cobble);
		// The antechamber, propped on spruce frames.
		room(level, bb, 2, FLOOR, 12, 14, FLOOR + 6, 20, brick);
		for (int x = 2; x <= 14; x++) for (int z = 12; z <= 20; z++) if (noise(x, z) < 25) set(level, bb, s(Blocks.CRACKED_STONE_BRICKS), x, FLOOR + 6, z);
		fill(level, bb, 7, FLOOR + 1, 12, 9, FLOOR + 4, 12, AIR);
		for (int z : new int[] {14, 18}) {
			fill(level, bb, 3, FLOOR + 1, z, 3, FLOOR + 4, z, log);
			fill(level, bb, 13, FLOOR + 1, z, 13, FLOOR + 4, z, log);
			fill(level, bb, 3, FLOOR + 5, z, 13, FLOOR + 5, z, s(Blocks.STRIPPED_SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		}
		hangingLantern(level, bb, 8, FLOOR + 4, 14);
		chest(level, bb, 3, FLOOR + 1, 16, MineSites.DELVE, Direction.EAST, 441);
		barrel(level, bb, 13, FLOOR + 1, 16, null, 0);
		set(level, bb, s(Blocks.STRIPPED_SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X), 9, FLOOR + 1, 19);
		guard(level, bb, EntityTypes.ZOMBIE, 10, FLOOR + 1, 15, List.of(Runes.BOLT, Runes.MIRE), false);
		// The pocket the miner waited in: a solid roof, a lamp, the cache.
		room(level, bb, 4, FLOOR, 21, 12, FLOOR + 4, 25, brick);
		set(level, bb, s(Blocks.LANTERN), 5, FLOOR + 1, 24);
		chest(level, bb, 11, FLOOR + 1, 24, MineSites.DELVE_CACHE, Direction.WEST, 442);
		set(level, bb, Blocks.CARPET.pick(net.minecraft.world.item.DyeColor.WHITE).defaultBlockState(), 6, FLOOR + 1, 23);
		sentry(level, bb, EntityTypes.VILLAGER, 8, FLOOR + 1, 23);
		// The fall: gravel from the floor through the roof, between the last beams. Dug from below, the rest drops in.
		fill(level, bb, 6, FLOOR + 1, 20, 10, FLOOR + 3, 21, gravel);
		fill(level, bb, 6, FLOOR + 4, 19, 10, FLOOR + 8, 21, gravel);
		fill(level, bb, 5, FLOOR + 9, 18, 11, FLOOR + 9, 22, cobble);
		fill(level, bb, 6, FLOOR + 1, 19, 6, FLOOR + 3, 19, gravel);
		fill(level, bb, 10, FLOOR + 1, 19, 10, FLOOR + 3, 19, gravel);
	}
}
