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
 * The Sandmaw Wyrm: a column of four great ringed segments rising out of the sand, narrowing upward, a ridge of spines
 * down its back, and a blunt head whose mouth, facing forward, is ringed with four jaw-plates that flare open.
 *
 * <p>Under the sand ({@code veil}) it sinks out of sight below the ground. Up, it sways slowly, jaws working. Bursting up
 * ({@code guard}) it rears straight up, jaws wide. The slam's tell ({@code windup}) is it rearing back, head high; the slam
 * ({@code acting}) whips the whole column forward, head to the ground, where it sticks ({@code stunned}), twitching. The
 * spit ({@code alt}) pulls its head back with its jaws flared wide, then thrusts it forward.</p>
 *
 * <p>Laid out on a 128x64 skin (see {@code tools/sandmaw_art.py}):</p>
 * <pre>
 *   seg0 (0,0) 12x10x12    seg1 (48,0) 11x10x11   seg2 (0,22) 10x10x10   seg3 (40,22) 9x10x9
 *   head (76,22) 10x8x10   jaw side (0,44) 1x6x4  jaw lip (12,44) 4x1x4  spine (30,44) 2x3x2
 * </pre>
 */
public class SandmawWyrmModel extends EntityModel<MonsterRenderState> {
	private static final float BODY_Y = 24.0F;
	/** How far it sinks under the sand, fully burrowed. */
	private static final float SINK = 54.0F;

	private final ModelPart body;
	private final ModelPart[] segs = new ModelPart[4];
	private final ModelPart head;
	private final ModelPart[] jaws = new ModelPart[4];

	public SandmawWyrmModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		ModelPart parent = body;
		for (int i = 0; i < 4; i++) {
			segs[i] = parent.getChild("seg_" + i);
			parent = segs[i];
		}
		this.head = segs[3].getChild("head");
		for (int i = 0; i < 4; i++) {
			jaws[i] = head.getChild("jaw_" + i);
		}
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, BODY_Y, 0.0F));

		int[][] uv = {{0, 0}, {48, 0}, {0, 22}, {40, 22}};
		float[] size = {12, 11, 10, 9};
		PartDefinition parent = body;
		for (int i = 0; i < 4; i++) {
			float s = size[i];
			CubeListBuilder cubes = CubeListBuilder.create().texOffs(uv[i][0], uv[i][1]).addBox(-s / 2, -10.0F, -s / 2, s, 10.0F, s);
			if (i > 0) {
				cubes.texOffs(30, 44).addBox(-1.0F, -7.0F, s / 2 - 0.5F, 2.0F, 3.0F, 2.0F);
			}
			parent = parent.addOrReplaceChild("seg_" + i, cubes, PartPose.offset(0.0F, i == 0 ? 0.0F : -9.0F, 0.0F));
		}
		PartDefinition head = parent.addOrReplaceChild("head", CubeListBuilder.create().texOffs(76, 22).addBox(-5.0F, -8.0F, -5.0F, 10.0F, 8.0F, 10.0F),
			PartPose.offset(0.0F, -9.0F, 0.0F));
		// Its jaw-plates, ringing the mouth: two at the sides, one above, one below.
		head.addOrReplaceChild("jaw_0", CubeListBuilder.create().texOffs(0, 44).addBox(-0.5F, -3.0F, -4.0F, 1.0F, 6.0F, 4.0F),
			PartPose.offset(-4.0F, -4.0F, -5.0F));
		head.addOrReplaceChild("jaw_1", CubeListBuilder.create().texOffs(0, 44).mirror().addBox(-0.5F, -3.0F, -4.0F, 1.0F, 6.0F, 4.0F),
			PartPose.offset(4.0F, -4.0F, -5.0F));
		head.addOrReplaceChild("jaw_2", CubeListBuilder.create().texOffs(12, 44).addBox(-2.0F, -0.5F, -4.0F, 4.0F, 1.0F, 4.0F),
			PartPose.offset(0.0F, -7.0F, -5.0F));
		head.addOrReplaceChild("jaw_3", CubeListBuilder.create().texOffs(12, 44).addBox(-2.0F, -0.5F, -4.0F, 4.0F, 1.0F, 4.0F),
			PartPose.offset(0.0F, -1.0F, -5.0F));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(MonsterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float rear = state.windup * (1 - state.alt);
		float slam = Math.max(state.acting * (1 - state.alt), state.stunned);
		float spitTell = state.windup * state.alt;
		float spit = state.acting * state.alt;
		float erupt = state.guard;
		float stuck = state.stunned;
		float settled = (1 - rear) * (1 - slam) * (1 - spitTell) * (1 - spit) * (1 - erupt);

		body.y = BODY_Y + SINK * state.veil;

		// The column: swaying slowly, rearing back for the slam, whipping forward into it, pulling back to spit.
		float lean = -0.16F * rear + 0.4F * slam - 0.08F * spitTell + 0.1F * spit - 0.05F * erupt + 0.03F * settled;
		float total = 0;
		for (int i = 0; i < 4; i++) {
			float share = i == 0 ? 0.5F : 1.0F;
			float sway = Mth.sin(t * 0.07F + i * 0.8F) * 0.07F * (1 - slam);
			segs[i].xRot = lean * share + Mth.cos(t * 0.05F + i * 0.6F) * 0.03F * settled;
			segs[i].zRot = sway + Mth.sin(t * 1.3F + i) * 0.06F * stuck;
			segs[i].yRot = 0;
			total += segs[i].xRot;
		}

		// The head: turning to look, held level against the column's lean; high in the tell, low in the slam.
		head.xRot = (-total * 0.6F + state.xRot * Mth.DEG_TO_RAD * 0.5F) * settled
			- 0.5F * rear + 0.25F * slam - 0.35F * spitTell + 0.15F * spit - 0.6F * erupt;
		head.yRot = Mth.clamp(state.yRot, -60, 60) * Mth.DEG_TO_RAD * 0.7F * settled;
		head.zRot = Mth.sin(t * 1.7F) * 0.08F * stuck;

		// The jaws: working slowly; flaring open as it rears, wide to spit and as it bursts up; clamped as it slams.
		float flare = Math.max(0, 0.15F + Mth.sin(t * 0.11F) * 0.1F * settled + 0.5F * rear + 1.0F * spitTell + 1.2F * spit
			+ 1.1F * erupt - 0.2F * slam + Math.max(0, Mth.sin(t * 0.9F)) * 0.3F * stuck);
		jaws[0].yRot = flare;
		jaws[1].yRot = -flare;
		jaws[2].xRot = -flare;
		jaws[3].xRot = flare;
	}
}
