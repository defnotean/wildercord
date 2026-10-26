package dev.wildercord.world.sites.belowkeeper;

import dev.wildercord.wildlife.FungalGarden;
import dev.wildercord.wildlife.GlowcapBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Belowkeeper Listening Station: a vaulted deepslate-and-copper acoustic station in the Belowkeeper Hollows.
 * Holds a damp moss-and-clay Glowcap basin along the west wall, an acoustic resonance shutter lamp and lever,
 * a lectern with What the Empty Bell Kept, a supply chest, and a persistent Belowkeeper Listener resident.
 */
public final class ListeningStationPiece extends BelowkeeperPiece {
	public static final int DECK = 2;
	public static final int LAMP_X = 16, LAMP_Y = DECK + 3, LAMP_Z = 5;
	public static final int LEVER_X = 16, LEVER_Y = DECK + 3, LEVER_Z = 6;
	private boolean residentPlaced;

	public ListeningStationPiece(int x, int y, int z, Direction facing) {
		super(BelowkeeperSites.STATION_PIECE, x, y, z, 21, 10, 21, facing);
	}
	public ListeningStationPiece(CompoundTag tag) {
		super(BelowkeeperSites.STATION_PIECE, tag);
		residentPlaced = tag.getBooleanOr("ListenerPlaced", false);
	}

	@Override
	protected void addAdditionalSaveData(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext ctx, CompoundTag tag) {
		super.addAdditionalSaveData(ctx, tag);
		tag.putBoolean("ListenerPlaced", residentPlaced);
	}

	public BlockPos lampPos() { return world(LAMP_X, LAMP_Y, LAMP_Z); }
	public BlockPos leverPos() { return world(LEVER_X, LEVER_Y, LEVER_Z); }

	private static boolean furniture(int x, int y, int z) {
		return (y == DECK + 1 && ((x == 15 && z == 9) || (x == 16 && z == 13)))
			|| (x == LAMP_X && y == LAMP_Y && z == LAMP_Z)
			|| (x == LEVER_X && y == LEVER_Y && z == LEVER_Z);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox clip, ChunkPos chunk, BlockPos pivot) {
		// Clear interior while preserving stateful furniture across chunk boundary replays.
		for (int x = 1; x <= 19; x++) for (int y = DECK + 1; y <= 8; y++) for (int z = 1; z <= 19; z++) {
			if (!furniture(x, y, z)) set(level, clip, AIR, x, y, z);
		}
		// Solid deepslate footing and chamber floor.
		footing(level, clip, 0, 0, 20, 20, DECK - 1);
		for (int x = 0; x <= 20; x++) for (int z = 0; z <= 20; z++) {
			int n = noise(x, DECK, z);
			BlockState floor = n < 20 ? MOSSY_BRICK : n < 55 ? POLISHED : BRICK;
			set(level, clip, floor, x, DECK, z);
		}
		// Vaulted deepslate-and-copper outer walls and roof (y = DECK + 1 .. 9) so external water or gravel never intrudes.
		for (int x = 0; x <= 20; x++) for (int z = 0; z <= 20; z++) {
			boolean border = x == 0 || x == 20 || z == 0 || z == 20;
			if (border) {
				for (int y = DECK + 1; y <= 8; y++) {
					int n = noise(x, y, z);
					BlockState wall = y == DECK + 4 ? CHISELED : (n < 25 ? CRACKED_BRICK : BRICK);
					set(level, clip, wall, x, y, z);
				}
			}
			BlockState roof = (x % 4 == 0 || z % 4 == 0) ? CHISELED : (Math.abs(x - 10) <= 2 && Math.abs(z - 10) <= 2 ? COPPER_GRATE : TILE);
			set(level, clip, roof, x, 9, z);
		}
		// Ribbed dark-oak and copper acoustic arches at z = 4, 10, 16.
		for (int z : new int[]{4, 10, 16}) {
			for (int y = DECK + 1; y <= 8; y++) {
				set(level, clip, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), 1, y, z);
				set(level, clip, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), 19, y, z);
			}
			for (int x = 2; x <= 18; x++) {
				set(level, clip, x % 3 == 0 ? OXIDIZED_COPPER : TILE, x, 8, z);
			}
		}
		// North and south tunnel thresholds framed in chiseled deepslate.
		for (int z : new int[]{0, 20}) {
			for (int x = 9; x <= 11; x++) for (int y = DECK + 1; y <= DECK + 3; y++) {
				set(level, clip, AIR, x, y, z);
			}
			set(level, clip, CHISELED, 8, DECK + 3, z);
			set(level, clip, CHISELED, 12, DECK + 3, z);
		}
		// Haunches under each arch beam, so the ribs spring from the posts in a curve instead of a corner.
		for (int z : new int[]{4, 10, 16}) {
			set(level, clip, st(Blocks.DARK_OAK_STAIRS, Direction.WEST, true), 2, 7, z);
			set(level, clip, st(Blocks.DARK_OAK_STAIRS, Direction.EAST, true), 18, 7, z);
			set(level, clip, CHISELED, 10, 8, z);
		}
		// A tile ledge round the walls at the chiseled course, and a corbel table under the vault.
		for (int i = 1; i <= 19; i++) {
			set(level, clip, ledge(Blocks.DEEPSLATE_TILE_SLAB), i, DECK + 4, 1);
			set(level, clip, ledge(Blocks.DEEPSLATE_TILE_SLAB), i, DECK + 4, 19);
			set(level, clip, st(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.SOUTH, true), i, 8, 1);
			set(level, clip, st(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.NORTH, true), i, 8, 19);
			if (i % 6 == 4) continue;
			set(level, clip, ledge(Blocks.DEEPSLATE_TILE_SLAB), 1, DECK + 4, i);
			set(level, clip, ledge(Blocks.DEEPSLATE_TILE_SLAB), 19, DECK + 4, i);
			set(level, clip, st(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.WEST, true), 1, 8, i);
			set(level, clip, st(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.EAST, true), 19, 8, i);
		}
		for (int x : new int[]{1, 19}) for (int z : new int[]{1, 19}) set(level, clip, CHISELED, x, 8, z);
		// Polished pilasters halfway between the arches on both long walls.
		for (int x : new int[]{1, 19}) for (int z : new int[]{7, 13}) {
			fill(level, clip, x, DECK + 1, z, x, 8, z, POLISHED);
			set(level, clip, CHISELED, x, DECK + 1, z);
			set(level, clip, CHISELED, x, 8, z);
		}
		// Each tunnel mouth gets a portal inside: polished jambs, a brick lintel with a chiseled keystone and corbelled shoulders.
		for (int[] t : new int[][]{{1, 0}, {19, 0}}) {
			int z = t[0];
			for (int x : new int[]{8, 12}) fill(level, clip, x, DECK + 1, z, x, DECK + 3, z, POLISHED);
			fill(level, clip, 8, DECK + 4, z, 12, DECK + 4, z, BRICK);
			set(level, clip, CHISELED, 10, DECK + 4, z);
			set(level, clip, st(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.WEST, true), 9, DECK + 3, z);
			set(level, clip, st(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.EAST, true), 11, DECK + 3, z);
			set(level, clip, Blocks.SOUL_LANTERN.defaultBlockState(), 8, DECK + 5, z);
			set(level, clip, Blocks.SOUL_LANTERN.defaultBlockState(), 12, DECK + 5, z);
		}

		// Damp Belowkeeper Glowcap basin along the west side (x = 3..6, z = 6..13), watered by a lined gutter.
		for (int z = 6; z <= 13; z++) {
			set(level, clip, BRICK, 2, DECK - 1, z);
			set(level, clip, BRICK, 3, DECK - 1, z);
			set(level, clip, z >= 7 && z <= 12 ? Blocks.WATER.defaultBlockState() : OXIDIZED_COPPER, 3, DECK, z);
			set(level, clip, (z & 1) == 0 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.CLAY.defaultBlockState(), 4, DECK, z);
			set(level, clip, Blocks.MOSS_BLOCK.defaultBlockState(), 5, DECK, z);
			set(level, clip, OXIDIZED_COPPER, 6, DECK, z);
		}
		for (int z : new int[]{7, 9, 11, 12}) {
			set(level, clip, FungalGarden.GLOWCAP.defaultBlockState().setValue(GlowcapBlock.AGE, z == 9 ? 1 : 2), 4, DECK + 1, z);
		}
		for (int z : new int[]{8, 10}) {
			set(level, clip, FungalGarden.GLOWCAP.defaultBlockState().setValue(GlowcapBlock.AGE, 2), 5, DECK + 1, z);
		}
		set(level, clip, FungalGarden.NURSERY.defaultBlockState(), 4, DECK + 3, 9);

		// Acoustic Listening Desk, cyan bed, workbenches, lectern, and supply chest on the east side.
		fill(level, clip, 12, DECK, 6, 18, DECK, 14, DARK_OAK);
		bed(level, clip, Blocks.BED.cyan(), 17, DECK + 1, 8, Direction.NORTH);
		set(level, clip, Blocks.CARTOGRAPHY_TABLE.defaultBlockState(), 17, DECK + 1, 11);
		set(level, clip, Blocks.CRAFTING_TABLE.defaultBlockState(), 17, DECK + 1, 12);
		lectern(level, clip, 15, DECK + 1, 9, Direction.WEST,
			BelowkeeperSites.book("belowkeeper_station", "What the Empty Bell Kept", "Mara, Belowkeeper"));
		chest(level, clip, 16, DECK + 1, 13, Direction.WEST, BelowkeeperSites.loot(BelowkeeperSites.LISTENING_STATION), 31L);
		set(level, clip, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 10, 8, 7);
		set(level, clip, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 10, 8, 13);
		set(level, clip, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 15, 7, 10);
		// Lamps under the end arches, kept far enough from the basin that the Glowcaps stay in the dark.
		for (int x : new int[]{6, 14}) for (int z : new int[]{4, 16}) {
			set(level, clip, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), x, 7, z);
		}
		// The listener's corner: a rug by the desk, shelves of records on the east wall, stores by the bed.
		fill(level, clip, 13, DECK + 1, 10, 14, DECK + 1, 12, Blocks.CARPET.pick(DyeColor.CYAN).defaultBlockState());
		for (int z : new int[]{11, 12, 14, 15}) fill(level, clip, 19, DECK + 1, z, 19, DECK + 2, z, Blocks.BOOKSHELF.defaultBlockState());
		set(level, clip, Blocks.BARREL.defaultBlockState(), 18, DECK + 1, 6);
		set(level, clip, Blocks.BARREL.defaultBlockState(), 18, DECK + 2, 6);
		set(level, clip, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2).setValue(CandleBlock.LIT, true), 19, DECK + 3, 12);

		// Acoustic Resonance Shutter pillar: copper-and-deepslate column with shuttered Redstone Lamp and Lever.
		for (int y = DECK + 1; y <= LAMP_Y - 1; y++) {
			set(level, clip, y == DECK + 1 ? COPPER : CHISELED, LAMP_X, y, LAMP_Z);
		}
		if (!level.getBlockState(lampPos()).is(Blocks.REDSTONE_LAMP)) {
			set(level, clip, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, false), LAMP_X, LAMP_Y, LAMP_Z);
		}
		set(level, clip, OXIDIZED_COPPER, LAMP_X, LAMP_Y + 1, LAMP_Z);
		if (!level.getBlockState(leverPos()).is(Blocks.LEVER)) {
			set(level, clip, Blocks.LEVER.defaultBlockState()
				.setValue(LeverBlock.FACE, AttachFace.WALL)
				.setValue(LeverBlock.FACING, Direction.SOUTH)
				.setValue(LeverBlock.POWERED, false), LEVER_X, LEVER_Y, LEVER_Z);
		}

		// A copper console behind the lever, so it has a face to hang on.
		set(level, clip, COPPER, LEVER_X, DECK + 1, LEVER_Z + 1);
		set(level, clip, CHISELED, LEVER_X, DECK + 2, LEVER_Z + 1);
		set(level, clip, POLISHED, LEVER_X, LEVER_Y, LEVER_Z + 1);
		set(level, clip, Blocks.POLISHED_DEEPSLATE_SLAB.defaultBlockState(), LEVER_X, LEVER_Y + 1, LEVER_Z + 1);

		var home = localPosition(10, DECK + 1, 10);
		if (!residentPlaced && clip.isInside(home)) residentPlaced = BelowkeeperResident.spawn(level, home);
	}

	private static BlockState st(Block stairs, Direction facing, boolean top) {
		return stairs.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
	}

	private static BlockState ledge(Block slab) {
		return slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
	}
}
