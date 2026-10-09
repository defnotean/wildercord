package dev.wildercord.spell;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The shapes pack: forty-one shapes that pick out a pattern of blocks (the field shapes: a furrow, a shaft,
 * a stairwell, a dome) or a kind of creature (the kin shapes: a herd, your pets, whoever holds a grudge).
 * This is their pure side: where each one's blocks lie relative to where it lands, how strongly it strikes,
 * and how the Codex says it. The world side (finding the ground, filtering ores and water, choosing
 * creatures, the visuals) is {@code dev.wildercord.cast.FieldShapes}.
 *
 * <p>Offsets are in blocks from the anchor block: y up, {@code (fx, fz)} the caster's horizontal facing
 * (one of the four cardinals), and {@code (-fz, fx)} its right hand. A field never strikes more than
 * {@link #MAX_CELLS} blocks, the cast's own block budget.
 */
public final class FieldShapeGeometry {
	private FieldShapeGeometry() {}

	/** No field ever offers more blocks than a cast may touch. */
	public static final int MAX_CELLS = 32;
	/** Lodeseek strikes at most this many ores. */
	public static final int LODE_MAX = 8;
	/** Grudge and Sentinel strike at most this many creatures. */
	public static final int GRUDGE_MAX = 6;
	public static final int SENTINEL_MAX = 8;
	/** A Widen or Focus never scales a field past these bounds. */
	public static final double MIN_SCALE = 0.5;
	public static final double MAX_SCALE = 2.25;

	/** Where a shape is anchored. */
	public enum Anchor {
		/** The ground block under where you look. */
		AIM_GROUND,
		/** The block you look at, whatever face. */
		AIM_BLOCK,
		/** The ground block under your feet. */
		SELF_GROUND,
		/** The first water block along your look. */
		WATER,
		/** You: a kin shape choosing creatures. */
		SELF
	}

	/** One block of a field, relative to the anchor. */
	public record Cell(int x, int y, int z) {
		public double distance() {
			return Math.sqrt(x * x + y * y + z * z);
		}
	}

	/** A pack shape's fixed numbers: its anchor, Codex category, strength, and its base reach (blocks). */
	public record Spec(Anchor anchor, String category, double strength, double reach) {}

	public static final Map<String, Spec> SPECS = Map.ofEntries(
		Map.entry("furrow", new Spec(Anchor.SELF_GROUND, "field", 0.7, 9)),
		Map.entry("plot", new Spec(Anchor.AIM_GROUND, "field", 0.8, 1)),
		Map.entry("seedbed", new Spec(Anchor.AIM_GROUND, "field", 0.6, 2)),
		Map.entry("shaft", new Spec(Anchor.AIM_BLOCK, "field", 0.8, 8)),
		Map.entry("stairwell", new Spec(Anchor.SELF_GROUND, "field", 0.7, 8)),
		Map.entry("corridor", new Spec(Anchor.AIM_BLOCK, "field", 0.8, 8)),
		Map.entry("seam", new Spec(Anchor.AIM_BLOCK, "field", 0.9, 3)),
		Map.entry("facade", new Spec(Anchor.AIM_BLOCK, "field", 0.8, 2)),
		Map.entry("dome", new Spec(Anchor.AIM_GROUND, "field", 0.9, 2)),
		Map.entry("footing", new Spec(Anchor.SELF_GROUND, "field", 0.9, 1)),
		Map.entry("canopy", new Spec(Anchor.AIM_GROUND, "field", 0.9, 1)),
		Map.entry("shoreline", new Spec(Anchor.AIM_GROUND, "field", 0.7, 4)),
		Map.entry("perimeter", new Spec(Anchor.AIM_GROUND, "field", 0.8, 3)),
		Map.entry("spire", new Spec(Anchor.AIM_BLOCK, "field", 1.0, 6)),
		Map.entry("pit", new Spec(Anchor.AIM_GROUND, "field", 0.9, 1)),
		Map.entry("crossway", new Spec(Anchor.AIM_GROUND, "field", 0.8, 4)),
		Map.entry("lodeseek", new Spec(Anchor.AIM_BLOCK, "field", 0.6, 3)),
		Map.entry("vault", new Spec(Anchor.AIM_BLOCK, "field", 0.9, 1)),
		Map.entry("lamplit", new Spec(Anchor.AIM_GROUND, "field", 0.8, 4)),
		Map.entry("fissure", new Spec(Anchor.SELF_GROUND, "field", 1.1, 10)),
		Map.entry("spiral", new Spec(Anchor.AIM_GROUND, "field", 1.0, 4)),
		Map.entry("rosette", new Spec(Anchor.AIM_GROUND, "field", 1.0, 3)),
		Map.entry("stepstones", new Spec(Anchor.SELF_GROUND, "field", 0.8, 5)),
		Map.entry("causeway", new Spec(Anchor.SELF_GROUND, "field", 0.7, 8)),
		Map.entry("hedgerow", new Spec(Anchor.AIM_GROUND, "field", 0.9, 4)),
		Map.entry("lattice", new Spec(Anchor.AIM_GROUND, "field", 0.8, 2)),
		Map.entry("collapse", new Spec(Anchor.AIM_BLOCK, "field", 1.2, 1)),
		Map.entry("fan", new Spec(Anchor.SELF_GROUND, "field", 1.0, 6)),
		Map.entry("bobber", new Spec(Anchor.WATER, "field", 1.0, 2.5)),
		Map.entry("herd", new Spec(Anchor.SELF, "kin", 0.9, 8)),
		Map.entry("fellowship", new Spec(Anchor.SELF, "kin", 0.8, 12)),
		Map.entry("saddle", new Spec(Anchor.SELF, "kin", 1.0, 0)),
		Map.entry("packbond", new Spec(Anchor.SELF, "kin", 1.0, 24)),
		Map.entry("nursery", new Spec(Anchor.SELF, "kin", 1.0, 8)),
		Map.entry("shoal", new Spec(Anchor.SELF, "kin", 0.9, 10)),
		Map.entry("rearguard", new Spec(Anchor.SELF, "kin", 1.1, 8)),
		Map.entry("grudge", new Spec(Anchor.SELF, "kin", 1.2, 24)),
		Map.entry("sentinel", new Spec(Anchor.SELF, "kin", 1.1, 16)),
		Map.entry("aureole", new Spec(Anchor.SELF, "kin", 1.0, 3)),
		Map.entry("tether", new Spec(Anchor.SELF, "kin", 1.1, 3)),
		Map.entry("flock", new Spec(Anchor.SELF, "kin", 1.0, 16)));

	/** The pack's ids, by path, in the order they're registered. */
	public static final List<String> PATHS = List.of("furrow", "plot", "seedbed", "shaft", "stairwell", "corridor", "seam", "facade", "dome",
		"footing", "canopy", "shoreline", "perimeter", "spire", "pit", "crossway", "lodeseek", "vault", "lamplit", "fissure", "spiral", "rosette",
		"stepstones", "causeway", "hedgerow", "lattice", "collapse", "fan", "bobber", "herd", "fellowship", "saddle", "packbond", "nursery",
		"shoal", "rearguard", "grudge", "sentinel", "aureole", "tether", "flock");

	/** The pack path of a rune id ("wildercord:furrow" is "furrow"), or "" for any other namespace. */
	public static String path(String id) {
		return id.startsWith("wildercord:") ? id.substring("wildercord:".length()) : "";
	}

	public static boolean handles(String path) {
		return SPECS.containsKey(path);
	}

	public static Spec spec(String path) {
		return SPECS.get(path);
	}

	/** The pack's strength for {@code path} (Shape strength in {@link SpellNumbers#groupPower}), or null if it isn't one. */
	public static Double strength(String path) {
		Spec spec = SPECS.get(path);
		return spec == null ? null : spec.strength();
	}

	/** A Widen or Focus, clamped: a field stays readable and within its block budget. */
	public static double scale(double shapeRadius) {
		return Math.max(MIN_SCALE, Math.min(MAX_SCALE, shapeRadius));
	}

	/** {@code base} blocks, scaled; never less than one. */
	public static int n(double base, double scale) {
		return Math.max(1, (int) Math.round(base * scale));
	}

	/** A kin shape's reach in blocks, scaled. */
	public static double reach(String path, double scale) {
		return SPECS.get(path).reach() * scale(scale);
	}

	/**
	 * The blocks of field shape {@code path}, relative to its anchor, nearest first, at most
	 * {@link #MAX_CELLS} (Lodeseek's candidates and Shoreline's ground are filtered by the world after).
	 * {@code (fx, fz)} is the caster's cardinal facing. Kin shapes have no cells.
	 */
	public static List<Cell> cells(String path, int fx, int fz, double shapeRadius) {
		double s = scale(shapeRadius);
		int rx = -fz;
		int rz = fx;
		Set<Cell> out = new LinkedHashSet<>();
		switch (path) {
			case "furrow" -> {
				for (int i = 1; i <= n(9, s); i++) out.add(new Cell(fx * i, 0, fz * i));
			}
			case "plot", "footing" -> square(out, n(1, s), 0);
			case "seedbed" -> square(out, n(2, s), 0);
			case "shaft" -> {
				for (int i = 0; i < n(8, s); i++) out.add(new Cell(0, -i, 0));
			}
			case "stairwell" -> {
				// Each step down a block: its floor-to-head space cleared (three high), so you walk down it.
				for (int i = 1; i <= n(8, s); i++) {
					for (int h = 0; h < 3; h++) out.add(new Cell(fx * i, 1 - i + h, fz * i));
				}
			}
			case "corridor" -> {
				for (int i = 0; i < n(8, s); i++) {
					out.add(new Cell(fx * i, 0, fz * i));
					out.add(new Cell(fx * i, 1, fz * i));
				}
			}
			case "seam" -> line(out, rx, rz, n(3, s), 0);
			case "hedgerow" -> line(out, rx, rz, n(4, s), 0);
			case "facade" -> {
				int h = n(2, s);
				for (int y = 0; y < 3; y++) line(out, rx, rz, h, y);
			}
			case "dome" -> {
				// A hollow half-sphere over the ground: its shell, one block thick.
				int r = Math.max(2, n(2, s));
				for (int x = -r; x <= r; x++) {
					for (int y = 1; y <= r + 1; y++) {
						for (int z = -r; z <= r; z++) {
							double d = Math.sqrt(x * x + (y - 1) * (y - 1) + z * z);
							if (Math.abs(d - r) < 0.5) out.add(new Cell(x, y, z));
						}
					}
				}
			}
			case "canopy" -> square(out, n(1, s), 3);
			case "shoreline" -> {
				int r = n(4, s);
				for (int y = 0; y >= -1; y--) {
					for (int x = -r; x <= r; x++) {
						for (int z = -r; z <= r; z++) {
							if (x * x + z * z <= r * r) out.add(new Cell(x, y, z));
						}
					}
				}
				return sorted(out, Integer.MAX_VALUE);
			}
			case "perimeter" -> {
				int h = n(3, s);
				for (int x = -h; x <= h; x++) {
					for (int z = -h; z <= h; z++) {
						if (Math.abs(x) == h || Math.abs(z) == h) out.add(new Cell(x, 0, z));
					}
				}
				// The outline is the point: not nearest first, but every side alike.
				return capped(out);
			}
			case "spire" -> {
				for (int y = 0; y < n(6, s); y++) out.add(new Cell(0, y, 0));
			}
			case "pit" -> {
				int h = n(1, s);
				square(out, h, 0);
				square(out, h, -1);
			}
			case "crossway" -> {
				out.add(new Cell(0, 0, 0));
				for (int i = 1; i <= n(4, s); i++) {
					out.add(new Cell(i, 0, 0));
					out.add(new Cell(-i, 0, 0));
					out.add(new Cell(0, 0, i));
					out.add(new Cell(0, 0, -i));
				}
			}
			case "lodeseek" -> {
				int r = n(3, s);
				for (int x = -r; x <= r; x++) {
					for (int y = -r; y <= r; y++) {
						for (int z = -r; z <= r; z++) out.add(new Cell(x, y, z));
					}
				}
				return sorted(out, Integer.MAX_VALUE);
			}
			case "vault" -> {
				int h = n(1, s);
				for (int y = -h; y <= h; y++) square(out, h, y);
			}
			case "lamplit" -> {
				int h = n(4, s);
				out.add(new Cell(0, 0, 0));
				out.add(new Cell(h, 0, h));
				out.add(new Cell(-h, 0, h));
				out.add(new Cell(h, 0, -h));
				out.add(new Cell(-h, 0, -h));
			}
			case "fissure" -> {
				// A jagged crack running away from you: it steps aside every other block.
				int len = n(10, s);
				for (int i = 2; i < 2 + len; i++) {
					int side = i % 4 == 1 ? 1 : i % 4 == 3 ? -1 : 0;
					out.add(new Cell(fx * i + rx * side, 0, fz * i + rz * side));
				}
			}
			case "spiral" -> {
				double reach = n(4, s) + 0.5;
				for (double t = 0; out.size() < MAX_CELLS; t += 0.35) {
					double r = 0.3 * t;
					if (r > reach) break;
					out.add(new Cell((int) Math.round(Math.cos(t) * r), 0, (int) Math.round(Math.sin(t) * r)));
				}
				return capped(out);
			}
			case "rosette" -> {
				out.add(new Cell(0, 0, 0));
				int inner = n(2, s);
				int outer = n(3, s) + 1;
				for (int k = 0; k < 8; k++) {
					double a = Math.PI * k / 4;
					out.add(new Cell((int) Math.round(Math.cos(a) * inner), 0, (int) Math.round(Math.sin(a) * inner)));
				}
				for (int k = 0; k < 4; k++) {
					double a = Math.PI * k / 2;
					out.add(new Cell((int) Math.round(Math.cos(a) * outer), 0, (int) Math.round(Math.sin(a) * outer)));
				}
			}
			case "stepstones" -> {
				for (int i = 1; i <= n(5, s); i++) out.add(new Cell(fx * 2 * i, 0, fz * 2 * i));
			}
			case "causeway" -> {
				for (int i = 1; i <= n(8, s); i++) {
					for (int k = -1; k <= 1; k++) out.add(new Cell(fx * i + rx * k, 0, fz * i + rz * k));
				}
				return capped(out);
			}
			case "lattice" -> {
				int h = n(2, s);
				for (int x = -h; x <= h; x++) {
					for (int z = -h; z <= h; z++) {
						if (((x + z) & 1) == 0) out.add(new Cell(x, 0, z));
					}
				}
			}
			case "collapse" -> {
				int h = n(1, s);
				square(out, h, 0);
				square(out, h, 1);
			}
			case "fan" -> {
				int far = n(6, s);
				for (int deg = -40; deg <= 40; deg += 20) {
					double a = Math.toRadians(deg);
					double dx = fx * Math.cos(a) + rx * Math.sin(a);
					double dz = fz * Math.cos(a) + rz * Math.sin(a);
					for (int d = 3; d <= far; d++) out.add(new Cell((int) Math.round(dx * d), 0, (int) Math.round(dz * d)));
				}
			}
			case "bobber" -> {
				out.add(new Cell(0, 0, 0));
				out.add(new Cell(1, 0, 0));
				out.add(new Cell(-1, 0, 0));
				out.add(new Cell(0, 0, 1));
				out.add(new Cell(0, 0, -1));
			}
			default -> {
				return List.of();
			}
		}
		return sorted(out, MAX_CELLS);
	}

	/** At most {@code limit} of a filtered candidate list (Lodeseek's ores, Shoreline's banks). */
	public static int limit(String path) {
		return path.equals("lodeseek") ? LODE_MAX : MAX_CELLS;
	}

	/** {@code (x, z)} turned to the nearest cardinal: the facing every field is laid along. */
	public static int[] cardinal(double x, double z) {
		if (Math.abs(x) >= Math.abs(z)) {
			return new int[] {x >= 0 ? 1 : -1, 0};
		}
		return new int[] {0, z >= 0 ? 1 : -1};
	}

	private static void square(Set<Cell> out, int h, int y) {
		for (int x = -h; x <= h; x++) {
			for (int z = -h; z <= h; z++) out.add(new Cell(x, y, z));
		}
	}

	private static void line(Set<Cell> out, int dx, int dz, int h, int y) {
		for (int k = -h; k <= h; k++) out.add(new Cell(dx * k, y, dz * k));
	}

	private static List<Cell> capped(Set<Cell> out) {
		List<Cell> list = new ArrayList<>(out);
		return list.size() > MAX_CELLS ? List.copyOf(list.subList(0, MAX_CELLS)) : List.copyOf(list);
	}

	private static List<Cell> sorted(Set<Cell> out, int limit) {
		List<Cell> list = new ArrayList<>(out);
		list.sort(Comparator.comparingDouble(Cell::distance));
		return List.copyOf(list.size() > limit ? list.subList(0, limit) : list);
	}

	// ------------------------------------------------------------------ the Codex

	/** How the Codex says it (the spell's phrase): null for a shape that isn't one of the pack's. */
	public static String phrase(String path, double shapeRadius) {
		if (!handles(path)) {
			return null;
		}
		double s = scale(shapeRadius);
		return switch (path) {
			case "furrow" -> "a furrow of " + n(9, s) + " blocks ahead of you";
			case "plot" -> "a " + side(n(1, s)) + " plot where you look";
			case "seedbed" -> "a " + side(n(2, s)) + " bed where you look";
			case "shaft" -> "a shaft " + n(8, s) + " blocks down from what you look at";
			case "stairwell" -> "a stairwell of " + n(8, s) + " steps down ahead of you";
			case "corridor" -> "a corridor 2 high and " + n(8, s) + " deep into what you look at";
			case "seam" -> "a seam " + (2 * n(3, s) + 1) + " blocks across what you look at";
			case "facade" -> "a facade " + (2 * n(2, s) + 1) + " wide and 3 high on what you look at";
			case "dome" -> "a dome of radius " + Math.max(2, n(2, s)) + " where you look";
			case "footing" -> "the " + side(n(1, s)) + " under your feet";
			case "canopy" -> "a " + side(n(1, s)) + " canopy 3 blocks over where you look";
			case "shoreline" -> "the shore within " + n(4, s) + " blocks of where you look";
			case "perimeter" -> "the edge of a " + side(n(3, s)) + " square where you look";
			case "spire" -> "a spire " + n(6, s) + " high from what you look at";
			case "pit" -> "a " + side(n(1, s)) + " pit 2 deep where you look";
			case "crossway" -> "a crossway with arms of " + n(4, s) + " where you look";
			case "lodeseek" -> "up to " + LODE_MAX + " ores within " + n(3, s) + " blocks of what you look at";
			case "vault" -> "a " + side(n(1, s)) + " cube around what you look at";
			case "lamplit" -> "the corners and middle of a " + side(n(4, s)) + " square where you look";
			case "fissure" -> "a fissure " + n(10, s) + " blocks ahead of you";
			case "spiral" -> "a spiral out to " + n(4, s) + " blocks where you look";
			case "rosette" -> "a rosette " + (2 * (n(3, s) + 1) + 1) + " across where you look";
			case "stepstones" -> n(5, s) + " stepping stones ahead of you";
			case "causeway" -> "a causeway 3 wide and " + n(8, s) + " long ahead of you";
			case "hedgerow" -> "a hedgerow " + (2 * n(4, s) + 1) + " blocks across where you look";
			case "lattice" -> "a lattice " + side(n(2, s)) + " where you look";
			case "collapse" -> "the " + side(n(1, s)) + " slab you look at and everything under it";
			case "fan" -> "a fan of " + n(6, s) + " blocks ahead of you";
			case "bobber" -> "the water where you look";
			case "herd" -> "every animal within " + blocks(reach(path, s));
			case "fellowship" -> "you and every ally within " + blocks(reach(path, s));
			case "saddle" -> "you, what you ride and whoever rides with you";
			case "packbond" -> "your pets within " + blocks(reach(path, s));
			case "nursery" -> "every young animal within " + blocks(reach(path, s));
			case "shoal" -> "every swimming creature within " + blocks(reach(path, s));
			case "rearguard" -> "everything behind you within " + blocks(reach(path, s));
			case "grudge" -> "up to " + GRUDGE_MAX + " creatures out to get you";
			case "sentinel" -> "up to " + SENTINEL_MAX + " enemies hunting you or an ally";
			case "aureole" -> "you and everything within " + blocks(reach(path, s));
			case "tether" -> "the creature you look at and everything within " + blocks(reach(path, s)) + " of it";
			case "flock" -> "every flying creature within " + blocks(reach(path, s));
			default -> null;
		};
	}

	private static String side(int h) {
		int w = 2 * h + 1;
		return w + "x" + w;
	}

	private static String blocks(double d) {
		double r = Math.round(d * 10) / 10.0;
		return (r == Math.floor(r) ? Integer.toString((int) r) : Double.toString(r)) + " blocks";
	}
}
