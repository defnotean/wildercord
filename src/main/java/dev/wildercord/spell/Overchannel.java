package dev.wildercord.spell;

/**
 * Casting as a performance: the pure rules of holding a charged spell past full (overchannel), of
 * letting it go on the beat, and of steadying it by tracing its glyph. {@code cast.Charging} runs
 * them; the HUD and the charging circle read the same numbers, so what you see is what you get.
 *
 * <p>The shape of a charge, in ticks after it began:</p>
 * <pre>
 *   0 ........ full ──24── I ──24── II ──24── III ──60── the channel tears loose
 *              beat        beat     beat      beat
 * </pre>
 * <ul>
 *   <li><b>Stages.</b> Held past full, the circle takes more mana and climbs a stage every
 *       {@link #STAGE_TICKS}, up to the stages the caster's heart can hold ({@link #stagesFor}).
 *       Each stage adds power and a chance the spell surges into wild magic when it's let go.</li>
 *   <li><b>Mana.</b> From stage I on, the channel drains a share of the spell's price every tick,
 *       and never touches the mana the spell itself needs: short of a surplus it simply stops
 *       climbing ({@link #fed}). A Heart Circle is never cracked to feed it.</li>
 *   <li><b>The beat.</b> The moment a charge fills, and the moment each stage lands, is a beat:
 *       let go within {@link #BEAT_WINDOW} ticks of it for {@code beatBonus} more power.</li>
 *   <li><b>Tearing loose.</b> Held {@link #GRACE} ticks past the last stage the heart can hold,
 *       the channel backfires: the spell fizzles, a harmless wild surge goes off, and the caster
 *       is dazed and loses some mana ({@link #backfire}). It never harms them.</li>
 *   <li><b>Tracing.</b> A traced glyph's accuracy (0 to 1, see {@link TraceGlyph}) steadies the
 *       channel ({@link #surgeChance}) and adds a little power ({@link #traceBonus}); the server
 *       only believes as much of it as the time the caster really spent steadying allows
 *       ({@link #validTrace}). Not tracing loses nothing.</li>
 * </ul>
 */
public final class Overchannel {
	private Overchannel() {}

	/** Ticks between stages: a little over a second, so each one can be felt coming. */
	public static final int STAGE_TICKS = 24;
	/** The most stages any heart can hold. */
	public static final int MAX_STAGES = 3;
	/**
	 * Ticks after the last stage the heart can hold before the channel tears loose: three seconds, so a player
	 * still aiming (a young heart reaches its one stage soon after full) has time to see the warning and let go.
	 */
	public static final int GRACE = 60;
	/**
	 * Ticks after a beat in which a release still counts as on it. Only after: the release travels to
	 * the server, so a press made right on the beat arrives a tick or two late, never early.
	 */
	public static final int BEAT_WINDOW = 6;
	/** How much of the overchannel's surge chance a perfectly traced glyph takes away. */
	public static final double STEADYING = 0.6;
	/** Ticks of holding sneak (steadying) before a traced glyph counts at all. */
	public static final int MIN_STEADY = 10;
	/** Ticks of steadying a glyph needs before its full accuracy can count. */
	public static final int FULL_STEADY = 20;
	/** The longest daze a backfire gives, whatever a server sets (the cast lock's own cap). */
	public static final int MAX_STUN = 40;

	/**
	 * A server's tuning (the {@code channeling} section of its config).
	 *
	 * @param enabled        whether charges can overchannel at all (off: a full charge just waits, as it always did)
	 * @param powerPerStage  power each stage adds (0.2: +20%, +40%, +60%)
	 * @param drainPerSecond the share of the spell's mana price drained each second from stage I on
	 * @param surgePerStage  the wild surge chance each stage adds
	 * @param beatBonus      power added by a release on the beat
	 * @param stunTicks      how long a backfire dazes the caster
	 * @param manaBurn       the share of full mana a backfire burns away
	 * @param tracing        whether traced glyphs count
	 * @param tracePower     the power a perfectly traced glyph adds
	 */
	public record Tuning(boolean enabled, double powerPerStage, double drainPerSecond, double surgePerStage, double beatBonus, int stunTicks,
			double manaBurn, boolean tracing, double tracePower) {
		public static final Tuning DEFAULTS = new Tuning(true, 0.2, 0.15, 0.07, 0.1, 30, 0.3, true, 0.08);
	}

	// ------------------------------------------------------------------ stages

	/**
	 * The stages a heart can hold: one with fewer than two working Heart Circles, two with two or three,
	 * three from the fourth. A cracked circle doesn't count, so overcasting costs overchannel too.
	 */
	public static int stagesFor(int activeCircles, boolean enabled) {
		if (!enabled) {
			return 0;
		}
		return Math.max(1, Math.min(MAX_STAGES, 1 + Math.max(0, activeCircles) / 2));
	}

	/** Whether the channel at {@code stage} may climb to the next one, {@code sinceStage} ticks after reaching it. */
	public static boolean due(int stage, int stages, long sinceStage) {
		return stage < stages && sinceStage >= STAGE_TICKS;
	}

	/** Whether an overheld channel at {@code stage} tears loose {@code sinceStage} ticks after reaching it. */
	public static boolean tears(int stage, int stages, long sinceStage) {
		return stages > 0 && stage >= stages && sinceStage >= GRACE;
	}

	/**
	 * The game time of the beat a charge is on now: the moment it filled while it hasn't climbed, else
	 * the moment it reached its stage.
	 */
	public static long beatTime(long start, int full, int stage, long stageTime) {
		return stage <= 0 ? start + full : stageTime;
	}

	/** Whether a release at {@code now} falls on the beat at {@code beat} (and the charge can overchannel at all). */
	public static boolean onBeat(long now, long beat, int stages) {
		return stages > 0 && now >= beat && now - beat <= BEAT_WINDOW;
	}

	/**
	 * When the next beat comes, as a game time, for the HUD's ring: the charge filling, the next stage, or
	 * (at the last stage) the moment the channel tears loose. {@code Long.MIN_VALUE} when none comes.
	 */
	public static long nextBeat(long start, int full, int stages, int stage, long stageTime, long now) {
		if (stages <= 0) {
			return Long.MIN_VALUE;
		}
		long filled = start + full;
		if (now < filled) {
			return filled;
		}
		long since = stage <= 0 ? filled : stageTime;
		return since + (stage >= stages ? GRACE : STAGE_TICKS);
	}

	// ------------------------------------------------------------------ what it gives

	/** The power multiplier of a release at {@code stage}. */
	public static double stagePower(int stage, double perStage) {
		return 1 + Math.max(0, perStage) * Math.max(0, Math.min(MAX_STAGES, stage));
	}

	/** The power a traced glyph of {@code accuracy} adds (0 to {@code max}): nothing below a third, all of it at full. */
	public static double traceBonus(double accuracy, double max) {
		double a = clamp01(accuracy);
		return Math.max(0, max) * clamp01((a - 1.0 / 3) / (2.0 / 3));
	}

	/** The whole power multiplier a release gets from how it was performed. */
	public static double power(int stage, boolean onBeat, double accuracy, Tuning tuning) {
		double beat = onBeat ? 1 + Math.max(0, tuning.beatBonus()) : 1;
		double trace = tuning.tracing() ? 1 + traceBonus(accuracy, tuning.tracePower()) : 1;
		return stagePower(stage, tuning.powerPerStage()) * beat * trace;
	}

	/** The chance a release at {@code stage} surges into wild magic, steadied by a glyph traced to {@code accuracy}. */
	public static double surgeChance(int stage, double perStage, double accuracy) {
		double raw = Math.max(0, Math.min(MAX_STAGES, stage)) * Math.max(0, perStage);
		return clamp01(raw * (1 - STEADYING * clamp01(accuracy)));
	}

	/** Two chances of a surge rolled as one (an overcast that was also overchanneled). */
	public static double combined(double a, double b) {
		return 1 - (1 - clamp01(a)) * (1 - clamp01(b));
	}

	// ------------------------------------------------------------------ mana

	/** Mana one tick of the channel drains: {@code perSecond} of the spell's price a second, at least a twentieth of a mana. */
	public static double drainPerTick(double cost, double perSecond) {
		return Math.max(0.05, Math.max(0, cost) * Math.max(0, perSecond) / 20.0);
	}

	/** Whether {@code mana} can feed one more tick of the channel and still pay the spell's own {@code cost}. */
	public static boolean fed(double mana, double cost, double drain) {
		return mana - drain >= Math.max(0, cost) - 1e-6;
	}

	// ------------------------------------------------------------------ tracing

	/**
	 * How much of a reported accuracy the server believes: clamped to 0-1, nothing unless tracing is
	 * allowed and the caster steadied (held sneak while charging) for {@link #MIN_STEADY} ticks, and no
	 * more than {@code steadyTicks / FULL_STEADY}: a glyph can't be traced faster than a hand can move.
	 */
	public static double validTrace(double reported, int steadyTicks, boolean allowed) {
		if (!allowed || Double.isNaN(reported) || steadyTicks < MIN_STEADY) {
			return 0;
		}
		return Math.min(clamp01(reported), Math.min(1.0, steadyTicks / (double) FULL_STEADY));
	}

	// ------------------------------------------------------------------ tearing loose

	/**
	 * What a backfire does: how much mana is left (a share of full mana burnt away, never below none) and
	 * how long the caster is dazed. It deals no damage: tearing loose can never kill.
	 */
	public record Backfire(float manaAfter, int stunTicks, float damage) {}

	public static Backfire backfire(float mana, float maxMana, boolean creative, Tuning tuning) {
		float have = Math.max(0, mana);
		float burn = creative ? 0 : (float) (Math.max(0, maxMana) * clamp01(tuning.manaBurn()));
		int stun = Math.max(0, Math.min(MAX_STUN, tuning.stunTicks()));
		return new Backfire(Math.max(0, have - burn), stun, 0);
	}

	private static double clamp01(double v) {
		return Double.isNaN(v) ? 0 : Math.max(0, Math.min(1, v));
	}
}
