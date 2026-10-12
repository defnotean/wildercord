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
 * The Coffer Mimic: a chest's own base and lid, the size and shape of a vanilla chest, iron-bound oak with a lock. Shut, it
 * sits flat on the ground and there's nothing else to see: its teeth, eyes and tongue are all inside the lid. Awake, it
 * rises onto four stubby legs and its lid hangs open on rows of teeth, a dark red gullet, two yellow eyes at the back and a
 * long pink tongue lolling out over the rim. It snaps its lid to bite; to lash, its tongue shoots straight out; crouching
 * to leap, it squats and its lid chatters; clamped, it shuts tight; a missed leap leaves it on its back, legs kicking.
 *
 * <p>{@code veil} here is how shut it is (see {@code CofferMimic#shut}): 1 a coffer, 0 awake.</p>
 *
 * <p>Laid out on a 64x64 skin (see {@code tools/mimic_art.py}):</p>
 * <pre>
 *   lid (0,0) 14x5x14      base (0,19) 14x10x14    tooth (56,0) 1x2x1    fang (56,4) 1x3x1    eye (56,9) 2x2x1
 *   lock (56,14) 2x4x1     tongue (0,43) 3x1x4     leg (16,43) 3x4x3     tip (30,43) 3x1x3
 * </pre>
 */
public class CofferMimicModel extends EntityModel<MonsterRenderState> {
	/** The base teeth along its rim: x, z. */
	private static final float[][] TEETH = {
		{-5.5F, -6.3F}, {-3.5F, -6.3F}, {-1.5F, -6.3F}, {0.5F, -6.3F}, {2.5F, -6.3F}, {4.5F, -6.3F},
		{-6.3F, -4.0F}, {-6.3F, -1.5F}, {-6.3F, 1.0F}, {6.3F, -4.0F}, {6.3F, -1.5F}, {6.3F, 1.0F}};
	/** The lid teeth hanging from its underside: x, z (in the lid, its front at -14). */
	private static final float[][] LID_TEETH = {
		{-2.5F, -13.3F}, {-0.5F, -13.3F}, {1.5F, -13.3F}, {3.5F, -13.3F},
		{-6.3F, -11.0F}, {-6.3F, -8.5F}, {-6.3F, -6.0F}, {6.3F, -11.0F}, {6.3F, -8.5F}, {6.3F, -6.0F}};

	private final ModelPart body;
	private final ModelPart lid;
	private final ModelPart tongue;
	private final ModelPart tongueMid;
	private final ModelPart tongueTip;
	private final ModelPart[] legs = new ModelPart[4];
	private final ModelPart[] inside;

	public CofferMimicModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.lid = body.getChild("lid");
		this.tongue = body.getChild("tongue");
		this.tongueMid = tongue.getChild("mid");
		this.tongueTip = tongueMid.getChild("tip");
		for (int i = 0; i < 4; i++) {
			legs[i] = body.getChild("leg_" + i);
		}
		this.inside = new ModelPart[] {body.getChild("eyes"), body.getChild("teeth"), lid.getChild("teeth")};
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 19).addBox(-7.0F, -10.0F, -7.0F, 14.0F, 10.0F, 14.0F),
			PartPose.offset(0.0F, 24.0F, 0.0F));
		PartDefinition lid = body.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -5.0F, -14.0F, 14.0F, 5.0F, 14.0F)
				.texOffs(56, 14).addBox(-1.0F, -3.0F, -15.0F, 2.0F, 4.0F, 1.0F),
			PartPose.offset(0.0F, -10.0F, 7.0F));

		CubeListBuilder teeth = CubeListBuilder.create();
		for (float[] t : TEETH) {
			teeth.texOffs(56, 0).addBox(t[0] - 0.5F, -12.0F, t[1] - 0.5F, 1.0F, 2.0F, 1.0F);
		}
		body.addOrReplaceChild("teeth", teeth, PartPose.ZERO);
		CubeListBuilder lidTeeth = CubeListBuilder.create();
		for (float[] t : LID_TEETH) {
			lidTeeth.texOffs(56, 0).addBox(t[0] - 0.5F, 0.0F, t[1] - 0.5F, 1.0F, 2.0F, 1.0F);
		}
		// The two fangs at the lid's front corners.
		lidTeeth.texOffs(56, 4).addBox(-5.5F, 0.0F, -13.8F, 1.0F, 3.0F, 1.0F);
		lidTeeth.texOffs(56, 4).mirror().addBox(4.5F, 0.0F, -13.8F, 1.0F, 3.0F, 1.0F);
		lid.addOrReplaceChild("teeth", lidTeeth, PartPose.ZERO);

		body.addOrReplaceChild("eyes", CubeListBuilder.create().texOffs(56, 9).addBox(-4.5F, -12.0F, 3.5F, 2.0F, 2.0F, 1.0F)
			.texOffs(56, 9).mirror().addBox(2.5F, -12.0F, 3.5F, 2.0F, 2.0F, 1.0F), PartPose.ZERO);

		PartDefinition tongue = body.addOrReplaceChild("tongue", CubeListBuilder.create().texOffs(0, 43).addBox(-1.5F, -0.5F, -4.0F, 3.0F, 1.0F, 4.0F),
			PartPose.offset(0.0F, -10.5F, -2.5F));
		PartDefinition mid = tongue.addOrReplaceChild("mid", CubeListBuilder.create().texOffs(0, 43).addBox(-1.5F, -0.5F, -4.0F, 3.0F, 1.0F, 4.0F),
			PartPose.offset(0.0F, 0.0F, -4.0F));
		mid.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(30, 43).addBox(-1.5F, -0.5F, -3.0F, 3.0F, 1.0F, 3.0F),
			PartPose.offset(0.0F, 0.0F, -4.0F));

		for (int i = 0; i < 4; i++) {
			float x = i % 2 == 0 ? -5.0F : 5.0F;
			float z = i < 2 ? -5.0F : 5.0F;
			body.addOrReplaceChild("leg_" + i, CubeListBuilder.create().texOffs(16, 43).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F),
				PartPose.offset(x, 0.0F, z));
		}
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(MonsterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float shut = state.veil;
		float awake = 1 - shut;
		float flipped = state.stunned;
		float windup = state.windup;
		float walk = state.walkAnimationPos;
		float stride = Math.min(1, state.walkAnimationSpeed);

		// Awake it stands up on its legs; crouching to leap it squats down onto them.
		float lift = awake * (1 - 0.5F * windup);
		body.y = Mth.lerp(flipped, 24.0F - 4.0F * lift, 9.0F);
		body.zRot = Mth.PI * flipped + Mth.sin(walk * 1.3F) * 0.08F * stride * awake;
		body.yScale = 1 - 0.08F * windup * awake;
		body.xRot = -0.12F * windup * awake;

		// The lid: hanging open awake, breathing; flung wide to bite; wide to lash; chattering in the crouch; a hair's lift
		// as it twitches shut (the tell); shut tight clamped; hanging loose on its back.
		float open = awake * (0.42F + 0.06F * Mth.sin(t * 0.12F))
			+ state.acting * 1.1F
			+ state.alt * 0.7F
			+ windup * awake * 0.3F * Math.abs(Mth.sin(t * 1.4F))
			+ windup * shut * 0.14F;
		open *= 1 - 0.95F * state.guard;
		open = Math.max(open, 0.7F * flipped);
		lid.xRot = -Math.min(open, 1.9F);

		for (ModelPart part : inside) {
			part.visible = shut < 0.98F || open > 0.05F;
		}

		// The tongue: lolling out over the rim, swaying; shot out straight to lash.
		float lash = state.alt;
		tongue.visible = awake > 0.15F || flipped > 0.1F;
		tongue.zScale = 1 + 7.0F * lash;
		tongue.xRot = (1 - lash) * 0.1F - lash * state.xRot * Mth.DEG_TO_RAD;
		tongue.yRot = (1 - lash) * Mth.sin(t * 0.07F) * 0.15F;
		tongueMid.xRot = (1 - lash) * (0.9F + 0.1F * Mth.sin(t * 0.21F));
		tongueTip.xRot = (1 - lash) * (0.5F + 0.15F * Mth.sin(t * 0.21F + 1.0F));
		tongue.yScale = awake;

		// Legs: tucked away shut, scuttling, kicking on its back.
		for (int i = 0; i < 4; i++) {
			ModelPart leg = legs[i];
			float scale = Math.max(awake, flipped);
			leg.visible = scale > 0.05F;
			leg.yScale = scale;
			float phase = i == 0 || i == 3 ? 0 : Mth.PI;
			leg.xRot = Mth.cos(walk * 1.3F + phase) * 1.0F * stride * (1 - flipped) + Mth.sin(t * 0.9F + i * 1.7F) * 0.7F * flipped;
			leg.zRot = (i % 2 == 0 ? 1 : -1) * (0.15F * windup * awake + Mth.cos(t * 0.8F + i) * 0.3F * flipped);
		}
	}
}
