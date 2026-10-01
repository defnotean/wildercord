package dev.wildercord.client.wildlife;

import com.mojang.blaze3d.vertex.PoseStack;
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
 * A mossback tortoise: a high domed shell built up in tiers (rim, dome, top and crown) over a flat plastron, with
 * strands of moss hanging from its rim, four thick pillar legs, a heavy head on a short neck, and a stub of a tail.
 * It plods (legs in diagonal pairs, its shell rocking side to side, its head nodding with each step), looks about
 * slowly, and draws everything in and settles on the ground when struck. Its garden is drawn on the crown by
 * {@link MossbackTortoiseRenderer}. A baby has a bigger head and a bare shell. UVs match {@code tortoise_skin} in
 * tools/wildlife_art.py.
 */
public final class MossbackTortoiseModel extends EntityModel<WildlifeRenderState> {
	/** Flat sheets (wings, moss) are two faces a hair apart, so neither flickers through the other. */
	private static final CubeDeformation SHEET = new CubeDeformation(0.001F);

	private final ModelPart shell, head, tail, frontLeft, frontRight, hindLeft, hindRight;
	private final ModelPart[] moss;

	/** The top of the crown, in the shell's own pixels: where the garden grows. */
	public static final float CROWN = -6;

	public MossbackTortoiseModel(ModelPart root) {
		super(root);
		shell = root.getChild("shell");
		head = root.getChild("head");
		tail = root.getChild("tail");
		frontLeft = root.getChild("front_left_leg");
		frontRight = root.getChild("front_right_leg");
		hindLeft = root.getChild("hind_left_leg");
		hindRight = root.getChild("hind_right_leg");
		moss = new ModelPart[] {shell.getChild("moss_left"), shell.getChild("moss_right"), shell.getChild("moss_front"), shell.getChild("moss_back")};
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition shell = root.addOrReplaceChild("shell", CubeListBuilder.create()
			.texOffs(0, 0).addBox(-10, 0, -12, 20, 3, 24)
			.texOffs(0, 27).addBox(-9, -3, -11, 18, 3, 22)
			.texOffs(0, 52).addBox(-7, -5, -8.5F, 14, 2, 17)
			.texOffs(62, 52).addBox(-4.5F, CROWN, -5, 9, 1, 10)
			.texOffs(0, 71).addBox(-8.5F, 3, -10.5F, 17, 2, 21), PartPose.offset(0, 13, 0));
		// Moss hanging from the rim: flat curtains, cut out into strands by the texture.
		shell.addOrReplaceChild("moss_left", CubeListBuilder.create().texOffs(88, 0).addBox(10.02F, 1.5F, -10, 0, 5, 20, SHEET), PartPose.ZERO);
		shell.addOrReplaceChild("moss_right", CubeListBuilder.create().texOffs(88, 0).mirror().addBox(-10.02F, 1.5F, -10, 0, 5, 20, SHEET), PartPose.ZERO);
		shell.addOrReplaceChild("moss_front", CubeListBuilder.create().texOffs(88, 25).addBox(-9, 1.5F, -12.02F, 18, 5, 0, SHEET), PartPose.ZERO);
		shell.addOrReplaceChild("moss_back", CubeListBuilder.create().texOffs(88, 25).mirror().addBox(-9, 1.5F, 12.02F, 18, 5, 0, SHEET), PartPose.ZERO);
		root.addOrReplaceChild("head", CubeListBuilder.create()
			.texOffs(76, 71).addBox(-2.5F, -2, -4.5F, 5, 5, 5)
			.texOffs(96, 71).addBox(-2.5F, -4, -9.5F, 5, 5, 6)
			.texOffs(76, 81).addBox(-1.5F, -1.5F, -10.5F, 3, 2, 2), PartPose.offset(0, 15, -11.5F));
		root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(24, 94).addBox(-1.5F, -1, 0, 3, 2, 4), PartPose.offset(0, 16, 11.5F));
		leg(root, "front_left", 7.5F, -7.5F, false);
		leg(root, "front_right", -7.5F, -7.5F, true);
		leg(root, "hind_left", 7.5F, 7.5F, false);
		leg(root, "hind_right", -7.5F, 7.5F, true);
		return LayerDefinition.create(mesh, 128, 128);
	}

	private static void leg(PartDefinition root, String name, float x, float z, boolean mirror) {
		CubeListBuilder cube = CubeListBuilder.create().texOffs(0, 94);
		if (mirror) {
			cube.mirror();
		}
		root.addOrReplaceChild(name + "_leg", cube.addBox(-3, 0, -3, 6, 7, 6), PartPose.offset(x, 17, z));
	}

	/** Moves {@code poseStack} onto the top of the crown, following the shell as it rocks and settles. */
	public void toCrown(PoseStack poseStack) {
		shell.translateAndRotate(poseStack);
		poseStack.translate(0, CROWN / 16F, 0);
	}

	@Override
	public void setupAnim(WildlifeRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks + state.seed;
		float speed = Math.min(1, state.walkAnimationSpeed * 1.4F);
		float walk = state.walkAnimationPos * 0.5F;
		float hide = state.hide;
		float out = 1 - hide;

		// A slow, heavy plod: diagonal pairs, the shell rocking from side to side.
		float swing = Mth.cos(walk) * 0.5F * speed * out;
		frontLeft.xRot = swing;
		hindRight.xRot = swing;
		frontRight.xRot = -swing;
		hindLeft.xRot = -swing;
		shell.zRot = Mth.sin(walk) * 0.035F * speed;
		shell.y = 13 - Mth.abs(Mth.sin(walk)) * 0.4F * speed + Mth.sin(t * 0.05F) * 0.12F * out;

		// The head: looking about, nodding with each step, stretching out now and then.
		float stretch = Mth.clamp(Mth.sin(t * 0.013F) * 4 - 3, 0, 1);
		head.yRot = Mth.clamp(state.yRot, -45, 45) * Mth.DEG_TO_RAD * out;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.6F * out + Mth.sin(walk * 2) * 0.06F * speed;
		head.z = -11.5F - stretch * 1.5F * out + Mth.sin(walk * 2) * 0.3F * speed;
		tail.yRot = Mth.sin(walk) * 0.3F * speed + Mth.sin(t * 0.04F) * 0.08F;

		// Into the shell: it settles on the ground, legs drawn up, head pulled back, tail tucked away.
		if (hide > 0) {
			shell.y += hide * 5.5F;
			for (ModelPart leg : new ModelPart[] {frontLeft, frontRight, hindLeft, hindRight}) {
				leg.y = 17 + hide * 2.5F;
				leg.yScale = 1 - hide * 0.75F;
				leg.xScale = leg.zScale = 1 - hide * 0.25F;
			}
			frontLeft.x = 7.5F - hide * 1.5F;
			hindLeft.x = 7.5F - hide * 1.5F;
			frontRight.x = -7.5F + hide * 1.5F;
			hindRight.x = -7.5F + hide * 1.5F;
			head.z += hide * 7.5F;
			head.y += hide * 4.5F;
			head.xScale = head.yScale = head.zScale = 1 - hide * 0.35F;
			tail.visible = hide < 0.5F;
		} else {
			tail.visible = true;
		}

		// A baby: bigger-headed, its shell still bare.
		if (state.isBaby) {
			head.xScale *= 1.45F;
			head.yScale *= 1.45F;
			head.zScale *= 1.45F;
			head.z += 1;
		}
		for (ModelPart strand : moss) {
			strand.visible = !state.isBaby;
		}
	}
}
