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
 * The Thunderwing Harpy: a lean, upright raptor of a body under a ruff of storm-grey feathers, a beaked head with a crest
 * swept back like a blade, great wings in three layers (coverts, flight feathers and long primaries), scaled legs ending in
 * hooked talons, and a fanned tail. It flies pitched forward, wings beating in deep, slow strokes; shrieking, it hangs
 * upright with its wings flung wide and its head thrown back; diving, it folds its wings back tight and drives in head
 * first, talons out; calling lightning, it raises both wings straight up; grounded, it slumps forward, wings spread limp.
 *
 * <p>Laid out on a 128x64 skin (see {@code thunderwing_harpy} in {@code tools/monster_art.py}):</p>
 * <pre>
 *   body (0,0) 6x9x4      head (20,0) 6x6x6     beak (44,0) 2x3x3     crest (54,0) 0x7x8    ruff (70,0) 7x3x5
 *   thigh (94,0) 3x4x3    shin (106,0) 2x4x2    foot (114,0) 3x1x4    talon (94,8) 1x2x1
 *   bone (0,16) 10x2x3    forebone (26,16) 10x2x2
 *   coverts (0,22) 10x0x9 flight (0,32) 12x0x11 primaries (0,44) 8x0x14  tail (48,22) 9x0x11
 * </pre>
 */
public class ThunderwingHarpyModel extends EntityModel<MonsterRenderState> {
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart beak;
	private final ModelPart crest;
	private final ModelPart rightWing;
	private final ModelPart leftWing;
	private final ModelPart rightOuter;
	private final ModelPart leftOuter;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;
	private final ModelPart rightShin;
	private final ModelPart leftShin;
	private final ModelPart tail;

	public ThunderwingHarpyModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.beak = head.getChild("beak");
		this.crest = head.getChild("crest");
		this.rightWing = body.getChild("right_wing");
		this.leftWing = body.getChild("left_wing");
		this.rightOuter = rightWing.getChild("outer");
		this.leftOuter = leftWing.getChild("outer");
		this.rightLeg = body.getChild("right_leg");
		this.leftLeg = body.getChild("left_leg");
		this.rightShin = rightLeg.getChild("shin");
		this.leftShin = leftLeg.getChild("shin");
		this.tail = body.getChild("tail");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -1.0F, -2.0F, 6.0F, 9.0F, 4.0F)
				.texOffs(70, 0).addBox(-3.5F, -1.5F, -2.5F, 7.0F, 3.0F, 5.0F),
			PartPose.offset(0.0F, 9.5F, 0.0F));
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(20, 0).addBox(-3.0F, -6.0F, -3.5F, 6.0F, 6.0F, 6.0F),
			PartPose.offset(0.0F, -1.0F, -0.5F));
		head.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(44, 0).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 3.0F, 3.0F),
			PartPose.offsetAndRotation(0.0F, -2.4F, -3.5F, 0.35F, 0.0F, 0.0F));
		head.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(54, 0).addBox(0.0F, -7.0F, 0.0F, 0.0F, 7.0F, 8.0F),
			PartPose.offsetAndRotation(0.0F, -5.5F, -2.0F, -0.95F, 0.0F, 0.0F));

		wing(body, "right_wing", -3.0F, false);
		wing(body, "left_wing", 3.0F, true);
		leg(body, "right_leg", -1.5F, false);
		leg(body, "left_leg", 1.5F, true);

		body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(48, 22).addBox(-4.5F, 0.0F, 0.0F, 9.0F, 0.0F, 11.0F),
			PartPose.offsetAndRotation(0.0F, 7.5F, 1.5F, 0.35F, 0.0F, 0.0F));
		return LayerDefinition.create(mesh, 128, 64);
	}

	/** A wing: a bone and its coverts from the shoulder, then the forearm with the flight feathers and long primaries. */
	private static void wing(PartDefinition body, String name, float x, boolean left) {
		float s = left ? 1 : -1;
		PartDefinition wing = body.addOrReplaceChild(name, mirror(CubeListBuilder.create().texOffs(0, 16), left)
				.addBox(left ? 0.0F : -10.0F, -1.0F, -1.5F, 10.0F, 2.0F, 3.0F)
				.texOffs(0, 22).addBox(left ? 0.0F : -10.0F, 0.0F, -1.0F, 10.0F, 0.0F, 9.0F),
			PartPose.offset(x, 0.5F, 0.5F));
		PartDefinition outer = wing.addOrReplaceChild("outer", mirror(CubeListBuilder.create().texOffs(26, 16), left)
				.addBox(left ? 0.0F : -10.0F, -1.0F, -1.0F, 10.0F, 2.0F, 2.0F)
				.texOffs(0, 32).addBox(left ? -1.0F : -11.0F, 0.0F, -1.0F, 12.0F, 0.0F, 11.0F),
			PartPose.offset(s * 10.0F, 0.0F, 0.0F));
		outer.addOrReplaceChild("primaries", mirror(CubeListBuilder.create().texOffs(0, 44), left)
				.addBox(left ? 0.0F : -8.0F, 0.0F, -1.0F, 8.0F, 0.0F, 14.0F),
			PartPose.offsetAndRotation(s * 8.0F, -0.1F, 0.0F, 0.0F, -s * 0.35F, 0.0F));
	}

	/** A scaled leg: thigh, shin, and a foot of hooked talons. */
	private static void leg(PartDefinition body, String name, float x, boolean left) {
		PartDefinition leg = body.addOrReplaceChild(name, mirror(CubeListBuilder.create().texOffs(94, 0), left).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F),
			PartPose.offset(x, 7.5F, 0.5F));
		PartDefinition shin = leg.addOrReplaceChild("shin", mirror(CubeListBuilder.create().texOffs(106, 0), left).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F),
			PartPose.offset(0.0F, 3.5F, 0.0F));
		PartDefinition foot = shin.addOrReplaceChild("foot", mirror(CubeListBuilder.create().texOffs(114, 0), left).addBox(-1.5F, 0.0F, -3.0F, 3.0F, 1.0F, 4.0F),
			PartPose.offset(0.0F, 3.5F, 0.0F));
		for (int i = 0; i < 3; i++) {
			foot.addOrReplaceChild("talon_" + i, CubeListBuilder.create().texOffs(94, 8).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F),
				PartPose.offsetAndRotation(-1.0F + i, 0.5F, -3.0F, -0.6F, 0.0F, 0.0F));
		}
	}

	private static CubeListBuilder mirror(CubeListBuilder builder, boolean mirror) {
		return mirror ? builder.mirror() : builder;
	}

	@Override
	public void setupAnim(MonsterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float shriek = state.windup;
		float dive = state.acting;
		float call = state.alt;
		float down = state.stunned;
		float air = 1 - down;

		// Flying: pitched forward, wings beating in deep, slow strokes (the outer half a beat behind).
		float f = t * 0.32F;
		float beat = Mth.sin(f);
		float wing = 0.1F + beat * 0.8F;
		float outer = Mth.sin(f - 0.7F) * 0.4F;
		float pitch = 0.75F + Mth.sin(f + 0.5F) * 0.05F;
		body.xRot = pitch;
		body.y = 9.5F - beat * 0.6F;
		float legs = 1.15F;
		float sweep = 0.1F;

		// Shrieking: hanging upright, wings flung wide and trembling, head thrown back, beak agape, crest up.
		float tremble = Mth.sin(t * 2.1F) * 0.06F;
		wing = Mth.lerp(shriek, wing, 0.55F + tremble);
		outer = Mth.lerp(shriek, outer, -0.15F);
		body.xRot = Mth.lerp(shriek, body.xRot, 0.15F);
		// Calling lightning: both wings straight up, upright.
		wing = Mth.lerp(call, wing, 1.3F + tremble);
		outer = Mth.lerp(call, outer, 0.35F);
		body.xRot = Mth.lerp(call, body.xRot, 0.05F);
		// Diving: wings folded back tight, head first, talons out.
		wing = Mth.lerp(dive, wing, -0.2F);
		sweep = Mth.lerp(dive, sweep, 1.2F);
		outer = Mth.lerp(dive, outer, 0.5F);
		body.xRot = Mth.lerp(dive, body.xRot, 1.35F);
		legs = Mth.lerp(dive, legs, -1.0F);
		legs = Mth.lerp(shriek + call, legs, 0.35F);
		// Grounded: slumped forward on the ground, wings spread limp, head hanging.
		wing = Mth.lerp(down, wing, -0.3F);
		sweep = Mth.lerp(down, sweep, 0.35F);
		outer = Mth.lerp(down, outer, -0.25F);
		body.xRot = Mth.lerp(down, body.xRot, 1.15F);
		body.y = Mth.lerp(down, body.y, 15.5F);
		legs = Mth.lerp(down, legs, -0.4F);

		rightWing.zRot = wing;
		leftWing.zRot = -wing;
		rightWing.yRot = sweep;
		leftWing.yRot = -sweep;
		rightOuter.zRot = outer;
		leftOuter.zRot = -outer;
		rightOuter.yRot = sweep * 0.6F;
		leftOuter.yRot = -sweep * 0.6F;

		rightLeg.xRot = legs;
		leftLeg.xRot = legs;
		rightShin.xRot = 0.3F * air;
		leftShin.xRot = 0.3F * air;
		rightLeg.zRot = 0.35F * down;
		leftLeg.zRot = -0.35F * down;

		// The head looks where it hunts, held level against the body's pitch; thrown back in a shriek.
		head.yRot = state.yRot * Mth.DEG_TO_RAD * air;
		head.xRot = -body.xRot * 0.85F + Mth.clamp(state.xRot, -40, 40) * Mth.DEG_TO_RAD * air - 0.7F * shriek - 0.4F * call + 0.6F * down;
		beak.xRot = 0.35F + 0.55F * shriek + 0.3F * call;
		crest.xRot = -0.95F + 0.6F * shriek + 0.4F * call + Mth.sin(t * 0.2F) * 0.04F;
		tail.xRot = 0.35F - 0.3F * dive + 0.2F * shriek + Mth.sin(f + 1.0F) * 0.06F * air;
	}
}
