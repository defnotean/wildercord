package dev.wildercord.spell;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The pure numbers and rules behind the delving pack (mining, masonry, lighting, light redstone and
 * storage runes; see cast/DelveEffects). No game classes here, so every bound can be tested.
 */
public final class DelveRules {
	private DelveRules() {}

	/** Every rune of the pack, in roster order. */
	public static final List<String> PATHS = List.of(
		"stairdelve", "riser", "plumbline", "siftfall", "gangue", "orepluck", "luckstrike", "silklift", "deepsound", "oretally",
		"lavaseal", "deepway", "hollowsense", "kilnbake", "blockpack", "unpack", "millstone", "toolmend", "levelground", "holefill",
		"shoreup", "stilt", "plankway", "polish", "brickwork", "agestone", "concreteset", "chalkline", "pitfloor", "torchfall",
		"gloomsight", "lumenpath", "snuffout", "headlamp", "leverflip", "buttonpush", "doorcall", "chestsort", "stow", "restock",
		"stocktake", "unburden", "lodepull", "caveward", "delvemark", "motherlode", "floorlay", "packtidy");

	public static final int STAIR_STEPS = 5;
	public static final int SHAFT_DEPTH = 8;
	public static final int SIFT_HEIGHT = 4;
	public static final int ORE_PLUCK_MAX = 8;
	public static final int DEEPSOUND_DEPTH = 16;
	public static final int TALLY_RADIUS = 6;
	public static final int SEAL_MAX = 16;
	public static final int CAVE_RADIUS = 12;
	public static final int BAKE_MAX = 9;
	public static final int PACK_MAX = 8;
	public static final int UNPACK_MAX = 4;
	public static final int GRIND_MAX = 16;
	public static final int MEND_MAX = 4;
	public static final int LEVEL_HEIGHT = 3;
	public static final int FILL_MAX = 16;
	public static final int SHORE_MAX = 8;
	public static final int STILT_MAX = 5;
	public static final int BRIDGE_MAX = 12;
	public static final int DRESS_MAX = 9;
	public static final int SET_MAX = 16;
	public static final int TORCH_MAX = 6;
	public static final int TORCH_SPACING = 5;
	public static final int LUMEN_MAX = 6;
	public static final int LUMEN_SPACING = 4;
	public static final int SWITCH_MAX = 4;
	public static final int MOTHERLODE_MAX = 16;
	public static final int FLOOR_SIDE = 5;
	/** How far (blocks) a Delvemark can call you back from, and how long (ticks) the mark lasts. */
	public static final int MARK_RANGE = 128;
	public static final int MARK_TICKS = 12000;
	/** The still moment (ticks) before a Delvemark return lands. */
	public static final int MARK_WARMUP = 40;
	/** The hardest a pack's own areas may reach, whatever Radius adds: no rune of the pack edits past 32 blocks a cast. */
	public static final int RADIUS_CAP = 8;

	/** A base radius grown by the spell's Radius scale, never past {@link #RADIUS_CAP}. */
	public static int radius(int base, double scale) {
		long r = Math.round(base * Math.max(0.0, scale));
		return (int) Math.max(1, Math.min(RADIUS_CAP, r));
	}

	/**
	 * The cells (forward, up) a staircase carves from the caster's feet, step by step: three tall going down (feet, head
	 * and the head's path from the step before), and going up the step's feet and head plus the head room over the step
	 * before.
	 */
	public static List<int[]> stairCells(int steps, boolean down) {
		List<int[]> cells = new ArrayList<>();
		for (int i = 1; i <= steps; i++) {
			if (down) {
				cells.add(new int[] {i, -i + 1});
				cells.add(new int[] {i, -i + 2});
				cells.add(new int[] {i, -i + 3});
			} else {
				cells.add(new int[] {i, i});
				cells.add(new int[] {i, i + 1});
				cells.add(new int[] {i - 1, i + 1});
			}
		}
		return cells;
	}

	/** The ore kind of a block id's path ("deepslate_iron_ore" is "iron"), or null if it isn't an ore. */
	public static String oreKind(String blockPath) {
		if (blockPath.equals("ancient_debris")) {
			return "debris";
		}
		if (blockPath.equals("nether_quartz_ore")) {
			return "quartz";
		}
		if (!blockPath.endsWith("_ore")) {
			return null;
		}
		String kind = blockPath.substring(0, blockPath.length() - 4);
		for (String prefix : List.of("deepslate_", "nether_")) {
			if (kind.startsWith(prefix)) {
				kind = kind.substring(prefix.length());
			}
		}
		return kind.isEmpty() ? null : kind;
	}

	/** A tally, biggest first (ties by name), as "3 iron, 2 coal"; "none" when empty. Five kinds at most. */
	public static String tally(Map<String, Integer> counts) {
		List<Map.Entry<String, Integer>> rows = new ArrayList<>(counts.entrySet());
		rows.removeIf(e -> e.getValue() <= 0);
		if (rows.isEmpty()) {
			return "none";
		}
		rows.sort(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed().thenComparing(Map.Entry::getKey));
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < Math.min(5, rows.size()); i++) {
			if (i > 0) {
				out.append(", ");
			}
			out.append(rows.get(i).getValue()).append(' ').append(rows.get(i).getKey());
		}
		return out.toString();
	}

	/** Fortune for the pack's lucky mining: Luckstrike I (II amplified), Motherlode II; never past vanilla's III. */
	public static int fortune(boolean motherlode, int amplify) {
		int level = (motherlode ? 2 : 1) + (amplify > 0 ? 1 : 0);
		return Math.min(3, level);
	}

	/** How many 9-into-1 packs fit in {@code count} items, at most {@link #PACK_MAX}. */
	public static int packs(int count) {
		return Math.max(0, Math.min(PACK_MAX, count / 9));
	}

	/**
	 * How many repair materials Tool Mend spends on a tool with {@code damage} wear out of {@code max}, carrying
	 * {@code carried}: a quarter of the whole for each (at least 1 point), never more than the wear needs, 4 at most.
	 */
	public static int mendUnits(int damage, int max, int carried) {
		if (damage <= 0 || max <= 0 || carried <= 0) {
			return 0;
		}
		int per = Math.max(1, max / 4);
		int needed = (damage + per - 1) / per;
		return Math.min(MEND_MAX, Math.min(carried, needed));
	}

	/** The wear left after {@code units} materials: never below none. */
	public static int mended(int damage, int max, int units) {
		return Math.max(0, damage - units * Math.max(1, max / 4));
	}

	/** How tall a Stilt rises: the open air over the caster's head, at most {@link #STILT_MAX}. */
	public static int stiltHeight(int openAbove) {
		return Math.max(0, Math.min(STILT_MAX, openAbove));
	}

	/** How far a Plankway reaches: open cells before the first solid one, the blocks carried, {@link #BRIDGE_MAX}. */
	public static int bridgeLength(int openCells, int carried) {
		return Math.max(0, Math.min(BRIDGE_MAX, Math.min(openCells, carried)));
	}

	/** Whether a spot is at least {@code spacing} blocks (straight line) from every spot already chosen. */
	public static boolean spaced(List<int[]> chosen, int[] spot, int spacing) {
		for (int[] c : chosen) {
			long dx = c[0] - spot[0];
			long dy = c[1] - spot[1];
			long dz = c[2] - spot[2];
			if (dx * dx + dy * dy + dz * dz < (long) spacing * spacing) {
				return false;
			}
		}
		return true;
	}

	/** How many lights a Lumen Path hangs along a line {@code length} blocks long: one every 4 blocks, 6 at most. */
	public static int lumens(double length) {
		if (length < 1) {
			return 1;
		}
		return (int) Math.max(1, Math.min(LUMEN_MAX, Math.ceil(length / LUMEN_SPACING)));
	}

	/** Whether a Delvemark can call you back across this offset: same dimension is the caller's to check. */
	public static boolean markReach(double dx, double dy, double dz) {
		return dx * dx + dy * dy + dz * dz <= (double) MARK_RANGE * MARK_RANGE;
	}

	/**
	 * Splits a total into stacks of at most {@code max} each (a sorted chest's layout for one kind of item): full
	 * stacks first, then what's left.
	 */
	public static List<Integer> stacks(int total, int max) {
		List<Integer> out = new ArrayList<>();
		if (max <= 0) {
			return out;
		}
		int left = total;
		while (left > 0) {
			int n = Math.min(max, left);
			out.add(n);
			left -= n;
		}
		return out;
	}
}
