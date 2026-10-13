package dev.wildercord.client.hollowbell;

import dev.wildercord.Wildercord;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

import static dev.wildercord.world.hollowbell.HollowBellRules.*;

public final class HollowBellModel extends EntityModel<HollowBellRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("hollow_bell"), "main");
	private final ModelPart yoke, mantle, skirt, clapper, shutterLeft, shutterRight, crown, gauge;

	public HollowBellModel(ModelPart root) {
		super(root);
		yoke = root.getChild("yoke");
		mantle = yoke.getChild("mantle");
		skirt = mantle.getChild("skirt");
		clapper = mantle.getChild("clapper");
		shutterLeft = mantle.getChild("shutter_left");
		shutterRight = mantle.getChild("shutter_right");
		crown = yoke.getChild("crown");
		gauge = mantle.getChild("gauge");
	}

	public static LayerDefinition createBodyLayer() {
		var mesh = new MeshDefinition();
		var root = mesh.getRoot();
		root.addOrReplaceChild("plinth_base", CubeListBuilder.create()
			.texOffs(0, 0).addBox(-16, -4, -16, 32, 4, 32)
			.texOffs(0, 36).addBox(-12, -8, -12, 24, 4, 24), PartPose.offset(0, 24, 0));
		var yoke = root.addOrReplaceChild("yoke", CubeListBuilder.create()
			.texOffs(0, 64).addBox(-11, -30, -5, 22, 6, 10), PartPose.offset(0, 14, 0));
		yoke.addOrReplaceChild("crown", CubeListBuilder.create()
			.texOffs(40, 94).addBox(-8, -36, -8, 16, 6, 16), PartPose.offset(0, 0, 0));
		var mantle = yoke.addOrReplaceChild("mantle", CubeListBuilder.create()
			.texOffs(0, 80).addBox(-8, 0, -8, 16, 14, 16), PartPose.offset(0, -24, 0));
		mantle.addOrReplaceChild("skirt", CubeListBuilder.create()
			.texOffs(64, 64).addBox(-10, 14, -10, 20, 8, 20), PartPose.offset(0, 0, 0));
		mantle.addOrReplaceChild("clapper", CubeListBuilder.create()
			.texOffs(150, 0).addBox(-2, 4, -2, 4, 20, 4)
			.texOffs(166, 0).addBox(-4, 22, -4, 8, 7, 8), PartPose.offset(0, 0, 0));
		mantle.addOrReplaceChild("shutter_left", CubeListBuilder.create()
			.texOffs(128, 0).addBox(0, 2, -6, 5, 18, 12), PartPose.offset(9, 0, 0));
		mantle.addOrReplaceChild("shutter_right", CubeListBuilder.create()
			.texOffs(128, 0).mirror().addBox(-5, 2, -6, 5, 18, 12), PartPose.offset(-9, 0, 0));
		mantle.addOrReplaceChild("gauge", CubeListBuilder.create()
			.texOffs(128, 48).addBox(-4, 4, -10, 8, 8, 4), PartPose.offset(0, 0, 0));
		return LayerDefinition.create(mesh, 256, 128);
	}

	@Override public void setupAnim(HollowBellRenderState s) {
		super.setupAnim(s);
		float t = s.ageInTicks;
		yoke.y += Mth.sin(t * .06F) * .45F;
		crown.yRot = t * .025F;
		clapper.xRot = Mth.sin(t * .07F) * .08F;
		clapper.zRot = Mth.cos(t * .05F) * .06F;
		gauge.zRot = Mth.sin(t * .09F) * .12F;
		shutterLeft.yRot = -.12F + Mth.sin(t * .05F) * .04F;
		shutterRight.yRot = .12F - Mth.sin(t * .05F) * .04F;
		float tell = Math.max(1, tell(s.move)), wind = smooth(s.elapsed / tell);
		float strike = smooth((s.elapsed - tell) / 6), recover = smooth((s.elapsed - tell - 6) / Math.max(1, recovery(s.move) - 6));
		float held = wind * (1 - recover);
		if (s.move == TOLL) {
			mantle.xRot = (-.38F + .72F * strike) * held;
			clapper.xRot = (.65F - 1.25F * strike) * held;
			skirt.y -= 1.2F * held;
		} else if (s.move == CLAPPER) {
			clapper.y += (-3.5F + 9.5F * strike) * held;
			clapper.zRot = .28F * held * (1 - strike);
			mantle.zRot = -.16F * held;
		} else if (s.move == CHORD) {
			shutterLeft.zRot = -.58F * held;
			shutterRight.zRot = .58F * held;
			gauge.z -= 1.6F * held;
		}
		if (s.second) {
			crown.y -= .6F;
			mantle.xRot += -.04F;
		}
		if (s.deathTime > 0) {
			yoke.y += Math.min(8, s.deathTime * .4F);
			mantle.xRot += Math.min(.7F, s.deathTime * .04F);
		}
	}

	private static float smooth(float x) {
		x = Mth.clamp(x, 0, 1);
		return x * x * (3 - 2 * x);
	}
}
