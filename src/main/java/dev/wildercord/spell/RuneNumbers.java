package dev.wildercord.spell;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The numbers an add-on modifier changes. Built-in modifiers are read by name in
 * {@link SpellNumbers}; a modifier from the add-on API declares its multipliers here instead, and
 * {@link SpellNumbers} folds them in, so the readout and the cast both see them.
 */
public final class RuneNumbers {
	private RuneNumbers() {}

	/** Multipliers on what the modifier attaches to. */
	public record Numbers(double power, double duration, double radius) {
		public static final Numbers NONE = new Numbers(1, 1, 1);
	}

	private static final Map<String, Numbers> BY_ID = new ConcurrentHashMap<>();

	public static void register(String runeId, Numbers numbers) {
		BY_ID.put(runeId, numbers);
	}

	public static Numbers of(String runeId) {
		return BY_ID.getOrDefault(runeId, Numbers.NONE);
	}

	static double power(List<RuneDef> mods) {
		if (BY_ID.isEmpty()) {
			return 1;
		}
		double p = 1;
		for (RuneDef mod : mods) {
			p *= of(mod.id()).power();
		}
		return p;
	}

	static double duration(List<RuneDef> mods) {
		if (BY_ID.isEmpty()) {
			return 1;
		}
		double d = 1;
		for (RuneDef mod : mods) {
			d *= of(mod.id()).duration();
		}
		return d;
	}

	static double radius(List<RuneDef> mods) {
		if (BY_ID.isEmpty()) {
			return 1;
		}
		double r = 1;
		for (RuneDef mod : mods) {
			r *= of(mod.id()).radius();
		}
		return r;
	}
}
