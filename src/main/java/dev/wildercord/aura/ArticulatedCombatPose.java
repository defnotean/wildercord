package dev.wildercord.aura;

/**
 * Original, Minecraft-independent articulated Spellcut and sword-master sweep choreography.
 * Model units are pixels, +Y is down, and -Z is forward. All transforms are local to the named
 * parent, include their bind translation, and are already blended. This class cannot move an
 * entity, choose a target, alter a camera, or make a damage/reach decision.
 */
public final class ArticulatedCombatPose {
	private ArticulatedCombatPose() {}

	public static final int SPELLCUT = 0, MASTER_SWEEP = 1;
	public enum Phase { NONE, WINDUP, ACTIVE, RECOVERY }

	public record Vec3(float x, float y, float z) {
		public static final Vec3 ZERO = new Vec3(0, 0, 0);
		Vec3 plus(Vec3 v) { return new Vec3(x + v.x, y + v.y, z + v.z); }
		Vec3 minus(Vec3 v) { return new Vec3(x - v.x, y - v.y, z - v.z); }
		Vec3 times(float n) { return new Vec3(x * n, y * n, z * n); }
		float dot(Vec3 v) { return x * v.x + y * v.y + z * v.z; }
		Vec3 cross(Vec3 v) { return new Vec3(y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y * v.x); }
		float length() { return (float) Math.sqrt(dot(this)); }
		Vec3 unit() { float d = length(); return d > 1.0e-7F ? times(1 / d) : ZERO; }
		Vec3 toward(Vec3 v, float t) { return plus(v.minus(this).times(t)); }
	}

	/** ModelPart-compatible Rz * Ry * Rx Euler angles, in radians. */
	public record Rotation(float x, float y, float z) {
		public static final Rotation ZERO = new Rotation(0, 0, 0);
		/** Shortest-arc quaternion interpolation also handles the +/- pi seam. */
		public Rotation toward(Rotation other, float amount) {
			float t = clamp(amount);
			if (t == 0) return this;
			if (t == 1) return other;
			return Quaternion.of(this).toward(Quaternion.of(other), t).matrix().rotation();
		}
	}

	public record Transform(float x, float y, float z, Rotation rotation) {
		public static final Transform IDENTITY = new Transform(0, 0, 0, Rotation.ZERO);
		public Vec3 translation() { return new Vec3(x, y, z); }
		public Matrix matrix() { return Matrix.of(this); }
		Transform toward(Transform other, float t) {
			return new Transform(lerp(x, other.x, t), lerp(y, other.y, t), lerp(z, other.z, t), rotation.toward(other.rotation, t));
		}
		Transform reflected() { return new Transform(x == 0 ? 0 : -x, y, z, r(rotation.x, rotation.y == 0 ? 0 : -rotation.y, rotation.z == 0 ? 0 : -rotation.z)); }
	}

	/** Parent-before-child order is intentional. A socket follows its hand, never the upper arm. */
	public enum Joint {
		PELVIS(null, 0, 12, 0), SPINE(PELVIS, 0, -3, 0), CHEST(SPINE, 0, -5, 0), HEAD(CHEST, 0, -4, 0),
		RIGHT_SHOULDER(CHEST, -5, -2, 0), RIGHT_UPPER_ARM(RIGHT_SHOULDER, 0, 0, 0),
		RIGHT_FOREARM(RIGHT_UPPER_ARM, 0, 4, 0), RIGHT_HAND(RIGHT_FOREARM, 0, 4, 0), RIGHT_SOCKET(RIGHT_HAND, 0, 1, 0),
		LEFT_SHOULDER(CHEST, 5, -2, 0), LEFT_UPPER_ARM(LEFT_SHOULDER, 0, 0, 0),
		LEFT_FOREARM(LEFT_UPPER_ARM, 0, 4, 0), LEFT_HAND(LEFT_FOREARM, 0, 4, 0), LEFT_SOCKET(LEFT_HAND, 0, 1, 0),
		RIGHT_THIGH(PELVIS, -1.9F, 0, 0), RIGHT_SHIN(RIGHT_THIGH, 0, 6, 0), RIGHT_FOOT(RIGHT_SHIN, 0, 4, 0),
		LEFT_THIGH(PELVIS, 1.9F, 0, 0), LEFT_SHIN(LEFT_THIGH, 0, 6, 0), LEFT_FOOT(LEFT_SHIN, 0, 4, 0);

		private final Joint parent;
		private final Transform bind;
		Joint(Joint parent, float x, float y, float z) { this.parent = parent; this.bind = new Transform(x, y, z, Rotation.ZERO); }
		public Joint parent() { return parent; }
		public Transform bind() { return bind; }
		public Joint opposite() {
			return switch (this) {
				case RIGHT_SHOULDER -> LEFT_SHOULDER; case LEFT_SHOULDER -> RIGHT_SHOULDER;
				case RIGHT_UPPER_ARM -> LEFT_UPPER_ARM; case LEFT_UPPER_ARM -> RIGHT_UPPER_ARM;
				case RIGHT_FOREARM -> LEFT_FOREARM; case LEFT_FOREARM -> RIGHT_FOREARM;
				case RIGHT_HAND -> LEFT_HAND; case LEFT_HAND -> RIGHT_HAND;
				case RIGHT_SOCKET -> LEFT_SOCKET; case LEFT_SOCKET -> RIGHT_SOCKET;
				case RIGHT_THIGH -> LEFT_THIGH; case LEFT_THIGH -> RIGHT_THIGH;
				case RIGHT_SHIN -> LEFT_SHIN; case LEFT_SHIN -> RIGHT_SHIN;
				case RIGHT_FOOT -> LEFT_FOOT; case LEFT_FOOT -> RIGHT_FOOT;
				default -> this;
			};
		}
	}

	/** Immutable rigid matrix, with column-major values suitable for independent preview tools. */
	public static final class Matrix {
		private final float[] m;
		private Matrix(float[] values) { m = values; }
		public static Matrix identity() { return of(Transform.IDENTITY); }
		static Matrix of(Transform t) {
			float sx = (float) Math.sin(t.rotation.x), cx = (float) Math.cos(t.rotation.x);
			float sy = (float) Math.sin(t.rotation.y), cy = (float) Math.cos(t.rotation.y);
			float sz = (float) Math.sin(t.rotation.z), cz = (float) Math.cos(t.rotation.z);
			return new Matrix(new float[] {cz * cy, sz * cy, -sy, 0,
				cz * sy * sx - sz * cx, sz * sy * sx + cz * cx, cy * sx, 0,
				cz * sy * cx + sz * sx, sz * sy * cx - cz * sx, cy * cx, 0, t.x, t.y, t.z, 1});
		}
		static Matrix basis(Vec3 x, Vec3 y, Vec3 z) {
			return new Matrix(new float[] {x.x, x.y, x.z, 0, y.x, y.y, y.z, 0, z.x, z.y, z.z, 0, 0, 0, 0, 1});
		}
		public float get(int row, int column) {
			if (row < 0 || row > 3 || column < 0 || column > 3) throw new IndexOutOfBoundsException();
			return m[column * 4 + row];
		}
		public float[] values() { return m.clone(); }
		public Matrix multiply(Matrix other) {
			float[] product = new float[16];
			for (int col = 0; col < 4; col++) for (int row = 0; row < 4; row++)
				for (int k = 0; k < 4; k++) product[col * 4 + row] += get(row, k) * other.get(k, col);
			return new Matrix(product);
		}
		public Vec3 transform(float x, float y, float z) {
			return new Vec3(m[0] * x + m[4] * y + m[8] * z + m[12],
				m[1] * x + m[5] * y + m[9] * z + m[13], m[2] * x + m[6] * y + m[10] * z + m[14]);
		}
		public Vec3 transform(Vec3 p) { return transform(p.x, p.y, p.z); }
		public Vec3 direction(Vec3 v) { return transform(v).minus(transform(0, 0, 0)); }
		public Matrix inverseRigid() {
			float[] inverse = {m[0], m[4], m[8], 0, m[1], m[5], m[9], 0, m[2], m[6], m[10], 0, 0, 0, 0, 1};
			for (int row = 0; row < 3; row++) inverse[12 + row] = -(inverse[row] * m[12] + inverse[4 + row] * m[13] + inverse[8 + row] * m[14]);
			return new Matrix(inverse);
		}
		public Rotation rotation() {
			float y = (float) Math.asin(Math.max(-1, Math.min(1, -m[2])));
			if (Math.abs(Math.cos(y)) > 1.0e-5)
				return r((float) Math.atan2(m[6], m[10]), y, (float) Math.atan2(m[1], m[0]));
			return r((float) Math.atan2(-m[9], m[5]), y, 0);
		}
	}

	public static final class Pose {
		private final float weight;
		private final Phase phase;
		private final Transform[] local;
		private final Matrix[] world;
		private final ViewKey camera;
		private Pose(float weight, Phase phase, Transform[] local, ViewKey camera) {
			this.weight = weight; this.phase = phase; this.local = local.clone(); this.world = worlds(local); this.camera = camera;
		}
		public float weight() { return weight; }
		public Phase phase() { return phase; }
		public Transform local(Joint joint) { return local[joint.ordinal()]; }
		public Matrix world(Joint joint) { return world[joint.ordinal()]; }
		public Matrix socket(boolean left) { return world(left ? Joint.LEFT_SOCKET : Joint.RIGHT_SOCKET); }
	}

	/** The camera remains fixed; origin is a model placement in camera-space blocks. */
	public static final class ViewPose {
		private final Pose pose;
		private ViewPose(Pose pose) { this.pose = pose; }
		public float weight() { return pose.weight; }
		public Phase phase() { return pose.phase; }
		public Vec3 origin() { return new Vec3(0, -.7F, -.9F); }
		public Transform local(Joint joint) { return pose.local(joint); }
		public Matrix world(Joint joint) { return pose.world(joint); }
		public Matrix socket(boolean left) { return pose.socket(left); }
		/** Applies the renderer's (-1,-1,+1) model scale and the 1/16 pixel scale. */
		public Vec3 cameraPoint(Joint joint, float x, float y, float z) {
			Vec3 p = world(joint).transform(x, y, z), root = origin();
			return new Vec3(root.x - p.x / 16, root.y - p.y / 16, root.z + p.z / 16);
		}
	}

	private record Arm(Rotation shoulder, Rotation upper, float elbow, Rotation wrist, Rotation socket) {
		Arm toward(Arm b, float t) { return new Arm(shoulder.toward(b.shoulder, t), upper.toward(b.upper, t),
			lerp(elbow, b.elbow, t), wrist.toward(b.wrist, t), socket.toward(b.socket, t)); }
	}
	private record ViewKey(Vec3 right, Arm sword, Vec3 left, Arm guard) {
		ViewKey toward(ViewKey b, float t) { return new ViewKey(right.toward(b.right, t), sword.toward(b.sword, t),
			left.toward(b.left, t), guard.toward(b.guard, t)); }
	}
	private record Key(Vec3 shift, Rotation pelvis, Rotation spine, Rotation chest, Rotation head, Arm sword, Arm guard, ViewKey view) {
		Key toward(Key b, float t) { return new Key(shift.toward(b.shift, t), pelvis.toward(b.pelvis, t), spine.toward(b.spine, t),
			chest.toward(b.chest, t), head.toward(b.head, t), sword.toward(b.sword, t), guard.toward(b.guard, t), view.toward(b.view, t)); }
	}
	private record Motion(Key chamber, Key impact, Key follow, Vec3 rightPlant, Vec3 leftPlant) {}

	// These are camera-space arm placements, not world motion or camera/FOV changes.
	// Keep the actual grip and guard wrist above the survival HUD at the native 70-degree
	// hand projection. Recovery stays peripheral rather than dropping behind the hotbar.
	// The small impact/follow grip twist exposes the blade face without extending its reach.
	private static final ViewKey VIEW_BIND = new ViewKey(v(-8.3F, -5.8F, 5.1F), arm(r(0, 0, 0), r(-1.12F, 0, 0), -.5F, r(.06F, 0, 0)),
		v(8.3F, -6.5F, 5.1F), arm(r(0, 0, 0), r(-1.05F, 0, 0), -.65F, r(0, 0, 0)));
	private static final ViewKey VIEW_CHAMBER = new ViewKey(v(-6.5F, -4.3F, 3), arm(r(.02F, .10F, -.04F), r(-1.58F, .22F, -.34F), -.92F, r(.13F, .20F, -.26F)),
		v(6.7F, -6.1F, 3.5F), arm(r(0, -.04F, .03F), r(-1.02F, .08F, .30F), -.93F, r(.12F, -.14F, .08F)));
	private static final ViewKey VIEW_IMPACT = new ViewKey(v(-7.5F, -4.4F, 2.2F), arm(r(.02F, -.08F, -.06F), r(-1.34F, -.58F, -.40F), -.36F, r(.12F, -.06F, .48F)),
		v(6.5F, -6.1F, 3.5F), arm(r(0, .06F, .02F), r(-.92F, .18F, .34F), -.84F, r(.10F, -.08F, .12F)));
	private static final ViewKey VIEW_FOLLOW = new ViewKey(v(-11.0F, -4.6F, 2.8F), arm(r(.02F, -.12F, -.03F), r(-1.10F, -.89F, -.18F), -.65F, r(.15F, -.18F, .65F)),
		v(7.5F, -6.1F, 3.8F), arm(r(0, .08F, .02F), r(-.85F, -.08F, .24F), -.82F, r(.10F, -.08F, .10F)));

	// Hips initiate, the chest counter-turns, and an independently flexed elbow opens through the cut.
	// Both ankles keep the same authored targets for all three keys.
	private static final Motion SPELLCUT_MOTION = new Motion(
		new Key(v(-.30F, 1.05F, .15F), r(.03F, .22F, -.025F), r(.07F, .18F, -.025F), r(.03F, .25F, -.045F), r(-.07F, -.40F, .03F),
			arm(r(.02F, .10F, -.07F), r(-1.40F, .40F, -.52F), -1.04F, r(.15F, .18F, -.22F)),
			arm(r(0, -.07F, .04F), r(-.75F, -.35F, -.12F), -.92F, r(.10F, -.15F, .08F)), VIEW_CHAMBER),
		new Key(v(.38F, 1.20F, -.65F), r(.045F, -.18F, .035F), r(.07F, -.12F, .025F), r(.06F, -.21F, .04F), r(-.08F, .31F, -.03F),
			arm(r(.03F, -.07F, -.06F), r(-1.20F, -.72F, -.28F), -.42F, r(.12F, -.20F, .46F)),
			arm(r(0, .07F, .04F), r(-.60F, -.30F, -.20F), -.80F, r(.08F, -.10F, .12F)), VIEW_IMPACT),
		new Key(v(.27F, .95F, -.35F), r(.025F, -.27F, .03F), r(.04F, -.17F, .03F), r(.03F, -.23F, .045F), r(-.05F, .40F, -.03F),
			arm(r(.03F, -.11F, -.02F), r(-.68F, -1.04F, .14F), -.78F, r(.14F, -.34F, .60F)),
			arm(r(0, .07F, .03F), r(-.48F, -.30F, -.20F), -.74F, r(.06F, -.10F, .10F)), VIEW_FOLLOW),
		v(-2.4F, 22, 1.5F), v(2.4F, 22, -1.7F));

	private static final Motion SWEEP_MOTION = new Motion(
		new Key(v(-.40F, 1.35F, .22F), r(.04F, .27F, -.035F), r(.055F, .20F, -.02F), r(.03F, .24F, -.045F), r(-.05F, -.44F, .03F),
			arm(r(.02F, .10F, -.08F), r(-1.30F, .50F, -.58F), -1.15F, r(.15F, .20F, -.28F)),
			arm(r(0, -.08F, .04F), r(-.74F, -.38F, -.14F), -1.04F, r(.12F, -.15F, .10F)), VIEW_CHAMBER),
		new Key(v(.43F, 1.40F, -.72F), r(.05F, -.20F, .04F), r(.07F, -.15F, .025F), r(.04F, -.24F, .03F), r(-.07F, .37F, -.02F),
			arm(r(.03F, -.08F, -.07F), r(-1.14F, -.83F, -.27F), -.48F, r(.13F, -.24F, .42F)),
			arm(r(0, .08F, .04F), r(-.55F, -.34F, -.23F), -.87F, r(.10F, -.10F, .12F)), VIEW_IMPACT),
		new Key(v(.32F, 1.15F, -.40F), r(.035F, -.31F, .04F), r(.04F, -.19F, .035F), r(.03F, -.26F, .04F), r(-.045F, .44F, -.03F),
			arm(r(.03F, -.12F, -.03F), r(-.62F, -1.12F, .10F), -.84F, r(.16F, -.38F, .57F)),
			arm(r(0, .08F, .03F), r(-.44F, -.34F, -.23F), -.80F, r(.08F, -.10F, .10F)), VIEW_FOLLOW),
		v(-2.5F, 22, 1.8F), v(2.5F, 22, -2.0F));

	public static final Pose NONE = new Pose(0, Phase.NONE, bind(), VIEW_BIND);

	/** Impact is exactly the accepted windup tick; unsupported arts deliberately retain their existing renderer. */
	public static Pose sampleSpellcut(int move, float age, int windup, int recovery, boolean leftHanded) {
		if (move != SPELLCUT || !valid(age, windup, 60, recovery) || age >= windup + recovery) return NONE;
		return sample(SPELLCUT_MOTION, age, windup, 1, windup + Math.min(4, recovery * .25F), windup + recovery, leftHanded);
	}

	/** Master attack 1 is the existing SWEEP. Its authoritative tell/active/recovery windows are preserved. */
	public static Pose sampleMaster(int attack, float age, int tell, int active, int recovery, boolean leftHanded) {
		if (attack != MASTER_SWEEP || !valid(age, tell, 80, recovery) || active < 1 || active > 10 || age >= tell + active + recovery) return NONE;
		return sample(SWEEP_MOTION, age, tell, active, tell + active + Math.min(3, recovery * .20F), tell + active + recovery, leftHanded);
	}

	/** Independently authored camera-space arm composition, with the same hand/socket hierarchy. */
	public static ViewPose view(Pose pose, boolean leftHanded) {
		ViewKey key = VIEW_BIND.toward(pose.camera, pose.weight);
		Transform[] local = bind();
		local[Joint.PELVIS.ordinal()] = local[Joint.SPINE.ordinal()] = local[Joint.CHEST.ordinal()] = Transform.IDENTITY;
		putArm(local, false, key.sword); putArm(local, true, key.guard);
		set(local, Joint.RIGHT_SHOULDER, key.right, key.sword.shoulder);
		set(local, Joint.LEFT_SHOULDER, key.left, key.guard.shoulder);
		return new ViewPose(new Pose(pose.weight, pose.phase, leftHanded ? reflect(local) : local, key));
	}

	/**
	 * Camera-independent depth margin for the renderer's bounded free-look viewmodel rotation.
	 * Subtract this from origin.z before rotating; origin.y may also be lowered by margin * .8.
	 * Canonical aim is unchanged. A non-finite offset chooses maximum clearance, never a NaN pose.
	 */
	public static float viewClearance(float yawDelta, float pitchDelta) {
		if (!Float.isFinite(yawDelta) || !Float.isFinite(pitchDelta)) return .25F;
		return .25F * Math.max(clamp(Math.abs(yawDelta) / 25), clamp(Math.abs(pitchDelta) / 20));
	}

	private static Pose sample(Motion motion, float age, int impact, int active, float followAt, int end, boolean leftHanded) {
		float chamberAt = impact * .65F;
		Key key = age < chamberAt ? motion.chamber : age < impact
			? motion.chamber.toward(motion.impact, smooth((age - chamberAt) / (impact - chamberAt)))
			: age < followAt ? motion.impact.toward(motion.follow, smooth((age - impact) / (followAt - impact))) : motion.follow;
		float weight = smooth(age / Math.max(1, chamberAt)) * (1 - smooth((age - followAt) / (end - followAt)));
		Transform[] local = bind(), target = bind();
		set(target, Joint.PELVIS, v(key.shift.x, 12 + key.shift.y, key.shift.z), key.pelvis);
		rotate(target, Joint.SPINE, key.spine); rotate(target, Joint.CHEST, key.chest); rotate(target, Joint.HEAD, key.head);
		putArm(target, false, key.sword); putArm(target, true, key.guard);
		for (Joint joint : Joint.values()) local[joint.ordinal()] = local[joint.ordinal()].toward(target[joint.ordinal()], weight);
		if (weight > 0) {
			plant(local, false, v(-1.9F, 22, 0).toward(motion.rightPlant, weight));
			plant(local, true, v(1.9F, 22, 0).toward(motion.leftPlant, weight));
		}
		Phase phase = age < impact ? Phase.WINDUP : age < impact + active ? Phase.ACTIVE : Phase.RECOVERY;
		return new Pose(weight, phase, leftHanded ? reflect(local) : local, key.view);
	}

	/**
	 * Analytical two-link IK with a forward knee pole. The 6/4-pixel lengths never scale. Projecting
	 * the pole into the hip-to-ankle plane avoids a yaw discontinuity as the bind leg straightens.
	 * Solving after blend, rather than blending solved angles, keeps every sole corner on Y=24.
	 */
	private static void plant(Transform[] local, boolean left, Vec3 ankle) {
		Joint thigh = left ? Joint.LEFT_THIGH : Joint.RIGHT_THIGH;
		Joint shin = left ? Joint.LEFT_SHIN : Joint.RIGHT_SHIN;
		Joint foot = left ? Joint.LEFT_FOOT : Joint.RIGHT_FOOT;
		Matrix pelvis = local[Joint.PELVIS.ordinal()].matrix();
		Vec3 hip = pelvis.transform(thigh.bind.translation());
		Matrix inverse = pelvis.inverseRigid();
		Vec3 delta = inverse.direction(ankle.minus(hip));
		float distance = delta.length();
		// Authored targets are inside the reachable annulus. This bound handles floating-point noise.
		float reach = Math.max(2.000001F, Math.min(10, distance));
		Vec3 direction = delta.unit();
		Vec3 pole = inverse.direction(v(0, 0, -1));
		Vec3 bend = pole.minus(direction.times(pole.dot(direction))).unit();
		float along = (36 + reach * reach - 16) / (2 * reach);
		float height = (float) Math.sqrt(Math.max(0, 36 - along * along));
		Vec3 upper = direction.times(along).plus(bend.times(height)).times(1 / 6F).unit();
		float knee = (float) Math.acos(Math.max(-1, Math.min(1, (reach * reach - 52) / 48)));
		Vec3 xAxis = direction.cross(bend).times(-1).unit();
		Vec3 zAxis = xAxis.cross(upper).unit();
		Rotation hipRotation = Matrix.basis(xAxis, upper, zAxis).rotation();
		rotate(local, thigh, hipRotation);
		rotate(local, shin, r(knee, 0, 0));
		Matrix lower = pelvis.multiply(local[thigh.ordinal()].matrix()).multiply(local[shin.ordinal()].matrix());
		rotate(local, foot, lower.inverseRigid().rotation());
	}

	private static Transform[] bind() {
		Transform[] local = new Transform[Joint.values().length];
		for (Joint joint : Joint.values()) local[joint.ordinal()] = joint.bind;
		return local;
	}
	private static Transform[] reflect(Transform[] local) {
		Transform[] mirrored = new Transform[local.length];
		for (Joint joint : Joint.values()) mirrored[joint.opposite().ordinal()] = local[joint.ordinal()].reflected();
		return mirrored;
	}
	private static Matrix[] worlds(Transform[] local) {
		Matrix[] world = new Matrix[local.length];
		for (Joint joint : Joint.values()) world[joint.ordinal()] = joint.parent == null ? local[joint.ordinal()].matrix()
			: world[joint.parent.ordinal()].multiply(local[joint.ordinal()].matrix());
		return world;
	}
	private static void putArm(Transform[] local, boolean left, Arm arm) {
		rotate(local, left ? Joint.LEFT_SHOULDER : Joint.RIGHT_SHOULDER, arm.shoulder);
		rotate(local, left ? Joint.LEFT_UPPER_ARM : Joint.RIGHT_UPPER_ARM, arm.upper);
		rotate(local, left ? Joint.LEFT_FOREARM : Joint.RIGHT_FOREARM, r(arm.elbow, 0, 0));
		rotate(local, left ? Joint.LEFT_HAND : Joint.RIGHT_HAND, arm.wrist);
		rotate(local, left ? Joint.LEFT_SOCKET : Joint.RIGHT_SOCKET, arm.socket);
	}
	private static void set(Transform[] local, Joint joint, Vec3 point, Rotation rotation) {
		local[joint.ordinal()] = new Transform(point.x, point.y, point.z, rotation);
	}
	private static void rotate(Transform[] local, Joint joint, Rotation rotation) {
		Transform p = local[joint.ordinal()]; local[joint.ordinal()] = new Transform(p.x, p.y, p.z, rotation);
	}
	private static boolean valid(float age, int tell, int maxTell, int recovery) {
		return Float.isFinite(age) && age >= 0 && tell > 0 && tell <= maxTell && recovery > 0 && recovery <= 120;
	}
	private static Arm arm(Rotation shoulder, Rotation upper, float elbow, Rotation authoredGrip) {
		// A cuboid hand has limited flexion clearance at its forearm. Keep its actual wrist subtle,
		// and place the remaining independent blade attitude on the child socket, not the hand mesh.
		Rotation wrist = r(authoredGrip.x * .25F, authoredGrip.y * .25F, authoredGrip.z * .25F);
		Matrix wristInverse = new Transform(0, 0, 0, wrist).matrix().inverseRigid();
		Rotation socket = wristInverse.multiply(new Transform(0, 0, 0, authoredGrip).matrix()).rotation();
		return new Arm(shoulder, upper, elbow, wrist, socket);
	}
	private static Rotation r(float x, float y, float z) { return new Rotation(x, y, z); }
	private static Vec3 v(float x, float y, float z) { return new Vec3(x, y, z); }
	private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
	private static float clamp(float t) { return Float.isFinite(t) ? Math.max(0, Math.min(1, t)) : 0; }
	private static float smooth(float t) { t = clamp(t); return t * t * (3 - 2 * t); }

	private record Quaternion(float x, float y, float z, float w) {
		static Quaternion of(Rotation r) {
			float sx = (float) Math.sin(r.x / 2), cx = (float) Math.cos(r.x / 2);
			float sy = (float) Math.sin(r.y / 2), cy = (float) Math.cos(r.y / 2);
			float sz = (float) Math.sin(r.z / 2), cz = (float) Math.cos(r.z / 2);
			return new Quaternion(sx * cy * cz - cx * sy * sz, cx * sy * cz + sx * cy * sz,
				cx * cy * sz - sx * sy * cz, cx * cy * cz + sx * sy * sz);
		}
		Quaternion toward(Quaternion b, float t) {
			float dot = x * b.x + y * b.y + z * b.z + w * b.w;
			if (dot < 0) { b = new Quaternion(-b.x, -b.y, -b.z, -b.w); dot = -dot; }
			float aWeight = 1 - t, bWeight = t;
			if (dot < .9995F) {
				float angle = (float) Math.acos(Math.min(1, dot)), sin = (float) Math.sin(angle);
				aWeight = (float) Math.sin((1 - t) * angle) / sin; bWeight = (float) Math.sin(t * angle) / sin;
			}
			float xx = x * aWeight + b.x * bWeight, yy = y * aWeight + b.y * bWeight;
			float zz = z * aWeight + b.z * bWeight, ww = w * aWeight + b.w * bWeight;
			float inverse = 1 / (float) Math.sqrt(xx * xx + yy * yy + zz * zz + ww * ww);
			return new Quaternion(xx * inverse, yy * inverse, zz * inverse, ww * inverse);
		}
		Matrix matrix() {
			return Matrix.basis(v(1 - 2 * (y * y + z * z), 2 * (x * y + z * w), 2 * (x * z - y * w)),
				v(2 * (x * y - z * w), 1 - 2 * (x * x + z * z), 2 * (y * z + x * w)),
				v(2 * (x * z + y * w), 2 * (y * z - x * w), 1 - 2 * (x * x + y * y)));
		}
	}
}
