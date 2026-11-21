package dev.wildercord.world.sites.oathhold;

/**
 * Pure criminal law and Knight aggro rules for Oathhold.
 * Oathbound High Knights remain peaceful toward law-abiding players unless a crime is committed inside city bounds,
 * or the player engages a Knight directly.
 */
public final class OathholdLawRules {
	private OathholdLawRules() {}

	public enum CrimeKind {
		NONE(0, "none"),
		VANDALISM(8, "vandalism"),
		THEFT(12, "theft"),
		ASSAULT(20, "assault"),
		MURDER(50, "murder");

		public final int bountyEmeralds;
		public final String id;

		CrimeKind(int bountyEmeralds, String id) {
			this.bountyEmeralds = bountyEmeralds;
			this.id = id;
		}
	}

	public static final long WANTED_DURATION_TICKS = 24000L;
	public static final int MAX_BOUNTY = 250;

	public static boolean isInsideCityWalls(int originX, int originZ, int wx, int wz) {
		return Math.abs(wx - originX) <= OathholdOrigin.OUTER_WALL_R
			&& Math.abs(wz - originZ) <= OathholdOrigin.OUTER_WALL_R;
	}

	public static boolean isWanted(int bountyEmeralds, long wantedUntil, long now) {
		return bountyEmeralds > 0 && now < wantedUntil;
	}

	public static int updatedBounty(int current, CrimeKind crime) {
		if (crime == null || crime == CrimeKind.NONE) return Math.max(0, current);
		return Math.min(MAX_BOUNTY, Math.max(0, current) + crime.bountyEmeralds);
	}

	public static long updatedWantedUntil(long currentUntil, long now) {
		return Math.max(currentUntil, now + WANTED_DURATION_TICKS);
	}
}
