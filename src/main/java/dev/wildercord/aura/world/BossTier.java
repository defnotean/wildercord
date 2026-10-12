package dev.wildercord.aura.world;

/**
 * How hard a guardian boss is, so no two of them sit on the same level. Each step up has more health, more armour, a
 * harder plain hit, scales every one of its moves' damage up, and moves a little quicker. The Sword Master sits above
 * them all (see {@link MastersRules}).
 *
 * <ol>
 *   <li>{@link #APPRENTICE}: the Rootcoil Serpent, a first guardian to learn the dodge on.</li>
 *   <li>{@link #JOURNEY}: the Lantern-Bearer.</li>
 *   <li>{@link #SEASONED}: the Hollow Matron.</li>
 *   <li>{@link #VETERAN}: the Ashen Stag.</li>
 *   <li>{@link #ELDER}: the Frost Wyrmling.</li>
 *   <li>{@link #DREAD}: the Drowned Bell-Warden, the hardest of the six.</li>
 * </ol>
 */
public enum BossTier {
	APPRENTICE(200, 4, 2, 7, 0.6, 0.9),
	JOURNEY(250, 6, 3, 9, 0.75, 0.95),
	SEASONED(300, 8, 4, 11, 0.9, 1.0),
	VETERAN(350, 10, 5, 13, 1.0, 1.05),
	ELDER(410, 12, 6, 15, 1.1, 1.1),
	DREAD(470, 14, 8, 17, 1.2, 1.1);

	/** Its health, armour, armour toughness and plain melee hit. */
	public final double health, armour, toughness, melee;
	/** What every move's damage is scaled by, and its walking speed. */
	public final double hits, pace;

	BossTier(double health, double armour, double toughness, double melee, double hits, double pace) {
		this.health = health;
		this.armour = armour;
		this.toughness = toughness;
		this.melee = melee;
		this.hits = hits;
		this.pace = pace;
	}

	/** A move's damage at this tier, to the nearest half heart. */
	public double hit(double damage) {
		return Math.round(damage * hits * 2) / 2.0;
	}

	/** A walking speed at this tier. */
	public double speed(double speed) {
		return speed * pace;
	}
}
