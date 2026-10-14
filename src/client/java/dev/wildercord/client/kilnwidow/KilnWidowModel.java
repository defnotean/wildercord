package dev.wildercord.client.kilnwidow;

import dev.wildercord.Wildercord;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

import static dev.wildercord.world.kilnwidow.KilnWidowRules.*;

public final class KilnWidowModel extends EntityModel<KilnWidowRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("kiln_widow"), "main");
	private final ModelPart hearth, dome, chimney, slagCore, damperLeft, damperRight, legFl, legFr, legBl, legBr;

	public KilnWidowModel(ModelPart root) {
		super(root);
		hearth = root.getChild("hearth");
		dome = hearth.getChild("dome");
		chimney = dome.getChild("chimney");
		slagCore = dome.getChild("slag_core");
		damperLeft = dome.getChild("damper_left");
		damperRight = dome.getChild("damper_right");
		legFl = hearth.getChild("leg_fl");
		legFr = hearth.getChild("leg_fr");
		legBl = hearth.getChild("leg_bl");
		legBr = hearth.getChild("leg_br");
	}

	public static LayerDefinition createBodyLayer() {
		var mesh = new MeshDefinition();
		var root = mesh.getRoot();
		root.addOrReplaceChild("plinth_base", CubeListBuilder.create()
			.texOffs(0, 0).addBox(-16, -4, -16, 32, 4, 32)
			.texOffs(0, 36).addBox(-12, -8, -12, 24, 4, 24), PartPose.offset(0, 24, 0));
		var hearth = root.addOrReplaceChild("hearth", CubeListBuilder.create()
			.texOffs(0, 64).addBox(-10, -14, -10, 20, 6, 20), PartPose.offset(0, 24, 0));
		var dome = hearth.addOrReplaceChild("dome", CubeListBuilder.create()
			.texOffs(0, 90).addBox(-8, -14, -8, 16, 14, 16), PartPose.offset(0, -14, 0));
		dome.addOrReplaceChild("chimney", CubeListBuilder.create()
			.texOffs(64, 90).addBox(-5, -22, -5, 10, 8, 10), PartPose.offset(0, 0, 0));
		dome.addOrReplaceChild("slag_core", CubeListBuilder.create()
			.texOffs(128, 48).addBox(-4, -10, -10, 8, 8, 4), PartPose.offset(0, 0, 0));
		dome.addOrReplaceChild("damper_left", CubeListBuilder.create()
			.texOffs(128, 0).addBox(0, -12, -6, 5, 12, 12), PartPose.offset(8, 0, 0));
		dome.addOrReplaceChild("damper_right", CubeListBuilder.create()
			.texOffs(128, 0).mirror().addBox(-5, -12, -6, 5, 12, 12), PartPose.offset(-8, 0, 0));
		hearth.addOrReplaceChild("leg_fl", CubeListBuilder.create()
			.texOffs(164, 0).addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(10, -12, -8));
		hearth.addOrReplaceChild("leg_fr", CubeListBuilder.create()
			.texOffs(164, 0).mirror().addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(-10, -12, -8));
		hearth.addOrReplaceChild("leg_bl", CubeListBuilder.create()
			.texOffs(164, 0).addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(10, -12, 8));
		hearth.addOrReplaceChild("leg_br", CubeListBuilder.create()
			.texOffs(164, 0).mirror().addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(-10, -12, 8));
		return LayerDefinition.create(mesh, 256, 128);
	}

	@Override public void setupAnim(KilnWidowRenderState s) {
		super.setupAnim(s);
		float t = s.ageInTicks;
		hearth.y += Mth.sin(t * .06F) * .35F;
		chimney.yRot = t * .02F;
		slagCore.zRot = Mth.sin(t * .09F) * .12F;
		damperLeft.yRot = -.1F + Mth.sin(t * .05F) * .04F;
		damperRight.yRot = .1F - Mth.sin(t * .05F) * .04F;
		legFl.zRot = -.22F + Mth.sin(t * .07F) * .05F;
		legFr.zRot = .22F - Mth.sin(t * .07F) * .05F;
		legBl.zRot = -.22F - Mth.sin(t * .07F) * .05F;
		legBr.zRot = .22F + Mth.sin(t * .07F) * .05F;
		float tell = Math.max(1, tell(s.move)), wind = smooth(s.elapsed / tell);
		float strike = smooth((s.elapsed - tell) / 6), recover = smooth((s.elapsed - tell - 6) / Math.max(1, recovery(s.move) - 6));
		float held = wind * (1 - recover);
		if (s.move == DRAFT) {
			dome.y -= 1.4F * held;
			damperLeft.zRot = -.45F * held;
			damperRight.zRot = .45F * held;
			chimney.y -= 1.0F * strike * held;
		} else if (s.move == SLAG) {
			dome.xRot = (.28F - .55F * strike) * held;
			slagCore.z -= 1.8F * held;
		} else if (s.move == SWEEP) {
			damperLeft.yRot = -.65F * held;
			damperRight.yRot = .65F * held;
			legFl.xRot = -.35F * held;
			legFr.xRot = -.35F * held;
		}
		if (s.second) {
			chimney.y -= .8F;
			legFl.zRot -= .1F;
			legFr.zRot += .1F;
		}
		if (s.deathTime > 0) {
			hearth.y += Math.min(6, s.deathTime * .35F);
			dome.xRot += Math.min(.6F, s.deathTime * .035F);
		}
	}

	private static float smooth(float x) {
		x = Mth.clamp(x, 0, 1);
		return x * x * (3 - 2 * x);
	}
}
