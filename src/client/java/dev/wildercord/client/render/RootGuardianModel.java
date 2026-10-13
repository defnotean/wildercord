package dev.wildercord.client.render;

import dev.wildercord.cast.RootGuardianRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/** A knotted trunk, leafy crown, branching arms and opening heart: its own articulated creature. */
public final class RootGuardianModel extends EntityModel<DungeonBossRenderState> {
	private final ModelPart trunk, head, left, right, legLeft, legRight, heart, crown, branchLeft, branchRight;
	private final ModelPart[] bindings = new ModelPart[3];
	private final Pose rest = new Pose(), now = new Pose(), pose = new Pose();
	public RootGuardianModel(ModelPart root) {
		super(root); trunk = root.getChild("trunk"); head = trunk.getChild("head"); crown = head.getChild("crown");
		left = trunk.getChild("left"); right = trunk.getChild("right"); legLeft = trunk.getChild("leg_left"); legRight = trunk.getChild("leg_right"); heart = trunk.getChild("heart");
		branchLeft = left.getChild("branch"); branchRight = right.getChild("branch");
		for(int i=0;i<3;i++) bindings[i]=trunk.getChild("binding"+i);
	}
	public static LayerDefinition createLayer() {
		var mesh = new MeshDefinition();
		var trunk = mesh.getRoot().addOrReplaceChild("trunk", CubeListBuilder.create().texOffs(0,0).addBox(-6,-14,-5,12,18,10), PartPose.offset(0,8,0));
		var head = trunk.addOrReplaceChild("head", CubeListBuilder.create().texOffs(48,0).addBox(-5,-10,-5,10,10,10), PartPose.offset(0,-14,0));
		head.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(48,24).addBox(-8,-4,-7,16,4,14), PartPose.offset(0,-10,0));
		for (int side : new int[] {-1,1}) {
			var arm = trunk.addOrReplaceChild(side < 0 ? "right" : "left", CubeListBuilder.create().texOffs(24,32).addBox(-2.5F,-2,-2.5F,5,18,5), PartPose.offset(side*8,-11,0));
			arm.addOrReplaceChild("branch", CubeListBuilder.create().texOffs(24,32).addBox(-1,0,-1,2,8,2), PartPose.offsetAndRotation(side*2,4,0,0,0,-side*.7F));
			trunk.addOrReplaceChild(side < 0 ? "leg_right" : "leg_left", CubeListBuilder.create().texOffs(0,32).addBox(-2.5F,0,-3,5,12,6), PartPose.offset(side*3.5F,4,0));
		}
		trunk.addOrReplaceChild("heart", CubeListBuilder.create().texOffs(96,0).addBox(-3,-4,-1,6,8,1), PartPose.offset(0,-6,-5));
		for(int i=0;i<3;i++) trunk.addOrReplaceChild("binding"+i,CubeListBuilder.create().texOffs(48,24).addBox(-7,-1,-.5F,14,2,1),PartPose.offsetAndRotation(0,-9+i*3,-6,0,0,(i-1)*.15F));
		return LayerDefinition.create(mesh,128,64);
	}
	@Override public void setupAnim(DungeonBossRenderState s) {
		super.setupAnim(s);
		float t = s.ageInTicks, sh = s.shifting, slam = s.slamming, die = s.deathTime > 0 ? Math.min(1, s.deathTime / 60F) : 0;
		for(int i=0;i<3;i++) bindings[i].visible=i<s.rootBindings;
		head.yRot = s.yRot * Mth.DEG_TO_RAD * (1 - sh); head.xRot = s.xRot * Mth.DEG_TO_RAD * (1 - sh);
		float sway = Mth.sin(t*.045F)*.08F;
		left.zRot = -.12F-sway-s.casting*.7F; right.zRot = .12F+sway+s.casting*.7F;
		left.xRot = -s.casting*.65F; right.xRot = -s.casting*.65F;
		legLeft.xRot = Mth.cos(s.walkAnimationPos*.6F)*s.walkAnimationSpeed*.65F;
		legRight.xRot = -legLeft.xRot;
		// Its crown rustles while its pollen hangs in the air.
		crown.zRot = sway + s.extra[1] * Mth.sin(t * .5F) * .08F;
		heart.z = -5-s.exposed*.8F; heart.xScale = heart.yScale = 1+s.exposed*(.08F+.04F*Mth.sin(t*.3F));
		// Between phases it sinks its legs in and throws its arms up, arched back and trembling as fresh roots knot over it; it rears
		// up on one leg as the ring gathers, and stamps it down.
		if (sh > 0) {
			float shake = Mth.sin(t * 1.3F) * .06F * sh;
			trunk.xRot = -.35F * sh - .15F * slam + shake;
			trunk.zRot = Mth.sin(t * .9F) * .05F * sh;
			head.xRot -= .6F * sh;
			right.xRot = Mth.lerp(sh, right.xRot, -2.8F + Mth.sin(t * 1.1F) * .1F); left.xRot = Mth.lerp(sh, left.xRot, -2.8F + Mth.sin(t * 1.1F + 1) * .1F);
			right.zRot = Mth.lerp(sh, right.zRot, .6F + .3F * slam); left.zRot = Mth.lerp(sh, left.zRot, -.6F - .3F * slam);
			legRight.xRot = Mth.lerp(sh, legRight.xRot, -1.1F * slam); legLeft.xRot = Mth.lerp(sh, legLeft.xRot, .15F * slam);
			crown.zRot += Mth.sin(t * 1.4F) * .2F * sh;
			heart.z -= .6F * sh;
		}
		moves(s, t, 1 - die);
		if (die > 0) trunk.xRot = Mth.lerp(die, trunk.xRot, .8F);
	}

	/** Lays its own moves over its walk, each as far as it has eased in or out; {@code alive} fades them as it dies. */
	private void moves(DungeonBossRenderState s, float t, float alive) {
		rest.lean = trunk.xRot; rest.twist = trunk.yRot; rest.roll = trunk.zRot; rest.drop = 0; rest.headX = 0; rest.crownZ = 0; rest.shiver = 0;
		rest.rx = right.xRot; rest.ry = right.yRot; rest.rz = right.zRot; rest.lx = left.xRot; rest.ly = left.yRot; rest.lz = left.zRot;
		rest.rLeg = legRight.xRot; rest.lLeg = legLeft.xRot; rest.rLegZ = legRight.zRot; rest.lLegZ = legLeft.zRot; rest.heart = 0; rest.branch = 0;
		now.copy(rest);
		boolean any = false;
		for (int m = 1; m <= RootGuardianRules.POSES; m++) {
			float w = s.moves[m] * alive;
			if (w < .01F) continue;
			boolean current = m == s.move;
			pose.copy(rest);
			fill(pose, m, current ? s.since : 999, current ? s.tell : 0, t, s.extra[0] >= 0 ? 1 : -1, s.extra[2], s.phase);
			now.mix(pose, Math.min(1, w));
			any = true;
		}
		if (!any) return;
		trunk.xRot = now.lean; trunk.yRot = now.twist; trunk.zRot = now.roll + Mth.sin(t * 1.9F) * .04F * now.shiver;
		trunk.y += now.drop; legRight.y -= now.drop; legLeft.y -= now.drop;
		head.xRot += now.headX; crown.zRot += now.crownZ;
		right.xRot = now.rx + Mth.sin(t * 2.3F) * .06F * now.shiver; right.yRot = now.ry; right.zRot = now.rz;
		left.xRot = now.lx + Mth.sin(t * 2.1F + 1) * .06F * now.shiver; left.yRot = now.ly; left.zRot = now.lz;
		legRight.xRot = now.rLeg; legLeft.xRot = now.lLeg; legRight.zRot = now.rLegZ; legLeft.zRot = now.lLegZ;
		heart.z -= now.heart;
		branchRight.zRot += now.branch; branchLeft.zRot -= now.branch;
	}

	/**
	 * One move's pose, {@code s} ticks in (past its end once it's easing out), its tell {@code tell} ticks long; {@code side} is the
	 * arm it lashes with (1 its right, -1 its left), {@code grip} how hard its root has hold of someone, {@code phase} its phase.
	 */
	private static void fill(Pose p, int move, float s, float tell, float t, int side, float grip, int phase) {
		boolean p3 = phase >= 3, mine = side > 0;
		switch (move) {
			case RootGuardianRules.SPEARS -> {
				// Bent low, both arms sunk to the elbow in the floor before it, feeling about; then wrenched up as the roots burst out.
				// In its last phase it sinks them again and wrenches a second time.
				float r = phase(s, tell * .7F), up = phase(s - tell, 2);
				if (p3) {
					float second = tell + RootGuardianRules.SPEARS_SECOND;
					up = Math.max(up * (1 - phase(s - tell - 3, RootGuardianRules.SPEARS_SECOND - 5)), phase(s - second, 2));
				}
				float down = r * (1 - up);
				p.lean += .55F * down + .1F * up;
				p.drop += 2.5F * down;
				p.headX += .3F * down - .3F * up;
				arms(p, down, -.75F, 0, -.12F);
				arms(p, up, -1.9F, 0, .45F);
				leg(p, true, down, -.4F);
				leg(p, false, down, .3F);
				p.shiver = .35F * down + decay(s - tell, 6);
			}
			case RootGuardianRules.LASH -> {
				// The whip arm drawn back and out over its shoulder, its trunk wound round after it; then swept across the front, the
				// trunk unwinding after it. In its last phase the other arm winds back at once and sweeps back the other way.
				boolean again = p3 && s > tell + 1;
				float blow = again ? tell + RootGuardianRules.LASH_BACK : tell, from = again ? tell + 1 : 0;
				float r = phase(s - from, (blow - from) * .7F), k = phase(s - blow, 2);
				p.twist += side * (.5F * r * (1 - k) - .55F * k);
				p.lean += .15F * k - .05F * r * (1 - k);
				p.headX -= .15F * r * (1 - k);
				arm(p, mine, r * (1 - k), .7F, -.4F, 1.25F);
				arm(p, mine, k, -1.35F, .9F, .2F);
				p.branch += .5F * r - .7F * k;
				// The arm that swung first still easing back from across its front.
				if (again) arm(p, !mine, 1 - phase(s - from, 4), -1.35F, .9F, .2F);
				p.shiver = .25F * r * (1 - k) + decay(s - blow, 5) * .5F;
			}
			case RootGuardianRules.SEEDS -> {
				// Arms raised and spread under its crown, which swells and shakes as the pods fatten; then thrown forward to fling them.
				float r = phase(s, tell * .7F), k = phase(s - tell, 2);
				p.lean += -.2F * r * (1 - k) + .35F * k;
				p.headX += -.45F * r * (1 - k) + .2F * k;
				p.crownZ += Mth.sin(t * 1.5F) * .25F * r * (1 - k);
				arms(p, r * (1 - k), -2.4F, 0, .55F);
				arms(p, k, -1.2F, 0, .25F);
				p.branch += .4F * r;
				p.shiver = .2F * r * (1 - k) + decay(s - tell, 6) * .4F;
			}
			case RootGuardianRules.SAP -> {
				// Arched back, arms thrown behind it and its heart swelling out of its bark; then lurching forward as the sap gushes.
				float r = phase(s, tell * .7F), k = phase(s - tell, 2), held = 1 - phase(s - tell - RootGuardianRules.SAP_RUN, 6);
				p.lean += -.45F * r * (1 - k) + .4F * k * held;
				p.headX += -.5F * r * (1 - k) + .25F * k * held;
				arms(p, r * (1 - k), .55F, 0, .5F);
				arms(p, k * held, .25F, 0, .9F);
				p.heart += 1.4F * r * (k > 0 ? held : 1);
				p.shiver = .3F * r * (1 - k) + .5F * k * held;
			}
			case RootGuardianRules.CREEP -> {
				// A leg lifted and stamped down, both arms reaching down after the root it sends under the floor; once the root has
				// someone, it hauls them in hand over hand.
				float r = phase(s, tell * .6F), lift = phase(s, tell * .4F) * (1 - phase(s - tell * .55F, 2));
				leg(p, mine, lift, -1.0F);
				leg(p, mine, r * (1 - lift), .2F);
				p.lean += .3F * r * (1 - grip) - .2F * grip;
				p.drop += 1.0F * r;
				arms(p, r, -1.1F, 0, .1F);
				float haul = Mth.sin(t * .6F) * .55F * grip;
				p.rx = Mth.lerp(grip, p.rx, -.7F + haul);
				p.lx = Mth.lerp(grip, p.lx, -.7F - haul);
				p.shiver = .2F * r + .3F * grip + decay(s - tell * .55F, 6) * .6F;
			}
			case RootGuardianRules.THORNS -> {
				// Hunched and bristling, arms held out and down; then flung open as the thorns burst out. In its last phase it hunches
				// again at once and bursts a second time.
				float r = phase(s, tell * .7F), k = phase(s - tell, 2);
				if (p3) {
					float second = tell + RootGuardianRules.THORNS_RUN + RootGuardianRules.THORNS_SECOND;
					float re = phase(s - tell - RootGuardianRules.THORNS_RUN, RootGuardianRules.THORNS_SECOND * .7F);
					k = Math.max(k * (1 - re), phase(s - second, 2));
				}
				float hunch = r * (1 - k);
				p.lean += .45F * hunch - .15F * k;
				p.drop += 2F * hunch;
				p.headX += .3F * hunch - .4F * k;
				arms(p, hunch, -.3F, 0, .9F);
				arms(p, k, -.6F, 0, 1.5F);
				p.rLegZ += .15F * hunch; p.lLegZ -= .15F * hunch;
				p.branch += .6F * hunch + .4F * k;
				p.shiver = .5F * hunch + decay(s - tell, 6) * .5F;
			}
			default -> fillLate(p, move, s, tell, t, side, phase);
		}
	}

	/** The rest of its moves' poses, and the poses it's left in when a move goes wrong for it. */
	private static void fillLate(Pose p, int move, float s, float tell, float t, int side, int phase) {
		boolean mine = side > 0;
		switch (move) {
			case RootGuardianRules.UPROOT -> {
				// Leaning back while it tears one leg then the other free of the floor; then bent low and running, arms swept back. In
				// its last phase it digs in, tears free again and runs a second time.
				float k = s - tell, run = RootGuardianRules.CHARGE_TICKS, re = RootGuardianRules.RETELL;
				boolean tearing = k < 0 || phase >= 3 && k >= run && k < run + re;
				float r = k < 0 ? phase(s, tell * .5F) : 1;
				if (tearing) {
					float in = k < 0 ? s : k - run;
					p.lean -= .15F * r;
					p.headX -= .2F * r;
					p.rLeg = Mth.lerp(r, p.rLeg, Mth.sin(in * .9F) * .7F - .2F);
					p.lLeg = Mth.lerp(r, p.lLeg, -Mth.sin(in * .9F) * .7F - .2F);
					arms(p, r, -.5F, 0, .7F);
					p.shiver = .45F * r;
				} else {
					float go = phase(k - (k >= run ? run + re : 0), 2);
					p.lean += .55F * go;
					p.headX -= .35F * go;
					arms(p, go, .8F, 0, .35F);
					p.rLeg = Mth.lerp(go, p.rLeg, Mth.cos(t * 1.2F) * .9F);
					p.lLeg = Mth.lerp(go, p.lLeg, -Mth.cos(t * 1.2F) * .9F);
					p.shiver = .2F * go;
				}
			}
			case RootGuardianRules.POLLEN -> {
				// Arms half raised and its whole crown thrashing side to side; then one great shudder as the cloud bursts out of it.
				float r = phase(s, tell * .6F), k = decay(s - tell, 8);
				p.crownZ += Mth.sin(t * 1.2F) * .45F * r * (1 - phase(s - tell, 6));
				p.roll += Mth.sin(t * .9F) * .12F * r * (1 - phase(s - tell, 6));
				p.headX -= .3F * r;
				arms(p, r, -1.6F + Mth.sin(t * .8F) * .15F, 0, .8F);
				p.branch += .5F * r;
				p.shiver = .3F * r + k;
			}
			case RootGuardianRules.CANOPY -> {
				// Arms raised high and spread, leaning back and trembling as the boughs creak; then brought down hard and held there
				// while the boughs come crashing down.
				float r = phase(s, tell * .7F), k = phase(s - tell, 2);
				p.lean += -.3F * r * (1 - k) + .4F * k;
				p.drop += 1.5F * k;
				p.headX += -.6F * r * (1 - k) + .2F * k;
				arms(p, r * (1 - k), -2.9F, 0, .35F);
				arms(p, k, -.4F, 0, .3F);
				p.branch += .5F * r * (1 - k);
				p.shiver = .45F * r * r * (1 - k) + decay(s - tell, 8) * .6F;
			}
			case RootGuardianRules.SNAGGED -> {
				// Its whip sunk in the floor across its front, it tugs and heaves at it; at last it wrenches it out.
				float r = phase(s, 3), free = phase(s - RootGuardianRules.SNAG_TICKS + 6, 4), tug = Math.max(0, Mth.sin(s * .45F)) * (1 - free);
				arm(p, mine, r, -.75F + .3F * tug, .9F, .1F);
				p.lean += (.35F - .15F * tug) * r * (1 - free);
				p.twist -= side * .4F * r * (1 - free);
				p.headX += .3F * r;
				leg(p, mine, r, .35F);
				leg(p, !mine, r, -.3F);
				p.shiver = .35F * r * (1 - free) + decay(s - RootGuardianRules.SNAG_TICKS + 6, 5);
			}
			case RootGuardianRules.TANGLED -> {
				// Its legs bound together by its own roots, sagging and swaying as it strains against them, its arms hanging.
				float r = phase(s, 4);
				p.rLegZ = Mth.lerp(r, p.rLegZ, -.18F); p.lLegZ = Mth.lerp(r, p.lLegZ, .18F);
				p.rLeg = Mth.lerp(r, p.rLeg, 0); p.lLeg = Mth.lerp(r, p.lLeg, 0);
				p.roll += Mth.sin(t * .3F) * .1F * r;
				p.lean += .25F * r;
				p.drop += 1F * r;
				p.headX += .4F * r;
				arms(p, r, .1F + Mth.sin(t * .25F) * .08F, 0, .05F);
				p.shiver = .3F * r * Math.max(0, Mth.sin(t * .2F));
			}
			case RootGuardianRules.STAGGER -> {
				// Wrenching its stamped leg back out of the floor in jerks, leaning back and its arms flailing for balance.
				float r = phase(s, 3) * (1 - phase(s - RootGuardianRules.STAGGER_TICKS + 6, 6)), jerk = Math.max(0, Mth.sin(s * .6F));
				leg(p, mine, r, -.3F - .5F * jerk);
				p.lean -= (.15F + .1F * jerk) * r;
				p.roll += side * .1F * r;
				arms(p, r, -.6F + Mth.sin(t * .7F) * .3F, 0, .9F);
				p.shiver = .4F * r;
			}
			case RootGuardianRules.REEL -> {
				// Thrown back on its heels, arms flung wide and its bare heart thrust out, slowly gathering itself again.
				float k = phase(s, 3) * (1 - phase(s - RootGuardianRules.REEL_TICKS * .5F, RootGuardianRules.REEL_TICKS * .5F));
				p.lean -= .55F * k;
				p.headX -= .6F * k;
				arms(p, k, -1.2F, 0, 1.2F);
				p.rLeg = Mth.lerp(k, p.rLeg, -.35F); p.lLeg = Mth.lerp(k, p.lLeg, .4F);
				p.heart += .8F * k;
				p.branch += .5F * k;
				p.shiver = decay(s, 14);
			}
			default -> {
			}
		}
	}

	/** Eases from 0 to 1 over {@code length} ticks from {@code s} = 0. */
	private static float phase(float s, float length) {
		if (length <= 0) return 1;
		float x = Mth.clamp(s / length, 0, 1);
		return x * x * (3 - 2 * x);
	}

	/** Falls from 1 to 0 over {@code length} ticks from {@code s} = 0, and 0 before. */
	private static float decay(float s, float length) { return s < 0 ? 0 : Math.max(0, 1 - s / length); }

	/** Eases one arm, its right if {@code right}, toward a pose by {@code w}: raised {@code x}, turned {@code y}, held out {@code out}. */
	private static void arm(Pose p, boolean right, float w, float x, float y, float out) {
		if (right) { p.rx = Mth.lerp(w, p.rx, x); p.ry = Mth.lerp(w, p.ry, y); p.rz = Mth.lerp(w, p.rz, out); }
		else { p.lx = Mth.lerp(w, p.lx, x); p.ly = Mth.lerp(w, p.ly, -y); p.lz = Mth.lerp(w, p.lz, -out); }
	}

	/** Eases both arms alike toward a pose by {@code w}. */
	private static void arms(Pose p, float w, float x, float y, float out) { arm(p, true, w, x, y, out); arm(p, false, w, x, y, out); }

	/** Eases one leg, its right if {@code right}, toward {@code x} by {@code w}. */
	private static void leg(Pose p, boolean right, float w, float x) {
		if (right) p.rLeg = Mth.lerp(w, p.rLeg, x); else p.lLeg = Mth.lerp(w, p.lLeg, x);
	}

	/** Where each part is, past the walk: the trunk's lean, twist, roll and drop, the head and crown, the arms, legs and heart. */
	private static final class Pose {
		float lean, twist, roll, drop, headX, crownZ, shiver, rx, ry, rz, lx, ly, lz, rLeg, lLeg, rLegZ, lLegZ, heart, branch;
		void copy(Pose o) {
			lean = o.lean; twist = o.twist; roll = o.roll; drop = o.drop; headX = o.headX; crownZ = o.crownZ; shiver = o.shiver;
			rx = o.rx; ry = o.ry; rz = o.rz; lx = o.lx; ly = o.ly; lz = o.lz; rLeg = o.rLeg; lLeg = o.lLeg; rLegZ = o.rLegZ; lLegZ = o.lLegZ;
			heart = o.heart; branch = o.branch;
		}
		void mix(Pose o, float w) {
			lean = Mth.lerp(w, lean, o.lean); twist = Mth.lerp(w, twist, o.twist); roll = Mth.lerp(w, roll, o.roll); drop = Mth.lerp(w, drop, o.drop);
			headX = Mth.lerp(w, headX, o.headX); crownZ = Mth.lerp(w, crownZ, o.crownZ); shiver = Mth.lerp(w, shiver, o.shiver);
			rx = Mth.lerp(w, rx, o.rx); ry = Mth.lerp(w, ry, o.ry); rz = Mth.lerp(w, rz, o.rz); lx = Mth.lerp(w, lx, o.lx); ly = Mth.lerp(w, ly, o.ly); lz = Mth.lerp(w, lz, o.lz);
			rLeg = Mth.lerp(w, rLeg, o.rLeg); lLeg = Mth.lerp(w, lLeg, o.lLeg); rLegZ = Mth.lerp(w, rLegZ, o.rLegZ); lLegZ = Mth.lerp(w, lLegZ, o.lLegZ);
			heart = Mth.lerp(w, heart, o.heart); branch = Mth.lerp(w, branch, o.branch);
		}
	}
}
