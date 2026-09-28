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
 * The Archivist: a tall hooded figure in a long robe that flares to the floor, hovering a little
 * above it, with long sleeves and an empty dark under the hood where two eyes burn. An open tome
 * floats before it, a page turning now and then, and three loose pages circle it at head height.
 * Casting, it raises its arms and the tome lifts and riffles; rewriting its Cord, it throws its
 * arms wide, the tome rises open over it and the pages fly far out and fast.
 *
 * <p>Laid out on a 128x64 skin (see {@code archivist_texture} in {@code tools/world_art.py}):</p>
 * <pre>
 *   hood (0,0) 10x11x8      robe (36,0) 10x9x7     mantle (70,0) 12x3x8    cheek (110,0) 2x8x2
 *   brim (70,11) 10x3x3     skirt (0,19) 11x7x8    hem (38,19) 13x7x10     sleeve (84,19) 4x11x4
 *   cuff (100,19) 5x5x5     cover (0,36) 7x10x1    pages (16,36) 6x9x1     turning page (30,36) 6x9
 *   loose page (42,36) 4x5
 * </pre>
 */
public class ArchivistModel extends EntityModel<ArchivistRenderState> {
	private static final int LOOSE_PAGES = 3;

	private final ModelPart figure;
	private final ModelPart body;
	private final ModelPart hem;
	private final ModelPart head;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart tome;
	private final ModelPart tomeRight;
	private final ModelPart tomeLeft;
	private final ModelPart turningPage;
	private final ModelPart orbit;
	private final ModelPart[] pages = new ModelPart[LOOSE_PAGES];

	public ArchivistModel(ModelPart root) {
		super(root);
		this.figure = root.getChild("figure");
		this.body = figure.getChild("body");
		this.hem = body.getChild("hem");
		this.head = figure.getChild("head");
		this.rightArm = figure.getChild("right_arm");
		this.leftArm = figure.getChild("left_arm");
		this.tome = root.getChild("tome");
		this.tomeRight = tome.getChild("right_half");
		this.tomeLeft = tome.getChild("left_half");
		this.turningPage = tome.getChild("turning_page");
		this.orbit = root.getChild("orbit");
		for (int i = 0; i < LOOSE_PAGES; i++) {
			pages[i] = orbit.getChild("page_" + i);
		}
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition figure = root.addOrReplaceChild("figure", CubeListBuilder.create(), PartPose.ZERO);

		// The robe: a mantle over the shoulders, then flaring in steps to a hem that hangs free.
		PartDefinition body = figure.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(70, 0).addBox(-6.0F, -4.0F, -4.0F, 12.0F, 3.0F, 8.0F)
				.texOffs(36, 0).addBox(-5.0F, -1.0F, -3.5F, 10.0F, 9.0F, 7.0F)
				.texOffs(0, 19).addBox(-5.5F, 8.0F, -4.0F, 11.0F, 7.0F, 8.0F),
			PartPose.ZERO);
		body.addOrReplaceChild("hem", CubeListBuilder.create().texOffs(38, 19).addBox(-6.5F, 0.0F, -5.0F, 13.0F, 7.0F, 10.0F),
			PartPose.offset(0.0F, 15.0F, 0.0F));

		// The hood: a deep cowl with a brim over the face and a cheek either side, the face itself set back in the dark.
		figure.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-5.0F, -11.0F, -3.0F, 10.0F, 11.0F, 8.0F)
				.texOffs(70, 11).addBox(-5.0F, -11.0F, -6.0F, 10.0F, 3.0F, 3.0F)
				.texOffs(110, 0).addBox(-5.0F, -8.0F, -5.0F, 2.0F, 8.0F, 2.0F)
				.texOffs(110, 0).mirror().addBox(3.0F, -8.0F, -5.0F, 2.0F, 8.0F, 2.0F),
			PartPose.offset(0.0F, -4.0F, 0.0F));

		// Long sleeves, wide at the cuff; no hands show.
		figure.addOrReplaceChild("right_arm", CubeListBuilder.create()
				.texOffs(84, 19).addBox(-3.0F, -0.5F, -2.0F, 4.0F, 11.0F, 4.0F)
				.texOffs(100, 19).addBox(-3.5F, 9.0F, -2.5F, 5.0F, 5.0F, 5.0F),
			PartPose.offset(-6.0F, -3.0F, 0.0F));
		figure.addOrReplaceChild("left_arm", CubeListBuilder.create()
				.texOffs(84, 19).mirror().addBox(-1.0F, -0.5F, -2.0F, 4.0F, 11.0F, 4.0F)
				.texOffs(100, 19).mirror().addBox(-1.5F, 9.0F, -2.5F, 5.0F, 5.0F, 5.0F),
			PartPose.offset(6.0F, -3.0F, 0.0F));

		// The tome, open toward whoever faces the Archivist: two halves hinged at the spine, and a page that turns.
		PartDefinition tome = root.addOrReplaceChild("tome", CubeListBuilder.create(), PartPose.offset(0.0F, 1.0F, -10.0F));
		tome.addOrReplaceChild("right_half", CubeListBuilder.create()
				.texOffs(0, 36).addBox(-7.0F, -5.0F, 0.0F, 7.0F, 10.0F, 1.0F)
				.texOffs(16, 36).addBox(-6.5F, -4.5F, -1.0F, 6.0F, 9.0F, 1.0F),
			PartPose.ZERO);
		tome.addOrReplaceChild("left_half", CubeListBuilder.create()
				.texOffs(0, 36).mirror().addBox(0.0F, -5.0F, 0.0F, 7.0F, 10.0F, 1.0F)
				.texOffs(16, 36).mirror().addBox(0.5F, -4.5F, -1.0F, 6.0F, 9.0F, 1.0F),
			PartPose.ZERO);
		tome.addOrReplaceChild("turning_page", CubeListBuilder.create()
				.texOffs(30, 36).addBox(0.0F, -4.5F, 0.0F, 6.0F, 9.0F, 0.0F, EnumSet.of(Direction.NORTH)),
			PartPose.offset(0.0F, 0.0F, -1.6F));

		// Loose pages circling at head height; placed each frame.
		PartDefinition orbit = root.addOrReplaceChild("orbit", CubeListBuilder.create(), PartPose.ZERO);
		for (int i = 0; i < LOOSE_PAGES; i++) {
			orbit.addOrReplaceChild("page_" + i, CubeListBuilder.create()
					.texOffs(42, 36).addBox(-2.0F, -2.5F, 0.0F, 4.0F, 5.0F, 0.0F, EnumSet.of(Direction.NORTH)),
				PartPose.ZERO);
		}
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(ArchivistRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float c = state.casting;
		float r = state.rewriting;
		float dying = state.deathTime;
		float walk = Math.min(1, state.walkAnimationSpeed);

		// It hovers, bobbing gently, leaning into its drift; the hem sways and trails.
		figure.y = -1.0F + Mth.sin(t * 0.08F) * 1.0F + dying * 0.7F;
		figure.xRot = walk * 0.22F + dying * 0.03F;
		body.zRot = Mth.sin(t * 0.045F) * 0.03F;
		hem.xRot = walk * 0.35F + Mth.sin(t * 0.09F) * 0.06F;
		hem.zRot = Mth.cos(t * 0.07F) * 0.045F;

		head.yRot = state.yRot * Mth.DEG_TO_RAD * (1 - r);
		head.xRot = Mth.lerp(r, Mth.clamp(state.xRot, -40, 40) * Mth.DEG_TO_RAD, -0.5F);

		// Arms: hanging a little forward; casting, raised in a V over the tome (swinging out wide on the way,
		// clear of the book); rewriting, thrown wide and high.
		float sway = Mth.sin(t * 0.06F) * 0.05F;
		float weave = Mth.sin(t * 0.55F) * 0.08F * c;
		float out = Mth.sin(Mth.PI * c) * 0.7F;
		poseArm(rightArm, 1, sway, weave, out, c, r, t);
		poseArm(leftArm, -1, sway, -weave, out, c, r, t + 17);

		// The tome floats before it, out of step with its bob; casting it lifts and opens flatter; rewriting it
		// rises over its head, lying open to the sky.
		float open = Mth.lerp(r, Mth.lerp(c, 0.45F, 0.22F), 0.06F) + dying * 0.05F;
		tome.y = Mth.lerp(r, 1.0F + Mth.sin(t * 0.07F + 1.3F) * 1.0F - 1.2F * c, -19.0F) + dying * 1.1F;
		tome.z = -10.0F + 4.0F * r;
		tome.xRot = Mth.lerp(r, Mth.lerp(c, -0.5F, -0.3F), -1.2F);
		tome.yRot = Mth.sin(t * 0.037F) * 0.08F * (1 - r) + r * Mth.sin(t * 0.08F) * 0.9F;
		tome.zRot = Mth.sin(t * 0.05F) * 0.05F;
		tomeLeft.yRot = open;
		tomeRight.yRot = -open;

		// A page turns now and then; casting they riffle; rewriting they fly.
		float period = r > 0.5F ? 5 : c > 0.5F ? 8 : 90;
		float turn = Math.min(18, period * 0.8F);
		float local = t % period;
		turningPage.visible = local < turn;
		if (turningPage.visible) {
			float f = local / turn;
			float ease = f * f * (3 - 2 * f);
			turningPage.yRot = Mth.lerp(ease, Mth.PI - open, open);
		}

		// The loose pages: circling, bobbing and fluttering; flung far and fast in the rewrite; scattering as it dies.
		// (The quickening is a swing on top of the steady turn, so easing in and out of a pose never jumps.)
		orbit.yRot = t * 0.035F + c * Mth.sin(t * 0.09F) * 0.6F + r * Mth.sin(t * 0.12F) * 2.0F;
		// (Casting, they widen a little to clear the raised sleeves.)
		float radius = 15.0F + 3.0F * c + 9.0F * r + dying * 1.5F;
		for (int i = 0; i < LOOSE_PAGES; i++) {
			ModelPart page = pages[i];
			float at = Mth.TWO_PI * i / LOOSE_PAGES;
			page.x = Mth.sin(at) * radius;
			page.z = Mth.cos(at) * radius;
			page.y = -9.0F - 3.0F * r + Mth.sin(t * 0.06F + i * 2.1F) * 2.0F + dying * 0.8F;
			page.yRot = at;
			page.xRot = Mth.sin(t * 0.15F + i * 1.7F) * (0.3F + 0.4F * r);
			page.zRot = Mth.sin(t * 0.11F + i) * 0.25F;
		}
	}

	/**
	 * One arm, {@code side} 1 for the right and -1 for the left: from the idle hang toward the casting
	 * V ({@code c}) and the rewriting spread ({@code r}).
	 */
	private static void poseArm(ModelPart arm, int side, float sway, float weave, float out, float c, float r, float t) {
		float xRot = Mth.lerp(c, -0.35F + sway, -2.4F + weave);
		float zRot = Mth.lerp(c, 0.1F, -0.35F) * side;
		float yRot = out * side;
		arm.xRot = Mth.lerp(r, xRot, -0.3F + Mth.sin(t * 0.3F) * 0.06F);
		arm.yRot = Mth.lerp(r, yRot, 0);
		arm.zRot = Mth.lerp(r, zRot, 2.2F * side);
	}
}
