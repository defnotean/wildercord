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
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

/** A stepped, root-wrapped jungle temple of the Verdant or Venom school; one Runebound bogged spars in the pit at its heart. */
public final class RootTemplePiece extends MasterSitePiece {
	public RootTemplePiece(int x, int y, int z, Direction facing, boolean alt) { super(MasterSites.ROOT_TEMPLE, x, y, z, 17, 12, 19, facing, alt); }
	public RootTemplePiece(CompoundTag tag) { super(MasterSites.ROOT_TEMPLE, tag); }
	@Override public String site() { return "master_root_temple"; }
	@Override public List<String> schools() { return List.of("verdant", "venom"); }

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		BlockState wall = alt ? s(Blocks.MUD_BRICKS) : s(Blocks.MOSSY_STONE_BRICKS), light = alt ? s(Blocks.OCHRE_FROGLIGHT) : s(Blocks.VERDANT_FROGLIGHT);
		BlockState leaves = (alt ? s(Blocks.DARK_OAK_LEAVES) : s(Blocks.FLOWERING_AZALEA_LEAVES)).setValue(LeavesBlock.PERSISTENT, true);
		pad(level, bb, 0, 0, 16, 18, 11, s(Blocks.MOSSY_COBBLESTONE), s(Blocks.DIRT));
		for (int x = 0; x <= 16; x++) for (int z = 0; z <= 18; z++) if (noise(x, z) < 35) set(level, bb, s(Blocks.MOSS_BLOCK), x, 0, z);
		room(level, bb, 1, 0, 2, 15, 6, 16, wall);
		fill(level, bb, 1, 0, 2, 15, 0, 16, s(Blocks.MOSSY_COBBLESTONE));
		fill(level, bb, 2, 7, 3, 14, 7, 15, wall); fill(level, bb, 4, 8, 5, 12, 8, 13, wall); fill(level, bb, 6, 9, 7, 10, 9, 11, wall);
		fill(level, bb, 7, 10, 8, 9, 10, 10, leaves);
		fill(level, bb, 7, 1, 2, 9, 3, 2, AIR);
		// Roots drape the outer walls.
		var roots = s(Blocks.MANGROVE_ROOTS);
		for (int z = 3; z <= 15; z++) { int a = noise(0, z) % 5, b = noise(16, z) % 5; if (a > 0) fill(level, bb, 0, 1, z, 0, a, z, roots); if (b > 0) fill(level, bb, 16, 1, z, 16, b, z, roots); }
		for (int x = 2; x <= 14; x++) { int c = noise(x, 17) % 4; if (c > 0) fill(level, bb, x, 1, 17, x, c, 17, roots); }
		for (int[] p : new int[][]{{4, 5}, {12, 5}, {4, 14}, {12, 14}}) set(level, bb, light, p[0], 6, p[1]);
		// The sparring pit.
		for (int x = 2; x <= 14; x++) for (int z = 4; z <= 16; z++) {
			double d = dist(x, z, 8, 10);
			if (d <= 5.5) fill(level, bb, x, -3, z, x, -1, z, wall);
			if (d <= 3.5) { fill(level, bb, x, -2, z, x, 0, z, AIR); set(level, bb, s(noise(x, z) < 50 ? Blocks.ROOTED_DIRT : Blocks.MOSS_BLOCK), x, -3, z); }
			else if (d <= 4.5) set(level, bb, s(Blocks.MOSSY_STONE_BRICKS), x, 0, z);
		}
		set(level, bb, light, 4, -2, 10); set(level, bb, light, 12, -2, 10);
		fill(level, bb, 8, -2, 13, 8, 0, 13, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH));
		chest(level, bb, 8, -2, 7, rewardLoot(), Direction.NORTH, 2);
		guard(level, bb, EntityTypes.BOGGED, 8, -2, 10, alt ? List.of(Runes.ARC, Runes.VENOM) : List.of(Runes.BOLT, Runes.ROOTSNARE), false);
		// Practice, the lectern and the garden corners.
		var post = s(Blocks.JUNGLE_FENCE);
		dummy(level, bb, 3, 1, 5, post); dummy(level, bb, 13, 1, 5, post);
		target(level, bb, 3, 1, 14, post); target(level, bb, 13, 1, 14, post);
		var plant = alt ? s(Blocks.FERN) : s(Blocks.FLOWERING_AZALEA);
		for (int[] p : new int[][]{{2, 3}, {14, 15}, {2, 15}}) { set(level, bb, s(Blocks.MOSS_BLOCK), p[0], 0, p[1]); set(level, bb, plant, p[0], 1, p[1]); }
		lectern(level, bb, 5, 1, 4);
		chest(level, bb, 11, 1, 3, MasterSites.supplies(site()), Direction.SOUTH, 1);
	}

	@Override public List<BoundingBox> wardedBoxes() { return List.of(); }
}
