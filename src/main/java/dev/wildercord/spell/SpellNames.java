package dev.wildercord.spell;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gives every spell a name from its runes, so the HUD and the spell wheel can say
 * "Splitting Frost Bolt" instead of showing a row of icons. The first group names the spell:
 * its first modifier becomes an adjective, its effects the element, its shape the noun. A link
 * adds what fires next, if it fits. Players can rename any spell; this is only the default.
 */
public final class SpellNames {
	private SpellNames() {}

	/** Longest name the HUD has room for. */
	public static final int MAX_LENGTH = 28;

	private static final Map<String, String> ADJECTIVES = Map.ofEntries(
		Map.entry("amplify", "Greater"), Map.entry("extend", "Lasting"), Map.entry("widen", "Wide"),
		Map.entry("quicken", "Quick"), Map.entry("pierce", "Piercing"), Map.entry("bounce", "Bouncing"),
		Map.entry("split", "Splitting"), Map.entry("homing", "Seeking"), Map.entry("chain", "Chained"),
		Map.entry("frugal", "Lesser"), Map.entry("linger", "Lingering"), Map.entry("volley", "Volleying"),
		Map.entry("focus", "Focused"), Map.entry("overcharge", "Overcharged"), Map.entry("rapid", "Rapid"),
		Map.entry("vow", "Vowed"), Map.entry("blood_price", "Blood-Bought"), Map.entry("execute", "Executing"),
		Map.entry("trial_key", "Opening"), Map.entry("kindled", "Kindled"), Map.entry("unstable", "Unstable"),
		Map.entry("kindred", "Kindred"), Map.entry("thirst", "Thirsting"), Map.entry("belated", "Belated"));

	/** A few effects read better as an element word before a shape. */
	private static final Map<String, String> EFFECT_WORDS = Map.ofEntries(
		Map.entry("harm", "Arcane"), Map.entry("heal", "Healing"), Map.entry("push", "Gale"), Map.entry("pull", "Drawing"),
		Map.entry("frost", "Frost"), Map.entry("fire", "Fire"), Map.entry("shock", "Shock"), Map.entry("venom", "Venom"),
		Map.entry("chill", "Chill"), Map.entry("launch", "Rising"), Map.entry("explode", "Blasting"), Map.entry("grow", "Verdant"),
		Map.entry("light", "Light"), Map.entry("break", "Breaking"), Map.entry("smite", "Holy"), Map.entry("root", "Rooting"));

	public static String auto(List<RuneDef> runes) {
		if (runes.isEmpty()) {
			return "";
		}
		if (RelayRules.contains(runes) && !RelayRules.valid(runes)) return "Unfinished Relay";
		if (ReweaveRules.contains(runes) && !ReweaveRules.valid(runes)) return "Unfinished Reweave";
		return auto(SpellCompiler.compile(runes).root());
	}

	public static String auto(SpellPlan.Segment root) {
		if (root == null || root.groups.isEmpty()) {
			return root != null && root.link != null ? root.link.link.name() : "";
		}
		String first = groupName(root.groups.getFirst());
		if (root.link != null && root.link.next != null && !root.link.next.groups.isEmpty()) {
			String next = groupName(root.link.next.groups.getFirst());
			String joined = first + " › " + next;
			if (joined.length() <= MAX_LENGTH) {
				return joined;
			}
			return fit(first) + "+";
		}
		if (root.groups.size() > 1) {
			String joined = first + " & " + root.groups.get(1).shape.name();
			if (joined.length() <= MAX_LENGTH) {
				return joined;
			}
		}
		return fit(first);
	}

	private static String groupName(SpellPlan.Group g) {
		Set<String> words = new LinkedHashSet<>();
		for (SpellPlan.EffectNode node : g.effects) {
			words.add(effectWord(node.effect));
		}
		List<String> elements = new ArrayList<>(words);
		String effects = elements.isEmpty() ? "" : elements.size() == 1 ? elements.getFirst()
			: elements.get(0) + "-" + elements.get(1);
		String adjective = adjective(g);
		boolean self = g.shape.is(Runes.SELF.id());
		if (g.shape.is(Runes.TRIGGER.id())) {
			return join(adjective, effects.isEmpty() ? "Strike" : effects + " Strike");
		}
		if (self) {
			// On yourself the effect is the whole story: "Swift", "Stoneskin & Leap".
			List<String> names = new ArrayList<>();
			for (SpellPlan.EffectNode node : g.effects) {
				if (!names.contains(node.effect.name())) {
					names.add(node.effect.name());
				}
			}
			String body = names.isEmpty() ? "Self" : names.size() == 1 ? names.getFirst() : names.get(0) + " & " + names.get(1);
			return join(adjective, body);
		}
		return join(adjective, join(effects, g.shape.name()));
	}

	private static String adjective(SpellPlan.Group g) {
		for (RuneDef mod : g.shapeMods) {
			String word = ADJECTIVES.get(mod.path());
			if (word != null) {
				return word;
			}
		}
		for (SpellPlan.EffectNode node : g.effects) {
			for (RuneDef mod : node.mods) {
				String word = ADJECTIVES.get(mod.path());
				if (word != null) {
					return word;
				}
			}
		}
		return "";
	}

	private static String effectWord(RuneDef effect) {
		String word = EFFECT_WORDS.get(effect.path());
		return word != null ? word : effect.name();
	}

	private static String join(String a, String b) {
		return a.isEmpty() ? b : b.isEmpty() ? a : a + " " + b;
	}

	/** Drops leading words until the name fits. */
	private static String fit(String name) {
		String out = name;
		while (out.length() > MAX_LENGTH && out.indexOf(' ') > 0) {
			out = out.substring(out.indexOf(' ') + 1);
		}
		return out.length() > MAX_LENGTH ? out.substring(0, MAX_LENGTH) : out;
	}

	/** A custom name as typed: trimmed, printable, and short enough for the HUD. */
	public static String clean(String typed) {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < typed.length() && out.length() < MAX_LENGTH; i++) {
			char c = typed.charAt(i);
			if (c == '§') {
				// A formatting code: drop it and the letter after it.
				i++;
			} else if (c >= ' ' && c != 127) {
				out.append(c);
			}
		}
		return out.toString().trim();
	}
}
