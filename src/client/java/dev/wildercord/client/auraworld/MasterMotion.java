package dev.wildercord.client.auraworld;

import dev.wildercord.aura.world.AuraFighter;
import dev.wildercord.aura.world.MasterAnimationRules;
import dev.wildercord.aura.world.MasterTechniques;
import dev.wildercord.aura.world.SwordMaster;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.jspecify.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Picks the Sword Master's rig pose each frame from what the server syncs: its idle breathing and its walk (matched to the
 * ground it covers), each attack's clip stretched so its strike lands on the server's hit tick, the follow-through played
 * out at its own pace once the server is done with it, and the sidestep as a vanish: it leaves an afterimage where it
 * stood, is gone for the step, and a counter-cut straight after comes out of the vanish clip's draw.
 */
public final class MasterMotion {
	public static final RenderStateDataKey<Frame> RIG = RenderStateDataKey.create(() -> "wildercord:master_rig");

	/** The pose to draw, its sword hand, how much of it is there (0 mid-vanish), and the afterimage it left, if any. */
	public record Frame(MasterRig.Pose pose, boolean left, float alpha, @Nullable Ghost ghost) {}

	/** An afterimage: the pose it vanished from, where in the world, facing which way, and how much of it is left. */
	public record Ghost(MasterRig.Pose pose, double x, double y, double z, float bodyRot, float alpha) {}

	private static final String[] CYCLE = {"sweep", "crescent", "thrust", "break_cast"};
	/** Ticks an afterimage lingers. */
	private static final float GHOST_TICKS = 14;
	/** How long a counter may come after the step and still be the vanish's cut. */
	private static final int COUNTER_WINDOW = 5;
	/** Vanilla's limb swing goes once round every 2π / 0.6662 of walk position: one full stride of the walk clip. */
	private static final float STRIDE = 9.4312F;
	private static final Map<SwordMaster, Track> TRACKS = new WeakHashMap<>();
	/** Ticks the wind-up freezes on its peak before the cut, so the beat reads. */
	private static final int COMMIT = 2;
	/** Ticks the blade stays on the target after it lands (the hit-stop). */
	private static final int HIT_STOP = 2;
	/** Ticks the follow-through's overshoot and settle run after the blow lands. */
	private static final float SETTLE_TICKS = 16;

	/**
	 * Per school mannerisms: idle roll (radians), its period in ticks, fast tremor (radians), forward lean while winding up,
	 * head cock (radians) and weight shift (rig pixels). Rig axes: +x rotation leans a bone back, -x leans it forward.
	 */
	private static final float[] DEFAULT_STYLE = {.020F, 60, .010F, .10F, .05F, .4F};
	private static final Map<String, float[]> STYLES = Map.ofEntries(
		Map.entry("ember", new float[]{.018F, 46, .035F, .14F, .04F, .5F}),
		Map.entry("gale", new float[]{.040F, 38, .008F, .26F, .09F, .8F}),
		Map.entry("stone", new float[]{.008F, 90, 0, -.10F, 0, .25F}),
		Map.entry("rime", new float[]{.006F, 120, 0, .02F, .12F, .1F}),
		Map.entry("thunder", new float[]{.015F, 30, .050F, .12F, .06F, .4F}),
		Map.entry("verdant", new float[]{.045F, 70, .004F, .06F, .08F, .7F}),
		Map.entry("hollow", new float[]{.030F, 85, .020F, .04F, .16F, .3F}),
		Map.entry("starlit", new float[]{.030F, 100, .003F, .08F, .10F, .5F}),
		Map.entry("hourglass", new float[]{.006F, 60, 0, 0, .03F, .2F}),
		Map.entry("crimson", new float[]{.025F, 40, .040F, .22F, .05F, .6F}),
		Map.entry("tide", new float[]{.050F, 80, 0, .10F, .06F, .9F}),
		Map.entry("iron", new float[]{.004F, 100, 0, -.06F, 0, .15F}),
		Map.entry("dune", new float[]{.030F, 66, .006F, .08F, .10F, .6F}),
		Map.entry("echo", new float[]{.020F, 56, .020F, .10F, .07F, .5F}),
		Map.entry("dawn", new float[]{.015F, 75, 0, .02F, .05F, .35F}),
		Map.entry("venom", new float[]{.035F, 44, .015F, .20F, .11F, .5F}));

	/** The loose parts that trail the body: the bone, its spring stiffness, how many ticks of motion it lags, and its limit. */
	private static final String[] LOOSE = {"ponytail", "band_tails", "coat_l", "coat_r", "ribbons", "sword"};
	private static final float[] LOOSE_K = {.11F, .15F, .08F, .08F, .20F, .30F};
	private static final float[] LOOSE_LAG = {3F, 4F, 2.5F, 2.5F, 5F, 1.4F};
	private static final float[] LOOSE_MAX = {.7F, .7F, .5F, .5F, .8F, .35F};

	/** Bone indices the overlays need, looked up once per rig; -1 where the rig lacks one. */
	private static final class Bones {
		final int hips, chest, armR, foreR, legR, legL, shinR, shinL, coatL, coatR;
		final int[] loose = new int[LOOSE.length];

		Bones(MasterRig rig) {
			hips = idx(rig, "hips");
			chest = idx(rig, "chest");
			armR = idx(rig, "arm_r");
			foreR = idx(rig, "forearm_r");
			legR = idx(rig, "leg_r");
			legL = idx(rig, "leg_l");
			shinR = idx(rig, "shin_r");
			shinL = idx(rig, "shin_l");
			coatL = idx(rig, "coat_l");
			coatR = idx(rig, "coat_r");
			for (int i = 0; i < LOOSE.length; i++) loose[i] = idx(rig, LOOSE[i]);
		}

		private static int idx(MasterRig rig, String name) {
			try {
				return rig.index(name);
			} catch (IllegalStateException missing) {
				return -1;
			}
		}
	}

	private static MasterRig bonesRig;
	private static Bones bonesCache;

	private static Bones bones(MasterRig rig) {
		if (bonesRig != rig || bonesCache == null) {
			bonesCache = new Bones(rig);
			bonesRig = rig;
		}
		return bonesCache;
	}

	private MasterMotion() {}

	/** Whether the rig draws masters: on unless {@code -Dwildercord.masterRig=false}, and off under the native-model game tests. */
	public static boolean enabled() {
		String flag = System.getProperty("wildercord.masterRig");
		if (flag != null) return Boolean.parseBoolean(flag);
		return !FabricLoader.getInstance().isModLoaded("wildercord-gametest") && MasterRig.get() != null;
	}

	private static final class Track {
		boolean dashing;
		long dashStart = Long.MIN_VALUE / 2, dashEnd = Long.MIN_VALUE / 2;
		MasterRig.@Nullable Pose last, fadeFrom, ghost;
		double gx, gy, gz;
		float gyaw;
		double fadeStart;
		float fadeLen;
		String key = "";
		MasterRig.@Nullable Clip tail;
		float tailFrom;
		double tailStart;
		MasterRig.@Nullable Clip clip;
		float clipTime;
		/** The clip whose blow landed and the tick it did: the follow-through's overshoot runs from there. */
		MasterRig.@Nullable Clip overClip;
		double overStart;
		/** Per overshoot bone: the angle the clip turns it by near the blow, then the axis (4 floats each). */
		MasterRig.@Nullable Clip velClip;
		float[] velocity = new float[OVER_BONES * 4];
		/** Hurt flinch: whether the vanilla red flash was up last frame, and when it last came on. */
		boolean red;
		double hurtAt = Long.MIN_VALUE / 2;
		/** Loose parts: each one's swing angle and speed about x, y, z, and the drivers' last orientation. */
		final float[] looseAngle = new float[LOOSE.length * 3], looseSpeed = new float[LOOSE.length * 3];
		final float[] driverQ = {0, 0, 0, 1, 0, 0, 0, 1};
		boolean haveDrivers;
		double lagTime = Double.NaN;
		float lastYaw;
	}

	/** Bones the overshoot carries past the blow, with how many of the clip's measured steps they run on and the most they may. */
	private static final int OVER_BONES = 5;
	private static final float[] OVER_GAIN = {1.5F, 1.5F, .8F, .8F, .6F};
	private static final float[] OVER_CAP = {.6F, .6F, .3F, .3F, .2F};

	public static void extract(SwordMaster master, AuraFighterRenderState state, float partial) {
		state.setData(RIG, null);
		MasterRig rig = MasterRig.get();
		if (rig == null || !enabled() || state.sit + state.yield + state.bow > 0.01F) return;
		Track track = TRACKS.computeIfAbsent(master, m -> new Track());
		long tick = master.level().getGameTime();
		double now = tick + partial;

		boolean dash = master.state(AuraFighter.DASH);
		if (dash && !track.dashing) {
			track.dashStart = tick;
			if (track.last != null) {
				track.ghost = track.last.copy();
				track.gx = state.x;
				track.gy = state.y;
				track.gz = state.z;
				track.gyaw = state.bodyRot;
			}
		}
		if (!dash && track.dashing) track.dashEnd = tick;
		track.dashing = dash;

		// Locomotion: idle breathing, crossed into the walk as it covers ground.
		MasterRig.Pose loco = rig.sample(rig.clip("idle"), state.ageInTicks / 20F, rig.newPose());
		float moving = Mth.clamp(state.walkAnimationSpeed * 2F, 0, 1);
		if (moving > 0) {
			MasterRig.Clip walk = rig.clip("walk");
			MasterRig.Pose step = rig.sample(walk, state.walkAnimationPos / STRIDE * walk.length(), rig.newPose());
			MasterRig.blend(loco, step, moving, loco);
		}

		MasterRig.Pose pose = loco;
		String key = "loco";
		float fadeLen = 4;
		boolean stagger = master.state(AuraFighter.STAGGER);
		boolean dying = state.deathTime > 0;
		int code = stagger || dying ? 0 : master.attackAnimation();
		// Wind-up strength (0 to 1, held through the freeze, dropped just after the cut) and ticks since the blow landed.
		float amt = 0;
		float post = -1;
		if (code != 0) {
			float elapsed = master.attackElapsed(partial);
			long begin = tick - (long) Math.floor(master.attackElapsed(0));
			MasterRig.Clip clip;
			float time;
			int strike = 0;
			int tellN, activeN, recoveryN, from = 0;
			if (code == MasterAnimationRules.TECHNIQUE && master.technique() != 0) {
				MasterTechniques.Technique technique = MasterTechniques.byId(master.technique());
				int last = technique.strikes().size() - 1;
				while (strike < last && elapsed >= technique.impact(strike) + 1) strike++;
				clip = rig.clip(CYCLE[strike % CYCLE.length]);
				from = strike == 0 ? 0 : technique.impact(strike - 1) + 1;
				float start = strike == 0 ? 0 : clip.strike() * 0.45F;
				tellN = technique.impact(strike) - from;
				activeN = 1;
				recoveryN = strike == last ? technique.recovery() - 1 : 0;
				time = warp(clip, elapsed - from, tellN, activeN, recoveryN, start);
			} else {
				boolean counter = code == MasterAnimationRules.SWEEP && begin - track.dashEnd >= -1 && begin - track.dashEnd <= COUNTER_WINDOW;
				clip = rig.clip(counter ? "vanish" : clipFor(code));
				tellN = master.attackTellTicks();
				activeN = master.attackActiveTicks();
				recoveryN = master.attackRecoveryTicks();
				time = warp(clip, elapsed, tellN, activeN, recoveryN, counter ? clip.strike() * 0.65F : 0);
			}
			{
				float local = elapsed - from;
				post = recoveryN > 0 ? local - tellN - activeN : -1;
				float ramp = strike == 0 || code != MasterAnimationRules.TECHNIQUE ? 1 : 0.4F;
				amt = (local < tellN ? smooth(Math.min(1, local / Math.max(1, tellN * 0.8F)))
					: Math.max(0, 1 - (local - tellN) / 3F)) * ramp;
			}
			if (post >= 0) {
				track.overClip = clip;
				track.overStart = now - post;
			} else {
				track.overClip = null;
			}
			pose = rig.sample(clip, time, rig.newPose());
			key = "attack:" + code + ":" + begin + ":" + strike;
			fadeLen = strike == 0 ? 2.5F : 3;
			track.clip = clip;
			track.clipTime = time;
			track.tail = null;
		} else {
			if (track.clip != null && !stagger) {
				// The server is done; the follow-through still plays out, at the clip's own pace.
				track.tail = track.clip;
				track.tailFrom = track.clipTime;
				track.tailStart = now;
			}
			track.clip = null;
			if (stagger || dying) {
				track.tail = null;
				track.overClip = null;
			}
			if (track.tail != null) {
				float time = track.tailFrom + (float) (now - track.tailStart) / 20F;
				float left = track.tail.length() - time;
				if (left <= 0) {
					track.tail = null;
				} else {
					float weight = Mth.clamp(left / 0.45F, 0, 1) * (1 - 0.85F * moving);
					MasterRig.blend(loco, rig.sample(track.tail, time, rig.newPose()), weight, pose);
				}
			}
		}

		if (stagger) {
			// Rocked back on its heels.
			pose.preRotate(rig.torso, new Quaternionf().rotateX(0.35F * state.stagger));
			pose.preRotate(rig.head, new Quaternionf().rotateX(0.25F * state.stagger));
			key = "stagger";
		}
		if (dying) key = "death";
		polish(rig, track, state, pose, now, amt, post, moving, code != 0, stagger, dying);

		if (!key.equals(track.key)) {
			track.fadeFrom = track.last;
			track.fadeStart = now;
			track.fadeLen = fadeLen;
			track.key = key;
		}
		if (track.fadeFrom != null) {
			float w = (float) ((now - track.fadeStart) / track.fadeLen);
			if (w >= 1) track.fadeFrom = null;
			else MasterRig.blend(track.fadeFrom, pose, smooth(w), pose);
		}
		track.last = pose.copy();

		// It looks where its head turns, less so mid-cut, where the clip owns the head.
		boolean left = master.getMainArm() == HumanoidArm.LEFT;
		float look = dying ? 0 : code != 0 ? 0.35F : 0.8F;
		float yaw = Mth.clamp(state.yRot, -60, 60) * look * Mth.DEG_TO_RAD * (left ? -1 : 1);
		float pitch = Mth.clamp(state.xRot, -45, 45) * look * Mth.DEG_TO_RAD;
		MasterRig.Pose drawn = pose.copy();
		drawn.preRotate(rig.head, new Quaternionf().rotateY(yaw).rotateX(-pitch));
		lag(rig, track, pose, drawn, now, state, left, moving);
		boolean out = state.drawn > 0.5F;
		drawn.scale(rig.sword, out ? 1 : 0);
		drawn.scale(rig.sheathHilt, out ? 0 : 1);

		float alpha = dash ? 0.12F : Mth.clamp((float) (now - track.dashEnd) / 2F, 0.12F, 1);
		Ghost ghost = null;
		float since = (float) (now - track.dashStart);
		if (track.ghost != null && since < GHOST_TICKS) {
			MasterRig.Pose image = track.ghost.copy();
			image.scale(rig.sword, out ? 1 : 0);
			image.scale(rig.sheathHilt, out ? 0 : 1);
			ghost = new Ghost(image, track.gx, track.gy, track.gz, track.gyaw, 0.55F * (1 - since / GHOST_TICKS));
		} else {
			track.ghost = null;
		}
		state.setData(RIG, new Frame(drawn, left, alpha, ghost));
	}

	private static final float TAU = (float) (Math.PI * 2);

	private static void turn(MasterRig.Pose pose, int bone, float x, float y, float z) {
		if (bone < 0 || x == 0 && y == 0 && z == 0) return;
		pose.preRotate(bone, new Quaternionf().rotateXYZ(x, y, z));
	}

	private static void shift(MasterRig.Pose pose, int bone, float x, float y, float z) {
		if (bone < 0) return;
		pose.p[bone * 3] += x;
		pose.p[bone * 3 + 1] += y;
		pose.p[bone * 3 + 2] += z;
	}

	/**
	 * Everything the clips do not author: the school's idle mannerisms with offset periods, the wind-up's lean, tremor and
	 * weight shift, the hit-stop judder and the follow-through that overshoots the blow and settles, the hurt flinch, and the
	 * staged death. All are additive turns after the clip, so they cross-fade with it.
	 */
	private static void polish(MasterRig rig, Track track, AuraFighterRenderState state, MasterRig.Pose pose, double now, float amt,
		float post, float moving, boolean attacking, boolean stagger, boolean dying) {
		Bones b = bones(rig);
		float[] style = STYLES.getOrDefault(state.method, DEFAULT_STYLE);
		float t = (float) now;

		// Idle: every layer on its own period so the loop never visibly repeats. Weight drifts hip to hip, the chest breathes,
		// and the head wanders on a third and fourth rhythm.
		float calm = dying ? 0 : (1 - moving) * (attacking ? 0.4F : 1);
		float roll = style[0] * Mth.sin(t * TAU / style[1]) * calm;
		turn(pose, rig.torso, 0, 0, roll);
		turn(pose, b.hips, 0, 0, -roll * 0.6F);
		shift(pose, b.hips, style[5] * 0.5F * Mth.sin(t * TAU / (style[1] * 1.37F)) * calm, 0, 0);
		turn(pose, b.chest, 0.016F * Mth.sin(t * 0.115F) * calm, 0, 0);
		turn(pose, rig.head, -0.012F * Mth.sin(t * 0.15F + 1) * calm, 0.025F * Mth.sin(t * 0.083F + 2) * calm,
			style[4] * 0.3F * Mth.sin(t * TAU / (style[1] * 0.8F)) * calm);

		// Wind-up: the school's own tell. A lean (back for the planted schools), a tremor, the head cocked, the weight loaded
		// onto one hip and the hips dipped, all held while the clip freezes on its peak.
		if (amt > 0 && !dying) {
			turn(pose, rig.torso, -style[3] * amt, 0, 0);
			float tremor = style[2] * amt * Mth.sin(t * 2.3F);
			turn(pose, rig.torso, 0, tremor * 0.5F, tremor);
			turn(pose, rig.head, 0, 0, style[4] * amt);
			shift(pose, b.hips, style[5] * amt, -0.8F * amt, 0);
		}

		// The blow landing: the blade stays on the target for the hit-stop with a judder through the body, then the arm and
		// body run on past it and settle, extrapolated from how fast the clip itself was moving into the impact.
		if (post >= 0 && post < SETTLE_TICKS && track.overClip != null && !dying) {
			if (post < HIT_STOP + 1) turn(pose, rig.torso, 0, 0, Mth.sin(t * 5F) * 0.03F * (1 - post / (HIT_STOP + 1)));
			overshoot(rig, b, track, pose, post);
		}

		// Hurt: a short snap away from the blow that rebounds a little, on the vanilla red flash's rising edge.
		if (state.hasRedOverlay && !track.red) track.hurtAt = now;
		track.red = state.hasRedOverlay;
		float age = (float) (now - track.hurtAt);
		if (!dying && age >= 0 && age < 10) {
			float k = Math.min(1, age / 0.8F) * (float) Math.exp(-0.35F * age) * Mth.cos(0.5F * age) * (stagger ? 0.6F : 1);
			turn(pose, rig.torso, 0.26F * k, 0, 0.06F * k);
			turn(pose, rig.head, 0.34F * k, 0, -0.12F * k);
			turn(pose, b.armR, 0.2F * k, 0, 0);
			shift(pose, b.hips, 0, -0.6F * k, 0.8F * k);
		}

		if (dying) death(rig, b, pose, state.deathTime);
	}

	/**
	 * Staged death: a recoil (the head and chest thrown back), the knees folding one after the other, the body slumping
	 * forward as the vanilla fall begins to carry it over, then a small settle once it is down.
	 */
	private static void death(MasterRig rig, Bones b, MasterRig.Pose pose, float d) {
		float recoil = smooth(Mth.clamp(d / 3F, 0, 1));
		float foldR = smooth(Mth.clamp((d - 3F) / 8F, 0, 1)), foldL = smooth(Mth.clamp((d - 5F) / 8F, 0, 1));
		float slump = smooth(Mth.clamp((d - 8F) / 8F, 0, 1));
		turn(pose, rig.torso, 0.5F * recoil * (1 - foldR) - 0.55F * slump, 0, 0);
		turn(pose, b.chest, 0.15F * recoil * (1 - slump) - 0.2F * slump, 0, 0);
		turn(pose, rig.head, 0.4F * recoil * (1 - slump) - 0.5F * slump, 0, 0.12F * slump);
		turn(pose, b.legR, 0.7F * foldR, 0, 0);
		turn(pose, b.shinR, -1.3F * foldR, 0, 0);
		turn(pose, b.legL, 0.35F * foldL, 0, 0);
		turn(pose, b.shinL, -1.0F * foldL, 0, 0);
		shift(pose, b.hips, 0, -4F * foldR, 0);
		turn(pose, b.armR, 0.35F * slump, 0, -0.2F * slump);
		if (d > 14) {
			float e = d - 14;
			turn(pose, rig.torso, 0.07F * (float) Math.exp(-0.45F * e) * Mth.cos(1.3F * e), 0, 0);
		}
	}

	/** Runs the arm and body past the blow, on a damped spring, by the angle the clip was turning each into the impact. */
	private static void overshoot(MasterRig rig, Bones b, Track track, MasterRig.Pose pose, float post) {
		MasterRig.Clip clip = track.overClip;
		if (clip == null) return;
		int[] ids = {b.armR, b.foreR, rig.torso, b.chest, b.hips};
		if (track.velClip != clip) {
			MasterRig.Pose at = rig.sample(clip, clip.impact(), rig.newPose());
			MasterRig.Pose before = rig.sample(clip, Math.max(0, clip.impact() - 0.1F), rig.newPose());
			for (int i = 0; i < OVER_BONES; i++) {
				float angle = 0, ax = 0, ay = 1, az = 0;
				int bone = ids[i];
				if (bone >= 0) {
					Quaternionf q1 = new Quaternionf(at.q[bone * 4], at.q[bone * 4 + 1], at.q[bone * 4 + 2], at.q[bone * 4 + 3]);
					Quaternionf q0 = new Quaternionf(before.q[bone * 4], before.q[bone * 4 + 1], before.q[bone * 4 + 2], before.q[bone * 4 + 3]);
					q1.mul(q0.conjugate());
					if (q1.w < 0) q1.set(-q1.x, -q1.y, -q1.z, -q1.w);
					float len = (float) Math.sqrt(q1.x * q1.x + q1.y * q1.y + q1.z * q1.z);
					if (len > 1.0E-5F) {
						angle = 2 * (float) Math.atan2(len, q1.w);
						ax = q1.x / len;
						ay = q1.y / len;
						az = q1.z / len;
					}
				}
				track.velocity[i * 4] = angle;
				track.velocity[i * 4 + 1] = ax;
				track.velocity[i * 4 + 2] = ay;
				track.velocity[i * 4 + 3] = az;
			}
			track.velClip = clip;
		}
		float settle = post - HIT_STOP;
		float a = settle < 0 ? smooth(Mth.clamp(post / HIT_STOP, 0, 1)) : (float) (Math.exp(-0.28F * settle) * Math.cos(0.42F * settle));
		for (int i = 0; i < OVER_BONES; i++) {
			float angle = track.velocity[i * 4];
			if (ids[i] < 0 || angle < 1.0E-4F) continue;
			float turn = Mth.clamp(OVER_GAIN[i] * a * angle, -OVER_CAP[i], OVER_CAP[i]);
			pose.preRotate(ids[i], new Quaternionf().rotationAxis(turn, track.velocity[i * 4 + 1], track.velocity[i * 4 + 2], track.velocity[i * 4 + 3]));
		}
	}

	/**
	 * Hair, sash tails, coat skirts, ribbons and the blade each trail what drives them on their own spring: a bone swings
	 * against the angular speed of its driver (the torso, or the sword arm for the blade) by a few ticks' worth, so a cut or a
	 * turn whips them out and they settle with a rebound. Run on the drawn copy only, so it never feeds back into the blend.
	 */
	private static void lag(MasterRig rig, Track track, MasterRig.Pose pose, MasterRig.Pose out, double now, AuraFighterRenderState state,
		boolean left, float moving) {
		Bones b = bones(rig);
		float dt = Double.isNaN(track.lagTime) ? 0 : (float) Mth.clamp(now - track.lagTime, 0, 6);
		track.lagTime = now;
		float turned = Mth.wrapDegrees(state.bodyRot - track.lastYaw);
		track.lastYaw = state.bodyRot;
		float yawRate = dt > 0.05F ? Mth.clamp(turned / dt, -25, 25) * Mth.DEG_TO_RAD : 0;
		if (dt >= 6) java.util.Arrays.fill(track.looseAngle, 0);
		float[] speed = new float[6];
		int[] drivers = {rig.torso, b.armR};
		for (int d = 0; d < drivers.length; d++) {
			int bone = drivers[d];
			if (bone < 0) continue;
			Quaternionf q = new Quaternionf(pose.q[bone * 4], pose.q[bone * 4 + 1], pose.q[bone * 4 + 2], pose.q[bone * 4 + 3]);
			if (track.haveDrivers && dt > 0.05F) {
				Quaternionf prev = new Quaternionf(track.driverQ[d * 4], track.driverQ[d * 4 + 1], track.driverQ[d * 4 + 2], track.driverQ[d * 4 + 3]);
				Quaternionf delta = new Quaternionf(q).mul(prev.conjugate());
				float sign = delta.w < 0 ? -2 : 2;
				speed[d * 3] = Mth.clamp(sign * delta.x / dt, -1.2F, 1.2F);
				speed[d * 3 + 1] = Mth.clamp(sign * delta.y / dt, -1.2F, 1.2F);
				speed[d * 3 + 2] = Mth.clamp(sign * delta.z / dt, -1.2F, 1.2F);
			}
			track.driverQ[d * 4] = q.x;
			track.driverQ[d * 4 + 1] = q.y;
			track.driverQ[d * 4 + 2] = q.z;
			track.driverQ[d * 4 + 3] = q.w;
		}
		track.haveDrivers = true;
		// The body turning in the world, seen from the rig (mirrored for the right-handed).
		speed[1] += yawRate * (left ? -1 : 1);
		int steps = Math.max(1, (int) Math.ceil(dt));
		float h = dt / steps;
		for (int i = 0; i < LOOSE.length; i++) {
			int bone = b.loose[i];
			if (bone < 0) continue;
			int driver = i == LOOSE.length - 1 ? 1 : 0;
			float k = LOOSE_K[i], damping = 0.9F * (float) Math.sqrt(k), max = LOOSE_MAX[i];
			for (int axis = 0; axis < 3; axis++) {
				int at = i * 3 + axis;
				// Trailing behind a walk: pitched back, about the same for hair, sash and coat.
				float drift = axis == 0 && i < LOOSE.length - 1 ? -0.22F * moving : 0;
				float target = Mth.clamp(-LOOSE_LAG[i] * speed[driver * 3 + axis] + drift, -max, max);
				for (int step = 0; step < steps && dt > 0; step++) {
					track.looseSpeed[at] += (k * (target - track.looseAngle[at]) - damping * track.looseSpeed[at]) * h;
					track.looseAngle[at] = Mth.clamp(track.looseAngle[at] + track.looseSpeed[at] * h, -max, max);
				}
			}
			out.preRotate(bone, new Quaternionf().rotateXYZ(track.looseAngle[i * 3], track.looseAngle[i * 3 + 1], track.looseAngle[i * 3 + 2]));
		}
		// The coat skirts part a little as it walks.
		float flare = 0.12F * moving;
		turn(out, b.coatL, 0, 0, -flare);
		turn(out, b.coatR, 0, 0, flare);
	}

	static String clipFor(int code) {
		return switch (code) {
			case MasterAnimationRules.SWEEP, MasterAnimationRules.CINDER_WAKE -> "sweep";
			case MasterAnimationRules.THRUST, MasterAnimationRules.PURSUIT_BREAK, MasterAnimationRules.STONE_FAULT_MARCH -> "thrust";
			case MasterAnimationRules.CRESCENT, MasterAnimationRules.CROSSWIND_REPRISE, MasterAnimationRules.KILN_RING -> "crescent";
			case MasterAnimationRules.BREAK_CAST, MasterAnimationRules.STONE_FRACTURE -> "break_cast";
			default -> CYCLE[Math.floorMod(code, CYCLE.length)];
		};
	}

	/**
	 * Clip time for {@code elapsed} ticks into an attack: the tell stretched from {@code start} to where the clip's strike
	 * begins, the hit tick onto the strike itself, and recovery onto the follow-through's hold.
	 */
	static float warp(MasterRig.Clip clip, float elapsed, int tell, int active, int recovery, float start) {
		// The wind-up reaches its peak a beat early and freezes there (the commit), so the cut is readable.
		if (elapsed < tell) return start + (clip.strike() - start) * Math.min(1, elapsed / Math.max(1, tell - Math.min(COMMIT, tell / 4)));
		elapsed -= tell;
		if (elapsed < active) return clip.strike() + (clip.impact() - clip.strike()) * elapsed / Math.max(1, active);
		elapsed -= active;
		if (recovery <= 0) return clip.impact();
		// The blade holds on the target for the hit-stop, then the follow-through plays out.
		int stop = Math.min(HIT_STOP, recovery / 3);
		if (elapsed < stop) return clip.impact();
		return clip.impact() + (clip.hold() - clip.impact()) * Math.min(1, (elapsed - stop) / Math.max(1, recovery - stop));
	}

	private static float smooth(float w) {
		return w * w * (3 - 2 * w);
	}
}
