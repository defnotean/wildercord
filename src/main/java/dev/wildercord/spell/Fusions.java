package dev.wildercord.spell;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Fusion Altar's rules, which work out what you mean from what you put in:
 * <ul>
 *   <li><b>Upgrade</b>: three of the same rune, at the same rank, make that rune's next rank
 *       ({@link Ranks}). 2 XP levels for rank II, 5 for rank III.</li>
 *   <li><b>Combine</b>: two effects of the right elements and an amethyst shard make a fused effect,
 *       for {@link #COMBINE_XP} XP levels. Recipes match on element tags, not on particular runes, so
 *       an add-on's fire effect works in every fire fusion.</li>
 *   <li><b>Tie a Knot</b>: a Blank Rune and string, and one of your spells, make a {@link Knots Knot}.</li>
 * </ul>
 * Pure rules, shared by the altar's screen (which shows what will happen) and the server (which
 * checks it all again before anything is used up).
 */
public final class Fusions {
	private Fusions() {}

	public static final int COMBINE_XP = 3;
	/** The Grimoire's key prefix for a fusion found: {@code fusion:firestorm}. */
	public static final String KEY_PREFIX = "fusion:";

	/** Two elements that fuse, and what they make. */
	public record Recipe(String first, String second, RuneDef result) {
		public boolean takes(String a, String b) {
			return first.equals(a) && second.equals(b) || first.equals(b) && second.equals(a);
		}

		public String key() {
			return KEY_PREFIX + result.path();
		}
	}

	/** Every fusion, in the order the Grimoire lists them. */
	public static final List<Recipe> RECIPES = List.of(
		new Recipe("fire", "wind", Runes.FIRESTORM),
		new Recipe("fire", "frost", Runes.STEAM),
		new Recipe("fire", "earth", Runes.MAGMA),
		new Recipe("storm", "wind", Runes.TEMPEST),
		new Recipe("storm", "fire", Runes.PLASMA),
		new Recipe("storm", "frost", Runes.HAIL),
		new Recipe("frost", "earth", Runes.GLACIER),
		new Recipe("life", "void", Runes.LIFESTEAL),
		new Recipe("wind", "void", Runes.WARP),
		new Recipe("life", "earth", Runes.BLOOM),
		new Recipe("life", "storm", Runes.SURGE),
		new Recipe("arcane", "void", Runes.NULLIFY));

	/** Whether a rune can go into a fusion: an effect with an element, and not an innate rune. */
	public static boolean fusible(RuneDef rune) {
		return rune.family() == RuneFamily.EFFECT && !rune.element().isEmpty() && !Runes.innate(rune);
	}

	/** What two effects fuse into, if anything. */
	public static Optional<Recipe> recipe(RuneDef a, RuneDef b) {
		if (!fusible(a) || !fusible(b)) {
			return Optional.empty();
		}
		for (Recipe recipe : RECIPES) {
			if (recipe.takes(a.element(), b.element())) {
				return Optional.of(recipe);
			}
		}
		return Optional.empty();
	}

	/** The recipe that makes {@code result}, if it's a fused effect. */
	public static Optional<Recipe> recipeFor(RuneDef result) {
		return RECIPES.stream().filter(r -> r.result().is(result.id())).findFirst();
	}

	// ------------------------------------------------------------------ what's on the altar

	/** What's in one of the altar's three rune slots. */
	public record Slot(RuneDef rune, int rank, boolean blank) {
		public static final Slot EMPTY = new Slot(null, 0, false);
		public static final Slot BLANK = new Slot(null, 0, true);
		/** Something the altar doesn't know (a rune from a missing add-on). */
		public static final Slot SILENT = new Slot(null, 1, false);

		public static Slot of(RuneDef rune, int rank) {
			return new Slot(rune, Ranks.clamp(rank), false);
		}

		public boolean empty() {
			return rune == null && !blank && rank == 0;
		}
	}

	/** What's in the altar's fourth slot. */
	public enum Catalyst { NONE, SHARD, STRING, OTHER }

	public enum Kind { NONE, UPGRADE, COMBINE, KNOT }

	/**
	 * What the altar would do.
	 *
	 * @param result  the rune made (null for a Knot: that depends on the spell chosen)
	 * @param rank    the result's rank
	 * @param xp      XP levels it costs (for a Knot, see {@link Knots#xpCost})
	 * @param problem why it can't go ahead, or null
	 * @param recipe  for a Combine, the recipe it follows
	 */
	public record Plan(Kind kind, RuneDef result, int rank, int xp, String problem, Recipe recipe) {
		static Plan hint(String text) {
			return new Plan(Kind.NONE, null, 0, 0, text, null);
		}

		static Plan refuse(Kind kind, String text) {
			return new Plan(kind, null, 0, 0, text, null);
		}

		public boolean ready() {
			return kind != Kind.NONE && problem == null;
		}
	}

	public static final String HOW = "Three of the same rune rank it up. Two effects and an amethyst shard combine. A Blank Rune and string tie a spell into a Knot.";

	/** Works out which fusion three rune slots and a catalyst mean, and whether it can go ahead. */
	public static Plan plan(List<Slot> slots, Catalyst catalyst) {
		List<Slot> filled = new ArrayList<>();
		boolean blank = false;
		for (Slot slot : slots) {
			if (!slot.empty()) {
				filled.add(slot);
				blank |= slot.blank();
			}
		}
		if (filled.isEmpty()) {
			return Plan.hint(catalyst == Catalyst.NONE ? HOW : "Now add runes.");
		}
		if (blank) {
			if (filled.size() > 1) {
				return Plan.refuse(Kind.KNOT, "A Knot takes only the Blank Rune: take the other runes out.");
			}
			if (catalyst != Catalyst.STRING) {
				return Plan.refuse(Kind.KNOT, "Tying a Knot takes string too.");
			}
			return new Plan(Kind.KNOT, null, 1, Knots.MIN_XP, null, null);
		}
		for (Slot slot : filled) {
			if (slot.rune() == null) {
				return Plan.hint("A silent rune (its add-on is missing) can't be fused.");
			}
		}
		if (filled.size() == 3) {
			if (catalyst != Catalyst.NONE) {
				return Plan.refuse(Kind.UPGRADE, "Ranking up takes only the three runes: take the catalyst out.");
			}
			RuneDef rune = filled.getFirst().rune();
			int rank = filled.getFirst().rank();
			for (Slot slot : filled) {
				if (!slot.rune().is(rune.id()) || slot.rank() != rank) {
					return Plan.refuse(Kind.UPGRADE, "Ranking up takes three of the same rune, at the same rank.");
				}
			}
			if (!Ranks.rankable(rune)) {
				return Plan.refuse(Kind.UPGRADE, rune.name() + " has no power to rank up: only effects with power have ranks.");
			}
			if (rank >= Ranks.MAX) {
				return Plan.refuse(Kind.UPGRADE, rune.name() + " is already at its highest rank.");
			}
			return new Plan(Kind.UPGRADE, rune, rank + 1, Ranks.xpCost(rank + 1), null, null);
		}
		if (filled.size() == 2) {
			if (catalyst != Catalyst.SHARD) {
				return Plan.refuse(Kind.COMBINE, "Combining two effects takes an amethyst shard.");
			}
			RuneDef a = filled.get(0).rune();
			RuneDef b = filled.get(1).rune();
			if (!fusible(a) || !fusible(b)) {
				return Plan.refuse(Kind.COMBINE, "Only effects with an element fuse.");
			}
			Optional<Recipe> recipe = recipe(a, b);
			if (recipe.isEmpty()) {
				return Plan.refuse(Kind.COMBINE, a.name() + " and " + b.name() + " don't fuse: fusions join two different elements, like fire and wind.");
			}
			// The fused rune keeps the lower of the two ranks put in, so ranking up first isn't wasted.
			RuneDef made = recipe.get().result();
			int kept = Ranks.rankable(made) ? Math.min(filled.get(0).rank(), filled.get(1).rank()) : 1;
			return new Plan(Kind.COMBINE, made, Math.max(1, kept), COMBINE_XP, null, recipe.get());
		}
		return Plan.hint("Add two more to rank it up, or a second effect and an amethyst shard to combine them.");
	}
}
