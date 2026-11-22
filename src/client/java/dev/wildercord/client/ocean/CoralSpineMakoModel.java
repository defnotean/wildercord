package dev.wildercord.client.ocean;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * 1:1 360-degree high-voxel rig for Coral-Spine Mako (256x128 sheet, 0.5x sub-voxel scale):
 * 3-tier overhanging wedge shark snout, glowing golden eyes + dark brow ridges, crimson mouth lining
 * + 10 protruding 3D white teeth (5 upper + 5 lower), muscular cobalt-and-silver body segments,
 * 5-slice swept crescent dorsal & pectoral fins, 9-slice forked caudal tail, and 16+ staggered
 * rose-pink staghorn coral dorsal spines.
 */
public final class CoralSpineMakoModel extends EntityModel<OceanRenderStates.CoralMakoState> {
	private final ModelPart rootBody;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart midBody;
	private final ModelPart tailPeduncle;
	private final ModelPart caudalFin;
	private final ModelPart pecLeft;
	private final ModelPart pecRight;

	public CoralSpineMakoModel(ModelPart root) {
		super(root);
		this.rootBody = root.getChild("body");
		this.head = this.rootBody.getChild("head");
		this.jaw = this.head.getChild("jaw");
		this.midBody = this.rootBody.getChild("mid_body");
		this.tailPeduncle = this.midBody.getChild("tail_peduncle");
		this.caudalFin = this.tailPeduncle.getChild("caudal_fin");
		this.pecLeft = this.rootBody.getChild("pectoral_left");
		this.pecRight = this.rootBody.getChild("pectoral_right");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition body = root.addOrReplaceChild("body",
			CubeListBuilder.create()
				// Broad muscular anterior shark torso + shoulder taper
				.texOffs(0, 0).addBox(-8.0F, -7.0F, -12.0F, 16, 14, 20)
				.texOffs(72, 0).addBox(-7.0F, -8.5F, -10.0F, 14, 3, 18)
				// Stepped swept dorsal fin (5 cascading slices)
				.texOffs(0, 82).addBox(-1.5F, -13.0F, -4.0F, 3, 6, 10)
				.texOffs(26, 82).addBox(-1.0F, -18.0F, -1.0F, 2, 5, 8)
				.texOffs(46, 82).addBox(-1.0F, -22.0F, 2.0F, 2, 4, 6)
				.texOffs(62, 82).addBox(-0.5F, -25.0F, 4.5F, 1, 3, 4)
				// Jagged rose-pink coral spines cresting behind head & dorsal fin
				.texOffs(136, 0).addBox(-1.5F, -13.5F, -11.0F, 3, 5, 3)
				.texOffs(150, 0).addBox(-2.5F, -11.5F, -7.0F, 2, 4, 3)
				.texOffs(150, 0).mirror().addBox(0.5F, -12.0F, -6.5F, 2, 4, 3)
				.texOffs(162, 0).addBox(-1.5F, -14.0F, 5.5F, 3, 6, 3),
			PartPose.offset(0.0F, 11.0F, 0.0F));

		// 3-tier wedge snout + brow ridges + glowing amber eyes + 5 upper teeth + red mouth cavity
		PartDefinition head = body.addOrReplaceChild("head",
			CubeListBuilder.create()
				.texOffs(0, 34).addBox(-7.5F, -6.5F, -14.0F, 15, 11, 14)
				.texOffs(58, 34).addBox(-6.5F, -5.5F, -22.0F, 13, 7, 8)
				.texOffs(100, 34).addBox(-4.5F, -4.0F, -27.0F, 9, 5, 5)
				// Dark menacing brow plates + golden eyes
				.texOffs(136, 10).addBox(6.8F, -5.0F, -15.0F, 2, 2, 6)
				.texOffs(136, 10).mirror().addBox(-8.8F, -5.0F, -15.0F, 2, 2, 6)
				.texOffs(154, 10).addBox(7.0F, -3.0F, -14.0F, 1, 3, 4)
				.texOffs(154, 10).mirror().addBox(-8.0F, -3.0F, -14.0F, 1, 3, 4)
				// Upper crimson palate & 5 protruding white fangs
				.texOffs(136, 20).addBox(-5.0F, 1.5F, -22.0F, 10, 2, 10)
				.texOffs(176, 20).addBox(-4.5F, 2.5F, -23.0F, 1, 2, 1)
				.texOffs(176, 20).addBox(-2.0F, 2.5F, -24.0F, 1, 2, 1)
				.texOffs(176, 20).addBox(1.0F, 2.5F, -24.0F, 1, 2, 1)
				.texOffs(176, 20).addBox(3.5F, 2.5F, -23.0F, 1, 2, 1)
				.texOffs(176, 20).addBox(-0.5F, 2.5F, -24.5F, 1, 2, 1)
				// Coral snout spines
				.texOffs(136, 0).addBox(-1.5F, -11.5F, -12.0F, 3, 5, 3)
				.texOffs(150, 0).addBox(-1.0F, -9.0F, -17.5F, 2, 4, 3),
			PartPose.offset(0.0F, -0.5F, -12.0F));

		head.addOrReplaceChild("jaw",
			CubeListBuilder.create()
				.texOffs(58, 50).addBox(-5.5F, 0.0F, -14.0F, 11, 4, 14)
				.texOffs(186, 20).addBox(-4.5F, -0.5F, -13.0F, 9, 1, 10)
				// 5 lower teeth pointing up
				.texOffs(176, 20).addBox(-4.0F, -2.0F, -13.5F, 1, 2, 1)
				.texOffs(176, 20).addBox(-2.0F, -2.0F, -14.0F, 1, 2, 1)
				.texOffs(176, 20).addBox(0.0F, -2.0F, -14.2F, 1, 2, 1)
				.texOffs(176, 20).addBox(2.0F, -2.0F, -14.0F, 1, 2, 1)
				.texOffs(176, 20).addBox(3.5F, -2.0F, -13.0F, 1, 2, 1),
			PartPose.offset(0.0F, 3.0F, -8.0F));

		// Mid Body + Tail Coral Ridge + Pelvic fins
		PartDefinition mid = body.addOrReplaceChild("mid_body",
			CubeListBuilder.create()
				.texOffs(110, 50).addBox(-6.0F, -5.5F, 0.0F, 12, 11, 16)
				.texOffs(162, 0).addBox(-1.5F, -11.0F, 3.0F, 3, 6, 3)
				.texOffs(150, 0).addBox(-2.5F, -9.0F, 8.0F, 2, 4, 3)
				.texOffs(176, 0).mirror().addBox(0.5F, -10.5F, 9.0F, 2, 5, 3)
				.texOffs(80, 82).addBox(-1.0F, -9.0F, 12.0F, 2, 4, 4),
			PartPose.offset(0.0F, 0.0F, 8.0F));
		mid.addOrReplaceChild("pelvic_left",
			CubeListBuilder.create().texOffs(94, 82).addBox(0.0F, 0.0F, 0.0F, 7, 2, 6),
			PartPose.offsetAndRotation(5.0F, 3.5F, 4.0F, 0.0F, -0.35F, 0.3F));
		mid.addOrReplaceChild("pelvic_right",
			CubeListBuilder.create().texOffs(94, 82).mirror().addBox(-7.0F, 0.0F, 0.0F, 7, 2, 6),
			PartPose.offsetAndRotation(-5.0F, 3.5F, 4.0F, 0.0F, 0.35F, -0.3F));

		PartDefinition tailPed = mid.addOrReplaceChild("tail_peduncle",
			CubeListBuilder.create()
				.texOffs(168, 50).addBox(-4.0F, -4.0F, 0.0F, 8, 8, 12)
				.texOffs(188, 0).addBox(-1.0F, -8.5F, 2.0F, 2, 5, 3)
				.texOffs(150, 0).addBox(-1.0F, -7.5F, 7.0F, 2, 4, 3),
			PartPose.offset(0.0F, 0.0F, 15.5F));

		// Stepped crescent caudal shark tail (upper long lobe + lower lobe)
		tailPed.addOrReplaceChild("caudal_fin",
			CubeListBuilder.create()
				.texOffs(122, 82).addBox(-1.5F, -6.0F, 0.0F, 3, 12, 5)
				.texOffs(140, 82).addBox(-1.0F, -12.0F, 3.0F, 2, 8, 5)
				.texOffs(156, 82).addBox(-1.0F, -17.0F, 6.0F, 2, 6, 5)
				.texOffs(172, 82).addBox(-0.5F, -21.0F, 9.0F, 1, 5, 4)
				.texOffs(184, 82).addBox(-1.0F, 4.0F, 3.0F, 2, 7, 5)
				.texOffs(200, 82).addBox(-0.5F, 9.0F, 6.0F, 1, 5, 4),
			PartPose.offset(0.0F, 0.0F, 11.5F));

		// Swept 4-slice Pectoral Wings
		body.addOrReplaceChild("pectoral_left",
			CubeListBuilder.create()
				.texOffs(0, 102).addBox(0.0F, -1.0F, -5.0F, 8, 2, 10)
				.texOffs(36, 102).addBox(7.0F, -1.0F, -3.0F, 7, 2, 8)
				.texOffs(66, 102).addBox(13.0F, -0.5F, -1.0F, 6, 1, 6),
			PartPose.offsetAndRotation(7.5F, 4.5F, -4.0F, 0.0F, -0.25F, 0.24F));

		body.addOrReplaceChild("pectoral_right",
			CubeListBuilder.create()
				.texOffs(0, 102).mirror().addBox(-8.0F, -1.0F, -5.0F, 8, 2, 10)
				.texOffs(36, 102).mirror().addBox(-14.0F, -1.0F, -3.0F, 7, 2, 8)
				.texOffs(66, 102).mirror().addBox(-19.0F, -0.5F, -1.0F, 6, 1, 6),
			PartPose.offsetAndRotation(-7.5F, 4.5F, -4.0F, 0.0F, 0.25F, -0.24F));

		return LayerDefinition.create(mesh, 256, 256);
	}

	@Override
	public void setupAnim(OceanRenderStates.CoralMakoState state) {
		super.setupAnim(state);
		float swim = Mth.sin(state.ageInTicks * 0.26F + state.walkAnimationPos * 0.65F);
		float torpedo = state.lungeTicks > 0 ? 1.0F : 0.0F;
		rootBody.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F;
		midBody.yRot = swim * (0.18F + 0.14F * state.walkAnimationSpeed);
		tailPeduncle.yRot = -swim * (0.28F + 0.2F * state.walkAnimationSpeed);
		caudalFin.yRot = swim * 0.34F;
		jaw.xRot = torpedo * 0.62F + (0.1F + 0.08F * Mth.sin(state.ageInTicks * 0.14F));
		pecLeft.zRot = 0.24F + 0.08F * Mth.sin(state.ageInTicks * 0.2F);
		pecRight.zRot = -pecLeft.zRot;
	}
}
