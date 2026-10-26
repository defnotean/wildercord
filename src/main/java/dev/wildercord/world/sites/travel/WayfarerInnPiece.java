package dev.wildercord.world.sites.travel;

import dev.wildercord.town.BountyBoardBlock;
import dev.wildercord.town.Town;
import dev.wildercord.town.WayfarerKeeper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A crossroads inn: two gravel roads meet at its door. Inside, a lit hearth, tables, three beds to sleep in, the
 * guest ledger on a lectern (leads to the other roadside sites) and the pantry chest. Notices hang beside the door,
 * over the bounty board; its keepers (a cook, a stablemaster and a Master's emissary) trade with travellers.
 */
public final class WayfarerInnPiece extends TravelPiece {
	public WayfarerInnPiece(int x, int y, int z, Direction facing) { super(TravelSites.INN_PIECE, x, y, z, 23, 17, 23, facing); }
	public WayfarerInnPiece(CompoundTag tag) { super(TravelSites.INN_PIECE, tag); }

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		clearCanopy(level, bb);
		BlockState planks = b(Blocks.SPRUCE_PLANKS), stone = b(Blocks.STONE_BRICKS), pane = b(Blocks.GLASS_PANE);
		BlockState upright = b(Blocks.STRIPPED_SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
		BlockState beamX = b(Blocks.DARK_OAK_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
		BlockState beamZ = b(Blocks.DARK_OAK_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
		Palette plinth = Palette.of(Blocks.STONE_BRICKS, 6, Blocks.MOSSY_STONE_BRICKS, 2, Blocks.COBBLESTONE, 2, Blocks.MOSSY_COBBLESTONE, 1);
		Palette plaster = Palette.of(Blocks.CALCITE, 1);
		Palette flue = Palette.of(Blocks.COBBLESTONE, 4, Blocks.STONE_BRICKS, 4, Blocks.MOSSY_COBBLESTONE, 2);
		// The crossroads: one road across the front, one up to a paved porch at the door.
		for (int x = 0; x <= 22; x++) for (int z = 1; z <= 3; z++) footing(level, bb, x, z, x, z, noise(x, z) < 25 ? b(Blocks.COARSE_DIRT) : b(Blocks.GRAVEL), DIRT, 3);
		for (int z = 0; z <= 3; z++) footing(level, bb, 10, z, 12, z, z == 0 || noise(11, z) < 25 ? b(Blocks.COARSE_DIRT) : b(Blocks.GRAVEL), DIRT, 3);
		footing(level, bb, 8, 4, 14, 5, stone, COBBLE, 9);
		fill(level, bb, 9, 0, 4, 13, 0, 5, Palette.of(Blocks.STONE_BRICKS, 5, Blocks.CRACKED_STONE_BRICKS, 1, Blocks.MOSSY_STONE_BRICKS, 1));
		// A yard on each side: a hitching rail and hay by the road, a woodpile under the west gable.
		footing(level, bb, 15, 4, 21, 5, GRASS, DIRT, 3);
		footing(level, bb, 0, 7, 3, 17, GRASS, DIRT, 4);
		for (int x : new int[]{18, 21}) set(level, bb, b(Blocks.SPRUCE_FENCE), x, 1, 4);
		fill(level, bb, 18, 2, 4, 21, 2, 4, b(Blocks.STRIPPED_SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		for (int[] h : new int[][]{{20, 1}, {21, 1}, {21, 2}}) set(level, bb, b(Blocks.HAY_BLOCK), h[0], h[1], 5);
		set(level, bb, b(Blocks.BARREL), 19, 1, 5);
		fill(level, bb, 1, 1, 12, 2, 2, 14, b(Blocks.SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
		set(level, bb, b(Blocks.SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z), 1, 3, 13);
		// The house: a stone plinth course under a dark timber frame infilled with pale plaster.
		footing(level, bb, 4, 6, 18, 18, stone, COBBLE, 14);
		room(level, bb, 4, 0, 6, 18, 5, 18, planks);
		walls(level, bb, 4, 0, 6, 18, 1, 18, plinth);
		walls(level, bb, 4, 2, 6, 18, 4, 18, plaster);
		fill(level, bb, 5, 0, 7, 17, 0, 17, planks);
		for (int x : new int[]{4, 8, 14, 18}) { fill(level, bb, x, 2, 6, x, 4, 6, upright); fill(level, bb, x, 2, 18, x, 4, 18, upright); }
		fill(level, bb, 4, 2, 12, 4, 4, 12, upright); fill(level, bb, 18, 2, 12, 18, 4, 12, upright);
		// The doorway framed by posts under a dark lintel.
		for (int x : new int[]{10, 12}) fill(level, bb, x, 1, 6, x, 3, 6, upright);
		fill(level, bb, 10, 4, 6, 12, 4, 6, beamX);
		door(level, bb, Blocks.SPRUCE_DOOR, 11, 1, 6, Direction.NORTH);
		// Windows in every bay, each with a sill below and a hood above; flowers on the front sills.
		for (int x : new int[]{6, 16}) {
			fill(level, bb, x, 2, 6, x, 3, 6, pane); fill(level, bb, x, 2, 18, x, 3, 18, pane);
			for (int y : new int[]{1, 4}) {
				set(level, bb, stair(Blocks.DARK_OAK_STAIRS, Direction.NORTH, true), x, y, 5);
				set(level, bb, stair(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, true), x, y, 19);
			}
		}
		set(level, bb, b(Blocks.POTTED_POPPY), 6, 2, 5);
		set(level, bb, b(Blocks.POTTED_DANDELION), 16, 2, 5);
		for (int z : new int[]{9, 15}) {
			fill(level, bb, 4, 2, z, 4, 3, z, pane); fill(level, bb, 18, 2, z, 18, 3, z, pane);
			for (int y : new int[]{1, 4}) {
				set(level, bb, stair(Blocks.DARK_OAK_STAIRS, Direction.EAST, true), 3, y, z);
				set(level, bb, stair(Blocks.DARK_OAK_STAIRS, Direction.WEST, true), 19, y, z);
			}
		}
		for (int x : new int[]{5, 17}) sconce(level, bb, x, 3, 5, Direction.SOUTH, false);
		// A cross gable over the porch on two posts, hung with the Road Wardens' colours and a lantern. It is built
		// before the main roof so the main slope cuts it off where the two meet.
		for (int x : new int[]{8, 14}) post(level, bb, upright, x, 4, 4);
		gableRoof(level, bb, 9, 4, 13, 7, 6, false, Blocks.DARK_OAK_STAIRS, b(Blocks.DARK_OAK_PLANKS), plaster);
		fill(level, bb, 9, 5, 4, 13, 5, 4, beamX);
		for (int x : new int[]{9, 13}) set(level, bb, upright, x, 6, 4);
		set(level, bb, pane, 11, 7, 4);
		hangingLantern(level, bb, 11, 4, 4, 0, false);
		for (int x : new int[]{8, 14}) banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.BLUE), x, 3, 3, Direction.SOUTH);
		set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.NORTH, false), 13, 1, 4);
		// A steep dark roof with plaster gables, each with a king post and two small windows.
		int ridge = gableRoof(level, bb, 4, 6, 18, 18, 6, true, Blocks.DARK_OAK_STAIRS, b(Blocks.DARK_OAK_PLANKS), plaster);
		for (int x : new int[]{4, 18}) {
			fill(level, bb, x, 6, 12, x, ridge - 1, 12, upright);
			for (int z : new int[]{10, 14}) set(level, bb, pane, x, 8, z);
		}
		// The wall plate all round and the ceiling under it, restored where the eaves overlapped them.
		fill(level, bb, 4, 5, 6, 18, 5, 6, beamX); fill(level, bb, 4, 5, 18, 18, 5, 18, beamX);
		fill(level, bb, 4, 5, 7, 4, 5, 17, beamZ); fill(level, bb, 18, 5, 7, 18, 5, 17, beamZ);
		fill(level, bb, 5, 5, 7, 17, 5, 17, planks);
		// The hearth on the back wall under a stone mantel; its chimney breast rises through the roof above the ridge.
		fill(level, bb, 9, 1, 17, 13, 3, 17, flue);
		fill(level, bb, 10, 1, 18, 12, 7, 18, flue);
		fill(level, bb, 11, 8, 18, 11, ridge + 1, 18, flue);
		set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, false), 10, 8, 18);
		set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, false), 12, 8, 18);
		set(level, bb, b(Blocks.STONE_BRICK_SLAB), 11, ridge + 2, 18);
		set(level, bb, b(Blocks.CAMPFIRE).setValue(CampfireBlock.LIT, true).setValue(CampfireBlock.FACING, Direction.SOUTH), 11, 1, 16);
		set(level, bb, AIR, 11, 1, 17);
		fill(level, bb, 10, 1, 16, 10, 1, 16, b(Blocks.COBBLESTONE_WALL));
		fill(level, bb, 12, 1, 16, 12, 1, 16, b(Blocks.COBBLESTONE_WALL));
		for (int x = 9; x <= 13; x++) set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, true), x, 3, 16);
		for (int x : new int[]{9, 13}) set(level, bb, b(Blocks.CANDLE).setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true), x, 4, 16);
		// Dark beams across the ceiling with lanterns under them, and a runner from the door.
		for (int z : new int[]{9, 12, 15}) fill(level, bb, 5, 5, z, 17, 5, z, beamX);
		fill(level, bb, 11, 1, 7, 11, 1, 10, b(Blocks.CARPET.pick(DyeColor.RED)));
		fill(level, bb, 10, 1, 14, 12, 1, 14, b(Blocks.CARPET.pick(DyeColor.BROWN)));
		for (int z : new int[]{9, 15}) hangingLantern(level, bb, 11, 4, z, 0, false);
		// Tables and benches, barrels along the west wall, beds along the east wall.
		for (int[] t : new int[][]{{7, 10}, {7, 14}, {11, 12}}) {
			set(level, bb, b(Blocks.SPRUCE_FENCE), t[0], 1, t[1]);
			set(level, bb, b(Blocks.SPRUCE_PRESSURE_PLATE), t[0], 2, t[1]);
			set(level, bb, b(Blocks.SPRUCE_STAIRS).setValue(StairBlock.FACING, Direction.EAST), t[0] - 1, 1, t[1]);
			set(level, bb, b(Blocks.SPRUCE_STAIRS).setValue(StairBlock.FACING, Direction.WEST), t[0] + 1, 1, t[1]);
		}
		for (int z = 8; z <= 16; z += 2) set(level, bb, b(Blocks.BARREL), 5, 1, z);
		for (int z : new int[]{10, 14}) set(level, bb, b(Blocks.BARREL), 5, 2, z);
		Block[] beds = {Blocks.BED.pick(DyeColor.RED), Blocks.BED.pick(DyeColor.GREEN), Blocks.BED.pick(DyeColor.BLUE)};
		for (int i = 0; i < 3; i++) bed(level, bb, beds[(i + variant()) % 3], 16, 1, 8 + i * 3, Direction.EAST);
		for (int[] l : new int[][]{{8, 12}, {14, 12}}) set(level, bb, b(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), l[0], 4, l[1]);
		lectern(level, bb, 8, 1, 8, Direction.SOUTH, TravelSites.book("travel_ledger", "Guest Ledger", "The Innkeeper"));
		chest(level, bb, 16, 1, 16, TravelSites.loot(TravelSites.INN), Direction.WEST, 1);
		// The notice board outside: notes and a blank map for the road.
		frame(level, bb, 9, 2, 5, Direction.SOUTH, new ItemStack(Items.PAPER));
		frame(level, bb, 13, 2, 5, Direction.SOUTH, new ItemStack(Items.MAP));
		resident(level, bb, EntityTypes.CAT, 9, 1, 13, 6);
		// 0.12: the bounty board under the notices, and the inn's keepers: the cook by the hearth, the Master's emissary
		// at the tables, the stablemaster out on the road.
		set(level, bb, Town.BOUNTY_BOARD.defaultBlockState().setValue(BountyBoardBlock.FACING, Direction.SOUTH), 9, 1, 5);
		resident(level, bb, Town.KEEPER, 13, 1, 15, 3, keeper -> keeper.setRole(WayfarerKeeper.Role.COOK));
		resident(level, bb, Town.KEEPER, 13, 1, 9, 4, keeper -> keeper.setRole(WayfarerKeeper.Role.EMISSARY));
		resident(level, bb, Town.KEEPER, 16, 1, 2, 5, keeper -> keeper.setRole(WayfarerKeeper.Role.STABLEMASTER));
	}

	/**
	 * Brings an inn generated before 0.12 up to date (0.13): when its bounty board is missing, the board is put up under the
	 * notices and the three keepers move in. Inns that already have a board are left alone.
	 */
	public void retrofit(ServerLevel level) {
		BlockPos board = getWorldPos(9, 1, 5).immutable();
		if (!level.isLoaded(board) || !level.getBlockState(board).isAir()) return;
		level.setBlockAndUpdate(board, Town.BOUNTY_BOARD.defaultBlockState().setValue(BountyBoardBlock.FACING, world(Direction.SOUTH)));
		keeper(level, 13, 1, 15, 3, WayfarerKeeper.Role.COOK);
		keeper(level, 13, 1, 9, 4, WayfarerKeeper.Role.EMISSARY);
		keeper(level, 16, 1, 2, 5, WayfarerKeeper.Role.STABLEMASTER);
	}

	private void keeper(ServerLevel level, int x, int y, int z, int radius, WayfarerKeeper.Role role) {
		BlockPos at = getWorldPos(x, y, z).immutable();
		WayfarerKeeper keeper = Town.KEEPER.create(level, EntitySpawnReason.STRUCTURE);
		if (keeper == null) return;
		keeper.setRole(role);
		keeper.setPersistenceRequired();
		keeper.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		keeper.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.STRUCTURE, null);
		keeper.setHomeTo(at, radius);
		level.addFreshEntity(keeper);
	}
}
