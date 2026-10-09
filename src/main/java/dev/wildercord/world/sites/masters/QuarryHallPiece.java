package dev.wildercord.world.sites.masters;

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
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

/** A gabled mountain hall of the Stone or Iron school around a sunken sparring ring, one Runebound sparring guard waiting in it. */
public final class QuarryHallPiece extends MasterSitePiece {
	public QuarryHallPiece(int x, int y, int z, Direction facing, boolean alt) { super(MasterSites.QUARRY_HALL, x, y, z, 19, 11, 19, facing, alt); }
	public QuarryHallPiece(CompoundTag tag) { super(MasterSites.QUARRY_HALL, tag); }
	@Override public String site() { return "master_quarry_hall"; }
	@Override public List<String> schools() { return List.of("stone", "iron"); }

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		BlockState wall = alt ? s(Blocks.DEEPSLATE_BRICKS) : s(Blocks.STONE_BRICKS), roof = alt ? s(Blocks.DEEPSLATE_TILES) : s(Blocks.COBBLESTONE);
		BlockState rim = alt ? s(Blocks.POLISHED_DEEPSLATE) : s(Blocks.POLISHED_ANDESITE), light = alt ? s(Blocks.SEA_LANTERN) : s(Blocks.SHROOMLIGHT);
		pad(level, bb, 0, 0, 18, 18, 10, s(Blocks.SMOOTH_STONE), s(Blocks.STONE));
		room(level, bb, 1, 0, 1, 17, 7, 17, wall);
		fill(level, bb, 1, 0, 1, 17, 0, 17, s(Blocks.SMOOTH_STONE));
		fill(level, bb, 1, 8, 1, 17, 8, 17, roof); fill(level, bb, 4, 9, 1, 14, 9, 17, roof); fill(level, bb, 7, 10, 1, 11, 10, 17, roof);
		fill(level, bb, 8, 1, 1, 10, 4, 1, AIR);
		for (int z : new int[]{5, 9, 13}) { fill(level, bb, 1, 3, z, 1, 4, z, s(Blocks.IRON_BARS)); fill(level, bb, 17, 3, z, 17, 4, z, s(Blocks.IRON_BARS)); }
		for (int[] p : new int[][]{{5, 5}, {13, 5}, {5, 15}, {13, 15}}) set(level, bb, light, p[0], 7, p[1]);
		// The sparring ring: a pit two blocks deep, lit, with a ladder out on the far side.
		for (int x = 3; x <= 15; x++) for (int z = 4; z <= 16; z++) {
			double d = dist(x, z, 9, 10);
			if (d <= 5.5) fill(level, bb, x, -3, z, x, -1, z, wall);
			if (d <= 3.5) { fill(level, bb, x, -2, z, x, 0, z, AIR); set(level, bb, s(noise(x, z) < 50 ? Blocks.GRAVEL : Blocks.COARSE_DIRT), x, -3, z); }
			else if (d <= 4.5) set(level, bb, rim, x, 0, z);
		}
		set(level, bb, light, 5, -2, 10); set(level, bb, light, 13, -2, 10);
		fill(level, bb, 9, -2, 13, 9, 0, 13, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH));
		chest(level, bb, 9, -2, 7, rewardLoot(), Direction.NORTH, 2);
		if (alt) guard(level, bb, EntityTypes.VINDICATOR, 9, -2, 10, List.of(Runes.ARC, Runes.TREMOR), false);
		else guard(level, bb, EntityTypes.HUSK, 9, -2, 10, List.of(Runes.NOVA, Runes.TREMOR), false);
		// Practice around the ring.
		var post = alt ? s(Blocks.IRON_BARS) : s(Blocks.OAK_FENCE);
		dummy(level, bb, 3, 1, 4, post); dummy(level, bb, 15, 1, 4, post); dummy(level, bb, 3, 1, 15, post);
		target(level, bb, 15, 1, 15, post);
		set(level, bb, alt ? s(Blocks.ANVIL) : s(Blocks.STONECUTTER), 15, 1, 9);
		set(level, bb, alt ? s(Blocks.RAW_IRON_BLOCK) : s(Blocks.CHISELED_STONE_BRICKS), 3, 1, 9);
		lectern(level, bb, 6, 1, 3);
		chest(level, bb, 12, 1, 2, MasterSites.supplies(site()), Direction.SOUTH, 1);
	}

	@Override public List<BoundingBox> wardedBoxes() { return List.of(); }
}
