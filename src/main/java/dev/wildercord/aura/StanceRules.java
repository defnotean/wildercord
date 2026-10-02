package dev.wildercord.aura;

/**
 * Stance and openings, the pure part. Every foe a swordsman fights has a stance: how much pressure it can take before its footing
 * goes. Blade and aura wear it down (a clean blow by what it was dealt at, an art more, Stone's most of all, a perfect guard a big
 * share at once); left alone it comes back. When it breaks the foe is <b>opened</b> for a moment: staggered, marked for everyone to
 * see, and the next full swing of a swordsman's blade on it is a <b>finisher</b>, a named, grand strike that deals a share of what
 * the foe has already lost and gives aura back. After a finisher (or an opening let pass) the foe stands steady a while, so nobody
 * is ever held open again and again.
 *
 * <ul>
 * <li><b>A creature's stance</b> comes from its health ({@link #pool}): at least {@link #POOL_LEAST}, so a weak foe usually dies to
 *     plain blows first and is opened only by technique (arts, a perfect guard, Stone); a sturdy one (a Runebound, a duelist, a
 *     fallen knight) a quarter more. Its stance comes back {@link #REGEN_DELAY} after the last wear, quickly.</li>
 * <li><b>A boss</b> has much more, takes less from each blow ({@link #BOSS_TOUGHNESS}), recovers sooner, is only slowed when opened
 *     (never held, as with every art), takes a small share of what it lost from a finisher ({@link #BOSS_SHARE}), stands steady long
 *     after, and grows steadier each time it's broken ({@link #BOSS_GROWTH}): breakable, never trivial.</li>
 * <li><b>A player</b> has a stance too ({@link #PLAYER_POOL}), so a duel rewards pressure and guarding: a blow takes no more than
 *     {@link #PVP_BLOW_CAP} of it, a held Aura Guard or a raised shield takes the wear itself, a perfect guard breaks into the
 *     attacker's. Opened, a player is slowed and their guard broken (shield and Aura Guard) for a moment, never held still; a
 *     finisher on them deals at most {@link #PVP_FINISHER} and a quarter of their health, through their armour and their totem as
 *     any blow is. Never a one-shot.</li>
 * </ul>
 *
 * <p>Shared by the server ({@code aura.Stance}), each client (the stance bars and the opened mark) and the unit tests. The
 * recovery is worked out on both sides from the same numbers ({@link #worn}), so a foe's stance is only sent when it changes.</p>
 */
public final class StanceRules {
	private StanceRules() {}

	// ------------------------------------------------------------------ who

	/** What kind of foe a stance belongs to. */
	public enum Kind {
		/** Anything ordinary. */
		CREATURE,
		/** A Runebound, a duelist or a fallen knight: a quarter more. */
		STURDY,
		/** A boss: much more, recovering. */
		BOSS,
		/** Another player. */
		PLAYER,
		/** A training dummy: a fixed stance, for practice. */
		DUMMY;

		public static Kind of(int ordinal) {
			Kind[] all = values();
			return ordinal >= 0 && ordinal < all.length ? all[ordinal] : CREATURE;
		}
	}

	// ------------------------------------------------------------------ how much

	/** A creature's stance: {@link #POOL_SHARE} of its health, never less than {@link #POOL_LEAST} nor more than {@link #POOL_MOST}. */
	public static final double POOL_SHARE = 0.8;
	public static final double POOL_LEAST = 20.0;
	public static final double POOL_MOST = 100.0;
	/** A sturdy foe's, times this. */
	public static final double STURDY = 1.25;
	/** A boss's: {@link #BOSS_POOL_SHARE} of its health, {@link #BOSS_POOL_LEAST} to {@link #BOSS_POOL_MOST}, more each time it's broken. */
	public static final double BOSS_POOL_SHARE = 0.3;
	public static final double BOSS_POOL_LEAST = 60.0;
	public static final double BOSS_POOL_MOST = 150.0;
	/** A boss's stance after each break: a quarter more, up to twice what it began with. */
	public static final double BOSS_GROWTH = 0.25;
	public static final double BOSS_GROWTH_MOST = 2.0;
	public static final double PLAYER_POOL = 30.0;
	public static final double DUMMY_POOL = 40.0;

	/** The stance of a foe of {@code kind} with {@code maxHealth}, broken {@code breaks} times already (a boss grows steadier). */
	public static double pool(Kind kind, double maxHealth, int breaks) {
		double h = Math.max(1, maxHealth);
		return switch (kind) {
			case CREATURE -> Math.max(POOL_LEAST, Math.min(POOL_MOST, h * POOL_SHARE));
			case STURDY -> Math.max(POOL_LEAST, Math.min(POOL_MOST, h * POOL_SHARE)) * STURDY;
			case BOSS -> Math.max(BOSS_POOL_LEAST, Math.min(BOSS_POOL_MOST, h * BOSS_POOL_SHARE))
				* Math.min(BOSS_GROWTH_MOST, 1 + BOSS_GROWTH * Math.max(0, breaks));
			case PLAYER -> PLAYER_POOL;
			case DUMMY -> DUMMY_POOL;
		};
	}

	// ------------------------------------------------------------------ what wears it

	/** What wears a stance, and how much of what it was dealt at counts. */
	public enum Source {
		/** A full swing of a swordsman's blade. */
		BLOW(1.0),
		/** A full swing that fell (a critical hit). */
		CRITICAL(1.35),
		/** A swing short of full, or one a sweep carried on to another foe. */
		GLANCE(0.35),
		/** A blow with no aura to coat it (an empty pool). */
		BARE(0.6),
		/** An art's strike: twice what it deals (more for what kind of art: see {@link #artWeight}). */
		ART(2.0),
		/** Aura off the blade that isn't an art (a slash, a spark): from afar, it wears little. */
		SLASH(0.5),
		/** A perfect guard against the foe's own blow: a share of its whole stance at once (see {@link #guardBreak}). */
		GUARD(1.0),
		/** A blow caught on a held Aura Guard or a raised shield: the guard takes the wear (another player's blow only). */
		GUARDED(0.6);

		public final double weight;

		Source(double weight) {
			this.weight = weight;
		}
	}

	/** Stone's blades wear stance hardest: each blow (not only its arts) a quarter more. */
	public static final double STONE_BLOWS = 1.25;

	/**
	 * What kind of art wears more: an art that shakes the ground (Stone's quakes) far more, one that holds a foe (a hold, a freeze, a
	 * root, a stopped moment, a shock) a little more. {@code quake} and {@code holds} come from {@code ArtRules.Art.kinds}.
	 */
	public static double artWeight(boolean quake, boolean holds) {
		return 1.0 + (quake ? 0.6 : 0) + (holds ? 0.2 : 0);
	}

	/** A boss takes this share of every wear. */
	public static final double BOSS_TOUGHNESS = 0.7;

	/**
	 * What a strike dealt at {@code amount} wears off a stance: its source's weight, the art's kind ({@code artWeight}, 1 for anything
	 * else), Stone's blades ({@code stone}), the striker's momentum ({@code momentum}, {@code MomentumRules.stanceFactor}) and the
	 * server's {@code stance_damage}; a boss less; and on a player under the PvP scale and never more than {@link #PVP_BLOW_CAP} of
	 * their stance at once.
	 */
	public static double wear(double amount, Source source, double artWeight, boolean stone, double momentum, Kind kind, double pvpScale,
			double scale) {
		if (amount <= 0 || scale <= 0) {
			return 0;
		}
		double w = amount * source.weight * Math.max(1, artWeight) * Math.max(0, momentum) * scale;
		if (stone && source != Source.ART && source != Source.GUARD) {
			w *= STONE_BLOWS;
		}
		if (kind == Kind.BOSS) {
			w *= BOSS_TOUGHNESS;
		}
		if (kind == Kind.PLAYER) {
			w = Math.min(w * Math.max(0, pvpScale), PLAYER_POOL * PVP_BLOW_CAP);
		}
		return Math.max(0, w);
	}

	/** A perfect guard breaks into the attacker's stance: this share of it at once (a boss's less). */
	public static final double GUARD_BREAK = 0.35;
	public static final double BOSS_GUARD_BREAK = 0.2;

	/** What a perfect guard against a foe of {@code kind} with a stance of {@code pool} wears off it. */
	public static double guardBreak(Kind kind, double pool, double scale) {
		return Math.max(0, pool * (kind == Kind.BOSS ? BOSS_GUARD_BREAK : GUARD_BREAK) * Math.max(0, scale));
	}

	/** The most of a player's stance one blow (or one art's strikes together) wears: a third, so it takes pressure, never a moment. */
	public static final double PVP_BLOW_CAP = 0.34;
	/** The most one art's strikes together wear off a player: half their stance. */
	public static final double PVP_ART_CAP = 0.5;

	// ------------------------------------------------------------------ coming back

	/** How long after the last wear (ticks) a stance starts to come back, and how much of it comes back a second. */
	public static int regenDelay(Kind kind) {
		return switch (kind) {
			case BOSS -> 30;
			case PLAYER -> 40;
			default -> 60;
		};
	}

	public static double regenShare(Kind kind) {
		return switch (kind) {
			case BOSS -> 0.1;
			case PLAYER -> 0.25;
			default -> 0.2;
		};
	}

	/** The default delay, for the docs. */
	public static final int REGEN_DELAY = 60;

	/**
	 * How worn a stance is at {@code now}: {@code worn} at {@code at}, coming back after {@link #regenDelay} at {@link #regenShare} of
	 * {@code pool} a second. Both sides read it this way.
	 */
	public static double worn(double worn, long at, double pool, Kind kind, long now) {
		long since = now - at - regenDelay(kind);
		if (since <= 0) {
			return Math.max(0, worn);
		}
		return Math.max(0, worn - pool * regenShare(kind) / 20.0 * since);
	}

	/** How much of a stance is left (0 to 1): 1 untouched, 0 broken. */
	public static double left(double worn, double pool) {
		return pool <= 0 ? 1 : Math.max(0, Math.min(1, 1 - worn / pool));
	}

	// ------------------------------------------------------------------ opened

	/** How long a broken foe stands opened (ticks): a creature three seconds, a boss two (slowed, never held), a player one and a half. */
	public static int openTicks(Kind kind) {
		return switch (kind) {
			case BOSS -> 40;
			case PLAYER -> 30;
			default -> 60;
		};
	}

	/** How long a foe stands steady after a finisher or an opening let pass (ticks): nothing wears its stance meanwhile. */
	public static int steadyTicks(Kind kind) {
		return switch (kind) {
			case BOSS -> 200;
			case PLAYER -> 100;
			default -> 60;
		};
	}

	/** The swing a finisher needs: a full one (vanilla's attack strength), as a clean hit. */
	public static final double FINISHER_SWING = AuraRules.FULL_SWING;

	// ------------------------------------------------------------------ the finisher

	/** A finisher deals this share of what the foe has lost (a boss's less), never more than so many of the blade's own damage. */
	public static final double SHARE = 0.35;
	public static final double BOSS_SHARE = 0.12;
	public static final double CAP = 4.0;
	public static final double BOSS_CAP = 2.5;
	/** On a player: a quarter of what they've lost, never more than {@link #PVP_FINISHER} (under the PvP scale) or a quarter of their health. */
	public static final double PLAYER_SHARE = 0.25;
	public static final double PVP_FINISHER = 8.0;
	public static final double PVP_FINISHER_HEALTH = 0.25;

	/**
	 * What a finisher adds to its blow: a share of {@code missing} health, held to a cap in the blade's damage ({@code weapon}) for a
	 * creature or a boss, and for a player to {@link #PVP_FINISHER} under the PvP scale and a quarter of {@code maxHealth}. Times the
	 * server's {@code finisher_damage}. It lands as part of the blow, so armour, a totem and every defence still have their say.
	 */
	public static double finisher(Kind kind, double missing, double maxHealth, double weapon, double pvpScale, double scale) {
		if (missing <= 0 || scale <= 0) {
			return 0;
		}
		double m = Math.max(0, missing);
		double extra = switch (kind) {
			case BOSS -> Math.min(m * BOSS_SHARE, BOSS_CAP * Math.max(1, weapon));
			case PLAYER -> Math.min(m * PLAYER_SHARE, Math.min(PVP_FINISHER * Math.max(0, pvpScale), PVP_FINISHER_HEALTH * Math.max(1, maxHealth)));
			default -> Math.min(m * SHARE, CAP * Math.max(1, weapon));
		};
		return Math.max(0, extra * scale);
	}

	/** The aura a finisher gives back: more at each stage (Glow 8 to Sovereign 16), a quarter of it on a practice target. */
	public static double finisherAura(int stage, boolean practice) {
		double a = 6.0 + 2.0 * AuraRules.clampStage(Math.max(AuraRules.GLOW, stage));
		return practice ? a * AuraRules.PRACTICE_GAIN : a;
	}

	/** Starlit gives back the most: a finisher's aura, half again. */
	public static final double STARLIT_AURA = 1.5;
}
