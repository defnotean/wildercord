package dev.wildercord.aura;

import dev.wildercord.aura.world.GaleRepriseRules;
import dev.wildercord.aura.world.MasterAnimationRules;
import dev.wildercord.aura.world.StoneFractureRules;
import dev.wildercord.aura.world.StoneMarchRules;

/**
 * Original, Minecraft-independent articulated shared-player arts and selected sword-master choreography.
 * Model units are pixels, +Y is down, and -Z is forward. All transforms are local to the named
 * parent, include their bind translation, and are already blended. This class cannot move an
 * entity, choose a target, alter a camera, or make a damage/reach decision.
 */
public final class ArticulatedCombatPose {
	private ArticulatedCombatPose() {}

	// Player activation ordinals and NPC attack ordinals are separate namespaces.
	public static final int SPELLCUT = 0, RISING_BREAK = 1, DRIVING_CUT = 2;
	public static final int KINDLING_DRAW = 3, FROSTBITE = 4;
	public static final int HAILFALL = 15, SKYFALL = 16;
	public static final int COLLAPSE = 17, RED_RAIN = 18;
	public static final int CRIMSON_MOON = 19;
	public static final int BACKDRAFT = 20, ROOTED_PARRY = 21;
	public static final int GLACIER_MIRROR = 22, STATIC_RIPOSTE = 23;
	public static final int UNMOVED = 24, NULL_PARRY = 25;
	public static final int EYE_OF_THE_STORM = 26, SANGUINE_PARRY = 27;
	public static final int CONSTELLATION_GUARD = 28, STOPPED_MOMENT = 29;
	public static final int MASTER_SWEEP = 1, MASTER_CROSSWIND_REPRISE = 7, MASTER_STONE_FRACTURE = 8, MASTER_EMBER_KILN_RING = 9, MASTER_STONE_FAULT_MARCH = 10;
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

	// Rising Break coils low, opens the knees/hips and lifts its independent elbow into a high
	// diagonal finish. Its bounded hip drop keeps the stock armor knee collars unfolded.
	// The two ankle targets never leave the ground; no jump is implied.
	private static final Motion RISING_MOTION = new Motion(
		new Key(v(-.25F, 1.10F, .25F), r(.12F, .23F, -.035F), r(.08F, .12F, -.025F), r(.06F, .16F, -.045F), r(-.15F, -.29F, .035F),
			arm(r(.03F, .06F, -.08F), r(-.35F, .20F, -.25F), -.85F, r(.85F, -.12F, -.36F)),
			arm(r(0, -.08F, .06F), r(-.80F, -.28F, -.23F), -1.00F, r(.10F, -.12F, .10F)),
			new ViewKey(v(-7.8F, -5.6F, 3.3F), arm(r(.02F, .04F, -.04F), r(-.95F, .10F, -.20F), -.92F, r(.50F, -.12F, -.25F)),
				v(7.4F, -6.1F, 3.7F), arm(r(0, -.03F, .03F), r(-1.00F, .10F, .25F), -.88F, r(.10F, -.10F, .10F)))),
		new Key(v(.20F, .29F, -.05F), r(-.055F, -.16F, .025F), r(-.04F, -.10F, .025F), r(-.07F, -.19F, .035F), r(.06F, .28F, -.025F),
			arm(r(.02F, -.06F, -.04F), r(-2.05F, -.20F, -.36F), -.48F, r(.75F, .08F, .35F)),
			arm(r(0, .05F, .04F), r(-.48F, -.23F, -.28F), -.82F, r(.10F, -.10F, .10F)),
			new ViewKey(v(-8.4F, -4.1F, 2.8F), arm(r(.02F, -.05F, -.04F), r(-1.65F, -.20F, -.27F), -.40F, r(.42F, .08F, .30F)),
				v(7.4F, -6.1F, 3.7F), arm(r(0, .03F, .03F), r(-.90F, .12F, .29F), -.86F, r(.10F, -.10F, .10F)))),
		new Key(v(.16F, 1.10F, -.18F), r(-.02F, -.23F, .02F), r(-.02F, -.12F, .025F), r(-.04F, -.20F, .03F), r(.04F, .34F, -.025F),
			arm(r(.02F, -.08F, -.03F), r(-2.24F, -.35F, -.46F), -.76F, r(.55F, .16F, .50F)),
			arm(r(0, .05F, .04F), r(-.38F, -.25F, -.25F), -.78F, r(.08F, -.10F, .08F)),
			new ViewKey(v(-9.2F, -4.6F, 3.0F), arm(r(.02F, -.07F, -.03F), r(-1.52F, -.35F, -.18F), -.64F, r(.38F, .12F, .40F)),
				v(7.6F, -6.1F, 3.8F), arm(r(0, .04F, .03F), r(-.88F, .06F, .26F), -.86F, r(.10F, -.10F, .10F)))),
		v(-2.50F, 22, 1.6F), v(2.50F, 22, -1.7F));

	// Driving Cut retracts the point beside the ribs, commits the pelvis/chest toward the
	// planted lead leg, then recoils through its elbow. The shallow hip drop retains the
	// existing netherite knee envelope. These shifts are model-local only.
	private static final Motion DRIVING_MOTION = new Motion(
		new Key(v(-.18F, 1.30F, .38F), r(-.045F, .24F, .025F), r(-.035F, .10F, .015F), r(-.035F, .13F, .02F), r(.05F, -.36F, -.015F),
			arm(r(.02F, .08F, -.05F), r(-.95F, .27F, -.24F), -1.10F, r(1.90F, -.64F, -.97F)),
			arm(r(0, -.06F, .05F), r(-.74F, -.27F, -.22F), -.96F, r(.10F, -.12F, .08F)),
			new ViewKey(v(-7.5F, -4.9F, 4.3F), arm(r(.02F, .06F, -.04F), r(-1.05F, .16F, -.26F), -.97F, r(1.555F, -.339F, -.458F)),
				v(7.2F, -6.1F, 3.6F), arm(r(0, -.04F, .03F), r(-.98F, .12F, .28F), -.90F, r(.10F, -.10F, .10F)))),
		new Key(v(.20F, 1.25F, -1.00F), r(.14F, -.12F, -.015F), r(.08F, -.08F, -.01F), r(.09F, -.13F, -.015F), r(-.18F, .25F, .01F),
			arm(r(.025F, -.06F, -.035F), r(-1.36F, -.04F, -.09F), -.25F, r(1.11F, .34F, .30F)),
			arm(r(0, .07F, .04F), r(.08F, -.22F, -.30F), -.78F, r(.10F, -.10F, .10F)),
			new ViewKey(v(-7.2F, -5.5F, 1.5F), arm(r(.02F, -.04F, -.04F), r(-1.28F, -.14F, -.15F), -.30F, r(1.040F, .074F, .015F)),
				v(7.6F, -6.1F, 3.8F), arm(r(0, .04F, .03F), r(-.87F, .12F, .28F), -.88F, r(.10F, -.10F, .10F)))),
		new Key(v(.10F, 1.45F, -.35F), r(.06F, -.18F, -.01F), r(.04F, -.09F, -.01F), r(.035F, -.14F, -.01F), r(-.08F, .28F, .01F),
			arm(r(.02F, -.07F, -.03F), r(-.92F, -.11F, -.18F), -.78F, r(1.15F, .31F, .32F)),
			arm(r(0, .06F, .04F), r(-.34F, -.23F, -.25F), -.84F, r(.08F, -.10F, .10F)),
			new ViewKey(v(-8.1F, -5.2F, 3.2F), arm(r(.02F, -.06F, -.03F), r(-1.11F, -.18F, -.22F), -.78F, r(1.464F, .134F, .077F)),
				v(7.5F, -6.1F, 3.7F), arm(r(0, .04F, .03F), r(-.90F, .08F, .26F), -.88F, r(.10F, -.10F, .10F)))),
		v(-2.55F, 22, 2.0F), v(2.55F, 22, -2.2F));

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

	// Gale's root still moves only on the server. These compact hip/ankle offsets depict one
	// lateral shuffle, then a planted point reply; they do not add to its accepted travel distance.
	private static final Key REPRISE_GATHER = new Key(v(-.2F, 1.2F, .10F), r(.025F, -.12F, .045F), r(.015F, -.06F, .02F), r(-.03F, -.12F, .035F), r(0, .22F, -.04F),
		arm(r(.02F, .10F, -.08F), r(-1.40F, .25F, -.45F), -.95F, r(1.36F, -.32F, -.36F)),
		arm(r(0, -.08F, .04F), r(-.65F, -.20F, -.20F), -.85F, r(.10F, -.12F, .08F)), VIEW_BIND);
	private static final Key REPRISE_STEP = new Key(v(-.65F, 1.75F, .10F), r(.035F, -.19F, .12F), r(.015F, .04F, -.025F), r(-.04F, -.13F, -.085F), r(-.02F, .25F, -.03F),
		arm(r(.02F, .12F, -.10F), r(-1.45F, .38F, -.50F), -.90F, r(1.31F, -.48F, -.53F)),
		arm(r(0, -.08F, .06F), r(-.45F, -.25F, -.30F), -.72F, r(.08F, -.15F, .10F)), VIEW_BIND);
	private static final Motion REPRISE_MOTION = new Motion(
		new Key(v(-.28F, 1.35F, .18F), r(-.025F, .22F, .025F), r(-.025F, .10F, .015F), r(-.04F, .12F, .015F), r(.05F, -.36F, -.02F),
			arm(r(.02F, .08F, -.04F), r(-1.15F, .26F, -.28F), -.92F, r(1.74F, -.65F, -1.05F)),
			arm(r(0, -.06F, .04F), r(-.72F, -.24F, -.19F), -.95F, r(.10F, -.12F, .08F)), VIEW_BIND),
		new Key(v(.18F, 1.45F, -.80F), r(.11F, -.10F, -.015F), r(.07F, -.06F, -.01F), r(.075F, -.12F, -.01F), r(-.14F, .22F, .01F),
			arm(r(.02F, -.05F, -.03F), r(-1.40F, -.05F, -.10F), -.28F, r(1.18F, .29F, .28F)),
			arm(r(0, .07F, .03F), r(.12F, -.20F, -.27F), -.76F, r(.10F, -.10F, .10F)), VIEW_BIND),
		new Key(v(.12F, 1.30F, -.35F), r(.055F, -.19F, -.01F), r(.04F, -.08F, -.01F), r(.04F, -.14F, -.01F), r(-.07F, .28F, .01F),
			arm(r(.02F, -.07F, -.03F), r(-.95F, -.12F, -.17F), -.68F, r(1.11F, .37F, .33F)),
			arm(r(0, .06F, .03F), r(-.30F, -.20F, -.24F), -.78F, r(.08F, -.10F, .10F)), VIEW_BIND),
		v(-2.55F, 22, 1.7F), v(2.55F, 22, -1.8F));

	// Stone holds both ankles through its brace, then visibly lifts an overhead warning.
	// The spine and elbows close into the narrow ground reply while the feet keep their plant.
	private static final Key FRACTURE_PLANT = new Key(v(0, 1.45F, .1F), r(.04F, .06F, 0), r(.025F, .04F, 0), r(.025F, .08F, -.015F), r(-.06F, -.12F, 0),
		arm(r(.015F, -.07F, -.04F), r(-.65F, -.36F, -.25F), -.82F, r(.90F, -.22F, -.22F)),
		arm(r(0, .06F, .04F), r(-.70F, .20F, -.18F), -.86F, r(.08F, -.10F, .10F)), VIEW_BIND);
	private static final Key FRACTURE_BRACE = new Key(v(0, 2.25F, .25F), r(.075F, .055F, 0), r(.055F, .04F, -.01F), r(.07F, .065F, -.015F), r(-.12F, -.13F, .015F),
		arm(r(.015F, -.08F, -.05F), r(-1.10F, -.45F, -.30F), -1.05F, r(1.24F, -.26F, -.27F)),
		arm(r(0, .07F, .04F), r(-.88F, .32F, -.18F), -1.05F, r(.10F, -.12F, .10F)), VIEW_BIND);
	private static final Motion FRACTURE_MOTION = new Motion(
		new Key(v(0, 1.55F, .30F), r(-.055F, .04F, 0), r(-.045F, .035F, 0), r(-.07F, .045F, -.015F), r(.08F, -.08F, .01F),
			arm(r(.02F, .05F, -.035F), r(-2.15F, .10F, -.28F), -.68F, r(1.105F, -.29F, -.26F)),
			arm(r(0, .04F, .035F), r(-1.26F, .22F, -.22F), -.95F, r(.08F, -.10F, .10F)), VIEW_BIND),
		new Key(v(0, 2.00F, -.68F), r(.17F, -.025F, .01F), r(.11F, -.015F, .005F), r(.15F, -.025F, .01F), r(-.21F, .04F, -.01F),
			arm(r(.035F, -.025F, -.035F), r(-.74F, -.06F, -.12F), -.38F, r(1.26F, .14F, .15F)),
			arm(r(0, .05F, .03F), r(-.62F, .13F, -.22F), -.82F, r(.08F, -.10F, .10F)), VIEW_BIND),
		new Key(v(0, 1.90F, -.30F), r(.12F, -.04F, .01F), r(.075F, -.025F, .005F), r(.10F, -.045F, .01F), r(-.15F, .06F, -.01F),
			arm(r(.025F, -.04F, -.03F), r(-.46F, -.08F, -.14F), -.62F, r(1.22F, .09F, .09F)),
			arm(r(0, .04F, .03F), r(-.36F, .12F, -.20F), -.85F, r(.08F, -.10F, .10F)), VIEW_BIND),
		v(-2.75F, 22, 1.8F), v(2.75F, 22, -1.8F));

	// Kiln coils through a whole model-space turn with a low, close blade. The release opens
	// only after both feet settle, in time with the single radial ignition. This is an NPC clip;
	// VIEW_BIND is inert metadata and no player move or camera composition is admitted for it.
	private static final Motion KILN_MOTION = new Motion(
		new Key(v(0, 2.70F, 0), r(.10F, .16F, 0), r(.07F, .13F, -.015F), r(.06F, .18F, -.025F), r(-.12F, -.32F, .015F),
			arm(r(.015F, .08F, -.035F), r(.08F, .40F, .30F), -.40F, r(.018758F, .385363F, -.290471F)),
			arm(r(0, -.06F, .035F), r(-.74F, -.26F, .22F), -.95F, r(.10F, -.10F, .10F)), VIEW_BIND),
		new Key(v(0, 2.80F, 0), r(.13F, -.18F, .015F), r(.085F, -.12F, .01F), r(.07F, -.23F, .025F), r(-.15F, .36F, -.015F),
			arm(r(.025F, -.06F, -.035F), r(-.08F, -.72F, -.58F), -.28F, r(.266354F, -.311736F, .627197F)),
			arm(r(0, .065F, .03F), r(-.42F, .22F, .48F), -.72F, r(.10F, -.10F, .12F)), VIEW_BIND),
		new Key(v(0, 2.55F, 0), r(.10F, -.27F, .01F), r(.06F, -.16F, .01F), r(.05F, -.27F, .02F), r(-.11F, .44F, -.015F),
			arm(r(.02F, -.08F, -.025F), r(.12F, -.96F, -.45F), -.54F, r(.468118F, -.221315F, .442651F)),
			arm(r(0, .05F, .025F), r(-.34F, .18F, .35F), -.82F, r(.08F, -.10F, .10F)), VIEW_BIND),
		v(-2.6F, 22, 1.1F), v(2.6F, 22, -1.1F));

	// Fault March keeps a fixed horizontal heading and both feet planted. Its overhead lever
	// becomes a sunk blade, followed by two small braced shocks and an exposed extraction.
	// Every arm has its own shoulder/elbow/wrist; the item uses the existing child hand socket.
	private static final Key MARCH_GATHER = new Key(v(0, 1.25F, .12F), r(.02F, 0, 0), r(-.015F, 0, 0), r(-.035F, 0, 0), r(.03F, 0, 0),
		arm(r(.02F, 0, -.03F), r(-1.28F, .06F, -.16F), -.82F, r(.88F, -.06F, .13F)),
		arm(r(0, .04F, .025F), r(-1.06F, -.15F, .22F), -.90F, r(.10F, -.08F, .10F)), VIEW_BIND);
	private static final Key MARCH_OVERHEAD = new Key(v(0, 1.20F, .22F), r(-.065F, 0, 0), r(-.045F, 0, 0), r(-.08F, 0, 0), r(.10F, 0, 0),
		arm(r(.02F, 0, -.025F), r(-2.52F, 0, -.12F), -.48F, r(1.42F, 0, 0)),
		arm(r(0, .035F, .025F), r(-2.12F, .08F, .20F), -.62F, r(.18F, -.10F, .12F)), VIEW_BIND);
	private static final Key MARCH_STRIKE = new Key(v(0, .85F, -.90F), r(.09F, 0, 0), r(.18F, 0, 0), r(.25F, 0, 0), r(-.28F, 0, 0),
		arm(r(.02F, 0, -.025F), r(-.50F, 0, -.10F), -.30F, r(1.6563F, 0, .10F)),
		arm(r(0, -.04F, .035F), r(-.32F, -.12F, .62F), -.52F, r(.10F, -.10F, .14F)), VIEW_BIND);
	private static final Key MARCH_BRACE = new Key(v(0, .75F, -.64F), r(.075F, 0, 0), r(.12F, 0, 0), r(.175F, 0, 0), r(-.20F, 0, 0),
		arm(r(.015F, 0, -.025F), r(-.60F, 0, -.10F), -.40F, r(2.0113F, 0, .10F)),
		arm(r(0, -.035F, .03F), r(-.58F, -.10F, .45F), -.68F, r(.10F, -.10F, .12F)), VIEW_BIND);
	private static final Key MARCH_RECOIL = new Key(v(0, 1.02F, -.76F), r(.085F, 0, 0), r(.15F, 0, 0), r(.225F, 0, 0), r(-.25F, 0, 0),
		arm(r(.02F, 0, -.025F), r(-.54F, 0, -.10F), -.36F, r(1.8163F, 0, .10F)),
		arm(r(0, -.04F, .035F), r(-.45F, -.16F, .56F), -.60F, r(.10F, -.10F, .14F)), VIEW_BIND);
	private static final Key MARCH_EXTRACT = new Key(v(0, .90F, -.36F), r(.085F, 0, 0), r(.07F, 0, 0), r(.085F, 0, 0), r(-.10F, 0, 0),
		arm(r(.025F, 0, -.035F), r(-.92F, 0, -.22F), -.78F, r(1.76F, 0, .18F)),
		arm(r(0, -.03F, .025F), r(-.16F, -.08F, .62F), -.54F, r(.08F, -.10F, .12F)), VIEW_BIND);
	private static final Vec3 MARCH_RIGHT_PLANT = v(-2.65F, 22, 1.70F), MARCH_LEFT_PLANT = v(2.65F, 22, -1.70F);

	// Kindling gathers the hilt beside the rear hip, opens the elbow into a low draw-cut,
	// then lets the blade and free arm travel outward. The separate ground fire line is
	// the existing performer's effect; neither this planted motion nor its view moves the player.
	private static final Motion KINDLING_MOTION = new Motion(
		new Key(v(-.32F, 1.05F, .28F), r(.09F, .30F, -.035F), r(.065F, .20F, -.025F), r(.05F, .29F, -.04F), r(-.12F, -.47F, .03F),
			arm(r(.02F, .12F, -.06F), r(-.22F, .64F, -.32F), -.80F, r(1.10F, -.36F, -.70F)),
			arm(r(0, -.08F, .04F), r(-.58F, -.30F, -.20F), -.95F, r(.10F, -.12F, .10F)),
			new ViewKey(v(-7.6F, -5.5F, 3.6F), arm(r(.02F, .09F, -.04F), r(-1.00F, .20F, -.28F), -.95F, r(1.273F, -.645F, .416F)),
				v(7.0F, -6.1F, 3.6F), arm(r(0, -.04F, .03F), r(-.98F, .10F, .28F), -.90F, r(.10F, -.10F, .10F)))),
		new Key(v(.36F, 1.10F, -.55F), r(.11F, -.27F, .035F), r(.08F, -.18F, .025F), r(.065F, -.27F, .04F), r(-.13F, .43F, -.03F),
			arm(r(.03F, -.09F, -.04F), r(-.52F, -.74F, -.27F), -.32F, r(1.10F, .27F, .38F)),
			arm(r(0, .08F, .04F), r(-.28F, -.32F, -.45F), -.72F, r(.10F, -.10F, .12F)),
			new ViewKey(v(-8.3F, -5.4F, 2.4F), arm(r(.02F, -.08F, -.04F), r(-1.10F, -.60F, -.36F), -.35F, r(1.370F, -.350F, -.446F)),
				v(7.3F, -6.1F, 3.7F), arm(r(0, .06F, .03F), r(-.90F, .08F, .32F), -.86F, r(.10F, -.10F, .10F)))),
		new Key(v(.25F, 1.00F, -.30F), r(.065F, -.34F, .025F), r(.04F, -.19F, .025F), r(.045F, -.31F, .04F), r(-.09F, .51F, -.025F),
			arm(r(.03F, -.12F, -.03F), r(-.30F, -1.12F, .09F), -.52F, r(1.20F, .24F, .58F)),
			arm(r(0, .07F, .04F), r(-.20F, -.22F, -.55F), -.70F, r(.08F, -.10F, .10F)),
			new ViewKey(v(-10.9F, -5.1F, 2.9F), arm(r(.02F, -.12F, -.03F), r(-1.05F, -.85F, -.18F), -.65F, r(.999F, -.067F, 1.740F)),
				v(7.7F, -6.1F, 3.8F), arm(r(0, .06F, .03F), r(-.86F, .02F, .26F), -.84F, r(.10F, -.10F, .10F)))),
		v(-2.5F, 22, 1.6F), v(2.5F, 22, -1.7F));

	// Frostbite presents a compact low point, makes one measured tilted cut, then closes
	// its guard. The point is a chamber, never an extra thrust or hit: the server releases
	// the existing crusting arc once at windup. Its torso stays quieter than Ember's draw.
	private static final Motion FROSTBITE_MOTION = new Motion(
		new Key(v(-.08F, .88F, .12F), r(.025F, .12F, -.015F), r(.025F, .09F, -.015F), r(.03F, .15F, -.02F), r(-.05F, -.28F, .015F),
			arm(r(.015F, .06F, -.04F), r(-.74F, .26F, -.20F), -.88F, r(1.27F, -.32F, -.44F)),
			arm(r(0, -.05F, .03F), r(-.95F, -.26F, -.16F), -.92F, r(.10F, -.12F, .08F)),
			new ViewKey(v(-7.5F, -5.1F, 3.6F), arm(r(.015F, .04F, -.03F), r(-1.10F, .12F, -.24F), -.90F, r(1.18F, -.20F, -.24F)),
				v(7.1F, -6.1F, 3.5F), arm(r(0, -.03F, .03F), r(-1.02F, .08F, .25F), -.92F, r(.10F, -.10F, .10F)))),
		new Key(v(.12F, .91F, -.35F), r(.035F, -.14F, .02F), r(.025F, -.09F, .015F), r(.04F, -.17F, .025F), r(-.06F, .29F, -.02F),
			arm(r(.025F, -.05F, -.035F), r(-.93F, -.52F, -.18F), -.42F, r(.97F, .19F, .38F)),
			arm(r(0, .05F, .03F), r(-.90F, -.20F, -.20F), -.90F, r(.10F, -.10F, .10F)),
			new ViewKey(v(-7.7F, -5.1F, 2.3F), arm(r(.02F, -.04F, -.035F), r(-1.17F, -.38F, -.28F), -.42F, r(.91F, .05F, .35F)),
				v(7.1F, -6.1F, 3.5F), arm(r(0, .03F, .03F), r(-.96F, .10F, .27F), -.92F, r(.10F, -.10F, .10F)))),
		new Key(v(.04F, .87F, -.10F), r(.02F, -.08F, .01F), r(.02F, -.05F, .01F), r(.025F, -.09F, .015F), r(-.04F, .16F, -.01F),
			arm(r(.02F, -.03F, -.03F), r(-.80F, -.21F, -.18F), -.95F, r(1.14F, .02F, .18F)),
			arm(r(0, .03F, .03F), r(-1.04F, -.24F, -.17F), -.94F, r(.10F, -.10F, .08F)),
			new ViewKey(v(-7.9F, -5.0F, 3.5F), arm(r(.015F, -.025F, -.03F), r(-1.06F, -.16F, -.22F), -.93F, r(1.08F, -.06F, .14F)),
				v(7.1F, -6.1F, 3.5F), arm(r(0, .02F, .03F), r(-1.02F, .08F, .25F), -.92F, r(.10F, -.10F, .10F)))),
		v(-2.25F, 22, 1.2F), v(2.25F, 22, -1.4F));

	// Hailfall releases one controlled upward cut from a folded Rime guard. Its high finish
	// settles while the separate cloud continues; knees and soles never imply a caster leap.
	private static final Motion HAILFALL_MOTION = new Motion(
		new Key(v(-.08F, .94F, .12F), r(.035F, .12F, -.02F), r(.025F, .08F, -.015F), r(.03F, .13F, -.02F), r(-.06F, -.25F, .015F),
			arm(r(.015F, .05F, -.035F), r(-.86F, .24F, -.20F), -.98F, r(1.02F, -.23F, -.28F)),
			arm(r(0, -.04F, .03F), r(-1.00F, -.24F, -.18F), -.96F, r(.10F, -.10F, .08F)),
			new ViewKey(v(-7.8F, -5.3F, 3.5F), arm(r(.015F, .04F, -.03F), r(-1.06F, .10F, -.24F), -.90F, r(1.02F, -.16F, -.18F)),
				v(7.2F, -6.1F, 3.6F), arm(r(0, -.03F, .03F), r(-1.01F, .08F, .25F), -.92F, r(.10F, -.10F, .10F)))),
		new Key(v(.10F, .68F, -.22F), r(-.025F, -.10F, .015F), r(-.02F, -.07F, .01F), r(-.035F, -.13F, .025F), r(.04F, .22F, -.015F),
			arm(r(.025F, -.045F, -.035F), r(-1.86F, -.18F, -.30F), -.40F, r(.53F, .06F, .22F)),
			arm(r(0, .04F, .03F), r(-.76F, -.21F, -.22F), -.90F, r(.10F, -.10F, .10F)),
			new ViewKey(v(-8.2F, -4.5F, 2.6F), arm(r(.02F, -.04F, -.03F), r(-1.53F, -.14F, -.24F), -.42F, r(.43F, .06F, .24F)),
				v(7.2F, -6.1F, 3.6F), arm(r(0, .03F, .03F), r(-.95F, .10F, .28F), -.90F, r(.10F, -.10F, .10F)))),
		new Key(v(.07F, .85F, -.10F), r(-.02F, -.12F, .01F), r(-.015F, -.08F, .01F), r(-.03F, -.14F, .015F), r(.035F, .25F, -.01F),
			arm(r(.02F, -.05F, -.03F), r(-2.14F, -.24F, -.34F), -.62F, r(.40F, .10F, .30F)),
			arm(r(0, .03F, .025F), r(-.94F, -.23F, -.18F), -.96F, r(.10F, -.10F, .08F)),
			new ViewKey(v(-8.8F, -4.5F, 2.9F), arm(r(.015F, -.05F, -.03F), r(-1.57F, -.24F, -.20F), -.62F, r(.35F, .10F, .32F)),
				v(7.3F, -6.1F, 3.6F), arm(r(0, .02F, .03F), r(-1.00F, .08F, .25F), -.92F, r(.10F, -.10F, .10F)))),
		v(-2.30F, 22, 1.25F), v(2.30F, 22, -1.45F));

	// Skyfall presents the blade to the sky above a braced base. The off hand opens at
	// release, the blade holds, then the elbow answers the bolt exactly six ticks later.
	private static final Motion SKYFALL_MOTION = new Motion(
		new Key(v(-.12F, .96F, .12F), r(-.025F, .07F, -.02F), r(-.035F, .04F, -.015F), r(-.045F, .08F, -.02F), r(-.05F, -.14F, .015F),
			arm(r(.03F, .04F, -.04F), r(-2.18F, .10F, -.28F), -.56F, r(.40F, -.05F, -.12F)),
			arm(r(0, -.04F, .03F), r(-.78F, -.26F, -.24F), -.92F, r(.10F, -.10F, .08F)),
			new ViewKey(v(-8.4F, -4.4F, 2.9F), arm(r(.02F, .025F, -.03F), r(-1.55F, .06F, -.24F), -.64F, r(.28F, -.04F, -.10F)),
				v(7.4F, -6.1F, 3.7F), arm(r(0, -.03F, .03F), r(-.96F, .10F, .27F), -.90F, r(.10F, -.10F, .10F)))),
		new Key(v(.04F, 1.02F, -.12F), r(-.04F, -.025F, -.015F), r(-.035F, -.015F, -.01F), r(-.055F, -.04F, -.015F), r(-.025F, .065F, .01F),
			arm(r(.04F, -.02F, -.025F), r(-2.48F, .025F, -.22F), -.34F, r(.25F, .025F, .08F)),
			arm(r(0, .045F, .03F), r(-.48F, -.21F, -.40F), -.76F, r(.08F, -.10F, .12F)),
			new ViewKey(v(-8.8F, -4.0F, 2.6F), arm(r(.025F, -.025F, -.03F), r(-1.72F, -.045F, -.22F), -.36F, r(.19F, .02F, .08F)),
				v(7.7F, -6.1F, 3.8F), arm(r(0, .035F, .03F), r(-.87F, .08F, .30F), -.85F, r(.10F, -.10F, .10F)))),
		new Key(v(.02F, 1.00F, -.08F), r(.035F, -.07F, .015F), r(.025F, -.035F, .01F), r(.025F, -.055F, .015F), r(-.045F, .12F, -.01F),
			arm(r(.02F, -.025F, -.03F), r(-1.18F, -.13F, -.22F), -.88F, r(.83F, .04F, .15F)),
			arm(r(0, .025F, .025F), r(-.86F, -.22F, -.20F), -.94F, r(.10F, -.10F, .08F)),
			new ViewKey(v(-8.2F, -5.0F, 3.2F), arm(r(.015F, -.025F, -.03F), r(-1.16F, -.10F, -.23F), -.82F, r(.85F, .025F, .13F)),
				v(7.4F, -6.1F, 3.7F), arm(r(0, .025F, .03F), r(-.98F, .08F, .26F), -.90F, r(.10F, -.10F, .10F)))),
		v(-2.40F, 22, 1.50F), v(2.40F, 22, -1.60F));
	private static final Key SKYFALL_ANSWER = new Key(v(.10F, 1.24F, -.26F), r(.085F, -.08F, .02F), r(.045F, -.045F, .015F),
		r(.065F, -.07F, .02F), r(-.10F, .15F, -.015F),
		arm(r(.025F, -.035F, -.03F), r(-.98F, -.16F, -.22F), -.54F, r(.72F, .07F, .20F)),
		arm(r(0, .04F, .03F), r(-.40F, -.24F, -.37F), -.82F, r(.10F, -.10F, .10F)),
		new ViewKey(v(-8.2F, -5.25F, 2.4F), arm(r(.02F, -.035F, -.03F), r(-1.16F, -.12F, -.24F), -.45F, r(.75F, .04F, .18F)),
			v(7.7F, -6.1F, 3.8F), arm(r(0, .03F, .03F), r(-.88F, .08F, .28F), -.88F, r(.10F, -.10F, .10F))));

	// Collapse carries a near-vertical drive through pelvis, spine and both elbows. The planted
	// stance absorbs the stop; the fissure's later pulses belong to the field, never this rig.
	private static final Motion COLLAPSE_MOTION = new Motion(
		new Key(v(-.02F, 1.05F, .18F), r(-.035F, .035F, -.01F), r(-.04F, .025F, -.005F), r(-.045F, .035F, -.01F), r(.06F, -.07F, .005F),
			arm(r(.025F, .035F, -.035F), r(-2.20F, .08F, -.22F), -.65F, r(.56F, -.06F, -.14F)),
			arm(r(0, -.03F, .03F), r(-1.32F, -.18F, -.20F), -.98F, r(.10F, -.10F, .08F)),
			new ViewKey(v(-8.4F, -4.35F, 2.9F), arm(r(.02F, .025F, -.03F), r(-1.60F, .035F, -.23F), -.66F, r(.40F, -.04F, -.12F)),
				v(7.1F, -6.1F, 3.5F), arm(r(0, -.03F, .03F), r(-1.04F, .06F, .25F), -.94F, r(.10F, -.10F, .10F)))),
		new Key(v(.03F, 1.10F, -.52F), r(.14F, -.02F, .005F), r(.09F, -.015F, .005F), r(.105F, -.03F, .01F), r(-.19F, .05F, -.01F),
			arm(r(.035F, -.02F, -.035F), r(-.70F, -.06F, -.15F), -.42F, r(1.30F, .06F, .16F)),
			arm(r(0, .035F, .03F), r(-.87F, -.16F, -.18F), -.85F, r(.10F, -.10F, .10F)),
			new ViewKey(v(-8.1F, -5.65F, 2.4F), arm(r(.02F, -.025F, -.03F), r(-1.02F, -.07F, -.24F), -.44F, r(0, .40F, -.15F)),
				v(7.1F, -6.1F, 3.5F), arm(r(0, .025F, .03F), r(-.96F, .08F, .27F), -.90F, r(.10F, -.10F, .10F)))),
		new Key(v(.02F, 1.05F, -.28F), r(.10F, -.03F, .005F), r(.065F, -.02F, .005F), r(.075F, -.04F, .01F), r(-.14F, .065F, -.01F),
			arm(r(.025F, -.03F, -.03F), r(-.48F, -.08F, -.14F), -.70F, r(1.22F, .05F, .14F)),
			arm(r(0, .025F, .025F), r(-.73F, -.18F, -.18F), -.92F, r(.10F, -.10F, .08F)),
			new ViewKey(v(-8.2F, -5.4F, 3.0F), arm(r(.015F, -.025F, -.03F), r(-1.04F, -.09F, -.23F), -.74F, r(.15F, .25F, -.08F)),
				v(7.2F, -6.1F, 3.6F), arm(r(0, .02F, .03F), r(-1.01F, .07F, .25F), -.92F, r(.10F, -.10F, .10F)))),
		v(-2.55F, 22, 1.55F), v(2.55F, 22, -1.65F));

	// Red Rain's coiled outside shoulder unwinds into one descending rake. Its open guard
	// counterbalances the cross-body blade while the separate pool continues after recovery.
	private static final Motion RED_RAIN_MOTION = new Motion(
		new Key(v(-.23F, 1.03F, .16F), r(.05F, .22F, -.03F), r(.035F, .16F, -.025F), r(.035F, .24F, -.04F), r(-.07F, -.39F, .03F),
			arm(r(.025F, .09F, -.05F), r(-1.95F, .48F, -.48F), -.85F, r(.62F, -.22F, -.42F)),
			arm(r(0, -.06F, .04F), r(-.79F, -.29F, -.22F), -.94F, r(.10F, -.12F, .08F)),
			new ViewKey(v(-7.7F, -4.7F, 3.1F), arm(r(.02F, .06F, -.035F), r(-1.43F, .20F, -.30F), -.84F, r(.62F, -.12F, -.25F)),
				v(7.3F, -6.1F, 3.6F), arm(r(0, -.035F, .03F), r(-1.00F, .08F, .26F), -.92F, r(.10F, -.10F, .10F)))),
		new Key(v(.25F, 1.10F, -.48F), r(.115F, -.21F, .035F), r(.075F, -.15F, .025F), r(.085F, -.24F, .04F), r(-.15F, .38F, -.03F),
			arm(r(.035F, -.075F, -.04F), r(-.71F, -.76F, -.28F), -.38F, r(1.13F, .22F, .48F)),
			arm(r(0, .065F, .04F), r(-.30F, -.24F, -.47F), -.77F, r(.10F, -.10F, .12F)),
			new ViewKey(v(-8.3F, -5.4F, 2.4F), arm(r(.02F, -.065F, -.035F), r(-1.08F, -.48F, -.31F), -.42F, r(-.15F, .65F, .45F)),
				v(7.6F, -6.1F, 3.8F), arm(r(0, .045F, .03F), r(-.89F, .08F, .29F), -.87F, r(.10F, -.10F, .10F)))),
		new Key(v(.18F, 1.05F, -.25F), r(.075F, -.28F, .025F), r(.05F, -.18F, .02F), r(.055F, -.29F, .03F), r(-.10F, .47F, -.025F),
			arm(r(.025F, -.10F, -.03F), r(-.42F, -.98F, -.10F), -.66F, r(1.18F, .22F, .57F)),
			arm(r(0, .05F, .03F), r(-.43F, -.22F, -.38F), -.84F, r(.08F, -.10F, .10F)),
			new ViewKey(v(-9.4F, -5.1F, 2.9F), arm(r(.015F, -.08F, -.03F), r(-1.06F, -.62F, -.22F), -.76F, r(.12F, .55F, .58F)),
				v(7.6F, -6.1F, 3.8F), arm(r(0, .035F, .03F), r(-.96F, .06F, .27F), -.90F, r(.10F, -.10F, .10F)))),
		v(-2.45F, 22, 1.45F), v(2.45F, 22, -1.65F));

	// One original low, broad sweep. The body and view use separate keys; no released wound
	// or expanding ground crescent can restart the physical blade after its accepted release.
	private static final Motion CRIMSON_MOON_MOTION = new Motion(
		new Key(v(-.28F, 1.10F, .14F), r(.065F, .32F, -.025F), r(.04F, .20F, -.02F), r(.045F, .25F, -.035F), r(-.08F, -.48F, .025F),
			arm(r(.025F, .10F, -.04F), r(-.38F, .38F, .22F), -.60F, r(1.05F, -.25F, -.62F)),
			arm(r(0, -.07F, .04F), r(-.66F, -.28F, -.37F), -.94F, r(.12F, -.12F, .10F)),
			new ViewKey(v(-7.8F, -5.3F, 3.2F), arm(r(.02F, .065F, -.035F), r(-1.08F, .28F, -.24F), -.85F, r(.65F, -.28F, -.42F)),
				v(7.4F, -6.1F, 3.7F), arm(r(0, -.04F, .03F), r(-1.00F, .08F, .28F), -.92F, r(.10F, -.10F, .10F)))),
		new Key(v(.25F, 1.04F, -.38F), r(.055F, -.20F, .025F), r(.035F, -.14F, .02F), r(.045F, -.28F, .035F), r(-.07F, .40F, -.025F),
			arm(r(.035F, -.08F, -.035F), r(-1.16F, -.95F, -.06F), -.35F, r(.78F, .28F, .55F)),
			arm(r(0, .075F, .045F), r(-.22F, .20F, -.90F), -.70F, r(.10F, -.10F, .14F)),
			new ViewKey(v(-7.9F, -4.95F, 2.5F), arm(r(.025F, -.065F, -.035F), r(-1.22F, -.56F, -.32F), -.40F, r(.25F, .52F, .50F)),
				v(7.8F, -6.1F, 3.8F), arm(r(0, .05F, .03F), r(-.88F, .14F, .32F), -.86F, r(.10F, -.10F, .12F)))),
		new Key(v(.18F, .98F, -.20F), r(.04F, -.28F, .02F), r(.025F, -.19F, .02F), r(.03F, -.33F, .03F), r(-.05F, .51F, -.02F),
			arm(r(.025F, -.10F, -.025F), r(-1.32F, -1.16F, .02F), -.60F, r(.86F, .30F, .62F)),
			arm(r(0, .055F, .035F), r(-.34F, .16F, -.74F), -.84F, r(.08F, -.10F, .12F)),
			new ViewKey(v(-8.8F, -5.15F, 2.8F), arm(r(.02F, -.08F, -.03F), r(-1.14F, -.68F, -.25F), -.74F, r(.30F, .48F, .60F)),
				v(7.8F, -6.1F, 3.8F), arm(r(0, .035F, .03F), r(-.94F, .10F, .30F), -.90F, r(.10F, -.10F, .10F)))),
		v(-2.75F, 22, 1.55F), v(2.75F, 22, -1.75F));

	// Backdraft keeps a braced rib-height guard and uses elbow extension for one short point.
	// Its shallow hip shift stays inside the planted base; it never animates a dash or step.
	private static final Motion BACKDRAFT_MOTION = new Motion(
		new Key(v(-.12F, 1.10F, .18F), r(.045F, .08F, -.02F), r(.035F, .04F, -.015F), r(.04F, .06F, -.025F), r(-.075F, -.22F, .02F),
			arm(r(.02F, .05F, -.045F), r(-.90F, .10F, -.25F), -1.05F, r(1.68F, -.42F, -.62F)),
			arm(r(0, -.05F, .04F), r(-1.00F, -.25F, -.17F), -.98F, r(.12F, -.10F, .08F)),
			new ViewKey(v(-7.7F, -5.05F, 3.7F), arm(r(.02F, .04F, -.035F), r(-1.08F, .12F, -.24F), -.96F, r(1.43F, -.25F, -.34F)),
				v(7.1F, -6.1F, 3.5F), arm(r(0, -.035F, .03F), r(-1.04F, .08F, .25F), -.94F, r(.10F, -.10F, .08F)))),
		new Key(v(.08F, 1.08F, -.38F), r(.085F, .04F, .015F), r(.055F, .02F, .01F), r(.065F, .03F, .02F), r(-.11F, -.08F, -.015F),
			arm(r(.025F, .02F, -.035F), r(-1.24F, .05F, -.14F), -.38F, r(1.16F, .16F, .18F)),
			arm(r(0, .035F, .035F), r(-.91F, -.23F, -.20F), -.94F, r(.10F, -.10F, .10F)),
			new ViewKey(v(-7.4F, -5.4F, 2.2F), arm(r(.02F, -.03F, -.035F), r(-1.23F, -.10F, -.19F), -.40F, r(1.06F, .05F, .06F)),
				v(7.2F, -6.1F, 3.6F), arm(r(0, .03F, .03F), r(-.99F, .10F, .27F), -.92F, r(.10F, -.10F, .10F)))),
		new Key(v(.02F, 1.06F, -.06F), r(.05F, -.035F, .01F), r(.035F, -.025F, .01F), r(.035F, -.04F, .015F), r(-.07F, .08F, -.01F),
			arm(r(.02F, -.025F, -.03F), r(-.88F, -.08F, -.22F), -.94F, r(1.49F, .09F, .15F)),
			arm(r(0, .025F, .03F), r(-1.04F, -.25F, -.18F), -.96F, r(.10F, -.10F, .08F)),
			new ViewKey(v(-7.9F, -5.15F, 3.5F), arm(r(.015F, -.025F, -.03F), r(-1.06F, -.12F, -.23F), -.92F, r(1.39F, .07F, .08F)),
				v(7.1F, -6.1F, 3.5F), arm(r(0, .02F, .03F), r(-1.04F, .08F, .25F), -.94F, r(.10F, -.10F, .08F)))),
		v(-2.45F, 22, 1.30F), v(2.45F, 22, -1.45F));

	// Rooted Parry absorbs into a wide, low brace then lifts one short rising reply.
	// Independent knees and elbow open together while both ankle targets remain level.
	private static final Motion ROOTED_PARRY_MOTION = new Motion(
		new Key(v(-.08F, 1.10F, .10F), r(.075F, .10F, -.025F), r(.045F, .06F, -.015F), r(.055F, .09F, -.025F), r(-.10F, -.18F, .02F),
			arm(r(.02F, .04F, -.045F), r(-.48F, .22F, -.26F), -1.02F, r(1.03F, -.26F, -.32F)),
			arm(r(0, -.04F, .04F), r(-1.04F, -.22F, -.18F), -1.04F, r(.10F, -.10F, .10F)),
			new ViewKey(v(-7.8F, -5.45F, 3.4F), arm(r(.02F, .035F, -.035F), r(-1.00F, .12F, -.24F), -.94F, r(.98F, -.18F, -.26F)),
				v(7.1F, -6.1F, 3.5F), arm(r(0, -.025F, .03F), r(-1.04F, .08F, .24F), -.98F, r(.10F, -.10F, .08F)))),
		new Key(v(.07F, .76F, -.22F), r(.01F, -.08F, .015F), r(-.015F, -.055F, .01F), r(-.02F, -.095F, .02F), r(.025F, .17F, -.015F),
			arm(r(.025F, -.035F, -.035F), r(-1.65F, -.17F, -.29F), -.48F, r(.68F, .10F, .28F)),
			arm(r(0, .03F, .03F), r(-.87F, -.20F, -.22F), -.96F, r(.10F, -.10F, .10F)),
			new ViewKey(v(-8.1F, -4.75F, 2.7F), arm(r(.02F, -.03F, -.03F), r(-1.43F, -.17F, -.25F), -.48F, r(.58F, .06F, .27F)),
				v(7.1F, -6.1F, 3.5F), arm(r(0, .025F, .03F), r(-.98F, .10F, .27F), -.96F, r(.10F, -.10F, .10F)))),
		new Key(v(.025F, .96F, -.10F), r(.035F, -.065F, .01F), r(.015F, -.04F, .01F), r(.02F, -.07F, .015F), r(-.035F, .13F, -.01F),
			arm(r(.02F, -.025F, -.03F), r(-1.36F, -.18F, -.24F), -.83F, r(.76F, .08F, .24F)),
			arm(r(0, .02F, .03F), r(-1.00F, -.22F, -.20F), -1.00F, r(.10F, -.10F, .08F)),
			new ViewKey(v(-8.3F, -4.95F, 3.3F), arm(r(.015F, -.025F, -.03F), r(-1.24F, -.18F, -.23F), -.84F, r(.80F, .04F, .21F)),
				v(7.1F, -6.1F, 3.5F), arm(r(0, .02F, .03F), r(-1.03F, .08F, .25F), -.98F, r(.10F, -.10F, .08F)))),
		v(-2.70F, 22, 1.15F), v(2.70F, 22, -1.30F));

	// Glacier Mirror frames a compact upright receiving plane, opens a level outside reply,
	// and refolds the same guard. A quiet chest counter-turn distinguishes its square stance.
	private static final Motion GLACIER_MIRROR_MOTION = new Motion(
		new Key(v(-.10F, .78F, .08F), r(.02F, .08F, -.025F), r(.025F, .045F, -.015F), r(.02F, .12F, -.04F), r(-.04F, -.20F, .03F),
			arm(r(.02F, .04F, -.045F), r(-1.38F, .28F, -.54F), -.98F, r(.44F, -.26F, -.48F)),
			arm(r(0, -.035F, .035F), r(-1.12F, -.20F, .12F), -1.00F, r(.10F, -.10F, .08F)),
			new ViewKey(v(-8.1F, -4.95F, 3.4F), arm(r(.02F, .035F, -.035F), r(-1.23F, .16F, -.34F), -.88F, r(.46F, -.18F, -.38F)),
				v(7.3F, -6.1F, 3.5F), arm(r(0, -.025F, .03F), r(-1.06F, .08F, .24F), -.96F, r(.10F, -.10F, .08F)))),
		new Key(v(.16F, .78F, -.22F), r(.03F, -.10F, .025F), r(.03F, -.06F, .015F), r(.025F, -.15F, .04F), r(-.045F, .24F, -.03F),
			arm(r(.025F, -.04F, -.035F), r(-1.18F, -.50F, -.44F), -.56F, r(.62F, .18F, .40F)),
			arm(r(0, .025F, .03F), r(-1.05F, -.18F, .16F), -.98F, r(.10F, -.10F, .10F)),
			new ViewKey(v(-8.5F, -5.25F, 2.7F), arm(r(.02F, -.035F, -.035F), r(-1.16F, -.32F, -.32F), -.58F, r(.30F, .18F, .36F)),
				v(7.3F, -6.1F, 3.5F), arm(r(0, .025F, .03F), r(-1.04F, .10F, .25F), -.94F, r(.10F, -.10F, .10F)))),
		new Key(v(.025F, .76F, -.04F), r(.02F, -.035F, .01F), r(.02F, -.02F, .01F), r(.02F, -.055F, .015F), r(-.04F, .085F, -.01F),
			arm(r(.02F, -.02F, -.03F), r(-1.34F, -.12F, -.48F), -.90F, r(.44F, .05F, .12F)),
			arm(r(0, .02F, .03F), r(-1.10F, -.20F, .12F), -.98F, r(.10F, -.10F, .08F)),
			new ViewKey(v(-8.3F, -5.0F, 3.3F), arm(r(.015F, -.02F, -.03F), r(-1.22F, -.10F, -.32F), -.86F, r(.48F, .04F, .12F)),
				v(7.3F, -6.1F, 3.5F), arm(r(0, .02F, .03F), r(-1.06F, .08F, .24F), -.96F, r(.10F, -.10F, .08F)))),
		v(-2.35F, 22, 1.10F), v(2.35F, 22, -1.20F));

	// Static Riposte compresses beside the outside shoulder and snaps a descending diagonal.
	// The free elbow opens in counterbalance before the blade recoils high; both feet stay set.
	private static final Motion STATIC_RIPOSTE_MOTION = new Motion(
		new Key(v(-.18F, 1.00F, .14F), r(.06F, .16F, -.035F), r(.045F, .08F, -.025F), r(.06F, .14F, -.045F), r(-.10F, -.30F, .035F),
			arm(r(.025F, .065F, -.045F), r(-1.78F, .38F, -.32F), -.96F, r(.28F, -.22F, -.34F)),
			arm(r(0, -.045F, .04F), r(-.82F, -.26F, -.38F), -.94F, r(.10F, -.10F, .08F)),
			new ViewKey(v(-7.8F, -4.65F, 3.3F), arm(r(.02F, .04F, -.035F), r(-1.35F, .18F, -.25F), -.84F, r(-.05F, -.18F, -.65F)),
				v(7.4F, -6.1F, 3.6F), arm(r(0, -.03F, .03F), r(-1.00F, .10F, .28F), -.94F, r(.10F, -.10F, .08F)))),
		new Key(v(.18F, 1.00F, -.34F), r(.09F, -.14F, .03F), r(.06F, -.08F, .02F), r(.08F, -.16F, .045F), r(-.12F, .28F, -.035F),
			arm(r(.025F, -.045F, -.035F), r(-.94F, -.38F, -.28F), -.50F, r(1.06F, .14F, .38F)),
			arm(r(0, .03F, .035F), r(-.64F, -.18F, -.50F), -.90F, r(.10F, -.10F, .10F)),
			new ViewKey(v(-8.0F, -5.55F, 2.4F), arm(r(.02F, -.035F, -.035F), r(-1.03F, -.24F, -.24F), -.54F, r(-.40F, .40F, -.40F)),
				v(7.5F, -6.1F, 3.6F), arm(r(0, .03F, .03F), r(-.98F, .12F, .30F), -.92F, r(.10F, -.10F, .10F)))),
		new Key(v(.04F, .92F, -.08F), r(.045F, -.07F, .015F), r(.03F, -.04F, .01F), r(.04F, -.09F, .02F), r(-.06F, .15F, -.015F),
			arm(r(.02F, -.025F, -.03F), r(-1.58F, -.18F, -.30F), -.86F, r(.30F, .07F, .18F)),
			arm(r(0, .025F, .03F), r(-.84F, -.24F, -.34F), -.94F, r(.10F, -.10F, .08F)),
			new ViewKey(v(-8.1F, -4.85F, 3.2F), arm(r(.015F, -.025F, -.03F), r(-1.30F, -.12F, -.24F), -.80F, r(.36F, .06F, .15F)),
				v(7.4F, -6.1F, 3.6F), arm(r(0, .02F, .03F), r(-1.00F, .10F, .28F), -.94F, r(.10F, -.10F, .08F)))),
		v(-2.50F, 22, 1.35F), v(2.50F, 22, -1.50F));


	// Unmoved settles behind a low, wide brace and drives the point from its ribs.
	// The hips stay loaded, the chest opens only slightly, and the elbow supplies the thrust.
	// Camera-space arms retain a visible side-on blade face and a separate supporting hand.
	private static final Motion UNMOVED_MOTION = new Motion(
		new Key(v(-.055F, 1.22F, .055F), r(.09F, .055F, -.018F), r(.045F, -.02F, -.012F), r(.065F, .08F, -.026F), r(-.13F, -.12F, .025F),
			arm(r(.035F, .045F, -.065F), r(-.91F, .16F, -.33F), -1.08F, r(1.56F, -.18F, -.24F)),
			arm(r(.025F, -.065F, .055F), r(-1.17F, -.38F, .27F), -1.12F, r(.13F, -.12F, .14F)),
			new ViewKey(v(-8.05F, -5.15F, 3.55F), arm(r(.03F, .025F, -.045F), r(-1.14F, .09F, -.28F), -1.07F, r(1.34F, -.12F, -.25F)),
				v(7.55F, -6.25F, 3.45F), arm(r(.015F, -.035F, .04F), r(-1.10F, .13F, .31F), -1.01F, r(.13F, -.12F, .11F)))),
		new Key(v(.075F, 1.22F, -.30F), r(.105F, -.025F, .014F), r(.06F, .015F, .009F), r(.095F, -.025F, .025F), r(-.16F, .035F, -.022F),
			arm(r(.045F, -.025F, -.045F), r(-1.45F, -.025F, -.19F), -.24F, r(1.18F, .075F, .16F)),
			arm(r(.025F, .035F, .045F), r(-1.22F, -.31F, .25F), -1.04F, r(.13F, -.10F, .12F)),
			new ViewKey(v(-8.25F, -5.55F, 2.0F), arm(r(.035F, -.025F, -.045F), r(-1.31F, -.055F, -.26F), -.31F, r(.73F, .09F, .13F)),
				v(7.45F, -6.25F, 3.4F), arm(r(.015F, .025F, .035F), r(-1.12F, .15F, .30F), -.97F, r(.12F, -.10F, .12F)))),
		new Key(v(.025F, 1.18F, -.065F), r(.075F, -.015F, .008F), r(.04F, .01F, .006F), r(.06F, -.035F, .014F), r(-.105F, .05F, -.012F),
			arm(r(.03F, -.015F, -.045F), r(-1.02F, -.075F, -.29F), -1.02F, r(1.48F, .045F, .07F)),
			arm(r(.02F, .02F, .04F), r(-1.19F, -.36F, .26F), -1.10F, r(.12F, -.11F, .12F)),
			new ViewKey(v(-8.15F, -5.2F, 3.35F), arm(r(.025F, -.015F, -.04F), r(-1.18F, -.08F, -.27F), -.98F, r(1.29F, .04F, .09F)),
				v(7.5F, -6.25F, 3.45F), arm(r(.015F, .015F, .035F), r(-1.11F, .13F, .31F), -1.00F, r(.12F, -.11F, .11F)))),
		v(-3.05F, 22, .85F), v(3.05F, 22, -.95F));

	// Null Parry receives wide of the blade shoulder, sweeps inward across a small pocket,
	// then folds the elbow to close. The opposite hand tracks beneath its own shoulder.
	// This is one release: the pulse-before-primary ordering belongs to the server performer.
	private static final Motion NULL_PARRY_MOTION = new Motion(
		new Key(v(-.16F, .94F, .095F), r(.04F, .14F, .035F), r(.035F, .06F, .02F), r(.025F, .17F, .035F), r(-.055F, -.30F, -.025F),
			arm(r(.03F, .075F, -.055F), r(-1.13F, .57F, -.39F), -.82F, r(.81F, -.34F, -.53F)),
			arm(r(.01F, -.06F, .04F), r(-.96F, -.32F, .28F), -1.08F, r(.12F, -.14F, .13F)),
			new ViewKey(v(-8.8F, -5.0F, 3.25F), arm(r(.025F, .06F, -.04F), r(-1.17F, .35F, -.31F), -.79F, r(.76F, -.26F, -.47F)),
				v(7.6F, -6.15F, 3.55F), arm(r(.01F, -.04F, .035F), r(-1.07F, .09F, .28F), -1.04F, r(.11F, -.12F, .10F)))),
		new Key(v(.11F, .96F, -.15F), r(.055F, -.095F, -.018F), r(.04F, -.045F, -.012F), r(.055F, -.14F, -.02F), r(-.085F, .23F, .015F),
			arm(r(.035F, -.045F, -.04F), r(-1.27F, -.36F, -.31F), -.66F, r(.99F, .21F, .36F)),
			arm(r(.015F, .025F, .045F), r(-1.14F, -.21F, .29F), -1.15F, r(.12F, -.10F, .12F)),
			new ViewKey(v(-8.65F, -5.3F, 2.75F), arm(r(.03F, -.035F, -.04F), r(-1.22F, -.24F, -.29F), -.65F, r(.58F, .20F, .32F)),
				v(7.55F, -6.2F, 3.5F), arm(r(.015F, .025F, .035F), r(-1.12F, .12F, .29F), -1.08F, r(.12F, -.10F, .11F)))),
		new Key(v(.035F, 1.02F, .015F), r(.055F, -.025F, .012F), r(.04F, -.015F, .01F), r(.045F, -.035F, .018F), r(-.075F, .055F, -.01F),
			arm(r(.025F, -.02F, -.045F), r(-1.07F, -.12F, -.35F), -1.10F, r(.93F, .06F, .12F)),
			arm(r(.015F, .015F, .04F), r(-1.20F, -.27F, .30F), -1.13F, r(.12F, -.11F, .12F)),
			new ViewKey(v(-8.45F, -5.15F, 3.4F), arm(r(.02F, -.015F, -.035F), r(-1.14F, -.075F, -.30F), -1.06F, r(.84F, .055F, .11F)),
				v(7.6F, -6.2F, 3.5F), arm(r(.015F, .015F, .035F), r(-1.11F, .10F, .28F), -1.05F, r(.11F, -.11F, .10F)))),
		v(-2.6F, 22, 1.0F), v(2.6F, 22, -1.15F));

	// Eye of the Storm winds through a deep rotational coil, sweeps a level 360-degree perimeter slice, then settles in a wide stance.
	private static final Motion EYE_OF_THE_STORM_MOTION = new Motion(
		new Key(v(-.14F, .96F, .12F), r(.03F, .28F, -.04F), r(.02F, .16F, -.02F), r(.025F, .24F, -.05F), r(-.05F, -.38F, .04F),
			arm(r(.025F, .08F, -.05F), r(-1.42F, .52F, -.32F), -.95F, r(.42F, -.30F, -.38F)),
			arm(r(0, -.05F, .04F), r(-.92F, -.24F, -.20F), -.98F, r(.10F, -.10F, .08F)),
			new ViewKey(v(-8.2F, -4.8F, 3.2F), arm(r(.02F, .05F, -.035F), r(-1.25F, .22F, -.26F), -.88F, r(.35F, -.20F, -.30F)),
				v(7.3F, -6.1F, 3.5F), arm(r(0, -.03F, .03F), r(-1.02F, .08F, .24F), -.95F, r(.10F, -.10F, .08F)))),
		new Key(v(.14F, .96F, -.26F), r(.05F, -.24F, .035F), r(.035F, -.16F, .025F), r(.04F, -.26F, .045F), r(-.08F, .42F, -.035F),
			arm(r(.035F, -.07F, -.04F), r(-1.22F, -.68F, -.22F), -.45F, r(.85F, .18F, .44F)),
			arm(r(0, .055F, .035F), r(-.82F, -.18F, -.35F), -.88F, r(.10F, -.10F, .10F)),
			new ViewKey(v(-8.6F, -5.3F, 2.6F), arm(r(.025F, -.05F, -.035F), r(-1.18F, -.42F, -.28F), -.48F, r(.22F, .48F, .42F)),
				v(7.5F, -6.1F, 3.6F), arm(r(0, .04F, .03F), r(-.95F, .12F, .28F), -.90F, r(.10F, -.10F, .10F)))),
		new Key(v(.04F, .90F, -.08F), r(.03F, -.32F, .02F), r(.02F, -.20F, .015F), r(.025F, -.30F, .03F), r(-.04F, .48F, -.02F),
			arm(r(.02F, -.08F, -.03F), r(-1.14F, -.82F, -.14F), -.78F, r(.92F, .22F, .48F)),
			arm(r(0, .04F, .03F), r(-.88F, -.20F, -.25F), -.94F, r(.08F, -.10F, .08F)),
			new ViewKey(v(-8.9F, -5.1F, 3.0F), arm(r(.015F, -.06F, -.03F), r(-1.12F, -.55F, -.22F), -.78F, r(.28F, .44F, .52F)),
				v(7.5F, -6.1F, 3.6F), arm(r(0, .03F, .03F), r(-.98F, .08F, .26F), -.92F, r(.10F, -.10F, .08F)))),
		v(-2.60F, 22, 1.35F), v(2.60F, 22, -1.45F));

	// Sanguine Parry braces across the chest in a deep crouch, snaps an aggressive rising cross-slash, and recoils high.
	private static final Motion SANGUINE_PARRY_MOTION = new Motion(
		new Key(v(-.16F, 1.15F, .16F), r(.08F, .15F, -.03F), r(.05F, .09F, -.02F), r(.06F, .16F, -.035F), r(-.11F, -.25F, .025F),
			arm(r(.03F, .06F, -.05F), r(-1.35F, .36F, -.42F), -.98F, r(.65F, -.24F, -.40F)),
			arm(r(0, -.05F, .04F), r(-.95F, -.28F, -.18F), -.98F, r(.12F, -.10F, .08F)),
			new ViewKey(v(-7.9F, -4.85F, 3.3F), arm(r(.02F, .04F, -.035F), r(-1.28F, .18F, -.25F), -.88F, r(.55F, -.18F, -.32F)),
				v(7.2F, -6.1F, 3.5F), arm(r(0, -.03F, .03F), r(-1.02F, .08F, .25F), -.94F, r(.10F, -.10F, .08F)))),
		new Key(v(.16F, 1.12F, -.34F), r(.11F, -.18F, .03F), r(.07F, -.11F, .02F), r(.08F, -.20F, .035F), r(-.14F, .32F, -.025F),
			arm(r(.035F, -.06F, -.035F), r(-.82F, -.52F, -.26F), -.44F, r(1.18F, .18F, .38F)),
			arm(r(0, .045F, .035F), r(-.52F, -.22F, -.36F), -.84F, r(.10F, -.10F, .10F)),
			new ViewKey(v(-8.1F, -5.35F, 2.5F), arm(r(.02F, -.045F, -.035F), r(-1.06F, -.32F, -.26F), -.46F, r(.05F, .52F, .38F)),
				v(7.4F, -6.1F, 3.6F), arm(r(0, .035F, .03F), r(-.94F, .10F, .28F), -.90F, r(.10F, -.10F, .10F)))),
		new Key(v(.08F, 1.05F, -.14F), r(.06F, -.22F, .02F), r(.04F, -.14F, .015F), r(.045F, -.24F, .025F), r(-.08F, .38F, -.02F),
			arm(r(.025F, -.08F, -.03F), r(-.62F, -.72F, -.14F), -.74F, r(1.22F, .18F, .48F)),
			arm(r(0, .035F, .03F), r(-.62F, -.22F, -.30F), -.90F, r(.08F, -.10F, .08F)),
			new ViewKey(v(-8.6F, -5.15F, 3.0F), arm(r(.015F, -.06F, -.03F), r(-1.08F, -.46F, -.20F), -.78F, r(.18F, .46F, .48F)),
				v(7.4F, -6.1F, 3.6F), arm(r(0, .025F, .03F), r(-.98F, .08F, .27F), -.92F, r(.10F, -.10F, .08F)))),
		v(-2.55F, 22, 1.30F), v(2.55F, 22, -1.45F));

	// Constellation Guard lifts high beside the temple, snaps a crisp angular forward intercept, then withdraws with crystalline poise.
	private static final Motion CONSTELLATION_GUARD_MOTION = new Motion(
		new Key(v(-.08F, 1.02F, .08F), r(.02F, .10F, -.015F), r(.025F, .06F, -.01F), r(.03F, .12F, -.02F), r(-.05F, -.18F, .015F),
			arm(r(.03F, .05F, -.04F), r(-1.82F, .28F, -.22F), -.88F, r(.35F, -.14F, -.24F)),
			arm(r(0, -.04F, .035F), r(-1.05F, -.22F, -.15F), -.98F, r(.10F, -.10F, .08F)),
			new ViewKey(v(-8.0F, -4.55F, 3.2F), arm(r(.02F, .035F, -.03F), r(-1.42F, .14F, -.22F), -.82F, r(.28F, -.10F, -.18F)),
				v(7.3F, -6.1F, 3.5F), arm(r(0, -.03F, .03F), r(-1.02F, .08F, .24F), -.94F, r(.10F, -.10F, .08F)))),
		new Key(v(.10F, 1.04F, -.32F), r(.06F, -.05F, .01F), r(.04F, -.03F, .01F), r(.045F, -.06F, .015F), r(-.08F, .12F, -.01F),
			arm(r(.03F, -.03F, -.035F), r(-1.52F, -.08F, -.12F), -.36F, r(.92F, .08F, .22F)),
			arm(r(0, .03F, .035F), r(-.72F, -.18F, -.28F), -.88F, r(.10F, -.10F, .10F)),
			new ViewKey(v(-7.9F, -5.25F, 2.3F), arm(r(.02F, -.025F, -.03F), r(-1.26F, -.08F, -.18F), -.38F, r(.75F, .04F, .16F)),
				v(7.4F, -6.1F, 3.6F), arm(r(0, .03F, .03F), r(-.96F, .10F, .26F), -.92F, r(.10F, -.10F, .10F)))),
		new Key(v(.03F, 1.00F, -.08F), r(.035F, -.03F, .01F), r(.025F, -.02F, .005F), r(.03F, -.04F, .01F), r(-.05F, .08F, -.01F),
			arm(r(.02F, -.02F, -.03F), r(-1.38F, -.06F, -.15F), -.86F, r(.85F, .04F, .14F)),
			arm(r(0, .02F, .03F), r(-.88F, -.20F, -.22F), -.94F, r(.10F, -.10F, .08F)),
			new ViewKey(v(-8.1F, -4.95F, 3.2F), arm(r(.015F, -.02F, -.03F), r(-1.24F, -.06F, -.18F), -.80F, r(.78F, .02F, .10F)),
				v(7.3F, -6.1F, 3.5F), arm(r(0, .02F, .03F), r(-1.00F, .08F, .25F), -.94F, r(.10F, -.10F, .08F)))),
		v(-2.40F, 22, 1.25F), v(2.40F, 22, -1.35F));

	// Stopped Moment draws to the floating ribs, drives a level suspended point through temporal stillness, and recoils along the thrust line.
	private static final Motion STOPPED_MOMENT_MOTION = new Motion(
		new Key(v(-.10F, 1.14F, .14F), r(.04F, .12F, -.015F), r(.03F, .06F, -.01F), r(.035F, .14F, -.025F), r(-.06F, -.22F, .02F),
			arm(r(.025F, .05F, -.045F), r(-1.02F, .20F, -.26F), -1.04F, r(1.42F, -.28F, -.44F)),
			arm(r(0, -.04F, .04F), r(-1.02F, -.24F, -.18F), -.98F, r(.12F, -.10F, .08F)),
			new ViewKey(v(-7.7F, -5.00F, 3.6F), arm(r(.02F, .035F, -.035F), r(-1.10F, .14F, -.22F), -.98F, r(1.28F, -.20F, -.26F)),
				v(7.2F, -6.1F, 3.5F), arm(r(0, -.03F, .03F), r(-1.02F, .08F, .25F), -.94F, r(.10F, -.10F, .08F)))),
		new Key(v(.12F, 1.12F, -.36F), r(.075F, -.06F, .015F), r(.045F, -.03F, .01F), r(.055F, -.07F, .02F), r(-.10F, .14F, -.015F),
			arm(r(.03F, -.03F, -.035F), r(-1.42F, -.04F, -.12F), -.30F, r(1.12F, .12F, .18F)),
			arm(r(0, .035F, .035F), r(-.82F, -.20F, -.25F), -.92F, r(.10F, -.10F, .10F)),
			new ViewKey(v(-7.6F, -5.35F, 2.1F), arm(r(.02F, -.025F, -.035F), r(-1.26F, -.08F, -.18F), -.32F, r(.96F, .06F, .08F)),
				v(7.3F, -6.1F, 3.6F), arm(r(0, .03F, .03F), r(-.98F, .10F, .27F), -.92F, r(.10F, -.10F, .10F)))),
		new Key(v(.04F, 1.08F, -.08F), r(.045F, -.04F, .01F), r(.03F, -.025F, .01F), r(.035F, -.05F, .015F), r(-.065F, .10F, -.01F),
			arm(r(.02F, -.025F, -.03F), r(-1.18F, -.08F, -.18F), -.92F, r(1.35F, .06F, .12F)),
			arm(r(0, .025F, .03F), r(-.96F, -.22F, -.20F), -.96F, r(.10F, -.10F, .08F)),
			new ViewKey(v(-7.9F, -5.10F, 3.4F), arm(r(.015F, -.02F, -.03F), r(-1.14F, -.08F, -.20F), -.88F, r(1.20F, .04F, .08F)),
				v(7.2F, -6.1F, 3.5F), arm(r(0, -.02F, .03F), r(-1.02F, .08F, .25F), -.94F, r(.10F, -.10F, .08F)))),
		v(-2.45F, 22, 1.25F), v(2.45F, 22, -1.35F));

	public static final Pose NONE = new Pose(0, Phase.NONE, bind(), VIEW_BIND);

	/** Impact is exactly the accepted windup tick; unsupported arts deliberately retain their existing renderer. */
	public static Pose sampleSpellcut(int move, float age, int windup, int recovery, boolean leftHanded) {
		if (move != SPELLCUT || !valid(age, windup, 60, recovery) || age >= windup + recovery) return NONE;
		return sample(SPELLCUT_MOTION, age, windup, 1, windup + Math.min(4, recovery * .25F), windup + recovery, leftHanded);
	}

	/** Only authored player presentation IDs are admitted here; NPC IDs use supportsMaster. */
	public static boolean supportsPlayer(int move) {
		return move == SPELLCUT || move == RISING_BREAK || move == DRIVING_CUT || move == KINDLING_DRAW || move == FROSTBITE
			|| move == HAILFALL || move == SKYFALL || move == COLLAPSE || move == RED_RAIN || move == CRIMSON_MOON || move == BACKDRAFT || move == ROOTED_PARRY || move == GLACIER_MIRROR || move == STATIC_RIPOSTE || move == UNMOVED || move == NULL_PARRY
			|| move == EYE_OF_THE_STORM || move == SANGUINE_PARRY || move == CONSTELLATION_GUARD || move == STOPPED_MOMENT;
	}

	/** Uses the existing accepted player window, whose recovery includes the one release tick. */
	public static Pose samplePlayer(int move, float age, int windup, int recovery, boolean leftHanded) {
		if (move == SPELLCUT) return sampleSpellcut(move, age, windup, recovery, leftHanded);
		if (!supportsPlayer(move) || !valid(age, windup, 60, recovery) || age >= windup + recovery) return NONE;
		if (move == SKYFALL) return skyfall(age, windup, recovery, leftHanded);
		Motion motion = switch (move) {
			case RISING_BREAK -> RISING_MOTION;
			case DRIVING_CUT -> DRIVING_MOTION;
			case KINDLING_DRAW -> KINDLING_MOTION;
			case FROSTBITE -> FROSTBITE_MOTION;
			case HAILFALL -> HAILFALL_MOTION;
			case COLLAPSE -> COLLAPSE_MOTION;
			case RED_RAIN -> RED_RAIN_MOTION;
			case CRIMSON_MOON -> CRIMSON_MOON_MOTION;
			case BACKDRAFT -> BACKDRAFT_MOTION;
			case ROOTED_PARRY -> ROOTED_PARRY_MOTION;
			case GLACIER_MIRROR -> GLACIER_MIRROR_MOTION;
			case STATIC_RIPOSTE -> STATIC_RIPOSTE_MOTION;
			case UNMOVED -> UNMOVED_MOTION;
			case NULL_PARRY -> NULL_PARRY_MOTION;
			case EYE_OF_THE_STORM -> EYE_OF_THE_STORM_MOTION;
			case SANGUINE_PARRY -> SANGUINE_PARRY_MOTION;
			case CONSTELLATION_GUARD -> CONSTELLATION_GUARD_MOTION;
			case STOPPED_MOMENT -> STOPPED_MOMENT_MOTION;
			default -> throw new AssertionError("Unsupported player motion passed admission");
		};
		return sample(motion, age, windup, 1, windup + Math.min(4, recovery * .25F), windup + recovery, leftHanded);
	}

	/** Only these original NPC clips own the segmented backend; every other ID keeps its fallback. */
	public static boolean supportsMaster(int attack) {
		return attack == MASTER_SWEEP || attack == MASTER_CROSSWIND_REPRISE || attack == MASTER_STONE_FRACTURE || attack == MASTER_EMBER_KILN_RING || attack == MASTER_STONE_FAULT_MARCH;
	}

	/** The sole locomotion exception: Gale's four accepted step ticks, never ordinary walking. */
	public static boolean masterFootwork(int attack, float age, int tell) {
		return attack == MASTER_CROSSWIND_REPRISE && Float.isFinite(age) && tell > 0 && tell <= 80
			&& age >= tell * GaleRepriseRules.GATHER / (float) GaleRepriseRules.TELL
			&& age < tell * (GaleRepriseRules.GATHER + GaleRepriseRules.STEP_TICKS) / (float) GaleRepriseRules.TELL;
	}

	/** Accepted tell/active/recovery windows are shared with the server, including school-form beats. */
	public static Pose sampleMaster(int attack, float age, int tell, int active, int recovery, boolean leftHanded) {
		if (!supportsMaster(attack) || !valid(age, tell, 80, recovery) || active < 1 || active > 10) return NONE;
		if (attack == MASTER_STONE_FAULT_MARCH) return age < StoneMarchRules.END ? march(age, leftHanded) : NONE;
		if (age >= tell + active + recovery) return NONE;
		if (attack == MASTER_EMBER_KILN_RING) return kiln(age, tell, active, recovery, leftHanded);
		if (attack == MASTER_CROSSWIND_REPRISE) return reprise(age, tell, active, recovery, leftHanded);
		if (attack == MASTER_STONE_FRACTURE) return fracture(age, tell, active, recovery, leftHanded);
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
		return assemble(key, weight, age, impact, active, motion.rightPlant, motion.leftPlant, leftHanded);
	}

	private static Pose skyfall(float age, int windup, int recovery, boolean leftHanded) {
		if (age < windup) return sample(SKYFALL_MOTION, age, windup, 1, windup + Math.min(4, recovery * .25F), windup + recovery, leftHanded);
		float t = age - windup, answerAt = Math.min(ArtRules.SKYFALL_DELAY, recovery * .65F);
		float holdUntil = answerAt * .5F, followAt = Math.min(recovery * .8F, answerAt + 2);
		Key key = t < holdUntil ? SKYFALL_MOTION.impact
			: t < answerAt ? SKYFALL_MOTION.impact.toward(SKYFALL_ANSWER, smooth((t - holdUntil) / (answerAt - holdUntil)))
			: t < followAt ? SKYFALL_ANSWER.toward(SKYFALL_MOTION.follow, smooth((t - answerAt) / (followAt - answerAt))) : SKYFALL_MOTION.follow;
		float weight = 1 - smooth((t - followAt) / (recovery - followAt));
		return assemble(key, weight, age, windup, 1, SKYFALL_MOTION.rightPlant, SKYFALL_MOTION.leftPlant, leftHanded);
	}

	private static Pose reprise(float age, int tell, int active, int recovery, boolean leftHanded) {
		float step = tell * GaleRepriseRules.GATHER / (float) GaleRepriseRules.TELL;
		float planted = tell * (GaleRepriseRules.GATHER + GaleRepriseRules.STEP_TICKS) / (float) GaleRepriseRules.TELL;
		float chamber = planted + (tell - planted) * .25F;
		float follow = tell + active + Math.min(3, recovery * .20F), end = tell + active + recovery;
		Key key = age < step ? REPRISE_GATHER.toward(REPRISE_STEP, smooth(age / step))
			: age < planted ? REPRISE_STEP.toward(REPRISE_MOTION.chamber, smooth((age - step) / (planted - step)))
			: age < chamber ? REPRISE_MOTION.chamber
			: age < tell ? REPRISE_MOTION.chamber.toward(REPRISE_MOTION.impact, smooth((age - chamber) / (tell - chamber)))
			: age < follow ? REPRISE_MOTION.impact.toward(REPRISE_MOTION.follow, smooth((age - tell) / (follow - tell))) : REPRISE_MOTION.follow;
		float weight = smooth(age / Math.max(1, step * .5F)) * (1 - smooth((age - follow) / (end - follow)));
		Vec3 right = REPRISE_MOTION.rightPlant, left = REPRISE_MOTION.leftPlant;
		if (age >= step && age < planted) {
			float t = (age - step) / (planted - step);
			// The leading foot opens, then the trailing foot clears. Flat soles settle exactly at
			// the reply warning. Ankles are body-local; accepted entity travel remains untouched.
			float lead = (float) Math.pow(Math.sin(Math.PI * clamp(t * 2)), 2);
			float trail = (float) Math.pow(Math.sin(Math.PI * clamp((t - .5F) * 2)), 2);
			right = right.plus(v(-1.0F * lead, -1.25F * lead, .20F * lead));
			left = left.plus(v(-.90F * trail, -1.30F * trail, .20F * trail));
		}
		return assemble(key, weight, age, tell, active, right, left, leftHanded);
	}

	private static Pose kiln(float age, int tell, int active, int recovery, boolean leftHanded) {
		float gather = tell * .20F, release = tell * .80F;
		float follow = tell + active + Math.min(3, recovery * .20F), end = tell + active + recovery;
		Key key = age < release ? KILN_MOTION.chamber
			: age < tell ? KILN_MOTION.chamber.toward(KILN_MOTION.impact, smooth((age - release) / (tell - release)))
			: age < follow ? KILN_MOTION.impact.toward(KILN_MOTION.follow, smooth((age - tell) / (follow - tell))) : KILN_MOTION.follow;
		float weight = smooth(age / Math.max(1, gather)) * (1 - smooth((age - follow) / (end - follow)));
		Pose posed = assemble(key, weight, age, tell, active, KILN_MOTION.rightPlant, KILN_MOTION.leftPlant, false);
		Transform[] local = posed.local.clone();
		float turn = MasterAnimationRules.kilnTurn(age, tell);
		Matrix root = new Transform(0, 0, 0, r(0, turn, 0)).matrix();
		// Compose AFTER the ordinary key blend: shortest-arc interpolation cannot represent a
		// complete revolution. Keep the completed turn through recovery, without unwinding it.
		rotate(local, Joint.PELVIS, root.multiply(local[Joint.PELVIS.ordinal()].matrix()).rotation());
		Vec3 pole = root.direction(v(0, 0, -1));
		for (boolean left : new boolean[] {false, true}) {
			Vec3 ankle = left ? KILN_MOTION.leftPlant : KILN_MOTION.rightPlant;
			float footTurn = turn, lift = 0;
			if (age >= gather && age < release) {
				// Four compact quarter-turn pivots. One foot clears while the other supports the
				// body; the second catches up before the next pivot. Both plant before ignition.
				float quarters = (age - gather) / (release - gather) * 4;
				int quarter = Math.min(3, (int) quarters);
				float part = quarters - quarter, step = clamp(part * 2 - (left ? 1 : 0));
				float from = MasterAnimationRules.kilnTurn(gather + (release - gather) * quarter / 4, tell);
				float to = MasterAnimationRules.kilnTurn(gather + (release - gather) * (quarter + 1) / 4, tell);
				footTurn = lerp(from, to, smooth(step));
				lift = .85F * (float) Math.pow(Math.sin(Math.PI * step), 2);
			}
			Matrix pivot = new Transform(0, 0, 0, r(0, footTurn, 0)).matrix();
			Vec3 target = pivot.transform(ankle).plus(v(0, -lift, 0));
			plant(local, left, v(left ? 1.9F : -1.9F, 22, 0).toward(target, weight), pole, footTurn);
		}
		return new Pose(weight, posed.phase, leftHanded ? reflect(local) : local, VIEW_BIND);
	}

	private static Pose march(float age, boolean leftHanded) {
		float gather = StoneMarchRules.TELL * .375F, overhead = StoneMarchRules.TELL * .75F;
		float firstBrace = (StoneMarchRules.TELL + StoneMarchRules.SECOND) * .5F;
		float secondBrace = (StoneMarchRules.SECOND + StoneMarchRules.THIRD) * .5F;
		float settle = StoneMarchRules.THIRD + (StoneMarchRules.THIRD - StoneMarchRules.SECOND);
		float extract = StoneMarchRules.THIRD + StoneMarchRules.RECOVERY * .5F;
		Key key = age < gather ? MARCH_GATHER
			: age < overhead ? MARCH_GATHER.toward(MARCH_OVERHEAD, smooth((age - gather) / (overhead - gather)))
			: age < StoneMarchRules.TELL ? MARCH_OVERHEAD.toward(MARCH_STRIKE, smooth((age - overhead) / (StoneMarchRules.TELL - overhead)))
			: age < firstBrace ? MARCH_STRIKE.toward(MARCH_BRACE, smooth((age - StoneMarchRules.TELL) / (firstBrace - StoneMarchRules.TELL)))
			: age < StoneMarchRules.SECOND ? MARCH_BRACE.toward(MARCH_RECOIL, smooth((age - firstBrace) / (StoneMarchRules.SECOND - firstBrace)))
			: age < secondBrace ? MARCH_RECOIL.toward(MARCH_BRACE, smooth((age - StoneMarchRules.SECOND) / (secondBrace - StoneMarchRules.SECOND)))
			: age < StoneMarchRules.THIRD ? MARCH_BRACE.toward(MARCH_RECOIL, smooth((age - secondBrace) / (StoneMarchRules.THIRD - secondBrace)))
			: age < settle ? MARCH_RECOIL.toward(MARCH_BRACE, smooth((age - StoneMarchRules.THIRD) / (settle - StoneMarchRules.THIRD)))
			: age < extract ? MARCH_BRACE.toward(MARCH_EXTRACT, smooth((age - settle) / (extract - settle))) : MARCH_EXTRACT;
		float weight = smooth(age / gather) * (1 - smooth((age - extract) / (StoneMarchRules.END - extract)));
		Pose posed = assemble(key, weight, age, StoneMarchRules.TELL, 1, MARCH_RIGHT_PLANT, MARCH_LEFT_PLANT, leftHanded);
		boolean pulse = age >= StoneMarchRules.TELL && age < StoneMarchRules.TELL + 1
			|| age >= StoneMarchRules.SECOND && age < StoneMarchRules.SECOND + 1
			|| age >= StoneMarchRules.THIRD && age < StoneMarchRules.THIRD + 1;
		return new Pose(weight, pulse ? Phase.ACTIVE : posed.phase, posed.local, VIEW_BIND);
	}

	private static Pose fracture(float age, int tell, int active, int recovery, boolean leftHanded) {
		float plant = tell * StoneFractureRules.PLANT / (float) StoneFractureRules.TELL;
		float warning = tell * (StoneFractureRules.PLANT + StoneFractureRules.BRACE) / (float) StoneFractureRules.TELL;
		float chamber = warning + (tell - warning) / 3;
		float follow = tell + active + Math.min(3, recovery * .20F), end = tell + active + recovery;
		Key key = age < plant ? FRACTURE_PLANT.toward(FRACTURE_BRACE, smooth(age / plant))
			: age < warning ? FRACTURE_BRACE
			: age < chamber ? FRACTURE_BRACE.toward(FRACTURE_MOTION.chamber, smooth((age - warning) / (chamber - warning)))
			: age < tell ? FRACTURE_MOTION.chamber.toward(FRACTURE_MOTION.impact, smooth((age - chamber) / (tell - chamber)))
			: age < follow ? FRACTURE_MOTION.impact.toward(FRACTURE_MOTION.follow, smooth((age - tell) / (follow - tell))) : FRACTURE_MOTION.follow;
		float weight = smooth(age / Math.max(1, plant)) * (1 - smooth((age - follow) / (end - follow)));
		return assemble(key, weight, age, tell, active, FRACTURE_MOTION.rightPlant, FRACTURE_MOTION.leftPlant, leftHanded);
	}

	private static Pose assemble(Key key, float weight, float age, int impact, int active, Vec3 rightPlant, Vec3 leftPlant, boolean leftHanded) {
		Transform[] local = bind(), target = bind();
		set(target, Joint.PELVIS, v(key.shift.x, 12 + key.shift.y, key.shift.z), key.pelvis);
		rotate(target, Joint.SPINE, key.spine); rotate(target, Joint.CHEST, key.chest); rotate(target, Joint.HEAD, key.head);
		putArm(target, false, key.sword); putArm(target, true, key.guard);
		for (Joint joint : Joint.values()) local[joint.ordinal()] = local[joint.ordinal()].toward(target[joint.ordinal()], weight);
		if (weight > 0) {
			plant(local, false, v(-1.9F, 22, 0).toward(rightPlant, weight));
			plant(local, true, v(1.9F, 22, 0).toward(leftPlant, weight));
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
		plant(local, left, ankle, v(0, 0, -1), 0);
	}

	private static void plant(Transform[] local, boolean left, Vec3 ankle, Vec3 kneePole, float footYaw) {
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
		Vec3 pole = inverse.direction(kneePole);
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
		Matrix level = lower.inverseRigid();
		if (footYaw != 0) level = level.multiply(new Transform(0, 0, 0, r(0, footYaw, 0)).matrix());
		rotate(local, foot, level.rotation());
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
		Rotation wrist = r(Math.max(-.18F, Math.min(.18F, authoredGrip.x * .25F)),
			Math.max(-.15F, Math.min(.15F, authoredGrip.y * .25F)), Math.max(-.18F, Math.min(.18F, authoredGrip.z * .25F)));
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
