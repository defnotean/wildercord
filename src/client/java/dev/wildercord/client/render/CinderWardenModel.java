package dev.wildercord.client.render;

import dev.wildercord.cast.CinderWardenRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The Cinder Warden: a hulking, hunched figure of black iron plates over magma, near three blocks
 * tall, a small helm sunk between great pauldrons, arms hanging almost to its knees in heavy
 * gauntlets, and a length of chain swinging from each wrist. Its core burns in the cracks of its
 * chest. Walking, it lumbers; casting, it brings its fists together before its core; between phases
 * it clutches its fists to its core, head thrown back and its chains thrashing, then heaves both
 * fists up over its head; dying, it sinks to its knees and slumps forward.
 *
 * <p>Each of its own moves has a pose of its own, eased in and out over its walk: both fists raised
 * and brought down; its chains let out and lying out to the ring they sweep; crouched low, dragging
 * its gauntlets; hunched round its core while steam blasts from its back; a chain whirled overhead
 * and thrown; its head thrown back to spew; its chest swelling, then its head thrust out to breathe;
 * curled tight round its heat and then flung wide; and left open, bent over its fists sunk in the
 * floor, wound in its own chain, slumped cold, or rocked back on its heels.</p>
 *
 * <p>Laid out on a 128x128 skin (see {@code cinder_warden_texture} in {@code tools/dungeon_art.py}):</p>
 * <pre>
 *   torso (0,0) 18x18x11     helm (60,0) 9x9x9         arm (98,0) 7x16x7
 *   pauldron (60,18) 9x6x10  leg (0,30) 8x20x8         belt (32,34) 17x5x10
 *   gauntlet (96,34) 8x7x8   chain (0,60) 2x12x2
 * </pre>
 */
public class CinderWardenModel extends EntityModel<DungeonBossRenderState> {
	/** How long its chain hangs, in blocks, unstretched. */
	private static final float CHAIN = 0.75F;

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightChain;
	private final ModelPart leftChain;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;
	private final Pose rest = new Pose();
	private final Pose now = new Pose();
	private final Pose pose = new Pose();

	public CinderWardenModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.rightArm = body.getChild("right_arm");
		this.leftArm = body.getChild("left_arm");
		this.rightChain = rightArm.getChild("chain");
		this.leftChain = leftArm.getChild("chain");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// The torso, pivoting at the hips, with its belt; a small helm sunk low and forward between the shoulders.
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-9.0F, -18.0F, -5.5F, 18.0F, 18.0F, 11.0F)
				.texOffs(32, 34).addBox(-8.5F, -2.0F, -5.0F, 17.0F, 5.0F, 10.0F),
			PartPose.offset(0.0F, 5.0F, 0.0F));
		body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(60, 0).addBox(-4.5F, -8.0F, -4.5F, 9.0F, 9.0F, 9.0F),
			PartPose.offset(0.0F, -17.0F, -3.0F));
		body.addOrReplaceChild("right_pauldron", CubeListBuilder.create().texOffs(60, 18).addBox(-4.5F, -3.0F, -5.0F, 9.0F, 6.0F, 10.0F),
			PartPose.offsetAndRotation(-9.5F, -17.0F, 0.0F, 0.0F, 0.0F, 0.25F));
		body.addOrReplaceChild("left_pauldron", CubeListBuilder.create().texOffs(60, 18).mirror().addBox(-4.5F, -3.0F, -5.0F, 9.0F, 6.0F, 10.0F),
			PartPose.offsetAndRotation(9.5F, -17.0F, 0.0F, 0.0F, 0.0F, -0.25F));

		// Long arms in heavy gauntlets, a chain hanging from each wrist.
		PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create()
				.texOffs(98, 0).addBox(-3.5F, -1.0F, -3.5F, 7.0F, 16.0F, 7.0F)
				.texOffs(96, 34).addBox(-4.0F, 14.0F, -4.0F, 8.0F, 7.0F, 8.0F),
			PartPose.offset(-12.0F, -14.0F, 0.0F));
		rightArm.addOrReplaceChild("chain", CubeListBuilder.create().texOffs(0, 60).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F),
			PartPose.offset(0.0F, 20.0F, 2.5F));
		PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create()
				.texOffs(98, 0).mirror().addBox(-3.5F, -1.0F, -3.5F, 7.0F, 16.0F, 7.0F)
				.texOffs(96, 34).mirror().addBox(-4.0F, 14.0F, -4.0F, 8.0F, 7.0F, 8.0F),
			PartPose.offset(12.0F, -14.0F, 0.0F));
		leftArm.addOrReplaceChild("chain", CubeListBuilder.create().texOffs(0, 60).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F),
			PartPose.offset(0.0F, 20.0F, 2.5F));

		// Thick legs, planted wide.
		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 30).addBox(-4.0F, -1.0F, -4.0F, 8.0F, 20.0F, 8.0F),
			PartPose.offset(-4.5F, 5.0F, 0.0F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 30).mirror().addBox(-4.0F, -1.0F, -4.0F, 8.0F, 20.0F, 8.0F),
			PartPose.offset(4.5F, 5.0F, 0.0F));
		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(DungeonBossRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float c = state.casting;
		float s = state.slamming;
		float sh = state.shifting;
		float walk = Math.min(1, state.walkAnimationSpeed);
		float pos = state.walkAnimationPos;
		float die = Math.min(1, state.deathTime / 30.0F);
		float slump = Math.max(0, state.deathTime - 30) / 30.0F;

		// A heavy, rolling gait; it breathes, slowly, even standing.
		float breathe = Mth.sin(t * 0.06F) * 0.03F;
		rightLeg.xRot = Mth.cos(pos * 0.45F) * 0.55F * walk;
		leftLeg.xRot = -Mth.cos(pos * 0.45F) * 0.55F * walk;
		body.zRot = Mth.cos(pos * 0.45F) * 0.05F * walk + sh * Mth.sin(t * 1.7F) * 0.05F;
		// Hunched forward; leaning back to swing both fists up; reared back and shaking between phases.
		body.xRot = 0.18F + breathe - 0.35F * s - 0.3F * sh + sh * Mth.sin(t * 1.3F) * 0.05F;
		head.yRot = state.yRot * Mth.DEG_TO_RAD * (1 - sh);
		head.xRot = Mth.clamp(state.xRot, -30, 30) * Mth.DEG_TO_RAD * (1 - sh) - 0.75F * sh - 0.2F * s;

		// Arms: swinging with the gait; casting, fists brought together before its core; between phases, clutched to its core;
		// a blow wound up over its head.
		float swing = Mth.cos(pos * 0.45F) * 0.45F * walk;
		poseArm(rightArm, 1, -swing, c, s, sh, t);
		poseArm(leftArm, -1, swing, c, s, sh, t + 11);

		// The chains swing after the arms, and with the step; between phases they thrash as the heat runs up them.
		rightChain.xRot = -rightArm.xRot * 0.6F + Mth.sin(t * 0.12F) * 0.15F + Mth.sin(pos * 0.45F) * 0.25F * walk;
		leftChain.xRot = -leftArm.xRot * 0.6F + Mth.sin(t * 0.12F + 1.7F) * 0.15F - Mth.sin(pos * 0.45F) * 0.25F * walk;
		rightChain.zRot = Mth.sin(t * 0.09F) * 0.1F + sh * Mth.sin(t * 0.8F) * 0.9F;
		leftChain.zRot = -Mth.sin(t * 0.09F + 0.8F) * 0.1F - sh * Mth.sin(t * 0.8F + 1.3F) * 0.9F;

		moves(state, t, 1 - die);

		// Sinking to its knees as it dies, its arms hanging limp.
		if (die > 0) {
			body.xRot += die * 0.25F + slump * 0.45F;
			body.y += die * 7.0F;
			rightLeg.y = 5.0F + die * 7.0F;
			leftLeg.y = 5.0F + die * 7.0F;
			rightLeg.xRot = -1.3F * die;
			leftLeg.xRot = -1.3F * die;
			rightArm.xRot = Mth.lerp(die, rightArm.xRot, 0.2F);
			rightArm.zRot = Mth.lerp(die, rightArm.zRot, 0.05F);
			leftArm.xRot = Mth.lerp(die, leftArm.xRot, 0.2F);
			leftArm.zRot = Mth.lerp(die, leftArm.zRot, -0.05F);
		}
	}

	private static void poseArm(ModelPart arm, int side, float swing, float c, float s, float sh, float t) {
		float idle = Mth.sin(t * 0.05F) * 0.04F;
		float xRot = Mth.lerp(c, swing + idle, -1.25F + Mth.sin(t * 0.4F) * 0.05F);
		float zRot = Mth.lerp(c, 0.08F * side, -0.45F * side);
		xRot = Mth.lerp(sh, xRot, -1.35F + Mth.sin(t * 1.1F) * 0.1F);
		zRot = Mth.lerp(sh, zRot, -0.7F * side);
		xRot = Mth.lerp(s, xRot, -2.9F);
		zRot = Mth.lerp(s, zRot, -0.15F * side);
		arm.xRot = xRot;
		arm.yRot = 0;
		arm.zRot = zRot;
	}

	/** Lays its own moves over its walk, each as far as it has eased in or out; {@code alive} fades them as it dies. */
	private void moves(DungeonBossRenderState state, float t, float alive) {
		rest.lean = rest.twist = rest.roll = rest.drop = rest.headX = rest.shiver = 0;
		rest.rx = rightArm.xRot;
		rest.ry = rightArm.yRot;
		rest.rz = rightArm.zRot;
		rest.lx = leftArm.xRot;
		rest.ly = leftArm.yRot;
		rest.lz = leftArm.zRot;
		rest.rLeg = rightLeg.xRot;
		rest.lLeg = leftLeg.xRot;
		rest.rcx = rightChain.xRot;
		rest.rcz = rightChain.zRot;
		rest.lcx = leftChain.xRot;
		rest.lcz = leftChain.zRot;
		rest.rLen = rest.lLen = 1;
		now.copy(rest);
		float reach = state.extra[0];
		float spin = state.extra[1];
		for (int m = 1; m <= CinderWardenRules.MOVES; m++) {
			float w = state.moves[m] * alive;
			if (w < 0.01F) {
				continue;
			}
			boolean current = m == state.move;
			pose.copy(rest);
			fill(pose, m, current ? state.since : 999, current ? state.tell : 0, t, reach, spin);
			now.mix(pose, Math.min(1, w));
		}
		body.xRot += now.lean;
		body.yRot += now.twist;
		body.zRot += now.roll + Mth.sin(t * 1.9F) * 0.03F * now.shiver;
		body.y += now.drop;
		head.xRot += now.headX;
		rightArm.xRot = now.rx + Mth.sin(t * 2.3F) * 0.05F * now.shiver;
		rightArm.yRot = now.ry;
		rightArm.zRot = now.rz;
		leftArm.xRot = now.lx + Mth.sin(t * 2.1F + 1) * 0.05F * now.shiver;
		leftArm.yRot = now.ly;
		leftArm.zRot = now.lz;
		rightLeg.xRot = now.rLeg;
		leftLeg.xRot = now.lLeg;
		rightChain.xRot = now.rcx;
		rightChain.zRot = now.rcz;
		rightChain.yScale = now.rLen;
		leftChain.xRot = now.lcx;
		leftChain.zRot = now.lcz;
		leftChain.yScale = now.lLen;
	}

	/**
	 * One move's pose, {@code s} ticks in (past its end once it's easing out), its tell {@code tell} ticks long; {@code reach} is
	 * how far its chain is let out, in blocks, and {@code spin} the way it turns (1 or -1).
	 */
	private static void fill(Pose p, int move, float s, float tell, float t, float reach, float spin) {
		switch (move) {
			case CinderWardenRules.HAMMERFALL -> {
				if (s < tell) {
					// Both fists clasped high over its head, leaning back into it, trembling as it holds them there.
					float r = phase(s, tell * 0.7F);
					p.rx = Mth.lerp(r, p.rx, -3.0F);
					p.lx = Mth.lerp(r, p.lx, -3.0F);
					p.rz = Mth.lerp(r, p.rz, -0.2F);
					p.lz = Mth.lerp(r, p.lz, 0.2F);
					p.lean -= 0.1F * r;
					p.headX -= 0.35F * r;
					p.shiver = 0.4F * r * r;
				} else {
					// Brought down together into the floor before it, its whole weight behind them.
					float k = phase(s - tell, 2);
					p.rx = Mth.lerp(k, -3.0F, -1.35F);
					p.lx = Mth.lerp(k, -3.0F, -1.35F);
					p.rz = Mth.lerp(k, -0.2F, -0.25F);
					p.lz = Mth.lerp(k, 0.2F, 0.25F);
					p.lean += Mth.lerp(k, -0.1F, 0.75F);
					p.drop += 3.0F * k;
					p.headX += Mth.lerp(k, -0.35F, -0.3F);
					p.rLeg = Mth.lerp(k, p.rLeg, -0.45F);
					p.lLeg = Mth.lerp(k, p.lLeg, 0.3F);
					p.shiver = decay(s - tell, 8);
				}
			}
			case CinderWardenRules.SWEEP -> {
				// Both arms held out, its chains let out to lie along the floor out to the ring; once it turns, it leans into the
				// turn and its chains trail after.
				float r = phase(s, Math.max(1, tell * 0.5F));
				float k = phase(s - tell + 2, 2);
				p.rx = Mth.lerp(r, p.rx, -0.2F);
				p.lx = Mth.lerp(r, p.lx, -0.2F);
				p.rz = Mth.lerp(r, p.rz, 1.35F);
				p.lz = Mth.lerp(r, p.lz, -1.35F);
				lay(p, r, reach);
				p.twist -= spin * 0.35F * k;
				p.lean += 0.1F * r;
				p.rLeg = Mth.lerp(r, p.rLeg, -0.3F);
				p.lLeg = Mth.lerp(r, p.lLeg, 0.3F);
			}
			case CinderWardenRules.SEAM -> {
				if (s < tell) {
					// Crouched low, its gauntlets dug into the floor behind it, scraping.
					float r = phase(s, tell * 0.6F);
					p.lean += 0.55F * r;
					p.drop += 3.0F * r;
					p.rx = Mth.lerp(r, p.rx, -0.35F);
					p.lx = Mth.lerp(r, p.lx, -0.35F);
					p.rz = Mth.lerp(r, p.rz, 0.3F);
					p.lz = Mth.lerp(r, p.lz, -0.3F);
					p.rLeg = Mth.lerp(r, p.rLeg, -0.5F);
					p.lLeg = Mth.lerp(r, p.lLeg, 0.35F);
					p.headX -= 0.45F * r;
					p.shiver = 0.25F * r;
				} else {
					// Wrenched up and forward, tearing the seam open before it.
					float k = phase(s - tell, 2);
					p.lean += Mth.lerp(k, 0.55F, 0.05F);
					p.drop += Mth.lerp(k, 3.0F, 0.5F);
					p.rx = Mth.lerp(k, -0.35F, -2.1F);
					p.lx = Mth.lerp(k, -0.35F, -2.1F);
					p.rz = Mth.lerp(k, 0.3F, -0.1F);
					p.lz = Mth.lerp(k, -0.3F, 0.1F);
					p.rLeg = Mth.lerp(k, -0.5F, -0.3F);
					p.lLeg = Mth.lerp(k, 0.35F, 0.2F);
					p.headX += Mth.lerp(k, -0.45F, -0.2F);
					p.shiver = decay(s - tell, 8);
				}
			}
			case CinderWardenRules.VENT -> {
				// Hunched round its core, arms wrapped close, while its back plates swell; it jerks as the steam blasts out behind.
				float r = phase(s, tell * 0.6F);
				float b = phase(s - tell, 2) * (1 - phase(s - tell - CinderWardenRules.VENT_BLAST, 4));
				p.lean += 0.55F * r + 0.2F * b;
				p.drop += 1.5F * r;
				p.headX += 0.4F * r - 0.5F * b;
				p.rx = Mth.lerp(r, p.rx, -1.45F);
				p.lx = Mth.lerp(r, p.lx, -1.45F);
				p.ry = Mth.lerp(r, p.ry, -0.6F);
				p.ly = Mth.lerp(r, p.ly, 0.6F);
				p.rz = Mth.lerp(r, p.rz, -0.2F);
				p.lz = Mth.lerp(r, p.lz, 0.2F);
				p.shiver = 0.3F * r + 0.7F * b;
			}
			case CinderWardenRules.HAUL -> haul(p, s, tell, reach);
			case CinderWardenRules.SLAG -> {
				if (s < tell) {
					// Its head thrown back and its chest heaving as the slag rises in its throat.
					float r = phase(s, tell * 0.7F);
					p.headX -= 0.9F * r;
					p.lean -= 0.25F * r;
					p.rx = Mth.lerp(r, p.rx, 0.35F);
					p.lx = Mth.lerp(r, p.lx, 0.35F);
					p.rz = Mth.lerp(r, p.rz, 0.35F);
					p.lz = Mth.lerp(r, p.lz, -0.35F);
					p.shiver = 0.35F * r;
				} else {
					// Thrown forward as it spews.
					float k = phase(s - tell, 2);
					p.headX += Mth.lerp(k, -0.9F, 0.3F);
					p.lean += Mth.lerp(k, -0.25F, 0.35F);
					p.rx = Mth.lerp(k, 0.35F, -0.3F);
					p.lx = Mth.lerp(k, 0.35F, -0.3F);
					p.rz = Mth.lerp(k, 0.35F, 0.2F);
					p.lz = Mth.lerp(k, -0.35F, -0.2F);
					p.shiver = decay(s - tell, 6);
				}
			}
			default -> fillLate(p, move, s, tell, t);
		}
	}

	/**
	 * Its chains let out to lie along the floor {@code reach} blocks from it, from wrists held out about two blocks from its
	 * middle and a block and a half above the floor, its arms a little under level; {@code r} how far into the pose it is.
	 */
	private static void lay(Pose p, float r, float reach) {
		float out = 2.0F, high = 1.5F, below = Mth.HALF_PI - 1.35F;
		float h = Math.max(0.3F, reach - out);
		float down = (float) Math.atan2(high, h) - below;
		float len = 1 + (Math.max(1, (float) Math.hypot(h, high) / CHAIN) - 1) * Mth.clamp(reach / out, 0, 1);
		p.rcx = Mth.lerp(r, p.rcx, 0);
		p.lcx = Mth.lerp(r, p.lcx, 0);
		p.rcz = Mth.lerp(r, p.rcz, -down);
		p.lcz = Mth.lerp(r, p.lcz, down);
		p.rLen = Mth.lerp(r, 1, len);
		p.lLen = Mth.lerp(r, 1, len);
	}

	/** The haul: a chain whirled overhead, faster and faster, then thrown out down its lane and dragged back in, braced. */
	private static void haul(Pose p, float s, float tell, float reach) {
		float whirl = Math.min(s, tell);
		float a = whirl * whirl * 0.06F;
		if (s < tell) {
			float r = phase(s, tell * 0.4F);
			p.rx = Mth.lerp(r, p.rx, -2.8F);
			p.rz = Mth.lerp(r, p.rz, 0.25F);
			p.rcx = Mth.lerp(r, p.rcx, Mth.cos(a) * 1.3F);
			p.rcz = Mth.lerp(r, p.rcz, Mth.sin(a) * 1.3F);
			p.lx = Mth.lerp(r, p.lx, -0.5F);
			p.lz = Mth.lerp(r, p.lz, -0.5F);
			p.twist -= 0.3F * r;
			p.lean -= 0.1F * r;
			return;
		}
		// Thrown: its arm out straight down the lane, the chain run out after it, angled down to where it bites.
		float k = phase(s - tell, 2);
		float back = phase(s - tell - CinderWardenRules.HAUL_CAST, 4);
		float h = Math.max(0.3F, reach - 1.3F);
		float len = 1 + (Math.max(1, (float) Math.hypot(h, 1.0F) / CHAIN) - 1) * Mth.clamp(reach / 1.3F, 0, 1);
		p.rx = Mth.lerp(k, -2.8F, -1.5F) + 0.4F * back;
		p.rz = Mth.lerp(k, 0.25F, 0.05F);
		p.rcx = Mth.lerp(k, Mth.cos(a) * 1.3F, (float) Math.atan2(1.0F, h));
		p.rcz = Mth.lerp(k, Mth.sin(a) * 1.3F, 0);
		p.rLen = Mth.lerp(k, 1, len);
		p.lx = Mth.lerp(k, -0.5F, -0.3F);
		p.lz = Mth.lerp(k, -0.5F, -0.4F);
		p.twist += Mth.lerp(k, -0.3F, 0.3F) - 0.2F * back;
		p.lean += Mth.lerp(k, -0.1F, 0.2F) - 0.3F * back;
		p.rLeg = Mth.lerp(k, p.rLeg, -0.4F);
		p.lLeg = Mth.lerp(k, p.lLeg, 0.35F);
	}

	/** The breath, the flare and the moves that leave it open. */
	private static void fillLate(Pose p, int move, float s, float tell, float t) {
		switch (move) {
			case CinderWardenRules.BELLOWS -> {
				float breath = CinderWardenRules.BREATH_TICKS;
				if (s < tell) {
					// Its chest swelling as it draws the hall's heat in, its arms spread like bellows.
					float r = phase(s, tell);
					p.lean -= 0.35F * r;
					p.headX -= 0.5F * r;
					p.rx = Mth.lerp(r, p.rx, 0.3F);
					p.lx = Mth.lerp(r, p.lx, 0.3F);
					p.rz = Mth.lerp(r, p.rz, 0.7F);
					p.lz = Mth.lerp(r, p.lz, -0.7F);
					p.shiver = 0.2F * r;
				} else if (s < tell + breath) {
					// Its head thrust out, breathing, braced against the force of it.
					float k = phase(s - tell, 3);
					p.lean += Mth.lerp(k, -0.35F, 0.25F);
					p.headX += Mth.lerp(k, -0.5F, -0.25F);
					p.rx = Mth.lerp(k, 0.3F, -0.6F);
					p.lx = Mth.lerp(k, 0.3F, -0.6F);
					p.rz = Mth.lerp(k, 0.7F, 0.45F);
					p.lz = Mth.lerp(k, -0.7F, -0.45F);
					p.rLeg = Mth.lerp(k, p.rLeg, -0.35F);
					p.lLeg = Mth.lerp(k, p.lLeg, 0.3F);
					p.shiver = 0.35F;
				} else {
					// Out of breath, bent over and heaving, its plates hanging open.
					float e = phase(s - tell - breath, 6);
					p.lean += Mth.lerp(e, 0.25F, 0.45F) + Mth.sin(t * 0.5F) * 0.04F;
					p.headX += Mth.lerp(e, -0.25F, 0.3F);
					p.rx = Mth.lerp(e, -0.6F, -0.3F);
					p.lx = Mth.lerp(e, -0.6F, -0.3F);
					p.rz = Mth.lerp(e, 0.45F, 0.15F);
					p.lz = Mth.lerp(e, -0.45F, -0.15F);
				}
			}
			case CinderWardenRules.FLARE -> {
				float gather = CinderWardenRules.FLARE_GATHER;
				if (s < tell + gather) {
					// Curled tight round its core, arms crossed over it, sinking lower and shaking harder as the heat gathers.
					float r = phase(s, tell);
					float g = Mth.clamp((s - tell) / gather, 0, 1);
					p.lean += (0.6F + 0.15F * g) * r;
					p.drop += (3.0F + 1.5F * g) * r;
					p.headX += 0.5F * r;
					p.rx = Mth.lerp(r, p.rx, -1.6F);
					p.lx = Mth.lerp(r, p.lx, -1.6F);
					p.ry = Mth.lerp(r, p.ry, -0.8F);
					p.ly = Mth.lerp(r, p.ly, 0.8F);
					p.rz = Mth.lerp(r, p.rz, -0.2F);
					p.lz = Mth.lerp(r, p.lz, 0.2F);
					p.rLeg = Mth.lerp(r, p.rLeg, -0.3F);
					p.lLeg = Mth.lerp(r, p.lLeg, 0.2F);
					p.shiver = 0.2F * r + 0.8F * g;
				} else {
					// Burst: thrown wide open, head back, roaring its heat out across the hall.
					float k = phase(s - tell - gather, 2);
					p.lean += Mth.lerp(k, 0.75F, -0.45F);
					p.drop += Mth.lerp(k, 4.5F, 0);
					p.headX += Mth.lerp(k, 0.5F, -0.7F);
					p.rx = Mth.lerp(k, -1.6F, -0.4F);
					p.lx = Mth.lerp(k, -1.6F, -0.4F);
					p.ry = Mth.lerp(k, -0.8F, 0);
					p.ly = Mth.lerp(k, 0.8F, 0);
					p.rz = Mth.lerp(k, -0.2F, 1.6F);
					p.lz = Mth.lerp(k, 0.2F, -1.6F);
					p.shiver = decay(s - tell - gather, 10);
				}
			}
			case CinderWardenRules.STUCK -> {
				// Bent over its fists, sunk to the wrists in the floor, and late on heaving at them, shaking, until they tear free.
				float end = CinderWardenRules.STUCK_TICKS;
				float h = phase(s - (end - 10), 8);
				p.lean += Mth.lerp(h, 0.75F, 0.35F);
				p.drop += Mth.lerp(h, 3.0F, 1.0F);
				p.headX -= 0.3F;
				p.rx = Mth.lerp(h, -1.35F, -0.6F);
				p.lx = Mth.lerp(h, -1.35F, -0.6F);
				p.rz = -0.25F;
				p.lz = 0.25F;
				p.rLeg = -0.45F;
				p.lLeg = 0.3F;
				p.shiver = s > end - 16 && s < end - 6 ? 0.7F : 0.1F;
			}
			case CinderWardenRules.TANGLED -> {
				// Its arms bound to its sides by its own chain, wrenching this way and that to throw it off.
				p.rx = 0.05F;
				p.lx = 0.05F;
				p.rz = -0.1F;
				p.lz = 0.1F;
				p.rcx = -0.3F;
				p.lcx = -0.3F;
				p.rcz = -1.5F;
				p.lcz = 1.5F;
				p.twist += Mth.sin(t * 0.5F) * 0.3F;
				p.roll += Mth.sin(t * 0.37F) * 0.08F;
				p.lean += 0.1F;
				p.headX += Mth.sin(t * 0.6F) * 0.15F;
				p.shiver = 0.5F;
			}
			case CinderWardenRules.SPENT -> {
				// Slumped and dark, gone cold and brittle after its heat has burst out of it.
				float r = phase(s, 6);
				p.lean += (0.45F + Mth.sin(t * 0.15F) * 0.05F) * r;
				p.drop += 2.5F * r;
				p.headX += 0.6F * r;
				p.rx = Mth.lerp(r, p.rx, -0.55F);
				p.lx = Mth.lerp(r, p.lx, -0.55F);
				p.rz = Mth.lerp(r, p.rz, 0.1F);
				p.lz = Mth.lerp(r, p.lz, -0.1F);
			}
			case CinderWardenRules.REEL -> {
				// Rocked back on its heels, arms flung out, its wind-up knocked out of it.
				float k = phase(s, 3) * (1 - phase(s - CinderWardenRules.REEL_TICKS * 0.5F, CinderWardenRules.REEL_TICKS * 0.5F));
				p.lean -= 0.55F * k;
				p.headX -= 0.6F * k;
				p.rx = Mth.lerp(k, p.rx, -1.4F);
				p.lx = Mth.lerp(k, p.lx, -1.4F);
				p.rz = Mth.lerp(k, p.rz, 0.7F);
				p.lz = Mth.lerp(k, p.lz, -0.7F);
				p.rLeg = Mth.lerp(k, p.rLeg, -0.35F);
				p.lLeg = Mth.lerp(k, p.lLeg, 0.4F);
				p.shiver = decay(s, 14);
			}
			default -> {
			}
		}
	}

	/** Eases from 0 to 1 over {@code length} ticks from {@code s} = 0. */
	private static float phase(float s, float length) {
		if (length <= 0) {
			return 1;
		}
		float x = Mth.clamp(s / length, 0, 1);
		return x * x * (3 - 2 * x);
	}

	/** Falls from 1 to 0 over {@code length} ticks from {@code s} = 0, and 0 before. */
	private static float decay(float s, float length) {
		return s < 0 ? 0 : Math.max(0, 1 - s / length);
	}

	/** Where each part is, past the walk: the body's lean, twist, roll and drop, the head, the arms, legs and chains. */
	private static final class Pose {
		float lean, twist, roll, drop, headX, shiver;
		float rx, ry, rz, lx, ly, lz;
		float rLeg, lLeg;
		float rcx, rcz, lcx, lcz, rLen, lLen;

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
			rcx = o.rcx;
			rcz = o.rcz;
			lcx = o.lcx;
			lcz = o.lcz;
			rLen = o.rLen;
			lLen = o.lLen;
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
			rcx = Mth.lerp(w, rcx, o.rcx);
			rcz = Mth.lerp(w, rcz, o.rcz);
			lcx = Mth.lerp(w, lcx, o.lcx);
			lcz = Mth.lerp(w, lcz, o.lcz);
			rLen = Mth.lerp(w, rLen, o.rLen);
			lLen = Mth.lerp(w, lLen, o.lLen);
		}
	}
}
