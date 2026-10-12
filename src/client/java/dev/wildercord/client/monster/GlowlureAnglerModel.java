package dev.wildercord.client.monster;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The Glowlure Angler: a broad flat salamander body on four stubby splayed legs, a thick tapering tail, a wide flat skull
 * over a hinged jaw lined with teeth, and from its brow a stalk that bends forward to hang a glowing bulb before its mouth.
 *
 * <p>Lying in wait ({@code guard}) it sinks flat to the ground, legs splayed, jaw just ajar, and only the lure moves,
 * swaying slow. The tell ({@code windup}) is its jaw dropping wide and its head rearing back. Lunging ({@code acting}) it
 * flings itself forward, jaw gaping, legs swept back. Winded ({@code stunned}) it sprawls, jaw hanging, panting.</p>
 *
 * <p>Laid out on a 64x64 skin (see {@code tools/angler_art.py}):</p>
 * <pre>
 *   body (0,0) 8x4x10     stalk (40,0) 1x5x1     reach (46,0) 1x1x4     bulb (40,8) 3x3x3
 *   skull (0,14) 9x4x6    jaw (30,14) 9x2x6      teeth (0,26) 8x1x1     leg (0,30) 2x3x2
 *   tail (0,36) 4x3x10    tail tip (30,36) 2x2x6
 * </pre>
 */
public class GlowlureAnglerModel extends EntityModel<MonsterRenderState> {
	private static final float BODY_Y = 20.0F;

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart stalk;
	private final ModelPart reach;
	private final ModelPart bulb;
	private final ModelPart tail;
	private final ModelPart tailTip;
	private final ModelPart[] legs = new ModelPart[4];

	public GlowlureAnglerModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.jaw = head.getChild("jaw");
		this.stalk = head.getChild("stalk");
		this.reach = stalk.getChild("reach");
		this.bulb = reach.getChild("bulb");
		this.tail = body.getChild("tail");
		this.tailTip = tail.getChild("tail_tip");
		for (int i = 0; i < 4; i++) {
			legs[i] = body.getChild("leg_" + i);
		}
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -2.0F, -5.0F, 8.0F, 4.0F, 10.0F),
			PartPose.offset(0.0F, BODY_Y, 0.0F));

		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 14).addBox(-4.5F, -3.0F, -6.0F, 9.0F, 4.0F, 6.0F)
				.texOffs(0, 26).addBox(-4.0F, 1.0F, -5.7F, 8.0F, 1.0F, 1.0F),
			PartPose.offset(0.0F, -0.5F, -5.0F));
		head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(30, 14).addBox(-4.5F, 0.0F, -6.0F, 9.0F, 2.0F, 6.0F),
			PartPose.offset(0.0F, 1.0F, 0.0F));
		PartDefinition stalk = head.addOrReplaceChild("stalk", CubeListBuilder.create().texOffs(40, 0).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 5.0F, 1.0F),
			PartPose.offset(0.0F, -3.0F, -3.0F));
		PartDefinition reach = stalk.addOrReplaceChild("reach", CubeListBuilder.create().texOffs(46, 0).addBox(-0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 4.0F),
			PartPose.offset(0.0F, -5.0F, 0.0F));
		reach.addOrReplaceChild("bulb", CubeListBuilder.create().texOffs(40, 8).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F),
			PartPose.offset(0.0F, 0.0F, -4.0F));

		PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 36).addBox(-2.0F, -1.5F, 0.0F, 4.0F, 3.0F, 10.0F),
			PartPose.offset(0.0F, -0.5F, 5.0F));
		tail.addOrReplaceChild("tail_tip", CubeListBuilder.create().texOffs(30, 36).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 6.0F),
			PartPose.offset(0.0F, 0.3F, 10.0F));

		for (int i = 0; i < 4; i++) {
			int side = i % 2 == 0 ? -1 : 1;
			float z = i < 2 ? -3.5F : 3.5F;
			body.addOrReplaceChild("leg_" + i, CubeListBuilder.create().texOffs(0, 30).mirror(side > 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
				PartPose.offset(side * 4.0F, 1.0F, z));
		}
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(MonsterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float lurk = state.guard;
		float tell = state.windup;
		float lunge = state.acting;
		float winded = state.stunned;
		float flat = Math.max(lurk, winded);
		float walk = state.walkAnimationPos * 1.2F;
		float stride = Math.min(1, state.walkAnimationSpeed * 1.8F) * (1 - flat);

		// Flat to the ground waiting or winded; crouched back for the tell; stretched long in the lunge.
		body.y = BODY_Y + 1.6F * flat + 0.6F * tell - 0.8F * lunge + Mth.sin(t * 0.5F) * 0.25F * winded;
		body.z = 1.0F * tell - 1.0F * lunge;
		body.xRot = -0.12F * tell + 0.1F * lunge;
		// A salamander's walk: the body swings side to side between its steps.
		body.yRot = Mth.sin(walk) * 0.18F * stride;
		body.zRot = 0;

		float settled = (1 - flat) * (1 - tell) * (1 - lunge);
		head.yRot = Mth.clamp(state.yRot, -30, 30) * Mth.DEG_TO_RAD * 0.6F * settled - body.yRot * 0.6F;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F * settled - 0.35F * tell - 0.15F * lunge + 0.18F * winded;
		// The jaw: just ajar waiting, dropping wide for the tell and the lunge, hanging open winded, working as it walks.
		float pant = Math.max(0, Mth.sin(t * 0.45F)) * 0.15F * winded;
		jaw.xRot = 0.08F * lurk + 0.95F * tell + 1.05F * lunge + 0.45F * winded + pant + Math.max(0, Mth.sin(walk * 0.5F)) * 0.1F * stride;

		// The lure: swaying slowly in the wait, flicking in the tell, swept back in the lunge, drooping when winded.
		float sway = Mth.sin(t * 0.07F);
		stalk.xRot = -0.25F + Mth.sin(t * 0.05F) * 0.08F * lurk + 0.5F * lunge + 0.45F * winded;
		stalk.zRot = sway * 0.12F * lurk + Mth.sin(t * 1.6F) * 0.2F * tell;
		reach.xRot = 0.15F + Mth.sin(t * 0.09F + 1) * 0.12F * lurk + 0.4F * winded;
		bulb.xRot = -stalk.xRot - reach.xRot;
		bulb.zRot = -stalk.zRot;

		// The tail: swinging against the body as it walks, lying still and curled a little to one side in the wait.
		tail.yRot = -Mth.sin(walk) * 0.35F * stride + 0.25F * lurk + Mth.sin(t * 0.04F) * 0.04F * lurk;
		tail.xRot = -0.08F + 0.1F * flat - 0.15F * lunge;
		tailTip.yRot = -Mth.sin(walk - 0.8F) * 0.4F * stride + 0.2F * lurk;
		tailTip.xRot = 0.08F * flat;

		for (int i = 0; i < 4; i++) {
			int side = i % 2 == 0 ? -1 : 1;
			boolean front = i < 2;
			// Diagonal pairs step together.
			float phase = walk + ((i == 0 || i == 3) ? 0 : Mth.PI);
			float step = Mth.sin(phase) * 0.6F * stride;
			legs[i].xRot = step + (front ? -1.1F : 1.1F) * lunge;
			// Splayed flat to the sides lying down, as a salamander rests.
			legs[i].zRot = side * (0.15F + 1.1F * flat + 0.25F * tell) - side * Math.max(0, Mth.cos(phase)) * 0.25F * stride;
			legs[i].yRot = 0;
		}
	}
}
