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
 * The Gulper Pitcher: a rosette of four broad leaves on the ground, and up out of it a great pitcher, a fat belly narrowing
 * to a neck, a thick rim round its mouth and a lid hinged at the back, a fringe of teeth under the lid's front edge; and a
 * vine of three segments ending in a hooked leaf, coiled on the ground at its foot.
 *
 * <p>Asleep it sways, its lid shut but for a crack now and then. Stirred ({@code alt}) its lid stands open. Its vine rears
 * back over it, the lid thrown wide ({@code windup}), then lashes straight out at whoever it's facing, stretching far beyond
 * its length ({@code acting}). Biting ({@code guard}) the whole pitcher lunges forward and the lid slams down.</p>
 *
 * <p>Laid out on a 64x64 skin (see {@code tools/pitcher_art.py}):</p>
 * <pre>
 *   belly (0,0) 8x9x8       neck (32,0) 6x8x6
 *   rim (0,18) 9x2x9        teeth (36,18) 7x1x1    vine (36,22) 1x1x5    hook (48,22) 2x2x3
 *   lid (0,30) 9x1x9        leaf (0,42) 4x1x8
 * </pre>
 */
public class GulperPitcherModel extends EntityModel<MonsterRenderState> {
	/** The vine's coil on the ground at rest, segment by segment, and reared back over the lid. */
	private static final float[] COIL_Y = {-1.3F, 1.1F, 1.2F, 1.4F};
	private static final float[] REAR_X = {-1.3F, -0.7F, -0.6F, -0.5F};

	private final ModelPart pitcher;
	private final ModelPart neck;
	private final ModelPart lid;
	private final ModelPart[] vine = new ModelPart[4];
	private final ModelPart[] leaves = new ModelPart[4];

	public GulperPitcherModel(ModelPart root) {
		super(root);
		this.pitcher = root.getChild("pitcher");
		this.neck = pitcher.getChild("neck");
		this.lid = neck.getChild("rim").getChild("lid");
		ModelPart part = root;
		for (int i = 0; i < vine.length; i++) {
			part = part.getChild(i < 3 ? "vine" + (i + 1) : "hook");
			vine[i] = part;
		}
		for (int i = 0; i < 4; i++) {
			leaves[i] = root.getChild("leaf" + i);
		}
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition pitcher = root.addOrReplaceChild("pitcher", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -9.0F, -4.0F, 8.0F, 9.0F, 8.0F),
			PartPose.offset(0.0F, 24.0F, 0.0F));
		PartDefinition neck = pitcher.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(32, 0).addBox(-3.0F, -8.0F, -3.0F, 6.0F, 8.0F, 6.0F),
			PartPose.offset(0.0F, -9.0F, 0.0F));
		PartDefinition rim = neck.addOrReplaceChild("rim", CubeListBuilder.create().texOffs(0, 18).addBox(-4.5F, -2.0F, -4.5F, 9.0F, 2.0F, 9.0F),
			PartPose.offset(0.0F, -8.0F, 0.0F));
		PartDefinition lid = rim.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 30).addBox(-4.5F, -1.0F, -9.0F, 9.0F, 1.0F, 9.0F),
			PartPose.offset(0.0F, -2.0F, 4.5F));
		lid.addOrReplaceChild("teeth", CubeListBuilder.create().texOffs(36, 18).addBox(-3.5F, 0.0F, -8.5F, 7.0F, 1.0F, 1.0F), PartPose.ZERO);

		// The vine, rooted at the front-right of the pitcher's foot.
		PartDefinition part = root.addOrReplaceChild("vine1", CubeListBuilder.create().texOffs(36, 22).addBox(-0.5F, -0.5F, -5.0F, 1.0F, 1.0F, 5.0F),
			PartPose.offset(-3.0F, 22.5F, -3.0F));
		for (int i = 2; i <= 3; i++) {
			part = part.addOrReplaceChild("vine" + i, CubeListBuilder.create().texOffs(36, 22).addBox(-0.5F, -0.5F, -5.0F, 1.0F, 1.0F, 5.0F),
				PartPose.offset(0.0F, 0.0F, -5.0F));
		}
		part.addOrReplaceChild("hook", CubeListBuilder.create().texOffs(48, 22).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 3.0F),
			PartPose.offset(0.0F, 0.0F, -5.0F));

		for (int i = 0; i < 4; i++) {
			root.addOrReplaceChild("leaf" + i, CubeListBuilder.create().texOffs(0, 42).addBox(-2.0F, -1.0F, -11.0F, 4.0F, 1.0F, 8.0F),
				PartPose.offsetAndRotation(0.0F, 24.0F, 0.0F, -0.12F, i * Mth.HALF_PI + Mth.PI / 4, 0.0F));
		}
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(MonsterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float awake = state.alt;
		float rear = state.windup;
		float lash = state.acting;
		float bite = state.guard;

		// The pitcher sways, slow asleep, a little restless awake; biting, it lunges forward over its mouth.
		pitcher.xRot = Mth.sin(t * 0.05F) * 0.04F + 0.45F * bite - 0.1F * rear;
		pitcher.zRot = Mth.sin(t * 0.037F + 1) * 0.04F * (1 + awake);
		neck.xRot = Mth.sin(t * 0.05F + 0.8F) * 0.05F + 0.2F * bite;
		neck.yRot = state.yRot * Mth.DEG_TO_RAD * 0.3F * awake;

		// The lid: shut asleep, cracking open now and then; open awake; thrown wide when the vine rears; slammed in the bite.
		float crack = Mth.clamp(Mth.sin(t * 0.02F) * 4 - 3, 0, 1) * 0.25F;
		float open = Math.max(crack, 0.75F * awake + Mth.sin(t * 0.21F) * 0.06F * awake);
		open = Mth.lerp(rear, open, 1.35F);
		open = Mth.lerp(bite, open, 0.0F);
		lid.xRot = -open;

		// The vine: coiled on the ground, its tip twitching; reared back over the lid; then lashed straight out, stretching.
		float twitch = Mth.sin(t * 0.13F) * 0.08F;
		float aim = state.xRot * Mth.DEG_TO_RAD;
		for (int i = 0; i < vine.length; i++) {
			ModelPart seg = vine[i];
			float restY = COIL_Y[i] + (i == 3 ? twitch * 3 : twitch) * (1 + awake);
			float restX = i == 0 ? 0.25F : 0.05F;
			float x = Mth.lerp(rear, restX, REAR_X[i]);
			float y = Mth.lerp(rear, restY, i == 0 ? 0.15F : 0.0F);
			x = Mth.lerp(lash, x, i == 0 ? aim * 0.8F - 0.05F : 0.0F);
			y = Mth.lerp(lash, y, i == 0 ? 0.15F : 0.0F);
			seg.xRot = x;
			seg.yRot = y;
			seg.zScale = 1.0F;
		}
		vine[0].zScale = 1.0F + 3.2F * lash;
		vine[0].y = 22.5F - 6.0F * rear * (1 - lash) - 4.0F * lash;

		// The leaves breathe; awake they lift a little, bristling.
		for (int i = 0; i < leaves.length; i++) {
			leaves[i].xRot = -0.12F - 0.15F * awake + Mth.sin(t * 0.06F + i * 1.3F) * 0.04F - 0.2F * bite;
		}
	}
}
