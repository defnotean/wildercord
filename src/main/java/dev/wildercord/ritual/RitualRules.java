package dev.wildercord.ritual;

/**
 * Ritual spells (0.13), as plain numbers. A Ritual Tablet is channelled for a few seconds to work one great spell: clear
 * the sky, call a storm, ripen the fields, ward a sanctuary or bring the dawn. A ritual costs far more mana than a spell,
 * so the casters standing in its circle share the cost, and each consumes a reagent.
 */
public final class RitualRules {
	private RitualRules() {}

	/** How far from the one channelling another caster can stand and still share the cost (blocks). */
	public static final double CIRCLE_RANGE = 8.0;
	/** How far Bounty reaches (blocks, square), and the stages it pushes each crop. */
	public static final int BOUNTY_RADIUS = 12, BOUNTY_HEIGHT = 3, BOUNTY_STAGES = 3;
	/** How far a Sanctuary's ward reaches (blocks) and how long it holds (ticks: five minutes). */
	public static final double SANCTUARY_RADIUS = 32.0;
	public static final int SANCTUARY_TICKS = 20 * 60 * 5;
	/** How long Clear Skies and Call Storm hold the weather (ticks: a day, and a quarter of one). */
	public static final int CLEAR_TICKS = 24000, STORM_TICKS = 6000;

	/** The rituals, in the order sneak-using a tablet cycles through them. */
	public enum Ritual {
		BOUNTY("bounty", 60, 80, 2),
		CLEAR_SKIES("clear_skies", 90, 100, 4),
		CALL_STORM("call_storm", 120, 120, 6),
		SANCTUARY("sanctuary", 160, 140, 8),
		DAWN("dawn", 240, 200, 10);

		public final String id;
		/** The mana it costs, shared by the circle. */
		public final float cost;
		/** How long it must be channelled (ticks). */
		public final int channel;
		/** The circles the one channelling must have formed. */
		public final int minCircle;

		Ritual(String id, float cost, int channel, int minCircle) {
			this.id = id;
			this.cost = cost;
			this.channel = channel;
			this.minCircle = minCircle;
		}

		public Ritual next() {
			Ritual[] all = values();
			return all[(ordinal() + 1) % all.length];
		}

		/** The ritual with this id, or the first for an unknown or missing one. */
		public static Ritual byId(String id) {
			for (Ritual ritual : values()) {
				if (ritual.id.equals(id)) return ritual;
			}
			return BOUNTY;
		}
	}

	/** Whether a caster with {@code circles} formed can lead {@code ritual}. */
	public static boolean canLead(Ritual ritual, int circles) {
		return circles >= ritual.minCircle;
	}

	/**
	 * How much each caster in the circle pays, given the mana each holds: an even share, and whoever can't meet it gives
	 * all they have while the rest cover the difference evenly. Returns null when the circle together holds too little.
	 */
	public static float[] split(float[] mana, float cost) {
		float total = 0;
		for (float m : mana) total += Math.max(0, m);
		if (mana.length == 0 || total + 1e-4F < cost) return null;
		float[] paid = new float[mana.length];
		boolean[] spent = new boolean[mana.length];
		float left = cost;
		int open = mana.length;
		while (left > 1e-4F && open > 0) {
			float share = left / open;
			boolean anyShort = false;
			for (int i = 0; i < mana.length; i++) {
				if (!spent[i] && Math.max(0, mana[i]) - paid[i] < share) {
					anyShort = true;
					left -= Math.max(0, mana[i]) - paid[i];
					paid[i] = Math.max(0, mana[i]);
					spent[i] = true;
					open--;
				}
			}
			if (!anyShort) {
				for (int i = 0; i < mana.length; i++) {
					if (!spent[i]) paid[i] += share;
				}
				left = 0;
			}
		}
		return paid;
	}
}
