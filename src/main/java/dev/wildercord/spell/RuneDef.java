package dev.wildercord.spell;

import java.util.Set;

/**
 * One rune's definition. Pure data with no Minecraft types, so the spell compiler, its
 * readout and its tests run anywhere.
 *
 * @param id          namespaced id, e.g. {@code wildercord:fire}
 * @param multiplier  shapes: multiplier on their effects' cost; modifiers: multiplier on
 *                    the cost of what they attach to; otherwise 1
 * @param element     element tag for effects (fire, frost, storm, wind, earth, life, void,
 *                    arcane), or empty
 * @param traits      what modifiers can change on this rune (see {@link Trait})
 * @param needs       modifiers only: the trait a rune must have for this to attach
 * @param category    sub-category within the family, e.g. "damage" or "support" (see {@link RuneCategories})
 */
public record RuneDef(
	String id,
	String name,
	RuneFamily family,
	int tier,
	double cost,
	double multiplier,
	String element,
	EffectKind kind,
	Set<String> traits,
	String needs,
	String description,
	String category
) {
	public RuneDef {
		traits = Set.copyOf(traits);
	}

	public boolean is(String otherId) {
		return id.equals(otherId);
	}

	public boolean has(String trait) {
		// ---- links-mods pack: the hearth modifiers' traits are worked out from what an effect is (see HearthLinkRules)
		return traits.contains(trait) || HearthLinkRules.derived(this, trait);
	}

	/** The id's path, e.g. {@code fire} for {@code wildercord:fire}. */
	public String path() {
		int colon = id.indexOf(':');
		return colon < 0 ? id : id.substring(colon + 1);
	}
}
