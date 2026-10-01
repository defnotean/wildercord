package dev.wildercord.spell;

import java.util.LinkedHashSet;
import java.util.List;

/** The materials a spell actually contains, including both halves of a fused or woven effect. */
public final class VisualElements {
	private VisualElements() {}
	public static List<String> of(List<RuneDef> runes) {
		var elements = new LinkedHashSet<String>();
		for (RuneDef rune : Knots.flatten(runes)) {
			if (WovenRunes.isWoven(rune)) { elements.addAll(of(WovenRunes.contents(rune))); continue; }
			var recipe = Fusions.recipeFor(rune);
			if (recipe.isPresent()) { elements.add(recipe.get().first()); elements.add(recipe.get().second()); }
			else if (!rune.element().isEmpty()) elements.add(rune.element());
		}
		return List.copyOf(elements);
	}
}
