package dev.wildercord.gear;

import java.util.ArrayList;
import java.util.List;

/**
 * Which spell slots are open: the Cord's own (the first {@code cordSpells}) and, while the Tome of
 * the Fifth Page is in the off-hand, the tome's slot ({@link #TOME}, always the fifth, whatever the
 * Cord). Pure, so both sides and the tests agree.
 */
public final class SpellSlots {
	private SpellSlots() {}

	/** Every slot a spellbook keeps: the Echo Cord's four and the tome's. */
	public static final int ALL = 5;
	/** The tome's slot: the fifth. */
	public static final int TOME = 4;

	public static boolean open(int cordSpells, boolean tome, int spell) {
		return spell >= 0 && (spell < cordSpells || tome && spell == TOME);
	}

	/** The open slots, in order. */
	public static List<Integer> open(int cordSpells, boolean tome) {
		List<Integer> open = new ArrayList<>();
		for (int i = 0; i < Math.min(cordSpells, TOME); i++) {
			open.add(i);
		}
		if (tome) {
			open.add(TOME);
		}
		return open;
	}

	/**
	 * The slot a request lands on: itself if it's open, otherwise the next open one after it, round to
	 * the first (so "the selected one plus one" steps through the Cord's spells and on to the tome's).
	 * A negative request means the last open slot.
	 */
	public static int resolve(int cordSpells, boolean tome, int requested) {
		List<Integer> open = open(cordSpells, tome);
		if (open.isEmpty()) {
			return 0;
		}
		if (requested < 0) {
			return open.getLast();
		}
		for (int slot : open) {
			if (slot >= requested) {
				return slot;
			}
		}
		return open.getFirst();
	}
}
