package dev.wildercord.client.monster;

import dev.wildercord.monster.TimbermaulRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The Timbermaul Bear: a long heavy body with a hump of muscle over its shoulders, a mat of moss on its back and a ridge
 * of hackles down its spine; a broad head with a long muzzle, a jaw that drops and small round ears; thick legs and a
 * stub of a tail. Drawn at an ordinary bear's size; its renderer makes it half again as big.
 *
 * <p>Everything but its hind legs hangs from its hips, so it can stand up on them. It ambles with its head low and
 * swinging. Each of its moves has a pose of its own, worked out from how far into the move it is and eased in and out
 * as one move gives way to the next: standing tall to bluff, huffing; the rake's paw drawn back on one side and swept
 * across, its shoulders turning with it; the snap's head drawn back, jaws wide, then driven forward and shut; reared
 * and trembling to crush, then slammed down; crouched with its rump wiggling to pounce, then stretched out in the air;
 * pawing the ground, head down, then galloping; dug in under a trunk, straining, then heaving it up and over; rump up,
 * scrabbling, then kicking earth back; reared with its forelegs spread wide to hug, then falling forward and mauling
 * whoever it pinned; reared to roar, head thrown back. Toppled it rolls over on its side, legs paddling; sprawled it
 * lies flat on its belly, panting; dazed it sways on splayed legs. Roused its hackles stand up and its ears go flat.</p>
 *
 * <p>Laid out on a 128x64 skin (see {@code tools/timbermaul_art.py}):</p>
 * <pre>
 *   body (0,0) 12x11x20     hump (64,0) 10x4x7      hackles (100,0) 3x2x10    moss (64,12) 9x1x12
 *   head (0,32) 8x7x6       muzzle (28,32) 4x3x4    jaw (44,32) 4x1x4         ear (60,32) 2x2x1    tail (68,32) 3x3x2
 *   foreleg (0,46) 4x10x4   hind leg (16,46) 4x10x5
 * </pre>
 */
public class TimbermaulBearModel extends EntityModel<MonsterRenderState> {
	private static final float HIPS_Y = 13.0F, HIPS_Z = 6.0F;
	private static final float HEAD_Y = -3.0F, HEAD_Z = -18.0F;
	private static final float FORE_Y = 1.0F, HIND_Y = 14.0F;

	/** How each part of it is set, as a pose: its hips reared up (front up), sunk, turned to its right and rolled. */
	private static final int RISE = 0, DROP = 1, TURN = 2, ROLL = 3;
	/** Its head bowed, turned to its right, drawn back and tilted; its jaw dropped. */
	private static final int HEAD_X = 4, HEAD_Y_ROT = 5, HEAD_BACK = 6, HEAD_ROLL = 7, JAW = 8;
	/** Its forelegs swung back (forward negative), out from its sides and twisted; both drawn up into it. */
	private static final int RF_X = 9, LF_X = 10, RF_Z = 11, LF_Z = 12, RF_Y = 13, LF_Y = 14, FORE_UP = 15;
	/** Its hind legs swung back and out; both let down. */
	private static final int RH_X = 16, LH_X = 17, RH_Z = 18, LH_Z = 19, HIND_DOWN = 20;
	/** Its tail raised, its ears laid flat, and how far over on its side it has rolled. */
	private static final int TAIL = 21, EARS = 22, TOPPLE = 23, CHANNELS = 24;

	private final ModelPart hips;
	private final ModelPart hackles;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart rightEar;
	private final ModelPart leftEar;
	private final ModelPart tail;
	private final ModelPart rightFore;
	private final ModelPart leftFore;
	private final ModelPart rightHind;
	private final ModelPart leftHind;

	public TimbermaulBearModel(ModelPart root) {
		super(root);
		this.hips = root.getChild("hips");
		this.hackles = hips.getChild("hackles");
		this.head = hips.getChild("head");
		this.jaw = head.getChild("jaw");
		this.rightEar = head.getChild("right_ear");
		this.leftEar = head.getChild("left_ear");
		this.tail = hips.getChild("tail");
		this.rightFore = hips.getChild("right_foreleg");
		this.leftFore = hips.getChild("left_foreleg");
		this.rightHind = root.getChild("right_hind_leg");
		this.leftHind = root.getChild("left_hind_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition hips = root.addOrReplaceChild("hips", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-6.0F, -7.0F, -18.0F, 12.0F, 11.0F, 20.0F)
				.texOffs(64, 0).addBox(-5.0F, -10.0F, -17.0F, 10.0F, 4.0F, 7.0F)
				.texOffs(64, 12).addBox(-4.5F, -7.6F, -11.0F, 9.0F, 1.0F, 12.0F),
			PartPose.offset(0.0F, HIPS_Y, HIPS_Z));
		hips.addOrReplaceChild("hackles", CubeListBuilder.create().texOffs(100, 0).addBox(-1.5F, -2.0F, 0.0F, 3.0F, 2.0F, 10.0F),
			PartPose.offset(0.0F, -9.0F, -12.0F));

		PartDefinition head = hips.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 32).addBox(-4.0F, -4.0F, -6.0F, 8.0F, 7.0F, 6.0F)
				.texOffs(28, 32).addBox(-2.0F, -0.5F, -10.0F, 4.0F, 3.0F, 4.0F),
			PartPose.offset(0.0F, HEAD_Y, HEAD_Z));
		head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(44, 32).addBox(-2.0F, 0.0F, -4.0F, 4.0F, 1.0F, 4.0F),
			PartPose.offset(0.0F, 2.5F, -6.0F));
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(60, 32).addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F),
			PartPose.offset(-3.0F, -4.0F, -2.0F));
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(60, 32).mirror().addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F),
			PartPose.offset(3.0F, -4.0F, -2.0F));

		hips.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(68, 32).addBox(-1.5F, -1.0F, 0.0F, 3.0F, 3.0F, 2.0F),
			PartPose.offsetAndRotation(0.0F, -5.0F, 1.5F, 0.4F, 0.0F, 0.0F));

		hips.addOrReplaceChild("right_foreleg", CubeListBuilder.create().texOffs(0, 46).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F),
			PartPose.offset(-4.0F, FORE_Y, -15.0F));
		hips.addOrReplaceChild("left_foreleg", CubeListBuilder.create().texOffs(0, 46).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F),
			PartPose.offset(4.0F, FORE_Y, -15.0F));
		root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(16, 46).addBox(-2.0F, 0.0F, -3.0F, 4.0F, 10.0F, 5.0F),
			PartPose.offset(-4.0F, HIND_Y, 4.0F));
		root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(16, 46).mirror().addBox(-2.0F, 0.0F, -3.0F, 4.0F, 10.0F, 5.0F),
			PartPose.offset(4.0F, HIND_Y, 4.0F));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(MonsterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float[] w = state.moves;
		float tell = Math.max(1, state.tell);

		// Each move's pose, weighted by how far it has eased in; for the move under way, by how far into it it is, and for
		// one easing out, as it ended.
		Pose pose = new Pose();
		float busy = 0;
		for (int move = TimbermaulRules.IDLE + 1; move < TimbermaulRules.MOVES; move++) {
			if (w[move] > 0.001F) {
				pose.add(poseFor(move, state, tell, t), w[move]);
				busy += w[move];
			}
		}
		float[] p = pose.v;
		float still = Mth.clamp(1 - busy, 0, 1);
		float look = 0.3F + 0.7F * still;
		float frenzy = state.roused ? 1 : 0;

		// The amble: legs in diagonal pairs, the shoulders rolling, the head low and swinging.
		float walk = state.walkAnimationPos * 0.6F;
		float stride = Math.min(1, state.walkAnimationSpeed * 1.5F) * still;
		float swing = Mth.cos(walk) * 0.65F * stride;

		hips.xRot = -p[RISE];
		hips.y = HIPS_Y + p[DROP] + Math.abs(Mth.sin(walk)) * 0.4F * stride;
		hips.yRot = p[TURN];
		hips.zRot = p[ROLL] + Mth.sin(walk) * 0.04F * stride;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * look + 0.18F + p[HEAD_X];
		head.yRot = Mth.clamp(state.yRot, -45, 45) * Mth.DEG_TO_RAD * look + Mth.sin(walk * 0.5F) * 0.08F * stride + p[HEAD_Y_ROT];
		head.zRot = p[HEAD_ROLL];
		head.z = HEAD_Z + p[HEAD_BACK];
		jaw.xRot = 0.04F + Mth.sin(t * 0.08F) * 0.03F * still + p[JAW];
		rightFore.xRot = p[RF_X] + swing;
		leftFore.xRot = p[LF_X] - swing;
		rightFore.zRot = p[RF_Z];
		leftFore.zRot = p[LF_Z];
		rightFore.yRot = p[RF_Y];
		leftFore.yRot = p[LF_Y];
		rightFore.y = leftFore.y = FORE_Y + p[FORE_UP];
		rightHind.xRot = p[RH_X] - swing;
		leftHind.xRot = p[LH_X] + swing;
		rightHind.zRot = p[RH_Z];
		leftHind.zRot = p[LH_Z];
		rightHind.y = leftHind.y = HIND_Y + p[HIND_DOWN];
		tail.xRot = 0.4F + Mth.sin(t * 0.1F) * 0.06F + p[TAIL];
		float flat = Math.min(1.1F, p[EARS] + 0.7F * frenzy);
		rightEar.xRot = leftEar.xRot = -flat;
		rightEar.zRot = -0.15F + Mth.sin(t * 0.06F) * 0.05F;
		leftEar.zRot = 0.15F - Mth.sin(t * 0.07F + 1) * 0.05F;
		hackles.yScale = 1 + 0.9F * frenzy;
		hackles.y = -9.0F - frenzy;

		float topple = p[TOPPLE];
		if (topple > 0) {
			// Rolled over on its side about the middle of its body, brought down to lie on the ground.
			ModelPart root = root();
			float roll = 1.35F * topple;
			float middle = hips.y - 1.5F;
			root.zRot = roll;
			root.x = middle * Mth.sin(roll);
			root.y = Mth.lerp(topple, middle, 18.0F) - middle * Mth.cos(roll);
		}
	}

	private static float ease(float x) {
		x = Mth.clamp(x, 0, 1);
		return x * x * (3 - 2 * x);
	}

	/** A pose for it: how far each part of it is set from standing, as the figures above. */
	private static final class Pose {
		final float[] v = new float[CHANNELS];

		Pose set(int channel, float x) {
			v[channel] = x;
			return this;
		}

		Pose plus(int channel, float x) {
			v[channel] += x;
			return this;
		}

		/** One foreleg (1 its right, -1 its left) swung back (forward negative) and out from its side. */
		Pose paw(int side, float swing, float out) {
			v[side > 0 ? RF_X : LF_X] = swing;
			v[side > 0 ? RF_Z : LF_Z] = side * out;
			return this;
		}

		Pose paws(float swing, float out) {
			return paw(1, swing, out).paw(-1, swing, out);
		}

		/** One hind leg swung back and out from its side. */
		Pose hind(int side, float swing, float out) {
			v[side > 0 ? RH_X : LH_X] = swing;
			v[side > 0 ? RH_Z : LH_Z] = side * out;
			return this;
		}

		Pose hinds(float swing, float out) {
			return hind(1, swing, out).hind(-1, swing, out);
		}

		/** One foreleg twisted, its paw turned in across its front. */
		Pose twist(int side, float turn) {
			v[side > 0 ? RF_Y : LF_Y] = side * turn;
			return this;
		}

		void add(Pose other, float weight) {
			for (int i = 0; i < CHANNELS; i++) {
				v[i] += other.v[i] * weight;
			}
		}

		Pose scale(float k) {
			Pose scaled = new Pose();
			for (int i = 0; i < CHANNELS; i++) {
				scaled.v[i] = v[i] * k;
			}
			return scaled;
		}

		static Pose mix(Pose a, Pose b, float k) {
			Pose mixed = new Pose();
			for (int i = 0; i < CHANNELS; i++) {
				mixed.v[i] = Mth.lerp(k, a.v[i], b.v[i]);
			}
			return mixed;
		}
	}

	/** Standing as it is drawn. */
	private static Pose rest() {
		return new Pose();
	}

	/**
	 * A move's pose: for the move under way, by how far into it it is; for one easing out, as it ended, or as it was
	 * broken off by what came after it.
	 */
	private static Pose poseFor(int move, MonsterRenderState state, float tell, float t) {
		boolean live = state.move == move;
		int now = state.move;
		float s = state.since;
		return switch (move) {
			case TimbermaulRules.BLUFF -> bluff(t);
			case TimbermaulRules.RAKE -> rake(live, s, tell, state.side, t);
			case TimbermaulRules.SNAP -> snap(live, s, tell, t);
			case TimbermaulRules.CRUSH -> crush(live, now, s, tell, t);
			case TimbermaulRules.TOPPLED -> toppled(t);
			case TimbermaulRules.POUNCE -> pounce(live, now, s, tell, t);
			case TimbermaulRules.SPRAWLED -> sprawled(t);
			case TimbermaulRules.CHARGE -> charge(live, now, s, tell, t);
			case TimbermaulRules.DAZED -> dazed(t);
			case TimbermaulRules.LOG -> log(live, s, tell, state.roused, t);
			case TimbermaulRules.DIG -> dig(live, s, tell, t);
			case TimbermaulRules.HUG -> hug(live, now, s, tell, t);
			case TimbermaulRules.PINNED -> pinned(s, t);
			case TimbermaulRules.ROAR -> roar(live, s, t);
			default -> rest();
		};
	}

	// Bluff: up on its hind legs, forelegs hanging, head up and swaying, huffing through a half-open jaw.
	private static Pose bluff(float t) {
		return rest().set(RISE, 1.15F + Mth.sin(t * 0.12F) * 0.03F).paws(0.8F, 0.2F).hinds(0.1F, 0).set(HEAD_X, 0.95F)
			.set(HEAD_Y_ROT, Mth.sin(t * 0.15F) * 0.12F).set(ROLL, Mth.sin(t * 0.12F) * 0.04F)
			.set(JAW, 0.2F + Math.abs(Mth.sin(t * 0.3F)) * 0.25F).set(EARS, 0.2F);
	}

	// Rake: one paw drawn up and out on the side it comes from, shoulders turned back with it and trembling; then swept
	// across its front, the shoulders swinging round after it; roused, the other paw back the other way.
	private static Pose rakeRaised(int side) {
		return rest().set(TURN, 0.35F * side).set(RISE, 0.3F).set(ROLL, -0.06F * side).paw(side, -1.6F, 0.7F).paw(-side, -0.15F, 0)
			.set(HEAD_X, 0.25F).set(HEAD_Y_ROT, -0.25F * side).set(JAW, 0.3F).set(EARS, 0.4F);
	}

	private static Pose rakeSwept(int side) {
		return rest().set(TURN, -0.45F * side).set(RISE, 0.1F).set(ROLL, 0.05F * side).paw(side, -1.0F, -0.5F).twist(side, 0.4F)
			.paw(-side, 0.1F, 0).set(HEAD_X, 0.1F).set(HEAD_Y_ROT, 0.3F * side).set(JAW, 0.5F).set(EARS, 0.3F);
	}

	private static Pose rake(boolean live, float s, float tell, int side, float t) {
		Pose swept = rakeSwept(side);
		Pose end = swept.scale(0.35F);
		if (!live) {
			return end;
		}
		Pose raised = rakeRaised(side);
		if (s < tell) {
			Pose from = rest();
			if (tell == TimbermaulRules.RAKE_BACK_TELL) {
				// The backhand starts from the first rake, swept the other way and coming back.
				Pose before = rakeSwept(-side);
				from = Pose.mix(before, before.scale(0.35F), ease(2.0F / TimbermaulRules.RAKE_RECOVER));
			}
			float r = ease(s / tell);
			return Pose.mix(from, raised, r).plus(side > 0 ? RF_X : LF_X, Mth.sin(t * 1.3F) * 0.05F * r);
		}
		float into = s - tell;
		if (into < TimbermaulRules.RAKE_SWEEP) {
			return Pose.mix(raised, swept, Mth.clamp(into / (TimbermaulRules.RAKE_SWEEP - 1), 0, 1));
		}
		return Pose.mix(swept, end, ease((into - TimbermaulRules.RAKE_SWEEP) / TimbermaulRules.RAKE_RECOVER));
	}

	// Snap: head drawn back into its shoulders, low, jaws wide and quivering; then driven forward, the jaws shut on the way.
	private static Pose snapCoiled() {
		return rest().set(RISE, -0.12F).set(DROP, 1.5F).set(FORE_UP, -1.5F).paws(-0.2F, 0.15F).hinds(0.35F, 0).set(HIND_DOWN, 0.6F)
			.set(HEAD_X, -0.2F).set(HEAD_BACK, 1.5F).set(JAW, 0.9F).set(EARS, 0.7F);
	}

	private static Pose snapLunged() {
		return rest().set(RISE, -0.05F).set(DROP, 0.5F).paws(-0.9F, 0.1F).hinds(0.5F, 0).set(HEAD_X, 0.15F).set(HEAD_BACK, -3.0F)
			.set(JAW, 0.05F).set(EARS, 0.5F);
	}

	private static Pose snap(boolean live, float s, float tell, float t) {
		Pose lunged = snapLunged();
		Pose end = lunged.scale(0.2F);
		if (!live) {
			return end;
		}
		Pose coiled = snapCoiled();
		if (s < tell) {
			Pose from = tell == TimbermaulRules.SNAP_AGAIN_TELL
				? Pose.mix(lunged, end, ease(2.0F / TimbermaulRules.SNAP_RECOVER)) : rest();
			float r = ease(s / tell);
			return Pose.mix(from, coiled, r).plus(HEAD_X, Mth.sin(t * 2.2F) * 0.03F * r);
		}
		float into = s - tell;
		if (into < TimbermaulRules.SNAP_LUNGE) {
			float jaw = into < 2 ? 1.0F : Mth.lerp(Mth.clamp((into - 2) / 1.5F, 0, 1), 1.0F, 0.05F);
			return Pose.mix(coiled, lunged, ease(into / 4)).set(JAW, jaw);
		}
		return Pose.mix(lunged, end, ease((into - TimbermaulRules.SNAP_LUNGE) / TimbermaulRules.SNAP_RECOVER));
	}

	// Crush: reared up tall, forelegs out and trembling, head up; then slammed down with its whole weight, the ground
	// giving under it, and its chest low after.
	private static Pose crushReared(float t) {
		return rest().set(RISE, 1.25F).paws(-0.3F + Mth.sin(t * 2.5F) * 0.05F, 0.45F).hinds(0.1F, 0).set(HEAD_X, 1.15F)
			.set(JAW, 0.55F).set(EARS, 0.4F).set(TAIL, 0.3F);
	}

	private static Pose crushSlammed() {
		return rest().set(RISE, -0.14F).set(DROP, 1.0F).set(FORE_UP, -1.0F).paws(-0.55F, 0.15F).hinds(0.15F, 0).set(HEAD_X, 0.35F)
			.set(JAW, 0.5F).set(EARS, 0.3F);
	}

	private static Pose crush(boolean live, int now, float s, float tell, float t) {
		Pose slammed = crushSlammed();
		Pose end = slammed.scale(0.3F);
		if (!live) {
			// Struck over while reared up, it goes over from there.
			return now == TimbermaulRules.TOPPLED ? crushReared(t) : end;
		}
		Pose reared = crushReared(t);
		float fall = tell - 2;
		if (s < fall) {
			Pose from = tell == TimbermaulRules.CRUSH_AGAIN_TELL
				? Pose.mix(slammed, end, ease(4.0F / TimbermaulRules.CRUSH_RECOVER)) : rest();
			return Pose.mix(from, reared, ease(s / Math.max(1, fall - 1)));
		}
		float land = tell + 0.5F;
		if (s < land) {
			float k = Mth.clamp((s - fall) / (land - fall), 0, 1);
			return Pose.mix(reared, slammed, k * k);
		}
		float after = s - land;
		return Pose.mix(slammed, end, ease(after / TimbermaulRules.CRUSH_RECOVER))
			.plus(DROP, 1.2F * Math.max(0, 1 - after / 3));
	}

	// Toppled: rolled over on its side, legs paddling at the air, jaw hanging.
	private static Pose toppled(float t) {
		return rest().set(TOPPLE, 1).set(RF_X, Mth.sin(t * 0.55F) * 0.6F).set(LF_X, Mth.sin(t * 0.55F + 1.6F) * 0.6F)
			.set(RH_X, Mth.sin(t * 0.55F + 3.1F) * 0.5F).set(LH_X, Mth.sin(t * 0.55F + 4.7F) * 0.5F).set(HEAD_X, 0.2F)
			.set(HEAD_ROLL, 0.35F).set(JAW, 0.45F + Mth.sin(t * 0.2F) * 0.1F).set(TAIL, 0.5F).set(EARS, 0.3F);
	}

	// Pounce: crouched low, rump up and wiggling more and more; then stretched out through the air, forelegs reaching,
	// and down on its forepaws; roused, crouched again where it landed for a second.
	private static Pose pounceCrouched(float t, float r) {
		float wiggle = Mth.sin(t * (0.6F + 0.6F * r)) * r;
		return rest().set(DROP, 2.5F).set(FORE_UP, -2.5F).set(RISE, 0.05F).hinds(0.45F, 0).set(HIND_DOWN, 1.0F)
			.set(ROLL, wiggle * 0.05F).set(TURN, wiggle * 0.06F).set(HEAD_X, -0.1F).set(JAW, 0.2F).set(EARS, 0.8F).set(TAIL, 0.4F);
	}

	private static Pose pounceFlying(float k) {
		return rest().set(RISE, Mth.lerp(k, 0.35F, -0.3F)).paws(-1.3F, 0.1F).hinds(1.1F, 0).set(HEAD_X, Mth.lerp(k, -0.3F, 0.2F))
			.set(JAW, 0.8F).set(EARS, 0.9F).set(TAIL, 0.3F);
	}

	private static Pose pounceLanded() {
		return rest().set(RISE, -0.2F).set(DROP, 1.5F).set(FORE_UP, -0.5F).paws(-0.6F, 0.2F).hinds(0.6F, 0).set(HEAD_X, 0.25F)
			.set(JAW, 0.6F).set(EARS, 0.5F);
	}

	private static Pose pounce(boolean live, int now, float s, float tell, float t) {
		if (!live) {
			return now == TimbermaulRules.SPRAWLED ? pounceLanded() : rest();
		}
		if (s < tell) {
			Pose from = tell == TimbermaulRules.POUNCE_AGAIN_TELL ? pounceFlying(1) : rest();
			return Pose.mix(from, pounceCrouched(t, ease(s / tell)), ease(s / Math.min(tell, 6)));
		}
		float into = s - tell;
		float flight = TimbermaulRules.POUNCE_FLIGHT - 1;
		if (into < flight) {
			return Pose.mix(pounceCrouched(t, 1), pounceFlying(into / flight), ease(into / 2));
		}
		float after = into - flight;
		if (after < 2) {
			return Pose.mix(pounceFlying(1), pounceLanded(), ease(after / 2));
		}
		return Pose.mix(pounceLanded(), rest(), ease((after - 2) / (TimbermaulRules.POUNCE_RECOVER - 1)));
	}

	// Sprawled: flat on its belly, legs splayed out front and back, panting.
	private static Pose sprawled(float t) {
		return rest().set(DROP, 5.5F).set(RISE, -0.08F).paws(-1.4F, 0.5F).hinds(1.4F, 0.3F).set(HIND_DOWN, 7.0F).set(HEAD_X, 0.1F)
			.set(JAW, 0.25F + Math.abs(Mth.sin(t * 0.35F)) * 0.25F).set(EARS, 0.4F).set(ROLL, Mth.sin(t * 0.35F) * 0.02F);
	}

	// Charge: head down, huffing, a forefoot dragged back through the dirt again and again; then a gallop, bounding, its
	// legs in pairs; and reared back on its haunches to stop; roused, wheeling round from there to run again.
	private static Pose chargePawing(float s, float t) {
		float scrape = (s % 5) / 5;
		return rest().set(RISE, -0.15F).set(HEAD_X, 0.45F).set(EARS, 0.8F).set(JAW, 0.15F + Math.abs(Mth.sin(t * 0.4F)) * 0.15F)
			.hinds(0.25F, 0).set(RF_X, Mth.lerp(scrape, -0.6F, 0.5F));
	}

	private static Pose chargeGallop(float into) {
		float g = into * 0.9F;
		float a = Mth.clamp(Math.min(into / 2, (TimbermaulRules.CHARGE_RUN - into) / 3), 0, 1);
		return rest().set(RF_X, Mth.sin(g + 0.3F) * 0.9F * a).set(LF_X, Mth.sin(g) * 0.9F * a)
			.set(RH_X, -Mth.sin(g) * 0.9F * a).set(LH_X, -Mth.sin(g + 0.3F) * 0.9F * a)
			.set(RISE, Mth.sin(g + 1) * 0.12F * a - 0.1F).set(DROP, -Math.abs(Mth.cos(g)) * 0.8F * a)
			.set(HEAD_X, 0.4F + Mth.sin(g) * 0.1F).set(JAW, 0.4F).set(EARS, 0.9F).set(TAIL, 0.5F);
	}

	private static Pose chargeBraked() {
		return rest().set(RISE, 0.45F).paws(-0.9F, 0.2F).hinds(-0.5F, 0).set(DROP, 1.0F).set(HEAD_X, 0).set(JAW, 0.5F);
	}

	private static Pose charge(boolean live, int now, float s, float tell, float t) {
		Pose braked = chargeBraked();
		Pose end = braked.scale(0.2F);
		if (!live) {
			// Run into something, it stands dazed from where it stopped.
			return now == TimbermaulRules.DAZED ? braked : end;
		}
		if (s < tell) {
			Pose from = tell == TimbermaulRules.CHARGE_AGAIN_TELL ? braked : rest();
			return Pose.mix(from, chargePawing(s, t), ease(s / Math.min(tell, 5)));
		}
		float into = s - tell;
		if (into < TimbermaulRules.CHARGE_RUN) {
			return Pose.mix(chargePawing(s, t), chargeGallop(into), ease(into / 2));
		}
		float after = into - TimbermaulRules.CHARGE_RUN;
		if (after < 3) {
			return Pose.mix(chargeGallop(TimbermaulRules.CHARGE_RUN), braked, ease(after / 3));
		}
		return Pose.mix(braked, end, ease((after - 3) / (TimbermaulRules.CHARGE_RECOVER - 3)));
	}

	// Dazed: swaying on splayed legs, head lolling from side to side.
	private static Pose dazed(float t) {
		return rest().set(ROLL, Mth.sin(t * 0.25F) * 0.12F).set(HEAD_Y_ROT, Mth.sin(t * 0.3F) * 0.35F)
			.set(HEAD_ROLL, Mth.sin(t * 0.3F + 1) * 0.2F).set(HEAD_X, 0.35F).set(JAW, 0.35F).paws(0, 0.25F).hinds(0, 0.2F)
			.set(DROP, 1.0F + Mth.sin(t * 0.5F) * 0.4F);
	}

	// Log: dug in low, claws hooked under the trunk and straining; then heaved up and over, forelegs flung high; roused,
	// down under a second one and heaving again.
	private static Pose logAim(float t) {
		return rest().set(RISE, -0.2F).set(DROP, 2.0F).paws(-0.7F + Mth.sin(t * 2.3F) * 0.04F, 0.1F).set(FORE_UP, -1.5F)
			.hinds(0.4F, 0).set(HIND_DOWN, 0.5F).set(HEAD_X, 0.1F).set(JAW, 0.25F).set(EARS, 0.3F);
	}

	private static Pose logHeave() {
		return rest().set(RISE, 0.7F).paws(-2.0F, 0.3F).set(HEAD_X, -0.2F).set(JAW, 0.8F).hinds(0.05F, 0).set(EARS, 0.1F);
	}

	private static Pose log(boolean live, float s, float tell, boolean roused, float t) {
		Pose heave = logHeave();
		Pose end = heave.scale(0.15F);
		if (!live) {
			return end;
		}
		Pose aim = logAim(t);
		if (s < tell) {
			return Pose.mix(rest(), aim, ease(s / Math.min(tell, 8)));
		}
		float into = s - tell;
		if (into < 3) {
			return Pose.mix(aim, heave, ease(into / 3));
		}
		if (!roused) {
			return into < 6 ? heave : Pose.mix(heave, end, ease((into - 6) / 6));
		}
		float again = TimbermaulRules.LOG_AGAIN;
		if (into < 5) {
			return heave;
		}
		if (into < again) {
			return Pose.mix(heave, aim, ease((into - 5) / 5));
		}
		if (into < again + 3) {
			return Pose.mix(aim, heave, ease((into - again) / 3));
		}
		return Pose.mix(heave, end, ease((into - again - 3) / 8));
	}

	// Dig: rump up and head down, forepaws scrabbling at the ground; then the hind legs kicking earth back in a spray.
	private static Pose digDug(float t) {
		float scrabble = Mth.sin(t * 1.5F) * 0.8F;
		return rest().set(RISE, -0.35F).set(DROP, -1.5F).set(FORE_UP, -3.0F).set(RF_X, -0.4F + scrabble).set(LF_X, -0.4F - scrabble)
			.set(HEAD_X, 0.5F).set(TAIL, 0.6F).set(JAW, 0.2F).set(EARS, 0.5F);
	}

	private static Pose digSpray(float t) {
		float kick = Mth.sin(t * 1.9F) * 0.9F;
		return rest().set(RH_X, 0.3F + kick).set(LH_X, 0.3F - kick).paws(-0.3F, 0.2F).set(FORE_UP, -2.5F).set(RISE, -0.25F)
			.set(DROP, -1.0F).set(HEAD_X, 0.4F).set(TAIL, 0.8F).set(EARS, 0.5F);
	}

	private static Pose dig(boolean live, float s, float tell, float t) {
		if (!live) {
			return rest();
		}
		if (s < tell) {
			return Pose.mix(rest(), digDug(t), ease(s / 4));
		}
		float into = s - tell;
		if (into < TimbermaulRules.DIG_SPRAY) {
			return Pose.mix(digDug(t), digSpray(t), ease(into / 2));
		}
		return Pose.mix(digSpray(t), rest(), ease((into - TimbermaulRules.DIG_SPRAY) / TimbermaulRules.DIG_RECOVER));
	}

	// Hug: reared up with its forelegs spread wide, swaying; then falling forward onto whoever stands before it.
	private static Pose hugWide(float t) {
		return rest().set(RISE, 1.2F).paws(-0.5F, 1.0F).set(HEAD_X, 0.9F).set(JAW, 0.6F).set(EARS, 0.6F).hinds(0.1F, 0)
			.set(ROLL, Mth.sin(t * 0.2F) * 0.05F).set(TURN, Mth.sin(t * 0.15F) * 0.08F).set(TAIL, 0.3F);
	}

	private static Pose hugFallen() {
		return rest().set(RISE, -0.25F).paws(-1.2F, -0.3F).set(DROP, 3.0F).set(HEAD_X, 0.3F).set(JAW, 0.9F).hinds(0.6F, 0)
			.set(HIND_DOWN, 1.0F);
	}

	private static Pose hug(boolean live, int now, float s, float tell, float t) {
		if (!live) {
			// Struck over while reared up, it goes over from there; otherwise it is down, on them or on its belly.
			return now == TimbermaulRules.TOPPLED ? hugWide(t) : hugFallen();
		}
		if (s < tell) {
			return Pose.mix(rest(), hugWide(t), ease(s / Math.min(tell, 8)));
		}
		float k = Mth.clamp((s - tell) / (TimbermaulRules.HUG_FALL - 1), 0, 1);
		return Pose.mix(hugWide(t), hugFallen(), k * k);
	}

	// Pinned: lying on whoever it caught, forelegs wrapped in, rocking; now and then its head drawn back, jaws opening,
	// and driven down on them.
	private static Pose pinned(float s, float t) {
		Pose pose = rest().set(RISE, -0.2F).set(DROP, 3.5F).paws(-1.2F, -0.3F).set(HEAD_X, 0.4F).hinds(0.6F, 0.15F)
			.set(HIND_DOWN, 1.0F).set(EARS, 0.6F).set(ROLL, Mth.sin(t * 0.6F) * 0.05F).set(TURN, Mth.sin(t * 0.45F) * 0.05F)
			.set(JAW, 0.2F);
		if (s < 9) {
			return pose;
		}
		float every = TimbermaulRules.HUG_MAUL_EVERY;
		float m = s % every;
		if (m >= every - 6 && m < every - 1) {
			float u = (m - (every - 6)) / 5;
			return pose.set(JAW, 0.2F + 0.9F * u).set(HEAD_X, 0.4F - 0.5F * u).set(HEAD_BACK, u);
		}
		if (m >= every - 1) {
			float d = m - (every - 1);
			return pose.set(HEAD_BACK, Mth.lerp(d, 1.0F, -2.0F)).set(HEAD_X, Mth.lerp(d, -0.1F, 0.7F)).set(JAW, Mth.lerp(d, 1.1F, 0.05F));
		}
		if (m < 4) {
			float r = ease(m / 4);
			return pose.set(HEAD_BACK, Mth.lerp(r, -2.0F, 0)).set(HEAD_X, Mth.lerp(r, 0.7F, 0.4F)).set(JAW, Mth.lerp(r, 0.05F, 0.2F));
		}
		return pose;
	}

	// Roar: reared up, forelegs hanging, head up; then the roar, head thrown forward, jaws wide, forelegs flung out.
	private static Pose roarUp() {
		return rest().set(RISE, 1.1F).paws(0.6F, 0.25F).set(HEAD_X, 1.0F).set(JAW, 0.3F).set(EARS, 0.5F).hinds(0.1F, 0);
	}

	private static Pose roaring(float t) {
		return rest().set(RISE, 1.15F).paws(0.4F, 0.75F).set(HEAD_X, 0.4F + Mth.sin(t * 1.8F) * 0.05F)
			.set(HEAD_Y_ROT, Mth.sin(t * 1.1F) * 0.1F).set(JAW, 1.1F).set(EARS, 1.0F).hinds(0.1F, 0);
	}

	private static Pose roar(boolean live, float s, float t) {
		if (!live) {
			return rest();
		}
		float tell = TimbermaulRules.ROAR_TELL;
		float done = TimbermaulRules.ROAR_TICKS - 6;
		if (s < 8) {
			return Pose.mix(rest(), roarUp(), ease(s / 8));
		}
		if (s < tell) {
			return roarUp();
		}
		if (s < tell + 2) {
			return Pose.mix(roarUp(), roaring(t), ease((s - tell) / 2));
		}
		if (s < done) {
			return roaring(t);
		}
		return Pose.mix(roaring(t), rest(), ease((s - done) / 6));
	}
}
