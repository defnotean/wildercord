package dev.wildercord.client.wildlife;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A brookbill duck: a plump boat of a body on two short legs with broad webbed feet, a neck rising from the front to a
 * round head and a long flat bill, wings folded along its sides and a little tail turned up behind. On land it waddles,
 * rocking, head bobbing. Afloat ({@link WildlifeRenderState#swim}) it sits low, legs folded back and paddling. Dabbling
 * ({@link WildlifeRenderState#dive}) it up-ends, bill down and tail straight up, feet kicking at the air. Flapping
 * ({@link WildlifeRenderState#flapStrength}) it rises up tall and beats its wings. UVs match tools/duck_art.py:
 * <pre>
 *   body (0,0) 6x5x8     neck (28,0) 2x4x2     head (36,0) 4x4x5     bill (0,14) 3x1x3
 *   tail (16,14) 4x1x3   leg (32,14) 1x2x1     foot (40,14) 2x1x2    wing (0,19) 1x4x6
 * </pre>
 */
public final class BrookbillDuckModel extends EntityModel<WildlifeRenderState> {
	private static final float BODY_Y = 18.5F;

	private final ModelPart body, neck, head, tail, leftWing, rightWing, leftLeg, rightLeg, leftFoot, rightFoot;

	public BrookbillDuckModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		neck = body.getChild("neck");
		head = neck.getChild("head");
		tail = body.getChild("tail");
		leftWing = body.getChild("left_wing");
		rightWing = body.getChild("right_wing");
		leftLeg = body.getChild("left_leg");
		rightLeg = body.getChild("right_leg");
		leftFoot = leftLeg.getChild("foot");
		rightFoot = rightLeg.getChild("foot");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -2.5F, -4, 6, 5, 8),
			PartPose.offset(0, BODY_Y, 0));
		PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(28, 0).addBox(-1, -4, -1, 2, 4, 2),
			PartPose.offset(0, -1.5F, -3));
		PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(36, 0).addBox(-2, -4, -3, 4, 4, 5),
			PartPose.offset(0, -3.5F, 0));
		head.addOrReplaceChild("bill", CubeListBuilder.create().texOffs(0, 14).addBox(-1.5F, -0.5F, -3, 3, 1, 3),
			PartPose.offset(0, -1.2F, -3));
		body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(16, 14).addBox(-2, -0.5F, 0, 4, 1, 3),
			PartPose.offsetAndRotation(0, -1.5F, 4, -0.35F, 0, 0));
		for (int side = -1; side <= 1; side += 2) {
			String name = side > 0 ? "left" : "right";
			body.addOrReplaceChild(name + "_wing", CubeListBuilder.create().texOffs(0, 19).mirror(side < 0)
					.addBox(side > 0 ? 0 : -1, 0, 0, 1, 4, 6),
				PartPose.offset(side * 3, -2, -2.5F));
			PartDefinition leg = body.addOrReplaceChild(name + "_leg", CubeListBuilder.create().texOffs(32, 14).addBox(-0.5F, 0, -0.5F, 1, 2, 1),
				PartPose.offset(side * 1.5F, 2.5F, 0.5F));
			leg.addOrReplaceChild("foot", CubeListBuilder.create().texOffs(40, 14).mirror(side < 0).addBox(-1, 0, -2, 2, 1, 2),
				PartPose.offset(0, 2, 0.5F));
		}
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(WildlifeRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks + state.seed;
		float swim = state.swim;
		float dabble = state.dive;
		float flap = state.flapStrength * (1 - dabble);
		float land = 1 - swim;
		float speed = Math.min(1, state.walkAnimationSpeed * 2.5F);
		float step = state.walkAnimationPos * 1.5F;

		// The waddle: rocking over each foot in turn; afloat, a gentle bob on the water.
		float rock = Mth.sin(step) * 0.18F * speed * land;
		body.zRot = rock + Mth.sin(t * 0.09F) * 0.04F * swim;
		// Up-ending: tipped right over, bill down, tail to the sky. Flapping: reared up tall on its tail.
		body.xRot = 1.35F * dabble - 0.5F * flap;
		body.y = BODY_Y + 1.8F * swim + Mth.sin(t * 0.12F) * 0.25F * swim + Math.abs(Mth.sin(step)) * 0.3F * speed * land
			- 1.2F * dabble - 0.8F * flap;

		// The neck bobs forward at each step and straightens up afloat; dabbling it stretches down; flapping it rears up.
		neck.xRot = Mth.sin(step * 2) * 0.12F * speed * land + 0.3F * dabble + 0.4F * flap;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * (1 - dabble) - neck.xRot * 0.8F;
		head.yRot = state.yRot * Mth.DEG_TO_RAD * (1 - dabble);
		head.zRot = -rock * 0.6F;

		// Wings folded along its sides, beating out when it flaps, a little lift as it hurries.
		float beat = flap * (0.9F + Mth.sin(t * 1.6F) * 0.8F) + Math.max(0, speed - 0.7F) * 0.4F * land;
		leftWing.zRot = -beat;
		rightWing.zRot = beat;
		leftWing.xRot = rightWing.xRot = -0.15F * flap;

		// The tail flicks and waggles: always a little, a lot when it comes up from a dabble or shakes off.
		tail.yRot = Mth.sin(t * 0.6F) * (0.08F + 0.3F * flap + 0.25F * dabble);
		tail.xRot = -0.35F - 0.3F * dabble;

		// Legs: walking stride on land; folded back and paddling afloat; kicking at the air up-ended.
		float stride = Mth.sin(step) * 0.8F * speed * land;
		float paddle = Mth.sin(t * 0.4F) * 0.5F * swim * (0.4F + speed) + Mth.sin(t * 0.9F) * 0.6F * dabble;
		leftLeg.xRot = stride + 0.9F * swim + paddle;
		rightLeg.xRot = -stride + 0.9F * swim - paddle;
		leftFoot.xRot = rightFoot.xRot = -0.3F * swim * (1 - dabble);

		float scale = state.isBaby ? 1.5F : 1;
		head.xScale = head.yScale = head.zScale = scale;
		neck.yScale = state.isBaby ? 0.6F : 1;
	}
}
