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
	}

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
		int code = stagger ? 0 : master.attackAnimation();
		if (code != 0) {
			float elapsed = master.attackElapsed(partial);
			long begin = tick - (long) Math.floor(master.attackElapsed(0));
			MasterRig.Clip clip;
			float time;
			int strike = 0;
			if (code == MasterAnimationRules.TECHNIQUE && master.technique() != 0) {
				MasterTechniques.Technique technique = MasterTechniques.byId(master.technique());
				int last = technique.strikes().size() - 1;
				while (strike < last && elapsed >= technique.impact(strike) + 1) strike++;
				clip = rig.clip(CYCLE[strike % CYCLE.length]);
				int from = strike == 0 ? 0 : technique.impact(strike - 1) + 1;
				float start = strike == 0 ? 0 : clip.strike() * 0.45F;
				time = warp(clip, elapsed - from, technique.impact(strike) - from, 1,
					strike == last ? technique.recovery() - 1 : 0, start);
			} else {
				boolean counter = code == MasterAnimationRules.SWEEP && begin - track.dashEnd >= -1 && begin - track.dashEnd <= COUNTER_WINDOW;
				clip = rig.clip(counter ? "vanish" : clipFor(code));
				time = warp(clip, elapsed, master.attackTellTicks(), master.attackActiveTicks(), master.attackRecoveryTicks(),
					counter ? clip.strike() * 0.65F : 0);
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
			if (stagger) track.tail = null;
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
		float look = code != 0 ? 0.35F : 0.8F;
		float yaw = Mth.clamp(state.yRot, -60, 60) * look * Mth.DEG_TO_RAD * (left ? -1 : 1);
		float pitch = Mth.clamp(state.xRot, -45, 45) * look * Mth.DEG_TO_RAD;
		MasterRig.Pose drawn = pose.copy();
		drawn.preRotate(rig.head, new Quaternionf().rotateY(yaw).rotateX(-pitch));
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
		if (elapsed < tell) return start + (clip.strike() - start) * elapsed / Math.max(1, tell);
		elapsed -= tell;
		if (elapsed < active) return clip.strike() + (clip.impact() - clip.strike()) * elapsed / Math.max(1, active);
		elapsed -= active;
		if (recovery <= 0) return clip.impact();
		return clip.impact() + (clip.hold() - clip.impact()) * Math.min(1, elapsed / recovery);
	}

	private static float smooth(float w) {
		return w * w * (3 - 2 * w);
	}
}
