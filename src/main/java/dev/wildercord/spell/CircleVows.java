package dev.wildercord.spell;

import java.util.List;

/**
 * Minecraft-free Circle Vows: the later Heart Circles that otherwise only deepen the heart's numbers each ask
 * for one choice between two vows. Taking a vow is free; releasing one to choose again costs experience levels.
 * A vow works only while its circle is active (a cracked circle silences its vow), and an old save with no
 * vows simply has none chosen.
 */
public final class CircleVows {
	private CircleVows() {}

	/** Experience levels to release a vow (nothing in creative). Choosing again after a release is free. */
	public static final int RELEASE_LEVELS = 5;
	public static final int NONE = 0, FIRST = 1, SECOND = 2;

	/** What one vow does. Factors multiply; mana and regen add. */
	public record Effect(int mana, float regen, double power, double cost, double cooldown, double duration) {
		public static final Effect NONE = new Effect(0, 0, 1, 1, 1, 1);
		public Effect plus(Effect other) {
			return new Effect(mana + other.mana, regen + other.regen, power * other.power, cost * other.cost,
				cooldown * other.cooldown, duration * other.duration);
		}
	}

	/** One side of a vow: a stable id for commands and text, its English name, and its effect. */
	public record Option(String id, String name, String text, Effect effect) {}

	/** The choice a circle asks for. {@code index} is this vow's place in the saved bits and never changes. */
	public record Vow(int circle, int index, Option first, Option second) {
		public Option option(int choice) { return choice == FIRST ? first : choice == SECOND ? second : null; }
		public String numeral() { return NUMERALS[circle]; }
	}

	private static final String[] NUMERALS = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
		"XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX"};

	private static Option option(String id, String name, String text, int mana, float regen, double power, double cost, double cooldown, double duration) {
		return new Option(id, name, text, new Effect(mana, regen, power, cost, cooldown, duration));
	}

	/**
	 * Only the circles that bring no lesson or perk of their own ask for a vow: VIII (Archmage, Relay),
	 * X (Tollgate), XII (Reweave), XIV (Lifeline), XVI (Excise) and XVIII (Conduit) already grant one.
	 * The two sides of the later vows trade one strength for another.
	 */
	public static final List<Vow> ALL = List.of(
		new Vow(9, 0, option("wellspring", "Wellspring", "+30 max mana", 30, 0, 1, 1, 1, 1),
			option("quickening", "Quickening", "+1.5 mana regen per second", 0, 1.5F, 1, 1, 1, 1)),
		new Vow(11, 1, option("keen_edge", "Keen Edge", "Spells 6% stronger", 0, 0, 1.06, 1, 1, 1),
			option("spare_hand", "Spare Hand", "Spells cost 6% less mana", 0, 0, 1, .94, 1, 1)),
		new Vow(13, 2, option("swift_hand", "Swift Hand", "Cooldowns 7% shorter", 0, 0, 1, 1, .93, 1),
			option("long_echo", "Long Echo", "Spell effects last 15% longer", 0, 0, 1, 1, 1, 1.15)),
		new Vow(15, 3, option("overcharge", "Overcharge", "Spells 10% stronger, but cost 5% more", 0, 0, 1.10, 1.05, 1, 1),
			option("austerity", "Austerity", "Spells cost 10% less, but are 3% weaker", 0, 0, .97, .90, 1, 1)),
		new Vow(17, 4, option("torrent", "Torrent", "Cooldowns 10% shorter, effects 10% briefer", 0, 0, 1, 1, .90, .90),
			option("vigil", "Vigil", "Effects last 25% longer, cooldowns 5% longer", 0, 0, 1, 1, 1.05, 1.25)),
		new Vow(19, 5, option("reservoir", "Reservoir", "+60 max mana, but 0.5 less regen", 60, -.5F, 1, 1, 1, 1),
			option("spring", "Spring", "+2.5 mana regen, but 20 less max mana", -20, 2.5F, 1, 1, 1, 1)),
		new Vow(20, 6, option("crown_of_power", "Crown of Power", "Spells 10% stronger", 0, 0, 1.10, 1, 1, 1),
			option("crown_of_ease", "Crown of Ease", "Spells cost 8% less and cooldowns 5% shorter", 0, 0, 1, .92, .95, 1)));

	public static Vow at(int circle) {
		for (Vow vow : ALL) if (vow.circle == circle) return vow;
		return null;
	}

	/** The saved choice for one vow. Both bits set (a malformed save) counts as no choice. */
	public static int choice(int saved, Vow vow) {
		int bits = (saved >>> (vow.index * 2)) & 3;
		return bits == 1 ? FIRST : bits == 2 ? SECOND : NONE;
	}

	public static int with(int saved, Vow vow, int choice) {
		int cleared = clean(saved) & ~(3 << (vow.index * 2));
		return choice == FIRST || choice == SECOND ? cleared | (choice << (vow.index * 2)) : cleared;
	}

	/** Drop unknown bits and malformed pairs, so an edited or future save can never grant more than one side. */
	public static int clean(int saved) {
		int out = 0;
		for (Vow vow : ALL) {
			int choice = choice(saved, vow);
			if (choice != NONE) out |= choice << (vow.index * 2);
		}
		return out;
	}

	/** Everything the chosen vows of the active circles add up to. */
	public static Effect effect(int saved, int activeCircles) {
		Effect total = Effect.NONE;
		for (Vow vow : ALL) {
			Option option = vow.option(choice(saved, vow));
			if (option != null && activeCircles >= vow.circle) total = total.plus(option.effect);
		}
		return total;
	}

	/** Vows this heart could take now but has not. */
	public static List<Vow> open(int saved, int activeCircles) {
		return ALL.stream().filter(vow -> activeCircles >= vow.circle && choice(saved, vow) == NONE).toList();
	}

	public enum Refusal { NO_VOW, NOT_REACHED, ALREADY_TAKEN, NOT_TAKEN, LEVELS }

	/** Why a vow cannot be taken, or null. A circle only offers its vow while it is formed and not cracked. */
	public static Refusal take(int saved, int activeCircles, int circle) {
		Vow vow = at(circle);
		if (vow == null) return Refusal.NO_VOW;
		if (activeCircles < circle) return Refusal.NOT_REACHED;
		return choice(saved, vow) != NONE ? Refusal.ALREADY_TAKEN : null;
	}

	/** Why a vow cannot be released, or null. Releasing never needs the circle active, so a cracked vow is never stuck. */
	public static Refusal release(int saved, int circle, int levels, boolean creative) {
		Vow vow = at(circle);
		if (vow == null) return Refusal.NO_VOW;
		if (choice(saved, vow) == NONE) return Refusal.NOT_TAKEN;
		return creative || levels >= RELEASE_LEVELS ? null : Refusal.LEVELS;
	}

	public static int choice(Vow vow, String id) {
		return vow.first.id.equals(id) ? FIRST : vow.second.id.equals(id) ? SECOND : NONE;
	}
}
