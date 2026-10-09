package dev.wildercord.world.sites.water;

import dev.wildercord.world.dungeons.DungeonPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * What the water sites share: probes of the water's surface and the sea floor for their locators, and the careful
 * placing water asks for. A block that can hold water is waterlogged exactly when it replaces water (so nothing
 * leaves a dry hole in a lake, and nothing drips onto a deck); foundations reach down through water to the first
 * solid block of their own column, so every chunk builds its part the same way.
 */
abstract class WaterPiece extends DungeonPiece {
	protected WaterPiece(StructurePieceType type, int x, int y, int z, int width, int height, int depth, Direction facing) {
		super(type, x, y, z, width, height, depth, facing);
	}

	protected WaterPiece(StructurePieceType type, CompoundTag tag) {
		super(type, tag);
	}

	/** Peaceful places: nothing is warded. */
	@Override
	public List<BoundingBox> wardedBoxes() {
		return List.of();
	}

	// ------------------------------------------------------------------ locating

	/** The first free y over a column, water counted: one over the water's surface where there is water. */
	static int surface(Structure.GenerationContext c, BlockPos at) {
		return c.chunkGenerator().getFirstFreeHeight(at.getX(), at.getZ(), Heightmap.Types.WORLD_SURFACE_WG, c.heightAccessor(), c.randomState());
	}

	/** The first free y over a column's solid ground, water not counted. */
	static int floor(Structure.GenerationContext c, BlockPos at) {
		return c.chunkGenerator().getFirstFreeHeight(at.getX(), at.getZ(), Heightmap.Types.OCEAN_FLOOR_WG, c.heightAccessor(), c.randomState());
	}

	/** How deep the water over a column is (0 on dry land). */
	static int depth(Structure.GenerationContext c, BlockPos at) {
		return surface(c, at) - floor(c, at);
	}

	/** Whether a column is open water at sea level, at least {@code min} deep and at most {@code max}. */
	static boolean water(Structure.GenerationContext c, BlockPos at, int min, int max) {
		int d = depth(c, at);
		return surface(c, at) == c.chunkGenerator().getSeaLevel() && d >= min && d <= max;
	}

	/** Whether a column is dry land whose top block lies between {@code lo} and {@code hi}. */
	static boolean land(Structure.GenerationContext c, BlockPos at, int lo, int hi) {
		int top = surface(c, at) - 1;
		return depth(c, at) == 0 && top >= lo && top <= hi;
	}

	/** The four facings, starting with a random one, for locators that try each way round. */
	static List<Direction> facings(Structure.GenerationContext c) {
		Direction first = Direction.Plane.HORIZONTAL.getRandomDirection(c.random());
		Direction[] all = new Direction[4];
		for (int i = 0; i < 4; i++) {
			all[i] = Direction.from2DDataValue(first.get2DDataValue() + i);
		}
		return Arrays.asList(all);
	}

	/** Origins across a chunk (offsets from its corner), for locators that look for their spot in more than one place. */
	static final int[][] OFFSETS = {{0, 0}, {6, 0}, {12, 0}, {0, 6}, {6, 6}, {12, 6}, {0, 12}, {6, 12}, {12, 12}};

	static Optional<Structure.GenerationStub> stub(DungeonPiece piece, BlockPos at) {
		return Optional.of(new Structure.GenerationStub(at, builder -> builder.addPiece(piece)));
	}

	// ------------------------------------------------------------------ placing

	/** Whether there's water (a source or a waterlogged block) at a spot now. */
	protected boolean wetAt(WorldGenLevel level, BoundingBox bb, int x, int y, int z) {
		return getBlock(level, x, y, z, bb).getFluidState().is(FluidTags.WATER);
	}

	/** Sets a block, waterlogged if it can be and there's water where it goes, dry otherwise. */
	protected void wet(WorldGenLevel level, BoundingBox bb, BlockState state, int x, int y, int z) {
		if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
			state = state.setValue(BlockStateProperties.WATERLOGGED, wetAt(level, bb, x, y, z));
		}
		set(level, bb, state, x, y, z);
	}

	/** Sets a waterloggable block that always stands in water (a coral fan in a pool, a conduit). */
	protected void soaked(WorldGenLevel level, BoundingBox bb, BlockState state, int x, int y, int z) {
		set(level, bb, state.setValue(BlockStateProperties.WATERLOGGED, true), x, y, z);
	}

	/** Air over a deck or floor, but water stays water: clearing never empties a lake. */
	protected void clear(WorldGenLevel level, BoundingBox bb, int x0, int y0, int z0, int x1, int y1, int z1) {
		for (int x = x0; x <= x1; x++) {
			for (int y = y0; y <= y1; y++) {
				for (int z = z0; z <= z1; z++) {
					BlockState there = getBlock(level, x, y, z, bb);
					if (!there.isAir() && there.getFluidState().isEmpty()) {
						set(level, bb, AIR, x, y, z);
					}
				}
			}
		}
	}

	/** A post from {@code top} down through air and water to the first solid block of its column (or the box's floor). */
	protected void post(WorldGenLevel level, BoundingBox bb, BlockState state, int x, int z, int top) {
		for (int y = top; y >= 0; y--) {
			BlockState there = getBlock(level, x, y, z, bb);
			if (!there.isAir() && there.getFluidState().isEmpty() && y < top) {
				break;
			}
			set(level, bb, state, x, y, z);
		}
	}

	/** Posts under every column of a rectangle. */
	protected void footing(WorldGenLevel level, BoundingBox bb, BlockState state, int x0, int z0, int x1, int z1, int top) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				post(level, bb, state, x, z, top);
			}
		}
	}

	/** A stair whose high side faces {@code facing} (in the piece's frame), so it climbs that way. */
	protected static BlockState stair(net.minecraft.world.level.block.Block block, Direction facing) {
		return block.defaultBlockState().setValue(StairBlock.FACING, facing);
	}

	/**
	 * A gabled roof running along z from {@code z0} to {@code z1}: stairs climbing in from {@code x0} and {@code x1},
	 * a ridge of {@code ridge} where they meet, its gable ends (the walls at {@code e0} and {@code e1}) filled with {@code end}.
	 */
	protected void gable(WorldGenLevel level, BoundingBox bb, int x0, int x1, int z0, int z1, int y, net.minecraft.world.level.block.Block stairs,
			BlockState ridge, BlockState end, int e0, int e1) {
		for (int k = 0; x0 + k <= x1 - k; k++) {
			for (int z = z0; z <= z1; z++) {
				if (x0 + k == x1 - k) {
					set(level, bb, ridge, x0 + k, y + k, z);
				} else {
					set(level, bb, stair(stairs, Direction.EAST), x0 + k, y + k, z);
					set(level, bb, stair(stairs, Direction.WEST), x1 - k, y + k, z);
				}
			}
			for (int x = x0 + k + 1; x < x1 - k; x++) {
				set(level, bb, end, x, y + k, e0);
				set(level, bb, end, x, y + k, e1);
			}
		}
	}

	/** A lectern facing {@code facing} (in the piece's frame), holding a written book of translated pages. */
	protected void lectern(WorldGenLevel level, BoundingBox bb, int x, int y, int z, Direction facing, String title, String key, int pages) {
		BlockPos pos = getWorldPos(x, y, z);
		if (!bb.isInside(pos)) {
			return;
		}
		set(level, bb, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, facing).setValue(LecternBlock.HAS_BOOK, true), x, y, z);
		if (level.getBlockEntity(pos) instanceof LecternBlockEntity lectern) {
			lectern.setBook(book(title, key, pages));
		}
	}

	/** A written book whose pages are {@code key.1} to {@code key.pages}, translated when read. */
	static ItemStack book(String title, String key, int pages) {
		List<Filterable<Component>> text = new java.util.ArrayList<>();
		for (int i = 1; i <= pages; i++) {
			text.add(Filterable.passThrough(Component.translatable(key + "." + i)));
		}
		ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
		stack.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough(title), "?", 0, text, true));
		return stack;
	}

	/** Marks the entities a water site placed itself, so they can be told apart from wanderers. */
	static final String RESIDENT = "wildercord.site_resident";

	/** An entity that isn't a guard (a boat at its mooring, a villager at work), placed once: by the chunk it's in. */
	protected void spawn(WorldGenLevel level, BoundingBox bb, EntityType<?> type, int x, int y, int z, double lift) {
		BlockPos pos = getWorldPos(x, y, z);
		if (!bb.isInside(pos)) {
			return;
		}
		Entity entity = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (entity == null) {
			return;
		}
		entity.snapTo(pos.getX() + 0.5, pos.getY() + lift, pos.getZ() + 0.5, 0, 0);
		entity.addTag(RESIDENT);
		if (entity instanceof net.minecraft.world.entity.Mob mob) {
			mob.setPersistenceRequired();
			mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
		}
		level.addFreshEntityWithPassengers(entity);
	}
}
