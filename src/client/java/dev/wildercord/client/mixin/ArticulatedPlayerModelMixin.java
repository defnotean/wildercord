package dev.wildercord.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.client.combat.ArticulatedCombat;
import dev.wildercord.client.combat.ArticulatedArmorRenderer;
import dev.wildercord.client.combat.ArticulatedModelAccess;
import dev.wildercord.client.combat.ArticulatedRig;
import dev.wildercord.client.combat.ArticulatedViewModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Models keep both backends, but exactly one may own an accepted frame's body and hand sockets. */
@Mixin(PlayerModel.class)
public abstract class ArticulatedPlayerModelMixin implements ArticulatedModelAccess {
	@Unique private ArticulatedRig wildercord$rig;
	@Unique private ArticulatedViewModel wildercord$view;

	@Unique private ArticulatedArmorRenderer wildercord$armor;
	/** The state the model was last posed for: 1.21.8 translateToHand is not given one. */
	@Unique private PlayerRenderState wildercord$posed;

	@Override public void wildercord$ownBody(boolean slim) {
		if (((Object) this).getClass() != PlayerModel.class || wildercord$rig != null) return;
		wildercord$rig = new ArticulatedRig(slim, false);
		wildercord$rig.attach(((PlayerModel) (Object) this).root());
		wildercord$view = new ArticulatedViewModel(slim);
	}

	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;)V", at = @At("HEAD"))
	private void wildercord$restoreBody(PlayerRenderState state, CallbackInfo ci) {
		wildercord$posed = state;
		if (wildercord$rig != null) wildercord$rig.restoreRigid((PlayerModel) (Object) this);
	}

	@Inject(method = "translateToHand(Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
		at = @At("HEAD"), cancellable = true)
	private void wildercord$hand(HumanoidArm arm, PoseStack stack, CallbackInfo ci) {
		PlayerRenderState state = wildercord$posed;
		var frame = ArticulatedCombat.frame(state);
		if (frame == null || !wildercord$bodyOwned()) return;
		ArticulatedCombat.translateHeld(wildercord$rig, (PlayerModel) (Object) this, state, arm, stack);
		ci.cancel();
	}

	@Override public boolean wildercord$bodyOwned() { return wildercord$rig != null; }
	@Override public ArticulatedArmorRenderer wildercord$armor() { return wildercord$armor; }
	@Override public void wildercord$setArmor(ArticulatedArmorRenderer armor) {
		if (!wildercord$bodyOwned()) throw new IllegalStateException("Armor requires the primary body owner");
		wildercord$armor = armor;
	}
	@Override public ArticulatedRig wildercord$rig() { return wildercord$rig; }
	@Override public ArticulatedViewModel wildercord$viewModel() { return wildercord$view; }
}
