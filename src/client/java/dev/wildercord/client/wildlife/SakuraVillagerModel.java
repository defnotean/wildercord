package dev.wildercord.client.wildlife;

import dev.wildercord.wildlife.SakuraVillager;
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
 * Custom 128x128 Japanese NPC rig for {@code Tea Master} and {@code Shrine Maiden (Miko)} matching
 * their turnaround &amp; pose sheets: hip-hinged kimono torso, brocade obi &amp; rear bow, pleated
 * hakama, 2-segment draped kimono sleeves, conical woven amigasa hat / 3D sakura kanzashi with
 * tabane-noshi ribbon, wooden tea tray &amp; matcha cup / gohei blessing wand.
 */
public class SakuraVillagerModel extends EntityModel<SakuraVillagerRenderState> {
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart hairBun;
	private final ModelPart ponytail;
	private final ModelPart hat;
	private final ModelPart kanzashi;
	private final ModelPart sideLocks;
	private final ModelPart rightArm;
	private final ModelPart rightForearm;
	private final ModelPart gohei;
	private final ModelPart leftArm;
	private final ModelPart leftForearm;
	private final ModelPart teaTray;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;

	public SakuraVillagerModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = this.body.getChild("head");
		this.hairBun = this.head.getChild("hair_bun");
		this.ponytail = this.head.getChild("ponytail");
		this.hat = this.head.getChild("hat");
		this.kanzashi = this.head.getChild("kanzashi");
		this.sideLocks = this.head.getChild("side_locks");
		this.rightArm = this.body.getChild("right_arm");
		this.rightForearm = this.rightArm.getChild("right_forearm");
		this.gohei = this.rightForearm.getChild("gohei");
		this.leftArm = this.body.getChild("left_arm");
		this.leftForearm = this.leftArm.getChild("left_forearm");
		this.teaTray = this.leftForearm.getChild("tea_tray");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Waist-hinged upper body (hip pivot Y = 14.0 so ceremonial bows bend cleanly from the waist).
		PartDefinition body = root.addOrReplaceChild("body",
			CubeListBuilder.create().texOffs(0, 18).addBox(-4.0F, -12.0F, -2.0F, 8, 12, 4),
			PartPose.offset(0.0F, 14.0F, 0.0F));
		body.addOrReplaceChild("obi",
			CubeListBuilder.create().texOffs(26, 18).addBox(-4.0F, -5.0F, -2.0F, 8, 4, 4, new CubeDeformation(0.32F)),
			PartPose.ZERO);
		body.addOrReplaceChild("obi_knot",
			CubeListBuilder.create().texOffs(52, 18).addBox(-3.5F, -2.0F, 0.0F, 7, 4, 2),
			PartPose.offset(0.0F, -3.2F, 2.2F));
		body.addOrReplaceChild("hakama_waist",
			CubeListBuilder.create().texOffs(72, 18).addBox(-4.5F, 0.0F, -2.5F, 9, 4, 5, new CubeDeformation(0.10F)),
			PartPose.offset(0.0F, -1.0F, 0.0F));

		// Head & role-specific headwear/hair accessories.
		PartDefinition head = body.addOrReplaceChild("head",
			CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8),
			PartPose.offset(0.0F, -12.0F, 0.0F));
		head.addOrReplaceChild("hair_bun",
			CubeListBuilder.create().texOffs(34, 0).addBox(-2.0F, -6.0F, 3.6F, 4, 4, 2),
			PartPose.ZERO);
		head.addOrReplaceChild("ponytail",
			CubeListBuilder.create().texOffs(48, 0).addBox(-1.5F, -3.5F, 4.0F, 3, 5, 1),
			PartPose.ZERO);
		head.addOrReplaceChild("side_locks",
			CubeListBuilder.create()
				.texOffs(58, 0).addBox(-4.3F, -4.5F, -3.8F, 1, 6, 2)
				.texOffs(58, 0).mirror().addBox(3.3F, -4.5F, -3.8F, 1, 6, 2),
			PartPose.ZERO);
		head.addOrReplaceChild("hat",
			CubeListBuilder.create()
				.texOffs(0, 42).addBox(-7.0F, -8.8F, -7.0F, 14, 1, 14)
				.texOffs(58, 42).addBox(-5.0F, -10.3F, -5.0F, 10, 2, 10)
				.texOffs(98, 42).addBox(-2.5F, -11.8F, -2.5F, 5, 2, 5),
			PartPose.ZERO);

		// Shrine Maiden: white tabane-noshi hair band + single right-temple 3D sakura kanzashi flower.
		head.addOrReplaceChild("kanzashi",
			CubeListBuilder.create()
				.texOffs(0, 62).addBox(-4.0F, -6.5F, -4.0F, 8, 1, 8, new CubeDeformation(0.28F))
				.texOffs(34, 62).addBox(-5.2F, -7.4F, -2.5F, 2, 3, 3)
				.texOffs(46, 62).addBox(-5.0F, -4.5F, -1.5F, 1, 4, 2)
				.texOffs(54, 62).addBox(-2.5F, -6.4F, 4.1F, 5, 2, 1),
			PartPose.ZERO);

		// Articulated 2-segment arms with draped kimono sleeves + Tea Tray & Gohei props.
		PartDefinition rightArm = body.addOrReplaceChild("right_arm",
			CubeListBuilder.create().texOffs(0, 78).addBox(-3.0F, -1.0F, -2.0F, 4, 5, 4),
			PartPose.offset(-5.0F, -11.0F, 0.0F));
		PartDefinition rightFore = rightArm.addOrReplaceChild("right_forearm",
			CubeListBuilder.create()
				.texOffs(18, 78).addBox(-3.0F, 0.0F, -2.0F, 4, 5, 4)
				.texOffs(18, 92).addBox(-3.0F, -0.5F, 0.0F, 4, 7, 4, new CubeDeformation(0.12F)),
			PartPose.offset(0.0F, 4.0F, 0.0F));
		rightFore.addOrReplaceChild("gohei",
			CubeListBuilder.create()
				.texOffs(70, 78).addBox(-1.5F, 3.0F, -8.5F, 1, 1, 10)
				.texOffs(76, 92).addBox(-2.0F, 4.0F, -9.0F, 2, 5, 2)
				.texOffs(86, 92).addBox(-2.0F, 5.0F, -7.2F, 2, 4, 2),
			PartPose.ZERO);

		PartDefinition leftArm = body.addOrReplaceChild("left_arm",
			CubeListBuilder.create().texOffs(0, 78).mirror().addBox(-1.0F, -1.0F, -2.0F, 4, 5, 4),
			PartPose.offset(5.0F, -11.0F, 0.0F));
		PartDefinition leftFore = leftArm.addOrReplaceChild("left_forearm",
			CubeListBuilder.create()
				.texOffs(18, 78).mirror().addBox(-1.0F, 0.0F, -2.0F, 4, 5, 4)
				.texOffs(18, 92).mirror().addBox(-1.0F, -0.5F, 0.0F, 4, 7, 4, new CubeDeformation(0.12F)),
			PartPose.offset(0.0F, 4.0F, 0.0F));
		leftFore.addOrReplaceChild("tea_tray",
			CubeListBuilder.create()
				.texOffs(0, 108).addBox(-4.5F, -0.5F, -5.0F, 6, 1, 5)
				.texOffs(24, 108).addBox(-2.5F, -2.5F, -3.5F, 2, 2, 2),
			PartPose.offset(0.0F, 4.2F, -1.5F));

		// Pleated hakama trouser legs & white tabi / wooden geta sandals (ground Y = 24.0).
		root.addOrReplaceChild("right_leg",
			CubeListBuilder.create()
				.texOffs(38, 78).addBox(-2.0F, 0.0F, -2.5F, 4, 8, 5)
				.texOffs(38, 94).addBox(-2.0F, 8.0F, -2.5F, 4, 2, 5, new CubeDeformation(-0.08F)),
			PartPose.offset(-2.1F, 14.0F, 0.0F));
		root.addOrReplaceChild("left_leg",
			CubeListBuilder.create()
				.texOffs(38, 78).mirror().addBox(-2.0F, 0.0F, -2.5F, 4, 8, 5)
				.texOffs(38, 94).mirror().addBox(-2.0F, 8.0F, -2.5F, 4, 2, 5, new CubeDeformation(-0.08F)),
			PartPose.offset(2.1F, 14.0F, 0.0F));

		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(SakuraVillagerRenderState state) {
		super.setupAnim(state);
		boolean maiden = state.role == SakuraVillager.SHRINE_MAIDEN;
		hat.visible = !maiden;
		hairBun.visible = true;
		ponytail.visible = maiden;
		sideLocks.visible = maiden;
		kanzashi.visible = maiden;
		gohei.visible = maiden;
		teaTray.visible = !maiden;

		float t = state.ageInTicks;
		float breathe = Mth.sin(t * 0.08F);
		float bow = state.bowTick > 0 ? Mth.sin(Math.min(1.0F, state.bowTick / 26.0F) * Mth.PI) : 0.0F;

		// Hip-hinged bow tilts torso, head & sleeved arms together naturally.
		body.y = 14.0F + breathe * 0.06F + bow * 0.6F;
		body.xRot = bow * 0.56F;
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.65F + bow * 0.25F;

		float walk = Mth.cos(state.walkAnimationPos * 0.6662F) * 0.58F * state.walkAnimationSpeed;
		rightLeg.xRot = walk;
		leftLeg.xRot = -walk;

		// Tea Master cradles wooden tea tray with left forearm; Shrine Maiden holds gohei with gentle sway.
		if (maiden) {
			rightArm.xRot = -walk * 0.45F - 0.38F + bow * 0.22F + Mth.sin(t * 0.09F) * 0.05F;
			rightForearm.xRot = -0.35F + Mth.sin(t * 0.09F - 0.6F) * 0.07F;
			leftArm.xRot = walk * 0.45F - 0.14F + bow * 0.22F;
			leftForearm.xRot = -0.18F;
		} else {
			rightArm.xRot = -walk * 0.45F - 0.14F + bow * 0.22F;
			rightForearm.xRot = -0.22F;
			leftArm.xRot = -0.42F + bow * 0.18F + Mth.sin(t * 0.07F) * 0.04F;
			leftForearm.xRot = -0.52F - bow * 0.18F;
			teaTray.xRot = -(body.xRot + leftArm.xRot + leftForearm.xRot);
		}
	}
}
