package dev.wildercord.cast;

/**
 * The pure rules of the signature fusions ({@link SignatureFusions}, {@link SignatureWards}), with no Minecraft
 * types so they're unit-tested. Numbers are at power 1 and duration 1 and match the rune descriptions in
 * {@code Runes}.
 */
final class SignatureRules {
	private SignatureRules() {}

	// ---- Frostwire (Chill and Shock): chilled, then a current through every cold enemy near.
	static final int FROSTWIRE_SLOW_SECONDS = 4;
	/** How long after the chill the current races through. */
	static final int FROSTWIRE_DELAY = 6;
	static final double FROSTWIRE_REACH = 6.0;
	/** Enemies one current runs through, the one it starts from among them. */
	static final int FROSTWIRE_CHAIN = 6;
	static final double FROSTWIRE_DAMAGE = 5.0;
	static final double FROSTWIRE_FROZEN = 7.0;
	/** At most this many targets of one hit start a current of their own. */
	static final int FROSTWIRE_ORIGINS = 4;

	/** The current's damage to one enemy in it: more to one frozen solid than to one only chilled. */
	static double frostwireDamage(boolean frozen) {
		return frozen ? FROSTWIRE_FROZEN : FROSTWIRE_DAMAGE;
	}

	// ---- Seethe (Bubble and Fire): a boiling bubble, then a burst of steam.
	static final int SEETHE_SECONDS = 2;
	/** A scald every half second while the bubble holds. */
	static final int SEETHE_EVERY = 10;
	static final double SEETHE_SCALD = 1.0;
	static final double SEETHE_BURST = 4.0;
	static final double SEETHE_RADIUS = 2.5;
	static final int SEETHE_BLIND_SECONDS = 2;
	static final int SEETHE_BUBBLES = 4;

	/** Scalds a bubble held for {@code ticks} deals: one every half second, never none. */
	static int seetheScalds(int ticks) {
		return Math.max(1, ticks / SEETHE_EVERY);
	}

	// ---- Bloomstep (Grow and Blink): a step through blossoms.
	static final double BLOOMSTEP_RANGE = 32.0;
	static final double BLOOMSTEP_RADIUS = 3.0;
	static final int BLOOMSTEP_REGEN_SECONDS = 5;
	/** Blocks of ground bone meal is asked of at each end of the step (grass, and whatever else it grows). */
	static final int BLOOMSTEP_BLOOMS = 3;

	// ---- Skyburst (Launch and Explode): flung up, then blown apart at the top.
	static final int SKYBURST_TARGETS = 3;
	/** How hard a target is flung up (Launch's is 1.5 at power 1). */
	static final double SKYBURST_LIFT = 1.2;
	static final double SKYBURST_DAMAGE = 7.0;
	static final double SKYBURST_RADIUS = 3.0;
	static final int SKYBURST_BURN_SECONDS = 4;
	/** The soonest a flung target may go off, and the latest (the top of its flight, or a bump on the way). */
	static final int SKYBURST_EARLIEST = 6;
	static final int SKYBURST_LATEST = 20;

	/** Whether a target flung {@code tick} ticks ago, rising at {@code rising} blocks a tick, is at the top of its flight. */
	static boolean skyburstApex(int tick, double rising) {
		return tick >= SKYBURST_LATEST || tick >= SKYBURST_EARLIEST && rising <= 0.05;
	}

	/** A blast's strength {@code distance} from its heart: full there, 30% less at its edge. */
	static double blastFalloff(double distance, double radius) {
		return 1.0 - 0.3 * Math.min(1.0, Math.max(0.0, distance) / Math.max(0.5, radius));
	}

	// ---- Stitchtime (Heal and Countdown): a heal now, and every wound of the next seconds healed back.
	static final double STITCH_HEAL = 4.0;
	static final double STITCH_CAP = 12.0;
	static final int STITCH_SECONDS = 4;

	/** What a stitch heals back when its time is up: every wound it counted, at most {@code cap}. */
	static double stitchBack(double counted, double cap) {
		return Math.max(0.0, Math.min(counted, cap));
	}

	/**
	 * When a stitch renewed at {@code now} for {@code ticks} ends: the later of its old end and the new one, but never
	 * more than three times its length after it began, so casting it over and over can't hold the count open for ever.
	 */
	static long stitchUntil(long began, long until, long now, int ticks) {
		return Math.min(began + 3L * ticks, Math.max(until, now + ticks));
	}

	// ---- Parasite (Venom and Leech): poisoned and drained, and it moves on when its host dies.
	static final int PARASITE_SECONDS = 6;
	static final double PARASITE_DRAIN = 1.0;
	static final double PARASITE_LEAP = 6.0;
	/** The caster is fed only from this close: a parasite across the world feeds nobody. */
	static final double PARASITE_FEED_RANGE = 32.0;

	/** Seconds a parasite still has after {@code drained} of its {@code seconds} (what one that leaps takes with it), at least one. */
	static int parasiteLeft(int seconds, int drained) {
		return Math.max(1, seconds - drained);
	}

	// ---- Razorgale (Windcut and Bleed): cut and left bleeding, then the wound torn open.
	static final double RAZORGALE_CUT = 2.0;
	static final double RAZORGALE_RADIUS = 3.0;
	/** How long the gale takes to come back round. */
	static final int RAZORGALE_RETURN = 10;
	static final int RAZORGALE_BLEED_TICKS = 80;

	// ---- Doomclock (Primer and Stasis): a clock every blow winds tighter, then a burst.
	static final int DOOMCLOCK_TARGETS = 3;
	static final int DOOMCLOCK_SECONDS = 3;
	static final double DOOMCLOCK_BURST = 8.0;
	static final double DOOMCLOCK_WIND = 2.0;
	static final double DOOMCLOCK_WOUND = 12.0;
	/** A blow winds the clock at most once in this many ticks (four a second). */
	static final int DOOMCLOCK_WIND_EVERY = 5;
	static final double DOOMCLOCK_RADIUS = 3.0;

	/** A clock wound {@code wound} tighter, wound once more: 2 more, never past 12. */
	static double wind(double wound) {
		return Math.min(DOOMCLOCK_WOUND, wound + DOOMCLOCK_WIND);
	}

	/** Whether a blow at {@code now} winds a clock last wound at {@code last}: four times a second at most. */
	static boolean winds(long last, long now) {
		// Never wound yet (Long.MIN_VALUE) is long enough ago: added, not subtracted, so it can't overflow.
		return now < last || now >= last + DOOMCLOCK_WIND_EVERY;
	}

	/** The burst of a clock wound {@code wound} tighter, at power 1: 8, and all it was wound. */
	static double doomclockBurst(double wound) {
		return DOOMCLOCK_BURST + Math.max(0.0, Math.min(DOOMCLOCK_WOUND, wound));
	}

	// ---- Thunderstep (Shadowstep and Lightning): down as a bolt behind the first enemy.
	static final double THUNDERSTEP_RANGE = 24.0;
	static final double THUNDERSTEP_DAMAGE = 8.0;
	static final double THUNDERSTEP_RADIUS = 2.5;
	static final int THUNDERSTEP_STUN = 10;

	// ---- Halo (Smite and Regrowth): a crown of light that smites and heals.
	static final int HALO_SECONDS = 8;
	/** The first smite comes this soon, then one every {@link #HALO_EVERY} ticks while it lasts. */
	static final int HALO_FIRST = 10;
	static final int HALO_EVERY = 30;
	static final double HALO_REACH = 6.0;
	static final double HALO_SMITE = 3.0;
	static final double HALO_UNDEAD = 3.0;
	static final double HALO_HEAL = 1.0;

	/** Smites a halo lasting {@code ticks} makes: the first a moment in, then one every 2 seconds, never none. */
	static int haloSmites(int ticks) {
		return ticks <= HALO_FIRST ? 1 : 1 + (ticks - HALO_FIRST - 1) / HALO_EVERY;
	}

	/** Whether a halo smites {@code tick} ticks after it was put on (it looks round every 10). */
	static boolean haloSmitesAt(int tick) {
		return tick >= HALO_FIRST && (tick - HALO_FIRST) % HALO_EVERY == 0;
	}

	// ---- Thunderquake (Thunderclap and Tremor): three shockwaves, each reaching further.
	static final int QUAKE_WAVES = 3;
	static final double QUAKE_STEP = 2.0;
	static final int QUAKE_EVERY = 10;
	static final double QUAKE_DAMAGE = 4.0;

	/** How far the {@code wave}th shockwave (1 = the first) reaches at radius 1: 2, 4, then 6 blocks. */
	static double quakeReach(int wave) {
		return QUAKE_STEP * Math.max(1, wave);
	}

	/** All three waves' damage at power 1 to an enemy {@code distance} from the heart: 4 for each that reaches it. */
	static double quakeTotal(double distance, double radius) {
		double total = 0;
		for (int wave = 1; wave <= QUAKE_WAVES; wave++) {
			if (distance <= quakeReach(wave) * radius) {
				total += QUAKE_DAMAGE;
			}
		}
		return total;
	}

	// ---- Cometfall (Starfall and Meteor): a comet, and its shards.
	static final int COMET_DELAY = 20;
	static final double COMET_DAMAGE = 16.0;
	static final double COMET_RADIUS = 4.0;
	/** The blast is weaker toward its edge: 40% less there. */
	static final double COMET_FALLOFF = 0.4;
	static final int COMET_BURN_SECONDS = 4;
	static final int COMET_SHARDS = 5;
	static final double COMET_SHARD_DAMAGE = 4.0;
	static final double COMET_SHARD_REACH = 10.0;

	/** The comet's blast at power 1, {@code distance} from where it falls. */
	static double cometDamage(double distance, double radius) {
		return COMET_DAMAGE * (1.0 - COMET_FALLOFF * Math.min(1.0, Math.max(0.0, distance) / Math.max(0.5, radius)));
	}

	// ---- Riposte (Reflect and Foresight): the next blows sidestepped and answered.
	static final int RIPOSTE_SECONDS = 10;
	static final int RIPOSTE_BLOWS = 2;
	static final double RIPOSTE_DAMAGE = 6.0;
	/** Only a striker this close is answered (an archer beyond it is still sidestepped). */
	static final double RIPOSTE_REACH = 24.0;

	// ---- Dust Devil (Summit Wind and Sandstorm): a wandering whirl that carries enemies off.
	static final int DEVIL_SECONDS = 5;
	static final double DEVIL_REACH = 2.0;
	/** How far it drifts toward its quarry each quarter second: 3 blocks a second. */
	static final double DEVIL_STEP = 0.75;
	static final double DEVIL_CHASE = 12.0;
	static final double DEVIL_SCOUR = 3.0;
	static final double DEVIL_FLING = 1.1;

	/** How far a devil at {@code distance} from its quarry drifts toward it in one step: all the way, or a step at most. */
	static double devilDrift(double distance) {
		return Math.max(0.0, Math.min(DEVIL_STEP, distance - 0.3));
	}

	// ---- Malison (Hex and Resonance): a curse that passes on when its bearer dies.
	static final double MALISON_DAMAGE = 3.0;
	static final int MALISON_SECONDS = 8;
	static final double MALISON_REACH = 6.0;
	static final int MALISON_SPREAD = 3;

	/** Ticks of curse a bearer dying at {@code now} passes on, cursed until {@code until}: what it had left, a second at least. */
	static int malisonLeft(long until, long now) {
		return (int) Math.max(20, Math.min(Integer.MAX_VALUE, until - now));
	}

	// ---- Avalanche (Coldsnap and Stalactite): snow and ice from above.
	static final double AVALANCHE_DAMAGE = 6.0;
	static final double AVALANCHE_BARE_HEAD = 1.5;
	static final double AVALANCHE_RADIUS = 3.0;
	static final int AVALANCHE_SLOW_SECONDS = 3;
	static final int AVALANCHE_DRIFT_SECONDS = 10;
	/** Drifts of snow one avalanche leaves at most. */
	static final int AVALANCHE_DRIFTS = 8;

	/** Avalanche's damage at power 1: half again on a bare head. */
	static double avalancheDamage(boolean bareHead) {
		return AVALANCHE_DAMAGE * (bareHead ? AVALANCHE_BARE_HEAD : 1.0);
	}
}
