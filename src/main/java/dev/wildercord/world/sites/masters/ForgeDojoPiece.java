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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

/** A badlands forge dojo of the Ember or Crimson school: a brick hall, a smoking chimney and a sealed vault. */
public final class ForgeDojoPiece extends MasterSitePiece {
	public ForgeDojoPiece(int x, int y, int z, Direction facing, boolean alt) { super(MasterSites.FORGE_DOJO, x, y, z, 17, 14, 19, facing, alt); }
	public ForgeDojoPiece(CompoundTag tag) { super(MasterSites.FORGE_DOJO, tag); }
	@Override public String site() { return "master_forge_dojo"; }
	@Override public List<String> schools() { return List.of("ember", "crimson"); }

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		DungeonWards.remember(level, this);
		BlockState wall = alt ? s(Blocks.RED_NETHER_BRICKS) : s(Blocks.BRICKS), dark = s(Blocks.POLISHED_BLACKSTONE_BRICKS);
		pad(level, bb, 0, 0, 16, 18, 13, s(Blocks.POLISHED_BLACKSTONE), s(Blocks.BLACKSTONE));
		room(level, bb, 1, 0, 2, 15, 7, 12, wall);
		fill(level, bb, 1, 0, 2, 15, 0, 12, s(Blocks.POLISHED_BLACKSTONE));
		for (int x = 1; x <= 15; x += 2) { set(level, bb, dark, x, 8, 2); set(level, bb, dark, x, 8, 12); }
		for (int z = 4; z <= 10; z += 2) { set(level, bb, dark, 1, 8, z); set(level, bb, dark, 15, 8, z); }
		fill(level, bb, 7, 1, 2, 9, 3, 2, AIR);
		fill(level, bb, 6, 4, 2, 10, 4, 2, dark);
		for (int z : new int[]{5, 8}) { fill(level, bb, 1, 3, z, 1, 4, z, s(Blocks.IRON_BARS)); fill(level, bb, 15, 3, z, 15, 4, z, s(Blocks.IRON_BARS)); }
		set(level, bb, s(Blocks.SHROOMLIGHT), 5, 7, 7); set(level, bb, s(Blocks.SHROOMLIGHT), 11, 7, 7);
		fill(level, bb, 8, 1, 3, 8, 1, 11, alt ? s(Blocks.CARPET.red()) : s(Blocks.CARPET.orange()));
		// The forge chimney: a lit hearth behind bars, its smoke rising through a hollow stack above the roof.
		fill(level, bb, 12, 1, 9, 14, 13, 11, dark);
		fill(level, bb, 13, 2, 10, 13, 13, 10, AIR);
		set(level, bb, s(Blocks.MAGMA_BLOCK), 13, 0, 10);
		set(level, bb, s(Blocks.CAMPFIRE), 13, 1, 10);
		fill(level, bb, 13, 1, 9, 13, 2, 9, s(Blocks.IRON_BARS));
		set(level, bb, s(Blocks.ANVIL), 11, 1, 10);
		set(level, bb, s(Blocks.SMITHING_TABLE), 2, 1, 3);
		// Practice: four dummies and two targets down the hall.
		var post = s(Blocks.DARK_OAK_FENCE);
		dummy(level, bb, 4, 1, 5, post); dummy(level, bb, 4, 1, 8, post); dummy(level, bb, 12, 1, 5, post);
		target(level, bb, 3, 1, 11, post); target(level, bb, 5, 1, 11, post);
		lectern(level, bb, 6, 1, 4);
		chest(level, bb, 14, 1, 3, MasterSites.supplies(site()), Direction.WEST, 1);
		// The vault behind the back wall, sealed with the school's element.
		room(level, bb, 5, 0, 12, 11, 5, 17, dark);
		fill(level, bb, 6, 0, 13, 10, 0, 16, s(Blocks.POLISHED_BLACKSTONE));
		var e = element(school());
		sealDoorAcross(level, bb, 12, 7, 9, 1, 3, e, e);
		set(level, bb, s(Blocks.LANTERN), 6, 1, 16); set(level, bb, s(Blocks.LANTERN), 10, 1, 16);
		set(level, bb, alt ? s(Blocks.CRYING_OBSIDIAN) : s(Blocks.GILDED_BLACKSTONE), 8, 0, 16);
		chest(level, bb, 8, 1, 15, rewardLoot(), Direction.SOUTH, 2);
	}

	@Override public List<BoundingBox> wardedBoxes() { return List.of(worldBox(5, 0, 12, 11, 5, 17)); }
}
