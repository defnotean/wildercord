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
		DEMOLITION(35, "demolition"),
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

	public static final double GUARD_BREAK_WITNESS_RANGE = 32.0;
	public static final double GUARD_BREAK_EARSHOT_RANGE = 10.0;
	public static final double GUARD_EXPLOSION_WITNESS_RANGE = 56.0;
	public static final double GUARD_EXPLOSION_EARSHOT_RANGE = 24.0;
	public static final double ARREST_CAPTURE_RANGE = 3.8;
	public static final long PERSECUTION_DURATION_TICKS = 2400L;
	public static final long PRISON_MIN_TICKS = 300L;
	public static final long PRISON_MAX_TICKS = 900L;
	public static final int REBUILD_INTERVAL_TICKS = 8;
	public static final long UNWITNESSED_REBUILD_DELAY_TICKS = 100L;
	public static final int MAX_REPAIR_CREW = 4;

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

	public static long sentenceTicks(int damagedBlocks, boolean isExplosion) {
		long raw = PRISON_MIN_TICKS + Math.max(1, damagedBlocks) * 10L + (isExplosion ? 100L : 0L);
		return Math.clamp(raw, PRISON_MIN_TICKS, PRISON_MAX_TICKS);
	}

	public static int repairCrewSize(int damagedBlocks) {
		return Math.max(1, Math.min(MAX_REPAIR_CREW, 1 + Math.max(0, damagedBlocks) / 6));
	}

	public static int compareForRebuild(int x1, int y1, int z1, boolean solid1, int x2, int y2, int z2, boolean solid2) {
		int cmp = Integer.compare(y1, y2);
		if (cmp != 0) return cmp;
		cmp = Boolean.compare(solid2, solid1);
		if (cmp != 0) return cmp;
		cmp = Integer.compare(x1, x2);
		if (cmp != 0) return cmp;
		return Integer.compare(z1, z2);
	}

	public static boolean isInsideCell(double px, double py, double pz, int cx, int cy, int cz) {
		return Math.abs(px - (cx + 0.5)) <= 1.35
			&& py >= cy - 0.5 && py <= cy + 3.2
			&& Math.abs(pz - (cz + 0.5)) <= 1.35;
	}
}
