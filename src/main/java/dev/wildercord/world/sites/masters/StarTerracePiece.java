package dev.wildercord.world.sites.masters;

import dev.wildercord.world.dungeons.DungeonWards;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

/**
 * A raised observatory terrace of the Starlit or Dawn school: a glowing star map in its floor, a copper telescope, and a domed
 * reading room sealed with arcane light.
 */
public final class StarTerracePiece extends MasterSitePiece {
	public StarTerracePiece(int x, int y, int z, Direction facing, boolean alt) { super(MasterSites.STAR_TERRACE, x, y, z, 17, 12, 18, facing, alt); }
	public StarTerracePiece(CompoundTag tag) { super(MasterSites.STAR_TERRACE, tag); }
	@Override public String site() { return "master_star_terrace"; }
	@Override public List<String> schools() { return List.of("starlit", "dawn"); }

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		DungeonWards.remember(level, this);
		BlockState base = alt ? s(Blocks.CALCITE) : s(Blocks.POLISHED_DEEPSLATE), wall = alt ? s(Blocks.QUARTZ_BRICKS) : s(Blocks.DEEPSLATE_TILES);
		BlockState glass = alt ? s(Blocks.STAINED_GLASS.orange()) : s(Blocks.STAINED_GLASS.purple()), glow = alt ? s(Blocks.GLOWSTONE) : s(Blocks.SEA_LANTERN);
		pad(level, bb, 0, 0, 16, 17, 11, s(Blocks.GRASS_BLOCK), s(Blocks.DIRT));
		var flower = alt ? s(Blocks.DANDELION) : s(Blocks.CORNFLOWER);
		for (int x = 0; x <= 16; x++) for (int z = 0; z <= 2; z++) if ((x < 6 || x > 10) && noise(x, z) < 40) set(level, bb, flower, x, 1, z);
		// The terrace, its steps and its star map: glass rays lit from beneath.
		fill(level, bb, 1, 0, 3, 15, 3, 16, base);
		var stair = s(Blocks.STONE_BRICK_STAIRS).setValue(StairBlock.FACING, Direction.NORTH);
		for (int x = 7; x <= 9; x++) {
			set(level, bb, stair, x, 1, 0);
			set(level, bb, base, x, 1, 1); set(level, bb, stair, x, 2, 1);
			fill(level, bb, x, 1, 2, x, 2, 2, base); set(level, bb, stair, x, 3, 2);
		}
		for (int x = 2; x <= 14; x++) for (int z = 4; z <= 11; z++) {
			double d = dist(x, z, 8, 8);
			if (d >= 2 && d <= 6 && onSpoke(x, z, 8, 8, 8, 0.5)) { set(level, bb, glass, x, 3, z); set(level, bb, glow, x, 2, z); }
		}
		for (int[] p : new int[][]{{1, 3}, {15, 3}, {1, 16}, {15, 16}}) {
			fill(level, bb, p[0], 4, p[1], p[0], 6, p[1], wall);
			set(level, bb, alt ? s(Blocks.LANTERN) : s(Blocks.END_ROD), p[0], 7, p[1]);
		}
		// The telescope.
		var copper = s(Blocks.COPPER_BLOCK.waxed().unaffected());
		fill(level, bb, 8, 4, 8, 8, 6, 8, wall);
		set(level, bb, copper, 8, 7, 8); set(level, bb, copper, 8, 8, 9); set(level, bb, copper, 8, 9, 10);
		// The reading room, sealed with arcane light.
		room(level, bb, 4, 3, 12, 12, 8, 16, wall);
		fill(level, bb, 6, 8, 13, 10, 8, 15, glass);
		set(level, bb, glow, 8, 8, 14);
		fill(level, bb, 5, 4, 13, 5, 5, 15, s(Blocks.BOOKSHELF)); fill(level, bb, 11, 4, 13, 11, 5, 15, s(Blocks.BOOKSHELF));
		var e = element(school());
		sealDoorAcross(level, bb, 12, 7, 9, 4, 6, e, e);
		chest(level, bb, 8, 4, 15, rewardLoot(), Direction.SOUTH, 2);
		// Practice and the lectern.
		var post = alt ? s(Blocks.CHERRY_FENCE) : s(Blocks.DARK_OAK_FENCE);
		dummy(level, bb, 3, 4, 5, post); dummy(level, bb, 13, 4, 5, post);
		target(level, bb, 3, 4, 10, post); target(level, bb, 13, 4, 10, post);
		lectern(level, bb, 6, 4, 5);
		chest(level, bb, 11, 4, 4, MasterSites.supplies(site()), Direction.SOUTH, 1);
	}

	@Override public List<BoundingBox> wardedBoxes() { return List.of(worldBox(4, 3, 12, 12, 8, 16)); }
}
