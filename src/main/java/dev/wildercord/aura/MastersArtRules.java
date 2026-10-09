package dev.wildercord.aura;

/** Three server-selected combat actions. Requests contain only an id; never a target, damage or claimed movement state. */
public final class MastersArtRules {
	private MastersArtRules() {}

	public record Move(String id, int stage, double cost, int rest, int windup, int recovery, double reach, double damage, int targets) {}
	public static final Move SPELLCUT = new Move("spellcut", 3, 14, 60, 4, 12, 4, 0.65, 3);
	public static final Move RISING_BREAK = new Move("rising_break", 4, 20, 100, 8, 18, 3.5, 1.15, 3);
	public static final Move DRIVING_CUT = new Move("driving_cut", 4, 18, 80, 6, 14, 5, 1.35, 2);
	public static final int SHARED_REST = 12;
	public static final int INTERRUPT_TICKS = 20;
	public static final double BREAK_STANCE = 2.2;

	/** Invalid network ordinals are refused instead of clamped into a valid move. */
	public static Move move(int ordinal) {
		return switch (ordinal) {
			case 0 -> SPELLCUT;
			case 1 -> RISING_BREAK;
			case 2 -> DRIVING_CUT;
			default -> null;
		};
	}

	/** Pure payment gate; a corrupt/nonfinite resource never makes an action free. */
	public static boolean canPay(Move move, int stage, double aura, long now, long readyAt) {
		return move != null && stage >= move.stage() && Double.isFinite(aura) && aura >= move.cost() && now >= readyAt;
	}
	/** A full block preserves the stance; damage through a partial block or absorbed by hearts still breaks a windup. */
	public static boolean interrupts(float base, float taken, boolean blocked) {
		return Float.isFinite(base) && Float.isFinite(taken) && (taken > 0 || !blocked && base > 0);
	}

}
