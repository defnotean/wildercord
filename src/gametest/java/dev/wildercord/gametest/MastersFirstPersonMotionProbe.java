package dev.wildercord.gametest;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.aura.MastersArtAnimation;
import dev.wildercord.client.MastersArtPose;
import dev.wildercord.client.MastersHandMotionState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.component.SwingAnimation;
import org.joml.Matrix4f;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/** Explicitly synthetic native-renderer contracts; never substituted for accepted phase PNGs. */
final class MastersFirstPersonMotionProbe {
	private MastersFirstPersonMotionProbe() {}

	static void verify(Minecraft mc) {
		var nativeHands = new FirstPersonHandsAndItems();
		var extracted = new FirstPersonHandsAndItemsRenderState();
		check(mc.player.getItemSwapScale(1) == 1, "Native equip contract begins after the ordinary item ticker settled");
		for (int tick = 0; tick < 5; tick++) {
			nativeHands.tick(mc.player); nativeHands.extractRenderState(mc.player, .5F, extracted);
			check(((MastersHandMotionState) extracted).wildercord$mainHandEquipping() == (tick < 4),
				"Actual native initial equip keeps lowering and raising provenance until settled: " + tick);
		}
		nativeHands.itemUsed(InteractionHand.MAIN_HAND);
		nativeHands.extractRenderState(mc.player, .5F, extracted);
		check(((MastersHandMotionState) extracted).wildercord$mainHandEquipping(), "Actual itemUsed lowering remains an equip transition");
		for (int tick = 0; tick < 4; tick++) nativeHands.tick(mc.player);
		nativeHands.extractRenderState(mc.player, .5F, extracted);
		check(!((MastersHandMotionState) extracted).wildercord$mainHandEquipping(), "Native use transition releases only after raising finishes");

		var player = new PlayerRenderState();
		player.hasPlayer = true;
		player.avatarRenderState = (AvatarRenderState) mc.getEntityRenderDispatcher().getRenderer(mc.player).createRenderState(mc.player, .5F);
		var avatar = player.avatarRenderState;
		avatar.setData(dev.wildercord.client.combat.ArticulatedCombat.FRAME, null);
		var hands = player.firstPersonHandsAndItems;
		mc.player.firstPersonHandsAndItems().extractRenderState(mc.player, .5F, hands);
		hands.handRenderSelection = FirstPersonHandsAndItemsRenderState.HandRenderSelection.RENDER_MAIN_HAND_ONLY;
		hands.viewXRot = hands.xBob; hands.viewYRot = hands.yBob;
		hands.mainHandSwapScale = 1;
		avatar.currentSwing = new LivingEntity.SwingDescription(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, 6);
		hands.attackHand = InteractionHand.MAIN_HAND;
		var pose = MastersArtAnimation.sample(3, 6.5F, 6, 12);
		boolean left = avatar.mainArm == net.minecraft.world.entity.HumanoidArm.LEFT;
		float side = left ? -1 : 1;
		var full = new MastersArtPose.Frame(pose, left, 0, -3, 0, 1, 3);

		avatar.setData(MastersArtPose.FRAME, null);
		Matrix4f neutral = submit(mc, player, 0, 1, false);
		Matrix4f ordinary = submit(mc, player, .5833333F, .1F, false);
		avatar.setData(MastersArtPose.FRAME, new MastersArtPose.Frame(MastersArtAnimation.NONE, left, 0, -3, 0, 1, 3));
		check(ordinary.equals(submit(mc, player, .5833333F, .1F, false), .00001F), "Zero ownership preserves the exact ordinary native item motion");
		avatar.setData(MastersArtPose.FRAME, full);
		Matrix4f authored = submit(mc, player, 0, 1, false);
		check(authored.equals(submit(mc, player, .5833333F, .1F, false), .00001F),
			"Full accepted ownership removes both triggering swing and attack-height from actual native item submission");
		PoseStack oldComposition = new PoseStack();
		MastersArtPose.firstPerson(oldComposition, InteractionHand.MAIN_HAND, avatar, .9F);
		Matrix4f expectedEquip = new Matrix4f(oldComposition.last().pose()).mul(ordinary);
		check(expectedEquip.equals(submit(mc, player, .5833333F, .1F, true), .00001F),
			"A genuine equip transition retains the original authored-plus-vanilla composition");
		// Recover the native item display and swing delta from untouched native submissions, then
		// verify the actual wrapper applies amplitude blending at partial ownership as well.
		Matrix4f display = new Matrix4f().translate(-.56F * side, .52F, .72F).mul(neutral);
		Matrix4f delta = new Matrix4f().translate(-.56F * side, .52F + .6F * .9F, .72F).mul(ordinary).mul(new Matrix4f(display).invert());
		var halfPose = new MastersArtAnimation.Pose(.5F, pose.body(), pose.head(), pose.sword(), pose.guard(), pose.frontLeg(),
			pose.rearLeg(), pose.lower(), pose.forward(), pose.hand());
		avatar.setData(MastersArtPose.FRAME, new MastersArtPose.Frame(halfPose, left, 0, -3, 0, 1, 3));
		PoseStack partial = new PoseStack();
		partial.translate(0, dev.wildercord.aura.MastersViewMotion.heightCompensation(.9F, .5F), 0);
		MastersArtPose.firstPerson(partial, InteractionHand.MAIN_HAND, avatar, .9F);
		partial.translate(.56F * side, -.52F - .6F * .9F, -.72F);
		partial.mulPose(dev.wildercord.aura.MastersViewMotion.fadeSwing(delta, .5F));
		check(new Matrix4f(partial.last().pose()).mul(display).equals(submit(mc, player, .5833333F, .1F, false), .00003F),
			"Actual native partial ownership blends transform amplitude, preserving its input attack phase");
		avatar.setData(MastersArtPose.FRAME, null);
		check(ordinary.equals(submit(mc, player, .5833333F, .1F, false), .00001F), "Cancellation returns to the exact native swing and height");

		hands.handRenderSelection = FirstPersonHandsAndItemsRenderState.HandRenderSelection.RENDER_OFF_HAND_ONLY;
		hands.offHandItem = hands.mainHandItem.copy(); hands.offHandHeight = hands.oldOffHandHeight = .1F; hands.offHandSwapScale = 1;
		mc.getItemModelResolver().updateForLiving(hands.offHandRenderState, hands.offHandItem, left ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, mc.player);
		hands.attackHand = InteractionHand.OFF_HAND;
		avatar.currentSwing = new LivingEntity.SwingDescription(InteractionHand.OFF_HAND, SwingAnimation.DEFAULT, 6);
		Matrix4f offhand = submit(mc, player, .5833333F, .1F, false);
		avatar.setData(MastersArtPose.FRAME, full);
		check(offhand.equals(submit(mc, player, .5833333F, .1F, false), .00001F), "Main-hand art ownership never suppresses native offhand motion");
		dev.wildercord.Wildercord.LOGGER.info("MASTERS_NATIVE_SYNTHETIC_MOTION_CONTRACT passed: ordinary, authored, partial, equip, cancellation, offhand");
	}

	private static Matrix4f submit(Minecraft mc, PlayerRenderState player, float attack, float height, boolean equipping) {
		var hands = player.firstPersonHandsAndItems;
		player.avatarRenderState.swingAnimation = attack;
		hands.mainHandHeight = hands.oldMainHandHeight = height;
		((MastersHandMotionState) hands).wildercord$mainHandEquipping(equipping);
		List<Matrix4f> items = new ArrayList<>();
		SubmitNodeCollector collector = (SubmitNodeCollector) Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(),
			new Class<?>[] {SubmitNodeCollector.class}, (proxy, method, args) -> {
				if (method.getDeclaringClass() == Object.class) return switch (method.getName()) {
					case "toString" -> "NativeMotionContract";
					case "hashCode" -> System.identityHashCode(proxy);
					case "equals" -> proxy == args[0];
					default -> throw new AssertionError(method);
				};
				if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
				if (method.getName().equals("order")) return proxy;
				if (method.getName().equals("submitItem")) items.add(new Matrix4f(((PoseStack) args[0]).last().pose()));
				return null;
			});
		mc.gameRenderer.firstPersonHandsAndItemsRenderer.submitHandsWithItems(.5F, new PoseStack(), collector, player, hands);
		check(items.size() == 1, "Native motion contract submits one actual held item");
		return items.getFirst();
	}
	private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
