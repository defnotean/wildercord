package dev.wildercord.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildercord.aura.ArticulatedCombatPose;
import dev.wildercord.aura.MastersViewMotion;
import dev.wildercord.client.compat.FirstPersonHandView;
import dev.wildercord.client.compat.RenderCompat;
import dev.wildercord.client.compat.McModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import dev.wildercord.client.render.RenderStateData;

/** Actual skin-textured upper arms, forearms and hands, authored independently of the world camera. */
public final class ArticulatedViewModel extends McModel<ArticulatedViewModel.Frame> {
	public record Frame(ArticulatedCombatPose.ViewPose pose, boolean leftSleeve, boolean rightSleeve) {}
	private final ArticulatedRig rig;

	public ArticulatedViewModel(boolean slim) { this(new ArticulatedRig(slim, false)); }
	private ArticulatedViewModel(ArticulatedRig rig) {
		super(rig.root, RenderType::entityTranslucent);
		this.rig = rig;
	}

	@Override
	public void setupAnim(Frame frame) {
		rig.apply(frame.pose()::local);
		rig.viewLayers(frame.leftSleeve(), frame.rightSleeve());
		rig.armsOnly();
	}

	/** True means this backend owns both arms and the sword, including the otherwise-empty offhand. */
	public static boolean submit(PlayerRenderState avatar, FirstPersonHandView hands,
			float partial, InteractionHand hand, PoseStack stack, MultiBufferSource buffer, int light) {
		ArticulatedCombat.Frame combat = ArticulatedCombat.viewFrame(avatar);
		if (combat == null || !handAdmission(combat, avatar, hands, partial).admitted()) return false;
		if (!(RenderCompat.livingModel(avatar) instanceof PlayerModel playerModel)
			|| playerModel.getClass() != PlayerModel.class || !(playerModel instanceof ArticulatedModelAccess access) || !access.wildercord$bodyOwned()) return false;
		if (hand != InteractionHand.MAIN_HAND) return true;
		var view = ArticulatedCombatPose.view(combat.pose(), combat.leftHanded());
		Frame frame = new Frame(view, avatar.showLeftSleeve, avatar.showRightSleeve);
		ArticulatedViewModel model = access.wildercord$viewModel();
		stack.pushPose();
		try {
			float weight = combat.pose().weight();
			float yaw = Math.max(-25, Math.min(25, Float.isFinite(combat.yawDelta()) ? combat.yawDelta() : 0)) * weight;
			float pitch = Math.max(-20, Math.min(20, Float.isFinite(combat.pitchDelta()) ? combat.pitchDelta() : 0)) * weight;
			float clearance = ArticulatedCombatPose.viewClearance(yaw, pitch);
			stack.translate(view.origin().x(), view.origin().y() - .8F * clearance, view.origin().z() - clearance);
			// Keep the game camera completely free. Only the viewmodel acknowledges a bounded
			// difference between accepted aim and free look; large turns use the existing aim cue.
			dev.wildercord.client.compat.RenderCompat.rotateDegrees(stack, Axis.YP, -yaw);
			dev.wildercord.client.compat.RenderCompat.rotateDegrees(stack, Axis.XP, -pitch);
			stack.scale(-1, -1, 1);
			dev.wildercord.client.compat.RenderCompat.submitModel(buffer, model, frame, stack, RenderType.entityTranslucent(avatar.skin.texture()), light, OverlayTexture.NO_OVERLAY, -1, 0);
			// The same immutable palette sets the hand at submission and deferred model rendering.
			model.setupAnim(frame);
			if (access.wildercord$armor() != null) access.wildercord$armor().submitView(model.rig, avatar, stack, buffer, light);
			var shell = RenderStateData.get(avatar, ArticulatedAuraShellRenderer.READY);
			if (shell != null) shell.submitView(model.rig, avatar, stack, buffer);
			model.rig.socket(avatar.mainArm, stack);
			ArticulatedCombat.orientItemAtSocket(stack);
			dev.wildercord.client.compat.RenderCompat.submitItemState(
			dev.wildercord.client.compat.RenderCompat.mainHandItemState(avatar), stack, buffer, light, OverlayTexture.NO_OVERLAY, 0);
		} finally {
			stack.popPose();
		}
		return true;
	}

	/** Reads the extracted state only; also used by passive native admission diagnostics. */
	public static MastersViewMotion.HandAdmission handAdmission(ArticulatedCombat.Frame combat, PlayerRenderState avatar,
			FirstPersonHandView hands, float partial) {
		boolean acceptedArt = combat.move() != -1 && combat.activation() != Long.MIN_VALUE && combat.pose().weight() > 0;
		boolean known = hands.equipKnown();
		if (!known) return MastersViewMotion.articulatedHandAdmission(acceptedArt, 0.0F, false, false, false);
		return MastersViewMotion.articulatedHandAdmission(acceptedArt,
			net.minecraft.util.Mth.lerp(partial, hands.oldMainHandHeight(), hands.mainHandHeight()),
			net.minecraft.world.item.ItemStack.isSameItemSameComponents(hands.mainHandItem(), dev.wildercord.client.compat.RenderCompat.mainHandStack(avatar)),
			true, hands.mainHandEquipping());
	}

	public ArticulatedRig rig() { return rig; }
}
