package dev.wildercord.aura.world;

import dev.wildercord.aura.world.MasterAnimationRules.Joint;
import dev.wildercord.aura.world.MasterAnimationRules.Pose;

/**
 * Classic (rigid rig) choreography for the Starlit, Hourglass and Crimson signatures. Each form is a short key list on
 * the executor's own clock, so every authored beat lands on the same tick as the server's strike.
 */
public final class MastersPackBAnimation {
	private MastersPackBAnimation() {}

	public static final int STARLIT_CONSTELLATION = 16, HOURGLASS_REWIND = 17, CRIMSON_FRENZY = 18;

	public static boolean owns(int attack) { return attack >= STARLIT_CONSTELLATION && attack <= CRIMSON_FRENZY; }

	private record Key(float at, Pose pose) {}

	private static final Pose[] SWEEP = MasterAnimationRules.keys(MasterAnimationRules.SWEEP);
	private static final Pose[] THRUST = MasterAnimationRules.keys(MasterAnimationRules.THRUST);
	private static final Pose[] CRESCENT = MasterAnimationRules.keys(MasterAnimationRules.CRESCENT);
	private static final Pose[] KILN = MasterAnimationRules.keys(MasterAnimationRules.KILN_RING);

	// Starlit: the blade is raised to mark the sky, then points down at each star as it bursts.
	private static final Pose MARK = CRESCENT[1];
	private static final Pose POINT = new Pose(1, new Joint(.22F, 0, .02F), new Joint(.20F, 0, 0), new Joint(-1.05F, -.04F, -.04F),
		new Joint(-.40F, .10F, -.30F), .26F, -95);
	private static final Pose HALF = POINT.toward(MARK, .45F);
	private static final Key[] STARLIT = {
		new Key(0, MARK), new Key(StarlitConstellationRules.FIRST - 4, MARK),
		new Key(StarlitConstellationRules.burst(0), POINT), new Key(StarlitConstellationRules.burst(0) + 3, HALF),
		new Key(StarlitConstellationRules.burst(1), turned(POINT, -.55F)), new Key(StarlitConstellationRules.burst(1) + 3, HALF),
		new Key(StarlitConstellationRules.burst(2), turned(POINT, .55F)), new Key(StarlitConstellationRules.burst(2) + 3, HALF),
		new Key(StarlitConstellationRules.burst(3), raised(POINT)), new Key(StarlitConstellationRules.burst(3) + 4, THRUST[2])};

	// Hourglass: one thrust, the arm drawn back along the same line as the glass turns, then the same thrust again.
	private static final Key[] HOURGLASS = {
		new Key(0, THRUST[0]), new Key(HourglassRewindRules.STRIKE - 4, THRUST[0]),
		new Key(HourglassRewindRules.STRIKE, THRUST[1]), new Key(HourglassRewindRules.STRIKE + 3, THRUST[2]),
		new Key(HourglassRewindRules.REPLAY - 6, THRUST[0]), new Key(HourglassRewindRules.REPLAY - 4, THRUST[0]),
		new Key(HourglassRewindRules.REPLAY, THRUST[1]), new Key(HourglassRewindRules.REPLAY + 3, THRUST[2])};

	// Crimson: low cut (jump), high cut (duck), lunge (sidestep), then a full turn (get clear).
	private static final Pose[] LOW = {lowered(SWEEP[0]), lowered(SWEEP[1]), lowered(SWEEP[2])};
	private static final Pose[] HIGH = {heightened(SWEEP[0]), heightened(SWEEP[1]), heightened(SWEEP[2])};
	private static final float TURN = -(float) (Math.PI * 2);
	private static final Key[] CRIMSON = crimson();

	private static Key[] crimson() {
		int[] b = CrimsonFrenzyRules.BEATS;
		return new Key[] {
			new Key(0, LOW[0]), new Key(b[0] - 6, LOW[0]), new Key(b[0], LOW[1]), new Key(b[0] + 3, LOW[2]),
			new Key(b[1] - 6, HIGH[0]), new Key(b[1], HIGH[1]), new Key(b[1] + 3, HIGH[2]),
			new Key(b[2] - 6, THRUST[0]), new Key(b[2], THRUST[1]), new Key(b[2] + 3, THRUST[2]),
			new Key(b[3] - 6, KILN[0]), new Key(b[3], spun(KILN[1])), new Key(b[3] + 3, spun(KILN[2]))};
	}

	/** Null for any attack this pack does not author; otherwise the eased pose, or NONE outside its timeline. */
	public static Pose sample(int attack, float age, int tell, int active, int recovery) {
		Key[] keys = switch (attack) {
			case STARLIT_CONSTELLATION -> STARLIT;
			case HOURGLASS_REWIND -> HOURGLASS;
			case CRIMSON_FRENZY -> CRIMSON;
			default -> null;
		};
		if (keys == null) return null;
		int end = tell + active + recovery;
		if (!Float.isFinite(age) || age < 0 || tell < 1 || tell > 80 || active < 1 || active > 10
			|| recovery < 1 || recovery > 120 || age >= end) return MasterAnimationRules.NONE;
		Pose pose = keys[keys.length - 1].pose();
		for (int i = 0; i + 1 < keys.length; i++) if (age < keys[i + 1].at()) {
			Key from = keys[i], to = keys[i + 1];
			pose = from.pose().toward(to.pose(), smooth((age - from.at()) / Math.max(1, to.at() - from.at())));
			break;
		}
		float last = keys[keys.length - 1].at();
		float weight = smooth(age / 4) * (1 - smooth((age - last) / Math.max(1, end - last)));
		return pose.weight(weight);
	}

	/** The beat ticks the classic and articulated forms both strike on, for parity tests. */
	public static float[] beats(int attack) {
		return switch (attack) {
			case STARLIT_CONSTELLATION -> new float[] {StarlitConstellationRules.burst(0), StarlitConstellationRules.burst(1),
				StarlitConstellationRules.burst(2), StarlitConstellationRules.burst(3)};
			case HOURGLASS_REWIND -> new float[] {HourglassRewindRules.STRIKE, HourglassRewindRules.REPLAY};
			case CRIMSON_FRENZY -> new float[] {CrimsonFrenzyRules.BEATS[0], CrimsonFrenzyRules.BEATS[1], CrimsonFrenzyRules.BEATS[2], CrimsonFrenzyRules.BEATS[3]};
			default -> new float[0];
		};
	}

	private static Pose turned(Pose p, float yaw) {
		return new Pose(p.weight(), new Joint(p.body().x(), p.body().y() + yaw, p.body().z()), new Joint(p.head().x(), p.head().y() + yaw * .5F, p.head().z()),
			p.sword(), p.offhand(), p.stance(), p.bladeTilt(), p.rootYaw());
	}
	private static Pose raised(Pose p) {
		return new Pose(p.weight(), new Joint(p.body().x() - .10F, p.body().y(), p.body().z()), new Joint(p.head().x() - .15F, p.head().y(), p.head().z()),
			new Joint(p.sword().x() - .35F, p.sword().y(), p.sword().z()), p.offhand(), p.stance(), -80, p.rootYaw());
	}
	private static Pose lowered(Pose p) {
		return new Pose(p.weight(), new Joint(p.body().x() + .30F, p.body().y(), p.body().z()), new Joint(p.head().x() - .20F, p.head().y(), p.head().z()),
			new Joint(p.sword().x() * .55F, p.sword().y(), p.sword().z()), p.offhand(), p.stance() + .10F, p.bladeTilt(), p.rootYaw());
	}
	private static Pose heightened(Pose p) {
		return new Pose(p.weight(), new Joint(p.body().x() - .08F, p.body().y(), p.body().z()), p.head(),
			new Joint(Math.max(-3.0F, p.sword().x() - .85F), p.sword().y(), p.sword().z()), p.offhand(), p.stance(), p.bladeTilt(), p.rootYaw());
	}
	private static Pose spun(Pose p) {
		return new Pose(p.weight(), p.body(), p.head(), p.sword(), p.offhand(), p.stance(), p.bladeTilt(), TURN);
	}

	private static float smooth(float t) {
		t = Float.isFinite(t) ? Math.max(0, Math.min(1, t)) : 0;
		return t * t * (3 - 2 * t);
	}
}
