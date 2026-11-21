package dev.wildercord.gametest;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.client.MastersArtPose;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.block.model.ItemTransform;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix4fc;
import dev.wildercord.client.compat.RenderStateData;

/** Binds the real item-entry and displayed-quad submission to one owner/frame/stack/model scope. */
public final class BraceNullItemDrawProbe {
	private static Call active;
	private static Report finished;
	private static String failure;
	private static ItemStackRenderState boundItem;
	private BraceNullItemDrawProbe() {}
	public static final class Call {
		final PlayerRenderState state; final Object model; ItemStackRenderState item; final PoseStack stack; final MultiBufferSource collector;
		final MastersArtPose.Frame frame; final boolean first; final float[] expectedPre;
		boolean pre, drawing, drawn, closed;
		Call(PlayerRenderState state, Object model, ItemStackRenderState item, PoseStack stack, MultiBufferSource buffer, boolean first, float[] expectedPre) {
			this.state = state; this.model = model; this.item = item; this.stack = stack; this.collector = buffer;
			this.frame = RenderStateData.get(state, MastersArtPose.FRAME); this.first = first; this.expectedPre = expectedPre == null ? null : expectedPre.clone();
		}
	}
	public record Report(boolean firstPerson, long stateIdentity, int itemIdentity, int modelIdentity, int stackIdentity,
		String displayContext, int actualQuads, List<Float> emittedQuadPositions, List<Float> expectedPreItem, List<Float> actualPreItem,
		List<Float> expectedDisplayed, List<Float> actualDisplayed, List<Float> expectedHilt, List<Float> actualHilt) {}
	private static List<Float> preExpected, preActual;
	public static Call worldBegin(Object model, PlayerRenderState state, ItemStackRenderState item, ItemStack sword, HumanoidArm arm, PoseStack stack, MultiBufferSource buffer) {
		var id = BraceNullCaptureProbe.identity();
		if (id == null || id.firstPerson() || state.id != id.owner() || arm != state.mainArm) return null;
		check(model instanceof PlayerModel && model instanceof BraceNullPlayerWidth, "Unknown world sword model/width");
		check(arm == (id.left() ? HumanoidArm.LEFT : HumanoidArm.RIGHT) && item == dev.wildercord.client.compat.RenderCompat.mainHandItemState(state) && !item.isEmpty()
			&& sword.is(Items.DIAMOND_SWORD) && ItemStack.isSameItemSameComponents(sword, dev.wildercord.client.compat.RenderCompat.mainHandStack(state))
			&& !state.isUsingItem && !state.isBaby && state.ticksUsingItem == 0,
			"Wrong world item/hand or unsupported use state");
		check(!dev.wildercord.client.compat.RenderCompat.isStabSwingType(state), "Unsupported native stabbing item motion");
		float[] body = BraceNullCaptureProbe.worldBody((PlayerModel) model, state, stack);
		float[] expected = BraceNullTransformOracle.worldItem(stack.last().pose().get(new float[16]), body, id.left(), ((BraceNullPlayerWidth) model).wildercord$braceNullSlim(), RenderStateData.get(state, MastersArtPose.FRAME).bladeTilt());
		return begin(new Call(state, model, item, stack, buffer, false, expected));
	}
	public static Call firstBegin(PlayerRenderState state, PoseStack stack, MultiBufferSource buffer) {
		var id = BraceNullCaptureProbe.identity(); if (id == null || !id.firstPerson() || state.id != id.owner()) return null;
		return begin(new Call(state, null, null, stack, buffer, true, null));
	}
	private static Call begin(Call call) { check(failure == null && active == null && finished == null, "Duplicate or nested owner item scope"); active = call; boundItem = call.item; return call; }
	public static void firstPre(PlayerRenderState state, ItemStackRenderState item, PoseStack stack, MultiBufferSource buffer, float[] wanted) {
		var id = BraceNullCaptureProbe.identity(); if (id == null || !id.firstPerson()) return;
		check(!item.isEmpty(), "Empty first-person item");
		check(active != null && active.first && active.state == state && active.stack == stack && active.collector == buffer
			&& (active.item == null || active.item == item), "Detached first-person item entry");
		if (active.item == null) { active.item = item; boundItem = item; }
		pre(active, wanted, stack.last().pose().get(new float[16]));
	}
	public static void worldPre(Call call, Object model, ItemStackRenderState item, PoseStack stack, MultiBufferSource buffer) {
		if (call == null) return;
		check(active == call && !call.first && call.model == model && call.item == item && call.stack == stack && call.collector == buffer, "Substituted world item entry");
		pre(call, call.expectedPre, stack.last().pose().get(new float[16]));
	}
	private static void pre(Call call, float[] wanted, float[] actual) {
		check(!call.closed && !call.pre && call.frame.equals(RenderStateData.get(call.state, MastersArtPose.FRAME)), "Duplicate/changed item frame");
		BraceNullCaptureProbe.observeFrame(call.state);
		try { BraceNullTransformOracle.requireHand(wanted, actual); } catch (AssertionError mismatch) { failure = mismatch.getMessage(); throw mismatch; }
		preExpected = list(wanted); preActual = list(actual); call.pre = true;
	}
	public static void itemEnter(ItemStackRenderState item, PoseStack stack, MultiBufferSource buffer) {
		if (active == null) { check(item != boundItem || BraceNullCaptureProbe.identity() == null, "Late detached owner item.submit"); return; }
		check(active.pre && active.item == item && active.stack == stack && active.collector == buffer && !active.drawing && !active.drawn, "Detached/duplicate native item.submit"); active.drawing = true;
	}
	public static void itemLeave(ItemStackRenderState item, boolean completed) {
		if (active == null) return;
		check(active.item == item && active.drawing && completed && active.drawn, "Missing native displayed item submission"); active.drawing = false;
	}
	public static void drawn(ItemStackRenderState owner, PoseStack stack, MultiBufferSource buffer, ItemDisplayContext context,
		ItemTransform display, Matrix4fc local, List<BakedQuad> quads, Matrix4fc submittedMatrix) {
		try { drawnChecked(owner, stack, buffer, context, display, local, quads, submittedMatrix); }
		catch (AssertionError rejected) { if (failure == null) failure = rejected.getMessage(); throw rejected; }
	}
	private static void drawnChecked(ItemStackRenderState owner, PoseStack stack, MultiBufferSource buffer, ItemDisplayContext context,
		ItemTransform display, Matrix4fc local, List<BakedQuad> quads, Matrix4fc submittedMatrix) {
		if (active == null) { check(owner != boundItem || BraceNullCaptureProbe.identity() == null, "Late detached owner draw"); return; }
		var call = active; var id = BraceNullCaptureProbe.identity();
		check(id != null && call.pre && call.drawing && !call.drawn && !call.closed && call.item == owner && call.stack == stack && call.collector == buffer,
			"Detached, substituted or duplicate displayed item callback");
		check(call.frame.equals(RenderStateData.get(call.state, MastersArtPose.FRAME)), "Displayed item frame changed"); BraceNullCaptureProbe.observeFrame(call.state);
		ItemDisplayContext expectedContext = call.first ? (id.left() ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
			: (id.left() ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
		check(context == expectedContext && quads != null && !quads.isEmpty(), "Wrong display context or missing actual quads");
		var vertices = new java.util.ArrayList<Float>();
		for (var quad : quads) for (int vertex = 0; vertex < 4; vertex++) {
			var position = dev.wildercord.client.compat.RenderCompat.quadPosition(quad, vertex);
			for (float component : new float[] {position.x(), position.y(), position.z()}) {
				check(Float.isFinite(component), "Nonfinite actual item quad"); vertices.add(component);
			}
		}
		float side = id.left() ? -1 : 1;
		check(display.rotation().x() == 0 && display.rotation().y() == -90 * side && display.rotation().z() == (call.first ? 25 : 55) * side
			&& display.translation().x() == (call.first ? 1.13F / 16 : 0) && display.translation().y() == (call.first ? 3.2F : 4F) / 16
			&& display.translation().z() == (call.first ? 1.13F : .5F) / 16 && display.scale().x() == (call.first ? .68F : .85F)
			&& display.scale().y() == display.scale().x() && display.scale().z() == display.scale().x(), "Stock sword display transform changed");
		BraceNullTransformOracle.requireHand(new org.joml.Matrix4f().get(new float[16]), local.get(new float[16]));
		float[] wanted = BraceNullTransformOracle.displayed(array(preExpected), call.first, id.left()); float[] actual = submittedMatrix.get(new float[16]);
		try { BraceNullTransformOracle.requireDisplayed(wanted, actual); } catch (AssertionError mismatch) { failure = mismatch.getMessage(); throw mismatch; }
		finished = new Report(call.first, BraceNullCaptureProbe.stateIdentity(call.state), System.identityHashCode(owner), System.identityHashCode(call.model), System.identityHashCode(stack),
			context.name(), quads.size(), List.copyOf(vertices), preExpected, preActual, list(wanted), list(actual), list(BraceNullTransformOracle.displayedHilt(wanted)), list(BraceNullTransformOracle.displayedHilt(actual)));
		call.drawn = true;
	}
	public static void end(Call call, boolean completed) {
		if (call == null) return;
		try { check(failure == null && active == call && !call.closed && completed && call.pre && call.drawn && !call.drawing, "Owner item scope did not complete an actual displayed draw"); call.closed = true; }
		finally { active = null; }
	}
	public static Report require() { check(failure == null && active == null && finished != null, "Missing complete native held-item draw"); return finished; }
	public static void clear() { active = null; finished = null; failure = null; boundItem = null; preExpected = preActual = null; }
	private static List<Float> list(float[] values) { var out = new java.util.ArrayList<Float>(); for (float v : values) out.add(v); return List.copyOf(out); }
	private static float[] array(List<Float> values) { float[] result = new float[values.size()]; for (int i = 0; i < result.length; i++) result[i] = values.get(i); return result; }
	private static void check(boolean pass, String message) { if (!pass) { if (failure == null) failure = message; throw new AssertionError("Brace/Null item draw: " + message); } }
}
