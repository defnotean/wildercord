package dev.wildercord.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildercord.aura.ArticulatedCombatPose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;

/** Actual skin-textured upper arms, forearms and hands, authored independently of the world camera. */
public final class ArticulatedViewModel extends Model<ArticulatedViewModel.Frame> {
	public record Frame(ArticulatedCombatPose.ViewPose pose, boolean leftSleeve, boolean rightSleeve) {}
	private final ArticulatedRig rig;

	public ArticulatedViewModel(boolean slim) { this(new ArticulatedRig(slim, false)); }
	private ArticulatedViewModel(ArticulatedRig rig) {
		super(rig.root, RenderTypes::entityTranslucent);
		this.rig = rig;
	}

	@Override
	public void setupAnim(Frame frame) {
		rig.apply(frame.pose()::local);
		rig.viewLayers(frame.leftSleeve(), frame.rightSleeve());
		rig.armsOnly();
	}

	/** True means this backend owns both arms and the sword, including the otherwise-empty offhand. */
	public static boolean submit(AvatarRenderState avatar, net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState hands,
			float partial, InteractionHand hand, PoseStack stack, SubmitNodeCollector collector, int light) {
		ArticulatedCombat.Frame combat = ArticulatedCombat.viewFrame(avatar);
		if (combat == null || net.minecraft.util.Mth.lerp(partial, hands.oldMainHandHeight, hands.mainHandHeight) < .999F
			|| !net.minecraft.world.item.ItemStack.isSameItemSameComponents(hands.mainHandItem, avatar.getMainHandItemStack())) return false;
		PlayerModel playerModel = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(avatar).getModel();
		if (playerModel.getClass() != PlayerModel.class || !(playerModel instanceof ArticulatedModelAccess access)) return false;
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
			stack.rotateDegrees(Axis.YP, -yaw);
			stack.rotateDegrees(Axis.XP, -pitch);
			stack.scale(-1, -1, 1);
			collector.submitModel(model, frame, stack, RenderTypes.entityTranslucent(avatar.skin.body().texturePath()), light,
				OverlayTexture.NO_OVERLAY, -1, null, 0);
			// The same immutable palette sets the hand at submission and deferred model rendering.
			model.setupAnim(frame);
			model.rig.socket(avatar.mainArm, stack);
			ArticulatedCombat.orientItemAtSocket(stack);
			avatar.getMainHandItemState().submit(stack, collector, light, OverlayTexture.NO_OVERLAY, 0);
		} finally {
			stack.popPose();
		}
		return true;
	}

	public ArticulatedRig rig() { return rig; }
}
