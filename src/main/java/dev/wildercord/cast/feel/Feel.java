package dev.wildercord.cast.feel;

import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.SpellPlan;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * How one group of a spell should read on screen and in the ear: derived, immutable and deterministic (the same runes
 * give the same feel on the server and the client). Pure: no Minecraft types, so it is unit-testable.
 *
 * @param motion   the shape's gesture family
 * @param element  the group's mana-dominant element (the first effect wins a tie), or "" for a shape with no effect
 * @param accent   a second element that has at least 30% of the group's effect mana, or ""
 * @param role     what the first effect does
 * @param band     scale class
 * @param scale    0.6 to 2.2, from cost, charge and tier; band M is today's sizes
 * @param mods     how many of each modifier are on the shape and on the first effect, by path ("amplify" 2)
 * @param shapeId  the shape's id
 * @param effectId the first effect's id, or "" (this is what {@link Signatures} looks up)
 */
public record Feel(Motion motion, String element, String accent, Role role, Band band, double scale, Map<String, Integer> mods, String shapeId,
		String effectId) {
	public Feel {
		mods = Map.copyOf(mods);
	}

	/** How many of the modifier (by path, e.g. {@code "widen"}) this group carries. */
	public int mod(String path) {
		return mods.getOrDefault(path, 0);
	}

	/** Whether this group carries the modifier at least once. */
	public boolean has(String path) {
		return mod(path) > 0;
	}

	/** 0.6 to 2.2: bigger for costlier and charged spells and higher tiers. */
	public static double scaleOf(double cost, double charge, int maxTier) {
		double s = 0.6 + 0.30 * Math.log(1 + Math.max(0, cost) / 12.0) / Math.log(2) + 0.25 * Math.max(0, Math.min(1, charge))
			+ 0.08 * Math.max(0, maxTier - 1);
		return Math.max(0.6, Math.min(2.2, s));
	}

	/**
	 * @param g      the group (shape, modifiers, effects)
	 * @param cost   the whole spell's mana as its list price ({@code Cast.weight()})
	 * @param charge 0 to 1
	 */
	public static Feel of(SpellPlan.Group g, double cost, double charge) {
		RuneDef first = g.effects.isEmpty() ? null : g.effects.getFirst().effect;
		// Insertion order is the spell's order, so ties go to the element that came first.
		Map<String, Double> mana = new LinkedHashMap<>();
		double total = 0;
		int tier = g.shape.tier();
		for (SpellPlan.EffectNode e : g.effects) {
			double c = Math.max(1.0E-6, e.effect.cost());
			if (!e.effect.element().isEmpty()) {
				mana.merge(e.effect.element(), c, Double::sum);
				total += c;
			}
			tier = Math.max(tier, e.effect.tier());
		}
		String element = "";
		double best = 0;
		for (Map.Entry<String, Double> en : mana.entrySet()) {
			if (en.getValue() > best + 1.0E-9) {
				best = en.getValue();
				element = en.getKey();
			}
		}
		String accent = "";
		double second = 0;
		for (Map.Entry<String, Double> en : mana.entrySet()) {
			if (!en.getKey().equals(element) && en.getValue() > second + 1.0E-9) {
				second = en.getValue();
				accent = en.getKey();
			}
		}
		if (total <= 0 || second / total < 0.3) {
			accent = "";
		}
		Map<String, Integer> mods = new HashMap<>();
		for (RuneDef m : g.shapeMods) {
			mods.merge(m.path(), 1, Integer::sum);
		}
		if (first != null) {
			for (RuneDef m : g.effects.getFirst().mods) {
				mods.merge(m.path(), 1, Integer::sum);
			}
		}
		double scale = scaleOf(cost, charge, tier);
		return new Feel(Motion.of(g.shape.id()), element, accent, Role.of(first), Band.of(scale), scale, mods, g.shape.id(),
			first == null ? "" : first.id());
	}

	/** A feel for a bare shape with no plan (a mob's cast, a test): band M. */
	public static Feel plain(String shapeId, String element) {
		return new Feel(Motion.of(shapeId), element, "", Role.STRIKE, Band.M, 1.0, Map.of(), shapeId, "");
	}
}
