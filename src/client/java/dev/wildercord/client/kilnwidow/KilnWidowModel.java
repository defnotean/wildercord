package dev.wildercord.client.kilnwidow;

import dev.wildercord.Wildercord;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

import static dev.wildercord.world.kilnwidow.KilnWidowRules.*;

public final class KilnWidowModel extends EntityModel<KilnWidowRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("kiln_widow"), "main");
	private final ModelPart hearth, dome, chimney, slagCore, damperLeft, damperRight, legFl, legFr, legBl, legBr;

	public KilnWidowModel(ModelPart root) {
		super(root);
		hearth = root.getChild("hearth");
		dome = hearth.getChild("dome");
		chimney = dome.getChild("chimney");
		slagCore = dome.getChild("slag_core");
		damperLeft = dome.getChild("damper_left");
		damperRight = dome.getChild("damper_right");
		legFl = hearth.getChild("leg_fl");
		legFr = hearth.getChild("leg_fr");
		legBl = hearth.getChild("leg_bl");
		legBr = hearth.getChild("leg_br");
	}

	public static LayerDefinition createBodyLayer() {
		var mesh = new MeshDefinition();
		var root = mesh.getRoot();
		root.addOrReplaceChild("plinth_base", CubeListBuilder.create()
			.texOffs(0, 0).addBox(-16, -4, -16, 32, 4, 32)
			.texOffs(0, 36).addBox(-12, -8, -12, 24, 4, 24), PartPose.offset(0, 24, 0));
		var hearth = root.addOrReplaceChild("hearth", CubeListBuilder.create()
			.texOffs(0, 64).addBox(-10, -14, -10, 20, 6, 20), PartPose.offset(0, 24, 0));
		var dome = hearth.addOrReplaceChild("dome", CubeListBuilder.create()
			.texOffs(0, 90).addBox(-8, -14, -8, 16, 14, 16), PartPose.offset(0, -14, 0));
		dome.addOrReplaceChild("chimney", CubeListBuilder.create()
			.texOffs(64, 90).addBox(-5, -22, -5, 10, 8, 10), PartPose.offset(0, 0, 0));
		dome.addOrReplaceChild("slag_core", CubeListBuilder.create()
			.texOffs(128, 48).addBox(-4, -10, -10, 8, 8, 4), PartPose.offset(0, 0, 0));
		dome.addOrReplaceChild("damper_left", CubeListBuilder.create()
			.texOffs(128, 0).addBox(0, -12, -6, 5, 12, 12), PartPose.offset(8, 0, 0));
		dome.addOrReplaceChild("damper_right", CubeListBuilder.create()
			.texOffs(128, 0).mirror().addBox(-5, -12, -6, 5, 12, 12), PartPose.offset(-8, 0, 0));
		hearth.addOrReplaceChild("leg_fl", CubeListBuilder.create()
			.texOffs(164, 0).addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(10, -12, -8));
		hearth.addOrReplaceChild("leg_fr", CubeListBuilder.create()
			.texOffs(164, 0).mirror().addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(-10, -12, -8));
		hearth.addOrReplaceChild("leg_bl", CubeListBuilder.create()
			.texOffs(164, 0).addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(10, -12, 8));
		hearth.addOrReplaceChild("leg_br", CubeListBuilder.create()
			.texOffs(164, 0).mirror().addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(-10, -12, 8));
		return LayerDefinition.create(mesh, 256, 128);
	}

	@Override public void setupAnim(KilnWidowRenderState s) {
		super.setupAnim(s);
		float t = s.ageInTicks, amp = s.second ? 1.4F : 1;
		boolean dying = s.deathTime > 0;
		// Idle: the furnace breathes, the chimney turns and puffs, the dampers flutter out of step, and the legs shift their weight in pairs.
		hearth.y += Mth.sin(t * .06F) * .4F * amp;
		hearth.zRot = Mth.sin(t * .031F) * .015F * amp;
		dome.y += Mth.sin(t * .06F - .7F) * .3F; dome.xRot = Mth.sin(t * .045F + .5F) * .02F;
		chimney.yRot = t * .02F; chimney.zRot = Mth.sin(t * .05F - .9F) * .04F; chimney.xRot = Mth.cos(t * .043F - 1.4F) * .03F;
		slagCore.zRot = Mth.sin(t * .09F) * .12F; slagCore.z += Mth.sin(t * .13F) * .3F * amp;
		damperLeft.yRot = -.1F + Mth.sin(t * .05F + .3F) * .05F; damperRight.yRot = .1F - Mth.sin(t * .05F + .9F) * .05F;
		damperLeft.zRot = Mth.sin(t * .08F - .6F) * .05F * amp; damperRight.zRot = -Mth.sin(t * .08F - 1.2F) * .05F * amp;
		float sway = Mth.sin(t * .07F), sway2 = Mth.sin(t * .07F + 1.9F);
		legFl.zRot = -.22F + sway * .05F; legFr.zRot = .22F - sway2 * .05F;
		legBl.zRot = -.22F - sway2 * .05F; legBr.zRot = .22F + sway * .05F;
		legFl.xRot = Mth.sin(t * .11F) * .03F; legFr.xRot = Mth.sin(t * .11F + 1.6F) * .03F;
		legBl.xRot = Mth.sin(t * .11F + 3.1F) * .03F; legBr.xRot = Mth.sin(t * .11F + 4.7F) * .03F;
		if (s.second) { chimney.y -= .8F; legFl.zRot -= .1F; legFr.zRot += .1F; legBl.zRot -= .1F; legBr.zRot += .1F; }

		if (s.move != IDLE && !dying) {
			float T = Math.max(1, tell(s.move)), R = Math.max(8, recovery(s.move)), el = Math.max(0, s.elapsed), dt = el - T;
			float wind = smooth(el / T), sk = burst(dt), rec = dt > 4 ? smooth((dt - 4) / (R - 4)) : 0, w = 1 - rec;
			float tw = wind * (1 - Mth.clamp(sk, 0, 1)) * w, st = sk * w, jolt = jolt(dt) * w;
			float quiver = el < T ? Mth.sin(el * 2.6F) * smooth((el - (T - 10)) / 10) : 0;
			float stag = dt > 1 ? (float) Math.exp(-dt * .1F) * Mth.sin(dt * .55F) * w : 0;
			float slump = dt > 2 ? smooth((dt - 2) / 6) * w : 0;
			if (s.move == DRAFT) {
				// Dampers flung open and the whole furnace drawing itself up on stretched legs, chimney reaching; then everything slams down, the chimney venting a recoil.
				dome.y += -2.2F * tw + 3F * jolt; chimney.y += -2.2F * tw + 2.5F * st; chimney.xRot += quiver * .06F + stag * .1F;
				damperLeft.zRot += -.6F * tw + .45F * burst(dt - 1) * w; damperRight.zRot += .6F * tw - .45F * burst(dt - 1) * w;
				hearth.y += -1.6F * tw + 2.2F * jolt;
				legFl.zRot += -.18F * tw; legFr.zRot += .18F * tw; legBl.zRot += -.18F * tw; legBr.zRot += .18F * tw;
				slagCore.z += quiver * .5F; slagCore.zRot += quiver * .2F;
				dome.xRot += .05F * slump;
			} else if (s.move == SLAG) {
				// Dome rocked back on its haunches so the core looks at the sky and glows, the front legs bracing; then it pitches forward hard, spitting the core out.
				dome.xRot += .45F * tw - .75F * st + .1F * stag;
				slagCore.z += -2.4F * tw + 1.5F * st; slagCore.y += -1.2F * tw; slagCore.zRot += quiver * .3F;
				hearth.xRot += -.1F * tw + .12F * st; hearth.y += -.6F * tw + 1.5F * jolt;
				legFl.xRot += -.4F * tw + .5F * st; legFr.xRot += -.4F * tw + .5F * st;
				legBl.xRot += .25F * tw - .2F * st; legBr.xRot += .25F * tw - .2F * st;
				chimney.xRot += -.2F * tw + .35F * st; damperLeft.zRot += -.2F * tw; damperRight.zRot += .2F * tw;
				dome.y += quiver * .4F;
			} else if (s.move == SWEEP) {
				// Dampers swung back like scythes while the furnace twists and squats and the legs gather; then it whips round, the dampers cutting an arc and the legs scrabbling to catch up.
				dome.yRot += .55F * tw - .85F * st + .15F * stag; hearth.yRot += .22F * tw - .4F * burst(dt - 1) * w;
				damperLeft.yRot += -.75F * tw + 1.2F * st; damperRight.yRot += .75F * tw - 1.2F * st;
				damperLeft.zRot += -.15F * tw; damperRight.zRot += .15F * tw;
				hearth.y += 1.4F * tw - .8F * jolt; dome.y += .8F * tw;
				legFl.xRot += -.4F * tw + .35F * burst(dt - 2) * w; legFr.xRot += -.4F * tw - .35F * burst(dt - 3) * w;
				legBl.xRot += .3F * tw + .3F * burst(dt - 4) * w; legBr.xRot += .3F * tw - .3F * burst(dt - 5) * w;
				legFl.zRot += -.2F * tw; legFr.zRot += .2F * tw; legBl.zRot += -.2F * tw; legBr.zRot += .2F * tw;
				chimney.yRot += -.4F * tw + .6F * burst(dt - 2) * w; chimney.zRot += quiver * .08F;
			}
			if (s.move != SWEEP) { hearth.zRot += stag * .05F; legFl.xRot += stag * .1F; legBr.xRot -= stag * .1F; }
		}
		if (s.hurt > 0 && !dying) {
			// A hit rocks the furnace back; the dampers clatter and the legs jolt.
			float h = s.hurt;
			dome.xRot += -.3F * h; hearth.y += .9F * h; chimney.xRot += Mth.sin(h * 12) * .15F * h;
			damperLeft.zRot += -.4F * h; damperRight.zRot += .4F * h; slagCore.zRot += Mth.sin(h * 22) * .3F * h;
			legFl.zRot += -.12F * h; legFr.zRot += .12F * h; legBl.zRot += -.12F * h; legBr.zRot += .12F * h;
		}
		if (dying) {
			// Dying: it shudders and flares; then the legs buckle and splay and the dome slumps; then the whole furnace settles low while the chimney tips over.
			float d = s.deathTime, s1 = smooth(d / 6), s2 = smooth((d - 5) / 9), s3 = smooth((d - 10) / 10);
			float shudder = Mth.sin(d * 2.2F) * (1 - s2);
			hearth.zRot += shudder * .05F * s1; slagCore.zRot += shudder * .5F * s1; slagCore.z += -1.5F * s1 + 2 * s3;
			legFl.zRot += -.5F * s2; legFr.zRot += .5F * s2; legBl.zRot += -.4F * s2; legBr.zRot += .4F * s2;
			legFl.xRot += .3F * s2; legFr.xRot += -.2F * s2; legBl.xRot += -.25F * s2; legBr.xRot += .3F * s2;
			dome.xRot += -.2F * s1 + .75F * s2; dome.zRot += .25F * s3;
			damperLeft.zRot += -.3F * s1 + .9F * s2; damperRight.zRot += .3F * s1 - .9F * s2;
			chimney.zRot += .5F * s2 + .35F * s3; chimney.xRot += .3F * s3;
			hearth.y += Math.min(7, 5 * s2 + 3 * s3);
		}
	}

	private static float smooth(float x) {
		x = Mth.clamp(x, 0, 1);
		return x * x * (3 - 2 * x);
	}
	/** 0 until the strike, then a two-tick snap that overshoots by a fifth and settles. */
	private static float burst(float dt) {
		if (dt <= 0) return 0;
		float k = Mth.clamp(dt / 2, 0, 1);
		return 1 - (1 - k) * (1 - k) + .2F * (float) Math.exp(-dt * .18F) * Mth.sin(dt * .75F);
	}
	/** An impact that lands in a tick and rings down. */
	private static float jolt(float dt) { return dt > 0 ? Mth.clamp(dt, 0, 1) * (float) Math.exp(-dt * .2F) * Mth.cos(dt * .55F) : 0; }
}
