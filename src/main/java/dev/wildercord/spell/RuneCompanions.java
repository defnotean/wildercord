package dev.wildercord.spell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * "Goes well with": for one rune, which of the player's other runes make a spell with it. Read from the same rules
 * the spell compiler follows, so it can't drift from them:
 * <ul>
 * <li>a modifier fits a rune that {@link RuneDef#has has} the trait it needs, and that the hearth rules
 * ({@link HearthLinkRules#refusal}) don't refuse;</li>
 * <li>a shape fits an effect when it reaches who that effect is for: harmful effects go to shapes that reach out,
 * helpful ones to you and allies ({@link ResonanceTwists#FRIENDLY}), world effects to shapes that land on a block,
 * movement to you or a point; a field or creature-picking shape fits when it shares a use with the effect;</li>
 * <li>a link fits when it shares a use ({@link RuneCatalog#uses}), e.g. On Catch with fishing effects;</li>
 * <li>a lesson rune (Relay, Excise, Tollgate...) goes only with the one rune its lesson names.</li>
 * </ul>
 * Runes that share a use come first. Traits every rune of a family has (Frugal, the cooldown and circle slots) are
 * left out, as they say nothing about this rune.
 */
public final class RuneCompanions {
	private RuneCompanions() {}

	/** Traits nearly every rune carries, so a modifier needing them is no hint. */
	public static final Set<String> UNIVERSAL = Set.of(Trait.FRUGAL, Trait.COOLDOWN, Trait.CIRCLE, Trait.SHARE);

	/** One row of the panel: which family, the best few, and how many more fit. */
	public record Group(RuneFamily family, List<RuneDef> runes, int more) {}

	/**
	 * What goes well with {@code rune}, drawn from {@code pool} (the runes the player knows), at most {@code limit}
	 * per group. Groups with nothing are left out. {@code anyEffect} is true for a rune that fits every spell;
	 * {@code fixed} for a lesson rune (Relay, Excise...) whose spell must be exactly its two runes, nothing added.
	 */
	public record Suggestions(List<Group> groups, boolean anyEffect, boolean fixed) {}

	/** Whether a spell holding {@code rune} must be exactly its lesson's runes: no modifiers, links or other groups. */
	public static boolean fixed(RuneDef rune) {
		List<RuneDef> one = List.of(rune);
		return RelayRules.contains(one) || ReweaveRules.contains(one) || ExciseRules.contains(one) || LessonPackRules.contains(one);
	}

	private static boolean lessonSpell(List<RuneDef> runes) {
		return RelayRules.valid(runes) || ReweaveRules.valid(runes) || ExciseRules.valid(runes) || LessonPackRules.valid(runes);
	}

	public static Suggestions suggest(RuneDef rune, Collection<RuneDef> pool, int limit) {
		List<RuneDef> shapes = new ArrayList<>();
		List<RuneDef> effects = new ArrayList<>();
		List<RuneDef> modifiers = new ArrayList<>();
		List<RuneDef> links = new ArrayList<>();
		boolean any = false;
		boolean fixed = fixed(rune);
		for (RuneDef other : pool) {
			if (other.id().equals(rune.id())) {
				continue;
			}
			if (fixed || fixed(other)) {
				// A lesson spell is its two runes and nothing else.
				if (lessonSpell(List.of(rune, other)) || lessonSpell(List.of(other, rune))) {
					switch (other.family()) {
						case SHAPE -> shapes.add(other);
						case EFFECT -> effects.add(other);
						default -> {}
					}
				}
				continue;
			}
			switch (rune.family()) {
				case EFFECT -> {
					switch (other.family()) {
						case SHAPE -> add(shapes, other, fits(other, rune));
						case MODIFIER -> add(modifiers, other, attaches(other, rune));
						case LINK -> add(links, other, shares(other, rune));
						default -> {}
					}
				}
				case SHAPE -> {
					switch (other.family()) {
						case EFFECT -> add(effects, other, fits(rune, other));
						case MODIFIER -> add(modifiers, other, attaches(other, rune));
						default -> {}
					}
				}
				case MODIFIER -> {
					switch (other.family()) {
						case SHAPE -> add(shapes, other, attaches(rune, other));
						case EFFECT -> add(effects, other, attaches(rune, other));
						default -> {}
					}
				}
				case LINK -> {
					if (other.family() == RuneFamily.EFFECT) {
						add(effects, other, shares(rune, other));
					}
				}
				default -> {}
			}
		}
		if (rune.family() == RuneFamily.LINK && RuneCatalog.uses(rune).isEmpty() && !fixed) {
			any = true;
		}
		if (rune.family() == RuneFamily.MODIFIER && UNIVERSAL.contains(rune.needs()) && !fixed) {
			any = true;
			effects.clear();
			shapes.clear();
		}
		List<Group> groups = new ArrayList<>();
		group(groups, RuneFamily.SHAPE, shapes, rune, limit);
		group(groups, RuneFamily.EFFECT, effects, rune, limit);
		group(groups, RuneFamily.MODIFIER, modifiers, rune, limit);
		group(groups, RuneFamily.LINK, links, rune, limit);
		return new Suggestions(List.copyOf(groups), any, fixed);
	}

	private static void add(List<RuneDef> into, RuneDef rune, boolean fits) {
		if (fits) {
			into.add(rune);
		}
	}

	private static void group(List<Group> into, RuneFamily family, List<RuneDef> runes, RuneDef to, int limit) {
		if (runes.isEmpty()) {
			return;
		}
		runes.sort(Comparator.<RuneDef>comparingInt(r -> -shared(r, to)).thenComparingInt(r -> written(r, to) ? 0 : 1).thenComparingInt(RuneDef::tier)
			.thenComparing(RuneDef::name).thenComparing(RuneDef::id));
		int n = Math.min(Math.max(0, limit), runes.size());
		into.add(new Group(family, List.copyOf(runes.subList(0, n)), runes.size() - n));
	}

	/** Whether a modifier's trait is written on the rune itself, not only worked out by the hearth rules. */
	private static boolean written(RuneDef a, RuneDef b) {
		if (a.family() == RuneFamily.MODIFIER) {
			return b.traits().contains(a.needs());
		}
		if (b.family() == RuneFamily.MODIFIER) {
			return a.traits().contains(b.needs());
		}
		return true;
	}

	/** How many uses (Passive aside) two runes share. */
	public static int shared(RuneDef a, RuneDef b) {
		EnumSet<RuneCatalog.Use> both = EnumSet.noneOf(RuneCatalog.Use.class);
		both.addAll(RuneCatalog.uses(a));
		both.retainAll(RuneCatalog.uses(b));
		both.remove(RuneCatalog.Use.PASSIVE);
		return both.size();
	}

	/** Whether {@code a} and {@code b} share a use (Passive aside). */
	public static boolean shares(RuneDef a, RuneDef b) {
		return shared(a, b) > 0;
	}

	/** Whether the compiler lets {@code modifier} sit on {@code target}, and it says something about the target. */
	public static boolean attaches(RuneDef modifier, RuneDef target) {
		if (modifier.family() != RuneFamily.MODIFIER || UNIVERSAL.contains(modifier.needs())) {
			return false;
		}
		return target.has(modifier.needs()) && HearthLinkRules.refusal(modifier, List.of()) == null;
	}

	/** Whether {@code shape} carries {@code effect} to whom it's meant for. */
	public static boolean fits(RuneDef shape, RuneDef effect) {
		if (shape.family() != RuneFamily.SHAPE || effect.family() != RuneFamily.EFFECT) {
			return false;
		}
		String cat = shape.category();
		if (cat.equals("field") || cat.equals("kin")) {
			return shares(shape, effect);
		}
		return switch (effect.kind()) {
			case HARMFUL -> !cat.equals("personal");
			case HELPFUL -> cat.equals("personal") || ResonanceTwists.FRIENDLY.contains(shape.path());
			case WORLD -> cat.equals("direct") || cat.equals("projectile") || cat.equals("area");
			case MOVEMENT -> cat.equals("personal") || cat.equals("direct") || cat.equals("projectile");
			case NONE -> false;
		};
	}
}
