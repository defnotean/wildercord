package dev.wildercord.client.monster;

import dev.wildercord.monster.GiantRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The Bramblewalker: a hunched trunk of bark on two stumpy root-legs, its head a knot of bramble sunk low between mossy,
 * leafy shoulders, thorned branches reaching up off its back like a crown, and long vine-wrapped arms hanging to its knees
 * in twig claws, with vines trailing off them. It lumbers, swaying; rearing for its lash it leans back with both arms
 * raised high behind it, vines lifting; the lash flings its right arm forward as its body snaps down after it. Grown into a
 * giant it fights by a whole set of moves ({@link GiantRules.Bramble}), each with its own pose: a foot raised and stamped, an
 * arm drawn back and swept across, a boulder heaved overhead and thrown, arms plunged into the ground, a reach and a squeeze,
 * both fists raised and hammered down (and stuck there), arms flung wide over a field of thorns, arms crossed and flung open
 * in a blaze, and a roar.
 *
 * <p>Laid out on a 128x64 skin (see {@code bramblewalker} in {@code tools/monster_art.py}):</p>
 * <pre>
 *   torso (0,0) 14x15x9    head (48,0) 9x8x8      arm (84,0) 5x17x5     hand (104,0) 6x4x6
 *   leg (0,26) 6x10x6      root (24,26) 8x3x8     leaves (56,22) 8x5x7  branch (88,22) 2x12x2
 *   twig (96,22) 1x5x1     vine (100,22) 4x10x0   tuft (0,44) 6x4x6
 * </pre>
 */
public class BramblewalkerModel extends EntityModel<MonsterRenderState> {
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;
	private final ModelPart[] branches;
	private final ModelPart[] vines;
	/** 0.13: a giant's pose at rest, the pose it's in, and the move being laid over it (reused each frame). */
	private final Pose rest = new Pose(), now = new Pose(), pose = new Pose();

	public BramblewalkerModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.rightArm = body.getChild("right_arm");
		this.leftArm = body.getChild("left_arm");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
		this.branches = new ModelPart[] {body.getChild("branch_0"), body.getChild("branch_1"), body.getChild("branch_2"), body.getChild("branch_3"),
			body.getChild("branch_4")};
		this.vines = new ModelPart[] {rightArm.getChild("vine"), leftArm.getChild("vine"), body.getChild("vine_0"), body.getChild("vine_1")};
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// The trunk, pivoting at the hips, hunched forward.
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -15.0F, -4.5F, 14.0F, 15.0F, 9.0F),
			PartPose.offsetAndRotation(0.0F, 14.0F, 1.0F, 0.32F, 0.0F, 0.0F));
		// Its head, a knot of bramble sunk low and forward between the shoulders.
		body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(48, 0).addBox(-4.5F, -6.0F, -6.5F, 9.0F, 8.0F, 8.0F),
			PartPose.offset(0.0F, -14.0F, -3.0F));
		// Mossy, leafy shoulders and a tuft on its back.
		body.addOrReplaceChild("right_leaves", CubeListBuilder.create().texOffs(56, 22).addBox(-4.0F, -3.0F, -3.5F, 8.0F, 5.0F, 7.0F),
			PartPose.offsetAndRotation(-6.0F, -14.0F, 0.5F, 0.0F, 0.0F, -0.22F));
		body.addOrReplaceChild("left_leaves", CubeListBuilder.create().texOffs(56, 22).mirror().addBox(-4.0F, -3.0F, -3.5F, 8.0F, 5.0F, 7.0F),
			PartPose.offsetAndRotation(6.0F, -14.0F, 0.5F, 0.0F, 0.0F, 0.22F));
		body.addOrReplaceChild("tuft", CubeListBuilder.create().texOffs(0, 44).addBox(-3.0F, -2.0F, -3.0F, 6.0F, 4.0F, 6.0F),
			PartPose.offsetAndRotation(0.0F, -9.0F, 4.0F, 0.3F, 0.0F, 0.0F));
		// Thorned branches reaching up off its back, each with a twig.
		float[][] crown = {{-5.0F, -13.0F, 2.5F, -0.35F, 0.2F, -0.55F}, {5.0F, -13.0F, 2.5F, -0.35F, -0.2F, 0.55F}, {-1.5F, -14.0F, 3.0F, -0.6F, 0.0F, -0.15F},
			{2.0F, -12.0F, 3.5F, -0.85F, 0.0F, 0.25F}, {-3.0F, -8.0F, 4.0F, -1.05F, 0.3F, -0.4F}};
		for (int i = 0; i < crown.length; i++) {
			float[] c = crown[i];
			PartDefinition branch = body.addOrReplaceChild("branch_" + i, CubeListBuilder.create().texOffs(88, 22).addBox(-1.0F, -12.0F, -1.0F, 2.0F, 12.0F, 2.0F),
				PartPose.offsetAndRotation(c[0], c[1], c[2], c[3], c[4], c[5]));
			branch.addOrReplaceChild("twig", CubeListBuilder.create().texOffs(96, 22).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 5.0F, 1.0F),
				PartPose.offsetAndRotation(0.0F, -7.0F - i % 2, 0.0F, 0.0F, 0.0F, i % 2 == 0 ? 0.7F : -0.7F));
		}
		// Vines trailing off its chest.
		body.addOrReplaceChild("vine_0", CubeListBuilder.create().texOffs(100, 22).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 10.0F, 0.0F),
			PartPose.offset(-3.5F, -5.0F, -4.6F));
		body.addOrReplaceChild("vine_1", CubeListBuilder.create().texOffs(100, 22).mirror().addBox(-2.0F, 0.0F, 0.0F, 4.0F, 10.0F, 0.0F),
			PartPose.offset(3.0F, -9.0F, -4.6F));

		// Long, vine-wrapped arms ending in twig claws.
		arm(body, "right_arm", -9.0F, false);
		arm(body, "left_arm", 9.0F, true);

		// Stumpy root-legs, spreading at the foot.
		leg(root, "right_leg", -3.6F, false);
		leg(root, "left_leg", 3.6F, true);
		return LayerDefinition.create(mesh, 128, 64);
	}

	private static void arm(PartDefinition body, String name, float x, boolean mirror) {
		CubeListBuilder upper = CubeListBuilder.create().texOffs(84, 0);
		PartDefinition arm = body.addOrReplaceChild(name, (mirror ? upper.mirror() : upper).addBox(-2.5F, -1.0F, -2.5F, 5.0F, 17.0F, 5.0F),
			PartPose.offset(x, -12.5F, -0.5F));
		CubeListBuilder palm = CubeListBuilder.create().texOffs(104, 0);
		PartDefinition hand = arm.addOrReplaceChild("hand", (mirror ? palm.mirror() : palm).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 4.0F, 6.0F),
			PartPose.offset(0.0F, 15.0F, 0.0F));
		for (int i = 0; i < 3; i++) {
			hand.addOrReplaceChild("claw_" + i, CubeListBuilder.create().texOffs(96, 22).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F),
				PartPose.offsetAndRotation(-1.8F + i * 1.8F, 3.5F, -2.0F, -0.35F, 0.0F, (i - 1) * 0.25F));
		}
		CubeListBuilder vine = CubeListBuilder.create().texOffs(100, 22);
		arm.addOrReplaceChild("vine", (mirror ? vine.mirror() : vine).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 10.0F, 0.0F),
			PartPose.offset(0.0F, 6.0F, 2.6F));
	}

	private static void leg(PartDefinition root, String name, float x, boolean mirror) {
		CubeListBuilder trunk = CubeListBuilder.create().texOffs(0, 26);
		PartDefinition leg = root.addOrReplaceChild(name, (mirror ? trunk.mirror() : trunk).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 10.0F, 6.0F),
			PartPose.offset(x, 14.0F, 1.0F));
		CubeListBuilder roots = CubeListBuilder.create().texOffs(24, 26);
		leg.addOrReplaceChild("roots", (mirror ? roots.mirror() : roots).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 3.0F, 8.0F), PartPose.offset(0.0F, 7.0F, 0.0F));
	}

	@Override
	public void setupAnim(MonsterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float walk = Math.min(1, state.walkAnimationSpeed);
		float pos = state.walkAnimationPos;
		float rear = state.windup;
		float lash = state.acting;
		float gait = Mth.cos(pos * 0.5F);

		// A heavy, swaying lumber; it breathes, slowly, standing.
		rightLeg.xRot = gait * 0.6F * walk;
		leftLeg.xRot = -gait * 0.6F * walk;
		float breathe = Mth.sin(t * 0.055F) * 0.03F;
		body.zRot = gait * 0.07F * walk;
		body.yRot = Mth.sin(pos * 0.25F) * 0.05F * walk;
		// Rearing back for the lash; snapping forward after it.
		body.xRot = 0.32F + breathe - 0.55F * rear + 0.38F * lash;

		head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.7F;
		head.xRot = Mth.clamp(state.xRot, -25, 25) * Mth.DEG_TO_RAD - 0.32F * (1 - rear) * 0.5F - 0.35F * rear;

		// Arms: swinging slow with the gait, hanging heavy; both raised high behind it as it rears; the right flung out in the lash.
		float swing = gait * 0.45F * walk;
		float sway = Mth.sin(t * 0.06F) * 0.05F;
		rightArm.xRot = Mth.lerp(rear, -swing - 0.15F + sway, -2.7F);
		leftArm.xRot = Mth.lerp(rear, swing - 0.15F - sway, -2.5F);
		rightArm.zRot = Mth.lerp(rear, 0.12F, 0.35F);
		leftArm.zRot = Mth.lerp(rear, -0.12F, -0.35F);
		rightArm.xRot = Mth.lerp(lash, rightArm.xRot, -1.35F);
		rightArm.zRot = Mth.lerp(lash, rightArm.zRot, 0.05F);
		leftArm.xRot = Mth.lerp(lash, leftArm.xRot, 0.3F);
		float shiver = state.giant ? giant(state, t) : 0;

		// The crown of branches stirs as if in a wind; it shivers as it rears, or as a giant gathers itself.
		for (int i = 0; i < branches.length; i++) {
			branches[i].zRot = baseZ(i) + Mth.sin(t * 0.07F + i * 1.3F) * 0.05F + rear * Mth.sin(t * 1.4F + i) * 0.06F
				+ shiver * Mth.sin(t * 1.4F + i) * 0.08F;
		}
		// Vines swing after the arms, lifting as it rears.
		for (int i = 0; i < vines.length; i++) {
			vines[i].xRot = Mth.sin(t * 0.09F + i * 0.9F) * 0.12F + Mth.sin(pos * 0.5F + i) * 0.2F * walk - 0.6F * rear - 0.5F * shiver;
		}
	}

	/** 0.13: lays a giant's moves over its walk, each weighted by how far it has eased in or out; returns how hard it shakes. */
	private float giant(MonsterRenderState state, float t) {
		rest.lean = rest.twist = rest.roll = rest.drop = rest.headX = rest.shiver = 0;
		rest.rx = rightArm.xRot;
		rest.ry = rightArm.yRot;
		rest.rz = rightArm.zRot;
		rest.lx = leftArm.xRot;
		rest.ly = leftArm.yRot;
		rest.lz = leftArm.zRot;
		rest.rLeg = rightLeg.xRot;
		rest.lLeg = leftLeg.xRot;
		now.copy(rest);
		int side = state.alt > 0.5F ? -1 : 1;
		for (int m = 1; m < GiantRules.Bramble.MOVES; m++) {
			float w = state.moves[m];
			if (w < 0.01F) {
				continue;
			}
			boolean current = m == state.move;
			pose.copy(rest);
			fill(pose, m, current ? state.since : 999, current ? state.tell : 0, side, t);
			now.mix(pose, Math.min(1, w));
		}
		float shiver = now.shiver + (state.roused ? 0.25F : 0);
		body.xRot += now.lean;
		body.yRot += now.twist;
		body.zRot += now.roll + Mth.sin(t * 1.9F) * 0.025F * shiver;
		body.y += now.drop;
		head.xRot += now.headX;
		rightArm.xRot = now.rx + Mth.sin(t * 2.3F) * 0.04F * shiver;
		rightArm.yRot = now.ry;
		rightArm.zRot = now.rz;
		leftArm.xRot = now.lx + Mth.sin(t * 2.1F + 1) * 0.04F * shiver;
		leftArm.yRot = now.ly;
		leftArm.zRot = now.lz;
		rightLeg.xRot = now.rLeg;
		leftLeg.xRot = now.lLeg;
		return shiver;
	}

	/**
	 * One move's pose, {@code s} ticks in (past its end once it's easing out), its tell {@code tell} ticks long; {@code side} is
	 * the arm a one-armed move swings (1 the right).
	 */
	private static void fill(Pose p, int move, float s, float tell, int side, float t) {
		switch (move) {
			case GiantRules.Bramble.STOMP -> {
				// A foot raised high, leaning back with its arms out; stamped down, sinking into it.
				if (s < tell) {
					float r = phase(s, tell);
					p.rLeg = Mth.lerp(r, p.rLeg, -1.1F);
					p.lean -= 0.25F * r;
					p.headX -= 0.3F * r;
					spread(p, r, 0.5F);
					p.shiver = 0.3F * r * r;
				} else {
					float k = phase(s - tell, 2);
					p.rLeg = Mth.lerp(k, -1.1F, 0.15F);
					p.lean += Mth.lerp(k, -0.25F, 0.3F);
					p.drop += 1.5F * k;
					p.headX += Mth.lerp(k, -0.3F, 0.15F);
					spread(p, 1, Mth.lerp(k, 0.5F, 0.25F));
					p.shiver = decay(s - tell, 10);
				}
			}
			case GiantRules.Bramble.SWEEP -> {
				// An arm drawn far back to one side, the trunk wound round after it; swept across its front.
				float k = s < tell ? 0 : phase(s - tell, GiantRules.Bramble.SWEEP_SWING);
				float r = s < tell ? phase(s, tell) : 1;
				p.twist += side * Mth.lerp(k, 0.55F, -0.65F) * r;
				p.lean += Mth.lerp(k, -0.1F, 0.2F) * r;
				p.drop += 0.8F * r;
				arm(p, side, r, -1.45F, Mth.lerp(k, 1.15F, -1.0F), 0.37F);
				arm(p, -side, r, 0.3F, 0, 0.12F);
			}
			case GiantRules.Bramble.HURL -> {
				// Digging in, then heaving overhead; thrown forward with its weight after it.
				if (s < tell) {
					float dig = phase(s, tell * 0.35F), heave = phase(s - tell * 0.35F, tell * 0.65F);
					p.lean += 0.6F * dig - 0.95F * heave;
					p.drop += 1.6F * dig * (1 - heave);
					p.headX -= 0.4F * heave;
					arms(p, dig, -0.75F, 0, 0.25F);
					arms(p, heave, -2.9F, 0, 0.2F);
					p.shiver = 0.4F * heave;
				} else {
					float k = phase(s - tell, 3);
					p.lean += Mth.lerp(k, -0.35F, 0.45F);
					p.drop += 0.8F * k;
					p.headX += Mth.lerp(k, -0.4F, 0.1F);
					arms(p, 1, Mth.lerp(k, -2.9F, -1.2F), 0, Mth.lerp(k, 0.2F, 0.1F));
				}
			}
			case GiantRules.Bramble.ROOTS -> {
				// Bent double with its arms plunged into the ground, trembling as the roots run.
				float r = phase(s, tell * 0.5F);
				p.lean += 0.85F * r;
				p.drop += 2.2F * r;
				p.headX += 0.2F * r;
				arms(p, r, -0.55F, 0, 0.25F);
				p.shiver = 0.6F * r;
			}
			case GiantRules.Bramble.GRAB -> {
				// Turned away with its right hand drawn back low; reaching out across its front, leaning after it.
				if (s < tell) {
					float r = phase(s, tell);
					p.twist += 0.35F * r;
					p.lean -= 0.1F * r;
					arm(p, 1, r, 0.5F, 0, 0.3F);
					arm(p, -1, r, -0.6F, 0, 0.12F);
				} else {
					float k = phase(s - tell, 3);
					p.twist += Mth.lerp(k, 0.35F, -0.3F);
					p.lean += Mth.lerp(k, -0.1F, 0.45F);
					p.drop += k;
					arm(p, 1, 1, Mth.lerp(k, 0.5F, -1.65F), -0.15F * k, Mth.lerp(k, 0.3F, 0.05F));
					arm(p, -1, 1, -0.6F, 0, 0.12F);
				}
			}
			case GiantRules.Bramble.CLUTCH -> {
				// Whoever it caught held up in its fist, squeezed now and then; flung away at the end.
				float in = phase(s, 4), squeeze = decay(s % GiantRules.Bramble.SQUEEZE_EVERY, 4) * in;
				float out = phase(s - (GiantRules.Bramble.CLUTCH_TICKS - 6), 4);
				p.lean -= 0.15F * in;
				arm(p, 1, in, -2.3F + 0.15F * squeeze, -0.1F, 0.2F);
				arm(p, -1, in, -0.5F, 0, 0.3F);
				p.shiver = 0.5F * squeeze;
				if (out > 0) {
					p.lean += 0.5F * out;
					p.twist -= 0.4F * out;
					arm(p, 1, out, -1.0F, 0, 0.1F);
				}
			}
			case GiantRules.Bramble.STUMBLE -> {
				// Its grip broken, it reels forward flailing, a foot thrown out to catch itself.
				float w = phase(s, 4) * (1 - phase(s - 14, 10));
				float flail = Mth.sin(t * 0.8F) * 0.4F;
				p.lean += 0.95F * w;
				p.drop += 2.5F * w;
				p.roll += 0.15F * w * Mth.sin(t * 0.5F);
				arm(p, 1, w, -1.2F + flail, 0, 0.4F);
				arm(p, -1, w, -1.2F - flail, 0, 0.4F);
				p.lLeg = Mth.lerp(w, p.lLeg, -0.6F);
				p.rLeg = Mth.lerp(w, p.rLeg, 0.4F);
				p.shiver = 0.3F * w;
			}
			case GiantRules.Bramble.HAMMER -> {
				// Both fists raised high together, leaning back, shaking as it holds; brought down into the floor.
				if (s < tell) {
					float r = phase(s, tell);
					p.lean -= 0.45F * r;
					p.headX -= 0.35F * r;
					arms(p, r, -2.95F, 0, -0.35F);
					p.shiver = Math.max(0, r - 0.7F) * 2;
				} else {
					float k = phase(s - tell, 2.5F);
					p.lean += Mth.lerp(k, -0.45F, 0.95F);
					p.drop += 2.5F * k;
					p.headX += Mth.lerp(k, -0.35F, 0.2F);
					arms(p, 1, Mth.lerp(k, -2.95F, -0.9F), 0, -0.35F);
					p.shiver = decay(s - tell - 2, 8);
				}
			}
			case GiantRules.Bramble.STUCK -> {
				// Bent over its fists stuck fast in the floor, hauling at them.
				float in = phase(s, 3), haul = Mth.sin(t * 0.5F) * 0.08F;
				p.lean += (0.95F + Mth.sin(t * 0.35F) * 0.07F) * in;
				p.drop += 2.5F * in;
				p.twist += Mth.sin(t * 0.2F) * 0.12F * in;
				arm(p, 1, in, -0.9F + haul, 0, -0.3F);
				arm(p, -1, in, -0.9F - haul, 0, -0.3F);
				p.shiver = 0.5F * in;
			}
			case GiantRules.Bramble.BRAMBLES -> {
				// Arms flung wide and high, head thrown back, quivering; thrust down to the ground as the thorns come.
				if (s < tell) {
					float r = phase(s, tell);
					p.lean -= 0.3F * r;
					p.headX -= 0.5F * r;
					arms(p, r, -1.9F, 0, 1.15F);
					p.shiver = r;
				} else {
					float k = phase(s - tell, 2);
					p.lean += Mth.lerp(k, -0.3F, 0.4F);
					p.drop += 1.5F * k;
					p.headX += Mth.lerp(k, -0.5F, 0.1F);
					arms(p, 1, Mth.lerp(k, -1.9F, -0.5F), 0, Mth.lerp(k, 1.15F, 0.55F));
					p.shiver = decay(s - tell, 8);
				}
			}
			case GiantRules.Bramble.KINDLE -> {
				// Hunched over its arms crossed on its chest, shaking harder and harder; flung open wide in the blaze.
				if (s < tell) {
					float r = phase(s, tell);
					p.lean += 0.45F * r;
					p.drop += 1.5F * r;
					p.headX += 0.2F * r;
					arms(p, r, -1.5F, -0.9F, 0.1F);
					p.shiver = r * r;
				} else {
					float k = phase(s - tell, 2);
					p.lean += Mth.lerp(k, 0.45F, -0.45F);
					p.drop += 1.5F * (1 - k);
					p.headX += Mth.lerp(k, 0.2F, -0.5F);
					arms(p, 1, -1.3F, Mth.lerp(k, -0.9F, 1.2F), Mth.lerp(k, 0.1F, 0.6F));
					p.shiver = decay(s - tell, 10);
				}
			}
			case GiantRules.Bramble.ROAR -> {
				// Reared back with its arms raised wide, roaring at the sky, shaking all over.
				float w = phase(s, 6) * (1 - phase(s - (GiantRules.Bramble.ROAR_TICKS - 10), 10));
				p.lean -= 0.55F * w;
				p.headX -= 0.7F * w;
				arms(p, w, -2.3F, 0, 0.9F);
				p.shiver = w;
			}
			default -> {}
		}
	}

	/** Eases one arm (1 the right) toward a pose given as the right arm's, mirrored for the left. */
	private static void arm(Pose p, int side, float w, float x, float y, float z) {
		if (side > 0) {
			p.rx = Mth.lerp(w, p.rx, x);
			p.ry = Mth.lerp(w, p.ry, y);
			p.rz = Mth.lerp(w, p.rz, z);
		} else {
			p.lx = Mth.lerp(w, p.lx, x);
			p.ly = Mth.lerp(w, p.ly, -y);
			p.lz = Mth.lerp(w, p.lz, -z);
		}
	}

	/** Eases both arms toward the same pose, mirrored. */
	private static void arms(Pose p, float w, float x, float y, float z) {
		arm(p, 1, w, x, y, z);
		arm(p, -1, w, x, y, z);
	}

	/** Eases both arms out to the sides, leaving their swing. */
	private static void spread(Pose p, float w, float z) {
		p.rz = Mth.lerp(w, p.rz, z);
		p.lz = Mth.lerp(w, p.lz, -z);
	}

	/** How far through {@code length} ticks {@code s} is, eased, 0 to 1 (1 if there's no length to it). */
	private static float phase(float s, float length) {
		if (length <= 0) {
			return 1;
		}
		float x = Mth.clamp(s / length, 0, 1);
		return x * x * (3 - 2 * x);
	}

	/** 1 the moment {@code s} reaches 0, falling to nothing over {@code length} ticks (0 before). */
	private static float decay(float s, float length) {
		return s < 0 ? 0 : Math.max(0, 1 - s / length);
	}

	/** 0.13: a giant's pose: the trunk's lean, twist, roll and sink and the head's nod over its walk, and its arms and legs outright. */
	private static final class Pose {
		float lean, twist, roll, drop, headX, shiver;
		float rx, ry, rz, lx, ly, lz, rLeg, lLeg;

		void copy(Pose o) {
			lean = o.lean;
			twist = o.twist;
			roll = o.roll;
			drop = o.drop;
			headX = o.headX;
			shiver = o.shiver;
			rx = o.rx;
			ry = o.ry;
			rz = o.rz;
			lx = o.lx;
			ly = o.ly;
			lz = o.lz;
			rLeg = o.rLeg;
			lLeg = o.lLeg;
		}

		void mix(Pose o, float w) {
			lean = Mth.lerp(w, lean, o.lean);
			twist = Mth.lerp(w, twist, o.twist);
			roll = Mth.lerp(w, roll, o.roll);
			drop = Mth.lerp(w, drop, o.drop);
			headX = Mth.lerp(w, headX, o.headX);
			shiver = Mth.lerp(w, shiver, o.shiver);
			rx = Mth.lerp(w, rx, o.rx);
			ry = Mth.lerp(w, ry, o.ry);
			rz = Mth.lerp(w, rz, o.rz);
			lx = Mth.lerp(w, lx, o.lx);
			ly = Mth.lerp(w, ly, o.ly);
			lz = Mth.lerp(w, lz, o.lz);
			rLeg = Mth.lerp(w, rLeg, o.rLeg);
			lLeg = Mth.lerp(w, lLeg, o.lLeg);
		}
	}

	/** Each branch's own lean (as built), for the sway to sit on. */
	private static float baseZ(int i) {
		return switch (i) {
			case 0 -> -0.55F;
			case 1 -> 0.55F;
			case 2 -> -0.15F;
			case 3 -> 0.25F;
			default -> -0.4F;
		};
	}
}
