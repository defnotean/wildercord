package dev.wildercord.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import java.util.EnumSet;

/**
 * The Tide Scribe: a drowned sorcerer from the waist up, hooded, in a sodden robe hung with kelp, a
 * quill in one hand; below the waist, six tendrils of ink in three joints each. An open scroll floats
 * before it, its writing glowing. Swimming in the flood it leans forward and its tendrils stream out
 * behind; casting, it raises the scroll and its quill scribbles; stranded in ice it slumps and its
 * tendrils hang; between phases it throws its arms wide and the tendrils writhe; dying, it sinks with
 * its head bowed and the scroll drifts down.
 *
 * <p>Laid out on a 128x64 skin (see {@code tide_scribe_texture} in {@code tools/dungeon_assets.py}):</p>
 * <pre>
 *   head (0,0) 8x8x8        hood (32,0) 9x9x9       torso (0,18) 8x12x4     collar (24,18) 10x3x6
 *   arm (56,18) 4x12x4      tendril (0,36) 3x7x3    tendril mid (12,36) 2x7x2   tendril tip (20,36) 1x6x1
 *   scroll (32,36) 12x8     scroll rod (58,36) 1x10x1   quill (64,36) 1x7x1
 * </pre>
 */
public class TideScribeModel extends EntityModel<DungeonBossRenderState> {
	private static final int TENDRILS = 6;

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart scroll;
	private final ModelPart[] tendrils = new ModelPart[TENDRILS];
	private final ModelPart[] mids = new ModelPart[TENDRILS];
	private final ModelPart[] tips = new ModelPart[TENDRILS];

	public TideScribeModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.rightArm = body.getChild("right_arm");
		this.leftArm = body.getChild("left_arm");
		this.scroll = root.getChild("scroll");
		for (int i = 0; i < TENDRILS; i++) {
			tendrils[i] = body.getChild("tendril_" + i);
			mids[i] = tendrils[i].getChild("mid");
			tips[i] = mids[i].getChild("tip");
		}
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 18).addBox(-4.0F, -12.0F, -2.0F, 8.0F, 12.0F, 4.0F)
				.texOffs(24, 18).addBox(-5.0F, -12.5F, -3.0F, 10.0F, 3.0F, 6.0F),
			PartPose.offset(0.0F, 8.0F, 0.0F));
		body.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
				.texOffs(32, 0).addBox(-4.5F, -8.5F, -4.5F, 9.0F, 9.0F, 9.0F),
			PartPose.offset(0.0F, -12.0F, 0.0F));
		PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(56, 18).addBox(-3.0F, -1.0F, -2.0F, 4.0F, 12.0F, 4.0F),
			PartPose.offset(-5.0F, -11.0F, 0.0F));
		rightArm.addOrReplaceChild("quill", CubeListBuilder.create().texOffs(64, 36).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 7.0F, 1.0F),
			PartPose.offsetAndRotation(-1.0F, 10.5F, -1.0F, -0.6F, 0.0F, 0.0F));
		body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(56, 18).mirror().addBox(-1.0F, -1.0F, -2.0F, 4.0F, 12.0F, 4.0F),
			PartPose.offset(5.0F, -11.0F, 0.0F));
		// Six tendrils of ink, round the hem, each in three joints.
		for (int i = 0; i < TENDRILS; i++) {
			float a = Mth.TWO_PI * i / TENDRILS;
			PartDefinition tendril = body.addOrReplaceChild("tendril_" + i, CubeListBuilder.create().texOffs(0, 36).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 7.0F, 3.0F),
				PartPose.offset(Mth.sin(a) * 2.5F, -0.5F, Mth.cos(a) * 1.2F));
			PartDefinition mid = tendril.addOrReplaceChild("mid", CubeListBuilder.create().texOffs(12, 36).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F),
				PartPose.offset(0.0F, 6.5F, 0.0F));
			mid.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(20, 36).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F),
				PartPose.offset(0.0F, 6.5F, 0.0F));
		}
		// The open scroll, floating before it between its two rods.
		PartDefinition scroll = root.addOrReplaceChild("scroll", CubeListBuilder.create()
				.texOffs(32, 36).addBox(-6.0F, -4.0F, 0.0F, 12.0F, 8.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)),
			PartPose.offset(0.0F, 2.0F, -9.0F));
		scroll.addOrReplaceChild("left_rod", CubeListBuilder.create().texOffs(58, 36).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 10.0F, 1.0F),
			PartPose.offset(6.5F, 0.0F, 0.0F));
		scroll.addOrReplaceChild("right_rod", CubeListBuilder.create().texOffs(58, 36).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 10.0F, 1.0F),
			PartPose.offset(-6.5F, 0.0F, 0.0F));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(DungeonBossRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float c = state.casting;
		float sh = state.shifting;
		float swim = state.guarded;
		float slump = state.exposed;
		float dying = Math.min(1, state.deathTime / 40.0F);

		// It hovers, bobbing; swimming it leans into the water; stranded or dying, it slumps.
		body.y = 8.0F + Mth.sin(t * 0.08F) * 0.8F + dying * 6.0F;
		body.xRot = 0.45F * swim + 0.3F * slump + 0.35F * dying - 0.1F * sh;
		body.zRot = Mth.sin(t * 0.05F) * 0.05F;
		head.yRot = state.yRot * Mth.DEG_TO_RAD * (1 - sh);
		head.xRot = Mth.clamp(state.xRot, -30, 30) * Mth.DEG_TO_RAD * (1 - sh) - 0.6F * sh + 0.5F * (slump + dying) - 0.3F * swim;

		// Arms: hanging; casting, the left lifts the scroll and the right scribbles; thrown wide between phases.
		float scribble = Mth.sin(t * 1.3F) * 0.15F * c;
		rightArm.xRot = Mth.lerp(c, -0.2F + Mth.sin(t * 0.06F) * 0.05F, -1.3F + scribble);
		rightArm.yRot = Mth.lerp(c, 0, -0.3F + Mth.cos(t * 1.1F) * 0.12F * c);
		rightArm.zRot = Mth.lerp(sh, 0.1F, 1.8F);
		leftArm.xRot = Mth.lerp(c, -0.2F + Mth.sin(t * 0.06F + 1) * 0.05F, -1.5F);
		leftArm.yRot = 0;
		leftArm.zRot = Mth.lerp(sh, -0.1F, -1.8F);
		rightArm.xRot = Mth.lerp(slump + dying * 0.8F, rightArm.xRot, 0.1F);
		leftArm.xRot = Mth.lerp(slump + dying * 0.8F, leftArm.xRot, 0.1F);

		// Tendrils: swaying in a slow wave; streaming out behind while it swims; writhing between phases; limp when stranded.
		float speed = 0.1F + 0.25F * sh + 0.1F * swim;
		float reach = (0.25F + 0.35F * sh) * (1 - 0.7F * slump) * (1 - dying);
		for (int i = 0; i < TENDRILS; i++) {
			float a = Mth.TWO_PI * i / TENDRILS;
			float wave = t * speed + i * 1.1F;
			float outward = 0.35F + 0.25F * sh;
			tendrils[i].xRot = Mth.cos(a) * outward + Mth.sin(wave) * reach + 0.9F * swim;
			tendrils[i].zRot = -Mth.sin(a) * outward + Mth.cos(wave) * reach * 0.6F;
			mids[i].xRot = Mth.sin(wave - 0.9F) * reach * 1.4F + 0.2F * swim;
			tips[i].xRot = Mth.sin(wave - 1.8F) * reach * 1.8F;
		}

		// The scroll floats before it; casting, it rises and spreads; dying, it drifts down.
		scroll.y = 2.0F + Mth.sin(t * 0.07F + 1.3F) * 1.0F - 4.0F * c + dying * 14.0F;
		scroll.z = -9.0F - 2.0F * c;
		scroll.xRot = -0.25F + 0.25F * c + Mth.sin(t * 0.05F) * 0.06F + dying * 0.9F;
		scroll.yRot = state.yRot * Mth.DEG_TO_RAD * 0.3F + Mth.sin(t * 0.04F) * 0.1F;
	}
}
