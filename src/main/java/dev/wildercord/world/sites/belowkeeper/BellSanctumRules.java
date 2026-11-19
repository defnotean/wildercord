package dev.wildercord.world.sites.belowkeeper;

/**
 * Where the ruined belfry stands over the Bell Sanctum. The sanctum is a sealed vault; a shaft and a wayside belfry
 * are built above it only when the ground sits within reach of the vault roof, so deep or mountaintop vaults stay as
 * they were. Pure arithmetic, kept apart from the piece so it can be tested without a world.
 */
public final class BellSanctumRules {
	private BellSanctumRules() {}

	/** Blocks of the belfry above the ground: walls, broken cornice, bell beam and lantern posts. */
	public static final int ABOVE = 8;
	/** The piece's height without a tower: floor at DECK, vaulted roof at 11. */
	public static final int ROOM = 12;
	/** The shaft is dug only when the ground's local height sits between these, measured with the piece's own frame. */
	static final int MIN_SURFACE = 14, MAX_SURFACE = 52;

	/**
	 * The piece height that carries a shaft and belfry from a vault placed at {@code vaultY} up to a ground of
	 * {@code surface} (world heights), or {@link #ROOM} when the ground is too near or too far for a shaft.
	 */
	public static int towerHeight(int surface, int vaultY) {
		int ground = surface - vaultY + 2; // the ground in piece-local y: local 0 is DECK below the vault floor
		if (ground < MIN_SURFACE || ground > MAX_SURFACE) return ROOM;
		return ground + ABOVE;
	}
}
