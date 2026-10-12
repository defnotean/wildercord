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
 * The Reefgrasp Octopus: a squat head with two bulging eyes on top, a great warty mantle sac swelling up and back behind it,
 * a siphon at its side, and eight long arms in a ring round its base, each a thick inner half and a thin curling tip.
 *
 * <p>Lying hidden ({@code veil}) its arms lie flat out on the seafloor and its mantle sags. Before a grab ({@code windup}) its
 * front arms rise; the grab ({@code acting}) shoots them out ahead. Holding on ({@code alt}) its front arms reach up and curl
 * round. Jetting off ({@code guard}) its arms bunch together beneath it and its mantle pumps.</p>
 *
 * <p>Laid out on a 64x64 skin (see {@code tools/octopus_art.py}):</p>
 * <pre>
 *   head (0,0) 7x5x7      eye (28,0) 2x2x2       siphon (28,6) 2x2x3
 *   arm (40,0) 2x6x2      tip (48,0) 1x6x1
 *   mantle (0,12) 8x8x9
 * </pre>
 */
public class ReefgraspOctopusModel extends EntityModel<MonsterRenderState> {
	private static final float HEAD_Y = 21.0F;
	private static final float ARM_Y = 21.5F;
	private static final float ARM_RADIUS = 2.5F;
	private static final int ARMS = 8;

	private final ModelPart head;
	private final ModelPart mantle;
	private final ModelPart[] arms = new ModelPart[ARMS];
	private final ModelPart[] tips = new ModelPart[ARMS];

	public ReefgraspOctopusModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.mantle = head.getChild("mantle");
		for (int i = 0; i < ARMS; i++) {
			arms[i] = root.getChild("arm" + i);
			tips[i] = arms[i].getChild("tip");
		}
	}

	private static float angle(int i) {
		return i * Mth.TWO_PI / ARMS + Mth.PI / ARMS;
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-3.5F, -4.0F, -3.5F, 7.0F, 5.0F, 7.0F)
				.texOffs(28, 0).addBox(-3.5F, -5.5F, -3.0F, 2.0F, 2.0F, 2.0F)
				.texOffs(28, 0).mirror().addBox(1.5F, -5.5F, -3.0F, 2.0F, 2.0F, 2.0F).mirror(false)
				.texOffs(28, 6).addBox(3.0F, -2.5F, -0.5F, 2.0F, 2.0F, 3.0F),
			PartPose.offset(0.0F, HEAD_Y, 0.0F));
		head.addOrReplaceChild("mantle", CubeListBuilder.create().texOffs(0, 12).addBox(-4.0F, -8.0F, -3.5F, 8.0F, 8.0F, 9.0F),
			PartPose.offsetAndRotation(0.0F, -3.0F, 1.0F, -0.45F, 0.0F, 0.0F));

		for (int i = 0; i < ARMS; i++) {
			float a = angle(i);
			PartDefinition arm = root.addOrReplaceChild("arm" + i, CubeListBuilder.create().texOffs(40, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F),
				PartPose.offsetAndRotation(Mth.sin(a) * ARM_RADIUS, ARM_Y, Mth.cos(a) * ARM_RADIUS, 1.3F, a, 0.0F));
			arm.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(48, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F),
				PartPose.offsetAndRotation(0.0F, 5.5F, 0.0F, 0.2F, 0.0F, 0.0F));
		}
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(MonsterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float swim = Math.min(1, state.walkAnimationSpeed * 2);
		float lurk = state.veil;
		float tell = state.windup;
		float grab = state.acting;
		float hold = state.alt;
		float jet = state.guard;

		// The head turns to look, settles hidden, bobs as it swims; the mantle breathes, sags hidden and pumps on the jet.
		head.yRot = Mth.clamp(state.yRot, -40, 40) * Mth.DEG_TO_RAD * (1 - lurk);
		head.y = HEAD_Y + 1.2F * lurk - 1.5F * jet + Mth.sin(t * 0.15F) * 0.3F * swim;
		float breath = Mth.sin(t * (0.08F + 0.5F * jet)) * (0.04F + 0.12F * jet);
		mantle.xRot = -0.45F - 0.5F * lurk + 0.25F * jet + 0.15F * tell;
		mantle.xScale = mantle.zScale = 1 + breath;
		mantle.yScale = 1 - breath - 0.15F * lurk;

		for (int i = 0; i < ARMS; i++) {
			float a = angle(i);
			// How much this arm faces forward (the model faces -z): the front pair reach first and furthest.
			float front = (1 - Mth.cos(a)) / 2;
			float reach = front * front;
			float ripple = Mth.sin(t * 0.09F + i * 1.3F) * (0.07F + 0.15F * swim);

			float rest = 1.3F + 0.25F * lurk + ripple * (1 - lurk * 0.7F);
			float x = rest;
			x += (2.3F - x) * tell * reach;
			x += (Mth.HALF_PI - x) * grab * reach;
			x += (1.9F - x) * hold * reach;
			x += (0.25F - x) * jet;
			ModelPart arm = arms[i];
			arm.xRot = x;
			arm.yRot = a;
			arm.y = ARM_Y + 1.2F * lurk - 1.5F * jet;
			arm.x = Mth.sin(a) * ARM_RADIUS * (1 - 0.5F * jet);
			arm.z = Mth.cos(a) * ARM_RADIUS * (1 - 0.5F * jet);
			arm.yScale = 1 + 0.5F * grab * reach + 0.2F * hold * reach;

			// The tips curl up a little at rest, ripple, curl right round to hold, and stream straight on the jet.
			float curl = 0.2F + ripple * 2 - 0.15F * lurk;
			curl += (1.3F - curl) * hold * reach;
			curl += (-0.1F - curl) * grab * reach;
			curl += (0.0F - curl) * jet;
			tips[i].xRot = curl;
		}
	}
}
