package dev.wildercord.loadout;

import java.util.List;
import java.util.Locale;
import java.util.function.IntUnaryOperator;

/**
 * The rules for loadouts, with no Minecraft types so they're unit-tested: how many a player keeps,
 * what a name may be, what saving under a name does, what stops a loadout loading, which one the
 * quick-switch key goes to next, and which spells a load puts on their cooldown.
 */
public final class LoadoutRules {
	private LoadoutRules() {}

	/** The most loadouts a player keeps. */
	public static final int MAX = 6;
	/** The longest name a loadout may have. */
	public static final int MAX_NAME = 24;

	// ------------------------------------------------------------------ names

	/**
	 * A name as it's kept: formatting codes and control characters dropped, trimmed, and cut to
	 * {@link #MAX_NAME}. Null when nothing is left.
	 */
	public static String name(String raw) {
		if (raw == null) {
			return null;
		}
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			if (c == '§') {
				// A formatting code: drop it and the letter after it.
				i++;
			} else if (c >= ' ' && c != 127) {
				out.append(c);
			}
		}
		String name = out.toString().trim();
		if (name.length() > MAX_NAME) {
			name = name.substring(0, MAX_NAME).trim();
		}
		return name.isEmpty() ? null : name;
	}

	/** Where the loadout called {@code name} is among {@code names}, ignoring case, or -1. */
	public static int find(List<String> names, String name) {
		if (name == null) {
			return -1;
		}
		String wanted = name.toLowerCase(Locale.ROOT);
		for (int i = 0; i < names.size(); i++) {
			if (names.get(i).toLowerCase(Locale.ROOT).equals(wanted)) {
				return i;
			}
		}
		return -1;
	}

	/** What saving under a name does. */
	public enum Save { NEW, REPLACE, FULL }

	/** Saving as {@code name}: over the loadout of that name (always allowed), a new one, or refused because {@code max} are kept. */
	public static Save save(List<String> names, String name, int max) {
		if (find(names, name) >= 0) {
			return Save.REPLACE;
		}
		return names.size() >= max ? Save.FULL : Save.NEW;
	}

	// ------------------------------------------------------------------ loading

	/** What stops a loadout loading right now, in the order they're checked. */
	public enum Block { NONE, NO_CORD, DEAD, CHARGING, DUEL, SEALED }

	/**
	 * Whether a loadout may load now. Never while a spell is being charged (the charge would go off as a
	 * different spell), in a duel (countdown included: what you walk in with is what you fight with), or
	 * sealed in a Cryostasis (the ice is no place to rethread a Cord).
	 */
	public static Block block(boolean cord, boolean alive, boolean charging, boolean duel, boolean sealed) {
		if (!cord) {
			return Block.NO_CORD;
		}
		if (!alive) {
			return Block.DEAD;
		}
		if (charging) {
			return Block.CHARGING;
		}
		if (duel) {
			return Block.DUEL;
		}
		return sealed ? Block.SEALED : Block.NONE;
	}

	/** The loadout the quick-switch key loads after {@code current} (-1 for none yet): round to the first. -1 with none saved. */
	public static int next(int current, int count) {
		if (count <= 0) {
			return -1;
		}
		return current < 0 || current >= count - 1 ? 0 : current + 1;
	}

	/** Which loadout was last loaded once {@code removed} is deleted: the same one moved up, or none if it was that one. */
	public static int afterDelete(int current, int removed) {
		if (current == removed) {
			return -1;
		}
		return current > removed ? current - 1 : current;
	}

	/**
	 * When each spell is ready again after a loadout is loaded. {@code before} and {@code after} are the
	 * runes that fire in each spell slot (the quiet ones left out). A spell whose firing runes changed,
	 * and that isn't cooling down already, starts its full cooldown now ({@code cooldown} gives it in
	 * ticks for a slot, 0 when there's nothing to cast); every other keeps the time it had. So swapping
	 * loadouts is never a way round a cooldown: a spell cooling down stays cooling, and a fresh one has to
	 * wait as if it had just been cast.
	 */
	public static long[] readyAfterLoad(List<List<String>> before, List<List<String>> after, long[] readyAt, long now, IntUnaryOperator cooldown) {
		long[] next = readyAt.clone();
		for (int slot = 0; slot < next.length; slot++) {
			List<String> was = slot < before.size() ? before.get(slot) : List.of();
			List<String> is = slot < after.size() ? after.get(slot) : List.of();
			if (was.equals(is) || next[slot] > now) {
				continue;
			}
			int ticks = cooldown.applyAsInt(slot);
			if (ticks > 0) {
				next[slot] = now + ticks;
			}
		}
		return next;
	}
}
