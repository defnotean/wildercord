package dev.wildercord.travel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Where a player can safely land, as plain Java over a grid of cells (no Minecraft types, so the
 * rules are unit-tested). The world side ({@link Landing}) says what each block is; this decides
 * whether a column is somewhere to stand and finds the nearest such spot to a saved one.
 *
 * <p>A spot is named by the block the player's feet are in. It's safe when the block below holds
 * them up, their feet and head are in open air (or, feet only, in shallow water), and nothing
 * harmful is under or around them. A strict search (a random teleport) also refuses water and
 * leaves, so nobody lands in a lake or on top of a tree.</p>
 */
public final class SafeSpots {
	private SafeSpots() {}

	/** What a block is, as far as landing goes. */
	public enum Cell {
		/** Nothing to bump into or be hurt by: air, grass, flowers, torches, a thin snow layer. */
		OPEN,
		/** Water (with nothing solid in it). */
		WATER,
		/** Something solid to stand on (anything with a collision shape that isn't below). */
		FLOOR,
		/** Leaves: solid, but not somewhere a random teleport should drop you. */
		LEAVES,
		/** Lava, fire, magma, cactus, berry bushes, powder snow, cobwebs, portals... */
		DANGER
	}

	/** The world as cells. {@link #fits} lets the world side make a last, exact check (its bounding box, the world border). */
	public interface Grid {
		Cell cell(int x, int y, int z);

		default boolean fits(int x, int y, int z) {
			return true;
		}
	}

	/** How far a search looks round a spot: 3 blocks to each side, 6 down and 4 up. */
	public static final int REACH = 3;
	public static final int DOWN = 6;
	public static final int UP = 4;

	/** Every offset a search tries, nearest first (and, at the same distance, lower first, then a fixed order). */
	private static final List<int[]> OFFSETS = offsets();

	private static List<int[]> offsets() {
		List<int[]> list = new ArrayList<>();
		for (int dy = -DOWN; dy <= UP; dy++) {
			for (int dx = -REACH; dx <= REACH; dx++) {
				for (int dz = -REACH; dz <= REACH; dz++) {
					list.add(new int[] {dx, dy, dz});
				}
			}
		}
		list.sort(Comparator.<int[]>comparingInt(o -> o[0] * o[0] + o[1] * o[1] + o[2] * o[2])
			.thenComparingInt(o -> o[1])
			.thenComparingInt(o -> o[0])
			.thenComparingInt(o -> o[2]));
		return List.copyOf(list);
	}

	/** Whether a player can stand with their feet in block {@code x}, {@code y}, {@code z}. */
	public static boolean standable(Grid grid, int x, int y, int z, boolean strict) {
		Cell below = grid.cell(x, y - 1, z);
		if (below != Cell.FLOOR && (strict || below != Cell.LEAVES)) {
			return false;
		}
		Cell feet = grid.cell(x, y, z);
		if (feet != Cell.OPEN && (strict || feet != Cell.WATER)) {
			return false;
		}
		return grid.cell(x, y + 1, z) == Cell.OPEN && grid.fits(x, y, z);
	}

	/**
	 * The nearest spot to {@code x}, {@code y}, {@code z} a player can stand on (the spot itself if it's
	 * fine), within {@link #REACH} blocks to the side, {@link #DOWN} below and {@link #UP} above.
	 *
	 * @return {x, y, z} of the feet's block, or null when there's nowhere safe that close
	 */
	public static int[] find(Grid grid, int x, int y, int z, boolean strict) {
		for (int[] o : OFFSETS) {
			if (standable(grid, x + o[0], y + o[1], z + o[2], strict)) {
				return new int[] {x + o[0], y + o[1], z + o[2]};
			}
		}
		return null;
	}
}
