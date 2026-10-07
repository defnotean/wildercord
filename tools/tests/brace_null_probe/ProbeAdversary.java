import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.aura.MastersArtAnimation;
import dev.wildercord.aura.MastersStyleRules;
import dev.wildercord.aura.MastersViewMotion;
import dev.wildercord.client.MastersArtPose;
import dev.wildercord.client.MastersArtsClient;
import dev.wildercord.gametest.BraceNullCaptureProbe;
import dev.wildercord.gametest.BraceNullItemDrawProbe;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;
import dev.wildercord.gametest.BraceNullTransformOracle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;

/** Actual unedited receipt probe; minimal Minecraft caller shapes, original PoseStack/JOML, real samplers. */
public final class ProbeAdversary {
	private static int controls, rejected;
	private static final SubmitNodeCollector COLLECTOR = new SubmitNodeCollector() {};
	public static void main(String[] args) {
		for (int move : new int[] {24, 25}) for (boolean left : new boolean[] {false, true})
			for (String phase : new String[] {"windup", "active", "recovery"}) {
				body(move, left, phase, "control"); controls++;
				hand(move, left, phase, "control"); controls++;
				hand(move, left, phase, "no_swing_control"); controls++;
				for (String mutation : new String[] {"all999", "noop", "idle", "one_joint", "missing_baseline", "missing_deferred", "missing_world", "world_idle", "world_wrong_hand", "world_wrong_stack", "world_wrong_item", "world_wrong_model", "world_missing_draw", "world_wrong_display", "world_empty_quads", "world_duplicate", "world_detached", "world_swallowed_duplicate"})
					reject(() -> body(move, left, phase, mutation), "body " + mutation);
				if (move == 24) for (String mutation : new String[] {"world_no_hilt", "world_reverse_hilt"}) reject(() -> body(move, left, phase, mutation), mutation);
				for (String mutation : new String[] {"noop", "idle", "identity", "offset", "wrong_hand", "missing_entry", "borrowed_stack", "missing_draw", "wrong_display", "empty_quads", "duplicate_draw", "detached_draw", "swallowed_duplicate"})
					reject(() -> hand(move, left, phase, mutation), "hand " + mutation);
			}
		for (String legacy : new String[] {"masters_style_glacier_mirror_first_active", "masters_style_static_riposte_third_back_active", "masters_style_unmoved_cancelled_neutral"}) {
			BraceNullCaptureProbe.begin(Minecraft.getInstance(), legacy); BraceNullCaptureProbe.rendering(true);
			BraceNullCaptureProbe.handConsumed(new AvatarRenderState(), new FirstPersonHandsAndItemsRenderState(), new net.minecraft.client.renderer.item.ItemStackRenderState(), new PoseStack(), 0, 0, COLLECTOR);
			if (BraceNullCaptureProbe.finish(legacy) != null) throw new AssertionError("Old capture was newly constrained");
			BraceNullCaptureProbe.end(); controls++;
		}
		System.out.println("Actual probe synthetic controls passed=" + controls + ", malformed/no-op/idle evidence rejected=" + rejected + "; native renderer not run");
	}
	private static AvatarRenderState start(int move, boolean left, String phase, boolean first) {
		var mc = Minecraft.getInstance(); mc.options.camera.first = first; mc.player.hand = left ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
		var style = MastersStyleRules.animation(move);
		float age = phase.equals("windup") ? style.windup() / 2F + .5F : phase.equals("active") ? style.windup() + .5F : style.windup() + style.recovery() - 3.5F;
		mc.level.tick = 100 + (int) age;
		MastersArtsClient.accepted = new MastersArtsClient.Accepted(7, move, 100, style.windup(), style.recovery());
		var state = new AvatarRenderState(); state.id = 7; state.ageInTicks = mc.player.tickCount + .5F; state.mainArm = mc.player.hand;
		var pose = MastersArtAnimation.sample(move, age, style.windup(), style.recovery());
		state.frame = new MastersArtPose.Frame(pose, left, 36, -22, MastersArtAnimation.bladeTilt(move, age, style.windup(), pose.weight()), 100, move);
		state.currentSwing = new net.minecraft.world.entity.LivingEntity.SwingDescription(net.minecraft.world.InteractionHand.MAIN_HAND,
			new net.minecraft.world.item.component.SwingAnimation(net.minecraft.world.item.SwingAnimationType.WHACK));
		BraceNullCaptureProbe.begin(mc, name(move, left, phase, first)); BraceNullCaptureProbe.rendering(true); return state;
	}
	private static String name(int move, boolean left, String phase, boolean first) {
		return "masters_style_" + (move == 24 ? "unmoved" : "null_parry") + "_" + (left ? "left_turn_" : "") + (first ? "first" : "third_back") + "_" + phase;
	}
	private static void body(int move, boolean left, String phase, String mutation) {
		var state = start(move, left, phase, false); var model = new PlayerModel();
		var parts = parts(model); float[][] bind = {{0,0,0},{0,0,0},{-5,2,0},{5,2,0},{-1.9F,12,0},{1.9F,12,0}};
		float[] vanilla = new float[36]; int index = 0;
		for (int i = 0; i < parts.length; i++) {
			var p = parts[i]; p.x = bind[i][0]; p.y = bind[i][1]; p.z = bind[i][2];
			p.initial = new ModelPart.Initial(p.x, p.y, p.z, 0, 0, 0);
			p.xRot = .02F * i; p.yRot = -.01F * i; p.zRot = .012F * i;
			for (float value : new float[] {p.x,p.y,p.z,p.xRot,p.yRot,p.zRot}) vanilla[index++] = value;
		}
		var root = new PoseStack(); root.translate(.13F, -.04F, .07F);
		BraceNullCaptureProbe.submitted(model, state, root);
		try {
			if (!mutation.equals("missing_world")) {
				BraceNullCaptureProbe.bodyBefore(model, state); referenceBody(model, state.frame.pose(), left);
				var call = BraceNullItemDrawProbe.worldBegin(model, state, state.itemState, state.item, state.mainArm, root, COLLECTOR);
				try {
					if (!mutation.equals("world_idle")) referenceWorld(root, model, mutation.equals("world_wrong_hand") ? !left : left,
						mutation.equals("world_no_hilt") ? 0 : mutation.equals("world_reverse_hilt") ? -state.frame.bladeTilt() : state.frame.bladeTilt());
					PoseStack actual = root;
					if (mutation.equals("world_wrong_stack")) { actual = new PoseStack(); actual.last().pose().set(root.last().pose()); }
					var item = mutation.equals("world_wrong_item") ? new net.minecraft.client.renderer.item.ItemStackRenderState() : state.itemState;
					BraceNullItemDrawProbe.worldPre(call, mutation.equals("world_wrong_model") ? new PlayerModel() : model, item, actual, COLLECTOR);
					deep(item, actual, left, false, mutation.replace("world_", ""));
					BraceNullItemDrawProbe.end(call, true);
				} catch (AssertionError failure) { throw failure; }
			}
			index = 0; for (var p : parts) { p.x=vanilla[index++]; p.y=vanilla[index++]; p.z=vanilla[index++]; p.xRot=vanilla[index++]; p.yRot=vanilla[index++]; p.zRot=vanilla[index++]; }
			var deferred = mutation.equals("missing_deferred") ? null : BraceNullCaptureProbe.deferredEnter(model, state);
			try {
				if (!mutation.equals("missing_baseline")) BraceNullCaptureProbe.bodyBefore(model, state);
				if (!mutation.equals("noop")) referenceBody(model, mutation.equals("idle") ? MastersArtAnimation.NONE : state.frame.pose(), left);
				if (mutation.equals("all999")) for (var p : parts) p.x = p.y = p.z = p.xRot = p.yRot = p.zRot = 999;
				if (mutation.equals("one_joint")) model.leftArm.zRot += .25F;
				BraceNullCaptureProbe.bodyConsumed(model, state);
				var report = BraceNullCaptureProbe.finish(name(move, left, phase, false));
				if (report.expectedRigidPalette().size()!=36 || report.itemDraw().actualDisplayed().size()!=16 || report.itemDraw().actualHilt().size()!=3) throw new AssertionError("Incomplete world item evidence");
			} finally { BraceNullCaptureProbe.deferredLeave(deferred); }
		} finally { BraceNullCaptureProbe.end(); }
	}
	private static void referenceWorld(PoseStack stack, PlayerModel model, boolean left, float tilt) {
		var arm = left ? model.leftArm : model.rightArm; float side = left ? -1 : 1;
		stack.translate((arm.x + side * (model.slim ? .5F : 0)) / 16, arm.y / 16, arm.z / 16);
		stack.last().pose().rotateZYX(arm.zRot,arm.yRot,arm.xRot);
		stack.last().pose().rotateX((float)Math.toRadians(-90)).rotateY((float)Math.toRadians(180));
		stack.translate(side/16,2F/16,-10F/16); stack.translate(0,-1.327F/16,1.439F/16);
		stack.last().pose().rotateX((float)Math.toRadians(tilt)); stack.translate(0,1.327F/16,-1.439F/16);
	}
	private static void deep(net.minecraft.client.renderer.item.ItemStackRenderState item, PoseStack stack, boolean left, boolean first, String mutation) {
		if (!mutation.equals("detached") && !mutation.equals("detached_draw")) BraceNullItemDrawProbe.itemEnter(item, stack, COLLECTOR);
		if (!mutation.equals("missing_draw")) {
			stack.pushPose();
			var transform = new ItemTransform(new Vector3f(0,left?90:-90,(left?-1:1)*(first?25:55)),
				new Vector3f(first?1.13F/16:0,(first?3.2F:4)/16,(first?1.13F:.5F)/16), new Vector3f(first?.68F:.85F));
			transform.apply(left, stack.last()); if (mutation.equals("wrong_display")) stack.translate(.25F,0,0);
			var quads = new ItemQuads(); quads.empty = mutation.equals("empty_quads");
			var context = first ? (left?ItemDisplayContext.FIRST_PERSON_LEFT_HAND:ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
				: (left?ItemDisplayContext.THIRD_PERSON_LEFT_HAND:ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
			BraceNullItemDrawProbe.drawn(item,stack,COLLECTOR,context,transform,new Matrix4f(),quads,new Matrix4f(stack.last().pose()));
			if (mutation.equals("duplicate") || mutation.equals("duplicate_draw")) BraceNullItemDrawProbe.drawn(item,stack,COLLECTOR,context,transform,new Matrix4f(),quads,new Matrix4f(stack.last().pose()));
			if (mutation.equals("swallowed_duplicate")) {
				try { BraceNullItemDrawProbe.drawn(item,stack,COLLECTOR,context,transform,new Matrix4f(),quads,new Matrix4f(stack.last().pose())); }
				catch (AssertionError intentionallyCaught) { /* The capture must remain rejected after a renderer catches the fault. */ }
			}
			stack.popPose();
		}
		BraceNullItemDrawProbe.itemLeave(item,true);
	}

	private static void hand(int move, boolean left, String phase, String mutation) {
		var state = start(move, left, phase, true); var hands = new FirstPersonHandsAndItemsRenderState();
		if (mutation.equals("no_swing_control")) state.currentSwing = null;
		var stack = new PoseStack();
		stack.translate(.07F, -.03F, .015F); stack.last().pose().rotateY(.13F);
		try {
			BraceNullItemDrawProbe.Call call = null;
			if (!mutation.equals("missing_entry")) call = BraceNullCaptureProbe.handBefore(state, hands, state.item, .5833333F, .35F, stack, COLLECTOR);
			if (!mutation.equals("noop")) referenceHand(stack, mutation.equals("idle") ? MastersArtAnimation.NONE : state.frame.pose(),
				mutation.equals("wrong_hand") ? !left : left, .5833333F, .35F, state.currentSwing != null);
			if (mutation.equals("identity")) stack.last().pose().identity();
			if (mutation.equals("offset")) stack.translate(.25F, 0, 0);
			if (mutation.equals("borrowed_stack")) { var other = new PoseStack(); other.last().pose().set(stack.last().pose()); stack = other; }
			BraceNullCaptureProbe.handConsumed(state, hands, hands.mainHandRenderState, stack, .5833333F, .35F, COLLECTOR);
			deep(hands.mainHandRenderState, stack, left, true, mutation); BraceNullItemDrawProbe.end(call,true);
			var report = BraceNullCaptureProbe.finish(name(move, left, phase, true));
			if (report.expectedHandMatrix().size() != 16 || report.consumedHandMatrix().size() != 16 || report.consumedGrip().size() != 3) throw new AssertionError("Incomplete hand evidence");
		} finally { BraceNullCaptureProbe.end(); }
	}
	// Independent control caller: uses existing pure geometry helpers, never the new transform oracle.
	private static void referenceBody(PlayerModel m, MastersArtAnimation.Pose p, boolean left) {
		float w = p.weight(), side = left ? -1 : 1;
		joint(m.body, p.body(), side, w); joint(left ? m.leftArm : m.rightArm, p.sword(), side, w); joint(left ? m.rightArm : m.leftArm, p.guard(), side, w);
		joint(left ? m.rightLeg : m.leftLeg, p.frontLeg(), side, w); joint(left ? m.leftLeg : m.rightLeg, p.rearLeg(), side, w);
		m.head.xRot += p.head().x() * w; m.head.yRot += p.head().y() * side * w; m.head.zRot += p.head().z() * side * w;
		var head = MastersArtAnimation.boundedHead(new MastersArtAnimation.Joint(m.body.xRot, m.body.yRot, m.body.zRot), new MastersArtAnimation.Joint(m.head.xRot, m.head.yRot, m.head.zRot), w);
		m.head.xRot = head.x(); m.head.yRot = head.y(); m.head.zRot = head.z();
		for (var part : new ModelPart[] {m.body, m.head, m.rightArm, m.leftArm}) {
			var i = part.initial; var target = MastersArtAnimation.pivot(p, i.x(), i.y(), i.z(), left);
			part.x += w * (target.x() - part.x); part.y += w * (target.y() - part.y); part.z += w * (target.z() - part.z);
		}
		for (var part : new ModelPart[] {m.rightLeg, m.leftLeg}) { part.y += w * (part.initial.y() + p.lower() - part.y); part.z += w * (part.initial.z() + p.forward() - part.z); }
	}
	private static void joint(ModelPart p, MastersArtAnimation.Joint j, float side, float w) { p.xRot += w * (j.x() - p.xRot); p.yRot += w * (j.y() * side - p.yRot); p.zRot += w * (j.z() * side - p.zRot); }
	private static ModelPart[] parts(PlayerModel p) { return new ModelPart[] {p.body, p.head, p.rightArm, p.leftArm, p.rightLeg, p.leftLeg}; }
	private static void referenceHand(PoseStack stack, MastersArtAnimation.Pose p, boolean left, float attack, float inverse, boolean swinging) {
		var v = MastersArtAnimation.view(p, left, inverse, 36, -22); var h = v.transform(); var g = v.grip();
		stack.translate(0, MastersViewMotion.heightCompensation(inverse, p.weight()), 0);
		stack.translate(h.x(), h.y(), h.z()); stack.translate(g.x(), g.y(), g.z());
		stack.last().pose().rotateY((float) Math.toRadians(h.yaw())).rotateX((float) Math.toRadians(h.pitch())).rotateZ((float) Math.toRadians(h.roll()));
		stack.translate(-g.x(), -g.y(), -g.z()); stack.translate((left ? -1 : 1) * .56F, -.52F - .6F * inverse, -.72F);
		float side = left ? -1 : 1, root = (float) Math.sqrt(attack), wave = net.minecraft.util.Mth.sin(root * (float) Math.PI), square = net.minecraft.util.Mth.sin(attack * attack * (float) Math.PI);
		var delta = new Matrix4f().translation(side * -.4F * wave, .2F * net.minecraft.util.Mth.sin(root * ((float) Math.PI * 2)), -.2F * net.minecraft.util.Mth.sin(attack * (float) Math.PI))
			.rotateY((float) Math.toRadians(side * (45 + square * -20))).rotateZ((float) Math.toRadians(side * wave * -20)).rotateX((float) Math.toRadians(wave * -80)).rotateY((float) Math.toRadians(side * -45));
		if (swinging) stack.mulPose(MastersViewMotion.fadeSwing(delta, p.weight()));
	}
	private static void reject(Runnable work, String name) { try { work.run(); } catch (AssertionError expected) { rejected++; return; } throw new AssertionError("Accepted malformed " + name); }
}
