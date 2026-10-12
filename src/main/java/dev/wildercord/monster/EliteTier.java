package dev.wildercord.monster;

/**
 * How hard a biome elite is: the mini-bosses of the wilds, each found alone and rarely in its own biomes, with a boss bar
 * while it fights. They sit below the guardian bosses (the softest of those, the Apprentice tier, has 200 health), and no
 * two sit on the same step. Each step up has more health, more armour, and scales every one of its moves' damage up.
 *
 * <ol>
 *   <li>{@link #BRUTE}: the Sandmaw Wyrm.</li>
 *   <li>{@link #CHAMPION}: the Timbermaul Bear.</li>
 *   <li>{@link #WARLORD}.</li>
 * </ol>
 */
public enum EliteTier {
	BRUTE(90, 4, 1, 1.0),
	CHAMPION(120, 6, 2, 1.15),
	WARLORD(150, 8, 3, 1.3);

	/** Its health, armour and armour toughness. */
	public final double health, armour, toughness;
	/** What every move's damage is scaled by. */
	public final double hits;

	EliteTier(double health, double armour, double toughness, double hits) {
		this.health = health;
		this.armour = armour;
		this.toughness = toughness;
		this.hits = hits;
	}

	/** A move's damage at this tier, to the nearest half heart. */
	public float hit(double damage) {
		return (float) (Math.round(damage * hits * 2) / 2.0);
	}
}
