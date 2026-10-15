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
		float t = s.ageInTicks;
		torso.y += Mth.sin(t * .06F) * .45F;
		ring.xRot = Mth.sin(t * .05F) * .14F;
		ring.yRot = t * .03F;
		ring.zRot = Mth.cos(t * .05F) * .08F;
		lens.zRot = Mth.sin(t * .08F) * .09F;
		wingLeft.yRot = -.15F + Mth.sin(t * .05F) * .04F;
		wingRight.yRot = .15F - Mth.sin(t * .05F) * .04F;
		head.yRot += s.yRot * ((float) Math.PI / 180F) * .35F;
		head.xRot += s.xRot * ((float) Math.PI / 180F) * .25F;
		float tell = Math.max(1, tell(s.move)), wind = smooth(s.elapsed / tell);
		float strike = smooth((s.elapsed - tell) / 6), recover = smooth((s.elapsed - tell - 6) / Math.max(1, recovery(s.move) - 6));
		float held = wind * (1 - recover);
		if (s.move == SWEEP) {
			rightArm.xRot = (-1.15F + .95F * strike) * held;
			rightArm.yRot = (-.95F + 1.85F * strike) * held;
			transit.xRot = (.85F - 1.2F * strike) * held;
			torso.yRot = (-.18F + .36F * strike) * held;
		} else if (s.move == PLUMB) {
			leftArm.xRot = -1.75F * held * (1 - strike);
			leftArm.zRot = -.32F * held;
			plumbWeight.y += (4F - 10F * strike) * held;
		} else if (s.move == GRID) {
			wingLeft.zRot = -.62F * held;
			wingRight.zRot = .62F * held;
			lens.y -= 3.2F * held;
			lens.z -= 1.8F * held;
		}
		if (s.second) {
			ring.y -= .6F;
			torso.xRot = -.03F;
		}
		if (s.deathTime > 0) {
			torso.y += Math.min(8, s.deathTime * .4F);
			torso.xRot += Math.min(.7F, s.deathTime * .04F);
		}
	}

	private static float smooth(float x) {
		x = Mth.clamp(x, 0, 1);
		return x * x * (3 - 2 * x);
	}
}
