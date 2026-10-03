package dev.wildercord.world.dungeons;

import dev.wildercord.aura.world.SleepingBladeEntity;
import dev.wildercord.aura.world.SleepingBladeRules;
import dev.wildercord.aura.world.SleepingBladeStone;
import dev.wildercord.aura.world.SleepingBlades;
import dev.wildercord.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import java.util.List;
import java.util.Optional;

/** A small, dry highland resting place: broken cairns, a lost threshold, or the final shelter. */
public final class SleepingBladePiece extends DungeonPiece {
	public SleepingBladePiece(int x, int y, int z, Direction facing) { super(DungeonWorldgen.SLEEPING_BLADE_PIECE, x, y, z, 19, 9, 19, facing); }
	public SleepingBladePiece(CompoundTag tag) { super(DungeonWorldgen.SLEEPING_BLADE_PIECE, tag); }
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		if (!Config.get().auraWorld().sleepingBlades()) return Optional.empty();
		var piece = new SleepingBladePiece(c.chunkPos().getMinBlockX(), 0, c.chunkPos().getMinBlockZ(), Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
		var centre = piece.localPosition(9, 0, 9);
		int surface = height(c, centre, Heightmap.Types.WORLD_SURFACE_WG);
		int floor = height(c, centre, Heightmap.Types.OCEAN_FLOOR_WG);
		int[] neighbours = new int[4]; int i = 0;
		for (int[] d : new int[][]{{-7,0}, {7,0}, {0,-7}, {0,7}}) neighbours[i++] = height(c, centre.offset(d[0], 0, d[1]), Heightmap.Types.WORLD_SURFACE_WG);
		if (!SleepingBladeRules.footing(surface, floor, c.chunkGenerator().getSeaLevel(), neighbours)) return Optional.empty();
		piece.move(0, surface, 0);
		return Optional.of(new Structure.GenerationStub(centre.atY(surface), b -> b.addPiece(piece)));
	}
	private static int height(Structure.GenerationContext c, BlockPos p, Heightmap.Types h) {
		return c.chunkGenerator().getFirstOccupiedHeight(p.getX(), p.getZ(), h, c.heightAccessor(), c.randomState());
	}
	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		for (int x=3; x<=15; x++) for (int z=3; z<=15; z++) {
			int dx=x-9, dz=z-9, r=dx*dx+dz*dz;
			if (r<=10 || (r<40 && noise(x,z)<18)) {
				fill(level,bb,x,-4,z,x,-1,z,Blocks.STONE.defaultBlockState());
				set(level,bb,noise(x,z)<30?Blocks.MOSSY_COBBLESTONE.defaultBlockState():Blocks.STONE.defaultBlockState(),x,0,z);
				fill(level,bb,x,1,z,x,3,z,AIR);
			}
		}
		var weathered = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
		switch (variant()) {
			case 0 -> {
				for (int[] p : new int[][]{{4,5},{13,4},{14,13},{5,14}}) {
					fill(level,bb,p[0],-4,p[1],p[0],1,p[1],weathered);
					set(level,bb,Blocks.STONE_BRICK_SLAB.defaultBlockState(),p[0],2,p[1]);
				}
			}
			case 1 -> {
				fill(level,bb,4,-4,5,4,3,5,weathered); fill(level,bb,14,-4,5,14,1,5,weathered);
				fill(level,bb,4,3,5,7,3,5,weathered);
				set(level,bb,Blocks.CHISELED_STONE_BRICKS.defaultBlockState(),7,0,6);
			}
			default -> {
				fill(level,bb,4,-4,12,4,2,15,weathered); fill(level,bb,5,0,15,11,1,15,weathered);
				set(level,bb,Blocks.AZALEA.defaultBlockState(),6,2,15);
				set(level,bb,Blocks.FLOWERING_AZALEA.defaultBlockState(),10,2,15);
			}
		}
		// Facing is transformed like every authored block; the socket is the sole reward-bearing object.
		BlockPos at = localPosition(9,1,9);
		if (bb.isInside(at)) {
			var state = SleepingBlades.STONE.defaultBlockState().setValue(SleepingBladeStone.FACING,Direction.NORTH).mirror(getMirror()).rotate(getRotation());
			level.setBlock(at,state,2);
			if (level.getBlockEntity(at) instanceof SleepingBladeEntity stone) stone.awaken();
		}
	}
	@Override public List<BoundingBox> wardedBoxes() { return List.of(); }
}
