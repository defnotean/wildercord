package dev.wildercord.client.monster;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Custom 128x128 Karakuri Sentinel rig matching the samurai automaton turnaround &amp; pose sheet:
 * carved wooden menpo mask &amp; kabuto with crescent kuwagata horns, counter-rotating chest gears,
 * spinning rear brass winding key, lacquered kusazuri hip tassets, sode pauldrons, 2-segment arms
 * wielding a vertical-grip naginata polearm &amp; lantern buckler shield, and 2-segment legs.
 */
public class KarakuriSentinelModel extends EntityModel<MonsterRenderState> {
	private final ModelPart body;
	private final ModelPart mainGear;
	private final ModelPart pinionGear;
	private final ModelPart windingKey;
	private final ModelPart kusazuri;
	private final ModelPart head;
	private final ModelPart rightArm;
	private final ModelPart rightForearm;
	private final ModelPart leftArm;
	private final ModelPart leftForearm;
	private final ModelPart rightLeg;
	private final ModelPart rightGreave;
	private final ModelPart leftLeg;
	private final ModelPart leftGreave;

	public KarakuriSentinelModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.mainGear = this.body.getChild("main_gear");
		this.pinionGear = this.body.getChild("pinion_gear");
		this.windingKey = this.body.getChild("winding_key");
		this.kusazuri = this.body.getChild("kusazuri");
		this.head = this.body.getChild("head");
		this.rightArm = this.body.getChild("right_arm");
		this.rightForearm = this.rightArm.getChild("right_forearm");
		this.leftArm = this.body.getChild("left_arm");
		this.leftForearm = this.leftArm.getChild("left_forearm");
		this.rightLeg = root.getChild("right_leg");
		this.rightGreave = this.rightLeg.getChild("right_greave");
		this.leftLeg = root.getChild("left_leg");
		this.leftGreave = this.leftLeg.getChild("left_greave");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Waist-hinged armored torso (hip pivot Y = 14.0).
		PartDefinition body = root.addOrReplaceChild("body",
			CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -10.0F, -3.0F, 8, 10, 6),
			PartPose.offset(0.0F, 14.0F, 0.0F));
		body.addOrReplaceChild("main_gear",
			CubeListBuilder.create().texOffs(30, 0).addBox(-2.5F, -2.5F, -0.5F, 5, 5, 1),
			PartPose.offset(-0.8F, -5.0F, -3.3F));
		body.addOrReplaceChild("pinion_gear",
			CubeListBuilder.create().texOffs(44, 0).addBox(-1.5F, -1.5F, -0.5F, 3, 3, 1),
			PartPose.offset(2.2F, -6.5F, -3.4F));
		body.addOrReplaceChild("winding_key",
			CubeListBuilder.create()
				.texOffs(54, 0).addBox(-0.5F, -0.5F, 0.0F, 1, 1, 3)
				.texOffs(66, 0).addBox(-1.5F, -2.5F, 2.5F, 3, 5, 1),
			PartPose.offset(0.0F, -5.5F, 3.0F));
		body.addOrReplaceChild("kusazuri",
			CubeListBuilder.create().texOffs(78, 0).addBox(-4.5F, -1.0F, -3.5F, 9, 4, 7, new CubeDeformation(0.15F)),
			PartPose.ZERO);

		// Head: carved menpo face, dark-lacquer kabuto dome, crimson shikoro plates, golden kuwagata crest.
		PartDefinition head = body.addOrReplaceChild("head",
			CubeListBuilder.create()
				.texOffs(0, 20).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8)
				.texOffs(34, 20).addBox(-4.5F, -8.6F, -4.5F, 9, 4, 9, new CubeDeformation(0.10F))
				.texOffs(72, 20).addBox(-5.0F, -4.8F, -1.5F, 10, 4, 7, new CubeDeformation(0.05F))
				.texOffs(0, 40).addBox(-1.5F, -10.5F, -4.8F, 3, 2, 1)
				.texOffs(10, 40).addBox(-4.2F, -12.0F, -4.6F, 2, 4, 1)
				.texOffs(10, 40).mirror().addBox(2.2F, -12.0F, -4.6F, 2, 4, 1)
				.texOffs(18, 40).addBox(-5.2F, -14.2F, -4.6F, 2, 3, 1)
				.texOffs(18, 40).mirror().addBox(3.2F, -14.2F, -4.6F, 2, 3, 1),
			PartPose.offset(0.0F, -10.0F, 0.0F));

		// Right arm: layered sode pauldron, 2-segment jointed arm, vertical-held Naginata polearm.
		PartDefinition rightArm = body.addOrReplaceChild("right_arm",
			CubeListBuilder.create()
				.texOffs(0, 52).addBox(-3.0F, -2.0F, -2.5F, 4, 4, 5, new CubeDeformation(0.12F))
				.texOffs(20, 52).addBox(-2.0F, -1.0F, -2.0F, 4, 5, 4),
			PartPose.offset(-5.5F, -9.0F, 0.0F));
		rightArm.addOrReplaceChild("right_forearm",
			CubeListBuilder.create()
				.texOffs(38, 52).addBox(-2.0F, 0.0F, -2.0F, 4, 5, 4)
				.texOffs(56, 52).addBox(-1.5F, -10.0F, -1.0F, 1, 20, 1)
				.texOffs(62, 52).addBox(-2.0F, -12.0F, -1.5F, 2, 2, 2)
				.texOffs(72, 52).addBox(-1.5F, -18.0F, -2.0F, 1, 6, 3),
			PartPose.offset(0.0F, 4.0F, 0.0F));

		// Left arm: layered sode pauldron, 2-segment jointed arm, mounted paper-lantern buckler.
		PartDefinition leftArm = body.addOrReplaceChild("left_arm",
			CubeListBuilder.create()
				.texOffs(0, 52).mirror().addBox(-1.0F, -2.0F, -2.5F, 4, 4, 5, new CubeDeformation(0.12F))
				.texOffs(20, 52).mirror().addBox(-2.0F, -1.0F, -2.0F, 4, 5, 4),
			PartPose.offset(5.5F, -9.0F, 0.0F));
		leftArm.addOrReplaceChild("left_forearm",
			CubeListBuilder.create()
				.texOffs(38, 52).mirror().addBox(-2.0F, 0.0F, -2.0F, 4, 5, 4)
				.texOffs(82, 52).addBox(2.1F, -1.0F, -3.5F, 1, 7, 7),
			PartPose.offset(0.0F, 4.0F, 0.0F));

		// 2-segment armored legs with shin greaves (ground Y = 24.0).
		PartDefinition rightLeg = root.addOrReplaceChild("right_leg",
			CubeListBuilder.create().texOffs(0, 74).addBox(-2.0F, 0.0F, -2.0F, 4, 5, 4),
			PartPose.offset(-2.1F, 14.0F, 0.0F));
		rightLeg.addOrReplaceChild("right_greave",
			CubeListBuilder.create().texOffs(18, 74).addBox(-2.0F, 0.0F, -2.0F, 4, 5, 4, new CubeDeformation(0.06F)),
			PartPose.offset(0.0F, 5.0F, 0.0F));

		PartDefinition leftLeg = root.addOrReplaceChild("left_leg",
			CubeListBuilder.create().texOffs(0, 74).mirror().addBox(-2.0F, 0.0F, -2.0F, 4, 5, 4),
			PartPose.offset(2.1F, 14.0F, 0.0F));
		leftLeg.addOrReplaceChild("left_greave",
			CubeListBuilder.create().texOffs(18, 74).mirror().addBox(-2.0F, 0.0F, -2.0F, 4, 5, 4, new CubeDeformation(0.06F)),
			PartPose.offset(0.0F, 5.0F, 0.0F));

		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(MonsterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float ws = Math.min(1.0F, state.walkAnimationSpeed * 1.4F);
		float wp = state.walkAnimationPos * 0.7F;

		body.y = 14.0F;
		body.xRot = 0.0F;
		body.yRot = 0.0F;
		body.zRot = 0.0F;
		kusazuri.xRot = 0.0F;
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;

		// Ratchet clockwork walk stride with knee greave bend.
		float sR = Mth.sin(wp), sL = -sR;
		rightLeg.xRot = sR * 0.62F * ws;
		leftLeg.xRot = sL * 0.62F * ws;
		rightGreave.xRot = Math.max(0.0F, sR) * 0.58F * ws;
		leftGreave.xRot = Math.max(0.0F, sL) * 0.58F * ws;
		body.y += Math.abs(Mth.sin(wp)) * 0.35F * ws;
		kusazuri.xRot = -Math.max(Math.abs(rightLeg.xRot), Math.abs(leftLeg.xRot)) * 0.25F;

		// Interlocking front chest gears & spinning rear brass winding key.
		float gearRate = 0.07F + state.windup * 0.42F + state.acting * 0.28F;
		mainGear.zRot = t * gearRate;
		pinionGear.zRot = -t * gearRate * (5.0F / 3.0F);
		windingKey.zRot = -t * (0.10F + state.windup * 0.60F);

		if (state.windup > 0.05F) {
			// Gear-Spin Overhead Windup Telegraph matching Strip (b).
			float w = state.windup;
			body.yRot = -0.45F * w;
			body.xRot = -0.14F * w;
			rightArm.xRot = -2.25F * w;
			rightArm.zRot = 0.35F * w;
			rightForearm.xRot = -0.35F * w;
			leftArm.xRot = -0.75F * w;
			leftArm.zRot = -0.20F * w;
			leftForearm.xRot = -0.65F * w;
		} else if (state.acting > 0.05F) {
			// Wide Forward Naginata Slash matching Strip (c).
			float a = state.acting;
			body.yRot = 0.68F * a;
			body.xRot = 0.18F * a;
			rightArm.xRot = -1.25F + 2.15F * a;
			rightArm.yRot = -0.55F * a;
			rightArm.zRot = 0.20F;
			rightForearm.xRot = -0.25F;
			leftArm.xRot = -0.55F;
			leftArm.zRot = -0.25F * a;
			leftForearm.xRot = -0.45F;
		} else {
			// Clockwork guard march & ready stance holding Naginata vertically + buckler raised.
			rightArm.xRot = -0.12F - sR * 0.45F * ws;
			rightArm.yRot = 0.0F;
			rightArm.zRot = 0.06F;
			rightForearm.xRot = -0.22F;
			leftArm.xRot = -0.28F + sR * 0.40F * ws;
			leftArm.zRot = -0.06F;
			leftForearm.xRot = -0.45F;
		}
	}
}
