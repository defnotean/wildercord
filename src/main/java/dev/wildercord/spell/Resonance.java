package dev.wildercord.spell;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * One of a world's resonances: an exact rune sequence that this world, and only this world, answers with a twist
 * of its own (see {@link ResonanceForge}, which draws them from the world's seed, and {@link ResonanceTwists}).
 * Like a secret spell it's an ordinary spell too, so a guess never looks broken; unlike one, it isn't written in
 * the mod at all, so no two worlds share them and nothing outside the world can give them away.
 *
 * <p>Pure data. The server keeps the world's set (and who found what) and never sends a resonance to a client
 * until its player has found it, or been shown its riddle or its name (see {@link ResonanceLore}).</p>
 *
 * @param id     a short hash, unique to this world's draw (Grimoire key {@code resonance:<id>})
 * @param name   what it's called once found, e.g. "the Glasswind Rite"
 * @param riddle a verse made from its twist and its runes, for Torn Pages and the Grimoire
 * @param runes  the exact sequence, as rune ids
 * @param twist  the id of its twist in {@link ResonanceTwists}
 * @param color  its colour in the Grimoire, the readout and its announcement
 */
public record Resonance(String id, String name, String riddle, List<String> runes, String twist, int color) {
	public static final String KEY_PREFIX = "resonance:";
	/** A riddle read: the Grimoire's {@code hint:} entries are riddles, worth no mana until solved. */
	public static final String HINT_PREFIX = "hint:" + KEY_PREFIX;

	public Resonance {
		runes = List.copyOf(runes);
	}

	public String key() {
		return KEY_PREFIX + id;
	}

	public String hintKey() {
		return HINT_PREFIX + id;
	}

	/** Its twist, or empty for one whose twist this version doesn't know (a world made by a newer one). */
	public Optional<ResonanceTwists.Twist> twistDef() {
		return ResonanceTwists.byId(twist);
	}

	/** Its runes as definitions, or empty if any of them isn't loaded (an add-on removed, say). */
	public Optional<List<RuneDef>> defs() {
		List<RuneDef> defs = new ArrayList<>(runes.size());
		for (String rune : runes) {
			Optional<RuneDef> def = Runes.get(rune);
			if (def.isEmpty()) {
				return Optional.empty();
			}
			defs.add(def.get());
		}
		return Optional.of(List.copyOf(defs));
	}

	/** Whether {@code spell} is exactly this sequence: nothing before, between or after, as with a secret. */
	public boolean matches(List<RuneDef> spell) {
		if (spell.size() != runes.size()) {
			return false;
		}
		for (int i = 0; i < runes.size(); i++) {
			if (!spell.get(i).is(runes.get(i))) {
				return false;
			}
		}
		return true;
	}

	/** Whether {@code ids} are exactly this sequence. */
	public boolean matchesIds(List<String> ids) {
		return runes.equals(ids);
	}
}
