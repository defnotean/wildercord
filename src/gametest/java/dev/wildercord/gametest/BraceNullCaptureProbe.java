package dev.wildercord.gametest;

import dev.wildercord.client.MastersArtPose;
import dev.wildercord.client.MastersArtsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.HumanoidArm;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Pair-only passive receipt inside MastersCaptureProbe's one native extract/render/readback. */
public final class BraceNullCaptureProbe {
	private static final String RUN = java.util.UUID.randomUUID().toString();
	private static final AtomicLong SEQUENCE = new AtomicLong();
	private static final IdentityHashMap<AvatarRenderState, Model<?>> SUBMITTED = new IdentityHashMap<>();
	private static final IdentityHashMap<AvatarRenderState, float[]> ROOTS = new IdentityHashMap<>();
	private static final IdentityHashMap<AvatarRenderState, BodyBasis> LAYER_BASES = new IdentityHashMap<>();
	private static final IdentityHashMap<AvatarRenderState, Long> STATES = new IdentityHashMap<>();
	private static BraceNullPhaseContract.Identity expected;
	private static BraceNullPhaseContract.Sample consumed;
	private static List<Float> bodyPalette, expectedBody, bodyBaseline, bodyBind, handMatrix, expectedHand, handEntry, handGrip, expectedGrip;
	private static final IdentityHashMap<AvatarRenderState, BodyBasis> BODY_BASES = new IdentityHashMap<>();
	private static final IdentityHashMap<AvatarRenderState, HandBasis> HAND_BASES = new IdentityHashMap<>();
	private record BodyBasis(PlayerModel model, MastersArtPose.Frame frame, float[] baseline, float[] bind) {}
	private record HandBasis(MastersArtPose.Frame frame, Object hands, Object stack, float attack, float inverseHeight, boolean swing, float[] entry) {}
	private static long gameTick;
	private static int entityTick;
	private static boolean rendering;
	private static String rejected;
	private static final NativeBodySubmission NATIVE_BODY = new NativeBodySubmission();
	private static final ThreadLocal<Deferred> DEFERRED = new ThreadLocal<>();
	public record Deferred(Object node, Model<?> model, Object state, NativeBodySubmission.Visit visit, Deferred previous) {}
	private BraceNullCaptureProbe() {}
	public record Report(BraceNullPhaseContract.Identity identity, BraceNullPhaseContract.Sample consumed,
		String path, List<Float> consumedRigidPalette, List<Float> expectedRigidPalette, List<Float> nativeBodyBaseline, List<Float> nativeBodyBind,
		List<Float> consumedHandMatrix, List<Float> expectedHandMatrix, List<Float> nativeHandEntry, List<Float> consumedGrip, List<Float> expectedGrip, BraceNullItemDrawProbe.Report itemDraw) {}

	public static void begin(Minecraft mc, String name) {
		end();
		if (!name.matches("masters_style_(unmoved|null_parry)_(third_back|first|left_turn_third_back|left_turn_first)_(windup|active|recovery)")) return;
		var accepted = MastersArtsClient.timeline(mc.player);
		check(accepted != null && accepted.entity() == mc.player.getId(), "No authentic accepted owner timeline");
		int move = name.startsWith("masters_style_unmoved_") ? 24 : 25;
		check(accepted.move() == move, "Screenshot name and accepted art differ");
		expected = new BraceNullPhaseContract.Identity(RUN, name, SEQUENCE.incrementAndGet(), mc.player.getUUID().toString(), mc.player.getId(), move,
			accepted.startTick(), mc.player.getMainArm() == HumanoidArm.LEFT, mc.options.getCameraType().isFirstPerson(),
			accepted.windup(), accepted.recovery(), name.substring(name.lastIndexOf('_') + 1));
		gameTick = mc.level.getGameTime(); entityTick = mc.player.tickCount;
	}
	public static <T> T passive(java.util.function.Supplier<T> observer, T fallback) { return NATIVE_BODY.observe(observer, fallback); }
	public static void passive(Runnable observer) { NATIVE_BODY.observe(observer); }
	public static void rendering(boolean value) { rendering = value; }
	public static void submitted(Model<?> model, Object state, com.mojang.blaze3d.vertex.PoseStack stack) {
		if (expected == null || !rendering || expected.firstPerson() || !(state instanceof AvatarRenderState avatar) || avatar.id != expected.owner()) return;
		check(model instanceof PlayerModel, "Counter body is not the registered player model");
		check(!SUBMITTED.containsKey(avatar), "Duplicate body submission");
		requireRoot((PlayerModel) model); SUBMITTED.put(avatar, model); ROOTS.put(avatar, stack.last().pose().get(new float[16]));
	}
	private static NativeBodySubmission.Frames frames(AvatarRenderState state) { return new NativeBodySubmission.Frames(state.getData(MastersArtPose.FRAME), null); }
	public static NativeBodySubmission.Origin submissionBegin(Model<?> model, Object state, com.mojang.blaze3d.vertex.PoseStack stack, Object material) {
		if (expected == null || !rendering || expected.firstPerson() || !(state instanceof AvatarRenderState avatar) || avatar.id != expected.owner()) return null;
		return NATIVE_BODY.begin(model, state, frames(avatar), material, stack.last().pose().get(new float[16]));
	}
	public static void submissionEnd(NativeBodySubmission.Origin call, boolean completed) { if (call != null) NATIVE_BODY.submittedEnd(call, completed); }
	public static void nativeSubmitted(Object node, Model<?> model, Object state, Object material, float[] root, boolean outline) {
		if (expected == null || !rendering || expected.firstPerson() || !(state instanceof AvatarRenderState avatar) || avatar.id != expected.owner()) return;
		NATIVE_BODY.submitted(node, model, state, frames(avatar), material, root, outline);
	}
	public static Deferred deferredEnter(Object node, Model<?> model, Object state, Object material, float[] root) {
		if (expected == null || !rendering || expected.firstPerson()) return null;
		boolean tracked = NATIVE_BODY.tracked(node);
		if (!tracked && (!(state instanceof AvatarRenderState avatar) || avatar.id != expected.owner())) return null;
		check(state instanceof AvatarRenderState, "Tracked native Submit changed state type");
		var avatar = (AvatarRenderState) state;
		var visit = NATIVE_BODY.enter(node, model, state, frames(avatar), material, root);
		var call = new Deferred(node, model, state, visit, DEFERRED.get()); DEFERRED.set(call); return call;
	}
	public static void bodyDrawn(Object node, Model<?> model, Object state, float[] root) {
		var call = DEFERRED.get(); if (call == null) return;
		check(state instanceof AvatarRenderState, "Native body draw changed state type");
		observe((AvatarRenderState) state);
		NATIVE_BODY.drawn(call.visit(), node, model, state, frames((AvatarRenderState) state), root);
	}
	public static void deferredLeave(Deferred call, boolean completed) {
		if (call == null) return;
		try { NATIVE_BODY.leave(call.visit(), completed); }
		finally { if (call.previous() == null) DEFERRED.remove(); else DEFERRED.set(call.previous()); }
	}
	/** Observes untouched vanilla output at the production adapter entry, only in the real deferred call. */
	public static void bodyBefore(PlayerModel model, AvatarRenderState state) {
		if (expected == null || !rendering || expected.firstPerson() || state.id != expected.owner()) return;
		var deferred = DEFERRED.get();
		if (deferred == null) {
			check(SUBMITTED.get(state) == model, "Layer baseline is not from the submitted model/state");
			observe(state); LAYER_BASES.put(state, new BodyBasis(model, state.getData(MastersArtPose.FRAME), palette(model, false), palette(model, true))); return;
		}
		if (!deferred.visit().primary()) return;
		NATIVE_BODY.baseline(deferred.visit(), model, state, frames(state));
		check(SUBMITTED.get(state) == model, "Body baseline is not from the submitted deferred model/state");
		observe(state);
		BODY_BASES.put(state, new BodyBasis(model, state.getData(MastersArtPose.FRAME), palette(model, false), palette(model, true)));
	}
	/** Called after the complete native PlayerModel.setupAnim, including production tail hooks. */
	public static void bodyConsumed(PlayerModel model, AvatarRenderState state) {
		if (expected == null || !rendering || expected.firstPerson() || state.id != expected.owner()) return;
		var deferred = DEFERRED.get();
		if (deferred == null || !deferred.visit().primary()) return;
		check(SUBMITTED.get(state) == model, "Deferred body did not consume its submitted model/state pair");
		check(model.head.visible && model.body.visible && model.rightArm.visible && model.leftArm.visible && model.rightLeg.visible && model.leftLeg.visible,
			"Classic receipt requires the complete rigid backend");
		observe(state); requireRoot(model);
		var basis = BODY_BASES.remove(state);
		check(basis != null && basis.model() == model && basis.frame().equals(state.getData(MastersArtPose.FRAME)), "Missing or borrowed pre-art native body baseline");
		float[] actual = palette(model, false);
		float[] wanted = BraceNullTransformOracle.body(consumed.pose(), expected.left(), basis.baseline(), basis.bind());
		validate(() -> BraceNullTransformOracle.requireBody(wanted, actual));
		bodyPalette = list(actual); expectedBody = list(wanted); bodyBaseline = list(basis.baseline()); bodyBind = list(basis.bind());
		NATIVE_BODY.palette(deferred.visit(), model, state, frames(state));
	}
	/** The outer native submitArmWithItem wrapper runs before any production HEAD injection. */
	public static BraceNullItemDrawProbe.Call handBefore(AvatarRenderState state, net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState hands,
		net.minecraft.world.item.ItemStack item, float attack, float inverseHeight, com.mojang.blaze3d.vertex.PoseStack stack, net.minecraft.client.renderer.SubmitNodeCollector collector) {
		if (expected == null || !rendering || !expected.firstPerson() || state.id != expected.owner()) return null;
		observe(state);
		check(!state.isUsingItem && !state.isAutoSpinAttack && !hands.isScoping && !hands.hasMainHandMapData
			&& item.is(net.minecraft.world.item.Items.DIAMOND_SWORD)
			&& net.minecraft.world.item.ItemStack.isSameItemSameComponents(item, state.getMainHandItemStack())
			&& hands instanceof dev.wildercord.client.MastersHandMotionState motion && !motion.wildercord$mainHandEquipping(),
			"Pair hand oracle requires the genuine same-item Classic sword path, without equip/use overrides");
		boolean swing = state.currentSwing != null && state.currentSwing.hand() == net.minecraft.world.InteractionHand.MAIN_HAND;
		if (swing) {
			var type = state.currentSwing.animation().type();
			check(type == net.minecraft.world.item.SwingAnimationType.NONE || type == net.minecraft.world.item.SwingAnimationType.WHACK, "Unsupported native swing path");
			swing = type == net.minecraft.world.item.SwingAnimationType.WHACK;
		}
		check(!HAND_BASES.containsKey(state), "Duplicate native main-hand entry");
		HAND_BASES.put(state, new HandBasis(state.getData(MastersArtPose.FRAME), hands, stack, attack, inverseHeight, swing, stack.last().pose().get(new float[16])));
		return BraceNullItemDrawProbe.firstBegin(state, hands.mainHandRenderState, stack, collector);
	}
	public static void handConsumed(AvatarRenderState state, net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState hands,
		net.minecraft.client.renderer.item.ItemStackRenderState item, com.mojang.blaze3d.vertex.PoseStack stack, float attack, float inverseHeight, net.minecraft.client.renderer.SubmitNodeCollector collector) {
		if (expected == null || !rendering || !expected.firstPerson() || state.id != expected.owner()) return;
		observe(state);
		var basis = HAND_BASES.remove(state);
		check(basis != null && basis.hands() == hands && basis.stack() == stack && basis.frame().equals(state.getData(MastersArtPose.FRAME))
			&& Float.floatToIntBits(basis.attack()) == Float.floatToIntBits(attack)
			&& Float.floatToIntBits(basis.inverseHeight()) == Float.floatToIntBits(inverseHeight), "Missing or borrowed native hand entry/matrix context");
		var raw = basis.frame();
		float[] wanted = BraceNullTransformOracle.hand(consumed.pose(), expected.left(), raw.yawDelta(), raw.pitchDelta(), inverseHeight, attack, basis.swing(), basis.entry());
		float[] actual = stack.last().pose().get(new float[16]);
		validate(() -> BraceNullTransformOracle.requireHand(wanted, actual));
		BraceNullItemDrawProbe.firstPre(state, item, stack, collector, wanted);
		handMatrix = list(actual); expectedHand = list(wanted); handEntry = list(basis.entry());
		handGrip = list(BraceNullTransformOracle.grip(actual)); expectedGrip = list(BraceNullTransformOracle.grip(wanted));
	}
	static BraceNullPhaseContract.Identity identity() { return rendering ? expected : null; }
	static long stateIdentity(AvatarRenderState state) { return STATES.getOrDefault(state, 0L); }
	static void observeFrame(AvatarRenderState state) { check(expected != null && rendering, "Detached native draw"); observe(state); }
	static float[] worldBody(PlayerModel model, AvatarRenderState state, com.mojang.blaze3d.vertex.PoseStack stack) {
		observeFrame(state); requireRoot(model);
		var basis = LAYER_BASES.remove(state);
		check(SUBMITTED.get(state) == model && basis != null && basis.model() == model && basis.frame().equals(state.getData(MastersArtPose.FRAME)), "Missing or borrowed attachment body baseline");
		validate(() -> BraceNullTransformOracle.requireHand(ROOTS.get(state), stack.last().pose().get(new float[16])));
		float[] body = BraceNullTransformOracle.body(consumed.pose(), expected.left(), basis.baseline(), basis.bind());
		validate(() -> BraceNullTransformOracle.requireBody(body, palette(model, false))); return body;
	}
	private static void requireRoot(PlayerModel model) {
		var p = model.root();
		check(p.visible && p.x == 0 && p.y == 0 && p.z == 0 && p.xRot == 0 && p.yRot == 0 && p.zRot == 0
			&& p.xScale == 1 && p.yScale == 1 && p.zScale == 1, "Classic body root differs from its authoritative identity bind");
	}

	private static float[] palette(PlayerModel model, boolean bind) {
		float[] values = new float[36]; int i = 0;
		for (var part : new net.minecraft.client.model.geom.ModelPart[] {model.body, model.head, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg}) {
			check(part.xScale == 1 && part.yScale == 1 && part.zScale == 1, "Unexpected scaled Classic body part");
			var initial = part.getInitialPose();
			float[] row = bind ? new float[] {initial.x(), initial.y(), initial.z(), initial.xRot(), initial.yRot(), initial.zRot()}
				: new float[] {part.x, part.y, part.z, part.xRot, part.yRot, part.zRot};
			for (float value : row) { check(Float.isFinite(value), "Nonfinite native body transform"); values[i++] = value; }
		}
		return values;
	}
	private static List<Float> list(float[] values) { var result = new java.util.ArrayList<Float>(); for (float value : values) result.add(value); return List.copyOf(result); }

	private static void observe(AvatarRenderState state) {
		var mc = Minecraft.getInstance();
		check(mc.player.getId() == expected.owner() && mc.player.getUUID().toString().equals(expected.ownerUuid())
			&& mc.level.getGameTime() == gameTick && mc.player.tickCount == entityTick
			&& mc.options.getCameraType().isFirstPerson() == expected.firstPerson(), "Owner, camera or clock changed within one native capture");
		var raw = state.getData(MastersArtPose.FRAME);
		check(raw != null && state.mainArm == (expected.left() ? HumanoidArm.LEFT : HumanoidArm.RIGHT), "Missing rendered art or changed hand");
		// HitStop restores ageInTicks together with the immutable art. This derives the actual
		// consumed animation age from that restored clock, not the requested screenshot tick.
		float age = state.ageInTicks - entityTick + (gameTick - expected.activation());
		long stateId = STATES.computeIfAbsent(state, ignored -> SEQUENCE.incrementAndGet());
		var sample = new BraceNullPhaseContract.Sample(expected, stateId, state.id, raw.move(), raw.activation(), raw.leftHanded(), age, raw.pose(), raw.bladeTilt());
		validate(() -> BraceNullPhaseContract.require(expected, sample));
		check(consumed == null || consumed.equals(sample), "One screenshot consumed differing owner palettes");
		consumed = sample;
	}
	public static Report finish(String name) {
		if (expected == null) return null;
		NATIVE_BODY.requireHealthy();
		if (!expected.firstPerson()) NATIVE_BODY.requireComplete();
		check(rejected == null && name.equals(expected.screenshot()) && consumed != null && (expected.firstPerson() ? handMatrix != null && expectedHand != null : bodyPalette != null && expectedBody != null), "Missing actual renderer-consumed phase receipt");
		return new Report(expected, consumed, expected.firstPerson() ? "native main-hand item submit" : "native deferred PlayerModel.setupAnim", bodyPalette, expectedBody, bodyBaseline, bodyBind, handMatrix, expectedHand, handEntry, handGrip, expectedGrip, BraceNullItemDrawProbe.require());
	}
	public static void end() { rejected = null; expected = null; consumed = null; bodyPalette = expectedBody = bodyBaseline = bodyBind = handMatrix = expectedHand = handEntry = handGrip = expectedGrip = null; rendering = false; SUBMITTED.clear(); ROOTS.clear(); LAYER_BASES.clear(); STATES.clear(); BODY_BASES.clear(); HAND_BASES.clear(); DEFERRED.remove(); NATIVE_BODY.clear(); BraceNullItemDrawProbe.clear(); }
	private static void validate(Runnable assertion) {
		try { assertion.run(); } catch (AssertionError failure) { if (rejected == null) rejected = failure.getMessage(); throw failure; }
	}
	private static void check(boolean pass, String message) { if (!pass) { if (rejected == null) rejected = message; throw new AssertionError("Brace/Null native capture: " + message); } }
}
