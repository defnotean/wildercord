package dev.wildercord.aura;

/**
 * Original, Minecraft-independent combat keyframes. The server's windup ends at the impact frame;
 * the rest of the motion follows through and settles during its recovery. These are cosmetic
 * poses only: no hit, reach, movement or timing decision is made by the animation.
 */
public final class MastersArtAnimation {
	private MastersArtAnimation() {}

	/** Joint rotations in radians, authored for a right-handed fighter and mirrored by the renderer. */
	public record Joint(float x, float y, float z) {
		private static final Joint ZERO = new Joint(0, 0, 0);
		private Joint toward(Joint other, float t) {
			return new Joint(lerp(x, other.x, t), lerp(y, other.y, t), lerp(z, other.z, t));
		}
	}

	/** First-person translation in blocks and rotation in degrees, separate from the rigid body rig. */
	public record Hand(float x, float y, float z, float pitch, float yaw, float roll) {
		private static final Hand ZERO = new Hand(0, 0, 0, 0, 0, 0);
		private Hand toward(Hand other, float t) {
			return new Hand(lerp(x, other.x, t), lerp(y, other.y, t), lerp(z, other.z, t),
				lerp(pitch, other.pitch, t), lerp(yaw, other.yaw, t), lerp(roll, other.roll, t));
		}
	}

	/** A full-body pose, with a shared blend weight and a small body offset in model pixels. */
	public record Pose(float weight, Joint body, Joint head, Joint sword, Joint guard, Joint frontLeg,
			Joint rearLeg, float lower, float forward, Hand hand) {
		Pose weight(float value) {
			return new Pose(value, body, head, sword, guard, frontLeg, rearLeg, lower, forward, hand);
		}
		Pose toward(Pose other, float t) {
			return new Pose(1, body.toward(other.body, t), head.toward(other.head, t), sword.toward(other.sword, t),
				guard.toward(other.guard, t), frontLeg.toward(other.frontLeg, t), rearLeg.toward(other.rearLeg, t),
				lerp(lower, other.lower, t), lerp(forward, other.forward, t), hand.toward(other.hand, t));
		}
	}

	public static final Pose NONE = new Pose(0, Joint.ZERO, Joint.ZERO, Joint.ZERO, Joint.ZERO,
		Joint.ZERO, Joint.ZERO, 0, 0, Hand.ZERO);

	/** A rigid-rig attachment point in model pixels. */
	public record Pivot(float x, float y, float z) {}

	/** Body and relative head yaw after committing the blade direction, plus its offset from free look. */
	public record Facing(float bodyYaw, float headYaw, float yawDelta, float pitchDelta) {}

	/** A weighted first-person transform and the vanilla grip point it rotates around. */
	public record View(Hand transform, Pivot grip) {}

	/**
	 * Keeps the hilt in front of the camera while the weapon turns about it. Large free-look offsets
	 * are bounded here and explained by the HUD; the camera itself is never steered by the art.
	 */
	public static View view(Pose pose, boolean leftHanded, float inverseArmHeight, float yawDelta, float pitchDelta) {
		float side = leftHanded ? -1 : 1, weight = pose.weight();
		Hand hand = pose.hand();
		float yaw = Math.max(-55, Math.min(55, yawDelta)), pitch = Math.max(-40, Math.min(40, pitchDelta));
		// Turning the blade across the view needs a little camera clearance; this never changes reach.
		float clearance = .20F * Math.max(Math.abs(yaw) / 55, Math.abs(pitch) / 40);
		Hand transform = new Hand(hand.x() * side * weight, hand.y() * weight, (hand.z() - clearance) * weight,
			(hand.pitch() - pitch) * weight, (hand.yaw() * side - yaw) * weight, hand.roll() * side * weight);
		return new View(transform, new Pivot(.56F * side, -.52F - .6F * Math.max(0, Math.min(1, inverseArmHeight)), -.72F));
	}

	/** Minecraft positive yaw turns to the viewer's right, opposite camera-space positive Y rotation. */
	public static String offscreenDirection(float yawDelta, float pitchDelta) {
		return Math.abs(yawDelta) > 55 ? yawDelta > 0 ? "right" : "left"
			: Math.abs(pitchDelta) > 55 ? pitchDelta > 0 ? "down" : "up" : "";
	}

	/** A sword's grip-local pitch: points thrust forward, and low draws keep the blade near the cut's height. */
	public static float bladeTilt(int move, float age, int windup, float weight) {
		// Blossom's falling cut finishes with its point toward the ground. Turn at the actual hilt,
		// entering during the cut and easing out with the same recovery weight as the rigid arm.
		if (move == 14) return -100 * smooth((age - windup * .65F) / (windup * .35F)) * Math.max(0, Math.min(1, weight));
		// Ground-field releases keep their falling edge on the hilt while the forearm absorbs
		// the stop. Their persistent fields do not contribute additional blade motion.
		if (move == 17 || move == 18) return (move == 17 ? -105 : -80)
			* smooth((age - windup * .65F) / (windup * .35F)) * Math.max(0, Math.min(1, weight));
		float thrust = move == 2 || move == 3 || move == 6 || move == 10 || move == 12 ? 1
			: move == 5 ? smooth((age - windup - ArtRules.CRACKLE_GAP) / ArtRules.CRACKLE_GAP) : 0;
		return -80 * thrust * Math.max(0, Math.min(1, weight));
	}

	/** Locks the animated body toward accepted aim, with bounded model free look; the camera is independent. */
	public static Facing facing(float bodyYaw, float headYaw, float viewPitch, float acceptedYaw, float acceptedPitch, float weight) {
		float absoluteHead = bodyYaw + headYaw;
		float blend = Math.max(0, Math.min(1, weight));
		float body = bodyYaw + degrees(acceptedYaw - bodyYaw) * blend;
		float wantedHead = degrees(absoluteHead - body);
		float head = lerp(wantedHead, Math.max(-75, Math.min(75, wantedHead)), blend);
		return new Facing(degrees(body), head, degrees(acceptedYaw - absoluteHead), acceptedPitch - viewPitch);
	}

	/** Neck limits are relative to the authored torso, not just the entity's world heading. */
	public static Joint boundedHead(Joint body, Joint wanted) {
		float radians = (float) (Math.PI / 180);
		float yaw = degrees((wanted.y() - body.y()) / radians) * radians;
		return new Joint(body.x() + Math.max(-45 * radians, Math.min(60 * radians, wanted.x() - body.x())),
			body.y() + Math.max(-75 * radians, Math.min(75 * radians, yaw)),
			body.z() + Math.max(-25 * radians, Math.min(25 * radians, wanted.z() - body.z())));
	}

	/** The limits enter and leave with the art, avoiding a snap from vanilla's full vertical look. */
	public static Joint boundedHead(Joint body, Joint wanted, float weight) {
		return wanted.toward(boundedHead(body, wanted), Math.max(0, Math.min(1, weight)));
	}

	private static float degrees(float value) {
		float wrapped = value % 360;
		if (wrapped >= 180) wrapped -= 360;
		if (wrapped < -180) wrapped += 360;
		return wrapped;
	}

	/**
	 * Turns an upper-body attachment about the hips, in ModelPart's Z/Y/X rotation order. Keeping
	 * that pivot fixed prevents a forward-leaning torso from separating from the leg roots.
	 */
	public static Pivot pivot(Pose pose, float x, float y, float z, boolean leftHanded) {
		float side = leftHanded ? -1 : 1;
		float rx = pose.body().x(), ry = pose.body().y() * side, rz = pose.body().z() * side;
		float yy = y - 12;
		float ax = x;
		float ay = yy * (float) Math.cos(rx) - z * (float) Math.sin(rx);
		float az = yy * (float) Math.sin(rx) + z * (float) Math.cos(rx);
		float bx = ax * (float) Math.cos(ry) + az * (float) Math.sin(ry);
		float bz = -ax * (float) Math.sin(ry) + az * (float) Math.cos(ry);
		return new Pivot(bx * (float) Math.cos(rz) - ay * (float) Math.sin(rz),
			bx * (float) Math.sin(rz) + ay * (float) Math.cos(rz) + 12 + pose.lower(), bz + pose.forward());
	}

	record Motion(Pose chamber, Pose impact, Pose follow) {}

	/** Across the body from a high guard: the hips unwind before the blade settles to the far side.
	 * The first-person follow keeps the cutting edge above the survival HUD as the wrist rolls over. */
	private static final Motion SPELLCUT = new Motion(
		pose(j(.10F, .55F, -.08F), j(0, -.25F, 0), j(-1.95F, .95F, -.65F), j(-.90F, -.30F, -.20F),
			j(-.40F, -.15F, -.08F), j(.35F, .10F, .10F), .45F, 0, h(.15F, .12F, .12F, -40, 35, -38)),
		pose(j(.18F, -.42F, .10F), j(-.06F, .20F, -.05F), j(-1.35F, -1.05F, -.45F), j(-.50F, .15F, -.55F),
			j(-.65F, .10F, -.08F), j(.40F, -.10F, .10F), .30F, -.8F, h(-.35F, -.12F, -.30F, 30, -42, 50)),
		pose(j(.10F, -.65F, .12F), j(0, .30F, 0), j(-.55F, -1.25F, .10F), j(-.30F, .15F, -.45F),
			j(-.35F, .15F, -.05F), j(.25F, -.10F, .06F), .15F, -.4F, h(-.48F, .25F, -.14F, 50, -48, 66)));

	/** A low chamber, a rising cut past the face, then a high guard with the shoulders opened. */
	private static final Motion RISING_BREAK = new Motion(
		pose(j(.40F, .30F, .10F), j(-.20F, -.15F, 0), j(.25F, .35F, .40F), j(-.85F, -.25F, -.40F),
			j(-.85F, -.15F, -.16F), j(.65F, .15F, .16F), 1.3F, .1F, h(.05F, .05F, -.45F, 55, 15, -20)),
		pose(j(-.25F, -.25F, -.10F), j(.10F, .10F, 0), j(-2.75F, -.25F, -.25F), j(-1.50F, -.10F, -.60F),
			j(-.20F, .10F, -.06F), j(.15F, -.05F, .05F), -.45F, -.65F, h(-.10F, .35F, -.30F, -95, -15, 12)),
		pose(j(-.10F, -.40F, -.08F), j(.05F, .15F, 0), j(-2.95F, -.45F, -.50F), j(-1.00F, -.15F, -.70F),
			j(-.25F, .12F, -.08F), j(.20F, -.08F, .10F), -.10F, -.20F, h(-.15F, .44F, -.12F, -112, -25, 28)));

	/** A drawn-back point, a committed forward thrust with a long stance, then a deliberate recoil. */
	private static final Motion DRIVING_CUT = new Motion(
		pose(j(-.12F, .55F, -.08F), j(.08F, -.25F, 0), j(-1.10F, .65F, -.20F), j(-1.35F, -.25F, -.15F),
			j(-.25F, -.20F, -.10F), j(.45F, .15F, .08F), .40F, .55F, h(.18F, -.02F, .34F, -100, 22, -8)),
		pose(j(.48F, -.25F, .06F), j(-.20F, .10F, 0), j(-1.65F, -.10F, -.08F), j(.45F, -.30F, -.25F),
			j(-1.05F, .08F, -.10F), j(.70F, -.08F, .08F), .90F, -1.75F, h(-.12F, .03F, -.64F, -110, -8, 8)),
		pose(j(.25F, -.35F, .08F), j(-.12F, .15F, 0), j(-1.45F, -.20F, -.12F), j(.20F, -.15F, -.30F),
			j(-.65F, .12F, -.08F), j(.45F, -.10F, .10F), .50F, -.90F, h(-.16F, -.02F, -.42F, -100, -12, 12)));

	/**
	 * Samples the server's timeline. Invalid or expired timelines are neutral; both ends blend to
	 * vanilla, and impact lands exactly at {@code windup}, independent of frame rate.
	 */
	public static Pose sample(int move, float age, int windup, int recovery) {
		Motion motion = switch (move) {
			case 0 -> SPELLCUT;
			case 1 -> RISING_BREAK;
			case 2 -> DRIVING_CUT;
			default -> MastersStyleAnimation.motion(move);
		};
		if (motion == null || !Float.isFinite(age) || windup <= 0 || windup > 60 || recovery <= 0 || recovery > 120
				|| age < 0 || age >= windup + recovery) return NONE;
		if (move == 5 || move == 11) return MastersStyleAnimation.repeated(move, motion, age, windup, recovery);
		if (move == 16) return MastersStyleAnimation.skyfall(motion, age, windup, recovery);
		return sample(motion, age, windup, recovery);
	}

	/** The cosmetic protocol accepts these IDs; the input protocol still accepts only the three shared arts. */
	public static boolean supports(int move) {
		return move >= 0 && move <= 2 || MastersStyleAnimation.motion(move) != null;
	}

	static Pose sample(Motion motion, float age, int windup, int recovery) {
		float chamberAt = windup * .65F;
		float followAt = windup + Math.min(4, recovery * .25F);
		Pose frame;
		if (age < chamberAt) {
			frame = motion.chamber;
		} else if (age < windup) {
			frame = motion.chamber.toward(motion.impact, smooth((age - chamberAt) / (windup - chamberAt)));
		} else if (age < followAt) {
			frame = motion.impact.toward(motion.follow, smooth((age - windup) / (followAt - windup)));
		} else {
			frame = motion.follow;
		}
		float enter = smooth(age / Math.max(1, chamberAt));
		float leave = 1 - smooth((age - followAt) / (windup + recovery - followAt));
		return frame.weight(enter * leave);
	}

	static float smooth(float value) {
		float t = Math.max(0, Math.min(1, value));
		return t * t * (3 - 2 * t);
	}

	private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
	static Joint j(float x, float y, float z) { return new Joint(x, y, z); }
	static Hand h(float x, float y, float z, float pitch, float yaw, float roll) { return new Hand(x, y, z, pitch, yaw, roll); }
	static Pose pose(Joint body, Joint head, Joint sword, Joint guard, Joint front, Joint rear, float lower, float forward, Hand hand) {
		return new Pose(1, body, head, sword, guard, front, rear, lower, forward, hand);
	}
}
