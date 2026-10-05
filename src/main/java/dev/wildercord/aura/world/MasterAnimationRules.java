package dev.wildercord.aura.world;

/**
 * Original sword-master choreography on Minecraft's rigid humanoid rig. This sampler only draws
 * the server's accepted attack; it never chooses hits, reach, movement, or invulnerability.
 * Angles are radians except the sword's grip tilt, which is in degrees.
 */
public final class MasterAnimationRules {
	private MasterAnimationRules() {}

	public static final int SWEEP = 1, THRUST = 2, CRESCENT = 3, BREAK_CAST = 4, CINDER_WAKE = 5, PURSUIT_BREAK = 6;
	public record Joint(float x, float y, float z) {
		Joint toward(Joint other, float t) {
			return new Joint(lerp(x, other.x, t), lerp(y, other.y, t), lerp(z, other.z, t));
		}
	}

	/** The two legs share a split-stance angle so both rigid soles can meet the same floor. */
	public record Pose(float weight, Joint body, Joint head, Joint sword, Joint offhand, float stance, float bladeTilt) {
		Pose weight(float value) { return new Pose(value, body, head, sword, offhand, stance, bladeTilt); }
		Pose toward(Pose other, float t) {
			return new Pose(1, body.toward(other.body, t), head.toward(other.head, t), sword.toward(other.sword, t),
				offhand.toward(other.offhand, t), lerp(stance, other.stance, t), lerp(bladeTilt, other.bladeTilt, t));
		}
	}

	/** A model-space attachment in pixels; positive Y is toward the floor. */
	public record Pivot(float x, float y, float z) {}
	private record Motion(Pose chamber, Pose impact, Pose follow) {}
	private static final Joint ZERO = j(0, 0, 0);
	public static final Pose NONE = new Pose(0, ZERO, ZERO, ZERO, ZERO, 0, 0);

	// Broad horizontal cut. The blade passes the target on the hit tick, then the shoulders
	// finish opening. Its modest stance is planted throughout the release and follow-through.
	private static final Motion SWEEP_MOTION = new Motion(
		p(j(.06F, .62F, -.03F), j(0, 0, 0), j(-1.70F, .82F, -.72F), j(-.85F, -.25F, -.35F), .18F, 0),
		p(j(.16F, -.34F, .04F), j(-.05F, 0, 0), j(-1.28F, -.95F, -.50F), j(-.62F, .25F, -.52F), .18F, 0),
		p(j(.12F, -.76F, .05F), j(-.02F, 0, 0), j(-.58F, -1.15F, -.32F), j(-.38F, .20F, -.45F), .18F, 0));

	// The point is chambered beside the face, then driven straight forward. The independent
	// grip tilt is essential: an ordinary Minecraft sword otherwise points up over the fist.
	private static final Motion THRUST_MOTION = new Motion(
		p(j(-.10F, .45F, -.03F), j(.04F, 0, 0), j(-1.18F, .55F, -.10F), j(-.95F, -.10F, -.30F), .30F, -70),
		p(j(.32F, -.20F, .02F), j(-.16F, 0, 0), j(-1.58F, -.06F, -.04F), j(.24F, .10F, -.24F), .30F, -80),
		p(j(.20F, -.28F, .02F), j(-.10F, 0, 0), j(-1.42F, -.10F, -.08F), j(-.25F, .08F, -.25F), .30F, -70));

	// A low-to-high diagonal cut releases the projected crescent, distinct from the flat sweep.
	private static final Motion CRESCENT_MOTION = new Motion(
		p(j(.23F, -.50F, .04F), j(-.09F, 0, 0), j(.12F, -.55F, .38F), j(-1.15F, .18F, -.25F), .22F, 0),
		p(j(-.10F, .24F, -.03F), j(.06F, 0, 0), j(-2.05F, .25F, -.45F), j(-.65F, -.25F, -.48F), .22F, 0),
		p(j(-.06F, .58F, -.04F), j(.02F, 0, 0), j(-2.65F, .55F, -.28F), j(-.40F, -.25F, -.40F), .22F, 0));

	// A compact high chamber and downward point-first interruption. This stays a thrust in
	// gameplay: the body does not depict a broad slash when the server tests a narrow lane.
	private static final Motion BREAK_MOTION = new Motion(
		p(j(-.06F, .28F, -.02F), j(.02F, 0, 0), j(-2.12F, .30F, -.08F), j(-1.12F, -.28F, -.25F), .24F, -55),
		p(j(.25F, -.08F, .01F), j(-.12F, 0, 0), j(-1.30F, -.04F, -.06F), j(-.60F, -.15F, -.35F), .24F, -92),
		p(j(.16F, -.18F, .02F), j(-.07F, 0, 0), j(-1.12F, -.08F, -.08F), j(-.45F, -.12F, -.30F), .24F, -85));

	// Ember lays a low, broad cut, then visibly gathers its point above the already marked wake.
	// The second point-drop is a timed ignition gesture, never a new target-tracking sword attack.
	private static final Motion CINDER_MOTION = new Motion(
		p(j(.20F, .48F, -.04F), j(-.07F, 0, 0), j(-.95F, .72F, -.55F), j(-1.12F, -.18F, -.32F), .20F, -12),
		p(j(.24F, -.54F, .04F), j(-.10F, 0, 0), j(-.78F, -1.04F, -.40F), j(-.78F, .30F, -.46F), .20F, -18),
		p(j(.16F, -.62F, .03F), j(-.05F, 0, 0), j(-.44F, -.86F, -.25F), j(-.56F, .18F, -.38F), .20F, -12));
	private static final Motion IGNITION_MOTION = new Motion(
		p(j(-.08F, -.12F, 0), j(.03F, 0, 0), j(-1.94F, .10F, -.20F), j(-1.12F, -.10F, .20F), .20F, -45),
		p(j(.28F, -.12F, .02F), j(-.12F, 0, 0), j(-.88F, -.12F, -.06F), j(-.64F, -.20F, -.34F), .20F, -108),
		p(j(.16F, -.18F, .01F), j(-.06F, 0, 0), j(-.65F, -.15F, -.10F), j(-.44F, -.14F, -.28F), .20F, -98));

	// A low runner's chamber rises into one planted point strike. The body follows server movement.
	private static final Motion PURSUIT_MOTION = new Motion(
		p(j(.18F, .30F, -.04F), j(-.08F, 0, 0), j(-1.68F, .38F, -.16F), j(-.85F, -.25F, -.35F), .28F, -65),
		p(j(.36F, -.16F, .02F), j(-.16F, 0, 0), j(-1.34F, -.08F, -.04F), j(.16F, .12F, -.28F), .28F, -96),
		p(j(.18F, -.28F, .02F), j(-.08F, 0, 0), j(-1.05F, -.16F, -.10F), j(-.28F, .12F, -.30F), .28F, -85));
	private static final Pose PURSUIT_STEP = p(j(.48F, .22F, -.05F), j(-.22F, 0, 0),
		j(-.70F, .35F, -.32F), j(.24F, -.18F, -.36F), .28F, -45);

	private static final Pose GUARD = p(j(.10F, .10F, 0), j(-.04F, 0, 0), j(-1.42F, -.55F, -.25F), j(-1.05F, .40F, -.25F), .14F, 0);
	private static final Pose DODGE = p(j(.34F, -.18F, -.08F), j(-.18F, 0, .04F), j(-1.05F, .20F, -.35F), j(-.68F, -.18F, -.38F), .28F, 0);
	private static final Pose STAGGER = p(j(-.15F, .06F, .03F), j(.12F, 0, 0), j(-.45F, .16F, .30F), j(-.35F, -.12F, -.40F), .12F, 0);

	/**
	 * Server ID 0 means idle. Impact is exactly at tell, whatever the current balancing numbers;
	 * active is the hit's occupied tick count, followed by recovery. Both ends ease to vanilla.
	 */
	public static Pose sample(int attack, float age, int tell, int active, int recovery) {
		Motion motion = switch (attack) {
			case SWEEP -> SWEEP_MOTION;
			case THRUST -> THRUST_MOTION;
			case CRESCENT -> CRESCENT_MOTION;
			case BREAK_CAST -> BREAK_MOTION;
			case CINDER_WAKE -> CINDER_MOTION;
			case PURSUIT_BREAK -> PURSUIT_MOTION;
			default -> null;
		};
		if (motion == null || !Float.isFinite(age) || age < 0 || tell < 1 || tell > 80 || active < 1 || active > 10
			|| recovery < 1 || recovery > 120 || age >= tell + active + recovery) return NONE;
		if (attack == PURSUIT_BREAK) return pursuit(age, tell, active, recovery);
		if (attack == CINDER_WAKE && age >= tell + 6) return emberWake(age, tell, active, recovery);
		float chamberAt = tell * .65F;
		float followAt = tell + active + Math.min(3, recovery * .20F);
		Pose pose;
		if (age < chamberAt) pose = motion.chamber;
		else if (age < tell) pose = motion.chamber.toward(motion.impact, smooth((age - chamberAt) / (tell - chamberAt)));
		else if (age < followAt) pose = motion.impact.toward(motion.follow, smooth((age - tell) / (followAt - tell)));
		else pose = motion.follow;
		float enter = smooth(age / Math.max(1, chamberAt));
		float leave = 1 - smooth((age - followAt) / (tell + active + recovery - followAt));
		return pose.weight(enter * leave);
	}

	private static Pose pursuit(float age, int tell, int active, int recovery) {
		float step = tell * MasterPursuitRules.WINDUP / MasterPursuitRules.TELL;
		float plant = tell * (MasterPursuitRules.WINDUP + MasterPursuitRules.DASH_TICKS) / MasterPursuitRules.TELL;
		float chamber = tell * .65F, follow = tell + active + Math.min(3, recovery * .20F);
		Pose pose;
		if (age < step) pose = PURSUIT_MOTION.chamber.toward(PURSUIT_STEP, smooth(age / step));
		else if (age < plant) pose = PURSUIT_STEP.toward(PURSUIT_MOTION.chamber, smooth((age - step) / (plant - step)));
		else if (age < chamber) pose = PURSUIT_MOTION.chamber;
		else if (age < tell) pose = PURSUIT_MOTION.chamber.toward(PURSUIT_MOTION.impact, smooth((age - chamber) / (tell - chamber)));
		else if (age < follow) pose = PURSUIT_MOTION.impact.toward(PURSUIT_MOTION.follow, smooth((age - tell) / (follow - tell)));
		else pose = PURSUIT_MOTION.follow;
		if (age >= step && age < plant) {
			float stride = .28F * (float) Math.cos(2 * Math.PI * (age - step) / (plant - step));
			pose = new Pose(pose.weight(), pose.body(), pose.head(), pose.sword(), pose.offhand(), stride, pose.bladeTilt());
		}
		return pose.weight(smooth(age / Math.max(1, step * .5F))
			* (1 - smooth((age - follow) / (tell + active + recovery - follow))));
	}

	private static Pose emberWake(float age, int tell, int active, int recovery) {
		float gather = tell + 6, ignite = tell + EmberWakeRules.AFTERBURN_TELL;
		float chamber = ignite - 8, follow = ignite + 4, end = tell + active + recovery;
		Pose pose;
		if (age < chamber) pose = CINDER_MOTION.follow.toward(IGNITION_MOTION.chamber, smooth((age - gather) / (chamber - gather)));
		else if (age < ignite) pose = IGNITION_MOTION.chamber.toward(IGNITION_MOTION.impact, smooth((age - chamber) / (ignite - chamber)));
		else if (age < follow) pose = IGNITION_MOTION.impact.toward(IGNITION_MOTION.follow, smooth((age - ignite) / (follow - ignite)));
		else pose = IGNITION_MOTION.follow;
		// Match the ordinary first follow-through exactly at the handoff before gathering again.
		float initialWeight = 1 - smooth((gather - (tell + active + Math.min(3, recovery * .20F)))
			/ (end - (tell + active + Math.min(3, recovery * .20F))));
		float weight = age < chamber ? lerp(initialWeight, 1, smooth((age - gather) / (chamber - gather)))
			: 1 - smooth((age - follow) / Math.max(1, end - follow));
		return pose.weight(weight);
	}

	/** Defensive flags are already eased by AuraFighter. They never extend a cancelled attack. */
	public static Pose defence(float guard, float dodge, float stagger) {
		if (clamp(stagger) > 0) return STAGGER.weight(clamp(stagger));
		if (clamp(dodge) > 0) return DODGE.weight(clamp(dodge));
		if (clamp(guard) > 0) return GUARD.weight(clamp(guard));
		return NONE;
	}

	/**
	 * Tilt the crescent's release plane toward the server's accepted elevation, without moving
	 * feet or looking up a target. A bounded shoulder adjustment avoids folding a rigid arm
	 * behind the hood at steep upward angles. Other attacks remain in their ground plane.
	 */
	public static Pose aimed(Pose pose, int attack, float pitchDegrees) {
		if (attack != CRESCENT || pose.weight() <= 0 || !Float.isFinite(pitchDegrees)) return pose;
		float pitch = (float) Math.toRadians(Math.max(-70, Math.min(70, pitchDegrees)));
		return new Pose(pose.weight(), j(pose.body().x() + pitch * .16F, pose.body().y(), pose.body().z()), pose.head(),
			j(Math.max(-3.05F, Math.min(.65F, pose.sword().x() + pitch * .5F)), pose.sword().y(), pose.sword().z()),
			j(pose.offhand().x() + pitch * .12F, pose.offhand().y(), pose.offhand().z()), pose.stance(), pose.bladeTilt());
	}

	/** Both rigid sole edges touch Y=24; account for the foot cube's two-pixel half-depth. */
	public static float lower(float stance) {
		return 12 * (1 - (float) Math.cos(stance)) - 2 * Math.abs((float) Math.sin(stance));
	}

	/** Carries neck and shoulder roots around the hip hinge, using ModelPart's Z/Y/X order. */
	public static Pivot pivot(Joint body, float lower, float x, float y, float z) {
		float yy = y - 12;
		float ay = yy * (float) Math.cos(body.x()) - z * (float) Math.sin(body.x());
		float az = yy * (float) Math.sin(body.x()) + z * (float) Math.cos(body.x());
		float bx = x * (float) Math.cos(body.y()) + az * (float) Math.sin(body.y());
		float bz = -x * (float) Math.sin(body.y()) + az * (float) Math.cos(body.y());
		return new Pivot(bx * (float) Math.cos(body.z()) - ay * (float) Math.sin(body.z()),
			bx * (float) Math.sin(body.z()) + ay * (float) Math.cos(body.z()) + 12 + lower, bz);
	}

	public static Joint mirrored(Joint joint, boolean leftHanded) {
		return leftHanded ? j(joint.x(), -joint.y(), -joint.z()) : joint;
	}

	private static float clamp(float value) { return Float.isFinite(value) ? Math.max(0, Math.min(1, value)) : 0; }
	private static float smooth(float value) { float t = clamp(value); return t * t * (3 - 2 * t); }
	private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
	private static Joint j(float x, float y, float z) { return new Joint(x, y, z); }
	private static Pose p(Joint body, Joint head, Joint sword, Joint offhand, float stance, float tilt) {
		return new Pose(1, body, head, sword, offhand, stance, tilt);
	}
}
