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
 * The Strawman: a burlap sack for a head with stitched eyes and a stitched grin, under a tattered pointed hat whose tip
 * droops; a patched coat with a ragged hem, sleeves stuffed with straw that sticks out of the cuffs, and two bare sticks for
 * legs with straw bound round the ankles.
 *
 * <p>Moving, it lurches stiff-legged with its arms flopping out to the sides (only ever seen by someone blinded, or out of
 * the corner of an eye). Caught, it is utterly still in one of its stances, snapped into at once, never eased: arms out
 * like any scarecrow; one arm reaching; both arms reaching; mid-stride and leaning; slumped with its head turned; one arm
 * raised high. Striking, its arm swings down; loosing its crows, its head is thrown back and its arms flung wide.</p>
 *
 * <p>{@code veil} here is whether it's held still (0 or 1, not eased), and {@code stance} which way.</p>
 *
 * <p>Laid out on a 64x64 skin (see {@code tools/strawman_art.py}):</p>
 * <pre>
 *   head (0,0) 8x8x8      crown (32,0) 6x4x6     tip (32,10) 3x3x3     brim (0,16) 12x1x12    torso (0,29) 8x12x4
 *   arm (24,29) 3x10x3    hand (36,29) 3x3x3     ankle (36,35) 3x2x3   leg (48,16) 2x12x2     hem (0,47) 9x4x5
 * </pre>
 */
public class StrawmanModel extends EntityModel<MonsterRenderState> {
	/**
	 * Its stances: right arm x and z, left arm x and z, head x, y and z, body x and z, right leg x, left leg x.
	 */
	private static final float[][] STANCES = {
		// Arms out, as any scarecrow, its head lolled to one side.
		{0.0F, 1.5F, 0.0F, -1.5F, 0.15F, 0.0F, 0.45F, 0.0F, 0.0F, 0.0F, 0.0F},
		// One arm reaching for you.
		{-1.4F, 0.2F, 0.1F, -0.15F, 0.35F, 0.2F, -0.25F, 0.15F, 0.0F, -0.35F, 0.3F},
		// Both arms reaching, its head cocked hard over.
		{-1.5F, 0.1F, -1.6F, -0.1F, 0.1F, 0.0F, 0.8F, 0.25F, 0.0F, 0.25F, -0.4F},
		// Mid-stride and leaning in, its head up, looking at you.
		{0.6F, 0.4F, 0.7F, -0.4F, -0.3F, 0.0F, 0.1F, 0.45F, 0.1F, -0.7F, 0.6F},
		// Slumped, its head turned round to you.
		{0.2F, 0.1F, 0.15F, -0.1F, 0.5F, 0.9F, 0.3F, 0.2F, -0.15F, 0.0F, 0.0F},
		// One arm raised high.
		{-2.8F, 0.2F, 0.0F, -1.2F, -0.4F, -0.3F, -0.5F, -0.1F, 0.12F, 0.2F, -0.1F},
	};

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart tip;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;

	public StrawmanModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.tip = head.getChild("tip");
		this.rightArm = body.getChild("right_arm");
		this.leftArm = body.getChild("left_arm");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 29).addBox(-4.0F, -12.0F, -2.0F, 8.0F, 12.0F, 4.0F)
				.texOffs(0, 47).addBox(-4.5F, -2.0F, -2.5F, 9.0F, 4.0F, 5.0F),
			PartPose.offset(0.0F, 12.0F, 0.0F));
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
				.texOffs(0, 16).addBox(-6.0F, -7.0F, -6.0F, 12.0F, 1.0F, 12.0F)
				.texOffs(32, 0).addBox(-3.0F, -11.0F, -3.0F, 6.0F, 4.0F, 6.0F),
			PartPose.offset(0.0F, -12.0F, 0.0F));
		head.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(32, 10).addBox(-1.5F, -3.0F, -1.5F, 3.0F, 3.0F, 3.0F),
			PartPose.offsetAndRotation(0.0F, -11.0F, 0.0F, 0.0F, 0.0F, 0.35F));

		body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(24, 29).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 10.0F, 3.0F)
				.texOffs(36, 29).addBox(-1.5F, 8.5F, -1.5F, 3.0F, 3.0F, 3.0F),
			PartPose.offset(-5.5F, -10.5F, 0.0F));
		body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(24, 29).mirror().addBox(-1.5F, -1.5F, -1.5F, 3.0F, 10.0F, 3.0F)
				.texOffs(36, 29).mirror().addBox(-1.5F, 8.5F, -1.5F, 3.0F, 3.0F, 3.0F),
			PartPose.offset(5.5F, -10.5F, 0.0F));

		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(48, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F)
				.texOffs(36, 35).addBox(-1.5F, 8.0F, -1.5F, 3.0F, 2.0F, 3.0F),
			PartPose.offset(-2.0F, 12.0F, 0.0F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(48, 16).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F)
				.texOffs(36, 35).mirror().addBox(-1.5F, 8.0F, -1.5F, 3.0F, 2.0F, 3.0F),
			PartPose.offset(2.0F, 12.0F, 0.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(MonsterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float held = state.veil;
		float moving = 1 - held;
		float walk = state.walkAnimationPos;
		float stride = Math.min(1, state.walkAnimationSpeed) * moving;
		float[] s = STANCES[Math.floorMod(state.stance, STANCES.length)];

		// Moving: a stiff lurch, its arms flopping out, its head lolling as it comes.
		float flop = Mth.sin(t * 0.35F) * 0.25F;
		rightArm.xRot = held * s[0] + moving * (Mth.cos(walk * 0.7F) * 0.5F * stride - 0.3F);
		rightArm.zRot = held * s[1] + moving * (1.2F + flop);
		leftArm.xRot = held * s[2] + moving * (-Mth.cos(walk * 0.7F) * 0.5F * stride - 0.3F);
		leftArm.zRot = held * s[3] + moving * (-1.2F + flop);
		head.xRot = held * s[4] + moving * (state.xRot * Mth.DEG_TO_RAD + 0.2F);
		head.yRot = held * s[5] + moving * state.yRot * Mth.DEG_TO_RAD;
		head.zRot = held * s[6] + moving * Mth.sin(t * 0.21F) * 0.35F;
		body.xRot = held * s[7] + moving * 0.2F;
		body.zRot = held * s[8] + moving * Mth.sin(walk * 0.7F) * 0.15F * stride;
		rightLeg.xRot = held * s[9] + moving * Mth.cos(walk * 0.7F) * 1.0F * stride;
		leftLeg.xRot = held * s[10] - moving * Mth.cos(walk * 0.7F) * 1.0F * stride;
		// Its hat's drooping tip, stirring a little in the wind only while it moves.
		tip.zRot = 0.35F + moving * Mth.sin(t * 0.17F) * 0.12F;

		// Its strike: the right arm swung down at you.
		float strike = state.acting;
		if (strike > 0) {
			rightArm.xRot = Mth.lerp(strike, rightArm.xRot, -2.4F + 1.6F * strike);
			rightArm.zRot = Mth.lerp(strike, rightArm.zRot, 0.3F);
			body.xRot += 0.25F * strike;
		}
		// Its crows: the head thrown back, the arms flung wide.
		float crows = state.alt;
		if (crows > 0) {
			head.xRot = Mth.lerp(crows, head.xRot, -0.9F);
			rightArm.zRot = Mth.lerp(crows, rightArm.zRot, 2.3F);
			leftArm.zRot = Mth.lerp(crows, leftArm.zRot, -2.3F);
			rightArm.xRot = Mth.lerp(crows, rightArm.xRot, 0.0F);
			leftArm.xRot = Mth.lerp(crows, leftArm.xRot, 0.0F);
		}
	}
}
