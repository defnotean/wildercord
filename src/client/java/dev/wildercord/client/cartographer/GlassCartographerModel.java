package dev.wildercord.client.cartographer;

import dev.wildercord.Wildercord;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

import static dev.wildercord.world.cartographer.CartographerRules.*;

public final class GlassCartographerModel extends EntityModel<GlassCartographerRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("glass_cartographer"), "main");
	private final ModelPart torso, head, ring, wingLeft, wingRight, rightArm, transit, leftArm, plumbWeight, lens;

	public GlassCartographerModel(ModelPart root) {
		super(root);
		torso = root.getChild("torso");
		head = torso.getChild("head");
		ring = head.getChild("ring");
		wingLeft = torso.getChild("wing_left");
		wingRight = torso.getChild("wing_right");
		rightArm = torso.getChild("right_arm");
		transit = rightArm.getChild("transit");
		leftArm = torso.getChild("left_arm");
		plumbWeight = leftArm.getChild("plumb_weight");
		lens = torso.getChild("lens");
	}

	public static LayerDefinition createBodyLayer() {
		var mesh = new MeshDefinition();
		var root = mesh.getRoot();
		root.addOrReplaceChild("pedestal_base", CubeListBuilder.create()
			.texOffs(0, 0).addBox(-16, -4, -16, 32, 4, 32)
			.texOffs(0, 36).addBox(-12, -8, -12, 24, 4, 24), PartPose.offset(0, 24, 0));
		var torso = root.addOrReplaceChild("torso", CubeListBuilder.create()
			.texOffs(0, 64).addBox(-7, -24, -5, 14, 20, 10)
			.texOffs(48, 64).addBox(-8, -4, -6, 16, 10, 12), PartPose.offset(0, 14, 0));
		var head = torso.addOrReplaceChild("head", CubeListBuilder.create()
			.texOffs(0, 94).addBox(-5, -9, -5, 10, 9, 10), PartPose.offset(0, -24, 0));
		head.addOrReplaceChild("ring", CubeListBuilder.create()
			.texOffs(40, 94).addBox(-8, -14, -8, 16, 6, 16), PartPose.offset(0, 0, 0));
		torso.addOrReplaceChild("wing_left", CubeListBuilder.create()
			.texOffs(104, 64).addBox(0, -2, -6, 6, 22, 12), PartPose.offset(7, -22, 0));
		torso.addOrReplaceChild("wing_right", CubeListBuilder.create()
			.texOffs(104, 64).mirror().addBox(-6, -2, -6, 6, 22, 12), PartPose.offset(-7, -22, 0));
		var rightArm = torso.addOrReplaceChild("right_arm", CubeListBuilder.create()
			.texOffs(128, 0).addBox(-4, -2, -3, 5, 18, 6), PartPose.offset(-10, -21, 0));
		rightArm.addOrReplaceChild("transit", CubeListBuilder.create()
			.texOffs(150, 0).addBox(-1, -18, -2, 2, 38, 4)
			.texOffs(162, 0).addBox(-3, 18, -3, 6, 10, 6), PartPose.offset(-1, 10, -2));
		var leftArm = torso.addOrReplaceChild("left_arm", CubeListBuilder.create()
			.texOffs(128, 24).addBox(-1, -2, -3, 5, 16, 6), PartPose.offset(10, -21, 0));
		leftArm.addOrReplaceChild("plumb_weight", CubeListBuilder.create()
			.texOffs(150, 42).addBox(-3, 0, -3, 6, 9, 6), PartPose.offset(1, 13, -3));
		torso.addOrReplaceChild("lens", CubeListBuilder.create()
			.texOffs(128, 48).addBox(-3, -6, -8, 6, 8, 6), PartPose.offset(0, -14, -2));
		return LayerDefinition.create(mesh, 256, 128);
	}

	@Override public void setupAnim(GlassCartographerRenderState s) {
		super.setupAnim(s);
		float t = s.ageInTicks, amp = s.second ? 1.35F : 1, rad = (float) Math.PI / 180F;
		boolean dying = s.deathTime > 0;
		// Idle: it floats, and each part trails the one above it with its own period.
		torso.y += Mth.sin(t * .06F) * .45F * amp;
		torso.yRot = Mth.sin(t * .031F) * .05F; torso.zRot = Mth.sin(t * .047F + .5F) * .025F;
		head.yRot += s.yRot * rad * .35F + Mth.sin(t * .04F - .8F) * .06F;
		head.xRot += s.xRot * rad * .25F + Mth.sin(t * .05F - .5F) * .03F;
		ring.xRot = Mth.sin(t * .05F) * .14F * amp; ring.yRot = t * (s.second ? .05F : .03F) + Mth.sin(t * .11F) * .1F; ring.zRot = Mth.cos(t * .05F - .6F) * .08F * amp;
		lens.zRot = Mth.sin(t * .08F) * .09F; lens.xRot = Mth.sin(t * .065F + 1) * .05F; lens.y += Mth.sin(t * .07F + .5F) * .4F;
		wingLeft.yRot = -.15F + Mth.sin(t * .05F + .4F) * .05F; wingRight.yRot = .15F - Mth.sin(t * .05F + .4F) * .05F;
		wingLeft.zRot = Mth.sin(t * .06F - .6F) * .05F; wingRight.zRot = -Mth.sin(t * .06F - .3F) * .05F;
		rightArm.xRot = Mth.sin(t * .05F + .9F) * .05F; rightArm.zRot = .06F + Mth.sin(t * .07F) * .03F;
		leftArm.xRot = Mth.sin(t * .05F + 1.4F) * .05F; leftArm.zRot = -.06F - Mth.sin(t * .07F + .7F) * .03F;
		transit.xRot = Mth.sin(t * .06F - .4F) * .05F; transit.zRot = Mth.sin(t * .045F) * .03F;
		plumbWeight.xRot = Mth.sin(t * .075F + .5F) * .1F; plumbWeight.zRot = Mth.sin(t * .06F) * .06F;
		if (s.second) { ring.y -= .6F; torso.xRot = -.03F; }

		if (s.move != IDLE && !dying) {
			float T = Math.max(1, tell(s.move)), R = Math.max(8, recovery(s.move)), el = Math.max(0, s.elapsed), dt = el - T;
			float wind = smooth(el / T), sk = burst(dt), rec = dt > 4 ? smooth((dt - 4) / (R - 4)) : 0, w = 1 - rec;
			float tw = wind * (1 - Mth.clamp(sk, 0, 1)) * w, st = sk * w, jolt = jolt(dt) * w;
			float quiver = el < T ? Mth.sin(el * 2.4F) * smooth((el - (T - 10)) / 10) * amp : 0;
			float stag = dt > 1 ? (float) Math.exp(-dt * .1F) * Mth.sin(dt * .55F) * w : 0;
			float slump = dt > 2 ? smooth((dt - 2) / 6) * w : 0;
			torso.zRot += quiver * .03F; ring.zRot += quiver * .1F;
			if (s.move == SWEEP) {
				// Survey staff cocked back over the shoulder, torso wound up and the head turned away; then the staff is whipped across, torso and head following a tick behind.
				rightArm.xRot += -1.15F * tw - .2F * st; rightArm.yRot += -.95F * tw + .9F * st + stag * .2F;
				transit.xRot += .85F * tw - .35F * st + stag * .5F;
				torso.yRot += -.3F * tw + .3F * burst(dt - 1) * w; torso.xRot += -.1F * tw - .12F * st + .1F * slump; torso.y += tw;
				head.yRot += .25F * tw - .35F * burst(dt - 2) * w; head.xRot += .15F * slump;
				ring.xRot += .5F * tw; ring.zRot += -.4F * tw + .3F * st;
				lens.z -= 1.2F * tw; leftArm.xRot += .5F * tw - .6F * st; leftArm.zRot += -.2F * tw;
				wingLeft.yRot += -.2F * tw + .3F * burst(dt - 2) * w; wingRight.yRot += .2F * tw - .3F * burst(dt - 2) * w;
				wingLeft.zRot += .2F * slump; wingRight.zRot -= .2F * slump;
			} else if (s.move == PLUMB) {
				// Back arched and the plumb weight hoisted overhead, swinging; wings lifted and the face turned to the sky; then the arm is slammed down, the weight dropped on its line and the whole body folds after it.
				leftArm.xRot += -1.75F * tw + .5F * st; leftArm.zRot += -.32F * tw;
				plumbWeight.y += 4 * tw + 9 * st; plumbWeight.xRot += Mth.sin(el * .35F) * .2F * tw + Mth.sin(dt * .5F) * .5F * (float) Math.exp(-Math.max(0, dt) * .1F) * (dt > 0 ? 1 : 0) * w;
				torso.xRot += -.2F * tw + .38F * st; torso.y += 2 * jolt;
				head.xRot += -.3F * tw + .45F * burst(dt - 1) * w;
				wingLeft.zRot += -.35F * tw + .25F * slump; wingRight.zRot += .35F * tw - .25F * slump;
				ring.xRot += -.4F * tw; lens.z -= tw;
				rightArm.xRot += .35F * tw; rightArm.zRot += .25F * tw - .25F * st;
			} else if (s.move == GRID) {
				// Wings flung wide, arms spread and the lens hoisted up and forward while the figure rises and trembles; then the wings clap, the arms cross down and it drops.
				wingLeft.zRot += -.62F * tw + .25F * slump; wingRight.zRot += .62F * tw - .25F * slump;
				wingLeft.yRot += -.7F * st; wingRight.yRot += .7F * st;
				rightArm.zRot += .9F * tw - 1F * st; leftArm.zRot += -.9F * tw + 1F * st;
				rightArm.xRot += -.3F * tw; leftArm.xRot += -.3F * tw;
				lens.y -= 3.2F * tw; lens.z -= 1.8F * tw + 2F * st; lens.zRot += Mth.sin(el * .8F) * .2F * tw;
				torso.y += -2 * tw + 3 * jolt; torso.xRot += -.1F * tw + .15F * st;
				head.xRot += -.2F * tw; ring.xRot += .3F * tw + .2F * jolt;
				ring.yRot += Mth.sin(el * 1.7F) * .2F * quiver;
			}
		}
		if (s.hurt > 0 && !dying) {
			// A blow: it rocks back, the ring and lens judder and the wings flare.
			float h = s.hurt;
			torso.xRot += -.25F * h; torso.zRot += Mth.sin(h * 14) * .06F * h; torso.y += .8F * h; head.xRot += -.2F * h;
			ring.zRot += Mth.sin(h * 16) * .35F * h; lens.zRot += Mth.sin(h * 12) * .4F * h;
			wingLeft.zRot += -.4F * h; wingRight.zRot += .4F * h; rightArm.xRot += .3F * h; leftArm.xRot += .3F * h;
		}
		if (dying) {
			// Dying: it reels and the ring spins wild; then the arms fall, the wings sag and the staff drops; then it crumples forward onto its plinth, the ring tipping loose.
			float d = s.deathTime, s1 = smooth(d / 6), s2 = smooth((d - 5) / 9), s3 = smooth((d - 10) / 10);
			torso.xRot += -.3F * s1 + 1.0F * s2; torso.zRot += Mth.sin(d * 1.7F) * .05F * s1 * (1 - s2) + .15F * s3;
			head.xRot += .7F * s2; head.zRot += .25F * s3; ring.yRot += d * d * .02F * (1 - s3); ring.zRot += Mth.sin(d * 2F) * .3F * s1 + .8F * s3; ring.xRot += .4F * s3; ring.y += 3 * s3;
			rightArm.xRot += .9F * s2; leftArm.xRot += .9F * s2; rightArm.zRot += .3F * s2; leftArm.zRot -= .3F * s2;
			transit.xRot += -.5F * s2; plumbWeight.y += 3 * s2; plumbWeight.xRot += Mth.sin(d * 1.1F) * .3F * (1 - s3);
			wingLeft.zRot += -.3F * s1 + .75F * s2; wingRight.zRot += .3F * s1 - .75F * s2; lens.y += 4 * s2; lens.zRot += .5F * s3;
			torso.y += Math.min(8, 8 * s3 + 3 * s2) + Mth.sin(d * 1.1F) * .4F * s3;
		}
	}

	private static float smooth(float x) {
		x = Mth.clamp(x, 0, 1);
		return x * x * (3 - 2 * x);
	}
	/** 0 until the strike, then a two-tick snap that overshoots by a fifth and settles. */
	private static float burst(float dt) {
		if (dt <= 0) return 0;
		float k = Mth.clamp(dt / 2, 0, 1);
		return 1 - (1 - k) * (1 - k) + .2F * (float) Math.exp(-dt * .18F) * Mth.sin(dt * .75F);
	}
	/** An impact that lands in a tick and rings down. */
	private static float jolt(float dt) { return dt > 0 ? Mth.clamp(dt, 0, 1) * (float) Math.exp(-dt * .2F) * Mth.cos(dt * .55F) : 0; }
}
