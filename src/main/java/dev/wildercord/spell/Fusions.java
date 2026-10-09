package dev.wildercord.spell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * The Fusion Altar's rules, which work out what you mean from what you put in:
 * <ul>
 *   <li><b>Upgrade</b>: three of the same rune, at the same rank, make that rune's next rank
 *       ({@link Ranks}). 2 XP levels for rank II, 5 for rank III.</li>
 *   <li><b>Combine</b>: two effects of the right elements and an amethyst shard make a fused effect,
 *       for {@link #COMBINE_XP} XP levels. Recipes match on element tags, not on particular runes, so
 *       an add-on's fire effect works in every fire fusion. A few pairs of particular runes have a
 *       {@link Signature signature} of their own instead, asked for first: Chill with Shock makes
 *       Frostwire, while any other frost and storm effects still make Hail.</li>
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

	/**
	 * A fusion of either kind: what it makes, the two elements it wears (for its magic circle's two halves,
	 * and the Grimoire's hints), and the Grimoire key it's recorded under once made.
	 */
	public sealed interface Fusion permits Recipe, Signature {
		RuneDef result();

		/** The first of its two elements (a signature's: its first rune's). */
		String first();

		/** The second of its two elements (a signature's: its second rune's). */
		String second();

		default String key() {
			return KEY_PREFIX + result().path();
		}

		/** Whether it's a signature fusion: two particular runes, rather than any two of their elements. */
		default boolean signature() {
			return this instanceof Signature;
		}
	}

	/** Two elements that fuse, and what they make. */
	public record Recipe(String first, String second, RuneDef result) implements Fusion {
		public boolean takes(String a, String b) {
			return first.equals(a) && second.equals(b) || first.equals(b) && second.equals(a);
		}

		@Override
		public String key() {
			return KEY_PREFIX + result.path();
		}
	}

	/**
	 * A signature fusion: two particular effects (not just their elements) that fuse into a rune of their
	 * own. Asked for before the element recipes, so the pair makes this instead of their elements' fusion.
	 */
	public record Signature(RuneDef a, RuneDef b, RuneDef result) implements Fusion {
		public boolean takes(RuneDef x, RuneDef y) {
			return a.is(x.id()) && b.is(y.id()) || a.is(y.id()) && b.is(x.id());
		}

		@Override
		public String first() {
			return a.element();
		}

		@Override
		public String second() {
			return b.element();
		}

		/** The element fusion its two runes would make if it weren't for this one (Hail, for Chill and Shock). */
		public Optional<Recipe> overrides() {
			return elementRecipe(first(), second());
		}
	}

	/**
	 * Every fusion, in the order the Grimoire lists them: one for every pair of the ten elements, and
	 * one for each element with itself, so any two elemental effects fuse into something.
	 */
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
		new Recipe("arcane", "void", Runes.NULLIFY),
		new Recipe("fire", "life", Runes.PHOENIX_PYRE),
		new Recipe("fire", "void", Runes.HELLMOUTH),
		new Recipe("fire", "arcane", Runes.STARFIRE),
		new Recipe("fire", "time", Runes.EVERBURN),
		new Recipe("fire", "blood", Runes.BLOODBOIL),
		new Recipe("frost", "wind", Runes.BLIZZARD),
		new Recipe("frost", "life", Runes.FROSTBLOOM),
		new Recipe("frost", "void", Runes.BLACK_ICE),
		new Recipe("frost", "arcane", Runes.RIME_SEAL),
		new Recipe("frost", "time", Runes.CRYOSTASIS),
		new Recipe("frost", "blood", Runes.FROSTBITE),
		new Recipe("storm", "earth", Runes.MAGNETIZE),
		new Recipe("storm", "void", Runes.RIFTBOLT),
		new Recipe("storm", "arcane", Runes.STORMWEAVE),
		new Recipe("storm", "time", Runes.STORMCLOCK),
		new Recipe("storm", "blood", Runes.HEARTSTOPPER),
		new Recipe("wind", "earth", Runes.DOWNDRAFT),
		new Recipe("wind", "life", Runes.ZEPHYR),
		new Recipe("wind", "arcane", Runes.SKYGLYPH),
		new Recipe("wind", "time", Runes.RECOIL),
		new Recipe("wind", "blood", Runes.CRIMSON_MIST),
		new Recipe("earth", "void", Runes.SINKHOLE),
		new Recipe("earth", "arcane", Runes.GEODE),
		new Recipe("earth", "time", Runes.FOSSILIZE),
		new Recipe("earth", "blood", Runes.BONESPUR),
		new Recipe("life", "arcane", Runes.SOULBOND),
		new Recipe("life", "time", Runes.SECOND_WIND),
		new Recipe("life", "blood", Runes.TRANSFUSION),
		new Recipe("void", "time", Runes.ENTROPY),
		new Recipe("void", "blood", Runes.DEVOUR),
		new Recipe("arcane", "time", Runes.TIMESTEAL),
		new Recipe("arcane", "blood", Runes.HEMOMANCY),
		new Recipe("time", "blood", Runes.RECKONING),
		// Two effects of the same element: that element at its purest.
		new Recipe("fire", "fire", Runes.CONFLAGRATION),
		new Recipe("frost", "frost", Runes.ABSOLUTE_ZERO),
		new Recipe("storm", "storm", Runes.THUNDERHEAD),
		new Recipe("wind", "wind", Runes.UPDRAFT),
		new Recipe("earth", "earth", Runes.MONOLITH),
		new Recipe("life", "life", Runes.LIFEBLOOM),
		new Recipe("void", "void", Runes.SINGULARITY),
		new Recipe("arcane", "arcane", Runes.PRISMATIC_BURST),
		new Recipe("time", "time", Runes.CHRONOSHIFT),
		new Recipe("blood", "blood", Runes.SANGUINE_RITE));

	// ---- signature fusions

	/**
	 * Every signature fusion, in the order the Grimoire lists them: each a pair of particular effects (never
	 * innate, never fused, always ones a caster can come by) and the rune only they make. Exact rune pairs
	 * remain distinct even when another signature uses the same two elements.
	 */
	public static final List<Signature> SIGNATURES = List.of(
		new Signature(Runes.CHILL, Runes.SHOCK, Runes.FROSTWIRE),
		new Signature(Runes.BUBBLE, Runes.FIRE, Runes.SEETHE),
		new Signature(Runes.GROW, Runes.BLINK, Runes.BLOOMSTEP),
		new Signature(Runes.LAUNCH, Runes.EXPLODE, Runes.SKYBURST),
		new Signature(Runes.HEAL, Runes.COUNTDOWN, Runes.STITCHTIME),
		new Signature(Runes.VENOM, Runes.LEECH, Runes.PARASITE),
		new Signature(Runes.WINDCUT, Runes.BLEED, Runes.RAZORGALE),
		new Signature(Runes.PRIMER, Runes.STASIS, Runes.DOOMCLOCK),
		new Signature(Runes.SHADOWSTEP, Runes.LIGHTNING, Runes.THUNDERSTEP),
		new Signature(Runes.SMITE, Runes.REGROWTH, Runes.HALO),
		new Signature(Runes.THUNDERCLAP, Runes.TREMOR, Runes.THUNDERQUAKE),
		new Signature(Runes.STARFALL, Runes.METEOR, Runes.COMETFALL),
		new Signature(Runes.REFLECT, Runes.FORESIGHT, Runes.RIPOSTE),
		new Signature(Runes.SUMMIT_WIND, Runes.SANDSTORM, Runes.DUST_DEVIL),
		new Signature(Runes.HEX, Runes.RESONANCE, Runes.MALISON),
		new Signature(Runes.COLDSNAP, Runes.STALACTITE, Runes.AVALANCHE),
		new Signature(Runes.STRATA_RISE, Runes.FIRE, Runes.CINDER_BULWARK),
		new Signature(Runes.STRATA_RISE, Runes.GROW, Runes.ROOT_BULWARK),
		new Signature(Runes.TIDAL_LIFT, Runes.FIRE, Runes.BOILING_SURGE),
		new Signature(Runes.TIDAL_LIFT, Runes.SHOCK, Runes.THUNDER_TIDE),
		new Signature(Runes.WIND_STEPS, Runes.FROST, Runes.RIME_CAUSEWAY),
		new Signature(Runes.WIND_STEPS, Runes.SHOCK, Runes.THUNDER_WALK),
		new Signature(Runes.BASINFILL, Runes.GROW, Runes.SPRINGBED),
		new Signature(Runes.EMBER, Runes.COLLECT, Runes.CINDER_SIEVE),
		new Signature(Runes.FIREWARD, Runes.CLEANSE, Runes.ASHEN_MERCY),
		new Signature(Runes.ROOT, Runes.FORESIGHT, Runes.CLOCKROOT),
		new Signature(Runes.LEVITATE, Runes.ANCHOR, Runes.SKYLATCH),
		new Signature(Runes.HARVEST, Runes.WINDCUT, Runes.THRESHERWIND),
		new Signature(Runes.REFLECT, Runes.COLLECT, Runes.NULLCATCH),
		new Signature(Runes.SHOCK, Runes.COUNTDOWN, Runes.SECOND_BELL),
		new Signature(Runes.BLEED, Runes.REVEAL, Runes.RED_LEDGER),
		new Signature(Runes.MANABURN, Runes.STASIS, Runes.QUIETUS),
		new Signature(Runes.LEECH, Runes.BARRIER, Runes.BLOOD_ESCROW),
		new Signature(Runes.FROSTWARD, Runes.CLEANSE, Runes.FROST_MOLT),
		new Signature(Runes.REGROWTH, Runes.TIME_SKIP, Runes.PULSE_FERRY),
		new Signature(Runes.LIGHT, Runes.REWIND, Runes.LAST_LANTERN),
		new Signature(Runes.BUBBLE, Runes.COLLECT, Runes.POCKET_CURRENT),
		new Signature(Runes.SPAN, Runes.GRAPPLE, Runes.WAYLINE),
		new Signature(Runes.LIGHT, Runes.ZIPPER, Runes.NIGHT_SEAM),
		new Signature(Runes.PROSPECT, Runes.TREASURE_SENSE, Runes.SHARD_COMPASS));

	/** Whether a rune can go into a fusion: an effect with an element, and not an innate rune. */
	public static boolean fusible(RuneDef rune) {
        if (rune.is(ExciseRules.ID) || LessonPackRules.byRune(rune.id()) != null) return false;
		return rune.family() == RuneFamily.EFFECT && !rune.element().isEmpty() && !Runes.innate(rune)
			&& (!WovenRunes.isWoven(rune) || !WovenRunes.contents(rune).isEmpty());
	}
	/** Exact weaving also supports innate magic; its heart ownership is checked when cast. */
	public static boolean weavable(RuneDef rune) {
        if (rune.is(ExciseRules.ID) || LessonPackRules.byRune(rune.id()) != null) return false;
		return rune.family()==RuneFamily.EFFECT && !rune.element().isEmpty()
			&& (!WovenRunes.isWoven(rune) || !WovenRunes.contents(rune).isEmpty());
	}

	/**
	 * What two effects fuse into, if anything: their signature fusion if the pair has one, or else the
	 * fusion of their two elements.
	 */
	public static Optional<Fusion> recipe(RuneDef a, RuneDef b) {
		if (!fusible(a) || !fusible(b) || WovenRunes.isWoven(a) || WovenRunes.isWoven(b)) {
			return Optional.empty();
		}
		Optional<Signature> signature = signature(a, b);
		if (signature.isPresent()) {
			return Optional.of(signature.get());
		}
		return elementRecipe(a.element(), b.element()).map(Fusion.class::cast);
	}

	/** The signature fusion of these two particular effects, if they have one. */
	public static Optional<Signature> signature(RuneDef a, RuneDef b) {
		if (!fusible(a) || !fusible(b)) {
			return Optional.empty();
		}
		return SIGNATURES.stream().filter(s -> s.takes(a, b)).findFirst();
	}

	/** The fusion of two elements (whatever the effects), if they have one: every pair of the ten does. */
	public static Optional<Recipe> elementRecipe(String a, String b) {
		for (Recipe recipe : RECIPES) {
			if (recipe.takes(a, b)) {
				return Optional.of(recipe);
			}
		}
		return Optional.empty();
	}

	/** The recipe that makes {@code result}, if it's a fused effect: an element fusion or a signature one. */
	public static Optional<Fusion> recipeFor(RuneDef result) {
		Optional<Recipe> recipe = RECIPES.stream().filter(r -> r.result().is(result.id())).findFirst();
		if (recipe.isPresent()) {
			return Optional.of(recipe.get());
		}
		return signatureFor(result).map(Fusion.class::cast);
	}

	/** The signature fusion that makes {@code result}, if it's a signature rune. */
	public static Optional<Signature> signatureFor(RuneDef result) {
		return SIGNATURES.stream().filter(s -> s.result().is(result.id())).findFirst();
	}

	/** Whether {@code rune} is made only by a signature fusion. */
	public static boolean isSignature(RuneDef rune) {
		return signatureFor(rune).isPresent();
	}

	/** How many of the element fusions a Grimoire holds (its signature ones aside). */
	public static int elementFusionsFound(Collection<String> grimoire) {
		return (int) RECIPES.stream().filter(r -> grimoire.contains(r.key())).count();
	}

	/** How many of the signature fusions a Grimoire holds. */
	public static int signaturesFound(Collection<String> grimoire) {
		return (int) SIGNATURES.stream().filter(s -> grimoire.contains(s.key())).count();
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
	public enum Catalyst { NONE, SHARD, BLOCK, STRING, OTHER }

	public enum Kind { NONE, UPGRADE, COMBINE, KNOT }

	/**
	 * What the altar would do.
	 *
	 * @param result  the rune made (null for a Knot: that depends on the spell chosen)
	 * @param rank    the result's rank
	 * @param xp      XP levels it costs (for a Knot, see {@link Knots#xpCost})
	 * @param problem why it can't go ahead, or null
	 * @param recipe  for a shard Combine, the recipe it follows; null when a block weaves an exact pair
	 */
	public record Plan(Kind kind, RuneDef result, int rank, int xp, String problem, Fusion recipe) {
		static Plan hint(String text) {
			return new Plan(Kind.NONE, null, 0, 0, text, null);
		}

		static Plan refuse(Kind kind, String text) {
			return new Plan(kind, null, 0, 0, text, null);
		}

		public boolean ready() {
			return kind != Kind.NONE && problem == null;
		}

		/** Whether it's a Combine that follows a signature fusion (two particular runes). */
		public boolean signature() {
			return recipe != null && recipe.signature();
		}
	}

	public static final String HOW = "Three matching runes rank up. Two effects and an amethyst shard make a named fusion; an amethyst block weaves up to eight exact effects, including an existing weave. A Blank Rune and string tie a Knot. Sneak-use a Blank Rune on the altar to imprint your innate for three XP levels.";

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
			if (catalyst != Catalyst.SHARD && catalyst != Catalyst.BLOCK) {
				return Plan.refuse(Kind.COMBINE, "Combining two effects takes an amethyst shard, or an amethyst block to weave their exact effects.");
			}
			RuneDef a = filled.get(0).rune();
			RuneDef b = filled.get(1).rune();
			if (catalyst==Catalyst.BLOCK ? !weavable(a)||!weavable(b) : !fusible(a)||!fusible(b)) {
				return Plan.refuse(Kind.COMBINE, "Only effects with an element fuse.");
			}
			if (catalyst == Catalyst.BLOCK) {
				RuneDef made;
				try {
					made = WovenRunes.bind(a, b);
				} catch (IllegalArgumentException exception) {
					return Plan.refuse(Kind.COMBINE, "A weave holds at most eight elemental effects; these inputs cannot fit.");
				}
				int kept = Ranks.rankable(made) ? Math.min(filled.get(0).rank(), filled.get(1).rank()) : 1;
				return new Plan(Kind.COMBINE, made, kept, COMBINE_XP * (WovenRunes.contents(made).size() - 1), null, null);
			}
			// A signature fusion of these two particular runes comes first; any other pair makes its elements' fusion.
			Optional<Fusion> recipe = recipe(a, b);
			if (recipe.isEmpty()) {
				return Plan.refuse(Kind.COMBINE, a.name() + " and " + b.name() + " don't fuse: only effects of the ten elements do.");
			}
			// The fused rune keeps the lower of the two ranks put in, so ranking up first isn't wasted (a signature's too).
			RuneDef made = recipe.get().result();
			int kept = Ranks.rankable(made) ? Math.min(filled.get(0).rank(), filled.get(1).rank()) : 1;
			return new Plan(Kind.COMBINE, made, Math.max(1, kept), COMBINE_XP, null, recipe.get());
		}
		return Plan.hint("Add two more to rank it up, or a second effect and amethyst to combine them.");
	}
}
