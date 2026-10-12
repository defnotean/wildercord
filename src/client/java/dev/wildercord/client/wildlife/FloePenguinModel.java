package dev.wildercord.client.wildlife;

import dev.wildercord.wildlife.PenguinRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A floe penguin: an upright round-shouldered body on two flat feet, a round head with a stubby beak, two stiff flippers
 * and a little wedge of a tail. On land it waddles, rocking side to side, flippers held out a little for balance.
 * Tobogganing ({@link WildlifeRenderState#dive}) and swimming ({@link WildlifeRenderState#swim}) it lies flat on its belly,
 * head up and feet trailing, the flippers pushing or beating. In the huddle ({@link WildlifeRenderState#rest}) it hunches
 * down with its head sunk into its shoulders and its flippers pressed tight. A caught fish
 * ({@link WildlifeRenderState#held}) sits crosswise in its beak. UVs match tools/penguin_art.py:
 * <pre>
 *   body (0,0) 6x9x5     head (24,0) 5x4x4     beak (44,0) 2x1x3
 *   flipper (24,10) 1x6x3     foot (34,10) 2x1x3     tail (46,10) 3x1x2
 *   cod (0,16) 5x2x1     salmon (0,20) 5x2x1
 * </pre>
 */
public final class FloePenguinModel extends EntityModel<WildlifeRenderState> {
	private static final float BODY_Y = 23, HEAD_Y = -9;

	private final ModelPart body, head, cod, salmon, leftFlipper, rightFlipper, leftFoot, rightFoot, tail;

	public FloePenguinModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		head = body.getChild("head");
		cod = head.getChild("cod");
		salmon = head.getChild("salmon");
		leftFlipper = body.getChild("left_flipper");
		rightFlipper = body.getChild("right_flipper");
		leftFoot = body.getChild("left_foot");
		rightFoot = body.getChild("right_foot");
		tail = body.getChild("tail");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -9, -2.5F, 6, 9, 5),
			PartPose.offset(0, BODY_Y, 0));
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(24, 0).addBox(-2.5F, -4, -2, 5, 4, 4),
			PartPose.offset(0, HEAD_Y, -0.3F));
		head.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(44, 0).addBox(-1, -0.5F, -3, 2, 1, 3),
			PartPose.offset(0, -1.5F, -1.2F));
		head.addOrReplaceChild("cod", CubeListBuilder.create().texOffs(0, 16).addBox(-2.5F, -1, -0.5F, 5, 2, 1),
			PartPose.offset(0, -1.2F, -3.8F));
		head.addOrReplaceChild("salmon", CubeListBuilder.create().texOffs(0, 20).addBox(-2.5F, -1, -0.5F, 5, 2, 1),
			PartPose.offset(0, -1.2F, -3.8F));
		body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(46, 10).addBox(-1.5F, -0.5F, 0, 3, 1, 2),
			PartPose.offsetAndRotation(0, -0.8F, 2.2F, 0.35F, 0, 0));
		for (int side = -1; side <= 1; side += 2) {
			String name = side > 0 ? "left" : "right";
			body.addOrReplaceChild(name + "_flipper", CubeListBuilder.create().texOffs(24, 10).mirror(side < 0)
					.addBox(side > 0 ? 0 : -1, 0, -1.5F, 1, 6, 3),
				PartPose.offset(side * 3, -8, 0));
			body.addOrReplaceChild(name + "_foot", CubeListBuilder.create().texOffs(34, 10).mirror(side < 0).addBox(-1, 0, -2.5F, 2, 1, 3),
				PartPose.offset(side * 1.5F, 0, 0));
		}
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(WildlifeRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks + state.seed;
		float speed = Math.min(1, state.walkAnimationSpeed * 2.5F);
		float step = state.walkAnimationPos * 1.6F;
		float slide = state.dive;
		float swim = state.swim;
		float lie = Math.max(slide, swim);
		float stand = 1 - lie;
		float huddle = state.rest * stand;

		// The waddle: rocking side to side over each foot in turn, the feet slapping forward short and flat.
		float rock = Mth.sin(step) * 0.2F * speed * stand;
		body.zRot = rock + Mth.sin(t * 0.05F) * 0.03F * huddle;
		body.yRot = 0;
		// Lying down to slide or swim: tipped forward onto its belly, lifted so the belly rests on the ground.
		body.xRot = Mth.HALF_PI * lie;
		body.y = BODY_Y - 2.5F * lie + Math.abs(Mth.sin(step)) * 0.3F * speed * stand + huddle * 0.6F
			+ Mth.sin(t * 0.15F) * 0.4F * swim;
		float breathe = 1 + Mth.sin(t * 0.08F) * 0.02F * huddle;
		body.xScale = body.zScale = 1 + 0.05F * huddle;
		body.yScale = breathe - 0.06F * huddle;

		leftFoot.xRot = Mth.sin(step) * 0.7F * speed * stand;
		rightFoot.xRot = -Mth.sin(step) * 0.7F * speed * stand;
		// Sliding, the feet push off behind it in turn; swimming they trail and steer.
		float kick = Mth.sin(t * 0.7F) * 0.5F * slide;
		leftFoot.xRot += 0.4F * lie + kick;
		rightFoot.xRot += 0.4F * lie - kick;
		leftFoot.y = rightFoot.y = 0;

		// Flippers: held out a little for balance as it waddles, pressed tight in the huddle, beating in the water.
		float out = 0.12F + 0.25F * speed * stand + Math.max(0, Mth.sin(t * 0.04F) * 8 - 7) * Mth.sin(t * 0.9F) * 0.4F * stand;
		out *= 1 - huddle;
		float beat = (0.5F + Mth.sin(t * 0.55F) * 0.6F) * swim + (0.35F + Mth.sin(t * 0.35F) * 0.1F) * slide * (1 - swim);
		leftFlipper.zRot = -(out + beat) - rock * 0.5F;
		rightFlipper.zRot = out + beat - rock * 0.5F;
		leftFlipper.xRot = rightFlipper.xRot = 0.1F * stand;

		// The head looks about upright; lying down it is held up and forward, looking where it goes.
		head.xRot = state.xRot * Mth.DEG_TO_RAD * stand * (1 - huddle) - Mth.HALF_PI * lie + 0.25F * lie + 0.5F * huddle;
		head.yRot = state.yRot * Mth.DEG_TO_RAD * (1 - 0.6F * lie) * (1 - huddle);
		head.zRot = -rock * 0.5F;
		// Huddled, it sinks its head down into its shoulders.
		head.y = HEAD_Y + 1.6F * huddle;
		tail.xRot = 0.35F + 0.25F * lie + Mth.sin(t * 0.3F) * 0.08F * speed;

		cod.visible = state.held == PenguinRules.COD;
		salmon.visible = state.held == PenguinRules.SALMON;

		float scale = state.isBaby ? 1.35F : 1;
		head.xScale = head.yScale = head.zScale = scale;
	}
}
