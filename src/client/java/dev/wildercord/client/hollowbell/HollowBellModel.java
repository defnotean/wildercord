package dev.wildercord.client.hollowbell;

import dev.wildercord.Wildercord;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

import static dev.wildercord.world.hollowbell.HollowBellRules.*;

public final class HollowBellModel extends EntityModel<HollowBellRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("hollow_bell"), "main");
	private final ModelPart yoke, mantle, skirt, clapper, shutterLeft, shutterRight, crown, gauge;

	public HollowBellModel(ModelPart root) {
		super(root);
		yoke = root.getChild("yoke");
		mantle = yoke.getChild("mantle");
		skirt = mantle.getChild("skirt");
		clapper = mantle.getChild("clapper");
		shutterLeft = mantle.getChild("shutter_left");
		shutterRight = mantle.getChild("shutter_right");
		crown = yoke.getChild("crown");
		gauge = mantle.getChild("gauge");
	}

	public static LayerDefinition createBodyLayer() {
		var mesh = new MeshDefinition();
		var root = mesh.getRoot();
		root.addOrReplaceChild("plinth_base", CubeListBuilder.create()
			.texOffs(0, 0).addBox(-16, -4, -16, 32, 4, 32)
			.texOffs(0, 36).addBox(-12, -8, -12, 24, 4, 24), PartPose.offset(0, 24, 0));
		var yoke = root.addOrReplaceChild("yoke", CubeListBuilder.create()
			.texOffs(0, 64).addBox(-11, -30, -5, 22, 6, 10), PartPose.offset(0, 14, 0));
		yoke.addOrReplaceChild("crown", CubeListBuilder.create()
			.texOffs(40, 94).addBox(-8, -36, -8, 16, 6, 16), PartPose.offset(0, 0, 0));
		var mantle = yoke.addOrReplaceChild("mantle", CubeListBuilder.create()
			.texOffs(0, 80).addBox(-8, 0, -8, 16, 14, 16), PartPose.offset(0, -24, 0));
		mantle.addOrReplaceChild("skirt", CubeListBuilder.create()
			.texOffs(64, 64).addBox(-10, 14, -10, 20, 8, 20), PartPose.offset(0, 0, 0));
		mantle.addOrReplaceChild("clapper", CubeListBuilder.create()
			.texOffs(150, 0).addBox(-2, 4, -2, 4, 20, 4)
			.texOffs(166, 0).addBox(-4, 22, -4, 8, 7, 8), PartPose.offset(0, 0, 0));
		mantle.addOrReplaceChild("shutter_left", CubeListBuilder.create()
			.texOffs(128, 0).addBox(0, 2, -6, 5, 18, 12), PartPose.offset(9, 0, 0));
		mantle.addOrReplaceChild("shutter_right", CubeListBuilder.create()
			.texOffs(128, 0).mirror().addBox(-5, 2, -6, 5, 18, 12), PartPose.offset(-9, 0, 0));
		mantle.addOrReplaceChild("gauge", CubeListBuilder.create()
			.texOffs(128, 48).addBox(-4, 4, -10, 8, 8, 4), PartPose.offset(0, 0, 0));
		return LayerDefinition.create(mesh, 256, 128);
	}

	@Override public void setupAnim(HollowBellRenderState s) {
		super.setupAnim(s);
		float t = s.ageInTicks, amp = s.second ? 1.35F : 1;
		boolean dying = s.deathTime > 0;
		// Idle: the bell hangs from its yoke and swings a little, the clapper trailing it, the skirt and shutters trailing both.
		yoke.y += Mth.sin(t * .06F) * .45F * amp;
		crown.yRot = t * .025F; crown.zRot = Mth.sin(t * .05F + .3F) * .03F; if (s.second) { crown.y -= .6F; mantle.xRot += -.04F; }
		mantle.xRot += Mth.sin(t * .04F) * .03F * amp; mantle.zRot = Mth.sin(t * .033F + 1) * .025F * amp;
		clapper.xRot = Mth.sin(t * .07F - .8F) * .08F - mantle.xRot * .5F; clapper.zRot = Mth.cos(t * .05F - .5F) * .06F;
		skirt.xRot = Mth.sin(t * .05F - .4F) * .02F; skirt.zRot = Mth.sin(t * .043F - 1) * .02F;
		gauge.zRot = Mth.sin(t * .09F) * .12F;
		shutterLeft.yRot = -.12F + Mth.sin(t * .05F + .4F) * .04F; shutterRight.yRot = .12F - Mth.sin(t * .05F + .4F) * .04F;
		shutterLeft.zRot = Mth.sin(t * .06F - .5F) * .03F; shutterRight.zRot = -Mth.sin(t * .06F - .2F) * .03F;

		if (s.move != IDLE && !dying) {
			float T = Math.max(1, tell(s.move)), R = Math.max(8, recovery(s.move)), el = Math.max(0, s.elapsed), dt = el - T;
			float wind = smooth(el / T), sk = burst(dt), rec = dt > 4 ? smooth((dt - 4) / (R - 4)) : 0, w = 1 - rec;
			float tw = wind * (1 - Mth.clamp(sk, 0, 1)) * w, st = sk * w, jolt = jolt(dt) * w;
			float quiver = el < T ? Mth.sin(el * 2.4F) * smooth((el - (T - 10)) / 10) * amp : 0;
			float stag = dt > 1 ? (float) Math.exp(-dt * .1F) * Mth.sin(dt * .55F) * w : 0;
			float slump = dt > 2 ? smooth((dt - 2) / 6) * w : 0;
			// A struck bell keeps ringing: a fast shiver in the skirt that rings down.
			float ring = dt > 0 ? (float) Math.exp(-dt * .1F) * Mth.sin(dt * 2.1F) * w : 0;
			crown.yRot += quiver * .15F;
			if (s.move == TOLL) {
				// Bell swung right back on its yoke with the clapper hanging forward and the crown lifted; then swung through hard, the clapper cracking the far wall, and the skirt rings.
				mantle.xRot += -.5F * tw + .5F * st + Mth.sin(dt * .9F) * .06F * (float) Math.exp(-Math.max(0, dt) * .1F) * (dt > 0 ? 1 : 0) * w;
				clapper.xRot += .65F * tw - 1F * burst(dt - 1) * w;
				skirt.y -= 1.2F * tw; skirt.xRot += ring * .05F; skirt.zRot += ring * .05F;
				yoke.y += -tw + 2.5F * jolt; crown.y -= 1.5F * tw + jolt;
				shutterLeft.zRot += -.15F * tw + .3F * st; shutterRight.zRot += .15F * tw - .3F * st;
				mantle.zRot += ring * .02F;
			} else if (s.move == CLAPPER) {
				// Bell tilted to one side, the clapper hauled up inside it and swung out, the gauge hunting for its mark; then the clapper plunges out through the mouth, the bell squatting on its yoke.
				clapper.y += -3.5F * tw + 9.5F * st; clapper.zRot += .28F * tw + Mth.sin(el * .5F) * .1F * tw + stag * .4F;
				clapper.xRot += stag * .3F - .2F * tw;
				mantle.zRot += -.16F * tw + .1F * st; mantle.xRot += .08F * slump;
				yoke.zRot += -.05F * tw; yoke.y += 2.5F * jolt;
				gauge.zRot += Mth.sin(el * .9F) * .5F * tw; gauge.z -= tw;
				skirt.xRot += ring * .03F; crown.y += jolt;
			} else if (s.move == CHORD) {
				// Both shutters thrown wide, the gauge thrust out and the whole bell humming and twisting; then the shutters slam shut, rattling, with a ring through the bell.
				shutterLeft.zRot += -.58F * tw + .35F * st + Mth.sin(dt * 1.9F) * .12F * (float) Math.exp(-Math.max(0, dt) * .15F) * (dt > 0 ? 1 : 0) * w;
				shutterRight.zRot += .58F * tw - .35F * st - Mth.sin(dt * 1.9F) * .12F * (float) Math.exp(-Math.max(0, dt) * .15F) * (dt > 0 ? 1 : 0) * w;
				gauge.z += -1.6F * tw + 1F * st;
				mantle.zRot += Mth.sin(el * 2.2F) * .04F * wind * w + ring * .03F; mantle.yRot += -.25F * tw + .25F * st; mantle.xRot += -.1F * tw;
				crown.yRot += Mth.sin(el * .9F) * .2F * tw; yoke.y += -1.5F * tw + 1.5F * jolt;
				clapper.zRot += ring * .15F; skirt.zRot += ring * .04F;
			}
		}
		if (s.hurt > 0 && !dying) {
			// A blow rings it: the bell rocks, the clapper jangles and the skirt and shutters shiver.
			float h = s.hurt;
			mantle.xRot += -.25F * h; yoke.y += .8F * h; clapper.xRot += Mth.sin(h * 14) * .4F * h;
			skirt.xRot += Mth.sin(h * 30) * .05F * h; skirt.zRot += Mth.cos(h * 26) * .05F * h;
			shutterLeft.zRot += -.3F * h; shutterRight.zRot += .3F * h; crown.yRot += .3F * h;
		}
		if (dying) {
			// Dying: it rings wildly; then the shutters fall open, the clapper drops out and the bell tips off its yoke; then the yoke sinks while the last ring dies away.
			float d = s.deathTime, s1 = smooth(d / 6), s2 = smooth((d - 5) / 9), s3 = smooth((d - 10) / 10);
			mantle.zRot += Mth.sin(d * 1.8F) * .08F * s1 * (1 - s2) + .4F * s3; mantle.xRot += -.2F * s1 + .9F * s2;
			skirt.xRot += Mth.sin(d * 2.4F) * .06F * (1 - s3); clapper.y += 6 * s2; clapper.zRot += .5F * s2; clapper.xRot += Mth.sin(d * 1.3F) * .3F * s1;
			shutterLeft.zRot += -.4F * s1 + .9F * s2; shutterRight.zRot += .4F * s1 - .9F * s2; gauge.zRot += .8F * s2; gauge.z += 2 * s2;
			crown.zRot += .35F * s2; crown.xRot += .3F * s2; crown.yRot += d * .05F * (1 - s2);
			yoke.y += Math.min(8, 8 * s3 + 2 * s2) + Mth.sin(d * 1.1F) * .4F * s3;
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
