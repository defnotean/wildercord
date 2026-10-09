package dev.wildercord.aura;

/**
 * Echo, Dawn and Venom Breath's numbers, the pure parts: their five arts each (read by {@link ArtRules#ARTS} and the arts in
 * {@code aura.arts}), and their coated blows' passives. Echo rings: a blow struck again by its own sound a moment later, and a foe
 * left reeling. Dawn shines: foes lit up to be seen and blinded a moment, the undead burned hardest, a glint of mending. Venom bites:
 * a toxin that stacks with every bite, weakness, and a body that slips away.
 */
public final class MethodsBArtRules {
	private MethodsBArtRules() {}

	public static final String ECHO = "echo";
	public static final String DAWN = "dawn";
	public static final String VENOM = "venom";

	// ================================================================== Echo

	/** Ringing Cut (I): an arc, and the ring of it a moment later on whoever it cut. */
	public static final double RING_REACH = 3.6;
	public static final double RING_DEGREES = 110;
	public static final int RING_TARGETS = 4;
	public static final double RING_FACTOR = 0.85;
	public static final double RING_REPEAT = 0.3;
	public static final int RING_DELAY = 10;
	public static final int RING_REEL = 30;

	/** Resonant Chord (II): a cone of sound, foes in it reeling and slowed. */
	public static final double CHORD_REACH = 6.0;
	public static final double CHORD_DEGREES = 70;
	public static final int CHORD_TARGETS = 6;
	public static final double CHORD_FACTOR = 1.0;
	public static final int CHORD_REEL = 50;
	public static final int CHORD_SLOW = 30;

	/** Counterpoint (III): the blow answered: a ring of sound all round, the attacker thrown, and an echo of it after. */
	public static final double COUNTER_RADIUS = 3.5;
	public static final double COUNTER_FACTOR = 0.9;
	public static final double COUNTER_REPEAT = 0.25;
	public static final int COUNTER_DELAY = 12;
	public static final double COUNTER_THROW = 0.9;
	public static final int COUNTER_REEL = 40;

	/** Reverb Step (IV): a dash, the foes in the way cut, and each struck again by the dash's echo. */
	public static final double REVERB_DISTANCE = 7.0;
	public static final int REVERB_TICKS = 5;
	public static final double REVERB_WIDTH = 2.2;
	public static final int REVERB_TARGETS = 5;
	public static final double REVERB_FACTOR = 1.0;
	public static final double REVERB_ECHO = 0.4;
	public static final int REVERB_DELAY = 14;

	/** Grand Resonance (V): a great toll round you, and three echoes of it, each a ring further out. */
	public static final double GRAND_RADIUS = 7.0;
	public static final int GRAND_TARGETS = 10;
	public static final double GRAND_FACTOR = 1.5;
	public static final double GRAND_ECHO = 0.35;
	public static final int GRAND_ECHOES = 3;
	public static final int GRAND_PERIOD = 10;
	public static final int GRAND_REEL = 60;

	// ================================================================== Dawn

	/** First Light (I): a bright cut that lights its foes up; the undead burn under it. */
	public static final double LIGHT_REACH = 4.0;
	public static final double LIGHT_DEGREES = 100;
	public static final int LIGHT_TARGETS = 4;
	public static final double LIGHT_FACTOR = 1.0;
	public static final int LIGHT_GLOW = 100;

	/** Sunrise Arc (II): a rising arc of light ahead, its foes blinded a moment and lit. */
	public static final double SUNRISE_REACH = 5.0;
	public static final double SUNRISE_DEGREES = 140;
	public static final int SUNRISE_TARGETS = 6;
	public static final double SUNRISE_FACTOR = 1.05;
	public static final int SUNRISE_BLIND = 30;

	/** Halo Guard (III): a halo round you: you mend, the foes near blinded and thrown back. */
	public static final double HALO_RADIUS = 3.5;
	public static final double HALO_FACTOR = 0.8;
	public static final double HALO_MEND = 5.0;
	public static final int HALO_BLIND = 40;
	public static final double HALO_THROW = 0.7;

	/** Dawnbreak Rush (IV): a dash in a line of light, the foes in the way cut and lit. */
	public static final double DAWNBREAK_DISTANCE = 8.0;
	public static final int DAWNBREAK_TICKS = 5;
	public static final double DAWNBREAK_WIDTH = 2.2;
	public static final int DAWNBREAK_TARGETS = 5;
	public static final double DAWNBREAK_FACTOR = 1.15;

	/** Noon Zenith (V): the sun overhead: a pillar of light round you, every foe near struck, blinded and lit, the undead most. */
	public static final double ZENITH_RADIUS = 7.0;
	public static final int ZENITH_TARGETS = 10;
	public static final double ZENITH_FACTOR = 2.4;
	public static final int ZENITH_BLIND = 60;
	public static final int ZENITH_DELAY = 10;

	/** The undead take this much more from Dawn's light (counted in the balance model at a share of a foe). */
	public static final double UNDEAD_BANE = 1.5;

	/** What Dawn's light does to a foe: more to the undead. */
	public static double dawnFactor(double factor, boolean undead) {
		return Math.max(0, factor) * (undead ? UNDEAD_BANE : 1.0);
	}

	// ================================================================== Venom

	/** Fang Strike (I): a quick lunge's bite, its toxin stacking. */
	public static final double FANG_REACH = 4.0;
	public static final double FANG_DEGREES = 60;
	public static final int FANG_TARGETS = 2;
	public static final double FANG_FACTOR = 0.8;
	public static final double FANG_TOXIN = 0.35;

	/** Spitting Cobra (II): a spray of venom ahead, poison and weakness on everything it wets. */
	public static final double COBRA_REACH = 6.5;
	public static final double COBRA_DEGREES = 50;
	public static final int COBRA_TARGETS = 6;
	public static final double COBRA_FACTOR = 0.8;
	public static final double COBRA_TOXIN = 0.35;
	public static final int COBRA_WEAKNESS = 80;

	/** Shed Skin (III): a cut and a slither back out of reach, the husk left where you stood poisoning whoever stands in it. */
	public static final double SHED_RADIUS = 3.0;
	public static final double SHED_BACK = 3.0;
	public static final double SHED_FACTOR = 0.7;
	public static final double SHED_TOXIN = 0.3;
	public static final int SHED_TICKS = 60;
	public static final int SHED_PERIOD = 20;

	/** Serpent Slither (IV): a weaving dash, every foe in the way bitten and weakened. */
	public static final double SLITHER_DISTANCE = 6.0;
	public static final int SLITHER_TICKS = 6;
	public static final double SLITHER_WIDTH = 2.6;
	public static final int SLITHER_TARGETS = 5;
	public static final double SLITHER_FACTOR = 1.0;
	public static final double SLITHER_TOXIN = 0.4;
	public static final double SLITHER_WEAVE = 0.9;

	/** Hydra Coil (V): heads striking all round, the toxin on each foe stacked to its height and bursting. */
	public static final double HYDRA_RADIUS = 6.5;
	public static final int HYDRA_TARGETS = 10;
	public static final int HYDRA_HEADS = 5;
	public static final double HYDRA_FACTOR = 1.9;
	public static final double HYDRA_TOXIN = 0.6;
	public static final int HYDRA_PERIOD = 4;

	/** A toxin stack lasts this long, refreshed by every bite. */
	public static final int TOXIN_TICKS = 100;
	/** The most a toxin stacks (as poison's amplifier, 0 the first): a player's lower, so a bite never runs away with a fight. */
	public static final int TOXIN_CAP = 3;
	public static final int PVP_TOXIN_CAP = 1;
	/** Weakness, from Venom's arts (ticks). */
	public static final int VENOM_WEAKNESS = 60;

	/** The toxin a bite leaves: one stack higher than what's there (a new one at the first), held to its cap. */
	public static int toxin(int current, boolean poisoned, boolean player) {
		int cap = player ? PVP_TOXIN_CAP : TOXIN_CAP;
		return Math.min(cap, poisoned ? Math.max(0, current) + 1 : 0);
	}

	/** Stacks {@code times} bites in a row from none: what's left on the foe. */
	public static int toxinAfter(int times, boolean player) {
		int amp = -1;
		for (int i = 0; i < times; i++) {
			amp = toxin(amp, amp >= 0, player);
		}
		return amp;
	}

	// ================================================================== their passives (a coated blow)

	/** Echo: the chance a coated blow is struck again by its echo a moment later, and how much of it the echo carries. */
	public static double echoChance(int stage) {
		return stage <= AuraRules.NONE ? 0 : 0.06 + 0.05 * stage;
	}

	public static final double ECHO_SHARE = 0.3;
	public static final int ECHO_COAT_DELAY = 8;
	/** From Edge the echo leaves its foe reeling (nausea) for a moment. */
	public static int echoReel(int stage) {
		return stage >= AuraRules.EDGE ? 20 + 10 * (stage - AuraRules.EDGE) : 0;
	}

	/** Dawn: a coated blow lights its foe up (ticks), and on an undead foe burns a little more. */
	public static int dawnGlow(int stage) {
		return stage <= AuraRules.NONE ? 0 : 20 + 20 * stage;
	}

	public static final double DAWN_UNDEAD_SHARE = 0.25;
	/** Dawn's glint: from Flow a coated blow mends a little, at most every {@link #DAWN_REST} ticks. */
	public static double dawnGlint(int stage) {
		return stage >= AuraRules.FLOW ? 0.2 + 0.1 * (stage - AuraRules.FLOW) : 0;
	}

	public static final int DAWN_REST = 40;

	/** Venom: the chance a coated blow leaves a toxin stack; from Edge every blow does, and weakens. */
	public static double venomChance(int stage) {
		return stage >= AuraRules.EDGE ? 1.0 : stage >= AuraRules.FLOW ? 0.35 : stage > AuraRules.NONE ? 0.15 : 0;
	}

	public static int venomTicks(int stage) {
		return stage <= AuraRules.NONE ? 0 : 40 + 15 * stage;
	}
}
