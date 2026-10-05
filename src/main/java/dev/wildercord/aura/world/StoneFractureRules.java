package dev.wildercord.aura.world;

/** Stone plants, braces toward one fixed direction, then warns a narrow fracture reply. */
public final class StoneFractureRules {
	private StoneFractureRules() {}

	public static final int PLANT = 8, BRACE = 12, REPLY_TELL = 12;
	public static final int TELL = PLANT + BRACE + REPLY_TELL, RECOVERY = 40, COOLDOWN = 180, WARNING_REFRESH = 3;
	/** Includes the brace: one payment, with no reactive guard or retaliation added to it. */
	public static final double COST = 28, DAMAGE = 28, MIN_DISTANCE = 1.5, MAX_DISTANCE = 5.5;
	public static final double REACH = 6, HALF_WIDTH = .7, HEIGHT = 1.8;

	public enum Beat { PLANT, BRACE, REPLY_WARNING, REPLY, EXPIRED }

	public static Beat beat(long age) {
		if (age < 0 || age > TELL) return Beat.EXPIRED;
		if (age < PLANT) return Beat.PLANT;
		if (age < PLANT + BRACE) return Beat.BRACE;
		return age < TELL ? Beat.REPLY_WARNING : Beat.REPLY;
	}

	/** Replaces a due guard after the first attack, then once per four completed attacks. */
	public static boolean eligible(int discipline, int sequence, double distance, double height, double aura, long now, long readyAt) {
		return discipline == MastersRules.STONE && sequence >= 0 && sequence % 4 == 1 && now >= readyAt
			&& Double.isFinite(distance) && distance >= MIN_DISTANCE && distance <= MAX_DISTANCE
			&& Double.isFinite(height) && Math.abs(height) <= 1 && Double.isFinite(aura) && aura >= COST;
	}

	public static boolean rear(double forward) { return Double.isFinite(forward) && forward < 0; }

	public static boolean hits(double forward, double side, double height) {
		return Double.isFinite(forward) && Double.isFinite(side) && Double.isFinite(height)
			&& forward >= 0 && forward <= REACH && Math.abs(side) <= HALF_WIDTH && Math.abs(height) <= HEIGHT;
	}
}
